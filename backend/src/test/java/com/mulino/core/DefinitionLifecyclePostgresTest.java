package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.domain.definitions.Definition;
import com.mulino.domain.definitions.DefinitionRepository;
import com.sap.cds.services.runtime.CdsRuntime;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Plan §8 package lifecycle on the stored Definition shape (user decision 2026-10-11 (2)), through the command pipeline. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class DefinitionLifecyclePostgresTest {
  static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static final Path KEY_PATH;
  static { try {PG.start();var generator=java.security.KeyPairGenerator.getInstance("RSA");generator.initialize(2048);var key=generator.generateKeyPair();KEY_PATH=Files.createTempFile("mulino-definition-public-",".pem");Files.writeString(KEY_PATH,"-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(key.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----\n");}catch(Exception failure){throw new ExceptionInInitializerError(failure);} }
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);r.add("JWT_PUBLIC_KEY",KEY_PATH::toString);r.add("JWT_ISSUER",()->"https://mulino.local.invalid");r.add("JWT_AUDIENCE",()->"mulino-platform");}
  @AfterAll static void stop()throws Exception{PG.stop();Files.deleteIfExists(KEY_PATH);}
  @Autowired JdbcTemplate jdbc;@Autowired ApplicationCommands commands;@Autowired ApplicationQueries queries;@Autowired CdsRuntime runtime;
  final ObjectMapper json=new ObjectMapper();
  static final List<String> LIFECYCLE=List.of("createDefinitionDraft","validateDefinition","submitDefinitionReview","approveDefinition","publishDefinition","activateDefinition","retireDefinition");
  String org,fde,approver,def1;
  String id(){return UUID.randomUUID().toString();}
  Map<String,Object> row(Object... v){var m=new LinkedHashMap<String,Object>();for(int i=0;i<v.length;i+=2)m.put((String)v[i],v[i+1]);return m;}
  void insert(String table,Map<String,Object> row){jdbc.update("INSERT INTO "+table+" ("+String.join(",",row.keySet())+") VALUES ("+String.join(",",Collections.nCopies(row.size(),"?"))+")",row.values().toArray());}
  String human(String subject){String a=id();Timestamp from=Timestamp.from(Instant.now().minusSeconds(3600)),until=Timestamp.from(Instant.now().plusSeconds(3600));
    insert("mulino_identity_Actors",row("organizationId",org,"ID",a,"kind","HUMAN","stableRequestOwner",a));insert("mulino_identity_AuthorityFences",row("organizationId",org,"actorId",a,"revision",1));
    insert("mulino_identity_ExternalIdentities",row("organizationId",org,"ID",id(),"actorId",a,"issuer","https://mulino.local.invalid","subject",subject,"organizationAlias",org));
    insert("mulino_identity_Memberships",row("organizationId",org,"ID",id(),"actorId",a,"validFrom",from,"validUntil",until));
    String grant=id();insert("mulino_identity_Grants",row("organizationId",org,"ID",grant,"actorId",a,"delegatorId",a,"validFrom",from,"validUntil",until));insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",grant,"scopeKind","ORGANIZATION","scopeId",org));
    var caps=new ArrayList<>(LIFECYCLE);caps.add("getDefinition");for(String cap:caps){insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",id(),"actorId",a,"capabilityId",cap,"scopeKind","ORGANIZATION","scopeId",org,"validFrom",from,"validUntil",until));insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",grant,"capabilityId",cap));}
    return a;}
  @BeforeEach void seed()throws Exception{
    org=id();insert("mulino_identity_Organizations",row("ID",org,"externalAlias",org));fde=human("fde-a");approver=human("approver-b");def1=id();
    var d=new Definition(org,def1,"definition-v1",null,"PUBLISHED","","core-v1","1.0.0",List.of(new Definition.NounType("DefinitionPackage",true),new Definition.NounType("TradeItem",true)),List.of(),
        LIFECYCLE.stream().map(c->new Definition.Verb(c,"COMMAND",c,"DEFINITION",Map.of())).toList(),List.of(),List.of(),LIFECYCLE.stream().map(c->new Definition.Capability(c,"1.0.0","core-v1","1.0.0","1.0.0",List.of())).toList());
    String content=json.writeValueAsString(d);insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",def1,"version","definition-v1","state","PUBLISHED","contentHash",DefinitionRepository.sha256(content),"content",content,"evaluatorVersion","core-v1","schemaVersion","1.0.0"));
    var rules=new LinkedHashMap<String,Object>();for(String c:LIFECYCLE)rules.put(c,Map.of("effectClass","DEFINITION"));String rule=json.writeValueAsString(Map.of("rules",rules));String policy=id();
    insert("mulino_governance_PolicyVersions",row("organizationId",org,"ID",policy,"version","definition-fixture-v1","kind","COMMAND","content",rule,"contentHash",DefinitionRepository.sha256(rule),"createdAt",LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1),"effectiveFrom",LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1)));
    jdbc.update("INSERT INTO mulino_governance_ActivePolicies(organizationId,kind,policyId,revision) VALUES(?,'COMMAND',?,0)",org,policy);
  }
  @AfterEach void clear(){SecurityContextHolder.clearContext();}
  void login(String subject){SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("trusted-test-context").header("alg","RS256").subject(subject).issuer("https://mulino.local.invalid").claim("organizationId",org).build(),List.of()));}
  Map<String,Object> pkg(){return row("version","definition-v2","evaluatorVersion","core-v1","schemaVersion","1.0.0","nouns",List.of(Map.of("name","TradeItem","core",true)),"attributes",List.of(),"verbs",List.of(),"relations",List.of(),"goals",List.of(),
      "capabilities",List.of(row("capabilityId","createDefinitionDraft","semanticVersion","1.0.0","evaluatorVersion","core-v1","inputSchemaVersion","1.0.0","outputSchemaVersion","1.0.0","supportedWorkMigration",List.of())));}
  Map<String,Object> run(String subject,String cap,Object revision,Map<String,Object> slots){login(subject);var intent=row("intentKind","COMMAND","definitionVersion","definition-v1","capabilityId",cap,"subjectRefs",List.of(),"slots",slots,"provenance",Map.of(),"commandIdempotencyKey",id());if(revision!=null)intent.put("expectedRevision",revision);return runtime.requestContext().run(c->{return commands.execute(intent);});}
  @SuppressWarnings("unchecked") Map<String,Object> read(String id){login("fde-a");return (Map<String,Object>)runtime.requestContext().run(c->{return queries.query(QueryRequests.parse("getDefinition",row("id",id,"scope",Map.of("organizationId",org))));}).get("data");}
  String code(Map<String,Object> r){return String.valueOf(((Map<?,?>)r.get("error")).get("code"));}
  void applied(Map<String,Object> r){assertEquals("APPLIED",r.get("outcome"),r.toString());}

  @Test void approvedPackageIsPublishedWithItsReviewedHashActivatedByPointerAndRetired(){
    var sql=new LinkedHashMap<>(pkg());sql.put("sql","SELECT 1");
    assertEquals("TYPE_INVALID",code(run("fde-a","createDefinitionDraft",1,row("parentDefinitionId",def1,"package",sql))));
    var operator=new LinkedHashMap<>(pkg());operator.put("goals",List.of(row("name","g","quantityMode","STATE_AT","endpoint","SELL_ELIGIBLE","evaluatorVersion","core-v1","predicate",Map.of("operator","runCode"))));
    assertEquals("TYPE_INVALID",code(run("fde-a","createDefinitionDraft",1,row("parentDefinitionId",def1,"package",operator))));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_definitions_DefinitionVersions WHERE organizationId=?",Integer.class,org));

    var draft=run("fde-a","createDefinitionDraft",1,row("parentDefinitionId",def1,"version","definition-v2","package",pkg(),"regressionFixtureRefs",List.of("T21")));applied(draft);
    String id=String.valueOf(draft.get("definitionId")),hash=String.valueOf(draft.get("hash"));
    assertEquals("DRAFT",draft.get("state"));assertEquals(1,draft.get("revision"));
    assertEquals(hash,DefinitionRepository.sha256(jdbc.queryForObject("SELECT content FROM mulino_definitions_DefinitionVersions WHERE ID=?",String.class,id)));
    assertEquals("DRAFT",read(id).get("state"));

    var validate=run("fde-a","validateDefinition",1,row("definitionId",id,"hash",hash));applied(validate);assertEquals(true,validate.get("publishable"),validate.toString());
    assertEquals("CONFLICT",run("fde-a","submitDefinitionReview",2,row("definitionId",id,"hash","0".repeat(64),"validationId",validate.get("validationId"))).get("outcome"));
    var submit=run("fde-a","submitDefinitionReview",2,row("definitionId",id,"hash",hash,"validationId",validate.get("validationId")));applied(submit);assertEquals("IN_REVIEW",submit.get("state"));
    assertEquals("FORBIDDEN",code(run("fde-a","approveDefinition",3,row("definitionId",id,"hash",hash,"decision","APPROVED","validUntil",Instant.now().plusSeconds(3600).toString()))));
    assertEquals("WAITING_APPROVAL",run("fde-a","publishDefinition",3,row("definitionId",id,"hash",hash)).get("outcome"));
    var approve=run("approver-b","approveDefinition",3,row("definitionId",id,"hash",hash,"decision","APPROVED","validUntil",Instant.now().plusSeconds(3600).toString(),"consumptionPolicy","ONE_PACKAGE_PUBLISH"));applied(approve);
    assertEquals(approver,jdbc.queryForObject("SELECT approverId FROM mulino_definitions_DefinitionApprovals WHERE ID=?",String.class,approve.get("approvalId")));
    var publish=run("fde-a","publishDefinition",4,row("definitionId",id,"hash",hash,"approvalId",approve.get("approvalId")));applied(publish);
    assertEquals("PUBLISHED",publish.get("state"));assertEquals(hash,publish.get("hash"));assertEquals("PUBLISHED",jdbc.queryForObject("SELECT state FROM mulino_definitions_DefinitionVersions WHERE ID=?",String.class,id));
    assertNotNull(jdbc.queryForObject("SELECT consumedAt FROM mulino_definitions_DefinitionApprovals WHERE ID=?",Timestamp.class,approve.get("approvalId")));
    assertEquals("TYPE_INVALID",code(run("fde-a","publishDefinition",5,row("definitionId",id,"hash",hash,"package",pkg()))));
    assertThrows(Exception.class,()->jdbc.update("UPDATE mulino_definitions_DefinitionVersions SET content='{}' WHERE ID=?",id));

    var activate=run("fde-a","activateDefinition",5,row("definitionId",id,"hash",hash));applied(activate);assertEquals("ACTIVE",activate.get("state"));
    assertEquals(id,jdbc.queryForObject("SELECT definitionId FROM mulino_definitions_ActiveDefinitionPointers WHERE organizationId=?",String.class,org));
    assertEquals("ACTIVE",read(id).get("lifecycleState"));assertEquals(6,read(id).get("revision"));
    String def1Hash=jdbc.queryForObject("SELECT contentHash FROM mulino_definitions_DefinitionVersions WHERE ID=?",String.class,def1);
    applied(run("fde-a","activateDefinition",0,row("definitionId",def1,"hash",def1Hash)));
    assertEquals(def1,jdbc.queryForObject("SELECT definitionId FROM mulino_definitions_ActiveDefinitionPointers WHERE organizationId=?",String.class,org));
    assertEquals("PUBLISHED",read(id).get("lifecycleState"));
    var retire=run("fde-a","retireDefinition",6,row("definitionId",id,"reason","다음 버전으로 전환한다"));applied(retire);assertEquals("RETIRED_FOR_NEW_WORK",retire.get("state"));
    assertEquals("HELD",run("fde-a","activateDefinition",7,row("definitionId",id,"hash",hash)).get("outcome"));
    assertEquals(hash,jdbc.queryForObject("SELECT contentHash FROM mulino_definitions_DefinitionVersions WHERE ID=?",String.class,id));
  }
  @Test void rejectionReturnsTheDraftAndARevisionChangesTheHash(){
    var draft=run("fde-a","createDefinitionDraft",1,row("parentDefinitionId",def1,"package",pkg()));applied(draft);String id=String.valueOf(draft.get("definitionId")),hash=String.valueOf(draft.get("hash"));
    var validate=run("fde-a","validateDefinition",1,row("definitionId",id,"hash",hash));applied(validate);
    applied(run("fde-a","submitDefinitionReview",2,row("definitionId",id,"hash",hash,"validationId",validate.get("validationId"))));
    var reject=run("approver-b","approveDefinition",3,row("definitionId",id,"hash",hash,"decision","REJECTED","reason","회귀 근거를 보완한다"));applied(reject);assertEquals("DRAFT",reject.get("state"));
    var revisedPackage=new LinkedHashMap<>(pkg());revisedPackage.put("nouns",List.of(Map.of("name","TradeItem","core",true),Map.of("name","Place","core",true)));
    var revised=run("fde-a","createDefinitionDraft",4,row("definitionId",id,"parentDraftRevision",4,"package",revisedPackage,"regressionFixtureRefs",List.of("T07")));applied(revised);
    assertEquals("DRAFT",revised.get("state"));assertEquals(5,revised.get("revision"));assertNotEquals(hash,revised.get("hash"));
    assertEquals(List.of("REJECTED"),jdbc.queryForList("SELECT decision FROM mulino_definitions_DefinitionApprovals WHERE definitionId=?",String.class,id));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_definitions_ActiveDefinitionPointers WHERE organizationId=?",Integer.class,org));
  }
}
