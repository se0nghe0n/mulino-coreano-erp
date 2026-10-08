package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.*;
import java.time.Instant;
import java.util.Set;
import org.mulino.verification.Json;

/**
 * The observer's own MVCC snapshot identity (contracts/acceptance-observation.schema.json, harness-guide).
 * snapshot.id is the observer connection's pg_current_snapshot(), never the requested snapshotRef. A literal
 * directive (or no snapshotRef) is a fresh read whose readMode is reported. A resolved API snapshotRevision
 * (RESULT_REVISION) would require recomputing the product's projection hash from rows; that recomputation does
 * not exist independently of the product, so it is reported as NOT_IMPLEMENTED instead of copying the token.
 */
final class ObserverSnapshot {
    static final Set<String> DIRECTIVES=Set.of("CURRENT_COMMITTED","CURRENT_LOCK_WAIT");
    private ObserverSnapshot() {}

    /** Resolves the read mode before any connection is opened. */
    static String readMode(JsonNode request) {
        JsonNode ref=request.path("snapshotRef");
        if(ref.isMissingNode()||ref.isNull())return "CURRENT_COMMITTED";
        if(!ref.isTextual()||ref.asText().isBlank())throw new IllegalArgumentException("snapshotRef must be a resolved non-empty string");
        if(DIRECTIVES.contains(ref.asText()))return ref.asText();
        throw new UnsupportedOperationException("RESULT_REVISION recomputation of the API projection revision is not implemented independently; the requested token is not a PostgreSQL snapshot and is never echoed");
    }

    /** First statement of the observer's REPEATABLE READ transaction: the snapshot every later read uses. */
    static String token(Connection connection) throws SQLException {
        try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT pg_current_snapshot()::text")){rows.next();return rows.getString(1);}
    }

    static ObjectNode snapshot(String token,String readMode,JsonNode request) {
        if(token==null||token.isBlank())throw new IllegalStateException("Observer snapshot token missing");
        if(token.equals(request.path("snapshotRef").asText())||DIRECTIVES.contains(token))throw new IllegalStateException("Observer token must not echo the requested reference");
        var snapshot=Json.object();snapshot.put("id",token).put("isolation","REPEATABLE_READ").put("capturedAt",Instant.now().toString()).put("readMode",readMode);return snapshot;
    }
}
