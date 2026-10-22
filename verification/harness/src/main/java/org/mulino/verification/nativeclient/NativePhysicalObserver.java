package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import org.mulino.verification.actual.ActualConfiguration;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Independent, read-only JDBC diagnostic capture, with physical column lineage.
 * This deliberately does not manufacture the missing model-binding effect taxonomy or projection revision. */
final class NativePhysicalObserver {
    private final ActualConfiguration configuration;
    private final NativeIntegrationEvidence evidence;
    NativePhysicalObserver(ActualConfiguration configuration,NativeIntegrationEvidence evidence) {this.configuration=configuration;this.evidence=evidence;}
    private record Table(String dataset,String name,List<String> columns,boolean temporal) {}
    private static final List<Table> TABLES=List.of(
        new Table("segments","mulino_inventory_QuantitySegments",List.of("ID","itemId","lotId","quantity","unit","placeId","ownerId","custodianId","controlScope","revision","validFrom","retiredAt","retirementRecordedAt","recordedAt"),true),
        new Table("allocations","mulino_inventory_SegmentAllocations",List.of("ID","segmentId","quantity","unit","state","workId","commandId","revision","recordedAt"),true),
        new Table("movements","mulino_inventory_QuantityMovements",List.of("ID","sourceId","targetId","quantity","unit","kind","commandId","occurredAt","recordedAt"),true),
        new Table("grants","mulino_identity_Grants",List.of("ID","actorId","delegatorId","validFrom","validUntil","revokedAt","revision","recordedAt"),true),
        new Table("outbox","mulino_runtime_Outbox",List.of("ID","commandId","operation","actorId","status","nextAction","nextCheckAt","createdAt"),false));
    ObjectNode capture(JsonNode scope,String asOf,String knownAt) throws Exception {
        UUID.fromString(Json.required(scope,"organizationId"));
        OffsetDateTime.parse(asOf);OffsetDateTime cutoff=OffsetDateTime.parse(knownAt);
        var result=evidence.base("ACTUAL_POSTGRESQL_DIAGNOSTIC",scope).put("mappingVersion","model-binding-v1")
            .put("mappingComplete",false).put("runtimeStatus","NOT_IMPLEMENTED").put("asOf",asOf).put("knownAt",knownAt);
        var queries=Json.array();var raw=Json.object();var columns=Json.array();
        String token;
        try(Connection c=DriverManager.getConnection(configuration.jdbcUrl(),configuration.username(),configuration.password())) {
            c.setAutoCommit(false);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            try(var statement=c.createStatement();var rows=statement.executeQuery("SELECT pg_current_snapshot()::text")){rows.next();token=rows.getString(1);}
            for(Table table:TABLES) {
                String timeColumn=table.temporal()?"recordedAt":"createdAt";
                String sql="SELECT "+String.join(",",table.columns())+" FROM "+table.name()+" WHERE organizationId=? AND "+timeColumn+"<=? ORDER BY ID";
                var params=Json.object().put("organizationId",scope.path("organizationId").asText()).put("knownAt",knownAt);
                var query=Json.object().put("statementId","native-physical-"+table.dataset()+"-v1").put("sql",sql).put("mappingVersion","model-binding-v1");
                query.set("parameters",params);queries.add(query);
                var rows=Json.array();
                try(var s=c.prepareStatement(sql)) {
                    s.setString(1,scope.path("organizationId").asText());s.setObject(2,cutoff);
                    try(var r=s.executeQuery()) { while(r.next()) {
                        var row=Json.object();String id=r.getString(1);
                        for(int i=0;i<table.columns().size();i++) {
                            String column=table.columns().get(i);Object value=r.getObject(i+1);
                            JsonNode node=value==null?Json.MAPPER.nullNode():value instanceof Boolean?Json.MAPPER.valueToTree(value):Json.MAPPER.getNodeFactory().textNode(value.toString());
                            row.set(column,node);
                            var cell=Json.object().put("dataset",table.dataset()).put("attribute",column).put("table",table.name()).put("column",column).put("rowId",id);
                            cell.set("value",node);if(table.columns().contains("unit")&&r.getString("unit")!=null)cell.put("unit",r.getString("unit"));columns.add(cell);
                        }rows.add(row);
                    }}
                }raw.set(table.dataset(),rows);
            }c.commit();
        }
        result.set("snapshot",Json.object().put("id",token).put("isolation","REPEATABLE_READ").put("readMode","CURRENT_COMMITTED").put("capturedAt",Instant.now().toString()));
        result.set("sourceQueries",queries);result.set("rawRows",raw);result.set("physicalColumns",columns);
        result.set("implementationSource",evidence.source("verification/harness/src/main/java/org/mulino/verification/nativeclient/NativePhysicalObserver.java"));
        result.set("missingObservations",Json.array().add("Independent API projection revision recomputation")
            .add("Immutable business effect IDs, operation/classes, installation/turn attribution")
            .add("Complete obligation, canonical physical scope and semantic dataset mappings")
            .add("Common observation rawRows object conflicts with model-binding physical-columns array"));
        return result;
    }
}
