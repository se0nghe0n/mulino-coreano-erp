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
        if(args.length==0) throw new IllegalArgumentException("validate|prepare|coverage|red|profile <profile> [case.json...]");
        String mode=args[0],profile=mode.equals("profile") ? args[1] : mode;
        int start=mode.equals("profile") ? 2 : 1;
        List<Path> paths=new ArrayList<>();
        for(int i=start;i<args.length;i++) if(!args[i].startsWith("--")) paths.add(validator.path(args[i]));
        if(paths.isEmpty()) {
            Path cases=root.resolve("verification/cases");
            if(Files.isDirectory(cases)) try(var files=Files.walk(cases)) { files.filter(p->p.getFileName().toString().equals("case.json")).sorted().forEach(paths::add); }
        }
        if(mode.equals("validate")) {
            if(paths.isEmpty()) throw new IllegalArgumentException("No case files: failIfNoTests");
            for(Path path:paths) { JsonNode c=validator.caseFile(path);System.out.println("PREPARATION_SCHEMA_VALID "+c.path("caseId").asText()+" "+root.relativize(path)); }
            return 0;
        }
        if(mode.equals("prepare") || mode.equals("coverage")) return prepare(validator,paths,mode);
        if(!mode.equals("profile") && !mode.equals("red")) throw new IllegalArgumentException("Unknown mode "+mode);
        AcceptanceDriver driver=DriverFactory.create(root,mode.equals("red"));
        boolean actual=driver instanceof org.mulino.verification.actual.ActualAcceptanceDriver;
        ArrayNode cases=Json.array();boolean anyFail=false,anyNotRun=false;
        int discovered=0,selected=0;
        for(Path path:paths) {
            JsonNode c=validator.caseFile(path); discovered+=c.path("subcases").size();
            if(!mode.equals("red") && !contains(c.path("profiles"),profile)) continue;
            for(JsonNode sub:c.path("subcases")) {
                selected++;
                CaseRunner runner=new CaseRunner(validator,driver,new AgentRunner.Scripted(),path,Json.required(sub,"id"));
                String status=runner.run(mode.equals("red")); anyFail|=status.equals("FAIL");anyNotRun|=status.equals("NOT_RUN");
                cases.add(runner.evidence(status,System.getProperty("verification.command","Java acceptance harness")));
            }
        }
        String status=anyFail ? "FAIL" : anyNotRun || selected==0 ? "NOT_RUN" : "PASS";
        int exit=status.equals("FAIL")?1:status.equals("NOT_RUN")?2:0;
        ObjectNode report=base(root,profile,status,exit);report.put("discoveredSubcases",discovered).put("selectedSubcases",selected).put("gateComplete",status.equals("PASS"));
        int actualExecutedActions=0;
        for(JsonNode evidence:cases) for(JsonNode action:evidence.path("actions")) if(actual && action.path("driverStatus").asText().equals("EXECUTED")) actualExecutedActions++;
        report.put("driver",actual?"actual":"unimplemented");report.put("actualExecutedActions",actualExecutedActions);report.put("productRuntimeClaimed",actualExecutedActions>0);report.set("cases",cases);report.put("reason",selected==0?"NOT_IMPLEMENTED: product adapter/cases missing; failIfNoTests prevents PASS":"Required actions and observers must execute; unavailable adapters do not establish zero effects");
        report.set("prerequisiteProfiles",Json.MAPPER.valueToTree(prerequisites(profile)));report.put("prerequisiteRuntimeComplete",false);
        if(!mode.equals("red") && !actual) {report.put("gateComplete",false);report.put("status",anyFail?"FAIL":"NOT_RUN");report.put("exitCode",anyFail?1:2);exit=anyFail?1:2;}
        if(actual) report.put("gateComplete",false); // Prerequisite integration gates remain independently unverified.
        Json.write(root.resolve("verification/harness/target/evidence/"+profile+".json"),report);
        System.out.println(report.toPrettyString());return exit;
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
        else catalogLinks(validator,Json.read(catalog),cases,problems);
        String status=problems.isEmpty()?"PREPARED":"FAIL";ObjectNode report=base(validator.root(),mode,status,problems.isEmpty()?0:1);
        report.put("gateComplete",false).put("runtimeStatus","NOT_RUN").put("runtimeComplete",false).put("preparedCases",cases.size()).put("preparedSubcases",subcases).put("preparedAssertions",assertions);
        report.set("preparationProblems",Json.MAPPER.valueToTree(problems));report.put("artifactCoverageStatus","NOT_RUN");report.put("semanticOracleEquivalence","REQUIRES_CASE_REVIEW");
        Json.write(validator.root().resolve("verification/harness/target/evidence/"+mode+".json"),report);System.out.println(report.toPrettyString());return problems.isEmpty()?0:1;
    }
    private static void catalogLinks(ContractValidator validator,JsonNode catalog,Map<String,JsonNode> cases,List<String> problems) throws IOException {
        CatalogLinkValidator.validate(validator,catalog,cases,problems);
    }
    private static ObjectNode base(Path root,String profile,String status,int exit) throws IOException,InterruptedException {
        ObjectNode r=Json.object();r.put("schemaVersion","1.0.0").put("recordType","ACCEPTANCE_HARNESS_REPORT").put("profile",profile).put("status",status).put("exitCode",exit).put("timestamp",Instant.now().toString()).put("command",System.getProperty("verification.command","Java acceptance harness"));
        Process p=new ProcessBuilder("git","rev-parse","HEAD").directory(root.toFile()).start();String commit=new String(p.getInputStream().readAllBytes()).trim();if(p.waitFor()!=0) throw new IOException("Cannot record code commit");r.put("codeCommit",commit);
        Process dirty=new ProcessBuilder("git","status","--porcelain").directory(root.toFile()).start();String changes=new String(dirty.getInputStream().readAllBytes());if(dirty.waitFor()!=0) throw new IOException("Cannot record working tree status");r.put("workingTreeDirty",!changes.isBlank());
        Path mainClass=root.resolve("verification/harness/target/classes/org/mulino/verification/Main.class");r.put("harnessMainClassSha256",Json.sha256(mainClass));
        r.put("javaVersion",System.getProperty("java.runtime.version")).put("harnessVersion","1.0.0").put("productRuntimeClaimed",false);return r;
    }
    private static boolean contains(JsonNode array,String value) {for(JsonNode n:array) if(n.asText().equals(value)) return true;return false;}
    private static List<String> prerequisites(String profile) {return switch(profile) {
        case "contracts" -> List.of("schema");case "scenarios","recovery" -> List.of("schema","contracts");case "mcp","skills" -> List.of("schema","contracts","scenarios","recovery");case "model","deployment" -> List.of("schema","contracts","scenarios","recovery","mcp","skills");default -> List.of();};}
}
