package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.DomainError;
import com.mulino.domain.trade.purchase.PurchaseHash;
import com.sap.cds.ql.Select;
import java.net.URI;
import java.net.http.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

/** Real HTTP/CQN/PostgreSQL checks plus the inherited commercial approval regression suite. */
class PurchaseGoalPinPostgresTest extends PurchaseCommandPostgresTest {
  @BeforeEach void publishCurrentGoalForThisFixture() throws Exception {
    String previous = goal;
    goal = id();
    var slots = goalInputs();
    slots.put("deadlineInclusive", true);
    var g = workRow(goal);
    g.putAll(row("workId", work, "definitionVersionId", definition, "goalVersion", 2,
        "previousGoalId", previous, "quantityMode", "CUMULATIVE_EVENT", "targetQuantity", 100,
        "unit", "BOX", "endpoint", "ARRIVED", "scopeJson", json.writeValueAsString(slots.get("scope")),
        "slotsJson", json.writeValueAsString(slots), "dueAt", Timestamp.from(Instant.parse(slots.get("dueAt").toString())),
        "timezone", "Asia/Seoul", "evidencePolicyVersion", "fixture-v1"));
    insert("mulino_work_read_GoalReferences", g);
    jdbc.update("UPDATE mulino_work_read_Works SET currentGoalVersionId=? WHERE organizationId=? AND ID=?", goal, org, work);
  }

  Map<String,Object> proposalSlots() {
    return row("workId", work, "quantity", Map.of("value", "100", "unit", "BOX"),
        "price", Map.of("value", "1.00", "currency", "EUR"), "supplierId", supplier,
        "destinationId", place, "dueAt", "2026-10-31T00:00:00Z", "endpoint", "ARRIVED",
        "quantityMode", "CUMULATIVE_EVENT");
  }
  Map<String,Object> stored(Map<String,Object> p, int version) {
    return runtime.requestContext().run(c -> { return new LinkedHashMap<String,Object>(persistence.run(Select.from("mulino.trade.purchase.ProposalRevisions")
        .where(x -> x.get("organizationId").eq(org).and(x.get("proposalId").eq(p.get("proposalId")))
            .and(x.get("revision").eq(version)))).single()); });
  }
  String changeGoal() {
    var changed = goalInputs(); changed.put("deadlineInclusive", false);
    var result = run("reviseGoal", revision(work), row("workId", work, "goal", changed, "reason", "기한 끝점 정책을 명시적으로 바꾼다"));
    return result.get("goalVersionId").toString();
  }
  void held(Map<String,Object> result, String code) {
    assertEquals("HELD", result.get("outcome"), result.toString());
    assertEquals(code, ((Map<?,?>)result.get("error")).get("code"), result.toString());
  }
  @Test void serverPinsGoalWhenCallerOmitsOptionalSlotAndHashSurvivesDatabaseScale() {
    var p = purchase("proposePurchase", revision(work), proposalSlots(), id());
    assertEquals("APPLIED", p.get("outcome"), p.toString());
    var revision = stored(p, 1);
    assertEquals(goal, revision.get("goalVersionId"));
    assertEquals(goal, p.get("goalVersionId"));
    assertEquals(PurchaseHash.of(revision), p.get("proposalHash"));
    var different = new LinkedHashMap<>(revision); different.put("goalVersionId", id());
    assertNotEquals(PurchaseHash.of(revision), PurchaseHash.of(different));
    assertEquals("APPLIED", dispatch(p, approve(p, "APPROVE")).get("outcome"));
  }
  @Test void currentGoalRequiredWithoutInferringFromHistoricalRows() {
    jdbc.update("UPDATE mulino_work_read_Works SET currentGoalVersionId=NULL WHERE organizationId=? AND ID=?", org, work);
    held(purchase("proposePurchase", revision(work), proposalSlots(), id()), "POLICY_UNRESOLVED");
    assertEquals(0, count("mulino_trade_purchase_Proposals"));
    assertEquals(0, count("mulino_trade_purchase_ProposalRevisions"));
    assertEquals(0, count("mulino_trade_purchase_Approvals"));
    assertEquals(0, count("mulino_runtime_Outbox"));
    assertEquals(2, count("mulino_work_read_GoalReferences"));
  }
  @Test void explicitCurrentPinIsCompatibleAndStaleOrForeignPinsNeverCreateProposal() {
    var slots = proposalSlots(); slots.put("goalVersionId", goal);
    assertEquals("APPLIED", purchase("proposePurchase", revision(work), slots, id()).get("outcome"));
    String otherWork = id(), otherGoal = id();
    var w = workRow(otherWork); w.putAll(row("itemId", item, "definitionVersionId", definition,
        "kind", "PURCHASE", "status", "ACTIVE", "lifecycleMode", "COMMAND", "ownerId", actor, "supervisorId", actor));
    insert("mulino_work_read_Works", w);
    var g = workRow(otherGoal); g.putAll(row("workId", otherWork, "definitionVersionId", definition, "scopeJson", "{}"));
    insert("mulino_work_read_GoalReferences", g);
    var foreign = new PurchaseCommandPostgresTest(); foreign.jdbc = jdbc; foreign.seed(); login();
    String oldGoal = goal; changeGoal();
    for (String supplied : List.of(oldGoal, otherGoal, foreign.goal, id())) {
      slots.put("goalVersionId", supplied);
      var denied = purchase("proposePurchase", revision(work), slots, id());
      assertEquals("REJECTED", denied.get("outcome"), denied.toString());
      assertEquals("TYPE_INVALID", ((Map<?,?>)denied.get("error")).get("code"));
    }
    assertEquals(1, count("mulino_trade_purchase_Proposals"));
    assertEquals(1, count("mulino_trade_purchase_ProposalRevisions"));
    assertEquals(0, count("mulino_trade_purchase_Approvals"));
    assertEquals(0, count("mulino_runtime_Outbox"));
  }
  @Test void goalChangeRequiresRevisionAndNewApprovalWhilePriorHistoryRemainsImmutable() {
    var p = purchase("proposePurchase", revision(work), proposalSlots(), id());
    var a = approve(p, "APPROVE");
    var previous = stored(p, 1); String newGoal = changeGoal();
    assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT (g.slotsJson::jsonb->>'deadlineInclusive')::boolean FROM mulino_trade_purchase_ProposalRevisions p JOIN mulino_work_read_GoalReferences g ON g.organizationId=p.organizationId AND g.ID=p.goalVersionId WHERE p.organizationId=? AND p.proposalId=? AND p.revision=1", Boolean.class, org, p.get("proposalId")));
    assertEquals(Boolean.FALSE, jdbc.queryForObject("SELECT (slotsJson::jsonb->>'deadlineInclusive')::boolean FROM mulino_work_read_GoalReferences WHERE organizationId=? AND ID=?", Boolean.class, org, newGoal));
    held(approve(p, "APPROVE"), "APPROVAL_HASH_MISMATCH");
    held(dispatch(p, a), "APPROVAL_HASH_MISMATCH");
    var revised = purchase("revisePurchase", 1, row("proposalId", p.get("proposalId"),
        "changes", Map.of(), "reason", "현재 목표에 새 제안을 연결한다"), id());
    assertEquals("APPLIED", revised.get("outcome"), revised.toString());
    assertEquals(newGoal, stored(p, 2).get("goalVersionId"));
    assertEquals(previous, stored(p, 1));
    assertNotEquals(p.get("proposalHash"), revised.get("proposalHash"));
    held(dispatch(revised, a), "APPROVAL_NOT_CURRENT");
    assertEquals(1, count("mulino_trade_purchase_Approvals"));
    assertEquals("APPLIED", dispatch(revised, approve(revised, "APPROVE")).get("outcome"));
    assertEquals(2, count("mulino_trade_purchase_Approvals"));
    assertEquals(p.get("proposalHash"), jdbc.queryForObject("SELECT proposalHash FROM mulino_trade_purchase_Approvals WHERE ID=?", String.class, a.get("approvalId")));
    assertThrows(Exception.class, () -> jdbc.update("UPDATE mulino_trade_purchase_ProposalRevisions SET goalVersionId=? WHERE organizationId=? AND revision=1", newGoal, org));
    assertThrows(Exception.class, () -> jdbc.update("UPDATE mulino_work_read_GoalReferences SET slotsJson='{}' WHERE organizationId=? AND ID=?", org, goal));
  }
  @Test void committedReplayReturnsOriginalPinAfterGoalSwitchAndNewKeyPinsCurrentGoal() {
    var slots = proposalSlots(); String key = id();
    var p = purchase("proposePurchase", revision(work), slots, key);
    String newGoal = changeGoal();
    assertEquals(p, purchase("proposePurchase", 0, slots, key));
    var next = purchase("proposePurchase", revision(work), slots, id());
    assertEquals(newGoal, stored(next, 1).get("goalVersionId"));
    assertEquals(goal, stored(p, 1).get("goalVersionId"));
    assertEquals(2, count("mulino_trade_purchase_Proposals"));
  }
  @Test void legacyNullPinIsHeldWithoutBackfillAndCanOnlyAcquirePinInNewRevision() {
    var p = propose(); var a = approve(p, "APPROVE");
    jdbc.execute("ALTER TABLE mulino_trade_purchase_ProposalRevisions DISABLE TRIGGER purchase_proposalrevisions_immutable");
    try { jdbc.update("UPDATE mulino_trade_purchase_ProposalRevisions SET goalVersionId=NULL WHERE organizationId=?", org); }
    finally { jdbc.execute("ALTER TABLE mulino_trade_purchase_ProposalRevisions ENABLE TRIGGER purchase_proposalrevisions_immutable"); }
    held(approve(p, "APPROVE"), "POLICY_UNRESOLVED"); held(dispatch(p, a), "POLICY_UNRESOLVED");
    assertNull(stored(p, 1).get("goalVersionId"));
    var revised = purchase("revisePurchase", 1, row("proposalId", p.get("proposalId"), "changes", Map.of(), "reason", "새 제안에서 현재 목표를 확정한다"), id());
    assertEquals("APPLIED", revised.get("outcome"), revised.toString());
    assertEquals(goal, stored(p, 2).get("goalVersionId")); assertNull(stored(p, 1).get("goalVersionId"));
    assertEquals("APPLIED", dispatch(revised, approve(revised, "APPROVE")).get("outcome"));
  }
  @Test void rollbackRemovesPinRevisionInvalidationAuditAndCommandResultBeforeRetry() {
    var p = propose(); var a = approve(p, "APPROVE");
    String newGoal = changeGoal(), key = id();
    var slots = row("proposalId", p.get("proposalId"), "changes", Map.of("price", Map.of("value", "0", "currency", "EUR")), "reason", "롤백을 확인한다");
    int audits = count("mulino_commands_CommandAudits"), commandsBefore = count("mulino_commands_CommandRecords"), workRevision = revision(work);
    runtime.requestContext().run(c -> { return new org.springframework.transaction.support.TransactionTemplate(transactions).execute(tx -> {
      assertEquals("APPLIED", purchase("revisePurchase", 1, slots, key).get("outcome")); tx.setRollbackOnly(); return null;
    }); });
    assertEquals(1, count("mulino_trade_purchase_ProposalRevisions"));
    assertEquals(workRevision, revision(work));
    assertEquals(audits, count("mulino_commands_CommandAudits"));
    assertEquals(commandsBefore, count("mulino_commands_CommandRecords"));
    assertEquals(a.get("approvalId"), jdbc.queryForObject("SELECT currentApprovalId FROM mulino_trade_purchase_Proposals WHERE organizationId=?", String.class, org));
    var revised = purchase("revisePurchase", 1, slots, key); assertEquals("APPLIED", revised.get("outcome"), revised.toString());
    assertEquals(newGoal, stored(p, 2).get("goalVersionId"));
    assertEquals("APPLIED", dispatch(revised, approve(revised, "APPROVE")).get("outcome"));
  }
  @Test void externalDeliveryCannotSendOldGoalApprovalAfterSwitch() {
    var p = propose(); var order = dispatch(p, approve(p, "APPROVE")); changeGoal();
    var failure = assertThrows(DomainError.class, () -> deliveryTransaction(() -> purchaseDelivery.claimDelivery(deliveryContext(), "goal-worker", "dispatchPurchaseOrder",
        new com.mulino.application.runtime.ExternalDeliveryAdapter.Support(false, false), 3, Duration.ofSeconds(30))));
    assertEquals("APPROVAL_HASH_MISMATCH", failure.code());
    assertEquals(0, jdbc.queryForObject("SELECT deliveryAttempts FROM mulino_runtime_Outbox WHERE organizationId=?", Integer.class, org));
    assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM mulino_runtime_Outbox WHERE organizationId=?", String.class, org));
    assertEquals(1, count("mulino_trade_purchase_Orders"));
  }
  @Test void authenticatedHttpProposalPersistsExactPinAndRejectsStalePinWithZeroNewEffects() throws Exception {
    var slots = proposalSlots(); var p = httpPurchase(slots); assertEquals("APPLIED", p.get("outcome"), p.toString());
    assertEquals(goal, stored(p, 1).get("goalVersionId"));
    changeGoal(); slots.put("goalVersionId", goal);
    assertEquals("REJECTED", httpPurchase(slots).get("outcome"));
    assertEquals(1, count("mulino_trade_purchase_ProposalRevisions"));
    assertEquals(0, count("mulino_trade_purchase_Approvals")); assertEquals(0, count("mulino_runtime_Outbox"));
  }
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings={"0", "0.000000000001", "1.000000000000", "99999999999999999999999999.999999999999"})
  void supportedTypedAndFlatPricesRemainDispatchableWithGoalPin(String price) {
    for (boolean typed : List.of(true, false)) {
      var slots = proposalSlots();
      slots.put("quantity", typed ? Map.of("value", "100.000000000000", "unit", "BOX") : "100");
      slots.put("price", typed ? Map.of("value", price, "currency", "EUR") : price);
      if (!typed) { slots.put("unit", "BOX"); slots.put("currency", "EUR"); }
      var p = purchase("proposePurchase", revision(work), slots, id());
      assertEquals("APPLIED", p.get("outcome"), p.toString());
      assertEquals(PurchaseHash.of(stored(p, 1)), p.get("proposalHash"));
      assertEquals("APPLIED", dispatch(p, approve(p, "APPROVE")).get("outcome"));
    }
    assertEquals(2, count("mulino_trade_purchase_Orders"));
    assertEquals(2, count("mulino_runtime_Outbox"));
  }
  @Test void unchangedGoalAndEmptyChangesDoNotCreateUnnecessaryRevision() {
    var p = propose();
    var denied = purchase("revisePurchase", 1, row("proposalId", p.get("proposalId"), "changes", Map.of(), "reason", "변경이 없다"), id());
    assertEquals("REJECTED", denied.get("outcome"));
    assertEquals(1, count("mulino_trade_purchase_ProposalRevisions"));
  }
  @Test void goalFromOtherWorkInLegacyRevisionCannotBeApproved() {
    var p = propose();
    String otherWork = id(), otherGoal = id();
    var w = workRow(otherWork); w.putAll(row("itemId", item, "definitionVersionId", definition,
        "kind", "PURCHASE", "status", "ACTIVE", "lifecycleMode", "COMMAND", "ownerId", actor, "supervisorId", actor));
    insert("mulino_work_read_Works", w);
    var g = workRow(otherGoal); g.putAll(row("workId", otherWork, "definitionVersionId", definition, "scopeJson", "{}"));
    insert("mulino_work_read_GoalReferences", g);
    jdbc.execute("ALTER TABLE mulino_trade_purchase_ProposalRevisions DISABLE TRIGGER purchase_proposalrevisions_immutable");
    try { jdbc.update("UPDATE mulino_trade_purchase_ProposalRevisions SET goalVersionId=? WHERE organizationId=?", otherGoal, org); }
    finally { jdbc.execute("ALTER TABLE mulino_trade_purchase_ProposalRevisions ENABLE TRIGGER purchase_proposalrevisions_immutable"); }
    var denied = approve(p, "APPROVE");
    assertEquals("REJECTED", denied.get("outcome"));
    assertEquals("FORBIDDEN", ((Map<?,?>)denied.get("error")).get("code"));
    assertEquals(0, count("mulino_trade_purchase_Approvals"));
    assertEquals(0, count("mulino_runtime_Outbox"));
  }
  @Test void historicalHashWithoutGoalCoverageRequiresFreshRevisionRatherThanReusingApproval() {
    var p = propose(); var old = stored(p, 1);
    var prePinHashInput = new TreeMap<String,Object>();
    for (String k : List.of("proposalId", "revision", "workId", "itemId", "supplierId", "destinationId", "quantity", "unit", "price", "currency", "dueAt", "endpoint", "quantityMode")) {
      prePinHashInput.put(k, old.get(k).toString());
    }
    String legacyHash = com.mulino.application.core.CommandRequests.hash(prePinHashInput);
    jdbc.execute("ALTER TABLE mulino_trade_purchase_ProposalRevisions DISABLE TRIGGER purchase_proposalrevisions_immutable");
    try { jdbc.update("UPDATE mulino_trade_purchase_ProposalRevisions SET proposalHash=? WHERE organizationId=?", legacyHash, org); }
    finally { jdbc.execute("ALTER TABLE mulino_trade_purchase_ProposalRevisions ENABLE TRIGGER purchase_proposalrevisions_immutable"); }
    p = new LinkedHashMap<>(p); p.put("proposalHash", legacyHash);
    held(approve(p, "APPROVE"), "APPROVAL_HASH_MISMATCH");
    var revised = purchase("revisePurchase", 1, row("proposalId", p.get("proposalId"), "changes", Map.of(), "reason", "현재 목표와 금액을 새 hash에 결합한다"), id());
    assertEquals("APPLIED", revised.get("outcome"), revised.toString());
    assertEquals(legacyHash, stored(p, 1).get("proposalHash"));
    assertEquals(goal, stored(p, 2).get("goalVersionId"));
    assertEquals("APPLIED", dispatch(revised, approve(revised, "APPROVE")).get("outcome"));
  }
  @Test void existingGoalColumnAndOrganizationForeignKeyNeedNoMigration() {
    assertEquals("YES", jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_name='mulino_trade_purchase_proposalrevisions' AND column_name='goalversionid'", String.class));
    assertTrue(jdbc.queryForObject("SELECT count(*) FROM pg_constraint WHERE conrelid='mulino_trade_purchase_proposalrevisions'::regclass AND contype='f' AND pg_get_constraintdef(oid) ILIKE '%(organizationid, goalversionid)%REFERENCES%goalreferences%'", Integer.class) > 0);
    var p = propose(); var foreign = new PurchaseCommandPostgresTest(); foreign.jdbc = jdbc; foreign.seed(); login();
    jdbc.execute("ALTER TABLE mulino_trade_purchase_ProposalRevisions DISABLE TRIGGER purchase_proposalrevisions_immutable");
    try { assertThrows(Exception.class, () -> jdbc.update("UPDATE mulino_trade_purchase_ProposalRevisions SET goalVersionId=? WHERE organizationId=?", foreign.goal, org)); }
    finally { jdbc.execute("ALTER TABLE mulino_trade_purchase_ProposalRevisions ENABLE TRIGGER purchase_proposalrevisions_immutable"); }
    assertEquals(goal, stored(p, 1).get("goalVersionId"));
    System.out.println("PURCHASE_GOAL_PIN_POSTGRES=" + jdbc.queryForObject("SHOW server_version", String.class));
  }
  Map<String,Object> httpPurchase(Map<String,Object> slots) throws Exception {
    var encoder = Base64.getUrlEncoder().withoutPadding();
    String header = encoder.encodeToString("{\"alg\":\"RS256\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String claims = encoder.encodeToString(json.writeValueAsBytes(row("iss", "https://mulino.local.invalid", "aud", "mulino-platform", "sub", "writer-a", "organizationId", org, "stableRequestOwner", "payload-owner-is-ignored",
        "iat", Instant.now().minusSeconds(10).getEpochSecond(), "nbf", Instant.now().minusSeconds(10).getEpochSecond(), "exp", Instant.now().plusSeconds(300).getEpochSecond())));
    var signature = java.security.Signature.getInstance("SHA256withRSA"); signature.initSign(KEY.getPrivate()); signature.update((header + "." + claims).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String token = header + "." + claims + "." + encoder.encodeToString(signature.sign());
    var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/ontology/commands/proposePurchase"))
        .timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(row("intentKind", "COMMAND", "capabilityId", "proposePurchase", "definitionVersion", "1.0.0",
            "expectedRevision", revision(work), "slots", slots, "provenance", Map.of(), "subjectRefs", List.of(), "commandIdempotencyKey", id())))).build();
    var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode(), response.body());
    return json.readValue(response.body(), Map.class);
  }
}
