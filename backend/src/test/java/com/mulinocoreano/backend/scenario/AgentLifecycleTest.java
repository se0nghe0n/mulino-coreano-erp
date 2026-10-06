package com.mulinocoreano.backend.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.cucumber.core.backend.ObjectFactory;
import io.cucumber.core.feature.FeatureWithLines;
import io.cucumber.core.options.RuntimeOptionsBuilder;
import io.cucumber.java.JavaBackendProviderService;
import io.cucumber.java.en.Given;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

/** Goal 5: actual Cucumber teardown keeps delayed native evidence and cleans up every outcome. */
class AgentLifecycleTest {
    @TempDir Path directory;
    final ObjectMapper mapper = new ObjectMapper();

    @Test
    void pendingBusinessProposalWaitsForNativeUsageBeforeProcessCleanup() throws Exception {
        var steps = steps(false);
        try {
            assertThat(run(steps, false)).isZero();
            var evidence = mapper.readTree(Files.readString(directory.resolve("TC-LIFECYCLE.json")));
            assertThat(evidence.path("status").asText()).isEqualTo("PASSED");
            assertThat(evidence.path("executionReady").asBoolean()).isTrue();
            assertThat(evidence.path("usageComplete").asBoolean()).isTrue();
            assertThat(evidence.path("costUsd").decimalValue()).isEqualByComparingTo("0.12");
            assertThat(evidence.path("nativeUsageEvents").size()).isEqualTo(1);
            assertThat(steps.driver.isAlive()).isFalse();
        } finally { steps.stopAgent(); }
    }

    @Test
    void nativeFinalizationFailureWritesUnknownUsageAndStillCleansUp() throws Exception {
        var steps = steps(true);
        try {
            assertThat(run(steps, false)).isNotZero();
            var evidence = mapper.readTree(Files.readString(directory.resolve("TC-LIFECYCLE.json")));
            assertThat(evidence.path("status").asText()).isEqualTo("FAILED");
            assertThat(evidence.path("executionReady").asBoolean()).isFalse();
            assertThat(evidence.path("usageComplete").asBoolean()).isFalse();
            assertThat(evidence.path("costUsd").isNull()).isTrue();
            assertThat(evidence.path("nativeUsageEvents").size()).isEqualTo(1);
            assertThat(steps.driver.isAlive()).isFalse();
        } finally { steps.stopAgent(); }
    }

    @Test
    void scenarioFailureStillWritesEvidenceAndCleansUp() throws Exception {
        var steps = steps(false);
        try {
            assertThat(run(steps, true)).isNotZero();
            var evidence = mapper.readTree(Files.readString(directory.resolve("TC-LIFECYCLE.json")));
            assertThat(evidence.path("status").asText()).isEqualTo("FAILED");
            assertThat(steps.driver.isAlive()).isFalse();
        } finally { steps.stopAgent(); }
    }

    AgentSteps steps(boolean nativeFailure) {
        var steps = new AgentSteps() {
            @Override boolean liveEvidence() { return true; }
            @Override String evidenceRuntime() { return "CLAUDE"; }
            @Override Path evidenceDirectory() { return directory; }
            @Override Duration timeout() { return Duration.ofSeconds(5); }
        };
        steps.mapper = mapper;
        steps.world = new ScenarioWorld();
        steps.world.caseRef("CASE-LIFECYCLE");
        steps.state = mock(BusinessState.class);
        when(steps.state.runsForCase("CASE-LIFECYCLE")).thenReturn(
                List.of(new BusinessState.RunRecord("QC", "RUN-LIFECYCLE", "COMPLETED", "WAITING")));
        when(steps.state.appliedPurchaseTotal()).thenReturn(java.math.BigDecimal.ZERO);
        when(steps.state.caseStatus("CASE-LIFECYCLE")).thenReturn("OPEN");
        when(steps.state.finalEvidence("CASE-LIFECYCLE")).thenReturn(Map.of());
        String event = nativeFailure
                ? "{event:'model_finished',runRef:'RUN-LIFECYCLE',failure:'TERMINAL_FINALIZATION_TIMEOUT'}"
                : "{event:'model_finished',runRef:'RUN-LIFECYCLE',runtime:'CLAUDE',model:'claude-sonnet-5',resolvedModel:'claude-sonnet-5',nativeExitCode:0,costUsd:0.12,inputTokens:2,outputTokens:3,cacheReadTokens:4,cacheWriteTokens:5}";
        steps.driver = new AgentDriver(mapper, List.of("node", "-e",
                "setTimeout(()=>console.log(JSON.stringify(" + event + ")),1500);setInterval(()=>{},1000)"));
        return steps;
    }

    byte run(AgentSteps steps, boolean scenarioFailure) throws Exception {
        var fixture = new BusinessFixture(steps, scenarioFailure);
        var factory = new ObjectFactory() {
            @Override public void start() {}
            @Override public void stop() {}
            @Override public boolean addClass(Class<?> type) { return true; }
            @Override public <T> T getInstance(Class<T> type) {
                return type.cast(type == AgentSteps.class ? steps : fixture);
            }
        };
        var feature = directory.resolve("lifecycle.feature");
        Files.writeString(feature, "@TC-LIFECYCLE\nFeature: Native evidence lifecycle\nScenario: Pending proposal\nGiven a persisted business proposal\n");
        var options = new RuntimeOptionsBuilder()
                .addGlueClass(AgentSteps.class.getName())
                .addGlueClass(BusinessFixture.class.getName())
                .addFeature(FeatureWithLines.create(feature.toUri(), List.of()))
                .setPublish(false).setPublishQuiet(true).setNoSummary().build();
        var backend = new JavaBackendProviderService().create(factory, factory, () -> getClass().getClassLoader());
        var runtime = io.cucumber.core.runtime.Runtime.builder().withRuntimeOptions(options)
                .withBackendSupplier(() -> List.of(backend)).withObjectFactorySupplier(() -> factory).build();
        runtime.run();
        return runtime.exitStatus();
    }

    public static class BusinessFixture {
        private final AgentSteps steps;
        private final boolean failure;
        BusinessFixture(AgentSteps steps, boolean failure) { this.steps = steps; this.failure = failure; }
        @Given("a persisted business proposal")
        public void proposal() {
            steps.driver.start(Map.of());
            if (failure) throw new AssertionError("Scenario business assertion failed");
        }
    }
}
