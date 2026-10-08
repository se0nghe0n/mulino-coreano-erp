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
    /** PRODUCT rejects selftest/captured evidence and enforces declared adapters; HARNESS_SELFTEST is for harness unit tests only. */
    public enum EvidencePolicy { PRODUCT, HARNESS_SELFTEST }
    /** Observer read-mode directives a case may declare instead of an API-issued revision. */
    public static final Set<String> SNAPSHOT_DIRECTIVES=Set.of("CURRENT_COMMITTED","CURRENT_LOCK_WAIT");
    private final EvidencePolicy policy;
    private volatile Set<String> observedAdapters;
    private final Map<String,String> agentActionRunners=new ConcurrentHashMap<>();
    private Instant lastObserverCapture;
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
    /** Harness instant captured right before an autonomous-loop parallel group starts, keyed by its watcher action id. */
    private final Map<String,Instant> observationBoundaries=new ConcurrentHashMap<>();
    /** The control request actually sent to the port, including the harness-resolved observeFrom of a passive watcher. */
    private final Map<String,JsonNode> resolvedControls=new ConcurrentHashMap<>();
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
        this(validator,driver,agentRunner,casePath,subcaseId,EvidencePolicy.PRODUCT);
    }
    /** Harness unit tests with captured/canned ports. Never used by Main or product Gherkin runs. */
    public static CaseRunner harnessSelftest(ContractValidator validator,AcceptanceDriver driver,AgentRunner agentRunner,Path casePath,String subcaseId) throws IOException {
        return new CaseRunner(validator,driver,agentRunner,casePath,subcaseId,EvidencePolicy.HARNESS_SELFTEST);
    }
    private CaseRunner(ContractValidator validator,AcceptanceDriver driver,AgentRunner agentRunner,Path casePath,String subcaseId,EvidencePolicy policy) throws IOException {
        this.validator=validator; this.driver=driver; this.agentRunner=agentRunner; this.policy=policy;
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
            Set<String> available=driver.availableAdapters();
            if(observedAdapters==null) observedAdapters=Set.copyOf(available);
            boolean adaptersAvailable=!available.isEmpty();
            // Metadata may block and ignore cancellation; fence again before dispatch.
            ensureActive();
            if(kind.equals("agent")) agentActionRunners.put(id,agentRunner.kind());
            String dependency=kind.equals("parallel") ? null : unavailableDependency(a);
            result=!adaptersAvailable && !kind.equals("parallel") ? StepResult.missing(id,"NOT_IMPLEMENTED: no real product adapters installed")
                : dependency!=null ? StepResult.missing(id,"NOT_IMPLEMENTED: "+dependency) : switch(kind) {
                case "installFixture" -> dispatch(() -> driver.installFixture(id,fixtureBundle()));
                case "invoke" -> dispatch(() -> a.has("protocolOperation") ? driver.wire(id,actor(a),Json.required(a,"protocolOperation"),resolve(a.path("request"))) : driver.invoke(id,Json.required(a,"route"),actor(a),Json.required(a,"capabilityId"),resolve(a.path("request"))));
                case "query" -> dispatch(() -> a.has("protocolOperation") ? driver.wire(id,actor(a),Json.required(a,"protocolOperation"),resolve(a.path("request"))) : driver.query(id,Json.required(a,"route"),actor(a),Json.required(a,"capabilityId"),resolve(a.path("request"))));
                case "observe" -> dispatch(() -> driver.observe(id,observationRequest(a.path("observation"))));
                case "control" -> {JsonNode control=controlRequest(id,a);yield dispatch(() -> driver.control(id,control));}
                case "agent" -> {
                    // A UAT-only subcase needs the real client host; the scripted runner must not stand in for it.
                    if(validator.requiresActualClient(subcase) && !agentRunner.actualClient())
                        yield StepResult.missing(id,"NOT_IMPLEMENTED: subcase requires an actual client/model runner; "+agentRunner.kind()+" cannot substitute");
                    yield dispatch(() -> agentRunner.run(id,Json.required(a,"route"),actor(a),resolve(a),driver));
                }
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
            if(result.driverStatus()==StepResult.DriverStatus.EXECUTED && policy==EvidencePolicy.PRODUCT && selftestProvenance(result.provenance()))
                throw new IllegalArgumentException("Selftest/captured/canned provenance is not product evidence: "+result.provenance().path("source").asText());
            if(result.driverStatus()==StepResult.DriverStatus.EXECUTED && kind.equals("control")) {
                if(!result.data().path("controlType").asText().equals(a.path("control").path("type").asText()) || !result.data().path("operation").asText().equals(a.path("control").path("operation").asText()))
                    throw new IllegalArgumentException("Control ACK does not match requested control type/operation");
                if(!result.data().hasNonNull("acknowledgedAt")) throw new IllegalArgumentException("Control ACK time absent");
                JsonNode requested=resolvedControls.containsKey(id)?resolvedControls.get(id):resolve(a.path("control"));
                HostObservationValidator.validate(validator,requested,result,policy==EvidencePolicy.PRODUCT,observationBoundaries.get(id));
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
                observerSnapshot(a.path("observation").path("snapshotRef"),requested.path("snapshotRef"),result);
                for(JsonNode source:requested.path("sources")) {
                    String name=source.asText();JsonNode rows=result.data().path("rawRows").path(name),evidence=result.data().path("sourceEvidence").path(name);
                    if(!rows.isArray() || !evidence.path("complete").asBoolean(false) || !evidence.path("rowPointer").asText().equals("/rawRows/"+name.replace("~","~0").replace("/","~1")))
                        throw new IllegalArgumentException("Requested raw source incomplete/missing: "+name);
                    String artifact=Json.required(evidence,"artifactRef");
                    if(!result.artifactRefs().contains(artifact) || !java.nio.file.Files.isRegularFile(validator.path(artifact))) throw new IllegalArgumentException("Requested raw source artifact missing: "+name);
                }
                if(!result.data().path("snapshot").equals(result.provenance().path("snapshot")) || !result.data().path("sourceQuery").equals(result.provenance().path("sourceQuery")))
                    throw new IllegalArgumentException("Observer data snapshot/sourceQuery differ from actual provenance");
                ObserverDerivations.verify(requested.path("sources"),result.data());
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
    /**
     * An action whose $alias/$result input comes from an action that did not execute cannot be sent;
     * it is NOT_IMPLEMENTED (NOT_RUN), never an environment abort and never a fabricated value.
     */
    private String unavailableDependency(JsonNode node) {
        if(node.isObject() && node.has("$alias")) {
            for(JsonNode action:actionIndex.values()) if(action.path("kind").asText().equals("installFixture")) {
                JsonNode r=results.get(action.path("id").asText());
                if(r!=null && !"EXECUTED".equals(r.path("driverStatus").asText())) return "fixture alias depends on installFixture "+action.path("id").asText()+" which did not execute";
            }
            return null;
        }
        if(node.isObject() && node.has("$result")) {
            JsonNode r=results.get(node.path("$result").path("actionId").asText());
            return r!=null && !"EXECUTED".equals(r.path("driverStatus").asText()) ? "input depends on action "+node.path("$result").path("actionId").asText()+" which did not execute" : null;
        }
        if(node.isContainerNode()) for(JsonNode child:node) {String reason=unavailableDependency(child);if(reason!=null) return reason;}
        return null;
    }
    /**
     * Declared adapters are a contract: one the driver/runner does not supply keeps the subcase out of PASS.
     * Uses the adapter set the driver reported when actions ran (metadata is never re-queried after a halt).
     */
    public Set<String> missingAdapters() {
        if(policy!=EvidencePolicy.PRODUCT) return Set.of();
        Set<String> missing=new TreeSet<>(validator.adapters(subcase.path("requiredAdapters")));
        if(observedAdapters!=null) missing.removeAll(validator.adapters(observedAdapters));
        missing.removeAll(validator.adapters(agentRunner.providedAdapters()));
        return Collections.unmodifiableSet(missing);
    }
    /** Selftest ports label themselves; product runs must never count such evidence. */
    static boolean selftestProvenance(JsonNode provenance) {
        for(String key:List.of("source","adapter","adapterVersion","buildVersion")) {
            String value=provenance.path(key).asText().toUpperCase(Locale.ROOT);
            if(value.contains("SELFTEST") || value.contains("CAPTURED") || value.contains("CANNED")) return true;
        }
        return false;
    }
    /**
     * The API's logical read revision and the observer's own MVCC snapshot are different things.
     * A $result-bound snapshotRef from an API read is a projection revision the observer must recompute from rows
     * (readMode RESULT_REVISION, revisionQuery recorded). The observer never receives the issued value
     * (see observationRequest), so equality here is a recomputation check. A $result-bound snapshotRef from an
     * awaitRuntimeTask terminal is a host runtime snapshot id, not a projection revision: nothing can recompute it from
     * DB rows, so it has its own read mode (RUNTIME_TASK_SNAPSHOT, see runtimeTaskSnapshot). A literal directive asks for
     * a fresh read (CURRENT_COMMITTED / CURRENT_LOCK_WAIT) and has no revision to equal. In every mode
     * snapshot.id is the observer's own token and must not echo the requested reference.
     */
    private void observerSnapshot(JsonNode declared,JsonNode requested,StepResult result) throws IOException {
        JsonNode data=result.data();
        JsonNode snapshot=data.path("snapshot");String readMode=snapshot.path("readMode").asText();
        if(declared.isTextual()) {
            if(!SNAPSHOT_DIRECTIVES.contains(declared.asText())) throw new IllegalArgumentException("Literal snapshotRef must be a read-mode directive "+SNAPSHOT_DIRECTIVES);
            if(!readMode.equals(declared.asText())) throw new IllegalArgumentException("Observer snapshot readMode differs from requested directive "+declared.asText());
            if(SNAPSHOT_DIRECTIVES.contains(data.path("snapshotRevision").asText())) throw new IllegalArgumentException("Observer snapshotRevision echoes a read-mode directive");
        } else if(runtimeTaskReference(declared)!=null) {
            runtimeTaskSnapshot(runtimeTaskReference(declared),requested,result);
        } else {
            if(!readMode.equals(RESULT_REVISION)) throw new IllegalArgumentException("Result-bound snapshotRef requires observer readMode RESULT_REVISION");
            if(!snapshot.path("revisionQuery").isObject()) throw new IllegalArgumentException("Observer must record the independent projection revision recomputation query");
            if(!data.path("snapshotRevision").equals(requested)) throw new IllegalArgumentException("Observer independently recomputed snapshotRevision differs from requested snapshotRef");
        }
        if(snapshot.path("id").equals(requested) || SNAPSHOT_DIRECTIVES.contains(snapshot.path("id").asText()) || READ_MODES.contains(snapshot.path("id").asText()))
            throw new IllegalArgumentException("Observer snapshot.id echoes the requested snapshotRef instead of its own DB snapshot token");
        Instant captured;
        try { captured=java.time.OffsetDateTime.parse(Json.required(snapshot,"capturedAt")).toInstant(); }
        catch(java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("Observer snapshot capturedAt is not an instant",e); }
        synchronized(this) {
            if(lastObserverCapture!=null && captured.isBefore(lastObserverCapture)) throw new IllegalArgumentException("Observer snapshot captured before an earlier observation in this subcase");
            lastObserverCapture=captured;
        }
    }
    /**
     * RUNTIME_TASK_SNAPSHOT: the case reads the DB at the terminal of an autonomous scheduler task
     * (/data/hostObservation/runtimeTask/snapshot/id of an awaitRuntimeTask control). The observer is told which task
     * (snapshotSource schedulerId/taskId/invocationHandle), not the snapshot id. It must locate that task's host snapshot
     * artifact itself and report it in snapshot.runtimeTaskSnapshot: the task identity, the snapshotId read from the
     * artifact, the artifactRef and the SHA-256 of its bytes. The harness checks the reported snapshotId against the
     * id it kept from the await result, the artifactRef against that result, the hash against the file bytes, that the
     * artifact is linked to this observation, and that the observer's DB read was not captured before the task terminal.
     * snapshotRevision is the observer's own value; there is no projection revision to equal.
     */
    private void runtimeTaskSnapshot(JsonNode issuing,JsonNode requested,StepResult result) throws IOException {
        JsonNode data=result.data(),snapshot=data.path("snapshot"),reported=snapshot.path("runtimeTaskSnapshot");
        if(!RUNTIME_TASK_SNAPSHOT.equals(snapshot.path("readMode").asText())) throw new IllegalArgumentException("Runtime-task-bound snapshotRef requires observer readMode "+RUNTIME_TASK_SNAPSHOT);
        if(!reported.isObject()) throw new IllegalArgumentException("Observer must report the host runtime snapshot it bound its read to (snapshot.runtimeTaskSnapshot)");
        JsonNode parameters=resolve(issuing.path("control").path("parameters"));
        JsonNode terminal=results.get(Json.required(issuing,"id"));
        if(terminal==null) throw new IllegalArgumentException("Runtime task snapshot source did not execute");
        JsonNode task=terminal.path("data").path("hostObservation").path("runtimeTask");
        if(!reported.path("schedulerId").equals(parameters.path("schedulerId"))) throw new IllegalArgumentException("Observer runtime snapshot belongs to a different scheduler");
        for(String key:List.of("taskId","invocationHandle")) if(!reported.path(key).equals(task.path(key))) throw new IllegalArgumentException("Observer runtime snapshot "+key+" differs from the awaited task");
        if(!reported.path("snapshotId").equals(requested)) throw new IllegalArgumentException("Observer runtime snapshotId differs from the host snapshot of the awaited task terminal");
        String artifact=Json.required(reported,"artifactRef");
        if(!artifact.equals(task.path("snapshot").path("artifactRef").asText())) throw new IllegalArgumentException("Observer runtime snapshot artifact differs from the awaited task's host snapshot artifact");
        if(!result.artifactRefs().contains(artifact) || !java.nio.file.Files.isRegularFile(validator.path(artifact))) throw new IllegalArgumentException("Observer runtime snapshot artifact is not linked to this observation");
        if(!Json.sha256(validator.path(artifact)).equals(reported.path("sha256").asText())) throw new IllegalArgumentException("Observer runtime snapshot sha256 differs from the host snapshot artifact bytes");
        if(!Json.read(validator.path(artifact)).path("snapshotId").equals(requested)) throw new IllegalArgumentException("Host runtime snapshot artifact names a different snapshotId");
        Instant captured=java.time.OffsetDateTime.parse(Json.required(snapshot,"capturedAt")).toInstant();
        if(captured.isBefore(Instant.parse(Json.required(task,"completedAt")))) throw new IllegalArgumentException("Observer DB read was captured before the awaited runtime task terminal");
        String revision=data.path("snapshotRevision").asText();
        if(data.path("snapshotRevision").equals(requested) || READ_MODES.contains(revision)) throw new IllegalArgumentException("Observer snapshotRevision echoes the runtime snapshot reference or a read mode");
    }
    /** The awaitRuntimeTask control a $result snapshotRef points at with the runtime snapshot pointer, or null. */
    private JsonNode runtimeTaskReference(JsonNode declared) {
        if(!declared.isObject() || !declared.has("$result")) return null;
        JsonNode ref=declared.path("$result");
        if(!RUNTIME_SNAPSHOT_POINTER.equals(ref.path("pointer").asText())) return null;
        JsonNode issuing=findAction(subcase.path("actions"),ref.path("actionId").asText());
        return issuing!=null && isRuntimeTaskAwait(issuing) ? issuing : null;
    }
    static boolean isRuntimeTaskAwait(JsonNode action) {
        return action.path("kind").asText().equals("control") && action.path("control").path("type").asText().equals("process") && action.path("control").path("operation").asText().equals("awaitRuntimeTask");
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
    /**
     * Resolves a control action. A passive natural-tick watcher (HostObservationValidator.passiveWatch) gets the observation
     * boundary as the resolved request parameter observeFrom: the group's pre-submission boundary for the autonomous-loop
     * watcher, otherwise the harness instant taken just before this watcher is dispatched. The host adapter's extractor reads
     * the scheduler's durable submission rows in [observeFrom, observeFrom+observationWindowSeconds] and the validator checks
     * the same window, so a group watcher cannot lose an early loop submission and a standalone repeat watcher does not
     * report an earlier sweep's row (host-observation-guide.md). observeFrom is harness-owned; a case never authors it.
     */
    private JsonNode controlRequest(String id,JsonNode action) {
        JsonNode control=resolve(action.path("control"));
        if(HostObservationValidator.passiveWatch(control) && control.path("parameters").isObject()) {
            Instant from=observationBoundaries.computeIfAbsent(id,k->Instant.now());
            ObjectNode copy=((ObjectNode)control).deepCopy();((ObjectNode)copy.path("parameters")).put(HostObservationValidator.OBSERVE_FROM,from.toString());control=copy;
        }
        resolvedControls.put(id,control);return control;
    }
    private StepResult parallel(JsonNode action) throws IOException {
        // One deadline covers all branches; ExecutorService.close() would wait forever on a blocked port.
        ExecutorService pool=Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger activeBranches=new AtomicInteger();
        List<Future<List<StepResult>>> futures=new ArrayList<>();List<StepResult> children=new ArrayList<>();
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(action.path("timeoutSeconds").asInt(30));
        Throwable failure=null;boolean interrupted=false;
        // Autonomous-loop group: branch 0 starts with a passive natural-tick watcher. The observation window is anchored at
        // this instant, before any branch (watcher or process start) is submitted, so the watcher thread's own start latency
        // cannot move the window past the loop's first submission (host-observation-guide.md, observation boundary).
        JsonNode watcher=action.path("branches").path(0).path("actions").path(0);
        Instant boundary=null;
        if(watcher.path("kind").asText().equals("control") && HostObservationValidator.passiveWatch(watcher.path("control"))) {
            boundary=Instant.now();observationBoundaries.put(Json.required(watcher,"id"),boundary);
        }
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
        if(boundary!=null) data.put("observationBoundaryAt",boundary.toString());
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
    /** Read-mode directive sent to the observer in place of a $result-bound API snapshotRevision. */
    public static final String RESULT_REVISION="RESULT_REVISION";
    /** Read-mode directive sent to the observer in place of a $result-bound host runtime task snapshot id. */
    public static final String RUNTIME_TASK_SNAPSHOT="RUNTIME_TASK_SNAPSHOT";
    /** The only host pointer a snapshotRef may bind: the terminal snapshot id of an awaitRuntimeTask control. */
    public static final String RUNTIME_SNAPSHOT_POINTER="/data/hostObservation/runtimeTask/snapshot/id";
    /** The only API pointer a snapshotRef may bind: the projection revision of an invoke/query/start response. */
    public static final String API_REVISION_POINTER="/response/snapshotRevision";
    static final Set<String> READ_MODES=Set.of("CURRENT_COMMITTED","CURRENT_LOCK_WAIT",RESULT_REVISION,RUNTIME_TASK_SNAPSHOT);
    /**
     * The observation request sent to the observer. A $result-bound snapshotRef is NOT resolved for the observer.
     * For an API projection revision it receives snapshotRef=RESULT_REVISION and snapshotSource, the identity of the
     * request that issued the revision (action, route, capability, actor and that action's resolved request), and must
     * recompute the projection revision from authoritative rows. For the terminal snapshot of an awaitRuntimeTask it
     * receives snapshotRef=RUNTIME_TASK_SNAPSHOT and snapshotSource with the awaited task identity (schedulerId, taskId,
     * invocationHandle, scope), and must bind its read to that task's host snapshot artifact. The harness keeps the
     * issued value and compares it afterwards, so copying the requested token is impossible and an echo is no longer
     * indistinguishable from a recomputation or a binding.
     */
    JsonNode observationRequest(JsonNode declared) {
        JsonNode resolved=resolve(declared),ref=declared.path("snapshotRef");
        if(!ref.isObject() || !ref.has("$result")) return resolved;
        ObjectNode request=(ObjectNode)resolved.deepCopy();
        String actionId=Json.required(ref.path("$result"),"actionId");
        ObjectNode source=Json.object().put("actionId",actionId).put("pointer",Json.required(ref.path("$result"),"pointer"));
        JsonNode issuing=findAction(subcase.path("actions"),actionId);
        JsonNode runtime=runtimeTaskReference(ref);
        if(runtime!=null) {
            request.put("snapshotRef",RUNTIME_TASK_SNAPSHOT);
            JsonNode parameters=resolve(runtime.path("control").path("parameters"));
            source.put("kind","control").put("operation","awaitRuntimeTask");
            for(String key:List.of("schedulerId","taskId","invocationHandle","scope")) if(parameters.has(key)) source.set(key,parameters.path(key));
        } else {
            request.put("snapshotRef",RESULT_REVISION);
            if(issuing!=null) {
                JsonNode call=issuing.path("kind").asText().equals("start") ? issuing.path("call") : issuing;
                for(String key:List.of("kind","route","capabilityId","protocolOperation","actorRef")) if(call.has(key)) source.set(key,call.path(key));
                if(call.has("actorRef")) source.set("actor",actor(call));
                if(call.has("request")) source.set("request",resolve(call.path("request")));
            }
        }
        request.set("snapshotSource",source);
        return request;
    }
    private static JsonNode findAction(JsonNode actions,String id) {
        for(JsonNode a:actions) {
            if(a.path("id").asText().equals(id)) return a;
            JsonNode found=findAction(a.path("call").isObject()?Json.array().add(a.path("call")):Json.array(),id);
            if(found!=null) return found;
            for(JsonNode branch:a.path("branches")) {found=findAction(branch.path("actions"),id);if(found!=null) return found;}
        }
        return null;
    }
    public void assertId(String id) {
        JsonNode assertion=assertionIndex.get(id); if(assertion==null) throw new IllegalArgumentException("Undeclared assertion "+id);
        asserted.add(id);ObjectNode evidence=Json.object(); evidence.put("assertionId",id);evidence.set("expected",assertion.path("expected"));evidence.set("source",assertion.path("source"));evidence.set("requirementRefs",assertion.path("requirementRefs"));evidence.set("evidenceRefs",assertion.path("evidenceRefs"));
        declaredComparison(evidence,assertion);
        // scope is declarative traceability; selection is enforced only by source.where and the observation request.
        evidence.set("declaredScope",assertion.path("scope"));evidence.put("scopeEnforced",false);
        AssertionEngine engine=new AssertionEngine();
        try {
            engine.requireSourcesAvailable(assertion,results);
            if(containsAlias(assertion)) for(JsonNode action:actionIndex.values()) if(action.path("kind").asText().equals("installFixture")) engine.requireExecuted(Json.required(action,"id"),results);
        }
        catch(AssertionError e) { evidence.put("status","FAIL").put("failureKind","SOURCE_UNAVAILABLE").put("reason",e.getMessage());assertionResults.add(evidence);throw e; }
        try { engine.check(assertion,results,aliases); evidence.put("status","PASS"); }
        catch(AssertionError e) { evidence.put("status","FAIL").put("failureKind","VIOLATION").put("reason",e.getMessage());recordCompared(evidence,engine);assertionResults.add(evidence);throw e; }
        recordCompared(evidence,engine);
        assertionResults.add(evidence);
    }
    /** op/unit/baseline/unit sources as declared, so the assembler can compare the record with the case and re-check it. */
    private static void declaredComparison(ObjectNode evidence,JsonNode assertion) {
        evidence.put("op",assertion.path("op").asText());
        for(String key:List.of("unit","baseline","unitSource","baselineUnitSource")) if(assertion.has(key)) evidence.set(key,assertion.path(key));
        if(assertion.path("source").has("where")) evidence.set("where",assertion.path("source").path("where"));
        if(assertion.path("source").has("field")) evidence.set("field",assertion.path("source").path("field"));
    }
    /** The post-projection values actually compared (after where/field), with resolved references; absent is explicit. */
    private static void recordCompared(ObjectNode evidence,AssertionEngine engine) {
        JsonNode resolved=engine.resolvedAssertion();
        if(resolved!=null) {
            if(resolved.has("expected")) evidence.set("resolvedExpected",resolved.path("expected"));
            if(resolved.path("source").has("where")) evidence.set("resolvedWhere",resolved.path("source").path("where"));
        }
        evidence.set("observed",compared(engine.observedValue()));
        if(engine.observedUnit()!=null) evidence.set("observedUnit",compared(engine.observedUnit()));
        if(engine.observedBaseline()!=null) evidence.set("observedBaseline",compared(engine.observedBaseline()));
    }
    private static JsonNode compared(JsonNode value) {
        if(value==null) return Json.MAPPER.nullNode();
        return value.isMissingNode() ? Json.object().put("observation","ABSENT") : value;
    }
    public String run(boolean contractRed) throws IOException {
        for(String id:actionIndex.keySet()) execute(id);
        boolean missing=results.values().stream().anyMatch(r->!"EXECUTED".equals(r.path("driverStatus").asText()));
        boolean failed=false;
        for(String id:assertionIndex.keySet()) {
            JsonNode assertion=assertionIndex.get(id);
            if(!contractRed && !sourcesExecuted(assertion)) {
                ObjectNode e=Json.object();e.put("assertionId",id).put("status","NOT_RUN").put("reason","Assertion source unavailable; no zero effects inferred");e.set("expected",assertion.path("expected"));e.set("source",assertion.path("source"));declaredComparison(e,assertion);e.putNull("observed");assertionResults.add(e);
            } else try { assertId(id); } catch(AssertionError e) { failed=true; }
        }
        boolean incomplete=!terminalStarts.containsAll(startedHandles.keySet()) || !runtimeTerminals.containsAll(runtimeSubmissions.keySet());
        return failed ? "FAIL" : missing || incomplete || !missingAdapters().isEmpty() ? "NOT_RUN" : "PASS";
    }
    /**
     * Gherkin stops at the first failed step. For an honest scenario verdict, evaluate every assertion
     * not yet asserted, without throwing, and classify by structured failureKind rather than message text.
     */
    public String verdictAfterStop() {
        for(String id:assertionIndex.keySet()) if(!asserted.contains(id)) {
            if(!sourcesExecuted(assertionIndex.get(id))) {
                asserted.add(id);ObjectNode e=Json.object();e.put("assertionId",id).put("status","NOT_RUN").put("failureKind","SOURCE_UNAVAILABLE").put("reason","Assertion source unavailable; no zero effects inferred");assertionResults.add(e);
            } else try { assertId(id); } catch(AssertionError ignored) { /* recorded with failureKind */ }
        }
        boolean violation=assertionResults.stream().anyMatch(e->"VIOLATION".equals(e.path("failureKind").asText()));
        boolean unavailable=assertionResults.stream().anyMatch(e->"SOURCE_UNAVAILABLE".equals(e.path("failureKind").asText()))
            || results.values().stream().anyMatch(r->!"EXECUTED".equals(r.path("driverStatus").asText())) || !results.keySet().containsAll(actionIndex.keySet());
        if(violation || halted || !parallelFailures.isEmpty()) return "FAIL";
        boolean incomplete=!terminalStarts.containsAll(startedHandles.keySet()) || !runtimeTerminals.containsAll(runtimeSubmissions.keySet());
        return unavailable || incomplete || !missingAdapters().isEmpty() ? "NOT_RUN" : "PASS";
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
        if(!missingAdapters().isEmpty()) throw new AssertionError("NOT_IMPLEMENTED: declared requiredAdapters not supplied by driver/runner "+missingAdapters());
    }
    public ObjectNode evidence(String status,String command) throws IOException {
        if(halted || !parallelFailures.isEmpty()) status="FAIL";
        ObjectNode e=Json.object();e.put("schemaVersion","1.0.0").put("caseId",caseFile.path("caseId").asText()).put("subcaseId",subcase.path("id").asText()).put("status",status).put("runtimeComplete",status.equals("PASS"));
        e.put("startedAt",startedAt.toString()).put("finishedAt",Instant.now().toString()).put("command",command);
        e.put("caseHash",caseHash);e.put("fixtureRef",Json.required(subcase,"fixtureRef")).put("fixtureHash",Json.sha256(validator.path(Json.required(subcase,"fixtureRef"))));
        e.set("versions",fixture.path("versions"));e.set("requiredAdapters",subcase.path("requiredAdapters"));
        e.put("evidencePolicy",policy.name()).put("adapterCheck",policy==EvidencePolicy.PRODUCT?"ENFORCED":"SKIPPED_HARNESS_SELFTEST");
        e.set("missingAdapters",Json.MAPPER.valueToTree(missingAdapters()));e.put("agentRunner",agentRunner.kind());e.set("agentActionRunners",Json.MAPPER.valueToTree(new TreeMap<>(agentActionRunners)));
        e.set("actions",Json.MAPPER.valueToTree(results));e.set("assertions",Json.MAPPER.valueToTree(assertionResults));
        e.set("parallelFailures",Json.MAPPER.valueToTree(parallelFailures));e.put("uncompletedRuntimeTasks",runtimeSubmissions.size()-runtimeTerminals.size());
        e.put("missingAssertions",assertionIndex.size()-asserted.size());e.put("productCoverageClaimed",false); return e;
    }
}
