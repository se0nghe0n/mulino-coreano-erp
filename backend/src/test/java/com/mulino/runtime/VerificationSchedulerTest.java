package com.mulino.runtime;

import com.mulino.application.runtime.ExternalDeliveryAdapter;
import com.mulino.application.runtime.VerificationResponder;
import com.mulino.application.runtime.VerificationScheduler;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** One host tick is exactly one recorded scheduler task; the synthetic receiver answers only as configured. */
class VerificationSchedulerTest {
  @Test void tickSubmitsOneTaskAndRecordsItsOwnTerminal() {
    var ran = new int[1]; var clock = new AtomicReference<>(Instant.parse("2026-10-07T00:00:00Z"));
    var scheduler = new VerificationScheduler(() -> { ran[0]++; return Map.of("recoveries", 1); }, clock::set, clock::get);
    Instant before = Instant.now();
    var reply = scheduler.tick(Map.of("schedulerId", "ontology-scheduler", "runId", "T11-due", "instant", "2026-10-07T03:00:00Z"));
    assertEquals(1, ran[0], "a tick runs exactly one cycle");
    assertEquals("SUBMITTED", reply.get("submissionStatus"));
    assertEquals("T11-due", reply.get("tickId"));
    assertFalse(reply.containsKey("terminalStatus"), "the submission reply never carries the terminal");
    assertFalse(Instant.parse(reply.get("submittedAt").toString()).isBefore(before), "submission time is wall clock");
    assertEquals(Instant.parse("2026-10-07T03:00:00Z"), clock.get(), "the business clock moved to the requested instant first");
    var records = scheduler.records("ontology-scheduler");
    var task = ((List<?>) records.get("tasks")).getFirst();
    assertEquals(reply.get("taskId"), ((Map<?, ?>) task).get("taskId"));
    assertEquals("SUCCEEDED", ((Map<?, ?>) task).get("terminalStatus"));
    assertEquals("autonomousScheduler", ((Map<?, ?>) task).get("origin"));
    assertEquals(1, ((List<?>) records.get("cycles")).size());
    assertEquals(List.of(), scheduler.records("other").get("tasks"), "records are per scheduler");
  }

  @Test void failingCycleIsAFailedTerminalAndSweepUsesSweepId() {
    var scheduler = new VerificationScheduler(() -> { throw new IllegalStateException("boom"); }, i -> {}, Instant::now);
    var reply = scheduler.tick(Map.of("schedulerId", "inventory-due-scheduler", "kind", "SWEEP", "runId", "T16-sweep"));
    assertEquals("T16-sweep", reply.get("sweepId"));
    var task = (Map<?, ?>) ((List<?>) scheduler.records(null).get("tasks")).getFirst();
    assertEquals("FAILED", task.get("terminalStatus"));
    assertEquals("IllegalStateException", task.get("technicalCode"));
    assertThrows(IllegalArgumentException.class, () -> scheduler.tick(Map.of("schedulerId", "s", "trigger", "OBSERVE_NEXT_NATURAL_TICK")));
  }

  @Test void responderAnswersPerConfigurationAndRecordsEveryRequest() {
    var responder = new VerificationResponder();
    assertFalse(responder.covers("dispatchPurchaseOrder"), "nothing is delivered before a configuration");
    responder.configure(Map.of("mode", "ACCEPT_THEN_DROP_RESPONSE", "idempotencySupported", true, "lookupSupported", true, "remoteResultId", "REMOTE-1"));
    responder.configure(Map.of("externalOperationId", "op-fail", "mode", "CONFIRMED_FAILURE"));
    assertTrue(responder.covers("dispatchPurchaseOrder"));
    assertEquals(new ExternalDeliveryAdapter.Support(true, true), responder.support("dispatchPurchaseOrder"));
    assertThrows(VerificationResponder.ResponseLost.class, () -> responder.deliver("dispatchPurchaseOrder", "op-1", Map.of("workId", "w")));
    assertThrows(VerificationResponder.ResponseLost.class, () -> responder.deliver("dispatchPurchaseOrder", "op-1", Map.of("workId", "w")),
        "the mode applies to every delivery, a redelivery included");
    assertEquals(ExternalDeliveryAdapter.Result.CONFIRMED_FAILURE, responder.deliver("dispatchPurchaseOrder", "op-fail", Map.of()));
    var first = responder.requests("op-1");
    assertEquals(2, first.size());
    assertEquals(true, first.get(0).get("effectApplied"));
    assertEquals(false, first.get(0).get("responseReturned"));
    assertEquals(true, first.get(1).get("duplicate"));
    assertEquals(false, first.get(1).get("effectApplied"), "the duplicate applies no second effect");
    assertEquals("REMOTE-1", first.get(0).get("remoteResultId"));
    assertEquals(ExternalDeliveryAdapter.Result.CONFIRMED_SUCCESS, responder.lookup("dispatchPurchaseOrder", "op-1"));
    assertEquals(ExternalDeliveryAdapter.Result.UNKNOWN, responder.lookup("dispatchPurchaseOrder", "op-fail"), "lookup only where configured");
    assertThrows(IllegalArgumentException.class, () -> responder.configure(Map.of("mode", "ACK_AND_DELETE_MESSAGE")));
  }
}
