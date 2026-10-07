package org.mulino.verification.cases.work;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mulino.verification.AssertionEngine;
import org.mulino.verification.ContractValidator;
import org.mulino.verification.Json;
import org.mulino.verification.PreparationValidator;
import static org.junit.jupiter.api.Assertions.*;

/** Fixed observations exercise authored T10 assertions; this is not a product adapter. */
class RecipientAcceptanceAssertionTest {
    private final AssertionEngine engine = new AssertionEngine();
    private final JsonNode aliases = Json.parse("""
        {"ORG":"org-id","P":"item-id","SOURCE":"source-id","TARGET":"target-id",
         "ROOT":"root-id","ASSIGN":"assignment-id","A":"human-a","B":"human-b"}
        """);

    private JsonNode subcase(String id) throws Exception {
        var file = Json.read(Path.of(System.getProperty("repo.root"), "verification/cases/T10/case.json"));
        for (JsonNode sub : file.path("subcases")) if (sub.path("id").asText().equals(id)) return sub;
        throw new AssertionError("Missing authored T10 subcase " + id);
    }
    private JsonNode assertion(String subcase, String id) throws Exception {
        for (JsonNode a : subcase(subcase).path("assertions")) if (a.path("id").asText().equals(id)) return a;
        throw new AssertionError("Missing authored assertion " + id);
    }
    private ObjectNode captured(String data) {
        var node = Json.object();
        node.put("driverStatus", "EXECUTED");
        node.set("provenance", Json.parse("{\"scopeComplete\":true,\"source\":\"CANNED_CONTRACT_SELFTEST\"}"));
        node.set("data", Json.parse(data));
        return node;
    }
    private Map<String, JsonNode> denied(String id) {
        var before = captured("""
            {"rawRows":{"works":[{"id":"source-id","ownerId":"human-a","state":"ACTIVE"},
             {"id":"target-id","ownerId":"human-b","state":"WAITING"}],
             "assignments":[{"id":"assignment-id","obligationId":"root-id","workId":"source-id",
             "ownerId":"human-a","status":"OPEN"}],
             "obligations":[{"id":"root-id","status":"OPEN","quantity":"10","unit":"BOX"}],
             "workLinks":[],"outbox":[],"audit":[]}}
            """);
        ObjectNode after = before.deepCopy();
        ObjectNode audit = Json.object();
        audit.put("commandKey", "T10-" + id + "-impersonated-acceptance");
        audit.put("actorId", "human-a"); audit.put("action", "transferObligation"); audit.put("outcome", "REJECTED");
        ((ArrayNode) after.at("/data/rawRows/audit")).add(audit);
        var response = captured("{}"); response.set("response", Json.parse("{\"outcome\":\"REJECTED\"}"));
        return new HashMap<>(Map.of("before-db", before, "after-impersonation-db", after, "impersonated-acceptance", response));
    }
    private Map<String, JsonNode> accepted(String id) throws Exception {
        String command = id.equals("partial-commit") ? "partial-transfer" : "transfer";
        var after = captured("""
            {"rawRows":{"audit":[{"actorId":"human-b","action":"transferObligation",
             "outcome":"APPLIED","transactionId":"tx-accepted"}],
             "assignments":[{"workId":"target-id","ownerId":"human-b","status":"OPEN",
             "createdTransactionId":"tx-accepted"}]}}
            """);
        ((ObjectNode) after.at("/data/rawRows/audit/0")).put("commandKey", "T10-" + id + "-" + command);
        return new HashMap<>(Map.of("after-db", after));
    }

    @Test void authoredTransferContractsAndKoreanFeaturesPrepareTogether() throws Exception {
        Path root = Path.of(System.getProperty("repo.root"));
        var validator = new ContractValidator(root);
        var preparation = new PreparationValidator(root);
        for (String caseId : new String[]{"T10", "T11"}) {
            Path path = root.resolve("verification/cases/" + caseId + "/case.json");
            JsonNode contract = validator.caseFile(path);
            assertEquals(java.util.List.of(), preparation.feature(path, contract));
        }
    }

    @Test void sameSlotsAreDeniedForAThenAcceptedByAuthenticatedB() throws Exception {
        for (String id : new String[]{"successful-transfer", "partial-commit"}) {
            JsonNode sub = subcase(id), fake = null, recipient = null;
            for (JsonNode a : sub.path("actions")) {
                if (a.path("id").asText().equals("impersonated-acceptance")) fake = a;
                else if (a.path("capabilityId").asText().equals("transferObligation")) recipient = a;
            }
            assertNotNull(fake); assertNotNull(recipient);
            assertEquals("A", fake.path("actorRef").asText());
            assertEquals("B", recipient.path("actorRef").asText());
            assertEquals(fake.path("request").path("slots"), recipient.path("request").path("slots"));
            var fixture = Json.read(Path.of(System.getProperty("repo.root"), sub.path("fixtureRef").asText()));
            JsonNode proposal = Json.parse(fixture.at("/baseline/documents/0/content").asText());
            assertEquals("OBLIGATION_TRANSFER_PROPOSAL", proposal.path("kind").asText());
            assertEquals("NOT_ACCEPTED", proposal.path("acceptanceState").asText());
            assertEquals("A", proposal.path("proposerAlias").asText());
            assertEquals("B", proposal.path("recipientAlias").asText());
            for (JsonNode a : sub.path("assertions"))
                if (a.path("id").asText().startsWith("impersonation-")) engine.check(a, denied(id), aliases);
            var accepted = accepted(id);
            engine.check(assertion(id, "recipient-authenticated-acceptance"), accepted, aliases);
            engine.check(assertion(id, "acceptance-and-assignment-one-transaction"), accepted, aliases);
        }
    }

    @Test void sourceClaimCannotReplaceRecipientAuditOrAtomicCommit() throws Exception {
        for (String id : new String[]{"successful-transfer", "partial-commit"}) {
            var rows = accepted(id);
            var recipient = assertion(id, "recipient-authenticated-acceptance");
            ((ObjectNode) rows.get("after-db").at("/data/rawRows/audit/0")).put("actorId", "human-a");
            final var impersonated = rows;
            assertThrows(AssertionError.class, () -> engine.check(recipient, impersonated, aliases));
            rows = accepted(id);
            var transaction = assertion(id, "acceptance-and-assignment-one-transaction");
            ((ObjectNode) rows.get("after-db").at("/data/rawRows/assignments/0")).put("createdTransactionId", "tx-other");
            final var mismatch = rows;
            assertThrows(AssertionError.class, () -> engine.check(transaction, mismatch, aliases));
            ((ObjectNode) rows.get("after-db").at("/data/rawRows/audit/0")).remove("transactionId");
            assertThrows(AssertionError.class, () -> engine.check(transaction, mismatch, aliases));
            var noAudit = accepted(id);
            ((ArrayNode) noAudit.get("after-db").at("/data/rawRows/audit")).removeAll();
            assertThrows(AssertionError.class, () -> engine.check(recipient, noAudit, aliases));
        }
    }

    @Test void denialMustRetainResponsibilityQuantityAndTargetAndAudit() throws Exception {
        for (String id : new String[]{"successful-transfer", "partial-commit"}) {
            for (String table : new String[]{"works", "assignments", "obligations", "workLinks", "outbox"}) {
                var rows = denied(id);
                ((ArrayNode) rows.get("after-impersonation-db").at("/data/rawRows/" + table)).add(Json.parse("{\"unexpectedEffect\":true}"));
                var a = assertion(id, "impersonation-" + table + "-unchanged");
                assertThrows(AssertionError.class, () -> engine.check(a, rows, aliases));
            }
            var quantity = denied(id);
            ((ObjectNode) quantity.get("after-impersonation-db").at("/data/rawRows/obligations/0")).put("quantity", "9");
            var sum = assertion(id, "impersonation-root10");
            assertThrows(AssertionError.class, () -> engine.check(sum, quantity, aliases));
            var audit = denied(id);
            ((ArrayNode) audit.get("after-impersonation-db").at("/data/rawRows/audit")).removeAll();
            var denial = assertion(id, "impersonation-denial-audit");
            assertThrows(AssertionError.class, () -> engine.check(denial, audit, aliases));
        }
    }

    @Test void transferGuardCasesUseActualRecipientRatherThanAnUnrelatedDenial() throws Exception {
        var file = Json.read(Path.of(System.getProperty("repo.root"), "verification/cases/T10/case.json"));
        for (JsonNode sub : file.path("subcases")) for (JsonNode action : sub.path("actions")) {
            if (!action.path("capabilityId").asText().equals("transferObligation") ||
                action.path("id").asText().equals("impersonated-acceptance")) continue;
            assertEquals(action.at("/request/slots/acceptedById/$alias").asText(), action.path("actorRef").asText());
            var fixture = Json.read(Path.of(System.getProperty("repo.root"), sub.path("fixtureRef").asText()));
            String actor = action.path("actorRef").asText();
            assertTrue(fixture.path("actors").path(actor).path("roleCapabilities").toString().contains("transferObligation"));
            assertTrue(fixture.path("actors").path(actor).path("grant").path("actions").toString().contains("transferObligation"));
        }
    }

    @Test void cancellationAfterShipmentRequiresTheActualResidualRecipient() throws Exception {
        var file = Json.read(Path.of(System.getProperty("repo.root"), "verification/cases/T11/case.json"));
        JsonNode sub = null;
        for (JsonNode candidate : file.path("subcases"))
            if (candidate.path("id").asText().equals("cancel-after-shipment")) sub = candidate;
        assertNotNull(sub);
        for (JsonNode action : sub.path("actions")) if (action.path("id").asText().equals("transfer")) {
            assertEquals("B", action.path("actorRef").asText());
            assertEquals("TRANSFERPROPOSAL", action.at("/request/slots/acceptanceEvidenceId/$alias").asText());
        }
        var rows = accepted("successful-transfer");
        ((ObjectNode) rows.get("after-db").at("/data/rawRows/audit/0")).put("commandKey", "T11-cancel-after-shipment-transfer");
        JsonNode recipient = null, transaction = null;
        for (JsonNode a : sub.path("assertions")) {
            if (a.path("id").asText().equals("recipient-authenticated-acceptance")) recipient = a;
            if (a.path("id").asText().equals("acceptance-and-assignment-one-transaction")) transaction = a;
        }
        assertNotNull(recipient); assertNotNull(transaction);
        engine.check(recipient, rows, aliases); engine.check(transaction, rows, aliases);
        ((ObjectNode) rows.get("after-db").at("/data/rawRows/audit/0")).put("actorId", "human-a");
        final JsonNode acceptanceAssertion = recipient;
        assertThrows(AssertionError.class, () -> engine.check(acceptanceAssertion, rows, aliases));
    }
}
