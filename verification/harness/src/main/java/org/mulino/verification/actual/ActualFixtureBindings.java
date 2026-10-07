package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import org.mulino.verification.Json;

/** Explicit isolated identity normalization only; grant scopes/times are never broadened. */
public final class ActualFixtureBindings {
    static JsonNode bind(Path root,JsonNode bundle,ActualConfiguration config) throws Exception {
        String ref=System.getProperty("verification.actual.identityBinding");
        if(ref==null)return bundle;
        Path path=root.resolve(ref).normalize();if(!path.startsWith(root))throw new IllegalArgumentException("Binding manifest escapes root");
        JsonNode manifest=Json.read(path);
        if(!manifest.path("synthetic").asBoolean()||!manifest.path("isolatedLoopbackOnly").asBoolean())throw new IllegalArgumentException("Isolated synthetic binding required");
        if(!config.issuer().equals(Json.required(manifest,"deployedIssuer"))||!config.audience().equals(Json.required(manifest,"deployedAudience")))throw new IllegalArgumentException("Binding differs from real deployed identity configuration");
        ObjectNode result=bundle.deepCopy();
        for(JsonNode actor:result.path("fixture").path("actors")) {
            String issuer=Json.required(actor,"issuer"),audience=Json.required(actor,"audience");
            if(!manifest.path("issuerMappings").path(issuer).asText().equals(config.issuer())||!manifest.path("audienceMappings").path(audience).asText().equals(config.audience()))throw new UnsupportedOperationException("Identity is not explicitly bound by isolated manifest");
            ((ObjectNode)actor).put("issuer",config.issuer()).put("audience",config.audience());
        }
        // Original authored fixture hash remains the contract identity. Binding has separate custody.
        result.put("identityBindingHash",Json.sha256(path));return result;
    }
    private ActualFixtureBindings() {}
}
