package com.mulino.application.quality;
import com.mulino.application.core.*;
import com.mulino.adapters.blob.LocalBlobStore;
import com.mulino.domain.evidence.EvidenceRepository;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class QualityEvidence {
 private final com.mulino.domain.inventory.QualityPrimitives decisions;private final com.mulino.application.evidence.CanonicalEvidenceVerification verification;private final EvidenceRepository r;private final LocalBlobStore blobs;private final com.mulino.application.evidence.EvidenceQueries queries;
 public QualityEvidence(EvidenceRepository r,LocalBlobStore blobs,com.mulino.application.evidence.EvidenceQueries queries,com.mulino.application.evidence.CanonicalEvidenceVerification verification,com.mulino.domain.inventory.QualityPrimitives decisions){this.decisions=decisions;this.verification=verification;this.r=r;this.blobs=blobs;this.queries=queries;}
 public void require(DomainContext c,String documentId,String segmentId){
  requireProof(c,documentId,segmentId,null,null);
 }
 private void requireProof(DomainContext c,String documentId,String segmentId,com.mulino.application.inventory.FulfillmentEvidencePurpose purpose,String expectedKind){
  var d=r.require("DocumentVersions",c.organizationId(),documentId);
  boolean verified=r.rows("Verifications",c.organizationId()).stream().anyMatch(v->documentId.equals(v.get("basisDocumentId"))&&"VERIFIED".equals(v.get("verdict"))&&List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(k->Boolean.TRUE.equals(v.get(k)))&&v.get("canonicalOccurrenceId") instanceof String canonical&&segmentId.equals(r.require("CanonicalOccurrences",c.organizationId(),canonical).get("physicalScopeId"))&&(expectedKind==null||expectedKind.equals(r.require("CanonicalOccurrences",c.organizationId(),canonical).get("kind")))&&(purpose==null?queries.verifiedAt(c,canonical):verification.verifiedForFulfillment(c,canonical,documentId,purpose)));
  if(!"SEGMENT".equals(d.get("subjectKind"))||!segmentId.equals(d.get("subjectId"))||!"AVAILABLE".equals(d.get("availability"))||d.get("blobId")==null||!blobs.available(UUID.fromString(d.get("blobId").toString()),d.get("sha256").toString())||r.rows("DocumentVersions",c.organizationId()).stream().anyMatch(x->documentId.equals(x.get("supersedesId")))||!verified)throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Original verified exact physical scope evidence required");
 }
 public void require(DomainContext c,String documentId,String segmentId,Map<String,Object>expected){require(c,documentId,segmentId);var d=r.require("DocumentVersions",c.organizationId(),documentId);if(expected.containsKey("quantity")){var amount=new java.math.BigDecimal(expected.get("quantity").toString());boolean matched=r.rows("Verifications",c.organizationId()).stream().anyMatch(v->documentId.equals(v.get("basisDocumentId"))&&"VERIFIED".equals(v.get("verdict"))&&v.get("canonicalOccurrenceId") instanceof String canonical&&r.require("CanonicalOccurrences",c.organizationId(),canonical).get("quantity") instanceof java.math.BigDecimal q&&q.compareTo(amount)==0);if(!matched)throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Canonical permission quantity mismatch");}try{var original=new com.fasterxml.jackson.databind.ObjectMapper().readTree(blobs.read(UUID.fromString(d.get("blobId").toString()),d.get("sha256").toString()));for(var e:expected.entrySet())if(!original.has(e.getKey())||!e.getValue().toString().equals(original.path(e.getKey()).asText()))throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Quality decision differs from immutable original");}catch(java.io.IOException malformed){throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Quality original JSON required");}}
 public boolean valid(DomainContext c,String documentId,String segmentId){try{require(c,documentId,segmentId);return true;}catch(DomainError e){return false;}}
 /** Fulfillment consumes only the exact immutable source backing this persisted permission. */
 public boolean valid(DomainContext c,Map<String,Object> basis,com.mulino.application.inventory.FulfillmentEvidencePurpose purpose){
  try{
   String documentId=(String)basis.get("evidenceRef");requireProof(c,documentId,(String)basis.get("segmentId"),purpose,"QUALITY_DECISION_recordDispositionBasis_"+basis.get("category"));
   var decision=decisions.decision(c,(String)basis.get("decisionId"));
   if(!documentId.equals(decision.get("evidenceId"))||!basis.get("segmentId").equals(decision.get("segmentId"))||!"recordDispositionBasis".equals(decision.get("operation")))return false;
   var d=r.require("DocumentVersions",c.organizationId(),documentId);
   var original=new com.fasterxml.jackson.databind.ObjectMapper().readTree(blobs.read(UUID.fromString(d.get("blobId").toString()),d.get("sha256").toString()));
   if(!"recordDispositionBasis".equals(original.path("operation").asText()))return false;
   for(String field:List.of("segmentId","workId","action","category","unit"))if(!Objects.equals(basis.get(field),original.path(field).asText()))return false;
   if(!Objects.equals(basis.get("customerId"),original.has("customerId")?original.path("customerId").asText():null))return false;
   for(String field:List.of("startQuantity","quantity"))if(new java.math.BigDecimal(original.path(field).asText()).compareTo((java.math.BigDecimal)basis.get(field))!=0)return false;
   for(String field:List.of("validFrom","validUntil"))if(!java.time.Instant.parse(original.path(field).asText()).equals(com.mulino.domain.evidence.EvidenceTypes.instant(basis.get(field))))return false;
   return true;
  }catch(DomainError|java.io.IOException|IllegalArgumentException e){return false;}
 }
}
