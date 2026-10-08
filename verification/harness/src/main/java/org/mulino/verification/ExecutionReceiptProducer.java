package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * Coverage execution receipt for one {@code ./verify <profile> --actual} run (verification/coverage/execution-receipt.schema.json).
 *
 * <p>The receipt binds the profile report, the exact case/fixture inputs and every captured artifact by path/hash/bytes to
 * one execution identity, command interval, exit code, clean code commit and the versions under test. It is integrity and
 * linkage evidence only: the coverage assembler still re-reads every byte and re-evaluates assertions, and a receipt never
 * attests that the system behind the adapter is genuine.
 *
 * <p>Only {@link #emit} decides eligibility, and it refuses whenever the run was not an actual product run: the driver must be
 * the actual adapter, every case must have run under the PRODUCT evidence policy, every executed action must carry
 * non-selftest provenance labels, the command must name the profile and select the whole discovered profile (no explicit
 * case files), the checkout must be clean at the same HEAD when Main observed it before the first case and again after the
 * run, the declared backend build commit (ACTUAL_BUILD_COMMIT) must be the recorded code commit, and the real versions must
 * be supplied. Harness selftests, the unimplemented driver, RED and preparation never reach {@link #write}. A refusal is
 * reported in the profile run output and leaves the profile NOT_RUN.
 *
 * <p>Limit: the harness cannot observe which code the running backend was built from. The backend exposes no build-info
 * endpoint, so the build commit stays a declaration and the receipt records it as {@code buildIdentity.source=
 * DECLARED_ACTUAL_BUILD_COMMIT}. The S-step native runners ({@code ./verify actual-sN}) build and launch the backend from
 * the checked-out tree themselves; the {@code ./verify <profile> --actual} path does not.
 */
final class ExecutionReceiptProducer {
    static final Set<String> RECEIPT_PROFILES=Set.of("schema","contracts","scenarios","recovery","mcp","skills");
    static final List<String> BAD_MARKERS=List.of("selftest","canned","stub","fake","captured","unimplemented");
    static final String EVIDENCE_DIR="verification/harness/target/evidence";
    static final String INDEX=EVIDENCE_DIR+"/actual-runtime-evidence-index.json";

    record Run(Path root,String profile,ObjectNode report,String reportRef,List<String> argv,String display,Instant startedAt,Instant completedAt,
               List<Path> caseFiles,ObjectNode versions) {}

    private ExecutionReceiptProducer() {}

    static ObjectNode identity(Path root,Map<String,String> env) throws IOException {
        ObjectNode id=Json.object();
        id.put("runId",UUID.randomUUID().toString());
        String host=env.getOrDefault("ACTUAL_HOST_ID","");
        if(host.isBlank()) host=InetAddress.getLocalHost().getHostName();
        id.put("hostId",host);
        id.put("workspaceId","workspace-"+Json.sha256Text(root.toRealPath().toString()).substring(0,16));
        String actor=env.getOrDefault("ACTUAL_EXECUTION_ACTOR","");
        id.put("actorId",actor.isBlank()?System.getProperty("user.name","unknown-actor"):actor);
        return id;
    }

    /** Versions under test. definition/evaluator/policy come from the executed case fixtures, the rest from the actual deployment. */
    static ObjectNode versions(String profile,Map<String,String> env,ArrayNode cases,List<String> missing) {
        ObjectNode v=Json.object();
        for(String[] key:new String[][]{{"schema","ACTUAL_SCHEMA_VERSION"},{"db","ACTUAL_DB_VERSION"},{"protocol","ACTUAL_MCP_PROTOCOL_VERSION"},{"build","ACTUAL_BUILD_COMMIT"}}) {
            String value=env.getOrDefault(key[1],"");
            if(!value.isBlank()) v.put(key[0],value);
        }
        if(!v.has("schema")) missing.add("ACTUAL_SCHEMA_VERSION");
        if(Set.of("scenarios","recovery","mcp","skills").contains(profile) && !v.has("db")) missing.add("ACTUAL_DB_VERSION");
        if(Set.of("mcp","skills").contains(profile) && !v.has("protocol")) missing.add("ACTUAL_MCP_PROTOCOL_VERSION");
        if(!v.has("build")) missing.add("ACTUAL_BUILD_COMMIT");
        v.put("tool","mulino-acceptance-harness-1.0.0 java-"+System.getProperty("java.runtime.version"));
        for(String key:List.of("definition","evaluator","policy")) {
            Set<String> seen=new TreeSet<>();
            for(JsonNode c:cases) seen.add(c.path("versions").path(key).asText(""));
            if(seen.size()==1 && !seen.iterator().next().isBlank()) v.put(key,seen.iterator().next());
            else v.put(key,"PER_CASE");
        }
        return v;
    }

    /**
     * Provenance label fields an adapter uses to name itself. Only these values are matched against {@link #BAD_MARKERS}:
     * the whole provenance also carries data (snapshot.capturedAt, sourceQuery SQL, parameters) whose keys or values may
     * contain a marker substring without saying anything about the adapter, as every real observe snapshot does.
     */
    static final List<String> PROVENANCE_LABELS=List.of("/source","/adapter","/adapterVersion","/buildVersion","/snapshot/isolation","/sourceQuery/mappingVersion");

    /** The first selftest/stub label among executed actions, or null. Structural: label values only, never keys or data. */
    static String actionMarker(JsonNode actions) {
        for(JsonNode action:actions) if("EXECUTED".equals(action.path("driverStatus").asText())) {
            JsonNode provenance=action.path("provenance");
            if(!provenance.isObject() || provenance.path("source").asText().isBlank()) return "action "+action.path("actionId").asText()+" has no provenance source label";
            for(String label:PROVENANCE_LABELS) {
                JsonNode value=provenance.at(label);
                if(!value.isValueNode()) continue;
                String text=value.asText().toLowerCase(Locale.ROOT);
                for(String marker:BAD_MARKERS) if(text.contains(marker)) return "action "+action.path("actionId").asText()+" carries selftest provenance label "+label+"="+value.asText();
            }
        }
        return null;
    }

    /** Returns null when eligible, otherwise the refusal reason. */
    static String refusal(Run run,boolean actualDriver,ArrayNode cases) throws IOException,InterruptedException {
        return refusal(run,actualDriver,cases,dirty(run.root()));
    }
    /** dirtyAfter is the gate's own git status after the report was written (tests pass it explicitly). */
    static String refusal(Run run,boolean actualDriver,ArrayNode cases,boolean dirtyAfter) {
        if(!actualDriver) return "driver is not the actual product adapter";
        if(!RECEIPT_PROFILES.contains(run.profile())) return "profile "+run.profile()+" has no coverage receipt (model/deployment need their own approved runner)";
        if(cases.isEmpty()) return "no subcase selected";
        for(JsonNode c:cases) {
            if(!"PRODUCT".equals(c.path("evidencePolicy").asText())) return "case "+c.path("caseId").asText()+"/"+c.path("subcaseId").asText()+" did not run under the PRODUCT evidence policy";
            String marker=actionMarker(c.path("actions"));
            if(marker!=null) return marker;
        }
        String labels=(run.versions().toString()+run.display()+String.join(" ",run.argv())).toLowerCase(Locale.ROOT);
        for(String marker:BAD_MARKERS) if(labels.contains(marker)) return "command/versions carry selftest marker "+marker;
        if(!run.argv().contains(run.profile())) return "command argv does not name profile "+run.profile();
        if(run.report().path("explicitCaseSelection").asBoolean(true)) return "explicit case-file selection is a partial profile run; only the whole discovered profile can carry a coverage receipt";
        if(!run.versions().path("build").asText().equals(run.report().path("codeCommit").asText())) return "ACTUAL_BUILD_COMMIT differs from the recorded code commit";
        // Observed by Main before the first case ran (preRun) and by Main/this gate after the run; never taken from the report alone.
        JsonNode before=run.report().path("preRun");
        if(!before.path("workingTreeDirty").isBoolean() || before.path("workingTreeDirty").asBoolean() || !before.path("codeCommit").asText().equals(run.report().path("codeCommit").asText()))
            return "working tree was dirty, or HEAD differed, before the run; codeCommit would not identify the tested code";
        if(run.report().path("workingTreeDirty").asBoolean(true) || dirtyAfter) return "working tree dirty after the run; codeCommit would not identify the tested code";
        return null;
    }

    /** Eligibility gate, then {@link #write}. Returns the run's receipt status record. */
    static ObjectNode emit(ContractValidator validator,Run run,boolean actualDriver,ArrayNode cases,List<String> missingVersions) throws IOException,InterruptedException {
        ObjectNode status=Json.object();
        String reason=missingVersions.isEmpty()?refusal(run,actualDriver,cases):"actual versions missing "+missingVersions;
        if(reason!=null) {status.put("status","NOT_EMITTED").put("reason",reason);return status;}
        String receiptRef=write(validator,run,cases);
        status.put("status","EMITTED").put("receiptRef",receiptRef).put("indexRef",INDEX);
        return status;
    }

    /**
     * Writes envelope documents, the receipt and the index entry. Callers outside {@link #emit} exist only in format tests
     * that write into a disposable repository; product evidence goes through the eligibility gate.
     */
    static String write(ContractValidator validator,Run run,ArrayNode cases) throws IOException {
        Path root=run.root();JsonNode report=run.report();
        String runId=report.path("executionIdentity").path("runId").asText();
        String dir=EVIDENCE_DIR+"/receipts/"+run.profile()+"-"+runId;
        ObjectNode command=Json.object();
        command.set("argv",Json.MAPPER.valueToTree(run.argv()));
        command.put("display",run.display()).put("exitCode",report.path("exitCode").asInt()).put("startedAt",run.startedAt().toString()).put("completedAt",run.completedAt().toString());
        ArrayNode artifacts=Json.array();Set<String> paths=new HashSet<>();
        ObjectNode profileDoc=envelope(report,command,run.versions(),Json.object().put("profile",run.profile()).put("runId",runId));
        profileDoc.set("profileResult",report);
        artifacts.add(writeEnvelope(root,dir+"/profile-result.json",profileDoc,paths));
        ArrayNode caseVersions=Json.array();Map<String,ObjectNode> inputs=new TreeMap<>();
        for(String ref:List.of("verification/cases/registry.json","verification/requirements/mandatory-oracles.json")) input(validator,ref,inputs);
        for(JsonNode c:cases) {
            String caseId=c.path("caseId").asText(),subcaseId=c.path("subcaseId").asText();
            ObjectNode scope=Json.object().put("profile",run.profile()).put("runId",runId).put("caseId",caseId).put("subcaseId",subcaseId);
            ObjectNode doc=envelope(report,command,run.versions(),scope);doc.set("observations",c.path("actions"));
            artifacts.add(writeEnvelope(root,dir+"/case-"+caseId+"-"+safe(subcaseId)+".json",doc,paths));
            for(JsonNode action:c.path("actions")) for(JsonNode ref:action.path("artifactRefs")) if(paths.add(ref.asText())) {
                ObjectNode raw=descriptor(validator,ref.asText());
                raw.set("scope",scope.deepCopy().put("actionId",action.path("actionId").asText()));
                raw.put("completeness","COMPLETE").put("role","RAW_CAPTURE");artifacts.add(raw);
            }
            ObjectNode cv=Json.object().put("caseId",caseId).put("subcaseId",subcaseId);
            ObjectNode v=Json.object();for(String key:List.of("definition","evaluator","policy")) v.set(key,c.path("versions").path(key));
            cv.set("versions",v);caseVersions.add(cv);
            input(validator,"verification/cases/"+caseId+"/case.json",inputs);
            fixtureInputs(validator,c.path("fixtureRef").asText(),inputs,new HashSet<>());
        }
        ObjectNode receipt=Json.object();
        receipt.put("schemaVersion","1.0.0").put("evidenceClass","ACTUAL").put("codeCommit",report.path("codeCommit").asText());
        receipt.set("executionIdentity",report.path("executionIdentity"));receipt.set("command",command);receipt.set("versions",run.versions());
        receipt.set("reportArtifact",descriptor(validator,run.reportRef()));
        receipt.set("inputs",Json.MAPPER.valueToTree(inputs.values()));receipt.set("artifacts",artifacts);
        receipt.set("caseVersions",caseVersions);receipt.put("environment","LOCAL").put("workingTreeClean",true);
        ObjectNode tree=Json.object();
        tree.set("before",Json.object().put("codeCommit",report.path("preRun").path("codeCommit").asText()).put("clean",!report.path("preRun").path("workingTreeDirty").asBoolean(true)).put("observedAt",report.path("preRun").path("observedAt").asText()));
        tree.set("after",Json.object().put("codeCommit",report.path("codeCommit").asText()).put("clean",!report.path("workingTreeDirty").asBoolean(true)).put("observedAt",report.path("timestamp").asText()));
        receipt.set("workingTreeObservations",tree);
        // The backend build commit is the ACTUAL_BUILD_COMMIT declaration; no build-info endpoint lets the harness observe it.
        receipt.set("buildIdentity",Json.object().put("commit",run.versions().path("build").asText()).put("source","DECLARED_ACTUAL_BUILD_COMMIT"));
        validator.schema("verification/coverage/execution-receipt.schema.json",receipt);
        String receiptRef=EVIDENCE_DIR+"/"+run.profile()+"-receipt.json";
        Json.write(root.resolve(receiptRef),receipt);
        updateIndex(root,run.profile(),run.reportRef(),receiptRef);
        return receiptRef;
    }

    private static ObjectNode envelope(JsonNode report,ObjectNode command,ObjectNode versions,ObjectNode scope) {
        ObjectNode doc=Json.object();doc.put("schemaVersion","1.0.0").put("evidenceClass","ACTUAL");
        doc.set("executionIdentity",report.path("executionIdentity"));doc.set("command",command);doc.set("versions",versions);doc.set("scope",scope);
        doc.set("provenance",Json.object().put("independent",true).put("source","ACTUAL_HARNESS_PROFILE_RUN").put("harnessMainClassSha256",report.path("harnessMainClassSha256").asText()));
        return doc;
    }
    private static ObjectNode writeEnvelope(Path root,String ref,ObjectNode doc,Set<String> paths) throws IOException {
        Json.write(root.resolve(ref),doc);paths.add(ref);
        ObjectNode d=Json.object();d.put("path",ref).put("sha256",Json.sha256(root.resolve(ref))).put("sizeBytes",Files.size(root.resolve(ref)));
        d.set("scope",doc.path("scope"));d.put("completeness","COMPLETE").put("role","ENVELOPE");return d;
    }
    private static ObjectNode descriptor(ContractValidator validator,String ref) throws IOException {
        Path path=validator.path(ref);
        if(!Files.isRegularFile(path) || !path.toRealPath().startsWith(validator.root().toRealPath())) throw new IOException("Receipt artifact is not an in-repository file: "+ref);
        ObjectNode d=Json.object();d.put("path",validator.root().relativize(path).toString().replace('\\','/')).put("sha256",Json.sha256(path)).put("sizeBytes",Files.size(path));return d;
    }
    private static void input(ContractValidator validator,String ref,Map<String,ObjectNode> inputs) throws IOException {
        if(!inputs.containsKey(ref)) inputs.put(ref,descriptor(validator,ref));
    }
    private static void fixtureInputs(ContractValidator validator,String ref,Map<String,ObjectNode> inputs,Set<String> visited) throws IOException {
        if(!visited.add(ref)) return;
        input(validator,ref,inputs);
        for(JsonNode base:Json.read(validator.path(ref)).path("baseRefs")) fixtureInputs(validator,base.asText(),inputs,visited);
    }
    private static void updateIndex(Path root,String profile,String reportRef,String receiptRef) throws IOException {
        Path index=root.resolve(INDEX);
        ObjectNode value=Files.isRegularFile(index)?(ObjectNode)Json.read(index):Json.object();
        value.put("schemaVersion","1.0.0");
        ArrayNode kept=Json.array();
        for(JsonNode entry:value.path("profiles")) if(!entry.path("profile").asText().equals(profile)) kept.add(entry);
        kept.add(Json.object().put("profile",profile).put("reportRef",reportRef).put("receiptRef",receiptRef).put("evidenceClass","ACTUAL"));
        value.set("profiles",kept);
        if(!value.has("modelBindingReportRef")) value.put("modelBindingReportRef",EVIDENCE_DIR+"/model-binding-preparation.json");
        Json.write(index,value);
    }
    private static boolean dirty(Path root) throws IOException,InterruptedException {
        Process p=new ProcessBuilder("git","status","--porcelain").directory(root.toFile()).start();
        String out=new String(p.getInputStream().readAllBytes());
        if(p.waitFor()!=0) return true;
        return !out.isBlank();
    }
    private static String safe(String id) {return id.replaceAll("[^A-Za-z0-9._-]","_");}
}
