package com.mulino.application.trade.returns;
import com.mulino.application.core.*;
import com.mulino.application.trade.*;
import com.mulino.domain.evidence.EvidenceSubjectPort;
import com.mulino.domain.inventory.StockPrimitives;
import com.mulino.domain.trade.returns.ReturnRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class ReturnEvidence implements EvidenceSubjectPort,TradeEvidenceScopePort {
 private final ReturnRepository r;private final ExecutionClock clock;private final ObjectMapper json=new ObjectMapper();
 public ReturnEvidence(ReturnRepository r,ExecutionClock clock){this.r=r;this.clock=clock;}
 public Set<String> subjectKinds(){return Set.of("RETURN");}
 public Set<String> eventKinds(){return Set.of("RETURN_RECEIPT","RETURN_DISPOSITION_REVIEW","RETURN_DISPOSITION_RESALE","RETURN_DISPOSITION_EXCHANGE","RETURN_DISPOSITION_REFUND","RETURN_DISPOSITION_DISPOSE");}
 public Map<String,Object> require(String organization,String kind,String id){return r.require(new DomainContext(organization,"00000000-0000-0000-0000-000000000000","lookup",clock.instant(),clock.instant()),"Observations",id);}
 public Scope require(DomainContext c,String physical,Map<String,Object>claim,Map<String,Object>event,Map<String,Object>document,byte[]original){try{var row=r.require(c,"Observations",physical);var bytes=json.readValue(original,Map.class);var payload=json.readValue(event.get("payload").toString(),Map.class);String kind=event.get("kind").toString();
  for(var source:List.of(bytes,payload)){for(String k:List.of("deliveryId","customerId","itemId","lotId","rangeRootId","unit","placeId","workId","eventId"))if(!Objects.equals(row.get(k),source.get(k)))throw unverified();if(!physical.equals(source.get("returnId"))||!kind.equals(source.get("kind")))throw unverified();for(String k:List.of("startQuantity","quantity"))if(new BigDecimal(source.get(k).toString()).compareTo((BigDecimal)row.get(k))!=0)throw unverified();if(!StockPrimitives.instant(row.get("occurredAt")).equals(StockPrimitives.instant(source.get("occurredAt"))))throw unverified();}
  for(var source:List.of(claim,event,document))if(!"RETURN".equals(source.get("subjectKind"))||!physical.equals(source.get("subjectId"))||!row.get("itemId").equals(source.get("itemId"))||!row.get("workId").equals(source.get("workId"))||!row.get("placeId").equals(source.get("placeId")))throw unverified();
  if(new BigDecimal(claim.get("quantity").toString()).compareTo((BigDecimal)row.get("quantity"))!=0||!row.get("unit").equals(claim.get("unit"))||!StockPrimitives.instant(row.get("occurredAt")).equals(StockPrimitives.instant(claim.get("effectiveFrom"))))throw unverified();
  var semantics=new TreeMap<String,Object>();for(String k:List.of("deliveryId","customerId","itemId","lotId","rangeRootId","unit","placeId","workId","eventId","occurredAt","startQuantity","quantity"))semantics.put(k,Objects.toString(row.get(k)));semantics.put("kind",kind);
  return new Scope(Map.of("itemId",row.get("itemId"),"placeId",row.get("placeId"),"workId",row.get("workId")),ReturnCommands.scopes(row),row.get("eventId").toString(),CommandRequests.hash(semantics));
 }catch(DomainError e){throw e;}catch(Exception e){throw unverified();}}
 private static DomainError unverified(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","Immutable return original differs from exact delivery/customer/physical range");}
}
