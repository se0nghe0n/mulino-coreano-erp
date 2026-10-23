package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import static org.mulino.verification.actual.T11GoalShortfallPostgresTest.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mulino.verification.Json;

/** Independent synthetic adversarial definitions/facts; never changes original T11 fixture/oracle bytes. */
@EnabledIfEnvironmentVariable(named="T11_PG_TEST",matches="true")
class T11ShortfallRemainingPostgresTest {
    static java.util.function.Consumer<ObjectNode> definition(String variant){return fixture->{
        try{
            var content=(ObjectNode)Json.read(Path.of(System.getProperty("repo.root"),"verification/actual/scenarios/installer-conventions.json")).path("definition").deepCopy();
            var goal=(ObjectNode)content.path("goals").get(0);var quantity=(ObjectNode)goal.path("predicate").deepCopy();
            if(variant.equals("ambiguous"))quantity.withObject("minimum").put("value","100");
            if(variant.equals("nonquantity")){
                ((com.fasterxml.jackson.databind.node.ArrayNode)content.path("attributes")).add(Json.object().put("nounType","Receipt").put("name","occurredAt").put("type","INSTANT").putNull("referenceType").putNull("unit").put("decimalPlaces",0).put("minimumCount",1).put("maximumCount",1).put("requiredStage","READ").put("core",true));
                goal.set("predicate",Json.object().put("operator","all").set("children",Json.array().add(quantity).add(Json.object().put("operator","timeIn").put("property","Receipt.occurredAt").put("minimum","2026-10-07T01:15:00Z").put("timezone","UTC").put("inclusiveStart",true).put("inclusiveEnd",false))));
            } else if(variant.equals("unsupported"))goal.set("predicate",Json.object().put("operator","any").set("children",Json.array().add(quantity).add(quantity.deepCopy())));
            else goal.set("predicate",quantity);
            ((ObjectNode)fixture.path("aliases")).set("SYNTHETIC_GOAL_DEFINITION",Json.object().put("type","DefinitionVersion").put("organizationAlias","ORG").set("content",content));
        }catch(Exception e){throw new IllegalStateException(e);}
    };}
    @ParameterizedTest @ValueSource(strings={"ambiguous","nonquantity","unsupported"})
    void actualPublishedConditionMustBeUniqueSupportedAndExclusivelyQuantitative(String variant) throws Exception {
        try(var w=new T11ParentHttp("parent90-cancelled","shortfall-contract-"+variant,definition(variant))){
            w.through("receive90");assertEquals("APPLIED",w.results.get("receive90").path("response").path("outcome").asText(),w.actions.toPrettyString());
            assess(w);assertEquals("UNSATISFIED",w.results.get("parent-assessment-evaluate").path("response").path("assessment").path("outcome").asText());if(variant.equals("nonquantity")){var conditions=Json.parse(w.results.get("parent-assessment-evaluate").path("response").path("assessment").path("conditionsJson").asText());assertTrue(java.util.stream.StreamSupport.stream(conditions.spliterator(),false).anyMatch(c->c.path("conditionId").asText().equals("root.0.1")&&c.path("truth").path("state").asText().equals("UNSATISFIED")));}syntheticOpenGrant(w);
            var before=w.rows("mulino_work_read_ObligationReferences");var canonical=w.rows("mulino_evidence_CanonicalOccurrences");var moves=w.rows("mulino_inventory_QuantityMovements");
            var denied=w.send("ambiguous-shortfall","A","/api/ontology/commands/openObligation",request(w));assertEquals("HELD",denied.path("outcome").asText(),denied.toPrettyString());assertEquals("ASSESSMENT_NOT_CURRENT",denied.path("error").path("code").asText());assertTrue(denied.path("effects").isEmpty());assertTrue(roots(w).isEmpty());assertEquals(before,w.rows("mulino_work_read_ObligationReferences"));assertEquals(canonical,w.rows("mulino_evidence_CanonicalOccurrences"));assertEquals(moves,w.rows("mulino_inventory_QuantityMovements"));
        }
    }
    @ParameterizedTest @ValueSource(strings={"missing","unverified"})
    void actualMissingOrUnverifiedAssessmentDoesNotProveZeroOrTen(String state) throws Exception {
        try(var w=new T11ParentHttp("parent90-cancelled","shortfall-head-"+state)){
            if(state.equals("missing")){
                var draft=(ObjectNode)new org.mulino.verification.ReferenceResolver(w.results,w.aliases).resolve(w.action("parent").path("request"));draft.put("capabilityId","createDraft").put("commandIdempotencyKey",UUID.randomUUID().toString());var created=w.send("parent","A","/api/ontology/commands/createDraft",draft);assertEquals("APPLIED",created.path("outcome").asText(),created.toPrettyString());
                var activate=draft.deepCopy().put("capabilityId","activateWork").put("commandIdempotencyKey",UUID.randomUUID().toString()).put("expectedRevision",created.path("revision").asInt());activate.set("slots",Json.object().put("workId",parent(w)));activate.set("subjectRefs",Json.array().add(Json.object().put("type","Work").put("id",parent(w))));assertEquals("APPLIED",w.send("activate-no-assessment","A","/api/ontology/commands/activateWork",activate).path("outcome").asText());assertTrue(w.rows("mulino_work_read_AssessmentReferences").stream().noneMatch(a->parent(w).equals(a.get("workid"))));
            }else w.through("child60");
            if(state.equals("unverified")){assess(w);assertEquals("UNVERIFIED",w.results.get("parent-assessment-evaluate").path("response").path("assessment").path("outcome").asText());}
            syntheticOpenGrant(w);
            // This new grant is for a Work with no physical source yet. No existing grant is edited.
            try(var q=w.db.prepareStatement("DELETE FROM mulino_identity_GrantScopes WHERE organizationId=? AND scopeKind='PLACE' AND grantId IN (SELECT grantId FROM mulino_identity_GrantActions WHERE organizationId=? AND capabilityId='openObligation')")){q.setString(1,w.org);q.setString(2,w.org);assertEquals(1,q.executeUpdate());}
            Map<String,Object> source=state.equals("missing")?w.rows("mulino_commands_CommandRecords").stream().filter(r->"activateWork".equals(r.get("capabilityid"))&&r.get("resultjson").toString().contains(parent(w))).findFirst().orElseThrow():null;
            var input=source==null?request(w):request(w,source);var before=w.rows("mulino_work_read_ObligationReferences");
            var response=w.send("no-proof","A","/api/ontology/commands/openObligation",input);assertEquals("HELD",response.path("outcome").asText(),response.toPrettyString());assertEquals("ASSESSMENT_NOT_CURRENT",response.path("error").path("code").asText());assertTrue(roots(w).isEmpty());assertEquals(before,w.rows("mulino_work_read_ObligationReferences"));
            for(String table:List.of("mulino_evidence_CanonicalOccurrences","mulino_trade_receipt_Receipts","mulino_inventory_QuantityMovements"))assertTrue(w.rows(table).isEmpty());
        }
    }
    static String transfer(T11ParentHttp w) throws Exception {
        var opened=w.send("open-shortfall","A","/api/ontology/commands/openObligation",request(w));assertEquals("APPLIED",opened.path("outcome").asText(),opened.toPrettyString());
        String assignment=opened.path("effects").path("responsibility").path("assignments").get(0).path("ID").asText();w.execute("followup");String target=w.results.get("followup").path("response").path("workId").asText();
        try(var q=w.db.prepareStatement("INSERT INTO mulino_identity_GrantScopes(organizationId,grantId,scopeKind,scopeId) SELECT organizationId,ID,'WORK',? FROM mulino_identity_Grants WHERE organizationId=? AND actorId=? AND delegatorId=actorId AND ID IN (SELECT grantId FROM mulino_identity_GrantActions WHERE capabilityId='openObligation')")){q.setString(1,target);q.setString(2,w.org);q.setString(3,w.aliases.path("A").asText());assertEquals(1,q.executeUpdate());}
        var move=request(w);move.put("capabilityId","transferObligation");move.set("slots",Json.object().put("workId",parent(w)).put("targetWorkId",target).put("assignmentId",assignment).put("recipientId",w.aliases.path("A").asText()).put("quantity","10").put("nextAction","남은10상자 수령 확인").put("nextCheckAt","2026-10-08T09:00:00Z").put("expiresAt","2026-10-08T09:00:00Z"));
        var proposal=w.send("transfer","A","/api/ontology/commands/transferObligation",move);assertEquals("APPLIED",proposal.path("outcome").asText(),proposal.toPrettyString());var accept=request(w);accept.put("capabilityId","acceptHandover").put("expectedRevision",0);accept.set("slots",Json.object().put("handoverId",proposal.path("effects").path("responsibility").path("ID").asText()));assertEquals("APPLIED",w.send("accept","A","/api/ontology/commands/acceptHandover",accept).path("outcome").asText());return target;
    }
    static JsonNode send(T11ParentHttp w,String suffix,ObjectNode request) throws Exception {request.put("commandIdempotencyKey",UUID.randomUUID().toString());String kind=request.path("intentKind").asText().equals("RECORD")?"records":"commands";String actor=request.path("capabilityId").asText().equals("confirmReceipt")?"A":"receiptSource";var response=w.send("synthetic-five-"+suffix,actor,"/api/ontology/"+kind+"/"+request.path("capabilityId").asText(),request);assertEquals("APPLIED",response.path("outcome").asText(),response.toPrettyString());return response;}
    static ObjectNode original(T11ParentHttp w,String action){return (ObjectNode)w.results.get(action).path("request").deepCopy();}
    static void additionalFive(T11ParentHttp w) throws Exception {
        String range=UUID.randomUUID().toString(),external="NEW-SYNTHETIC-five-"+UUID.randomUUID();
        var payload=(ObjectNode)Json.parse(w.results.get("receive90-event").path("request").path("slots").path("payload").asText());payload.put("quantity","5").put("rangeRootId",range).put("canonicalOccurrenceKey",external);byte[] bytes=payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var doc=original(w,"receive90-event-document");((ObjectNode)doc.path("slots")).put("sourceReference",external).put("expectedHash",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))).put("contentBase64",Base64.getEncoder().encodeToString(bytes));String document=send(w,"document",doc).path("id").asText();
        var event=original(w,"receive90-event");((ObjectNode)event.path("slots")).put("externalEventId",external).put("quantity","5").put("documentId",document).put("payload",payload.toString());var eventResult=send(w,"event",event);
        var provisional=original(w,"receive90-provisional");((ObjectNode)provisional.path("slots")).put("rangeRootId",range).put("quantity","5").put("eventId",eventResult.path("id").asText());var receipt=send(w,"provisional",provisional);
        var match=original(w,"receive90-match");((ObjectNode)match.path("slots")).put("physicalScopeId",range).put("quantity","5").put("claimId",eventResult.path("claimId").asText()).put("basisDocumentId",document).put("sourceIdentity","synthetic-receipt:"+external+":1");var reconciliation=send(w,"match",match);
        var link=original(w,"receive90-canonical");((ObjectNode)link.path("slots")).put("reconciliationId",reconciliation.path("id").asText());var canonical=send(w,"canonical",link);
        var confirm=original(w,"receive90");((ObjectNode)confirm.path("slots")).put("receiptId",receipt.path("effects").path("receiptId").asText()).put("canonicalOccurrenceId",canonical.path("id").asText());send(w,"confirm",confirm);
    }
    @ParameterizedTest @ValueSource(strings={"source","goal","policy","identity"})
    void currentInvalidationRetainsTransferredRootWithoutEffects(String change) throws Exception {
        try(var w=new T11ParentHttp("parent90-cancelled","shortfall-current-"+change)){
            w.through("parent-assessment");syntheticOpenGrant(w);String target=transfer(w);var initial=roots(w);String root=initial.getFirst().get("id").toString();var assignments=w.rows("mulino_work_read_ObligationReferences").stream().filter(a->root.equals(a.get("rootid"))).toList();
            switch(change){
                case "source"->{w.send("later","A","/__verification/runtime/clock",Json.object().put("instant","2026-10-07T02:00:00.000001Z"));var correction=original(w,"receive90-event").put("capabilityId","correctEvidence").put("commandIdempotencyKey",UUID.randomUUID().toString());((ObjectNode)correction.path("slots")).put("supersedesId",w.results.get("receive90-event").path("response").path("id").asText()).put("sourceVersion","2");assertEquals("APPLIED",w.send("source-correction","A","/api/ontology/records/correctEvidence",correction).path("outcome").asText());}
                case "goal"->{var goal=w.rows("mulino_work_read_GoalReferences").stream().filter(g->Objects.equals(g.get("id"),workUnchecked(w).get("currentgoalversionid"))).findFirst().orElseThrow();var revision=request(w).put("capabilityId","reviseGoal");revision.set("slots",Json.object().put("workId",parent(w)).put("previousGoalVersion",((Number)goal.get("goalversion")).intValue()).put("newDueAt","2026-10-30T00:00:00Z").put("reason","NEW synthetic revision negative"));assertEquals("APPLIED",w.send("goal-revision","A","/api/ontology/commands/reviseGoal",revision).path("outcome").asText());}
                case "policy"->{try(var q=w.db.prepareStatement("DELETE FROM mulino_governance_ActivePolicies WHERE organizationId=? AND kind='EVIDENCE'")){q.setString(1,w.org);assertEquals(1,q.executeUpdate());}}
                case "identity"->{try(var q=w.db.prepareStatement("UPDATE mulino_identity_Grants SET revokedAt='2026-10-07T02:00:00Z' WHERE organizationId=? AND ID IN (SELECT grantId FROM mulino_identity_GrantActions WHERE organizationId=? AND capabilityId='openObligation')")){q.setString(1,w.org);q.setString(2,w.org);assertEquals(1,q.executeUpdate());}}
            }
            var response=w.send("retain-invalidated","A","/api/ontology/commands/openObligation",request(w));assertTrue(Set.of("HELD","REJECTED","CONFLICT").contains(response.path("outcome").asText()),response.toPrettyString());assertTrue(response.path("effects").isEmpty());
            if(Set.of("source","policy").contains(change)){assertEquals("HELD",response.path("outcome").asText());assertEquals(root,response.path("responsibility").path("rootId").asText());assertEquals("OPEN",response.path("responsibility").path("status").asText());}
            assertEquals(initial,roots(w));assertEquals(assignments,w.rows("mulino_work_read_ObligationReferences").stream().filter(a->root.equals(a.get("rootid"))).toList());assertEquals(1,assignments.stream().filter(a->target.equals(a.get("workid"))&&"OPEN".equals(a.get("status"))).count());physicalUnchanged(w);
        }
    }
    @Test void changedActualDeficitPreservesTenAndTransferredOwner() throws Exception {
        try(var w=new T11ParentHttp("parent90-cancelled","shortfall-actual-progress")){
            w.through("parent-assessment");syntheticOpenGrant(w);String target=transfer(w);var initial=roots(w);var initialAssignments=w.rows("mulino_work_read_ObligationReferences").stream().filter(a->initial.getFirst().get("id").equals(a.get("rootid"))).toList();
            additionalFive(w);assess(w);var assessment=w.results.get("parent-assessment-evaluate").path("response").path("assessment");assertEquals("UNSATISFIED",assessment.path("outcome").asText());
            var facts=Json.parse(assessment.path("conditionsJson").asText()).get(0).path("inputs");BigDecimal recognized=BigDecimal.ZERO;for(var fact:facts)recognized=recognized.add(new BigDecimal(fact.path("value").asText()));assertEquals(0,new BigDecimal("95").compareTo(recognized));
            for(String quantity:List.of("5","10")){var r=request(w);((ObjectNode)r.path("slots")).put("quantity",quantity);var held=w.send("retain-for-"+quantity,"A","/api/ontology/commands/openObligation",r);assertEquals("HELD",held.path("outcome").asText(),held.toPrettyString());assertEquals(initial.getFirst().get("id"),held.path("responsibility").path("rootId").asText());assertTrue(held.path("effects").isEmpty());}
            assertEquals(initial,roots(w));var current=w.rows("mulino_work_read_ObligationReferences").stream().filter(a->initial.getFirst().get("id").equals(a.get("rootid"))).toList();assertEquals(initialAssignments,current);assertEquals(1,current.stream().filter(a->"OPEN".equals(a.get("status"))&&target.equals(a.get("workid"))).count());assertEquals(new BigDecimal("10.000000000000"),initial.getFirst().get("quantity"));
            assertEquals(2,w.rows("mulino_evidence_CanonicalOccurrences").size());assertEquals(2,w.rows("mulino_trade_receipt_Receipts").size());assertEquals(2,w.rows("mulino_inventory_QuantityMovements").size());assertEquals(2,w.rows("mulino_work_WorkContributions").size());assertEquals(0,new BigDecimal("95").compareTo(w.rows("mulino_trade_receipt_Receipts").stream().map(x->(BigDecimal)x.get("quantity")).reduce(BigDecimal.ZERO,BigDecimal::add)));
            assertEquals(1,w.rows("mulino_work_read_ObligationReferences").stream().filter(a->"RECEIPT_UNALLOCATED".equals(a.get("kind"))&&new BigDecimal("90").compareTo((BigDecimal)a.get("quantity"))==0&&"OPEN".equals(a.get("status"))).count());
        }
    }
}
