package com.mulinocoreano.backend.recall;

import com.mulinocoreano.backend.execution.*;
import com.mulinocoreano.backend.governance.GovernancePolicy;
import com.mulinocoreano.backend.idempotency.RequestIdempotency;
import com.mulinocoreano.backend.interfacepackage.*;
import com.mulinocoreano.backend.persistence.PlanningDataGuard;
import com.mulinocoreano.backend.planning.CanonicalJson;
import com.mulinocoreano.backend.procurement.PurchaseDecisionRequest;
import com.mulinocoreano.backend.procurement.PurchaseTransactions;
import com.mulinocoreano.backend.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Service
public class RecallService {
  private final LotTraceService traces;
  private final JdbcClient jdbc;
  private final CanonicalJson json;
  private final RequestIdempotency keys;
  private final PlanningDataGuard guard;
  private final PurchaseTransactions transactions;
  private final GovernancePolicy policy;
  private final RunCapabilityAccess capabilities;
  private final RunExecutionService execution;
  private final DispatcherService dispatcher;
  private final Clock clock;
  private final RunService runService;

  public RecallService(
      LotTraceService traces,
      JdbcClient jdbc,
      CanonicalJson json,
      RequestIdempotency keys,
      PlanningDataGuard guard,
      PurchaseTransactions transactions,
      GovernancePolicy policy,
      RunCapabilityAccess capabilities,
      RunExecutionService execution,
      DispatcherService dispatcher,
      @Qualifier("planningClock") Clock clock,
      RunService runService) {
    this.traces = traces;
    this.runService = runService;
    this.jdbc = jdbc;
    this.json = json;
    this.keys = keys;
    this.guard = guard;
    this.transactions = transactions;
    this.policy = policy;
    this.capabilities = capabilities;
    this.execution = execution;
    this.dispatcher = dispatcher;
    this.clock = clock;
  }

  public record RecallRequest(String caseRef, String reason) {}

  public record ScopeRefs(long lotId, long workItemId, String caseRef) {}

  public JsonNode trace(long id) {
    return traces.trace(id);
  }

  @org.springframework.transaction.annotation.Transactional
  public JsonNode agentTrace(long id, String token, AgentActor actor) {
    if (!Set.of("QC", "SUPPLY_CHAIN").contains(actor.agentKey()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    var scope = capabilities.requireLocked(token, actor.agentKey(), actor.caseRef());
    requireScope(id, scope);
    return traces.trace(id);
  }

  private void requireScope(long id, RunCapabilityAccess.RunScope scope) {
    var refs =
        jdbc.sql(
                "SELECT (w.metadata->>'lotId')::bigint lot_id,w.work_item_id,c.case_ref FROM"
                    + " work_items w JOIN cases c USING(case_id) WHERE w.work_item_id=:work AND"
                    + " w.case_id=:case")
            .param("work", scope.workItemId())
            .param("case", scope.caseId())
            .query(
                (rs, n) ->
                    new ScopeRefs(
                        rs.getLong("lot_id"), rs.getLong("work_item_id"), rs.getString("case_ref")))
            .optional()
            .orElseThrow(() -> missing());
    if (refs.lotId() != id
        || refs.workItemId() != scope.workItemId()
        || !refs.caseRef().equals(scope.caseRef()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }

  public JsonNode approval(long id) {
    return tree(
        jdbc.sql(
                """
                  SELECT jsonb_build_object('approvalId',a.governance_action_id,'status',a.status,'requiredRole',a.required_role,
                  'version',q.version,'proposalHash',q.proposal_hash,'proposal',q.snapshot,'reason',q.reason,'incidentLotId',q.incident_lot_id,
                  'caseRef',c.case_ref,'workItemRef',w.work_item_ref,
                  'report',(SELECT to_jsonb(r) FROM recall_reports r WHERE r.governance_action_id=a.governance_action_id),
                  'recalls',(SELECT coalesce(jsonb_agg(to_jsonb(r) ORDER BY recall_id),'[]') FROM recalls r WHERE r.governance_action_id=a.governance_action_id),
                  'decision',(SELECT to_jsonb(d) FROM governance_decisions d WHERE d.governance_action_id=a.governance_action_id AND is_final))::text
                  FROM recall_proposals q JOIN governance_actions a USING(governance_action_id) JOIN cases c ON c.case_id=q.case_id JOIN work_items w ON w.work_item_id=q.work_item_id WHERE a.governance_action_id=:id
                """)
            .param("id", id)
            .query(String.class)
            .optional()
            .orElseThrow(() -> missing()));
  }

  @org.springframework.transaction.annotation.Transactional
  public JsonNode assign(long lotId, RecallRequest request, ErpActor actor, String key) {
    if (request.caseRef() == null
        || request.caseRef().isBlank()
        || request.reason() == null
        || request.reason().isBlank()
        || request.reason().length() > 4000)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    guard.lock();
    if (!(actor instanceof HumanActor human)
        || !human.capabilities().contains("work:write")
        || !jdbc.sql(
                "SELECT is_active AND role IN ('OPERATOR','MANAGER') FROM users WHERE user_id=:id"
                    + " FOR UPDATE")
            .param("id", human.userId())
            .query(Boolean.class)
            .optional()
            .orElse(false))
      throw new org.springframework.security.access.AccessDeniedException(
          "Operator delegation required");
    return keys.executeCanonicalJson(
        "recall.assign:" + human.userId(),
        key,
        Map.of("lotId", lotId, "request", request),
        () -> {
          traces.trace(lotId);
          long caseId =
              jdbc.sql(
                      "SELECT case_id FROM cases WHERE case_ref=:ref AND status::text NOT IN"
                          + " ('RESOLVED','CLOSED','CANCELLED') FOR UPDATE")
                  .param("ref", request.caseRef())
                  .query(Long.class)
                  .optional()
                  .orElseThrow(() -> missing());
          long agentId =
              jdbc.sql("SELECT agent_id FROM agents WHERE agent_key='QC' AND is_active FOR SHARE")
                  .query(Long.class)
                  .single();
          String ref = "WI-" + UUID.randomUUID().toString().substring(0, 12);
          jdbc.sql(
                  "INSERT INTO work_items(work_item_ref,case_id,title,assigned_agent_id,metadata)"
                      + " VALUES (:ref,:case,'리콜 조사',:agent,jsonb_build_object('lotId',:id))")
              .param("ref", ref)
              .param("case", caseId)
              .param("agent", agentId)
              .param("id", lotId)
              .update();
          jdbc.sql(
                  "INSERT INTO case_participants(case_id,actor_type,agent_id) VALUES"
                      + " (:case,'AGENT',:agent) ON CONFLICT DO NOTHING")
              .param("case", caseId)
              .param("agent", agentId)
              .update();
          var run =
              runService.createRun(
                  new CreateRunRequest("QC", request.caseRef(), ref, runService.defaultRuntime()),
                  null);
          if (!"QUEUED".equals(run.status()))
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "QC context could not be queued");
          return Map.of("status", "QUEUED", "workItemRef", ref, "runRef", run.runRef());
        });
  }

  public JsonNode propose(
      long lotId, RecallRequest request, ErpActor actor, String token, String key) {
    if (request.caseRef() == null
        || request.caseRef().isBlank()
        || request.reason() == null
        || request.reason().isBlank()
        || request.reason().length() > 4000)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    return transactions.execute(
        () -> {
          String namespace = "recall.propose:" + actor.subject();
          keys.coordinate(namespace, key);
          guard.lock();
          var source = traces.trace(lotId);
          var warehouseIds = new TreeSet<Long>();
          for (var row : source.path("productionLots"))
            if (!row.path("warehouse_id").isNull())
              warehouseIds.add(row.path("warehouse_id").asLong());
          for (long warehouseId : warehouseIds)
            keys.coordinate("planning.warehouse", Long.toString(warehouseId));
          long caseId =
              jdbc.sql(
                      "SELECT case_id FROM cases WHERE case_ref=:ref AND status::text NOT IN"
                          + " ('CLOSED','RESOLVED','CANCELLED') FOR UPDATE")
                  .param("ref", request.caseRef())
                  .query(Long.class)
                  .optional()
                  .orElseThrow(() -> missing());
          RunCapabilityAccess.RunScope scope = null;
          if (actor instanceof AgentActor agent) {
            scope = capabilities.requireLocked(token, "QC", request.caseRef());
            requireScope(lotId, scope);
          } else if (actor instanceof HumanActor human) {
            if (!human.capabilities().contains("work:write")
                || !jdbc.sql(
                        "SELECT is_active AND role IN ('OPERATOR','MANAGER') FROM users WHERE"
                            + " user_id=:id FOR UPDATE")
                    .param("id", human.userId())
                    .query(Boolean.class)
                    .optional()
                    .orElse(false))
              throw new org.springframework.security.access.AccessDeniedException(
                  "Active operator required");
          } else
            throw new org.springframework.security.access.AccessDeniedException(
                "Proposal identity required");
          final var runScope = scope;
          return keys.executeCanonicalJson(
              namespace,
              key,
              Map.of("lotId", lotId, "request", request),
              () -> prepare(lotId, caseId, request, actor, runScope, source));
        });
  }

  private Object prepare(
      long id,
      long caseId,
      RecallRequest request,
      ErpActor actor,
      RunCapabilityAccess.RunScope scope,
      JsonNode source) {
    if (!source.path("complete").asBoolean())
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Incomplete LOT trace requires source repair");
    var prior =
        jdbc.sql(
                "SELECT q.version,q.case_id,q.work_item_id,a.status::text status FROM"
                    + " recall_proposals q JOIN governance_actions a USING(governance_action_id)"
                    + " WHERE incident_lot_id=:id ORDER BY q.version DESC LIMIT 1")
            .param("id", id)
            .query(
                (rs, n) ->
                    Map.<String, Object>of(
                        "version",
                        rs.getInt("version"),
                        "case_id",
                        rs.getLong("case_id"),
                        "work_item_id",
                        rs.getLong("work_item_id"),
                        "status",
                        rs.getString("status")))
            .optional();
    int version = 1;
    if (prior.isPresent()) {
      var row = prior.get();
      if (Set.of("PENDING", "APPROVED").contains(row.get("status"))
          || ((Number) row.get("case_id")).longValue() != caseId
          || scope != null && ((Number) row.get("work_item_id")).longValue() == scope.workItemId())
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Fresh explicit investigation required");
      version = ((Number) row.get("version")).intValue() + 1;
    }
    long agentId =
        jdbc.sql("SELECT agent_id FROM agents WHERE agent_key='QC' AND is_active FOR SHARE")
            .query(Long.class)
            .single();
    long work =
        scope == null
            ? jdbc.sql(
                    "INSERT INTO"
                        + " work_items(work_item_ref,case_id,title,status,assigned_agent_id,metadata)"
                        + " VALUES (:ref,:case,'리콜 조사와 ADMIN"
                        + " 판단','WAITING',:agent,jsonb_build_object('lotId',:id)) RETURNING"
                        + " work_item_id")
                .param("ref", "WI-" + UUID.randomUUID().toString().substring(0, 12))
                .param("case", caseId)
                .param("agent", agentId)
                .param("id", id)
                .query(Long.class)
                .single()
            : scope.workItemId();
    String hash = json.sha256(source);
    long action =
        jdbc.sql(
                "INSERT INTO"
                    + " governance_actions(requested_by,action_type,resource_type,resource_id,payload,required_role)"
                    + " VALUES ((SELECT user_id FROM users WHERE"
                    + " email='recall-service@mulino.internal'),'RECALL_CREATE','RECALL_PROPOSAL',:id,:source::jsonb,'ADMIN')"
                    + " RETURNING governance_action_id")
            .param("id", id)
            .param("source", source.toString())
            .query(Long.class)
            .single();
    long proposal =
        jdbc.sql(
                """
                  INSERT INTO recall_proposals(incident_lot_id,governance_action_id,case_id,work_item_id,proposed_by_agent_id,proposed_by_user_id,version,proposal_hash,snapshot,reason,retain_until)
                  VALUES (:id,:action,:case,:work,:agent,:user,:version,:hash,:source::jsonb,:reason,greatest(CURRENT_TIMESTAMP+INTERVAL '2 years',:expiry::date+INTERVAL '2 years')) RETURNING recall_proposal_id
                """)
            .param("id", id)
            .param("action", action)
            .param("case", caseId)
            .param("work", work)
            .param("version", version)
            .param("agent", actor instanceof AgentActor ? agentId : null)
            .param("user", actor instanceof HumanActor h ? h.userId() : null)
            .param("hash", hash)
            .param("source", source.toString())
            .param("reason", request.reason())
            .param("expiry", latestExpiry(source))
            .query(Long.class)
            .single();
    for (var lot : source.path("productionLots"))
      if (lot.path("affected").asBoolean())
        jdbc.sql("INSERT INTO recall_scope_lots(recall_proposal_id,lot_id) VALUES (:proposal,:lot)")
            .param("proposal", proposal)
            .param("lot", lot.path("id").asLong())
            .update();
    jdbc.sql(
            "INSERT INTO"
                + " attention_requests(case_id,work_item_id,reason_type,title,question,consequence,suggested_scope,requested_by_agent_id,governance_action_id)"
                + " VALUES (:case,:work,'AUTHORITY_REQUIRED','ADMIN 리콜 승인 필요',:reason,'승인 전 영향 LOT의"
                + " 출고·소비·계획을 차단한다','THIS_ACTION',:agent,:action)")
        .param("case", caseId)
        .param("work", work)
        .param("reason", request.reason())
        .param("agent", actor instanceof AgentActor ? agentId : null)
        .param("action", action)
        .update();
    // An offline draft exists immediately upon incident proposal; transmission is deliberately
    // absent.
    jdbc.sql(
            "INSERT INTO recall_reports(governance_action_id,due_at,retain_until,manifest) SELECT"
                + " :action,CURRENT_TIMESTAMP,retain_until,:manifest::jsonb FROM recall_proposals"
                + " WHERE recall_proposal_id=:proposal")
        .param("action", action)
        .param("proposal", proposal)
        .param(
            "manifest",
            json.write(
                Map.of(
                    "sourceTrace",
                    source,
                    "proposer",
                    actor,
                    "reason",
                    request.reason(),
                    "governanceActionId",
                    action,
                    "requiredRole",
                    "ADMIN",
                    "formVerified",
                    false)))
        .update();
    audit(
        action,
        null,
        "RECALL_PROPOSED",
        id,
        Map.of("source", source, "proposer", actor, "reason", request.reason()));
    if (scope != null) execution.awaitRecallApproval(scope.runId(), action, "ADMIN 리콜 결정 필요");
    else
      jdbc.sql("UPDATE cases SET status='WAITING' WHERE case_id=:id").param("id", caseId).update();
    return Map.of(
        "status",
        "PENDING_APPROVAL",
        "approvalId",
        action,
        "version",
        version,
        "proposalHash",
        hash,
        "executionResult",
        Map.of(
            "outcome",
            "WAITING",
            "summary",
            "ADMIN 승인 필요",
            "waitingConditions",
            List.of(),
            "resultRef",
            "APPROVAL-" + action));
  }

  private LocalDate latestExpiry(JsonNode source) {
    LocalDate date = LocalDate.now(clock);
    for (var lot : source.path("productionLots")) {
      var expiry = LocalDate.parse(lot.path("expiry_date").asText());
      if (expiry.isAfter(date)) date = expiry;
    }
    return date;
  }

  public JsonNode decide(long action, PurchaseDecisionRequest request, ErpActor actor, String key) {
    if (request == null
        || request.decision() == null
        || !Set.of("APPROVE", "BLOCK", "CANCEL").contains(request.decision())
        || request.reason() == null
        || request.reason().isBlank()
        || request.reason().length() > 4000
        || request.expectedVersion() == null
        || request.expectedVersion() < 1
        || request.proposalHash() == null
        || !request.proposalHash().matches("[0-9a-f]{64}"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    return transactions.execute(
        () -> {
          guard.lock();
          var human = policy.requireHuman(actor, GovernancePolicy.Action.RECALL_CREATE);
          policy.requireHuman(actor, GovernancePolicy.Action.PRODUCTION_RECALL);
          jdbc.sql(
                  "SELECT governance_action_id FROM governance_actions WHERE"
                      + " governance_action_id=:id FOR UPDATE")
              .param("id", action)
              .query(Long.class)
              .optional()
              .orElseThrow(() -> missing());
          return keys.executeCanonicalJson(
              "recall.decide:" + human.userId(),
              key,
              Map.of("approvalId", action, "request", request),
              () -> apply(action, request, human));
        });
  }

  private Object apply(long action, PurchaseDecisionRequest request, HumanActor human) {
    var proposal = approval(action);
    if (!"PENDING".equals(proposal.path("status").asText()))
      return Map.of("error", "ALREADY_DECIDED");
    if (request.expectedVersion() != proposal.path("version").asInt()
        || !request.proposalHash().equals(proposal.path("proposalHash").asText()))
      return Map.of("error", "PROPOSAL_VERSION_MISMATCH");
    var responsibility =
        jdbc.sql(
                "SELECT c.status::text case_status,w.status::text work_status FROM recall_proposals"
                    + " q JOIN cases c ON c.case_id=q.case_id JOIN work_items w ON"
                    + " w.work_item_id=q.work_item_id AND w.case_id=q.case_id WHERE"
                    + " q.governance_action_id=:id FOR UPDATE OF c,w")
            .param("id", action)
            .query()
            .singleRow();
    if (Set.of("CLOSED", "RESOLVED", "CANCELLED").contains(responsibility.get("case_status"))
        || Set.of("DONE", "CANCELLED").contains(responsibility.get("work_status")))
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    long lot = proposal.path("incidentLotId").asLong();
    var current = traces.trace(lot);
    if ("APPROVE".equals(request.decision())
        && (!current.path("complete").asBoolean()
            || !json.sha256(current).equals(request.proposalHash()))) {
      jdbc.sql("UPDATE governance_actions SET status='EXPIRED' WHERE governance_action_id=:id")
          .param("id", action)
          .update();
      resolveAttention(action, human, "변경된 원자료로 재조사 필요", "EXPIRED");
      jdbc.sql(
              "UPDATE waiting_conditions SET status='EXPIRED',resolved_at=CURRENT_TIMESTAMP WHERE"
                  + " condition_type='APPROVAL' AND condition_payload->>'approval_id'=:id AND"
                  + " status='ACTIVE'")
          .param("id", Long.toString(action))
          .update();
      followup(action, human, "원자료가 바뀌었다. 기존 barrier를 유지하고 새 조사·조치를 명시적으로 지시한다.");
      audit(
          action,
          human.userId(),
          "RECALL_EXPIRED",
          lot,
          Map.of("current", current, "proposal", proposal));
      return Map.of("error", "PROPOSAL_EXPIRED", "approvalId", action);
    }
    jdbc.sql(
            "INSERT INTO"
                + " governance_decisions(governance_action_id,decided_by,decision,reason,is_final)"
                + " VALUES (:id,:user,:decision::governance_decision_type,:reason,true)")
        .param("id", action)
        .param("user", human.userId())
        .param("decision", request.decision())
        .param("reason", request.reason())
        .update();
    String outcome =
        switch (request.decision()) {
          case "APPROVE" -> "APPROVED";
          case "BLOCK" -> "BLOCKED";
          default -> "CANCELLED";
        };
    jdbc.sql(
            "UPDATE governance_actions SET status=:status::governance_action_status WHERE"
                + " governance_action_id=:id")
        .param("id", action)
        .param("status", outcome)
        .update();
    if ("APPROVE".equals(request.decision())) {
      jdbc.sql(
              "INSERT INTO recalls(lot_id,recall_date,reason,governance_action_id,retain_until)"
                  + " SELECT"
                  + " s.lot_id,:today,:reason,:id,greatest(q.retain_until,CURRENT_TIMESTAMP+INTERVAL"
                  + " '2 years') FROM recall_scope_lots s JOIN recall_proposals q"
                  + " USING(recall_proposal_id) WHERE q.governance_action_id=:id")
          .param("today", LocalDate.now(clock))
          .param("reason", request.reason())
          .param("id", action)
          .update();
      jdbc.sql(
              "UPDATE production_lots SET status='RECALLED' WHERE production_lot_id IN(SELECT"
                  + " s.lot_id FROM recall_scope_lots s JOIN recall_proposals q"
                  + " USING(recall_proposal_id) WHERE governance_action_id=:id)")
          .param("id", action)
          .update();
    }
    resolveAttention(action, human, request.reason(), "ANSWERED");
    audit(
        action,
        human.userId(),
        "RECALL_DECIDED",
        lot,
        Map.of(
            "proposal",
            proposal,
            "decision",
            request,
            "after",
            approval(action),
            "productionLotsAfter",
            physicalStatuses(action)));
    var event =
        dispatcher.ingest(
            new CreateEventRequest(
                "RECALL_DECISION_RECORDED",
                "recall-decision-" + action,
                proposal.path("caseRef").asText(),
                null,
                Map.of("approvalId", Long.toString(action), "recallDecision", outcome)));
    jdbc.sql(
            "UPDATE waiting_conditions SET"
                + " status='SATISFIED',resolved_at=CURRENT_TIMESTAMP,resolved_by_event_id=:event"
                + " WHERE condition_type='APPROVAL' AND condition_payload->>'approval_id'=:id AND"
                + " status='ACTIVE'")
        .param("event", event.eventId())
        .param("id", Long.toString(action))
        .update();
    jdbc.sql(
            "UPDATE work_items SET status='DONE',resolved_at=CURRENT_TIMESTAMP WHERE"
                + " work_item_ref=:ref")
        .param("ref", proposal.path("workItemRef").asText())
        .update();
    dispatcher.ingest(
        new CreateEventRequest(
            "WORK_ITEM_STATUS_CHANGED",
            "recall-work-done-" + action,
            proposal.path("caseRef").asText(),
            proposal.path("workItemRef").asText(),
            Map.of("status", "DONE")));
    followup(
        action,
        human,
        "APPROVE".equals(request.decision())
            ? "식약처 보고 초안은 OFFLINE/PENDING이다. 인간 담당자가 즉시 검토·보고하고 회수 조치를 진행한다."
            : "리콜 반려·취소는 위험 소멸의 증거가 아니다. 안전 barrier를 유지하고 대체 조치를 명시한다.");
    return approval(action);
  }

  private JsonNode physicalStatuses(long action) {
    return tree(
        jdbc.sql(
                "SELECT"
                    + " coalesce(jsonb_agg(jsonb_build_object('lotId',p.production_lot_id,'status',p.status,'warehouseId',p.warehouse_id)"
                    + " ORDER BY p.production_lot_id),'[]')::text FROM production_lots p JOIN"
                    + " recall_scope_lots s ON s.lot_id=p.production_lot_id JOIN recall_proposals q"
                    + " USING(recall_proposal_id) WHERE q.governance_action_id=:action")
            .param("action", action)
            .query(String.class)
            .single());
  }

  private void resolveAttention(long id, HumanActor h, String reason, String status) {
    jdbc.sql(
            "UPDATE attention_requests SET"
                + " status=:status::attention_request_status,resolved_by_user_id=:user,answer_text=:reason,answer_scope='THIS_ACTION',resolved_at=CURRENT_TIMESTAMP,version=version+1"
                + " WHERE governance_action_id=:id AND status='OPEN'")
        .param("status", status)
        .param("user", h.userId())
        .param("reason", reason)
        .param("id", id)
        .update();
  }

  private void followup(long action, HumanActor h, String question) {
    long work =
        jdbc.sql(
                "INSERT INTO"
                    + " work_items(work_item_ref,case_id,title,status,assigned_user_id,metadata)"
                    + " SELECT :ref,case_id,'리콜 안전 조치와 규제"
                    + " 보고','WAITING',:user,jsonb_build_object('recallActionId',:id) FROM"
                    + " recall_proposals WHERE governance_action_id=:id RETURNING work_item_id")
            .param("ref", "WI-" + UUID.randomUUID().toString().substring(0, 12))
            .param("user", h.userId())
            .param("id", action)
            .query(Long.class)
            .single();
    jdbc.sql(
            "INSERT INTO"
                + " attention_requests(case_id,work_item_id,reason_type,title,question,consequence,suggested_scope)"
                + " SELECT case_id,:work,'JUDGMENT_REQUIRED','리콜 후속 조치 필요',:question,'영향 LOT의 안전"
                + " barrier와 기록 보관을 유지한다','THIS_ACTION' FROM recall_proposals WHERE"
                + " governance_action_id=:id")
        .param("work", work)
        .param("question", question)
        .param("id", action)
        .update();
  }

  private void audit(long action, Long user, String event, long lot, Object state) {
    jdbc.sql(
            "INSERT INTO"
                + " governance_audit_logs(governance_action_id,actor_id,event_type,resource_type,resource_id,after_state)"
                + " VALUES (:action,:user,:event,'PRODUCTION_LOT',:lot,:state::jsonb)")
        .param("action", action)
        .param("user", user)
        .param("event", event)
        .param("lot", lot)
        .param("state", json.write(state))
        .update();
  }

  private JsonNode tree(String value) {
    return json.readTree(value);
  }

  private static ResponseStatusException missing() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND);
  }
}
