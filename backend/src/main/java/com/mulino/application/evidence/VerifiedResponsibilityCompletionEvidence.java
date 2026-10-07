package com.mulino.application.evidence;
import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.application.responsibility.ResponsibilityCompletionEvidence;
import com.mulino.domain.evidence.EvidenceRepository;
import com.mulino.adapters.blob.LocalBlobStore;
import com.fasterxml.jackson.databind.*;
import com.sap.cds.ql.Select;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import static com.mulino.domain.evidence.EvidenceTypes.*;

/** Neither caller range hints nor quantity alone can discharge a different duty range. */
@Service
public class VerifiedResponsibilityCompletionEvidence implements ResponsibilityCompletionEvidence {
 private final EvidenceRepository r;private final IdentityAuthorization auth;private final EvidenceQueries queries;private final EvidenceRecords records;private final LocalBlobStore blobs;private final ExecutionClock clock;
 public VerifiedResponsibilityCompletionEvidence(EvidenceRepository r,IdentityAuthorization auth,EvidenceQueries queries,EvidenceRecords records,LocalBlobStore blobs,ExecutionClock clock){this.r=r;this.auth=auth;this.queries=queries;this.records=records;this.blobs=blobs;this.clock=clock;}
 record SourceRange(String rootId,BigDecimal start,BigDecimal quantity,String unit){}
 SourceRange sourceRange(DomainContext c,Map<String,Object> claim,Map<String,Object> event,Map<String,Object> doc,String physicalScope) {
  if(!"RESPONSE_COMPLETED".equals(event.get("kind"))||!"WORK".equals(claim.get("subjectKind")))throw unverified();
  auth.authorizeScopes(c,"getEvidence",scopes(doc));
  if(doc.get("blobId")==null||!"AVAILABLE".equals(doc.get("availability")))throw unverified();
  try {
   var mapper=new ObjectMapper();var original=mapper.readTree(blobs.read(UUID.fromString(doc.get("blobId").toString()),doc.get("sha256").toString()));var payload=mapper.readTree(event.get("payload").toString());
   String rootId=uuid(requiredText(original,"dutyRootId"));BigDecimal start=decimal(original,"startQuantity",true),quantity=decimal(original,"quantity",false);String unit=nullableText(original,"unit");
   if(!rootId.equals(requiredText(payload,"dutyRootId"))||start.compareTo(decimal(payload,"startQuantity",true))!=0||quantity.compareTo(decimal(payload,"quantity",false))!=0||!Objects.equals(unit,nullableText(payload,"unit")))throw unverified();
   var root=r.db().run(Select.from("mulino.responsibility.Roots").where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("ID").eq(rootId)))).first().orElseThrow(DomainError::forbidden);
   BigDecimal total=root.get("quantity")==null?BigDecimal.ONE:(BigDecimal)root.get("quantity");BigDecimal claimQuantity=claim.get("quantity")==null?BigDecimal.ONE:(BigDecimal)claim.get("quantity");
   var rootScope=mapper.readTree(root.get("scopeJson").toString());
   if(start.add(quantity).compareTo(total)>0||quantity.compareTo(claimQuantity)!=0||!Objects.equals(unit,root.get("unit"))||!Objects.equals(unit,claim.get("unit"))||!Objects.equals(physicalScope,rootScope.path("physicalScopeId").asText()))throw unverified();
   boolean assigned=r.db().run(Select.from("mulino.work.read.ObligationReferences").where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("rootId").eq(rootId)).and(x.get("workId").eq(claim.get("subjectId"))).and(x.get("valid").eq(true)))).rowCount()>0;
   if(!assigned)throw unverified();
   return new SourceRange(rootId,start,quantity,unit);
  }catch(java.io.IOException malformed){throw unverified();}
 }
 void publish(DomainContext c,String occurrenceId,String claimId,String basisDocumentId) {
  var claim=r.require("Claims",c.organizationId(),claimId);var event=r.require("Events",c.organizationId(),claim.get("eventId").toString());if(!"RESPONSE_COMPLETED".equals(event.get("kind")))return;
  var occurrence=r.require("CanonicalOccurrences",c.organizationId(),occurrenceId);var doc=r.require("DocumentVersions",c.organizationId(),basisDocumentId);var range=sourceRange(c,claim,event,doc,occurrence.get("physicalScopeId").toString());
  r.sourceFence(c.organizationId(),"completion-coverage",occurrenceId,"1");var existing=r.rows("CompletionCoverages",c.organizationId()).stream().filter(x->occurrenceId.equals(x.get("occurrenceId"))).findFirst();
  if(existing.isPresent()){var old=existing.get();if(!range.rootId().equals(old.get("rootId"))||range.start().compareTo((BigDecimal)old.get("startQuantity"))!=0||range.quantity().compareTo((BigDecimal)old.get("quantity"))!=0||!Objects.equals(range.unit(),old.get("unit")))throw unverified();return;}
  var verification=r.rows("Verifications",c.organizationId()).stream().filter(x->occurrenceId.equals(x.get("canonicalOccurrenceId"))&&claimId.equals(x.get("claimId"))&&basisDocumentId.equals(x.get("basisDocumentId"))&&"VERIFIED".equals(x.get("verdict"))).findFirst().orElseThrow(VerifiedResponsibilityCompletionEvidence::unverified);
  var row=new LinkedHashMap<String,Object>();var now=clock.instant();row.put("ID",UUID.randomUUID().toString());row.put("organizationId",c.organizationId());row.put("revision",1);row.put("createdAt",now);row.put("recordedAt",now);row.put("recordedBy",c.actorId());row.put("occurrenceId",occurrenceId);row.put("claimId",claimId);row.put("eventId",event.get("ID"));row.put("verificationId",verification.get("ID"));row.put("documentVersionId",basisDocumentId);row.put("rootId",range.rootId());row.put("startQuantity",range.start());row.put("quantity",range.quantity());if(range.unit()!=null)row.put("unit",range.unit());row.put("originalHash",doc.get("sha256"));row.put("sourcePayloadHash",event.get("payloadHash"));row.put("policyVersion",verification.get("policyVersion"));r.insert("CompletionCoverages",row);
 }
 public Coverage requireCoverage(DomainContext c,String occurrenceId) {
  uuid(occurrenceId);r.sourceFence(c.organizationId(),"completion-coverage",occurrenceId,"1");
  var coverage=r.rows("CompletionCoverages",c.organizationId()).stream().filter(x->occurrenceId.equals(x.get("occurrenceId"))).findFirst().orElseThrow(VerifiedResponsibilityCompletionEvidence::unverified);
  var claim=r.require("Claims",c.organizationId(),coverage.get("claimId").toString());var event=r.require("Events",c.organizationId(),coverage.get("eventId").toString());var doc=r.require("DocumentVersions",c.organizationId(),coverage.get("documentVersionId").toString());
  r.sourceFence(c.organizationId(),event.get("sourceNamespace").toString(),event.get("externalEventId").toString(),event.get("sourceVersion").toString());r.sourceFence(c.organizationId(),"document",doc.get("ID").toString(),"revision");r.sourceFence(c.organizationId(),"claim",claim.get("ID").toString(),"revision");
  var profile=records.source(c,event.get("sourceNamespace").toString());
  if(!Objects.equals(profile.get("policyVersion"),coverage.get("policyVersion"))||!Objects.equals(coverage.get("originalHash"),doc.get("sha256"))||!Objects.equals(coverage.get("sourcePayloadHash"),event.get("payloadHash"))||!queries.verifiedAt(c,occurrenceId))throw unverified();
  var occurrence=r.require("CanonicalOccurrences",c.organizationId(),occurrenceId);if(!"COMPLETE".equals(occurrence.get("reassessmentState"))||!"RESPONSE_COMPLETED".equals(occurrence.get("kind")))throw unverified();
  var selected=r.require("Verifications",c.organizationId(),coverage.get("verificationId").toString());if(!occurrenceId.equals(selected.get("canonicalOccurrenceId"))||!claim.get("ID").equals(selected.get("claimId"))||!doc.get("ID").equals(selected.get("basisDocumentId"))||!"VERIFIED".equals(selected.get("verdict"))||!List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(k->Boolean.TRUE.equals(selected.get(k))))throw unverified();
  var range=sourceRange(c,claim,event,doc,occurrence.get("physicalScopeId").toString());
  if(!range.rootId().equals(coverage.get("rootId"))||range.start().compareTo((BigDecimal)coverage.get("startQuantity"))!=0||range.quantity().compareTo((BigDecimal)coverage.get("quantity"))!=0||!Objects.equals(range.unit(),coverage.get("unit")))throw unverified();
  return new Coverage(range.rootId(),range.start(),range.quantity(),range.unit(),selected.get("ID").toString(),coverage.get("ID").toString());
 }
 private static BigDecimal decimal(JsonNode node,String key,boolean zeroAllowed){String value=requiredText(node,key);BigDecimal number;try{number=new BigDecimal(value);}catch(NumberFormatException e){throw unverified();}if(number.signum()<0||(!zeroAllowed&&number.signum()==0)||number.scale()>12||number.precision()-number.scale()>26)throw unverified();return number;}
 private static String requiredText(JsonNode node,String key){var field=node.get(key);if(field==null||!field.isTextual()||field.asText().isBlank())throw unverified();return field.asText();}
 private static String nullableText(JsonNode node,String key){var field=node.get(key);if(field==null||field.isNull())return null;return requiredText(node,key);}
 private static DomainError unverified(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","Immutable verified completion duty range unavailable");}
}
