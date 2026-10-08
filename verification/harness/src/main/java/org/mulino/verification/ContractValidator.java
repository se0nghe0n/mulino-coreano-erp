package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class ContractValidator {
    private final Path root;
    private final Set<String> capabilities=new HashSet<>();
    private final Map<String,String> adapterAliases=new HashMap<>();
    /** Adapter names that only an actual client/model host (UAT runner) can supply. */
    public static final Set<String> CLIENT_ADAPTERS=Set.of("client","model");
    /** Driver metadata is request-side provenance written by the harness port, never an independent oracle source. */
    public static final List<String> NON_ORACLE_POINTERS=List.of("/provenance","/reason","/artifactRefs","/driverStatus","/actionId");
    public ContractValidator(Path root) throws IOException {
        this.root=root.toAbsolutePath().normalize();
        JsonNode registry=Json.read(root.resolve("contracts/acceptance-capabilities.json"));
        for(JsonNode c:registry.path("capabilities")) capabilities.add(Json.required(c,"id"));
        registry.path("adapterAliases").fields().forEachRemaining(e->adapterAliases.put(e.getKey(),e.getValue().asText()));
    }
    /** Canonical adapter vocabulary; unknown names stay distinct so no driver can satisfy them implicitly. */
    public String adapter(String name) { return adapterAliases.getOrDefault(name,name); }
    public Set<String> adapters(Iterable<?> names) {
        Set<String> out=new TreeSet<>();
        for(Object n:names) out.add(adapter(n instanceof JsonNode j ? j.asText() : String.valueOf(n)));
        return out;
    }
    /** UAT-only subcases need a real client/model host; a scripted SIT runner cannot substitute. */
    public boolean requiresActualClient(JsonNode subcase) {
        for(String a:adapters(subcase.path("requiredAdapters"))) if(CLIENT_ADAPTERS.contains(a)) return true;
        return false;
    }
    /** Preparation problems for oracle sources that read harness-side driver metadata. */
    public List<String> oracleSourceProblems(JsonNode caseFile) {
        List<String> problems=new ArrayList<>();
        for(JsonNode sub:caseFile.path("subcases")) for(JsonNode assertion:sub.path("assertions"))
            for(String field:List.of("source","baseline","unitSource","baselineUnitSource")) if(assertion.has(field)) {
                String pointer=assertion.path(field).path("pointer").asText();
                for(String forbidden:NON_ORACLE_POINTERS) if(pointer.equals(forbidden) || pointer.startsWith(forbidden+"/"))
                    problems.add(caseFile.path("caseId").asText()+"/"+sub.path("id").asText()+"/"+assertion.path("id").asText()+": "+field+" "+pointer+" reads driver request-side metadata, not an independent server/DB observation");
            }
        return problems;
    }
    /**
     * contracts/command-response.schema.json fixes one structured error envelope: the code is at /error/code of the
     * response object, read as /response/error/code (and /response/body/result/structuredContent/error/code over raw
     * MCP wire). A case that reads /response/errorCode, /response/code or any other *errorCode field asserts a field
     * the contract forbids, so a correct product cannot pass it while a non-conforming one can. JSON-RPC protocol
     * errors (/response/body/error/code) are a different, official envelope and stay allowed.
     */
    public List<String> errorPointerProblems(JsonNode caseFile) {
        List<String> problems=new ArrayList<>();
        for(JsonNode sub:caseFile.path("subcases")) for(JsonNode assertion:sub.path("assertions"))
            for(String field:List.of("source","baseline")) if(assertion.has(field)) {
                String pointer=assertion.path(field).path("pointer").asText();
                if(!pointer.equals("/response") && !pointer.startsWith("/response/")) continue;
                String last=pointer.substring(pointer.lastIndexOf('/')+1);
                if(pointer.equals("/response/code") || last.equals("errorCode") || last.equals("error_code"))
                    problems.add(caseFile.path("caseId").asText()+"/"+sub.path("id").asText()+"/"+assertion.path("id").asText()+": "+field+" "+pointer
                        +" is not the canonical error envelope /response/error/code (contracts/command-response.schema.json)");
            }
        return problems;
    }
    /**
     * A process control whose operation is not in the host-observation vocabulary (contracts/acceptance-host-observation.schema.json
     * operation enum) can never yield a valid host observation. The case schema allows any operation string, so preparation
     * rejects it here instead of reporting the subcase prepared and failing closed only at runtime.
     */
    public List<String> hostOperationProblems(JsonNode caseFile) throws IOException {
        Set<String> defined=new HashSet<>();
        for(JsonNode op:Json.read(path("contracts/acceptance-host-observation.schema.json")).path("properties").path("operation").path("enum")) defined.add(op.asText());
        List<String> problems=new ArrayList<>();
        for(JsonNode sub:caseFile.path("subcases")) {
            List<JsonNode> actions=new ArrayList<>();collect(sub.path("actions"),actions);
            for(JsonNode action:actions) if(action.path("kind").asText().equals("control") && action.path("control").path("type").asText().equals("process")
                    && !defined.contains(action.path("control").path("operation").asText()))
                problems.add(caseFile.path("caseId").asText()+"/"+sub.path("id").asText()+"/"+action.path("id").asText()+": process operation "
                    +action.path("control").path("operation").asText()+" is not defined in contracts/acceptance-host-observation.schema.json");
        }
        return problems;
    }
    /**
     * contracts/audit-observation-fields.json publishes the logical audit rows an independent observer returns under
     * /data/rawRows/audit (command audit) and /data/rawRows/queryAudit (query audit). An audit assertion that reads another
     * audit-like source, filters on a field that is not ALWAYS present, or projects/requires a field outside the contract
     * cannot be satisfied by a correct product, so preparation rejects it. Forbidding values (notEquals/absent) still has
     * to name published fields. Outcome and error-code values are checked by verification/cases/check_vocabulary.py.
     */
    public List<String> auditFieldProblems(JsonNode caseFile) throws IOException {
        JsonNode contract=Json.read(path("contracts/audit-observation-fields.json"));
        Map<String,Map<String,String>> published=new HashMap<>();
        for(JsonNode source:contract.path("sources")) published.put(source.path("source").asText(),new HashMap<>());
        for(JsonNode field:contract.path("fields")) published.computeIfAbsent(field.path("source").asText(),k->new HashMap<>()).put(field.path("name").asText(),field.path("presence").asText());
        List<String> problems=new ArrayList<>();
        String caseId=caseFile.path("caseId").asText();
        for(JsonNode sub:caseFile.path("subcases")) {
            List<JsonNode> actions=new ArrayList<>();collect(sub.path("actions"),actions);
            for(JsonNode action:actions) for(JsonNode source:action.path("observation").path("sources"))
                if(auditLike(source.asText()) && !published.containsKey(source.asText()))
                    problems.add(caseId+"/"+sub.path("id").asText()+"/"+action.path("id").asText()+": observe source "+source.asText()+" is not a published audit source "+published.keySet()+" (contracts/audit-observation-fields.json)");
            for(JsonNode assertion:sub.path("assertions")) for(String role:List.of("source","baseline","unitSource","baselineUnitSource")) {
                JsonNode src=assertion.path(role);String pointer=src.path("pointer").asText();
                if(!pointer.startsWith("/data/rawRows/")) continue;
                String[] segments=pointer.substring("/data/rawRows/".length()).split("/",-1);
                String name=segments[0].replace("~1","/").replace("~0","~");
                if(!auditLike(name)) continue;
                String where=caseId+"/"+sub.path("id").asText()+"/"+assertion.path("id").asText()+": "+role+" "+pointer;
                Map<String,String> fields=published.get(name);
                if(fields==null) {problems.add(where+" reads audit source "+name+", not one of "+published.keySet()+" (contracts/audit-observation-fields.json)");continue;}
                List<String> named=new ArrayList<>();
                if(segments.length>=3 && segments[1].matches("\\d+")) named.add(segments[2]);
                else if(segments.length>=2 && !segments[1].matches("\\d+")) named.add(segments[1]);
                if(src.path("field").isTextual()) named.add(src.path("field").asText());
                for(JsonNode f:src.path("field")) named.add(f.asText());
                if(role.equals("source") && assertion.path("op").asText().equals("fieldsPresent")) for(JsonNode f:assertion.path("expected")) named.add(f.asText());
                for(String f:named) if(!fields.containsKey(f)) problems.add(where+" names audit field "+f+", not a published "+name+" field");
                src.path("where").fieldNames().forEachRemaining(key->{
                    if(!fields.containsKey(key)) problems.add(where+" filters on "+key+", not a published "+name+" field");
                    else if(!fields.get(key).equals("ALWAYS")) problems.add(where+" filters on "+key+" ("+fields.get(key)+"); only ALWAYS audit fields may filter");
                });
            }
        }
        return problems;
    }
    private static boolean auditLike(String source) { return source.toLowerCase(Locale.ROOT).contains("audit"); }
    /**
     * Fixture runtimeProfile tick semantics (contracts/acceptance-fixture.schema.json, host-observation-guide.md):
     * controlledTicks=true with pausedUntilTickControl=true means the scheduler and sweeper loops submit nothing by themselves
     * and each harness tickScheduler/sweepDue request causes exactly that tick; controlledTicks=false with
     * pausedUntilTickControl=false means the loops run on their own tick and only a passive OBSERVE_NEXT_NATURAL_TICK watcher may
     * observe them. A fixture without runtimeProfile is a harness-tick fixture. A subcase that mixes the two cannot attribute a
     * submission, so preparation rejects it. The autonomous-loop
     * pattern is also fixed: the first passive watcher is the first action of the first branch of a parallel action whose
     * other branches each start exactly one process that is not running at that point (never restart), and such a loop
     * process is not running at any fault/seed/observation action between setup and that group (step2r round 5).
     */
    public List<String> runtimeProfileProblems(JsonNode caseFile) throws IOException {
        List<String> problems=new ArrayList<>();
        String caseId=caseFile.path("caseId").asText();
        for(JsonNode sub:caseFile.path("subcases")) {
            String where=caseId+"/"+sub.path("id").asText();
            List<JsonNode> all=new ArrayList<>();collect(sub.path("actions"),all);
            List<JsonNode> ticks=new ArrayList<>();
            for(JsonNode a:all) if(isProcess(a,"tickScheduler") || isProcess(a,"sweepDue")) ticks.add(a);
            if(ticks.isEmpty()) continue;
            JsonNode profile=runtimeProfile(sub.path("fixtureRef").asText(),new HashSet<>());
            boolean anyPassive=false,anyHarness=false;
            for(JsonNode t:ticks) {if(passive(t)) anyPassive=true; else anyHarness=true;}
            if(anyPassive && anyHarness) problems.add(where+": mixes harness tickScheduler/sweepDue with passive natural-tick observation; one fixture runtimeProfile cannot attribute both");
            // Without a runtimeProfile the fixture is a harness-tick fixture (controlledTicks=true, pausedUntilTickControl=true).
            if(profile==null) {if(anyPassive) problems.add(where+": passive natural-tick observation needs an explicit fixture baseline.runtimeProfile with controlledTicks=false and pausedUntilTickControl=false");continue;}
            boolean controlled=profile.path("controlledTicks").asBoolean(true),paused=profile.path("pausedUntilTickControl").asBoolean(true);
            if(anyHarness && !(controlled && paused)) problems.add(where+": harness tickScheduler/sweepDue requires runtimeProfile controlledTicks=true and pausedUntilTickControl=true");
            if(!anyPassive) continue;
            if(controlled || paused) problems.add(where+": passive natural-tick observation requires runtimeProfile controlledTicks=false and pausedUntilTickControl=false");
            JsonNode top=sub.path("actions");JsonNode group=null;int groupAt=-1;
            for(int i=0;i<top.size() && group==null;i++) {
                JsonNode a=top.get(i);
                if(a.path("kind").asText().equals("parallel")) {for(JsonNode b:a.path("branches")) for(JsonNode x:b.path("actions")) if(passive(x)) {group=a;groupAt=i;}}
                else if(passive(a)) {problems.add(where+"/"+a.path("id").asText()+": the first passive watcher must run inside a parallel action with the process starts (autonomous-loop pattern)");group=Json.object();}
            }
            if(group==null || groupAt<0) continue;
            JsonNode branches=group.path("branches");
            JsonNode watcher=branches.path(0).path("actions").path(0);
            if(!passive(watcher)) problems.add(where+"/"+group.path("id").asText()+": the watcher branch (first action OBSERVE_NEXT_NATURAL_TICK) must be branch 0");
            Map<String,String> last=new HashMap<>();
            for(int i=0;i<groupAt;i++) {List<JsonNode> before=new ArrayList<>();collect(Json.array().add(top.get(i)),before);
                for(JsonNode a:before) if(a.path("kind").asText().equals("control") && Set.of("start","stop","restart").contains(a.path("control").path("operation").asText()))
                    last.put(a.path("control").path("parameters").path("processId").asText(),a.path("control").path("operation").asText());}
            int starts=0;Set<String> loops=new LinkedHashSet<>();
            for(int b=1;b<branches.size();b++) {
                if(branches.get(b).path("actions").size()!=1) problems.add(where+"/"+group.path("id").asText()+": each non-watcher branch starts exactly one process; serialized starts in one branch use up the observation window");
                for(JsonNode a:branches.get(b).path("actions")) {
                    if(!isProcess(a,"start")) {problems.add(where+"/"+group.path("id").asText()+": a non-watcher branch may only start processes, found "+a.path("id").asText());continue;}
                    starts++;String id=a.path("control").path("parameters").path("processId").asText();loops.add(id);
                    if(Set.of("start","restart").contains(last.getOrDefault(id,"stop"))) problems.add(where+"/"+a.path("id").asText()+": process "+id+" is already running before the watcher starts; stop it before the clock advance");
                }
            }
            if(starts==0 || branches.size()<2) problems.add(where+"/"+group.path("id").asText()+": the autonomous-loop group needs at least one process start branch after the watcher");
            // A loop process started by the group must not run during the fault/seed/observation phase either: a free-running
            // loop would consume the seeded state (for example an UNLINKED intake) before before-db or before the watcher.
            Map<String,Boolean> running=new HashMap<>();Set<String> reported=new HashSet<>();
            for(int i=0;i<groupAt;i++) {List<JsonNode> before=new ArrayList<>();collect(Json.array().add(top.get(i)),before);
                for(JsonNode a:before) {
                    if(a.path("kind").asText().equals("parallel") || a.path("kind").asText().equals("installFixture")) continue;
                    String operation=a.path("control").path("operation").asText();
                    if(a.path("kind").asText().equals("control") && a.path("control").path("type").asText().equals("process") && Set.of("start","stop","restart").contains(operation)) {
                        running.put(a.path("control").path("parameters").path("processId").asText(),!operation.equals("stop"));continue;
                    }
                    for(String loop:loops) if(running.getOrDefault(loop,false) && reported.add(loop))
                        problems.add(where+"/"+a.path("id").asText()+": loop process "+loop+" (started by the watcher group) is running during the fault/seed phase; keep it stopped from setup until the autonomous-loop group");
                }
            }
            for(JsonNode a:all) if(isProcess(a,"restart")) problems.add(where+"/"+a.path("id").asText()+": a restart lets the loop submit before the watcher; split it into stop (before) and start (in the group)");
        }
        return problems;
    }
    /**
     * A $result-bound observe snapshotRef has exactly two meanings (harness-guide.md snapshot section): an API projection
     * revision (/response/snapshotRevision of an invoke/query/start; the observer gets RESULT_REVISION and recomputes it) or
     * the host terminal snapshot id of an awaitRuntimeTask control (/data/hostObservation/runtimeTask/snapshot/id; the
     * observer gets RUNTIME_TASK_SNAPSHOT and binds its read to that host artifact). Any other pointer would be sent as
     * RESULT_REVISION and could never be recomputed from rows, so a correct product could not pass it.
     */
    public List<String> snapshotRefProblems(JsonNode caseFile) {
        List<String> problems=new ArrayList<>();
        for(JsonNode sub:caseFile.path("subcases")) {
            String where=caseFile.path("caseId").asText()+"/"+sub.path("id").asText();
            List<JsonNode> all=new ArrayList<>();collect(sub.path("actions"),all);
            Map<String,JsonNode> byId=new HashMap<>();
            for(JsonNode a:all) {byId.put(a.path("id").asText(),a);if(a.has("call")) byId.put(a.path("call").path("id").asText(),a);}
            for(JsonNode a:all) {
                JsonNode ref=a.path("observation").path("snapshotRef");
                if(!a.path("kind").asText().equals("observe") || !ref.isObject() || !ref.has("$result")) continue;
                String pointer=ref.path("$result").path("pointer").asText();JsonNode issuing=byId.get(ref.path("$result").path("actionId").asText());
                boolean api=issuing!=null && Set.of("invoke","query","start").contains(issuing.path("kind").asText()) && pointer.equals(CaseRunner.API_REVISION_POINTER);
                boolean runtime=issuing!=null && CaseRunner.isRuntimeTaskAwait(issuing) && pointer.equals(CaseRunner.RUNTIME_SNAPSHOT_POINTER);
                if(!api && !runtime) problems.add(where+"/"+a.path("id").asText()+": snapshotRef "+ref.path("$result")+" is neither an API "+CaseRunner.API_REVISION_POINTER
                    +" of an invoke/query/start nor the "+CaseRunner.RUNTIME_SNAPSHOT_POINTER+" of an awaitRuntimeTask control");
            }
        }
        return problems;
    }
    private static boolean isProcess(JsonNode a,String operation) {
        return a.path("kind").asText().equals("control") && a.path("control").path("type").asText().equals("process") && a.path("control").path("operation").asText().equals(operation);
    }
    private static boolean passive(JsonNode a) {
        JsonNode p=a.path("control").path("parameters");
        return (isProcess(a,"tickScheduler") || isProcess(a,"sweepDue")) && (p.has("trigger") || p.has("triggeredBy") || p.has("observationWindowSeconds"));
    }
    private JsonNode runtimeProfile(String ref,Set<String> visiting) throws IOException {
        if(ref.isBlank() || !visiting.add(ref) || !Files.isRegularFile(path(ref))) return null;
        JsonNode fixture=Json.read(path(ref));
        if(fixture.path("baseline").has("runtimeProfile")) return fixture.path("baseline").path("runtimeProfile");
        for(JsonNode base:fixture.path("baseRefs")) {JsonNode found=runtimeProfile(base.asText(),visiting);if(found!=null) return found;}
        return null;
    }
    public Path root() { return root; }
    public Path path(String relative) {
        Path p=root.resolve(relative).normalize();
        if(!p.startsWith(root)) throw new IllegalArgumentException("Reference escapes repository: "+relative);
        return p;
    }
    public void schema(String file,JsonNode instance) throws IOException {
        JsonSchema s=JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(Json.read(path(file)));
        Set<ValidationMessage> errors=s.validate(instance);
        if(!errors.isEmpty()) throw new IllegalArgumentException("Schema invalid "+file+": "+errors);
    }
    public JsonNode caseFile(Path file) throws IOException {
        JsonNode c=Json.read(file); schema("contracts/acceptance-case.schema.json",c);
        boolean example=c.path("caseId").asText().startsWith("HARNESS-");
        Set<String> subs=new HashSet<>();
        for(JsonNode sub:c.path("subcases")) {
            require(subs.add(Json.required(sub,"id")),"Duplicate subcase ID");
            fixture(Json.required(sub,"fixtureRef"));
            Set<String> ids=new HashSet<>(); List<JsonNode> all=new ArrayList<>(); collect(sub.path("actions"),all);
            boolean uatOnly=requiresActualClient(sub);
            for(JsonNode a:all) { require(ids.add(Json.required(a,"id")),"Duplicate action ID"); checkAction(a,uatOnly); }
            Set<String> starts=new HashSet<>(), awaited=new HashSet<>();
            for(JsonNode a:all) if(a.path("kind").asText().equals("start")) starts.add(Json.required(a,"id"));
            for(JsonNode a:all) if(a.path("kind").asText().equals("await")) {
                String source=Json.required(a,"awaitActionId");
                require(starts.contains(source),"awaitActionId must reference start");
                require(awaited.add(source),"start must have exactly one terminal await");
            }
            require(awaited.equals(starts),"Every start requires terminal await, including parallel children");
            Set<String> assertionIds=new HashSet<>();
            for(JsonNode assertion:sub.path("assertions")) {
                require(assertionIds.add(Json.required(assertion,"id")),"Duplicate assertion ID");
                require(ids.contains(Json.required(assertion.path("source"),"actionId")),"Unknown assertion source");
                if(assertion.has("unitSource")) require(ids.contains(Json.required(assertion.path("unitSource"),"actionId")),"Unknown assertion unit source");
                if(assertion.has("baselineUnitSource")) require(ids.contains(Json.required(assertion.path("baselineUnitSource"),"actionId")),"Unknown assertion baseline unit source");
                if(assertion.has("baseline")) require(ids.contains(Json.required(assertion.path("baseline"),"actionId")),"Unknown assertion baseline");
                if(!example) require(assertion.has("oracleRef"),"Product assertion requires independent oracleRef");
                references(assertion,ids);
                for(String field:List.of("expected","scope")) identityReferences(assertion.path(field));
                for(String field:List.of("source","baseline","unitSource","baselineUnitSource")) identityReferences(assertion.path(field).path("where"));
                for(JsonNode ref:assertion.path("evidenceRefs")) require(!ref.asText().isBlank(),"Empty evidence requirement");
            }
            require(!sub.path("assertions").isEmpty(),"No substantive assertions");
            for(JsonNode action:all) references(action,ids);
        }
        return c;
    }
    private void references(JsonNode node,Set<String> ids) {
        if(node.isObject() && node.has("$result")) {
            require(node.size()==1,"Result reference allows only $result");JsonNode ref=node.path("$result");
            require(ref.isObject() && ref.size()==2 && ref.has("actionId") && ref.has("pointer"),"Result reference requires only actionId/pointer");
            require(ids.contains(Json.required(ref,"actionId")),"Unknown result reference action");
            require(Json.required(ref,"pointer").startsWith("/"),"Result reference must use RFC6901 pointer");
        } else if(node.isObject() && node.has("$transform")) {
            require(node.size()==1,"Transform reference allows only $transform");
            validateTransformDeclaration(node.path("$transform"));references(node.path("$transform").path("source"),ids);
        } else if(node.isObject() && node.has("$alias")) { require(node.size()==1 && node.path("$alias").isTextual(),"Alias reference requires only textual $alias"); }
        else if(node.isContainerNode()) node.forEach(n->references(n,ids));
    }
    private void identityReferences(JsonNode node) {
        if(node.isObject() && node.has("$transform")) require(false,"Assertion identity bindings cannot transform expected values");
        if(node.isObject() && node.has("$result")) require(ReferenceResolver.identityPointer(node.path("$result").path("pointer").asText()),"Assertion references must bind identity, not observed business values");
        else if(node.isContainerNode()) node.forEach(this::identityReferences);
    }
    private void validateTransformDeclaration(JsonNode t) {
        try {
            JsonNode schema=Json.read(path("contracts/acceptance-case.schema.json"));
            schema=((com.fasterxml.jackson.databind.node.ObjectNode)schema).deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)schema).put("$ref","#/$defs/transformReference");
            ((com.fasterxml.jackson.databind.node.ObjectNode)schema).remove(List.of("type","properties","required","additionalProperties"));
            Set<ValidationMessage> errors=JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schema).validate(Json.object().set("$transform",t));
            require(errors.isEmpty(),"Invalid bounded transform declaration: "+errors);
        } catch(IOException e) {throw new IllegalArgumentException(e);}
    }
    private void checkAction(JsonNode action,boolean uatOnly) {
        String kind=Json.required(action,"kind");
        if(Set.of("invoke","query").contains(kind) && !action.has("protocolOperation")) require(capabilities.contains(Json.required(action,"capabilityId")),"Unknown public capability "+action.path("capabilityId"));
        if(kind.equals("start")) { String child=action.path("call").path("kind").asText(); require(Set.of("invoke","query").contains(child),"start call must be invoke/query"); checkAction(action.path("call"),uatOnly); }
        if(kind.equals("agent")) {
            JsonNode context=action.path("permittedContext");
            for(String forbidden:List.of("expected","assertions","oracle","oracleRef","intent","capabilityId","slots"))
                require(!containsKey(context,forbidden),"Actual agent context contains planned answer/oracle: "+forbidden);
            JsonNode capability=action.path("intent").path("capabilityId");
            // SIT replays a declared typed intent; only UAT-only subcases may omit it.
            if(capability.isMissingNode()) require(uatOnly,"Agent action without scripted typed intent must belong to a UAT-only subcase (requiredAdapters client/model)");
            else require(capability.isTextual() && capabilities.contains(capability.asText()),"Unknown scripted agent capability "+capability);
        }
    }
    private static boolean containsKey(JsonNode node,String key) {
        if(node.isObject() && node.has(key)) return true;
        if(node.isContainerNode()) for(JsonNode child:node) if(containsKey(child,key)) return true;
        return false;
    }
    private static void collect(JsonNode actions,List<JsonNode> all) {
        for(JsonNode a:actions) { all.add(a); for(JsonNode b:a.path("branches")) collect(b.path("actions"),all); }
    }
    public JsonNode fixture(String ref) throws IOException {
        return fixture(ref,new HashSet<>());
    }
    private JsonNode fixture(String ref,Set<String> visiting) throws IOException {
        require(visiting.add(ref),"Fixture baseRefs cycle"); JsonNode fixture=Json.read(path(ref)); schema("contracts/acceptance-fixture.schema.json",fixture);
        for(JsonNode base:fixture.path("baseRefs")) fixture(base.asText(),visiting);
        visiting.remove(ref); return fixture;
    }
    public void result(StepResult result,String kind) throws IOException {
        schema("contracts/acceptance-driver.schema.json",result.toJson());
        if(result.driverStatus()!=StepResult.DriverStatus.EXECUTED) return;
        require(result.provenance().path("scopeComplete").asBoolean(false),"Executed action scope is incomplete");
        for(String artifact:result.artifactRefs()) require(Files.isRegularFile(path(artifact)),"Missing actual execution artifact "+artifact);
        if(kind.equals("observe")) {
            require(result.provenance().path("independent").asBoolean(false),"DB observation must be independent");
            schema("contracts/acceptance-observation.schema.json",result.data());
            require(!result.provenance().path("sourceQuery").isNull() && !result.provenance().path("snapshot").isNull(),"Observer query/snapshot provenance missing");
        }
        if(kind.equals("control")) require(result.data()!=null && result.data().path("acknowledged").asBoolean(false),"Control needs actual ACK");
        if(kind.equals("start")) require(result.data()!=null && result.data().hasNonNull("invocationHandle"),"Async submission needs actual invocationHandle ACK");
    }
    static void require(boolean ok,String message) { if(!ok) throw new IllegalArgumentException(message); }
}
