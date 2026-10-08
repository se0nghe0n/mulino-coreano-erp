package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/** Step 2 closure review 4 (step2r round 7): counterexamples for the confirmed findings. Captured selftest ports only. */
final class StepTwoRoundSevenRegressionTest {
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
    private static ObjectNode slots(JsonNode s,String id) {return (ObjectNode)action(s,id).path("request").path("slots");}
    private String fixtureRef(JsonNode fixture) throws Exception {
        Path p=Files.createTempFile(root.resolve("verification/harness/target"),"round7-fixture-",".json");Json.write(p,fixture);return root.relativize(p).toString();
    }

    // P2: a direct receipt whose stock is reserved/dispatched/moved names an evidenced internal receiving custodian.
    private List<String> custodyProblems(String caseId,String subId,java.util.function.Consumer<ObjectNode> changeSub,java.util.function.Consumer<ObjectNode> changeFixture) throws Exception {
        JsonNode c=caseJson(caseId);ObjectNode s=sub(c,subId).deepCopy();changeSub.accept(s);
        ObjectNode fixture=(ObjectNode)Json.read(root.resolve(s.path("fixtureRef").asText()));changeFixture.accept(fixture);
        s.put("fixtureRef",fixtureRef(fixture));
        return new ContractValidator(root).receiptCustodyProblems(only(c,s));
    }
    private static ObjectNode content(ObjectNode fixture,String doc) {return (ObjectNode)fixture.path("aliases").path(doc).path("fixtureContent");}
    @Test void directReceiptsWhoseStockIsUsedNameAnEvidencedInternalReceivingCustodian() throws Exception {
        ContractValidator v=new ContractValidator(root);
        for(String id:CASES) assertEquals(List.of(),v.receiptCustodyProblems(caseJson(id)),id);
        java.util.function.Consumer<ObjectNode> none=x->{};
        assertEquals(List.of(),custodyProblems("E1","full-flow-quantities",none,none));
        Map<String,List<java.util.function.Consumer<ObjectNode>>> mutants=new LinkedHashMap<>();
        // The closure review 4 shape: no slot, so the received stock has no custodian and the reserve is SCOPE_INELIGIBLE.
        mutants.put("names no receivingCustodianId",List.of(s->slots(s,"receipt60").remove("receivingCustodianId"),none));
        mutants.put("is not an internal custodian",List.of(s->slots(s,"receipt60").set("receivingCustodianId",Json.parse("{\"$alias\":\"SUPPLIER\"}")),none));
        mutants.put("has no confirmReceipt role and grant",List.of(s->{slots(s,"receipt60").set("receivingCustodianId",Json.parse("{\"$alias\":\"observer\"}"));},
            f->{content(f,"warehouse-60").put("receivingCustodianAlias","observer");}));
        mutants.put("names receiving custodian",List.of(none,f->content(f,"warehouse-60").put("receivingCustodianAlias","qc")));
        mutants.put("no receipt original it cites",List.of(none,f->content(f,"warehouse-60").remove("receivingCustodianAlias")));
        mutants.put("is not the SHA-256",List.of(none,f->{for(JsonNode e:f.path("evidence")) if(e.path("alias").asText().equals("warehouse-60")) ((ObjectNode)e).put("sha256","0".repeat(64));}));
        mutants.put("grant scope placeAliases does not include",List.of(none,f->{ArrayNode places=(ArrayNode)f.at("/actors/receiver/grant/scope/placeAliases");for(int i=places.size()-1;i>=0;i--) if(places.get(i).asText().equals("W")) places.remove(i);}));
        // A retry of the same receipt with another custodian.
        mutants.put("carries different receivingCustodianId values",List.of(s->{ObjectNode retry=action(s,"receipt60").deepCopy();retry.put("id","receipt60-retry");
            ((ObjectNode)retry.path("request").path("slots")).set("receivingCustodianId",Json.parse("{\"$alias\":\"procurement\"}"));((ArrayNode)s.path("actions")).add(retry);},none));
        for(var m:mutants.entrySet()) {
            List<String> problems=custodyProblems("E1","full-flow-quantities",m.getValue().get(0),m.getValue().get(1));
            assertTrue(problems.stream().anyMatch(p->p.contains(m.getKey())),m.getKey()+": "+problems);
        }
        // T13: the move to W-alt and the reserve need the custody too; dropping the slot of receipt40 alone is caught.
        assertTrue(custodyProblems("T13","partial-excess-return-relocation",s->slots(s,"receipt40").remove("receivingCustodianId"),none).stream()
            .anyMatch(p->p.contains("receipt40") && p.contains("move moveQuantity")));
        // A transit receipt keeps the transit leaf's custodian and never carries the slot (C2 receive60 confirms A60, a TRANSIT
        // leaf since step2r round 8; StepTwoRoundEightRegressionTest pins the exact-leaf rule).
        assertTrue(custodyProblems("C2","cumulative-versus-state",s->slots(s,"receive60").set("receivingCustodianId",Json.parse("{\"$alias\":\"warehouse\"}")),none).stream()
            .anyMatch(p->p.contains("transit receipt")));
        // A direct receipt whose stock nobody reserves/dispatches/moves needs no slot (T07 two documents, one receipt).
        assertEquals(List.of(),v.receiptCustodyProblems(caseJson("T07")));
    }
    @Test void e1ProvesConfirmedInternalCustodyBeforeTheHolds() throws Exception {
        for(JsonNode s:caseJson("E1").path("subcases")) {
            if(s.path("id").asText().equals("receipt-custody-unverified")) continue; // the step2r round 8 declared custody negative
            List<String> ids=new ArrayList<>();for(JsonNode a:s.path("actions")) ids.add(a.path("id").asText());
            int custody=ids.indexOf("received-custody-db");
            assertTrue(custody>ids.indexOf("receipt40") && custody<ids.indexOf("qc-hold60"),s.path("id").asText()+": control sits after both receipts and before the first hold");
            JsonNode control=null;for(JsonNode a:s.path("assertions")) if(a.path("id").asText().equals("received-custody-control")) control=a;
            assertNotNull(control,s.path("id").asText());
            assertEquals("relationSet",control.path("op").asText());
            assertEquals(2,control.path("expected").size());
            for(JsonNode row:control.path("expected")) assertEquals("receiver",row.get(4).path("$alias").asText(),"custodian is the evidenced receiver, not the confirming procurement");
            JsonNode fixture=Json.read(root.resolve(s.path("fixtureRef").asText()));
            for(String doc:List.of("warehouse-60","warehouse-40")) assertEquals("receiver",fixture.at("/aliases/"+doc+"/fixtureContent/receivingCustodianAlias").asText());
            for(String r:List.of("receipt60","receipt40")) assertEquals("receiver",slots(s,r).at("/receivingCustodianId/$alias").asText());
        }
        ObjectNode t13=sub(caseJson("T13"),"partial-excess-return-relocation");
        for(String r:List.of("receipt60","receipt40","receipt5")) assertEquals("warehouse",slots(t13,r).at("/receivingCustodianId/value/$alias").asText());
    }

    // P3 (a): C1 unrecognized-place-kind differs from custody-not-sale only by the place kind.
    @Test void unrecognizedKindNegativeKeepsTheSaleSourcePlaces() throws Exception {
        JsonNode original=Json.read(root.resolve("verification/cases/C1/fixtures/custody-not-sale.json"));
        JsonNode negative=Json.read(root.resolve("verification/cases/C1/fixtures/unrecognized-place-kind.json"));
        List<String> expected=new ArrayList<>();for(JsonNode p:original.at("/baseline/policies/saleSourcePlaceAliases")) expected.add(p.asText());
        expected.add("W-UNRECOGNIZED");
        List<String> actual=new ArrayList<>();for(JsonNode p:negative.at("/baseline/policies/saleSourcePlaceAliases")) actual.add(p.asText());
        assertEquals(expected,actual);
        assertTrue(actual.containsAll(List.of("W","W2")));
    }

    // P3 (b): a NO_TASK passive observation lasts long enough after observeFrom (two natural ticks since step2r round 8) and
    // its extractor reads the scheduler rows after the watcher completed.
    HostObservationValidatorTest.Capture noTask(String commandStart,String commandEnd,Integer tick) throws Exception {return noTask(commandStart,commandEnd,tick,commandEnd);}
    HostObservationValidatorTest.Capture noTask(String commandStart,String commandEnd,Integer tick,String extractorStart) throws Exception {
        HostObservationValidatorTest.Capture c=new HostObservationValidatorTest().capture("sweepDue","sweepDue-natural-rows.json");
        ObjectNode rows=(ObjectNode)c.host().path("extractor").path("rawRows");
        ObjectNode e=(ObjectNode)rows.path("operationEvidence");e.remove(List.of("taskId","invocationHandle","submittedAt"));e.put("submissionStatus","NO_TASK");
        rows.set("schedulerSubmissions",Json.array());
        // step2r round 9: the scheduler's own record of a natural cycle that ran entirely inside [observeFrom, completedAt].
        ObjectNode cycle=Json.object();cycle.put("schedulerId",e.path("schedulerId").asText()).put("sweepId","synthetic-natural-cycle")
            .put("startedAt",commandStart).put("completedAt",commandEnd).put("startedBy","SCHEDULER_LOOP");
        rows.set("schedulerCycles",Json.array().add(cycle));
        ((ObjectNode)rows.path("command")).put("startedAt",commandStart).put("completedAt",commandEnd);
        c.host().set("command",rows.path("command").deepCopy());c.host().set("operationEvidence",e.deepCopy());
        ((ObjectNode)c.host().path("extractor").path("command")).put("startedAt",extractorStart).put("completedAt",extractorStart);
        ObjectNode params=(ObjectNode)c.control().path("parameters");params.put("trigger","OBSERVE_NEXT_NATURAL_TICK").put("observationWindowSeconds",30).put("triggeredBy","SCHEDULER_LOOP");
        params.put("observeFrom",commandStart);if(tick!=null) params.put("naturalTickSeconds",tick);
        c.host().set("requestedInputs",params.deepCopy());
        Path copy=Files.createTempFile(root.resolve("verification/harness/target"),"round7-rows-",".json");Json.write(copy,rows);
        String refPath=root.relativize(copy).toString();((ObjectNode)c.host().path("extractor")).put("rawRowsArtifactRef",refPath);
        ObjectNode a=Json.object();a.put("path",refPath).put("sha256",Json.sha256(copy)).put("sizeBytes",Files.size(copy)).put("completeness","COMPLETE");a.set("scope",Json.parse("{\"workspace\":\"host-selftest\"}"));
        ((ArrayNode)c.host().path("extractor").path("inputArtifacts")).set(0,a);
        List<String> refs=new ArrayList<>(c.result().artifactRefs());refs.add(refPath);
        return new HostObservationValidatorTest.Capture(c.control(),c.host(),new StepResult(c.result().actionId(),c.result().driverStatus(),c.result().data(),null,c.result().reason(),c.result().provenance(),refs));
    }
    String rejection(HostObservationValidatorTest.Capture c) {
        return assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(new ContractValidator(root),c.control(),c.result(),false,null)).getMessage();
    }
    @Test void noTaskObservationCoversAtLeastOneNaturalTick() throws Exception {
        ContractValidator v=new ContractValidator(root);
        HostObservationValidatorTest.Capture full=noTask("2026-10-07T00:00:00Z","2026-10-07T00:00:02Z",1);
        HostObservationValidator.validate(v,full.control(),full.result(),false,null);
        // The closure review 4 shape: the watcher returned 0.5 s after observeFrom with tickSeconds=1.
        assertTrue(rejection(noTask("2026-10-07T00:00:00Z","2026-10-07T00:00:00.500Z",1)).contains("ended before 2 natural ticks"));
        assertTrue(rejection(noTask("2026-10-07T00:00:00Z","2026-10-07T00:00:03Z",null)).contains("naturalTickSeconds"));
        assertTrue(rejection(noTask("2026-10-07T00:00:00Z","2026-10-07T00:00:03Z",31)).contains("naturalTickSeconds must be"));
        // Prepare: a case does not author naturalTickSeconds, and passive subcases declare a tick that fits the window.
        JsonNode t26=caseJson("T26");ObjectNode loop=sub(t26,"lot-expiry-autonomous-loop").deepCopy();
        assertEquals(List.of(),v.runtimeProfileProblems(only(t26,loop)));
        ObjectNode authored=loop.deepCopy();
        for(JsonNode a:authored.path("actions")) if(a.path("kind").asText().equals("parallel")) ((ObjectNode)a.path("branches").get(0).path("actions").get(0).path("control").path("parameters")).put("naturalTickSeconds",1);
        assertTrue(v.runtimeProfileProblems(only(t26,authored)).stream().anyMatch(p->p.contains("naturalTickSeconds")));
        ObjectNode fixture=(ObjectNode)Json.read(root.resolve(loop.path("fixtureRef").asText()));((ObjectNode)fixture.at("/baseline/runtimeProfile")).put("tickSeconds",45);
        ObjectNode slow=loop.deepCopy();slow.put("fixtureRef",fixtureRef(fixture));
        assertTrue(v.runtimeProfileProblems(only(t26,slow)).stream().anyMatch(p->p.contains("tickSeconds")));
    }
    @Test void caseRunnerResolvesTheFixtureTickForAPassiveWatcher() throws Exception {
        Instant now=Instant.now();
        HostObservationValidatorTest.Capture w=noTask(now.plusSeconds(1).toString(),now.plusSeconds(3).toString(),null);
        ((ObjectNode)w.control().path("parameters")).remove(List.of("observeFrom","naturalTickSeconds"));
        JsonNode c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
        ObjectNode s=(ObjectNode)c.path("subcases").get(0);ObjectNode watch=Json.object();watch.put("id","watch").put("kind","control");watch.set("control",w.control());watch.set("evidenceRefs",Json.parse("[\"CAPTURED_SELFTEST\"]"));
        s.set("actions",Json.array().add(watch));
        s.put("fixtureRef",sub(caseJson("T26"),"lot-expiry-autonomous-loop").path("fixtureRef").asText());
        ObjectNode assertion=(ObjectNode)s.path("assertions").get(0).deepCopy();assertion.put("id","acked").put("op","equals");assertion.remove(List.of("unit","unitSource"));
        assertion.set("source",Json.parse("{\"actionId\":\"watch\",\"pointer\":\"/data/acknowledged\"}"));assertion.set("expected",Json.MAPPER.valueToTree(true));s.set("assertions",Json.array().add(assertion));
        Path file=Files.createTempFile(root.resolve("verification/harness/target"),"round7-",".json");Json.write(file,c);
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
        assertNotNull(seen.get(),"the watcher was dispatched");
        assertEquals(1,seen.get().path("parameters").path("naturalTickSeconds").asInt(),"the fixture runtimeProfile tickSeconds reaches the watcher request");
        assertTrue(seen.get().path("parameters").path("observeFrom").isTextual());
    }

    // P3 (c): the 406 rule compares media types; it is not an exact string match.
    @Test void acceptListsBothMediaTypesInAnyFormButNotAsRefusedOrWildcardRanges() throws Exception {
        for(String ok:List.of("application/json, text/event-stream","text/event-stream, application/json","application/json;q=0.9, text/event-stream;q=0.5",
                "Application/JSON,Text/Event-Stream","application/json, text/event-stream, text/plain","text/event-stream ; charset=utf-8 , application/json"))
            assertTrue(ContractValidator.acceptsMcp(ok),ok);
        for(String bad:Arrays.asList(null,"","application/json","text/event-stream","*/*","application/*, text/*","application/json;q=0, text/event-stream","application/json, text/event-stream;q=0.000","application/jsonx, text/event-stream"))
            assertFalse(ContractValidator.acceptsMcp(bad),String.valueOf(bad));
        // A reordered Accept on a non-negative wire request is no longer a preparation problem.
        ContractValidator v=new ContractValidator(root);JsonNode t20=caseJson("T20");
        ObjectNode reordered=sub(t20,"wire-discover").deepCopy();
        for(JsonNode a:reordered.path("actions")) if(a.path("route").asText().equals("wire") && a.path("request").path("headers").has("Accept")) ((ObjectNode)a.path("request").path("headers")).put("Accept","text/event-stream, application/json");
        assertEquals(List.of(),v.wireTransportProblems(only(t20,reordered)));
        ObjectNode refused=sub(t20,"wire-discover").deepCopy();
        for(JsonNode a:refused.path("actions")) if(a.path("route").asText().equals("wire") && a.path("request").path("headers").has("Accept")) ((ObjectNode)a.path("request").path("headers")).put("Accept","application/json, text/event-stream;q=0");
        assertTrue(v.wireTransportProblems(only(t20,refused)).stream().anyMatch(p->p.contains("406")));
    }
}
