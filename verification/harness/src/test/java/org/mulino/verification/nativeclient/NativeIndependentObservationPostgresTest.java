package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.*;
import org.mulino.verification.actual.*;
import org.mulino.verification.modelbinding.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Bounded real HTTP/PostgreSQL proofs. Does not run a model, scenario runner, or acceptance profile. */
@EnabledIfEnvironmentVariable(named="INDEPENDENT_OBSERVER_ACTUAL",matches="true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NativeIndependentObservationPostgresTest {
    static final Path ROOT=Path.of(System.getProperty("repo.root"));
    static ActualConfiguration config;static JsonNode aliases,actor,scope;static NativeIntegrationEvidence evidence;static NativeMcpTransport transport;static NativePhysicalObserver observer;
    static final String NOW="2026-10-07T09:00:00Z";
    static ObjectNode parameters(){var p=Json.object().put("authorityAt",NOW);p.set("scope",scope);p.set("businessScope",Json.object().put("itemId",aliases.path("P").asText()));p.set("bindings",Json.object());return p;}
    @BeforeAll static void setup()throws Exception {
        config=ActualConfiguration.environment(System.getenv());var fixture=(ObjectNode)Json.read(ROOT.resolve("verification/actual/s2/fixture.json"));((ObjectNode)fixture.path("clock")).put("knownAt",NOW);((ObjectNode)fixture.path("aliases").path("W")).put("kind","INTERNAL_STORAGE");
        var bundle=Json.object();bundle.set("fixture",fixture);bundle.put("fixtureHash",Json.sha256Text(fixture.toString()));bundle.put("caseHash",Json.sha256Text("independent-focused-probe"));bundle.put("organizationExternalAlias","independent-"+UUID.randomUUID());
        var installed=new FixtureInstaller(config).install(bundle);aliases=installed.path("aliasMap");actor=fixture.path("actors").path("reader").deepCopy();((ObjectNode)actor).put("organizationAlias",bundle.path("organizationExternalAlias").asText());
        scope=Json.object().put("organizationId",aliases.path("ORG").asText()).put("installationId",UUID.randomUUID().toString()).put("caseId","INDEPENDENT-PROBE").put("turnId","turn-1");
        evidence=new NativeIntegrationEvidence(ROOT);transport=new NativeMcpTransport(config,evidence);observer=new NativePhysicalObserver(config,evidence);
        var receipt=evidence.base("DIRECT_HTTP_PG_PROBE_INPUT",scope).put("fixtureSha256",Json.sha256(ROOT.resolve("verification/actual/s2/fixture.json"))).put("backendJarSha256",Json.sha256(ROOT.resolve("backend/target/ontology-0.1.0-SNAPSHOT.jar")));receipt.set("installed",installed);evidence.save("probe-installation",receipt);
    }
    static ObjectNode projection(){var q=Json.object().put("operation","getObject").put("id",aliases.path("W").asText());q.set("scope",Json.object().put("organizationId",scope.path("organizationId").asText()).put("objectType","Place"));return q;}
    static ObjectNode observeRequest(JsonNode response){var q=Json.object().put("asOf",NOW).put("knownAt",NOW).put("snapshotRef",response.path("snapshotRevision").asText()).put("projectionActorId",aliases.path("reader").asText());q.set("scope",scope);q.set("projectionRequest",projection());q.set("observationScope",parameters());return q;}
    static NativeMcpTransport.Capture read(String id)throws Exception {var q=(ObjectNode)projection().deepCopy();q.remove("operation");q.put("asOf",NOW).put("knownAt",NOW);return transport.send(id,"tools/call","getObject",q,actor,scope);}
    @Test @Order(1) void actualReadRevisionAndIndependentRawColumnsAndDerivedOnHandAgree()throws Exception {
        var read=read("turn-1/context");assertEquals(200,read.status());assertTrue(read.businessResponse().has("snapshotRevision"),read.response().toString());
        var result=observer.observe("turn-1/before",observeRequest(read.businessResponse()));assertEquals(StepResult.DriverStatus.EXECUTED,result.driverStatus());
        assertEquals(read.businessResponse().path("snapshotRevision"),result.data().path("snapshotRevision"));assertNotEquals(result.data().path("snapshotRevision"),result.data().path("snapshot").path("id"));
        new ContractValidator(ROOT).schema("contracts/acceptance-observation.schema.json",result.data());ProjectionRevisionObservation.validateArtifact(new ContractValidator(ROOT),result.data(),observeRequest(read.businessResponse()));
        var value=new BindingEvaluator(new BindingContract(ROOT)).value("state.inventory.onHand.value",Json.object(),result.toJson(),List.of());assertEquals("100",value.asText());
        evidence.save("actual-read-observe",result.toJson());
    }
    @Test @Order(2) void immutableCommittedWorkEffectHasActualResponseAuditTransactionAndRowProof()throws Exception {
        var flow=Json.read(ROOT.resolve("verification/actual/s3/flow.json"));var request=(ObjectNode)flow.path("actions").get(0).path("request").deepCopy();
        replace(request,"$reader",aliases.path("reader"));replace(request,"$supervisor",aliases.path("supervisor"));replace(request,"$P",aliases.path("P"));
        var command=transport.send("turn-1/agent","tools/call","createWork",request,actor,scope);assertEquals("APPLIED",command.businessResponse().path("outcome").asText(),command.response().toString());
        var read=read("turn-1/post-read");var result=observer.observe("turn-1/after",observeRequest(read.businessResponse()));
        assertTrue(result.data().path("data").path("effects").size()>=2);int works=0;for(JsonNode effect:result.data().path("data").path("effects")){assertEquals(command.businessResponse().path("commandId"),effect.path("commandId"));if(effect.path("operation").asText().equals("CREATE_WORK"))works++;}assertEquals(1,works);
        evidence.save("actual-command-observe",result.toJson());
    }
    @Test @Order(3) void foreignAndOutOfTurnCommittedWorkIsVisibleButNeverCountedAsOwnEffect()throws Exception {
        var before=observer.capture(scope,NOW,NOW);var otherScope=(ObjectNode)scope.deepCopy();otherScope.put("turnId","foreign-turn");
        var request=(ObjectNode)Json.read(ROOT.resolve("verification/actual/s3/flow.json")).path("actions").get(0).path("request").deepCopy();
        request.put("commandIdempotencyKey","foreign-"+UUID.randomUUID());replace(request,"$reader",aliases.path("reader"));replace(request,"$supervisor",aliases.path("supervisor"));replace(request,"$P",aliases.path("P"));
        var reply=transport.send("foreign-turn/agent","tools/call","createWork",request,actor,otherScope);assertEquals("APPLIED",reply.businessResponse().path("outcome").asText());
        var after=observer.capture(scope,NOW,NOW);Path directory=ROOT.resolve(after.path("snapshot").path("artifactRef").asText()).getParent();var effects=NativeCausalEffects.observe(before,after,NativeCausalEffects.wires(directory));
        assertTrue(effects.path("complete").asBoolean());assertTrue(effects.path("rows").isEmpty());assertFalse(effects.path("unattributed").isEmpty());evidence.save("foreign-command-negative",effects);
    }
    @Test @Order(4) void actualCurrentReadAllOfRejectsMissingItemDimension()throws Exception {
        String grant;try(Connection c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());var s=c.prepareStatement("SELECT ID FROM mulino_identity_Grants WHERE organizationId=? AND actorId=?")){s.setString(1,scope.path("organizationId").asText());s.setString(2,aliases.path("reader").asText());try(var result=s.executeQuery()){assertTrue(result.next());grant=result.getString(1);}}
        try(Connection c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())){
            try(var s=c.prepareStatement("INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) VALUES(?,?,'ITEM',?)")){s.setString(1,scope.path("organizationId").asText());s.setString(2,grant);s.setString(3,aliases.path("P").asText());s.executeUpdate();}
            try{var reply=read("all-of-negative");assertEquals("FORBIDDEN",reply.businessResponse().path("error").path("code").asText(),reply.response().toString());var capture=observer.capture(scope,NOW,NOW);var params=parameters();var rows=new PhysicalRecipeRows(capture,params);assertFalse(PhysicalAuthorization.allowed(rows,aliases.path("reader").asText(),"getObject",Map.of("TARGET",Set.of(aliases.path("W").asText()),"PLACE",Set.of(aliases.path("W").asText())),new HashSet<>()));evidence.save("all-of-current-read-negative",capture);}
            finally{try(var s=c.prepareStatement("DELETE FROM mulino_identity_GrantScopes WHERE organizationId=? AND grantId=? AND scopeKind='ITEM'")){s.setString(1,scope.path("organizationId").asText());s.setString(2,grant);s.executeUpdate();}}
        }
    }
    @Test @Order(5) void missingChangedColumnsAndUnknownTablesFailClosed()throws Exception {
        try(Connection c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())){
            execute(c,"ALTER TABLE mulino_inventory_Places RENAME COLUMN name TO changed_name");try{assertThrows(Exception.class,()->observer.capture(scope,NOW,NOW));}finally{execute(c,"ALTER TABLE mulino_inventory_Places RENAME COLUMN changed_name TO name");}
            execute(c,"ALTER TABLE mulino_inventory_Places ALTER COLUMN name TYPE text");try{assertThrows(Exception.class,()->observer.capture(scope,NOW,NOW));}finally{execute(c,"ALTER TABLE mulino_inventory_Places ALTER COLUMN name TYPE varchar(200)");}
            execute(c,"ALTER TABLE mulino_inventory_QuantitySegments ALTER COLUMN quantity TYPE numeric(38,11)");try{assertThrows(Exception.class,()->observer.capture(scope,NOW,NOW));}finally{execute(c,"ALTER TABLE mulino_inventory_QuantitySegments ALTER COLUMN quantity TYPE numeric(38,12)");}
            execute(c,"CREATE TABLE mulino_unknown_effect (organizationId varchar(36),ID varchar(36))");try{assertThrows(Exception.class,()->observer.capture(scope,NOW,NOW));}finally{execute(c,"DROP TABLE mulino_unknown_effect");}
        }
    }
    @Test @Order(6) void nonemptyInventoryAndWorkRevisionsAreRecomputedFromOnlyTheirVisibleBusinessWorld()throws Exception {
        for(String operation:List.of("getInventory","getWork","getObject")){
            var query=Json.object().put("asOf",NOW).put("knownAt",NOW);var queryScope=Json.object().put("organizationId",scope.path("organizationId").asText());query.set("scope",queryScope);
            if(operation.equals("getInventory"))queryScope.set("itemId",aliases.path("P"));
            if(operation.equals("getObject")){query.put("id",aliases.path("P").asText());queryScope.put("objectType","TradeItem");}
            if(operation.equals("getWork")){var captured=observer.capture(scope,NOW,NOW);query.put("id",captured.path("rawRows").path("mulino_work_read_Works").get(0).path("ID").asText());}
            var wire=transport.send("business-world-"+operation,"tools/call",operation,query,actor,scope);assertTrue(wire.businessResponse().has("snapshotRevision"),wire.response().toString());
            var capture=observer.capture(scope,NOW,NOW);var projection=query.deepCopy();projection.remove(List.of("asOf","knownAt"));projection.put("operation",operation);
            var proof=Json.object().put("recipe",BusinessProjectionRecipes.ID).put("version",BusinessProjectionRecipes.VERSION).put("actorId",aliases.path("reader").asText());proof.set("request",projection);proof.set("parameters",parameters());proof.set("documents",capture.path("sourceDocuments"));
            var computed=BusinessProjectionRecipes.compute(capture,proof);var diagnostic=evidence.base("BUSINESS_REVISION_DIRECT_HTTP_PG_DIAGNOSTIC",scope);diagnostic.set("request",query);diagnostic.set("response",wire.businessResponse());diagnostic.set("recomputedWorld",computed.world());diagnostic.set("rawRows",capture.path("rawRows"));evidence.save("business-world-"+operation,diagnostic);
            assertEquals(wire.businessResponse().path("snapshotRevision").asText(),computed.revision(),operation+" direct HTTP/SQL world differs");
            var independent=NativeProjectionObserver.prove(capture,projection,aliases.path("reader").asText(),parameters(),evidence);assertEquals(computed.revision(),independent.path("revision").asText());
        }
    }
    @Test @Order(7) void actualProvisionalConfirmedDuplicateAndFinalDeliveryUseIndependentSourcesAndVisibleWorld()throws Exception {
        var flow=new NativeSourceFlowProbe(ROOT,config);var inputs=new LinkedHashSet<String>(List.of("create-work","proposal100","approve100","dispatch100","RANGE60","RANGE40","receipt60","receipt60-provisional","receipt60-match","receipt60-link","receipt60-confirm","sourceB60","sourceB60-provisional","sourceB60-match","sourceB60-link","sourceB60-confirm","receipt40","receipt40-provisional","receipt40-match","receipt40-link","receipt40-confirm","assess-arrival","reg-draft","reg-prepare","reg-submitted","reg-submitted-match","reg-submitted-link","submit-reg","reg-allow30","reg-allow30-match","reg-allow30-link","allow30","reg-label","reg-label-match","reg-label-link","label-verified","s60-QC","s60-QC-match","s60-QC-link","s60-QC-decision","s60-CUSTOMER","s60-CUSTOMER-match","s60-CUSTOMER-link","s60-CUSTOMER-decision","s60-COMMERCIAL","s60-COMMERCIAL-match","s60-COMMERCIAL-link","s60-COMMERCIAL-decision"));
        flow.selected("verification/actual/s4/e1-upstream.json",inputs);
        var capture=flow.capture();var parameters=flow.parameters();var receipt=PhysicalFactRecipes.compute("state.receipt.cumulative.value",capture,parameters);assertEquals(new java.math.BigDecimal("100"),receipt.facts().stream().map(f->new java.math.BigDecimal(f.value().asText())).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add));assertEquals(2,capture.path("rawRows").path("mulino_trade_receipt_Receipts").size());
        sourceCounterexamples(flow,capture);assertTrue(PhysicalFactRecipes.compute("state.purchaseProposal.approvalValid",capture,parameters).facts().getFirst().value().asBoolean());for(String path:List.of("state.purchaseProposal.dueInclusive","state.purchaseProposal.qcNotArrivalPrecondition")){var missing=assertThrows(IllegalArgumentException.class,()->PhysicalFactRecipes.compute(path,capture,parameters));assertEquals("RECIPE_OPERAND_NULL:goalVersionId",missing.getMessage());}
        var expired=capture.deepCopy();((ObjectNode)expired.path("rawRows").path("mulino_trade_purchase_Approvals").get(0)).put("validUntil",flow.now);assertFalse(PhysicalFactRecipes.compute("state.purchaseProposal.approvalValid",expired,parameters).facts().getFirst().value().asBoolean());
        var conditional=capture.deepCopy();((ObjectNode)conditional.path("rawRows").path("mulino_trade_purchase_Approvals").get(0)).put("decision","CONDITIONAL");assertThrows(IllegalArgumentException.class,()->PhysicalFactRecipes.compute("state.purchaseProposal.approvalValid",conditional,parameters));
        NativePhysicalFacts.map(ROOT,capture,parameters);var independentResult=Json.object().set("data",capture);var evaluator=new BindingEvaluator(new BindingContract(ROOT));assertEquals("100",evaluator.value("state.receipt.cumulative.value",Json.object(),independentResult,List.of()).asText());assertEquals(2,evaluator.value("state.receipt.distinctOccurrenceCount",Json.object(),independentResult,List.of()).asInt());
        var readScope=Json.object().put("organizationId",flow.bindings.path("ORG").asText()).put("itemId",flow.bindings.path("P").asText());flow.revision("getInventory",null,readScope);flow.revision("getWork",flow.bindings.path("WORK").asText(),Json.object().put("organizationId",flow.bindings.path("ORG").asText()));
        var dispatchInputs=new LinkedHashSet<String>();for(JsonNode a:Json.read(ROOT.resolve("verification/actual/s4/e1-dispatch-authority.json")).path("actions"))dispatchInputs.add(a.path("id").asText());flow.selected("verification/actual/s4/e1-dispatch-authority.json",dispatchInputs);
        flow.until("verification/actual/s4/e1-sales-return.json","e1-delivery-confirm");
        var delivered=PhysicalFactRecipes.compute("state.delivery.cumulative.value",flow.capture(),flow.parameters());assertEquals("30",delivered.facts().getFirst().value().asText());flow.revision("getInventory",null,readScope);flow.revision("getWork",flow.bindings.path("WORK").asText(),Json.object().put("organizationId",flow.bindings.path("ORG").asText()));flow.intakeHistoryProof();
    }

    static void sourceCounterexamples(NativeSourceFlowProbe flow,ObjectNode captured)throws Exception {
        var parameters=flow.parameters();
        for(String mutation:List.of("missing-bytes","changed-bytes","unverified","duplicate-physical","incomplete-source","foreign-scope")) {
            var changed=captured.deepCopy();
            switch(mutation) {
                case "missing-bytes" -> changed.set("sourceDocuments",Json.array());
                case "changed-bytes" -> {String occurrence=changed.path("rawRows").path("mulino_trade_receipt_Receipts").get(0).path("canonicalOccurrenceId").asText();var documents=new HashSet<String>();for(JsonNode verification:changed.path("rawRows").path("mulino_evidence_Verifications"))if(verification.path("canonicalOccurrenceId").asText().equals(occurrence))documents.add(verification.path("basisDocumentId").asText());assertFalse(documents.isEmpty());int targeted=0;for(JsonNode witness:changed.path("sourceDocuments"))if(documents.contains(witness.path("documentId").asText())){((ObjectNode)witness).put("bytesBase64",Base64.getEncoder().encodeToString("changed".getBytes(java.nio.charset.StandardCharsets.UTF_8)));targeted++;}assertEquals(documents.size(),targeted);}
                case "unverified" -> {for(JsonNode row:changed.path("rawRows").path("mulino_evidence_Verifications"))((ObjectNode)row).put("quantityMatched",false);}
                case "duplicate-physical" -> ((ArrayNode)changed.path("rawRows").path("mulino_trade_receipt_Receipts")).add(changed.path("rawRows").path("mulino_trade_receipt_Receipts").get(0).deepCopy());
                case "incomplete-source" -> ((ObjectNode)changed.path("sourceEvidence").path("mulino_evidence_Verifications")).put("complete",false);
                case "foreign-scope" -> ((ObjectNode)changed.path("rawRows").path("mulino_trade_receipt_Receipts").get(0)).put("organizationId",UUID.randomUUID().toString());
            }
            assertThrows(IllegalArgumentException.class,()->PhysicalFactRecipes.compute("state.receipt.cumulative.value",changed,parameters),mutation);
        }
        var past=NativePhysicalFacts.obligations(captured.path("rawRows"),flow.now,flow.now,"2026-10-07T09:00:03Z");assertFalse(past.path("complete").asBoolean());
        flow.evidence.save("source-and-history-negative-results",Json.object().put("mutationsRejected",6).put("pastObligationHistoryRejected",true));
    }
    @Test @Order(8) void concurrentForeignCommandAndReplayNeverAcquireOwnTurnEffects()throws Exception {
        var own=(ObjectNode)scope.deepCopy();own.put("turnId","concurrent-own");var foreign=(ObjectNode)scope.deepCopy();foreign.put("turnId","concurrent-foreign");
        var before=observer.capture(own,NOW,NOW);
        var template=(ObjectNode)Json.read(ROOT.resolve("verification/actual/s3/flow.json")).path("actions").get(0).path("request").deepCopy();replace(template,"$reader",aliases.path("reader"));replace(template,"$supervisor",aliases.path("supervisor"));replace(template,"$P",aliases.path("P"));
        var ownRequest=template.deepCopy();ownRequest.put("commandIdempotencyKey","own-"+UUID.randomUUID());var foreignRequest=template.deepCopy();foreignRequest.put("commandIdempotencyKey","foreign-"+UUID.randomUUID());
        var ownCall=java.util.concurrent.CompletableFuture.supplyAsync(()->{try{return transport.send("concurrent-own/agent","tools/call","createWork",ownRequest,actor,own);}catch(Exception failure){throw new java.util.concurrent.CompletionException(failure);}});
        var foreignCall=java.util.concurrent.CompletableFuture.supplyAsync(()->{try{return transport.send("concurrent-foreign/agent","tools/call","createWork",foreignRequest,actor,foreign);}catch(Exception failure){throw new java.util.concurrent.CompletionException(failure);}});
        var reply=ownCall.get(45,java.util.concurrent.TimeUnit.SECONDS);var other=foreignCall.get(45,java.util.concurrent.TimeUnit.SECONDS);assertEquals("APPLIED",reply.businessResponse().path("outcome").asText());assertEquals("APPLIED",other.businessResponse().path("outcome").asText());
        var after=observer.capture(own,NOW,NOW);var wires=NativeCausalEffects.wires(ROOT.resolve(after.path("snapshot").path("artifactRef").asText()).getParent());var effects=NativeCausalEffects.observe(before,after,wires);assertTrue(effects.path("complete").asBoolean(),effects.toString());int count=0;for(JsonNode effect:effects.path("rows")){assertEquals(reply.businessResponse().path("commandId"),effect.path("commandId"));if(effect.path("operation").asText().equals("CREATE_WORK"))count++;}assertEquals(1,count);assertFalse(effects.path("unattributed").isEmpty());evidence.save("concurrent-foreign-negative",effects);
        var replay=transport.send("concurrent-own/replay","tools/call","createWork",ownRequest,actor,own);assertEquals(reply.businessResponse(),replay.businessResponse());var replayAfter=observer.capture(own,NOW,NOW);var replayEffects=NativeCausalEffects.observe(after,replayAfter,NativeCausalEffects.wires(ROOT.resolve(replayAfter.path("snapshot").path("artifactRef").asText()).getParent()));assertTrue(replayEffects.path("complete").asBoolean());assertTrue(replayEffects.path("rows").isEmpty());evidence.save("replay-zero-negative",replayEffects);
        var incomplete=after.deepCopy();((ObjectNode)incomplete.path("catalogEvidence")).put("complete",false);assertThrows(IllegalStateException.class,()->NativeCausalEffects.observe(before,incomplete,wires));
        var changed=after.deepCopy();((ArrayNode)changed.path("rawRows").path("mulino_work_WorkTransitions")).remove(0);assertFalse(NativeCausalEffects.observe(after,changed,List.of()).path("complete").asBoolean());
    }
    static void execute(Connection c,String sql)throws Exception{try(var s=c.createStatement()){s.execute(sql);}}
    static void replace(JsonNode node,String key,JsonNode value){if(node.isObject())node.fields().forEachRemaining(e->{if(e.getValue().isTextual()&&e.getValue().asText().equals(key))((ObjectNode)node).set(e.getKey(),value);else replace(e.getValue(),key,value);});else if(node.isArray()){for(int i=0;i<node.size();i++){var e=node.get(i);if(e.isTextual()&&e.asText().equals(key))((ArrayNode)node).set(i,value);else replace(e,key,value);}}}
}
