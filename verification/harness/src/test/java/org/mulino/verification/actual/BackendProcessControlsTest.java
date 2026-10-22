package org.mulino.verification.actual;

import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real local HTTP process and extractor smoke; this is not ontology business acceptance. */
final class BackendProcessControlsTest {
    @Test void fixtureAttachmentAndRealLifecycleCarryValidatedEvidence() throws Exception {
        Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
        Path dir=Files.createTempDirectory("mulino-process-smoke-");
        Path pid=dir.resolve("backend.pid"), app=dir.resolve("http_process.py");
        Files.writeString(pid,"");
        Files.writeString(app,"import http.server, sys\nhttp.server.HTTPServer(('127.0.0.1', int(sys.argv[-1].split('=')[1])), http.server.BaseHTTPRequestHandler).serve_forever()\n");
        int port;try(var socket=new ServerSocket(0)){port=socket.getLocalPort();}
        var env=Map.of("ACTUAL_BACKEND_PID_FILE",pid.toString(),"ACTUAL_BACKEND_LOG",dir.resolve("application.log").toString(),
            "ACTUAL_BACKEND_ARGV",Json.MAPPER.writeValueAsString(List.of("python3",app.toString())));
        var controls=new BackendProcessControls(root,"process-smoke-"+UUID.randomUUID(),URI.create("http://127.0.0.1:"+port),env);
        HostProcessControls.Evidence evidence=()->Json.object().put("adapter","local-process-smoke").put("adapterVersion","1.0")
            .put("buildVersion","local").put("source","ACTUAL_HOST_PROCESS").put("independent",true).put("scopeComplete",true);
        try {
            controls.ensureRunning();
            String initialPid=Files.readString(pid);
            for(String role:List.of("api","scheduler","worker-a","worker-b","due-sweeper")) {
                var request=request("start",role);
                var result=controls.control("attach-"+role,request,evidence);
                HostObservationValidator.validate(new ContractValidator(root),request,result,true);
                assertFalse(result.data().path("launched").asBoolean());
                assertEquals("ATTACHED_EXISTING",result.data().path("processAction").asText());
                assertEquals(initialPid,result.data().at("/hostObservation/processObservation/after/instanceId").asText());
                // A same-instance attachment must not become a fictitious launch or hidden restart.
                ((ObjectNode)result.data()).put("launched",true);
                assertTrue(assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(new ContractValidator(root),request,result,true)).getMessage().contains("cannot claim"));
                ((ObjectNode)result.data()).put("launched",false);
                ((ObjectNode)result.data().at("/hostObservation/processObservation/after")).put("instanceId","different");
                assertTrue(assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(new ContractValidator(root),request,result,true)).getMessage().contains("same running application"));
            }
            var restart=request("restart","application");
            var restarted=controls.control("restart",restart,evidence);
            HostObservationValidator.validate(new ContractValidator(root),restart,restarted,true);
            assertTrue(restarted.data().path("launched").asBoolean());
            assertNotEquals(initialPid,Files.readString(pid));
            assertFalse(ProcessHandle.of(Long.parseLong(initialPid)).map(ProcessHandle::isAlive).orElse(false));
            var stop=request("stop","application");
            var stopped=controls.control("stop",stop,evidence);
            HostObservationValidator.validate(new ContractValidator(root),stop,stopped,true);
            assertEquals("STOPPED",stopped.data().at("/hostObservation/processObservation/after/status").asText());
            var failed=controls.control("stop-again",stop,evidence);
            HostObservationValidator.validate(new ContractValidator(root),stop,failed,true);
            assertEquals("FAILED",failed.data().at("/hostObservation/processObservation/terminalStatus").asText());
            var start=request("start","application");
            var started=controls.control("start",start,evidence);
            HostObservationValidator.validate(new ContractValidator(root),start,started,true);
            assertTrue(started.data().path("launched").asBoolean());
            assertEquals("STARTED",started.data().path("processAction").asText());
        } finally {
            String current=Files.readString(pid).trim();
            if(!current.isEmpty()) {
                var handle=ProcessHandle.of(Long.parseLong(current));
                if(handle.isPresent()) {handle.get().destroyForcibly();handle.get().onExit().get(5,java.util.concurrent.TimeUnit.SECONDS);}
            }
            try(var paths=Files.walk(dir)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
        }
    }
    private ObjectNode request(String operation,String role) {
        var request=Json.object().put("type","process").put("operation",operation);
        request.set("parameters",Json.object().put("processId",role).put("environmentId","local-process-smoke"));
        return request;
    }
}
