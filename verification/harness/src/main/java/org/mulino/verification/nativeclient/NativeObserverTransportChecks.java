package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.*;
import org.mulino.verification.actual.ActualConfiguration;
import org.mulino.verification.modelbinding.*;

/** Additional real JDBC/MCP checks in the disposable transport SIT; never opens a model. */
final class NativeObserverTransportChecks {
    static int verify(ActualConfiguration config,NativeIntegrationEvidence evidence,NativeMcpTransport transport,
                      JsonNode actor,JsonNode aliases,JsonNode scope,String asOf,String knownAt,JsonNode snapshot,ArrayNode artifacts) throws Exception {
        var validator=new ContractValidator(evidence.root);
        validator.schema("verification/model-binding/native-physical-observation.schema.json",snapshot);
        PhysicalObservation.validate(validator,snapshot);
        check(snapshot.path("sourceQueries").size()==NativePhysicalCatalog.TABLES.size(),"PHYSICAL_QUERY_INVENTORY_INCOMPLETE");
        check(snapshot.path("rawRows").size()==NativePhysicalCatalog.TABLES.size(),"PHYSICAL_TABLE_INVENTORY_INCOMPLETE");
        check(snapshot.path("obligationObservation").path("rows").isArray(),"OBLIGATION_QUERY_MISSING");
        check(!snapshot.path("effectObservation").path("complete").asBoolean(),"UNATTRIBUTED_EFFECTS_CLAIMED_COMPLETE");
        check(snapshot.path("semanticCoverage").size()>60,"SEMANTIC_MAPPING_GAPS_NOT_RECORDED");
        JsonNode place=snapshot.path("rawRows").path("mulino_inventory_Places").get(0);
        check(place!=null,"SIT_PLACE_MISSING");
        var scopeRequest=Json.object().put("organizationId",scope.path("organizationId").asText()).put("objectType","Place");
        var request=Json.object().put("id",Json.required(place,"ID")).put("asOf",asOf).put("knownAt",knownAt);request.set("scope",scopeRequest);
        var wire=transport.send("observer-place-proof","tools/call","getObject",request,actor,scope);artifacts.add(wire.artifact());
        check(wire.status()==200&&!wire.response().path("result").path("isError").asBoolean(),"PLACE_QUERY_FAILED");
        var proofRequest=Json.object().put("operation","getObject").put("id",Json.required(place,"ID"));proofRequest.set("scope",scopeRequest);
        var captured=new NativePhysicalObserver(config,evidence).capture(scope,asOf,knownAt,proofRequest,aliases.path("reader").asText());
        artifacts.add(evidence.save("projection-proof-check",captured));
        String expected=wire.businessResponse().path("snapshotRevision").asText();
        check(captured.path("projectionRevisionProof").path("revision").asText().equals(expected),"INDEPENDENT_PROJECTION_SHA_MISMATCH");
        check(!captured.path("snapshot").path("id").asText().equals(expected),"MVCC_CONFUSED_WITH_REVISION");
        var envelope=captured.deepCopy().put("snapshotRevision",expected);
        ((ObjectNode)envelope.path("snapshot")).put("readMode","RESULT_REVISION");
        var observeRequest=Json.object().put("asOf",asOf).put("knownAt",knownAt).put("snapshotRef",expected)
            .put("projectionActorId",aliases.path("reader").asText());observeRequest.set("scope",scope);observeRequest.set("projectionRequest",proofRequest);
        ProjectionRevisionObservation.validateArtifact(validator,envelope,observeRequest);
        var mutant=captured.deepCopy();((ObjectNode)mutant.path("physicalCells").get(0)).put("value","forged");
        boolean rejected=false;try{PhysicalObservation.validateLineage(mutant);}catch(IllegalArgumentException e){rejected=true;}
        check(rejected,"FORGED_PHYSICAL_CELL_ACCEPTED");
        var proof=captured.path("projectionRevisionProof").deepCopy();((ObjectNode)proof).put("actorId","different-actor");
        check(!ProjectionRevisionObservation.recompute(proof).equals(expected),"PROJECTION_ACTOR_NOT_BOUND");
        return 13;
    }
    private static void check(boolean b,String code){NativeClientPolicy.require(b,code);}
    private NativeObserverTransportChecks() {}
}
