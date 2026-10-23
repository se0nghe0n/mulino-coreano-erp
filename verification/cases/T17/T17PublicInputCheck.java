package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.mulino.application.core.CapabilitySlots;
import java.nio.file.*;
import java.util.*;
import org.mulino.verification.Json;
import org.mulino.verification.ReferenceResolver;

/** Uses the compiled public slot registry. Reports open contracts, never treats them as business PASS. */
public final class T17PublicInputCheck {
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]);var report=Json.object();var open=report.putArray("openSlotContracts");int corrected=0;
        Set<String> remaining=Set.of("replacement-no-revival/replace/evidenceId",
            "replacement-no-revival/allocation-release/reason","eligibility-fefo-with-reason/reserve/FEFOExceptionReason");
        for(JsonNode sub:Json.read(root.resolve("verification/cases/T17/case.json")).path("subcases"))for(JsonNode a:sub.path("actions")){
            if(!a.path("kind").asText().equals("invoke"))continue;
            String cap=a.path("capabilityId").asText();var slots=a.path("request").path("slots");
            for(var names=slots.fieldNames();names.hasNext();){String name=names.next();if(!CapabilitySlots.allowed(cap).contains(name)){
                String key=sub.path("id").asText()+"/"+a.path("id").asText()+"/"+name;
                if(!remaining.contains(key))throw new AssertionError("Unexpected unsupported slot "+key);
                open.add(key);
            }}
            if(Set.of("recordDelivery","recordObservedMovement").contains(cap)){
                String form=slots.has("canonicalOccurrenceId")?"confirmation":"observation";
                var names=new HashSet<String>();slots.fieldNames().forEachRemaining(names::add);
                if(!CapabilitySlots.form(cap,form).containsAll(names))throw new AssertionError("Wrong delivery form "+a.path("id"));
                corrected++;
            }
        }
        report.put("correctedDeliveryForms",corrected).put("businessOracleAcceptance",false);
        var refs=report.putObject("sourceEncoderIdentitySupport");
        for(String field:List.of("dispatchId","cargoScopeId","deliveryId","transitSegmentId","allocationId","workId"))
            refs.put(field,ReferenceResolver.identityPointer("/response/effects/"+field));
        Path target=root.resolve(".local/t17-public-contracts/compiled-input-check.json");Files.createDirectories(target.getParent());Files.writeString(target,report.toPrettyString());
        System.out.println(report.toPrettyString());
    }
}
