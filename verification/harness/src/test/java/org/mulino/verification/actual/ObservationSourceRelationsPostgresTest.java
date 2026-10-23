package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mulino.verification.Json;

/** Real JDBC observer proof; composition reuses setup without rerunning the base class's inherited tests. */
@EnabledIfEnvironmentVariable(named="OBSERVER_PG_TEST",matches="true")
class ObservationSourceRelationsPostgresTest {
    final ScenarioObservationPostgresTest f=new ScenarioObservationPostgresTest();
    String segment,command;
    @BeforeEach void setup(TestInfo info)throws Exception {
        f.setup(info);f.base();segment=ScenarioObservationPostgresTest.id();command=ScenarioObservationPostgresTest.id();
        f.insert("mulino_inventory_QuantitySegments",f.row(segment).put("itemId",f.item).put("lotId",f.lot).put("quantity","100").put("unit","KG"));
    }
    @AfterEach void cleanup()throws Exception {f.cleanup();}
    JsonNode capture(String...sources)throws Exception {return f.capture(sources);}
    void change(String table,String id,String field,Object value)throws Exception {
        try(var s=f.writer.prepareStatement("UPDATE "+table+" SET "+field+"=? WHERE ID=?")) {
            if(field.endsWith("At"))s.setObject(1,java.time.OffsetDateTime.parse(value.toString()));else s.setObject(1,value);
            s.setString(2,id);assertEquals(1,s.executeUpdate());
        }
    }
    @Test void emptyQuantityRowsGetOnlyTheActualItemUnitFromTheSameSnapshot()throws Exception {
        f.insert("mulino_inventory_TradeItems",f.row(f.item).put("baseUnit","KG"));
        var r=capture("deliveries");assertTrue(r.at("/rawRows/deliveries").isEmpty());assertEquals("KG",r.at("/data/unit").asText());
        assertEquals(f.item,r.at("/data/itemId").asText());assertEquals("REPEATABLE_READ",r.at("/snapshot/isolation").asText());
        assertTrue(r.at("/snapshot/measurementMetadataQueries").toString().contains("r.ID=?"));assertTrue(r.at("/snapshot/measurementMetadataQueries").toString().contains(f.item));
    }
    @ParameterizedTest @ValueSource(strings={"foreign","futureRecorded","futureCreated","missing","ambiguous"})
    void missingOrForeignFutureAndAmbiguousItemMetadataNeverDefaultsToFixtureUnit(String variant)throws Exception {
        if(!variant.equals("missing"))f.insert("mulino_inventory_TradeItems",f.row(f.item).put("baseUnit","KG").put("organizationId",variant.equals("foreign")?ScenarioObservationPostgresTest.id():f.org).put("recordedAt",variant.equals("futureRecorded")?f.later:f.now).put("createdAt",variant.equals("futureCreated")?f.later:f.before));
        if(variant.equals("ambiguous")){f.insert("mulino_inventory_TradeItems",f.row(f.item).put("baseUnit","BOX"));assertThrows(UnsupportedOperationException.class,()->capture("deliveries"));}
        else assertFalse(capture("deliveries").at("/data").has("unit"));
    }
    @Test void movementOriginIsItsStoredSourceIncludingExplicitNullOpeningBalance()throws Exception {
        String opening=ScenarioObservationPostgresTest.id(),dispatch=ScenarioObservationPostgresTest.id(),origin=ScenarioObservationPostgresTest.id();
        f.insert("mulino_inventory_QuantityMovements",f.row(opening).putNull("sourceId").put("targetId",segment).put("kind","INITIAL_BALANCE"));
        f.insert("mulino_inventory_QuantityMovements",f.row(dispatch).put("sourceId",origin).put("targetId",segment).put("kind","DISPATCH"));
        var rows=capture("movements").at("/rawRows/movements");assertEquals(2,rows.size());
        for(var row:rows){assertEquals(row.path("sourceId"),row.path("originId"));assertEquals(segment,row.path("segmentId").asText());}
        assertTrue(rows.findValues("originId").stream().anyMatch(JsonNode::isNull));
    }
    ObjectNode hold()throws Exception {
        String decision=ScenarioObservationPostgresTest.id();var row=f.row(ScenarioObservationPostgresTest.id()).put("segmentId",segment).put("workId",f.work).put("commandId",command).put("decisionId",decision).put("state","ACTIVE");
        f.insert("mulino_inventory_Restrictions",row);f.insert("mulino_quality_Decisions",f.row(decision).put("segmentId",segment).put("commandId",command).put("operation","placeHold").put("reason","원래 담당자의 검토 사유"));return row;
    }
    @Test void holdReasonKeepsItsOriginalDecisionAfterASeparateReleaseDecision()throws Exception {
        var hold=hold();var before=capture("restrictions").at("/rawRows/restrictions/0");assertEquals("원래 담당자의 검토 사유",before.path("reason").asText());
        String release=ScenarioObservationPostgresTest.id();f.insert("mulino_quality_Decisions",f.row(release).put("segmentId",segment).put("restrictionId",hold.path("ID").asText()).put("operation","releaseHold").put("reason","별도 해제 사유"));
        change("mulino_inventory_Restrictions",hold.path("ID").asText(),"state","RELEASED");change("mulino_inventory_Restrictions",hold.path("ID").asText(),"releaseDecisionId",release);
        var after=capture("restrictions").at("/rawRows/restrictions/0");assertEquals(before.path("reason"),after.path("reason"));assertEquals("별도 해제 사유",after.path("releaseReason").asText());
    }
    @ParameterizedTest @ValueSource(strings={"foreign","future","segment","command"})
    void anUnrelatedDecisionCannotSupplyAHoldReason(String variant)throws Exception {
        var h=hold();String id=h.path("decisionId").asText();String field=switch(variant){case "foreign"->"organizationId";case "future"->"recordedAt";case "segment"->"segmentId";default->"commandId";};
        change("mulino_quality_Decisions",id,field,variant.equals("future")?f.later:ScenarioObservationPostgresTest.id());
        var rows=capture("restrictions").at("/rawRows/restrictions");assertEquals(1,rows.size());assertFalse(rows.get(0).hasNonNull("reason"));
    }
    ObjectNode allocation(String capability)throws Exception {
        var row=f.row(ScenarioObservationPostgresTest.id()).put("segmentId",segment).put("workId",f.work).put("orderLineId",ScenarioObservationPostgresTest.id()).put("startQuantity","0").put("quantity","10").put("unit","KG").put("commandId",command).put("state","EXECUTABLE");f.insert("mulino_inventory_SegmentAllocations",row);
        var effects=row.deepCopy();effects.set("fulfillmentBasis",Json.object().put("FEFOExceptionReason","검증한 고객 포장 요구"));
        var result=Json.object().put("outcome","APPLIED");result.set("effects",effects);
        var cmd=f.row(command);cmd.remove("recordedAt");cmd.put("completedAt",f.now).put("state","COMMITTED").put("capabilityId",capability).put("resultJson",result.toString());f.insert("mulino_commands_CommandRecords",cmd);return row;
    }
    @ParameterizedTest @ValueSource(strings={"reserveQuantity","replaceAllocation"})
    void allocationReasonComesFromItsOwnCommittedCommandAndExactPhysicalRange(String capability)throws Exception {
        allocation(capability);var row=capture("allocations").at("/rawRows/allocations/0");assertEquals("검증한 고객 포장 요구",row.path("FEFOExceptionReason").asText());assertEquals(command,row.path("fulfillmentBasisCommandId").asText());
    }
    @ParameterizedTest @ValueSource(strings={"foreign","future","uncommitted","effectId","quantity","segment","wrongCapability"})
    void mismatchedAllocationProofDoesNotBecomeAnObservedFefoReason(String variant)throws Exception {
        var a=allocation("reserveQuantity");
        if(variant.equals("foreign"))change("mulino_commands_CommandRecords",command,"organizationId",ScenarioObservationPostgresTest.id());
        else if(variant.equals("future"))change("mulino_commands_CommandRecords",command,"completedAt",f.later);
        else if(variant.equals("uncommitted"))change("mulino_commands_CommandRecords",command,"state","IN_PROGRESS");
        else if(variant.equals("wrongCapability"))change("mulino_commands_CommandRecords",command,"capabilityId","releaseAllocation");
        else {
            var result=Json.object().put("outcome","APPLIED");var effects=a.deepCopy();effects.put(variant.equals("effectId")?"ID":variant.equals("segment")?"segmentId":variant,variant.equals("quantity")?"20":ScenarioObservationPostgresTest.id());effects.set("fulfillmentBasis",Json.object().put("FEFOExceptionReason","unrelated"));result.set("effects",effects);change("mulino_commands_CommandRecords",command,"resultJson",result.toString());
        }
        assertFalse(capture("allocations").at("/rawRows/allocations/0").hasNonNull("FEFOExceptionReason"));
    }
    void pendingTransition(int revision,boolean pending)throws Exception {
        var snapshot=Json.object().put("ID",f.work).put("organizationId",f.org).put("revision",revision).put("effectiveAt",f.now).put("currentGoalVersionId",f.goal).put("pendingInvalidation",pending);
        var row=f.row(ScenarioObservationPostgresTest.id()).put("workId",f.work).put("revision",revision).put("state","ACTIVE").put("snapshotJson",snapshot.toString());row.remove("createdAt");f.insert("mulino_work_WorkTransitions",row);
    }
    @Test void unassessedPendingWorkHasATransitionMarkerAndNoInventedAssessmentOrCapture()throws Exception {
        pendingTransition(2,true);var rows=capture("assessmentInputs").at("/rawRows/assessmentInputs");assertEquals(1,rows.size());var marker=rows.get(0);
        assertEquals(f.work,marker.path("workId").asText());assertEquals("PENDING",marker.path("status").asText());assertEquals("UNASSESSED",marker.path("assessmentBinding").asText());assertEquals(f.goal,marker.path("goalVersionId").asText());
        for(String key:List.of("assessmentId","snapshotId","inputEnvelopeId","conditionId"))assertFalse(marker.has(key),key);
        pendingTransition(3,false);assertTrue(capture("assessmentInputs").at("/rawRows/assessmentInputs").isEmpty());
    }
    @Test void pendingMarkerRequiresTheActualGoalAndEffectiveTime()throws Exception {
        pendingTransition(2,true);var request=f.request(f.scope(),"assessmentInputs").put("asOf",f.before);assertTrue(ScenarioJdbcObservation.capture(f.config,f.root,request).at("/rawRows/assessmentInputs").isEmpty());
        change("mulino_work_read_GoalReferences",f.goal,"workId",ScenarioObservationPostgresTest.id());assertThrows(UnsupportedOperationException.class,()->capture("assessmentInputs"));
    }

    record OrderFixture(String order,String revision,String line,String customer) {}
    OrderFixture salesOrder()throws Exception {
        String order=ScenarioObservationPostgresTest.id(),revision=ScenarioObservationPostgresTest.id(),line=ScenarioObservationPostgresTest.id(),customer=ScenarioObservationPostgresTest.id();
        f.insert("mulino_trade_sales_Orders",f.row(order).put("effectiveAt",f.before).put("workId",f.work).put("currentRevision",2));
        f.insert("mulino_trade_sales_OrderRevisions",f.row(revision).put("effectiveAt",f.before).put("orderId",order).put("workId",f.work).put("customerId",customer).put("revisionNumber",2));
        f.insert("mulino_trade_sales_OrderLines",f.row(line).put("effectiveAt",f.before).put("orderId",order).put("revisionId",revision).put("workId",f.work).put("customerId",customer).put("itemId",f.item).put("deliveryEndpoint","DELIVERED"));
        return new OrderFixture(order,revision,line,customer);
    }
    @Test void salesOrderEndpointIsItsOneActualCurrentImmutableLineAndKeepsTheHeaderGrain()throws Exception {
        var order=salesOrder();String oldRevision=ScenarioObservationPostgresTest.id();
        f.insert("mulino_trade_sales_OrderRevisions",f.row(oldRevision).put("effectiveAt",f.before).put("orderId",order.order()).put("workId",f.work).put("customerId",order.customer()).put("revisionNumber",1));
        f.insert("mulino_trade_sales_OrderLines",f.row(ScenarioObservationPostgresTest.id()).put("effectiveAt",f.before).put("orderId",order.order()).put("revisionId",oldRevision).put("workId",f.work).put("customerId",order.customer()).put("itemId",f.item).put("deliveryEndpoint","DISPATCHED"));
        var rows=capture("orders").at("/rawRows/orders");assertEquals(1,rows.size());var header=rows.get(0);
        assertEquals(order.order(),header.path("id").asText());assertEquals("DELIVERED",header.path("deliveryEndpoint").asText());assertEquals(order.revision(),header.path("currentRevisionId").asText());assertEquals(1,header.path("currentLines").size());assertEquals(order.line(),header.at("/currentLines/0/id").asText());
    }
    @ParameterizedTest @ValueSource(strings={"foreignRevision","futureRevision","futureEffectiveRevision","revisionWork","foreignLine","futureLine","futureEffectiveLine","lineWork","lineItem","lineCustomer","missingRevision"})
    void salesOrderEndpointNeverComesFromAForeignFutureMismatchedOrMissingLine(String variant)throws Exception {
        var o=salesOrder();String table=variant.contains("Line")||variant.startsWith("line")?"mulino_trade_sales_OrderLines":"mulino_trade_sales_OrderRevisions";String id=table.endsWith("OrderLines")?o.line():o.revision();
        String field=variant.equals("missingRevision")?"revisionNumber":variant.startsWith("foreign")?"organizationId":variant.startsWith("futureEffective")?"effectiveAt":variant.startsWith("future")?"recordedAt":variant.equals("lineItem")?"itemId":variant.equals("lineCustomer")?"customerId":"workId";
        change(table,id,field,variant.equals("missingRevision")?3:variant.startsWith("future")?f.later:ScenarioObservationPostgresTest.id());
        var rows=capture("orders").at("/rawRows/orders");assertEquals(1,rows.size());assertFalse(rows.get(0).hasNonNull("deliveryEndpoint"));
    }
    @Test void salesOrderMultipleCurrentLinesRemainDistinctWithoutAnArbitraryFlatEndpoint()throws Exception {
        var o=salesOrder();f.insert("mulino_trade_sales_OrderLines",f.row(ScenarioObservationPostgresTest.id()).put("effectiveAt",f.before).put("orderId",o.order()).put("revisionId",o.revision()).put("workId",f.work).put("customerId",o.customer()).put("itemId",f.item).put("deliveryEndpoint","DISPATCHED"));
        var header=capture("orders").at("/rawRows/orders/0");assertEquals(2,header.path("currentLines").size());assertEquals("MULTIPLE_LINES",header.path("orderLineBinding").asText());assertFalse(header.has("deliveryEndpoint"));
    }
}
