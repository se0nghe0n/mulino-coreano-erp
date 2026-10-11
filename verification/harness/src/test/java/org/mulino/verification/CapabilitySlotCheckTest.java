package org.mulino.verification;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** User decision 2026-10-11 (3): prepare reports case commands whose slot names the product contract does not publish. */
class CapabilitySlotCheckTest {
    @TempDir Path root;

    @Test
    void unpublishedSlotsAndCapabilitiesAreStepTwoFindingsAcrossRoutes() throws Exception {
        Files.createDirectories(root.resolve("contracts"));
        Files.writeString(root.resolve(CapabilitySlotCheck.CONTRACT),"{\"capabilities\":{\"moveQuantity\":{\"slots\":[\"destinationId\",\"segmentId\"]}}}");
        var c=Json.parse("""
            {"caseId":"X1","subcases":[{"id":"s","actions":[
              {"id":"ok","kind":"invoke","request":{"intentKind":"COMMAND","capabilityId":"moveQuantity","slots":{"segmentId":"a","destinationId":"b"}}},
              {"id":"bad","kind":"invoke","request":{"intentKind":"COMMAND","capabilityId":"moveQuantity","slots":{"segmentId":"a","quantity":"20"}}},
              {"id":"wire","kind":"invoke","request":{"method":"tools/call","params":{"arguments":{"capabilityId":"moveQuantity","slots":{"to":"x"}}}}},
              {"id":"intended","kind":"invoke","harness":{"intentionalViolation":["slot"]},"request":{"capabilityId":"moveQuantity","slots":{"zz":"1"}}},
              {"id":"query","kind":"query","request":{"intentKind":"QUERY","capabilityId":"getWork","slots":{"zz":"1"}}},
              {"id":"branch","kind":"choice","branches":[{"actions":[{"id":"unknown","kind":"invoke","request":{"capabilityId":"createDefinitionDraft","slots":{}}}]}]}]}]}
            """);
        List<String> problems=new ArrayList<>();var known=Json.array();
        var out=CapabilitySlotCheck.run(root,Map.of("X1",c),problems,known);
        assertTrue(problems.isEmpty(),problems.toString());
        assertEquals("FINDINGS",out.path("status").asText());
        assertEquals(4,out.path("checkedCommands").asInt());
        assertEquals(1,out.path("exemptIntentionalViolations").asInt());
        assertEquals(2,out.path("violatingCommands").asInt());
        assertEquals("[\"quantity\"]",out.path("violations").get(0).path("unpublishedSlots").toString());
        assertEquals("wire",out.path("violations").get(1).path("actionId").asText());
        assertEquals(1,out.path("unpublishedCapabilities").path("createDefinitionDraft").asInt());
        assertTrue(known.toString().contains("KNOWN_OPEN CAPABILITY-SLOT owner=Step 2 case=X1 count=2"),known.toString());
    }
    /** Round 14 Step 3 finding 1: verbs inside a stored Definition package are vocabulary, not commands (30 false positives). */
    @Test
    void definitionPackageVerbsAreNotCommands() throws Exception {
        Files.createDirectories(root.resolve("contracts"));
        Files.writeString(root.resolve(CapabilitySlotCheck.CONTRACT),"{\"capabilities\":{\"createDefinitionDraft\":{\"slots\":[\"package\",\"version\"]},\"createWork\":{\"slots\":[\"goal\"]}}}");
        var c=Json.parse("""
            {"caseId":"X2","subcases":[{"id":"s","actions":[
              {"id":"draft","kind":"invoke","request":{"intentKind":"COMMAND","capabilityId":"createDefinitionDraft","slots":{"version":"v2","package":{"nouns":[],"verbs":[{"name":"arrive","intentKind":"COMMAND","capabilityId":"createWork","stage":"DRAFT","slots":{"item":"TradeItem"}}]}}}}]}]}
            """);
        List<String> problems=new ArrayList<>();
        var out=CapabilitySlotCheck.run(root,Map.of("X2",c),problems,Json.array());
        assertEquals(1,out.path("checkedCommands").asInt(),out.toString());
        assertEquals(0,out.path("violatingCommands").asInt(),out.toString());
    }
    @Test
    void missingContractIsAPreparationProblem() {
        List<String> problems=new ArrayList<>();
        CapabilitySlotCheck.run(root,Map.of(),problems,Json.array());
        assertEquals(1,problems.size());
        assertTrue(problems.getFirst().startsWith("Capability slot contract unreadable"));
    }
}
