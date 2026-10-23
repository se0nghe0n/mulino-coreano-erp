package com.mulino.application.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.adapters.blob.LocalBlobStore;
import com.mulino.application.core.*;
import com.mulino.application.quality.QualityEligibility;
import com.mulino.domain.evidence.EvidenceRepository;
import com.mulino.domain.evidence.EvidenceTypes;
import com.mulino.domain.inventory.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

/** Validated fulfillment decisions become part of the transaction's durable command result. */
@Component
public final class FulfillmentBasis {
 private final InventoryRepository inventory;
 private final QualityEligibility quality;
 private final ReadAuthorizer auth;
 private final EvidenceRepository evidence;
 private final LocalBlobStore blobs;
 public FulfillmentBasis(InventoryRepository inventory,QualityEligibility quality,ReadAuthorizer auth,EvidenceRepository evidence,LocalBlobStore blobs){this.inventory=inventory;this.quality=quality;this.auth=auth;this.evidence=evidence;this.blobs=blobs;}

 public record Reason(String value) {
  public static Reason required(Map<String,Object> slots,String name){
   Object value=slots.get(name);
   if(!(value instanceof String text)||text.isBlank()||text.length()>500)throw DomainError.invalid(name+" must be nonblank text of at most 500 characters");
   return new Reason(text);
  }
  public static Reason optional(Map<String,Object> slots,String name){return slots.containsKey(name)?required(slots,name):null;}
 }

 /** Only earlier, actionable, unreserved current warehouse stock participates in FEFO. */
 public Map<String,Object> allocation(DomainContext c,String capability,Map<String,Object> selected,Map<String,Object> line,Map<String,Object> replaced,Reason reason){
  var expiry=expiry(c,selected);var earlier=new ArrayList<Map<String,Object>>();
  var allocations=inventory.currentRows(c,"SegmentAllocations");
  for(var candidate:inventory.currentRows(c,"QuantitySegments")){
   if(selected.get("ID").equals(candidate.get("ID"))||!selected.get("itemId").equals(candidate.get("itemId"))||!selected.get("unit").equals(candidate.get("unit"))||candidate.get("retiredAt")!=null||StockPrimitives.instant(candidate.get("recordedAt")).isAfter(c.knownAt())||StockPrimitives.instant(candidate.get("validFrom")).isAfter(c.asOf()))continue;
   Instant candidateExpiry=expiry(c,candidate);if(candidateExpiry==null||expiry==null||!candidateExpiry.isBefore(expiry))continue;
   if(!"INTERNAL_STORAGE".equals(inventory.current(c,"Places",(String)candidate.get("placeId")).get("kind"))||!inventory.internalCustodian(c,(String)candidate.get("custodianId")))continue;
   var scopes=Map.of("TARGET",List.of((String)candidate.get("ID")),"ITEM",List.of((String)candidate.get("itemId")),"PLACE",List.of((String)candidate.get("placeId")),"WORK",List.of((String)line.get("workId")));
   if(!auth.permittedScopes(c,capability,scopes))continue;
   var free=quality.assess(c,candidate,"SELL",(String)line.get("customerId")).ranges();
   for(var allocation:allocations)if(candidate.get("ID").equals(allocation.get("segmentId"))&&Set.of("EXECUTABLE","SUSPENDED").contains(allocation.get("state"))&&(replaced==null||!replaced.get("ID").equals(allocation.get("ID")))){
    if(allocation.get("startQuantity")==null){free=List.of();break;}
    free=QualityRanges.subtract(free,List.of(QualityEligibility.range(allocation)));
   }
   if(QualityRanges.quantity(free).signum()>0)earlier.add(Map.of("segmentId",candidate.get("ID"),"expiresAt",candidateExpiry.toString(),"unreservedEligibleQuantity",InventoryQuantity.text(QualityRanges.quantity(free))));
  }
  earlier.sort(Comparator.comparing((Map<String,Object> row)->Instant.parse(row.get("expiresAt").toString())).thenComparing(row->row.get("segmentId").toString()));
  if(!earlier.isEmpty()&&reason==null)throw DomainError.invalid("FEFOExceptionReason required to select later-expiring eligible stock");
  var basis=new LinkedHashMap<String,Object>();basis.put("ordering","FEFO");basis.put("exception",!earlier.isEmpty());basis.put("earlierEligibleCandidates",List.copyOf(earlier));
  if(reason!=null)basis.put("FEFOExceptionReason",reason.value());
  return basis;
 }
 private Instant expiry(DomainContext c,Map<String,Object> segment){var value=inventory.current(c,"ManufacturingLots",(String)segment.get("lotId")).get("expiresAt");return value==null?null:StockPrimitives.instant(value);}

 /** A replacement decision cites a current original of the right type and an affected business subject.
  * This is a decision document, not a claim that replacement has already happened or a new approval.
  */
 public Map<String,Object> replacement(DomainContext c,String id,Map<String,Object> old,Map<String,Object> selected,Map<String,Object> line){
  var doc=evidence.require("DocumentVersions",c.organizationId(),id);
  auth.authorizeScopes(c,"getEvidence",EvidenceTypes.scopes(doc));
  boolean subject=switch(Objects.toString(doc.get("subjectKind"),"")){
   case "SEGMENT"->(old.get("segmentId").equals(doc.get("subjectId"))||selected.get("ID").equals(doc.get("subjectId")));
   case "WORK"->line.get("workId").equals(doc.get("subjectId"));
   case "SALES_ORDER_LINE"->line.get("ID").equals(doc.get("subjectId"));
   default->false;
  };
  if(!subject||!visible(doc,c)||!"application/json".equals(doc.get("mediaType"))||!"AVAILABLE".equals(doc.get("availability"))||doc.get("blobId")==null||evidence.rows("DocumentVersions",c.organizationId()).stream().anyMatch(row->visible(row,c)&&id.equals(row.get("supersedesId"))))throw unverified();
  try{
   var original=new ObjectMapper().readTree(blobs.read(UUID.fromString(doc.get("blobId").toString()),doc.get("sha256").toString()));
   if(!original.isObject()||!original.path("witnessKind").isTextual()||!"replacement-proof".equals(original.path("witnessKind").asText()))throw unverified();
  }catch(java.io.IOException malformed){throw unverified();}
  return Map.of("evidenceId",id,"evidenceHash",doc.get("sha256"),"evidenceType","replacement-proof","subjectKind",doc.get("subjectKind"),"subjectId",doc.get("subjectId"),"replacedAllocationId",old.get("ID"));
 }
 public void addEvidenceFences(DomainContext c,String id,Set<String> fences){
  var doc=evidence.require("DocumentVersions",c.organizationId(),id);
  fences.add("evidence-subject:"+doc.get("subjectId"));
  if(doc.get("sourceProfileId")!=null)fences.add("source:"+doc.get("sourceProfileId"));
 }
 private static boolean visible(Map<String,Object> row,DomainContext c){return !EvidenceTypes.instant(row.get("recordedAt")).isAfter(c.knownAt())&&(row.get("effectiveFrom")==null||!EvidenceTypes.instant(row.get("effectiveFrom")).isAfter(c.asOf()))&&(row.get("effectiveUntil")==null||c.asOf().isBefore(EvidenceTypes.instant(row.get("effectiveUntil"))));}
 private static DomainError unverified(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","Current original replacement-proof for the affected allocation subject required");}
}
