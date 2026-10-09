package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.mulino.verification.Json;

/**
 * Scenario-suite DB observer (./verify scenarios --actual). Reads each requested source from the product tables named in
 * verification/actual/scenarios/observation-sources.json inside one read-only REPEATABLE READ transaction.
 *
 * Scope: organizationId/organizationIds restrict every table; itemId(s), workId(s), lotId restrict tables having that
 * column (Works: workId restricts ID). includeDescendants=true widens the work restriction to the organization/item
 * scope (a superset that contains descendant works). Case namespace keys (caseId, subcaseId, ...) are test metadata.
 *
 * RESULT_REVISION: the observer recomputes an organization row digest in its own snapshot and reports the product
 * revision only when it equals the digest bound around the issuing query (ScenarioRevisionBinding). The product API is
 * never re-issued. An issuing action that is not an api/mcp query is refused.
 */
final class ScenarioJdbcObservation {
    static final Set<String> CASE_METADATA=Set.of("caseId","subcaseId","scenarioId","caseNamespace","caseKey","environmentId","includeDescendants","rawRowsOrder");
    private static volatile JsonNode mapping;
    private static volatile Map<String,Map<String,String>> csnNames;
    private ScenarioJdbcObservation() {}

    static JsonNode mapping(Path root) throws Exception {
        if(mapping==null)mapping=Json.read(root.resolve("verification/actual/scenarios/observation-sources.json"));
        return mapping;
    }
    /** CSN element names by lower-case column, per lower-case table: PostgreSQL folds the CDS identifiers. */
    private static Map<String,String> names(Path root,String table) throws Exception {
        if(csnNames==null) {
            JsonNode csn=Json.read(root.resolve("backend/src/main/resources/edmx/csn.json"));Map<String,Map<String,String>> out=new HashMap<>();
            for(var it=csn.path("definitions").fields();it.hasNext();){var e=it.next();if(!e.getKey().startsWith("mulino."))continue;Map<String,String> cols=new HashMap<>();
                e.getValue().path("elements").fieldNames().forEachRemaining(n->cols.put(n.toLowerCase(Locale.ROOT),n));out.put(e.getKey().replace('.','_').toLowerCase(Locale.ROOT),cols);}
            csnNames=out;
        }
        return csnNames.getOrDefault(table.toLowerCase(Locale.ROOT),Map.of());
    }

    static ObjectNode capture(ActualConfiguration config,Path root,JsonNode request) throws Exception {
        if(request.path("sources").isEmpty())throw new IllegalArgumentException("Raw sources required");
        String ref=request.path("snapshotRef").asText(null);
        String readMode=ref==null?"CURRENT_COMMITTED":ref;
        if(!Set.of("CURRENT_COMMITTED","CURRENT_LOCK_WAIT","RESULT_REVISION").contains(readMode))throw new UnsupportedOperationException("observer readMode "+readMode+" not implemented");
        JsonNode sources=mapping(root).path("sources");
        List<String> unmapped=new ArrayList<>();for(JsonNode s:request.path("sources"))if(!sources.has(s.asText()))unmapped.add(s.asText());
        if(!unmapped.isEmpty())throw new UnsupportedOperationException("observation source not mapped: "+String.join(",",unmapped));
        JsonNode scope=request.path("scope");
        List<String> orgs=ids(scope,"organizationId","organizationIds"),items=ids(scope,"itemId","itemIds"),works=ids(scope,"workId","workIds"),lots=ids(scope,"lotId","lotIds");
        if(orgs.isEmpty())throw new IllegalArgumentException("Observer scope requires organizationId");
        for(var it=scope.fieldNames();it.hasNext();){String k=it.next();if(!CASE_METADATA.contains(k)&&!Set.of("organizationId","organizationIds","itemId","itemIds","workId","workIds","lotId","lotIds").contains(k))throw new UnsupportedOperationException("observer scope key "+k+" not implemented");}
        boolean descendants=scope.path("includeDescendants").asBoolean(false);
        if(descendants)works=List.of();
        String revision=null;ObjectNode revisionQuery=null;ScenarioRevisionBinding.Binding binding=null;JsonNode source=null;
        if(readMode.equals("RESULT_REVISION")) {
            source=request.path("snapshotSource");
            if(!source.path("kind").asText().equals("query")||!Set.of("api","mcp").contains(source.path("route").asText()))throw new UnsupportedOperationException("RESULT_REVISION from "+source.path("kind").asText()+"/"+source.path("route").asText()+" has no independent row binding");
            binding=ScenarioRevisionBinding.binding(source.path("actionId").asText());
        }
        OffsetDateTime knownAt=OffsetDateTime.parse(Json.required(request,"knownAt"));
        var raw=Json.object();var evidence=Json.object();String mvcc;
        try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {
            c.setAutoCommit(false);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            mvcc=ObserverSnapshot.token(c);
            if(readMode.equals("RESULT_REVISION")) {
                // Independent: the digest is computed from this transaction's rows; the product API is not called.
                String digest=ScenarioRevisionBinding.digest(c);
                revision=binding!=null&&binding.digest().equals(digest)?binding.revision():"row-digest:"+digest;
                revisionQuery=Json.object();revisionQuery.put("statementId","s5b-organization-row-digest-v1")
                    .put("sql","md5 over every row (row::text, ordered) of every organization-scoped mulino_* table except mulino_runtime_*, in the observer's REPEATABLE READ snapshot; compared with the digest bound around the issuing query (before = after)");
                var p=Json.object();p.put("capabilityId",source.path("capabilityId").asText()).put("actionId",source.path("actionId").asText()).put("observedDigest",digest)
                    .put("boundDigest",binding==null?null:binding.digest()).put("digestMatched",binding!=null&&binding.digest().equals(digest)).put("organizations",String.join(",",ScenarioRevisionBinding.organizations())).put("productApiCalled",false);
                revisionQuery.set("parameters",p);revisionQuery.put("mappingVersion","2.0.0");
            }
            Map<String,ArrayNode> cache=new HashMap<>();Map<String,JsonNode> commandCache=new HashMap<>();
            for(JsonNode requested:request.path("sources")) {
                String name=requested.asText();JsonNode tables=sources.path(name);var rows=Json.array();var queries=Json.array();
                for(JsonNode t:tables) {
                    String table=t.asText();String key=table;
                    ArrayNode tableRows=cache.get(key);
                    if(table.startsWith("@")){var statement=Json.object();if(tableRows==null){tableRows=virtual(c,table,orgs,statement);cache.put(key,tableRows);}else statement.put("sql","(same statement as an earlier source in this observation)");
                        statement.put("table",table);queries.add(statement);for(JsonNode row:tableRows){ObjectNode copy=(ObjectNode)row.deepCopy();if(tables.size()>1)copy.put("_table",table);rows.add(copy);}continue;}
                    var columns=columns(c,table);var statement=Json.object();
                    if(tableRows==null) {
                        List<Object> params=new ArrayList<>();StringBuilder sql=new StringBuilder("SELECT * FROM "+table+" r WHERE ");
                        if(columns.contains("organizationid")){sql.append("r.organizationId IN (").append(String.join(",",Collections.nCopies(orgs.size(),"?"))).append(")");params.addAll(orgs);}
                        else sql.append("FALSE");
                        if(!items.isEmpty()&&columns.contains("itemid")){sql.append(" AND r.itemId IN (").append(String.join(",",Collections.nCopies(items.size(),"?"))).append(")");params.addAll(items);}
                        String workColumn=table.equalsIgnoreCase("mulino_work_read_Works")?"id":columns.contains("workid")?"workid":null;
                        if(!works.isEmpty()&&workColumn!=null){sql.append(" AND r.").append(workColumn).append(" IN (").append(String.join(",",Collections.nCopies(works.size(),"?"))).append(")");params.addAll(works);}
                        if(!lots.isEmpty()&&columns.contains("lotid")){sql.append(" AND r.lotId IN (").append(String.join(",",Collections.nCopies(lots.size(),"?"))).append(")");params.addAll(lots);}
                        if(columns.contains("recordedat")){sql.append(" AND r.recordedAt<=?");params.add(knownAt);}
                        sql.append(columns.contains("id")?" ORDER BY r.ID":"");
                        tableRows=Json.array();
                        try(var s=c.prepareStatement(sql.toString())){for(int i=0;i<params.size();i++)s.setObject(i+1,params.get(i));
                            try(var r=s.executeQuery()){var md=r.getMetaData();var names=names(root,table);while(r.next()){var row=Json.object();for(int col=1;col<=md.getColumnCount();col++){String label=md.getColumnLabel(col);String n=names.getOrDefault(label,label);if(n.equals("ID"))n="id";put(row,n,r.getObject(col));}tableRows.add(row);}}}
                        derive(c,table,tableRows,commandCache);
                        cache.put(key,tableRows);
                        statement.put("sql",sql.toString());statement.set("boundValues",Json.MAPPER.valueToTree(params.stream().map(Object::toString).toList()));
                    } else statement.put("sql","(same statement as an earlier source in this observation)");
                    statement.put("table",table);queries.add(statement);
                    for(JsonNode row:tableRows){ObjectNode copy=(ObjectNode)row.deepCopy();if(tables.size()>1)copy.put("_table",table);rows.add(copy);}
                }
                raw.set(name,rows);
                var query=Json.object();query.put("statementId","s5a-"+name+"-v1").put("sql",queries.toString()).put("mappingVersion","1.0.0");var p=Json.object();p.set("scope",scope);p.put("knownAt",knownAt.toString());query.set("parameters",p);
                var e=Json.object();e.put("complete",true).put("rowPointer","/rawRows/"+name.replace("~","~0").replace("/","~1"));e.set("sourceQuery",query);
                if(mapping(root).path("approximate").toString().contains("\""+name+"\""))e.put("approximateMapping",true);
                evidence.set(name,e);
            }
            c.commit();
        }
        var result=Json.object();result.put("snapshotRevision",revision!=null?revision:mvcc).put("asOf",Json.required(request,"asOf")).put("knownAt",Json.required(request,"knownAt")).put("scopeComplete",true);
        result.set("scope",scope);result.set("sourceEvidence",evidence);result.set("sourceQuery",evidence.path(request.path("sources").get(0).asText()).path("sourceQuery"));
        result.set("rawRows",raw);result.set("data",Json.object());
        var snapshot=ObserverSnapshot.snapshot(mvcc,readMode,request);if(revisionQuery!=null)snapshot.set("revisionQuery",revisionQuery);
        result.set("snapshot",snapshot);
        return result;
    }
    /**
     * Virtual sources read from the same snapshot, never from the product API:
     * @transactions  one row per PostgreSQL transaction (xmin) that last wrote an organization CommandRecords row
     *                (id = that xid, capabilityId, commandIds); a later update of the record moves it (approximate).
     * @dbPrivileges  information_schema.role_table_grants of every mulino_* table (roleId = grantee, table = CDS entity
     *                name without the trailing plural s, entity = full CDS entity name, privilege).
     * @conditionResults  each element of AssessmentReferences.conditionsJson, with assessmentId, workId and the
     *                assessment's evaluatorVersion added when the element does not state them.
     */
    private static ArrayNode virtual(Connection c,String source,List<String> orgs,ObjectNode statement) throws SQLException {
        var out=Json.array();String in=String.join(",",Collections.nCopies(orgs.size(),"?"));
        switch(source) {
            case "@transactions" -> {
                String sql="SELECT xmin::text AS xid,capabilityId,ID FROM mulino_commands_CommandRecords WHERE organizationId IN ("+in+") ORDER BY xmin::text,ID";
                Map<String,ObjectNode> byXid=new LinkedHashMap<>();
                try(var st=c.prepareStatement(sql)){for(int i=0;i<orgs.size();i++)st.setString(i+1,orgs.get(i));try(var r=st.executeQuery()){while(r.next()){var row=byXid.computeIfAbsent(r.getString(1),x->{var o=Json.object();o.put("id",x).put("transactionId",x);o.putArray("commandIds");return o;});
                    if(!row.has("capabilityId"))row.put("capabilityId",r.getString(2));else if(!row.path("capabilityId").asText().equals(r.getString(2)))row.put("capabilityId","MIXED");((ArrayNode)row.path("commandIds")).add(r.getString(3));}}}
                byXid.values().forEach(out::add);statement.put("sql",sql);
            }
            case "@dbPrivileges" -> {
                String sql="SELECT grantee,table_name,privilege_type FROM information_schema.role_table_grants WHERE table_schema=current_schema() AND table_name LIKE 'mulino\\_%' ORDER BY table_name,grantee,privilege_type";
                try(var st=c.prepareStatement(sql);var r=st.executeQuery()){while(r.next()){String t=r.getString(2);String entity=t.substring(t.lastIndexOf('_')+1);
                    var row=Json.object();row.put("roleId",r.getString(1));row.put("tableName",t);row.put("privilege",r.getString(3));out.add(row);row.put("entityTable",entity);}}
                // CDS element case is lost in PostgreSQL identifiers; map back through the CSN names.
                for(JsonNode n:out){ObjectNode row=(ObjectNode)n;String cds=cdsEntity(row.path("tableName").asText());if(cds!=null){row.put("entity",cds);row.put("table",cds.endsWith("s")?cds.substring(0,cds.length()-1):cds);}}
                statement.put("sql",sql);
            }
            case "@conditionResults" -> {
                String sql="SELECT ID,workId,evaluatorVersion,conditionsJson FROM mulino_work_read_AssessmentReferences WHERE organizationId IN ("+in+") ORDER BY ID";
                try(var st=c.prepareStatement(sql)){for(int i=0;i<orgs.size();i++)st.setString(i+1,orgs.get(i));try(var r=st.executeQuery()){while(r.next()){JsonNode conditions;try{conditions=Json.parse(r.getString(4));}catch(Exception invalid){continue;}
                    if(conditions.isObject()&&conditions.has("conditions"))conditions=conditions.path("conditions");
                    for(JsonNode cond:conditions)if(cond.isObject()){var row=(ObjectNode)cond.deepCopy();if(!row.has("assessmentId"))row.put("assessmentId",r.getString(1));if(!row.has("workId"))row.put("workId",r.getString(2));if(!row.has("evaluatorVersion"))row.put("evaluatorVersion",r.getString(3));out.add(row);}}}}
                statement.put("sql",sql);
            }
            default -> throw new UnsupportedOperationException("virtual observation source "+source+" not implemented");
        }
        statement.set("boundValues",Json.MAPPER.valueToTree(orgs));
        return out;
    }
    private static volatile Map<String,String> cdsEntities;
    private static String cdsEntity(String table) {
        if(cdsEntities==null){var out=new HashMap<String,String>();try{JsonNode csn=Json.read(Path.of(System.getProperty("repo.root",".")).resolve("backend/src/main/resources/edmx/csn.json"));csn.path("definitions").fieldNames().forEachRemaining(k->{if(k.startsWith("mulino."))out.put(k.replace('.','_').toLowerCase(Locale.ROOT),k.substring(k.lastIndexOf('.')+1));});}catch(Exception missing){}cdsEntities=out;}
        return cdsEntities.get(table.toLowerCase(Locale.ROOT));
    }
    private static final Map<String,Set<String>> COLUMNS=new java.util.concurrent.ConcurrentHashMap<>();
    private static Set<String> columns(Connection c,String table) throws SQLException {
        Set<String> cached=COLUMNS.get(table);if(cached!=null)return cached;
        Set<String> out=new HashSet<>();
        try(var s=c.prepareStatement("SELECT column_name FROM information_schema.columns WHERE table_name=?")){s.setString(1,table.toLowerCase(Locale.ROOT));try(var r=s.executeQuery()){while(r.next())out.add(r.getString(1));}}
        if(out.isEmpty())throw new UnsupportedOperationException("observation table "+table+" absent from the product schema");
        COLUMNS.put(table,out);return out;
    }
    /** Observation conveniences computed only from the same transaction's rows; never from the product API. */
    private static void derive(Connection c,String table,ArrayNode rows,Map<String,JsonNode> commands) throws SQLException {
        Map<String,JsonNode> roots=new HashMap<>();
        for(JsonNode n:rows) {
            ObjectNode row=(ObjectNode)n;
            String commandId=row.path("commandId").asText(null);
            if(commandId!=null&&!row.has("commandIdempotencyKey")) {
                JsonNode cmd=commands.computeIfAbsent(commandId,id->command(c,id));
                if(cmd!=null){row.set("commandIdempotencyKey",cmd.path("commandIdempotencyKey"));if(!row.has("capabilityId"))row.set("capabilityId",cmd.path("capabilityId"));if(!row.has("stableRequestOwnerId"))row.set("stableRequestOwnerId",cmd.path("stableRequestOwner"));}
            }
            switch(table) {
                case "mulino_inventory_QuantitySegments" -> {row.put("active",row.path("retiredAt").isNull()||row.path("retiredAt").isMissingNode());row.set("locationId",row.path("placeId"));row.set("physicalScope",row.path("controlScope"));}
                case "mulino_commands_CommandRecords" -> {row.set("stableRequestOwnerId",row.path("stableRequestOwner"));row.set("status",row.path("state"));}
                case "mulino_inventory_SegmentAllocations","mulino_inventory_Restrictions","mulino_inventory_DispositionBases" -> {if(!row.has("status"))row.set("status",row.path("state"));}
                case "mulino_work_read_Works" -> row.set("state",row.path("status"));
                case "mulino_work_read_ObligationReferences" -> {row.set("current",row.path("valid"));row.set("responsibleWorkId",row.path("workId"));row.set("obligationId",row.path("rootId"));
                    JsonNode root=row.hasNonNull("rootId")?roots.computeIfAbsent(row.path("rootId").asText(),id->root(c,id)):null;
                    if(root!=null){if(!row.has("sourceKind"))row.set("sourceKind",root.path("sourceKind"));if(!row.has("sourceId"))row.set("sourceId",root.path("sourceId"));}}
                case "mulino_inventory_QuantityMovements" -> {if(!row.has("segmentId"))row.set("segmentId",row.hasNonNull("targetId")?row.path("targetId"):row.path("sourceId"));}
                case "mulino_runtime_Outbox" -> {row.set("stableRequestOwnerId",row.path("stableRequestOwner"));}
                default -> {}
            }
        }
    }
    private static JsonNode root(Connection c,String id) {
        try(var s=c.prepareStatement("SELECT sourceKind,sourceId FROM mulino_responsibility_Roots WHERE ID=?")){s.setString(1,id);
            try(var r=s.executeQuery()){if(!r.next())return null;var o=Json.object();o.put("sourceKind",r.getString(1));o.put("sourceId",r.getString(2));return o;}}
        catch(SQLException failure){throw new IllegalStateException(failure);}
    }
    private static JsonNode command(Connection c,String id) {
        try(var s=c.prepareStatement("SELECT commandIdempotencyKey,capabilityId,stableRequestOwner FROM mulino_commands_CommandRecords WHERE ID=?")){s.setString(1,id);
            try(var r=s.executeQuery()){if(!r.next())return null;var o=Json.object();o.put("commandIdempotencyKey",r.getString(1));o.put("capabilityId",r.getString(2));o.put("stableRequestOwner",r.getString(3));return o;}}
        catch(SQLException failure){throw new IllegalStateException(failure);}
    }
    private static void put(ObjectNode row,String name,Object value) {
        if(value==null)row.putNull(name);
        else if(value instanceof java.math.BigDecimal d)row.put(name,d.stripTrailingZeros().toPlainString());
        else if(value instanceof Number number)row.set(name,Json.MAPPER.valueToTree(number));
        else if(value instanceof Boolean b)row.put(name,b);
        else if(value instanceof java.sql.Timestamp ts)row.put(name,ts.toInstant().toString());
        else row.put(name,value.toString());
    }
    private static List<String> ids(JsonNode scope,String single,String plural) {
        List<String> out=new ArrayList<>();
        if(scope.hasNonNull(single))out.add(UUID.fromString(scope.path(single).asText()).toString());
        for(JsonNode v:scope.path(plural))out.add(UUID.fromString(v.asText()).toString());
        return out;
    }
}
