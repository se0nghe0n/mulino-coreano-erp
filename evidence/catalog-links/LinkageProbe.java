package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.nio.file.*;
import java.util.*;

/** Read-only reproduction of one authored oracle's structural linkage, not product execution. */
public final class LinkageProbe {
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args[0]).toAbsolutePath(),casePath=Path.of(args[1]).toAbsolutePath();
        String oracleId="T11.dependency-and-followup",observation="shared-distinct-contribution";
        ObjectNode catalog=(ObjectNode)Json.read(root.resolve("verification/requirements/mandatory-oracles.json"));
        ArrayNode selected=Json.array();for(JsonNode o:catalog.path("oracles")) if(o.path("oracleId").asText().equals(oracleId)) selected.add(o);
        catalog.set("oracles",selected);ObjectNode c=(ObjectNode)Json.read(casePath);
        for(JsonNode sub:c.path("subcases")) {
            ArrayNode assertions=Json.array();for(JsonNode a:sub.path("assertions")) if(a.path("oracleRef").path("oracleId").asText().equals(oracleId)) assertions.add(a);
            ((ObjectNode)sub).set("assertions",assertions);
        }
        ContractValidator validator=new ContractValidator(root);List<String> valid=check(validator,catalog,c);
        ObjectNode missing=c.deepCopy(),wrongValue=c.deepCopy(),wrongUnit=c.deepCopy();boolean found=false;
        for(JsonNode sub:c.path("subcases")) for(JsonNode a:sub.path("assertions"))
            if(a.path("op").asText().equals("sumEquals") && a.path("expected").asText().equals("60")
                && a.path("oracleRef").path("observationNames").toString().contains(observation)) {
                found=true;String id=a.path("id").asText();
                for(JsonNode s:missing.path("subcases")) ((ArrayNode)s.path("assertions")).removeAll();
                // Keep original support only, making the absence of a substantive quantity test visible.
                for(JsonNode s:c.path("subcases")) for(JsonNode b:s.path("assertions")) if(!b.path("id").asText().equals(id))
                    ((ArrayNode)missing.path("subcases").get(0).path("assertions")).add(b.deepCopy());
                for(JsonNode s:wrongValue.path("subcases")) for(JsonNode b:s.path("assertions")) if(b.path("id").asText().equals(id)) ((ObjectNode)b).put("expected","59");
                for(JsonNode s:wrongUnit.path("subcases")) for(JsonNode b:s.path("assertions")) if(b.path("id").asText().equals(id)) ((ObjectNode)b).put("unit","EA");
            }
        if(!found) throw new IllegalArgumentException("Actual shared-sum60 assertion not found");
        ObjectNode report=Json.object().put("recordType","READ_ONLY_CATALOG_LINKAGE_REPRODUCTION").put("baseline","feaca0af9673620eff9a5ac0f08a657ce14e9ccd")
            .put("sourceCase",casePath.toString()).put("sourceCaseSha256",Json.sha256(casePath)).put("oracleId",oracleId)
            .put("catalogSha256",Json.sha256(root.resolve("verification/requirements/mandatory-oracles.json")))
            .put("helperSha256",Json.sha256(root.resolve("verification/harness/src/main/java/org/mulino/verification/CatalogLinkValidator.java")))
            .put("runtimeStatus","NOT_RUN").put("semanticCompleteness","REQUIRES_FINAL_REVIEW");
        report.set("originalLinkageProblems",Json.MAPPER.valueToTree(valid));
        List<String> absent=check(validator,catalog,missing),value=check(validator,catalog,wrongValue),unit=check(validator,catalog,wrongUnit);
        report.set("missingPrimaryProblems",Json.MAPPER.valueToTree(absent));report.set("wrongValueProblems",Json.MAPPER.valueToTree(value));report.set("wrongUnitProblems",Json.MAPPER.valueToTree(unit));
        boolean pass=valid.isEmpty() && !absent.isEmpty() && !value.isEmpty() && !unit.isEmpty();report.put("structuralReproduction",pass?"PASS":"FAIL");
        Json.write(Path.of(args[2]),report);if(!pass) throw new AssertionError(report.toPrettyString());
    }
    private static List<String> check(ContractValidator validator,JsonNode catalog,JsonNode c) throws Exception {
        List<String> problems=new ArrayList<>();CatalogLinkValidator.validate(validator,catalog,Map.of("T11",c),problems);return problems;
    }
}
