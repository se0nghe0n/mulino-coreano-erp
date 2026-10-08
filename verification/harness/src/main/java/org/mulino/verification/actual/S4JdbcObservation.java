package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.mulino.verification.Json;

/** Full organization fixture snapshot. Table names are harness-owned, never request SQL. */
final class S4JdbcObservation {
    private static final Set<String> PREFIXES=Set.of("mulino_identity_","mulino_work_","mulino_sales_","mulino_returns_","mulino_recall_","mulino_settlement_","mulino_trade_","mulino_purchase_","mulino_shipment_","mulino_regulatory_","mulino_receipt_","mulino_quality_","mulino_inventory_","mulino_commands_","mulino_governance_","mulino_work_read_","mulino_responsibility_","mulino_evidence_","mulino_evaluation_","mulino_runtime_");
    static ObjectNode capture(ActualConfiguration config,JsonNode request)throws Exception {
        String readMode=ObserverSnapshot.readMode(request);
        JsonNode scope=request.path("scope");
        String organization=Json.required(scope,"organizationId");UUID.fromString(organization);
        if(scope.size()!=1)throw new UnsupportedOperationException("S4 native snapshot requires explicit complete fixture organization scope");
        var raw=Json.object();var evidence=Json.object();String mvcc;
        try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {
            c.setAutoCommit(false);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            mvcc=ObserverSnapshot.token(c);
            var tables=new TreeSet<String>();
            try(var s=c.prepareStatement("SELECT table_name FROM information_schema.tables WHERE table_schema=current_schema() AND table_type='BASE TABLE' ORDER BY table_name");var rows=s.executeQuery()) {
                while(rows.next()){String table=rows.getString(1);if(PREFIXES.stream().anyMatch(prefix->table.startsWith(prefix.toLowerCase(Locale.ROOT))))tables.add(table);}
            }
            for(String domain:List.of("sales_","returns_","recall_","settlement_"))if(tables.stream().noneMatch(t->t.contains(domain)))throw new UnsupportedOperationException("S4 product tables not deployed: "+domain);
            for(String table:tables) {
                if(!table.matches("[a-z][a-z0-9_]*"))throw new IllegalStateException("Unexpected product table identifier");
                boolean organizationColumn=false;var columns=new ArrayList<String>();
                try(var s=c.prepareStatement("SELECT column_name FROM information_schema.columns WHERE table_schema=current_schema() AND table_name=? ORDER BY ordinal_position")){s.setString(1,table);try(var rows=s.executeQuery()){while(rows.next()){String column=rows.getString(1);columns.add(column);if(column.equals("organizationid"))organizationColumn=true;}}}
                if(!organizationColumn)continue;
                String sql="SELECT "+String.join(",",columns)+" FROM "+table+" WHERE organizationId=?"+(columns.contains("id")?" ORDER BY id":"");
                var rows=Json.array();try(var s=c.prepareStatement(sql)){s.setString(1,organization);try(var result=s.executeQuery()){while(result.next()){var row=Json.object();for(int col=1;col<=columns.size();col++){Object value=result.getObject(col);if(value==null)row.putNull(columns.get(col-1));else if(value instanceof java.math.BigDecimal d)row.put(columns.get(col-1),d.stripTrailingZeros().toPlainString());else row.set(columns.get(col-1),Json.MAPPER.valueToTree(value instanceof java.sql.Timestamp?value.toString():value));}rows.add(row);}}}
                raw.set(table,rows);var query=Json.object();query.put("statementId","s4-"+table+"-v1").put("sql",sql).put("mappingVersion","1.0.0");query.set("parameters",Json.parse("{\"boundValues\":[\""+organization+"\"]}"));
                var source=Json.object();source.put("complete",true).put("rowPointer","/rawRows/"+table);source.set("sourceQuery",query);evidence.set(table,source);
            }c.commit();
        }
        var result=Json.object();result.put("snapshotRevision",mvcc).put("asOf",Json.required(request,"asOf")).put("knownAt",Json.required(request,"knownAt")).put("scopeComplete",true);result.set("scope",scope);result.set("rawRows",raw);result.set("sourceEvidence",evidence);result.set("sourceQuery",evidence.elements().next().path("sourceQuery"));result.set("data",Json.object());
        result.set("snapshot",ObserverSnapshot.snapshot(mvcc,readMode,request));return result;
    }
    private S4JdbcObservation(){}
}
