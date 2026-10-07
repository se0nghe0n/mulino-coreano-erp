package com.mulino.commands;
import com.mulino.application.core.*;
import com.sap.cds.services.runtime.CdsRuntime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;
@SpringBootTest
@ActiveProfiles("local")
@Import(CommandTransactionTest.Fixture.class)
class CommandTransactionTest {
 static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
 static final Path KEY,BLOB;
 static {try{PG.start();BLOB=Files.createTempDirectory("mulino-command-blob-");Files.setPosixFilePermissions(BLOB,java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));KEY=Files.createTempFile("mulino-command-public-",".pem");var g=java.security.KeyPairGenerator.getInstance("RSA");g.initialize(2048);Files.writeString(KEY,"-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(g.generateKeyPair().getPublic().getEncoded())+"\n-----END PUBLIC KEY-----\n");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);r.add("JWT_PUBLIC_KEY",KEY::toString);r.add("JWT_ISSUER",()->"https://mulino.local.invalid");r.add("JWT_AUDIENCE",()->"mulino-platform");r.add("mulino.evidence.blob-root",BLOB::toString);}
 @MockitoBean com.mulino.application.identity.IdentityAuthorization auth;

 @Autowired ApplicationCommands commands;@Autowired JdbcTemplate jdbc;@Autowired CdsRuntime runtime;@Autowired Guard guard;@Autowired CommandLeasePort lease;
 String org,actor;DomainContext context;
 @TestConfiguration static class Fixture {
  @Bean @Primary CommandLeasePort fixtureLease(){return mock(CommandLeasePort.class);}
  @Bean @Primary Guard guard(){return new Guard();}
  @Bean CommandHandler fixtureHandler(JdbcTemplate jdbc){return new CommandHandler(){
   public Set<String> capabilities(){return Set.of("fixtureIncrement","fixtureClose");}
   public CommandPreparation prepare(DomainContext c,Map<String,Object> i){if("fixtureClose".equals(i.get("capabilityId"))&&jdbc.queryForObject("SELECT revision FROM command_fixture WHERE organizationId=?",Integer.class,c.organizationId())>0)throw DomainError.invalid("Parent consumed");if(!((Map<?,?>)i.get("slots")).keySet().equals(Set.of("amount")))throw DomainError.invalid("Typed amount required");return CommandPreparation.ordinary(Map.of("ORGANIZATION",List.of(c.organizationId()),"WORK",List.of(c.actorId())),List.of("fixture-state"),"INTERNAL_MOVE",null,jdbc.queryForObject("SELECT revision FROM command_fixture WHERE organizationId=?",Integer.class,c.organizationId()));}
   public Map<String,Object> execute(DomainContext c,Map<String,Object> i){jdbc.update("UPDATE command_fixture SET revision=revision+1,quantity=quantity+? WHERE organizationId=?",Integer.parseInt((String)((Map<?,?>)i.get("slots")).get("amount")),c.organizationId());return Map.of("outcome","APPLIED","revision",jdbc.queryForObject("SELECT revision FROM command_fixture WHERE organizationId=?",Integer.class,c.organizationId()),"effects",Map.of("movement","fixture"));}
  };}
 }
 static class Guard implements CommandGuard {
  AtomicBoolean allowed=new AtomicBoolean(true);AtomicBoolean approval=new AtomicBoolean(true);AtomicBoolean after=new AtomicBoolean(false);int verifies;
  public void fence(DomainContext c,CommandPreparation p){}
  public void verify(DomainContext c,String cap,String hash,CommandPreparation p,Map<String,Object> i){verifies++;if(!allowed.get()||after.get()&&verifies%2==0)throw DomainError.forbidden();if(!approval.get())throw new DomainError("WAITING_APPROVAL","APPROVAL_REQUIRED","Approval required");}
 }
 @BeforeEach void setup(){org=UUID.randomUUID().toString();actor=UUID.randomUUID().toString();jdbc.execute("CREATE TABLE IF NOT EXISTS command_fixture(organizationId VARCHAR(36) PRIMARY KEY,revision INTEGER NOT NULL,quantity INTEGER NOT NULL)");jdbc.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES (?,?)",org,org);jdbc.update("INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner) VALUES (?,?,'HUMAN',?)",org,actor,actor);jdbc.update("INSERT INTO command_fixture VALUES (?,0,0)",org);context=new DomainContext(org,actor,actor,Instant.now(),Instant.now());when(auth.context(any(),any())).thenReturn(context);reset(lease);when(lease.resolveContext(anyMap(),any())).thenReturn(context);guard.allowed.set(true);guard.approval.set(true);guard.after.set(false);guard.verifies=0;
    try{
      String definition=UUID.randomUUID().toString();
      var d=new com.mulino.domain.definitions.Definition(org,definition,"fixture-published",null,"PUBLISHED",null,"core-v1","1.0.0",List.of(),List.of(),List.of(),List.of(),List.of(),List.of(new com.mulino.domain.definitions.Definition.Capability("fixtureIncrement","1.0.0","core-v1","1.0.0","1.0.0",List.of()),new com.mulino.domain.definitions.Definition.Capability("fixtureClose","1.0.0","core-v1","1.0.0","1.0.0",List.of())));
      String content=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(d);
      jdbc.update("INSERT INTO mulino_definitions_DefinitionVersions(organizationId,ID,createdAt,version,state,contentHash,content,evaluatorVersion,schemaVersion) VALUES (?,?,CURRENT_TIMESTAMP,?,'PUBLISHED',?,?,?,?)",org,definition,"fixture-published",com.mulino.domain.definitions.DefinitionRepository.sha256(content),content,"core-v1","1.0.0");
    }catch(Exception failure){throw new IllegalStateException(failure);}
  }
 Map<String,Object> intent(String key,int rev,String amount){return Map.of("intentKind","COMMAND","definitionVersion","fixture-published","capabilityId","fixtureIncrement","subjectRefs",List.of(),"slots",Map.of("amount",amount),"provenance",Map.of("amount","USER"),"commandIdempotencyKey",key,"expectedRevision",rev);}
 Map<String,Object> execute(Map<String,Object> intent){return runtime.requestContext().run(c->{return commands.execute(intent);});}
 int quantity(){return jdbc.queryForObject("SELECT quantity FROM command_fixture WHERE organizationId=?",Integer.class,org);}
 int count(String table){return jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE organizationId=?",Integer.class,org);}
 @Test void claimForDifferentCapabilityOrKeyOrWorkCannotExecuteAnotherAction(){for(String field:List.of("capabilityId","commandId","workId")){String key="claim-key-"+field;var claim=new HashMap<String,Object>(Map.of("capabilityId","fixtureIncrement","commandId",key,"workId",actor));claim.put(field,"different");var outcome=runtime.requestContext().run(c->{return commands.executeClaimed(intent(key,0,"4"),claim);});assertEquals("REJECTED",outcome.get("outcome"));assertEquals(0,quantity());}var valid=Map.<String,Object>of("capabilityId","fixtureIncrement","commandId","valid-claim","workId",actor);assertEquals("APPLIED",runtime.requestContext().run(c->{return commands.executeClaimed(intent("valid-claim",0,"4"),valid);}).get("outcome"));assertEquals(4,quantity());}
 @Test void replayOfConsumingActionUsesSavedScopeInsteadOfConsumedPrerequisites(){var close=new LinkedHashMap<>(intent("close",0,"4"));close.put("capabilityId","fixtureClose");var first=execute(close);assertEquals("APPLIED",first.get("outcome"));assertEquals(first,execute(close));assertEquals(4,quantity());assertEquals(1,count("mulino_commands_CommandRecords"));assertNotNull(jdbc.queryForObject("SELECT authorizationScopeJson FROM mulino_commands_CommandRecords WHERE organizationId=?",String.class,org));}
 @Test void operationalRetryUsesStoredSameActorOwnerKeyAndNoNewEffect(){var result=execute(intent("retry",0,"4"));String command=(String)result.get("commandId");assertEquals("APPLIED",runtime.requestContext().run(c->{return commands.retryOriginal(command,Map.of());}).get("outcome"));assertEquals(4,quantity());assertEquals(1,count("mulino_commands_CommandRecords"));assertEquals(actor,jdbc.queryForObject("SELECT actorId FROM mulino_commands_CommandRecords WHERE ID=?",String.class,command));assertNotNull(jdbc.queryForObject("SELECT canonicalIntentJson FROM mulino_commands_CommandRecords WHERE ID=?",String.class,command));}
 @Test void unsupportedPublishedCapabilityAndDefinitionHaveZeroEffects(){var request=new LinkedHashMap<>(intent("unsupported",0,"4"));request.put("definitionVersion","absent-version");assertEquals("HELD",execute(request).get("outcome"));assertEquals(0,quantity());}
 @Test void replayAndDifferentPayloadHaveOneRealEffect(){var original=intent("same",0,"4");assertEquals("APPLIED",execute(original).get("outcome"));assertEquals("APPLIED",execute(original).get("outcome"));assertEquals(4,quantity());assertEquals(1,count("mulino_commands_CommandRecords"));assertEquals(1,count("mulino_commands_CommandAudits"));assertEquals("CONFLICT",execute(intent("same",0,"5")).get("outcome"));assertEquals(4,quantity());}
 @Test void staleAndApprovalRequiredPersistStableRejectedWithoutEffects(){assertEquals("CONFLICT",execute(intent("stale",3,"4")).get("outcome"));guard.approval.set(false);var waiting=intent("waiting",0,"4");assertEquals("WAITING_APPROVAL",execute(waiting).get("outcome"));guard.approval.set(true);assertEquals("WAITING_APPROVAL",execute(waiting).get("outcome"));assertEquals(0,quantity());assertEquals(2,count("mulino_commands_CommandRecords"));assertEquals(2,count("mulino_commands_CommandAudits"));}
 @Test void currentGrantIsRequiredOnReplayAndCommit(){var original=intent("grant",0,"4");assertEquals("APPLIED",execute(original).get("outcome"));doThrow(DomainError.forbidden()).when(auth).authorizeScopes(any(),anyString(),anyMap());assertEquals("REJECTED",execute(original).get("outcome"));assertEquals(4,quantity());}
 @Test void postEffectDenialRollsBackEffectAndPersistsOnlySafeDenial(){guard.after.set(true);assertEquals("REJECTED",execute(intent("revoked",0,"4")).get("outcome"));assertEquals(0,quantity());assertEquals(1,count("mulino_commands_CommandRecords"));assertEquals("REJECTED",jdbc.queryForObject("SELECT state FROM mulino_commands_CommandRecords WHERE organizationId=?",String.class,org));}
 @Test void auditFailureRollsBackEffectAndIdempotency(){jdbc.execute("CREATE OR REPLACE FUNCTION command_fixture_fail_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'audit failure fixture'; END $$");jdbc.execute("CREATE TRIGGER command_fixture_audit_failure BEFORE INSERT ON mulino_commands_CommandAudits FOR EACH ROW EXECUTE FUNCTION command_fixture_fail_audit()");try{assertThrows(RuntimeException.class,()->execute(intent("audit",0,"4")));assertEquals(0,quantity());assertEquals(0,count("mulino_commands_CommandRecords"));assertEquals(0,count("mulino_commands_CommandAudits"));}finally{jdbc.execute("DROP TRIGGER command_fixture_audit_failure ON mulino_commands_CommandAudits");}}
 @AfterAll static void cleanup()throws Exception{PG.stop();Files.deleteIfExists(KEY);try(var paths=Files.walk(BLOB)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
}
