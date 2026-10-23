package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mulino.verification.*;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;

/** Bounded T09 public HTTP input probe. Installs declared fixtures; never seeds successful facts. */
final class T09SaleSourceFacts {
    static final String BASE = "e932710856553570094a3350268417f19b03a021";
    static final String PREFIX = "t09-sale-source-";
    final Path root;
    final ActualConfiguration config;
    final ObjectNode fixture, installed;
    final JsonNode aliases, sub;
    final Map<String, JsonNode> results = new LinkedHashMap<>();
    final List<JsonNode> httpReceipts = new ArrayList<>();
    final Map<String, JsonNode> inventoryScopeAudits = new LinkedHashMap<>();
    final HttpClient http = HttpClient.newHttpClient();

    T09SaleSourceFacts(Path root, ActualConfiguration config, String sid, boolean original,
                       Consumer<ObjectNode> mutate) throws Exception {
        if (!Set.of("cumulative-versus-state", "exists-versus-end-throughout").contains(sid))
            throw new IllegalArgumentException("Only two declared T09 sale-source subcases allowed");
        this.root = root; this.config = config;
        JsonNode cases = original ? original("verification/cases/T09/case.json") : Json.read(root.resolve("verification/cases/T09/case.json"));
        JsonNode selected = null;
        for (var candidate : cases.path("subcases")) if (sid.equals(candidate.path("id").asText())) selected = candidate;
        sub = Objects.requireNonNull(selected);
        fixture = (ObjectNode)(original ? original(sub.path("fixtureRef").asText()) : Json.read(root.resolve(sub.path("fixtureRef").asText())));
        mutate.accept(fixture);
        var bundle = Json.object().put("fixtureHash", FixtureInstaller.contentHash(fixture)); bundle.set("fixture", fixture);
        installed = ScenarioFixtureInstaller.install(config, root, ActualFixtureBindings.bind(root, bundle, config), true);
        aliases = installed.path("aliasMap");
        ScenarioRevisionBinding.reset(List.of(aliases.path("ORG").asText()));
        post("/__verification/runtime/clock/fixture-start", Json.object().put("instant", fixture.path("clock").path("asOf").asText()), "A");
    }
    JsonNode original(String path) throws Exception {
        var p = new ProcessBuilder("git", "show", BASE+":"+path).directory(root.toFile()).start();
        String text = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        if (p.waitFor()!=0) throw new IllegalStateException("Frozen input unavailable: "+path);
        return Json.parse(text);
    }
    JsonNode post(String path, ObjectNode body, String actor) throws Exception {
        var credential = (ObjectNode)ActualFixtureBindings.credentialActor(root, config, fixture.path("actors").path(actor));
        credential.put("organizationAlias", installed.path("organizationExternalAlias").asText());
        var request = HttpRequest.newBuilder(config.baseUri().resolve(path)).timeout(java.time.Duration.ofSeconds(60))
            .header("Authorization", "Bearer "+new JwtSigner(config).sign(credential)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        var receipt = Json.object().put("httpStatus", response.statusCode()).put("driverStatus", "EXECUTED");
        receipt.set("request", body); receipt.set("response", Json.parse(response.body()));
        receipt.set("transport", Json.object().put("method", "POST").put("path", path).put("actorRef", actor));
        receipt.set("provenance", Json.object().put("scopeComplete",true).put("source","HTTP"));
        httpReceipts.add(receipt); return receipt;
    }
    JsonNode call(String cap, ObjectNode request, String actor) throws Exception {
        return post("/api/ontology/"+(request.has("intentKind") ? request.path("intentKind").asText().equals("RECORD")?"records/":"commands/" : "queries/")+cap, request, actor);
    }
    ObjectNode request(String id) {
        for(var action:sub.path("actions")) if(id.equals(action.path("id").asText()))
            return (ObjectNode)new ReferenceResolver(results,aliases).resolve(action.path("request"));
        throw new IllegalArgumentException(id);
    }
    String reserveId() { return sub.path("id").asText().equals("cumulative-versus-state")?"reserve60":"reserve"; }
    String saleId() { return reserveId().equals("reserve60")?"sale60":"sale"; }
    String segment() {
        for(var action:sub.path("actions"))if(reserveId().equals(action.path("id").asText()))
            return new ReferenceResolver(results,aliases).resolve(action.at("/request/slots/segmentId")).asText();
        throw new IllegalArgumentException(reserveId());
    }
    String work() { return results.get(saleId()).at("/response/workId").asText(); }
    JsonNode execute(JsonNode action) throws Exception {
        String id=action.path("id").asText(), kind=action.path("kind").asText(); JsonNode result;
        if(kind.equals("invoke")||kind.equals("query")) {
            String before=kind.equals("query")?ScenarioRevisionBinding.digest(config):null;
            result=call(action.path("capabilityId").asText(),request(id),action.path("actorRef").asText());
            if(before!=null && result.path("response").hasNonNull("snapshotRevision"))
                ScenarioRevisionBinding.bind(config,id,before,result.at("/response/snapshotRevision").asText());
            if(kind.equals("query") && action.path("capabilityId").asText().equals("getInventory"))
                auditInventoryScope(id, action, result, before);
        } else if(kind.equals("control")) {
            var p=(ObjectNode)new ReferenceResolver(results,aliases).resolve(action.path("control").path("parameters"));
            result=post("/__verification/runtime/clock",Json.object().put("instant",p.path("instant").asText()),"A");
        } else if(kind.equals("observe")) {
            var observation=(ObjectNode)new ReferenceResolver(results,aliases).resolve(action.path("observation"));
            var declared=action.path("observation").path("snapshotRef");
            if(declared.has("$result")) {
                String issuing=declared.path("$result").path("actionId").asText();
                observation.put("snapshotRef","RESULT_REVISION");
                for(var a:sub.path("actions")) if(issuing.equals(a.path("id").asText())) {
                    var source=Json.object().put("actionId",issuing);
                    for(String key:List.of("kind","route","capabilityId","actorRef"))source.set(key,a.path(key));
                    source.set("request",request(issuing));observation.set("snapshotSource",source);
                }
            }
            var data=ScenarioJdbcObservation.capture(config,root,observation);
            var r=Json.object().put("driverStatus","EXECUTED");r.set("data",data);
            r.set("provenance",Json.object().put("scopeComplete",true).put("source","POSTGRESQL"));result=r;
        } else return null;
        results.put(id,result);return result;
    }
    /** Extra read-only probes preserve the original action, actor, clock and result. */
    void auditInventoryScope(String id, JsonNode action, JsonNode authoredResult, String before) throws Exception {
        var authored=request(id);
        var global=authored.deepCopy();((ObjectNode)global.path("scope")).remove("placeId");
        var transit=authored.deepCopy();((ObjectNode)transit.path("scope")).put("placeId",aliases.path("TRANSIT").asText());
        var audit=Json.object();audit.set("authoredRequest",authored);audit.set("authoredResult",authoredResult);
        audit.set("itemOnly",call("getInventory",global,action.path("actorRef").asText()));
        audit.set("transit",call("getInventory",transit,action.path("actorRef").asText()));
        String sql="SELECT id,itemid,placeid,quantity,unit,retiredat FROM mulino_inventory_QuantitySegments WHERE organizationid=? AND itemid=? ORDER BY id";
        audit.put("sql",sql);audit.set("sqlParameters",Json.array().add(aliases.path("ORG").asText()).add(authored.path("scope").path("itemId").asText()));
        try(var db=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {
            db.setAutoCommit(false);db.setReadOnly(true);db.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            try(var statement=db.createStatement();var rs=statement.executeQuery("SELECT txid_current_snapshot()::text, current_timestamp::text")) {
                rs.next();audit.put("independentSqlSnapshot",rs.getString(1));audit.put("independentSqlTime",rs.getString(2));
            }
            try(var s=db.prepareStatement(sql)) {
                s.setString(1,aliases.path("ORG").asText());s.setString(2,authored.path("scope").path("itemId").asText());
                var rows=Json.array();try(var rs=s.executeQuery()) {while(rs.next()) {
                    var row=Json.object();for(int i=1;i<=rs.getMetaData().getColumnCount();i++) {
                        String key=rs.getMetaData().getColumnLabel(i),value=rs.getString(i);
                        if(value==null)row.putNull(key);else row.put(key,value);
                    }rows.add(row);
                }}audit.set("sqlRows",rows);
            }
            db.commit();
        }
        audit.put("businessDigestBefore",before);audit.put("businessDigestAfter",ScenarioRevisionBinding.digest(config));
        audit.put("sqlIsSeparateReadOnlySnapshot",true);
        inventoryScopeAudits.put(id,audit);
    }
    /** Stops at the first non-APPLIED command, retaining the exact HTTP receipt. */
    String through(String stop, Set<String> omitted) throws Exception {
        for(var action:sub.path("actions")) {
            String id=action.path("id").asText();if(results.containsKey(id)||omitted.contains(id))continue;
            JsonNode result=execute(action);
            if(result!=null && (action.path("kind").asText().equals("invoke") && !result.at("/response/outcome").asText().equals("APPLIED")
                || result.path("httpStatus").asInt(200)>=400)) return id;
            if(id.equals(stop)) return id;
        }
        return null;
    }
    String through(String stop) throws Exception { return through(stop,Set.of()); }
    Set<String> sources() {
        Set<String> ids=new HashSet<>();for(var a:sub.path("actions"))if(a.path("id").asText().startsWith(PREFIX))ids.add(a.path("id").asText());return ids;
    }
    JsonNode reserve() throws Exception {
        // Execute the declared public selector even when a bounded test calls
        // reserve directly instead of walking the complete action sequence.
        for(var action:sub.path("actions")) {
            String id=action.path("id").asText();
            if(id.equals("t09-sale-source-reserve60-current-segment")&&!results.containsKey(id))execute(action);
        }
        String before=physicalState();
        var r=call("reserveQuantity",request(reserveId()),"warehouse");results.put(reserveId(),r);
        if(!"APPLIED".equals(r.at("/response/outcome").asText())&&!before.equals(physicalState()))
            throw new IllegalStateException("Rejected reservation changed physical ledger/allocation rows");
        return r;
    }
    String physicalState() throws Exception {
        var result=new ArrayList<String>();
        for(String table:List.of("mulino_inventory_QuantitySegments","mulino_inventory_QuantityMovements","mulino_inventory_SegmentAllocations")) {
            var sorted=new ArrayList<String>();for(var row:rows(table))sorted.add(new TreeMap<>(row).toString());Collections.sort(sorted);result.add(table+sorted);
        }
        return result.toString();
    }
    List<Map<String,Object>> rows(String table) throws Exception {
        try(var db=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());
            var s=db.prepareStatement("SELECT * FROM "+table+" WHERE organizationId=?")) {
            s.setString(1,aliases.path("ORG").asText());try(var rs=s.executeQuery()) {
                List<Map<String,Object>> out=new ArrayList<>();while(rs.next()) {
                    var row=new LinkedHashMap<String,Object>();for(int i=1;i<=rs.getMetaData().getColumnCount();i++)row.put(rs.getMetaData().getColumnLabel(i),rs.getObject(i));out.add(row);
                }return out;
            }
        }
    }
    private static void referencedActions(JsonNode node, Set<String> names) {
        if(node.isObject()&&node.path("actionId").isTextual())names.add(node.path("actionId").asText());
        if(node.isContainerNode())for(var child:node)referencedActions(child,names);
    }
    JsonNode oracleReport() throws Exception {
        var report=Json.object().put("recordType","T09_BOUNDED_ORACLE_OBSERVATIONS")
            .put("fixtureSubcase",sub.path("id").asText()).put("fullScenarioRunnerUsed",false)
            .put("caseSha256",Json.sha256(root.resolve("verification/cases/T09/case.json")));
        var rows=Json.array();
        for(var scenario:Json.read(root.resolve("verification/cases/T09/case.json")).path("subcases")) {
            boolean selected=scenario.path("id").equals(sub.path("id"));
            for(var assertion:scenario.path("assertions")) {
                var row=Json.object().put("subcase",scenario.path("id").asText()).put("id",assertion.path("id").asText());
                var dependencies=new TreeSet<String>();referencedActions(assertion,dependencies);
                if(!selected)row.put("status","NOT_RUN").put("reason","Different isolated fixture; outside this probe instance");
                else {
                    dependencies.removeAll(results.keySet());
                    if(!dependencies.isEmpty()) {
                        row.put("status","NOT_RUN").put("reason","Required actions not reached");row.set("unreachedActions",Json.MAPPER.valueToTree(dependencies));
                    } else try {
                        var engine=new AssertionEngine();engine.check(assertion,results,aliases);row.put("status","PASS");
                    } catch(AssertionError|IllegalArgumentException failed) {row.put("status","FAIL").put("reason",failed.getMessage());}
                }
                rows.add(row);
            }
        }
        report.set("assertions",rows);return report;
    }
    void save(Path path) throws Exception {
        var data=Json.object();data.set("httpReceipts",Json.MAPPER.valueToTree(httpReceipts));
        data.set("commands",Json.MAPPER.valueToTree(results));data.set("installer",installed);
        data.set("inventoryScopeAudits",Json.MAPPER.valueToTree(inventoryScopeAudits));
        var sql=Json.object();
        for(String table:List.of("mulino_inventory_QuantitySegments","mulino_inventory_ManufacturingLots",
            "mulino_inventory_DispositionBases","mulino_inventory_SegmentAllocations","mulino_inventory_QuantityMovements",
            "mulino_inventory_Dispatches","mulino_inventory_CargoScopes","mulino_governance_PolicyVersions",
            "mulino_quality_Decisions","mulino_evidence_DocumentVersions","mulino_evidence_Events",
            "mulino_evidence_Claims","mulino_evidence_SourceProfiles","mulino_evidence_Reconciliations","mulino_evidence_Verifications",
            "mulino_evidence_CanonicalOccurrences","mulino_trade_regulatory_Policies",
            "mulino_trade_regulatory_Procedures","mulino_trade_regulatory_ProcedureVersions",
            "mulino_trade_regulatory_DecisionVersions","mulino_identity_Grants","mulino_identity_CapabilityAssignments")) {
            var values=Json.array();for(var row:rows(table)) {var v=Json.object();row.forEach((k,x)->{if(x==null)v.putNull(k);else v.put(k,x.toString());});values.add(v);}sql.set(table,values);
        }
        data.set("rawRows",sql);Json.write(path,data);
        Json.write(path.resolveSibling(path.getFileName().toString().replace(".json","-oracles.json")),oracleReport());
    }
}
