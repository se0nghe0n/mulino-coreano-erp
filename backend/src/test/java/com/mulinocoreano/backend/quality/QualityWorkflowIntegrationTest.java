package com.mulinocoreano.backend.quality;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.mulinocoreano.backend.planning.ReplenishmentDemoFixture;
import com.mulinocoreano.backend.procurement.PurchaseDecisionRequest;
import com.mulinocoreano.backend.security.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.*;
import java.time.*;
import java.util.*;

/** Goals 1,2,6: safe release, source freshness, immutable evidence and exact input balance. */
@SpringBootTest(properties={"spring.flyway.schemas=quality_workflow_it","spring.flyway.clean-disabled=false","spring.datasource.hikari.schema=quality_workflow_it","spring.flyway.init-sqls=CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public","spring.main.allow-bean-definition-overriding=true"})
@ActiveProfiles("local") @AutoConfigureMockMvc
class QualityWorkflowIntegrationTest {
    @Autowired JdbcClient jdbc; @Autowired Flyway flyway; @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;
    @Autowired com.mulinocoreano.backend.planning.PlanningSnapshotRepository snapshots;
    @Autowired QualityService service; @Autowired LocalActorDirectory actors;
    long inbound,lot,record; String caseRef;
    @TestConfiguration static class Time {
        @Bean("planningClock") @Primary Clock clock(){return Clock.fixed(Instant.parse("2026-09-05T00:00:00Z"),ZoneId.of("Asia/Seoul"));}
    }
    @BeforeEach void fixture() throws Exception {
        flyway.clean();flyway.migrate();ReplenishmentDemoFixture.load(jdbc);
        inbound=id("SELECT inbound_id FROM raw_material_lots WHERE lot_number='DEMO-RM-FLOUR-HOLD'");
        lot=id("SELECT raw_material_lot_id FROM raw_material_lots WHERE lot_number='DEMO-RM-FLOUR-HOLD'");
        record=id("INSERT INTO production_records(lot_id,warehouse_id,operator_id,process_type,start_time) SELECT production_lot_id,warehouse_id,(SELECT user_id FROM users WHERE email='replenishment-demo@example.invalid'),'MIX','2026-09-05 10:00:00' FROM production_lots WHERE lot_number='DEMO-DOUGH-CURRENT' RETURNING production_record_id");
        jdbc.sql("UPDATE production_lots SET production_date='2026-09-05' WHERE lot_number='DEMO-DOUGH-CURRENT'").update();
        caseRef=mapper.readTree(mvc.perform(post("/api/v1/cases").header("X-Mulino-Local-Role","OPERATOR").header("Idempotency-Key","qc-case").contentType("application/json").content("{\"objective\":\"입고 안전 검사\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("caseRef").asText();
    }
    private long id(String sql){return jdbc.sql(sql).query(Long.class).single();}
    private JsonNode propose(){return service.inspect(inbound,new QualityService.InspectRequest(caseRef),actors.humanForRole("OPERATOR"),null,"inspect");}
    private PurchaseDecisionRequest decision(JsonNode p,String choice){return new PurchaseDecisionRequest(choice,p.path("version").asInt(),p.path("proposalHash").asText(),"검사 근거 확인");}
    private String receiptStatus(){return jdbc.sql("SELECT status::text FROM inbound WHERE inbound_id=:id").param("id",inbound).query(String.class).single();}
    @Test void safeReleaseIsHumanApprovedAndProductionDecrementsExactlyOnce() {
        var p=propose();assertThat(receiptStatus()).isEqualTo("HOLD");
        assertThat(service.inbound(inbound).path("eligible").asBoolean()).isFalse();
        var r=service.decide(p.path("approvalId").asLong(),decision(p,"APPROVE"),actors.humanForRole("QC"),"approve");
        assertThat(r.path("status").asText()).isEqualTo("APPROVED");assertThat(receiptStatus()).isEqualTo("RELEASED");
        var input=new QualityService.ProductionInput(record,lot,new java.math.BigDecimal("0.5"));
        var first=service.produce(input,actors.humanForRole("OPERATOR"),"input");
        assertThat(service.produce(input,actors.humanForRole("OPERATOR"),"input")).isEqualTo(first);
        assertThat(new java.math.BigDecimal(first.path("remainingQuantity").asText())).isEqualByComparingTo("1.5");
        assertThat(id("SELECT count(*) FROM governance_decisions WHERE is_final")).isEqualTo(1);
        assertThat(id("SELECT count(*) FROM governance_audit_logs WHERE event_type='QUALITY_DECIDED'")).isEqualTo(1);
        assertThat(id("SELECT count(*) FROM governance_audit_logs WHERE event_type='PRODUCTION_INPUT_RECORDED' AND actor_id=(SELECT user_id FROM users WHERE email='local-operator@mulino.local')")).isEqualTo(1);
        assertThat(jdbc.sql("SELECT (before_state->>'remainingQuantity')::numeric FROM governance_audit_logs WHERE event_type='PRODUCTION_INPUT_RECORDED'").query(java.math.BigDecimal.class).single()).isEqualByComparingTo("2");
        assertThat(jdbc.sql("SELECT (after_state->>'remainingQuantity')::numeric FROM governance_audit_logs WHERE event_type='PRODUCTION_INPUT_RECORDED'").query(java.math.BigDecimal.class).single()).isEqualByComparingTo("1.5");
        assertThat(service.decide(p.path("approvalId").asLong(),decision(p,"APPROVE"),actors.humanForRole("QC"),"approve")).isEqualTo(r);
    }
    @Test void changedSafetyFactsExpireProposalAndCannotReleaseReceipt() {
        var p=propose();jdbc.sql("DELETE FROM material_quality_declarations WHERE raw_material_id=(SELECT raw_material_id FROM inbound WHERE inbound_id=:id)").param("id",inbound).update();
        var r=service.decide(p.path("approvalId").asLong(),decision(p,"APPROVE"),actors.humanForRole("QC"),"approve");
        assertThat(service.inspection(p.path("approvalId").asLong()).path("status").asText()).isEqualTo("EXPIRED");assertThat(receiptStatus()).isEqualTo("HOLD");assertThat(service.inbound(inbound).path("eligible").asBoolean()).isFalse();
    }
    @Test void wrongHumanRolesAndAgentCannotMakeQcDecision() {
        var p=propose();for(String role:List.of("MANAGER","OPERATOR","ADMIN","VIEWER"))
            assertThatThrownBy(()->service.decide(p.path("approvalId").asLong(),decision(p,"APPROVE"),actors.humanForRole(role),"wrong-"+role)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->service.decide(p.path("approvalId").asLong(),decision(p,"APPROVE"),new AgentActor("r",caseRef,"w","QC"),"agent")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(receiptStatus()).isEqualTo("HOLD");assertThat(id("SELECT count(*) FROM governance_decisions")).isZero();
    }
    @Test void databaseRoleAndActivityOverrideForgedPrincipal() {
        var p=propose();var qc=actors.humanForRole("QC");jdbc.sql("UPDATE users SET is_active=false WHERE user_id=:id").param("id",qc.userId()).update();
        assertThatThrownBy(()->service.decide(p.path("approvalId").asLong(),decision(p,"APPROVE"),qc,"inactive")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(receiptStatus()).isEqualTo("HOLD");
    }
    @Test void rejectedReleasedHazardKeepsBarrierWithoutUnapprovedStatusMutation() {
        jdbc.sql("UPDATE inbound SET status='RELEASED',status_reason='historical approval',status_decided_by=(SELECT user_id FROM users WHERE email='replenishment-demo@example.invalid'),status_decided_at=CURRENT_TIMESTAMP WHERE inbound_id=:id").param("id",inbound).update();
        jdbc.sql("DELETE FROM material_quality_declarations WHERE raw_material_id=(SELECT raw_material_id FROM inbound WHERE inbound_id=:id)").param("id",inbound).update();
        var p=propose();service.decide(p.path("approvalId").asLong(),decision(p,"BLOCK"),actors.humanForRole("QC"),"reject");
        assertThat(receiptStatus()).isEqualTo("RELEASED");assertThat(service.inbound(inbound).path("eligible").asBoolean()).isFalse();
        assertThatThrownBy(()->service.produce(new QualityService.ProductionInput(record,lot,java.math.BigDecimal.ONE),actors.humanForRole("OPERATOR"),"unsafe")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(jdbc.sql("SELECT remaining_quantity FROM raw_material_lots WHERE raw_material_lot_id=:id").param("id",lot).query(java.math.BigDecimal.class).single()).isEqualByComparingTo("2");
    }
    @Test void auditFailureRollsBackDecisionStatusAndErpWrite() {
        var p=propose();jdbc.sql("CREATE FUNCTION fail_quality_audit() RETURNS trigger AS $$ BEGIN IF NEW.event_type='QUALITY_DECIDED' THEN RAISE EXCEPTION 'fixture audit failure'; END IF; RETURN NEW; END; $$ LANGUAGE plpgsql").update();jdbc.sql("CREATE TRIGGER fail_quality_audit BEFORE INSERT ON governance_audit_logs FOR EACH ROW EXECUTE FUNCTION fail_quality_audit()").update();
        assertThatThrownBy(()->service.decide(p.path("approvalId").asLong(),decision(p,"APPROVE"),actors.humanForRole("QC"),"fail")).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(receiptStatus()).isEqualTo("HOLD");assertThat(id("SELECT count(*) FROM governance_decisions")).isZero();
        assertThat(service.inspection(p.path("approvalId").asLong()).path("status").asText()).isEqualTo("PENDING");
    }
    @Test void pendingAndFinalEvidenceCannotBeRewritten() {
        var p=propose();long action=p.path("approvalId").asLong();
        assertThatThrownBy(()->jdbc.sql("UPDATE inbound_inspections SET proposed_status='BLOCKED'").update()).isInstanceOf(org.springframework.dao.DataAccessException.class);
        service.decide(action,decision(p,"APPROVE"),actors.humanForRole("QC"),"approve");
        assertThatThrownBy(()->jdbc.sql("DELETE FROM governance_decisions WHERE governance_action_id=:id").param("id",action).update()).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(()->jdbc.sql("UPDATE governance_actions SET payload='{}' WHERE governance_action_id=:id").param("id",action).update()).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    @Test void qcRaceAndWrongVersionApplyOnlyOneFinalDecision() throws Exception {
        var p=propose();long action=p.path("approvalId").asLong();var qc=actors.humanForRole("QC");
        service.decide(action,new PurchaseDecisionRequest("APPROVE",p.path("version").asInt()+1,p.path("proposalHash").asText(),"old version"),qc,"wrong-version");
        assertThat(receiptStatus()).isEqualTo("HOLD");
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var one=pool.submit(()->service.decide(action,decision(p,"APPROVE"),qc,"race-one"));
            var two=pool.submit(()->service.decide(action,decision(p,"CANCEL"),qc,"race-two"));
            one.get(15,java.util.concurrent.TimeUnit.SECONDS);two.get(15,java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(id("SELECT count(*) FROM governance_decisions WHERE is_final")).isEqualTo(1);
        assertThat(id("SELECT count(*) FROM governance_audit_logs WHERE event_type='QUALITY_DECIDED'")).isEqualTo(1);
    }
    @Test void explicitAllergenFreeAndThirtyDayNoticeArePositiveEvidence() {
        jdbc.sql("UPDATE raw_material_allergens SET is_trace=true WHERE raw_material_id=(SELECT raw_material_id FROM inbound WHERE inbound_id=:id)").param("id",inbound).update();
        jdbc.sql("UPDATE supplier_certifications SET expiry_date='2026-09-30' WHERE supplier_id=(SELECT supplier_id FROM inbound WHERE inbound_id=:id) AND cert_type='HACCP'").param("id",inbound).update();
        var p=propose();assertThat(p.path("proposedStatus").asText()).isEqualTo("RELEASED");
        assertThat(id("SELECT count(*) FROM attention_requests WHERE title='HACCP 만료 30일 전 통지' AND status='OPEN'")).isEqualTo(1);
        assertThat(id("SELECT count(DISTINCT code) FROM allergens WHERE standard='KR_MFDS'")).isEqualTo(22);
        assertThat(id("SELECT count(DISTINCT legal_category) FROM allergens WHERE standard='KR_MFDS'")).isEqualTo(19);
    }

    @Test void databaseGuardRejectsHeldInsertAndUnsafeUpdateWithoutChangingBalance() {
        assertThatThrownBy(()->jdbc.sql("INSERT INTO production_ingredients(production_record_id,raw_material_lot_id,quantity_used) VALUES (:record,:lot,0.5)").param("record",record).param("lot",lot).update()).isInstanceOf(org.springframework.dao.DataAccessException.class);
        var p=propose();service.decide(p.path("approvalId").asLong(),decision(p,"APPROVE"),actors.humanForRole("QC"),"approve");
        service.produce(new QualityService.ProductionInput(record,lot,new java.math.BigDecimal("0.5")),actors.humanForRole("OPERATOR"),"valid-input");
        jdbc.sql("DELETE FROM material_quality_declarations WHERE raw_material_id=(SELECT raw_material_id FROM inbound WHERE inbound_id=:id)").param("id",inbound).update();
        assertThatThrownBy(()->jdbc.sql("UPDATE production_ingredients SET quantity_used=1 WHERE raw_material_lot_id=:id").param("id",lot).update()).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(jdbc.sql("SELECT remaining_quantity FROM raw_material_lots WHERE raw_material_lot_id=:id").param("id",lot).query(java.math.BigDecimal.class).single()).isEqualByComparingTo("1.5");
    }
    @Test void pendingReleasedReceiptIsExcludedFromPlanningAndMissingFreeDeclarationFailsClosed() {
        jdbc.sql("UPDATE inbound SET status='RELEASED',status_reason='historical approval',status_decided_by=(SELECT user_id FROM users WHERE email='replenishment-demo@example.invalid'),status_decided_at=CURRENT_TIMESTAMP WHERE inbound_id=:id").param("id",inbound).update();
        propose();
        long warehouse=id("SELECT warehouse_id FROM warehouses WHERE plant_id='DEMO-KR-01'");
        var products=jdbc.sql("SELECT product_id FROM products WHERE sku IN ('DEMO-AMR','DEMO-BSC') ORDER BY product_id").query(Long.class).list();
        var supply=snapshots.load(warehouse,products,LocalDate.of(2026,9,5),30).supply();
        assertThat(supply.stream().filter(x->x.sourceRef().equals("raw_material_lots:"+lot)).findFirst().orElseThrow().exclusionReason()).isNotNull();
        long sugar=id("SELECT inbound_id FROM raw_material_lots WHERE lot_number='DEMO-RM-SUGAR'");
        assertThat(service.inbound(sugar).path("eligible").asBoolean()).isTrue();
        jdbc.sql("DELETE FROM material_quality_declarations WHERE raw_material_id=(SELECT raw_material_id FROM inbound WHERE inbound_id=:id)").param("id",sugar).update();
        assertThat(service.inbound(sugar).path("eligible").asBoolean()).isFalse();
    }

    @Test void contradictoryAllergenFreeAttestationProposesBlockDespiteCompleteMaster() {
        jdbc.sql("UPDATE material_quality_declarations SET allergen_classification='ALLERGEN_FREE' WHERE raw_material_id=(SELECT raw_material_id FROM inbound WHERE inbound_id=:id)").param("id",inbound).update();
        assertThat(id("SELECT count(DISTINCT code) FROM allergens WHERE standard='KR_MFDS'")).isEqualTo(22);
        var p=propose();assertThat(p.path("proposedStatus").asText()).isEqualTo("BLOCKED");
        service.decide(p.path("approvalId").asLong(),decision(p,"APPROVE"),actors.humanForRole("QC"),"approve-block");
        assertThat(receiptStatus()).isEqualTo("BLOCKED");assertThat(service.inbound(inbound).path("eligible").asBoolean()).isFalse();
    }

    @Test void productionAuditFailureRollsBackInputBalanceAndIdempotencyReceipt() {
        var p=propose();service.decide(p.path("approvalId").asLong(),decision(p,"APPROVE"),actors.humanForRole("QC"),"approve");
        jdbc.sql("CREATE FUNCTION fail_production_audit() RETURNS trigger AS $$ BEGIN IF NEW.event_type='PRODUCTION_INPUT_RECORDED' THEN RAISE EXCEPTION 'fixture production audit failure'; END IF; RETURN NEW; END; $$ LANGUAGE plpgsql").update();
        jdbc.sql("CREATE TRIGGER fail_production_audit BEFORE INSERT ON governance_audit_logs FOR EACH ROW EXECUTE FUNCTION fail_production_audit()").update();
        assertThatThrownBy(()->service.produce(new QualityService.ProductionInput(record,lot,new java.math.BigDecimal("0.5")),actors.humanForRole("OPERATOR"),"fail-production")).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(jdbc.sql("SELECT remaining_quantity FROM raw_material_lots WHERE raw_material_lot_id=:id").param("id",lot).query(java.math.BigDecimal.class).single()).isEqualByComparingTo("2");
        assertThat(jdbc.sql("SELECT count(*) FROM production_ingredients WHERE raw_material_lot_id=:id").param("id",lot).query(Long.class).single()).isZero();
        assertThat(id("SELECT count(*) FROM request_idempotency WHERE request_key='fail-production'")).isZero();
        assertThat(id("SELECT count(*) FROM governance_audit_logs WHERE event_type='PRODUCTION_INPUT_RECORDED'")).isZero();
    }
    @Test void productionRejectsReceiptRecordAndKnownLotWarehouseConflicts() throws Exception {
        var p=propose();service.decide(p.path("approvalId").asLong(),decision(p,"APPROVE"),actors.humanForRole("QC"),"approve");
        long other=id("INSERT INTO warehouses(name,location,type) VALUES ('QC other warehouse','KR','AMBIENT') RETURNING warehouse_id");
        long original=id("SELECT warehouse_id FROM inbound WHERE inbound_id="+inbound);
        long production=id("INSERT INTO production_lots(product_id,warehouse_id,lot_number,production_date,expiry_date,quantity) SELECT product_id,"+other+",'QC-CROSS-PLANT','2026-09-05','2026-12-31',1 FROM production_lots WHERE lot_number='DEMO-DOUGH-CURRENT' RETURNING production_lot_id");
        long inconsistent=id("INSERT INTO production_records(lot_id,warehouse_id,operator_id,process_type,start_time) SELECT "+production+","+original+",user_id,'MIX','2026-09-05 10:00:00' FROM users WHERE email='replenishment-demo@example.invalid' RETURNING production_record_id");
        long foreign=id("INSERT INTO production_records(lot_id,warehouse_id,operator_id,process_type,start_time) SELECT "+production+","+other+",user_id,'MIX','2026-09-05 10:00:00' FROM users WHERE email='replenishment-demo@example.invalid' RETURNING production_record_id");
        for(long invalidRecord:List.of(inconsistent,foreign)) {
            mvc.perform(post("/api/v1/quality/production-inputs").header("X-Mulino-Local-Role","OPERATOR").header("Idempotency-Key","warehouse-rejected-"+invalidRecord)
                .contentType("application/json").content(mapper.writeValueAsString(new QualityService.ProductionInput(invalidRecord,lot,new java.math.BigDecimal("0.5"))))).andExpect(status().isConflict());
        }
        assertThat(jdbc.sql("SELECT remaining_quantity FROM raw_material_lots WHERE raw_material_lot_id=:id").param("id",lot).query(java.math.BigDecimal.class).single()).isEqualByComparingTo("2");
        assertThat(jdbc.sql("SELECT count(*) FROM production_ingredients WHERE raw_material_lot_id=:id").param("id",lot).query(Long.class).single()).isZero();
        assertThat(id("SELECT count(*) FROM governance_audit_logs WHERE event_type='PRODUCTION_INPUT_RECORDED'")).isZero();
    }

    @Test void currentlyExpiredRawLotAndProductionBeforeReceiptLeaveNoInputBalanceOrAudit() throws Exception {
        var p=propose();service.decide(p.path("approvalId").asLong(),decision(p,"APPROVE"),actors.humanForRole("QC"),"approve");
        jdbc.sql("UPDATE raw_material_lots SET expiry_date='2026-09-04' WHERE raw_material_lot_id=:id").param("id",lot).update();
        mvc.perform(post("/api/v1/quality/production-inputs").header("X-Mulino-Local-Role","OPERATOR").header("Idempotency-Key","expired-lot")
            .contentType("application/json").content(mapper.writeValueAsString(new QualityService.ProductionInput(record,lot,new java.math.BigDecimal("0.5"))))).andExpect(status().isConflict());
        jdbc.sql("UPDATE raw_material_lots SET expiry_date='2026-12-31' WHERE raw_material_lot_id=:id").param("id",lot).update();
        long prematureLot=id("INSERT INTO production_lots(product_id,warehouse_id,lot_number,production_date,expiry_date,quantity) SELECT product_id,warehouse_id,'QC-BEFORE-ARRIVAL','2026-09-03','2026-12-31',1 FROM production_lots WHERE lot_number='DEMO-DOUGH-CURRENT' RETURNING production_lot_id");
        long beforeArrival=id("INSERT INTO production_records(lot_id,warehouse_id,operator_id,process_type,start_time) SELECT "+prematureLot+",warehouse_id,(SELECT user_id FROM users WHERE email='replenishment-demo@example.invalid'),'MIX','2026-09-03 10:00:00' FROM inbound WHERE inbound_id="+inbound+" RETURNING production_record_id");
        mvc.perform(post("/api/v1/quality/production-inputs").header("X-Mulino-Local-Role","OPERATOR").header("Idempotency-Key","before-arrival")
            .contentType("application/json").content(mapper.writeValueAsString(new QualityService.ProductionInput(beforeArrival,lot,new java.math.BigDecimal("0.5"))))).andExpect(status().isConflict());
        assertThat(jdbc.sql("SELECT remaining_quantity FROM raw_material_lots WHERE raw_material_lot_id=:id").param("id",lot).query(java.math.BigDecimal.class).single()).isEqualByComparingTo("2");
        assertThat(jdbc.sql("SELECT count(*) FROM production_ingredients WHERE raw_material_lot_id=:id").param("id",lot).query(Long.class).single()).isZero();
        assertThat(id("SELECT count(*) FROM governance_audit_logs WHERE event_type='PRODUCTION_INPUT_RECORDED'")).isZero();
    }

}
