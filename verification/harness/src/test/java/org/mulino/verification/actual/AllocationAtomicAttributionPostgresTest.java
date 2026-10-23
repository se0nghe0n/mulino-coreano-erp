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
import org.mulino.verification.Json;
import org.mulino.verification.ContractValidator;

/** Real PG tuple-version proof; CSN-shaped tables isolate observer behavior from command execution. */
@EnabledIfEnvironmentVariable(named="OBSERVER_PG_TEST",matches="true")
class AllocationAtomicAttributionPostgresTest {
    final ScenarioObservationPostgresTest f=new ScenarioObservationPostgresTest();
    final String old=id(),next=id(),creation=id(),replacement=id(),release=id(),segment=id(),line=id(),customer=id();
    static String id(){return UUID.randomUUID().toString();}
    @BeforeEach void setup(TestInfo info)throws Exception {
        f.setup(info);f.base();
        f.insert("mulino_inventory_QuantitySegments",f.row(segment).put("itemId",f.item).put("lotId",f.lot).put("quantity","100").put("unit","KG"));
        f.writer.setAutoCommit(false);
        var row=allocation(old,creation,null,"EXECUTABLE",0);f.insert("mulino_inventory_SegmentAllocations",row);
        command(creation,"reserveQuantity",Json.object().put("segmentId",segment).put("salesLineId",line),0,row,f.now,f.org);
        f.writer.commit();f.writer.setAutoCommit(true);
    }
    @AfterEach void cleanup()throws Exception {if(f.writer!=null&&!f.writer.getAutoCommit()){f.writer.rollback();f.writer.setAutoCommit(true);}f.cleanup();}
    ObjectNode allocation(String id,String command,String predecessor,String state,int revision){
        var row=f.row(id).put("segmentId",segment).put("workId",f.work).put("orderLineId",line).put("customerId",customer)
            .put("startQuantity","0").put("quantity","20").put("unit","KG").put("commandId",command).put("state",state).put("revision",revision);
        if(predecessor!=null)row.put("predecessorId",predecessor);return row;
    }
    void command(String id,String cap,ObjectNode slots,int expected,ObjectNode effects,String at,String org)throws Exception {
        var result=Json.object().put("outcome","APPLIED").put("commandId",id).put("revision",effects.path("revision").asInt());result.set("effects",effects);
        var intent=Json.object().put("capabilityId",cap).put("expectedRevision",expected);intent.set("slots",slots);
        var row=f.row(id);row.remove("recordedAt");row.put("organizationId",org).put("createdAt",at).put("state","IN_PROGRESS").put("capabilityId",cap)
            .put("commandIdempotencyKey",id()).put("canonicalIntentJson",intent.toString());f.insert("mulino_commands_CommandRecords",row);
        try(var s=f.writer.prepareStatement("UPDATE mulino_commands_CommandRecords SET state='COMMITTED',completedAt=?,resultJson=? WHERE ID=?")){
            s.setObject(1,OffsetDateTime.parse(at));s.setString(2,result.toString());s.setString(3,id);assertEquals(1,s.executeUpdate());
        }
    }
    void replace(String variant)throws Exception {
        f.writer.setAutoCommit(false);
        try(var s=f.writer.prepareStatement("UPDATE mulino_inventory_SegmentAllocations SET state='REPLACED',revision=1 WHERE ID=?")){s.setString(1,old);assertEquals(1,s.executeUpdate());}
        var successor=allocation(next,replacement,old,"EXECUTABLE",0);f.insert("mulino_inventory_SegmentAllocations",successor);
        var effects=successor.deepCopy();effects.set("fulfillmentBasis",Json.object().put("replacedAllocationId",old));
        if(variant.equals("wrongEffect"))effects.put("ID",id());
        if(variant.equals("wrongRange"))effects.put("quantity","19");
        if(variant.equals("wrongPredecessor"))effects.put("predecessorId",id());
        if(variant.equals("wrongOrganization"))effects.put("organizationId",id());
        var slots=Json.object().put("allocationId",variant.equals("wrongTarget")?id():old).put("segmentId",segment);
        if(!variant.equals("missing"))command(replacement,variant.equals("wrongCapability")?"pickQuantity":"replaceAllocation",slots,
            variant.equals("wrongRevision")?3:0,effects,variant.equals("future")?f.later:f.now,variant.equals("foreign")?id():f.org);
        if(variant.equals("ambiguous")||variant.equals("ambiguousFuture"))command(id(),"reserveQuantity",Json.object(),0,successor,variant.equals("ambiguousFuture")?f.later:f.now,f.org);
        if(variant.equals("notCommitted"))try(var s=f.writer.createStatement()){s.executeUpdate("UPDATE mulino_commands_CommandRecords SET state='IN_PROGRESS' WHERE ID='"+replacement+"'");}
        f.writer.commit();f.writer.setAutoCommit(true);
    }
    void release(boolean commit)throws Exception {
        f.writer.setAutoCommit(false);
        try(var s=f.writer.prepareStatement("UPDATE mulino_inventory_SegmentAllocations SET state='RELEASED',revision=1 WHERE ID=?")){s.setString(1,next);assertEquals(1,s.executeUpdate());}
        var effects=allocation(next,replacement,old,"RELEASED",1);effects.set("fulfillmentBasis",Json.object().put("releasedAllocationId",next));
        command(release,"releaseAllocation",Json.object().put("allocationId",next),0,effects,f.now,f.org);
        if(commit){f.writer.commit();f.writer.setAutoCommit(true);}
    }
    JsonNode capture()throws Exception {return f.capture("allocations");}
    JsonNode row(JsonNode result,String id){for(var row:result.path("rawRows").path("allocations"))if(id.equals(row.path("id").asText()))return row;throw new AssertionError("Missing allocation "+id);}
    String xmin(String table,String id)throws Exception {try(var s=f.writer.prepareStatement("SELECT xmin::text FROM "+table+" WHERE ID=?")){s.setString(1,id);try(var r=s.executeQuery()){assertTrue(r.next());return r.getString(1);}}}
    void unsupported(JsonNode row,String reason){assertFalse(row.has("lastCommandId"));assertFalse(row.has("transactionId"));assertEquals(reason,row.at("/atomicAttribution/gap").asText());}

    @Test void initialReservationUsesItsActualCreationTransactionWithoutNeedingAPredecessor()throws Exception {
        var observed=row(capture(),old);assertEquals(creation,observed.path("lastCommandId").asText());
        assertEquals(creation,observed.path("commandId").asText());assertEquals(xmin("mulino_inventory_SegmentAllocations",old),observed.path("transactionId").asText());
        assertEquals(xmin("mulino_commands_CommandRecords",creation),observed.path("transactionId").asText());
    }
    @Test void replacementSharesActualTupleTransactionAndUniqueCommittedCommandWithoutChangingCreation()throws Exception {
        replace("valid");var captured=capture();var a=row(captured,old);var b=row(captured,next);
        assertEquals(replacement,a.path("lastCommandId").asText());assertEquals(replacement,b.path("lastCommandId").asText());
        assertEquals(xmin("mulino_inventory_SegmentAllocations",old),a.path("transactionId").asText());assertEquals(a.path("transactionId"),b.path("transactionId"));
        assertEquals(xmin("mulino_commands_CommandRecords",replacement),b.path("transactionId").asText());
        assertEquals(creation,a.path("commandId").asText());assertEquals(replacement,b.path("commandId").asText());assertFalse(a.at("/atomicAttribution/historyComplete").asBoolean());
        String queries=captured.at("/sourceEvidence/allocations/sourceQuery").toString();assertTrue(queries.contains("r.xmin=?::xid"));assertTrue(queries.contains(f.org));assertTrue(queries.contains(a.path("transactionId").asText()));
        assertEquals("REPEATABLE_READ",captured.at("/snapshot/isolation").asText());
        String ref=".local/observer-atomic-attribution/schema-observation.json";((ObjectNode)captured.path("snapshot")).put("artifactRef",ref);captured.path("sourceEvidence").forEach(e->((ObjectNode)e).put("artifactRef",ref));Json.write(f.root.resolve(ref),captured);
        new ContractValidator(f.root).schema("contracts/acceptance-observation.schema.json",captured);
    }
    @Test void laterReleaseChangesOnlyTheActuallyUpdatedTupleAndRetainsCreationCommand()throws Exception {
        replace("valid");String replaced=xmin("mulino_inventory_SegmentAllocations",old);release(true);var result=capture();
        assertEquals(replacement,row(result,old).path("lastCommandId").asText());assertEquals(replaced,row(result,old).path("transactionId").asText());
        assertEquals(release,row(result,next).path("lastCommandId").asText());assertEquals(replacement,row(result,next).path("commandId").asText());
        assertEquals(xmin("mulino_commands_CommandRecords",release),row(result,next).path("transactionId").asText());assertNotEquals(replaced,row(result,next).path("transactionId").asText());
    }
    @ParameterizedTest @ValueSource(strings={"foreign","missing","ambiguous","ambiguousFuture","notCommitted","future","wrongEffect","wrongRange","wrongPredecessor","wrongOrganization","wrongTarget","wrongRevision","wrongCapability"})
    void incompleteAmbiguousUnrelatedAndTimeIneligibleTransactionsNeverAcquireAWriter(String variant)throws Exception {
        replace(variant);var result=capture();
        String reason=switch(variant){case "foreign","missing"->"NO_COMMAND_IN_TUPLE_TRANSACTION";case "ambiguous","ambiguousFuture"->"AMBIGUOUS_COMMAND_TRANSACTION";case "notCommitted"->"COMMAND_NOT_COMMITTED";case "future"->"COMMAND_OUTSIDE_TIME_BOUNDARY";default->"COMMAND_EFFECT_DOES_NOT_PROVE_ALLOCATION_WRITE";};
        unsupported(row(result,old),reason);if(!variant.equals("wrongTarget")&&!variant.equals("wrongRevision"))unsupported(row(result,next),reason);
    }
    @Test void laterNonCommandUpdateAndSeparatelyRewrittenCommandCannotBorrowOldAttribution()throws Exception {
        replace("valid");try(var s=f.writer.createStatement()){s.executeUpdate("UPDATE mulino_inventory_SegmentAllocations SET revision=2 WHERE ID='"+old+"'");}
        unsupported(row(capture(),old),"NO_COMMAND_IN_TUPLE_TRANSACTION");
        try(var s=f.writer.createStatement()){s.executeUpdate("UPDATE mulino_commands_CommandRecords SET resultJson=resultJson WHERE ID='"+replacement+"'");}
        unsupported(row(capture(),next),"NO_COMMAND_IN_TUPLE_TRANSACTION");
    }
    JsonNode inSnapshot(Connection c)throws Exception {
        var p=new ScenarioObservationProjection(c,f.root,List.of(f.org),List.of(f.item),List.of(f.work),List.of(f.lot),OffsetDateTime.parse(f.now),OffsetDateTime.parse(f.now));
        var rows=p.read("mulino_inventory_SegmentAllocations");p.enrich("mulino_inventory_SegmentAllocations",rows);var raw=Json.object();raw.set("allocations",rows);var result=Json.object();result.set("rawRows",raw);return result;
    }
    @Test void uncommittedReleaseIsInvisibleAndRepeatableReadKeepsItsOriginalPhysicalVersionAfterCommit()throws Exception {
        replace("valid");
        try(var reader=DriverManager.getConnection(f.config.jdbcUrl(),f.config.username(),f.config.password())){
            reader.setAutoCommit(false);reader.setReadOnly(true);reader.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            assertTrue(reader.isReadOnly());assertEquals(Connection.TRANSACTION_REPEATABLE_READ,reader.getTransactionIsolation());
            var before=row(inSnapshot(reader),next);release(false);
            assertEquals(before,row(inSnapshot(reader),next));f.writer.commit();f.writer.setAutoCommit(true);
            assertEquals(before,row(inSnapshot(reader),next));assertEquals(release,row(capture(),next).path("lastCommandId").asText());reader.rollback();
        }
    }
    @Test void earlierAsOfDoesNotPresentCurrentMutableTupleAttributionAsHistory()throws Exception {
        replace("valid");var request=f.request(f.scope(),"allocations").put("asOf",f.before);
        var result=ScenarioJdbcObservation.capture(f.config,f.root,request);unsupported(row(result,old),"COMMAND_OUTSIDE_TIME_BOUNDARY");unsupported(row(result,next),"COMMAND_OUTSIDE_TIME_BOUNDARY");
    }
    @Test void actualTerminalCommandTriggerPreservesTheImmutableProofAndObserverCannotWrite()throws Exception {
        String migration=java.nio.file.Files.readString(f.root.resolve("database/migrations/V8__commands.sql"));
        String terminal=migration.substring(migration.indexOf("CREATE FUNCTION mulino_commands_terminal_record()"));
        try(var s=f.writer.createStatement()){s.execute(terminal);}
        replace("valid");var before=capture();
        for(String sql:List.of("UPDATE mulino_commands_CommandRecords SET resultJson='{}' WHERE ID=?","DELETE FROM mulino_commands_CommandRecords WHERE ID=?")){
            try(var s=f.writer.prepareStatement(sql)){s.setString(1,replacement);assertThrows(SQLException.class,s::executeUpdate);}
        }
        assertEquals(row(before,old).path("lastCommandId"),row(capture(),old).path("lastCommandId"));
        try(var reader=DriverManager.getConnection(f.config.jdbcUrl(),f.config.username(),f.config.password())){
            reader.setAutoCommit(false);reader.setReadOnly(true);reader.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);inSnapshot(reader);
            try(var s=reader.createStatement()){assertEquals("25006",assertThrows(SQLException.class,()->s.executeUpdate("UPDATE mulino_inventory_SegmentAllocations SET state='RELEASED'" )).getSQLState());}finally{reader.rollback();}
        }
    }
    @Test void physicalEpochGuardRefusesFrozenReservedWrappedOldOrMalformedIdentity() {
        assertTrue(ScenarioObservationProjection.unwrappedTupleXid("123","124"));
        for(String[] values:List.of(new String[]{"2","124"},new String[]{"123","4294967420"},new String[]{"123","1000000000"},new String[]{"124","124"},new String[]{"-1","124"},new String[]{"bad","124"}))assertFalse(ScenarioObservationProjection.unwrappedTupleXid(values[0],values[1]));
    }
}
