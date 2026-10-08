package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
            for(JsonNode t:ticks) for(String harnessOwned:List.of(HostObservationValidator.OBSERVE_FROM,HostObservationValidator.NATURAL_TICK_SECONDS)) if(t.path("control").path("parameters").has(harnessOwned))
                problems.add(where+"/"+t.path("id").asText()+": "+harnessOwned+" is resolved by the harness (CaseRunner) from the observation boundary and the fixture runtimeProfile; a case does not author it");
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
            // step2r round 7: a NO_TASK watcher must observe at least one natural tick, so the period has to fit every window.
            JsonNode tickSeconds=profile.path("tickSeconds");
            for(JsonNode t:ticks) if(passive(t)) {
                JsonNode window=t.path("control").path("parameters").path("observationWindowSeconds");
                if(!tickSeconds.isIntegralNumber() || tickSeconds.asInt()<1 || window.isIntegralNumber() && tickSeconds.asInt()>window.asInt())
                    problems.add(where+"/"+t.path("id").asText()+": passive natural-tick observation needs runtimeProfile tickSeconds, an integer 1..observationWindowSeconds, found "+tickSeconds);
            }
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
    /**
     * contracts/fixture-place-kinds.json (step2r round 6). A product treats only INTERNAL_STORAGE under an internal custodian as
     * confirmed custody, TRANSIT/CUSTOMER/SUPPLIER/EXTERNAL_* as outside custody (a known 0) and any other kind as unknown,
     * so a fixture Place without a contract kind makes its eligibility oracles depend on an uncontracted adapter mapping.
     * Every Place alias of a subcase fixture (and its baseRefs) therefore needs a contract kind; a kind outside the vocabulary
     * is accepted only as the declared negative control (kindControl=UNRECOGNIZED_PLACE_KIND, not an EXTERNAL_* kind).
     * baseline.places repeats the alias kind. A segment located (alias or baseline.segments, locationAlias/placeAlias) at
     * INTERNAL_STORAGE names an internal custodian: a Human/Agent alias of the place's organization.
     */
    public List<String> placeKindProblems(JsonNode caseFile) throws IOException {
        JsonNode contract=Json.read(path("contracts/fixture-place-kinds.json"));
        Set<String> kinds=new HashSet<>(),internal=new HashSet<>();
        for(JsonNode k:contract.path("kinds")) kinds.add(Json.required(k,"kind"));
        for(JsonNode t:contract.path("internalCustodianAliasTypes")) internal.add(t.asText());
        String controlField=contract.path("unrecognizedKindControl").path("field").asText(),controlValue=contract.path("unrecognizedKindControl").path("value").asText();
        List<String> problems=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(JsonNode sub:caseFile.path("subcases")) {
            Deque<String> refs=new ArrayDeque<>();refs.add(sub.path("fixtureRef").asText());
            while(!refs.isEmpty()) {
                String ref=refs.pop();
                if(ref.isBlank() || !seen.add(ref) || !Files.isRegularFile(path(ref))) continue;
                JsonNode fixture=Json.read(path(ref));JsonNode aliases=fixture.path("aliases");
                for(JsonNode base:fixture.path("baseRefs")) refs.add(base.asText());
                String where=caseFile.path("caseId").asText()+" "+ref;
                for(var it=aliases.fields();it.hasNext();) {
                    var e=it.next();JsonNode a=e.getValue();
                    if(!a.path("type").asText().equals("Place")) continue;
                    String kind=a.path("kind").asText(null);
                    if(a.has(controlField)) {
                        if(!controlValue.equals(a.path(controlField).asText()) || kind==null || kinds.contains(kind) || kind.startsWith("EXTERNAL_"))
                            problems.add(where+": Place "+e.getKey()+" "+controlField+" must be "+controlValue+" on a kind outside the vocabulary (and not EXTERNAL_*), found "+a.path(controlField)+"/"+kind);
                    } else if(kind==null) problems.add(where+": Place "+e.getKey()+" has no kind; use one of "+new TreeSet<>(kinds)+" (contracts/fixture-place-kinds.json)");
                    else if(!kinds.contains(kind)) problems.add(where+": Place "+e.getKey()+" kind "+kind+" is not in the fixture place vocabulary "+new TreeSet<>(kinds)+" (contracts/fixture-place-kinds.json)");
                }
                for(JsonNode place:fixture.path("baseline").path("places")) {
                    JsonNode a=aliases.path(place.path("alias").asText());
                    if(!a.path("type").asText().equals("Place")) problems.add(where+": baseline.places "+place.path("alias").asText()+" is not a Place alias");
                    else if(place.has("kind") && !place.path("kind").equals(a.path("kind"))) problems.add(where+": baseline.places "+place.path("alias").asText()+" kind "+place.path("kind").asText()+" differs from the alias kind "+a.path("kind").asText());
                }
                Map<String,List<JsonNode>> segments=new LinkedHashMap<>();
                for(var it=aliases.fields();it.hasNext();) {var e=it.next();if(e.getValue().path("type").asText().equals("QuantitySegment")) segments.computeIfAbsent(e.getKey(),k->new ArrayList<>()).add(e.getValue());}
                for(JsonNode s:fixture.path("baseline").path("segments")) if(s.path("alias").isTextual()) segments.computeIfAbsent(s.path("alias").asText(),k->new ArrayList<>()).add(s);
                for(var e:segments.entrySet()) {
                    String location=null,custodian=null;
                    for(JsonNode r:e.getValue()) {
                        for(String key:List.of("locationAlias","placeAlias")) if(location==null && r.path(key).isTextual()) location=r.path(key).asText();
                        if(custodian==null && r.path("custodianAlias").isTextual()) custodian=r.path("custodianAlias").asText();
                    }
                    if(custodian!=null && !aliases.has(custodian)) problems.add(where+": segment "+e.getKey()+" custodianAlias "+custodian+" is not a fixture alias");
                    JsonNode place=location==null?null:aliases.get(location);
                    if(place==null || !place.path("type").asText().equals("Place") || !"INTERNAL_STORAGE".equals(place.path("kind").asText()) || place.has(controlField)) continue;
                    JsonNode holder=custodian==null?null:aliases.get(custodian);
                    if(holder==null || !internal.contains(holder.path("type").asText()))
                        problems.add(where+": segment "+e.getKey()+" at INTERNAL_STORAGE "+location+" needs an internal custodian (a "+internal+" alias), found "+(custodian==null?"none":custodian+" "+(holder==null?"":holder.path("type").asText())));
                    else if(place.has("organizationAlias") && holder.has("organizationAlias") && !place.path("organizationAlias").equals(holder.path("organizationAlias")))
                        problems.add(where+": segment "+e.getKey()+" custodian "+custodian+" belongs to "+holder.path("organizationAlias").asText()+", not to the organization of "+location);
                }
            }
        }
        return problems;
    }
    /**
     * contracts/fixture-place-kinds.json directReceiptCustody (step2r round 7). A confirmReceipt whose slots name no fixture
     * QuantitySegment is a direct receipt: there is no transit leaf whose custody it keeps, so the product (ReceiptCommands)
     * records the received stock with no custodian unless the receivingCustodianId slot names one, and then reserve/pick/
     * dispatch/move of it is SCOPE_INELIGIBLE. When such stock is used later (directly or through a split/move/hold/reserve
     * result derived from it), the receipt must name an internal Human/Agent fixture actor with confirmReceipt authority for
     * the place, the cited receipt original must name the same alias (fixtureContent.receivingCustodianAlias, hashed by its
     * evidence row), and every confirm of the same receipt carries the same slot. A transit receipt never carries the slot.
     */
    public List<String> receiptCustodyProblems(JsonNode caseFile) throws IOException {
        JsonNode contract=Json.read(path("contracts/fixture-place-kinds.json"));JsonNode rule=contract.path("directReceiptCustody");
        String capability=Json.required(rule,"capability"),slot=Json.required(rule,"slot"),field=Json.required(rule,"originalField");
        Set<String> internal=new HashSet<>(),usedBy=new HashSet<>(),derived=new HashSet<>();
        for(JsonNode t:contract.path("internalCustodianAliasTypes")) internal.add(t.asText());
        for(JsonNode t:rule.path("requiredWhenStockIsUsedBy")) usedBy.add(t.asText());
        for(JsonNode t:rule.path("stockDerivedThrough")) derived.add(t.asText());
        List<String> problems=new ArrayList<>();
        for(JsonNode sub:caseFile.path("subcases")) {
            String where=caseFile.path("caseId").asText()+"/"+sub.path("id").asText();
            String ref=sub.path("fixtureRef").asText();
            if(ref.isBlank() || !Files.isRegularFile(path(ref))) continue;
            ObjectNode aliases=Json.object(),actors=Json.object();ArrayNode evidence=Json.array();
            mergeFixture(ref,new HashSet<>(),aliases,actors,evidence);
            List<JsonNode> all=new ArrayList<>();collect(sub.path("actions"),all);
            List<JsonNode> actions=new ArrayList<>();for(JsonNode a:all) {actions.add(a);if(a.has("call")) actions.add(a.path("call"));}
            Map<String,Set<String>> origins=new HashMap<>();Map<String,JsonNode> receipts=new LinkedHashMap<>();Map<String,List<String>> users=new LinkedHashMap<>();
            for(JsonNode a:actions) {
                String id=a.path("id").asText(),cap=a.path("capabilityId").asText(a.path("request").path("capabilityId").asText());
                Set<String> from=new TreeSet<>();for(String r:resultRefs(a.path("request"))) from.addAll(origins.getOrDefault(r,Set.of()));
                if(cap.equals(capability)) {
                    Set<String> segments=new TreeSet<>();for(String x:aliasRefs(a.path("request").path("slots"))) if(aliases.path(x).path("type").asText().equals("QuantitySegment")) segments.add(x);
                    String custodian=slotAlias(a.path("request").path("slots").path(slot));
                    if(!segments.isEmpty()) {if(a.path("request").path("slots").has(slot)) problems.add(where+"/"+id+": transit receipt of "+segments+" carries "+slot+"; a transit receipt keeps the leaf's custodian (contracts/fixture-place-kinds.json directReceiptCustody)");continue;}
                    receipts.put(id,a);origins.put(id,new TreeSet<>(Set.of(id)));
                    if(custodian!=null || a.path("request").path("slots").has(slot)) custodianProblems(where,a,custodian,slot,field,internal,aliases,actors,evidence,problems);
                    continue;
                }
                if(from.isEmpty()) continue;
                if(usedBy.contains(cap)) for(String r:from) users.computeIfAbsent(r,k->new ArrayList<>()).add(id+" "+cap);
                if(derived.contains(cap)) origins.put(id,from);
            }
            for(var e:users.entrySet()) if(!receipts.get(e.getKey()).path("request").path("slots").has(slot))
                problems.add(where+"/"+e.getKey()+": direct receipt (no existing fixture QuantitySegment) is later used by "+e.getValue()+" but names no "+slot
                    +"; a conforming product records the stock without custodian and rejects that use as SCOPE_INELIGIBLE (contracts/fixture-place-kinds.json directReceiptCustody)");
            Map<String,Set<String>> perReceipt=new LinkedHashMap<>();
            for(JsonNode a:actions) {
                if(!a.path("capabilityId").asText(a.path("request").path("capabilityId").asText()).equals(capability)) continue;
                JsonNode slots=a.path("request").path("slots");String key=plainText(slots.path("canonicalOccurrenceKey"));
                if(key==null) key="idempotency:"+a.path("request").path("commandIdempotencyKey").asText();
                String custodian=slots.has(slot)?String.valueOf(slotAlias(slots.path(slot))):"(none)";
                perReceipt.computeIfAbsent(key,k->new TreeSet<>()).add(custodian);
            }
            for(var e:perReceipt.entrySet()) if(e.getValue().size()>1) problems.add(where+": confirmReceipt of the same receipt "+e.getKey()+" carries different "+slot+" values "+e.getValue()+"; every confirm/retry names the same custodian");
        }
        return problems;
    }
    private void custodianProblems(String where,JsonNode receipt,String custodian,String slot,String field,Set<String> internal,JsonNode aliases,JsonNode actors,JsonNode evidence,List<String> problems) throws IOException {
        String id=where+"/"+receipt.path("id").asText();
        if(custodian==null) {problems.add(id+": "+slot+" must name a fixture alias ({\"$alias\":...} or {\"value\":{\"$alias\":...}})");return;}
        JsonNode holder=aliases.get(custodian);
        if(holder==null || !internal.contains(holder.path("type").asText())) {problems.add(id+": "+slot+" "+custodian+" is not an internal custodian (a "+internal+" alias)");return;}
        JsonNode actor=actors.get(custodian);
        boolean authority=false;
        if(actor!=null) {
            boolean role=false,grant=false;
            for(JsonNode c:actor.path("roleCapabilities")) role|=c.asText().equals("confirmReceipt");
            for(JsonNode c:actor.path("grant").path("actions")) grant|=c.asText().equals("confirmReceipt");
            authority=role && grant;
        }
        if(!authority) problems.add(id+": "+slot+" "+custodian+" has no confirmReceipt role and grant; the product requires the receiving custodian's current receive authority");
        String place=null;JsonNode slots=receipt.path("request").path("slots");
        for(String key:List.of("locationId","placeId","destinationId")) if(place==null) place=slotAlias(slots.path(key));
        if(actor!=null && place!=null) for(String key:List.of("placeAliases","places")) {
            JsonNode scope=actor.path("grant").path("scope").path(key);
            if(!scope.isArray()) continue;
            boolean listed=false;for(JsonNode p:scope) listed|=p.asText().equals(place);
            if(!listed) problems.add(id+": "+slot+" "+custodian+" grant scope "+key+" does not include the receipt place "+place);
        }
        Set<String> docs=new LinkedHashSet<>();
        String cited=slotAlias(slots.path("evidenceId"));if(cited!=null) docs.add(cited);
        for(JsonNode e:receipt.path("request").path("evidenceRefs")) {String x=e.isTextual()?e.asText():slotAlias(e);if(x!=null) docs.add(x);}
        boolean named=false;
        for(String doc:docs) {
            JsonNode a=aliases.path(doc);
            if(!a.path("type").asText().equals("DocumentVersion") || !a.path("fixtureContent").has(field)) continue;
            String stated=a.path("fixtureContent").path(field).asText();
            if(!stated.equals(custodian)) {problems.add(id+": receipt original "+doc+" names receiving custodian "+stated+", the slot "+custodian);continue;}
            named=true;
            String expected=Json.sha256Text(CANONICAL.writeValueAsString(Json.MAPPER.treeToValue(a.path("fixtureContent"),Object.class)));
            boolean hashed=false;for(JsonNode row:evidence) if(row.path("alias").asText().equals(doc)) hashed|=row.path("sha256").asText().equals(expected);
            if(!hashed) problems.add(id+": fixture evidence sha256 of "+doc+" is not the SHA-256 of its canonical fixtureContent, so the original naming "+custodian+" is not the hashed one");
        }
        if(!named) problems.add(id+": no receipt original it cites ("+docs+") names "+custodian+" in fixtureContent."+field+"; the product accepts "+slot+" only when verified receipt evidence names it");
    }
    private static final com.fasterxml.jackson.databind.ObjectMapper CANONICAL=new com.fasterxml.jackson.databind.ObjectMapper().configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true);
    private void mergeFixture(String ref,Set<String> visiting,ObjectNode aliases,ObjectNode actors,ArrayNode evidence) throws IOException {
        if(ref.isBlank() || !visiting.add(ref) || !Files.isRegularFile(path(ref))) return;
        JsonNode fixture=Json.read(path(ref));
        for(JsonNode base:fixture.path("baseRefs")) mergeFixture(base.asText(),visiting,aliases,actors,evidence);
        aliases.setAll((ObjectNode)(fixture.path("aliases").isObject()?fixture.path("aliases"):Json.object()));
        actors.setAll((ObjectNode)(fixture.path("actors").isObject()?fixture.path("actors"):Json.object()));
        evidence.addAll((ArrayNode)(fixture.path("evidence").isArray()?fixture.path("evidence"):Json.array()));
    }
    /** The alias a slot names: {"$alias":X} or the typed {"value":{"$alias":X}}; null otherwise. */
    private static String slotAlias(JsonNode slot) {
        if(slot.path("$alias").isTextual()) return slot.path("$alias").asText();
        if(slot.path("value").path("$alias").isTextual()) return slot.path("value").path("$alias").asText();
        return null;
    }
    private static String plainText(JsonNode slot) {
        if(slot.isTextual()) return slot.asText();
        if(slot.path("value").isTextual()) return slot.path("value").asText();
        return null;
    }
    private static Set<String> resultRefs(JsonNode node) {Set<String> out=new TreeSet<>();walk(node,"$result",out);return out;}
    private static Set<String> aliasRefs(JsonNode node) {Set<String> out=new TreeSet<>();walk(node,"$alias",out);return out;}
    private static void walk(JsonNode node,String key,Set<String> out) {
        if(node.isObject()) {
            if(key.equals("$result") && node.path("$result").path("actionId").isTextual()) out.add(node.path("$result").path("actionId").asText());
            if(key.equals("$alias") && node.path("$alias").isTextual()) out.add(node.path("$alias").asText());
            for(JsonNode v:node) walk(v,key,out);
        } else if(node.isArray()) for(JsonNode v:node) walk(v,key,out);
    }
    /** The Accept every Streamable HTTP request sends (contracts/mcp/s0-protocol.md "독립 요청"). */
    public static final String MCP_ACCEPT="application/json, text/event-stream";
    /** The media types a Streamable HTTP Accept must list (contracts/mcp/s0-protocol.md 406 row). */
    static final Set<String> MCP_ACCEPT_TYPES=Set.of("application/json","text/event-stream");
    /**
     * Whether an Accept header lists both MCP media types (step2r round 7): media ranges are compared case-insensitively
     * without their parameters, in any order and with other ranges beside them. A range whose q parameter is 0 is
     * "not acceptable" (RFC 9110 §12.4.2) and does not count, and a wildcard range does not list a specific type.
     */
    static boolean acceptsMcp(String accept) {
        if(accept==null) return false;
        Set<String> listed=new HashSet<>();
        for(String range:accept.split(",")) {
            String[] parts=range.split(";");String type=parts[0].strip().toLowerCase(Locale.ROOT);boolean refused=false;
            for(int i=1;i<parts.length;i++) {String[] kv=parts[i].split("=",2);
                if(kv.length==2 && kv[0].strip().equalsIgnoreCase("q") && kv[1].strip().matches("0(\\.0{0,3})?")) refused=true;}
            if(!refused) listed.add(type);
        }
        return listed.containsAll(MCP_ACCEPT_TYPES);
    }
    /**
     * contracts/mcp/s0-protocol.md: a Streamable HTTP request whose Accept does not list both application/json and
     * text/event-stream (media-type comparison, acceptsMcp) is answered 406 and a request with an Origin outside the allowlist 403, before any JSON-RPC processing, and the relative order of
     * these transport rejections and the other error rows is not fixed. No contract or fixture declares an allowed Origin
     * (an Origin-less request is accepted). A wire request that omits/changes Accept or sends an Origin therefore makes every
     * other expectation of its subcase unsatisfiable for a conforming server, unless it is the transport negative itself:
     * the subcase pins that action's /response/httpStatus to 406 (Accept) or 403 (Origin), and a request breaks only one of
     * the two headers so the expected status is unambiguous.
     */
    public List<String> wireTransportProblems(JsonNode caseFile) {
        List<String> problems=new ArrayList<>();
        for(JsonNode sub:caseFile.path("subcases")) {
            String where=caseFile.path("caseId").asText()+"/"+sub.path("id").asText();
            List<JsonNode> all=new ArrayList<>();collect(sub.path("actions"),all);
            List<JsonNode> requests=new ArrayList<>();
            for(JsonNode a:all) {requests.add(a);if(a.has("call")) requests.add(a.path("call"));}
            for(JsonNode a:requests) {
                JsonNode request=a.path("request");
                if(!a.path("route").asText().equals("wire") || !request.path("transport").asText().equals("streamable-http")) continue;
                String accept=null;boolean origin=false;
                for(var it=request.path("headers").fields();it.hasNext();) {var h=it.next();String name=h.getKey().toLowerCase(Locale.ROOT);
                    if(name.equals("accept")) accept=h.getValue().asText();
                    if(name.equals("origin")) origin=true;}
                boolean badAccept=!acceptsMcp(accept);
                String id=a.path("id").asText();
                if(badAccept && origin) {problems.add(where+"/"+id+": a Streamable HTTP request breaks both Accept and Origin; the expected transport rejection (406 or 403) is ambiguous");continue;}
                if(!badAccept && !origin) continue;
                int status=badAccept?406:403;
                if(!pinsHttpStatus(sub,id,status)) problems.add(where+"/"+id+": Streamable HTTP request "+(badAccept?"whose Accept does not list both "+new TreeSet<>(MCP_ACCEPT_TYPES):"with an Origin no contract allowlists")
                    +" is answered "+status+" before JSON-RPC processing; send the required Accept and no Origin, or pin /response/httpStatus equals "+status+" as the transport negative (contracts/mcp/s0-protocol.md)");
            }
        }
        return problems;
    }
    private static boolean pinsHttpStatus(JsonNode sub,String actionId,int status) {
        for(JsonNode assertion:sub.path("assertions")) if(assertion.path("op").asText().equals("equals") && assertion.path("source").path("actionId").asText().equals(actionId)
                && assertion.path("source").path("pointer").asText().equals("/response/httpStatus") && assertion.path("expected").isIntegralNumber() && assertion.path("expected").asInt()==status) return true;
        return false;
    }
    private static boolean isProcess(JsonNode a,String operation) {
        return a.path("kind").asText().equals("control") && a.path("control").path("type").asText().equals("process") && a.path("control").path("operation").asText().equals(operation);
    }
    private static boolean passive(JsonNode a) {
        JsonNode p=a.path("control").path("parameters");
        return (isProcess(a,"tickScheduler") || isProcess(a,"sweepDue")) && (p.has("trigger") || p.has("triggeredBy") || p.has("observationWindowSeconds"));
    }
    /** The fixture baseline.runtimeProfile of a fixture or its baseRefs, or null (a harness-tick fixture). */
    public JsonNode runtimeProfileOf(String ref) throws IOException {return runtimeProfile(ref,new HashSet<>());}
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
