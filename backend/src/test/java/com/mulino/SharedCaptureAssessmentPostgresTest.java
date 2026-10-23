package com.mulino;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.mulino.application.evaluation.AssessmentService;
import com.mulino.domain.definitions.*;
import com.mulino.adapters.blob.LocalBlobStore;
import com.sap.cds.services.runtime.CdsRuntime;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Actual CQN/PostgreSQL assessment against an independent SQL physical-scope oracle. */
@SpringBootTest @ActiveProfiles("local")
class SharedCaptureAssessmentPostgresTest {
 @org.springframework.boot.test.context.TestConfiguration static class FixedTime {
  @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary ExecutionClock fixedEvaluationClock(){return new ExecutionClock(Clock.fixed(Instant.parse("2026-10-08T00:10:00Z"),ZoneOffset.UTC));}
 }
 static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
 static final Path KEY,BLOBS;
 static {try{PG.start();var g=java.security.KeyPairGenerator.getInstance("RSA");g.initialize(2048);KEY=Files.createTempFile("eval-public-",".pem");Files.writeString(KEY,"-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(g.generateKeyPair().getPublic().getEncoded())+"\n-----END PUBLIC KEY-----\n");BLOBS=Files.createTempDirectory("eval-blobs-");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 @DynamicPropertySource static void props(DynamicPropertyRegistry r){r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);r.add("JWT_PUBLIC_KEY",KEY::toString);r.add("JWT_ISSUER",()->"https://evaluation.invalid");r.add("JWT_AUDIENCE",()->"evaluation");r.add("mulino.evidence.blob-root",BLOBS::toString);}
 @Autowired ApplicationQueries queries;
 @Autowired ApplicationCommands commands;
 @Autowired DefinitionValidator definitionValidator;
 @Autowired com.mulino.domain.inventory.StockPrimitives stock;
 @Autowired com.mulino.application.evidence.EvidenceRecords records;
 @Autowired com.mulino.application.evidence.EvidenceReconciliation reconciliation;
 @Autowired com.mulino.application.identity.IdentityAuthorization auth;
 @Autowired JdbcTemplate jdbc;@Autowired AssessmentService service;@Autowired LocalBlobStore blobs;@Autowired CdsRuntime runtime;@Autowired PlatformTransactionManager tx;
 final ObjectMapper json=new ObjectMapper();final Instant t=Instant.parse("2026-10-08T00:00:00Z");String org,actor,item,definition,work,goal,profile,policy;
 String id(){return UUID.randomUUID().toString();}
 Map<String,Object> row(Object... kv){var r=new LinkedHashMap<String,Object>();for(int i=0;i<kv.length;i+=2)r.put(kv[i].toString(),kv[i+1]);return r;}
 void insert(String table,Map<String,Object> r){jdbc.update("INSERT INTO "+table+" ("+String.join(",",r.keySet())+") VALUES ("+String.join(",",Collections.nCopies(r.size(),"?"))+")",r.values().toArray());}
 Map<String,Object> scoped(String id){return row("organizationId",org,"ID",id,"revision",0,"createdAt",Timestamp.from(t.minusSeconds(100)),"recordedAt",Timestamp.from(t.minusSeconds(100)));}
 Map<String,Object> evidence(String id){var r=scoped(id);r.put("revision",1);r.put("recordedBy",actor);return r;}
 Map<String,Object> slots(){return row("quantityMode","CUMULATIVE_EVENT","targetQuantity","100","unit","BOX","endpoint","ARRIVED","scope",Map.of("workId",work),"timezone","UTC","evidencePolicyVersion","fixture-evidence-v1","periodStart",t.minusSeconds(100).toString(),"periodEnd",t.plusSeconds(200).toString(),"eventKind","RECEIPT","contributionScope","DIRECT","deduplication","PHYSICAL_SCOPE","conditions",List.of(Map.of("id","Arrival")),"evaluatorVersion","core-v1","dueAt",t.toString());}
 @BeforeEach void seed()throws Exception{
  org=id();actor=id();item=id();definition=id();work=id();goal=id();profile=id();policy=id();String product=id(),spec=id(),pack=id();
  insert("mulino_identity_Organizations",row("ID",org,"externalAlias",org));insert("mulino_identity_Actors",row("organizationId",org,"ID",actor,"kind","HUMAN","stableRequestOwner",actor));
  var p=scoped(product);p.put("name","fixture");insert("mulino_inventory_Products",p);
  for(var e:Map.of("SpecificationVersions",spec,"PackagingVersions",pack).entrySet()){var r=scoped(e.getValue());r.putAll(row("productId",product,"version","1","contentHash","a".repeat(64)));insert("mulino_inventory_"+e.getKey(),r);}
  var i=scoped(item);i.putAll(row("productId",product,"name","fixture","baseUnit","BOX","decimalPlaces",0,"specificationVersionId",spec,"packagingVersionId",pack));insert("mulino_inventory_TradeItems",i);
  var predicate=Map.<String,Object>of("operator","quantitySum","property","Receipt.quantity","minimum",Map.of("value","1","unit","BOX"),"unit","BOX","evidenceSelector","VERIFIED_DISTINCT");
  var d=new Definition(org,definition,"fixture-v1",null,"PUBLISHED",null,"core-v1","1.0.0",List.of(new Definition.NounType("Receipt",true)),List.of(new Definition.Attribute("Receipt","quantity",Definition.ValueType.DECIMAL,null,"BOX",0,1,1,"READ",true)),List.of(),List.of(),List.of(new Definition.Goal("Arrival","CUMULATIVE_EVENT","ARRIVED","core-v1",predicate)),List.of(new Definition.Capability("getAssessment","1.0.0","core-v1","1.0.0","1.0.0",List.of()),new Definition.Capability("assessGoal","1.0.0","core-v1","1.0.0","1.0.0",List.of())));String content=json.writeValueAsString(d);
  insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",definition,"version","fixture-v1","state","PUBLISHED","contentHash",DefinitionRepository.sha256(content),"content",content,"evaluatorVersion","core-v1","schemaVersion","1.0.0","createdAt",Timestamp.from(t.minusSeconds(100))));
  var w=scoped(work);w.putAll(row("effectiveAt",Timestamp.from(t.minusSeconds(100)),"itemId",item,"definitionVersionId",definition,"kind","PURCHASE","status","ACTIVE","ownerId",actor,"supervisorId",actor,"lifecycleMode","COMMAND"));insert("mulino_work_read_Works",w);
  var g=scoped(goal);g.putAll(row("effectiveAt",Timestamp.from(t.minusSeconds(100)),"workId",work,"definitionVersionId",definition,"quantityMode","CUMULATIVE_EVENT","targetQuantity",new BigDecimal("100"),"unit","BOX","endpoint","ARRIVED","scopeJson","{}","slotsJson",json.writeValueAsString(slots())));insert("mulino_work_read_GoalReferences",g);jdbc.update("UPDATE mulino_work_read_Works SET currentGoalVersionId=? WHERE organizationId=? AND ID=?",goal,org,work);
  insert("mulino_governance_PolicyVersions",row("organizationId",org,"ID",policy,"version","fixture-evidence-v1","kind","EVIDENCE","content","{}","contentHash",DefinitionRepository.sha256("{}"),"createdAt",LocalDateTime.ofInstant(t.minusSeconds(100),ZoneOffset.UTC),"effectiveFrom",LocalDateTime.ofInstant(t.minusSeconds(100),ZoneOffset.UTC)));
  if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass('mulino_governance_activepolicies') IS NOT NULL",Boolean.class)))jdbc.update("INSERT INTO mulino_governance_ActivePolicies(organizationId,kind,policyId,revision) VALUES(?,'EVIDENCE',?,0)",org,policy);
  var s=evidence(profile);s.putAll(row("namespace","fixture","policyVersion","fixture-evidence-v1","intakeOwnerId",actor,"supervisorId",actor,"nextAction","compare","nextCheckAt",Timestamp.from(t.plusSeconds(3600))));insert("mulino_evidence_SourceProfiles",s);
 }
 void occurrence(String physical,String quantity,Instant occurred,String source){occurrence(physical,quantity,occurred,source,null,null);}
 void occurrence(String physical,String quantity,Instant occurred,String source,String place,Instant until){occurrence(physical,quantity,occurred,source,place,until,"RECEIPT",true);}
 void occurrence(String physical,String quantity,Instant occurred,String source,String place,Instant until,String kind,boolean verified){
  String subjectKind=kind.equals("STATE_OBSERVATION")?"SEGMENT":"WORK",subjectId=kind.equals("STATE_OBSERVATION")?physical:work;
  Instant recorded=kind.equals("STATE_OBSERVATION")?(until==null?occurred:until):t.minusSeconds(100);
  String event=id(),claim=id(),canonical=id(),doc=id();byte[] content=(source+quantity).getBytes();var blob=blobs.stage(content,LocalBlobStore.hash(content));blobs.commit(blob);
  var document=evidence(doc);document.put("recordedAt",Timestamp.from(recorded));document.putAll(row("subjectKind",subjectKind,"subjectId",subjectId,"itemId",item,"workId",work,"sha256",blob.sha256(),"blobId",blob.id().toString(),"byteLength",blob.size(),"mediaType","text/plain","sourceNamespace","fixture","sourceProfileId",profile,"availability","AVAILABLE","provenance","{}"));insert("mulino_evidence_DocumentVersions",document);
  var e=evidence(event);e.put("recordedAt",Timestamp.from(recorded));e.putAll(row("subjectKind",subjectKind,"subjectId",subjectId,"itemId",item,"workId",work,"effectiveFrom",Timestamp.from(occurred),"timeZone","UTC","timePrecision","SECOND","valueState","KNOWN","kind",kind,"sourceNamespace","fixture","sourceProfileId",profile,"externalEventId",source,"sourceVersion","1","payloadHash",LocalBlobStore.hash(content),"payload","{}"));if(until!=null)e.put("effectiveUntil",Timestamp.from(until));insert("mulino_evidence_Events",e);
  var in=evidence(id());in.put("recordedAt",Timestamp.from(recorded));in.putAll(row("subjectKind",subjectKind,"subjectId",subjectId,"itemId",item,"workId",work,"eventId",event,"sourceNamespace","fixture","sourceProfileId",profile,"externalEventId",source,"sourceVersion","1","payloadHash",LocalBlobStore.hash(content),"state","RECEIVED","intakeOwnerId",actor,"supervisorId",actor,"nextAction","compare","nextCheckAt",Timestamp.from(t.plusSeconds(3600))));insert("mulino_evidence_InboxRecords",in);
  var c=evidence(claim);c.put("recordedAt",Timestamp.from(recorded));c.putAll(row("sourceProfileId",profile,"subjectKind",subjectKind,"subjectId",subjectId,"itemId",item,"workId",work,"effectiveFrom",Timestamp.from(occurred),"timeZone","UTC","timePrecision","SECOND","valueState","KNOWN","eventId",event,"documentVersionId",doc,"assertion","receipt","quantity",new BigDecimal(quantity),"unit","BOX"));if(until!=null)c.put("effectiveUntil",Timestamp.from(until));insert("mulino_evidence_Claims",c);
  var o=evidence(canonical);o.put("recordedAt",Timestamp.from(recorded));o.putAll(row("sourceProfileId",profile,"subjectKind",subjectKind,"subjectId",subjectId,"itemId",item,"workId",work,"effectiveFrom",Timestamp.from(occurred),"timeZone","UTC","timePrecision","SECOND","valueState","KNOWN","kind",kind,"physicalScopeId",physical,"quantity",new BigDecimal(quantity),"unit","BOX","reassessmentState","PENDING"));if(place!=null)o.put("placeId",place);if(until!=null)o.put("effectiveUntil",Timestamp.from(until));var existing=jdbc.queryForList("SELECT ID FROM mulino_evidence_CanonicalOccurrences WHERE organizationId=? AND physicalScopeId=? AND kind=? AND effectiveFrom=?",String.class,org,physical,kind,Timestamp.from(occurred));if(existing.isEmpty())insert("mulino_evidence_CanonicalOccurrences",o);else canonical=existing.getFirst();
  var v=evidence(id());v.put("recordedAt",Timestamp.from(recorded));v.putAll(row("claimId",claim,"canonicalOccurrenceId",canonical,"basisDocumentId",doc,"policyVersion","fixture-evidence-v1","sourceMatched",true,"identityMatched",true,"quantityMatched",true,"timeMatched",true,"duplicateChecked",true,"verdict",verified?"VERIFIED":"UNKNOWN","reason","fixture"));insert("mulino_evidence_Verifications",v);
 }
 Map<String,Object> assess(Instant at){return runtime.requestContext().run(c->{return new TransactionTemplate(tx).execute(s->service.assess(new DomainContext(org,actor,actor,at,t.plusSeconds(1000)),work));});}
 String physicalTwenty(){
  String place=id(),manufacturer=id(),lot=id(),segment=id();
  var p=scoped(place);p.putAll(row("name","W","kind","INTERNAL_STORAGE"));insert("mulino_inventory_Places",p);
  var m=scoped(manufacturer);m.put("name","fixture");insert("mulino_inventory_Manufacturers",m);
  var l=scoped(lot);l.putAll(row("manufacturerId",manufacturer,"itemId",item,"originalLot","L"));insert("mulino_inventory_ManufacturingLots",l);
  var q=scoped(segment);q.putAll(row("itemId",item,"lotId",lot,"identificationStatus","CONFIRMED","quantity",new BigDecimal("20"),"unit","BOX","placeId",place,"custodianId",actor,"ownerId",actor,"controlScope","PHYSICAL","validFrom",Timestamp.from(t.minusSeconds(100)),"mixtureStatus","IDENTIFIED"));insert("mulino_inventory_QuantitySegments",q);
  return place;
 }
 @Test void threeModesShareRealCaptureButRetainSeparateImmutableCalculations() throws Exception {
  String transit=physicalTwenty();String lot=jdbc.queryForObject("SELECT lotId FROM mulino_inventory_QuantitySegments WHERE organizationId=?",String.class,org);
  String place=id(),segment=id();var placeRow=scoped(place);placeRow.putAll(row("name","shared W","kind","INTERNAL_STORAGE"));insert("mulino_inventory_Places",placeRow);
  var segmentRow=scoped(segment);segmentRow.putAll(row("itemId",item,"lotId",lot,"identificationStatus","CONFIRMED","quantity",new BigDecimal("100"),"unit","BOX","placeId",place,"custodianId",actor,"ownerId",actor,"controlScope","PHYSICAL","validFrom",Timestamp.from(t.minusSeconds(100)),"retiredAt",Timestamp.from(t.minusSeconds(50)),"retirementRecordedAt",Timestamp.from(t.minusSeconds(50)),"mixtureStatus","IDENTIFIED"));String child=id(),movement=id();
  new TransactionTemplate(tx).executeWithoutResult(status->{
   insert("mulino_inventory_QuantitySegments",segmentRow);
   var childRow=new LinkedHashMap<String,Object>(segmentRow);childRow.remove("retiredAt");childRow.remove("retirementRecordedAt");childRow.putAll(row("ID",child,"placeId",transit,"validFrom",Timestamp.from(t.minusSeconds(50)),"recordedAt",Timestamp.from(t.minusSeconds(50))));insert("mulino_inventory_QuantitySegments",childRow);
   var edge=scoped(id());edge.putAll(row("sourceId",segment,"targetId",child,"quantity",new BigDecimal("100"),"unit","BOX","kind","TRANSFER","uncertain",false,"occurredAt",Timestamp.from(t.minusSeconds(50))));insert("mulino_inventory_GenealogyEdges",edge);
   var m=scoped(movement);m.putAll(row("sourceId",segment,"targetId",child,"quantity",new BigDecimal("100"),"unit","BOX","kind","DISPATCH","occurredAt",Timestamp.from(t.minusSeconds(50)),"recordedAt",Timestamp.from(t.minusSeconds(50))));insert("mulino_inventory_QuantityMovements",m);
  });
  String pinned=id();var predicate=Map.<String,Object>of("operator","stateQuantity","property","QuantitySegment.stateQuantity","minimum",Map.of("value","100","unit","BOX"),"unit","BOX","evidenceSelector","CURRENT_STATE");
  var templates=new ArrayList<Definition.Goal>();for(String mode:List.of("EXISTS_IN","STATE_AT","THROUGHOUT"))templates.add(new Definition.Goal("held-"+mode,mode,"ARRIVED","core-v1",predicate));
  var d=new Definition(org,pinned,"shared-v1",null,"PUBLISHED",null,"core-v1","1.0.0",List.of(new Definition.NounType("QuantitySegment",true)),List.of(new Definition.Attribute("QuantitySegment","stateQuantity",Definition.ValueType.DECIMAL,null,"BOX",0,1,1,"READ",true)),List.of(),List.of(),templates,List.of(new Definition.Capability("getAssessment","1.0.0","core-v1","1.0.0","1.0.0",List.of())));String definitionContent=json.writeValueAsString(d);
  insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",pinned,"version","shared-v1","state","PUBLISHED","contentHash",DefinitionRepository.sha256(definitionContent),"content",definitionContent,"evaluatorVersion","core-v1","schemaVersion","1.0.0","createdAt",Timestamp.from(t.minusSeconds(100))));
  var captures=new HashSet<String>();var envelopes=new HashSet<String>();var hashes=new HashSet<String>();var rows=new ArrayList<Map<String,Object>>();
  for(String mode:List.of("EXISTS_IN","STATE_AT","THROUGHOUT")){
   String w=id(),g=id();var workRow=scoped(w);workRow.putAll(row("effectiveAt",Timestamp.from(t.minusSeconds(100)),"itemId",item,"definitionVersionId",pinned,"kind","CHECK","status","ACTIVE","ownerId",actor,"supervisorId",actor,"lifecycleMode","COMMAND"));insert("mulino_work_read_Works",workRow);
   var slots=row("quantityMode",mode,"targetQuantity","100","unit","BOX","endpoint","ARRIVED","scope",Map.of("itemId",item),"placeId",place,"action","PHYSICAL_HELD","includeReserved",true,"evidencePolicyVersion","fixture-evidence-v1","periodStart",t.minusSeconds(100).toString(),"periodEnd",t.toString(),"periodStartInclusive",true,"periodEndInclusive",true,"evaluationAt",t.toString(),"conditions",List.of("held-"+mode),"evaluatorVersion","core-v1","timezone","UTC");if(!mode.equals("STATE_AT"))slots.put("observationPolicy","complete-initial-and-final-ledger");
   var goalRow=scoped(g);goalRow.putAll(row("effectiveAt",Timestamp.from(t.minusSeconds(100)),"workId",w,"definitionVersionId",pinned,"quantityMode",mode,"targetQuantity",new BigDecimal("100"),"unit","BOX","endpoint","ARRIVED","scopeJson","{}","slotsJson",json.writeValueAsString(slots)));insert("mulino_work_read_GoalReferences",goalRow);jdbc.update("UPDATE mulino_work_read_Works SET currentGoalVersionId=? WHERE ID=?",g,w);
   var result=runtime.requestContext().run(r->{return new TransactionTemplate(tx).execute(status->service.assess(new DomainContext(org,actor,actor,t,t.plusSeconds(1000)),w));});
   var a=(Map<?,?>)result.get("assessment");assertEquals(mode.equals("EXISTS_IN")?"SATISFIED":"UNSATISFIED",a.get("outcome"),result.toString());
   var input=jdbc.queryForMap("SELECT * FROM mulino_evaluation_InputSnapshots WHERE assessmentId=?",a.get("ID"));captures.add(input.get("evidencecaptureid").toString());envelopes.add(input.get("id").toString());hashes.add(input.get("contenthash").toString());rows.add(input);
   var content=json.readTree(input.get("contentjson").toString());assertEquals(1,content.path("publicConditions").size());assertTrue(content.path("publicConditions").get(0).path("diagnostics").size()>0);assertTrue(content.path("facts").path("sources").path("inventory.QuantityMovements").toString().contains(movement));
  }
  assertEquals(1,captures.size());assertEquals(3,envelopes.size());assertEquals(3,hashes.size());String capture=captures.iterator().next();assertDoesNotThrow(()->UUID.fromString(capture));
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evaluation_EvidenceCaptures WHERE organizationId=?",Integer.class,org));
  var first=rows.getFirst();assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE mulino_evaluation_InputSnapshots SET evidenceCaptureId=NULL WHERE ID=?",first.get("id")));
  assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("DELETE FROM mulino_evaluation_EvidenceCaptures WHERE ID=?",capture));
  int before=jdbc.queryForObject("SELECT count(*) FROM mulino_evaluation_EvidenceCaptures WHERE organizationId=?",Integer.class,org);
  assertThrows(IllegalStateException.class,()->runtime.requestContext().run(r->{return new TransactionTemplate(tx).execute(status->{service.assess(new DomainContext(org,actor,actor,t.plusSeconds(1),t.plusSeconds(1000)),rows.getFirst().get("workid").toString());throw new IllegalStateException("rollback capture + assessment + Work together");});}));
  assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM mulino_evaluation_EvidenceCaptures WHERE organizationId=?",Integer.class,org));
  if(System.getProperty("capture.evidence")!=null){var dest=java.nio.file.Path.of(System.getProperty("capture.evidence"));java.nio.file.Files.createDirectories(dest);java.nio.file.Files.writeString(dest.resolve("three-modes-cap-jdbc.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("captureId",capture,"envelopes",rows,"movementId",movement,"segmentId",segment)));}
 }

 void readableWork()throws Exception {
  var snapshot=jdbc.queryForMap("SELECT * FROM mulino_work_read_Works WHERE organizationId=? AND ID=?",org,work);
  // WorkReadRepository replays production transition snapshots, with CDS field case.
  var value=new LinkedHashMap<String,Object>();
  value.putAll(row("ID",work,"organizationId",org,"itemId",item,"currentGoalVersionId",snapshot.get("currentgoalversionid"),"definitionVersionId",definition,"status","ACTIVE","ownerId",actor,"supervisorId",actor,"revision",snapshot.get("revision"),"lifecycleMode","COMMAND","effectiveAt",t.minusSeconds(100).toString()));
  insert("mulino_work_WorkTransitions",row("organizationId",org,"ID",id(),"workId",work,"revision",snapshot.get("revision"),"operation","createWork","state","ACTIVE","actorId",actor,"recordedAt",Timestamp.from(t.minusSeconds(100)),"snapshotJson",json.writeValueAsString(value)));
 }
 @Test void publicQuantityIsStoredActualAndAllOfGrantIsRechecked() throws Exception {
  readableWork();occurrence(id(),"60",t.minusSeconds(10),"sixty");occurrence(id(),"40",t.minusSeconds(5),"forty");var assessed=(Map<?,?>)assess(t).get("assessment");
  String grant=id();Timestamp from=Timestamp.from(t.minusSeconds(3600)),until=Timestamp.from(t.plusSeconds(3600));
  insert("mulino_identity_ExternalIdentities",row("organizationId",org,"ID",id(),"actorId",actor,"issuer","https://evaluation.invalid","subject",actor,"organizationAlias",org));
  insert("mulino_identity_Memberships",row("organizationId",org,"ID",id(),"actorId",actor,"validFrom",from,"validUntil",until));
  insert("mulino_identity_Grants",row("organizationId",org,"ID",grant,"actorId",actor,"delegatorId",actor,"validFrom",from,"validUntil",until));
  for(var scope:Map.of("WORK",work,"TARGET",work,"ITEM",item).entrySet())insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",grant,"scopeKind",scope.getKey(),"scopeId",scope.getValue()));
  insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",id(),"actorId",actor,"capabilityId","getAssessment","scopeKind","ORGANIZATION","scopeId",org,"validFrom",from,"validUntil",until));
  insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",grant,"capabilityId","getAssessment"));insert("mulino_identity_AuthorityFences",row("organizationId",org,"actorId",actor,"revision",1));
  var jwt=Jwt.withTokenValue("local-capture-test").header("alg","RS256").issuer("https://evaluation.invalid").subject(actor).claim("organizationId",org).build();SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,List.of()));
  java.util.function.Supplier<Map<String,Object>> read=()->runtime.requestContext().run(c->{return queries.query(new QueryRequest("getAssessment",assessed.get("ID").toString(),Map.of(),Map.of(),50,null,null,t,t.plusSeconds(1000),null));});
  try{
   var data=(Map<?,?>)read.get().get("data");assertEquals("100",data.get("evaluatedQuantity"));assertEquals("BOX",data.get("unit"));assertEquals(1,((List<?>)data.get("conditionResults")).size());assertNotNull(data.get("evidenceCaptureId"));
   jdbc.update("UPDATE mulino_identity_GrantScopes SET scopeId=? WHERE organizationId=? AND grantId=? AND scopeKind='ITEM'",id(),org,grant);assertEquals("FORBIDDEN",assertThrows(DomainError.class,read::get).code());
  }finally{SecurityContextHolder.clearContext();}
 }
 @AfterAll static void cleanupSharedCaptureFixture() throws Exception {
  PG.stop();Files.deleteIfExists(KEY);if(Files.exists(BLOBS))try(var paths=Files.walk(BLOBS)){for(var path:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
 }
}
