package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Step 2 closure review 7 (step2r round 10): the product check chain after the pick (transit place, occurrence time) and the
 * authority chain (grant capability scope, delegator, acting grant and places) as preparation rules. Counterexamples are the
 * review's shapes applied to the committed cases; preparation checks only, never product PASS evidence.
 */
final class StepTwoRoundTenRegressionTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private static final List<String> CASES=new ArrayList<>();
    static {
        for(int i=1;i<=26;i++) CASES.add(String.format("T%02d",i));
        for(int i=1;i<=5;i++) CASES.add("C"+i);
        for(int i=1;i<=8;i++) CASES.add("V"+i);
        CASES.add("E1");CASES.add("E2");
    }
    private static final Consumer<ObjectNode> NONE=x->{};
    private static final String T05="manager-disposition",T13="partial-excess-return-relocation",C2="cumulative-versus-state";
    interface Rule {List<String> apply(ContractValidator v,JsonNode c) throws Exception;}
    private JsonNode caseJson(String id) throws Exception {return Json.read(root.resolve("verification/cases/"+id+"/case.json"));}
    private static ObjectNode sub(JsonNode c,String id) {for(JsonNode s:c.path("subcases")) if(s.path("id").asText().equals(id)) return (ObjectNode)s;throw new AssertionError(id);}
    private static ObjectNode only(JsonNode c,ObjectNode s) {ObjectNode copy=((ObjectNode)c).deepCopy();copy.set("subcases",Json.array().add(s));return copy;}
    private static List<JsonNode> flat(JsonNode actions) {List<JsonNode> out=new ArrayList<>();for(JsonNode a:actions) {out.add(a);if(a.has("call")) out.add(a.path("call"));for(JsonNode b:a.path("branches")) out.addAll(flat(b.path("actions")));}return out;}
    private static ObjectNode action(JsonNode s,String id) {for(JsonNode a:flat(s.path("actions"))) if(a.path("id").asText().equals(id)) return (ObjectNode)a;throw new AssertionError(id);}
    private static ObjectNode assertion(JsonNode s,String id) {for(JsonNode a:s.path("assertions")) if(a.path("id").asText().equals(id)) return (ObjectNode)a;throw new AssertionError(id);}
    private static ObjectNode slots(JsonNode s,String id) {return (ObjectNode)action(s,id).path("request").path("slots");}
    private static void drop(ObjectNode s,String field,String id) {ArrayNode a=(ArrayNode)s.path(field);for(int i=a.size()-1;i>=0;i--) if(a.get(i).path("id").asText().equals(id)) a.remove(i);}
    private static JsonNode alias(String a) {return Json.parse("{\"$alias\":\""+a+"\"}");}
    private static ObjectNode actor(ObjectNode f,String a) {return (ObjectNode)f.path("actors").path(a);}
    private static void remove(JsonNode array,String value) {if(!array.isArray()) return;ArrayNode a=(ArrayNode)array;for(int i=a.size()-1;i>=0;i--) if(a.get(i).asText().equals(value)) a.remove(i);}
    private void rejects(String key,List<String> problems) {assertTrue(problems.stream().anyMatch(p->p.contains(key)),key+": "+problems);}
    private List<String> run(Rule rule,String caseId,String subId,Consumer<ObjectNode> changeSub,Consumer<ObjectNode> changeFixture) throws Exception {
        JsonNode c=caseJson(caseId);ObjectNode s=sub(c,subId).deepCopy();changeSub.accept(s);
        ObjectNode fixture=(ObjectNode)Json.read(root.resolve(s.path("fixtureRef").asText()));changeFixture.accept(fixture);
        Files.createDirectories(root.resolve("verification/harness/target"));
        Path p=Files.createTempFile(root.resolve("verification/harness/target"),"round10-fixture-",".json");Json.write(p,fixture);
        s.put("fixtureRef",root.relativize(p).toString());
        return rule.apply(new ContractValidator(root),only(c,s));
    }
    private void clean(Rule rule) throws Exception {ContractValidator v=new ContractValidator(root);for(String id:CASES) assertEquals(List.of(),rule.apply(v,caseJson(id)),id);}

    // P1: a dispatch names the TRANSIT place it moves the goods to, inside the dispatcher's grant places.
    @Test void dispatchNamesATransitPlaceInsideTheDispatchersPlaceScope() throws Exception {
        Rule rule=ContractValidator::dispatchTransitProblems;clean(rule);
        // The closure review 7 shapes: T05 and T13 dispatch with no transit place, C3 with a fixture that has none.
        rejects("names an allocation but no TRANSIT place in cargoPlaceId",run(rule,"T05",T05,s->slots(s,"dispatch").remove("cargoPlaceId"),NONE));
        rejects("names an allocation but no TRANSIT place in cargoPlaceId",run(rule,"T13",T13,s->slots(s,"dispatch-sale").remove("cargoPlaceId"),NONE));
        rejects("names an allocation but no TRANSIT place in cargoPlaceId",run(rule,"C3","api-dispatchQuantity",s->slots(s,"authorized-same-input").remove("cargoPlaceId"),NONE));
        // T24 and T26 carried the allocation on the request itself; since step2r round 12 every business value is a slot.
        rejects("names an allocation but no TRANSIT place in cargoPlaceId",run(rule,"T24","audit-rollback-and-retry",s->slots(s,"retry").remove("cargoPlaceId"),NONE));
        rejects("names an allocation but no TRANSIT place in cargoPlaceId",run(rule,"T26","lot-expiry-no-event",s->slots(s,"dispatch").remove("cargoPlaceId"),NONE));
        // An internal storage place, or a TRANSIT place outside the dispatcher's (or its delegator's) place scope, is refused.
        rejects("is not a fixture Place of kind TRANSIT",run(rule,"T05",T05,s->slots(s,"dispatch").set("cargoPlaceId",alias("W2")),NONE));
        rejects("without the transit place TRANSIT",run(rule,"T05",T05,NONE,f->remove(actor(f,"ordinary").path("grant").path("scope").path("placeAliases"),"TRANSIT")));
        rejects("the grant of delegator lists places",run(rule,"C3","api-dispatchQuantity",NONE,f->remove(actor(f,"delegator").path("grant").path("scope").path("placeAliases"),"TRANSIT")));
        // A negative pinned FORBIDDEN is decided before the PLACE authorization: the reader of C3 needs no transit scope.
        assertEquals(List.of(),run(rule,"C3","api-dispatchQuantity",NONE,f->remove(actor(f,"reader").path("grant").path("scope").path("placeAliases"),"TRANSIT")));
        // Every positive and negative dispatch of the corpus names the place now; the contract slot is the corpus slot.
        JsonNode contract=Json.read(root.resolve("contracts/fixture-place-kinds.json")).path("dispatchTransit");
        assertEquals("cargoPlaceId",contract.path("slot").asText());assertEquals("transitPlaceId",contract.path("productSlot").asText());assertEquals("1.4.0",Json.read(root.resolve("contracts/fixture-place-kinds.json")).path("version").asText());
    }

    // P2: dispatch occurrence is at or after its authorized pick on the product clock; deliveries follow their dispatch.
    @Test void dispatchOccursAfterItsPickOnTheProductClock() throws Exception {
        Rule rule=ContractValidator::occurrenceTimeProblems;clean(rule);
        // The closure review 7 shape: C2/T09 cumulative-versus-state on the old clock (asOf 10-07T02:00) pick at 02:00 and
        // dispatch at 10-06T09:00; T09 exists-versus-end-throughout picks at 02:00 and dispatches at 01:00.
        Consumer<ObjectNode> oldClock=f->{((ObjectNode)f.path("clock")).put("asOf","2026-10-07T02:00:00Z").put("knownAt","2026-10-07T04:00:00Z");};
        for(String id:List.of("C2","T09")) rejects("precedes its authorized pick",run(rule,id,C2,s->{for(String c:List.of("clock-dispatch60","clock-receive40","clock-known")) drop(s,"actions",c);},oldClock));
        rejects("precedes its authorized pick",run(rule,"T09","exists-versus-end-throughout",s->{for(String c:List.of("clock-dispatch","clock-known")) drop(s,"actions",c);},oldClock));
        rejects("precedes its authorized pick",run(rule,"T11","cancel-after-shipment",s->slots(s,"dispatch").put("occurredAt","2026-10-07T01:00:00Z"),NONE));
        // Without the clock advance the historical dispatch lies in the product's future.
        rejects("is after the product clock",run(rule,"C2",C2,s->drop(s,"actions","clock-dispatch60"),NONE));
        // A fixture pick is a past fact at pickedAt: a dispatch dated before it is refused too.
        rejects("precedes its authorized pick",run(rule,"V3","late-v1-release",s->slots(s,"dispatch").put("occurredAt","2026-10-06T00:00:00Z"),NONE));
        // A delivery dated before its dispatch.
        rejects("precedes the dispatch dispatch",run(rule,"C4","return-not-correction",s->slots(s,"delivery").put("occurredAt","2026-10-07T08:59:00Z"),NONE));
        // The re-timed cases keep the historical instants: dispatch60 on 10-06T09:00, receive40 on 10-07T01:30, queries at 02:00/04:00.
        ObjectNode c2=sub(caseJson("C2"),C2);
        assertEquals("2026-10-06T09:00:00Z",slots(c2,"dispatch60").path("occurredAt").asText());
        assertEquals("2026-10-07T01:30:00Z",slots(c2,"receive40").path("occurredAt").asText());
        assertEquals("2026-10-05T09:00:00Z",Json.read(root.resolve(c2.path("fixtureRef").asText())).at("/clock/asOf").asText());
        assertEquals("2026-10-06T09:00:00Z",action(c2,"clock-dispatch60").at("/control/parameters/instant").asText());
    }

    // P2 and the audit: the authority chain the product intersects.
    @Test void grantsAreConsistentAndCoverTheActionsTheyAuthorize() throws Exception {
        Rule rule=ContractValidator::grantAuthorityProblems;clean(rule);
        // The closure review 7 shape: T26 warehouse holds pickQuantity in grant.actions but not in grant.scope.capabilityIds.
        for(String g:List.of("lot-expiry-no-event","grant-expiry-no-event","lot-expiry-autonomous-loop")) {
            rejects("grant action pickQuantity is missing from its grant scope capabilityIds",run(rule,"T26",g,NONE,f->remove(actor(f,"warehouse").path("grant").path("scope").path("capabilityIds"),"pickQuantity")));
            assertTrue(Json.read(root.resolve(sub(caseJson("T26"),g).path("fixtureRef").asText())).at("/actors/warehouse/grant/scope/capabilityIds").toString().contains("\"pickQuantity\""),g);
        }
        // A fixture-actor delegator that does not hold what it delegates (C2 supervisor before round 10).
        rejects("delegator supervisor of warehouse does not hold the delegated action dispatchQuantity",run(rule,"C2",C2,NONE,f->{remove(actor(f,"supervisor").path("roleCapabilities"),"dispatchQuantity");remove(actor(f,"supervisor").path("grant").path("actions"),"dispatchQuantity");}));
        // The acting actor without the capability, with an expired grant, or outside the places it touches.
        rejects("pickQuantity is expected to apply but warehouse does not hold it",run(rule,"T05",T05,NONE,f->remove(actor(f,"warehouse").path("grant").path("actions"),"pickQuantity")));
        rejects("is not valid at the product clock",run(rule,"T26","grant-expiry-no-event",NONE,f->((ObjectNode)actor(f,"warehouse").path("grant")).put("validUntil","2026-10-07T09:00:00Z")));
        rejects("without W, which the product authorizes for it",run(rule,"T05",T05,NONE,f->remove(actor(f,"ordinary").path("grant").path("scope").path("placeAliases"),"W")));
        rejects("without W-alt, which the product authorizes for it",run(rule,"T13",T13,NONE,f->{for(JsonNode a:f.path("actors")) for(String k:List.of("placeAliases","places")) remove(a.path("grant").path("scope").path(k),"W-alt");}));
        // A negative that pins its refusal is not an action expected to apply (T20 readAgent reserve pinned FORBIDDEN).
        assertEquals(List.of(),run(rule,"T20","domain-forbidden",NONE,NONE));
    }

    // Audit: dispatch and stock commands name their basis; fulfilment and moves touch internal storage only.
    @Test void commandsNameTheirBasisAndUseWarehouseStock() throws Exception {
        clean(ContractValidator::commandBasisProblems);clean(ContractValidator::warehouseCustodyProblems);
        rejects("moveQuantity is expected to apply but names no basis",run(ContractValidator::commandBasisProblems,"T26","safe-retry-canonical-current-grant",s->slots(s,"original").remove("evidenceRef"),NONE));
        rejects("splitQuantity is expected to apply but names no basis",run(ContractValidator::commandBasisProblems,"T26","restore-complete",s->slots(s,"split").remove("evidenceRef"),NONE));
        rejects("but the product requires INTERNAL_STORAGE",run(ContractValidator::warehouseCustodyProblems,"T05",T05,NONE,f->((ObjectNode)f.path("aliases").path("CON40")).put("locationAlias","PORT")));
        rejects("moveQuantity of Q20 goes to W;",run(ContractValidator::warehouseCustodyProblems,"T26","safe-retry-canonical-current-grant",s->slots(s,"original").set("destinationId",alias("W")),NONE));
    }

    // P3 (a)/(b): an unpicked dispatch is a negative only for a pre-pick reason the case shows; a dispatch needs an allocation.
    @Test void unpickedNegativesShowWhyTheyPrecedeThePick() throws Exception {
        Rule rule=ContractValidator::pickBeforeDispatchProblems;clean(rule);
        Consumer<ObjectNode> negative=s->{drop(s,"actions","pick");drop(s,"assertions","pick-before-dispatch-applied");drop(s,"actions","independent-qc");drop(s,"actions","independent-recall");
            assertion(s,"ordinary-authorized-dispatch-after-confirmation-17").put("expected","REJECTED");};
        java.util.function.Function<String,Consumer<ObjectNode>> code=value->s->{ObjectNode x=assertion(s,"ordinary-authorized-dispatch-after-confirmation-17").deepCopy();
            x.put("id","dispatch-code").put("expected",value);((ObjectNode)x.path("source")).put("pointer","/response/error/code");((ArrayNode)s.path("assertions")).add(x);};
        // FORBIDDEN, STALE_REVISION or INSUFFICIENT_ELIGIBLE_QUANTITY alone also come from checks after the pick (transit PLACE,
        // expectedRevision, DISPATCH/continuous authority): the authorized ordinary dispatcher is not a pre-pick FORBIDDEN.
        for(String c:List.of("FORBIDDEN","STALE_REVISION","INSUFFICIENT_ELIGIBLE_QUANTITY","SCOPE_INELIGIBLE")) rejects("does not pin a non-APPLIED outcome with a code whose earlier check the case shows",run(rule,"T05",T05,negative.andThen(code.apply(c)),NONE));
        // ... but an ordinary actor without dispatchQuantity is.
        assertEquals(List.of(),run(rule,"T05",T05,negative.andThen(code.apply("FORBIDDEN")),f->{remove(actor(f,"ordinary").path("roleCapabilities"),"dispatchQuantity");remove(actor(f,"ordinary").path("grant").path("actions"),"dispatchQuantity");}));
        assertEquals(List.of(),run(rule,"T05",T05,negative.andThen(code.apply("VERSION_UNSUPPORTED")).andThen(s->((ObjectNode)action(s,"dispatch").path("request")).put("definitionVersion","definition-v9")),NONE));
        // T26 expiry guards without their pick: the expired grant shows FORBIDDEN at the dispatch clock; LOT expiry does not show
        // that INSUFFICIENT_ELIGIBLE_QUANTITY precedes the pick, so that negative keeps its pick.
        Consumer<ObjectNode> unpick=s->{drop(s,"actions","pick");drop(s,"assertions","guard-pick-applied");};
        assertEquals(List.of(),run(rule,"T26","grant-expiry-no-event",unpick,NONE));
        rejects("does not pin a non-APPLIED outcome with a code whose earlier check the case shows",run(rule,"T26","lot-expiry-no-event",unpick,NONE));
        // T06 dispatch-pending: the consumed fixture allocation pinned STALE_REVISION needs no pick; a malformed dispatch
        // without an allocation, or another code, is refused.
        ObjectNode t06=sub(caseJson("T06"),"inconsistent-after-dispatch");
        assertEquals("STALE_REVISION",assertion(t06,"pending-code").path("expected").asText());assertEquals("CONFLICT",assertion(t06,"pending-rejection").path("expected").asText());
        rejects("names no allocation",run(rule,"T06","inconsistent-after-dispatch",s->slots(s,"dispatch-pending").remove("allocationId"),NONE));
        rejects("has no picked state",run(rule,"T06","inconsistent-after-dispatch",s->assertion(s,"pending-code").put("expected","INSUFFICIENT_ELIGIBLE_QUANTITY"),NONE));
        rejects("has no picked state",run(rule,"T06","inconsistent-after-dispatch",NONE,f->{((ObjectNode)f.path("aliases").path("old-allocation")).remove("state");((ObjectNode)f.path("baseline")).remove("priorHistory");}));
        // A route-level denial without an allocation (T24 batch/worker attacks pinned FORBIDDEN) stays accepted only on those routes
        // or with a subject of another organization (deny-batch names ORG-B's B60, so it stays a denial on any route).
        rejects("names no allocation",run(rule,"T24","deny-worker",s->action(s,"attack").put("route","api"),NONE));
        assertEquals(List.of(),run(rule,"T24","deny-batch",s->action(s,"attack").put("route","api"),NONE));
    }

    // P3: the passive NO_TASK assertion that needs the scheduler cycle record is a named runtime gate.
    @Test void schedulerCycleDependencyIsANamedRuntimeGate() throws Exception {
        Map<String,JsonNode> cases=new TreeMap<>();for(String id:CASES) cases.put(id,caseJson(id));
        ObjectNode gate=Main.schedulerCycleGate(cases);
        assertEquals("NOT_RUN_GATED",gate.path("status").asText());assertEquals("SCHEDULER_CYCLE_RECORD",gate.path("gate").asText());
        assertEquals(List.of("T26/lot-expiry-autonomous-loop/repeat-no-due-task"),Json.MAPPER.convertValue(gate.path("assertions"),List.class));
        // Harness-triggered sweeps (lot-expiry-no-event and the other repeat-no-due-task assertions) are not gated.
        assertFalse(gate.path("subcases").toString().contains("lot-expiry-no-event"));
        assertEquals(2,Main.runtimeGates(root,cases).size());
    }
}
