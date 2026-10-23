package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.Json;

/** Synthetic CSN-shaped source graphs plus explicitly labeled captured source-row replay; no product commands. */
@EnabledIfEnvironmentVariable(named="OBSERVER_PG_TEST",matches="true")
class T17CanonicalDeliveryProjectionPostgresTest {
    final Path root=Path.of(System.getProperty("repo.root"));
    final String org=id(), item=id(), work=id(), lot=id();
    final String now="2026-10-07T02:00:00Z", before="2026-10-07T00:00:00Z", later="2026-10-08T00:00:00Z";
    Connection writer;ActualConfiguration config;String schema;String testName;int captures;
    @org.junit.jupiter.api.io.TempDir Path blobs;String previousBlobRoot;
    final Map<String,ObjectNode> graph=new LinkedHashMap<>();
    static String id(){return UUID.randomUUID().toString();}
    @BeforeEach void setup(TestInfo info) throws Exception {
        testName=info.getTestMethod().orElseThrow().getName()+"-"+info.getDisplayName().replaceAll("[^A-Za-z0-9_-]","_");
        previousBlobRoot=System.getProperty("verification.actual.blobRoot");
        System.setProperty("verification.actual.blobRoot",blobs.toString());Files.createDirectories(blobs.resolve("objects"));
        String url=System.getenv("OBSERVER_PG_URL");schema="obs_"+UUID.randomUUID().toString().replace("-","");
        writer=DriverManager.getConnection(url,"observer","observer-local");
        try(var s=writer.createStatement()){s.execute("CREATE SCHEMA "+schema);s.execute("SET search_path TO "+schema);}
        var csn=Json.read(root.resolve("backend/src/main/resources/edmx/csn.json"));
        for(var it=csn.path("definitions").fields();it.hasNext();) {
            var entry=it.next();if(!entry.getKey().startsWith("mulino.")||!"entity".equals(entry.getValue().path("kind").asText()))continue;
            var columns=new ArrayList<String>();
            for(var fields=entry.getValue().path("elements").fields();fields.hasNext();) {
                var field=fields.next();String type=field.getValue().path("type").asText();
                String sqlType=switch(type){case "cds.Timestamp","cds.DateTime"->"timestamptz";case "cds.Boolean"->"boolean";case "cds.Integer"->"integer";case "cds.Integer64"->"bigint";case "cds.Decimal","cds.Double"->"numeric(38,12)";default->"text";};
                columns.add(field.getKey()+" "+sqlType);
            }
            try(var s=writer.createStatement()){s.execute("CREATE TABLE "+entry.getKey().replace('.','_')+" ("+String.join(",",columns)+")");}
        }
        config=new ActualConfiguration(java.net.URI.create("http://127.0.0.1:1"),url+(url.contains("?")?"&":"?")+"currentSchema="+schema,"observer","observer-local",Path.of("unused"),"unused","unused","648ca638-source-row-test");
    }
    @AfterEach void cleanup() throws Exception {
        if(previousBlobRoot==null)System.clearProperty("verification.actual.blobRoot");else System.setProperty("verification.actual.blobRoot",previousBlobRoot);
        if(writer!=null){try(var s=writer.createStatement()){s.execute("DROP SCHEMA "+schema+" CASCADE");}writer.close();}
    }
    void insert(String table,ObjectNode row) throws Exception {
        var names=new ArrayList<String>();row.fieldNames().forEachRemaining(names::add);
        try(var s=writer.prepareStatement("INSERT INTO "+table+" ("+String.join(",",names)+") VALUES ("+String.join(",",Collections.nCopies(names.size(),"?"))+")")) {
            var columns=writer.getMetaData().getColumns(null,schema,table.toLowerCase(Locale.ROOT),null);var types=new HashMap<String,Integer>();while(columns.next())types.put(columns.getString("COLUMN_NAME"),columns.getInt("DATA_TYPE"));columns.close();
            for(int n=0;n<names.size();n++){var value=row.get(names.get(n));if(value.isNull())s.setObject(n+1,null);else if(types.get(names.get(n).toLowerCase(Locale.ROOT))==Types.TIMESTAMP_WITH_TIMEZONE||types.get(names.get(n).toLowerCase(Locale.ROOT))==Types.TIMESTAMP)s.setObject(n+1,OffsetDateTime.parse(value.asText()));else if(value.isBoolean())s.setBoolean(n+1,value.booleanValue());else if(value.isNumber())s.setObject(n+1,value.numberValue());else s.setObject(n+1,value.asText(),types.get(names.get(n).toLowerCase(Locale.ROOT)));}s.executeUpdate();
        }
    }
    ObjectNode row(String id){return Json.object().put("ID",id).put("organizationId",org).put("recordedAt",now).put("createdAt",before);}
    ObjectNode scope(){return Json.object().put("organizationId",org).put("itemId",item).put("workId",work).put("lotId",lot);}
    ObjectNode request(ObjectNode scope,String... sources){var r=Json.object().put("asOf",now).put("knownAt",now);r.set("scope",scope);r.set("sources",Json.MAPPER.valueToTree(sources));return r;}
    JsonNode capture(String... sources) throws Exception {
        var result=ScenarioJdbcObservation.capture(config,root,request(scope(),sources));
        if(System.getenv("OBSERVER_TEST_EVIDENCE")!=null)Json.write(Path.of(System.getenv("OBSERVER_TEST_EVIDENCE")).resolve(testName+"-"+(++captures)+".json"),result);
        return result;
    }
    ObjectNode fact(String identity) {return row(identity).put("workId",work).put("itemId",item).put("placeId","destination").put("unit","BOX");}
    void sourceGraph(String quantity,String legitimate) throws Exception {
        var d=fact("delivery").put("canonicalOccurrenceId","canonical").put("observationId","observation").put("dispatchId","dispatch")
            .put("cargoScopeId","cargo").put("salesLineId","line").put("customerId","customer").put("lotId",lot)
            .put("rangeRootId","root").put("startQuantity","0").put("quantity",quantity).put("legitimateQuantity",legitimate).put("occurredAt",now);
        graph.put("mulino_trade_sales_Deliveries",d);
        var observation=d.deepCopy().put("ID","observation").put("eventId","event");
        observation.remove(List.of("canonicalOccurrenceId","observationId","legitimateQuantity"));graph.put("mulino_trade_sales_Observations",observation);
        var dispatch=d.deepCopy().put("ID","dispatch").put("destinationId","destination").put("quantity","30");
        dispatch.remove(List.of("canonicalOccurrenceId","observationId","dispatchId","legitimateQuantity","placeId"));graph.put("mulino_inventory_Dispatches",dispatch);
        var payload=d.deepCopy();payload.remove(List.of("ID","organizationId","createdAt","recordedAt","legitimateQuantity","canonicalOccurrenceId","observationId","workId"));
        payload.put("physicalScopeId","observation").put("deliveryEventId","physical-event");
        var occurrence=fact("canonical").put("kind","PHYSICAL_DELIVERY").put("physicalScopeId","observation").put("occurrenceIdentity","physical-event")
            .put("subjectKind","DISPATCH").put("subjectId","dispatch").put("sourceProfileId","profile").put("quantity",quantity)
            .put("valueState","KNOWN").put("reassessmentState","COMPLETE").put("effectiveFrom",now);
        graph.put("mulino_evidence_CanonicalOccurrences",occurrence);
        var claim=occurrence.deepCopy().put("ID","claim").put("eventId","event").put("documentVersionId","document");
        claim.remove(List.of("kind","physicalScopeId","occurrenceIdentity","reassessmentState"));graph.put("mulino_evidence_Claims",claim);
        var event=claim.deepCopy().put("ID","event").put("kind","PHYSICAL_DELIVERY").put("sourceNamespace","delivery-source")
            .put("externalEventId","original").put("sourceVersion","1").put("payloadHash","stored-payload-hash").put("payload",payload.toString());
        event.remove(List.of("eventId","documentVersionId","quantity","unit"));graph.put("mulino_evidence_Events",event);
        String blob=id();byte[] bytes=payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(blobs.resolve("objects").resolve(blob),bytes);
        String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        var document=fact("document").put("subjectKind","DISPATCH").put("subjectId","dispatch").put("sourceProfileId","profile")
            .put("sourceNamespace","delivery-source").put("sourceReference","original").put("availability","AVAILABLE").put("blobId",blob).put("sha256",hash).put("byteLength",bytes.length);
        document.remove("unit");graph.put("mulino_evidence_DocumentVersions",document);
        var verification=row("verification").put("claimId","claim").put("canonicalOccurrenceId","canonical").put("basisDocumentId","document")
            .put("policyVersion","persisted-policy").put("verdict","VERIFIED");
        for(String flag:List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked"))verification.put(flag,true);
        graph.put("mulino_evidence_Verifications",verification);
        graph.put("mulino_evidence_SourceProfiles",row("profile").put("namespace","delivery-source").put("policyVersion","persisted-policy"));
        graph.put("mulino_evidence_InboxRecords",row("inbox").put("eventId","event").put("sourceNamespace","delivery-source")
            .put("externalEventId","original").put("sourceVersion","1").put("payloadHash","stored-payload-hash").put("state","ACCEPTED"));
    }
    void installGraph() throws Exception {for(var entry:graph.entrySet())insert(entry.getKey(),entry.getValue());}
    JsonNode projected() throws Exception {return capture("deliveries").path("rawRows").path("deliveries").get(0);}
    ObjectNode source(String suffix){return graph.entrySet().stream().filter(e->e.getKey().endsWith("_"+suffix)).findFirst().orElseThrow().getValue();}

    @ParameterizedTest @ValueSource(strings={"normal","lateRestriction","expiry"})
    void canonicalPhysicalQuantityNeverBecomesLegitimatePermission(String variant) throws Exception {
        sourceGraph(variant.equals("normal")?"30":"20",variant.equals("normal")?"30":"0");installGraph();
        var result=capture("deliveries");var actual=result.path("rawRows").path("deliveries").get(0);
        assertTrue(actual.path("canonical").asBoolean());assertEquals("canonical",actual.path("canonicalSource").path("canonicalOccurrenceId").asText());
        assertEquals(0,new java.math.BigDecimal(source("Deliveries").path("quantity").asText()).compareTo(new java.math.BigDecimal(actual.path("quantity").asText())));
        assertEquals(0,new java.math.BigDecimal(variant.equals("normal")?"30":"0").compareTo(new java.math.BigDecimal(actual.path("legitimateQuantity").asText())));
        assertEquals("REPEATABLE_READ",result.path("snapshot").path("isolation").asText());
        for(String name:List.of("eligibilityStatus","contributedQuantity","goalResult"))assertFalse(actual.has(name));
        assertTrue(result.path("sourceEvidence").toString().contains("r.organizationId=?"));
        try(var s=writer.createStatement();var r=s.executeQuery("SELECT count(*) FROM mulino_trade_sales_Deliveries")){assertTrue(r.next());assertEquals(1,r.getInt(1));}
    }

    @Test void reverificationOfTheSameCanonicalSourceKeepsOnePhysicalDeliveryAndEveryProof() throws Exception {
        sourceGraph("20","0");
        insert("mulino_evidence_Verifications",source("Verifications").deepCopy().put("ID","second"));
        installGraph();
        var result=capture("deliveries");var rows=result.path("rawRows").path("deliveries");
        assertEquals(1,rows.size());var actual=rows.get(0);
        assertTrue(actual.path("canonical").asBoolean(),"A second valid verification of the same physical fact is not ambiguity");
        assertEquals("20",actual.path("quantity").asText());assertEquals("0",actual.path("legitimateQuantity").asText());
        var proof=actual.path("canonicalSource");assertEquals("canonical",proof.path("canonicalOccurrenceId").asText());
        assertEquals("observation",proof.path("observationId").asText());assertEquals("dispatch",proof.path("dispatchId").asText());
        assertFalse(proof.has("verificationId"));var chains=proof.path("verificationChains");assertEquals(2,chains.size());
        assertEquals("second",chains.get(0).path("verificationId").asText());assertEquals("verification",chains.get(1).path("verificationId").asText());
        for(var chain:chains){assertEquals("claim",chain.path("claimId").asText());assertEquals("event",chain.path("eventId").asText());assertEquals("document",chain.path("documentVersionId").asText());}
    }

    @ParameterizedTest @ValueSource(strings={"independentClaimDocument","futureVerification","rejectedVerification","conflictingProofInbox"})
    void additionalProofsPreserveValidSourcesWithoutDuplicatingQuantity(String variant) throws Exception {
        sourceGraph("20","0");
        var second=source("Verifications").deepCopy().put("ID","second");
        if(variant.equals("independentClaimDocument")) {
            insert("mulino_evidence_DocumentVersions",source("DocumentVersions").deepCopy().put("ID","second-document"));
            insert("mulino_evidence_Claims",source("Claims").deepCopy().put("ID","second-claim").put("documentVersionId","second-document"));
            second.put("claimId","second-claim").put("basisDocumentId","second-document");
        }
        if(variant.equals("futureVerification"))second.put("recordedAt",later);
        if(variant.equals("rejectedVerification"))second.put("verdict","REJECTED");
        if(variant.equals("conflictingProofInbox")) {
            var event=source("Events").deepCopy().put("ID","second-event").put("sourceVersion","2");
            insert("mulino_evidence_Events",event);
            insert("mulino_evidence_Claims",source("Claims").deepCopy().put("ID","second-claim").put("eventId","second-event"));
            insert("mulino_evidence_InboxRecords",source("InboxRecords").deepCopy().put("ID","second-inbox").put("eventId","second-event").put("sourceVersion","2").put("state","CONFLICT"));
            second.put("claimId","second-claim");
        }
        insert("mulino_evidence_Verifications",second);installGraph();
        var rows=capture("deliveries").path("rawRows").path("deliveries");assertEquals(1,rows.size());var actual=rows.get(0);
        assertTrue(actual.path("canonical").asBoolean());assertEquals("20",actual.path("quantity").asText());assertEquals("0",actual.path("legitimateQuantity").asText());
        var chains=actual.path("canonicalSource").path("verificationChains");assertEquals(variant.equals("independentClaimDocument")?2:1,chains.size());
        if(variant.equals("independentClaimDocument")) {
            assertEquals("second",chains.get(0).path("verificationId").asText());assertEquals("second-claim",chains.get(0).path("claimId").asText());
            assertEquals("second-document",chains.get(0).path("documentVersionId").asText());assertEquals("event",chains.get(0).path("eventId").asText());
        }
        assertEquals("verification",chains.get(chains.size()-1).path("verificationId").asText());
    }

    @ParameterizedTest @ValueSource(strings={"otherCanonical","otherWork","foreignOrg","wrongType","wrongPhysicalScope","wrongItem","wrongUnit","wrongTime","wrongDispatch","outsideDispatch","wrongPayload","unverified","futureSource","futureVerification","futureCanonical","missingDocument","missingOriginal","corruptOriginal","superseded","competingCanonical","multipleOwners","inboxConflict","wrongPolicy","wrongClaimedRoot","wrongOriginalNamespace","missingWork","endedSource"})
    void invalidOrAmbiguousStoredSourceLeavesCanonicalUnknown(String variant) throws Exception {
        sourceGraph("20","0");
        switch(variant) {
            case "otherCanonical"->source("Deliveries").put("canonicalOccurrenceId","other");
            case "otherWork"->source("CanonicalOccurrences").put("workId","other");
            case "foreignOrg"->source("Claims").put("organizationId","foreign");
            case "wrongType"->source("CanonicalOccurrences").put("kind","RETURN_RECEIPT");
            case "wrongPhysicalScope"->source("CanonicalOccurrences").put("physicalScopeId","other");
            case "wrongItem"->source("CanonicalOccurrences").put("itemId","other");
            case "wrongUnit"->source("Claims").put("unit","KG");
            case "wrongTime"->source("CanonicalOccurrences").put("effectiveFrom",later);
            case "wrongDispatch"->source("Dispatches").put("workId","other");
            case "outsideDispatch"->source("Dispatches").put("quantity","10");
            case "wrongPayload"->source("Events").put("payload","{}");
            case "unverified"->source("Verifications").put("quantityMatched",false);
            case "futureSource"->source("Events").put("recordedAt",later);
            case "futureVerification"->source("Verifications").put("recordedAt",later);
            case "futureCanonical"->source("CanonicalOccurrences").put("recordedAt",later);
            case "missingDocument"->source("Verifications").put("basisDocumentId","absent");
            case "missingOriginal"->Files.delete(blobs.resolve("objects").resolve(source("DocumentVersions").path("blobId").asText()));
            case "corruptOriginal"->Files.writeString(blobs.resolve("objects").resolve(source("DocumentVersions").path("blobId").asText()),"{}");
            case "superseded"->insert("mulino_evidence_Events",row("new-event").put("supersedesId","event"));
            case "competingCanonical"->insert("mulino_evidence_CanonicalOccurrences",source("CanonicalOccurrences").deepCopy().put("ID","competing"));
            case "multipleOwners"->insert("mulino_trade_sales_Deliveries",source("Deliveries").deepCopy().put("ID","second"));
            case "inboxConflict"->source("InboxRecords").put("state","CONFLICT");
            case "wrongPolicy"->source("SourceProfiles").put("policyVersion","other");
            case "wrongClaimedRoot"->source("Observations").put("claimedRangeRootId","other");
            case "wrongOriginalNamespace"->source("DocumentVersions").put("sourceNamespace","other");
            case "endedSource"->source("Claims").put("effectiveUntil",before);
            case "missingWork"->{for(var row:graph.values())if(row.has("workId"))row.putNull("workId");}
        }
        installGraph();var actual=variant.equals("missingWork")
            ?ScenarioJdbcObservation.capture(config,root,request(scope().without("workId"),"deliveries")).path("rawRows").path("deliveries").get(0)
            :projected();assertFalse(actual.has("canonical"),variant);assertFalse(actual.has("canonicalSource"),variant);
        assertEquals(0,new java.math.BigDecimal(actual.path("legitimateQuantity").asText()).signum());
        assertEquals(0,new java.math.BigDecimal(actual.path("quantity").asText()).compareTo(new java.math.BigDecimal("20")));
    }

    @Test void joinsKeepTheOriginalReadOnlySnapshotAcrossConcurrentSourceChange() throws Exception {
        sourceGraph("20","0");installGraph();
        try(var reader=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {
            reader.setAutoCommit(false);reader.setReadOnly(true);reader.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            var projection=new ScenarioObservationProjection(reader,root,List.of(org),List.of(item),List.of(work),List.of(lot),OffsetDateTime.parse(now));
            var rows=projection.read("mulino_trade_sales_Deliveries");
            try(var s=writer.createStatement()){s.executeUpdate("UPDATE mulino_evidence_Verifications SET verdict='REJECTED'");}
            projection.enrich("mulino_trade_sales_Deliveries",rows);assertTrue(rows.get(0).path("canonical").asBoolean());
            assertTrue(reader.isReadOnly());assertEquals(Connection.TRANSACTION_REPEATABLE_READ,reader.getTransactionIsolation());
            assertThrows(SQLException.class,()->{try(var s=reader.createStatement()){s.executeUpdate("DELETE FROM mulino_trade_sales_Deliveries");}});reader.rollback();
        }
        assertFalse(projected().has("canonical"));
    }

    @Test void capturedDeliveryRowsWithoutUncapturedEvidenceStayUnknownInFreshPostgres() throws Exception {
        String path=System.getenv("T17_CANONICAL_REPLAY");Assumptions.assumeTrue(path!=null);
        var report=Json.read(Path.of(path));int cases=0;
        for(var c:report.path("cases"))if(Set.of("normal-consumed-delivery","late-restriction-actual-delivery","post-dispatch-expiry").contains(c.path("subcaseId").asText())) {
            var action=c.path("actions").path("after-delivery-db");var d=action.path("data").path("rawRows").path("deliveries").get(0);
            assertNotNull(d);var stored=(ObjectNode)d.deepCopy();stored.set("ID",stored.remove("id"));
            var names=ScenarioJdbcObservation.names(root,"mulino_trade_sales_Deliveries");var remove=new ArrayList<String>();stored.fieldNames().forEachRemaining(k->{if(!names.containsValue(k))remove.add(k);});stored.remove(remove);
            insert("mulino_trade_sales_Deliveries",stored);
            var request=request(Json.object().put("organizationId",d.path("organizationId").asText()),"deliveries")
                .put("knownAt",action.path("data").path("knownAt").asText());
            var result=ScenarioJdbcObservation.capture(config,root,request);var actual=result.path("rawRows").path("deliveries").get(0);
            assertFalse(actual.has("canonical")); // this capture has no canonical/verification/document rows or original blobs
            assertEquals(0,new java.math.BigDecimal(d.path("quantity").asText()).compareTo(new java.math.BigDecimal(actual.path("quantity").asText())));
            assertEquals(0,new java.math.BigDecimal(d.path("legitimateQuantity").asText()).compareTo(new java.math.BigDecimal(actual.path("legitimateQuantity").asText())));
            if(System.getenv("OBSERVER_TEST_EVIDENCE")!=null)Json.write(Path.of(System.getenv("OBSERVER_TEST_EVIDENCE")).resolve(c.path("subcaseId").asText()+"-source-row-replay.json"),result);
            cases++;
        }
        assertEquals(3,cases);
    }
}
