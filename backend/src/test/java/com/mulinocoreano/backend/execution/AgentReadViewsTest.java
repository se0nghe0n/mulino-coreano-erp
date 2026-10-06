package com.mulinocoreano.backend.execution;

import static org.assertj.core.api.Assertions.assertThat;
import com.mulinocoreano.backend.planning.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Goals 3/4/5: models see authoritative decisions while complete sources remain immutable. */
class AgentReadViewsTest {
    final ObjectMapper mapper=new ObjectMapper();
    final CanonicalJson json=new CanonicalJson(mapper);
    final AgentReadViews views=new AgentReadViews(mapper,json);

    PlanDto plan(String status) {
        var source=mapper.createObjectNode();source.set("products",json.readTree("[{\"item\":{\"id\":9007199254740993}}]"));source.put("history","x".repeat(100000));
        var result=json.readTree("""
                {"status":"READY","issues":[],"totalAmount":16500,"purchases":[{"material":{"id":1},"selection":{"chosen":{"purchaseQuantity":999999999999.999999,"totalAmount":16500,"warnings":[{"code":"CERT_EXPIRING"}]},"rejectedCandidates":[{"supplierId":2,"reasons":["EXPIRED"]}]}}],"requirements":{"materials":[]},"forecasts":{"1":{"dailyMean":2.000001,"dailyDemand":[],"sourceRefs":[]}},"futureDecision":{"reviewRequired":true}}
                """);
        ((tools.jackson.databind.node.ObjectNode)result).put("status",status);
        if(!"READY".equals(status)) { ((tools.jackson.databind.node.ObjectNode)result).putNull("totalAmount");((tools.jackson.databind.node.ObjectNode)result).set("issues",json.readTree("[{\"code\":\"NO_ELIGIBLE_SUPPLIER\",\"resourceRef\":\"raw_materials:1\"}]")); }
        return new PlanDto("PLAN-1","CASE-1",1,1,Instant.parse("2026-09-05T00:00:00Z"),30,LocalDate.parse("2026-10-04"),source,result,"source-hash","plan-hash",null);
    }

    @Test void largeReadyPlanKeepsExactSelectedQuantitiesWarningsAndTotalsWithoutChangingFullEvidence() {
        var full=plan("READY");((tools.jackson.databind.node.ObjectNode)full.result().path("requirements")).put("futureHoldRequired",true);
        String before=json.write(full);var view=views.plan(full);
        assertThat(view.path("complete").asBoolean()).isTrue();
        assertThat(view.path("result").path("status").asText()).isEqualTo("READY");
        assertThat(view.path("result").path("totalAmount").decimalValue()).isEqualByComparingTo("16500");
        assertThat(view.path("result").path("purchases").get(0).path("selection").path("chosen").path("purchaseQuantity").decimalValue()).isEqualByComparingTo("999999999999.999999");
        assertThat(view.path("productIds").get(0).asLong()).isEqualTo(9007199254740993L);
        assertThat(view.path("result").path("purchases")).isEqualTo(full.result().path("purchases"));
        assertThat(view.path("result").path("futureDecision").path("reviewRequired").asBoolean()).isTrue();
        assertThat(view.path("result").path("requirements").path("futureHoldRequired").asBoolean()).isTrue();
        assertThat(view.path("planningBasis").path("date").asText()).isEqualTo("2026-09-05");
        assertThat(view.path("hash").asText()).isEqualTo("plan-hash");
        assertThat(json.write(view).getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isLessThan(16384);
        assertThat(json.write(full)).isEqualTo(before);
    }

    @Test void needsAttentionRetainsIssuesAndUnknownTotalInsteadOfImplyingReady() {
        var view=views.plan(plan("NEEDS_ATTENTION"));
        assertThat(view.path("result").path("status").asText()).isEqualTo("NEEDS_ATTENTION");
        assertThat(view.path("result").path("issues").get(0).path("code").asText()).isEqualTo("NO_ELIGIBLE_SUPPLIER");
        assertThat(view.path("result").path("totalAmount").isNull()).isTrue();
    }

    @Test void compactCasePreservesDependenciesHumanProvenanceAndExactMetadataWhileAuditRetainsContent() {
        var full=new LinkedHashMap<String,Object>();full.put("caseRef","CASE-1");
        full.put("caseStatus","OPEN");full.put("planningAttempts",List.of());
        full.put("planningBasis",Map.of("date","2026-09-05","timezone","Asia/Seoul"));
        var captured=(tools.jackson.databind.node.ObjectNode)mapper.valueToTree(plan("READY"));
        captured.set("currentAssociation",json.readTree("{\"originWorkItemRef\":\"WI-1\",\"latestAttemptOutcome\":\"READY\",\"isLatestPlanForOriginWork\":true}"));
        full.put("latestPlan",captured);
        full.put("obligation",List.of(Map.of("ref","WI-1","dependencies",List.of("WI-2"),"status","WAITING")));
        full.put("epistemic",Map.of("decisions",List.of(Map.of("decided_by",Map.of("user_id",9007199254740993L),"metadata",Map.of("sourceAttentionId",4),"scope","THIS_CASE")),
                "evidence",List.of(Map.of("ref","EV-1","content_hash","hash","provenance",Map.of("content","x".repeat(100000),"observed_at","2026-09-05")))));
        full.put("futurePolicy",Map.of("limit",new BigDecimal("999999999999.999999")));
        var before=json.write(full);var compact=views.transport(full);
        assertThat(compact.get("obligation")).isEqualTo(full.get("obligation"));
        var node=json.readTree(json.write(compact));
        assertThat(node.path("epistemic").path("decisions").get(0).path("decided_by").path("user_id").asLong()).isEqualTo(9007199254740993L);
        assertThat(node.path("epistemic").path("evidence").get(0).path("content_hash").asText()).isEqualTo("hash");
        assertThat(node.path("futurePolicy").path("limit").decimalValue()).isEqualByComparingTo("999999999999.999999");
        assertThat(node.path("planningBasis").path("date").asText()).isEqualTo("2026-09-05");
        assertThat(node.path("latestPlan").path("currentAssociation").path("originWorkItemRef").asText()).isEqualTo("WI-1");
        assertThat(node.path("latestPlan").path("result").path("totalAmount").decimalValue()).isEqualByComparingTo("16500");
        assertThat(node.path("latestPlan").path("associationAvailability").asText()).isEqualTo("CAPTURED_SCOPED_READ");
        assertThat(json.write(full)).isEqualTo(before);
    }

    @Test void oversizedDecisionArraysAreExplicitlyUnavailableAndNeverPartiallyReady() {
        var full=plan("READY");((tools.jackson.databind.node.ObjectNode)full.result()).put("futureDecision","x".repeat(20000));
        var view=views.plan(full);
        assertThat(view.path("complete").asBoolean()).isFalse();
        assertThat(view.path("status").asText()).isEqualTo("UNAVAILABLE");
        assertThat(view.has("result")).isFalse();
        assertThat(full.result().path("status").asText()).isEqualTo("READY");
    }

    @Test void unsupportedStoredShapeCannotInventAReadyScope() {
        var full=plan("READY");((tools.jackson.databind.node.ObjectNode)full.sourceSnapshot()).remove("products");
        var view=views.plan(full);
        assertThat(view.path("complete").asBoolean()).isFalse();
        assertThat(view.has("result")).isFalse();
        assertThat(full.result().path("totalAmount").decimalValue()).isEqualByComparingTo("16500");
    }
}
