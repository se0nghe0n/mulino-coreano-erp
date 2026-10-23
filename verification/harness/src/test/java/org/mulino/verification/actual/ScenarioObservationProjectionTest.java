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

    @Test void goalProjectionPreservesQuantityUnitAndNeverSearchesNestedExpressionsForDefaults() {
        var row=Json.object().put("targetQuantity","17.25").put("unit","KG");
        var slots=Json.parse("{\"quantity\":{\"value\":\"999\",\"unit\":\"BOX\"},\"conditions\":[{\"source\":{\"placeId\":\"other\"}}],\"includeReserved\":false}");
        ScenarioObservationProjection.goalFields(row,slots);
        assertEquals("17.25",row.path("quantity").asText());assertEquals("KG",row.path("unit").asText());
        assertFalse(row.has("placeId"));assertFalse(row.has("eventKinds"));assertFalse(row.has("evaluationTime"));assertFalse(row.path("includeReservations").asBoolean());
    }
    @Test void snapshotValidationBindsConditionGoalHashAndTimeWithoutReadingMutableFacts() throws Exception {
        var conditions=Json.parse("[{\"conditionId\":\"root.0\",\"inputs\":[{\"state\":\"UNKNOWN\"}]}]");
        var content=Json.object().put("goalId","g");content.set("conditions",conditions);
        var goal=Json.object().put("ID","g").put("organizationId","o").put("workId","w").put("slotsJson","{\"unit\":\"KG\"}");
        content.set("goal",goal);content.set("goalSlots",Json.parse(goal.path("slotsJson").asText()));
        var snapshot=Json.object().put("id","real-id").put("organizationId","o").put("workId","w").put("goalId","g").put("assessmentId","a").put("asOf","time").put("knownAt","known");
        var assessment=snapshot.deepCopy().put("id","a").put("conditionsJson",conditions.toString());
        for(String mutation:java.util.List.of("none","condition","goal","slots","hash")) {
            var c=content.deepCopy();var a=assessment.deepCopy();
            if(mutation.equals("condition"))c.set("conditions",Json.array());
            if(mutation.equals("goal"))((com.fasterxml.jackson.databind.node.ObjectNode)c.path("goal")).put("ID","wrong");
            if(mutation.equals("slots"))c.set("goalSlots",Json.object().put("unit","BOX"));
            String text=c.toString();String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            var s=snapshot.deepCopy().put("contentJson",text).put("contentHash",hash);a.put("inputSnapshotHash",hash);
            if(mutation.equals("hash"))s.put("contentJson",text+" ");
            if(mutation.equals("none"))assertDoesNotThrow(()->ScenarioObservationProjection.validateInputSnapshot(s,c,a));
            else assertThrows(UnsupportedOperationException.class,()->ScenarioObservationProjection.validateInputSnapshot(s,c,a),mutation);
        }
    }

    @Test void waitProjectionCopiesStoredNamesWithoutExpectedValueOrActorDefaults() throws Exception {
        var row=Json.object().put("waitJson","{\"reason\":\"stored reason\",\"verifier\":\"unrelated-verifier\",\"overdueAction\":\"CUSTOM_STORED_ACTION\"}")
            .put("supervisorId","supervisor").put("ownerId","actor").put("closeReason","existing");
        var original=row.deepCopy();
        var projection=new ScenarioObservationProjection(null,null,java.util.List.of(),java.util.List.of(),java.util.List.of(),java.util.List.of(),null);
        projection.enrich("mulino_work_read_Works",Json.array().add(row));
        assertEquals("unrelated-verifier",row.path("verifierId").asText());
        assertEquals("CUSTOM_STORED_ACTION",row.path("overdueAction").asText());
        assertEquals(original.path("waitJson"),row.path("waitJson"));assertFalse(row.has("state"));
        assertTrue(projection.drainQueries().isEmpty());
    }

    @Test void canonicalIdOrUntrustedMarkerWithoutSourceCannotCertifyDelivery() throws Exception {
        var row=Json.object().put("canonicalOccurrenceId","stored-id").put("canonical",true).put("quantity","20").put("legitimateQuantity","0");
        row.set("canonicalSource",Json.object().put("verificationId","untrusted"));
        var projection=new ScenarioObservationProjection(null,null,java.util.List.of(),java.util.List.of(),java.util.List.of(),java.util.List.of(),null);
        projection.enrich("mulino_trade_sales_Deliveries",Json.array().add(row));
        assertFalse(row.has("canonical"));assertFalse(row.has("canonicalSource"));
        assertEquals("20",row.path("quantity").asText());assertEquals("0",row.path("legitimateQuantity").asText());
    }

    @Test void missingOrMalformedCanonicalSourcesAreUnknownRatherThanFalseFacts() {
        var d=Json.object().put("canonicalOccurrenceId","id").put("quantity","20");
        assertFalse(ScenarioObservationProjection.deliveryScope(d,null,Json.object(),Json.object()));
        assertFalse(ScenarioObservationProjection.deliveryScope(d,Json.object(),Json.object(),Json.object()));
        assertFalse(ScenarioObservationProjection.deliverySource(d,Json.object(),Json.object(),null,null,null));
        assertFalse(ScenarioObservationProjection.deliveryOriginal(Json.object(),Json.object().put("blobId","../foreign")));
        assertFalse(d.has("canonical"));assertEquals("20",d.path("quantity").asText());
    }

}
