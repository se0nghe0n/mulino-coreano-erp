package com.mulino.application.trade.purchase;
import com.mulino.application.core.*;
import com.mulino.application.trade.TradeEvidenceScopePort;
import com.mulino.domain.evidence.EvidenceSubjectPort;
import com.mulino.domain.trade.purchase.PurchaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;
/** Original supplier response is bound to an immutable order, not caller-selected stock. */
@Component
public class PurchaseEvidence implements EvidenceSubjectPort,TradeEvidenceScopePort {
 private final PurchaseRepository r;private final ExecutionClock clock;private final ObjectMapper json=new ObjectMapper();
 public PurchaseEvidence(PurchaseRepository r,ExecutionClock clock){this.r=r;this.clock=clock;}
 public Set<String> subjectKinds(){return Set.of("PURCHASE_ORDER");}
 public Set<String> eventKinds(){return Set.of("SUPPLIER_ACCEPT","SUPPLIER_REJECT","SUPPLIER_CHANGE");}
 public Map<String,Object> require(String org,String kind,String id){var c=new DomainContext(org,"00000000-0000-0000-0000-000000000000","lookup",clock.instant(),clock.instant());var order=r.require(c,"Orders",id);var line=r.rows(c,"OrderLines").stream().filter(x->id.equals(x.get("orderId"))).findFirst().orElseThrow(DomainError::forbidden);var result=new LinkedHashMap<>(order);result.put("itemId",line.get("itemId"));result.put("workId",line.get("workId"));return result;}
 public Scope require(DomainContext c,String physicalScope,Map<String,Object> claim,Map<String,Object> event,Map<String,Object> document,byte[] bytes){try{var original=json.readValue(bytes,Map.class);var payload=json.readValue(event.get("payload").toString(),Map.class);String orderId=physicalScope;var order=r.require(c,"Orders",orderId);var line=r.rows(c,"OrderLines").stream().filter(x->orderId.equals(x.get("orderId"))).findFirst().orElseThrow(DomainError::forbidden);String kind=event.get("kind").toString(),reply=kind.substring("SUPPLIER_".length());for(var source:List.of(original,payload)){if(!orderId.equals(source.get("orderId"))||!line.get("itemId").equals(source.get("itemId"))||!reply.equals(source.get("reply"))||!line.get("unit").equals(source.get("unit"))||!(source.get("proposalRevision") instanceof Number n)||n.intValue()!=((Number)order.get("proposalRevision")).intValue())throw unverified();if(claim.get("quantity")==null||new BigDecimal(source.get("quantity").toString()).compareTo(new BigDecimal(claim.get("quantity").toString()))!=0)throw unverified();}
 for(var source:List.of(claim,event,document))if(!"PURCHASE_ORDER".equals(source.get("subjectKind"))||!orderId.equals(source.get("subjectId"))||!line.get("itemId").equals(source.get("itemId")))throw unverified();
 if(reply.equals("ACCEPT")&&new BigDecimal(claim.get("quantity").toString()).compareTo(new BigDecimal(line.get("quantity").toString()))!=0)throw unverified();
 return new Scope(Map.of("itemId",line.get("itemId"),"workId",line.get("workId")),Map.of("ITEM",List.of(line.get("itemId").toString()),"WORK",List.of(line.get("workId").toString()),"TARGET",List.of(orderId)));}catch(DomainError e){throw e;}catch(Exception e){throw unverified();}}
 private static DomainError unverified(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","Original supplier response differs from immutable order scope");}
}
