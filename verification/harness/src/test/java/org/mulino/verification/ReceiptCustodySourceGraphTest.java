package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Static custody validation only; never executes a receipt or alters a case fixture. */
final class ReceiptCustodySourceGraphTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private static final Consumer<ObjectNode> NONE=x->{};
    private ObjectNode scenario() throws Exception {
        JsonNode c=Json.read(root.resolve("verification/cases/T09/case.json"));
        return (ObjectNode)c.path("subcases").get(0).deepCopy();
    }
    private ObjectNode action(ObjectNode sub,String id) {
        for(JsonNode a:sub.path("actions"))if(a.path("id").asText().equals(id))return (ObjectNode)a;
        throw new AssertionError(id);
    }
    private ObjectNode slots(ObjectNode sub,String id) {return (ObjectNode)action(sub,id).path("request").path("slots");}
    private JsonNode ref(String action,String pointer) {
        return Json.object().set("$result",Json.object().put("actionId",action).put("pointer",pointer));
    }
    private ObjectNode alias(String name) {return Json.object().put("$alias",name);}
    private void moveAfter(ObjectNode sub,String moved,String after) {
        var rows=(ArrayNode)sub.path("actions");var value=action(sub,moved);
        for(int i=0;i<rows.size();i++)if(rows.get(i)==value){rows.remove(i);break;}
        for(int i=0;i<rows.size();i++)if(rows.get(i).path("id").asText().equals(after)){rows.insert(i+1,value);return;}
        throw new AssertionError(after);
    }
    private List<String> problems(Consumer<ObjectNode> mutate,Consumer<ObjectNode> fixtureMutation) throws Exception {
        var sub=scenario();mutate.accept(sub);
        var fixture=(ObjectNode)Json.read(root.resolve(sub.path("fixtureRef").asText()));fixtureMutation.accept(fixture);
        Path tmp=Files.createTempFile(root.resolve("verification/harness/target"),"custody-graph-",".json");
        try {
            Json.write(tmp,fixture);sub.put("fixtureRef",root.relativize(tmp).toString());
            return new ContractValidator(root).receiptCustodyProblems(Json.object().put("caseId","T09").set("subcases",Json.array().add(sub)));
        } finally {Files.deleteIfExists(tmp);}
    }
    private void rejects(String text,List<String> errors) {assertTrue(errors.stream().anyMatch(x->x.contains(text)),errors.toString());}
    @Test void bothAuthoredTransportReceiptsUseTheirProvisionalPhysicalInputs() throws Exception {
        assertEquals(List.of(),problems(NONE,NONE));
    }
    @Test void directReceiptCannotBorrowItsRangeAliasOrAnotherActionsTransport() throws Exception {
        rejects("direct receipt",problems(s->slots(s,"receive60-provisional").remove("transitSegmentId"),NONE));
        // An unused segment alias on the final command is not a transport source.
        rejects("direct receipt",problems(s->{slots(s,"receive60-provisional").remove("transitSegmentId");slots(s,"receive60").set("segmentId",alias("A60"));},NONE));
        rejects("earlier sequential receiveProvisional",problems(s->slots(s,"receive60").set("receiptId",ref("cumulative","/response/effects/receiptId")),NONE));
    }
    @Test void receiptResultMustBeStrictAndFromTheRightCapability() throws Exception {
        for(String pointer:List.of("/response/id","/response/receiptId","/response/effects/segmentId"))
            rejects("earlier sequential receiveProvisional",problems(s->slots(s,"receive60").set("receiptId",ref("receive60-provisional",pointer)),NONE));
        rejects("earlier sequential receiveProvisional",problems(s->((ObjectNode)slots(s,"receive60").path("receiptId")).put("extra",true),NONE));
        rejects("earlier sequential receiveProvisional",problems(s->((ObjectNode)slots(s,"receive60").path("receiptId").path("$result")).put("extra",true),NONE));
        rejects("earlier sequential receiveProvisional",problems(s->action(s,"receive60-provisional").put("capabilityId","recordActivity"),NONE));
        rejects("earlier sequential receiveProvisional",problems(s->((ObjectNode)action(s,"receive60-provisional").path("request")).put("capabilityId","recordActivity"),NONE));
        rejects("earlier sequential receiveProvisional",problems(s->((ObjectNode)action(s,"receive60-provisional").path("request")).put("intentKind","COMMAND"),NONE));
    }
    @Test void futureParallelStartedAndAmbiguousResultsDoNotEstablishTransport() throws Exception {
        rejects("earlier sequential receiveProvisional",problems(s->moveAfter(s,"receive60-provisional","receive60"),NONE));
        rejects("earlier sequential receiveProvisional",problems(s->{
            var original=action(s,"receive60-provisional").deepCopy();var holder=action(s,"receive60-provisional");holder.removeAll();
            holder.put("id","started-provisional").put("kind","start").set("call",original);
        },NONE));
        rejects("earlier sequential receiveProvisional",problems(s->{
            var original=action(s,"receive60-provisional").deepCopy();var holder=action(s,"receive60-provisional");holder.removeAll();
            holder.put("id","parallel-provisional").put("kind","parallel").set("branches",Json.array().add(Json.object().set("actions",Json.array().add(original))));
        },NONE));
        rejects("earlier sequential receiveProvisional",problems(s->((ArrayNode)s.path("actions")).add(action(s,"receive60-provisional").deepCopy()),NONE));
    }
    @Test void transportRequiresExactQuantityLotPlaceAndNoInventedCustodian() throws Exception {
        rejects("differs from leaf A60 60 BOX",problems(s->slots(s,"receive60-provisional").put("quantity","59"),NONE));
        rejects("differs from provisional lotId",problems(s->slots(s,"receive60").set("lotId",alias("P")),NONE));
        rejects("lotId P differs",problems(s->{slots(s,"receive60-provisional").set("lotId",alias("P"));slots(s,"receive60").set("lotId",alias("P"));},NONE));
        rejects("destination PORT",problems(s->slots(s,"receive60-provisional").set("placeId",alias("PORT")),f->((ObjectNode)f.path("aliases")).set("PORT",Json.object().put("type","Place").put("kind","EXTERNAL_PORT"))));
        rejects("carries receivingCustodianId",problems(s->slots(s,"receive60").set("receivingCustodianId",alias("warehouse")),NONE));
        rejects("not an internal custodian",problems(NONE,f->{for(JsonNode row:f.path("baseline").path("segments"))if(row.path("alias").asText().equals("A60"))((ObjectNode)row).put("custodianAlias","ORG");}));
    }
    @Test void malformedAndUnrelatedTransitReferencesAreNotAcceptedAsLeaves() throws Exception {
        rejects("actual fixture leaf",problems(s->slots(s,"receive60-provisional").set("transitSegmentId",Json.object().put("$alias","A60").put("extra",true)),NONE));
        rejects("actual fixture leaf",problems(s->slots(s,"receive60-provisional").set("transitSegmentId",alias("unknown")),NONE));
        rejects("actual fixture leaf",problems(s->slots(s,"receive60-provisional").set("transitSegmentId",ref("receive40-provisional","/response/effects/segmentId")),NONE));
    }
    private void split(ObjectNode s) {
        var split=Json.object().put("id","split-source").put("kind","invoke").put("capabilityId","splitQuantity");
        split.set("request",Json.object().put("capabilityId","splitQuantity").set("slots",Json.object().put("unit","BOX").set("segmentId",alias("A60"))));
        ((ObjectNode)split.path("request").path("slots")).set("quantities",Json.array().add("30").add("30"));
        var actions=(ArrayNode)s.path("actions");for(int i=0;i<actions.size();i++)if(actions.get(i).path("id").asText().equals("receive60-provisional")){actions.insert(i,split);break;}
        slots(s,"receive60-provisional").put("quantity","30").set("transitSegmentId",ref("split-source","/response/children/0/segmentId"));
    }
    @Test void exactEarlierSplitChildWorksButFutureAndMalformedChildDoNot() throws Exception {
        assertEquals(List.of(),problems(this::split,NONE));
        rejects("actual fixture leaf",problems(s->{split(s);moveAfter(s,"split-source","receive60-provisional");},NONE));
        rejects("actual fixture leaf",problems(s->{split(s);slots(s,"receive60-provisional").set("transitSegmentId",ref("split-source","/response/children/0/id"));},NONE));
    }
}
