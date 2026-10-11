package com.mulino.application.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Explicit verification profile only: the synthetic external receiver the verification scheduler's delivery tick talks to.
 * It is never a product adapter. A host control configures how it answers (per externalOperationId, per operation, or as
 * the default) and reads back every request it received. The product still decides what a reply means: without immutable
 * verified evidence every delivery ends UNKNOWN_EXTERNAL and needs reconciliation (RuntimeService.deliverUnderFence).
 */
@Component
@Profile("verification")
public class VerificationResponder implements ExternalDeliveryAdapter {
  /** CONFIRMED_SUCCESS/CONFIRMED_FAILURE answer at once; ACCEPT_THEN_DROP_RESPONSE applies the effect and loses the reply;
   * TIMEOUT_BEFORE_EFFECT loses the request before any effect. */
  public static final Set<String> MODES = Set.of("CONFIRMED_SUCCESS", "CONFIRMED_FAILURE", "ACCEPT_THEN_DROP_RESPONSE", "TIMEOUT_BEFORE_EFFECT");
  private static final Set<String> FIELDS = Set.of("organizationId", "operation", "externalOperationId", "mode", "idempotencySupported", "lookupSupported", "remoteResultId");
  private final List<Map<String, Object>> configurations = new CopyOnWriteArrayList<>();
  private final List<Map<String, Object>> requests = new CopyOnWriteArrayList<>();

  /** The answer the responder lost on purpose; DurableDeliveryWorker treats a thrown delivery as UNKNOWN. */
  public static final class ResponseLost extends RuntimeException {
    public ResponseLost(String externalOperationId) { super("Verification responder lost the response of " + externalOperationId); }
  }

  public synchronized Map<String, Object> configure(Map<String, Object> body) {
    if (!FIELDS.containsAll(body.keySet())) throw new IllegalArgumentException("Unsupported responder field");
    String mode = String.valueOf(body.get("mode"));
    if (!MODES.contains(mode)) throw new IllegalArgumentException("Unsupported responder mode");
    for (String key : List.of("idempotencySupported", "lookupSupported"))
      if (body.containsKey(key) && !(body.get(key) instanceof Boolean)) throw new IllegalArgumentException(key + " must be boolean");
    var c = new LinkedHashMap<String, Object>(body);
    c.putIfAbsent("idempotencySupported", false); c.putIfAbsent("lookupSupported", false);
    c.put("configurationId", UUID.randomUUID().toString()); c.put("configuredAt", Instant.now().toString());
    configurations.add(c);
    return Map.copyOf(c);
  }

  /** The most specific configuration: externalOperationId, then operation, then the default; the latest one wins. */
  Optional<Map<String, Object>> configuration(String operation, String externalOperationId) {
    Map<String, Object> best = null; int rank = -1;
    for (var c : configurations) {
      int r;
      if (c.get("externalOperationId") != null) { if (!c.get("externalOperationId").equals(externalOperationId)) continue; r = 2; }
      else if (c.get("operation") != null) { if (!c.get("operation").equals(operation)) continue; r = 1; }
      else r = 0;
      if (r >= rank) { best = c; rank = r; }
    }
    return Optional.ofNullable(best);
  }

  /** Whether a delivery tick should hand this operation to the responder. */
  public boolean covers(String operation) {
    return configurations.stream().anyMatch(c -> c.get("externalOperationId") != null || c.get("operation") == null || c.get("operation").equals(operation));
  }

  @Override public Support support(String operation) {
    var c = configuration(operation, null).orElse(Map.of());
    return new Support(Boolean.TRUE.equals(c.get("idempotencySupported")), Boolean.TRUE.equals(c.get("lookupSupported")));
  }

  @Override public Result deliver(String operation, String externalOperationId, Map<String, Object> payload) {
    var c = configuration(operation, externalOperationId).orElseThrow(() -> new ResponseLost(externalOperationId));
    String mode = String.valueOf(c.get("mode"));
    boolean duplicate = Boolean.TRUE.equals(c.get("idempotencySupported")) && effectApplied(externalOperationId);
    boolean effect = !duplicate && Set.of("CONFIRMED_SUCCESS", "ACCEPT_THEN_DROP_RESPONSE").contains(mode);
    boolean answered = Set.of("CONFIRMED_SUCCESS", "CONFIRMED_FAILURE").contains(mode);
    var r = new LinkedHashMap<String, Object>();
    r.put("requestId", UUID.randomUUID().toString()); r.put("externalOperationId", externalOperationId); r.put("operation", operation);
    r.put("payloadSha256", sha256(String.valueOf(new TreeMap<>(payload)))); r.put("receivedAt", Instant.now().toString());
    r.put("mode", mode); r.put("configurationId", c.get("configurationId")); r.put("duplicate", duplicate);
    r.put("effectApplied", effect); r.put("responseReturned", answered);
    if (c.get("remoteResultId") != null && (effect || duplicate)) r.put("remoteResultId", c.get("remoteResultId"));
    if (c.get("organizationId") != null) r.put("organizationId", c.get("organizationId"));
    requests.add(r);
    if (!answered) throw new ResponseLost(externalOperationId);
    return mode.equals("CONFIRMED_SUCCESS") || duplicate ? Result.CONFIRMED_SUCCESS : Result.CONFIRMED_FAILURE;
  }

  @Override public Result lookup(String operation, String externalOperationId) {
    var c = configuration(operation, externalOperationId).orElse(Map.of());
    if (!Boolean.TRUE.equals(c.get("lookupSupported"))) return Result.UNKNOWN;
    return effectApplied(externalOperationId) ? Result.CONFIRMED_SUCCESS : Result.CONFIRMED_FAILURE;
  }

  private boolean effectApplied(String externalOperationId) {
    return requests.stream().anyMatch(r -> externalOperationId.equals(r.get("externalOperationId")) && Boolean.TRUE.equals(r.get("effectApplied")));
  }

  /** Every request received, in order; optionally only one externalOperationId. */
  public List<Map<String, Object>> requests(String externalOperationId) {
    return requests.stream().filter(r -> externalOperationId == null || externalOperationId.equals(r.get("externalOperationId"))).map(Map::copyOf).toList();
  }

  static String sha256(String s) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); }
    catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
  }
}
