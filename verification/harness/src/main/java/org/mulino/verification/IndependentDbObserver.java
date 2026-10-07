package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;

/** Read-only raw-row observer. API projections alone cannot satisfy this port. */
public interface IndependentDbObserver {
    StepResult observe(String actionId, JsonNode scopeSnapshotAndTimes);
}
