package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mulino.verification.Json;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/** Focused installer probes only: no scenario runner, Workflow, model call or acceptance claim. */
class ModelFixtureInstallerTest {
    @TempDir Path temp;
    final Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath();
    static final String NOW="2026-10-07T03:00:00Z",FROM="2026-10-01T00:00:00Z",UNTIL="2026-10-31T00:00:00Z";
    @Test void inventoriesEveryOriginalCaseAndPreservesEveryOriginalByte() throws Exception {
        JsonNode lock=Json.read(root.resolve("verification/model-installation/v1/original-input-lock.json"));
        var inventories=Json.array();for(int n=1;n<=60;n++) {
            Path p=root.resolve("verification/model-binding/cases/M%02d/fixture.json".formatted(n));JsonNode f=Json.read(p);String hash=Json.sha256(p);String before=f.toString();
            var inventory=ModelFixtureInstaller.inventory(f,ModelFixtureInstaller.emptyInputs(hash),hash);
            assertFalse(inventory.path("installable").asBoolean());assertFalse(inventory.path("issues").isEmpty());assertEquals(before,f.toString());inventories.add(Json.object().put("caseId","M%02d".formatted(n)).put("fixtureSha256",hash).set("inventory",inventory));
        }
        for(JsonNode artifact:lock.path("artifacts"))assertEquals(artifact.path("sha256").asText(),Json.sha256(root.resolve(artifact.path("path").asText())),artifact.path("path").asText());
        Path output=root.resolve("verification/harness/target/model-fixture-inventory.json");Files.createDirectories(output.getParent());Json.MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.toFile(),inventories);
    }
    @Test void unknownOriginalFactsAndHashMismatchFailBeforeDatabaseAccess() {
        Setup setup=new Setup();((ObjectNode)setup.fixture.path("baseline").path("domainFacts")).put("imaginaryFact",true);
        assertTrue(ModelFixtureInstaller.inventory(setup.fixture,setup.inputs,setup.hash).path("issues").toString().contains("imaginaryFact"));
        setup.inputs.put("fixtureSha256","b".repeat(64));assertThrows(IllegalArgumentException.class,()->ModelFixtureInstaller.inventory(setup.fixture,setup.inputs,setup.hash));
    }
    @Test void originalValueAndUnknownKindCannotBeOverridden() {
        Setup setup=new Setup();setup.record("Place","W",j("name","wrong","kind","INTERNAL_STORAGE"),"/baseline/domainFacts/objects/W","");
        ((ObjectNode)setup.fixture.path("baseline").path("domainFacts").path("objects")).set("W",j("type","Place","name","authored","organizationRef","org","ref","W"));
        assertTrue(ModelFixtureInstaller.inventory(setup.fixture,setup.inputs,setup.hash).path("issues").toString().contains("ORIGINAL_VALUE_OVERRIDE_FORBIDDEN"));
        setup.record("BooleanAnswer","evil",j("eligible",true),"","");assertTrue(ModelFixtureInstaller.inventory(setup.fixture,setup.inputs,setup.hash).path("issues").toString().contains("UNKNOWN_FACT_KIND"));
    }
    @Test void authorityCorrectionPreservesNegativeCasesAndIsExactlyReversible() throws Exception {
        String pin=Json.sha256(root.resolve("contracts/acceptance-capabilities.json"));
        for(int n=1;n<=60;n++) {
            Path file=root.resolve("verification/model-binding/cases/M%02d/fixture.json".formatted(n));JsonNode original=Json.read(file);String hash=Json.sha256(file);
            var correction=ModelFixtureAuthority.apply(root,original,j("version",ModelFixtureAuthority.VERSION,"capabilityContractSha256",pin,"supplementalRead",true),hash);
            JsonNode effective=correction.fixture();assertEquals(original.path("aliases").path("actor"),effective.path("aliases").path("actor"));
            for(var entries=original.path("baseline").path("grantFacts").fields();entries.hasNext();) {
                var e=entries.next();JsonNode after=effective.path("baseline").path("grantFacts").path(e.getKey());
                for(String field:List.of("targetScope","sourceNamespace","sourceNamespaces","validFrom","validUntil","revokedAt","revision"))assertEquals(e.getValue().path(field),after.path(field));
            }
            JsonNode grant=effective.path("baseline").path("grantFacts").path("grant");
            if(n==42)assertFalse(grant.path("actions").toString().contains("releaseHold"));
            if(n==46)assertFalse(grant.path("actions").toString().contains("approveRecall"));
            if(n==47){assertEquals(2,grant.path("revision").asInt());assertEquals("2026-10-07T02:59:00Z",grant.path("revokedAt").asText());assertFalse(effective.path("baseline").path("grantFacts").path("readOnlyProbeGrant").path("actions").toString().contains("dispatchQuantity"));}
            if(n==48)assertFalse(grant.path("actions").toString().contains("dispatchQuantity"));
            if(n==57){JsonNode admin=effective.path("baseline").path("principalFacts").path("admin").path("roles");assertTrue(admin.toString().contains("closeRecall"));assertFalse(admin.toString().contains("approveRecall"));}
            ObjectNode restored=correction.fixture().deepCopy();for(int i=correction.ledger().size()-1;i>=0;i--) {
                JsonNode operation=correction.ledger().get(i);String pointer=operation.path("pointer").asText();int slash=pointer.lastIndexOf('/');ObjectNode parent=(ObjectNode)restored.at(pointer.substring(0,slash));String key=pointer.substring(slash+1);assertEquals(operation.path("after"),parent.path(key));if(operation.path("beforePresent").asBoolean())parent.set(key,operation.path("before"));else parent.remove(key);
            }assertEquals(original,restored);assertEquals(hash,Json.sha256(file));
        }
    }
    @Test void selectedM59NormalizationKeepsOriginalIdentityAndReversesExactQuantityUnits() throws Exception {
        Path file=root.resolve("verification/model-binding/cases/M59/fixture.json");JsonNode original=Json.read(file);String hash=Json.sha256(file);var corrected=ModelFixtureInputCorrection.apply(original,j("version","model-unit-normalization-v1","fromUnit","BOX","toUnit","EA","factor","12"),hash);
        assertEquals("720",corrected.fixture().at("/baseline/domainFacts/objects/A/quantity").asText());assertEquals("480",corrected.fixture().at("/baseline/domainFacts/objects/B/quantity").asText());assertEquals(original.path("aliases"),corrected.fixture().path("aliases"));
        ObjectNode restored=corrected.fixture().deepCopy();for(int i=corrected.ledger().size()-1;i>=0;i--){JsonNode e=corrected.ledger().get(i);String p=e.path("pointer").asText();((ObjectNode)restored.at(p.substring(0,p.lastIndexOf('/')))).set(p.substring(p.lastIndexOf('/')+1),e.path("before"));}assertEquals(original,restored);
        assertThrows(IllegalArgumentException.class,()->ModelFixtureInputCorrection.apply(original,j("version","model-unit-normalization-v1","fromUnit","BOX","toUnit","EA","factor","11"),hash));assertEquals(hash,Json.sha256(file));
    }
    @Test void actualPostgresInstallsTypedIdentityInventorySourceAndResponsibilityAndRollsBackInvalidFacts() throws Exception {
        String url=System.getenv(ModelFixtureTestDatabaseConfiguration.JDBC_URL);assumeTrue(url!=null,"Focused actual PostgreSQL probe skipped: set MODEL_INSTALLER_TEST_JDBC_URL only for an isolated disposable PostgreSQL cluster");
        ModelFixtureTestDatabaseConfiguration database=ModelFixtureTestDatabaseConfiguration.fromEnvironment(System.getenv());
        ObjectNode databaseProof=database.prepareFreshDatabase(root);
        Setup setup=new Setup();setup.positive();setup.source(root);Path blobs=temp.resolve("blobs");Files.createDirectories(blobs.resolve("objects"));
        var config=database.actualConfiguration(temp.resolve("unused.pem"));
        var installer=new ModelFixtureInstaller(root,config,blobs);setup.seal();var plan=ModelFixtureInstaller.inventory(setup.fixture,setup.inputs,setup.hash);
        assertTrue(plan.path("installable").asBoolean(),plan.path("issues").toPrettyString());
        try(Connection c=database.openConnection();Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT count(*) FROM mulino_identity_Organizations")) {
            assertTrue(rs.next());assertEquals(0L,rs.getLong(1),"Focused installer requires an empty disposable identity database before fixture effects");
        } catch(SQLException failure) { throw database.redacted(failure); }
        ObjectNode installed;
        try { installed=installer.install(setup.bundle(),setup.inputs); }
        catch(SQLException failure) { throw database.redacted(failure); }
        installed.set("focusedDatabaseProof",databaseProof);
        assertTrue(installed.path("committed").asBoolean());assertEquals(setup.ids.get("A"),installed.path("aliasMap").path("A").asText());
        assertFalse(installed.path("sourceFactReceipts").isEmpty());assertEquals(installed.path("sourceFactReceipts").size(),installed.path("inverse").size());
        try(Connection c=database.openConnection()) {
            assertEquals("60.000000000000",scalar(c,"SELECT quantity FROM mulino_inventory_QuantitySegments WHERE ID=?",setup.ids.get("A")));
            assertEquals(setup.ids.get("human"),scalar(c,"SELECT custodianId FROM mulino_inventory_QuantitySegments WHERE ID=?",setup.ids.get("A")));
            assertEquals("UNKNOWN",scalar(c,"SELECT availability FROM mulino_evidence_DocumentVersions WHERE ID=?",setup.ids.get("doc")));
            assertEquals("VERIFIED",scalar(c,"SELECT verdict FROM mulino_evidence_Verifications WHERE ID=?",setup.ids.get("verification")));
            assertEquals("AVAILABLE",scalar(c,"SELECT availability FROM mulino_evidence_DocumentVersions WHERE ID=?",setup.ids.get("actualDoc")));
            assertEquals("OPEN",scalar(c,"SELECT status FROM mulino_work_read_ObligationReferences WHERE ID=?",setup.ids.get("duty")));
            assertEquals("0",scalar(c,"SELECT count(*) FROM mulino_identity_CapabilityAssignments WHERE actorId=?",setup.ids.get("human")));
            long before=Long.parseLong(scalar(c,"SELECT count(*) FROM mulino_identity_Organizations WHERE ID<>?",UUID.randomUUID().toString()));
            Setup invalid=new Setup();invalid.positive();invalid.source(root);invalid.record("QuantitySegment","badFraction",j("itemId","P","lotId","L","identificationStatus","CONFIRMED","quantity","0.5","unit","BOX","placeId","W","controlScope","invalid","validFrom",NOW,"mixtureStatus","IDENTIFIED"),"","");
            long blobCount;try(var stream=Files.list(blobs.resolve("objects"))){blobCount=stream.count();}
            var invalidFailure=assertThrows(SQLException.class,()->installer.install(invalid.bundle(),invalid.inputs));
            try(var stream=Files.list(blobs.resolve("objects"))){assertEquals(blobCount,stream.count(),"Rollback removes only newly written blobs");}
            assertEquals(before,Long.parseLong(scalar(c,"SELECT count(*) FROM mulino_identity_Organizations WHERE ID<>?",UUID.randomUUID().toString())));
            assertEquals("0",scalar(c,"SELECT count(*) FROM mulino_identity_Organizations WHERE ID=?",invalid.ids.get("org")));
            Setup unknown=new Setup();unknown.positive();unknown.record("UnpublishedFact","bad",j("x","y"),"","");var unknownFailure=assertThrows(ModelFixtureInstaller.MappingException.class,()->installer.install(unknown.bundle(),unknown.inputs));
            assertEquals("0",scalar(c,"SELECT count(*) FROM mulino_identity_Organizations WHERE ID=?",unknown.ids.get("org")));
            var rollback=j("invalidSqlState",invalidFailure.getSQLState(),"invalidFailureType",invalidFailure.getClass().getSimpleName(),"invalidOrganizationRowsAfter",scalar(c,"SELECT count(*) FROM mulino_identity_Organizations WHERE ID=?",invalid.ids.get("org")),"unknownOrganizationRowsAfter",scalar(c,"SELECT count(*) FROM mulino_identity_Organizations WHERE ID=?",unknown.ids.get("org")),"unknownMappingInventory",unknownFailure.inventory(),"blobFilesBeforeInvalid",blobCount);try(var stream=Files.list(blobs.resolve("objects"))){rollback.put("blobFilesAfterInvalid",stream.count());}installed.set("focusedRollbackProof",rollback);
        } catch(SQLException failure) { throw database.redacted(failure); }
        Path output=root.resolve("verification/harness/target/model-fixture-postgresql-proof.json");Json.MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.toFile(),installed);
    }
    static String scalar(Connection c,String sql,String id) throws Exception {try(var s=c.prepareStatement(sql)){s.setString(1,id);try(var rs=s.executeQuery()){assertTrue(rs.next());return rs.getString(1);}}}
    static ObjectNode j(Object... values){var n=Json.object();for(int i=0;i<values.length;i+=2)n.set(values[i].toString(),Json.MAPPER.valueToTree(values[i+1]));return n;}
    static final class Setup {
        String hash="a".repeat(64);final ObjectNode fixture,inputs;final Map<String,String> ids=new LinkedHashMap<>();
        Setup() {
            fixture=j("schemaVersion","1.0.0","fixtureId","focused-model-installer-probe","synthetic",true,"baseRefs",List.of(),"clock",j("asOf",NOW,"knownAt",NOW,"timezone","Asia/Seoul","precision","MILLISECONDS","deadlineInclusive",true),"versions",j(),"actors",j(),"aliases",j(),"baseline",j("domainFacts",j("organizationId",id("org"),"objects",j(),"sources",j(),"evidence",j(),"obligations",List.of(),"approvalFacts",List.of(),"baselineEffects",j("bankTransferCount",0,"externalSubmissionCount",0,"newStockCount",0)),"principalFacts",j(),"grantFacts",j()),"evidence",List.of(),"responsibilities",List.of());
            ((ObjectNode)fixture.path("aliases")).put("org",ids.get("org"));inputs=ModelFixtureInstaller.emptyInputs(hash);
        }
        String id(String alias){return ids.computeIfAbsent(alias,k->UUID.randomUUID().toString());}
        ObjectNode record(String type,String alias,ObjectNode fields,String pointer,String part) {
            var r=j("type",type,"alias",alias,"id",id(alias),"sourcePointer",pointer,"part",part,"fields",fields,"originalInputProvided",false,"originalAuthorityClaimed",false,"operatingApprovalClaimed",false);((ArrayNode)inputs.path("records")).add(r);return r;
        }
        void seal(){hash=NativeClientPolicy.sha(fixture.toString());inputs.put("fixtureSha256",hash);}
        ObjectNode bundle(){seal();return j("fixture",fixture,"fixtureHash",hash);}
        void source(Path root) throws Exception {
            ObjectNode source=j("subjectKind","SEGMENT","subjectId",id("A"),"itemId",id("P"),"placeId",id("W"),"workId",id("work"),"effectiveFrom",NOW,"quantity","60","unit","BOX");
            Path file=root.resolve("verification/harness/target/model-source-"+id("source")+".json");Files.createDirectories(file.getParent());Files.writeString(file,source.toString());String sha=Json.sha256(file);
            record("Document","actualDoc",j("recordedBy","human","subjectKind","SEGMENT","subjectId","A","itemId","P","placeId","W","workId","work","sha256",sha,"sourceProfileId","source","sourceNamespace","warehouse-W","availability","AVAILABLE","byteLength",Files.size(file),"mediaType","application/json","provenance",j("source","coordinator-selected-synthetic-original","originalAuthorityClaimed",false)),"","");
            ((ObjectNode)inputs.path("documentBytes")).put("actualDoc",root.relativize(file).toString());
            ObjectNode scope=j("recordedBy","human","subjectKind","SEGMENT","subjectId","A","itemId","P","placeId","W","workId","work","effectiveFrom",NOW,"timeZone","Asia/Seoul","timePrecision","INSTANT","valueState","KNOWN");
            ObjectNode event=scope.deepCopy();event.setAll(j("kind","RECEIPT_CONFIRMED","sourceNamespace","warehouse-W","sourceProfileId","source","externalEventId","independent-synthetic-source","sourceVersion","1","payloadHash",sha,"payload",source));record("Event","event",event,"","");
            record("Inbox","inbox",j("recordedBy","human","subjectKind","SEGMENT","subjectId","A","itemId","P","placeId","W","workId","work","eventId","event","sourceNamespace","warehouse-W","sourceProfileId","source","externalEventId","independent-synthetic-source","sourceVersion","1","payloadHash",sha,"state","RECEIVED","intakeOwnerId","human","supervisorId","human","nextAction","Retain source evidence","nextCheckAt",UNTIL),"","");
            ObjectNode claim=scope.deepCopy();claim.setAll(j("sourceProfileId","source","eventId","event","documentVersionId","actualDoc","assertion",source,"quantity","60","unit","BOX"));record("Claim","claim",claim,"","");
            ObjectNode canonical=scope.deepCopy();canonical.setAll(j("sourceProfileId","source","kind","RECEIPT_CONFIRMED","physicalScopeId","A","quantity","60","unit","BOX","reassessmentState","COMPLETE"));record("CanonicalOccurrence","canonical",canonical,"","");
            record("Verification","verification",j("recordedBy","human","claimId","claim","canonicalOccurrenceId","canonical","basisDocumentId","actualDoc","policyVersion","explicit-source-v1","reason","Explicit independent synthetic bytes and exact physical source scope match"),"","");
            record("EvidenceLink","evidenceLink",j("recordedBy","human","documentVersionId","actualDoc","canonicalOccurrenceId","canonical","role","SOURCE"),"","");
            ObjectNode policy=j("synthetic",true,"operationalAuthority",false);record("PolicyVersion","policy",j("version","synthetic-v1","kind","EVIDENCE","content",policy,"contentHash",NativeClientPolicy.sha(policy.toString()),"effectiveFrom",FROM,"effectiveUntil",UNTIL),"","");
        }
        void positive() {
            record("Principal","human",j("kind","HUMAN","stableRequestOwner",id("human")),"","");
            record("Manufacturer","manufacturer",j("name","Explicit synthetic manufacturer"),"","");
            record("Product","product",j("name","Explicit synthetic biscuit"),"","");
            record("SpecificationVersion","spec",j("productId","product","version","v1","contentHash","c".repeat(64),"description","Explicit synthetic specification"),"","");
            record("PackagingVersion","pack",j("productId","product","version","v1","contentHash","d".repeat(64),"description","Explicit synthetic packaging"),"","");
            record("TradeItem","P",j("productId","product","name","Biscuit","baseUnit","BOX","decimalPlaces",0,"specificationVersionId","spec","packagingVersionId","pack"),"","");
            record("Place","W",j("name","Warehouse","kind","INTERNAL_STORAGE"),"","");
            record("Customer","customer",j("name","Explicit customer"),"","");
            record("ManufacturerLot","L",j("manufacturerId","manufacturer","itemId","P","originalLot","LOT-7"),"","");
            record("QuantitySegment","A",j("itemId","P","lotId","L","identificationStatus","CONFIRMED","quantity","60","unit","BOX","placeId","W","custodianId","human","ownerId","human","controlScope","explicit-root","validFrom",FROM,"mixtureStatus","IDENTIFIED"),"","");
            record("ExternalIdentity","identity",j("actorId","human","issuer","fixture-issuer","subject","explicit-synthetic","organizationAlias",ids.get("org")),"","");
            record("UnitConversion","conversion",j("itemId","P","fromUnit","BOX","toUnit","EA","factor","12","validFrom",FROM),"","");
            record("ExternalIdentifier","identifier",j("itemId","P","issuer","Explicit manufacturer","namespace","SKU","value","MB-01","validFrom",FROM),"","");
            record("SourceProfile","source",j("recordedBy","human","namespace","warehouse-W","policyVersion","explicit-source-v1","intakeOwnerId","human","supervisorId","human","nextAction","Review source evidence","nextCheckAt",UNTIL),"","");
            record("Document","doc",j("recordedBy","human","subjectKind","ITEM","subjectId","P","itemId","P","sha256","e".repeat(64),"sourceProfileId","source","sourceNamespace","warehouse-W","availability","UNKNOWN","byteLength",0L,"mediaType","application/octet-stream","provenance",j("source","explicit-input-hash-only","originalInputProvided",false)),"","");
            ObjectNode content=j("version","explicit-v1","evaluatorVersion","core-v1","schemaVersion","1.0.0");
            record("DefinitionVersion","definition",j("version","explicit-v1","state","PUBLISHED","contentHash",NativeClientPolicy.sha(content.toString()),"content",content,"evaluatorVersion","core-v1","schemaVersion","1.0.0"),"","");
            record("Work","work",j("itemId","P","definitionVersionId","definition","kind","PURCHASE","status","ACTIVE","ownerId","human","supervisorId","human","lifecycleMode","COMMAND","nextAction","Review arrival","nextCheckAt",UNTIL),"","");
            record("Goal","goal",j("workId","work","definitionVersionId","definition","quantityMode","CUMULATIVE_EVENT","targetQuantity","100","unit","BOX","endpoint","ARRIVED","scopeJson",j("placeId",id("W")),"evidencePolicyVersion","explicit-source-v1","timezone","Asia/Seoul","dueAt",UNTIL),"","");
            record("SubjectLink","subject",j("workId","work","itemId","P","lotId","L"),"","");
            record("ObligationRoot","dutyRoot",j("dutyKey","f".repeat(64),"sourceId","work","sourceKind","WORK","sourceVersion","1","kind","RECEIPT_REVIEW","scopeJson",j("targetId",id("A")),"quantity","60","unit","BOX"),"","");
            record("ObligationScope","dutyScope",j("rootId","dutyRoot","startQuantity","0","quantity","60","unit","BOX","leaf",true,"scopeJson",j("targetId",id("A"))),"","");
            record("Obligation","duty",j("workId","work","kind","RECEIPT_REVIEW","status","OPEN","ownerId","human","supervisorId","human","nextAction","Review source receipt","nextCheckAt",UNTIL,"scopeJson",j("targetId",id("A")),"rootId","dutyRoot","scopeId","dutyScope","quantity","60","unit","BOX","valid",true),"","");
        }
    }
}
