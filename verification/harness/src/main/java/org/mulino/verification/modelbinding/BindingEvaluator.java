package org.mulino.verification.modelbinding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.*;
import java.math.BigDecimal;
import java.util.*;

/** Observation-only arithmetic over immutable rows. No eligibility, policy or workflow simulation. */
public final class BindingEvaluator {
    private final BindingContract contract;
    public BindingEvaluator(BindingContract contract){this.contract=contract;}
    private static final Map<String,String> OPERATIONS=Map.ofEntries(
        Map.entry("dispatchCount","DISPATCH"),Map.entry("newDispatchCount","DISPATCH"),Map.entry("newWarehouseDispatchCount","WAREHOUSE_DISPATCH"),
        Map.entry("allocationConsumptionCount","CONSUME_ALLOCATION"),Map.entry("allocationCount","CREATE_ALLOCATION"),Map.entry("newAllocationCount","CREATE_ALLOCATION"),
        Map.entry("receiptCount","RECORD_RECEIPT"),Map.entry("newReceiptCount","RECORD_RECEIPT"),Map.entry("returnCount","RECEIVE_RETURN"),Map.entry("newReturnCount","RECEIVE_RETURN"),
        Map.entry("approvalCount","APPROVE"),Map.entry("recallApprovalCount","APPROVE_RECALL"),Map.entry("authorizationEscalationCount","ESCALATE_AUTHORITY"),
        Map.entry("bankTransferCount","BANK_TRANSFER"),Map.entry("customerDeliveryCount","RECORD_DELIVERY"),Map.entry("definitionPublishCount","PUBLISH_DEFINITION"),
        Map.entry("deliveryCorrectionCount","CORRECT_DELIVERY"),Map.entry("externalSendCount","EXTERNAL_SEND"),Map.entry("externalSubmissionCount","EXTERNAL_SUBMISSION"),
        Map.entry("goalMigrationCount","MIGRATE_GOAL"),Map.entry("holdReleaseCount","RELEASE_HOLD"),Map.entry("humanApprovalRequestCount","REQUEST_HUMAN_APPROVAL"),
        Map.entry("lossAdjustmentCount","ADJUST_LOSS"),Map.entry("movementCount","MOVE"),Map.entry("newMovementCount","MOVE"),Map.entry("newOutboxCount","CREATE_OUTBOX"),
        Map.entry("newWorkCount","CREATE_WORK"),Map.entry("taxIssueCount","ISSUE_TAX_INVOICE"),Map.entry("allocationSuspensionCount","SUSPEND_ALLOCATION"),Map.entry("reauthorizationObligationCount","CREATE_REAUTHORIZE_OBLIGATION"));
    public static JsonNode aliases(JsonNode node,JsonNode aliases){
        if(node.isTextual() && aliases.has(node.asText())) return aliases.get(node.asText()).deepCopy();
        if(node.isObject()){ObjectNode r=Json.object();node.fields().forEachRemaining(e->r.set(e.getKey(),aliases(e.getValue(),aliases)));return r;}
        if(node.isArray()){ArrayNode r=Json.array();node.forEach(v->r.add(aliases(v,aliases)));return r;}return node.deepCopy();
    }
    public static List<JsonNode> newEffects(JsonNode before,JsonNode after){
        Map<String,JsonNode> b=index(before),a=index(after);for(var e:b.entrySet()) BindingContract.require(a.containsKey(e.getKey())&&a.get(e.getKey()).equals(e.getValue()),"Effect deleted or changed after turn");
        return a.entrySet().stream().filter(e->!b.containsKey(e.getKey())).map(Map.Entry::getValue).toList();
    }
    private static Map<String,JsonNode> index(JsonNode rows){BindingContract.require(rows.isArray(),"Effects scope unobserved");Map<String,JsonNode> r=new LinkedHashMap<>();for(JsonNode row:rows){String id=Json.required(row,"id");BindingContract.require(r.put(id,row)==null,"Duplicate effect ID");BindingContract.require(row.path("classes").isArray()&&!row.path("classes").isEmpty(),"Effect classes missing");}return r;}
    public static boolean business(JsonNode e){for(JsonNode c:e.path("classes")) if(!Set.of("READ_AUDIT","COMMAND_AUDIT").contains(c.asText())) return true;return false;}
    public void effectWhitelist(JsonNode oracle,JsonNode path,List<JsonNode> effects,JsonNode aliases){
        Map<String,JsonNode> allowed=new HashMap<>();for(JsonNode a:oracle.path("allowedEffects")) allowed.put(a.path("class").asText(),a);
        for(JsonNode a:path.path("allowedEffects")){String k=a.path("class").asText();BindingContract.require(!allowed.containsKey(k),"Conflicting path effect allowance "+k);allowed.put(k,a);}
        Map<String,Integer> counts=new HashMap<>();
        for(JsonNode e:effects) for(JsonNode c:e.path("classes")) {
            String k=c.asText();BindingContract.require(allowed.containsKey(k),"Unlisted/forbidden effect class "+k);counts.merge(k,1,Integer::sum);
            JsonNode scope=allowed.get(k).path("scope");if(scope.isObject()) {
                for(String key:List.of("actorRef","organizationRef","authorizationGrantRef")) if(scope.has(key)) BindingContract.require(e.path(key).equals(aliases(scope.path(key),aliases)),"Audit scope mismatch "+key);
                if(scope.has("targetRefs")){JsonNode targets=aliases(scope.path("targetRefs"),aliases);boolean found=false;for(JsonNode target:targets) found|=target.equals(e.path("targetRef"));BindingContract.require(found,"Audit target outside grant");}
                if(scope.path("selfAccessContextOnly").asBoolean()) BindingContract.require(!e.path("accessContextActorRef").isMissingNode()&&e.path("accessContextActorRef").equals(e.path("actorRef")),"Other actor access context disclosed");
            }
        }
        for(var e:counts.entrySet()) BindingContract.require(e.getValue()<=allowed.get(e.getKey()).path("maxNew").asInt(-1),"Effect allowance exceeded "+e.getKey());
        if(path.has("businessEffectsMaximum")) BindingContract.require(effects.stream().filter(BindingEvaluator::business).count()<=path.path("businessEffectsMaximum").asInt(),"Path business effects exceed bound");
    }
    /** Path derives from actual tool records; a model-authored completion label is ignored. */
    public String selectPath(JsonNode oracle,JsonNode transcript,List<JsonNode> effects,JsonNode aliases,JsonNode fixture){
        BindingContract.require(transcript.path("terminal").asBoolean(false),"Host invocation not terminal");
        JsonNode calls=transcript.path("toolCalls");BindingContract.require(calls.isArray(),"Host tool calls unobserved");
        int commands=0;boolean grounded=false;
        List<JsonNode> witnesses=new ArrayList<>();calls.forEach(witnesses::add);transcript.path("contextReads").forEach(witnesses::add);
        for(JsonNode call:witnesses) {
            BindingContract.require(call.path("wireArtifactRef").isTextual() && call.path("authenticated").asBoolean(false),"Tool call lacks actual authenticated wire evidence");
            String profile=Json.required(call,"actorProfile");JsonNode actor=fixture.path("actors").path(profile);
            BindingContract.require(!actor.isMissingNode(),"Unknown tool actor profile");
            for(String key:List.of("issuer","subject","audience")) BindingContract.require(call.path(key).equals(actor.path(key)),"Tool principal mismatch "+key);
            BindingContract.require(call.path("organizationRef").equals(aliases.get(actor.path("organizationAlias").asText())),"Tool organization mismatch");
            BindingContract.require(call.path("authorizationGrantRef").equals(aliases.get(actor.path("grant").path("scope").path("grantAlias").asText())),"Tool grant substitution");
            if(call.path("kind").asText().equals("READ")) {
                java.time.Instant time=java.time.Instant.parse(fixture.path("clock").path("asOf").asText());JsonNode grant=actor.path("grant");BindingContract.require(!time.isBefore(java.time.Instant.parse(grant.path("validFrom").asText()))&&time.isBefore(java.time.Instant.parse(grant.path("validUntil").asText()))&&grant.path("scope").path("revokedAt").isNull(),"Read witness grant is not current");
                JsonNode permitted=aliases(grant.path("scope").path("targetAliases"),aliases);BindingContract.require(call.path("targetRefs").isArray()&&!call.path("targetRefs").isEmpty(),"Read target scope unobserved");for(JsonNode target:call.path("targetRefs")){boolean allowed=false;for(JsonNode id:permitted)allowed|=id.equals(target);BindingContract.require(allowed,"Read witness outside grant targets");}
                BindingContract.require(actor.path("grant").path("actions").toString().contains("\"READ\""),"Read has no READ grant");
                if(call.path("constraintFacts").isArray()&&!call.path("constraintFacts").isEmpty()) grounded=true;
            } else {commands++;BindingContract.require(profile.equals("command-actor"),"READ probe used for command");BindingContract.require(call.path("outcome").asText().equals(oracle.path("sitDirectCommand").path("expectedOutcome").asText()),"Actual rejection outcome differs");}
        }
        if(commands>0)return "SERVER_REJECTION";
        BindingContract.require(grounded,"Preflight lacks authenticated constraint facts");
        BindingContract.require(effects.stream().noneMatch(BindingEvaluator::business),"Preflight created business effect");return "EVIDENCED_PREFLIGHT_STOP";
    }
    public JsonNode value(String semantic,JsonNode execution,JsonNode observation,List<JsonNode> effects){return value(semantic,execution,observation,effects,null);}
    public JsonNode value(String semantic,JsonNode execution,JsonNode observation,List<JsonNode> effects,CapturedApiObservation api){
        JsonNode m=contract.mapping(semantic);
        switch(m.path("evidenceClass").asText()) {
            case "AUTHENTICATED_API": {BindingContract.require(api!=null,"Client final answer is not an authenticated API observation");JsonNode value=api.response(semantic).at(m.path("pointer").asText().substring("/response".length()));BindingContract.require(!value.isMissingNode()&&!value.isNull(),"Missing actual response "+semantic);return value;}
            case "INDEPENDENT_DB_ROWS": {
                BindingContract.require(observation.path("data").path("rawRows").isArray(),"Independent raw rows scope unobserved");List<JsonNode> matches=new ArrayList<>();for(JsonNode row:observation.path("data").path("rawRows")) {
                    if(row.path("dataset").equals(m.path("dataset"))&&row.path("attribute").equals(m.path("attribute"))) {
                        for(String key:List.of("table","column","rowId")) Json.required(row,key);
                        BindingContract.require(row.hasNonNull("value"),"Physical column null/missing "+semantic);matches.add(row);
                    }
                }
                if(m.has("reducer"))return reduce(m.path("reducer").asText(),matches);
                BindingContract.require(matches.size()==1,"Independent physical-column cardinality differs for "+semantic+": "+matches.size());return matches.get(0).get("value");
            }
            case "SCOPED_EFFECT_DELTA": return metric(m.path("metric").asText(),effects);
            default: throw new IllegalArgumentException("Unknown observation binding");
        }
    }
    private JsonNode reduce(String reducer,List<JsonNode> rows){
        if(reducer.equals("count")){Set<String> ids=new HashSet<>();for(JsonNode row:rows)BindingContract.require(ids.add(Json.required(row,"rowId")),"Duplicate independent raw row identity");return Json.MAPPER.valueToTree(ids.size());}
        ArrayNode collected=Json.array();Map<String,JsonNode> unique=new LinkedHashMap<>();String unit=null;
        for(JsonNode row:rows){String u=Json.required(row,"unit");if(unit==null)unit=u;BindingContract.require(unit.equals(u),"Mixed units in quantity aggregation");
            JsonNode q=row.path("value");BindingContract.require(q.isTextual()&&q.asText().matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?"),"Invalid raw decimal");collected.add(q);
            if(reducer.startsWith("uniquePhysical")||reducer.equals("distinctPhysicalCount")){String id=Json.required(row,"physicalScopeId");JsonNode previous=unique.putIfAbsent(id,q);BindingContract.require(previous==null||previous.equals(q),"Conflicting physical occurrence quantity");}}
        if(reducer.equals("collect"))return collected;if(reducer.equals("distinctPhysicalCount"))return Json.MAPPER.valueToTree(unique.size());
        BigDecimal sum=BigDecimal.ZERO;Iterable<JsonNode> values=reducer.equals("uniquePhysicalSum")?unique.values():collected;for(JsonNode value:values)sum=sum.add(new BigDecimal(value.asText()));return Json.MAPPER.valueToTree(sum.toPlainString());
    }
    private JsonNode metric(String name,List<JsonNode> effects){
        if(name.equals("businessEffectCount"))return Json.MAPPER.valueToTree(effects.stream().filter(BindingEvaluator::business).count());
        if(Set.of("newInventoryQuantity","newPhysicalQuantity","confirmedPhysicalQuantity").contains(name)) {
            String kind=name.equals("newInventoryQuantity")?"INVENTORY":name.equals("newPhysicalQuantity")?"PHYSICAL":"CONFIRMED_PHYSICAL";BigDecimal sum=BigDecimal.ZERO;
            for(JsonNode e:effects)for(JsonNode q:e.path("measurements"))if(q.path("kind").asText().equals(kind)){Json.required(q,"unit");sum=sum.add(new BigDecimal(Json.required(q,"value")));}
            return Json.MAPPER.valueToTree(sum.toPlainString());
        }
        String operation=OPERATIONS.get(name);BindingContract.require(operation!=null,"Unbound effect metric "+name);return Json.MAPPER.valueToTree(effects.stream().filter(e->e.path("operation").asText().equals(operation)).count());
    }
    public void assertSemantic(JsonNode assertion,JsonNode execution,JsonNode observation,List<JsonNode> effects,JsonNode aliases){assertSemantic(assertion,execution,observation,effects,aliases,null);}
    public void assertSemantic(JsonNode assertion,JsonNode execution,JsonNode observation,List<JsonNode> effects,JsonNode aliases,String expectedUnit){assertSemantic(assertion,execution,observation,effects,aliases,expectedUnit,null);}
    public void assertSemantic(JsonNode assertion,JsonNode execution,JsonNode observation,List<JsonNode> effects,JsonNode aliases,String expectedUnit,CapturedApiObservation api){
        if(expectedUnit!=null)checkExpectedUnit(assertion.path("path").asText(),execution,observation,effects,expectedUnit,api);
        JsonNode observed=value(assertion.path("path").asText(),execution,observation,effects,api),expected=aliases(assertion.path("expected"),aliases);
        ObjectNode a=Json.object();a.put("op",expected.isTextual()&&expected.asText().matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?")?"decimalEquals":"equals");a.set("expected",expected);a.set("source",Json.parse("{\"actionId\":\"observed\",\"pointer\":\"/data/value\"}"));
        ObjectNode sample=Json.object();sample.put("driverStatus","EXECUTED");sample.set("provenance",Json.parse("{\"scopeComplete\":true}"));sample.set("data",Json.object().set("value",observed));new AssertionEngine().check(a,Map.of("observed",sample));
    }
    private void checkExpectedUnit(String semantic,JsonNode execution,JsonNode observation,List<JsonNode> effects,String expected,CapturedApiObservation api){
        JsonNode m=contract.mapping(semantic);String type=m.path("evidenceClass").asText();
        if(type.equals("AUTHENTICATED_API")){String pointer=m.path("pointer").asText();int slash=pointer.lastIndexOf('/');BindingContract.require(api!=null,"API quantity has no captured source");BindingContract.require(api.response(semantic).at(pointer.substring("/response".length(),slash)+"/unit").asText().equals(expected),"API quantity unit mismatch");}
        else if(type.equals("INDEPENDENT_DB_ROWS")){boolean found=false;for(JsonNode row:observation.path("data").path("rawRows"))if(row.path("dataset").equals(m.path("dataset"))&&row.path("attribute").equals(m.path("attribute"))){found=true;BindingContract.require(row.path("unit").asText().equals(expected),"Independent quantity/currency unit mismatch");}if(!found)BindingContract.require(observation.path("data").path("data").path("units").path(m.path("dataset").asText()).asText().equals(expected),"Empty quantity scope lacks independently observed unit");}
        else {String name=m.path("metric").asText(),kind=name.equals("newInventoryQuantity")?"INVENTORY":name.equals("newPhysicalQuantity")?"PHYSICAL":"CONFIRMED_PHYSICAL";BindingContract.require(observation.path("data").path("data").path("units").path(kind).asText().equals(expected),"Effect quantity dimension/unit absent");for(JsonNode e:effects)for(JsonNode q:e.path("measurements"))if(q.path("kind").asText().equals(kind))BindingContract.require(q.path("unit").asText().equals(expected),"Effect quantity unit mismatch");}
    }
    public void preserveObligations(JsonNode before,JsonNode after,List<JsonNode> effects){
        BindingContract.require(before.isArray()&&after.isArray(),"Obligation snapshots unobserved");Map<String,JsonNode> current=new HashMap<>();for(JsonNode row:after)BindingContract.require(current.put(Json.required(row,"id"),row)==null,"Duplicate current obligation ID");
        for(JsonNode old:before){String id=Json.required(old,"id");JsonNode next=current.get(id);if(next!=null&&next.equals(old))continue;boolean authorizedEffect=false;for(JsonNode e:effects)if(e.path("obligationRef").asText().equals(id))for(JsonNode k:e.path("classes"))authorizedEffect|=Set.of("OBLIGATION","OBLIGATION_ASSIGNMENT").contains(k.asText());BindingContract.require(authorizedEffect,"Existing duty/owner changed without corresponding allowed obligation effect "+id);}
    }
    public void obligations(JsonNode oracle,JsonNode path,JsonNode rows,JsonNode aliases){
        List<JsonNode> needed=new ArrayList<>();oracle.path("obligations").forEach(needed::add);path.path("obligations").forEach(needed::add);
        BindingContract.require(rows.isArray(),"Current obligation rows unobserved");
        for(JsonNode duty:needed){JsonNode expected=aliases(duty,aliases);int matches=0;for(JsonNode row:rows){boolean match=true;for(var it=expected.fields();it.hasNext();){var e=it.next();match&=e.getValue().equals(row.path(e.getKey()));}if(match)matches++;}BindingContract.require(matches==1,"Missing/duplicated human duty "+expected);}
    }
}
