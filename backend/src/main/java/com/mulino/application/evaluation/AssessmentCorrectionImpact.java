package com.mulino.application.evaluation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.core.*;
import com.mulino.application.evidence.EvidenceCorrectionImpact;
import com.mulino.application.responsibility.ResponsibilityService;
import com.mulino.domain.evaluation.AssessmentRepository;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
/** Exact persisted input matches and typed target scopes. All changes share the evidence transaction. */
@Component
public class AssessmentCorrectionImpact implements EvidenceCorrectionImpact {
 private final AssessmentService service;private final AssessmentRepository repository;private final ObjectProvider<ResponsibilityService> duties;private final ObjectMapper json=new ObjectMapper();private final ObjectProvider<com.mulino.application.trade.DeliveryCorrectionPort> deliveryCorrections;private final com.mulino.domain.evidence.EvidenceRepository evidence;private final ObjectProvider<com.mulino.application.trade.SettlementContributionPort> settlements;
 public AssessmentCorrectionImpact(AssessmentService service,AssessmentRepository repository,ObjectProvider<ResponsibilityService> duties,ObjectProvider<com.mulino.application.trade.DeliveryCorrectionPort> deliveryCorrections,com.mulino.domain.evidence.EvidenceRepository evidence,ObjectProvider<com.mulino.application.trade.SettlementContributionPort> settlements){this.settlements=settlements;this.service=service;this.repository=repository;this.duties=duties;this.deliveryCorrections=deliveryCorrections;this.evidence=evidence;}
 public void apply(DomainContext request,Correction correction){
  // The correcting evidence row was recorded in this transaction after the request knownAt (moving clock); re-derive through a
  // context that covers exactly that row (plan §4.2 lock-then-reread, §4.3 정정 재평가).
  var c=request.knownThrough(recordedAt(request,correction.currentId()));
  var ids=affected(c,correction.previousId(),correction.currentId(),correction.affectedWorkIds());
  var occurrences=relatedOccurrences(c,correction.previousId());
  for(String id:ids){
    service.invalidate(c,id);
    var duty=duties.getIfAvailable();if(duty==null)throw new DomainError("HELD","FOLLOWUP_UNAVAILABLE","Correction requires retained responsibility");
    if(occurrences.isEmpty()){
      var raw=rawEvidence(c,correction.currentId());
      var profile=profile(c,raw.row.get("sourceProfileId"));
      duty.ensureEvidenceCorrectionDuty(c,id,correction.currentId(),raw.kind,profile.get("nextAction").toString(),instant(profile.get("nextCheckAt")));continue;
    }
    for(var occurrence:occurrences){
      var profile=repository.rows(c,"mulino.evidence.SourceProfiles").stream().filter(p->Objects.equals(p.get("ID"),occurrence.get("sourceProfileId"))).findFirst().orElseThrow(()->new DomainError("HELD","SOURCE_UNVERIFIED","Source follow-up profile missing"));
      if(profile.get("nextAction")==null||profile.get("nextCheckAt")==null)throw new DomainError("HELD","FOLLOWUP_UNAVAILABLE","Source next action and check required");
      duty.ensureCorrectionDuty(c,id,occurrence.get("ID").toString(),profile.get("nextAction").toString(),instant(profile.get("nextCheckAt")));
    }
  }
  // A receipt canonical is never superseded by a new canonical (EvidenceReconciliation only supersedes deliveries), so a corrected
  // receipt chain reaches settlement here: every invoice match on that receipt now reads UNVERIFIED and settlement opens its own
  // owned SETTLEMENT_DIFFERENCE in this transaction (plan §6 정산 "정산 차이는 별도 미해결 목표", §4.3, §5.3).
  // A delivery canonical is normally superseded by a relinked correcting canonical, which reaches settlement through evidenceLinked.
  // An invalidating correction never relinks, so its delivery matches read UNVERIFIED with no settlement owner unless settlement
  // re-derives them here as well (plan §6 정산, §4.3, §5.3).
  boolean invalidates=evidence.rows("Events",c.organizationId()).stream().anyMatch(x->correction.currentId().equals(x.get("ID"))&&x.get("invalidatesId")!=null);
  for(var occurrence:occurrences)if("PHYSICAL_RECEIPT".equals(occurrence.get("kind"))||invalidates&&"PHYSICAL_DELIVERY".equals(occurrence.get("kind"))){
   var settlement=settlements.stream().toList();if(settlement.size()>1)throw new DomainError("HELD","FOLLOWUP_UNAVAILABLE","Exact settlement contribution provider required");
   if(settlement.size()==1)settlement.getFirst().contributionChanged(c,occurrence.get("ID").toString());
  }
 }
 /** recordedAt of the exact correcting evidence row and the claims this correction recorded on it (no knownAt filter). */
 private Instant recordedAt(DomainContext c,String id){
  Instant latest=null;
  for(String table:List.of("Events","Claims","DocumentVersions"))for(var row:evidence.rows(table,c.organizationId()))if((id.equals(row.get("ID"))||"Claims".equals(table)&&id.equals(row.get("eventId")))&&row.get("recordedAt")!=null){var at=instant(row.get("recordedAt"));if(latest==null||at.isAfter(latest))latest=at;}
  return latest;
 }
 public void evidenceLinked(DomainContext request,String claimId,String canonicalId){
  // This transaction appended the canonical after the request knownAt fence; read the exact written row, never the historical view.
  var canonical=evidence.require("CanonicalOccurrences",request.organizationId(),canonicalId);
  // Under a moving clock the canonical and its verification are recorded after the request knownAt. Invalidation, correction and
  // settlement re-derivation must read exactly these just-written rows, so knowledge time is extended to cover them and no
  // further (plan §4.2 lock-then-reread, §4.3 정정 재평가). asOf is unchanged.
  var c=request.knownThrough(written(request,canonicalId,canonical));
  for(String id:affected(c,claimId,canonicalId,Set.of()))service.invalidate(c,id);
  if("PHYSICAL_DELIVERY".equals(canonical.get("kind"))&&canonical.get("supersedesId")!=null){
   var deliveries=repository.rows(c,"mulino.trade.sales.Deliveries").stream().filter(x->Objects.equals(x.get("observationId"),canonical.get("physicalScopeId"))).toList();
   if(deliveries.size()!=1)throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Delivery correction requires one applied original delivery");
   var ports=deliveryCorrections.stream().toList();if(ports.size()!=1)throw new DomainError("HELD","FOLLOWUP_UNAVAILABLE","Exact delivery correction responsibility provider required");
   ports.getFirst().correctionImpact(c,deliveries.getFirst().get("ID").toString(),canonicalId);
  }
  // A verified correcting canonical may change the recognized contribution behind an invoice match: settlement owns that difference (plan §6 정산, §4.3).
  if(canonical.get("supersedesId")!=null&&Set.of("PHYSICAL_DELIVERY","PHYSICAL_RECEIPT").contains(canonical.get("kind"))){
   var settlement=settlements.stream().toList();if(settlement.size()>1)throw new DomainError("HELD","FOLLOWUP_UNAVAILABLE","Exact settlement contribution provider required");
   if(settlement.size()==1)settlement.getFirst().contributionChanged(c,canonicalId);
  }
 }
 /** Latest recordedAt among the canonical row and the verification/link rows this link wrote for it. */
 private Instant written(DomainContext c,String canonicalId,Map<String,Object> canonical){
  Instant latest=instant(canonical.get("recordedAt"));
  for(String table:List.of("Verifications","EvidenceLinks"))for(var row:evidence.rows(table,c.organizationId()))if(canonicalId.equals(row.get("canonicalOccurrenceId"))&&row.get("recordedAt")!=null){var at=instant(row.get("recordedAt"));if(at.isAfter(latest))latest=at;}
  return latest;
 }
 private record Raw(String kind,Map<String,Object> row){}
 private Raw rawEvidence(DomainContext c,String id){
  for(var type:Map.of("EVENT","Events","CLAIM","Claims","DOCUMENT","DocumentVersions").entrySet())for(var row:repository.rows(c,"mulino.evidence."+type.getValue()))if(id.equals(row.get("ID")))return new Raw(type.getKey(),row);
  throw new DomainError("HELD","SOURCE_UNVERIFIED","Typed correction source missing");
 }
 private Map<String,Object> profile(DomainContext c,Object id){return repository.rows(c,"mulino.evidence.SourceProfiles").stream().filter(p->Objects.equals(p.get("ID"),id)&&p.get("nextAction")!=null&&p.get("nextCheckAt")!=null).findFirst().orElseThrow(()->new DomainError("HELD","FOLLOWUP_UNAVAILABLE","Source follow-up policy missing"));}
 private Set<String> affected(DomainContext c,String previous,String current,Set<String> direct){
  var ids=new TreeSet<>(direct);
  for(var snapshot:repository.rows(c,"mulino.evaluation.InputSnapshots"))try{if(contains(json.readValue(snapshot.get("contentJson").toString(),Object.class),previous))ids.add(snapshot.get("workId").toString());}catch(java.io.IOException e){throw DomainError.invalid("Invalid historical input snapshot");}
  var related=new ArrayList<>(relatedOccurrences(c,previous));related.addAll(relatedOccurrences(c,current));
  for(var occurrence:related){
    if(occurrence.get("workId")!=null)ids.add(occurrence.get("workId").toString());
    for(var work:repository.rows(c,"mulino.work.read.Works")) {
      if(!Set.of("ACTIVE","WAITING","CLOSED").contains(work.get("status"))||!Objects.equals(occurrence.get("itemId"),work.get("itemId")))continue;
      var goals=repository.rows(c,"mulino.work.read.GoalReferences").stream().filter(g->Objects.equals(g.get("ID"),work.get("currentGoalVersionId"))).toList();
      for(var goal:goals)try{var slots=json.readValue(Objects.toString(goal.get("slotsJson"),"{}"),Map.class);if(slots.get("placeId")!=null&&!Objects.equals(slots.get("placeId"),occurrence.get("placeId")))continue;if(slots.get("eventKind")!=null&&!Objects.equals(slots.get("eventKind"),occurrence.get("kind")))continue;ids.add(work.get("ID").toString());}catch(java.io.IOException e){throw DomainError.invalid("Invalid current goal scope");}
    }
    for(var credit:repository.rows(c,"mulino.work.WorkContributions"))if(Objects.equals(occurrence.get("ID"),credit.get("occurrenceId")))ids.add(credit.get("targetWorkId").toString());
  }
  // An IMPORTED S1 Work is an immutable reference (V9 work_lifecycle_guard); later evidence does not rewrite it, it only affects
  // command-managed Works (plan §5.1 "폐쇄 업무의 원 사건을 다시 쓰는 reopen은 기본 action으로 제공하지 않는다", §4.3).
  // Imported rows never change after import, so the knownAt view sees them; a command Work this transaction just rewrote stays included.
  for(var work:repository.rows(c,"mulino.work.read.Works"))if("IMPORTED".equals(work.get("lifecycleMode")))ids.remove(Objects.toString(work.get("ID"),""));
  return ids;
 }
 private List<Map<String,Object>> relatedOccurrences(DomainContext c,String id){
  var claimIds=new HashSet<String>();var canonicalIds=new HashSet<String>();
  for(var claim:repository.rows(c,"mulino.evidence.Claims"))if(id.equals(claim.get("ID"))||id.equals(claim.get("eventId"))||id.equals(claim.get("documentVersionId")))claimIds.add(claim.get("ID").toString());
  for(var v:repository.rows(c,"mulino.evidence.Verifications"))if(claimIds.contains(Objects.toString(v.get("claimId")))||id.equals(v.get("basisDocumentId")))canonicalIds.add(Objects.toString(v.get("canonicalOccurrenceId")));
  return repository.rows(c,"mulino.evidence.CanonicalOccurrences").stream().filter(o->id.equals(o.get("ID"))||canonicalIds.contains(o.get("ID").toString())).toList();
 }
 private boolean contains(Object value,String id){if(value instanceof Map<?,?> m)return m.values().stream().anyMatch(v->contains(v,id));if(value instanceof List<?> l)return l.stream().anyMatch(v->contains(v,id));return id.equals(value);}
 private static Instant instant(Object o){return o instanceof Instant i?i:OffsetDateTime.parse(o.toString()).toInstant();}
}
