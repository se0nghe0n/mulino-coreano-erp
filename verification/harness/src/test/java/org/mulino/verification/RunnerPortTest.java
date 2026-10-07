package org.mulino.verification;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

public final class RunnerPortTest {
    private final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
    @Test void actualClientReceivesOnlyRawUtteranceAndPermittedContext() {
        AtomicReference<String> utterance=new AtomicReference<>();AtomicReference<com.fasterxml.jackson.databind.JsonNode> context=new AtomicReference<>();
        var port=new AgentRunner.Actual((id,route,actor,raw,permitted)->{utterance.set(raw);context.set(permitted);return StepResult.missing(id,"No actual host/model called in selftest");});
        var action=Json.parse("{\"userUtterance\":\"현재 보류 물량은?\",\"permittedContext\":{\"organizationAlias\":\"ORG\"},\"intent\":{\"capabilityId\":\"placeHold\"},\"expected\":\"DO_NOT_SEND\",\"oracle\":\"DO_NOT_SEND\"}");
        port.run("a","client",Json.object(),action,new UnimplementedDriver());
        assertEquals("현재 보류 물량은?",utterance.get());assertEquals(action.path("permittedContext"),context.get());assertFalse(context.get().has("intent"));assertFalse(context.get().has("expected"));
    }
    @Test void everyUnavailableParallelChildIsRecordedAndWholeCaseCannotPass() throws Exception {
        var c=Json.read(root.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json"));
        var actions=(com.fasterxml.jackson.databind.node.ArrayNode)c.path("subcases").get(0).path("actions");
        actions.add(Json.parse("{\"id\":\"parallel-probe\",\"kind\":\"parallel\",\"evidenceRefs\":[\"parallel:ACK\"],\"branches\":[{\"id\":\"left\",\"actions\":[{\"id\":\"child-a\",\"kind\":\"control\",\"control\":{\"type\":\"barrier\",\"operation\":\"waitReached\",\"parameters\":{}},\"evidenceRefs\":[\"barrier:left\"]}]},{\"id\":\"right\",\"actions\":[{\"id\":\"child-b\",\"kind\":\"control\",\"control\":{\"type\":\"process\",\"operation\":\"restart\",\"parameters\":{}},\"evidenceRefs\":[\"process:right\"]}]}]}"));
        Path file=root.resolve("verification/harness/target/parallel-port-selftest.json");Json.write(file,c);
        var runner=new CaseRunner(new ContractValidator(root),new UnimplementedDriver(),new AgentRunner.Scripted(),file,"hold-preserves-physical");
        assertEquals("NOT_RUN",runner.run(false));
        assertEquals("NOT_IMPLEMENTED",runner.results().get("child-a").path("driverStatus").asText());
        assertEquals("NOT_IMPLEMENTED",runner.results().get("child-b").path("driverStatus").asText());
        assertEquals("NOT_IMPLEMENTED",runner.results().get("parallel-probe").path("driverStatus").asText());
    }
}
