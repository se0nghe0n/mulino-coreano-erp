package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.mulino.verification.Json;
import org.mulino.verification.actual.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** M05's explicit graph read grant keeps every requested dimension in one grant. */
final class ModelFixtureReadCompletion {
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("existing-M05-installation.json output.json");var root=java.nio.file.Path.of(System.getProperty("repo.root",".")).toAbsolutePath();var result=(ObjectNode)Json.read(root.resolve(args[0]));complete(ActualConfiguration.environment(System.getenv()),Json.read(root.resolve("verification/model-binding/cases/M05/fixture.json")),Json.read(root.resolve("verification/model-installation/v1/inputs/M05-isolated.json")),result);Json.MAPPER.writerWithDefaultPrettyPrinter().writeValue(root.resolve(args[1]).toFile(),result);
    }
    static void complete(ActualConfiguration config,JsonNode fixture,JsonNode inputs,ObjectNode result) throws Exception {
        if(!fixture.path("fixtureId").asText().equals("M05-isolated")||!inputs.path("readCompletion").path("version").asText().equals("model-M05-shipment-read-v1"))throw new IllegalArgumentException("Only M05 graph read completion authorized");
        String org=fixture.at("/aliases/org").asText(),shipment=fixture.at("/aliases/SH1").asText();JsonNode original=fixture.at("/baseline/grantFacts/grant");if(original.hasNonNull("revokedAt"))throw new IllegalArgumentException("Graph read cannot rescue revocation");
        var actor=(ObjectNode)fixture.at("/actors/command-actor").deepCopy();actor.put("organizationAlias",org);var request=Json.object().put("id",shipment).put("asOf",fixture.at("/clock/asOf").asText()).put("knownAt",fixture.at("/clock/knownAt").asText());request.set("scope",Json.object().put("objectType","Shipment"));
        var proof=Json.object().put("actualModelCalls",0).put("coordinatorSelectedSynthetic",true).put("originalAuthorityClaimed",false);result.set("correctedShipmentReadProof",proof);proof.set("uncorrectedGraphRead",ModelFixturePriorReceipt.post(config,actor,"/api/ontology/queries/getObject",request));
        if(!proof.path("uncorrectedGraphRead").path("response").path("error").path("code").asText().equals("FORBIDDEN"))throw new IllegalStateException("Original graph read did not fail closed");
        var receipts=Json.array();var inverse=Json.array();
        try(var c=DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password())) {c.setAutoCommit(false);try {
            var targets=new ArrayList<String>();targets.add(shipment);var places=new LinkedHashSet<String>();String work=null,item=null;
            try(var s=c.prepareStatement("SELECT originId,destinationId,workId FROM mulino_trade_shipment_Shipments WHERE organizationId=? AND ID=?")){s.setString(1,org);s.setString(2,shipment);try(var rs=s.executeQuery()){if(!rs.next())throw new IllegalStateException("Installed shipment missing");places.add(rs.getString(1));places.add(rs.getString(2));work=rs.getString(3);}}
            String sql="SELECT ID,itemId FROM mulino_trade_shipment_ShipmentCargo WHERE organizationId=? AND shipmentId=? ORDER BY ID";try(var s=c.prepareStatement(sql)){s.setString(1,org);s.setString(2,shipment);try(var rs=s.executeQuery()){while(rs.next()){targets.add(rs.getString(1));if(item!=null&&!item.equals(rs.getString(2)))throw new IllegalStateException("Unexpected multi-item graph");item=rs.getString(2);}}}if(targets.size()!=2)throw new IllegalStateException("M05 requires exactly its one newly persisted cargo");
            try(var s=c.prepareStatement("SELECT originId,destinationId FROM mulino_trade_shipment_ShipmentLegs WHERE organizationId=? AND shipmentId=?")){s.setString(1,org);s.setString(2,shipment);try(var rs=s.executeQuery()){while(rs.next()){places.add(rs.getString(1));places.add(rs.getString(2));}}}
            var scopes=new LinkedHashMap<String,List<String>>();scopes.put("ORGANIZATION",List.of(org));scopes.put("TARGET",targets);scopes.put("ITEM",List.of(item));scopes.put("PLACE",List.copyOf(places));scopes.put("WORK",List.of(work));
            var source=Json.object().put("sql",sql);source.set("parameters",Json.array().add(org).add(shipment));source.set("actualTargetIds",Json.MAPPER.valueToTree(targets));proof.set("causalGraphReadback",source);
            for(String principal:List.of("actor","manager")) {
                String id=inputs.path("readCompletion").path(principal.equals("actor")?"grantId":"rootGrantId").asText();UUID.fromString(id);var values=map("organizationId",org,"ID",id,"actorId",fixture.at("/aliases/"+principal).asText(),"delegatorId",fixture.at("/aliases/manager").asText(),"revision",original.path("revision").asInt(),"validFrom",OffsetDateTime.parse(original.path("validFrom").asText()),"validUntil",OffsetDateTime.parse(original.path("validUntil").asText()),"createdAt",OffsetDateTime.parse(fixture.at("/clock/asOf").asText()),"recordedAt",OffsetDateTime.parse(fixture.at("/clock/knownAt").asText()));
                add(c,receipts,inverse,"Grant","mulino_identity_Grants",map("organizationId",org,"ID",id),values);
                for(String target:targets){String assignment=UUID.nameUUIDFromBytes((id+":getShipment:"+target).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();var capability=map("organizationId",org,"ID",assignment,"actorId",fixture.at("/aliases/"+principal).asText(),"capabilityId","getShipment","scopeKind","TARGET","scopeId",target,"validFrom",OffsetDateTime.parse(original.path("validFrom").asText()),"validUntil",OffsetDateTime.parse(original.path("validUntil").asText()),"revision",1,"createdAt",OffsetDateTime.parse(fixture.at("/clock/asOf").asText()),"recordedAt",OffsetDateTime.parse(fixture.at("/clock/knownAt").asText()));add(c,receipts,inverse,"CapabilityAssignment","mulino_identity_CapabilityAssignments",map("organizationId",org,"ID",assignment),capability);}

                for(String action:List.of("getObject","getShipment")){var a=map("organizationId",org,"grantId",id,"capabilityId",action);add(c,receipts,inverse,"GrantAction","mulino_identity_GrantActions",a,a);}
                for(var scope:scopes.entrySet())for(String scopeId:scope.getValue()){var s=map("organizationId",org,"grantId",id,"scopeKind",scope.getKey(),"scopeId",scopeId);add(c,receipts,inverse,"GrantScope","mulino_identity_GrantScopes",s,s);}
                var binding=Json.object().put("purpose","M05_SHIPMENT_GRAPH_READ"+(principal.equals("manager")?"_DELEGATOR":"")).put("grantId",id).put("originalGrantId",fixture.at("/aliases/grant").asText()).put("originalAuthorityClaimed",false).put("coordinatorSelectedSynthetic",true).put("publicCatalogMissing",true).put("currentReadPrerequisite","getShipment");binding.set("exactScopes",Json.MAPPER.valueToTree(scopes));binding.set("actions",Json.array().add("getObject").add("getShipment"));((ArrayNode)result.path("purposeGrantBindings")).add(binding);
            }
            ModelFixtureInstaller.readback(c,receipts);c.commit();
        }catch(Exception failure){c.rollback();throw failure;}}
        ((ArrayNode)result.path("sourceFactReceipts")).addAll(receipts);for(JsonNode undo:inverse)((ArrayNode)result.path("inverse")).insert(0,undo);
        proof.set("beforeCorrectedReadBusinessCounts",ModelFixturePriorReceipt.counts(config,org));
        proof.set("correctedGraphRead",ModelFixturePriorReceipt.post(config,actor,"/api/ontology/queries/getObject",request));proof.set("afterCorrectedReadBusinessCounts",ModelFixturePriorReceipt.counts(config,org));if(!proof.path("beforeCorrectedReadBusinessCounts").equals(proof.path("afterCorrectedReadBusinessCounts")))throw new IllegalStateException("Read changed business effects");proof.put("businessEffectsAdded",0);proof.put("readApplied",proof.path("correctedGraphRead").path("httpStatus").asInt()==200&&!proof.path("correctedGraphRead").path("response").has("error"));
    }
    private static void add(Connection c,ArrayNode receipts,ArrayNode inverse,String type,String table,Map<String,Object> key,Map<String,Object> values) throws Exception {ModelFixtureInstaller.insert(c,table,values);ModelFixtureInstaller.receipt(receipts,inverse,"/readCompletion",type,table,Json.MAPPER.valueToTree(key),values,true);}
    private static LinkedHashMap<String,Object> map(Object... kv){var out=new LinkedHashMap<String,Object>();for(int i=0;i<kv.length;i+=2)out.put(kv[i].toString(),kv[i+1]);return out;}
    private ModelFixtureReadCompletion() {}
}
