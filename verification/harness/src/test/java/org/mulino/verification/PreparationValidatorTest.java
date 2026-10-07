package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class PreparationValidatorTest {
    @TempDir Path root;
    private PreparationValidator validator;
    private Path casePath;
    private ObjectNode caseNode;
    private static final String CASE="verification/cases/HARNESS-MINIMAL/case.json";

    @BeforeEach void setup() throws Exception {
        casePath=root.resolve(CASE);Files.createDirectories(casePath.getParent());
        caseNode=Json.object().put("caseId","HARNESS-MINIMAL");
        caseNode.set("subcases",Json.array().add(sub("one")).add(sub("two")));
        Json.write(casePath,caseNode);Files.writeString(casePath.resolveSibling("scenario.feature"),feature());
        validator=new PreparationValidator(root);
    }
    private static ObjectNode sub(String id) {
        ObjectNode sub=Json.object().put("id",id);
        sub.set("actions",Json.array().add(Json.object().put("id","setup").put("kind","installFixture"))
            .add(Json.object().put("id","read").put("kind","query").put("actorRef","qc")));
        sub.set("assertions",Json.array().add(Json.object().put("id","quantity")).add(Json.object().put("id","owner")));
        return sub;
    }
    private static String scenario(String id) {
        return """
          시나리오: %s
            먼저 사례 파일 "%s"의 "%s"를 준비한다
            만일 "시스템" 역할이 "setup" 행동을 수행한다
            만일 "qc" 역할이 "read" 행동을 수행한다
            그러면 "quantity" assertion으로 "수량"를 확인한다
            그리고 "owner" assertion으로 "담당"를 확인한다
        """.formatted(id,CASE,id);
    }
    private static String feature() {return "# language: ko\n기능: 준비 계약\n"+scenario("one")+scenario("two");}
    private List<String> check(String feature) throws Exception {
        Files.writeString(casePath.resolveSibling("scenario.feature"),feature);return validator.feature(casePath,caseNode);
    }
    @Test void minimalValidScenarioMapping() throws Exception {assertEquals(List.of(),check(feature()));}
    @Test void completeRealHarnessExampleIsValid() throws Exception {
        Path repo=Path.of(System.getProperty("repo.root"));
        Path example=repo.resolve("verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/case.json");
        JsonNode c=new ContractValidator(repo).caseFile(example);
        assertEquals(List.of(),new PreparationValidator(repo).feature(example,c));
    }
    @Test void backgroundAndOutlineAreExpandedToIndependentSubcases() throws Exception {
        String outline="""
            # language: ko
            기능: 확장 계약
              배경:
                먼저 사례 파일 "%s"의 "<sub>"를 준비한다
                만일 "시스템" 역할이 "setup" 행동을 수행한다
              시나리오 개요: <sub>
                만일 "qc" 역할이 "read" 행동을 수행한다
                그러면 "quantity" assertion으로 "수량"를 확인한다
                그리고 "owner" assertion으로 "담당"를 확인한다
                예:
                  | sub |
                  | one |
                  | two |
            """.formatted(CASE);
        // Placeholders in Background are not substituted by Gherkin; the scenario must prepare its own subcase.
        assertFalse(check(outline).isEmpty());
        String expanded="""
            # language: ko
            기능: 확장 계약
              배경:
                먼저 사례 파일 "%s"의 "one"를 준비한다
                만일 "시스템" 역할이 "setup" 행동을 수행한다
              시나리오: one
                만일 "qc" 역할이 "read" 행동을 수행한다
                그러면 "quantity" assertion으로 "수량"를 확인한다
                그리고 "owner" assertion으로 "담당"를 확인한다
            """.formatted(CASE);
        ((ArrayNode)caseNode.path("subcases")).remove(1);
        assertEquals(List.of(),check(expanded));
        caseNode.set("subcases",Json.array().add(sub("one")).add(sub("two")));
        String noBackground=outline.replace("  배경:\n","  시나리오 개요: <sub>\n").replace("  시나리오 개요: <sub>\n    만일 \"qc\"","    만일 \"qc\"");
        assertEquals(List.of(),check(noBackground));
    }
    @Test void assertionsMayBeReorderedButMembershipMustMatch() throws Exception {
        assertEquals(List.of(),check(feature().replace("그러면 \"quantity\" assertion으로 \"수량\"를 확인한다\n    그리고 \"owner\" assertion으로 \"담당\"를 확인한다", "그러면 \"owner\" assertion으로 \"담당\"를 확인한다\n    그리고 \"quantity\" assertion으로 \"수량\"를 확인한다")));
    }
    @TestFactory Stream<DynamicTest> scenarioCounterexamples() {
        String first=scenario("one"),second=scenario("two"),prefix="# language: ko\n기능: 준비 계약\n";
        Map<String,String> bad=new LinkedHashMap<>();
        bad.put("missing action hidden by other scenario",prefix+first.replace("    만일 \"qc\" 역할이 \"read\" 행동을 수행한다\n","")+second);
        bad.put("missing assertion hidden by other scenario",prefix+first.replace("    그리고 \"owner\" assertion으로 \"담당\"를 확인한다\n","")+second);
        bad.put("wrong actor",feature().replace("\"qc\" 역할이","\"warehouse\" 역할이"));
        bad.put("wrong system actor",feature().replace("\"시스템\" 역할이","\"qc\" 역할이"));
        bad.put("reversed actions",feature().replace("\"시스템\" 역할이 \"setup\"","\"qc\" 역할이 \"read\"").replace("\"qc\" 역할이 \"read\" 행동을 수행한다\n    그러면","\"시스템\" 역할이 \"setup\" 행동을 수행한다\n    그러면"));
        bad.put("duplicate action",prefix+first.replace("    그러면", "    만일 \"qc\" 역할이 \"read\" 행동을 수행한다\n    그러면")+second);
        bad.put("duplicate assertion",prefix+first.replace("    그리고", "    그러면 \"quantity\" assertion으로 \"수량\"를 확인한다\n    그리고")+second);
        bad.put("duplicate subcase",prefix+first+first);
        bad.put("unknown subcase",feature().replace("의 \"one\"","의 \"other\""));
        bad.put("missing subcase",prefix+first);
        bad.put("wrong case path",feature().replace(CASE,"verification/cases/OTHER/case.json"));
        bad.put("second preparation",prefix+first.replace("    만일 \"시스템\"", "    먼저 사례 파일 \""+CASE+"\"의 \"one\"를 준비한다\n    만일 \"시스템\"")+second);
        bad.put("unknown step",prefix+first+second+"    그러면 알 수 없는 단계를 수행한다\n");
        bad.put("unknown assertion",feature().replace("\"owner\" assertion", "\"other\" assertion"));
        bad.put("action after assertion",prefix+first.replace("    만일 \"qc\" 역할이 \"read\" 행동을 수행한다\n", "").replace("    그리고", "    만일 \"qc\" 역할이 \"read\" 행동을 수행한다\n    그러면")+second);
        bad.put("step type mismatch",feature().replace("만일 \"qc\"", "그러면 \"qc\""));
        bad.put("empty discovery","# language: ko\n기능: 빈 기능\n");
        bad.put("syntax error","# language: ko\n기능: 오류\n 시나리오 개요: invalid\n 예:\n |a|\n |b|c|\n");
        bad.put("ids only in comments",prefix+"# "+first.replace("\n","\n# "));
        bad.put("extra table argument",feature().replace("    만일 \"qc\"", "      | extraneous |\n    만일 \"qc\""));
        return bad.entrySet().stream().map(e->DynamicTest.dynamicTest(e.getKey(),()->assertFalse(check(e.getValue()).isEmpty(),e.getKey())));
    }
    private ObjectNode registry() {
        ObjectNode entry=Json.object().put("caseId","HARNESS-MINIMAL").put("path",CASE).put("feature",CASE.replace("case.json","scenario.feature"));
        entry.set("subcaseIds",Json.array().add("one").add("two"));
        ObjectNode registry=Json.object().put("expectedCases",1).put("expectedSubcases",2);registry.set("cases",Json.array().add(entry));return registry;
    }
    private List<String> registryCheck(JsonNode r) {return validator.registry(r,Map.of("HARNESS-MINIMAL",caseNode),Map.of("HARNESS-MINIMAL",casePath),Set.of("HARNESS-MINIMAL"));}
    @Test void minimalRegistryIsValidWithoutFortyOneProductCases() {assertEquals(List.of(),registryCheck(registry()));}
    @TestFactory Stream<DynamicTest> registryCounterexamples() {
        Map<String,ObjectNode> bad=new LinkedHashMap<>();ObjectNode r;
        r=registry();((ArrayNode)r.path("cases")).add(r.path("cases").get(0).deepCopy());bad.put("duplicate case entry",r);
        r=registry();((ObjectNode)r.path("cases").get(0)).put("caseId","OTHER");bad.put("missing and extra ID same count",r);
        r=registry();((ObjectNode)r.path("cases").get(0)).put("path","missing.json");bad.put("missing path",r);
        r=registry();((ObjectNode)r.path("cases").get(0)).put("feature",CASE);bad.put("feature points to case",r);
        r=registry();((ObjectNode)r.path("cases").get(0)).put("path",CASE.replace("case.json","scenario.feature"));bad.put("case points to feature",r);
        r=registry();((ObjectNode)r.path("cases").get(0)).put("path","../outside.json");bad.put("outside root",r);
        r=registry();((ArrayNode)r.path("cases").get(0).path("subcaseIds")).add("one");bad.put("duplicate subcase entry",r);
        r=registry();((ArrayNode)r.path("cases").get(0).path("subcaseIds")).set(1,Json.MAPPER.getNodeFactory().textNode("other"));bad.put("missing and extra subcase",r);
        r=registry();r.put("expectedSubcases",1);bad.put("wrong count",r);
        r=registry();r.put("expectedCases","1");bad.put("string count",r);
        return bad.entrySet().stream().map(e->DynamicTest.dynamicTest(e.getKey(),()->assertFalse(registryCheck(e.getValue()).isEmpty(),e.getKey())));
    }
    @Test void duplicateEntryCannotReplaceAnotherCaseAtUnchangedCounts() throws Exception {
        Path second=root.resolve("verification/cases/HARNESS-SECOND/case.json");
        Files.createDirectories(second.getParent());
        ObjectNode secondNode=caseNode.deepCopy().put("caseId","HARNESS-SECOND");
        Json.write(second,secondNode);Files.writeString(second.resolveSibling("scenario.feature"),feature());
        ObjectNode r=registry().put("expectedCases",2).put("expectedSubcases",4);
        ((ArrayNode)r.path("cases")).add(r.path("cases").get(0).deepCopy());
        List<String> errors=validator.registry(r,Map.of("HARNESS-MINIMAL",caseNode,"HARNESS-SECOND",secondNode),
            Map.of("HARNESS-MINIMAL",casePath,"HARNESS-SECOND",second),Set.of("HARNESS-MINIMAL","HARNESS-SECOND"));
        assertTrue(errors.stream().anyMatch(e->e.contains("Duplicate registry caseId")));
        assertTrue(errors.stream().anyMatch(e->e.contains("Registry case membership mismatch")));
        assertFalse(errors.stream().anyMatch(e->e.contains("count differs")));
    }
    @Test void wrongExistingPreparedCaseAndMissingFeatureAreRejected() throws Exception {
        Json.write(root.resolve("other.json"),caseNode);
        assertTrue(check(feature().replace(CASE,"other.json")).stream().anyMatch(e->e.contains("Preparation case path mismatch")));
        Files.delete(casePath.resolveSibling("scenario.feature"));
        assertFalse(validator.feature(casePath,caseNode).isEmpty());
    }
    @Test void sameExistingWrongFileAndOutsideSymlinkAreRejected() throws Exception {
        Path other=root.resolve("other.json");Json.write(other,caseNode);
        ObjectNode r=registry();((ObjectNode)r.path("cases").get(0)).put("path","other.json");assertFalse(registryCheck(r).isEmpty());
        Path outside=Files.createTempFile(root.getParent(),"preparation-outside-",".json");
        try {
            Files.createSymbolicLink(root.resolve("outside-link.json"),outside);
            r=registry();((ObjectNode)r.path("cases").get(0)).put("path","outside-link.json");assertFalse(registryCheck(r).isEmpty());
            assertFalse(check(feature().replace(CASE,"outside-link.json")).isEmpty());
        } finally {Files.deleteIfExists(outside);}
    }
    @Test void duplicateOutlineRowsCannotHideMissingSubcase() throws Exception {
        String outline="""
            # language: ko
            기능: 중복 예제
              시나리오 개요: <sub>
                먼저 사례 파일 "%s"의 "<sub>"를 준비한다
                만일 "시스템" 역할이 "setup" 행동을 수행한다
                만일 "qc" 역할이 "read" 행동을 수행한다
                그러면 "quantity" assertion으로 "수량"를 확인한다
                그리고 "owner" assertion으로 "담당"를 확인한다
                예:
                  | sub |
                  | one |
                  | one |
            """.formatted(CASE);
        assertFalse(check(outline).isEmpty());
    }
}
