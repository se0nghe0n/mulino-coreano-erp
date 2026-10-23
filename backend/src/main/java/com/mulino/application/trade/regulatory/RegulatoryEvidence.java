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
 private final com.mulino.application.evidence.CanonicalEvidenceVerification verification;private final EvidenceRepository r;private final EvidenceQueries q;private final LocalBlobStore blobs;private final IdentityAuthorization auth;
 public RegulatoryEvidence(EvidenceRepository r,EvidenceQueries q,LocalBlobStore blobs,IdentityAuthorization auth,com.mulino.application.evidence.CanonicalEvidenceVerification verification){this.verification=verification;this.r=r;this.q=q;this.blobs=blobs;this.auth=auth;}
 public Map<String,Object> document(DomainContext c,String id,String item){return document(c,id,item,null);}
 private Map<String,Object> document(DomainContext c,String id,String item,com.mulino.application.inventory.FulfillmentEvidencePurpose purpose){var d=r.require("DocumentVersions",c.organizationId(),id);if(purpose==null)auth.authorizeScopes(c,"getEvidence",scopes(d));else if(!purpose.permitsSource(c,d))throw held();if(instant(d.get("recordedAt")).isAfter(c.knownAt())||!item.equals(d.get("itemId"))||!"AVAILABLE".equals(d.get("availability"))||d.get("blobId")==null||!blobs.available(UUID.fromString(d.get("blobId").toString()),d.get("sha256").toString()))throw held();return d;}
 public Map<String,Object> require(DomainContext c,String occurrence,String kind,Map<String,Object> procedure,Map<String,Object> policy,Map<String,Object> expected){
  return require(c,occurrence,kind,procedure,policy,expected,null);
 }
 private Map<String,Object> require(DomainContext c,String occurrence,String kind,Map<String,Object> procedure,Map<String,Object> policy,Map<String,Object> expected,com.mulino.application.inventory.FulfillmentEvidencePurpose purpose){
  var fact=r.require("CanonicalOccurrences",c.organizationId(),occurrence);if(purpose==null)auth.authorizeScopes(c,"getEvidence",scopes(fact));else if(!purpose.permitsCanonical(c,fact))throw held();
  if(expected.containsKey("quantity")&&(fact.get("quantity")==null||new java.math.BigDecimal(expected.get("quantity").toString()).compareTo(new java.math.BigDecimal(fact.get("quantity").toString()))!=0||!Objects.equals(expected.get("unit"),fact.get("unit"))))throw held();
  if(!(purpose==null?q.verifiedAt(c,occurrence):verification.verifiedForFulfillment(c,occurrence,null,purpose))||!kind.equals(fact.get("kind"))||!procedure.get("itemId").equals(fact.get("itemId"))||!procedure.get("physicalScopeId").equals(fact.get("physicalScopeId")))throw held();
  for(var v:r.rows("Verifications",c.organizationId())){
   if(!occurrence.equals(v.get("canonicalOccurrenceId"))||!"VERIFIED".equals(v.get("verdict"))||!policy.get("version").equals(v.get("policyVersion"))||!List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(k->Boolean.TRUE.equals(v.get(k))))continue;
   if(instant(v.get("recordedAt")).isAfter(c.knownAt()))continue;var claim=r.require("Claims",c.organizationId(),v.get("claimId").toString());var event=r.require("Events",c.organizationId(),claim.get("eventId").toString());
   if(!policy.get("sourceNamespace").equals(event.get("sourceNamespace"))||!kind.equals(event.get("kind"))||!"KNOWN".equals(event.get("valueState"))||!"KNOWN".equals(claim.get("valueState")))continue;
   if(purpose!=null&&!verification.verifiedForFulfillment(c,occurrence,v.get("basisDocumentId").toString(),purpose))continue;
   var doc=document(c,v.get("basisDocumentId").toString(),procedure.get("itemId").toString(),purpose);var profile=r.require("SourceProfiles",c.organizationId(),event.get("sourceProfileId").toString());if(!policy.get("version").equals(profile.get("policyVersion"))||!policy.get("sourceNamespace").equals(profile.get("namespace"))||!Objects.equals(profile.get("ID"),doc.get("sourceProfileId"))||!Objects.equals(profile.get("ID"),claim.get("sourceProfileId"))||!Objects.equals(profile.get("ID"),fact.get("sourceProfileId")))continue;
   try{
    var mapper=new ObjectMapper();var original=mapper.readTree(blobs.read(UUID.fromString(doc.get("blobId").toString()),doc.get("sha256").toString()));var payload=mapper.readTree(event.get("payload").toString());
    var all=new LinkedHashMap<String,Object>(expected);all.put("itemId",procedure.get("itemId"));all.put("lotId",procedure.get("lotId"));all.put("physicalScopeId",procedure.get("physicalScopeId"));all.put("authority",policy.get("authority"));all.put("policyVersion",policy.get("version"));
    boolean matches=all.entrySet().stream().allMatch(e->(purpose==null?Objects.equals(original.path(e.getKey()).asText(),String.valueOf(e.getValue()))&&Objects.equals(payload.path(e.getKey()).asText(),String.valueOf(e.getValue())):sameValue(e.getKey(),e.getValue(),original.path(e.getKey()).asText())&&sameValue(e.getKey(),e.getValue(),payload.path(e.getKey()).asText())));
    if(matches)return fact;
   }catch(java.io.IOException ignored){}
  }throw held();
 }
 public String source(DomainContext c,String kind,String id){var row=r.require(kind,c.organizationId(),id);return row.get("sourceProfileId").toString();}
 public boolean current(DomainContext c,String occurrence){try{return q.verifiedAt(c,occurrence);}catch(DomainError e){return false;}}
 public boolean current(DomainContext c,String kind,Map<String,Object> row,Map<String,Object> procedure,Map<String,Object> policy,com.mulino.application.inventory.FulfillmentEvidencePurpose purpose){
  try{
   var expected=new LinkedHashMap<String,Object>();expected.put("procedureId",procedure.get("ID"));
   expected.put("procedureVersionId",kind.equals("REGULATORY_SUBMISSION")?row.get("previousVersionId"):row.get("procedureVersionId"));
   for(String field:kind.equals("REGULATORY_SUBMISSION")?List.of("occurredAt"):kind.equals("REGULATORY_DECISION")?List.of("decision","action","startQuantity","quantity","unit","validFrom","validUntil"):List.of("decision","packagingVersionId","specificationVersionId","validFrom","validUntil")){
    Object value=row.get(field);if(value!=null){if(value instanceof java.math.BigDecimal n)value=n.stripTrailingZeros().toPlainString();expected.put(field,value);}
   }
   if(!kind.equals("REGULATORY_SUBMISSION"))expected.put("validUntil",row.get("validUntil"));
   require(c,row.get("occurrenceId").toString(),kind,procedure,policy,expected,purpose);return true;
  }catch(DomainError e){return false;}
 }
 private static boolean sameValue(String key,Object value,String original){try{
  if(value==null)return original.isEmpty();
  if(Set.of("quantity","startQuantity").contains(key))return new java.math.BigDecimal(value.toString()).compareTo(new java.math.BigDecimal(original))==0;
  if(Set.of("occurredAt","validFrom","validUntil").contains(key))return instant(value).equals(java.time.Instant.parse(original));
  return value.toString().equals(original);
 }catch(IllegalArgumentException invalid){return false;}}
 static DomainError held(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","Exact regulatory source and original evidence unavailable");}
}
