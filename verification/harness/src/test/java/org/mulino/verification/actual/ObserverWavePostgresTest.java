package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mulino.verification.Json;
import org.mulino.verification.AssertionEngine;
import static org.junit.jupiter.api.Assertions.*;

/** Actual PostgreSQL read/identity/fence tests; no scenario runner or product evaluator simulation. */
@EnabledIfEnvironmentVariable(named="OBSERVER_PG_TEST",matches="true")
class ObserverWavePostgresTest {
    final ScenarioObservationPostgresTest f=new ScenarioObservationPostgresTest();
    @BeforeEach void setup(TestInfo info)throws Exception {f.setup(info);f.base();}
    @AfterEach void cleanup()throws Exception {f.cleanup();}
    String id(){return ScenarioObservationPostgresTest.id();}
    JsonNode capture(String...sources)throws Exception {return f.capture(sources);}
    ObjectNode contribution(String occurrence,String start,String quantity) {
        return Json.object().put("ID",id()).put("organizationId",f.org).put("recordedAt",f.now)
            .put("linkId",id()).put("occurrenceId",occurrence).put("targetWorkId",f.work).put("goalId",f.goal)
            .put("startQuantity",start).put("quantity",quantity).put("unit","BOX");
    }
    @Test void contributionTargetScopeAndPhysicalCoordinatesRetainCanonicalIdentitiesWithoutSerialLabels()throws Exception {
        String occurrence=id(),physical=id();
        f.insert("mulino_evidence_CanonicalOccurrences",f.row(occurrence).put("physicalScopeId",physical).put("quantity","60").put("unit","BOX"));
        var first=contribution(occurrence,"0","30");var second=contribution(occurrence,"30","30");
        f.insert("mulino_work_WorkContributions",first);f.insert("mulino_work_WorkContributions",second);
        f.insert("mulino_work_WorkContributions",contribution(occurrence,"0","1").put("targetWorkId",id()));
        var rows=capture("contributions").at("/rawRows/contributions");assertEquals(2,rows.size());
        var starts=new HashSet<String>();for(var row:rows){
            assertEquals(f.work,row.path("workId").asText());assertEquals(row.path("targetWorkId"),row.path("workId"));
            assertEquals(physical,row.at("/physicalRange/physicalScopeId").asText());assertEquals(occurrence,row.at("/physicalRange/occurrenceId").asText());
            starts.add(row.at("/physicalRange/startQuantity").asText());assertEquals("BOX",row.at("/physicalRange/unit").asText());
            assertFalse(row.has("physicalScope"));assertFalse(row.toString().contains("serial-boxes"));
        }
        assertEquals(Set.of("0","30"),starts);
        var all=ScenarioJdbcObservation.capture(f.config,f.root,f.request(Json.object().put("organizationId",f.org),"contributions"));
        assertEquals(3,all.at("/rawRows/contributions").size());
    }
    @ParameterizedTest @ValueSource(strings={"foreign","future","overflow","unit","negative"})
    void unrelatedOrInvalidCanonicalRangeNeverCreatesPhysicalCoordinates(String variant)throws Exception {
        String occurrence=id();var source=f.row(occurrence).put("physicalScopeId",id()).put("quantity","60").put("unit","BOX");
        if(variant.equals("foreign"))source.put("organizationId",id());if(variant.equals("future"))source.put("recordedAt",f.later);
        f.insert("mulino_evidence_CanonicalOccurrences",source);
        var row=contribution(occurrence,variant.equals("negative")?"-1":"0",variant.equals("overflow")?"61":"30");
        if(variant.equals("unit"))row.put("unit","KG");f.insert("mulino_work_WorkContributions",row);
        var observed=capture("contributions").at("/rawRows/contributions/0");
        assertEquals(f.work,observed.path("workId").asText());assertFalse(observed.has("physicalRange"));assertFalse(observed.has("physicalScope"));
    }
    @Test void directReceiptsPreserveDistinctActualOccurrencesAndEveryScopeAndKnownAtFence()throws Exception {
        var occurrenceIds=new HashSet<String>();
        for(String quantity:List.of("60","40")){
            String occurrence=id();occurrenceIds.add(occurrence);
            f.insert("mulino_trade_receipt_Receipts",f.row(id()).put("itemId",f.item).put("workId",f.work).put("lotId",f.lot)
                .put("quantity",quantity).put("unit","BOX").put("canonicalOccurrenceId",occurrence));
        }
        for(String mismatch:List.of("organizationId","itemId","workId","lotId","recordedAt")){
            var row=f.row(id()).put("itemId",f.item).put("workId",f.work).put("lotId",f.lot).put("quantity","999").put("unit","BOX");
            row.put(mismatch,mismatch.equals("recordedAt")?f.later:id());f.insert("mulino_trade_receipt_Receipts",row);
        }
        var raw=capture("receipts","contributions").path("rawRows");assertTrue(raw.path("contributions").isEmpty());
        var rows=raw.path("receipts");assertEquals(2,rows.size());var actual=new HashSet<String>();var sum=java.math.BigDecimal.ZERO;
        for(var row:rows){actual.add(row.path("canonicalOccurrenceId").asText());sum=sum.add(new java.math.BigDecimal(row.path("quantity").asText()));}
        assertEquals(occurrenceIds,actual);assertEquals(0,new java.math.BigDecimal("100").compareTo(sum));
    }
    @Test void movementBaselineKeepsInitialBalanceAndDetectsSameInstantNewEffects()throws Exception {
        String segment=id();f.insert("mulino_inventory_QuantitySegments",f.row(segment).put("itemId",f.item).put("lotId",f.lot));
        f.insert("mulino_inventory_QuantityMovements",f.row(id()).put("targetId",segment).put("quantity","100").put("unit","BOX").put("kind","INITIAL_BALANCE"));
        var before=capture("movements");var after=capture("movements");assertEquals(1,before.at("/rawRows/movements").size());
        var assertion=Json.object().put("op","countAdded").put("expected",0);
        assertion.set("source",Json.object().put("actionId","after").put("pointer","/data/rawRows/movements"));
        assertion.set("baseline",Json.object().put("actionId","before").put("pointer","/data/rawRows/movements"));
        var results=new HashMap<String,JsonNode>();results.put("before",f.step(before).toJson());results.put("after",f.step(after).toJson());
        var engine=new AssertionEngine();assertDoesNotThrow(()->engine.check(assertion,results));
        f.insert("mulino_inventory_QuantityMovements",f.row(id()).put("sourceId",segment).put("targetId",segment).put("quantity","1").put("unit","BOX").put("kind","DISPATCH"));
        results.put("after",f.step(capture("movements")).toJson());assertThrows(AssertionError.class,()->engine.check(assertion,results));
    }
    @Test void deliveryObservationEntityDoesNotHideGeneralEvidenceClaims()throws Exception {
        String segment=id();f.insert("mulino_inventory_QuantitySegments",f.row(segment).put("itemId",f.item).put("lotId",f.lot));
        for(int n=0;n<3;n++)f.insert("mulino_evidence_Claims",f.row(id()).put("itemId",f.item).put("workId",f.work).put("subjectId",segment));
        var report=f.row(id()).put("itemId",f.item).put("workId",f.work).put("lotId",f.lot).put("state","PROVISIONAL");
        f.insert("mulino_trade_sales_Observations",report);
        var raw=capture("claims","deliveryObservations").path("rawRows");assertEquals(3,raw.path("claims").size());assertEquals(1,raw.path("deliveryObservations").size());
        var observed=raw.path("deliveryObservations").get(0);assertEquals(report.path("ID"),observed.path("id"));
        assertFalse(observed.path("canonical").asBoolean(true));assertEquals("PENDING_RECONCILIATION",observed.path("status").asText());
        assertEquals("PROVISIONAL",observed.path("state").asText());assertFalse(observed.has("canonicalOccurrenceId"));
    }
    @Test void unassessedMarkerHasAnExplicitNullFilterValueWhileMissingColumnsStillFail()throws Exception {
        var snapshot=Json.object().put("ID",f.work).put("organizationId",f.org).put("revision",2).put("effectiveAt",f.now)
            .put("currentGoalVersionId",f.goal).put("pendingInvalidation",true);
        var transition=f.row(id()).put("workId",f.work).put("revision",2).put("state","ACTIVE").put("snapshotJson",snapshot.toString());transition.remove("createdAt");
        f.insert("mulino_work_WorkTransitions",transition);
        var observed=capture("assessmentInputs");var marker=observed.at("/rawRows/assessmentInputs/0");
        assertTrue(marker.path("assessmentId").isNull());assertFalse(marker.hasNonNull("snapshotId"));
        var assertion=Json.object().put("op","count").put("expected",0);
        var source=Json.object().put("actionId","observe").put("pointer","/data/rawRows/assessmentInputs");
        source.set("where",Json.object().put("assessmentId",id()));assertion.set("source",source);
        var engine=new AssertionEngine();assertDoesNotThrow(()->engine.check(assertion,Map.of("observe",f.step(observed).toJson())));
        var missing=observed.deepCopy();((ObjectNode)missing.at("/rawRows/assessmentInputs/0")).remove("assessmentId");
        assertThrows(AssertionError.class,()->engine.check(assertion,Map.of("observe",f.step(missing).toJson())));
    }
    @Test void deliveryAssessmentKindAndCurrentRevisionFollowActualGoalAndSuccessorLinks()throws Exception {
        f.changeSupplier("mulino_work_read_GoalReferences",f.row(f.goal),"endpoint","DELIVERED");
        f.changeSupplier("mulino_work_read_Works",f.row(f.work),"currentGoalVersionId",f.goal);
        String first=id(),second=id();
        f.insert("mulino_work_read_AssessmentReferences",f.assessment(first,f.work,f.now,"[]").put("asOf",f.now).put("knownAt",f.now));
        f.insert("mulino_work_read_AssessmentReferences",f.assessment(second,f.work,f.now,"[]").put("asOf",f.now).put("knownAt",f.now).put("previousAssessmentId",first));
        f.insert("mulino_work_read_AssessmentReferences",f.assessment(id(),f.work,f.later,"[]").put("asOf",f.later).put("knownAt",f.later).put("previousAssessmentId",second));
        var rows=capture("assessments").at("/rawRows/assessments");assertEquals(2,rows.size());
        for(var row:rows){assertEquals("DELIVERY",row.path("goalKind").asText());assertEquals(second.equals(row.path("id").asText()),row.path("current").asBoolean());}
    }

}
