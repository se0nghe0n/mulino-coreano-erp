package com.mulino.application.runtime;

import com.mulino.application.core.CommandFaults;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Explicit verification profile only. A host control arms a fault for one organization at one pipeline point; the next
 * matching command fails there with {@link Injected}, a non-transient technical failure, so the command transaction
 * rolls back (or, after commit, the response is lost). Points the pipeline does not report are refused at arm time
 * instead of being accepted and never firing.
 */
@Component
@Profile("verification")
public class VerificationFaults implements CommandFaults {
  /** Pipeline locations ApplicationCommands reports. The first three name the same location: after the handler's
   * effects and before the command result and audit rows are written in the same transaction. */
  public static final Set<String> POINTS = Set.of("command.beforeCommit", "AFTER_IN_PROGRESS_BEFORE_EFFECT_COMMIT", "AUDIT_INSERT",
      "AFTER_DB_COMMIT_BEFORE_RESPONSE");
  private static final Set<String> FIELDS = Set.of("operation", "faultId", "organizationId", "point", "capabilityId", "commandKey", "occurrences", "fault");
  private final Map<String, Fault> faults = new ConcurrentHashMap<>();

  /** The failure an armed point throws. It is not a SQL serialization failure, so the pipeline never retries it. */
  public static final class Injected extends RuntimeException {
    public Injected(String point, String faultId) { super("Verification fault " + faultId + " injected at " + point); }
  }
  /** An authored point the pipeline does not report. */
  public static final class UnsupportedPoint extends RuntimeException {
    public UnsupportedPoint(String point) { super(point); }
  }

  static final class Fault {
    final String faultId, organizationId, point, capabilityId, commandKey, kind; final Instant armedAt = Instant.now();
    int remaining, fired; String state = "ARMED"; Instant firedAt;
    Fault(String faultId, String organizationId, String point, String capabilityId, String commandKey, String kind, int remaining) {
      this.faultId = faultId; this.organizationId = organizationId; this.point = point; this.capabilityId = capabilityId;
      this.commandKey = commandKey; this.kind = kind; this.remaining = remaining;
    }
    Map<String, Object> view() {
      var m = new LinkedHashMap<String, Object>();
      m.put("faultId", faultId); m.put("organizationId", organizationId); m.put("point", point);
      if (capabilityId != null) m.put("capabilityId", capabilityId);
      if (commandKey != null) m.put("commandKey", commandKey);
      if (kind != null) m.put("fault", kind);
      m.put("state", state); m.put("remaining", remaining); m.put("fired", fired); m.put("armedAt", armedAt.toString());
      if (firedAt != null) m.put("firedAt", firedAt.toString());
      return m;
    }
  }

  /** arm/inject/install create a fault; disarm/remove retire it by id. */
  public synchronized Map<String, Object> control(Map<String, Object> body) {
    if (!FIELDS.containsAll(body.keySet())) throw new IllegalArgumentException("Unsupported fault control field");
    String operation = text(body, "operation");
    switch (operation) {
      case "arm", "inject", "install" -> {
        String point = text(body, "point");
        if (!POINTS.contains(point)) throw new UnsupportedPoint(point);
        int occurrences = body.get("occurrences") instanceof Number n ? n.intValue() : 1;
        if (occurrences < 1 || occurrences > 10) throw new IllegalArgumentException("Fault occurrences must be 1..10");
        String id = body.get("faultId") == null ? UUID.randomUUID().toString() : text(body, "faultId");
        if (faults.containsKey(id) && "ARMED".equals(faults.get(id).state)) throw new IllegalArgumentException("Fault already armed");
        var f = new Fault(id, UUID.fromString(text(body, "organizationId")).toString(), point, optional(body, "capabilityId"), optional(body, "commandKey"), optional(body, "fault"), occurrences);
        faults.put(id, f);
        return f.view();
      }
      case "disarm", "remove" -> {
        Fault f = faults.get(text(body, "faultId"));
        if (f == null) return new LinkedHashMap<>(Map.of("faultId", body.get("faultId"), "state", "NOT_SEEN"));
        if ("ARMED".equals(f.state)) f.state = "DISARMED";
        return f.view();
      }
      default -> throw new IllegalArgumentException("Unsupported fault operation");
    }
  }

  @Override public void check(String organizationId, String capabilityId, String commandKey, String... points) {
    Fault hit = null; String at = null;
    synchronized (this) {
      for (Fault f : faults.values()) {
        if (!"ARMED".equals(f.state) || !f.organizationId.equals(organizationId)) continue;
        if (f.capabilityId != null && !f.capabilityId.equals(capabilityId)) continue;
        if (f.commandKey != null && !f.commandKey.equals(commandKey)) continue;
        if (!Arrays.asList(points).contains(f.point)) continue;
        f.remaining--; f.fired++; f.firedAt = Instant.now(); if (f.remaining == 0) f.state = "FIRED";
        hit = f; at = f.point; break;
      }
    }
    if (hit != null) throw new Injected(at, hit.faultId);
  }

  private static String text(Map<String, Object> body, String key) {
    Object v = body.get(key);
    if (!(v instanceof String s) || s.isBlank()) throw new IllegalArgumentException("Fault control requires " + key);
    return s;
  }
  private static String optional(Map<String, Object> body, String key) { return body.get(key) == null ? null : text(body, key); }
}
