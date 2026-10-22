package org.mulino.verification.modelbinding;

import com.fasterxml.jackson.databind.JsonNode;
import org.mulino.verification.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Recomputable ApplicationQueries snapshot recipe; MVCC tokens are never compared to this hash. */
public final class ProjectionRevisionObservation {
    public static final String RECIPE = "APPLICATION_QUERIES_V1_SIMPLE_OBJECT";
    public static final List<String> SOURCES=List.of("backend/src/main/java/com/mulino/application/core/ApplicationQueries.java",
        "backend/src/main/java/com/mulino/application/core/TransportValues.java",
        "backend/src/main/java/com/mulino/application/inventory/InventoryQueries.java",
        "backend/src/main/java/com/mulino/domain/inventory/InventoryRepository.java");
    private ProjectionRevisionObservation() {}
    public static String recompute(JsonNode proof) {
        BindingContract.require(proof.path("recipe").asText().equals(RECIPE), "Unsupported independent projection recipe");
        JsonNode request=proof.path("request"), scope=request.path("scope");
        Set<String> requestKeys=new HashSet<>();request.fieldNames().forEachRemaining(requestKeys::add);
        BindingContract.require(requestKeys.equals(Set.of("operation","id","scope")),"Unsupported projection request options");
        BindingContract.require(request.path("operation").asText().equals("getObject")
            && Set.of("Place","Product","Manufacturer").contains(scope.path("objectType").asText()), "Unsupported projection scope");
        BindingContract.require(!scope.has("itemId") && !scope.has("lotId") && !scope.has("placeId")
            && !scope.has("customerId") && !scope.has("workId"), "World projection requires backend lineage support");
        Set<String> keys=new HashSet<>();scope.fieldNames().forEachRemaining(keys::add);
        BindingContract.require(keys.equals(Set.of("organizationId","objectType")), "Unexpected simple projection scope");
        JsonNode rows=proof.path("rows"), row=rows.path("object");
        BindingContract.require(row.isObject() && row.path("ID").equals(request.path("id"))
            && row.path("organizationId").equals(scope.path("organizationId")), "Projection raw row identity mismatch");
        BindingContract.require(rows.path("relations").isArray() && rows.path("relations").isEmpty(),
            "Related object visibility requires backend lineage support");
        BindingContract.require(!row.has("itemId"), "Simple projection unexpectedly contains item scope");
        String org=Json.required(scope,"organizationId"), actor=Json.required(proof,"actorId");
        String asOf=Instant.parse(Json.required(proof,"asOf")).toString(), knownAt=Instant.parse(Json.required(proof,"knownAt")).toString();
        var state=(com.fasterxml.jackson.databind.node.ObjectNode)row.deepCopy();state.set("relations",Json.array());
        String raw=org+"|"+actor+"|"+asOf+"|"+knownAt+"|"+canonical(scope)+"|"+canonical(state);
        return Json.sha256Text(raw);
    }
    static String canonical(JsonNode value) {
        if(value.isObject()) {var sorted=Json.object();var fields=new TreeMap<String,JsonNode>();value.fields().forEachRemaining(e->fields.put(e.getKey(),e.getValue()));fields.forEach((k,v)->sorted.set(k,Json.parse(canonical(v))));return sorted.toString();}
        if(value.isArray()) {var array=Json.array();value.forEach(v->array.add(Json.parse(canonical(v))));return array.toString();}
        return value.toString();
    }
    public static void validateArtifact(ContractValidator validator,JsonNode observation,JsonNode requested) throws IOException {
        BindingContract.require(observation.path("snapshot").path("readMode").asText().equals("RESULT_REVISION"), "Projection revision read mode required");
        String ref=Json.required(observation.path("snapshot"),"artifactRef");Path path=validator.path(ref).toRealPath();
        BindingContract.require(path.startsWith(validator.root().toRealPath()),"Projection artifact escapes repository");
        JsonNode capture=Json.read(path),proof=capture.path("projectionRevisionProof");
        validator.schema("verification/model-binding/projection-revision-observation.schema.json",proof);
        BindingContract.require(capture.path("rawRows").equals(observation.path("rawRows"))
            && capture.path("snapshot").path("id").equals(observation.path("snapshot").path("id")),"Revision proof has a different DB read");
        BindingContract.require(proof.path("asOf").equals(requested.path("asOf"))&&proof.path("knownAt").equals(requested.path("knownAt"))
            &&proof.path("request").path("scope").path("organizationId").equals(requested.path("scope").path("organizationId")),"Projection proof scope/time mismatch");
        BindingContract.require(requested.path("projectionRequest").isObject()
            &&requested.path("projectionRequest").equals(proof.path("request")),"Independent revision needs original authenticated query scope");
        BindingContract.require(requested.path("projectionActorId").equals(proof.path("actorId")),"Projection actor differs from authenticated query actor");
        BindingContract.require(capture.at(Json.required(proof,"objectRowPointer")).equals(proof.path("rows").path("object")),"Projection object is not the captured DB row");
        var relations=Json.array();for(JsonNode row:capture.path("rawRows").path(Json.required(proof,"relationDataset")))
            if(row.path("sourceId").equals(proof.path("request").path("id")))relations.add(row);
        BindingContract.require(relations.equals(proof.path("rows").path("relations")),"Projection relations differ from captured DB rows");
        Set<String> sources=new HashSet<>();
        for(JsonNode source:proof.path("recipeSources")) {
            String name=Json.required(source,"path");BindingContract.require(SOURCES.contains(name)&&sources.add(name),"Unexpected projection recipe source");
            Path recipeSource=validator.path(name);
            BindingContract.require(Json.sha256(recipeSource).equals(source.path("sha256").asText()),"Projection recipe source changed");
        }
        BindingContract.require(sources.equals(new HashSet<>(SOURCES)),"Projection recipe source evidence incomplete");
        String digest=recompute(proof);
        BindingContract.require(proof.path("revision").asText().equals(digest)&&observation.path("snapshotRevision").asText().equals(digest)
            &&requested.path("snapshotRef").asText().equals(digest),"Independent projection revision mismatch");
    }
}
