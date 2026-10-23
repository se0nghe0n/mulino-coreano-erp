package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import java.math.BigDecimal;
import java.util.*;

/** Closed coordinator-selected corrections; the original tree and byte identity never change. */
final class ModelFixtureInputCorrection {
    record Correction(ObjectNode fixture,ArrayNode ledger) {}
    static Correction apply(JsonNode original,JsonNode input,String hash) {
        ObjectNode effective=original.deepCopy();ArrayNode ledger=Json.array();
        if(input.isMissingNode())return new Correction(effective,ledger);
        if(!input.path("version").asText().equals("model-unit-normalization-v1")||!original.path("fixtureId").asText().equals("M59-isolated"))throw new IllegalArgumentException("Unapproved input correction");
        Set<String> allowed=Set.of("version","fromUnit","toUnit","factor");input.fieldNames().forEachRemaining(k->{if(!allowed.contains(k))throw new IllegalArgumentException("Unknown normalization field: "+k);});
        JsonNode item=original.at("/baseline/domainFacts/objects/P");
        if(!item.path("baseUnit").asText().equals("EA")||item.path("scale").asInt()!=0||!input.path("fromUnit").asText().equals("BOX")||!input.path("toUnit").asText().equals("EA")||!input.path("factor").asText().equals("12")||!item.at("/conversion/BOX_to_EA").asText().equals("12"))throw new IllegalArgumentException("Explicit original BOX_to_EA conversion required");
        for(String alias:List.of("A","B","O1","S1")) {
            String pointer="/baseline/domainFacts/objects/"+alias+(alias.equals("O1")?"/goal":"");ObjectNode target=(ObjectNode)effective.at(pointer);
            if(!target.path("unit").asText().equals("BOX"))throw new IllegalArgumentException("Unexpected original normalization unit");
            replace(target,"quantity",Json.MAPPER.valueToTree(new BigDecimal(target.path("quantity").asText()).multiply(new BigDecimal("12")).toPlainString()),pointer+"/quantity",hash,ledger);
            replace(target,"unit",Json.MAPPER.valueToTree("EA"),pointer+"/unit",hash,ledger);
            if(target.hasNonNull("allocated"))replace(target,"allocated",Json.MAPPER.valueToTree(new BigDecimal(target.path("allocated").asText()).multiply(new BigDecimal("12")).toPlainString()),pointer+"/allocated",hash,ledger);
        }
        return new Correction(effective,ledger);
    }
    private static void replace(ObjectNode owner,String key,JsonNode after,String pointer,String hash,ArrayNode ledger) {
        var entry=Json.object().put("fixtureSha256",hash).put("pointer",pointer).put("originalInputProvided",false).put("originalAuthorityClaimed",false).put("operatingApprovalClaimed",false).put("inverse","RESTORE_EXACT_BEFORE");entry.set("before",owner.path(key).deepCopy());entry.set("after",after);ledger.add(entry);owner.set(key,after);
    }
    private ModelFixtureInputCorrection() {}
}
