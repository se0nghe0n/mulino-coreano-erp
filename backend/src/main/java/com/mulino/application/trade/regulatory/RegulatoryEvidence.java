package com.mulino.application.trade.regulatory;
import com.mulino.application.core.*;
import com.mulino.application.evidence.EvidenceQueries;
import com.mulino.domain.evidence.EvidenceRepository;
import com.mulino.adapters.blob.LocalBlobStore;
import com.mulino.application.identity.IdentityAuthorization;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.mulino.domain.evidence.EvidenceTypes.*;
/** Immutable original content, source policy and current canonical verification agree. */
@Component
public class RegulatoryEvidence {
 private final EvidenceRepository r;private final EvidenceQueries q;private final LocalBlobStore blobs;private final IdentityAuthorization auth;
 public RegulatoryEvidence(EvidenceRepository r,EvidenceQueries q,LocalBlobStore blobs,IdentityAuthorization auth){this.r=r;this.q=q;this.blobs=blobs;this.auth=auth;}
 public Map<String,Object> document(DomainContext c,String id,String item){var d=r.require("DocumentVersions",c.organizationId(),id);auth.authorizeScopes(c,"getEvidence",scopes(d));if(instant(d.get("recordedAt")).isAfter(c.knownAt())||!item.equals(d.get("itemId"))||!"AVAILABLE".equals(d.get("availability"))||d.get("blobId")==null||!blobs.available(UUID.fromString(d.get("blobId").toString()),d.get("sha256").toString()))throw held();return d;}
 public Map<String,Object> require(DomainContext c,String occurrence,String kind,Map<String,Object> procedure,Map<String,Object> policy,Map<String,Object> expected){
  var fact=r.require("CanonicalOccurrences",c.organizationId(),occurrence);auth.authorizeScopes(c,"getEvidence",scopes(fact));
  if(expected.containsKey("quantity")&&(fact.get("quantity")==null||new java.math.BigDecimal(expected.get("quantity").toString()).compareTo(new java.math.BigDecimal(fact.get("quantity").toString()))!=0||!Objects.equals(expected.get("unit"),fact.get("unit"))))throw held();
  if(!q.verifiedAt(c,occurrence)||!kind.equals(fact.get("kind"))||!procedure.get("itemId").equals(fact.get("itemId"))||!procedure.get("physicalScopeId").equals(fact.get("physicalScopeId")))throw held();
  for(var v:r.rows("Verifications",c.organizationId())){
   if(!occurrence.equals(v.get("canonicalOccurrenceId"))||!"VERIFIED".equals(v.get("verdict"))||!policy.get("version").equals(v.get("policyVersion"))||!List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(k->Boolean.TRUE.equals(v.get(k))))continue;
   if(instant(v.get("recordedAt")).isAfter(c.knownAt()))continue;var claim=r.require("Claims",c.organizationId(),v.get("claimId").toString());var event=r.require("Events",c.organizationId(),claim.get("eventId").toString());
   if(!policy.get("sourceNamespace").equals(event.get("sourceNamespace"))||!kind.equals(event.get("kind"))||!"KNOWN".equals(event.get("valueState"))||!"KNOWN".equals(claim.get("valueState")))continue;
   var doc=document(c,v.get("basisDocumentId").toString(),procedure.get("itemId").toString());var profile=r.require("SourceProfiles",c.organizationId(),event.get("sourceProfileId").toString());if(!policy.get("version").equals(profile.get("policyVersion"))||!policy.get("sourceNamespace").equals(profile.get("namespace"))||!Objects.equals(profile.get("ID"),doc.get("sourceProfileId"))||!Objects.equals(profile.get("ID"),claim.get("sourceProfileId"))||!Objects.equals(profile.get("ID"),fact.get("sourceProfileId")))continue;
   try{
    var mapper=new ObjectMapper();var original=mapper.readTree(blobs.read(UUID.fromString(doc.get("blobId").toString()),doc.get("sha256").toString()));var payload=mapper.readTree(event.get("payload").toString());
    var all=new LinkedHashMap<String,Object>(expected);all.put("itemId",procedure.get("itemId"));all.put("lotId",procedure.get("lotId"));all.put("physicalScopeId",procedure.get("physicalScopeId"));all.put("authority",policy.get("authority"));all.put("policyVersion",policy.get("version"));
    boolean matches=all.entrySet().stream().allMatch(e->Objects.equals(original.path(e.getKey()).asText(),String.valueOf(e.getValue()))&&Objects.equals(payload.path(e.getKey()).asText(),String.valueOf(e.getValue())));
    if(matches)return fact;
   }catch(java.io.IOException ignored){}
  }throw held();
 }
 public String source(DomainContext c,String kind,String id){var row=r.require(kind,c.organizationId(),id);return row.get("sourceProfileId").toString();}
 public boolean current(DomainContext c,String occurrence){try{return q.verifiedAt(c,occurrence);}catch(DomainError e){return false;}}
 static DomainError held(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","Exact regulatory source and original evidence unavailable");}
}
