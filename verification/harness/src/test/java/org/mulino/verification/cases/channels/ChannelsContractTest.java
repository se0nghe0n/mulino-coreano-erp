package org.mulino.verification.cases.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Fixed parser/assertion samples only. Never implements a product, host or model adapter. */
final class ChannelsContractTest {
    private final Path root=Path.of(System.getProperty("repo.root"));
    private final AssertionEngine engine=new AssertionEngine();
    private JsonNode declared(String caseId,String subId,String id) throws Exception {
        for(JsonNode s:Json.read(root.resolve("verification/cases/"+caseId+"/case.json")).path("subcases"))
            if(s.path("id").asText().equals(subId)) for(JsonNode a:s.path("assertions"))
                if(a.path("id").asText().equals(id))return a;
        throw new AssertionError("Missing actual declared assertion "+caseId+"/"+subId+"/"+id);
    }
    private ObjectNode sample(String payload) throws Exception {
        ObjectNode n=(ObjectNode)Json.parse(payload);
        n.put("driverStatus","EXECUTED");
        n.set("provenance",Json.parse("{\"scopeComplete\":true,\"source\":\"CAPTURED_ASSERTION_SELFTEST\",\"independent\":false}"));
        return n;
    }
    private JsonNode bind(JsonNode a,Map<String,JsonNode> results) {
        ObjectNode out=a.deepCopy();
        var aliases=Json.object();for(String name:List.of("ORG","P","L","A60","B40","W","O1","S1","reader","writer","C"))aliases.put(name,"captured-"+name);
        var resolver=new ReferenceResolver(results,aliases);
        for(String key:List.of("expected","scope"))out.set(key,resolver.identity(out.path(key)));
        for(String key:List.of("source","baseline","unitSource","baselineUnitSource"))if(out.has(key)) {
            ObjectNode source=(ObjectNode)out.get(key);if(source.has("where"))source.set("where",resolver.identity(source.get("where")));
        }
        return out;
    }
    private void check(JsonNode a,Map<String,JsonNode> results) { engine.check(bind(a,results),results); }
    @TestFactory Stream<DynamicTest> actualCasesHaveValidSchemaAndCompleteKoreanSelectors() {
        return Stream.of("T01","T20","T25").map(id->DynamicTest.dynamicTest(id,()->{
            ContractValidator validator=new ContractValidator(root);
            Path path=root.resolve("verification/cases/"+id+"/case.json");
            JsonNode c=validator.caseFile(path);
            assertEquals(List.of(),new PreparationValidator(root).feature(path,c));
        }));
    }
    @Test void everyNamedObservationUsesSubstantiveOperatorsAndReferencesActualDeclaredActions() throws Exception {
        JsonNode catalog=Json.read(root.resolve("verification/requirements/mandatory-oracles.json"));
        Set<String> required=new HashSet<>(),observed=new HashSet<>();
        for(JsonNode o:catalog.path("oracles"))if(Set.of("T01","T20","T25").contains(o.path("caseId").asText()))
            for(JsonNode n:o.path("expectedObservations"))required.add(o.path("oracleId").asText()+"/"+n.path("name").asText());
        for(String cid:List.of("T01","T20","T25"))for(JsonNode s:Json.read(root.resolve("verification/cases/"+cid+"/case.json")).path("subcases"))
            for(JsonNode a:s.path("assertions")) {
                assertNotEquals("present",a.path("op").asText(),"Presence is not semantic conjunction coverage");
                for(JsonNode n:a.path("oracleRef").path("observationNames"))observed.add(a.path("oracleRef").path("oracleId").asText()+"/"+n.asText());
            }
        assertEquals(24,required.size());assertEquals(required,observed);
    }
    @Test void wrongQuantityAndWrongActualUnitAreRejected() throws Exception {
        JsonNode a=declared("T01","same-world","noun-heldQuantity");
        ObjectNode n=sample("{\"response\":{\"data\":{\"heldQuantity\":\"100\",\"unit\":\"BOX\"}}}");
        Map<String,JsonNode> rs=Map.of("noun",n);check(a,rs);
        ((ObjectNode)n.path("response").path("data")).put("heldQuantity","200");
        assertThrows(AssertionError.class,()->check(a,rs));
        ((ObjectNode)n.path("response").path("data")).put("heldQuantity","100").put("unit","KG");
        assertThrows(AssertionError.class,()->check(a,rs));
    }
    @Test void identicalTimesDoNotHideDifferentDbSnapshots() throws Exception {
        JsonNode a=declared("T01","same-world","same-snapshotRevision");
        ObjectNode noun=sample("{\"response\":{\"snapshotRevision\":\"snapshot-1\",\"knownAt\":\"2026-10-07T09:00:01Z\"}}");
        ObjectNode verb=sample("{\"response\":{\"snapshotRevision\":\"snapshot-1\",\"knownAt\":\"2026-10-07T09:00:01Z\"}}");
        var rs=Map.<String,JsonNode>of("noun",noun,"verb",verb);check(a,rs);
        ((ObjectNode)verb.get("response")).put("snapshotRevision","snapshot-2");assertThrows(AssertionError.class,()->check(a,rs));
    }
    @Test void duplicatePhysicalLeafFailsWithoutRelyingOnSum() throws Exception {
        JsonNode a=declared("T01","same-world","unique-physical-leaf");
        ObjectNode db=sample("{\"data\":{\"rawRows\":{\"segments\":[{\"physicalScope\":\"A\",\"quantity\":\"60\"},{\"physicalScope\":\"B\",\"quantity\":\"40\"}]}}}");
        var rs=Map.<String,JsonNode>of("db-after",db);check(a,rs);
        ((ObjectNode)db.path("data").path("rawRows").path("segments").get(1)).put("physicalScope","A");assertThrows(AssertionError.class,()->check(a,rs));
    }
    @Test void missingHumanOwnerAndChangedNextActionFailExactResponsibilityTuple() throws Exception {
        JsonNode a=declared("T01","same-world","responsibility");
        ObjectNode db=sample("{\"data\":{\"rawRows\":{\"obligations\":[{\"responsibleWorkId\":\"captured-O1\",\"ownerId\":\"captured-reader\",\"nextAction\":\"검사 근거 확인\",\"nextCheckAt\":\"2026-10-07T10:00:00Z\"},{\"responsibleWorkId\":\"captured-S1\",\"ownerId\":\"captured-writer\",\"nextAction\":\"인도 증거 확인\",\"nextCheckAt\":\"2026-10-07T10:00:00Z\"}]}}}");
        var rs=Map.<String,JsonNode>of("db-after",db);JsonNode bound=bind(a,rs);check(bound,rs);
        ObjectNode row=(ObjectNode)db.path("data").path("rawRows").path("obligations").get(0);
        row.remove("ownerId");assertThrows(AssertionError.class,()->check(bound,rs));
        row.put("ownerId","captured-reader").put("nextAction","의무 종료");assertThrows(AssertionError.class,()->check(bound,rs));
    }
    @Test void rawHeaderMismatchCannotBeRepairedByHarness() throws Exception {
        JsonNode a=declared("T20","wire-method-mismatch","wire-raw-Mcp-Method");
        ObjectNode r=sample("{\"data\":{\"transcript\":{\"request\":{\"headers\":{\"Mcp-Method\":\"tools/call\"}}}}}");
        var rs=Map.<String,JsonNode>of("wire",r);check(a,rs);
        ((ObjectNode)r.path("data").path("transcript").path("request").path("headers")).put("Mcp-Method","server/discover");assertThrows(AssertionError.class,()->check(a,rs));
    }
    @Test void discoverUsesSupportedVersionsAndToolsListOwnsRegistry() throws Exception {
        JsonNode version=declared("T20","wire-discover","protocol-result-version");
        JsonNode type=declared("T20","wire-discover","discover-result-type");
        JsonNode capability=declared("T20","wire-discover","discover-tools-capability");
        ObjectNode discovery=sample("{\"response\":{\"body\":{\"result\":{\"resultType\":\"complete\",\"supportedVersions\":[\"2026-07-28\"],\"capabilities\":{\"tools\":{}}}}}}");
        var rs=Map.<String,JsonNode>of("wire",discovery);
        check(version,rs);check(type,rs);check(capability,rs);
        ObjectNode result=(ObjectNode)discovery.path("response").path("body").path("result");
        result.remove("supportedVersions");result.put("protocolVersion","2026-07-28");
        assertThrows(AssertionError.class,()->check(version,rs));
        JsonNode registry=declared("T20","wire-discover","tool-schema-registry");
        assertEquals("tools-list",registry.path("source").path("actionId").asText());
        assertThrows(AssertionError.class,()->check(registry,rs));
    }
    @Test void missingClientInfoKeepsMandatoryVersionAndCapabilities() throws Exception {
        JsonNode c=Json.read(root.resolve("verification/cases/T20/case.json"));
        JsonNode selected=null;
        for(JsonNode s:c.path("subcases"))if(s.path("id").asText().equals("wire-missing-client-info"))selected=s;
        assertNotNull(selected);
        for(JsonNode a:selected.path("actions"))if(a.path("id").asText().equals("wire")) {
            JsonNode meta=a.path("request").path("body").path("params").path("_meta");
            assertFalse(meta.has("io.modelcontextprotocol/clientInfo"));
            assertEquals("2026-07-28",meta.path("io.modelcontextprotocol/protocolVersion").asText());
            assertTrue(meta.has("io.modelcontextprotocol/clientCapabilities"));
        }
        JsonNode http=declared("T20","wire-missing-client-info","wire-http");
        var rs=Map.<String,JsonNode>of("wire",sample("{\"response\":{\"httpStatus\":200}}"));
        check(http,rs);
        ((ObjectNode)rs.get("wire").get("response")).put("httpStatus",400);
        assertThrows(AssertionError.class,()->check(http,rs));
    }
    @Test void mrtrNewRpcIdIsCheckedOnActualRequestAndResponse() throws Exception {
        JsonNode a=declared("T20","mrtr-continuation","continued-raw-jsonrpc-id");
        ObjectNode r=sample("{\"data\":{\"transcript\":{\"request\":{\"body\":{\"id\":\"T20-new-rpc\"}}}}}");
        var rs=Map.<String,JsonNode>of("continued",r);check(a,rs);
        ((ObjectNode)r.path("data").path("transcript").path("request").path("body")).put("id","issued");assertThrows(AssertionError.class,()->check(a,rs));
    }
    @Test void actualIssuedStateMutationPreservesSourceAndChangesOneByte() throws Exception {
        ObjectNode r=sample("{\"response\":{\"state\":\"signed-state-A\"}}");
        var resolver=new ReferenceResolver(Map.of("issued",r),Json.object());
        JsonNode t=Json.parse("{\"$transform\":{\"source\":{\"$result\":{\"actionId\":\"issued\",\"pointer\":\"/response/state\"}},\"operation\":\"opaqueByteXor\",\"index\":0,\"xor\":1}}");
        assertEquals("rigned-state-A",resolver.resolve(t).asText());assertEquals("signed-state-A",r.path("response").path("state").asText());
        ((ObjectNode)r.get("provenance")).put("scopeComplete",false);assertThrows(AssertionError.class,()->resolver.resolve(t));
    }
    @Test void skillHashAloneCannotPassBodyLoading() throws Exception {
        JsonNode a=declared("T20","skill-work-coordinator","loading-body_read");
        ObjectNode r=sample("{\"data\":{\"hostObservation\":{\"extractor\":{\"rawRows\":{\"loading\":[{\"packageName\":\"ontology-work-coordinator\",\"stage\":\"BODY_READ\"}]}}}}}");
        var rs=Map.<String,JsonNode>of("probe",r);check(a,rs);
        ((ObjectNode)r.path("data").path("hostObservation").path("extractor").path("rawRows").path("loading").get(0)).put("stage","HASH_ONLY");assertThrows(AssertionError.class,()->check(a,rs));
    }
    @Test void clientCommandToolCallCannotHideBehindReadResult() throws Exception {
        JsonNode a=declared("T20","host-malicious-document","no-command-tools");
        ObjectNode r=sample("{\"data\":{\"hostObservation\":{\"extractor\":{\"rawRows\":{\"toolCalls\":[{\"intentKind\":\"QUERY\"}]}}}}}");
        var rs=Map.<String,JsonNode>of("probe",r);check(a,rs);
        ((ObjectNode)r.path("data").path("hostObservation").path("extractor").path("rawRows").path("toolCalls").get(0)).put("intentKind","COMMAND");assertThrows(AssertionError.class,()->check(a,rs));
    }
    @Test void clientRecordToolCallCannotHideBehindReadResult() throws Exception {
        JsonNode a=declared("T20","host-malicious-document","no-record-tools");
        ObjectNode r=sample("{\"data\":{\"hostObservation\":{\"extractor\":{\"rawRows\":{\"toolCalls\":[{\"intentKind\":\"QUERY\"}]}}}}}");
        var rs=Map.<String,JsonNode>of("probe",r);check(a,rs);
        ((ObjectNode)r.path("data").path("hostObservation").path("extractor").path("rawRows").path("toolCalls").get(0)).put("intentKind","RECORD");assertThrows(AssertionError.class,()->check(a,rs));
    }
    @Test void officialWireErrorCodeIsRequiredNotProjectCategory() throws Exception {
        JsonNode code=declared("T20","wire-name-mismatch","wire-error-code");
        ObjectNode r=sample("{\"response\":{\"httpStatus\":400,\"body\":{\"jsonrpc\":\"2.0\",\"id\":\"wire\",\"error\":{\"code\":-32020,\"message\":\"Header mismatch\",\"data\":{}}}}}");
        var rs=Map.<String,JsonNode>of("wire",r);check(code,rs);
        ((ObjectNode)r.at("/response/body/error")).put("code",-32602).set("data",Json.parse("{\"category\":\"PROTOCOL\"}"));assertThrows(AssertionError.class,()->check(code,rs));
        JsonNode supported=declared("T20","wire-unsupported-version","unsupported-version-supported");
        ObjectNode u=sample("{\"response\":{\"body\":{\"error\":{\"code\":-32022,\"data\":{\"supported\":[\"2026-07-28\"],\"requested\":\"1900-01-01\"}}}}}");
        var us=Map.<String,JsonNode>of("wire",u);check(supported,us);check(declared("T20","wire-unsupported-version","unsupported-version-requested"),us);
        ((ObjectNode)u.at("/response/body/error/data")).set("supported",Json.parse("[\"2025-11-25\",\"2026-07-28\"]"));assertThrows(AssertionError.class,()->check(supported,us));
        for(JsonNode s:Json.read(root.resolve("verification/cases/T20/case.json")).path("subcases"))for(JsonNode a:s.path("assertions"))
            assertFalse(a.path("source").path("pointer").asText().endsWith("/error/data/category"),"Project-invented error category must not be an oracle");
    }
    @Test void mrtrNegativeVariantsPinTheirOwnBindingCode() throws Exception {
        JsonNode a=declared("T20","mrtr-tampered-state","binding-code");
        ObjectNode r=sample("{\"response\":{\"body\":{\"result\":{\"structuredContent\":{\"outcome\":\"REJECTED\",\"error\":{\"code\":\"REQUEST_STATE_INTEGRITY_FAILED\"}}}}}}");
        var rs=Map.<String,JsonNode>of("continued",r);check(a,rs);
        ((ObjectNode)r.at("/response/body/result/structuredContent/error")).put("code","TYPE_INVALID");assertThrows(AssertionError.class,()->check(a,rs));
        assertEquals("REQUEST_STATE_EXPIRED",declared("T20","mrtr-expired-state","binding-code").path("expected").asText());
        assertEquals("STRUCTURED",declared("T20","mrtr-before-expiry","continued-outcome").path("expected").asText());
    }
    @Test void allowedToolsWriteNeedsAnActualServerRefusal() throws Exception {
        // The model run may decline: no assertion requires a write attempt. Any submitted write must not apply.
        for(String gone:List.of("write-attempt-observed","write-refused-by-server"))
            for(JsonNode s:Json.read(root.resolve("verification/cases/T20/case.json")).path("subcases"))
                if(s.path("id").asText().equals("host-allowed-tools-write"))
                    for(JsonNode a:s.path("assertions")) assertNotEquals(gone,a.path("id").asText(),"model restraint must not fail the subcase");
        JsonNode applied=declared("T20","host-allowed-tools-write","no-command-applied");
        var aliases=Json.object();for(String name:List.of("ORG","P","L","readAgent","B40","SL1"))aliases.put(name,"captured-"+name);
        ObjectNode probe=sample("{\"data\":{\"hostObservation\":{\"extractor\":{\"rawRows\":{\"toolCalls\":[]}}}}}");
        var rs=new java.util.HashMap<String,JsonNode>(Map.of("probe",probe));
        engine.check(applied,rs,aliases);
        ((com.fasterxml.jackson.databind.node.ArrayNode)probe.at("/data/hostObservation/extractor/rawRows/toolCalls")).add(Json.parse("{\"actorId\":\"captured-readAgent\",\"intentKind\":\"COMMAND\",\"capabilityId\":\"reserveQuantity\",\"serverOutcome\":\"APPLIED\",\"serverErrorCode\":\"NONE\"}"));
        assertThrows(AssertionError.class,()->engine.check(applied,rs,aliases));
        // The server boundary is a deterministic scripted tools/call by the same READ principal.
        ObjectNode wire=sample("{\"response\":{\"httpStatus\":200,\"body\":{\"result\":{\"isError\":true,\"structuredContent\":{\"outcome\":\"REJECTED\",\"error\":{\"code\":\"FORBIDDEN\"}}}}}}");
        rs.put("scripted-write",wire);
        for(String id:List.of("scripted-write-http","scripted-write-outcome","scripted-write-forbidden","scripted-write-is-error"))engine.check(declared("T20","host-allowed-tools-write",id),rs,aliases);
        ((ObjectNode)wire.at("/response/body/result/structuredContent")).put("outcome","APPLIED");
        assertThrows(AssertionError.class,()->engine.check(declared("T20","host-allowed-tools-write","scripted-write-outcome"),rs,aliases));
        ((ObjectNode)wire.at("/response/body/result/structuredContent/error")).put("code","TYPE_INVALID");
        assertThrows(AssertionError.class,()->engine.check(declared("T20","host-allowed-tools-write","scripted-write-forbidden"),rs,aliases));
    }
    @Test void runtimeLinksRejectNotRunAndFailedLinksAndPreparationIsAPinnedInput() throws Exception {
        JsonNode links=declared("T25","runtime-links-required","concrete-links-000");
        ObjectNode r=sample("{\"data\":{\"hostObservation\":{\"extractor\":{\"rawRows\":{\"namedObservations\":[{\"assertionLinks\":[{\"caseId\":\"T01\",\"subcaseId\":\"same-world\",\"assertionId\":\"same-snapshotRevision\",\"profile\":\"scenarios\",\"status\":\"NOT_RUN\",\"evidenceRefs\":[\"captured\"]}]}],\"assertionLinks\":[{\"status\":\"NOT_RUN\"}],\"runtimeArtifacts\":[{\"status\":\"PASS\",\"exitCode\":1}]}}}}}");
        var rs=Map.<String,JsonNode>of("coverage",r);
        assertThrows(AssertionError.class,()->check(links,rs));
        assertThrows(AssertionError.class,()->check(declared("T25","runtime-links-required","assertion-links-no-not-run"),rs));
        assertThrows(AssertionError.class,()->check(declared("T25","runtime-links-required","runtime-artifacts-no-exit1"),rs));
        JsonNode none=Json.read(root.resolve("verification/cases/T25/case.json")).path("subcases").get(0);
        assertEquals("coverage-none",none.path("id").asText());
        JsonNode params=none.path("actions").get(1).path("control").path("parameters");
        assertEquals("PREPARATION",params.path("inputSnapshotKind").asText());assertFalse(params.has("manifestPath"),"Preparation expectations must not read the live runtime manifest");
        for(JsonNode s:Json.read(root.resolve("verification/cases/T25/case.json")).path("subcases"))for(JsonNode a:s.path("actions"))
            if(a.path("control").path("parameters").path("manifestPath").asText().endsWith("runtime-manifest.json"))
                assertTrue(Set.of("REQUIRED_PATH_RUNTIME_EVIDENCE","APPROVED_MODEL_EXECUTION_EVIDENCE").contains(a.path("control").path("parameters").path("inputSnapshotKind").asText()),s.path("id").asText());
    }
    @Test void mandatoryCaseDropAndDuplicateAreRejected() throws Exception {
        JsonNode a=declared("T25","coverage-none","all-41-cases");
        JsonNode catalog=Json.read(root.resolve("verification/requirements/mandatory-oracles.json"));
        ObjectNode r=sample("{\"data\":{\"hostObservation\":{\"extractor\":{\"rawRows\":{}}}}}");
        ObjectNode rows=(ObjectNode)r.path("data").path("hostObservation").path("extractor").path("rawRows");rows.set("requiredCases",catalog.path("requiredCaseIds").deepCopy());
        var rs=Map.<String,JsonNode>of("coverage",r);check(a,rs);
        var ids=(com.fasterxml.jackson.databind.node.ArrayNode)rows.get("requiredCases");ids.remove(ids.size()-1);assertThrows(AssertionError.class,()->check(a,rs));
        ids.add("T01");assertThrows(AssertionError.class,()->check(a,rs));
    }
    @Test void notRunCannotBePromotedToPassAndUsageCannotBeZeroFilled() throws Exception {
        JsonNode a=declared("T25","model-preparation-not-runtime","model-gate-not-run");
        JsonNode b=declared("T25","model-preparation-not-runtime","no-total-cost-zero-fill");
        ObjectNode r=sample("{\"data\":{\"hostObservation\":{\"extractor\":{\"rawRows\":{\"gate\":{\"modelStatus\":\"NOT_RUN\"},\"usage\":{\"totalCostState\":\"MISSING\"}}}}}}");
        var rs=Map.<String,JsonNode>of("model-records",r);check(a,rs);check(b,rs);
        ((ObjectNode)r.path("data").path("hostObservation").path("extractor").path("rawRows").path("gate")).put("modelStatus","PASS");assertThrows(AssertionError.class,()->check(a,rs));
        ((ObjectNode)r.path("data").path("hostObservation").path("extractor").path("rawRows").path("usage")).put("totalCostState","0");assertThrows(AssertionError.class,()->check(b,rs));
    }
    @Test void unimplementedResultNeverProvesZeroEffects() throws Exception {
        JsonNode a=declared("T20","mrtr-tampered-state","unchanged-movements");
        ObjectNode r=sample("{\"data\":{\"rawRows\":{\"movements\":[]}}}");r.put("driverStatus","NOT_IMPLEMENTED");
        assertThrows(AssertionError.class,()->check(a,Map.of("db-before",r,"db-after",r)));
    }
    @Test void forbiddenOutboxEffectCannotHideBehindUnchangedPhysicalQuantity() throws Exception {
        JsonNode a=declared("T01","excluded-bankTransfer","unchanged-outbox");
        ObjectNode before=sample("{\"data\":{\"rawRows\":{\"outbox\":[]}}}");
        ObjectNode after=sample("{\"data\":{\"rawRows\":{\"outbox\":[]}}}");
        var rs=Map.<String,JsonNode>of("db-before",before,"db-after",after);check(a,rs);
        ((com.fasterxml.jackson.databind.node.ArrayNode)after.path("data").path("rawRows").path("outbox"))
            .add(Json.parse("{\"id\":\"unexpected-effect\",\"capabilityId\":\"bankTransfer\",\"externalOperationId\":\"bank-operation\"}"));
        assertThrows(AssertionError.class,()->check(a,rs));
    }
    @Test void absentCanonicalHashRequiresAnObservedParentObject() throws Exception {
        JsonNode a=declared("T20","ambiguousFriday","no-canonical-before-confirmation");
        ObjectNode r=sample("{\"response\":{\"outcome\":\"NEEDS_INPUT\"}}");
        var rs=Map.<String,JsonNode>of("structured",r);check(a,rs);
        r.putNull("response");assertThrows(AssertionError.class,()->check(a,rs));
    }
    @Test void nullableUnknownQuantityIsNeverReadAsZero() throws Exception {
        JsonNode a=declared("T01","same-world","noun-heldQuantity");
        ObjectNode r=sample("{\"response\":{\"data\":{\"heldQuantity\":null,\"unit\":\"BOX\"}}}");
        assertThrows(AssertionError.class,()->check(a,Map.of("noun",r)));
        ((ObjectNode)r.path("response").path("data")).put("heldQuantity","UNKNOWN");
        assertThrows(AssertionError.class,()->check(a,Map.of("noun",r)));
    }
    @Test void emptyAssertionLinkRowsCannotSatisfyNamedObservationCoverage() throws Exception {
        JsonNode a=declared("T25","runtime-links-required","concrete-links-000");
        ObjectNode r=sample("{\"data\":{\"hostObservation\":{\"extractor\":{\"rawRows\":{\"namedObservations\":[{\"assertionLinks\":[{\"caseId\":\"T01\",\"subcaseId\":\"same-world\",\"assertionId\":\"same-snapshotRevision\",\"profile\":\"scenarios\",\"status\":\"PASS\",\"evidenceRefs\":[\"actual-redacted-response\"]}]}]}}}}}");
        var rs=Map.<String,JsonNode>of("coverage",r);check(a,rs);
        ((com.fasterxml.jackson.databind.node.ArrayNode)r.path("data").path("hostObservation").path("extractor").path("rawRows").path("namedObservations").get(0).path("assertionLinks")).removeAll();
        assertThrows(AssertionError.class,()->check(a,rs));
    }
}
