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
    @ParameterizedTest @ValueSource(strings={"parent90-cancelled","parent90-impossible","parent90-superseded"})
    void sameCanonicalClosesBothChildrenAndPreservesParent90(String subcase) throws Exception {
        try(var world=new T11ParentHttp(subcase,"source-pins")) {
            world.through("child30-link");
            assertEquals("APPLIED",world.results.get("child30-link").path("response").path("outcome").asText());
            freshAssessment(world,"parent");
            assertParent90(world);
            // Continue original inputs; the injected observations do not change any original action.
            boolean after=false;
            for(var action:world.subcase.path("actions")){
                if(after&&Set.of("invoke","query").contains(action.path("kind").asText()))world.execute(action.path("id").asText());
                if(action.path("id").asText().equals("child30-link"))after=true;
                if(action.path("id").asText().equals("parent-assessment"))break;
            }
            for(String name:List.of("child30","child60")){
                assertEquals("SATISFIED",world.results.get(name+"-assessment").path("response").path("data").path("outcome").asText());
                assertEquals("APPLIED",world.results.get(name+"-close").path("response").path("outcome").asText(),world.actions.toPrettyString());
                String child=world.results.get(name).path("response").path("workId").asText();
                assertEquals("CLOSED",world.rows("mulino_work_read_Works").stream().filter(r->child.equals(r.get("id"))).findFirst().orElseThrow().get("status"));
            }
            assertParent90(world);
            var rows=world.rows("mulino_work_WorkContributions");assertEquals(2,rows.size());
            assertEquals(2,rows.stream().map(r->r.get("id")).distinct().count());
            String parent=world.results.get("parent").path("response").path("workId").asText();
            for(var row:rows){
                assertEquals(parent,row.get("targetworkid"));
                assertNotEquals(parent,row.get("sourceworkid"));assertNotNull(row.get("sourcegoalversionid"));assertEquals("Receipt.quantity",row.get("sourceconditionid"));
                var child=world.rows("mulino_work_read_Works").stream().filter(w->Objects.equals(w.get("id"),row.get("sourceworkid"))).findFirst().orElseThrow();
                assertEquals(child.get("currentgoalversionid"),row.get("sourcegoalversionid"));
            }
            assertEquals(1,world.rows("mulino_evidence_CanonicalOccurrences").size());
            assertEquals(1,world.rows("mulino_trade_receipt_Receipts").size());
            assertEquals(1,world.rows("mulino_inventory_QuantityMovements").size());
            // Actual gateway replay must keep the very same allocation IDs and Work revisions.
            var original=world.results.get("child30-link");
            assertEquals(original.path("response"),world.send("link-replay","A","/api/ontology/commands/createWorkLink",original.path("request")));
            assertEquals(rows,world.rows("mulino_work_WorkContributions"));
        }
    }
    @Test void rangeViewsAndPartialParentCreditKeepOriginalIdentity() throws Exception {
        try(var world=new T11ParentHttp("parent90-cancelled","views")) {
            world.through("child30-link");
            freshAssessment(world,"parent");assertParent90(world);
            freshAssessment(world,"child30");
            assertEquals("SATISFIED",world.results.get("child30-assessment").path("response").path("data").path("outcome").asText());
            world.execute("child30-close-current-work");var sourceView=world.results.get("child30-close-current-work").path("response").path("data").path("contributions");
            var source=java.util.stream.StreamSupport.stream(sourceView.spliterator(),false).filter(v->"SOURCE".equals(v.path("attributionRole").asText())).toList();assertEquals(1,source.size());
            var allocation=world.rows("mulino_work_WorkContributions").getFirst();
            assertEquals(allocation.get("id"),source.getFirst().path("ID").asText());assertEquals(allocation.get("goalid"),source.getFirst().path("goalId").asText());assertEquals(allocation.get("sourcegoalversionid"),source.getFirst().path("boundGoalId").asText());
            var original=world.results.get("child30-link");assertEquals(original.path("response"),world.send("range-replay","A","/api/ontology/commands/createWorkLink",original.path("request")));
            assertEquals(1,world.rows("mulino_work_WorkContributions").size());
            world.execute("child60-link-current-work");world.execute("child60-link");world.execute("child60-assessment-evaluation-work");world.execute("child60-assessment-evaluate");world.execute("child60-assessment");
            assertEquals("SATISFIED",world.results.get("child60-assessment").path("response").path("data").path("outcome").asText());
            freshAssessment(world,"parent");assertParent90(world);
            var rows=world.rows("mulino_work_WorkContributions");assertEquals(2,rows.size());assertEquals(2,rows.stream().map(r->r.get("id")).distinct().count());
            assertTrue(rows.stream().allMatch(r->r.get("sourcegoalversionid")!=null&&r.get("sourceconditionid").equals("Receipt.quantity")));
            for(String table:List.of("mulino_evidence_CanonicalOccurrences","mulino_trade_receipt_Receipts","mulino_inventory_QuantityMovements"))assertEquals(1,world.rows(table).size());
        }
    }
    @Test void sourceGoalRevisionDoesNotReuseHistoricalAllocation() throws Exception {
        try(var world=new T11ParentHttp("parent90-cancelled","revision")) {
            world.through("child30-assessment");
            var allocation=world.rows("mulino_work_WorkContributions");
            world.execute("child30-close-current-work");
            var current=world.results.get("child30-close-current-work").path("response").path("data");
            String child=current.path("ID").asText();
            var request=Json.object().put("intentKind","COMMAND").put("capabilityId","reviseGoal").put("definitionVersion",world.results.get("child30-link").path("request").path("definitionVersion").asText()).put("commandIdempotencyKey",UUID.randomUUID().toString()).put("expectedRevision",current.path("revision").asInt());
            request.set("subjectRefs",Json.array().add(Json.object().put("type","Work").put("id",child)));
            request.set("slots",Json.object().put("workId",child).put("newDueAt","2026-10-30T00:00:00Z").put("previousGoalVersion",current.path("currentGoal").path("version").asInt()).put("reason","New goal must not inherit old source range"));request.set("provenance",Json.object().put("newDueAt","USER"));
            var revised=world.send("revise-source","A","/api/ontology/commands/reviseGoal",request);assertEquals("APPLIED",revised.path("outcome").asText(),revised.toPrettyString());
            freshAssessment(world,"child30");
            assertEquals("UNVERIFIED",world.results.get("child30-assessment").path("response").path("data").path("outcome").asText());
            assertEquals(allocation,world.rows("mulino_work_WorkContributions"));
            world.execute("child30-close-current-work");world.execute("child30-close");assertEquals("HELD",world.results.get("child30-close").path("response").path("outcome").asText());
        }
    }
    private void freshAssessment(T11ParentHttp world,String name) throws Exception {
        world.execute(name+"-assessment-evaluation-work");
        var request=(ObjectNode)new ReferenceResolver(world.results,world.aliases).resolve(world.action(name+"-assessment-evaluate").path("request"));
        request.put("commandIdempotencyKey",UUID.randomUUID().toString());
        var actor=world.action(name+"-assessment-evaluate").path("actorRef").asText();
        var response=world.send(name+"-assessment-evaluate",actor,"/api/ontology/commands/assessGoal",request);
        assertEquals("APPLIED",response.path("outcome").asText(),response.toPrettyString());
        world.execute(name+"-assessment");
    }
    private void assertParent90(T11ParentHttp world) throws Exception {
        assertEquals("UNSATISFIED",world.results.get("parent-assessment").path("response").path("data").path("outcome").asText());
        String parent=world.results.get("parent").path("response").path("workId").asText();
        try(var s=world.db.prepareStatement("SELECT contentJson FROM mulino_evaluation_InputSnapshots WHERE organizationId=? AND workId=? AND assessmentId=?")){
            s.setString(1,world.org);s.setString(2,parent);s.setString(3,world.results.get("parent-assessment").path("response").path("data").path("ID").asText());try(var r=s.executeQuery()){assertTrue(r.next());var facts=Json.parse(r.getString(1)).path("facts").path("properties").path("Receipt.quantity");assertEquals(1,facts.size(),facts.toPrettyString());assertEquals(0,new BigDecimal("90").compareTo(new BigDecimal(facts.get(0).path("value").asText())));}
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
