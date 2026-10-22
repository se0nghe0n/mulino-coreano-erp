package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyPairGenerator;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Loopback transport tests, not acceptance of the backend's claim or effect checks. */
final class ActualWorkerTransportTest {
    @TempDir Path root;
    private HttpServer server;
    private ActualAcceptanceDriver driver;
    private String oldSuite;
    private final AtomicReference<JsonNode> received=new AtomicReference<>();
    private final AtomicReference<String> authorization=new AtomicReference<>();
    private final AtomicReference<String> path=new AtomicReference<>();
    private final AtomicReference<String> method=new AtomicReference<>();
    private int status=403;
    private String response="{\"outcome\":\"REJECTED\",\"error\":{\"code\":\"FORBIDDEN\"}}";
    private final JsonNode actor=Json.parse("{\"subject\":\"worker-actor\",\"organizationAlias\":\"ORG\"}");
    private final JsonNode intent=Json.parse("{\"intentKind\":\"COMMAND\",\"capabilityId\":\"createWork\",\"definitionVersion\":\"v1\",\"commandIdempotencyKey\":\"worker-key\",\"slots\":{\"workType\":\"purchase\"}}");

    @BeforeEach void setup() throws Exception {
        oldSuite=System.getProperty("verification.actual.suiteIsolation");
        System.setProperty("verification.actual.suiteIsolation","true");
        var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);
        Path pem=root.resolve("private.pem");
        Files.writeString(pem,"-----BEGIN PRIVATE KEY-----\n"+Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded())+"\n-----END PRIVATE KEY-----\n");
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            try {
                received.set(Json.parse(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8)));
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                path.set(exchange.getRequestURI().getPath());method.set(exchange.getRequestMethod());
                byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);
            } finally {exchange.close();}
        });
        server.start();
        driver=new ActualAcceptanceDriver(root,new ActualConfiguration(URI.create("http://127.0.0.1:"+server.getAddress().getPort()),"jdbc:postgresql://localhost/unused","unused","unused",pem,"https://test.invalid","ontology","transport-test"));
        // No DB fixture is claimed here: satisfy the transport-only installation guard.
        var installed=ActualAcceptanceDriver.class.getDeclaredField("externalOrganization");installed.setAccessible(true);installed.set(driver,"ORG");
    }
    @AfterEach void cleanup() {
        if(server!=null)server.stop(0);
        if(oldSuite==null)System.clearProperty("verification.actual.suiteIsolation");else System.setProperty("verification.actual.suiteIsolation",oldSuite);
    }
    @Test void missingClaimReachesEndpointAndPreservesDenial() throws Exception {
        assertTrue(driver.availableAdapters().contains("worker"));
        var result=driver.invoke("missing","worker",actor,"createWork",intent);
        assertEquals(Set.of("intent"),keys(received.get()));
        assertEquals(intent,received.get().path("intent"));
        assertEquals("FORBIDDEN",result.response().at("/error/code").asText());
        assertReceipt(result);
    }
    @Test void explicitClaimIsSentUnchangedAndOnlyServerResponseDeterminesOutcome() throws Exception {
        var request=Json.object();request.set("intent",intent.deepCopy());
        // Opaque transport sample, deliberately not described as an installed/valid lease.
        request.set("claim",Json.parse("{\"claimId\":\"opaque-claim\",\"fencingToken\":9223372036854775806}"));
        var unchanged=request.deepCopy();
        var denied=driver.invoke("denied","worker",actor,"createWork",request);
        assertEquals(unchanged,received.get());assertEquals(unchanged,request);assertReceipt(denied);
        status=200;response="{\"outcome\":\"APPLIED\",\"data\":{\"revision\":23}}";
        var applied=driver.invoke("applied","worker",actor,"createWork",request);
        assertEquals(23,applied.response().at("/data/revision").asInt());assertReceipt(applied);
    }
    @Test void malformedClaimAndAuthenticationVariantAreNotRepaired() throws Exception {
        var request=Json.object();request.set("intent",intent);request.put("claim","malformed");
        var declared=Json.parse("{\"authenticationTest\":{\"variant\":\"subject\",\"invalidValue\":\"unknown-actor\"}}");
        var result=driver.invoke("invalid","worker",actor,"createWork",request,declared);
        assertEquals(request,received.get());
        var jwt=Json.parse(new String(Base64.getUrlDecoder().decode(authorization.get().substring(7).split("\\.")[1]),StandardCharsets.UTF_8));
        assertEquals("unknown-actor",jwt.path("sub").asText());assertReceipt(result);
    }
    @Test void missingEndpointRemainsNotImplemented() {
        status=404;response="{}";
        var result=driver.invoke("absent","worker",actor,"createWork",intent);
        assertNotEquals(StepResult.DriverStatus.EXECUTED,result.driverStatus());
        assertTrue(result.reason().contains("/api/runtime/worker/commands"));
    }
    private void assertReceipt(StepResult result) throws Exception {
        assertEquals("/api/runtime/worker/commands",path.get());assertEquals("POST",method.get());
        assertTrue(authorization.get().startsWith("Bearer "));
        assertEquals(StepResult.DriverStatus.EXECUTED,result.driverStatus());
        assertEquals(status,result.data().path("httpStatus").asInt());assertEquals("worker",result.data().path("route").asText());
        assertEquals(Json.parse(response),result.response());
        var receipt=Json.read(root.resolve(result.artifactRefs().getFirst()));
        assertEquals(received.get(),receipt.path("wireRequest"));assertEquals(result.response(),receipt.path("response"));
        assertEquals(path.get(),receipt.path("path").asText());assertEquals(status,receipt.path("httpStatus").asInt());
        assertFalse(receipt.toString().contains(authorization.get().substring(7)));
        assertEquals(64,receipt.path("credentialSha256").asText().length());
    }
    private static Set<String> keys(JsonNode value){var names=new HashSet<String>();value.fieldNames().forEachRemaining(names::add);return names;}
}
