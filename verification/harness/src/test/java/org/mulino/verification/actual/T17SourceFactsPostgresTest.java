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
    Connection connection;String testName;final Map<String,JsonNode> actionResults=new HashMap<>();
    @BeforeEach void install(TestInfo info)throws Exception {
        testName=info.getTestMethod().orElseThrow().getName();
        String selected=System.getenv("T17_PG_METHODS");
        Assumptions.assumeTrue(selected==null||Set.of(selected.split(",")).contains(testName),"Explicit focused deterministic PG selection");
        config=ActualConfiguration.environment(System.getenv());
        String fixtureName=switch(testName){case "completedReplacementUsesRealWitnessAndReleaseSource"->"replacement-no-revival";case "newCategoryHoldAppliesWithinOriginalScope"->"reserve-pick-dispatch";case "newCategoryEarlyHoldApplies"->"eligibility-fefo-with-reason";case "explicitDispatchNewRecallHoldApplies"->"late-restriction-actual-delivery";case "explicitDispatchLateHoldSourceStopsAtRecallAuthority"->"late-restriction-actual-delivery";case "holdSourceReachesCategoryAuthorizationWithoutGrantExpansion"->"reserve-pick-dispatch";case "declaredExpiryGroupsMapToDistinctLots"->"eligibility-fefo-with-reason";case "postDispatchExpiryBoundaryIsExplicit","explicitDispatchExpiredDelivery"->"post-dispatch-expiry";case "explicitDispatchLateDelivery"->"late-restriction-actual-delivery";case "explicitDispatchFalseClaim"->"claim-falseclaim";case "explicitDispatchConflictingClaim"->"claim-conflictingquantity";case "explicitDispatchUnidentifiedClaim"->"claim-unidentifiedscope";case "explicitDispatchOverflowClaim"->"claim-scopeoverflow";case "otherpackUsesItsOwnTypedLot"->"eligibility-packaging-unapproved";case "authoredNoDispatchObservationUsesRealGateway"->"claim-nopriordispatch";default->"normal-consumed-delivery";};
        Path fixturePath=root.resolve("verification/cases/T17/fixtures/"+fixtureName+".json");
        fixture=(ObjectNode)Json.read(fixturePath);
        if(Set.of("holdSourceReachesCategoryAuthorizationWithoutGrantExpansion","explicitDispatchLateHoldSourceStopsAtRecallAuthority").contains(testName)) {
            // Historical negative copy uses the complete original actor/grant,
            // not the newly authorized synthetic category precondition.
            for(var decision:fixture.path("baseline").path("policy").path("completionInputAuthoring").path("decisions"))if("NEW_CATEGORY_CAPABILITY".equals(decision.path("kind").asText()))
                ((ObjectNode)fixture.path("actors")).set(decision.path("actorAlias").asText(),decision.path("originalActor").deepCopy());
        }
        if(!testName.startsWith("explicitDispatch")) {
            // Deliberate SELL-only control; the checked-in fixture now has separate
            // coordinator-authored DISPATCH inputs. No grants are changed.
            ((ObjectNode)fixture.path("baseline").path("t17SourceFacts")).remove(List.of("dispatchInputs","dispatchPolicyAlias","dispatchSegmentAliases"));
            ((ObjectNode)fixture.path("aliases")).remove("t17DispatchRegulatoryPolicy");
        }
        if(testName.equals("explicitDispatchMissingReviseGoalGrant")) {
            var source=(ObjectNode)fixture.path("actors").path("t17Source");var caps=Json.array();for(var cap:source.path("roleCapabilities"))if(!cap.asText().equals("reviseGoal"))caps.add(cap);source.set("roleCapabilities",caps);((ObjectNode)source.path("grant")).set("actions",caps.deepCopy());
        }
        if(testName.equals("explicitDispatchMissingActivationGrant")) {
            var source=(ObjectNode)fixture.path("actors").path("t17Source");
            var caps=Json.array();for(var cap:source.path("roleCapabilities"))if(!cap.asText().equals("activateWork"))caps.add(cap);
            source.set("roleCapabilities",caps);((ObjectNode)source.path("grant")).set("actions",caps.deepCopy());
        }
        for(String state:List.of("UNKNOWN","DENIED","CONFLICT"))if(testName.equals("explicitDispatch"+state+"RemainsBlocked")) {
            ((ObjectNode)fixture.path("baseline").path("conditions").get(0)).put("status",state);
            ((ObjectNode)fixture.path("baseline").path("t17SourceFacts").path("dispatchInputs").get(0)).set("originalCondition",fixture.path("baseline").path("conditions").get(0).deepCopy());
        }
        // Explicit negative copy: the authored positive fixture keeps its expiry.
        if(testName.equals("missingExpiryCopyRemainsUnknown")){
            ((ObjectNode)fixture.path("aliases").path("L")).remove("expiresAt");
            ((ObjectNode)fixture.path("baseline").path("policy")).remove("lotExpiryAuthoring");
        }
        if(info.getTestMethod().orElseThrow().getName().equals("unknownConditionRemainsOmittedWithoutInventedPermission"))((ObjectNode)fixture.path("baseline").path("conditions").get(0)).put("status","UNKNOWN");
        var bundle=Json.object().put("fixtureHash",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(fixture.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));bundle.set("fixture",fixture);
        bundle=(ObjectNode)ActualFixtureBindings.bind(root,bundle,config);
        fixture=(ObjectNode)bundle.path("fixture");
        installed=ScenarioFixtureInstaller.install(config,root,bundle,true);aliases=(ObjectNode)installed.path("aliasMap");org=aliases.path("ORG").asText();
        sources=new T17SourceFacts(config,fixture,aliases,installed.path("organizationExternalAlias").asText());
        connection=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());
        Path artifacts=Path.of(System.getenv("T17_TEST_EVIDENCE"));Files.createDirectories(artifacts);Files.writeString(artifacts.resolve(testName+"-installer-source-results.json"),Json.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(installed));
    }
    @AfterEach void close()throws Exception {if(sources!=null)Files.writeString(Path.of(System.getenv("T17_TEST_EVIDENCE")).resolve(testName+"-negative-command-results.json"),sources.commandEvidence().toPrettyString());if(connection!=null){try{ledgerEvidence();}finally{connection.close();}}}
    int count(String table)throws Exception {try(var s=connection.prepareStatement("SELECT count(*) FROM "+table+" WHERE organizationId=?")){s.setString(1,org);try(var r=s.executeQuery()){r.next();return r.getInt(1);}}}
    String id(String alias){return aliases.path(alias).asText();}
    void sql(String sql,Object...args)throws Exception {try(var s=connection.prepareStatement(sql)){for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);s.executeUpdate();}}
    ObjectNode permission(String evidence)throws Exception {
        var receipt=installed.path("sourceFactEvidence").path("conditions").get(0);
        // Use the actual immutable source decision, not an assertion expected value.
        try(var s=connection.prepareStatement("SELECT e.payload FROM mulino_evidence_Events e WHERE e.organizationId=? AND e.ID=?")){s.setString(1,org);s.setString(2,receipt.path("eventId").asText());try(var r=s.executeQuery()){r.next();var p=(ObjectNode)Json.parse(r.getString(1));p.remove(List.of("operation","sourceDecisionId","sourceVersion","givenEvidenceAlias","syntheticActionInput"));return p.put("evidenceId",evidence).put("reason","T17 scoped negative").put("nextCheckAt","2026-10-07T10:00:00Z");}}
    }
    JsonNode queryEligibility()throws Exception {return queryEligibility("2026-10-07T09:00:00Z");}
    JsonNode queryEligibility(String at)throws Exception {return queryEligibility(at,"SELL");}
    JsonNode queryEligibility(String at,String action)throws Exception {
        var credential=(ObjectNode)fixture.path("actors").path("observer").deepCopy();credential.put("organizationAlias",installed.path("organizationExternalAlias").asText());
        var body=Json.object().put("id",id("A")).put("asOf",at).put("knownAt",java.time.Instant.parse(at).plusSeconds(1).toString());body.set("filters",Json.object().put("action",action));body.set("scope",Json.object().put("itemId",id("P")));
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(config.baseUri().resolve("/api/ontology/queries/evaluateEligibility")).header("Content-Type","application/json").header("Authorization","Bearer "+new JwtSigner(config).sign(credential)).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode(),response.body());return Json.parse(response.body());
    }
    @Test void sourceChainsUseRealCurrentAuthorityWithDeclaredExpiry()throws Exception {
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
        var query=queryEligibility();assertFalse(query.path("unknowns").toString().contains("LOT_EXPIRY_UNKNOWN"),query.toPrettyString());assertEquals("30",query.path("data").path("eligibleQuantity").asText(),query.toPrettyString());assertEquals("ALLOWED",query.path("data").path("eligibilityStatus").asText());
        Files.writeString(Path.of(System.getenv("T17_TEST_EVIDENCE")).resolve("positive-expiry-query.json"),query.toPrettyString());
    }
    @Test void unknownConditionRemainsOmittedWithoutInventedPermission()throws Exception {
        var result=installed.path("sourceFactEvidence");assertEquals(4,result.path("conditions").size());assertEquals(1,result.path("uninstalledConditions").size());
        assertTrue(result.path("uninstalledConditions").get(0).path("reason").asText().contains("UNKNOWN"));assertTrue(installed.path("omittedFacts").toString().contains("baseline.conditions"));assertEquals(2,count("mulino_inventory_DispositionBases"));
        try(var s=connection.prepareStatement("SELECT count(*) FROM mulino_inventory_DispositionBases WHERE organizationId=? AND category='QC'")){s.setString(1,org);try(var r=s.executeQuery()){r.next();assertEquals(0,r.getInt(1));}}
    }
    @Test void missingExpiryCopyRemainsUnknown()throws Exception {
        assertEquals(5,installed.path("sourceFactEvidence").path("conditions").size());
        assertEquals(3,count("mulino_inventory_DispositionBases"));assertEquals(8,count("mulino_evidence_Verifications"));
        var query=queryEligibility();assertTrue(query.path("unknowns").toString().contains("LOT_EXPIRY_UNKNOWN"),query.toPrettyString());assertEquals("0",query.path("data").path("eligibleQuantity").asText());
        Files.writeString(Path.of(System.getenv("T17_TEST_EVIDENCE")).resolve("expiry-unknown-query.json"),query.toPrettyString());
    }
    @Test void declaredExpiryGroupsMapToDistinctLots()throws Exception {
        var lots=new HashSet<String>();
        for(String name:List.of("A","B","EARLY"))try(var s=connection.prepareStatement("SELECT q.lotId,l.expiresAt,l.manufacturerId,l.itemId FROM mulino_inventory_QuantitySegments q JOIN mulino_inventory_ManufacturingLots l ON l.organizationId=q.organizationId AND l.ID=q.lotId WHERE q.organizationId=? AND q.ID=?")){
            s.setString(1,org);s.setString(2,id(name));try(var r=s.executeQuery()){assertTrue(r.next());lots.add(r.getString(1));assertEquals(java.time.Instant.parse(fixture.path("aliases").path(name).path("expiresAt").asText()),r.getTimestamp(2).toInstant());assertEquals(id("t17LotManufacturer"),r.getString(3));assertEquals(id("P"),r.getString(4));}
        }
        assertEquals(3,lots.size());assertEquals("L",fixture.path("aliases").path("A").path("lotAlias").asText());
        var query=queryEligibility();assertEquals("20",query.path("data").path("eligibleQuantity").asText(),query.toPrettyString());
    }
    @Test void postDispatchExpiryBoundaryIsExplicit()throws Exception {
        assertEquals("30",queryEligibility().path("data").path("eligibleQuantity").asText());
        var credential=(ObjectNode)fixture.path("actors").path("observer").deepCopy();credential.put("organizationAlias",installed.path("organizationExternalAlias").asText());
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(config.baseUri().resolve("/__verification/runtime/clock")).header("Content-Type","application/json").header("Authorization","Bearer "+new JwtSigner(config).sign(credential)).POST(HttpRequest.BodyPublishers.ofString("{\"instant\":\"2026-10-07T09:06:00Z\"}")).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode(),response.body());var query=queryEligibility("2026-10-07T09:06:00Z");assertEquals("0",query.path("data").path("eligibleQuantity").asText(),query.toPrettyString());assertTrue(query.path("data").path("conditions").toString().contains("LOT_VALIDITY"));
        Files.writeString(Path.of(System.getenv("T17_TEST_EVIDENCE")).resolve("post-expiry-query.json"),query.toPrettyString());
    }
    @Test void otherpackUsesItsOwnTypedLot()throws Exception {
        try(var s=connection.prepareStatement("SELECT s.itemId,s.lotId,l.itemId,l.manufacturerId,l.expiresAt,s.quantity,s.placeId,s.identificationStatus FROM mulino_inventory_QuantitySegments s JOIN mulino_inventory_ManufacturingLots l ON l.organizationId=s.organizationId AND l.ID=s.lotId WHERE s.organizationId=? AND s.ID=?")){
            s.setString(1,org);s.setString(2,id("OTHERPACK"));try(var r=s.executeQuery()){
                assertTrue(r.next());assertEquals(id("ALT"),r.getString(1));assertEquals(id("L_ALT"),r.getString(2));assertEquals(id("ALT"),r.getString(3));
                assertEquals(id("t17LotManufacturer"),r.getString(4));assertEquals(java.time.Instant.parse("2026-10-20T00:00:00Z"),r.getTimestamp(5).toInstant());
                assertEquals(0,new BigDecimal("20").compareTo(r.getBigDecimal(6)));assertEquals(id("W"),r.getString(7));assertEquals("CONFIRMED",r.getString(8));
            }
        }
        assertNotEquals(id("L"),id("L_ALT"));
        // Original LABEL status is preserved; package mismatch remains a separate guard.
        var labels=new ArrayList<JsonNode>();for(var c:fixture.path("baseline").path("conditions"))if("OTHERPACK".equals(c.path("segmentAlias").asText())&&"LABEL".equals(c.path("conditionId").asText()))labels.add(c);
        assertEquals(1,labels.size());assertEquals("ALLOWED",labels.getFirst().path("status").asText());
        assertEquals("other-box-v1",fixture.path("aliases").path("ALT").path("packagingVersion").asText());assertNotEquals(fixture.path("aliases").path("P").path("packagingVersion"),fixture.path("aliases").path("ALT").path("packagingVersion"));
    }
    JsonNode gateway(String actor,String capability,ObjectNode request)throws Exception {
        var credential=(ObjectNode)fixture.path("actors").path(actor).deepCopy();credential.put("organizationAlias",installed.path("organizationExternalAlias").asText());
        String category=Set.of("getObject","getWork").contains(capability)?"queries":"RECORD".equals(request.path("intentKind").asText())?"records":"commands";
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(config.baseUri().resolve("/api/ontology/"+category+"/"+capability)).header("Content-Type","application/json").header("Authorization","Bearer "+new JwtSigner(config).sign(credential)).POST(HttpRequest.BodyPublishers.ofString(request.toString())).build(),HttpResponse.BodyHandlers.ofString());
        var result=Json.parse(response.body());var artifact=Json.object().put("httpStatus",response.statusCode()).put("actor",actor).put("capability",capability);artifact.set("request",request);artifact.set("response",result);
        Files.writeString(Path.of(System.getenv("T17_TEST_EVIDENCE")).resolve(testName+"-"+request.path("commandIdempotencyKey").asText(capability+"-"+request.path("id").asText())+".json"),artifact.toPrettyString());return result;
    }
    // Plan §6: accepting a report is provisional; only canonical confirmation
    // applies physical delivery. These expectations belong to the test helper,
    // independently of the case's unchanged business assertions.
    boolean observationAction(JsonNode action) {
        String capability=action.path("capabilityId").asText();
        return Set.of("recordDelivery","recordObservedMovement").contains(capability)
            &&!action.at("/request/slots").hasNonNull("canonicalOccurrenceId");
    }
    ObjectNode deliveryState()throws Exception {
        var state=Json.object();
        for(String table:List.of("mulino_inventory_QuantitySegments","mulino_inventory_QuantityMovements","mulino_inventory_SegmentAllocations","mulino_inventory_Dispatches","mulino_inventory_CargoScopes","mulino_inventory_DeliveryTransfers","mulino_trade_sales_Deliveries","mulino_trade_sales_ExecutionEffects","mulino_work_WorkContributions","mulino_work_read_AssessmentReferences","mulino_evaluation_InputSnapshots")) {
            var rows=state.putArray(table);
            try(var q=connection.prepareStatement("SELECT row_to_json(t)::text FROM "+table+" t WHERE organizationId=? ORDER BY ID")){
                q.setString(1,org);try(var r=q.executeQuery()){while(r.next())rows.add(Json.parse(r.getString(1)));}
            }
        }
        return state;
    }
    JsonNode authoredAction(JsonNode action,ObjectNode request)throws Exception {
        boolean observation=observationAction(action);
        var before=observation?deliveryState():null;
        var result=gateway(action.path("actorRef").asText(),action.path("capabilityId").asText(),request);
        if(observation) {
            assertTrue(Set.of("claim","delivery-provisional").contains(action.path("id").asText()),"Declare the exact observation action contract");
            assertPendingObservation(action,request,result,before);
        } else if(action.path("kind").asText().equals("query"))assertTrue(result.path("data").hasNonNull("ID"),result.toPrettyString());
        // These two actions also have explicit denied-authority controls; their
        // callers assert the exact positive or negative outcome and DB effect.
        else if(!action.path("id").asText().equals("dispatch")&&!action.path("capabilityId").asText().equals("placeHold"))assertEquals("APPLIED",result.path("outcome").asText(),result.toPrettyString());
        return result;
    }
    void assertPendingObservation(JsonNode action,ObjectNode request,JsonNode result,ObjectNode before)throws Exception {
        var after=deliveryState();var proof=Json.object();proof.set("before",before);proof.set("after",after);
        Files.writeString(Path.of(System.getenv("T17_TEST_EVIDENCE")).resolve(testName+"-"+action.path("id").asText()+"-provisional-state.json"),proof.toPrettyString());
        assertEquals("ACCEPTED_PENDING_EXTERNAL",result.path("outcome").asText(),result.toPrettyString());
        assertEquals(1,result.path("revision").asInt());
        var slots=request.path("slots");String observation=slots.path("observationId").asText();
        assertFalse(observation.isBlank());assertEquals(UUID.fromString(observation).toString(),observation);
        assertEquals(Json.object().put("observationId",observation).put("physicalQuantity","0").put("contributedQuantity","0"),result.path("effects"));
        assertEquals(before,after,"Provisional intake preserves physical, allocation, contribution and assessment rows");
        try(var q=connection.prepareStatement("SELECT state,revision,rangeRootId,claimedRangeRootId,claimPayloadJson,eventId,workId FROM mulino_trade_sales_Observations WHERE organizationId=? AND ID=?")){
            q.setString(1,org);q.setString(2,observation);try(var r=q.executeQuery()){
                assertTrue(r.next());assertEquals("PROVISIONAL",r.getString(1));assertEquals(1,r.getInt(2));assertNull(r.getString(3));
                assertEquals(slots.path("rangeRootId").asText(),r.getString(4));assertEquals(slots,Json.parse(r.getString(5)));
                assertEquals(slots.path("eventId").asText(),r.getString(6));assertEquals(slots.path("workId").asText(),r.getString(7));assertFalse(r.next());
            }
        }
        try(var q=connection.prepareStatement("SELECT o.ownerId,p.intakeOwnerId,a.kind,o.supervisorId,p.supervisorId,o.nextAction,o.nextCheckAt,o.quantity,o.unit,d.sourceId,d.sourceKind,d.scopeJson FROM mulino_work_read_ObligationReferences o JOIN mulino_responsibility_Roots d ON d.organizationId=o.organizationId AND d.ID=o.rootId JOIN mulino_evidence_Events e ON e.organizationId=o.organizationId AND e.ID=? JOIN mulino_evidence_SourceProfiles p ON p.organizationId=e.organizationId AND p.ID=e.sourceProfileId JOIN mulino_identity_Actors a ON a.organizationId=o.organizationId AND a.ID=o.ownerId WHERE o.organizationId=? AND o.kind='DELIVERY_RECONCILIATION' AND o.valid AND o.status='OPEN' AND d.scopeJson::jsonb->>'domainSourceId'=?")){
            q.setString(1,slots.path("eventId").asText());q.setString(2,org);q.setString(3,observation);try(var r=q.executeQuery()){
                assertTrue(r.next(),"Exactly one source-owned reconciliation duty");assertEquals(r.getString(2),r.getString(1));assertEquals("HUMAN",r.getString(3));assertEquals(r.getString(5),r.getString(4));
                assertEquals(slots.path("nextAction").asText(),r.getString(6));assertEquals(java.time.Instant.parse(slots.path("nextCheckAt").asText()),r.getTimestamp(7).toInstant());
                assertEquals(0,new BigDecimal(slots.path("quantity").asText()).compareTo(r.getBigDecimal(8)));assertEquals(slots.path("unit").asText(),r.getString(9));
                assertEquals(result.path("commandId").asText(),r.getString(10));assertEquals("DECISION",r.getString(11));var scope=Json.parse(r.getString(12));
                assertEquals(observation,scope.path("physicalScopeId").asText());assertEquals(slots.path("workId").asText(),scope.path("originalWorkId").asText());assertFalse(r.next());
            }
        }
        try(var q=connection.prepareStatement("SELECT state,resultJson,canonicalIntentJson,actorId,effectClass FROM mulino_commands_CommandRecords WHERE organizationId=? AND ID=? AND capabilityId=? AND commandIdempotencyKey=?")){
            q.setString(1,org);q.setString(2,result.path("commandId").asText());q.setString(3,action.path("capabilityId").asText());q.setString(4,request.path("commandIdempotencyKey").asText());try(var r=q.executeQuery()){
                assertTrue(r.next());assertEquals("COMMITTED",r.getString(1));var stored=Json.parse(r.getString(2));assertEquals("ACCEPTED_PENDING_EXTERNAL",stored.path("outcome").asText());assertEquals(result.path("effects"),stored.path("effects"));
                assertEquals(slots,Json.parse(r.getString(3)).path("slots"));assertEquals(id(action.path("actorRef").asText()),r.getString(4));assertEquals("RECORD",r.getString(5));assertFalse(r.next());
            }
        }
    }
    @Test void authoredNoDispatchObservationUsesRealGateway()throws Exception {
        var cases=Json.read(root.resolve("verification/cases/T17/case.json"));JsonNode selected=null;for(var s:cases.path("subcases"))if("claim-nopriordispatch".equals(s.path("id").asText()))selected=s;
        assertNotNull(selected);var results=new HashMap<String,JsonNode>();var resolver=new org.mulino.verification.ReferenceResolver(results,aliases);
        for(var action:selected.path("actions"))if(Set.of("claim-source-document","claim-source-event","claim").contains(action.path("id").asText())){
            var request=(ObjectNode)resolver.resolve(action.path("request"));var result=authoredAction(action,request);
            var wrapped=Json.object().put("driverStatus","EXECUTED");wrapped.set("response",result);wrapped.set("provenance",Json.object().put("scopeComplete",true).put("source","ACTUAL_HTTP_COMMAND_GATEWAY").put("authenticatedActor",id(action.path("actorRef").asText())));results.put(action.path("id").asText(),wrapped);
        }
        var claim=results.get("claim").path("response");assertEquals("0",claim.path("effects").path("physicalQuantity").asText());assertEquals("0",claim.path("effects").path("contributedQuantity").asText());
        try(var s=connection.prepareStatement("SELECT intakeOwnerId,sourceNamespace,externalEventId FROM mulino_evidence_InboxRecords WHERE organizationId=? AND eventId=?")){
            s.setString(1,org);s.setString(2,results.get("claim-source-event").path("response").path("id").asText());
            try(var r=s.executeQuery()){assertTrue(r.next());assertEquals(id("intake"),r.getString(1));assertEquals("SYNTHETIC-UNVERIFIED-SOURCE",r.getString(2));assertEquals("claim-nopriordispatch-claim",r.getString(3));}
        }
        assertEquals(1,count("mulino_trade_sales_Observations"));assertEquals(0,count("mulino_trade_sales_Deliveries"));assertEquals(0,count("mulino_inventory_Dispatches"));
        var request=Json.object().put("intentKind","RECORD").put("definitionVersion","definition-v1").put("capabilityId","recordDelivery").put("commandIdempotencyKey",UUID.randomUUID().toString()).put("expectedRevision",1);
        request.set("subjectRefs",Json.array());request.set("provenance",Json.object().put("observationId","CONTEXT").put("canonicalOccurrenceId","CONTEXT"));request.set("slots",Json.object().put("observationId",claim.path("effects").path("observationId").asText()).put("canonicalOccurrenceId",UUID.randomUUID().toString()));
        var denied=gateway("receiver","recordDelivery",request);assertEquals("DELIVERY_UNMATCHED",denied.path("error").path("code").asText(),denied.toPrettyString());assertEquals(0,count("mulino_trade_sales_Deliveries"));
    }
    @Test void unsupportedSlotsAreNotBusinessGuardAcceptance()throws Exception {
        for(String cap:List.of("reserveQuantity","replaceAllocation","releaseAllocation","placeHold")){
            String key="unsupportedFulfillmentSlot";
            var denied=sources.request(cap,Json.object().put(key,"original unsupported field"),1);
            assertEquals("TYPE_INVALID",denied.path("error").path("code").asText(),denied.toPrettyString());
        }
        assertEquals(0,count("mulino_inventory_SegmentAllocations"));assertEquals(0,count("mulino_inventory_Restrictions"));assertEquals(0,count("mulino_inventory_Dispatches"));
    }
    String claim(String event)throws Exception {try(var s=connection.prepareStatement("SELECT ID FROM mulino_evidence_Claims WHERE organizationId=? AND eventId=?")){s.setString(1,org);s.setString(2,event);try(var r=s.executeQuery()){r.next();return r.getString(1);}}}
    JsonNode runActions(String subcase,Set<String> names)throws Exception {
        JsonNode selected=null;for(var sub:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases"))if(subcase.equals(sub.path("id").asText()))selected=sub;
        assertNotNull(selected);var results=actionResults;var resolver=new org.mulino.verification.ReferenceResolver(results,aliases);JsonNode last=null;
        names=new HashSet<>(names);if(names.contains("activate-order-delivery-work")){names.add("complete-order-delivery-goal");names.add("completed-order-delivery-work");}
        for(var action:selected.path("actions"))if(names.contains(action.path("id").asText())) {
            if(action.path("kind").asText().equals("control")){
                var credential=(ObjectNode)fixture.path("actors").path("observer").deepCopy();credential.put("organizationAlias",installed.path("organizationExternalAlias").asText());
                var clockRequest=Json.object().put("instant",action.at("/control/parameters/asOf").asText());
                var changed=HttpClient.newHttpClient().send(HttpRequest.newBuilder(config.baseUri().resolve("/__verification/runtime/clock")).header("Content-Type","application/json").header("Authorization","Bearer "+new JwtSigner(config).sign(credential)).POST(HttpRequest.BodyPublishers.ofString(clockRequest.toString())).build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,changed.statusCode(),changed.body());continue;
            }
            var resolved=(ObjectNode)resolver.resolve(action.path("request"));
            if(action.path("id").asText().equals("complete-order-delivery-goal"))assertGoalBinding(resolved);
            last=authoredAction(action,resolved);
            var wrapped=Json.object().put("driverStatus","EXECUTED");wrapped.set("response",last);wrapped.set("provenance",Json.object().put("scopeComplete",true).put("source","ACTUAL_HTTP_COMMAND_GATEWAY").put("authenticatedActor",id(action.path("actorRef").asText())));results.put(action.path("id").asText(),wrapped);
        }
        return last;
    }
    void assertGoalBinding(ObjectNode request)throws Exception {
        var previous=actionResults.get("order-delivery-work").at("/response/data/currentGoal/slots");var goal=request.at("/slots/goal");
        previous.fields().forEachRemaining(e->assertEquals(e.getValue(),goal.get(e.getKey()),"Original goal slot preserved: "+e.getKey()));
        assertEquals(fixture.at("/clock/timezone"),goal.get("timezone"));assertEquals(fixture.at("/clock/asOf"),goal.get("periodStart"));assertEquals(previous.get("dueAt"),goal.get("periodEnd"));
        String definition=goal.at("/defaultContext/definitionVersionId").asText();
        assertEquals(actionResults.get("order-delivery-work").at("/response/data/definitionVersionId").asText(),definition);
        try(var q=connection.prepareStatement("SELECT state,contentHash,content,evaluatorVersion FROM mulino_definitions_DefinitionVersions WHERE organizationId=? AND ID=?")){q.setString(1,org);q.setString(2,definition);try(var r=q.executeQuery()){assertTrue(r.next());assertEquals("PUBLISHED",r.getString(1));assertEquals(r.getString(2),goal.at("/defaultContext/definitionContentHash").asText());assertEquals(r.getString(4),goal.path("evaluatorVersion").asText());var content=Json.parse(r.getString(3));var matches=new ArrayList<JsonNode>();for(var t:content.path("goals"))if(t.path("endpoint").asText().equals("DELIVERED")&&t.path("quantityMode").asText().equals("CUMULATIVE_EVENT"))matches.add(t);assertEquals(1,matches.size());assertEquals(matches.getFirst().path("name"),goal.at("/conditions/0/id"));}}
        try(var q=connection.prepareStatement("SELECT p.contentHash FROM mulino_governance_PolicyVersions p JOIN mulino_governance_ActivePolicies a ON a.organizationId=p.organizationId AND a.policyId=p.ID WHERE p.organizationId=? AND p.kind='EVIDENCE' AND p.version=?")){q.setString(1,org);q.setString(2,goal.path("evidencePolicyVersion").asText());try(var r=q.executeQuery()){assertTrue(r.next());assertEquals(r.getString(1),goal.at("/defaultContext/evidencePolicyHash").asText());assertFalse(r.next());}}
    }
    JsonNode allCommands(String sid)throws Exception {
        JsonNode selected=null;for(var sub:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases"))if(sid.equals(sub.path("id").asText()))selected=sub;
        assertNotNull(selected);var names=new HashSet<String>();
        for(var a:selected.path("actions"))if(a.path("kind").asText().equals("invoke")||a.path("kind").asText().equals("control")||a.path("kind").asText().equals("query")&&Set.of("order-delivery-work","completed-order-delivery-work","delivery-source-dispatch","delivery-source-sales-line","delivery-source-observation","claim-source-dispatch","claim-source-sales-line","late-hold-source-scope").contains(a.path("id").asText()))names.add(a.path("id").asText());
        return runActions(sid,names);
    }
    void ledgerEvidence()throws Exception {
        var snapshot=Json.object().put("organizationId",org).put("test",testName);
        for(String table:List.of("mulino_inventory_QuantityMovements","mulino_inventory_SegmentAllocations","mulino_inventory_QuantitySegments","mulino_trade_sales_Deliveries","mulino_trade_sales_Observations","mulino_trade_sales_ExecutionEffects")) {
            var rows=snapshot.putArray(table);
            try(var q=connection.prepareStatement("SELECT * FROM "+table+" WHERE organizationId=? ORDER BY ID")){q.setString(1,org);try(var r=q.executeQuery()){var meta=r.getMetaData();while(r.next()){var row=rows.addObject();for(int col=1;col<=meta.getColumnCount();col++){Object value=r.getObject(col);if(value==null)row.putNull(meta.getColumnLabel(col));else row.put(meta.getColumnLabel(col),value.toString());}}}}
        }
        Files.writeString(Path.of(System.getenv("T17_TEST_EVIDENCE")).resolve(testName+"-ledger.json"),snapshot.toPrettyString());
    }
    void actualDelivery(String sid,String expected,String legitimate)throws Exception {
        var result=allCommands(sid);assertEquals("APPLIED",result.path("outcome").asText(),result.toPrettyString());
        var delivery=actionResults.get("delivery").path("response");assertEquals(expected,delivery.path("effects").path("physicalQuantity").asText());assertEquals(legitimate,delivery.path("effects").path("contributedQuantity").asText());
        assertEquals(1,count("mulino_trade_sales_Deliveries"));assertEquals(1,count("mulino_inventory_Dispatches"));
        try(var q=connection.prepareStatement("SELECT quantity,legitimateQuantity,workId,salesLineId,dispatchId FROM mulino_trade_sales_Deliveries WHERE organizationId=?")){q.setString(1,org);try(var r=q.executeQuery()){assertTrue(r.next());assertEquals(0,new BigDecimal(expected).compareTo(r.getBigDecimal(1)));assertEquals(0,new BigDecimal(legitimate).compareTo(r.getBigDecimal(2)));assertEquals(actionResults.get("order").at("/response/workId").asText(),r.getString(3));assertEquals(actionResults.get("order").at("/response/line/id").asText(),r.getString(4));assertEquals(actionResults.get("dispatch").at("/response/effects/dispatchId").asText(),r.getString(5));}}
        try(var q=connection.prepareStatement("SELECT state,quantity FROM mulino_inventory_SegmentAllocations WHERE organizationId=?")){q.setString(1,org);try(var r=q.executeQuery()){assertTrue(r.next());assertEquals("CONSUMED",r.getString(1));assertEquals(0,new BigDecimal("30").compareTo(r.getBigDecimal(2)));}}
        var expectedMovements=new HashMap<String,BigDecimal>();expectedMovements.put("INITIAL_BALANCE",new BigDecimal("30"));expectedMovements.put("DISPATCH",new BigDecimal("30"));expectedMovements.put("DELIVERY",new BigDecimal(expected));
        BigDecimal remaining=new BigDecimal("30").subtract(new BigDecimal(expected));if(remaining.signum()>0)expectedMovements.put("RETAINED",remaining);
        var actualMovements=new HashMap<String,BigDecimal>();
        try(var q=connection.prepareStatement("SELECT kind,sum(quantity),count(*) FROM mulino_inventory_QuantityMovements WHERE organizationId=? GROUP BY kind")){q.setString(1,org);try(var r=q.executeQuery()){while(r.next()){assertEquals(1,r.getInt(3));actualMovements.put(r.getString(1),r.getBigDecimal(2));}}}
        assertEquals(expectedMovements.keySet(),actualMovements.keySet());for(var entry:expectedMovements.entrySet())assertEquals(0,entry.getValue().compareTo(actualMovements.get(entry.getKey())));
        for(String place:List.of("TRANSIT","CUSTOMER"))try(var q=connection.prepareStatement("SELECT coalesce(sum(quantity),0) FROM mulino_inventory_QuantitySegments WHERE organizationId=? AND retiredAt IS NULL AND placeId=?")){q.setString(1,org);q.setString(2,id(place));try(var r=q.executeQuery()){assertTrue(r.next());assertEquals(0,(place.equals("TRANSIT")?remaining:new BigDecimal(expected)).compareTo(r.getBigDecimal(1)));}}
        ledgerEvidence();
    }
    @Test void explicitDispatchUnactivatedWorkRemainsRejected()throws Exception {
        runActions("normal-consumed-delivery",Set.of("order","order-delivery-work","reserve","pick","dispatch","delivery-source-dispatch","delivery-source-sales-line","delivery-source-document","delivery-source-event"));
        assertEquals("DRAFT",actionResults.get("order-delivery-work").at("/response/data/status").asText());
        var request=actionRequest("normal-consumed-delivery","delivery-provisional");
        var denied=gateway("receiver","recordDelivery",request);
        assertEquals("TYPE_INVALID",denied.at("/error/code").asText());assertEquals("Duty needs active work",denied.at("/error/message").asText());
        assertEquals(0,count("mulino_trade_sales_Observations"));assertEquals(0,count("mulino_trade_sales_Deliveries"));assertEquals(2,count("mulino_inventory_QuantityMovements"));ledgerEvidence();
    }
    @Test void explicitDispatchMissingActivationGrant()throws Exception {
        int originalMovements=count("mulino_inventory_QuantityMovements");
        runActions("normal-consumed-delivery",Set.of("order","order-delivery-work","complete-order-delivery-goal","completed-order-delivery-work"));
        var denied=gateway("t17Source","activateWork",actionRequest("normal-consumed-delivery","activate-order-delivery-work"));
        assertEquals("FORBIDDEN",denied.at("/error/code").asText());
        assertEquals(0,count("mulino_trade_sales_Observations"));assertEquals(originalMovements,count("mulino_inventory_QuantityMovements"));
    }
    @Test void explicitDispatchMissingReviseGoalGrant()throws Exception {
        int movements=count("mulino_inventory_QuantityMovements");
        runActions("normal-consumed-delivery",Set.of("order","order-delivery-work"));
        var denied=gateway("t17Source","reviseGoal",actionRequest("normal-consumed-delivery","complete-order-delivery-goal"));
        assertEquals("FORBIDDEN",denied.at("/error/code").asText());assertEquals(movements,count("mulino_inventory_QuantityMovements"));
        assertEquals(0,count("mulino_trade_sales_Deliveries"));
    }
    @Test void explicitDispatchIncompleteGoalRemainsClosed()throws Exception {
        String sid="normal-consumed-delivery";int movements=count("mulino_inventory_QuantityMovements");
        runActions(sid,Set.of("order","order-delivery-work"));
        var complete=actionRequest(sid,"complete-order-delivery-goal");
        var query=actionRequest(sid,"order-delivery-work");JsonNode current=actionResults.get("order-delivery-work").path("response");
        for(String field:List.of("timezone","evidencePolicyVersion","conditions","evaluatorVersion","invalid-condition","invalid-evaluator")) {
            var request=complete.deepCopy();request.put("commandIdempotencyKey",UUID.randomUUID().toString());request.put("expectedRevision",current.at("/data/revision").asInt());
            var goal=(ObjectNode)request.at("/slots/goal");
            if(field.equals("invalid-condition"))goal.set("conditions",Json.array().add(Json.object().put("id","not-a-published-template")));
            else if(field.equals("invalid-evaluator"))goal.put("evaluatorVersion","evaluator-v1");else goal.remove(field);
            var revised=gateway("t17Source","reviseGoal",request);assertEquals("APPLIED",revised.path("outcome").asText(),revised.toPrettyString());
            current=gateway("observer","getWork",query);
            var activation=Json.object().put("intentKind","COMMAND").put("definitionVersion","definition-v1").put("capabilityId","activateWork").put("commandIdempotencyKey",UUID.randomUUID().toString()).put("expectedRevision",current.at("/data/revision").asInt());
            activation.set("subjectRefs",Json.array());activation.set("provenance",Json.object().put("workId","CONTEXT"));activation.set("slots",Json.object().put("workId",current.at("/data/ID").asText()));
            var denied=gateway("t17Source","activateWork",activation);
            assertTrue(Set.of("GOAL_SLOT_MISSING","VERSION_UNSUPPORTED").contains(denied.at("/error/code").asText()),denied.toPrettyString());
            current=gateway("observer","getWork",query);assertEquals("DRAFT",current.at("/data/status").asText());
            assertEquals(movements,count("mulino_inventory_QuantityMovements"));assertEquals(0,count("mulino_trade_sales_Deliveries"));
        }
    }
    @Test void explicitDispatchDeliverySourceBoundaries()throws Exception {
        String sid="normal-consumed-delivery";
        runActions(sid,Set.of("order","order-delivery-work","activate-order-delivery-work","reserve","pick","dispatch","delivery-source-dispatch","delivery-source-sales-line","delivery-source-document","delivery-source-event","delivery-provisional","delivery-source-observation"));
        assertEquals(1,count("mulino_trade_sales_Observations"));int canonicalBefore=count("mulino_evidence_CanonicalOccurrences");
        for(String field:List.of("physicalScopeId","basisDocumentId","claimId","sourceIdentity","quantity")) {
            var request=actionRequest(sid,"delivery-source-match");request.put("commandIdempotencyKey",UUID.randomUUID().toString());
            ((ObjectNode)request.path("slots")).put(field,field.equals("quantity")?"31":field.equals("sourceIdentity")?"wrong-source:unverified:1":UUID.randomUUID().toString());
            var denied=gateway("t17Source","matchSourceIdentity",request);
            assertFalse(denied.path("outcome").asText().equals("APPLIED")&&denied.path("evidenceStatus").asText().equals("MATCHED"),denied.toPrettyString());
            assertEquals(canonicalBefore,count("mulino_evidence_CanonicalOccurrences"));assertEquals(0,count("mulino_trade_sales_Deliveries"));assertEquals(2,count("mulino_inventory_QuantityMovements"));
        }
        var wrongWork=actionRequest(sid,"delivery-provisional");wrongWork.put("commandIdempotencyKey",UUID.randomUUID().toString());((ObjectNode)wrongWork.path("slots")).put("observationId",UUID.randomUUID().toString()).put("workId",id("t17SourceWork-A"));
        assertNotEquals("APPLIED",gateway("receiver","recordDelivery",wrongWork).path("outcome").asText());
        var missingWork=actionRequest(sid,"delivery-provisional");missingWork.put("commandIdempotencyKey",UUID.randomUUID().toString());((ObjectNode)missingWork.path("slots")).put("observationId",UUID.randomUUID().toString()).put("workId",UUID.randomUUID().toString());
        assertNotEquals("APPLIED",gateway("receiver","recordDelivery",missingWork).path("outcome").asText());
        runActions(sid,Set.of("delivery-source-match","delivery-source-canonical","delivery"));
        var duplicate=actionRequest(sid,"delivery");
        assertEquals("APPLIED",gateway("receiver","recordDelivery",duplicate).path("outcome").asText());
        assertEquals(1,count("mulino_trade_sales_Deliveries"));assertEquals(3,count("mulino_inventory_QuantityMovements"));ledgerEvidence();
    }
    @Test void explicitDispatchNormalDelivery()throws Exception {actualDelivery("normal-consumed-delivery","30","30");}
    @Test void explicitDispatchLateDelivery()throws Exception {actualDelivery("late-restriction-actual-delivery","20","0");}
    @Test void explicitDispatchExpiredDelivery()throws Exception {actualDelivery("post-dispatch-expiry","20","0");}
    void actualClaim(String sid)throws Exception {
        var result=allCommands(sid);assertEquals("ACCEPTED_PENDING_EXTERNAL",result.path("outcome").asText(),result.toPrettyString());
        assertEquals("0",result.path("effects").path("physicalQuantity").asText());assertEquals("0",result.path("effects").path("contributedQuantity").asText());
        assertEquals(1,count("mulino_trade_sales_Observations"));assertEquals(0,count("mulino_trade_sales_Deliveries"));assertEquals(1,count("mulino_inventory_Dispatches"));assertEquals(2,count("mulino_inventory_QuantityMovements"));
        var slots=actionRequest(sid,"claim").path("slots");
        try(var q=connection.prepareStatement("SELECT rangeRootId,claimedRangeRootId,claimPayloadJson,state FROM mulino_trade_sales_Observations WHERE organizationId=?")){q.setString(1,org);try(var r=q.executeQuery()){assertTrue(r.next());assertNull(r.getString(1));assertEquals(slots.path("rangeRootId").asText(),r.getString(2));assertEquals(slots,Json.parse(r.getString(3)));assertEquals("PROVISIONAL",r.getString(4));assertFalse(r.next());}}
        try(var q=connection.prepareStatement("SELECT o.nextAction,o.nextCheckAt,a.kind FROM mulino_work_read_ObligationReferences o JOIN mulino_identity_Actors a ON a.organizationId=o.organizationId AND a.ID=o.ownerId WHERE o.organizationId=? AND o.kind='DELIVERY_RECONCILIATION' AND o.valid AND o.status='OPEN'")){q.setString(1,org);try(var r=q.executeQuery()){assertTrue(r.next());assertEquals(slots.path("nextAction").asText(),r.getString(1));assertEquals(java.time.Instant.parse(slots.path("nextCheckAt").asText()),r.getTimestamp(2).toInstant());assertEquals("HUMAN",r.getString(3));assertFalse(r.next());}}
        try(var q=connection.prepareStatement("SELECT state FROM mulino_inventory_SegmentAllocations WHERE organizationId=?")){q.setString(1,org);try(var r=q.executeQuery()){assertTrue(r.next());assertEquals("CONSUMED",r.getString(1));}}ledgerEvidence();
    }
    @Test void explicitDispatchFalseClaim()throws Exception {actualClaim("claim-falseclaim");}
    @Test void explicitDispatchConflictingClaim()throws Exception {actualClaim("claim-conflictingquantity");}
    @Test void explicitDispatchUnidentifiedClaim()throws Exception {actualClaim("claim-unidentifiedscope");}
    @Test void explicitDispatchOverflowClaim()throws Exception {actualClaim("claim-scopeoverflow");}
    @Test void sellOnlyDoesNotAuthorizeDispatch()throws Exception {
        assertEquals("30",queryEligibility().path("data").path("eligibleQuantity").asText());
        assertEquals("0",queryEligibility("2026-10-07T09:00:00Z","DISPATCH").path("data").path("eligibleQuantity").asText());
        var denied=runActions("normal-consumed-delivery",Set.of("order","reserve","pick","dispatch"));
        assertEquals("INSUFFICIENT_ELIGIBLE_QUANTITY",denied.path("error").path("code").asText());
        assertEquals("Current dispatch authority denied",denied.path("error").path("message").asText());
        assertEquals(0,count("mulino_inventory_Dispatches"));assertEquals(1,count("mulino_inventory_QuantityMovements"));
    }
    @Test void explicitDispatchSupportsOriginalRangeAndRejectsFakeEvidence()throws Exception {
        assertEquals(5,installed.path("sourceFactEvidence").path("dispatchConditions").size());
        assertEquals(6,count("mulino_inventory_DispositionBases"));
        var query=queryEligibility("2026-10-07T09:00:00Z","DISPATCH");assertEquals("30",query.path("data").path("eligibleQuantity").asText(),query.toPrettyString());
        JsonNode receipt=installed.path("sourceFactEvidence").path("dispatchConditions").get(0);
        ObjectNode original;
        try(var s=connection.prepareStatement("SELECT payload FROM mulino_evidence_Events WHERE organizationId=? AND ID=?")){s.setString(1,org);s.setString(2,receipt.path("eventId").asText());try(var r=s.executeQuery()){assertTrue(r.next());original=(ObjectNode)Json.parse(r.getString(1));}}
        original.remove(List.of("operation","sourceDecisionId","sourceVersion","givenEvidenceAlias","syntheticActionInput"));original.put("evidenceId",receipt.path("documentId").asText()).put("reason","Exact original boundary negative").put("nextCheckAt","2026-10-07T10:00:00Z");
        for(String field:List.of("quantity","validUntil","segmentId","evidenceId")) {
            var wrong=original.deepCopy();wrong.put(field,switch(field){case "quantity"->"31";case "validUntil"->"2026-11-01T00:00:00Z";case "segmentId"->UUID.randomUUID().toString();default->id("qc-proof");});
            var result=sources.request("recordDispositionBasis",wrong,1);assertNotEquals("APPLIED",result.path("outcome").asText(),result.toPrettyString());assertEquals(6,count("mulino_inventory_DispositionBases"));
        }
        var dispatched=runActions("normal-consumed-delivery",Set.of("order","reserve","pick","dispatch"));assertEquals("APPLIED",dispatched.path("outcome").asText(),dispatched.toPrettyString());assertEquals(1,count("mulino_inventory_Dispatches"));
        try(var s=connection.prepareStatement("SELECT quantity FROM mulino_inventory_Dispatches WHERE organizationId=?")){s.setString(1,org);try(var r=s.executeQuery()){assertTrue(r.next());assertEquals(0,new BigDecimal("30").compareTo(r.getBigDecimal(1)));}}
    }
    void assertExplicitBlocked(String state)throws Exception {
        var evidence=installed.path("sourceFactEvidence");assertEquals(4,evidence.path("dispatchConditions").size());
        assertTrue(evidence.path("uninstalledConditions").toString().contains(state));
        assertEquals("0",queryEligibility("2026-10-07T09:00:00Z","DISPATCH").path("data").path("eligibleQuantity").asText());
        assertEquals(0,count("mulino_inventory_Dispatches"));
    }
    @Test void explicitDispatchUNKNOWNRemainsBlocked()throws Exception {assertExplicitBlocked("UNKNOWN");}
    @Test void explicitDispatchDENIEDRemainsBlocked()throws Exception {assertExplicitBlocked("DENIED");}
    @Test void explicitDispatchCONFLICTRemainsBlocked()throws Exception {assertExplicitBlocked("CONFLICT");}
    @Test void holdSourceReachesCategoryAuthorizationWithoutGrantExpansion()throws Exception {
        var result=runActions("reserve-pick-dispatch",Set.of("hold-B40-source-document","hold-B40-source-event","hold-B40-source-match","hold-B40-source-canonical","hold-B40"));
        assertEquals("FORBIDDEN",result.path("error").path("code").asText(),result.toPrettyString());
        assertEquals(0,count("mulino_inventory_Restrictions"));
        assertFalse(fixture.path("actors").path("qc").path("grant").path("actions").toString().contains("decideQuality"));
        assertTrue(count("mulino_evidence_CanonicalOccurrences")>0);
    }

    @Test void missingOriginalDueAtRemainsRejectedWithoutOrderEffects()throws Exception {
        JsonNode action=null;for(var sub:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases"))if("replacement-no-revival".equals(sub.path("id").asText()))for(var candidate:sub.path("actions"))if("order".equals(candidate.path("id").asText()))action=candidate;
        assertNotNull(action);action=action.deepCopy();
        // Explicit negative copy of the original missing input.
        ((ObjectNode)action.path("request").path("slots")).remove("dueAt");
        ((ObjectNode)action.path("request").path("provenance")).remove("dueAt");
        assertFalse(action.path("request").path("slots").has("dueAt"));
        var resolver=new org.mulino.verification.ReferenceResolver(new HashMap<>(),aliases);
        var result=gateway("sales","createSalesOrder",(ObjectNode)resolver.resolve(action.path("request")));
        assertEquals("TYPE_INVALID",result.path("error").path("code").asText());assertEquals("Invalid dueAt",result.path("error").path("message").asText());
        assertEquals(0,count("mulino_trade_sales_Orders"));assertEquals(0,count("mulino_inventory_SegmentAllocations"));
    }
    @Test void explicitDispatchRejectsCrossOrganizationEvidence()throws Exception {
        var other=(ObjectNode)Json.read(root.resolve("verification/cases/T17/fixtures/normal-consumed-delivery.json"));
        var bundle=Json.object().put("fixtureHash",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(other.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));bundle.set("fixture",other);
        bundle=(ObjectNode)ActualFixtureBindings.bind(root,bundle,config);
        var otherInstalled=ScenarioFixtureInstaller.install(config,root,bundle,true);
        String foreign=otherInstalled.path("sourceFactEvidence").path("conditions").get(0).path("documentId").asText();
        var result=sources.request("recordDispositionBasis",permission(foreign),1);
        assertNotEquals("APPLIED",result.path("outcome").asText(),result.toPrettyString());
        assertEquals(6,count("mulino_inventory_DispositionBases"));assertEquals(0,count("mulino_inventory_Dispatches"));
    }

    @Test void explicitDispatchLateHoldSourceStopsAtRecallAuthority()throws Exception {
        var result=runActions("late-restriction-actual-delivery",Set.of("order","reserve","pick","dispatch","late-hold-source-scope","late-hold-source-document","late-hold-source-event","late-hold-source-match","late-hold-source-canonical","late-hold"));
        assertEquals("FORBIDDEN",result.path("error").path("code").asText(),result.toPrettyString());
        assertEquals(1,count("mulino_inventory_Dispatches"));assertEquals(0,count("mulino_inventory_Restrictions"));
        assertFalse(fixture.path("actors").path("admin").path("grant").path("actions").toString().contains("decideRecallRestriction"));
    }

    ObjectNode actionRequest(String subcase,String name)throws Exception {
        for(var sub:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases"))if(subcase.equals(sub.path("id").asText()))for(var action:sub.path("actions"))if(name.equals(action.path("id").asText()))
            return (ObjectNode)new org.mulino.verification.ReferenceResolver(actionResults,aliases).resolve(action.path("request"));
        throw new AssertionError("Missing bounded test action "+name);
    }
    @Test void newSyntheticDueDatesReachAppliedForEveryPreviouslyMissingOrder()throws Exception {
        int count=0;
        for(var sub:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases"))for(var action:sub.path("actions"))if("createSalesOrder".equals(action.path("capabilityId").asText())) {
            String sid=sub.path("id").asText(),name=action.path("id").asText();
            if(!(sid.equals("replacement-no-revival")||sid.startsWith("eligibility-")||(sid.equals("reserve-pick-dispatch")&&name.equals("second-order"))))continue;
            var request=actionRequest(sid,name);assertEquals("2026-10-07T10:00:00Z",request.path("slots").path("dueAt").asText());
            assertEquals("APPLIED",gateway("sales","createSalesOrder",request).path("outcome").asText());count++;
        }
        assertEquals(7,count);assertEquals(7,count("mulino_trade_sales_Orders"));
    }
    @Test void newCategoryHoldAppliesWithinOriginalScope()throws Exception {
        var result=runActions("reserve-pick-dispatch",Set.of("hold-B40-source-document","hold-B40-source-event","hold-B40-source-match","hold-B40-source-canonical","hold-B40"));
        assertEquals("APPLIED",result.path("outcome").asText(),result.toPrettyString());assertEquals(1,count("mulino_inventory_Restrictions"));
        var wrong=actionRequest("reserve-pick-dispatch","hold-B40");((ObjectNode)wrong.path("slots")).put("segmentId",id("A"));wrong.put("commandIdempotencyKey",UUID.randomUUID().toString());
        assertEquals("EVIDENCE_UNVERIFIED",gateway("qc","placeHold",wrong).path("error").path("code").asText());assertEquals(1,count("mulino_inventory_Restrictions"));
        int bases=count("mulino_inventory_DispositionBases");
        var deniedRecord=Json.object().put("intentKind","RECORD").put("definitionVersion","definition-v1").put("capabilityId","recordDispositionBasis").put("commandIdempotencyKey",UUID.randomUUID().toString()).put("expectedRevision",1);
        deniedRecord.set("subjectRefs",Json.array());deniedRecord.set("provenance",Json.object().put("source","CONTEXT"));deniedRecord.set("slots",permission(installed.path("sourceFactEvidence").path("conditions").get(0).path("documentId").asText()));
        assertEquals("FORBIDDEN",gateway("qc","recordDispositionBasis",deniedRecord).path("error").path("code").asText());assertEquals(bases,count("mulino_inventory_DispositionBases"));
        // Current grants still compose all dimensions. A wrong WORK/PLACE does
        // not become authorized just because the original ITEM dimension fits.
        String grant;try(var s=connection.prepareStatement("SELECT ID FROM mulino_identity_Grants WHERE organizationId=? AND actorId=?")){s.setString(1,org);s.setString(2,id("qc"));try(var r=s.executeQuery()){assertTrue(r.next());grant=r.getString(1);}}
        for(String dimension:List.of("WORK","PLACE")) {
            sql("INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) VALUES(?,?,?,?)",org,grant,dimension,UUID.randomUUID().toString());
            var denied=actionRequest("reserve-pick-dispatch","hold-B40");denied.put("commandIdempotencyKey",UUID.randomUUID().toString());
            var rejection=gateway("qc","placeHold",denied);
            assertTrue(Set.of("FORBIDDEN","EVIDENCE_UNVERIFIED").contains(rejection.path("error").path("code").asText()),rejection.toPrettyString());
            sql("DELETE FROM mulino_identity_GrantScopes WHERE organizationId=? AND grantId=? AND scopeKind=?",org,grant,dimension);
        }
        assertEquals(1,count("mulino_inventory_Restrictions"));
    }
    @Test void newCategoryEarlyHoldApplies()throws Exception {
        var result=runActions("eligibility-fefo-with-reason",Set.of("early-hold-source-document","early-hold-source-event","early-hold-source-match","early-hold-source-canonical","early-hold"));
        assertEquals("APPLIED",result.path("outcome").asText(),result.toPrettyString());assertEquals(1,count("mulino_inventory_Restrictions"));
    }
    @Test void explicitDispatchNewRecallHoldApplies()throws Exception {
        var result=runActions("late-restriction-actual-delivery",Set.of("order","reserve","pick","dispatch","late-hold-source-scope","late-hold-source-document","late-hold-source-event","late-hold-source-match","late-hold-source-canonical","late-hold"));
        assertEquals("APPLIED",result.path("outcome").asText(),result.toPrettyString());assertEquals(1,count("mulino_inventory_Restrictions"));
    }
    @Test void completedReplacementUsesRealWitnessAndReleaseSource()throws Exception {
        String sid="replacement-no-revival";int originalMovements=count("mulino_inventory_QuantityMovements");
        var held=runActions(sid,Set.of("order","reserve","hold-source-document","hold-source-event","hold-source-match","hold-source-canonical","hold-current-segment","hold"));
        assertEquals("APPLIED",held.path("outcome").asText(),held.toPrettyString());assertEquals(1,count("mulino_inventory_SegmentAllocations"));
        var proof=runActions(sid,Set.of("replace-source-allocation","replace-source-document","replace-source-event","replace-source-match","replace-source-canonical"));assertNotNull(proof);
        var fake=actionRequest(sid,"replace");((ObjectNode)fake.path("slots")).put("evidenceId",id("replacement-proof"));fake.put("commandIdempotencyKey",UUID.randomUUID().toString());
        assertEquals("EVIDENCE_UNVERIFIED",gateway("sales","replaceAllocation",fake).path("error").path("code").asText());assertEquals(1,count("mulino_inventory_SegmentAllocations"));
        byte[] bytes="{\"witnessKind\":\"replacement-proof\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var upload=Json.object().put("sourceNamespace","synthetic-t17-eligibility").put("sourceReference",UUID.randomUUID().toString()).put("mediaType","application/json").put("expectedHash",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))).put("contentBase64",Base64.getEncoder().encodeToString(bytes)).put("provenance","Explicit wrong Work negative source");
        upload.set("subject",Json.object().put("kind","WORK").put("id",id("t17SourceWork-A")));
        var wrongDoc=sources.request("attachEvidence",upload,null);assertEquals("APPLIED",wrongDoc.path("outcome").asText(),wrongDoc.toPrettyString());
        var wrongScope=actionRequest(sid,"replace");((ObjectNode)wrongScope.path("slots")).put("evidenceId",wrongDoc.path("id").asText());wrongScope.put("commandIdempotencyKey",UUID.randomUUID().toString());
        assertEquals("EVIDENCE_UNVERIFIED",gateway("sales","replaceAllocation",wrongScope).path("error").path("code").asText());assertEquals(1,count("mulino_inventory_SegmentAllocations"));
        var payload=actionResults.get("replace-source-event").path("response");
        try(var s=connection.prepareStatement("SELECT payload FROM mulino_evidence_Events WHERE organizationId=? AND ID=?")){s.setString(1,org);s.setString(2,payload.path("id").asText());try(var r=s.executeQuery()){assertTrue(r.next());var original=Json.parse(r.getString(1));assertEquals("replacement-proof",original.path("witnessKind").asText());assertEquals(id("A"),original.path("oldSegmentId").asText());assertEquals(id("B"),original.path("newSegmentId").asText());assertEquals(actionResults.get("order").path("response").path("workId"),original.path("workId"));assertEquals(actionResults.get("order").path("response").path("line").path("id"),original.path("salesLineId"));}}
        assertEquals(originalMovements,count("mulino_inventory_QuantityMovements"));
        var replaced=runActions(sid,Set.of("replace"));assertEquals("APPLIED",replaced.path("outcome").asText(),replaced.toPrettyString());assertEquals(2,count("mulino_inventory_SegmentAllocations"));
        runActions(sid,Set.of("release-hold-source-document","release-hold-source-event","release-hold-source-match","release-hold-source-canonical"));
        var invalidRelease=actionRequest(sid,"release-hold");invalidRelease.put("commandIdempotencyKey",UUID.randomUUID().toString());((ObjectNode)invalidRelease.path("slots")).put("evidenceId",id("qc-proof"));
        assertEquals("EVIDENCE_UNVERIFIED",gateway("qc","releaseHold",invalidRelease).path("error").path("code").asText());
        var released=runActions(sid,Set.of("release-hold"));assertEquals("APPLIED",released.path("outcome").asText(),released.toPrettyString());
        var end=runActions(sid,Set.of("allocation-release"));assertEquals("APPLIED",end.path("outcome").asText(),end.toPrettyString());
        try(var s=connection.prepareStatement("SELECT state FROM mulino_inventory_SegmentAllocations WHERE organizationId=? ORDER BY state")){s.setString(1,org);try(var r=s.executeQuery()){assertTrue(r.next());assertEquals("RELEASED",r.getString(1));assertTrue(r.next());assertEquals("REPLACED",r.getString(1));assertFalse(r.next());}}
        assertEquals(originalMovements,count("mulino_inventory_QuantityMovements"));
        try(var s=connection.prepareStatement("SELECT state FROM mulino_inventory_Restrictions WHERE organizationId=?")){s.setString(1,org);try(var r=s.executeQuery()){assertTrue(r.next());assertEquals("RELEASED",r.getString(1));}}
    }

}
