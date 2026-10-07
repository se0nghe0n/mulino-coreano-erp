package org.mulino.verification;

import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

public class ContractValidatorTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    @Test void comprehensiveExamplePassesExecutableSchemaAndSemanticValidation() throws Exception {
        new ContractValidator(root).caseFile(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
    }
    @Test void unimplementedDriverNeverReturnsFabricatedFactsForAnyPort() throws Exception {
        var d=new UnimplementedDriver(); var empty=Json.object();
        var results=java.util.List.of(d.installFixture("f",empty),d.invoke("i","api",empty,"confirmReceipt",empty),d.query("q","api",empty,"getInventory",empty),d.observe("o",empty),d.control("c",empty),d.start("s","api",empty,"dispatchQuantity",empty),d.await("a",empty,30));
        for(var r:results) {assertEquals(StepResult.DriverStatus.NOT_IMPLEMENTED,r.driverStatus());assertNull(r.data());assertNull(r.response());new ContractValidator(root).result(r,"observe");}
    }
    @Test void noObservationSchemaCanBeNullOrEmpty() throws Exception {
        var v=new ContractValidator(root);assertThrows(IllegalArgumentException.class,()->v.schema("contracts/acceptance-observation.schema.json",Json.MAPPER.nullNode()));assertThrows(IllegalArgumentException.class,()->v.schema("contracts/acceptance-observation.schema.json",Json.object()));
    }
    @Test void productRunMissingAdaptersIsNotRunWhileContractRedIsFail() throws Exception {
        var v=new ContractValidator(root);var path=root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json");
        assertEquals("NOT_RUN",new CaseRunner(v,new UnimplementedDriver(),new AgentRunner.Scripted(),path,"hold-preserves-physical").run(false));
        assertEquals("FAIL",new CaseRunner(v,new UnimplementedDriver(),new AgentRunner.Scripted(),path,"hold-preserves-physical").run(true));
    }
    @Test void unitBearingDeltaRequiresExplicitBaselineUnitSource() throws Exception {
        JsonNode c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
        for(JsonNode a:c.path("subcases").get(0).path("assertions")) if(a.path("op").asText().equals("decimalDelta")) ((com.fasterxml.jackson.databind.node.ObjectNode)a).remove("baselineUnitSource");
        assertThrows(IllegalArgumentException.class,()->new ContractValidator(root).schema("contracts/acceptance-case.schema.json",c));
    }
    @Test void assertionReferencesRejectUnknownActionAndSelfCopiedQuantityBeforeRuntime() throws Exception {
        JsonNode c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
        var assertion=(com.fasterxml.jackson.databind.node.ObjectNode)c.path("subcases").get(0).path("assertions").get(0);
        assertion.put("op","equals").set("expected",Json.parse("{\"$result\":{\"actionId\":\"after\",\"pointer\":\"/response/data/heldQuantity\"}}"));
        Path p=root.resolve("verification/harness/target/invalid-reference-selftest.json");Json.write(p,c);
        assertTrue(assertThrows(IllegalArgumentException.class,()->new ContractValidator(root).caseFile(p)).getMessage().contains("identity"));
        assertion.set("expected",Json.parse("{\"$result\":{\"actionId\":\"undeclared\",\"pointer\":\"/response/workId\"}}"));Json.write(p,c);
        assertTrue(assertThrows(IllegalArgumentException.class,()->new ContractValidator(root).caseFile(p)).getMessage().contains("Unknown result reference"));
    }
    @Test void boundedTransformDeclarationIsValidatedBeforeRuntime() throws Exception {
        JsonNode c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
        var request=(com.fasterxml.jackson.databind.node.ObjectNode)c.path("subcases").get(0).path("actions").get(2).path("request");
        request.set("state",Json.parse("{\"$transform\":{\"source\":{\"$result\":{\"actionId\":\"before\",\"pointer\":\"/response/state\"}},\"operation\":\"opaqueByteXor\",\"index\":0,\"xor\":1}}"));
        Path p=root.resolve("verification/harness/target/transform-declaration-selftest.json");Json.write(p,c);new ContractValidator(root).caseFile(p);
        ((com.fasterxml.jackson.databind.node.ObjectNode)request.path("state").path("$transform")).put("operation","eval");Json.write(p,c);
        assertThrows(IllegalArgumentException.class,()->new ContractValidator(root).caseFile(p));
    }
}
