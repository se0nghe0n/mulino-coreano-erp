package com.mulinocoreano.backend.recall;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mulinocoreano.backend.planning.ReplenishmentDemoFixture;
import com.mulinocoreano.backend.procurement.PurchaseDecisionRequest;
import com.mulinocoreano.backend.security.*;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.*;

/** Goals 1,2,4,6: complete contamination scope, ADMIN gate, durable continuation and retention. */
@SpringBootTest(
    properties = {
      "mulino.local-auth.human-secret=test-human-gateway",
      "spring.flyway.schemas=recall_workflow_it",
      "spring.flyway.clean-disabled=false",
      "spring.datasource.hikari.schema=recall_workflow_it",
      "spring.flyway.init-sqls=CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public",
      "spring.main.allow-bean-definition-overriding=true"
    })
@ActiveProfiles("local")
@AutoConfigureMockMvc
class RecallWorkflowIntegrationTest {
  @Autowired JdbcClient jdbc;
  @Autowired Flyway flyway;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired RecallService service;
  @Autowired com.mulinocoreano.backend.planning.PlanningSnapshotRepository snapshots;
  @Autowired LocalActorDirectory actors;
  @Autowired com.mulinocoreano.backend.execution.RunExecutionService execution;
  long lot;
  String caseRef;

  @TestConfiguration
  static class Time {
    @Bean("planningClock")
    @Primary
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-09-05T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    }
  }

  @BeforeEach
  void fixture() throws Exception {
    flyway.clean();
    flyway.migrate();
    ReplenishmentDemoFixture.load(jdbc);
    RecallDemoFixture.load(jdbc);
    lot = id("SELECT production_lot_id FROM production_lots WHERE lot_number='RC-FINAL'");
    caseRef =
        mapper
            .readTree(
                mvc.perform(
                        post("/api/v1/cases")
                            .header("X-Mulino-Local-Human", "test-human-gateway")
                            .header("X-Mulino-Local-Role", "OPERATOR")
                            .header("Idempotency-Key", "rc-case")
                            .contentType("application/json")
                            .content("{\"objective\":\"완제품 리콜 조사\"}"))
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

  JsonNode propose() {
    return service.propose(
        lot,
        new RecallService.RecallRequest(caseRef, "제품 컴플레인"),
        actors.humanForRole("OPERATOR"),
        null,
        "proposal");
  }

  PurchaseDecisionRequest decision(JsonNode p, String choice) {
    return new PurchaseDecisionRequest(
        choice, p.path("version").asInt(), p.path("proposalHash").asText(), "전수 영향 범위를 확인한 인간 판단");
  }

  @Test
  void multilevelTraceIncludesAllRawSourcesAndEveryShipmentWithoutDuplicatingCustomers() {
    var t = service.trace(lot);
    assertThat(t.path("problems").toString()).isEqualTo("[]");
    assertThat(t.path("complete").asBoolean()).isTrue();
    assertThat(t.path("rawLots").size()).isEqualTo(3);
    var rawEvidence = new java.util.ArrayList<JsonNode>();
    t.path("rawLots").forEach(rawEvidence::add);
    assertThat(rawEvidence.stream().filter(r -> r.path("incidentRoot").asBoolean()).count())
        .isEqualTo(2);
    assertThat(
            rawEvidence.stream().filter(r -> r.path("lot_number").asText().equals("DEMO-RM-PACK")))
        .allSatisfy(r -> assertThat(r.path("incidentRoot").asBoolean()).isFalse());
    var lotEvidence = new java.util.ArrayList<JsonNode>();
    t.path("productionLots").forEach(lotEvidence::add);
    assertThat(lotEvidence)
        .noneSatisfy(r -> assertThat(r.path("lot_number").asText()).isEqualTo("RC-UNRELATED-PACK"));
    assertThat(t.path("suppliers").size()).isEqualTo(1);
    assertThat(t.path("customers").size()).isEqualTo(2);
    assertThat(t.path("shipments").size()).isEqualTo(115);
    assertThat(t.path("productionLots").size()).isEqualTo(10);
    var balance = t.path("lotBalances").findValues("remaining");
    assertThat(balance)
        .anySatisfy(
            v -> assertThat(new java.math.BigDecimal(v.asText())).isEqualByComparingTo("2.25"));
  }

  @Test
  void adminApprovalAppliesWholeScopeAndImmediateOfflineReportReplaysOnce() {
    var p = propose();
    assertThat(id("SELECT count(*) FROM recalls")).isZero();
    assertThat(id("SELECT count(*) FROM production_lots WHERE status='RECALLED'")).isZero();
    assertThat(
            jdbc.sql("SELECT recall_lot_barrier(:lot)")
                .param("lot", lot)
                .query(Boolean.class)
                .single())
        .isTrue();
    var approved =
        service.decide(
            p.path("approvalId").asLong(),
            decision(p, "APPROVE"),
            actors.humanForRole("ADMIN"),
            "approve");
    assertThat(approved.path("status").asText()).isEqualTo("APPROVED");
    assertThat(id("SELECT count(*) FROM recalls")).isEqualTo(10);
    assertThat(id("SELECT count(*) FROM production_lots WHERE status='RECALLED'")).isEqualTo(10);
    assertThat(
            id(
                "SELECT count(*) FROM production_lots WHERE lot_number='RC-UNRELATED-PACK' AND"
                    + " status='ACTIVE'"))
        .isEqualTo(1);
    assertThat(
            id(
                "SELECT count(*) FROM recall_reports WHERE due_at<=created_at AND status='PENDING'"
                    + " AND transport='OFFLINE' AND submitted_at IS NULL AND confirmation IS NULL"
                    + " AND retain_until>=created_at+INTERVAL '2 years'"))
        .isEqualTo(1);
    assertThat(
            service.decide(
                p.path("approvalId").asLong(),
                decision(p, "APPROVE"),
                actors.humanForRole("ADMIN"),
                "approve"))
        .isEqualTo(approved);
    assertThat(id("SELECT count(*) FROM governance_decisions WHERE is_final")).isEqualTo(1);
    assertThat(id("SELECT count(*) FROM governance_audit_logs WHERE event_type='RECALL_DECIDED'"))
        .isEqualTo(1);
    assertThat(
            id(
                "SELECT count(*) FROM governance_audit_logs a CROSS JOIN LATERAL"
                    + " jsonb_array_elements(a.after_state->'productionLotsAfter') p WHERE"
                    + " a.event_type='RECALL_DECIDED' AND p->>'status'='RECALLED'"))
        .isEqualTo(10);
    assertThat(
            id(
                "SELECT count(*) FROM attention_requests WHERE status='OPEN' AND"
                    + " governance_action_id IS NULL"))
        .isGreaterThan(0);
  }

  @Test
  void wrongRolesInactiveAdminAndAgentCannotDecide() {
    var p = propose();
    for (String role : List.of("OPERATOR", "MANAGER", "QC", "VIEWER"))
      assertThatThrownBy(
              () ->
                  service.decide(
                      p.path("approvalId").asLong(),
                      decision(p, "APPROVE"),
                      actors.humanForRole(role),
                      "wrong-" + role))
          .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    assertThatThrownBy(
            () ->
                service.decide(
                    p.path("approvalId").asLong(),
                    decision(p, "APPROVE"),
                    new AgentActor("r", caseRef, "w", "QC"),
                    "agent"))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    var admin = actors.humanForRole("ADMIN");
    jdbc.sql("UPDATE users SET is_active=false WHERE user_id=:id")
        .param("id", admin.userId())
        .update();
    assertThatThrownBy(
            () ->
                service.decide(
                    p.path("approvalId").asLong(), decision(p, "APPROVE"), admin, "inactive"))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    assertThat(id("SELECT count(*) FROM recalls")).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"BLOCK", "CANCEL"})
  void rejectionAndCancellationKeepPhysicalStateAndSafetyBarrier(String choice) {
    var p = propose();
    var result =
        service.decide(
            p.path("approvalId").asLong(),
            decision(p, choice),
            actors.humanForRole("ADMIN"),
            "decision-" + choice);
    assertThat(result.path("status").asText())
        .isEqualTo(choice.equals("BLOCK") ? "BLOCKED" : "CANCELLED");
    assertThat(id("SELECT count(*) FROM recalls")).isZero();
    assertThat(
            jdbc.sql("SELECT recall_lot_barrier(:lot)")
                .param("lot", lot)
                .query(Boolean.class)
                .single())
        .isTrue();
  }

  @Test
  void changedCustomerEvidenceExpiresAndPreservesResponsibility() {
    var p = propose();
    jdbc.sql("UPDATE customers SET address='changed' WHERE name='RC 두 번째 고객'").update();
    var result =
        service.decide(
            p.path("approvalId").asLong(),
            decision(p, "APPROVE"),
            actors.humanForRole("ADMIN"),
            "changed");
    assertThat(result.path("error").asText()).isEqualTo("PROPOSAL_EXPIRED");
    assertThat(service.approval(p.path("approvalId").asLong()).path("status").asText())
        .isEqualTo("EXPIRED");
    assertThat(id("SELECT count(*) FROM recalls")).isZero();
    assertThat(
            id(
                "SELECT count(*) FROM attention_requests WHERE status='OPEN' AND"
                    + " governance_action_id IS NULL"))
        .isGreaterThan(0);
  }

  @Test
  void auditFailureRollsBackAllErpWrites() {
    var p = propose();
    jdbc.sql(
            "CREATE FUNCTION fail_recall_audit() RETURNS trigger AS $$ BEGIN IF"
                + " NEW.event_type='RECALL_DECIDED' THEN RAISE EXCEPTION 'fixture audit failure';"
                + " END IF; RETURN NEW; END; $$ LANGUAGE plpgsql")
        .update();
    jdbc.sql(
            "CREATE TRIGGER fail_recall_audit BEFORE INSERT ON governance_audit_logs FOR EACH ROW"
                + " EXECUTE FUNCTION fail_recall_audit()")
        .update();
    assertThatThrownBy(
            () ->
                service.decide(
                    p.path("approvalId").asLong(),
                    decision(p, "APPROVE"),
                    actors.humanForRole("ADMIN"),
                    "fail"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    assertThat(id("SELECT count(*) FROM recalls")).isZero();
    assertThat(id("SELECT count(*) FROM production_lots WHERE status='RECALLED'")).isZero();
    assertThat(id("SELECT count(*) FROM governance_decisions")).isZero();
    assertThat(service.approval(p.path("approvalId").asLong()).path("status").asText())
        .isEqualTo("PENDING");
  }

  @Test
  void raceAndWrongVersionHaveExactlyOneFinalOutcome() throws Exception {
    var p = propose();
    long action = p.path("approvalId").asLong();
    var admin = actors.humanForRole("ADMIN");
    service.decide(
        action,
        new PurchaseDecisionRequest("APPROVE", 2, p.path("proposalHash").asText(), "stale"),
        admin,
        "wrong");
    assertThat(id("SELECT count(*) FROM recalls")).isZero();
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(3)) {
      var futures =
          List.of(
              pool.submit(() -> service.decide(action, decision(p, "APPROVE"), admin, "race1")),
              pool.submit(() -> service.decide(action, decision(p, "BLOCK"), admin, "race2")),
              pool.submit(() -> service.decide(action, decision(p, "CANCEL"), admin, "race3")));
      for (var f : futures) f.get(20, java.util.concurrent.TimeUnit.SECONDS);
    }
    assertThat(id("SELECT count(*) FROM governance_decisions WHERE is_final")).isEqualTo(1);
    assertThat(id("SELECT count(*) FROM governance_audit_logs WHERE event_type='RECALL_DECIDED'"))
        .isEqualTo(1);
  }

  @Test
  void retainedDraftScopeAndRecordsRejectUpdateDeleteTruncate() {
    var p = propose();
    for (String table : List.of("recall_proposals", "recall_scope_lots", "recall_reports")) {
      assertThatThrownBy(() -> jdbc.sql("DELETE FROM " + table).update())
          .isInstanceOf(org.springframework.dao.DataAccessException.class);
      assertThatThrownBy(() -> jdbc.sql("TRUNCATE " + table + " CASCADE").update())
          .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    assertThatThrownBy(() -> jdbc.sql("UPDATE recall_reports SET manifest='{}'").update())
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    service.decide(
        p.path("approvalId").asLong(),
        decision(p, "APPROVE"),
        actors.humanForRole("ADMIN"),
        "approve");
    assertThatThrownBy(() -> jdbc.sql("DELETE FROM recalls").update())
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
  }

  @Test
  void hostKeyIsRequiredForPublicTraceAndWrongRoleCannotApprove() throws Exception {
    mvc.perform(get("/api/v1/recall/lots/" + lot + "/trace").header("X-Mulino-Local-Role", "ADMIN"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/v1/recall/lots/" + lot + "/trace")
                .header("X-Mulino-Local-Human", "bad")
                .header("X-Mulino-Local-Role", "ADMIN"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/v1/recall/lots/" + lot + "/trace")
                .header("X-Mulino-Local-Human", "test-human-gateway")
                .header("X-Mulino-Local-Role", "VIEWER"))
        .andExpect(status().isOk());
    var p = propose();
    mvc.perform(
            post("/api/v1/recall/approvals/" + p.path("approvalId").asLong() + "/decision")
                .header("X-Mulino-Local-Human", "test-human-gateway")
                .header("X-Mulino-Local-Role", "QC")
                .header("Idempotency-Key", "wrong-http")
                .contentType("application/json")
                .content(mapper.writeValueAsString(decision(p, "APPROVE"))))
        .andExpect(status().isForbidden());
    assertThat(id("SELECT count(*) FROM recalls")).isZero();
  }

  @Test
  void incompleteAllocationAndMissingLocationFailClosed() {
    jdbc.sql(
            "UPDATE outbound SET quantity=quantity+0.1 WHERE outbound_id=(SELECT max(outbound_id)"
                + " FROM outbound_lots WHERE lot_id=:lot)")
        .param("lot", lot)
        .update();
    assertThat(service.trace(lot).path("complete").asBoolean()).isFalse();
    assertThatThrownBy(() -> propose())
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    jdbc.sql("UPDATE production_lots SET warehouse_id=NULL WHERE production_lot_id=:lot")
        .param("lot", lot)
        .update();
    assertThat(service.trace(lot).path("complete").asBoolean()).isFalse();
  }

  @Test
  void explicitHumanRepairCreatesNextVersionAndRetainsExpiredEvidence() {
    var p = propose();
    jdbc.sql("UPDATE customers SET address='repair-needed' WHERE name='RC 두 번째 고객'").update();
    service.decide(
        p.path("approvalId").asLong(),
        decision(p, "APPROVE"),
        actors.humanForRole("ADMIN"),
        "expire");
    var next =
        service.propose(
            lot,
            new RecallService.RecallRequest(caseRef, "새 자료를 확인한 인간 재조사"),
            actors.humanForRole("OPERATOR"),
            null,
            "explicit-repair");
    assertThat(next.path("version").asInt()).isEqualTo(2);
    assertThat(next.path("proposalHash").asText()).isNotEqualTo(p.path("proposalHash").asText());
    service.decide(
        next.path("approvalId").asLong(),
        decision(next, "APPROVE"),
        actors.humanForRole("ADMIN"),
        "approve-repaired");
    assertThat(id("SELECT count(*) FROM recalls")).isEqualTo(10);
    assertThat(id("SELECT count(*) FROM recall_reports")).isEqualTo(2);
    assertThat(service.approval(p.path("approvalId").asLong()).path("status").asText())
        .isEqualTo("EXPIRED");
  }

  @Test
  void expiredProductStillHasTwoCalendarYearsFromActualRecordCreation() {
    jdbc.sql("UPDATE production_lots SET expiry_date='2026-09-04'").update();
    var p = propose();
    assertThat(
            id(
                "SELECT count(*) FROM recall_proposals WHERE retain_until>=created_at+INTERVAL '2"
                    + " years'"))
        .isEqualTo(1);
    assertThat(
            id(
                "SELECT count(*) FROM recall_reports WHERE retain_until>=created_at+INTERVAL '2"
                    + " years'"))
        .isEqualTo(1);
    service.decide(
        p.path("approvalId").asLong(),
        decision(p, "APPROVE"),
        actors.humanForRole("ADMIN"),
        "approve-expired");
    assertThat(id("SELECT count(*) FROM recalls WHERE retain_until>=created_at+INTERVAL '2 years'"))
        .isEqualTo(10);
  }

  @Test
  void assignedAgentCannotReadOtherIncidentAndOnlyNewRunCanReinvestigate() {
    var assigned =
        service.assign(
            lot,
            new RecallService.RecallRequest(caseRef, "QC 조사"),
            actors.humanForRole("OPERATOR"),
            "assign");
    var claim = execution.claim("recall-worker").orElseThrow();
    var actor =
        new AgentActor(claim.runRef(), claim.caseRef(), claim.workItemRef(), claim.agentKey());
    assertThat(service.agentTrace(lot, claim.capabilityToken(), actor).path("complete").asBoolean())
        .isTrue();
    long other =
        id("SELECT production_lot_id FROM production_lots WHERE lot_number='DEMO-AMR-HISTORY'");
    assertThatThrownBy(() -> service.agentTrace(other, claim.capabilityToken(), actor))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                service.propose(
                    other,
                    new RecallService.RecallRequest(caseRef, "out of scope"),
                    actor,
                    claim.capabilityToken(),
                    "bad-scope"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    var p =
        service.propose(
            lot,
            new RecallService.RecallRequest(caseRef, "QC 사고 근거"),
            actor,
            claim.capabilityToken(),
            "agent-proposal");
    assertThat(id("SELECT count(*) FROM runs WHERE status='COMPLETED' AND outcome='WAITING'"))
        .isEqualTo(1);
    assertThat(
            id(
                "SELECT count(*) FROM waiting_conditions WHERE status='ACTIVE' AND"
                    + " condition_type='APPROVAL'"))
        .isEqualTo(1);
  }

  @Test
  void pendingRecallBlocksProductionAllocationAndPlanningWithoutPhysicalMutation() {

    long flour =
        id("SELECT raw_material_lot_id FROM raw_material_lots WHERE lot_number='DEMO-RM-FLOUR'");
    long doughRecord =
        id(
            "SELECT production_record_id FROM production_records r JOIN production_lots p ON"
                + " p.production_lot_id=r.lot_id WHERE lot_number='DEMO-DOUGH-CURRENT'");
    long finalRecord =
        id(
            "SELECT production_record_id FROM production_records r JOIN production_lots p ON"
                + " p.production_lot_id=r.lot_id WHERE lot_number='RC-FINAL'");
    var mutations =
        List.of(
            "UPDATE production_ingredients SET quantity_used=quantity_used WHERE"
                + " production_record_id="
                + doughRecord
                + " AND raw_material_lot_id="
                + flour,
            "UPDATE production_product_inputs SET quantity_used=quantity_used WHERE"
                + " production_record_id="
                + finalRecord,
            "UPDATE outbound_lots SET lot_quantity=lot_quantity WHERE lot_id=" + lot);
    for (String mutation : mutations) assertThat(jdbc.sql(mutation).update()).isGreaterThan(0);
    var rawBefore =
        jdbc.sql("SELECT remaining_quantity FROM raw_material_lots WHERE raw_material_lot_id=:raw")
            .param("raw", flour)
            .query(java.math.BigDecimal.class)
            .single();
    var allocationsBefore =
        jdbc.sql("SELECT sum(lot_quantity) FROM outbound_lots WHERE lot_id=:lot")
            .param("lot", lot)
            .query(java.math.BigDecimal.class)
            .single();
    assertThat(allocationsBefore).isEqualByComparingTo("0.75");
    propose();
    for (String mutation : mutations)
      assertThatThrownBy(() -> jdbc.sql(mutation).update())
          .isInstanceOf(org.springframework.dao.DataAccessException.class);
    assertThat(
            jdbc.sql(
                    "SELECT remaining_quantity FROM raw_material_lots WHERE"
                        + " raw_material_lot_id=:raw")
                .param("raw", flour)
                .query(java.math.BigDecimal.class)
                .single())
        .isEqualByComparingTo(rawBefore);
    assertThat(
            jdbc.sql("SELECT sum(lot_quantity) FROM outbound_lots WHERE lot_id=:lot")
                .param("lot", lot)
                .query(java.math.BigDecimal.class)
                .single())
        .isEqualByComparingTo(allocationsBefore);
    assertThat(id("SELECT count(*) FROM production_lots WHERE status='RECALLED'")).isZero();
    long warehouse = id("SELECT warehouse_id FROM warehouses WHERE plant_id='DEMO-KR-01'");
    var products =
        jdbc.sql(
                "SELECT product_id FROM products WHERE sku IN('DEMO-AMR','DEMO-BSC') ORDER BY"
                    + " product_id")
            .query(Long.class)
            .list();
    var snapshot = snapshots.load(warehouse, products, LocalDate.of(2026, 9, 5), 30);

    var affectedIds = new java.util.HashSet<String>();
    for (var row : service.trace(lot).path("productionLots"))
      if (row.path("affected").asBoolean())
        affectedIds.add("production_lots:" + row.path("id").asLong());
    var blocked =
        snapshot.supply().stream()
            .filter(l -> affectedIds.contains(l.sourceRef()) && l.quantity().signum() > 0)
            .toList();
    assertThat(blocked).isNotEmpty();
    assertThat(blocked).allSatisfy(l -> assertThat(l.exclusionReason()).isNotNull());
    long unrelated =
        id("SELECT production_lot_id FROM production_lots WHERE lot_number='RC-UNRELATED-PACK'");
    assertThat(
            snapshot.supply().stream()
                .filter(l -> l.sourceRef().equals("production_lots:" + unrelated)))
        .hasSize(1)
        .allSatisfy(l -> assertThat(l.exclusionReason()).isNull());
  }

  @Test
  void databaseRejectsUnapprovedRecalledStatusAndNewCyclesButSurfacesLegacyCycle() {
    assertThatThrownBy(
            () ->
                jdbc.sql(
                        "UPDATE production_lots SET status='RECALLED' WHERE production_lot_id=:lot")
                    .param("lot", lot)
                    .update())
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    long record =
        id(
            "SELECT production_record_id FROM production_records r JOIN production_lots p ON"
                + " p.production_lot_id=r.lot_id WHERE lot_number='DEMO-DOUGH-CURRENT'");
    String cycle =
        "INSERT INTO"
            + " production_product_inputs(production_record_id,source_production_lot_id,quantity_used)"
            + " VALUES (:record,:lot,0.125)";
    assertThatThrownBy(() -> jdbc.sql(cycle).param("record", record).param("lot", lot).update())
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    jdbc.sql("ALTER TABLE production_product_inputs DISABLE TRIGGER trg_recall_product_graph")
        .update();
    jdbc.sql(cycle).param("record", record).param("lot", lot).update();
    jdbc.sql("ALTER TABLE production_product_inputs ENABLE TRIGGER trg_recall_product_graph")
        .update();
    assertThat(service.trace(lot).path("complete").asBoolean()).isFalse();
    assertThatThrownBy(() -> propose())
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    long warehouse = id("SELECT warehouse_id FROM warehouses WHERE plant_id='DEMO-KR-01'");
    var products =
        jdbc.sql(
                "SELECT product_id FROM products WHERE sku IN('DEMO-AMR','DEMO-BSC') ORDER BY"
                    + " product_id")
            .query(Long.class)
            .list();
    assertThatThrownBy(() -> snapshots.load(warehouse, products, LocalDate.of(2026, 9, 5), 30))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
