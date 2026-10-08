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
    private CaseRunner runner(Path file,Map<String,StepResult> captures) throws Exception { return CaseRunner.harnessSelftest(new ContractValidator(root),new Captures(captures),new AgentRunner.Scripted(),file,subcase); }
    private static class Captures implements AcceptanceDriver {
        private final Map<String,StepResult> captures; Captures(Map<String,StepResult> captures){this.captures=captures;}
        private StepResult get(String id){return captures.getOrDefault(id,StepResult.missing(id,"NOT_IMPLEMENTED: selftest capture absent"));}
        public Set<String> availableAdapters(){return Set.of("CAPTURED_CONTRACT_SELFTEST_ONLY");}
        public StepResult installFixture(String id,JsonNode f){return get(id);} public StepResult invoke(String id,String r,JsonNode a,String c,JsonNode q){return get(id);}
        public StepResult query(String id,String r,JsonNode a,String c,JsonNode q){return get(id);} public StepResult observe(String id,JsonNode q){return get(id);}
        public StepResult control(String id,JsonNode q){return get(id);} public StepResult start(String id,String r,JsonNode a,String c,JsonNode q){return get(id);}
        public StepResult await(String id,JsonNode h,int t){return get(id);}
    }
    @Test void protocolOperationPreservesHostileRawRequestRatherThanRepairingIt() throws Exception {
        ObjectNode a=invocation("wire","query");a.remove("capabilityId");a.put("route","wire").put("protocolOperation","server/discover");
        JsonNode raw=Json.parse("{\"headers\":{\"Content-Type\":\"text/plain\",\"MCP-Protocol-Version\":\"invalid\"},\"body\":\"{\\\"method\\\":\\\"different/method\\\"}\"}");a.set("request",raw);
        Path file=caseFile(List.of(a),List.of(assertion("denied","wire","/response/status",Json.MAPPER.valueToTree(415))));
        var capturedRequest=new java.util.concurrent.atomic.AtomicReference<JsonNode>();var capturedOperation=new java.util.concurrent.atomic.AtomicReference<String>();
        var d=new Captures(Map.of()) {public StepResult wire(String id,JsonNode actor,String operation,JsonNode request) {capturedRequest.set(request);capturedOperation.set(operation);return captured(id,Json.object(),Json.parse("{\"status\":415}"),false);}};
        assertEquals("PASS",CaseRunner.harnessSelftest(new ContractValidator(root),d,new AgentRunner.Scripted(),file,subcase).run(false));
        assertEquals(raw,capturedRequest.get());assertEquals("server/discover",capturedOperation.get());
        a.put("route","api");Path invalid=caseFile(List.of(a),List.of(assertion("denied","wire","/response/status",Json.MAPPER.valueToTree(415))));
        assertThrows(IllegalArgumentException.class,()->new ContractValidator(root).caseFile(invalid));
    }
    private static final String QUERY_SQL="SELECT * FROM captured_rows WHERE work = :work";
    /** Observer result for a $result-bound snapshotRef: recomputed projection revision, own MVCC token, derived data. */
    private ObjectNode observation() {
        return (ObjectNode)Json.parse("{\"snapshotRevision\":\"revision-W\",\"asOf\":\"2026-10-07T00:00:00Z\",\"knownAt\":\"2026-10-07T00:00:00Z\",\"scope\":{\"work\":\"W\"},\"scopeComplete\":true,\"sourceQuery\":{\"statementId\":\"capture\",\"sql\":\""+QUERY_SQL+"\",\"parameters\":{\"work\":\"W\"},\"mappingVersion\":\"v1\"},\"snapshot\":{\"id\":\"811:811:\",\"isolation\":\"REPEATABLE_READ\",\"capturedAt\":\"2026-10-07T00:00:00Z\",\"artifactRef\":\""+artifact+"\",\"readMode\":\"RESULT_REVISION\",\"revisionQuery\":{\"statementId\":\"projection-revision\",\"sql\":\"SELECT shared_world_hash(:work)\",\"parameters\":{\"work\":\"W\"},\"mappingVersion\":\"v1\"}},\"rawRows\":{\"effects\":[]},\"sourceEvidence\":{\"effects\":{\"complete\":true,\"rowPointer\":\"/rawRows/effects\",\"sourceQuery\":{\"statementId\":\"capture-effects\",\"sql\":\"SELECT * FROM captured_effects WHERE work = :work\",\"parameters\":{\"work\":\"W\"},\"mappingVersion\":\"v1\"},\"artifactRef\":\""+artifact+"\"}},\"data\":{\"effects\":0},\"derivations\":{\"effects\":{\"rowPointer\":\"/rawRows/effects\",\"aggregate\":\"count\"}}}");
    }
    private ObjectNode read() {return invocation("read","query");}
    private StepResult readResult() {return captured("read",Json.object(),Json.parse("{\"snapshotRevision\":\"revision-W\"}"),false);}
    private ObjectNode observeAction(String id,JsonNode snapshotRef,String sources) {
        ObjectNode a=(ObjectNode)action(id,"observe");ObjectNode o=(ObjectNode)Json.parse("{\"scope\":{\"work\":\"W\"},\"asOf\":\"2026-10-07T00:00:00Z\",\"knownAt\":\"2026-10-07T00:00:00Z\",\"sources\":"+sources+"}");
        o.set("snapshotRef",snapshotRef);a.set("observation",o);return a;
    }
    private static final JsonNode RESULT_REF=Json.parse("{\"$result\":{\"actionId\":\"read\",\"pointer\":\"/response/snapshotRevision\"}}");
    private String rejection(Path file,ObjectNode data) {
        return assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("read",readResult(),"observe",captured("observe",data,null,true))).run(false)).getMessage();
    }
    @Test void observerMustMatchRequestedScopeSnapshotAndActualProvenance() throws Exception {
        Path file=caseFile(List.of(read(),observeAction("observe",RESULT_REF,"[\"effects\"]")),List.of(assertion("zero","observe","/data/data/effects",Json.MAPPER.valueToTree(0))));
        assertEquals("PASS",runner(file,Map.of("read",readResult(),"observe",captured("observe",observation(),null,true))).run(false));
        ObjectNode wrongScope=observation();wrongScope.set("scope",Json.parse("{\"work\":\"W2\"}"));assertTrue(rejection(file,wrongScope).contains("scope"));
        ObjectNode wrongSnapshot=observation();wrongSnapshot.put("snapshotRevision","revision-W2");assertTrue(rejection(file,wrongSnapshot).contains("snapshotRevision"));
        // An observer that copies the API revision into its own snapshot id is an echo, not an independent snapshot.
        ObjectNode echo=observation();((ObjectNode)echo.path("snapshot")).put("id","revision-W");assertTrue(rejection(file,echo).contains("echoes"));
        ObjectNode noMode=observation();((ObjectNode)noMode.path("snapshot")).remove("readMode");assertTrue(rejection(file,noMode).contains("RESULT_REVISION"));
        ObjectNode noQuery=observation();((ObjectNode)noQuery.path("snapshot")).remove("revisionQuery");assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("read",readResult(),"observe",captured("observe",noQuery,null,true))).run(false));
        StepResult mismatch=captured("observe",observation(),null,true);((ObjectNode)mismatch.provenance()).set("snapshot",Json.parse("{\"id\":\"OTHER\"}"));
        assertTrue(assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("read",readResult(),"observe",mismatch)).run(false)).getMessage().contains("provenance"));
    }
    /** step2-closure P3: the observer receives the issuing request identity, never the issued revision, so echo is detectable. */
    @Test void resultRevisionObserverGetsIssuingRequestIdentityNotTheRevision() throws Exception {
        Path file=caseFile(List.of(read(),observeAction("observe",RESULT_REF,"[\"effects\"]")),List.of(assertion("zero","observe","/data/data/effects",Json.MAPPER.valueToTree(0))));
        var seen=new java.util.concurrent.atomic.AtomicReference<JsonNode>();
        // An echoing observer copies whatever snapshotRef it was handed into snapshotRevision.
        var echo=new Captures(Map.of("read",readResult())) {public StepResult observe(String id,JsonNode request) {
            seen.set(request);ObjectNode data=observation();data.set("snapshotRevision",request.path("snapshotRef"));return captured(id,data,null,true);}};
        String reason=assertThrows(IllegalArgumentException.class,()->CaseRunner.harnessSelftest(new ContractValidator(root),echo,new AgentRunner.Scripted(),file,subcase).run(false)).getMessage();
        assertTrue(reason.contains("snapshotRevision"),reason);
        JsonNode request=seen.get();
        assertEquals(CaseRunner.RESULT_REVISION,request.path("snapshotRef").asText());
        assertFalse(request.toString().contains("revision-W"),"the issued revision must not reach the observer: "+request);
        JsonNode source=request.path("snapshotSource");
        assertEquals("read",source.path("actionId").asText());assertEquals("/response/snapshotRevision",source.path("pointer").asText());
        assertEquals("qc",source.path("actorRef").asText());assertEquals("placeHold",source.path("capabilityId").asText());
        assertTrue(source.path("actor").isObject() && source.path("request").isObject(),source.toString());
        // A recomputing observer (here: the captured recomputed value) still passes.
        assertEquals("PASS",runner(file,Map.of("read",readResult(),"observe",captured("observe",observation(),null,true))).run(false));
    }
    @Test void directiveSnapshotUsesObserverTokenAndOrderedCaptureNotEcho() throws Exception {
        JsonNode directive=Json.MAPPER.valueToTree("CURRENT_COMMITTED");
        Path file=caseFile(List.of(observeAction("first",directive,"[\"effects\"]"),observeAction("observe",directive,"[\"effects\"]")),List.of(assertion("zero","observe","/data/data/effects",Json.MAPPER.valueToTree(0))));
        ObjectNode fresh=observation();fresh.put("snapshotRevision","observer-world-17");ObjectNode snap=(ObjectNode)fresh.path("snapshot");snap.put("readMode","CURRENT_COMMITTED");snap.remove("revisionQuery");
        ObjectNode later=fresh.deepCopy();((ObjectNode)later.path("snapshot")).put("capturedAt","2026-10-07T00:00:01Z").put("id","812:812:");
        assertEquals("PASS",runner(file,Map.of("first",captured("first",fresh,null,true),"observe",captured("observe",later,null,true))).run(false));
        ObjectNode earlier=later.deepCopy();((ObjectNode)earlier.path("snapshot")).put("capturedAt","2026-10-06T23:59:59Z");
        assertTrue(assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("first",captured("first",fresh,null,true),"observe",captured("observe",earlier,null,true))).run(false)).getMessage().contains("captured before"));
        for(String field:List.of("id","readMode")) {
            ObjectNode bad=later.deepCopy();((ObjectNode)bad.path("snapshot")).put(field,field.equals("id")?"CURRENT_COMMITTED":"CURRENT_LOCK_WAIT");
            assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("first",captured("first",fresh,null,true),"observe",captured("observe",bad,null,true))).run(false));
        }
        ObjectNode revisionEcho=later.deepCopy();revisionEcho.put("snapshotRevision","CURRENT_COMMITTED");
        assertTrue(assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("first",captured("first",fresh,null,true),"observe",captured("observe",revisionEcho,null,true))).run(false)).getMessage().contains("directive"));
        Path literal=caseFile(List.of(observeAction("observe",Json.MAPPER.valueToTree("revision-W"),"[\"effects\"]")),List.of(assertion("zero","observe","/data/data/effects",Json.MAPPER.valueToTree(0))));
        assertThrows(IllegalArgumentException.class,()->new ContractValidator(root).caseFile(literal));
    }
    @Test void observerDataMustBeRecomputableFromRawRows() throws Exception {
        Path file=caseFile(List.of(read(),observeAction("observe",RESULT_REF,"[\"effects\"]")),List.of(assertion("held","observe","/data/data/held",Json.MAPPER.valueToTree("20"))));
        ObjectNode data=observation();((ObjectNode)data.path("rawRows")).set("effects",Json.parse("[{\"kind\":\"HOLD\",\"quantity\":\"12\",\"unit\":\"BOX\"},{\"kind\":\"HOLD\",\"quantity\":\"8\",\"unit\":\"BOX\"},{\"kind\":\"MOVE\",\"quantity\":\"5\",\"unit\":\"BOX\"}]"));
        data.set("data",Json.parse("{\"held\":\"20\"}"));data.set("derivations",Json.parse("{\"held\":{\"rowPointer\":\"/rawRows/effects\",\"where\":{\"kind\":\"HOLD\"},\"aggregate\":\"sum\",\"field\":\"quantity\",\"unitField\":\"unit\"}}"));
        assertEquals("PASS",runner(file,Map.of("read",readResult(),"observe",captured("observe",data,null,true))).run(false));
        ObjectNode invented=data.deepCopy();invented.set("data",Json.parse("{\"held\":\"20\",\"extra\":\"80\"}"));assertTrue(rejection(file,invented).contains("no raw-row derivation"));
        ObjectNode copied=data.deepCopy();copied.set("data",Json.parse("{\"held\":\"25\"}"));assertTrue(rejection(file,copied).contains("recomputation"));
        ObjectNode undeclared=data.deepCopy();undeclared.remove("derivations");assertTrue(rejection(file,undeclared).contains("no raw-row derivation"));
        ObjectNode mixed=data.deepCopy();((ObjectNode)mixed.at("/rawRows/effects/1")).put("unit","KG");assertTrue(rejection(file,mixed).contains("different units"));
        ObjectNode foreign=data.deepCopy();((ObjectNode)foreign.at("/derivations/held")).put("rowPointer","/rawRows/other");assertTrue(rejection(file,foreign).contains("requested raw source"));
    }
    @Test void everyRequestedRawSourceNeedsRowsAndIndependentQueryEvidence() throws Exception {
        Path file=caseFile(List.of(read(),observeAction("observe",RESULT_REF,"[\"movements\",\"allocations\"]")),List.of(assertion("zero","observe","/data/rawRows/movements",Json.MAPPER.valueToTree(List.of()))));
        ObjectNode data=observation();ObjectNode rows=Json.object(),evidence=Json.object();
        for(String source:List.of("movements","allocations")) {rows.set(source,Json.array());ObjectNode e=(ObjectNode)data.path("sourceEvidence").path("effects").deepCopy();e.put("rowPointer","/rawRows/"+source);evidence.set(source,e);}
        data.set("rawRows",rows);data.set("sourceEvidence",evidence);data.set("data",Json.object());data.remove("derivations");
        assertEquals("PASS",runner(file,Map.of("read",readResult(),"observe",captured("observe",data,null,true))).run(false));
        rows.remove("allocations");assertTrue(rejection(file,data).contains("allocations"));
        rows.set("allocations",Json.array());evidence.remove("allocations");assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("read",readResult(),"observe",captured("observe",data,null,true))).run(false));
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
    @Test void processWiringRejectsBareAckAndPreservesUnavailableStatus() throws Exception {
        ObjectNode a=(ObjectNode)action("process","control");a.set("control",Json.parse("{\"type\":\"process\",\"operation\":\"restart\",\"parameters\":{\"processId\":\"synthetic-worker\"}}"));
        Path file=caseFile(List.of(a),List.of(assertion("ack","process","/data/acknowledged",Json.MAPPER.valueToTree(true))));
        ObjectNode ack=Json.object();ack.put("acknowledged",true).put("controlType","process").put("operation","restart").put("acknowledgedAt","2026-10-07T00:00:00Z");
        assertTrue(assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("process",captured("process",ack,null,false))).run(false)).getMessage().contains("hostObservation"));
        ack.set("hostObservation",Json.object());assertThrows(IllegalArgumentException.class,()->runner(file,Map.of("process",captured("process",ack,null,false))).run(false));
        CaseRunner missing=runner(file,Map.of());assertEquals("NOT_RUN",missing.run(false));assertEquals("NOT_IMPLEMENTED",missing.results().get("process").path("driverStatus").asText());assertTrue(missing.results().get("process").path("data").isNull());
        assertThrows(AssertionError.class,missing::verifyComplete);
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
