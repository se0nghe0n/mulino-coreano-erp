package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mulino.verification.Json;
import org.mulino.verification.modelbinding.ProjectionRevisionObservation;
import java.util.*;

/** Recomputes the factual simple-object subset from the same independent JDBC capture. */
final class NativeProjectionObserver {
    static ObjectNode prove(JsonNode raw,JsonNode request,String actor,String asOf,String knownAt,NativeIntegrationEvidence store) throws Exception {
        String type=Json.required(request.path("scope"),"objectType");
        String table=switch(type){case "Place"->"mulino_inventory_Places";case "Product"->"mulino_inventory_Products";
            case "Manufacturer"->"mulino_inventory_Manufacturers";default->throw new IllegalStateException("PROJECTION_RECIPE_NOT_IMPLEMENTED");};
        var objects=raw.path(table);JsonNode object=null;String pointer=null;int index=0;
        for(JsonNode row:objects) {if(row.path("ID").equals(request.path("id"))){NativeClientPolicy.require(object==null,"PROJECTION_OBJECT_AMBIGUOUS");object=row;pointer="/rawRows/"+table+"/"+index;}index++;}
        NativeClientPolicy.require(object!=null,"PROJECTION_OBJECT_UNOBSERVED");
        var relations=Json.array();
        // Conservative: any recorded relation requires the visibility/interval recipe; never assume it is invisible.
        for(JsonNode row:raw.path("mulino_inventory_ObjectRelations"))if(row.path("sourceId").equals(request.path("id")))relations.add(row);
        var proof=Json.object().put("recipe",ProjectionRevisionObservation.RECIPE).put("actorId",actor).put("asOf",asOf).put("knownAt",knownAt)
            .put("objectRowPointer",pointer).put("relationDataset","mulino_inventory_ObjectRelations");
        proof.set("request",request.deepCopy());proof.set("rows",Json.object().set("object",object.deepCopy()));
        ((ObjectNode)proof.path("rows")).set("relations",relations);
        proof.put("revision",ProjectionRevisionObservation.recompute(proof));
        var sources=Json.array();for(String source:ProjectionRevisionObservation.SOURCES)sources.add(store.source(source));proof.set("recipeSources",sources);
        return proof;
    }
    private NativeProjectionObserver() {}
}
