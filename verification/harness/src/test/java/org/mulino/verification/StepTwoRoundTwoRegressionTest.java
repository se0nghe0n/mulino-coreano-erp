package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Step 2 re-review round 2 (harness2): each test pins a counterexample of the previous harness/assembler contract. */
final class StepTwoRoundTwoRegressionTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private static final String EXAMPLE="verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json";
    private static final String ARTIFACT="verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/canned-observations.json";

    // Item 1: required-profile reachability, identical to assemble.py.
    private static ObjectNode catalog(String layers) {
        ObjectNode observation=Json.object().put("name","obs").put("type","equality").put("operator","eq");
        ObjectNode oracle=Json.object().put("oracleId","E1.flow").put("caseId","E1");
        oracle.set("requiredLayers",Json.parse(layers));oracle.set("expectedObservations",Json.array().add(observation));
        ObjectNode c=Json.object();c.set("oracles",Json.array().add(oracle));return c;
    }
    private static ObjectNode caseWith(String profiles) {
        ObjectNode a=Json.object().put("id","a");a.set("oracleRef",Json.parse("{\"oracleId\":\"E1.flow\",\"observationNames\":[\"obs\"]}"));
        ObjectNode sub=Json.object().put("id","s");sub.set("assertions",Json.array().add(a));
        ObjectNode c=Json.object().put("caseId","E1");c.set("profiles",Json.parse(profiles));c.set("subcases",Json.array().add(sub));return c;
    }
    private static List<String> reach(String layers,String profiles) {
        List<String> problems=new ArrayList<>();CatalogLinkValidator.requiredProfileReachability(catalog(layers),Map.of("E1",caseWith(profiles)),problems);return problems;
    }
    @Test void requiredLayerWithoutLinkedProfileIsAPreparationProblem() {
        assertEquals(List.of(),reach("[\"API\",\"DB\"]","[\"scenarios\"]"));
        List<String> mcp=reach("[\"API\",\"DB\",\"MCP\"]","[\"scenarios\"]");
        assertEquals(1,mcp.size());assertTrue(mcp.get(0).startsWith("Unreachable required profile: E1.flow/obs requires mcp"),mcp.toString());
        assertEquals(List.of(),reach("[\"API\",\"MCP\"]","[\"scenarios\",\"mcp\"]"));
        assertEquals(List.of(),reach("[\"LOCAL_DEPLOYMENT\",\"BTP_DEPLOYMENT\"]","[\"deployment\"]"),"deployment expands to both deployment profiles");
        assertEquals(List.of(),reach("[\"API\",\"REGULATORY_REVIEW\"]","[\"scenarios\"]"),"regulatory evidence profile is implied by the linked assertion");
        assertEquals(2,reach("[\"UNIT\",\"SKILLS\"]","[\"scenarios\"]").size());
        assertTrue(reach("[\"API\"]","[\"scenarios\",\"unknown\"]").get(0).contains("unknown verification profile"));
    }
    // Item 6: canonical error envelope.
    @Test void nonCanonicalErrorPointersAreRejectedAndWireEnvelopesAllowed() throws Exception {
        ContractValidator v=new ContractValidator(root);JsonNode c=Json.read(root.resolve(EXAMPLE));
        ObjectNode source=(ObjectNode)c.path("subcases").get(0).path("assertions").get(0).path("source");
        for(String ok:List.of("/response/error/code","/response/body/error/code","/response/body/result/structuredContent/error/code","/response/data/heldQuantity")) {
            source.put("pointer",ok);assertEquals(List.of(),v.errorPointerProblems(c),ok);
        }
        for(String bad:List.of("/response/errorCode","/response/code","/response/body/errorCode","/response/error_code")) {
            source.put("pointer",bad);assertEquals(1,v.errorPointerProblems(c).size(),bad);
        }
    }
    // Items 1 and 7: preparation runs the normative lock validator and case-asset checks and fails closed.
    @Test void preparationAssetChecksFailClosed(@TempDir Path temp) throws Exception {
        List<String> problems=new ArrayList<>();
        ArrayNode none=PreparationAssetChecks.run(temp,problems);
        assertEquals(PreparationAssetChecks.CHECKS.size(),problems.size(),"every missing script is a problem");
        for(JsonNode r:none) assertEquals("FAIL",r.path("status").asText());
        for(var check:PreparationAssetChecks.CHECKS) {
            Path script=temp.resolve(check.script());Files.createDirectories(script.getParent());
            Files.writeString(script,check.name().equals("cases-b-invariants")?"import sys\nprint('1 problem')\nsys.exit(1)\n":"print('ok')\n");
        }
        // Generator checks run unittest discovery; an empty test directory passes discovery only when a module exists.
        for(var check:PreparationAssetChecks.CHECKS) if(check.argv().contains("unittest"))
            Files.writeString(temp.resolve(check.script()),"import unittest\nclass T(unittest.TestCase):\n    def test_ok(self): pass\n");
        // KNOWN_OPEN lines are copied into the record rather than hidden in the output tail.
        Path routes=temp.resolve("verification/requirements/check_layer_routes.py");Files.writeString(routes,"print('KNOWN_OPEN owner-x\\tV4\\tV4.o/n\\tMCP')\n");
        problems.clear();ArrayNode records=PreparationAssetChecks.run(temp,problems);
        assertEquals(1,problems.size(),problems.toString());assertTrue(problems.get(0).contains("cases-b-invariants exit=1"));
        for(JsonNode r:records) assertTrue(r.path("scriptSha256").asText().matches("[0-9a-f]{64}"));
        JsonNode layer=null;for(JsonNode r:records) if(r.path("name").asText().equals("layer-routes")) layer=r;
        assertNotNull(layer);assertEquals("PASS",layer.path("status").asText());assertEquals(1,layer.path("knownOpen").size());assertTrue(layer.path("knownOpen").get(0).asText().startsWith("KNOWN_OPEN owner-x"));
    }
    // Item 3: runtime assertion records carry op/unit/where/field and the post-projection compared value.
    @Test void assertionRecordsCarryOperatorUnitAndComparedValues() throws Exception {
        JsonNode c=Json.read(root.resolve(EXAMPLE));ObjectNode sub=(ObjectNode)c.path("subcases").get(0);
        sub.set("actions",Json.array().add(Json.parse("{\"id\":\"read\",\"kind\":\"query\",\"actorRef\":\"qc\",\"route\":\"api\",\"capabilityId\":\"getInventory\",\"request\":{},\"evidenceRefs\":[\"read:artifact\"]}")));
        ObjectNode sum=(ObjectNode)sub.path("assertions").get(0).deepCopy();
        sum.put("id","held-sum").put("op","sumEquals").put("expected","100");
        sum.set("source",Json.parse("{\"actionId\":\"read\",\"pointer\":\"/response/rows\",\"where\":{\"kind\":\"HOLD\"},\"field\":\"quantity\"}"));
        sum.set("unitSource",Json.parse("{\"actionId\":\"read\",\"pointer\":\"/response/rows\",\"where\":{\"kind\":\"HOLD\"},\"field\":\"unit\"}"));
        sub.set("assertions",Json.array().add(sum));sub.set("requiredAdapters",Json.parse("[\"api\"]"));
        Path file=Files.createTempFile(root.resolve("verification/harness/target"),"record-fields-",".json");Json.write(file,c);
        var port=new AcceptanceDriver() {
            public Set<String> availableAdapters(){return Set.of("api");}
            public StepResult installFixture(String id,JsonNode f){return StepResult.missing(id,"unused");}
            public StepResult invoke(String id,String r,JsonNode a,String cap,JsonNode q){return StepResult.missing(id,"unused");}
            public StepResult query(String id,String r,JsonNode a,String cap,JsonNode q){
                ObjectNode p=Json.object();p.put("adapter","regression-port").put("adapterVersion","1").put("buildVersion","1").putNull("authenticatedActor").put("source","CANNED_CONTRACT_SELFTEST").put("independent",false).put("scopeComplete",true).putNull("sourceQuery").putNull("snapshot");
                return new StepResult(id,StepResult.DriverStatus.EXECUTED,Json.object(),Json.parse("{\"rows\":[{\"kind\":\"HOLD\",\"quantity\":\"60\",\"unit\":\"BOX\"},{\"kind\":\"HOLD\",\"quantity\":\"40\",\"unit\":\"BOX\"},{\"kind\":\"SALE\",\"quantity\":\"7\",\"unit\":\"BOX\"}]}"),null,p,List.of(ARTIFACT));
            }
            public StepResult observe(String id,JsonNode q){return StepResult.missing(id,"unused");}
            public StepResult control(String id,JsonNode q){return StepResult.missing(id,"unused");}
            public StepResult start(String id,String r,JsonNode a,String cap,JsonNode q){return StepResult.missing(id,"unused");}
            public StepResult await(String id,JsonNode h,int t){return StepResult.missing(id,"unused");}
        };
        CaseRunner runner=CaseRunner.harnessSelftest(new ContractValidator(root),port,new AgentRunner.Scripted(),file,sub.path("id").asText());
        assertEquals("PASS",runner.run(false));
        JsonNode record=runner.evidence("PASS","regression").path("assertions").get(0);
        assertEquals("sumEquals",record.path("op").asText());assertEquals("BOX",record.path("unit").asText());
        assertEquals(Json.parse("{\"kind\":\"HOLD\"}"),record.path("where"));assertEquals("quantity",record.path("field").asText());
        assertEquals(Json.parse("[\"60\",\"40\"]"),record.path("observed"),"post-projection value, not the raw rows");
        assertEquals(Json.parse("[\"BOX\",\"BOX\"]"),record.path("observedUnit"));
        assertEquals(sum.path("unitSource"),record.path("unitSource"));
    }
    // Item 2: profile report fields aligned with the assembler; selftest/unimplemented runs never emit ACTUAL.
    @Test void unimplementedProfileRunReportsAssemblerFieldsAndNoReceipt() throws Exception {
        Path receipt=root.resolve("verification/harness/target/evidence/scenarios-receipt.json");Files.deleteIfExists(receipt);
        assertEquals(2,Main.execute(new String[]{"profile","scenarios",EXAMPLE}));
        JsonNode report=Json.read(root.resolve("verification/harness/target/evidence/scenarios.json"));
        // An explicit case file is a subset: discovered counts every subcase the repository declares for the profile.
        assertTrue(report.path("discovered").asInt()>1,report.path("discovered").toString());assertEquals(1,report.path("selectedSubcases").asInt());
        assertEquals(1,report.path("started").asInt());assertEquals(1,report.path("completed").asInt());assertEquals(0,report.path("skipped").asInt());
        assertTrue(report.path("explicitCaseSelection").asBoolean());assertTrue(report.path("preRun").path("codeCommit").asText().matches("[0-9a-f]{40}"));
        assertFalse(report.path("gateComplete").asBoolean());assertFalse(report.has("executionIdentity"));
        assertFalse(Files.exists(receipt),"the unimplemented driver can never emit a coverage receipt");
        assertThrows(IllegalArgumentException.class,()->Main.execute(new String[]{"coverage"}),"coverage is the assembler, not the Java preparation report");
    }
    private ObjectNode actualLike(String provenanceSource,String policy) {
        ObjectNode action=Json.object();action.put("actionId","read").put("driverStatus","EXECUTED");action.set("data",Json.object());action.set("response",Json.parse("{\"quantity\":\"80\"}"));action.putNull("reason");
        action.set("provenance",Json.object().put("adapter","actual-http-jdbc").put("adapterVersion","1").put("buildVersion","b").put("source",provenanceSource).put("independent",true).put("scopeComplete",true));
        action.set("artifactRefs",Json.parse("[\"verification/harness/target/evidence/actual/read-1.json\"]"));
        ObjectNode c=Json.object();c.put("caseId","T01").put("subcaseId","only").put("status","PASS").put("evidencePolicy",policy).put("fixtureRef","verification/cases/T01/fixture.json");
        c.set("versions",Json.parse("{\"definition\":\"definition-v1\",\"evaluator\":\"evaluator-v1\",\"policy\":\"policy-v1\"}"));
        c.set("actions",Json.object().set("read",action));return c;
    }
    private ExecutionReceiptProducer.Run run(Path base,ObjectNode report,ObjectNode versions) {
        return new ExecutionReceiptProducer.Run(base,"scenarios",report,"verification/harness/target/evidence/scenarios.json",List.of("./verify","scenarios","--actual"),"./verify scenarios",
            Instant.parse("2026-10-08T00:00:00Z"),Instant.parse("2026-10-08T00:01:00Z"),List.of(),versions);
    }
    @Test void receiptGateRefusesEverythingButACleanActualProductRun() throws Exception {
        String commit="b".repeat(40);ObjectNode versions=Json.parse("{\"schema\":\"ontology-v1\",\"db\":\"postgres-18\",\"build\":\""+commit+"\"}").deepCopy();
        ObjectNode report=Json.object().put("codeCommit",commit).put("workingTreeDirty",false).put("explicitCaseSelection",false);
        report.set("preRun",Json.object().put("codeCommit",commit).put("workingTreeDirty",false).put("observedAt","2026-10-08T00:00:00Z"));
        ArrayNode good=Json.array().add(actualLike("ACTUAL_HTTP","PRODUCT"));
        assertNull(ExecutionReceiptProducer.refusal(run(root,report,versions),true,good,false),"a clean actual product run is eligible");
        assertTrue(ExecutionReceiptProducer.refusal(run(root,report,versions),true,good,true).contains("after the run"));
        ObjectNode dirtyBefore=report.deepCopy();((ObjectNode)dirtyBefore.path("preRun")).put("workingTreeDirty",true);
        assertTrue(ExecutionReceiptProducer.refusal(run(root,dirtyBefore,versions),true,good,false).contains("before the run"));
        ObjectNode movedHead=report.deepCopy();((ObjectNode)movedHead.path("preRun")).put("codeCommit","c".repeat(40));
        assertTrue(ExecutionReceiptProducer.refusal(run(root,movedHead,versions),true,good,false).contains("before the run"));
        ObjectNode noPreRun=report.deepCopy();noPreRun.remove("preRun");
        assertTrue(ExecutionReceiptProducer.refusal(run(root,noPreRun,versions),true,good,false).contains("before the run"));
        assertTrue(ExecutionReceiptProducer.refusal(run(root,report.deepCopy().put("explicitCaseSelection",true),versions),true,good,false).contains("explicit case-file selection"));
        var otherProfile=new ExecutionReceiptProducer.Run(root,"scenarios",report,"r",List.of("./verify","mcp","--actual"),"./verify mcp",Instant.now(),Instant.now(),List.of(),versions);
        assertTrue(ExecutionReceiptProducer.refusal(otherProfile,true,good,false).contains("does not name profile"));
        assertNotNull(ExecutionReceiptProducer.refusal(run(root,report,versions),false,good),"not the actual driver");
        assertTrue(ExecutionReceiptProducer.refusal(run(root,report,versions),true,Json.array().add(actualLike("ACTUAL_HTTP","HARNESS_SELFTEST"))).contains("PRODUCT"));
        assertTrue(ExecutionReceiptProducer.refusal(run(root,report,versions),true,Json.array().add(actualLike("CANNED_CONTRACT_SELFTEST","PRODUCT"))).contains("selftest"));
        assertTrue(ExecutionReceiptProducer.refusal(run(root,report.deepCopy().put("workingTreeDirty",true),versions),true,good).contains("dirty"));
        assertTrue(ExecutionReceiptProducer.refusal(run(root,report,versions.deepCopy().put("build","c".repeat(40))),true,good).contains("ACTUAL_BUILD_COMMIT"));
        assertNotNull(ExecutionReceiptProducer.refusal(run(root,report,versions),true,Json.array()));
        List<String> missing=new ArrayList<>();ExecutionReceiptProducer.versions("mcp",Map.of(),good,missing);
        assertEquals(List.of("ACTUAL_SCHEMA_VERSION","ACTUAL_DB_VERSION","ACTUAL_MCP_PROTOCOL_VERSION","ACTUAL_BUILD_COMMIT"),missing);
        ObjectNode status=ExecutionReceiptProducer.emit(new ContractValidator(root),run(root,report,versions),true,good,missing);
        assertEquals("NOT_EMITTED",status.path("status").asText());
    }
    /** An actual observe action with a real observer snapshot (capturedAt, sourceQuery, revisionQuery). */
    private ObjectNode actualObserve(String source,String isolation) {
        ObjectNode c=actualLike("ACTUAL_HTTP","PRODUCT");ObjectNode action=(ObjectNode)c.path("actions").path("read");
        ObjectNode query=Json.object().put("statementId","s1-physical-segments-v1").put("sql","SELECT quantity FROM segments WHERE organization_id = ?").put("mappingVersion","1.0.0");query.set("parameters",Json.object());
        ObjectNode snapshot=Json.object().put("id","811:811:").put("isolation",isolation).put("capturedAt","2026-10-08T00:00:30Z").put("artifactRef","verification/harness/target/evidence/actual/read-1.json").put("readMode","RESULT_REVISION");
        snapshot.set("revisionQuery",query);
        ObjectNode provenance=(ObjectNode)action.path("provenance");provenance.put("source",source).put("adapter","actual-http-jdbc").put("adapterVersion","2.0.0");provenance.set("sourceQuery",query);provenance.set("snapshot",snapshot);
        ObjectNode data=Json.object();data.set("snapshot",snapshot);data.set("sourceQuery",query);action.set("data",data);
        return c;
    }
    /** step2-closure P2: the selftest marker check is structural, so a real observe snapshot (capturedAt) is not a marker. */
    @Test void actualObserveWithSnapshotIsEligibleAndSelftestLabelsAreRefused() throws Exception {
        assertNull(ExecutionReceiptProducer.actionMarker(Json.array().add(actualObserve("POSTGRESQL_JDBC","REPEATABLE_READ").path("actions").path("read"))));
        assertTrue(actualObserve("POSTGRESQL_JDBC","REPEATABLE_READ").path("actions").toString().toLowerCase(Locale.ROOT).contains("captured"),
            "the counterexample must contain the substring that the old whole-JSON scan refused");
        String commit="b".repeat(40);ObjectNode versions=Json.parse("{\"schema\":\"ontology-v1\",\"db\":\"postgres-18\",\"build\":\""+commit+"\"}").deepCopy();
        ObjectNode report=Json.object().put("codeCommit",commit).put("workingTreeDirty",false).put("explicitCaseSelection",false);
        report.set("preRun",Json.object().put("codeCommit",commit).put("workingTreeDirty",false).put("observedAt","2026-10-08T00:00:00Z"));
        assertNull(ExecutionReceiptProducer.refusal(run(root,report,versions),true,Json.array().add(actualObserve("POSTGRESQL_JDBC","REPEATABLE_READ")),false));
        for(ObjectNode selftest:List.of(actualObserve("CANNED_CONTRACT_SELFTEST","REPEATABLE_READ"),actualObserve("POSTGRESQL_JDBC","SELFTEST_CAPTURED"))) {
            String reason=ExecutionReceiptProducer.refusal(run(root,report,versions),true,Json.array().add(selftest),false);
            assertNotNull(reason);assertTrue(reason.contains("selftest provenance label"),reason);
        }
        ObjectNode stub=actualObserve("POSTGRESQL_JDBC","REPEATABLE_READ");((ObjectNode)stub.path("actions").path("read").path("provenance")).put("adapter","stub-adapter");
        assertTrue(ExecutionReceiptProducer.refusal(run(root,report,versions),true,Json.array().add(stub),false).contains("/adapter"));
    }
    /** Format only, in a disposable directory: the written receipt validates and the real assembler accepts it. */
    @Test void writtenReceiptIsAcceptedByTheCoverageAssembler(@TempDir Path base) throws Exception {
        String commit="b".repeat(40);
        Files.createDirectories(base.resolve("verification/coverage"));
        Files.copy(root.resolve("verification/coverage/execution-receipt.schema.json"),base.resolve("verification/coverage/execution-receipt.schema.json"));
        Files.createDirectories(base.resolve("contracts"));Files.copy(root.resolve("contracts/acceptance-capabilities.json"),base.resolve("contracts/acceptance-capabilities.json"));
        for(String ref:List.of("verification/cases/registry.json","verification/requirements/mandatory-oracles.json","verification/cases/T01/case.json"))
            {Files.createDirectories(base.resolve(ref).getParent());Files.writeString(base.resolve(ref),"{\"fixture\":\""+ref+"\"}\n");}
        Files.writeString(base.resolve("verification/cases/T01/fixture.json"),"{\"versions\":{},\"baseRefs\":[\"verification/fixtures/base.json\"]}\n");
        Files.createDirectories(base.resolve("verification/fixtures"));Files.writeString(base.resolve("verification/fixtures/base.json"),"{}\n");
        Files.createDirectories(base.resolve("verification/harness/target/evidence/actual"));
        Files.writeString(base.resolve("verification/harness/target/evidence/actual/read-1.json"),"{\"httpStatus\":200}\n",StandardCharsets.UTF_8);
        ArrayNode cases=Json.array().add(actualObserve("POSTGRESQL_JDBC","REPEATABLE_READ"));
        ObjectNode report=Json.object();report.put("schemaVersion","1.0.0").put("profile","scenarios").put("status","PASS").put("exitCode",0).put("command","./verify scenarios").put("codeCommit",commit)
            .put("workingTreeDirty",false).put("gateComplete",true).put("discovered",1).put("started",1).put("completed",1).put("skipped",0).put("harnessMainClassSha256","0".repeat(64))
            .put("explicitCaseSelection",false).put("timestamp","2026-10-08T00:01:00Z");
        report.set("preRun",Json.object().put("codeCommit",commit).put("workingTreeDirty",false).put("observedAt","2026-10-08T00:00:00Z"));
        report.set("executionIdentity",Json.parse("{\"runId\":\"run-1\",\"hostId\":\"host-1\",\"workspaceId\":\"workspace-1\",\"actorId\":\"actor-1\"}"));report.set("cases",cases);
        Json.write(base.resolve("verification/harness/target/evidence/scenarios.json"),report);
        ObjectNode versions=Json.parse("{\"schema\":\"ontology-v1\",\"db\":\"postgres-18\",\"build\":\""+commit+"\",\"tool\":\"harness\",\"definition\":\"definition-v1\",\"evaluator\":\"evaluator-v1\",\"policy\":\"policy-v1\"}").deepCopy();
        String ref=ExecutionReceiptProducer.write(new ContractValidator(base),run(base,report,versions),cases);
        JsonNode receipt=Json.read(base.resolve(ref));
        assertEquals("ACTUAL",receipt.path("evidenceClass").asText());assertTrue(receipt.path("workingTreeClean").asBoolean());
        long raw=0;for(JsonNode a:receipt.path("artifacts")) if(a.path("role").asText().equals("RAW_CAPTURE")) raw++;assertEquals(1,raw);
        String script=String.join("\n",
            "import importlib.util,json,sys",
            "spec=importlib.util.spec_from_file_location('a',sys.argv[1]);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)",
            "a=m.Assembly(sys.argv[2],sys.argv[3]);ev='verification/harness/target/evidence/'",
            "r=a.receipt(json.load(open(sys.argv[2]+'/'+ev+'scenarios.json')),ev+'scenarios-receipt.json','scenarios',ev+'scenarios.json')",
            "print(json.dumps({'accepted':r is not None,'problems':a.problems}))");
        Process p=new ProcessBuilder("python3","-I","-c",script,root.resolve("verification/coverage/assemble.py").toString(),base.toString(),commit).redirectErrorStream(true).start();
        String out=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);assertEquals(0,p.waitFor(),out);
        JsonNode verdict=Json.parse(out.strip().lines().reduce((x,y)->y).orElse("{}"));
        assertTrue(verdict.path("accepted").asBoolean(),out);assertTrue(verdict.path("problems").isEmpty(),out);
        JsonNode index=Json.read(base.resolve(ExecutionReceiptProducer.INDEX));
        assertEquals("ACTUAL",index.path("profiles").get(0).path("evidenceClass").asText());
    }
}
