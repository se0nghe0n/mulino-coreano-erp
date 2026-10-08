package org.mulino.verification.modelbinding;
import org.mulino.verification.*;
import io.cucumber.java.en.*;
import java.nio.file.*;

public final class ModelBindingGlue {
    private ModelBindingRunner runner;
    @Given("model binding {string}를 준비한다")
    public void prepare(String id) throws Exception {var c=new BindingContract(Path.of(System.getProperty("repo.root")));String profile=System.getProperty("model.binding.profile","SIT");var providers=java.util.ServiceLoader.load(ModelBindingRuntimeProvider.class).stream().toList();BindingContract.require(providers.size()<=1,"Ambiguous runtime providers");ModelBindingRuntimeProvider provider=providers.isEmpty()?null:providers.get(0).get();AcceptanceDriver driver=provider==null?new UnimplementedDriver():provider.driver();AgentRunner agent=profile.equals("SIT")?new AgentRunner.Scripted():new AgentRunner.Actual(provider==null?(action,route,actor,text,context)->StepResult.missing(action,"NOT_IMPLEMENTED: actual model/client port absent"):provider.actualClient());runner=new ModelBindingRunner(c,id,profile,driver,agent);if(provider!=null&&profile.equals("UAT"))runner.authorizedRuntime(provider.executionGate());}
    @When("model binding {string}의 {string} 단계를 실행한다")
    public void execute(String id,String step) throws Exception {runner.execute(step);}
    @Then("model binding {string}의 {string} oracle로 {string}를 확인한다")
    public void assertion(String id,String assertion,String semantic){runner.declaredAssertion(assertion,semantic);}
    @Then("model binding {string}의 모든 turn을 독립 관찰로 판정한다")
    public void judge(String id){runner.verifyComplete();String status=runner.status();
        // An observed violation is FAIL, never relabelled as missing implementation/NOT_RUN.
        if(status.equals("FAIL"))throw new AssertionError("FAIL: "+id+" observed model/product acceptance violation: "+runner.evidence().path("perTurn"));
        if(!status.equals("PASS"))throw new AssertionError("NOT_IMPLEMENTED: "+id+" actual product/model observations missing; runtime NOT_RUN");}
}
