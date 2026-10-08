package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/** Step 2 closure review 2 (step2r round 5): counterexamples for the confirmed findings. Captured selftest ports only. */
final class StepTwoRoundFiveRegressionTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private static final String DIR="verification/harness/src/test/resources/host-observation/";
    private static final String CANNED="verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/canned-observations.json";
    private static final String SNAPSHOT=DIR+"runtime-snapshot.json";
    private static final List<String> CASES=new ArrayList<>();
    static {
        for(int i=1;i<=26;i++) CASES.add(String.format("T%02d",i));
        for(int i=1;i<=5;i++) CASES.add("C"+i);
        for(int i=1;i<=8;i++) CASES.add("V"+i);
        CASES.add("E1");CASES.add("E2");
    }
    private JsonNode caseJson(String id) throws Exception {return Json.read(root.resolve("verification/cases/"+id+"/case.json"));}
    private static ObjectNode sub(JsonNode c,String id) {for(JsonNode s:c.path("subcases")) if(s.path("id").asText().equals(id)) return (ObjectNode)s;throw new AssertionError(id);}
    private static ObjectNode only(JsonNode c,ObjectNode s) {ObjectNode copy=((ObjectNode)c).deepCopy();copy.set("subcases",Json.array().add(s));return copy;}
    private static JsonNode ref(String action,String pointer) {return Json.parse("{\"$result\":{\"actionId\":\""+action+"\",\"pointer\":\""+pointer+"\"}}");}

    private Path caseFile(List<JsonNode> actions,List<JsonNode> assertions) throws Exception {
        JsonNode c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
        ObjectNode s=(ObjectNode)c.path("subcases").get(0);s.set("actions",Json.MAPPER.valueToTree(actions));s.set("assertions",Json.MAPPER.valueToTree(assertions));
        Path p=Files.createTempFile(root.resolve("verification/harness/target"),"round5-",".json");Json.write(p,c);return p;
    }
    private JsonNode assertion(String id,String action,String pointer,JsonNode expected) throws Exception {
        ObjectNode a=(ObjectNode)Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json")).path("subcases").get(0).path("assertions").get(0).deepCopy();
        a.put("id",id).put("op","equals");a.remove(List.of("unit","unitSource"));a.set("source",Json.parse("{\"actionId\":\""+action+"\",\"pointer\":\""+pointer+"\"}"));a.set("expected",expected);return a;
    }
    private static ObjectNode control(String id,JsonNode control) {ObjectNode a=Json.object();a.put("id",id).put("kind","control");a.set("control",control);a.set("evidenceRefs",Json.parse("[\"CAPTURED_SELFTEST\"]"));return a;}
    private static StepResult as(String id,StepResult r) {return new StepResult(id,r.driverStatus(),r.data(),r.response(),r.reason(),r.provenance(),r.artifactRefs());}
    private record Port(Map<String,StepResult> captures,java.util.function.Function<JsonNode,StepResult> observer) implements AcceptanceDriver {
        private StepResult get(String id){return captures.getOrDefault(id,StepResult.missing(id,"NOT_IMPLEMENTED: selftest capture absent"));}
        public Set<String> availableAdapters(){return Set.of("CAPTURED_CONTRACT_SELFTEST_ONLY");}
        public StepResult installFixture(String id,JsonNode f){return get(id);} public StepResult invoke(String id,String r,JsonNode a,String c,JsonNode q){return get(id);}
        public StepResult query(String id,String r,JsonNode a,String c,JsonNode q){return get(id);} public StepResult observe(String id,JsonNode q){return observer.apply(q);}
        public StepResult control(String id,JsonNode q){return get(id);} public StepResult start(String id,String r,JsonNode a,String c,JsonNode q){return get(id);}
        public StepResult await(String id,JsonNode h,int t){return get(id);}
    }

    // Item 1 (opus[0]): a DB observe bound to the host terminal snapshot of an awaitRuntimeTask is satisfiable and bound.
    private static final String QUERY="{\"statementId\":\"capture\",\"sql\":\"SELECT * FROM captured_rows WHERE work = :work\",\"parameters\":{\"work\":\"W\"},\"mappingVersion\":\"v1\"}";
    private ObjectNode runtimeObservation() throws Exception {
        ObjectNode d=(ObjectNode)Json.parse("{\"snapshotRevision\":\"observer-world-3\",\"asOf\":\"2026-10-07T00:00:00Z\",\"knownAt\":\"2026-10-07T00:00:00Z\",\"scope\":{\"workspace\":\"host-selftest\"},\"scopeComplete\":true,"
            +"\"sourceQuery\":"+QUERY+",\"snapshot\":{\"id\":\"901:901:\",\"isolation\":\"REPEATABLE_READ\",\"capturedAt\":\"2026-10-07T00:00:05Z\",\"artifactRef\":\""+CANNED+"\",\"readMode\":\"RUNTIME_TASK_SNAPSHOT\"},"
            +"\"rawRows\":{\"effects\":[]},\"sourceEvidence\":{\"effects\":{\"complete\":true,\"rowPointer\":\"/rawRows/effects\",\"sourceQuery\":"+QUERY+",\"artifactRef\":\""+CANNED+"\"}},\"data\":{}}");
        ObjectNode bound=Json.object();bound.put("schedulerId","synthetic-schedulerId").put("taskId","synthetic-task-1").put("invocationHandle","synthetic-handle-1").put("snapshotId","synthetic-snapshot-1")
            .put("artifactRef",SNAPSHOT).put("sha256",Json.sha256(root.resolve(SNAPSHOT)));
        ((ObjectNode)d.path("snapshot")).set("runtimeTaskSnapshot",bound);return d;
    }
    private StepResult observed(ObjectNode data,List<String> refs) {
        ObjectNode p=Json.object();p.put("adapter","captured-contract-selftest").put("adapterVersion","1.0").put("buildVersion","SELFTEST").putNull("authenticatedActor").put("source","CANNED_CONTRACT_SELFTEST").put("independent",true).put("scopeComplete",true);
        p.set("sourceQuery",data.path("sourceQuery"));p.set("snapshot",data.path("snapshot"));
        return new StepResult("sweep-db",StepResult.DriverStatus.EXECUTED,data,null,null,p,refs);
    }
    private Path runtimeCase(HostObservationValidatorTest.Capture tick,HostObservationValidatorTest.Capture await) throws Exception {
        ObjectNode awaitControl=await.control().deepCopy();
        ((ObjectNode)awaitControl.path("parameters")).set("taskId",ref("tick","/data/hostObservation/operationEvidence/taskId"));
        ((ObjectNode)awaitControl.path("parameters")).set("invocationHandle",ref("tick","/data/hostObservation/operationEvidence/invocationHandle"));
        ObjectNode observe=Json.object();observe.put("id","sweep-db").put("kind","observe");observe.set("evidenceRefs",Json.parse("[\"CAPTURED_SELFTEST\"]"));
        ObjectNode o=(ObjectNode)Json.parse("{\"scope\":{\"workspace\":\"host-selftest\"},\"asOf\":\"2026-10-07T00:00:00Z\",\"knownAt\":\"2026-10-07T00:00:00Z\",\"sources\":[\"effects\"]}");
        o.set("snapshotRef",ref("terminal",CaseRunner.RUNTIME_SNAPSHOT_POINTER));observe.set("observation",o);
        return caseFile(List.of(control("tick",tick.control()),control("terminal",awaitControl),observe),List.of(assertion("effects","sweep-db","/data/rawRows/effects",Json.array())));
    }
    @Test void runtimeTaskSnapshotObserveIsSatisfiableAndBoundToTheHostArtifact() throws Exception {
        HostObservationValidatorTest captures=new HostObservationValidatorTest();
        HostObservationValidatorTest.Capture tick=captures.capture("tickScheduler","tickScheduler-submitted-rows.json"),await=captures.capture("awaitRuntimeTask");
        Path file=runtimeCase(tick,await);
        Map<String,StepResult> ports=Map.of("tick",as("tick",tick.result()),"terminal",as("terminal",await.result()));
        List<String> refs=List.of(CANNED,SNAPSHOT);
        AtomicReference<JsonNode> seen=new AtomicReference<>();
        Port honest=new Port(ports,q->{seen.set(q);try{return observed(runtimeObservation(),refs);}catch(Exception e){throw new IllegalStateException(e);}});
        assertEquals("PASS",CaseRunner.harnessSelftest(new ContractValidator(root),honest,new AgentRunner.Scripted(),file,"hold-preserves-physical").run(false));
        JsonNode request=seen.get();
        assertEquals(CaseRunner.RUNTIME_TASK_SNAPSHOT,request.path("snapshotRef").asText());
        JsonNode source=request.path("snapshotSource");
        assertEquals("awaitRuntimeTask",source.path("operation").asText());
        assertEquals("synthetic-schedulerId",source.path("schedulerId").asText());
        assertEquals("synthetic-task-1",source.path("taskId").asText());assertEquals("synthetic-handle-1",source.path("invocationHandle").asText());
        assertFalse(request.toString().contains("synthetic-snapshot-1"),"the host snapshot id itself is withheld from the observer: "+request);
        Map<String,String> reasons=Map.of("snapshotId","snapshotId differs","sha256","sha256 differs","artifact","artifact differs","taskId","taskId differs","schedulerId","different scheduler",
            "captured before terminal","before the awaited","echoed snapshot id","echoes","API read mode","RUNTIME_TASK_SNAPSHOT","unbound","Schema invalid");
        Map<String,java.util.function.Consumer<ObjectNode>> mutants=new LinkedHashMap<>();
        mutants.put("snapshotId",d->((ObjectNode)d.at("/snapshot/runtimeTaskSnapshot")).put("snapshotId","synthetic-snapshot-2"));
        mutants.put("sha256",d->((ObjectNode)d.at("/snapshot/runtimeTaskSnapshot")).put("sha256","0".repeat(64)));
        mutants.put("artifact",d->((ObjectNode)d.at("/snapshot/runtimeTaskSnapshot")).put("artifactRef",CANNED));
        mutants.put("taskId",d->((ObjectNode)d.at("/snapshot/runtimeTaskSnapshot")).put("taskId","synthetic-task-2"));
        mutants.put("schedulerId",d->((ObjectNode)d.at("/snapshot/runtimeTaskSnapshot")).put("schedulerId","other-scheduler"));
        mutants.put("captured before terminal",d->((ObjectNode)d.path("snapshot")).put("capturedAt","2026-10-07T00:00:00.900Z"));
        mutants.put("echoed snapshot id",d->((ObjectNode)d.path("snapshot")).put("id","synthetic-snapshot-1"));
        mutants.put("API read mode",d->{((ObjectNode)d.path("snapshot")).put("readMode","RESULT_REVISION");((ObjectNode)d.path("snapshot")).remove("runtimeTaskSnapshot");
            ((ObjectNode)d.path("snapshot")).set("revisionQuery",Json.parse(QUERY));d.put("snapshotRevision","synthetic-snapshot-1");});
        mutants.put("unbound",d->((ObjectNode)d.path("snapshot")).remove("runtimeTaskSnapshot"));
        for(var m:mutants.entrySet()) {
            Port bad=new Port(ports,q->{try{ObjectNode d=runtimeObservation();m.getValue().accept(d);return observed(d,refs);}catch(Exception e){throw new IllegalStateException(e);}});
            String reason=assertThrows(IllegalArgumentException.class,()->CaseRunner.harnessSelftest(new ContractValidator(root),bad,new AgentRunner.Scripted(),file,"hold-preserves-physical").run(false),m.getKey()).getMessage();
            assertTrue(reason.contains(reasons.get(m.getKey())),m.getKey()+": "+reason);
        }
        Port unlinked=new Port(ports,q->{try{return observed(runtimeObservation(),List.of(CANNED));}catch(Exception e){throw new IllegalStateException(e);}});
        assertTrue(assertThrows(IllegalArgumentException.class,()->CaseRunner.harnessSelftest(new ContractValidator(root),unlinked,new AgentRunner.Scripted(),file,"hold-preserves-physical").run(false)).getMessage().contains("not linked"));
    }
    @Test void everyT26RuntimeSnapshotObserveUsesTheRuntimeReadModeAndNoOtherPointerIsAccepted() throws Exception {
        ContractValidator v=new ContractValidator(root);
        for(String id:CASES) assertEquals(List.of(),v.snapshotRefProblems(caseJson(id)),id);
        JsonNode t26=caseJson("T26");int runtime=0;
        for(JsonNode s:t26.path("subcases")) for(JsonNode a:s.path("actions"))
            if(a.path("observation").path("snapshotRef").path("$result").path("pointer").asText().equals(CaseRunner.RUNTIME_SNAPSHOT_POINTER)) runtime++;
        assertEquals(5,runtime,"four harness-tick expiry sweep-db observes and the autonomous lot-expiry sweep-db");
        for(String[] bad:new String[][]{{"sweep-terminal","/data/hostObservation/runtimeTask/snapshot/capturedAt"},{"sweep-terminal","/response/snapshotRevision"},{"before","/response/revision"},{"sweep","/data/hostObservation/runtimeTask/snapshot/id"}}) {
            ObjectNode s=sub(t26,"lot-expiry-no-event").deepCopy();
            for(JsonNode a:s.path("actions")) if(a.path("id").asText().equals("sweep-db")) ((ObjectNode)a.path("observation")).set("snapshotRef",ref(bad[0],bad[1]));
            assertEquals(1,v.snapshotRefProblems(only(t26,s)).size(),Arrays.toString(bad));
        }
    }

    // Item 2 (opus[1]): a loop process may not run through the fault/seed phase of an autonomous subcase.
    @Test void autonomousLoopProcessesStayStoppedUntilTheWatcherGroup() throws Exception {
        ContractValidator v=new ContractValidator(root);
        for(String id:CASES) assertEquals(List.of(),v.runtimeProfileProblems(caseJson(id)),id);
        JsonNode t26=caseJson("T26");
        for(String id:List.of("orphan-intake-autonomous-loop","due-wait-autonomous-loop","lot-expiry-autonomous-loop")) {
            ObjectNode s=sub(t26,id).deepCopy();ArrayNode acts=(ArrayNode)s.path("actions");
            // The round 4 shape: the scheduler starts with the application and is stopped only before the clock advance.
            JsonNode start=null;for(JsonNode a:acts) if(a.path("kind").asText().equals("parallel")) start=a.path("branches").get(1).path("actions").get(0);
            ObjectNode early=start.deepCopy();early.put("id","start-app-loop");ObjectNode stop=start.deepCopy();stop.put("id","stop-loop-before-advance");((ObjectNode)stop.path("control")).put("operation","stop");
            int advance=0;for(int i=0;i<acts.size();i++) if(acts.get(i).path("id").asText().equals("advance")) advance=i;
            acts.insert(advance,stop);acts.insert(2,early);
            List<String> problems=v.runtimeProfileProblems(only(t26,s));
            assertTrue(problems.stream().anyMatch(p->p.contains("running during the fault/seed phase")),id+": "+problems);
            assertFalse(problems.stream().anyMatch(p->p.contains("already running")),"the old rule alone accepted this shape: "+problems);
        }
        // api/workers serialized in the watcher group (opus[2]) are rejected: one loop start per branch.
        ObjectNode serialized=sub(t26,"orphan-intake-autonomous-loop").deepCopy();
        for(JsonNode a:serialized.path("actions")) if(a.path("kind").asText().equals("parallel")) {
            ArrayNode branch=(ArrayNode)a.path("branches").get(1).path("actions");ObjectNode api=branch.get(0).deepCopy();api.put("id","start-api-in-window");
            ((ObjectNode)api.path("control").path("parameters")).put("processId","api-2");branch.add(api);
        }
        assertTrue(v.runtimeProfileProblems(only(t26,serialized)).stream().anyMatch(p->p.contains("exactly one process")));
    }

    // astra[1]/opus[2]: the passive window is anchored at the pre-group harness boundary, not at the watcher thread's start.
    private HostObservationValidatorTest.Capture shifted(String commandStart,String commandEnd,String submittedAt) throws Exception {
        HostObservationValidatorTest.Capture c=new HostObservationValidatorTest().capture("tickScheduler","tickScheduler-natural-rows.json");
        ObjectNode rows=(ObjectNode)c.host().path("extractor").path("rawRows");
        ((ObjectNode)rows.path("operationEvidence")).put("submittedAt",submittedAt);((ObjectNode)rows.path("schedulerSubmissions").get(0)).put("submittedAt",submittedAt);
        ((ObjectNode)rows.path("command")).put("startedAt",commandStart).put("completedAt",commandEnd);
        c.host().set("command",rows.path("command").deepCopy()); // the watcher's own argv/interval
        c.host().set("operationEvidence",rows.path("operationEvidence").deepCopy());
        ObjectNode params=(ObjectNode)c.control().path("parameters");params.put("trigger","OBSERVE_NEXT_NATURAL_TICK").put("observationWindowSeconds",30).put("triggeredBy","SCHEDULER_LOOP");
        c.host().set("requestedInputs",params.deepCopy());
        Path copy=Files.createTempFile(root.resolve("verification/harness/target"),"round5-rows-",".json");Json.write(copy,rows);
        String refPath=root.relativize(copy).toString();((ObjectNode)c.host().path("extractor")).put("rawRowsArtifactRef",refPath);
        ObjectNode a=Json.object();a.put("path",refPath).put("sha256",Json.sha256(copy)).put("sizeBytes",Files.size(copy)).put("completeness","COMPLETE");a.set("scope",Json.parse("{\"workspace\":\"host-selftest\"}"));
        ((ArrayNode)c.host().path("extractor").path("inputArtifacts")).set(0,a);
        List<String> refs=new ArrayList<>(c.result().artifactRefs());refs.add(refPath);
        return new HostObservationValidatorTest.Capture(c.control(),c.host(),new StepResult(c.result().actionId(),c.result().driverStatus(),c.result().data(),null,c.result().reason(),c.result().provenance(),refs));
    }
    @Test void passiveWindowStartsAtThePreGroupBoundary() throws Exception {
        ContractValidator v=new ContractValidator(root);Instant boundary=Instant.parse("2026-10-07T00:00:00Z");
        // The watcher thread started late (00:00:02.5) but the loop submitted at 00:00:02 after the boundary: durable rows still count.
        HostObservationValidatorTest.Capture late=shifted("2026-10-07T00:00:02.500Z","2026-10-07T00:00:03Z","2026-10-07T00:00:02Z");
        HostObservationValidator.validate(v,late.control(),late.result(),false,boundary);
        assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(v,late.control(),late.result(),false,null),"without the boundary the late watcher loses the submission");
        // A watcher command cannot claim to have started before the group existed.
        HostObservationValidatorTest.Capture early=shifted("2026-10-07T00:00:01Z","2026-10-07T00:00:03Z","2026-10-07T00:00:02Z");
        assertTrue(assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(v,early.control(),early.result(),false,Instant.parse("2026-10-07T00:00:01.500Z"))).getMessage().contains("boundary"));
        // The window still ends 30 s after the boundary.
        HostObservationValidatorTest.Capture tooLate=shifted("2026-10-07T00:00:25Z","2026-10-07T00:00:32Z","2026-10-07T00:00:31Z");
        assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(v,tooLate.control(),tooLate.result(),false,boundary));
    }
    @Test void parallelWatcherGroupRecordsItsObservationBoundary() throws Exception {
        Instant now=Instant.now();
        String commandStart=now.plusSeconds(2).toString();
        HostObservationValidatorTest.Capture watcher=shifted(commandStart,now.plusSeconds(4).toString(),now.plusSeconds(3).toString());
        HostObservationValidatorTest.Capture start=new HostObservationValidatorTest().capture("start");
        ObjectNode group=Json.object();group.put("id","group").put("kind","parallel").put("timeoutSeconds",30);group.set("evidenceRefs",Json.parse("[\"CAPTURED_SELFTEST\"]"));
        ArrayNode branches=Json.array();
        branches.add(Json.object().put("id","watch").set("actions",Json.array().add(control("watch",watcher.control()))));
        branches.add(Json.object().put("id","loop").set("actions",Json.array().add(control("start-loop",start.control()))));
        group.set("branches",branches);
        Path file=caseFile(List.of(group),List.of(assertion("acked","group","/data/submissionAcknowledged",Json.MAPPER.valueToTree(true))));
        Port port=new Port(Map.of("watch",as("watch",watcher.result()),"start-loop",as("start-loop",start.result())),q->{throw new AssertionError("no observe");});
        CaseRunner runner=CaseRunner.harnessSelftest(new ContractValidator(root),port,new AgentRunner.Scripted(),file,"hold-preserves-physical");
        // The watcher's SUBMITTED task has no awaited terminal here, so the selftest stays NOT_RUN; the group itself executed.
        assertEquals("NOT_RUN",runner.run(false));
        assertEquals("EXECUTED",runner.results().get("group").path("driverStatus").asText());
        Instant recorded=Instant.parse(runner.results().get("group").path("data").path("observationBoundaryAt").asText());
        assertFalse(recorded.isBefore(now) || recorded.isAfter(Instant.parse(commandStart)),recorded.toString());
    }

    // astra[0]/[7]: applicableTargets is recomputed from the kind -> probe class policy, so a 0/0 complete=true row cannot hide work.
    private StepResult rebound(HostObservationValidatorTest.Capture c) throws Exception {
        ObjectNode rows=(ObjectNode)c.host().path("extractor").path("rawRows");
        Path copy=Files.createTempFile(root.resolve("verification/harness/target"),"round5-surface-",".json");Json.write(copy,rows);
        String refPath=root.relativize(copy).toString();((ObjectNode)c.host().path("extractor")).put("rawRowsArtifactRef",refPath);
        ObjectNode a=Json.object();a.put("path",refPath).put("sha256",Json.sha256(copy)).put("sizeBytes",Files.size(copy)).put("completeness","COMPLETE");a.set("scope",Json.parse("{\"workspace\":\"host-selftest\"}"));
        ((ArrayNode)c.host().path("extractor").path("inputArtifacts")).set(0,a);
        List<String> refs=new ArrayList<>(c.result().artifactRefs());refs.add(refPath);
        return new StepResult(c.result().actionId(),c.result().driverStatus(),c.result().data(),null,c.result().reason(),c.result().provenance(),refs);
    }
    private String surfaceRejection(java.util.function.Consumer<HostObservationValidatorTest.Capture> change) throws Exception {
        HostObservationValidatorTest.Capture c=new HostObservationValidatorTest().capture("enumerateWriteSurface");change.accept(c);
        c.host().set("requestedInputs",c.control().path("parameters").deepCopy());StepResult r=rebound(c);
        return assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(new ContractValidator(root),c.control(),r)).getMessage();
    }
    private static ObjectNode rows(HostObservationValidatorTest.Capture c) {return (ObjectNode)c.host().path("extractor").path("rawRows");}
    private static void requestBatch(HostObservationValidatorTest.Capture c) {((ArrayNode)c.control().path("parameters").path("probeClasses")).add("BATCH_CHANGESET");}
    @Test void writeSurfaceCoverageIsRecomputedFromTheApplicabilityPolicy() throws Exception {
        HostObservationValidatorTest.Capture ok=new HostObservationValidatorTest().capture("enumerateWriteSurface");
        HostObservationValidator.validate(new ContractValidator(root),ok.control(),ok.result());
        // 0/0 complete=true for BATCH_CHANGESET while an enumerated unbound action needs a $batch changeset probe.
        String zero=surfaceRejection(c->{requestBatch(c);((ArrayNode)rows(c).path("probeCoverage")).add(Json.parse("{\"probeClass\":\"BATCH_CHANGESET\",\"applicableTargets\":0,\"probedTargets\":0,\"complete\":true}"));});
        assertTrue(zero.contains("never probed"),zero);
        // The probe exists but the extractor under-reports applicability.
        String under=surfaceRejection(c->{requestBatch(c);
            ObjectNode probe=(ObjectNode)rows(c).path("probes").get(0).deepCopy();probe.put("probeId","batch-placeHold").put("probeClass","BATCH_CHANGESET");((ArrayNode)rows(c).path("probes")).add(probe);
            ((ArrayNode)rows(c).path("probeCoverage")).add(Json.parse("{\"probeClass\":\"BATCH_CHANGESET\",\"applicableTargets\":0,\"probedTargets\":1,\"complete\":false}"));});
        assertTrue(under.contains("applicability policy"),under);
        // A kind without a policy, and a kind from the wrong surface, are not accepted.
        assertTrue(surfaceRejection(c->((ObjectNode)rows(c).path("surfaceItems").get(1)).put("kind","CUSTOM_WRITE")).contains("kind"),"schema enum and validator policy both reject an unknown kind");
        assertTrue(surfaceRejection(c->((ObjectNode)rows(c).path("surfaceItems").get(2)).put("kind","ENTITY_SET")).contains("cannot be enumerated"));
        // A read-only entity set (writeCapable=false) still needs the requested entity write probes.
        String readonly=surfaceRejection(c->((ArrayNode)c.control().path("parameters").path("probeClasses")).add("DIRECT_CREATE"));
        assertTrue(readonly.contains("never probed"),readonly);
    }

    // opus[4]: in a product run the verifier's checkoutCommit/checkoutDirty are compared with the harness's own git state.
    @Test void preparationCheckoutIsComparedWithTheHarnessGitState() throws Exception {
        JsonNode git=Main.gitState(root);
        ObjectNode in=Json.object();in.put("codeCommit",git.path("codeCommit").asText()).put("workingTreeDirty",false)
            .put("checkoutCommit",git.path("codeCommit").asText()).put("checkoutDirty",git.path("workingTreeDirty").asBoolean());
        HostObservationValidator.checkoutMatchesHarness(in,git);
        ObjectNode copied=in.deepCopy();copied.put("checkoutCommit","0".repeat(40));
        assertTrue(assertThrows(IllegalArgumentException.class,()->HostObservationValidator.checkoutMatchesHarness(copied,git)).getMessage().contains("git HEAD"));
        ObjectNode clean=in.deepCopy();clean.put("checkoutDirty",!git.path("workingTreeDirty").asBoolean());
        assertTrue(assertThrows(IllegalArgumentException.class,()->HostObservationValidator.checkoutMatchesHarness(clean,git)).getMessage().contains("working tree"));
    }

    // opus[7]/astra[3]: a fixed-quantity primary may not read an observer-derived /data/data value.
    @Test void fixedQuantityPrimariesReadRowsNotObserverDerivations() throws Exception {
        ContractValidator v=new ContractValidator(root);JsonNode catalog=Json.read(root.resolve("verification/requirements/mandatory-oracles.json"));
        Map<String,JsonNode> cases=new LinkedHashMap<>();for(String id:CASES) cases.put(id,caseJson(id));
        List<String> problems=new ArrayList<>();CatalogLinkValidator.validate(v,catalog,cases,problems);
        assertEquals(List.of(),problems.stream().filter(p->p.contains("fixed quantity")).toList());
        // The round 4 V7 primary: decimalDelta over the observer's own dispatchedQuantity derivation.
        ObjectNode v7=caseJson("V7").deepCopy();
        for(JsonNode s:v7.path("subcases")) {
            ArrayNode kept=Json.array();
            for(JsonNode a:s.path("assertions")) {
                if(a.path("id").asText().equals("new-effect-quantity0")) {
                    ObjectNode old=a.deepCopy();old.put("op","decimalDelta");old.set("source",Json.parse("{\"actionId\":\"after\",\"pointer\":\"/data/data/dispatchedQuantity\"}"));
                    old.set("unitSource",Json.parse("{\"actionId\":\"after\",\"pointer\":\"/data/data/unit\"}"));
                    String base=s.path("id").asText().equals("restart-after-revoke")?"prior-committed":"before";
                    old.set("baseline",Json.parse("{\"actionId\":\""+base+"\",\"pointer\":\"/data/data/dispatchedQuantity\"}"));
                    old.set("baselineUnitSource",Json.parse("{\"actionId\":\""+base+"\",\"pointer\":\"/data/data/unit\"}"));kept.add(old);
                } else kept.add(a);
            }
            ((ObjectNode)s).set("assertions",kept);
        }
        cases.put("V7",v7);problems.clear();CatalogLinkValidator.validate(v,catalog,cases,problems);
        for(String name:List.of("V7.enqueue-then-revoke/new-effects-quantity","V7.authorization-then-revoke/effect-if-revoke-commits-first","V7.restart-after-revocation/unauthorized-recovered-effects"))
            assertTrue(problems.contains("Missing substantive fixed quantity assertion (value/unit/operator) "+name),name+": "+problems);
    }
}
