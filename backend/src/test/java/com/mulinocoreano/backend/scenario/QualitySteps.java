package com.mulinocoreano.backend.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import java.util.Map;

/** Goals 1,2,4,5,6: real runner/CLI inspection and human stdio decisions. SQL only builds fixtures or reads final ERP evidence. */
public class QualitySteps {
    @Autowired ScenarioWorld world; @Autowired JdbcClient jdbc; @Autowired ObjectMapper mapper; @Autowired AgentSteps agent; @Autowired BusinessState state;
    long inboundId,lotId,recordId,approvalId;
    private HumanChannel human(){return new HumanChannel(mapper,world.apiBase());}
    private void fixture(String kind){
        inboundId=jdbc.sql("SELECT i.inbound_id FROM inbound i JOIN raw_material_lots l USING(inbound_id) WHERE l.lot_number='DEMO-RM-FLOUR-HOLD'").query(Long.class).single();
        lotId=jdbc.sql("SELECT raw_material_lot_id FROM raw_material_lots WHERE inbound_id=:id").param("id",inboundId).query(Long.class).single();
        recordId=jdbc.sql("SELECT r.production_record_id FROM production_records r JOIN production_lots l ON l.production_lot_id=r.lot_id WHERE l.lot_number='DEMO-DOUGH-CURRENT'").query(Long.class).single();
        if(kind.equals("temperature")){
            jdbc.sql("UPDATE material_quality_declarations SET min_temperature=0,max_temperature=5 WHERE raw_material_id=(SELECT raw_material_id FROM inbound WHERE inbound_id=:id)").param("id",inboundId).update();
            jdbc.sql("INSERT INTO inbound_temperature_logs(inbound_id,temperature,sensor_id) VALUES (:id,12,'QC-SIT')").param("id",inboundId).update();
        } else if(kind.equals("allergens")) jdbc.sql("DELETE FROM material_quality_declarations WHERE raw_material_id=(SELECT raw_material_id FROM inbound WHERE inbound_id=:id)").param("id",inboundId).update();
        else if(kind.equals("certificate")) jdbc.sql("UPDATE supplier_certifications SET expiry_date='2026-09-03' WHERE supplier_id=(SELECT supplier_id FROM inbound WHERE inbound_id=:id) AND cert_type='HACCP'").param("id",inboundId).update();
        var created=human().call("OPERATOR","create_case",Map.of("objective","입고 온도·알레르겐·인증 품질 검사","requestKey","quality-case"));
        assertThat(created.isError()).isFalse(); world.caseRef(created.content().path("caseRef").asText());
    }
    private void inspect(){
        var assigned=human().call("OPERATOR","request_quality_inspection",Map.of("inboundId",inboundId,"caseRef",world.caseRef(),"requestKey","quality-assign"));
        assertThat(assigned.isError()).isFalse();
        agent.driver().awaitState("QC inspection pending",()->jdbc.sql("SELECT count(*) FROM inbound_inspections WHERE inbound_id=:id").param("id",inboundId).query(Long.class).single()==1,()->state.latestRunFailed(world.caseRef()),agent.timeout());
        approvalId=jdbc.sql("SELECT governance_action_id FROM inbound_inspections WHERE inbound_id=:id").param("id",inboundId).query(Long.class).single();
    }
    @Given("냉장 원재료 입고의 기록 온도가 허용 범위를 벗어났다") public void temperature(){fixture("temperature");}
    @Given("원재료 LOT 보류 제안이 QC 승인을 기다린다") public void pendingFixture(){fixture("temperature");inspect();}
    @Given("알레르겐 매핑이 없는 원재료가 입고된다") public void allergens(){fixture("allergens");}
    @Given("공급사의 HACCP 인증이 입고일 이전에 만료되었다") public void certificate(){fixture("certificate");}
    @When("에이전트가 입고 품질을 점검한다") public void agentInspects(){inspect();}
    @When("해당 공급사의 원재료가 입고된다") public void receipt(){inspect();}
    private JsonNode approval(){return human().call("QC","get_quality_approval",Map.of("approvalId",approvalId)).content();}
    private void decide(String role,String decision){
        var a=approval(); human().call(role,"decide_quality",Map.of("approvalId",approvalId,"decision",decision,"expectedVersion",a.path("version").asInt(),"proposalHash",a.path("proposalHash").asText(),"reason","품질 근거를 검토한 인간 결정","requestKey","quality-"+role+decision));
    }
    @When("QC가 보류 제안을 승인한다") public void approve(){decide("QC","APPROVE");}
    @When("MANAGER가 보류 제안 승인을 시도한다") public void manager(){decide("MANAGER","APPROVE");}
    @When("QC가 보류 제안을 반려한다") public void reject(){decide("QC","BLOCK");}
    @Then("해당 원재료 LOT은 보류 제안 상태다") public void proposed(){assertThat(approval().path("status").asText()).isEqualTo("PENDING");assertThat(status()).isEqualTo("HOLD");}
    @Then("해당 입고는 차단 제안 상태다") public void blocking(){assertThat(approval().path("proposedStatus").asText()).isEqualTo("BLOCKED");assertThat(approval().path("status").asText()).isEqualTo("PENDING");assertThat(status()).isEqualTo("HOLD");}
    @Then("보류 제안은 승인 대기 상태다") public void stillPending(){assertThat(approval().path("status").asText()).isEqualTo("PENDING");}
    @Then("해당 원재료 LOT 상태는 바뀌지 않았다") public void unchanged(){assertThat(status()).isEqualTo("HOLD");}
    @Then("해당 원재료 LOT은 차단 상태다") public void blocked(){assertThat(status()).isEqualTo("BLOCKED");assertThat(approval().path("status").asText()).isEqualTo("APPROVED");}
    @Then("보류 제안은 반려 상태다") public void rejected(){assertThat(approval().path("status").asText()).isEqualTo("BLOCKED");assertThat(status()).isEqualTo("HOLD");}
    private String status(){return jdbc.sql("SELECT status::text FROM inbound WHERE inbound_id=:id").param("id",inboundId).query(String.class).single();}
    @Then("해당 원재료 LOT은 생산에 투입할 수 없다") public void noProduction(){
        var before=jdbc.sql("SELECT remaining_quantity FROM raw_material_lots WHERE raw_material_lot_id=:lot").param("lot",lotId).query(java.math.BigDecimal.class).single();
        var count=jdbc.sql("SELECT count(*) FROM production_ingredients WHERE raw_material_lot_id=:lot").param("lot",lotId).query(Long.class).single();
        human().call("OPERATOR","record_production_input",Map.of("productionRecordId",recordId,"rawMaterialLotId",lotId,"quantity",1,"requestKey","quality-production-rejected"));
        assertThat(jdbc.sql("SELECT remaining_quantity FROM raw_material_lots WHERE raw_material_lot_id=:lot").param("lot",lotId).query(java.math.BigDecimal.class).single()).isEqualByComparingTo(before);
        assertThat(jdbc.sql("SELECT count(*) FROM production_ingredients WHERE raw_material_lot_id=:lot").param("lot",lotId).query(Long.class).single()).isEqualTo(count);
        assertThat(human().call("QC","get_inbound_quality",Map.of("inboundId",inboundId)).content().path("eligible").asBoolean()).isFalse();
    }
}
