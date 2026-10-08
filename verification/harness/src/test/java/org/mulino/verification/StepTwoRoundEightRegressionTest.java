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

/** Step 2 closure review 5 (step2r round 8): counterexamples for the confirmed findings. Preparation checks and captured selftest ports only. */
final class StepTwoRoundEightRegressionTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private static final List<String> CASES=new ArrayList<>();
    static {
        for(int i=1;i<=26;i++) CASES.add(String.format("T%02d",i));
        for(int i=1;i<=5;i++) CASES.add("C"+i);
        for(int i=1;i<=8;i++) CASES.add("V"+i);
        CASES.add("E1");CASES.add("E2");
    }
    private static final Consumer<ObjectNode> NONE=x->{};
    private JsonNode caseJson(String id) throws Exception {return Json.read(root.resolve("verification/cases/"+id+"/case.json"));}
    private static ObjectNode sub(JsonNode c,String id) {for(JsonNode s:c.path("subcases")) if(s.path("id").asText().equals(id)) return (ObjectNode)s;throw new AssertionError(id);}
    private static ObjectNode only(JsonNode c,ObjectNode s) {ObjectNode copy=((ObjectNode)c).deepCopy();copy.set("subcases",Json.array().add(s));return copy;}
    private static ObjectNode action(JsonNode s,String id) {for(JsonNode a:s.path("actions")) if(a.path("id").asText().equals(id)) return (ObjectNode)a;throw new AssertionError(id);}
    private static ObjectNode slots(JsonNode s,String id) {return (ObjectNode)action(s,id).path("request").path("slots");}
    private static ObjectNode segmentRow(JsonNode fixture,String alias) {for(JsonNode r:fixture.path("baseline").path("segments")) if(r.path("alias").asText().equals(alias)) return (ObjectNode)r;throw new AssertionError(alias);}
    private static void dropAssertion(ObjectNode s,String id) {ArrayNode a=(ArrayNode)s.path("assertions");for(int i=a.size()-1;i>=0;i--) if(a.get(i).path("id").asText().equals(id)) a.remove(i);}
    private List<String> problems(String caseId,String subId,Consumer<ObjectNode> changeSub,Consumer<ObjectNode> changeFixture) throws Exception {
        JsonNode c=caseJson(caseId);ObjectNode s=sub(c,subId).deepCopy();changeSub.accept(s);
        ObjectNode fixture=(ObjectNode)Json.read(root.resolve(s.path("fixtureRef").asText()));changeFixture.accept(fixture);
        Path p=Files.createTempFile(root.resolve("verification/harness/target"),"round8-fixture-",".json");Json.write(p,fixture);
        s.put("fixtureRef",root.relativize(p).toString());
        return new ContractValidator(root).receiptCustodyProblems(only(c,s));
    }
    private void rejects(String key,List<String> problems) {assertTrue(problems.stream().anyMatch(p->p.contains(key)),key+": "+problems);}
    private static final ObjectMapper CANONICAL=new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true);
    private static void nameCustodian(ObjectNode fixture,String doc,String custodian) {
        ObjectNode content=(ObjectNode)fixture.path("aliases").path(doc).path("fixtureContent");content.put("receivingCustodianAlias",custodian);
        try {String sha=Json.sha256Text(CANONICAL.writeValueAsString(Json.MAPPER.treeToValue(content,Object.class)));
            for(JsonNode e:fixture.path("evidence")) if(e.path("alias").asText().equals(doc)) ((ObjectNode)e).put("sha256",sha);}
        catch(Exception e) {throw new AssertionError(e);}
    }

    // P2: a transit receipt names an identified, exact-quantity leaf at a TRANSIT place (split partial cargo first).
    @Test void transitReceiptsConsumeAnExactIdentifiedTransitLeaf() throws Exception {
        ContractValidator v=new ContractValidator(root);
        for(String id:CASES) assertEquals(List.of(),v.receiptCustodyProblems(caseJson(id)),id);
        // The closure review 5 shapes, restored one by one.
        rejects("is at PORT (kind EXTERNAL_PORT)",problems("C2","cumulative-versus-state",NONE,f->{
            ((ObjectNode)f.path("aliases")).set("PORT",Json.parse("{\"type\":\"Place\",\"kind\":\"EXTERNAL_PORT\"}"));segmentRow(f,"A60").put("locationAlias","PORT");}));
        rejects("is at PORT (kind EXTERNAL_PORT)",problems("T09","cumulative-versus-state",NONE,f->{
            ((ObjectNode)f.path("aliases")).set("PORT",Json.parse("{\"type\":\"Place\",\"kind\":\"EXTERNAL_PORT\"}"));segmentRow(f,"B40").put("locationAlias","PORT");}));
        rejects("is at PORT (kind EXTERNAL_PORT)",problems("T16","provisional-holds",NONE,f->{
            ((ObjectNode)f.path("aliases")).set("PORT",Json.parse("{\"type\":\"Place\",\"name\":\"IT-port\",\"kind\":\"EXTERNAL_PORT\"}"));((ObjectNode)f.path("aliases").path("TRANSIT60")).put("locationAlias","PORT");}));
        for(String sub:List.of("discrepancy-transit2","discrepancy-unobserved2"))
            rejects("differs from leaf Q100 100 BOX",problems("T14",sub,s->slots(s,"receive98").set("existingSegmentId",Json.parse("{\"value\":{\"$alias\":\"Q100\"},\"provenance\":\"CONTEXT\"}")),NONE));
        rejects("differs from leaf",problems("T14","discrepancy-transit2",s->((ObjectNode)slots(s,"split98").path("children").path("value").get(0)).put("quantity","97"),NONE));
        rejects("explicit children entry",problems("T14","discrepancy-transit2",s->slots(s,"receive98").set("existingSegmentId",Json.parse("{\"value\":{\"$result\":{\"actionId\":\"split98\",\"pointer\":\"/response/children/0/id\"}}}")),NONE));
        rejects("lotId",problems("C2","cumulative-versus-state",s->slots(s,"receive60").set("lotId",Json.parse("{\"$alias\":\"P\"}")),NONE));
        rejects("destination TRANSIT is not a INTERNAL_STORAGE",problems("C2","cumulative-versus-state",s->slots(s,"receive60").set("placeId",Json.parse("{\"$alias\":\"TRANSIT\"}")),NONE));
        rejects("INDISTINGUISHABLE_MIXTURE",problems("C2","cumulative-versus-state",NONE,f->segmentRow(f,"A60").put("identifiability","INDISTINGUISHABLE_MIXTURE")));
        rejects("not an internal custodian, but its stock is used by",problems("C2","cumulative-versus-state",NONE,f->segmentRow(f,"A60").put("custodianAlias","ORG")));
        // What the fixtures now say.
        for(String c:List.of("C2","T09")) {
            JsonNode f=Json.read(root.resolve("verification/cases/"+c+"/fixtures/cumulative-versus-state.json"));
            assertEquals("TRANSIT",f.at("/aliases/TRANSIT/kind").asText(),c);assertTrue(f.at("/aliases/PORT").isMissingNode(),c);
            for(String leaf:List.of("A60","B40")) {assertEquals("TRANSIT",segmentRow(f,leaf).path("locationAlias").asText());assertEquals("warehouse",segmentRow(f,leaf).path("custodianAlias").asText());}
        }
        JsonNode t16=Json.read(root.resolve("verification/cases/T16/fixtures/provisional-holds.json"));
        assertEquals("TRANSIT",t16.at("/aliases/TRANSIT60/locationAlias").asText());assertEquals("TRANSIT",t16.at("/aliases/TRANSIT/kind").asText());assertEquals("CUSTODIAN",t16.at("/aliases/TRANSIT60/custodianAlias").asText());
        for(JsonNode a:sub(caseJson("T16"),"provisional-holds").path("assertions")) if(a.path("id").asText().equals("receipt-transit-double-creation-7"))
            assertEquals("TRANSIT",a.at("/source/where/locationId/$alias").asText(),"no active stock is left at the leaf's (TRANSIT) place");
        for(String s:List.of("discrepancy-transit2","discrepancy-unobserved2")) {
            ObjectNode sub=sub(caseJson("T14"),s);List<String> ids=new ArrayList<>();for(JsonNode a:sub.path("actions")) ids.add(a.path("id").asText());
            assertTrue(ids.indexOf("split98")<ids.indexOf("receive98"),s);
            assertEquals("/response/children/RECEIVED98/segmentId",slots(sub,"receive98").at("/existingSegmentId/value/$result/pointer").asText());
            for(JsonNode x:sub.path("assertions")) if(List.of("received98","physical-total100","transit2").contains(x.path("id").asText())) assertTrue(x.at("/source/where/active").asBoolean(),x.path("id").asText()+" reads active rows only");
        }
        assertEquals("/response/children/REMAINDER2/segmentId",slots(sub(caseJson("T14"),"discrepancy-transit2"),"transit2").at("/segmentId/value/$result/pointer").asText());
    }

    // P3 (a): a declared custody negative names the product's first failing check, pins the outcome and proves zero effect.
    @Test void declaredCustodyNegativeIsTheProductsFirstFailingCheck() throws Exception {
        String neg="receipt-custody-unverified";
        assertEquals(List.of(),problems("E1",neg,NONE,NONE));
        ObjectNode s=sub(caseJson("E1"),neg);
        assertEquals("EVIDENCE_UNVERIFIED",action(s,"receipt60").path("custodyControl").asText());
        assertEquals("procurement",slots(s,"receipt60").at("/receivingCustodianId/$alias").asText());
        // Without the declaration the same receipt is an ordinary preparation problem (the original names receiver).
        rejects("names receiving custodian receiver, the slot procurement",problems("E1",neg,x->action(x,"receipt60").remove("custodyControl"),NONE));
        rejects("is not the product's first failing",problems("E1",neg,x->action(x,"receipt60").put("custodyControl","SCOPE_INELIGIBLE"),NONE));
        // step2r round 9: FORBIDDEN is a defined value (a slot outside the confirming organization's actors); TYPE_INVALID is not.
        rejects("is not one of",problems("E1",neg,x->action(x,"receipt60").put("custodyControl","TYPE_INVALID"),NONE));
        rejects("must pin /response/outcome",problems("E1",neg,x->dropAssertion(x,"custody-unverified-outcome"),NONE));
        rejects("must pin /response/outcome",problems("E1",neg,x->dropAssertion(x,"custody-unverified-code"),NONE));
        rejects("must assert zero effect",problems("E1",neg,x->{for(String id:List.of("custody-unverified-no-stock-at-W","custody-unverified-no-custody","custody-unverified-no-receipt")) dropAssertion(x,id);},NONE));
        ObjectNode full=sub(caseJson("E1"),"full-flow-quantities");
        rejects("creates no stock",problems("E1",neg,x->{for(String id:List.of("split60","reserve")) ((ArrayNode)x.path("actions")).add(action(full,id).deepCopy());},NONE));
        // A valid, evidenced slot is no negative; nor is the field on another capability.
        rejects("none: the slot is valid and evidenced",problems("E1","full-flow-quantities",x->action(x,"receipt60").put("custodyControl","EVIDENCE_UNVERIFIED"),NONE));
        rejects("applies only to a confirmReceipt",problems("E1",neg,x->action(x,"shipment").put("custodyControl","EVIDENCE_UNVERIFIED"),NONE));
        // The other two declared negatives are accepted when they are the first failing check and pinned.
        Consumer<ObjectNode> pin=x->{for(JsonNode a:x.path("assertions")) {
            if(a.path("id").asText().equals("custody-unverified-outcome")) ((ObjectNode)a).put("expected","REJECTED");
            if(a.path("id").asText().equals("custody-unverified-code")) ((ObjectNode)a).put("expected","SCOPE_INELIGIBLE");}};
        assertEquals(List.of(),problems("E1",neg,x->{pin.accept(x);action(x,"receipt60").put("custodyControl","SCOPE_INELIGIBLE");slots(x,"receipt60").set("receivingCustodianId",Json.parse("{\"$alias\":\"observer\"}"));},NONE));
        // EVIDENCE_CONFLICT needs two verification-basis originals of the receipt's canonical occurrence that disagree (step2r
        // round 9, closure review 6 P3): delivery-proof as a verifiedEvidenceIds basis naming procurement beside warehouse-60
        // naming receiver. The same document cited only in request evidenceRefs is a witness the product never reads, so
        // there the first failing check stays EVIDENCE_UNVERIFIED.
        Consumer<ObjectNode> conflict=x->{action(x,"receipt60").put("custodyControl","EVIDENCE_CONFLICT");
            for(JsonNode a:x.path("assertions")) if(a.path("id").asText().equals("custody-unverified-code")) ((ObjectNode)a).put("expected","EVIDENCE_CONFLICT");};
        assertEquals(List.of(),problems("E1",neg,x->{conflict.accept(x);slots(x,"receipt60").set("verifiedEvidenceIds",Json.parse("[{\"$alias\":\"delivery-proof\"}]"));},
            f->nameCustodian(f,"delivery-proof","procurement")));
        rejects("is not the product's first failing receiving-custody check (EVIDENCE_UNVERIFIED",problems("E1",neg,conflict,f->nameCustodian(f,"delivery-proof","procurement")));
        // Originals: verifiedEvidenceIds count, a runtime-attached JSON original counts, and the custodian's organization is checked.
        assertEquals(List.of(),problems("E1","full-flow-quantities",x->{ObjectNode sl=slots(x,"receipt60");sl.set("verifiedEvidenceIds",Json.array().add(sl.path("evidenceId")));sl.remove("evidenceId");},NONE));
        rejects("names receiving custodian qc",problems("E1","full-flow-quantities",x->slots(x,"receipt60").set("verifiedEvidenceIds",Json.parse("[{\"$alias\":\"delivery-proof\"}]")),f->nameCustodian(f,"delivery-proof","qc")));
        String content="{\"receivingCustodianAlias\":\"qc\"}";
        Consumer<ObjectNode> attach=x->{ObjectNode a=Json.object();a.put("id","attach").put("kind","invoke").put("actorRef","receiver").put("route","api").put("capabilityId","attachEvidence");
            ObjectNode doc=Json.object();doc.put("content",content).put("sha256",Json.sha256Text(content)).put("mediaType","application/json");
            ObjectNode sl=Json.object();sl.set("document",doc);ObjectNode req=Json.object();req.put("intentKind","RECORD").put("capabilityId","attachEvidence");req.set("slots",sl);a.set("request",req);a.set("evidenceRefs",Json.parse("[\"attach:api-response\"]"));
            ArrayNode actions=(ArrayNode)x.path("actions");int at=0;for(int i=0;i<actions.size();i++) if(actions.get(i).path("id").asText().equals("receipt60")) at=i;actions.insert(at,a);
            slots(x,"receipt60").set("evidenceIds",Json.parse("[{\"$result\":{\"actionId\":\"attach\",\"pointer\":\"/response/evidenceId\"}}]"));};
        rejects("names receiving custodian qc",problems("E1","full-flow-quantities",attach,NONE));
        rejects("runtime original attach sha256",problems("E1","full-flow-quantities",x->{attach.accept(x);((ObjectNode)action(x,"attach").at("/request/slots/document")).put("sha256","0".repeat(64));},NONE));
        rejects("belongs to organization ORG-B",problems("E1","full-flow-quantities",NONE,f->((ObjectNode)f.path("actors").path("receiver")).put("organizationAlias","ORG-B")));
    }

    // P3 (b): NO_TASK covers two natural ticks and its extractor reads the rows after the watcher completed.
    @Test void noTaskCoversTwoTicksAndReadsRowsUpToCompletion() throws Exception {
        StepTwoRoundSevenRegressionTest seven=new StepTwoRoundSevenRegressionTest();
        ContractValidator v=new ContractValidator(root);
        HostObservationValidatorTest.Capture two=seven.noTask("2026-10-07T00:00:00Z","2026-10-07T00:00:02Z",1);
        HostObservationValidator.validate(v,two.control(),two.result(),false,null);
        // The closure review 5 shape: completedAt = observeFrom + one tick is no longer enough.
        assertTrue(seven.rejection(seven.noTask("2026-10-07T00:00:00Z","2026-10-07T00:00:01Z",1)).contains("ended before 2 natural ticks"));
        assertTrue(seven.rejection(seven.noTask("2026-10-07T00:00:00Z","2026-10-07T00:00:02Z",1,"2026-10-07T00:00:01Z")).contains("extractor"));
        assertTrue(seven.rejection(seven.noTask("2026-10-07T00:00:00Z","2026-10-07T00:00:30Z",16)).contains("naturalTickSeconds must be"));
        JsonNode t26=caseJson("T26");ObjectNode loop=sub(t26,"lot-expiry-autonomous-loop").deepCopy();
        assertEquals(List.of(),v.runtimeProfileProblems(only(t26,loop)));
        ObjectNode fixture=(ObjectNode)Json.read(root.resolve(loop.path("fixtureRef").asText()));((ObjectNode)fixture.at("/baseline/runtimeProfile")).put("tickSeconds",16);
        Path p=Files.createTempFile(root.resolve("verification/harness/target"),"round8-fixture-",".json");Json.write(p,fixture);
        loop.put("fixtureRef",root.relativize(p).toString());
        assertTrue(v.runtimeProfileProblems(only(t26,loop)).stream().anyMatch(x->x.contains("observationWindowSeconds/2")));
    }

    // P3 (c): T13 asserts the dispatch, return and move outcomes and the segments before the sameAs contribution checks.
    @Test void t13ObservesTheReturnAndTheMoveBeforeTheirContributionChecks() throws Exception {
        ObjectNode s=sub(caseJson("T13"),"partial-excess-return-relocation");
        List<String> actions=new ArrayList<>(),assertions=new ArrayList<>();
        for(JsonNode a:s.path("actions")) actions.add(a.path("id").asText());
        for(JsonNode a:s.path("assertions")) assertions.add(a.path("id").asText());
        for(String db:List.of("return-db","move-db")) {boolean seg=false;for(JsonNode x:action(s,db).at("/observation/sources")) seg|=x.asText().equals("segments");assertTrue(seg,db);}
        assertTrue(actions.indexOf("return-db")<actions.indexOf("split40") && actions.indexOf("split40")<actions.indexOf("move"));
        assertEquals("/response/children/MOVE20/segmentId",slots(s,"move").at("/segmentId/value/$result/pointer").asText(),"the whole-leaf move takes the split 20 child");
        for(String id:List.of("dispatch-sale-applied","return-applied","returned-at-W")) assertTrue(assertions.indexOf(id)>=0 && assertions.indexOf(id)<assertions.indexOf("return-no-new-contribution"),id);
        for(String id:List.of("move-applied","relocated-at-W-alt","left-at-W")) assertTrue(assertions.indexOf(id)>assertions.indexOf("return-no-new-contribution") && assertions.indexOf(id)<assertions.indexOf("relocation-no-new-contribution"),id);
        assertEquals(List.of(),new ContractValidator(root).receiptCustodyProblems(only(caseJson("T13"),s)));
    }
}
