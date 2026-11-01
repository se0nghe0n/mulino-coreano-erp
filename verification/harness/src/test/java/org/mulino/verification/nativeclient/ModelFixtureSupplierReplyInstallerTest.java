package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mulino.verification.Json;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Bounded actual installation coverage for an explicit synthetic SupplierReply fact. */
class ModelFixtureSupplierReplyInstallerTest {
    @TempDir Path temp;
    final Path root = Path.of(System.getProperty("repo.root")).toAbsolutePath();
    static final String NOW = ModelFixtureInstallerTest.NOW;
    static final String FROM = ModelFixtureInstallerTest.FROM;
    static final String UNTIL = ModelFixtureInstallerTest.UNTIL;
    static final String NAMESPACE = "supplier-reply-test";
    static final String POLICY = "synthetic-supplier-reply-v1";

    @Test void standaloneSupplierReplyPlanRequiresItsOwnTypedRowAndEvidenceBindings() throws Exception {
        ModelFixtureInstallerTest.Setup setup = supplierReplySetup();
        ObjectNode bundle = setup.bundle();
        ObjectNode inventory = ModelFixtureInstaller.inventory(bundle.path("fixture"), setup.inputs,
                bundle.path("fixtureHash").asText());

        assertTrue(inventory.path("installable").asBoolean(), inventory.path("issues").toPrettyString());
        JsonNode replyMapping = null;
        for (JsonNode mapping : inventory.path("mapping")) {
            if (mapping.path("type").asText().equals("SupplierReply")) replyMapping = mapping;
        }
        assertNotNull(replyMapping, "An Event, Supplier, and PurchaseOrder are not a SupplierReply row");
        assertEquals("supplierReply", replyMapping.path("alias").asText());
        assertEquals("mulino_trade_purchase_SupplierReplies", replyMapping.path("table").asText());

        JsonNode reply = record(setup, "SupplierReply");
        assertEquals("supplierOrder", reply.path("fields").path("orderId").asText());
        assertEquals("supplierReplyEvidence", reply.path("fields").path("evidenceId").asText());
        assertEquals("supplierAcceptOccurrence", reply.path("fields").path("canonicalOccurrenceId").asText());

        ((ObjectNode) reply.path("fields")).remove("canonicalOccurrenceId");
        bundle = setup.bundle();
        inventory = ModelFixtureInstaller.inventory(bundle.path("fixture"), setup.inputs,
                bundle.path("fixtureHash").asText());
        assertFalse(inventory.path("installable").asBoolean());
        assertTrue(inventory.path("issues").toString().contains("REQUIRED_TYPED_FIELD:SupplierReply"),
                inventory.path("issues").toPrettyString());
    }

    @Test void actualPostgresReadsBackSupplierReplyEvidenceAndExactInverseAndRejectsMissingOccurrenceAtomically() throws Exception {
        String url = System.getenv(ModelFixtureTestDatabaseConfiguration.JDBC_URL);
        assumeTrue(url != null,
                "SupplierReply PostgreSQL probe skipped: set MODEL_INSTALLER_TEST_JDBC_URL only for an isolated disposable PostgreSQL cluster");
        ModelFixtureTestDatabaseConfiguration database = ModelFixtureTestDatabaseConfiguration.fromEnvironment(System.getenv());
        ObjectNode databaseProof = database.prepareFreshDatabase(root);
        Path blobs = temp.resolve("blobs");
        Files.createDirectories(blobs.resolve("objects"));
        var installer = new ModelFixtureInstaller(root,
                database.actualConfiguration(temp.resolve("unused.pem")), blobs);

        ModelFixtureInstallerTest.Setup setup = supplierReplySetup();
        ObjectNode bundle = setup.bundle();
        ObjectNode plan = ModelFixtureInstaller.inventory(bundle.path("fixture"), setup.inputs,
                bundle.path("fixtureHash").asText());
        assertTrue(plan.path("installable").asBoolean(), plan.path("issues").toPrettyString());
        try (Connection c = database.openConnection()) {
            assertEquals("0", ModelFixtureInstallerTest.scalar(c,
                    "SELECT count(*) FROM mulino_identity_Organizations WHERE ID=?", setup.ids.get("org")));
        } catch (SQLException failure) {
            throw database.redacted(failure);
        }

        ObjectNode installed;
        try {
            installed = installer.install(bundle, setup.inputs);
        } catch (SQLException failure) {
            throw database.redacted(failure);
        }
        installed.set("focusedDatabaseProof", databaseProof);
        assertTrue(installed.path("committed").asBoolean());
        assertFalse(installed.path("businessExecutionClaimed").asBoolean());
        assertEquals("NOT_RUN", installed.path("acceptanceStatus").asText());

        JsonNode receipt = null;
        for (JsonNode row : installed.path("sourceFactReceipts")) {
            if (row.path("type").asText().equals("SupplierReply")) receipt = row;
        }
        assertNotNull(receipt, "The typed SupplierReply row must have installer SQL readback");
        assertEquals(setup.ids.get("supplierReply"), receipt.path("key").path("ID").asText());
        assertEquals(setup.ids.get("supplierOrder"), receipt.path("installedValues").path("orderId").asText());
        assertEquals("ACCEPT", receipt.path("readback").path("reply").asText());
        assertEquals("60.000000000000", receipt.path("readback").path("proposedquantity").asText());
        assertEquals(setup.ids.get("supplierReplyEvidence"), receipt.path("readback").path("evidenceid").asText());
        assertEquals(setup.ids.get("supplierAcceptOccurrence"), receipt.path("readback").path("canonicaloccurrenceid").asText());
        assertEquals(java.time.Instant.parse(NOW), OffsetDateTime.parse(
                receipt.path("installedValues").path("effectiveAt").asText()).toInstant());
        try (Connection c = database.openConnection();
             var statement = c.prepareStatement("SELECT orderId,reply,proposedQuantity,unit,evidenceId,canonicalOccurrenceId,effectiveAt FROM mulino_trade_purchase_SupplierReplies WHERE organizationId=? AND ID=?")) {
            statement.setString(1, setup.ids.get("org"));
            statement.setString(2, setup.ids.get("supplierReply"));
            try (var rs = statement.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(setup.ids.get("supplierOrder"), rs.getString("orderId"));
                assertEquals("ACCEPT", rs.getString("reply"));
                assertEquals(0, rs.getBigDecimal("proposedQuantity").compareTo(new java.math.BigDecimal("60")));
                assertEquals("BOX", rs.getString("unit"));
                assertEquals(setup.ids.get("supplierReplyEvidence"), rs.getString("evidenceId"));
                assertEquals(setup.ids.get("supplierAcceptOccurrence"), rs.getString("canonicalOccurrenceId"));
                assertEquals(java.time.Instant.parse(NOW), rs.getObject("effectiveAt", OffsetDateTime.class).toInstant());
                assertFalse(rs.next());
            }
        } catch (SQLException failure) {
            throw database.redacted(failure);
        }
        try (Connection c = database.openConnection()) {
            assertEquals("PURCHASE_ORDER", scalar(c,
                    "SELECT subjectKind FROM mulino_evidence_DocumentVersions WHERE organizationId=? AND ID=?",
                    setup.ids.get("org"), setup.ids.get("supplierReplyEvidence")));
        } catch (SQLException failure) {
            throw database.redacted(failure);
        }

        JsonNode inverse = null;
        for (JsonNode row : installed.path("inverse")) {
            if (row.path("table").asText().equals("mulino_trade_purchase_SupplierReplies")
                    && row.path("key").path("ID").asText().equals(setup.ids.get("supplierReply"))) inverse = row;
        }
        assertNotNull(inverse, "The inverse ledger must include the exact inserted SupplierReply row");
        assertEquals("DELETE_EXACT_INSERTED_ROW", inverse.path("operation").asText());
        assertEquals(receipt.path("installedValues"), inverse.path("expectedValues"));

        long blobCountBefore;
        try (var files = Files.list(blobs.resolve("objects"))) { blobCountBefore = files.count(); }
        ModelFixtureInstallerTest.Setup invalid = supplierReplySetup();
        JsonNode invalidReply = record(invalid, "SupplierReply");
        ((ObjectNode) invalidReply.path("fields")).put("canonicalOccurrenceId", UUID.randomUUID().toString());
        ObjectNode invalidBundle = invalid.bundle();
        SQLException foreignKeyFailure = assertThrows(SQLException.class,
                () -> installer.install(invalidBundle, invalid.inputs));
        assertEquals("23503", foreignKeyFailure.getSQLState(), "Missing canonical occurrence is a typed FK boundary");
        try (Connection c = database.openConnection()) {
            assertEquals("0", ModelFixtureInstallerTest.scalar(c,
                    "SELECT count(*) FROM mulino_identity_Organizations WHERE ID=?", invalid.ids.get("org")));
            assertEquals("0", scalar(c,
                    "SELECT count(*) FROM mulino_trade_purchase_SupplierReplies WHERE organizationId=? AND ID=?",
                    invalid.ids.get("org"), invalid.ids.get("supplierReply")));
        } catch (SQLException failure) {
            throw database.redacted(failure);
        }
        try (var files = Files.list(blobs.resolve("objects"))) {
            assertEquals(blobCountBefore, files.count(), "Rollback removes blobs written only for the rejected fixture");
        }

        long blobCountAfter;
        try (var files = Files.list(blobs.resolve("objects"))) { blobCountAfter = files.count(); }
        ObjectNode output = installed.deepCopy();
        output.set("missingOccurrenceRollback", ModelFixtureInstallerTest.j(
                "sqlState", foreignKeyFailure.getSQLState(),
                "organizationRowsAfter", 0,
                "supplierReplyRowsAfter", 0,
                "blobFilesBefore", blobCountBefore,
                "blobFilesAfter", blobCountAfter));
        Path report = root.resolve("verification/harness/target/model-fixture-supplier-reply-postgresql-proof.json");
        Files.createDirectories(report.getParent());
        Json.MAPPER.writerWithDefaultPrettyPrinter().writeValue(report.toFile(), output);
    }

    private ModelFixtureInstallerTest.Setup supplierReplySetup() throws Exception {
        var setup = new ModelFixtureInstallerTest.Setup();
        setup.positive();
        setup.source(root);

        setup.record("Supplier", "supplier", j("name", "Explicit synthetic supplier", "effectiveAt", NOW), "", "");
        setup.record("PurchaseProposal", "supplierProposal",
                j("workId", "work", "currentRevision", 1, "status", "APPROVED"), "", "");

        String hash = proposalHash(setup);
        setup.record("PurchaseRevision", "supplierProposalRevision",
                j("proposalId", "supplierProposal", "revision", 1, "workId", "work", "goalVersionId", "goal",
                        "itemId", "P", "supplierId", "supplier", "destinationId", "W", "quantity", "60",
                        "unit", "BOX", "price", "4.20", "currency", "EUR", "dueAt", UNTIL,
                        "endpoint", "ARRIVED", "quantityMode", "CUMULATIVE_EVENT", "proposalHash", hash), "", "");
        setup.record("PurchaseApproval", "supplierApproval",
                j("proposalId", "supplierProposal", "proposalRevision", 1, "proposalHash", hash,
                        "approverId", "human", "policyHash", NativeClientPolicy.sha("synthetic supplier reply test policy"),
                        "decision", "APPROVE", "conditionsJson", java.util.List.of(),
                        "decidedAt", NOW, "validUntil", UNTIL, "consumptionPolicy", "SINGLE_ORDER_REVISION"), "", "");
        setup.record("PurchaseOrder", "supplierOrder",
                j("proposalId", "supplierProposal", "proposalRevision", 1, "proposalHash", hash,
                        "approvalId", "supplierApproval", "channel", "SYNTHETIC_TEST_ONLY",
                        "externalOperationId", "supplier-reply-fixture-test", "outboxId", setup.id("syntheticOutbox"),
                        "conditionAssessmentJson", j("decision", "UNCONDITIONAL"), "effectiveAt", NOW), "", "");
        setup.record("PurchaseLine", "supplierOrderLine",
                j("orderId", "supplierOrder", "proposalId", "supplierProposal", "proposalRevision", 1,
                        "workId", "work", "itemId", "P", "destinationId", "W", "quantity", "60",
                        "unit", "BOX", "status", "ORDERED"), "", "");

        setup.record("PolicyVersion", "supplierReplyPolicy",
                policyVersion("synthetic-supplier-reply-v1"), "", "");
        setup.record("SourceProfile", "supplierReplyProfile",
                j("recordedBy", "human", "namespace", NAMESPACE, "policyVersion", POLICY,
                        "intakeOwnerId", "human", "supervisorId", "human",
                        "nextAction", "Review synthetic supplier reply", "nextCheckAt", UNTIL), "", "");

        ObjectNode payload = j("subjectKind", "PURCHASE_ORDER", "subjectId", setup.id("supplierOrder"),
                "itemId", setup.id("P"), "workId", setup.id("work"), "effectiveFrom", NOW,
                "supplierId", setup.id("supplier"), "reply", "ACCEPT", "quantity", "60", "unit", "BOX",
                "externalReference", "synthetic-supplier-reply-only");
        byte[] sourceBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
        Path source = root.resolve("verification/harness/target/model-supplier-reply-" + setup.id("replySource") + ".json");
        Files.createDirectories(source.getParent());
        Files.write(source, sourceBytes);
        String sourceHash = Json.sha256(source);

        setup.record("Document", "supplierReplyEvidence",
                j("recordedBy", "human", "subjectKind", "PURCHASE_ORDER", "subjectId", "supplierOrder",
                        "itemId", "P", "workId", "work", "sha256", sourceHash,
                        "sourceProfileId", "supplierReplyProfile", "sourceNamespace", NAMESPACE,
                        "availability", "AVAILABLE", "byteLength", sourceBytes.length,
                        "mediaType", "application/json", "provenance", j("source", "authored-synthetic-supplier-reply",
                                "originalAuthorityClaimed", false)), "", "");
        ((ObjectNode) setup.inputs.path("documentBytes")).put("supplierReplyEvidence", root.relativize(source).toString());

        setup.record("Event", "supplierAcceptEvent",
                j("recordedBy", "human", "subjectKind", "PURCHASE_ORDER", "subjectId", "supplierOrder",
                        "itemId", "P", "workId", "work", "effectiveFrom", NOW, "timeZone", "Asia/Seoul",
                        "timePrecision", "INSTANT", "valueState", "KNOWN", "kind", "SUPPLIER_ACCEPT",
                        "sourceNamespace", NAMESPACE, "sourceProfileId", "supplierReplyProfile",
                        "externalEventId", "supplier-reply-test-01", "sourceVersion", "1",
                        "payloadHash", sourceHash, "payload", payload), "", "");
        setup.record("Inbox", "supplierReplyInbox",
                j("recordedBy", "human", "subjectKind", "PURCHASE_ORDER", "subjectId", "supplierOrder",
                        "itemId", "P", "workId", "work", "eventId", "supplierAcceptEvent",
                        "sourceNamespace", NAMESPACE, "sourceProfileId", "supplierReplyProfile",
                        "externalEventId", "supplier-reply-test-01", "sourceVersion", "1",
                        "payloadHash", sourceHash, "state", "RECEIVED", "intakeOwnerId", "human",
                        "supervisorId", "human", "nextAction", "Review synthetic supplier reply",
                        "nextCheckAt", UNTIL), "", "");
        setup.record("Claim", "supplierReplyClaim",
                j("recordedBy", "human", "subjectKind", "PURCHASE_ORDER", "subjectId", "supplierOrder",
                        "itemId", "P", "workId", "work", "effectiveFrom", NOW, "timeZone", "Asia/Seoul",
                        "timePrecision", "INSTANT", "valueState", "KNOWN", "sourceProfileId", "supplierReplyProfile",
                        "eventId", "supplierAcceptEvent", "documentVersionId", "supplierReplyEvidence",
                        "assertion", payload, "quantity", "60", "unit", "BOX"), "", "");
        setup.record("CanonicalOccurrence", "supplierAcceptOccurrence",
                j("recordedBy", "human", "subjectKind", "PURCHASE_ORDER", "subjectId", "supplierOrder",
                        "itemId", "P", "workId", "work", "effectiveFrom", NOW, "timeZone", "Asia/Seoul",
                        "timePrecision", "INSTANT", "valueState", "KNOWN", "sourceProfileId", "supplierReplyProfile",
                        "kind", "SUPPLIER_ACCEPT", "physicalScopeId", "supplierOrder", "occurrenceIdentity", "supplierAcceptEvent",
                        "occurrenceSemanticHash", sourceHash, "quantity", "60", "unit", "BOX",
                        "reassessmentState", "COMPLETE"), "", "");
        setup.record("Verification", "supplierReplyVerification",
                j("recordedBy", "human", "claimId", "supplierReplyClaim",
                        "canonicalOccurrenceId", "supplierAcceptOccurrence", "basisDocumentId", "supplierReplyEvidence",
                        "policyVersion", POLICY,
                        "reason", "Synthetic reply bytes, purchase order, supplier, quantity and time match"), "", "");
        setup.record("EvidenceLink", "supplierReplyEventLink",
                j("recordedBy", "human", "documentVersionId", "supplierReplyEvidence",
                        "eventId", "supplierAcceptEvent", "role", "SOURCE"), "", "");
        setup.record("EvidenceLink", "supplierReplyClaimLink",
                j("recordedBy", "human", "documentVersionId", "supplierReplyEvidence",
                        "claimId", "supplierReplyClaim", "role", "SOURCE"), "", "");
        setup.record("EvidenceLink", "supplierReplyOccurrenceLink",
                j("recordedBy", "human", "documentVersionId", "supplierReplyEvidence",
                        "canonicalOccurrenceId", "supplierAcceptOccurrence", "role", "SOURCE"), "", "");
        setup.record("SupplierReply", "supplierReply",
                j("orderId", "supplierOrder", "reply", "ACCEPT", "proposedQuantity", "60", "unit", "BOX",
                        "evidenceId", "supplierReplyEvidence", "canonicalOccurrenceId", "supplierAcceptOccurrence",
                        "effectiveAt", NOW), "", "");
        return setup;
    }

    private static ObjectNode policyVersion(String version) {
        ObjectNode content = j("version", version, "evaluatorVersion", "source-evidence-v1",
                "synthetic", true, "operationalAuthority", false);
        return j("version", version, "kind", "EVIDENCE", "content", content,
                "contentHash", NativeClientPolicy.sha(content.toString()), "effectiveFrom", FROM, "effectiveUntil", UNTIL);
    }

    private static String proposalHash(ModelFixtureInstallerTest.Setup setup) throws Exception {
        Map<String, String> canonical = new TreeMap<>();
        canonical.put("proposalId", setup.id("supplierProposal"));
        canonical.put("revision", "1");
        canonical.put("workId", setup.id("work"));
        canonical.put("itemId", setup.id("P"));
        canonical.put("supplierId", setup.id("supplier"));
        canonical.put("destinationId", setup.id("W"));
        canonical.put("quantity", "60");
        canonical.put("unit", "BOX");
        canonical.put("price", "4.20");
        canonical.put("currency", "EUR");
        canonical.put("dueAt", java.time.OffsetDateTime.parse(UNTIL).toInstant().toString());
        canonical.put("endpoint", "ARRIVED");
        canonical.put("quantityMode", "CUMULATIVE_EVENT");
        return NativeClientPolicy.sha(Json.MAPPER.writeValueAsString(canonical));
    }

    private static JsonNode record(ModelFixtureInstallerTest.Setup setup, String type) {
        for (JsonNode row : setup.inputs.path("records")) if (row.path("type").asText().equals(type)) return row;
        throw new AssertionError("Missing explicit " + type + " input");
    }

    private static String scalar(Connection connection, String sql, String first, String second) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, first);
            statement.setString(2, second);
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                String value = result.getString(1);
                assertFalse(result.next());
                return value;
            }
        }
    }

    private static ObjectNode j(Object... values) { return ModelFixtureInstallerTest.j(values); }
}
