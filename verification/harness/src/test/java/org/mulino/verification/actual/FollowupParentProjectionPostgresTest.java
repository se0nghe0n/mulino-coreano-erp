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

@EnabledIfEnvironmentVariable(named="OBSERVER_PG_TEST",matches="true")
class FollowupParentProjectionPostgresTest {
    final ScenarioObservationPostgresTest f=new ScenarioObservationPostgresTest();
    String parent,link;
    @BeforeEach void setup(TestInfo info)throws Exception {
        f.setup(info);f.base();parent=ScenarioObservationPostgresTest.id();link=ScenarioObservationPostgresTest.id();
        f.insert("mulino_work_read_Works",f.row(parent).put("itemId",f.item).put("lotId",f.lot).put("status","CLOSED"));
        f.insert("mulino_work_WorkLinks",Json.object().put("ID",link).put("organizationId",f.org)
            .put("sourceWorkId",parent).put("targetWorkId",f.work).put("kind","FOLLOWUP").put("createdAt",f.now));
    }
    @AfterEach void cleanup()throws Exception {f.cleanup();}
    void change(String table,String record,String field,Object value)throws Exception {
        try(var st=f.writer.prepareStatement("UPDATE "+table+" SET "+field+"=? WHERE ID=?")) {
            st.setObject(1,field.endsWith("At")?OffsetDateTime.parse(value.toString()):value);st.setString(2,record);assertEquals(1,st.executeUpdate());
        }
    }
    JsonNode observed()throws Exception {return f.capture("works").at("/rawRows/works/0");}
    @Test void unchangedT11ParentLinkOracleUsesTheUniqueTypedLinkAndPreservesTheClosedParent()throws Exception {
        var captured=f.capture("works");var child=captured.at("/rawRows/works/0");assertEquals(parent,child.path("parentWorkId").asText());
        assertEquals("PROVEN_FOLLOWUP_LINK",child.at("/followupParentProof/status").asText());
        assertEquals(link,child.at("/followupParentProof/linkRow/id").asText());assertEquals("CLOSED",child.at("/followupParentProof/parentRow/status").asText());
        var assertion=find(Json.read(f.root.resolve("verification/cases/T11/case.json")),"followup-parent-link");assertNotNull(assertion);
        var envelope=Json.object().put("driverStatus","EXECUTED");envelope.set("data",captured);
        envelope.putObject("provenance").put("scopeComplete",true);
        var followup=Json.object().put("driverStatus","EXECUTED");followup.putObject("response").put("workId",f.work);
        followup.putObject("provenance").put("scopeComplete",true);
        new AssertionEngine().check(assertion,Map.of("after-db",envelope,"followup",followup),Json.object().put("PARENT",parent).put("ORG",f.org).put("P",f.item));
        String query=captured.at("/sourceEvidence/works/sourceQuery/sql").asText();
        for(String value:List.of("mulino_work_WorkLinks","targetWorkId=?","kind='FOLLOWUP'","createdAt<=?","boundValues",parent,f.work))assertTrue(query.contains(value),value);
        assertFalse(query.contains("FollowupLinks"));
    }
    JsonNode find(JsonNode node,String id) {
        if(node.isObject()&&id.equals(node.path("id").asText()))return node;
        if(node.isContainerNode())for(var child:node){var found=find(child,id);if(found!=null)return found;}return null;
    }
    @ParameterizedTest @ValueSource(strings={"foreignLink","futureLink","wrongChild","reversedLink","otherKind","foreignParent","futureParentCreated","futureParentRecorded","differentItem","danglingParent","selfParent","ambiguousChild","duplicateLink"})
    void foreignFutureDanglingOrAmbiguousRelationshipsNeverInventAParent(String variant)throws Exception {
        String links="mulino_work_WorkLinks",parents="mulino_work_read_Works",other=ScenarioObservationPostgresTest.id();
        switch(variant) {
            case "foreignLink" -> change(links,link,"organizationId",other);
            case "futureLink" -> change(links,link,"createdAt",f.later);
            case "wrongChild" -> change(links,link,"targetWorkId",other);
            case "reversedLink" -> {change(links,link,"targetWorkId",parent);change(links,link,"sourceWorkId",f.work);}
            case "otherKind" -> change(links,link,"kind","DEPENDS_ON");
            case "foreignParent" -> change(parents,parent,"organizationId",other);
            case "futureParentCreated","futureParentRecorded" -> change(parents,parent,variant.equals("futureParentCreated")?"createdAt":"recordedAt",f.later);
            case "differentItem" -> change(parents,parent,"itemId",other);
            case "danglingParent" -> change(links,link,"sourceWorkId",other);
            case "selfParent" -> change(links,link,"sourceWorkId",f.work);
            default -> f.insert(links,Json.object().put("ID",other).put("organizationId",f.org).put("sourceWorkId",variant.equals("duplicateLink")?parent:other).put("targetWorkId",f.work).put("kind","FOLLOWUP").put("createdAt",f.now));
        }
        var child=observed();assertEquals(f.work,child.path("id").asText());assertEquals("ACTIVE",child.path("status").asText());assertFalse(child.hasNonNull("parentWorkId"));
        assertEquals("UNAVAILABLE",child.at("/followupParentProof/status").asText());
    }
    @Test void linkAndParentUseTheSameReadOnlySnapshotAcrossConcurrentCommit()throws Exception {
        try(var reader=DriverManager.getConnection(f.config.jdbcUrl(),f.config.username(),f.config.password())) {
            reader.setAutoCommit(false);reader.setReadOnly(true);reader.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);ObserverSnapshot.token(reader);
            change("mulino_work_WorkLinks",link,"sourceWorkId",ScenarioObservationPostgresTest.id());
            var p=new ScenarioObservationProjection(reader,f.root,List.of(f.org),List.of(f.item),List.of(f.work),List.of(f.lot),OffsetDateTime.parse(f.now),OffsetDateTime.parse(f.now));
            var rows=p.read("mulino_work_read_Works");p.followupParent((ObjectNode)rows.get(0));assertEquals(parent,rows.get(0).path("parentWorkId").asText());
            try(var st=reader.createStatement()){assertEquals("25006",assertThrows(SQLException.class,()->st.executeUpdate("DELETE FROM mulino_work_WorkLinks")).getSQLState());}reader.rollback();
        }
        assertFalse(observed().hasNonNull("parentWorkId"));
    }
}
