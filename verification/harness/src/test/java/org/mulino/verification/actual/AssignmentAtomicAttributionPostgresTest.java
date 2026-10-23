package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mulino.verification.AssertionEngine;
import org.mulino.verification.Json;

/** Actual PG transactions with protocol-shaped rows; not an HTTP/scenario execution. */
@EnabledIfEnvironmentVariable(named="OBSERVER_PG_TEST",matches="true")
class AssignmentAtomicAttributionPostgresTest {
    final ScenarioObservationPostgresTest f=new ScenarioObservationPostgresTest();
    final String sourceWork=id(),root=id(),scope=id(),predecessor=id(),successor=id(),handover=id(),command=id(),audit=id(),owner=id(),recipient=id(),supervisor=id();
    ObjectNode proposal,accepted,intent,result,cmd,auditRow;String xid,hash;
    static String id(){return UUID.randomUUID().toString();}
    @BeforeEach void setup(TestInfo info)throws Exception {
        f.setup(info);f.base();
        change("mulino_work_read_Works",f.work,"ownerId",recipient);change("mulino_work_read_Works",f.work,"supervisorId",supervisor);
        f.insert("mulino_work_read_Works",f.row(sourceWork).put("itemId",f.item).put("lotId",f.lot).put("ownerId",owner).put("supervisorId",supervisor).put("status","ACTIVE"));
        String domainScope=Json.object().put("itemId",f.item).toString();
        f.insert("mulino_responsibility_Roots",f.row(root).put("revision",0).put("kind","QUANTITY_SHORTFALL").put("sourceKind","DOCUMENT").put("sourceId",id()).put("sourceVersion","1").put("scopeJson",domainScope).put("quantity","10").put("unit","BOX"));
        f.insert("mulino_responsibility_Scopes",scopeRow(scope,null,"0","10",true,domainScope));
        f.insert("mulino_work_read_ObligationReferences",assignment(predecessor,sourceWork,owner,scope,"10",null).put("createdAt",f.before).put("recordedAt",f.before));
        proposal=f.row(handover).put("revision",0).put("createdAt",f.before).put("recordedAt",f.before).put("kind","TRANSFER").put("workId",sourceWork).put("assignmentId",predecessor)
            .put("targetWorkId",f.work).put("previousOwnerId",owner).put("recipientId",recipient).put("status","PROPOSED").put("expiresAt",f.later)
            .put("nextAction","남은 책임을 인수한다").put("nextCheckAt",f.later).putNull("quantity").putNull("reason");
        f.insert("mulino_responsibility_Handovers",proposal);
    }
    @AfterEach void cleanup()throws Exception {if(f.writer!=null&&!f.writer.getAutoCommit()){f.writer.rollback();f.writer.setAutoCommit(true);}f.cleanup();}
    ObjectNode scopeRow(String scopeId,String parent,String start,String quantity,boolean leaf,String json) {
        var row=f.row(scopeId).put("revision",0).put("rootId",root).put("startQuantity",start).put("quantity",quantity).put("unit","BOX").put("leaf",leaf).put("scopeJson",json);
        if(parent!=null)row.put("parentScopeId",parent);return row;
    }
    ObjectNode assignment(String assignment,String work,String actor,String scoped,String quantity,String previous) {
        var row=f.row(assignment).put("createdAt",f.now).put("recordedAt",f.now).put("revision",0).put("effectiveAt",f.now).put("workId",work).put("kind","QUANTITY_SHORTFALL").put("status","OPEN").put("valid",true)
            .put("rootId",root).put("scopeId",scoped).put("quantity",quantity).put("unit","BOX").put("ownerId",actor).put("supervisorId",supervisor)
            .put("nextAction","남은 책임을 인수한다").put("nextCheckAt",f.later).put("scopeJson",Json.object().put("itemId",f.item).toString());
        if(previous!=null)row.put("predecessorId",previous);return row;
    }
    void change(String table,String record,String field,Object value)throws Exception {
        try(var st=f.writer.prepareStatement("UPDATE "+table+" SET "+field+"=? WHERE ID=?")) {
            st.setObject(1,value!=null&&field.endsWith("At")?OffsetDateTime.parse(value.toString()):value);st.setString(2,record);assertEquals(1,st.executeUpdate());
        }
    }
    void accept(String variant,boolean commit)throws Exception {
        f.writer.setAutoCommit(false);
        try(var st=f.writer.createStatement();var r=st.executeQuery("SELECT pg_current_xact_id()::xid::text")){assertTrue(r.next());xid=r.getString(1);}
        change("mulino_work_read_ObligationReferences",predecessor,"status","TRANSFERRED");change("mulino_work_read_ObligationReferences",predecessor,"valid",false);change("mulino_work_read_ObligationReferences",predecessor,"revision",1);
        change("mulino_responsibility_Handovers",handover,"status","ACCEPTED");change("mulino_responsibility_Handovers",handover,"revision",1);
        accepted=proposal.deepCopy().put("status","ACCEPTED").put("revision",1);
        String afterScope=scope,quantity="10";
        if(variant.equals("partial")) {
            quantity="4";afterScope=id();accepted.put("quantity","4");change("mulino_responsibility_Handovers",handover,"quantity",new java.math.BigDecimal("4"));
            change("mulino_responsibility_Scopes",scope,"leaf",false);
            f.insert("mulino_responsibility_Scopes",scopeRow(afterScope,scope,"6","4",true,Json.object().put("itemId",f.item).toString()));
            String retained=id();f.insert("mulino_responsibility_Scopes",scopeRow(retained,scope,"0","6",true,Json.object().put("itemId",f.item).toString()));
            f.insert("mulino_work_read_ObligationReferences",assignment(id(),sourceWork,owner,retained,"6",predecessor));
        }
        f.insert("mulino_work_read_ObligationReferences",assignment(successor,f.work,recipient,afterScope,quantity,predecessor));
        intent=Json.object().put("intentKind","COMMAND").put("capabilityId","acceptHandover").put("definitionVersion","fixture-v1").put("capabilityVersion","1.0.0")
            .put("commandIdempotencyKey","T11-cancel-after-shipment-transfer").put("expectedRevision",0);
        intent.putObject("slots").put("handoverId",handover);intent.putArray("subjectRefs").addObject().put("type","TradeItem").put("id",f.item);intent.putObject("provenance").put("handoverId","CONTEXT");
        var canonical=intent.deepCopy();canonical.remove("commandIdempotencyKey");hash=Json.sha256Text(sorted(canonical).toString());
        result=Json.object().put("outcome","APPLIED").put("commandId",command).put("transactionId",xid).put("canonicalIntentHash",hash).put("proposalRevision",1);
        result.putObject("effects").set("responsibility",accepted);
        var scopes=Json.object();scopes.putArray("ITEM").add(f.item);scopes.putArray("WORK").add(f.work);
        cmd=Json.object().put("ID",command).put("organizationId",f.org).put("actorId",recipient).put("stableRequestOwner",recipient).put("createdAt",f.now).put("completedAt",f.now)
            .put("capabilityId","acceptHandover").put("definitionVersion","fixture-v1").put("capabilityVersion","1.0.0").put("commandIdempotencyKey","T11-cancel-after-shipment-transfer")
            .put("state","COMMITTED").put("effectClass","RESPONSIBILITY").put("canonicalHash",hash).put("canonicalIntentJson",intent.toString()).put("authorizationScopeJson",scopes.toString()).put("resultJson",result.toString());
        f.insert("mulino_commands_CommandRecords",cmd);
        var facts=Json.object().put("targetId",handover).put("beforeRevision",0);facts.set("scope",scopes);
        auditRow=Json.object().put("ID",audit).put("organizationId",f.org).put("actorId",recipient).put("commandId",command).put("capabilityId","acceptHandover")
            .put("canonicalHash",hash).put("outcome","APPLIED").put("createdAt",f.now).put("effectRefs",Json.object().put("commandId",command).toString()).put("auditFactsJson",facts.toString());
        f.insert("mulino_commands_CommandAudits",auditRow);
        corrupt(variant);
        if(commit){f.writer.commit();f.writer.setAutoCommit(true);}
    }
    JsonNode sorted(JsonNode node){if(node.isArray()){var out=Json.array();for(var v:node)out.add(sorted(v));return out;}if(!node.isObject())return node;var out=Json.object();var keys=new TreeSet<String>();node.fieldNames().forEachRemaining(keys::add);for(String key:keys)out.set(key,sorted(node.path(key)));return out;}
    void corrupt(String variant)throws Exception {
        switch(variant) {
            case "foreignCommand" -> change("mulino_commands_CommandRecords",command,"organizationId",id());
            case "foreignAudit" -> change("mulino_commands_CommandAudits",audit,"organizationId",id());
            case "foreignProposal" -> change("mulino_responsibility_Handovers",handover,"organizationId",id());
            case "foreignRoot" -> change("mulino_responsibility_Roots",root,"organizationId",id());
            case "foreignTarget" -> change("mulino_work_read_Works",f.work,"organizationId",id());
            case "wrongRoot" -> change("mulino_work_read_ObligationReferences",successor,"rootId",id());
            case "danglingPredecessor" -> change("mulino_work_read_ObligationReferences",successor,"predecessorId",id());
            case "wrongTarget" -> change("mulino_responsibility_Handovers",handover,"targetWorkId",id());
            case "wrongOwner" -> change("mulino_work_read_ObligationReferences",successor,"ownerId",id());
            case "wrongActor" -> change("mulino_commands_CommandRecords",command,"actorId",id());
            case "wrongScope" -> change("mulino_work_read_ObligationReferences",successor,"scopeId",id());
            case "wrongRange" -> change("mulino_work_read_ObligationReferences",successor,"quantity",new java.math.BigDecimal("9"));
            case "wrongRootQuantity" -> change("mulino_responsibility_Roots",root,"quantity",new java.math.BigDecimal("9"));
            case "wrongSourceOwner" -> change("mulino_work_read_Works",sourceWork,"ownerId",id());
            case "sourceItem" -> change("mulino_work_read_Works",sourceWork,"itemId",id());
            case "wrongTargetSupervisor" -> change("mulino_work_read_Works",f.work,"supervisorId",id());
            case "wrongScopeRoot" -> change("mulino_responsibility_Scopes",scope,"rootId",id());
            case "futureScope" -> change("mulino_responsibility_Scopes",scope,"createdAt",f.later);
            case "wrongAuditCommand" -> change("mulino_commands_CommandAudits",audit,"commandId",id());
            case "wrongHash" -> change("mulino_commands_CommandRecords",command,"canonicalHash","2".repeat(64));
            case "wrongAuditHash" -> change("mulino_commands_CommandAudits",audit,"canonicalHash","2".repeat(64));
            case "wrongIntentHash" -> {var changed=intent.deepCopy();changed.put("definitionVersion","different");change("mulino_commands_CommandRecords",command,"canonicalIntentJson",changed.toString());}
            case "futureCommandCreated","futureCommandCompleted" -> change("mulino_commands_CommandRecords",command,variant.equals("futureCommandCreated")?"createdAt":"completedAt",f.later);
            case "futureAudit" -> change("mulino_commands_CommandAudits",audit,"createdAt",f.later);
            case "futureRoot" -> change("mulino_responsibility_Roots",root,"createdAt",f.later);
            case "futureTarget" -> change("mulino_work_read_Works",f.work,"createdAt",f.later);
            case "futurePredecessor" -> change("mulino_work_read_ObligationReferences",predecessor,"recordedAt",f.later);
            case "futureSuccessor" -> change("mulino_work_read_ObligationReferences",successor,"createdAt",f.later);
            case "differentRecordedAt" -> change("mulino_work_read_ObligationReferences",successor,"recordedAt",f.before);
            case "notFresh" -> change("mulino_work_read_ObligationReferences",successor,"revision",1);
            case "resolvedCurrent" -> change("mulino_work_read_ObligationReferences",successor,"status","RESOLVED");
            case "invalidAssignment" -> change("mulino_work_read_ObligationReferences",successor,"valid",false);
            case "notAccepted" -> change("mulino_responsibility_Handovers",handover,"status","REJECTED");
            case "ownerHandover" -> change("mulino_responsibility_Handovers",handover,"kind","OWNER");
            case "notCommitted" -> change("mulino_commands_CommandRecords",command,"state","IN_PROGRESS");
            case "wrongCapability" -> change("mulino_commands_CommandRecords",command,"capabilityId","transferObligation");
            case "wrongAuditOutcome" -> change("mulino_commands_CommandAudits",audit,"outcome","REJECTED");
            case "wrongAuditFacts" -> change("mulino_commands_CommandAudits",audit,"auditFactsJson","{}");
            case "wrongAuthorizationScope" -> change("mulino_commands_CommandRecords",command,"authorizationScopeJson","{}");
            case "wrongEffectRefs" -> change("mulino_commands_CommandAudits",audit,"effectRefs","{}");
            case "duplicateCommand","duplicateFutureCommand" -> {var other=cmd.deepCopy().put("ID",id()).put("commandIdempotencyKey",id());if(variant.equals("duplicateFutureCommand"))other.put("createdAt",f.later);f.insert("mulino_commands_CommandRecords",other);}
            case "duplicateAudit" -> f.insert("mulino_commands_CommandAudits",auditRow.deepCopy().put("ID",id()));
            case "duplicateSuccessor" -> f.insert("mulino_work_read_ObligationReferences",assignment(id(),f.work,recipient,scope,"10",predecessor));
            case "resultXid","missingXid","resultCommand","resultHash","resultProposal","resultAssignment","resultTarget" -> {
                var altered=result.deepCopy();
                switch(variant) {
                    case "resultXid" -> altered.put("transactionId","2");case "missingXid" -> altered.remove("transactionId");case "resultCommand" -> altered.put("commandId",id());case "resultHash" -> altered.put("canonicalIntentHash","2".repeat(64));
                    default -> ((ObjectNode)altered.at("/effects/responsibility")).put(variant.equals("resultProposal")?"ID":variant.equals("resultAssignment")?"assignmentId":"targetWorkId",id());
                }
                change("mulino_commands_CommandRecords",command,"resultJson",altered.toString());
            }
        }
    }
    JsonNode capture()throws Exception {
        var request=f.request(Json.object().put("organizationId",f.org),"audit","assignments");var captured=ScenarioJdbcObservation.capture(f.config,f.root,request);
        String ref=".local/t11-assignment-atomic/captures/"+f.testName+"-"+(++f.captures)+".json";
        ScenarioJdbcObservation.bindArtifact(captured,request.path("sources"),ref);Json.write(f.root.resolve(ref),captured);f.validate(request,captured);return captured;
    }
    JsonNode successor(JsonNode capture){for(var row:capture.at("/rawRows/assignments"))if(successor.equals(row.path("id").asText()))return row;throw new AssertionError("successor hidden");}
    JsonNode audit(JsonNode capture){for(var row:capture.at("/rawRows/audit"))if(audit.equals(row.path("id").asText()))return row;throw new AssertionError("audit hidden");}
    JsonNode oracle()throws Exception {return find(Json.read(f.root.resolve("verification/cases/T11/case.json")),"acceptance-and-assignment-one-transaction");}
    JsonNode find(JsonNode node,String key){if(node.isObject()&&key.equals(node.path("id").asText()))return node;if(node.isContainerNode())for(var child:node){var found=find(child,key);if(found!=null)return found;}return null;}
    void originalOracle(JsonNode captured)throws Exception {
        var envelope=Json.object().put("driverStatus","EXECUTED");envelope.set("data",captured);envelope.putObject("provenance").put("scopeComplete",true);
        new AssertionEngine().check(oracle(),Map.of("after-db",envelope),Json.object().put("TARGET",f.work).put("ORG",f.org).put("P",f.item));
    }
    @ParameterizedTest @ValueSource(strings={"full","partial"})
    void originalAtomicOracleUsesActualCommandAuditAndFreshTargetTupleTransactions(String mode)throws Exception {
        accept(mode,true);var captured=capture();originalOracle(captured);
        assertEquals(xid,audit(captured).path("transactionId").asText());assertEquals(xid,successor(captured).path("createdTransactionId").asText());
        assertEquals(result.path("transactionId").asText(),xid);assertEquals("PROVEN_NEW_ASSIGNMENT_CREATION",successor(captured).at("/atomicAttribution/status").asText());
        assertEquals(command,successor(captured).at("/atomicAttribution/commandRow/id").asText());assertEquals(handover,successor(captured).at("/atomicAttribution/handoverRow/id").asText());
        assertEquals("REPEATABLE_READ",captured.at("/snapshot/isolation").asText());String sql=captured.at("/sourceEvidence/assignments/sourceQuery/sql").asText();
        for(String part:List.of("r.xmin::text","pg_snapshot_xmax","r.xmin=?::xid","boundValues",f.org,command,successor))assertTrue(sql.contains(part),part);
        assertTrue(successor(captured).path("current").asBoolean());assertEquals("OPEN",successor(captured).path("status").asText());
    }
    @ParameterizedTest @ValueSource(strings={"foreignCommand","foreignAudit","foreignProposal","foreignRoot","foreignTarget","wrongRoot","danglingPredecessor","wrongTarget","wrongOwner","wrongActor","wrongScope","wrongRange","wrongRootQuantity","wrongSourceOwner","sourceItem","wrongTargetSupervisor","wrongScopeRoot","futureScope","wrongAuditCommand","wrongHash","wrongAuditHash","wrongIntentHash","futureCommandCreated","futureCommandCompleted","futureAudit","futureRoot","futureTarget","futurePredecessor","futureSuccessor","differentRecordedAt","notFresh","resolvedCurrent","invalidAssignment","notAccepted","ownerHandover","notCommitted","wrongCapability","wrongAuditOutcome","wrongAuditFacts","wrongAuthorizationScope","wrongEffectRefs","duplicateCommand","duplicateFutureCommand","duplicateAudit","duplicateSuccessor","resultXid","missingXid","resultCommand","resultHash","resultProposal","resultAssignment","resultTarget"})
    void incompleteOrUnrelatedProofLeavesOriginalRowsWithoutManufacturedTransactionIds(String variant)throws Exception {
        accept(variant,true);var captured=capture();
        assertFalse(successor(captured).hasNonNull("createdTransactionId"));assertEquals("UNSUPPORTED",successor(captured).at("/atomicAttribution/status").asText());
        assertThrows(AssertionError.class,()->originalOracle(captured));
        if(variant.equals("resolvedCurrent")){assertTrue(successor(captured).path("current").asBoolean());assertEquals("RESOLVED",successor(captured).path("status").asText());}
    }
    @ParameterizedTest @ValueSource(strings={"mulino_commands_CommandRecords","mulino_commands_CommandAudits","mulino_responsibility_Handovers","mulino_work_read_ObligationReferences"})
    void aLaterNoOpUpdateCannotBeRenamedAsCreation(String table)throws Exception {
        accept("full",true);String record=table.endsWith("CommandRecords")?command:table.endsWith("CommandAudits")?audit:table.endsWith("Handovers")?handover:successor;
        try(var st=f.writer.prepareStatement("UPDATE "+table+" SET ID=ID WHERE ID=?")){st.setString(1,record);assertEquals(1,st.executeUpdate());}
        var captured=capture();assertFalse(successor(captured).hasNonNull("createdTransactionId"));assertThrows(AssertionError.class,()->originalOracle(captured));
    }
    @Test void uncommittedAndRolledBackAcceptanceHasNoPublishedSuccessorOrAcceptAudit()throws Exception {
        accept("full",false);assertFalse(capture().at("/rawRows/assignments").findValuesAsText("id").contains(successor));assertTrue(capture().at("/rawRows/audit").isEmpty());
        f.writer.rollback();f.writer.setAutoCommit(true);assertFalse(capture().at("/rawRows/assignments").findValuesAsText("id").contains(successor));
    }
    @Test void repeatableReadRetainsTheSamePhysicalProofAndRefusesWritesAfterConcurrentUpdate()throws Exception {
        accept("full",true);
        try(var reader=DriverManager.getConnection(f.config.jdbcUrl(),f.config.username(),f.config.password())) {
            reader.setAutoCommit(false);reader.setReadOnly(true);reader.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);ObserverSnapshot.token(reader);
            change("mulino_work_read_ObligationReferences",successor,"revision",1);
            var p=new ScenarioObservationProjection(reader,f.root,List.of(f.org),List.of(f.item),List.of(f.work),List.of(f.lot),OffsetDateTime.parse(f.now),OffsetDateTime.parse(f.now));
            var rows=p.read("mulino_work_read_ObligationReferences");p.enrich("mulino_work_read_ObligationReferences",rows);assertEquals(xid,rows.get(0).path("createdTransactionId").asText());
            try(var st=reader.createStatement()){assertEquals("25006",assertThrows(SQLException.class,()->st.executeUpdate("DELETE FROM mulino_work_read_ObligationReferences")).getSQLState());}reader.rollback();
        }
        assertFalse(successor(capture()).hasNonNull("createdTransactionId"));
    }
    @Test void xidGuardDoesNotGuessFrozenOrWrappedTransactionIdentity() {
        for(String[] pair:List.of(new String[]{"2","1022"},new String[]{"1020","1020"},new String[]{"1020","1000000000"},new String[]{"1020","4294968318"},new String[]{"bad","1022"}))assertFalse(ScenarioObservationProjection.unwrappedTupleXid(pair[0],pair[1]));
    }
    @Test void realProductTerminalAndAuditGuardsPreserveTheProvenOriginalRows()throws Exception {
        String migration=Files.readString(f.root.resolve("database/migrations/V8__commands.sql"));
        String auditGuard=migration.substring(migration.indexOf("CREATE FUNCTION mulino_commands_immutable_audit()"),migration.indexOf("CREATE TABLE mulino_commands_Approvals"));
        String terminal=migration.substring(migration.indexOf("CREATE FUNCTION mulino_commands_terminal_record()"));
        try(var st=f.writer.createStatement()){st.execute(auditGuard);st.execute(terminal);}
        accept("full",true);originalOracle(capture());
        for(String table:List.of("mulino_commands_CommandRecords","mulino_commands_CommandAudits")) {
            try(var st=f.writer.prepareStatement("UPDATE "+table+" SET ID=ID WHERE ID=?")){st.setString(1,table.endsWith("CommandRecords")?command:audit);assertThrows(SQLException.class,st::executeUpdate);}
        }
        originalOracle(capture());
    }
    @Test void earlierAsOfNeverBackdatesCurrentCreationAttribution()throws Exception {
        accept("full",true);var request=f.request(Json.object().put("organizationId",f.org),"audit","assignments").put("asOf",f.before);
        var observed=ScenarioJdbcObservation.capture(f.config,f.root,request);assertFalse(successor(observed).hasNonNull("createdTransactionId"));assertFalse(audit(observed).hasNonNull("transactionId"));
    }
    @Test void actualAttempt7CanonicalBytesAndPhysicalSourceEvidenceRemainDistinctFromNewPgTransactions()throws Exception {
        String source=System.getenv("T11_ATOMIC_SOURCE");Assumptions.assumeTrue(source!=null,"Optional independent attempt7 source artifact required");
        var path=Path.of(source);assertEquals("109dbd3105e4cf4e90892e427232829fda048563ef3ae0cf6df036415c5c7958",Json.sha256(path));
        var original=Json.read(path);assertEquals("1020",original.at("/acceptResponse/transactionId").asText());
        for(var statement:original.path("statements"))for(var row:statement.path("rows"))assertEquals("1020",row.path("tupleWriteTransactionId").asText());
        var raw=Json.parse(original.at("/statements/0/rows/0/rawRowBytes").asText());
        assertEquals(raw.path("canonicalhash").asText(),ScenarioObservationProjection.canonicalCommandHash(Json.parse(raw.path("canonicalintentjson").asText())));
        assertEquals(original.path("acceptResponse"),Json.parse(raw.path("resultjson").asText()));
        // No original xmin is inserted into this database or used to fill an observer field.
    }
}
