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
import org.mulino.verification.ContractValidator;
import org.mulino.verification.StepResult;

/** Real JDBC/MVCC observer tests. CSN-shaped test tables isolate projection from product write validation. */
@EnabledIfEnvironmentVariable(named="OBSERVER_PG_TEST",matches="true")
class ScenarioObservationPostgresTest {
    final Path root=Path.of(System.getProperty("repo.root"));
    final String org=id(), item=id(), work=id(), lot=id(), definition=id(), policy=id(), goal=id();
    final String now="2026-10-07T02:00:00Z", before="2026-10-07T00:00:00Z", later="2026-10-08T00:00:00Z";
    Connection writer;ActualConfiguration config;String schema;String testName;int captures;
    ObserverPostgresTestDatabaseConfiguration database;
    static String id(){return UUID.randomUUID().toString();}
    @BeforeEach void setup(TestInfo info) throws Exception {
        testName=info.getTestMethod().orElseThrow().getName()+"-"+info.getDisplayName().replaceAll("[^A-Za-z0-9_-]","_");
        database=ObserverPostgresTestDatabaseConfiguration.fromEnvironment(System.getenv());
        schema="obs_"+UUID.randomUUID().toString().replace("-","");
        writer=database.openVerifiedConnection();
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
        config=new ActualConfiguration(java.net.URI.create("http://127.0.0.1:1"),database.schemaJdbcUrl(schema),database.username(),database.password(),Path.of("unused"),"unused","unused","594958ee");
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
        var request=request(scope(),sources);var result=ScenarioJdbcObservation.capture(config,root,request);
        String ref="verification/harness/target/evidence/observer-capture-"+UUID.randomUUID()+".json";
        ScenarioJdbcObservation.bindArtifact(result,request.path("sources"),ref);
        Json.write(root.resolve(ref),result);
        validate(request,result);
        if(System.getenv("OBSERVER_TEST_EVIDENCE")!=null)Json.write(Path.of(System.getenv("OBSERVER_TEST_EVIDENCE")).resolve(testName+"-"+(++captures)+".json"),result);
        return result;
    }
    StepResult step(JsonNode result) {
        var provenance=Json.object().put("adapter","actual-jdbc-observer-test").put("adapterVersion","1.0").put("buildVersion","FOCUSED_PG")
            .putNull("authenticatedActor").put("source","POSTGRESQL_JDBC").put("independent",true).put("scopeComplete",true);
        provenance.set("sourceQuery",result.path("sourceQuery"));provenance.set("snapshot",result.path("snapshot"));
        return new StepResult("observe",StepResult.DriverStatus.EXECUTED,result,null,null,provenance,List.of(result.path("snapshot").path("artifactRef").asText()));
    }
    void validate(JsonNode request,JsonNode result)throws Exception {
        var step=step(result);var validator=new ContractValidator(root);validator.result(step,"observe");validator.observation(request,step);
    }
    ObjectNode assessment(String id,String workId,String at,String conditions){return row(id).put("workId",workId).put("goalId",goal).put("recordedAt",at).put("definitionVersionId",definition).put("policyVersionId",policy).put("evaluatorVersion","core-v1").put("outcome","UNVERIFIED").put("conditionsJson",conditions).put("inputSnapshotHash","snapshot-hash");}
    String conditions(){return "[{\"conditionId\":\"root.0\",\"truth\":{\"state\":\"UNVERIFIED\",\"conflict\":true},\"evidenceRefs\":[\"original-evidence\"],\"inputs\":[{\"state\":\"UNKNOWN\",\"value\":null,\"sourceId\":\"actual-source\"}]}]";}

    record SupplierRows(ObjectNode order, ObjectNode revision, ObjectNode goal, ObjectNode reply) {}
    SupplierRows supplier(String kind,String quantity,String organization,String scopedItem,String scopedWork,String scopedLot) throws Exception {
        String proposal=id(),order=id(),revision=id(),goalId=id();
        var g=row(goalId).put("organizationId",organization).put("workId",scopedWork)
            .put("scopeJson",Json.object().put("itemId",scopedItem).put("lotId",scopedLot).toString());
        var p=row(revision).put("organizationId",organization).put("proposalId",proposal).put("revision",1)
            .put("proposalHash","original-hash").put("workId",scopedWork).put("itemId",scopedItem).put("goalVersionId",goalId);
        var o=row(order).put("organizationId",organization).put("proposalId",proposal).put("proposalRevision",1).put("proposalHash","original-hash");
        var r=row(id()).put("organizationId",organization).put("orderId",order).put("reply",kind).put("proposedQuantity",quantity).put("unit","BOX").put("canonicalOccurrenceId",id()).put("evidenceId",id());
        insert("mulino_work_read_GoalReferences",g);insert("mulino_trade_purchase_ProposalRevisions",p);
        insert("mulino_trade_purchase_Orders",o);insert("mulino_trade_purchase_SupplierReplies",r);
        return new SupplierRows(o,p,g,r);
    }
    void changeSupplier(String table,ObjectNode row,String field,Object value) throws Exception {
        try(var s=writer.prepareStatement("UPDATE "+table+" SET "+field+"=? WHERE organizationId=? AND ID=?")) {
            if(field.endsWith("At"))s.setObject(1,OffsetDateTime.parse(value.toString()));else s.setObject(1,value);
            s.setString(2,row.path("organizationId").asText());s.setString(3,row.path("ID").asText());assertEquals(1,s.executeUpdate());
        }
    }

    @ParameterizedTest @ValueSource(strings={"ACCEPT:100","REJECT:100","CHANGE:80"})
    void supplierAliasesPreserveEachReplyAndOriginalQuantity(String sample) throws Exception {
        var parts=sample.split(":");var fixture=supplier(parts[0],parts[1],org,item,work,lot);
        var result=capture("supplierCommitments");var rows=result.path("rawRows").path("supplierCommitments");assertEquals(1,rows.size());var actual=rows.get(0);
        assertEquals(fixture.reply().path("ID"),actual.path("id"));assertEquals(parts[0],actual.path("reply").asText());
        assertEquals(fixture.order().path("ID"),actual.path("orderId"));assertEquals(actual.path("orderId"),actual.path("purchaseId"));
        assertEquals(actual.path("proposedQuantity"),actual.path("quantity"));assertEquals(0,new java.math.BigDecimal(parts[1]).compareTo(new java.math.BigDecimal(actual.path("quantity").asText())));
        assertEquals("BOX",actual.path("unit").asText());assertEquals(item,actual.path("itemId").asText());assertEquals(work,actual.path("workId").asText());assertEquals(lot,actual.path("lotId").asText());
        assertEquals(fixture.revision().path("ID"),actual.path("proposalRevisionId"));assertEquals("original-hash",actual.path("proposalHash").asText());
        assertFalse(actual.has("supplierAcceptance"));assertFalse(actual.has("arrivalQuantity"));assertFalse(actual.has("transmissionStatus"));
        assertTrue(result.path("scopeComplete").asBoolean());assertEquals("REPEATABLE_READ",result.path("snapshot").path("isolation").asText());
        String evidence=result.path("sourceEvidence").path("supplierCommitments").toString();
        for(String fragment:List.of("p.itemId IN (?)","p.workId IN (?)","g.scopeJson::jsonb->>'lotId' IN (?)","p.proposalHash=o.proposalHash","o.recordedAt<=?","p.recordedAt<=?","boundValues",org,item,work,lot))assertTrue(evidence.contains(fragment),fragment);
    }

    @Test void supplierScopeHasPositiveAndEmptyProofWithoutOtherOrganizationsItemsWorksLotsOrFutureReplies() throws Exception {
        var accepted=supplier("ACCEPT","100",org,item,work,lot);
        supplier("REJECT","100",id(),item,work,lot);supplier("CHANGE","80",org,id(),work,lot);
        supplier("CHANGE","80",org,item,id(),lot);supplier("CHANGE","80",org,item,work,id());
        var future=supplier("CHANGE","80",org,item,work,lot);changeSupplier("mulino_trade_purchase_SupplierReplies",future.reply(),"recordedAt",later);
        var rows=capture("supplierCommitments").path("rawRows").path("supplierCommitments");assertEquals(1,rows.size());assertEquals(accepted.reply().path("ID"),rows.get(0).path("id"));
        var empty=ScenarioJdbcObservation.capture(config,root,request(scope().put("itemId",id()),"supplierCommitments"));
        assertTrue(empty.path("rawRows").path("supplierCommitments").isEmpty());assertTrue(empty.path("scopeComplete").asBoolean());
        var request=request(scope(),"supplierCommitments").put("knownAt",before);assertTrue(ScenarioJdbcObservation.capture(config,root,request).path("rawRows").path("supplierCommitments").isEmpty());
    }

    @ParameterizedTest @ValueSource(strings={"orderRecorded","orderCreated","revision","goal"})
    void supplierVisibleReplyWithTimeIneligibleImmutableLinkIsUnknown(String target) throws Exception {
        var f=supplier("ACCEPT","100",org,item,work,lot);
        switch(target) {
            case "orderRecorded" -> changeSupplier("mulino_trade_purchase_Orders",f.order(),"recordedAt",later);
            case "orderCreated" -> changeSupplier("mulino_trade_purchase_Orders",f.order(),"createdAt",later);
            case "revision" -> changeSupplier("mulino_trade_purchase_ProposalRevisions",f.revision(),"recordedAt",later);
            case "goal" -> changeSupplier("mulino_work_read_GoalReferences",f.goal(),"recordedAt",later);
        }
        assertTrue(assertThrows(UnsupportedOperationException.class,()->capture("supplierCommitments")).getMessage().contains("time-ineligible"));
    }

    @ParameterizedTest @ValueSource(strings={"missingOrder","duplicateOrder","foreignOrder","missingRevision","duplicateRevision","hashMismatch","missingGoal","duplicateGoal","goalWorkMismatch","goalItemMismatch"})
    void supplierMissingAmbiguousOrMismatchedLinksFailClosedEvenForAnEmptyRequestedScope(String target) throws Exception {
        var f=supplier("ACCEPT","100",org,item,work,lot);
        switch(target) {
            case "missingOrder" -> changeSupplier("mulino_trade_purchase_SupplierReplies",f.reply(),"orderId",id());
            case "duplicateOrder" -> insert("mulino_trade_purchase_Orders",f.order());
            case "foreignOrder" -> changeSupplier("mulino_trade_purchase_Orders",f.order(),"organizationId",id());
            case "missingRevision" -> changeSupplier("mulino_trade_purchase_Orders",f.order(),"proposalId",id());
            case "duplicateRevision" -> insert("mulino_trade_purchase_ProposalRevisions",f.revision().deepCopy().put("ID",id()).put("proposalHash","other-hash"));
            case "hashMismatch" -> changeSupplier("mulino_trade_purchase_Orders",f.order(),"proposalHash","other-hash");
            case "missingGoal" -> changeSupplier("mulino_trade_purchase_ProposalRevisions",f.revision(),"goalVersionId",id());
            case "duplicateGoal" -> insert("mulino_work_read_GoalReferences",f.goal());
            case "goalWorkMismatch" -> changeSupplier("mulino_work_read_GoalReferences",f.goal(),"workId",id());
            case "goalItemMismatch" -> changeSupplier("mulino_work_read_GoalReferences",f.goal(),"scopeJson",Json.object().put("itemId",id()).put("lotId",lot).toString());
        }
        assertThrows(UnsupportedOperationException.class,()->ScenarioJdbcObservation.capture(config,root,request(scope().put("itemId",id()),"supplierCommitments")));
    }

    @Test void supplierLotRequiresExplicitRevisionPinnedGoalRatherThanCurrentWork() throws Exception {
        base();var f=supplier("ACCEPT","100",org,item,work,lot);
        changeSupplier("mulino_trade_purchase_ProposalRevisions",f.revision(),"goalVersionId",null);
        assertTrue(assertThrows(UnsupportedOperationException.class,()->capture("supplierCommitments")).getMessage().contains("LOT"));
        var noLot=scope();noLot.remove("lotId");var rows=ScenarioJdbcObservation.capture(config,root,request(noLot,"supplierCommitments")).path("rawRows").path("supplierCommitments");
        assertEquals(1,rows.size());assertFalse(rows.get(0).has("lotId"));assertEquals(item,rows.get(0).path("itemId").asText());
        changeSupplier("mulino_trade_purchase_ProposalRevisions",f.revision(),"goalVersionId",f.goal().path("ID").asText());
        changeSupplier("mulino_work_read_GoalReferences",f.goal(),"scopeJson",Json.object().put("itemId",item).toString());
        assertThrows(UnsupportedOperationException.class,()->capture("supplierCommitments"));
    }

    @Test void supplierUsesImmutableRevisionAndGoalDespiteNewerProposalAndCurrentWorkLot() throws Exception {
        base();var f=supplier("ACCEPT","100",org,item,work,lot);
        insert("mulino_trade_purchase_Proposals",row(f.order().path("proposalId").asText()).put("currentRevision",2).put("workId",work));
        insert("mulino_trade_purchase_ProposalRevisions",f.revision().deepCopy().put("ID",id()).put("revision",2).put("proposalHash","new-hash").put("itemId",id()).put("workId",id()).put("goalVersionId",id()));
        changeSupplier("mulino_work_read_Works",row(work),"lotId",id());
        var actual=capture("supplierCommitments").path("rawRows").path("supplierCommitments").get(0);
        assertEquals(f.revision().path("ID"),actual.path("proposalRevisionId"));assertEquals(item,actual.path("itemId").asText());assertEquals(work,actual.path("workId").asText());assertEquals(lot,actual.path("lotId").asText());
    }

    @Test void supplierReadOnlySnapshotExcludesLaterCommitAndDoesNotWrite() throws Exception {
        var f=supplier("ACCEPT","100",org,item,work,lot);
        try(var reader=database.openSchemaConnection(schema)) {
            reader.setAutoCommit(false);reader.setReadOnly(true);reader.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);ObserverSnapshot.token(reader);
            supplier("CHANGE","80",org,item,work,lot);
            var projection=new ScenarioObservationProjection(reader,root,List.of(org),List.of(item),List.of(work),List.of(lot),OffsetDateTime.parse(now));
            var rows=projection.read("mulino_trade_purchase_SupplierReplies");assertEquals(1,rows.size());assertEquals(f.reply().path("ID"),rows.get(0).path("id"));
            try(var s=reader.createStatement()) {
                try(var state=s.executeQuery("SHOW transaction_read_only")){assertTrue(state.next());assertEquals("on",state.getString(1));}
                assertEquals("25006",assertThrows(SQLException.class,()->s.executeUpdate("DELETE FROM mulino_trade_purchase_SupplierReplies")).getSQLState());
            }
            reader.rollback();
        }
        assertEquals(2,capture("supplierCommitments").path("rawRows").path("supplierCommitments").size());
        try(var s=writer.createStatement();var rows=s.executeQuery("SELECT count(*) FROM mulino_trade_purchase_SupplierReplies")){assertTrue(rows.next());assertEquals(2,rows.getInt(1));}
    }

    @Test void goalInputProjectionReplaysCapturedImmutableRowsInFreshPostgres() throws Exception {
        String directory=System.getenv("T09_GOAL_INPUT_SOURCE");
        Assumptions.assumeTrue(directory!=null,"Optional source-row replay requires T09_GOAL_INPUT_SOURCE");
        for(String file:List.of("completeCumulativeSources-http.json","completeExistsSourcesAndInternalAuthorizationSeam-http.json")) {
            var artifact=Json.read(Path.of(directory).resolve(file));
            var tables=Map.of("goalReferences","mulino_work_read_GoalReferences","assessmentInputSnapshots","mulino_evaluation_InputSnapshots");
            var replayRows=new LinkedHashMap<String,LinkedHashMap<String,ObjectNode>>();String organization=null;
            var httpAssessments=new LinkedHashMap<String,List<JsonNode>>();
            for(var receipt:artifact.path("httpReceipts"))for(var entry:tables.entrySet())for(var source:receipt.path("response").path("data").path(entry.getKey())) {
                var row=Json.object();var names=ScenarioJdbcObservation.names(root,entry.getValue());
                for(var fields=source.fields();fields.hasNext();) {
                    var field=fields.next();String name=field.getKey();
                    if(Set.of("slots","scope","provenance","conditions").contains(name))name+="Json";
                    if(!names.containsValue(name))continue;
                    row.set(name,field.getValue().isContainerNode()?Json.MAPPER.valueToTree(field.getValue().toString()):field.getValue());
                }
                String rowId=source.path("ID").asText();assertFalse(rowId.isBlank(),"Source row has no ID: "+entry.getKey());
                var sourceRows=replayRows.computeIfAbsent(entry.getKey(),ignored->new LinkedHashMap<>());
                var previous=sourceRows.putIfAbsent(rowId,row);
                if(previous!=null)assertEquals(previous,row,"Repeated HTTP row changed: "+entry.getKey()+"/"+rowId);
                else insert(entry.getValue(),row);
                String rowOrganization=source.path("organizationId").asText();assertFalse(rowOrganization.isBlank(),"Source row has no organizationId: "+entry.getKey());
                if(organization==null)organization=rowOrganization;else assertEquals(organization,rowOrganization,"T09 source replay crossed organizations");
            }
            for(var receipt:artifact.path("httpReceipts"))for(var source:receipt.path("response").path("data").path("assessmentReferences")) {
                String rowId=source.path("ID").asText();assertFalse(rowId.isBlank(),"HTTP assessment has no ID");
                httpAssessments.computeIfAbsent(rowId,ignored->new ArrayList<>()).add(source.deepCopy());
                String rowOrganization=source.path("organizationId").asText();assertFalse(rowOrganization.isBlank(),"HTTP assessment has no organizationId");
                if(organization==null)organization=rowOrganization;else assertEquals(organization,rowOrganization,"HTTP assessment crossed organizations");
            }
            var snapshots=replayRows.getOrDefault("assessmentInputSnapshots",new LinkedHashMap<>());
            assertNotNull(organization);assertFalse(snapshots.isEmpty());assertFalse(httpAssessments.isEmpty());
            var assessmentNames=ScenarioJdbcObservation.names(root,T09CaptureReplayRows.ASSESSMENT_TABLE);
            var assessments=T09CaptureReplayRows.persistedRows(artifact,T09CaptureReplayRows.ASSESSMENT_TABLE,organization,assessmentNames);
            var snapshotsByAssessment=new LinkedHashMap<String,ObjectNode>();
            for(var snapshot:snapshots.values()) {
                String assessmentId=snapshot.path("assessmentId").asText();assertFalse(assessmentId.isBlank(),"Immutable input has no assessment ID");
                assertNull(snapshotsByAssessment.putIfAbsent(assessmentId,snapshot),"Multiple input snapshots share one assessment");
            }
            for(var entry:httpAssessments.entrySet()) {
                var persisted=assessments.get(entry.getKey());assertNotNull(persisted,"HTTP assessment is absent from persisted source rows: "+entry.getKey());
                var snapshot=snapshotsByAssessment.get(entry.getKey());
                for(var projection:entry.getValue())T09CaptureReplayRows.validateAssessmentProjection(projection,persisted,snapshot,assessmentNames);
            }
            for(var assessment:assessments.values())insert(T09CaptureReplayRows.ASSESSMENT_TABLE,assessment);
            var captures=T09CaptureReplayRows.referencedCaptures(artifact,snapshots.values(),ScenarioJdbcObservation.names(root,T09CaptureReplayRows.TABLE));
            var capturesById=new LinkedHashMap<String,ObjectNode>();
            for(var capture:captures){insert(T09CaptureReplayRows.TABLE,capture);capturesById.put(capture.path("ID").asText(),capture);}
            for(var snapshot:snapshots.values()) {
                assertEquals(inputHash(snapshot.path("contentJson").asText()),snapshot.path("contentHash").asText(),"Envelope hash changed before replay");
                if(!snapshot.hasNonNull("evidenceCaptureId"))continue; // Keep legacy envelopes without backfill.
                var capture=capturesById.get(snapshot.path("evidenceCaptureId").asText());assertNotNull(capture,"Referenced source capture was not exported");
                assertEquals(snapshot.path("organizationId"),capture.path("organizationId"));
                assertEquals(java.time.OffsetDateTime.parse(snapshot.path("asOf").asText()).toInstant(),java.time.OffsetDateTime.parse(capture.path("asOf").asText()).toInstant());
                assertEquals(java.time.OffsetDateTime.parse(snapshot.path("knownAt").asText()).toInstant(),java.time.OffsetDateTime.parse(capture.path("knownAt").asText()).toInstant());
                assertEquals(inputHash(capture.path("contentJson").asText()),capture.path("contentHash").asText(),"Capture hash differs from persisted bytes");
                var envelope=Json.parse(snapshot.path("contentJson").asText());var captured=Json.parse(capture.path("contentJson").asText());
                assertEquals(envelope.path("captureBoundary"),captured.path("boundary"),"Capture boundary differs from immutable envelope");
                assertEquals(envelope.path("facts"),captured.path("facts"),"Capture facts differ from immutable envelope");
                assertEquals(snapshot.path("organizationId"),captured.at("/boundary/organizationId"),"Capture boundary organization differs");
            }
            var result=ScenarioJdbcObservation.capture(config,root,request(Json.object().put("organizationId",organization),"goalVersions","assessmentInputs").put("knownAt","2026-10-07T04:00:00Z"));
            var actualEnvelopeIds=new HashSet<String>();for(var input:result.path("rawRows").path("assessmentInputs")) {
                String envelopeId=input.path("inputEnvelopeId").asText(input.path("id").asText());
                var snapshot=snapshots.get(envelopeId);assertNotNull(snapshot,"Projection invented an input envelope: "+envelopeId);
                actualEnvelopeIds.add(envelopeId);
                if(snapshot.hasNonNull("evidenceCaptureId"))assertEquals(snapshot.path("evidenceCaptureId"),input.path("snapshotId"),"snapshotId must identify the actual shared capture");
                else assertEquals(snapshot.path("ID"),input.path("snapshotId"),"Legacy envelope identity must remain unchanged");
                assertEquals(snapshot.path("ID"),input.path("id"),"Projected id remains the envelope ID");
                if(input.hasNonNull("inputEnvelopeId"))assertEquals(snapshot.path("ID"),input.path("inputEnvelopeId"));
                assertEquals(snapshot.path("contentJson"),input.path("contentJson"));
                assertEquals(snapshot.path("contentHash"),input.path("contentHash"));
                assertEquals(input.path("contentHash").asText(),inputHash(input.path("contentJson").asText()));
                assertEquals(Json.parse(snapshot.path("contentJson").asText()),input.path("inputSnapshot"));
            }
            assertEquals(snapshots.keySet(),actualEnvelopeIds,"Every captured envelope must be represented without assuming one output row per envelope");
            if(System.getenv("OBSERVER_TEST_EVIDENCE")!=null)Json.write(Path.of(System.getenv("OBSERVER_TEST_EVIDENCE")).resolve(file+"-fresh-pg-projection.json"),result);
        }
    }

    static String inputHash(String content) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    ObjectNode inputFixture() throws Exception {
        base();String assessment=id();
        var content=Json.object().put("goalId",goal);content.set("conditions",Json.parse(conditions()));
        String text=content.toString(),hash=inputHash(text);
        insert("mulino_work_read_AssessmentReferences",assessment(assessment,work,now,conditions()).put("asOf",now).put("knownAt",now).put("inputSnapshotHash",hash));
        var snapshot=Json.object().put("ID",id()).put("organizationId",org).put("recordedAt",now).put("assessmentId",assessment).put("workId",work).put("goalId",goal).put("asOf",now).put("knownAt",now).put("contentHash",hash).put("contentJson",text);
        insert("mulino_evaluation_InputSnapshots",snapshot);return snapshot;
    }
    @Test void goalInputProjectionKeepsActualSnapshotAndGoalSlots() throws Exception {
        var snapshot=inputFixture();
        String slots="{\"periodStart\":\"2026-10-07T00:00:00Z\",\"periodEnd\":\"2026-10-07T02:00:00Z\",\"placeId\":\"stored-place\",\"eventKind\":\"CONFIRMED_RECEIPT\",\"contributionScope\":\"stored-scope\",\"evaluationAt\":\"2026-10-07T02:00:00Z\",\"action\":\"PHYSICAL_HELD\",\"includeReserved\":false}";
        changeSupplier("mulino_work_read_GoalReferences",row(goal),"slotsJson",slots);
        changeSupplier("mulino_work_read_GoalReferences",row(goal),"targetQuantity",new java.math.BigDecimal("123.75"));
        var result=capture("goalVersions","assessmentInputs");var g=result.at("/rawRows/goalVersions/0");
        assertEquals("stored-place",g.path("placeId").asText());assertEquals(Json.parse(slots).path("periodStart"),g.path("periodStart"));
        assertEquals(g.path("targetQuantity"),g.path("quantity"));assertFalse(g.path("includeReservations").asBoolean());
        assertEquals("CONFIRMED_RECEIPT",g.path("eventKinds").get(0).asText());assertEquals("stored-scope",g.path("distinctContributionScope").asText());
        for(var input:result.path("rawRows").path("assessmentInputs")) {
            assertEquals(snapshot.path("ID"),input.path("snapshotId"));assertEquals(input.path("id"),input.path("snapshotId"));
            assertEquals(snapshot.path("contentJson"),input.path("contentJson"));assertFalse(input.has("segmentId"));assertFalse(input.has("movementId"));
        }
        assertTrue(result.path("sourceEvidence").toString().contains("assessmentId=?"));
    }
    @Test void goalInputProjectionRetainsReadOnlyRepeatableReadAndKnownAtFence() throws Exception {
        var snapshot=inputFixture();
        try(var reader=database.openSchemaConnection(schema)) {
            reader.setAutoCommit(false);reader.setReadOnly(true);reader.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);ObserverSnapshot.token(reader);
            insert("mulino_evaluation_InputSnapshots",snapshot.deepCopy().put("ID",id()));
            var projection=new ScenarioObservationProjection(reader,root,List.of(org),List.of(item),List.of(work),List.of(lot),OffsetDateTime.parse(now));
            assertEquals(1,projection.virtual("@assessmentInputs").size());
            try(var statement=reader.createStatement();var state=statement.executeQuery("SHOW transaction_read_only")){assertTrue(state.next());assertEquals("on",state.getString(1));}
            reader.rollback();
        }
        assertThrows(UnsupportedOperationException.class,()->capture("assessmentInputs"));
        var beforeRequest=request(scope(),"assessmentInputs").put("knownAt",before);
        assertTrue(ScenarioJdbcObservation.capture(config,root,beforeRequest).path("rawRows").path("assessmentInputs").isEmpty());
    }

    @ParameterizedTest @ValueSource(strings={"organizationId","workId","goalId","contentHash","knownAt","conditions","missingAssessment","duplicateSnapshot"})
    void goalInputProjectionRejectsUnboundOrTamperedSnapshots(String field) throws Exception {
        var snapshot=inputFixture();
        switch(field) {
            case "missingAssessment" -> changeSupplier("mulino_evaluation_InputSnapshots",snapshot,"assessmentId",id());
            case "duplicateSnapshot" -> insert("mulino_evaluation_InputSnapshots",snapshot.deepCopy().put("ID",id()));
            case "conditions" -> changeSupplier("mulino_evaluation_InputSnapshots",snapshot,"contentJson","{\"goalId\":\""+goal+"\",\"conditions\":[]}");
            case "knownAt" -> changeSupplier("mulino_work_read_AssessmentReferences",row(snapshot.path("assessmentId").asText()),field,later);
            case "organizationId","workId" -> changeSupplier("mulino_work_read_AssessmentReferences",row(snapshot.path("assessmentId").asText()),field,id());
            default -> changeSupplier("mulino_evaluation_InputSnapshots",snapshot,field,id());
        }
        assertThrows(UnsupportedOperationException.class,()->capture("assessmentInputs"));
    }

    @Test void historicalVersionsAndImmutableInputDetailsKeepTheirStoredIdentities() throws Exception {
        base();String assessment=id(),snapshot=id();
        String content="{\"conditions\":"+conditions()+",\"goalId\":\""+goal+"\"}";
        String hash=inputHash(content);
        insert("mulino_work_read_AssessmentReferences",assessment(assessment,work,now,conditions()).put("asOf",now).put("knownAt",now).put("inputSnapshotHash",hash));
        var input=row(snapshot);input.remove("createdAt");insert("mulino_evaluation_InputSnapshots",input.put("assessmentId",assessment).put("workId",work).put("goalId",goal).put("asOf",now).put("knownAt",now).put("contentHash",hash).put("contentJson",content));
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
        var result=capture("approvals").path("rawRows").path("approvals");assertEquals(1,result.size());var approval=result.get(0);assertEquals("unsupported-selector",approval.path("conditions").get(0).path("selector").asText());assertTrue(approval.path("consumptionPolicy").isNull());assertEquals(7,approval.path("proposalRevision").asInt());assertEquals("approval-policy",approval.path("policyHash").asText());
        // V38 preserves historical NULL while new decisions store the policy.
        // This CSN-shaped fixture tests projection only; product immutability is
        // separately exercised by PurchaseApprovalMigrationPostgresTest.
        try(var statement=writer.prepareStatement("UPDATE mulino_trade_purchase_Approvals SET consumptionPolicy=? WHERE organizationId=?")) {
            statement.setString(1,"SINGLE_ORDER_REVISION");statement.setString(2,org);assertEquals(1,statement.executeUpdate());
        }
        assertEquals("SINGLE_ORDER_REVISION",capture("approvals").path("rawRows").path("approvals").get(0).path("consumptionPolicy").asText());
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

    @Test void emptyRecoverySupersetAllowsNarrowScopeAlongsideOtherObservedSources() throws Exception {
        base();insert("mulino_work_read_AssessmentReferences",assessment(id(),work,now,conditions()));
        insert("mulino_work_read_AssessmentReferences",assessment(id(),work,later,conditions()));
        var result=capture("supervisionIssues","works","goalVersions","conditionResults");
        var raw=result.path("rawRows");assertTrue(raw.path("supervisionIssues").isArray());assertTrue(raw.path("supervisionIssues").isEmpty());
        assertEquals(1,raw.path("works").size());assertEquals(1,raw.path("goalVersions").size());assertEquals(1,raw.path("conditionResults").size());
        var proofs=Json.parse(result.path("sourceEvidence").path("supervisionIssues").path("sourceQuery").path("sql").asText()).get(0).path("queries");
        assertEquals(4,proofs.size());
        for(var proof:proofs) {
            assertTrue(proof.path("sql").asText().contains("?"));
            assertTrue(proof.path("boundValues").toString().contains(org));
        }
        assertTrue(proofs.get(3).path("sql").asText().contains("recordedAt<=?"));
        assertEquals(now,result.path("knownAt").asText());assertEquals("REPEATABLE_READ",result.path("snapshot").path("isolation").asText());
    }

    @Test void recoveryRowsOfUnrequestedOrganizationsDoNotInvalidateEmptyProof() throws Exception {
        base();String foreign=id();
        insert("mulino_runtime_RecoverySchedules",Json.object().put("ID",id()).put("organizationId",foreign).put("recoveryStatus","HELD_MANUAL"));
        insert("mulino_runtime_IntakeRecoveries",Json.object().put("ID",id()).put("organizationId",foreign).put("state","HELD_SOURCE"));
        assertTrue(capture("supervisionIssues","works").path("rawRows").path("supervisionIssues").isEmpty());
        var both=scope();both.remove("organizationId");both.set("organizationIds",Json.array().add(org).add(foreign));
        assertThrows(UnsupportedOperationException.class,()->ScenarioJdbcObservation.capture(config,root,request(both,"supervisionIssues")));
    }

    @Test void anyRecoveryRowInRequestedOrganizationRemainsFailClosedWithoutHistory() throws Exception {
        base();
        for(String table:List.of("mulino_runtime_RecoverySchedules","mulino_runtime_IntakeRecoveries")) {
            for(String state:List.of("HELD_MANUAL","QUEUED","DONE")) {
                // Deliberately unrelated Work and a future check: neither proves historical absence.
                var recovery=Json.object().put("ID",id()).put("organizationId",org).put("nextCheckAt",later);
                if(table.endsWith("RecoverySchedules"))recovery.put("recoveryStatus",state).put("workId",id());
                else recovery.put("state",state).put("linkedWorkId",id()).put("createdWorkId",id());
                insert(table,recovery);
                var denied=assertThrows(UnsupportedOperationException.class,()->capture("supervisionIssues","works"),table+" "+state);
                assertTrue(denied.getMessage().contains("superset is nonempty"));
                try(var statement=writer.createStatement()){statement.executeUpdate("DELETE FROM "+table);}
            }
        }
    }

    @Test void emptySupersetProofUsesItsOriginalReadOnlySnapshotAcrossConcurrentInsert() throws Exception {
        base();
        try(var c=database.openSchemaConnection(schema)) {
            c.setAutoCommit(false);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            ObserverSnapshot.token(c); // Establish the snapshot before the independent writer commits.
            insert("mulino_runtime_IntakeRecoveries",Json.object().put("ID",id()).put("organizationId",org).put("state","HELD_SOURCE"));
            var projection=new ScenarioObservationProjection(c,root,List.of(org),List.of(item),List.of(work),List.of(lot),OffsetDateTime.parse(now));
            assertTrue(projection.virtual("@supervisionIssues").isEmpty());assertEquals(4,projection.drainQueries().size());c.rollback();
        }
        assertThrows(UnsupportedOperationException.class,()->capture("supervisionIssues"));
    }

    void installHistory() throws Exception {
        try(var st=writer.createStatement()){
            st.execute("DROP TABLE mulino_runtime_RecoveryObservationHistory");
            st.execute(Files.readString(root.resolve("database/migrations/V39__recovery_observation_history.sql")));
        }
    }
    void change(String time,String sql,Object... args) throws Exception {
        writer.setAutoCommit(false);
        try {
            try(var st=writer.prepareStatement("SELECT set_config('mulino.recovery_recorded_at',?,true)")){st.setString(1,time);st.execute();}
            try(var st=writer.prepareStatement(sql)){for(int n=0;n<args.length;n++)st.setObject(n+1,args[n]);st.executeUpdate();}
            writer.commit();
        }catch(Exception failure){writer.rollback();throw failure;}finally{writer.setAutoCommit(true);}
    }
    String schedule(String time,String source,String selectedWork) throws Exception {
        change(time,"INSERT INTO mulino_runtime_RecoverySchedules(organizationId,ID,workId,revision,recoveryStatus,ownerId,supervisorId,nextAction,nextCheckAt) VALUES(?,?,?,0,'HELD_MANUAL',?,?,'Stored human action',?)",org,source,selectedWork,id(),id(),OffsetDateTime.parse(time));return source;
    }
    JsonNode at(String time,ObjectNode selectedScope) throws Exception {
        var request=request(selectedScope,"supervisionIssues");request.put("knownAt",time);
        var result=ScenarioJdbcObservation.capture(config,root,request);
        if(System.getenv("OBSERVER_TEST_EVIDENCE")!=null)Json.write(Path.of(System.getenv("OBSERVER_TEST_EVIDENCE")).resolve(testName+"-"+(++captures)+".json"),result);
        return result;
    }
    @Test void realHistoryReconstructsHeldCompletedAndDeletedSourcesAtKnownAt() throws Exception {
        base();installHistory();String source=schedule(now,id(),work);
        var observed=at(now,scope());assertFalse(observed.path("sourceEvidence").path("supervisionIssues").path("approximateMapping").asBoolean());
        var held=observed.path("rawRows").path("supervisionIssues");assertEquals(1,held.size());assertEquals(source,held.get(0).path("id").asText());assertEquals("HELD_MANUAL",held.get(0).path("status").asText());assertEquals("Stored human action",held.get(0).path("nextAction").asText());assertEquals(item,held.get(0).path("itemId").asText());
        change(later,"UPDATE mulino_runtime_RecoverySchedules SET recoveryStatus='CHECKED',revision=revision+1 WHERE ID=?",source);
        assertEquals(1,at(now,scope()).path("rawRows").path("supervisionIssues").size());assertTrue(at(later,scope()).path("rawRows").path("supervisionIssues").isEmpty());
        change(later,"UPDATE mulino_runtime_RecoverySchedules SET recoveryStatus='HELD_MANUAL',revision=revision+1 WHERE ID=?",source);
        assertEquals(2,at(later,scope()).path("rawRows").path("supervisionIssues").get(0).path("revision").asInt());
        change(later,"DELETE FROM mulino_runtime_RecoverySchedules WHERE ID=?",source);
        assertTrue(at(later,scope()).path("rawRows").path("supervisionIssues").isEmpty());assertEquals(1,at(now,scope()).path("rawRows").path("supervisionIssues").size());
        assertTrue(at(before,scope()).path("rawRows").path("supervisionIssues").isEmpty());
    }
    @Test void historyScopeTimeMvccAndReadOnlyAreIndependentOfCurrentSource() throws Exception {
        base();installHistory();String source=schedule(now,id(),work);
        for(String field:List.of("organizationId","itemId","workId","lotId"))assertTrue(at(now,scope().put(field,id())).path("rawRows").path("supervisionIssues").isEmpty(),field);
        assertTrue(at(before,scope()).path("rawRows").path("supervisionIssues").isEmpty());
        try(var c=database.openSchemaConnection(schema)) {
            c.setAutoCommit(false);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);ObserverSnapshot.token(c);
            change(later,"DELETE FROM mulino_runtime_RecoverySchedules WHERE ID=?",source);
            var p=new ScenarioObservationProjection(c,root,List.of(org),List.of(item),List.of(work),List.of(lot),OffsetDateTime.parse(later));assertEquals(1,p.virtual("@supervisionIssues").size());
            try(var st=c.createStatement()){assertThrows(SQLException.class,()->st.executeUpdate("DELETE FROM mulino_runtime_RecoveryObservationHistory"));}c.rollback();
        }
        assertTrue(at(later,scope()).path("rawRows").path("supervisionIssues").isEmpty());
    }
    @Test void preMigrationRowsStayUnknownEvenAfterUpdateOrDeletion() throws Exception {
        base();String source=id();insert("mulino_runtime_RecoverySchedules",Json.object().put("organizationId",org).put("ID",source).put("workId",work).put("revision",0).put("recoveryStatus","READY"));installHistory();
        assertThrows(UnsupportedOperationException.class,()->at(now,scope()));
        change(now,"UPDATE mulino_runtime_RecoverySchedules SET recoveryStatus='HELD_MANUAL' WHERE ID=?",source);assertThrows(UnsupportedOperationException.class,()->at(now,scope()));
        change(later,"DELETE FROM mulino_runtime_RecoverySchedules WHERE ID=?",source);assertThrows(UnsupportedOperationException.class,()->at(later,scope()));
    }

    @Test void intakeHistoryUsesImmutableEventScopeAndPreservesOriginalResponsibility() throws Exception {
        base();String event=id(),inbox=id(),owner=id(),supervisor=id();
        // An explicit Work relation proves its item/LOT even when the event omits the redundant item field.
        insert("mulino_evidence_Events",row(event).put("workId",work).put("subjectKind","WORK").put("subjectId",work));
        insert("mulino_evidence_InboxRecords",row(inbox).put("eventId",event));installHistory();
        change(now,"INSERT INTO mulino_runtime_IntakeRecoveries(organizationId,ID,eventId,sourceProfileId,intakeOwnerId,supervisorId,nextAction,nextCheckAt,state,revision) VALUES(?,?,?,?,?,?,'Resolve actual source',?,'HELD_POLICY',3)",org,inbox,event,id(),owner,supervisor,OffsetDateTime.parse(now));
        var issue=at(now,scope()).path("rawRows").path("supervisionIssues").get(0);assertEquals(inbox,issue.path("id").asText());assertEquals(owner,issue.path("ownerId").asText());assertEquals(supervisor,issue.path("supervisorId").asText());assertEquals("HELD_POLICY",issue.path("state").asText());assertEquals(3,issue.path("revision").asInt());
        change(later,"UPDATE mulino_runtime_IntakeRecoveries SET state='LINKED',linkedWorkId=?,revision=revision+1 WHERE ID=?",id(),inbox);
        assertEquals(1,at(now,scope()).path("rawRows").path("supervisionIssues").size());assertTrue(at(later,scope()).path("rawRows").path("supervisionIssues").isEmpty());
        var sql=at(now,scope()).path("sourceEvidence").path("supervisionIssues").path("sourceQuery").path("sql").asText();assertTrue(sql.contains("boundValues"));assertTrue(sql.contains("recordedAt<=?"));assertTrue(sql.contains("r.itemId IN (?)"));assertTrue(sql.contains("r.workId IN (?)"));assertTrue(sql.contains("r.lotId IN (?)"));
    }
    @Test void sourceAndHistoryRollbackTogetherAndUncommittedStateStaysInvisible() throws Exception {
        base();installHistory();String source=schedule(now,id(),work);
        writer.setAutoCommit(false);
        try {
            try(var st=writer.prepareStatement("SELECT set_config('mulino.recovery_recorded_at',?,true)")){st.setString(1,later);st.execute();}
            try(var st=writer.prepareStatement("UPDATE mulino_runtime_RecoverySchedules SET recoveryStatus='CHECKED' WHERE ID=?")){st.setString(1,source);st.executeUpdate();}
            assertEquals(1,at(later,scope()).path("rawRows").path("supervisionIssues").size());writer.rollback();
        }finally{writer.setAutoCommit(true);}
        assertEquals(1,at(later,scope()).path("rawRows").path("supervisionIssues").size());
        try(var st=writer.createStatement();var r=st.executeQuery("SELECT count(*) FROM mulino_runtime_RecoveryObservationHistory")){r.next();assertEquals(1,r.getInt(1));}
    }

    @Test void aProvenFutureInsertDoesNotInventPastPresenceEvenWhenItsScopeIsUnknown() throws Exception {
        base();installHistory();schedule(later,id(),id());
        assertTrue(at(now,scope()).path("rawRows").path("supervisionIssues").isEmpty());
        assertThrows(UnsupportedOperationException.class,()->at(later,scope()));
    }

    @Test void recoveryHistoryComparisonUsesAbsoluteInstantsAcrossSessionTimezones() throws Exception {
        base();installHistory();
        try(var st=writer.createStatement()){st.execute("SET TIME ZONE 'Asia/Seoul'");}
        String source=schedule(now,id(),work);
        try(var st=writer.createStatement();var rs=st.executeQuery("SELECT snapshotJson FROM mulino_runtime_RecoveryObservationHistory")){
            assertTrue(rs.next());assertTrue(rs.getString(1).contains("+09:00"));
        }
        var defaultZone=java.util.TimeZone.getDefault();
        try {
            // pgjdbc sends the JVM default zone in its startup packet, overriding the options zone.
            for(String zone:List.of("UTC","America/New_York")) {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zone));
                try(var c=database.openSchemaConnection(schema);var st=c.createStatement();var rs=st.executeQuery("SHOW TIME ZONE")){assertTrue(rs.next());assertEquals(zone,rs.getString(1));}
                var rows=at(now,scope()).path("rawRows").path("supervisionIssues");
                assertEquals(1,rows.size());assertEquals(source,rows.get(0).path("id").asText());assertEquals("Stored human action",rows.get(0).path("nextAction").asText());
            }
        } finally {java.util.TimeZone.setDefault(defaultZone);}
    }

    @Test void typedSnapshotComparisonStillRejectsMissingKeysAndChangedSourceValues() throws Exception {
        // CSN-shaped tables intentionally have no product writers/triggers, so this fixture can
        // represent independently corrupted source/history pairs without disabling a product guard.
        base();String source=id();
        insert("mulino_runtime_RecoverySchedules",Json.object().put("organizationId",org).put("ID",source).put("workId",work).put("revision",0).put("recoveryStatus","HELD_MANUAL").put("nextAction","Original"));
        String snapshot;
        try(var st=writer.prepareStatement("SELECT to_jsonb(r)::text FROM mulino_runtime_RecoverySchedules r WHERE ID=?")){st.setString(1,source);try(var rs=st.executeQuery()){assertTrue(rs.next());snapshot=rs.getString(1);}}
        insert("mulino_runtime_RecoveryObservationHistory",Json.object().put("organizationId",org).put("ID",1).put("sourceTable","mulino_runtime_recoveryschedules").put("sourceId",source).put("sourceRevision",0).put("recordedAt",now).put("transactionId","1").put("operation","INSERT").put("historyComplete",true).put("scopeComplete",true).put("itemId",item).put("workId",work).put("lotId",lot).put("snapshotJson",snapshot));
        assertEquals(1,at(now,scope()).path("rawRows").path("supervisionIssues").size());
        try(var st=writer.createStatement()){st.executeUpdate("UPDATE mulino_runtime_RecoveryObservationHistory SET snapshotJson=(snapshotJson::jsonb-'ownerid')::text");}
        assertThrows(UnsupportedOperationException.class,()->at(now,scope()));
        try(var st=writer.prepareStatement("UPDATE mulino_runtime_RecoveryObservationHistory SET snapshotJson=?")){st.setString(1,snapshot);st.executeUpdate();}
        // A string that can be coerced into the source column type must still fail JSON integrity.
        try(var st=writer.createStatement()){st.executeUpdate("UPDATE mulino_runtime_RecoveryObservationHistory SET snapshotJson=jsonb_set(snapshotJson::jsonb,'{revision}',to_jsonb('0'::text))::text");}
        assertThrows(UnsupportedOperationException.class,()->at(now,scope()));
        try(var st=writer.prepareStatement("UPDATE mulino_runtime_RecoveryObservationHistory SET snapshotJson=?")){st.setString(1,snapshot);st.executeUpdate();}
        try(var st=writer.createStatement()){st.executeUpdate("UPDATE mulino_runtime_RecoverySchedules SET nextAction='Different actual value'");}
        assertThrows(UnsupportedOperationException.class,()->at(now,scope()));
    }

    @Test void unsupportedVirtualScopeFailsClosedAndNoDatabaseWritesAreAllowed() throws Exception {
        base();assertThrows(UnsupportedOperationException.class,()->capture("domain_records"));assertThrows(UnsupportedOperationException.class,()->capture("transactions"));assertThrows(UnsupportedOperationException.class,()->capture("unmappedSource"));
        var descendants=scope().put("includeDescendants",true).put("workId",id());assertThrows(UnsupportedOperationException.class,()->ScenarioJdbcObservation.capture(config,root,request(descendants,"works")));
        try(var c=database.openSchemaConnection(schema)) {
            c.setAutoCommit(false);c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            var p=new ScenarioObservationProjection(c,root,List.of(org),List.of(item),List.of(work),List.of(lot),OffsetDateTime.parse(now));
            var first=p.read("mulino_work_read_Works");try(var s=writer.createStatement()){s.execute("UPDATE mulino_work_read_Works SET status='CLOSED'");}
            // Independent uncached query after a concurrent commit still sees the same MVCC state.
            var again=p.query("mulino_work_read_Works","r.organizationId=? AND r.ID=?",List.of(org,work));assertEquals(first.get(0).path("status"),again.get(0).path("status"));
            try(var s=c.createStatement()){assertThrows(SQLException.class,()->s.executeUpdate("UPDATE mulino_work_read_Works SET status='DRAFT'"));}c.rollback();
        }
    }
    // Source-row projection checks only; these do not execute a T11 scenario.
    String storedWait(){return "{\"reason\":\"수령 근거 대기\",\"verifier\":\"1feb8408-bc65-434c-ba71-a26344b85651\",\"overdueAction\":\"ESCALATE_SUPERVISOR\"}";}
    ObjectNode waitingWork(){return row(work).put("itemId",item).put("lotId",lot).put("revision",2).put("status","WAITING").put("supervisorId","different-supervisor");}
    @Test void storedWaitFieldsProjectVerbatimFromCurrentWorkRevision() throws Exception {
        var source=waitingWork().put("waitJson",storedWait());insert("mulino_work_read_Works",source);
        var result=capture("works");var actual=result.path("rawRows").path("works").get(0);
        assertEquals("1feb8408-bc65-434c-ba71-a26344b85651",actual.path("verifierId").asText());
        assertEquals("ESCALATE_SUPERVISOR",actual.path("overdueAction").asText());
        assertEquals(source.path("waitJson"),actual.path("waitJson"));assertEquals(2,actual.path("revision").asInt());
        assertEquals("REPEATABLE_READ",result.path("snapshot").path("isolation").asText());
        try(var q=writer.createStatement();var r=q.executeQuery("SELECT waitJson FROM mulino_work_read_Works WHERE ID='"+work+"'")) {
            assertTrue(r.next());assertEquals(storedWait(),r.getString(1));assertFalse(r.next());
        }
    }
    @ParameterizedTest @ValueSource(strings={"missing","malformed","partial","otherWork","otherRevision","otherOrganization","laterTransition"})
    void missingWaitFieldsNeverBorrowFromActorSupervisorOrTransition(String variant) throws Exception {
        var source=waitingWork();
        if(variant.equals("malformed"))source.put("waitJson","not-json");
        if(variant.equals("partial"))source.put("waitJson","{\"reason\":\"stored reason\"}");
        insert("mulino_work_read_Works",source);
        var snapshot=source.deepCopy().put("waitJson",storedWait());
        var transition=Json.object().put("ID",id()).put("organizationId",org).put("workId",work)
            .put("revision",2).put("state","WAITING").put("recordedAt",now).put("snapshotJson",snapshot.toString());
        if(variant.equals("otherWork"))transition.put("workId",id());
        if(variant.equals("otherRevision"))transition.put("revision",1);
        if(variant.equals("otherOrganization"))transition.put("organizationId",id());
        if(variant.equals("laterTransition"))transition.put("recordedAt",later);
        insert("mulino_work_WorkTransitions",transition);
        var actual=capture("works").path("rawRows").path("works").get(0);
        assertFalse(actual.hasNonNull("verifierId"));assertFalse(actual.hasNonNull("overdueAction"));
        assertEquals("WAITING",actual.path("status").asText());
    }
    @ParameterizedTest @ValueSource(strings={"otherWork","otherOrganization","laterWork"})
    void waitFieldsRespectWorkOrganizationAndKnownAtScope(String variant) throws Exception {
        var source=waitingWork().put("waitJson",storedWait());
        if(variant.equals("otherWork"))source.put("ID",id());
        if(variant.equals("otherOrganization"))source.put("organizationId",id());
        if(variant.equals("laterWork"))source.put("recordedAt",later);
        insert("mulino_work_read_Works",source);
        assertTrue(capture("works").path("rawRows").path("works").isEmpty());
    }

}
