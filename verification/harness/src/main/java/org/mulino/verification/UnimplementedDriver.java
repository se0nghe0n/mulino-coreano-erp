package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

public final class UnimplementedDriver implements AcceptanceDriver, IndependentDbObserver {
    public Set<String> availableAdapters() { return Set.of(); }
    private StepResult missing(String id) { return StepResult.missing(id,"NOT_IMPLEMENTED: real product adapter is absent; no business outcome or observation fabricated"); }
    public StepResult installFixture(String id,JsonNode f) { return missing(id); }
    public StepResult invoke(String id,String route,JsonNode actor,String capability,JsonNode request) { return missing(id); }
    public StepResult query(String id,String route,JsonNode actor,String query,JsonNode request) { return missing(id); }
    public StepResult observe(String id,JsonNode request) { return missing(id); }
    public StepResult control(String id,JsonNode control) { return missing(id); }
    public StepResult start(String id,String route,JsonNode actor,String capability,JsonNode request) { return missing(id); }
    public StepResult await(String id,JsonNode handle,int timeout) { return missing(id); }
}
