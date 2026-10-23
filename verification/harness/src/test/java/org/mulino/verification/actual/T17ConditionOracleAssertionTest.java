package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mulino.verification.*;
import static org.junit.jupiter.api.Assertions.*;

/** Controlled assertion-engine witnesses, not product or observer runtime acceptance. */
class T17ConditionOracleAssertionTest {
    final Path root=Path.of(System.getProperty("repo.root"));
    final ObjectNode aliases=Json.object();
    T17ConditionOracleAssertionTest(){for(String a:List.of("ORG","P","A","B","EARLY","U"))aliases.put(a,UUID.nameUUIDFromBytes(a.getBytes(StandardCharsets.UTF_8)).toString());}
    JsonNode assertion(String id)throws Exception {for(var s:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases"))if("eligibility-conflict".equals(s.path("id").asText()))for(var a:s.path("assertions"))if(id.equals(a.path("id").asText()))return a;throw new AssertionError(id);}
    ArrayNode golden(){
        var rows=Json.array();
        for(String segment:List.of("A","B","EARLY","U"))for(String kind:List.of("QC","REGULATORY","LABEL","DISPOSITION","CUSTOMER_CONTRACT")){
            String state=segment.equals("B")&&kind.equals("REGULATORY")?"CONFLICT":segment.equals("U")&&kind.equals("REGULATORY")?"UNKNOWN":segment.equals("EARLY")&&kind.equals("QC")?"DENIED":"ALLOWED";
            var row=Json.object().put("segmentId",aliases.path(segment).asText()).put("conditionId",kind).put("status",state).put("evaluatorVersion","quality-eligibility-v1");
            var refs=Json.array();if(!state.equals("UNKNOWN")){refs.add(segment+"-"+kind+"-real-source-1");if(state.equals("CONFLICT")||state.equals("DENIED"))refs.add(segment+"-"+kind+"-real-source-2");}row.set("evidenceRefs",refs);rows.add(row);
        }
        var sorted=new ArrayList<JsonNode>();rows.forEach(sorted::add);sorted.sort(Comparator.comparing(n->n.path("segmentId").asText()));var out=Json.array();sorted.forEach(out::add);return out;
    }
    ObjectNode envelope(){return Json.object().put("driverStatus","EXECUTED").set("provenance",Json.object().put("scopeComplete",true));}
    Map<String,JsonNode> results(ArrayNode product,ArrayNode source){
        var p=envelope();p.set("response",Json.object().set("data",Json.object().set("conditionResults",product)));
        var db=envelope();db.set("data",Json.object().set("rawRows",Json.object().set("sellConditionSourceRefs",source)));
        return Map.of("eligibility",p,"after-reserve-db",db);
    }
    void check(String id,ArrayNode p,ArrayNode db)throws Exception{new AssertionEngine().check(assertion(id),results(p,db),aliases);}
    @Test void originalFixedConflictUnknownDeniedAndFiveConditionsRemainRequired()throws Exception {
        check("eligibility-response-1",golden(),golden());
        for(String semantic:List.of("QC","REGULATORY","LABEL","DISPOSITION","CUSTOMER_CONTRACT")){
            var missing=golden();for(int i=0;i<missing.size();i++)if(semantic.equals(missing.get(i).path("conditionId").asText())){missing.remove(i);break;}
            assertThrows(AssertionError.class,()->check("eligibility-response-1",missing,golden()));
        }
        for(String target:List.of("CONFLICT","UNKNOWN","DENIED")){
            var wrong=golden();for(var r:wrong)if(target.equals(r.path("status").asText()))((ObjectNode)r).put("status","ALLOWED");
            assertThrows(AssertionError.class,()->check("eligibility-response-1",wrong,golden()));
        }
        var wrongVersion=golden();((ObjectNode)wrongVersion.get(0)).put("evaluatorVersion","evaluator-v1");assertThrows(AssertionError.class,()->check("eligibility-response-1",wrongVersion,golden()));
        var duplicate=golden();duplicate.add(duplicate.get(0).deepCopy());assertThrows(AssertionError.class,()->check("eligibility-response-1",duplicate,golden()));
    }
    @Test void independentEvidenceComparisonPreservesZeroOneAndMultipleRefsAndRejectsMissingHoldOrAlias()throws Exception {
        check("eligibility-response-2",golden(),golden());
        // The source has no business status/evaluator fields. Only identities are compared.
        var source=golden();for(var row:source)((ObjectNode)row).remove(List.of("status","evaluatorVersion"));check("eligibility-response-2",golden(),source);
        for(String mismatch:List.of("missing-hold","fixture-alias","invented-positive","foreign-source")){
            var wrong=golden();
            for(var row:wrong){var refs=(ArrayNode)row.path("evidenceRefs");
                if(mismatch.equals("missing-hold")&&"DENIED".equals(row.path("status").asText()))refs.remove(1);
                if(mismatch.equals("fixture-alias")&&"QC".equals(row.path("conditionId").asText())){refs.removeAll();refs.add("qc-proof");}
                if(mismatch.equals("invented-positive")&&"UNKNOWN".equals(row.path("status").asText()))refs.add("agency-proof");
                if(mismatch.equals("foreign-source")&&"CONFLICT".equals(row.path("status").asText()))refs.set(1,Json.MAPPER.valueToTree("foreign-canonical"));
            }
            assertThrows(AssertionError.class,()->check("eligibility-response-2",wrong,source));
        }
    }
    @Test void featureDeclaresEveryActionInOrderAndAll434Assertions()throws Exception {
        Path path=root.resolve("verification/cases/T17/case.json");var data=Json.read(path);
        assertEquals(List.of(),new PreparationValidator(root).feature(path,data));
        int count=0,original=0;for(var sub:data.path("subcases"))for(var a:sub.path("assertions")){count++;if(!a.path("id").asText().startsWith("read-quantity-")&&!a.path("id").asText().startsWith("response-scope-"))original++;}assertEquals(434,count);assertEquals(301,original);
    }
    @Test void conflictRequiresTwoDistinctDeclaredSourceAliasesAndOriginalProvenance()throws Exception {
        var f=(ObjectNode)Json.read(root.resolve("verification/cases/T17/fixtures/eligibility-conflict.json"));T17SourceFacts.validate(f);
        var conflict=(ObjectNode)f.at("/baseline/conditions/6");conflict.set("evidenceAliases",Json.array().add("agency-proof").add("agency-proof"));assertThrows(IllegalArgumentException.class,()->T17SourceFacts.validate(f));
        conflict.set("evidenceAliases",Json.array().add("agency-proof").add("missing-proof"));assertThrows(IllegalArgumentException.class,()->T17SourceFacts.validate(f));
        conflict.set("evidenceAliases",Json.array().add("agency-proof").add("claim-proof"));
        var originalEvidence=Json.array();for(var evidence:f.path("evidence"))if(!"claim-proof".equals(evidence.path("alias").asText()))originalEvidence.add(evidence);f.set("evidence",originalEvidence);
        assertThrows(IllegalArgumentException.class,()->T17SourceFacts.validate(f));
    }
}
