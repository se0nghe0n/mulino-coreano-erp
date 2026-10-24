package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;

class T09CaptureReplayRowsTest {
    static final String ORG = "org-1";
    static final String CAPTURE = "capture-1";
    static final String AT = "2026-10-07T02:00:00Z";
    static final Map<String,String> COLUMNS = Map.of(
        "id", "ID", "organizationid", "organizationId", "asof", "asOf",
        "knownat", "knownAt", "recordedat", "recordedAt", "contenthash", "contentHash",
        "contentjson", "contentJson");
    static final Map<String,String> ASSESSMENT_COLUMNS = Map.ofEntries(
        Map.entry("id", "ID"), Map.entry("organizationid", "organizationId"), Map.entry("asof", "asOf"),
        Map.entry("knownat", "knownAt"), Map.entry("recordedat", "recordedAt"), Map.entry("createdat", "createdAt"),
        Map.entry("effectiveat", "effectiveAt"), Map.entry("assessedat", "assessedAt"), Map.entry("workid", "workId"),
        Map.entry("goalid", "goalId"), Map.entry("outcome", "outcome"), Map.entry("evaluatorversion", "evaluatorVersion"),
        Map.entry("inputsnapshothash", "inputSnapshotHash"), Map.entry("definitionversionid", "definitionVersionId"),
        Map.entry("policyversionid", "policyVersionId"), Map.entry("previousassessmentid", "previousAssessmentId"),
        Map.entry("deadlineviolated", "deadlineViolated"), Map.entry("held", "held"), Map.entry("conflict", "conflict"),
        Map.entry("revision", "revision"), Map.entry("conditionsjson", "conditionsJson"));

    @Test void selectsOnlyActualReferencedCaptureAndKeepsLegacyEnvelopes() {
        var artifact = artifact(captureRow(CAPTURE, ORG));
        var snapshots = List.<JsonNode>of(Json.object().put("ID", "envelope-1").put("organizationId", ORG)
            .put("evidenceCaptureId", CAPTURE));
        var selected = T09CaptureReplayRows.referencedCaptures(artifact, snapshots, COLUMNS);
        assertEquals(1, selected.size());
        assertEquals(CAPTURE, selected.getFirst().path("ID").asText());
        assertEquals(ORG, selected.getFirst().path("organizationId").asText());

        var legacy = Json.object().put("ID", "legacy-envelope").put("organizationId", ORG);
        assertTrue(T09CaptureReplayRows.referencedCaptures(Json.object(), List.of(legacy), COLUMNS).isEmpty());
    }

    @Test void missingCaptureRelationshipFailsInsteadOfFabricatingOne() {
        var artifact = artifact();
        var snapshot = Json.object().put("ID", "envelope-1").put("organizationId", ORG).put("evidenceCaptureId", CAPTURE);
        var failure = assertThrows(IllegalArgumentException.class,
            () -> T09CaptureReplayRows.referencedCaptures(artifact, List.of(snapshot), COLUMNS));
        assertTrue(failure.getMessage().contains("Missing actual shared evidence capture"));
    }

    @Test void duplicateAndForeignCaptureRowsFailClosed() {
        var snapshot = Json.object().put("ID", "envelope-1").put("organizationId", ORG).put("evidenceCaptureId", CAPTURE);
        var duplicate = artifact(captureRow(CAPTURE, ORG), captureRow(CAPTURE, ORG));
        assertTrue(assertThrows(IllegalArgumentException.class,
            () -> T09CaptureReplayRows.referencedCaptures(duplicate, List.of(snapshot), COLUMNS))
            .getMessage().contains("Duplicate"));

        var foreign = artifact(captureRow(CAPTURE, "org-foreign"));
        assertTrue(assertThrows(IllegalArgumentException.class,
            () -> T09CaptureReplayRows.referencedCaptures(foreign, List.of(snapshot), COLUMNS))
            .getMessage().contains("Foreign"));
    }

    @Test void postgresTimestampSerializationRemainsTypedAndParseable() {
        var timestamp = OffsetDateTime.parse("2026-10-07T02:00:00.123456Z");
        JsonNode serialized = T09CaptureReplayRows.databaseValue(timestamp);
        assertEquals(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp), serialized.asText());
        assertEquals(timestamp.toInstant(), OffsetDateTime.parse(serialized.asText()).toInstant());
        assertTrue(T09CaptureReplayRows.databaseValue(new BigDecimal("12.50")).isNumber());
        assertTrue(T09CaptureReplayRows.databaseValue(true).asBoolean());
        assertTrue(T09CaptureReplayRows.databaseValue(null).isNull());
    }

    @Test void assessmentReplayAllowsOnlyTheTwoExactEnvelopeConditionViews() throws Exception {
        var internal = Json.array().add(Json.object().put("conditionId", "root.0")).add(Json.object().put("conditionId", "root"));
        var publicView = Json.array().add(Json.object().put("conditionId", "arrived-at-W").set("diagnostics", internal.deepCopy()));
        var envelope = Json.object().put("goalId", "goal-1");envelope.set("conditions", internal.deepCopy());envelope.set("publicConditions", publicView.deepCopy());
        String bytes=envelope.toString(), hash=ScenarioObservationPostgresTest.inputHash(bytes);
        var stored=Json.object().put("ID", "assessment-1").put("organizationId", ORG).put("asOf", AT).put("knownAt", AT)
            .put("recordedAt", AT).put("createdAt", AT).put("effectiveAt", AT).put("assessedAt", AT).put("workId", "work-1")
            .put("goalId", "goal-1").put("outcome", "SATISFIED").put("evaluatorVersion", "core-v1")
            .put("inputSnapshotHash", hash).put("conditionsJson", internal.toString());
        var snapshot=Json.object().put("ID", "envelope-1").put("organizationId", ORG).put("assessmentId", "assessment-1")
            .put("workId", "work-1").put("goalId", "goal-1").put("asOf", AT).put("knownAt", AT).put("contentHash", hash).put("contentJson", bytes);
        var internalHttp=stored.deepCopy();internalHttp.remove("conditionsJson");internalHttp.set("conditionResults", internal.deepCopy());
        var publicHttp=stored.deepCopy();publicHttp.remove("conditionsJson");publicHttp.set("conditionResults", publicView.deepCopy());
        assertDoesNotThrow(() -> T09CaptureReplayRows.validateAssessmentProjection(internalHttp,stored,snapshot,ASSESSMENT_COLUMNS));
        assertDoesNotThrow(() -> T09CaptureReplayRows.validateAssessmentProjection(publicHttp,stored,snapshot,ASSESSMENT_COLUMNS));

        var thirdView=publicHttp.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)thirdView.path("conditionResults").get(0)).put("conditionId", "invented-condition");
        assertTrue(assertThrows(IllegalArgumentException.class,
            () -> T09CaptureReplayRows.validateAssessmentProjection(thirdView,stored,snapshot,ASSESSMENT_COLUMNS))
            .getMessage().contains("neither stored diagnostics nor exact publicConditions"));
        var changedField=publicHttp.deepCopy().put("outcome", "UNSATISFIED");
        assertTrue(assertThrows(IllegalArgumentException.class,
            () -> T09CaptureReplayRows.validateAssessmentProjection(changedField,stored,snapshot,ASSESSMENT_COLUMNS))
            .getMessage().contains("outcome"));
    }

    @Test void sharedCaptureValidatorBindsIdentityHashTimeBoundaryAndFacts() throws Exception {
        var fixture = validationFixture();
        assertDoesNotThrow(() -> ScenarioObservationProjection.validateCapture(fixture.snapshot(), fixture.envelope(), fixture.capture()));

        var missing = assertThrows(UnsupportedOperationException.class,
            () -> ScenarioObservationProjection.validateCapture(fixture.snapshot(), fixture.envelope(), null));
        assertTrue(missing.getMessage().contains("Missing shared evidence capture"));

        var wrongIdentity = fixture.snapshot().deepCopy().put("evidenceCaptureId", "other-capture");
        assertThrows(UnsupportedOperationException.class,
            () -> ScenarioObservationProjection.validateCapture(wrongIdentity, fixture.envelope(), fixture.capture()));
        var wrongTime = fixture.capture().deepCopy().put("knownAt", "2026-10-08T02:00:00Z");
        assertThrows(UnsupportedOperationException.class,
            () -> ScenarioObservationProjection.validateCapture(fixture.snapshot(), fixture.envelope(), wrongTime));
        var wrongScope = fixture.envelope().deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode)wrongScope.path("captureBoundary").path("scope")).put("itemId", "other-item");
        assertThrows(UnsupportedOperationException.class,
            () -> ScenarioObservationProjection.validateCapture(fixture.snapshot(), wrongScope, fixture.capture()));
        var wrongHash = fixture.capture().deepCopy().put("contentHash", "0".repeat(64));
        assertThrows(UnsupportedOperationException.class,
            () -> ScenarioObservationProjection.validateCapture(fixture.snapshot(), fixture.envelope(), wrongHash));
    }

    @Test void sharedCaptureValidatorRejectsForeignAndConflictingSourceRows() throws Exception {
        var foreign = validationFixture();
        var foreignBody = Json.parse(foreign.capture().path("contentJson").asText());
        ((com.fasterxml.jackson.databind.node.ObjectNode)foreignBody.path("facts").path("sources")).set("test", Json.array()
            .add(Json.object().put("ID", "source-1").put("organizationId", "org-foreign")));
        var foreignCapture = foreign.capture().deepCopy().put("contentJson", foreignBody.toString())
            .put("contentHash", ScenarioObservationPostgresTest.inputHash(foreignBody.toString()));
        assertTrue(assertThrows(UnsupportedOperationException.class,
            () -> ScenarioObservationProjection.validateCapture(foreign.snapshot(), foreign.envelope(), foreignCapture))
            .getMessage().contains("Foreign source"));

        var conflicting = validationFixture();
        var body = Json.parse(conflicting.capture().path("contentJson").asText());
        ((com.fasterxml.jackson.databind.node.ObjectNode)body.path("facts").path("sources")).set("test", Json.array()
            .add(Json.object().put("ID", "source-1").put("value", "first"))
            .add(Json.object().put("ID", "source-1").put("value", "different")));
        var conflictingCapture = conflicting.capture().deepCopy().put("contentJson", body.toString())
            .put("contentHash", ScenarioObservationPostgresTest.inputHash(body.toString()));
        assertTrue(assertThrows(UnsupportedOperationException.class,
            () -> ScenarioObservationProjection.validateCapture(conflicting.snapshot(), conflicting.envelope(), conflictingCapture))
            .getMessage().contains("Conflicting immutable source identity"));
    }

    record ValidationFixture(ObjectNode snapshot, ObjectNode envelope, ObjectNode capture) {}

    static ValidationFixture validationFixture() throws Exception {
        var boundary = Json.object().put("organizationId", ORG).put("asOf", AT).put("knownAt", AT);
        boundary.set("scope", Json.object().put("itemId", "item-1"));
        var facts = Json.object();facts.set("properties", Json.object());facts.set("sources", Json.object());
        var content = Json.object();content.set("boundary", boundary);content.set("facts", facts);
        var snapshot = Json.object().put("evidenceCaptureId", CAPTURE).put("organizationId", ORG).put("asOf", AT).put("knownAt", AT);
        var envelope = Json.object();envelope.set("captureBoundary", boundary.deepCopy());envelope.set("facts", facts.deepCopy());
        String bytes = content.toString();
        var capture = Json.object().put("id", CAPTURE).put("organizationId", ORG).put("asOf", AT).put("knownAt", AT)
            .put("contentJson", bytes).put("contentHash", ScenarioObservationPostgresTest.inputHash(bytes));
        return new ValidationFixture(snapshot, envelope, capture);
    }

    static JsonNode artifact(JsonNode... captureRows) {
        var artifact = Json.object();var rawRows = Json.object();var rows = Json.array();
        for (var row : captureRows) rows.add(row);
        rawRows.set(T09CaptureReplayRows.TABLE, rows);artifact.set("rawRows", rawRows);return artifact;
    }

    static JsonNode captureRow(String id, String organization) {
        return Json.object().put("id", id).put("organizationid", organization);
    }
}
