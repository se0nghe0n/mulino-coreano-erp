package com.mulino.application.evidence;

import com.mulino.application.core.*;
import com.mulino.domain.evidence.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.adapters.blob.LocalBlobStore;
import com.sap.cds.ql.Select;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import static com.mulino.domain.evidence.EvidenceTypes.*;

/** Immutable recording ports joining the enclosing command transaction; no physical effects. */
@Service
public class EvidenceRecords {
  private final EvidenceRepository r;
  private final IdentityAuthorization auth;
  private final LocalBlobStore blobs;
  private final ExecutionClock clock;
  public EvidenceRecords(EvidenceRepository r,IdentityAuthorization auth,LocalBlobStore blobs,ExecutionClock clock) { this.r=r; this.auth=auth; this.blobs=blobs; this.clock=clock; }
  public record DocumentInput(Subject subject,String sourceNamespace,String sourceReference,String mediaType,
      String expectedHash,String provenance,String supersedesId) {}
  public record EventInput(Subject subject,String kind,String sourceNamespace,String externalEventId,String sourceVersion,
      EffectiveTime time,ValueState valueState,String payload,String supersedesId,String invalidatesId) {}
  public record ClaimInput(String eventId,String documentId,String assertion,String quantity,String unit,ValueState valueState,String supersedesId) {}
  public record CanonicalInput(String claimId,String physicalScopeId,String basisDocumentId,String policyVersion,
      boolean sourceMatched,boolean identityMatched,boolean quantityMatched,boolean timeMatched,boolean duplicateChecked,
      String existingCanonicalId,String supersedesId,String reason) {}

  @Transactional
  public Map<String,Object> attachDocument(DocumentInput input,byte[] content) {
    var c=now(); var profile=source(c,input.sourceNamespace());
    var row=base(c); subject(c,row,input.subject()); sourceFields(row,profile);
    row.put("sourceReference",text(input.sourceReference(),320)); row.put("mediaType",text(input.mediaType(),160));
    row.put("provenance",text(input.provenance(),100000));
    authorize(c,"attachEvidence",row,input.subject().id());
    supersedes(c,"DocumentVersions",row,input.supersedesId());
    var staged=blobs.stage(content,input.expectedHash());
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      public void afterCompletion(int status) { if(status!=STATUS_COMMITTED)blobs.discard(staged.id()); }
    });
    blobs.commit(staged);
    row.put("blobId",staged.id().toString()); row.put("sha256",staged.sha256()); row.put("byteLength",staged.size()); row.put("availability","AVAILABLE");
    r.insert("DocumentVersions",row);
    return result(row,"RECORDED");
  }
  @Transactional
  public Map<String,Object> recordUnavailableDocument(DocumentInput input) {
    var c=now(); var profile=source(c,input.sourceNamespace()); var row=base(c); subject(c,row,input.subject()); sourceFields(row,profile);
    authorize(c,"attachEvidence",row,input.subject().id());
    if(input.expectedHash()==null||!input.expectedHash().matches("[0-9a-f]{64}"))throw DomainError.invalid("Invalid content hash");
    row.put("sha256",input.expectedHash()); row.put("byteLength",0L); row.put("mediaType",text(input.mediaType(),160));
    row.put("sourceReference",text(input.sourceReference(),320)); row.put("availability","UNKNOWN"); row.put("provenance",text(input.provenance(),100000));
    supersedes(c,"DocumentVersions",row,input.supersedesId()); r.insert("DocumentVersions",row); return result(row,"RECORDED_UNAVAILABLE");
  }
  @Transactional
  public Map<String,Object> recordActivity(EventInput input) {
    var c=now(); var profile=source(c,input.sourceNamespace()); var row=base(c); subject(c,row,input.subject()); sourceFields(row,profile);
    row.put("kind",text(input.kind(),80)); row.put("externalEventId",text(input.externalEventId(),160)); row.put("sourceVersion",text(input.sourceVersion(),80));
    row.put("payload",text(input.payload(),1000000)); row.put("payloadHash",eventHash(input));
    time(row,input.time()); row.put("valueState",Objects.requireNonNull(input.valueState()).name());
    authorize(c,input.supersedesId()==null?"recordActivity":"correctEvidence",row,input.subject().id());
    r.sourceFence(c.organizationId(),input.sourceNamespace(),input.externalEventId(),input.sourceVersion());
    var prior=r.rows("InboxRecords",c.organizationId()).stream().filter(x->input.sourceNamespace().equals(x.get("sourceNamespace"))&&input.externalEventId().equals(x.get("externalEventId"))&&input.sourceVersion().equals(x.get("sourceVersion"))).toList();
    for(var inbox:prior)if(row.get("payloadHash").equals(inbox.get("payloadHash"))) {
      var original=r.require("Events",c.organizationId(),inbox.get("eventId").toString());
      auth.authorizeScopes(c,"recordActivity",scopes(original));
      return result(original,"CONFLICT".equals(inbox.get("state"))?"EVIDENCE_CONFLICT":"REPLAYED");
    }
    supersedes(c,"Events",row,input.supersedesId());
    if(input.invalidatesId()!=null) { var invalidated=r.require("Events",c.organizationId(),uuid(input.invalidatesId()));sameSubject(row,invalidated);auth.authorizeScopes(c,"correctEvidence",scopes(invalidated));row.put("invalidatesId",input.invalidatesId()); }
    if(!prior.isEmpty())row.put("valueState","CONFLICT");
    r.insert("Events",row);
    var inbox=base(c); copySubject(row,inbox); sourceFields(inbox,profile); inbox.put("eventId",row.get("ID"));
    inbox.put("externalEventId",input.externalEventId()); inbox.put("sourceVersion",input.sourceVersion()); inbox.put("payloadHash",row.get("payloadHash"));
    inbox.put("state",prior.isEmpty()?"RECEIVED":"CONFLICT");
    if(!prior.isEmpty())inbox.put("conflictsWithId",prior.getFirst().get("ID"));
    for(String field:List.of("intakeOwnerId","supervisorId","nextAction","nextCheckAt"))inbox.put(field,profile.get(field));
    r.insert("InboxRecords",inbox); return result(row,prior.isEmpty()?"RECORDED":"EVIDENCE_CONFLICT");
  }
  @Transactional
  public Map<String,Object> recordClaim(ClaimInput input) {
    var c=now(); var event=r.require("Events",c.organizationId(),uuid(input.eventId()));
    auth.authorizeScopes(c,"recordActivity",scopes(event));
    var doc=r.require("DocumentVersions",c.organizationId(),uuid(input.documentId())); auth.authorizeScopes(c,"attachEvidence",scopes(doc));
    sameSubject(event,doc);
    var row=base(c); copySubject(event,row); copyTime(event,row);row.put("sourceProfileId",event.get("sourceProfileId")); row.put("eventId",event.get("ID")); row.put("documentVersionId",doc.get("ID"));
    row.put("assertion",text(input.assertion(),100000)); row.put("valueState",input.valueState().name());
    var quantity=quantity(input.quantity(),input.unit(),input.valueState());
    if(quantity!=null){
      if(row.get("itemId")!=null){var item=r.subject(c.organizationId(),"ITEM",row.get("itemId").toString());if(!input.unit().equals(item.get("baseUnit"))||quantity.stripTrailingZeros().scale()>((Number)item.get("decimalPlaces")).intValue())throw DomainError.invalid("Claim quantity violates item base unit precision");}
      row.put("quantity",quantity);row.put("unit",text(input.unit(),24));}
    supersedes(c,"Claims",row,input.supersedesId()); r.insert("Claims",row);
    link(c,doc.get("ID").toString(),null,row.get("ID").toString(),null,"CLAIM_SOURCE"); return result(row,"RECORDED");
  }
  @Transactional
  public Map<String,Object> verifyCanonical(CanonicalInput input) {
    var c=now(); var claim=r.require("Claims",c.organizationId(),uuid(input.claimId())); var event=r.require("Events",c.organizationId(),claim.get("eventId").toString());
    auth.authorizeScopes(c,"linkCanonicalOccurrence",scopes(event));
    if(r.rows("Events",c.organizationId()).stream().anyMatch(x->event.get("ID").equals(x.get("supersedesId"))||event.get("ID").equals(x.get("invalidatesId")))||r.rows("Claims",c.organizationId()).stream().anyMatch(x->claim.get("ID").equals(x.get("supersedesId"))))throw new DomainError("CONFLICT","EVIDENCE_CONFLICT","Superseded or invalidated evidence cannot become current canonical evidence");
    var basis=r.require("DocumentVersions",c.organizationId(),uuid(input.basisDocumentId())); auth.authorizeScopes(c,"getEvidence",scopes(basis));
    sameSubject(claim,basis);
    if(!input.sourceMatched()||!input.identityMatched()||!input.quantityMatched()||!input.timeMatched()||!input.duplicateChecked()||!"KNOWN".equals(claim.get("valueState"))||!"KNOWN".equals(event.get("valueState")))
      throw new DomainError("REJECTED","EVIDENCE_UNVERIFIED","Canonical identity and source checks incomplete");
    var profile=source(c,event.get("sourceNamespace").toString());
    long sourceVariants=r.rows("InboxRecords",c.organizationId()).stream().filter(x->Objects.equals(event.get("sourceNamespace"),x.get("sourceNamespace"))&&Objects.equals(event.get("externalEventId"),x.get("externalEventId"))&&Objects.equals(event.get("sourceVersion"),x.get("sourceVersion"))).count();
    if(sourceVariants>1)throw new DomainError("CONFLICT","EVIDENCE_CONFLICT","Source key has unresolved competing content");
    if(!Objects.equals(profile.get("policyVersion"),input.policyVersion()))throw DomainError.invalid("Source evidence policy mismatch");
    if(!"AVAILABLE".equals(basis.get("availability"))||basis.get("blobId")==null||!blobs.available(UUID.fromString(basis.get("blobId").toString()),basis.get("sha256").toString()))throw new DomainError("REJECTED","EVIDENCE_UNAVAILABLE","Verification original unavailable");
    String physical=uuid(input.physicalScopeId()); r.sourceFence(c.organizationId(),"canonical",physical,event.get("kind").toString());
    Map<String,Object> canonical;
    if(input.existingCanonicalId()!=null) {
      canonical=r.require("CanonicalOccurrences",c.organizationId(),uuid(input.existingCanonicalId()));
      sameSubject(claim,canonical);
      if(!physical.equals(canonical.get("physicalScopeId"))||!Objects.equals(claim.get("quantity"),canonical.get("quantity"))||!Objects.equals(claim.get("unit"),canonical.get("unit"))||!Objects.equals(event.get("kind"),canonical.get("kind"))||!instant(claim.get("effectiveFrom")).equals(instant(canonical.get("effectiveFrom")))||!Objects.equals(claim.get("effectiveUntil"),canonical.get("effectiveUntil"))||!Objects.equals(claim.get("timePrecision"),canonical.get("timePrecision")))throw DomainError.invalid("Canonical occurrence mismatch");
    } else {
      var existing=r.rows("CanonicalOccurrences",c.organizationId()).stream().filter(x->physical.equals(x.get("physicalScopeId"))&&Objects.equals(event.get("kind"),x.get("kind"))&&instant(claim.get("effectiveFrom")).equals(instant(x.get("effectiveFrom")))).toList();
      if(!existing.isEmpty()&&input.supersedesId()==null)throw new DomainError("CONFLICT","EVIDENCE_CONFLICT","Canonical occurrence already exists; explicit reconciliation needed");
      canonical=base(c); copySubject(claim,canonical);copyTime(claim,canonical);canonical.put("sourceProfileId",event.get("sourceProfileId"));canonical.put("kind",event.get("kind"));canonical.put("physicalScopeId",physical);canonical.put("valueState","KNOWN");canonical.put("reassessmentState","NOT_IMPLEMENTED");
      if(claim.get("quantity")!=null){canonical.put("quantity",claim.get("quantity"));canonical.put("unit",claim.get("unit"));}
      supersedes(c,"CanonicalOccurrences",canonical,input.supersedesId()); r.insert("CanonicalOccurrences",canonical);
    }
    var verification=base(c);verification.put("claimId",claim.get("ID"));verification.put("canonicalOccurrenceId",canonical.get("ID"));verification.put("basisDocumentId",basis.get("ID"));verification.put("policyVersion",input.policyVersion());
    for(String check:List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked"))verification.put(check,true);
    verification.put("verdict","VERIFIED");verification.put("reason",text(input.reason(),640));r.insert("Verifications",verification);
    link(c,basis.get("ID").toString(),null,null,canonical.get("ID").toString(),"VERIFICATION_BASIS");return result(canonical,"VERIFIED_RECORD_ONLY");
  }
  static String eventHash(EventInput input) {
    try {
      var fields=new TreeMap<String,Object>();
      fields.put("subjectKind",input.subject().kind().name());fields.put("subjectId",input.subject().id());fields.put("kind",input.kind());
      fields.put("sourceNamespace",input.sourceNamespace());fields.put("externalEventId",input.externalEventId());fields.put("sourceVersion",input.sourceVersion());
      fields.put("from",input.time().from().toString());fields.put("until",input.time().until()==null?null:input.time().until().toString());fields.put("zone",input.time().timeZone());fields.put("precision",input.time().precision());
      fields.put("valueState",input.valueState().name());fields.put("payload",input.payload());fields.put("supersedesId",input.supersedesId());fields.put("invalidatesId",input.invalidatesId());
      return LocalBlobStore.hash(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(fields));
    }catch(java.io.IOException e){throw new IllegalStateException(e);}
  }
  private DomainContext now() { var now=clock.instant(); var c=auth.context(now,now); auth.fence(c,List.of()); return c; }
  private Map<String,Object> base(DomainContext c) {
    var row=new LinkedHashMap<String,Object>();row.put("ID",UUID.randomUUID().toString());row.put("organizationId",c.organizationId());row.put("revision",1);var now=clock.instant();row.put("createdAt",now);row.put("recordedAt",now);row.put("recordedBy",c.actorId());return row;
  }
  Map<String,Object> source(DomainContext c,String namespace) {
    text(namespace,160);
    var profiles=r.rows("SourceProfiles",c.organizationId()).stream().filter(x->namespace.equals(x.get("namespace"))).toList();
    if(profiles.size()!=1)throw new DomainError("REJECTED","POLICY_UNRESOLVED","Source profile or intake responsibility unresolved");
    var profile=profiles.getFirst();text(Objects.toString(profile.get("policyVersion"),null),160);
    for(String role:List.of("intakeOwnerId","supervisorId")) {
      var actor=r.db().run(Select.from("mulino.identity.Actors").where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("ID").eq(profile.get(role))))).first().orElseThrow(DomainError::forbidden);
      if(!"HUMAN".equals(actor.get("kind")))throw new DomainError("REJECTED","POLICY_UNRESOLVED","Intake responsibility requires humans");
      Instant now=clock.instant();
      boolean active=r.db().run(Select.from("mulino.identity.Memberships").where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("actorId").eq(profile.get(role))))).listOf(Map.class).stream()
        .anyMatch(m->m.get("revokedAt")==null&&m.get("validFrom")!=null&&!instant(m.get("validFrom")).isAfter(now)&&(m.get("validUntil")==null||now.isBefore(instant(m.get("validUntil")))));
      if(!active)throw new DomainError("REJECTED","POLICY_UNRESOLVED","Intake responsibility requires active humans");
    }
    text(Objects.toString(profile.get("nextAction"),null),320);
    if(profile.get("nextCheckAt")==null)throw new DomainError("REJECTED","POLICY_UNRESOLVED","Intake next check unresolved");
    return profile;
  }
  private void sourceFields(Map<String,Object> row,Map<String,Object> profile) { row.put("sourceNamespace",profile.get("namespace"));row.put("sourceProfileId",profile.get("ID")); }
  private void subject(DomainContext c,Map<String,Object> row,Subject input) {
    var subject=r.subject(c.organizationId(),input.kind().name(),input.id());row.put("subjectKind",input.kind().name());row.put("subjectId",input.id());
    if(input.kind()==SubjectKind.ITEM)row.put("itemId",input.id()); else if(input.kind()==SubjectKind.PLACE)row.put("placeId",input.id());else if(input.kind()==SubjectKind.WORK)row.put("workId",input.id());
    for(String field:List.of("itemId","placeId","workId"))if(subject.get(field)!=null)row.put(field,subject.get(field));
  }
  private void authorize(DomainContext c,String capability,Map<String,Object> row,String target) {
    var dimensions=scopes(row);dimensions.put("TARGET",List.of(target));auth.authorizeScopes(c,capability,dimensions);
  }
  private void supersedes(DomainContext c,String entity,Map<String,Object> row,String previous) {
    if(previous==null)return;
    var old=r.require(entity,c.organizationId(),uuid(previous));sameSubject(old,row);auth.authorizeScopes(c,"correctEvidence",scopes(old));
    row.put("supersedesId",previous);row.put("revision",((Number)old.get("revision")).intValue()+1);
  }
  private static void sameSubject(Map<String,Object> a,Map<String,Object> b) {
    if(!Objects.equals(a.get("subjectKind"),b.get("subjectKind"))||!Objects.equals(a.get("subjectId"),b.get("subjectId")))throw DomainError.invalid("Evidence subject mismatch");
  }
  private static void time(Map<String,Object> row,EffectiveTime t) { row.put("effectiveFrom",t.from());if(t.until()!=null)row.put("effectiveUntil",t.until());row.put("timeZone",t.timeZone());row.put("timePrecision",t.precision()); }
  private static void copyTime(Map<String,Object> from,Map<String,Object> to) { for(String key:List.of("effectiveFrom","effectiveUntil","timeZone","timePrecision"))if(from.get(key)!=null)to.put(key,from.get(key)); }
  private static void copySubject(Map<String,Object> from,Map<String,Object> to) { for(String key:List.of("subjectKind","subjectId","itemId","placeId","workId"))if(from.get(key)!=null)to.put(key,from.get(key)); }
  private void link(DomainContext c,String document,String event,String claim,String canonical,String role) {
    var row=base(c);row.put("documentVersionId",document);if(event!=null)row.put("eventId",event);if(claim!=null)row.put("claimId",claim);if(canonical!=null)row.put("canonicalOccurrenceId",canonical);row.put("role",role);r.insert("EvidenceLinks",row);
  }
  private static Map<String,Object> result(Map<String,Object> row,String status) { return Map.of("outcome",status,"id",row.get("ID"),"revision",row.get("revision"),"inventoryEffects","NONE","workReassessment","NOT_REQUESTED"); }
}
