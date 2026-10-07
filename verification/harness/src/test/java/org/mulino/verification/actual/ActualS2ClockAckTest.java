package org.mulino.verification.actual;

import org.junit.jupiter.api.Test;
import org.mulino.verification.Json;
import static org.junit.jupiter.api.Assertions.*;

/** Receipt normalization only. No controlled backend clock execution claim. */
final class ActualS2ClockAckTest {
    @Test void matchingSuccessfulAuthorityResponseProducesRequiredAck() throws Exception {
        var server=Json.parse("{\"instant\":\"2026-10-07T09:00:00Z\",\"profile\":\"verification\",\"authorityClock\":true}");
        var data=ActualAcceptanceDriver.clockAcknowledgment(200,"2026-10-07T09:00:00Z",server);
        assertTrue(data.path("acknowledged").asBoolean());
        assertEquals("clock",data.path("controlType").asText());
        assertEquals("advanceTo",data.path("operation").asText());
        assertTrue(data.hasNonNull("acknowledgedAt"));
        assertEquals(server,data.path("serverObservation"));
        assertEquals(server.path("instant"),data.path("instant"));
    }
    @Test void unsuccessfulOrUnprovenResponseCannotProduceAck() throws Exception {
        var server=Json.parse("{\"instant\":\"2026-10-07T09:00:00Z\",\"profile\":\"verification\",\"authorityClock\":true}");
        assertThrows(IllegalStateException.class,()->ActualAcceptanceDriver.clockAcknowledgment(409,"2026-10-07T09:00:00Z",server));
        assertThrows(IllegalStateException.class,()->ActualAcceptanceDriver.clockAcknowledgment(200,"2026-10-07T09:00:01Z",server));
        var unavailable=(com.fasterxml.jackson.databind.node.ObjectNode)server.deepCopy();unavailable.put("authorityClock",false);
        assertThrows(IllegalStateException.class,()->ActualAcceptanceDriver.clockAcknowledgment(200,"2026-10-07T09:00:00Z",unavailable));
        unavailable.put("authorityClock",true).put("profile","local");
        assertThrows(IllegalStateException.class,()->ActualAcceptanceDriver.clockAcknowledgment(200,"2026-10-07T09:00:00Z",unavailable));
    }
}
