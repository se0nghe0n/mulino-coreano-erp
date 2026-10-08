package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Fixed host captures and deliberately blocked local test ports; no product/process behavior. */
final class CommonReviewRegressionTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private final HostObservationValidatorTest hostSamples=new HostObservationValidatorTest();
    private static final String SUBCASE="hold-preserves-physical";

    private HostObservationValidatorTest.Capture rows(HostObservationValidatorTest.Capture capture) throws Exception {
        ObjectNode rows=Json.object();for(String key:List.of("evidenceClass","operation","scope","command","environment","operationEvidence")) rows.set(key,capture.host().path(key).deepCopy());rows.set("driverProvenance",capture.result().provenance().deepCopy());
        Path file=Files.createTempFile(root.resolve("verification/harness/target"),"host-review-rows-",".json");Json.write(file,rows);String ref=root.relativize(file).toString();
        ObjectNode descriptor=Json.object();descriptor.put("path",ref).put("sha256",Json.sha256(file)).put("sizeBytes",Files.size(file)).put("completeness","COMPLETE");descriptor.set("scope",capture.host().path("scope").deepCopy());
        ObjectNode extractor=(ObjectNode)capture.host().path("extractor");extractor.set("rawRows",rows);extractor.put("rawRowsArtifactRef",ref);((ArrayNode)extractor.path("inputArtifacts")).add(descriptor);
        List<String> refs=new ArrayList<>(capture.result().artifactRefs());refs.add(ref);StepResult result=capture.result();
        return new HostObservationValidatorTest.Capture(capture.control(),capture.host(),new StepResult(result.actionId(),result.driverStatus(),result.data(),result.response(),result.reason(),result.provenance(),refs));
    }
    private HostObservationValidatorTest.Capture submitted(String operation) throws Exception {
        var capture=hostSamples.capture(operation);ObjectNode identity=(ObjectNode)capture.host().path("operationEvidence");identity.put("submissionStatus","SUBMITTED").put("taskId","synthetic-task-1").put("invocationHandle","synthetic-handle-1").put("submittedAt","2026-10-07T00:00:00Z");return rows(capture);
    }
    private ObjectNode control(String id,JsonNode control) {ObjectNode a=Json.object();a.put("id",id).put("kind","control");a.set("control",control.deepCopy());a.set("evidenceRefs",Json.parse("[\"CAPTURED_SELFTEST:mechanical-host-evidence\"]"));return a;}
    private ObjectNode terminalAction(HostObservationValidatorTest.Capture capture,boolean withHandle) {
        ObjectNode a=control("terminal",capture.control());ObjectNode parameters=(ObjectNode)a.path("control").path("parameters");
        parameters.set("taskId",Json.parse("{\"$result\":{\"actionId\":\"submitted\",\"pointer\":\"/data/hostObservation/operationEvidence/taskId\"}}"));
        if(withHandle) parameters.set("invocationHandle",Json.parse("{\"$result\":{\"actionId\":\"submitted\",\"pointer\":\"/data/hostObservation/operationEvidence/invocationHandle\"}}"));else parameters.remove("invocationHandle");return a;
    }
    private Path caseFile(List<JsonNode> actions,String assertionAction) throws Exception {
        JsonNode c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));ObjectNode sub=(ObjectNode)c.path("subcases").get(0);sub.set("actions",Json.MAPPER.valueToTree(actions));
        ObjectNode assertion=(ObjectNode)sub.path("assertions").get(0).deepCopy();assertion.put("id","actual-ack").put("op","equals");assertion.remove(List.of("unit","unitSource"));ObjectNode source=Json.object();source.put("actionId",assertionAction).put("pointer","/data/acknowledged");assertion.set("source",source);assertion.put("expected",true);sub.set("assertions",Json.array().add(assertion));
        Path file=Files.createTempFile(root.resolve("verification/harness/target"),"common-review-case-",".json");Json.write(file,c);return file;
    }
    private abstract static class TestPort implements AcceptanceDriver {
        public Set<String> availableAdapters(){return Set.of("CAPTURED_SELFTEST_ONLY");}
        public StepResult installFixture(String id,JsonNode fixture){throw new AssertionError("Unexpected fixture port");}
        public StepResult invoke(String id,String route,JsonNode actor,String cap,JsonNode request){throw new AssertionError("Unexpected business invoke");}
        public StepResult query(String id,String route,JsonNode actor,String cap,JsonNode request){throw new AssertionError("Unexpected business query");}
        public StepResult observe(String id,JsonNode request){throw new AssertionError("Unexpected database observation");}
        public StepResult control(String id,JsonNode request){throw new AssertionError("Unexpected control");}
        public StepResult start(String id,String route,JsonNode actor,String cap,JsonNode request){throw new AssertionError("Unexpected start");}
        public StepResult await(String id,JsonNode handle,int timeout){throw new AssertionError("Unexpected await");}
    }
    private CaseRunner runner(Path path,Map<String,StepResult> captures) throws Exception {
        return CaseRunner.harnessSelftest(new ContractValidator(root),new TestPort(){public StepResult control(String id,JsonNode requested){StepResult capture=captures.get(id);return new StepResult(id,capture.driverStatus(),capture.data(),capture.response(),capture.reason(),capture.provenance(),capture.artifactRefs());}},new AgentRunner.Scripted(),path,SUBCASE);
    }
    @Test void actualTypedSubmissionReferencesResolveAndRequireMatchingAutonomousTerminal() throws Exception {
        for(String operation:List.of("tickScheduler","sweepDue")) {
            var submit=submitted(operation);var terminal=hostSamples.capture("awaitRuntimeTask");
            Path file=caseFile(List.of(control("submitted",submit.control()),terminalAction(terminal,true)),"terminal");
            CaseRunner runner=runner(file,Map.of("submitted",submit.result(),"terminal",terminal.result()));assertEquals("PASS",runner.run(false));runner.verifyComplete();assertEquals(0,runner.evidence("PASS","CAPTURED_SELFTEST").path("uncompletedRuntimeTasks").asInt());
        }
    }
    @Test void noTaskIsObservedNotInferredAndCannotSupplyFakeTaskIdentifiers() throws Exception {
        var noTask=hostSamples.capture("tickScheduler");HostObservationValidator.validate(new ContractValidator(root),noTask.control(),noTask.result());
        CaseRunner runner=runner(caseFile(List.of(control("no-task",noTask.control())),"no-task"),Map.of("no-task",noTask.result()));assertEquals("PASS",runner.run(false));runner.verifyComplete();
        ((ObjectNode)noTask.host().path("operationEvidence")).put("taskId","not-submitted");assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(new ContractValidator(root),noTask.control(),noTask.result()));
        var submitted=submitted("tickScheduler");submitted.host().set("runtimeTask",hostSamples.capture("awaitRuntimeTask").host().path("runtimeTask"));assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(new ContractValidator(root),submitted.control(),submitted.result()));
    }
    @Test void submissionAckAloneAndWrongHandleEnvironmentOrUnknownTerminalCannotComplete() throws Exception {
        var submit=submitted("tickScheduler");CaseRunner incomplete=runner(caseFile(List.of(control("submitted",submit.control())),"submitted"),Map.of("submitted",submit.result()));assertEquals("NOT_RUN",incomplete.run(false));assertThrows(AssertionError.class,incomplete::verifyComplete);
        var terminal=hostSamples.capture("awaitRuntimeTask");CaseRunner unknown=runner(caseFile(List.of(control("terminal",terminal.control())),"terminal"),Map.of("terminal",terminal.result()));assertThrows(IllegalArgumentException.class,()->unknown.run(false));
        var wrongHandle=submitted("tickScheduler");((ObjectNode)wrongHandle.host().path("operationEvidence")).put("invocationHandle","other-handle");wrongHandle=rows(wrongHandle);
        ((ObjectNode)terminal.control().path("parameters")).remove("invocationHandle");terminal.host().set("requestedInputs",terminal.control().path("parameters").deepCopy());
        CaseRunner wrong=runner(caseFile(List.of(control("submitted",wrongHandle.control()),terminalAction(terminal,false)),"terminal"),Map.of("submitted",wrongHandle.result(),"terminal",terminal.result()));assertThrows(IllegalArgumentException.class,()->wrong.run(false));
        var environment=submitted("tickScheduler");((ObjectNode)environment.host().path("environment")).put("workspaceId","different-isolated-environment");environment=rows(environment);
        CaseRunner different=runner(caseFile(List.of(control("submitted",environment.control()),terminalAction(terminal,false)),"terminal"),Map.of("submitted",environment.result(),"terminal",terminal.result()));assertThrows(IllegalArgumentException.class,()->different.run(false));
    }
    private ObjectNode query(String id) {return (ObjectNode)Json.parse("{\"id\":\""+id+"\",\"kind\":\"query\",\"actorRef\":\"qc\",\"route\":\"api\",\"capabilityId\":\"getObject\",\"request\":{},\"evidenceRefs\":[\"CAPTURED_SELFTEST:blocked-local-port\"]}");}
    private Path parallelCase() throws Exception {
        ObjectNode p=Json.object();p.put("id","parallel").put("kind","parallel").put("timeoutSeconds",1);p.set("evidenceRefs",Json.parse("[\"CAPTURED_SELFTEST:parallel\"]"));ArrayNode branches=Json.array();
        for(String id:List.of("left","right")) {ObjectNode b=Json.object();b.put("id",id);b.set("actions",Json.array().add(query(id)));branches.add(b);}p.set("branches",branches);return caseFile(List.of(p,query("after-timeout")),"parallel");
    }
    @Test void blockedBranchUsesOneDeadlineInterruptsAndTerminatesBeforeFailure() throws Exception {
        CountDownLatch release=new CountDownLatch(1),rightStarted=new CountDownLatch(1),rightStopped=new CountDownLatch(1);AtomicInteger calls=new AtomicInteger();
        TestPort port=new TestPort(){public StepResult query(String id,String route,JsonNode actor,String cap,JsonNode request) {
            calls.incrementAndGet();try {if(id.equals("left")) {if(!rightStarted.await(1,TimeUnit.SECONDS)) throw new AssertionError("Right branch not submitted concurrently");Thread.sleep(650);}else {rightStarted.countDown();release.await();}}
            catch(InterruptedException interrupted) {Thread.currentThread().interrupt();}finally {if(id.equals("right")) rightStopped.countDown();}
            return StepResult.missing(id,"CAPTURED_SELFTEST local port finished; no product request");
        }};
        CaseRunner runner=CaseRunner.harnessSelftest(new ContractValidator(root),port,new AgentRunner.Scripted(),parallelCase(),SUBCASE);
        ExecutorService caller=Executors.newSingleThreadExecutor();long started=System.nanoTime();
        try {
            Future<IOException> result=caller.submit(()->assertThrows(IOException.class,()->runner.execute("parallel")));result.get(3,TimeUnit.SECONDS);long elapsedMs=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started);
            assertTrue(elapsedMs<1550,"Deadline must cover both branches, not restart after the 650ms first result: "+elapsedMs);assertEquals(0,rightStopped.getCount());
            JsonNode evidence=runner.evidence("PASS","CAPTURED_SELFTEST");assertEquals("FAIL",evidence.path("status").asText());assertTrue(evidence.path("parallelFailures").get(0).path("cleanupComplete").asBoolean());assertFalse(evidence.path("runtimeComplete").asBoolean());
            assertThrows(IOException.class,()->runner.execute("after-timeout"));assertEquals(2,calls.get());assertNull(runner.results().get("right"));
        } finally {release.countDown();caller.shutdownNow();assertTrue(caller.awaitTermination(2,TimeUnit.SECONDS));}
    }
    @Test void uncooperativePortHasBoundedCleanupFailureAndLatePublicationIsFenced() throws Exception {
        CountDownLatch release=new CountDownLatch(1),rightStopped=new CountDownLatch(1);
        TestPort port=new TestPort(){public StepResult query(String id,String route,JsonNode actor,String cap,JsonNode request) {
            if(id.equals("right")) {try {while(release.getCount()>0) try {release.await();}catch(InterruptedException ignored) {/* Deliberately hostile local port, never an actual adapter. */}}finally{rightStopped.countDown();}}
            return StepResult.missing(id,"CAPTURED_SELFTEST no external mutation");
        }};
        CaseRunner runner=CaseRunner.harnessSelftest(new ContractValidator(root),port,new AgentRunner.Scripted(),parallelCase(),SUBCASE);ExecutorService caller=Executors.newSingleThreadExecutor();
        try {
            caller.submit(()->assertThrows(IOException.class,()->runner.execute("parallel"))).get(3,TimeUnit.SECONDS);
            JsonNode evidence=runner.evidence("PASS","CAPTURED_SELFTEST");JsonNode failure=evidence.path("parallelFailures").get(0);assertFalse(failure.path("cleanupComplete").asBoolean());assertTrue(Files.isRegularFile(root.resolve(failure.path("artifactRef").asText())));assertEquals("FAIL",evidence.path("status").asText());
            assertNull(runner.results().get("right"));release.countDown();assertTrue(rightStopped.await(1,TimeUnit.SECONDS));assertNull(runner.results().get("right"));assertThrows(AssertionError.class,runner::verifyComplete);
        } finally {release.countDown();caller.shutdownNow();assertTrue(caller.awaitTermination(2,TimeUnit.SECONDS));}
    }
    @Test void v7FenceComesFromSchemaValidClaimOfTheSubmittedTaskNotTickAck() throws Exception {
        ContractValidator validator=new ContractValidator(root);JsonNode file=validator.caseFile(root.resolve("verification/cases/V7/case.json"));
        JsonNode sub=null;for(JsonNode candidate:file.path("subcases")) if(candidate.path("id").asText().equals("restart-after-revoke")) sub=candidate;
        assertNotNull(sub);Map<String,JsonNode> actions=new HashMap<>();for(JsonNode action:sub.path("actions")) actions.put(action.path("id").asText(),action);
        ObjectNode aliases=Json.object();aliases.put("ORG-A","synthetic-organization").put("P","synthetic-item").put("WORK","synthetic-work");
        var tick=submitted("tickScheduler");JsonNode tickControl=new ReferenceResolver(Map.of(),aliases).resolve(actions.get("scheduler-tick").path("control"));
        tick.control().set("parameters",tickControl.path("parameters"));tick.host().set("requestedInputs",tickControl.path("parameters"));tick.host().set("scope",tickControl.path("parameters").path("scope"));
        ((ObjectNode)tick.host().path("operationEvidence")).put("schedulerId","authority-scheduler").put("tickId","post-revoke-tick");tick=rows(tick);HostObservationValidator.validate(validator,tick.control(),tick.result());
        var claim=hostSamples.capture("claim");JsonNode claimControl=new ReferenceResolver(Map.of("scheduler-tick",tick.result().toJson()),aliases).resolve(actions.get("runtime-claim").path("control"));
        claim.control().set("parameters",claimControl.path("parameters"));claim.host().set("requestedInputs",claimControl.path("parameters"));claim.host().set("scope",claimControl.path("parameters").path("scope"));
        ObjectNode identity=(ObjectNode)claim.host().path("operationEvidence");identity.put("schedulerId","authority-scheduler");for(String key:List.of("taskId","invocationHandle")) identity.set(key,claimControl.path("parameters").path(key));
        claim=rows(claim);HostObservationValidator.validate(validator,claim.control(),claim.result());
        ReferenceResolver resolver=new ReferenceResolver(Map.of("scheduler-tick",tick.result().toJson(),"runtime-claim",claim.result().toJson()),aliases);
        assertEquals(identity.path("fencingToken"),resolver.resolve(actions.get("safe-retry").path("request").path("slots").path("claimFencingToken")));
        JsonNode terminal=resolver.resolve(actions.get("runtime-terminal").path("control"));for(String key:List.of("taskId","invocationHandle")) assertEquals(claimControl.path("parameters").path(key),terminal.path("parameters").path(key));
        identity.remove("taskId");var missing=rows(claim);assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(validator,missing.control(),missing.result()));
        identity.put("taskId","different-task");var wrong=rows(claim);assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(validator,wrong.control(),wrong.result()));
        ((ObjectNode)tick.host().path("operationEvidence")).put("fencingToken","invented-tick-fence");var forbidden=rows(tick);assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(validator,forbidden.control(),forbidden.result()));
    }
    @Test void delayedAdapterMetadataCannotStartNewPortsOrPublishAfterParallelHalt() throws Exception {
        CountDownLatch entered=new CountDownLatch(2),release=new CountDownLatch(1);AtomicInteger metadataCalls=new AtomicInteger(),portCalls=new AtomicInteger();List<Thread> blocked=new CopyOnWriteArrayList<>();
        TestPort port=new TestPort(){
            public Set<String> availableAdapters() {
                if(metadataCalls.incrementAndGet()>1) {
                    blocked.add(Thread.currentThread());entered.countDown();
                    while(release.getCount()>0) try {release.await();}catch(InterruptedException ignored) {/* Actual JVM hostile metadata callback; no product behavior. */}
                }
                return Set.of("CAPTURED_SELFTEST_ONLY");
            }
            public StepResult query(String id,String route,JsonNode actor,String cap,JsonNode request) {portCalls.incrementAndGet();return StepResult.missing(id,"CAPTURED_SELFTEST no product call");}
        };
        CaseRunner runner=CaseRunner.harnessSelftest(new ContractValidator(root),port,new AgentRunner.Scripted(),parallelCase(),SUBCASE);ExecutorService caller=Executors.newSingleThreadExecutor();
        try {
            Future<IOException> completion=caller.submit(()->assertThrows(IOException.class,()->runner.execute("parallel")));assertTrue(entered.await(1,TimeUnit.SECONDS));completion.get(3,TimeUnit.SECONDS);
            JsonNode failure=runner.evidence("PASS","CAPTURED_SELFTEST").path("parallelFailures").get(0);assertFalse(failure.path("cleanupComplete").asBoolean());assertEquals(0,portCalls.get());
            release.countDown();for(Thread thread:blocked) {thread.join(1000);assertFalse(thread.isAlive(),"Metadata branch must stop after returning to cancelled dispatcher");}
            assertEquals(0,portCalls.get(),"No new actual port calls after metadata returns following halt");assertTrue(runner.results().isEmpty(),"No child or parallel result may publish after halt");assertThrows(IOException.class,()->runner.execute("after-timeout"));
            ObjectNode evidence=Json.object();evidence.put("evidenceClass","ACTUAL_JVM_SELFTEST").put("productRuntimeClaimed",false).put("metadataCalls",metadataCalls.get()).put("newPortCallsAfterHalt",portCalls.get()).put("publishedResults",runner.results().size()).put("allMetadataThreadsTerminated",true);evidence.set("parallelFailure",failure);Json.write(root.resolve("verification/harness/target/evidence/delayed-metadata-fence-selftest.json"),evidence);
        } finally {release.countDown();for(Thread thread:blocked) thread.join(1000);caller.shutdownNow();assertTrue(caller.awaitTermination(2,TimeUnit.SECONDS));}
    }

}
