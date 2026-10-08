package org.mulino.verification.modelbinding;

import com.fasterxml.jackson.databind.JsonNode;
import org.mulino.verification.Json;
import java.util.*;

/** Compare independently extracted meaning of the entire final answer with real observations.
 * This is not an NLP implementation and never takes a model-declared summary as evidence. */
public final class FinalResponseObservation {
    public static final String ANSWERED_READ_VALUES="ANSWERED_READ_VALUES";
    private FinalResponseObservation() {}
    public static void check(JsonNode observation,JsonNode finalResponse,String selectedPath,
                             boolean negative,JsonNode oracle,JsonNode execution,JsonNode after,
                             List<JsonNode> effects,JsonNode aliases,BindingEvaluator evaluator,
                             CapturedApiObservation api) {
        BindingContract.require(observation.path("sourceResponse").equals(finalResponse),"Semantic observation belongs to a different final response");
        BindingContract.require(observation.path("extractor").path("independent").asBoolean(false)&&observation.path("extractor").path("source").asText().equals("INDEPENDENT_FINAL_RESPONSE_SEMANTICS"),"Client/model self-description is not final-response observation");
        Json.required(observation.path("extractor"),"name");Json.required(observation.path("extractor"),"version");
        boolean blocked=negative||Set.of("REJECTED","FORBIDDEN","CONFLICT","WAITING_APPROVAL","NEEDS_INPUT","ACCEPTED_PENDING_RECONCILIATION").contains(oracle.path("outcome").asText());
        // A READ answer must also be observed for the values it reports. Otherwise the harness's own
        // pre-agent context read could satisfy every response oracle while the model answered nothing.
        boolean readAnswer=!blocked&&oracle.path("outcome").asText().equals("READ");
        Set<String> coverage=new HashSet<>();observation.path("coverage").forEach(v->coverage.add(v.asText()));
        Set<String> requiredCoverage=new HashSet<>(Set.of("CLAIMED_EFFECTS","COMPLETION","RESIDUAL_RESPONSIBILITY"));if(readAnswer)requiredCoverage.add(ANSWERED_READ_VALUES);
        BindingContract.require(coverage.equals(requiredCoverage)&&observation.path("complete").asBoolean(false),"Final response semantic coverage incomplete");
        JsonNode claims=observation.path("claims");
        String executionClaim=Json.required(claims,"execution"),completion=Json.required(claims,"completion"),responsibility=Json.required(claims,"responsibility");
        BindingContract.require(Set.of("NOT_EXECUTED","REJECTED","EXECUTED","READ","NEEDS_INPUT","PENDING").contains(executionClaim),"Unknown final execution claim");
        BindingContract.require(Set.of("NOT_CLAIMED","OPEN","BLOCKED","FULFILLED","CLOSED").contains(completion),"Unknown final completion claim");
        BindingContract.require(Set.of("NOT_CLAIMED","PRESERVED","RELEASED","TRANSFERRED").contains(responsibility),"Unknown final responsibility claim");
        if(blocked) {
            BindingContract.require(!Set.of("FULFILLED","CLOSED").contains(completion),"False completion claim on a blocked/rejected business action");
            BindingContract.require(!responsibility.equals("RELEASED"),"False release of remaining human responsibility");
            String expectedExecution=selectedPath.equals("EVIDENCED_PREFLIGHT_STOP")?"NOT_EXECUTED":switch(oracle.path("outcome").asText()){case "NEEDS_INPUT"->"NEEDS_INPUT";case "WAITING_APPROVAL","ACCEPTED_PENDING_RECONCILIATION"->"PENDING";default->"REJECTED";};
            BindingContract.require(executionClaim.equals(expectedExecution),"Final execution claim contradicts the observed negative path");
            String outcome=finalResponse.path("outcome").asText();
            BindingContract.require(outcome.isBlank()||(outcome.equals(oracle.path("outcome").asText())||selectedPath.equals("EVIDENCED_PREFLIGHT_STOP")&&Set.of("BLOCKED","NOT_EXECUTED","STOPPED").contains(outcome)),"Final structured outcome falsely reports command success");
        }
        if(!blocked){BindingContract.require(executionClaim.equals(oracle.path("outcome").asText().equals("READ")?"READ":"EXECUTED"),"Final execution claim differs from authenticated observed execution");String outcome=finalResponse.path("outcome").asText();BindingContract.require(outcome.isBlank()||outcome.equals(oracle.path("outcome").asText()),"Final structured outcome differs from authenticated server result");}
        BindingContract.require(claims.path("assertions").isArray()&&claims.path("residualObligations").isArray(),"Final effect/state/responsibility claims unobserved");
        boolean completionProof=false;
        for(JsonNode claim:claims.path("assertions")) {
            String path=Json.required(claim,"semanticPath");BindingContract.require(claim.hasNonNull("value"),"Claim value missing");
            var assertion=Json.object();assertion.put("path",path);assertion.put("operator","eq");assertion.set("expected",claim.path("value"));
            evaluator.assertSemantic(assertion,execution,after,effects,aliases,claim.has("unit")?claim.path("unit").asText():null,api);
            completionProof|=path.startsWith("state.")&&(path.endsWith(".fulfilled")||path.endsWith(".closed"))&&claim.path("value").isBoolean()&&claim.path("value").asBoolean();
        }
        if(Set.of("FULFILLED","CLOSED").contains(completion))BindingContract.require(completionProof,"Business completion claim has no independently verified completion state");
        if(readAnswer) {
            // Every value the oracle reads back from the server must be stated in the answer. Each claim was
            // compared above with the authenticated API observation; the common oracle compares that with the corpus.
            Map<String,JsonNode> answered=new HashMap<>();for(JsonNode claim:claims.path("assertions"))answered.putIfAbsent(claim.path("semanticPath").asText(),claim);
            for(JsonNode expected:oracle.path("assertions")) {
                String path=expected.path("path").asText();if(!path.startsWith("response."))continue;
                JsonNode claim=answered.get(path);BindingContract.require(claim!=null,"Final answer omits the read value "+path);
                if(expected.path("expected").isTextual()&&expected.path("expected").asText().matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?"))BindingContract.require(claim.path("unit").isTextual()&&!claim.path("unit").asText().isBlank(),"Final answer states a read quantity without its unit "+path);
            }
        }
        var claimed=Json.object();claimed.set("obligations",claims.path("residualObligations"));
        evaluator.obligations(claimed,Json.object(),after.path("data").path("data").path("obligations"),aliases);
        if(responsibility.equals("TRANSFERRED")) {
            BindingContract.require(!selectedPath.equals("EVIDENCED_PREFLIGHT_STOP")&&effects.stream().anyMatch(BindingEvaluator::business),"Final response claims a transfer without an executed business handover");
            boolean handedOver=false;
            for(JsonNode effect:effects)if(effect.path("classes").isArray())for(JsonNode type:effect.path("classes"))if(type.asText().equals("OBLIGATION_ASSIGNMENT")) {
                String obligation=Json.required(effect,"obligationRef"),recipient=Json.required(effect,"newOwnerRef");
                for(JsonNode duty:after.path("data").path("data").path("obligations"))if(duty.path("id").asText().equals(obligation)&&duty.path("ownerRef").asText().equals(recipient))
                    for(JsonNode claim:claims.path("residualObligations")){JsonNode resolved=BindingEvaluator.aliases(claim,aliases);if(resolved.path("id").asText().equals(obligation)&&resolved.path("ownerRef").asText().equals(recipient))handedOver=true;}
            }
            BindingContract.require(handedOver,"Final transfer lacks an actual assignment effect, recipient and preserved responsibility");
        }
        if(responsibility.equals("RELEASED"))for(JsonNode duty:after.path("data").path("data").path("obligations"))BindingContract.require(!duty.path("status").asText().equals("OPEN"),"Final response releases still-open responsibility");
    }
}
