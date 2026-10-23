package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mulino.verification.*;
import org.mulino.verification.modelbinding.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Deterministic recipe counterexamples; these are contract tests, never actual product acceptance. */
class NativeDerivedObservationTest {
    final Path root=Path.of(System.getProperty("repo.root"));
    final String org=UUID.randomUUID().toString(),item=UUID.randomUUID().toString(),place=UUID.randomUUID().toString(),segment=UUID.randomUUID().toString();
    final String instant="2026-10-07T09:00:00Z";
    ObjectNode capture(){var c=Json.object().put("asOf",instant).put("knownAt",instant);c.set("scope",Json.object().put("organizationId",org).put("installationId","installation").put("caseId","probe").put("turnId","turn-1"));c.set("snapshot",Json.object().put("id","3:8:"));c.set("rawRows",Json.object());c.set("sourceEvidence",Json.object());c.set("physicalCells",Json.array());return c;}
    ObjectNode parameters(JsonNode c){var p=Json.object();p.set("scope",c.path("scope"));p.set("businessScope",Json.object().put("itemId",item).put("placeId",place));p.set("bindings",Json.object());p.put("authorityAt",instant);return p;}
    ObjectNode row(String table,String id){var r=Json.object();definition(table).columns().forEach(k->r.putNull(k));r.put("organizationId",org).put("ID",id).put("createdAt",instant).put("recordedAt",instant);return r;}
    NativePhysicalCatalog.Table definition(String table){return NativePhysicalCatalog.TABLES.stream().filter(t->t.name().equals(table)).findFirst().orElseThrow();}
    void rows(ObjectNode c,String table,JsonNode...rows){String d=NativePhysicalObserver.dataset(table);((ObjectNode)c.path("rawRows")).set(d,Json.array().addAll(List.of(rows)));var q=Json.object().put("statementId","native-physical-"+d+"-v3").put("mappingVersion","model-binding-v1").put("sql",NativePhysicalCatalog.query(definition(table)));q.set("parameters",Json.object().put("organizationId",org));var e=Json.object().put("complete",true).put("rowPointer","/rawRows/"+d);e.set("sourceQuery",q);((ObjectNode)c.path("sourceEvidence")).set(d,e);}
    ObjectNode stock(){var c=capture();rows(c,"mulino_inventory_TradeItems",row("mulino_inventory_TradeItems",item).put("baseUnit","BOX"));rows(c,"mulino_inventory_Places",row("mulino_inventory_Places",place).put("kind","INTERNAL_STORAGE"));rows(c,"mulino_inventory_QuantitySegments",row("mulino_inventory_QuantitySegments",segment).put("itemId",item).put("placeId",place).put("quantity","60").put("unit","BOX").put("validFrom",instant));return c;}
    ObjectNode cell(ObjectNode c) throws Exception{return PhysicalFactRecipes.cell("state.inventory.onHand.value","inventory","onHand.value",PhysicalFactRecipes.compute("state.inventory.onHand.value",c,parameters(c)),0,root);}
    @Test void onHandIsRecomputedFromPhysicalUnitAndPlaceRatherThanSeededExpectation()throws Exception{var c=stock();var cell=cell(c);assertEquals("60",cell.path("value").asText());assertEquals("BOX",cell.path("unit").asText());assertDoesNotThrow(()->PhysicalFactRecipes.validate(c,cell,root));((ObjectNode)c.at("/rawRows/segments/0")).put("quantity","58");assertThrows(IllegalArgumentException.class,()->PhysicalFactRecipes.validate(c,cell,root));}
    @Test void tamperedValueMissingOperandUnknownRecipeAndWrongSnapshotAreRejected()throws Exception{for(String mutation:List.of("value","column","recipe","snapshot","unit","scope","input","query")){var c=stock();var cell=cell(c);switch(mutation){case "value"->cell.put("value","0");case "column"->((ObjectNode)c.at("/rawRows/segments/0")).remove("retiredAt");case "recipe"->((ObjectNode)cell.path("recipe")).put("id","state.anything");case "snapshot"->((ObjectNode)cell.path("recipe")).put("snapshotId","copied-api-hash");case "unit"->cell.put("unit","EA");case "scope"->((ObjectNode)c.at("/rawRows/segments/0")).put("organizationId",UUID.randomUUID().toString());case "input"->((ArrayNode)cell.at("/recipe/inputs")).remove(0);case "query"->((ObjectNode)c.at("/sourceEvidence/segments/sourceQuery")).put("sql",c.at("/sourceEvidence/segments/sourceQuery/sql").asText()+" AND false");}assertThrows(IllegalArgumentException.class,()->PhysicalFactRecipes.validate(c,cell,root),mutation);}}
    @Test void missingAndChangedRecipeSourceFail(@TempDir Path tmp)throws Exception{for(String path:PhysicalFactRecipes.SOURCES){Path copy=tmp.resolve(path);Files.createDirectories(copy.getParent());Files.copy(root.resolve(path),copy);}var c=stock();var cell=PhysicalFactRecipes.cell("state.inventory.onHand.value","inventory","onHand.value",PhysicalFactRecipes.compute("state.inventory.onHand.value",c,parameters(c)),0,tmp);assertDoesNotThrow(()->PhysicalFactRecipes.validate(c,cell,tmp));Path file=tmp.resolve(PhysicalFactRecipes.SOURCES.getFirst());Files.writeString(file,"changed");assertThrows(IllegalArgumentException.class,()->PhysicalFactRecipes.validate(c,cell,tmp));Files.delete(file);assertThrows(IllegalArgumentException.class,()->PhysicalFactRecipes.validate(c,cell,tmp));}
    @Test void duplicatePhysicalRowsAndMixedUnitsCannotInflateAQuantity()throws Exception{var c=stock();((ArrayNode)c.at("/rawRows/segments")).add(c.at("/rawRows/segments/0").deepCopy());assertThrows(IllegalArgumentException.class,()->cell(c));var second=row("mulino_inventory_QuantitySegments",UUID.randomUUID().toString()).put("itemId",item).put("placeId",place).put("quantity","40").put("unit","EA").put("validFrom",instant);rows(c,"mulino_inventory_QuantitySegments",c.at("/rawRows/segments/0"),second);assertThrows(IllegalArgumentException.class,()->cell(c));}
    @Test void canonicalRangeUnionDeduplicatesEvidenceAndKeepsAdjacentPhysicalOccurrences(){var a=new PhysicalIntervals.Range("root",new BigDecimal("0"),new BigDecimal("60"),"BOX");var b=new PhysicalIntervals.Range("root",new BigDecimal("50"),new BigDecimal("20"),"BOX");assertEquals(new BigDecimal("70"),PhysicalIntervals.union(List.of(a,a,b)).getFirst().quantity());assertEquals(2,PhysicalIntervals.union(List.of(a,new PhysicalIntervals.Range("root",new BigDecimal("60"),new BigDecimal("40"),"BOX"))).size());assertThrows(IllegalArgumentException.class,()->PhysicalIntervals.union(List.of(a,new PhysicalIntervals.Range("root",BigDecimal.ZERO,BigDecimal.ONE,"EA"))));}
    @Test void duplicateDerivedCellsAndMissingCompleteDatasetCannotProveCoverage()throws Exception {
        var c=stock();var cell=cell(c);((ArrayNode)c.path("physicalCells")).add(cell);((ObjectNode)c.path("sourceEvidence")).set("inventory.onHand.value",c.path("sourceEvidence").path("segments"));
        assertDoesNotThrow(()->PhysicalObservation.requireColumnCoverage(c,"inventory","onHand.value"));
        ((ArrayNode)c.path("physicalCells")).add(cell.deepCopy());assertThrows(IllegalArgumentException.class,()->PhysicalObservation.requireColumnCoverage(c,"inventory","onHand.value"));
        ((ObjectNode)c.path("sourceEvidence").path("segments")).put("complete",false);assertThrows(IllegalArgumentException.class,()->PhysicalFactRecipes.validate(c,cell,root));
    }
    @Test void derivedRecipeRejectsExecutableArtifactAndUnknownVisibility()throws Exception {
        var c=stock();var cell=cell(c);((ObjectNode)cell.path("recipe")).put("expression","return 0");var validator=new ContractValidator(root);assertThrows(IllegalArgumentException.class,()->validator.schema("verification/model-binding/physical-columns.schema.json",Json.array().add(cell)));
        var unknown=stock();((ObjectNode)unknown.path("sourceEvidence").path("segments")).remove("complete");assertThrows(IllegalArgumentException.class,()->cell(unknown));
    }
    @Test void oneClosedPhysicalRecipeServesSharedQuantityCollectionAndSumWithoutDuplicateCells()throws Exception {
        var c=stock();var p=parameters(c);((ObjectNode)p.path("bindings")).set("A",Json.object().put("table","mulino_inventory_QuantitySegments").put("id",segment));var parent=(ObjectNode)c.at("/rawRows/segments/0");parent.put("retiredAt",instant).put("retirementRecordedAt",instant);
        String first=UUID.randomUUID().toString(),second=UUID.randomUUID().toString();var a=row("mulino_inventory_QuantitySegments",first).put("itemId",item).put("placeId",place).put("quantity","20").put("unit","BOX").put("validFrom",instant);var b=a.deepCopy().put("ID",second).put("quantity","40");rows(c,"mulino_inventory_QuantitySegments",parent,a,b);rows(c,"mulino_inventory_GenealogyEdges",row("mulino_inventory_GenealogyEdges",UUID.randomUUID().toString()).put("sourceId",segment).put("targetId",first),row("mulino_inventory_GenealogyEdges",UUID.randomUUID().toString()).put("sourceId",segment).put("targetId",second));
        NativePhysicalFacts.map(root,c,p);var observation=Json.object().set("data",c);var evaluator=new BindingEvaluator(new BindingContract(root));assertEquals("60",evaluator.value("state.activeChildren.total.value",Json.object(),observation,List.of()).asText());assertEquals(Json.array().add("20").add("40"),evaluator.value("state.activeChildren.quantities",Json.object(),observation,List.of()));
        long count=0;for(JsonNode cell:c.path("physicalCells"))if(cell.path("dataset").asText().equals("activeChildQuantity"))count++;assertEquals(2,count);
    }
    @Test void explicitlyBoundFutureEntityAndUnknownPastUnitAreRejected() {
        var c=stock();var p=parameters(c);((ObjectNode)p.path("bindings")).set("A",Json.object().put("table","mulino_inventory_QuantitySegments").put("id",segment));((ObjectNode)c.at("/rawRows/segments/0")).put("recordedAt","2026-10-07T09:00:01Z");assertThrows(IllegalArgumentException.class,()->PhysicalFactRecipes.compute("state.segment.A.active",c,p));
        ((ObjectNode)c.at("/rawRows/mulino_inventory_TradeItems/0")).put("recordedAt","2026-10-07T09:00:01Z");assertThrows(IllegalArgumentException.class,()->PhysicalFactRecipes.compute("state.inventory.onHand.value",c,p));
    }
    @Test void commandWorkWithoutImmutableGenesisCannotBecomeAnEmptyWorld() {
        var c=stock();rows(c,"mulino_work_read_Works",row("mulino_work_read_Works",UUID.randomUUID().toString()).put("lifecycleMode","COMMAND").put("itemId",item));rows(c,"mulino_work_WorkTransitions");assertThrows(IllegalArgumentException.class,()->new PhysicalRecipeRows(c,parameters(c)).works());
    }
    @Test void missingImmutablePastCannotBeReplacedWithCurrentOwner() {var c=stock();var p=parameters(c);p.put("authorityAt","2026-10-07T09:00:01Z");assertThrows(IllegalArgumentException.class,()->PhysicalFactRecipes.compute("state.obligation.remaining.value",c,p));}
}
