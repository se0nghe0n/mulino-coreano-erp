package org.mulino.verification.actual;

import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RecoveryGoalDefinitionTest {
    private final Path root=Path.of(System.getProperty("repo.root", "../.."));
    private ObjectNode common() throws Exception {
        return (ObjectNode)Json.read(root.resolve("verification/actual/scenarios/installer-conventions.json")).path("definition").deepCopy();
    }
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
    @Test void allAuthoredRecoveryContractsReuseOnlyIdenticalCommonCapabilities() throws Exception {
        int count=0;
        for (String caseId : List.of("T26", "V5")) {
            try (var paths=Files.list(root.resolve("verification/cases/"+caseId+"/fixtures"))) {
                for (Path path : paths.filter(p->p.toString().endsWith(".json")).toList()) {
                    var fixture=Json.read(path);
                    var contract=fixture.path("baseline").path("definitionContract");
                    if (contract.isMissingNode()) continue;
                    var original=common();
                    var actual=original.deepCopy();
                    var fixtureBefore=fixture.deepCopy();
                    RecoveryGoalDefinition.apply(actual,contract);
                    assertEquals(fixtureBefore,fixture,path.toString());
                    assertEquals(original.path("verbs"),actual.path("verbs"),path.toString());
                    assertEquals(original.path("capabilities"),actual.path("capabilities"),path.toString());
                    assertEquals(original.path("goals").size()+1,actual.path("goals").size(),path.toString());
                    assertEquals(contract.path("goalTemplate"),actual.path("goals").get(original.path("goals").size()));
                    var ids=new HashSet<String>();
                    actual.path("capabilities").forEach(c->assertTrue(ids.add(c.path("capabilityId").asText())));
                    for (var field=original.fieldNames();field.hasNext();) {
                        String name=field.next();
                        if (!name.equals("goals")) assertEquals(original.get(name),actual.get(name),name);
                    }
                    count++;
                }
            }
        }
        assertEquals(28,count,"Every existing T26/V5 authored contract must be checked");
    }
    @Test void conflictingRepeatedRuntimeMetadataIsRejected() throws Exception {
        var contract=(ObjectNode)Json.read(root.resolve("verification/cases/V5/fixtures/waiting-full-restart.json"))
            .path("baseline").path("definitionContract").deepCopy();
        for (String field : List.of("semanticVersion","evaluatorVersion","inputSchemaVersion","outputSchemaVersion","supportedWorkMigration")) {
            var changed=contract.deepCopy();
            var capability=(ObjectNode)changed.path("capabilities").get(0);
            if (field.equals("supportedWorkMigration")) capability.putArray(field).add("unreviewed-migration");
            else capability.put(field,"unsupported-version");
            var definition=common();
            var failure=assertThrows(IllegalArgumentException.class,()->RecoveryGoalDefinition.apply(definition,changed),field);
            assertEquals("definitionContract conflicts with a capability",failure.getMessage());
        }
    }
}
