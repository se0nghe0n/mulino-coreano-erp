package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.util.*;

/**
 * User decision 2026-10-11 (3): every case command (any route: api, mcp, worker, batch operations, tool arguments) is
 * checked against the product's published per-capability slot names, contracts/capability-slots.json, which the backend
 * itself loads and enforces. A command naming a slot its capability does not publish, or calling a capability the product
 * does not publish, is a Step 2 case finding: it is reported (KNOWN_OPEN owner=Step 2) instead of being repaired here.
 * Actions that declare action.harness.intentionalViolation are exempt; they send invalid shapes on purpose.
 * A missing or malformed contract is a preparation problem.
 */
final class CapabilitySlotCheck {
    static final String CONTRACT="contracts/capability-slots.json";
    private CapabilitySlotCheck() {}

    static ObjectNode run(Path root,Map<String,JsonNode> cases,List<String> problems,ArrayNode knownOpen) {
        ObjectNode out=Json.object();out.put("contract",CONTRACT);
        Map<String,Set<String>> slots=new TreeMap<>();
        try {
            JsonNode contract=Json.read(root.resolve(CONTRACT));out.put("contractSha256",Json.sha256(root.resolve(CONTRACT)));
            for(var it=contract.path("capabilities").fields();it.hasNext();){var e=it.next();var names=new TreeSet<String>();for(JsonNode n:e.getValue().path("slots"))names.add(n.asText());if(names.isEmpty())throw new IllegalArgumentException("capability "+e.getKey()+" publishes no slots");slots.put(e.getKey(),names);}
            if(slots.isEmpty())throw new IllegalArgumentException("no capabilities");
        } catch(Exception e) {problems.add("Capability slot contract unreadable: "+CONTRACT+": "+e.getMessage());out.put("status","FAIL");return out;}
        int checked=0,exempt=0;ArrayNode violations=Json.array();Map<String,Integer> unpublished=new TreeMap<>();Map<String,List<String>> byCase=new TreeMap<>();
        for(var c:cases.entrySet()) for(JsonNode sub:c.getValue().path("subcases")) for(JsonNode action:actions(sub.path("actions"))) {
            List<JsonNode> intents=new ArrayList<>();intents(action.path("request"),intents);
            for(JsonNode intent:intents) {
                String cap=intent.path("capabilityId").asText();
                if(!slots.containsKey(cap)){unpublished.merge(cap,1,Integer::sum);continue;}
                checked++;
                var extra=new TreeSet<String>();intent.path("slots").fieldNames().forEachRemaining(n->{if(!slots.get(cap).contains(n))extra.add(n);});
                if(extra.isEmpty())continue;
                if(!action.path("harness").path("intentionalViolation").isMissingNode()&&!action.path("harness").path("intentionalViolation").isNull()){exempt++;continue;}
                String where=c.getKey()+"/"+sub.path("id").asText()+"/"+action.path("id").asText();
                ObjectNode v=violations.addObject();v.put("caseId",c.getKey()).put("subcaseId",sub.path("id").asText()).put("actionId",action.path("id").asText()).put("capabilityId",cap);v.set("unpublishedSlots",Json.MAPPER.valueToTree(extra));
                byCase.computeIfAbsent(c.getKey(),k->new ArrayList<>()).add(where+" "+cap+" "+extra);
            }
        }
        for(var e:byCase.entrySet())knownOpen.add(Json.object().put("check","capability-slots").put("gap","KNOWN_OPEN CAPABILITY-SLOT owner=Step 2 case="+e.getKey()+" count="+e.getValue().size()+" example="+e.getValue().getFirst()));
        for(var e:unpublished.entrySet())knownOpen.add(Json.object().put("check","capability-slots").put("gap","KNOWN_OPEN CAPABILITY-UNPUBLISHED owner=Step 3 capability="+e.getKey()+" count="+e.getValue()));
        out.put("status",violations.isEmpty()&&unpublished.isEmpty()?"PASS":"FINDINGS").put("checkedCommands",checked).put("exemptIntentionalViolations",exempt).put("violatingCommands",violations.size());
        out.set("unpublishedCapabilities",Json.MAPPER.valueToTree(unpublished));out.set("violations",violations);
        return out;
    }
    /** Every action, including branch actions, in authored order. */
    static List<JsonNode> actions(JsonNode list) {
        List<JsonNode> out=new ArrayList<>();for(JsonNode a:list){out.add(a);for(JsonNode b:a.path("branches"))out.addAll(actions(b.path("actions")));}return out;
    }
    /** Command intents anywhere in a request: an object with a capabilityId string and a slots object (RECORD/COMMAND or unstated kind). */
    static void intents(JsonNode node,List<JsonNode> out) {
        if(node.isObject()) {
            // A stored Definition package (user decision 2026-10-11 (2), e.g. createDefinitionDraft slots.package) lists verbs
            // {name,intentKind,capabilityId,stage,slots} whose slots map slot names to types; those are vocabulary, not commands.
            if(node.path("nouns").isArray()&&node.path("verbs").isArray())return;
            String kind=node.path("intentKind").asText("");
            if(node.path("capabilityId").isTextual()&&node.path("slots").isObject()&&(kind.isEmpty()||kind.equals("COMMAND")||kind.equals("RECORD")))out.add(node);
            node.elements().forEachRemaining(v->intents(v,out));
        } else if(node.isArray()) node.elements().forEachRemaining(v->intents(v,out));
    }
}
