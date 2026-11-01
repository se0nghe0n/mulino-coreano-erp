package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.mulino.verification.Json;

/** Selects only the persisted shared captures named by T09's immutable envelopes. */
final class T09CaptureReplayRows {
    static final String ASSESSMENT_TABLE = "mulino_work_read_AssessmentReferences";
    static final String TABLE = "mulino_evaluation_EvidenceCaptures";
    static final Set<String> TYPED_TABLES = Set.of(ASSESSMENT_TABLE, TABLE);

    private T09CaptureReplayRows() {}

    /**
     * Normalizes PostgreSQL values before artifact serialization. Timestamp values
     * stay ISO-8601 text so the replay installer can parse them using the CSN type.
     */
    static JsonNode databaseValue(Object value) {
        if (value == null) return Json.MAPPER.getNodeFactory().nullNode();
        if (value instanceof OffsetDateTime timestamp)
            return Json.MAPPER.getNodeFactory().textNode(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp));
        if (value instanceof Timestamp timestamp)
            return Json.MAPPER.getNodeFactory().textNode(timestamp.toInstant().toString());
        if (value instanceof BigDecimal || value instanceof Number || value instanceof Boolean)
            return Json.MAPPER.valueToTree(value);
        if (value instanceof java.time.Instant instant)
            return Json.MAPPER.getNodeFactory().textNode(instant.toString());
        return Json.MAPPER.getNodeFactory().textNode(value.toString());
    }

    /**
     * Maps a lower-case JDBC row from rawRows back to its CSN element names and
     * returns only captures actually referenced by the HTTP-recorded envelopes.
     * Missing, duplicate, and cross-organization rows are not repaired or guessed.
     */
    static List<ObjectNode> referencedCaptures(JsonNode artifact, Collection<? extends JsonNode> snapshots,
                                                Map<String, String> csnNames) {
        var organizations = new LinkedHashSet<String>();
        for (var snapshot : snapshots) {
            if (snapshot.hasNonNull("organizationId")) organizations.add(snapshot.path("organizationId").asText());
        }
        if (organizations.size() > 1)
            throw new IllegalArgumentException("T09 replay snapshots span multiple organizations");
        String expectedOrganization = organizations.stream().findFirst().orElse(null);

        var captures = persistedRows(artifact, TABLE, expectedOrganization, csnNames);

        var referenced = new LinkedHashMap<String, ObjectNode>();
        for (var snapshot : snapshots) {
            if (!snapshot.hasNonNull("evidenceCaptureId")) continue; // Legacy envelopes remain supported.
            String id = required(snapshot, "evidenceCaptureId");
            var capture = captures.get(id);
            if (capture == null)
                throw new IllegalArgumentException("Missing actual shared evidence capture " + id);
            if (!snapshot.path("organizationId").equals(capture.path("organizationId")))
                throw new IllegalArgumentException("Shared evidence capture organization mismatch");
            referenced.putIfAbsent(id, capture);
        }
        return List.copyOf(referenced.values());
    }

    /** Reads actual rawRows for one organization; this never derives source rows from HTTP projections. */
    static Map<String, ObjectNode> persistedRows(JsonNode artifact, String table, String organization,
                                                 Map<String, String> csnNames) {
        var rows = new LinkedHashMap<String, ObjectNode>();
        JsonNode rawRows = artifact.path("rawRows").path(table);
        if (!rawRows.isMissingNode() && !rawRows.isArray())
            throw new IllegalArgumentException("Persisted T09 source rows are not an array: " + table);
        if (rawRows.isArray()) for (var raw : rawRows) {
            var row = canonicalRawRow(raw, csnNames);
            String id = required(row, "ID");
            String rowOrganization = required(row, "organizationId");
            if (organization != null && !organization.equals(rowOrganization))
                throw new IllegalArgumentException("Foreign persisted T09 source row: " + table);
            if (rows.putIfAbsent(id, row) != null)
                throw new IllegalArgumentException("Duplicate persisted T09 source row: " + table + "/" + id);
        }
        return Collections.unmodifiableMap(rows);
    }

    /**
     * Checks every persisted HTTP-facing assessment field against its saved row.
     * The only projected field is conditionResults, which must be an exact stored
     * diagnostics view or the exact publicConditions view from the linked envelope.
     */
    static void validateAssessmentProjection(JsonNode http, JsonNode persisted, JsonNode snapshot,
                                             Map<String, String> csnNames) {
        if (!http.path("ID").equals(persisted.path("ID"))
            || !http.path("organizationId").equals(persisted.path("organizationId")))
            throw new IllegalArgumentException("HTTP assessment identity differs from persisted row");
        for (var fields = http.fields(); fields.hasNext();) {
            var field = fields.next();
            if (field.getKey().equals("conditionResults") || !csnNames.containsValue(field.getKey())) continue;
            JsonNode saved = persisted.get(field.getKey());
            if (saved == null || !sameValue(field.getKey(), field.getValue(), saved))
                throw new IllegalArgumentException("HTTP assessment field differs from persisted row: " + field.getKey());
        }

        JsonNode storedConditions = parseJson(persisted.path("conditionsJson"));
        JsonNode actual = http.path("conditionResults");
        if (snapshot == null) {
            if (!actual.equals(storedConditions))
                throw new IllegalArgumentException("HTTP assessment conditionResults differs from stored conditionsJson");
            return;
        }
        if (!snapshot.path("assessmentId").equals(persisted.path("ID"))
            || !snapshot.path("organizationId").equals(persisted.path("organizationId")))
            throw new IllegalArgumentException("HTTP assessment has a mismatched immutable input envelope");
        JsonNode envelope = parseJson(snapshot.path("contentJson"));
        String envelopeHash = sha256(snapshot.path("contentJson").asText());
        if (!envelope.isObject() || !envelopeHash.equals(snapshot.path("contentHash").asText())
            || !snapshot.path("contentHash").equals(persisted.path("inputSnapshotHash"))
            || !snapshot.path("goalId").equals(persisted.path("goalId"))
            || !snapshot.path("workId").equals(persisted.path("workId"))
            || !sameValue("asOf", snapshot.path("asOf"), persisted.path("asOf"))
            || !sameValue("knownAt", snapshot.path("knownAt"), persisted.path("knownAt"))
            || !envelope.path("conditions").equals(storedConditions))
            throw new IllegalArgumentException("Persisted assessment and immutable envelope differ");

        if (actual.equals(storedConditions)) return;
        JsonNode publicConditions = envelope.path("publicConditions");
        if (!publicConditions.isArray() || !actual.equals(publicConditions))
            throw new IllegalArgumentException("HTTP conditionResults is neither stored diagnostics nor exact publicConditions");
    }

    private static boolean sameValue(String name, JsonNode first, JsonNode second) {
        if (Set.of("createdAt", "recordedAt", "effectiveAt", "assessedAt", "knownAt", "asOf").contains(name)
            && first.isTextual() && second.isTextual()) {
            try { return OffsetDateTime.parse(first.asText()).toInstant().equals(OffsetDateTime.parse(second.asText()).toInstant()); }
            catch (RuntimeException invalid) { return false; }
        }
        return first.equals(second);
    }

    private static JsonNode parseJson(JsonNode value) {
        if (value.isTextual()) try { return Json.parse(value.asText()); }
        catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("Invalid persisted T09 JSON source", invalid); }
        return value;
    }

    private static String sha256(String text) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static ObjectNode canonicalRawRow(JsonNode raw, Map<String, String> csnNames) {
        if (!raw.isObject()) throw new IllegalArgumentException("Shared evidence capture row is not an object");
        var result = Json.object();
        for (var fields = raw.fields(); fields.hasNext();) {
            var field = fields.next();
            String name = csnNames.get(field.getKey().toLowerCase(Locale.ROOT));
            if (name == null) throw new IllegalArgumentException("Unknown shared evidence capture column " + field.getKey());
            if (result.has(name)) throw new IllegalArgumentException("Duplicate shared evidence capture column " + name);
            result.set(name, field.getValue().deepCopy());
        }
        return result;
    }

    private static String required(JsonNode row, String field) {
        var value = row.path(field);
        if (!value.isTextual() || value.asText().isBlank())
            throw new IllegalArgumentException("Missing shared evidence capture " + field);
        return value.asText();
    }
}
