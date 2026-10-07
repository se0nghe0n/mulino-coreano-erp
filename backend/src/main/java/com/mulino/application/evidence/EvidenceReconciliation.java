package com.mulino.application.evidence;

import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.domain.evidence.*;
import com.mulino.adapters.blob.LocalBlobStore;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import com.sap.cds.ql.Select;
import org.springframework.transaction.annotation.Transactional;
import static com.mulino.domain.evidence.EvidenceTypes.*;

/** Explicit scoped human review. Source authentication is not proof of physical truth. */
@Service
public class EvidenceReconciliation {
  public record Review(String claimId,String basisDocumentId,String physicalScopeId,String existingCanonicalId,
      String policyVersion,String sourceIdentity,String quantity,String unit,Instant effectiveFrom,String reason) {}
  private final EvidenceRepository r;
  private final EvidenceRecords records;
  private final IdentityAuthorization auth;
  private final LocalBlobStore blobs;
  private final ExecutionClock clock;
  private final VerifiedResponsibilityCompletionEvidence completion;
  private final ObjectProvider<EvidenceCorrectionImpact> impacts;
  private final ObjectProvider<ExternalOperationScopePort> externalOperations;
  private final ObjectProvider<com.mulino.application.trade.ReceiptEvidenceScopePort> receiptScopes;
  public EvidenceReconciliation(EvidenceRepository r,EvidenceRecords records,IdentityAuthorization auth,LocalBlobStore blobs,ObjectProvider<EvidenceCorrectionImpact> impacts,ObjectProvider<ExternalOperationScopePort> externalOperations,ExecutionClock clock,VerifiedResponsibilityCompletionEvidence completion,ObjectProvider<com.mulino.application.trade.ReceiptEvidenceScopePort> receiptScopes) {
    this.r=r;this.records=records;this.auth=auth;this.blobs=blobs;this.impacts=impacts;this.externalOperations=externalOperations;this.clock=clock;this.completion=completion;this.receiptScopes=receiptScopes;
  }
  public Map<String,Object> claim(DomainContext c,String id){return r.require("Claims",c.organizationId(),uuid(id));}
  public Map<String,Object> review(DomainContext c,String id){return r.require("Reconciliations",c.organizationId(),uuid(id));}
  public Map<String,Object> profile(DomainContext c,Map<String,Object> claim) {
    var event=r.require("Events",c.organizationId(),claim.get("eventId").toString());
    return records.source(c,event.get("sourceNamespace").toString());
  }
  public void authorizeReviewer(DomainContext c,String capability,Map<String,Object> claim) {
    auth.authorizeScopes(c,capability,scopes(claim));
    var profile=profile(c,claim);
    if(!c.actorId().equals(profile.get("intakeOwnerId"))&&!c.actorId().equals(profile.get("supervisorId")))throw DomainError.forbidden();
  }
  @Transactional
  public Map<String,Object> match(DomainContext c,Review input,String capability) {
    var claim=claim(c,input.claimId());authorizeReviewer(c,capability,claim);
    var event=r.require("Events",c.organizationId(),claim.get("eventId").toString());var profile=profile(c,claim);
    r.sourceFence(c.organizationId(),event.get("sourceNamespace").toString(),event.get("externalEventId").toString(),event.get("sourceVersion").toString());
    var doc=r.require("DocumentVersions",c.organizationId(),uuid(input.basisDocumentId()));auth.authorizeScopes(c,"getEvidence",scopes(doc));
    if(!Objects.equals(doc.get("subjectKind"),claim.get("subjectKind"))||!Objects.equals(doc.get("subjectId"),claim.get("subjectId")))throw DomainError.invalid("Review basis subject mismatch");
    if(!Objects.equals(profile.get("policyVersion"),input.policyVersion()))throw new DomainError("REJECTED","POLICY_UNRESOLVED","Review evidence policy mismatch");
    text(input.reason(),640);text(input.sourceIdentity(),320);Objects.requireNonNull(input.effectiveFrom());
    boolean current=!r.rows("Claims",c.organizationId()).stream().anyMatch(x->claim.get("ID").equals(x.get("supersedesId")))
      &&!r.rows("Events",c.organizationId()).stream().anyMatch(x->event.get("ID").equals(x.get("supersedesId"))||event.get("ID").equals(x.get("invalidatesId")));
    long variants=r.rows("InboxRecords",c.organizationId()).stream().filter(x->Objects.equals(event.get("sourceNamespace"),x.get("sourceNamespace"))&&Objects.equals(event.get("externalEventId"),x.get("externalEventId"))&&Objects.equals(event.get("sourceVersion"),x.get("sourceVersion"))).count();
    BigDecimal quantity=quantity(input.quantity(),input.unit(),ValueState.KNOWN);
    boolean external="EXTERNAL_RESULT".equals(event.get("kind"));
    boolean response="RESPONSE_COMPLETED".equals(event.get("kind"));
    boolean receipt="PHYSICAL_RECEIPT".equals(event.get("kind"));
    boolean sameQuantity=external?quantity==null&&claim.get("quantity")==null:quantity!=null&&claim.get("quantity") instanceof BigDecimal q&&quantity.compareTo(q)==0&&Objects.equals(input.unit(),claim.get("unit"));
    boolean original=!r.rows("DocumentVersions",c.organizationId()).stream().anyMatch(x->doc.get("ID").equals(x.get("supersedesId")))&&"AVAILABLE".equals(doc.get("availability"))&&doc.get("blobId")!=null&&blobs.available(UUID.fromString(doc.get("blobId").toString()),doc.get("sha256").toString());
    boolean identity=false;
    if(receipt&&input.physicalScopeId()!=null&&original) {
      var providers=receiptScopes.stream().toList();if(providers.size()!=1)throw new DomainError("HELD","POLICY_UNRESOLVED","Receipt identity provider unavailable");
      var range=providers.getFirst().require(c,uuid(input.physicalScopeId()));
      var allowed=new LinkedHashMap<String,Collection<String>>(scopes(claim));allowed.put("TARGET",List.of(range.receiptRangeId()));allowed.put("ITEM",List.of(range.itemId()));allowed.put("PLACE",List.of(range.placeId()));if(range.workId()!=null)allowed.put("WORK",List.of(range.workId()));auth.authorizeScopes(c,capability,allowed);
      identity=range.receiptRangeId().equals(input.physicalScopeId())&&Objects.equals(range.itemId(),claim.get("itemId"))&&Objects.equals(range.placeId(),claim.get("placeId"))&&Objects.equals(range.workId(),claim.get("workId"))&&sameQuantity(quantity,range.quantity())&&Objects.equals(input.unit(),range.unit())&&range.occurredAt().equals(input.effectiveFrom())&&switch(claim.get("subjectKind").toString()){case "ITEM"->range.itemId().equals(claim.get("subjectId"));case "LOT"->Objects.equals(range.lotId(),claim.get("subjectId"));default->false;};
      try {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();var originalPayload=mapper.readTree(blobs.read(UUID.fromString(doc.get("blobId").toString()),doc.get("sha256").toString()));var eventPayload=mapper.readTree(event.get("payload").toString());
        for(var payload:List.of(originalPayload,eventPayload))identity &= receiptPayloadMatches(payload,range);
      }catch(java.io.IOException malformed){identity=false;}
    }else if(response&&input.physicalScopeId()!=null&&original) {
      try{completion.sourceRange(c,claim,event,doc,uuid(input.physicalScopeId()));identity=true;}catch(DomainError unavailable){if(!"EVIDENCE_UNVERIFIED".equals(unavailable.code()))throw unavailable;}
    }else if(external&&input.physicalScopeId()!=null) {
      var operations=externalOperations.getIfAvailable();
      if(operations==null)throw new DomainError("HELD","POLICY_UNRESOLVED","External operation scope adapter unavailable");
      var operation=operations.require(c,uuid(input.physicalScopeId()));
      identity="WORK".equals(claim.get("subjectKind"))&&Objects.equals(operation.get("workId"),claim.get("subjectId"))&&Objects.equals(operation.get("ID"),input.physicalScopeId());
      var operationScopes=new LinkedHashMap<String,Collection<String>>(scopes(claim));operationScopes.put("TARGET",List.of(input.physicalScopeId()));
      auth.authorizeScopes(c,capability,operationScopes);
      if(original)try {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        var originalPayload=mapper.readTree(blobs.read(UUID.fromString(doc.get("blobId").toString()),doc.get("sha256").toString()));
        var eventPayload=mapper.readTree(event.get("payload").toString());
        String outcome=originalPayload.path("outcome").asText();
        identity&=input.physicalScopeId().equals(originalPayload.path("externalOperationId").asText())
          &&input.physicalScopeId().equals(eventPayload.path("externalOperationId").asText())&&outcome.equals(eventPayload.path("outcome").asText())
          &&Set.of("CONFIRMED_SUCCESS","CONFIRMED_FAILURE").contains(outcome);
      }catch(java.io.IOException malformedOriginal){identity=false;}
    }else if(input.physicalScopeId()!=null) {
      var physical=r.subject(c.organizationId(),"SEGMENT",uuid(input.physicalScopeId()));
      var physicalScopes=new LinkedHashMap<String,Collection<String>>(scopes(claim));physicalScopes.put("TARGET",List.of(input.physicalScopeId()));physicalScopes.put("ITEM",List.of(physical.get("itemId").toString()));
      if(physical.get("placeId")!=null)physicalScopes.put("PLACE",List.of(physical.get("placeId").toString()));
      auth.authorizeScopes(c,capability,physicalScopes);
      identity="CONFIRMED".equals(physical.get("identificationStatus"))&&"IDENTIFIED".equals(physical.get("mixtureStatus"))&&quantity!=null&&quantity.compareTo((BigDecimal)physical.get("quantity"))==0&&Objects.equals(input.unit(),physical.get("unit"))&&switch(claim.get("subjectKind").toString()) {
        case "SEGMENT" -> input.physicalScopeId().equals(claim.get("subjectId"));
        case "ITEM" -> claim.get("subjectId").equals(physical.get("itemId"));
        case "LOT" -> claim.get("subjectId").equals(physical.get("lotId"));
        default -> false;
      };
    }
    String decision=variants>1||"CONFLICT".equals(event.get("valueState"))?"CONFLICT":"UNVERIFIED";
    if(current&&variants==1&&identity&&original&&sameQuantity&&"KNOWN".equals(claim.get("valueState"))&&"KNOWN".equals(event.get("valueState"))
       &&Objects.equals(input.sourceIdentity(),event.get("sourceNamespace")+":"+event.get("externalEventId")+":"+event.get("sourceVersion"))
       &&input.effectiveFrom().equals(instant(claim.get("effectiveFrom"))))decision="MATCHED";
    if(input.physicalScopeId()!=null) {
      var relatedClaims=r.rows("Claims",c.organizationId()).stream().filter(x->Objects.equals(claim.get("eventId"),x.get("eventId"))).map(x->x.get("ID").toString()).collect(java.util.stream.Collectors.toSet());
      var canonicalIds=r.rows("Verifications",c.organizationId()).stream().filter(x->relatedClaims.contains(x.get("claimId"))&&"VERIFIED".equals(x.get("verdict"))).map(x->x.get("canonicalOccurrenceId").toString()).collect(java.util.stream.Collectors.toSet());
      if(r.rows("CanonicalOccurrences",c.organizationId()).stream().anyMatch(x->canonicalIds.contains(x.get("ID"))&&!input.physicalScopeId().equals(x.get("physicalScopeId"))))decision="CONFLICT";
      Set<String> overlapping=external||response||receipt?Set.of(input.physicalScopeId()):r.overlappingScopes(c.organizationId(),input.physicalScopeId());
      if(r.rows("CanonicalOccurrences",c.organizationId()).stream().anyMatch(x->!input.physicalScopeId().equals(x.get("physicalScopeId"))&&overlapping.contains(x.get("physicalScopeId"))&&event.get("kind").equals(x.get("kind"))))decision="CONFLICT";
    }
    if(input.physicalScopeId()!=null && r.rows("CanonicalOccurrences",c.organizationId()).stream().anyMatch(x->input.physicalScopeId().equals(x.get("physicalScopeId"))&&event.get("kind").equals(x.get("kind"))&&!input.effectiveFrom().equals(instant(x.get("effectiveFrom")))))decision="CONFLICT";
    if(input.physicalScopeId()!=null && r.rows("CanonicalOccurrences",c.organizationId()).stream().anyMatch(x->input.physicalScopeId().equals(x.get("physicalScopeId"))&&event.get("kind").equals(x.get("kind"))
        &&(!Objects.equals(claim.get("effectiveUntil"),x.get("effectiveUntil"))||!Objects.equals(claim.get("timePrecision"),x.get("timePrecision")))))decision="CONFLICT";
    if(input.effectiveFrom().isAfter(clock.instant()))decision="UNVERIFIED";
    if(input.existingCanonicalId()!=null) {
      var canonical=r.require("CanonicalOccurrences",c.organizationId(),uuid(input.existingCanonicalId()));auth.authorizeScopes(c,capability,scopes(canonical));
      if(!Objects.equals(input.physicalScopeId(),canonical.get("physicalScopeId"))||!Objects.equals(claim.get("subjectId"),canonical.get("subjectId"))||!Objects.equals(event.get("kind"),canonical.get("kind"))
          ||!sameQuantity(quantity,canonical.get("quantity"))||!Objects.equals(input.unit(),canonical.get("unit"))||!input.effectiveFrom().equals(instant(canonical.get("effectiveFrom"))))decision="CONFLICT";
    }
    var row=new LinkedHashMap<String,Object>();row.put("ID",UUID.randomUUID().toString());row.put("organizationId",c.organizationId());row.put("revision",1);var now=clock.instant();row.put("createdAt",now);row.put("recordedAt",now);row.put("recordedBy",c.actorId());
    for(String field:List.of("subjectKind","subjectId","itemId","placeId","workId","sourceProfileId"))if(claim.get(field)!=null)row.put(field,claim.get(field));
    row.put("claimId",claim.get("ID"));row.put("basisDocumentId",doc.get("ID"));if(input.physicalScopeId()!=null)row.put("physicalScopeId",uuid(input.physicalScopeId()));
    if(input.existingCanonicalId()!=null)row.put("existingCanonicalId",uuid(input.existingCanonicalId()));
    row.put("policyVersion",input.policyVersion());row.put("decision",decision);row.put("reason",input.reason());row.put("sourceIdentity",input.sourceIdentity());row.put("effectiveFrom",input.effectiveFrom());
    if(quantity!=null){row.put("quantity",quantity);row.put("unit",input.unit());}
    for(String field:List.of("intakeOwnerId","supervisorId","nextAction","nextCheckAt"))row.put(field,profile.get(field));
    r.insert("Reconciliations",row);
    return Map.of("id",row.get("ID"),"revision",1,"outcome",decision,"inventoryEffects","NONE");
  }
  private static boolean receiptPayloadMatches(com.fasterxml.jackson.databind.JsonNode payload,com.mulino.application.trade.ReceiptEvidenceScopePort.Scope range){
    try{return payload.path("rangeRootId").isTextual()&&range.receiptRangeId().equals(payload.path("rangeRootId").asText())&&payload.path("startQuantity").isTextual()&&range.startQuantity().compareTo(new BigDecimal(payload.path("startQuantity").asText()))==0&&range.itemId().equals(payload.path("itemId").asText())&&Objects.equals(range.lotId(),payload.hasNonNull("lotId")?payload.get("lotId").asText():null)&&range.placeId().equals(payload.path("placeId").asText())&&Objects.equals(range.workId(),payload.hasNonNull("workId")?payload.get("workId").asText():null)&&payload.path("quantity").isTextual()&&range.quantity().compareTo(new BigDecimal(payload.path("quantity").asText()))==0&&range.unit().equals(payload.path("unit").asText())&&range.occurredAt().equals(Instant.parse(payload.path("occurredAt").asText()));}catch(RuntimeException invalid){return false;}
  }
  private static boolean sameQuantity(BigDecimal requested,Object stored) {
    return requested==null?stored==null:stored instanceof BigDecimal quantity&&requested.compareTo(quantity)==0;
  }
  @Transactional
  public Map<String,Object> link(DomainContext c,String reconciliationId) {
    var review=review(c,reconciliationId);var claim=claim(c,review.get("claimId").toString());authorizeReviewer(c,"linkCanonicalOccurrence",claim);
    if(!"MATCHED".equals(review.get("decision")))throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Reconciliation remains unresolved");
    // Recheck all source, policy, original, identity and quantity checks at the current commit fence.
    var checked=match(c,new Review(review.get("claimId").toString(),review.get("basisDocumentId").toString(),review.get("physicalScopeId").toString(),Objects.toString(review.get("existingCanonicalId"),null),
      review.get("policyVersion").toString(),review.get("sourceIdentity").toString(),Objects.toString(review.get("quantity"),null),Objects.toString(review.get("unit"),null),instant(review.get("effectiveFrom")),review.get("reason").toString()),"linkCanonicalOccurrence");
    if(!"MATCHED".equals(checked.get("outcome")))throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Current review checks failed");
    String physical=review.get("physicalScopeId").toString();String existing=Objects.toString(review.get("existingCanonicalId"),null);
    var event=r.require("Events",c.organizationId(),claim.get("eventId").toString());
    r.sourceFence(c.organizationId(),"canonical",physical,event.get("kind").toString());
    if(existing==null) {
      var candidates=r.rows("CanonicalOccurrences",c.organizationId()).stream().filter(x->physical.equals(x.get("physicalScopeId"))&&event.get("kind").equals(x.get("kind"))&&instant(claim.get("effectiveFrom")).equals(instant(x.get("effectiveFrom")))).toList();
      if(candidates.size()==1)existing=candidates.getFirst().get("ID").toString();
      if(candidates.size()>1)throw new DomainError("HELD","EVIDENCE_CONFLICT","Canonical scope has competing revisions");
    }
    var result=records.verifyReviewedCanonical(new EvidenceRecords.CanonicalInput(claim.get("ID").toString(),physical,review.get("basisDocumentId").toString(),review.get("policyVersion").toString(),true,true,true,true,true,existing,null,review.get("reason").toString()));
    completion.publish(c,result.get("id").toString(),claim.get("ID").toString(),review.get("basisDocumentId").toString());
    var impact=impacts.getIfAvailable();
    if(impact!=null)impact.evidenceLinked(c,claim.get("ID").toString(),result.get("id").toString());
    else {
      boolean hasWorks=r.db().run(Select.from("mulino.work.read.Works").where(x->x.get("organizationId").eq(c.organizationId()))).listOf(Map.class).stream()
        .anyMatch(w->claim.get("workId")!=null&&claim.get("workId").equals(w.get("ID"))||claim.get("itemId")!=null&&claim.get("itemId").equals(w.get("itemId")));
      if(hasWorks)throw new DomainError("HELD","POLICY_UNRESOLVED","Affected Work invalidation service unavailable");
    }
    return result;
  }
}
