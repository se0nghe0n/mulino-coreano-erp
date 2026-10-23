package org.mulino.verification.modelbinding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import org.mulino.verification.nativeclient.NativePhysicalCatalog;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Read-only operands for closed recipes. Selection inputs select identities; they never supply facts. */
public final class PhysicalRecipeRows {
    public final JsonNode capture, parameters;
    public final Instant asOf,knownAt;
    public final String organization;
    public final Set<String> used=new TreeSet<>();
    public PhysicalRecipeRows(JsonNode capture,JsonNode parameters) {
        this.capture=capture;this.parameters=parameters;
        require(parameters.isObject()&&parameters.path("scope").equals(capture.path("scope")),"RECIPE_SCOPE_MISMATCH");
        parameters.fieldNames().forEachRemaining(k->require(Set.of("scope","businessScope","bindings","actorId","authorityAt","capabilityId").contains(k),"UNKNOWN_RECIPE_PARAMETER"));
        require(parameters.path("businessScope").isObject()&&parameters.path("bindings").isObject(),"RECIPE_SELECTORS_MISSING");
        parameters.path("businessScope").fields().forEachRemaining(e->{require(Set.of("itemId","lotId","placeId","workId","customerId","orderId","salesLineId").contains(e.getKey()),"UNKNOWN_BUSINESS_SCOPE");UUID.fromString(e.getValue().asText());});
        organization=Json.required(capture.path("scope"),"organizationId");
        asOf=Instant.parse(Json.required(capture,"asOf"));knownAt=Instant.parse(Json.required(capture,"knownAt"));
    }
    public List<JsonNode> rows(String table) {
        String dataset=dataset(table);JsonNode rows=capture.path("rawRows").path(dataset),source=capture.path("sourceEvidence").path(dataset);
        require(rows.isArray()&&source.path("complete").asBoolean(false)&&source.path("rowPointer").asText().equals("/rawRows/"+dataset),"RECIPE_DATASET_INCOMPLETE:"+table);
        require(source.path("sourceQuery").path("sql").asText().toLowerCase(Locale.ROOT).contains(" from "+table.toLowerCase(Locale.ROOT)+" "),"RECIPE_SOURCE_TABLE_MISMATCH");
        require(source.path("sourceQuery").path("parameters").path("organizationId").asText().equals(organization),"RECIPE_SOURCE_SCOPE_MISMATCH");
        var definition=NativePhysicalCatalog.TABLES.stream().filter(t->t.name().equals(table)).findFirst().orElseThrow(()->new IllegalArgumentException("UNKNOWN_RECIPE_TABLE"));
        String selected="select "+String.join(",",definition.columns()).toLowerCase(Locale.ROOT)+",";
        require(source.path("sourceQuery").path("sql").asText().equals(NativePhysicalCatalog.query(definition))&&source.path("sourceQuery").path("parameters").size()==1&&source.path("sourceQuery").path("statementId").asText().equals("native-physical-"+dataset+"-v3")&&source.path("sourceQuery").path("mappingVersion").asText().equals("model-binding-v1"),"RECIPE_QUERY_OR_VISIBILITY_UNPROVEN");
        used.add(table);List<JsonNode> result=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(JsonNode row:rows){for(String column:definition.columns())require(row.has(column),"RECIPE_COLUMN_MISSING:"+column);require(row.path("organizationId").asText().equals(organization)||table.equals("mulino_identity_Organizations")&&row.path("ID").asText().equals(organization),"RECIPE_FOREIGN_ORGANIZATION");if(row.has("ID"))require(ids.add(row.path("ID").asText()),"DUPLICATE_PHYSICAL_ROW");result.add(row);}
        return result;
    }
    public List<JsonNode> known(String table) {return rows(table).stream().filter(r->at(r,"recordedAt",knownAt)&&at(r,"createdAt",knownAt)).toList();}
    public List<JsonNode> temporal(String table) {return known(table).stream().filter(r->at(r,"effectiveAt",asOf)&&at(r,"occurredAt",asOf)).toList();}
    public JsonNode one(String table,String id) {return single(rows(table).stream().filter(r->r.path("ID").asText().equals(id)).toList());}
    public String scope(String key){return parameters.path("businessScope").path(key).asText(null);}
    public JsonNode bound(String label,String table) {
        JsonNode binding=parameters.path("bindings").path(label);
        require(binding.path("table").asText().equals(table),"RECIPE_ENTITY_BINDING_MISSING:"+label);
        binding.fieldNames().forEachRemaining(k->require(Set.of("table","id").contains(k),"UNKNOWN_ENTITY_BINDING_PARAMETER"));
        JsonNode row=one(table,Json.required(binding,"id"));require(at(row,"recordedAt",knownAt)&&at(row,"createdAt",knownAt)&&at(row,"effectiveAt",asOf)&&at(row,"occurredAt",asOf),"BOUND_ENTITY_NOT_VISIBLE_AT_SNAPSHOT");require(matches(row),"BOUND_ENTITY_OUTSIDE_BUSINESS_SCOPE");return row;
    }
    public JsonNode selected(String table,String...labels) {
        var selected=new LinkedHashMap<String,JsonNode>();for(String label:labels)if(parameters.path("bindings").has(label)){var row=bound(label,table);selected.put(row.path("ID").asText(),row);}
        if(!selected.isEmpty())return single(new ArrayList<>(selected.values()));
        return single(temporal(table).stream().filter(this::matches).toList());
    }
    public boolean matches(JsonNode row){for(String k:List.of("itemId","lotId","placeId","workId","customerId"))if(scope(k)!=null&&row.hasNonNull(k)&&!scope(k).equals(row.path(k).asText()))return false;return true;}
    public JsonNode field(JsonNode row,String name){require(row.has(name),"RECIPE_COLUMN_MISSING:"+name);require(!row.path(name).isNull(),"RECIPE_OPERAND_NULL:"+name);return row.path(name);}
    public String text(JsonNode row,String name){return field(row,name).asText();}
    public BigDecimal decimal(JsonNode row,String name){JsonNode n=field(row,name);require(n.isTextual()&&n.asText().matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?"),"RECIPE_DECIMAL_INVALID");return new BigDecimal(n.asText());}
    public boolean active(JsonNode row){require(row.has("retiredAt")&&row.has("retirementRecordedAt")&&row.has("validFrom"),"PHYSICAL_TEMPORAL_COLUMNS_MISSING");return at(row,"validFrom",asOf)&&at(row,"recordedAt",knownAt)&&(row.path("retiredAt").isNull()||!at(row,"retiredAt",asOf)||!at(row,"retirementRecordedAt",knownAt));}
    public List<JsonNode> segments(){return known("mulino_inventory_QuantitySegments").stream().filter(this::matches).filter(this::active).toList();}
    public String unit(){String item=scope("itemId");require(item!=null,"ITEM_SCOPE_REQUIRED");var row=one("mulino_inventory_TradeItems",item);require(at(row,"recordedAt",knownAt)&&at(row,"createdAt",knownAt),"ITEM_UNIT_NOT_KNOWN_AT_SNAPSHOT");return text(row,"baseUnit");}
    public BigDecimal sum(List<JsonNode> rows,String field,String unit){BigDecimal sum=BigDecimal.ZERO;for(JsonNode row:rows){require(text(row,"unit").equals(unit),"RECIPE_MIXED_UNITS");sum=sum.add(decimal(row,field));}return sum;}
    public ArrayNode pointers(){var result=Json.array();for(String table:used){String d=dataset(table);int i=0;for(JsonNode row:capture.path("rawRows").path(d)){var columns=new TreeSet<String>();row.fieldNames().forEachRemaining(columns::add);for(String column:columns)result.add("/rawRows/"+d+"/"+i+"/"+column);i++;}}return result;}
    public ArrayNode datasets(){var a=Json.array();used.forEach(t->a.add(dataset(t)));return a;}
    public List<JsonNode> works(){
        var transitions=known("mulino_work_WorkTransitions");var result=new ArrayList<JsonNode>();
        for(JsonNode w:rows("mulino_work_read_Works")){
            if(w.path("lifecycleMode").asText().equals("COMMAND")){
                var options=transitions.stream().filter(t->t.path("workId").equals(w.path("ID"))).sorted(Comparator.comparingInt((JsonNode t)->t.path("revision").asInt()).reversed()).toList();
                if(at(w,"createdAt",knownAt))require(options.stream().anyMatch(t->Set.of("createDraft","createWork").contains(t.path("operation").asText())),"WORK_HISTORY_GENESIS_UNOBSERVED");
                for(JsonNode t:options){JsonNode snapshot=Json.parse(text(t,"snapshotJson"));require(snapshot.path("organizationId").asText().equals(organization)&&snapshot.path("ID").equals(w.path("ID"))&&snapshot.path("revision").equals(t.path("revision")),"WORK_HISTORY_IDENTITY_MISMATCH");if(at(snapshot,"effectiveAt",asOf)){result.add(snapshot);break;}}
            }else if(at(w,"recordedAt",knownAt)&&at(w,"effectiveAt",asOf))result.add(w);
        }
        result.sort(Comparator.comparing(r->r.path("ID").asText()));return result;
    }
    public JsonNode work(){String work=scope("workId");if(work==null&&parameters.path("bindings").has("O1"))work=Json.required(parameters.path("bindings").path("O1"),"id");final String id=work;return single(works().stream().filter(r->id==null?matches(r):id.equals(r.path("ID").asText())).toList());}
    public static JsonNode single(List<JsonNode> rows){require(rows.size()==1,"RECIPE_CARDINALITY:"+rows.size());return rows.getFirst();}
    public static boolean at(JsonNode row,String field,Instant cutoff){return !row.hasNonNull(field)||!Instant.parse(row.path(field).asText()).isAfter(cutoff);}
    public static String dataset(String table){return switch(table){case "mulino_inventory_QuantitySegments"->"segments";case "mulino_inventory_SegmentAllocations"->"allocations";case "mulino_inventory_QuantityMovements"->"movements";case "mulino_identity_Grants"->"grants";case "mulino_runtime_Outbox"->"outbox";default->table;};}
    public static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
}
