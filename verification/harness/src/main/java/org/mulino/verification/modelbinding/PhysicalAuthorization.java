package org.mulino.verification.modelbinding;
import com.fasterxml.jackson.databind.JsonNode;
import org.mulino.verification.Json;
import java.time.Instant;
import java.util.*;
import static org.mulino.verification.modelbinding.PhysicalRecipeRows.require;
/** Independent current membership ∩ assignment ∩ grant ∩ delegator recipe; all grant dimensions intersect. */
public final class PhysicalAuthorization {
    private static final String P="mulino_identity_";
    public static boolean allowed(PhysicalRecipeRows rows,String actor,String capability,Map<String,Set<String>> targets,Set<String> visited){
        Instant now=Instant.parse(Json.required(rows.parameters,"authorityAt"));
        if(!visited.add(actor)||visited.size()>32)return false;
        if(rows.rows(P+"Actors").stream().noneMatch(r->r.path("ID").asText().equals(actor)))return false;
        if(rows.rows(P+"Memberships").stream().noneMatch(r->r.path("actorId").asText().equals(actor)&&active(r,now)))return false;
        var assigned=rows.rows(P+"CapabilityAssignments").stream().filter(r->r.path("actorId").asText().equals(actor)&&r.path("capabilityId").asText().equals(capability)&&active(r,now)).toList();
        var actions=rows.rows(P+"GrantActions");var allScopes=rows.rows(P+"GrantScopes");
        for(JsonNode grant:rows.rows(P+"Grants")){
            if(!grant.path("actorId").asText().equals(actor)||!active(grant,now))continue;
            if(actions.stream().noneMatch(a->a.path("grantId").equals(grant.path("ID"))&&a.path("capabilityId").asText().equals(capability)))continue;
            var scopes=allScopes.stream().filter(s->s.path("grantId").equals(grant.path("ID"))).toList();if(scopes.isEmpty())continue;
            String delegator=grant.path("delegatorId").asText(actor);if(!delegator.equals(actor)&&!allowed(rows,delegator,capability,targets,new HashSet<>(visited)))continue;
            for(JsonNode assignment:assigned){
                if(targets==null){if(scopes.stream().anyMatch(s->organization(s,rows.organization)||organization(assignment,rows.organization)||s.path("scopeKind").equals(assignment.path("scopeKind"))&&s.path("scopeId").equals(assignment.path("scopeId"))))return true;}
                else if(matches(assignment,targets,rows.organization)){
                    var dimensions=new HashSet<String>();for(JsonNode s:scopes)if(!organization(s,rows.organization))dimensions.add(s.path("scopeKind").asText());
                    if(dimensions.stream().allMatch(d->scopes.stream().filter(s->s.path("scopeKind").asText().equals(d)).anyMatch(s->matches(s,targets,rows.organization))))return true;
                }
            }
        }return false;
    }
    public static Map<String,Set<String>> businessTargets(JsonNode scope){var targets=new LinkedHashMap<String,Set<String>>();for(var pair:Map.of("itemId","ITEM","lotId","LOT","placeId","PLACE","workId","WORK").entrySet())if(scope.hasNonNull(pair.getKey()))targets.put(pair.getValue(),Set.of(scope.path(pair.getKey()).asText()));if(scope.hasNonNull("workId"))targets.put("TARGET",Set.of(scope.path("workId").asText()));return targets;}
    public static Map<String,Set<String>> segmentTargets(JsonNode row,String work){var targets=new LinkedHashMap<String,Set<String>>();targets.put("TARGET",Set.of(Json.required(row,"ID")));for(var pair:Map.of("itemId","ITEM","placeId","PLACE").entrySet())if(row.hasNonNull(pair.getKey()))targets.put(pair.getValue(),Set.of(row.path(pair.getKey()).asText()));if(work!=null)targets.put("WORK",Set.of(work));return targets;}
    private static boolean matches(JsonNode scope,Map<String,Set<String>> targets,String org){return organization(scope,org)||targets.getOrDefault(scope.path("scopeKind").asText(),Set.of()).contains(scope.path("scopeId").asText());}
    private static boolean organization(JsonNode row,String org){return row.path("scopeKind").asText().equals("ORGANIZATION")&&row.path("scopeId").asText().equals(org);}
    private static boolean active(JsonNode row,Instant now){for(String key:List.of("validFrom","validUntil","revokedAt"))require(row.has(key),"AUTHORITY_COLUMN_UNOBSERVED:"+key);return row.hasNonNull("validFrom")&&row.hasNonNull("validUntil")&&!now.isBefore(Instant.parse(row.path("validFrom").asText()))&&now.isBefore(Instant.parse(row.path("validUntil").asText()))&&(!row.hasNonNull("revokedAt")||now.isBefore(Instant.parse(row.path("revokedAt").asText())));}
    private PhysicalAuthorization() {}
}
