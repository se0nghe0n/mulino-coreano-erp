package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Step 2 closure review 8 P1 (step2r round 11): the recording time of installed fixture facts against the product clock (NF1)
 * and reservations that double-book a fixture or runtime allocation (NF2) as preparation rules. Counterexamples are the
 * review's shapes applied to the committed cases; preparation checks only, never product PASS evidence.
 */
final class StepTwoRoundElevenRegressionTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private static final List<String> CASES=new ArrayList<>();
    static {
        for(int i=1;i<=26;i++) CASES.add(String.format("T%02d",i));
        for(int i=1;i<=5;i++) CASES.add("C"+i);
        for(int i=1;i<=8;i++) CASES.add("V"+i);
        CASES.add("E1");CASES.add("E2");
    }
    private static final Consumer<ObjectNode> NONE=x->{};
    interface Rule {List<String> apply(ContractValidator v,JsonNode c) throws Exception;}
    private JsonNode caseJson(String id) throws Exception {return Json.read(root.resolve("verification/cases/"+id+"/case.json"));}
    private static ObjectNode sub(JsonNode c,String id) {for(JsonNode s:c.path("subcases")) if(s.path("id").asText().equals(id)) return (ObjectNode)s;throw new AssertionError(id);}
    private static ObjectNode only(JsonNode c,ObjectNode s) {ObjectNode copy=((ObjectNode)c).deepCopy();copy.set("subcases",Json.array().add(s));return copy;}
    private static List<JsonNode> flat(JsonNode actions) {List<JsonNode> out=new ArrayList<>();for(JsonNode a:actions) {out.add(a);if(a.has("call")) out.add(a.path("call"));for(JsonNode b:a.path("branches")) out.addAll(flat(b.path("actions")));}return out;}
    private static ObjectNode action(JsonNode s,String id) {for(JsonNode a:flat(s.path("actions"))) if(a.path("id").asText().equals(id)) return (ObjectNode)a;throw new AssertionError(id);}
    private static ObjectNode slots(JsonNode s,String id) {return (ObjectNode)action(s,id).path("request").path("slots");}
    private static void drop(ObjectNode s,String field,java.util.function.Predicate<JsonNode> which) {ArrayNode a=(ArrayNode)s.path(field);for(int i=a.size()-1;i>=0;i--) if(which.test(a.get(i))) a.remove(i);}
    private static JsonNode alias(String a) {return Json.parse("{\"$alias\":\""+a+"\"}");}
    private static ObjectNode evidence(ObjectNode f,String alias) {for(JsonNode e:f.path("evidence")) if(e.path("alias").asText().equals(alias)) return (ObjectNode)e;throw new AssertionError(alias);}
    /** Pins the outcome of an action to APPLIED (removing its error-code and outcome pins). */
    private static void applied(ObjectNode s,String id) {
        drop(s,"assertions",x->x.path("source").path("actionId").asText().equals(id) && Set.of("/response/outcome","/response/error/code").contains(x.path("source").path("pointer").asText()));
        ((ArrayNode)s.path("assertions")).add(Json.parse("{\"id\":\"round11-"+id+"-applied\",\"op\":\"equals\",\"source\":{\"actionId\":\""+id+"\",\"pointer\":\"/response/outcome\"},\"expected\":\"APPLIED\"}"));
    }
    private void rejects(String key,List<String> problems) {assertTrue(problems.stream().anyMatch(p->p.contains(key)),key+": "+problems);}
    private List<String> run(Rule rule,String caseId,String subId,Consumer<ObjectNode> changeSub,Consumer<ObjectNode> changeFixture) throws Exception {
        JsonNode c=caseJson(caseId);ObjectNode s=sub(c,subId).deepCopy();changeSub.accept(s);
        ObjectNode fixture=(ObjectNode)Json.read(root.resolve(s.path("fixtureRef").asText()));changeFixture.accept(fixture);
        Files.createDirectories(root.resolve("verification/harness/target"));
        Path p=Files.createTempFile(root.resolve("verification/harness/target"),"round11-fixture-",".json");Json.write(p,fixture);
        s.put("fixtureRef",root.relativize(p).toString());
        return rule.apply(new ContractValidator(root),only(c,s));
    }
    private void clean(Rule rule) throws Exception {ContractValidator v=new ContractValidator(root);for(String id:CASES) assertEquals(List.of(),rule.apply(v,caseJson(id)),id);}

    // NF1: installed facts are recorded no later than the starting clock; late-known evidence is read only after its recording.
    @Test void installedFactsAreRecordedNoLaterThanTheStartingClock() throws Exception {
        Rule rule=ContractValidator::fixtureRecordTimeProblems;clean(rule);
        // The contract pins option 1 of the review: recording at or before asOf, knownAt only the default query knowledge.
        JsonNode installation=Json.read(root.resolve("contracts/execution-preconditions.json")).path("clock").path("installation");
        assertEquals("asOf",installation.path("recordedAtNotAfter").asText());assertEquals("knownAt",installation.path("defaultQueryKnowledge").asText());
        // The review's shape: T05 manager-disposition reserves at 09:00:00 with knownAt 09:00:01; with installation at asOf the
        // same-second fixture is clean, but a clock control that rewinds before the installation is refused.
        ObjectNode rewind=(ObjectNode)Json.parse("{\"id\":\"round11-rewind\",\"kind\":\"control\",\"control\":{\"type\":\"clock\",\"operation\":\"advanceTo\",\"parameters\":{\"instant\":\"2026-10-07T08:59:59Z\"}}}");
        rejects("before the fixture is installed at the starting clock",run(rule,"T05","manager-disposition",s->((ArrayNode)s.path("actions")).insert(1,rewind),NONE));
        // T17 post-dispatch-expiry before round 11: the delivery at clock 09:06:00 names delivery-20 recorded at 09:06:01.
        rejects("names evidence delivery-20, which the fixture records later",run(rule,"T17","post-dispatch-expiry",NONE,f->evidence(f,"delivery-20").put("recordedAt","2026-10-07T09:06:01Z")));
        // Evidence that occurs after asOf cannot be installation knowledge (recorded at asOf, before it occurred).
        rejects("inside the fixture knowledge window",run(rule,"T17","post-dispatch-expiry",NONE,f->evidence(f,"qc-proof").put("occurredAt","2026-10-07T09:00:01Z")));
        rejects("before it occurred",run(rule,"T17","post-dispatch-expiry",NONE,f->evidence(f,"delivery-20").put("occurredAt","2026-10-07T09:07:00Z")));
        // NF8 stays backlog: C2/T09 DOC uses are known open gaps, not problems, and a stale backlog entry is refused.
        ContractValidator v=new ContractValidator(root);
        assertEquals(1,v.fixtureRecordTimeKnownOpen(caseJson("C2")).stream().map(x->x.split(":")[0]).distinct().count());
        assertTrue(v.fixtureRecordTimeKnownOpen(caseJson("C2")).stream().allMatch(x->x.startsWith("KNOWN_OPEN C2/cumulative-versus-state/DOC")));
        assertEquals(2,v.fixtureRecordTimeKnownOpen(caseJson("T09")).stream().map(x->x.split(":")[0]).distinct().count());
        rejects("no action reads it before its recording any more",run(rule,"C2","cumulative-versus-state",NONE,f->evidence(f,"DOC").put("occurredAt","2026-10-05T09:00:00Z").put("recordedAt","2026-10-05T09:00:00Z")));
        // An installed pick is recorded no later than asOf (V3 dispatch-first picks at asOf).
        rejects("is after the fixture clock asOf",run(ContractValidator::pickBeforeDispatchProblems,"V3","dispatch-first",NONE,f->{
            for(JsonNode a:f.path("aliases")) if(a.has("pickedAt")) ((ObjectNode)a).put("pickedAt","2026-10-07T09:00:01Z");
            for(JsonNode a:f.path("baseline").path("allocations")) if(a.has("pickedAt")) ((ObjectNode)a).put("pickedAt","2026-10-07T09:00:01Z");}));
    }

    // NF2: an authorized reserve never double-books a physical interval or a sales line held by a fixture or runtime allocation.
    @Test void reservesDoNotDoubleBookFixtureOrRuntimeAllocations() throws Exception {
        Rule rule=ContractValidator::reserveCapacityProblems;clean(rule);
        // The review's shape: C3/V4 reserve fixture with ALLOCATION (EXECUTABLE, A20 20 BOX, all of SALE-LINE 20).
        Consumer<ObjectNode> heldA20=f->((ObjectNode)f.path("baseline").path("priorEntities")).set("ALLOCATION",Json.parse("{\"revision\":1,\"segmentAlias\":\"A20\",\"quantity\":\"20\",\"unit\":\"BOX\",\"state\":\"EXECUTABLE\",\"orderLineAlias\":\"SALE-LINE\"}"));
        for(String route:List.of("api","mcp","worker")) {
            List<String> p=run(rule,"C3",route+"-reserveQuantity",NONE,heldA20);
            rejects("overlaps the EXECUTABLE allocation ALLOCATION (no startQuantity",p);rejects("SALES_LINE_QUANTITY_EXCEEDED",p);
        }
        for(String sid:List.of("direct-allocation","batch-allocation","blob-allocation","management-allocation")) rejects("overlaps the EXECUTABLE allocation ALLOCATION",run(rule,"V4",sid,NONE,heldA20));
        // A SUSPENDED fixture allocation still holds the interval; a CONSUMED one only the line.
        rejects("overlaps the SUSPENDED allocation",run(rule,"C3","api-reserveQuantity",NONE,heldA20.andThen(f->((ObjectNode)f.path("baseline").path("priorEntities").path("ALLOCATION")).put("state","SUSPENDED"))));
        List<String> consumed=run(rule,"C3","api-reserveQuantity",NONE,heldA20.andThen(f->((ObjectNode)f.path("baseline").path("priorEntities").path("ALLOCATION")).put("state","CONSUMED")));
        assertTrue(consumed.stream().noneMatch(p->p.contains("overlaps")),consumed.toString());rejects("SALES_LINE_QUANTITY_EXCEEDED",consumed);
        // V2 reserve-commits-first before round 11: ALLOC 40 of A60 without a coordinate, the winner at the default start 0.
        rejects("overlaps the EXECUTABLE allocation ALLOC",run(rule,"V2","reserve-commits-first",s->slots(s,"winner-call").remove("startQuantity"),NONE));
        rejects("overlaps the EXECUTABLE allocation ALLOC (no startQuantity",run(rule,"V2","reserve-commits-first",NONE,f->{
            ((ObjectNode)f.path("aliases").path("ALLOC")).remove("startQuantity");for(JsonNode a:f.path("baseline").path("allocations")) ((ObjectNode)a).remove("startQuantity");}));
        rejects("SALES_LINE_QUANTITY_EXCEEDED",run(rule,"V2","reserve-commits-first",s->slots(s,"winner-call").set("orderLineId",alias("ORDER")),NONE));
        rejects("Reservation exceeds physical scope",run(rule,"V2","reserve-commits-first",s->((ObjectNode)slots(s,"winner-call").path("startQuantity")).put("value","50"),NONE));
        // A runtime allocation holds its interval too: T17's second reservation of the same 60, if pinned APPLIED.
        rejects("overlaps the EXECUTABLE allocation $reserve",run(rule,"T17","reserve-pick-dispatch",s->applied(s,"second-reserve"),NONE));
        rejects("SALES_LINE_QUANTITY_EXCEEDED",run(rule,"T17","reserve-pick-dispatch",s->{applied(s,"second-reserve");slots(s,"second-reserve").set("orderLineId",Json.parse("{\"$result\":{\"actionId\":\"order\",\"pointer\":\"/response/line/id\"}}"));},NONE));
        // The pinned negative itself (REJECTED INSUFFICIENT_ELIGIBLE_QUANTITY) is not an expected application.
        assertEquals(List.of(),run(rule,"T17","reserve-pick-dispatch",NONE,NONE));
    }
}
