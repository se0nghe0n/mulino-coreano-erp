package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.Json;

/** Real JDBC/MVCC observer tests. CSN-shaped test tables isolate projection from product write validation. */
@EnabledIfEnvironmentVariable(named="OBSERVER_PG_TEST",matches="true")
class ScenarioObservationPostgresTest {
    final Path root=Path.of(System.getProperty("repo.root"));
    final String org=id(), item=id(), work=id(), lot=id(), definition=id(), policy=id(), goal=id();
    final String now="2026-10-07T02:00:00Z", before="2026-10-07T00:00:00Z", later="2026-10-08T00:00:00Z";
    Connection writer;ActualConfiguration config;String schema;String testName;int captures;
    static String id(){return UUID.randomUUID().toString();}
    @BeforeEach void setup(TestInfo info) throws Exception {
        testName=info.getTestMethod().orElseThrow().getName();
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
        config=new ActualConfiguration(java.net.URI.create("http://127.0.0.1:1"),url+(url.contains("?")?"&":"?")+"currentSchema="+schema,"observer","observer-local",Path.of("unused"),"unused","unused","594958ee");
    }
    @AfterEach void cleanup() throws Exception {
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
    void base() throws Exception {
        insert("mulino_work_read_Works",row(work).put("itemId",item).put("lotId",lot).put("revision",2).put("status","ACTIVE").put("definitionVersionId",definition));
        var def=row(definition);def.remove("recordedAt");insert("mulino_definitions_DefinitionVersions",def.put("version","definition-original").put("contentHash","definition-hash"));
        var pol=row(policy);pol.remove("recordedAt");insert("mulino_governance_PolicyVersions",pol.put("version","policy-original").put("contentHash","policy-hash"));
        insert("mulino_work_read_GoalReferences",row(goal).put("workId",work).put("definitionVersionId",definition).put("slotsJson","{\"periodStart\":\"2026-10-07T00:00:00Z\",\"periodEnd\":\"2026-10-07T02:00:00Z\",\"periodEndInclusive\":false,\"evidencePolicyVersion\":\"stored-policy\"}"));
    }
    ObjectNode scope(){return Json.object().put("organizationId",org).put("itemId",item).put("workId",work).put("lotId",lot);}
    ObjectNode request(ObjectNode scope,String... sources){var r=Json.object().put("asOf",now).put("knownAt",now);r.set("scope",scope);r.set("sources",Json.MAPPER.valueToTree(sources));return r;}
    JsonNode capture(String... sources) throws Exception {
        var result=ScenarioJdbcObservation.capture(config,root,request(scope(),sources));
        if(System.getenv("OBSERVER_TEST_EVIDENCE")!=null)Json.write(Path.of(System.getenv("OBSERVER_TEST_EVIDENCE")).resolve(testName+"-"+(++captures)+".json"),result);
        return result;
    }
    ObjectNode assessment(String id,String workId,String at,String conditions){return row(id).put("workId",workId).put("goalId",goal).put("recordedAt",at).put("definitionVersionId",definition).put("policyVersionId",policy).put("evaluatorVersion","core-v1").put("outcome","UNVERIFIED").put("conditionsJson",conditions).put("inputSnapshotHash","snapshot-hash");}
    String conditions(){return "[{\"conditionId\":\"root.0\",\"truth\":{\"state\":\"UNVERIFIED\",\"conflict\":true},\"evidenceRefs\":[\"original-evidence\"],\"inputs\":[{\"state\":\"UNKNOWN\",\"value\":null,\"sourceId\":\"actual-source\"}]}]";}

    @Test void historicalVersionsAndImmutableInputDetailsKeepTheirStoredIdentities() throws Exception {
        base();String assessment=id(),snapshot=id();insert("mulino_work_read_AssessmentReferences",assessment(assessment,work,now,conditions()));
        var input=row(snapshot);input.remove("createdAt");insert("mulino_evaluation_InputSnapshots",input.put("assessmentId",assessment).put("workId",work).put("goalId",goal).put("contentHash","snapshot-hash").put("contentJson","{\"conditions\":"+conditions()+",\"goalId\":\""+goal+"\"}"));
        var newer=row(id());newer.remove("recordedAt");insert("mulino_definitions_DefinitionVersions",newer.put("version","definition-current"));
        var result=capture("assessments","assessmentInputs","conditionResults","goalVersions","goals");var raw=result.path("rawRows");var actual=raw.path("assessments").get(0);
        assertEquals("definition-original",actual.path("definitionVersion").asText());assertEquals("policy-original",actual.path("policyVersion").asText());assertEquals("core-v1",actual.path("evaluatorVersion").asText());
        assertEquals(goal,actual.path("goalVersionId").asText());assertEquals(snapshot,actual.path("inputSnapshotIds").get(0).asText());
        assertEquals("root.0",raw.path("conditionResults").get(0).path("conditionId").asText());assertTrue(raw.path("conditionResults").get(0).path("inputs").get(0).path("value").isNull());
        assertEquals("UNKNOWN",raw.path("assessmentInputs").get(0).path("inputs").get(0).path("state").asText());assertFalse(raw.path("conditionResults").get(0).has("quantity"));
        assertEquals(false,raw.path("goalVersions").get(0).path("endInclusive").booleanValue());assertEquals("stored-policy",raw.path("goalVersions").get(0).path("evidencePolicyVersion").asText());
        assertEquals(raw.path("goalVersions"),raw.path("goals"));assertEquals(result.path("sourceEvidence").path("goals").path("sourceQuery").path("sql"),result.path("sourceEvidence").path("goalVersions").path("sourceQuery").path("sql"));
        String sql=result.path("sourceEvidence").toString();assertTrue(sql.contains("boundValues"));assertTrue(sql.contains("organizationId=?"));assertTrue(sql.contains("policyVersion")||sql.contains("PolicyVersions"));
        assertEquals("REPEATABLE_READ",result.path("snapshot").path("isolation").asText());
    }

    @Test void sameNamedVersionDoesNotSubstituteForTheStoredHistoricalId() throws Exception {
        base();String foreignId=id();var foreign=row(foreignId).put("organizationId",id()).put("version","definition-original");foreign.remove("recordedAt");insert("mulino_definitions_DefinitionVersions",foreign);
        var a=assessment(id(),work,now,conditions());a.put("definitionVersionId",foreignId);insert("mulino_work_read_AssessmentReferences",a);
        var raw=capture("assessments").path("rawRows").path("assessments").get(0);
        assertFalse(raw.has("definitionVersion"));assertEquals("core-v1",raw.path("evaluatorVersion").asText());
    }

    @Test void scopeAndKnownAtExcludeOtherOrganizationsItemsWorksLotsAndFutureRows() throws Exception {
        base();String accepted=id();insert("mulino_work_read_AssessmentReferences",assessment(accepted,work,now,conditions()));
        for(String type:List.of("org","item","work","lot","future")) {
            String otherWork=id();var w=row(otherWork).put("itemId",type.equals("item")?id():item).put("lotId",type.equals("lot")?id():lot);
            if(type.equals("org"))w.put("organizationId",id());insert("mulino_work_read_Works",w);
            var a=assessment(id(),type.equals("future")?work:otherWork,type.equals("future")?later:now,conditions());a.put("organizationId",w.path("organizationId").asText());insert("mulino_work_read_AssessmentReferences",a);
            var snap=row(id()).put("organizationId",a.path("organizationId").asText()).put("assessmentId",a.path("ID").asText()).put("workId",a.path("workId").asText()).put("goalId",goal).put("recordedAt",a.path("recordedAt").asText()).put("contentJson","{\"conditions\":"+conditions()+"}");snap.remove("createdAt");insert("mulino_evaluation_InputSnapshots",snap);
        }
        var raw=capture("assessments","conditionResults","assessmentInputs").path("rawRows");assertTrue(raw.path("assessmentInputs").isEmpty());assertEquals(1,raw.path("assessments").size());assertEquals(accepted,raw.path("conditionResults").get(0).path("assessmentId").asText());
        assertEquals(1,raw.path("conditionResults").size());
        // Removing only the Work fence still retains the item and lot fences.
        var itemScope=scope();itemScope.remove("workId");var itemRaw=ScenarioJdbcObservation.capture(config,root,request(itemScope,"conditionResults")).path("rawRows").path("conditionResults");assertEquals(2,itemRaw.size());
    }

    @Test void approvalConditionsAreStoredDataAndNoConsumptionPolicyIsInvented() throws Exception {
        base();String proposal=id();insert("mulino_trade_purchase_ProposalRevisions",row(id()).put("proposalId",proposal).put("revision",7).put("proposalHash","exact-hash").put("workId",work).put("itemId",item));
        insert("mulino_trade_purchase_Approvals",row(id()).put("proposalId",proposal).put("proposalRevision",7).put("proposalHash","exact-hash").put("policyHash","approval-policy").put("approverId",id()).put("conditionsJson","[{\"selector\":\"unsupported-selector\"}]").put("decidedAt",now).put("validUntil",later));
        var result=capture("approvals").path("rawRows").path("approvals");assertEquals(1,result.size());var approval=result.get(0);assertEquals("unsupported-selector",approval.path("conditions").get(0).path("selector").asText());assertFalse(approval.has("consumptionPolicy"));assertEquals(7,approval.path("proposalRevision").asInt());assertEquals("approval-policy",approval.path("policyHash").asText());
    }

    @Test void workReasonsRequireTheExactStoredRevisionAndState() throws Exception {
        base();try(var s=writer.createStatement()){s.execute("UPDATE mulino_work_read_Works SET status='WAITING',waitJson='{\"reason\":\"actual-wait\"}'");}
        var observed=capture("works").path("rawRows").path("works").get(0);assertEquals("actual-wait",observed.path("waitReason").asText());assertTrue(observed.path("closeReason").isNull());
        try(var s=writer.createStatement()){s.execute("UPDATE mulino_work_read_Works SET status='CLOSED',waitJson=null");}
        var t=row(id());t.remove("createdAt");insert("mulino_work_WorkTransitions",t.put("workId",work).put("revision",1).put("state","CLOSED").put("reason","stale-reason").put("snapshotJson","{\"ID\":\""+work+"\",\"revision\":1}"));
        assertTrue(capture("works").path("rawRows").path("works").get(0).path("closeReason").isNull());
        t.put("ID",id()).put("revision",2).put("reason","actual-close").put("snapshotJson","{\"ID\":\""+work+"\",\"revision\":2}");insert("mulino_work_WorkTransitions",t);
        assertEquals("actual-close",capture("works").path("rawRows").path("works").get(0).path("closeReason").asText());
    }

    ObjectNode observation(String mutation) throws Exception {
        String event=id(),claim=id(),canonical=id(),doc=id(),segment=id(),profile=id();String quantity="37";
        insert("mulino_inventory_QuantitySegments",row(segment).put("itemId",item).put("lotId",lot).put("placeId","warehouse").put("quantity",quantity).put("unit","BOX"));
        var e=row(event).put("valueState","KNOWN").put("kind","STATE_OBSERVATION").put("subjectKind","SEGMENT").put("subjectId",segment).put("itemId",item).put("workId",work).put("placeId","warehouse").put("sourceProfileId",profile).put("externalEventId",event).put("payload","{\"activityKind\":\"STATE_OBSERVATION\",\"segmentId\":\""+segment+"\",\"placeId\":\"warehouse\",\"quantity\":\""+(mutation.equals("payload")?"100":quantity)+"\",\"unit\":\"BOX\",\"coverageStart\":\""+before+"\",\"coverageEnd\":\""+now+"\"}");
        insert("mulino_evidence_Events",e);
        insert("mulino_evidence_Claims",row(claim).put("valueState","KNOWN").put("subjectKind","SEGMENT").put("documentVersionId",mutation.equals("samevalue-document")?id():doc).put("eventId",event).put("workId",work).put("subjectId",segment).put("itemId",item).put("placeId","warehouse").put("quantity",quantity).put("unit","BOX").put("effectiveFrom",before).put("effectiveUntil","2026-10-07T02:00:00.000001Z"));
        insert("mulino_evidence_CanonicalOccurrences",row(canonical).put("subjectKind","SEGMENT").put("workId",mutation.equals("work")?id():work).put("kind",mutation.equals("kind")?"UNSUPPORTED":"STATE_OBSERVATION").put("subjectId",segment).put("itemId",item).put("placeId","warehouse").put("physicalScopeId",segment).put("quantity",mutation.equals("quantity")?"100":quantity).put("unit","BOX").put("valueState",mutation.equals("unknown")?"UNKNOWN":"KNOWN").put("effectiveFrom",before).put("effectiveUntil","2026-10-07T02:00:00.000001Z"));
        insert("mulino_evidence_DocumentVersions",row(doc).put("availability",mutation.equals("document")?"MISSING":"AVAILABLE").put("blobId",id()).put("sourceProfileId",profile).put("sourceReference",event));
        var v=row(id()).put("claimId",claim).put("canonicalOccurrenceId",canonical).put("basisDocumentId",doc).put("verdict","VERIFIED").put("sourceMatched",true).put("identityMatched",!mutation.equals("identity")).put("quantityMatched",true).put("timeMatched",true).put("duplicateChecked",true);
        if(mutation.equals("future"))v.put("recordedAt",later);if(mutation.equals("foreign"))v.put("organizationId",id());insert("mulino_evidence_Verifications",v);
        if(mutation.equals("superseded"))insert("mulino_evidence_Events",row(id()).put("supersedesId",event));
        return e;
    }

    @Test void coverageAndActivityQuantityComeFromTheVerifiedSameSnapshotChain() throws Exception {
        base();var e=observation("");var result=capture("observationCoverage","activities");var raw=result.path("rawRows");
        assertEquals(1,raw.path("observationCoverage").size());assertEquals(e.path("ID"),raw.path("observationCoverage").get(0).path("activityId"));assertEquals("37",raw.path("activities").get(0).path("quantity").asText());
        assertEquals(now,raw.path("observationCoverage").get(0).path("end").asText());assertTrue(result.path("sourceEvidence").toString().contains("Verifications"));
    }

    @Test void fabricatedIdentityValueUnverifiedOrFutureEvidenceCannotProvideCoverage() throws Exception {
        base();for(String mutation:List.of("payload","kind","quantity","document","identity","future","foreign","superseded","work","unknown","samevalue-document"))observation(mutation);
        var raw=capture("activities","observationCoverage").path("rawRows");assertTrue(raw.path("observationCoverage").isEmpty(),raw.toPrettyString());
        for(var e:raw.path("activities"))assertFalse(e.has("quantity"),e.toPrettyString());
    }

    @Test void rawQuantityMovementsAndUnknownTypedDecisionsAreNotHiddenOrChanged() throws Exception {
        base();String segment=id();insert("mulino_inventory_QuantitySegments",row(segment).put("itemId",item).put("lotId",lot));
        insert("mulino_inventory_QuantityMovements",row(id()).put("kind","INITIAL_BALANCE").put("quantity","100").put("unit","BOX").put("targetId",segment));
        insert("mulino_inventory_DispositionBases",row(id()).put("segmentId",id()).put("workId",work).put("category","QC").put("state","UNKNOWN").put("action","SELL"));
        // No matching segment means the scoped decision cannot be attributed to the requested item.
        var raw=capture("movements","conditions").path("rawRows");assertEquals(1,raw.path("movements").size());assertEquals("INITIAL_BALANCE",raw.path("movements").get(0).path("kind").asText());assertEquals("100",raw.path("movements").get(0).path("quantity").asText());assertTrue(raw.path("conditions").isEmpty());
        insert("mulino_inventory_DispositionBases",row(id()).put("segmentId",segment).put("workId",work).put("category","QC").put("state","UNKNOWN").put("action","SELL"));
        var decision=capture("conditions").path("rawRows").path("conditions").get(0);assertEquals("UNKNOWN",decision.path("status").asText());assertTrue(decision.path("quantity").isNull());assertEquals("SELL",decision.path("action").asText());
    }

    @Test void unsupportedVirtualScopeFailsClosedAndNoDatabaseWritesAreAllowed() throws Exception {
        base();assertThrows(UnsupportedOperationException.class,()->capture("transactions"));assertThrows(UnsupportedOperationException.class,()->capture("unmappedSource"));
        var descendants=scope().put("includeDescendants",true);assertThrows(UnsupportedOperationException.class,()->ScenarioJdbcObservation.capture(config,root,request(descendants,"works")));
        try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {
            c.setAutoCommit(false);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            var p=new ScenarioObservationProjection(c,root,List.of(org),List.of(item),List.of(work),List.of(lot),OffsetDateTime.parse(now));
            var first=p.read("mulino_work_read_Works");try(var s=writer.createStatement()){s.execute("UPDATE mulino_work_read_Works SET status='CLOSED'");}
            // Independent uncached query after a concurrent commit still sees the same MVCC state.
            var again=p.query("mulino_work_read_Works","r.organizationId=? AND r.ID=?",List.of(org,work));assertEquals(first.get(0).path("status"),again.get(0).path("status"));
            try(var s=c.createStatement()){assertThrows(SQLException.class,()->s.executeUpdate("UPDATE mulino_work_read_Works SET status='DRAFT'"));}c.rollback();
        }
    }
}
