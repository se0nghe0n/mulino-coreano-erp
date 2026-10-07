package com.mulino.domain.evaluation;

import com.mulino.application.core.*;
import com.mulino.domain.definitions.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.mulino.domain.evaluation.EvaluationFacts.*;

/** CQN-backed observation provider. Raw claims and unsupported downstream adapters cannot fulfil a goal. */
@Component
public class AssessmentFactProvider {
  private final AssessmentRepository r;
  public AssessmentFactProvider(AssessmentRepository r){this.r=r;}
  public EvaluationFacts load(DomainContext c,Map<String,Object> work,Map<String,Object> slots,Definition definition) {
    var properties=new TreeMap<String,List<Fact>>();var relations=new TreeMap<String,List<Fact>>();
    var occurrences=r.rows(c,"mulino.evidence.CanonicalOccurrences");var verifications=r.rows(c,"mulino.evidence.Verifications");
    var claims=r.rows(c,"mulino.evidence.Claims");var events=r.rows(c,"mulino.evidence.Events");var inbox=r.rows(c,"mulino.evidence.InboxRecords");
    var documents=r.rows(c,"mulino.evidence.DocumentVersions");
    for(var attribute:definition.attributes()) {
      String property=attribute.nounType()+"."+attribute.name();var facts=new ArrayList<Fact>();
      if(attribute.name().equals("quantity")||attribute.name().equals("occurredAt"))for(var occurrence:occurrences) {
        if(!Objects.equals(work.get("ID"),occurrence.get("workId"))||!Objects.equals(work.get("itemId"),occurrence.get("itemId")))continue;
        if(slots.get("eventKind")!=null&&!Objects.equals(slots.get("eventKind"),occurrence.get("kind")))continue;
        if(slots.get("placeId")!=null&&!Objects.equals(slots.get("placeId"),occurrence.get("placeId")))continue;
        if(occurrences.stream().anyMatch(x->occurrence.get("ID").equals(x.get("supersedesId"))))continue;
        Instant effective=instant(occurrence.get("effectiveFrom"));
        if(slots.get("periodStart")!=null&&effective.isBefore(instant(slots.get("periodStart"))))continue;
        if(slots.get("periodEnd")!=null&&effective.isAfter(instant(slots.get("periodEnd"))))continue;
        if(effective.isAfter(c.asOf()))continue;
        var verification=verifications.stream().filter(v->Objects.equals(occurrence.get("ID"),v.get("canonicalOccurrenceId"))&&Objects.equals(slots.get("evidencePolicyVersion"),v.get("policyVersion"))&&"VERIFIED".equals(v.get("verdict"))&&List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(k->Boolean.TRUE.equals(v.get(k)))).findFirst();
        boolean verified=verification.isPresent();var refs=new ArrayList<String>();
        if(verified) {
          var v=verification.get();refs.add(v.get("ID").toString());
          var claim=claims.stream().filter(x->Objects.equals(v.get("claimId"),x.get("ID"))).findFirst();
          var document=documents.stream().filter(x->Objects.equals(v.get("basisDocumentId"),x.get("ID"))).findFirst();
          verified=claim.isPresent()&&document.isPresent()&&"AVAILABLE".equals(document.get().get("availability"));
          if(document.isPresent())refs.add(document.get().get("ID").toString());
          if(claim.isPresent()) {
            var event=events.stream().filter(x->Objects.equals(claim.get().get("eventId"),x.get("ID"))).findFirst();
            verified=verified&&event.isPresent();
            if(event.isPresent()) {
              var e=event.get();verified=verified&&!events.stream().anyMatch(x->Objects.equals(e.get("ID"),x.get("invalidatesId"))||Objects.equals(e.get("ID"),x.get("supersedesId")));
              verified=verified&&inbox.stream().filter(x->Objects.equals(e.get("sourceNamespace"),x.get("sourceNamespace"))&&Objects.equals(e.get("externalEventId"),x.get("externalEventId"))&&Objects.equals(e.get("sourceVersion"),x.get("sourceVersion"))).count()==1;
              refs.add(e.get("ID").toString());
            }
          }
        }
        State state=state(occurrence.get("valueState"));
        Object value=attribute.name().equals("occurredAt")?effective:occurrence.get("quantity");
        facts.add(new Fact(occurrence.get("ID").toString(),Objects.toString(occurrence.get("revision")),DefinitionRepository.sha256(occurrence.toString()),occurrence.get("ID").toString(),Objects.toString(occurrence.get("physicalScopeId"),null),value,Objects.toString(occurrence.get("unit"),null),state,verified,effective,instantOrNull(occurrence.get("effectiveUntil")),instant(occurrence.get("recordedAt")),refs));
      }
      // Existing segment facts represent actual state only; action eligibility and contribution adapters are S3/S4.
      if(attribute.name().equals("stateQuantity")&&!slots.containsKey("action")&&!Boolean.TRUE.equals(slots.get("includeReserved")))for(var segment:r.rows(c,"mulino.inventory.QuantitySegments")) {
        if(!Objects.equals(work.get("itemId"),segment.get("itemId"))||slots.get("placeId")!=null&&!Objects.equals(slots.get("placeId"),segment.get("placeId")))continue;
        boolean known="CONFIRMED".equals(segment.get("identificationStatus"))||"IDENTIFIED".equals(segment.get("identificationStatus"));
        Instant retired=instantOrNull(segment.get("retiredAt"));
        if(segment.get("retirementRecordedAt")!=null&&instant(segment.get("retirementRecordedAt")).isAfter(c.knownAt()))retired=null;
        facts.add(new Fact(segment.get("ID").toString(),Objects.toString(segment.get("revision")),DefinitionRepository.sha256(segment.toString()),null,segment.get("ID").toString(),segment.get("quantity"),Objects.toString(segment.get("unit")),known?State.KNOWN:State.UNKNOWN,known,instant(segment.get("validFrom")),retired,instant(segment.get("recordedAt")),List.of(Objects.toString(segment.get("evidenceRef"),""))));
      }
      properties.put(property,facts);
    }
    for(var relation:definition.relations()) {
      var facts=new ArrayList<Fact>();for(var row:r.rows(c,"mulino.inventory.ObjectRelations")) {
        if(!Objects.equals(work.get("itemId"),row.get("sourceId"))||!Objects.equals(definition.id(),row.get("definitionVersionId"))||!Objects.equals(relation.name(),row.get("relationDefinitionId")))continue;
        facts.add(new Fact(row.get("ID").toString(),Objects.toString(row.get("revision")),DefinitionRepository.sha256(row.toString()),null,null,row.get("targetId"),null,State.KNOWN,true,instant(row.get("validFrom")),instantOrNull(row.get("validUntil")),instant(row.get("recordedAt")),List.of()));
      }relations.put(relation.name(),facts);
    }
    return new EvaluationFacts(properties,relations);
  }
  static Instant instant(Object v){return v instanceof Instant i?i:OffsetDateTime.parse(v.toString()).toInstant();}
  static Instant instantOrNull(Object v){return v==null?null:instant(v);}
  private static State state(Object v){try{return State.valueOf(Objects.toString(v,"UNKNOWN"));}catch(IllegalArgumentException e){return State.UNKNOWN;}}
}
