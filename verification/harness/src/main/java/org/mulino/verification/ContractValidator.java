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
            // step2r rounds 7-8: a NO_TASK watcher must observe two natural ticks, so two periods have to fit every window.
            JsonNode tickSeconds=profile.path("tickSeconds");
            for(JsonNode t:ticks) if(passive(t)) {
                JsonNode window=t.path("control").path("parameters").path("observationWindowSeconds");
                if(!tickSeconds.isIntegralNumber() || tickSeconds.asInt()<1 || window.isIntegralNumber() && HostObservationValidator.NO_TASK_TICKS*tickSeconds.asLong()>window.asLong())
                    problems.add(where+"/"+t.path("id").asText()+": passive natural-tick observation needs runtimeProfile tickSeconds, an integer 1..observationWindowSeconds/"+HostObservationValidator.NO_TASK_TICKS+", found "+tickSeconds);
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
     * contracts/fixture-place-kinds.json transitReceipt and directReceiptCustody (step2r rounds 7 and 8). A confirmReceipt that
     * names a fixture QuantitySegment (or the $result child of a splitQuantity of one) is a transit receipt. The product
     * (ReceiptStockPrimitives.receive) accepts it only for one identified leaf at a TRANSIT place with the same item, LOT and
     * unit and exactly the received quantity, into INTERNAL_STORAGE (partial cargo is split first); the received stock keeps
     * the leaf custodian, which must then be internal when the stock is used. Any other confirmReceipt is a direct receipt:
     * the product records the received stock with no custodian unless the receivingCustodianId slot names one, and then
     * reserve/pick/dispatch/move of it is SCOPE_INELIGIBLE. When such stock is used later (directly or through a split/move/
     * hold/reserve result derived from it), the receipt must name an internal Human/Agent fixture actor of the confirming
     * actor's organization with confirmReceipt authority for the place, a cited receipt original must name the same alias
     * (DocumentVersion fixtureContent.receivingCustodianAlias hashed by its evidence row, or an inline JSON document attached
     * at runtime) and every confirm of the same receipt carries the same slot. A declared custody negative (custodyControl)
     * instead names the product's first failing check, pins the outcome and error code, and proves zero effect.
     */
    public List<String> receiptCustodyProblems(JsonNode caseFile) throws IOException {
        JsonNode contract=Json.read(path("contracts/fixture-place-kinds.json"));JsonNode rule=contract.path("directReceiptCustody"),transit=contract.path("transitReceipt");
        String capability=Json.required(rule,"capability"),slot=Json.required(rule,"slot"),field=Json.required(rule,"originalField");
        String controlField=Json.required(rule.path("custodyControl"),"field"),splitCapability=Json.required(transit,"splitCapability");
        Map<String,String> controls=new LinkedHashMap<>();rule.path("custodyControl").path("values").fields().forEachRemaining(e->controls.put(e.getKey(),e.getValue().asText()));
        List<String> basis=new ArrayList<>();for(JsonNode b:rule.path("verificationBasisSlots")) basis.add(b.asText());
        require(!basis.isEmpty(),"contracts/fixture-place-kinds.json directReceiptCustody.verificationBasisSlots is empty");
        Set<String> internal=new HashSet<>(),usedBy=new HashSet<>(),derived=new HashSet<>();
        for(JsonNode t:contract.path("internalCustodianAliasTypes")) internal.add(t.asText());
        for(JsonNode t:rule.path("requiredWhenStockIsUsedBy")) usedBy.add(t.asText());
        for(JsonNode t:rule.path("stockDerivedThrough")) derived.add(t.asText());
        List<String> problems=new ArrayList<>();
        for(JsonNode sub:caseFile.path("subcases")) {
            String where=caseFile.path("caseId").asText()+"/"+sub.path("id").asText();
            List<JsonNode> all=new ArrayList<>();collect(sub.path("actions"),all);
            List<JsonNode> actions=new ArrayList<>();for(JsonNode a:all) {actions.add(a);if(a.has("call")) actions.add(a.path("call"));}
            for(JsonNode a:actions) if(a.has(controlField) && !capability.equals(a.path("capabilityId").asText(a.path("request").path("capabilityId").asText())))
                problems.add(where+"/"+a.path("id").asText()+": "+controlField+" applies only to a "+capability+" action");
            String ref=sub.path("fixtureRef").asText();
            if(ref.isBlank() || !Files.isRegularFile(path(ref))) continue;
            ObjectNode aliases=Json.object(),actors=Json.object();ArrayNode evidence=Json.array();Map<String,ObjectNode> leaves=new LinkedHashMap<>();
            mergeFixture(ref,new HashSet<>(),aliases,actors,evidence);mergeSegments(ref,new HashSet<>(),leaves);
            for(var it=aliases.fields();it.hasNext();) {var e=it.next();if(e.getValue().path("type").asText().equals("QuantitySegment")) {ObjectNode leaf=e.getValue().deepCopy();JsonNode row=leaves.get(e.getKey());
                if(row!=null) for(var f=row.fields();f.hasNext();) {var x=f.next();if(!leaf.has(x.getKey())) leaf.set(x.getKey(),x.getValue());}
                leaves.put(e.getKey(),leaf);}}
            leaves.keySet().removeIf(k->!aliases.path(k).path("type").asText().equals("QuantitySegment"));
            Map<String,JsonNode> byId=new HashMap<>();for(JsonNode a:actions) byId.putIfAbsent(a.path("id").asText(),a);
            Map<String,ObjectNode> children=new HashMap<>();
            Map<String,Set<String>> origins=new HashMap<>();Map<String,JsonNode> receipts=new LinkedHashMap<>();Map<String,ObjectNode> transitLeaf=new HashMap<>();Map<String,List<String>> users=new LinkedHashMap<>();
            for(JsonNode a:actions) {
                String id=a.path("id").asText(),cap=a.path("capabilityId").asText(a.path("request").path("capabilityId").asText());
                JsonNode slots=a.path("request").path("slots");
                Set<String> from=new TreeSet<>();for(String r:resultRefs(a.path("request"))) from.addAll(origins.getOrDefault(r,Set.of()));
                if(cap.equals(splitCapability)) splitChildren(id,slots,leaves,children);
                if(cap.equals(capability)) {
                    Map<String,ObjectNode> named=new LinkedHashMap<>();
                    for(String x:aliasRefs(slots)) if(leaves.containsKey(x)) named.put(x,leaves.get(x));
                    for(JsonNode r:resultNodes(slots)) {
                        JsonNode source=byId.get(r.path("actionId").asText());
                        if(source==null || !splitCapability.equals(source.path("capabilityId").asText(source.path("request").path("capabilityId").asText()))) continue;
                        String key=r.path("actionId").asText()+r.path("pointer").asText();
                        if(children.containsKey(key)) named.put(key,children.get(key));
                        else if(splitSource(source.path("request").path("slots"),leaves,children)!=null)
                            problems.add(where+"/"+id+": transit receipt names "+key+", which is not a "+transit.path("splitChildPointer").asText()+" of an explicit children entry of that split; the received leaf quantity cannot be checked");
                    }
                    if(!named.isEmpty()) {
                        if(slots.has(slot)) problems.add(where+"/"+id+": transit receipt of "+named.keySet()+" carries "+slot+"; a transit receipt keeps the leaf's custodian (contracts/fixture-place-kinds.json directReceiptCustody)");
                        if(a.has(controlField)) problems.add(where+"/"+id+": "+controlField+" declares a direct-receipt custody negative on a transit receipt");
                        if(named.size()!=1) problems.add(where+"/"+id+": transit receipt names "+named.keySet()+"; the product consumes exactly one identified leaf");
                        else {var e=named.entrySet().iterator().next();transitProblems(where+"/"+id,e.getKey(),e.getValue(),slots,aliases,transit,problems);transitLeaf.put(id,e.getValue());}
                        receipts.put(id,a);origins.put(id,new TreeSet<>(Set.of(id)));
                        continue;
                    }
                    receipts.put(id,a);origins.put(id,new TreeSet<>(Set.of(id)));
                    if(a.has(controlField) && !slots.has(slot)) problems.add(where+"/"+id+": "+controlField+" needs the "+slot+" slot it refutes");
                    if(a.has(controlField) || slots.has(slot)) custodianProblems(where,sub,a,actions,slot,field,basis,internal,aliases,actors,evidence,byId,controlField,controls,problems);
                    continue;
                }
                if(from.isEmpty()) continue;
                if(usedBy.contains(cap)) for(String r:from) users.computeIfAbsent(r,k->new ArrayList<>()).add(id+" "+cap);
                if(derived.contains(cap)) origins.put(id,from);
            }
            for(var e:users.entrySet()) {
                JsonNode receipt=receipts.get(e.getKey());
                if(transitLeaf.containsKey(e.getKey())) {
                    String holder=transitLeaf.get(e.getKey()).path("custodianAlias").asText(null);
                    if(holder==null || !internal.contains(aliases.path(holder).path("type").asText()))
                        problems.add(where+"/"+e.getKey()+": transit receipt keeps the leaf custodian "+holder+", which is not an internal custodian, but its stock is used by "+e.getValue());
                } else if(receipt.has(controlField)) problems.add(where+"/"+e.getKey()+": declared custody negative "+receipt.path(controlField).asText()+" creates no stock, but its result is used by "+e.getValue());
                else if(!receipt.path("request").path("slots").has(slot))
                    problems.add(where+"/"+e.getKey()+": direct receipt (no existing fixture QuantitySegment) is later used by "+e.getValue()+" but names no "+slot
                        +"; a conforming product records the stock without custodian and rejects that use as SCOPE_INELIGIBLE (contracts/fixture-place-kinds.json directReceiptCustody)");
            }
            Map<String,Set<String>> perReceipt=new LinkedHashMap<>();
            for(JsonNode a:actions) {
                if(!a.path("capabilityId").asText(a.path("request").path("capabilityId").asText()).equals(capability) || a.has(controlField)) continue;
                JsonNode slots=a.path("request").path("slots");String key=plainText(slots.path("canonicalOccurrenceKey"));
                if(key==null) key="idempotency:"+a.path("request").path("commandIdempotencyKey").asText();
                String custodian=slots.has(slot)?String.valueOf(slotAlias(slots.path(slot))):"(none)";
                perReceipt.computeIfAbsent(key,k->new TreeSet<>()).add(custodian);
            }
            for(var e:perReceipt.entrySet()) if(e.getValue().size()>1) problems.add(where+": confirmReceipt of the same receipt "+e.getKey()+" carries different "+slot+" values "+e.getValue()+"; every confirm/retry names the same custodian");
        }
        return problems;
    }
    /** Fulfilment capabilities whose order the product enforces (FulfillmentCommands, step2r round 9). */
    static final String PICK="pickQuantity",DISPATCH="dispatchQuantity";
    /**
     * Pick before dispatch (step2r rounds 9-10, Step 2 closure reviews 6 and 7). The product (FulfillmentCommands.prepare and
     * FulfillmentStockPrimitives.dispatch, read only) rejects dispatchQuantity with INVALID 'Pick before dispatch required'
     * when the allocation has no pickedAt; only pickQuantity (FulfillmentStockPrimitives.pick) sets it, rejects a second pick
     * ('Allocation already picked') and increments the allocation revision. The adapter never creates a pick (harness-guide.md).
     * For every dispatchQuantity of a subcase:
     * - a fixture allocation ($alias of an Allocation) is installed state: the fixture declares it picked (pickedAt, not after
     *   the fixture clock knownAt, and pickedByAlias naming a fixture actor, beside its state: alias, baseline.priorEntities,
     *   baseline.allocations or baseline.allocation row) or an earlier pickQuantity of the subcase picks it; a declared pick
     *   followed by another pick that is not pinned to fail is a problem. A fixture allocation the fixture declares terminal
     *   (CONSUMED, RELEASED, REPLACED, or the allocation of a baseline.priorHistory DISPATCH) needs no pick when the dispatch
     *   pins STALE_REVISION: the terminal-allocation check is the first one;
     * - a runtime allocation (the $result of an earlier action) that is expected to apply (outcome pinned APPLIED, or a later
     *   action or an assertion reads another part of its result) needs an earlier pickQuantity naming the same $result;
     * - an unpicked runtime allocation that is not expected to apply is a negative only when it pins a non-APPLIED outcome and
     *   a code whose earlier check the case shows (prePickReason, contracts/execution-preconditions.json prePickDispatch):
     *   FORBIDDEN for an actor without current dispatch authority, VERSION_UNSUPPORTED for another definition version and
     *   STALE_REVISION for an allocation an earlier action consumed. The same codes also come from later checks (an
     *   expectedRevision mismatch after prepare, the transit PLACE authorization, the DISPATCH and continuous-authority
     *   assessments), so a pinned code alone does not show that the negative is decided before the pick (closure review 7 P3);
     * - a dispatch that names no allocation at all is malformed for every product, so it is accepted only as a route-level
     *   denial pinned FORBIDDEN (batch or worker route, or a subject of another organization);
     * - an earlier pick must not be pinned to an outcome other than APPLIED, and the dispatch expectedRevision must not be the
     *   $result of an action before the pick (the pre-pick revision is stale).
     * Pins of an asynchronous dispatch (a start call) are the assertions on its await action.
     */
    public List<String> pickBeforeDispatchProblems(JsonNode caseFile) throws IOException {
        JsonNode rule=Json.read(path("contracts/execution-preconditions.json")).path("prePickDispatch");
        Set<String> terminal=new HashSet<>(),revoking=new HashSet<>();
        for(JsonNode s:rule.path("terminalStates")) terminal.add(s.asText());
        for(JsonNode s:rule.path("revokingCapabilities")) revoking.add(s.asText());
        List<String> problems=new ArrayList<>();
        for(JsonNode sub:caseFile.path("subcases")) {
            String where=caseFile.path("caseId").asText()+"/"+sub.path("id").asText();
            List<JsonNode> actions=actionsOf(sub);
            Map<String,Integer> index=new HashMap<>();for(int i=0;i<actions.size();i++) index.putIfAbsent(actions.get(i).path("id").asText(),i);
            Map<String,Set<String>> awaits=awaitIds(actions);
            String ref=sub.path("fixtureRef").asText();
            ObjectNode aliases=Json.object(),actors=Json.object();ArrayNode ignored=Json.array();Map<String,List<JsonNode>> declared=new HashMap<>();JsonNode clock=Json.object();JsonNode fixture=Json.object();
            if(!ref.isBlank() && Files.isRegularFile(path(ref))) {mergeFixture(ref,new HashSet<>(),aliases,actors,ignored);allocationDeclarations(ref,new HashSet<>(),declared);fixture=Json.read(path(ref));clock=fixture.path("clock");}
            List<java.time.Instant> clocks=clocks(sub,actions);
            for(int i=0;i<actions.size();i++) {
                JsonNode d=actions.get(i);if(!DISPATCH.equals(capabilityOf(d))) continue;
                String id=d.path("id").asText();JsonNode slot=allocationSlot(d);
                Set<String> pinSources=new HashSet<>(Set.of(id));pinSources.addAll(awaits.getOrDefault(id,Set.of()));
                String fixtureAllocation=slotAlias(slot);
                List<JsonNode> runtime=resultNodes(slot);
                if(fixtureAllocation==null && runtime.size()!=1) {
                    if(!routeDenial(sub,d,pinSources,aliases,actors)) problems.add(where+"/"+id+": dispatchQuantity names no allocation (an alias or one $result in allocationId), so every product rejects it as malformed; name the allocation (picked) or, for a route-level denial, pin /response/error/code FORBIDDEN on the batch/worker route or with a subject of another organization");
                    continue;
                }
                String source=fixtureAllocation==null?runtime.get(0).path("actionId").asText():null,pointer=fixtureAllocation==null?runtime.get(0).path("pointer").asText():null;
                Integer produced=source==null?Integer.valueOf(-1):index.get(source);
                if(fixtureAllocation!=null && !aliases.path(fixtureAllocation).path("type").asText().equals("Allocation")) continue;
                if(produced==null || produced>=i) continue;
                int pick=-1;
                for(int k=produced+1;k<i;k++) if(PICK.equals(capabilityOf(actions.get(k)))) {
                    JsonNode named=allocationSlot(actions.get(k));
                    if(fixtureAllocation!=null) {if(fixtureAllocation.equals(slotAlias(named))) pick=k;}
                    else for(JsonNode r:resultNodes(named)) if(r.path("actionId").asText().equals(source) && r.path("pointer").asText().equals(pointer)) pick=k;
                }
                String what=where+"/"+id+": dispatchQuantity of the "+(fixtureAllocation!=null?"fixture allocation "+fixtureAllocation:"runtime allocation "+source+pointer+" ("+capabilityOf(actions.get(produced))+")");
                boolean fixturePicked=false;
                if(fixtureAllocation!=null) {
                    for(JsonNode row:declared.getOrDefault(fixtureAllocation,List.of())) if(row.has("pickedAt")) {
                        fixturePicked=true;
                        java.time.Instant at=null;try {at=java.time.Instant.parse(row.path("pickedAt").asText());} catch(RuntimeException bad) {problems.add(what+": fixture pickedAt "+row.path("pickedAt")+" is not an ISO-8601 instant");}
                        if(at!=null && clock.path("knownAt").isTextual() && at.isAfter(java.time.Instant.parse(clock.path("knownAt").asText()))) problems.add(what+": fixture pickedAt "+at+" is after the fixture clock knownAt "+clock.path("knownAt").asText()+"; an installed pick is a past fact");
                        String by=row.path("pickedByAlias").asText(null);
                        if(by==null || !actors.has(by)) problems.add(what+": fixture pick names pickedByAlias "+by+", which is not a fixture actor; an installed pick records who picked");
                    }
                    boolean consumed=fixtureTerminal(fixtureAllocation,declared,fixture,terminal);
                    if(!fixturePicked && pick<0 && consumed && pinnedTo(sub,pinSources,"/response/error/code","STALE_REVISION"::equals)) continue;
                    if(!fixturePicked && pick<0) {problems.add(what+" has no picked state: the fixture declares no pickedAt (with pickedByAlias) for it and no earlier pickQuantity picks it, so the product rejects it 'Pick before dispatch required' before the behaviour the subcase tests");continue;}
                    if(fixturePicked && pick>=0 && !pinnedTo(sub,Set.of(actions.get(pick).path("id").asText()),"/response/outcome",v->!v.equals("APPLIED")))
                        problems.add(what+" is picked in the fixture and again by "+actions.get(pick).path("id").asText()+"; the product rejects a second pick ('Allocation already picked')");
                } else if(pick<0) {
                    if(expectedToApply(sub,actions,i,pinSources)) {
                        problems.add(what+" is expected to apply, but no earlier pickQuantity names that allocation; the product rejects it 'Pick before dispatch required' (FulfillmentCommands) and the adapter never creates a pick, so the case inserts an explicit, authorized pick");
                    } else {
                        String code=pinnedValue(sub,pinSources,"/response/error/code");
                        String reason=!pinnedTo(sub,pinSources,"/response/outcome",v->!v.equals("APPLIED")) || code==null?null
                            :prePickReason(code,sub,actions,i,source,pointer,clocks.get(i),aliases,actors,fixture,revoking);
                        if(reason==null) problems.add(what+" is never picked, and the subcase does not pin a non-APPLIED outcome with a code whose earlier check the case shows (FORBIDDEN for an actor without current dispatch authority, VERSION_UNSUPPORTED for another definition version, STALE_REVISION for an allocation an earlier action consumed; pinned "
                            +code+"); a product without the rule under test rejects it for the missing pick alone, so pick it first and pin the expected code");
                    }
                    continue;
                }
                if(pick<0) continue;
                String pickId=actions.get(pick).path("id").asText();
                if(!fixturePicked && pinnedTo(sub,Set.of(pickId),"/response/outcome",v->!v.equals("APPLIED")))
                    problems.add(what+" relies on pick "+pickId+", whose outcome the subcase pins to a value other than APPLIED; a refused pick leaves the allocation unpicked");
                for(JsonNode r:resultNodes(d.path("request").path("expectedRevision"))) {
                    Integer at=index.get(r.path("actionId").asText());
                    if(at!=null && at<pick) problems.add(what+" sends expectedRevision from "+r.path("actionId").asText()+", an action before pick "+pickId
                        +"; the pick increments the allocation revision, so expectedRevision chains to the pick result or a later read");
                }
            }
        }
        return problems;
    }
    /** The reason the case shows for a pre-pick code of an unpicked runtime dispatch, or null (contracts/execution-preconditions.json prePickDispatch). */
    private static String prePickReason(String code,JsonNode sub,List<JsonNode> actions,int at,String source,String pointer,java.time.Instant clock,JsonNode aliases,JsonNode actors,JsonNode fixture,Set<String> revoking) {
        JsonNode d=actions.get(at);
        switch(code) {
            case "FORBIDDEN" -> {
                JsonNode actor=actors.get(d.path("actorRef").asText());
                if(actor==null) return "actor "+d.path("actorRef").asText()+" is no fixture actor";
                if(!contains(actor.path("roleCapabilities"),DISPATCH) || !contains(actor.path("grant").path("actions"),DISPATCH)) return "actor lacks "+DISPATCH;
                if(clock!=null && !grantValidAt(actor.path("grant"),clock)) return "grant not valid at "+clock;
                for(int k=0;k<at;k++) if(revoking.contains(capabilityOf(actions.get(k)))) return "earlier "+capabilityOf(actions.get(k));
                return null;
            }
            case "VERSION_UNSUPPORTED" -> {
                String asked=d.path("request").path("definitionVersion").asText(null),installed=fixture.path("versions").path("definition").asText(null);
                return asked!=null && installed!=null && !asked.equals(installed)?"definitionVersion "+asked:null;
            }
            case "STALE_REVISION" -> {
                for(int k=0;k<at;k++) if(Set.of(DISPATCH,"releaseAllocation","replaceAllocation").contains(capabilityOf(actions.get(k))))
                    for(JsonNode r:resultNodes(allocationSlot(actions.get(k)))) if(r.path("actionId").asText().equals(source) && r.path("pointer").asText().equals(pointer)) return "consumed by "+actions.get(k).path("id").asText();
                return null;
            }
            default -> {return null;}
        }
    }
    /** A fixture allocation the fixture declares terminal: a terminal state/status beside it, or the allocation of a baseline.priorHistory DISPATCH. */
    private static boolean fixtureTerminal(String name,Map<String,List<JsonNode>> declared,JsonNode fixture,Set<String> terminal) {
        for(JsonNode row:declared.getOrDefault(name,List.of())) for(String key:List.of("state","status")) if(terminal.contains(row.path(key).asText())) return true;
        for(JsonNode h:fixture.path("baseline").path("priorHistory")) if(h.path("kind").asText().equals("DISPATCH") && name.equals(h.path("allocationAlias").asText())) return true;
        return false;
    }
    /** A dispatch without an allocation that is a route-level denial: pinned FORBIDDEN on the batch/worker route or naming a subject of another organization. */
    private static boolean routeDenial(JsonNode sub,JsonNode d,Set<String> pins,JsonNode aliases,JsonNode actors) {
        if(!pinnedTo(sub,pins,"/response/error/code","FORBIDDEN"::equals)) return false;
        if(Set.of("batch","worker").contains(d.path("route").asText())) return true;
        String org=actors.path(d.path("actorRef").asText()).path("organizationAlias").asText(null);
        for(String a:aliasRefs(d.path("request"))) {String other=aliases.path(a).path("organizationAlias").asText(null);if(org!=null && other!=null && !org.equals(other)) return true;}
        return false;
    }
    // ---- step2r round 10: product check chain preconditions (contracts/execution-preconditions.json, fixture-place-kinds.json dispatchTransit) ----
    /** Actions of a subcase in declaration order, a start's call right after the start. */
    private static List<JsonNode> actionsOf(JsonNode sub) {
        List<JsonNode> all=new ArrayList<>();collect(sub.path("actions"),all);
        List<JsonNode> actions=new ArrayList<>();for(JsonNode a:all) {actions.add(a);if(a.has("call")) actions.add(a.path("call"));}
        return actions;
    }
    /** The await action ids that carry the pins of each start call id. */
    private static Map<String,Set<String>> awaitIds(List<JsonNode> actions) {
        Map<String,Set<String>> pinIds=new HashMap<>();
        for(JsonNode a:actions) if(a.has("call")) pinIds.computeIfAbsent(a.path("call").path("id").asText(),k->new HashSet<>()).add(a.path("id").asText());
        Map<String,Set<String>> awaits=new HashMap<>();
        for(JsonNode a:actions) if(a.path("kind").asText().equals("await")) for(var e:pinIds.entrySet()) if(e.getValue().contains(a.path("awaitActionId").asText())) awaits.computeIfAbsent(e.getKey(),k->new HashSet<>()).add(a.path("id").asText());
        return awaits;
    }
    private Set<String> pins(List<JsonNode> actions,int i) {
        String id=actions.get(i).path("id").asText();Set<String> out=new HashSet<>(Set.of(id));out.addAll(awaitIds(actions).getOrDefault(id,Set.of()));return out;
    }
    /** expectedToApply without a pinned error code or a pinned non-APPLIED outcome (a negative may still read /response fields). */
    private boolean applies(JsonNode sub,List<JsonNode> actions,int i) {
        Set<String> ids=pins(actions,i);
        return expectedToApply(sub,actions,i,ids) && pinnedValue(sub,ids,"/response/error/code")==null && !pinnedTo(sub,ids,"/response/outcome",v->!v.equals("APPLIED"));
    }
    private static String pinnedValue(JsonNode sub,Set<String> actionIds,String pointer) {
        for(JsonNode x:sub.path("assertions")) if(x.path("op").asText().equals("equals") && actionIds.contains(x.path("source").path("actionId").asText())
                && x.path("source").path("pointer").asText().equals(pointer) && x.path("expected").isTextual()) return x.path("expected").asText();
        return null;
    }
    private static boolean contains(JsonNode array,String value) {for(JsonNode v:array) if(v.asText().equals(value)) return true;return false;}
    private static java.time.Instant instant(JsonNode node) {
        String text=node.isTextual()?node.asText():node.path("value").isTextual()?node.path("value").asText():null;
        if(text==null) return null;
        try {return java.time.Instant.parse(text);} catch(RuntimeException bad) {return null;}
    }
    private static boolean grantValidAt(JsonNode grant,java.time.Instant at) {
        java.time.Instant from=instant(grant.path("validFrom")),until=instant(grant.path("validUntil"));
        return from!=null && until!=null && !at.isBefore(from) && at.isBefore(until);
    }
    /** The fixture clock asOf of a fixture or its baseRefs. */
    private java.time.Instant fixtureClock(String ref,Set<String> visiting) throws IOException {
        if(ref.isBlank() || !visiting.add(ref) || !Files.isRegularFile(path(ref))) return null;
        JsonNode fixture=Json.read(path(ref));
        java.time.Instant own=instant(fixture.path("clock").path("asOf"));if(own!=null) return own;
        for(JsonNode base:fixture.path("baseRefs")) {java.time.Instant found=fixtureClock(base.asText(),visiting);if(found!=null) return found;}
        return null;
    }
    /**
     * The product clock at each action (contracts/execution-preconditions.json clock): the fixture clock asOf, set by every
     * clock control to its instant, else asOf, else knownAt parameter.
     */
    private List<java.time.Instant> clocks(JsonNode sub,List<JsonNode> actions) throws IOException {
        java.time.Instant now=fixtureClock(sub.path("fixtureRef").asText(),new HashSet<>());List<java.time.Instant> out=new ArrayList<>();
        for(JsonNode a:actions) {
            if(a.path("kind").asText().equals("control") && a.path("control").path("type").asText().equals("clock")) {
                JsonNode p=a.path("control").path("parameters");
                for(String key:List.of("instant","asOf","knownAt")) {java.time.Instant t=instant(p.path(key));if(t!=null) {now=t;break;}}
            }
            out.add(now);
        }
        return out;
    }
    /** The object of a dispatch request that carries its allocation (slots, else the request), or a missing node. */
    private static JsonNode allocationHolder(JsonNode a) {
        JsonNode slots=a.path("request").path("slots");
        return slots.has("allocationId")?slots:a.path("request").has("allocationId")?a.path("request"):com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }
    private static List<String> grantPlaces(JsonNode actor) {
        JsonNode scope=actor.path("grant").path("scope");
        for(String key:List.of("placeAliases","places")) if(scope.path(key).isArray()) {List<String> out=new ArrayList<>();for(JsonNode p:scope.path(key)) out.add(p.asText());return out;}
        return null;
    }
    /** The acting actor and, when it is a fixture actor, its delegator: both are checked for the same targets (IdentityAuthorization). */
    private static List<String> authorityChain(String actorRef,JsonNode actors) {
        List<String> out=new ArrayList<>();String current=actorRef;
        while(current!=null && actors.has(current) && !out.contains(current)) {out.add(current);String d=actors.path(current).path("grant").path("delegatorAlias").asText(null);current=d==null || d.equals(current)?null:d;}
        return out;
    }
    /**
     * contracts/fixture-place-kinds.json dispatchTransit (step2r round 10, Step 2 closure review 7 P1). After the pick the product
     * reads transitPlaceId (TYPE_INVALID), requires Place.kind TRANSIT ('Transit place required') and authorizes the PLACE
     * [transit] target (FORBIDDEN). Every dispatch that names an allocation names a fixture TRANSIT place in cargoPlaceId beside
     * the allocation, and unless the dispatch pins FORBIDDEN, every actor of its authority chain that lists grant places lists it.
     */
    public List<String> dispatchTransitProblems(JsonNode caseFile) throws IOException {
        JsonNode rule=Json.read(path("contracts/fixture-place-kinds.json")).path("dispatchTransit");
        String capability=Json.required(rule,"capability"),slot=Json.required(rule,"slot"),productSlot=Json.required(rule,"productSlot"),kind=Json.required(rule,"placeKind");
        List<String> problems=new ArrayList<>();
        for(JsonNode sub:caseFile.path("subcases")) {
            String where=caseFile.path("caseId").asText()+"/"+sub.path("id").asText();
            List<JsonNode> actions=actionsOf(sub);
            ObjectNode aliases=Json.object(),actors=Json.object();ArrayNode ignored=Json.array();
            String ref=sub.path("fixtureRef").asText();
            if(!ref.isBlank() && Files.isRegularFile(path(ref))) mergeFixture(ref,new HashSet<>(),aliases,actors,ignored);
            for(int i=0;i<actions.size();i++) {
                JsonNode d=actions.get(i);if(!capability.equals(capabilityOf(d))) continue;
                JsonNode holder=allocationHolder(d);if(holder.isMissingNode()) continue;
                String id=where+"/"+d.path("id").asText();
                String transit=slotAlias(holder.path(slot));if(transit==null) transit=slotAlias(holder.path(productSlot));
                if(transit==null) {problems.add(id+": "+capability+" names an allocation but no "+kind+" place in "+slot+"; the product rejects it after the pick (TYPE_INVALID 'Invalid "+productSlot+"'), so name the fixture "+kind+" place the goods move to (contracts/fixture-place-kinds.json dispatchTransit)");continue;}
                if(!aliases.path(transit).path("type").asText().equals("Place") || !kind.equals(aliases.path(transit).path("kind").asText())) {
                    problems.add(id+": "+slot+" "+transit+" is not a fixture Place of kind "+kind+" (found "+aliases.path(transit).path("kind").asText("none")+"); the product answers TYPE_INVALID 'Transit place required'");continue;}
                if(pinnedTo(sub,pins(actions,i),"/response/error/code","FORBIDDEN"::equals)) continue;
                for(String who:authorityChain(d.path("actorRef").asText(),actors)) {
                    List<String> places=grantPlaces(actors.path(who));
                    if(places!=null && !places.contains(transit)) problems.add(id+": the grant of "+who+" lists places "+places+" without the transit place "+transit+"; the product authorizes PLACE ["+transit+"] for the dispatch (FORBIDDEN)");
                }
            }
        }
        return problems;
    }
    /**
     * contracts/execution-preconditions.json occurrence (step2r round 10, closure review 7 P2). With the product clock of
     * clocks(), an action's occurrence is its explicit occurredAt, else the request asOf, else the clock. requireContinuousAuthority
     * rejects a dispatch dated before its authorized pick and observeDelivery a delivery before its dispatch; no actual occurrence
     * may lie after the clock.
     */
    public List<String> occurrenceTimeProblems(JsonNode caseFile) throws IOException {
        Set<String> preTime=Set.of("FORBIDDEN","STALE_REVISION","SCOPE_INELIGIBLE","VERSION_UNSUPPORTED");
        List<String> problems=new ArrayList<>();
        for(JsonNode sub:caseFile.path("subcases")) {
            String where=caseFile.path("caseId").asText()+"/"+sub.path("id").asText();
            List<JsonNode> actions=actionsOf(sub);List<java.time.Instant> clocks=clocks(sub,actions);
            ObjectNode aliases=Json.object(),actors=Json.object();ArrayNode ignored=Json.array();Map<String,List<JsonNode>> declared=new HashMap<>();
            String ref=sub.path("fixtureRef").asText();
            if(!ref.isBlank() && Files.isRegularFile(path(ref))) {mergeFixture(ref,new HashSet<>(),aliases,actors,ignored);allocationDeclarations(ref,new HashSet<>(),declared);}
            Map<String,java.time.Instant> picked=new HashMap<>(),dispatched=new HashMap<>();
            for(int i=0;i<actions.size();i++) {
                JsonNode a=actions.get(i);String cap=capabilityOf(a);java.time.Instant now=clocks.get(i);
                if(cap.isBlank() || now==null || !a.has("request")) continue;
                String id=where+"/"+a.path("id").asText();Set<String> pins=pins(actions,i);
                JsonNode holder=a.path("request").path("slots").has("occurredAt")?a.path("request").path("slots"):a.path("request");
                java.time.Instant explicit=instant(holder.path("occurredAt"));
                java.time.Instant occurrence=explicit!=null?explicit:instant(a.path("request").path("asOf"));if(occurrence==null) occurrence=now;
                if(PICK.equals(cap)) for(JsonNode r:resultNodes(allocationSlot(a))) picked.put(r.path("actionId").asText()+r.path("pointer").asText(),now);
                String code=pinnedValue(sub,pins,"/response/error/code");
                boolean applies=applies(sub,actions,i);
                if(explicit!=null && explicit.isAfter(now) && (applies || code==null || !preTime.contains(code)))
                    problems.add(id+": occurredAt "+explicit+" is after the product clock "+now+" at this action; the product rejects a future actual occurrence (contracts/execution-preconditions.json occurrence)");
                if(DISPATCH.equals(cap)) {
                    dispatched.put(a.path("id").asText(),occurrence);
                    if(code!=null && preTime.contains(code)) continue;
                    java.time.Instant pick=null;String alloc=slotAlias(allocationSlot(a));
                    if(alloc!=null) for(JsonNode row:declared.getOrDefault(alloc,List.of())) if(row.has("pickedAt")) pick=instant(row.path("pickedAt"));
                    for(JsonNode r:resultNodes(allocationSlot(a))) pick=picked.getOrDefault(r.path("actionId").asText()+r.path("pointer").asText(),pick);
                    if(occurrence.isAfter(now)) problems.add(id+": dispatch occurrence "+occurrence+" is after the product clock "+now);
                    if(pick!=null && occurrence.isBefore(pick)) problems.add(id+": dispatch occurrence "+occurrence+" precedes its authorized pick at "+pick
                        +"; the product answers TYPE_INVALID 'Dispatch occurrence cannot precede its authorized pick' (requireContinuousAuthority). Pick earlier on the clock (a fixture past-fact pick, or a clock advance between pick and dispatch) or date the dispatch at or after the pick");
                }
                if(cap.equals("recordDelivery") && applies) for(JsonNode r:resultNodes(a.path("request"))) {
                    java.time.Instant d=dispatched.get(r.path("actionId").asText());
                    if(d!=null && occurrence.isBefore(d)) {problems.add(id+": delivery occurrence "+occurrence+" precedes the dispatch "+r.path("actionId").asText()+" at "+d+"; the product rejects 'Delivery outside dispatched exact range or time' (FulfillmentStockPrimitives.observeDelivery)");break;}
                }
            }
        }
        return problems;
    }
    /**
     * contracts/execution-preconditions.json grantAuthority (step2r round 10). Fixture level: a grant scope's capabilityIds list
     * every grant action, and a fixture-actor delegator holds every action it delegates. Action level: a COMMAND/RECORD action the
     * subcase expects to apply is performed by a fixture actor holding the capability (role, grant, capabilityIds) with a grant
     * valid at the clock, and every authority-chain actor that lists grant places lists the places the product authorizes.
     */
    public List<String> grantAuthorityProblems(JsonNode caseFile) throws IOException {
        JsonNode rule=Json.read(path("contracts/execution-preconditions.json")).path("grantAuthority");String capabilityScope=Json.required(rule,"capabilityScopeField");
        Map<String,String> kinds=new HashMap<>();
        for(JsonNode c:Json.read(path("contracts/acceptance-capabilities.json")).path("capabilities")) kinds.put(c.path("id").asText(),c.path("kind").asText());
        List<String> problems=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(JsonNode sub:caseFile.path("subcases")) {
            String where=caseFile.path("caseId").asText()+"/"+sub.path("id").asText(),ref=sub.path("fixtureRef").asText();
            if(ref.isBlank() || !Files.isRegularFile(path(ref))) continue;
            ObjectNode aliases=Json.object(),actors=Json.object();ArrayNode ignored=Json.array();mergeFixture(ref,new HashSet<>(),aliases,actors,ignored);
            Map<String,ObjectNode> rows=new LinkedHashMap<>();mergeSegments(ref,new HashSet<>(),rows);Map<String,List<JsonNode>> declared=new HashMap<>();allocationDeclarations(ref,new HashSet<>(),declared);
            if(seen.add(ref)) for(var it=actors.fields();it.hasNext();) {
                var e=it.next();JsonNode grant=e.getValue().path("grant");
                JsonNode ids=grant.path("scope").path(capabilityScope);
                if(ids.isArray()) for(JsonNode action:grant.path("actions")) if(!contains(ids,action.asText()))
                    problems.add(caseFile.path("caseId").asText()+" "+ref+": actor "+e.getKey()+" grant action "+action.asText()+" is missing from its grant scope "+capabilityScope+"; an installer intersects or rejects the inconsistent grant, so the action is FORBIDDEN");
                String d=grant.path("delegatorAlias").asText(null);
                if(d!=null && !d.equals(e.getKey()) && actors.has(d)) for(JsonNode action:grant.path("actions"))
                    if(!contains(actors.path(d).path("roleCapabilities"),action.asText()) || !contains(actors.path(d).path("grant").path("actions"),action.asText()))
                        problems.add(caseFile.path("caseId").asText()+" "+ref+": delegator "+d+" of "+e.getKey()+" does not hold the delegated action "+action.asText()+" in roleCapabilities and grant.actions; the product checks the delegator for the same capability (IdentityAuthorization, plan section 7.1)");
            }
            List<JsonNode> actions=actionsOf(sub);List<java.time.Instant> clocks=clocks(sub,actions);
            for(int i=0;i<actions.size();i++) {
                JsonNode a=actions.get(i);String cap=capabilityOf(a);
                if(!Set.of("COMMAND","RECORD").contains(kinds.getOrDefault(cap,"")) || !Set.of("invoke","query").contains(a.path("kind").asText()) || !applies(sub,actions,i)) continue;
                String id=where+"/"+a.path("id").asText(),who=a.path("actorRef").asText();JsonNode actor=actors.get(who);
                if(actor==null) {problems.add(id+": "+cap+" is expected to apply but "+who+" is no fixture actor");continue;}
                JsonNode grant=actor.path("grant");
                if(!contains(actor.path("roleCapabilities"),cap) || !contains(grant.path("actions"),cap) || grant.path("scope").path(capabilityScope).isArray() && !contains(grant.path("scope").path(capabilityScope),cap))
                    problems.add(id+": "+cap+" is expected to apply but "+who+" does not hold it in roleCapabilities, grant.actions"+(grant.path("scope").has(capabilityScope)?" and grant.scope."+capabilityScope:"")+" (IdentityAuthorization: FORBIDDEN)");
                if(clocks.get(i)!=null && grant.has("validFrom") && !grantValidAt(grant,clocks.get(i))) problems.add(id+": the grant of "+who+" ("+grant.path("validFrom").asText()+".."+grant.path("validUntil").asText()+") is not valid at the product clock "+clocks.get(i)+" of this action");
                for(String place:authorizedPlaces(a,cap,aliases,rows,declared)) for(String chain:authorityChain(who,actors)) {
                    List<String> places=grantPlaces(actors.path(chain));
                    if(places!=null && !places.contains(place)) problems.add(id+": "+cap+" is expected to apply but the grant of "+chain+" lists places "+places+" without "+place+", which the product authorizes for it (PLACE scope, FORBIDDEN)");
                }
            }
        }
        return problems;
    }
    /** The place of a fixture segment: its alias or baseline.segments locationAlias/placeAlias. */
    private static String segmentPlace(String segment,JsonNode aliases,Map<String,ObjectNode> rows) {
        for(JsonNode r:List.of(aliases.path(segment),rows.getOrDefault(segment,Json.object()))) for(String key:List.of("locationAlias","placeAlias")) if(r.path(key).isTextual()) return r.path(key).asText();
        return null;
    }
    private static String segmentCustodian(String segment,JsonNode aliases,Map<String,ObjectNode> rows) {
        for(JsonNode r:List.of(aliases.path(segment),rows.getOrDefault(segment,Json.object()))) if(r.path("custodianAlias").isTextual()) return r.path("custodianAlias").asText();
        return null;
    }
    /**
     * The fixture segment an action works on: a reserve/split/move segment slot or its declared QuantitySegment subject (the
     * product binds QuantitySegment to the command target, so the adapter maps the subject to segmentId), or the segment of a
     * fixture allocation it names.
     */
    private static String fixtureSegment(JsonNode a,String cap,JsonNode aliases,Map<String,List<JsonNode>> declared) {
        JsonNode slots=a.path("request").path("slots"),req=a.path("request");
        if(Set.of("reserveQuantity","splitQuantity","moveQuantity").contains(cap)) {
            List<String> named=new ArrayList<>();
            for(String key:List.of("segmentId","sourceSegmentId")) {String s=slotAlias(slots.path(key));if(s==null) s=slotAlias(req.path(key));if(s!=null) named.add(s);}
            for(JsonNode ref:req.path("subjectRefs")) if(ref.path("type").asText().equals("QuantitySegment") && slotAlias(ref.path("id"))!=null) named.add(slotAlias(ref.path("id")));
            for(String s:named) if(aliases.path(s).path("type").asText().equals("QuantitySegment")) return s;
        }
        if(Set.of(PICK,DISPATCH,"releaseAllocation","replaceAllocation").contains(cap)) {
            String alloc=slotAlias(allocationSlot(a));
            if(alloc!=null) for(JsonNode row:declared.getOrDefault(alloc,List.of())) if(row.path("segmentAlias").isTextual() && aliases.path(row.path("segmentAlias").asText()).path("type").asText().equals("QuantitySegment")) return row.path("segmentAlias").asText();
        }
        return null;
    }
    private static List<String> authorizedPlaces(JsonNode a,String cap,JsonNode aliases,Map<String,ObjectNode> rows,Map<String,List<JsonNode>> declared) {
        List<String> out=new ArrayList<>();JsonNode slots=a.path("request").path("slots"),req=a.path("request");
        String segment=fixtureSegment(a,cap,aliases,declared);
        if(segment!=null) {String place=segmentPlace(segment,aliases,rows);if(place!=null) out.add(place);}
        if(cap.equals("moveQuantity")) {String dest=slotAlias(slots.path("destinationId"));if(dest==null) dest=slotAlias(req.path("destinationId"));if(dest!=null && aliases.path(dest).path("type").asText().equals("Place")) out.add(dest);}
        if(cap.equals("confirmReceipt")) for(String key:List.of("placeId","destinationId","locationId")) {String p=slotAlias(slots.path(key));if(p!=null) {if(aliases.path(p).path("type").asText().equals("Place")) out.add(p);break;}}
        return out;
    }
    /**
     * contracts/execution-preconditions.json commandBasis (step2r round 10). dispatchQuantity and the stock commands require an
     * evidenceRef (FulfillmentCommands, InventoryCommands); a command the subcase expects to apply names a basis anywhere in its request.
     */
    public List<String> commandBasisProblems(JsonNode caseFile) throws IOException {
        JsonNode rule=Json.read(path("contracts/execution-preconditions.json")).path("commandBasis");
        Set<String> caps=new HashSet<>(),fields=new HashSet<>();for(JsonNode c:rule.path("capabilities")) caps.add(c.asText());for(JsonNode f:rule.path("fields")) fields.add(f.asText());
        List<String> problems=new ArrayList<>();
        for(JsonNode sub:caseFile.path("subcases")) {
            List<JsonNode> actions=actionsOf(sub);
            for(int i=0;i<actions.size();i++) {
                JsonNode a=actions.get(i);String cap=capabilityOf(a);
                if(!caps.contains(cap) || !a.has("request") || !applies(sub,actions,i) || namesBasis(a.path("request"),fields)) continue;
                problems.add(caseFile.path("caseId").asText()+"/"+sub.path("id").asText()+"/"+a.path("id").asText()+": "+cap+" is expected to apply but names no basis ("+new TreeSet<>(fields)+"); the product requires an evidenceRef (contracts/execution-preconditions.json commandBasis)");
            }
        }
        return problems;
    }
    private static boolean namesBasis(JsonNode node,Set<String> fields) {
        if(node.isObject()) {
            for(var it=node.fields();it.hasNext();) {var e=it.next();
                if(fields.contains(e.getKey()) && !(e.getValue().isArray() && e.getValue().isEmpty()) && !e.getValue().isNull() && !(e.getValue().isTextual() && e.getValue().asText().isBlank())) return true;
                if(namesBasis(e.getValue(),fields)) return true;}
        } else if(node.isArray()) for(JsonNode v:node) if(namesBasis(v,fields)) return true;
        return false;
    }
    /**
     * contracts/execution-preconditions.json warehouseCustody (step2r round 10). requireWarehouse and moveQuantity accept only stock at
     * INTERNAL_STORAGE under an internal Human/Agent custodian; a move goes to another INTERNAL_STORAGE place.
     */
    public List<String> warehouseCustodyProblems(JsonNode caseFile) throws IOException {
        JsonNode contract=Json.read(path("contracts/fixture-place-kinds.json"));Set<String> internal=new HashSet<>();for(JsonNode t:contract.path("internalCustodianAliasTypes")) internal.add(t.asText());
        Set<String> caps=new HashSet<>();for(JsonNode c:Json.read(path("contracts/execution-preconditions.json")).path("warehouseCustody").path("capabilities")) caps.add(c.asText());
        List<String> problems=new ArrayList<>();
        for(JsonNode sub:caseFile.path("subcases")) {
            String ref=sub.path("fixtureRef").asText();if(ref.isBlank() || !Files.isRegularFile(path(ref))) continue;
            ObjectNode aliases=Json.object(),actors=Json.object();ArrayNode ignored=Json.array();mergeFixture(ref,new HashSet<>(),aliases,actors,ignored);
            Map<String,ObjectNode> rows=new LinkedHashMap<>();mergeSegments(ref,new HashSet<>(),rows);Map<String,List<JsonNode>> declared=new HashMap<>();allocationDeclarations(ref,new HashSet<>(),declared);
            List<JsonNode> actions=actionsOf(sub);
            for(int i=0;i<actions.size();i++) {
                JsonNode a=actions.get(i);String cap=capabilityOf(a);
                if(!caps.contains(cap) || !applies(sub,actions,i)) continue;
                String segment=fixtureSegment(a,cap,aliases,declared);if(segment==null) continue;
                String id=caseFile.path("caseId").asText()+"/"+sub.path("id").asText()+"/"+a.path("id").asText(),place=segmentPlace(segment,aliases,rows),holder=segmentCustodian(segment,aliases,rows);
                if(place==null || !"INTERNAL_STORAGE".equals(aliases.path(place).path("kind").asText()) || holder==null || !internal.contains(aliases.path(holder).path("type").asText()))
                    problems.add(id+": "+cap+" is expected to apply to fixture segment "+segment+" at "+place+" (custodian "+holder+"), but the product requires INTERNAL_STORAGE under an internal Human/Agent custodian (FulfillmentCommands.requireWarehouse, InventoryCommands moveQuantity)");
                if(cap.equals("moveQuantity")) {
                    String dest=slotAlias(a.path("request").path("slots").path("destinationId"));if(dest==null) dest=slotAlias(a.path("request").path("destinationId"));
                    if(dest!=null && (!"INTERNAL_STORAGE".equals(aliases.path(dest).path("kind").asText()) || dest.equals(place)))
                        problems.add(id+": moveQuantity of "+segment+" goes to "+dest+"; the product moves only between distinct INTERNAL_STORAGE places");
                }
            }
        }
        return problems;
    }
    /** Every declaration of a fixture allocation (alias entry, baseline.priorEntities entry, baseline.allocations/allocation row), baseRefs first. */
    private void allocationDeclarations(String ref,Set<String> visiting,Map<String,List<JsonNode>> out) throws IOException {
        if(ref.isBlank() || !visiting.add(ref) || !Files.isRegularFile(path(ref))) return;
        JsonNode fixture=Json.read(path(ref));
        for(JsonNode base:fixture.path("baseRefs")) allocationDeclarations(base.asText(),visiting,out);
        fixture.path("aliases").fields().forEachRemaining(e->{if(e.getValue().path("type").asText().equals("Allocation")) out.computeIfAbsent(e.getKey(),k->new ArrayList<>()).add(e.getValue());});
        fixture.path("baseline").path("priorEntities").fields().forEachRemaining(e->out.computeIfAbsent(e.getKey(),k->new ArrayList<>()).add(e.getValue()));
        for(String key:List.of("allocations","allocation")) for(JsonNode row:fixture.path("baseline").path(key)) if(row.path("alias").isTextual()) out.computeIfAbsent(row.path("alias").asText(),k->new ArrayList<>()).add(row);
    }
    private static boolean pinnedTo(JsonNode sub,Set<String> actionIds,String pointer,java.util.function.Predicate<String> value) {
        for(JsonNode x:sub.path("assertions")) if(x.path("op").asText().equals("equals") && actionIds.contains(x.path("source").path("actionId").asText())
                && x.path("source").path("pointer").asText().equals(pointer) && x.path("expected").isTextual() && value.test(x.path("expected").asText())) return true;
        return false;
    }
    private static String capabilityOf(JsonNode a) {return a.path("capabilityId").asText(a.path("request").path("capabilityId").asText());}
    private static JsonNode allocationSlot(JsonNode a) {
        JsonNode slot=a.path("request").path("slots").path("allocationId");
        return slot.isMissingNode()?a.path("request").path("allocationId"):slot;
    }
    /** A dispatch is expected to apply when its outcome is pinned APPLIED (on it or its await), or a later action or an assertion reads another part of its result. */
    private static boolean expectedToApply(JsonNode sub,List<JsonNode> actions,int at,Set<String> ids) {
        java.util.function.Predicate<String> effect=p->!p.equals("/response/outcome") && !p.startsWith("/response/error");
        String id=actions.get(at).path("id").asText();
        for(JsonNode x:sub.path("assertions")) {
            JsonNode s=x.path("source");String op=x.path("op").asText();
            if(ids.contains(s.path("actionId").asText())) {
                String p=s.path("pointer").asText();
                if(p.equals("/response/outcome")) {if(op.equals("equals") && x.path("expected").asText().equals("APPLIED")) return true;}
                else if(p.startsWith("/response/") && effect.test(p) && !Set.of("absent","notEquals").contains(op)) return true;
            }
            for(JsonNode r:resultNodes(x)) if(r.path("actionId").asText().equals(id) && effect.test(r.path("pointer").asText())) return true;
        }
        for(int k=at+1;k<actions.size();k++) for(JsonNode r:resultNodes(actions.get(k).path("request"))) if(r.path("actionId").asText().equals(id)) return true;
        return false;
    }
    /** The fixture leaf (or explicit split child) a splitQuantity divides: its segmentId slot alias or $result child, else null. */
    private static ObjectNode splitSource(JsonNode slots,Map<String,ObjectNode> leaves,Map<String,ObjectNode> children) {
        JsonNode s=slots.path("segmentId");
        String alias=slotAlias(s);if(alias!=null) return leaves.get(alias);
        for(JsonNode r:resultNodes(s)) {ObjectNode c=children.get(r.path("actionId").asText()+r.path("pointer").asText());if(c!=null) return c;}
        return null;
    }
    /** Explicit children of a split of a fixture leaf: {"alias","quantity","unit"} entries keyed by their $result pointer. */
    private static void splitChildren(String id,JsonNode slots,Map<String,ObjectNode> leaves,Map<String,ObjectNode> children) {
        ObjectNode source=splitSource(slots,leaves,children);if(source==null) return;
        JsonNode list=slots.path("children").has("value")?slots.path("children").path("value"):slots.path("children");
        for(JsonNode c:list) if(c.path("alias").isTextual()) {
            ObjectNode child=source.deepCopy();child.set("quantity",c.path("quantity"));child.set("unit",c.path("unit"));
            children.put(id+"/response/children/"+c.path("alias").asText()+"/segmentId",child);
        }
    }
    private static List<JsonNode> resultNodes(JsonNode node) {List<JsonNode> out=new ArrayList<>();resultNodes(node,out);return out;}
    private static void resultNodes(JsonNode node,List<JsonNode> out) {
        if(node.isObject()) {if(node.path("$result").path("actionId").isTextual()) out.add(node.path("$result"));for(JsonNode v:node) resultNodes(v,out);}
        else if(node.isArray()) for(JsonNode v:node) resultNodes(v,out);
    }
    private void transitProblems(String id,String name,ObjectNode leaf,JsonNode slots,JsonNode aliases,JsonNode rule,List<String> problems) {
        String location=leaf.path("locationAlias").asText(leaf.path("placeAlias").asText(null)),kind=rule.path("leafPlaceKind").asText();
        JsonNode place=location==null?null:aliases.get(location);
        if(place==null || !kind.equals(place.path("kind").asText()))
            problems.add(id+": transit receipt leaf "+name+" is at "+location+" (kind "+(place==null?"none":place.path("kind").asText())+"); the product accepts only a leaf at a "+kind
                +" place ('Exact identified transit leaf required; split partial cargo first', contracts/fixture-place-kinds.json transitReceipt)");
        for(JsonNode u:rule.path("unidentifiedIdentifiability")) if(leaf.path("identifiability").asText().equals(u.asText()))
            problems.add(id+": transit receipt leaf "+name+" is "+u.asText()+"; the product accepts only an identified leaf");
        if(leaf.has("identificationStatus") && !leaf.path("identificationStatus").asText().equals("CONFIRMED"))
            problems.add(id+": transit receipt leaf "+name+" identificationStatus "+leaf.path("identificationStatus").asText()+" is not CONFIRMED");
        JsonNode quantity=slots.path("quantity").has("value") && slots.path("quantity").has("unit")?slots.path("quantity"):slots.path("quantity").path("value");
        String received=quantity.path("value").asText(null),unit=quantity.path("unit").asText(null);
        String leafQuantity=leaf.path("quantity").asText(null),leafUnit=leaf.path("unit").asText(null);
        if(received==null || leafQuantity==null) problems.add(id+": transit receipt of "+name+" needs a quantity slot and a leaf quantity to show the exact-leaf rule");
        else if(new java.math.BigDecimal(received).compareTo(new java.math.BigDecimal(leafQuantity))!=0 || !Objects.equals(unit,leafUnit))
            problems.add(id+": transit receipt quantity "+received+" "+unit+" differs from leaf "+name+" "+leafQuantity+" "+leafUnit+"; the product requires exactly the leaf quantity, so split partial cargo first (plan §4.2)");
        for(String[] k:new String[][]{{"itemId","itemAlias"},{"lotId","lotAlias"}}) {
            String asked=slotAlias(slots.path(k[0]));
            if(asked!=null && leaf.path(k[1]).isTextual() && !asked.equals(leaf.path(k[1]).asText())) problems.add(id+": transit receipt "+k[0]+" "+asked+" differs from leaf "+name+" "+k[1]+" "+leaf.path(k[1]).asText());
        }
        String destination=null;for(String key:List.of("placeId","destinationId","locationId")) if(destination==null) destination=slotAlias(slots.path(key));
        String into=rule.path("destinationPlaceKind").asText();
        if(destination==null || !into.equals(aliases.path(destination).path("kind").asText())) problems.add(id+": transit receipt destination "+destination+" is not a "+into+" place");
    }
    /** baseline.segments rows by alias of a fixture and its baseRefs (later fixtures win). */
    private void mergeSegments(String ref,Set<String> visiting,Map<String,ObjectNode> rows) throws IOException {
        if(ref.isBlank() || !visiting.add(ref) || !Files.isRegularFile(path(ref))) return;
        JsonNode fixture=Json.read(path(ref));
        for(JsonNode base:fixture.path("baseRefs")) mergeSegments(base.asText(),visiting,rows);
        for(JsonNode s:fixture.path("baseline").path("segments")) if(s.path("alias").isTextual()) rows.put(s.path("alias").asText(),(ObjectNode)s.deepCopy());
    }
    /**
     * The receiving-custodian slot of a direct receipt. Problems are grouped like the product checks them (ReceiptCommands):
     * FORBIDDEN when the slot is not an actor of the confirming organization (identity.actor(...).orElseThrow(forbidden):
     * no fixture actor, or another organization), then SCOPE_INELIGIBLE at preparation (not an internal Human/Agent, no
     * confirmReceipt role/grant or place scope), then EVIDENCE_CONFLICT when the verification-basis originals of the
     * receipt's canonical occurrence name different custodians, then EVIDENCE_UNVERIFIED when they do not name exactly the
     * slot custodian (step2r round 9: only verification bases count, see originals). A positive receipt needs none; a
     * declared custodyControl must equal the first one found. A malformed slot or a hash that does not bind the naming
     * original is always a problem.
     */
    private void custodianProblems(String where,JsonNode sub,JsonNode receipt,List<JsonNode> actions,String slot,String field,List<String> basis,Set<String> internal,JsonNode aliases,JsonNode actors,JsonNode evidence,
                                   Map<String,JsonNode> byId,String controlField,Map<String,String> controls,List<String> problems) throws IOException {
        String id=where+"/"+receipt.path("id").asText();JsonNode slots=receipt.path("request").path("slots");
        String custodian=slotAlias(slots.path(slot));
        if(custodian==null) {problems.add(id+": "+slot+" must name a fixture alias ({\"$alias\":...} or {\"value\":{\"$alias\":...}})");return;}
        Map<String,List<String>> defects=new LinkedHashMap<>();for(String c:List.of("FORBIDDEN","SCOPE_INELIGIBLE","EVIDENCE_CONFLICT","EVIDENCE_UNVERIFIED")) defects.put(c,new ArrayList<>());
        JsonNode holder=aliases.get(custodian),actor=actors.get(custodian);
        String organization=actors.path(receipt.path("actorRef").asText()).path("organizationAlias").asText(null);
        if(actor==null) defects.get("FORBIDDEN").add(slot+" "+custodian+" is not a fixture actor; the product looks the custodian up among the confirming organization's actors and answers REJECTED FORBIDDEN");
        else for(JsonNode o:List.of(actor,holder==null?Json.object():holder)) if(organization!=null && o.path("organizationAlias").isTextual() && !organization.equals(o.path("organizationAlias").asText()))
            defects.get("FORBIDDEN").add(slot+" "+custodian+" belongs to organization "+o.path("organizationAlias").asText()+", not to the confirming actor's "+organization+"; the product finds no such actor in that organization (REJECTED FORBIDDEN)");
        if(holder==null || !internal.contains(holder.path("type").asText())) defects.get("SCOPE_INELIGIBLE").add(slot+" "+custodian+" is not an internal custodian (a "+internal+" alias)");
        if(actor!=null) {
            boolean role=false,grant=false;
            for(JsonNode c:actor.path("roleCapabilities")) role|=c.asText().equals("confirmReceipt");
            for(JsonNode c:actor.path("grant").path("actions")) grant|=c.asText().equals("confirmReceipt");
            if(!(role && grant)) defects.get("SCOPE_INELIGIBLE").add(slot+" "+custodian+" has no confirmReceipt role and grant; the product requires the receiving custodian's current receive authority");
            String place=null;for(String key:List.of("locationId","placeId","destinationId")) if(place==null) place=slotAlias(slots.path(key));
            if(place!=null) for(String key:List.of("placeAliases","places")) {
                JsonNode scope=actor.path("grant").path("scope").path(key);
                if(!scope.isArray()) continue;
                boolean listed=false;for(JsonNode p:scope) listed|=p.asText().equals(place);
                if(!listed) defects.get("SCOPE_INELIGIBLE").add(slot+" "+custodian+" grant scope "+key+" does not include the receipt place "+place);
            }
        }
        // Originals: the verification bases of the receipt's canonical occurrence only (step2r round 9). The product reads the
        // custodians of the verified chains of that occurrence (ReceiptCommands.evidencedCustodians, TradeEvidence.verifiedCanonical),
        // not request-level evidenceRefs: the receipt's own basis slots, a runtime-attached original cited there, and the basis of
        // every earlier confirmReceipt of the same canonicalOccurrenceKey (a duplicate source adds a verified chain).
        Map<String,String> stated=new LinkedHashMap<>();Set<String> cited=new LinkedHashSet<>();Set<String> visiting=new HashSet<>(Set.of(receipt.path("id").asText()));
        originals(receipt.path("request"),false,basis,aliases,byId,visiting,field,evidence,stated,cited,id,problems);
        String key=plainText(slots.path("canonicalOccurrenceKey"));
        if(key!=null) for(JsonNode other:actions) {
            if(other==receipt) break;
            if(capabilityOf(other).equals(capabilityOf(receipt)) && key.equals(plainText(other.path("request").path("slots").path("canonicalOccurrenceKey"))) && visiting.add(other.path("id").asText()))
                originals(other.path("request"),false,basis,aliases,byId,visiting,field,evidence,stated,cited,id,problems);
        }
        Set<String> names=new TreeSet<>(stated.values());
        if(names.size()>1) defects.get("EVIDENCE_CONFLICT").add("cited receipt originals name different receiving custodians "+stated);
        for(var e:stated.entrySet()) if(!e.getValue().equals(custodian)) defects.get("EVIDENCE_UNVERIFIED").add("receipt original "+e.getKey()+" names receiving custodian "+e.getValue()+", the slot "+custodian);
        if(!names.contains(custodian)) defects.get("EVIDENCE_UNVERIFIED").add("no receipt original it cites ("+cited+") names "+custodian+" in "+field+"; the product accepts "+slot+" only when verified receipt evidence names it");
        String first=null;for(var e:defects.entrySet()) if(first==null && !e.getValue().isEmpty()) first=e.getKey();
        if(!receipt.has(controlField)) {for(List<String> d:defects.values()) for(String x:d) problems.add(id+": "+x);return;}
        String control=receipt.path(controlField).asText();
        if(!controls.containsKey(control)) {problems.add(id+": "+controlField+" "+control+" is not one of "+controls.keySet());return;}
        if(!control.equals(first)) {problems.add(id+": "+controlField+" "+control+" is not the product's first failing receiving-custody check ("+(first==null?"none: the slot is valid and evidenced":first+" "+defects.get(first))+")");return;}
        String action=receipt.path("id").asText();
        if(!pinsResponse(sub,action,"/response/outcome",controls.get(control)) || !pinsResponse(sub,action,"/response/error/code",control))
            problems.add(id+": declared custody negative "+control+" must pin /response/outcome equals "+controls.get(control)+" and /response/error/code equals "+control);
        int at=-1;for(int i=0;i<actions.size();i++) if(actions.get(i)==receipt) at=i;
        Set<String> later=new HashSet<>();for(int i=at+1;i<actions.size();i++) if(actions.get(i).path("kind").asText().equals("observe")) later.add(actions.get(i).path("id").asText());
        boolean zero=false;
        for(JsonNode x:sub.path("assertions")) zero|=x.path("op").asText().equals("count") && x.path("expected").isIntegralNumber() && x.path("expected").asInt()==0
            && later.contains(x.path("source").path("actionId").asText()) && List.of("/data/rawRows/segments","/data/rawRows/receipts").contains(x.path("source").path("pointer").asText());
        if(!zero) problems.add(id+": declared custody negative "+control+" must assert zero effect: a count 0 over /data/rawRows/segments or /data/rawRows/receipts of an observe action after it");
    }
    /**
     * Collects the custody statements of the verification-basis originals a request names (directReceiptCustody rule 2,
     * step2r round 9): DocumentVersion aliases in its verificationBasisSlots (and, for a runtime attachment, its document
     * slot), and the documents earlier actions attached at runtime when a basis slot cites their $result. Request-level
     * evidenceRefs and other slots are witnesses, not verification bases, and do not count.
     */
    private void originals(JsonNode request,boolean attached,List<String> basis,JsonNode aliases,Map<String,JsonNode> byId,Set<String> visiting,String field,JsonNode evidence,
                           Map<String,String> stated,Set<String> cited,String id,List<String> problems) throws IOException {
        List<JsonNode> evidenceNodes=new ArrayList<>();
        for(String key:basis) if(request.path("slots").has(key)) evidenceNodes.add(request.path("slots").path(key));
        if(attached && request.path("slots").has("document")) evidenceNodes.add(request.path("slots").path("document"));
        Set<String> docs=new LinkedHashSet<>();
        for(JsonNode n:evidenceNodes) for(String x:aliasRefs(n)) if(aliases.path(x).path("type").asText().equals("DocumentVersion")) docs.add(x);
        List<JsonNode> flat=new ArrayList<>();for(JsonNode n:evidenceNodes) {JsonNode v=n.has("value") && !n.has("$alias")?n.path("value"):n;if(v.isArray()) v.forEach(flat::add);else flat.add(v);}
        for(JsonNode n:flat) {String x=plainText(n);if(x!=null && aliases.path(x).path("type").asText().equals("DocumentVersion")) docs.add(x);}
        for(String doc:docs) {
            cited.add(doc);JsonNode a=aliases.path(doc);
            if(!a.path("fixtureContent").has(field)) continue;
            stated.put(doc,a.path("fixtureContent").path(field).asText());
            String expected=Json.sha256Text(CANONICAL.writeValueAsString(Json.MAPPER.treeToValue(a.path("fixtureContent"),Object.class)));
            boolean hashed=false;for(JsonNode row:evidence) if(row.path("alias").asText().equals(doc)) hashed|=row.path("sha256").asText().equals(expected);
            if(!hashed) problems.add(id+": fixture evidence sha256 of "+doc+" is not the SHA-256 of its canonical fixtureContent, so the original naming "+stated.get(doc)+" is not the hashed one");
        }
        for(JsonNode n:flat) for(JsonNode r:resultNodes(n)) {
            String source=r.path("actionId").asText();JsonNode attachment=byId.get(source);
            if(attachment==null || !visiting.add(source)) continue;
            cited.add("$result "+source);
            JsonNode document=attachment.path("request").path("slots").path("document");
            if(document.path("content").isTextual()) {
                JsonNode content;try {content=Json.MAPPER.readTree(document.path("content").asText());} catch(IOException unstructured) {content=null;}
                if(content!=null && content.path(field).isTextual()) {
                    stated.put("$result "+source,content.path(field).asText());
                    if(!document.path("sha256").asText().equals(Json.sha256Text(document.path("content").asText())))
                        problems.add(id+": runtime original "+source+" sha256 is not the SHA-256 of its inline content naming "+content.path(field).asText());
                }
            }
            originals(attachment.path("request"),true,basis,aliases,byId,visiting,field,evidence,stated,cited,id,problems);
        }
    }
    private static boolean pinsResponse(JsonNode sub,String actionId,String pointer,String expected) {
        for(JsonNode x:sub.path("assertions")) if(x.path("op").asText().equals("equals") && x.path("source").path("actionId").asText().equals(actionId)
                && x.path("source").path("pointer").asText().equals(pointer) && x.path("expected").asText().equals(expected)) return true;
        return false;
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
