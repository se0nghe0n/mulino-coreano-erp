package com.mulino.runtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.application.runtime.*;
import com.mulino.domain.runtime.RuntimeRepository;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Current recovery selection respects trigger-owned chronology; future rows remain immutable. */
class RecoveryClockBoundaryPostgresTest {
 @Test void futureRecordedScheduleCannotBlockCurrentDueWorkAndBecomesVisibleAtItsBoundary() throws Exception {
  try(var pg=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280")) {
   pg.start();var source=new DriverManagerDataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
   Flyway.configure().dataSource(source).locations("filesystem:../database/migrations").load().migrate();
   var db=new JdbcTemplate(source);var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
   Instant current=Instant.parse("2026-10-07T09:00:00Z"),future=current.plusSeconds(30);
   var time=new AtomicReference<>(current);ExecutionClock clock=new ExecutionClock(){@Override public Instant instant(){return time.get();}};
   var repository=new RuntimeRepository(db,clock);
   @SuppressWarnings("unchecked") ObjectProvider<ExternalDeliveryAuthorization> guards=mock(ObjectProvider.class);
   var service=new RuntimeService(repository,clock,mock(IdentityAuthorization.class),mock(RuntimeDutyPort.class),mock(com.mulino.application.evidence.ExternalResultEvidenceGuard.class),guards);
   String org=UUID.randomUUID().toString(),actor=UUID.randomUUID().toString(),nowWork=UUID.randomUUID().toString(),futureWork=UUID.randomUUID().toString();
   var c=new DomainContext(org,actor,actor,current,current);
   tx.executeWithoutResult(s->service.schedule(c,nowWork,"WORK_RECHECK",current,null,true,actor,actor,"Check current due work"));
   time.set(future);
   tx.executeWithoutResult(s->service.schedule(c,futureWork,"WORK_RECHECK",current,null,true,actor,actor,"Retain future recorded schedule"));
   var before=db.queryForMap("SELECT * FROM mulino_runtime_RecoverySchedules WHERE workId=?",futureWork);
   var history=db.queryForList("SELECT * FROM mulino_runtime_RecoveryObservationHistory ORDER BY ID");
   time.set(current);
   var due=tx.execute(s->service.dueSchedules(100));
   assertEquals(List.of(nowWork),due.stream().map(row->row.get("workid")).toList());
   assertEquals(before,db.queryForMap("SELECT * FROM mulino_runtime_RecoverySchedules WHERE workId=?",futureWork));
   assertEquals(history,db.queryForList("SELECT * FROM mulino_runtime_RecoveryObservationHistory ORDER BY ID"));
   assertThrows(RuntimeException.class,()->tx.executeWithoutResult(s->service.manualRecovery(before,"CLOCK_COUNTEREXAMPLE","Must reject a backwards write",c)));
   assertEquals(history,db.queryForList("SELECT * FROM mulino_runtime_RecoveryObservationHistory ORDER BY ID"));
   time.set(future.minusMillis(1));
   assertEquals(List.of(nowWork),tx.execute(s->service.dueSchedules(100)).stream().map(row->row.get("workid")).toList());
   time.set(future);
   var atBoundary=tx.execute(s->service.dueSchedules(100));
   assertEquals(Set.of(nowWork,futureWork),new HashSet<>(atBoundary.stream().map(row->row.get("workid")).toList()));
   tx.executeWithoutResult(s->service.completedRecovery(before,"RECHECKED_AT_BOUNDARY"));
   assertEquals("CHECKED",db.queryForObject("SELECT recoveryStatus FROM mulino_runtime_RecoverySchedules WHERE workId=?",String.class,futureWork));
   assertEquals(List.of(nowWork),tx.execute(s->service.dueSchedules(100)).stream().map(row->row.get("workid")).toList());
   String evidence=System.getProperty("recovery.evidence");
   if(evidence!=null){Path p=Path.of(evidence);Files.createDirectories(p);Files.writeString(p.resolve("future-current-boundary.json"),new tools.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("postgresVersion",db.queryForObject("SHOW server_version",String.class),"current",current.toString(),"future",future.toString(),"futureSourceUnchanged",true,"backwardsWriteRejected",true,"history",db.queryForList("SELECT row_to_json(h)::text AS raw FROM mulino_runtime_RecoveryObservationHistory h ORDER BY ID"),"currentSelectedWorkId",nowWork,"futureSelectedAtExactBoundary",futureWork)));}
  }
 }
}
