package com.mulinocoreano.backend.execution;

import static com.mulinocoreano.backend.generated.Tables.*;

import static org.jooq.impl.DSL.*;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Reads current execution facts without changing stored Run or plan evidence. */
@Repository
public class ExecutionContextRepository {
    private final DSLContext dsl;

    public ExecutionContextRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public String recallWorks(long caseId) {
        return dsl.fetchOne("SELECT coalesce(jsonb_agg(jsonb_build_object('workItemRef',work_item_ref,'lotId',metadata->'lotId','status',status) ORDER BY work_item_id),'[]')::text FROM work_items w JOIN agents a ON a.agent_id=w.assigned_agent_id WHERE case_id=? AND a.agent_key='QC' AND metadata->'lotId' IS NOT NULL",caseId).get(0,String.class);
    }
    public String qualityWorks(long caseId) {
        return dsl.fetchOne("SELECT coalesce(jsonb_agg(jsonb_build_object('workItemRef',work_item_ref,'inboundId',metadata->'inboundId','status',status) ORDER BY work_item_id),'[]')::text FROM work_items w JOIN agents a ON a.agent_id=w.assigned_agent_id WHERE case_id=? AND a.agent_key='QC' AND metadata->'inboundId' IS NOT NULL",caseId).get(0,String.class);
    }
    public String caseMetadata(long caseId) {
        return dsl.select(coalesce(CASES.METADATA, JSONB.valueOf("{}")))
                .from(CASES)
                .where(CASES.CASE_ID.eq(caseId))
                .fetchSingle()
                .value1()
                .data();
    }

    public List<String> recentPurchasing(long caseId) {
        var g = GOVERNANCE_ACTIONS;
        var p = REPLENISHMENT_PLANS;
        var w = WORK_ITEMS;
        var a = PURCHASE_APPLICATIONS;
        var po = PURCHASE_ORDERS;
        var orders =
                select(jsonbArrayAgg(po.PURCHASE_ORDER_ID).orderBy(po.PURCHASE_ORDER_ID))
                        .from(po)
                        .where(po.PURCHASE_APPLICATION_ID.eq(a.PURCHASE_APPLICATION_ID));
        var item =
                jsonbObject(
                        key("approvalId").value(g.GOVERNANCE_ACTION_ID),
                        key("status").value(g.STATUS),
                        key("version").value(g.PROPOSAL_VERSION),
                        key("proposalHash").value(g.PROPOSAL_HASH),
                        key("planRef").value(p.PLAN_REF),
                        key("workItemRef").value(w.WORK_ITEM_REF),
                        key("parentWorkItemRef").value(field("{0}->>'parentWorkItemRef'",String.class,w.METADATA)),
                        key("finalGovDecision").value(field("""
                            (SELECT jsonb_build_object(
                                'decisionId',d.governance_decision_id,'decision',d.decision,
                                'isFinal',d.is_final,'userId',d.decided_by,'decidedAt',d.decided_at,
                                'actorRole',audit.after_state->'actor'->>'role',
                                'actorProvenance',CASE WHEN audit.governance_audit_log_id IS NULL THEN 'UNKNOWN' ELSE 'IMMUTABLE_PURCHASE_DECIDED_AUDIT' END,
                                'actionId',{0},'version',{1},'proposalHash',{2},'planRef',{3})
                             FROM governance_decisions d
                             LEFT JOIN LATERAL (
                                SELECT l.governance_audit_log_id,l.after_state FROM governance_audit_logs l
                                WHERE l.governance_action_id=d.governance_action_id
                                  AND l.actor_id=d.decided_by AND l.event_type='PURCHASE_DECIDED'
                                  AND l.resource_type='GOVERNANCE_ACTION' AND l.resource_id=d.governance_action_id
                                  AND l.after_state->>'governanceDecisionId'=d.governance_decision_id::text
                                  AND l.after_state->>'decision'=d.decision::text
                                  AND l.after_state->>'expectedVersion'={1}::text
                                  AND l.after_state->>'proposalHash'={2}
                                  AND l.after_state->'actor'->>'userId'=d.decided_by::text
                                  AND l.after_state->'actor'->>'role' IN ('MANAGER','OPERATOR','QC','ADMIN')
                                ORDER BY l.governance_audit_log_id LIMIT 1
                             ) audit ON true
                             WHERE d.governance_action_id={0} AND d.is_final)
                            """,JSONB.class,g.GOVERNANCE_ACTION_ID,g.PROPOSAL_VERSION,g.PROPOSAL_HASH,p.PLAN_REF)),
                        key("applicationId").value(a.PURCHASE_APPLICATION_ID),
                        key("purchaseOrderIds")
                                .value(coalesce(orders.asField(), val(JSONB.valueOf("[]")))));
        return dsl.select(item)
                .from(g)
                .join(p)
                .on(p.REPLENISHMENT_PLAN_ID.eq(g.REPLENISHMENT_PLAN_ID))
                .join(w)
                .on(w.WORK_ITEM_ID.eq(g.WORK_ITEM_ID))
                .leftJoin(a)
                .on(a.GOVERNANCE_ACTION_ID.eq(g.GOVERNANCE_ACTION_ID))
                .where(g.CASE_ID.eq(caseId))
                .orderBy(g.GOVERNANCE_ACTION_ID.desc())
                .limit(10)
                .fetch(record -> record.value1().data());
    }

    public boolean hasAppliedPurchase(long caseId, long orderId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(PURCHASE_ORDERS)
                        .join(PURCHASE_APPLICATIONS)
                        .on(
                                PURCHASE_APPLICATIONS.PURCHASE_APPLICATION_ID.eq(
                                        PURCHASE_ORDERS.PURCHASE_APPLICATION_ID))
                        .where(
                                PURCHASE_ORDERS
                                        .PURCHASE_ORDER_ID
                                        .eq(orderId)
                                        .and(PURCHASE_APPLICATIONS.CASE_ID.eq(caseId))));
    }

    public Optional<PriorPlan> latestPlan(long caseId) {
        var p = REPLENISHMENT_PLANS;
        return dsl.select(
                        p.PLAN_REF,
                        p.VERSION,
                        p.WAREHOUSE_ID,
                        p.HORIZON_DAYS,
                        p.SOURCE_SNAPSHOT,
                        p.RESULT)
                .from(p)
                .where(p.CASE_ID.eq(caseId))
                .orderBy(p.VERSION.desc())
                .limit(1)
                .fetchOptional(
                        r ->
                                new PriorPlan(
                                        r.value1(),
                                        r.value2(),
                                        r.value3(),
                                        r.value4(),
                                        r.value5().data(),
                                        r.value6().data()));
    }

    public record PriorPlan(
            String ref, int version, long warehouse, int horizon, String source, String result) {}
}
