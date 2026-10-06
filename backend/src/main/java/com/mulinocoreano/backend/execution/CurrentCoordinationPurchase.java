package com.mulinocoreano.backend.execution;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

/** Selects current responsibility facts, not a stop/approval verdict. Historical rows remain available. */
final class CurrentCoordinationPurchase {
    static JsonNode select(JsonNode purchasing,JsonNode obligations,String currentParent,String currentPlan) {
        if(currentParent==null || currentPlan==null || currentPlan.isEmpty()) return NullNode.getInstance();
        // The repository orders by descending action ID. Never fall back to an older blocked action.
        for(var purchase:purchasing) {
            if(!currentParent.equals(purchase.path("parentWorkItemRef").asText())) continue;
            if(!currentPlan.equals(purchase.path("planRef").asText())) return NullNode.getInstance();
            for(var child:obligations) if(purchase.path("workItemRef").asText().equals(child.path("ref").asText())
                    && currentParent.equals(child.path("parentWorkItemRef").asText())) return purchase;
            return NullNode.getInstance();
        }
        return NullNode.getInstance();
    }
}
