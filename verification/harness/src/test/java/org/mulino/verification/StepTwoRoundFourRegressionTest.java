package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Step 2 re-review round 4: counterexamples for the cross-owner residuals closed in step2r/round4. */
final class StepTwoRoundFourRegressionTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private static final List<String> CASES=new ArrayList<>();
    static {
        for(int i=1;i<=26;i++) CASES.add(String.format("T%02d",i));
        for(int i=1;i<=5;i++) CASES.add("C"+i);
        for(int i=1;i<=8;i++) CASES.add("V"+i);
        CASES.add("E1");CASES.add("E2");
    }
    private JsonNode caseJson(String id) throws Exception {return Json.read(root.resolve("verification/cases/"+id+"/case.json"));}

    // Item 1: the vocabulary check is a fail-closed case-asset check whose pending entries are recorded as KNOWN_OPEN.
    @Test void vocabularyCheckIsAPreparationAssetCheck() {
        var check=PreparationAssetChecks.CHECKS.stream().filter(c->c.name().equals("vocabulary")).findFirst().orElseThrow();
        assertEquals("verification/cases/check_vocabulary.py",check.script());
        assertEquals(List.of("verification/cases/check_vocabulary.py","--check"),check.argv());
    }

    // Item 2: audit assertions must use the published logical audit rows.
    private static ObjectNode auditCase(String source,String op,JsonNode expected) {
        ObjectNode a=Json.object().put("id","a").put("op",op);a.set("source",Json.parse(source));a.set("expected",expected);
        ObjectNode sub=Json.object().put("id","s");
        sub.set("actions",Json.parse("[{\"id\":\"db\",\"kind\":\"observe\",\"observation\":{\"sources\":[\"audit\",\"queryAudit\"]}}]"));
        sub.set("assertions",Json.array().add(a));
        ObjectNode c=Json.object().put("caseId","X01");c.set("subcases",Json.array().add(sub));return c;
    }
    @Test void auditAssertionsOutsideThePublishedContractAreRejected() throws Exception {
        ContractValidator v=new ContractValidator(root);
        for(String id:CASES) assertEquals(List.of(),v.auditFieldProblems(caseJson(id)),id);
        assertEquals(List.of(),v.auditFieldProblems(auditCase("{\"actionId\":\"db\",\"pointer\":\"/data/rawRows/audit\",\"where\":{\"commandIdempotencyKey\":\"k\"},\"field\":\"errorCode\"}","equals",Json.parse("\"FORBIDDEN\""))));
        Map<String,ObjectNode> bad=new LinkedHashMap<>();
        bad.put("retired field",auditCase("{\"actionId\":\"db\",\"pointer\":\"/data/rawRows/audit\",\"where\":{\"commandKey\":\"k\"}}","count",Json.parse("1")));
        bad.put("CONDITIONAL filter",auditCase("{\"actionId\":\"db\",\"pointer\":\"/data/rawRows/audit\",\"where\":{\"errorCode\":\"FORBIDDEN\"}}","count",Json.parse("1")));
        bad.put("unknown projection",auditCase("{\"actionId\":\"db\",\"pointer\":\"/data/rawRows/queryAudit\",\"field\":[\"queryAuditId\",\"result\"]}","relationSet",Json.parse("[]")));
        bad.put("unknown source",auditCase("{\"actionId\":\"db\",\"pointer\":\"/data/rawRows/denialAudit\"}","count",Json.parse("0")));
        bad.put("indexed field",auditCase("{\"actionId\":\"db\",\"pointer\":\"/data/rawRows/audit/0/action\"}","notEquals",Json.parse("\"x\"")));
        bad.put("fieldsPresent",auditCase("{\"actionId\":\"db\",\"pointer\":\"/data/rawRows/audit\"}","fieldsPresent",Json.parse("[\"auditId\",\"kind\"]")));
        for(var e:bad.entrySet()) assertEquals(1,v.auditFieldProblems(e.getValue()).size(),e.getKey()+": "+v.auditFieldProblems(e.getValue()));
        ObjectNode observe=auditCase("{\"actionId\":\"db\",\"pointer\":\"/data/rawRows/audit\"}","count",Json.parse("0"));
        ((ArrayNode)observe.path("subcases").get(0).path("actions").get(0).path("observation").path("sources")).add("denialAudit");
        assertTrue(v.auditFieldProblems(observe).get(0).contains("observe source denialAudit"));
    }

    // Item 3: runtimeProfile tick semantics and the autonomous-loop pattern.
    private static ObjectNode sub(JsonNode c,String id) {for(JsonNode s:c.path("subcases")) if(s.path("id").asText().equals(id)) return (ObjectNode)s;throw new AssertionError(id);}
    private static ObjectNode only(JsonNode c,ObjectNode s) {ObjectNode copy=((ObjectNode)c).deepCopy();copy.set("subcases",Json.array().add(s));return copy;}
    @Test void runtimeProfileAndAutonomousLoopPatternAreValidated() throws Exception {
        ContractValidator v=new ContractValidator(root);
        for(String id:CASES) assertEquals(List.of(),v.runtimeProfileProblems(caseJson(id)),id);
        JsonNode t26=caseJson("T26");
        // Passive watcher against a harness-tick fixture.
        ObjectNode controlled=sub(t26,"due-wait-autonomous-loop").deepCopy();controlled.put("fixtureRef","verification/cases/T26/fixtures/due-wait-db-rediscovery.json");
        assertTrue(v.runtimeProfileProblems(only(t26,controlled)).stream().anyMatch(p->p.contains("controlledTicks=false")));
        // Harness tick against an autonomous fixture.
        ObjectNode harness=sub(t26,"due-wait-db-rediscovery").deepCopy();harness.put("fixtureRef","verification/cases/T26/fixtures/due-wait-autonomous-loop.json");
        assertTrue(v.runtimeProfileProblems(only(t26,harness)).stream().anyMatch(p->p.contains("controlledTicks=true")));
        // Watcher branch second.
        ObjectNode swapped=sub(t26,"due-wait-autonomous-loop").deepCopy();
        for(JsonNode a:swapped.path("actions")) if(a.path("kind").asText().equals("parallel")) {ArrayNode b=(ArrayNode)a.path("branches");JsonNode first=b.remove(0);b.add(first);}
        assertTrue(v.runtimeProfileProblems(only(t26,swapped)).stream().anyMatch(p->p.contains("branch 0")),v.runtimeProfileProblems(only(t26,swapped)).toString());
        // The loop process is already running when the watcher starts (started before the clock advance, never stopped).
        ObjectNode running=sub(t26,"due-wait-autonomous-loop").deepCopy();
        ArrayNode acts=(ArrayNode)running.path("actions");
        for(int i=0;i<acts.size();i++) if(acts.get(i).path("kind").asText().equals("parallel")) {
            ObjectNode early=acts.get(i).path("branches").get(1).path("actions").get(0).deepCopy();early.put("id","start-loop-early");acts.insert(i-1,early);break;
        }
        assertTrue(v.runtimeProfileProblems(only(t26,running)).stream().anyMatch(p->p.contains("already running")),v.runtimeProfileProblems(only(t26,running)).toString());
        // A watcher outside a parallel group.
        ObjectNode bare=sub(t26,"due-wait-autonomous-loop").deepCopy();ArrayNode list=(ArrayNode)bare.path("actions");
        for(int i=0;i<list.size();i++) if(list.get(i).path("kind").asText().equals("parallel")) {
            ArrayNode flat=Json.array();for(JsonNode b:list.get(i).path("branches")) b.path("actions").forEach(flat::add);
            list.remove(i);for(int k=flat.size()-1;k>=0;k--) list.insert(i,flat.get(k));break;
        }
        assertTrue(v.runtimeProfileProblems(only(t26,bare)).stream().anyMatch(p->p.contains("inside a parallel")));
    }
    @Test void fixtureSchemaAllowsOnlyTheTwoDefinedTickProfiles() throws Exception {
        ContractValidator v=new ContractValidator(root);
        ObjectNode fixture=(ObjectNode)Json.read(root.resolve("verification/cases/T26/fixtures/due-wait-autonomous-loop.json"));
        v.schema("contracts/acceptance-fixture.schema.json",fixture);
        ObjectNode profile=(ObjectNode)fixture.path("baseline").path("runtimeProfile");
        for(boolean[] pair:new boolean[][]{{true,false},{false,true}}) {
            profile.put("controlledTicks",pair[0]).put("pausedUntilTickControl",pair[1]);
            assertThrows(IllegalArgumentException.class,()->v.schema("contracts/acceptance-fixture.schema.json",fixture),Arrays.toString(pair));
        }
        profile.put("controlledTicks",false).put("pausedUntilTickControl",false).remove("pausedUntilTickControl");
        assertThrows(IllegalArgumentException.class,()->v.schema("contracts/acceptance-fixture.schema.json",fixture),"both flags are required");
    }
}
