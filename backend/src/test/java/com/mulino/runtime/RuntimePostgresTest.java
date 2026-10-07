package com.mulino.runtime;

import com.mulino.application.core.*;
import com.mulino.domain.runtime.RuntimeRepository;
import com.mulino.application.runtime.*;
import com.mulino.application.identity.IdentityAuthorization;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;

/** Real isolated PostgreSQL races; no CAP mock or successful queue shortcut. */
class RuntimePostgresTest {
 static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
 static final String ORG="00000000-0000-0000-0000-000000000101",ACTOR="00000000-0000-0000-0000-000000000102",WORK="00000000-0000-0000-0000-000000000103";
 static final Instant T=Instant.parse("2026-10-08T00:00:00Z");
 JdbcTemplate jdbc;TransactionTemplate tx;RuntimeRepository repo;AtomicReference<Instant> now;
 DomainContext context(){return new DomainContext(ORG,ACTOR,"stable-owner",T,T);}
 @BeforeAll static void start(){PG.start();}
 @AfterAll static void stop(){PG.stop();}
 @BeforeEach void setup() throws Exception {
  var source=new DriverManagerDataSource(PG.getJdbcUrl(),PG.getUsername(),PG.getPassword());jdbc=new JdbcTemplate(source);tx=new TransactionTemplate(new DataSourceTransactionManager(source));
  jdbc.execute("DROP SCHEMA public CASCADE");jdbc.execute("CREATE SCHEMA public");
  jdbc.execute(Files.readString(Path.of("../database/migrations/V13__runtime.sql")));
  jdbc.execute("CREATE TABLE fixture_effect(ID text PRIMARY KEY, quantity integer NOT NULL)");
  now=new AtomicReference<>(T);repo=new RuntimeRepository(jdbc,new ExecutionClock(){@Override public Instant instant(){return now.get();}});
 }
 @Test void expiredClaimReclaimFencesStaleWriterAndRollsBackEffectAndOutbox(){
  var first=tx.execute(s->repo.claim(context(),WORK,"activateWork","command-1","worker-1",Duration.ofSeconds(5)).orElseThrow());
  now.set(T.plusSeconds(6));var second=tx.execute(s->repo.claim(context(),WORK,"activateWork","command-1","worker-2",Duration.ofSeconds(5)).orElseThrow());
  assertEquals(2L,second.get("leaseToken"));
  assertThrows(DomainError.class,()->tx.execute(s->{jdbc.update("INSERT INTO fixture_effect VALUES('stale',60)");repo.enqueue(context(),"command-1","00000000-0000-0000-0000-000000000201","fixtureSend",Map.of("quantity","60"));repo.fenceAndVerify(context(),first);return null;}));
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM fixture_effect",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_runtime_Outbox",Integer.class));
  tx.execute(s->{repo.fenceAndVerify(context(),second);jdbc.update("INSERT INTO fixture_effect VALUES('current',60)");repo.enqueue(context(),"command-1","00000000-0000-0000-0000-000000000202","fixtureSend",Map.of("quantity","60"));repo.finish(context(),second,"SUCCEEDED",null);return null;});
  assertEquals(60,jdbc.queryForObject("SELECT sum(quantity) FROM fixture_effect",Integer.class));
  assertTrue(tx.execute(s->repo.claim(context(),WORK,"activateWork","command-1","worker-3",Duration.ofSeconds(5))).isEmpty());
  assertEquals("EXPIRED",jdbc.queryForObject("SELECT status FROM mulino_runtime_ExecutionAttempts WHERE ID=?",String.class,first.get("attemptId")));
 }
 @Test void twoWorkersCannotClaimSameScopeAndNoSleepControlsRace() throws Exception {
  var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
  try {
   var first=pool.submit(()->tx.execute(s->{var claim=repo.claim(context(),WORK,"activateWork","race","first",Duration.ofSeconds(5));entered.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}return claim;}));
   assertTrue(entered.await(10,TimeUnit.SECONDS));
   // Existing scope rows avoid unique INSERT waiting, and demonstrate SKIP LOCKED.
   // First insertion remains invisible; second legitimately blocks until committed.
   var second=pool.submit(()->tx.execute(s->repo.claim(context(),WORK,"activateWork","race","second",Duration.ofSeconds(5))));
   release.countDown();assertTrue(first.get(10,TimeUnit.SECONDS).isPresent());assertTrue(second.get(10,TimeUnit.SECONDS).isEmpty());
   assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_runtime_ExecutionAttempts WHERE status='ACTIVE'",Integer.class));
  }finally{release.countDown();pool.shutdownNow();}
 }
 @Test void scopeLockCannotBeReclaimedWhileCurrentCommandCommits() throws Exception {
  var claim=tx.execute(s->repo.claim(context(),WORK,"activateWork","lock","first",Duration.ofSeconds(5)).orElseThrow());
  var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
  try {
   var writer=pool.submit(()->tx.execute(s->{repo.fenceAndVerify(context(),claim);entered.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}jdbc.update("INSERT INTO fixture_effect VALUES('winner',60)");repo.fenceAndVerify(context(),claim);return null;}));
   assertTrue(entered.await(10,TimeUnit.SECONDS));now.set(T.plusSeconds(6));
   var contender=pool.submit(()->tx.execute(s->repo.claim(context(),WORK,"activateWork","lock","second",Duration.ofSeconds(5))));
   release.countDown();assertThrows(ExecutionException.class,()->writer.get(10,TimeUnit.SECONDS));contender.get(10,TimeUnit.SECONDS);
   assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM fixture_effect",Integer.class));
  }finally{release.countDown();pool.shutdownNow();}
 }
 @Test void atomicOutboxRollbackDurableReplayAndSecretRejection(){
  assertThrows(IllegalStateException.class,()->repo.enqueue(context(),"c","00000000-0000-0000-0000-000000000203","send",Map.of()));
  assertThrows(RuntimeException.class,()->tx.execute(s->{jdbc.update("INSERT INTO fixture_effect VALUES('rolledback',60)");repo.enqueue(context(),"c","00000000-0000-0000-0000-000000000203","send",Map.of("quantity","60"));throw new RuntimeException("injected audit failure");}));
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_runtime_Outbox",Integer.class));
  String id=tx.execute(s->repo.enqueue(context(),"c","00000000-0000-0000-0000-000000000203","send",Map.of("quantity","60")));
  // Fresh repository models host restart: no in-memory success data is required.
  var restarted=new RuntimeRepository(jdbc,new ExecutionClock(){@Override public Instant instant(){return now.get();}});
  assertEquals(id,tx.execute(s->restarted.enqueue(context(),"c","00000000-0000-0000-0000-000000000203","send",Map.of("quantity","60"))));
  assertThrows(DomainError.class,()->tx.execute(s->repo.enqueue(context(),"c","00000000-0000-0000-0000-000000000203","send",Map.of("quantity","40"))));
  assertThrows(DomainError.class,()->tx.execute(s->repo.enqueue(context(),"c","00000000-0000-0000-0000-000000000204","send",Map.of("authorization","Bearer unsafe"))));
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_runtime_Outbox",Integer.class));
 }
 @Test void unknownExternalCannotBeClaimedAgainEvenWithIdempotencySupport(){
  var auth=mock(IdentityAuthorization.class);var duties=mock(RuntimeDutyPort.class);
  var service=new RuntimeService(repo,new ExecutionClock(){@Override public Instant instant(){return now.get();}},auth,duties,mock(com.mulino.application.evidence.ExternalResultEvidenceGuard.class),mock(org.springframework.beans.factory.ObjectProvider.class));
  String id=tx.execute(s->repo.enqueue(context(),"command-record-id","00000000-0000-0000-0000-000000000205","fixtureExternalEffect",Map.of("workId",WORK,"quantity","60")));
  var delivery=tx.execute(s->service.claimDelivery(context(),"worker-a","fixtureExternalEffect",new ExternalDeliveryAdapter.Support(true,true),3,Duration.ofSeconds(5)).orElseThrow());
  tx.execute(s->{service.deliveryResult(context(),id,((Number)delivery.get("fencingtoken")).longValue(),"worker-a",ExternalDeliveryAdapter.Result.UNKNOWN,null);return null;});
  now.set(T.plusSeconds(6));
  assertTrue(tx.execute(s->service.claimDelivery(context(),"worker-b","fixtureExternalEffect",new ExternalDeliveryAdapter.Support(true,true),3,Duration.ofSeconds(5))).isEmpty());
  assertEquals("UNKNOWN_EXTERNAL",jdbc.queryForObject("SELECT status FROM mulino_runtime_Outbox WHERE ID=?",String.class,id));
  assertEquals(1,jdbc.queryForObject("SELECT deliveryAttempts FROM mulino_runtime_Outbox WHERE ID=?",Integer.class,id));
  verify(duties).ensureRuntimeDuty(eq(context()),eq(WORK),eq("command-record-id"),eq("EXTERNAL_RECONCILIATION"),anyString(),any());
 }
 @Test void crashAfterDeliveryIntentBecomesUnknownAndRetainsDuty(){
  var auth=mock(IdentityAuthorization.class);var duties=mock(RuntimeDutyPort.class);
  var service=new RuntimeService(repo,new ExecutionClock(){@Override public Instant instant(){return now.get();}},auth,duties,mock(com.mulino.application.evidence.ExternalResultEvidenceGuard.class),mock(org.springframework.beans.factory.ObjectProvider.class));
  String id=tx.execute(s->repo.enqueue(context(),"command-record-id","00000000-0000-0000-0000-000000000206","fixtureExternalEffect",Map.of("workId",WORK)));
  assertTrue(tx.execute(s->service.claimDelivery(context(),"dead-worker","fixtureExternalEffect",new ExternalDeliveryAdapter.Support(false,false),3,Duration.ofSeconds(5))).isPresent());
  now.set(T.plusSeconds(6));assertEquals(1,tx.execute(s->service.recoverExpiredDeliveries()).intValue());assertEquals(0,tx.execute(s->service.recoverExpiredDeliveries()).intValue());
  assertEquals("UNKNOWN_EXTERNAL",jdbc.queryForObject("SELECT status FROM mulino_runtime_Outbox WHERE ID=?",String.class,id));
  assertTrue(tx.execute(s->service.claimDelivery(context(),"new-worker","fixtureExternalEffect",new ExternalDeliveryAdapter.Support(false,false),3,Duration.ofSeconds(5))).isEmpty());
  verify(duties,times(1)).ensureRuntimeDuty(any(),eq(WORK),eq("command-record-id"),eq("EXTERNAL_RECONCILIATION"),anyString(),any());
 }
}
