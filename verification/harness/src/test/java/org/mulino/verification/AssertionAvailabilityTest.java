package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Availability precedence is separate from malformed EXECUTED observations and fixed oracle FAIL. */
public final class AssertionAvailabilityTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private static final String ARTIFACT="verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/canned-observations.json";
    private static StepResult executed(String id,JsonNode data,JsonNode response) {
        ObjectNode p=Json.object();p.put("adapter","availability-captured-selftest").put("adapterVersion","1.0").put("buildVersion","SELFTEST").putNull("authenticatedActor").put("source","CANNED_CONTRACT_SELFTEST").put("independent",false).put("scopeComplete",true).putNull("sourceQuery").putNull("snapshot");
        return new StepResult(id,StepResult.DriverStatus.EXECUTED,data,response,"CAPTURED_SELFTEST only",p,List.of(ARTIFACT));
    }
    private JsonNode assertion(String expected) {return Json.parse("{\"op\":\"equals\",\"source\":{\"actionId\":\"observed\",\"pointer\":\"/response/workId\"},\"expected\":"+expected+",\"scope\":{\"organizationId\":{\"$alias\":\"ORG\"}}}");}
    @Test void unavailablePrimaryOrResultSourcePrecedesAliasResolution() {
        var engine=new AssertionEngine();var a=assertion("{\"$alias\":\"A\"}");
        var missing=new HashMap<String,JsonNode>();missing.put("observed",StepResult.missing("observed","NOT_IMPLEMENTED: observer absent").toJson());
        assertTrue(assertThrows(AssertionError.class,()->engine.check(a,missing)).getMessage().contains("NOT_IMPLEMENTED"));
        var results=new HashMap<String,JsonNode>();results.put("observed",executed("observed",Json.object(),Json.parse("{\"workId\":\"actual-work\"}")).toJson());results.put("created",StepResult.missing("created","NOT_IMPLEMENTED: command absent").toJson());
        JsonNode ref=assertion("{\"$result\":{\"actionId\":\"created\",\"pointer\":\"/response/workId\"}}");
        assertTrue(assertThrows(AssertionError.class,()->engine.check(ref,results)).getMessage().contains("NOT_IMPLEMENTED"));
    }
    @Test void executedBadAliasPointerOrTypeRemainRealErrors() {
        var engine=new AssertionEngine();var results=new HashMap<String,JsonNode>();results.put("observed",executed("observed",Json.object(),Json.parse("{\"workId\":\"actual-work\"}")).toJson());
        assertThrows(IllegalArgumentException.class,()->engine.check(assertion("{\"$alias\":\"WRONG\"}"),results,Json.parse("{\"ORG\":\"actual-org\"}")));
        var missingPointer=assertion("\"fixed-work\"");((ObjectNode)missingPointer.path("source")).put("pointer","/response/unknownId");
        assertTrue(assertThrows(AssertionError.class,()->engine.check(missingPointer,results,Json.parse("{\"ORG\":\"actual-org\"}"))).getMessage().contains("Missing/null"));
        ((ObjectNode)results.get("observed").path("response")).put("workId",false);
        assertThrows(IllegalArgumentException.class,()->engine.check(assertion("{\"$result\":{\"actionId\":\"observed\",\"pointer\":\"/response/workId\"}}"),results,Json.parse("{\"ORG\":\"actual-org\"}")));
    }
    private Path preparedCase() throws Exception {
        JsonNode c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));ObjectNode sub=(ObjectNode)c.path("subcases").get(0);
        sub.set("actions",Json.parse("[{\"id\":\"install\",\"kind\":\"installFixture\",\"evidenceRefs\":[\"fixture\"]},{\"id\":\"observed\",\"kind\":\"query\",\"actorRef\":\"qc\",\"route\":\"api\",\"capabilityId\":\"getWork\",\"request\":{},\"evidenceRefs\":[\"query\"]}]"));
        ObjectNode fixed=(ObjectNode)sub.path("assertions").get(0).deepCopy();fixed.put("id","fixed-quantity").put("op","equals");fixed.remove(List.of("unit","unitSource"));fixed.set("scope",Json.parse("{\"test\":\"fixed-oracle\"}"));fixed.set("source",Json.parse("{\"actionId\":\"observed\",\"pointer\":\"/response/quantity\"}"));fixed.put("expected",0);
        ObjectNode alias=fixed.deepCopy();alias.put("id","actual-id");alias.set("source",Json.parse("{\"actionId\":\"observed\",\"pointer\":\"/response/workId\"}"));alias.set("expected",Json.parse("{\"$alias\":\"WORK\"}"));
        sub.set("assertions",Json.array().add(fixed).add(alias));Path p=Files.createTempFile(root.resolve("verification/harness/target"),"assertion-availability-",".json");Json.write(p,c);return p;
    }
    private static final class Captures implements AcceptanceDriver {
        private final Map<String,StepResult> results;Captures(Map<String,StepResult> results){this.results=results;}
        private StepResult result(String id){return results.getOrDefault(id,StepResult.missing(id,"NOT_IMPLEMENTED: capture absent"));}
        public Set<String> availableAdapters(){return Set.of("CAPTURED_SELFTEST_ONLY");}
        public StepResult installFixture(String id,JsonNode f){return result(id);}public StepResult invoke(String id,String route,JsonNode actor,String cap,JsonNode q){return result(id);}public StepResult query(String id,String route,JsonNode actor,String cap,JsonNode q){return result(id);}public StepResult observe(String id,JsonNode q){return result(id);}public StepResult control(String id,JsonNode q){return result(id);}public StepResult start(String id,String route,JsonNode actor,String cap,JsonNode q){return result(id);}public StepResult await(String id,JsonNode h,int t){return result(id);}
    }
    private CaseRunner runner(Path p,AcceptanceDriver driver) throws Exception {return new CaseRunner(new ContractValidator(root),driver,new AgentRunner.Scripted(),p,"hold-preserves-physical");}
    @Test void missingFixtureAliasIsNotRunButConfirmedIndependentViolationRemainsFail() throws Exception {
        Path p=preparedCase();var output=executed("observed",Json.object(),Json.parse("{\"quantity\":1,\"workId\":\"actual-work\"}"));CaseRunner r=runner(p,new Captures(Map.of("observed",output)));
        assertEquals("FAIL",r.run(false));var evidence=r.evidence("FAIL","SELFTEST");assertEquals("FAIL",evidence.path("assertions").get(0).path("status").asText());assertEquals("NOT_RUN",evidence.path("assertions").get(1).path("status").asText());
        var clean=executed("observed",Json.object(),Json.parse("{\"quantity\":0,\"workId\":\"actual-work\"}"));assertEquals("NOT_RUN",runner(p,new Captures(Map.of("observed",clean))).run(false));
        CaseRunner red=runner(p,new Captures(Map.of("observed",clean)));red.execute("install");red.execute("observed");assertTrue(assertThrows(AssertionError.class,()->red.assertId("actual-id")).getMessage().contains("NOT_IMPLEMENTED"));
        assertEquals("FAIL",runner(p,new UnimplementedDriver()).run(true));assertEquals("NOT_RUN",runner(p,new UnimplementedDriver()).run(false));
    }
    @Test void executedFixtureWithWrongAliasIsContractErrorRatherThanNotRun() throws Exception {
        var installed=executed("install",Json.parse("{\"aliasMap\":{\"OTHER\":\"actual-other\"},\"fixtureHash\":\"SELFTEST_HASH\"}"),null);
        var output=executed("observed",Json.object(),Json.parse("{\"quantity\":0,\"workId\":\"actual-work\"}"));
        assertThrows(IllegalArgumentException.class,()->runner(preparedCase(),new Captures(Map.of("install",installed,"observed",output))).run(false));
    }
}
