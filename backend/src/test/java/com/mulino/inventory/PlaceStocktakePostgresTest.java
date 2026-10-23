package com.mulino.inventory;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.core.*;
import com.mulino.domain.definitions.*;
import com.sap.cds.services.runtime.CdsRuntime;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.nio.file.*;
import java.net.*;
import java.net.http.*;
import java.security.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real gateway, current all-of authorization, CQN and PostgreSQL; no scenario/model runner. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class PlaceStocktakePostgresTest {
  static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static final KeyPair KEY;static final Path PUBLIC_KEY;static final Path BLOBS;
  static {try{PG.start();var keys=KeyPairGenerator.getInstance("RSA");keys.initialize(2048);KEY=keys.generateKeyPair();PUBLIC_KEY=Files.createTempFile("mulino-place-count-public-",".pem");BLOBS=Files.createTempDirectory("mulino-place-count-blobs-");Files.writeString(PUBLIC_KEY,"-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(KEY.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----\n");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
    r.add("JWT_PUBLIC_KEY",PUBLIC_KEY::toString);r.add("JWT_ISSUER",()->"https://stocktake.invalid");r.add("JWT_AUDIENCE",()->"stocktake");r.add("mulino.evidence.blob-root",BLOBS::toString);
  }
  @AfterAll static void stop()throws Exception{PG.stop();Files.deleteIfExists(PUBLIC_KEY);try(var paths=Files.walk(BLOBS)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
  @Autowired JdbcTemplate jdbc;
  @Autowired CdsRuntime runtime;
  @Autowired ApplicationCommands commands;
  @Autowired ApplicationQueries queries;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
  @LocalServerPort int port;
  final ObjectMapper json=new ObjectMapper();
  final String org=id(),actor=id(),product=id(),item=id(),place=id(),secondPlace=id(),a=id(),b=id(),grant=id(),manufacturer=id(),lot=id();
  Instant at;
  static String id(){return UUID.randomUUID().toString();}
  static Map<String,Object> row(Object... values){var m=new LinkedHashMap<String,Object>();for(int i=0;i<values.length;i+=2)m.put((String)values[i],values[i+1]);return m;}
  void insert(String table,Map<String,Object> row){jdbc.update("INSERT INTO "+table+" ("+String.join(",",row.keySet())+") VALUES ("+String.join(",",Collections.nCopies(row.size(),"?"))+")",row.values().toArray());}
  @BeforeEach void seed()throws Exception {
    at=Instant.now().minusSeconds(30);
    insert("mulino_identity_Organizations",row("ID",org,"externalAlias",org));
    insert("mulino_identity_Actors",row("organizationId",org,"ID",actor,"kind","HUMAN","stableRequestOwner",actor));
    insert("mulino_identity_AuthorityFences",row("organizationId",org,"actorId",actor,"revision",1));
    insert("mulino_identity_ExternalIdentities",row("organizationId",org,"ID",id(),"actorId",actor,"issuer","https://stocktake.invalid","subject","counter","organizationAlias",org));
    insert("mulino_identity_Memberships",row("organizationId",org,"ID",id(),"actorId",actor,"validFrom",Timestamp.from(at.minusSeconds(60)),"validUntil",Timestamp.from(at.plusSeconds(3600))));
    insert("mulino_identity_Grants",row("organizationId",org,"ID",grant,"actorId",actor,"delegatorId",actor,"validFrom",Timestamp.from(at.minusSeconds(60)),"validUntil",Timestamp.from(at.plusSeconds(3600))));
    for(String cap:List.of("recordStocktake","getObject","searchObjects","getInventory")) {
      insert("mulino_identity_CapabilityAssignments",row("organizationId",org,"ID",id(),"actorId",actor,"capabilityId",cap,"scopeKind","ITEM","scopeId",item,"validFrom",Timestamp.from(at.minusSeconds(60)),"validUntil",Timestamp.from(at.plusSeconds(3600))));
      insert("mulino_identity_GrantActions",row("organizationId",org,"grantId",grant,"capabilityId",cap));
    }
    for(var scope:Map.of("TARGET",List.of(place,a,b),"ITEM",List.of(item),"PLACE",List.of(place)).entrySet())for(String target:scope.getValue())
      insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",grant,"scopeKind",scope.getKey(),"scopeId",target));
    insert("mulino_inventory_Products",row("organizationId",org,"ID",product,"name","test"));
    String spec=id(),pack=id();
    for(String table:List.of("SpecificationVersions","PackagingVersions"))insert("mulino_inventory_"+table,row("organizationId",org,"ID",table.startsWith("Spec")?spec:pack,"productId",product,"version","v1","contentHash","1".repeat(64)));
    insert("mulino_inventory_TradeItems",row("organizationId",org,"ID",item,"productId",product,"name","item","baseUnit","BOX","decimalPlaces",0,"specificationVersionId",spec,"packagingVersionId",pack));
    insert("mulino_inventory_Manufacturers",row("organizationId",org,"ID",manufacturer,"name","maker"));
    insert("mulino_inventory_ManufacturingLots",row("organizationId",org,"ID",lot,"manufacturerId",manufacturer,"itemId",item,"originalLot","count-lot"));
    for(String p:List.of(place,secondPlace))insert("mulino_inventory_Places",row("organizationId",org,"ID",p,"name","warehouse","kind","INTERNAL_STORAGE"));
    segment(a,place,"60");segment(b,place,"40");segment(id(),secondPlace,"20");
    String definition=id();
    var d=new Definition(org,definition,"stocktake-v1",null,"PUBLISHED","","core-v1","1.0.0",
      List.of(new Definition.NounType("TradeItem",true),new Definition.NounType("Place",true)),List.of(),
      List.of(new Definition.Verb("recordStocktake","RECORD","recordStocktake","ITEM",Map.of())),List.of(),List.of(),
      List.of(new Definition.Capability("recordStocktake","1.0.0","core-v1","1.0.0","1.0.0",List.of())));
    String content=json.writeValueAsString(d);
    insert("mulino_definitions_DefinitionVersions",row("organizationId",org,"ID",definition,"version","stocktake-v1","state","PUBLISHED","content",content,"contentHash",DefinitionRepository.sha256(content),"evaluatorVersion","core-v1","schemaVersion","1.0.0"));
    String policy=id(),rules="{\"rules\":{\"recordStocktake\":{\"effectClass\":\"RECORD_STOCKTAKE\"}}}";
    insert("mulino_governance_PolicyVersions",row("organizationId",org,"ID",policy,"version","count-v1","kind","COMMAND","content",rules,"contentHash",DefinitionRepository.sha256(rules),"createdAt",Timestamp.from(at),"effectiveFrom",Timestamp.from(at)));
    insert("mulino_governance_ActivePolicies",row("organizationId",org,"kind","COMMAND","policyId",policy,"revision",0));
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("trusted-test-context").header("alg","RS256").issuer("https://stocktake.invalid").subject("counter").claim("organizationId",org).build(),List.of()));
  }
  void segment(String id,String location,String amount){insert("mulino_inventory_QuantitySegments",row("organizationId",org,"ID",id,"revision",0,"createdAt",Timestamp.from(at),"recordedAt",Timestamp.from(at),"itemId",item,"lotId",lot,"identificationStatus","CONFIRMED","quantity",new java.math.BigDecimal(amount),"unit","BOX","placeId",location,"controlScope","warehouse","validFrom",Timestamp.from(at),"mixtureStatus","IDENTIFIED"));}
  @AfterEach void clear(){SecurityContextHolder.clearContext();}
  Map<String,Object> slots(){return row("itemId",item,"placeId",place,"observedQuantity","98","unit","BOX","occurredAt",at.toString(),"evidenceRef","synthetic-count-sheet-98");}
  Map<String,Object> intent(String key,Map<String,Object> slots){return row("intentKind","RECORD","capabilityId","recordStocktake","definitionVersion","stocktake-v1","capabilityVersion","1.0.0","slots",slots,"provenance",Map.of(),"subjectRefs",List.of(),"commandIdempotencyKey",key);}
  Map<String,Object> execute(Map<String,Object> intent){return runtime.requestContext().run(c->{return commands.execute(intent);});}
  int count(String table){return jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE organizationId=?",Integer.class,org);}
  List<Map<String,Object>> physical(){return jdbc.queryForList("SELECT row_to_json(s)::text FROM mulino_inventory_QuantitySegments s WHERE organizationId=? ORDER BY ID",org);}
  List<Integer> effects(){return List.of("mulino_inventory_QuantityMovements","mulino_inventory_SegmentAllocations","mulino_inventory_StockAdjustments","mulino_responsibility_Roots","mulino_runtime_Outbox").stream().map(this::count).toList();}
  @SuppressWarnings("unchecked") Map<String,Object> read(String id){return runtime.requestContext().run(c->{return queries.query(new QueryRequest("getObject",id,Map.of("organizationId",org,"objectType","PlaceStocktake"),Map.of(),50,null,"1.0.0",Instant.now(),Instant.now(),null));});}
  @Test @SuppressWarnings("unchecked") void records98AgainstBook100WithExactMembersAndNoPhysicalEffectsOrDuplicate(){
    var before=physical();var noEffects=effects();String key=id();var request=intent(key,slots());var result=execute(request);
    assertEquals("APPLIED",result.get("outcome"),result.toString());
    var effect=(Map<String,Object>)result.get("effects");String count=effect.get("placeStocktakeId").toString();assertEquals(List.of(),effect.get("quantityEffects"));
    var stored=jdbc.queryForMap("SELECT * FROM mulino_inventory_PlaceStocktakes WHERE organizationId=? AND ID=?",org,count);
    assertEquals(98,((java.math.BigDecimal)stored.get("observedquantity")).intValueExact());assertEquals(100,((java.math.BigDecimal)stored.get("bookquantity")).intValueExact());
    assertEquals("RECORDED_UNASSESSED",stored.get("observationstatus"));assertEquals(Set.of(a,b),new HashSet<>(jdbc.queryForList("SELECT segmentId FROM mulino_inventory_PlaceStocktakeMembers WHERE organizationId=? AND stocktakeId=?",String.class,org,count)));
    assertEquals(result.get("commandId"),stored.get("commandid"));
    assertEquals(result.get("transactionId"),jdbc.queryForObject("SELECT xmin::text FROM mulino_inventory_PlaceStocktakes WHERE organizationId=? AND ID=?",String.class,org,count));
    assertEquals(before,physical());assertEquals(noEffects,effects());
    assertEquals(result,execute(request));assertEquals(1,count("mulino_inventory_PlaceStocktakes"));
    var changed=slots();changed.put("observedQuantity","97");assertEquals("CONFLICT",execute(intent(key,changed)).get("outcome"));assertEquals(before,physical());assertEquals(noEffects,effects());
    var data=(Map<String,Object>)read(count).get("data");assertEquals("98",data.get("observedQuantity"));assertEquals("100",data.get("bookQuantity"));assertEquals(2,((List<?>)data.get("bookMembers")).size());assertFalse(data.containsKey("sourceMembersJson"));
  }
  @Test void invalidUnitPrecisionMixedSelectorsForeignScopeAndFutureFailWithoutObservationOrStockChange(){
    var before=physical();var noEffects=effects();
    for(var delta:List.of(Map.of("unit","EA"),Map.of("observedQuantity","0.5"),Map.of("segmentId",a),Map.of("placeId",id()),Map.of("occurredAt",Instant.now().plusSeconds(60).toString()),Map.of("lotId",id()))) {
      var s=slots();s.putAll(delta);assertNotEquals("APPLIED",execute(intent(id(),s)).get("outcome"),delta.toString());
      assertEquals(0,count("mulino_inventory_PlaceStocktakes"));assertEquals(0,count("mulino_inventory_PlaceStocktakeMembers"));assertEquals(before,physical());assertEquals(noEffects,effects());
    }
  }
  @Test void hiddenMemberAndCurrentRevocationCannotBeHiddenByAggregateAuthorization(){
    jdbc.update("DELETE FROM mulino_identity_GrantScopes WHERE organizationId=? AND grantId=? AND scopeKind='TARGET' AND scopeId=?",org,grant,b);
    assertNotEquals("APPLIED",execute(intent(id(),slots())).get("outcome"));assertEquals(0,count("mulino_inventory_PlaceStocktakes"));
    insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",grant,"scopeKind","TARGET","scopeId",b));
    var success=execute(intent(id(),slots()));assertEquals("APPLIED",success.get("outcome"),success.toString());
    String count=((Map<?,?>)success.get("effects")).get("placeStocktakeId").toString();
    jdbc.update("UPDATE mulino_identity_Grants SET revokedAt=CURRENT_TIMESTAMP,revision=revision+1 WHERE organizationId=? AND ID=?",org,grant);
    assertThrows(Exception.class,()->read(count));assertNotEquals("APPLIED",execute(intent(id(),slots())).get("outcome"));assertEquals(1,count("mulino_inventory_PlaceStocktakes"));
  }
  @Test void immutableSourceAndMembersRejectEditsDeletesTruncationAndIncompleteDirectInsert(){
    var success=execute(intent(id(),slots()));assertEquals("APPLIED",success.get("outcome"),success.toString());String count=((Map<?,?>)success.get("effects")).get("placeStocktakeId").toString();
    for(String sql:List.of("UPDATE mulino_inventory_PlaceStocktakes SET observedQuantity=99 WHERE ID=?","DELETE FROM mulino_inventory_PlaceStocktakes WHERE ID=?","DELETE FROM mulino_inventory_PlaceStocktakeMembers WHERE stocktakeId=?"))assertThrows(Exception.class,()->jdbc.update(sql,count));
    assertThrows(Exception.class,()->jdbc.execute("TRUNCATE mulino_inventory_PlaceStocktakes CASCADE"));
    assertThrows(Exception.class,()->new org.springframework.transaction.support.TransactionTemplate(transactions).execute(s->{
      jdbc.update("INSERT INTO mulino_inventory_PlaceStocktakeMembers SELECT organizationId,?,revision,createdAt,recordedAt,stocktakeId,?,segmentRevision,bookQuantity,unit FROM mulino_inventory_PlaceStocktakeMembers WHERE organizationId=? LIMIT 1",id(),id(),org);return null;}));
    assertEquals(1,count("mulino_inventory_PlaceStocktakes"));assertEquals(2,count("mulino_inventory_PlaceStocktakeMembers"));
  }
  @Test void laterFailureRollsBackHeaderMembersCommandAndAuditTogether(){
    var before=physical();assertThrows(IllegalStateException.class,()->runtime.requestContext().run(c->{return new org.springframework.transaction.support.TransactionTemplate(transactions).execute(s->{
      assertEquals("APPLIED",commands.execute(intent(id(),slots())).get("outcome"));throw new IllegalStateException("synthetic outer changeset failure");});}));
    assertEquals(0,count("mulino_inventory_PlaceStocktakes"));assertEquals(0,count("mulino_inventory_PlaceStocktakeMembers"));assertEquals(0,count("mulino_commands_CommandRecords"));assertEquals(0,count("mulino_commands_CommandAudits"));assertEquals(before,physical());
  }
  @Test void actualAuthenticatedHttpRecordsMeasurementAndReadsItWithoutPhysicalMutation()throws Exception {
    var before=physical();var expectedEffects=effects();String key=id();var body=intent(key,slots());
    var request=http("/api/ontology/records/recordStocktake",body);assertEquals(200,request.statusCode(),request.body());var result=json.readTree(request.body());assertEquals("APPLIED",result.path("outcome").asText());
    var repeat=http("/api/ontology/records/recordStocktake",body);assertEquals(result,json.readTree(repeat.body()));
    String stocktake=result.at("/effects/placeStocktakeId").asText();
    var read=http("/api/ontology/queries/getObject",row("id",stocktake,"scope",Map.of("organizationId",org,"objectType","PlaceStocktake"),"asOf",Instant.now().toString(),"knownAt",Instant.now().toString()));
    assertEquals(200,read.statusCode(),read.body());assertEquals("98",json.readTree(read.body()).at("/data/observedQuantity").asText());
    assertEquals(before,physical());assertEquals(expectedEffects,effects());assertEquals(1,count("mulino_inventory_PlaceStocktakes"));
    assertEquals(actor,jdbc.queryForObject("SELECT stableRequestOwner FROM mulino_commands_CommandRecords WHERE organizationId=? AND ID=?",String.class,org,result.path("commandId").asText()));
    String output=System.getProperty("stocktake.proof.dir");if(output!=null){Path target=Path.of(output);Files.createDirectories(target);json.writerWithDefaultPrettyPrinter().writeValue(target.resolve("actual-http.json").toFile(),row("recordStatus",request.statusCode(),"recordRaw",request.body(),"replayRaw",repeat.body(),"readStatus",read.statusCode(),"readRaw",read.body(),"physicalBefore",before,"physicalAfter",physical(),"businessEffectsBefore",expectedEffects,"businessEffectsAfter",effects(),"sourceBookRows",jdbc.queryForList("SELECT row_to_json(r)::text FROM mulino_inventory_PlaceStocktakeMembers r WHERE organizationId=?",org)));}
  }
  @Test void genuineEmptyBookCanRecordZeroAndFuturePhysicalSourceCannotBecomeAFalseZero() {
    String empty=id();insert("mulino_inventory_Places",row("organizationId",org,"ID",empty,"name","empty warehouse","kind","INTERNAL_STORAGE"));
    for(String kind:List.of("TARGET","PLACE"))insert("mulino_identity_GrantScopes",row("organizationId",org,"grantId",grant,"scopeKind",kind,"scopeId",empty));
    var s=slots();s.put("placeId",empty);s.put("observedQuantity","0");var noEffects=effects();var before=physical();
    var result=execute(intent(id(),s));assertEquals("APPLIED",result.get("outcome"),result.toString());
    assertEquals(1,count("mulino_inventory_PlaceStocktakes"));assertEquals(0,count("mulino_inventory_PlaceStocktakeMembers"));
    assertEquals("[]",jdbc.queryForObject("SELECT sourceMembersJson FROM mulino_inventory_PlaceStocktakes WHERE organizationId=?",String.class,org));
    assertEquals(0,jdbc.queryForObject("SELECT bookQuantity FROM mulino_inventory_PlaceStocktakes WHERE organizationId=?",java.math.BigDecimal.class,org).intValueExact());
    assertEquals(before,physical());assertEquals(noEffects,effects());
    Instant future=Instant.now().plusSeconds(3600);
    insert("mulino_inventory_QuantitySegments",row("organizationId",org,"ID",id(),"revision",0,"createdAt",Timestamp.from(future),"recordedAt",Timestamp.from(future),"itemId",item,"lotId",lot,"identificationStatus","CONFIRMED","quantity",new java.math.BigDecimal("1"),"unit","BOX","placeId",place,"controlScope","warehouse","validFrom",Timestamp.from(future),"mixtureStatus","IDENTIFIED"));
    assertNotEquals("APPLIED",execute(intent(id(),slots())).get("outcome"));assertEquals(1,count("mulino_inventory_PlaceStocktakes"));assertEquals(noEffects,effects());
  }
  @Test void excessiveScopeIsRejectedCompletelyInsteadOfCountingOnlyAnAuthorizedPrefix() {
    jdbc.update("INSERT INTO mulino_inventory_QuantitySegments(organizationId,ID,revision,createdAt,recordedAt,itemId,lotId,identificationStatus,quantity,unit,placeId,controlScope,validFrom,mixtureStatus) SELECT ?,md5(?||g::text)::uuid::text,0,?::timestamptz,?::timestamptz,?,?,'CONFIRMED',1,'BOX',?,'warehouse',?::timestamptz,'IDENTIFIED' FROM generate_series(1,254) g",org,id(),at.toString(),at.toString(),item,lot,place,at.toString());
    var before=physical();var noEffects=effects();assertNotEquals("APPLIED",execute(intent(id(),slots())).get("outcome"));
    assertEquals(0,count("mulino_inventory_PlaceStocktakes"));assertEquals(0,count("mulino_inventory_PlaceStocktakeMembers"));assertEquals(before,physical());assertEquals(noEffects,effects());
  }
  HttpResponse<String> http(String path,Map<String,Object> body)throws Exception {
    var encoder=Base64.getUrlEncoder().withoutPadding();String header=encoder.encodeToString("{\"alg\":\"RS256\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String payload=encoder.encodeToString(json.writeValueAsBytes(row("iss","https://stocktake.invalid","sub","counter","aud","stocktake","organizationId",org,"stableRequestOwner","caller-claim-is-not-authority","iat",Instant.now().getEpochSecond(),"exp",Instant.now().plusSeconds(120).getEpochSecond())));
    String signed=header+"."+payload;var signature=Signature.getInstance("SHA256withRSA");signature.initSign(KEY.getPrivate());signature.update(signed.getBytes(java.nio.charset.StandardCharsets.US_ASCII));String token=signed+"."+encoder.encodeToString(signature.sign());
    return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(20)).header("Authorization","Bearer "+token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
  }
}
