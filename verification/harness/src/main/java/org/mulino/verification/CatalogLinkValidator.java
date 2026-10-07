package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/** Structural linkage only; supporting assertions and final semantic review remain necessary. */
final class CatalogLinkValidator {
    private static final Set<String> EXACT_QUANTITY_OPS=Set.of("decimalEquals","sumEquals","decimalDelta");
    private CatalogLinkValidator() {}

    static void validate(ContractValidator validator,JsonNode catalog,Map<String,JsonNode> cases,List<String> problems) throws IOException {
        Map<String,JsonNode> oracles=new HashMap<>();
        for(JsonNode oracle:catalog.path("oracles")) oracles.put(oracle.path("oracleId").asText(),oracle);
        Set<String> covered=new HashSet<>(),fixedQuantities=new HashSet<>();
        for(JsonNode c:cases.values()) for(JsonNode sub:c.path("subcases")) for(JsonNode assertion:sub.path("assertions")) {
            JsonNode ref=assertion.path("oracleRef");String id=ref.path("oracleId").asText();JsonNode oracle=oracles.get(id);
            if(oracle==null) {problems.add("Unknown independent oracle "+id);continue;}
            if(!oracle.path("caseId").asText().equals(c.path("caseId").asText())) {
                problems.add("Oracle points at different case "+id);continue;
            }
            for(JsonNode name:ref.path("observationNames")) {
                JsonNode observation=null;
                for(JsonNode item:oracle.path("expectedObservations")) if(item.path("name").asText().equals(name.asText())) observation=item;
                String key=id+"/"+name.asText();
                if(observation==null) {problems.add("Unknown oracle observation "+key);continue;}
                covered.add(key);
                if(observation.path("type").asText().equals("quantity") && fixedQuantityAssertion(assertion,observation)) fixedQuantities.add(key);
                // Relations, identities, counts, and other quantity aspects may support this same
                // observation. They need not repeat its aggregate value; none replaces its primary.
            }
        }
        for(JsonNode oracle:catalog.path("oracles")) for(JsonNode observation:oracle.path("expectedObservations")) {
            String key=oracle.path("oracleId").asText()+"/"+observation.path("name").asText();
            if(!covered.contains(key)) problems.add("Unlinked normative observation "+key);
            else if(observation.path("type").asText().equals("quantity") && !fixedQuantities.contains(key))
                problems.add("Missing substantive fixed quantity assertion (value/unit/operator) "+key);
        }
        for(JsonNode source:catalog.path("sourceFiles"))
            if(!Json.sha256(validator.path(source.path("path").asText())).equals(source.path("sha256").asText()))
                problems.add("Normative source hash drift "+source.path("path"));
    }

    private static boolean fixedQuantityAssertion(JsonNode assertion,JsonNode observation) {
        String op=assertion.path("op").asText();
        boolean quantityOp=switch(observation.path("operator").asText()) {
            case "eq" -> EXACT_QUANTITY_OPS.contains(op);
            case "lte" -> op.equals("decimalAtMost");
            case "gte" -> op.equals("decimalAtLeast");
            default -> false;
        };
        if(!quantityOp || !decimalConstant(assertion.path("expected")) || !decimalConstant(observation.path("expected").path("value"))) return false;
        if(new BigDecimal(assertion.path("expected").asText()).compareTo(new BigDecimal(observation.path("expected").path("value").asText()))!=0) return false;
        JsonNode expectedUnit=observation.path("expected").path("unit");
        if(!expectedUnit.isTextual() || expectedUnit.asText().isBlank() || !expectedUnit.equals(assertion.path("unit"))) return false;
        if(!observedSource(assertion.path("source")) || !observedSource(assertion.path("unitSource"))) return false;
        return !op.equals("decimalDelta") || (observedSource(assertion.path("baseline")) && observedSource(assertion.path("baselineUnitSource"))
            && !assertion.path("source").equals(assertion.path("baseline")));
    }
    private static boolean decimalConstant(JsonNode node) {
        return node.isTextual() && node.asText().matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?");
    }
    private static boolean observedSource(JsonNode source) {
        return source.isObject() && source.path("actionId").isTextual() && !source.path("actionId").asText().isBlank()
            && source.path("pointer").isTextual() && source.path("pointer").asText().startsWith("/");
    }
}
