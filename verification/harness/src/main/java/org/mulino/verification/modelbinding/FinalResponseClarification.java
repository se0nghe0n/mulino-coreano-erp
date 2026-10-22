package org.mulino.verification.modelbinding;

import com.fasterxml.jackson.databind.JsonNode;

/** Called only after host/artifact validation. Native text remains byte-for-byte unchanged. */
public final class FinalResponseClarification {
    private FinalResponseClarification() {}
    public static void checkRequirement(JsonNode observation,JsonNode response,boolean required) {
        BindingContract.require(observation.path("complete").asBoolean()&&observation.path("sourceResponse").equals(response),"Clarification response custody differs");
        BindingContract.require(observation.path("extractor").path("independent").asBoolean()&&observation.path("extractor").path("source").asText().equals("INDEPENDENT_FINAL_RESPONSE_SEMANTICS"),"Clarification requires independent final-response semantics");
        String execution=observation.path("claims").path("execution").asText();
        BindingContract.require(java.util.Set.of("NOT_EXECUTED","REJECTED","EXECUTED","READ","NEEDS_INPUT","PENDING").contains(execution),"Clarification execution observation missing");
        BindingContract.require(execution.equals("NEEDS_INPUT")==required,"Clarification requirement differs");
    }
}
