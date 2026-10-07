package com.mulino;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.core.*;
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
  var d=new Definition(org,definition,"fixture-v1",null,"PUBLISHED",null,"core-v1","1.0.0",List.of(new Definition.NounType("Receipt",true)),List.of(new Definition.Attribute("Receipt","quantity",Definition.ValueType.DECIMAL,null,"BOX",0,1,1,"READ",true)),List.of(),List.of(),List.of(new Definition.Goal("Arrival","CUMULATIVE_EVENT","ARRIVED","core-v1",predicate)),List.of(new Definition.Capability("getAssessment","1.0.0","core-v1","1.0.0","1.0.0",List.of())));String content=json.writeValueAsString(d);
  insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",definition,"version","fixture-v1","state","PUBLISHED","contentHash",DefinitionRepository.sha256(content),"content",content,"evaluatorVersion","core-v1","schemaVersion","1.0.0","createdAt",Timestamp.from(t.minusSeconds(100))));
  var w=scoped(work);w.putAll(row("effectiveAt",Timestamp.from(t.minusSeconds(100)),"itemId",item,"definitionVersionId",definition,"kind","PURCHASE","status","ACTIVE","ownerId",actor,"supervisorId",actor,"lifecycleMode","COMMAND"));insert("mulino_work_read_Works",w);
  var g=scoped(goal);g.putAll(row("effectiveAt",Timestamp.from(t.minusSeconds(100)),"workId",work,"definitionVersionId",definition,"quantityMode","CUMULATIVE_EVENT","targetQuantity",new BigDecimal("100"),"unit","BOX","endpoint","ARRIVED","scopeJson","{}","slotsJson",json.writeValueAsString(slots())));insert("mulino_work_read_GoalReferences",g);jdbc.update("UPDATE mulino_work_read_Works SET currentGoalVersionId=? WHERE organizationId=? AND ID=?",goal,org,work);
  insert("mulino_governance_PolicyVersions",row("organizationId",org,"ID",policy,"version","fixture-evidence-v1","kind","EVIDENCE","content","{}","contentHash",DefinitionRepository.sha256("{}"),"createdAt",LocalDateTime.ofInstant(t.minusSeconds(100),ZoneOffset.UTC),"effectiveFrom",LocalDateTime.ofInstant(t.minusSeconds(100),ZoneOffset.UTC)));
  if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass('mulino_governance_activepolicies') IS NOT NULL",Boolean.class)))jdbc.update("INSERT INTO mulino_governance_ActivePolicies(organizationId,kind,policyId,revision) VALUES(?,'EVIDENCE',?,0)",org,policy);
  var s=evidence(profile);s.putAll(row("namespace","fixture","policyVersion","fixture-evidence-v1","intakeOwnerId",actor,"supervisorId",actor,"nextAction","compare","nextCheckAt",Timestamp.from(t.plusSeconds(3600))));insert("mulino_evidence_SourceProfiles",s);
 }
 void occurrence(String physical,String quantity,Instant occurred,String source){
  String event=id(),claim=id(),canonical=id(),doc=id();byte[] content=(source+quantity).getBytes();var blob=blobs.stage(content,LocalBlobStore.hash(content));blobs.commit(blob);
  var document=evidence(doc);document.putAll(row("subjectKind","WORK","subjectId",work,"itemId",item,"workId",work,"sha256",blob.sha256(),"blobId",blob.id().toString(),"byteLength",blob.size(),"mediaType","text/plain","sourceNamespace","fixture","sourceProfileId",profile,"availability","AVAILABLE","provenance","{}"));insert("mulino_evidence_DocumentVersions",document);
  var e=evidence(event);e.putAll(row("subjectKind","WORK","subjectId",work,"itemId",item,"workId",work,"effectiveFrom",Timestamp.from(occurred),"timeZone","UTC","timePrecision","SECOND","valueState","KNOWN","kind","RECEIPT","sourceNamespace","fixture","sourceProfileId",profile,"externalEventId",source,"sourceVersion","1","payloadHash",LocalBlobStore.hash(content),"payload","{}"));insert("mulino_evidence_Events",e);
  var in=evidence(id());in.putAll(row("subjectKind","WORK","subjectId",work,"itemId",item,"workId",work,"eventId",event,"sourceNamespace","fixture","sourceProfileId",profile,"externalEventId",source,"sourceVersion","1","payloadHash",LocalBlobStore.hash(content),"state","RECEIVED","intakeOwnerId",actor,"supervisorId",actor,"nextAction","compare","nextCheckAt",Timestamp.from(t.plusSeconds(3600))));insert("mulino_evidence_InboxRecords",in);
  var c=evidence(claim);c.putAll(row("sourceProfileId",profile,"subjectKind","WORK","subjectId",work,"itemId",item,"workId",work,"effectiveFrom",Timestamp.from(occurred),"timeZone","UTC","timePrecision","SECOND","valueState","KNOWN","eventId",event,"documentVersionId",doc,"assertion","receipt","quantity",new BigDecimal(quantity),"unit","BOX"));insert("mulino_evidence_Claims",c);
  var o=evidence(canonical);o.putAll(row("sourceProfileId",profile,"subjectKind","WORK","subjectId",work,"itemId",item,"workId",work,"effectiveFrom",Timestamp.from(occurred),"timeZone","UTC","timePrecision","SECOND","valueState","KNOWN","kind","RECEIPT","physicalScopeId",physical,"quantity",new BigDecimal(quantity),"unit","BOX","reassessmentState","PENDING"));var existing=jdbc.queryForList("SELECT ID FROM mulino_evidence_CanonicalOccurrences WHERE organizationId=? AND physicalScopeId=? AND kind='RECEIPT'",String.class,org,physical);if(existing.isEmpty())insert("mulino_evidence_CanonicalOccurrences",o);else canonical=existing.getFirst();
  var v=evidence(id());v.putAll(row("claimId",claim,"canonicalOccurrenceId",canonical,"basisDocumentId",doc,"policyVersion","fixture-evidence-v1","sourceMatched",true,"identityMatched",true,"quantityMatched",true,"timeMatched",true,"duplicateChecked",true,"verdict","VERIFIED","reason","fixture"));insert("mulino_evidence_Verifications",v);
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
}
