package com.mulino.application.policy;
import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.*;
import com.mulino.application.identity.*;
import com.mulino.domain.identity.IdentityRepository;
import com.mulino.domain.governance.*;
import com.sap.cds.services.runtime.CdsRuntime;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
@SpringBootTest
@ActiveProfiles("local")
class ControlPersistenceTest {
 static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");static{PG.start();}
 @DynamicPropertySource static void props(DynamicPropertyRegistry r){r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);r.add("JWT_PUBLIC_KEY",()->System.getenv("JWT_PUBLIC_KEY"));r.add("JWT_ISSUER",()->"https://fixture.invalid");r.add("JWT_AUDIENCE",()->"mulino-acceptance");}
 @Autowired JdbcTemplate jdbc;@Autowired IdentityRepository identity;@Autowired PolicyRepository policy;@Autowired ApprovalRepository approvals;@Autowired CdsRuntime runtime;@Autowired PlatformTransactionManager tm;
 String org,actor,owner,grant,pid;Instant now=Instant.parse("2026-10-08T00:00:00Z");DomainContext ctx;IdentityAuthorization auth;PolicyCommandGuard guard;
 @BeforeEach void seed(){
   org=id();actor=id();owner=actor;grant=id();pid=id();ctx=new DomainContext(org,actor,owner,now.minusSeconds(1000000),now.minusSeconds(1000000));
   jdbc.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?,?)",org,id());
   jdbc.update("INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner) VALUES(?,?,'HUMAN',?)",org,actor,owner);
   jdbc.update("INSERT INTO mulino_identity_AuthorityFences VALUES(?,?,1)",org,actor);
   jdbc.update("INSERT INTO mulino_identity_Memberships(organizationId,ID,actorId,validFrom,validUntil) VALUES(?,?,?,?,?)",org,id(),actor,now.minusSeconds(60),now.plusSeconds(60));
   for(String cap:List.of("reserveQuantity","createGrant","dispatchQuantity"))jdbc.update("INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,?,'ORGANIZATION',?,?,?)",org,id(),actor,cap,org,now.minusSeconds(60),now.plusSeconds(60));
   jdbc.update("INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil) VALUES(?,?,?,?,?,?)",org,grant,actor,actor,now.minusSeconds(60),now.plusSeconds(60));
   for(String cap:List.of("reserveQuantity","createGrant"))jdbc.update("INSERT INTO mulino_identity_GrantActions VALUES(?,?,?)",org,grant,cap);
   jdbc.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'ORGANIZATION',?)",org,grant,org);
   String content="{\"rules\":{\"reserveQuantity\":{\"effectClass\":\"RESERVE\"}}}";
   jdbc.update("INSERT INTO mulino_governance_PolicyVersions(organizationId,ID,revision,createdAt,version,kind,content,contentHash,effectiveFrom,effectiveUntil) VALUES(?,?,1,?,'fixture-v1','COMMAND',?,?,?,?)",org,pid,now,content,PolicyCommands.hash(content),now.minusSeconds(60),now.plusSeconds(60));
   jdbc.update("INSERT INTO mulino_governance_ActivePolicies VALUES(?,'COMMAND',?,1)",org,pid);
   auth=new IdentityAuthorization(identity,new ExecutionClock(Clock.fixed(now,ZoneOffset.UTC)));guard=new PolicyCommandGuard(auth,identity,policy,approvals);
 }
 CommandPreparation preparation(){return CommandPreparation.ordinary(Map.of("ORGANIZATION",List.of(org)),List.of(),"RESERVE",null,null);}
 void tx(Runnable action){new TransactionTemplate(tm).executeWithoutResult(s->runtime.requestContext().run(c->action.run()));}
 @Test void currentGrantIntersectionPolicyMatrixAndHistoricalQuery(){tx(()->{
   guard.fence(ctx,preparation());guard.verify(ctx,"reserveQuantity","hash",preparation(),Map.of());
   assertThrows(DomainError.class,()->guard.verify(ctx,"dispatchQuantity","hash",preparation(),Map.of()));
   assertThrows(DomainError.class,()->guard.verify(ctx,"reserveQuantity","hash",CommandPreparation.ordinary(preparation().scopes(),List.of(),"DISPATCH",null,null),Map.of()));
   var expired=new IdentityAuthorization(identity,new ExecutionClock(Clock.fixed(now.plusSeconds(61),ZoneOffset.UTC)));assertFalse(expired.permittedScopes(ctx,"reserveQuantity",preparation().scopes()));
   assertEquals(1,policy.current(org,"COMMAND",now).size());assertTrue(policy.current(org,"COMMAND",now.plusSeconds(61)).isEmpty());
 });}
 @Test void publishedPolicyIsImmutableAndCrossOrganizationPointerRejected(){
   assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE mulino_governance_PolicyVersions SET content='changed' WHERE organizationId=? AND ID=?",org,pid));
   String other=id();jdbc.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?,?)",other,id());assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("INSERT INTO mulino_governance_ActivePolicies VALUES(?,'COMMAND',?,1)",other,pid));
 }
 @Test void revocationAndExecutionSerializeOnSameFence() throws Exception {
   var locked=new CountDownLatch(1);var release=new CountDownLatch(1);var revoked=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
   try{
    Future<?> first=pool.submit(()->tx(()->{guard.fence(ctx,preparation());guard.verify(ctx,"reserveQuantity","hash",preparation(),Map.of());locked.countDown();try{assertTrue(release.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}}));
    assertTrue(locked.await(5,TimeUnit.SECONDS));Future<?> second=pool.submit(()->tx(()->{identity.fence(org,List.of(actor));identity.update("Grants",org,grant,Map.of("revokedAt",now,"revision",2));revoked.countDown();}));
    assertFalse(revoked.await(150,TimeUnit.MILLISECONDS));release.countDown();first.get(10,TimeUnit.SECONDS);second.get(10,TimeUnit.SECONDS);
    tx(()->{guard.fence(ctx,preparation());assertThrows(DomainError.class,()->guard.verify(ctx,"reserveQuantity","hash",preparation(),Map.of()));});
   }finally{release.countDown();pool.shutdownNow();}
 }
 private static String id(){return UUID.randomUUID().toString();}
}
