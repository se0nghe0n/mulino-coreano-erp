package org.mulino.verification.actual;

import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class RecoveryGoalDefinitionTest {
    @Test void typedTemplateIsInstalledVerbatimAndNeverCreatesEvidence() throws Exception {
        Path root=Path.of(System.getProperty("repo.root", "../.."));
        var fixture=Json.read(root.resolve("verification/cases/V5/fixtures/waiting-full-restart.json"));
        var definition=(com.fasterxml.jackson.databind.node.ObjectNode)Json.read(root.resolve("verification/actual/scenarios/installer-conventions.json")).path("definition").deepCopy();
        var contract=fixture.path("baseline").path("definitionContract");
        int old=definition.path("goals").size();
        RecoveryGoalDefinition.apply(definition,contract);
        assertEquals(old+1,definition.path("goals").size());
        assertEquals(contract.path("goalTemplate"),definition.path("goals").get(old));
        assertFalse(definition.has("evidence"));
        assertThrows(IllegalArgumentException.class,()->RecoveryGoalDefinition.apply(definition,contract));
    }
    @Test void unsupportedEvaluatorIsNotSilentlyReplaced() throws Exception {
        var definition=Json.object().put("version","definition-v1").put("evaluatorVersion","core-v1");
        var contract=Json.object().put("definitionVersion","definition-v1").put("evaluatorVersion","future");
        contract.set("goalTemplate",Json.object().put("evaluatorVersion","future"));
        assertThrows(IllegalArgumentException.class,()->RecoveryGoalDefinition.apply(definition,contract));
    }
}
