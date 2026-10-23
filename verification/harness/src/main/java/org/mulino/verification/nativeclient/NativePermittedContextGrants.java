package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import org.mulino.verification.Json;
import java.time.Instant;
import java.util.*;
import static org.mulino.verification.nativeclient.NativePermittedContext.require;

/** Observation recipes for the targets the current read handlers actually authorize.
 * Uses only captured server rows and request type; never client grants, aliases or fixture purpose labels.
 * Delegator authorization remains the product's decision (the capture still says NOT_ASSERTED). */
final class NativePermittedContextGrants {
    static final Set<String> READS=Set.of("getObject","searchObjects","getWork","searchWorks","getObligations","getInventory","getEvidence");
    static final Set<String> RECORDS=Set.of("Allocation","Restriction","DispositionBasis","Dispatch","ReturnAuthorization","Obligation","Handover","CommandRecord");
    static List<Map<String,Set<String>>> targets(String operation,JsonNode request,JsonNode response) {
        require(READS.contains(operation),"PERMITTED_CONTEXT_GRANT_SCOPE_RECIPE_UNAVAILABLE");
        var out=new ArrayList<Map<String,Set<String>>>();var data=response.path("data");
        if(operation.equals("getInventory")) {
            if(data.path("items").isArray())for(JsonNode block:data.path("items"))inventory(block,out);
            else inventory(data,out);
            return out;
        }
        require(data.isArray()||data.isObject(),"PERMITTED_CONTEXT_GRANT_SCOPE_RECIPE_UNAVAILABLE");
        var rows=new ArrayList<JsonNode>();if(data.isArray())data.forEach(rows::add);else rows.add(data);
        String type=request.path("scope").path("objectType").asText(request.path("type").asText("TradeItem"));
        for(JsonNode row:rows) {
            require(row.isObject()&&row.hasNonNull("ID"),"PERMITTED_CONTEXT_GRANT_SCOPE_RECIPE_UNAVAILABLE");
            var s=new LinkedHashMap<String,Set<String>>();add(s,"TARGET",row,"ID");
            if(operation.equals("getEvidence")) {
                add(s,"TARGET",row,"subjectId");common(s,row);add(s,"SOURCE",row,"sourceProfileId");
            } else if(Set.of("getWork","searchWorks","getObligations").contains(operation)) {
                add(s,"WORK",row,"ID");add(s,"ITEM",row,"itemId");
            } else if(RECORDS.contains(type)) {
                for(String k:List.of("segmentId","rootId","transitSegmentId","rangeRootId","allocationId"))add(s,"TARGET",row,k);
                common(s,row);add(s,"WORK",row,"targetWorkId");add(s,"CUSTOMER",row,"customerId");
                addArray(s,"ITEM",row.path("itemIds"));addArray(s,"WORK",row.path("workIds"));
            } else switch(type) {
                case "Product","Manufacturer","LogisticsUnit" -> {}
                case "TradeItem" -> add(s,"ITEM",row,"ID");
                case "ManufacturingLot" -> add(s,"ITEM",row,"itemId");
                case "QuantitySegment" -> {add(s,"ITEM",row,"itemId");add(s,"PLACE",row,"placeId");}
                case "Place" -> add(s,"PLACE",row,"ID");
                case "PurchaseProposal","PurchaseOrder" -> {s.remove("TARGET");add(s,"ITEM",row,"itemId");add(s,"WORK",row,"workId");}
                case "Invoice" -> {
                    s.clear();s.putAll(invoiceScope(row));
                    if(row.has("corrections")){require(row.path("corrections").isArray(),"PERMITTED_CONTEXT_GRANT_SCOPE_RECIPE_UNAVAILABLE");for(JsonNode correction:row.path("corrections"))out.add(invoiceScope(correction));}
                }
                case "RegulatoryProcedure" -> {add(s,"TARGET",row,"physicalScopeId");add(s,"ITEM",row,"itemId");add(s,"WORK",row,"workId");}
                case "Recall","RecallScope" -> {add(s,"ITEM",row,"itemId");add(s,"WORK",row,"workId");}
                case "Customer","SalesOrder","SalesOrderRevision","SalesOrderLine","Delivery","DeliveryObservation" -> {
                    common(s,row);add(s,"PLACE",row,row.hasNonNull("placeId")?"placeId":"destinationId");
                }
                case "Receipt","ReceiptObservation" -> common(s,row);
                case "Return" -> {add(s,"ITEM",row,"itemId");add(s,"WORK",row,"workId");add(s,"PLACE",row,row.hasNonNull("destinationId")?"destinationId":"placeId");}
                // ShipmentQueries publishes its computed target scope and checks every Cartesian binding.
                case "Shipment" -> {
                    s.clear();require(row.path("scope").isObject(),"PERMITTED_CONTEXT_GRANT_SCOPE_RECIPE_UNAVAILABLE");
                    for(String k:List.of("TARGET","ITEM","PLACE","WORK"))addArray(s,k,row.path("scope").path(k));
                    var combinations=new ArrayList<Map<String,Set<String>>>();combinations.add(new LinkedHashMap<>());
                    for(var e:s.entrySet()) {
                        require(!e.getValue().isEmpty()&&(long)combinations.size()*e.getValue().size()<=256,"PERMITTED_CONTEXT_GRANT_SCOPE_BOUND_EXCEEDED");
                        var next=new ArrayList<Map<String,Set<String>>>();for(var c:combinations)for(String id:e.getValue()){var n=new LinkedHashMap<>(c);n.put(e.getKey(),Set.of(id));next.add(n);}combinations=next;
                    }
                    out.addAll(combinations);continue;
                }
                default -> throw new IllegalStateException("PERMITTED_CONTEXT_GRANT_SCOPE_RECIPE_UNAVAILABLE");
            }
            out.add(s);
        }
        return out;
    }
    static List<JsonNode> applicable(JsonNode server,String operation,List<Map<String,Set<String>>> targets) {
        String org=Json.required(server.path("identity"),"organizationId");Instant now=Instant.parse(Json.required(server,"authorityAsOf"));
        var applicable=new LinkedHashMap<String,JsonNode>();
        require(server.path("grants").isArray()&&server.path("assignments").isArray(),"PERMITTED_CONTEXT_GRANTS_MISSING");
        var capturedIds=new HashSet<String>();for(JsonNode grant:server.path("grants"))require(capturedIds.add(Json.required(grant,"ID")),"PERMITTED_CONTEXT_GRANT_CAPTURE_INVALID");
        for(var target:targets) {
            boolean covered=false;
            for(JsonNode grant:server.path("grants")) {
                require(grant.path("actorId").equals(server.path("identity").path("actorId")),"PERMITTED_CONTEXT_GRANT_ACTOR_MISMATCH");
                if(!active(grant,now)||!hasAction(grant,operation)||!matchesAll(grant.path("scopes"),target,org))continue;
                boolean assigned=false;for(JsonNode a:server.path("assignments"))assigned|=a.path("capabilityId").asText().equals(operation)&&active(a,now)&&matches(a,target,org);
                if(assigned){applicable.put(Json.required(grant,"ID"),grant);covered=true;}
            }
            require(covered,"PERMITTED_CONTEXT_GRANT_ATTRIBUTION_UNAVAILABLE");
        }
        return List.copyOf(applicable.values());
    }
    static boolean emptyRead(String operation,JsonNode request,JsonNode response) {
        return READS.contains(operation)&&targets(operation,request,response).isEmpty();
    }
    private static boolean matchesAll(JsonNode scopes,Map<String,Set<String>> targets,String org) {
        require(scopes.isArray(),"PERMITTED_CONTEXT_GRANTS_MISSING");if(scopes.isEmpty())return false;
        var dimensions=new LinkedHashMap<String,List<JsonNode>>();for(JsonNode s:scopes)dimensions.computeIfAbsent(Json.required(s,"scopeKind"),k->new ArrayList<>()).add(s);
        // Within one dimension alternatives are OR; every dimension (including SOURCE) must match.
        return dimensions.values().stream().allMatch(rows->rows.stream().anyMatch(s->matches(s,targets,org)));
    }
    private static boolean matches(JsonNode scope,Map<String,Set<String>> target,String org) {
        String kind=Json.required(scope,"scopeKind"),id=Json.required(scope,"scopeId");
        return kind.equals("ORGANIZATION")?id.equals(org):target.getOrDefault(kind,Set.of()).contains(id);
    }
    private static boolean hasAction(JsonNode grant,String operation){for(JsonNode a:grant.path("actions"))if(operation.equals(a.path("capabilityId").asText()))return true;return false;}
    static boolean active(JsonNode row,Instant now) {
        return !now.isBefore(Instant.parse(Json.required(row,"validFrom")))&&now.isBefore(Instant.parse(Json.required(row,"validUntil")))
            &&(!row.hasNonNull("revokedAt")||now.isBefore(Instant.parse(Json.required(row,"revokedAt"))));
    }
    private static Map<String,Set<String>> invoiceScope(JsonNode row) {
        var joined=row.path("readScope");
        require(joined.isObject()&&joined.path("ITEM").isArray()&&!joined.path("ITEM").isEmpty()
            &&joined.path("WORK").isArray()&&!joined.path("WORK").isEmpty(),"PERMITTED_CONTEXT_GRANT_SCOPE_RECIPE_UNAVAILABLE");
        require(Set.of("ITEM","WORK","PLACE","CUSTOMER","SUPPLIER").containsAll(NativePermittedContext.keys(joined)),"PERMITTED_CONTEXT_GRANT_SCOPE_RECIPE_UNAVAILABLE");
        for(JsonNode ids:joined){require(ids.isArray()&&!ids.isEmpty(),"PERMITTED_CONTEXT_GRANT_SCOPE_RECIPE_UNAVAILABLE");for(JsonNode id:ids)UUID.fromString(id.asText());}
        var scopes=new LinkedHashMap<String,Set<String>>();for(String dimension:List.of("ITEM","WORK","PLACE","CUSTOMER","SUPPLIER"))addArray(scopes,dimension,joined.path(dimension));return scopes;
    }
    private static void inventory(JsonNode block,List<Map<String,Set<String>>> out) {
        require(block.isObject()&&block.hasNonNull("itemId"),"PERMITTED_CONTEXT_GRANT_SCOPE_RECIPE_UNAVAILABLE");
        if(block.path("segments").isArray()&&!block.path("segments").isEmpty())for(JsonNode segment:block.path("segments")) {
            var s=new LinkedHashMap<String,Set<String>>();add(s,"TARGET",segment,"ID");add(s,"ITEM",segment,"itemId");add(s,"PLACE",segment,"placeId");out.add(s);
        } else {var s=new LinkedHashMap<String,Set<String>>();add(s,"TARGET",block,"itemId");add(s,"ITEM",block,"itemId");out.add(s);}
    }
    private static void common(Map<String,Set<String>> s,JsonNode row){add(s,"ITEM",row,"itemId");add(s,"PLACE",row,"placeId");add(s,"WORK",row,"workId");}
    private static void add(Map<String,Set<String>> s,String dimension,JsonNode row,String field){if(row.hasNonNull(field))s.computeIfAbsent(dimension,k->new LinkedHashSet<>()).add(Json.required(row,field));}
    private static void addArray(Map<String,Set<String>> s,String dimension,JsonNode ids){if(ids.isArray())for(JsonNode id:ids){require(id.isTextual()&&!id.asText().isBlank(),"PERMITTED_CONTEXT_GRANT_SCOPE_RECIPE_UNAVAILABLE");s.computeIfAbsent(dimension,k->new LinkedHashSet<>()).add(id.asText());}}
    private NativePermittedContextGrants() {}
}
