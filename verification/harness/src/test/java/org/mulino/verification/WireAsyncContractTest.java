package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;

/** Mechanical raw submission/terminal ACK samples; no product, transport, or model execution. */
public final class WireAsyncContractTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private static final String ARTIFACT="verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/canned-observations.json";
    private static final String SUBCASE="hold-preserves-physical";
    private JsonNode raw() {return Json.parse("{\"headers\":{\"Content-Type\":\"text/plain\",\"MCP-Protocol-Version\":\"intentionally-invalid\"},\"body\":\"{\\\"method\\\":\\\"different/method\\\",\\\"id\\\":\\\"new-rpc-id\\\",\\\"effectKey\\\":\\\"same-effect-key\\\"}\"}");}
    private ObjectNode start(String id) {
        ObjectNode a=Json.object();a.put("id",id).put("kind","start");a.set("evidenceRefs",Json.parse("[\"CAPTURED_SELFTEST:submission\"]"));
        ObjectNode call=Json.object();call.put("id",id+"-call").put("kind","invoke").put("actorRef","qc").put("route","wire").put("protocolOperation","tools/call");call.set("request",raw());call.set("evidenceRefs",Json.parse("[\"CAPTURED_SELFTEST:raw\"]"));a.set("call",call);return a;
    }
    private ObjectNode awaitAction(String id,String submitted) {
        ObjectNode a=Json.object();a.put("id",id).put("kind","await").put("awaitActionId",submitted);a.set("evidenceRefs",Json.parse("[\"CAPTURED_SELFTEST:terminal\"]"));return a;
    }
    private JsonNode assertion(String id,String source) throws Exception {
        ObjectNode a=(ObjectNode)Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json")).path("subcases").get(0).path("assertions").get(0).deepCopy();
        a.put("id",id).put("op","equals");a.remove(List.of("unit","unitSource"));a.set("source",Json.parse("{\"actionId\":\""+source+"\",\"pointer\":\"/data/completed\"}"));a.put("expected",true);return a;
    }
    private Path caseFile(List<JsonNode> actions,List<JsonNode> assertions) throws Exception {
        JsonNode c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
        ObjectNode sub=(ObjectNode)c.path("subcases").get(0);sub.set("actions",Json.MAPPER.valueToTree(actions));sub.set("assertions",Json.MAPPER.valueToTree(assertions));
        Path p=Files.createTempFile(root.resolve("verification/harness/target"),"wire-async-",".json");Json.write(p,c);return p;
    }
    private static StepResult ack(String id,JsonNode data) {
        ObjectNode p=Json.object();p.put("adapter","wire-async-captured-selftest").put("adapterVersion","1.0").put("buildVersion","SELFTEST").putNull("authenticatedActor").put("source","CANNED_CONTRACT_SELFTEST").put("independent",false).put("scopeComplete",true).putNull("sourceQuery").putNull("snapshot");
        return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,null,"CAPTURED_SELFTEST mechanical ACK only",p,List.of(ARTIFACT));
    }
    private static class NoAsyncWire implements AcceptanceDriver {
        public Set<String> availableAdapters(){return Set.of("CAPTURED_SELFTEST_ONLY");}
        public StepResult installFixture(String id,JsonNode f){throw new AssertionError("Unexpected fixture port");}
        public StepResult invoke(String id,String route,JsonNode actor,String cap,JsonNode request){throw new AssertionError("Unexpected business invoke");}
        public StepResult query(String id,String route,JsonNode actor,String cap,JsonNode request){throw new AssertionError("Unexpected business query");}
        public StepResult observe(String id,JsonNode request){throw new AssertionError("Unexpected observer");}
        public StepResult control(String id,JsonNode request){throw new AssertionError("Unexpected control");}
        public StepResult start(String id,String route,JsonNode actor,String cap,JsonNode request){throw new AssertionError("Raw wire must not use business start");}
        public StepResult wire(String id,JsonNode actor,String operation,JsonNode request){throw new AssertionError("Async submission must not call sync wire");}
        public StepResult await(String id,JsonNode handle,int timeout){throw new AssertionError("Unavailable submission cannot be awaited as executed");}
    }
    private static final class CapturedAsync extends NoAsyncWire {
        final Map<String,JsonNode> requests=new ConcurrentHashMap<>(),awaitedHandles=new ConcurrentHashMap<>();
        final Map<String,String> operations=new ConcurrentHashMap<>();
        final boolean wrongHandle,missingSecondAwait;
        CapturedAsync(boolean wrongHandle,boolean missingSecondAwait){this.wrongHandle=wrongHandle;this.missingSecondAwait=missingSecondAwait;}
        public StepResult startWire(String id,JsonNode actor,String operation,JsonNode request) {
            requests.put(id,request.deepCopy());operations.put(id,operation);
            return ack(id,Json.parse(id.equals("left")?"{\"invocationHandle\":\"left-issued-handle\"}":"{\"invocationHandle\":\"right-issued-handle\"}"));
        }
        public StepResult await(String id,JsonNode handle,int timeout) {
            awaitedHandles.put(id,handle.deepCopy());
            if(missingSecondAwait && id.equals("await-right")) return StepResult.missing(id,"NOT_IMPLEMENTED: terminal capture absent");
            return ack(id,Json.parse("{\"invocationHandle\":\""+(wrongHandle?"different-issued-handle":handle.asText())+"\",\"completed\":true,\"terminalStatus\":\"SUCCEEDED\"}"));
        }
    }
    private Path parallelCase() throws Exception {
        ObjectNode p=Json.object();p.put("id","parallel").put("kind","parallel");p.set("evidenceRefs",Json.parse("[\"CAPTURED_SELFTEST:parallel-submission\"]"));
        var branches=Json.array();for(String id:List.of("left","right")) {ObjectNode b=Json.object();b.put("id",id);b.set("actions",Json.array().add(start(id)));branches.add(b);}p.set("branches",branches);
        return caseFile(List.of(p,awaitAction("await-left","left"),awaitAction("await-right","right")),List.of(assertion("left-terminal","await-left"),assertion("right-terminal","await-right")));
    }
    private CaseRunner runner(Path path,AcceptanceDriver driver) throws Exception {return CaseRunner.harnessSelftest(new ContractValidator(root),driver,new AgentRunner.Scripted(),path,SUBCASE);}
    @Test void parallelRawWireUsesTypedAsyncPortPreservesHostileInputAndAwaitsEachHandle() throws Exception {
        CapturedAsync port=new CapturedAsync(false,false);CaseRunner r=runner(parallelCase(),port);assertEquals("PASS",r.run(false));r.verifyComplete();
        assertEquals(raw(),port.requests.get("left"));assertEquals(raw(),port.requests.get("right"));assertEquals(Map.of("left","tools/call","right","tools/call"),port.operations);
        assertEquals("left-issued-handle",port.awaitedHandles.get("await-left").asText());assertEquals("right-issued-handle",port.awaitedHandles.get("await-right").asText());
        assertFalse(r.results().get("parallel").path("data").path("transactionCompletionClaimed").asBoolean());
    }
    @Test void defaultAsyncWireNeverCallsSyncPortOrReturnsFabricatedFacts() throws Exception {
        NoAsyncWire port=new NoAsyncWire();StepResult missing=port.startWire("probe",Json.object(),"tools/call",raw());assertEquals(StepResult.DriverStatus.NOT_IMPLEMENTED,missing.driverStatus());assertNull(missing.data());assertNull(missing.response());
        CaseRunner r=runner(parallelCase(),port);assertEquals("NOT_RUN",r.run(false));
        for(String id:List.of("left","right","await-left","await-right")) {assertEquals("NOT_IMPLEMENTED",r.results().get(id).path("driverStatus").asText());assertTrue(r.results().get(id).path("data").isNull());}
        assertThrows(AssertionError.class,r::verifyComplete);
    }
    @Test void missingOrWrongTerminalCannotCompleteAsyncWireCase() throws Exception {
        Path file=parallelCase();assertEquals("NOT_RUN",runner(file,new CapturedAsync(false,true)).run(false));
        assertTrue(assertThrows(IllegalArgumentException.class,()->runner(file,new CapturedAsync(true,false)).run(false)).getMessage().contains("handle differs"));
        JsonNode c=Json.read(file);((com.fasterxml.jackson.databind.node.ArrayNode)c.path("subcases").get(0).path("actions")).remove(2);Json.write(file,c);
        assertTrue(assertThrows(IllegalArgumentException.class,()->new ContractValidator(root).caseFile(file)).getMessage().contains("Every start"));
    }
    @Test void protocolCallRequiresWireRouteAndCannotAlsoDeclareBusinessCapability() throws Exception {
        ObjectNode submitted=start("left");((ObjectNode)submitted.path("call")).put("route","api");Path invalid=caseFile(List.of(submitted,awaitAction("await-left","left")),List.of(assertion("terminal","await-left")));
        assertThrows(IllegalArgumentException.class,()->new ContractValidator(root).caseFile(invalid));
        ((ObjectNode)submitted.path("call")).put("route","wire").put("capabilityId","placeHold");Json.write(invalid,caseFileNode(submitted));assertThrows(IllegalArgumentException.class,()->new ContractValidator(root).caseFile(invalid));
    }
    private JsonNode caseFileNode(JsonNode start) throws Exception {return Json.read(caseFile(List.of(start,awaitAction("await-left","left")),List.of(assertion("terminal","await-left"))));}
}
