package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.mulino.application.core.*;
import com.sap.cds.ql.Select;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class S2WorkActualIntegrationTest {
  static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static final java.security.KeyPair KEY;
  static final Path KEY_PATH;
  static final Path BLOB_PATH;
  static { try {PG.start();var generator=java.security.KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KEY=generator.generateKeyPair();BLOB_PATH=Files.createTempDirectory("mulino-s1-read-blob-");KEY_PATH=Files.createTempFile("mulino-s1-read-public-",".pem");Files.writeString(KEY_PATH,"-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(KEY.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----\n");}catch(Exception failure){throw new ExceptionInInitializerError(failure);} }
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);r.add("JWT_PUBLIC_KEY",KEY_PATH::toString);r.add("JWT_ISSUER",()->"https://mulino.local.invalid");r.add("JWT_AUDIENCE",()->"mulino-platform");r.add("mulino.evidence.blob-root",BLOB_PATH::toString);}
  @Autowired com.mulino.application.work.WorkLifecycle lifecycle;
  @Autowired com.mulino.domain.work.WorkRepository workRepository;
  @Autowired com.mulino.application.evaluation.AssessmentService assessmentService;
  @Autowired com.mulino.adapters.blob.LocalBlobStore blobs;

  @Autowired JdbcTemplate jdbc;
  @Autowired ApplicationQueries queries;
  @Autowired PersistenceService persistence;
  @Autowired CdsRuntime runtime;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
  @LocalServerPort int port;
  final ObjectMapper json=new ObjectMapper();
  String org,actor,product,item,spec,pack,manufacturer,lot,place,segment,definition,work,goal,obligation,document,profile;
  static final Instant AS_OF=Instant.parse("2026-10-07T09:00:00Z"),KNOWN=Instant.parse("2026-10-07T09:00:01Z");
  String id(){return UUID.randomUUID().toString();}
  void insert(String table,Map<String,Object> row){String columns=String.join(",",row.keySet());jdbc.update("INSERT INTO "+table+" ("+columns+") VALUES ("+String.join(",",Collections.nCopies(row.size(),"?"))+")",row.values().toArray());}
  Map<String,Object> row(Object... values){var m=new LinkedHashMap<String,Object>();for(int i=0;i<values.length;i+=2)m.put((String)values[i],values[i+1]);return m;}
  Map<String,Object> scoped(String id){return row("organizationId",org,"ID",id,"revision",0,"createdAt",Timestamp.from(AS_OF.minusSeconds(10)),"recordedAt",Timestamp.from(KNOWN.minusSeconds(10)));}
  Map<String,Object> workRow(String id){var r=scoped(id);r.put("effectiveAt",Timestamp.from(AS_OF));return r;}
  @BeforeEach void seed(){
    org=id();actor=id();product=id();item=id();spec=id();pack=id();manufacturer=id();lot=id();place=id();segment=id();definition=id();work=id();goal=id();obligation=id();document=id();
    insert("mulino_identity_Organizations",row("ID",org,"externalAlias",org));
    insert("mulino_identity_Actors",row("organizationId",org,"ID",actor,"kind","HUMAN","stableRequestOwner",id()));
    insert("mulino_identity_ExternalIdentities",row("organizationId",org,"ID",id(),"actorId",actor,"issuer","https://mulino.local.invalid","subject","writer-a","organizationAlias",org));
    insert("mulino_identity_Memberships",row("organizationId",org,"ID",id(),"actorId",actor,"validFrom",Timestamp.from(Instant.now().minusSeconds(3600)),"validUntil",Timestamp.from(Instant.now().plusSeconds(3600))));
    String grant=id();insert("mulino_identity_Grants",row("organizationId",org,"ID",grant,"actorId",actor,"delegatorId",actor,"validFrom",Timestamp.from(Instant.now().minusSeconds(3600)),"validUntil",Timestamp.from(Instant.now().plusSeconds(3600))));
    for(String cap:List.of("getObject","searchObjects","getWork","searchWorks","getInventory","getEvidence","getAssessment","getObligations","getDefinition")){
      insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",id(),"actorId",actor,"capabilityId",cap,"scopeKind","ORGANIZATION","scopeId",org,"validFrom",Timestamp.from(Instant.now().minusSeconds(3600)),"validUntil",Timestamp.from(Instant.now().plusSeconds(3600))));
      insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",grant,"capabilityId",cap));
    }
    insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",grant,"scopeKind","ORGANIZATION","scopeId",org));
    var p=scoped(product);p.put("name","integration item");insert("mulino_inventory_Products",p);
    var s=scoped(spec);s.putAll(row("productId",product,"version","1","contentHash","1".repeat(64)));insert("mulino_inventory_SpecificationVersions",s);
    var pk=scoped(pack);pk.putAll(row("productId",product,"version","1","contentHash","2".repeat(64)));insert("mulino_inventory_PackagingVersions",pk);
    var i=scoped(item);i.putAll(row("productId",product,"name","integration item","baseUnit","BOX","decimalPlaces",0,"specificationVersionId",spec,"packagingVersionId",pack));insert("mulino_inventory_TradeItems",i);
    var m=scoped(manufacturer);m.put("name","maker");insert("mulino_inventory_Manufacturers",m);
    var l=scoped(lot);l.putAll(row("manufacturerId",manufacturer,"itemId",item,"originalLot","LOT-1"));insert("mulino_inventory_ManufacturingLots",l);
    var loc=scoped(place);loc.putAll(row("name","warehouse","kind","WAREHOUSE"));insert("mulino_inventory_Places",loc);
    var seg=scoped(segment);seg.putAll(row("itemId",item,"lotId",lot,"identificationStatus","CONFIRMED","quantity",new java.math.BigDecimal("100"),"unit","BOX","placeId",place,"controlScope","warehouse","validFrom",Timestamp.from(AS_OF),"mixtureStatus","IDENTIFIED"));insert("mulino_inventory_QuantitySegments",seg);
    try {
      var predicate=Map.<String,Object>of("operator","quantitySum","property","Receipt.quantity","minimum",Map.of("value","1","unit","BOX"),"unit","BOX","evidenceSelector","VERIFIED_DISTINCT");
      var d=new com.mulino.domain.definitions.Definition(org,definition,"1.0.0",null,"PUBLISHED","", "core-v1","1.0.0",List.of(new com.mulino.domain.definitions.Definition.NounType("Receipt",true)),List.of(new com.mulino.domain.definitions.Definition.Attribute("Receipt","quantity",com.mulino.domain.definitions.Definition.ValueType.DECIMAL,null,"BOX",0,1,1,"READ",true)),List.of(),List.of(),List.of(new com.mulino.domain.definitions.Definition.Goal("Arrival","CUMULATIVE_EVENT","ARRIVED","core-v1",predicate)),List.of());
      String content=json.writeValueAsString(d);
      insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",definition,"createdAt",Timestamp.from(AS_OF),"version","1.0.0","state","PUBLISHED","contentHash",com.mulino.domain.definitions.DefinitionRepository.sha256(content),"content",content,"evaluatorVersion","core-v1","schemaVersion","1.0.0"));
    }catch(Exception failure){throw new IllegalStateException(failure);}

    var w=workRow(work);w.putAll(row("itemId",item,"lotId",lot,"definitionVersionId",definition,"kind","PURCHASE","status","WAITING","ownerId",actor,"supervisorId",actor,"waitJson","{\"reason\":\"QC_PENDING\"}"));insert("mulino_work_read_Works",w);
    var g=workRow(goal);g.putAll(row("workId",work,"definitionVersionId",definition,"quantityMode","CUMULATIVE_EVENT","targetQuantity",new java.math.BigDecimal("100"),"unit","BOX","endpoint","ARRIVED","scopeJson","{}"));insert("mulino_work_read_GoalReferences",g);
    var o=workRow(obligation);o.putAll(row("workId",work,"kind","QC_REVIEW","status","OPEN","ownerId",actor,"supervisorId",actor,"nextAction","검사 근거 확인","nextCheckAt",Timestamp.from(KNOWN.plusSeconds(3600)),"scopeJson","{}"));insert("mulino_work_read_ObligationReferences",o);
    profile=id();insert("mulino_evidence_SourceProfiles",row("ID",profile,"organizationId",org,"revision",1,"recordedAt",Timestamp.from(KNOWN),"recordedBy",actor,"namespace","native-fixture","policyVersion","fixture-v1","intakeOwnerId",actor,"supervisorId",actor,"nextAction","review","nextCheckAt",Timestamp.from(KNOWN.plusSeconds(3600))));
    insert("mulino_evidence_DocumentVersions",row("ID",document,"organizationId",org,"revision",1,"recordedAt",Timestamp.from(KNOWN),"recordedBy",actor,"subjectKind","ITEM","subjectId",item,"itemId",item,"sha256","3".repeat(64),"availability","UNKNOWN","byteLength",0L,"mediaType","text/plain","sourceProfileId",profile,"provenance","{}"));
    var e=workRow(id());e.putAll(row("workId",work,"documentVersionId",document,"role","QC"));insert("mulino_work_read_EvidenceReferences",e);
    String policy=id();insert("mulino_governance_PolicyVersions",row("organizationId",org,"ID",policy,"version","fixture-v1","kind","EVIDENCE","content","{}","contentHash",com.mulino.domain.definitions.DefinitionRepository.sha256("{}"),"createdAt",LocalDateTime.ofInstant(AS_OF.minusSeconds(100),ZoneOffset.UTC),"effectiveFrom",LocalDateTime.ofInstant(AS_OF.minusSeconds(100),ZoneOffset.UTC)));
    if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass('mulino_governance_activepolicies') IS NOT NULL",Boolean.class)))jdbc.update("INSERT INTO mulino_governance_ActivePolicies(organizationId,kind,policyId,revision) VALUES(?,'EVIDENCE',?,0)",org,policy);
    login();
  }
  void login(){SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("trusted-test-context").header("alg","RS256").subject("writer-a").issuer("https://mulino.local.invalid").claim("organizationId",org).claim("stableRequestOwner","forged-owner-ignored").build(),List.of()));}
  @AfterEach void clear(){SecurityContextHolder.clearContext();}
  @AfterAll static void stop()throws Exception{PG.stop();Files.deleteIfExists(KEY_PATH);try(var paths=Files.walk(BLOB_PATH)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
  Map<String,Object> request(String operation,String target,String snapshot){Map<String,Object> body=row("scope",Map.of("organizationId",org,"itemId",item,"lotId",lot),"asOf",AS_OF.toString(),"knownAt",KNOWN.toString());if(target!=null)body.put("id",target);if(snapshot!=null)body.put("snapshotRef",snapshot);return body;}
  Map<String,Object> read(String operation,String target,String snapshot){return runtime.requestContext().run(c->{return queries.query(QueryRequests.parse(operation,request(operation,target,snapshot)));});}

  DomainContext context(){return new DomainContext(org,actor,actor,Instant.now().plusSeconds(60),Instant.now().plusSeconds(60));}
  Map<String,Object> command(String op,int rev,Map<String,Object> slots){return row("capabilityId",op,"expectedRevision",rev,"slots",slots);}
  Map<String,Object> run(String op,int rev,Map<String,Object> slots){return runtime.requestContext().run(ctx->{var template=new org.springframework.transaction.support.TransactionTemplate(transactions);return template.execute(tx->lifecycle.execute(context(),command(op,rev,slots)));});}
  Map<String,Object> goalInputs(){return row("quantityMode","CUMULATIVE_EVENT","targetQuantity","100","unit","BOX","endpoint","ARRIVED","dueAt","2026-10-31T00:00:00Z","scope",Map.of("itemId",item),"timezone","Asia/Seoul","evidencePolicyVersion","fixture-v1","periodStart","2026-10-01T00:00:00Z","periodEnd","2026-10-31T00:00:00Z","eventKind","RECEIPT","contributionScope",Map.of("itemId",item),"deduplication","CANONICAL_OCCURRENCE","conditions",List.of(Map.of("id","arrived")),"evaluatorVersion","core-v1");}
  String draft(Map<String,Object> goal){var result=run("createDraft",0,row("itemId",item,"definitionVersionId",definition,"kind","PURCHASE","ownerId",actor,"supervisorId",actor,"goal",goal));return String.valueOf(((Map<?,?>)result.get("effects")).get("workId"));}

  Map<String,Object> assess(String id){return runtime.requestContext().run(ctx->new org.springframework.transaction.support.TransactionTemplate(transactions).execute(tx->assessmentService.assess(context(),id)));}
  int revision(String id){return jdbc.queryForObject("SELECT revision FROM mulino_work_read_Works WHERE ID=?",Integer.class,id);}
  void occurrence(String workId,String amount,String externalId){
    String event=id(),claim=id(),canonical=id(),doc=id();byte[] bytes=(externalId+amount).getBytes();var blob=blobs.stage(bytes,com.mulino.adapters.blob.LocalBlobStore.hash(bytes));blobs.commit(blob);Instant now=Instant.now().minusSeconds(2);
    var d=row("organizationId",org,"ID",doc,"revision",1,"recordedAt",Timestamp.from(now),"recordedBy",actor,"subjectKind","WORK","subjectId",workId,"itemId",item,"workId",workId,"sha256",blob.sha256(),"blobId",blob.id().toString(),"byteLength",blob.size(),"mediaType","text/plain","sourceNamespace","native-fixture","sourceProfileId",profile,"availability","AVAILABLE","provenance","{}");insert("mulino_evidence_DocumentVersions",d);
    var e=row("organizationId",org,"ID",event,"revision",1,"recordedAt",Timestamp.from(now),"recordedBy",actor,"subjectKind","WORK","subjectId",workId,"itemId",item,"workId",workId,"effectiveFrom",Timestamp.from(now),"timeZone","UTC","timePrecision","SECOND","valueState","KNOWN","kind","RECEIPT","sourceNamespace","native-fixture","sourceProfileId",profile,"externalEventId",externalId,"sourceVersion","1","payloadHash",blob.sha256(),"payload","{}");insert("mulino_evidence_Events",e);
    var in=row("organizationId",org,"ID",id(),"revision",1,"recordedAt",Timestamp.from(now),"recordedBy",actor,"subjectKind","WORK","subjectId",workId,"itemId",item,"workId",workId,"eventId",event,"sourceNamespace","native-fixture","sourceProfileId",profile,"externalEventId",externalId,"sourceVersion","1","payloadHash",blob.sha256(),"state","RECEIVED","intakeOwnerId",actor,"supervisorId",actor,"nextAction","compare","nextCheckAt",Timestamp.from(now.plusSeconds(3600)));insert("mulino_evidence_InboxRecords",in);
    var c=row("organizationId",org,"ID",claim,"revision",1,"recordedAt",Timestamp.from(now),"recordedBy",actor,"sourceProfileId",profile,"subjectKind","WORK","subjectId",workId,"itemId",item,"workId",workId,"effectiveFrom",Timestamp.from(now),"timeZone","UTC","timePrecision","SECOND","valueState","KNOWN","eventId",event,"documentVersionId",doc,"assertion","receipt","quantity",new java.math.BigDecimal(amount),"unit","BOX");insert("mulino_evidence_Claims",c);
    var o=row("organizationId",org,"ID",canonical,"revision",1,"recordedAt",Timestamp.from(now),"recordedBy",actor,"sourceProfileId",profile,"subjectKind","WORK","subjectId",workId,"itemId",item,"workId",workId,"effectiveFrom",Timestamp.from(now),"timeZone","UTC","timePrecision","SECOND","valueState","KNOWN","kind","RECEIPT","physicalScopeId",id(),"quantity",new java.math.BigDecimal(amount),"unit","BOX","reassessmentState","PENDING");insert("mulino_evidence_CanonicalOccurrences",o);
    var v=row("organizationId",org,"ID",id(),"revision",1,"recordedAt",Timestamp.from(now),"recordedBy",actor,"claimId",claim,"canonicalOccurrenceId",canonical,"basisDocumentId",doc,"policyVersion","fixture-v1","sourceMatched",true,"identityMatched",true,"quantityMatched",true,"timeMatched",true,"duplicateChecked",true,"verdict","VERIFIED","reason","synthetic native verified fixture");insert("mulino_evidence_Verifications",v);
  }
  @Test @SuppressWarnings("unchecked") void realAssessment90CannotCloseBut100CanAndTwoReadEntrypointsShareWorld(){String id=draft(goalInputs());run("activateWork",0,Map.of("workId",id));occurrence(id,"90","receipt90");var first=(Map<String,Object>)assess(id).get("assessment");assertEquals("UNSATISFIED",first.get("outcome"));assertThrows(DomainError.class,()->run("closeWork",revision(id),Map.of("workId",id,"reason","FULFILLED")));assertEquals("ACTIVE",jdbc.queryForObject("SELECT status FROM mulino_work_read_Works WHERE ID=?",String.class,id));occurrence(id,"10","receipt10");var current=(Map<String,Object>)assess(id).get("assessment");assertEquals("SATISFIED",current.get("outcome"));run("closeWork",revision(id),Map.of("workId",id,"reason","FULFILLED"));assertEquals("CLOSED",jdbc.queryForObject("SELECT status FROM mulino_work_read_Works WHERE ID=?",String.class,id));assertEquals("UNSATISFIED",jdbc.queryForObject("SELECT outcome FROM mulino_work_read_AssessmentReferences WHERE ID=?",String.class,first.get("ID")));Instant asOf=Instant.now().plusSeconds(60),known=asOf;var query=row("scope",Map.of("organizationId",org,"itemId",item),"asOf",asOf.toString(),"knownAt",known.toString());var noun=runtime.requestContext().run(ctx->queries.query(QueryRequests.parse("getObject",new LinkedHashMap<>(query))));query.put("id",id);var verb=runtime.requestContext().run(ctx->queries.query(QueryRequests.parse("getWork",new LinkedHashMap<>(query))));var nounData=(Map<String,Object>)noun.get("data");var verbData=(Map<String,Object>)verb.get("data");assertTrue(((List<Map<String,Object>>)nounData.get("workReferences")).stream().anyMatch(w->id.equals(w.get("ID"))&&"CLOSED".equals(w.get("status"))));assertEquals(verbData.get("goals"),((List<Map<String,Object>>)nounData.get("goalReferences")).stream().filter(g->id.equals(g.get("workId"))).toList());assertEquals(verbData.get("assessments"),((List<Map<String,Object>>)nounData.get("assessmentReferences")).stream().filter(a->id.equals(a.get("workId"))).toList());assertFalse(((List<?>)verbData.get("assessmentInputSnapshots")).isEmpty());}
}
