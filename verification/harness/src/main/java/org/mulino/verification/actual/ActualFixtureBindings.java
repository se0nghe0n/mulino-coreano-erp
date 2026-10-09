package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import org.mulino.verification.Json;

/** Explicit isolated identity normalization only; grant scopes/times are never broadened. */
public final class ActualFixtureBindings {
    /** Marker on a bound actor whose authored issuer/audience is a declared negative identity. */
    static final String UNTRUSTED="untrustedAuthoredIdentity";
    static JsonNode bind(Path root,JsonNode bundle,ActualConfiguration config) throws Exception {
        JsonNode manifest=manifest(root,config);
        if(manifest==null)return bundle;
        ObjectNode result=bundle.deepCopy();
        for(JsonNode actor:result.path("fixture").path("actors")) bindActor(manifest,config,(ObjectNode)actor);
        // Original authored fixture hash remains the contract identity. Binding has separate custody.
        result.put("identityBindingHash",Json.sha256(manifestPath(root)));return result;
    }
    /** The credential identity the driver signs for an authored actor: mapped when trusted, verbatim when declared untrusted. */
    static JsonNode credentialActor(Path root,ActualConfiguration config,JsonNode authored) throws Exception {
        JsonNode manifest=manifest(root,config);
        ObjectNode copy=authored.deepCopy();
        if(manifest!=null) bindActor(manifest,config,copy);
        return copy;
    }
    private static void bindActor(JsonNode manifest,ActualConfiguration config,ObjectNode actor) {
        String issuer=Json.required(actor,"issuer"),audience=Json.required(actor,"audience");
        boolean untrusted=contains(manifest.path("untrustedIssuers"),issuer)||contains(manifest.path("untrustedAudiences"),audience);
        if(untrusted){actor.put(UNTRUSTED,true);return;}
        if(!manifest.path("issuerMappings").path(issuer).asText().equals(config.issuer())||!manifest.path("audienceMappings").path(audience).asText().equals(config.audience()))throw new UnsupportedOperationException("Identity is not explicitly bound by isolated manifest");
        actor.put("issuer",config.issuer()).put("audience",config.audience());
    }
    private static Path manifestPath(Path root) {
        String ref=System.getProperty("verification.actual.identityBinding");
        if(ref==null)return null;
        Path base=root.toAbsolutePath().normalize();Path path=base.resolve(ref).normalize();if(!path.startsWith(base))throw new IllegalArgumentException("Binding manifest escapes root");
        return path;
    }
    private static JsonNode manifest(Path root,ActualConfiguration config) throws Exception {
        Path path=manifestPath(root);if(path==null)return null;
        JsonNode manifest=Json.read(path);
        if(!manifest.path("synthetic").asBoolean()||!manifest.path("isolatedLoopbackOnly").asBoolean())throw new IllegalArgumentException("Isolated synthetic binding required");
        if(!config.issuer().equals(Json.required(manifest,"deployedIssuer"))||!config.audience().equals(Json.required(manifest,"deployedAudience")))throw new IllegalArgumentException("Binding differs from real deployed identity configuration");
        return manifest;
    }
    private static boolean contains(JsonNode array,String value){for(JsonNode n:array)if(n.asText().equals(value))return true;return false;}
    private ActualFixtureBindings() {}
}
