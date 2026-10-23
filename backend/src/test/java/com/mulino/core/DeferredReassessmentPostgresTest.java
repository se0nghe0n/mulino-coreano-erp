package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.*;
import com.mulino.application.runtime.*;
import com.mulino.domain.definitions.DefinitionRepository;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.*;

/** Independent synthetic fixture: actual identity, policy, command transaction and PG, no mocked guards/facts. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
 properties="mulino.verification.instant=2026-10-07T09:00:02Z")
@ActiveProfiles({"local","verification"})
@SuppressWarnings({"unchecked","rawtypes"})
class DeferredReassessmentPostgresTest {
 private static final WorkPostgresTestResources OWNED=new WorkPostgresTestResources("deferred-reassessment");
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){OWNED.properties(r);}
 @Autowired AutowireCapableBeanFactory factory;
 @org.springframework.boot.test.web.server.LocalServerPort int port;
 @Autowired VerificationClock clock;
 @Autowired VerificationFaults faults;
 @Autowired VerificationScheduler scheduler;
 S2WorkActualIntegrationTest f;
 static final Instant NOW=Instant.parse("2026-10-07T09:00:02Z");
 static final List<String> CAPS=List.of("recordActivity","correctEvidence","assessGoal");

 @BeforeEach void setup()throws Exception{clock.resetForFixture(NOW);f=fixture();}
 S2WorkActualIntegrationTest fixture()throws Exception{
  var x=new S2WorkActualIntegrationTest(){@Override DomainContext context(){return new DomainContext(org,actor,actor,clock.instant(),clock.instant());}};factory.autowireBean(x);x.seed();
  // NEW synthetic permissions only; no original scenario fixture/grant is edited.
  for(String table:List.of("Memberships","Grants","CapabilityAssignments"))x.jdbc.update("UPDATE mulino_identity_"+table+" SET validFrom=?,validUntil=? WHERE organizationId=?",Timestamp.from(NOW.minusSeconds(86400)),Timestamp.from(NOW.plusSeconds(86400)),x.org);
  String grant=x.id();x.insert("mulino_identity_Grants",x.row("organizationId",x.org,"ID",grant,"actorId",x.actor,"delegatorId",x.actor,"validFrom",Timestamp.from(NOW.minusSeconds(60)),"validUntil",Timestamp.from(NOW.plusSeconds(3600))));
  x.insert("mulino_identity_GrantScopes",x.row("organizationId",x.org,"grantId",grant,"scopeKind","ORGANIZATION","scopeId",x.org));
  for(String cap:CAPS){x.insert("mulino_identity_GrantActions",x.row("organizationId",x.org,"grantId",grant,"capabilityId",cap));x.insert("mulino_identity_CapabilityAssignments",x.row("organizationId",x.org,"ID",x.id(),"actorId",x.actor,"capabilityId",cap,"scopeKind","ORGANIZATION","scopeId",x.org,"validFrom",Timestamp.from(NOW.minusSeconds(60)),"validUntil",Timestamp.from(NOW.plusSeconds(3600))));}
  var rules=new LinkedHashMap<String,Object>();for(String cap:CAPS)rules.put(cap,Map.of("effectClass",cap.equals("assessGoal")?"ASSESSMENT":"RECORD"));
  String policy=x.id(),content=x.json.writeValueAsString(Map.of("rules",rules));x.insert("mulino_governance_PolicyVersions",x.row("organizationId",x.org,"ID",policy,"version","defer-command-fixture","kind","COMMAND","content",content,"contentHash",DefinitionRepository.sha256(content),"createdAt",Timestamp.from(NOW.minusSeconds(60)),"effectiveFrom",Timestamp.from(NOW.minusSeconds(60))));x.jdbc.update("UPDATE mulino_governance_ActivePolicies SET policyId=?,revision=revision+1 WHERE organizationId=? AND kind='COMMAND'",policy,x.org);
  // Publish a separate immutable synthetic definition with the real command names.
  var old=x.jdbc.queryForMap("SELECT * FROM mulino_definitions_DefinitionVersions WHERE ID=?",x.definition);
  var d=x.json.readValue(old.get("content").toString(),Map.class);String definition=x.id();d.put("id",definition);d.put("version","1.1.0");
  var verbs=new ArrayList<Map<String,Object>>((List)d.get("verbs"));var capabilities=new ArrayList<Map<String,Object>>((List)d.get("capabilities"));
  for(String cap:CAPS){verbs.add(x.row("name",cap,"intentKind",cap.equals("assessGoal")?"COMMAND":"RECORD","capabilityId",cap,"stage",cap.equals("assessGoal")?"ASSESSMENT":"RECORD","slots",Map.of()));capabilities.add(x.row("capabilityId",cap,"semanticVersion","1.0.0","evaluatorVersion","core-v1","inputSchemaVersion","1.0.0","outputSchemaVersion","1.0.0","supportedWorkMigration",List.of()));}
  d.put("verbs",verbs);d.put("capabilities",capabilities);content=x.json.writeValueAsString(d);x.insert("mulino_definitions_DefinitionVersions",x.row("organizationId",x.org,"ID",definition,"createdAt",Timestamp.from(NOW.minusSeconds(1)),"version","1.1.0","state","PUBLISHED","contentHash",DefinitionRepository.sha256(content),"content",content,"evaluatorVersion","core-v1","schemaVersion","1.0.0"));x.definition=definition;return x;
 }
 String work(){String w=f.draft(f.goalInputs());f.run("activateWork",0,Map.of("workId",w));return w;}
 Map<String,Object> execute(String cap,String work,int rev,Map<String,Object> slots,String key){
  var input=f.row("intentKind",cap.equals("assessGoal")?"COMMAND":"RECORD","definitionVersion",f.definition,"capabilityId",cap,"capabilityVersion","1.0.0","subjectRefs",List.of(Map.of("type","Work","id",work)),"slots",slots,"provenance",Map.of(),"expectedRevision",rev,"commandIdempotencyKey",key);
  return f.runtime.requestContext().run(ctx->{return f.commands.execute(input);});
 }
 Map<String,Object> event(String work,String version,String previous,String quantity){var s=f.row("evidenceType","EVENT","subject",Map.of("kind","WORK","id",work),"kind","RECEIPT","sourceNamespace","native-fixture","externalEventId",work,"sourceVersion",version,"effectiveFrom",NOW.minusSeconds(1).toString(),"timeZone","UTC","timePrecision","SECOND","valueState","KNOWN","payload","{\"quantity\":\""+quantity+"\",\"unit\":\"BOX\"}");if(previous!=null)s.put("supersedesId",previous);return s;}
 String source(String w){var result=execute("recordActivity",w,0,event(w,"1",null,"100"),"event:"+w);assertEquals("APPLIED",result.get("outcome"),result.toString());return result.get("id").toString();}
 Map<String,Object> correct(String w,String source,String key){return execute("correctEvidence",w,1,event(w,"2",source,"98"),key);}
 Map<String,Object> arm(String org,String key){return faults.control(Map.of("operation","arm","organizationId",org,"point",VerificationFaults.DEFER_POINT,"capabilityId","correctEvidence","commandKey",key));}
 Map<String,Object> release(String org){return faults.control(Map.of("operation","release","organizationId",org,"point",VerificationFaults.DEFER_POINT));}
 int count(String table,String w){return f.jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE organizationId=? AND workId=?",Integer.class,f.org,w);}
 boolean pending(String w){return f.jdbc.queryForObject("SELECT pendingInvalidation FROM mulino_work_read_Works WHERE ID=?",Boolean.class,w);}
 void assess(String w){var r=execute("assessGoal",w,f.revision(w),Map.of("workId",w),"assess:"+w);assertEquals("APPLIED",r.get("outcome"),r.toString());}
 void tick(String id){scheduler.tick(Map.of("schedulerId",id));var tasks=(List<Map<String,Object>>)scheduler.records(id).get("tasks");assertEquals("SUCCEEDED",tasks.getLast().get("terminalStatus"),tasks.toString());}
 @AfterEach void clear(org.junit.jupiter.api.TestInfo info)throws Exception{
  try{String destination=System.getProperty("defer.evidence");if(destination!=null&&f!=null){var proof=new LinkedHashMap<String,Object>();proof.put("organizationId",f.org);proof.put("clock",clock.instant().toString());for(String table:List.of("mulino_evidence_Events","mulino_evaluation_InputSnapshots","mulino_work_read_AssessmentReferences","mulino_work_read_Works","mulino_runtime_RecoverySchedules","mulino_work_read_ObligationReferences","mulino_commands_CommandRecords"))proof.put(table,f.jdbc.queryForList("SELECT row_to_json(t)::text FROM "+table+" t WHERE organizationId=? ORDER BY ID",String.class,f.org));proof.put("scheduler",scheduler.records(null));proof.put("containerId",OWNED.pg.getContainerId());proof.put("publicKeyPath",OWNED.publicKeyPath.toString());proof.put("blobRoot",OWNED.blobRoot.toString());var path=java.nio.file.Path.of(destination);java.nio.file.Files.createDirectories(path);java.nio.file.Files.writeString(path.resolve(info.getTestMethod().orElseThrow().getName()+".json"),f.json.writerWithDefaultPrettyPrinter().writeValueAsString(proof));}}
  finally{SecurityContextHolder.clearContext();}
 }
 @AfterAll static void cleanup()throws Exception{OWNED.close();}

 @Test void fixedClockHttpCorrection98CommitsWithoutInventingAssessment()throws Exception{
  String w=work(),source=source(w);var original=f.jdbc.queryForMap("SELECT * FROM mulino_evidence_Events WHERE ID=?",source);
  var control=f.row("operation","arm","organizationId",f.org,"point",VerificationFaults.DEFER_POINT);
  assertEquals("ARMED",http("/__verification/runtime/fault",control,200).get("state"));
  var input=f.row("intentKind","RECORD","definitionVersion",f.definition,"capabilityId","correctEvidence","subjectRefs",List.of(Map.of("type","Work","id",w)),"slots",event(w,"2",source,"98"),"provenance",Map.of(),"expectedRevision",1,"commandIdempotencyKey","fixed-clock-http98");
  var result=http("/api/ontology/records/correctEvidence",input,200);assertEquals("APPLIED",result.get("outcome"),result.toString());assertTrue(pending(w));assertTrue(faults.held(f.org,w));assertEquals(NOW,clock.instant());
  assertEquals(original,f.jdbc.queryForMap("SELECT * FROM mulino_evidence_Events WHERE ID=?",source));var corrected=f.jdbc.queryForMap("SELECT * FROM mulino_evidence_Events WHERE supersedesId=?",source);assertEquals(original.get("recordedat"),corrected.get("recordedat"));assertEquals(2,corrected.get("revision"));assertEquals("98",f.json.readTree(corrected.get("payload").toString()).get("quantity").asText());
  assertEquals(2,count("mulino_evidence_Events",w));assertEquals(0,count("mulino_work_read_AssessmentReferences",w));assertEquals(0,count("mulino_evaluation_InputSnapshots",w));assertEquals(result,http("/api/ontology/records/correctEvidence",input,200));assertEquals(2,count("mulino_evidence_Events",w));
  assertEquals(false,http("/__verification/runtime/fault",f.row("operation","release","organizationId",UUID.randomUUID().toString(),"point",VerificationFaults.DEFER_POINT),422).get("accepted"));control.put("operation","release");assertEquals("RELEASED",http("/__verification/runtime/fault",control,200).get("state"));assertEquals("RELEASED",http("/__verification/runtime/fault",control,200).get("state"));
  System.out.println("FIXED_CLOCK_HTTP98 status=200 APPLIED eventRevisions=1,2 recordedAt="+NOW+" assessments=0 inputSnapshots=0 originalBytesPreserved=true replayEffects=0");
 }
 Map<String,Object> http(String path,Map<String,Object> body,int status)throws Exception{
  var encoder=Base64.getUrlEncoder().withoutPadding();var claims=f.row("iss","https://mulino.local.invalid","aud","mulino-platform","sub","writer-a","organizationId",f.org,"stableRequestOwner","UNTRUSTED-CLIENT-CLAIM","iat",Instant.now().getEpochSecond(),"exp",Instant.now().plusSeconds(120).getEpochSecond());
  String input=encoder.encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))+"."+encoder.encodeToString(f.json.writeValueAsBytes(claims));var signature=java.security.Signature.getInstance("SHA256withRSA");signature.initSign(OWNED.key.getPrivate());signature.update(input.getBytes(java.nio.charset.StandardCharsets.US_ASCII));String token=input+"."+encoder.encodeToString(signature.sign());
  var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json").header("Authorization","Bearer "+token).POST(java.net.http.HttpRequest.BodyPublishers.ofString(f.json.writeValueAsString(body))).build();var result=java.net.http.HttpClient.newHttpClient().send(request,java.net.http.HttpResponse.BodyHandlers.ofString());assertEquals(status,result.statusCode(),result.body());return f.json.readValue(result.body(),Map.class);
 }
 @Test void committedCorrectionHoldsOnlyMatchingOrganizationAndReleaseRunsActualScheduler()throws Exception{
  String w=work(),source=source(w);assess(w);var snapshots=f.jdbc.queryForList("SELECT * FROM mulino_evaluation_InputSnapshots WHERE workId=?",w);var assessments=f.jdbc.queryForList("SELECT * FROM mulino_work_read_AssessmentReferences WHERE workId=?",w);
  arm(f.org,"correct98");var result=correct(w,source,"correct98");assertEquals("APPLIED",result.get("outcome"),result.toString());assertTrue(pending(w));assertTrue(faults.held(f.org,w));
  assertEquals("COMMITTED",f.jdbc.queryForObject("SELECT state FROM mulino_commands_CommandRecords WHERE organizationId=? AND commandIdempotencyKey='correct98'",String.class,f.org));
  assertEquals("98",f.json.readTree(f.jdbc.queryForObject("SELECT payload FROM mulino_evidence_Events WHERE supersedesId=?",String.class,source)).get("quantity").asText());
  assertEquals(1,count("mulino_runtime_RecoverySchedules",w));assertEquals(true,f.jdbc.queryForObject("SELECT pendingAssessment FROM mulino_runtime_RecoverySchedules WHERE workId=?",Boolean.class,w));tick("held");assertEquals(1,count("mulino_work_read_AssessmentReferences",w));assertEquals(snapshots,f.jdbc.queryForList("SELECT * FROM mulino_evaluation_InputSnapshots WHERE workId=?",w));assertEquals(assessments,f.jdbc.queryForList("SELECT * FROM mulino_work_read_AssessmentReferences WHERE workId=?",w));
  assertThrows(IllegalArgumentException.class,()->release(UUID.randomUUID().toString()));
  var original=f;f=fixture();String unrelated=work(),otherSource=source(unrelated);assertEquals("APPLIED",correct(unrelated,otherSource,"unarmed-other-org").get("outcome"));tick("other-org");assertEquals(1,count("mulino_work_read_AssessmentReferences",unrelated),f.jdbc.queryForList("SELECT resultJson FROM mulino_commands_CommandRecords WHERE organizationId=?",f.org).toString()+f.jdbc.queryForList("SELECT * FROM mulino_runtime_RecoverySchedules WHERE organizationId=?",f.org));f=original;f.login();assertTrue(pending(w));
  assertEquals("RELEASED",release(f.org).get("state"));assertEquals("RELEASED",release(f.org).get("state"));tick("released");assertFalse(pending(w));assertEquals(2,count("mulino_work_read_AssessmentReferences",w));assertTrue(f.jdbc.queryForList("SELECT * FROM mulino_evaluation_InputSnapshots WHERE workId=?",w).containsAll(snapshots));assertTrue(f.jdbc.queryForList("SELECT * FROM mulino_work_read_AssessmentReferences WHERE workId=?",w).containsAll(assessments));assertEquals(2,count("mulino_evidence_Events",w));assertEquals(0,count("mulino_evidence_CanonicalOccurrences",w));assertEquals(0,count("mulino_trade_receipt_Receipts",w));assertEquals(0,f.jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_QuantityMovements WHERE organizationId=?",Integer.class,f.org));
 }
 @Test void releasedReassessmentStillRequiresCurrentAuthority()throws Exception{
  String w=work(),source=source(w);arm(f.org,"revoke98");assertEquals("APPLIED",correct(w,source,"revoke98").get("outcome"));
  f.jdbc.update("DELETE FROM mulino_identity_GrantActions WHERE organizationId=? AND capabilityId='assessGoal'",f.org);release(f.org);tick("revoked");assertTrue(pending(w));assertEquals(0,count("mulino_work_read_AssessmentReferences",w));assertEquals(2,count("mulino_evidence_Events",w));assertEquals("CURRENT_AUTHORITY_DENIED",f.jdbc.queryForObject("SELECT lastOutcome FROM mulino_runtime_RecoverySchedules WHERE organizationId=? AND workId=?",String.class,f.org,w));assertEquals(0,f.jdbc.queryForObject("SELECT count(*) FROM mulino_commands_CommandRecords WHERE organizationId=? AND capabilityId='assessGoal'",Integer.class,f.org));
 }
 @Test void rollbackAfterCheckpointLeavesNoCorrectionPendingDutyOrRelease()throws Exception{
  String w=work(),source=source(w);arm(f.org,"rollback98");faults.control(Map.of("operation","arm","organizationId",f.org,"point","command.beforeCommit","capabilityId","correctEvidence","commandKey","rollback98"));
  assertThrows(VerificationFaults.Injected.class,()->correct(w,source,"rollback98"));assertFalse(pending(w));assertFalse(faults.held(f.org,w));assertEquals(1,count("mulino_evidence_Events",w));assertEquals(0,count("mulino_work_read_ObligationReferences",w));assertThrows(IllegalArgumentException.class,()->release(f.org));
 }
 }
