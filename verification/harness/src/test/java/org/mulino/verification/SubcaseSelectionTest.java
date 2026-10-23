package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Partial runs retain the original case/fixture hashes and cannot represent the whole profile. */
class SubcaseSelectionTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    private static final String CASE="verification/cases/T17/case.json";

    private JsonNode run(String... selectors) throws Exception {
        var args=new ArrayList<>(List.of("profile","scenarios",CASE));
        for(String selector:selectors)args.add("--subcase="+selector);
        assertEquals(2,Main.execute(args.toArray(String[]::new)),"unimplemented execution must remain NOT_RUN");
        return Json.read(root.resolve("verification/harness/target/evidence/scenarios.json"));
    }

    @Test void selectingOneOriginalSubcasePreservesCaseFixtureAndDeclaredCoverage() throws Exception {
        var original=Json.read(root.resolve(CASE));var sub=original.path("subcases").get(0);
        String id=sub.path("id").asText();String caseHash=Json.sha256(root.resolve(CASE));
        var report=run("T17/"+id);
        assertEquals(15,report.path("discoveredSubcases").asInt());assertTrue(report.path("discovered").asInt()>15);
        assertEquals(1,report.path("selectedSubcases").asInt());assertEquals(1,report.path("started").asInt());assertEquals(1,report.path("completed").asInt());
        assertTrue(report.path("explicitCaseSelection").asBoolean());assertTrue(report.path("explicitSubcaseSelection").asBoolean());
        assertFalse(report.path("gateComplete").asBoolean());assertFalse(report.path("productRuntimeClaimed").asBoolean());
        var actual=report.path("cases").get(0);assertEquals(id,actual.path("subcaseId").asText());assertEquals(caseHash,actual.path("caseHash").asText());
        assertEquals(Json.sha256(root.resolve(sub.path("fixtureRef").asText())),actual.path("fixtureHash").asText());
        assertEquals(caseHash,Json.sha256(root.resolve(CASE)));
    }

    @Test void requestedSubcasesExecuteInOriginalDeclarationOrder() throws Exception {
        var subs=Json.read(root.resolve(CASE)).path("subcases");
        String first=subs.get(0).path("id").asText(),second=subs.get(1).path("id").asText();
        var report=run("T17/"+second,"T17/"+first);assertEquals(2,report.path("cases").size());
        assertEquals(first,report.path("cases").get(0).path("subcaseId").asText());assertEquals(second,report.path("cases").get(1).path("subcaseId").asText());
    }

    @Test void selectingEverySubcaseExplicitlyIsStillAPartialProfile() throws Exception {
        var keys=new ArrayList<String>();for(var sub:Json.read(root.resolve(CASE)).path("subcases"))keys.add("T17/"+sub.path("id").asText());
        var report=run(keys.toArray(String[]::new));assertEquals(15,report.path("selectedSubcases").asInt());
        assertTrue(report.path("explicitCaseSelection").asBoolean());assertFalse(report.path("gateComplete").asBoolean());
    }

    @Test void noFilterRetainsAllOriginalSubcases() throws Exception {
        var report=run();assertEquals(15,report.path("selectedSubcases").asInt());assertFalse(report.path("explicitSubcaseSelection").asBoolean());
    }

    @Test void selectorWithoutAnExplicitFileStillUsesOriginalCaseDiscovery() throws Exception {
        String id=Json.read(root.resolve(CASE)).path("subcases").get(0).path("id").asText();
        assertEquals(2,Main.execute(new String[]{"profile","scenarios","--subcase","T17/"+id}));
        var report=Json.read(root.resolve("verification/harness/target/evidence/scenarios.json"));
        assertFalse(report.path("explicitCaseFileSelection").asBoolean());assertTrue(report.path("explicitCaseSelection").asBoolean());
        assertEquals(1,report.path("selectedSubcases").asInt());assertEquals("T17",report.path("cases").get(0).path("caseId").asText());
        assertTrue(report.path("discovered").asInt()>15);assertFalse(report.path("gateComplete").asBoolean());
    }

    @Test void unknownOrWrongCaseSelectorsFailBeforeAnyCaseRuns() {
        for(String key:List.of("T17/no-such-subcase","T09/missing-quantity"))
            assertTrue(assertThrows(IllegalArgumentException.class,()->Main.execute(new String[]{"profile","scenarios",CASE,"--subcase",key})).getMessage().contains("does not name a declared"));
    }

    @Test void malformedMissingAndDuplicateSelectorsAreRejected() {
        for(String key:List.of("","T17","/normal","T17/","T17/a/b"," T17/a","T17/a b"))
            assertThrows(IllegalArgumentException.class,()->Main.execute(new String[]{"profile","scenarios",CASE,"--subcase="+key}));
        assertThrows(IllegalArgumentException.class,()->Main.execute(new String[]{"profile","scenarios",CASE,"--subcase"}));
        assertThrows(IllegalArgumentException.class,()->Main.execute(new String[]{"profile","scenarios",CASE,"--subcase=T17/a","--subcase=T17/a"}));
    }

    @Test void selectionIsNotAcceptedForPreparationOrOtherProfiles() {
        for(String[] args:List.of(new String[]{"prepare",CASE,"--subcase=T17/a"},new String[]{"profile","recovery",CASE,"--subcase=T17/a"}))
            assertTrue(assertThrows(IllegalArgumentException.class,()->Main.execute(args)).getMessage().contains("applies only to profile scenarios"));
    }
}
