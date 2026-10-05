package com.mulinocoreano.backend.evidence;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mulinocoreano.backend.execution.*;
import com.mulinocoreano.backend.interfacepackage.*;
import com.mulinocoreano.backend.security.*;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.*;

/** Goals 2,4,5: explicit human attestation, durable history and scoped live actors. */
@SpringBootTest(
    properties = {
      "mulino.local-auth.human-secret=test-human-gateway",
      "spring.flyway.schemas=evidence_claim_it",
      "spring.flyway.clean-disabled=false",
      "spring.datasource.hikari.schema=evidence_claim_it",
      "spring.flyway.init-sqls=CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public"
    })
@ActiveProfiles("local")
@AutoConfigureMockMvc
class EvidenceClaimIntegrationTest {
  @Autowired JdbcClient jdbc;
  @Autowired Flyway flyway;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired EvidenceClaimService service;
  @Autowired LocalActorDirectory actors;
  @Autowired DispatcherService dispatcher;
  @Autowired RunExecutionService execution;
  @Autowired RunService runs;
  @Autowired ContextSnapshotService contexts;
  HumanActor writer;
  String caseRef;
  long caseId;

  @BeforeEach
  void fixture() throws Exception {
    flyway.clean();
    flyway.migrate();
    writer = actors.humanForRole("OPERATOR");
    caseRef = createCase("case");
    caseId = id("SELECT case_id FROM cases WHERE case_ref='" + caseRef + "'");
  }

  String createCase(String key) throws Exception {
    return mapper
        .readTree(
            mvc.perform(
                    post("/api/v1/cases")
                        .header("X-Mulino-Local-Human", "test-human-gateway")
                        .header("X-Mulino-Local-Role", "OPERATOR")
                        .header("Idempotency-Key", key)
                        .contentType("application/json")
                        .content("{\"objective\":\"증거 기반 업무\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .path("caseRef")
        .asText();
  }

  long id(String sql) {
    return jdbc.sql(sql).query(Long.class).single();
  }

  EvidenceClaimService.Source source(String body, String predecessor) {
    return new EvidenceClaimService.Source(
        "EMAIL",
        "mail-original",
        "2026-10-05T12:30:00+09:00",
        "Supplier observation",
        body,
        null,
        null,
        predecessor,
        predecessor == null ? null : "Original date corrected");
  }

  JsonNode register(String content, String key) {
    return service.register(caseRef, source(content, null), writer, null, key);
  }

  JsonNode assertion() {
    return service.assertClaim(
        caseRef,
        new EvidenceClaimService.Assertion("ETA", "PO-1.expected", "Delivery tomorrow"),
        writer,
        null,
        "claim");
  }

  JsonNode link(long claim, JsonNode e, String relation, String key) {
    return service.link(
        caseRef,
        claim,
        new EvidenceClaimService.Link(e.path("evidence_ref").asText(), relation),
        writer,
        null,
        key);
  }

  EvidenceClaimService.Judgment judgment(JsonNode c, String state) {
    return new EvidenceClaimService.Judgment(
        state,
        c.path("revision").asLong(),
        c.path("evidenceFingerprint").asText(),
        "Human source review, no ERP authority");
  }

  JsonNode review(long claim) {
    return service.review(caseRef, claim, writer, null);
  }

  @Test
  void supportIsOnlyAssertionAndHumanJudgmentPreservesExactSources() {
    var e = register("1.234567890123456789 and promised tomorrow", "source");
    long c = assertion().path("claim_id").asLong();
    var linked = link(c, e, "SUPPORTS", "link");
    assertThat(linked.path("status").asText()).isEqualTo("ASSERTED");
    var verified =
        service.judge(caseRef, c, judgment(linked, "VERIFIED"), writer, null, "judgment");
    assertThat(verified.path("status").asText()).isEqualTo("VERIFIED");
    assertThat(verified.path("sources").get(0).path("content").asText())
        .isEqualTo("1.234567890123456789 and promised tomorrow");
    assertThat(verified.path("sources").get(0).path("writer_user_id").asLong())
        .isEqualTo(writer.userId());
    assertThat(id("SELECT count(*) FROM purchase_orders")).isZero();
  }

  @Test
  void contradictoryLinkRetainsVerifiedHistoryAndRequiresConflictedHumanJudgment() {
    long c = assertion().path("claim_id").asLong();
    var s = link(c, register("Tomorrow", "support"), "SUPPORTS", "link");
    service.judge(caseRef, c, judgment(s, "VERIFIED"), writer, null, "verified");
    var conflicted = link(c, register("Next month", "counter"), "REFUTES", "refutes");
    assertThat(conflicted.path("status").asText()).isEqualTo("CONFLICTED");
    assertThat(conflicted.path("judgment_stale").asBoolean()).isTrue();
    assertThat(conflicted.path("judgments").get(1).path("status").asText()).isEqualTo("VERIFIED");
    assertThatThrownBy(
            () -> service.judge(caseRef, c, judgment(conflicted, "VERIFIED"), writer, null, "bad"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThat(
            service
                .judge(caseRef, c, judgment(conflicted, "CONFLICTED"), writer, null, "conflict")
                .path("judgment_stale")
                .asBoolean())
        .isFalse();
    assertThat(id("SELECT count(*) FROM claim_evidence")).isEqualTo(2);
  }

  @Test
  void successorAndSuccessorOfSuccessorInvalidateOldAttestationWithoutOverwritingOriginal() {
    var original = register("Tomorrow", "original");
    long c = assertion().path("claim_id").asLong();
    var s = link(c, original, "SUPPORTS", "link");
    service.judge(caseRef, c, judgment(s, "VERIFIED"), writer, null, "v1");
    var correction =
        service.register(
            caseRef,
            source("Next week", original.path("evidence_ref").asText()),
            writer,
            null,
            "correction");
    var stale = review(c);
    assertThat(stale.path("status").asText()).isEqualTo("ASSERTED");
    assertThat(stale.path("sources").size()).isEqualTo(2);
    assertThat(stale.path("judgments").get(2).path("previous_status").asText())
        .isEqualTo("VERIFIED");
    service.judge(caseRef, c, judgment(stale, "VERIFIED"), writer, null, "v2");
    service.register(
        caseRef,
        source("Next month", correction.path("evidence_ref").asText()),
        writer,
        null,
        "correction2");
    assertThat(review(c).path("judgment_stale").asBoolean()).isTrue();
    assertThat(review(c).path("sources").size()).isEqualTo(3);
    assertThat(
            jdbc.sql("SELECT content FROM evidence WHERE evidence_id=:id")
                .param("id", original.path("evidence_id").asLong())
                .query(String.class)
                .single())
        .isEqualTo("Tomorrow");
    assertThatThrownBy(() -> jdbc.sql("UPDATE evidence SET content='erased'").update())
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.sql("DELETE FROM claim_judgments").update())
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  void registrationAndJudgmentReplayRemainIdenticalButChangedInputAndStaleRevisionFail() {
    var e = register("Tomorrow", "source");
    assertThat(register("Tomorrow", "source")).isEqualTo(e);
    assertThatThrownBy(() -> register("Other", "source"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    long c = assertion().path("claim_id").asLong();
    var s = link(c, e, "SUPPORTS", "link");
    var j = judgment(s, "VERIFIED");
    var v = service.judge(caseRef, c, j, writer, null, "judge");
    assertThat(service.judge(caseRef, c, j, writer, null, "judge")).isEqualTo(v);
    assertThatThrownBy(() -> service.judge(caseRef, c, j, writer, null, "stale"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThat(id("SELECT count(*) FROM claim_judgments")).isEqualTo(2);
  }

  @Test
  void crossCaseAndNonMemberRejectWithoutRelationsOrReceipts() throws Exception {
    var e = register("Original", "source");
    long c = assertion().path("claim_id").asLong();
    String other = createCase("other");
    var foreign = service.register(other, source("Foreign", null), writer, null, "foreign");
    assertThatThrownBy(() -> link(c, foreign, "SUPPORTS", "cross"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    var manager = actors.humanForRole("MANAGER");
    assertThatThrownBy(
            () -> service.register(caseRef, source("Unauthorized", null), manager, null, "unauth"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThat(id("SELECT count(*) FROM claim_evidence")).isZero();
    assertThat(
            id("SELECT count(*) FROM request_idempotency WHERE request_key IN ('cross','unauth')"))
        .isZero();
    assertThat(id("SELECT count(*) FROM evidence")).isEqualTo(2);
  }

  @Test
  void viewerServiceAndInactiveHumanCannotWriteOrReplay() {
    var source = source("source", null);
    service.register(caseRef, source, writer, null, "original");
    assertThatThrownBy(
            () -> service.register(caseRef, source, actors.humanForRole("VIEWER"), null, "viewer"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                service.register(
                    caseRef,
                    source,
                    new ServiceActor("issuer", "service", "worker", Set.of("worker:dispatch")),
                    null,
                    "service"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    jdbc.sql("UPDATE users SET is_active=FALSE WHERE user_id=:id")
        .param("id", writer.userId())
        .update();
    assertThatThrownBy(() -> service.register(caseRef, source, writer, null, "original"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThat(id("SELECT count(*) FROM evidence")).isEqualTo(1);
  }

  @Test
  void liveAgentWritesObservationsButCannotJudgeOrCrossCaseAndExpiredReplayFails()
      throws Exception {
    String wi =
        jdbc.sql("SELECT work_item_ref FROM work_items WHERE case_id=:case")
            .param("case", caseId)
            .query(String.class)
            .single();
    runs.createRun(new CreateRunRequest("ORCHESTRATOR", caseRef, wi, "CODEX"), null);
    var run = execution.claim("evidence-worker").orElseThrow();
    var actor = new AgentActor(run.runRef(), caseRef, wi, "ORCHESTRATOR");
    var e =
        service.register(
            caseRef, source("Run observation", null), actor, run.capabilityToken(), "source");
    assertThat(e.path("ingested_by_run_id").asLong())
        .isEqualTo(id("SELECT run_id FROM runs WHERE run_ref='" + run.runRef() + "'"));
    long c =
        service
            .assertClaim(
                caseRef,
                new EvidenceClaimService.Assertion("ETA", "PO-2", "tomorrow"),
                actor,
                run.capabilityToken(),
                "claim")
            .path("claim_id")
            .asLong();
    var l =
        service.link(
            caseRef,
            c,
            new EvidenceClaimService.Link(e.path("evidence_ref").asText(), "SUPPORTS"),
            actor,
            run.capabilityToken(),
            "link");
    assertThatThrownBy(
            () ->
                service.judge(
                    caseRef, c, judgment(l, "VERIFIED"), actor, run.capabilityToken(), "judge"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    String other = createCase("other");
    assertThatThrownBy(
            () ->
                service.register(
                    other, source("foreign", null), actor, run.capabilityToken(), "cross"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    jdbc.sql("UPDATE runs SET lease_expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second'").update();
    assertThatThrownBy(
            () ->
                service.register(
                    caseRef,
                    source("Run observation", null),
                    actor,
                    run.capabilityToken(),
                    "source"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThat(id("SELECT count(*) FROM evidence")).isEqualTo(1);
  }

  @Test
  void dispatcherManagedBypassRollsBackEventWhileLegacyRelationStillUsesSharedContract() {
    long c = assertion().path("claim_id").asLong();
    var e = register("Managed", "managed");
    assertThatThrownBy(
            () ->
                dispatcher.ingest(
                    new CreateEventRequest(
                        "SUPPLIER_REPLY",
                        "managed-event",
                        caseRef,
                        null,
                        Map.of("claimId", c, "evidenceRef", e.path("evidence_ref").asText()))))
        .isInstanceOf(InvalidInterfaceRequestException.class);
    assertThat(id("SELECT count(*) FROM events WHERE external_ref='managed-event'")).isZero();
    long legacy =
        jdbc.sql(
                "INSERT INTO claims(case_id,subject_type,subject_ref,claim_text,status,resolved_at)"
                    + " VALUES(:case,'ETA','OLD','Legacy','VERIFIED','2026-09-05 13:00:00')"
                    + " RETURNING claim_id")
            .param("case", caseId)
            .query(Long.class)
            .single();
    jdbc.sql(
            "INSERT INTO evidence(evidence_ref,case_id,source_type,content)"
                + " VALUES('EV-LEGACY',:case,'API','Historical')")
        .param("case", caseId)
        .update();
    var request =
        new CreateEventRequest(
            "OBSERVATION",
            "legacy-event",
            caseRef,
            null,
            Map.of("claimId", legacy, "evidenceRef", "EV-LEGACY"));
    dispatcher.ingest(request);
    dispatcher.ingest(request);
    assertThat(id("SELECT count(*) FROM claim_evidence")).isEqualTo(1);
    assertThat(
            id(
                "SELECT count(*) FROM evidence WHERE evidence_ref='EV-LEGACY' AND writer_principal"
                    + " IS NULL"))
        .isEqualTo(1);
    assertThat(id("SELECT count(*) FROM claim_judgments")).isEqualTo(1);
    assertThat(review(legacy).path("status").asText()).isEqualTo("ASSERTED");
    assertThat(review(legacy).path("judgments").get(0).path("previous_status").asText())
        .isEqualTo("VERIFIED");
    assertThat(review(legacy).path("judgments").get(0).path("previous_resolved_at").asText())
        .isEqualTo("2026-09-05T13:00:00");
    service.register(
        caseRef, source("Legacy corrected", "EV-LEGACY"), writer, null, "legacy-correction");
    assertThat(review(legacy).path("judgment_stale").asBoolean()).isTrue();
    assertThat(review(legacy).path("sources").size()).isEqualTo(2);
  }

  @Test
  void mismatchedInlineHashRollsBackAndDbRejectsCrossCaseCorrection() throws Exception {
    var a = source("inline", null);
    var bad =
        new EvidenceClaimService.Source(
            a.sourceType(),
            a.externalRef(),
            a.observedAt(),
            a.title(),
            a.content(),
            null,
            "0".repeat(64),
            null,
            null);
    assertThatThrownBy(() -> service.register(caseRef, bad, writer, null, "bad-hash"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThat(id("SELECT count(*) FROM evidence")).isZero();
    assertThat(id("SELECT count(*) FROM request_idempotency WHERE request_key='bad-hash'"))
        .isZero();
    var e = register("original", "original");
    String other = createCase("other");
    assertThatThrownBy(
            () ->
                service.register(
                    other,
                    source("corrected", e.path("evidence_ref").asText()),
                    writer,
                    null,
                    "bad-correction"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThat(id("SELECT count(*) FROM evidence")).isEqualTo(1);
  }

  @Test
  void concurrentExactReplayCreatesOneSourceAndConcurrentJudgmentsAcceptOneRevision()
      throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    try {
      var first = pool.submit(() -> register("Same source", "race"));
      var second = pool.submit(() -> register("Same source", "race"));
      assertThat(first.get()).isEqualTo(second.get());
      assertThat(id("SELECT count(*) FROM evidence")).isEqualTo(1);
      long c = assertion().path("claim_id").asLong();
      var s = link(c, first.get(), "SUPPORTS", "link");
      var j = judgment(s, "VERIFIED");
      Callable<Boolean> judge =
          () -> {
            try {
              service.judge(caseRef, c, j, writer, null, UUID.randomUUID().toString());
              return true;
            } catch (org.springframework.web.server.ResponseStatusException e) {
              return false;
            }
          };
      var one = pool.submit(judge);
      var two = pool.submit(judge);
      assertThat(List.of(one.get(), two.get())).containsExactlyInAnyOrder(true, false);
      assertThat(id("SELECT count(*) FROM claim_judgments")).isEqualTo(2);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void failedProjectionRollsBackJudgmentHistoryAndReceiptTogether() {
    long c = assertion().path("claim_id").asLong();
    var s = link(c, register("Tomorrow", "source"), "SUPPORTS", "link");
    jdbc.sql(
            "CREATE FUNCTION reject_judgment_projection() RETURNS TRIGGER AS $$ BEGIN IF"
                + " NEW.status='VERIFIED' THEN RAISE EXCEPTION 'fixture fail' USING"
                + " ERRCODE='23514'; END IF; RETURN NEW; END; $$ LANGUAGE plpgsql")
        .update();
    jdbc.sql(
            "CREATE TRIGGER reject_projection BEFORE UPDATE ON claims FOR EACH ROW EXECUTE FUNCTION"
                + " reject_judgment_projection()")
        .update();
    assertThatThrownBy(
            () -> service.judge(caseRef, c, judgment(s, "VERIFIED"), writer, null, "rollback"))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThat(review(c).path("status").asText()).isEqualTo("ASSERTED");
    assertThat(id("SELECT count(*) FROM claim_judgments")).isEqualTo(1);
    assertThat(id("SELECT count(*) FROM request_idempotency WHERE request_key='rollback'"))
        .isZero();
  }

  @Test
  void newRunReconstructsCorrectedSourcesAndHumanAttestationExactly() {
    var original = register("0.000000000001 original", "original");
    long c = assertion().path("claim_id").asLong();
    var s = link(c, original, "SUPPORTS", "link");
    service.judge(caseRef, c, judgment(s, "VERIFIED"), writer, null, "v1");
    service.register(
        caseRef,
        source("0.000000000002 corrected", original.path("evidence_ref").asText()),
        writer,
        null,
        "correction");
    String wi =
        jdbc.sql("SELECT work_item_ref FROM work_items WHERE case_id=:case")
            .param("case", caseId)
            .query(String.class)
            .single();
    runs.createRun(new CreateRunRequest("ORCHESTRATOR", caseRef, wi, "CODEX"), null);
    var next = execution.claim("new-worker").orElseThrow();
    var context = mapper.valueToTree(next.context()).path("epistemic");
    assertThat(context.path("evidence").size()).isEqualTo(2);
    assertThat(context.path("evidence").get(0).path("provenance").path("content").asText())
        .isEqualTo("0.000000000001 original");
    assertThat(context.path("evidence").get(1).path("provenance").path("content").asText())
        .isEqualTo("0.000000000002 corrected");
    assertThat(context.path("claims").get(0).path("status").asText()).isEqualTo("ASSERTED");
    assertThat(context.path("claims").get(0).path("judgments").get(1).path("status").asText())
        .isEqualTo("VERIFIED");
    assertThat(context.path("claims").get(0).path("judgment_stale").asBoolean()).isTrue();
  }

  @Test
  void revokedAgentMembershipAndInactiveAgentRejectPreviouslySuccessfulReplay() {
    String wi =
        jdbc.sql("SELECT work_item_ref FROM work_items WHERE case_id=:case")
            .param("case", caseId)
            .query(String.class)
            .single();
    runs.createRun(new CreateRunRequest("ORCHESTRATOR", caseRef, wi, "CODEX"), null);
    var run = execution.claim("membership-worker").orElseThrow();
    var actor = new AgentActor(run.runRef(), caseRef, wi, "ORCHESTRATOR");
    var request = source("Durable observation", null);
    service.register(caseRef, request, actor, run.capabilityToken(), "observation");
    jdbc.sql("DELETE FROM case_participants WHERE actor_type='AGENT' AND case_id=:case")
        .param("case", caseId)
        .update();
    assertThatThrownBy(
            () -> service.register(caseRef, request, actor, run.capabilityToken(), "observation"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    jdbc.sql(
            "INSERT INTO case_participants(case_id,actor_type,agent_id) SELECT"
                + " :case,'AGENT',agent_id FROM agents WHERE agent_key='ORCHESTRATOR'")
        .param("case", caseId)
        .update();
    jdbc.sql("UPDATE agents SET is_active=FALSE WHERE agent_key='ORCHESTRATOR'").update();
    assertThatThrownBy(
            () -> service.register(caseRef, request, actor, run.capabilityToken(), "observation"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThat(id("SELECT count(*) FROM evidence")).isEqualTo(1);
  }

  @Test
  void explicitSuccessorCanBeAttestedWhileOriginalCounterevidenceAndConflictRemain()
      throws Exception {
    long old = assertion().path("claim_id").asLong();
    var original = register("Tomorrow", "support");
    link(old, original, "SUPPORTS", "s");
    var counter = register("Next month", "counter");
    var conflicted = link(old, counter, "REFUTES", "r");
    service.judge(caseRef, old, judgment(conflicted, "CONFLICTED"), writer, null, "old-human");
    var corrected =
        service.register(
            caseRef,
            source("Tomorrow confirmed by supplier", counter.path("evidence_ref").asText()),
            writer,
            null,
            "corrected");
    // Both original and successor are linked; each source/relation appears exactly once.
    var deduped = link(old, corrected, "REFUTES", "corrected-refute");
    assertThat(deduped.path("sources").size()).isEqualTo(3);
    assertThatThrownBy(
            () ->
                service.judge(
                    caseRef, old, judgment(deduped, "VERIFIED"), writer, null, "cannot-erase"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    var next =
        new EvidenceClaimService.Assertion(
            "ETA",
            "PO-1.expected",
            "Delivery tomorrow after corrected source review",
            old,
            "Counterevidence was corrected; retain old disagreement and assess new assertion");
    long current =
        service.assertClaim(caseRef, next, writer, null, "successor").path("claim_id").asLong();
    var support = link(current, corrected, "SUPPORTS", "new-support");
    var verified =
        service.judge(caseRef, current, judgment(support, "VERIFIED"), writer, null, "new-human");
    assertThat(verified.path("status").asText()).isEqualTo("VERIFIED");
    assertThat(verified.path("supersedes_claim_id").asLong()).isEqualTo(old);
    assertThat(review(old).path("status").asText()).isEqualTo("CONFLICTED");
    assertThat(review(old).path("judgments").get(2).path("status").asText())
        .isEqualTo("CONFLICTED");
    String other = createCase("other");
    assertThatThrownBy(() -> service.assertClaim(other, next, writer, null, "cross-successor"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                service.assertClaim(
                    caseRef,
                    new EvidenceClaimService.Assertion(
                        "LOT", "Other", "Unrelated", old, "Unrelated subject"),
                    writer,
                    null,
                    "bad-subject"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThat(id("SELECT count(*) FROM claims")).isEqualTo(2);
  }

  @Test
  void migrationPreservesLegacyUnattachedSourcesAndHistoricalJudgmentWithoutInventedAuthor() {
    flyway.clean();
    Flyway.configure()
        .dataSource(flyway.getConfiguration().getDataSource())
        .schemas("evidence_claim_it")
        .target("30")
        .initSql("CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public")
        .load()
        .migrate();
    jdbc.sql(
            "INSERT INTO evidence(evidence_ref,source_type,content,observed_at)"
                + " VALUES('EV-UNATTACHED','API','0.1234567890123456789 historical','2026-09-05"
                + " 12:00:00')")
        .update();
    jdbc.sql(
            "INSERT INTO cases(case_ref,title,objective,intent_type)"
                + " VALUES('CASE-OLD','Historical','Legacy','ACT')")
        .update();
    jdbc.sql(
            "INSERT INTO claims(case_id,subject_type,subject_ref,claim_text,status,resolved_at)"
                + " SELECT case_id,'ETA','OLD','Historical verified','VERIFIED','2026-09-05"
                + " 13:00:00' FROM cases WHERE case_ref='CASE-OLD'")
        .update();
    jdbc.sql(
            "INSERT INTO evidence(evidence_ref,case_id,source_type,content) SELECT"
                + " 'EV-OLD-SCOPED',case_id,'API','Historical scoped source' FROM cases WHERE"
                + " case_ref='CASE-OLD'")
        .update();
    flyway.migrate();
    assertThat(
            id(
                "SELECT count(*) FROM evidence WHERE evidence_ref='EV-UNATTACHED' AND case_id IS"
                    + " NULL AND writer_principal IS NULL AND writer_user_id IS NULL AND"
                    + " writer_agent_id IS NULL AND observed_instant IS NULL"))
        .isEqualTo(1);
    assertThat(
            jdbc.sql("SELECT content FROM evidence WHERE evidence_ref='EV-UNATTACHED'")
                .query(String.class)
                .single())
        .isEqualTo("0.1234567890123456789 historical");
    assertThat(
            id(
                "SELECT count(*) FROM claims WHERE status='VERIFIED' AND revision=0 AND"
                    + " writer_principal IS NULL"))
        .isEqualTo(1);
    assertThat(id("SELECT count(*) FROM claim_judgments")).isZero();
    var currentWriter = actors.humanForRole("OPERATOR");
    jdbc.sql(
            "INSERT INTO case_participants(case_id,actor_type,user_id) SELECT case_id,'USER',:user"
                + " FROM cases WHERE case_ref='CASE-OLD'")
        .param("user", currentWriter.userId())
        .update();
    long oldClaim = id("SELECT claim_id FROM claims");
    var stale =
        service.link(
            "CASE-OLD",
            oldClaim,
            new EvidenceClaimService.Link("EV-OLD-SCOPED", "SUPPORTS"),
            currentWriter,
            null,
            "old-link");
    assertThat(stale.path("status").asText()).isEqualTo("ASSERTED");
    assertThat(stale.path("judgments").get(0).path("previous_resolved_at").asText())
        .isEqualTo("2026-09-05T13:00:00");
    var reattested =
        service.judge(
            "CASE-OLD",
            oldClaim,
            judgment(stale, "VERIFIED"),
            currentWriter,
            null,
            "old-human-reattestation");
    assertThat(reattested.path("status").asText()).isEqualTo("VERIFIED");
    assertThat(reattested.path("judgments").get(0).path("previous_resolved_at").asText())
        .isEqualTo("2026-09-05T13:00:00");
    assertThat(reattested.path("judgments").get(0).path("evidence_fingerprint").isNull()).isTrue();
    assertThat(reattested.path("writer_principal").isNull()).isTrue();
    assertThat(reattested.path("judgments").get(1).path("writer_user_id").asLong())
        .isEqualTo(currentWriter.userId());
  }

  @Test
  void concurrentLegacyEventsUseOneLockedClaimAndRetainBothRelationsWithoutDeadlock()
      throws Exception {
    long legacy =
        jdbc.sql(
                "INSERT INTO claims(case_id,subject_type,subject_ref,claim_text,status,resolved_at)"
                    + " VALUES(:case,'ETA','OLD','Legacy','VERIFIED',CURRENT_TIMESTAMP) RETURNING"
                    + " claim_id")
            .param("case", caseId)
            .query(Long.class)
            .single();
    jdbc.sql(
            "INSERT INTO evidence(evidence_ref,case_id,source_type,content)"
                + " VALUES('EV-EVENT',:case,'API','Source')")
        .param("case", caseId)
        .update();
    var pool = Executors.newFixedThreadPool(2);
    try {
      var support =
          pool.submit(
              () ->
                  dispatcher.ingest(
                      new CreateEventRequest(
                          "OBSERVATION",
                          "event-support",
                          caseRef,
                          null,
                          Map.of(
                              "claimId",
                              legacy,
                              "evidenceRef",
                              "EV-EVENT",
                              "relation",
                              "SUPPORTS"))));
      var refute =
          pool.submit(
              () ->
                  dispatcher.ingest(
                      new CreateEventRequest(
                          "OBSERVATION",
                          "event-refute",
                          caseRef,
                          null,
                          Map.of(
                              "claimId",
                              legacy,
                              "evidenceRef",
                              "EV-EVENT",
                              "relation",
                              "REFUTES"))));
      support.get(20, TimeUnit.SECONDS);
      refute.get(20, TimeUnit.SECONDS);
      assertThat(review(legacy).path("status").asText()).isEqualTo("CONFLICTED");
      assertThat(id("SELECT count(*) FROM claim_evidence")).isEqualTo(2);
      assertThat(id("SELECT count(*) FROM claim_judgments")).isEqualTo(2);
      assertThat(id("SELECT count(*) FROM events WHERE external_ref LIKE 'event-%'")).isEqualTo(2);
    } finally {
      pool.shutdownNow();
    }
  }
}
