package com.mulinocoreano.backend.supplier;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** 목표 2·3·5: 인간 master 변경만 허용하고 계획과 이력의 공급업체 신원을 보존한다. */
@SpringBootTest(properties={"mulino.local-auth.human-secret=test-human-gateway", "mulino.local-auth.service-secret=local-test-secret",
    "spring.flyway.schemas=supplier_it","spring.flyway.clean-disabled=false",
    "spring.flyway.init-sqls=CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public","spring.datasource.hikari.schema=supplier_it"})
@AutoConfigureMockMvc @ActiveProfiles("local")
class SupplierIntegrationTest {
    @Autowired com.mulinocoreano.backend.planning.PlanningSnapshotRepository snapshots;
    @Autowired org.springframework.transaction.PlatformTransactionManager manager;
    @Autowired SupplierService service;
    @Autowired com.mulinocoreano.backend.security.LocalActorDirectory directory;
    @Autowired MockMvc mvc; @Autowired JdbcClient jdbc; @Autowired Flyway flyway; @Autowired ObjectMapper mapper;
    static final String INPUT="{\"name\":\"Italian mill\",\"country\":\"Italy\",\"addressLine\":\"Via Roma 1\",\"city\":\"Parma\",\"postalCode\":\"43100\",\"paymentTerms\":\"NET_60\",\"currency\":\"EUR\"}";
    @BeforeEach void setup(){flyway.clean();flyway.migrate();}
    JsonNode send(String method,String suffix,String key,String body,String role,int expected) throws Exception {
        var request=switch(method){case "POST"->post("/api/v1/suppliers"+suffix);case "PUT"->put("/api/v1/suppliers"+suffix);case "DELETE"->delete("/api/v1/suppliers"+suffix);default->get("/api/v1/suppliers"+suffix);};
        request.header("X-Mulino-Local-Human","test-human-gateway").header("X-Mulino-Local-Role",role);
        if(key!=null)request.header("Idempotency-Key",key);if(body!=null)request.contentType("application/json").content(body);
        var response=mvc.perform(request).andReturn().getResponse();assertThat(response.getStatus()).isEqualTo(expected);
        return response.getContentAsString().isBlank()?null:mapper.readTree(response.getContentAsString());
    }
    long count(String table){return jdbc.sql("SELECT count(*) FROM "+table).query(Long.class).single();}
    @Test void humanCrudReplaysPreserveIdentityAndImmutableAudits() throws Exception {
        var created=send("POST","","create",INPUT,"OPERATOR",201);var supplier=created.path("data");long id=supplier.path("supplierId").asLong();
        assertThat(supplier.path("paymentTerms").asText()).isEqualTo("NET_60");assertThat(supplier.path("currency").asText()).isEqualTo("EUR");
        assertThat(send("POST","","create",INPUT,"OPERATOR",201)).isEqualTo(created);assertThat(count("suppliers")).isOne();assertThat(count("governance_audit_logs")).isOne();
        assertThat(send("GET","/"+id,null,null,"VIEWER",200).path("data")).isEqualTo(supplier);
        assertThat(send("GET","",null,null,"VIEWER",200).path("data").size()).isOne();
        String update="{\"supplier\":"+INPUT.replace("Italian mill","Updated mill")+",\"expectedVersion\":0}";
        var updated=send("PUT","/"+id,"update",update,"OPERATOR",200);
        assertThat(updated.path("data").path("supplierId").asLong()).isEqualTo(id);assertThat(updated.path("data").path("version").asLong()).isEqualTo(1);
        assertThat(send("PUT","/"+id,"update",update,"OPERATOR",200)).isEqualTo(updated);
        send("PUT","/"+id,"stale",update,"OPERATOR",409);
        var deactivated=send("DELETE","/"+id+"?expectedVersion=1","deactivate",null,"OPERATOR",200);
        assertThat(deactivated.path("data").path("active").asBoolean()).isFalse();
        assertThat(send("DELETE","/"+id+"?expectedVersion=1","deactivate",null,"OPERATOR",200)).isEqualTo(deactivated);
        assertThat(count("suppliers")).isOne();assertThat(count("governance_audit_logs")).isEqualTo(3);
        var audit=jdbc.sql("SELECT before_state->'supplier'->>'name' FROM governance_audit_logs WHERE event_type='SUPPLIER_UPDATED'").query(String.class).single();assertThat(audit).isEqualTo("Italian mill");
        assertThat(jdbc.sql("SELECT count(*) FROM governance_audit_logs a JOIN users u ON u.user_id=a.actor_id WHERE u.email='local-operator@mulino.local' AND (a.after_state->'actor'->>'userId')::bigint=u.user_id").query(Long.class).single()).isEqualTo(3);
        assertThatThrownBy(()->jdbc.sql("DELETE FROM governance_audit_logs WHERE resource_type='SUPPLIER'").update()).isInstanceOf(Exception.class);
        assertThat(send("GET","/"+id,null,null,"VIEWER",200).path("data").path("active").asBoolean()).isFalse();
    }
    @Test void unauthorizedOrInvalidCommandsLeaveMasterAndAuditUnchanged() throws Exception {
        for(String role:List.of("VIEWER","QC","ADMIN"))send("POST","","denied",INPUT,role,403);
        for(var headers:List.of(Map.of("X-Mulino-Local-Role","MANAGER"),Map.of("X-Mulino-Local-Human","test-human-gateway","X-Mulino-Local-Role","MANAGER","Authorization","Bearer fake"),Map.of("X-Mulino-Local-Human","test-human-gateway","X-Mulino-Local-Role","MANAGER","X-Mulino-Local-Service","local-test-secret"))) {
            var request=post("/api/v1/suppliers").header("Idempotency-Key","fake").contentType("application/json").content(INPUT);headers.forEach(request::header);
            assertThat(mvc.perform(request).andReturn().getResponse().getStatus()).isEqualTo(401);
        }
        send("POST","","invalid",INPUT.replace("Italian mill",""),"MANAGER",400);
        send("POST","","invalid2",INPUT.replace("EUR","euro"),"MANAGER",400);
        assertThat(count("suppliers")).isZero();assertThat(count("governance_audit_logs")).isZero();
        var created=send("POST","","valid",INPUT,"MANAGER",201);
        send("POST","","valid",INPUT.replace("Italy","Korea"),"MANAGER",409);
        jdbc.sql("UPDATE users SET is_active=false WHERE email='local-manager@mulino.local'").update();
        send("POST","","valid",INPUT,"MANAGER",403);assertThat(count("suppliers")).isOne();assertThat(count("governance_audit_logs")).isOne();
    }
    @Test void transactionRevalidatesStoredAuthorityBeforeReplay() throws Exception {
        send("POST","","revalidate",INPUT,"OPERATOR",201);
        var actor=directory.humanForRole("OPERATOR");
        var context=org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new com.mulinocoreano.backend.security.ActorAuthenticationToken(actor));
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        try {
            jdbc.sql("UPDATE users SET role='QC' WHERE user_id=:id").param("id",actor.userId()).update();
            assertThatThrownBy(()->service.create(mapper.readValue(INPUT,CreateSupplierRequest.class),actor,"revalidate"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            jdbc.sql("UPDATE users SET role='OPERATOR',is_active=false WHERE user_id=:id").param("id",actor.userId()).update();
            assertThatThrownBy(()->service.create(mapper.readValue(INPUT,CreateSupplierRequest.class),actor,"revalidate"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        } finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
        assertThat(count("suppliers")).isOne();assertThat(count("governance_audit_logs")).isOne();
    }
    @Test void competingVersionedUpdatesAcceptOnlyOneCommand() throws Exception {
        long id=send("POST","","race-source",INPUT,"MANAGER",201).path("data").path("supplierId").asLong();
        try(var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            Callable<Integer> a=()->{start.await();return mvc.perform(put("/api/v1/suppliers/"+id)
                .header("X-Mulino-Local-Human","test-human-gateway").header("X-Mulino-Local-Role","MANAGER").header("Idempotency-Key","race-a")
                .contentType("application/json").content("{\"supplier\":"+INPUT.replace("Italian mill","Winner A")+",\"expectedVersion\":0}")).andReturn().getResponse().getStatus();};
            Callable<Integer> b=()->{start.await();return mvc.perform(put("/api/v1/suppliers/"+id)
                .header("X-Mulino-Local-Human","test-human-gateway").header("X-Mulino-Local-Role","MANAGER").header("Idempotency-Key","race-b")
                .contentType("application/json").content("{\"supplier\":"+INPUT.replace("Italian mill","Winner B")+",\"expectedVersion\":0}")).andReturn().getResponse().getStatus();};
            var first=pool.submit(a);var second=pool.submit(b);start.countDown();assertThat(List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(send("GET","/"+id,null,null,"VIEWER",200).path("data").path("version").asLong()).isEqualTo(1);
        assertThat(count("governance_audit_logs")).isEqualTo(2);assertThat(count("request_idempotency")).isEqualTo(2);
    }
    @Test void simultaneousIdenticalCommandsReturnOneSupplierAndOneAudit() throws Exception {
        // Provision the same Human before starting both requests.
        send("GET","",null,null,"MANAGER",200);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            Callable<JsonNode> command=()->{start.await();return send("POST","","concurrent",INPUT,"MANAGER",201);};
            var a=pool.submit(command);var b=pool.submit(command);start.countDown();assertThat(a.get(20,TimeUnit.SECONDS)).isEqualTo(b.get(20,TimeUnit.SECONDS));
        }
        assertThat(count("suppliers")).isOne();assertThat(count("governance_audit_logs")).isOne();
    }
    @Test void deactivationPreservesHistoricalTraceAndExcludesNewPlanningCandidate() throws Exception {
        com.mulinocoreano.backend.planning.ReplenishmentDemoFixture.load(jdbc);
        long id=jdbc.sql("SELECT supplier_id FROM suppliers ORDER BY supplier_id LIMIT 1").query(Long.class).single();
        jdbc.sql("UPDATE suppliers SET created_at='2001-02-03 04:05:06',updated_at=NULL WHERE supplier_id=:id").param("id",id).update();
        var legacy=send("GET","/"+id,null,null,"VIEWER",200).path("data");
        assertThat(legacy.path("createdAt").asText()).isEqualTo("2001-02-03T04:05:06");
        assertThat(legacy.path("updatedAt").isNull() || legacy.path("updatedAt").isMissingNode()).isTrue();
        String history=jdbc.sql("SELECT jsonb_build_object('po',(SELECT jsonb_agg(to_jsonb(p)) FROM purchase_orders p),'inbound',(SELECT jsonb_agg(to_jsonb(i)) FROM inbound i))::text").query(String.class).single();
        long warehouse=jdbc.sql("SELECT warehouse_id FROM warehouses WHERE plant_id='DEMO-KR-01'").query(Long.class).single();
        var products=jdbc.sql("SELECT product_id FROM products WHERE sku IN ('DEMO-AMR','DEMO-BSC') ORDER BY product_id").query(Long.class).list();
        var tx=new org.springframework.transaction.support.TransactionTemplate(manager);
        tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
        var before=tx.execute(status->snapshots.load(warehouse,products,java.time.LocalDate.of(2026,9,5),30));
        assertThat(before.supplierTerms().values().stream().flatMap(List::stream).filter(t->t.supplierId()==id).allMatch(t->t.active())).isTrue();
        var updated=send("PUT","/"+id,"currency", "{\"supplier\":"+INPUT+",\"expectedVersion\":0}","MANAGER",200);
        assertThat(updated.path("data").path("createdAt").asText()).isEqualTo("2001-02-03T04:05:06");
        assertThat(updated.path("data").path("updatedAt").asText()).isNotBlank();
        send("DELETE","/"+id+"?expectedVersion=1","historical",null,"MANAGER",200);
        var after=tx.execute(status->snapshots.load(warehouse,products,java.time.LocalDate.of(2026,9,5),30));
        var terms=after.supplierTerms().values().stream().flatMap(List::stream).filter(t->t.supplierId()==id).toList();
        assertThat(terms).isNotEmpty();assertThat(terms).allMatch(t->!t.active());
        assertThat(terms.stream().map(t->t.unitPrice()).toList()).isEqualTo(before.supplierTerms().values().stream().flatMap(List::stream).filter(t->t.supplierId()==id).map(t->t.unitPrice()).toList());
        assertThat(jdbc.sql("SELECT jsonb_build_object('po',(SELECT jsonb_agg(to_jsonb(p)) FROM purchase_orders p),'inbound',(SELECT jsonb_agg(to_jsonb(i)) FROM inbound i))::text").query(String.class).single()).isEqualTo(history);
        assertThat(jdbc.sql("SELECT count(*) FROM inbound i JOIN suppliers s USING(supplier_id) WHERE s.supplier_id=:id").param("id",id).query(Long.class).single()).isPositive();
    }
    @Test void auditFailureRollsBackSupplierAndReceipt() throws Exception {
        jdbc.sql("CREATE FUNCTION reject_supplier_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.resource_type='SUPPLIER' THEN RAISE EXCEPTION 'audit unavailable'; END IF; RETURN NEW; END $$").update();
        jdbc.sql("CREATE TRIGGER supplier_audit_failure BEFORE INSERT ON governance_audit_logs FOR EACH ROW EXECUTE FUNCTION reject_supplier_audit()").update();
        send("POST","","rollback",INPUT,"OPERATOR",500);
        assertThat(count("suppliers")).isZero();assertThat(count("governance_audit_logs")).isZero();assertThat(count("request_idempotency")).isZero();
    }
}
