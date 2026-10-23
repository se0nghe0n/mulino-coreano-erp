package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import org.mulino.verification.modelbinding.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** SQL facts, current responsibility, and explicit semantic recipes. Unknowns are retained, never seeded. */
final class NativePhysicalFacts {
    private static final String DUTIES="mulino_work_read_ObligationReferences";
    static ObjectNode obligations(JsonNode raw,String asOf,String knownAt,String authorityAt) {
        var out=Json.object().put("status","CURRENT_SQL_RESPONSIBILITY").put("complete",true);var duties=Json.array();out.set("rows",duties);
        var humans=new HashSet<String>();for(JsonNode actor:raw.path("mulino_identity_Actors"))if(actor.path("kind").asText().equals("HUMAN"))humans.add(Json.required(actor,"ID"));
        JsonNode rows=raw.path(DUTIES);NativeClientPolicy.require(rows.isArray(),"OBLIGATION_TABLE_UNOBSERVED");var current=new HashSet<String>();int i=0;
        boolean currentProven=authorityAt!=null&&Instant.parse(authorityAt).equals(Instant.parse(knownAt))&&Instant.parse(asOf).equals(Instant.parse(knownAt));
        if(!currentProven)out.put("complete",false).put("status","MUTABLE_OBLIGATION_HISTORY_UNAVAILABLE");
        for(JsonNode row:rows) {
            int index=i++;if(!currentProven)continue;if(!row.path("valid").asBoolean(false)||Instant.parse(Json.required(row,"effectiveAt")).isAfter(Instant.parse(asOf)))continue;
            var duty=responsibility(row,DUTIES,index,"ownerId");
            for(String field:List.of("workId","rootId","scopeId","supervisorId","quantity","unit","revision","valid"))if(row.hasNonNull(field))duty.set(field,row.path(field));
            duty.set("scope",Json.parse(Json.required(row,"scopeJson")));duty.put("scopeRef",row.hasNonNull("scopeId")?Json.required(row,"scopeId"):Json.required(row,"workId"));
            if(row.path("status").asText().equals("OPEN")){
                String rootId=Json.required(row,"rootId"),scopeId=Json.required(row,"scopeId");
                JsonNode root=unique(raw.path("mulino_responsibility_Roots"),rootId),physicalScope=unique(raw.path("mulino_responsibility_Scopes"),scopeId);
                NativeClientPolicy.require(physicalScope.path("rootId").asText().equals(rootId)&&physicalScope.path("scopeJson").equals(row.path("scopeJson")),"DUTY_ROOT_SCOPE_BINDING_MISMATCH");
                for(JsonNode source:List.of(root,physicalScope))for(String field:List.of("createdAt","recordedAt"))NativeClientPolicy.require(!Instant.parse(Json.required(source,field)).isAfter(Instant.parse(knownAt)),"DUTY_SCOPE_NOT_KNOWN");
                Instant.parse(Json.required(row,"nextCheckAt"));duty.set("rootSource",root);duty.set("scopeSource",physicalScope);
                NativeClientPolicy.require(humans.contains(duty.path("ownerRef").asText())&&humans.contains(Json.required(row,"supervisorId")),"OPEN_DUTY_REQUIRES_ACTUAL_HUMAN_RESPONSIBILITY");
                if(row.hasNonNull("scopeId"))NativeClientPolicy.require(current.add(Json.required(row,"scopeId")),"DUPLICATE_CURRENT_DUTY_SCOPE");
            }duties.add(duty);
        }
        // Intake responsibility is a real duty before any Work assignment; use trigger-owned history.
        var heads=new TreeMap<String,JsonNode>();
        for(JsonNode history:raw.path("mulino_runtime_RecoveryObservationHistory"))if(history.path("sourceTable").asText().equals("mulino_runtime_intakerecoveries")&&!Instant.parse(Json.required(history,"recordedAt")).isAfter(Instant.parse(knownAt))){String key=Json.required(history,"sourceId");JsonNode old=heads.get(key);if(old==null||Long.parseLong(history.path("ID").asText())>Long.parseLong(old.path("ID").asText()))heads.put(key,history);}
        for(JsonNode intake:raw.path("mulino_runtime_IntakeRecoveries")) {
            JsonNode head=heads.get(intake.path("ID").asText());if(head==null||!head.path("historyComplete").asBoolean(false)||!head.path("scopeComplete").asBoolean(false)){out.put("complete",false);continue;}
            if(head.path("operation").asText().equals("DELETE"))continue;JsonNode state=Json.parse(Json.required(head,"snapshotJson"));
            if(Set.of("LINKED","NO_RESPONSE").contains(state.path("state").asText()))continue;
            NativeClientPolicy.require(Set.of("PENDING","HELD_POLICY","HELD_LINK","HELD_MANUAL").contains(state.path("state").asText()),"UNKNOWN_INTAKE_RESPONSIBILITY_STATE");
            var duty=Json.object().put("id","intake:"+Json.required(intake,"ID")).put("kind","INTAKE_RECONCILIATION").put("status","OPEN").put("ownerRef",Json.required(state,"intakeownerid")).put("supervisorId",Json.required(state,"supervisorid")).put("nextAction",Json.required(state,"nextaction")).put("nextCheckAt",Json.required(state,"nextcheckat")).put("scopeRef",Json.required(intake,"eventId")).put("table","mulino_runtime_RecoveryObservationHistory").put("rowId",Json.required(head,"ID")).put("historyComplete",true);
            NativeClientPolicy.require(humans.contains(duty.path("ownerRef").asText())&&humans.contains(duty.path("supervisorId").asText()),"INTAKE_REQUIRES_ACTUAL_HUMAN_RESPONSIBILITY");duty.set("historySource",head);duties.add(duty);
        }
        return out;
    }
    private static JsonNode unique(JsonNode rows,String id){NativeClientPolicy.require(rows.isArray(),"DUTY_SCOPE_SOURCE_UNOBSERVED");JsonNode result=null;for(JsonNode row:rows)if(row.path("ID").asText().equals(id)){NativeClientPolicy.require(result==null,"DUTY_SCOPE_DUPLICATE");result=row;}NativeClientPolicy.require(result!=null,"DUTY_SCOPE_SOURCE_MISSING");return result;}
    private static ObjectNode responsibility(JsonNode row,String table,int index,String owner){
        var duty=Json.object().put("id",Json.required(row,"ID")).put("table",table).put("rowId",Json.required(row,"ID")).put("rawRowPointer","/rawRows/"+table+"/"+index);
        for(String field:List.of("kind","status","nextAction","nextCheckAt"))duty.put(field,Json.required(row,field));duty.put("ownerRef",Json.required(row,owner));return duty;
    }
    static ObjectNode effects(JsonNode raw) {return Json.object().put("complete",false).put("status","TURN_BOUNDARY_REQUIRED").set("rows",Json.array());}
    static ArrayNode coverage(Path root,JsonNode raw) throws Exception {return Json.array();}
    static ObjectNode map(Path root,ObjectNode capture,JsonNode parameters) throws Exception {
        var cells=(ArrayNode)capture.path("physicalCells");var sources=(ObjectNode)capture.path("sourceEvidence");var coverage=Json.array();var units=Json.object();var emitted=new HashSet<String>();
        try{var r=new PhysicalRecipeRows(capture,parameters);String unit=r.unit();for(String kind:List.of("INVENTORY","PHYSICAL","CONFIRMED_PHYSICAL"))units.put(kind,unit);}catch(IllegalArgumentException unknown){/* Explicit source coverage below retains the missing item/unit. */}
        for(JsonNode mapping:Json.read(root.resolve("verification/model-binding/semantic-paths.json")).path("paths")) {
            String type=mapping.path("evidenceClass").asText(),path=mapping.path("semanticPath").asText();var status=Json.object().put("semanticPath",path);coverage.add(status);
            if(type.equals("AUTHENTICATED_API")){status.put("status","WIRE_ONLY").put("reason","Value must be extracted from actual authenticated HTTP response");continue;}
            if(type.equals("SCOPED_EFFECT_DELTA")){status.put("status",capture.path("effectObservation").path("complete").asBoolean(false)?"OBSERVED":"UNKNOWN").put("reason","Closed catalog and captured committed-command causality");continue;}
            try {
                var computed=PhysicalFactRecipes.compute(path,capture,parameters);String dataset=Json.required(mapping,"dataset"),attribute=Json.required(mapping,"attribute");
                if(emitted.add(dataset+"|"+attribute)){
                    if(computed.facts().isEmpty()){cells.add(PhysicalFactRecipes.cell(path,dataset,attribute,computed,-1,root));try{units.put(dataset,computed.rows().unit());}catch(IllegalArgumentException missingUnit){/* A nonquantity empty scope has no invented unit. */}}
                    for(int index=0;index<computed.facts().size();index++){var fact=computed.facts().get(index);cells.add(PhysicalFactRecipes.cell(path,dataset,attribute,computed,index,root));if(fact.unit()!=null)units.put(dataset,fact.unit());}
                }
                if(!computed.rows().used.isEmpty()){String first=PhysicalRecipeRows.dataset(computed.rows().used.iterator().next());sources.set(dataset+"."+attribute,sources.path(first).deepCopy());}
                status.put("status","OBSERVED").put("reason","Closed versioned independent raw-column recipe");
            }catch(IllegalArgumentException missing){status.put("status","UNKNOWN").put("reason",missing.getMessage());}
        }
        capture.set("semanticCoverage",coverage);capture.set("units",units);return capture;
    }
    private NativePhysicalFacts() {}
}
