package com.mulinocoreano.backend.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** 목표 5 증거: UAT는 Run별 결과(에이전트, 상태, 완료 결과, 실패 코드, 비용·토큰)와 그 합계를
 * 남기고, 준비되지 않은 환경에서는 모델을 부르지 않는다. */
class UatEvidenceTest {
    ObjectMapper m = new ObjectMapper();

    @Test
    void successfulNativePermissionDiagnosticsPreserveFailedBusinessAndApprovalGates() throws Exception {
        var rows = List.of(new BusinessState.RunRecord("QC", "RUN-Q", "FAILED", "FAILED"));
        var event = m.readTree("""
            {"runRef":"RUN-Q","runtime":"CLAUDE","model":"claude-sonnet-5","resolvedModel":"claude-sonnet-5",
             "nativeExitCode":0,"nativeResultSubtype":"success","costUsd":0.1,"inputTokens":1,"outputTokens":2,
             "cacheReadTokens":3,"cacheWriteTokens":4,"nativePermissionDenialCount":1,
             "nativeDeniedToolKinds":{"Bash":1},"nativeDeniedCommandShapes":{"CUSTOM_ENV_PREFIX":1},
             "permission_denials":[{"tool_input":{"command":"secret-command"},"tool_use_id":"secret-id"}]}
            """);
        var summary = UatEvidence.summarize(rows, List.of(event));
        var safe = m.valueToTree(summary);
        assertThat(safe.path("runs").get(0).path("nativePermissionDenialCount").asInt()).isEqualTo(1);
        assertThat(safe.path("runs").get(0).path("nativeDeniedToolKinds").path("Bash").asInt()).isEqualTo(1);
        assertThat(safe.path("runs").get(0).path("nativeDeniedCommandShapes").path("CUSTOM_ENV_PREFIX").asInt()).isEqualTo(1);
        assertThat(safe.toString()).doesNotContain("secret-command", "secret-id", "permission_denials");
        assertThat(summary.get("failures")).isEqualTo(List.of());
        assertThat(summary.get("usageComplete")).isEqualTo(true);
        assertThat(UatEvidence.finalized(rows, List.of(event), false, "CLAUDE")).isFalse();
        // A denied optional tool is diagnostic data, never an invented execution failure.
        assertThat(UatEvidence.finalized(List.of(new BusinessState.RunRecord("QC", "RUN-Q", "COMPLETED", "WAITING")),
                List.of(event), false, "CLAUDE")).isTrue();
        assertThat(UatEvidence.finalized(List.of(new BusinessState.RunRecord("QC", "RUN-Q", "RUNNING", null)),
                List.of(event), false, "CLAUDE")).isFalse();
    }

    @Test
    void perRunEntryCarriesOutcomeFailureAndUsageMatchedByRunRef() throws Exception {
        var dbRuns = List.of(
                new BusinessState.RunRecord("SUPPLY_CHAIN", "RUN-1", "COMPLETED", "DONE"),
                new BusinessState.RunRecord("ORCHESTRATOR", "RUN-2", "COMPLETED", "FAILED"));
        var modelFinished = List.of(
                m.readTree("{\"event\":\"model_finished\",\"runRef\":\"RUN-1\",\"costUsd\":0.12,\"inputTokens\":100,\"outputTokens\":20}"),
                m.readTree("{\"event\":\"model_finished\",\"runRef\":\"RUN-2\",\"failure\":\"MODEL_OUTPUT_TOO_LARGE\",\"nativeFailureCategory\":\"USAGE_LIMIT\",\"nativeHttpStatus\":429,\"costUsd\":0.05}"));

        var s = UatEvidence.summarize(dbRuns, modelFinished);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> runs = (List<Map<String, Object>>) s.get("runs");
        assertThat(runs).hasSize(2);
        assertThat(runs.get(0)).containsEntry("agentKey", "SUPPLY_CHAIN").containsEntry("runRef", "RUN-1")
                .containsEntry("outcome", "DONE").containsEntry("failure", null);
        assertThat((java.math.BigDecimal) runs.get(0).get("costUsd")).isEqualByComparingTo("0.12");
        assertThat(runs.get(1)).containsEntry("agentKey", "ORCHESTRATOR").containsEntry("runRef", "RUN-2")
                .containsEntry("outcome", "FAILED").containsEntry("failure", "MODEL_OUTPUT_TOO_LARGE");

        assertThat((java.math.BigDecimal) s.get("costUsd")).isEqualByComparingTo("0.17");
        assertThat(runs.get(0).get("inputTokens")).isEqualTo(100L);
        assertThat(s.get("inputTokens")).isNull();
        assertThat(s.get("failures")).isEqualTo(List.of("MODEL_OUTPUT_TOO_LARGE"));
        assertThat(((tools.jackson.databind.JsonNode)runs.get(1).get("nativeFailureCategory")).asText()).isEqualTo("USAGE_LIMIT");
        assertThat(((tools.jackson.databind.JsonNode)runs.get(1).get("nativeHttpStatus")).asInt()).isEqualTo(429);
    }

    @Test
    void runWithoutAModelFinishedLogRetainsUnknownUsage() {
        var dbRuns = List.of(new BusinessState.RunRecord("SUPPLY_CHAIN", "RUN-3", "QUEUED", null));

        var s = UatEvidence.summarize(dbRuns, List.of());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> runs = (List<Map<String, Object>>) s.get("runs");
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0)).containsEntry("outcome", null).containsEntry("failure", null)
                .containsEntry("costUsd", null).containsEntry("inputTokens",null).containsEntry("usageReported",false);
        assertThat(s.get("failures")).isEqualTo(List.of());
    }

    @Test
    void missingPrerequisitesAreNamedSoNoModelIsCalled() {
        assertThat(UatEvidence.prerequisitesMissing(Map.of("MULINO_AGENT_RUNTIME", "CLAUDE")))
                .containsExactlyInAnyOrder("MULINO_AGENT_MODEL", "MULINO_RUNTIME_IMAGE", "MULINO_AUTH_VOLUME");
        assertThat(UatEvidence.summarize(List.of(),List.of()).get("usageComplete")).isEqualTo(false);
    }
    @Test
    void evidenceFileRetainsTheRunAndFinalBusinessState(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        var record=new java.util.LinkedHashMap<String,Object>();
        record.put("testCase","TC-P2P-001");record.put("runtime","CLAUDE");record.put("model","claude-sonnet-5");
        record.putAll(UatEvidence.summarize(List.of(new BusinessState.RunRecord("PROCUREMENT","RUN-1","COMPLETED","WAITING")),
            List.of(m.readTree("{\"runRef\":\"RUN-1\",\"runtime\":\"CLAUDE\",\"model\":\"claude-sonnet-5\",\"resolvedModel\":\"claude-sonnet-5\",\"costUsd\":0.12,\"inputTokens\":20,\"outputTokens\":8}"))));
        record.put("caseStatus","WAITING");record.put("appliedPurchaseOrders",0);
        record.put("businessEvidence", Map.of("decisions", List.of(Map.of("decision", "BLOCK", "human_role", "MANAGER", "reason", "Business policy review")),
            "work", List.of(Map.of("agent_key", "ORCHESTRATOR", "status", "BLOCKED")),
            "attention", List.of(Map.of("status", "OPEN")), "audit", List.of(Map.of("event_type", "PURCHASE_BLOCKED"))));
        var persisted=m.readTree(java.nio.file.Files.readString(UatEvidence.write(m,dir,"TC-P2P-001",record)));
        assertThat(persisted.path("runs").get(0).path("outcome").asText()).isEqualTo("WAITING");
        assertThat(persisted.path("runs").get(0).path("resolvedModel").asText()).isEqualTo("claude-sonnet-5");
        assertThat(persisted.path("costUsd").decimalValue()).isEqualByComparingTo("0.12");
        assertThat(persisted.path("caseStatus").asText()).isEqualTo("WAITING");
        assertThat(persisted.path("appliedPurchaseOrders").asLong()).isZero();
        var facts = persisted.path("businessEvidence");
        assertThat(facts.path("decisions").get(0).path("human_role").asText()).isEqualTo("MANAGER");
        assertThat(facts.path("decisions").get(0).path("reason").asText()).isEqualTo("Business policy review");
        assertThat(facts.path("work").get(0).path("status").asText()).isEqualTo("BLOCKED");
        assertThat(facts.path("attention").get(0).path("status").asText()).isEqualTo("OPEN");
        assertThat(facts.path("audit").get(0).path("event_type").asText()).isEqualTo("PURCHASE_BLOCKED");
    }

    @Test
    void terminalCancellationCannotInventZeroUsageOrACompleteCostTotal() throws Exception {
        var rows=List.of(new BusinessState.RunRecord("ORCHESTRATOR","RUN-1","COMPLETED","WAITING"),
            new BusinessState.RunRecord("PROCUREMENT","RUN-2","COMPLETED","WAITING"));
        var summary=UatEvidence.summarize(rows,List.of(
            m.readTree("{\"runRef\":\"RUN-1\",\"costUsd\":0.12,\"inputTokens\":20,\"outputTokens\":8}"),
            m.readTree("{\"runRef\":\"RUN-2\",\"runtime\":\"CLAUDE\",\"model\":\"claude-sonnet-5\",\"failure\":\"TERMINAL_FINALIZATION_TIMEOUT\"}")));
        assertThat(summary.get("costUsd")).isNull();
        assertThat((java.math.BigDecimal)summary.get("partialCostUsd")).isEqualByComparingTo("0.12");
        assertThat(summary.get("usageComplete")).isEqualTo(false);
        assertThat(summary.get("inputTokens")).isNull();
        assertThat(UatEvidence.completeUsage(m.readTree("{\"failure\":\"TERMINAL_FINALIZATION_TIMEOUT\"}"))).isFalse();
    }

    @Test
    void persistedPendingProposalIsAcceptedOnlyAfterSuccessfulNativeFinalization() throws Exception {
        var waiting = List.of(new BusinessState.RunRecord("QC", "RUN-QC", "COMPLETED", "WAITING"));
        var usage = m.readTree("{\"runRef\":\"RUN-QC\",\"nativeExitCode\":0,\"runtime\":\"CLAUDE\",\"model\":\"claude-sonnet-5\",\"resolvedModel\":\"claude-sonnet-5\",\"costUsd\":0.1,\"inputTokens\":2,\"outputTokens\":3,\"cacheReadTokens\":4,\"cacheWriteTokens\":5}");
        assertThat(UatEvidence.finalized(waiting, List.of(), false, "CLAUDE")).isFalse();
        assertThat(UatEvidence.finalized(waiting, List.of(usage), false, "CLAUDE")).isTrue();
        assertThat(UatEvidence.finalized(List.of(new BusinessState.RunRecord("ORCHESTRATOR", "RUN-QC", "ABORTED", "ABORTED")), List.of(usage), true, "CLAUDE")).isTrue();
        assertThat(UatEvidence.finalized(List.of(new BusinessState.RunRecord("ORCHESTRATOR", "RUN-QC", "ABORTED", "ABORTED")), List.of(usage), false, "CLAUDE")).isFalse();
        assertThat(UatEvidence.finalized(List.of(new BusinessState.RunRecord("QC", "RUN-QC", "ABORTED", "ABORTED")), List.of(usage), true, "CLAUDE")).isFalse();
        assertThat(UatEvidence.finalized(List.of(new BusinessState.RunRecord("QC", "RUN-QC", "RUNNING", null)), List.of(usage), false, "CLAUDE")).isFalse();
        assertThat(UatEvidence.finalized(List.of(new BusinessState.RunRecord("QC", "RUN-QC", "COMPLETED", "FAILED")), List.of(usage), false, "CLAUDE")).isFalse();
        assertThat(UatEvidence.finalized(waiting, List.of(m.readTree("{\"runRef\":\"RUN-QC\",\"failure\":\"TERMINAL_FINALIZATION_TIMEOUT\"}")), false, "CLAUDE")).isFalse();
    }

    @Test
    void codexBusinessExecutionCanFinishWhileUnreportedAccountingRemainsUnknown() throws Exception {
        var rows = List.of(new BusinessState.RunRecord("PROCUREMENT", "RUN-C", "COMPLETED", "WAITING"));
        var event = m.readTree("{\"runRef\":\"RUN-C\",\"runtime\":\"CODEX\",\"model\":\"gpt-5.6-sol\",\"nativeExitCode\":0,\"inputTokens\":42,\"outputTokens\":13,\"cacheReadTokens\":7}");
        assertThat(UatEvidence.finalized(rows, List.of(event), false, "CODEX")).isTrue();
        var summary = UatEvidence.summarize(rows, List.of(event));
        assertThat(summary.get("usageComplete")).isEqualTo(false);
        assertThat(summary.get("costUsd")).isNull();
        assertThat(summary.get("partialCostUsd")).isNull();
        assertThat(summary.get("cacheWriteTokens")).isNull();
        assertThat(summary.get("inputTokens")).isEqualTo(42L);
        assertThat(UatEvidence.finalized(rows, List.of(), false, "CODEX")).isFalse();
        assertThat(UatEvidence.executionReported(event, "CLAUDE")).isFalse();
        for (String failure : List.of("\"failure\":\"MODEL_PROCESS_FAILED\"", "\"nativeSignal\":\"SIGKILL\"", "\"nativeExitCode\":1", "\"inputTokens\":null")) {
            var failed = m.readTree(event.toString().substring(0, event.toString().length()-1) + "," + failure + "}");
            assertThat(UatEvidence.finalized(rows, List.of(failed), false, "CODEX")).isFalse();
            assertThat(UatEvidence.finalized(rows, List.of(event, failed), false, "CODEX")).isFalse();
            assertThat(UatEvidence.finalized(rows, List.of(failed, event), false, "CODEX")).isFalse();
        }
    }

}
