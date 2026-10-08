package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class CatalogLinkValidatorTest {
    @TempDir Path root;
    private ContractValidator validator;
    private ObjectNode catalog,caseNode;
    private static final String ORACLE="T11.dependency-and-followup",OBS="shared-distinct-contribution";
    @BeforeEach void setup() throws Exception {
        Json.write(root.resolve("contracts/acceptance-capabilities.json"),Json.object().set("capabilities",Json.array()));
        Files.createDirectories(root.resolve("docs"));Files.writeString(root.resolve("docs/normative.md"),"shared contribution60 BOX\n");
        validator=new ContractValidator(root);
        ObjectNode observation=Json.object().put("name",OBS).put("type","quantity").put("operator","eq");
        observation.set("expected",Json.object().put("value","60").put("unit","BOX"));
        ObjectNode oracle=Json.object().put("oracleId",ORACLE).put("caseId","T11");oracle.set("expectedObservations",Json.array().add(observation));
        catalog=Json.object();catalog.set("oracles",Json.array().add(oracle));
        catalog.set("sourceFiles",Json.array().add(Json.object().put("path","docs/normative.md").put("sha256",Json.sha256(root.resolve("docs/normative.md")))));
        caseNode=Json.object().put("caseId","T11");ObjectNode sub=Json.object().put("id","shared-distinct60");
        sub.set("assertions",Json.array().add(primary()).add(partition()));caseNode.set("subcases",Json.array().add(sub));
    }
    private static ObjectNode source(String field) {
        return Json.object().put("actionId","after-db").put("pointer","/data/rawRows/contributions").put("field",field);
    }
    private static ObjectNode link(ObjectNode assertion) {
        ObjectNode ref=Json.object().put("oracleId",ORACLE);ref.set("observationNames",Json.array().add(OBS));assertion.set("oracleRef",ref);return assertion;
    }
    private static ObjectNode primary() {
        ObjectNode a=link(Json.object().put("id","shared-sum60").put("op","sumEquals").put("expected","60").put("unit","BOX"));
        a.set("source",source("quantity"));a.set("unitSource",source("unit"));return a;
    }
    private static ObjectNode partition() {
        ObjectNode a=link(Json.object().put("id","shared-scope-partition").put("op","relationSet"));
        ObjectNode tupleSource=source("physicalScope");tupleSource.set("field",Json.array().add("workId").add("quantity").add("physicalScope"));
        a.set("source",tupleSource);a.set("expected",Json.array()
            .add(Json.array().add(Json.parse("{\"$result\":{\"actionId\":\"parent-one\",\"pointer\":\"/response/workId\"}}")).add("30").add("serial-boxes-001-030"))
            .add(Json.array().add(Json.parse("{\"$result\":{\"actionId\":\"parent-two\",\"pointer\":\"/response/workId\"}}")).add("30").add("serial-boxes-031-060")));
        return a;
    }
    private ArrayNode assertions() {return (ArrayNode)caseNode.path("subcases").get(0).path("assertions");}
    private ObjectNode first() {return (ObjectNode)assertions().get(0);}
    private ObjectNode observation() {return (ObjectNode)catalog.path("oracles").get(0).path("expectedObservations").get(0);}
    private List<String> check() throws Exception {
        List<String> errors=new ArrayList<>();CatalogLinkValidator.validate(validator,catalog,Map.of(caseNode.path("caseId").asText(),caseNode),errors);return errors;
    }
    @Test void primaryAggregateAndSupportingIdentityPartitionShareQuantityObservation() throws Exception {assertEquals(List.of(),check());}
    @Test void otherFixedQuantityAspectsMaySupportTheSameAggregate() throws Exception {
        assertions().add(primary().put("id","parent-one30").put("expected","30"));assertEquals(List.of(),check());
    }
    @Test void exactDecimalNormalizationPreservesIndependentConstant() throws Exception {first().put("expected","60.000");assertEquals(List.of(),check());}
    @Test void upperBoundUsesItsOwnOperatorWithoutDemandingExactQuantity() throws Exception {
        observation().put("operator","lte");first().put("op","decimalAtMost");assertEquals(List.of(),check());
    }
    @Test void lowerBoundUsesItsOwnOperatorWithoutDemandingExactQuantity() throws Exception {
        observation().put("operator","gte");first().put("op","decimalAtLeast");assertEquals(List.of(),check());
    }
    @Test void supportingCountCannotReplacePrimaryEvenIfItsNumberIsSixty() throws Exception {
        assertions().remove(0);assertions().add(link(Json.object().put("op","count").put("expected",60)));assertFalse(check().isEmpty());
    }
    @Test void missingPrimaryLeavesIdentityOnlyCoverageInvalid() throws Exception {assertions().remove(0);assertFalse(check().isEmpty());}
    @Test void allSupportingAssertionsCanRemainWhenPrimaryExists() throws Exception {
        ObjectNode count=link(Json.object().put("id","count").put("op","count").put("expected",2));count.set("source",source("quantity"));assertions().add(count);
        ObjectNode present=link(Json.object().put("id","present").put("op","present").put("expected",true));present.set("source",source("quantity"));assertions().add(present);
        ObjectNode identities=link(Json.object().put("id","identities").put("op","exactSet"));identities.set("source",source("workId"));
        identities.set("expected",Json.array().add("work-one").add("work-two"));assertions().add(identities);assertEquals(List.of(),check());
    }
    @Test void quantityAndPartitionAssertionsExecuteAgainstTheSameRows() throws Exception {
        ObjectNode rows=Json.object();rows.set("contributions",Json.array()
            .add(Json.object().put("workId","work-one").put("quantity","30").put("unit","BOX").put("physicalScope","serial-boxes-001-030"))
            .add(Json.object().put("workId","work-two").put("quantity","30").put("unit","BOX").put("physicalScope","serial-boxes-031-060")));
        ObjectNode db=Json.object().put("driverStatus","EXECUTED");db.set("provenance",Json.object().put("scopeComplete",true));db.set("data",Json.object().set("rawRows",rows));
        Map<String,JsonNode> results=Map.of("after-db",db,
            "parent-one",identityResult("work-one"),"parent-two",identityResult("work-two"));
        for(JsonNode assertion:assertions()) assertDoesNotThrow(()->new AssertionEngine().check(assertion,results));
        ((ObjectNode)rows.path("contributions").get(1)).put("physicalScope","serial-boxes-001-030");
        assertThrows(AssertionError.class,()->new AssertionEngine().check(assertions().get(1),results));
        assertEquals(List.of(),check()); // Linkage permits support; execution independently detects bad partition.
    }
    private static ObjectNode identityResult(String workId) {
        ObjectNode result=Json.object().put("driverStatus","EXECUTED");result.set("provenance",Json.object().put("scopeComplete",true));
        result.set("response",Json.object().put("workId",workId));return result;
    }
    @TestFactory Stream<DynamicTest> missingOrWeakQuantityPrimary() {
        Map<String,java.util.function.Consumer<ObjectNode>> mutations=new LinkedHashMap<>();
        mutations.put("wrong fixed value",a->a.put("expected","59"));
        mutations.put("wrong unit",a->a.put("unit","EA"));
        mutations.put("missing observed unit",a->a.remove("unitSource"));
        mutations.put("no actual source",a->a.remove("source"));
        mutations.put("self-copy business result",a->a.set("expected",Json.parse("{\"$result\":{\"actionId\":\"after-db\",\"pointer\":\"/data/quantity\"}}")));
        mutations.put("boolean true",a->a.put("expected",true));
        mutations.put("floating JSON number",a->a.put("expected",60.0));
        mutations.put("count with quantity metadata",a->a.put("op","count"));
        mutations.put("presence only",a->a.put("op","present"));
        mutations.put("identity equality only",a->a.put("op","equals"));
        mutations.put("wrong upper comparison",a->a.put("op","decimalAtMost"));
        mutations.put("delta lacks baseline units",a->a.put("op","decimalDelta"));
        return mutations.entrySet().stream().map(e->DynamicTest.dynamicTest(e.getKey(),()->{
            setup();e.getValue().accept(first());assertTrue(check().stream().anyMatch(p->p.contains("Missing substantive fixed quantity assertion")),e.getKey());
        }));
    }
    @Test void exactCannotStandInForRequiredUpperBound() throws Exception {observation().put("operator","lte");assertFalse(check().isEmpty());}
    @Test void oppositeBoundCannotStandInForRequiredLowerBound() throws Exception {observation().put("operator","gte");first().put("op","decimalAtMost");assertFalse(check().isEmpty());}
    @Test void separateNamedObservationsEachNeedTheirOwnFixedPrimary() throws Exception {
        ObjectNode second=observation().deepCopy().put("name","other-contribution");((ObjectNode)second.path("expected")).put("value","40");
        ((ArrayNode)catalog.path("oracles").get(0).path("expectedObservations")).add(second);
        ((ArrayNode)first().path("oracleRef").path("observationNames")).add("other-contribution");
        assertFalse(check().isEmpty());ObjectNode a=primary().put("id","other40").put("expected","40");
        ((ObjectNode)a.path("oracleRef")).set("observationNames",Json.array().add("other-contribution"));assertions().add(a);assertEquals(List.of(),check());
    }
    @Test void unknownOracleIsRejected() throws Exception {((ObjectNode)first().path("oracleRef")).put("oracleId","T11.unknown");assertTrue(check().stream().anyMatch(s->s.contains("Unknown independent oracle")));}
    @Test void unknownObservationIsRejected() throws Exception {((ObjectNode)first().path("oracleRef")).set("observationNames",Json.array().add("unknown"));assertTrue(check().stream().anyMatch(s->s.contains("Unknown oracle observation")));}
    @Test void differentCaseCannotClaimCoverage() throws Exception {caseNode.put("caseId","T10");assertTrue(check().stream().anyMatch(s->s.contains("different case")));}
    @Test void sourceHashDriftStillFails() throws Exception {Files.writeString(root.resolve("docs/normative.md"),"changed quantity120\n");assertTrue(check().stream().anyMatch(s->s.contains("Normative source hash drift")));}
    @Test void decimalDeltaNeedsIndependentQuantityAndUnitBaselines() throws Exception {
        first().put("op","decimalDelta");first().set("baseline",source("quantity").put("actionId","before-db"));
        first().set("baselineUnitSource",source("unit").put("actionId","before-db"));assertEquals(List.of(),check());
    }
    @Test void sameObservationCannotBeItsOwnZeroDeltaBaseline() throws Exception {
        ((ObjectNode)observation().path("expected")).put("value","0");first().put("op","decimalDelta").put("expected","0");
        first().set("baseline",source("quantity"));first().set("baselineUnitSource",source("unit"));assertFalse(check().isEmpty());
    }
    private List<String> gaps() throws Exception {
        List<String> errors=new ArrayList<>(),gaps=new ArrayList<>();CatalogLinkValidator.validate(validator,catalog,Map.of(caseNode.path("caseId").asText(),caseNode),errors,gaps);return gaps;
    }
    private void actions(String... kinds) {
        ((ObjectNode)caseNode.path("subcases").get(0)).set("requiredAdapters",Json.array().add("api").add("db").add("fixture"));
        ArrayNode actions=Json.array();for(String k:kinds) {String[] p=k.split(":");actions.add(Json.object().put("id",p[0]).put("kind",p[1]));}
        ((ObjectNode)caseNode.path("subcases").get(0)).set("actions",actions);
    }
    @Test void dbSnapshotArtifactKindNeedsAnIndependentObserveInALinkedSubcase() throws Exception {
        observation().set("artifactKinds",Json.array().add("api_response").add("db_snapshot"));
        actions("after-db:observe");assertEquals(List.of(),check());
        // The same assertions read from an API query: the API projection alone cannot satisfy db_snapshot.
        actions("after-db:query");
        assertTrue(check().stream().anyMatch(p->p.contains("requires db_snapshot") && p.contains(ORACLE+"/"+OBS)),check().toString());
        observation().set("artifactKinds",Json.array().add("api_response"));assertEquals(List.of(),check());
    }
    @Test void dbSnapshotWithoutAnyDbAdapterIsCatalogLayerGapNotDoubleGated() throws Exception {
        observation().set("artifactKinds",Json.array().add("db_snapshot"));actions("after-db:query");
        ((ObjectNode)caseNode.path("subcases").get(0)).set("requiredAdapters",Json.array().add("coverage").add("host"));
        assertEquals(List.of(),check());assertEquals(1,gaps().size());
        ((ObjectNode)caseNode.path("subcases").get(0)).set("requiredAdapters",Json.array().add("db"));
        assertEquals(1,check().size());assertEquals(List.of(),gaps());
    }
    @Test void siblingObservationObservedInSameSubcaseSharesTheDbSnapshot() throws Exception {
        ObjectNode sibling=Json.object().put("name","sibling-db").put("type","state").put("operator","eq");sibling.set("artifactKinds",Json.array().add("db_snapshot"));
        ((ArrayNode)catalog.path("oracles").get(0).path("expectedObservations")).add(sibling);
        observation().set("artifactKinds",Json.array().add("db_snapshot"));
        actions("after-db:query","raw:observe");
        ObjectNode observed=Json.object().put("id","sibling-row").put("op","count").put("expected",1);observed.set("source",Json.object().put("actionId","raw").put("pointer","/data/rawRows/contributions"));
        ObjectNode ref=Json.object().put("oracleId",ORACLE);ref.set("observationNames",Json.array().add("sibling-db"));observed.set("oracleRef",ref);
        assertions().add(observed);assertEquals(List.of(),check());
        assertions().remove(assertions().size()-1);
        assertTrue(check().stream().anyMatch(p->p.contains("requires db_snapshot")));
    }
}
