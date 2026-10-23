package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mulino.verification.AssertionEngine;
import org.mulino.verification.Json;

/** Actual PostgreSQL/JDBC projection proof, not product commands or scenario acceptance. */
@EnabledIfEnvironmentVariable(named="OBSERVER_PG_TEST",matches="true")
class ObligationSourceOccurrencePostgresTest {
    final ScenarioObservationPostgresTest f=new ScenarioObservationPostgresTest();
    String root,source,domain,physical,obligation;
    String hash;
    @BeforeEach void setup(TestInfo info)throws Exception {f.setup(info);f.base();}
    @AfterEach void cleanup()throws Exception {f.cleanup();}
    String id(){return ScenarioObservationPostgresTest.id();}
    void fixture(String kind)throws Exception {
        root=id();source=id();domain=id();physical=id();obligation=id();
        boolean delivery=kind.equals("DELIVERY_RESTRICTION_RESPONSE");
        boolean reconciliation=kind.equals("DELIVERY_RECONCILIATION");
        if(reconciliation)physical=domain;
        var slots=Json.object().put("workId",f.work).put("itemId",f.item).put("lotId",f.lot).put("segmentId",physical).put("observationId",id());
        var intent=Json.object().put("capabilityId",delivery||reconciliation?"recordDelivery":"openInvestigation");intent.set("slots",slots);
        hash=Json.sha256Text(sorted(intent).toString());
        var scope=Json.object().put("domainSourceId",domain).put("originalWorkId",f.work).put("physicalScopeId",physical);
        var r=f.row(root).put("sourceKind","DECISION").put("sourceId",source).put("sourceVersion",hash).put("kind",kind).put("scopeJson",scope.toString());
        f.insert("mulino_responsibility_Roots",r);
        f.insert("mulino_work_read_ObligationReferences",f.row(obligation).put("rootId",root).put("workId",f.work).put("kind",kind)
            .put("status","OPEN").put("valid",true).put("scopeJson",scope.toString()).put("ownerId",id()).put("nextAction","원래 근거를 확인한다").put("nextCheckAt",f.later));
        var d=f.row(domain).put("workId",f.work).put("itemId",f.item).put("lotId",f.lot).put(delivery||reconciliation?"rangeRootId":"segmentId",physical);
        if(delivery)d.set("observationId",slots.path("observationId"));
        f.insert(delivery?"mulino_trade_sales_Deliveries":reconciliation?"mulino_trade_sales_Observations":"mulino_trade_recall_Investigations",d);
        var result=Json.object().put("outcome",reconciliation?"ACCEPTED_PENDING_EXTERNAL":"APPLIED").put("commandId",source);
        result.putObject("effects").put(delivery?"deliveryId":reconciliation?"observationId":"investigationId",domain);
        var cmd=f.row(source);cmd.remove("recordedAt");cmd.put("state","COMMITTED").put("completedAt",f.now).put("canonicalHash",hash)
            .put("capabilityId",intent.path("capabilityId").asText()).put("canonicalIntentJson",intent.toString()).put("resultJson",result.toString());
        f.insert("mulino_commands_CommandRecords",cmd);
    }
    JsonNode sorted(JsonNode node) {
        if(!node.isObject())return node;
        var out=Json.object();var keys=new TreeSet<String>();node.fieldNames().forEachRemaining(keys::add);
        keys.forEach(key->out.set(key,sorted(node.path(key))));return out;
    }
    void change(String table,String record,String field,Object value)throws Exception {
        try(var st=f.writer.prepareStatement("UPDATE "+table+" SET "+field+"=? WHERE ID=?")) {
            st.setObject(1,field.endsWith("At")?OffsetDateTime.parse(value.toString()):value);st.setString(2,record);assertEquals(1,st.executeUpdate());
        }
    }
    JsonNode observed()throws Exception {return f.capture("obligations").at("/rawRows/obligations/0");}
    void absent(JsonNode row) {
        assertEquals(obligation,row.path("id").asText());assertEquals(root,row.path("rootId").asText());
        assertEquals("OPEN",row.path("status").asText());assertTrue(row.path("valid").asBoolean());assertTrue(row.path("current").asBoolean());
        assertFalse(row.hasNonNull("sourceOccurrenceId"));assertEquals("UNAVAILABLE",row.at("/sourceOccurrenceProof/status").asText());
        assertTrue(row.at("/sourceOccurrenceProof/gap").isTextual());
    }
    @Test void unchangedLateExpiryAndFiveClaimRequiredFieldOraclesReadActualTypedCommandSources()throws Exception {
        fixture("DELIVERY_RESTRICTION_RESPONSE");String deliverySource=source;
        fixture("RECALL_INVESTIGATION");String recallSource=source;
        fixture("DELIVERY_RECONCILIATION");String reconciliationSource=source;
        var captured=f.capture("obligations");var envelope=Json.object().put("driverStatus","EXECUTED");envelope.set("data",captured);
        envelope.putObject("provenance").put("scopeComplete",true);
        var aliases=Json.object().put("ORG",f.org).put("P",f.item);
        var engine=new AssertionEngine();var assertions=new ArrayList<JsonNode>();
        collect(Json.read(f.root.resolve("verification/cases/T17/case.json")),assertions);
        assertEquals(8,assertions.size());
        for(var assertion:assertions)engine.check(assertion,Map.of("after-delivery-db",envelope,"after-claim-db",envelope),aliases);
        for(var row:captured.at("/rawRows/obligations")) {
            String actual=row.path("sourceId").asText();assertTrue(Set.of(deliverySource,recallSource,reconciliationSource).contains(actual));
            assertEquals(actual,row.path("sourceOccurrenceId").asText());assertEquals("DECISION",row.path("sourceKind").asText());
            assertEquals(row.at("/sourceOccurrenceProof/sourceRow/canonicalHash"),row.path("sourceVersion"));assertEquals("PROVEN_TYPED_SOURCE",row.at("/sourceOccurrenceProof/status").asText());
            assertEquals("mulino_commands_CommandRecords",row.at("/sourceOccurrenceProof/sourceTable").asText());
            assertEquals(actual,row.at("/sourceOccurrenceProof/sourceRow/id").asText());
            if(row.at("/sourceOccurrenceProof/domainSourceRow").isObject())assertNotEquals(row.at("/sourceOccurrenceProof/domainSourceRow/id").asText(),actual);
        }
        String trace=captured.at("/sourceEvidence/obligations/sourceQuery/sql").asText();
        for(String expected:List.of("mulino_responsibility_Roots","mulino_commands_CommandRecords","mulino_trade_sales_Deliveries","mulino_trade_recall_Investigations","boundValues",f.org,deliverySource,recallSource))assertTrue(trace.contains(expected),expected);
        assertEquals("REPEATABLE_READ",captured.at("/snapshot/isolation").asText());
    }
    void collect(JsonNode value,List<JsonNode> out) {
        if(value.isObject()&&Set.of("late-restriction-duty-18","late-restriction-duty-25","expiry-violation-duty-18","claim-reconciliation-20").contains(value.path("id").asText()))out.add(value);
        if(value.isContainerNode())value.forEach(v->collect(v,out));
    }
    @ParameterizedTest @ValueSource(strings={"foreignRoot","missingRoot","rootCreated","rootRecorded","rootKind","rootScope","missingCommand","foreignCommand","hash","commandCreated","commandCompleted","commandPending","foreignDomain","missingDomain","domainCreated","domainRecorded","domainWork","domainPhysical","domainItem","domainLot","differentCommandEffect","differentCapability","differentResultCommand","differentIntentScope"})
    void brokenCausalLinksNeverManufactureSourceIdentityOrHideTheObligation(String variant)throws Exception {
        fixture("RECALL_INVESTIGATION");
        switch(variant) {
            case "foreignRoot" -> change("mulino_responsibility_Roots",root,"organizationId",id());
            case "missingRoot" -> change("mulino_work_read_ObligationReferences",obligation,"rootId",root=id());
            case "rootCreated","rootRecorded" -> change("mulino_responsibility_Roots",root,variant.equals("rootCreated")?"createdAt":"recordedAt",f.later);
            case "rootKind" -> change("mulino_responsibility_Roots",root,"kind","QUALITY_REVIEW");
            case "rootScope" -> change("mulino_responsibility_Roots",root,"scopeJson","{}");
            case "missingCommand" -> change("mulino_commands_CommandRecords",source,"ID",id());
            case "foreignCommand" -> change("mulino_commands_CommandRecords",source,"organizationId",id());
            case "hash" -> change("mulino_commands_CommandRecords",source,"canonicalHash","2".repeat(64));
            case "commandCreated","commandCompleted" -> change("mulino_commands_CommandRecords",source,variant.equals("commandCreated")?"createdAt":"completedAt",f.later);
            case "commandPending" -> change("mulino_commands_CommandRecords",source,"state","IN_PROGRESS");
            case "foreignDomain" -> change("mulino_trade_recall_Investigations",domain,"organizationId",id());
            case "missingDomain" -> change("mulino_trade_recall_Investigations",domain,"ID",id());
            case "domainCreated","domainRecorded" -> change("mulino_trade_recall_Investigations",domain,variant.equals("domainCreated")?"createdAt":"recordedAt",f.later);
            case "domainWork","domainPhysical","domainItem","domainLot" -> change("mulino_trade_recall_Investigations",domain,switch(variant){case "domainWork"->"workId";case "domainPhysical"->"segmentId";case "domainItem"->"itemId";default->"lotId";},id());
            case "differentCapability" -> change("mulino_commands_CommandRecords",source,"capabilityId","proposeRecall");
            case "differentIntentScope" -> change("mulino_commands_CommandRecords",source,"canonicalIntentJson","{\"capabilityId\":\"openInvestigation\",\"slots\":{}}");
            default -> {
                var result=Json.object().put("outcome","APPLIED").put("commandId",variant.equals("differentResultCommand")?id():source);
                result.putObject("effects").put("investigationId",variant.equals("differentCommandEffect")?id():domain);
                change("mulino_commands_CommandRecords",source,"resultJson",result.toString());
            }
        }
        absent(observed());
    }
    @ParameterizedTest @ValueSource(strings={"OPEN:true","RESOLVED:true","WAIVED:true","TRANSFERRED:false"})
    void currentRemainsValidIndependentlyOfStatus(String sample)throws Exception {
        fixture("DELIVERY_RESTRICTION_RESPONSE");var parts=sample.split(":");
        change("mulino_work_read_ObligationReferences",obligation,"status",parts[0]);change("mulino_work_read_ObligationReferences",obligation,"valid",Boolean.parseBoolean(parts[1]));
        var row=observed();assertEquals(parts[0],row.path("status").asText());assertEquals(Boolean.parseBoolean(parts[1]),row.path("current").asBoolean());
        assertEquals(row.path("valid"),row.path("current"));assertEquals(source,row.path("sourceOccurrenceId").asText());
    }
    @ParameterizedTest @ValueSource(strings={"EVENT","CLAIM","DOCUMENT","OCCURRENCE","WORK","APPROVAL"})
    void actualSourceKindsUseOnlyTheirOwnTablesAndTheirRealTimeAndVersionFields(String kind)throws Exception {
        fixture("QUALITY_REVIEW");String table=switch(kind){case "EVENT"->"mulino_evidence_Events";case "CLAIM"->"mulino_evidence_Claims";case "DOCUMENT"->"mulino_evidence_DocumentVersions";case "OCCURRENCE"->"mulino_evidence_CanonicalOccurrences";case "WORK"->"mulino_work_read_Works";default->"mulino_commands_Approvals";};
        String sourceKind=kind.equals("APPROVAL")?"DECISION":kind;String version=kind.equals("APPROVAL")?hash:"1";
        String actualSource=kind.equals("WORK")?f.work:id();
        if(kind.equals("APPROVAL"))f.insert(table,Json.object().put("ID",actualSource).put("organizationId",f.org).put("canonicalHash",hash).put("decidedAt",f.now));
        else if(!kind.equals("WORK"))f.insert(table,f.row(actualSource).put("revision",1));
        change("mulino_responsibility_Roots",root,"sourceKind",sourceKind);change("mulino_responsibility_Roots",root,"sourceId",actualSource);change("mulino_responsibility_Roots",root,"sourceVersion",version);
        if(kind.equals("WORK")) {
            String scope=Json.object().put("workId",f.work).toString();
            for(String target:List.of("mulino_responsibility_Roots","mulino_work_read_ObligationReferences")) {
                String targetId=target.endsWith("Roots")?root:obligation;change(target,targetId,"kind","WORK_NEXT_ACTION");change(target,targetId,"scopeJson",scope);
            }
        }
        // No blanket Command/Approval/Event/Canonical joins for a raw typed source.
        if(!kind.equals("APPROVAL"))try(var st=f.writer.createStatement()){st.execute("DROP TABLE mulino_commands_Approvals");}
        var row=observed();assertEquals(actualSource,row.path("sourceOccurrenceId").asText());assertEquals(sourceKind,row.path("sourceKind").asText());
        assertEquals(table,row.at("/sourceOccurrenceProof/sourceTable").asText());
        change(table,actualSource,kind.equals("APPROVAL")?"decidedAt":"createdAt",f.later);assertFalse(observed().hasNonNull("sourceOccurrenceId"));
    }
    @Test void danglingRootAndUnrelatedRequestedWorkNeverBecomeEvidence()throws Exception {
        fixture("RECALL_INVESTIGATION");
        assertTrue(ScenarioJdbcObservation.capture(f.config,f.root,f.request(f.scope().put("workId",id()),"obligations")).at("/rawRows/obligations").isEmpty());
        change("mulino_work_read_ObligationReferences",obligation,"rootId",null);
        var row=observed();assertFalse(row.hasNonNull("sourceOccurrenceId"));assertTrue(row.path("current").asBoolean());
    }
    @ParameterizedTest @ValueSource(strings={"duplicateRoot","duplicateSource","approvalAndCommand","unsupportedKind","missingVersion"})
    void unsupportedOrAmbiguousTypedIdentityStaysUnavailable(String variant)throws Exception {
        fixture("RECALL_INVESTIGATION");
        if(variant.equals("unsupportedKind"))change("mulino_responsibility_Roots",root,"sourceKind","EXTERNAL_EVENT");
        else if(variant.equals("missingVersion"))change("mulino_responsibility_Roots",root,"sourceVersion",null);
        else if(variant.equals("approvalAndCommand"))f.insert("mulino_commands_Approvals",Json.object().put("ID",source).put("organizationId",f.org).put("canonicalHash",hash).put("decidedAt",f.now));
        else {
            String table=variant.equals("duplicateRoot")?"mulino_responsibility_Roots":"mulino_commands_CommandRecords";
            try(var st=f.writer.prepareStatement("INSERT INTO "+table+" SELECT * FROM "+table+" WHERE ID=?")){st.setString(1,variant.equals("duplicateRoot")?root:source);assertEquals(1,st.executeUpdate());}
        }
        absent(observed());
    }
    @Test void causalReadUsesTheOriginalReadOnlySnapshotAcrossAConcurrentCommit()throws Exception {
        fixture("RECALL_INVESTIGATION");
        try(var reader=DriverManager.getConnection(f.config.jdbcUrl(),f.config.username(),f.config.password())) {
            reader.setAutoCommit(false);reader.setReadOnly(true);reader.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);ObserverSnapshot.token(reader);
            change("mulino_commands_CommandRecords",source,"canonicalHash","2".repeat(64));
            var p=new ScenarioObservationProjection(reader,f.root,List.of(f.org),List.of(f.item),List.of(f.work),List.of(f.lot),OffsetDateTime.parse(f.now),OffsetDateTime.parse(f.now));
            var row=(ObjectNode)p.read("mulino_work_read_ObligationReferences").get(0);p.obligationSource(row);
            assertEquals(source,row.path("sourceOccurrenceId").asText());assertEquals(hash,row.path("sourceVersion").asText());
            try(var st=reader.createStatement()){assertEquals("25006",assertThrows(SQLException.class,()->st.executeUpdate("DELETE FROM mulino_responsibility_Roots")).getSQLState());}
            reader.rollback();
        }
        absent(observed());
    }
}
