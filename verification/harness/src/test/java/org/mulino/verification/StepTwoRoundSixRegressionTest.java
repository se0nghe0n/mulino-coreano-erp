package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/** Step 2 closure review 3 (step2r round 6): counterexamples for the confirmed findings. Captured selftest ports only. */
final class StepTwoRoundSixRegressionTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private static final List<String> CASES=new ArrayList<>();
    static {
        for(int i=1;i<=26;i++) CASES.add(String.format("T%02d",i));
        for(int i=1;i<=5;i++) CASES.add("C"+i);
        for(int i=1;i<=8;i++) CASES.add("V"+i);
        CASES.add("E1");CASES.add("E2");
    }
    private JsonNode caseJson(String id) throws Exception {return Json.read(root.resolve("verification/cases/"+id+"/case.json"));}
    private static ObjectNode sub(JsonNode c,String id) {for(JsonNode s:c.path("subcases")) if(s.path("id").asText().equals(id)) return (ObjectNode)s;throw new AssertionError(id);}
    private static ObjectNode only(JsonNode c,ObjectNode s) {ObjectNode copy=((ObjectNode)c).deepCopy();copy.set("subcases",Json.array().add(s));return copy;}
    private static ObjectNode action(JsonNode s,String id) {for(JsonNode a:s.path("actions")) if(a.path("id").asText().equals(id)) return (ObjectNode)a;throw new AssertionError(id);}

    // P1 index 0/5: every subcase fixture Place has a contract kind and internal storage stock an internal custodian.
    private String fixtureRef(JsonNode fixture) throws Exception {
        Path p=Files.createTempFile(root.resolve("verification/harness/target"),"round6-fixture-",".json");Json.write(p,fixture);return root.relativize(p).toString();
    }
    private List<String> placeProblems(java.util.function.Consumer<ObjectNode> change) throws Exception {
        JsonNode c1=caseJson("C1");ObjectNode s=sub(c1,"custody-not-sale").deepCopy();
        ObjectNode fixture=(ObjectNode)Json.read(root.resolve(s.path("fixtureRef").asText()));change.accept(fixture);
        s.put("fixtureRef",fixtureRef(fixture));
        return new ContractValidator(root).placeKindProblems(only(c1,s));
    }
    private static ObjectNode alias(ObjectNode fixture,String name) {return (ObjectNode)fixture.path("aliases").path(name);}
    @Test void everyFixturePlaceHasAContractKindAndInternalStorageStockAnInternalCustodian() throws Exception {
        ContractValidator v=new ContractValidator(root);
        for(String id:CASES) assertEquals(List.of(),v.placeKindProblems(caseJson(id)),id);
        assertEquals(List.of(),placeProblems(f->{}));
        Map<String,java.util.function.Consumer<ObjectNode>> mutants=new LinkedHashMap<>();
        mutants.put("has no kind",f->alias(f,"W").remove("kind"));
        mutants.put("is not in the fixture place vocabulary",f->alias(f,"W").put("kind","INTERNAL_WAREHOUSE"));
        mutants.put("is not in the fixture place vocabulary ",f->alias(f,"PORT").put("kind","PORT"));
        mutants.put("needs an internal custodian",f->alias(f,"CON40").remove("custodianAlias"));
        mutants.put("needs an internal custodian ",f->{ObjectNode o=Json.object();o.put("type","Organization").put("kind","WAREHOUSE");((ObjectNode)f.path("aliases")).set("CUSTODIAN",o);});
        mutants.put("is not a fixture alias",f->alias(f,"CON40").put("custodianAlias","nobody"));
        mutants.put("kindControl must be",f->alias(f,"W2").put("kindControl","UNRECOGNIZED_PLACE_KIND"));
        mutants.put("kindControl must be ",f->alias(f,"W2").put("kind","EXTERNAL_DEPOT").put("kindControl","UNRECOGNIZED_PLACE_KIND"));
        mutants.put("differs from the alias kind",f->{ObjectNode p=Json.object();p.put("alias","W").put("kind","INTERNAL_WAREHOUSE");((ObjectNode)f.path("baseline")).set("places",Json.array().add(p));});
        for(var m:mutants.entrySet()) {
            List<String> problems=placeProblems(m.getValue());
            assertTrue(problems.stream().anyMatch(p->p.contains(m.getKey().strip())),m.getKey()+": "+problems);
        }
        // The declared negative control is accepted and its stock needs no internal-storage custody claim.
        assertEquals(List.of(),placeProblems(f->{alias(f,"W2").put("kind","WAREHOUSE").put("kindControl","UNRECOGNIZED_PLACE_KIND");}));
    }
    @Test void theUnrecognizedKindNegativeAndThePositiveControlsAreAuthored() throws Exception {
        JsonNode c1=caseJson("C1");ObjectNode neg=sub(c1,"unrecognized-place-kind");
        JsonNode fixture=Json.read(root.resolve(neg.path("fixtureRef").asText()));
        assertEquals("UNRECOGNIZED_PLACE_KIND",fixture.at("/aliases/W-UNRECOGNIZED/kindControl").asText());
        assertEquals("INTERNAL_STORAGE",fixture.at("/aliases/W/kind").asText());
        Map<String,String> expected=new HashMap<>();for(JsonNode a:neg.path("assertions")) expected.put(a.path("id").asText(),a.path("expected").toString());
        assertEquals("\"0\"",expected.get("unrecognized-kind-eligible-0"));assertEquals("\"UNKNOWN\"",expected.get("unrecognized-kind-unknown"));
        assertEquals("\"40\"",expected.get("known-kind-eligible-40"));assertEquals("\"40\"",expected.get("item-eligible-40"));
        ObjectNode t16=sub(caseJson("T16"),"provisional-holds");boolean control=false;
        for(JsonNode a:t16.path("assertions")) control|=a.path("id").asText().equals("confirmed-eligible-control") && a.path("expected").asText().equals("60");
        assertTrue(control,"T16 provisional-holds has a positive eligibility control");
        for(JsonNode s:caseJson("T17").path("subcases")) if(s.path("id").asText().startsWith("eligibility-")) {
            boolean found=false;for(JsonNode a:s.path("assertions")) found|=a.path("id").asText().equals("eligibility-positive-control");
            assertTrue(found,s.path("id").asText());
        }
    }

    // P1 index 1: Streamable HTTP requests carry the required Accept and no unallowlisted Origin unless they are the negative.
    @Test void wireRequestsCarryAcceptAndNoOriginUnlessPinnedAsTheTransportNegative() throws Exception {
        ContractValidator v=new ContractValidator(root);
        for(String id:CASES) assertEquals(List.of(),v.wireTransportProblems(caseJson(id)),id);
        JsonNode t20=caseJson("T20");
        ObjectNode accept=sub(t20,"wire-missing-accept");
        assertFalse(action(accept,"wire").path("request").path("headers").has("Accept"));
        ObjectNode origin=sub(t20,"wire-bad-origin");
        assertTrue(action(origin,"wire").path("request").path("headers").has("Origin"));
        // The round 5 request shape: an Origin no contract allowlists on a subcase that expects discovery to succeed.
        ObjectNode discover=sub(t20,"wire-discover").deepCopy();((ObjectNode)action(discover,"wire").path("request").path("headers")).put("Origin","https://isolated-client.example.invalid");
        assertTrue(v.wireTransportProblems(only(t20,discover)).stream().anyMatch(p->p.contains("Origin")));
        ObjectNode noAccept=sub(t20,"wire-discover").deepCopy();((ObjectNode)action(noAccept,"wire").path("request").path("headers")).remove("Accept");
        assertTrue(v.wireTransportProblems(only(t20,noAccept)).stream().anyMatch(p->p.contains("406")));
        // The negative must pin its own transport status.
        ObjectNode unpinned=accept.deepCopy();for(JsonNode a:unpinned.path("assertions")) if(a.path("source").path("pointer").asText().equals("/response/httpStatus")) ((ObjectNode)a).put("expected",400);
        assertEquals(1,v.wireTransportProblems(only(t20,unpinned)).size());
        ObjectNode both=accept.deepCopy();((ObjectNode)action(both,"wire").path("request").path("headers")).put("Origin","https://untrusted.example.invalid");
        assertTrue(v.wireTransportProblems(only(t20,both)).stream().anyMatch(p->p.contains("ambiguous")));
        // V4's MCP requests are well-formed too.
        for(String id:List.of("surface-discover","surface-tools")) assertEquals("application/json, text/event-stream",action(sub(caseJson("V4"),"exposed-write-surface"),id).at("/request/headers/Accept").asText());
    }

    // P2 index 2: QUERY tools/actions are exempt from write probes like FUNCTION; COMMAND/RECORD/unknown/writeCapable items are not.
    private HostObservationValidatorTest.Capture surface(java.util.function.Consumer<HostObservationValidatorTest.Capture> change) throws Exception {
        HostObservationValidatorTest.Capture c=new HostObservationValidatorTest().capture("enumerateWriteSurface");change.accept(c);
        c.host().set("requestedInputs",c.control().path("parameters").deepCopy());
        ObjectNode rows=(ObjectNode)c.host().path("extractor").path("rawRows");
        Path copy=Files.createTempFile(root.resolve("verification/harness/target"),"round6-surface-",".json");Json.write(copy,rows);
        String refPath=root.relativize(copy).toString();((ObjectNode)c.host().path("extractor")).put("rawRowsArtifactRef",refPath);
        ObjectNode a=Json.object();a.put("path",refPath).put("sha256",Json.sha256(copy)).put("sizeBytes",Files.size(copy)).put("completeness","COMPLETE");a.set("scope",Json.parse("{\"workspace\":\"host-selftest\"}"));
        ((ArrayNode)c.host().path("extractor").path("inputArtifacts")).set(0,a);
        List<String> refs=new ArrayList<>(c.result().artifactRefs());refs.add(refPath);
        return new HostObservationValidatorTest.Capture(c.control(),c.host(),new StepResult(c.result().actionId(),c.result().driverStatus(),c.result().data(),null,c.result().reason(),c.result().provenance(),refs));
    }
    private static ObjectNode rows(HostObservationValidatorTest.Capture c) {return (ObjectNode)c.host().path("extractor").path("rawRows");}
    private static void addItem(HostObservationValidatorTest.Capture c,String surface,String itemId,String kind,String capability,boolean writable,boolean allowlisted) {
        ObjectNode item=Json.object();item.put("surface",surface).put("itemId",itemId).put("kind",kind).put("writeCapable",writable);
        if(capability==null) item.putNull("capabilityId"); else item.put("capabilityId",capability);item.put("allowlisted",allowlisted);
        ((ArrayNode)rows(c).path("surfaceItems")).add(item);
        for(JsonNode s:rows(c).path("surfaces")) if(s.path("surface").asText().equals(surface)) ((ObjectNode)s).put("itemCount",s.path("itemCount").asInt()+1);
    }
    private void accepts(java.util.function.Consumer<HostObservationValidatorTest.Capture> change) throws Exception {
        HostObservationValidatorTest.Capture c=surface(change);HostObservationValidator.validate(new ContractValidator(root),c.control(),c.result());
    }
    private String rejects(java.util.function.Consumer<HostObservationValidatorTest.Capture> change) throws Exception {
        HostObservationValidatorTest.Capture c=surface(change);
        return assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(new ContractValidator(root),c.control(),c.result())).getMessage();
    }
    @Test void queryToolsAndActionsNeedNoWriteProbeButCommandsRecordsAndUnknownItemsDo() throws Exception {
        // A READ actor's getInventory/evaluateEligibility call is a read: no probe row and the coverage counts stay 1/1.
        accepts(c->{addItem(c,"MCP_TOOLS_LIST","getInventory","TOOL","getInventory",false,true);addItem(c,"ODATA_METADATA","evaluateEligibility","UNBOUND_ACTION","evaluateEligibility",false,true);});
        accepts(c->addItem(c,"ODATA_METADATA","OntologyService.getInventory","BOUND_ACTION","getInventory",false,true));
        // The other direction: command and record items, an item that borrows a QUERY id under another name, a generic
        // dispatcher without a capability, and a QUERY item claimed writeCapable all still need their probes.
        assertTrue(rejects(c->addItem(c,"MCP_TOOLS_LIST","reserveQuantity","TOOL","reserveQuantity",true,true)).contains("never probed"));
        assertTrue(rejects(c->addItem(c,"MCP_TOOLS_LIST","recordStocktake","TOOL","recordStocktake",false,true)).contains("never probed"));
        assertTrue(rejects(c->addItem(c,"MCP_TOOLS_LIST","dispatchQuantity","TOOL","getInventory",false,true)).contains("never probed"));
        assertTrue(rejects(c->addItem(c,"ODATA_METADATA","query","UNBOUND_ACTION",null,false,false)).contains("never probed"));
        assertTrue(rejects(c->addItem(c,"MCP_TOOLS_LIST","getInventory","TOOL","getInventory",true,true)).contains("never probed"));
        // An exempt item still cannot hide behind an under-reported applicability count.
        assertTrue(rejects(c->{addItem(c,"MCP_TOOLS_LIST","getInventory","TOOL","getInventory",false,true);((ObjectNode)rows(c).path("probeCoverage").get(1)).put("applicableTargets",2);}).contains("applicability policy"));
        assertTrue(HostObservationValidator.queryExempt(Json.parse("{\"kind\":\"TOOL\",\"itemId\":\"getObject\",\"capabilityId\":\"getObject\",\"writeCapable\":false}"),Map.of("getObject","QUERY")));
        assertFalse(HostObservationValidator.queryExempt(Json.parse("{\"kind\":\"ENTITY_SET\",\"itemId\":\"getObject\",\"capabilityId\":\"getObject\",\"writeCapable\":false}"),Map.of("getObject","QUERY")));
    }

    // P3 index 3/6: the observation boundary reaches the watcher host adapter as observeFrom and anchors the window.
    private HostObservationValidatorTest.Capture watcher(String observeFrom,String commandStart,String commandEnd,String... submittedAt) throws Exception {
        HostObservationValidatorTest.Capture c=new HostObservationValidatorTest().capture("tickScheduler","tickScheduler-natural-rows.json");
        ObjectNode rows=(ObjectNode)c.host().path("extractor").path("rawRows");
        ArrayNode subs=(ArrayNode)rows.path("schedulerSubmissions");ObjectNode first=(ObjectNode)subs.get(0).deepCopy();subs.removeAll();
        for(int i=0;i<submittedAt.length;i++) {ObjectNode r=first.deepCopy();r.put("submittedAt",submittedAt[i]);if(i>0) r.put("taskId","task-"+i).put("tickId","tick-"+i).put("invocationHandle","handle-"+i);subs.add(r);}
        ((ObjectNode)rows.path("operationEvidence")).put("submittedAt",submittedAt[0]);
        ((ObjectNode)rows.path("command")).put("startedAt",commandStart).put("completedAt",commandEnd);
        c.host().set("command",rows.path("command").deepCopy());c.host().set("operationEvidence",rows.path("operationEvidence").deepCopy());
        ObjectNode params=(ObjectNode)c.control().path("parameters");params.put("trigger","OBSERVE_NEXT_NATURAL_TICK").put("observationWindowSeconds",30).put("triggeredBy","SCHEDULER_LOOP");
        if(observeFrom!=null) params.put("observeFrom",observeFrom);
        c.host().set("requestedInputs",params.deepCopy());
        Path copy=Files.createTempFile(root.resolve("verification/harness/target"),"round6-rows-",".json");Json.write(copy,rows);
        String refPath=root.relativize(copy).toString();((ObjectNode)c.host().path("extractor")).put("rawRowsArtifactRef",refPath);
        ObjectNode a=Json.object();a.put("path",refPath).put("sha256",Json.sha256(copy)).put("sizeBytes",Files.size(copy)).put("completeness","COMPLETE");a.set("scope",Json.parse("{\"workspace\":\"host-selftest\"}"));
        ((ArrayNode)c.host().path("extractor").path("inputArtifacts")).set(0,a);
        List<String> refs=new ArrayList<>(c.result().artifactRefs());refs.add(refPath);
        return new HostObservationValidatorTest.Capture(c.control(),c.host(),new StepResult(c.result().actionId(),c.result().driverStatus(),c.result().data(),null,c.result().reason(),c.result().provenance(),refs));
    }
    private String windowRejection(HostObservationValidatorTest.Capture c,Instant boundary) throws Exception {
        return assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(new ContractValidator(root),c.control(),c.result(),false,boundary)).getMessage();
    }
    @Test void theWatcherReceivesObserveFromAndTheWindowIsMeasuredFromIt() throws Exception {
        ContractValidator v=new ContractValidator(root);
        HostObservationValidator.validate(v,watcher("2026-10-07T00:00:00Z","2026-10-07T00:00:01Z","2026-10-07T00:00:03Z","2026-10-07T00:00:02Z").control(),
            watcher("2026-10-07T00:00:00Z","2026-10-07T00:00:01Z","2026-10-07T00:00:03Z","2026-10-07T00:00:02Z").result());
        assertTrue(windowRejection(watcher(null,"2026-10-07T00:00:01Z","2026-10-07T00:00:03Z","2026-10-07T00:00:02Z"),null).contains("observeFrom"));
        // A group watcher may run 30 s from its own start but not past observeFrom+30 (closure review 3, index 3).
        assertTrue(windowRejection(watcher("2026-10-07T00:00:00Z","2026-10-07T00:00:05Z","2026-10-07T00:00:32Z","2026-10-07T00:00:06Z"),Instant.parse("2026-10-07T00:00:00Z")).contains("completed after"));
        assertTrue(windowRejection(watcher("2026-10-07T00:00:00Z","2026-10-07T00:00:01Z","2026-10-07T00:00:03Z","2026-10-07T00:00:02Z"),Instant.parse("2026-10-07T00:00:00.500Z")).contains("differs from the harness observation boundary"));
        // Standalone repeat watcher: the first sweep's row before observeFrom is outside the window, so the extractor reads
        // only [observeFrom, observeFrom+window] instead of the unfiltered history (lot-expiry-autonomous-loop repeat-sweep).
        assertTrue(windowRejection(watcher("2026-10-07T00:00:10Z","2026-10-07T00:00:10Z","2026-10-07T00:00:13Z","2026-10-07T00:00:12Z","2026-10-07T00:00:02Z"),null).contains("outside the observation window"));
        HostObservationValidatorTest.Capture filtered=watcher("2026-10-07T00:00:10Z","2026-10-07T00:00:10Z","2026-10-07T00:00:13Z","2026-10-07T00:00:12Z");
        HostObservationValidator.validate(v,filtered.control(),filtered.result(),false,null);
    }
    @Test void caseRunnerResolvesObserveFromForAStandaloneWatcherAndPrepareRejectsAnAuthoredOne() throws Exception {
        Instant now=Instant.now();
        HostObservationValidatorTest.Capture w=watcher(null,now.plusSeconds(1).toString(),now.plusSeconds(3).toString(),now.plusSeconds(2).toString());
        ((ObjectNode)w.control().path("parameters")).remove("observeFrom");
        JsonNode c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
        ObjectNode s=(ObjectNode)c.path("subcases").get(0);ObjectNode watch=Json.object();watch.put("id","watch").put("kind","control");watch.set("control",w.control());watch.set("evidenceRefs",Json.parse("[\"CAPTURED_SELFTEST\"]"));
        s.set("actions",Json.array().add(watch));
        ObjectNode assertion=(ObjectNode)s.path("assertions").get(0).deepCopy();assertion.put("id","acked").put("op","equals");assertion.remove(List.of("unit","unitSource"));
        assertion.set("source",Json.parse("{\"actionId\":\"watch\",\"pointer\":\"/data/acknowledged\"}"));assertion.set("expected",Json.MAPPER.valueToTree(true));s.set("assertions",Json.array().add(assertion));
        Path file=Files.createTempFile(root.resolve("verification/harness/target"),"round6-",".json");Json.write(file,c);
        AtomicReference<JsonNode> seen=new AtomicReference<>();
        AcceptanceDriver port=new AcceptanceDriver() {
            public Set<String> availableAdapters(){return Set.of("CAPTURED_CONTRACT_SELFTEST_ONLY");}
            public StepResult installFixture(String id,JsonNode f){return StepResult.missing(id,"NOT_IMPLEMENTED");}
            public StepResult invoke(String id,String r,JsonNode a,String cap,JsonNode q){return StepResult.missing(id,"NOT_IMPLEMENTED");}
            public StepResult query(String id,String r,JsonNode a,String cap,JsonNode q){return StepResult.missing(id,"NOT_IMPLEMENTED");}
            public StepResult observe(String id,JsonNode q){return StepResult.missing(id,"NOT_IMPLEMENTED");}
            public StepResult start(String id,String r,JsonNode a,String cap,JsonNode q){return StepResult.missing(id,"NOT_IMPLEMENTED");}
            public StepResult await(String id,JsonNode h,int t){return StepResult.missing(id,"NOT_IMPLEMENTED");}
            public StepResult control(String id,JsonNode q){
                seen.set(q);StepResult r=w.result();ObjectNode data=r.data().deepCopy();((ObjectNode)data.path("hostObservation")).set("requestedInputs",q.path("parameters").deepCopy());
                return new StepResult(id,r.driverStatus(),data,r.response(),r.reason(),r.provenance(),r.artifactRefs());
            }
        };
        CaseRunner runner=CaseRunner.harnessSelftest(new ContractValidator(root),port,new AgentRunner.Scripted(),file,"hold-preserves-physical");
        runner.run(false);
        assertEquals("EXECUTED",runner.results().get("watch").path("driverStatus").asText());
        Instant from=Instant.parse(seen.get().path("parameters").path("observeFrom").asText());
        assertFalse(from.isBefore(now) || from.isAfter(now.plusSeconds(1)),from.toString());
        // A case never authors observeFrom.
        JsonNode t26=caseJson("T26");ObjectNode loop=sub(t26,"lot-expiry-autonomous-loop").deepCopy();
        for(JsonNode a:loop.path("actions")) if(a.path("kind").asText().equals("parallel")) ((ObjectNode)a.path("branches").get(0).path("actions").get(0).path("control").path("parameters")).put("observeFrom","2026-10-07T00:00:00Z");
        assertTrue(new ContractValidator(root).runtimeProfileProblems(only(t26,loop)).stream().anyMatch(p->p.contains("observeFrom")));
    }
}
