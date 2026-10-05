package com.mulinocoreano.backend.scenario;

import static org.assertj.core.api.Assertions.*;

import com.mulinocoreano.backend.recall.RecallDemoFixture;
import io.cucumber.java.en.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.*;

/** Goals 1,2,4,6: real CLI/runner proposal and ADMIN stdio decisions. */
public class RecallSteps {
  @Autowired ScenarioWorld world;
  @Autowired JdbcClient jdbc;
  @Autowired ObjectMapper mapper;
  @Autowired AgentSteps agent;
  long lot, approval;
  JsonNode trace;

  HumanChannel human() {
    return new HumanChannel(mapper, world.apiBase());
  }

  long count(String sql) {
    return jdbc.sql(sql).query(Long.class).single();
  }

  @Given("고객이 완제품 LOT에 대해 컴플레인을 접수했다")
  public void incident() throws Exception {
    RecallDemoFixture.load(jdbc);
    lot = count("SELECT production_lot_id FROM production_lots WHERE lot_number='RC-FINAL'");
    var c =
        human()
            .call(
                "OPERATOR",
                "create_case",
                Map.of("objective", "완제품 LOT 컴플레인 전수 리콜 조사", "requestKey", "rc-case"));
    assertThat(c.isError()).isFalse();
    world.caseRef(c.content().path("caseRef").asText());
  }

  @When("에이전트가 해당 LOT을 역추적한다")
  public void investigate() {
    var assigned =
        human()
            .call(
                "OPERATOR",
                "request_recall_investigation",
                Map.of(
                    "lotId",
                    lot,
                    "caseRef",
                    world.caseRef(),
                    "reason",
                    "완제품 컴플레인 조사",
                    "requestKey",
                    "rc-assign"));
    assertThat(assigned.isError()).isFalse();
    agent
        .driver()
        .awaitState(
            "QC recall pending",
            () -> count("SELECT count(*) FROM recall_proposals") == 1,
            agent.timeout());
    approval = count("SELECT governance_action_id FROM recall_proposals");
    trace = human().call("VIEWER", "get_lot_trace", Map.of("lotId", lot)).content();
    assertThat(trace.path("complete").asBoolean()).isTrue();
  }

  @Given("에이전트가 리콜을 제안했다")
  public void pending() throws Exception {
    incident();
    investigate();
  }

  @Then("원재료 LOT과 공급사까지 추적된다")
  public void sources() {
    assertThat(trace.path("rawLots").size()).isEqualTo(3);
    assertThat(trace.path("suppliers").size()).isEqualTo(1);
    long roots =
        java.util.stream.StreamSupport.stream(trace.path("rawLots").spliterator(), false)
            .filter(r -> r.path("incidentRoot").asBoolean())
            .count();
    assertThat(roots).isEqualTo(2);
    assertThat(trace.path("productInputs").size()).isEqualTo(8);
  }

  @Then("같은 원재료 LOT을 쓴 완제품의 출고 고객이 모두 식별된다")
  public void customers() {
    assertThat(trace.path("customers").size()).isEqualTo(2);
    assertThat(trace.path("shipments").size()).isEqualTo(115);
    assertThat(trace.path("productionLots").size()).isEqualTo(10);
  }

  @Then("리콜은 생성되지 않았다")
  public void noRecall() {
    assertThat(count("SELECT count(*) FROM recalls")).isZero();
  }

  @Then("대상 완제품 LOT 상태는 바뀌지 않았다")
  public void unchanged() {
    assertThat(count("SELECT count(*) FROM production_lots WHERE status='RECALLED'")).isZero();
    assertThat(
            jdbc.sql("SELECT recall_lot_barrier(:id)")
                .param("id", lot)
                .query(Boolean.class)
                .single())
        .isTrue();
  }

  JsonNode approval() {
    var a = human().call("ADMIN", "get_recall_approval", Map.of("approvalId", approval));
    assertThat(a.isError()).isFalse();
    return a.content();
  }

  void decide(String role, String decision) {
    var a = approval();
    var r =
        human()
            .call(
                role,
                "decide_recall",
                Map.of(
                    "approvalId",
                    approval,
                    "decision",
                    decision,
                    "expectedVersion",
                    a.path("version").asInt(),
                    "proposalHash",
                    a.path("proposalHash").asText(),
                    "reason",
                    "영향 범위와 위험 근거 검토",
                    "requestKey",
                    "rc-" + role + decision));
    assertThat(r.isError()).isEqualTo(!role.equals("ADMIN"));
  }

  @When("ADMIN이 리콜을 승인한다")
  public void approve() {
    decide("ADMIN", "APPROVE");
  }

  @When("ADMIN이 리콜을 반려한다")
  public void reject() {
    decide("ADMIN", "BLOCK");
  }

  @When("ADMIN이 리콜을 취소한다")
  public void cancel() {
    decide("ADMIN", "CANCEL");
  }

  @When("MANAGER가 리콜 승인을 시도한다")
  public void wrongRole() {
    decide("MANAGER", "APPROVE");
  }

  @Then("리콜 제안은 승인 대기 상태다")
  public void stillPending() {
    assertThat(approval().path("status").asText()).isEqualTo("PENDING");
  }

  @Then("대상 완제품 LOT은 회수 상태다")
  public void recalled() {
    assertThat(count("SELECT count(*) FROM production_lots WHERE status='RECALLED'")).isEqualTo(10);
    assertThat(count("SELECT count(*) FROM recalls")).isEqualTo(10);
  }

  @Then("식약처 보고 기록이 생성되었다")
  public void report() {
    assertThat(
            count(
                "SELECT count(*) FROM recall_reports WHERE status='PENDING' AND transport='OFFLINE'"
                    + " AND due_at<=created_at AND submitted_at IS NULL AND confirmation IS NULL"
                    + " AND retain_until>=created_at+INTERVAL '2 years'"))
        .isEqualTo(1);
    assertThat(
            count(
                "SELECT count(*) FROM attention_requests WHERE status='OPEN' AND"
                    + " governance_action_id IS NULL"))
        .isGreaterThan(0);
    assertThat(count("SELECT count(*) FROM cases WHERE status IN('CLOSED','RESOLVED')")).isZero();
  }
}
