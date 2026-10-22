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

/** Independent read-only acquisition. Missing semantic bindings remain UNKNOWN, never zero. */
final class NativePhysicalObserver {
    private final ActualConfiguration configuration;
    private final NativeIntegrationEvidence evidence;
    NativePhysicalObserver(ActualConfiguration configuration,NativeIntegrationEvidence evidence) {this.configuration=configuration;this.evidence=evidence;}
    ObjectNode capture(JsonNode scope,String asOf,String knownAt) throws Exception {
        return capture(scope,asOf,knownAt,null,null);
    }
    ObjectNode capture(JsonNode scope,String asOf,String knownAt,JsonNode projectionRequest,String projectionActorId) throws Exception {
        UUID.fromString(Json.required(scope,"organizationId"));
        for(String key:List.of("installationId","caseId","turnId")) Json.required(scope,key);
        Instant.parse(asOf);OffsetDateTime cutoff=OffsetDateTime.parse(knownAt);
        var result=evidence.base("ACTUAL_POSTGRESQL_DIAGNOSTIC",scope).put("mappingVersion","model-binding-v1")
            .put("mappingComplete",false).put("runtimeStatus","NOT_IMPLEMENTED").put("asOf",asOf).put("knownAt",knownAt);
        var queries=Json.array();var raw=Json.object();var cells=Json.array();var sources=Json.object();
        var snapshot=Json.object().put("isolation","REPEATABLE_READ").put("readMode","CURRENT_COMMITTED");
        try(Connection c=DriverManager.getConnection(configuration.jdbcUrl(),configuration.username(),configuration.password())) {
            c.setAutoCommit(false);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            try(var statement=c.createStatement();var rows=statement.executeQuery("SELECT pg_current_snapshot()::text")){rows.next();snapshot.put("id",rows.getString(1));}
            for(var table:NativePhysicalCatalog.TABLES) {
                String dataset=dataset(table.name());
                String timeColumn=table.columns().contains("recordedAt")?"recordedAt":table.columns().contains("createdAt")?"createdAt":null;
                String sql="SELECT "+String.join(",",table.columns())+" FROM "+table.name()+" WHERE organizationId=?"+(timeColumn==null?"":" AND "+timeColumn+"<=?")+" ORDER BY ID";
                var params=Json.object().put("organizationId",scope.path("organizationId").asText());if(timeColumn!=null)params.put("knownAt",knownAt);
                var query=Json.object().put("statementId","native-physical-"+dataset+"-v2").put("sql",sql).put("mappingVersion","model-binding-v1");query.set("parameters",params);queries.add(query);
                var source=Json.object().put("complete",true).put("rowPointer","/rawRows/"+dataset);source.set("sourceQuery",query);sources.set(dataset,source);
                var rows=Json.array();
                try(var statement=c.prepareStatement(sql)) {
                    statement.setString(1,scope.path("organizationId").asText());if(timeColumn!=null)statement.setObject(2,cutoff);
                    try(var records=statement.executeQuery()) {while(records.next()) {
                        var row=Json.object();for(int i=0;i<table.columns().size();i++)row.set(table.columns().get(i),value(records.getObject(i+1)));
                        String id=Json.required(row,"ID");int rowIndex=rows.size();rows.add(row);
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
        PhysicalObservation.validateLineage(result);
        // Candidate facts retain actual IDs and row links; missing installation/turn causality prevents acceptance.
        result.set("obligationObservation",NativePhysicalFacts.obligations(raw,asOf));
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
    static String dataset(String table) {
        return switch(table){case "mulino_inventory_QuantitySegments"->"segments";case "mulino_inventory_SegmentAllocations"->"allocations";
            case "mulino_inventory_QuantityMovements"->"movements";case "mulino_identity_Grants"->"grants";case "mulino_runtime_Outbox"->"outbox";default->table;};
    }
    private static JsonNode value(Object value) {
        if(value==null)return Json.MAPPER.nullNode();
        if(value instanceof BigDecimal decimal)return Json.MAPPER.getNodeFactory().textNode(decimal.stripTrailingZeros().toPlainString());
        if(value instanceof Timestamp time)return Json.MAPPER.getNodeFactory().textNode(time.toInstant().toString());
        if(value instanceof OffsetDateTime time)return Json.MAPPER.getNodeFactory().textNode(time.toInstant().toString());
        if(value instanceof Number||value instanceof Boolean)return Json.MAPPER.valueToTree(value);
        return Json.MAPPER.getNodeFactory().textNode(value.toString());
    }
}
