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
 private final AssessmentService service;private final AssessmentRepository repository;private final ObjectProvider<ResponsibilityService> duties;private final ObjectMapper json=new ObjectMapper();
 public AssessmentCorrectionImpact(AssessmentService service,AssessmentRepository repository,ObjectProvider<ResponsibilityService> duties){this.service=service;this.repository=repository;this.duties=duties;}
 public void apply(DomainContext c,Correction correction){
  var ids=affected(c,correction.previousId(),correction.currentId(),correction.affectedWorkIds());
  var occurrences=relatedOccurrences(c,correction.previousId());
  for(String id:ids){
    service.invalidate(c,id);
    var duty=duties.getIfAvailable();if(duty==null)throw new DomainError("HELD","FOLLOWUP_UNAVAILABLE","Correction requires retained responsibility");
    if(occurrences.isEmpty())throw new DomainError("HELD","SOURCE_UNVERIFIED","Correction impact requires canonical source reconciliation");
    for(var occurrence:occurrences){
      var profile=repository.rows(c,"mulino.evidence.SourceProfiles").stream().filter(p->Objects.equals(p.get("ID"),occurrence.get("sourceProfileId"))).findFirst().orElseThrow(()->new DomainError("HELD","SOURCE_UNVERIFIED","Source follow-up profile missing"));
      if(profile.get("nextAction")==null||profile.get("nextCheckAt")==null)throw new DomainError("HELD","FOLLOWUP_UNAVAILABLE","Source next action and check required");
      duty.ensureCorrectionDuty(c,id,occurrence.get("ID").toString(),profile.get("nextAction").toString(),instant(profile.get("nextCheckAt")));
    }
  }
 }
 public void evidenceLinked(DomainContext c,String claimId,String canonicalId){for(String id:affected(c,claimId,canonicalId,Set.of()))service.invalidate(c,id);}
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
