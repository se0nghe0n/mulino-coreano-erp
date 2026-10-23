package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.nio.file.*;
import java.net.http.*;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

/** Genuine Spring HTTP command gateway + PostgreSQL; no scenario runner or business oracle edits. */
@EnabledIfEnvironmentVariable(named="T17_PG_TEST",matches="true")
class T17SourceFactsPostgresTest {
    final Path root=Path.of(System.getProperty("repo.root"));
    ActualConfiguration config;ObjectNode fixture,aliases,installed;T17SourceFacts sources;String org;
    Connection connection;String testName;
    @BeforeEach void install(TestInfo info)throws Exception {
        testName=info.getTestMethod().orElseThrow().getName();
        config=ActualConfiguration.environment(System.getenv());
        fixture=(ObjectNode)Json.read(root.resolve("verification/cases/T17/fixtures/normal-consumed-delivery.json"));
        if(info.getTestMethod().orElseThrow().getName().equals("unknownConditionRemainsOmittedWithoutInventedPermission"))((ObjectNode)fixture.path("baseline").path("conditions").get(0)).put("status","UNKNOWN");
        var bundle=Json.object().put("fixtureHash",Json.sha256(root.resolve("verification/cases/T17/fixtures/normal-consumed-delivery.json")));bundle.set("fixture",fixture);
        bundle=(ObjectNode)ActualFixtureBindings.bind(root,bundle,config);
        fixture=(ObjectNode)bundle.path("fixture");
        installed=ScenarioFixtureInstaller.install(config,root,bundle,true);aliases=(ObjectNode)installed.path("aliasMap");org=aliases.path("ORG").asText();
        sources=new T17SourceFacts(config,fixture,aliases,installed.path("organizationExternalAlias").asText());
        connection=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());
        Path artifacts=Path.of(System.getenv("T17_TEST_EVIDENCE"));Files.createDirectories(artifacts);Files.writeString(artifacts.resolve(testName+"-installer-source-results.json"),Json.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(installed));
    }
    @AfterEach void close()throws Exception {if(sources!=null)Files.writeString(Path.of(System.getenv("T17_TEST_EVIDENCE")).resolve(testName+"-negative-command-results.json"),sources.commandEvidence().toPrettyString());if(connection!=null)connection.close();}
    int count(String table)throws Exception {try(var s=connection.prepareStatement("SELECT count(*) FROM "+table+" WHERE organizationId=?")){s.setString(1,org);try(var r=s.executeQuery()){r.next();return r.getInt(1);}}}
    String id(String alias){return aliases.path(alias).asText();}
    void sql(String sql,Object...args)throws Exception {try(var s=connection.prepareStatement(sql)){for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);s.executeUpdate();}}
    ObjectNode permission(String evidence)throws Exception {
        var receipt=installed.path("sourceFactEvidence").path("conditions").get(0);
        // Use the actual immutable source decision, not an assertion expected value.
        try(var s=connection.prepareStatement("SELECT e.payload FROM mulino_evidence_Events e WHERE e.organizationId=? AND e.ID=?")){s.setString(1,org);s.setString(2,receipt.path("eventId").asText());try(var r=s.executeQuery()){r.next();var p=(ObjectNode)Json.parse(r.getString(1));p.remove(List.of("operation","sourceDecisionId","sourceVersion","givenEvidenceAlias"));return p.put("evidenceId",evidence).put("reason","T17 scoped negative").put("nextCheckAt","2026-10-07T10:00:00Z");}}
    }
    JsonNode queryEligibility()throws Exception {
        var credential=(ObjectNode)fixture.path("actors").path("observer").deepCopy();credential.put("organizationAlias",installed.path("organizationExternalAlias").asText());
        var body=Json.object().put("id",id("A")).put("asOf","2026-10-07T09:00:00Z").put("knownAt","2026-10-07T09:00:01Z");body.set("filters",Json.object().put("action","SELL"));body.set("scope",Json.object().put("itemId",id("P")));
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(config.baseUri().resolve("/api/ontology/queries/evaluateEligibility")).header("Content-Type","application/json").header("Authorization","Bearer "+new JwtSigner(config).sign(credential)).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode(),response.body());return Json.parse(response.body());
    }
    @Test void sourceChainsUseRealCurrentAuthorityAndKeepUnknownExpiryFailClosed()throws Exception {
        var evidence=installed.path("sourceFactEvidence");assertEquals(5,evidence.path("conditions").size(),evidence.toPrettyString());assertTrue(evidence.path("uninstalledConditions").isEmpty());
        assertEquals(3,count("mulino_inventory_DispositionBases"));assertEquals(1,count("mulino_trade_regulatory_DecisionVersions"));assertEquals(1,count("mulino_trade_regulatory_LabelVerifications"));assertEquals(8,count("mulino_evidence_Verifications"));
        try(var s=connection.prepareStatement("SELECT count(*) FROM mulino_inventory_DispositionBases b JOIN mulino_commands_CommandRecords c ON c.organizationId=b.organizationId AND c.ID=b.commandId WHERE b.organizationId=? AND c.capabilityId='recordDispositionBasis'")){s.setString(1,org);try(var r=s.executeQuery()){r.next();assertEquals(3,r.getInt(1));}}
        for(JsonNode receipt:evidence.path("conditions"))assertTrue(receipt.hasNonNull("canonicalOccurrenceId")&&receipt.hasNonNull("typedDecisionId"));
        try(var s=connection.prepareStatement("SELECT sum(quantity) FROM mulino_inventory_QuantitySegments WHERE organizationId=?")){s.setString(1,org);try(var r=s.executeQuery()){r.next();assertEquals(0,new BigDecimal("30").compareTo(r.getBigDecimal(1)));}}
        assertEquals(1,count("mulino_inventory_QuantityMovements"));
        assertEquals(0,evidence.path("seededPermissionRows").asInt());assertEquals(0,evidence.path("seededVerificationRows").asInt());
        assertTrue(installed.path("omittedFacts").toString().contains("baseline.sourceProvenance"));
        var unverified=sources.request("recordDispositionBasis",permission(id("qc-proof")),1);assertEquals("EVIDENCE_UNVERIFIED",unverified.path("error").path("code").asText(),unverified.toPrettyString());assertEquals(3,count("mulino_inventory_DispositionBases"));
        var first=evidence.path("conditions").get(0);String doc=first.path("documentId").asText();
        String grant;try(var s=connection.prepareStatement("SELECT ID FROM mulino_identity_Grants WHERE organizationId=? AND actorId=?")){s.setString(1,org);s.setString(2,id("t17Source"));try(var r=s.executeQuery()){assertTrue(r.next());grant=r.getString(1);}}
        // Additional grant dimensions must all match; an ITEM match cannot bypass wrong WORK/PLACE.
        for(String kind:List.of("WORK","PLACE")){
            sql("INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) VALUES(?,?,?,?)",org,grant,kind,UUID.randomUUID().toString());
            byte[] bytes="{\"purpose\":\"all-of scope negative\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            var upload=Json.object().put("sourceNamespace","synthetic-t17-eligibility").put("sourceReference",UUID.randomUUID().toString()).put("mediaType","application/json").put("expectedHash",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))).put("provenance","SYNTHETIC").put("contentBase64",Base64.getEncoder().encodeToString(bytes));upload.set("subject",Json.object().put("kind","SEGMENT").put("id",id("A")));
            var denied=sources.request("attachEvidence",upload,null);assertEquals("FORBIDDEN",denied.path("error").path("code").asText(),denied.toPrettyString());
            var blockedDecision=sources.request("recordDispositionBasis",permission(doc),1);assertNotEquals("APPLIED",blockedDecision.path("outcome").asText(),blockedDecision.toPrettyString());
            sql("DELETE FROM mulino_identity_GrantScopes WHERE organizationId=? AND grantId=? AND scopeKind=?",org,grant,kind);
        }
        assertEquals(3,count("mulino_inventory_DispositionBases"));
        var wrong=sources.request("matchSourceIdentity",Json.object().put("claimId",claim(first.path("eventId").asText())).put("basisDocumentId",doc).put("physicalScopeId",UUID.randomUUID().toString()).put("policyVersion","t17-synthetic-v1").put("sourceIdentity","forged").put("quantity","30").put("unit","BOX").put("effectiveFrom","2026-10-01T00:00:00Z").put("reason","Wrong scope negative"),1);
        assertNotEquals("APPLIED",wrong.path("outcome").asText(),wrong.toPrettyString());
        var forged=permission(doc);forged.put("quantity","29");var rejected=sources.request("recordDispositionBasis",forged,1);assertEquals("EVIDENCE_UNVERIFIED",rejected.path("error").path("code").asText(),rejected.toPrettyString());
        assertEquals(3,count("mulino_inventory_DispositionBases"));assertEquals(8,count("mulino_evidence_Verifications"));
        var query=queryEligibility();assertTrue(query.path("unknowns").toString().contains("LOT_EXPIRY_UNKNOWN"),query.toPrettyString());assertEquals("0",query.path("data").path("eligibleQuantity").asText(),query.toPrettyString());
        Files.writeString(Path.of(System.getenv("T17_TEST_EVIDENCE")).resolve("expiry-unknown-query.json"),query.toPrettyString());
    }
    @Test void unknownConditionRemainsOmittedWithoutInventedPermission()throws Exception {
        var result=installed.path("sourceFactEvidence");assertEquals(4,result.path("conditions").size());assertEquals(1,result.path("uninstalledConditions").size());
        assertTrue(result.path("uninstalledConditions").get(0).path("reason").asText().contains("UNKNOWN"));assertTrue(installed.path("omittedFacts").toString().contains("baseline.conditions"));assertEquals(2,count("mulino_inventory_DispositionBases"));
        try(var s=connection.prepareStatement("SELECT count(*) FROM mulino_inventory_DispositionBases WHERE organizationId=? AND category='QC'")){s.setString(1,org);try(var r=s.executeQuery()){r.next();assertEquals(0,r.getInt(1));}}
    }
    String claim(String event)throws Exception {try(var s=connection.prepareStatement("SELECT ID FROM mulino_evidence_Claims WHERE organizationId=? AND eventId=?")){s.setString(1,org);s.setString(2,event);try(var r=s.executeQuery()){r.next();return r.getString(1);}}}
}
