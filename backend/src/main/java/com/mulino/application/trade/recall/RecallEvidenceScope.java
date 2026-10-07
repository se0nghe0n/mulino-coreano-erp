package com.mulino.application.trade.recall;
import com.mulino.application.core.*;
import com.mulino.application.trade.TradeEvidenceScopePort;
import com.mulino.domain.evidence.EvidenceSubjectPort;
import com.mulino.domain.trade.recall.RecallRepository;
import com.mulino.domain.inventory.*;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.mulino.application.trade.recall.RecallInputs.*;
@Component
public final class RecallEvidenceScope implements TradeEvidenceScopePort,EvidenceSubjectPort {
 private final RecallRepository r;private final InventoryRepository inventory;private final ReadAuthorizer auth;
 public RecallEvidenceScope(RecallRepository r,InventoryRepository inventory,ReadAuthorizer auth){this.r=r;this.inventory=inventory;this.auth=auth;}
 public Set<String> eventKinds(){return Set.of("RECALL_NOTICE","RECALL_RECOVERY","RECALL_DISPOSAL","RECALL_SAFE","RECALL_CONSUMED_LOST","RECALL_EXCEPTION","RECALL_CLOSURE");}
 public Set<String> subjectKinds(){return Set.of("RECALL","RECALL_SCOPE");}
 public Map<String,Object> require(String org,String kind,String id){return r.current(org,kind.equals("RECALL")?"Investigations":"Scopes",id);}
 public Scope require(DomainContext c,String physical,Map<String,Object> claim,Map<String,Object> event,Map<String,Object> document,byte[] original){
  try{var mapper=new ObjectMapper();var source=mapper.readTree(original);var payload=mapper.readTree(event.get("payload").toString());if(!source.isObject()||!source.equals(payload))throw held();
   var scope=r.require(c,"Scopes",physical);var investigation=r.require(c,"Investigations",scope.get("investigationId").toString());
   if(!Objects.equals(investigation.get("currentScopeId"),physical)||!Objects.equals(claim.get("subjectKind"),event.get("subjectKind"))||!Objects.equals(claim.get("subjectId"),event.get("subjectId"))||!("RECALL_SCOPE".equals(claim.get("subjectKind"))&&physical.equals(claim.get("subjectId"))||"RECALL".equals(claim.get("subjectKind"))&&investigation.get("ID").equals(claim.get("subjectId"))))throw held();
   for(String key:List.of("itemId","lotId","rootSegmentId","scopeHash"))if(!scope.get(key).toString().equals(source.path(key).asText()))throw held();
   if(!physical.equals(source.path("scopeId").asText())||n(scope.get("version"))!=source.path("scopeVersion").asInt()||!scope.get("itemId").equals(claim.get("itemId")))throw held();
   BigDecimal start=decimal(source.path("startQuantity").asText(),false),q=InventoryQuantity.parse(source.path("quantity").asText(),n(inventory.current(c,"TradeItems",scope.get("itemId").toString()).get("decimalPlaces")));
   if(start.compareTo((BigDecimal)scope.get("startQuantity"))<0||start.add(q).compareTo(((BigDecimal)scope.get("startQuantity")).add((BigDecimal)scope.get("quantity")))>0||q.compareTo((BigDecimal)claim.get("quantity"))!=0||!scope.get("unit").equals(source.path("unit").asText())||!scope.get("unit").equals(claim.get("unit")))throw held();
   var occurred=at(source.path("occurredAt").asText());if(!occurred.equals(at(event.get("effectiveFrom")))||!occurred.equals(at(claim.get("effectiveFrom")))||occurred.isAfter(c.knownAt())||!event.get("kind").equals(source.path("eventKind").asText()))throw held();
   String eventId=source.path("actualEventId").asText();CommandRequests.uuid(eventId);
   String place=source.path("currentPlaceId").asText();inventory.current(c,"Places",place);
   Map<String,List<String>> scopes=Map.of("TARGET",List.of(physical,investigation.get("ID").toString(),scope.get("rootSegmentId").toString()),"ITEM",List.of(scope.get("itemId").toString()),"WORK",List.of(scope.get("workId").toString()),"PLACE",List.of(place));auth.authorizeScopes(c,"getEvidence",scopes);
   var semantic=mapper.convertValue(source,new com.fasterxml.jackson.core.type.TypeReference<TreeMap<String,Object>>(){});semantic.put("quantity",q.stripTrailingZeros().toPlainString());semantic.put("startQuantity",start.stripTrailingZeros().toPlainString());String hash=com.mulino.domain.definitions.DefinitionRepository.sha256(json(semantic));
   return new Scope(Map.of("itemId",scope.get("itemId"),"workId",scope.get("workId"),"placeId",place),scopes,eventId,hash);
  }catch(java.io.IOException|IllegalArgumentException|NullPointerException malformed){throw held();}
 }
}
