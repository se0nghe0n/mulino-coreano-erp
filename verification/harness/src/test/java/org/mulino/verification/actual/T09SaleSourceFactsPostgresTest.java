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
    JsonNode sourceEligibility(String action) throws Exception {
        var query=Json.object().put("id",f.segment());query.set("scope",Json.object().put("organizationId",f.aliases.path("ORG").asText()));
        query.set("filters",Json.object().put("action",action).put("customerId",f.aliases.path("CUSTOMER").asText()));
        var result=f.call("evaluateEligibility",query,"t09SaleSource");
        f.results.put("source-eligibility-"+action+"-"+f.results.size(),result);
        assertEquals(200,result.path("httpStatus").asInt(),result.toString());return result;
    }
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
        assertEquals(sid.equals("cumulative-versus-state")?"60":"100",sourceEligibility("SELL").at("/response/data/eligibleQuantity").asText());
        String doc=f.results.get("t09-sale-source-sell-qc-document").at("/response/id").asText();
        var read=Json.object().put("id",doc);read.set("scope",Json.object().put("organizationId",f.aliases.path("ORG").asText()));
        var sourceRead=f.call("getEvidence",read,"t09SaleSource");assertEquals(200,sourceRead.path("httpStatus").asInt(),sourceRead.toString());
        var warehouseRead=f.call("getEvidence",read,"warehouse");f.results.put("warehouse-public-evidence",warehouseRead);
        assertEquals(sid.equals("exists-versus-end-throughout")?403:200,warehouseRead.path("httpStatus").asInt(),warehouseRead.toString());
        JsonNode r=f.reserve();
        applied(r);assertEquals(1,f.rows("mulino_inventory_SegmentAllocations").size());
        var replay=f.reserve();applied(replay);assertFalse(r.at("/response/effects/allocationId").asText().isEmpty());assertEquals(r.at("/response/effects/allocationId"),replay.at("/response/effects/allocationId"));assertEquals(1,f.rows("mulino_inventory_SegmentAllocations").size());
        reached(sid.equals("cumulative-versus-state")?"dispatch60":"dispatch");
        auditPhysicalDispatch(sid);
        f.save(out.resolve("physical-dispatch-"+sid+".json"));
        reached("after-db");
        auditInventoryQueries(sid);
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
        assertTrue(java.util.stream.StreamSupport.stream(checks.spliterator(),false).allMatch(x->"PASS".equals(x.path("status").asText())),checks.toPrettyString());
    }
    void auditInventoryQueries(String sid) {
        var ids=sid.equals("cumulative-versus-state")?List.of("inventory"):List.of("initial","final");
        assertEquals(new HashSet<>(ids),f.inventoryScopeAudits.keySet());
        for(String id:ids) {
            var audit=f.inventoryScopeAudits.get(id);
            assertEquals(audit.path("businessDigestBefore"),audit.path("businessDigestAfter"),audit.toPrettyString());
            BigDecimal warehouse=BigDecimal.ZERO,transit=BigDecimal.ZERO,total=BigDecimal.ZERO;
            for(var row:audit.path("sqlRows"))if(row.path("retiredat").isNull()) {
                var quantity=new BigDecimal(row.path("quantity").asText());total=total.add(quantity);
                if(row.path("placeid").asText().equals(f.aliases.path("W").asText()))warehouse=warehouse.add(quantity);
                if(row.path("placeid").asText().equals(f.aliases.path("TRANSIT").asText()))transit=transit.add(quantity);
            }
            assertEquals(0,new BigDecimal("100").compareTo(total));
            assertEquals(0,total.compareTo(warehouse.add(transit)));
            BigDecimal expectedWarehouse=new BigDecimal(id.equals("initial")?"100":sid.equals("cumulative-versus-state")?"40":"0");
            assertEquals(0,expectedWarehouse.compareTo(warehouse));
            for(String key:List.of("authoredResult","itemOnly","transit"))
                assertEquals(200,audit.path(key).path("httpStatus").asInt(),audit.toPrettyString());
            assertEquals(f.aliases.path("W"),audit.at("/authoredRequest/scope/placeId"));
            assertFalse(audit.at("/itemOnly/request/scope").has("placeId"));
            assertEquals(0,total.compareTo(new BigDecimal(audit.at("/itemOnly/response/data/heldQuantity").asText())));
            assertEquals(0,warehouse.compareTo(new BigDecimal(audit.at("/authoredResult/response/data/heldQuantity").asText())));
            assertEquals(0,transit.compareTo(new BigDecimal(audit.at("/transit/response/data/heldQuantity").asText())));
        }
    }
    void auditPhysicalDispatch(String sid) throws Exception {
        String suffix=sid.equals("cumulative-versus-state")?"60":"";
        BigDecimal quantity=new BigDecimal(suffix.isEmpty()?"100":"60");
        applied(f.results.get("pick"+suffix));applied(f.results.get("dispatch"+suffix));
        var allocations=f.rows("mulino_inventory_SegmentAllocations");assertEquals(1,allocations.size());
        var a=allocations.getFirst();assertEquals("CONSUMED",a.get("state"));assertNotNull(a.get("pickedat"));
        var dispatches=f.rows("mulino_inventory_Dispatches");assertEquals(1,dispatches.size());var d=dispatches.getFirst();
        var cargo=f.rows("mulino_inventory_CargoScopes");assertEquals(1,cargo.size());var c=cargo.getFirst();
        assertEquals(a.get("id"),d.get("allocationid"));assertEquals(d.get("id"),c.get("dispatchid"));assertEquals(d.get("cargoscopeid"),c.get("id"));
        assertEquals(f.results.get("dispatch"+suffix).at("/response/effects/dispatchId").asText(),d.get("id"));
        for(var row:List.of(a,d,c)) {
            assertEquals(0,quantity.compareTo((BigDecimal)row.get("quantity")));
            assertEquals(0,BigDecimal.ZERO.compareTo((BigDecimal)row.get("startquantity")));assertEquals("BOX",row.get("unit"));
            assertEquals(f.work(),row.get("workid"));assertEquals(f.aliases.path("CUSTOMER").asText(),row.get("customerid"));
        }
        BigDecimal warehouse=BigDecimal.ZERO,transit=BigDecimal.ZERO;
        for(var row:f.rows("mulino_inventory_QuantitySegments"))if(row.get("retiredat")==null) {
            if(f.aliases.path("W").asText().equals(row.get("placeid")))warehouse=warehouse.add((BigDecimal)row.get("quantity"));
            if(f.aliases.path("TRANSIT").asText().equals(row.get("placeid")))transit=transit.add((BigDecimal)row.get("quantity"));
        }
        assertEquals(0,warehouse.compareTo(BigDecimal.ZERO));BigDecimal originalTotal=BigDecimal.ZERO;
        for(var input:f.fixture.path("baseline").path("segments"))originalTotal=originalTotal.add(new BigDecimal(input.path("quantity").asText()));
        assertEquals(0,transit.compareTo(originalTotal));
        var leaf=f.rows("mulino_inventory_QuantitySegments").stream().filter(x->d.get("transitsegmentid").equals(x.get("id"))).findFirst().orElseThrow();
        assertEquals(0,quantity.compareTo((BigDecimal)leaf.get("quantity")));assertEquals(f.aliases.path("TRANSIT").asText(),leaf.get("placeid"));
        assertEquals(1,f.rows("mulino_inventory_QuantityMovements").stream().filter(x->"DISPATCH".equals(x.get("kind"))).count());
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
        for(var entry:Map.of("qc","decideQuality","customer","confirmCustomerConditions","commercial","confirmCommercialDisposition").entrySet()) {
            install("exists-versus-end-throughout",false,fixture->{
                var actor=fixture.path("actors").path("t09SaleSource");
                for(var list:List.of(actor.path("roleCapabilities"),actor.path("grant").path("actions"))) {
                    var array=(com.fasterxml.jackson.databind.node.ArrayNode)list;
                    for(int i=array.size()-1;i>=0;i--)if(entry.getValue().equals(array.get(i).asText()))array.remove(i);
                }
            });
            String prefix="t09-sale-source-sell-"+entry.getKey();reached(prefix+"-work");
            var result=f.execute(java.util.stream.StreamSupport.stream(f.sub.path("actions").spliterator(),false).filter(x->(prefix+"-basis").equals(x.path("id").asText())).findFirst().orElseThrow());
            assertEquals("FORBIDDEN",result.at("/response/error/code").asText(),result.toString());noAllocation();
            f.save(out.resolve("missing-category-capability-"+entry.getKey()+".json"));
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

    void activateCorrectionWork() throws Exception {
        // Complete only this independent negative-test instance, from actual stored
        // goal/definition plus the fixture's explicit timezone and observation window.
        var work=f.rows("mulino_work_read_Works").stream().filter(x->f.work().equals(x.get("id"))).findFirst().orElseThrow();
        var goalRow=f.rows("mulino_work_read_GoalReferences").stream().filter(x->work.get("currentgoalversionid").equals(x.get("id"))).findFirst().orElseThrow();
        var definition=f.rows("mulino_definitions_DefinitionVersions").stream().filter(x->work.get("definitionversionid").equals(x.get("id"))).findFirst().orElseThrow();
        assertEquals("PUBLISHED",definition.get("state"));
        var published=Json.parse(definition.get("content").toString());
        ObjectNode original=(ObjectNode)Json.parse(goalRow.get("slotsjson").toString());
        JsonNode template=null;
        for(var candidate:published.path("goals"))if(candidate.path("endpoint").equals(original.path("endpoint"))&&candidate.path("quantityMode").equals(original.path("quantityMode"))) {
            assertNull(template,"Exactly one published goal must match the original target");template=candidate;
        }
        assertNotNull(template,"Published delivery goal required");
        ObjectNode completed=original.deepCopy();
        completed.put("timezone",f.fixture.path("clock").path("timezone").asText());
        completed.put("evidencePolicyVersion",f.fixture.path("versions").path("policy").asText());
        completed.put("evaluatorVersion",template.path("evaluatorVersion").asText());
        completed.set("conditions",Json.array().add(Json.object().put("id",template.path("name").asText())));
        completed.put("periodStart",f.fixture.path("clock").path("asOf").asText());
        completed.set("periodEnd",original.path("dueAt"));
        completed.set("periodStartInclusive",f.results.get("cumulative").at("/request/slots/period/startInclusive"));
        assertTrue(completed.path("periodStartInclusive").isBoolean(),"Original cumulative interval must declare start inclusion");
        assertTrue(f.fixture.path("clock").has("deadlineInclusive"));
        completed.set("periodEndInclusive",f.fixture.path("clock").path("deadlineInclusive"));
        for(var fields=original.fields();fields.hasNext();) {
            var field=fields.next();assertEquals(field.getValue(),completed.path(field.getKey()),"Original target field changed: "+field.getKey());
        }
        var evidence=Json.object().put("driverStatus","EXECUTED");
        evidence.set("provenance",Json.object().put("source","POSTGRESQL_READ_ONLY").put("scopeComplete",true));
        evidence.set("response",Json.object().put("definitionId",definition.get("id").toString()).put("definitionHash",definition.get("contenthash").toString()).set("completedGoal",completed));
        f.results.put("correction-explicit-goal-inputs",evidence);
        var read=Json.object().put("workId",f.work());read.set("scope",Json.object().put("organizationId",f.aliases.path("ORG").asText()));
        var current=f.call("getWork",read,"A");f.results.put("correction-current-sale-work",current);
        var revise=Json.object().put("intentKind","COMMAND").put("definitionVersion","definition-v1").put("capabilityId","reviseGoal").put("commandIdempotencyKey","correction-complete-test-goal");
        revise.set("subjectRefs",Json.array());revise.set("slots",Json.object().put("workId",f.work()).put("reason","새 correction 반례의 목표 입력을 발행 정의와 fixture 시점으로 명시한다").set("goal",completed));
        revise.set("provenance",Json.object().put("workId","CONTEXT").put("reason","USER").put("goal","USER"));revise.set("expectedRevision",current.at("/response/data/revision"));
        var revised=f.call("reviseGoal",revise,"A");f.results.put("correction-complete-test-goal",revised);applied(revised);
        current=f.call("getWork",read,"A");f.results.put("correction-current-completed-work",current);
        var activate=Json.object().put("intentKind","COMMAND").put("definitionVersion","definition-v1").put("capabilityId","activateWork").put("commandIdempotencyKey","correction-activate-sale-work");
        activate.set("subjectRefs",Json.array());activate.set("slots",Json.object().put("workId",f.work()));activate.set("provenance",Json.object().put("workId","CONTEXT"));activate.set("expectedRevision",current.at("/response/data/revision"));
        var activated=f.call("activateWork",activate,"A");f.results.put("correction-activate-sale-work",activated);applied(activated);
    }

    @Test void missingExpiryAndCorrectedSourceCannotAllocate() throws Exception {
        for(String mode:List.of("missing-expiry","corrected-source")) {
            install("cumulative-versus-state",false,fixture->{if(mode.equals("missing-expiry"))((ObjectNode)fixture.path("aliases").path("LOT")).remove("expiresAt");});
            reached("t09-sale-source-dispatch-regulatory-allow");auditSources();
            if(mode.equals("corrected-source")) {
                assertEquals("60",sourceEligibility("SELL").at("/response/data/eligibleQuantity").asText());
                activateCorrectionWork();
                f.post("/__verification/runtime/clock",Json.object().put("instant",java.time.Instant.parse(f.fixture.path("clock").path("asOf").asText()).plusSeconds(1).toString()),"A");
                var req=f.request("t09-sale-source-sell-qc-document").put("capabilityId","correctEvidence").put("commandIdempotencyKey","correct-qc-source").put("expectedRevision",1);
                var slots=(ObjectNode)req.path("slots");slots.put("evidenceType","DOCUMENT").put("supersedesId",f.results.get("t09-sale-source-sell-qc-document").at("/response/id").asText());
                var corrected=f.call("correctEvidence",req,"A");f.results.put("correct-qc-source",corrected);applied(corrected);
            }
            assertEquals("0",sourceEligibility("SELL").at("/response/data/eligibleQuantity").asText());
            denied(f.reserve());f.save(out.resolve(mode+".json"));
        }
    }
    @Test void sellAloneDoesNotAuthorizeDispatch() throws Exception {
        install("cumulative-versus-state");
        var omit=new HashSet<String>();for(String id:f.sources())if(id.startsWith("t09-sale-source-dispatch-"))omit.add(id);
        assertEquals(f.reserveId(),f.through(f.reserveId(),omit));
        assertEquals("60",sourceEligibility("SELL").at("/response/data/eligibleQuantity").asText());
        assertEquals("0",sourceEligibility("DISPATCH").at("/response/data/eligibleQuantity").asText());
        applied(f.results.get(f.reserveId()));
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
