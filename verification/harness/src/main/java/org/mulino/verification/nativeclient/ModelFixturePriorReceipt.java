package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import org.mulino.verification.actual.*;
import java.net.http.*;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;

/** The M50 exception performs a real gateway command and replay; no command history is seeded. */
final class ModelFixturePriorReceipt {
    static void complete(Path root,ActualConfiguration config,JsonNode fixture,JsonNode inputs,ObjectNode result) throws Exception {
        JsonNode setup=inputs.path("priorReceipt");if(!fixture.path("fixtureId").asText().equals("M50-isolated")||!setup.path("version").asText().equals("model-M50-prior-receipt-v1"))throw new IllegalArgumentException("Only the pinned M50 prior command is authorized");
        String org=fixture.at("/aliases/org").asText(),actorId=fixture.at("/aliases/actor").asText(),manager=fixture.at("/aliases/manager").asText();
        JsonNode originalGrant=fixture.at("/baseline/grantFacts/grant");if(originalGrant.hasNonNull("revokedAt"))throw new IllegalArgumentException("Supplemental receipt authority cannot rescue revocation");
        var actor=(ObjectNode)fixture.path("actors").path("command-actor").deepCopy();actor.put("organizationAlias",org);
        var intent=Json.object().put("intentKind","COMMAND").put("definitionVersion",fixture.at("/baseline/domainFacts/objects/O1/definitionVersion").asText()).put("capabilityVersion","1.0.0").put("capabilityId","confirmReceipt").put("commandIdempotencyKey",setup.path("commandIdempotencyKey").asText()).put("expectedRevision",setup.path("observationRevision").asInt());
        intent.set("subjectRefs",Json.array());intent.set("slots",Json.object().put("receiptId",setup.path("observationId").asText()).put("lotId",fixture.at("/aliases/L").asText()).put("canonicalOccurrenceId",setup.path("canonicalOccurrenceId").asText()));intent.set("provenance",Json.object().put("receiptId","CONTEXT").put("lotId","CONTEXT").put("canonicalOccurrenceId","CONTEXT"));
        var proof=Json.object().put("evidenceClass","ACTUAL_FOCUSED_HTTP_PG_PRIOR_RECEIPT").put("actualModelCalls",0).put("originalUnchangedCaseStatus","BLOCKED_PHYSICAL_IDENTITY_CONFLICT").put("originalIdentityPreservedInProduct",false).put("correctedGiven",true).put("coordinatorSelectedSynthetic",true).put("originalAuthorityClaimed",false);
        proof.set("originalReference",setup.deepCopy());proof.set("actualResolvedIntent",intent.deepCopy());result.set("priorReceiptProof",proof);
        JsonNode before=counts(config,org);proof.set("before",before);
        ObjectNode raw=post(config,actor,"/api/ontology/commands/validate",intent);proof.set("uncorrectedOriginalAuthority",raw);
        if(!raw.path("response").path("error").path("code").asText().equals("FORBIDDEN")) {proof.put("status","UNEXPECTED_ORIGINAL_GUARD");result.put("functionalCorrectedModelInstalled",false);return;}
        if(!before.equals(counts(config,org)))throw new IllegalStateException("Validation changed product effects");proof.put("uncorrectedEffectsAdded",0);
        installAuthority(config,fixture,setup,result);
        var validated=post(config,actor,"/api/ontology/commands/validate",intent);proof.set("correctedValidation",validated);
        if(!validated.path("response").path("outcome").asText().equals("VALIDATED")){proof.put("status","CORRECTED_VALIDATION_FAILED");result.put("functionalCorrectedModelInstalled",false);return;}
        intent.put("canonicalIntentHash",validated.path("response").path("canonicalIntentHash").asText());intent.put("proposalRevision",validated.path("response").path("proposalRevision").asInt());proof.set("actualResolvedIntent",intent.deepCopy());
        var committed=post(config,actor,"/api/ontology/commands/confirmReceipt",intent);proof.set("actualFirstCommit",committed);
        if(!committed.path("response").path("outcome").asText().equals("APPLIED")){proof.put("status","ACTUAL_PRIOR_COMMAND_FAILED");result.put("functionalCorrectedModelInstalled",false);return;}
        proof.set("afterFirstCommit",counts(config,org));var replay=post(config,actor,"/api/ontology/commands/confirmReceipt",intent);proof.set("actualReplay",replay);proof.set("afterReplay",counts(config,org));
        if(!proof.path("afterFirstCommit").equals(proof.path("afterReplay"))||!replay.path("response").equals(committed.path("response")))throw new IllegalStateException("Real prior command replay changed effects/result");
        var conflicting=intent.deepCopy();conflicting.remove("canonicalIntentHash");((ObjectNode)conflicting.path("slots")).put("quantity","40");var conflict=post(config,actor,"/api/ontology/commands/confirmReceipt",conflicting);proof.set("changedPayloadConflict",conflict);proof.set("afterConflict",counts(config,org));if(!proof.path("afterConflict").equals(proof.path("afterFirstCommit"))||!conflict.path("response").path("error").path("code").asText().equals("IDEMPOTENCY_CONFLICT"))throw new IllegalStateException("Changed full payload did not reject without effects");
        String leaf=committed.path("response").path("effects").path("segmentId").asText();UUID.fromString(leaf);String receipt=committed.path("response").path("effects").path("receiptId").asText();String command=committed.path("response").path("commandId").asText();
        try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {
            proof.set("actualCommandRecord",row(c,"mulino_commands_CommandRecords",org,command));proof.set("actualReceipt",row(c,"mulino_trade_receipt_Receipts",org,receipt));proof.set("actualLeaf",row(c,"mulino_inventory_QuantitySegments",org,leaf));
            try(var s=c.prepareStatement("SELECT COALESCE(SUM(quantity),0) FROM mulino_inventory_QuantitySegments WHERE organizationId=? AND placeId=? AND retiredAt IS NULL")){s.setString(1,org);s.setString(2,fixture.at("/aliases/W").asText());try(var rs=s.executeQuery()){rs.next();String total=rs.getBigDecimal(1).stripTrailingZeros().toPlainString();proof.put("actualWarehouseQuantity",total);if(!total.equals("100"))throw new IllegalStateException("Corrected M50 physical total differs from100");}}
        }
        var binding=Json.object().put("table","mulino_inventory_QuantitySegments").put("id",leaf).put("originalReferenceId",fixture.at("/aliases/A").asText()).put("originalIdentityPreservedInProduct",false).put("correctedGiven",true).put("coordinatorSelectedSynthetic",true).put("originalAuthorityClaimed",false);binding.set("sourceReceipt",proof.path("actualReceipt").deepCopy());result.set("correctedPhysicalBindings",Json.object().set("A",binding));
        // The authored UUID remains a reference locator; physical consumers must explicitly select correctedPhysicalBindings.
        ((ObjectNode)result.path("aliasMap")).put("A",fixture.at("/aliases/A").asText());proof.put("status","ACTUAL_COMMIT_AND_ZERO_EFFECT_REPLAY").put("replayEffectsAdded",0);result.put("functionalCorrectedModelInstalled",true);
    }
    static ObjectNode post(ActualConfiguration config,JsonNode actor,String endpoint,JsonNode body) throws Exception {
        var request=HttpRequest.newBuilder(config.baseUri().resolve(endpoint)).header("Content-Type","application/json").header("Authorization","Bearer "+new JwtSigner(config).sign(actor)).timeout(Duration.ofSeconds(30)).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();var response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());var out=Json.object().put("endpoint",endpoint).put("httpStatus",response.statusCode()).put("rawResponse",response.body());out.set("request",body.deepCopy());out.set("response",Json.MAPPER.readTree(response.body()));return out;
    }
    static void installAuthority(ActualConfiguration config,JsonNode f,JsonNode setup,ObjectNode result) throws Exception {
        if(!(f.path("fixtureId").asText().equals("M50-isolated")&&setup.path("version").asText().equals("model-M50-prior-receipt-v1"))&&!(f.path("fixtureId").asText().equals("M03-isolated")&&setup.path("version").asText().equals("model-M03-confirm-authority-v1")))throw new IllegalArgumentException("Closed receipt authority completion required");
        String org=f.at("/aliases/org").asText();JsonNode original=f.at("/baseline/grantFacts/grant");var rows=Json.array();var undo=Json.array();
        try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {c.setAutoCommit(false);try {
            for(String principal:List.of("actor","manager")) {
                String id=setup.path(principal.equals("actor")?"grantId":"delegatorGrantId").asText();UUID.fromString(id);String actor=f.at("/aliases/"+principal).asText(),manager=f.at("/aliases/manager").asText();var values=map("organizationId",org,"ID",id,"actorId",actor,"delegatorId",manager,"revision",original.path("revision").asInt(),"validFrom",OffsetDateTime.parse(original.path("validFrom").asText()),"validUntil",OffsetDateTime.parse(original.path("validUntil").asText()),"createdAt",OffsetDateTime.parse(f.at("/clock/asOf").asText()),"recordedAt",OffsetDateTime.parse(f.at("/clock/knownAt").asText()));
                write(c,rows,undo,"Grant","mulino_identity_Grants",map("organizationId",org,"ID",id),values);
                var action=map("organizationId",org,"grantId",id,"capabilityId","confirmReceipt");write(c,rows,undo,"GrantAction","mulino_identity_GrantActions",action,action);
                var scopes=new LinkedHashMap<String,String>();scopes.put("ORGANIZATION",org);scopes.put("TARGET",setup.path("observationId").asText());scopes.put("ITEM",f.at("/aliases/P").asText());scopes.put("PLACE",f.at("/aliases/W").asText());scopes.put("WORK",f.at("/aliases/O1").asText());
                for(var scope:scopes.entrySet()){var v=map("organizationId",org,"grantId",id,"scopeKind",scope.getKey(),"scopeId",scope.getValue());write(c,rows,undo,"GrantScope","mulino_identity_GrantScopes",v,v);}
                var binding=Json.object().put("purpose",f.path("fixtureId").asText().substring(0,3)+(principal.equals("actor")?"_CONFIRM_RECEIPT":"_CONFIRM_DELEGATOR")).put("grantId",id).put("originalGrantId",f.at("/aliases/grant").asText()).put("principalId",actor).put("originalReceiptObservationProvided",false).put("coordinatorSelectedSynthetic",true).put("originalAuthorityClaimed",false);binding.set("exactScopes",Json.MAPPER.valueToTree(scopes));binding.set("exactGrant",Json.MAPPER.valueToTree(values.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,e->e.getValue() instanceof OffsetDateTime t?t.toString():e.getValue()))));((ArrayNode)result.path("purposeGrantBindings")).add(binding);
            }
            ModelFixtureInstaller.readback(c,rows);c.commit();
        }catch(Exception failure){c.rollback();throw failure;}}
        ((ArrayNode)result.path("sourceFactReceipts")).addAll(rows);var inverse=(ArrayNode)result.path("inverse");for(JsonNode entry:undo)inverse.insert(0,entry);
    }
    private static void write(Connection c,ArrayNode receipts,ArrayNode inverse,String type,String table,Map<String,Object> key,Map<String,Object> values) throws Exception {ModelFixtureInstaller.insert(c,table,values);ModelFixtureInstaller.receipt(receipts,inverse,"/priorReceipt",type,table,Json.MAPPER.valueToTree(key),values,true);}
    static ObjectNode counts(ActualConfiguration config,String org) throws Exception {var out=Json.object();try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())){for(String table:List.of("mulino_trade_receipt_Receipts","mulino_inventory_QuantityMovements","mulino_runtime_Outbox","mulino_commands_CommandRecords","mulino_inventory_QuantitySegments","mulino_inventory_SegmentAllocations","mulino_work_read_Works","mulino_trade_purchase_Approvals")){try(var s=c.prepareStatement("SELECT count(*) FROM "+table+" WHERE organizationId=?")){s.setString(1,org);try(var rs=s.executeQuery()){rs.next();out.put(table,rs.getLong(1));}}}}return out;}
    private static ObjectNode row(Connection c,String table,String org,String id) throws Exception {String sql="SELECT to_jsonb(r)::text,xmin::text FROM "+table+" r WHERE organizationId=? AND ID=?";try(var s=c.prepareStatement(sql)){s.setString(1,org);s.setString(2,id);try(var rs=s.executeQuery()){if(!rs.next())throw new IllegalStateException("Actual command effect missing");var out=Json.object().put("table",table).put("id",id).put("sql",sql).put("xminTransactionId",rs.getString(2));out.set("actualValues",Json.MAPPER.readTree(rs.getString(1)));return out;}}}
    private static LinkedHashMap<String,Object> map(Object... kv){var out=new LinkedHashMap<String,Object>();for(int i=0;i<kv.length;i+=2)out.put(kv[i].toString(),kv[i+1]);return out;}
    private ModelFixturePriorReceipt() {}
}
