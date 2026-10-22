package org.mulino.verification.actual;

import java.net.*;
import java.nio.file.*;
import java.security.KeyPairGenerator;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real process/HTTP clock transport. This does not claim ontology business acceptance. */
final class ActualStoppedClockTest {
    @TempDir Path dir;
    private ActualAcceptanceDriver driver;
    private Path pid,app;
    private String previousSuite;
    @BeforeEach void setup() throws Exception {
        previousSuite=System.getProperty("verification.actual.suiteIsolation");
        System.setProperty("verification.actual.suiteIsolation","true");
        Path root=Path.of(System.getProperty("repo.root"));
        var keys=KeyPairGenerator.getInstance("RSA");keys.initialize(2048);
        Path key=dir.resolve("private.pem");
        Files.writeString(key,"-----BEGIN PRIVATE KEY-----\n"+Base64.getEncoder().encodeToString(keys.generateKeyPair().getPrivate().getEncoded())+"\n-----END PRIVATE KEY-----\n");
        app=dir.resolve("clock.py");
        Files.writeString(app,"""
            import http.server, json, sys
            options=dict(arg[2:].split('=',1) for arg in sys.argv[1:])
            instant=options.get('mulino.verification.instant','2026-10-07T09:00:00Z')
            class Handler(http.server.BaseHTTPRequestHandler):
                def log_message(self,*args): pass
                def do_GET(self): self.send_response(401); self.end_headers()
                def do_POST(self):
                    global instant
                    instant=json.loads(self.rfile.read(int(self.headers['Content-Length'])))['instant']
                    data=json.dumps({'instant':instant,'profile':'verification','authorityClock':True}).encode()
                    self.send_response(200);self.end_headers();self.wfile.write(data)
            http.server.HTTPServer(('127.0.0.1',int(options['server.port'])),Handler).serve_forever()
            """);
        int port;try(var socket=new ServerSocket(0)){port=socket.getLocalPort();}
        pid=dir.resolve("backend.pid");Files.writeString(pid,"");
        driver=new ActualAcceptanceDriver(root,new ActualConfiguration(URI.create("http://127.0.0.1:"+port),"jdbc:postgresql://unused","unused","unused",key,"https://test.invalid","ontology","clock-test"),Map.of(
            "ACTUAL_BACKEND_PID_FILE",pid.toString(),"ACTUAL_BACKEND_LOG",dir.resolve("backend.log").toString(),
            "ACTUAL_BACKEND_ARGV",Json.MAPPER.writeValueAsString(List.of("python3",app.toString()))));
        var field=ActualAcceptanceDriver.class.getDeclaredField("controlActor");field.setAccessible(true);
        field.set(driver,Json.parse("{\"subject\":\"clock-actor\",\"organizationAlias\":\"ORG\"}"));
    }
    @AfterEach void cleanup() throws Exception {
        if(pid!=null&&!Files.readString(pid).isBlank()){
            var handle=ProcessHandle.of(Long.parseLong(Files.readString(pid).trim()));
            if(handle.isPresent()){handle.get().destroyForcibly();handle.get().onExit().get(5,java.util.concurrent.TimeUnit.SECONDS);}
        }
        if(previousSuite==null)System.clearProperty("verification.actual.suiteIsolation");else System.setProperty("verification.actual.suiteIsolation",previousSuite);
    }
    private Path rootPath(String ref) {return Path.of(System.getProperty("repo.root")).resolve(ref);}
    private StepResult process(String operation) {
        return driver.control(operation,Json.parse("{\"type\":\"process\",\"operation\":\""+operation+"\",\"parameters\":{\"processId\":\"application\"}}"));
    }
    private StepResult clock(String instant) {
        return driver.control("clock",Json.parse("{\"type\":\"clock\",\"operation\":\"advanceTo\",\"parameters\":{\"instant\":\""+instant+"\"}}"));
    }
    @Test void stoppedClockPersistsIntentAndRestartRequiresActualServerProof() throws Exception {
        process("start");
        assertTrue(clock("2026-10-07T09:00:00Z").data().path("authorityClockApplied").asBoolean());
        process("stop");
        var pending=clock("2026-10-07T09:00:11Z");
        assertEquals("PENDING_RESTART",pending.data().path("status").asText());
        assertFalse(pending.data().path("authorityClockApplied").asBoolean(true));
        assertNull(pending.response());
        assertThrows(IllegalStateException.class,()->clock("2026-10-07T09:00:10Z"));
        var receipt=Json.read(rootPath(pending.artifactRefs().getFirst()));
        assertFalse(receipt.path("httpExecuted").asBoolean(true));
        assertEquals("2026-10-07T09:00:11Z",Json.read(Path.of(pid+".clock.json")).path("instant").asText());
        var restarted=process("start");
        assertEquals("2026-10-07T09:00:11Z",restarted.data().path("clockReapplied").asText());
        assertTrue(restarted.data().path("clockServerObservation").path("authorityClock").asBoolean());
    }
    @Test void restartWithUnprovenServerClockDoesNotReturnAppliedReceipt() throws Exception {
        process("start");clock("2026-10-07T09:00:00Z");process("stop");clock("2026-10-07T09:00:11Z");
        Files.writeString(app,Files.readString(app).replace("'profile':'verification'","'profile':'wrong'"));
        var failure=assertThrows(IllegalStateException.class,()->process("start"));
        assertTrue(failure.getMessage().contains("did not prove applied authority clock"));
        assertEquals("PENDING_RESTART",Json.read(Path.of(pid+".clock.json")).path("status").asText());
    }
    @Test void malformedStoppedInstantCannotBecomePendingOrApplied() {
        process("start");process("stop");
        assertThrows(IllegalStateException.class,()->clock("not-an-instant"));
        assertFalse(Files.exists(Path.of(pid+".clock.json")));
    }
}
