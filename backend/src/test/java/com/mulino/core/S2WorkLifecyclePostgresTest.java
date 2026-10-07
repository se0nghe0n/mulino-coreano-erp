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
class S2WorkLifecyclePostgresTest {
  static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static final java.security.KeyPair KEY;
  static final Path KEY_PATH;
  static final Path BLOB_PATH;
  static { try {PG.start();var generator=java.security.KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KEY=generator.generateKeyPair();BLOB_PATH=Files.createTempDirectory("mulino-s1-read-blob-");KEY_PATH=Files.createTempFile("mulino-s1-read-public-",".pem");Files.writeString(KEY_PATH,"-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(KEY.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----\n");}catch(Exception failure){throw new ExceptionInInitializerError(failure);} }
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);r.add("JWT_PUBLIC_KEY",KEY_PATH::toString);r.add("JWT_ISSUER",()->"https://mulino.local.invalid");r.add("JWT_AUDIENCE",()->"mulino-platform");r.add("mulino.evidence.blob-root",BLOB_PATH::toString);}
  @Autowired com.mulino.application.work.WorkLifecycle lifecycle;
  @Autowired com.mulino.domain.work.WorkRepository workRepository;
  @org.springframework.test.context.bean.override.mockito.MockitoBean com.mulino.application.work.WorkAssessmentGuard assessmentGuard;
  @org.springframework.test.context.bean.override.mockito.MockitoBean com.mulino.application.work.WorkResponsibility dutyGuard;
  @Autowired JdbcTemplate jdbc;
  @Autowired ApplicationQueries queries;
  @Autowired PersistenceService persistence;
  @Autowired CdsRuntime runtime;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
  @LocalServerPort int port;
  final ObjectMapper json=new ObjectMapper();
  String org,actor,product,item,spec,pack,manufacturer,lot,place,segment,definition,work,goal,obligation,document;
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
      var d=new com.mulino.domain.definitions.Definition(org,definition,"1.0.0",null,"PUBLISHED","", "core-v1","1.0.0",List.of(),List.of(),List.of(),List.of(),List.of(),List.of());
      String content=json.writeValueAsString(d);
      insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",definition,"createdAt",Timestamp.from(AS_OF),"version","1.0.0","state","PUBLISHED","contentHash",com.mulino.domain.definitions.DefinitionRepository.sha256(content),"content",content,"evaluatorVersion","core-v1","schemaVersion","1.0.0"));
    }catch(Exception failure){throw new IllegalStateException(failure);}

    var w=workRow(work);w.putAll(row("itemId",item,"lotId",lot,"definitionVersionId",definition,"kind","PURCHASE","status","WAITING","ownerId",actor,"supervisorId",actor,"waitJson","{\"reason\":\"QC_PENDING\"}"));insert("mulino_work_read_Works",w);
    var g=workRow(goal);g.putAll(row("workId",work,"definitionVersionId",definition,"quantityMode","CUMULATIVE_EVENT","targetQuantity",new java.math.BigDecimal("100"),"unit","BOX","endpoint","ARRIVED","scopeJson","{}"));insert("mulino_work_read_GoalReferences",g);
    var o=workRow(obligation);o.putAll(row("workId",work,"kind","QC_REVIEW","status","OPEN","ownerId",actor,"supervisorId",actor,"nextAction","검사 근거 확인","nextCheckAt",Timestamp.from(KNOWN.plusSeconds(3600)),"scopeJson","{}"));insert("mulino_work_read_ObligationReferences",o);
    String profile=id();insert("mulino_evidence_SourceProfiles",row("ID",profile,"organizationId",org,"revision",1,"recordedAt",Timestamp.from(KNOWN),"recordedBy",actor,"namespace","native-fixture","policyVersion","read-v1","intakeOwnerId",actor,"supervisorId",actor,"nextAction","review","nextCheckAt",Timestamp.from(KNOWN.plusSeconds(3600))));
    insert("mulino_evidence_DocumentVersions",row("ID",document,"organizationId",org,"revision",1,"recordedAt",Timestamp.from(KNOWN),"recordedBy",actor,"subjectKind","ITEM","subjectId",item,"itemId",item,"sha256","3".repeat(64),"availability","UNKNOWN","byteLength",0L,"mediaType","text/plain","sourceProfileId",profile,"provenance","{}"));
    var e=workRow(id());e.putAll(row("workId",work,"documentVersionId",document,"role","QC"));insert("mulino_work_read_EvidenceReferences",e);
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
  @Test void draftMissingGoalHasNoActivationEffect(){String id=draft(Map.of());var error=assertThrows(DomainError.class,()->run("activateWork",0,Map.of("workId",id)));assertEquals("NEEDS_INPUT",error.outcome());assertEquals("DRAFT",jdbc.queryForObject("SELECT status FROM mulino_work_read_Works WHERE ID=?",String.class,id));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_work_WorkTransitions WHERE workId=?",Integer.class,id));assertNull(jdbc.queryForObject("SELECT quantityMode FROM mulino_work_read_GoalReferences WHERE workId=?",String.class,id));}
  @Test void lifecycleWaitDoesNotResumeFromElapsedTimeAndRevisionHistoryIsImmutable(){String id=draft(goalInputs());run("activateWork",0,Map.of("workId",id));var wait=row("reason","WAIT_RECEIPT","target",item,"resumePredicate",Map.of("operator","exists","property","receipt"),"verifier","WAREHOUSE","nextCheckAt","2020-01-01T00:00:00Z","overdueAction","ESCALATE");run("waitWork",1,Map.of("workId",id,"wait",wait));doThrow(new DomainError("HELD","MISSING_RESUME_EVIDENCE","No actual receipt")).when(assessmentGuard).requireResume(any(),any(),any(),any());assertThrows(DomainError.class,()->run("resumeWork",2,Map.of("workId",id,"evidence",Map.of())));assertEquals("WAITING",jdbc.queryForObject("SELECT status FROM mulino_work_read_Works WHERE ID=?",String.class,id));assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM mulino_work_WorkTransitions WHERE workId=?",Integer.class,id));assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE mulino_work_WorkTransitions SET state='CLOSED' WHERE workId=?",id));}
  @Test void revisionPreservesPreviousGoalAndClosedWorkNeverReopens(){String id=draft(goalInputs());run("activateWork",0,Map.of("workId",id));String first=jdbc.queryForObject("SELECT currentGoalVersionId FROM mulino_work_read_Works WHERE ID=?",String.class,id);var goal=goalInputs();goal.put("targetQuantity","120");run("reviseGoal",1,row("workId",id,"goal",goal,"reason","New demand"));assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_work_read_GoalReferences WHERE workId=?",Integer.class,id));assertEquals(first,jdbc.queryForObject("SELECT previousGoalId FROM mulino_work_read_GoalReferences WHERE workId=? AND goalVersion=2",String.class,id));assertEquals("HELD",assertThrows(DomainError.class,()->run("closeWork",2,Map.of("workId",id,"reason","FULFILLED"))).outcome());run("closeWork",2,Map.of("workId",id,"reason","SUPERSEDED"));assertThrows(DomainError.class,()->run("reviseGoal",3,row("workId",id,"goal",goal,"reason","reopen")));assertEquals("CLOSED",jdbc.queryForObject("SELECT status FROM mulino_work_read_Works WHERE ID=?",String.class,id));}
  @Test void remainingDutyAndUnsatisfiedGoalIndependentlyPreventFulfilledClose(){String id=draft(goalInputs());run("activateWork",0,Map.of("workId",id));doThrow(new DomainError("HELD","DUTY_OPEN","Remaining10")).when(dutyGuard).requireSettled(any(),eq(id));assertThrows(DomainError.class,()->run("closeWork",1,Map.of("workId",id,"reason","FULFILLED")));doNothing().when(dutyGuard).requireSettled(any(),eq(id));doThrow(new DomainError("HELD","GOAL_UNSATISFIED","90 is below100")).when(assessmentGuard).requireFulfilled(any(),any(),any());assertThrows(DomainError.class,()->run("closeWork",1,Map.of("workId",id,"reason","FULFILLED")));assertEquals("ACTIVE",jdbc.queryForObject("SELECT status FROM mulino_work_read_Works WHERE ID=?",String.class,id));assertEquals(1,jdbc.queryForObject("SELECT revision FROM mulino_work_read_Works WHERE ID=?",Integer.class,id));}
  @Test void dependencyCycleRollsBackButSharedActivityIsNotGoalQuantity(){String a=draft(goalInputs()),b=draft(goalInputs());run("createWorkLink",0,row("workId",a,"targetWorkId",b,"kind","DEPENDS_ON"));assertThrows(DomainError.class,()->run("createWorkLink",0,row("workId",b,"targetWorkId",a,"kind","DEPENDS_ON")));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_work_WorkLinks WHERE organizationId=?",Integer.class,org));run("createWorkLink",0,row("workId",b,"targetWorkId",a,"kind","SHARES_ACTIVITY"));assertEquals(new java.math.BigDecimal("100.000000000000"),jdbc.queryForObject("SELECT targetQuantity FROM mulino_work_read_GoalReferences WHERE workId=?",java.math.BigDecimal.class,a));}
}
