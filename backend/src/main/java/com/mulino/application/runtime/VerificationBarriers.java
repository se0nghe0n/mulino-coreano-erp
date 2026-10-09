package com.mulino.application.runtime;

import com.mulino.application.core.CommandBarrier;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Explicit verification profile only (plan section 10: barriers control races in the dev/CI profile). A loopback
 * request may carry {@value #HEADER} naming one barrier participant and one pause point. The write pipeline calls
 * {@link #reach} at its named locations; only the matching request pauses there, once, until a resume control or the
 * bounded wait expires. An expired pause fails the request closed (the transaction rolls back), never continues as if
 * it had been resumed. Requests without the header are never affected.
 */
@Component
@Profile("verification")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class VerificationBarriers implements CommandBarrier, Filter {
  public static final String HEADER = "X-Mulino-Test-Barrier";
  /** Every point some pipeline location reports; anything else is refused before the request starts. */
  public static final Set<String> POINTS = Set.of("AFTER_AUTH_BEFORE_IDEMPOTENCY_FENCE", "AFTER_INITIAL_READ_BEFORE_SCOPE_LOCK",
      "BEFORE_SCOPE_LOCK", "AFTER_INITIAL_READ_BEFORE_SCOPE_FENCE", "AFTER_ELIGIBILITY_READ_BEFORE_SCOPE_FENCE",
      "AFTER_AUTH_READ_BEFORE_SCOPE_FENCE", "AFTER_SCOPE_LOCK", "AFTER_AUTH_SCOPE_FENCE_ACQUIRED_BEFORE_EFFECT_COMMIT",
      "WAITING_FOR_GRANT_SCOPE_FENCE", "WAITING_FOR_SCOPE_FENCE");
  static final Duration PAUSE_LIMIT = Duration.ofSeconds(30);
  private static final Set<String> LOOPBACK = Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1");
  private final ThreadLocal<Participant> current = new ThreadLocal<>();
  private final ConcurrentMap<String, Participant> participants = new ConcurrentHashMap<>();
  private final ObjectMapper json = new ObjectMapper();

  /** One declared participant of one barrier: its point, observed state and the latch its resume releases. */
  static final class Participant {
    final String barrierId, participantId, transactionId, point;
    final CountDownLatch resumed = new CountDownLatch(1);
    volatile String state = "DECLARED";
    volatile Instant reachedAt, resumedAt;
    volatile boolean fired;
    Participant(String barrierId, String participantId, String transactionId, String point) {
      this.barrierId = barrierId; this.participantId = participantId; this.transactionId = transactionId; this.point = point;
    }
    Map<String, Object> view() {
      var m = new LinkedHashMap<String, Object>();
      m.put("barrierId", barrierId); m.put("participantId", participantId); m.put("transactionId", transactionId);
      m.put("point", point); m.put("state", state);
      if (reachedAt != null) m.put("reachedAt", reachedAt.toString());
      if (resumedAt != null) m.put("resumedAt", resumedAt.toString());
      return m;
    }
  }

  static String key(String barrierId, String participantId) { return barrierId + "\u0000" + participantId; }

  @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    String header = request instanceof HttpServletRequest http ? http.getHeader(HEADER) : null;
    if (header == null) { chain.doFilter(request, response); return; }
    if (!LOOPBACK.contains(request.getRemoteAddr())) { ((HttpServletResponse) response).sendError(403, "Verification barrier requires loopback"); return; }
    Participant declared;
    try {
      @SuppressWarnings("unchecked") Map<String, Object> body = json.readValue(header, Map.class);
      if (!body.keySet().equals(Set.of("barrierId", "participantId", "transactionId", "point"))) throw new IllegalArgumentException("barrier fields");
      String point = String.valueOf(body.get("point"));
      if (!POINTS.contains(point)) throw new IllegalArgumentException("unsupported point");
      declared = new Participant(String.valueOf(body.get("barrierId")), String.valueOf(body.get("participantId")), String.valueOf(body.get("transactionId")), point);
    } catch (RuntimeException invalid) { ((HttpServletResponse) response).sendError(400, "Invalid verification barrier header"); return; }
    participants.put(key(declared.barrierId, declared.participantId), declared);
    current.set(declared);
    try { chain.doFilter(request, response); }
    finally { current.remove(); }
  }

  @Override public void reach(String... points) {
    Participant p = matching(points);
    if (p == null) return;
    p.reachedAt = Instant.now(); p.state = "REACHED";
    boolean released;
    try { released = p.resumed.await(PAUSE_LIMIT.toSeconds(), TimeUnit.SECONDS); }
    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); p.state = "INTERRUPTED"; throw new IllegalStateException("Verification barrier interrupted"); }
    if (!released) { p.state = "TIMED_OUT"; throw new IllegalStateException("Verification barrier was not resumed within " + PAUSE_LIMIT); }
    if (p.resumedAt == null) p.resumedAt = Instant.now();
    p.state = "RESUMED";
  }

  @Override public void blocking(String... points) {
    Participant p = matching(points);
    if (p == null) return;
    p.reachedAt = Instant.now(); p.state = "BLOCKED";
  }

  private Participant matching(String... points) {
    Participant p = current.get();
    if (p == null || p.fired || !Arrays.asList(points).contains(p.point)) return null;
    p.fired = true; // a retried transaction does not pause twice
    return p;
  }

  /** Control: wait until the participant reports one of the states, bounded by the timeout. */
  public Map<String, Object> waitFor(String barrierId, String participantId, Set<String> states, Duration timeout) throws InterruptedException {
    Instant deadline = Instant.now().plus(timeout);
    while (true) {
      Participant p = participants.get(key(barrierId, participantId));
      if (p != null && states.contains(p.state)) return p.view();
      if (Instant.now().isAfter(deadline)) {
        var view = p == null ? new LinkedHashMap<String, Object>(Map.of("barrierId", barrierId, "participantId", participantId, "state", "NOT_SEEN")) : new LinkedHashMap<>(p.view());
        view.put("timedOut", true); return view;
      }
      Thread.sleep(20);
    }
  }

  /** Control: release a paused participant. A BLOCKED participant never paused; its resume is an acknowledgement. */
  public Map<String, Object> resume(String barrierId, String participantId) {
    Participant p = participants.get(key(barrierId, participantId));
    if (p == null) return new LinkedHashMap<>(Map.of("barrierId", barrierId, "participantId", participantId, "state", "NOT_SEEN"));
    if (Set.of("REACHED", "BLOCKED").contains(p.state)) { p.resumedAt = Instant.now(); p.state = "RESUMED"; }
    p.resumed.countDown();
    return p.view();
  }
}
