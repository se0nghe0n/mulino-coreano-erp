package com.mulino.application.trade;

import com.mulino.application.core.*;
import com.mulino.application.responsibility.ResponsibilityService;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import com.mulino.domain.trade.purchase.PurchaseRepository;
import com.mulino.domain.trade.receipt.ReceiptRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

/** Receipt-only trusted hook; invoked after receipt and purchasing credit in the same transaction. */
@Component
public final class TradeResidualRemedy {
 private final PurchaseRepository purchase;
 private final ReceiptRepository receipts;
 private final ResponsibilityRepository duties;
 private final ResponsibilityService service;
 private final TradeImpact impact;
 private final TradeEvidence evidence;
 public TradeResidualRemedy(PurchaseRepository purchase,ReceiptRepository receipts,ResponsibilityRepository duties,ResponsibilityService service,TradeImpact impact,TradeEvidence evidence){this.purchase=purchase;this.receipts=receipts;this.duties=duties;this.service=service;this.impact=impact;this.evidence=evidence;}
 public void reconcileReceiptShortfall(DomainContext c,String receiptId,Instant nextCheckAt){
  var receipt=receipts.require(c,"Receipts",receiptId);
  String lineId=Objects.toString(receipt.get("purchaseLineId"),"");
  if(lineId.isBlank())throw DomainError.invalid("Allocated receipt required for shortfall remedy");
  purchase.fence(c,"receipt-residual/"+lineId);
  var line=purchase.require(c,"OrderLines",lineId);
  for(String k:List.of("workId","itemId","unit"))if(!Objects.equals(line.get(k),receipt.get(k)))throw DomainError.invalid("Receipt residual line scope mismatch");
  if(!Objects.equals(line.get("destinationId"),receipt.get("placeId")))throw DomainError.invalid("Receipt residual destination mismatch");
  String occurrence=receipt.get("canonicalOccurrenceId").toString();
  evidence.requireCanonical(c,occurrence,"PHYSICAL_RECEIPT",receipt.get("itemId").toString(),receipt.get("rangeRootId").toString(),decimal(receipt,"quantity"),receipt.get("unit").toString());
  var credits=purchase.rows(c,"ReceiptCredits").stream().filter(x->lineId.equals(x.get("lineId"))).toList();
  var source=credits.stream().filter(x->occurrence.equals(x.get("occurrenceId"))).findFirst().orElseThrow(()->DomainError.invalid("Committed purchasing contribution required"));
  if(!Objects.equals(source.get("unit"),receipt.get("unit"))||decimal(source,"contributedQuantity").compareTo(decimal(receipt,"contributedQuantity"))!=0||decimal(source,"actualQuantity").compareTo(decimal(receipt,"quantity"))!=0)throw DomainError.invalid("Receipt residual contribution mismatch");
  BigDecimal total=credits.stream().map(x->decimal(x,"contributedQuantity")).reduce(BigDecimal.ZERO,BigDecimal::add);
  BigDecimal ordered=decimal(line,"quantity");
  if(total.signum()<0||total.compareTo(ordered)>0)throw DomainError.invalid("Invalid cumulative purchase contribution");
  var existing=duties.rows("ReceiptResidualRoots",c.organizationId()).stream().filter(x->lineId.equals(x.get("lineId"))).findFirst();
  Map<String,Object> binding;
  if(existing.isEmpty()){
   BigDecimal remaining=ordered.subtract(total);if(remaining.signum()==0)return;
   var opened=impact.recorded(c,receipt.get("workId").toString(),occurrence,"RECEIPT_SHORTFALL",receipt.get("rangeRootId").toString(),"남은 발주 수령량을 대조한다",nextCheckAt,Map.of("purchaseLineId",lineId),remaining,receipt.get("unit").toString());
   String rootId=opened.get("rootId").toString();
   binding=new LinkedHashMap<>(Map.of("organizationId",c.organizationId(),"ID",UUID.randomUUID().toString(),"revision",0,"createdAt",c.knownAt(),"recordedAt",c.knownAt(),"rootId",rootId,"lineId",lineId,"initialReceiptId",receiptId));
   binding.put("initialContribution",total);binding.put("orderedQuantity",ordered);binding.put("unit",line.get("unit"));duties.insert("ReceiptResidualRoots",binding);
  }else binding=existing.get();
  if(decimal(binding,"orderedQuantity").compareTo(ordered)!=0||!Objects.equals(binding.get("unit"),line.get("unit")))throw DomainError.invalid("Changed order requires explicit residual reassessment");
  BigDecimal covered=total.subtract(decimal(binding,"initialContribution"));
  if(covered.signum()<0)throw DomainError.invalid("Receipt contribution history regressed");
  service.applyReceiptProgress(c,new ReceiptProgress(binding.get("rootId").toString(),receiptId,occurrence,covered));
 }
 public void reconcileReceiptObservation(DomainContext c,String observationId,String receiptId){
  var observation=receipts.require(c,"Observations",observationId);var receipt=receipts.require(c,"Receipts",receiptId);
  if(!"CONFIRMED".equals(observation.get("state")))throw DomainError.invalid("Confirmed observation required");
  for(String k:List.of("rangeRootId","itemId","unit","placeId","workId","purchaseLineId"))if(!Objects.equals(observation.get(k),receipt.get(k)))throw DomainError.invalid("Observation receipt scope mismatch");
  for(String k:List.of("startQuantity","quantity"))if(decimal(observation,k).compareTo(decimal(receipt,k))!=0)throw DomainError.invalid("Observation receipt range mismatch");
  if(observation.get("lotId")!=null&&!Objects.equals(observation.get("lotId"),receipt.get("lotId")))throw DomainError.invalid("Observation receipt LOT mismatch");
  String occurrence=receipt.get("canonicalOccurrenceId").toString();evidence.requireCanonical(c,occurrence,"PHYSICAL_RECEIPT",receipt.get("itemId").toString(),receipt.get("rangeRootId").toString(),decimal(receipt,"quantity"),receipt.get("unit").toString());
  duties.fence(c.organizationId(),observation.get("workId").toString());
  for(var root:duties.rows("Roots",c.organizationId()))if("RECEIPT_RECONCILIATION".equals(root.get("kind"))){
   try{var scope=new com.fasterxml.jackson.databind.ObjectMapper().readTree(root.get("scopeJson").toString());
    if(observationId.equals(scope.path("domainSourceId").asText())&&observation.get("workId").equals(scope.path("originalWorkId").asText())&&observation.get("rangeRootId").equals(scope.path("physicalScopeId").asText()))service.applyReceiptObservation(c,new ObservationCompletion(root.get("ID").toString(),observationId,receiptId,occurrence));
   }catch(java.io.IOException e){throw DomainError.invalid("Invalid receipt duty scope");}
  }
 }
 public static final class ObservationCompletion {
  private final String rootId,observationId,receiptId,occurrenceId;
  private ObservationCompletion(String rootId,String observationId,String receiptId,String occurrenceId){this.rootId=rootId;this.observationId=observationId;this.receiptId=receiptId;this.occurrenceId=occurrenceId;}
  public String rootId(){return rootId;}public String observationId(){return observationId;}public String receiptId(){return receiptId;}public String occurrenceId(){return occurrenceId;}
 }
 private static BigDecimal decimal(Map<String,Object> row,String key){return new BigDecimal(row.get(key).toString());}
 /** Only this authoritative receipt hook can construct a progress value. No client scalar is accepted. */
 public static final class ReceiptProgress {
  private final String rootId,receiptId,occurrenceId;private final BigDecimal covered;
  private ReceiptProgress(String rootId,String receiptId,String occurrenceId,BigDecimal covered){this.rootId=rootId;this.receiptId=receiptId;this.occurrenceId=occurrenceId;this.covered=covered;}
  public String rootId(){return rootId;}public String receiptId(){return receiptId;}public String occurrenceId(){return occurrenceId;}public BigDecimal covered(){return covered;}
 }
}
