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
    public static void checkSlots(JsonNode observation,JsonNode expected) {
        JsonNode observed=observation.path("claims").path("missingSlots");
        BindingContract.require(observed.isArray()&&expected.isArray(),"Independent missing-slot set absent");
        var actual=new java.util.HashSet<String>();var wanted=new java.util.HashSet<String>();
        for(JsonNode slot:observed)BindingContract.require(slot.isTextual()&&!slot.asText().isBlank()&&actual.add(slot.asText()),"Invalid observed missing-slot set");
        for(JsonNode slot:expected)BindingContract.require(slot.isTextual()&&wanted.add(slot.asText()),"Invalid expected missing-slot set");
        BindingContract.require(actual.equals(wanted),"Independent missing-slot set differs");
    }
}
