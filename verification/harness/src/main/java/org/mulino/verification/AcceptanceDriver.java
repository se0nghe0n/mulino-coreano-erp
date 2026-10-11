package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

/** Public test port. Implementations call the real product; they never compute an oracle. */
public interface AcceptanceDriver {
    Set<String> availableAdapters();
    StepResult installFixture(String actionId, JsonNode fixture);
    StepResult invoke(String actionId, String route, JsonNode authenticatedActor, String capabilityId, JsonNode request);
    StepResult query(String actionId, String route, JsonNode authenticatedActor, String queryName, JsonNode request);
    /** Declared protocol operation is classification only. Preserve hostile raw headers/body unchanged. */
    default StepResult wire(String actionId,JsonNode authenticatedActor,String protocolOperation,JsonNode rawRequest) {
        return StepResult.missing(actionId,"NOT_IMPLEMENTED: actual raw protocol transport adapter absent");
    }
    /**
     * Same call with the action's resolved {@code action.harness} (test barrier, environment, authentication variant,
     * worker execution context, MCP protocol context). The harness block is never part of the product request; an
     * adapter that does not consume it falls back to the five-argument call.
     */
    default StepResult invoke(String actionId, String route, JsonNode authenticatedActor, String capabilityId, JsonNode request, JsonNode harness) {
        return invoke(actionId,route,authenticatedActor,capabilityId,request);
    }
    default StepResult query(String actionId, String route, JsonNode authenticatedActor, String queryName, JsonNode request, JsonNode harness) {
        return query(actionId,route,authenticatedActor,queryName,request);
    }
    default StepResult start(String actionId, String route, JsonNode authenticatedActor, String capabilityId, JsonNode request, JsonNode harness) {
        return start(actionId,route,authenticatedActor,capabilityId,request);
    }
    StepResult observe(String actionId, JsonNode scopeSnapshotAndTimes);
    StepResult control(String actionId, JsonNode control);
    /** ACK only after a real asynchronous invocation is submitted. data.invocationHandle is opaque. */
    StepResult start(String actionId, String route, JsonNode authenticatedActor, String capabilityId, JsonNode request);
    /** Submit real raw protocol transport asynchronously; never delegate to synchronous wire().
     * Classification does not normalize raw method/headers/body. ACK requires actual invocationHandle. */
    default StepResult startWire(String actionId,JsonNode authenticatedActor,String protocolOperation,JsonNode rawRequest) {
        return StepResult.missing(actionId,"NOT_IMPLEMENTED: actual async raw protocol transport adapter absent");
    }
    /** Terminal ACK: completed=true, identical invocationHandle, terminalStatus SUCCEEDED/FAILED/CANCELLED.
     * Preserve the actual result/commit response and artifact; submission alone is not completion. */
    StepResult await(String actionId, JsonNode invocationHandle, int timeoutSeconds);
}
