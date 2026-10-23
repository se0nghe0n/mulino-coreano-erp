package com.mulino.domain.evaluation;

import com.mulino.application.core.*;
import com.mulino.domain.definitions.*;
import com.mulino.adapters.blob.LocalBlobStore;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import com.mulino.application.work.WorkContributionRead;
import static com.mulino.domain.evaluation.EvaluationFacts.*;

/** CQN-backed observation provider. Raw claims and unsupported downstream adapters cannot fulfil a goal. */
@Component
public class AssessmentFactProvider {
  private final AssessmentRepository r; private final LocalBlobStore blobs;private final ObjectProvider<WorkContributionRead> contributionPort;private final ObjectProvider<CanonicalFactAdmission> admissions;
  public AssessmentFactProvider(AssessmentRepository r,LocalBlobStore blobs,ObjectProvider<WorkContributionRead> contributionPort,ObjectProvider<CanonicalFactAdmission> admissions){this.r=r;this.blobs=blobs;this.contributionPort=contributionPort;this.admissions=admissions;}
  public EvaluationFacts load(DomainContext c,Map<String,Object> work,Map<String,Object> slots,Definition definition){return load(c,work,slots,definition,null);}
  public EvaluationFacts load(DomainContext c,Map<String,Object> work,Map<String,Object> slots,Definition definition,String goalId) {
    return load(c,work,slots,definition,goalId,c.asOf());
  }
  /** The evidence cutoff can precede the real assessment horizon at an exclusive deadline.
   * STATE_AT keeps its fixed evaluationAt; only a genuinely future state is unknown. */
  public EvaluationFacts load(DomainContext c,Map<String,Object> work,Map<String,Object> slots,Definition definition,String goalId,Instant evaluationHorizon) {
    return load(c,work,slots,definition,goalId,evaluationHorizon,CanonicalFactAdmission.Purpose.ASSESS_GOAL);
  }
  public EvaluationFacts load(DomainContext c,Map<String,Object> work,Map<String,Object> slots,Definition definition,String goalId,Instant evaluationHorizon,CanonicalFactAdmission.Purpose purpose) {
    Objects.requireNonNull(purpose);
    var port=contributionPort.getIfAvailable();var credits=goalId==null||port==null?List.<Map<String,Object>>of():port.contributions(c,work.get("ID").toString(),goalId);
    Map<?,?> scope=slots.get("scope") instanceof Map<?,?> m?m:Map.of();
    Object lotScope=scope.get("lotId")!=null?scope.get("lotId"):work.get("lotId");
    Object placeScope=scope.get("placeId")!=null?scope.get("placeId"):slots.get("placeId");
    var segments=r.rows(c,"mulino.inventory.QuantitySegments");
    var templates=definition.goals().stream().filter(g->Objects.equals(slots.get("endpoint"),g.endpoint())&&Objects.equals(slots.get("quantityMode"),g.quantityMode())).toList();
    boolean canonicalState="STATE_AT".equals(slots.get("quantityMode"))&&templates.stream().anyMatch(g->usesCanonicalQuantity(g.predicate()));
    boolean stateGoal=!canonicalState&&("STATE_AT".equals(slots.get("quantityMode"))||templates.stream().anyMatch(g->usesCurrentState(g.predicate())));
    var movements=stateGoal?r.rows(c,"mulino.inventory.QuantityMovements"):List.<Map<String,Object>>of();
    Instant evaluationAt=canonicalState?instant(slots.get("evaluationAt")):c.asOf();
    var properties=new TreeMap<String,List<Fact>>();var relations=new TreeMap<String,List<Fact>>();
    var occurrences=r.rows(c,"mulino.evidence.CanonicalOccurrences");var verifications=r.rows(c,"mulino.evidence.Verifications");
    var claims=r.rows(c,"mulino.evidence.Claims");var events=r.rows(c,"mulino.evidence.Events");var inbox=r.rows(c,"mulino.evidence.InboxRecords");
    var documents=r.rows(c,"mulino.evidence.DocumentVersions");
    for(var attribute:definition.attributes()) {
      String property=attribute.nounType()+"."+attribute.name();var facts=new ArrayList<Fact>();
      boolean coverageAttribute=property.equals("QuantitySegment.observedQuantity")&&templates.stream().anyMatch(template->usesProperty(template.predicate(),property));
      if(coverageAttribute||!stateGoal&&(attribute.name().equals("quantity")||attribute.name().equals("occurredAt")))for(var occurrence:occurrences) {
        if(coverageAttribute&&!"STATE_OBSERVATION".equals(occurrence.get("kind")))continue;
        var allocations=credits.stream().filter(x->Objects.equals(occurrence.get("ID"),x.get("occurrenceId"))).toList();
        if((!coverageAttribute&&!canonicalState&&!Objects.equals(work.get("ID"),occurrence.get("workId"))&&allocations.isEmpty())||!Objects.equals(work.get("itemId"),occurrence.get("itemId")))continue;
        var domainProviders=admissions.stream().filter(p->p.eventKinds().contains(occurrence.get("kind"))).toList();
        var admission=domainProviders.size()==1?domainProviders.getFirst().admit(c,work.get("ID").toString(),goalId,occurrence,purpose):new CanonicalFactAdmission.Admission(!Set.of("PHYSICAL_RECEIPT","PHYSICAL_DELIVERY","RETURN_RECEIPT").contains(occurrence.get("kind"))&&domainProviders.isEmpty(),(java.math.BigDecimal)occurrence.get("quantity"),null,List.of());
        Object factLot=admission.lotId()!=null?admission.lotId():"LOT".equals(occurrence.get("subjectKind"))?occurrence.get("subjectId"):segments.stream().filter(x->Objects.equals(x.get("ID"),occurrence.get("physicalScopeId"))).map(x->x.get("lotId")).findFirst().orElse(null);
        if(lotScope!=null&&!Objects.equals(lotScope,factLot))continue;
        if(!coverageAttribute&&!CanonicalEventSemantics.matches(slots.get("eventKind"),occurrence.get("kind")))continue;
        if(placeScope!=null&&!Objects.equals(placeScope,occurrence.get("placeId")))continue;
        if(occurrences.stream().anyMatch(x->occurrence.get("ID").equals(x.get("supersedesId"))))continue;
        Instant effective=instant(occurrence.get("effectiveFrom"));
        if(!coverageAttribute&&slots.get("periodStart")!=null){Instant start=instant(slots.get("periodStart"));if(effective.isBefore(start)||effective.equals(start)&&Boolean.FALSE.equals(slots.get("periodStartInclusive")))continue;}
        if(!coverageAttribute&&slots.get("periodEnd")!=null){Instant end=instant(slots.get("periodEnd"));if(effective.isAfter(end)||effective.equals(end)&&Boolean.FALSE.equals(slots.get("periodEndInclusive")))continue;}
        if(effective.isAfter(c.asOf()))continue;
        // STATE_AT observes canonical evidence valid at this point, never current inventory.
        if(canonicalState&&(evaluationAt.isAfter(evaluationHorizon)||effective.isAfter(evaluationAt)||occurrence.get("effectiveUntil")!=null&&!evaluationAt.isBefore(instant(occurrence.get("effectiveUntil")))))continue;
        if(slots.containsKey("evidenceSource")&&verifications.stream().noneMatch(v->Objects.equals(occurrence.get("ID"),v.get("canonicalOccurrenceId"))&&matchesSource(v,slots,claims,events)))continue;
        var verification=verifications.stream().filter(v->Objects.equals(occurrence.get("ID"),v.get("canonicalOccurrenceId"))&&Objects.equals(slots.get("evidencePolicyVersion"),v.get("policyVersion"))&&matchesSource(v,slots,claims,events)&&"VERIFIED".equals(v.get("verdict"))&&List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(k->Boolean.TRUE.equals(v.get(k)))).findFirst();
        boolean verified=verification.isPresent();boolean sourceConflict=false;var refs=new ArrayList<String>();
        if(verified) {
          var v=verification.get();refs.add(v.get("ID").toString());
          var claim=claims.stream().filter(x->Objects.equals(v.get("claimId"),x.get("ID"))).findFirst();
          var document=documents.stream().filter(x->Objects.equals(v.get("basisDocumentId"),x.get("ID"))).findFirst();
          verified=claim.isPresent()&&document.isPresent()&&"AVAILABLE".equals(document.get().get("availability"))&&document.get().get("blobId")!=null&&blobs.available(UUID.fromString(document.get().get("blobId").toString()),document.get().get("sha256").toString());
          if(document.isPresent()) {
            refs.add(document.get().get("ID").toString());
            var supersedingDocuments=documents.stream().filter(x->Objects.equals(document.get().get("ID"),x.get("supersedesId"))).toList();supersedingDocuments.forEach(x->refs.add(x.get("ID").toString()));verified=verified&&supersedingDocuments.isEmpty();
          }
          if(claim.isPresent()) {
            refs.add(claim.get().get("ID").toString());
            var supersedingClaims=claims.stream().filter(x->Objects.equals(claim.get().get("ID"),x.get("supersedesId"))).toList();supersedingClaims.forEach(x->refs.add(x.get("ID").toString()));verified=verified&&supersedingClaims.isEmpty();
            var event=events.stream().filter(x->Objects.equals(claim.get().get("eventId"),x.get("ID"))).findFirst();
            verified=verified&&event.isPresent();
            if(event.isPresent()) {
              var e=event.get();var supersedingEvents=events.stream().filter(x->Objects.equals(e.get("ID"),x.get("invalidatesId"))||Objects.equals(e.get("ID"),x.get("supersedesId"))).toList();supersedingEvents.forEach(x->refs.add(x.get("ID").toString()));verified=verified&&supersedingEvents.isEmpty();
              var variants=inbox.stream().filter(x->Objects.equals(e.get("sourceNamespace"),x.get("sourceNamespace"))&&Objects.equals(e.get("externalEventId"),x.get("externalEventId"))&&Objects.equals(e.get("sourceVersion"),x.get("sourceVersion"))).toList();
              variants.forEach(x->refs.add(x.get("ID").toString()));sourceConflict=variants.size()>1||variants.stream().anyMatch(x->"CONFLICT".equals(x.get("state")));verified=verified&&variants.size()==1&&!sourceConflict;
              refs.add(e.get("ID").toString());
            }
          }
        }
        verified&=admission.admitted();refs.addAll(admission.evidenceRefs());
        if(coverageAttribute){
          var observed=segments.stream().filter(segment->Objects.equals(segment.get("ID"),occurrence.get("physicalScopeId"))).toList();
          verified&=observed.size()==1&&"SEGMENT".equals(occurrence.get("subjectKind"))&&Objects.equals(occurrence.get("subjectId"),occurrence.get("physicalScopeId"));
          if(observed.size()==1){
            var segment=observed.getFirst();
            verified&=Objects.equals(segment.get("itemId"),occurrence.get("itemId"))&&Objects.equals(segment.get("placeId"),occurrence.get("placeId"))&&Objects.equals(segment.get("unit"),occurrence.get("unit"))
              &&segment.get("quantity") instanceof java.math.BigDecimal quantity&&occurrence.get("quantity") instanceof java.math.BigDecimal observedQuantity&&quantity.compareTo(observedQuantity)==0
              &&!effective.isBefore(instant(segment.get("validFrom")));
          }
        }
        if(admission.admitted()&&occurrence.get("quantity") instanceof java.math.BigDecimal actual&&(admission.recognizedQuantity()==null||admission.recognizedQuantity().signum()<0||admission.recognizedQuantity().compareTo(actual)>0))verified=false;
        State state=sourceConflict?State.CONFLICT:state(occurrence.get("valueState"));
        Object value=attribute.name().equals("occurredAt")?effective:admission.admitted()?admission.recognizedQuantity():occurrence.get("quantity");
        Instant until=instantOrNull(occurrence.get("effectiveUntil"));
        if(Set.of("EXISTS_IN","THROUGHOUT").contains(slots.get("quantityMode"))&&until==null)until=effective.plusNanos(1);
        if(coverageAttribute){
          // A point observation covers one instant. Explicit canonical validity is required for an interval;
          // even verified source assertions cannot observe a segment beyond its known physical lifetime.
          var observed=segments.stream().filter(segment->Objects.equals(segment.get("ID"),occurrence.get("physicalScopeId"))).findFirst();
          if(observed.isPresent()&&observed.get().get("retiredAt")!=null&&observed.get().get("retirementRecordedAt")!=null&&!instant(observed.get().get("retirementRecordedAt")).isAfter(c.knownAt())){
            Instant retired=instant(observed.get().get("retiredAt"));if(until==null||retired.isBefore(until))until=retired;
          }
        }
        // A directly admitted canonical quantity is the superset of its allocations.
        // Keep it once even after the first partial allocation; allocation IDs remain
        // in the immutable input snapshot for lineage and correction invalidation.
        if(coverageAttribute||allocations.isEmpty()||Objects.equals(work.get("ID"),occurrence.get("workId")))facts.add(new Fact(coverageAttribute?Objects.toString(occurrence.get("physicalScopeId"),occurrence.get("ID").toString()):occurrence.get("ID").toString(),Objects.toString(occurrence.get("revision")),DefinitionRepository.sha256(occurrence.toString()),occurrence.get("ID").toString(),Objects.toString(occurrence.get("physicalScopeId"),null),value,Objects.toString(occurrence.get("unit"),null),state,verified,effective,until,instant(occurrence.get("recordedAt")),refs));
        else for(var credit:allocations){
          var creditRefs=new ArrayList<>(refs);creditRefs.add(credit.get("ID").toString());
          boolean boundCondition=!attribute.name().equals("quantity")||Objects.equals(property,credit.get("boundConditionId"))
            ||"TARGET".equals(credit.get("attributionRole"))&&credit.get("sourceGoalVersionId")==null&&"quantity".equals(credit.get("boundConditionId"));
          boolean validCredit=boundCondition&&!"UNKNOWN".equals(credit.get("attributionState"))&&occurrence.get("physicalScopeId")!=null&&Objects.equals(credit.get("unit"),occurrence.get("unit"))&&(admission.recognizedQuantity()==null||((java.math.BigDecimal)credit.get("startQuantity")).add((java.math.BigDecimal)credit.get("quantity")).compareTo(admission.recognizedQuantity())<=0);
          facts.add(new Fact(occurrence.get("ID").toString(),Objects.toString(occurrence.get("revision")),DefinitionRepository.sha256(occurrence.toString()),occurrence.get("ID").toString(),occurrence.get("physicalScopeId")+"|"+credit.get("ID"),attribute.name().equals("quantity")?credit.get("quantity"):value,Objects.toString(credit.get("unit")),"UNKNOWN".equals(credit.get("attributionState"))?State.UNKNOWN:validCredit?state:State.CONFLICT,verified&&validCredit,effective,until,instant(occurrence.get("recordedAt")),creditRefs));
        }
      }
      // Existing segment facts represent actual state only; action eligibility and contribution adapters are S3/S4.
      if(stateGoal&&(attribute.name().equals("stateQuantity")||attribute.name().equals("quantity"))&&(!slots.containsKey("action")||Set.of("PHYSICAL","PHYSICAL_HELD").contains(slots.get("action")))&&(!slots.containsKey("includeReserved")||Boolean.TRUE.equals(slots.get("includeReserved"))))for(var segment:segments) {
        if(lotScope!=null&&!Objects.equals(lotScope,segment.get("lotId")))continue;
        if(!Objects.equals(work.get("itemId"),segment.get("itemId"))||placeScope!=null&&!Objects.equals(placeScope,segment.get("placeId")))continue;
        boolean known="CONFIRMED".equals(segment.get("identificationStatus"))||"IDENTIFIED".equals(segment.get("identificationStatus"));
        Instant retired=instantOrNull(segment.get("retiredAt"));
        if(retired!=null&&segment.get("retirementRecordedAt")==null)known=false;
        if(segment.get("retirementRecordedAt")!=null&&instant(segment.get("retirementRecordedAt")).isAfter(c.knownAt()))retired=null;
        Instant horizon=c.asOf().plusNanos(1);
        Instant until=retired==null||retired.isAfter(c.asOf())?horizon:retired;
        String source=segment.get("ID").toString(),hash=DefinitionRepository.sha256(segment.toString());
        var refs=new ArrayList<String>();if(segment.get("evidenceRef")!=null)refs.add(segment.get("evidenceRef").toString());
        final Instant retirement=retired;
        var retirementMovements=retirement==null?List.<Map<String,Object>>of():movements.stream().filter(movement->source.equals(movement.get("sourceId"))&&retirement.equals(instant(movement.get("occurredAt")))).toList();
        retirementMovements.forEach(movement->refs.add(movement.get("ID").toString()));
        facts.add(new Fact(source,Objects.toString(segment.get("revision")),hash,null,source,segment.get("quantity"),Objects.toString(segment.get("unit")),known?State.KNOWN:State.UNKNOWN,known,instant(segment.get("validFrom")),until,instant(segment.get("recordedAt")),refs));
        // A committed retirement proves this exact segment contributes zero afterwards.
        // An empty inventory without such ledger evidence remains UNVERIFIED.
        if(retired!=null&&!retired.isAfter(c.asOf()))facts.add(new Fact(source,Objects.toString(segment.get("revision")),hash,null,source,java.math.BigDecimal.ZERO,Objects.toString(segment.get("unit")),known&&!retirementMovements.isEmpty()?State.KNOWN:State.UNKNOWN,known&&!retirementMovements.isEmpty(),retired,horizon,instant(segment.get("retirementRecordedAt")!=null?segment.get("retirementRecordedAt"):segment.get("recordedAt")),refs));
      }
      var conversions=r.rows(c,"mulino.inventory.UnitConversions");
      properties.put(property,facts.stream().map(f->normalize(f,attribute.unit(),work.get("itemId"),conversions,c.asOf())).toList());
    }
    for(var relation:definition.relations()) {
      var ids=r.rows(c,"mulino.definitions.RelationDefinitions").stream().filter(x->Objects.equals(definition.id(),x.get("definitionVersionId"))&&Objects.equals(relation.name(),x.get("name"))).map(x->x.get("ID")).toList();
      var facts=new ArrayList<Fact>();for(var row:r.rows(c,"mulino.inventory.ObjectRelations")) {
        if(!Objects.equals(work.get("itemId"),row.get("sourceId"))||!Objects.equals(definition.id(),row.get("definitionVersionId"))||!ids.contains(row.get("relationDefinitionId")))continue;
        facts.add(new Fact(row.get("ID").toString(),Objects.toString(row.get("revision")),DefinitionRepository.sha256(row.toString()),null,null,row.get("targetId"),null,State.KNOWN,true,instant(row.get("validFrom")),instantOrNull(row.get("validUntil")),instant(row.get("recordedAt")),List.of()));
      }relations.put(relation.name(),facts);
    }
    var referenced=new HashSet<String>();properties.values().stream().flatMap(Collection::stream).forEach(f->{referenced.add(f.sourceId());if(f.canonicalId()!=null)referenced.add(f.canonicalId());referenced.addAll(f.evidenceRefs());});
    var sources=new TreeMap<String,List<Map<String,Object>>>();
    for(String entity:List.of("CanonicalOccurrences","Verifications","Claims","Events","InboxRecords","DocumentVersions")) {
      var selected=r.rows(c,"mulino.evidence."+entity).stream().filter(x->referenced.contains(Objects.toString(x.get("ID")))||entity.equals("InboxRecords")&&referenced.contains(Objects.toString(x.get("eventId")))).map(x->new TreeMap<String,Object>(x)).map(x->(Map<String,Object>)x).toList();sources.put("evidence."+entity,selected);
    }
    sources.put("work.Contributions",credits);
    if(stateGoal)sources.put("inventory.QuantityMovements",movements.stream().filter(movement->referenced.contains(Objects.toString(movement.get("ID")))).toList());
    sources.put("inventory.UnitConversions",r.rows(c,"mulino.inventory.UnitConversions").stream().filter(x->referenced.contains(Objects.toString(x.get("ID")))).toList());
    return new EvaluationFacts(properties,relations,sources);
  }
  private boolean matchesSource(Map<String,Object> verification,Map<String,Object> slots,List<Map<String,Object>> claims,List<Map<String,Object>> events){
    if(!(slots.get("evidenceSource") instanceof Map<?,?> source))return true;
    return claims.stream().filter(claim->Objects.equals(claim.get("ID"),verification.get("claimId")))
      .anyMatch(claim->events.stream().anyMatch(event->Objects.equals(event.get("ID"),claim.get("eventId"))&&source.entrySet().stream().allMatch(e->Objects.equals(e.getValue(),event.get(e.getKey())))));
  }
  private boolean usesProperty(Map<String,Object> node,String property){
    if(property.equals(node.get("property")))return true;
    return node.get("children") instanceof List<?> children&&children.stream().anyMatch(child->child instanceof Map<?,?> m&&usesProperty((Map<String,Object>)m,property));
  }
  private boolean usesCanonicalQuantity(Map<String,Object> node){
    if("quantitySum".equals(node.get("operator")))return true;
    return node.get("children") instanceof List<?> children&&children.stream().anyMatch(x->x instanceof Map<?,?> m&&usesCanonicalQuantity((Map<String,Object>)m));
  }
  private boolean usesCurrentState(Map<String,Object> node){
    if("CURRENT_STATE".equals(node.get("evidenceSelector"))||"stateQuantity".equals(node.get("operator")))return true;
    if(node.get("children") instanceof List<?> children)return children.stream().anyMatch(x->x instanceof Map<?,?> m&&usesCurrentState((Map<String,Object>)m));return false;
  }
  private Fact normalize(Fact f,String expected,Object item,List<Map<String,Object>> conversions,Instant at){
    if(expected==null||f.unit()==null||Objects.equals(expected,f.unit())||f.value()==null)return f;
    Instant conversionAt=f.canonicalId()!=null?f.effectiveFrom():at;
    var matches=conversions.stream().filter(x->Objects.equals(item,x.get("itemId"))&&Objects.equals(f.unit(),x.get("fromUnit"))&&Objects.equals(expected,x.get("toUnit"))&&!conversionAt.isBefore(instant(x.get("validFrom")))&&(x.get("validUntil")==null||conversionAt.isBefore(instant(x.get("validUntil"))))&&x.get("evidenceRef")!=null).toList();
    if(matches.size()!=1)return new Fact(f.sourceId(),f.sourceVersion(),f.sourceHash(),f.canonicalId(),f.physicalScopeId(),f.value(),f.unit(),State.UNKNOWN,false,f.effectiveFrom(),f.effectiveUntil(),f.recordedAt(),f.evidenceRefs());
    var conversion=matches.getFirst();var factor=new java.math.BigDecimal(conversion.get("factor").toString());var value=new java.math.BigDecimal(f.value().toString()).multiply(factor);
    if(factor.signum()<=0||value.scale()>12||value.precision()-value.scale()>26)return new Fact(f.sourceId(),f.sourceVersion(),f.sourceHash(),f.canonicalId(),f.physicalScopeId(),f.value(),f.unit(),State.UNKNOWN,false,f.effectiveFrom(),f.effectiveUntil(),f.recordedAt(),f.evidenceRefs());
    var refs=new ArrayList<>(f.evidenceRefs());refs.add(conversion.get("ID").toString());refs.add(conversion.get("evidenceRef").toString());
    return new Fact(f.sourceId(),f.sourceVersion(),f.sourceHash(),f.canonicalId(),f.physicalScopeId(),value,expected,f.state(),f.verified(),f.effectiveFrom(),f.effectiveUntil(),f.recordedAt(),refs);
  }
  static Instant instant(Object v){return v instanceof Instant i?i:OffsetDateTime.parse(v.toString()).toInstant();}
  static Instant instantOrNull(Object v){return v==null?null:instant(v);}
  private static State state(Object v){try{return State.valueOf(Objects.toString(v,"UNKNOWN"));}catch(IllegalArgumentException e){return State.UNKNOWN;}}
}
