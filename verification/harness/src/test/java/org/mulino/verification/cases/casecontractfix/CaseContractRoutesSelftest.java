package org.mulino.verification.cases.casecontractfix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mulino.verification.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Fixed EXECUTED payloads validate schema/routes/oracles only; no runtime acceptance. */
public final class CaseContractRoutesSelftest {
    private final Path root=Path.of(System.getProperty("repo.root"));
    private final JsonNode aliases=Json.parse("{\"ORG\":\"SELFTEST-org\",\"Q20\":\"SELFTEST-Q20\",\"DOC-TEMP\":\"SELFTEST-doc\"}");
    private final AssertionEngine engine=new AssertionEngine();
    private JsonNode caseFile(String id) throws Exception {return Json.read(root.resolve("verification/cases/"+id+"/case.json"));}
    private JsonNode subcase(String id) throws Exception {
        for(JsonNode sub:caseFile("T26").path("subcases")) if(sub.path("id").asText().equals(id))return sub;
        throw new IllegalArgumentException(id);
    }
    private JsonNode assertion(JsonNode sub,String id) {
        for(JsonNode a:sub.path("assertions")) if(a.path("id").asText().equals(id))return a;
        throw new IllegalArgumentException(id);
    }
    private ObjectNode hostResult(String operation) throws Exception {
        return (ObjectNode)Json.read(root.resolve("verification/cases/T26/selftest/host-shaped-results.json")).path(operation).deepCopy();
    }
    private void refs(JsonNode node,List<JsonNode> refs) {
        if(node.isObject() && node.has("$result") && node.path("$result").path("pointer").asText().startsWith("/data/hostObservation/"))refs.add(node);
        else if(node.isContainerNode())for(JsonNode child:node)refs(child,refs);
    }
    private void actions(JsonNode node,Map<String,JsonNode> out) {
        for(JsonNode a:node){out.put(a.path("id").asText(),a);if(a.has("actions"))actions(a.path("actions"),out);}
    }
    @Test void downstreamHostReferencesResolveFromSchemaValidExecutedPayloads() throws Exception {
        ContractValidator validator=new ContractValidator(root);int resolved=0,selected=0;
        for(String id:List.of("T26","V5","T14"))for(JsonNode sub:caseFile(id).path("subcases")) {
            assertFalse(sub.toString().contains("/data/hostObservation/rawRows"),"Forbidden obsolete host row route in "+id);
            List<JsonNode> references=new ArrayList<>();refs(sub,references);
            Map<String,JsonNode> declarations=new HashMap<>();actions(sub.path("actions"),declarations);
            for(JsonNode reference:references) {
                JsonNode ref=reference.path("$result");String actionId=ref.path("actionId").asText();
                String op=declarations.get(actionId).path("control").path("operation").asText();
                ObjectNode result=hostResult(op);assertTrue(result.path("data").path("hostObservation").isObject(),op);
                validator.schema("contracts/acceptance-host-observation.schema.json",result.path("data").path("hostObservation"));
                JsonNode value=new ReferenceResolver(Map.of(actionId,result),aliases).resolve(reference);
                assertFalse(value.isMissingNode());assertFalse(value.isNull());resolved++;
                String pointer=ref.path("pointer").asText();
                if(pointer.startsWith("/data/hostObservation/operationEvidence/taskId") || pointer.endsWith("/invocationHandle")) {
                    assertTrue(pointer.startsWith("/data/hostObservation/operationEvidence/"));
                    String obsolete=pointer.replace("/operationEvidence/","/rawRows/tasks/0/");
                    ObjectNode bad=Json.object().set("$result",Json.parse("{\"actionId\":\""+actionId+"\",\"pointer\":\""+obsolete+"\"}"));
                    assertThrows(AssertionError.class,()->new ReferenceResolver(Map.of(actionId,result),aliases).resolve(bad));
                }
            }
            for(JsonNode a:sub.path("assertions"))for(String sourceField:List.of("source","baseline","unitSource","baselineUnitSource")) {
                JsonNode declared=a.path(sourceField);
                if(!declared.path("pointer").asText().startsWith("/data/hostObservation/extractor/rawRows/"))continue;
                String actionId=declared.path("actionId").asText();String op=declarations.get(actionId).path("control").path("operation").asText();
                ObjectNode result=hostResult(op);validator.schema("contracts/acceptance-host-observation.schema.json",result.at("/data/hostObservation"));
                JsonNode source=new ReferenceResolver(Map.of(actionId,result),aliases).identity(declared);
                assertDoesNotThrow(()->engine.select(source,Map.of(actionId,result),false));selected++;
            }
        }
        assertTrue(selected>=21,"Exercise every corrected extractor selector, including claim time baseline and restore fields");
        assertTrue(resolved>=67,"Resolve every declared downstream host result ref, including claims and snapshot identity");
    }
    @Test void forbiddenTopLevelRowsCannotRescueAnOldPointerAndMissingClaimRowsFailClosed() throws Exception {
        ContractValidator validator=new ContractValidator(root);ObjectNode result=hostResult("claim");
        ObjectNode host=(ObjectNode)result.at("/data/hostObservation");host.set("rawRows",host.path("extractor").path("rawRows").deepCopy());
        assertThrows(IllegalArgumentException.class,()->validator.schema("contracts/acceptance-host-observation.schema.json",host));
        host.remove("rawRows");validator.schema("contracts/acceptance-host-observation.schema.json",host);
        ObjectNode source=Json.parse("{\"actionId\":\"claim\",\"pointer\":\"/data/hostObservation/extractor/rawRows/claims/0/barrierId\"}").deepCopy();
        assertEquals("SELFTEST-barrier",engine.select(source,Map.of("claim",result),false).asText());
        ((ObjectNode)host.path("extractor").path("rawRows")).remove("claims");
        assertThrows(AssertionError.class,()->engine.select(source,Map.of("claim",result),false));
    }
    @Test void correctedRestoreAssertionRunsOnActualHostShapeAndRejectsIncompleteRestore() throws Exception {
        JsonNode a=assertion(subcase("restore-complete"),"restore-completeness");ObjectNode result=hostResult("restore");
        new ContractValidator(root).schema("contracts/acceptance-host-observation.schema.json",result.at("/data/hostObservation"));
        engine.check(a,Map.of("restore",result),aliases);
        ((ObjectNode)result.at("/data/hostObservation/extractor/rawRows/restoreResult")).put("status","INCOMPLETE");
        assertThrows(AssertionError.class,()->engine.check(a,Map.of("restore",result),aliases));
    }
    @Test void repeatedSweepNoTaskIsSchemaValidAndCannotSupplyAStaleTaskHandle() throws Exception {
        ObjectNode result=hostResult("sweepDue");JsonNode a=assertion(subcase("lot-expiry-no-event"),"repeat-no-due-task");
        assertThrows(AssertionError.class,()->engine.check(a,Map.of("repeat-sweep",result),aliases));
        for(JsonNode node:List.of(result.at("/data/hostObservation/operationEvidence"),result.at("/data/hostObservation/extractor/rawRows/operationEvidence"))) {
            ObjectNode identity=(ObjectNode)node;identity.put("submissionStatus","NO_TASK");identity.remove(List.of("taskId","invocationHandle","submittedAt"));
        }
        new ContractValidator(root).schema("contracts/acceptance-host-observation.schema.json",result.at("/data/hostObservation"));
        engine.check(a,Map.of("repeat-sweep",result),aliases);
        JsonNode stale=Json.parse("{\"$result\":{\"actionId\":\"repeat-sweep\",\"pointer\":\"/data/hostObservation/operationEvidence/taskId\"}}");
        assertThrows(AssertionError.class,()->new ReferenceResolver(Map.of("repeat-sweep",result),aliases).resolve(stale));
        ((ObjectNode)result.at("/data/hostObservation/operationEvidence")).put("taskId","SELFTEST-stale");
        assertThrows(IllegalArgumentException.class,()->new ContractValidator(root).schema("contracts/acceptance-host-observation.schema.json",result.at("/data/hostObservation")));
    }
    private ObjectNode dbResult(JsonNode rows) {
        ObjectNode r=Json.object();r.put("driverStatus","EXECUTED");r.set("provenance",Json.parse("{\"scopeComplete\":true,\"source\":\"CANNED_ASSERTION_SELFTEST\"}"));
        r.set("data",Json.object().set("rawRows",rows));return r;
    }
    private List<String> expiryIds(){return List.of("lot-expiry-no-event","disposition-expiry-no-event","grant-expiry-no-event","policy-expiry-no-event");}
    @Test void autonomousObservationPrecedesAnyPostBoundaryUserActionAndDelayedGuardIsIndependent() throws Exception {
        for(String id:expiryIds()) {
            JsonNode sub=subcase(id);List<String> order=new ArrayList<>();for(JsonNode a:sub.path("actions"))order.add(a.path("id").asText());
            assertTrue(order.indexOf("advance")<order.indexOf("sweep"));assertTrue(order.indexOf("sweep-terminal")<order.indexOf("sweep-db"));
            assertTrue(order.indexOf("sweep-db")<order.indexOf("repeat-sweep"));assertTrue(order.indexOf("repeat-db")<order.indexOf("dispatch"));
            boolean expired=false,captured=false;
            for(JsonNode a:sub.path("actions")){String aid=a.path("id").asText();if(aid.equals("advance"))expired=true;if(aid.equals("sweep-db"))captured=true;
                if(expired && !captured)assertFalse(Set.of("invoke","query","agent","start").contains(a.path("kind").asText()),"No user request may prime the expiry result: "+aid);}
            JsonNode delayed=subcase(id.replace("-no-event","-delayed-guard"));List<String> ops=new ArrayList<>();for(JsonNode a:delayed.path("actions"))ops.add(a.path("control").path("operation").asText());
            assertFalse(ops.contains("sweepDue"));assertFalse(ops.contains("awaitRuntimeTask"));assertTrue(ops.contains("stop"));
            assertEquals("REJECTED",assertion(delayed,"guard-outcome").path("expected").asText());
        }
    }
    @Test void expiryEventGoalAndOpenFollowupAreOnceAndRepeatedRowsKeepIdentity() throws Exception {
        JsonNode rows=Json.parse("{\"events\":[{\"id\":\"SELFTEST-expiry\",\"kind\":\"VALIDITY_EXPIRED\"}],\"assessments\":[{\"id\":\"SELFTEST-assessment\",\"current\":true,\"causeKind\":\"VALIDITY_EXPIRED\",\"workId\":\"SELFTEST-work\",\"result\":\"UNVERIFIED\",\"asOf\":\"2026-10-07T09:00:20Z\"}],\"obligations\":[{\"id\":\"SELFTEST-duty\",\"sourceKind\":\"VALIDITY_EXPIRED\",\"status\":\"OPEN\"}]}");
        ObjectNode create=Json.object();create.put("driverStatus","EXECUTED");create.set("provenance",Json.parse("{\"scopeComplete\":true}"));create.set("response",Json.parse("{\"workId\":\"SELFTEST-work\"}"));
        for(String id:expiryIds())for(String pair:List.of("expiry-event-one:events","goal-refresh-one:assessments","followup-open-one:obligations")) {
            String[] parts=pair.split(":");JsonNode sub=subcase(id);ObjectNode before=dbResult(rows.deepCopy()),after=dbResult(rows.deepCopy());
            Map<String,JsonNode> r=new HashMap<>(Map.of("create",create,"sweep-db",before,"repeat-db",after));
            for(String action:List.of("sweep-db","repeat-db"))engine.check(assertion(sub,action+"-"+parts[0]),r,aliases);
            JsonNode unchanged=assertion(sub,"repeat-"+parts[1]+"-identity-kept");engine.check(unchanged,r,aliases);
            ArrayNode values=(ArrayNode)after.at("/data/rawRows/"+parts[1]);JsonNode original=values.get(0).deepCopy();values.removeAll();
            assertThrows(AssertionError.class,()->engine.check(assertion(sub,"repeat-db-"+parts[0]),r,aliases));
            values.add(original.deepCopy()).add(original.deepCopy());assertThrows(AssertionError.class,()->engine.check(assertion(sub,"repeat-db-"+parts[0]),r,aliases));
            values.remove(1);((ObjectNode)values.get(0)).put("id","SELFTEST-other");assertThrows(AssertionError.class,()->engine.check(unchanged,r,aliases));
        }
        JsonNode sub=subcase("lot-expiry-no-event");Map<String,JsonNode> r=Map.of("create",create,"sweep-db",dbResult(rows.deepCopy()));
        engine.check(assertion(sub,"sweep-db-goal-still-unverified"),r,aliases);
        ((ObjectNode)r.get("sweep-db").at("/data/rawRows/assessments/0")).put("result","SATISFIED");
        assertThrows(AssertionError.class,()->engine.check(assertion(sub,"sweep-db-goal-still-unverified"),r,aliases));
    }
    @Test void dryrunAndDeniedApplyRejectEarlyProjectionRepairEmptyRowsAndIdentityChanges() throws Exception {
        JsonNode sub=subcase("emergency-repair-dryrun-apply");List<String> order=new ArrayList<>();for(JsonNode a:sub.path("actions"))order.add(a.path("id").asText());
        assertTrue(order.indexOf("before-db")<order.indexOf("dry-run"));assertTrue(order.indexOf("dry-run")<order.indexOf("dryrun-db"));assertTrue(order.indexOf("dryrun-db")<order.indexOf("unauthorized-apply"));assertTrue(order.indexOf("denied-db")<order.indexOf("apply"));
        JsonNode rows=Json.parse("{\"projections\":[{\"id\":\"SELFTEST-projection\",\"targetId\":\"SELFTEST-Q20\",\"sourceRevision\":0}]}");
        for(String action:List.of("dryrun-db","denied-db")) {
            ObjectNode before=dbResult(rows.deepCopy()),after=dbResult(rows.deepCopy());Map<String,JsonNode> r=Map.of("before-db",before,action,after);
            for(String suffix:List.of("one","identity-revision","complete","unchanged"))engine.check(assertion(sub,action+"-projection-"+suffix),r,aliases);
            ((ObjectNode)after.at("/data/rawRows/projections/0")).put("sourceRevision",1);
            assertThrows(AssertionError.class,()->engine.check(assertion(sub,action+"-projection-unchanged"),r,aliases));
            assertThrows(AssertionError.class,()->engine.check(assertion(sub,action+"-projection-identity-revision"),r,aliases));
            ((ObjectNode)after.at("/data/rawRows/projections/0")).put("sourceRevision",0).put("id","SELFTEST-other");
            assertThrows(AssertionError.class,()->engine.check(assertion(sub,action+"-projection-unchanged"),r,aliases));
            ((ArrayNode)after.at("/data/rawRows/projections")).removeAll();((ArrayNode)before.at("/data/rawRows/projections")).removeAll();
            engine.check(assertion(sub,action+"-projection-unchanged"),r,aliases);
            assertThrows(AssertionError.class,()->engine.check(assertion(sub,action+"-projection-one"),r,aliases));
            assertThrows(AssertionError.class,()->engine.check(assertion(sub,action+"-projection-complete"),r,aliases));
        }
    }
}
