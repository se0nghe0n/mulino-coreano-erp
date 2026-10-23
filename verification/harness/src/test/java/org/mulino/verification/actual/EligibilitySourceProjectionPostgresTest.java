package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mulino.verification.Json;
import static org.junit.jupiter.api.Assertions.*;

/** JDBC source identity/bytes/time tests, independent of the product eligibility evaluator. */
@EnabledIfEnvironmentVariable(named="OBSERVER_PG_TEST",matches="true")
class EligibilitySourceProjectionPostgresTest {
    final ScenarioObservationPostgresTest f=new ScenarioObservationPostgresTest();
    final String segment=id(),regPolicy=id(),procedure=id(),version=id(),profile=id();
    Path blobs;String priorBlobRoot;
    record Chain(String document,String canonical,String event,String blob){}
    static String id(){return UUID.randomUUID().toString();}
    @BeforeEach void setup(TestInfo info)throws Exception {
        f.setup(info);f.base();priorBlobRoot=System.getProperty("verification.actual.blobRoot");
        blobs=Files.createTempDirectory("eligibility-source-bytes-");Files.createDirectories(blobs.resolve("objects"));System.setProperty("verification.actual.blobRoot",blobs.toString());
        f.insert("mulino_inventory_TradeItems",f.row(f.item).put("baseUnit","BOX").put("packagingVersionId",id()).put("specificationVersionId",id()));
        f.insert("mulino_inventory_ManufacturingLots",f.row(f.lot).put("itemId",f.item).put("expiresAt",f.later));
        f.insert("mulino_inventory_QuantitySegments",f.row(segment).put("itemId",f.item).put("lotId",f.lot).put("quantity","20").put("unit","BOX").put("controlScope","leaf").put("validFrom",f.before));
        f.insert("mulino_governance_ActivePolicies",Json.object().put("organizationId",f.org).put("kind","ELIGIBILITY").put("policyId",f.policy));
        f.insert("mulino_trade_regulatory_Policies",f.row(regPolicy).put("action","SELL").put("status","ACTIVE").put("requiresLabel",true).put("validFrom",f.before).put("validUntil",f.later));
        f.insert("mulino_evidence_SourceProfiles",f.row(profile).put("namespace","source-fixture").put("policyVersion","source-policy-v1"));
    }
    @AfterEach void cleanup()throws Exception {
        try{f.cleanup();}finally{
            if(priorBlobRoot==null)System.clearProperty("verification.actual.blobRoot");else System.setProperty("verification.actual.blobRoot",priorBlobRoot);
            if(blobs!=null)try(var paths=Files.walk(blobs)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}
        }
    }
    JsonNode capture()throws Exception{return f.capture("sellConditionSourceRefs","sellConditionSourceLinks");}
    ObjectNode quality(String table)throws Exception {
        var row=f.row(id()).put("segmentId",segment).put("workId",f.work).put("category","QC").put("action","SELL").put("state","ACTIVE").put("quantity","20").put("startQuantity","0").put("unit","BOX").put("controlScope","leaf").put("validFrom",f.before).put("validUntil",f.later);
        var chain=source(row,table.endsWith("Restrictions")?"QUALITY_DECISION_placeHold_QC":"QUALITY_DECISION_recordDispositionBasis_QC");
        row.put("evidenceRef",chain.document());f.insert(table,row);return row;
    }
    Chain source(ObjectNode stored,String kind)throws Exception {
        String document=id(),canonical=id(),event=id(),claim=id(),blob=id(),external=id();
        var payload=Json.object();
        for(String key:List.of("category","action","segmentId","quantity","unit","startQuantity","validFrom","validUntil","procedureId","procedureVersionId","decision","packagingVersionId","specificationVersionId","occurredAt"))if(stored.hasNonNull(key))payload.set(key,stored.path(key));
        byte[] bytes=payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);Files.write(blobs.resolve("objects").resolve(blob),bytes);
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        var fact=f.row(canonical).put("itemId",f.item).put("physicalScopeId",segment).put("subjectKind","SEGMENT").put("subjectId",segment).put("sourceProfileId",profile).put("kind",kind).put("valueState","KNOWN").put("reassessmentState","COMPLETE").put("effectiveFrom",f.before);
        f.insert("mulino_evidence_CanonicalOccurrences",fact);
        var e=f.row(event).put("itemId",f.item).put("subjectKind","SEGMENT").put("subjectId",segment).put("sourceProfileId",profile).put("sourceNamespace","source-fixture").put("externalEventId",external).put("sourceVersion","1").put("kind",kind).put("valueState","KNOWN").put("payloadHash",hash).put("payload",payload.toString()).put("effectiveFrom",f.before);
        f.insert("mulino_evidence_Events",e);
        f.insert("mulino_evidence_Claims",f.row(claim).put("itemId",f.item).put("subjectKind","SEGMENT").put("subjectId",segment).put("sourceProfileId",profile).put("documentVersionId",document).put("eventId",event).put("effectiveFrom",f.before));
        f.insert("mulino_evidence_DocumentVersions",f.row(document).put("subjectKind","SEGMENT").put("subjectId",segment).put("sourceProfileId",profile).put("availability","AVAILABLE").put("blobId",blob).put("sha256",hash).put("byteLength",bytes.length));
        var verification=f.row(id()).put("claimId",claim).put("basisDocumentId",document).put("canonicalOccurrenceId",canonical).put("verdict","VERIFIED").put("policyVersion","source-policy-v1");
        for(String flag:List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked"))verification.put(flag,true);
        f.insert("mulino_evidence_Verifications",verification);
        f.insert("mulino_evidence_InboxRecords",f.row(id()).put("sourceNamespace","source-fixture").put("externalEventId",external).put("sourceVersion","1").put("eventId",event).put("payloadHash",hash).put("state","ACCEPTED"));
        return new Chain(document,canonical,event,blob);
    }
    void procedure()throws Exception {
        f.insert("mulino_trade_regulatory_Procedures",f.row(procedure).put("itemId",f.item).put("lotId",f.lot).put("physicalScopeId",segment).put("policyId",regPolicy).put("currentVersionId",version).put("workId",f.work));
        var v=f.row(version).put("procedureId",procedure).put("status","SUBMITTED").put("workId",f.work).put("occurredAt",f.before);var chain=source(v,"REGULATORY_SUBMISSION");v.put("occurrenceId",chain.canonical());f.insert("mulino_trade_regulatory_ProcedureVersions",v);
    }
    ObjectNode decision(String state)throws Exception {
        return f.row(id()).put("itemId",f.item).put("lotId",f.lot).put("physicalScopeId",segment).put("policyId",regPolicy).put("procedureId",procedure).put("procedureVersionId",version).put("action","SELL").put("decision",state).put("quantity","20").put("startQuantity","0").put("unit","BOX").put("validFrom",f.before).put("validUntil",f.later);
    }
    JsonNode condition(JsonNode result,String condition){for(var row:result.at("/rawRows/sellConditionSourceRefs"))if(condition.equals(row.path("conditionId").asText()))return row;throw new AssertionError(condition);}
    @Test void currentPermissionAndHoldKeepBothActualDocumentsWithoutInventingAConditionStatus()throws Exception {
        var permission=quality("mulino_inventory_DispositionBases");var hold=quality("mulino_inventory_Restrictions");
        var result=capture();var qc=condition(result,"QC");assertEquals(5,result.at("/rawRows/sellConditionSourceRefs").size());
        assertEquals(new TreeSet<>(List.of(permission.path("evidenceRef").asText(),hold.path("evidenceRef").asText())),new TreeSet<>(Json.MAPPER.convertValue(qc.path("evidenceRefs"),List.class)));
        assertFalse(qc.has("status"));assertFalse(qc.has("evaluatorVersion"));assertFalse(qc.has("eligibleQuantity"));
        assertEquals(f.item,qc.at("/contextSources/item/id").asText());assertEquals(f.lot,qc.at("/contextSources/lot/id").asText());
        var links=result.at("/rawRows/sellConditionSourceLinks");assertEquals(2,links.size());
        for(var row:links){assertEquals(1,row.path("verificationChains").size());assertEquals(segment,row.path("segmentId").asText());assertEquals(row.path("evidenceId"),row.at("/verificationChains/0/documentId"));}
        assertTrue(condition(result,"REGULATORY").path("evidenceRefs").isEmpty());assertTrue(condition(result,"LABEL").path("evidenceRefs").isEmpty());
    }
    @Test void twoCurrentContradictoryDecisionsAndLabelRemainDistinctOriginalCanonicalSources()throws Exception {
        procedure();var expected=new TreeSet<String>();
        for(String state:List.of("ALLOWED","REVOKED")){var row=decision(state);var chain=source(row,"REGULATORY_DECISION");expected.add(chain.canonical());row.put("occurrenceId",chain.canonical());f.insert("mulino_trade_regulatory_DecisionVersions",row);}
        var label=decision("VERIFIED").put("revision",4);label.remove(List.of("action","quantity","startQuantity","unit"));var proof=source(label,"LABEL_VERIFICATION");label.put("occurrenceId",proof.canonical());f.insert("mulino_trade_regulatory_LabelVerifications",label);
        var result=capture();assertEquals(expected,new TreeSet<>(Json.MAPPER.convertValue(condition(result,"REGULATORY").path("evidenceRefs"),List.class)));
        assertEquals(Json.array().add(proof.canonical()),condition(result,"LABEL").path("evidenceRefs"));
        var states=new HashSet<String>();for(var row:result.at("/rawRows/sellConditionSourceLinks"))if("REGULATORY".equals(row.path("conditionId").asText()))states.add(row.at("/sourceRow/decision").asText());assertEquals(Set.of("ALLOWED","REVOKED"),states);
    }
    @ParameterizedTest @ValueSource(strings={"foreign-document","future-document","unavailable-bytes","changed-bytes","wrong-segment","wrong-payload","superseded-event"})
    void brokenStoredSourceCannotBePublishedAsACompleteReference(String mutation)throws Exception {
        var permission=quality("mulino_inventory_DispositionBases");String document=permission.path("evidenceRef").asText();
        if(mutation.equals("foreign-document"))execute("UPDATE mulino_evidence_DocumentVersions SET organizationId=? WHERE ID=?",id(),document);
        if(mutation.equals("future-document"))execute("UPDATE mulino_evidence_DocumentVersions SET recordedAt=?::timestamptz WHERE ID=?",f.later,document);
        if(mutation.equals("unavailable-bytes")||mutation.equals("changed-bytes")){try(var paths=Files.list(blobs.resolve("objects"))){var p=paths.findFirst().orElseThrow();if(mutation.equals("changed-bytes"))Files.writeString(p,"changed");else Files.delete(p);}}
        if(mutation.equals("wrong-segment"))execute("UPDATE mulino_evidence_CanonicalOccurrences SET physicalScopeId=? WHERE organizationId=?",id(),f.org);
        if(mutation.equals("wrong-payload"))execute("UPDATE mulino_inventory_DispositionBases SET quantity=19 WHERE ID=?",permission.path("ID").asText());
        if(mutation.equals("superseded-event")){String event;try(var s=f.writer.createStatement();var r=s.executeQuery("SELECT ID FROM mulino_evidence_Events")){r.next();event=r.getString(1);}f.insert("mulino_evidence_Events",f.row(id()).put("invalidatesId",event));}
        assertThrows(UnsupportedOperationException.class,this::capture);
    }
    @Test void labelNotRequiredUsesOnlyTheActualCurrentPolicyIdentity()throws Exception {
        execute("UPDATE mulino_trade_regulatory_Policies SET requiresLabel=false WHERE ID=?",regPolicy);var result=capture();
        assertEquals(Json.array().add(regPolicy),condition(result,"LABEL").path("evidenceRefs"));assertEquals(regPolicy,result.at("/rawRows/sellConditionSourceLinks/0/sourceId").asText());
    }
    @Test void inheritedPhysicalScopeIsExplicitlyUnavailable()throws Exception {
        f.insert("mulino_inventory_GenealogyEdges",f.row(id()).put("sourceId",segment).put("targetId",id()));assertThrows(UnsupportedOperationException.class,this::capture);
    }
    void execute(String sql,String...values)throws Exception {try(var s=f.writer.prepareStatement(sql)){for(int i=0;i<values.length;i++)s.setString(i+1,values[i]);s.executeUpdate();}}
}
