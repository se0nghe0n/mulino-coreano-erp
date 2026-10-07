package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

/** Adapter execution availability is separate from business outcome. */
public record StepResult(String actionId, DriverStatus driverStatus, JsonNode data,
                         JsonNode response, String reason, JsonNode provenance, List<String> artifactRefs) {
    public enum DriverStatus { EXECUTED, NOT_IMPLEMENTED, UNAVAILABLE }
    public StepResult {
        if (actionId == null || actionId.isBlank() || driverStatus == null || provenance == null) throw new IllegalArgumentException("Incomplete step result");
        artifactRefs = List.copyOf(artifactRefs);
        if (driverStatus != DriverStatus.EXECUTED && ((data != null && !data.isNull()) || (response != null && !response.isNull())))
            throw new IllegalArgumentException("Unavailable adapter must not return application facts");
        if (driverStatus == DriverStatus.EXECUTED && artifactRefs.isEmpty()) throw new IllegalArgumentException("Executed step requires artifact evidence");
    }
    public ObjectNode toJson() {
        ObjectNode n=Json.object(); n.put("actionId",actionId).put("driverStatus",driverStatus.name());
        n.set("data",data == null ? Json.MAPPER.nullNode() : data); n.set("response",response == null ? Json.MAPPER.nullNode() : response);
        n.put("reason",reason); n.set("provenance",provenance); n.set("artifactRefs",Json.MAPPER.valueToTree(artifactRefs)); return n;
    }
    public static StepResult missing(String id, String reason) {
        ObjectNode p=Json.object(); p.put("adapter","unimplemented").put("adapterVersion","1.0.0").put("buildVersion","NONE");
        p.putNull("authenticatedActor").put("source","NONE").put("independent",false).put("scopeComplete",false);
        p.putNull("sourceQuery").putNull("snapshot");
        return new StepResult(id,DriverStatus.NOT_IMPLEMENTED,null,null,reason,p,List.of());
    }
}
