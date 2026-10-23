package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mulino.verification.Json;
import org.mulino.verification.ReferenceResolver;

/** New diagnostic inputs only; original T11 fixtures, actions and 110 oracles remain unchanged. */
@EnabledIfEnvironmentVariable(named="T11_PG_TEST",matches="true")
class T11GoalShortfallPostgresTest {
    static String parent(T11ParentHttp w){return w.results.get("parent").path("response").path("workId").asText();}
    static Map<String,Object> work(T11ParentHttp w) throws Exception{return w.rows("mulino_work_read_Works").stream().filter(r->parent(w).equals(r.get("id"))).findFirst().orElseThrow();}
    static List<Map<String,Object>> roots(T11ParentHttp w) throws Exception{
        var rows=w.rows("mulino_responsibility_Roots").stream().filter(r->"GOAL_QUANTITY_SHORTFALL".equals(r.get("kind"))).toList();
        var evidence=Json.object().put("sql","SELECT row_to_json(r) FROM mulino_responsibility_Roots r WHERE organizationId=? AND kind=? ORDER BY ID");evidence.set("boundParameters",Json.array().add(w.org).add("GOAL_QUANTITY_SHORTFALL"));var raw=Json.array();
        try(var q=w.db.prepareStatement(evidence.path("sql").asText())){q.setString(1,w.org);q.setString(2,"GOAL_QUANTITY_SHORTFALL");try(var result=q.executeQuery()){while(result.next())raw.add(Json.parse(result.getString(1)));}}evidence.set("rows",raw);Json.write(w.evidence.resolve("independent-roots.json"),evidence);return rows;
    }
    /** Explicit NEW synthetic open-only grant. Does not change any original capability/grant row. */
    static void syntheticOpenGrant(T11ParentHttp w) throws Exception {
        String actor=w.aliases.path("A").asText(),grant=UUID.randomUUID().toString();
        try(var s=w.db.prepareStatement("INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil,revision,createdAt,recordedAt) VALUES (?,?,?,'openObligation','ORGANIZATION',?,'2026-10-01T00:00:00Z','2026-11-01T00:00:00Z',1,'2026-10-07T02:00:00Z','2026-10-07T02:00:00Z')")){s.setString(1,w.org);s.setString(2,UUID.randomUUID().toString());s.setString(3,actor);s.setString(4,w.org);s.executeUpdate();}
        try(var s=w.db.prepareStatement("INSERT INTO mulino_identity_Grants(organizationId,ID,actorId,delegatorId,validFrom,validUntil,revision,createdAt,recordedAt) VALUES (?,?,?,?,'2026-10-01T00:00:00Z','2026-11-01T00:00:00Z',1,'2026-10-07T02:00:00Z','2026-10-07T02:00:00Z')")){s.setString(1,w.org);s.setString(2,grant);s.setString(3,actor);s.setString(4,actor);s.executeUpdate();}
        try(var s=w.db.prepareStatement("INSERT INTO mulino_identity_GrantActions(organizationId,grantId,capabilityId) VALUES (?,?,'openObligation')")){s.setString(1,w.org);s.setString(2,grant);s.executeUpdate();}
        try(var s=w.db.prepareStatement("INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) VALUES (?,?,?,?)")){
            var target=work(w);for(var scope:List.of(List.of("WORK",parent(w)),List.of("TARGET",parent(w)),List.of("ITEM",target.get("itemid").toString()),List.of("PLACE",w.rows("mulino_trade_receipt_Receipts").getFirst().get("placeid").toString()))){s.setString(1,w.org);s.setString(2,grant);s.setString(3,scope.get(0));s.setString(4,scope.get(1));s.executeUpdate();}
        }
        Json.write(w.evidence.resolve("NEW-synthetic-open-grant.json"),Json.object().put("actorId",actor).put("grantId",grant).put("capability","openObligation").put("originalFixturesUnchanged",true));
    }
    static ObjectNode request(T11ParentHttp w) throws Exception {
        var assessment=w.results.get("parent-assessment-evaluate").path("response").path("assessment");String aid=assessment.path("ID").asText();
        var source=w.rows("mulino_commands_CommandRecords").stream().filter(r->"assessGoal".equals(r.get("capabilityid"))&&r.get("resultjson")!=null&&r.get("resultjson").toString().contains(aid)).findFirst().orElseThrow();
        var goal=w.rows("mulino_work_read_GoalReferences").stream().filter(r->Objects.equals(workUnchecked(w).get("currentgoalversionid"),r.get("id"))).findFirst().orElseThrow();
        var scope=(ObjectNode)Json.parse(goal.get("slotsjson").toString()).path("scope").deepCopy();scope.put("workId",parent(w)).put("goalVersionId",goal.get("id").toString()).put("conditionId","Receipt.quantity");
        var p=Json.object().put("kind","GOAL_QUANTITY_SHORTFALL").put("workId",parent(w)).put("sourceKind","DECISION").put("sourceId",source.get("id").toString()).put("sourceVersion",source.get("canonicalhash").toString()).put("scopeJson",scope.toString()).put("quantity","10").put("unit","BOX").put("nextAction","남은10상자 수령 확인").put("nextCheckAt","2026-10-08T09:00:00Z");
        var r=Json.object().put("intentKind","COMMAND").put("definitionVersion","definition-v1").put("capabilityId","openObligation").put("commandIdempotencyKey",UUID.randomUUID().toString()).put("expectedRevision",((Number)work(w).get("revision")).intValue());r.set("slots",p);r.set("subjectRefs",Json.array().add(Json.object().put("type","Work").put("id",parent(w))));var provenance=Json.object();p.fieldNames().forEachRemaining(k->provenance.put(k,"USER"));r.set("provenance",provenance);return r;
    }
    static Map<String,Object> workUnchecked(T11ParentHttp w){try{return work(w);}catch(Exception e){throw new RuntimeException(e);}}
    static void assess(T11ParentHttp w) throws Exception{
        w.execute("parent-assessment-evaluation-work");var r=(ObjectNode)new ReferenceResolver(w.results,w.aliases).resolve(w.action("parent-assessment-evaluate").path("request"));r.put("commandIdempotencyKey",UUID.randomUUID().toString());assertEquals("APPLIED",w.send("parent-assessment-evaluate","assessmentEvaluator","/api/ontology/commands/assessGoal",r).path("outcome").asText());
    }
    static void physicalUnchanged(T11ParentHttp w) throws Exception{for(String table:List.of("mulino_evidence_CanonicalOccurrences","mulino_trade_receipt_Receipts","mulino_inventory_QuantityMovements"))assertEquals(1,w.rows(table).size(),table);assertEquals(2,w.rows("mulino_work_WorkContributions").size());assertEquals(new BigDecimal("90.000000000000"),w.rows("mulino_trade_receipt_Receipts").getFirst().get("quantity"));}
    @Test void explicitProofPins100Minus90AndReusesRoot() throws Exception{
        try(var w=new T11ParentHttp("parent90-cancelled","shortfall-positive")){
            w.through("parent-assessment");syntheticOpenGrant(w);var r=request(w);
            var a=w.send("open-shortfall","A","/api/ontology/commands/openObligation",r);assertEquals("APPLIED",a.path("outcome").asText(),a.toPrettyString());
            String root=a.path("effects").path("responsibility").path("rootId").asText();assertEquals(1,roots(w).size());var original=roots(w).getFirst();assertEquals(new BigDecimal("10.000000000000"),original.get("quantity"));
            var proof=Json.parse(original.get("scopejson").toString()).path("proof");assertEquals("100",proof.path("targetQuantity").asText());assertEquals("90",proof.path("recognizedQuantity").asText());assertEquals("10",proof.path("shortfallQuantity").asText());
            assertEquals(a,w.send("same-key","A","/api/ontology/commands/openObligation",r));
            r.put("commandIdempotencyKey",UUID.randomUUID().toString());assertEquals(root,w.send("new-key","A","/api/ontology/commands/openObligation",r).path("effects").path("responsibility").path("rootId").asText());
            assess(w);r=request(w);assertEquals(root,w.send("new-assessment","A","/api/ontology/commands/openObligation",r).path("effects").path("responsibility").path("rootId").asText());assertEquals(original,roots(w).getFirst());
            var publicAssess=(ObjectNode)w.results.get("parent-assessment-evaluate").path("request").deepCopy();publicAssess.put("commandIdempotencyKey",UUID.randomUUID().toString());assertEquals("FORBIDDEN",w.send("open-is-not-assess","A","/api/ontology/commands/assessGoal",publicAssess).path("error").path("code").asText());
            physicalUnchanged(w);
            for(String child:List.of("child30","child60"))assertEquals("APPLIED",w.results.get(child+"-close").path("response").path("outcome").asText());
        }
    }
    @Test void transferRepeatKeepsActualSuccessorAndOriginalNinetyDuty() throws Exception {
        try(var w=new T11ParentHttp("parent90-cancelled","shortfall-transfer")){
            w.through("parent-assessment");syntheticOpenGrant(w);var opened=w.send("open-shortfall","A","/api/ontology/commands/openObligation",request(w));assertEquals("APPLIED",opened.path("outcome").asText(),opened.toPrettyString());
            var root=roots(w).getFirst();String assignment=opened.path("effects").path("responsibility").path("assignments").get(0).path("ID").asText();
            w.execute("followup");String target=w.results.get("followup").path("response").path("workId").asText();
            try(var q=w.db.prepareStatement("INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) SELECT organizationId,ID,'WORK',? FROM mulino_identity_Grants WHERE organizationId=? AND actorId=? AND delegatorId=actorId AND ID IN (SELECT grantId FROM mulino_identity_GrantActions WHERE capabilityId='openObligation')")){q.setString(1,target);q.setString(2,w.org);q.setString(3,w.aliases.path("A").asText());assertEquals(1,q.executeUpdate());}
            var transfer=request(w);transfer.put("capabilityId","transferObligation");transfer.set("slots",Json.object().put("workId",parent(w)).put("targetWorkId",target).put("assignmentId",assignment).put("recipientId",w.aliases.path("A").asText()).put("quantity","10").put("nextAction","남은10상자 수령 확인").put("nextCheckAt","2026-10-08T09:00:00Z").put("expiresAt","2026-10-08T09:00:00Z"));
            var proposal=w.send("propose-transfer","A","/api/ontology/commands/transferObligation",transfer);assertEquals("APPLIED",proposal.path("outcome").asText(),proposal.toPrettyString());
            var accept=request(w);accept.put("capabilityId","acceptHandover").put("expectedRevision",0);accept.set("slots",Json.object().put("handoverId",proposal.path("effects").path("responsibility").path("ID").asText()));
            var accepted=w.send("accept-transfer","A","/api/ontology/commands/acceptHandover",accept);assertEquals("APPLIED",accepted.path("outcome").asText(),accepted.toPrettyString());
            assess(w);var repeat=w.send("repeat-after-transfer","A","/api/ontology/commands/openObligation",request(w));assertEquals("APPLIED",repeat.path("outcome").asText(),repeat.toPrettyString());
            var current=repeat.path("effects").path("responsibility").path("assignments");assertEquals(1,current.size());assertEquals(target,current.get(0).path("workId").asText());assertEquals(w.aliases.path("A").asText(),current.get(0).path("ownerId").asText());assertEquals("OPEN",current.get(0).path("status").asText());assertNotEquals(assignment,current.get(0).path("ID").asText());assertEquals(root,roots(w).getFirst());
            assertEquals(1,w.rows("mulino_work_read_ObligationReferences").stream().filter(a->target.equals(a.get("workid"))&&"OPEN".equals(a.get("status"))).count());
            assertEquals(1,w.rows("mulino_work_read_ObligationReferences").stream().filter(a->parent(w).equals(a.get("workid"))&&"RECEIPT_UNALLOCATED".equals(a.get("kind"))&&"OPEN".equals(a.get("status"))&&new BigDecimal("90").compareTo((BigDecimal)a.get("quantity"))==0).count());physicalUnchanged(w);
        }
    }
    @Test void concurrentNewKeysAndFaultRollbackKeepOneAtomicRoot() throws Exception {
        try(var w=new T11ParentHttp("parent90-cancelled","shortfall-race")){
            w.through("parent-assessment");syntheticOpenGrant(w);var request=request(w);
            var control=Json.object().put("operation","arm").put("organizationId",w.org).put("point","command.beforeCommit").put("capabilityId","openObligation").put("commandKey",request.path("commandIdempotencyKey").asText()).put("occurrences",1);
            var armed=w.send("arm-rollback","A","/__verification/runtime/fault",control);assertEquals("ARMED",armed.path("state").asText());
            var priorScopes=w.rows("mulino_responsibility_Scopes");var priorAssignments=w.rows("mulino_work_read_ObligationReferences");
            var failed=w.send("rolled-back","A","/api/ontology/commands/openObligation",request);assertNotEquals("APPLIED",failed.path("outcome").asText());assertTrue(roots(w).isEmpty());assertEquals(priorScopes,w.rows("mulino_responsibility_Scopes"));assertEquals(priorAssignments,w.rows("mulino_work_read_ObligationReferences"));var fired=w.send("confirm-rollback-fault","A","/__verification/runtime/fault",Json.object().put("operation","disarm").put("faultId",armed.path("faultId").asText()));assertEquals("FIRED",fired.path("state").asText());assertEquals(1,fired.path("fired").asInt());
            var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var ready=new java.util.concurrent.CountDownLatch(2);var go=new java.util.concurrent.CountDownLatch(1);var responses=new ArrayList<JsonNode>();
            try {
                var jobs=new ArrayList<java.util.concurrent.Future<JsonNode>>();
                for(int i=0;i<2;i++){var input=request.deepCopy().put("commandIdempotencyKey",UUID.randomUUID().toString());jobs.add(pool.submit(()->{
                    var credential=(ObjectNode)ActualFixtureBindings.credentialActor(w.root,w.config,w.fixture.path("actors").path("A"));credential.put("organizationAlias",w.installed.path("organizationExternalAlias").asText());
                    var httpRequest=java.net.http.HttpRequest.newBuilder(w.config.baseUri().resolve("/api/ontology/commands/openObligation")).header("Content-Type","application/json").header("Authorization","Bearer "+w.signer.sign(credential)).POST(java.net.http.HttpRequest.BodyPublishers.ofString(input.toString())).build();ready.countDown();go.await();return Json.parse(w.http.send(httpRequest,java.net.http.HttpResponse.BodyHandlers.ofString()).body());}));}
                assertTrue(ready.await(10,java.util.concurrent.TimeUnit.SECONDS));go.countDown();for(var job:jobs)responses.add(job.get(30,java.util.concurrent.TimeUnit.SECONDS));
            } finally {pool.shutdownNow();}
            Json.write(w.evidence.resolve("concurrent-responses.json"),Json.MAPPER.valueToTree(responses));for(var response:responses)assertEquals("APPLIED",response.path("outcome").asText(),response.toPrettyString());assertEquals(responses.get(0).path("effects").path("responsibility").path("rootId"),responses.get(1).path("effects").path("responsibility").path("rootId"));assertEquals(responses.get(0).path("effects").path("responsibility").path("assignments").get(0).path("ID"),responses.get(1).path("effects").path("responsibility").path("assignments").get(0).path("ID"));assertEquals(1,roots(w).size());physicalUnchanged(w);
        }
    }
    @Test void changedPolicyRetainsFirstProofAndCurrentResponsibilityWithoutEffects() throws Exception {
        try(var w=new T11ParentHttp("parent90-cancelled","shortfall-review")){
            w.through("parent-assessment");syntheticOpenGrant(w);assertEquals("APPLIED",w.send("open-shortfall","A","/api/ontology/commands/openObligation",request(w)).path("outcome").asText());var before=roots(w);var assignments=w.rows("mulino_work_read_ObligationReferences");
            try(var q=w.db.prepareStatement("DELETE FROM mulino_governance_ActivePolicies WHERE organizationId=? AND kind='EVIDENCE'")){q.setString(1,w.org);assertEquals(1,q.executeUpdate());}
            var held=w.send("review-required","A","/api/ontology/commands/openObligation",request(w));assertEquals("HELD",held.path("outcome").asText(),held.toPrettyString());assertEquals(before.getFirst().get("id"),held.path("responsibility").path("rootId").asText());assertEquals("OPEN",held.path("responsibility").path("status").asText());assertTrue(held.path("effects").isEmpty());assertEquals(before,roots(w));assertEquals(assignments,w.rows("mulino_work_read_ObligationReferences"));physicalUnchanged(w);
        }
    }
    @Test void firstRootProofMustBeDatabaseImmutable() throws Exception {
        try(var w=new T11ParentHttp("parent90-cancelled","shortfall-immutable")){
            w.through("parent-assessment");syntheticOpenGrant(w);var opened=w.send("open-shortfall","A","/api/ontology/commands/openObligation",request(w));assertEquals("APPLIED",opened.path("outcome").asText(),opened.toPrettyString());
            w.db.setAutoCommit(false);
            try {assertThrows(java.sql.SQLException.class,()->{try(var q=w.db.prepareStatement("UPDATE mulino_responsibility_Roots SET sourceVersion='mutated' WHERE organizationId=? AND ID=?")){q.setString(1,w.org);q.setString(2,roots(w).getFirst().get("id").toString());q.executeUpdate();}});}finally {w.db.rollback();w.db.setAutoCommit(true);}
        }
    }
    @ParameterizedTest @ValueSource(strings={"quantity","unit","condition","goal","scope","source","hash","sourceKind","nextCheck","policy","revoked","alias","staleAssessment","sourceWork","wrongPlace","corrected","crossOrg","callerPurpose"})
    void invalidProofOrAuthorityCreatesNoRoot(String variant) throws Exception{
        try(var w=new T11ParentHttp("parent90-cancelled","shortfall-negative-"+variant)){
            w.through("parent-assessment");syntheticOpenGrant(w);var r=request(w);var p=(ObjectNode)r.path("slots");
            switch(variant){
                case "quantity"->p.put("quantity","11");case "unit"->p.put("unit","KG");case "source"->p.put("sourceId",UUID.randomUUID().toString());case "hash"->p.put("sourceVersion","0".repeat(64));case "sourceKind"->p.put("sourceKind","OCCURRENCE");case "nextCheck"->p.put("nextCheckAt","2026-10-07T02:00:00Z");
                case "condition","goal","scope"->{var scope=(ObjectNode)Json.parse(p.path("scopeJson").asText());scope.put(variant.equals("condition")?"conditionId":variant.equals("goal")?"goalVersionId":"itemId",UUID.randomUUID().toString());p.put("scopeJson",scope.toString());}
                case "policy"->{try(var s=w.db.prepareStatement("DELETE FROM mulino_governance_ActivePolicies WHERE organizationId=? AND kind='EVIDENCE'")){s.setString(1,w.org);s.executeUpdate();}}
                case "revoked"->{try(var s=w.db.prepareStatement("UPDATE mulino_identity_Grants SET revokedAt='2026-10-07T02:00:00Z' WHERE organizationId=? AND actorId=?")){s.setString(1,w.org);s.setString(2,w.aliases.path("A").asText());s.executeUpdate();}}
                case "crossOrg"->{try(var other=new T11ParentHttp("parent90-cancelled","foreign-shortfall")){other.through("parent-assessment");var otherRequest=request(other);p.put("sourceId",otherRequest.path("slots").path("sourceId").asText()).put("sourceVersion",otherRequest.path("slots").path("sourceVersion").asText());}}
                case "callerPurpose"->p.put("evaluationPurpose","ASSESS_GOAL");
                case "sourceWork"->{var source=w.rows("mulino_commands_CommandRecords").stream().filter(x->"assessGoal".equals(x.get("capabilityid"))&&x.get("resultjson")!=null&&x.get("resultjson").toString().contains(w.results.get("child30-assessment-evaluate").path("response").path("effects").path("assessmentId").asText())).findFirst().orElseThrow();p.put("sourceId",source.get("id").toString()).put("sourceVersion",source.get("canonicalhash").toString());}
                case "wrongPlace"->{try(var q=w.db.prepareStatement("UPDATE mulino_identity_GrantScopes SET scopeId=? WHERE organizationId=? AND scopeKind='PLACE' AND grantId IN (SELECT grantId FROM mulino_identity_GrantActions WHERE capabilityId='openObligation')")){q.setString(1,UUID.randomUUID().toString());q.setString(2,w.org);q.executeUpdate();}}
                case "corrected"->{String later="2026-10-07T02:00:00.000001Z";w.send("correction-clock","A","/__verification/runtime/clock",Json.object().put("instant",later));var correction=(ObjectNode)w.results.get("receive90-event").path("request").deepCopy();correction.put("capabilityId","correctEvidence").put("commandIdempotencyKey",UUID.randomUUID().toString());((ObjectNode)correction.path("slots")).put("supersedesId",w.results.get("receive90-event").path("response").path("id").asText()).put("sourceVersion","2");assertEquals("APPLIED",w.send("correct","A","/api/ontology/records/correctEvidence",correction).path("outcome").asText());r.put("expectedRevision",((Number)work(w).get("revision")).intValue());}
                case "alias"->r.put("capabilityId","createObligation");
                case "staleAssessment"->{assess(w);r.put("expectedRevision",((Number)work(w).get("revision")).intValue());}
            }
            var response=w.send("invalid-"+variant,"A","/api/ontology/commands/"+r.path("capabilityId").asText(),r);assertTrue(Set.of("HELD","REJECTED","CONFLICT").contains(response.path("outcome").asText()),response.toPrettyString());assertTrue(roots(w).isEmpty());physicalUnchanged(w);
        }
    }
}
