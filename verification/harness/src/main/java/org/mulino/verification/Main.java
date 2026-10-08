package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Explicit CLI statuses: 0 harness/preparation pass; 1 assertion/contract fail; 2 NOT_RUN; 3 environment/format error. */
public final class Main {
    public static void main(String[] args) {
        int exit;
        try { exit=execute(args); }
        catch(Exception e) { System.err.println("ENVIRONMENT_OR_CONTRACT_FORMAT_FAILURE: "+e.getMessage());exit=3; }
        System.exit(exit);
    }
    static int execute(String[] args) throws Exception {
        Path root=Path.of(System.getProperty("repo.root",".")).toAbsolutePath().normalize();
        ContractValidator validator=new ContractValidator(root);
        if(args.length==0) throw new IllegalArgumentException("validate|prepare|red|profile <profile> [case.json...]");
        String mode=args[0],profile=mode.equals("profile") ? args[1] : mode;
        int start=mode.equals("profile") ? 2 : 1;
        List<Path> paths=new ArrayList<>();String manifest=null;
        for(int i=start;i<args.length;i++) {
            String arg=args[i];
            if(arg.equals("--manifest")) {
                if(i+1>=args.length || args[i+1].startsWith("--")) throw new IllegalArgumentException("--manifest requires a path");
                manifest=args[++i];
            } else if(arg.startsWith("--manifest=")) manifest=arg.substring("--manifest=".length());
            else if(arg.equals("--all") && mode.equals("red")) continue;
            else if(arg.startsWith("--")) throw new IllegalArgumentException("Unknown option "+arg);
            else paths.add(validator.path(arg));
        }
        if(manifest!=null && !(mode.equals("profile") && RUN_MANIFEST_PROFILES.contains(profile))) throw new IllegalArgumentException("--manifest applies only to profile model/deployment");
        // Explicit case files select a subset; a subset run never closes the profile's gate.
        boolean explicitSelection=!paths.isEmpty();
        if(paths.isEmpty()) paths.addAll(discoverCases(root));
        if(mode.equals("validate")) {
            if(paths.isEmpty()) throw new IllegalArgumentException("No case files: failIfNoTests");
            for(Path path:paths) { JsonNode c=validator.caseFile(path);System.out.println("PREPARATION_SCHEMA_VALID "+c.path("caseId").asText()+" "+root.relativize(path)); }
            return 0;
        }
        if(mode.equals("coverage")) throw new IllegalArgumentException("coverage is assembled by verification/coverage/assemble.py (./verify coverage), not by the Java preparation report");
        if(mode.equals("prepare")) return prepare(validator,paths,mode);
        if(!mode.equals("profile") && !mode.equals("red")) throw new IllegalArgumentException("Unknown mode "+mode);
        ObjectNode runManifest=mode.equals("profile") && RUN_MANIFEST_PROFILES.contains(profile) ? runManifest(validator,profile,manifest) : null;
        Instant runStarted=Instant.now();
        // Observed before the first case runs, so a receipt never rests only on the post-run status (ExecutionReceiptProducer).
        ObjectNode preRun=mode.equals("profile") ? gitState(root) : null;
        AcceptanceDriver driver=DriverFactory.create(root,mode.equals("red"));
        AgentRunner agentRunner=mode.equals("red") ? new AgentRunner.Scripted() : AgentRunner.fromProperty(System.getProperty("verification.agentRunner","scripted"));
        boolean actual=driver instanceof org.mulino.verification.actual.ActualAcceptanceDriver;
        ArrayNode cases=Json.array();boolean anyFail=false,anyNotRun=false;
        int discovered=0,selected=0,started=0,completed=0;List<Path> selectedFiles=new ArrayList<>();
        for(Path path:paths) {
            JsonNode c=validator.caseFile(path); discovered+=c.path("subcases").size();
            if(!mode.equals("red") && !contains(c.path("profiles"),profile)) continue;
            selectedFiles.add(path);
            for(JsonNode sub:c.path("subcases")) {
                selected++;
                CaseRunner runner=new CaseRunner(validator,driver,agentRunner,path,Json.required(sub,"id"));
                started++;
                String status=runner.run(mode.equals("red")); anyFail|=status.equals("FAIL");anyNotRun|=status.equals("NOT_RUN");
                cases.add(runner.evidence(status,System.getProperty("verification.command","Java acceptance harness")));
                completed++;
            }
        }
        String status=anyFail ? "FAIL" : anyNotRun || selected==0 ? "NOT_RUN" : "PASS";
        int exit=status.equals("FAIL")?1:status.equals("NOT_RUN")?2:0;
        ObjectNode report=base(root,profile,status,exit);report.put("discoveredSubcases",discovered).put("selectedSubcases",selected);
        // Assembler fields: discovered is every subcase the repository declares for the profile (an explicit case-file subset
        // therefore cannot look complete); started/completed count what ran; skipped is always0 (no skip path).
        int declared=mode.equals("profile") ? (explicitSelection ? profileDeclaredSubcases(validator,discoverCases(root),profile) : selected) : selected;
        report.put("discovered",declared).put("started",started).put("completed",completed).put("skipped",0);
        report.put("explicitCaseSelection",explicitSelection);
        if(preRun!=null) report.set("preRun",preRun);
        int actualExecutedActions=0;
        for(JsonNode evidence:cases) for(JsonNode action:evidence.path("actions")) if(actual && action.path("driverStatus").asText().equals("EXECUTED")) actualExecutedActions++;
        report.put("driver",actual?"actual":"unimplemented");report.put("actualExecutedActions",actualExecutedActions);report.put("productRuntimeClaimed",actualExecutedActions>0);report.set("cases",cases);report.put("reason",selected==0?"NOT_IMPLEMENTED: product adapter/cases missing; failIfNoTests prevents PASS":"Required actions and observers must execute; unavailable adapters do not establish zero effects");
        report.set("prerequisiteProfiles",Json.MAPPER.valueToTree(prerequisites(profile)));report.put("prerequisiteRuntimeComplete",false);
        report.put("agentRunner",agentRunner.kind());
        if(runManifest!=null) {
            report.set("runManifest",runManifest);
            // Approval/account evidence alone runs nothing: the paid model/deployment runner is a separate gate.
            if(status.equals("PASS")) {status="NOT_RUN";exit=2;report.put("status",status).put("exitCode",exit);}
        }
        if(!mode.equals("red") && !actual) {report.put("status",anyFail?"FAIL":"NOT_RUN");report.put("exitCode",anyFail?1:2);exit=anyFail?1:2;status=report.path("status").asText();}
        // gateComplete is this profile's own gate: an actual-driver run whose every selected subcase was discovered, started and
        // completed with PASS and none skipped. Prerequisite profiles stay separate (prerequisiteRuntimeComplete=false); the
        // coverage assembler composes them and refuses PASS when a prerequisite is not PASS.
        boolean gate=mode.equals("profile") && actual && runManifest==null && !explicitSelection && status.equals("PASS") && selected>0 && declared==selected && selected==started && started==completed;
        report.put("gateComplete",gate);
        Map<String,String> env=System.getenv();
        List<String> missingVersions=new ArrayList<>();ObjectNode versions=null;
        if(mode.equals("profile") && actual) {
            report.set("executionIdentity",ExecutionReceiptProducer.identity(root,env));
            versions=ExecutionReceiptProducer.versions(profile,env,cases,missingVersions);
        }
        String reportRef="verification/harness/target/evidence/"+profile+".json";
        Json.write(root.resolve(reportRef),report);
        System.out.println(report.toPrettyString());
        if(mode.equals("profile") && actual) {
            List<String> argv=new ArrayList<>();
            JsonNode declaredArgv=Json.parse(System.getProperty("verification.argv","[]"));
            for(JsonNode a:declaredArgv) argv.add(a.asText());
            if(argv.isEmpty()) argv.addAll(List.of("./verify",profile,"--actual"));
            var run=new ExecutionReceiptProducer.Run(root,profile,report,reportRef,argv,System.getProperty("verification.command","Java acceptance harness"),runStarted,Instant.now(),selectedFiles,versions);
            ObjectNode receipt=ExecutionReceiptProducer.emit(validator,run,true,cases,missingVersions);
            System.out.println("COVERAGE_RECEIPT "+receipt);
        }
        return exit;
    }
    private static int prepare(ContractValidator validator,List<Path> paths,String mode) throws Exception {
        Set<String> expected=new LinkedHashSet<>();for(int i=1;i<=26;i++) expected.add(String.format("T%02d",i));for(int i=1;i<=5;i++) expected.add("C"+i);for(int i=1;i<=8;i++) expected.add("V"+i);expected.add("E1");expected.add("E2");
        PreparationValidator preparation=new PreparationValidator(validator.root());
        Map<String,Path> casePaths=new LinkedHashMap<>();
        Map<String,JsonNode> cases=new LinkedHashMap<>();Set<String> covered=new HashSet<>();int subcases=0,assertions=0;
        List<String> problems=new ArrayList<>();
        for(Path path:paths) {
            JsonNode c=validator.caseFile(path);String id=Json.required(c,"caseId");
            if(cases.put(id,c)!=null) problems.add("Duplicate caseId "+id);
            casePaths.put(id,path);
            subcases+=c.path("subcases").size();
            for(JsonNode sub:c.path("subcases")) for(JsonNode assertion:sub.path("assertions")) {
                assertions++;for(JsonNode r:assertion.path("requirementRefs")) covered.add(r.asText());
            }
            problems.addAll(preparation.feature(path,c));
            problems.addAll(validator.oracleSourceProblems(c));
            problems.addAll(validator.errorPointerProblems(c));
            problems.addAll(validator.hostOperationProblems(c));
            problems.addAll(validator.auditFieldProblems(c));
            problems.addAll(validator.runtimeProfileProblems(c));
            problems.addAll(validator.snapshotRefProblems(c));
            problems.addAll(validator.wireTransportProblems(c));
            problems.addAll(validator.placeKindProblems(c));
            problems.addAll(validator.receiptCustodyProblems(c));
            problems.addAll(validator.pickBeforeDispatchProblems(c));
        }
        for(String id:expected) if(!cases.containsKey(id)) problems.add("Missing required case "+id);
        for(int i=1;i<=26;i++) if(!covered.contains(String.format("D%02d",i))) problems.add("Missing requirement assertion "+String.format("D%02d",i));
        Path registry=validator.path("verification/cases/registry.json");
        if(!Files.isRegularFile(registry)) problems.add("Missing independent expected scenario/subcase registry");
        else {
            problems.addAll(preparation.registry(Json.read(registry),cases,casePaths,expected));
        }
        Path catalog=validator.path("verification/requirements/mandatory-oracles.json");
        if(!Files.isRegularFile(catalog)) problems.add("Missing independent normative oracle catalog");
        List<String> attributionGaps=new ArrayList<>();
        if(Files.isRegularFile(catalog)) CatalogLinkValidator.validate(validator,Json.read(catalog),cases,problems,attributionGaps);
        // The normative catalog/lock validator and the owner-maintained case-asset checks; the assembler loads the same lock validator.
        ArrayNode assetChecks=PreparationAssetChecks.run(validator.root(),problems);
        String status=problems.isEmpty()?"PREPARED":"FAIL";ObjectNode report=base(validator.root(),mode,status,problems.isEmpty()?0:1);
        report.put("gateComplete",false).put("runtimeStatus","NOT_RUN").put("runtimeComplete",false).put("preparedCases",cases.size()).put("preparedSubcases",subcases).put("preparedAssertions",assertions);
        report.set("preparationProblems",Json.MAPPER.valueToTree(problems));report.set("artifactKindAttributionGaps",Json.MAPPER.valueToTree(attributionGaps));report.set("caseAssetChecks",assetChecks);
        ArrayNode knownOpen=Json.array();for(JsonNode check:assetChecks) for(JsonNode line:check.path("knownOpen")) knownOpen.add(Json.object().put("check",check.path("name").asText()).put("gap",line.asText()));
        report.set("knownOpenGaps",knownOpen);report.set("runtimeGates",runtimeGates(validator.root(),cases));report.put("artifactCoverageStatus","NOT_RUN");report.put("semanticOracleEquivalence","REQUIRES_CASE_REVIEW");
        Json.write(validator.root().resolve("verification/harness/target/evidence/"+mode+".json"),report);System.out.println(report.toPrettyString());return problems.isEmpty()?0:1;
    }
    private static ObjectNode base(Path root,String profile,String status,int exit) throws IOException,InterruptedException {
        ObjectNode r=Json.object();r.put("schemaVersion","1.0.0").put("recordType","ACCEPTANCE_HARNESS_REPORT").put("profile",profile).put("status",status).put("exitCode",exit).put("timestamp",Instant.now().toString()).put("command",System.getProperty("verification.command","Java acceptance harness"));
        ObjectNode git=gitState(root);r.put("codeCommit",git.path("codeCommit").asText()).put("workingTreeDirty",git.path("workingTreeDirty").asBoolean());
        Path mainClass=root.resolve("verification/harness/target/classes/org/mulino/verification/Main.class");r.put("harnessMainClassSha256",Json.sha256(mainClass));
        r.put("javaVersion",System.getProperty("java.runtime.version")).put("harnessVersion","1.0.0").put("productRuntimeClaimed",false);return r;
    }
    static final String REGULATORY_GATE="NOT_RUN_GATED: no regulatory runner. ./verify regulatory reports NOT_RUN; a reviewed regulatory record "
        +"(official source, jurisdiction, applicable date, reviewer) and an approved runner that reruns these subcases are required before any regulatory receipt exists";
    /**
     * Required evidence that preparation links but no command in this harness can produce. REGULATORY_REVIEW observations
     * are reachable only through a reviewed regulatory receipt (plan §13.4); the coverage assembler reports the same gate.
     */
    static ArrayNode runtimeGates(Path root,Map<String,JsonNode> cases) throws IOException {
        Path catalog=root.resolve("verification/requirements/mandatory-oracles.json");ArrayNode gates=Json.array();
        if(!Files.isRegularFile(catalog)) return gates;
        Set<String> regulatory=new TreeSet<>();
        for(JsonNode oracle:Json.read(catalog).path("oracles")) if(contains(oracle.path("requiredLayers"),"REGULATORY_REVIEW"))
            for(JsonNode o:oracle.path("expectedObservations")) regulatory.add(oracle.path("oracleId").asText()+"/"+o.path("name").asText());
        Set<String> subcases=new TreeSet<>(),observations=new TreeSet<>();
        for(var entry:cases.entrySet()) for(JsonNode sub:entry.getValue().path("subcases")) for(JsonNode a:sub.path("assertions"))
            for(JsonNode name:a.path("oracleRef").path("observationNames")) {
                String key=a.path("oracleRef").path("oracleId").asText()+"/"+name.asText();
                if(regulatory.contains(key)) {subcases.add(entry.getKey()+"/"+sub.path("id").asText());observations.add(key);}
            }
        ObjectNode gate=Json.object().put("profile","regulatory").put("status","NOT_RUN_GATED").put("reason",REGULATORY_GATE);
        gate.set("subcases",Json.MAPPER.valueToTree(subcases));gate.set("observations",Json.MAPPER.valueToTree(observations));
        gates.add(gate);return gates;
    }
    private static List<Path> discoverCases(Path root) throws IOException {
        List<Path> found=new ArrayList<>();Path cases=root.resolve("verification/cases");
        if(Files.isDirectory(cases)) try(var files=Files.walk(cases)) { files.filter(p->p.getFileName().toString().equals("case.json")).sorted().forEach(found::add); }
        return found;
    }
    private static int profileDeclaredSubcases(ContractValidator validator,List<Path> files,String profile) throws IOException {
        int count=0;
        for(Path file:files) {JsonNode c=validator.caseFile(file);if(contains(c.path("profiles"),profile)) count+=c.path("subcases").size();}
        return count;
    }
    /** HEAD and git status of the checkout at this instant. */
    static ObjectNode gitState(Path root) throws IOException,InterruptedException {
        ObjectNode state=Json.object();
        Process p=new ProcessBuilder("git","rev-parse","HEAD").directory(root.toFile()).start();String commit=new String(p.getInputStream().readAllBytes()).trim();if(p.waitFor()!=0) throw new IOException("Cannot record code commit");
        Process dirty=new ProcessBuilder("git","status","--porcelain").directory(root.toFile()).start();String changes=new String(dirty.getInputStream().readAllBytes());if(dirty.waitFor()!=0) throw new IOException("Cannot record working tree status");
        state.put("codeCommit",commit).put("workingTreeDirty",!changes.isBlank()).put("observedAt",Instant.now().toString());
        return state;
    }
    static final Set<String> RUN_MANIFEST_PROFILES=Set.of("model","deployment");
    /** Validates the plan §13.4 run manifest; a manifest never substitutes for executing the profile. */
    static ObjectNode runManifest(ContractValidator validator,String profile,String manifest) throws IOException {
        ObjectNode r=Json.object();
        if(manifest==null) {r.putNull("path");r.put("status","ABSENT").put("notRunReason","NOT_RUN: "+profile+" requires --manifest with R8 approval/account evidence");return r;}
        Path file=validator.path(manifest);
        if(!Files.isRegularFile(file)) throw new IllegalArgumentException("Run manifest not found: "+manifest);
        JsonNode m=Json.read(file);validator.schema("contracts/acceptance-run-manifest.schema.json",m);
        if(!m.path("profile").asText().equals(profile)) throw new IllegalArgumentException("Run manifest profile "+m.path("profile").asText()+" differs from requested "+profile);
        String approvalKey=profile.equals("model")?"modelApproval":"deploymentApproval";boolean approved=m.path("approvals").has(approvalKey);
        r.put("path",validator.root().relativize(file).toString()).put("sha256",Json.sha256(file)).put("status","VALID").put("approvalEvidence",approved?"PRESENT":"ABSENT");
        r.put("notRunReason",approved?"NOT_RUN: approval evidence recorded; the "+profile+" runner did not execute in this harness":"NOT_RUN: "+approvalKey+" (R8/BTP account evidence) absent; no paid model or deployment call made");
        return r;
    }
    private static boolean contains(JsonNode array,String value) {for(JsonNode n:array) if(n.asText().equals(value)) return true;return false;}
    private static List<String> prerequisites(String profile) {return switch(profile) {
        case "contracts" -> List.of("schema");case "scenarios","recovery" -> List.of("schema","contracts");case "mcp","skills" -> List.of("schema","contracts","scenarios","recovery");case "model","deployment" -> List.of("schema","contracts","scenarios","recovery","mcp","skills");default -> List.of();};}
}
