package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mulino.verification.*;
import org.mulino.verification.actual.ActualConfiguration;
import org.mulino.verification.modelbinding.FinalResponseClarification;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit protocol contract doubles. No model or real product acceptance is claimed by this suite. */
class NativeProductIntegrationTest {
    @TempDir Path tmp;
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath();
    @Test void serviceIsInstalledButMissingEvidenceAndDirectPortRemainClosed() {
        assertInstanceOf(NativeProductIntegration.class,NativeCodexClient.discover());
        var integration=new NativeProductIntegration(tmp,Map.of());
        assertEquals("DETERMINISTIC_EVIDENCE_MISSING",assertThrows(IllegalStateException.class,()->integration.requireReady("M01","turn-1")).getMessage());
        assertThrows(IllegalStateException.class,()->integration.open("direct","MCP",Json.object(),"조회",Json.object()));
    }
    @Test void preparationAndOnePassingProfileCannotAuthorizeAModel() {
        var manifest=Json.object().put("recordType","ACCEPTANCE_HARNESS_REPORT").put("preparationStatus","PREPARED");
        var profiles=Json.array();for(String p:NativeDeterministicGate.REQUIRED)profiles.add(Json.object().put("profile",p).put("status","PASS"));manifest.set("profiles",profiles);
        ((ObjectNode)profiles.get(2)).put("status","FAIL");
        assertEquals("DETERMINISTIC_PREREQUISITE_SCENARIOS_NOT_PASS",assertThrows(IllegalStateException.class,()->NativeDeterministicGate.requirePassingProfiles(manifest)).getMessage());
        ((ObjectNode)profiles.get(2)).put("status","PASS");profiles.add(profiles.get(0).deepCopy());
        assertThrows(IllegalStateException.class,()->NativeDeterministicGate.requirePassingProfiles(manifest));
    }
    @Test void rawFinalTextIsPreservedButNeverLabeledSemanticSuccess() throws Exception {
        var store=new NativeIntegrationEvidence(root);var raw=Json.object();raw.set("result",Json.object().put("finalResponse","수령 완료, 의무 종료.\n실제 값?"));
        var descriptor=store.save("contract-capture",raw);Path capture=root.resolve(descriptor.path("path").asText());
        var result=NativeFinalResponseObserver.observe("turn-1/agent",Json.object().put("installationId","i"),capture,raw,store);
        assertEquals(StepResult.DriverStatus.NOT_IMPLEMENTED,result.driverStatus());assertNull(result.response());
        var observation=Json.read(root.resolve(result.artifactRefs().get(1)));
        assertEquals(raw.path("result").path("finalResponse"),observation.path("sourceResponse"));
        assertEquals(Json.sha256(capture),observation.path("sourceCapture").path("sha256").asText());
        assertFalse(observation.has("claims"));assertEquals("CAPTURED_CONTRACT_SELFTEST",observation.path("evidenceClass").asText());
        assertThrows(IllegalStateException.class,()->NativeFinalResponseObserver.observe("turn",Json.object(),capture,Json.object(),store));
    }
    @Test void clarificationUsesIndependentObservationOfTextRatherThanObjectFieldOrSelfLabel() {
        var response=Json.MAPPER.getNodeFactory().textNode("수량을 알려 주세요.");
        var observation=Json.object().put("complete",true);observation.set("sourceResponse",response);
        observation.set("claims",Json.object().put("execution","NEEDS_INPUT"));
        observation.set("extractor",Json.object().put("independent",true).put("source","INDEPENDENT_FINAL_RESPONSE_SEMANTICS"));
        assertDoesNotThrow(()->FinalResponseClarification.checkRequirement(observation,response,true));
        assertThrows(IllegalArgumentException.class,()->FinalResponseClarification.checkRequirement(observation,response,false));
        ((ObjectNode)observation.path("extractor")).put("independent",false);
        assertThrows(IllegalArgumentException.class,()->FinalResponseClarification.checkRequirement(observation,response,true));
    }
    @Test void transportPreservesRawRpcSchemaErrorAndCredentialsStayOffDisk() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var request=new AtomicReference<JsonNode>();var bearer=new AtomicReference<String>();
        server.createContext("/mcp/ontology",exchange->{
            try {
                bearer.set(exchange.getRequestHeaders().getFirst("Authorization"));
                assertEquals("application/json, text/event-stream",exchange.getRequestHeaders().getFirst("Accept"));
                JsonNode input=Json.parse(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));request.set(input);
                var output=Json.object().put("jsonrpc","2.0");output.set("id",input.path("id"));
                if(input.path("method").asText().equals("tools/list")) {
                    var schema=Json.object().put("type","object").put("additionalProperties",false);
                    schema.set("properties",Json.object().set("id",Json.object().put("type","string")));
                    output.set("result",Json.object().set("tools",Json.array().add(Json.object().put("name","getObject").put("description","public schema").set("inputSchema",schema))));
                } else output.set("result",Json.object().put("isError",true).set("content",Json.array().add(Json.object().put("type","text").put("text","typed rejection"))));
                byte[] bytes=output.toString().getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);
            } finally {exchange.close();}
        });server.start();
        try {
            var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);
            Path key=tmp.resolve("private.pem");Files.writeString(key,"-----BEGIN PRIVATE KEY-----\n"+Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded())+"\n-----END PRIVATE KEY-----");
            var config=new ActualConfiguration(URI.create("http://127.0.0.1:"+server.getAddress().getPort()),"jdbc:postgresql://127.0.0.1:1/unused","u","p",key,"issuer","aud","commit");
            var transport=new NativeMcpTransport(config,new NativeIntegrationEvidence(root));var actor=Json.object().put("issuer","issuer").put("audience","aud").put("subject","subject").put("organizationAlias","org");
            var listed=transport.send("list","tools/list",null,Json.object(),actor,Json.object());
            assertEquals(listed.response().path("result").path("tools").get(0).path("inputSchema"),transport.tools(listed).get(0).path("inputSchema"));
            var arguments=Json.object().put("id","verbatim-user-id");
            var rejected=transport.send("call","tools/call","getObject",arguments,actor,Json.object().put("installationId","i"));
            assertEquals(arguments,request.get().path("params").path("arguments"));assertFalse(NativeMcpTransport.nativeReply(rejected).path("success").asBoolean());
            String saved=Files.readString(root.resolve(rejected.artifact().path("path").asText()));
            assertFalse(saved.contains(bearer.get()));assertFalse(saved.contains("BEGIN PRIVATE KEY"));assertTrue(saved.contains("NOT_IMPLEMENTED"));
            assertThrows(IllegalStateException.class,()->transport.send("call","tools/call","getObject",arguments,actor.deepCopy().put("issuer","unbound"),Json.object()));
        } finally {server.stop(0);}
    }
}
