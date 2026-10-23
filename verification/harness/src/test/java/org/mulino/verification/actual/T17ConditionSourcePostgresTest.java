package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Bounded real installer + HTTP + PostgreSQL source contract; never executes the scenario runner. */
@EnabledIfEnvironmentVariable(named="T17_PG_TEST",matches="true")
class T17ConditionSourcePostgresTest {
    final Path root=Path.of(System.getProperty("repo.root"));
    ActualConfiguration config;ObjectNode fixture,installed,aliases;Connection db;String org,sid,name;
    Map<String,JsonNode> results=new HashMap<>();
    @BeforeEach void install(TestInfo info)throws Exception {
        name=info.getTestMethod().orElseThrow().getName();
        sid=name.startsWith("otherpack")?"eligibility-packaging-unapproved":"eligibility-conflict";
        config=ActualConfiguration.environment(System.getenv());
        fixture=(ObjectNode)Json.read(root.resolve("verification/cases/T17/fixtures/"+sid+".json"));
        var bundle=Json.object().put("fixtureHash",hash(fixture.toString().getBytes(StandardCharsets.UTF_8)));bundle.set("fixture",fixture);
        bundle=(ObjectNode)ActualFixtureBindings.bind(root,bundle,config);fixture=(ObjectNode)bundle.path("fixture");
        installed=ScenarioFixtureInstaller.install(config,root,bundle,true);aliases=(ObjectNode)installed.path("aliasMap");org=id("ORG");
        db=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());
        save("setup",installed);
    }
    @AfterEach void close()throws Exception {if(db!=null)db.close();}
    String id(String alias){return Json.required(aliases,alias);}
    static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    void save(String suffix,JsonNode node)throws Exception{Files.writeString(Path.of(System.getenv("T17_TEST_EVIDENCE")).resolve(name+"-"+suffix+".json"),node.toPrettyString());}
    JsonNode gateway(String actor,String path,JsonNode body)throws Exception {
        var credential=(ObjectNode)fixture.path("actors").path(actor).deepCopy();credential.put("organizationAlias",installed.path("organizationExternalAlias").asText());
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(config.baseUri().resolve("/api/ontology/"+path)).timeout(Duration.ofSeconds(60)).header("Content-Type","application/json").header("Authorization","Bearer "+new JwtSigner(config).sign(credential)).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode(),response.body());return Json.parse(response.body());
    }
    JsonNode query(String item)throws Exception {
        var body=Json.object().put("asOf",fixture.path("clock").path("asOf").asText()).put("knownAt",fixture.path("clock").path("knownAt").asText());
        body.set("scope",Json.object().put("organizationId",org).put("itemId",id(item)));body.set("filters",Json.object().put("action","SELL").put("customerId",id("C")));
        var r=gateway("observer","queries/evaluateEligibility",body);save(item+"-query",r);return r.path("data");
    }
    void earlyHold()throws Exception {
        for(var sub:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases"))if(sid.equals(sub.path("id").asText()))for(var a:sub.path("actions"))if(a.path("id").asText().startsWith("early-hold")){
            var request=new ReferenceResolver(results,aliases).resolve(a.path("request"));String cap=a.path("capabilityId").asText();
            var response=gateway(a.path("actorRef").asText(),("RECORD".equals(request.path("intentKind").asText())?"records/":"commands/")+cap,request);
            assertEquals("APPLIED",response.path("outcome").asText(),response.toPrettyString());
            var result=Json.object().put("driverStatus","EXECUTED");result.set("response",response);result.set("provenance",Json.object().put("scopeComplete",true));results.put(a.path("id").asText(),result);save(a.path("id").asText(),result);
        }
    }
    JsonNode row(JsonNode query,String segment,String condition){for(var row:query.path("conditionResults"))if(id(segment).equals(row.path("segmentId").asText())&&condition.equals(row.path("conditionId").asText()))return row;throw new AssertionError(segment+"/"+condition);}
    JsonNode receipt(String segment,String condition){for(var r:installed.path("sourceFactEvidence").path("conditions"))if(segment.equals(r.path("given").path("segmentAlias").asText())&&condition.equals(r.path("given").path("conditionId").asText()))return r;throw new AssertionError(segment+"/"+condition);}
    void assertFixedStatuses(JsonNode query,String item)throws Exception {
        JsonNode sub=null;for(var s:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases"))if(sid.equals(s.path("id").asText()))sub=s;
        assertNotNull(sub);JsonNode assertion=null;for(var a:sub.path("assertions"))if((item.equals("P")?"eligibility-response-1":"condition-source-alt-business-status").equals(a.path("id").asText()))assertion=a;
        assertNotNull(assertion);var result=Json.object().put("driverStatus","EXECUTED");result.set("response",Json.object().set("data",query));result.set("provenance",Json.object().put("scopeComplete",true));
        new AssertionEngine().check(assertion,Map.of(item.equals("P")?"eligibility":"eligibility-alt",result),aliases);
    }
    @Test void conflictUsesTwoVerifiedImmutableHeadsAndUnknownHasNoPositiveSource()throws Exception {
        earlyHold();var data=query("P");assertFixedStatuses(data,"P");
        var conflict=receipt("B","REGULATORY");assertFalse(conflict.has("canonicalOccurrenceId"));assertFalse(conflict.has("documentId"));
        assertEquals("/baseline/conditions/6",conflict.path("pointer").asText());assertEquals(fixture.at("/baseline/conditions/6"),conflict.path("given"));
        var proofs=conflict.path("decisionReceipts");assertEquals(2,proofs.size());var canonical=new TreeSet<String>();var docs=new HashSet<String>();var decisions=new HashSet<String>();
        for(int n=0;n<2;n++){
            var proof=proofs.get(n);String state=n==0?"ALLOWED":"REVOKED",given=n==0?"agency-proof":"claim-proof";
            assertEquals(given,proof.path("givenEvidenceAlias").asText());assertEquals(state,proof.path("decision").asText());
            boolean commandFound=false;
            for(var command:installed.path("sourceFactEvidence").path("commands"))if("recordRegulatoryDecision".equals(command.path("request").path("capabilityId").asText())&&proof.path("typedDecisionId").equals(command.path("response").path("effects").path("versionId"))){assertEquals("APPLIED",command.path("response").path("outcome").asText());commandFound=true;}
            assertTrue(commandFound,"Decision must come from actual HTTP command result");
            canonical.add(proof.path("canonicalOccurrenceId").asText());docs.add(proof.path("documentId").asText());decisions.add(proof.path("typedDecisionId").asText());
            try(var s=db.prepareStatement("SELECT d.decision,d.procedureVersionId,p.currentVersionId,v.status,d.previousVersionId,d.quantity,d.unit,d.validFrom,d.validUntil FROM mulino_trade_regulatory_DecisionVersions d JOIN mulino_trade_regulatory_Procedures p ON p.organizationId=d.organizationId AND p.ID=d.procedureId JOIN mulino_trade_regulatory_ProcedureVersions v ON v.organizationId=d.organizationId AND v.ID=d.procedureVersionId WHERE d.organizationId=? AND d.ID=?")){
                s.setString(1,org);s.setString(2,proof.path("typedDecisionId").asText());try(var r=s.executeQuery()){assertTrue(r.next());assertEquals(state,r.getString(1));assertEquals(r.getString(2),r.getString(3));assertEquals("SUBMITTED",r.getString(4));assertNull(r.getString(5));assertEquals(0,new java.math.BigDecimal("20").compareTo(r.getBigDecimal(6)));assertEquals("BOX",r.getString(7));assertEquals(java.time.Instant.parse(fixture.at("/baseline/conditions/6/validFrom").asText()),r.getTimestamp(8).toInstant());assertEquals(java.time.Instant.parse(fixture.at("/baseline/conditions/6/validUntil").asText()),r.getTimestamp(9).toInstant());assertFalse(r.next());}}
            try(var s=db.prepareStatement("SELECT v.verdict,v.sourceMatched,v.identityMatched,v.quantityMatched,v.timeMatched,v.duplicateChecked,e.payload,doc.blobId,doc.sha256,co.physicalScopeId,co.quantity,co.unit FROM mulino_evidence_Verifications v JOIN mulino_evidence_CanonicalOccurrences co ON co.organizationId=v.organizationId AND co.ID=v.canonicalOccurrenceId JOIN mulino_evidence_Claims cl ON cl.organizationId=v.organizationId AND cl.ID=v.claimId JOIN mulino_evidence_Events e ON e.organizationId=cl.organizationId AND e.ID=cl.eventId JOIN mulino_evidence_DocumentVersions doc ON doc.organizationId=v.organizationId AND doc.ID=v.basisDocumentId WHERE v.organizationId=? AND v.canonicalOccurrenceId=? AND v.basisDocumentId=?")){
                s.setString(1,org);s.setString(2,proof.path("canonicalOccurrenceId").asText());s.setString(3,proof.path("documentId").asText());try(var r=s.executeQuery()){assertTrue(r.next());assertEquals("VERIFIED",r.getString(1));for(int col=2;col<=6;col++)assertTrue(r.getBoolean(col));
                    byte[] bytes=Files.readAllBytes(Path.of(System.getProperty("verification.actual.blobRoot"),"objects",r.getString(8)));assertEquals(r.getString(9),hash(bytes));assertEquals(Json.parse(r.getString(7)),Json.parse(new String(bytes,StandardCharsets.UTF_8)));
                    var original=Json.parse(r.getString(7));assertEquals(state,original.path("decision").asText());assertEquals(given,original.path("givenEvidenceAlias").asText());assertEquals(conflict.path("given"),original.path("originalCondition"));assertEquals(conflict.path("pointer"),original.path("originalConditionPointer"));
                    JsonNode provenance=null;for(var declared:fixture.path("evidence"))if(given.equals(declared.path("alias").asText()))provenance=declared;
                    assertEquals(provenance,original.path("originalEvidenceProvenance"));assertTrue(original.path("synthetic").asBoolean());assertFalse(original.path("operatingApprovalClaimed").asBoolean(true));
                    assertEquals(id("B"),original.path("physicalScopeId").asText());assertEquals(id("B"),r.getString(10));assertEquals(0,new java.math.BigDecimal("20").compareTo(r.getBigDecimal(11)));assertEquals("BOX",r.getString(12));assertFalse(r.next());}}
        }
        assertEquals(2,canonical.size());assertEquals(2,docs.size());assertEquals(2,decisions.size());
        assertEquals(Json.MAPPER.valueToTree(canonical),row(data,"B","REGULATORY").path("evidenceRefs"));assertEquals("CONFLICT",row(data,"B","REGULATORY").path("status").asText());
        assertEquals("ALLOWED",row(data,"B","LABEL").path("status").asText());
        assertTrue(row(data,"U","REGULATORY").path("evidenceRefs").isEmpty());assertEquals("UNKNOWN",row(data,"U","REGULATORY").path("status").asText());
        var missing=installed.path("sourceFactEvidence").path("uninstalledConditions");assertEquals(1,missing.size());assertEquals("/baseline/conditions/16",missing.get(0).path("pointer").asText());assertEquals(fixture.at("/baseline/conditions/16"),missing.get(0).path("given"));
        var qc=row(data,"EARLY","QC");assertEquals("ALLOWED",qc.path("permissionStatus").asText());assertEquals("DENIED",qc.path("restrictionStatus").asText());assertEquals(2,qc.path("evidenceRefs").size());
        assertEquals(0,installed.path("sourceFactEvidence").path("seededPermissionRows").asInt());assertEquals(0,installed.path("sourceFactEvidence").path("seededVerificationRows").asInt());
    }
    @Test void otherpackKeepsAllFiveTuplesInSeparateAltReadAndOriginalOrderDeniesSubstitution()throws Exception {
        earlyHold();var p=query("P");var alt=query("ALT");assertFixedStatuses(p,"P");assertFixedStatuses(alt,"ALT");
        assertEquals(20,p.path("conditionResults").size());assertEquals(5,alt.path("conditionResults").size());
        for(var row:alt.path("conditionResults"))assertEquals(id("OTHERPACK"),row.path("segmentId").asText());
        for(var row:p.path("conditionResults"))assertNotEquals(id("OTHERPACK"),row.path("segmentId").asText());
        for(var sub:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases"))if(sid.equals(sub.path("id").asText()))for(var a:sub.path("actions"))if(Set.of("order","reserve").contains(a.path("id").asText())){
            var request=new ReferenceResolver(results,aliases).resolve(a.path("request"));var response=gateway(a.path("actorRef").asText(),"commands/"+a.path("capabilityId").asText(),request);
            assertEquals(a.path("id").asText().equals("order")?"APPLIED":"REJECTED",response.path("outcome").asText(),response.toPrettyString());
            var result=Json.object().put("driverStatus","EXECUTED");result.set("response",response);result.set("provenance",Json.object().put("scopeComplete",true));results.put(a.path("id").asText(),result);save(a.path("id").asText(),result);
        }
        try(var s=db.prepareStatement("SELECT count(*) FROM mulino_inventory_SegmentAllocations WHERE organizationId=?")){s.setString(1,org);try(var r=s.executeQuery()){assertTrue(r.next());assertEquals(0,r.getInt(1));}}
    }
}
