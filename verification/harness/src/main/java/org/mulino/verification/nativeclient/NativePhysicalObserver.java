package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.*;
import org.mulino.verification.actual.ActualConfiguration;
import org.mulino.verification.modelbinding.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.nio.file.*;

/** Independent read-only acquisition. Missing semantic bindings remain UNKNOWN, never zero. */
final class NativePhysicalObserver {
    private final ActualConfiguration configuration;
    private final NativeIntegrationEvidence evidence;
    private final Map<String,ObjectNode> boundaries=new HashMap<>();
    NativePhysicalObserver(ActualConfiguration configuration,NativeIntegrationEvidence evidence) {this.configuration=configuration;this.evidence=evidence;}
    synchronized StepResult observe(String action,JsonNode request) throws Exception {
        var scope=request.path("scope");String key=scope.toString();
        var capture=capture(scope,Json.required(request,"asOf"),Json.required(request,"knownAt"));
        Path directory=evidence.root.resolve(capture.path("snapshot").path("artifactRef").asText()).getParent();
        List<JsonNode> wires=NativeCausalEffects.wires(directory);
        JsonNode parameters=request.path("observationScope");
        NativeClientPolicy.require(parameters.isObject()&&parameters.path("scope").equals(scope),"EXPLICIT_OBSERVATION_SCOPE_REQUIRED");
        String authority=null;
        for(JsonNode wire:wires)if(wire.path("scope").equals(scope)&&wire.path("response").path("result").path("structuredContent").path("snapshotRevision").equals(request.path("snapshotRef"))) {
            String found=Json.required(wire.path("serverAuthenticatedCapture"),"authorityAsOf");
            NativeClientPolicy.require(authority==null||authority.equals(found),"PROJECTION_AUTHORITY_TIME_AMBIGUOUS");authority=found;
        }
        NativeClientPolicy.require(authority!=null&&authority.equals(parameters.path("authorityAt").asText()),"OBSERVATION_AUTHORITY_TIME_UNPROVEN");
        ObjectNode before;
        if(action.endsWith("/before")){NativeClientPolicy.require(!boundaries.containsKey(key),"DUPLICATE_TURN_BOUNDARY");before=capture;}
        else {NativeClientPolicy.require(action.endsWith("/after")&&boundaries.containsKey(key),"TURN_BEFORE_BOUNDARY_MISSING");before=boundaries.get(key);}
        capture.set("obligationObservation",NativePhysicalFacts.obligations(capture.path("rawRows"),Json.required(request,"asOf"),Json.required(request,"knownAt"),authority));
        if(!capture.path("obligationObservation").path("complete").asBoolean(false))evidence.save("obligation-history-incomplete",capture);
        NativeClientPolicy.require(capture.path("obligationObservation").path("complete").asBoolean(false),"OBLIGATION_HISTORY_INCOMPLETE");
        capture.set("effectObservation",NativeCausalEffects.observe(before,capture,wires));
        if(!capture.path("effectObservation").path("complete").asBoolean(false))evidence.save("causal-effect-incomplete",capture);
        NativeClientPolicy.require(capture.path("effectObservation").path("complete").asBoolean(false),"CAUSAL_EFFECT_MAPPING_INCOMPLETE");
        NativePhysicalFacts.map(evidence.root,capture,parameters);
        var proof=NativeProjectionObserver.prove(capture,request.path("projectionRequest"),Json.required(request,"projectionActorId"),parameters,evidence);
        NativeClientPolicy.require(proof.path("revision").equals(request.path("snapshotRef")),"INDEPENDENT_PROJECTION_REVISION_MISMATCH");
        capture.set("projectionRevisionProof",proof);capture.put("runtimeStatus","OBSERVED");
        var unresolved=Json.array();for(JsonNode status:capture.path("semanticCoverage"))if(status.path("status").asText().equals("UNKNOWN"))unresolved.add(status.deepCopy());
        capture.set("missingObservations",unresolved);capture.put("mappingComplete",unresolved.isEmpty());
        var snapshot=(ObjectNode)capture.path("snapshot");snapshot.put("readMode","RESULT_REVISION");snapshot.set("revisionQuery",capture.path("sourceQueries").get(0));
        var data=Json.object();data.set("effects",capture.path("effectObservation").path("rows"));data.set("obligations",capture.path("obligationObservation").path("rows"));data.set("units",capture.path("units"));
        var envelope=Json.object().put("snapshotRevision",proof.path("revision").asText()).put("asOf",Json.required(request,"asOf")).put("knownAt",Json.required(request,"knownAt")).put("scopeComplete",true);
        envelope.set("scope",scope);envelope.set("snapshot",snapshot);envelope.set("sourceQuery",capture.path("sourceQueries").get(0));envelope.set("rawRows",capture.path("rawRows"));envelope.set("sourceEvidence",capture.path("sourceEvidence"));envelope.set("physicalCells",capture.path("physicalCells"));envelope.set("data",data);
        var artifact=evidence.save("model-physical-observation",capture);snapshot.put("artifactRef",artifact.path("path").asText());
        PhysicalObservation.validate(new ContractValidator(evidence.root),envelope);
        var provenance=Json.object().put("adapter","native-physical-model-observer").put("adapterVersion","1").put("buildVersion",configuration.buildVersion()).put("source","ACTUAL_POSTGRESQL_INDEPENDENT_RECIPES").put("independent",true).put("scopeComplete",true);
        provenance.set("sourceQuery",envelope.path("sourceQuery"));provenance.set("snapshot",snapshot);provenance.putNull("authenticatedActor");
        if(action.endsWith("/before"))boundaries.put(key,capture.deepCopy());
        return new StepResult(action,StepResult.DriverStatus.EXECUTED,envelope,null,null,provenance,List.of(artifact.path("path").asText()));
    }
    ObjectNode capture(JsonNode scope,String asOf,String knownAt) throws Exception {
        return capture(scope,asOf,knownAt,null,null);
    }
    ObjectNode capture(JsonNode scope,String asOf,String knownAt,JsonNode projectionRequest,String projectionActorId) throws Exception {
        UUID.fromString(Json.required(scope,"organizationId"));
        for(String key:List.of("installationId","caseId","turnId")) Json.required(scope,key);
        Instant.parse(asOf);OffsetDateTime cutoff=OffsetDateTime.parse(knownAt);
        var result=evidence.base("ACTUAL_POSTGRESQL_DIAGNOSTIC",scope).put("mappingVersion","model-binding-v1")
            .put("mappingComplete",false).put("runtimeStatus","CAPTURED").put("asOf",asOf).put("knownAt",knownAt);
        var queries=Json.array();var raw=Json.object();var cells=Json.array();var sources=Json.object();
        var snapshot=Json.object().put("isolation","REPEATABLE_READ").put("readMode","CURRENT_COMMITTED");
        try(Connection c=DriverManager.getConnection(configuration.jdbcUrl(),configuration.username(),configuration.password())) {
            c.setAutoCommit(false);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            try(var statement=c.createStatement();var rows=statement.executeQuery("SELECT pg_current_snapshot()::text")){rows.next();snapshot.put("id",rows.getString(1));}
            result.set("catalogEvidence",catalog(c));
            for(var table:NativePhysicalCatalog.TABLES) {
                String dataset=dataset(table.name());
                // Capture current committed storage, including rows newer than knownAt. Recipes must
                // select immutable history; filtering away a later mutation would invent an empty past.
                String sql=NativePhysicalCatalog.query(table);
                var params=Json.object().put("organizationId",scope.path("organizationId").asText());
                var query=Json.object().put("statementId","native-physical-"+dataset+"-v3").put("sql",sql).put("mappingVersion","model-binding-v1");query.set("parameters",params);queries.add(query);
                var source=Json.object().put("complete",true).put("rowPointer","/rawRows/"+dataset);source.set("sourceQuery",query);sources.set(dataset,source);
                for(String column:table.columns())sources.set(dataset+"."+column,source.deepCopy());
                var rows=Json.array();
                try(var statement=c.prepareStatement(sql)) {
                    statement.setString(1,scope.path("organizationId").asText());
                    try(var records=statement.executeQuery()) {while(records.next()) {
                        var row=Json.object();for(int i=0;i<table.columns().size();i++) {
                            Object operand=records.getMetaData().getColumnTypeName(i+1).equals("timestamp")?records.getObject(i+1,LocalDateTime.class):records.getObject(i+1);
                            row.set(table.columns().get(i),table.columns().get(i).equals("ID")?Json.MAPPER.getNodeFactory().textNode(records.getString(i+1)):value(operand));
                        }
                        row.put("__xmin",records.getString(table.columns().size()+1));row.put("__tableoid",records.getString(table.columns().size()+2));
                        String id=row.has("ID")?row.path("ID").asText():identity(row,table.keys());int rowIndex=rows.size();rows.add(row);
                        if(!table.columns().contains("ID"))continue; // Composite keys remain raw operands, never fabricated IDs.
                        for(String column:table.columns()) {
                            var cell=Json.object().put("dataset",dataset).put("attribute",column).put("table",table.name()).put("column",column).put("rowId",id)
                                .put("rawRowPointer","/rawRows/"+dataset+"/"+rowIndex+"/"+column);
                            cell.set("value",row.path(column));if(row.hasNonNull("unit"))cell.set("unit",row.path("unit"));
                            if(row.hasNonNull("physicalScopeId"))cell.set("physicalScopeId",row.path("physicalScopeId"));cells.add(cell);
                        }
                    }}
                }raw.set(dataset,rows);
            }
            c.commit();
        }
        snapshot.put("capturedAt",Instant.now().toString());result.set("snapshot",snapshot);
        result.set("sourceQueries",queries);result.set("sourceEvidence",sources);result.set("rawRows",raw);result.set("physicalCells",cells);
        result.set("sourceDocuments",NativeDocumentWitnesses.capture(raw));
        PhysicalObservation.validateLineage(result);
        // Candidate facts retain actual IDs and row links; missing installation/turn causality prevents acceptance.
        result.set("obligationObservation",NativePhysicalFacts.obligations(raw,asOf,knownAt,null));
        result.set("effectObservation",NativePhysicalFacts.effects(raw));
        result.set("semanticCoverage",NativePhysicalFacts.coverage(evidence.root,raw));
        if(projectionRequest!=null) result.set("projectionRevisionProof",NativeProjectionObserver.prove(raw,projectionRequest,projectionActorId,asOf,knownAt,evidence));
        result.set("implementationSource",evidence.source("verification/harness/src/main/java/org/mulino/verification/nativeclient/NativePhysicalObserver.java"));
        result.set("missingObservations",Json.array().add("Authoritative installation/turn effect attribution and complete immutable effect journal")
            .add("Independent world projection recipe and authenticated query actor/scope")
            .add("Alias-bound semantic selectors, derived fields and canonical physical interval identity"));
        var artifact=evidence.save("postgresql-observation",result);
        snapshot.put("artifactRef",artifact.path("path").asText());
        sources.forEach(source->((ObjectNode)source).put("artifactRef",artifact.path("path").asText()));
        // artifact deliberately has no self hash/ref; returned envelope points to immutable captured bytes.
        return result;
    }
    private ObjectNode catalog(Connection c) throws Exception {
        var expected=new TreeMap<String,NativePhysicalCatalog.Table>();
        for(var table:NativePhysicalCatalog.TABLES)expected.put(table.name().toLowerCase(Locale.ROOT),table);
        var seen=new TreeMap<String,Set<String>>();
        String sql="SELECT table_name,column_name,data_type,udt_name,numeric_precision,numeric_scale FROM information_schema.columns WHERE table_schema=current_schema() AND table_name LIKE 'mulino_%' ORDER BY table_name,ordinal_position";
        var captured=Json.array();
        try(var statement=c.createStatement();var rows=statement.executeQuery(sql)) {while(rows.next()) {
            String table=rows.getString(1),column=rows.getString(2);
            if(NativePhysicalCatalog.EXCLUDED.contains(table))continue;
            NativeClientPolicy.require(expected.containsKey(table),"UNKNOWN_PHYSICAL_TABLE:"+table);
            var definition=expected.get(table);int position=-1;
            for(int j=0;j<definition.columns().size();j++)if(definition.columns().get(j).equalsIgnoreCase(column))position=j;
            NativeClientPolicy.require(position>=0,"PHYSICAL_COLUMN_DRIFT:"+table);
            String type=definition.types().get(position),actual=rows.getString(4);
            boolean compatible=switch(type){case "UUID","String"->actual.equals("varchar");case "LargeString"->actual.equals("text");case "Boolean"->actual.equals("bool");case "Integer"->actual.equals("int4");case "Integer64"->actual.equals("int8");case "Decimal"->actual.equals("numeric");case "Timestamp"->Set.of("timestamp","timestamptz").contains(actual);case "Date"->actual.equals("date");default->false;};
            NativeClientPolicy.require(compatible&&(!type.equals("Decimal")||rows.getInt(5)==38&&rows.getInt(6)==12),"PHYSICAL_COLUMN_TYPE_DRIFT:"+table+"."+column);
            seen.computeIfAbsent(table,k->new TreeSet<>()).add(column);
            captured.add(Json.object().put("table",table).put("column",column).put("type",rows.getString(3)).put("udt",rows.getString(4))
                .set("precision",value(rows.getObject(5))));
            ((ObjectNode)captured.get(captured.size()-1)).set("scale",value(rows.getObject(6)));
        }}
        NativeClientPolicy.require(seen.keySet().equals(expected.keySet()),"PHYSICAL_TABLE_INVENTORY_INCOMPLETE");
        for(var entry:expected.entrySet()) {
            var columns=new TreeSet<String>();entry.getValue().columns().forEach(v->columns.add(v.toLowerCase(Locale.ROOT)));
            NativeClientPolicy.require(columns.equals(seen.get(entry.getKey())),"PHYSICAL_COLUMN_DRIFT:"+entry.getKey());
        }
        var sources=Json.array();var paths=new TreeSet<String>();paths.add("verification/harness/src/main/java/org/mulino/verification/nativeclient/NativePhysicalCatalog.java");
        NativePhysicalCatalog.TABLES.forEach(t->paths.add(t.source()));
        try(var files=Files.list(evidence.root.resolve("database/migrations"))){files.filter(p->p.toString().endsWith(".sql")).forEach(p->paths.add(evidence.root.relativize(p).toString()));}
        for(String path:paths)sources.add(evidence.source(path));
        var result=Json.object().put("complete",true).put("sql",sql);result.set("columns",captured);result.set("sources",sources);return result;
    }
    private static String identity(JsonNode row,List<String> keys) {var values=new ArrayList<String>();for(String key:keys)values.add(row.path(key).asText());return String.join("|",values);}
    static String dataset(String table) {
        return switch(table){case "mulino_inventory_QuantitySegments"->"segments";case "mulino_inventory_SegmentAllocations"->"allocations";
            case "mulino_inventory_QuantityMovements"->"movements";case "mulino_identity_Grants"->"grants";case "mulino_runtime_Outbox"->"outbox";default->table;};
    }
    private static JsonNode value(Object value) {
        if(value==null)return Json.MAPPER.nullNode();
        if(value instanceof BigDecimal decimal)return Json.MAPPER.getNodeFactory().textNode(decimal.stripTrailingZeros().toPlainString());
        if(value instanceof Timestamp time)return Json.MAPPER.getNodeFactory().textNode(time.toInstant().toString());
        if(value instanceof OffsetDateTime time)return Json.MAPPER.getNodeFactory().textNode(time.toInstant().toString());
        if(value instanceof LocalDateTime time)return Json.MAPPER.getNodeFactory().textNode(time.toInstant(ZoneOffset.UTC).toString());
        if(value instanceof Number)return Json.parse(value.toString()); // Stable JSON integer type across retained artifact round trips.
        if(value instanceof Boolean)return Json.MAPPER.valueToTree(value);
        return Json.MAPPER.getNodeFactory().textNode(value.toString());
    }
}
