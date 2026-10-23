package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class T17SourceIdentityReferenceTest {
    final String id="f41d0264-dc2b-4aa6-9026-562ee906ff8c";
    JsonNode reference(String pointer){var ref=Json.object().put("actionId","setup").put("pointer",pointer);return Json.object().set("$result",ref);}
    ObjectNode receipt(){var row=Json.object().put("documentId",id).put("canonicalOccurrenceId",id).put("status","ALLOWED").put("quantity","20");row.set("decisionReceipts",Json.array().add(Json.object().put("canonicalOccurrenceId",id)).add(Json.object().put("canonicalOccurrenceId",id)));return row;}
    ObjectNode step(ObjectNode receipt){var conditions=Json.array().add(receipt);var data=Json.object().set("sourceFactEvidence",Json.object().set("conditions",conditions));var step=Json.object().put("driverStatus","EXECUTED");step.set("provenance",Json.object().put("scopeComplete",true));step.set("data",data);return step;}
    @Test void actualDocumentAndBothConflictCanonicalIdentitiesBindWithoutObservedStatus(){
        var row=receipt();var resolver=new ReferenceResolver(Map.of("setup",step(row)),Json.object());
        for(String leaf:List.of("documentId","canonicalOccurrenceId","decisionReceipts/0/canonicalOccurrenceId","decisionReceipts/1/canonicalOccurrenceId"))assertEquals(id,resolver.identity(reference("/data/sourceFactEvidence/conditions/0/"+leaf)).asText());
        for(String leaf:List.of("status","quantity","evaluatorVersion","given/status","evidenceRefs")){
            String pointer="/data/sourceFactEvidence/conditions/0/"+leaf;assertFalse(ReferenceResolver.identityPointer(pointer));assertThrows(IllegalArgumentException.class,()->resolver.identity(reference(pointer)));
        }
    }
    @Test void aliasTextQuantityBooleanObjectAndNoncanonicalUuidCannotStandInForAProof(){
        for(JsonNode value:List.of(Json.MAPPER.valueToTree("agency-proof"),Json.MAPPER.valueToTree("1-1-1-1-1"),Json.MAPPER.valueToTree(20),Json.MAPPER.valueToTree(false),Json.object(),Json.MAPPER.nullNode())){
            var row=receipt();row.set("documentId",value);var resolver=new ReferenceResolver(Map.of("setup",step(row)),Json.object());if(value.isNull())assertThrows(AssertionError.class,()->resolver.identity(reference("/data/sourceFactEvidence/conditions/0/documentId")));else assertThrows(IllegalArgumentException.class,()->resolver.identity(reference("/data/sourceFactEvidence/conditions/0/documentId")));
        }
    }
    @Test void receiptPermissionIsLimitedToExactTypedLeavesAndAvailableActions(){
        for(String pointer:List.of("/response/documentId","/data/conditions/0/documentId","/data/sourceFactEvidence/conditions/00/documentId","/data/sourceFactEvidence/conditions/-1/documentId","/data/sourceFactEvidence/conditions/0/decisionReceipts/2/canonicalOccurrenceId","/data/sourceFactEvidence/conditions/0/decisionReceipts/0/status"))assertFalse(ReferenceResolver.identityPointer(pointer),pointer);
        var resolver=new ReferenceResolver(Map.of("setup",StepResult.missing("setup","NOT_RUN").toJson()),Json.object());assertThrows(AssertionError.class,()->resolver.identity(reference("/data/sourceFactEvidence/conditions/0/documentId")));
    }
}
