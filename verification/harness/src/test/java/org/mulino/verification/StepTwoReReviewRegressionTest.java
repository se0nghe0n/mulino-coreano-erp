package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Step 2 re-review regressions: each test pins a counterexample the earlier harness accepted or crashed on. */
final class StepTwoReReviewRegressionTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private static final String ARTIFACT="verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/canned-observations.json";
    private static final String C3="verification/cases/C3/case.json";

    /** Declares real adapter names but implements nothing: the path that used to reach Scripted.run(intent={}). */
    private static class DeclaredButAbsent implements AcceptanceDriver {
        final Set<String> adapters;final List<String> calls=new ArrayList<>();
        DeclaredButAbsent(Set<String> adapters){this.adapters=adapters;}
        public Set<String> availableAdapters(){return adapters;}
        private StepResult none(String id){calls.add(id);return StepResult.missing(id,"NOT_IMPLEMENTED: regression port");}
        /** Real-looking install: alias map from the fixture bundle so later actions reach their ports. */
        public StepResult installFixture(String id,JsonNode bundle){
            calls.add(id);ObjectNode aliases=Json.object();
            bundle.path("fixture").path("aliases").fieldNames().forEachRemaining(a->aliases.put(a,"regression-"+a));
            for(JsonNode base:bundle.path("bases")) base.path("fixture").path("aliases").fieldNames().forEachRemaining(a->aliases.put(a,"regression-"+a));
            bundle.path("fixture").path("actors").fieldNames().forEachRemaining(a->{if(!aliases.has(a)) aliases.put(a,"regression-"+a);});
            ObjectNode data=Json.object();data.set("aliasMap",aliases);data.set("fixtureHash",bundle.path("fixtureHash"));
            ObjectNode p=Json.object();p.put("adapter","regression-port").put("adapterVersion","1").put("buildVersion","1").putNull("authenticatedActor").put("source","REGRESSION_FIXTURE_INSTALL").put("independent",false).put("scopeComplete",true).putNull("sourceQuery").putNull("snapshot");
            return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,null,null,p,List.of(ARTIFACT));
        }
        public StepResult invoke(String id,String r,JsonNode a,String c,JsonNode q){return none(id);}
        public StepResult query(String id,String r,JsonNode a,String c,JsonNode q){return none(id);}
        public StepResult observe(String id,JsonNode q){return none(id);}
        public StepResult control(String id,JsonNode q){return none(id);}
        public StepResult start(String id,String r,JsonNode a,String c,JsonNode q){return none(id);}
        public StepResult await(String id,JsonNode h,int t){return none(id);}
    }
    private String firstUatSubcase(JsonNode c) {
        for(JsonNode s:c.path("subcases")) for(JsonNode a:s.path("actions")) if(a.path("kind").asText().equals("agent")) return s.path("id").asText();
        throw new AssertionError("C3 has no agent action");
    }
    @Test void uatOnlyAgentActionIsNotRunUnderScriptedRunnerInsteadOfAborting() throws Exception {
        ContractValidator v=new ContractValidator(root);JsonNode c=v.caseFile(root.resolve(C3));String sub=firstUatSubcase(c);
        var driver=new DeclaredButAbsent(Set.of("api","fixture","db"));
        CaseRunner runner=new CaseRunner(v,driver,new AgentRunner.Scripted(),root.resolve(C3),sub);
        assertEquals("NOT_RUN",runner.run(false));
        JsonNode evidence=runner.evidence("NOT_RUN","regression");
        assertEquals("SCRIPTED_SIT",evidence.path("agentRunner").asText());
        assertTrue(evidence.path("missingAdapters").toString().contains("client"),evidence.path("missingAdapters").toString());
        assertFalse(evidence.path("agentActionRunners").isEmpty());
        for(var e:evidence.path("agentActionRunners").properties()) {
            assertEquals("SCRIPTED_SIT",e.getValue().asText());
            assertEquals("NOT_IMPLEMENTED",runner.results().get(e.getKey()).path("driverStatus").asText());
            assertFalse(driver.calls.contains(e.getKey()),"Scripted runner must not dispatch a UAT-only agent action");
        }
        assertThrows(AssertionError.class,runner::verifyComplete);
    }
    @Test void actualRunnerReceivesUatAgentActionAndRecordsItsKind() throws Exception {
        ContractValidator v=new ContractValidator(root);JsonNode c=v.caseFile(root.resolve(C3));String sub=firstUatSubcase(c);
        List<String> utterances=new ArrayList<>();
        var actual=new AgentRunner.Actual((id,route,actor,text,context)->{utterances.add(text);return StepResult.missing(id,"NOT_IMPLEMENTED: no model call in regression");});
        CaseRunner runner=new CaseRunner(v,new DeclaredButAbsent(Set.of("api","fixture","db","mcp")),actual,root.resolve(C3),sub);
        assertEquals("NOT_RUN",runner.run(false));assertEquals(1,utterances.size());
        assertEquals("ACTUAL_UAT",runner.evidence("NOT_RUN","regression").path("agentRunner").asText());
    }
    @Test void scriptedAgentActionOutsideUatSubcaseNeedsDeclaredIntent() throws Exception {
        JsonNode c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
        ObjectNode agent=(ObjectNode)Json.parse("{\"id\":\"agent\",\"kind\":\"agent\",\"actorRef\":\"qc\",\"route\":\"client\",\"intent\":{},\"userUtterance\":\"보류 재고\",\"permittedContext\":{},\"evidenceRefs\":[\"agent:artifact\"]}");
        ((ArrayNode)c.path("subcases").get(0).path("actions")).add(agent);
        Path file=Files.createTempFile(root.resolve("verification/harness/target"),"agent-intent-",".json");Json.write(file,c);
        assertTrue(assertThrows(IllegalArgumentException.class,()->new ContractValidator(root).caseFile(file)).getMessage().contains("UAT-only"));
        agent.set("intent",Json.parse("{\"capabilityId\":\"placeHold\"}"));Json.write(file,c);assertDoesNotThrow(()->new ContractValidator(root).caseFile(file));
    }
    @Test void declaredAdaptersMissingFromDriverKeepProductSubcaseOutOfPass() throws Exception {
        JsonNode c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
        ContractValidator v=new ContractValidator(root);Path file=root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json");
        CaseRunner runner=new CaseRunner(v,new DeclaredButAbsent(Set.of("api")),new AgentRunner.Scripted(),file,c.path("subcases").get(0).path("id").asText());
        assertEquals("NOT_RUN",runner.run(false));
        assertEquals(List.of("db","fixture"),Json.MAPPER.convertValue(runner.evidence("NOT_RUN","regression").path("missingAdapters"),List.class));
        assertEquals("db",v.adapter("independent-db-observer"));assertEquals("client",v.adapter("actualClient"));
    }
    private StepResult capturedAck(String id) {
        ObjectNode p=Json.object();p.put("adapter","regression-port").put("adapterVersion","1").put("buildVersion","1").putNull("authenticatedActor").put("source","CANNED_CONTRACT_SELFTEST").put("independent",false).put("scopeComplete",true).putNull("sourceQuery").putNull("snapshot");
        return new StepResult(id,StepResult.DriverStatus.EXECUTED,Json.object(),Json.parse("{\"outcome\":\"APPLIED\"}"),null,p,List.of(ARTIFACT));
    }
    @Test void productPolicyRejectsCapturedSelftestEvidenceThatSelftestPolicyAccepts() throws Exception {
        JsonNode c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
        ObjectNode sub=(ObjectNode)c.path("subcases").get(0);
        sub.set("actions",Json.array().add(Json.parse("{\"id\":\"read\",\"kind\":\"query\",\"actorRef\":\"qc\",\"route\":\"api\",\"capabilityId\":\"getInventory\",\"request\":{},\"evidenceRefs\":[\"read:artifact\"]}")));
        ObjectNode a=(ObjectNode)sub.path("assertions").get(0).deepCopy();a.put("id","applied").put("op","equals").put("expected","APPLIED");a.remove(List.of("unit","unitSource"));a.set("source",Json.parse("{\"actionId\":\"read\",\"pointer\":\"/response/outcome\"}"));sub.set("assertions",Json.array().add(a));
        sub.set("requiredAdapters",Json.parse("[\"api\"]"));
        Path file=Files.createTempFile(root.resolve("verification/harness/target"),"selftest-policy-",".json");Json.write(file,c);
        var port=new DeclaredButAbsent(Set.of("api")) {public StepResult query(String id,String r,JsonNode actor,String cap,JsonNode q){return capturedAck(id);}};
        assertEquals("PASS",CaseRunner.harnessSelftest(new ContractValidator(root),port,new AgentRunner.Scripted(),file,sub.path("id").asText()).run(false));
        var product=new CaseRunner(new ContractValidator(root),port,new AgentRunner.Scripted(),file,sub.path("id").asText());
        assertTrue(assertThrows(IllegalArgumentException.class,()->product.run(false)).getMessage().contains("not product evidence"));
    }
    @Test void productRunRequiresActualHostEvidenceForProcessControls() throws Exception {
        var capture=new HostObservationValidatorTest().capture("tickScheduler");ContractValidator v=new ContractValidator(root);
        assertDoesNotThrow(()->HostObservationValidator.validate(v,capture.control(),capture.result()));
        assertTrue(assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(v,capture.control(),capture.result(),true)).getMessage().contains("ACTUAL_HOST"));
    }
    @Test void provenancePointersAreNotOracleSources() throws Exception {
        ContractValidator v=new ContractValidator(root);
        JsonNode c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
        assertEquals(List.of(),v.oracleSourceProblems(c));
        ((ObjectNode)c.path("subcases").get(0).path("assertions").get(0).path("source")).put("pointer","/provenance/authenticatedActor/actorId");
        assertEquals(1,v.oracleSourceProblems(c).size());
    }
    private Path manifest(String profile,boolean approval) throws Exception {
        ObjectNode m=Json.object();m.put("schemaVersion","1.0.0").put("recordType","ACCEPTANCE_RUN_MANIFEST").put("profile",profile);ObjectNode approvals=Json.object();
        if(approval) approvals.set(profile.equals("model")?"modelApproval":"deploymentApproval",Json.parse("{\"decisionRef\":\"docs/execution/decisions.md#R8\",\"approvedBy\":\"synthetic-approver\",\"approvedAt\":\"2026-10-08T00:00:00Z\",\"scope\":{\"client\":\"synthetic\"},\"costCeiling\":{\"currency\":\"USD\",\"amount\":\"0\"}}"));
        m.set("approvals",approvals);Path file=Files.createTempFile(root.resolve("verification/harness/target"),"run-manifest-",".json");Json.write(file,m);return file;
    }
    private static final String EXAMPLE="verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json";
    @Test void manifestIsParsedAsRunManifestNotAsCaseFile() throws Exception {
        for(String profile:List.of("model","deployment")) for(boolean approval:List.of(false,true)) {
            Path m=manifest(profile,approval);String rel=root.relativize(m).toString();
            assertEquals(2,Main.execute(new String[]{"profile",profile,"--manifest",rel,EXAMPLE}),profile);
            JsonNode report=Json.read(root.resolve("verification/harness/target/evidence/"+profile+".json"));
            assertEquals("VALID",report.path("runManifest").path("status").asText());assertEquals(approval?"PRESENT":"ABSENT",report.path("runManifest").path("approvalEvidence").asText());
            assertEquals("NOT_RUN",report.path("status").asText());assertFalse(report.path("gateComplete").asBoolean());
            assertEquals(2,Main.execute(new String[]{"profile",profile,"--manifest="+rel,EXAMPLE}));
        }
        Path model=manifest("model",true);String rel=root.relativize(model).toString();
        assertThrows(IllegalArgumentException.class,()->Main.execute(new String[]{"profile","deployment","--manifest",rel,EXAMPLE}));
        assertThrows(IllegalArgumentException.class,()->Main.execute(new String[]{"profile","contracts","--manifest",rel,EXAMPLE}));
        assertThrows(IllegalArgumentException.class,()->Main.execute(new String[]{"profile","model","--manifest"}));
        assertThrows(IllegalArgumentException.class,()->Main.execute(new String[]{"profile","model","--unknown",EXAMPLE}));
        assertThrows(IllegalArgumentException.class,()->Main.execute(new String[]{"profile","model","--manifest",EXAMPLE,EXAMPLE}));
        assertEquals(2,Main.execute(new String[]{"profile","model",EXAMPLE}));
        assertEquals("ABSENT",Json.read(root.resolve("verification/harness/target/evidence/model.json")).path("runManifest").path("status").asText());
    }
    @Test void t08IdentityOracleReadsServerAuditRowsAndForgedActorCouldActuallyMove() throws Exception {
        ContractValidator v=new ContractValidator(root);Path file=root.resolve("verification/cases/T08/case.json");JsonNode c=v.caseFile(file);
        assertEquals(List.of(),v.oracleSourceProblems(c));
        JsonNode warehouse=v.fixture("verification/cases/T08/fixture.json").path("actors").path("warehouse");
        assertTrue(warehouse.path("roleCapabilities").toString().contains("\"moveQuantity\"") && warehouse.path("grant").path("actions").toString().contains("\"moveQuantity\""),
            "The forged payload actor must be authorized, so a payload-trusting server applies the move and fails");
        int identity=0;
        for(JsonNode sub:c.path("subcases")) for(JsonNode a:sub.path("assertions")) if(a.path("id").asText().equals("server-recorded-identity")) {
            identity++;assertEquals("/data/rawRows/audit",a.path("source").path("pointer").asText());
            String key=null;for(JsonNode act:sub.path("actions")) if(act.path("id").asText().equals("attack")) key=act.path("request").path("commandIdempotencyKey").asText();
            assertEquals(key,a.path("source").path("where").path("commandIdempotencyKey").asText());
            for(JsonNode act:sub.path("actions")) if(act.path("id").asText().equals(a.path("source").path("actionId").asText())) assertEquals("observe",act.path("kind").asText());
            if(sub.path("id").asText().equals("payload-actor")) for(JsonNode act:sub.path("actions")) if(act.path("id").asText().equals("attack")) {
                assertEquals("warehouse",act.path("request").path("actorId").path("$alias").asText());assertEquals("reader",act.path("actorRef").asText());
                assertEquals("reader",a.path("expected").get(0).get(0).path("$alias").asText());
            }
        }
        assertEquals(6,identity);
        assertEquals(Json.sha256(file),Json.read(root.resolve("verification/cases/T08/observation-bindings.json")).path("caseHash").asText(),"Run verification/cases/T08/bind_observations.py");
    }
}
