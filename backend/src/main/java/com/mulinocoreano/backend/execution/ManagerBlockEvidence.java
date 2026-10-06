package com.mulinocoreano.backend.execution;

import tools.jackson.databind.JsonNode;

/** Read-only provenance check; never approves, applies, normalizes outcomes or changes work state. */
public final class ManagerBlockEvidence {
    private ManagerBlockEvidence() {}
    public static boolean matches(JsonNode purchase) {
        return matches(purchase,"BLOCKED","BLOCK");
    }
    public static boolean matchesStop(JsonNode purchase) {
        return matches(purchase) || matches(purchase,"CANCELLED","CANCEL");
    }
    private static boolean matches(JsonNode purchase,String status,String decision) {
        if(purchase==null || !status.equals(purchase.path("status").asText())) return false;
        var d=purchase.path("finalGovDecision");
        for(String key:new String[]{"decisionId","userId","actionId","version"}) if(!d.path(key).isIntegralNumber()) return false;
        if(!purchase.path("approvalId").isIntegralNumber() || !purchase.path("version").isIntegralNumber() || !d.path("isFinal").isBoolean()) return false;
        return d.path("isFinal").asBoolean() && decision.equals(d.path("decision").asText())
                && "MANAGER".equals(d.path("actorRole").asText())
                && "IMMUTABLE_PURCHASE_DECIDED_AUDIT".equals(d.path("actorProvenance").asText())
                && d.path("decisionId").asLong()>0 && d.path("userId").asLong()>0
                && d.path("actionId").asLong()>0 && d.path("actionId").asLong()==purchase.path("approvalId").asLong()
                && d.path("version").asInt()>0 && d.path("version").asInt()==purchase.path("version").asInt()
                && !d.path("proposalHash").asText().isEmpty() && d.path("proposalHash").asText().equals(purchase.path("proposalHash").asText())
                && !d.path("planRef").asText().isEmpty() && d.path("planRef").asText().equals(purchase.path("planRef").asText());
    }
}
