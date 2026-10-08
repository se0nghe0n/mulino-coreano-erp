package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
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
class S1ReadIntegrationTest {
  static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static final java.security.KeyPair KEY;
  static final Path KEY_PATH;
  static final Path BLOB_PATH;
  static { try {PG.start();var generator=java.security.KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KEY=generator.generateKeyPair();BLOB_PATH=Files.createTempDirectory("mulino-s1-read-blob-");KEY_PATH=Files.createTempFile("mulino-s1-read-public-",".pem");Files.writeString(KEY_PATH,"-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(KEY.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----\n");}catch(Exception failure){throw new ExceptionInInitializerError(failure);} }
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
    var loc=scoped(place);loc.putAll(row("name","warehouse","kind","INTERNAL_STORAGE"));insert("mulino_inventory_Places",loc);
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
  Map<String,Object> request(String operation,String target,String snapshot){Map<String,Object> body=row("scope",Map.of("organizationId",org,"itemId",item,"lotId",lot),"asOf",AS_OF.toString(),"knownAt",KNOWN.toString());if(target!=null)body.put("id",target);if(snapshot!=null)body.put("snapshotRef",snapshot);return body;}
  Map<String,Object> read(String operation,String target,String snapshot){return runtime.requestContext().run(c->{return queries.query(QueryRequests.parse(operation,request(operation,target,snapshot)));});}
  @Test void twoEntrypointsSharePhysicalReferencesEvidenceAndResponsibility(){
    var noun=read("getObject",item,null);var verb=read("getWork",work,(String)noun.get("snapshotRevision"));
    for(String key:List.of("snapshotRevision","asOf","knownAt","scope","evidenceRefs"))assertEquals(noun.get(key),verb.get(key));
    var n=(Map<?,?>)noun.get("data");var v=(Map<?,?>)verb.get("data");
    for(String key:List.of("itemId","workIds","heldQuantity","eligibleQuantity","eligibilityStatus","cumulativeArrival","unit","ownerIds","nextActions","evidenceRefs"))assertEquals(n.get(key),v.get(key));
    assertEquals("100",n.get("heldQuantity"));
    // No current eligibility policy: the confirmed SELL subset cannot be determined,
    // so the quantity stays null (unknown), never zero (plan §3.1, §6; AGENTS.md).
    assertTrue(n.containsKey("eligibleQuantity"));assertNull(n.get("eligibleQuantity"));assertNull(n.get("unreservedEligibleQuantity"));assertEquals("UNKNOWN",n.get("eligibilityStatus"));
    assertTrue(((List<?>)noun.get("unknowns")).contains("CURRENT_ELIGIBILITY_POLICY_UNRESOLVED"));
    assertTrue(((List<?>)noun.get("unknowns")).contains("QC_AUTHORITY_UNCONFIRMED"));
    // No authoritative confirmed Receipts exist in this snapshot: this sum is
    // confirmed zero, unlike a provisional/withdrawn receipt source unknown.
    assertEquals("0",n.get("cumulativeArrival"));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_trade_receipt_Receipts WHERE organizationId=?",Integer.class,org));
    assertEquals(List.of(actor),n.get("ownerIds"));assertEquals(List.of(document),n.get("evidenceRefs"));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_work_read_ObligationReferences WHERE organizationId=? AND status='OPEN'",Integer.class,org));
    assertEquals(new java.math.BigDecimal("100.000000000000"),jdbc.queryForObject("SELECT sum(quantity) FROM mulino_inventory_QuantitySegments WHERE organizationId=?",java.math.BigDecimal.class,org));
  }
  @Test void wrongOrgAndReadProjectionChangesFailClosed(){
    var noun=read("getObject",item,null);
    assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->read("getWork",id(),null)).code());
    var q=request("getWork",work,null);q.put("scope",Map.of("organizationId",id()));assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->runtime.requestContext().run(c->{return queries.query(QueryRequests.parse("getWork",q));})).code());
    var w=workRow(id());w.putAll(row("itemId",item,"lotId",lot,"definitionVersionId",definition,"kind","REVIEW","status","READY","ownerId",actor,"supervisorId",actor));insert("mulino_work_read_Works",w);
    assertEquals("SNAPSHOT_CHANGED",assertThrows(DomainError.class,()->read("getWork",work,(String)noun.get("snapshotRevision"))).code());
  }
  @Test void organizationAndHumanReferenceFencesAreDatabaseConstraints(){
    var w=workRow(id());w.putAll(row("itemId",item,"lotId",lot,"definitionVersionId",definition,"kind","REVIEW","status","READY","ownerId",id(),"supervisorId",actor));assertThrows(org.springframework.dao.DataAccessException.class,()->insert("mulino_work_read_Works",w));
    String agent=id();insert("mulino_identity_Actors",row("organizationId",org,"ID",agent,"kind","AGENT","stableRequestOwner",id()));w.put("ownerId",agent);assertThrows(org.springframework.dao.DataAccessException.class,()->insert("mulino_work_read_Works",w));
    assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE mulino_work_read_Works SET status='FULFILLED' WHERE organizationId=? AND ID=?",org,work));
    String otherOrg=id(),otherActor=id();insert("mulino_identity_Organizations",row("ID",otherOrg,"externalAlias",otherOrg));insert("mulino_identity_Actors",row("organizationId",otherOrg,"ID",otherActor,"kind","HUMAN","stableRequestOwner",id()));
    String profile=id();insert("mulino_evidence_SourceProfiles",row("ID",profile,"organizationId",otherOrg,"revision",1,"recordedAt",Timestamp.from(KNOWN),"recordedBy",otherActor,"namespace","other-fixture","policyVersion","read-v1","intakeOwnerId",otherActor,"supervisorId",otherActor,"nextAction","review","nextCheckAt",Timestamp.from(KNOWN.plusSeconds(3600))));
    var doc=row("ID",id(),"organizationId",otherOrg,"revision",1,"recordedAt",Timestamp.from(KNOWN),"recordedBy",otherActor,"subjectKind","WORK","subjectId",work,"workId",work,"sha256","3".repeat(64),"availability","UNKNOWN","byteLength",0L,"mediaType","text/plain","sourceProfileId",profile,"provenance","{}");
    assertThrows(org.springframework.dao.DataAccessException.class,()->insert("mulino_evidence_DocumentVersions",doc));doc.put("workId",null);assertThrows(org.springframework.dao.DataAccessException.class,()->insert("mulino_evidence_DocumentVersions",doc));
  }
  @Test void absoluteCqnTimesRemainStableAcrossSessionTimezonesAndKnowledgeBoundary(){
    var inserted=workRow(id());inserted.putAll(row("itemId",item,"lotId",lot,"definitionVersionId",definition,"kind","REVIEW","status","READY","ownerId",actor,"supervisorId",actor));for(String key:List.of("createdAt","recordedAt","effectiveAt"))inserted.put(key,((Timestamp)inserted.get(key)).toInstant());
    runtime.requestContext().run(c->{persistence.run(com.sap.cds.ql.Insert.into("mulino.work.read.Works").entry(inserted));return null;});
    for(String zone:List.of("UTC","Asia/Seoul","America/New_York")){
      var transaction=new org.springframework.transaction.support.TransactionTemplate(transactions);transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
      transaction.execute(status->runtime.requestContext().run(c->{jdbc.execute("SET LOCAL TIME ZONE '"+zone+"'");assertEquals(zone,jdbc.queryForObject("SHOW TIME ZONE",String.class));var row=persistence.run(Select.from("mulino.work.read.Works").where(r->r.get("organizationId").eq(org).and(r.get("ID").eq(inserted.get("ID"))))).single();assertEquals(AS_OF,asInstant(row.get("effectiveAt")));assertEquals("100",((Map<?,?>)queries.query(QueryRequests.parse("getWork",request("getWork",work,null))).get("data")).get("heldQuantity"));return null;}));
    }
    var q=request("getWork",work,null);q.put("asOf","2026-10-07T18:00:00+09:00");q.put("knownAt","2026-10-07T18:00:01+09:00");var offset=runtime.requestContext().run(c->{return queries.query(QueryRequests.parse("getWork",q));});assertEquals(AS_OF.toString(),offset.get("asOf"));q.put("asOf","2026-10-07T05:00:00-04:00");assertEquals(AS_OF.toString(),runtime.requestContext().run(c->{return queries.query(QueryRequests.parse("getWork",q));}).get("asOf"));
    q.put("knownAt",KNOWN.minusSeconds(1).toString());var before=runtime.requestContext().run(c->{return queries.query(QueryRequests.parse("getWork",q));});assertEquals(List.of(document),offset.get("evidenceRefs"));
    assertEquals(List.of(),before.get("evidenceRefs"));
    q.put("asOf",AS_OF.minusNanos(1).toString());assertEquals("FORBIDDEN",assertThrows(DomainError.class,()->runtime.requestContext().run(c->{return queries.query(QueryRequests.parse("getWork",q));})).code());
  }
  @Test @SuppressWarnings("unchecked") void freshFlywayMatchesAllCdsColumnsAndPrimaryKeysWithExplicitWidening() throws Exception {
    Path output=Path.of("target/s4-compiler-expected.sql");String node=System.getenv().getOrDefault("NODE24_BIN","node");
    var compiler=new ProcessBuilder(node,"node_modules/@sap/cds-dk/bin/cds.js","compile","db","--to","sql","--dialect","postgres").redirectOutput(output.toFile()).redirectError(ProcessBuilder.Redirect.INHERIT).start();assertTrue(compiler.waitFor(30,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,compiler.exitValue());
    try(var connection=java.sql.DriverManager.getConnection(PG.getJdbcUrl(),PG.getUsername(),PG.getPassword())){
      connection.createStatement().execute("CREATE SCHEMA s4_compiler_expected");connection.createStatement().execute("SET search_path TO s4_compiler_expected");connection.createStatement().execute(Files.readString(output));
      var expected=columns(connection,"s4_compiler_expected");var actual=columns(connection,"public");assertEquals(expected.keySet(),actual.keySet(),"Exact CDS/Flyway entity and column identity");
      var policy=json.readValue(Files.readString(Path.of("../docs/execution/s4-integration/schema-compatibility.json")),Map.class);
      var timestamps=new TreeSet<String>();var strengthening=new TreeSet<String>();var differences=new ArrayList<Map<String,Object>>();
      for(String key:expected.keySet()){
        var e=expected.get(key);var a=actual.get(key);String et=String.valueOf(e.get(0)),at=String.valueOf(a.get(0));
        if(et.equals("timestamp without time zone")&&at.equals("timestamp with time zone")){timestamps.add(key);a.set(0,et);}
        if(e.get(4).equals("YES")&&a.get(4).equals("NO")){strengthening.add(key);a.set(4,"YES");}
        if(!e.equals(a))differences.add(Map.of("column",key,"expected",e,"actual",a));
      }
      Files.writeString(Path.of("target/s4-compatibility-observed.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("timestampWidening",timestamps,"notNullStrengthening",strengthening,"columnCount",actual.size(),"columns",actual.keySet(),"structuralDifferences",differences,"primaryKeyDifferences",!primaryKeys(connection,"s4_compiler_expected").equals(primaryKeys(connection,"public")))));
      assertEquals(List.of(),differences,"Only enumerated Timestamp widening and mandatory NOT NULL strengthening are eligible for the explicit inventory; other shape changes must be corrected in source");
      assertEquals(((Number)policy.get("observedColumnCount")).intValue(),actual.size(),"Explicit combined inventory column count");
      assertEquals(new TreeSet<>((List<String>)policy.get("exactColumnInventory")),new TreeSet<>(actual.keySet()),"Every installed column has explicit S4 inventory identity");
      assertEquals(435,((List<?>)policy.get("s4AddedColumnInventory")).size(),"Exact S3 1432 plus S4 435 delta (V31 recall Actions.investigationId, V30 legitimateRangesJson, V29 scopeDifference, V24 legitimateQuantity)");
      assertEquals(new TreeSet<>((List<String>)policy.get("timestampWidening")),timestamps,"Explicit absolute-instant column list");assertEquals(new TreeSet<>((List<String>)policy.get("notNullStrengthening")),strengthening,"Explicit required-domain column list");
      assertEquals(primaryKeys(connection,"s4_compiler_expected"),primaryKeys(connection,"public"),"Exact CDS/Flyway primary key identity and ordering");
    }
  }
  Map<String,List<Object>> columns(Connection connection,String schema)throws Exception{
    Map<String,List<Object>> result=new TreeMap<>();try(var statement=connection.prepareStatement("SELECT table_name,column_name,data_type,coalesce(character_maximum_length,0),coalesce(numeric_precision,0),coalesce(numeric_scale,0),is_nullable FROM information_schema.columns WHERE table_schema=? AND table_name LIKE 'mulino_%' ORDER BY table_name,column_name")){statement.setString(1,schema);try(var rows=statement.executeQuery()){while(rows.next())result.put(rows.getString(1)+"."+rows.getString(2),new ArrayList<>(List.of(rows.getString(3),rows.getInt(4),rows.getInt(5),rows.getInt(6),rows.getString(7))));}}return result;
  }
  Map<String,String> primaryKeys(Connection connection,String schema)throws Exception{
    Map<String,String> result=new TreeMap<>();try(var statement=connection.prepareStatement("SELECT tc.table_name,string_agg(k.column_name,',' ORDER BY k.ordinal_position) FROM information_schema.table_constraints tc JOIN information_schema.key_column_usage k ON tc.constraint_name=k.constraint_name AND tc.table_schema=k.table_schema WHERE tc.table_schema=? AND tc.constraint_type='PRIMARY KEY' AND tc.table_name LIKE 'mulino_%' GROUP BY tc.table_name")){statement.setString(1,schema);try(var rows=statement.executeQuery()){while(rows.next())result.put(rows.getString(1),rows.getString(2));}}return result;
  }
  @Test void ordinaryStartupCannotReachSpikeWritesOrDefaultCapProjection()throws Exception{
    String forgedRoleToken=token(true);var client=HttpClient.newHttpClient();
    for(String path:List.of("/api/platform/actions/reserve","/mcp","/odata/v4/platform/reserve")){
      var response=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Authorization","Bearer "+forgedRoleToken).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}" )).build(),HttpResponse.BodyHandlers.ofString());
      assertTrue(Set.of(403,404).contains(response.statusCode()),path+" must be unavailable outside platform-spike: "+response.statusCode()+" "+response.body());
    }
    var projected=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/odata/v4/platform/Scopes")).header("Authorization","Bearer "+forgedRoleToken).GET().build(),HttpResponse.BodyHandlers.ofString());
    assertTrue(Set.of(403,404).contains(projected.statusCode()),"Default CAP projection must remain inaccessible: "+projected.statusCode());
    assertEquals(200,http("/api/ontology/queries/getObject",request("getObject",item,null),true,false).statusCode());
  }
  @Test @SuppressWarnings("unchecked") void signedEvidenceSelectorsStayTypedAndOrganizationScoped()throws Exception{
    var request=row("id",document,"scope",Map.of("organizationId",org,"kind","DOCUMENT","subjectKind","ITEM","subjectId",item),"asOf",AS_OF.toString(),"knownAt",KNOWN.toString());
    var allowed=http("/api/ontology/queries/getEvidence",request,true,false);assertEquals(200,allowed.statusCode(),allowed.body());var result=json.readValue(allowed.body(),Map.class);assertEquals(document,((Map<?,?>)result.get("data")).get("ID"));assertEquals("UNKNOWN",((Map<?,?>)result.get("data")).get("availability"));
    request.put("scope",Map.of("organizationId",id(),"kind","DOCUMENT","subjectKind","ITEM","subjectId",item));assertEquals(403,http("/api/ontology/queries/getEvidence",request,true,false).statusCode());
    request.put("scope",Map.of("organizationId",org,"kind","SQL","subjectKind","ITEM","subjectId",item));assertEquals(400,http("/api/ontology/queries/getEvidence",request,true,false).statusCode());
  }
  String token() throws Exception {return token(false);}
  String token(boolean spikeRole) throws Exception {
    long now=Instant.now().getEpochSecond();var encode=Base64.getUrlEncoder().withoutPadding();
    String header=encode.encodeToString(json.writeValueAsBytes(Map.of("alg","RS256","typ","JWT")));
    var tokenClaims=row("iss","https://mulino.local.invalid","aud",List.of("mulino-platform"),"sub","writer-a","organizationId",org,"stableRequestOwner","forged-owner-ignored","iat",now,"nbf",now-5,"exp",now+3600);if(spikeRole)tokenClaims.put("roles",List.of("platform-spike"));
    String claims=encode.encodeToString(json.writeValueAsBytes(tokenClaims));
    String body=header+"."+claims;var signer=java.security.Signature.getInstance("SHA256withRSA");signer.initSign(KEY.getPrivate());signer.update(body.getBytes(java.nio.charset.StandardCharsets.US_ASCII));return body+"."+encode.encodeToString(signer.sign());
  }
  HttpResponse<String> http(String path,Map<String,Object> body,boolean signed,boolean mcp)throws Exception{
    var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json");if(signed)builder.header("Authorization","Bearer "+token());
    if(mcp){builder.header("Accept","application/json, text/event-stream").header("MCP-Protocol-Version","2026-07-28").header("Mcp-Method",String.valueOf(body.get("method")));if(((Map<?,?>)body.get("params")).get("name")!=null)builder.header("Mcp-Name",String.valueOf(((Map<?,?>)body.get("params")).get("name")));}
    return HttpClient.newHttpClient().send(builder.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
  }
  @Test @SuppressWarnings("unchecked") void signedRestOdataAndMcpShareTheActualDomainRead()throws Exception{
    var request=request("getObject",item,null);var noun=http("/api/ontology/queries/getObject",request,true,false);assertEquals(200,noun.statusCode(),noun.body());Map<String,Object> result=json.readValue(noun.body(),Map.class);
    var workRequest=request("getWork",work,(String)result.get("snapshotRevision"));var verb=http("/api/ontology/queries/getWork",workRequest,true,false);assertEquals(200,verb.statusCode(),verb.body());Map<String,Object> workResponse=json.readValue(verb.body(),Map.class);assertEquals(result.get("snapshotRevision"),workResponse.get("snapshotRevision"));
    var odata=http("/odata/v4/ontology/query",Map.of("operation","getWork","requestJson",json.writeValueAsString(workRequest)),true,false);assertEquals(200,odata.statusCode(),odata.body());var odataBody=json.readValue(odata.body(),Map.class);var domain=json.readValue(String.valueOf(odataBody.get("value")),Map.class);assertEquals(workResponse,domain);
    var rpc=Map.<String,Object>of("jsonrpc","2.0","id",7,"method","tools/call","params",Map.of("name","getWork","arguments",workRequest,"_meta",Map.of("io.modelcontextprotocol/protocolVersion","2026-07-28","io.modelcontextprotocol/clientCapabilities",Map.of())));
    var mcp=http("/mcp/ontology",rpc,true,true);assertEquals(200,mcp.statusCode(),mcp.body());var rpcBody=json.readValue(mcp.body(),Map.class);var tool=(Map<?,?>)rpcBody.get("result");assertEquals(false,tool.get("isError"));assertEquals(workResponse,tool.get("structuredContent"));
    var listed=http("/mcp/ontology",Map.of("jsonrpc","2.0","id",8,"method","tools/list","params",Map.of("_meta",Map.of("io.modelcontextprotocol/protocolVersion","2026-07-28","io.modelcontextprotocol/clientCapabilities",Map.of()))),true,true);assertEquals(200,listed.statusCode(),listed.body());var listResult=(Map<?,?>)json.readValue(listed.body(),Map.class).get("result");assertTrue(((List<Map<String,Object>>)listResult.get("tools")).stream().anyMatch(t->"getWork".equals(t.get("name"))));
        assertEquals(401,http("/api/ontology/queries/getObject",request,false,false).statusCode());
    var forbiddenRpc=Map.<String,Object>of("jsonrpc","2.0","id",9,"method","tools/call","params",Map.of("name","getWork","arguments",Map.of("id",work,"scope",Map.of("organizationId",id())),"_meta",Map.of("io.modelcontextprotocol/protocolVersion","2026-07-28","io.modelcontextprotocol/clientCapabilities",Map.of())));
    var deniedMcp=http("/mcp/ontology",forbiddenRpc,true,true);assertEquals(200,deniedMcp.statusCode());var deniedTool=(Map<?,?>)json.readValue(deniedMcp.body(),Map.class).get("result");assertEquals(true,deniedTool.get("isError"));assertEquals("FORBIDDEN",((Map<?,?>)((Map<?,?>)deniedTool.get("structuredContent")).get("error")).get("code"));
        request.put("scope",Map.of("organizationId",id()));var denied=http("/api/ontology/queries/getObject",request,true,false);assertEquals(403,denied.statusCode(),denied.body());assertEquals("FORBIDDEN",((Map<?,?>)json.readValue(denied.body(),Map.class).get("error")).get("code"));
  }
  Instant asInstant(Object value){return value instanceof Instant i?i:value instanceof Timestamp t?t.toInstant():Instant.parse(value.toString());}
}
