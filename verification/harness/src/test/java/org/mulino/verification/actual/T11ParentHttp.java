package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import org.mulino.verification.Json;
import org.mulino.verification.ReferenceResolver;

/** T11-only HTTP execution and independent SQL evidence; no scenario runner or request adaptation. */
final class T11ParentHttp implements AutoCloseable {
    final Path root=Path.of(System.getProperty("repo.root"));
    final ActualConfiguration config=ActualConfiguration.environment(System.getenv());
    final ObjectNode fixture,installed,aliases,actions=Json.object();
    final JsonNode subcase;
    final com.fasterxml.jackson.databind.node.ArrayNode httpCalls=Json.array();
    final Map<String,JsonNode> results=new LinkedHashMap<>();
    final Connection db;
    final String org;
    final Path evidence;
    final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    final JwtSigner signer=new JwtSigner(config);

    T11ParentHttp(String name,String test) throws Exception {this(name,test,fixture->{});}
    T11ParentHttp(String name,String test,java.util.function.Consumer<ObjectNode> diagnosticFixture) throws Exception {
        evidence=Path.of(System.getenv("T11_TEST_EVIDENCE")).resolve(test+"-"+name);Files.createDirectories(evidence);
        var c=Json.read(root.resolve("verification/cases/T11/case.json"));
        subcase=java.util.stream.StreamSupport.stream(c.path("subcases").spliterator(),false).filter(s->s.path("id").asText().equals(name)).findFirst().orElseThrow();
        fixture=(ObjectNode)Json.read(root.resolve(subcase.path("fixtureRef").asText()));
        var originalFixture=fixture.deepCopy();diagnosticFixture.accept(fixture);
        String fixtureHash=Json.sha256(root.resolve(subcase.path("fixtureRef").asText()));
        if(!originalFixture.equals(fixture)){Json.write(evidence.resolve("NEW-synthetic-fixture.json"),fixture);String originalHash=fixtureHash;fixtureHash=Json.sha256(evidence.resolve("NEW-synthetic-fixture.json"));Json.write(evidence.resolve("fixture-provenance.json"),Json.object().put("originalSha256",originalHash).put("diagnosticSha256",fixtureHash).put("originalFileUnchanged",true));}
        var bundle=Json.object().put("fixtureHash",fixtureHash);bundle.set("fixture",fixture);
        var bound=ActualFixtureBindings.bind(root,bundle,config);
        installed=ScenarioFixtureInstaller.install(config,root,bound,true);aliases=(ObjectNode)installed.path("aliasMap");org=aliases.path("ORG").asText();
        db=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());
        Json.write(evidence.resolve("installation.json"),installed);
        send("fixture-start","A","/__verification/runtime/clock/fixture-start",Json.object().put("instant",fixture.path("clock").path("asOf").asText()));
    }
    JsonNode action(String id) {for(var a:subcase.path("actions"))if(id.equals(a.path("id").asText()))return a;throw new IllegalArgumentException(id);}
    JsonNode execute(String id) throws Exception {
        var a=action(id);var request=new ReferenceResolver(results,aliases).resolve(a.path("request"));
        String category="query".equals(a.path("kind").asText())?"queries":"RECORD".equals(request.path("intentKind").asText())?"records":"commands";
        return send(id,a.path("actorRef").asText(),"/api/ontology/"+category+"/"+a.path("capabilityId").asText(),request);
    }
    JsonNode send(String id,String actor,String path,JsonNode request) throws Exception {
        var credential=(ObjectNode)ActualFixtureBindings.credentialActor(root,config,fixture.path("actors").path(actor));
        credential.put("organizationAlias",installed.path("organizationExternalAlias").asText());
        var response=http.send(HttpRequest.newBuilder(config.baseUri().resolve(path)).timeout(Duration.ofSeconds(30))
            .header("Content-Type","application/json").header("Authorization","Bearer "+signer.sign(credential))
            .POST(HttpRequest.BodyPublishers.ofString(request.toString())).build(),HttpResponse.BodyHandlers.ofString());
        JsonNode body=Json.parse(response.body());var result=Json.object().put("httpStatus",response.statusCode()).put("driverStatus","EXECUTED");result.set("provenance",Json.object().put("source","ACTUAL_HTTP").put("scopeComplete",true).put("independent",false));result.set("request",request);result.set("response",body);
        httpCalls.add(result.deepCopy().put("actionId",id));Json.write(evidence.resolve("http-calls.json"),httpCalls);
        actions.set(id,result);results.put(id,result);Json.write(evidence.resolve("http.json"),actions);return body;
    }
    void through(String last) throws Exception {
        for(var a:subcase.path("actions")) {
            String id=a.path("id").asText();
            if(Set.of("invoke","query").contains(a.path("kind").asText()))execute(id);
            if(id.equals(last))return;
        }
        throw new IllegalArgumentException(last);
    }
    void allActions() throws Exception {
        for(var a:subcase.path("actions"))if(Set.of("invoke","query").contains(a.path("kind").asText())) {
            try {execute(a.path("id").asText());}
            catch(IllegalArgumentException | AssertionError failure) {actions.set(a.path("id").asText(),Json.object().put("NOT_RUN",failure.getMessage()));}
        }
        Json.write(evidence.resolve("http.json"),actions);
    }
    List<Map<String,Object>> rows(String table) throws Exception {
        var result=new ArrayList<Map<String,Object>>();
        try(var s=db.prepareStatement("SELECT * FROM "+table+" WHERE organizationId=? ORDER BY ID")) {
            s.setString(1,org);try(var r=s.executeQuery()){while(r.next()){var row=new LinkedHashMap<String,Object>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)row.put(r.getMetaData().getColumnLabel(i),r.getObject(i));result.add(row);}}
        }
        return result;
    }
    void dump() throws Exception {
        var out=Json.object();db.setAutoCommit(false);db.setReadOnly(true);db.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        out.put("snapshot",ObserverSnapshot.token(db));out.put("readOnly",db.isReadOnly());
        for(String table:List.of("mulino_work_read_Works","mulino_work_WorkLinks","mulino_work_WorkContributions","mulino_work_read_AssessmentReferences","mulino_work_read_ObligationReferences","mulino_inventory_QuantityMovements","mulino_trade_receipt_Receipts","mulino_evidence_CanonicalOccurrences")) {
            var entries=Json.array();String sql="SELECT row_to_json(r)::text FROM "+table+" r WHERE organizationId=? ORDER BY ID";
            try(var s=db.prepareStatement(sql)){s.setString(1,org);try(var r=s.executeQuery()){while(r.next())entries.add(Json.parse(r.getString(1)));}}
            var proof=Json.object().put("sql",sql);proof.set("boundParameters",Json.array().add(org));proof.set("rows",entries);out.set(table,proof);
        }
        db.rollback();db.setReadOnly(false);db.setAutoCommit(true);Json.write(evidence.resolve("independent-db.json"),out);
    }
    @Override public void close() throws Exception {try{dump();}finally{db.close();}}
}
