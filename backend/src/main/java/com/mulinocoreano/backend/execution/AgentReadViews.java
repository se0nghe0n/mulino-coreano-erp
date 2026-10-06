package com.mulinocoreano.backend.execution;

import com.mulinocoreano.backend.planning.CanonicalJson;
import com.mulinocoreano.backend.planning.PlanDto;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** Version 1 decision views. Only named audit expansions are omitted; unknown facts survive. */
@Component
public class AgentReadViews {
    static final int MAX_BYTES = 16384;
    private final ObjectMapper mapper;
    private final CanonicalJson json;
    public AgentReadViews(ObjectMapper mapper, CanonicalJson json) {
        this.mapper=mapper; this.json=json;
    }

    public JsonNode plan(PlanDto full) {
        return plan((ObjectNode)mapper.valueToTree(full),null,"NOT_IN_STORED_DTO");
    }

    public JsonNode planRead(PlanDto full,String association) {
        return plan((ObjectNode)mapper.valueToTree(full),json.readTree(association),"CURRENT_SCOPED_READ");
    }

    private JsonNode plan(ObjectNode view,JsonNode association,String availability) {
        var source=view.remove("sourceSnapshot");
        String fullRead="mulino plan show "+view.path("ref").asText()+" --full";
        if(source==null || !source.path("products").isArray() || !view.path("result").isObject()
                || !List.of("READY","NEEDS_ATTENTION").contains(view.path("result").path("status").asText())
                || !view.path("result").path("issues").isArray() || !view.path("result").path("purchases").isArray())
            return unavailable(view,fullRead,"UNSUPPORTED_PLAN_VIEW_SHAPE");
        for(var product:source.path("products")) if(!product.path("item").path("id").isIntegralNumber()
                || product.path("item").path("id").asLong()<=0) return unavailable(view,fullRead,"UNSUPPORTED_PLAN_VIEW_SHAPE");
        view.set("productIds", mapper.valueToTree(java.util.stream.StreamSupport.stream(source.path("products").spliterator(),false)
                .map(p -> p.path("item").path("id").asLong()).distinct().sorted().toList()));
        view.set("planningBasis", basis(java.time.Instant.parse(view.path("asOf").asText())));
        view.put("associationAvailability",availability);
        if(association!=null) view.set("currentAssociation",association);
        var result=(ObjectNode)view.path("result");
        // All status, issue, purchase-selection and unknown result fields remain verbatim.
        if(result.path("requirements") instanceof ObjectNode requirements) {
            // Preserve future decision extensions inside this container, not only at result's top level.
            for(String key:List.of("production","materials","allocations","exclusions")) requirements.remove(key);
            if(requirements.isEmpty()) result.remove("requirements");
        } else if(result.hasNonNull("requirements")) return unavailable(view,fullRead,"UNSUPPORTED_PLAN_VIEW_SHAPE");
        if(result.path("forecasts").isObject()) result.path("forecasts").forEach(f -> {
            if(f instanceof ObjectNode forecast) { forecast.remove("dailyDemand"); forecast.remove("sourceRefs"); }
        });
        return bounded(view,fullRead,
                List.of("sourceSnapshot","result.requirements.production","result.requirements.materials","result.requirements.allocations","result.requirements.exclusions","result.forecasts.*.dailyDemand","result.forecasts.*.sourceRefs"));
    }

    public JsonNode caseView(Map<String,Object> full) {
        ObjectNode view=(ObjectNode)mapper.valueToTree(full).deepCopy();
        String ref=view.path("caseRef").asText();
        view.remove("currentBusinessFacts");
        String planRef=view.path("latestPlan").path("ref").asText("");
        if(!planRef.isEmpty()) {
            ObjectNode captured=(ObjectNode)view.path("latestPlan");
            view.set("latestPlan",plan(captured,captured.get("currentAssociation"),"CAPTURED_SCOPED_READ"));
        }
        // Keep small observations as well as identity, corrections and human judgments. Large bodies are audit expansions.
        view.path("epistemic").path("evidence").forEach(e -> {
            if(e.path("provenance") instanceof ObjectNode provenance
                    && mapper.writeValueAsBytes(provenance.path("content")).length>2048) provenance.remove("content");
        });
        return bounded(view,"mulino case show "+ref+" --full",
                List.of("currentBusinessFacts","latestPlan.sourceSnapshot","latestPlan.result.requirements.production","latestPlan.result.requirements.materials","latestPlan.result.requirements.allocations","latestPlan.result.requirements.exclusions",
                        "latestPlan.result.forecasts.*.dailyDemand","latestPlan.result.forecasts.*.sourceRefs","epistemic.evidence.*.provenance.content"));
    }

    public Map<String,Object> transport(Map<String,Object> full) {
        return mapper.readerFor(Map.class).with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readValue(caseView(full).toString());
    }

    private ObjectNode basis(java.time.Instant instant) {
        ObjectNode basis=mapper.createObjectNode();
        basis.put("asOf",instant.toString()); basis.put("date",instant.atZone(ZoneId.of("Asia/Seoul")).toLocalDate().toString());
        basis.put("timezone","Asia/Seoul"); basis.put("authority","SERVER_PLANNING_CLOCK"); return basis;
    }

    private JsonNode bounded(ObjectNode view,String fullRead,List<String> omitted) {
        view.put("viewVersion",1);view.put("complete",true);view.put("fullRead",fullRead);view.set("omitted",mapper.valueToTree(omitted));
        if(view.path("latestPlan").path("complete").isBoolean() && !view.path("latestPlan").path("complete").asBoolean())
            return unavailable(view,fullRead,view.path("latestPlan").path("reason").asText("DEPENDENT_PLAN_VIEW_UNAVAILABLE"));
        if(mapper.writeValueAsBytes(view).length<=MAX_BYTES) return view;
        return unavailable(view,fullRead,"READ_VIEW_TOO_LARGE");
    }

    private JsonNode unavailable(ObjectNode view,String fullRead,String reason) {
        ObjectNode unavailable=mapper.createObjectNode();
        unavailable.put("viewVersion",1);unavailable.put("complete",false);unavailable.put("status","UNAVAILABLE");
        unavailable.put("reason",reason);unavailable.put("fullRead",fullRead);
        unavailable.put("requiredAction","Report FAILED and request human review of the full scoped record; do not decide from incomplete facts.");
        for(String key:List.of("caseRef","ref","execution")) if(view.has(key)) unavailable.set(key,view.get(key));
        return unavailable;
    }
}
