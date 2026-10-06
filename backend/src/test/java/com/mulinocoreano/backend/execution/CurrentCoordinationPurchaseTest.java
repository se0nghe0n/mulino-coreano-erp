package com.mulinocoreano.backend.execution;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

/** Goal 4/5: human stop facts belong to the current responsibility, never arbitrary history. */
class CurrentCoordinationPurchaseTest {
    final ObjectMapper mapper=new ObjectMapper();
    JsonNode read(String s) {return mapper.readTree(s);}
    final String child="[{\"ref\":\"PROC-1\",\"parentWorkItemRef\":\"ROOT-1\"}]";
    final String blocked="{\"workItemRef\":\"PROC-1\",\"parentWorkItemRef\":\"ROOT-1\",\"planRef\":\"PLAN-1\",\"status\":\"BLOCKED\"}";

    @Test void currentParentPlanAndChildSelectTheirActualPurchaseFacts() {
        var rows=read("["+blocked+"]");
        assertThat(CurrentCoordinationPurchase.select(rows,read(child),"ROOT-1","PLAN-1")).isEqualTo(rows.get(0));
        assertThat(rows.size()).isEqualTo(1);
    }

    @Test void unrelatedParentHistoricalPlanAndMissingChildCannotStopCurrentWork() {
        var rows=read("["+blocked+"]");
        assertThat(CurrentCoordinationPurchase.select(rows,read(child),"ROOT-other","PLAN-1").isNull()).isTrue();
        assertThat(CurrentCoordinationPurchase.select(rows,read(child),"ROOT-1","PLAN-new").isNull()).isTrue();
        assertThat(CurrentCoordinationPurchase.select(rows,read("[]"),"ROOT-1","PLAN-1").isNull()).isTrue();
    }

    @Test void replacementPurchaseRemainsCurrentInsteadOfFallingBackToHistoricalBlock() {
        var rows=read("[{\"workItemRef\":\"PROC-2\",\"parentWorkItemRef\":\"ROOT-1\",\"planRef\":\"PLAN-2\",\"status\":\"PENDING\"},"+blocked+"]");
        var children=read("[{\"ref\":\"PROC-2\",\"parentWorkItemRef\":\"ROOT-1\"}]");
        assertThat(CurrentCoordinationPurchase.select(rows,children,"ROOT-1","PLAN-2").path("status").asText()).isEqualTo("PENDING");
        assertThat(CurrentCoordinationPurchase.select(rows,children,"ROOT-1","PLAN-1").isNull()).isTrue();
        assertThat(rows.size()).isEqualTo(2);
    }

    @Test void missingConflictingActorAndUnmatchedActionCannotCountAsManagerBlockEvidence() {
        var valid=read("""
            {"status":"BLOCKED","approvalId":1,"version":2,"proposalHash":"hash","planRef":"PLAN-1",
             "finalGovDecision":{"decisionId":3,"userId":4,"decision":"BLOCK","isFinal":true,"actorRole":"MANAGER",
              "actorProvenance":"IMMUTABLE_PURCHASE_DECIDED_AUDIT","actionId":1,"version":2,"proposalHash":"hash","planRef":"PLAN-1"}}
            """);
        assertThat(ManagerBlockEvidence.matches(valid)).isTrue();
        for(String field:new String[]{"actorRole","actorProvenance","actionId","proposalHash","planRef"}) {
            var conflicting=valid.deepCopy();((tools.jackson.databind.node.ObjectNode)conflicting.path("finalGovDecision")).put(field,"UNKNOWN");
            assertThat(ManagerBlockEvidence.matches(conflicting)).isFalse();
        }
        var cancelled=valid.deepCopy();((tools.jackson.databind.node.ObjectNode)cancelled).put("status","CANCELLED");
        assertThat(ManagerBlockEvidence.matches(cancelled)).isFalse();
        assertThat(ManagerBlockEvidence.matchesStop(cancelled)).isFalse();
        ((tools.jackson.databind.node.ObjectNode)cancelled.path("finalGovDecision")).put("decision","CANCEL");
        assertThat(ManagerBlockEvidence.matchesStop(cancelled)).isTrue();
        assertThat(ManagerBlockEvidence.matches(cancelled)).isFalse();
    }
}
