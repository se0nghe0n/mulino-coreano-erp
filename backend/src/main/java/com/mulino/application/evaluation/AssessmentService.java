package com.mulino.application.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mulino.application.core.*;
import com.mulino.application.work.*;
import com.mulino.domain.definitions.*;
import com.mulino.domain.evaluation.*;
import com.mulino.domain.governance.PolicyRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentService implements CommandHandler,WorkAssessmentGuard {
  private final WorkAccess work;private final AssessmentRepository repository;private final DefinitionRepository definitions;
  private final ExecutionClock clock;private final DefinitionValidator validator;private final AssessmentFactProvider provider;private final PolicyRepository policies;
  private final TypedPredicateEvaluator evaluator=new TypedPredicateEvaluator();
  private final ObjectMapper json=new ObjectMapper().findAndRegisterModules().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true);
  public AssessmentService(WorkAccess work,AssessmentRepository repository,DefinitionRepository definitions,DefinitionValidator validator,AssessmentFactProvider provider,PolicyRepository policies,ExecutionClock clock){this.clock=clock;this.work=work;this.repository=repository;this.definitions=definitions;this.validator=validator;this.provider=provider;this.policies=policies;}
  public Set<String> capabilities(){return Set.of("assessGoal");}
  public CommandPreparation prepare(DomainContext c,Map<String,Object> intent){String id=id(parameters(intent).get("workId"));var w=work.require(c,id,false);return CommandPreparation.ordinary(Map.of("WORK",List.of(id),"ITEM",List.of(w.get("itemId").toString())),List.of("work:"+id),"ASSESSMENT",id,((Number)w.get("revision")).intValue());}
  @Transactional
  public Map<String,Object> execute(DomainContext c,Map<String,Object> intent){var p=parameters(intent);if(!Set.of("workId").equals(p.keySet()))throw DomainError.invalid("Only workId selects a current pinned goal");return assess(c,id(p.get("workId")));}
  @Transactional
  public Map<String,Object> assess(DomainContext c,String workId){
    var w=work.require(c,workId,true);var goal=work.currentGoal(c,workId);var calculation=calculate(c,w,goal);String assessmentId=UUID.randomUUID().toString();
    var previous=latest(c,workId,null);Instant now=Instant.now();
    var a=new LinkedHashMap<String,Object>();a.put("organizationId",c.organizationId());a.put("ID",assessmentId);a.put("revision",0);a.put("createdAt",now);a.put("recordedAt",now);a.put("effectiveAt",c.asOf());a.put("workId",workId);a.put("goalId",goal.get("ID"));a.put("outcome",calculation.result.truth().state().name());a.put("evaluatorVersion",Objects.toString(decode(goal.get("slotsJson")).get("evaluatorVersion"),"UNAVAILABLE"));a.put("assessedAt",now);a.put("conditionsJson",encode(calculation.result.conditions()));a.put("definitionVersionId",goal.get("definitionVersionId"));a.put("policyVersionId",calculation.policy);a.put("knownAt",c.knownAt());a.put("asOf",c.asOf());a.put("previousAssessmentId",previous==null?null:previous.get("ID"));a.put("inputSnapshotHash",calculation.hash);a.put("conflict",calculation.result.truth().conflict());a.put("held",calculation.held);
    boolean violated=previous!=null&&Boolean.TRUE.equals(previous.get("deadlineViolated"))||deadline(c,w,goal,calculation);a.put("deadlineViolated",violated);
    var s=new LinkedHashMap<String,Object>();s.put("organizationId",c.organizationId());s.put("ID",UUID.randomUUID().toString());s.put("assessmentId",assessmentId);s.put("workId",workId);s.put("goalId",goal.get("ID"));s.put("recordedAt",now);s.put("asOf",c.asOf());s.put("knownAt",c.knownAt());s.put("contentHash",calculation.hash);s.put("contentJson",calculation.snapshot);
    repository.save(a,s);work.markInvalidation(c,workId,calculation.held);
    return Map.of("outcome",calculation.held?"HELD":"APPLIED","effects",Map.of("assessmentId",assessmentId,"workId",workId),"assessment",TransportValues.normalize(a),"quantityEffects",List.of());
  }
  public Map<String,Object> assessCreated(DomainContext c,String workId){return assess(c,workId);}
  public void requireSupportedGoal(DomainContext c,Map<String,Object> goal){definition(goal,c);}
  public void requireFulfilled(DomainContext c,Map<String,Object> w,Map<String,Object> goal){
    var latest=latest(c,w.get("ID").toString(),goal.get("ID").toString());var current=calculate(c,w,goal);
    if(!policies.current(c.organizationId(),"EVIDENCE",clock.instant()).stream().anyMatch(p->Objects.equals(p.get("ID"),current.policy)))throw new DomainError("HELD","POLICY_UNRESOLVED","Current evidence policy required for closure");
    if(latest==null||latest.get("inputSnapshotHash")==null||Boolean.TRUE.equals(latest.get("held"))||current.held||!"SATISFIED".equals(latest.get("outcome"))||current.result.truth().state()!=PredicateTruth.State.SATISFIED||current.result.truth().conflict()||!Objects.equals(latest.get("inputSnapshotHash"),current.hash))throw new DomainError("HELD","ASSESSMENT_NOT_CURRENT","Current immutable goal assessment and inputs required");
  }
  public void requireResume(DomainContext c,Map<String,Object> w,Map<String,Object> wait,Map<String,Object> evidence){
    var goal=work.currentGoal(c,w.get("ID").toString());Definition d=definition(goal,c);var predicate=map(wait.get("resumePredicate"));if(!"VALID".equals(validator.validatePredicate(d,predicate).outcome()))throw DomainError.invalid("Unsupported resume predicate");
    var result=evaluator.evaluate(predicate,provider.load(c,w,decode(goal.get("slotsJson")),d,goal.get("ID").toString()),c.asOf(),c.knownAt());
    if(result.truth().state()!=PredicateTruth.State.SATISFIED||result.truth().conflict())throw new DomainError("HELD","WAIT_UNVERIFIED","Verified resume condition required");
  }
  public void goalChanged(DomainContext c,String workId,String previousGoalId,String newGoalId){/* WorkLifecycle records pending flag with the new goal in its own atomic transition. */}
  /** Evidence correction is pending in the same transaction; past assessment rows are never rewritten. */
  public void invalidate(DomainContext c,String workId){work.require(c,workId,true);work.markInvalidation(c,workId,true);}
  private record Calculation(TypedPredicateEvaluator.Result result,String hash,String snapshot,String policy,boolean held){}
  private Calculation calculate(DomainContext c,Map<String,Object> w,Map<String,Object> goal){
    var slots=decode(goal.get("slotsJson"));Definition d;
    try{d=definition(goal,c);}catch(DomainError|NoSuchElementException e){return held(goal,slots,"PINNED_CONTRACT_UNAVAILABLE");}
    var policy=policies.current(c.organizationId(),"EVIDENCE",c.asOf()).stream().filter(p->p.get("createdAt")!=null&&!instant(p.get("createdAt")).isAfter(c.knownAt())&&Objects.equals(slots.get("evidencePolicyVersion"),p.get("version"))).toList();
    if(policy.size()!=1)return held(goal,slots,"EVIDENCE_POLICY_UNRESOLVED");
    var facts=provider.load(c,w,slots,d,goal.get("ID").toString());Map<String,Object> predicate=goalPredicate(d,slots);
    TypedPredicateEvaluator.Result result;
    String mode=slots.get("quantityMode").toString();
    if(mode.equals("EXISTS_IN")||mode.equals("THROUGHOUT"))result=evaluator.evaluateInterval(predicate,facts,instant(slots.get("periodStart")),instant(slots.get("periodEnd")),c.knownAt(),mode.equals("THROUGHOUT"));
    else result=evaluator.evaluate(predicate,facts,mode.equals("STATE_AT")?instant(slots.get("evaluationAt")):c.asOf(),c.knownAt());
    var snapshot=new LinkedHashMap<String,Object>();snapshot.put("goalId",goal.get("ID"));snapshot.put("definitionId",d.id());snapshot.put("definitionHash",d.contentHash());snapshot.put("goalSlots",slots);snapshot.put("policy",policy.getFirst());snapshot.put("facts",facts);snapshot.put("conditions",result.conditions());String payload=encode(snapshot);
    return new Calculation(result,DefinitionRepository.sha256(payload),payload,policy.getFirst().get("ID").toString(),false);
  }
  private Definition definition(Map<String,Object> goal,DomainContext c){var slots=decode(goal.get("slotsJson"));if(!TypedPredicateEvaluator.VERSION.equals(slots.get("evaluatorVersion")))throw new DomainError("HELD","VERSION_UNSUPPORTED","Pinned evaluator unsupported");Definition d=definitions.get(c.organizationId(),goal.get("definitionVersionId").toString());var template=template(d,slots);validateConditions(slots,template);if(!TypedPredicateEvaluator.VERSION.equals(d.evaluatorVersion())||!"1.0.0".equals(d.schemaVersion())||!"VALID".equals(validator.validate(d).outcome())||!TypedPredicateEvaluator.VERSION.equals(template.evaluatorVersion())||!"VALID".equals(validator.validatePredicate(d,template.predicate()).outcome()))throw new DomainError("HELD","VERSION_UNSUPPORTED","Pinned predicate unsupported");return d;}
  private void validateConditions(Map<String,Object> slots,Definition.Goal template){
    if(!(slots.get("conditions") instanceof List<?> selected)||selected.size()!=1)throw new DomainError("HELD","VERSION_UNSUPPORTED","Complete pinned goal conditions required");
    Object condition=selected.getFirst();
    boolean valid=condition instanceof String text&&template.name().equals(text)||condition instanceof Map<?,?> m&&m.keySet().equals(Set.of("id"))&&template.name().equals(m.get("id"));
    if(!valid)throw new DomainError("HELD","VERSION_UNSUPPORTED","Condition selector not published in pinned definition");
  }
  private Map<String,Object> goalPredicate(Definition d,Map<String,Object> slots){
    var original=template(d,slots).predicate();
    if(slots.get("targetQuantity")==null)return original;
    var quantity=d.attributes().stream().filter(a->a.name().equals("quantity")&&a.type()==Definition.ValueType.DECIMAL&&Objects.equals(a.unit(),slots.get("unit"))).toList();
    if(quantity.size()!=1)throw new DomainError("HELD","VERSION_UNSUPPORTED","Unique typed quantity property required");
    String mode=slots.get("quantityMode").toString();
    if(!Set.of("CUMULATIVE_EVENT","STATE_AT").contains(mode))return original;
    var target=Map.<String,Object>of("operator",mode.equals("CUMULATIVE_EVENT")?"quantitySum":"stateQuantity","property",quantity.getFirst().nounType()+".quantity","minimum",Map.of("value",slots.get("targetQuantity"),"unit",slots.get("unit")),"unit",slots.get("unit"),"evidenceSelector",mode.equals("CUMULATIVE_EVENT")?"VERIFIED_DISTINCT":"CURRENT_STATE");
    return Map.of("operator","all","children",List.of(original,target));
  }
  private Definition.Goal template(Definition d,Map<String,Object> slots){var candidates=d.goals().stream().filter(g->Objects.equals(g.endpoint(),slots.get("endpoint"))&&Objects.equals(g.quantityMode(),slots.get("quantityMode"))).toList();if(candidates.size()!=1)throw new DomainError("HELD","VERSION_UNSUPPORTED","Unique pinned goal template required");return candidates.getFirst();}
  private Calculation held(Map<String,Object> goal,Map<String,Object> slots,String reason){String payload=encode(Map.of("goalId",goal.get("ID"),"slots",slots,"unknown",reason));return new Calculation(new TypedPredicateEvaluator.Result(new PredicateTruth(PredicateTruth.State.UNVERIFIED,false),List.of()),DefinitionRepository.sha256(payload),payload,null,true);}
  private boolean deadline(DomainContext c,Map<String,Object> w,Map<String,Object> goal,Calculation current){var slots=decode(goal.get("slotsJson"));if(slots.get("dueAt")==null||c.asOf().isBefore(instant(slots.get("dueAt")))||current.held)return false;var deadlineContext=new DomainContext(c.organizationId(),c.actorId(),c.stableRequestOwner(),instant(slots.get("dueAt")),c.knownAt());var atDeadline=calculate(deadlineContext,w,goal);return !atDeadline.held&&atDeadline.result.truth().state()==PredicateTruth.State.UNSATISFIED;}
  private Map<String,Object> latest(DomainContext c,String workId,String goalId){return repository.rows(c,"mulino.work.read.AssessmentReferences").stream().filter(a->workId.equals(a.get("workId"))&&(goalId==null||goalId.equals(a.get("goalId")))).max(Comparator.comparing(a->instant(a.get("assessedAt")))).orElse(null);}
  private Object normalize(Object o){
    if(o==null)return null;
    if(o.getClass().isRecord()){var out=new TreeMap<String,Object>();for(var field:o.getClass().getRecordComponents())try{out.put(field.getName(),normalize(field.getAccessor().invoke(o)));}catch(ReflectiveOperationException e){throw DomainError.invalid("Assessment snapshot type unavailable");}return out;}
    if(o instanceof Map<?,?> m){var out=new TreeMap<String,Object>();m.forEach((k,v)->out.put(k.toString(),normalize(v)));return out;}
    if(o instanceof Collection<?> values)return values.stream().map(this::normalize).toList();
    return TransportValues.normalize(o);
  }
  private String encode(Object o){try{return json.writeValueAsString(normalize(o));}catch(Exception e){throw DomainError.invalid("Assessment serialization failed");}}
  private Map<String,Object> decode(Object o){try{return json.readValue(o.toString(),Map.class);}catch(Exception e){throw new DomainError("HELD","VERSION_UNSUPPORTED","Goal snapshot unavailable");}}
  private Map<String,Object> parameters(Map<String,Object> intent){var r=new LinkedHashMap<String,Object>();map(intent.get("slots")).forEach((k,v)->r.put(k,v instanceof Map<?,?> m&&m.containsKey("type")?m.get("value"):v));return r;}
  @SuppressWarnings("unchecked") private Map<String,Object> map(Object o){if(!(o instanceof Map<?,?>))throw DomainError.invalid("Typed object required");return (Map<String,Object>)o;}
  private String id(Object v){try{return UUID.fromString(v.toString()).toString();}catch(Exception e){throw DomainError.invalid("UUID workId required");}}
  private static Instant instant(Object o){return o instanceof Instant i?i:OffsetDateTime.parse(o.toString()).toInstant();}
}
