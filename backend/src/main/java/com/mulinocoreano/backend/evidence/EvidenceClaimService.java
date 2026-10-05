package com.mulinocoreano.backend.evidence;

import com.mulinocoreano.backend.execution.RunCapabilityAccess;
import com.mulinocoreano.backend.idempotency.RequestIdempotency;
import com.mulinocoreano.backend.interfacepackage.ClaimEvidenceLinks;
import com.mulinocoreano.backend.planning.CanonicalJson;
import com.mulinocoreano.backend.security.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

/** Source registration is an observation; human attestation never grants ERP authority. */
@Service
public class EvidenceClaimService {
  private final JdbcClient jdbc;
  private final CanonicalJson json;
  private final RequestIdempotency receipts;
  private final EvidenceTransactions transactions;
  private final RunCapabilityAccess runs;
  private final ClaimEvidenceLinks links;

  public EvidenceClaimService(
      JdbcClient jdbc,
      CanonicalJson json,
      RequestIdempotency receipts,
      EvidenceTransactions transactions,
      RunCapabilityAccess runs,
      ClaimEvidenceLinks links) {
    this.jdbc = jdbc;
    this.json = json;
    this.receipts = receipts;
    this.transactions = transactions;
    this.runs = runs;
    this.links = links;
  }

  public record Source(
      String sourceType,
      String externalRef,
      String observedAt,
      String title,
      String content,
      String contentUri,
      String contentHash,
      String correctsEvidenceRef,
      String correctionReason) {}

  public record Assertion(
      String subjectType,
      String subjectRef,
      String claimText,
      Long supersedesClaimId,
      String supersessionReason) {
    public Assertion(String subjectType, String subjectRef, String claimText) {
      this(subjectType, subjectRef, claimText, null, null);
    }
  }

  public record Link(String evidenceRef, String relation) {}

  public record Judgment(
      String status, Long expectedRevision, String evidenceFingerprint, String reason) {}

  private record Writer(long caseId, String principal, Long userId, Long agentId, Long runId) {}

  public JsonNode register(
      String caseRef, Source source, ErpActor actor, String token, String key) {
    return transactions.execute(
        () -> {
          Writer w = authorize(caseRef, actor, token, false);
          text(source.sourceType(), 50);
          text(source.externalRef(), 255);
          text(source.title(), 200);
          if (!Set.of("EMAIL", "EXCEL_3PL", "API", "SLACK", "MANUAL").contains(source.sourceType()))
            throw invalid();
          try {
            OffsetDateTime.parse(source.observedAt());
          } catch (Exception e) {
            throw invalid();
          }
          String hash;
          if (source.content() != null && !source.content().isBlank()) {
            text(source.content(), 262144);
            hash = hash(source.content());
            if (source.contentHash() != null && !hash.equals(source.contentHash())) throw invalid();
          } else {
            text(source.contentUri(), 500);
            if (source.contentHash() == null || !source.contentHash().matches("[0-9a-f]{64}"))
              throw invalid();
            hash = source.contentHash();
          }
          if (source.contentUri() != null) {
            try {
              var uri = java.net.URI.create(source.contentUri());
              if (!"https".equals(uri.getScheme())
                  || uri.getHost() == null
                  || uri.getUserInfo() != null) throw invalid();
            } catch (IllegalArgumentException e) {
              throw invalid();
            }
          }
          if (source.correctsEvidenceRef() != null) text(source.correctionReason(), 4000);
          else if (source.correctionReason() != null) throw invalid();
          return receipts.executeCanonicalJson(
              namespace(w, "source"),
              key,
              source,
              () -> {
                Long predecessor = null;
                if (source.correctsEvidenceRef() != null) {
                  predecessor = sourceId(w.caseId(), source.correctsEvidenceRef());
                  // Corrections form one auditable chain; concurrent divergent successors conflict.
                  if (jdbc.sql("SELECT count(*) FROM evidence WHERE corrects_evidence_id=:id")
                          .param("id", predecessor)
                          .query(Long.class)
                          .single()
                      > 0) throw conflict();
                }
                return tree(
                    jdbc.sql(
                            """
                            INSERT INTO evidence(evidence_ref,case_id,source_type,external_ref,title,content,content_uri,
                                content_hash,observed_at,ingested_by_run_id,writer_principal,writer_user_id,writer_agent_id,
                                corrects_evidence_id,correction_reason,observed_instant)
                            VALUES(:ref,:case,:type,:external,:title,:content,:uri,:hash,CAST(:observed AS timestamptz) AT TIME ZONE 'UTC',
                                :run,:principal,:user,:agent,:predecessor,:reason,CAST(:observed AS timestamptz))
                            RETURNING to_jsonb(evidence)::text
                            """)
                        .param("ref", ref())
                        .param("case", w.caseId())
                        .param("type", source.sourceType())
                        .param("external", source.externalRef())
                        .param("title", source.title())
                        .param("content", source.content())
                        .param("uri", source.contentUri())
                        .param("hash", hash)
                        .param("observed", source.observedAt())
                        .param("run", w.runId())
                        .param("principal", w.principal())
                        .param("user", w.userId())
                        .param("agent", w.agentId())
                        .param("predecessor", predecessor)
                        .param("reason", source.correctionReason())
                        .query(String.class)
                        .single());
              });
        });
  }

  public JsonNode assertClaim(
      String caseRef, Assertion a, ErpActor actor, String token, String key) {
    return transactions.execute(
        () -> {
          Writer w = authorize(caseRef, actor, token, false);
          text(a.subjectType(), 50);
          text(a.subjectRef(), 100);
          text(a.claimText(), 16000);
          if (a.supersedesClaimId() != null) {
            text(a.supersessionReason(), 4000);
            requireClaim(w.caseId(), a.supersedesClaimId());
            var predecessor = claim(a.supersedesClaimId());
            if (!a.subjectType().equals(predecessor.path("subject_type").asText())
                || !a.subjectRef().equals(predecessor.path("subject_ref").asText()))
              throw invalid();
          } else if (a.supersessionReason() != null) throw invalid();
          return receipts.executeCanonicalJson(
              namespace(w, "claim"),
              key,
              a,
              () ->
                  tree(
                      jdbc.sql(
                              """
                              INSERT INTO claims(case_id,subject_type,subject_ref,claim_text,asserted_by_agent_id,
                                  asserted_by_user_id,asserted_by_run_id,writer_principal,supersedes_claim_id,supersession_reason)
                              VALUES(:case,:type,:ref,:text,:agent,:user,:run,:principal,:supersedes,:reason) RETURNING to_jsonb(claims)::text
                              """)
                          .param("case", w.caseId())
                          .param("type", a.subjectType())
                          .param("ref", a.subjectRef())
                          .param("text", a.claimText())
                          .param("supersedes", a.supersedesClaimId())
                          .param("reason", a.supersessionReason())
                          .param("agent", w.agentId())
                          .param("user", w.userId())
                          .param("run", w.runId())
                          .param("principal", w.principal())
                          .query(String.class)
                          .single()));
        });
  }

  public JsonNode link(
      String caseRef, long claimId, Link l, ErpActor actor, String token, String key) {
    return transactions.execute(
        () -> {
          Writer w = authorize(caseRef, actor, token, false);
          requireClaim(w.caseId(), claimId);
          long source = sourceId(w.caseId(), l.evidenceRef());
          if (l.relation() == null || !Set.of("SUPPORTS", "REFUTES").contains(l.relation()))
            throw invalid();
          return receipts.executeCanonicalJson(
              namespace(w, "link:" + claimId),
              key,
              l,
              () -> {
                links.insert(claimId, source, l.relation());
                return claim(claimId);
              });
        });
  }

  public JsonNode judge(
      String caseRef, long claimId, Judgment j, ErpActor actor, String token, String key) {
    return transactions.execute(
        () -> {
          Writer w = authorize(caseRef, actor, token, true);
          requireClaim(w.caseId(), claimId);
          text(j.reason(), 4000);
          if (j.status() == null
              || !Set.of("VERIFIED", "REFUTED", "CONFLICTED").contains(j.status())
              || j.expectedRevision() == null
              || j.evidenceFingerprint() == null
              || !j.evidenceFingerprint().matches("[0-9a-f]{64}")) throw invalid();
          return receipts.executeCanonicalJson(
              namespace(w, "judge:" + claimId),
              key,
              j,
              () -> {
                JsonNode current = claim(claimId);
                if (current.path("revision").asLong() != j.expectedRevision()
                    || !current
                        .path("evidenceFingerprint")
                        .asText()
                        .equals(j.evidenceFingerprint())) throw conflict();
                boolean supports = current.path("supports").asBoolean(),
                    refutes = current.path("refutes").asBoolean();
                if ((j.status().equals("VERIFIED") && (!supports || refutes))
                    || (j.status().equals("REFUTED") && (!refutes || supports))
                    || (j.status().equals("CONFLICTED") && !refutes)) throw conflict();
                jdbc.sql(
                        """
                        INSERT INTO claim_judgments(claim_id,revision,previous_status,previous_resolved_at,status,reason,evidence_fingerprint,
                            writer_principal,writer_user_id,stale)
                        SELECT claim_id,revision+1,status,resolved_at,CAST(:status AS claim_status),:reason,:hash,:principal,:user,FALSE
                        FROM claims WHERE claim_id=:id
                        """)
                    .param("id", claimId)
                    .param("status", j.status())
                    .param("reason", j.reason())
                    .param("hash", j.evidenceFingerprint())
                    .param("principal", w.principal())
                    .param("user", w.userId())
                    .update();
                jdbc.sql(
                        "UPDATE claims SET status=CAST(:status AS"
                            + " claim_status),revision=revision+1,judgment_fingerprint=:hash,judgment_stale=FALSE,resolved_at=CURRENT_TIMESTAMP"
                            + " WHERE claim_id=:id")
                    .param("id", claimId)
                    .param("status", j.status())
                    .param("hash", j.evidenceFingerprint())
                    .update();
                return claim(claimId);
              });
        });
  }

  public JsonNode review(String caseRef, long claimId, ErpActor actor, String token) {
    return transactions.execute(
        () -> {
          authorize(caseRef, actor, token, false, true);
          requireClaim(caseId(caseRef), claimId);
          return claim(claimId);
        });
  }

  private Writer authorize(String ref, ErpActor actor, String token, boolean judgment) {
    return authorize(ref, actor, token, judgment, false);
  }

  private Writer authorize(
      String ref, ErpActor actor, String token, boolean judgment, boolean read) {
    if (actor == null || actor instanceof ServiceActor) throw forbidden();
    Long user = null, agent = null, run = null;
    long caseId;
    if (actor instanceof AgentActor a) {
      if (judgment) throw forbidden();
      var scope = runs.requireLocked(token, a.agentKey(), ref);
      caseId = scope.caseId();
      run = scope.runId();
      agent =
          jdbc.sql("SELECT agent_id FROM runs WHERE run_id=:id")
              .param("id", run)
              .query(Long.class)
              .single();
      if (jdbc.sql(
              "SELECT case_participant_id FROM case_participants WHERE case_id=:case AND"
                  + " agent_id=:agent FOR SHARE")
          .param("case", caseId)
          .param("agent", agent)
          .query(Long.class)
          .optional()
          .isEmpty()) throw forbidden();
    } else if (actor instanceof HumanActor h) {
      user = h.userId();
      String role =
          jdbc.sql("SELECT role::text FROM users WHERE user_id=:id AND is_active FOR SHARE")
              .param("id", user)
              .query(String.class)
              .optional()
              .orElseThrow(EvidenceClaimService::forbidden);
      if (!read && !Set.of("OPERATOR", "MANAGER").contains(role)) throw forbidden();
      caseId = caseId(ref);
      if (!read
          && jdbc.sql(
                      "SELECT count(*) FROM cases c WHERE c.case_id=:case AND"
                          + " (c.opened_by_user_id=:user OR EXISTS(SELECT 1 FROM case_participants"
                          + " p WHERE p.case_id=c.case_id AND p.user_id=:user) OR EXISTS(SELECT 1"
                          + " FROM work_items w WHERE w.case_id=c.case_id AND"
                          + " w.assigned_user_id=:user))")
                  .param("case", caseId)
                  .param("user", user)
                  .query(Long.class)
                  .single()
              == 0) throw forbidden();
      jdbc.sql(
              "SELECT case_participant_id FROM case_participants WHERE case_id=:case AND"
                  + " user_id=:user FOR SHARE")
          .param("case", caseId)
          .param("user", user)
          .query(Long.class)
          .list();
    } else throw forbidden();
    String state =
        jdbc.sql("SELECT status::text FROM cases WHERE case_id=:id FOR SHARE")
            .param("id", caseId)
            .query(String.class)
            .single();
    if (!read && Set.of("RESOLVED", "CLOSED").contains(state)) throw conflict();
    String principal = actor.issuer() + "|" + actor.subject();
    jdbc.sql(
            "SELECT"
                + " set_config('mulino.evidence_principal',:principal,TRUE),set_config('mulino.evidence_user',:user,TRUE),"
                + "set_config('mulino.evidence_run',:run,TRUE)")
        .param("principal", principal)
        .param("user", user == null ? "" : user.toString())
        .param("run", run == null ? "" : run.toString())
        .query((rs, n) -> true)
        .single();
    return new Writer(caseId, principal, user, agent, run);
  }

  private long caseId(String ref) {
    return jdbc.sql("SELECT case_id FROM cases WHERE case_ref=:ref")
        .param("ref", ref)
        .query(Long.class)
        .optional()
        .orElseThrow(EvidenceClaimService::missing);
  }

  private long sourceId(long caseId, String ref) {
    return jdbc.sql(
            "SELECT evidence_id FROM evidence WHERE evidence_ref=:ref AND case_id=:case FOR SHARE")
        .param("ref", ref)
        .param("case", caseId)
        .query(Long.class)
        .optional()
        .orElseThrow(EvidenceClaimService::missing);
  }

  private void requireClaim(long caseId, long id) {
    jdbc.sql("SELECT claim_id FROM claims WHERE claim_id=:id AND case_id=:case FOR UPDATE")
        .param("id", id)
        .param("case", caseId)
        .query(Long.class)
        .optional()
        .orElseThrow(EvidenceClaimService::missing);
  }

  private JsonNode claim(long id) {
    var source =
        tree(
            jdbc.sql(
                    """
                    WITH RECURSIVE sources AS (
                        SELECT e.*,ce.relation FROM evidence e JOIN claim_evidence ce USING(evidence_id) WHERE ce.claim_id=:id
                        UNION SELECT e.*,s.relation FROM evidence e JOIN sources s ON e.corrects_evidence_id=s.evidence_id
                    ) SELECT coalesce(jsonb_agg(to_jsonb(s) ORDER BY evidence_id,relation),'[]')::text FROM sources s
                    """)
                .param("id", id)
                .query(String.class)
                .single());
    var c =
        (tools.jackson.databind.node.ObjectNode)
            tree(
                jdbc.sql("SELECT to_jsonb(c)::text FROM claims c WHERE claim_id=:id")
                    .param("id", id)
                    .query(String.class)
                    .single());
    c.set("sources", source);
    c.put("evidenceFingerprint", json.sha256(source));
    var supports = false;
    var refutes = false;
    for (var s : source) {
      if (s.path("relation").asText().equals("SUPPORTS")) supports = true;
      else refutes = true;
    }
    c.put("supports", supports);
    c.put("refutes", refutes);
    c.set(
        "judgments",
        tree(
            jdbc.sql(
                    "SELECT coalesce(jsonb_agg(to_jsonb(j) ORDER BY revision),'[]')::text FROM"
                        + " claim_judgments j WHERE claim_id=:id")
                .param("id", id)
                .query(String.class)
                .single()));
    return c;
  }

  private JsonNode tree(String value) {
    return json.readTree(value);
  }

  private static String namespace(Writer w, String op) {
    return "evidence:" + hash(w.principal()) + ":" + w.caseId() + ":" + op;
  }

  private static String ref() {
    return "EV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 17);
  }

  private static void text(String value, int max) {
    if (value == null || value.isBlank() || value.length() > max) throw invalid();
  }

  private static String hash(String s) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static ResponseStatusException invalid() {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST);
  }

  private static ResponseStatusException missing() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND);
  }

  private static ResponseStatusException forbidden() {
    return new ResponseStatusException(HttpStatus.FORBIDDEN);
  }

  private static ResponseStatusException conflict() {
    return new ResponseStatusException(HttpStatus.CONFLICT);
  }
}
