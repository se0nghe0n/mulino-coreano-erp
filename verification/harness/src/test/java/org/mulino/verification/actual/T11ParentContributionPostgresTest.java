package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.Json;
import org.mulino.verification.ReferenceResolver;

/** Original T11 inputs over real Spring HTTP, with independent PG range/effect checks. */
@EnabledIfEnvironmentVariable(named="T11_PG_TEST",matches="true")
class T11ParentContributionPostgresTest {
    @ParameterizedTest @ValueSource(strings={"parent90-cancelled","parent90-impossible","parent90-superseded"})
    void originalParentFlows(String subcase) throws Exception {
        try(var world=new T11ParentHttp(subcase,"original")) {
            world.allActions();
            var contributions=world.rows("mulino_work_WorkContributions");
            assertEquals(2,contributions.size(),world.actions.toPrettyString());
            String parent=world.results.get("parent").path("response").path("workId").asText();
            String canonical=world.results.get("receive90-canonical").path("response").path("id").asText();
            assertTrue(contributions.stream().allMatch(r->parent.equals(r.get("targetworkid"))&&canonical.equals(r.get("occurrenceid"))));
            assertEquals(0,new BigDecimal("90").compareTo(contributions.stream().map(r->(BigDecimal)r.get("quantity")).reduce(BigDecimal.ZERO,BigDecimal::add)));
            assertEquals(Set.of(new BigDecimal("0.000000000000"),new BigDecimal("30.000000000000")),new HashSet<>(contributions.stream().map(r->(BigDecimal)r.get("startquantity")).toList()));
            assertEquals("UNSATISFIED",world.results.get("parent-assessment").path("response").path("data").path("outcome").asText());
            assertEquals(new BigDecimal("90.000000000000"),world.rows("mulino_trade_receipt_Receipts").getFirst().get("quantity"));
            assertEquals(1,world.rows("mulino_inventory_QuantityMovements").size(),"Only the original receive90 may create arrival movement");
            // Do not hide downstream failure behind a weakened expected result.
            assertEquals("APPLIED",world.results.get("child30-close").path("response").path("outcome").asText(),world.actions.path("child30-close").toPrettyString());
            String child=world.results.get("child30").path("response").path("workId").asText();
            assertEquals("CLOSED",world.rows("mulino_work_read_Works").stream().filter(r->child.equals(r.get("id"))).findFirst().orElseThrow().get("status"));
            assertEquals("UNSATISFIED",world.results.get("parent-assessment").path("response").path("data").path("outcome").asText());
            assertEquals("REJECTED",world.results.get("false-close").path("response").path("outcome").asText());
            assertEquals("APPLIED",world.results.get("alternative-close").path("response").path("outcome").asText());
        }
    }
    @ParameterizedTest @ValueSource(strings={"overlap","overflow","missingCanonical","crossOrganization","revokedGrant"})
    void invalidContributionRangesAndAuthorityHaveNoEffects(String variant) throws Exception {
        try(var world=new T11ParentHttp("parent90-cancelled",variant)) {
            world.through("child30-link");assertEquals("APPLIED",world.results.get("child30-link").path("response").path("outcome").asText());
            var before=world.rows("mulino_work_WorkContributions");var links=world.rows("mulino_work_WorkLinks");
            world.execute("child60-link-current-work");
            var request=(ObjectNode)new ReferenceResolver(world.results,world.aliases).resolve(world.action("child60-link").path("request"));request.put("commandIdempotencyKey",UUID.randomUUID().toString());
            var slots=(ObjectNode)request.path("slots");
            if(variant.equals("overlap"))slots.put("startQuantity","0");
            if(variant.equals("overflow"))slots.put("startQuantity","80");
            if(variant.equals("missingCanonical"))slots.put("occurrenceId",UUID.randomUUID().toString());
            if(variant.equals("crossOrganization"))try(var other=new T11ParentHttp("parent90-cancelled","foreign")) {
                other.through("receive90");slots.put("occurrenceId",other.results.get("receive90-canonical").path("response").path("id").asText());
            }
            if(variant.equals("revokedGrant"))try(var s=world.db.prepareStatement("UPDATE mulino_identity_Grants SET revokedAt=? WHERE organizationId=? AND actorId=?")) {
                s.setObject(1,java.time.OffsetDateTime.parse("2026-10-07T02:00:00Z"));s.setString(2,world.org);s.setString(3,world.aliases.path("A").asText());assertTrue(s.executeUpdate()>0);
            }
            var response=world.send("negative-"+variant,"A","/api/ontology/commands/createWorkLink",request);
            assertTrue(Set.of("REJECTED","CONFLICT","HELD").contains(response.path("outcome").asText()),response.toPrettyString());
            if(variant.equals("overlap"))assertEquals("ACTUAL_RANGE_ALREADY_CREDITED",response.path("error").path("code").asText());
            assertEquals(before,world.rows("mulino_work_WorkContributions"));assertEquals(links,world.rows("mulino_work_WorkLinks"));
            assertEquals(1,world.rows("mulino_inventory_QuantityMovements").size());
        }
    }
}
