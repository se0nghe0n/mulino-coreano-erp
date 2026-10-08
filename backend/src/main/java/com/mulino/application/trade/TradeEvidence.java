package com.mulino.application.trade;

import com.mulino.application.core.*;
import com.mulino.application.evidence.EvidenceQueries;
import com.mulino.domain.evidence.EvidenceRepository;
import com.mulino.domain.evidence.EvidenceTypes;
import com.mulino.adapters.blob.LocalBlobStore;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;

/** Exact canonical fact gate; a document or caller-provided verified flag is insufficient. */
@Component
public class TradeEvidence {
  private final EvidenceRepository repository;
  private final EvidenceQueries queries;
  private final ReadAuthorizer authorizer;
  private final LocalBlobStore blobs;
  public TradeEvidence(EvidenceRepository repository,EvidenceQueries queries,ReadAuthorizer authorizer,LocalBlobStore blobs){this.repository=repository;this.queries=queries;this.authorizer=authorizer;this.blobs=blobs;}

  public Map<String,Object> requireCanonical(DomainContext c,String id,String kind,String itemId,
      String physicalScopeId,BigDecimal quantity,String unit){
    var verified=verifiedCanonical(c,id,kind,itemId,physicalScopeId,quantity,unit);
    if(verified.isEmpty())throw unverified();
    return verified.getFirst();
  }

  /** Every current verified original/claim/event chain of one exact canonical fact; empty when none qualifies. */
  public List<Map<String,Object>> verifiedCanonical(DomainContext c,String id,String kind,String itemId,
      String physicalScopeId,BigDecimal quantity,String unit){
    var chains=new ArrayList<Map<String,Object>>();
    var occurrence=repository.require("CanonicalOccurrences",c.organizationId(),EvidenceTypes.uuid(id));
    authorizer.authorizeScopes(c,"getEvidence",EvidenceTypes.scopes(occurrence));
    if(!visible(occurrence,c)||!kind.equals(occurrence.get("kind"))||!Objects.equals(itemId,occurrence.get("itemId"))||!Objects.equals(physicalScopeId,occurrence.get("physicalScopeId"))||!decimal(quantity,occurrence.get("quantity"))||!Objects.equals(unit,occurrence.get("unit"))||!"KNOWN".equals(occurrence.get("valueState"))||!"COMPLETE".equals(occurrence.get("reassessmentState")))throw unverified();
    if(repository.rows("CanonicalOccurrences",c.organizationId()).stream().anyMatch(x->visible(x,c)&&id.equals(x.get("supersedesId"))))throw unverified();
    if(!queries.verifiedAt(c,id))throw unverified();
    for(var verification:repository.rows("Verifications",c.organizationId())){
      if(!visible(verification,c)||!id.equals(verification.get("canonicalOccurrenceId"))||!"VERIFIED".equals(verification.get("verdict"))||!List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(k->Boolean.TRUE.equals(verification.get(k))))continue;
      var claim=repository.require("Claims",c.organizationId(),verification.get("claimId").toString());var event=repository.require("Events",c.organizationId(),claim.get("eventId").toString());var doc=repository.require("DocumentVersions",c.organizationId(),verification.get("basisDocumentId").toString());
      if(!visible(claim,c)||!visible(event,c)||!visible(doc,c)||!"KNOWN".equals(claim.get("valueState"))||!"KNOWN".equals(event.get("valueState")))continue;
      if(!kind.equals(event.get("kind"))||!Objects.equals(itemId,claim.get("itemId"))||!decimal(quantity,claim.get("quantity"))||!Objects.equals(unit,claim.get("unit"))||!Objects.equals(occurrence.get("subjectKind"),claim.get("subjectKind"))||!Objects.equals(occurrence.get("subjectId"),claim.get("subjectId"))||!EvidenceTypes.instant(occurrence.get("effectiveFrom")).equals(EvidenceTypes.instant(claim.get("effectiveFrom"))))continue;
      var profile=repository.require("SourceProfiles",c.organizationId(),event.get("sourceProfileId").toString());if(!Objects.equals(profile.get("policyVersion"),verification.get("policyVersion")))continue;
      if(!authorizer.permittedScopes(c,"getEvidence",EvidenceTypes.scopes(claim))||!authorizer.permittedScopes(c,"getEvidence",EvidenceTypes.scopes(event))||!authorizer.permittedScopes(c,"getEvidence",EvidenceTypes.scopes(doc)))continue;
      if(!"AVAILABLE".equals(doc.get("availability"))||doc.get("blobId")==null||!blobs.available(UUID.fromString(doc.get("blobId").toString()),doc.get("sha256").toString()))continue;
      if(repository.rows("Claims",c.organizationId()).stream().anyMatch(x->visible(x,c)&&claim.get("ID").equals(x.get("supersedesId")))||repository.rows("Events",c.organizationId()).stream().anyMatch(x->visible(x,c)&&(event.get("ID").equals(x.get("supersedesId"))||event.get("ID").equals(x.get("invalidatesId"))))||repository.rows("DocumentVersions",c.organizationId()).stream().anyMatch(x->visible(x,c)&&doc.get("ID").equals(x.get("supersedesId"))))continue;
      var inbox=repository.rows("InboxRecords",c.organizationId()).stream().filter(x->visible(x,c)&&Objects.equals(event.get("sourceNamespace"),x.get("sourceNamespace"))&&Objects.equals(event.get("externalEventId"),x.get("externalEventId"))&&Objects.equals(event.get("sourceVersion"),x.get("sourceVersion"))).toList();
      if(inbox.size()!=1||"CONFLICT".equals(inbox.getFirst().get("state")))continue;
      var result=new LinkedHashMap<String,Object>(occurrence);result.put("verificationId",verification.get("ID"));result.put("evidenceRefs",List.of(id,verification.get("ID"),claim.get("ID"),event.get("ID"),doc.get("ID")));chains.add(result);
    }
    return List.copyOf(chains);
  }
  private static boolean decimal(BigDecimal expected,Object value){return expected==null?value==null:value instanceof BigDecimal actual&&expected.compareTo(actual)==0;}
  private static boolean visible(Map<String,Object> row,DomainContext c){return !EvidenceTypes.instant(row.get("recordedAt")).isAfter(c.knownAt())&&(row.get("effectiveFrom")==null||!EvidenceTypes.instant(row.get("effectiveFrom")).isAfter(c.asOf()))&&(row.get("effectiveUntil")==null||c.asOf().isBefore(EvidenceTypes.instant(row.get("effectiveUntil"))));}
  private static DomainError unverified(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","Current immutable exact canonical fact required");}
}
