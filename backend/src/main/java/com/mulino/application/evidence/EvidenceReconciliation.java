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
  private final ObjectProvider<EvidenceCorrectionImpact> impacts;
  public EvidenceReconciliation(EvidenceRepository r,EvidenceRecords records,IdentityAuthorization auth,LocalBlobStore blobs,ObjectProvider<EvidenceCorrectionImpact> impacts) {
    this.r=r;this.records=records;this.auth=auth;this.blobs=blobs;this.impacts=impacts;
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
    boolean sameQuantity=quantity!=null&&claim.get("quantity") instanceof BigDecimal q&&quantity.compareTo(q)==0&&Objects.equals(input.unit(),claim.get("unit"));
    boolean original=!r.rows("DocumentVersions",c.organizationId()).stream().anyMatch(x->doc.get("ID").equals(x.get("supersedesId")))&&"AVAILABLE".equals(doc.get("availability"))&&doc.get("blobId")!=null&&blobs.available(UUID.fromString(doc.get("blobId").toString()),doc.get("sha256").toString());
    boolean identity=false;
    if(input.physicalScopeId()!=null) {
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
      Set<String> overlapping=r.overlappingScopes(c.organizationId(),input.physicalScopeId());
      if(r.rows("CanonicalOccurrences",c.organizationId()).stream().anyMatch(x->!input.physicalScopeId().equals(x.get("physicalScopeId"))&&overlapping.contains(x.get("physicalScopeId"))&&event.get("kind").equals(x.get("kind"))))decision="CONFLICT";
    }
    if(input.physicalScopeId()!=null && r.rows("CanonicalOccurrences",c.organizationId()).stream().anyMatch(x->input.physicalScopeId().equals(x.get("physicalScopeId"))&&event.get("kind").equals(x.get("kind"))&&!input.effectiveFrom().equals(instant(x.get("effectiveFrom")))))decision="CONFLICT";
    if(input.effectiveFrom().isAfter(Instant.now()))decision="UNVERIFIED";
    if(input.existingCanonicalId()!=null) {
      var canonical=r.require("CanonicalOccurrences",c.organizationId(),uuid(input.existingCanonicalId()));auth.authorizeScopes(c,capability,scopes(canonical));
      if(!Objects.equals(input.physicalScopeId(),canonical.get("physicalScopeId"))||!Objects.equals(claim.get("subjectId"),canonical.get("subjectId"))||!Objects.equals(event.get("kind"),canonical.get("kind"))
          ||!(canonical.get("quantity") instanceof BigDecimal q)||quantity==null||quantity.compareTo(q)!=0||!Objects.equals(input.unit(),canonical.get("unit"))||!input.effectiveFrom().equals(instant(canonical.get("effectiveFrom"))))decision="CONFLICT";
    }
    var row=new LinkedHashMap<String,Object>();row.put("ID",UUID.randomUUID().toString());row.put("organizationId",c.organizationId());row.put("revision",1);row.put("createdAt",Instant.now());row.put("recordedAt",Instant.now());row.put("recordedBy",c.actorId());
    for(String field:List.of("subjectKind","subjectId","itemId","placeId","workId","sourceProfileId"))if(claim.get(field)!=null)row.put(field,claim.get(field));
    row.put("claimId",claim.get("ID"));row.put("basisDocumentId",doc.get("ID"));if(input.physicalScopeId()!=null)row.put("physicalScopeId",uuid(input.physicalScopeId()));
    if(input.existingCanonicalId()!=null)row.put("existingCanonicalId",uuid(input.existingCanonicalId()));
    row.put("policyVersion",input.policyVersion());row.put("decision",decision);row.put("reason",input.reason());row.put("sourceIdentity",input.sourceIdentity());row.put("effectiveFrom",input.effectiveFrom());
    if(quantity!=null){row.put("quantity",quantity);row.put("unit",input.unit());}
    for(String field:List.of("intakeOwnerId","supervisorId","nextAction","nextCheckAt"))row.put(field,profile.get(field));
    r.insert("Reconciliations",row);
    return Map.of("id",row.get("ID"),"revision",1,"outcome",decision,"inventoryEffects","NONE");
  }
  @Transactional
  public Map<String,Object> link(DomainContext c,String reconciliationId) {
    var review=review(c,reconciliationId);var claim=claim(c,review.get("claimId").toString());authorizeReviewer(c,"linkCanonicalOccurrence",claim);
    if(!"MATCHED".equals(review.get("decision")))throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Reconciliation remains unresolved");
    // Recheck all source, policy, original, identity and quantity checks at the current commit fence.
    var checked=match(c,new Review(review.get("claimId").toString(),review.get("basisDocumentId").toString(),review.get("physicalScopeId").toString(),Objects.toString(review.get("existingCanonicalId"),null),
      review.get("policyVersion").toString(),review.get("sourceIdentity").toString(),review.get("quantity").toString(),review.get("unit").toString(),instant(review.get("effectiveFrom")),review.get("reason").toString()),"linkCanonicalOccurrence");
    if(!"MATCHED".equals(checked.get("outcome")))throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Current review checks failed");
    String physical=review.get("physicalScopeId").toString();String existing=Objects.toString(review.get("existingCanonicalId"),null);
    var event=r.require("Events",c.organizationId(),claim.get("eventId").toString());
    r.sourceFence(c.organizationId(),"canonical",physical,event.get("kind").toString());
    if(existing==null) {
      var candidates=r.rows("CanonicalOccurrences",c.organizationId()).stream().filter(x->physical.equals(x.get("physicalScopeId"))&&event.get("kind").equals(x.get("kind"))&&instant(claim.get("effectiveFrom")).equals(instant(x.get("effectiveFrom")))).toList();
      if(candidates.size()==1)existing=candidates.getFirst().get("ID").toString();
      if(candidates.size()>1)throw new DomainError("HELD","EVIDENCE_CONFLICT","Canonical scope has competing revisions");
    }
    var result=records.verifyCanonical(new EvidenceRecords.CanonicalInput(claim.get("ID").toString(),physical,review.get("basisDocumentId").toString(),review.get("policyVersion").toString(),true,true,true,true,true,existing,null,review.get("reason").toString()));
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
