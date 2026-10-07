package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public final class CaseRunner {
    private final ContractValidator validator;
    private final AcceptanceDriver driver;
    private final AgentRunner agentRunner;
    private final JsonNode caseFile,subcase,fixture;
    private final String caseHash;
    private final Map<String,JsonNode> results=new ConcurrentHashMap<>();
    private final Set<String> asserted=new HashSet<>();
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
        String id=Json.required(a,"id"),kind=Json.required(a,"kind"); StepResult result;
        try {
            result=driver.availableAdapters().isEmpty() && !kind.equals("parallel") ? StepResult.missing(id,"NOT_IMPLEMENTED: no real product adapters installed") : switch(kind) {
                case "installFixture" -> driver.installFixture(id,fixtureBundle());
                case "invoke" -> driver.invoke(id,Json.required(a,"route"),actor(a),Json.required(a,"capabilityId"),resolve(a.path("request")));
                case "query" -> driver.query(id,Json.required(a,"route"),actor(a),Json.required(a,"capabilityId"),resolve(a.path("request")));
                case "observe" -> driver.observe(id,resolve(a.path("observation")));
                case "control" -> driver.control(id,resolve(a.path("control")));
                case "agent" -> agentRunner.run(id,Json.required(a,"route"),actor(a),resolve(a),driver);
                case "start" -> {
                    JsonNode call=a.path("call");
                    yield driver.start(id,Json.required(call,"route"),actor(call),Json.required(call,"capabilityId"),resolve(call.path("request")));
                }
                case "await" -> {
                    String sourceId=Json.required(a,"awaitActionId"); JsonNode previous=results.get(sourceId);
                    if(previous==null) throw new IllegalArgumentException("Await without submission "+sourceId);
                    if(!"EXECUTED".equals(previous.path("driverStatus").asText())) yield StepResult.missing(id,"NOT_IMPLEMENTED: asynchronous submission "+sourceId+" did not execute");
                    yield driver.await(id,previous.path("data").path("invocationHandle"),a.path("timeoutSeconds").asInt(30));
                }
                case "parallel" -> parallel(a);
                default -> throw new IllegalArgumentException("Unknown action kind "+kind);
            };
            if(!id.equals(result.actionId())) throw new IllegalArgumentException("Adapter actionId mismatch");
            validator.result(result,kind);
            if(result.driverStatus()==StepResult.DriverStatus.EXECUTED && kind.equals("control")) {
                if(!result.data().path("controlType").asText().equals(a.path("control").path("type").asText()) || !result.data().path("operation").asText().equals(a.path("control").path("operation").asText()))
                    throw new IllegalArgumentException("Control ACK does not match requested control type/operation");
                if(!result.data().hasNonNull("acknowledgedAt")) throw new IllegalArgumentException("Control ACK time absent");
                if(a.path("control").path("type").asText().equals("barrier")) for(String field:List.of("barrierId","participantId","transactionId","point","state"))
                    if(!result.data().hasNonNull(field)) throw new IllegalArgumentException("Barrier real ACK missing "+field);
            }
            if(result.driverStatus()==StepResult.DriverStatus.EXECUTED && kind.equals("await") && !result.data().path("completed").asBoolean(false))
                throw new IllegalArgumentException("await requires actual terminal invocation ACK, not only submission ACK");
            if(result.driverStatus()==StepResult.DriverStatus.EXECUTED && kind.equals("observe")) {
                if(!result.data().path("asOf").equals(resolve(a.path("observation").path("asOf"))) || !result.data().path("knownAt").equals(resolve(a.path("observation").path("knownAt"))))
                    throw new IllegalArgumentException("Observer time context differs from requested context");
            }
            results.put(id,result.toJson());
            if(kind.equals("installFixture") && result.driverStatus()==StepResult.DriverStatus.EXECUTED) {
                if(!result.data().hasNonNull("aliasMap") || !result.data().hasNonNull("fixtureHash")) throw new IllegalArgumentException("Fixture install requires actual alias map/hash");
                aliases=result.data().path("aliasMap");
            }
            return result;
        } catch(RuntimeException e) { throw new IllegalArgumentException("Action "+id+": "+e.getMessage(),e); }
    }
    private StepResult parallel(JsonNode action) throws IOException {
        // Each branch submits real async calls. Joining submission ACKs never claims transaction completion.
        try(ExecutorService pool=Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<StepResult>>> futures=new ArrayList<>();
            for(JsonNode branch:action.path("branches")) futures.add(pool.submit(() -> {
                List<StepResult> branchResults=new ArrayList<>();
                for(JsonNode child:branch.path("actions")) {
                    if(!Set.of("start","control","query").contains(child.path("kind").asText())) throw new IllegalArgumentException("parallel branch requires asynchronous start/control/query; await is explicit later");
                    branchResults.add(executeAction(child));
                }
                return branchResults;
            }));
            List<StepResult> children=new ArrayList<>();
            for(Future<List<StepResult>> future:futures) children.addAll(future.get(action.path("timeoutSeconds").asInt(30),TimeUnit.SECONDS));
            if(children.stream().anyMatch(c->c.driverStatus()!=StepResult.DriverStatus.EXECUTED)) return StepResult.missing(Json.required(action,"id"),"NOT_IMPLEMENTED: a required parallel child did not execute");
            List<String> artifacts=children.stream().flatMap(r->r.artifactRefs().stream()).distinct().toList();
            ObjectNode p=Json.object();p.put("adapter","harness-parallel-submission").put("adapterVersion","1.0.0").put("buildVersion","1.0.0").putNull("authenticatedActor").put("source","REAL_ADAPTER_ACKS").put("independent",false).put("scopeComplete",true).putNull("sourceQuery").putNull("snapshot");
            ObjectNode data=Json.object();data.set("childActionIds",Json.MAPPER.valueToTree(children.stream().map(StepResult::actionId).toList()));data.put("submissionAcknowledged",true).put("transactionCompletionClaimed",false);
            return new StepResult(Json.required(action,"id"),StepResult.DriverStatus.EXECUTED,data,null,null,p,artifacts);
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("Parallel interrupted",e); }
        catch(ExecutionException|TimeoutException e) { throw new IOException("Parallel submission failed",e); }
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
    private JsonNode resolve(JsonNode node) {
        if(node.isObject() && node.has("$alias")) {
            JsonNode value=aliases.get(node.path("$alias").asText());
            if(value==null || value.isNull()) throw new IllegalArgumentException("Unresolved server fixture alias "+node.path("$alias")); return value;
        }
        if(node.isObject() && node.has("$result")) {
            JsonNode source=node.path("$result");return new AssertionEngine().select(source,results,false);
        }
        if(node.isObject()) { ObjectNode out=Json.object();node.fields().forEachRemaining(e->out.set(e.getKey(),resolve(e.getValue())));return out; }
        if(node.isArray()) { var out=Json.array();node.forEach(n->out.add(resolve(n)));return out; }
        return node;
    }
    public void assertId(String id) {
        JsonNode assertion=assertionIndex.get(id); if(assertion==null) throw new IllegalArgumentException("Undeclared assertion "+id);
        asserted.add(id);ObjectNode evidence=Json.object(); evidence.put("assertionId",id);evidence.set("expected",assertion.path("expected"));evidence.set("source",assertion.path("source"));evidence.set("requirementRefs",assertion.path("requirementRefs"));evidence.set("evidenceRefs",assertion.path("evidenceRefs"));
        try { new AssertionEngine().check(assertion,results); evidence.put("status","PASS"); }
        catch(AssertionError e) { evidence.put("status","FAIL").put("reason",e.getMessage());assertionResults.add(evidence);throw e; }
        assertionResults.add(evidence);
    }
    public String run(boolean contractRed) throws IOException {
        for(String id:actionIndex.keySet()) execute(id);
        boolean missing=results.values().stream().anyMatch(r->!"EXECUTED".equals(r.path("driverStatus").asText()));
        if(missing && !contractRed) {
            for(JsonNode assertion:assertionIndex.values()) { ObjectNode e=Json.object();e.put("assertionId",assertion.path("id").asText()).put("status","NOT_RUN").put("reason","Required action/observer unavailable; no zero effects inferred");e.set("expected",assertion.path("expected"));e.set("source",assertion.path("source"));e.putNull("observed");assertionResults.add(e); }
            return "NOT_RUN";
        }
        boolean failed=false;
        for(String id:assertionIndex.keySet()) try { assertId(id); } catch(AssertionError e) { failed=true; }
        return failed ? "FAIL" : missing ? "NOT_RUN" : "PASS";
    }
    public void verifyComplete() {
        if(!results.keySet().containsAll(actionIndex.keySet())) throw new AssertionError("Feature omitted required actions");
        if(!asserted.containsAll(assertionIndex.keySet())) throw new AssertionError("Feature omitted substantive assertions");
        if(results.values().stream().anyMatch(r->!"EXECUTED".equals(r.path("driverStatus").asText()))) throw new AssertionError("Required adapter action NOT_IMPLEMENTED/UNAVAILABLE");
    }
    public ObjectNode evidence(String status,String command) throws IOException {
        ObjectNode e=Json.object();e.put("schemaVersion","1.0.0").put("caseId",caseFile.path("caseId").asText()).put("subcaseId",subcase.path("id").asText()).put("status",status).put("runtimeComplete",status.equals("PASS"));
        e.put("startedAt",startedAt.toString()).put("finishedAt",Instant.now().toString()).put("command",command);
        e.put("caseHash",caseHash);e.put("fixtureRef",Json.required(subcase,"fixtureRef")).put("fixtureHash",Json.sha256(validator.path(Json.required(subcase,"fixtureRef"))));
        e.set("versions",fixture.path("versions"));e.set("requiredAdapters",subcase.path("requiredAdapters"));
        e.set("actions",Json.MAPPER.valueToTree(results));e.set("assertions",Json.MAPPER.valueToTree(assertionResults));
        e.put("missingAssertions",assertionIndex.size()-asserted.size());e.put("productCoverageClaimed",false); return e;
    }
}
