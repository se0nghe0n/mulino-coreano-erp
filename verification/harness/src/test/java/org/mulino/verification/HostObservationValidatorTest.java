package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Fixed, explicitly synthetic captures exercise the validator, not a process/product adapter. */
public final class HostObservationValidatorTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private final String dir="verification/harness/src/test/resources/host-observation/";
    private static final Set<String> GENERATES=Set.of("archiveInventory","archiveRestore","dataInventory","schemaInstall","schemaUpgrade","compileSchema","compilerSchemaProbe","backup","restore","cutoverStage","deploymentProbe","clientProbe","retentionSweep","verifyCoverage");
    record Capture(ObjectNode control,ObjectNode host,StepResult result) {}
    private ObjectNode artifact(String name) throws Exception {
        ObjectNode a=Json.object();String path=dir+name;a.put("path",path).put("sha256",Json.sha256(root.resolve(path))).put("sizeBytes",Files.size(root.resolve(path))).put("completeness","COMPLETE");
        a.set("scope",Json.parse("{\"workspace\":\"host-selftest\"}"));return a;
    }
    private ObjectNode command(String operation) {
        ObjectNode c=Json.object();c.set("argv",Json.MAPPER.valueToTree(List.of("CAPTURED_CONTRACT_SELFTEST_ONLY",operation)));
        c.put("exitCode",0).put("startedAt","2026-10-07T00:00:00Z").put("completedAt","2026-10-07T00:00:03Z").put("transcriptRef",dir+"transcript.txt").put("redacted",true);return c;
    }
    Capture capture(String operation) throws Exception {return capture(operation,operation+"-rows.json");}
    Capture capture(String operation,String rowsName) throws Exception {
        JsonNode rows=Json.read(root.resolve(dir+rowsName));ObjectNode params=Json.object();params.set("scope",Json.parse("{\"workspace\":\"host-selftest\"}"));
        if(rows.path("operationEvidence").has("schedulerId")) params.set("schedulerId",rows.path("operationEvidence").path("schedulerId"));
        ObjectNode host=Json.object();host.put("schemaVersion","1.0.0").put("evidenceClass","CAPTURED_SELFTEST").put("operation",operation).put("scopeComplete",true);
        host.set("scope",params.path("scope").deepCopy());host.set("command",command(operation));host.set("toolVersions",Json.parse("[{\"name\":\"CAPTURED_SELFTEST_ONLY\",\"version\":\"1.0.0\"}]"));
        String profile=operation.equals("deploymentProbe")?"BTP":operation.equals("clientProbe")?"CLIENT":"LOCAL";
        ObjectNode env=Json.object();env.put("profile",profile).put("hostId","synthetic-host").put("workspaceId","host-selftest").put("isolated",true);host.set("environment",env);
        host.set("inputArtifacts",Json.array());host.set("generatedOutputs",Json.array());host.set("observedArtifacts",Json.array());host.set("operationEvidence",rows.path("operationEvidence").deepCopy());
        ObjectNode extractor=Json.object();extractor.put("name","fixed-contract-capture").put("version","1.0.0").put("source","FILESYSTEM_READ_ONLY").put("independent",true).put("readOnly",true);extractor.set("command",command("read-only-extract"));
        ArrayNode inputs=Json.array();inputs.add(artifact(rowsName));extractor.set("inputArtifacts",inputs);extractor.set("rawRows",rows.deepCopy());extractor.put("rawRowsArtifactRef",dir+rowsName);host.set("extractor",extractor);
        List<String> refs=new ArrayList<>(List.of(dir+"transcript.txt",dir+rowsName));
        if(GENERATES.contains(operation)) ((ArrayNode)host.path("generatedOutputs")).add(artifact(rowsName));
        if(Set.of("inspectArtifacts","scanArtifacts").contains(operation)) {
            ArrayNode list=Json.array();list.add(artifact("safe.txt"));list.add(artifact("sentinel.txt"));params.set("artifacts",list.deepCopy());host.set("observedArtifacts",list.deepCopy());list.forEach(inputs::add);
            if(operation.equals("scanArtifacts")) {
                params.set("patterns",Json.parse("[{\"id\":\"virtual-secret\",\"literal\":\"SYNTHETIC_SECRET_SENTINEL\"}]"));
                ArrayNode reads=Json.array();for(JsonNode a:list) {
                    ObjectNode read=Json.object();read.set("artifact",a.deepCopy());read.set("digest",a.path("sha256"));read.set("bytesRead",a.path("sizeBytes"));
                    // Fixed UTF-8 byte positions in the committed sentinel sample, independently asserted below.
                    read.set("findings",a.path("path").asText().endsWith("sentinel.txt")?Json.parse("[{\"patternId\":\"virtual-secret\",\"byteOffset\":15,\"lengthBytes\":25},{\"patternId\":\"virtual-secret\",\"byteOffset\":41,\"lengthBytes\":25}]"):Json.array());reads.add(read);
                }host.set("reads",reads);
            }
        }
        if(operation.equals("verifyCoverage")) {
            params.put("registryPath",dir+"coverage-registry.json").put("catalogPath",dir+"coverage-catalog.json").put("inputSnapshotKind","PREPARATION").put("preparationReportPath",dir+"coverage-preparation.json");
            for(String name:List.of("coverage-registry.json","coverage-catalog.json","coverage-preparation.json")) ((ArrayNode)host.path("inputArtifacts")).add(artifact(name));
        }
        if(operation.equals("enumerateWriteSurface")) {
            String allowlist="contracts/acceptance-capabilities.json";
            params.put("environmentId","synthetic-environmentId").put("enumerationId","synthetic-enumerationId").put("actorRef","reader");
            params.set("surfaces",Json.parse("[\"ODATA_METADATA\",\"MCP_TOOLS_LIST\"]"));params.set("probeClasses",Json.parse("[\"UNBOUND_ACTION\",\"MCP_TOOL_CALL\"]"));
            params.put("allowlistRef",allowlist).put("allowlistSha256",Json.sha256(root.resolve(allowlist))).put("targetPolicy","SYNTHETIC_FIXTURE_ENTITIES_ONLY");
            ObjectNode a=Json.object();a.put("path",allowlist).put("sha256",Json.sha256(root.resolve(allowlist))).put("sizeBytes",Files.size(root.resolve(allowlist))).put("completeness","COMPLETE");a.set("scope",Json.parse("{\"workspace\":\"host-selftest\"}"));
            ((ArrayNode)host.path("inputArtifacts")).add(a);
            for(String name:List.of("safe.txt","sentinel.txt")) {((ArrayNode)host.path("observedArtifacts")).add(artifact(name));inputs.add(artifact(name));}
        }
        if(operation.equals("awaitRuntimeTask")) {
            JsonNode snap=Json.read(root.resolve(dir+"runtime-snapshot.json"));ObjectNode task=Json.object();
            for(String key:List.of("taskId","invocationHandle","origin","terminalStatus","completedAt")) task.set(key,snap.path(key));
            ObjectNode snapshot=Json.object();snapshot.set("id",snap.path("snapshotId"));snapshot.set("capturedAt",snap.path("capturedAt"));snapshot.put("artifactRef",dir+"runtime-snapshot.json");task.set("snapshot",snapshot);host.set("runtimeTask",task);
            params.set("taskId",snap.path("taskId"));params.set("invocationHandle",snap.path("invocationHandle"));inputs.add(artifact("runtime-snapshot.json"));refs.add(dir+"runtime-snapshot.json");
        }
        if(Set.of("start","stop","restart").contains(operation)) {
            JsonNode snapshot=Json.read(root.resolve(dir+operation+"-process-snapshot.json"));host.set("processObservation",snapshot.path("processObservation").deepCopy());
            params.set("processId",rows.path("operationEvidence").path("processId"));JsonNode a=artifact(operation+"-process-snapshot.json");inputs.add(a);((ArrayNode)host.path("generatedOutputs")).add(a);refs.add(dir+operation+"-process-snapshot.json");
        }
        host.set("requestedInputs",params.deepCopy());ObjectNode control=Json.object();control.put("type","process").put("operation",operation).set("parameters",params);
        ObjectNode data=Json.object();data.put("acknowledged",true).put("controlType","process").put("operation",operation).put("acknowledgedAt","2026-10-07T00:00:03Z");data.set("hostObservation",host);
        ObjectNode provenance=Json.object();provenance.put("adapter","captured-contract-selftest").put("adapterVersion","1.0.0").put("buildVersion","SELFTEST").putNull("authenticatedActor").put("source","CANNED_CONTRACT_SELFTEST").put("independent",false).put("scopeComplete",true).putNull("sourceQuery").putNull("snapshot");
        return new Capture(control,host,new StepResult("captured-host",StepResult.DriverStatus.EXECUTED,data,null,"CAPTURED_SELFTEST only; no actual host process",provenance,refs));
    }
    private void check(Capture c) throws Exception {HostObservationValidator.validate(new ContractValidator(root),c.control,c.result);}
    @Test void everyBoundedOperationAcceptsFixedObservationButDoesNotExecuteAProcess() throws Exception {
        JsonNode schema=Json.read(root.resolve("contracts/acceptance-host-observation.schema.json"));
        for(JsonNode op:schema.path("properties").path("operation").path("enum")) check(capture(op.asText()));
    }
    private String rejectSurface(java.util.function.Consumer<Capture> change) throws Exception {
        Capture c=capture("enumerateWriteSurface");change.accept(c);StepResult result=rebind(c);
        return assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(new ContractValidator(root),c.control,result)).getMessage();
    }
    /** Mutated rows are written to a disposable copy so the extractor artifact still matches its bytes. */
    private StepResult rebind(Capture c) throws Exception {
        ObjectNode rows=(ObjectNode)c.host.path("extractor").path("rawRows");
        Path copy=Files.createTempFile(root.resolve("verification/harness/target"),"captured-rows-",".json");Json.write(copy,rows);
        String ref=root.relativize(copy).toString();((ObjectNode)c.host.path("extractor")).put("rawRowsArtifactRef",ref);
        ObjectNode a=Json.object();a.put("path",ref).put("sha256",Json.sha256(copy)).put("sizeBytes",Files.size(copy)).put("completeness","COMPLETE");a.set("scope",Json.parse("{\"workspace\":\"host-selftest\"}"));
        String old=c.host.path("extractor").path("inputArtifacts").get(0).path("path").asText();
        ((ArrayNode)c.host.path("extractor").path("inputArtifacts")).set(0,a);
        ArrayNode generated=(ArrayNode)c.host.path("generatedOutputs");
        for(int i=0;i<generated.size();i++) if(generated.get(i).path("path").asText().equals(old)) generated.set(i,a.deepCopy());
        List<String> refs=new ArrayList<>(c.result.artifactRefs());refs.add(ref);
        return new StepResult(c.result.actionId(),c.result.driverStatus(),c.result.data(),null,c.result.reason(),c.result.provenance(),refs);
    }
    /** Changes what the scheduler/verifier recorded (rows and the typed identity together) and returns the rejection message. */
    private String rejectRows(Capture c,java.util.function.Consumer<ObjectNode> change) throws Exception {
        ObjectNode rows=(ObjectNode)c.host.path("extractor").path("rawRows");change.accept(rows);
        c.host.set("operationEvidence",rows.path("operationEvidence").deepCopy());StepResult result=rebind(c);
        return assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(new ContractValidator(root),c.control,result)).getMessage();
    }
    /** V4 exposed-write-surface: the enumeration contract is defined, and the harness recomputes what the extractor claims. */
    @Test void writeSurfaceEnumerationIsCheckedAgainstTheCommittedAllowlistAndProbeRows() throws Exception {
        check(capture("enumerateWriteSurface"));
        assertTrue(rejectSurface(c->((ObjectNode)c.host.path("extractor").path("rawRows").path("surfaceItems").get(0)).put("writeCapable",true)).contains("never probed"));
        assertTrue(rejectSurface(c->((ObjectNode)c.host.path("extractor").path("rawRows").path("surfaceItems").get(1)).put("capabilityId","rawCoreInsert")).contains("allowlist"),
            "an extractor cannot mark an unlisted write item allowlisted");
        assertTrue(rejectSurface(c->((ArrayNode)c.host.path("extractor").path("rawRows").path("surfaces")).remove(1)).contains("omitted requested surfaces"));
        assertTrue(rejectSurface(c->((ObjectNode)c.host.path("extractor").path("rawRows").path("surfaces").get(0)).put("itemCount",5)).contains("itemCount"));
        assertTrue(rejectSurface(c->((ObjectNode)c.host.path("extractor").path("rawRows").path("probeCoverage").get(0)).put("applicableTargets",2)).contains("applicability policy"));
        assertTrue(rejectSurface(c->((ObjectNode)c.host.path("extractor").path("rawRows").path("probeCoverage").get(0)).put("probedTargets",0).put("complete",false)).contains("probedTargets"));
        assertTrue(rejectSurface(c->((ArrayNode)c.host.path("extractor").path("rawRows").path("probeCoverage")).remove(1)).contains("omitted requested classes"));
        assertTrue(rejectSurface(c->((ObjectNode)c.host.path("extractor").path("rawRows").path("probes").get(0)).put("target","Unknown")).contains("not an enumerated surface item"));
        assertTrue(rejectSurface(c->((ObjectNode)c.host.path("extractor").path("rawRows").path("probes").get(0)).put("outcome","DENIED")).contains("outcome"));
        assertTrue(rejectSurface(c->{((ObjectNode)c.control.path("parameters")).put("targetPolicy","ANY");c.host.set("requestedInputs",c.control.path("parameters").deepCopy());}).contains("synthetic"));
    }
    @Test void preparationRejectsAProcessOperationOutsideTheHostVocabulary() throws Exception {
        ContractValidator v=new ContractValidator(root);
        assertEquals(List.of(),v.hostOperationProblems(Json.read(root.resolve("verification/cases/V4/case.json"))),"enumerateWriteSurface is defined");
        ObjectNode c=(ObjectNode)Json.parse("{\"caseId\":\"V4\",\"subcases\":[{\"id\":\"s\",\"actions\":[{\"id\":\"x\",\"kind\":\"control\",\"control\":{\"type\":\"process\",\"operation\":\"listEverything\",\"parameters\":{}}}]}]}");
        List<String> problems=v.hostOperationProblems(c);
        assertEquals(1,problems.size());assertTrue(problems.get(0).contains("listEverything is not defined"),problems.toString());
    }
    @Test void unavailableAdaptersRemainUnavailableAndAckCannotReplaceObservation() throws Exception {
        Capture c=capture("inspectArtifacts");StepResult missing=StepResult.missing("missing","actual process adapter NOT_IMPLEMENTED");
        HostObservationValidator.validate(new ContractValidator(root),c.control,missing);assertNull(missing.data());assertEquals(StepResult.DriverStatus.NOT_IMPLEMENTED,missing.driverStatus());
        ((ObjectNode)c.result.data()).remove("hostObservation");assertThrows(IllegalArgumentException.class,()->check(c));
    }
    @Test void inspectionMustMatchEveryRequestedFileHashSizeScopeAndCompleteness() throws Exception {
        for(String field:List.of("path","sha256","sizeBytes","scope","completeness")) {
            Capture c=capture("inspectArtifacts");ObjectNode a=(ObjectNode)c.host.path("observedArtifacts").get(0);
            switch(field) {case "path"->a.put(field,dir+"sentinel.txt");case "sha256"->a.put(field,"0".repeat(64));case "sizeBytes"->a.put(field,0);case "scope"->a.set(field,Json.parse("{\"workspace\":\"wrong\"}"));default->a.put(field,"PARTIAL");}
            assertThrows(IllegalArgumentException.class,()->check(c),field);
        }
        Capture missing=capture("inspectArtifacts");((ArrayNode)missing.host.path("observedArtifacts")).remove(1);assertThrows(IllegalArgumentException.class,()->check(missing));
        Capture zero=capture("inspectArtifacts");zero.host.set("observedArtifacts",Json.array());assertThrows(IllegalArgumentException.class,()->check(zero));
        Capture request=capture("inspectArtifacts");((ObjectNode)request.control.path("parameters").path("artifacts").get(0)).put("sha256","0".repeat(64));request.host.set("requestedInputs",request.control.path("parameters").deepCopy());assertThrows(IllegalArgumentException.class,()->check(request));
    }
    @Test void scannerMustReadFullBytesAndDetectPositiveSyntheticSentinels() throws Exception {
        Capture c=capture("scanArtifacts");check(c);assertEquals(2,c.host.path("reads").get(1).path("findings").size());
        Capture omitted=capture("scanArtifacts");((ObjectNode)omitted.host.path("reads").get(1)).set("findings",Json.array());assertThrows(IllegalArgumentException.class,()->check(omitted));
        Capture bytes=capture("scanArtifacts");((ObjectNode)bytes.host.path("reads").get(1)).put("bytesRead",0);assertThrows(IllegalArgumentException.class,()->check(bytes));
        Capture wrongOffset=capture("scanArtifacts");((ObjectNode)wrongOffset.host.path("reads").get(1).path("findings").get(0)).put("byteOffset",14);assertThrows(IllegalArgumentException.class,()->check(wrongOffset));
        Capture missing=capture("scanArtifacts");((ArrayNode)missing.host.path("reads")).remove(1);assertThrows(IllegalArgumentException.class,()->check(missing));
        Capture digest=capture("scanArtifacts");((ObjectNode)digest.host.path("reads").get(1)).put("digest","0".repeat(64));assertThrows(IllegalArgumentException.class,()->check(digest));
        Capture regex=capture("scanArtifacts");((ObjectNode)regex.control.path("parameters").path("patterns").get(0)).put("regex",".*");regex.host.set("requestedInputs",regex.control.path("parameters").deepCopy());assertThrows(IllegalArgumentException.class,()->check(regex));
    }
    @Test void generatedHashesComeFromActualCaptureAndSupportLaterExactInspection() throws Exception {
        Capture backup=capture("backup");check(backup);assertFalse(backup.control.path("parameters").has("outputs"));
        Capture inspect=capture("inspectArtifacts");ArrayNode generated=(ArrayNode)backup.host.path("generatedOutputs").deepCopy();((ObjectNode)inspect.control.path("parameters")).set("artifacts",generated.deepCopy());inspect.host.set("requestedInputs",inspect.control.path("parameters").deepCopy());inspect.host.set("observedArtifacts",generated.deepCopy());((ArrayNode)inspect.host.path("extractor").path("inputArtifacts")).add(generated.get(0));check(inspect);
        ((ObjectNode)generated.get(0)).put("sha256","0".repeat(64));backup.host.set("generatedOutputs",generated);assertThrows(IllegalArgumentException.class,()->check(backup));
    }
    @Test void runtimeTerminalRequiresSameAutonomousTaskActualSnapshotAndCompletion() throws Exception {
        Capture failed=capture("awaitRuntimeTask");check(failed);assertEquals("FAILED",failed.host.path("runtimeTask").path("terminalStatus").asText());
        for(String key:List.of("taskId","invocationHandle","origin","terminalStatus","completedAt")) {
            Capture c=capture("awaitRuntimeTask");((ObjectNode)c.host.path("runtimeTask")).put(key,key.equals("terminalStatus")?"SUBMITTED":key.equals("completedAt")?"2026-10-07T00:00:05Z":"other");assertThrows(IllegalArgumentException.class,()->check(c),key);
        }
        Capture ack=capture("awaitRuntimeTask");ack.host.remove("runtimeTask");assertThrows(IllegalArgumentException.class,()->check(ack));
        Capture direct=capture("awaitRuntimeTask");((ArrayNode)direct.host.path("command").path("argv")).add("resumeWork");assertThrows(IllegalArgumentException.class,()->check(direct));
        Capture snapshot=capture("awaitRuntimeTask");((ObjectNode)snapshot.host.path("runtimeTask").path("snapshot")).put("id","wrong-token");assertThrows(IllegalArgumentException.class,()->check(snapshot));
    }
    @Test void actualProfileAndExtractorCannotBeReplacedByLocalSuccessOrServerBoolean() throws Exception {
        Capture btp=capture("deploymentProbe");((ObjectNode)btp.host.path("environment")).put("profile","LOCAL");assertThrows(IllegalArgumentException.class,()->check(btp));
        Capture client=capture("clientProbe");((ObjectNode)client.host.path("environment")).put("profile","LOCAL");assertThrows(IllegalArgumentException.class,()->check(client));
        Capture independent=capture("inspectArtifacts");((ObjectNode)independent.host.path("extractor")).put("independent",false);assertThrows(IllegalArgumentException.class,()->check(independent));
        Capture source=capture("scanArtifacts");((ObjectNode)source.host.path("extractor")).put("source","PRODUCT_LEAK_COUNT_ZERO");assertThrows(IllegalArgumentException.class,()->check(source));
        Capture wrongRows=capture("backup");((ObjectNode)wrongRows.host.path("extractor").path("rawRows").path("operationEvidence")).put("backupId","forged");assertThrows(IllegalArgumentException.class,()->check(wrongRows));
        Capture identity=capture("backup");((ObjectNode)identity.host.path("operationEvidence")).put("backupId","forged");assertThrows(IllegalArgumentException.class,()->check(identity));
        Capture unknown=capture("inspectArtifacts");unknown.host.put("operation","runShell");assertThrows(IllegalArgumentException.class,()->check(unknown));
        Capture actual=capture("inspectArtifacts");actual.host.put("evidenceClass","ACTUAL_HOST");assertThrows(IllegalArgumentException.class,()->check(actual));
    }
    @Test void actualCommandCompletionAndTranscriptLinksAreRequired() throws Exception {
        Capture time=capture("backup");((ObjectNode)time.host.path("command")).put("completedAt","2026-10-06T00:00:00Z");assertThrows(IllegalArgumentException.class,()->check(time));
        Capture missing=capture("backup");((ObjectNode)missing.host.path("command")).put("transcriptRef",dir+"not-existing.txt");assertThrows(IllegalArgumentException.class,()->check(missing));
        Capture exit=capture("backup");((ObjectNode)exit.host.path("extractor").path("command")).put("exitCode",1);assertThrows(IllegalArgumentException.class,()->check(exit));
    }
    @Test void relabelledSelftestRowsTranscriptAndSnapshotCannotBecomeActualEvidence() throws Exception {
        Capture c=capture("awaitRuntimeTask");c.host.put("evidenceClass","ACTUAL_HOST");((ObjectNode)c.result.provenance()).put("source","ACTUAL_HOST_PROCESS");
        assertTrue(assertThrows(IllegalArgumentException.class,()->check(c)).getMessage().contains("evidence class"));
        Capture rows=capture("backup");((ObjectNode)rows.host.path("extractor").path("rawRows")).put("evidenceClass","ACTUAL_HOST");
        assertThrows(IllegalArgumentException.class,()->check(rows));
        Capture scope=capture("backup");scope.host.set("scope",Json.parse("{\"workspace\":\"forged\"}"));((ObjectNode)scope.control.path("parameters")).set("scope",scope.host.path("scope"));scope.host.set("requestedInputs",scope.control.path("parameters").deepCopy());
        assertThrows(IllegalArgumentException.class,()->check(scope));
        Capture requested=capture("backup");((ObjectNode)requested.control.path("parameters")).put("backupId","different");assertThrows(IllegalArgumentException.class,()->check(requested));
        Capture future=capture("awaitRuntimeTask");((ObjectNode)future.host.path("runtimeTask").path("snapshot")).put("capturedAt","2026-10-07T00:00:05Z");assertThrows(IllegalArgumentException.class,()->check(future));
    }
    private Capture naturalTick(String operation) throws Exception {return naturalTick(operation,operation+"-natural-rows.json");}
    private Capture naturalTick(String operation,String rowsName) throws Exception {
        Capture c=capture(operation,rowsName);ObjectNode params=(ObjectNode)c.control.path("parameters");
        c.host.set("command",Json.read(root.resolve(dir+rowsName)).path("command").deepCopy()); // the watcher's own argv/interval
        params.put("trigger","OBSERVE_NEXT_NATURAL_TICK").put("observationWindowSeconds",30).put("triggeredBy","SCHEDULER_LOOP");
        c.host.set("requestedInputs",params.deepCopy());return c;
    }
    private static void reshape(Capture c,java.util.function.Consumer<ObjectNode> change) {
        ObjectNode params=(ObjectNode)c.control.path("parameters");change.accept(params);c.host.set("requestedInputs",params.deepCopy());
    }
    @Test void naturalTickIsPassiveObservationWithinTheWindowAndNeverAHarnessTrigger() throws Exception {
        for(String op:List.of("tickScheduler","sweepDue")) check(naturalTick(op));
        // A harness-triggered tick (no passive request) cannot claim the loop's natural trigger recorded in the rows.
        Capture relabel=naturalTick("tickScheduler");reshape(relabel,p->{p.remove("trigger");p.remove("triggeredBy");p.remove("observationWindowSeconds");});
        assertTrue(assertThrows(IllegalArgumentException.class,()->check(relabel)).getMessage().contains("cannot be reported"));
        Capture otherTrigger=naturalTick("tickScheduler");reshape(otherTrigger,p->p.put("trigger","HARNESS_TICK"));assertThrows(IllegalArgumentException.class,()->check(otherTrigger));
        Capture harness=naturalTick("tickScheduler");reshape(harness,p->p.put("triggeredBy","HARNESS"));assertThrows(IllegalArgumentException.class,()->check(harness));
        for(int window:List.of(0,31)) {Capture w=naturalTick("sweepDue");reshape(w,p->p.put("observationWindowSeconds",window));assertThrows(IllegalArgumentException.class,()->check(w),""+window);}
        Capture shortWindow=naturalTick("tickScheduler");reshape(shortWindow,p->p.put("observationWindowSeconds",2));assertThrows(IllegalArgumentException.class,()->check(shortWindow));
        Capture longCommand=naturalTick("tickScheduler","tickScheduler-natural-long-rows.json");
        assertTrue(assertThrows(IllegalArgumentException.class,()->check(longCommand)).getMessage().contains("outlasted"));
        Capture trigger=naturalTick("sweepDue","sweepDue-natural-trigger-rows.json");
        assertTrue(assertThrows(IllegalArgumentException.class,()->check(trigger)).getMessage().contains("must not trigger"));
        Capture writable=naturalTick("tickScheduler");((ObjectNode)writable.host.path("extractor")).put("readOnly",false);assertThrows(IllegalArgumentException.class,()->check(writable));
    }
    /** Round 4 item 5: the scheduler's own submission rows, not an echo of the watcher's request parameters, carry the evidence. */
    @Test void naturalTickEvidenceComesFromSchedulerRowsNotRequestEchoes() throws Exception {
        for(String key:List.of("trigger","triggeredBy","observationWindowSeconds")) {
            Capture echo=naturalTick("tickScheduler");
            String message=rejectRows(echo,r->((ObjectNode)r.path("operationEvidence")).set(key,echo.control.path("parameters").path(key).deepCopy()));
            // The schema already forbids the field; the validator names the echo when a schema would allow it.
            assertTrue(message.contains("echo") || message.contains(key),key+": "+message);
        }
        assertTrue(rejectRows(naturalTick("sweepDue"),r->r.remove("schedulerSubmissions")).contains("schedulerSubmissions"));
        assertTrue(rejectRows(naturalTick("tickScheduler"),r->((ObjectNode)r.path("schedulerSubmissions").get(0)).remove("submittedBy")).contains("submittedBy"));
        assertTrue(rejectRows(naturalTick("tickScheduler"),r->((ObjectNode)r.path("schedulerSubmissions").get(0)).put("taskId","another-task")).contains("first scheduler-recorded"));
        assertTrue(rejectRows(naturalTick("sweepDue"),r->((ObjectNode)r.path("schedulerSubmissions").get(0)).put("sweepId","another-sweep")).contains("first scheduler-recorded"));
        assertTrue(rejectRows(naturalTick("tickScheduler"),r->((ObjectNode)r.path("schedulerSubmissions").get(0)).put("schedulerId","other-scheduler")).contains("different scheduler"));
        assertTrue(rejectRows(naturalTick("tickScheduler"),r->((ArrayNode)r.path("schedulerSubmissions")).add(((ObjectNode)r.path("schedulerSubmissions").get(0).deepCopy()).put("submittedAt","2026-10-07T00:00:40Z"))).contains("outside the observation window"));
        // An earlier extra row means the identity is not the first submission of the window.
        assertTrue(rejectRows(naturalTick("tickScheduler"),r->((ArrayNode)r.path("schedulerSubmissions")).add(((ObjectNode)r.path("schedulerSubmissions").get(0).deepCopy()).put("taskId","earlier").put("submittedAt","2026-10-07T00:00:01Z"))).contains("first scheduler-recorded"));
        assertTrue(rejectRows(naturalTick("tickScheduler"),r->{ObjectNode e=(ObjectNode)r.path("operationEvidence");e.remove(List.of("taskId","invocationHandle","submittedAt"));e.put("submissionStatus","NO_TASK");}).contains("NO_TASK"));
        // NO_TASK with no scheduler row in the window is a valid observation (the case's DB assertions then fail).
        Capture none=naturalTick("tickScheduler");ObjectNode rows=(ObjectNode)none.host.path("extractor").path("rawRows");
        ObjectNode e=(ObjectNode)rows.path("operationEvidence");e.remove(List.of("taskId","invocationHandle","submittedAt"));e.put("submissionStatus","NO_TASK");rows.set("schedulerSubmissions",Json.array());
        none.host.set("operationEvidence",e.deepCopy());StepResult result=rebind(none);
        HostObservationValidator.validate(new ContractValidator(root),none.control,result);
    }
    /** Round 4 item 4: a PREPARATION input names its commit and clean state, recomputed from the bound report bytes. */
    @Test void preparationInputCarriesCommitAndCleanStateOfReportAndCheckout() throws Exception {
        for(String key:List.of("codeCommit","workingTreeDirty","checkoutCommit","checkoutDirty"))
            assertTrue(rejectRows(capture("verifyCoverage"),r->((ObjectNode)r.path("input")).remove(key)).contains(key),key);
        assertTrue(rejectRows(capture("verifyCoverage"),r->((ObjectNode)r.path("input")).put("codeCommit","d".repeat(40))).contains("differs from the bound preparation report"));
        assertTrue(rejectRows(capture("verifyCoverage"),r->((ObjectNode)r.path("input")).put("workingTreeDirty",true)).contains("differs from the bound preparation report"));
        assertTrue(rejectRows(capture("verifyCoverage"),r->((ObjectNode)r.path("input")).put("checkoutDirty","false")).contains("checkoutDirty"));
        assertTrue(rejectRows(capture("verifyCoverage"),r->((ObjectNode)r.path("input")).put("checkoutCommit","HEAD")).contains("checkoutCommit"));
    }
    private Capture coverage(String kind,java.util.function.Consumer<ObjectNode> change) throws Exception {
        Capture c=capture("verifyCoverage");reshape(c,p->{p.put("inputSnapshotKind",kind);change.accept(p);});return c;
    }
    @Test void verifyCoverageReadsExactlyTheRequestedInputSnapshot() throws Exception {
        check(capture("verifyCoverage"));
        Capture none=coverage("PREPARATION",p->p.remove("inputSnapshotKind"));assertThrows(IllegalArgumentException.class,()->check(none));
        Capture unknown=coverage("LIVE",p->{});assertThrows(IllegalArgumentException.class,()->check(unknown));
        // A preparation request that also points at the runtime manifest, or a runtime request without its manifest/currentExecution.
        Capture mixed=coverage("PREPARATION",p->p.put("manifestPath",dir+"coverage-preparation.json"));assertThrows(IllegalArgumentException.class,()->check(mixed));
        Capture runtime=coverage("REQUIRED_PATH_RUNTIME_EVIDENCE",p->{p.remove("preparationReportPath");p.put("manifestPath",dir+"coverage-preparation.json");});
        assertThrows(IllegalArgumentException.class,()->check(runtime),"currentExecution required");
        Capture kindMismatch=coverage("REQUIRED_PATH_RUNTIME_EVIDENCE",p->{p.remove("preparationReportPath");p.put("manifestPath",dir+"coverage-preparation.json");p.set("currentExecution",Json.object().put("caseId","T25"));});
        assertTrue(assertThrows(IllegalArgumentException.class,()->check(kindMismatch)).getMessage().contains("input snapshot kind"));
        Capture currentOnPrep=coverage("PREPARATION",p->p.set("currentExecution",Json.object().put("caseId","T25")));assertThrows(IllegalArgumentException.class,()->check(currentOnPrep));
        Capture unbound=capture("verifyCoverage");((ArrayNode)unbound.host.path("inputArtifacts")).remove(2);assertThrows(IllegalArgumentException.class,()->check(unbound));
        Capture registry=capture("verifyCoverage");((ArrayNode)registry.host.path("inputArtifacts")).set(0,artifact("coverage-catalog.json").put("path",dir+"coverage-registry.json"));
        assertThrows(IllegalArgumentException.class,()->check(registry),"registry hash must be the read bytes");
        Capture otherInput=coverage("PREPARATION",p->p.put("preparationReportPath",dir+"coverage-catalog.json"));assertThrows(IllegalArgumentException.class,()->check(otherInput));
        Capture mutation=coverage("PREPARATION",p->p.put("mutation","dropOracle"));assertThrows(IllegalArgumentException.class,()->check(mutation));
    }
    @Test void lifecycleNeedsRequestedProcessNewInstanceAndActualTerminalSnapshot() throws Exception {
        for(String op:List.of("start","stop","restart")) check(capture(op));
        Capture identity=capture("restart");((ObjectNode)identity.host.path("processObservation").path("after")).put("processId","other");assertThrows(IllegalArgumentException.class,()->check(identity));
        Capture instance=capture("restart");((ObjectNode)instance.host.path("processObservation").path("after")).put("instanceId","synthetic-instance-before");assertThrows(IllegalArgumentException.class,()->check(instance));
        Capture state=capture("stop");((ObjectNode)state.host.path("processObservation").path("after")).put("status","RUNNING");assertThrows(IllegalArgumentException.class,()->check(state));
        Capture terminal=capture("start");terminal.host.remove("processObservation");assertThrows(IllegalArgumentException.class,()->check(terminal));
        Capture output=capture("restart");output.host.set("generatedOutputs",Json.array());assertThrows(IllegalArgumentException.class,()->check(output));
        Capture request=capture("restart");((ObjectNode)request.control.path("parameters")).put("processId","other");request.host.set("requestedInputs",request.control.path("parameters").deepCopy());assertThrows(IllegalArgumentException.class,()->check(request));
    }
}
