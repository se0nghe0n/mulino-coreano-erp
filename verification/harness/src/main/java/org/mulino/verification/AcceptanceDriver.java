package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

/** Public test port. Implementations call the real product; they never compute an oracle. */
public interface AcceptanceDriver {
    Set<String> availableAdapters();
    StepResult installFixture(String actionId, JsonNode fixture);
    StepResult invoke(String actionId, String route, JsonNode authenticatedActor, String capabilityId, JsonNode request);
    StepResult query(String actionId, String route, JsonNode authenticatedActor, String queryName, JsonNode request);
    StepResult observe(String actionId, JsonNode scopeSnapshotAndTimes);
    StepResult control(String actionId, JsonNode control);
    /** ACK only after a real asynchronous invocation is submitted. data.invocationHandle is opaque. */
    StepResult start(String actionId, String route, JsonNode authenticatedActor, String capabilityId, JsonNode request);
    /** Wait for the real invocation result/commit ACK, preserving its actual response and artifact. */
    StepResult await(String actionId, JsonNode invocationHandle, int timeoutSeconds);
}
