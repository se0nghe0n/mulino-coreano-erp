package com.mulino.application.evidence;

import com.mulino.application.core.*;
import com.mulino.application.inventory.FulfillmentEvidencePurpose;
import com.mulino.domain.evidence.EvidenceRepository;
import com.mulino.adapters.blob.LocalBlobStore;
import java.util.*;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;
import static com.mulino.domain.evidence.EvidenceTypes.*;
import static com.mulino.application.evidence.EvidenceQueries.visible;

/** Shared canonical integrity chain. Only the authority strategy differs; no raw content is returned. */
@Component
public final class CanonicalEvidenceVerification {
  private final EvidenceRepository r;
  private final LocalBlobStore blobs;
  private final ReadAuthorizer auth;
  public CanonicalEvidenceVerification(EvidenceRepository r,LocalBlobStore blobs,ReadAuthorizer auth){this.r=r;this.blobs=blobs;this.auth=auth;}
  private record Graph(Map<String,Object> verification,Map<String,Object> claim,Map<String,Object> event,Map<String,Object> document) {}
  boolean verifiedForRead(DomainContext c,String canonicalId) {
    return verified(c,canonicalId,g->auth.permittedScopes(c,"getEvidence",scopes(g.claim()))
        &&auth.permittedScopes(c,"getEvidence",scopes(g.event()))&&auth.permittedScopes(c,"getEvidence",scopes(g.document())));
  }
  public boolean verifiedForFulfillment(DomainContext c,String canonicalId,String documentId,FulfillmentEvidencePurpose purpose) {
    Objects.requireNonNull(purpose);
    var canonical=r.require("CanonicalOccurrences",c.organizationId(),canonicalId);
    if(!Set.of("QUALITY_DECISION_recordDispositionBasis_QC","QUALITY_DECISION_recordDispositionBasis_CUSTOMER",
        "QUALITY_DECISION_recordDispositionBasis_COMMERCIAL","REGULATORY_SUBMISSION","REGULATORY_DECISION","LABEL_VERIFICATION").contains(canonical.get("kind"))
        ||!visible(canonical,c)||!purpose.permitsCanonical(c,canonical))return false;
    return verified(c,canonicalId,g->(documentId==null||documentId.equals(g.document().get("ID")))
        &&strict(c,canonical,g)&&purpose.permitsSource(c,g.claim())&&purpose.permitsSource(c,g.event())&&purpose.permitsSource(c,g.document()));
  }
  /** Causal RECORD reuses the same integrity chain without issuing a public evidence-read grant. */
  public boolean verifiedForDeliveryRecord(DomainContext c,String canonicalId,com.mulino.application.trade.sales.DeliveryRecordCause cause){
    Objects.requireNonNull(cause);var canonical=r.require("CanonicalOccurrences",c.organizationId(),canonicalId);
    if(!visible(canonical,c)||!cause.permitsCanonical(c,canonical))return false;
    return verified(c,canonicalId,g->strict(c,canonical,g)&&cause.permitsSource(c,g.claim())&&cause.permitsSource(c,g.event())&&cause.permitsSource(c,g.document()));
  }
  private boolean strict(DomainContext c,Map<String,Object> canonical,Graph g) {
    var v=g.verification();var claim=g.claim();var event=g.event();var doc=g.document();
    if(!List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(k->Boolean.TRUE.equals(v.get(k)))
        ||!visible(claim,c)||!visible(event,c)||!visible(doc,c)||!"KNOWN".equals(claim.get("valueState"))||!"KNOWN".equals(event.get("valueState"))
        ||!"KNOWN".equals(canonical.get("valueState"))||!Objects.equals(claim.get("documentVersionId"),doc.get("ID")))return false;
    for(String field:List.of("subjectKind","subjectId","sourceProfileId"))
      if(!Objects.equals(claim.get(field),event.get(field))||!Objects.equals(claim.get(field),doc.get(field))||!Objects.equals(claim.get(field),canonical.get(field)))return false;
    for(String field:List.of("quantity","unit","effectiveFrom","effectiveUntil","timePrecision","timeZone"))
      if(!Objects.equals(claim.get(field),canonical.get(field)))return false;
    for(String field:List.of("effectiveFrom","effectiveUntil","timePrecision","timeZone"))
      if(!Objects.equals(claim.get(field),event.get(field)))return false;
    if(!Objects.equals(event.get("kind"),canonical.get("kind")))return false;
    try {
      var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
      if(!mapper.readTree(blobs.read(UUID.fromString(doc.get("blobId").toString()),doc.get("sha256").toString())).equals(mapper.readTree(event.get("payload").toString())))return false;
    }catch(java.io.IOException|IllegalArgumentException invalid){return false;}
    var inbox=r.rows("InboxRecords",c.organizationId()).stream().filter(x->visible(x,c)&&Objects.equals(event.get("sourceNamespace"),x.get("sourceNamespace"))&&Objects.equals(event.get("externalEventId"),x.get("externalEventId"))&&Objects.equals(event.get("sourceVersion"),x.get("sourceVersion"))).toList();
    if(inbox.size()!=1||!Objects.equals(inbox.getFirst().get("eventId"),event.get("ID"))||!Objects.equals(inbox.getFirst().get("payloadHash"),event.get("payloadHash"))||"CONFLICT".equals(inbox.getFirst().get("state")))return false;
    if(!(event.get("sourceProfileId") instanceof String))return false;
    var profile=r.require("SourceProfiles",c.organizationId(),event.get("sourceProfileId").toString());
    if(!visible(profile,c)||!Objects.equals(profile.get("namespace"),event.get("sourceNamespace"))
        ||!Objects.equals(profile.get("policyVersion"),v.get("policyVersion")))return false;
    return r.rows("SourceProfiles",c.organizationId()).stream().filter(x->visible(x,c)&&Objects.equals(x.get("namespace"),profile.get("namespace"))).count()==1
        &&r.rows("CanonicalOccurrences",c.organizationId()).stream().noneMatch(x->visible(x,c)&&(canonical.get("ID").equals(x.get("supersedesId"))||canonical.get("ID").equals(x.get("invalidatesId"))));
  }
  private boolean verified(DomainContext c,String canonicalId,Predicate<Graph> authority) {
    return r.rows("Verifications",c.organizationId()).stream().anyMatch(v->{
      if(!canonicalId.equals(v.get("canonicalOccurrenceId"))||!"VERIFIED".equals(v.get("verdict"))||!visible(v,c))return false;
      var claim=r.require("Claims",c.organizationId(),v.get("claimId").toString());
      var event=r.require("Events",c.organizationId(),claim.get("eventId").toString());
      var basis=r.require("DocumentVersions",c.organizationId(),v.get("basisDocumentId").toString());
      if(!"AVAILABLE".equals(basis.get("availability"))||basis.get("blobId")==null||!blobs.available(UUID.fromString(basis.get("blobId").toString()),basis.get("sha256").toString()))return false;
      if(!authority.test(new Graph(v,claim,event,basis)))return false;
      if(r.rows("Events",c.organizationId()).stream().anyMatch(x->visible(x,c)&&(event.get("ID").equals(x.get("invalidatesId"))||event.get("ID").equals(x.get("supersedesId")))))return false;
      if(r.rows("DocumentVersions",c.organizationId()).stream().anyMatch(x->visible(x,c)&&basis.get("ID").equals(x.get("supersedesId"))))return false;
      if(r.rows("Claims",c.organizationId()).stream().anyMatch(x->visible(x,c)&&claim.get("ID").equals(x.get("supersedesId"))))return false;
      long variants=r.rows("InboxRecords",c.organizationId()).stream().filter(x->visible(x,c)&&Objects.equals(event.get("sourceNamespace"),x.get("sourceNamespace"))&&Objects.equals(event.get("externalEventId"),x.get("externalEventId"))&&Objects.equals(event.get("sourceVersion"),x.get("sourceVersion"))).count();
      return variants==1;
    });
  }
}
