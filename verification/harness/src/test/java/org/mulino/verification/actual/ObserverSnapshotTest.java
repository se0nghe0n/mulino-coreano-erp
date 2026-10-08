package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;

/** Observer snapshot identity rules; no product runtime/acceptance coverage is claimed. */
public final class ObserverSnapshotTest {
    static final ActualConfiguration UNREACHABLE=new ActualConfiguration(java.net.URI.create("http://127.0.0.1:8080"),"jdbc:postgresql://127.0.0.1:1/unreachable","u","p",Path.of("key"),"i","a","commit");

    @Test void freshReadsReportTheirDirectiveAndNoReferenceIsCurrentCommitted() {
        assertEquals("CURRENT_COMMITTED",ObserverSnapshot.readMode(Json.parse("{}")));
        assertEquals("CURRENT_COMMITTED",ObserverSnapshot.readMode(Json.parse("{\"snapshotRef\":\"CURRENT_COMMITTED\"}")));
        assertEquals("CURRENT_LOCK_WAIT",ObserverSnapshot.readMode(Json.parse("{\"snapshotRef\":\"CURRENT_LOCK_WAIT\"}")));
        assertThrows(IllegalArgumentException.class,()->ObserverSnapshot.readMode(Json.parse("{\"snapshotRef\":{\"$result\":{}}}")));
    }

    /** A resolved product snapshotRevision is never copied into the observer token: every profile refuses before connecting. */
    @Test void resolvedProductRevisionIsNotImplementedInEveryProfileWithoutOpeningAConnection() {
        String hash="a".repeat(64);
        for(String profile:new String[]{"","S3","S4"}) {
            var request=Json.parse("{\"profile\":\""+profile+"\",\"snapshotRef\":\""+hash+"\",\"scope\":{\"organizationId\":\"00000000-0000-0000-0000-000000000001\"},\"sources\":[\"works\"],\"asOf\":\"2026-10-07T09:00:00Z\",\"knownAt\":\"2026-10-07T09:00:00Z\"}");
            var failure=assertThrows(UnsupportedOperationException.class,()->new JdbcObservation(UNREACHABLE).capture(request),profile);
            assertTrue(failure.getMessage().startsWith("RESULT_REVISION"),failure.getMessage());
        }
    }

    @Test void snapshotCarriesTheObserverTokenAndReadModeAndRefusesEchoes() {
        var request=Json.parse("{\"snapshotRef\":\"CURRENT_COMMITTED\"}");
        var snapshot=ObserverSnapshot.snapshot("812:812:",ObserverSnapshot.readMode(request),request);
        assertEquals("812:812:",snapshot.path("id").asText());assertEquals("CURRENT_COMMITTED",snapshot.path("readMode").asText());assertEquals("REPEATABLE_READ",snapshot.path("isolation").asText());
        assertThrows(IllegalStateException.class,()->ObserverSnapshot.snapshot("CURRENT_COMMITTED","CURRENT_COMMITTED",request));
        assertThrows(IllegalStateException.class,()->ObserverSnapshot.snapshot("",ObserverSnapshot.readMode(request),request));
    }
}
