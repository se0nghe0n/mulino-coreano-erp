package com.mulino.application.work;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.core.*;
import com.mulino.application.evaluation.GoalQuantityProperty;
import com.mulino.domain.definitions.DefinitionRepository;
import com.mulino.domain.evaluation.CanonicalEventSemantics;
import com.mulino.domain.governance.PolicyRepository;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Resolves a quantity condition from the actual immutable goal, never from a caller label. */
@Component
public final class ContributionGoalBinding {
 private final DefinitionRepository definitions;
 private final ObjectProvider<WorkAssessmentGuard> guard;
 private final PolicyRepository policies;
 @org.springframework.beans.factory.annotation.Autowired private com.sap.cds.services.persistence.PersistenceService db;
 private final ObjectMapper json=new ObjectMapper();
 public ContributionGoalBinding(DefinitionRepository definitions,ObjectProvider<WorkAssessmentGuard> guard,PolicyRepository policies){this.definitions=definitions;this.guard=guard;this.policies=policies;}
 public String condition(DomainContext c,Map<String,Object> work,Map<String,Object> goal,Map<String,Object> actual,String unit){
  guard.getObject().requireSupportedGoal(c,goal);
  if(!Objects.equals(goal.get("workId"),work.get("ID"))||!Objects.equals(goal.get("ID"),work.get("currentGoalVersionId"))||!Objects.equals(actual.get("itemId"),work.get("itemId")))throw DomainError.invalid("Contribution goal identity differs from Work");
  var slots=decode(goal.get("slotsJson"));
  if(!"CUMULATIVE_EVENT".equals(slots.get("quantityMode"))||!Objects.equals(unit,slots.get("unit")))throw DomainError.invalid("Contribution requires a matching cumulative quantity goal");
  var definition=definitions.get(c.organizationId(),goal.get("definitionVersionId").toString());
  var templates=definition.goals().stream().filter(g->Objects.equals(g.endpoint(),slots.get("endpoint"))&&Objects.equals(g.quantityMode(),slots.get("quantityMode"))).toList();
  if(templates.size()!=1)throw DomainError.unsupported();
  String property=GoalQuantityProperty.resolve(definition,templates.getFirst().predicate(),unit);
  if(!"quantitySum".equals(GoalQuantityProperty.operator(templates.getFirst().predicate(),property,"CUMULATIVE_EVENT")))throw DomainError.unsupported();
  var scope=slots.get("scope") instanceof Map<?,?> m?m:Map.of();
  if(scope.get("workId")!=null&&!Objects.equals(scope.get("workId"),work.get("ID"))||scope.get("itemId")!=null&&!Objects.equals(scope.get("itemId"),work.get("itemId")))throw DomainError.forbidden();
  Object place=scope.get("placeId")!=null?scope.get("placeId"):slots.get("placeId");
  if(place!=null&&!Objects.equals(place,actual.get("placeId")))throw DomainError.invalid("Contribution place differs from pinned goal");
  Object lot=scope.get("lotId")!=null?scope.get("lotId"):work.get("lotId");
  if(lot!=null&&!Objects.equals(lot,actualLot(c,actual)))throw DomainError.invalid("Contribution lot differs from pinned goal");
  var at=instant(actual.get("effectiveFrom"));
  if(at.isAfter(c.asOf())||instant(actual.get("recordedAt")).isAfter(c.knownAt())||instant(goal.get("recordedAt")).isAfter(c.knownAt())||!CanonicalEventSemantics.matches(slots.get("eventKind"),actual.get("kind")))throw DomainError.invalid("Contribution occurrence is outside the pinned goal time or event scope");
  for(String bound:List.of("Start","End"))if(slots.get("period"+bound)!=null){var limit=instant(slots.get("period"+bound));int cmp=at.compareTo(limit);if((bound.equals("Start")?cmp<0:cmp>0)||cmp==0&&Boolean.FALSE.equals(slots.get("period"+bound+"Inclusive")))throw DomainError.invalid("Contribution occurrence is outside the pinned goal period");}
  var current=policies.current(c.organizationId(),"EVIDENCE",c.asOf()).stream().filter(p->Objects.equals(slots.get("evidencePolicyVersion"),p.get("version"))&&p.get("createdAt")!=null&&!instant(p.get("createdAt")).isAfter(c.knownAt())).toList();
  if(current.size()!=1)throw new DomainError("HELD","POLICY_UNRESOLVED","Contribution requires the pinned current evidence policy");
  return property;
 }
 private Object actualLot(DomainContext c,Map<String,Object> actual){
  if("LOT".equals(actual.get("subjectKind")))return actual.get("subjectId");
  var receipts=db.run(com.sap.cds.ql.Select.from("mulino.trade.receipt.Receipts").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("canonicalOccurrenceId").eq(actual.get("ID"))).and(r.get("recordedAt").le(c.knownAt())))).listOf(Map.class);
  if(receipts.size()==1)return receipts.getFirst().get("lotId");
  var segments=db.run(com.sap.cds.ql.Select.from("mulino.inventory.QuantitySegments").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(actual.get("physicalScopeId"))).and(r.get("recordedAt").le(c.knownAt())))).listOf(Map.class);
  return segments.size()==1?segments.getFirst().get("lotId"):null;
 }
 private Map<String,Object> decode(Object value){try{return json.readValue(value.toString(),Map.class);}catch(Exception e){throw DomainError.invalid("Invalid pinned goal slots");}}
 private static Instant instant(Object value){return value instanceof Instant i?i:OffsetDateTime.parse(value.toString()).toInstant();}
}
