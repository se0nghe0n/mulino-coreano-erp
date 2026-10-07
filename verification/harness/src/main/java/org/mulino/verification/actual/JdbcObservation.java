package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.*;
import java.time.Instant;
import org.mulino.verification.Json;

/** A separate JDBC read transaction with explicit projection. API hashes are not MVCC IDs. */
public final class JdbcObservation {
    private final ActualConfiguration configuration;
    public JdbcObservation(ActualConfiguration configuration){this.configuration=configuration;}
    public ObjectNode capture(JsonNode request) throws Exception {
        if(request.hasNonNull("snapshotRef"))throw new UnsupportedOperationException("Independent API projection snapshot reconstruction pending; requested token is not a PostgreSQL snapshot");
        if(request.path("sources").isEmpty())throw new IllegalArgumentException("Raw sources required");
        if(request.path("sources").size()!=1||!request.path("sources").get(0).asText().equals("segments"))return S2JdbcObservation.capture(configuration,request);
        var scope=request.path("scope");
        for(String field:java.util.List.of("organizationId","itemId","lotId"))java.util.UUID.fromString(Json.required(scope,field));
        if(scope.size()!=3)throw new UnsupportedOperationException("Raw segments require complete organization/item/lot scope");
        String sql="SELECT ID AS \"id\",itemId AS \"itemId\",lotId AS \"lotId\",quantity,unit,placeId AS \"locationId\",identificationStatus AS \"identificationStatus\",mixtureStatus AS \"mixtureStatus\" FROM mulino_inventory_QuantitySegments WHERE organizationId=? AND itemId=? AND lotId=? AND validFrom<=? AND recordedAt<=? AND (retiredAt IS NULL OR retiredAt>? OR retirementRecordedAt>?) ORDER BY ID";
        var query=Json.object();query.put("statementId","s1-physical-segments-v1").put("sql",sql).put("mappingVersion","1.0.0");var parameters=Json.object();parameters.set("scope",scope);parameters.put("asOf",Json.required(request,"asOf")).put("knownAt",Json.required(request,"knownAt"));query.set("parameters",parameters);
        var rows=Json.array();String snapshot;
        try(var c=DriverManager.getConnection(configuration.jdbcUrl(),configuration.username(),configuration.password())) {
            c.setAutoCommit(false);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            try(var statement=c.createStatement();var result=statement.executeQuery("SELECT pg_current_snapshot()::text")){result.next();snapshot=result.getString(1);}
            try(var statement=c.prepareStatement(sql)) {
                statement.setString(1,scope.path("organizationId").asText());statement.setString(2,scope.path("itemId").asText());statement.setString(3,scope.path("lotId").asText());
                statement.setObject(4,java.time.OffsetDateTime.parse(request.path("asOf").asText()));statement.setObject(5,java.time.OffsetDateTime.parse(request.path("knownAt").asText()));statement.setObject(6,java.time.OffsetDateTime.parse(request.path("asOf").asText()));statement.setObject(7,java.time.OffsetDateTime.parse(request.path("knownAt").asText()));
                try(var result=statement.executeQuery()){while(result.next()){var row=Json.object();var metadata=result.getMetaData();for(int i=1;i<=metadata.getColumnCount();i++){Object value=result.getObject(i);if(value==null)row.putNull(metadata.getColumnLabel(i));else row.put(metadata.getColumnLabel(i),value.toString());}rows.add(row);}}
            }c.commit();
        }
        var result=Json.object();result.put("snapshotRevision",snapshot).put("asOf",request.path("asOf").asText()).put("knownAt",request.path("knownAt").asText()).put("scopeComplete",true);result.set("scope",scope);result.set("sourceQuery",query);
        var snap=Json.object();snap.put("id",snapshot).put("isolation","REPEATABLE_READ").put("capturedAt",Instant.now().toString());result.set("snapshot",snap);
        var raw=Json.object();raw.set("segments",rows);result.set("rawRows",raw);result.set("data",Json.object());return result;
    }
}
