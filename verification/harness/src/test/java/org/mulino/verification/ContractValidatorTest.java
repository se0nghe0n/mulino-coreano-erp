package org.mulino.verification;

import org.junit.jupiter.api.*;
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
}
