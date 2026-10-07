package org.mulino.verification.cases.authority;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.mulino.verification.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Captured inputs exercise real case assertions. No product driver or business state machine. */
public class AuthorityAssertionsTest {
    private final Path root=Path.of(System.getProperty("repo.root"));
    private final AssertionEngine engine=new AssertionEngine();
    private JsonNode assertion(String c,String sub,String id) throws Exception {
        for(JsonNode s:Json.read(root.resolve("verification/cases/"+c+"/case.json")).path("subcases"))
            if(s.path("id").asText().equals(sub)) for(JsonNode a:s.path("assertions")) if(a.path("id").asText().equals(id)) return a;
        throw new IllegalArgumentException("Missing concrete assertion "+c+"/"+sub+"/"+id);
    }
    private ObjectNode observed(String rows,String data,String response) throws Exception {
        ObjectNode r=Json.object();r.put("driverStatus","EXECUTED");r.set("provenance",Json.parse("{\"source\":\"CAPTURED_ASSERTION_SELFTEST\",\"scopeComplete\":true}"));
        r.set("data",Json.parse("{\"rawRows\":"+rows+",\"data\":"+data+"}"));r.set("response",Json.parse(response));return r;
    }
    private JsonNode aliases() throws Exception {return Json.parse("{\"ORG-A\":\"captured-org-A\",\"ORG-B\":\"captured-org-B\",\"P\":\"captured-item\",\"WORK\":\"captured-work\",\"warehouse\":\"captured-human\",\"GRANT\":\"captured-grant\"}");}
    @Test void unitBearingReceipt60RejectsWrongQuantityUnitDuplicatesAndUnavailable() throws Exception {
        var a=assertion("V6","lost-response-token-rpc-concurrent","receipt-physical-total");
        var r=observed("{\"segments\":[{\"physicalScope\":\"Q60-unreceived\",\"active\":true,\"quantity\":\"60\",\"unit\":\"BOX\"}]}","{}","{}");
        var map=new HashMap<String,JsonNode>();map.put("after",r);engine.check(a,map,aliases());
        ObjectNode row=(ObjectNode)r.at("/data/rawRows/segments/0");row.put("quantity","40");assertThrows(AssertionError.class,()->engine.check(a,map,aliases()));row.put("quantity","60");row.put("unit","KG");assertThrows(AssertionError.class,()->engine.check(a,map,aliases()));row.put("unit","BOX");
        ((com.fasterxml.jackson.databind.node.ArrayNode)r.at("/data/rawRows/segments")).add(row.deepCopy());assertThrows(AssertionError.class,()->engine.check(a,map,aliases()));
        map.put("after",StepResult.missing("after","observer missing; no factual zero").toJson());assertThrows(AssertionError.class,()->engine.check(a,map,aliases()));
    }
    @Test void zeroQuantityDeltaRejectsBaselineUnitMismatchUnknownAndMissing() throws Exception {
        var a=assertion("V7","enqueue-then-revoke","new-effect-quantity0");
        var before=observed("{}","{\"dispatchedQuantity\":\"0\",\"unit\":\"BOX\"}","{}");var after=before.deepCopy();var map=Map.<String,JsonNode>of("before",before,"after",after);engine.check(a,map,aliases());
        ((ObjectNode)before.at("/data/data")).put("unit","KG");assertThrows(AssertionError.class,()->engine.check(a,map,aliases()));((ObjectNode)before.at("/data/data")).put("unit","BOX");
        ((ObjectNode)after.at("/data/data")).put("dispatchedQuantity","UNKNOWN");assertThrows(AssertionError.class,()->engine.check(a,map,aliases()));((ObjectNode)after.at("/data/data")).remove("dispatchedQuantity");assertThrows(AssertionError.class,()->engine.check(a,map,aliases()));
    }
    @Test void readonlyGrantRejectsMutationOfExistingRowsAndNewEffectEvenWithDeniedResponse() throws Exception {
        var a=assertion("C3","api-moveQuantity","unchanged-movements");
        var before=observed("{\"movements\":[{\"id\":\"captured-movement\",\"quantity\":\"20\",\"unit\":\"BOX\"}]}","{}","{\"outcome\":\"REJECTED\",\"error\":{\"code\":\"FORBIDDEN\"}}");var after=before.deepCopy();var map=Map.<String,JsonNode>of("before",before,"after",after);engine.check(a,map,aliases());
        ((ObjectNode)after.at("/data/rawRows/movements/0")).put("quantity","21");assertThrows(AssertionError.class,()->engine.check(a,map,aliases()));
        ((ObjectNode)after.at("/data/rawRows/movements/0")).put("quantity","20");((com.fasterxml.jackson.databind.node.ArrayNode)after.at("/data/rawRows/movements")).add(Json.parse("{\"id\":\"second-effect\",\"quantity\":\"1\",\"unit\":\"BOX\"}"));assertThrows(AssertionError.class,()->engine.check(a,map,aliases()));
    }
    @Test void blockedDutyRejectsMissingOwnerWrongHumanWrongDeadlineAndDuplicateAssignment() throws Exception {
        var owner=assertion("V7","enqueue-then-revoke","blocked-duty-owner");var count=assertion("V7","enqueue-then-revoke","blocked-duty-assignment1");
        var r=observed("{\"obligations\":[{\"kind\":\"REAUTHORIZE\",\"status\":\"OPEN\",\"ownerId\":\"captured-human\",\"nextAction\":\"현재 권한을 재확인하고 필요한 재인가를 요청한다\",\"nextCheckAt\":\"2026-10-07T10:00:00Z\"}],\"assignments\":[{\"kind\":\"REAUTHORIZE\",\"current\":true,\"id\":\"captured-assignment\"}]}","{}","{}");
        var map=Map.<String,JsonNode>of("after",r);engine.check(owner,map,aliases());engine.check(count,map,aliases());ObjectNode duty=(ObjectNode)r.at("/data/rawRows/obligations/0");duty.put("ownerId","wrong-agent");assertThrows(AssertionError.class,()->engine.check(owner,map,aliases()));duty.put("ownerId","captured-human");duty.put("nextCheckAt","2026-10-08T10:00:00Z");assertThrows(AssertionError.class,()->engine.check(owner,map,aliases()));duty.remove("ownerId");assertThrows(AssertionError.class,()->engine.check(owner,map,aliases()));
        ((com.fasterxml.jackson.databind.node.ArrayNode)r.at("/data/rawRows/assignments")).add(Json.parse("{\"kind\":\"REAUTHORIZE\",\"current\":true,\"id\":\"duplicate\"}"));assertThrows(AssertionError.class,()->engine.check(count,map,aliases()));
    }
    @Test void staleVersionAndCrossOrganizationIdCannotMatch() throws Exception {
        var version=assertion("V6","lost-response-token-rpc-concurrent","definition-preserved");var org=assertion("T08","direct-id","own-org-id");
        var r=observed("{}","{}","{\"definitionVersion\":\"definition-v1\",\"data\":{\"organizationId\":\"captured-org-A\"}}");var map=Map.<String,JsonNode>of("replay",r,"own-read",r);engine.check(version,map,aliases());engine.check(org,map,aliases());((ObjectNode)r.path("response")).put("definitionVersion","definition-v2");assertThrows(AssertionError.class,()->engine.check(version,map,aliases()));((ObjectNode)r.at("/response/data")).put("organizationId","captured-org-B");assertThrows(AssertionError.class,()->engine.check(org,map,aliases()));
    }
    @Test void resultReferenceIdentityRejectsRetryWithDifferentCommittedEffect() throws Exception {
        var a=assertion("V6","lost-response-token-rpc-concurrent","result-a-receipt");var replay=observed("{}","{}","{\"activityId\":\"captured-receipt60\"}");var retry=replay.deepCopy();var map=Map.<String,JsonNode>of("replay",replay,"result-a",retry);engine.check(a,map,aliases());((ObjectNode)retry.path("response")).put("activityId","second-receipt");assertThrows(AssertionError.class,()->engine.check(a,map,aliases()));
    }
    @Test void actualDeniedResponseRemainsExecutedAndMissingObserverCannotProveNoWrite() throws Exception {
        var deny=assertion("C3","api-moveQuantity","attempt-code");var zero=assertion("C3","api-moveQuantity","inventory-effects-rows0");var r=observed("{}","{}","{\"outcome\":\"REJECTED\",\"error\":{\"code\":\"FORBIDDEN\"}}");engine.check(deny,Map.of("attempt",r),aliases());assertThrows(AssertionError.class,()->engine.check(zero,Map.of("after",StepResult.missing("after","independent DB unavailable").toJson()),aliases()));
    }
    @Test void queryModelWriteCallIsNotHiddenByServerDenial() throws Exception {
        var a=assertion("C3","model-query-01-1","no-write-tool-dispatchQuantity");var r=observed("{}","{}","{}");((ObjectNode)r.path("data")).set("toolTranscript",Json.parse("{\"calls\":[]}"));engine.check(a,Map.of("query-agent",r),aliases());((ObjectNode)r.at("/data/toolTranscript")).set("calls",Json.parse("[{\"capabilityId\":\"dispatchQuantity\",\"serverOutcome\":\"FORBIDDEN\"}]"));assertThrows(AssertionError.class,()->engine.check(a,Map.of("query-agent",r),aliases()));
    }
    @Test void assignedFeatureAndAllNamedObservationsAreLinked() throws Exception {
        var linked=new HashSet<String>();var cases=List.of("T08","C3","V4","V6","V7");var prep=new PreparationValidator(root);int subs=0;
        for(String c:cases) {Path p=root.resolve("verification/cases/"+c+"/case.json");JsonNode n=Json.read(p);assertEquals(List.of(),prep.feature(p,n));for(JsonNode s:n.path("subcases")){subs++;for(JsonNode a:s.path("assertions"))for(JsonNode o:a.path("oracleRef").path("observationNames"))linked.add(a.path("oracleRef").path("oracleId").asText()+"/"+o.asText());}}
        var required=new HashSet<String>();for(JsonNode o:Json.read(root.resolve("verification/requirements/mandatory-oracles.json")).path("oracles"))if(cases.contains(o.path("caseId").asText()))for(JsonNode n:o.path("expectedObservations"))required.add(o.path("oracleId").asText()+"/"+n.path("name").asText());
        assertEquals(54,required.size());assertEquals(required,linked);assertEquals(437,subs);
    }
}
