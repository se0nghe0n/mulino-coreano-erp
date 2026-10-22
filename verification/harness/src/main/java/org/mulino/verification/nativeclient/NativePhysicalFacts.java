package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** Projection of directly persisted facts only. Incomplete semantic coverage never becomes []/zero. */
final class NativePhysicalFacts {
    private static final String DUTIES="mulino_work_read_ObligationReferences";
    static ObjectNode obligations(JsonNode raw,String asOf) {
        var out=Json.object().put("status","OBSERVED_TABLE_ONLY").put("complete",false);
        var duties=Json.array();out.set("rows",duties);
        JsonNode rows=raw.path(DUTIES);NativeClientPolicy.require(rows.isArray(),"OBLIGATION_TABLE_UNOBSERVED");
        int i=0;
        for(JsonNode row:rows) {
            int index=i++;
            if(!row.path("valid").asBoolean(true)||Instant.parse(Json.required(row,"effectiveAt")).isAfter(Instant.parse(asOf)))continue;
            var duty=Json.object();duty.put("id",Json.required(row,"ID")).put("table",DUTIES).put("rowId",Json.required(row,"ID"))
                .put("rawRowPointer","/rawRows/"+DUTIES+"/"+index);
            for(String field:List.of("kind","status","nextAction","nextCheckAt"))duty.put(field,Json.required(row,field));
            duty.put("ownerRef",Json.required(row,"ownerId"));
            // Work-scoped duty rows without a split responsibility scope remain explicitly Work scoped.
            duty.put("scopeRef",row.hasNonNull("scopeId")?Json.required(row,"scopeId"):Json.required(row,"workId"));
            duty.set("scope",Json.parse(Json.required(row,"scopeJson")));
            for(String field:List.of("workId","rootId","scopeId","supervisorId","quantity","unit","revision","valid"))if(row.hasNonNull(field))duty.set(field,row.path(field));
            duties.add(duty);
        }
        out.put("boundary","Current valid assignment rows only; intake, handover and outbox responsibilities remain separate raw tables; historical mutation cannot be reconstructed from a current row.");
        return out;
    }
    static ObjectNode effects(JsonNode raw) {
        var result=Json.object().put("complete",false).put("status","UNATTRIBUTED_PHYSICAL_FACTS");
        var facts=Json.array();result.set("rows",facts);
        for(var table:NativePhysicalCatalog.TABLES) {
            String dataset=NativePhysicalObserver.dataset(table.name());int index=0;
            for(JsonNode row:raw.path(dataset)) {
                var fact=Json.object().put("id",Json.required(row,"ID")).put("table",table.name())
                    .put("identity",table.name()+":"+Json.required(row,"ID"))
                    .put("rawRowPointer","/rawRows/"+dataset+"/"+index++);
                for(String field:List.of("commandId","operation","kind","state","status","actorId","recordedAt","createdAt","quantity","unit","canonicalOccurrenceId","physicalScopeId"))
                    if(row.hasNonNull(field))fact.set(field,row.path(field));
                // No operation/class derived from handler names, mutable latest state or requested intent.
                facts.add(fact);
            }
        }
        result.put("boundary","Stable row identities are observed; completeness, immutable semantic event classes and installation/turn causality require a transactional server effect journal. Request scope is not attribution proof.");
        return result;
    }
    static ArrayNode coverage(Path root,JsonNode raw) throws Exception {
        var result=Json.array();
        for(JsonNode mapping:Json.read(root.resolve("verification/model-binding/semantic-paths.json")).path("paths")) {
            String type=mapping.path("evidenceClass").asText();if(type.equals("AUTHENTICATED_API"))continue;
            var status=Json.object().put("semanticPath",mapping.path("semanticPath").asText()).put("status","UNKNOWN");
            status.put("reason",type.equals("SCOPED_EFFECT_DELTA")?"Transactional effect-class and turn attribution journal absent"
                :"Physical tables captured; alias-bound selector and/or derived semantic field not yet evidenced");
            result.add(status);
        }
        return result;
    }
    private NativePhysicalFacts() {}
}
