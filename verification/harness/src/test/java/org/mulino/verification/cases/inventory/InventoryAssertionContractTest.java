package org.mulino.verification.cases.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Fixed, independent observation samples test real assertions, never an application driver. */
public final class InventoryAssertionContractTest {
    private final Path root=Path.of(System.getProperty("repo.root"));
    private final AssertionEngine engine=new AssertionEngine();
    private final JsonNode aliases=Json.parse("{\"ORG\":\"org-fixture-id\",\"P\":\"item-fixture-id\",\"S1\":\"sales-work-id\",\"sales\":\"sales-human-id\",\"supervisor\":\"supervisor-human-id\",\"Q100\":\"parent-physical-id\",\"A20\":\"cargo-physical-id\"}");
    private JsonNode sub(String caseId,String subId) throws Exception {
        JsonNode c=Json.read(root.resolve("verification/cases/"+caseId+"/case.json"));
        for(JsonNode s:c.path("subcases")) if(s.path("id").asText().equals(subId)) return s;
        throw new AssertionError("Missing owned subcase "+caseId+"/"+subId);
    }
    private JsonNode assertion(String caseId,String subId,String id) throws Exception {
        for(JsonNode a:sub(caseId,subId).path("assertions")) if(a.path("id").asText().equals(id)) return a;
        throw new AssertionError("Missing owned assertion "+id);
    }
    private JsonNode find(String caseId,String subId,String name,String op,String prefix) throws Exception {
        for(JsonNode a:sub(caseId,subId).path("assertions")) if(a.path("oracleRef").path("observationNames").get(0).asText().equals(name) && a.path("op").asText().equals(op) && a.path("source").path("pointer").asText().startsWith(prefix)) return a;
        throw new AssertionError("Missing independent assertion "+name+"/"+op);
    }
    private ObjectNode observation(String response,String data) {
        ObjectNode r=Json.object();r.put("driverStatus","EXECUTED");r.put("evidenceClass","CAPTURED_SELFTEST");
        r.set("response",response==null?Json.MAPPER.nullNode():Json.parse(response));r.set("data",data==null?Json.MAPPER.nullNode():Json.parse(data));
        r.set("provenance",Json.parse("{\"scopeComplete\":true,\"source\":\"CANNED_ASSERTION_SELFTEST\"}"));return r;
    }
    private void check(JsonNode a,Map<String,JsonNode> observations) {engine.check(a,observations,aliases);}

    @Test void wrongPhysicalQuantityAndNumericWireValueAreRejected() throws Exception {
        JsonNode a=find("T04","hold-dispose","held-after-hold","decimalEquals","/response");
        ObjectNode r=observation("{\"data\":{\"heldQuantity\":\"100\",\"unit\":\"BOX\"}}",null);
        Map<String,JsonNode> observations=Map.of("held-api",r);check(a,observations);
        ObjectNode data=(ObjectNode)r.path("response").path("data");data.put("heldQuantity","90");assertThrows(AssertionError.class,()->check(a,observations));
        data.put("heldQuantity",100);assertThrows(AssertionError.class,()->check(a,observations));
    }
    @Test void zeroDeltaStillRejectsBaselineUnitSubstitution() throws Exception {
        JsonNode a=assertion("T04","hold-dispose","hold-physical-delta");
        ObjectNode before=observation("{\"data\":{\"heldQuantity\":\"100\",\"unit\":\"BOX\"}}",null);
        ObjectNode held=before.deepCopy();Map<String,JsonNode> observations=Map.of("before-api",before,"held-api",held);check(a,observations);
        ((ObjectNode)before.path("response").path("data")).put("unit","KG");assertThrows(AssertionError.class,()->check(a,observations));
        ((ObjectNode)before.path("response").path("data")).put("unit","BOX");((ObjectNode)held.path("response").path("data")).put("unit","KG");assertThrows(AssertionError.class,()->check(a,observations));
    }
    @Test void duplicatePhysicalLeafCannotHideBehindAConservedSum() throws Exception {
        JsonNode a=assertion("T03","split-merge","active-physical-identities");
        ObjectNode db=observation(null,"{\"rawRows\":{\"segments\":[{\"active\":true,\"physicalScope\":\"distinguishable-A\",\"quantity\":\"60\"},{\"active\":true,\"physicalScope\":\"distinguishable-B\",\"quantity\":\"40\"}]}}");
        Map<String,JsonNode> observations=Map.of("after-db",db);check(a,observations);
        ((ObjectNode)db.at("/data/rawRows/segments/1")).put("physicalScope","distinguishable-A");assertThrows(AssertionError.class,()->check(a,observations));
    }
    @Test void exactOwnerAndCurrentAssignmentCannotBeReplacedByPresence() throws Exception {
        JsonNode a=find("T16","expiry-sweeper","expiry-duty","relationSet","/data/rawRows/assignments");
        ObjectNode db=observation(null,"{\"rawRows\":{\"assignments\":[{\"obligationId\":\"expiry-obligation-id\",\"workId\":\"sales-work-id\",\"status\":\"OPEN\",\"current\":true,\"ownerId\":\"sales-human-id\",\"supervisorId\":\"supervisor-human-id\",\"nextAction\":\"근거 대조 후 대체 배분 검토\",\"nextCheckAt\":\"2026-10-07T10:00:00Z\"}]}}");
        Map<String,JsonNode> observations=Map.of("after-db",db,"expiry-obligations",observation("{\"data\":{\"obligations\":[{\"id\":\"expiry-obligation-id\"}]}}",null));check(a,observations);
        ObjectNode assignment=(ObjectNode)db.at("/data/rawRows/assignments/0");assignment.put("ownerId","agent-id");assertThrows(AssertionError.class,()->check(a,observations));
        assignment.remove("ownerId");assertThrows(AssertionError.class,()->check(a,observations));
        assignment.put("ownerId","sales-human-id");assignment.put("current",false);assertThrows(AssertionError.class,()->check(a,observations));
    }
    @Test void expiredAllocationMustActuallySuspendAndNotConsume() throws Exception {
        JsonNode a=find("T16","expiry-sweeper","allocation-after-boundary","equals","/data/rawRows/allocations");
        ObjectNode reserve=observation("{\"allocationId\":\"allocation-20-id\"}",null);
        ObjectNode db=observation(null,"{\"rawRows\":{\"allocations\":[{\"id\":\"allocation-20-id\",\"state\":\"SUSPENDED\"}]}}");
        Map<String,JsonNode> observations=Map.of("after-db",db,"reserve",reserve);check(a,observations);
        ((ObjectNode)db.at("/data/rawRows/allocations/0")).put("state","CONSUMED");assertThrows(AssertionError.class,()->check(a,observations));
    }
    @Test void actual50UpperBoundAcceptsBothZeroAndFiftyButRejectsFiftyOne() throws Exception {
        JsonNode a=find("V2","actual50-correction","executable-allocation","decimalAtMost","/data/data");
        ObjectNode db=observation(null,"{\"data\":{\"executableAllocationQuantity\":\"50\"},\"rawRows\":{\"segments\":[{\"active\":true,\"quantity\":\"50\",\"unit\":\"BOX\"}]}}");
        Map<String,JsonNode> observations=Map.of("after-db",db);check(a,observations);
        ((ObjectNode)db.at("/data/data")).put("executableAllocationQuantity","0");check(a,observations);
        ((ObjectNode)db.at("/data/data")).put("executableAllocationQuantity","51");assertThrows(AssertionError.class,()->check(a,observations));
    }
    @Test void shortageLowerBoundRejectsNineAndPreservesMoreThanTen() throws Exception {
        JsonNode a=find("V2","actual50-correction","minimum-shortage-duty","decimalAtLeast","/data/data");
        ObjectNode db=observation(null,"{\"data\":{\"shortageQuantity\":\"10\"},\"rawRows\":{\"segments\":[{\"active\":true,\"quantity\":\"50\",\"unit\":\"BOX\"}]}}");
        Map<String,JsonNode> observations=Map.of("after-db",db);check(a,observations);
        ((ObjectNode)db.at("/data/data")).put("shortageQuantity","11");check(a,observations);
        ((ObjectNode)db.at("/data/data")).put("shortageQuantity","9");assertThrows(AssertionError.class,()->check(a,observations));
    }
    @Test void lateOldReleaseMustNotOverwriteActualNewRestrictionIdentity() throws Exception {
        JsonNode a=find("V3","late-v1-release","late-release-overwrites-new-hold","relationSet","/data/rawRows/restrictions");
        ObjectNode hold=observation("{\"restrictionId\":\"new-restriction-v2\"}",null);
        ObjectNode db=observation(null,"{\"rawRows\":{\"restrictions\":[{\"id\":\"new-restriction-v2\",\"state\":\"ACTIVE\",\"sourceVersion\":\"2\"}]}}");
        Map<String,JsonNode> observations=Map.of("after-db",db,"new-hold",hold);check(a,observations);
        ((ObjectNode)db.at("/data/rawRows/restrictions/0")).put("state","RELEASED");assertThrows(AssertionError.class,()->check(a,observations));
        ((ObjectNode)db.at("/data/rawRows/restrictions/0")).put("state","ACTIVE");((ObjectNode)db.at("/data/rawRows/restrictions/0")).put("sourceVersion","1");assertThrows(AssertionError.class,()->check(a,observations));
    }
    @Test void missingAndUnknownDoNotBecomeZeroDispatched() throws Exception {
        JsonNode a=find("V3","hold-first","new-dispatched","decimalEquals","/response");
        ObjectNode r=observation("{\"data\":{\"dispatchedQuantity\":\"0\",\"unit\":\"BOX\"}}",null);
        Map<String,JsonNode> observations=Map.of("after-api",r);check(a,observations);
        ((ObjectNode)r.at("/response/data")).put("dispatchedQuantity","UNKNOWN");assertThrows(AssertionError.class,()->check(a,observations));
        ((ObjectNode)r.at("/response/data")).remove("dispatchedQuantity");assertThrows(AssertionError.class,()->check(a,observations));
        r.put("driverStatus","NOT_IMPLEMENTED");assertThrows(AssertionError.class,()->check(a,observations));
    }
    @Test void wrongVersionOrIncompleteScopeIsNotAccepted() throws Exception {
        JsonNode a=assertion("T03","split-merge","response-definition-version");
        ObjectNode r=observation("{\"definitionVersion\":\"ontology-v1\"}",null);Map<String,JsonNode> observations=Map.of("after-api",r);check(a,observations);
        ((ObjectNode)r.path("response")).put("definitionVersion","ontology-v2");assertThrows(AssertionError.class,()->check(a,observations));
        ((ObjectNode)r.path("response")).put("definitionVersion","ontology-v1");((ObjectNode)r.path("provenance")).put("scopeComplete",false);assertThrows(AssertionError.class,()->check(a,observations));
    }
    @Test void oracleCatalogNamedObservationsAreAllBoundWithoutInventingNames() throws Exception {
        Set<String> ids=Set.of("T03","T04","T05","T16","C1","V2","V3");Set<String> mandatory=new HashSet<>(), actual=new HashSet<>();
        for(JsonNode o:Json.read(root.resolve("verification/requirements/mandatory-oracles.json")).path("oracles")) if(ids.contains(o.path("caseId").asText())) for(JsonNode observation:o.path("expectedObservations")) mandatory.add(o.path("oracleId").asText()+"/"+observation.path("name").asText());
        for(String id:ids) for(JsonNode s:Json.read(root.resolve("verification/cases/"+id+"/case.json")).path("subcases")) for(JsonNode a:s.path("assertions")) for(JsonNode name:a.path("oracleRef").path("observationNames")) actual.add(a.path("oracleRef").path("oracleId").asText()+"/"+name.asText());
        assertEquals(91,mandatory.size());assertEquals(mandatory,actual,"Names only establish links; case review remains necessary for semantic completeness");
    }
    @Test void allOwnedCaseAndFixtureSchemasValidate() throws Exception {
        ContractValidator validator=new ContractValidator(root);
        for(String id:List.of("T03","T04","T05","T16","C1","V2","V3")) assertEquals(id,validator.caseFile(root.resolve("verification/cases/"+id+"/case.json")).path("caseId").asText());
    }
}
