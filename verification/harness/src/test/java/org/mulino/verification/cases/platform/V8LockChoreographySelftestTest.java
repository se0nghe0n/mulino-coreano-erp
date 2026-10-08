package org.mulino.verification.cases.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mulino.verification.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** SELFTEST of authored choreography and mutation rejection, never a product/DB execution. */
public final class V8LockChoreographySelftestTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private static final String ARTIFACT="verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/canned-observations.json";
    private static final String SUBCASE="real-lock-two-transactions";
    private static final String HOLDER="captured-db-xid-810",WAITER="captured-db-xid-811";
    private Path casePath(boolean serialized) throws Exception {return casePath(SUBCASE,serialized);}
    private Path casePath(String subcase,boolean serialized) throws Exception {
        ObjectNode file=(ObjectNode)Json.read(root.resolve("verification/cases/V8/case.json"));
        JsonNode selected=null;for(JsonNode sub:file.path("subcases")) if(subcase.equals(sub.path("id").asText())) selected=sub.deepCopy();
        assertNotNull(selected);file.set("subcases",Json.array().add(selected));
        if(!subcase.equals(SUBCASE)) {
            ArrayNode actions=Json.array(),assertions=Json.array();
            for(JsonNode a:selected.path("actions")) if(a.path("id").asText().equals("setup") || a.path("id").asText().startsWith("lock-")) actions.add(a);
            for(JsonNode a:selected.path("assertions")) if(a.path("id").asText().startsWith("lock-")) assertions.add(a);
            ((ObjectNode)selected).set("actions",actions);((ObjectNode)selected).set("assertions",assertions);
        }
        if(serialized) {
            ArrayNode actions=(ArrayNode)selected.path("actions");Map<String,JsonNode> byId=new HashMap<>();actions.forEach(a->byId.put(a.path("id").asText(),a));
            actions.removeAll();for(String id:List.of("setup","before","reserve40","reserve40-reached","reserve30","reserve30-reached","resume40","terminal40","resume30","waiter-before-release","terminal30","after")) actions.add(byId.get(id));
        }
        Files.createDirectories(root.resolve("verification/harness/target"));Path p=Files.createTempFile(root.resolve("verification/harness/target"),"V8-lock-SELFTEST-",".json");Json.write(p,file);return p;
    }
    private CaseRunner runner(Path p,CapturedLockDriver d) throws Exception {return CaseRunner.harnessSelftest(new ContractValidator(root),d,new AgentRunner.Scripted(),p,SUBCASE);}
    private static class CapturedLockDriver implements AcceptanceDriver {
        final List<String> events=new ArrayList<>();boolean holderOpen,waiterResumed,holderCompleted;
        final String mutation;int snapshots;
        CapturedLockDriver(String mutation){this.mutation=mutation;}
        public Set<String> availableAdapters(){return Set.of("SELFTEST_CAPTURED_ONLY");}
        private StepResult captured(String id,JsonNode data,JsonNode response,JsonNode query,JsonNode snapshot) {
            events.add(id);ObjectNode provenance=Json.object();
            provenance.put("adapter","V8-lock-SELFTEST").put("adapterVersion","1").put("buildVersion","SELFTEST").putNull("authenticatedActor").put("source","CANNED_CONTRACT_SELFTEST").put("independent",true).put("scopeComplete",true);
            provenance.set("sourceQuery",query==null?Json.MAPPER.nullNode():query);provenance.set("snapshot",snapshot==null?Json.MAPPER.nullNode():snapshot);
            return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,response,"SELFTEST captured mechanics; no PostgreSQL/API execution",provenance,List.of(ARTIFACT));
        }
        public StepResult installFixture(String id,JsonNode fixture) {
            ObjectNode aliases=Json.object();fixture.path("fixture").path("aliases").fieldNames().forEachRemaining(a->aliases.put(a,"captured-"+a));
            ObjectNode data=Json.object();data.set("aliasMap",aliases);data.set("fixtureHash",fixture.path("fixtureHash"));return captured(id,data,null,null,null);
        }
        public StepResult start(String id,String route,JsonNode actor,String capability,JsonNode request) {
            if(id.endsWith("reserve40")) holderOpen=true;
            return captured(id,Json.object().put("invocationHandle","captured-"+id),null,null,null);
        }
        public StepResult control(String id,JsonNode control) {
            ObjectNode data=control.path("parameters").deepCopy();data.put("controlType",control.path("type").asText()).put("operation",control.path("operation").asText()).put("acknowledged",true).put("acknowledgedAt","2026-10-07T09:00:01Z");
            String participant=data.path("participantId").asText();
            data.set("database",Json.object().put("transactionId",participant.equals("reserve40")?HOLDER:WAITER));
            if(id.endsWith("resume30")) waiterResumed=true;
            if(id.endsWith("resume40")) holderOpen=false;
            return captured(id,data,null,null,null);
        }
        public StepResult await(String id,JsonNode handle,int timeout) {
            if(id.endsWith("terminal40")) {assertFalse(holderOpen,"SELFTEST holder must be released before terminal");holderCompleted=true;}
            else {assertTrue(holderCompleted,"SELFTEST waiter terminal must follow holder lock release/commit");assertTrue(waiterResumed);}
            ObjectNode data=Json.object();data.set("invocationHandle",handle);data.put("completed",true).put("terminalStatus","SUCCEEDED");
            ObjectNode response=Json.object();if(id.endsWith("terminal40"))response.put("outcome","APPLIED");else response.set("error",Json.object().put("code","STALE_REVISION"));
            return captured(id,data,response,null,null);
        }
        public StepResult observe(String id,JsonNode request) {
            ObjectNode query=Json.object().put("statementId","SELFTEST-captured-locks").put("sql","SELFTEST captured catalogue rows; no SQL executed").put("mappingVersion","SELFTEST-v1");query.set("parameters",request.path("scope"));
            // The observer reports its own snapshot token and read mode; it never echoes the requested reference.
            String ref=request.path("snapshotRef").asText();boolean directive=CaseRunner.SNAPSHOT_DIRECTIVES.contains(ref);
            ObjectNode snapshot=Json.object().put("id","selftest-mvcc-"+(++snapshots)).put("isolation","SELFTEST_CAPTURED").put("capturedAt","2026-10-07T09:00:01Z").put("artifactRef",ARTIFACT).put("readMode",directive?ref:"RESULT_REVISION");
            if(!directive) snapshot.set("revisionQuery",query);
            ObjectNode data=Json.object();data.put("snapshotRevision",directive?"selftest-observer-world-"+snapshots:ref);for(String field:List.of("asOf","knownAt","scope"))data.set(field,request.path(field));data.put("scopeComplete",true).set("sourceQuery",query);data.set("snapshot",snapshot);
            ObjectNode rows=Json.object(),evidence=Json.object();
            for(JsonNode source:request.path("sources")) {
                String name=source.asText();rows.set(name,Json.array());ObjectNode e=Json.object().put("complete",true).put("rowPointer","/rawRows/"+name).put("artifactRef",ARTIFACT);e.set("sourceQuery",query);evidence.set(name,e);
            }
            if(id.endsWith("waiter-before-release") && holderOpen && waiterResumed && !mutation.equals("NO_LOCK")) {
                ObjectNode wait=Json.object().put("scopeId","captured-A60").put("transactionId",WAITER).put("blockingTransactionId",mutation.equals("WRONG_BLOCKER")?"other-xid":HOLDER).put("lockResourceId",HOLDER).put("lockType","transactionid").put("lockMode","ShareLock").put("granted",false).put("holderLockMode","ExclusiveLock").put("holderGranted",true).put("waitEventType","Lock").put("source","pg_catalog");rows.set("lockWaits",Json.array().add(wait));
                rows.set("databaseTransactions",Json.array().add(Json.object().put("transactionId",HOLDER).put("status","OPEN").put("lockState","HOLDING_SCOPE_LOCK")).add(Json.object().put("transactionId",WAITER).put("status","OPEN").put("lockState","WAITING_SCOPE_LOCK")));
            }
            if(id.endsWith("after")) {
                rows.set("lockEvidence",Json.array().add(Json.object().put("scopeId","captured-A60").put("transactionId",HOLDER).put("lockMode","EXCLUSIVE")).add(Json.object().put("scopeId","captured-A60").put("transactionId",WAITER).put("lockMode","EXCLUSIVE")));
                rows.set("lockRevalidations",Json.array().add(Json.object().put("scopeId","captured-A60").put("transactionId",WAITER).put("lockedRevision",mutation.equals("STALE_REREAD")?1:2).put("requestedRevision",1).put("point","AFTER_SCOPE_LOCK").put("result","STALE_REVISION")));
                rows.set("newAllocations",Json.array().add(Json.object().put("quantity","40").put("unit","BOX").put("status","EXECUTABLE")));
                rows.set("obligations",Json.array().add(Json.object().put("id","captured-DUTY").put("ownerId","captured-procurement").put("status","OPEN").put("quantity","40").put("unit","BOX").put("nextAction","나머지 수령 확인").put("nextCheckAt","2026-10-08T09:00:00Z")));
            }
            // Inventory values are derived from captured segment/allocation rows, never typed in directly.
            boolean ledger=rows.has("segments") && rows.has("allocations");
            if(ledger) {
                rows.set("segments",Json.array().add(Json.object().put("segmentId","captured-A60").put("quantity","60").put("unit","BOX")));
                ArrayNode allocations=Json.array().add(Json.object().put("allocationId","captured-ALLOC20").put("quantity","20").put("unit","BOX").put("status","EXECUTABLE"));
                if(id.equals("after") || id.equals("lock-after")) allocations.add(Json.object().put("allocationId","captured-ALLOC40").put("quantity","40").put("unit","BOX").put("status","EXECUTABLE"));
                rows.set("allocations",allocations);
            }
            data.set("rawRows",rows);data.set("sourceEvidence",evidence);
            if(ledger) {
                data.set("data",Json.object().set("inventory",Json.object().put("heldQuantity","60").put("reservedQuantity",id.equals("after")||id.equals("lock-after")?"60":"20").put("unit","BOX")));
                data.set("derivations",Json.parse("{\"/inventory/heldQuantity\":{\"rowPointer\":\"/rawRows/segments\",\"aggregate\":\"sum\",\"field\":\"quantity\",\"unitField\":\"unit\"},\"/inventory/unit\":{\"rowPointer\":\"/rawRows/segments\",\"aggregate\":\"distinct\",\"field\":\"unit\"},\"/inventory/reservedQuantity\":{\"rowPointer\":\"/rawRows/allocations\",\"where\":{\"status\":\"EXECUTABLE\"},\"aggregate\":\"sum\",\"field\":\"quantity\",\"unitField\":\"unit\"}}"));
            } else data.set("data",Json.object());
            return captured(id,data,null,query,snapshot);
        }
        public StepResult invoke(String id,String route,JsonNode actor,String cap,JsonNode request){throw new AssertionError("Unexpected SELFTEST sync call");}
        public StepResult query(String id,String route,JsonNode actor,String cap,JsonNode request){throw new AssertionError("Unexpected SELFTEST query");}
    }
    @Test void authoredScheduleRequiresCapturedWaitBeforeHolderReleaseAndBothTerminalAcks() throws Exception {
        CapturedLockDriver driver=new CapturedLockDriver("NONE");CaseRunner runner=runner(casePath(false),driver);assertEquals("PASS",runner.run(false));runner.verifyComplete();
        assertTrue(driver.events.indexOf("resume30")<driver.events.indexOf("waiter-before-release"));assertTrue(driver.events.indexOf("waiter-before-release")<driver.events.indexOf("resume40"));
        assertFalse(runner.evidence("PASS","SELFTEST only").path("productCoverageClaimed").asBoolean());
        assertEquals("CANNED_CONTRACT_SELFTEST",runner.results().get("waiter-before-release").path("provenance").path("source").asText());
    }
    @Test void upgradeLockSectionExecutesSameIdentityBoundWaitAndRevalidationContract() throws Exception {
        String subcase="upgrade-revalidate-transactions";CapturedLockDriver driver=new CapturedLockDriver("NONE");
        CaseRunner runner=CaseRunner.harnessSelftest(new ContractValidator(root),driver,new AgentRunner.Scripted(),casePath(subcase,false),subcase);
        assertEquals("PASS",runner.run(false));runner.verifyComplete();
        assertTrue(driver.events.indexOf("lock-waiter-before-release")<driver.events.indexOf("lock-resume40"));
    }
    @Test void priorSerializedScheduleFailsAlthoughFinalResponseAndQuantityRemainCorrect() throws Exception {
        CaseRunner runner=runner(casePath(true),new CapturedLockDriver("NONE"));assertEquals("FAIL",runner.run(false));
        assertEquals("STALE_REVISION",runner.results().get("terminal30").at("/response/error/code").asText());
        assertEquals("60",runner.results().get("after").at("/data/data/inventory/reservedQuantity").asText());
        assertTrue(runner.results().get("waiter-before-release").at("/data/rawRows/lockWaits").isEmpty());
    }
    @Test void noDatabaseLockWrongBlockerAndStaleRereadEachFailIndependently() throws Exception {
        for(String mutation:List.of("NO_LOCK","WRONG_BLOCKER","STALE_REREAD")) assertEquals("FAIL",runner(casePath(false),new CapturedLockDriver(mutation)).run(false),mutation);
    }
    @Test void absentProductAdaptersRemainNotRunAndNeverInventLockRows() throws Exception {
        CaseRunner runner=CaseRunner.harnessSelftest(new ContractValidator(root),new UnimplementedDriver(),new AgentRunner.Scripted(),casePath(false),SUBCASE);
        assertEquals("NOT_RUN",runner.run(false));
        assertEquals("NOT_IMPLEMENTED",runner.results().get("waiter-before-release").path("driverStatus").asText());
        assertTrue(runner.results().get("waiter-before-release").path("data").isNull());
        assertFalse(runner.evidence("NOT_RUN","SELFTEST missing-product-ports").path("runtimeComplete").asBoolean());
    }
    @Test void freshAndUpgradeUseSameWaitBeforeReleaseSequenceAndIndependentObserverSources() throws Exception {
        JsonNode file=Json.read(root.resolve("verification/cases/V8/case.json"));
        for(JsonNode sub:file.path("subcases")) if(Set.of(SUBCASE,"upgrade-revalidate-transactions").contains(sub.path("id").asText())) {
            String prefix=sub.path("id").asText().equals(SUBCASE)?"":"lock-";List<String> ids=new ArrayList<>();sub.path("actions").forEach(a->ids.add(a.path("id").asText()));
            assertTrue(ids.indexOf(prefix+"resume30")<ids.indexOf(prefix+"waiter-before-release"));assertTrue(ids.indexOf(prefix+"waiter-before-release")<ids.indexOf(prefix+"resume40"));assertTrue(ids.indexOf(prefix+"resume40")<ids.indexOf(prefix+"terminal40"));assertTrue(ids.indexOf(prefix+"terminal40")<ids.indexOf(prefix+"terminal30"));
        }
    }
}
