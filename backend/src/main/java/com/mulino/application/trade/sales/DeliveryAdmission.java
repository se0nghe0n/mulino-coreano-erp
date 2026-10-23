package com.mulino.application.trade.sales;
import com.mulino.application.core.*;import com.mulino.domain.evaluation.CanonicalFactAdmission;import com.mulino.domain.trade.sales.SalesRepository;import com.mulino.domain.inventory.*;import java.math.BigDecimal;import java.util.*;import org.springframework.stereotype.Component;
@Component
public class DeliveryAdmission implements CanonicalFactAdmission {
 private final SalesRepository r;private final ReadAuthorizer auth;private final com.mulino.application.evidence.CanonicalEvidenceVerification verification;
 public DeliveryAdmission(SalesRepository r,ReadAuthorizer auth){this(r,auth,null);}
 @org.springframework.beans.factory.annotation.Autowired public DeliveryAdmission(SalesRepository r,ReadAuthorizer auth,com.mulino.application.evidence.CanonicalEvidenceVerification verification){this.r=r;this.auth=auth;this.verification=verification;}
 public Set<String> eventKinds(){return Set.of("PHYSICAL_DELIVERY");}
 public Admission admit(DomainContext c,String work,String goal,Map<String,Object>fact){return admit(c,work,goal,fact,Purpose.ASSESS_GOAL);}
 public Admission admit(DomainContext c,String work,String goal,Map<String,Object>fact,Purpose purpose){Objects.requireNonNull(purpose);String canonical=fact.get("ID").toString();Map<String,Object>delivery=null;BigDecimal recognized=null,actual=null;for(var d:r.rows(c,"Deliveries"))if(canonical.equals(d.get("canonicalOccurrenceId"))){delivery=d;recognized=(BigDecimal)d.get("legitimateQuantity");actual=(BigDecimal)d.get("quantity");}for(var correction:r.rows(c,"DeliveryCorrections"))if(canonical.equals(correction.get("canonicalOccurrenceId"))){delivery=r.require(c,"Deliveries",correction.get("deliveryId").toString());recognized=(BigDecimal)correction.get("legitimateQuantity");actual=(BigDecimal)correction.get("quantity");}if(delivery==null||!(fact.get("quantity") instanceof BigDecimal q)||q.compareTo(actual)!=0||!Objects.equals(delivery.get("workId"),fact.get("workId"))||!Objects.equals(delivery.get("itemId"),fact.get("itemId"))||!Objects.equals(delivery.get("placeId"),fact.get("placeId"))||!Objects.equals(delivery.get("observationId"),fact.get("physicalScopeId"))||!Objects.equals(delivery.get("unit"),fact.get("unit"))||!StockPrimitives.instant(delivery.get("occurredAt")).equals(StockPrimitives.instant(fact.get("effectiveFrom"))))return new Admission(false,null,null,List.of());
  if(purpose.causalRecord()){
   if(verification==null)return new Admission(false,null,null,List.of());
   var cause=DeliveryRecordCause.authorize(c,purpose,r,auth,work);
   if(cause.isEmpty()||!verification.verifiedForDeliveryRecord(c,canonical,cause.get()))return new Admission(false,null,null,List.of());
   return new Admission(true,recognized,delivery.get("lotId").toString(),List.of(delivery.get("ID").toString(),delivery.get("segmentId").toString(),canonical));
  }
  // A WORK dimension matches any granted value: each endpoint needs its own proof.
  for(String requiredWork:new LinkedHashSet<>(List.of(work,delivery.get("workId").toString())))
   if(!auth.permittedScopes(c,purpose.capability(),Map.of("TARGET",List.of(work),"WORK",List.of(requiredWork),"ITEM",List.of(delivery.get("itemId").toString()),"PLACE",List.of(delivery.get("placeId").toString()))))return new Admission(false,null,null,List.of());
  return new Admission(true,recognized,delivery.get("lotId").toString(),List.of(delivery.get("ID").toString(),delivery.get("segmentId").toString(),canonical));}
}
