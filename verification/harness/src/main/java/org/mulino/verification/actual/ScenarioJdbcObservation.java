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
 * column (Works: workId restricts ID). includeDescendants=true with a work restriction is refused until a descendant relation is supported. Case namespace keys (caseId, subcaseId, ...) are test metadata.
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
    static Map<String,String> names(Path root,String table) throws Exception {
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
        if(descendants&&!works.isEmpty())throw new UnsupportedOperationException("Scoped descendants require an explicit descendant relation, not an organization-wide superset");
        String revision=null;ObjectNode revisionQuery=null;ScenarioRevisionBinding.Binding binding=null;JsonNode source=null;
        if(readMode.equals("RESULT_REVISION")) {
            source=request.path("snapshotSource");
            if(!source.path("kind").asText().equals("query")||!Set.of("api","mcp").contains(source.path("route").asText()))throw new UnsupportedOperationException("RESULT_REVISION from "+source.path("kind").asText()+"/"+source.path("route").asText()+" has no independent row binding");
            binding=ScenarioRevisionBinding.binding(source.path("actionId").asText());
        }
        OffsetDateTime knownAt=OffsetDateTime.parse(Json.required(request,"knownAt"));
        var raw=Json.object();var evidence=Json.object();var measurement=Json.object();var measurementQueries=Json.array();ArrayNode measurementRows=null;String mvcc;
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
            Map<String,ObjectNode> statements=new HashMap<>();
            var projection=new ScenarioObservationProjection(c,root,orgs,items,works,lots,knownAt,
                OffsetDateTime.parse(Json.required(request,"asOf")));
            for(JsonNode requested:request.path("sources")) {
                String name=requested.asText();JsonNode tables=sources.path(name);var rows=Json.array();var queries=Json.array();
                for(JsonNode t:tables) {
                    String table=t.asText();String key=table;
                    ArrayNode tableRows=cache.get(key);
                    if(table.startsWith("@")) {
                        ObjectNode statement=statements.get(key);
                        if(tableRows==null) {
                            statement=Json.object().put("table",table);
                            if(Set.of("@conditionResults","@observationCoverage","@assessmentInputs","@supervisionIssues","@sellConditionSourceRefs","@sellConditionSourceLinks").contains(table)) {
                                tableRows=projection.virtual(table);statement.set("queries",projection.drainQueries());
                            } else {
                                if(!items.isEmpty()||!works.isEmpty()||!lots.isEmpty())throw new UnsupportedOperationException("No item/work/lot binding for virtual source "+table);
                                tableRows=virtual(c,table,orgs,knownAt,statement);
                            }
                            cache.put(key,tableRows);statements.put(key,statement);
                        }
                        queries.add(statement.deepCopy());
                        for(JsonNode row:tableRows){ObjectNode copy=(ObjectNode)row.deepCopy();if(tables.size()>1)copy.put("_table",table);rows.add(copy);}continue;
                    }
                    ObjectNode statement=statements.get(key);
                    if(tableRows==null) {
                        tableRows=projection.read(table);
                        derive(projection,table,tableRows,commandCache);
                        projection.enrich(table,tableRows);
                        cache.put(key,tableRows);
                        statement=Json.object().put("table",table);statement.set("queries",projection.drainQueries());statements.put(key,statement);
                    }
                    queries.add(statement.deepCopy());
                    for(JsonNode row:tableRows){ObjectNode copy=(ObjectNode)row.deepCopy();if(tables.size()>1)copy.put("_table",table);rows.add(copy);}
                }
                raw.set(name,rows);
                var query=Json.object();query.put("statementId","s5a-"+name+"-v1").put("sql",queries.toString()).put("mappingVersion",mapping(root).path("schemaVersion").asText());var p=Json.object();p.set("scope",scope);p.put("knownAt",knownAt.toString());query.set("parameters",p);
                var e=Json.object();e.put("complete",true).put("rowPointer","/rawRows/"+name.replace("~","~0").replace("/","~1"));e.set("sourceQuery",query);
                if(mapping(root).path("approximate").toString().contains("\""+name+"\""))e.put("approximateMapping",true);
                evidence.set(name,e);
            }
            measurement=projection.measurementMetadata();measurementRows=projection.measurementMetadataRows();measurementQueries=projection.drainQueries();
            c.commit();
        }
        var result=Json.object();result.put("snapshotRevision",revision!=null?revision:mvcc).put("asOf",Json.required(request,"asOf")).put("knownAt",Json.required(request,"knownAt")).put("scopeComplete",true);
        result.set("scope",scope);result.set("sourceEvidence",evidence);result.set("sourceQuery",evidence.path(request.path("sources").get(0).asText()).path("sourceQuery"));
        result.set("rawRows",raw);result.set("data",measurement);
        if(measurementRows!=null)result.set("measurementMetadataRows",measurementRows);
        if(!measurement.isEmpty()) {
            var derivations=Json.object();
            for(String field:List.of("itemId","unit"))if(measurement.has(field))derivations.set("/"+field,
                Json.object().put("rowPointer","/measurementMetadataRows").put("aggregate","single").put("field",field.equals("itemId")?"id":"baseUnit"));
            result.set("derivations",derivations);
        }
        var snapshot=ObserverSnapshot.snapshot(mvcc,readMode,request);if(revisionQuery!=null)snapshot.set("revisionQuery",revisionQuery);
        if(!measurementQueries.isEmpty())snapshot.set("measurementMetadataQueries",measurementQueries);
        result.set("snapshot",snapshot);
        return result;
    }
    /** The actual adapter's artifact wire binding, also used by PG tests before native semantic validation. */
    static void bindArtifact(ObjectNode data,JsonNode observedNames,String ref) {
        ((ObjectNode)data.path("snapshot")).put("artifactRef",ref);
        var sources=Json.object();
        for(var requested:observedNames) {
            String name=requested.asText();var evidence=Json.object();
            evidence.put("complete",true).put("rowPointer","/rawRows/"+name.replace("~","~0").replace("/","~1")).put("artifactRef",ref);
            evidence.set("sourceQuery",data.path("sourceEvidence").path(name).hasNonNull("sourceQuery")?data.path("sourceEvidence").path(name).path("sourceQuery"):data.path("sourceQuery"));
            sources.set(name,evidence);
        }
        data.set("sourceEvidence",sources);
    }

    /** Organization-only technical observations; missing business/time bindings are refused. */
    private static ArrayNode virtual(Connection c,String source,List<String> orgs,OffsetDateTime knownAt,ObjectNode statement) throws SQLException {
        var out=Json.array();String in=String.join(",",Collections.nCopies(orgs.size(),"?"));
        switch(source) {
            case "@transactions" -> {
                String sql="SELECT xmin::text AS xid,capabilityId,ID FROM mulino_commands_CommandRecords WHERE organizationId IN ("+in+") AND createdAt<=? ORDER BY xmin::text,ID";
                Map<String,ObjectNode> byXid=new LinkedHashMap<>();
                try(var st=c.prepareStatement(sql)){for(int i=0;i<orgs.size();i++)st.setString(i+1,orgs.get(i));st.setObject(orgs.size()+1,knownAt);try(var r=st.executeQuery()){while(r.next()){var row=byXid.computeIfAbsent(r.getString(1),x->{var o=Json.object();o.put("id",x).put("transactionId",x);o.putArray("commandIds");return o;});
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
            case "@domainRecords" -> throw new UnsupportedOperationException("Domain digest has no complete per-table scope/time binding");
            default -> throw new UnsupportedOperationException("virtual observation source "+source+" not implemented");
        }
        var values=Json.array();if(source.equals("@transactions")){orgs.forEach(values::add);values.add(knownAt.toString());}statement.set("boundValues",values);
        return out;
    }
    private static volatile Map<String,String> cdsEntities;
    private static String cdsEntity(String table) {
        if(cdsEntities==null){var out=new HashMap<String,String>();try{JsonNode csn=Json.read(Path.of(System.getProperty("repo.root",".")).resolve("backend/src/main/resources/edmx/csn.json"));csn.path("definitions").fieldNames().forEachRemaining(k->{if(k.startsWith("mulino."))out.put(k.replace('.','_').toLowerCase(Locale.ROOT),k.substring(k.lastIndexOf('.')+1));});}catch(Exception missing){}cdsEntities=out;}
        return cdsEntities.get(table.toLowerCase(Locale.ROOT));
    }
    private static final Map<String,Set<String>> COLUMNS=new java.util.concurrent.ConcurrentHashMap<>();
    static Set<String> columns(Connection c,String table) throws SQLException {
        String cacheKey=c.getMetaData().getURL()+"|"+c.getSchema()+"|"+table;
        Set<String> cached=COLUMNS.get(cacheKey);if(cached!=null)return cached;
        Set<String> out=new HashSet<>();
        try(var s=c.prepareStatement("SELECT column_name FROM information_schema.columns WHERE table_schema=current_schema() AND table_name=?")){s.setString(1,table.toLowerCase(Locale.ROOT));try(var r=s.executeQuery()){while(r.next())out.add(r.getString(1));}}
        if(out.isEmpty())throw new UnsupportedOperationException("observation table "+table+" absent from the product schema");
        COLUMNS.put(cacheKey,out);return out;
    }
    /** Observation conveniences computed only from the same transaction's rows; never from the product API. */
    static final String NO_COMMAND_RECORD="NO_COMMAND_RECORD";
    private static void derive(ScenarioObservationProjection projection,String table,ArrayNode rows,Map<String,JsonNode> commands) throws Exception {
        for(JsonNode n:rows) {
            ObjectNode row=(ObjectNode)n;
            String commandId=row.path("commandId").asText(null);
            if(commandId!=null&&!row.has("commandIdempotencyKey")) {
                JsonNode cmd=projection.linked("mulino_commands_CommandRecords",row,commandId);
                if(cmd!=null){row.set("commandIdempotencyKey",cmd.path("commandIdempotencyKey"));if(!row.has("capabilityId"))row.set("capabilityId",cmd.path("capabilityId"));if(!row.has("stableRequestOwnerId"))row.set("stableRequestOwnerId",cmd.path("stableRequestOwner"));}
            }
            // A row of a command-attributed table that no CommandRecord produced (fixture-installed opening balance,
            // installer allocation) was made by no command: it carries the explicit NO_COMMAND_RECORD key, which equals
            // no case idempotency key, instead of a missing field (round 13: 59 "filter field missing" failures).
            if(row.has("commandId")&&!row.has("commandIdempotencyKey"))row.put("commandIdempotencyKey",NO_COMMAND_RECORD);
            // A table without a commandId column (Works, CapabilityAssignments, ...) is attributed to the earliest COMMITTED
            // command whose result names the row id (approximate: the creating command); otherwise to NO_COMMAND_RECORD.
            if(!row.has("commandId")&&!row.has("commandIdempotencyKey")&&row.hasNonNull("id")&&row.hasNonNull("organizationId")&&!table.equals("mulino_commands_CommandRecords")) {
                JsonNode cmd=effectCommands(projection,row.path("organizationId").asText(),commands).get(row.path("id").asText());
                if(cmd!=null){row.set("commandIdempotencyKey",cmd.path("commandIdempotencyKey"));if(!row.has("capabilityId"))row.set("capabilityId",cmd.path("capabilityId"));if(!row.has("stableRequestOwnerId"))row.set("stableRequestOwnerId",cmd.path("stableRequestOwner"));}
                else row.put("commandIdempotencyKey",NO_COMMAND_RECORD);
            }
            switch(table) {
                case "mulino_inventory_QuantitySegments" -> {row.put("active",row.path("retiredAt").isNull()||row.path("retiredAt").isMissingNode());row.set("locationId",row.path("placeId"));row.set("physicalScope",row.path("controlScope"));}
                case "mulino_commands_CommandRecords" -> {row.set("stableRequestOwnerId",row.path("stableRequestOwner"));row.set("status",row.path("state"));}
                case "mulino_inventory_SegmentAllocations","mulino_inventory_Restrictions","mulino_inventory_DispositionBases" -> {if(!row.has("status"))row.set("status",row.path("state"));}
                case "mulino_work_read_Works" -> row.set("state",row.path("status"));
                case "mulino_work_read_ObligationReferences" -> {row.set("current",row.path("valid"));row.set("responsibleWorkId",row.path("workId"));row.set("obligationId",row.path("rootId"));
                    JsonNode root=row.hasNonNull("rootId")?projection.linked("mulino_responsibility_Roots",row,row.path("rootId").asText()):null;
                    if(root!=null){if(!row.has("sourceKind"))row.set("sourceKind",root.path("sourceKind"));if(!row.has("sourceId"))row.set("sourceId",root.path("sourceId"));}}
                case "mulino_inventory_QuantityMovements" -> {if(!row.has("segmentId"))row.set("segmentId",row.hasNonNull("targetId")?row.path("targetId"):row.path("sourceId"));row.set("originId",row.path("sourceId"));}
                case "mulino_runtime_Outbox" -> {row.set("stableRequestOwnerId",row.path("stableRequestOwner"));}
                case "mulino_definitions_DefinitionVersions" -> {row.set("hash",row.path("contentHash"));row.set("definitionId",row.path("id"));}
                default -> {}
            }
        }
    }
    /** Row id -> the earliest COMMITTED command of the organization whose result (effects and *Id fields) names it. */
    private static JsonNode effectCommands(ScenarioObservationProjection projection,String org,Map<String,JsonNode> cache) throws Exception {
        var rows=projection.commands(org);
        String key="effects:"+org;if(cache.containsKey(key))return cache.get(key);
        var out=Json.object();
        for(var cmd:rows) {
            if(!cmd.hasNonNull("resultJson"))continue;
            JsonNode parsed=ScenarioObservationProjection.parse(cmd,"resultJson");Set<String> named=new HashSet<>();
            named(parsed.path("effects"),named);parsed.fields().forEachRemaining(f->{if((f.getKey().endsWith("Id")||f.getKey().equals("id"))&&f.getValue().isTextual())named.add(f.getValue().asText());});
            for(String id:named)if(!out.has(id))out.set(id,cmd);
        }
        cache.put(key,out);return out;
    }
    private static void named(JsonNode node,Set<String> out){if(node.isTextual()&&node.asText().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))out.add(node.asText());else if(node.isContainerNode())node.elements().forEachRemaining(v->named(v,out));}
    static void put(ObjectNode row,String name,Object value) {
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
