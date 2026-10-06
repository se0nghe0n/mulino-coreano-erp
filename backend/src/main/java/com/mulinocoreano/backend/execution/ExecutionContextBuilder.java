package com.mulinocoreano.backend.execution;

import com.mulinocoreano.backend.followup.ReplenishmentFollowupRepository;
import com.mulinocoreano.backend.interfacepackage.ContextSnapshotService;
import com.mulinocoreano.backend.planning.CanonicalJson;
import com.mulinocoreano.backend.planning.PlanningSnapshotRepository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reconstructs current facts without replacing the original Run or plan audit snapshots. */
@Component
public class ExecutionContextBuilder {
    private final ReplenishmentFollowupRepository followups;
    private final ContextSnapshotService contexts;
    private final PlanningSnapshotRepository snapshots;
    private final ExecutionContextRepository repository;
    private final CanonicalJson json;
    private final Clock clock;
    private final com.mulinocoreano.backend.planning.PlanQueryRepository plans;
    private final tools.jackson.databind.ObjectMapper mapper;

    public ExecutionContextBuilder(
            ContextSnapshotService contexts,
            PlanningSnapshotRepository snapshots,
            ExecutionContextRepository repository,
            CanonicalJson json,
            @Qualifier("planningClock") Clock clock,
            ReplenishmentFollowupRepository followups,
            com.mulinocoreano.backend.planning.PlanQueryRepository plans,tools.jackson.databind.ObjectMapper mapper) {
        this.plans=plans;this.mapper=mapper;
        this.followups = followups;
        this.contexts = contexts;
        this.snapshots = snapshots;
        this.repository = repository;
        this.json = json;
        this.clock = clock;
    }

    public Map<String, Object> build(String caseRef, long caseId) {
        return build(caseRef,caseId,null);
    }

    public Map<String,Object> build(String caseRef,long caseId,String currentCoordinationWork) {
        var context = new LinkedHashMap<String, Object>(contexts.build(caseRef));
        var basisInstant=clock.instant();
        var basisDate=basisInstant.atZone(clock.getZone()).toLocalDate();
        context.put("planningBasis",Map.of("asOf",basisInstant,"date",basisDate,"timezone",clock.getZone().getId(),"authority","SERVER_PLANNING_CLOCK"));
        context.put("caseStatus",plans.caseStatus(caseRef));
        context.put("planningAttempts",json.readTree(plans.planningAttempts(caseRef)));
        context.put("caseRef", caseRef);
        context.put("recallWork",json.readTree(repository.recallWorks(caseId)));
        context.put("qualityWork",json.readTree(repository.qualityWorks(caseId)));
        context.put("followups", followups.forCase(caseId));
        context.put("caseMetadata", json.readTree(repository.caseMetadata(caseId)));
        context.put(
                "purchasing",
                json.readTree("[" + String.join(",", repository.recentPurchasing(caseId)) + "]"));
        repository
                .latestPlan(caseId)
                .ifPresent(
                        plan -> {
                            var source = json.readTree(plan.source());
                            var productIds =
                                    java.util.stream.StreamSupport.stream(
                                                    source.path("products").spliterator(), false)
                                            .map(
                                                    product ->
                                                            product.path("item")
                                                                    .path("id")
                                                                    .asLong())
                                            .distinct()
                                            .sorted()
                                            .toList();
                            var captured=(tools.jackson.databind.node.ObjectNode)mapper.valueToTree(plans.find(plan.ref()).orElseThrow());
                            captured.set("currentAssociation",json.readTree(plans.currentAssociation(plan.ref())));
                            context.put("latestPlan",captured);
                            context.put(
                                    "currentBusinessFacts",
                                    snapshots.load(
                                            plan.warehouse(),
                                            productIds,
                                            basisDate,
                                            plan.horizon()));
                        });
        var captured=mapper.valueToTree(context);
        context.put("currentCoordinationWorkItemRef",currentCoordinationWork);
        context.put("currentCoordinationPurchase",CurrentCoordinationPurchase.select(captured.path("purchasing"),captured.path("obligation"),currentCoordinationWork,captured.path("latestPlan").path("ref").asText("")));
        return context;
    }
}
