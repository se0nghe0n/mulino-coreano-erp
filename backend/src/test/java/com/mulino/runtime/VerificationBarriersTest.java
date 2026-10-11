package com.mulino.runtime;

import com.mulino.application.runtime.VerificationBarriers;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;

/** The verification barrier pauses only the declared participant at its declared point, once, until resumed. */
class VerificationBarriersTest {
  private static MockHttpServletRequest request(String header) {
    var r = new MockHttpServletRequest("POST", "/api/ontology/commands/reserveQuantity");
    r.setRemoteAddr("127.0.0.1");
    if (header != null) r.addHeader(VerificationBarriers.HEADER, header);
    return r;
  }
  private static String header(String participant, String point) {
    return "{\"barrierId\":\"race\",\"participantId\":\"" + participant + "\",\"transactionId\":\"tx-" + participant + "\",\"point\":\"" + point + "\"}";
  }

  @Test void declaredParticipantPausesAtItsPointUntilResumed() throws Exception {
    var barriers = new VerificationBarriers();
    var passed = new CopyOnWriteArrayList<String>();
    var pool = Executors.newSingleThreadExecutor();
    try {
      Future<?> call = pool.submit(() -> { barriers.doFilter(request(header("winner", "AFTER_SCOPE_LOCK")), new MockHttpServletResponse(), (q, s) -> {
        barriers.reach("AFTER_AUTH_BEFORE_IDEMPOTENCY_FENCE"); passed.add("idempotency");
        barriers.reach("AFTER_SCOPE_LOCK"); passed.add("scope");
        barriers.reach("AFTER_SCOPE_LOCK"); passed.add("again"); // a retried transaction does not pause twice
      }); return null; });
      var reached = barriers.waitFor("race", "winner", Set.of("REACHED"), Duration.ofSeconds(10));
      assertEquals("REACHED", reached.get("state"));
      assertEquals("tx-winner", reached.get("transactionId"));
      assertEquals(List.of("idempotency"), passed, "non-matching points never pause and the matching point holds the request");
      assertEquals("RESUMED", barriers.resume("race", "winner").get("state"));
      call.get(10, TimeUnit.SECONDS);
      assertEquals(List.of("idempotency", "scope", "again"), passed);
    } finally { pool.shutdownNow(); }
  }

  @Test void fenceWaitPointReportsBlockedWithoutPausing() throws Exception {
    var barriers = new VerificationBarriers();
    barriers.doFilter(request(header("revoke", "WAITING_FOR_GRANT_SCOPE_FENCE")), new MockHttpServletResponse(), (q, s) -> barriers.blocking("WAITING_FOR_GRANT_SCOPE_FENCE"));
    assertEquals("BLOCKED", barriers.waitFor("race", "revoke", Set.of("REACHED", "BLOCKED"), Duration.ofSeconds(1)).get("state"));
    assertEquals("RESUMED", barriers.resume("race", "revoke").get("state"));
  }

  @Test void requestsWithoutHeaderAreUnaffectedAndUnknownParticipantTimesOut() throws Exception {
    var barriers = new VerificationBarriers();
    var ran = new boolean[1];
    barriers.doFilter(request(null), new MockHttpServletResponse(), (q, s) -> { barriers.reach("AFTER_SCOPE_LOCK"); ran[0] = true; });
    assertTrue(ran[0]);
    var view = barriers.waitFor("race", "nobody", Set.of("REACHED"), Duration.ofMillis(100));
    assertEquals("NOT_SEEN", view.get("state"));
    assertEquals(true, view.get("timedOut"));
  }

  @Test void unsupportedPointOrNonLoopbackIsRefusedBeforeTheRequestRuns() throws Exception {
    var barriers = new VerificationBarriers();
    var unsupported = new MockHttpServletResponse();
    barriers.doFilter(request(header("x", "BEFORE_MRTR_APPROVAL_CONSUMPTION")), unsupported, (q, s) -> fail("must not run"));
    assertEquals(400, unsupported.getStatus());
    var remote = request(header("x", "AFTER_SCOPE_LOCK")); remote.setRemoteAddr("10.0.0.5");
    var forbidden = new MockHttpServletResponse();
    barriers.doFilter(remote, forbidden, (q, s) -> fail("must not run"));
    assertEquals(403, forbidden.getStatus());
  }
}
