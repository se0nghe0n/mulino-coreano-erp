package org.mulino.verification;

import io.cucumber.java.*;
import io.cucumber.java.en.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class ScenarioGlue {
    private CaseRunner runner;
    private int checked;
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    @Given("사례 파일 {string}의 {string}를 준비한다")
    public void prepare(String caseRef,String subcase) throws IOException {
        ContractValidator validator=new ContractValidator(root);
        runner=new CaseRunner(validator,new UnimplementedDriver(),new AgentRunner.Scripted(),validator.path(caseRef),subcase);
    }
    @When("{string} 역할이 {string} 행동을 수행한다")
    public void action(String actor,String id) throws IOException {
        var declared=runner.subcase().path("actions");
        for(var a:declared) if(a.path("id").asText().equals(id) && a.has("actorRef") && !a.path("actorRef").asText().equals(actor))
            throw new IllegalArgumentException("Gherkin actor does not match explicit action actor");
        runner.execute(id);
    }
    @Then("{string} assertion으로 {string}를 확인한다")
    public void assertion(String id,String humanExplanation) { runner.assertId(id); }
    @After
    public void evidence(Scenario scenario) throws IOException {
        if(runner==null) return;
        if(!scenario.isFailed()) runner.verifyComplete();
        String mode=System.getProperty("verification.mode","harness-selftest");
        var report=runner.evidence(scenario.isFailed()?"FAIL":"PASS",System.getProperty("verification.command","Maven Cucumber"));
        String name=mode+"-"+report.path("caseId").asText()+"-"+report.path("subcaseId").asText();
        name=java.net.URLEncoder.encode(name,java.nio.charset.StandardCharsets.UTF_8);
        Json.write(root.resolve("verification/harness/target/evidence/"+name+".json"),report);
        if(report.path("caseId").asText().equals("HARNESS-EXAMPLE")) Json.write(root.resolve("verification/harness/target/evidence/"+mode+"-example.json"),report);
    }
    @Given("고정 관찰 표본과 독립 기대값을 읽는다")
    public void canned() throws IOException { checked=0; }
    @When("표본 수량과 책임을 assertion으로 검사한다")
    public void checkCanned() throws IOException {
        var sample=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
        var observations=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/canned-observations.json"));
        Map<String,com.fasterxml.jackson.databind.JsonNode> results=new HashMap<>(); observations.fields().forEachRemaining(e->results.put(e.getKey(),e.getValue()));
        for(var a:sample.path("subcases").get(0).path("assertions")) {new AssertionEngine().check(a,results);checked++;}
    }
    @Then("제품 실행 coverage 없이 {int}개 assertion 자체가 통과한다")
    public void count(int expected) { if(checked!=expected) throw new AssertionError("Assertion smoke count "+checked+" != "+expected); }
}
