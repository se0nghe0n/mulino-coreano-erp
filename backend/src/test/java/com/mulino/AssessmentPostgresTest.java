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
class AssessmentPostgresTest {
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
 @Test void physicalDedupTargetLateArrivalAndImmutablePastAreActual()throws Exception{
  String physical=id();occurrence(physical,"60",t.minusSeconds(10),"sourceA");occurrence(physical,"60",t.minusSeconds(10),"sourceB");var result=assess(t);assertEquals("APPLIED",result.get("outcome"),()->result.toString());var a=(Map<?,?>)result.get("assessment");assertEquals("UNSATISFIED",a.get("outcome"));
  assertEquals(new BigDecimal("60.000000000000"),jdbc.queryForObject("SELECT SUM(q) FROM(SELECT physicalScopeId,MAX(quantity) q FROM mulino_evidence_CanonicalOccurrences WHERE organizationId=? GROUP BY physicalScopeId) s",BigDecimal.class,org));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM mulino_evaluation_InputSnapshots WHERE organizationId=?",Integer.class,org));assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE mulino_work_read_AssessmentReferences SET outcome='SATISFIED' WHERE organizationId=?",org));assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("DELETE FROM mulino_evaluation_InputSnapshots WHERE organizationId=?",org));
  String old=a.get("ID").toString();occurrence(id(),"40",t.plusSeconds(5),"late40");var next=(Map<?,?>)assess(t.plusSeconds(10)).get("assessment");assertEquals("SATISFIED",next.get("outcome"));assertEquals(old,next.get("previousAssessmentId"));assertEquals(true,next.get("deadlineViolated"));assertEquals("UNSATISFIED",jdbc.queryForObject("SELECT outcome FROM mulino_work_read_AssessmentReferences WHERE ID=?",String.class,old));
 }
 @Test void unknownAndUnsupportedPinnedEvaluatorRemainHeld()throws Exception{
  assertEquals("UNVERIFIED",((Map<?,?>)assess(t).get("assessment")).get("outcome"));var slots=slots();slots.put("evaluatorVersion","future-v9");String newGoal=id();var g=scoped(newGoal);g.putAll(row("effectiveAt",Timestamp.from(t),"workId",work,"definitionVersionId",definition,"quantityMode","CUMULATIVE_EVENT","targetQuantity",new BigDecimal("100"),"unit","BOX","endpoint","ARRIVED","scopeJson","{}","slotsJson",json.writeValueAsString(slots),"previousGoalId",goal,"goalVersion",2));insert("mulino_work_read_GoalReferences",g);jdbc.update("UPDATE mulino_work_read_Works SET currentGoalVersionId=? WHERE organizationId=? AND ID=?",newGoal,org,work);assertEquals("HELD",assess(t).get("outcome"));assertEquals(true,jdbc.queryForObject("SELECT pendingInvalidation FROM mulino_work_read_Works WHERE ID=?",Boolean.class,work));
 }
 void revisePeriod(boolean startInclusive,boolean endInclusive)throws Exception{
  var slots=slots();slots.put("periodStart",t.minusSeconds(10).toString());slots.put("periodEnd",t.toString());slots.put("periodStartInclusive",startInclusive);slots.put("periodEndInclusive",endInclusive);
  String next=id();var g=scoped(next);g.putAll(row("effectiveAt",Timestamp.from(t),"workId",work,"definitionVersionId",definition,"quantityMode","CUMULATIVE_EVENT","targetQuantity",new BigDecimal("100"),"unit","BOX","endpoint","ARRIVED","scopeJson","{}","slotsJson",json.writeValueAsString(slots),"previousGoalId",goal,"goalVersion",2));insert("mulino_work_read_GoalReferences",g);jdbc.update("UPDATE mulino_work_read_Works SET currentGoalVersionId=? WHERE organizationId=? AND ID=?",next,org,work);goal=next;
 }
 @Test void immutableGoalPeriodEndpointsControlActualCanonicalContribution()throws Exception{
  occurrence(id(),"60",t.minusSeconds(10),"start60");occurrence(id(),"40",t,"end40");
  revisePeriod(true,false);var startOnly=(Map<?,?>)assess(t).get("assessment");assertEquals("UNSATISFIED",startOnly.get("outcome"));
  assertEquals(new BigDecimal("60.000000000000"),jdbc.queryForObject("SELECT SUM(quantity) FROM mulino_evidence_CanonicalOccurrences WHERE organizationId=? AND effectiveFrom>=? AND effectiveFrom<?",BigDecimal.class,org,Timestamp.from(t.minusSeconds(10)),Timestamp.from(t)));
  revisePeriod(false,true);var endOnly=(Map<?,?>)assess(t).get("assessment");assertEquals("UNSATISFIED",endOnly.get("outcome"));
  revisePeriod(true,true);var both=(Map<?,?>)assess(t).get("assessment");assertEquals("SATISFIED",both.get("outcome"));assertEquals(endOnly.get("ID"),both.get("previousAssessmentId"));
  assertEquals("UNSATISFIED",jdbc.queryForObject("SELECT outcome FROM mulino_work_read_AssessmentReferences WHERE ID=?",String.class,startOnly.get("ID")));
 }

 @Test void declaredSubjectsBindOnlyActualWorkAndCurrentImmutableGoal()throws Exception{
  var intent=Map.<String,Object>of("slots",Map.of("workId",work));
  runtime.requestContext().run(request->{var c=new DomainContext(org,actor,actor,t,t.plusSeconds(1000));var preparation=service.prepare(c,intent);var bindings=service.subjectBindings(c,intent,preparation);
    assertEquals(List.of("Work","Goal"),bindings.stream().map(SubjectBinding::nounType).toList());assertEquals(Set.of(work),bindings.get(0).targetIds());assertEquals(Set.of(goal),bindings.get(1).targetIds());assertTrue(bindings.stream().allMatch(b->b.minimumCount()==0&&b.maximumCount()==1));
    assertThrows(DomainError.class,()->service.subjectBindings(c,intent,CommandPreparation.ordinary(preparation.scopes(),preparation.fenceKeys(),"ASSESSMENT",id(),0)));
    return null;});
  String previous=goal;revisePeriod(true,false);
  runtime.requestContext().run(request->{var c=new DomainContext(org,actor,actor,t,t.plusSeconds(1000));var bindings=service.subjectBindings(c,intent,service.prepare(c,intent));assertEquals(Set.of(goal),bindings.get(1).targetIds());assertFalse(bindings.get(1).targetIds().contains(previous));return null;});
 }

 String physicalTwenty(){
  String place=id(),manufacturer=id(),lot=id(),segment=id();
  var p=scoped(place);p.putAll(row("name","W","kind","INTERNAL_STORAGE"));insert("mulino_inventory_Places",p);
  var m=scoped(manufacturer);m.put("name","fixture");insert("mulino_inventory_Manufacturers",m);
  var l=scoped(lot);l.putAll(row("manufacturerId",manufacturer,"itemId",item,"originalLot","L"));insert("mulino_inventory_ManufacturingLots",l);
  var q=scoped(segment);q.putAll(row("itemId",item,"lotId",lot,"identificationStatus","CONFIRMED","quantity",new BigDecimal("20"),"unit","BOX","placeId",place,"custodianId",actor,"ownerId",actor,"controlScope","PHYSICAL","validFrom",Timestamp.from(t.minusSeconds(100)),"mixtureStatus","IDENTIFIED"));insert("mulino_inventory_QuantitySegments",q);
  return place;
 }
 Map<String,Object> canonicalStateGoal(String place,Instant evaluationAt,boolean inclusive)throws Exception{
  return canonicalStateGoal(place,evaluationAt,inclusive,true);
 }
 Map<String,Object> canonicalStateGoal(String place,Instant evaluationAt,boolean inclusive,boolean exactSource)throws Exception{
  var content=json.readValue(jdbc.queryForObject("SELECT content FROM mulino_definitions_DefinitionVersions WHERE ID=?",String.class,definition),Map.class);
  definition=id();content.put("id",definition);content.put("version","state-v1");
  var predicate=Map.<String,Object>of("operator","quantitySum","property","Receipt.quantity","minimum",Map.of("value","1","unit","BOX"),"unit","BOX","evidenceSelector","VERIFIED_DISTINCT");
  content.put("goals",List.of(Map.of("name","Verified","quantityMode","STATE_AT","endpoint","VERIFIED","evaluatorVersion","core-v1","predicate",predicate)));
  String encoded=json.writeValueAsString(content);
  insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",definition,"version","state-v1","state","PUBLISHED","contentHash",DefinitionRepository.sha256(encoded),"content",encoded,"evaluatorVersion","core-v1","schemaVersion","1.0.0","createdAt",Timestamp.from(t.minusSeconds(100))));
  work=id();var w=scoped(work);w.putAll(row("effectiveAt",Timestamp.from(t.minusSeconds(100)),"itemId",item,"definitionVersionId",definition,"kind","CHECK","status","ACTIVE","ownerId",actor,"supervisorId",actor,"lifecycleMode","COMMAND"));insert("mulino_work_read_Works",w);
  var slots=row("quantityMode","STATE_AT","targetQuantity","20","unit","BOX","endpoint","VERIFIED","scope",Map.of("workId",work),"timezone","UTC","evidencePolicyVersion","fixture-evidence-v1","evaluationAt",evaluationAt.toString(),"placeId",place,"action","PHYSICAL","includeReserved",true,"conditions",List.of("Verified"),"evaluatorVersion","core-v1","dueAt",t.toString(),"deadlineInclusive",inclusive,"evidenceSource",Map.of("sourceNamespace","fixture","externalEventId","EV-READY","sourceVersion","1"));
  if(!exactSource)slots.remove("evidenceSource");
  com.mulino.domain.work.GoalInput.validate(slots,true);
  String next=id();var g=scoped(next);g.putAll(row("effectiveAt",Timestamp.from(t.minusSeconds(1)),"workId",work,"definitionVersionId",definition,"quantityMode","STATE_AT","targetQuantity",new BigDecimal("20"),"unit","BOX","endpoint","VERIFIED","scopeJson","{}","slotsJson",json.writeValueAsString(slots),"goalVersion",1));insert("mulino_work_read_GoalReferences",g);
  jdbc.update("UPDATE mulino_work_read_Works SET currentGoalVersionId=? WHERE ID=?",next,work);goal=next;return slots;
 }
 @Test void stateAtRequiresExactCanonicalEvidenceNotPhysicalTwentyAndPreservesHistory()throws Exception{
  String place=physicalTwenty();var slots=canonicalStateGoal(place,t,true);
  var before=(Map<?,?>)assess(t).get("assessment");assertEquals("UNVERIFIED",before.get("outcome"));assertEquals(false,before.get("held"));
  occurrence(id(),"20",t.minusSeconds(1),"OTHER-EVENT",place,null);
  assertEquals("UNVERIFIED",((Map<?,?>)assess(t).get("assessment")).get("outcome"));
  String physical=id();occurrence(physical,"20",t,"EV-READY",place,null);
  // Another report of the same physical scope cannot double the target quantity.
  occurrence(physical,"20",t,"OTHER-REPORT",place,null);
  var verified=(Map<?,?>)assess(t.plusSeconds(1)).get("assessment");assertEquals("SATISFIED",verified.get("outcome"));assertEquals(false,verified.get("deadlineViolated"));
  assertEquals("UNVERIFIED",jdbc.queryForObject("SELECT outcome FROM mulino_work_read_AssessmentReferences WHERE ID=?",String.class,before.get("ID")));
  String snapshot=jdbc.queryForObject("SELECT contentJson FROM mulino_evaluation_InputSnapshots WHERE assessmentId=?",String.class,verified.get("ID"));assertTrue(snapshot.contains("EV-READY"),"source identity");assertTrue(jdbc.queryForObject("SELECT content FROM mulino_definitions_DefinitionVersions WHERE ID=?",String.class,definition).contains("VERIFIED_DISTINCT"),"published predicate selector");assertTrue(snapshot.contains(definition),"pinned definition ID");
  assertEquals(new BigDecimal("20.000000000000"),jdbc.queryForObject("SELECT SUM(quantity) FROM mulino_inventory_QuantitySegments WHERE organizationId=?",BigDecimal.class,org));
  runtime.requestContext().run(c->{service.requireResume(new DomainContext(org,actor,actor,t.plusSeconds(1),t.plusSeconds(1000)),Map.of("ID",work,"itemId",item),Map.of("resumePredicate",Map.of("operator","quantitySum","property","Receipt.quantity","minimum",Map.of("value","20","unit","BOX"),"unit","BOX","evidenceSelector","VERIFIED_DISTINCT")),Map.of());return null;});
 }
 @Test void canonicalStateHonorsEvaluationTimeAndExclusiveValidityEnd()throws Exception{
  String place=physicalTwenty();canonicalStateGoal(place,t,false);
  occurrence(id(),"20",t.minusSeconds(1),"EV-READY",place,t);
  assertEquals("UNVERIFIED",((Map<?,?>)assess(t.plusSeconds(1)).get("assessment")).get("outcome"));
 }
 @Test void canonicalStateCannotPredictFutureOrIgnoreMissingBlob()throws Exception{
  String place=physicalTwenty();canonicalStateGoal(place,t.plusSeconds(10),true);
  occurrence(id(),"20",t,"EV-READY",place,null);
  assertEquals("UNVERIFIED",((Map<?,?>)assess(t).get("assessment")).get("outcome"));
  assertEquals("SATISFIED",((Map<?,?>)assess(t.plusSeconds(10)).get("assessment")).get("outcome"));
  String blob=jdbc.queryForObject("SELECT blobId FROM mulino_evidence_DocumentVersions WHERE organizationId=?",String.class,org);Files.delete(BLOBS.resolve("objects").resolve(blob));
  assertEquals("UNVERIFIED",((Map<?,?>)assess(t.plusSeconds(11)).get("assessment")).get("outcome"));
 }

 Map<String,Object> evidenceCall(java.util.function.Supplier<Map<String,Object>> call){return runtime.requestContext().run(c->{return call.get();});}
 @Test void stateTargetAndInclusiveDeadlineAreIndependent()throws Exception{
  String place=physicalTwenty();canonicalStateGoal(place,t,true);
  occurrence(id(),"10",t,"EV-READY",place,null);
  var boundary=(Map<?,?>)assess(t).get("assessment");assertEquals("UNSATISFIED",boundary.get("outcome"));assertEquals(false,boundary.get("deadlineViolated"));
  var overdue=(Map<?,?>)assess(t.plusSeconds(1)).get("assessment");assertEquals("UNSATISFIED",overdue.get("outcome"));assertEquals(true,overdue.get("deadlineViolated"));
 }
 @Test void exclusiveDeadlineUsesCutoffWithoutMovingFixedStateEvaluationIntoFuture()throws Exception{
  String place=physicalTwenty();canonicalStateGoal(place,t,false);
  occurrence(id(),"10",t.minusSeconds(1),"EV-READY",place,null);
  var boundary=(Map<?,?>)assess(t).get("assessment");
  assertEquals("UNSATISFIED",boundary.get("outcome"));
  assertEquals(true,boundary.get("deadlineViolated"),"known shortage before the exclusive deadline must survive the fixed evaluationAt guard");
 }
 @Test void exclusiveDeadlineExcludesEvidenceAtDueAndNeverPredictsFutureState()throws Exception{
  String place=physicalTwenty();canonicalStateGoal(place,t,false,false);
  occurrence(id(),"10",t.minusSeconds(1),"BEFORE-DUE",place,null);
  occurrence(id(),"10",t,"EV-READY",place,null);
  var boundary=(Map<?,?>)assess(t).get("assessment");
  assertEquals("SATISFIED",boundary.get("outcome"));
  assertEquals(true,boundary.get("deadlineViolated"),"the second 10 is late for an exclusive deadline");
 }
 @Test void exclusiveDeadlineCannotAssessFutureState()throws Exception{
  String place=physicalTwenty();canonicalStateGoal(place,t.plusSeconds(10),false);
  occurrence(id(),"10",t.minusSeconds(1),"EV-READY",place,null);
  var future=(Map<?,?>)assess(t).get("assessment");
  assertEquals("UNVERIFIED",future.get("outcome"));assertEquals(false,future.get("deadlineViolated"));
 }
 @Test void canonicalEvidenceDoesNotProveEligibilityOrUnreservedQuantity()throws Exception{
  String place=physicalTwenty();var slots=canonicalStateGoal(place,t,true);
  for(var change:List.of(Map.<String,Object>of("action","SELL"),Map.<String,Object>of("includeReserved",false))){
   var invalid=new LinkedHashMap<>(slots);invalid.putAll(change);var pinned=Map.<String,Object>of("definitionVersionId",definition,"slotsJson",json.writeValueAsString(invalid));
   runtime.requestContext().run(c->{assertEquals("VERSION_UNSUPPORTED",assertThrows(DomainError.class,()->service.requireSupportedGoal(new DomainContext(org,actor,actor,t,t.plusSeconds(1000)),pinned)).code());return null;});
  }
 }
 void authorizeObservations(){
  String grant=id();Timestamp from=Timestamp.from(t.minusSeconds(3600)),until=Timestamp.from(t.plusSeconds(3600));
  insert("mulino_identity_ExternalIdentities",row("organizationId",org,"ID",id(),"actorId",actor,"issuer","https://evaluation.invalid","subject",actor,"organizationAlias",org));
  insert("mulino_identity_Memberships",row("organizationId",org,"ID",id(),"actorId",actor,"validFrom",from,"validUntil",until));
  insert("mulino_identity_Grants",row("organizationId",org,"ID",grant,"actorId",actor,"delegatorId",actor,"validFrom",from,"validUntil",until));
  insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",grant,"scopeKind","ORGANIZATION","scopeId",org));
  insert("mulino_identity_AuthorityFences",row("organizationId",org,"actorId",actor,"revision",1));
  for(String capability:List.of("recordActivity","attachEvidence","getEvidence","matchSourceIdentity","linkCanonicalOccurrence")){
   insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",id(),"actorId",actor,"capabilityId",capability,"scopeKind","ORGANIZATION","scopeId",org,"validFrom",from,"validUntil",until));
   insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",grant,"capabilityId",capability));
  }
  var jwt=Jwt.withTokenValue("local-test-context").header("alg","RS256").issuer("https://evaluation.invalid").subject(actor).claim("organizationId",org).build();
  SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,List.of()));
 }
 @Test void actualEvidenceReviewAndCanonicalLinkFulfilStateGoalWithoutInventoryEffect()throws Exception{
  String place=physicalTwenty();canonicalStateGoal(place,t,true);
  String segment=jdbc.queryForObject("SELECT ID FROM mulino_inventory_QuantitySegments WHERE organizationId=?",String.class,org);
  authorizeObservations();
  try{
   var subject=new com.mulino.domain.evidence.EvidenceTypes.Subject(com.mulino.domain.evidence.EvidenceTypes.SubjectKind.SEGMENT,segment);
   byte[] original="Independent warehouse observation: 20 BOX, EV-READY".getBytes();
   String doc=evidenceCall(()->records.attachDocument(new com.mulino.application.evidence.EvidenceRecords.DocumentInput(subject,"fixture","EV-READY","text/plain",LocalBlobStore.hash(original),"local scenario original",null),original)).get("id").toString();
   String event=evidenceCall(()->records.recordActivity(new com.mulino.application.evidence.EvidenceRecords.EventInput(subject,"STOCK_OBSERVATION","fixture","EV-READY","1",new com.mulino.domain.evidence.EvidenceTypes.EffectiveTime(t,null,"UTC","SECOND"),com.mulino.domain.evidence.EvidenceTypes.ValueState.KNOWN,"20 BOX observed",null,null))).get("id").toString();
   String claim=evidenceCall(()->records.recordClaim(new com.mulino.application.evidence.EvidenceRecords.ClaimInput(event,doc,"Warehouse observation","20","BOX",com.mulino.domain.evidence.EvidenceTypes.ValueState.KNOWN,null))).get("id").toString();
   assertEquals("UNVERIFIED",((Map<?,?>)assess(t.plusSeconds(601)).get("assessment")).get("outcome"));
   var review=evidenceCall(()->reconciliation.match(auth.context(t.plusSeconds(600),t.plusSeconds(600)),new com.mulino.application.evidence.EvidenceReconciliation.Review(claim,doc,segment,null,"fixture-evidence-v1","fixture:EV-READY:1","20","BOX",t,"Exact source, time, physical range and quantity reviewed"),"matchSourceIdentity"));
   assertEquals("MATCHED",review.get("outcome"));
   evidenceCall(()->reconciliation.link(auth.context(t.plusSeconds(600),t.plusSeconds(600)),review.get("id").toString()));
   assertEquals("SATISFIED",((Map<?,?>)assess(t.plusSeconds(601)).get("assessment")).get("outcome"));
   assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM mulino_evidence_CanonicalOccurrences WHERE organizationId=?",Integer.class,org));
   assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM mulino_inventory_QuantityMovements WHERE organizationId=?",Integer.class,org));
  }finally{SecurityContextHolder.clearContext();}
 }

 @Test void recoveryFixturePublishesEveryVerbAndSupportedTypedGoal()throws Exception{
  var content=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(Path.of("../verification/actual/scenarios/installer-conventions.json").toFile()).path("definition");
  var contract=json.readTree(Path.of("../verification/cases/V5/fixtures/waiting-full-restart.json").toFile()).path("baseline").path("definitionContract");
  ((com.fasterxml.jackson.databind.node.ArrayNode)content.path("goals")).add(contract.path("goalTemplate"));
  for(var capability:contract.path("capabilities")){
    var existing=java.util.stream.StreamSupport.stream(content.path("capabilities").spliterator(),false).filter(c->c.path("capabilityId").equals(capability.path("capabilityId"))).findFirst();
    if(existing.isPresent())assertEquals(existing.get(),capability,"Recovery reuses the identical published capability");
    else ((com.fasterxml.jackson.databind.node.ArrayNode)content.path("capabilities")).add(capability);
  }
  var verbs=json.createArrayNode();for(var verb:content.path("verbs"))if(!verb.path("capabilityId").asText().equals("getCommandResult"))verbs.add(verb);content.set("verbs",verbs);
  content.put("organizationId",org).put("id",id());
  Definition published=json.treeToValue(content,Definition.class);
  // This fixture-only handler is registered by the actual run's verification profile, absent in this local-profile test.
  assertTrue(definitionValidator.validate(published).problems().stream().allMatch(p->p.path().equals("fixtureExternalEffect")&&p.code().equals("UNSUPPORTED_CAPABILITY")),()->definitionValidator.validate(published).toString());
  assertEquals("VALID",definitionValidator.validatePredicate(published,published.goals().getLast().predicate()).outcome());
 }

 // Uses the published stock predicates, the real CQN fact provider and immutable assessment service.
 Map<String,Object> stockGoal(String place,String mode,Instant evaluationAt)throws Exception{return stockGoal(place,mode,evaluationAt,"20");}
 Map<String,Object> stockGoal(String place,String mode,Instant evaluationAt,String target)throws Exception{
  var common=json.readTree(Path.of(System.getProperty("repo.root","..")).resolve("verification/actual/scenarios/installer-conventions.json").toFile()).path("definition");
  var selected=java.util.stream.StreamSupport.stream(common.path("goals").spliterator(),false).filter(g->mode.equals(g.path("quantityMode").asText())).findFirst().orElseThrow();
  var content=json.readValue(jdbc.queryForObject("SELECT content FROM mulino_definitions_DefinitionVersions WHERE ID=?",String.class,definition),Map.class);
  definition=id();content.put("id",definition);content.put("version","stock-"+definition);
  content.put("nouns",List.of(Map.of("name","QuantitySegment","core",true)));
  content.put("attributes",json.convertValue(common.path("attributes"),List.class).stream().filter(a->"QuantitySegment".equals(((Map<?,?>)a).get("nounType"))).toList());
  content.put("goals",List.of(json.convertValue(selected,Map.class)));
  String encoded=json.writeValueAsString(content);
  insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",definition,"version","stock-"+definition,"state","PUBLISHED","contentHash",DefinitionRepository.sha256(encoded),"content",encoded,"evaluatorVersion","core-v1","schemaVersion","1.0.0","createdAt",Timestamp.from(t.minusSeconds(100))));
  work=id();var w=scoped(work);w.putAll(row("effectiveAt",Timestamp.from(t.minusSeconds(100)),"itemId",item,"definitionVersionId",definition,"kind","CHECK","status","ACTIVE","ownerId",actor,"supervisorId",actor,"lifecycleMode","COMMAND"));insert("mulino_work_read_Works",w);
  var slots=row("quantityMode",mode,"targetQuantity",target,"unit","BOX","endpoint","ARRIVED","scope",Map.of("itemId",item),"timezone","UTC","evidencePolicyVersion","fixture-evidence-v1","evaluationAt",evaluationAt.toString(),"placeId",place,"action","PHYSICAL_HELD","includeReserved",true,"conditions",List.of(selected.path("name").asText()),"evaluatorVersion","core-v1","dueAt",t.plusSeconds(100).toString(),"periodStart",t.minusSeconds(10).toString(),"periodEnd",t.plusSeconds(10).toString(),"periodStartInclusive",true,"periodEndInclusive",false,"observationPolicy","complete-initial-and-final-ledger");
  String next=id();var g=scoped(next);g.putAll(row("effectiveAt",Timestamp.from(t.minusSeconds(1)),"workId",work,"definitionVersionId",definition,"quantityMode",mode,"targetQuantity",new BigDecimal(target),"unit","BOX","endpoint","ARRIVED","scopeJson","{}","slotsJson",json.writeValueAsString(slots),"goalVersion",1));insert("mulino_work_read_GoalReferences",g);
  jdbc.update("UPDATE mulino_work_read_Works SET currentGoalVersionId=? WHERE ID=?",next,work);goal=next;return slots;
 }
 Map<?,?> lastStateAssessment;
 String stateAt(Instant at,Instant known){return runtime.requestContext().run(c->{return new TransactionTemplate(tx).execute(s->{lastStateAssessment=(Map<?,?>)service.assess(new DomainContext(org,actor,actor,at,known),work).get("assessment");return lastStateAssessment.get("outcome").toString();});});}
 void retireAt(Instant at){
  String segment=jdbc.queryForObject("SELECT ID FROM mulino_inventory_QuantitySegments WHERE organizationId=? AND retiredAt IS NULL",String.class,org);
  runtime.requestContext().run(c->{new TransactionTemplate(tx).executeWithoutResult(status->stock.disposeRange(new DomainContext(org,actor,actor,at,at),segment,BigDecimal.ZERO,new BigDecimal("20"),at,id(),id()));});
 }
 void coverage(String place,Instant start,Instant end,String source){
  occurrence(jdbc.queryForObject("SELECT ID FROM mulino_inventory_QuantitySegments WHERE organizationId=?",String.class,org),"20",start,source,place,end,"STATE_OBSERVATION",!source.equals("unverified-interval"));
 }
 @Test void stockStateUsesEvaluationTimeAndRecordedRetirementWithoutInventingEmptyInventory()throws Exception{
  String place=physicalTwenty();stockGoal(place,"STATE_AT",t);
  assertEquals("SATISFIED",stateAt(t.plusSeconds(20),t.plusSeconds(20)));
  retireAt(t);
  assertEquals("UNSATISFIED",stateAt(t.plusSeconds(20),t.plusSeconds(20)));
  String movement=jdbc.queryForObject("SELECT ID FROM mulino_inventory_QuantityMovements WHERE organizationId=?",String.class,org);
  String snapshot=jdbc.queryForObject("SELECT contentJson FROM mulino_evaluation_InputSnapshots WHERE assessmentId=?",String.class,lastStateAssessment.get("ID"));
  assertTrue(snapshot.contains(movement),"Zero state retains its actual disposal movement in the input snapshot");
  assertEquals("SATISFIED",stateAt(t.plusSeconds(20),t.minusSeconds(1)),"A later retirement must not rewrite earlier knowledge");
  assertEquals("UNVERIFIED",stateAt(t.minusSeconds(1),t.plusSeconds(20)),"Future state cannot be predicted");
 }
 @Test void intervalStockNeedsContinuousVerifiedCoverageAndBindsActualTarget()throws Exception{
  String place=physicalTwenty();stockGoal(place,"THROUGHOUT",t);
  assertEquals("UNVERIFIED",stateAt(t.plusSeconds(20),t.plusSeconds(20)),"Inventory alone is not an observation interval");
  coverage(place,t.minusSeconds(10),t.minusSeconds(1),"start");
  coverage(place,t.plusSeconds(1),t.plusSeconds(10),"end");
  assertEquals("UNVERIFIED",stateAt(t.plusSeconds(20),t.plusSeconds(20)),"Verified endpoints must not bridge the gap");
  coverage(place,t.minusSeconds(1),t.plusSeconds(1),"middle");
  assertEquals("SATISFIED",stateAt(t.plusSeconds(20),t.plusSeconds(20)));
  stockGoal(place,"THROUGHOUT",t,"21");
  assertEquals("UNSATISFIED",stateAt(t.plusSeconds(20),t.plusSeconds(20)),"An interval must use its requested 21 BOX target, not template minimum 1");
 }
 @Test void verifiedPointObservationsCannotProveThroughoutAndRawClaimsCannotProvideCoverage()throws Exception{
  String place=physicalTwenty();stockGoal(place,"THROUGHOUT",t);
  coverage(place,t.minusSeconds(10),null,"point-start");coverage(place,t.plusSeconds(10),null,"point-end");
  assertEquals("UNVERIFIED",stateAt(t.plusSeconds(20),t.plusSeconds(20)));
  coverage(place,t.minusSeconds(10),t.plusSeconds(10),"unverified-interval");
  assertEquals("UNVERIFIED",stateAt(t.plusSeconds(20),t.plusSeconds(20)));
 }
 @Test void intervalExistsAndThroughoutKeepDifferentAnswersAfterCommittedStockRetirement()throws Exception{
  String place=physicalTwenty();stockGoal(place,"EXISTS_IN",t);retireAt(t);
  assertEquals("SATISFIED",stateAt(t.plusSeconds(20),t.plusSeconds(20)));
  stockGoal(place,"THROUGHOUT",t);
  assertEquals("UNSATISFIED",stateAt(t.plusSeconds(20),t.plusSeconds(20)),"Known zero disproves throughout even when coverage is incomplete");
 }

 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(booleans={true,false})
 void reviewedStateObservationIntervalProvidesCoverageOnlyAfterCanonicalLink(boolean complete)throws Exception{
  String place=physicalTwenty();stockGoal(place,"THROUGHOUT",t);readableWork();authorizeAssessmentRead();configureAssessmentCommandPolicy();
  String segment=jdbc.queryForObject("SELECT ID FROM mulino_inventory_QuantitySegments WHERE organizationId=?",String.class,org);
  try{
   var subject=new com.mulino.domain.evidence.EvidenceTypes.Subject(com.mulino.domain.evidence.EvidenceTypes.SubjectKind.SEGMENT,segment);
   // The source's closed microsecond end preserves the original exclusive
   // 00:00:10 boundary; a prose assertion alone cannot verify its coverage.
   String payload=json.writeValueAsString(Map.of("activityKind","STATE_OBSERVATION",
     "segmentId",segment,"placeId",place,"quantity","20","unit","BOX",
     "coverageStart",t.minusSeconds(10).toString(),"coverageEnd",(complete?t.plusSeconds(10).minusNanos(1000):t.minusSeconds(10)).toString(),
     "occurredAt",t.plusSeconds(10).toString(),"sourceNamespace","fixture","sourceVersion","1"));
   byte[] original=payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
   String doc=evidenceCall(()->records.attachDocument(new com.mulino.application.evidence.EvidenceRecords.DocumentInput(subject,"fixture","INTERVAL-20","application/json",LocalBlobStore.hash(original),"explicit interval original",null),original)).get("id").toString();
   String event=evidenceCall(()->records.recordActivity(new com.mulino.application.evidence.EvidenceRecords.EventInput(subject,"STATE_OBSERVATION","fixture","INTERVAL-20","1",new com.mulino.domain.evidence.EvidenceTypes.EffectiveTime(t.minusSeconds(10),complete?t.plusSeconds(10):null,"UTC","SECOND"),com.mulino.domain.evidence.EvidenceTypes.ValueState.KNOWN,payload,null,null))).get("id").toString();
   String claim=evidenceCall(()->records.recordClaim(new com.mulino.application.evidence.EvidenceRecords.ClaimInput(event,doc,"Observed full identified segment interval","20","BOX",com.mulino.domain.evidence.EvidenceTypes.ValueState.KNOWN,null))).get("id").toString();
   assertEquals("UNVERIFIED",publicAssessmentOutcome());
   var review=evidenceCall(()->reconciliation.match(auth.context(t.plusSeconds(600),t.plusSeconds(600)),new com.mulino.application.evidence.EvidenceReconciliation.Review(claim,doc,segment,null,"fixture-evidence-v1","fixture:INTERVAL-20:1","20","BOX",t.minusSeconds(10),"Original interval and identified segment checked"),"matchSourceIdentity"));
   assertEquals("MATCHED",review.get("outcome"));
   assertEquals("UNVERIFIED",publicAssessmentOutcome());
   evidenceCall(()->reconciliation.link(auth.context(t.plusSeconds(600),t.plusSeconds(600)),review.get("id").toString()));
   assertEquals(complete?"SATISFIED":"UNVERIFIED",publicAssessmentOutcome(),"Only a verified covering interval proves throughout");
   assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_QuantityMovements WHERE organizationId=?",Integer.class,org));
  }finally{SecurityContextHolder.clearContext();}
 }

 void configureAssessmentCommandPolicy()throws Exception {
  String content=json.writeValueAsString(Map.of("rules",Map.of("assessGoal",Map.of("effectClass","ASSESSMENT"))));String policyId=id();
  insert("mulino_governance_PolicyVersions",row("organizationId",org,"ID",policyId,"version","assessment-command-v1","kind","COMMAND","content",content,"contentHash",DefinitionRepository.sha256(content),"createdAt",Timestamp.from(t.minusSeconds(100)),"effectiveFrom",Timestamp.from(t.minusSeconds(100))));
  insert("mulino_governance_ActivePolicies",row("organizationId",org,"kind","COMMAND","policyId",policyId,"revision",0));
 }
 String publicAssessmentOutcome(){
  var result=runtime.requestContext().run(c->{return commands.execute(row("intentKind","COMMAND","definitionVersion",definition,"capabilityId","assessGoal","subjectRefs",List.of(),"slots",Map.of("workId",work),"provenance",Map.of("workId","CONTEXT"),"expectedRevision",jdbc.queryForObject("SELECT revision FROM mulino_work_read_Works WHERE ID=?",Integer.class,work),"commandIdempotencyKey",id()));});
  assertEquals("APPLIED",result.get("outcome"),result::toString);
  String assessment=((Map<?,?>)result.get("effects")).get("assessmentId").toString();
  var before=databaseState();var response=readAssessment(assessment,t.plusSeconds(600),t.plusSeconds(1000));
  assertEquals(before,databaseState(),"The query after a public assessment performs no writes");
  assertEquals(assessment,data(response).get("ID"));assertEquals(false,data(response).get("held"));
  return data(response).get("outcome").toString();
 }
 void readableWork()throws Exception {
  var snapshot=jdbc.queryForMap("SELECT * FROM mulino_work_read_Works WHERE organizationId=? AND ID=?",org,work);
  // WorkReadRepository replays production transition snapshots, with CDS field case.
  var value=new LinkedHashMap<String,Object>();
  value.putAll(row("ID",work,"organizationId",org,"itemId",item,"currentGoalVersionId",snapshot.get("currentgoalversionid"),"definitionVersionId",definition,"status","ACTIVE","ownerId",actor,"supervisorId",actor,"revision",snapshot.get("revision"),"lifecycleMode","COMMAND","effectiveAt",t.minusSeconds(100).toString()));
  insert("mulino_work_WorkTransitions",row("organizationId",org,"ID",id(),"workId",work,"revision",snapshot.get("revision"),"operation","createWork","state","ACTIVE","actorId",actor,"recordedAt",Timestamp.from(t.minusSeconds(100)),"snapshotJson",json.writeValueAsString(value)));
 }
 void authorizeAssessmentRead(){
  authorizeObservations();
  String grant=jdbc.queryForObject("SELECT ID FROM mulino_identity_Grants WHERE organizationId=?",String.class,org);
  for(String cap:List.of("getAssessment","assessGoal")){
   insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",id(),"actorId",actor,"capabilityId",cap,"scopeKind","ORGANIZATION","scopeId",org,"validFrom",Timestamp.from(t.minusSeconds(3600)),"validUntil",Timestamp.from(t.plusSeconds(3600))));
   insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",grant,"capabilityId",cap));
  }
 }
 Map<String,Object> readAssessment(String target,Instant at,Instant known){return readAssessment(target,at,known,Map.of(),null);}
 Map<String,Object> readAssessment(String target,Instant at,Instant known,Map<String,Object> scope,String snapshot){
  return runtime.requestContext().run(c->{return queries.query(new QueryRequest("getAssessment",target,scope,Map.of(),50,null,null,at,known,snapshot));});
 }
 Map<?,?> data(Map<String,Object> response){return (Map<?,?>)response.get("data");}
 Map<String,Object> databaseState(){
  var result=new LinkedHashMap<String,Object>();
  for(String table:List.of("mulino_work_read_Works","mulino_work_read_GoalReferences","mulino_work_read_AssessmentReferences","mulino_evaluation_InputSnapshots","mulino_work_read_ObligationReferences","mulino_inventory_QuantityMovements","mulino_commands_CommandRecords","mulino_runtime_Outbox"))
   result.put(table,jdbc.queryForList("SELECT row_to_json(r)::text AS row FROM "+table+" r WHERE organizationId=? ORDER BY row_to_json(r)::text",org));
  return result;
 }
 @Test void applicationRegistryReadsPersistedAssessmentByWorkAndIdWithoutWrites()throws Exception{
  readableWork();authorizeAssessmentRead();
  try{
   assertTrue(queries.operations().contains("getAssessment"));
   var before=databaseState();var pending=readAssessment(work,t,t.plusSeconds(1000));
   assertEquals(true,data(pending).get("pending"));assertFalse(data(pending).containsKey("outcome"));assertFalse(data(pending).containsKey("ID"));
   assertEquals(before,databaseState());
   occurrence(id(),"60",t.minusSeconds(10),"first");var assessed=(Map<?,?>)assess(t).get("assessment");
   before=databaseState();
   var current=readAssessment(work,t,t.plusSeconds(1000));var explicit=readAssessment(assessed.get("ID").toString(),t,t.plusSeconds(1000));
   assertEquals("UNSATISFIED",data(current).get("outcome"));assertEquals(assessed.get("ID"),data(current).get("ID"));
   assertEquals(goal,data(current).get("goalId"));assertEquals(definition,data(current).get("definitionVersionId"));assertEquals(policy,data(current).get("policyVersionId"));
   assertEquals(data(current),data(explicit));assertEquals(current.get("snapshotRevision"),explicit.get("snapshotRevision"));
   assertEquals(1,((List<?>)data(current).get("inputSnapshots")).size());assertFalse(((List<?>)data(current).get("conditionResults")).isEmpty());
   readAssessment(work,t,t.plusSeconds(1000),Map.of(),current.get("snapshotRevision").toString());
   var scoped=runtime.requestContext().run(c->{return queries.query(new QueryRequest("getAssessment",null,Map.of("workId",work),Map.of(),50,null,null,t,t.plusSeconds(1000),null));});
   assertEquals(assessed.get("ID"),data(scoped).get("ID"));
   assertEquals("SNAPSHOT_CHANGED",assertThrows(DomainError.class,()->readAssessment(work,t,t.plusSeconds(1000),Map.of(),"wrong-snapshot")).code());
   assertEquals(before,databaseState(),"All public reads leave business rows, commands and outbox unchanged");
  }finally{SecurityContextHolder.clearContext();}
 }
 @Test void assessmentReadsHonorBothTimeBoundsPendingGoalAndCurrentAllOfGrant()throws Exception{
  readableWork();authorizeAssessmentRead();
  try{
   var first=(Map<?,?>)assess(t).get("assessment");String firstId=first.get("ID").toString();
   assertEquals(true,data(readAssessment(work,t,t.plusSeconds(599))).get("pending"),"Assessment recorded at +600 is invisible before knownAt");
   occurrence(id(),"100",t.plusSeconds(5),"later");var second=(Map<?,?>)assess(t.plusSeconds(10)).get("assessment");
   assertEquals(firstId,data(readAssessment(work,t,t.plusSeconds(1000))).get("ID"),"asOf excludes a future assessment even when known");
   assertEquals(second.get("ID"),data(readAssessment(work,t.plusSeconds(10),t.plusSeconds(1000))).get("ID"));
   assertEquals("UNVERIFIED",data(readAssessment(firstId,t.plusSeconds(10),t.plusSeconds(1000))).get("outcome"));
   var changed=scoped(id());changed.putAll(row("effectiveAt",Timestamp.from(t),"workId",work,"definitionVersionId",definition,"quantityMode","CUMULATIVE_EVENT","targetQuantity",new BigDecimal("100"),"unit","BOX","endpoint","ARRIVED","scopeJson","{}","slotsJson",json.writeValueAsString(slots()),"previousGoalId",goal,"goalVersion",2));
   insert("mulino_work_read_GoalReferences",changed);jdbc.update("UPDATE mulino_work_read_Works SET currentGoalVersionId=?,pendingInvalidation=true WHERE ID=?",changed.get("ID"),work);
   jdbc.update("UPDATE mulino_work_read_Works SET revision=revision+1 WHERE ID=?",work);readableWork();
   var pending=readAssessment(work,t.plusSeconds(10),t.plusSeconds(1000));assertEquals(true,data(pending).get("pending"));assertEquals(changed.get("ID"),data(pending).get("goalId"));assertFalse(data(pending).containsKey("outcome"));
   assertEquals(firstId,data(readAssessment(firstId,t.plusSeconds(10),t.plusSeconds(1000))).get("ID"));
   assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->readAssessment(firstId,t,t.plusSeconds(1000),Map.of("organizationId",id()),null)).code());
   assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->readAssessment(firstId,t,t.plusSeconds(1000),Map.of("workId",id()),null)).code());
   String grant=jdbc.queryForObject("SELECT ID FROM mulino_identity_Grants WHERE organizationId=?",String.class,org);
   insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",grant,"scopeKind","WORK","scopeId",work));
   insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",grant,"scopeKind","ITEM","scopeId",id()));
   assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->readAssessment(firstId,t,t.plusSeconds(1000))).code(),"An OR over grant dimensions would leak this assessment");
   jdbc.update("DELETE FROM mulino_identity_GrantScopes WHERE organizationId=? AND scopeKind='ITEM'",org);
   assertEquals(firstId,data(readAssessment(firstId,t,t.plusSeconds(1000))).get("ID"));
   jdbc.update("UPDATE mulino_identity_Grants SET revokedAt=? WHERE organizationId=?",Timestamp.from(t.plusSeconds(1)),org);
   assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->readAssessment(firstId,t,t.plusSeconds(1000))).code(),"Past query time does not revive a revoked current grant");
  }finally{SecurityContextHolder.clearContext();}
 }

 @Test void multipleVisibleAssessmentHeadsHoldTheReadWithoutChangingHistory()throws Exception {
  readableWork();authorizeAssessmentRead();
  try{
   String first=((Map<?,?>)assess(t).get("assessment")).get("ID").toString();
   var fork=new LinkedHashMap<>(jdbc.queryForMap("SELECT * FROM mulino_work_read_AssessmentReferences WHERE organizationId=? AND ID=?",org,first));
   fork.put("id",id());insert("mulino_work_read_AssessmentReferences",fork);
   var before=databaseState();
   var held=assertThrows(DomainError.class,()->readAssessment(work,t,t.plusSeconds(1000)));
   assertEquals("HELD",held.outcome());assertEquals("ASSESSMENT_CONFLICT",held.code());
   assertEquals(first,data(readAssessment(first,t,t.plusSeconds(1000))).get("ID"),"An explicitly identified immutable assessment remains readable");
   assertEquals(before,databaseState(),"Ambiguous current heads neither select a winner nor mutate assessment history");
  }finally{SecurityContextHolder.clearContext();}
 }
 @Test void directAssessmentRejectsUnsupportedPlaceAndCustomerScopes()throws Exception {
  readableWork();authorizeAssessmentRead();
  try{
   String assessment=((Map<?,?>)assess(t).get("assessment")).get("ID").toString();
   var before=databaseState();
   for(String target:List.of(work,assessment))for(String key:List.of("placeId","customerId")){
    assertEquals("TYPE_INVALID",assertThrows(DomainError.class,()->readAssessment(target,t,t.plusSeconds(1000),Map.of(key,id()),null)).code());
   }
   var input=Map.<String,Object>of("id",work,"filters",Map.of("customerId",id()),"asOf",t.toString(),"knownAt",t.plusSeconds(1000).toString());
   assertEquals("TYPE_INVALID",assertThrows(DomainError.class,()->runtime.requestContext().run(c->{return queries.query(QueryRequests.parse("getAssessment",input));})).code());
   assertEquals(before,databaseState());
  }finally{SecurityContextHolder.clearContext();}
 }

}
