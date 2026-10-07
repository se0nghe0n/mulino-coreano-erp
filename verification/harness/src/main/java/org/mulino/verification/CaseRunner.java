package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class CaseRunner {
    private final ContractValidator validator;
    private final AcceptanceDriver driver;
    private final AgentRunner agentRunner;
    private final JsonNode caseFile,subcase,fixture;
    private final String caseHash;
    private final Map<String,JsonNode> results=new ConcurrentHashMap<>();
    private final Set<String> asserted=new HashSet<>();
    private final Map<String,JsonNode> startedHandles=new ConcurrentHashMap<>();
    private final Set<String> terminalStarts=ConcurrentHashMap.newKeySet();
    private record RuntimeIdentity(String schedulerId,String taskId,String invocationHandle) {}
    private record RuntimeSubmission(String actionId,JsonNode scope,JsonNode environment,JsonNode evidenceClass,Instant submittedAt) {}
    private final Map<RuntimeIdentity,RuntimeSubmission> runtimeSubmissions=new ConcurrentHashMap<>();
    private final Set<RuntimeIdentity> runtimeTerminals=ConcurrentHashMap.newKeySet();
    private final List<ObjectNode> parallelFailures=new CopyOnWriteArrayList<>();
    private final Object executionGate=new Object();
    private volatile boolean halted;
    private final Map<String,JsonNode> actionIndex=new LinkedHashMap<>();
    private final Map<String,JsonNode> assertionIndex=new LinkedHashMap<>();
    private final List<ObjectNode> assertionResults=new ArrayList<>();
    private JsonNode aliases=Json.object();
    private final Instant startedAt=Instant.now();
    public CaseRunner(ContractValidator validator,AcceptanceDriver driver,AgentRunner agentRunner,Path casePath,String subcaseId) throws IOException {
        this.validator=validator; this.driver=driver; this.agentRunner=agentRunner;
        this.caseFile=validator.caseFile(casePath);this.caseHash=Json.sha256(casePath);
        JsonNode selected=null; for(JsonNode s:caseFile.path("subcases")) if(s.path("id").asText().equals(subcaseId)) selected=s;
        if(selected==null) throw new IllegalArgumentException("Unknown subcase "+subcaseId);
        this.subcase=selected; this.fixture=validator.fixture(Json.required(subcase,"fixtureRef"));
        for(JsonNode a:subcase.path("actions")) actionIndex.put(Json.required(a,"id"),a);
        for(JsonNode a:subcase.path("assertions")) assertionIndex.put(Json.required(a,"id"),a);
    }
    public JsonNode subcase() { return subcase; }
    public Map<String,JsonNode> results() { return Collections.unmodifiableMap(results); }
    public void execute(String id) throws IOException {
        JsonNode a=actionIndex.get(id); if(a==null) throw new IllegalArgumentException("Undeclared action "+id);
        if(results.containsKey(id)) throw new IllegalArgumentException("Action already executed "+id);
        executeAction(a);
    }
    private StepResult executeAction(JsonNode a) throws IOException {
        ensureActive();
        String id=Json.required(a,"id"),kind=Json.required(a,"kind"); StepResult result;
        try {
            boolean adaptersAvailable=!driver.availableAdapters().isEmpty();
            // Metadata may block and ignore cancellation; fence again before dispatch.
            ensureActive();
            result=!adaptersAvailable && !kind.equals("parallel") ? StepResult.missing(id,"NOT_IMPLEMENTED: no real product adapters installed") : switch(kind) {
                case "installFixture" -> dispatch(() -> driver.installFixture(id,fixtureBundle()));
                case "invoke" -> dispatch(() -> a.has("protocolOperation") ? driver.wire(id,actor(a),Json.required(a,"protocolOperation"),resolve(a.path("request"))) : driver.invoke(id,Json.required(a,"route"),actor(a),Json.required(a,"capabilityId"),resolve(a.path("request"))));
                case "query" -> dispatch(() -> a.has("protocolOperation") ? driver.wire(id,actor(a),Json.required(a,"protocolOperation"),resolve(a.path("request"))) : driver.query(id,Json.required(a,"route"),actor(a),Json.required(a,"capabilityId"),resolve(a.path("request"))));
                case "observe" -> dispatch(() -> driver.observe(id,resolve(a.path("observation"))));
                case "control" -> dispatch(() -> driver.control(id,resolve(a.path("control"))));
                case "agent" -> dispatch(() -> agentRunner.run(id,Json.required(a,"route"),actor(a),resolve(a),driver));
                case "start" -> {
                    JsonNode call=a.path("call");
                    yield dispatch(() -> call.has("protocolOperation")
                        ? driver.startWire(id,actor(call),Json.required(call,"protocolOperation"),resolve(call.path("request")))
                        : driver.start(id,Json.required(call,"route"),actor(call),Json.required(call,"capabilityId"),resolve(call.path("request"))));
                }
                case "await" -> {
                    String sourceId=Json.required(a,"awaitActionId"); JsonNode previous=results.get(sourceId);
                    if(previous==null) throw new IllegalArgumentException("Await without submission "+sourceId);
                    if(!"EXECUTED".equals(previous.path("driverStatus").asText())) yield StepResult.missing(id,"NOT_IMPLEMENTED: asynchronous submission "+sourceId+" did not execute");
                    yield dispatch(() -> driver.await(id,previous.path("data").path("invocationHandle"),a.path("timeoutSeconds").asInt(30)));
                }
                case "parallel" -> parallel(a);
                default -> throw new IllegalArgumentException("Unknown action kind "+kind);
            };
            if(!id.equals(result.actionId())) throw new IllegalArgumentException("Adapter actionId mismatch");
            ensureActive();
            validator.result(result,kind);
            if(result.driverStatus()==StepResult.DriverStatus.EXECUTED && kind.equals("control")) {
                if(!result.data().path("controlType").asText().equals(a.path("control").path("type").asText()) || !result.data().path("operation").asText().equals(a.path("control").path("operation").asText()))
                    throw new IllegalArgumentException("Control ACK does not match requested control type/operation");
                if(!result.data().hasNonNull("acknowledgedAt")) throw new IllegalArgumentException("Control ACK time absent");
                JsonNode requested=resolve(a.path("control"));
                HostObservationValidator.validate(validator,requested,result);
                if(requested.path("type").asText().equals("barrier")) for(String field:List.of("barrierId","participantId","transactionId","point","state"))
                    if(!requested.path("parameters").hasNonNull(field) || !result.data().hasNonNull(field) || !result.data().path(field).equals(requested.path("parameters").path(field)))
                        throw new IllegalArgumentException("Barrier ACK differs from requested "+field);
            }
            if(result.driverStatus()==StepResult.DriverStatus.EXECUTED && kind.equals("await")) {
                String source=Json.required(a,"awaitActionId");
                if(!result.data().path("completed").asBoolean(false) || !Set.of("SUCCEEDED","FAILED","CANCELLED").contains(result.data().path("terminalStatus").asText()))
                    throw new IllegalArgumentException("await requires actual terminal invocation ACK/status, not only submission ACK");
                if(!result.data().path("invocationHandle").equals(startedHandles.get(source))) throw new IllegalArgumentException("await terminal ACK handle differs from submitted invocation");
            }
            if(result.driverStatus()==StepResult.DriverStatus.EXECUTED && kind.equals("observe")) {
                JsonNode requested=resolve(a.path("observation"));
                for(String field:List.of("asOf","knownAt","scope")) if(!result.data().path(field).equals(requested.path(field)))
                    throw new IllegalArgumentException("Observer "+field+" differs from requested context");
                if(!result.data().path("snapshotRevision").equals(requested.path("snapshotRef")) || !result.data().path("snapshot").path("id").equals(requested.path("snapshotRef")))
                    throw new IllegalArgumentException("Observer actual snapshot id/snapshotRevision differs from requested snapshotRef");
                for(JsonNode source:requested.path("sources")) {
                    String name=source.asText();JsonNode rows=result.data().path("rawRows").path(name),evidence=result.data().path("sourceEvidence").path(name);
                    if(!rows.isArray() || !evidence.path("complete").asBoolean(false) || !evidence.path("rowPointer").asText().equals("/rawRows/"+name.replace("~","~0").replace("/","~1")))
                        throw new IllegalArgumentException("Requested raw source incomplete/missing: "+name);
                    String artifact=Json.required(evidence,"artifactRef");
                    if(!result.artifactRefs().contains(artifact) || !java.nio.file.Files.isRegularFile(validator.path(artifact))) throw new IllegalArgumentException("Requested raw source artifact missing: "+name);
                }
                if(!result.data().path("snapshot").equals(result.provenance().path("snapshot")) || !result.data().path("sourceQuery").equals(result.provenance().path("sourceQuery")))
                    throw new IllegalArgumentException("Observer data snapshot/sourceQuery differ from actual provenance");
            }
            synchronized(executionGate) {
                ensureActive();
                if(result.driverStatus()==StepResult.DriverStatus.EXECUTED) {
                    if(kind.equals("start")) startedHandles.put(id,result.data().path("invocationHandle"));
                    if(kind.equals("await")) terminalStarts.add(Json.required(a,"awaitActionId"));
                    if(kind.equals("control") && a.path("control").path("type").asText().equals("process")) runtimeControl(id,result);
                    if(kind.equals("installFixture")) {
                        if(!result.data().hasNonNull("aliasMap") || !result.data().hasNonNull("fixtureHash")) throw new IllegalArgumentException("Fixture install requires actual alias map/hash");
                        aliases=result.data().path("aliasMap");
                    }
                }
                results.put(id,result.toJson());
            }
            return result;
        } catch(RuntimeException e) { throw new IllegalArgumentException("Action "+id+": "+e.getMessage(),e); }
    }
    @FunctionalInterface private interface PortDispatch { StepResult call() throws IOException; }
    private StepResult dispatch(PortDispatch call) throws IOException {
        // Do not lock across an uncooperative port. Already dispatched remote effects need reconciliation.
        ensureActive();return call.call();
    }
    private void ensureActive() throws IOException {
        if(halted || Thread.currentThread().isInterrupted()) throw new IOException("Execution stopped after parallel cancellation; no further adapter calls or late result publication");
    }
    private void runtimeControl(String actionId,StepResult result) {
        JsonNode host=result.data().path("hostObservation"),identity=host.path("operationEvidence");String operation=host.path("operation").asText();
        if(Set.of("tickScheduler","sweepDue").contains(operation) && identity.path("submissionStatus").asText().equals("SUBMITTED")) {
            RuntimeIdentity key=new RuntimeIdentity(Json.required(identity,"schedulerId"),Json.required(identity,"taskId"),Json.required(identity,"invocationHandle"));
            RuntimeSubmission submission=new RuntimeSubmission(actionId,host.path("scope").deepCopy(),host.path("environment").deepCopy(),host.path("evidenceClass").deepCopy(),Instant.parse(Json.required(identity,"submittedAt")));
            if(runtimeSubmissions.putIfAbsent(key,submission)!=null) throw new IllegalArgumentException("Scheduler task identity was submitted more than once");
        }
        if(operation.equals("awaitRuntimeTask")) {
            JsonNode task=host.path("runtimeTask");RuntimeIdentity key=new RuntimeIdentity(Json.required(identity,"schedulerId"),Json.required(task,"taskId"),Json.required(task,"invocationHandle"));
            RuntimeSubmission submission=runtimeSubmissions.get(key);
            if(submission==null) throw new IllegalArgumentException("Runtime terminal has no matching scheduler/taskId/invocationHandle submission");
            if(!submission.scope().equals(host.path("scope")) || !submission.environment().equals(host.path("environment")) || !submission.evidenceClass().equals(host.path("evidenceClass"))) throw new IllegalArgumentException("Runtime terminal scope/environment/evidence class differs from submitted task");
            if(Instant.parse(Json.required(task,"completedAt")).isBefore(submission.submittedAt())) throw new IllegalArgumentException("Runtime task completed before actual submission");
            if(!runtimeTerminals.add(key)) throw new IllegalArgumentException("Runtime task has more than one terminal observation");
        }
    }
    private StepResult parallel(JsonNode action) throws IOException {
        // One deadline covers all branches; ExecutorService.close() would wait forever on a blocked port.
        ExecutorService pool=Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger activeBranches=new AtomicInteger();
        List<Future<List<StepResult>>> futures=new ArrayList<>();List<StepResult> children=new ArrayList<>();
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(action.path("timeoutSeconds").asInt(30));
        Throwable failure=null;boolean interrupted=false;
        try {
            for(JsonNode branch:action.path("branches")) futures.add(pool.submit(() -> {
                synchronized(executionGate) {ensureActive();activeBranches.incrementAndGet();}
                try {
                    List<StepResult> branchResults=new ArrayList<>();
                    for(JsonNode child:branch.path("actions")) {
                        ensureActive();
                        if(!Set.of("start","control","query").contains(child.path("kind").asText())) throw new IllegalArgumentException("parallel branch requires asynchronous start/control/query; await is explicit later");
                        branchResults.add(executeAction(child));
                    }
                    return branchResults;
                } finally {
                    synchronized(executionGate) {activeBranches.decrementAndGet();executionGate.notifyAll();}
                }
            }));
            for(Future<List<StepResult>> future:futures) {
                long remaining=deadline-System.nanoTime();
                if(remaining<=0) throw new TimeoutException("Total parallel submission deadline exceeded");
                children.addAll(future.get(remaining,TimeUnit.NANOSECONDS));
            }
        } catch(InterruptedException e) {failure=e;interrupted=true;}
        catch(ExecutionException|TimeoutException|RuntimeException e) {failure=e;}
        if(failure!=null) {
            synchronized(executionGate) {halted=true;}
            futures.forEach(f->f.cancel(true));pool.shutdownNow();
        } else pool.shutdown();
        // Local port implementations must honour interruption. A port that does not is unresolved, never PASS.
        boolean cleanupComplete=false;long cleanupDeadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(1);
        while(!cleanupComplete && System.nanoTime()<cleanupDeadline) {
            try {
                boolean executorTerminated=pool.awaitTermination(Math.max(1,cleanupDeadline-System.nanoTime()),TimeUnit.NANOSECONDS);
                synchronized(executionGate) {
                    cleanupComplete=executorTerminated && activeBranches.get()==0;
                    if(!cleanupComplete && executorTerminated) {
                        long remaining=cleanupDeadline-System.nanoTime();
                        if(remaining>0) TimeUnit.NANOSECONDS.timedWait(executionGate,remaining);
                    }
                }
            }
            catch(InterruptedException e) {
                interrupted=true;if(failure==null) failure=e;
                synchronized(executionGate) {halted=true;}
                futures.forEach(f->f.cancel(true));pool.shutdownNow();
            }
        }
        if(failure!=null || !cleanupComplete) {
            synchronized(executionGate) {halted=true;}
            if(!cleanupComplete) {futures.forEach(f->f.cancel(true));pool.shutdownNow();}
            ObjectNode evidence=Json.object();evidence.put("actionId",Json.required(action,"id")).put("status","FAIL").put("failureKind",failure instanceof TimeoutException?"TOTAL_DEADLINE_EXCEEDED":failure instanceof InterruptedException?"INTERRUPTED":"PARALLEL_EXECUTION_FAILED");
            evidence.put("reason",failure==null?"Executor did not terminate within bounded cleanup":failure.toString()).put("cancellationRequested",true).put("cleanupComplete",cleanupComplete).put("lateResultPublicationFenced",true).put("runtimeComplete",false).put("timeoutSeconds",action.path("timeoutSeconds").asInt(30)).put("cleanupBoundSeconds",1);
            evidence.put("localBranchesRemaining",activeBranches.get()).put("cleanupScope","LOCAL_PORT_EXECUTOR").put("remoteEffectsCancellationClaimed",false);
            var ids=Json.array();for(JsonNode branch:action.path("branches")) for(JsonNode child:branch.path("actions")) ids.add(child.path("id"));evidence.set("childActionIds",ids);
            String artifact="verification/harness/target/evidence/parallel-failure-"+UUID.randomUUID()+".json";evidence.put("artifactRef",artifact);parallelFailures.add(evidence);Json.write(validator.path(artifact),evidence);
            if(interrupted) Thread.currentThread().interrupt();
            throw new IOException("Parallel submission failed; cleanupComplete="+cleanupComplete+"; evidence="+artifact,failure);
        }
        if(interrupted) Thread.currentThread().interrupt();
        if(children.stream().anyMatch(c->c.driverStatus()!=StepResult.DriverStatus.EXECUTED)) return StepResult.missing(Json.required(action,"id"),"NOT_IMPLEMENTED: a required parallel child did not execute");
        List<String> artifacts=children.stream().flatMap(r->r.artifactRefs().stream()).distinct().toList();
        ObjectNode p=Json.object();p.put("adapter","harness-parallel-submission").put("adapterVersion","1.0.0").put("buildVersion","1.0.0").putNull("authenticatedActor").put("source","REAL_ADAPTER_ACKS").put("independent",false).put("scopeComplete",true).putNull("sourceQuery").putNull("snapshot");
        ObjectNode data=Json.object();data.set("childActionIds",Json.MAPPER.valueToTree(children.stream().map(StepResult::actionId).toList()));data.put("submissionAcknowledged",true).put("transactionCompletionClaimed",false);
        return new StepResult(Json.required(action,"id"),StepResult.DriverStatus.EXECUTED,data,null,null,p,artifacts);
    }
    private JsonNode actor(JsonNode action) {
        String actor=Json.required(action,"actorRef"); JsonNode a=fixture.path("actors").get(actor);
        if(a==null) throw new IllegalArgumentException("Actor absent from synthetic fixture "+actor);
        return a;
    }
    private JsonNode fixtureBundle() throws IOException {
        ObjectNode bundle=Json.object();bundle.set("fixture",fixture);bundle.put("caseHash",caseHash);bundle.put("fixtureRef",Json.required(subcase,"fixtureRef"));
        bundle.put("fixtureHash",Json.sha256(validator.path(Json.required(subcase,"fixtureRef"))));
        var bases=Json.array(); for(JsonNode ref:fixture.path("baseRefs")) {
            ObjectNode b=Json.object();b.put("ref",ref.asText()).put("sha256",Json.sha256(validator.path(ref.asText())));b.set("fixture",validator.fixture(ref.asText()));bases.add(b);
        }
        bundle.set("bases",bases);return bundle;
    }
    private JsonNode resolve(JsonNode node) { return new ReferenceResolver(results,aliases).resolve(node); }
    public void assertId(String id) {
        JsonNode assertion=assertionIndex.get(id); if(assertion==null) throw new IllegalArgumentException("Undeclared assertion "+id);
        asserted.add(id);ObjectNode evidence=Json.object(); evidence.put("assertionId",id);evidence.set("expected",assertion.path("expected"));evidence.set("source",assertion.path("source"));evidence.set("requirementRefs",assertion.path("requirementRefs"));evidence.set("evidenceRefs",assertion.path("evidenceRefs"));
        try {
            AssertionEngine engine=new AssertionEngine();engine.requireSourcesAvailable(assertion,results);
            if(containsAlias(assertion)) for(JsonNode action:actionIndex.values()) if(action.path("kind").asText().equals("installFixture")) engine.requireExecuted(Json.required(action,"id"),results);
            engine.check(assertion,results,aliases); evidence.put("status","PASS");
        }
        catch(AssertionError e) { evidence.put("status","FAIL").put("reason",e.getMessage());assertionResults.add(evidence);throw e; }
        assertionResults.add(evidence);
    }
    public String run(boolean contractRed) throws IOException {
        for(String id:actionIndex.keySet()) execute(id);
        boolean missing=results.values().stream().anyMatch(r->!"EXECUTED".equals(r.path("driverStatus").asText()));
        boolean failed=false;
        for(String id:assertionIndex.keySet()) {
            JsonNode assertion=assertionIndex.get(id);
            if(!contractRed && !sourcesExecuted(assertion)) {
                ObjectNode e=Json.object();e.put("assertionId",id).put("status","NOT_RUN").put("reason","Assertion source unavailable; no zero effects inferred");e.set("expected",assertion.path("expected"));e.set("source",assertion.path("source"));e.putNull("observed");assertionResults.add(e);
            } else try { assertId(id); } catch(AssertionError e) { failed=true; }
        }
        boolean incomplete=!terminalStarts.containsAll(startedHandles.keySet()) || !runtimeTerminals.containsAll(runtimeSubmissions.keySet());
        return failed ? "FAIL" : missing || incomplete ? "NOT_RUN" : "PASS";
    }
    private boolean sourcesExecuted(JsonNode assertion) {
        for(String key:List.of("source","baseline","unitSource","baselineUnitSource")) if(assertion.has(key)) {
            JsonNode r=results.get(assertion.path(key).path("actionId").asText());
            if(r==null || !r.path("driverStatus").asText().equals("EXECUTED")) return false;
        }
        if(containsAlias(assertion)) for(JsonNode action:actionIndex.values()) if(action.path("kind").asText().equals("installFixture")) {
            JsonNode r=results.get(action.path("id").asText());if(r==null || !r.path("driverStatus").asText().equals("EXECUTED")) return false;
        }
        return referenceSourcesExecuted(assertion);
    }
    private boolean containsAlias(JsonNode node) {
        if(node.isObject() && node.has("$alias")) return true;
        if(node.isContainerNode()) for(JsonNode child:node) if(containsAlias(child)) return true;
        return false;
    }
    private boolean referenceSourcesExecuted(JsonNode node) {
        if(node.isObject() && node.has("$result")) {
            JsonNode r=results.get(node.path("$result").path("actionId").asText());return r!=null && r.path("driverStatus").asText().equals("EXECUTED");
        }
        if(node.isContainerNode()) for(JsonNode child:node) if(!referenceSourcesExecuted(child)) return false;
        return true;
    }
    public void verifyComplete() {
        if(!results.keySet().containsAll(actionIndex.keySet())) throw new AssertionError("Feature omitted required actions");
        if(!asserted.containsAll(assertionIndex.keySet())) throw new AssertionError("Feature omitted substantive assertions");
        if(!terminalStarts.containsAll(startedHandles.keySet())) throw new AssertionError("Started invocation lacks terminal await ACK");
        if(!runtimeTerminals.containsAll(runtimeSubmissions.keySet())) throw new AssertionError("Submitted scheduler task lacks matching runtime terminal observation");
        if(halted || !parallelFailures.isEmpty()) throw new AssertionError("Parallel cancellation/cleanup failure prevents completion");
        if(results.values().stream().anyMatch(r->!"EXECUTED".equals(r.path("driverStatus").asText()))) throw new AssertionError("Required adapter action NOT_IMPLEMENTED/UNAVAILABLE");
    }
    public ObjectNode evidence(String status,String command) throws IOException {
        if(halted || !parallelFailures.isEmpty()) status="FAIL";
        ObjectNode e=Json.object();e.put("schemaVersion","1.0.0").put("caseId",caseFile.path("caseId").asText()).put("subcaseId",subcase.path("id").asText()).put("status",status).put("runtimeComplete",status.equals("PASS"));
        e.put("startedAt",startedAt.toString()).put("finishedAt",Instant.now().toString()).put("command",command);
        e.put("caseHash",caseHash);e.put("fixtureRef",Json.required(subcase,"fixtureRef")).put("fixtureHash",Json.sha256(validator.path(Json.required(subcase,"fixtureRef"))));
        e.set("versions",fixture.path("versions"));e.set("requiredAdapters",subcase.path("requiredAdapters"));
        e.set("actions",Json.MAPPER.valueToTree(results));e.set("assertions",Json.MAPPER.valueToTree(assertionResults));
        e.set("parallelFailures",Json.MAPPER.valueToTree(parallelFailures));e.put("uncompletedRuntimeTasks",runtimeSubmissions.size()-runtimeTerminals.size());
        e.put("missingAssertions",assertionIndex.size()-asserted.size());e.put("productCoverageClaimed",false); return e;
    }
}
