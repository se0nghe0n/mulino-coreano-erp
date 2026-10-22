package org.mulino.verification.modelbinding;

import com.fasterxml.jackson.databind.JsonNode;
import org.mulino.verification.*;
import java.io.IOException;
import java.util.*;

/** Cell values are a view of retained DB rows, never a replacement for those rows. */
public final class PhysicalObservation {
    private PhysicalObservation() {}
    public static void validate(ContractValidator validator, JsonNode observation) throws IOException {
        validator.schema("verification/model-binding/physical-columns.schema.json", observation.path("physicalCells"));
        validateLineage(observation);
    }
    /** A zero reduction is valid only for a completely observed, explicitly mapped empty scope. */
    public static void requireColumnCoverage(JsonNode envelope,String dataset,String attribute) {
        JsonNode evidence=envelope.path("sourceEvidence").path(dataset+"."+attribute);
        BindingContract.require(evidence.path("complete").asBoolean(false),"Independent column mapping incomplete: "+dataset+"."+attribute);
        String pointer=Json.required(evidence,"rowPointer");
        BindingContract.require(pointer.matches("^/rawRows/[^/]+$")&&envelope.at(pointer).isArray(),"Independent column scope lacks raw rows");
        int count=0;
        for(JsonNode cell:envelope.path("physicalCells"))if(cell.path("dataset").asText().equals(dataset)&&cell.path("attribute").asText().equals(attribute)) {
            BindingContract.require(cell.path("rawRowPointer").asText().startsWith(pointer+"/"),"Physical cell escapes its complete scope");count++;
        }
        BindingContract.require(count==envelope.at(pointer).size(),"Independent column coverage is incomplete");
    }
    public static void validateLineage(JsonNode observation) {
        BindingContract.require(observation.path("rawRows").isObject(), "Raw DB rows must remain an object");
        BindingContract.require(observation.path("physicalCells").isArray(), "Physical cells unobserved");
        Set<String> identities = new HashSet<>();
        for (JsonNode cell : observation.path("physicalCells")) {
            String pointer = Json.required(cell, "rawRowPointer");
            BindingContract.require(pointer.matches("^/rawRows/[^/]+/[0-9]+/[^/]+$"), "Invalid physical cell lineage");
            JsonNode value = observation.at(pointer);
            BindingContract.require(!value.isMissingNode() && value.equals(cell.path("value")), "Physical cell differs from raw DB column");
            int split = pointer.lastIndexOf('/');
            JsonNode row = observation.at(pointer.substring(0, split));
            JsonNode id = row.has("ID") ? row.path("ID") : row.path("id");
            BindingContract.require(id.isTextual() && id.equals(cell.path("rowId")), "Physical cell row identity differs");
            String column = pointer.substring(split + 1).replace("~1", "/").replace("~0", "~");
            BindingContract.require(column.equals(cell.path("column").asText()), "Physical cell column differs");
            String dataset = pointer.split("/")[2];
            JsonNode source = observation.path("sourceEvidence").path(dataset);
            BindingContract.require(source.path("rowPointer").asText().equals("/rawRows/" + dataset)
                && source.path("sourceQuery").path("sql").asText().toLowerCase(Locale.ROOT)
                    .contains(" from " + cell.path("table").asText().toLowerCase(Locale.ROOT) + " "), "Physical cell table has no query lineage");
            // Different semantic attributes may reference one physical column; duplicate semantic cells may not.
            String key = cell.path("dataset").asText()+"|"+cell.path("attribute").asText()+"|"+cell.path("table").asText()+"|"+id.asText();
            BindingContract.require(identities.add(key), "Duplicate physical semantic cell");
            if(cell.has("unit")) BindingContract.require(row.path("unit").equals(cell.path("unit"))
                || row.path("currency").equals(cell.path("unit")), "Physical unit has no row lineage");
            if(cell.has("physicalScopeId")) BindingContract.require(row.path("physicalScopeId").equals(cell.path("physicalScopeId")),
                "Canonical physical scope has no row lineage");
        }
    }
}
