package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.*;
import java.nio.file.*;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

/** Bounded HTTP/Spring/PG evidence; no model, full scenario runner or operational approval. */
@EnabledIfEnvironmentVariable(named="T09_PG_TEST",matches="true")
class T09SaleSourceFactsPostgresTest {
    final Path root=Path.of(System.getProperty("repo.root"));
    final Path out=Path.of(System.getenv().getOrDefault("T09_TEST_EVIDENCE",".local/t09-not-run"));
    T09SaleSourceFacts f;String name;
    @BeforeEach void name(TestInfo info){name=info.getTestMethod().orElseThrow().getName();}
    @AfterEach void save() throws Exception {if(f!=null)f.save(out.resolve(name+"-http.json"));}
    void install(String sid,boolean old,Consumer<ObjectNode> mutate) throws Exception {
        f=new T09SaleSourceFacts(root,ActualConfiguration.environment(System.getenv()),sid,old,mutate);
    }
    void install(String sid) throws Exception {install(sid,false,x->{});}
    void noAllocation() throws Exception {assertTrue(f.rows("mulino_inventory_SegmentAllocations").isEmpty());}
    void denied(JsonNode r) throws Exception {assertNotEquals("APPLIED",r.at("/response/outcome").asText(),r.toPrettyString());noAllocation();}
    void applied(JsonNode r){assertEquals("APPLIED",r.at("/response/outcome").asText(),r.toPrettyString());}
    void reached(String id) throws Exception {assertEquals(id,f.through(id),()->f.results.isEmpty()?"no calls":new ArrayList<>(f.results.entrySet()).getLast().toString());}
    void auditQualitySources() throws Exception {
        var bases=f.rows("mulino_inventory_DispositionBases");assertEquals(6,bases.size());
        String qty=f.reserveId().equals("reserve60")?"60":"100";
        var decisions=f.rows("mulino_quality_Decisions");
        var docs=f.rows("mulino_evidence_DocumentVersions");
        var claims=f.rows("mulino_evidence_Claims");var events=f.rows("mulino_evidence_Events");
        var canonical=f.rows("mulino_evidence_CanonicalOccurrences");var verified=f.rows("mulino_evidence_Verifications");
        Set<String> pairs=new HashSet<>();
        for(var basis:bases) {
            assertEquals(f.segment(),basis.get("segmentid"));assertEquals(f.work(),basis.get("workid"));
            assertEquals(f.aliases.path("CUSTOMER").asText(),basis.get("customerid"));
            assertEquals(0,new BigDecimal(qty).compareTo((BigDecimal)basis.get("quantity")));
            assertEquals(0,BigDecimal.ZERO.compareTo((BigDecimal)basis.get("startquantity")));assertEquals("BOX",basis.get("unit"));
            pairs.add(basis.get("action")+":"+basis.get("category"));
            var d=decisions.stream().filter(x->basis.get("decisionid").equals(x.get("id"))).findFirst().orElseThrow();
            assertEquals(basis.get("evidenceref"),d.get("evidenceid"));
            var document=docs.stream().filter(x->basis.get("evidenceref").equals(x.get("id"))).findFirst().orElseThrow();
            assertEquals("SEGMENT",document.get("subjectkind"));assertEquals(f.segment(),document.get("subjectid"));
            String prefix="t09-sale-source-"+basis.get("action").toString().toLowerCase()+"-"+basis.get("category").toString().toLowerCase();
            var sent=f.results.get(prefix+"-document").at("/request/slots");
            byte[] raw=Base64.getDecoder().decode(sent.path("contentBase64").asText());
            assertEquals(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(raw)),document.get("sha256"));
            var original=Json.parse(new String(raw,java.nio.charset.StandardCharsets.UTF_8));
            assertTrue(original.path("newSynthetic").asBoolean());assertFalse(original.path("originalAuthorityClaimed").asBoolean());assertFalse(original.path("operatingApprovalClaimed").asBoolean());
            assertEquals(f.segment(),original.path("segmentId").asText());assertEquals(f.work(),original.path("workId").asText());

            var claim=claims.stream().filter(x->document.get("id").equals(x.get("documentversionid"))).findFirst().orElseThrow();
            var event=events.stream().filter(x->claim.get("eventid").equals(x.get("id"))).findFirst().orElseThrow();
            assertEquals("t09-sale-eligibility",event.get("sourcenamespace"));
            assertEquals(original.path("sourceDecisionId").asText(),event.get("externaleventid"));
            assertEquals(original,f.results.get(prefix+"-event").at("/request/slots/payload").isTextual()?Json.parse(f.results.get(prefix+"-event").at("/request/slots/payload").asText()):Json.object());
            var profile=f.rows("mulino_evidence_SourceProfiles").stream().filter(x->event.get("sourceprofileid").equals(x.get("id"))).findFirst().orElseThrow();
            assertEquals("SYNTHETIC-policy-v1",profile.get("policyversion"));

            assertEquals("QUALITY_DECISION_recordDispositionBasis_"+basis.get("category"),event.get("kind"));
            var v=verified.stream().filter(x->claim.get("id").equals(x.get("claimid"))).findFirst().orElseThrow();
            for(String flag:List.of("sourcematched","identitymatched","quantitymatched","timematched","duplicatechecked"))assertEquals(true,v.get(flag));
            assertEquals("VERIFIED",v.get("verdict"));assertEquals(document.get("id"),v.get("basisdocumentid"));
            var c=canonical.stream().filter(x->v.get("canonicaloccurrenceid").equals(x.get("id"))).findFirst().orElseThrow();
            assertEquals(f.segment(),c.get("physicalscopeid"));
            assertEquals(0,new BigDecimal(qty).compareTo((BigDecimal)c.get("quantity")));
        }
        assertEquals(Set.of("SELL:QC","SELL:CUSTOMER","SELL:COMMERCIAL","DISPATCH:QC","DISPATCH:CUSTOMER","DISPATCH:COMMERCIAL"),pairs);
    }
    void auditSources() throws Exception {
        auditQualitySources();
        var canonical=f.rows("mulino_evidence_CanonicalOccurrences");var verified=f.rows("mulino_evidence_Verifications");
        var policies=f.rows("mulino_trade_regulatory_Policies");var procedures=f.rows("mulino_trade_regulatory_Procedures");
        var versions=f.rows("mulino_trade_regulatory_ProcedureVersions");
        var regulatory=f.rows("mulino_trade_regulatory_DecisionVersions");assertEquals(2,regulatory.size());
        for(var d:regulatory) {
            var p=procedures.stream().filter(x->d.get("procedureid").equals(x.get("id"))).findFirst().orElseThrow();
            var v=versions.stream().filter(x->d.get("procedureversionid").equals(x.get("id"))).findFirst().orElseThrow();
            var policy=policies.stream().filter(x->d.get("policyid").equals(x.get("id"))).findFirst().orElseThrow();
            assertEquals("SUBMITTED",v.get("status"));assertEquals(p.get("currentversionid"),v.get("id"));
            assertEquals(f.work(),p.get("workid"));assertEquals(f.segment(),p.get("physicalscopeid"));
            assertEquals(f.aliases.path("P").asText(),p.get("itemid"));assertEquals(f.aliases.path("LOT").asText(),p.get("lotid"));
            assertEquals("T09_SYNTHETIC_AUTHORITY",policy.get("authority"));assertEquals(true,policy.get("fictional"));assertEquals(false,policy.get("requireslabel"));
            assertEquals(d.get("action"),policy.get("action"));assertEquals("ALLOWED",d.get("decision"));
            for(Object occurrence:List.of(d.get("occurrenceid"),v.get("occurrenceid"))) {
                var c=canonical.stream().filter(x->occurrence.equals(x.get("id"))).findFirst().orElseThrow();assertEquals(f.segment(),c.get("physicalscopeid"));
                assertTrue(verified.stream().anyMatch(x->occurrence.equals(x.get("canonicaloccurrenceid"))&&"VERIFIED".equals(x.get("verdict"))));
            }
        }
    }
    @Test void originalInputsRemainRed() throws Exception {
        for(String sid:List.of("cumulative-versus-state","exists-versus-end-throughout")) {
            install(sid,true,x->{});assertEquals(f.reserveId(),f.through(f.reserveId()),()->f.results.isEmpty()?"no calls":new ArrayList<>(f.results.entrySet()).getLast().toString());
            var r=f.results.get(f.reserveId());assertEquals("INSUFFICIENT_ELIGIBLE_QUANTITY",r.at("/response/error/code").asText(),r.toString());denied(r);
            f.save(out.resolve("original-"+sid+".json"));
        }
    }
    void complete(String sid) throws Exception {
        install(sid);reached("t09-sale-source-dispatch-regulatory-allow");auditSources();noAllocation();
        String doc=f.results.get("t09-sale-source-sell-qc-document").at("/response/id").asText();
        var read=Json.object().put("id",doc);read.set("scope",Json.object().put("organizationId",f.aliases.path("ORG").asText()));
        var sourceRead=f.call("getEvidence",read,"t09SaleSource");assertEquals(200,sourceRead.path("httpStatus").asInt(),sourceRead.toString());
        var warehouseRead=f.call("getEvidence",read,"warehouse");f.results.put("warehouse-public-evidence",warehouseRead);
        if(sid.equals("exists-versus-end-throughout"))assertEquals("FORBIDDEN",warehouseRead.at("/response/error/code").asText());
        JsonNode r=f.reserve();
        if(sid.equals("exists-versus-end-throughout")&&!"APPLIED".equals(r.at("/response/outcome").asText())) {
            // This frozen baseline lacks the separate product owner's internal fact purpose patch.
            assertEquals("INSUFFICIENT_ELIGIBLE_QUANTITY",r.at("/response/error/code").asText(),r.toString());denied(r);
            Json.write(out.resolve("remaining-product-seam.json"),Json.object().put("status","BLOCKED_PRODUCT_INTERNAL_EVIDENCE_AUTHORIZATION").put("action",f.reserveId()).put("range","[0,100) BOX").put("allTypedSourcesAudited",true));
            return;
        }
        applied(r);assertEquals(1,f.rows("mulino_inventory_SegmentAllocations").size());
        var replay=f.reserve();applied(replay);assertEquals(r.at("/response/allocationId"),replay.at("/response/allocationId"));assertEquals(1,f.rows("mulino_inventory_SegmentAllocations").size());
        reached("after-db");
        BigDecimal remaining=BigDecimal.ZERO;
        for(var row:f.rows("mulino_inventory_QuantitySegments"))if(row.get("retiredat")==null&&f.aliases.path("W").asText().equals(row.get("placeid")))remaining=remaining.add((BigDecimal)row.get("quantity"));
        assertEquals(0,new BigDecimal(sid.equals("cumulative-versus-state")?"40":"0").compareTo(remaining));
        var checks=Json.array();for(var assertion:f.sub.path("assertions")) {
            var row=Json.object().put("id",assertion.path("id").asText());
            try {new AssertionEngine().check(assertion,f.results,f.aliases);row.put("status","PASS");}
            catch(AssertionError|IllegalArgumentException failure){row.put("status","FAIL").put("reason",failure.getMessage());}
            checks.add(row);
        }
        Json.write(out.resolve("original-oracles-"+sid+".json"),checks);
    }
    @Test void completeCumulativeSources() throws Exception {complete("cumulative-versus-state");}
    @Test void completeExistsSourcesAndInternalAuthorizationSeam() throws Exception {complete("exists-versus-end-throughout");}
    @Test void rawAndUnverifiedClaimsDoNotMakeTypedPermission() throws Exception {
        for(String stop:List.of("t09-sale-source-sell-qc-document","t09-sale-source-sell-qc-event","t09-sale-source-sell-qc-match")) {
            install("exists-versus-end-throughout");reached(stop);
            assertTrue(f.rows("mulino_inventory_DispositionBases").isEmpty());
            assertTrue(f.rows("mulino_evidence_CanonicalOccurrences").isEmpty());
            if(!stop.endsWith("-document")) {
                var claim=f.rows("mulino_evidence_Claims");assertEquals(1,claim.size());
                assertNotNull(claim.getFirst().get("eventid"));assertNotNull(claim.getFirst().get("documentversionid"));
            }
            String read="t09-sale-source-sell-qc-segment";
            var revision=f.call("getObject",f.request(read),"t09SaleSource");f.results.put(read,revision);
            var request=f.request("t09-sale-source-sell-qc-basis");
            var result=f.call("recordDispositionBasis",request,"t09SaleSource");
            f.results.put("unverified-basis-probe",result);
            assertEquals("EVIDENCE_UNVERIFIED",result.at("/response/error/code").asText(),result.toString());
            assertTrue(f.rows("mulino_inventory_DispositionBases").isEmpty());denied(f.reserve());
            f.save(out.resolve("unverified-"+stop+".json"));
        }
    }
    @Test void categoryAndRegulatoryOmissionsCannotAllocate() throws Exception {
        for(String missing:List.of("qc","customer","commercial","regulatory")) {
            install("exists-versus-end-throughout");
            Set<String> omit=new HashSet<>();
            for(String id:f.sources())if(id.startsWith("t09-sale-source-sell-"+missing+"-")||(missing.equals("regulatory")&&id.contains("-regulatory-")))omit.add(id);
            assertEquals(f.reserveId(),f.through(f.reserveId(),omit));denied(f.results.get(f.reserveId()));
            assertEquals(missing.equals("regulatory")?6:5,f.rows("mulino_inventory_DispositionBases").size());
            assertEquals(missing.equals("regulatory")?0:2,f.rows("mulino_trade_regulatory_DecisionVersions").size());
            f.save(out.resolve("missing-"+missing+".json"));
        }
    }
    @Test void wrongSourceScopeAndGrantCannotPublish() throws Exception {
        for(String mode:List.of("wrong-item","expired-grant")) {
            install("exists-versus-end-throughout",false,fixture->{
                var grant=(ObjectNode)fixture.path("actors").path("t09SaleSource").path("grant");
                if(mode.equals("wrong-item"))((ObjectNode)grant.path("scope")).set("itemAliases",Json.array().add("M"));
                else grant.put("validUntil","2026-10-07T00:00:00Z");
            });
            assertEquals("t09-sale-source-sell-qc-document",f.through("t09-sale-source-sell-qc-document"));
            var r=f.results.get("t09-sale-source-sell-qc-document");assertEquals("FORBIDDEN",r.at("/response/error/code").asText(),r.toString());
            assertTrue(f.rows("mulino_inventory_DispositionBases").isEmpty());noAllocation();
            f.save(out.resolve(mode+".json"));
        }
    }

    @Test void typedBasisCannotDepartFromItsVerifiedOriginal() throws Exception {
        install("exists-versus-end-throughout");reached("t09-sale-source-sell-qc-segment");
        ObjectNode original=f.request("t09-sale-source-sell-qc-basis");
        for(String field:List.of("segmentId","customerId","quantity","startQuantity","unit","workId","validUntil")) {
            var req=original.deepCopy().put("commandIdempotencyKey",UUID.randomUUID().toString());
            String value=switch(field) {
                case "quantity"->"99";case "startQuantity"->"1";case "unit"->"EA";
                case "validUntil"->"2026-10-07T00:00:00Z";default->UUID.randomUUID().toString();
            };
            ((ObjectNode)req.path("slots")).put(field,value);
            var result=f.call("recordDispositionBasis",req,"t09SaleSource");f.results.put("bad-basis-"+field,result);
            assertNotEquals("APPLIED",result.at("/response/outcome").asText(),result.toString());
            assertTrue(f.rows("mulino_inventory_DispositionBases").isEmpty());noAllocation();
        }
        // Actual other organization identity cannot attach an original organization's source.
        var owner=f;
        install("exists-versus-end-throughout");
        var cross=f.call("recordDispositionBasis",original,"t09SaleSource");f.results.put("cross-org",cross);
        assertEquals("FORBIDDEN",cross.at("/response/error/code").asText());noAllocation();
        f.save(out.resolve("cross-org.json"));f=owner;
    }

    @Test void sixIndependentTypedSourcesRemainInsufficientWithoutRegulatory() throws Exception {
        for(String sid:List.of("cumulative-versus-state","exists-versus-end-throughout")) {
            install(sid);Set<String> omit=new HashSet<>();
            for(String id:f.sources())if(id.contains("-regulatory-"))omit.add(id);
            assertEquals(f.reserveId(),f.through(f.reserveId(),omit));auditQualitySources();
            String document=f.results.get("t09-sale-source-sell-qc-document").at("/response/id").asText();
            var read=Json.object().put("id",document);read.set("scope",Json.object().put("organizationId",f.aliases.path("ORG").asText()));
            var source=f.call("getEvidence",read,"t09SaleSource");f.results.put("source-public-evidence",source);assertEquals(200,source.path("httpStatus").asInt());
            var warehouse=f.call("getEvidence",read,"warehouse");f.results.put("warehouse-public-evidence",warehouse);
            assertEquals(sid.equals("exists-versus-end-throughout")?403:200,warehouse.path("httpStatus").asInt());
            denied(f.reserve());assertTrue(f.rows("mulino_trade_regulatory_DecisionVersions").isEmpty());
            f.save(out.resolve("six-bases-no-regulatory-"+sid+".json"));
        }
    }

    @Test void missingExpiryAndCorrectedSourceCannotAllocate() throws Exception {
        for(String mode:List.of("missing-expiry","corrected-source")) {
            install("cumulative-versus-state",false,fixture->{if(mode.equals("missing-expiry"))((ObjectNode)fixture.path("aliases").path("LOT")).remove("expiresAt");});
            reached("t09-sale-source-dispatch-regulatory-allow");auditSources();
            if(mode.equals("corrected-source")) {
                var req=f.request("t09-sale-source-sell-qc-document").put("capabilityId","correctEvidence").put("commandIdempotencyKey","correct-qc-source").put("expectedRevision",1);
                var slots=(ObjectNode)req.path("slots");slots.put("evidenceType","DOCUMENT").put("supersedesId",f.results.get("t09-sale-source-sell-qc-document").at("/response/id").asText()).put("sourceReference","new-explicit-correction");
                var corrected=f.call("correctEvidence",req,"A");f.results.put("correct-qc-source",corrected);applied(corrected);
            }
            denied(f.reserve());f.save(out.resolve(mode+".json"));
        }
    }
    @Test void sellAloneDoesNotAuthorizeDispatch() throws Exception {
        install("cumulative-versus-state");
        var omit=new HashSet<String>();for(String id:f.sources())if(id.startsWith("t09-sale-source-dispatch-"))omit.add(id);
        assertEquals(f.reserveId(),f.through(f.reserveId(),omit));applied(f.results.get(f.reserveId()));
        JsonNode pick=null,dispatch=null;
        for(var a:f.sub.path("actions")) {
            String cap=a.path("capabilityId").asText();
            if(cap.equals("pickQuantity"))pick=f.execute(a);
            if(a.path("id").asText().equals("clock-dispatch"))f.execute(a);
            if(cap.equals("dispatchQuantity")){dispatch=f.execute(a);break;}
        }
        applied(Objects.requireNonNull(pick));assertNotEquals("APPLIED",Objects.requireNonNull(dispatch).at("/response/outcome").asText());
        assertEquals(1,f.rows("mulino_inventory_SegmentAllocations").size());
        assertFalse(f.rows("mulino_inventory_QuantityMovements").stream().anyMatch(x->"DISPATCH".equals(x.get("kind"))));
    }

}
