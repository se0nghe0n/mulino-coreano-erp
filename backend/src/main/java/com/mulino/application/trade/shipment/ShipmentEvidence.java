package com.mulino.application.trade.shipment;

import com.mulino.application.core.*;
import com.mulino.application.evidence.EvidenceQueries;
import com.mulino.domain.evidence.EvidenceRepository;
import com.mulino.domain.inventory.StockPrimitives;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

/** Current exact canonical reconciliation, never a carrier/document truthiness check. */
@Component
public class ShipmentEvidence {
 private final EvidenceRepository evidence;private final EvidenceQueries queries;private final com.mulino.application.trade.TradeEvidence trade;
 public ShipmentEvidence(EvidenceRepository evidence,EvidenceQueries queries,com.mulino.application.trade.TradeEvidence trade){this.evidence=evidence;this.queries=queries;this.trade=trade;}
 public Map<String,Object> canonical(DomainContext c,String id){return evidence.require("CanonicalOccurrences",c.organizationId(),id);}
 public void require(DomainContext c,String id,String kind,String item,String scope,String place,BigDecimal quantity,String unit,Instant occurred){
  var r=trade.requireCanonical(c,id,kind,item,scope,quantity,unit);
  if(!queries.verifiedAt(c,id)||!"KNOWN".equals(r.get("valueState"))||!kind.equals(r.get("kind"))||!item.equals(r.get("itemId"))||!scope.equals(r.get("physicalScopeId"))||!place.equals(r.get("placeId"))||!unit.equals(r.get("unit"))||!(r.get("quantity") instanceof BigDecimal q)||q.compareTo(quantity)!=0||!occurred.equals(StockPrimitives.instant(r.get("effectiveFrom"))))throw held();
  if(evidence.rows("CanonicalOccurrences",c.organizationId()).stream().anyMatch(x->id.equals(x.get("supersedesId"))&&!StockPrimitives.instant(x.get("recordedAt")).isAfter(c.knownAt())))throw held();
  var verification=evidence.rows("Verifications",c.organizationId()).stream().filter(v->id.equals(v.get("canonicalOccurrenceId"))&&"VERIFIED".equals(v.get("verdict"))&&List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(k->Boolean.TRUE.equals(v.get(k)))).findFirst().orElseThrow(ShipmentEvidence::held);
  var claim=evidence.require("Claims",c.organizationId(),verification.get("claimId").toString());
  if(!Objects.equals(r.get("sourceProfileId"),claim.get("sourceProfileId"))||!"KNOWN".equals(claim.get("valueState")))throw held();
 }
 public Map<String,Object> profile(DomainContext c,String canonical){return evidence.require("SourceProfiles",c.organizationId(),canonical(c,canonical).get("sourceProfileId").toString());}
 public Map<String,Object> event(DomainContext c,String canonical){var v=evidence.rows("Verifications",c.organizationId()).stream().filter(x->canonical.equals(x.get("canonicalOccurrenceId"))&&"VERIFIED".equals(x.get("verdict"))).findFirst().orElseThrow(ShipmentEvidence::held);var claim=evidence.require("Claims",c.organizationId(),v.get("claimId").toString());return evidence.require("Events",c.organizationId(),claim.get("eventId").toString());}
 public void requireObservationShape(DomainContext c,String id,Map<String,Object>slots){
  try {var event=event(c,id);var payload=new com.fasterxml.jackson.databind.ObjectMapper().readTree(event.get("payload").toString());
   for(String key:List.of("shipmentId","cargoId","legId","placeId","physicalScopeId"))if(!slots.get(key).equals(payload.path(key).asText()))throw held();
   if(slots.containsKey("lineAllocations")&&!new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(slots.get("lineAllocations")).equals(payload.path("lineAllocations")))throw held();
   if(slots.containsKey("fromCustodianId"))for(String key:List.of("fromCustodianId","toCustodianId"))if(!slots.get(key).equals(payload.path(key).asText()))throw held();
  }catch(java.io.IOException malformed){throw held();}
 }
 private static DomainError held(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","Exact current verified shipment source, identity, time and quantity required");}
}
