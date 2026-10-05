package com.mulinocoreano.backend.quality;

import com.mulinocoreano.backend.execution.*;
import com.mulinocoreano.backend.governance.GovernancePolicy;
import com.mulinocoreano.backend.idempotency.RequestIdempotency;
import com.mulinocoreano.backend.persistence.PlanningDataGuard;
import com.mulinocoreano.backend.planning.CanonicalJson;
import com.mulinocoreano.backend.procurement.PurchaseDecisionRequest;
import com.mulinocoreano.backend.procurement.PurchaseTransactions;
import com.mulinocoreano.backend.security.*;
import com.mulinocoreano.backend.interfacepackage.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import java.time.*;
import java.util.*;

@Service
public class QualityService {
    private final JdbcClient jdbc;
    private final CanonicalJson json;
    private final RequestIdempotency keys;
    private final PlanningDataGuard guard;
    private final PurchaseTransactions transactions;
    private final GovernancePolicy policy;
    private final RunCapabilityAccess capabilities;
    private final RunExecutionService execution;
    private final DispatcherService dispatcher;
    private final Clock clock;
    private final RunService runService;
    public QualityService(JdbcClient jdbc, CanonicalJson json, RequestIdempotency keys, PlanningDataGuard guard,
            PurchaseTransactions transactions, GovernancePolicy policy, RunCapabilityAccess capabilities,
            RunExecutionService execution, DispatcherService dispatcher, @Qualifier("planningClock") Clock clock, RunService runService) {
        this.runService=runService;
        this.jdbc=jdbc; this.json=json; this.keys=keys; this.guard=guard; this.transactions=transactions;
        this.policy=policy; this.capabilities=capabilities; this.execution=execution; this.dispatcher=dispatcher; this.clock=clock;
    }
    public record ProductionInput(long productionRecordId,long rawMaterialLotId,java.math.BigDecimal quantity) {}
    public JsonNode produce(ProductionInput input,ErpActor actor,String key) {
        return transactions.execute(()-> {
            guard.lock();
            if(!(actor instanceof HumanActor human) || !human.capabilities().contains("work:write") ||
                !jdbc.sql("SELECT is_active AND role IN ('OPERATOR','MANAGER') FROM users WHERE user_id=:id FOR UPDATE").param("id",human.userId()).query(Boolean.class).optional().orElse(false))
                throw new org.springframework.security.access.AccessDeniedException("Production operator required");
            return keys.executeCanonicalJson("quality.production:"+human.userId(),key,input,()-> {
                if(input.quantity()==null || input.quantity().signum()<=0 || input.quantity().stripTrailingZeros().scale()>6 || input.quantity().precision()-input.quantity().scale()>12) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                long inboundId=jdbc.sql("SELECT inbound_id FROM raw_material_lots WHERE raw_material_lot_id=:lot").param("lot",input.rawMaterialLotId()).query(Long.class).optional().orElseThrow(()->missing());
                if(!snapshot(inboundId).path("eligible").asBoolean() || !jdbc.sql("SELECT expiry_date IS NOT NULL AND expiry_date>=:today FROM raw_material_lots WHERE raw_material_lot_id=:lot")
                    .param("today",LocalDate.now(clock)).param("lot",input.rawMaterialLotId()).query(Boolean.class).single())
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"Raw material LOT is unavailable");
                var before=jdbc.sql("SELECT remaining_quantity FROM raw_material_lots WHERE raw_material_lot_id=:lot").param("lot",input.rawMaterialLotId()).query(java.math.BigDecimal.class).single();
                long id=jdbc.sql("INSERT INTO production_ingredients(production_record_id,raw_material_lot_id,quantity_used) VALUES (:record,:lot,:quantity) RETURNING production_ingredient_id")
                    .param("record",input.productionRecordId()).param("lot",input.rawMaterialLotId()).param("quantity",input.quantity()).query(Long.class).single();
                var after=jdbc.sql("SELECT remaining_quantity FROM raw_material_lots WHERE raw_material_lot_id=:lot").param("lot",input.rawMaterialLotId()).query(java.math.BigDecimal.class).single();
                jdbc.sql("INSERT INTO governance_audit_logs(actor_id,event_type,resource_type,resource_id,before_state,after_state) VALUES (:actor,'PRODUCTION_INPUT_RECORDED','PRODUCTION_INGREDIENT',:id,:before::jsonb,:after::jsonb)")
                    .param("actor",human.userId()).param("id",id)
                    .param("before",json.write(Map.of("input",input,"remainingQuantity",before,"actor",human)))
                    .param("after",json.write(Map.of("input",input,"productionIngredientId",id,"remainingQuantity",after,"actor",human))).update();
                return Map.of("productionIngredientId",id,"remainingQuantity",after);
            });
        });
    }
    public record InspectRequest(String caseRef) {}
    public JsonNode inspection(long actionId) {
        return tree(jdbc.sql("""
            SELECT jsonb_build_object('approvalId',a.governance_action_id,'status',a.status,'requiredRole',a.required_role,
                'version',q.version,'proposalHash',q.proposal_hash,'proposal',q.snapshot,'proposedStatus',q.proposed_status,
                'inboundId',q.inbound_id,'caseRef',c.case_ref,'workItemRef',w.work_item_ref,
                'decision',(SELECT to_jsonb(d) FROM governance_decisions d WHERE d.governance_action_id=a.governance_action_id AND d.is_final))::text
            FROM inbound_inspections q JOIN governance_actions a USING(governance_action_id)
            JOIN cases c ON c.case_id=q.case_id JOIN work_items w ON w.work_item_id=q.work_item_id
            WHERE a.governance_action_id=:id
            """).param("id",actionId).query(String.class).optional().orElseThrow(()->missing()));
    }
    public JsonNode inbound(long id) { return snapshot(id); }
    @org.springframework.transaction.annotation.Transactional
    public JsonNode agentInbound(long id,String token,AgentActor actor) {
        var scope=capabilities.requireLocked(token,"QC",actor.caseRef());
        if(!jdbc.sql("SELECT (metadata->>'inboundId')::bigint=:id FROM work_items WHERE work_item_id=:work")
                .param("id",id).param("work",scope.workItemId()).query(Boolean.class).optional().orElse(false))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return snapshot(id);
    }
    private JsonNode snapshot(long id) {
        return tree(jdbc.sql("""
            SELECT jsonb_build_object('receipt',to_jsonb(i),'asOf',:today::date,
              'declaration',(SELECT to_jsonb(d) FROM material_quality_declarations d WHERE d.raw_material_id=i.raw_material_id),
              'allergenMaster',(SELECT coalesce(jsonb_agg(to_jsonb(a) ORDER BY allergen_id),'[]') FROM allergens a WHERE standard='KR_MFDS'),
              'allergenMapping',(SELECT coalesce(jsonb_agg(to_jsonb(m) ORDER BY raw_material_allergen_id),'[]') FROM raw_material_allergens m WHERE m.raw_material_id=i.raw_material_id),
              'certificates',(SELECT coalesce(jsonb_agg(to_jsonb(s) ORDER BY supplier_certification_id),'[]') FROM supplier_certifications s WHERE s.supplier_id=i.supplier_id),
              'temperatures',(SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY inbound_temperature_id),'[]') FROM inbound_temperature_logs t WHERE t.inbound_id=i.inbound_id),
              'reasons',to_jsonb(inbound_quality_reasons(i.inbound_id,:today::date)),
              'eligible',inbound_quality_eligible(i.inbound_id,:today::date),
              'certificateNotice',EXISTS(SELECT 1 FROM supplier_certifications s WHERE s.supplier_id=i.supplier_id AND s.cert_type='HACCP' AND s.expiry_date BETWEEN :today::date AND :today::date+30))::text
            FROM inbound i WHERE inbound_id=:id
            """).param("id",id).param("today",LocalDate.now(clock)).query(String.class).optional().orElseThrow(()->missing()));
    }
    @org.springframework.transaction.annotation.Transactional
    public JsonNode assign(long inboundId,InspectRequest request,ErpActor actor,String key) {
        guard.lock();
        if(!(actor instanceof HumanActor human) || !human.capabilities().contains("work:write")
          || !jdbc.sql("SELECT is_active AND role IN ('OPERATOR','MANAGER') FROM users WHERE user_id=:id FOR UPDATE").param("id",human.userId()).query(Boolean.class).optional().orElse(false))
            throw new org.springframework.security.access.AccessDeniedException("Operator delegation required");
        return keys.executeCanonicalJson("quality.assign:"+human.userId(),key,Map.of("inboundId",inboundId,"request",request),()-> {
            snapshot(inboundId);
            long caseId=jdbc.sql("SELECT case_id FROM cases WHERE case_ref=:ref AND status::text NOT IN ('RESOLVED','CLOSED','CANCELLED') FOR UPDATE").param("ref",request.caseRef()).query(Long.class).optional().orElseThrow(()->missing());
            long agentId=jdbc.sql("SELECT agent_id FROM agents WHERE agent_key='QC' AND is_active FOR SHARE").query(Long.class).single();
            String ref="WI-"+UUID.randomUUID().toString().substring(0,12);
            jdbc.sql("INSERT INTO work_items(work_item_ref,case_id,title,assigned_agent_id,metadata) VALUES (:ref,:case,'입고 검사',:agent,jsonb_build_object('inboundId',:id))")
                .param("ref",ref).param("case",caseId).param("agent",agentId).param("id",inboundId).update();
            jdbc.sql("INSERT INTO case_participants(case_id,actor_type,agent_id) VALUES (:case,'AGENT',:agent) ON CONFLICT DO NOTHING").param("case",caseId).param("agent",agentId).update();
            var run=runService.createRun(new CreateRunRequest("QC",request.caseRef(),ref,runService.defaultRuntime()),null);
            if(!"QUEUED".equals(run.status())) throw new ResponseStatusException(HttpStatus.CONFLICT,"QC context could not be queued");
            return Map.of("status","QUEUED","workItemRef",ref,"runRef",run.runRef());
        });
    }
    public JsonNode inspect(long inboundId, InspectRequest request, ErpActor actor, String token, String key) {
        if (request.caseRef()==null || request.caseRef().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return transactions.execute(()-> {
            String namespace="quality.inspect:"+actor.subject(); keys.coordinate(namespace,key); guard.lock();
            var source=snapshot(inboundId);
            keys.coordinate("planning.warehouse",source.path("receipt").path("warehouse_id").asText());
            long caseId=jdbc.sql("SELECT case_id FROM cases WHERE case_ref=:ref AND status::text NOT IN ('CLOSED','RESOLVED','CANCELLED') FOR UPDATE")
                .param("ref",request.caseRef()).query(Long.class).optional().orElseThrow(()->missing());
            RunCapabilityAccess.RunScope scope=null;
            if(actor instanceof AgentActor agent) {
                scope=capabilities.requireLocked(token,"QC",request.caseRef());
                if(!jdbc.sql("SELECT (metadata->>'inboundId')::bigint=:id FROM work_items WHERE work_item_id=:work")
                    .param("id",inboundId).param("work",scope.workItemId()).query(Boolean.class).optional().orElse(false))
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Receipt is outside the assigned QC work");
            } else if(actor instanceof HumanActor human) {
                if(!human.capabilities().contains("work:write") || !jdbc.sql("SELECT is_active AND role IN ('OPERATOR','MANAGER') FROM users WHERE user_id=:id FOR UPDATE").param("id",human.userId()).query(Boolean.class).optional().orElse(false))
                    throw new org.springframework.security.access.AccessDeniedException("Active operator required");
            } else throw new org.springframework.security.access.AccessDeniedException("Proposal identity required");
            final var runScope=scope;
            return keys.executeCanonicalJson(namespace,key,Map.of("inboundId",inboundId,"request",request),()->prepare(inboundId,caseId,request.caseRef(),actor,runScope,source));
        });
    }
    private Object prepare(long id,long caseId,String caseRef,ErpActor actor,RunCapabilityAccess.RunScope scope,JsonNode source) {
        if(jdbc.sql("SELECT count(*) FROM inbound_inspections q JOIN governance_actions a USING(governance_action_id) WHERE inbound_id=:id AND a.status='PENDING'").param("id",id).query(Long.class).single()>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Receipt already awaits a QC decision");
        long agentId=jdbc.sql("SELECT agent_id FROM agents WHERE agent_key='QC' AND is_active FOR UPDATE").query(Long.class).single();
        long workId=scope==null ? jdbc.sql("""
            INSERT INTO work_items(work_item_ref,case_id,title,description,status,assigned_agent_id,metadata)
            VALUES (:ref,:case,'입고 품질 결정과 후속 조치','반품·폐기 또는 출고 가능한 입고인지 QC가 판단한다','WAITING',:agent,jsonb_build_object('inboundId',:id)) RETURNING work_item_id
            """).param("ref","WI-"+UUID.randomUUID().toString().substring(0,12)).param("case",caseId).param("agent",agentId).param("id",id).query(Long.class).single() : scope.workItemId();
        int version=jdbc.sql("SELECT coalesce(max(version),0)+1 FROM inbound_inspections WHERE inbound_id=:id").param("id",id).query(Integer.class).single();
        // Eligibility is a derived projection and changes when this proposal is inserted. The
        // immutable decision source excludes that projection while retaining all ERP facts.
        var facts=source.deepCopy(); ((tools.jackson.databind.node.ObjectNode)facts).remove("eligible");
        String hash=json.sha256(facts); String target=source.path("reasons").isEmpty()?"RELEASED":"BLOCKED";
        long action=jdbc.sql("""
            INSERT INTO governance_actions(requested_by,action_type,resource_type,resource_id,payload,required_role)
            VALUES ((SELECT user_id FROM users WHERE email='quality-service@mulino.internal'),'INBOUND_STATUS','INBOUND_INSPECTION',:id,:payload::jsonb,'QC') RETURNING governance_action_id
            """).param("id",id).param("payload",facts.toString()).query(Long.class).single();
        jdbc.sql("""
            INSERT INTO inbound_inspections(inbound_id,governance_action_id,case_id,work_item_id,proposed_by_agent_id,proposed_by_user_id,version,proposal_hash,snapshot,proposed_status)
            VALUES (:id,:action,:case,:work,:agent,:user,:version,:hash,:snapshot::jsonb,:target::inbound_status)
            """).param("id",id).param("action",action).param("case",caseId).param("work",workId)
            .param("agent",actor instanceof AgentActor?agentId:null).param("user",actor instanceof HumanActor h?h.userId():null)
            .param("version",version).param("hash",hash).param("snapshot",facts.toString()).param("target",target).update();
        jdbc.sql("""
            INSERT INTO attention_requests(case_id,work_item_id,reason_type,title,question,consequence,suggested_scope,requested_by_agent_id,governance_action_id)
            VALUES (:case,:work,'AUTHORITY_REQUIRED','QC 입고 품질 승인 필요',:question,'결정 전 생산 투입을 금지한다','THIS_ACTION',:agent,:action)
            """).param("case",caseId).param("work",workId).param("question","QC 승인안 "+action+": "+target+" / "+source.path("reasons")).param("agent",actor instanceof AgentActor?agentId:null).param("action",action).update();
        if(source.path("certificateNotice").asBoolean()) jdbc.sql("""
            INSERT INTO attention_requests(case_id,work_item_id,reason_type,title,question,consequence,suggested_scope)
            VALUES (:case,:work,'MATERIAL_EXCEPTION','HACCP 만료 30일 전 통지','인증 갱신 증빙을 확보해 주세요','만료 후 입고·생산이 차단된다','THIS_CASE')
            """).param("case",caseId).param("work",workId).update();
        for(var reason:source.path("reasons")) jdbc.sql("INSERT INTO alert_events(inbound_id,alert_type,severity,observed_value) VALUES (:id,:type,'WARNING',:source::jsonb)")
            .param("id",id).param("type",reason.asText()).param("source",facts.toString()).update();
        audit(action,null,"QUALITY_PROPOSED",id,Map.of("source",facts,"proposer",actor));
        if(scope!=null) execution.awaitQualityApproval(scope.runId(),action,"QC 입고 결정을 기다립니다.");
        else jdbc.sql("UPDATE cases SET status='WAITING' WHERE case_id=:id").param("id",caseId).update();
        return Map.of("status","PENDING_APPROVAL","approvalId",action,"version",version,"proposalHash",hash,
            "proposedStatus",target,"executionResult",Map.of("outcome","WAITING","summary","QC 승인 필요","waitingConditions",List.of(),"resultRef","APPROVAL-"+action));
    }
    public JsonNode decide(long action,PurchaseDecisionRequest request,ErpActor actor,String key) {
        return transactions.execute(()-> {
            guard.lock();
            var human=policy.requireHuman(actor,GovernancePolicy.Action.INBOUND_STATUS);
            keys.coordinate("quality.decide:"+human.userId(),key);
            var approval=inspection(action); long inboundId=approval.path("inboundId").asLong();
            keys.coordinate("planning.warehouse",snapshot(inboundId).path("receipt").path("warehouse_id").asText());
            jdbc.sql("SELECT governance_action_id FROM governance_actions WHERE governance_action_id=:id FOR UPDATE").param("id",action).query(Long.class).single();
            return keys.executeCanonicalJson("quality.decide:"+human.userId(),key,Map.of("approvalId",action,"request",request),()->apply(action,request,human));
        });
    }
    private Object apply(long action,PurchaseDecisionRequest request,HumanActor human) {
        var approval=inspection(action);
        if(!"PENDING".equals(approval.path("status").asText())) return Map.of("error","ALREADY_DECIDED");
        if(request.expectedVersion()!=approval.path("version").asInt() || !request.proposalHash().equals(approval.path("proposalHash").asText())) return Map.of("error","PROPOSAL_VERSION_MISMATCH");
        var responsibility=jdbc.sql("SELECT c.status::text AS case_status,w.status::text AS work_status FROM inbound_inspections q JOIN cases c ON c.case_id=q.case_id JOIN work_items w ON w.work_item_id=q.work_item_id AND w.case_id=q.case_id WHERE q.governance_action_id=:id FOR UPDATE OF c,w")
            .param("id",action).query().singleRow();
        if(Set.of("CLOSED","RESOLVED").contains(responsibility.get("case_status")) || Set.of("DONE","CANCELLED").contains(responsibility.get("work_status"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Quality responsibility is no longer active");
        long id=approval.path("inboundId").asLong(); var current=snapshot(id).deepCopy(); ((tools.jackson.databind.node.ObjectNode)current).remove("eligible");
        String outcome=switch(request.decision()) {case "APPROVE"->"APPROVED";case "BLOCK"->"BLOCKED";default->"CANCELLED";};
        if("APPROVE".equals(request.decision()) && !json.sha256(current).equals(request.proposalHash())) {
            jdbc.sql("UPDATE governance_actions SET status='EXPIRED' WHERE governance_action_id=:id").param("id",action).update();
            audit(action,human.userId(),"QUALITY_EXPIRED",id,Map.of("current",current,"proposal",approval));
            jdbc.sql("UPDATE attention_requests SET status='EXPIRED',resolved_by_user_id=:user,answer_text='새 자료로 재검사 필요',resolved_at=CURRENT_TIMESTAMP,version=version+1 WHERE governance_action_id=:action AND status='OPEN'").param("user",human.userId()).param("action",action).update();
            jdbc.sql("UPDATE waiting_conditions SET status='EXPIRED',resolved_at=CURRENT_TIMESTAMP WHERE condition_type='APPROVAL' AND condition_payload->>'approval_id'=:action AND status='ACTIVE'").param("action",Long.toString(action)).update();
            jdbc.sql("INSERT INTO attention_requests(case_id,work_item_id,reason_type,title,question,consequence,suggested_scope) SELECT case_id,work_item_id,'JUDGMENT_REQUIRED','입고 재검사 필요','변경된 증빙으로 다시 검사할지 명시적으로 지시해 주세요','품질 barrier는 계속 유지된다','THIS_ACTION' FROM inbound_inspections WHERE governance_action_id=:id").param("id",action).update();
            // A general human answer can resume the assigned QC Run with current facts.
            return Map.of("error","PROPOSAL_EXPIRED","approvalId",action);
        }
        if("APPROVE".equals(request.decision())) {
            if("RELEASED".equals(approval.path("proposedStatus").asText()) && !current.path("reasons").isEmpty()) return Map.of("error","UNSAFE_RELEASE");
            jdbc.sql("UPDATE inbound SET status=:status::inbound_status,status_reason=:reason,status_decided_by=:user,status_decided_at=CURRENT_TIMESTAMP WHERE inbound_id=:id")
                .param("status",approval.path("proposedStatus").asText()).param("reason",request.reason()).param("user",human.userId()).param("id",id).update();
        }
        jdbc.sql("INSERT INTO governance_decisions(governance_action_id,decided_by,decision,reason,is_final) VALUES (:id,:user,:decision::governance_decision_type,:reason,true)")
            .param("id",action).param("user",human.userId()).param("decision",request.decision()).param("reason",request.reason()).update();
        jdbc.sql("UPDATE governance_actions SET status=:status::governance_action_status WHERE governance_action_id=:id").param("status",outcome).param("id",action).update();
        jdbc.sql("UPDATE attention_requests SET status='ANSWERED',resolved_by_user_id=:user,answer_text=:reason,answer_scope='THIS_ACTION',resolved_at=CURRENT_TIMESTAMP,version=version+1 WHERE governance_action_id=:id AND status='OPEN'")
            .param("user",human.userId()).param("reason",request.reason()).param("id",action).update();
        audit(action,human.userId(),"QUALITY_DECIDED",id,Map.of("approval",approval,"decision",request,"after",snapshot(id)));
        var event=dispatcher.ingest(new CreateEventRequest("QUALITY_DECISION_RECORDED","quality-decision-"+action,approval.path("caseRef").asText(),null,Map.of("approvalId",Long.toString(action),"qualityDecision",outcome)));
        jdbc.sql("UPDATE waiting_conditions SET status='SATISFIED',resolved_at=CURRENT_TIMESTAMP,resolved_by_event_id=:event WHERE condition_type='APPROVAL' AND condition_payload->>'approval_id'=:action AND status='ACTIVE'")
            .param("event",event.eventId()).param("action",Long.toString(action)).update();
        // Decision completion is durable; rejected unsafe stock keeps its derived barrier.
        jdbc.sql("UPDATE work_items SET status='DONE',resolved_at=CURRENT_TIMESTAMP WHERE work_item_ref=:ref").param("ref",approval.path("workItemRef").asText()).update();
        dispatcher.ingest(new CreateEventRequest("WORK_ITEM_STATUS_CHANGED","quality-work-done-"+action,approval.path("caseRef").asText(),approval.path("workItemRef").asText(),Map.of("status","DONE")));
        if(!"RELEASED".equals(snapshot(id).path("receipt").path("status").asText()) || !"APPROVE".equals(request.decision())) {
            long caseId=jdbc.sql("SELECT case_id FROM cases WHERE case_ref=:ref").param("ref",approval.path("caseRef").asText()).query(Long.class).single();
            long followup=jdbc.sql("INSERT INTO work_items(work_item_ref,case_id,title,description,status,assigned_user_id,metadata) VALUES (:ref,:case,'입고 반품·폐기 판단','검사 결정과 안전 자료를 검토하여 반품·폐기 조치안을 확정한다','WAITING',:user,jsonb_build_object('inboundId',:id,'qualityActionId',:action)) RETURNING work_item_id")
                .param("ref","WI-"+UUID.randomUUID().toString().substring(0,12)).param("case",caseId).param("user",human.userId()).param("id",id).param("action",action).query(Long.class).single();
            jdbc.sql("INSERT INTO attention_requests(case_id,work_item_id,reason_type,title,question,consequence,suggested_scope) VALUES (:case,:work,'JUDGMENT_REQUIRED','반품·폐기 조치 판단 필요','QC가 차단 원인과 반품 또는 폐기 조치안을 검토해 주세요','원재료 LOT은 생산에 사용할 수 없다','THIS_ACTION')")
                .param("case",caseId).param("work",followup).update();
        }
        return inspection(action);
    }
    private void audit(long action,Long user,String event,long inbound,Object state) {
        jdbc.sql("INSERT INTO governance_audit_logs(governance_action_id,actor_id,event_type,resource_type,resource_id,after_state) VALUES (:action,:user,:event,'INBOUND',:id,:state::jsonb)")
            .param("action",action).param("user",user).param("event",event).param("id",inbound).param("state",json.write(state)).update();
    }
    private JsonNode tree(String value) { return json.readTree(value); }
    private static ResponseStatusException missing(){ return new ResponseStatusException(HttpStatus.NOT_FOUND); }
}
