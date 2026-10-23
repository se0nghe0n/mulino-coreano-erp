package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;

class ScenarioObservationProjectionTest {
    @Test void conditionTruthIsProjectedWithoutRelabelingIdentityOrUnknownInputs() {
        var owner=Json.parse("{\"id\":\"assessment\",\"workId\":\"work\",\"organizationId\":\"org\",\"evaluatorVersion\":\"core-v1\"}");
        var source=Json.parse("[{\"conditionId\":\"root.1\",\"truth\":{\"state\":\"UNVERIFIED\",\"conflict\":true},\"inputs\":[{\"state\":\"UNKNOWN\",\"value\":null}],\"evidenceRefs\":[\"original\"]}]");
        var row=ScenarioObservationProjection.conditionRows(owner,source).get(0);
        assertEquals("root.1",row.path("conditionId").asText());assertEquals("core-v1",row.path("evaluatorVersion").asText());
        assertEquals("UNVERIFIED",row.path("result").asText());assertTrue(row.path("conflict").asBoolean());
        assertEquals(source.get(0).path("inputs"),row.path("inputs"));assertEquals(source.get(0).path("evidenceRefs"),row.path("evidenceIds"));
        assertFalse(row.has("quantityKnowledge"));assertFalse(row.has("quantity"));assertFalse(source.get(0).has("result"));
    }
    @Test void malformedOrUnsupportedPayloadNeverBecomesAnExpectedPredicate() {
        var owner=Json.parse("{\"id\":\"assessment\"}");
        assertTrue(ScenarioObservationProjection.conditionRows(owner,Json.parse("{\"operator\":\"unsupported\"}")).isEmpty());
        assertTrue(ScenarioObservationProjection.parse(Json.parse("{\"payload\":\"not json\"}"),"payload").isNull());
        var rows=ScenarioObservationProjection.conditionRows(owner,Json.parse("[{\"conditionId\":\"unsupported-selector\",\"truth\":{\"state\":\"UNVERIFIED\"}},{\"conditionId\":\"different-selector\",\"truth\":{\"state\":\"UNVERIFIED\"}}]"));
        assertNotEquals(rows.get(0).path("conditionId"),rows.get(1).path("conditionId"));assertFalse(rows.get(0).has("conflict"));
    }
    @Test void equalTruthDoesNotAllowAConditionToImpersonateAnotherAssessment() {
        var owner=Json.parse("{\"id\":\"real-assessment\",\"workId\":\"real-work\"}");
        var forged=Json.parse("[{\"conditionId\":\"root\",\"assessmentId\":\"other-assessment\",\"truth\":{\"state\":\"SATISFIED\"}}]");
        assertThrows(IllegalArgumentException.class,()->ScenarioObservationProjection.conditionRows(owner,forged));
    }

}
