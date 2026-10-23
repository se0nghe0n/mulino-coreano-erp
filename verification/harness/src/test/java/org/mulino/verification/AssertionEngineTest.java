package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class AssertionEngineTest {
    private Map<String,JsonNode> results;
    private final Map<String,JsonNode> assertions=new HashMap<>();
    private final AssertionEngine engine=new AssertionEngine();
    @BeforeEach void fixture() throws Exception {
        Path p=Path.of(System.getProperty("repo.root"),"verification/harness/src/test/resources/examples/HARNESS-EXAMPLE");
        results=new HashMap<>(); Json.read(p.resolve("canned-observations.json")).fields().forEachRemaining(e->results.put(e.getKey(),e.getValue()));
        for(JsonNode a:Json.read(p.resolve("case.json")).path("subcases").get(0).path("assertions")) assertions.put(a.path("id").asText(),a);
    }
    @Test void allKnownCannedObservationsMatchFixedIndependentExpectations() { assertions.values().forEach(a->engine.check(a,results)); }
    @Test void wrongObservedUnitFails() { ((ObjectNode)results.get("after").path("response").path("data")).put("unit","KG");assertThrows(AssertionError.class,()->engine.check(assertions.get("held-100"),results)); }
    @Test void wrongQuantityFails() { ((ObjectNode)results.get("after").path("response").path("data")).put("heldQuantity","80");assertThrows(AssertionError.class,()->engine.check(assertions.get("held-100"),results)); }
    @Test void duplicatePhysicalIdentityFailsEvenIfQuantitySumMatches() { ((ObjectNode)results.get("db-after").path("data").path("rawRows").path("segments").get(1)).put("physicalScope","A60-physical");assertThrows(AssertionError.class,()->engine.check(assertions.get("physical-unique"),results)); }
    @Test void missingHumanOwnerFails() { ((ObjectNode)results.get("db-after").path("data").path("rawRows").path("obligations").get(0)).remove("ownerAlias");assertThrows(AssertionError.class,()->engine.check(assertions.get("owner-qc"),results));assertThrows(AssertionError.class,()->engine.check(assertions.get("obligation-fields"),results)); }
    @Test void wrongDefinitionVersionFails() { ((ObjectNode)results.get("after").path("response")).put("definitionVersion","definition-v2");assertThrows(AssertionError.class,()->engine.check(assertions.get("version-v1"),results)); }
    @Test void unavailableObservationCannotBecomeZeroEffectPass() { results.put("after",StepResult.missing("after","missing observer").toJson());assertThrows(AssertionError.class,()->engine.check(assertions.get("held-100"),results)); }
    @Test void missingQuantityCannotCoerceToZero() { ((ObjectNode)results.get("after").path("response").path("data")).remove("heldQuantity");var a=assertions.get("held-100").deepCopy();((ObjectNode)a).put("expected","0");assertThrows(AssertionError.class,()->engine.check(a,results)); }
    @Test void unknownQuantityCannotCoerceToZero() { ((ObjectNode)results.get("after").path("response").path("data")).put("heldQuantity","UNKNOWN");assertThrows(AssertionError.class,()->engine.check(assertions.get("held-100"),results)); }
    @Test void scopeIncompleteFailsBeforeAnyBusinessAssertion() { ((ObjectNode)results.get("after").path("provenance")).put("scopeComplete",false);assertThrows(AssertionError.class,()->engine.check(assertions.get("held-100"),results)); }
    @Test void wrongDeadlineFails() { ((ObjectNode)results.get("db-after").path("data").path("rawRows").path("obligations").get(0)).put("nextCheckAt","2026-10-08T10:00:00Z");assertThrows(AssertionError.class,()->engine.check(assertions.get("next-check"),results)); }
    @Test void absentNeedsObservedContainerParentNotNullOrScalar() {
        var a=Json.parse("{\"op\":\"absent\",\"source\":{\"actionId\":\"after\",\"pointer\":\"/response/parent/missing\"},\"expected\":true}");
        ObjectNode response=(ObjectNode)results.get("after").path("response");
        response.set("parent",Json.object());assertDoesNotThrow(()->engine.check(a,results));
        for(JsonNode invalid:List.of(Json.MAPPER.nullNode(),Json.MAPPER.valueToTree("scalar"),Json.MAPPER.valueToTree(0))) {
            response.set("parent",invalid);assertThrows(AssertionError.class,()->engine.check(a,results));
        }
    }
    @Test void deltaMustCompareActualBaselineUnitEvenForZeroDelta() {
        ObjectNode a=(ObjectNode)assertions.values().stream().filter(x->x.path("op").asText().equals("decimalDelta")).findFirst().orElseThrow().deepCopy();
        a.put("expected","0");
        JsonNode current=engine.select(a.path("source"),results,false);
        var baseline=a.path("baselineUnitSource");ObjectNode parent=(ObjectNode)results.get(baseline.path("actionId").asText()).at("/response/data");
        String quantityField=a.path("baseline").path("pointer").asText().substring("/response/data/".length());parent.set(quantityField,current);
        assertDoesNotThrow(()->engine.check(a,results));
        parent.put("unit","KG");assertThrows(AssertionError.class,()->engine.check(a,results));
    }
    @Test void countAddedKeepsFullOpeningRowsAndRejectsEveryUnobservedOrChangedBaseline() {
        var a=Json.object().put("op","countAdded").put("expected",0);
        a.set("source",Json.object().put("actionId","db-after").put("pointer","/data/rawRows/movements"));
        a.set("baseline",Json.object().put("actionId","db-before").put("pointer","/data/rawRows/movements"));
        var opening=Json.object().put("id","actual-opening").put("kind","INITIAL_BALANCE").put("quantity","100");
        var before=Json.object().put("driverStatus","EXECUTED");before.set("provenance",Json.object().put("scopeComplete",true));
        before.set("data",Json.object().set("rawRows",Json.object().set("movements",Json.array().add(opening))));
        results.put("db-before",before);results.put("db-after",before.deepCopy());
        assertDoesNotThrow(()->engine.check(a,results));
        for(String mutation:List.of("add","replace","delete","change","duplicate","missingId","missingField","notExecuted")) {
            var after=before.deepCopy();var rows=(com.fasterxml.jackson.databind.node.ArrayNode)after.at("/data/rawRows/movements");
            switch(mutation) {
                case "add" -> rows.add(opening.deepCopy().put("id","new-dispatch").put("kind","DISPATCH"));
                case "replace" -> ((ObjectNode)rows.get(0)).put("id","other-opening");
                case "delete" -> rows.removeAll();
                case "change" -> ((ObjectNode)rows.get(0)).put("quantity","99");
                case "duplicate" -> rows.add(rows.get(0).deepCopy());
                case "missingId" -> ((ObjectNode)rows.get(0)).remove("id");
                case "missingField" -> ((ObjectNode)after.at("/data/rawRows")).remove("movements");
                case "notExecuted" -> after.put("driverStatus","NOT_IMPLEMENTED");
            }
            results.put("db-after",after);assertThrows(AssertionError.class,()->engine.check(a,results),mutation);
        }
        results.put("db-after",before.deepCopy());((ObjectNode)before.at("/data/rawRows/movements/0")).remove("id");
        assertThrows(AssertionError.class,()->engine.check(a,results));
    }

}
