package com.mulino.application.evidence;
import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.domain.evidence.*;
import com.mulino.adapters.blob.LocalBlobStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import static com.mulino.domain.evidence.EvidenceTypes.*;
@Service
public class VerifiedExternalResultEvidence implements ExternalResultEvidenceGuard {
  private final EvidenceRepository r;private final EvidenceQueries queries;private final IdentityAuthorization auth;private final LocalBlobStore blobs;
  public VerifiedExternalResultEvidence(EvidenceRepository r,EvidenceQueries queries,IdentityAuthorization auth,LocalBlobStore blobs){this.r=r;this.queries=queries;this.auth=auth;this.blobs=blobs;}
  public void requireExternalResult(DomainContext c,String documentId,String externalOperationId,String decision){
    uuid(documentId);uuid(externalOperationId);
    if(!Set.of("CONFIRMED_SUCCESS","CONFIRMED_FAILURE").contains(decision))throw DomainError.invalid("Confirmed external decision required");
    var doc=r.require("DocumentVersions",c.organizationId(),documentId);auth.authorizeScopes(c,"getEvidence",scopes(doc));
    if(!"AVAILABLE".equals(doc.get("availability"))||doc.get("blobId")==null||!blobs.available(UUID.fromString(doc.get("blobId").toString()),doc.get("sha256").toString()))throw unavailable();
    try {
      var original=new ObjectMapper().readTree(blobs.read(UUID.fromString(doc.get("blobId").toString()),doc.get("sha256").toString()));
      if(!externalOperationId.equals(original.path("externalOperationId").asText())||!decision.equals(original.path("outcome").asText()))throw unavailable();
    }catch(java.io.IOException invalidOriginal){throw unavailable();}
    for(var verification:r.rows("Verifications",c.organizationId())){
      if(!documentId.equals(verification.get("basisDocumentId"))||!"VERIFIED".equals(verification.get("verdict")))continue;
      if(!List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(flag->Boolean.TRUE.equals(verification.get(flag))))continue;
      var canonical=r.require("CanonicalOccurrences",c.organizationId(),verification.get("canonicalOccurrenceId").toString());
      if(!"EXTERNAL_RESULT".equals(canonical.get("kind"))||!externalOperationId.equals(canonical.get("physicalScopeId"))||!queries.verifiedAt(c,canonical.get("ID").toString()))continue;
      var claim=r.require("Claims",c.organizationId(),verification.get("claimId").toString());var event=r.require("Events",c.organizationId(),claim.get("eventId").toString());
      if(!"EXTERNAL_RESULT".equals(event.get("kind"))||!"KNOWN".equals(event.get("valueState"))||!"KNOWN".equals(claim.get("valueState")))continue;
      auth.authorizeScopes(c,"getEvidence",scopes(canonical));
      r.sourceFence(c.organizationId(),event.get("sourceNamespace").toString(),event.get("externalEventId").toString(),event.get("sourceVersion").toString());
      try{
        var payload=new ObjectMapper().readTree(event.get("payload").toString());
        if(externalOperationId.equals(payload.path("externalOperationId").asText())&&decision.equals(payload.path("outcome").asText()))return;
      }catch(java.io.IOException ignored){/* malformed immutable original remains unverified */}
    }
    throw unavailable();
  }
  private static DomainError unavailable(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","Exact verified external operation evidence unavailable");}
}
