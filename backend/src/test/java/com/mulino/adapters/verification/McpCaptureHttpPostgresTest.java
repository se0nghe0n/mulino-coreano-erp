package com.mulino.adapters.verification;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real authenticated HTTP/CAP/PostgreSQL probes; synthetic local identity, never model/client acceptance. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local","verification"})
class McpCaptureHttpPostgresTest {
  static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280");
  static final Instant T=Instant.now();
  static final Path DIRECTORY;
  static final KeyPair KEYS;
  static final String ISSUER="https://capture-fixture.invalid",AUDIENCE="mulino-capture-probe";
  static {
    Path created=null;
    try {
      created=Files.createTempDirectory("mulino-auth-capture-");DIRECTORY=created;
      var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KEYS=generator.generateKeyPair();
      Files.writeString(DIRECTORY.resolve("public.pem"),"-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(KEYS.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----\n");
      PG.start();
    } catch(Exception failure) {
      PG.stop();
      if(created!=null)try(var paths=Files.walk(created)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
      catch(Exception cleanup){failure.addSuppressed(cleanup);}
      throw new ExceptionInInitializerError(failure);
    }
  }
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
    r.add("JWT_PUBLIC_KEY",()->DIRECTORY.resolve("public.pem").toString());r.add("JWT_ISSUER",()->ISSUER);r.add("JWT_AUDIENCE",()->AUDIENCE);
    r.add("mulino.verification.mcp-capture-enabled",()->"true");r.add("mulino.verification.instant",T::toString);
    r.add("mulino.evidence.blob-root",()->DIRECTORY.resolve("blobs").toString());
  }
  @Autowired JdbcTemplate db;
  @LocalServerPort int port;
  final ObjectMapper json=new ObjectMapper();
  final HttpClient client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
  String org,actor,owner,grant,alias,otherActor,foreignOrg;
  static String id(){return UUID.randomUUID().toString();}
  @BeforeEach void seed() {
    org=id();actor=id();owner=id();grant=id();alias="capture-"+id();otherActor=id();foreignOrg=id();
    db.update("INSERT INTO mulino_identity_Organizations(ID,externalAlias,revision) VALUES(?,?,1),(?,?,1)",org,alias,foreignOrg,"foreign-"+alias);
    for(String a:List.of(actor,otherActor)) {
      db.update("INSERT INTO mulino_identity_Actors(organizationId,ID,kind,stableRequestOwner,revision) VALUES(?,?,'AGENT',?,1)",org,a,a.equals(actor)?owner:id());
      db.update("INSERT INTO mulino_identity_ExternalIdentities(organizationId,ID,actorId,issuer,subject,organizationAlias,revision) VALUES(?,?,?,?,?,?,1)",org,id(),a,ISSUER,a,alias);
      db.update("INSERT INTO mulino_identity_Memberships(organizationId,ID,actorId,validFrom,validUntil,revision) VALUES(?,?,?,?,?,1)",org,id(),a,Timestamp.from(T.minusSeconds(100)),Timestamp.from(T.plusSeconds(3600)));
      db.update("INSERT INTO mulino_identity_AuthorityFences VALUES(?,?,1)",org,a);
    }
    db.update("INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil,revision) VALUES(?,?,?,?,?,?,1)",org,grant,actor,actor,Timestamp.from(T.minusSeconds(100)),Timestamp.from(T.plusSeconds(3600)));
    db.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'ORGANIZATION',?)",org,grant,org);
    for(String capability:List.of("getAccessContext","getObject","getInventory")) {
      db.update("INSERT INTO mulino_identity_GrantActions VALUES(?,?,?)",org,grant,capability);
      db.update("INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil,revision) VALUES(?,?,?,?,'ORGANIZATION',?,?,?,1)",org,id(),actor,capability,org,Timestamp.from(T.minusSeconds(100)),Timestamp.from(T.plusSeconds(3600)));
    }
  }
  String credential(String issuer,String audience,String subject,String organization)throws Exception {
    long now=Instant.now().getEpochSecond();var encoder=Base64.getUrlEncoder().withoutPadding();
    String first=encoder.encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    String second=encoder.encodeToString(json.writeValueAsBytes(Map.of("iss",issuer,"aud",List.of(audience),"sub",subject,"organizationId",organization,"stableRequestOwner","forged-owner","roles",List.of("ADMIN"),"iat",now,"exp",now+120)));
    String body=first+"."+second;var signer=Signature.getInstance("SHA256withRSA");signer.initSign(KEYS.getPrivate());signer.update(body.getBytes(StandardCharsets.US_ASCII));
    return body+"."+encoder.encodeToString(signer.sign());
  }
  String own()throws Exception{return credential(ISSUER,AUDIENCE,actor,alias);}
  ObjectNode rpc(String method,String tool,Map<String,Object> arguments) {
    var value=json.createObjectNode().put("jsonrpc","2.0").put("id",id()).put("method",method);var params=value.putObject("params");
    params.putObject("_meta").put("io.modelcontextprotocol/protocolVersion","2026-07-28").putObject("io.modelcontextprotocol/clientCapabilities");
    if(tool!=null){params.put("name",tool);params.set("arguments",json.valueToTree(arguments));}return value;
  }
  HttpResponse<String> post(ObjectNode rpc,String token,String nonce,String headerMethod)throws Exception {
    var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/mcp/ontology"))
      .timeout(java.time.Duration.ofSeconds(15)).header("Content-Type","application/json").header("Accept","application/json, text/event-stream")
      .header("Mcp-Method",headerMethod).header("MCP-Protocol-Version","2026-07-28");
    if(token!=null)request.header("Authorization","Bearer "+token);
    if(nonce!=null)request.header(McpCaptureFilter.REQUEST_HEADER,nonce);
    if(rpc.path("params").has("name"))request.header("Mcp-Name",rpc.path("params").path("name").asText());
    return client.send(request.POST(HttpRequest.BodyPublishers.ofString(rpc.toString())).build(),HttpResponse.BodyHandlers.ofString());
  }
  HttpResponse<String> read(String capture,String token)throws Exception {
    var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/internal/verification/mcp-captures/"+capture)).timeout(java.time.Duration.ofSeconds(15));
    if(token!=null)request.header("Authorization","Bearer "+token);
    return client.send(request.GET().build(),HttpResponse.BodyHandlers.ofString());
  }
  JsonNode receipt(ObjectNode rpc,HttpResponse<String> response,String nonce,String token)throws Exception {
    String capture=response.headers().firstValue(McpCaptureFilter.RECEIPT_HEADER).orElseThrow(()->new AssertionError("No server receipt: "+response.statusCode()+" "+response.body()));
    var received=read(capture,token);assertEquals(200,received.statusCode(),received.body());var value=json.readTree(received.body());
    assertEquals(McpCaptureFilter.sha(rpc.toString().getBytes(StandardCharsets.UTF_8)),value.path("requestSha256").asText());
    assertEquals(McpCaptureFilter.sha(response.body().getBytes(StandardCharsets.UTF_8)),value.path("responseSha256").asText());
    assertEquals(rpc.path("id"),value.path("rpcId"));assertEquals(response.statusCode(),value.path("httpStatus").asInt());
    assertEquals(nonce,value.path("requestNonce").asText());assertEquals("NOT_ASSERTED",value.path("actionAuthorization").asText());
    assertFalse(received.body().contains(token));assertFalse(received.body().contains("forged-owner"));assertFalse(received.body().contains("ADMIN"));
    assertEquals(404,read(capture,token).statusCode());
    Path output=Path.of("target/auth-capture-probe");Files.createDirectories(output);Files.writeString(output.resolve(capture+".json"),json.writeValueAsString(Map.of("request",rpc,"response",json.readTree(response.body()),"serverCapture",value)));
    return value;
  }
  @Test void realRequestIdentityMapsServerOwnerAndCurrentGrantFactsWithoutChangingWire()throws Exception {
    String token=own(),nonce=id();var rpc=rpc("tools/call","getAccessContext",Map.of());var response=post(rpc,token,nonce,"tools/call");
    assertEquals(200,response.statusCode());var wire=json.readTree(response.body());assertEquals(Set.of("jsonrpc","id","result"),json.convertValue(wire,Map.class).keySet());
    var value=receipt(rpc,response,nonce,token);var identity=value.path("identity");
    assertEquals(ISSUER,identity.path("issuer").asText());assertEquals(AUDIENCE,identity.path("audiences").get(0).asText());
    assertEquals(actor,identity.path("subject").asText());assertEquals(actor,identity.path("actorId").asText());
    assertEquals(org,identity.path("organizationId").asText());assertEquals(owner,identity.path("stableRequestOwner").asText());
    assertEquals(grant,value.path("grants").get(0).path("ID").asText());assertEquals(1,value.path("grants").get(0).path("revision").asInt());
    assertEquals(3,value.path("grants").get(0).path("actions").size());assertEquals(org,value.path("grants").get(0).path("scopes").get(0).path("scopeId").asText());
    assertEquals(owner,wire.path("result").path("structuredContent").path("data").path("stableRequestOwner").asText());
  }
  @Test void wrongIssuerAudienceAndMissingAuthenticationProduceNoCapture()throws Exception {
    for(String token:List.of(credential("https://wrong.invalid",AUDIENCE,actor,alias),credential(ISSUER,"wrong-audience",actor,alias))) {
      var response=post(rpc("tools/list",null,Map.of()),token,id(),"tools/list");assertEquals(401,response.statusCode());assertTrue(response.headers().firstValue(McpCaptureFilter.RECEIPT_HEADER).isEmpty());
    }
    assertEquals(401,post(rpc("tools/list",null,Map.of()),null,id(),"tools/list").statusCode());
  }
  @Test void unknownOrganizationOrSubjectCannotCreateServerResolvedReceipt()throws Exception {
    for(String token:List.of(credential(ISSUER,AUDIENCE,actor,"foreign-"+alias),credential(ISSUER,AUDIENCE,id(),alias))) {
      var response=post(rpc("tools/call","getAccessContext",Map.of()),token,id(),"tools/call");
      assertTrue(response.body().contains("FORBIDDEN"),response.body());assertTrue(response.headers().firstValue(McpCaptureFilter.RECEIPT_HEADER).isEmpty());
    }
  }
  @Test void hiddenForeignAndAnonymousReceiptReadsCannotConsumeOrRevealOwnedReceipt()throws Exception {
    String token=own(),nonce=id();var rpc=rpc("tools/list",null,Map.of());var response=post(rpc,token,nonce,"tools/list");String capture=response.headers().firstValue(McpCaptureFilter.RECEIPT_HEADER).orElseThrow();
    var other=read(capture,credential(ISSUER,AUDIENCE,otherActor,alias));assertEquals(404,other.statusCode());assertFalse(other.body().contains(actor));
    var foreign=read(capture,credential(ISSUER,AUDIENCE,actor,"foreign-"+alias));assertEquals(other.body(),foreign.body());
    assertEquals(other.body(),read(id(),token).body());assertEquals(401,read(capture,null).statusCode());
    receipt(rpc,response,nonce,token);
  }
  @Test void revokedGrantIsObservedWhileActionRemainsForbiddenAndReceiptReadsHaveNoDbWrites()throws Exception {
    db.update("UPDATE mulino_identity_Grants SET revokedAt=?,revision=2 WHERE ID=?",Timestamp.from(T),grant);
    String token=own(),nonce=id();var rpc=rpc("tools/call","getAccessContext",Map.of());var response=post(rpc,token,nonce,"tools/call");
    assertTrue(response.body().contains("FORBIDDEN"),response.body());var before=allTableRows();var value=receipt(rpc,response,nonce,token);
    assertEquals(before,allTableRows());assertEquals(T.toString(),Instant.parse(value.path("grants").get(0).path("revokedAt").asText()).toString());
    assertEquals(2,value.path("grants").get(0).path("revision").asInt());
  }
  @Test void allOfScopeMismatchKeepsIdentityCaptureButDoesNotAuthorizeRead()throws Exception {
    String place=id();db.update("INSERT INTO mulino_inventory_Places(organizationId,ID,name,kind,revision,recordedAt,createdAt) VALUES(?,?,'창고','INTERNAL_STORAGE',1,?,?)",org,place,Timestamp.from(T.minusSeconds(10)),Timestamp.from(T.minusSeconds(10)));
    db.update("DELETE FROM mulino_identity_GrantScopes WHERE grantId=?",grant);
    db.update("INSERT INTO mulino_identity_GrantScopes VALUES(?,?,'TARGET',?),(?,?,'PLACE',?)",org,grant,place,org,grant,id());
    String token=own(),nonce=id();var rpc=rpc("tools/call","getObject",Map.of("id",place,"scope",Map.of("objectType","Place")));
    var response=post(rpc,token,nonce,"tools/call");assertTrue(response.body().contains("FORBIDDEN"),response.body());var value=receipt(rpc,response,nonce,token);
    assertEquals(2,value.path("grants").get(0).path("scopes").size());
  }
  @Test void wireHeaderMismatchRemainsProtocolErrorWithSameRequestHashAndIdentity()throws Exception {
    String token=own(),nonce=id();var rpc=rpc("tools/list",null,Map.of());var response=post(rpc,token,nonce,"tools/call");
    assertEquals(400,response.statusCode());assertEquals(-32020,json.readTree(response.body()).path("error").path("code").asInt());receipt(rpc,response,nonce,token);
  }
  @Test void payloadActorAndForeignScopeCannotChangeServerIdentityOrAuthority()throws Exception {
    String token=own();
    for(var arguments:List.of(Map.<String,Object>of("actor",Map.of("actorId",otherActor,"role","ADMIN")),Map.<String,Object>of("scope",Map.of("organizationId",foreignOrg)))) {
      String nonce=id();var rpc=rpc("tools/call","getAccessContext",arguments);var response=post(rpc,token,nonce,"tools/call");
      assertTrue(json.readTree(response.body()).path("result").path("isError").asBoolean(),response.body());
      var value=receipt(rpc,response,nonce,token);assertEquals(actor,value.path("identity").path("actorId").asText());
      assertEquals(org,value.path("identity").path("organizationId").asText());assertEquals(owner,value.path("identity").path("stableRequestOwner").asText());
    }
  }
  @Test void childGrantCannotReplaceMissingCurrentDelegatorAuthority()throws Exception {
    db.update("UPDATE mulino_identity_Grants SET delegatorId=?,revision=2 WHERE ID=?",otherActor,grant);
    String token=own(),nonce=id();var rpc=rpc("tools/call","getAccessContext",Map.of());var response=post(rpc,token,nonce,"tools/call");
    assertTrue(response.body().contains("FORBIDDEN"),response.body());var value=receipt(rpc,response,nonce,token);
    assertEquals(otherActor,value.path("grants").get(0).path("delegatorId").asText());assertEquals("NOT_ASSERTED",value.path("actionAuthorization").asText());
  }
  @Test void noOptInHeaderLeavesPublicMcpResponseUnobserved()throws Exception {
    var response=post(rpc("tools/list",null,Map.of()),own(),null,"tools/list");assertEquals(200,response.statusCode());assertTrue(response.headers().firstValue(McpCaptureFilter.RECEIPT_HEADER).isEmpty());
  }
  Map<String,List<String>> allTableRows() {
    var rows=new TreeMap<String,List<String>>();
    for(String table:db.queryForList("SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename LIKE 'mulino_%'",String.class)) {
      assertTrue(table.matches("[a-z0-9_]+"));
      rows.put(table,db.queryForList("SELECT to_jsonb(row)::text FROM "+table+" row",String.class).stream().sorted().toList());
    }
    return rows;
  }
  @AfterAll static void cleanup()throws Exception {
    PG.stop();try(var paths=Files.walk(DIRECTORY)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
    assertFalse(PG.isRunning());assertFalse(Files.exists(DIRECTORY));
    Path output=Path.of("target/auth-capture-probe");Files.createDirectories(output);Files.writeString(output.resolve("cleanup.json"),"{\"postgresStopped\":true,\"ephemeralFilesRemoved\":true}");
  }
}
