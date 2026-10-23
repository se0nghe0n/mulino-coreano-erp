package com.mulino.application.evidence;

import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.adapters.blob.LocalBlobStore;
import com.mulino.domain.evidence.*;
import com.sap.cds.services.runtime.CdsRuntime;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static com.mulino.domain.evidence.EvidenceTypes.*;
import static com.mulino.application.evidence.EvidenceRecords.*;

@SpringBootTest
@ActiveProfiles("local")
class EvidenceReconciliationTest {
  static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static final Path BLOBS;
  static { try { BLOBS=Files.createTempDirectory("mulino-evidence-test-"); }catch(Exception e){throw new IllegalStateException(e);} PG.start(); }
  static final String ORG=uuid(),ACTOR=uuid(),OWNER=uuid(),OTHER=uuid(),ITEM=uuid(),SOURCE=uuid(),GRANT=uuid();
  static final Instant OCCURRED=Instant.parse("2026-10-07T09:00:00Z");
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
    r.add("JWT_PUBLIC_KEY",()->System.getenv("JWT_PUBLIC_KEY"));r.add("JWT_ISSUER",()->"https://fixture.invalid");r.add("JWT_AUDIENCE",()->"mulino-acceptance");
    r.add("mulino.evidence.blob-root",BLOBS::toString);
  }
  @Autowired EvidenceReconciliation reconciliation; @Autowired EvidenceRecordCommands commands;
  @Autowired JdbcTemplate jdbc; @Autowired IdentityAuthorization auth; @Autowired EvidenceRecords records;
  @Autowired com.mulino.domain.evaluation.AssessmentFactProvider facts;
  final com.mulino.domain.evaluation.TypedPredicateEvaluator evaluator=new com.mulino.domain.evaluation.TypedPredicateEvaluator();
  @Autowired EvidenceReconciliationCommands reviewCommands;
  @Autowired EvidenceQueries queries; @Autowired EvidenceRepository repository; @Autowired LocalBlobStore blobs;
  @Autowired CdsRuntime runtime; @Autowired PlatformTransactionManager tx;
  static String uuid(){return UUID.randomUUID().toString();}
  @BeforeEach void seed() {
    for(String directory:List.of("objects","staging"))try(var files=Files.list(BLOBS.resolve(directory))){for(Path file:files.toList())Files.delete(file);}catch(Exception e){throw new IllegalStateException(e);}
    jdbc.execute("TRUNCATE mulino_evidence_SourceProfiles, mulino_inventory_TradeItems,mulino_identity_Organizations CASCADE");
    jdbc.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias) VALUES(?,'ORG-A'),(?,'ORG-B')",ORG,OTHER);
    jdbc.update("INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner) VALUES(?,?,'HUMAN',?)",ORG,ACTOR,OWNER);
    jdbc.update("INSERT INTO mulino_identity_ExternalIdentities(organizationId,ID,actorId,issuer,subject,organizationAlias) VALUES(?,?,?,'https://fixture.invalid','recorder','ORG-A')",ORG,uuid(),ACTOR);
    jdbc.update("INSERT INTO mulino_identity_Memberships(organizationId,ID,actorId,validFrom,validUntil) VALUES(?,?,?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,uuid(),ACTOR);
    jdbc.update("INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil) VALUES(?,?,?,?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,GRANT,ACTOR,ACTOR);
    for(String cap:List.of("getEvidence","getInbox","getReconciliation","attachEvidence","recordActivity","correctEvidence","linkCanonicalOccurrence","matchSourceIdentity","resolveEvidenceConflict")) {
      jdbc.update("INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,?,'ORGANIZATION',?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,uuid(),ACTOR,cap,ORG);
      jdbc.update("INSERT INTO mulino_identity_GrantActions VALUES(?,?,?)",ORG,GRANT,cap);
    }
    jdbc.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'ORGANIZATION',?)",ORG,GRANT,ORG);
    jdbc.update("INSERT INTO mulino_identity_AuthorityFences VALUES(?,?,1)",ORG,ACTOR);
    String product=uuid(),spec=uuid(),pack=uuid();
    jdbc.update("INSERT INTO mulino_inventory_Products(organizationId,ID,name) VALUES(?,?,'Synthetic product')",ORG,product);
    for(String table:List.of("SpecificationVersions","PackagingVersions"))jdbc.update("INSERT INTO mulino_inventory_"+table+"(organizationId,ID,productId,version,contentHash) VALUES(?,?,?,'1',?)",ORG,table.startsWith("Spec")?spec:pack,product,"a".repeat(64));
    jdbc.update("INSERT INTO mulino_inventory_TradeItems(organizationId,ID,productId,name,baseUnit,decimalPlaces,specificationVersionId,packagingVersionId) VALUES(?,?,?,'Synthetic biscuit','BOX',0,?,?)",ORG,ITEM,product,spec,pack);
    jdbc.update("INSERT INTO mulino_evidence_SourceProfiles(ID,organizationId,revision,recordedAt,recordedBy,namespace,policyVersion,intakeOwnerId,supervisorId,nextAction,nextCheckAt) VALUES(?,?,1,CURRENT_TIMESTAMP,?,'warehouse','synthetic-v1',?,?,'Compare source',CURRENT_TIMESTAMP+INTERVAL '1 hour')",SOURCE,ORG,ACTOR,ACTOR,ACTOR);
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("local-test-context").header("alg","RS256").issuer("https://fixture.invalid").subject("recorder").claim("organizationId","ORG-A").build(),List.of()));
  }
  @AfterEach void clear(){SecurityContextHolder.clearContext();}
  <T>T request(Callable<T> work){return runtime.requestContext().run(ctx->{try{return work.call();}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}});}
  DocumentInput document(String prior){return new DocumentInput(new Subject(SubjectKind.ITEM,ITEM),"warehouse","source-reference","text/plain",LocalBlobStore.hash("original".getBytes()),"SYNTHETIC fixture",prior);}
  String document(){return request(()->records.attachDocument(document(null),"original".getBytes())).get("id").toString();}
  EventInput event(String sourceId,String version,String payload,String prior){return new EventInput(new Subject(SubjectKind.ITEM,ITEM),"RECEIPT","warehouse",sourceId,version,new EffectiveTime(OCCURRED,null,"Asia/Seoul","SECOND"),ValueState.KNOWN,payload,prior,null);}
  String event(){return request(()->records.recordActivity(event("R60","1","receipt60",null))).get("id").toString();}
  String claim(String event,String doc,String quantity){return request(()->records.recordClaim(new ClaimInput(event,doc,"Receipt reported",quantity,"BOX",ValueState.KNOWN,null))).get("id").toString();}
  QueryResult read(String kind,String id,Instant knownAt){return request(()->queries.query(auth.context(OCCURRED.plusSeconds(100),knownAt),new QueryRequest("getEvidence",id,Map.of("kind",kind),Map.of(),50,null,null,OCCURRED.plusSeconds(100),knownAt,null)));}

  String segment(){
    String id=uuid(),manufacturer=uuid(),lot=uuid(),place=uuid();
    jdbc.update("INSERT INTO mulino_inventory_Manufacturers(organizationId,ID,name) VALUES(?,?, 'Fixture maker')",ORG,manufacturer);
    jdbc.update("INSERT INTO mulino_inventory_ManufacturingLots(organizationId,ID,itemId,manufacturerId,originalLot) VALUES(?,?,?,?,?)",ORG,lot,ITEM,manufacturer,id);
    jdbc.update("INSERT INTO mulino_inventory_Places(organizationId,ID,name,kind) VALUES(?,?, 'Fixture W','WAREHOUSE')",ORG,place);
    jdbc.update("INSERT INTO mulino_inventory_QuantitySegments(organizationId,ID,itemId,lotId,placeId,controlScope,quantity,unit,identificationStatus,mixtureStatus,validFrom) VALUES(?,?,?,?,?,'fixture',60,'BOX','CONFIRMED','IDENTIFIED',?)",ORG,id,ITEM,lot,place,java.sql.Timestamp.from(OCCURRED));
    return id;
  }
  EvidenceReconciliation.Review review(String claim,String doc,String segment,String quantity){return new EvidenceReconciliation.Review(claim,doc,segment,null,"synthetic-v1","warehouse:R60:1",quantity,"BOX",OCCURRED,"Human reviewed original, source, event identity and exact physical scope");}
  @Test void authorizedReviewLinksTwoDocumentsToOneCanonicalSixty(){
    String physical=segment(),event=event(),first=document(),second=document();
    String c1=claim(event,first,"60"),c2=claim(event,second,"60");
    var r1=request(()->reconciliation.match(auth.context(null,null),review(c1,first,physical,"60"),"matchSourceIdentity"));
    assertEquals("MATCHED",r1.get("outcome"));
    var canonical1=request(()->reconciliation.link(auth.context(null,null),r1.get("id").toString()));
    var r2=request(()->reconciliation.match(auth.context(null,null),review(c2,second,physical,"60"),"matchSourceIdentity"));
    var canonical2=request(()->reconciliation.link(auth.context(null,null),r2.get("id").toString()));
    assertEquals(canonical1.get("id"),canonical2.get("id"));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_QuantityMovements",Integer.class));
  }
  @Test void oneSourceObservationCannotBeCountedAgainstTwoPhysicalScopes(){
    String physical=segment(),otherPhysical=segment(),event=event(),doc=document(),claim=claim(event,doc,"60");
    var first=request(()->reconciliation.match(auth.context(null,null),review(claim,doc,physical,"60"),"matchSourceIdentity"));
    request(()->reconciliation.link(auth.context(null,null),first.get("id").toString()));
    var conflicting=request(()->reconciliation.match(auth.context(null,null),review(claim,doc,otherPhysical,"60"),"matchSourceIdentity"));
    assertEquals("CONFLICT",conflicting.get("outcome"));
    assertThrows(DomainError.class,()->request(()->reconciliation.link(auth.context(null,null),conflicting.get("id").toString())));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));
  }
  @Test void unknownIdentityAndQuantityMismatchRetainIntakeResponsibility(){
    String event=event(),doc=document(),claim=claim(event,doc,"60"),physical=segment();
    var result=request(()->reconciliation.match(auth.context(null,null),review(claim,doc,physical,"58"),"matchSourceIdentity"));
    assertEquals("UNVERIFIED",result.get("outcome"));
    assertThrows(DomainError.class,()->request(()->reconciliation.link(auth.context(null,null),result.get("id").toString())));
    var persisted=jdbc.queryForMap("SELECT intakeOwnerId,nextAction,nextCheckAt FROM mulino_evidence_Reconciliations WHERE ID=?",result.get("id"));
    assertEquals(ACTOR,persisted.get("intakeOwnerId"));assertNotNull(persisted.get("nextAction"));assertNotNull(persisted.get("nextCheckAt"));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));
  }
  @Test void competingSourceHashCannotBeResolvedBySelectingPriority(){
    String event=event(),doc=document(),claim=claim(event,doc,"60"),physical=segment();
    request(()->records.recordActivity(event("R60","1","different original claim",null)));
    var result=request(()->reconciliation.match(auth.context(null,null),review(claim,doc,physical,"60"),"resolveEvidenceConflict"));
    assertEquals("CONFLICT",result.get("outcome"));
    assertThrows(DomainError.class,()->request(()->reconciliation.link(auth.context(null,null),result.get("id").toString())));
    assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_Events",Integer.class));
    assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("UPDATE mulino_evidence_Reconciliations SET decision='MATCHED' WHERE ID=?",result.get("id")));
  }
  @Test void publicSlotsRejectBooleanProofAndCrossSourceGrant(){
    var slots=new LinkedHashMap<String,Object>();slots.put("subject",Map.of("kind","ITEM","id",ITEM));slots.put("sourceNamespace","warehouse");slots.put("sourceMatched",true);
    assertThrows(DomainError.class,()->request(()->commands.prepare(auth.context(null,null),Map.of("capabilityId","recordActivity","slots",slots))));
    String event=event(),doc=document(),claim=claim(event,doc,"60"),physical=segment();
    jdbc.update("DELETE FROM mulino_identity_GrantScopes WHERE grantId=?",GRANT);
    jdbc.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'SOURCE',?)",ORG,GRANT,uuid());
    assertThrows(DomainError.class,()->request(()->reconciliation.match(auth.context(null,null),review(claim,doc,physical,"60"),"matchSourceIdentity")));
  }
  @Test void correctionRequiresAtomicImpactImplementation(){
    String previous=event();
    var slots=new LinkedHashMap<String,Object>();slots.put("subject",Map.of("kind","ITEM","id",ITEM));slots.put("kind","RECEIPT");slots.put("sourceNamespace","warehouse");slots.put("externalEventId","R60");slots.put("sourceVersion","2");slots.put("effectiveFrom",OCCURRED.toString());slots.put("timeZone","Asia/Seoul");slots.put("timePrecision","SECOND");slots.put("valueState","KNOWN");slots.put("payload","actual58");slots.put("supersedesId",previous);
    var unavailable=new EvidenceRecordCommands(repository,records,auth,new org.springframework.beans.factory.support.DefaultListableBeanFactory().getBeanProvider(EvidenceCorrectionImpact.class));
    assertThrows(DomainError.class,()->request(()->unavailable.execute(auth.context(null,null),Map.of("capabilityId","correctEvidence","slots",slots))));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_Events",Integer.class));
  }

  record Observation(String event,String claim,String document,Instant start,Instant end) {}
  Map<String,Object> invoke(String capability,Map<String,Object> slots) {
    return request(()->{
      var c=auth.context(null,null);var intent=Map.<String,Object>of("capabilityId",capability,"slots",slots);
      var handler=Set.of("matchSourceIdentity","linkCanonicalOccurrence").contains(capability)?reviewCommands:commands;
      handler.prepare(c,intent);
      var output=new LinkedHashMap<>(handler.execute(c,intent));
      if(output.containsKey("evidenceStatus"))output.put("outcome",output.get("evidenceStatus"));
      return output;
    });
  }
  Observation observation(String physical,String source,Instant start,Instant end,Instant reported,
      String quantity,java.util.function.Consumer<Map<String,Object>> mutate,Instant declaredUntil) {
    var segment=request(()->repository.subject(ORG,"SEGMENT",physical));
    var payload=new TreeMap<String,Object>();payload.put("activityKind","STATE_OBSERVATION");
    payload.put("segmentId",physical);payload.put("placeId",segment.get("placeId"));payload.put("quantity",quantity);
    payload.put("unit","BOX");payload.put("coverageStart",start.toString());payload.put("coverageEnd",end.toString());
    payload.put("occurredAt",reported.toString());payload.put("sourceNamespace","warehouse");payload.put("sourceVersion","1");mutate.accept(payload);
    String json;try{json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);}catch(Exception e){throw new IllegalStateException(e);}
    byte[] bytes=json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var doc=invoke("attachEvidence",Map.of("subject",Map.of("kind","SEGMENT","id",physical),"sourceNamespace","warehouse",
      "sourceReference",source,"mediaType","application/json","expectedHash",LocalBlobStore.hash(bytes),
      "provenance","Synthetic exact observation","contentBase64",Base64.getEncoder().encodeToString(bytes)));
    var slots=new LinkedHashMap<String,Object>();slots.put("subject",Map.of("kind","SEGMENT","id",physical));
    slots.put("kind","STATE_OBSERVATION");slots.put("sourceNamespace","warehouse");slots.put("externalEventId",source);
    slots.put("sourceVersion","1");slots.put("effectiveFrom",start.toString());if(declaredUntil!=null)slots.put("effectiveUntil",declaredUntil.toString());
    slots.put("timeZone","UTC");slots.put("timePrecision","SECOND");slots.put("valueState","KNOWN");slots.put("payload",json);
    slots.put("quantity",quantity);slots.put("unit","BOX");slots.put("documentId",doc.get("id"));slots.put("assertion","Exact original observation");
    var event=invoke("recordActivity",slots);
    return new Observation(event.get("id").toString(),event.get("claimId").toString(),doc.get("id").toString(),start,end);
  }
  Observation observation(String physical,String source,Instant start,Instant end) {
    return observation(physical,source,start,end,end,"60",p->{},start.equals(end)?null:end.plusNanos(1000));
  }
  Map<String,Object> matchObservation(Observation observation,String physical,String source,String quantity) {
    return invoke("matchSourceIdentity",Map.of("claimId",observation.claim(),"basisDocumentId",observation.document(),
      "physicalScopeId",physical,"policyVersion","synthetic-v1","sourceIdentity","warehouse:"+source+":1",
      "quantity",quantity,"unit","BOX","effectiveFrom",observation.start().toString(),"reason","Compare original interval and exact physical scope"));
  }
  Map<String,Object> linkObservation(Map<String,Object> review) {return invoke("linkCanonicalOccurrence",Map.of("reconciliationId",review.get("id")));}
  static final Map<String,Object> OBSERVED=Map.of("operator","stateQuantity","property","QuantitySegment.observedQuantity","unit","BOX","minimum","60","maximum","60");
  com.mulino.domain.evaluation.EvaluationFacts observed(Instant known) {
    var definition=new com.mulino.domain.definitions.Definition(ORG,uuid(),"v1",null,"PUBLISHED","a".repeat(64),"v1","v1",
      List.of(),List.of(new com.mulino.domain.definitions.Definition.Attribute("QuantitySegment","observedQuantity",com.mulino.domain.definitions.Definition.ValueType.DECIMAL,null,"BOX",0,1,1,"ACTIVE",true)),List.of(),List.of(),
      List.of(new com.mulino.domain.definitions.Definition.Goal("held","THROUGHOUT","ARRIVED","v1",OBSERVED)),List.of());
    return request(()->facts.load(auth.context(OCCURRED.plusSeconds(7200),known),Map.of("ID",uuid(),"itemId",ITEM),
      Map.of("endpoint","ARRIVED","quantityMode","THROUGHOUT","evidencePolicyVersion","synthetic-v1"),definition));
  }
  String throughout(Instant known) {
    return evaluator.evaluateInterval(OBSERVED,observed(known),OCCURRED,OCCURRED.plusSeconds(7200),known,true,true,true).truth().state().name();
  }
  @Test void observedIntervalsRequireCanonicalLinkAndRespectKnownAtAndInclusiveEndpoints(){
    String physical=segment();var first=observation(physical,"interval-a",OCCURRED,OCCURRED.plusSeconds(3600));
    assertEquals("UNVERIFIED",throughout(Instant.now()));
    var review=matchObservation(first,physical,"interval-a","60");assertEquals("MATCHED",review.get("outcome"));
    assertEquals("UNVERIFIED",throughout(Instant.now()));
    Instant beforeLink=Instant.now().minusSeconds(1);linkObservation(review);
    assertTrue(observed(Instant.now()).properties().get("QuantitySegment.observedQuantity").getFirst().verified());
    assertEquals("UNVERIFIED",throughout(Instant.now()));
    var last=observation(physical,"interval-b",OCCURRED.plusSeconds(3600),OCCURRED.plusSeconds(7200));
    var matched=matchObservation(last,physical,"interval-b","60");assertEquals("MATCHED",matched.get("outcome"));linkObservation(matched);
    assertEquals("SATISFIED",throughout(Instant.now()));assertEquals("UNVERIFIED",throughout(beforeLink));
    var rows=request(()->repository.rows("CanonicalOccurrences",ORG));assertEquals(2,rows.size());
    assertTrue(rows.stream().anyMatch(r->instant(r.get("effectiveUntil")).equals(OCCURRED.plusSeconds(7200).plusNanos(1000))));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_QuantityMovements",Integer.class));
  }
  @Test void observedPointNeverFillsGapAndExactDuplicateDoesNotDoubleCount(){
    String physical=segment();var first=observation(physical,"gap-a",OCCURRED,OCCURRED.plusSeconds(3600));
    String canonical=linkObservation(matchObservation(first,physical,"gap-a","60")).get("id").toString();
    var duplicate=observation(physical,"gap-copy",OCCURRED,OCCURRED.plusSeconds(3600));
    assertEquals(canonical,linkObservation(matchObservation(duplicate,physical,"gap-copy","60")).get("id"));
    var point=observation(physical,"gap-point",OCCURRED.plusSeconds(7200),OCCURRED.plusSeconds(7200));
    linkObservation(matchObservation(point,physical,"gap-point","60"));
    assertEquals("UNVERIFIED",throughout(Instant.now()));
    var rows=request(()->repository.rows("CanonicalOccurrences",ORG));assertEquals(2,rows.size());
    assertEquals(1,rows.stream().filter(r->r.get("effectiveUntil")==null).count());
    assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_Verifications",Integer.class));
  }
  @Test void overlappingEqualIntervalsWithSameStartCoexistButContradictionIsHeld(){
    String physical=segment();var first=observation(physical,"overlap-a",OCCURRED,OCCURRED.plusSeconds(3600));
    linkObservation(matchObservation(first,physical,"overlap-a","60"));
    var larger=observation(physical,"overlap-b",OCCURRED,OCCURRED.plusSeconds(7200));
    linkObservation(matchObservation(larger,physical,"overlap-b","60"));
    var wrong=observation(physical,"overlap-wrong",OCCURRED.plusSeconds(1800),OCCURRED.plusSeconds(5400),OCCURRED.plusSeconds(5400),"59",p->{},OCCURRED.plusSeconds(5400).plusNanos(1000));
    var held=matchObservation(wrong,physical,"overlap-wrong","59");assertEquals("CONFLICT",held.get("outcome"));
    assertThrows(DomainError.class,()->linkObservation(held));
    var responsibility=jdbc.queryForMap("SELECT intakeOwnerId,nextAction,nextCheckAt FROM mulino_evidence_Reconciliations WHERE ID=?",held.get("id"));
    assertEquals(ACTOR,responsibility.get("intakeOwnerId"));assertNotNull(responsibility.get("nextAction"));assertNotNull(responsibility.get("nextCheckAt"));
    assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));
  }
  @Test void observationOriginalAndTemporalForgeryCannotBeCanonicalized(){
    String physical=segment();Instant end=OCCURRED.plusSeconds(3600);
    int n=0;
    for(String field:List.of("segmentId","placeId","quantity","unit","coverageEnd","coverageStart","sourceVersion","occurredAt")) {
      String source="forged-"+(n++);var observation=observation(physical,source,OCCURRED,end,end,"60",p->p.put(field,"wrong"),end.plusNanos(1000));
      var held=matchObservation(observation,physical,source,"60");assertEquals("UNVERIFIED",held.get("outcome"),field);assertThrows(DomainError.class,()->linkObservation(held));
    }
    var invented=observation(physical,"invented-tail",OCCURRED,end,end,"60",p->{},end.plusSeconds(1));
    assertEquals("UNVERIFIED",matchObservation(invented,physical,"invented-tail","60").get("outcome"));
    var reversed=observation(physical,"reversed-source",OCCURRED,end,end,"60",p->p.put("coverageStart",end.plusSeconds(1).toString()),end.plusNanos(1000));
    assertEquals("UNVERIFIED",matchObservation(reversed,physical,"reversed-source","60").get("outcome"));
    var pointTail=observation(physical,"point-tail",OCCURRED,OCCURRED,OCCURRED,"60",p->{},end);
    assertEquals("UNVERIFIED",matchObservation(pointTail,physical,"point-tail","60").get("outcome"));
    assertThrows(DomainError.class,()->new EffectiveTime(end,OCCURRED,"UTC","SECOND"));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_QuantityMovements",Integer.class));
  }
  @Test void differentOriginalDocumentAndUnknownClaimRemainUnverified(){
    String physical=segment();var observation=observation(physical,"document-a",OCCURRED,OCCURRED.plusSeconds(3600));
    byte[] different="{\"quantity\":\"59\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    String differentDoc=request(()->records.attachDocument(new DocumentInput(new Subject(SubjectKind.SEGMENT,physical),
      "warehouse","document-a","application/json",LocalBlobStore.hash(different),"Different original",null),different)).get("id").toString();
    String differentClaim=request(()->records.recordClaim(new ClaimInput(observation.event(),differentDoc,"Different source", "60","BOX",ValueState.KNOWN,null))).get("id").toString();
    var mismatch=new Observation(observation.event(),differentClaim,differentDoc,observation.start(),observation.end());
    assertEquals("UNVERIFIED",matchObservation(mismatch,physical,"document-a","60").get("outcome"));
    String unknownClaim=request(()->records.recordClaim(new ClaimInput(observation.event(),observation.document(),"Unknown observation",null,null,ValueState.UNKNOWN,null))).get("id").toString();
    var unknown=new Observation(observation.event(),unknownClaim,observation.document(),observation.start(),observation.end());
    var held=matchObservation(unknown,physical,"document-a","60");assertEquals("UNVERIFIED",held.get("outcome"));
    assertThrows(DomainError.class,()->linkObservation(held));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_QuantityMovements",Integer.class));
  }
  @Test void itemScopedSourceReviewerLinksWithoutWeakeningAllOf(){
    String physical=segment();
    jdbc.update("DELETE FROM mulino_identity_GrantScopes WHERE grantId=?",GRANT);
    jdbc.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'ITEM',?)",ORG,GRANT,ITEM);
    var observation=observation(physical,"item-source",OCCURRED,OCCURRED.plusSeconds(3600));
    var matched=matchObservation(observation,physical,"item-source","60");assertEquals("MATCHED",matched.get("outcome"));
    linkObservation(matched);
    assertTrue(observed(Instant.now()).properties().get("QuantitySegment.observedQuantity").getFirst().verified());
    // A second WORK dimension must still deny a SEGMENT-only source request.
    jdbc.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'WORK',?)",ORG,GRANT,uuid());
    assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->observation(physical,"item-and-work",OCCURRED,OCCURRED.plusSeconds(3600))).code());
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_QuantityMovements",Integer.class));
  }
  @Test void observationCurrentSourceOwnerAndOrganizationAreMandatory(){
    String physical=segment();var observation=observation(physical,"scope-a",OCCURRED,OCCURRED.plusSeconds(3600));
    assertEquals("UNVERIFIED",request(()->reconciliation.match(auth.context(null,null),new EvidenceReconciliation.Review(observation.claim(),observation.document(),segment(),null,"synthetic-v1","warehouse:scope-a:1","60","BOX",OCCURRED,"Wrong physical segment"),"matchSourceIdentity")).get("outcome"));
    for(var foreign:List.of(new DomainContext(ORG,OWNER,OWNER,Instant.now(),Instant.now()),
        new DomainContext(OTHER,ACTOR,OWNER,Instant.now(),Instant.now()))) {
      assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->request(()->reconciliation.match(foreign,
        new EvidenceReconciliation.Review(observation.claim(),observation.document(),physical,null,"synthetic-v1",
          "warehouse:scope-a:1","60","BOX",OCCURRED,"Foreign reviewer"),"matchSourceIdentity"))).code());
    }
    jdbc.update("DELETE FROM mulino_identity_GrantScopes WHERE grantId=?",GRANT);
    jdbc.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'ORGANIZATION',?)",ORG,GRANT,OTHER);
    assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->matchObservation(observation,physical,"scope-a","60")).code());
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));
  }
}
