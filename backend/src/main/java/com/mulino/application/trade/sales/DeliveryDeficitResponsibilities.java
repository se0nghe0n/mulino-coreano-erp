package com.mulino.application.trade.sales;
import com.mulino.application.core.*;
import com.mulino.application.responsibility.ResponsibilityKindEvidence;
import com.mulino.application.trade.TradeEvidence;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import com.mulino.domain.trade.sales.SalesRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;
/**
 * A corrected-delivery deficit range is fulfilled only when the current, verified PHYSICAL_DELIVERY
 * correction of the same delivery recognizes enough quantity that no unit of this exact duty leaf
 * remains deficient (plan §4.3, §5.3). Document presence, an older or superseded correction, or the
 * correction that created the deficit cannot discharge it.
 */
@Component
public class DeliveryDeficitResponsibilities implements ResponsibilityKindEvidence {
 private final ResponsibilityRepository duties;private final SalesRepository sales;private final TradeEvidence verified;
 public DeliveryDeficitResponsibilities(ResponsibilityRepository duties,SalesRepository sales,TradeEvidence verified){this.duties=duties;this.sales=sales;this.verified=verified;}
 public String kind(){return "DELIVERY_CORRECTED_DEFICIT";}
 public void requireResolution(DomainContext c,String rootId,String scopeId,String evidenceId){
  var leaf=duties.require("Scopes",c.organizationId(),scopeId);var root=duties.require("Roots",c.organizationId(),rootId);
  if(!rootId.equals(leaf.get("rootId"))||!Boolean.TRUE.equals(leaf.get("leaf"))||!kind().equals(root.get("kind")))throw DomainError.invalid("Exact corrected-delivery deficit leaf required");
  String deliveryId;BigDecimal scopeStart;
  try{var scope=new ObjectMapper().readTree(root.get("scopeJson").toString());deliveryId=scope.path("deliveryId").asText();scopeStart=new BigDecimal(scope.path("startQuantity").asText());}catch(Exception e){throw DomainError.invalid("Corrected-delivery deficit scope invalid");}
  sales.fence(c,"sales/delivery-correction/"+deliveryId);
  var delivery=sales.require(c,"Deliveries",deliveryId);
  var current=sales.rows(c,"DeliveryCorrections").stream().filter(x->deliveryId.equals(x.get("deliveryId"))).max(Comparator.comparingInt(x->((Number)x.get("revision")).intValue())).orElseThrow(DeliveryDeficitResponsibilities::unverified);
  if(!evidenceId.equals(current.get("canonicalOccurrenceId")))throw unverified();
  verified.requireCanonical(c,evidenceId,"PHYSICAL_DELIVERY",delivery.get("itemId").toString(),delivery.get("observationId").toString(),(BigDecimal)current.get("quantity"),delivery.get("unit").toString());
  BigDecimal remainingDeficit=((BigDecimal)delivery.get("legitimateQuantity")).subtract((BigDecimal)current.get("legitimateQuantity"));
  BigDecimal leafStart=scopeStart.add((BigDecimal)leaf.get("startQuantity"));
  if(remainingDeficit.compareTo(leafStart)>0)throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Current verified delivery still leaves this exact deficit range unfulfilled");
 }
 private static DomainError unverified(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","Only the current verified delivery correction can fulfil a corrected-delivery deficit");}
}
