package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

/** Step 2 closure review 6 (step2r round 9): counterexamples for the confirmed findings. Preparation checks and captured selftest ports only. */
final class StepTwoRoundNineRegressionTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private static final List<String> CASES=new ArrayList<>();
    static {
        for(int i=1;i<=26;i++) CASES.add(String.format("T%02d",i));
        for(int i=1;i<=5;i++) CASES.add("C"+i);
        for(int i=1;i<=8;i++) CASES.add("V"+i);
        CASES.add("E1");CASES.add("E2");
    }
    private static final Consumer<ObjectNode> NONE=x->{};
    private static final String T13="partial-excess-return-relocation",T05="manager-disposition";
    private JsonNode caseJson(String id) throws Exception {return Json.read(root.resolve("verification/cases/"+id+"/case.json"));}
    private static ObjectNode sub(JsonNode c,String id) {for(JsonNode s:c.path("subcases")) if(s.path("id").asText().equals(id)) return (ObjectNode)s;throw new AssertionError(id);}
    private static ObjectNode only(JsonNode c,ObjectNode s) {ObjectNode copy=((ObjectNode)c).deepCopy();copy.set("subcases",Json.array().add(s));return copy;}
    private static ObjectNode action(JsonNode s,String id) {for(JsonNode a:s.path("actions")) if(a.path("id").asText().equals(id)) return (ObjectNode)a;throw new AssertionError(id);}
    private static ObjectNode assertion(JsonNode s,String id) {for(JsonNode a:s.path("assertions")) if(a.path("id").asText().equals(id)) return (ObjectNode)a;throw new AssertionError(id);}
    private static ObjectNode slots(JsonNode s,String id) {return (ObjectNode)action(s,id).path("request").path("slots");}
    private static void drop(ObjectNode s,String field,String id) {ArrayNode a=(ArrayNode)s.path(field);for(int i=a.size()-1;i>=0;i--) if(a.get(i).path("id").asText().equals(id)) a.remove(i);}
    private static List<String> ids(JsonNode s) {List<String> out=new ArrayList<>();for(JsonNode a:s.path("actions")) out.add(a.path("id").asText());return out;}
    private static JsonNode result(String action,String pointer) {return Json.parse("{\"$result\":{\"actionId\":\""+action+"\",\"pointer\":\""+pointer+"\"}}");}
    private void rejects(String key,List<String> problems) {assertTrue(problems.stream().anyMatch(p->p.contains(key)),key+": "+problems);}
    private List<String> pick(String caseId,String subId,Consumer<ObjectNode> change) throws Exception {
        JsonNode c=caseJson(caseId);ObjectNode s=sub(c,subId).deepCopy();change.accept(s);
        return new ContractValidator(root).pickBeforeDispatchProblems(only(c,s));
    }
    private List<String> custody(String caseId,String subId,Consumer<ObjectNode> changeSub,Consumer<ObjectNode> changeFixture) throws Exception {
        JsonNode c=caseJson(caseId);ObjectNode s=sub(c,subId).deepCopy();changeSub.accept(s);
        ObjectNode fixture=(ObjectNode)Json.read(root.resolve(s.path("fixtureRef").asText()));changeFixture.accept(fixture);
        Path p=Files.createTempFile(root.resolve("verification/harness/target"),"round9-fixture-",".json");Json.write(p,fixture);
        s.put("fixtureRef",root.relativize(p).toString());
        return new ContractValidator(root).receiptCustodyProblems(only(c,s));
    }
    private static final ObjectMapper CANONICAL=new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true);
    private static void nameCustodian(ObjectNode fixture,String doc,String custodian) {
        ObjectNode content=(ObjectNode)fixture.path("aliases").path(doc).path("fixtureContent");content.put("receivingCustodianAlias",custodian);
        try {String sha=Json.sha256Text(CANONICAL.writeValueAsString(Json.MAPPER.treeToValue(content,Object.class)));
            for(JsonNode e:fixture.path("evidence")) if(e.path("alias").asText().equals(doc)) ((ObjectNode)e).put("sha256",sha);}
        catch(Exception e) {throw new AssertionError(e);}
    }

    // P2: a dispatch of a runtime reservation that is expected to apply needs an earlier, explicit, authorized pick.
    @Test void dispatchOfARuntimeAllocationIsPrecededByItsPick() throws Exception {
        ContractValidator v=new ContractValidator(root);
        for(String id:CASES) assertEquals(List.of(),v.pickBeforeDispatchProblems(caseJson(id)),id);
        // The closure review 6 shapes: T13 and T05 reserve and dispatch with no pick.
        Consumer<ObjectNode> t13NoPick=s->{drop(s,"actions","pick");drop(s,"assertions","pick-applied");
            ((ObjectNode)action(s,"dispatch-sale").path("request")).set("expectedRevision",result("reserve","/response/revision"));};
        rejects("no earlier pickQuantity names that allocation",pick("T13",T13,t13NoPick));
        rejects("no earlier pickQuantity names that allocation",pick("T05",T05,s->{drop(s,"actions","pick");drop(s,"assertions","pick-before-dispatch-applied");
            ((ObjectNode)action(s,"dispatch").path("request")).set("expectedRevision",result("reserve","/response/revision"));}));
        // Only the dispatch's dependants make it "expected to apply" in T13 (the delivery reads its result) when the pin is gone.
        rejects("no earlier pickQuantity names that allocation",pick("T13",T13,t13NoPick.andThen(s->drop(s,"assertions","dispatch-sale-applied"))));
        // A pick of another allocation, a stale pre-pick revision and a pick pinned to fail do not count.
        rejects("no earlier pickQuantity names that allocation",pick("T13",T13,s->slots(s,"pick").set("allocationId",Json.parse("{\"value\":"+result("reserve","/response/id")+"}"))));
        rejects("no earlier pickQuantity names that allocation",pick("T13",T13,s->{ArrayNode a=(ArrayNode)s.path("actions");ObjectNode p=action(s,"pick");drop(s,"actions","pick");
            a.insert(ids(s).indexOf("dispatch-sale")+1,p);}));
        rejects("an action before pick pick",pick("T13",T13,s->((ObjectNode)action(s,"dispatch-sale").path("request")).set("expectedRevision",result("reserve","/response/revision"))));
        rejects("whose outcome the subcase pins to a value other than APPLIED",pick("T05",T05,s->assertion(s,"pick-before-dispatch-applied").put("expected","REJECTED")));
        // An unpicked dispatch that nothing reads is a negative only when it pins a non-APPLIED outcome and an error code the
        // product returns before the pick check (round 9 follow-up): T05 with the dispatch pinned REJECTED and without the two
        // holds that read its remainingSegmentId fails until it also pins such a code (here FORBIDDEN), TYPE_INVALID does not count.
        Consumer<ObjectNode> negative=s->{drop(s,"actions","pick");drop(s,"assertions","pick-before-dispatch-applied");
            assertion(s,"ordinary-authorized-dispatch-after-confirmation-17").put("expected","REJECTED");};
        Consumer<ObjectNode> unread=s->{drop(s,"actions","independent-qc");drop(s,"actions","independent-recall");};
        java.util.function.Function<String,Consumer<ObjectNode>> code=value->s->{ObjectNode x=assertion(s,"ordinary-authorized-dispatch-after-confirmation-17").deepCopy();
            x.put("id","dispatch-code").put("expected",value);((ObjectNode)x.path("source")).put("pointer","/response/error/code");((ArrayNode)s.path("assertions")).add(x);};
        rejects("is never picked, and the subcase does not pin",pick("T05",T05,negative.andThen(unread)));
        rejects("is never picked, and the subcase does not pin",pick("T05",T05,negative.andThen(unread).andThen(code.apply("TYPE_INVALID"))));
        // step2r round 10 (closure review 7 P3): FORBIDDEN alone no longer counts, because the transit PLACE authorization after
        // the pick also answers FORBIDDEN; the authorized ordinary dispatcher shows no pre-pick reason (StepTwoRoundTenRegressionTest).
        rejects("is never picked, and the subcase does not pin",pick("T05",T05,negative.andThen(unread).andThen(code.apply("FORBIDDEN"))));
        // ... while a later action that uses its result still makes the dispatch one that must apply.
        rejects("no earlier pickQuantity names that allocation",pick("T05",T05,negative));
        // What the cases now say: the picker holds pickQuantity in role and grant, the pick sits between reserve and dispatch,
        // is pinned APPLIED, and the dispatch revision chains to it.
        for(String[] c:new String[][]{{"T13",T13,"sales","dispatch-sale","pick-applied"},{"T05",T05,"warehouse","dispatch","pick-before-dispatch-applied"}}) {
            ObjectNode s=sub(caseJson(c[0]),c[1]);List<String> order=ids(s);
            assertTrue(order.indexOf("reserve")<order.indexOf("pick") && order.indexOf("pick")<order.indexOf(c[3]),c[0]);
            assertEquals(c[2],action(s,"pick").path("actorRef").asText());assertEquals("pickQuantity",action(s,"pick").path("capabilityId").asText());
            assertEquals("pick",action(s,c[3]).at("/request/expectedRevision/$result/actionId").asText(),c[0]);
            assertEquals("APPLIED",assertion(s,c[4]).path("expected").asText());assertEquals("/response/outcome",assertion(s,c[4]).at("/source/pointer").asText());
            JsonNode actor=Json.read(root.resolve(s.path("fixtureRef").asText())).path("actors").path(c[2]);
            assertTrue(actor.path("roleCapabilities").toString().contains("\"pickQuantity\"") && actor.path("grant").path("actions").toString().contains("\"pickQuantity\""),c[0]);
        }
    }

    // Round 9 follow-up: an installed (fixture) allocation is dispatched only when the fixture declares it picked; expiry
    // negatives pick first and pin the guard's code, so a product without the guard cannot pass for the missing pick alone.
    private List<String> pickWithFixture(String caseId,String subId,Consumer<ObjectNode> changeSub,Consumer<ObjectNode> changeFixture) throws Exception {
        JsonNode c=caseJson(caseId);ObjectNode s=sub(c,subId).deepCopy();changeSub.accept(s);
        ObjectNode fixture=(ObjectNode)Json.read(root.resolve(s.path("fixtureRef").asText()));changeFixture.accept(fixture);
        Path p=Files.createTempFile(root.resolve("verification/harness/target"),"round9-fixture-",".json");Json.write(p,fixture);
        s.put("fixtureRef",root.relativize(p).toString());
        return new ContractValidator(root).pickBeforeDispatchProblems(only(c,s));
    }
    private static void unpick(JsonNode node) {
        if(node.isObject()) {((ObjectNode)node).remove(List.of("pickedAt","pickedByAlias"));for(JsonNode v:node) unpick(v);}
        else if(node.isArray()) for(JsonNode v:node) unpick(v);
    }
    private static void eachPicked(JsonNode node,Consumer<ObjectNode> change) {
        if(node.isObject()) {if(node.has("pickedAt")) change.accept((ObjectNode)node);for(JsonNode v:node) eachPicked(v,change);}
        else if(node.isArray()) for(JsonNode v:node) eachPicked(v,change);
    }
    @Test void fixtureAllocationsArePickedAndExpiryNegativesOnlyFailForTheExpiry() throws Exception {
        // The follow-up shapes: every fixture-allocation dispatch (T04 rollback, T24 audit/retry, C3 authorized, V3 races, V7
        // revocation, T08, C1) without a declared pick.
        String[][] fixtureCases={{"T04","rollback-afterMovementBeforeAllocation"},{"T24","audit-rollback-and-retry"},{"C3","api-dispatchQuantity"},{"V3","dispatch-first"},
            {"V3","hold-first"},{"V7","authorization-effect-first"},{"V7","restart-after-revoke"},{"T08","revoke-grant"},{"C1","revoked-basis"}};
        for(String[] c:fixtureCases) {
            assertEquals(List.of(),pickWithFixture(c[0],c[1],NONE,NONE),c[0]+"/"+c[1]);
            rejects("has no picked state",pickWithFixture(c[0],c[1],NONE,StepTwoRoundNineRegressionTest::unpick));
        }
        rejects("is after the fixture clock knownAt",pickWithFixture("T04","rollback-afterMovementBeforeAllocation",NONE,f->eachPicked(f,x->x.put("pickedAt","2026-10-08T00:00:00Z"))));
        rejects("is not an ISO-8601 instant",pickWithFixture("T04","rollback-afterMovementBeforeAllocation",NONE,f->eachPicked(f,x->x.put("pickedAt","yesterday"))));
        rejects("which is not a fixture actor",pickWithFixture("T04","rollback-afterMovementBeforeAllocation",NONE,f->eachPicked(f,x->x.put("pickedByAlias","nobody"))));
        // A picked fixture allocation picked again: the product answers 'Allocation already picked'.
        rejects("rejects a second pick",pickWithFixture("T04","rollback-afterMovementBeforeAllocation",s->{ObjectNode p=Json.object();p.put("id","pick-again").put("kind","invoke").put("actorRef","warehouse").put("capabilityId","pickQuantity");
            ObjectNode r=Json.object();r.put("capabilityId","pickQuantity");r.set("allocationId",Json.parse("{\"$alias\":\"ALLOC\"}"));p.set("request",r);
            ((ArrayNode)s.path("actions")).insert(ids(s).indexOf("dispatch"),p);},NONE));
        // An explicit pick action of an unpicked fixture allocation is the other accepted form.
        assertEquals(List.of(),pickWithFixture("T04","rollback-afterMovementBeforeAllocation",s->{ObjectNode p=Json.object();p.put("id","pick").put("kind","invoke").put("actorRef","warehouse").put("capabilityId","pickQuantity");
            ObjectNode r=Json.object();r.put("capabilityId","pickQuantity");r.set("allocationId",Json.parse("{\"$alias\":\"ALLOC\"}"));p.set("request",r);
            ((ArrayNode)s.path("actions")).insert(ids(s).indexOf("dispatch"),p);},StepTwoRoundNineRegressionTest::unpick));
        // T26 expiry guards and T16 expiry-sweeper: without the pick and the guard code a guard-less product passes; with the code
        // alone (a code the product returns before the pick check) the negative still discriminates.
        String[] guards={"lot-expiry-no-event","lot-expiry-delayed-guard","disposition-expiry-no-event","disposition-expiry-delayed-guard","grant-expiry-no-event",
            "grant-expiry-delayed-guard","policy-expiry-no-event","policy-expiry-delayed-guard","lot-expiry-autonomous-loop"};
        for(String g:guards) {
            ObjectNode s=sub(caseJson("T26"),g);List<String> order=ids(s);
            assertTrue(order.indexOf("reserve")<order.indexOf("pick") && order.indexOf("pick")<order.indexOf("advance"),g+": picked before the expiry boundary");
            assertEquals("APPLIED",assertion(s,"guard-pick-applied").path("expected").asText(),g);
            assertEquals(g.startsWith("grant")?"FORBIDDEN":"INSUFFICIENT_ELIGIBLE_QUANTITY",assertion(s,"guard-code").path("expected").asText(),g);
            assertEquals("pick",action(s,"dispatch").at("/request/expectedRevision/$result/actionId").asText(),g);
            assertTrue(Json.read(root.resolve(s.path("fixtureRef").asText())).at("/actors/warehouse/grant/actions").toString().contains("\"pickQuantity\""),g);
            rejects("is never picked, and the subcase does not pin",pick("T26",g,x->{drop(x,"actions","pick");drop(x,"assertions","guard-pick-applied");drop(x,"assertions","guard-code");}));
            // step2r round 10: the code alone is accepted only when the case shows its pre-pick reason (the expired grant at
            // the dispatch clock); a LOT, disposition or policy expiry does not show that INSUFFICIENT_ELIGIBLE_QUANTITY precedes the pick.
            List<String> codeOnly=pick("T26",g,x->{drop(x,"actions","pick");drop(x,"assertions","guard-pick-applied");});
            if(g.startsWith("grant")) assertEquals(List.of(),codeOnly,g);else rejects("is never picked, and the subcase does not pin",codeOnly);
        }
        rejects("is never picked, and the subcase does not pin",pick("T16","expiry-sweeper",x->{drop(x,"actions","pick");for(String a:List.of("pick-applied","dispatch-after-sweep-rejected","dispatch-after-sweep-code")) drop(x,"assertions",a);}));
        ObjectNode t16=sub(caseJson("T16"),"expiry-sweeper");
        assertEquals("INSUFFICIENT_ELIGIBLE_QUANTITY",assertion(t16,"dispatch-after-sweep-code").path("expected").asText());
        assertEquals("pick",action(t16,"dispatch").at("/request/expectedRevision/$result/actionId").asText());
        assertEquals("APPLIED",assertion(sub(caseJson("T16"),"expiry-delayed-sweep"),"pick-applied").path("expected").asText());
    }

    // P3 (a): a slot custodian that is no actor of the confirming organization is REJECTED FORBIDDEN, before SCOPE_INELIGIBLE.
    @Test void custodianOutsideTheConfirmingOrganizationIsForbidden() throws Exception {
        String neg="receipt-custody-unverified";
        Consumer<ObjectNode> pinForbidden=x->{action(x,"receipt60").put("custodyControl","FORBIDDEN");
            assertion(x,"custody-unverified-outcome").put("expected","REJECTED");assertion(x,"custody-unverified-code").put("expected","FORBIDDEN");};
        Consumer<ObjectNode> receiver=x->slots(x,"receipt60").set("receivingCustodianId",Json.parse("{\"$alias\":\"receiver\"}"));
        Consumer<ObjectNode> otherOrg=f->((ObjectNode)f.path("actors").path("receiver")).put("organizationAlias","ORG-B");
        // The closure review 6 shape: an ORG-B receiver declared SCOPE_INELIGIBLE passed prepare; the product answers FORBIDDEN.
        rejects("is not the product's first failing receiving-custody check (FORBIDDEN",custody("E1",neg,receiver.andThen(x->{action(x,"receipt60").put("custodyControl","SCOPE_INELIGIBLE");
            assertion(x,"custody-unverified-outcome").put("expected","REJECTED");assertion(x,"custody-unverified-code").put("expected","SCOPE_INELIGIBLE");}),otherOrg));
        assertEquals(List.of(),custody("E1",neg,receiver.andThen(pinForbidden),otherOrg));
        // An alias that is no fixture actor (SUPPLIER) is FORBIDDEN too.
        assertEquals(List.of(),custody("E1",neg,x->{pinForbidden.accept(x);slots(x,"receipt60").set("receivingCustodianId",Json.parse("{\"$alias\":\"SUPPLIER\"}"));},NONE));
        // FORBIDDEN must also be pinned as REJECTED/FORBIDDEN, and a same-organization actor without authority stays SCOPE_INELIGIBLE.
        rejects("must pin /response/outcome equals REJECTED and /response/error/code equals FORBIDDEN",custody("E1",neg,receiver.andThen(x->action(x,"receipt60").put("custodyControl","FORBIDDEN")),otherOrg));
        rejects("is not the product's first failing receiving-custody check (SCOPE_INELIGIBLE",custody("E1",neg,x->{pinForbidden.accept(x);slots(x,"receipt60").set("receivingCustodianId",Json.parse("{\"$alias\":\"observer\"}"));},NONE));
    }

    // P3 (b): EVIDENCE_CONFLICT/EVIDENCE_UNVERIFIED count only verification-basis originals of the same canonical occurrence.
    @Test void onlyVerificationBasesOfTheCanonicalOccurrenceNameTheCustodian() throws Exception {
        ContractValidator v=new ContractValidator(root);
        for(String id:CASES) assertEquals(List.of(),v.receiptCustodyProblems(caseJson(id)),id);
        // A request-level evidenceRefs witness does not name the custodian: warehouse-60 cited only there leaves no basis.
        rejects("no receipt original it cites",custody("E1","full-flow-quantities",x->{slots(x,"receipt60").remove("evidenceId");
            ((ArrayNode)action(x,"receipt60").path("request").path("evidenceRefs")).add(Json.parse("{\"$alias\":\"warehouse-60\"}"));},NONE));
        // ... nor does a witness naming someone else create a conflict.
        assertEquals(List.of(),custody("E1","full-flow-quantities",NONE,f->nameCustodian(f,"delivery-proof","qc")));
        // T13 now names its original as the verification basis; without it the receipts are unevidenced.
        rejects("no receipt original it cites",custody("T13",T13,x->slots(x,"receipt60").remove("evidenceId"),NONE));
        for(String r:List.of("receipt60","receipt40","receipt5")) assertEquals("warehouse-receipt",slots(sub(caseJson("T13"),T13),r).at("/evidenceId/value/$alias").asText(),r);
        // A duplicate source of the same canonicalOccurrenceKey adds a verified chain: its basis naming qc conflicts with warehouse-60.
        rejects("name different receiving custodians",custody("E1","full-flow-quantities",x->{ObjectNode dup=action(x,"receipt60").deepCopy();dup.put("id","receipt60-other-source");
            ((ObjectNode)dup.path("request").path("slots")).set("evidenceId",Json.parse("{\"$alias\":\"delivery-proof\"}"));
            ((ObjectNode)dup.path("request")).put("commandIdempotencyKey","E1-full-flow-quantities-receipt60-other-source");
            ArrayNode a=(ArrayNode)x.path("actions");a.insert(ids(x).indexOf("receipt60"),dup);},f->nameCustodian(f,"delivery-proof","qc")));
    }

    // P3 (c): NO_TASK needs a scheduler-recorded natural cycle that ran entirely inside [observeFrom, completedAt].
    private HostObservationValidatorTest.Capture noTask(String start,String end,Consumer<ObjectNode> rows) throws Exception {
        HostObservationValidatorTest.Capture c=new StepTwoRoundSevenRegressionTest().noTask(start,end,1);
        ObjectNode raw=(ObjectNode)c.host().path("extractor").path("rawRows");rows.accept(raw);
        Path copy=Files.createTempFile(root.resolve("verification/harness/target"),"round9-rows-",".json");Json.write(copy,raw);
        String refPath=root.relativize(copy).toString();((ObjectNode)c.host().path("extractor")).put("rawRowsArtifactRef",refPath);
        ObjectNode a=Json.object();a.put("path",refPath).put("sha256",Json.sha256(copy)).put("sizeBytes",Files.size(copy)).put("completeness","COMPLETE");a.set("scope",Json.parse("{\"workspace\":\"host-selftest\"}"));
        ((ArrayNode)c.host().path("extractor").path("inputArtifacts")).set(0,a);
        List<String> refs=new ArrayList<>(c.result().artifactRefs());refs.add(refPath);
        return new HostObservationValidatorTest.Capture(c.control(),c.host(),new StepResult(c.result().actionId(),c.result().driverStatus(),c.result().data(),null,c.result().reason(),c.result().provenance(),refs));
    }
    private String rejection(HostObservationValidatorTest.Capture c) {
        return assertThrows(IllegalArgumentException.class,()->HostObservationValidator.validate(new ContractValidator(root),c.control(),c.result(),false,null)).getMessage();
    }
    private static Consumer<ObjectNode> cycle(String startedAt,String completedAt,String startedBy) {
        return rows->{ObjectNode row=Json.object();row.put("schedulerId",rows.at("/operationEvidence/schedulerId").asText()).put("sweepId","synthetic-natural-cycle")
            .put("startedAt",startedAt).put("completedAt",completedAt).put("startedBy",startedBy);rows.set("schedulerCycles",Json.array().add(row));};
    }
    @Test void noTaskNeedsASchedulerRecordedCycleCompletedInsideTheWatch() throws Exception {
        String from="2026-10-07T00:00:00Z",end="2026-10-07T00:00:02Z";
        HostObservationValidatorTest.Capture ok=noTask(from,end,cycle("2026-10-07T00:00:00.500Z","2026-10-07T00:00:01.500Z","SCHEDULER_LOOP"));
        HostObservationValidator.validate(new ContractValidator(root),ok.control(),ok.result(),false,null);
        assertTrue(rejection(noTask(from,end,r->r.remove("schedulerCycles"))).contains("schedulerCycles"));
        // The closure review 6 shape: the cycle in progress at observeFrom finishes at +1.2s and the next one would submit at
        // +2.2s, after a watcher that ended at +2s. Only the cycle that began before observeFrom completed inside the watch.
        assertTrue(rejection(noTask(from,end,cycle("2026-10-06T23:59:59.800Z","2026-10-07T00:00:01.200Z","SCHEDULER_LOOP")))
            .contains("no scheduler-recorded natural cycle"));
        assertTrue(rejection(noTask(from,end,cycle("2026-10-07T00:00:01.200Z","2026-10-07T00:00:02.200Z","SCHEDULER_LOOP"))).contains("no scheduler-recorded natural cycle"));
        assertTrue(rejection(noTask(from,end,cycle("2026-10-07T00:00:00.500Z","2026-10-07T00:00:01.500Z","HARNESS"))).contains("no scheduler-recorded natural cycle"));
        assertTrue(rejection(noTask(from,end,cycle("2026-10-07T00:00:01.500Z","2026-10-07T00:00:00.500Z","SCHEDULER_LOOP"))).contains("completed before it started"));
        assertTrue(rejection(noTask(from,end,cycle("2026-10-07T00:00:00.500Z","2026-10-07T00:00:01.500Z","SCHEDULER_LOOP").andThen(r->((ObjectNode)r.path("schedulerCycles").get(0)).put("schedulerId","other"))))
            .contains("different scheduler"));
        // The guide no longer claims that two tick periods alone prove a completed cycle.
        String guide=Files.readString(root.resolve("verification/host-observation-guide.md"));
        assertTrue(guide.contains("rawRows.schedulerCycles[]") && guide.contains("두 tick은 하한일 뿐 증명이 아니다"));
    }
}
