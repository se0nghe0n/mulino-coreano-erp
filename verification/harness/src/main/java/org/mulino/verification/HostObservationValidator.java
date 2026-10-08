package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Validates captured host evidence; never launches a command or implements product behavior. */
public final class HostObservationValidator {
    private static final Set<String> READ_OPERATIONS=Set.of("inspectArtifacts","scanArtifacts");
    private static final Set<String> LOCAL_ONLY=Set.of("archiveInventory","archiveRestore","dataInventory","schemaInstall","schemaUpgrade","compileSchema","compilerSchemaProbe","backup","restore","cutoverStage","inspectArtifacts","scanArtifacts","verifyCoverage","enumerateWriteSurface");
    private HostObservationValidator() {}

    /** resolvedControl is the already-resolved action.control object, not the enclosing action. */
    /** Harness selftest form: captured selftest evidence may be validated for internal consistency only. */
    public static void validate(ContractValidator validator, JsonNode resolvedControl, StepResult result) throws IOException {
        validate(validator,resolvedControl,result,false);
    }
    /** requireActualHost: product profile runs accept only ACTUAL_HOST evidence from ACTUAL_HOST_PROCESS. */
    public static void validate(ContractValidator validator, JsonNode resolvedControl, StepResult result, boolean requireActualHost) throws IOException {
        if(!"process".equals(resolvedControl.path("type").asText())) return;
        if(result.driverStatus()!=StepResult.DriverStatus.EXECUTED) return;
        JsonNode host=result.data()==null?null:result.data().get("hostObservation");
        ContractValidator.require(host!=null,"Process control requires independent hostObservation, not only command ACK");
        validator.schema("contracts/acceptance-host-observation.schema.json",host);
        String source=result.provenance().path("source").asText();
        if(requireActualHost) ContractValidator.require(host.path("evidenceClass").asText().equals("ACTUAL_HOST") && source.equals("ACTUAL_HOST_PROCESS"),"Product run requires ACTUAL_HOST evidence from ACTUAL_HOST_PROCESS; captured selftest evidence is not product acceptance");
        ContractValidator.require(host.path("evidenceClass").asText().equals("CAPTURED_SELFTEST") ? source.equals("CANNED_CONTRACT_SELFTEST") : source.equals("ACTUAL_HOST_PROCESS"),"Host evidence class does not match actual versus captured provenance source");
        if(host.path("evidenceClass").asText().equals("ACTUAL_HOST")) {
            ContractValidator.require(result.provenance().path("independent").asBoolean(false),"Actual host evidence class requires independent provenance");
            for(String key:List.of("adapter","adapterVersion","buildVersion")) {
                String value=result.provenance().path(key).asText().toLowerCase(Locale.ROOT);
                ContractValidator.require(!value.contains("selftest") && !value.contains("captured"),"Actual host evidence class cannot use selftest/captured driver provenance");
            }
            for(JsonNode arg:host.path("command").path("argv")) ContractValidator.require(!arg.asText().toLowerCase(Locale.ROOT).contains("selftest") && !arg.asText().toLowerCase(Locale.ROOT).contains("captured_contract"),"Actual host evidence class cannot use captured command argv");
        }
        String operation=Json.required(resolvedControl,"operation");
        ContractValidator.require(operation.equals(host.path("operation").asText()),"Host operation differs from requested process operation");
        JsonNode requested=resolvedControl.path("parameters");
        ContractValidator.require(requested.isObject() && requested.equals(host.path("requestedInputs")),"Host requestedInputs differ from resolved parameters");
        if(requested.has("scope")) ContractValidator.require(requested.path("scope").equals(host.path("scope")),"Host scope differs from request");
        ContractValidator.require(result.provenance().path("scopeComplete").asBoolean(false),"Process provenance scope incomplete");
        ContractValidator.require(!"API_PROJECTION".equals(result.provenance().path("source").asText()),"API projection is not host evidence");
        JsonNode command=host.path("command"), extractor=host.path("extractor");
        command(validator,command,result,host.path("evidenceClass").asText());
        command(validator,extractor.path("command"),result,host.path("evidenceClass").asText());
        ContractValidator.require(extractor.path("command").path("exitCode").asInt()==0,"Independent extractor did not complete successfully");
        Set<String> toolNames=new HashSet<>();
        for(JsonNode tool:host.path("toolVersions")) ContractValidator.require(toolNames.add(Json.required(tool,"name")),"Duplicate host tool version");
        for(String group:List.of("inputArtifacts","generatedOutputs","observedArtifacts")) artifacts(validator,host.path(group));
        Map<String,JsonNode> extracted=artifacts(validator,extractor.path("inputArtifacts"));
        for(JsonNode ref:extractor.path("transcriptRefs")) evidence(validator,ref.asText(),result);
        for(JsonNode artifact:host.path("generatedOutputs")) ContractValidator.require(extracted.containsKey(artifact.path("path").asText()) && extracted.get(artifact.path("path").asText()).equals(artifact),"Generated output lacks matching independent extractor input hash");
        for(JsonNode artifact:host.path("observedArtifacts")) ContractValidator.require(extracted.containsKey(artifact.path("path").asText()) && extracted.get(artifact.path("path").asText()).equals(artifact),"Observed artifact lacks matching independent extractor input hash");
        JsonNode identity=host.path("operationEvidence");
        JsonNode extractedRows=extractedRows(validator,extractor,extracted,result);
        ContractValidator.require(extractedRows.path("evidenceClass").equals(host.path("evidenceClass")),"Independent extractor artifact evidence class differs from host evidence class");
        ContractValidator.require(extractedRows.path("operationEvidence").equals(identity),"Host operation identity differs from independently extracted rows/transcript");
        ContractValidator.require(extractedRows.path("scope").equals(host.path("scope")),"Host scope differs from independently extracted rows/transcript");
        for(String key:List.of("operation","command","environment")) ContractValidator.require(extractedRows.path(key).equals(host.path(key)),"Independent artifact differs from declared host "+key);
        ContractValidator.require(extractedRows.path("driverProvenance").equals(result.provenance()),"Independent artifact differs from declared driver provenance");
        identity.fields().forEachRemaining(e->{if(requested.has(e.getKey())) ContractValidator.require(e.getValue().equals(requested.get(e.getKey())),"Host operation identity differs from requested "+e.getKey());});
        if(operation.equals("claim")) for(String key:List.of("taskId","invocationHandle")) if(requested.has(key))
            ContractValidator.require(requested.path(key).isTextual() && !requested.path(key).asText().isBlank() && requested.path(key).equals(identity.path(key)),"Claim identity missing or different from requested "+key);
        // Fail closed when an explicitly requested profile would be replaced by a local probe.
        if(requested.has("profile")) ContractValidator.require(requested.path("profile").equals(host.path("environment").path("profile")),"Host profile differs from requested profile");
        if(LOCAL_ONLY.contains(operation)) ContractValidator.require(host.path("environment").path("profile").asText().equals("LOCAL"),"Local host operation cannot establish another profile's acceptance");
        if(operation.equals("deploymentProbe")) ContractValidator.require(host.path("environment").path("profile").asText().equals("BTP") && identity.path("profile").asText().equals("BTP"),"deploymentProbe requires actual BTP environment evidence");
        if(operation.equals("clientProbe")) ContractValidator.require(host.path("environment").path("profile").asText().equals("CLIENT") && identity.path("profile").asText().equals("CLIENT"),"clientProbe requires actual client environment evidence");
        if(Set.of("schemaInstall","schemaUpgrade").contains(operation)) ContractValidator.require(identity.path("migrationOwner").asText().equals("Flyway"),"Flyway must be the only schema migration owner");
        if(operation.equals("schemaUpgrade")) ContractValidator.require(!identity.path("fromVersion").equals(identity.path("toVersion")),"schemaUpgrade must observe different source and target ontology versions");
        if(operation.equals("cutoverStage")) ContractValidator.require(Set.of("WRITE_FREEZE","FINAL_SNAPSHOT","RECONCILE","APPLY_VERSION","SMOKE_AUTH_RESUME","OPEN_WRITES","ROLLBACK_BEFORE_OPEN","STOP_AND_RECONCILE_AFTER_OPEN","FORWARD_REPAIR").contains(identity.path("stage").asText()),"Unknown bounded cutover stage");
        if(READ_OPERATIONS.contains(operation)) inspect(validator,requested,host,extracted);
        if(operation.equals("scanArtifacts")) scan(validator,requested,host);
        if(Set.of("tickScheduler","sweepDue").contains(operation)) schedulerSubmission(requested,host);
        if(operation.equals("verifyCoverage")) coverageSnapshot(validator,requested,host,extractedRows);
        if(operation.equals("enumerateWriteSurface")) writeSurface(validator,requested,host,extractedRows,result);
        if(operation.equals("awaitRuntimeTask")) runtimeTask(validator,requested,host,result);
        if(Set.of("start","stop","restart").contains(operation)) lifecycle(validator,requested,host,result);
    }
    static final String NATURAL_TICK="OBSERVE_NEXT_NATURAL_TICK",SCHEDULER_LOOP="SCHEDULER_LOOP";
    /** Plan §10 development/CI recovery profile: test observation limit 30 seconds. */
    static final int MAX_NATURAL_TICK_WINDOW_SECONDS=30;
    private static void schedulerSubmission(JsonNode requested,JsonNode host) {
        JsonNode identity=host.path("operationEvidence");
        ContractValidator.require(Json.required(requested,"schedulerId").equals(identity.path("schedulerId").asText()),"Scheduler submission belongs to a different requested scheduler");
        ContractValidator.require(Set.of("SUBMITTED","NO_TASK").contains(identity.path("submissionStatus").asText()),"Scheduler submissionStatus must be an observed SUBMITTED or NO_TASK");
        if(identity.path("submissionStatus").asText().equals("SUBMITTED")) {
            Instant submitted=instant(identity,"submittedAt");
            ContractValidator.require(!submitted.isBefore(instant(host.path("command"),"startedAt")) && !submitted.isAfter(instant(host.path("command"),"completedAt")),"Scheduler submission timestamp is outside actual command observation");
        }
        boolean passive=requested.has("trigger") || requested.has("triggeredBy") || requested.has("observationWindowSeconds");
        if(passive) naturalTick(requested,host);
        else ContractValidator.require(!NATURAL_TICK.equals(identity.path("trigger").asText()) && !SCHEDULER_LOOP.equals(identity.path("triggeredBy").asText()),
            "A harness-triggered tick/sweep cannot be reported as the scheduler loop's own natural tick");
    }
    /**
     * Passive observation of the scheduler loop's next natural tick (T26 autonomous-loop subcases). The harness does not
     * trigger anything: the host command only watches, read-only, for at most observationWindowSeconds, and the submission
     * identity, trigger and triggeredBy come from independently extracted scheduler rows. The case's own DB assertions check
     * that every attempt was started by the loop; this validator fixes the shape that makes that observation meaningful.
     */
    private static void naturalTick(JsonNode requested,JsonNode host) {
        JsonNode identity=host.path("operationEvidence"),command=host.path("command");
        ContractValidator.require(NATURAL_TICK.equals(requested.path("trigger").asText()),"Only trigger="+NATURAL_TICK+" is a defined passive scheduler observation");
        ContractValidator.require(SCHEDULER_LOOP.equals(requested.path("triggeredBy").asText()),"Passive tick observation requests triggeredBy="+SCHEDULER_LOOP);
        JsonNode window=requested.path("observationWindowSeconds");
        ContractValidator.require(window.isIntegralNumber() && window.asInt()>=1 && window.asInt()<=MAX_NATURAL_TICK_WINDOW_SECONDS,"observationWindowSeconds must be an integer 1.."+MAX_NATURAL_TICK_WINDOW_SECONDS+" (plan §10)");
        for(String key:List.of("trigger","triggeredBy","observationWindowSeconds"))
            ContractValidator.require(identity.has(key) && identity.path(key).equals(requested.path(key)),"Independently extracted scheduler rows must record "+key+" equal to the passive request");
        Instant start=instant(command,"startedAt"),end=instant(command,"completedAt");
        ContractValidator.require(!end.isAfter(start.plusSeconds(window.asLong())),"Passive observation command outlasted observationWindowSeconds");
        if(identity.path("submissionStatus").asText().equals("SUBMITTED"))
            ContractValidator.require(!instant(identity,"submittedAt").isAfter(start.plusSeconds(window.asLong())),"Natural tick submission observed outside the observation window");
        ContractValidator.require(host.path("extractor").path("readOnly").asBoolean(false) && host.path("extractor").path("independent").asBoolean(false),"Passive tick observation needs an independent read-only extractor");
        for(JsonNode arg:command.path("argv")) {
            String a=arg.asText().toLowerCase(Locale.ROOT);
            ContractValidator.require(!a.contains("tickscheduler") && !a.contains("sweepdue") && !a.contains("resumework") && !a.contains("fakeworker"),"Passive observation command must not trigger the scheduler, a sweep or a worker");
        }
    }

    static final Set<String> SNAPSHOT_KINDS=Set.of("PREPARATION","REQUIRED_PATH_RUNTIME_EVIDENCE","MODEL_BINDING_PREPARATION","APPROVED_MODEL_EXECUTION_EVIDENCE");
    static final Set<String> LINK_STATUSES=Set.of("PASS","FAIL","NOT_RUN","CURRENT_EXECUTION");
    /**
     * verifyCoverage reads one named input snapshot (T25). inputSnapshotKind fixes which file is the input: PREPARATION reads
     * the ./verify prepare report, REQUIRED_PATH_RUNTIME_EVIDENCE and APPROVED_MODEL_EXECUTION_EVIDENCE the actual runtime
     * manifest, MODEL_BINDING_PREPARATION the model-binding preparation report. Mixing them would let a preparation report
     * stand in for runtime evidence or make runtime expectations contradict preparation ones. currentExecution names the case
     * whose own links are reported as CURRENT_EXECUTION instead of being required as a prerequisite PASS.
     */
    private static void coverageSnapshot(ContractValidator validator,JsonNode requested,JsonNode host,JsonNode rows) throws IOException {
        String kind=requested.path("inputSnapshotKind").asText();
        ContractValidator.require(SNAPSHOT_KINDS.contains(kind),"verifyCoverage requires inputSnapshotKind "+SNAPSHOT_KINDS);
        String input=switch(kind) {case "PREPARATION"->"preparationReportPath";case "MODEL_BINDING_PREPARATION"->"modelManifestPath";default->"manifestPath";};
        for(String key:List.of("preparationReportPath","modelManifestPath","manifestPath"))
            ContractValidator.require(requested.has(key)==key.equals(input),"inputSnapshotKind "+kind+" reads exactly "+input+"; "+key+" "+(key.equals(input)?"missing":"not allowed"));
        boolean current=kind.equals("REQUIRED_PATH_RUNTIME_EVIDENCE");
        ContractValidator.require(requested.has("currentExecution")==current,"currentExecution belongs only to REQUIRED_PATH_RUNTIME_EVIDENCE");
        String currentCase=current?Json.required(requested.path("currentExecution"),"caseId"):null;
        if(current) ContractValidator.require(requested.path("currentExecution").size()==1,"currentExecution names only caseId");
        Map<String,JsonNode> inputs=new HashMap<>();for(JsonNode a:host.path("inputArtifacts")) inputs.put(a.path("path").asText(),a);
        for(String key:List.of(input,"registryPath","catalogPath")) {
            String path=Json.required(requested,key);
            ContractValidator.require(inputs.containsKey(path),"verifyCoverage input "+key+" is not bound by hash in inputArtifacts: "+path);
        }
        JsonNode identity=host.path("operationEvidence");
        ContractValidator.require(identity.path("registryHash").equals(inputs.get(requested.path("registryPath").asText()).path("sha256")),"registryHash differs from the read registry bytes");
        ContractValidator.require(identity.path("catalogHash").equals(inputs.get(requested.path("catalogPath").asText()).path("sha256")),"catalogHash differs from the read catalog bytes");
        ContractValidator.require(host.path("extractor").has("rawRows"),"verifyCoverage returns structured extractor rawRows");
        JsonNode in=rows.path("input");JsonNode bound=inputs.get(requested.path(input).asText());
        ContractValidator.require(in.path("snapshotKind").asText().equals(kind),"Verifier did not read the requested input snapshot kind");
        ContractValidator.require(in.path("path").equals(bound.path("path")) && in.path("sha256").equals(bound.path("sha256")),"Verifier input snapshot path/hash differs from the requested bound file");
        if(current) ContractValidator.require(in.path("currentExecution").equals(requested.path("currentExecution")),"Verifier currentExecution differs from request");
        String mutation=requested.path("mutation").asText("none");
        if(!mutation.equals("none")) ContractValidator.require(rows.path("mutatedInput").path("mutation").asText().equals(mutation),"Verifier mutated a different input than requested");
        List<JsonNode> links=new ArrayList<>();
        rows.path("assertionLinks").forEach(links::add);
        for(JsonNode observation:rows.path("namedObservations")) observation.path("assertionLinks").forEach(links::add);
        for(JsonNode link:links) {
            String status=link.path("status").asText();
            ContractValidator.require(LINK_STATUSES.contains(status),"Unknown assertion link status "+status);
            boolean own=current && link.path("caseId").asText().equals(currentCase);
            ContractValidator.require(own==status.equals("CURRENT_EXECUTION"),"Only the currentExecution case's links are CURRENT_EXECUTION, and all of them are: "+link.path("caseId").asText()+" "+status);
        }
    }
    static final Set<String> WRITE_SURFACES=Set.of("ODATA_METADATA","MCP_SERVER_DISCOVER","MCP_TOOLS_LIST","WORKER_HANDLER_REGISTRY","MANAGEMENT_ENDPOINTS");
    static final Set<String> PROBE_CLASSES=Set.of("DIRECT_CREATE","DIRECT_UPDATE","DIRECT_DELETE","DEEP_INSERT","UPSERT","BATCH_CHANGESET","DRAFT_ACTIVATE",
        "NESTED_NAVIGATION_CREATE","NESTED_NAVIGATION_UPDATE","NESTED_NAVIGATION_DELETE","BOUND_ACTION","UNBOUND_ACTION","MCP_TOOL_CALL","WORKER_HANDLER_SUBMIT","MANAGEMENT_ENDPOINT_WRITE");
    /** Transport-level probe results beside the command outcomes of contracts/domain-vocabulary.json. */
    static final Set<String> PROBE_TRANSPORT_OUTCOMES=Set.of("NOT_EXPOSED","UNKNOWN");
    static final String SYNTHETIC_TARGETS="SYNTHETIC_FIXTURE_ENTITIES_ONLY";
    /**
     * V4 exposed-write-surface (plan §4.2/§13.2 V4): the host command reads the running system's write surfaces itself
     * (OData $metadata, MCP server/discover and tools/list, worker handler registry, management endpoints), probes every
     * write-capable item with the requested probe classes as the READ-grant actor, and an independent read-only extractor
     * records surfaces, items, probes and per-class coverage. The harness does not trust the extractor's own allowlist
     * verdict: it re-reads the committed allowlist bytes and recomputes allowlisted for every item. It also requires every
     * write-capable item to be probed and the per-class coverage counts to equal the probe rows. Whether any probe was
     * APPLIED, committed or UNKNOWN is the case's oracle, not this validator's.
     */
    private static void writeSurface(ContractValidator validator,JsonNode requested,JsonNode host,JsonNode rows,StepResult result) throws IOException {
        for(String key:List.of("environmentId","enumerationId","actorRef","allowlistRef","allowlistSha256","targetPolicy")) Json.required(requested,key);
        ContractValidator.require(SYNTHETIC_TARGETS.equals(requested.path("targetPolicy").asText()),"enumerateWriteSurface probes only synthetic fixture entities (targetPolicy "+SYNTHETIC_TARGETS+")");
        Set<String> surfaces=requestedSet(requested.path("surfaces"),WRITE_SURFACES,"surfaces"),classes=requestedSet(requested.path("probeClasses"),PROBE_CLASSES,"probeClasses");
        String allowlistRef=requested.path("allowlistRef").asText(),allowlistSha=requested.path("allowlistSha256").asText();
        ContractValidator.require(Json.sha256(file(validator,allowlistRef)).equals(allowlistSha),"Requested allowlistSha256 differs from the committed allowlist bytes "+allowlistRef);
        boolean bound=false;for(JsonNode a:host.path("inputArtifacts")) if(a.path("path").asText().equals(allowlistRef) && a.path("sha256").asText().equals(allowlistSha)) bound=true;
        ContractValidator.require(bound,"The enumeration's allowlist is not bound by hash in inputArtifacts: "+allowlistRef);
        Set<String> allowlist=new HashSet<>();
        for(JsonNode capability:Json.read(file(validator,allowlistRef)).path("capabilities")) allowlist.add(Json.required(capability,"id"));
        ContractValidator.require(!allowlist.isEmpty(),"Allowlist has no capability ids: "+allowlistRef);
        Set<String> outcomes=new HashSet<>(PROBE_TRANSPORT_OUTCOMES);
        for(JsonNode o:Json.read(file(validator,"contracts/domain-vocabulary.json")).path("outcomes")) outcomes.add(Json.required(o,"outcome"));
        Map<String,JsonNode> observed=new HashMap<>();for(JsonNode a:host.path("observedArtifacts")) observed.put(a.path("path").asText(),a);
        Map<String,Integer> itemCounts=new HashMap<>();Set<String> seenSurfaces=new HashSet<>();
        for(JsonNode row:rows.path("surfaces")) {
            String surface=Json.required(row,"surface");
            ContractValidator.require(surfaces.contains(surface) && seenSurfaces.add(surface),"Enumerated surface is unrequested or duplicated: "+surface);
            JsonNode artifact=observed.get(row.path("sourceRef").asText());
            ContractValidator.require(artifact!=null && artifact.path("sha256").equals(row.path("sha256")),"Surface source is not a hash-bound observed artifact: "+surface);
            itemCounts.put(surface,row.path("itemCount").asInt(-1));
        }
        ContractValidator.require(seenSurfaces.equals(surfaces),"Enumeration omitted requested surfaces "+difference(surfaces,seenSurfaces));
        Map<String,Integer> actualCounts=new HashMap<>();Set<String> items=new HashSet<>(),writable=new TreeSet<>();
        for(JsonNode item:rows.path("surfaceItems")) {
            String surface=Json.required(item,"surface"),key=surface+"|"+Json.required(item,"itemId");
            ContractValidator.require(surfaces.contains(surface) && items.add(key),"Surface item is unrequested or duplicated: "+key);
            actualCounts.merge(surface,1,Integer::sum);
            boolean listed=item.path("capabilityId").isTextual() && allowlist.contains(item.path("capabilityId").asText());
            ContractValidator.require(item.path("allowlisted").isBoolean() && item.path("allowlisted").asBoolean()==listed,"Surface item allowlisted differs from the committed allowlist: "+key);
            if(item.path("writeCapable").asBoolean(false)) writable.add(key);
        }
        for(String surface:surfaces) ContractValidator.require(itemCounts.get(surface)==actualCounts.getOrDefault(surface,0),"Surface itemCount differs from its enumerated items: "+surface);
        Set<String> probeIds=new HashSet<>(),probed=new HashSet<>();Map<String,Set<String>> targetsByClass=new HashMap<>();
        for(JsonNode probe:rows.path("probes")) {
            String id=Json.required(probe,"probeId"),surface=Json.required(probe,"surface"),probeClass=Json.required(probe,"probeClass"),key=surface+"|"+Json.required(probe,"target");
            ContractValidator.require(probeIds.add(id),"Duplicate probe "+id);
            ContractValidator.require(classes.contains(probeClass),"Probe uses an unrequested probe class: "+id+" "+probeClass);
            ContractValidator.require(items.contains(key),"Probe target is not an enumerated surface item: "+id+" "+key);
            ContractValidator.require(outcomes.contains(probe.path("outcome").asText()),"Unknown probe outcome "+probe.path("outcome").asText());
            evidence(validator,Json.required(probe,"transcriptRef"),result);
            probed.add(key);targetsByClass.computeIfAbsent(probeClass,k->new HashSet<>()).add(key);
        }
        ContractValidator.require(probed.containsAll(writable),"Write-capable surface items were enumerated but never probed: "+difference(writable,probed));
        Set<String> coveredClasses=new HashSet<>();
        for(JsonNode coverage:rows.path("probeCoverage")) {
            String probeClass=Json.required(coverage,"probeClass");
            ContractValidator.require(classes.contains(probeClass) && coveredClasses.add(probeClass),"Probe coverage class is unrequested or duplicated: "+probeClass);
            int applicable=coverage.path("applicableTargets").asInt(-1),done=coverage.path("probedTargets").asInt(-1);
            ContractValidator.require(done==targetsByClass.getOrDefault(probeClass,Set.of()).size(),"Probe coverage probedTargets differs from probe rows: "+probeClass);
            ContractValidator.require(applicable>=done && coverage.path("complete").isBoolean() && coverage.path("complete").asBoolean()==(applicable==done),"Probe coverage completeness contradicts its counts: "+probeClass);
        }
        ContractValidator.require(coveredClasses.equals(classes),"Probe coverage omitted requested classes "+difference(classes,coveredClasses));
    }
    private static Set<String> requestedSet(JsonNode values,Set<String> vocabulary,String name) {
        ContractValidator.require(values.isArray() && !values.isEmpty(),"enumerateWriteSurface requires a non-empty "+name+" list");
        Set<String> set=new HashSet<>();
        for(JsonNode v:values) ContractValidator.require(vocabulary.contains(v.asText()) && set.add(v.asText()),"Unknown or duplicate requested "+name+" entry "+v.asText());
        return set;
    }
    private static Set<String> difference(Set<String> left,Set<String> right) {Set<String> d=new TreeSet<>(left);d.removeAll(right);return d;}
    private static void lifecycle(ContractValidator validator,JsonNode requested,JsonNode host,StepResult result) throws IOException {
        JsonNode observed=host.path("processObservation"),before=observed.path("before"),after=observed.path("after");
        String process=Json.required(requested,"processId");
        for(JsonNode id:List.of(observed.path("requestedProcessId"),before.path("processId"),after.path("processId"),host.path("operationEvidence").path("processId"))) ContractValidator.require(id.asText().equals(process),"Lifecycle observed process identity differs from requested processId");
        Instant start=instant(host.path("command"),"startedAt"),end=instant(host.path("command"),"completedAt"),completed=instant(observed,"completedAt");
        ContractValidator.require(!instant(before,"observedAt").isAfter(start) && !instant(after,"observedAt").isBefore(completed) && !instant(after,"observedAt").isAfter(end) && !completed.isBefore(start) && !completed.isAfter(end),"Lifecycle before/after observation does not bound actual terminal command");
        for(JsonNode state:List.of(before,after)) if(state.path("status").asText().equals("RUNNING")) ContractValidator.require(state.hasNonNull("instanceId"),"Running lifecycle process lacks actual instance identity");
        if(observed.path("terminalStatus").asText().equals("SUCCEEDED")) {
            ContractValidator.require(host.path("command").path("exitCode").asInt()==0,"Lifecycle success conflicts with actual command exit");
            String operation=host.path("operation").asText();
            ContractValidator.require(operation.equals("stop")?!after.path("status").asText().equals("RUNNING"):after.path("status").asText().equals("RUNNING"),"Lifecycle successful terminal status has wrong final process state");
            if(operation.equals("start")) ContractValidator.require(!before.path("status").asText().equals("RUNNING"),"start cannot claim to start an already-running process");
            if(operation.equals("stop")) ContractValidator.require(before.path("status").asText().equals("RUNNING"),"stop cannot claim to stop an already-stopped process");
            if(operation.equals("restart")) ContractValidator.require(before.hasNonNull("instanceId") && !before.path("status").asText().equals("NOT_PRESENT") && !before.path("instanceId").equals(after.path("instanceId")),"restart requires a previous instance and a different new process instance identity");
        }
        String ref=Json.required(observed,"artifactRef");evidence(validator,ref,result);evidenceClass(validator,ref,host.path("evidenceClass").asText());
        JsonNode snapshot=Json.read(file(validator,ref));
        for(String key:List.of("scope","operation")) ContractValidator.require(snapshot.path(key).equals(host.path(key)),"Lifecycle terminal artifact differs from declared "+key);
        ContractValidator.require(snapshot.path("processObservation").equals(observed),"Lifecycle before/after/terminal differs from actual snapshot");
        boolean linked=false;for(JsonNode a:host.path("generatedOutputs")) if(a.path("path").asText().equals(ref)) linked=true;
        ContractValidator.require(linked,"Lifecycle terminal snapshot not linked as generated output");
    }

    private static JsonNode extractedRows(ContractValidator validator,JsonNode extractor,Map<String,JsonNode> inputs,StepResult result) throws IOException {
        if(extractor.has("rawRows")) {
            String ref=Json.required(extractor,"rawRowsArtifactRef");evidence(validator,ref,result);
            ContractValidator.require(inputs.containsKey(ref),"Extractor raw rows artifact lacks input hash");
            JsonNode actual=Json.read(file(validator,ref));
            ContractValidator.require(actual.equals(extractor.path("rawRows")),"Extractor rows differ from actual read-only artifact content");return actual;
        }
        // Structured redacted transcripts support tools whose read-only extraction emits JSON.
        for(JsonNode ref:extractor.path("transcriptRefs")) {
            ContractValidator.require(inputs.containsKey(ref.asText()),"Extractor transcript lacks input hash");
            JsonNode actual=Json.read(file(validator,ref.asText()));if(actual.has("operationEvidence")) return actual;
        }
        throw new IllegalArgumentException("Extractor transcript has no independent operation evidence");
    }

    private static void command(ContractValidator validator,JsonNode command,StepResult result,String evidenceClass) throws IOException {
        Instant started=instant(command,"startedAt"),completed=instant(command,"completedAt");
        ContractValidator.require(!completed.isBefore(started),"Host command completion precedes start");
        evidence(validator,Json.required(command,"transcriptRef"),result);
        evidenceClass(validator,Json.required(command,"transcriptRef"),evidenceClass);
    }
    private static void evidenceClass(ContractValidator validator,String ref,String expected) throws IOException {
        String content=Files.readString(file(validator,ref));String actual;
        if(content.stripLeading().startsWith("{")) actual=Json.parse(content).path("evidenceClass").asText();
        else {String first=content.lines().findFirst().orElse("");actual=first.startsWith("evidenceClass=")?first.substring("evidenceClass=".length()):"";}
        ContractValidator.require(expected.equals(actual),"Host transcript/snapshot evidence class differs from declared host class: "+ref);
    }
    private static Instant instant(JsonNode node,String field) {
        try {return Instant.parse(Json.required(node,field));} catch(RuntimeException e) {throw new IllegalArgumentException("Invalid actual host instant "+field,e);}
    }
    private static void evidence(ContractValidator validator,String ref,StepResult result) throws IOException {
        ContractValidator.require(result.artifactRefs().contains(ref),"Host transcript/snapshot not linked in StepResult artifactRefs: "+ref);
        file(validator,ref);
    }
    private static Path file(ContractValidator validator,String ref) throws IOException {
        Path path=validator.path(ref);
        ContractValidator.require(Files.isRegularFile(path),"Missing host artifact "+ref);
        // A lexical in-repository symlink must not silently read an unrelated host file.
        ContractValidator.require(path.toRealPath().startsWith(validator.root().toRealPath()),"Host artifact escapes repository through symlink: "+ref);
        return path;
    }
    private static Map<String,JsonNode> artifacts(ContractValidator validator,JsonNode artifacts) throws IOException {
        Map<String,JsonNode> indexed=new LinkedHashMap<>();
        for(JsonNode a:artifacts) {
            String ref=Json.required(a,"path");Path path=file(validator,ref);
            ContractValidator.require(indexed.put(ref,a)==null,"Duplicate host artifact path "+ref);
            ContractValidator.require(Files.size(path)==a.path("sizeBytes").asLong(-1),"Host artifact bytes mismatch "+ref);
            ContractValidator.require(Json.sha256(path).equals(a.path("sha256").asText()),"Host artifact SHA-256 mismatch "+ref);
            ContractValidator.require(a.path("completeness").asText().equals("COMPLETE") && a.path("scope").isObject() && !a.path("scope").isEmpty(),"Host artifact scope/completeness absent "+ref);
        }
        return indexed;
    }
    private static void inspect(ContractValidator validator,JsonNode requested,JsonNode host,Map<String,JsonNode> extracted) throws IOException {
        JsonNode list=requested.path("artifacts");
        ContractValidator.require(list.isArray() && !list.isEmpty(),"inspect/scan requires explicit requested artifacts; count0 is not an inventory");
        Map<String,JsonNode> observed=artifacts(validator,host.path("observedArtifacts"));Set<String> paths=new HashSet<>();
        for(JsonNode expected:list) {
            String path=Json.required(expected,"path");ContractValidator.require(paths.add(path),"Duplicate requested artifact "+path);
            ContractValidator.require(expected.hasNonNull("sha256") && expected.hasNonNull("sizeBytes") && expected.path("sizeBytes").isIntegralNumber() && expected.path("scope").isObject() && !expected.path("scope").isEmpty(),"Requested artifact needs hash/bytes/scope "+path);
            JsonNode actual=observed.get(path);
            ContractValidator.require(actual!=null,"Requested artifact omitted "+path);
            for(String field:List.of("path","sha256","sizeBytes","scope")) ContractValidator.require(actual.path(field).equals(expected.path(field)),"Observed artifact differs from requested "+field+": "+path);
            ContractValidator.require(extracted.containsKey(path) && extracted.get(path).equals(actual),"Inspected artifact not independently extracted "+path);
        }
        ContractValidator.require(observed.keySet().equals(paths),"Inspection includes unrequested artifact or omits requested file");
    }
    private static void scan(ContractValidator validator,JsonNode requested,JsonNode host) throws IOException {
        JsonNode patterns=requested.path("patterns");ContractValidator.require(patterns.isArray() && !patterns.isEmpty(),"Scan needs explicit literal patterns");
        Map<String,byte[]> literals=new LinkedHashMap<>();
        for(JsonNode pattern:patterns) {
            ContractValidator.require(pattern.isObject() && pattern.size()==2 && pattern.has("id") && pattern.has("literal"),"Scan pattern permits only id/literal; no shell or regex DSL");
            String id=Json.required(pattern,"id"),literal=Json.required(pattern,"literal");
            ContractValidator.require(literals.put(id,literal.getBytes(StandardCharsets.UTF_8))==null,"Duplicate scan pattern ID");
        }
        Map<String,JsonNode> reads=new LinkedHashMap<>();
        for(JsonNode read:host.path("reads")) ContractValidator.require(reads.put(Json.required(read.path("artifact"),"path"),read)==null,"Duplicate scan read");
        Set<String> requestedPaths=new HashSet<>();
        for(JsonNode artifact:host.path("observedArtifacts")) {
            String path=Json.required(artifact,"path");requestedPaths.add(path);JsonNode read=reads.get(path);
            ContractValidator.require(read!=null && read.path("artifact").equals(artifact),"Scan did not independently read exact requested artifact "+path);
            byte[] bytes=Files.readAllBytes(file(validator,path));
            ContractValidator.require(read.path("bytesRead").asLong(-1)==bytes.length,"Scan did not read all requested bytes "+path);
            ContractValidator.require(read.path("digest").equals(artifact.path("sha256")),"Scan digest differs from requested file "+path);
            Set<String> expected=new HashSet<>();
            for(var entry:literals.entrySet()) {
                byte[] needle=entry.getValue();
                for(int at=0;at<=bytes.length-needle.length;at++) {
                    boolean same=true;for(int n=0;n<needle.length;n++) if(bytes[at+n]!=needle[n]) {same=false;break;}
                    if(same) expected.add(entry.getKey()+":"+at+":"+needle.length);
                }
            }
            Set<String> actual=new HashSet<>();
            for(JsonNode finding:read.path("findings")) ContractValidator.require(actual.add(Json.required(finding,"patternId")+":"+finding.path("byteOffset").asLong()+":"+finding.path("lengthBytes").asLong()),"Duplicate scan finding");
            ContractValidator.require(actual.equals(expected),"Scan findings differ from independently read bytes; zero count cannot hide a positive sentinel: "+path);
        }
        ContractValidator.require(reads.keySet().equals(requestedPaths),"Scan read coverage differs from requested artifact set");
    }
    private static void runtimeTask(ContractValidator validator,JsonNode requested,JsonNode host,StepResult result) throws IOException {
        JsonNode task=host.path("runtimeTask");
        ContractValidator.require(requested.hasNonNull("taskId") || requested.hasNonNull("invocationHandle"),"awaitRuntimeTask needs an actual requested taskId or invocationHandle");
        for(String key:List.of("taskId","invocationHandle")) if(requested.has(key)) ContractValidator.require(requested.path(key).equals(task.path(key)),"Runtime terminal identity differs from requested "+key);
        for(JsonNode arg:host.path("command").path("argv")) ContractValidator.require(!arg.asText().toLowerCase(Locale.ROOT).contains("resumework") && !arg.asText().toLowerCase(Locale.ROOT).contains("fakeworker"),"Autonomous task observation cannot be replaced by resumeWork/fakeworker");
        Instant completed=instant(task,"completedAt"),snapshot=instant(task.path("snapshot"),"capturedAt");
        ContractValidator.require(!snapshot.isBefore(completed),"Runtime task snapshot precedes terminal completion");
        ContractValidator.require(!completed.isAfter(instant(host.path("command"),"completedAt")),"Runtime terminal occurred after await command completed");
        evidence(validator,Json.required(task.path("snapshot"),"artifactRef"),result);
        // The terminal snapshot must be independently extracted, not merely named by a server ACK.
        String ref=task.path("snapshot").path("artifactRef").asText();boolean extracted=false;
        for(JsonNode artifact:host.path("extractor").path("inputArtifacts")) if(artifact.path("path").asText().equals(ref)) extracted=true;
        ContractValidator.require(extracted,"Runtime terminal snapshot lacks independent extractor input hash");
        JsonNode actual=Json.read(file(validator,ref));
        ContractValidator.require(actual.path("evidenceClass").equals(host.path("evidenceClass")),"Runtime snapshot evidence class differs from host class");
        for(String key:List.of("taskId","invocationHandle","origin","terminalStatus","completedAt")) ContractValidator.require(actual.path(key).equals(task.path(key)),"Runtime terminal differs from actual snapshot "+key);
        ContractValidator.require(actual.path("snapshotId").equals(task.path("snapshot").path("id")) && actual.path("capturedAt").equals(task.path("snapshot").path("capturedAt")),"Runtime snapshot token/time differs from actual host snapshot");
        ContractValidator.require(actual.path("schedulerId").equals(host.path("operationEvidence").path("schedulerId")),"Runtime snapshot belongs to a different scheduler");
        ContractValidator.require(!snapshot.isAfter(instant(host.path("command"),"completedAt")),"Runtime snapshot was captured after await command completed");
    }
}
