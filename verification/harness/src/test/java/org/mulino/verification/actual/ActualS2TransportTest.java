package org.mulino.verification.actual;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real isolated HTTP transport plumbing only; no product acceptance claim. */
final class ActualS2TransportTest {
    @Test void commandsRecordsAndConcurrentTerminalReceiptsPreserveServerResponse() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var executor=Executors.newFixedThreadPool(2);server.setExecutor(executor);
        var entered=new CountDownLatch(2);var release=new CountDownLatch(1);
        var paths=new CopyOnWriteArrayList<String>();var bodies=new CopyOnWriteArrayList<String>();
        server.createContext("/api/ontology/",exchange->{
            try {
                paths.add(exchange.getRequestURI().getPath());bodies.add(new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
                assertTrue(exchange.getRequestHeaders().getFirst("Authorization").startsWith("Bearer "));
                entered.countDown();if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Controlled test barrier was not released");
                byte[] response="{\"outcome\":\"COMMITTED\",\"revision\":7}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);
            }catch(InterruptedException failure){Thread.currentThread().interrupt();}
            finally{exchange.close();}
        });
        Path root=Files.createTempDirectory("actual-s2-transport-");Path pem=root.resolve("private.pem");
        var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);var keys=generator.generateKeyPair();
        Files.writeString(pem,"-----BEGIN PRIVATE KEY-----\n"+Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded())+"\n-----END PRIVATE KEY-----\n");
        server.start();
        try {
            var config=new ActualConfiguration(URI.create("http://127.0.0.1:"+server.getAddress().getPort()),"jdbc:postgresql://localhost/unused","unused","unused",pem,"https://mulino-native.invalid","isolated-ontology","test-source");
            var driver=new ActualAcceptanceDriver(root,config);var actor=Json.parse("{\"subject\":\"actor\",\"organizationAlias\":\"ORG\"}");
            var command=Json.parse("{\"intentKind\":\"COMMAND\",\"commandIdempotencyKey\":\"key-1\",\"slots\":{\"quantity\":\"10\"}}");
            var record=Json.parse("{\"intentKind\":\"RECORD\",\"commandIdempotencyKey\":\"key-2\",\"slots\":{\"value\":\"observed\"}}");
            var first=driver.start("start-1","api",actor,"createDraft",command);var second=driver.start("start-2","api",actor,"recordActivity",record);
            assertTrue(entered.await(5,TimeUnit.SECONDS),"Both real HTTP requests must enter before release");
            assertNotEquals(first.data().path("invocationHandle"),second.data().path("invocationHandle"));release.countDown();
            for(var submission:List.of(first,second)) {
                var terminal=driver.await("terminal",submission.data().path("invocationHandle"),10);
                assertTrue(terminal.data().path("completed").asBoolean());assertEquals("SUCCEEDED",terminal.data().path("terminalStatus").asText());
                assertEquals(submission.data().path("invocationHandle"),terminal.data().path("invocationHandle"));
                assertEquals(7,terminal.response().path("revision").asInt());
                var receipt=Json.read(root.resolve(terminal.artifactRefs().getFirst()));assertEquals(terminal.response(),receipt.path("response"));
                assertFalse(Files.readString(root.resolve(terminal.artifactRefs().getFirst())).contains("Bearer"));
            }
            assertTrue(paths.contains("/api/ontology/commands/createDraft"));assertTrue(paths.contains("/api/ontology/records/recordActivity"));
            assertTrue(bodies.contains(command.toString()));assertTrue(bodies.contains(record.toString()));
            assertThrows(IllegalArgumentException.class,()->driver.await("unknown",Json.MAPPER.getNodeFactory().textNode("missing"),1));
        }finally {
            release.countDown();server.stop(0);executor.shutdownNow();
            try(var files=Files.walk(root)){for(Path path:files.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}
        }
    }
}
