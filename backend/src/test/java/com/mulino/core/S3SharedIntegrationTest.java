package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.*;
import com.mulino.application.trade.TradeImpact;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.definitions.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
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
@Import(S3SharedIntegrationTest.Configuration.class)
class S3SharedIntegrationTest {
  static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static final java.security.KeyPair KEY;
  static final Path KEY_PATH;
  static final Path BLOB_PATH;
  static { try {PG.start();var generator=java.security.KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KEY=generator.generateKeyPair();BLOB_PATH=Files.createTempDirectory("mulino-s3-shared-blob-");KEY_PATH=Files.createTempFile("mulino-s3-shared-public-",".pem");Files.writeString(KEY_PATH,"-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(KEY.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----\n");}catch(Exception failure){throw new ExceptionInInitializerError(failure);} }
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);r.add("JWT_PUBLIC_KEY",KEY_PATH::toString);r.add("JWT_ISSUER",()->"https://mulino.local.invalid");r.add("JWT_AUDIENCE",()->"mulino-platform");r.add("mulino.evidence.blob-root",BLOB_PATH::toString);}
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
  void seed(){
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
    insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",definition,"createdAt",Timestamp.from(AS_OF),"version","integration-v1","state","PUBLISHED","contentHash","44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a","content","{}","evaluatorVersion","read-v1","schemaVersion","1"));
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
  @Autowired ApplicationCommands commands;
  @Autowired TradeImpact impact;
  @BeforeEach void commandContract()throws Exception {
    seed();
    insert("mulino_identity_AuthorityFences",row("organizationId",org,"actorId",actor,"revision",1));
    // Same authoritative COMMAND Work setup as ReceiptGatewayPostgresTest;
    // imported S1 rows remain immutable and are not rewritten by this fixture.
    work=id();
    var authoritative=workRow(work);
    authoritative.putAll(row("itemId",item,"lotId",lot,"definitionVersionId",definition,"kind","REVIEW","status","ACTIVE","ownerId",actor,"supervisorId",actor,"lifecycleMode","COMMAND"));
    insert("mulino_work_read_Works",authoritative);
    String grant=jdbc.queryForObject("SELECT ID FROM mulino_identity_Grants WHERE organizationId=?",String.class,org);
    insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",id(),"actorId",actor,"capabilityId","s3SharedImpact","scopeKind","ORGANIZATION","scopeId",org,"validFrom",Timestamp(Instant.now().minusSeconds(3600)),"validUntil",Timestamp(Instant.now().plusSeconds(3600))));
    insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",grant,"capabilityId","s3SharedImpact"));
    var caps=List.of(new Definition.Capability("s3SharedImpact","1.0.0","core-v1","1.0.0","1.0.0",List.of()));
    var d=new Definition(org,id(),"s3-shared-v1",null,"PUBLISHED",null,"core-v1","1.0.0",List.of(new Definition.NounType("Work",true)),List.of(),List.of(new Definition.Verb("s3SharedImpact","COMMAND","s3SharedImpact","ACTIVE",Map.of())),List.of(),List.of(),caps);
    String body=json.writeValueAsString(d);
    insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",d.id(),"createdAt",Timestamp(Instant.now()),"version",d.version(),"state","PUBLISHED","contentHash",DefinitionRepository.sha256(body),"content",body,"evaluatorVersion","core-v1","schemaVersion","1.0.0"));
    String policy=id(),content="{\"rules\":{\"s3SharedImpact\":{\"effectClass\":\"TRADE_IMPACT\"}}}";
    insert("mulino_governance_PolicyVersions",row("organizationId",org,"ID",policy,"revision",1,"createdAt",Timestamp(Instant.now()),"version","s3-shared-v1","kind","COMMAND","content",content,"contentHash",DefinitionRepository.sha256(content),"effectiveFrom",Timestamp(Instant.now().minusSeconds(60)),"effectiveUntil",Timestamp(Instant.now().plusSeconds(3600))));
    insert("mulino_governance_ActivePolicies",row("organizationId",org,"kind","COMMAND","policyId",policy,"revision",1));
  }
  java.sql.Timestamp Timestamp(Instant time){return java.sql.Timestamp.from(time);}
  Map<String,Object> command(boolean fail,String key){return row("intentKind","COMMAND","definitionVersion","s3-shared-v1","capabilityId","s3SharedImpact","commandIdempotencyKey",key,"subjectRefs",List.of(Map.of("type","Work","id",work)),"slots",Map.of("workId",work,"sourceId","regulatory-version-1","physicalScopeId",segment,"fail",fail),"provenance",Map.of("sourceNamespace","USER"),"expectedRevision",jdbc.queryForObject("SELECT revision FROM mulino_work_read_Works WHERE organizationId=? AND ID=?",Integer.class,org,work));}
  Map<String,Object> execute(Map<String,Object> input){return runtime.requestContext().run(c->{return commands.execute(input);});}
  @Test void gatewayDutyAndAssessmentPendingCommitOnceAndCurrentGrantReplayIsDenied(){
    var input=command(false,"once");var first=execute(input);assertEquals("APPLIED",first.get("outcome"),first.toString());assertEquals(first,execute(input));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_responsibility_Roots WHERE organizationId=?",Integer.class,org));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_work_read_ObligationReferences WHERE organizationId=? AND rootId IS NOT NULL AND ownerId=? AND nextAction IS NOT NULL AND nextCheckAt IS NOT NULL",Integer.class,org,actor));
    assertEquals(true,jdbc.queryForObject("SELECT pendingInvalidation FROM mulino_work_read_Works WHERE organizationId=? AND ID=?",Boolean.class,org,work));
    assertEquals(first.get("commandId"),jdbc.queryForObject("SELECT sourceId FROM mulino_responsibility_Roots WHERE organizationId=?",String.class,org));
    jdbc.update("DELETE FROM mulino_identity_GrantActions WHERE organizationId=? AND capabilityId='s3SharedImpact'",org);
    assertEquals("FORBIDDEN",((Map<?,?>)execute(input).get("error")).get("code"));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_responsibility_Roots WHERE organizationId=?",Integer.class,org));
    assertEquals("COMMITTED",jdbc.queryForObject("SELECT state FROM mulino_commands_CommandRecords WHERE ID=?",String.class,first.get("commandId")));
  }
  @Test void domainFailureRollsBackDutyPendingAndTransitionsTogether(){
    var rejected=execute(command(true,"fail"));assertEquals("HELD",rejected.get("outcome"),rejected.toString());
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_responsibility_Roots WHERE organizationId=?",Integer.class,org));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_work_WorkTransitions WHERE organizationId=?",Integer.class,org));
    assertEquals(false,jdbc.queryForObject("SELECT pendingInvalidation FROM mulino_work_read_Works WHERE organizationId=? AND ID=?",Boolean.class,org,work));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_QuantityMovements WHERE organizationId=?",Integer.class,org));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_commands_CommandAudits WHERE organizationId=? AND outcome='HELD'",Integer.class,org));
  }
  @Test void distinctExpiryBoundariesHaveSeparateStableDuties(){
    var applied=execute(command(false,"origin"));assertEquals("APPLIED",applied.get("outcome"),applied.toString());String source=applied.get("commandId").toString();
    var transaction=new org.springframework.transaction.support.TransactionTemplate(transactions);
    for(String boundary:List.of("permission-v1:2026-10-08T00:00:00Z","permission-v1:2026-10-08T00:00:00Z","permission-v2:2026-10-09T00:00:00Z"))transaction.executeWithoutResult(s->runtime.requestContext().run(ctx->{Instant now=Instant.now();impact.recorded(new DomainContext(org,actor,actor,now,now),work,boundary,"VALIDITY_EXPIRED",segment,"Review expiry",now.plusSeconds(3600),source);return null;}));
    assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM mulino_responsibility_Roots WHERE organizationId=?",Integer.class,org));
    assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_responsibility_Roots WHERE organizationId=? AND kind='VALIDITY_EXPIRED'",Integer.class,org));
  }
  @Test void nounAndVerbShareWorldSnapshotAndPreserveCustomerContext(){
    Instant now=Instant.now();String customer=id();
    var base=row("scope",Map.of("itemId",item,"customerId",customer),"asOf",now.toString(),"knownAt",now.toString());
    var noun=new LinkedHashMap<String,Object>(base);noun.put("id",item);noun.put("scope",Map.of("itemId",item,"customerId",customer,"objectType","TradeItem"));
    var left=runtime.requestContext().run(c->{return queries.query(QueryRequests.parse("getObject",noun));});
    var right=runtime.requestContext().run(c->{return queries.query(QueryRequests.parse("getInventory",base));});
    assertEquals(left.get("snapshotRevision"),right.get("snapshotRevision"));
    assertEquals(customer,((Map<?,?>)right.get("scope")).get("customerId"));
    var other=new LinkedHashMap<String,Object>(base);other.put("scope",Map.of("itemId",item,"customerId",id()));
    var changed=runtime.requestContext().run(c->{return queries.query(QueryRequests.parse("getInventory",other));});
    assertNotEquals(right.get("snapshotRevision"),changed.get("snapshotRevision"));
    assertEquals(((Map<?,?>)left.get("data")).get("eligibleQuantity"),((Map<?,?>)right.get("data")).get("eligibleQuantity"));
  }
  @TestConfiguration static class Configuration {
    @Bean CommandHandler sharedImpactHandler(TradeImpact impact,WorkAccess works){return new CommandHandler(){
      public Set<String> capabilities(){return Set.of("s3SharedImpact");}
      public CommandPreparation prepare(DomainContext c,Map<String,Object> intent){var p=(Map<String,Object>)intent.get("slots");String id=p.get("workId").toString();var w=works.require(c,id,false);return CommandPreparation.ordinary(Map.of("WORK",List.of(id),"ITEM",List.of(w.get("itemId").toString())),List.of("work:"+id),"TRADE_IMPACT",id,((Number)w.get("revision")).intValue());}
      public List<SubjectBinding> subjectBindings(DomainContext c,Map<String,Object> intent,CommandPreparation p){return List.of(SubjectBinding.required("Work",Set.of(p.targetId())));}
      public Map<String,Object> execute(DomainContext c,Map<String,Object> intent){var p=(Map<String,Object>)intent.get("slots");String id=p.get("workId").toString();impact.recorded(c,id,p.get("sourceId").toString(),"REGULATORY_REVIEW",p.get("physicalScopeId").toString(),"Review regulatory evidence",Instant.now().plusSeconds(3600));if(Boolean.TRUE.equals(p.get("fail")))throw new DomainError("HELD","FIXTURE_FAILURE","Rollback shared impact");return Map.of("outcome","APPLIED","effects",Map.of("workId",id));}
    };}
  }
}
