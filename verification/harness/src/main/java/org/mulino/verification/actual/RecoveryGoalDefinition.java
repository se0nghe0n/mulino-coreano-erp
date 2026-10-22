package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mulino.verification.Json;

/** Installs an authored typed template; never synthesizes observations or goal answers. */
final class RecoveryGoalDefinition {
    private RecoveryGoalDefinition() {}
    static void apply(ObjectNode content, JsonNode contract) {
        if (contract.isMissingNode() || contract.isNull()) return;
        if (!contract.path("definitionVersion").asText().equals(content.path("version").asText())) return;
        JsonNode template = contract.path("goalTemplate");
        if (!template.isObject() || !contract.path("evaluatorVersion").asText().equals(content.path("evaluatorVersion").asText())
                || !template.path("evaluatorVersion").asText().equals(content.path("evaluatorVersion").asText()))
            throw new IllegalArgumentException("Authored definitionContract requires a typed template and matching evaluator");
        var goals = Json.array();
        for (JsonNode goal : content.path("goals")) {
            if (goal.path("endpoint").equals(template.path("endpoint")) && goal.path("quantityMode").equals(template.path("quantityMode")))
                throw new IllegalArgumentException("definitionContract duplicates a published endpoint/mode");
            goals.add(goal);
        }
        goals.add(template.deepCopy());
        content.set("goals", goals);
    }
}
