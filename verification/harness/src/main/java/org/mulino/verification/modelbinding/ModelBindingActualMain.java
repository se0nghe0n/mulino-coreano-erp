package org.mulino.verification.modelbinding;

import org.mulino.verification.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.file.Path;
import java.util.ServiceLoader;

/** Actual corpus execution; no fabricated runtime rows, usage or final-response observations. */
public final class ModelBindingActualMain {
    private ModelBindingActualMain() {}
    public static int execute(Path root) throws Exception {
        var contract=new BindingContract(root);ObjectNode report=contract.prepare();
        report.put("profile","UAT").put("runtimeStatus","NOT_RUN").put("status","NOT_RUN");
        report.put("command",System.getProperty("verification.command","verification/model-binding/run actual UAT"));
        report.put("productGateComplete",false);report.set("attempts",Json.array());
        Path target=root.resolve("verification/harness/target/evidence/model-binding-actual.json");
        var providers=ServiceLoader.load(ModelBindingRuntimeProvider.class).stream().toList();
        if(!Boolean.getBoolean("verification.nativeClient.enabled")||providers.size()!=1) {
            report.put("missingReason","Explicit native execution and exactly one runtime provider required").put("exitCode",2);
            Json.write(target,report);return 2;
        }
        var provider=providers.getFirst().get();var port=provider.actualClient();
        if(!port.providedAdapters().containsAll(java.util.Set.of("client","model"))) {
            report.put("missingReason","Native product transport/independent observation integration absent").put("exitCode",2);
            Json.write(target,report);return 2;
        }
        report.remove("missingReason");report.putNull("usage").putNull("cost");
        report.put("usageUnknownReason","Per-invocation native captures are authoritative; this report does not estimate missing usage");
        report.put("costUnknownReason","Subscription native client provides no monetary charge evidence");
        int invocations=0;boolean failed=false,missing=false;
        for(int repeat=1;repeat<=3;repeat++) for(var entry:contract.registry.path("cases")) {
            String caseId=entry.path("caseId").asText();
            var runner=new ModelBindingRunner(contract,caseId,"UAT",provider.driver(),new AgentRunner.Actual(port));
            runner.authorizedRuntime(provider.executionGate());
            String failureClass=null;
            try {
                runner.execute("install");
                for(var turn:contract.binding(caseId).path("turns"))for(var step:turn.path("steps"))
                    runner.execute(turn.path("id").asText()+"/"+step.asText());
                runner.verifyComplete();
            }catch(Exception|AssertionError e){failureClass=e.getClass().getSimpleName();failed=true;}
            ObjectNode evidence=runner.evidence();evidence.put("repeat",repeat);
            if(failureClass!=null)evidence.put("runtimeStatus","FAIL").put("failureClass",failureClass);
            invocations+=evidence.path("actualClientInvocations").asInt();
            failed|=evidence.path("runtimeStatus").asText().equals("FAIL");
            missing|=evidence.path("runtimeStatus").asText().equals("NOT_RUN");
            ((ArrayNode)report.path("attempts")).add(evidence);
            report.put("actualClientInvocations",invocations);
            if(invocations==0)report.put("actualModelCalls",0);else report.putNull("actualModelCalls");
            // Save progress as incomplete even if all completed cases passed so far.
            report.put("status",failed?"FAIL":"NOT_RUN").put("runtimeStatus",failed?"FAIL":"NOT_RUN").put("exitCode",failed?1:2);
            Json.write(target,report);
            // A missing prerequisite cannot be repaired by spending additional subscription calls.
            if(failed||missing)return failed?1:2;
        }
        report.put("status","PASS").put("runtimeStatus","PASS").put("exitCode",0);
        // Product gate and aggregate semantic quality acceptance remain the coordinator's separate checks.
        Json.write(target,report);return 0;
    }
}
