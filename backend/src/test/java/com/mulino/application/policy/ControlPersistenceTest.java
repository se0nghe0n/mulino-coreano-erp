package com.mulino.application.policy;
import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.*;
import com.mulino.application.identity.*;
import com.mulino.domain.identity.IdentityRepository;
import com.mulino.domain.governance.*;
import com.sap.cds.services.runtime.CdsRuntime;
import java.time.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
@org.springframework.context.annotation.Import(ControlPersistenceTest.TimeFixture.class)
class ControlPersistenceTest {
 @org.springframework.boot.test.context.TestConfiguration static class TimeFixture { @org.springframework.context.annotation.Bean Clock clock(){return Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"),ZoneOffset.UTC);}
  @org.springframework.context.annotation.Bean CommandHandler twoWorksHandler(JdbcTemplate jdbc){return new CommandHandler(){
    public Set<String> capabilities(){return Set.of("fixtureTwoWorks");}
    public CommandPreparation prepare(DomainContext c,Map<String,Object> intent){var works=(List<String>)IdentityCommands.payload(intent).get("works");if(works==null||works.size()!=2||new HashSet<>(works).size()!=2)throw DomainError.invalid("Two distinct works required");works.forEach(CommandRequests::uuid);return CommandPreparation.ordinary(Map.of("WORK",List.copyOf(works)),works.stream().map(w->"control-fixture:"+w).toList(),"CONTROL_TEST",null,null);}
    public Map<String,Object> execute(DomainContext c,Map<String,Object> intent){var works=(List<String>)IdentityCommands.payload(intent).get("works");jdbc.update("INSERT INTO control_fixture_effects(organizationId,ID,work1,work2) VALUES(?,?,?,?)",c.organizationId(),UUID.randomUUID().toString(),works.get(0),works.get(1));return Map.of("outcome","APPLIED","effects",Map.of("works",List.copyOf(works)));}
  };}
 }
 static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");static{PG.start();}
 static final java.nio.file.Path BLOB;static {try {BLOB=java.nio.file.Files.createTempDirectory("mulino-control-blob-");java.nio.file.Files.setPosixFilePermissions(BLOB,java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 @DynamicPropertySource static void props(DynamicPropertyRegistry r){r.add("mulino.evidence.blob-root",BLOB::toString);r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);r.add("JWT_PUBLIC_KEY",()->System.getenv("JWT_PUBLIC_KEY"));r.add("JWT_ISSUER",()->"https://fixture.invalid");r.add("JWT_AUDIENCE",()->"mulino-acceptance");}
 @Autowired ApplicationCommands commands;@Autowired JdbcTemplate jdbc;@Autowired IdentityRepository identity;@Autowired PolicyRepository policy;@Autowired ApprovalRepository approvals;@Autowired CdsRuntime runtime;@Autowired PlatformTransactionManager tm;
 String org,actor,owner,grant,pid;Instant now=Instant.parse("2026-10-08T00:00:00Z");DomainContext ctx;IdentityAuthorization auth;PolicyCommandGuard guard;
 @BeforeEach void seed(){
   jdbc.execute("CREATE TABLE IF NOT EXISTS control_fixture_effects(organizationId VARCHAR(36) NOT NULL,ID VARCHAR(36) PRIMARY KEY,work1 VARCHAR(36) NOT NULL,work2 VARCHAR(36) NOT NULL)");
   org=id();actor=id();owner=actor;grant=id();pid=id();ctx=new DomainContext(org,actor,owner,now.minusSeconds(1000000),now.minusSeconds(1000000));
   jdbc.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?,?)",org,id());
   jdbc.update("INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner) VALUES(?,?,'HUMAN',?)",org,actor,owner);
   jdbc.update("INSERT INTO mulino_identity_AuthorityFences VALUES(?,?,1)",org,actor);
   jdbc.update("INSERT INTO mulino_identity_Memberships(organizationId,ID,actorId,validFrom,validUntil) VALUES(?,?,?,?,?)",org,id(),actor,ts(now.minusSeconds(60)),ts(now.plusSeconds(60)));
   for(String cap:List.of("reserveQuantity","createGrant","dispatchQuantity","revokeGrant","createPolicyDraft","approvePolicy","activatePolicy"))jdbc.update("INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,?,'ORGANIZATION',?,?,?)",org,id(),actor,cap,org,ts(now.minusSeconds(60)),ts(now.plusSeconds(60)));
   jdbc.update("INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil) VALUES(?,?,?,?,?,?)",org,grant,actor,actor,ts(now.minusSeconds(60)),ts(now.plusSeconds(60)));
   for(String cap:List.of("reserveQuantity","createGrant","revokeGrant","createPolicyDraft","approvePolicy","activatePolicy"))jdbc.update("INSERT INTO mulino_identity_GrantActions VALUES(?,?,?)",org,grant,cap);
   jdbc.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'ORGANIZATION',?)",org,grant,org);
   String content="{\"rules\":{\"reserveQuantity\":{\"effectClass\":\"RESERVE\"},\"revokeGrant\":{\"effectClass\":\"IDENTITY_CONTROL\"},\"createPolicyDraft\":{\"effectClass\":\"POLICY_CONTROL\"},\"approvePolicy\":{\"effectClass\":\"POLICY_CONTROL\"},\"activatePolicy\":{\"effectClass\":\"POLICY_CONTROL\"}}}";
   jdbc.update("INSERT INTO mulino_governance_PolicyVersions(organizationId,ID,revision,createdAt,version,kind,content,contentHash,effectiveFrom,effectiveUntil) VALUES(?,?,1,?,'fixture-v1','COMMAND',?,?,?,?)",org,pid,ts(now),content,PolicyCommands.hash(content),ts(now.minusSeconds(60)),ts(now.plusSeconds(60)));
   jdbc.update("INSERT INTO mulino_governance_ActivePolicies VALUES(?,'COMMAND',?,1)",org,pid);

   jdbc.update("INSERT INTO mulino_identity_ExternalIdentities(organizationId,ID,actorId,issuer,subject,organizationAlias) VALUES(?,?,?,'https://fixture.invalid',?,?)",org,id(),actor,actor,org);
   try {
     var capabilities=List.of("revokeGrant","createPolicyDraft","approvePolicy","activatePolicy","fixtureTwoWorks").stream().map(cap->new com.mulino.domain.definitions.Definition.Capability(cap,"1.0.0","core-v1","1.0.0","1.0.0",List.of())).toList();
     var nouns=List.of("Human","Agent","Grant","CapabilityAssignment","PolicyVersion").stream().map(noun->new com.mulino.domain.definitions.Definition.NounType(noun,true)).toList();
     var verbs=capabilities.stream().map(cap->new com.mulino.domain.definitions.Definition.Verb(cap.capabilityId(),"COMMAND",cap.capabilityId(),"CONTROL",Map.of())).toList();
     var d=new com.mulino.domain.definitions.Definition(org,id(),"1.0.0",null,"PUBLISHED",null,"core-v1","1.0.0",nouns,List.of(),verbs,List.of(),List.of(),capabilities);
     String definition=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(d);
     jdbc.update("INSERT INTO mulino_definitions_DefinitionVersions(organizationId,ID,createdAt,version,state,contentHash,content,evaluatorVersion,schemaVersion) VALUES(?,?,CURRENT_TIMESTAMP,'1.0.0','PUBLISHED',?,?,'core-v1','1.0.0')",org,d.id(),com.mulino.domain.definitions.DefinitionRepository.sha256(definition),definition);
   }catch(Exception failure){throw new IllegalStateException(failure);}
   auth=new IdentityAuthorization(identity,new ExecutionClock(Clock.fixed(now,ZoneOffset.UTC)));guard=new PolicyCommandGuard(auth,identity,policy,approvals);
 }
 CommandPreparation preparation(){return CommandPreparation.ordinary(Map.of("ORGANIZATION",List.of(org)),List.of(),"RESERVE",null,null);}
 void tx(Runnable action){new TransactionTemplate(tm).executeWithoutResult(s->runtime.requestContext().run(c->{action.run();}));}
 @Test void currentGrantIntersectionPolicyMatrixAndHistoricalQuery(){tx(()->{
   guard.fence(ctx,preparation());guard.verify(ctx,"reserveQuantity","hash",preparation(),Map.of());
   assertThrows(DomainError.class,()->guard.verify(ctx,"dispatchQuantity","hash",preparation(),Map.of()));
   assertThrows(DomainError.class,()->guard.verify(ctx,"reserveQuantity","hash",CommandPreparation.ordinary(preparation().scopes(),List.of(),"DISPATCH",null,null),Map.of()));
   var expired=new IdentityAuthorization(identity,new ExecutionClock(Clock.fixed(now.plusSeconds(61),ZoneOffset.UTC)));assertFalse(expired.permittedScopes(ctx,"reserveQuantity",preparation().scopes()));
   assertEquals(1,policy.current(org,"COMMAND",now).size());assertTrue(policy.current(org,"COMMAND",now.plusSeconds(61)).isEmpty());
 });}

 @Test void conflictingGrantAndPolicySubjectsHaveZeroControlEffects(){
   var badGrant=command("revokeGrant",Map.of("id",grant),1);badGrant.put("subjectRefs",List.of(Map.of("type","Grant","id",id())));assertEquals("REJECTED",execute(badGrant).get("outcome"));
   assertEquals(1,jdbc.queryForObject("SELECT revision FROM mulino_identity_Grants WHERE organizationId=? AND ID=?",Integer.class,org,grant));assertNull(jdbc.queryForObject("SELECT revokedAt FROM mulino_identity_Grants WHERE organizationId=? AND ID=?",java.sql.Timestamp.class,org,grant));
   String draft=id(),content="{\"rules\":{}}";tx(()->policy.insert("PolicyDrafts",IdentityCommands.fields("organizationId",org,"ID",draft,"kind","COMMAND","version","subject-fixture","content",content,"contentHash",PolicyCommands.hash(content),"source","synthetic-local","regressionEvidence","subject-counterexample","effectiveFrom",now.minusSeconds(1),"effectiveUntil",now.plusSeconds(30),"legallyRestrictive",false,"fixtureOnly",true,"status","DRAFT","revision",1)));
   var badPolicy=command("approvePolicy",Map.of("id",draft,"contentHash",PolicyCommands.hash(content)),1);badPolicy.put("subjectRefs",List.of(Map.of("type","PolicyVersion","id",pid)));assertEquals("REJECTED",execute(badPolicy).get("outcome"));
   assertEquals("DRAFT",jdbc.queryForObject("SELECT status FROM mulino_governance_PolicyDrafts WHERE organizationId=? AND ID=?",String.class,org,draft));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_governance_PolicyApprovals WHERE organizationId=?",Integer.class,org));
   var valid=command("approvePolicy",Map.of("id",draft,"contentHash",PolicyCommands.hash(content)),1);valid.put("subjectRefs",List.of(Map.of("type","PolicyVersion","id",draft)));assertEquals("APPLIED",execute(valid).get("outcome"));
   assertEquals("APPROVED",jdbc.queryForObject("SELECT status FROM mulino_governance_PolicyDrafts WHERE organizationId=? AND ID=?",String.class,org,draft));
 }
 @Test void replayRequiresEverySavedWorkAfterPartialGrantRevocation() throws Exception {
   String w1=id(),w2=id(),g1=id(),g2=id(),cap="fixtureTwoWorks";
   for(var entry:Map.of(w1,g1,w2,g2).entrySet()){
     jdbc.update("INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,?,'WORK',?,?,?)",org,id(),actor,cap,entry.getKey(),ts(now.minusSeconds(60)),ts(now.plusSeconds(60)));
     jdbc.update("INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil) VALUES(?,?,?,?,?,?)",org,entry.getValue(),actor,actor,ts(now.minusSeconds(60)),ts(now.plusSeconds(60)));
     jdbc.update("INSERT INTO mulino_identity_GrantActions VALUES(?,?,?)",org,entry.getValue(),cap);jdbc.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'WORK',?)",org,entry.getValue(),entry.getKey());
   }
   tx(()->{var original=policy.row("PolicyVersions",org,pid).orElseThrow();String content=(String)original.get("content");var document=new HashMap<String,Object>();try{document.putAll(new com.fasterxml.jackson.databind.ObjectMapper().readValue(content,Map.class));}catch(Exception e){throw new RuntimeException(e);}var rules=new HashMap<String,Object>((Map<String,Object>)document.get("rules"));rules.put(cap,Map.of("effectClass","CONTROL_TEST"));document.put("rules",rules);String json;try{json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(document);}catch(Exception e){throw new RuntimeException(e);}String updated=id();policy.insert("PolicyVersions",Map.of("organizationId",org,"ID",updated,"revision",1,"createdAt",now,"version","two-work-fixture","kind","COMMAND","content",json,"contentHash",PolicyCommands.hash(json),"effectiveFrom",now.minusSeconds(1),"effectiveUntil",now.plusSeconds(60)));policy.activate(org,"COMMAND",updated);});
   var request=command(cap,Map.of("works",List.of(w1,w2)),null);var first=execute(request);assertEquals("APPLIED",first.get("outcome"));assertEquals(List.of(w1,w2),((Map<?,?>)first.get("effects")).get("works"));
   tx(()->{auth.fence(ctx,List.of());identity.update("Grants",org,g2,Map.of("revokedAt",now,"revision",2));});
   tx(()->{assertTrue(auth.permittedScopes(ctx,cap,Map.of("WORK",List.of(w1,w2))),"Search OR still has W1");assertTrue(auth.permittedScopes(ctx,cap,Map.of("WORK",List.of(w1))));assertFalse(auth.permittedScopes(ctx,cap,Map.of("WORK",List.of(w2))));});
   var replay=execute(request);assertEquals("REJECTED",replay.get("outcome"));assertEquals(Map.of(),replay.get("effects"));
   assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM control_fixture_effects WHERE organizationId=?",Integer.class,org));assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_commands_CommandAudits WHERE organizationId=?",Integer.class,org));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_commands_CommandRecords WHERE organizationId=?",Integer.class,org));
   assertEquals("COMMITTED",jdbc.queryForObject("SELECT state FROM mulino_commands_CommandRecords WHERE organizationId=?",String.class,org));
   assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_commands_CommandAudits WHERE organizationId=? AND commandId=?",Integer.class,org,first.get("commandId")));
   assertEquals(Set.of("APPLIED","REJECTED"),new HashSet<>(jdbc.queryForList("SELECT outcome FROM mulino_commands_CommandAudits WHERE organizationId=?",String.class,org)));
   var denial=new com.fasterxml.jackson.databind.ObjectMapper().readValue(jdbc.queryForObject("SELECT auditFactsJson FROM mulino_commands_CommandAudits WHERE organizationId=? AND outcome='REJECTED'",String.class,org),Map.class);assertEquals(true,denial.get("expectedDenial"));assertEquals(true,denial.get("replayAttempt"));
   assertEquals(Map.of(),new com.fasterxml.jackson.databind.ObjectMapper().readValue(jdbc.queryForObject("SELECT effectRefs FROM mulino_commands_CommandAudits WHERE organizationId=? AND outcome='REJECTED'",String.class,org),Map.class));
 }
 @Test void selfRevocationCommitsThroughActualGateway() throws Exception {
   var result=execute("revokeGrant",Map.of("id",grant),1);assertEquals("APPLIED",result.get("outcome"));
   assertEquals(2,jdbc.queryForObject("SELECT revision FROM mulino_identity_Grants WHERE organizationId=? AND ID=?",Integer.class,org,grant));
   tx(()->assertFalse(auth.permittedScopes(ctx,"reserveQuantity",preparation().scopes())));
   assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_commands_CommandAudits WHERE organizationId=?",Integer.class,org));
   var facts=new com.fasterxml.jackson.databind.ObjectMapper().readValue(jdbc.queryForObject("SELECT auditFactsJson FROM mulino_commands_CommandAudits WHERE organizationId=?",String.class,org),Map.class);
   assertEquals(pid,facts.get("policyVersionId"));var chain=(List<Map<String,Object>>)facts.get("authorityChain");assertEquals(grant,chain.getFirst().get("grantId"));assertEquals(1,((Number)chain.getFirst().get("grantRevision")).intValue());

 }
 @Test void tighteningPolicyActivationCommitsThroughActualGateway() throws Exception {
   String content="{\"rules\":{}}";
   var create=execute("createPolicyDraft",IdentityCommands.fields("kind","COMMAND","version","tight-v2","content",content,"source","synthetic-local-only","regressionEvidence","fixture-regression","effectiveFrom",now.minusSeconds(1).toString(),"effectiveUntil",now.plusSeconds(30).toString(),"fixtureOnly",true,"legallyRestrictive",true),null);
   assertEquals("APPLIED",create.get("outcome"));String draft=(String)create.get("id");
   assertEquals("APPLIED",execute("approvePolicy",Map.of("id",draft,"contentHash",PolicyCommands.hash(content)),1).get("outcome"));
   assertEquals("APPLIED",execute("activatePolicy",Map.of("id",draft),2).get("outcome"));
   assertEquals(draft,jdbc.queryForObject("SELECT policyId FROM mulino_governance_ActivePolicies WHERE organizationId=? AND kind='COMMAND'",String.class,org));
   tx(()->assertThrows(DomainError.class,()->guard.verify(ctx,"reserveQuantity","hash",preparation(),Map.of())));
   var facts=new com.fasterxml.jackson.databind.ObjectMapper().readValue(jdbc.queryForObject("SELECT auditFactsJson FROM mulino_commands_CommandAudits WHERE organizationId=? AND capabilityId='activatePolicy'",String.class,org),Map.class);
   assertEquals(pid,facts.get("policyVersionId"));assertNotEquals(draft,facts.get("policyVersionId"));

 }
 Map<String,Object> command(String capability,Map<String,Object> slots,Integer revision){var intent=new LinkedHashMap<String,Object>(Map.of("intentKind","COMMAND","definitionVersion","1.0.0","capabilityId",capability,"subjectRefs",List.of(),"slots",slots,"provenance",Map.of(),"commandIdempotencyKey",id()));if(revision!=null)intent.put("expectedRevision",revision);return intent;}
 Map<String,Object> execute(String capability,Map<String,Object> slots,Integer revision){return execute(command(capability,slots,revision));}
 Map<String,Object> execute(Map<String,Object> intent){
   var jwt=Jwt.withTokenValue("synthetic-native-command-context").header("alg","RS256").issuer("https://fixture.invalid").subject(actor).claim("organizationId",org).build();
   SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,List.of()));
   try{return runtime.requestContext().run(c->{return commands.execute(intent);});}finally{SecurityContextHolder.clearContext();}
 }
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
 @AfterAll static void cleanup() throws Exception {PG.stop();try(var paths=java.nio.file.Files.walk(BLOB)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())java.nio.file.Files.deleteIfExists(path);}}
 private static java.sql.Timestamp ts(Instant at){return java.sql.Timestamp.from(at);}
 private static String id(){return UUID.randomUUID().toString();}
}
