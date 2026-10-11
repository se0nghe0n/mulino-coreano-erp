package com.mulino.application.runtime;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Explicit verification profile only: the scenario server runs no scheduler loop of its own (controlledTicks), so a host
 * control asks for exactly one scheduler cycle. A cycle is the product composition RuntimeVerificationHost runs every
 * second: due recovery schedules and validity boundaries (RuntimeRecoverySweeper), due intake recovery
 * (RuntimeIntakeProcessor) and one durable delivery tick against the verification responder. The scheduler records its
 * own submission, cycle and terminal rows; a host extractor reads them back instead of trusting the tick reply. Times are
 * wall-clock instants (they are compared with the host command interval); the business clock is the verification clock.
 */
@Component
@Profile("verification")
public class VerificationScheduler {
  public static final String STARTED_BY = "HARNESS_CONTROL", ORIGIN = "autonomousScheduler";
  private static final Set<String> FIELDS = Set.of("schedulerId", "kind", "runId", "instant", "organizationId");
  private static final int RETAINED = 10_000;
  private final Supplier<Map<String, Object>> cycle;
  private final Consumer<Instant> advance;
  private final Supplier<Instant> businessClock;
  private final List<Map<String, Object>> submissions = new CopyOnWriteArrayList<>(), cycles = new CopyOnWriteArrayList<>(), tasks = new CopyOnWriteArrayList<>();

  @Autowired
  public VerificationScheduler(RuntimeRecoverySweeper sweeper, RuntimeIntakeProcessor intake, DurableDeliveryWorker worker,
      VerificationResponder responder, com.sap.cds.services.runtime.CdsRuntime cds, VerificationClock clock) {
    this(() -> {
      var counts = new LinkedHashMap<String, Object>();
      cds.requestContext().run(c -> { counts.put("recoveries", sweeper.sweep()); counts.put("intakes", intake.recoverDue()); });
      var adapters = new AbstractMap<String, ExternalDeliveryAdapter>() {
        @Override public Set<Entry<String, ExternalDeliveryAdapter>> entrySet() { return Set.of(); }
        @Override public ExternalDeliveryAdapter get(Object operation) { return operation instanceof String op && responder.covers(op) ? responder : null; }
      };
      counts.put("deliveries", worker.tick("verification-scheduler", adapters));
      return counts;
    }, clock::advance, clock::instant);
  }

  public VerificationScheduler(Supplier<Map<String, Object>> cycle, Consumer<Instant> advance, Supplier<Instant> businessClock) {
    this.cycle = cycle; this.advance = advance; this.businessClock = businessClock;
  }

  /** One submitted task, run to its terminal before the reply. The reply is the submission only, never the terminal. */
  public synchronized Map<String, Object> tick(Map<String, Object> body) {
    if (!FIELDS.containsAll(body.keySet())) throw new IllegalArgumentException("Unsupported scheduler field");
    String scheduler = text(body, "schedulerId");
    String kind = body.getOrDefault("kind", "TICK").toString();
    if (!Set.of("TICK", "SWEEP").contains(kind)) throw new IllegalArgumentException("Scheduler kind must be TICK or SWEEP");
    String runKey = kind.equals("SWEEP") ? "sweepId" : "tickId";
    String taskId = UUID.randomUUID().toString();
    String run = body.get("runId") == null ? kind.toLowerCase(Locale.ROOT) + "-" + taskId : text(body, "runId");
    if (body.get("instant") != null) advance.accept(Instant.parse(body.get("instant").toString()));
    String handle = "verification-scheduler/" + scheduler + "/" + taskId;
    Instant submitted = Instant.now();
    var submission = new LinkedHashMap<String, Object>();
    submission.put("schedulerId", scheduler); submission.put(runKey, run); submission.put("taskId", taskId); submission.put("invocationHandle", handle);
    submission.put("submittedAt", submitted.toString()); submission.put("submittedBy", STARTED_BY); submission.put("businessInstant", businessClock.get().toString());
    if (body.get("organizationId") != null) submission.put("organizationId", body.get("organizationId"));
    keep(submissions, submission);
    Map<String, Object> counts; String status = "SUCCEEDED", technical = null;
    try { counts = cycle.get(); }
    catch (RuntimeException failure) { counts = Map.of(); status = "FAILED"; technical = failure.getClass().getSimpleName(); }
    Instant completed = Instant.now();
    var c = new LinkedHashMap<String, Object>();
    c.put("schedulerId", scheduler); c.put(runKey, run); c.put("startedAt", submitted.toString()); c.put("completedAt", completed.toString()); c.put("startedBy", STARTED_BY);
    keep(cycles, c);
    var task = new LinkedHashMap<String, Object>();
    task.put("schedulerId", scheduler); task.put(runKey, run); task.put("taskId", taskId); task.put("invocationHandle", handle); task.put("origin", ORIGIN);
    task.put("terminalStatus", status); task.put("submittedAt", submitted.toString()); task.put("completedAt", completed.toString()); task.put("result", counts);
    if (technical != null) task.put("technicalCode", technical);
    keep(tasks, task);
    var reply = new LinkedHashMap<String, Object>(submission); reply.put("submissionStatus", "SUBMITTED");
    return reply;
  }

  /** The scheduler's own rows, optionally for one scheduler. */
  public Map<String, Object> records(String schedulerId) {
    return Map.of("submissions", only(submissions, schedulerId), "cycles", only(cycles, schedulerId), "tasks", only(tasks, schedulerId));
  }

  private static List<Map<String, Object>> only(List<Map<String, Object>> rows, String schedulerId) {
    return rows.stream().filter(r -> schedulerId == null || schedulerId.equals(r.get("schedulerId"))).map(Map::copyOf).toList();
  }
  private static void keep(List<Map<String, Object>> rows, Map<String, Object> row) {
    rows.add(row);
    while (rows.size() > RETAINED) rows.removeFirst();
  }
  private static String text(Map<String, Object> body, String key) {
    if (!(body.get(key) instanceof String s) || !s.matches("[A-Za-z0-9._:/-]{1,160}")) throw new IllegalArgumentException("Invalid " + key);
    return s;
  }
}
