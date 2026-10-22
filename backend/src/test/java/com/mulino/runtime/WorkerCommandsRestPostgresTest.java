package com.mulino.runtime;

import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.runtime.*;
import com.mulino.application.core.*;
import com.mulino.domain.definitions.*;
import com.sap.cds.services.runtime.CdsRuntime;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

/** Real HTTP/JWT/CAP/PostgreSQL worker boundary; all policy and data are synthetic. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local","verification"})
class WorkerCommandsRestPostgresTest {
 static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
 static final Instant T=Instant.parse("2026-10-07T09:00:00Z");static final Path KEY;static final java.security.KeyPair KEYPAIR;static final Path BLOBS;
 static{try{BLOBS=Files.createTempDirectory("runtime-intake-blobs-",java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));PG.start();var generator=java.security.KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KEYPAIR=generator.generateKeyPair();KEY=Files.createTempFile("runtime-intake-public-",".pem");Files.writeString(KEY,"-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(KEYPAIR.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----\n");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 @DynamicPropertySource static void props(DynamicPropertyRegistry r){r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);r.add("JWT_PUBLIC_KEY",KEY::toString);r.add("JWT_ISSUER",()->"https://runtime-fixture.invalid");r.add("JWT_AUDIENCE",()->"mulino-test");r.add("mulino.verification.instant",()->T.toString());r.add("mulino.evidence.blob-root",BLOBS::toString);}
 @Autowired JdbcTemplate db;@Autowired RuntimeIntakeProcessor processor;@Autowired CdsRuntime cds;@Autowired VerificationClock clock;@Autowired RuntimeService runtime;@Autowired ApplicationCommands commands;
 String org,actor,stableOwner,item,definition,work,profile,grant,event,inbox;ObjectMapper json=new ObjectMapper();
 static String id(){return UUID.randomUUID().toString();}
 static Map<String,Object> map(Object... pairs){var result=new LinkedHashMap<String,Object>();for(int n=0;n<pairs.length;n+=2)result.put(pairs[n].toString(),pairs[n+1]);return result;}
 void insert(String table,Map<String,Object> values){db.update("INSERT INTO "+table+"("+String.join(",",values.keySet())+") VALUES("+String.join(",",Collections.nCopies(values.size(),"?"))+")",values.values().toArray());}
 Map<String,Object> scoped(String id){return map("organizationId",org,"ID",id,"revision",0,"createdAt",Timestamp.from(T.minusSeconds(10)),"recordedAt",Timestamp.from(T.minusSeconds(10)));}
 Map<String,Object> workRow(String id){var r=scoped(id);r.put("effectiveAt",Timestamp.from(T));return r;}
 void seed(){
  clock.resetForFixture(T);
  org=id();actor=id();stableOwner=id();item=id();definition=id();work=id();profile=id();grant=id();event=id();inbox=id();
  insert("mulino_identity_Organizations",map("ID",org,"externalAlias",org));insert("mulino_identity_Actors",map("organizationId",org,"ID",actor,"kind","HUMAN","stableRequestOwner",stableOwner));
  insert("mulino_identity_AuthorityFences",map("organizationId",org,"actorId",actor,"revision",1));
  insert("mulino_identity_Memberships",map("organizationId",org,"ID",id(),"actorId",actor,"validFrom",Timestamp.from(T.minusSeconds(100)),"validUntil",Timestamp.from(T.plusSeconds(3600))));
  insert("mulino_identity_Grants",map("organizationId",org,"ID",grant,"actorId",actor,"delegatorId",actor,"validFrom",Timestamp.from(T.minusSeconds(100)),"validUntil",Timestamp.from(T.plusSeconds(3600))));
  insert("mulino_identity_GrantScopes",map("organizationId",org,"grantId",grant,"scopeKind","ORGANIZATION","scopeId",org));
  for(String cap:List.of("createObligation","createFollowup"))insert("mulino_identity_CapabilityAssignments",map("organizationId",org,"ID",id(),"actorId",actor,"capabilityId",cap,"scopeKind","ORGANIZATION","scopeId",org,"validFrom",Timestamp.from(T.minusSeconds(100)),"validUntil",Timestamp.from(T.plusSeconds(3600))));
  insert("mulino_identity_GrantActions",map("organizationId",org,"grantId",grant,"capabilityId","createFollowup"));
  String product=id(),spec=id(),pack=id();var productRow=scoped(product);productRow.put("name","runtime fixture");insert("mulino_inventory_Products",productRow);
  for(String table:List.of("SpecificationVersions","PackagingVersions")){var row=scoped(table.startsWith("Spec")?spec:pack);row.putAll(map("productId",product,"version","1","contentHash","a".repeat(64)));insert("mulino_inventory_"+table,row);}
  var itemRow=scoped(item);itemRow.putAll(map("productId",product,"name","fixture","baseUnit","BOX","decimalPlaces",0,"specificationVersionId",spec,"packagingVersionId",pack));insert("mulino_inventory_TradeItems",itemRow);
  var predicate=Map.<String,Object>of("operator","quantitySum","property","Receipt.quantity","minimum",Map.of("value","1","unit","BOX"),"unit","BOX","evidenceSelector","VERIFIED_DISTINCT");
  var d=new Definition(org,definition,"1.0.0",null,"PUBLISHED","","core-v1","1.0.0",List.of(new Definition.NounType("Receipt",true),new Definition.NounType("Work",true)),List.of(new Definition.Attribute("Receipt","quantity",Definition.ValueType.DECIMAL,null,"BOX",0,1,1,"READ",true)),List.of(new Definition.Verb("FixtureExternalEffect","COMMAND","fixtureExternalEffect","COMMIT",Map.of("workId","Work"))),List.of(),List.of(new Definition.Goal("Arrival","CUMULATIVE_EVENT","ARRIVED","core-v1",predicate)),List.of(new Definition.Capability("fixtureExternalEffect","1.0.0","core-v1","1.0.0","1.0.0",List.of())));String content=json.writeValueAsString(d);
  insert("mulino_definitions_DefinitionVersions",map("organizationId",org,"ID",definition,"createdAt",Timestamp.from(T.minusSeconds(100)),"version","1.0.0","state","PUBLISHED","contentHash",DefinitionRepository.sha256(content),"content",content,"evaluatorVersion","core-v1","schemaVersion","1.0.0"));
  var source=workRow(work);source.putAll(map("itemId",item,"definitionVersionId",definition,"kind","PURCHASE","status","CLOSED","closeReason","CANCELLED","lifecycleMode","COMMAND","ownerId",actor,"supervisorId",actor));insert("mulino_work_read_Works",source);
  String goalId=id();var goal=workRow(goalId);goal.putAll(map("workId",work,"definitionVersionId",definition,"quantityMode","CUMULATIVE_EVENT","targetQuantity",new java.math.BigDecimal("100"),"unit","BOX","endpoint","ARRIVED","scopeJson","{}","slotsJson",json.writeValueAsString(goalSlots()),"provenanceJson","{}","goalVersion",1));insert("mulino_work_read_GoalReferences",goal);db.update("UPDATE mulino_work_read_Works SET currentGoalVersionId=? WHERE organizationId=? AND ID=?",goalId,org,work);
  insert("mulino_evidence_SourceProfiles",map("organizationId",org,"ID",profile,"revision",1,"recordedAt",Timestamp.from(T.minusSeconds(10)),"recordedBy",actor,"namespace",profile,"policyVersion","fixture-v1","intakeOwnerId",actor,"supervisorId",actor,"nextAction","Review temperature evidence","nextCheckAt",Timestamp.from(T)));
  policy("EVIDENCE",json.writeValueAsString(Map.of("intakeRules",Map.of("TEMPERATURE_ANOMALY",Map.of("requiresResponse",true),"NORMAL",Map.of("requiresResponse",false)))));
  policy("COMMAND",json.writeValueAsString(Map.of("rules",Map.of("createObligation",Map.of("effectClass","RESPONSIBILITY")))));
  observation("TEMPERATURE_ANOMALY");
 }
 Map<String,Object> goalSlots(){return map("quantityMode","CUMULATIVE_EVENT","targetQuantity","100","unit","BOX","endpoint","ARRIVED","dueAt","2026-10-31T00:00:00Z","scope",Map.of("itemId",item),"timezone","Asia/Seoul","evidencePolicyVersion","fixture-v1","periodStart","2026-10-01T00:00:00Z","periodEnd","2026-10-31T00:00:00Z","periodStartInclusive",true,"periodEndInclusive",false,"eventKind","RECEIPT","contributionScope",Map.of("itemId",item),"deduplication","CANONICAL_OCCURRENCE","conditions",List.of(Map.of("id","Arrival")),"evaluatorVersion","core-v1");}
 void policy(String kind,String content){String id=id();insert("mulino_governance_PolicyVersions",map("organizationId",org,"ID",id,"kind",kind,"version","fixture-v1","content",content,"contentHash",DefinitionRepository.sha256(content),"createdAt",Timestamp.from(T.minusSeconds(100)),"effectiveFrom",Timestamp.from(T.minusSeconds(100))));insert("mulino_governance_ActivePolicies",map("organizationId",org,"kind",kind,"policyId",id,"revision",1));}
 void observation(String kind){insert("mulino_evidence_Events",map("organizationId",org,"ID",event,"revision",1,"recordedAt",Timestamp.from(T.minusSeconds(1)),"recordedBy",actor,"subjectKind","WORK","subjectId",work,"itemId",item,"workId",work,"effectiveFrom",Timestamp.from(T.minusSeconds(1)),"timeZone","UTC","timePrecision","SECOND","valueState","KNOWN","kind",kind,"sourceNamespace",profile,"sourceProfileId",profile,"externalEventId",event,"sourceVersion","1","payloadHash",DefinitionRepository.sha256("{}"),"payload","{}"));insert("mulino_evidence_InboxRecords",map("organizationId",org,"ID",inbox,"revision",1,"recordedAt",Timestamp.from(T.minusSeconds(1)),"recordedBy",actor,"subjectKind","WORK","subjectId",work,"itemId",item,"workId",work,"eventId",event,"sourceNamespace",profile,"sourceProfileId",profile,"externalEventId",event,"sourceVersion","1","payloadHash",DefinitionRepository.sha256("{}"),"state","RECEIVED","intakeOwnerId",actor,"supervisorId",actor,"nextAction","Review temperature evidence","nextCheckAt",Timestamp.from(T)));}
 int tick(){return cds.requestContext().run(c->{return processor.recoverDue();});}
 @org.springframework.boot.test.web.server.LocalServerPort int port;
 Map<String,Object> claim,intent;
 @BeforeEach void workerFixture(){
  seed();
  insert("mulino_identity_GrantActions",map("organizationId",org,"grantId",grant,"capabilityId","createObligation"));tick();String linked=db.queryForObject("SELECT linkedWorkId FROM mulino_runtime_IntakeRecoveries WHERE ID=?",String.class,inbox);
  insert("mulino_identity_GrantActions",map("organizationId",org,"grantId",grant,"capabilityId","fixtureExternalEffect"));insert("mulino_identity_CapabilityAssignments",map("organizationId",org,"ID",id(),"actorId",actor,"capabilityId","fixtureExternalEffect","scopeKind","ORGANIZATION","scopeId",org,"validFrom",Timestamp.from(T.minusSeconds(100)),"validUntil",Timestamp.from(T.plusSeconds(3600))));
  String policy=id(),content=json.writeValueAsString(Map.of("rules",Map.of("fixtureExternalEffect",Map.of("effectClass","EXTERNAL_FIXTURE"))));insert("mulino_governance_PolicyVersions",map("organizationId",org,"ID",policy,"kind","COMMAND","version","fixture-v2","content",content,"contentHash",DefinitionRepository.sha256(content),"createdAt",Timestamp.from(T.minusSeconds(100)),"effectiveFrom",Timestamp.from(T.minusSeconds(100))));db.update("UPDATE mulino_governance_ActivePolicies SET policyId=?,revision=revision+1 WHERE organizationId=? AND kind='COMMAND'",policy,org);
  var c=new DomainContext(org,actor,stableOwner,clock.instant(),clock.instant());claim=cds.requestContext().run(ctx->{return runtime.claim(c,linked,"fixtureExternalEffect","external-fixture-once","fixture-producer",Duration.ofSeconds(5));}).orElseThrow();
  intent=map("intentKind","COMMAND","capabilityId","fixtureExternalEffect","capabilityVersion","1.0.0","definitionVersion",definition,"commandIdempotencyKey","external-fixture-once","expectedRevision",db.queryForObject("SELECT revision FROM mulino_work_read_Works WHERE ID=?",Integer.class,linked),"subjectRefs",List.of(Map.of("type","Work","id",linked)),"slots",Map.of("workId",Map.of("type","WORK","id",linked),"dropResponse",Map.of("type","BOOLEAN","value",true)),"provenance",Map.of("workId","CONTEXT","dropResponse","CONTEXT"));
  insert("mulino_identity_ExternalIdentities",map("organizationId",org,"ID",id(),"actorId",actor,"issuer","https://runtime-fixture.invalid","subject",actor,"organizationAlias",org));
 }
 Map<String,Object> reference(){return map("claimId",claim.get("attemptId"),"fencingToken",claim.get("leaseToken"));}
 String token(String subject,String organization)throws Exception{
  long now=Instant.now().getEpochSecond();var encoder=Base64.getUrlEncoder().withoutPadding();
  String header=encoder.encodeToString(json.writeValueAsBytes(Map.of("alg","RS256","typ","JWT")));
  String payload=encoder.encodeToString(json.writeValueAsBytes(map("iss","https://runtime-fixture.invalid","aud",List.of("mulino-test"),"sub",subject,"organizationId",organization,"stableRequestOwner","payload-cannot-choose-owner","iat",now,"nbf",now-5,"exp",now+3600)));
  String body=header+"."+payload;var signer=java.security.Signature.getInstance("SHA256withRSA");signer.initSign(KEYPAIR.getPrivate());signer.update(body.getBytes(java.nio.charset.StandardCharsets.US_ASCII));return body+"."+encoder.encodeToString(signer.sign());
 }
 java.net.http.HttpResponse<String> post(Map<String,Object> body,String bearer)throws Exception{
  var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:"+port+"/api/runtime/worker/commands")).header("Content-Type","application/json");
  if(bearer!=null)request.header("Authorization","Bearer "+bearer);
  return java.net.http.HttpClient.newHttpClient().send(request.POST(java.net.http.HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),java.net.http.HttpResponse.BodyHandlers.ofString());
 }
 int outbox(){return db.queryForObject("SELECT count(*) FROM mulino_runtime_Outbox WHERE organizationId=?",Integer.class,org);}
 int committed(){return db.queryForObject("SELECT count(*) FROM mulino_commands_CommandRecords WHERE organizationId=? AND capabilityId='fixtureExternalEffect' AND state='COMMITTED'",Integer.class,org);}
 void forbidden(Map<String,Object> body,String bearer)throws Exception{
  int before=outbox(),commandsBefore=committed();var domainBefore=domainRows();
  var response=post(body,bearer);assertEquals(403,response.statusCode(),response.body());
  var result=json.readValue(response.body(),Map.class);assertEquals("REJECTED",result.get("outcome"));assertEquals("FORBIDDEN",((Map<?,?>)result.get("error")).get("code"));assertEquals(Map.of(),result.get("effects"));
  assertEquals(before,outbox());assertEquals(commandsBefore,committed());assertEquals(domainBefore,domainRows());
 }
 @Test void authenticatedClaimCommitsExactlyOnceAndReplaysSameResult()throws Exception{
  var body=map("intent",intent,"claim",reference());String bearer=token(actor,org);
  var first=post(body,bearer);assertEquals(200,first.statusCode(),first.body());
  var result=json.readValue(first.body(),Map.class);assertEquals("ACCEPTED_PENDING_EXTERNAL",result.get("outcome"));
  var second=post(body,bearer);assertEquals(200,second.statusCode(),second.body());assertEquals(result,json.readValue(second.body(),Map.class));
  assertEquals(1,outbox());assertEquals(1,committed());
  db.update("UPDATE mulino_identity_Grants SET revokedAt=? WHERE organizationId=? AND ID=?",Timestamp.from(T),org,grant);
  forbidden(body,bearer);assertEquals(1,outbox());assertEquals(1,committed());
 }
 @Test void missingForgedMalformedAndForeignClaimsHaveNoEffects()throws Exception{
  String bearer=token(actor,org);
  forbidden(map("intent",intent),bearer);
  for(var ref:List.of(Map.of(),map("claimId",id(),"fencingToken",1),map("claimId",claim.get("attemptId"),"fencingToken",2),map("claimId","invalid","fencingToken",1),map("claimId",claim.get("attemptId"),"fencingToken",1.5),map("claimId",claim.get("attemptId"),"fencingToken",1,"actorId",actor)))forbidden(map("intent",intent,"claim",ref),bearer);
  forbidden(map("intent",intent,"claim",reference()),token(actor,id()));
  String other=id(),owner=id();insert("mulino_identity_Actors",map("organizationId",org,"ID",other,"kind","HUMAN","stableRequestOwner",owner));
  insert("mulino_identity_Memberships",map("organizationId",org,"ID",id(),"actorId",other,"validFrom",Timestamp.from(T.minusSeconds(100)),"validUntil",Timestamp.from(T.plusSeconds(3600))));
  insert("mulino_identity_ExternalIdentities",map("organizationId",org,"ID",id(),"actorId",other,"issuer","https://runtime-fixture.invalid","subject",other,"organizationAlias",org));
  forbidden(map("intent",intent,"claim",reference()),token(other,org));assertEquals(0,outbox());
 }
 @Test void expiredAndReplacedClaimsHaveNoEffects()throws Exception{
  var old=reference();clock.advance(T.plusSeconds(5));String bearer=token(actor,org);
  forbidden(map("intent",intent,"claim",old),bearer);
  var c=new DomainContext(org,actor,stableOwner,clock.instant(),clock.instant());
  var replacement=cds.requestContext().run(ctx->{return runtime.claim(c,claim.get("workId").toString(),"fixtureExternalEffect","external-fixture-once","replacement",Duration.ofSeconds(5));}).orElseThrow();
  forbidden(map("intent",intent,"claim",old),bearer);
  claim=replacement;assertEquals(200,post(map("intent",intent,"claim",reference()),bearer).statusCode());assertEquals(1,outbox());
 }
 @Test void revokedGrantAndMismatchedCommandHaveNoEffects()throws Exception{
  String bearer=token(actor,org);var wrong=new LinkedHashMap<>(intent);wrong.put("commandIdempotencyKey","different-command");
  forbidden(map("intent",wrong,"claim",reference()),bearer);
  db.update("UPDATE mulino_identity_Grants SET revokedAt=? WHERE organizationId=? AND ID=?",Timestamp.from(T),org,grant);
  forbidden(map("intent",intent,"claim",reference()),bearer);assertEquals(0,outbox());
 }
 @Test void anonymousSubmissionIsRejectedBySecurityFilter()throws Exception{
  var response=post(map("intent",intent,"claim",reference()),null);assertEquals(401,response.statusCode());assertEquals(0,outbox());assertEquals(0,committed());
 }
 Map<String,Object> domainRows(){
  var snapshot=new LinkedHashMap<String,Object>();
  for(String table:List.of("mulino_work_read_Works","mulino_work_read_ObligationReferences","mulino_responsibility_Roots","mulino_runtime_ExecutionAttempts"))
   snapshot.put(table,db.queryForList("SELECT * FROM "+table+" WHERE organizationId=? ORDER BY ID",org));
  return snapshot;
 }
 @Test void revokedMembershipAndChangedStableOwnerCannotBorrowClaim()throws Exception{
  String bearer=token(actor,org);var body=map("intent",intent,"claim",reference());
  db.update("UPDATE mulino_identity_Actors SET stableRequestOwner=? WHERE organizationId=? AND ID=?",id(),org,actor);
  forbidden(body,bearer);
  db.update("UPDATE mulino_identity_Actors SET stableRequestOwner=? WHERE organizationId=? AND ID=?",stableOwner,org,actor);
  db.update("UPDATE mulino_identity_Memberships SET revokedAt=? WHERE organizationId=? AND actorId=?",Timestamp.from(T),org,actor);
  forbidden(body,bearer);
 }
 @Test void readOnlyGrantDoesNotAuthorizeWorkerWrite()throws Exception{
  db.update("DELETE FROM mulino_identity_GrantActions WHERE organizationId=? AND grantId=?",org,grant);
  insert("mulino_identity_GrantActions",map("organizationId",org,"grantId",grant,"capabilityId","getWork"));
  forbidden(map("intent",intent,"claim",reference()),token(actor,org));
 }
 @AfterAll static void cleanup()throws Exception{PG.stop();Files.deleteIfExists(KEY);try(var paths=Files.walk(BLOBS)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
}
