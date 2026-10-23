package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Coordinator-authorized corrected synthetic Given. Product authorization remains exact and unchanged. */
final class ModelFixtureAuthority {
    static final String VERSION="model-authority-correction-v1";
    private static final Map<String,String> ACTIONS=Map.ofEntries(
        Map.entry("PROPOSE_PURCHASE","proposePurchase"),Map.entry("RECORD_RECEIPT","confirmReceipt"),Map.entry("RECORD_ACTIVITY","recordActivity"),
        Map.entry("RECORD_SUBMISSION","recordSubmission"),Map.entry("RESERVE","reserveQuantity"),Map.entry("SPLIT","splitQuantity"),Map.entry("PICK","pickQuantity"),
        Map.entry("DISPATCH","dispatchQuantity"),Map.entry("RECORD_DELIVERY","recordDelivery"),Map.entry("AUTHORIZE_RETURN","authorizeReturn"),Map.entry("RECEIVE_RETURN","receiveReturn"),
        Map.entry("RECORD_INVOICE","recordInvoice"),Map.entry("RECORD_PAYMENT_REFERENCE","recordPaymentReference"),Map.entry("LINK_RELATION","recordRelation"),
        Map.entry("RECORD_STOCKTAKE","recordStocktake"),Map.entry("MOVE_INTERNAL","moveQuantity"),Map.entry("CLOSE_WORK","closeWork"),Map.entry("RESUME_WORK","resumeWork"),
        Map.entry("REVISE_PURCHASE","revisePurchase"),Map.entry("RECORD_SUPPLIER_REPLY","recordSupplierReply"),Map.entry("ATTACH_EVIDENCE","attachEvidence"),
        Map.entry("CORRECT_EVIDENCE","correctEvidence"),Map.entry("OPERATIONS_RETRY","retrySafeCommand"),Map.entry("RECALL_CLOSE","closeRecall"));
    record Correction(ObjectNode fixture,ArrayNode ledger,ArrayNode purposeBindings) {}
    static Correction apply(Path root,JsonNode original,JsonNode authorization,String fixtureHash) throws Exception {
        ObjectNode corrected=original.deepCopy();var ledger=Json.array();var purposeBindings=Json.array();
        if(authorization.isMissingNode())return new Correction(corrected,ledger,purposeBindings);
        authorization.fieldNames().forEachRemaining(k->{if(!Set.of("version","capabilityContractSha256","supplementalRead").contains(k))throw new IllegalArgumentException("Unknown authority correction field: "+k);});
        if(!authorization.path("version").asText().equals(VERSION))throw new IllegalArgumentException("Unknown authority correction version");
        Path contract=root.resolve("contracts/acceptance-capabilities.json");if(!Json.sha256(contract).equals(authorization.path("capabilityContractSha256").asText()))throw new IllegalArgumentException("Authority capability contract drift");
        var kinds=new LinkedHashMap<String,String>();for(JsonNode c:Json.read(contract).path("capabilities"))kinds.put(c.path("id").asText(),c.path("kind").asText());
        ObjectNode principals=(ObjectNode)corrected.path("baseline").path("principalFacts"),grants=(ObjectNode)corrected.path("baseline").path("grantFacts");
        // Human profiles supply only the actions explicitly intersecting their own original grant (M57 ADMIN/READ).
        for(var entry:fields(principals)) {
            String alias=entry.getKey();ObjectNode principal=(ObjectNode)entry.getValue();var semanticRoles=new LinkedHashSet<String>();principal.path("roles").forEach(r->semanticRoles.add(r.asText()));
            for(JsonNode profile:original.path("actors"))if(profile.path("subject").equals(principal.path("subject")))profile.path("roleCapabilities").forEach(r->semanticRoles.add(r.asText()));
            var allowedKinds=new HashSet<String>();for(String role:semanticRoles)switch(role){case "READ"->allowedKinds.add("QUERY");case "WRITE"->{allowedKinds.add("COMMAND");allowedKinds.add("RECORD");}case "RECORD"->allowedKinds.add("RECORD");case "ADMIN"->{if(!principal.path("human").asBoolean())throw new IllegalArgumentException("ADMIN role completion requires explicit Human profile");allowedKinds.add("COMMAND");allowedKinds.add("RECORD");}default->throw new IllegalArgumentException("Unknown semantic role: "+role);}
            var granted=new LinkedHashSet<String>();for(JsonNode grant:grants)if(grant.path("actorRef").asText().equals(alias))granted.addAll(translate(grant.path("actions"),kinds));
            var roles=Json.array();for(String capability:granted)if(allowedKinds.contains(kinds.get(capability)))roles.add(capability);
            replace(principal,"roles",roles,"/baseline/principalFacts/"+alias+"/roles",ledger,fixtureHash);
        }
        var originals=fields(grants);
        for(var entry:originals) {
            String alias=entry.getKey();ObjectNode grant=(ObjectNode)entry.getValue();var actions=Json.array();translate(grant.path("actions"),kinds).forEach(actions::add);
            replace(grant,"actions",actions,"/baseline/grantFacts/"+alias+"/actions",ledger,fixtureHash);
            var originalBinding=Json.object().put("purpose","ORIGINAL_TRANSLATED_GRANT").put("originalGrantAlias",alias).put("grantId",original.path("aliases").path(alias).asText()).put("originalGrantIdentityProvided",true).put("originalInputProvided",false).put("originalAuthorityClaimed",false).put("operatingApprovalClaimed",false);originalBinding.set("exactGrant",grant.deepCopy());purposeBindings.add(originalBinding);
            String actor=grant.path("actorRef").asText();String delegator=grant.path("delegatorRef").asText(principals.path(actor).path("delegatorRef").asText());
            if(delegator.isEmpty())for(JsonNode profile:original.path("actors"))if(profile.path("grant").path("scope").path("grantAlias").asText().equals(alias))delegator=profile.path("grant").path("delegatorAlias").asText();
            if(delegator.isEmpty())throw new IllegalArgumentException("Explicit delegator missing for "+alias);
            if(!grant.has("delegatorRef"))replace(grant,"delegatorRef",Json.MAPPER.valueToTree(delegator),"/baseline/grantFacts/"+alias+"/delegatorRef",ledger,fixtureHash);
            if(authorization.path("supplementalRead").asBoolean()&&(grant.has("sourceNamespace")||!grant.path("sourceNamespaces").isEmpty())&&original.path("baseline").path("grantFacts").path(alias).path("actions").toString().contains("\"READ\"")) {
                String readAlias="synthetic-context-read:"+alias;
                var read=grant.deepCopy();read.remove(List.of("sourceNamespace","sourceNamespaces"));var readActions=Json.array();kinds.forEach((cap,kind)->{if(kind.equals("QUERY"))readActions.add(cap);});read.set("actions",readActions);
                String readId=UUID.nameUUIDFromBytes((fixtureHash+":"+readAlias).getBytes(StandardCharsets.UTF_8)).toString();
                replace(grants,readAlias,read,"/baseline/grantFacts/"+readAlias,ledger,fixtureHash);replace((ObjectNode)corrected.path("aliases"),readAlias,Json.MAPPER.valueToTree(readId),"/aliases/"+readAlias,ledger,fixtureHash);
                var supplementalBinding=Json.object().put("purpose","CONTEXT_READ").put("originalGrantAlias",alias).put("originalGrantId",original.path("aliases").path(alias).asText()).put("supplementalGrantAlias",readAlias).put("supplementalGrantId",readId).put("principalAlias",actor).put("originalInputProvided",false).put("originalAuthorityClaimed",false).put("operatingApprovalClaimed",false);supplementalBinding.set("exactGrant",read.deepCopy());purposeBindings.add(supplementalBinding);
                if(delegator.equals("manager")) {
                    String rootAlias="synthetic-manager-read-root:"+alias;var readRoot=read.deepCopy().put("actorRef","manager").put("delegatorRef","manager");
                    replace(grants,rootAlias,readRoot,"/baseline/grantFacts/"+rootAlias,ledger,fixtureHash);replace((ObjectNode)corrected.path("aliases"),rootAlias,Json.MAPPER.valueToTree(UUID.nameUUIDFromBytes((fixtureHash+":"+rootAlias).getBytes(StandardCharsets.UTF_8)).toString()),"/aliases/"+rootAlias,ledger,fixtureHash);
                }
            }
            // One root per original grant avoids a scope/action/time Cartesian product.
            if(delegator.equals("manager")&&principals.path("manager").path("human").asBoolean()) {
                String rootAlias="synthetic-manager-root:"+alias;String id=UUID.nameUUIDFromBytes((fixtureHash+":"+rootAlias).getBytes(StandardCharsets.UTF_8)).toString();
                var rootGrant=grant.deepCopy().put("actorRef","manager").put("delegatorRef","manager");
                replace(grants,rootAlias,rootGrant,"/baseline/grantFacts/"+rootAlias,ledger,fixtureHash);
                replace((ObjectNode)corrected.path("aliases"),rootAlias,Json.MAPPER.valueToTree(id),"/aliases/"+rootAlias,ledger,fixtureHash);
                var manager=(ObjectNode)principals.path("manager");var caps=new LinkedHashSet<String>();manager.path("roles").forEach(c->caps.add(c.asText()));actions.forEach(c->caps.add(c.asText()));var roles=Json.array();caps.forEach(roles::add);replace(manager,"roles",roles,"/baseline/principalFacts/manager/roles",ledger,fixtureHash);
            }
        }
        return new Correction(corrected,ledger,purposeBindings);
    }
    static List<String> translate(JsonNode actions,Map<String,String> kinds) {
        var result=new LinkedHashSet<String>();for(JsonNode action:actions) {
            String semantic=action.asText();if(semantic.equals("READ")){kinds.forEach((cap,kind)->{if(kind.equals("QUERY"))result.add(cap);});continue;}
            String cap=ACTIONS.get(semantic);if(cap==null||!kinds.containsKey(cap))throw new IllegalArgumentException("Unknown or ambiguous semantic action: "+semantic);result.add(cap);
        }return List.copyOf(result);
    }
    private static List<Map.Entry<String,JsonNode>> fields(JsonNode n){var out=new ArrayList<Map.Entry<String,JsonNode>>();n.fields().forEachRemaining(out::add);return out;}
    private static void replace(ObjectNode owner,String key,JsonNode after,String pointer,ArrayNode ledger,String hash) {
        JsonNode before=owner.get(key);if(Objects.equals(before,after))return;
        var correction=Json.object().put("fixtureSha256",hash).put("pointer",pointer).put("originalInputProvided",false).put("originalAuthorityClaimed",false).put("operatingApprovalClaimed",false).put("beforePresent",before!=null);
        correction.set("before",before==null?Json.MAPPER.nullNode():before.deepCopy());correction.set("after",after.deepCopy());correction.put("inverse",before==null?"REMOVE_EXACT_ADDITION":"RESTORE_EXACT_BEFORE");ledger.add(correction);owner.set(key,after);
    }
}
