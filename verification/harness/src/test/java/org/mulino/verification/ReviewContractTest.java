package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Mechanical captured-port contract samples, never a business service or product execution. */
public final class ReviewContractTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private final String artifact="verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/canned-observations.json";
    private final String subcase="hold-preserves-physical";
    private JsonNode action(String id,String kind) { return Json.parse("{\"id\":\""+id+"\",\"kind\":\""+kind+"\",\"evidenceRefs\":[\"CAPTURED_SELFTEST\"]}"); }
    private ObjectNode invocation(String id,String kind) {
        ObjectNode a=(ObjectNode)action(id,kind);a.put("actorRef","qc").put("route","api").put("capabilityId","placeHold");a.set("request",Json.object());return a;
    }
    private JsonNode assertion(String id,String action,String pointer,JsonNode expected) throws Exception {
        ObjectNode a=(ObjectNode)Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json")).path("subcases").get(0).path("assertions").get(0).deepCopy();
        a.put("id",id).put("op","equals");a.remove(List.of("unit","unitSource"));a.set("source",Json.parse("{\"actionId\":\""+action+"\",\"pointer\":\""+pointer+"\"}"));a.set("expected",expected);return a;
    }
    private Path caseFile(List<JsonNode> actions,List<JsonNode> assertions) throws Exception {
        JsonNode c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
        ObjectNode sub=(ObjectNode)c.path("subcases").get(0);sub.set("actions",Json.MAPPER.valueToTree(actions));sub.set("assertions",Json.MAPPER.valueToTree(assertions));
        Path p=Files.createTempFile(root.resolve("verification/harness/target"),"review-contract-",".json");Json.write(p,c);return p;
    }
    private StepResult captured(String id,JsonNode data,JsonNode response,boolean independent) {
        ObjectNode p=Json.object();p.put("adapter","captured-contract-selftest").put("adapterVersion","1.0").put("buildVersion","SELFTEST").putNull("authenticatedActor").put("source","CANNED_CONTRACT_SELFTEST").put("independent",independent).put("scopeComplete",true);
        p.set("sourceQuery",independent?data.path("sourceQuery"):Json.MAPPER.nullNode());p.set("snapshot",independent?data.path("snapshot"):Json.MAPPER.nullNode());
        return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,response,null,p,List.of(artifact));
    }
    private CaseRunner runner(Path file,Map<String,StepResult> captures) throws Exception { return new CaseRunner(new ContractValidator(root),new Captures(captures),new AgentRunner.Scripted(),file,subcase); }
    private static final class Captures implements AcceptanceDriver {
        private final Map<String,StepResult> captures; Captures(Map<String,StepResult> captures){this.captures=captures;}
        private StepResult get(String id){return captures.getOrDefault(id,StepResult.missing(id,"NOT_IMPLEMENTED: selftest capture absent"));}
        public Set<String> availableAdapters(){return Set.of("CAPTURED_CONTRACT_SELFTEST_ONLY");}
        public StepResult installFixture(String id,JsonNode f){return get(id);} public StepResult invoke(String id,String r,JsonNode a,String c,JsonNode q){return get(id);}
        public StepResult query(String id,String r,JsonNode a,String c,JsonNode q){return get(id);} public StepResult observe(String id,JsonNode q){return get(id);}
        public StepResult control(String id,JsonNode q){return get(id);} public StepResult start(String id,String r,JsonNode a,String c,JsonNode q){return get(id);}
        public StepResult await(String id,JsonNode h,int t){return get(id);}
    }
    private ObjectNode observation() {
        return (ObjectNode)Json.parse("{\"snapshotRevision\":\"revision-W\",\"asOf\":\"2026-10-07T00:00:00Z\",\"knownAt\":\"2026-10-07T00:00:00Z\",\"scope\":{\"work\":\"W\"},\"scopeComplete\":true,\"sourceQuery\":{\"statementId\":\"capture\",\"sql\":\"SELECT * FROM captured_rows WHERE work = :work\",\"parameters\":{\"work\":\"W\"},\"mappingVersion\":\"v1\"},\"snapshot\":{\"id\":\"revision-W\",\"isolation\":\"REPEATABLE_READ\",\"capturedAt\":\"2026-10-07T00:00:00Z\",\"artifactRef\":\""+artifact+"\"},\"rawRows\":{},\"data\":{\"effects\":0}}");
    }
    @Test void observerMustMatchRequestedScopeSnapshotAndActualProvenance() throws Exception {
        ObjectNode a=(ObjectNode)action("observe","observe");a.set("observation",Json.parse("{\"scope\":{\"work\":\"W\"},\"snapshotRef\":\"revision-W\",\"asOf\":\"2026-10-07T00:00:00Z\",\"knownAt\":\"2026-10-07T00:00:00Z\",\"sources\":[\"effects\"]}"));
        Path file=caseFile(List.of(a),List.of(assertion("zero","observe","/data/data/effects",Json.MAPPER.valueToTree(0))));
        assertEquals("PASS",runner(file,Map.of("observe",captured("observe",observation(),null,true))).run(false));
        ObjectNode wrongScope=observation();wrongScope.set("scope",Json.parse("{\"work\":\"W2\"}"));
        assertTrue(assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("observe",captured("observe",wrongScope,null,true))).run(false)).getMessage().contains("scope"));
        ObjectNode wrongSnapshot=observation();wrongSnapshot.put("snapshotRevision","revision-W2");
        assertTrue(assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("observe",captured("observe",wrongSnapshot,null,true))).run(false)).getMessage().contains("snapshotRevision"));
        ObjectNode wrongToken=observation();((ObjectNode)wrongToken.path("snapshot")).put("id","other-actual-token");
        assertTrue(assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("observe",captured("observe",wrongToken,null,true))).run(false)).getMessage().contains("actual snapshot"));
        StepResult mismatch=captured("observe",observation(),null,true);((ObjectNode)mismatch.provenance()).set("snapshot",Json.parse("{\"id\":\"OTHER\"}"));
        assertTrue(assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("observe",mismatch)).run(false)).getMessage().contains("provenance"));
    }
    @Test void barrierRequiresEveryRequestedIdentityAndStateRatherThanFieldPresence() throws Exception {
        JsonNode params=Json.parse("{\"barrierId\":\"b1\",\"participantId\":\"dispatch\",\"transactionId\":\"tx1\",\"point\":\"before-commit\",\"state\":\"REACHED\"}");
        ObjectNode a=(ObjectNode)action("barrier","control");ObjectNode control=Json.object();control.put("type","barrier").put("operation","waitReached").set("parameters",params);a.set("control",control);
        ObjectNode ack=(ObjectNode)params.deepCopy();ack.put("acknowledged",true).put("controlType","barrier").put("operation","waitReached").put("acknowledgedAt","2026-10-07T00:00:00Z");
        Path file=caseFile(List.of(a),List.of(assertion("ack","barrier","/data/acknowledged",Json.MAPPER.valueToTree(true))));
        assertEquals("PASS",runner(file,Map.of("barrier",captured("barrier",ack,null,false))).run(false));
        for(String field:List.of("barrierId","participantId","transactionId","point","state")) {
            ObjectNode mutant=ack.deepCopy();mutant.put(field,"OTHER");
            assertTrue(assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("barrier",captured("barrier",mutant,null,false))).run(false)).getMessage().contains(field));
        }
    }
    @Test void confirmedFailureSurvivesAnotherUnavailableAssertionSource() throws Exception {
        Path file=caseFile(List.of(invocation("executed","invoke"),invocation("missing","invoke")),List.of(assertion("wrong","executed","/response/effects",Json.MAPPER.valueToTree(0)),assertion("unavailable","missing","/response/effects",Json.MAPPER.valueToTree(0))));
        CaseRunner r=runner(file,Map.of("executed",captured("executed",Json.object(),Json.parse("{\"effects\":1}"),false)));
        assertEquals("FAIL",r.run(false));JsonNode evidence=r.evidence("FAIL","SELFTEST");assertEquals("FAIL",evidence.path("assertions").get(0).path("status").asText());assertEquals("NOT_RUN",evidence.path("assertions").get(1).path("status").asText());
        assertFalse(evidence.path("runtimeComplete").asBoolean());
    }
    private ObjectNode start() {ObjectNode a=(ObjectNode)action("started","start");a.set("call",invocation("call","invoke"));return a;}
    private ObjectNode awaitAction() {ObjectNode a=(ObjectNode)action("awaited","await");a.put("awaitActionId","started");return a;}
    @Test void everyStartIncludingParallelRequiresDeclaredTerminalAwait() throws Exception {
        Path file=caseFile(List.of(start()),List.of(assertion("not-terminal","started","/data/invocationHandle",Json.MAPPER.valueToTree("handle-1"))));
        assertTrue(assertThrows(IllegalArgumentException.class,()->new ContractValidator(root).caseFile(file)).getMessage().contains("Every start"));
        ObjectNode parallel=(ObjectNode)action("parallel","parallel");parallel.set("branches",Json.parse("[{\"id\":\"left\",\"actions\":[]},{\"id\":\"right\",\"actions\":[]}]"));
        ((com.fasterxml.jackson.databind.node.ArrayNode)parallel.path("branches").get(0).path("actions")).add(start());
        ((com.fasterxml.jackson.databind.node.ArrayNode)parallel.path("branches").get(1).path("actions")).add(invocation("read","query"));
        Path p=caseFile(List.of(parallel),List.of(assertion("not-terminal","started","/data/invocationHandle",Json.MAPPER.valueToTree("handle-1"))));
        assertTrue(assertThrows(IllegalArgumentException.class,()->new ContractValidator(root).caseFile(p)).getMessage().contains("Every start"));
    }
    @Test void terminalAwaitRequiresSameHandleAndStatusAndMissingAwaitCannotPassZeroEffect() throws Exception {
        Path file=caseFile(List.of(start(),awaitAction(),invocation("db","query")),List.of(assertion("zero","db","/response/effects",Json.MAPPER.valueToTree(0))));
        StepResult start=captured("started",Json.parse("{\"invocationHandle\":\"handle-1\"}"),null,false);
        StepResult zero=captured("db",Json.object(),Json.parse("{\"effects\":0}"),false);
        StepResult complete=captured("awaited",Json.parse("{\"invocationHandle\":\"handle-1\",\"completed\":true,\"terminalStatus\":\"SUCCEEDED\"}"),null,false);
        assertEquals("PASS",runner(file,Map.of("started",start,"awaited",complete,"db",zero)).run(false));
        assertEquals("NOT_RUN",runner(file,Map.of("started",start,"db",zero)).run(false));
        for(JsonNode invalid:List.of(Json.parse("{\"invocationHandle\":\"OTHER\",\"completed\":true,\"terminalStatus\":\"SUCCEEDED\"}"),Json.parse("{\"invocationHandle\":\"handle-1\",\"completed\":true}")))
            assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("started",start,"awaited",captured("awaited",invalid,null,false),"db",zero)).run(false));
    }
}
