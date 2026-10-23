package org.mulino.verification.nativeclient;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mulino.verification.Json;
import org.mulino.verification.actual.ActualConfiguration;

/** SELFTEST mutations/transport plumbing; the fake HTTP peer is never product/server acceptance. */
class NativeMcpTransportCaptureTest {
  static final String ISSUER="https://capture.invalid",AUDIENCE="capture-test",SUBJECT="server-subject",ALIAS="server-alias";
  static final String ORG="00000000-0000-0000-0000-000000000001",ACTOR="00000000-0000-0000-0000-000000000002",OWNER="00000000-0000-0000-0000-000000000003",GRANT="00000000-0000-0000-0000-000000000004";
  static final String ID=UUID.randomUUID().toString(),NONCE=UUID.randomUUID().toString();
  static final String SENT="2026-10-24T00:00:00Z",DONE="2026-10-24T00:00:03Z";
  @TempDir Path temporary;
  JsonNode actor(){return Json.object().put("issuer",ISSUER).put("audience",AUDIENCE).put("subject",SUBJECT).put("organizationAlias",ALIAS).put("actorId","untrusted-actor").put("stableRequestOwner","untrusted-owner");}
  JsonNode scope(){return Json.object().put("organizationId",ORG);}
  ObjectNode rpc(){return Json.object().put("jsonrpc","2.0").put("id","wire-1").put("method","tools/list").set("params",Json.object());}
  ObjectNode capture(JsonNode rpc,String response,String nonce,String observed,String finished) {
    var c=Json.object().put("recordType","SERVER_AUTHENTICATED_MCP_CAPTURE_V1").put("captureId",ID).put("requestNonce",nonce)
      .put("endpointPath","/mcp/ontology").put("requestSha256",NativeClientPolicy.sha(rpc.toString())).put("responseSha256",NativeClientPolicy.sha(response))
      .put("httpStatus",200).put("rpcMethod",rpc.path("method").asText()).put("toolName",rpc.path("params").path("name").asText(""))
      .put("observedAt",observed).put("completedAt",finished).put("expiresAt",Instant.parse(finished).plusSeconds(60).toString())
      .put("authorityAsOf","2026-10-07T00:00:00Z").put("observationPhase","BEFORE_DISPATCH_READ_SNAPSHOT").put("actionAuthorization","NOT_ASSERTED");
    c.set("rpcId",rpc.path("id"));c.set("identity",Json.object().put("issuer",ISSUER).put("subject",SUBJECT).put("organizationAlias",ALIAS)
      .put("organizationId",ORG).put("actorId",ACTOR).put("stableRequestOwner",OWNER).set("audiences",Json.array().add(AUDIENCE)));
    var grant=Json.object().put("ID",GRANT).put("actorId",ACTOR).put("delegatorId",ACTOR).put("revision",1)
      .put("validFrom","2026-10-01T00:00:00Z").put("validUntil","2026-11-01T00:00:00Z");
    grant.set("actions",Json.array().add(Json.object().put("capabilityId","getObject")));
    grant.set("scopes",Json.array().add(Json.object().put("scopeKind","ITEM").put("scopeId",ORG)).add(Json.object().put("scopeKind","PLACE").put("scopeId",OWNER)));
    c.set("grants",Json.array().add(grant));
    c.set("memberships",Json.array().add(Json.object().put("ID",OWNER).put("revision",1).put("validFrom","2026-10-01T00:00:00Z").put("validUntil","2026-11-01T00:00:00Z")));
    c.set("assignments",Json.array());return c;
  }
  void validate(ObjectNode c,JsonNode rpc,String reply){NativeMcpTransport.validateServerCapture(c,ID,NONCE,rpc,reply,200,actor(),scope(),SENT,DONE);}
  @Test void resolvedIdentityAndGrantObservationCanAccompanyDeniedActionWithoutImplyingAuthorization() {
    var rpc=rpc();String reply="{\"jsonrpc\":\"2.0\",\"id\":\"wire-1\",\"result\":{\"isError\":true,\"structuredContent\":{\"outcome\":\"REJECTED\",\"error\":{\"code\":\"FORBIDDEN\"}}}}";
    var capture=capture(rpc,reply,NONCE,"2026-10-24T00:00:01Z","2026-10-24T00:00:02Z");
    ((ObjectNode)capture.path("grants").get(0)).put("revokedAt","2026-10-23T00:00:00Z").put("revision",2);
    assertDoesNotThrow(()->validate(capture,rpc,reply));
    assertEquals(OWNER,capture.path("identity").path("stableRequestOwner").asText());
    assertNotEquals(actor().path("stableRequestOwner"),capture.path("identity").path("stableRequestOwner"));
  }
  @Test void forgedCorrelationIdentityGrantAndTimeMutantsFailClosed() {
    var rpc=rpc();String reply="{\"jsonrpc\":\"2.0\",\"id\":\"wire-1\"}";
    List<Consumer<ObjectNode>> mutants=List.of(
      c->c.put("captureId",UUID.randomUUID().toString()),c->c.put("requestNonce",UUID.randomUUID().toString()),
      c->c.put("requestSha256","0".repeat(64)),c->c.put("responseSha256","0".repeat(64)),c->c.put("rpcId","other"),
      c->c.put("rpcMethod","tools/call"),c->c.put("toolName","createWork"),c->c.put("httpStatus",403),c->c.put("httpStatus","200"),c->c.put("endpointPath","/mcp"),
      c->((ObjectNode)c.path("identity")).put("issuer","https://other.invalid"),c->((ObjectNode)c.path("identity")).put("subject","other"),
      c->((ObjectNode)c.path("identity")).put("organizationAlias","other"),c->((ObjectNode)c.path("identity")).put("organizationId",OWNER),
      c->((ObjectNode)c.path("identity")).set("audiences",Json.array().add("wrong")),c->((ObjectNode)c.path("identity")).remove("stableRequestOwner"),
      c->c.remove("grants"),c->c.remove("authorityAsOf"),c->c.set("memberships",Json.array()),c->c.set("memberships",Json.object()),c->((ObjectNode)c.path("grants").get(0)).remove("revision"),
      c->((ObjectNode)c.path("grants").get(0)).put("actorId",OWNER),c->((ObjectNode)c.path("grants").get(0)).remove("actions"),
      c->((ObjectNode)c.path("grants").get(0)).remove("scopes"),c->c.put("actionAuthorization","ALLOWED"),
      c->c.put("observedAt","2026-10-23T00:00:00Z"),c->c.put("completedAt","2026-10-25T00:00:00Z"),c->c.put("expiresAt",SENT));
    for(var mutant:mutants) {
      var c=capture(rpc,reply,NONCE,"2026-10-24T00:00:01Z","2026-10-24T00:00:02Z");mutant.accept(c);
      assertThrows(RuntimeException.class,()->validate(c,rpc,reply),c.toString());
    }
  }
  @Test void absentOrMismatchedReceiptIsSavedAsUnavailableAndCannotReturnTransportSuccess()throws Exception {
    runTransport(false,false);runTransport(true,true);
  }
  @Test void typedSourceNamespacesUseCurrentVarchar36ContractAndOtherDimensionsStillRequireUuid() {
    var rpc=rpc();String reply="{}";
    for(String namespace:List.of("carrier-it","SOURCE.v1:물류","n".repeat(36))) {
      var c=capture(rpc,reply,NONCE,"2026-10-24T00:00:01Z","2026-10-24T00:00:02Z");
      ((ArrayNode)c.path("grants").get(0).path("scopes")).add(Json.object().put("scopeKind","SOURCE").put("scopeId",namespace));
      var assignment=((ObjectNode)c.path("memberships").get(0)).deepCopy().put("capabilityId","getEvidence").put("scopeKind","SOURCE").put("scopeId",namespace);
      ((ArrayNode)c.path("assignments")).add(assignment);
      assertDoesNotThrow(()->validate(c,rpc,reply));
      assertEquals(namespace,c.path("grants").get(0).path("scopes").get(2).path("scopeId").asText());
      assertEquals("NOT_ASSERTED",c.path("actionAuthorization").asText());
    }
    List<ObjectNode> bad=List.of(
      Json.object().put("scopeKind","SOURCE"),Json.object().put("scopeId","carrier-it"),
      Json.object().put("scopeKind","SOURCE").putNull("scopeId"),Json.object().put("scopeKind","SOURCE").put("scopeId",42),
      Json.object().put("scopeKind","SOURCE").set("scopeId",Json.object().put("namespace","carrier-it")),
      Json.object().put("SOURCE","carrier-it"),Json.object().put("scopeKind","SOURCE").put("scopeId"," "),
      Json.object().put("scopeKind","SOURCE").put("scopeId","n".repeat(37)),
      Json.object().put("scopeKind","source").put("scopeId","carrier-it"),
      Json.object().put("scopeKind","NAMESPACE").put("scopeId",ORG));
    var mutants=new ArrayList<ObjectNode>(bad);
    for(String kind:List.of("ORGANIZATION","ITEM","WORK","PLACE","TARGET"))mutants.add(Json.object().put("scopeKind",kind).put("scopeId","carrier-it"));
    for(var invalid:mutants)for(boolean assignment:List.of(false,true)){
      var c=capture(rpc,reply,NONCE,"2026-10-24T00:00:01Z","2026-10-24T00:00:02Z");
      if(assignment){var a=((ObjectNode)c.path("memberships").get(0)).deepCopy().put("capabilityId","getEvidence");a.setAll(invalid);((ArrayNode)c.path("assignments")).add(a);}
      else ((ArrayNode)c.path("grants").get(0).path("scopes")).add(invalid);
      assertThrows(RuntimeException.class,()->validate(c,rpc,reply),invalid.toString());
    }
  }
  @Test void serverReceiptIsFetchedIndependentlyAndPersistedWithoutCredentialOrCallerAuthority()throws Exception {
    runTransport(true,false);
  }
  @Test void typedTradeScopesAreCapturedAsObservationsWithStrictUuidIdentifiers() {
    var rpc=rpc();String reply="{}";
    for(String kind:List.of("LOT","CUSTOMER","SUPPLIER"))for(boolean assignment:List.of(false,true)) {
      var c=capture(rpc,reply,NONCE,"2026-10-24T00:00:01Z","2026-10-24T00:00:02Z");
      var scope=Json.object().put("scopeKind",kind).put("scopeId",ORG);
      if(assignment){var a=((ObjectNode)c.path("memberships").get(0)).deepCopy().put("capabilityId","getObject");a.setAll(scope);((ArrayNode)c.path("assignments")).add(a);}
      else ((ArrayNode)c.path("grants").get(0).path("scopes")).add(scope);
      assertDoesNotThrow(()->validate(c,rpc,reply));
      assertEquals("NOT_ASSERTED",c.path("actionAuthorization").asText());
      if(assignment)((ObjectNode)c.path("assignments").get(0)).put("scopeId","carrier-it");else scope.put("scopeId","carrier-it");
      assertThrows(RuntimeException.class,()->validate(c,rpc,reply));
    }
  }
  void runTransport(boolean enabled,boolean mismatch)throws Exception {
    var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var snapshot=new ObjectNode[1];
    server.createContext("/mcp/ontology",exchange->{
      String raw=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);var request=Json.parse(raw);
      String response=Json.object().put("jsonrpc","2.0").set("id",request.path("id")).toString();
      snapshot[0]=capture(request,response,exchange.getRequestHeaders().getFirst("X-Mulino-Verification-Capture"),Instant.now().toString(),Instant.now().toString());
      if(mismatch)snapshot[0].put("requestSha256","f".repeat(64));
      if(enabled)exchange.getResponseHeaders().set("X-Mulino-Verification-Receipt",ID);
      exchange.sendResponseHeaders(200,response.getBytes(StandardCharsets.UTF_8).length);exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));exchange.close();
    });
    server.createContext("/internal/verification/mcp-captures/",exchange->{
      assertNotNull(exchange.getRequestHeaders().getFirst("Authorization"));String body=snapshot[0].toString();
      exchange.sendResponseHeaders(200,body.getBytes(StandardCharsets.UTF_8).length);exchange.getResponseBody().write(body.getBytes(StandardCharsets.UTF_8));exchange.close();
    });server.start();
    Path privateKey=temporary.resolve("private.pem");var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);var key=generator.generateKeyPair();
    Files.writeString(privateKey,"-----BEGIN PRIVATE KEY-----\n"+Base64.getEncoder().encodeToString(key.getPrivate().getEncoded())+"\n-----END PRIVATE KEY-----\n");
    Path root=Path.of(System.getProperty("repo.root"));var evidence=new NativeIntegrationEvidence(root);
    var configuration=new ActualConfiguration(URI.create("http://127.0.0.1:"+server.getAddress().getPort()),"jdbc:postgresql://unused/probe","unused","unused",privateKey,ISSUER,AUDIENCE,"SELFTEST");
    try {
      var transport=new NativeMcpTransport(configuration,evidence);
      if(!enabled||mismatch)assertThrows(IllegalStateException.class,()->transport.send("capture-selftest","tools/list",null,Json.object(),actor(),scope()));
      else {
        var result=transport.send("capture-selftest","tools/list",null,Json.object(),actor(),scope());var artifact=Json.read(root.resolve(result.artifact().path("path").asText()));
        assertEquals("CAPTURED",artifact.path("authenticatedActorStatus").asText());assertEquals(OWNER,artifact.path("serverAuthenticatedCapture").path("identity").path("stableRequestOwner").asText());
        NativeClientPolicy.rejectSecrets(artifact);assertFalse(artifact.toString().contains("untrusted-owner"));
      }
    } finally {server.stop(0);Files.deleteIfExists(privateKey);}
  }
}
