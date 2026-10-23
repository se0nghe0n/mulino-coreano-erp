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
public class AssessmentService implements CommandHandler,WorkAssessmentGuard,GoalShortfallProof {
  private final WorkAccess work;private final AssessmentRepository repository;private final DefinitionRepository definitions;
  private final ExecutionClock clock;private final DefinitionValidator validator;private final AssessmentFactProvider provider;private final PolicyRepository policies;
  @org.springframework.beans.factory.annotation.Autowired private ReadAuthorizer authority;
  @org.springframework.beans.factory.annotation.Autowired private EvidenceCaptureRepository captures;
  private final TypedPredicateEvaluator evaluator=new TypedPredicateEvaluator();
  private final ObjectMapper json=new ObjectMapper().findAndRegisterModules().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true);
  public AssessmentService(WorkAccess work,AssessmentRepository repository,DefinitionRepository definitions,DefinitionValidator validator,AssessmentFactProvider provider,PolicyRepository policies,ExecutionClock clock){this.clock=clock;this.work=work;this.repository=repository;this.definitions=definitions;this.validator=validator;this.provider=provider;this.policies=policies;}
  public Set<String> capabilities(){return Set.of("assessGoal");}
  public CommandPreparation prepare(DomainContext c,Map<String,Object> intent){String id=id(parameters(intent).get("workId"));var w=work.require(c,id,false);return CommandPreparation.ordinary(Map.of("WORK",List.of(id),"ITEM",List.of(w.get("itemId").toString())),List.of("work:"+id),"ASSESSMENT",id,((Number)w.get("revision")).intValue());}
  public List<SubjectBinding> subjectBindings(DomainContext c,Map<String,Object> intent,CommandPreparation preparation){
    var slots=parameters(intent);if(!Set.of("workId").equals(slots.keySet()))throw DomainError.invalid("Only workId selects a current pinned goal");
    String workId=id(slots.get("workId"));if(!Objects.equals(workId,preparation.targetId()))throw DomainError.invalid("Assessment target changed");
    var target=work.require(c,workId,false);var goal=work.currentGoal(c,workId);
    return List.of(SubjectBinding.optional("Work",Set.of(target.get("ID").toString())),SubjectBinding.optional("Goal",Set.of(goal.get("ID").toString())));
  }
  @Transactional
  public Map<String,Object> execute(DomainContext c,Map<String,Object> intent){var p=parameters(intent);if(!Set.of("workId").equals(p.keySet()))throw DomainError.invalid("Only workId selects a current pinned goal");return assess(c,id(p.get("workId")));}
  @Transactional
  public Map<String,Object> assess(DomainContext c,String workId){
    var w=work.require(c,workId,true);var goal=work.currentGoal(c,workId);var calculation=calculate(c,w,goal);String assessmentId=UUID.randomUUID().toString();
    var previous=latest(c,workId,null);Instant now=clock.instant();
    var a=new LinkedHashMap<String,Object>();a.put("organizationId",c.organizationId());a.put("ID",assessmentId);a.put("revision",0);a.put("createdAt",now);a.put("recordedAt",now);a.put("effectiveAt",c.asOf());a.put("workId",workId);a.put("goalId",goal.get("ID"));a.put("outcome",calculation.result.truth().state().name());a.put("evaluatorVersion",Objects.toString(decode(goal.get("slotsJson")).get("evaluatorVersion"),"UNAVAILABLE"));a.put("assessedAt",now);a.put("conditionsJson",encode(calculation.result.conditions()));a.put("definitionVersionId",goal.get("definitionVersionId"));a.put("policyVersionId",calculation.policy);a.put("knownAt",c.knownAt());a.put("asOf",c.asOf());a.put("previousAssessmentId",previous==null?null:previous.get("ID"));a.put("inputSnapshotHash",calculation.hash);a.put("conflict",calculation.result.truth().conflict());a.put("held",calculation.held);
    boolean violated=previous!=null&&Boolean.TRUE.equals(previous.get("deadlineViolated"))||deadline(c,w,goal,calculation);a.put("deadlineViolated",violated);
    var s=new LinkedHashMap<String,Object>();s.put("organizationId",c.organizationId());s.put("ID",UUID.randomUUID().toString());s.put("assessmentId",assessmentId);s.put("workId",workId);s.put("goalId",goal.get("ID"));s.put("recordedAt",now);s.put("asOf",c.asOf());s.put("knownAt",c.knownAt());s.put("contentHash",calculation.hash);s.put("contentJson",calculation.snapshot);
    if(calculation.capturePayload!=null){
      var boundary=map(decode(calculation.capturePayload).get("boundary"));
      String capture=captures.acquire(c.organizationId(),encode(boundary.get("scope")),c.asOf(),c.knownAt(),c.asOf(),"ASSESS_GOAL",calculation.capturePayload,now);
      s.put("evidenceCaptureId",capture);
    }
    repository.save(a,s);work.markInvalidation(c,workId,calculation.held);
    return Map.of("outcome",calculation.held?"HELD":"APPLIED","effects",Map.of("assessmentId",assessmentId,"workId",workId),"assessment",TransportValues.normalize(a),"quantityEffects",List.of());
  }
  public Map<String,Object> assessCreated(DomainContext c,String workId){return assess(c,workId);}
  public void requireSupportedGoal(DomainContext c,Map<String,Object> goal){definition(goal,c);}
  public void requireFulfilled(DomainContext c,Map<String,Object> w,Map<String,Object> goal){
    var latest=latest(c,w.get("ID").toString(),goal.get("ID").toString());var current=calculate(c,w,goal,c.asOf(),CanonicalFactAdmission.Purpose.CLOSE_WORK);
    if(!policies.current(c.organizationId(),"EVIDENCE",clock.instant()).stream().anyMatch(p->Objects.equals(p.get("ID"),current.policy)))throw new DomainError("HELD","POLICY_UNRESOLVED","Current evidence policy required for closure");
    if(latest==null||latest.get("inputSnapshotHash")==null||Boolean.TRUE.equals(latest.get("held"))||current.held||!"SATISFIED".equals(latest.get("outcome"))||current.result.truth().state()!=PredicateTruth.State.SATISFIED||current.result.truth().conflict()||!Objects.equals(latest.get("inputSnapshotHash"),current.hash))throw new DomainError("HELD","ASSESSMENT_NOT_CURRENT","Current immutable goal assessment and inputs required");
  }
  /** Scope selection uses immutable snapshot lineage; execution revalidates every admitted source. */
  public Map<String,List<String>> scopes(DomainContext c,String workId){
    var w=work.require(c,workId,false);var goal=work.currentGoal(c,workId);
    var selected=latest(c,workId,goal.get("ID").toString());
    var scopes=new LinkedHashMap<String,List<String>>();scopes.put("TARGET",List.of(workId));scopes.put("ITEM",List.of(w.get("itemId").toString()));
    var works=new TreeSet<String>();works.add(workId);var places=new TreeSet<String>();
    if(selected!=null){var snapshot=proofSnapshot(c,selected);var facts=map(snapshot.get("facts"));var sources=map(facts.get("sources"));
      if(sources.get("evidence.CanonicalOccurrences") instanceof List<?> rows)for(Object row:rows){var actual=map(row);if(actual.get("workId")!=null)works.add(actual.get("workId").toString());if(actual.get("placeId")!=null)places.add(actual.get("placeId").toString());}
    }
    scopes.put("WORK",List.copyOf(works));if(!places.isEmpty())scopes.put("PLACE",List.copyOf(places));return scopes;
  }
  public GoalShortfallProof.Proof require(DomainContext c,String workId,Map<String,Object> command){
    var w=work.require(c,workId,true);var goal=work.currentGoal(c,workId);var latest=latest(c,workId,goal.get("ID").toString());
    if(latest==null||Boolean.TRUE.equals(w.get("pendingInvalidation"))||Boolean.TRUE.equals(latest.get("held"))||Boolean.TRUE.equals(latest.get("conflict"))||!"UNSATISFIED".equals(latest.get("outcome")))throw shortfallUnavailable();
    if(!"COMMITTED".equals(command.get("state"))||!"assessGoal".equals(command.get("capabilityId"))||!c.organizationId().equals(command.get("organizationId"))||command.get("completedAt")==null||instant(command.get("completedAt")).isAfter(c.knownAt()))throw shortfallUnavailable();
    var result=decode(command.get("resultJson"));var recorded=map(result.get("assessment"));var effects=map(result.get("effects"));
    if(!"APPLIED".equals(result.get("outcome"))||!Objects.equals(effects.get("assessmentId"),latest.get("ID"))||!workId.equals(effects.get("workId")))throw shortfallUnavailable();
    for(String key:List.of("ID","workId","goalId","inputSnapshotHash","outcome","definitionVersionId","policyVersionId","conditionsJson"))if(!Objects.equals(recorded.get(key),latest.get(key)))throw shortfallUnavailable();
    var original=decode(command.get("canonicalIntentJson"));
    if(!"assessGoal".equals(original.get("capabilityId"))||!workId.equals(map(original.get("slots")).get("workId"))||!Objects.equals(CommandRequests.hash(original),command.get("canonicalHash")))throw shortfallUnavailable();
    var snapshot=proofSnapshot(c,latest);var slots=decode(goal.get("slotsJson"));
    if(!"CUMULATIVE_EVENT".equals(slots.get("quantityMode"))||slots.get("targetQuantity")==null||slots.get("unit")==null||slots.get("periodStart")==null||slots.get("periodEnd")==null||!Objects.equals(goal.get("ID"),snapshot.get("goalId")))throw shortfallUnavailable();
    var current=calculate(c,w,goal,c.asOf(),CanonicalFactAdmission.Purpose.OPEN_OBLIGATION);
    if(current.held||current.result.truth().state()!=PredicateTruth.State.UNSATISFIED||current.result.truth().conflict()||!Objects.equals(current.hash,latest.get("inputSnapshotHash"))||!policies.current(c.organizationId(),"EVIDENCE",clock.instant()).stream().anyMatch(p->Objects.equals(p.get("ID"),current.policy)))throw shortfallUnavailable();
    // IdentityAuthorization's WORK list is any-of. Check each endpoint separately.
    var sourceFacts=map(map(snapshot.get("facts")).get("sources"));
    if(!(sourceFacts.get("evidence.CanonicalOccurrences") instanceof List<?> actuals)||actuals.isEmpty())throw shortfallUnavailable();
    for(Object value:actuals){var actual=map(value);if(actual.get("workId")==null||actual.get("placeId")==null||!Objects.equals(actual.get("itemId"),w.get("itemId")))throw shortfallUnavailable();
      for(String endpoint:new LinkedHashSet<>(List.of(workId,actual.get("workId").toString())))authority.authorizeScopes(c,"openObligation",Map.of("TARGET",List.of(workId),"WORK",List.of(endpoint),"ITEM",List.of(w.get("itemId").toString()),"PLACE",List.of(actual.get("placeId").toString())));
    }
    Definition d=definition(goal,c);var predicate=goalPredicate(d,slots);var leaves=new LinkedHashMap<String,Map<String,Object>>();quantityLeaves(predicate,"root",leaves);
    String unit=slots.get("unit").toString(),property=GoalQuantityProperty.resolve(d,template(d,slots).predicate(),unit);
    var target=new java.math.BigDecimal(slots.get("targetQuantity").toString());if(target.signum()<=0)throw shortfallUnavailable();
    TypedPredicateEvaluator.Condition chosen=null;int targetLeaves=0;
    for(var entry:leaves.entrySet()){
      var condition=current.result.conditions().stream().filter(x->x.conditionId().equals(entry.getKey())).findFirst().orElseThrow(AssessmentService::shortfallUnavailable);
      var node=entry.getValue();
      if("quantitySum".equals(node.get("operator"))){
        if(!property.equals(node.get("property"))||!unit.equals(node.get("unit"))||node.get("minimum")==null||node.get("maximum")!=null)throw shortfallUnavailable();
        Object bound=node.get("minimum") instanceof Map<?,?> m?m.get("value"):node.get("minimum");
        var minimum=new java.math.BigDecimal(bound.toString());if(minimum.compareTo(target)>0)throw shortfallUnavailable();
        if(minimum.compareTo(target)==0){chosen=condition;targetLeaves++;}
      }else if(condition.truth().state()!=PredicateTruth.State.SATISFIED||condition.truth().conflict())throw shortfallUnavailable();
    }
    if(targetLeaves!=1||chosen==null)throw shortfallUnavailable();
    var amount=TypedPredicateEvaluator.quantity(chosen.inputs(),unit,true);
    if(amount.uncertain()||amount.conflict()||amount.total().signum()<0||amount.total().compareTo(target)>=0)throw shortfallUnavailable();
    var scope=new LinkedHashMap<String,Object>(map(slots.get("scope")));scope.put("workId",workId);scope.put("goalVersionId",goal.get("ID"));scope.put("conditionId",property);
    var proof=new LinkedHashMap<String,Object>();proof.put("assessmentId",latest.get("ID"));proof.put("inputSnapshotHash",current.hash);proof.put("definitionVersionId",goal.get("definitionVersionId"));proof.put("definitionHash",snapshot.get("definitionHash"));proof.put("policyVersionId",current.policy);proof.put("policy",snapshot.get("policy"));proof.put("goalSlots",slots);proof.put("targetQuantity",target);proof.put("recognizedQuantity",amount.total());proof.put("shortfallQuantity",target.subtract(amount.total()));proof.put("unit",unit);proof.put("conditionId",property);proof.put("inputs",normalize(chosen.inputs()));proof.put("asOf",c.asOf());proof.put("knownAt",c.knownAt());
    return new GoalShortfallProof.Proof(workId,goal.get("ID").toString(),property,target,amount.total(),target.subtract(amount.total()),unit,Map.copyOf(scope),Map.copyOf(proof));
  }
  private Map<String,Object> proofSnapshot(DomainContext c,Map<String,Object> assessment){
    var snapshots=repository.rows(c,"mulino.evaluation.InputSnapshots").stream().filter(x->Objects.equals(assessment.get("ID"),x.get("assessmentId"))).toList();
    if(snapshots.size()!=1)throw shortfallUnavailable();var row=snapshots.getFirst();
    if(!Objects.equals(row.get("contentHash"),assessment.get("inputSnapshotHash"))||!Objects.equals(DefinitionRepository.sha256(row.get("contentJson").toString()),row.get("contentHash")))throw shortfallUnavailable();return decode(row.get("contentJson"));
  }
  private static void quantityLeaves(Map<String,Object> node,String path,Map<String,Map<String,Object>> leaves){
    if("all".equals(node.get("operator"))){if(!(node.get("children") instanceof List<?> children))throw shortfallUnavailable();for(int i=0;i<children.size();i++)quantityLeaves((Map<String,Object>)children.get(i),path+"."+i,leaves);}
    else if(Set.of("any","not","stateQuantity").contains(node.get("operator")))throw shortfallUnavailable();else leaves.put(path,node);
  }
  private static DomainError shortfallUnavailable(){return new DomainError("HELD","ASSESSMENT_NOT_CURRENT","A current unambiguous immutable quantity shortfall proof is required");}
  public void requireResume(DomainContext c,Map<String,Object> w,Map<String,Object> wait,Map<String,Object> evidence){
    var goal=work.currentGoal(c,w.get("ID").toString());Definition d=definition(goal,c);var predicate=map(wait.get("resumePredicate"));if(!"VALID".equals(validator.validatePredicate(d,predicate).outcome()))throw DomainError.invalid("Unsupported resume predicate");
    var result=evaluator.evaluate(predicate,provider.load(c,w,decode(goal.get("slotsJson")),d,goal.get("ID").toString()),c.asOf(),c.knownAt());
    if(result.truth().state()!=PredicateTruth.State.SATISFIED||result.truth().conflict())throw new DomainError("HELD","WAIT_UNVERIFIED","Verified resume condition required");
  }
  public void goalChanged(DomainContext c,String workId,String previousGoalId,String newGoalId){/* WorkLifecycle records pending flag with the new goal in its own atomic transition. */}
  /** Evidence correction is pending in the same transaction; past assessment rows are never rewritten. */
  public void invalidate(DomainContext c,String workId){work.require(c,workId,true);work.markInvalidation(c,workId,true);}
  private record Calculation(TypedPredicateEvaluator.Result result,String hash,String snapshot,String policy,boolean held,String capturePayload){}
  private Calculation calculate(DomainContext c,Map<String,Object> w,Map<String,Object> goal){
    return calculate(c,w,goal,c.asOf());
  }
  private Calculation calculate(DomainContext c,Map<String,Object> w,Map<String,Object> goal,Instant evaluationHorizon){
    return calculate(c,w,goal,evaluationHorizon,CanonicalFactAdmission.Purpose.ASSESS_GOAL);
  }
  private Calculation calculate(DomainContext c,Map<String,Object> w,Map<String,Object> goal,Instant evaluationHorizon,CanonicalFactAdmission.Purpose purpose){
    var slots=decode(goal.get("slotsJson"));
    if(!(slots.get("scope") instanceof Map<?,?> scope)||!Set.of("itemId","lotId","placeId","workId").containsAll(scope.keySet()))throw DomainError.invalid("Typed bounded goal scope required");
    if(scope.get("itemId")!=null&&!Objects.equals(scope.get("itemId"),w.get("itemId"))||scope.get("workId")!=null&&!Objects.equals(scope.get("workId"),w.get("ID")))throw DomainError.forbidden();
    Definition d;
    try{d=definition(goal,c);}catch(DomainError|NoSuchElementException|IllegalStateException e){return held(goal,slots,"PINNED_CONTRACT_UNAVAILABLE");}
    var policy=policies.current(c.organizationId(),"EVIDENCE",c.asOf()).stream().filter(p->p.get("createdAt")!=null&&!instant(p.get("createdAt")).isAfter(c.knownAt())&&Objects.equals(slots.get("evidencePolicyVersion"),p.get("version"))).toList();
    if(policy.size()!=1)return held(goal,slots,"EVIDENCE_POLICY_UNRESOLVED");
    var facts=provider.load(c,w,slots,d,goal.get("ID").toString(),evaluationHorizon,purpose);Map<String,Object> predicate=goalPredicate(d,slots);
    TypedPredicateEvaluator.Result result;
    String mode=slots.get("quantityMode").toString();
    if(mode.equals("EXISTS_IN")||mode.equals("THROUGHOUT"))result=evaluator.evaluateInterval(predicate,facts,instant(slots.get("periodStart")),instant(slots.get("periodEnd")),c.knownAt(),mode.equals("THROUGHOUT"),!Boolean.FALSE.equals(slots.get("periodStartInclusive")),!Boolean.FALSE.equals(slots.get("periodEndInclusive")));
    else result=evaluator.evaluate(predicate,facts,mode.equals("STATE_AT")?instant(slots.get("evaluationAt")):c.asOf(),c.knownAt());
    var rawResult=result;
    var publicConditions=AssessmentPresentation.authored(template(d,slots).name(),rawResult);
    var quantityEvaluation=AssessmentPresentation.quantity(predicate,rawResult,mode);
    if(usesStock(template(d,slots).predicate())){
      String name=template(d,slots).name();
      result=new TypedPredicateEvaluator.Result(result.truth(),result.conditions().stream().map(condition->new TypedPredicateEvaluator.Condition(
        condition.conditionId().replaceFirst("^root",java.util.regex.Matcher.quoteReplacement(name)),condition.truth(),condition.inputs(),condition.evidenceRefs(),condition.evaluatorVersion())).toList());
    }
    var snapshot=new LinkedHashMap<String,Object>();snapshot.put("goalId",goal.get("ID"));snapshot.put("goal",goal);snapshot.put("definitionId",d.id());snapshot.put("definitionHash",d.contentHash());snapshot.put("goalSlots",slots);snapshot.put("policy",policy.getFirst());snapshot.put("facts",facts);snapshot.put("conditions",result.conditions());
    snapshot.put("publicConditions",publicConditions);quantityEvaluation.ifPresent(proof->snapshot.put("quantityEvaluation",proof));
    var captureScope=new TreeMap<String,Object>();captureScope.putAll((Map<String,Object>)scope);captureScope.put("itemId",w.get("itemId"));
    if(!captureScope.containsKey("lotId")&&w.get("lotId")!=null)captureScope.put("lotId",w.get("lotId"));
    if(!captureScope.containsKey("placeId")&&slots.get("placeId")!=null)captureScope.put("placeId",slots.get("placeId"));
    var boundary=new TreeMap<String,Object>();boundary.put("organizationId",c.organizationId());boundary.put("scope",captureScope);boundary.put("asOf",c.asOf());boundary.put("knownAt",c.knownAt());boundary.put("evaluationHorizon",evaluationHorizon);boundary.put("admissionPurpose","ASSESS_GOAL");boundary.put("admissionContract","canonical-admission-v1");boundary.put("definitionHash",d.contentHash());boundary.put("definitionId",d.id());boundary.put("evaluatorVersion",d.evaluatorVersion());boundary.put("policyHash",policy.getFirst().get("contentHash"));boundary.put("policyId",policy.getFirst().get("ID"));boundary.put("policyVersion",policy.getFirst().get("version"));
    for(String key:List.of("action","includeReserved","evaluationAt","eventKind","evidenceSource","periodStart","periodEnd","periodStartInclusive","periodEndInclusive"))if(slots.containsKey(key))boundary.put(key,slots.get(key));
    // Purpose is checked by the provider. Only ASSESS_GOAL creates persisted captures;
    // read-only guard calculations must keep their existing envelope hash semantics.
    snapshot.put("captureBoundary",boundary);String payload=encode(snapshot);
    return new Calculation(result,DefinitionRepository.sha256(payload),payload,policy.getFirst().get("ID").toString(),false,encode(Map.of("boundary",boundary,"facts",facts)));
  }
  private Definition definition(Map<String,Object> goal,DomainContext c){var slots=decode(goal.get("slotsJson"));if(!TypedPredicateEvaluator.VERSION.equals(slots.get("evaluatorVersion")))throw new DomainError("HELD","VERSION_UNSUPPORTED","Pinned evaluator unsupported");Definition d=definitions.get(c.organizationId(),goal.get("definitionVersionId").toString());var template=template(d,slots);validateConditions(slots,template);
    if(Set.of("EXISTS_IN","THROUGHOUT").contains(slots.get("quantityMode"))&&usesStock(template.predicate())){
      if(!"complete-initial-and-final-ledger".equals(slots.get("observationPolicy")))throw new DomainError("HELD","VERSION_UNSUPPORTED","Explicit supported stock observation policy required");
      if(!Set.of("PHYSICAL","PHYSICAL_HELD").contains(Objects.toString(slots.get("action")))||!Boolean.TRUE.equals(slots.get("includeReserved"))||slots.get("placeId")==null||slots.get("targetQuantity")==null||slots.get("unit")==null)throw new DomainError("NEEDS_INPUT","GOAL_SLOT_MISSING","Stock interval requires targetQuantity, unit, placeId, physical action and includeReserved=true");
    }
    if("STATE_AT".equals(slots.get("quantityMode"))||slots.containsKey("evidenceSource")){
      String operator=GoalQuantityProperty.operator(template.predicate(),GoalQuantityProperty.resolve(d,template.predicate(),Objects.toString(slots.get("unit"),null)),Objects.toString(slots.get("quantityMode")));
      if(slots.containsKey("evidenceSource")&&!"quantitySum".equals(operator))throw new DomainError("HELD","VERSION_UNSUPPORTED","Source identity requires canonical evidence");
      if("STATE_AT".equals(slots.get("quantityMode"))&&"quantitySum".equals(operator)&&(!"PHYSICAL".equals(slots.get("action"))||!Boolean.TRUE.equals(slots.get("includeReserved"))))throw new DomainError("HELD","VERSION_UNSUPPORTED","Canonical state evidence does not establish action eligibility or unreserved quantity");
    }
    if(!TypedPredicateEvaluator.VERSION.equals(d.evaluatorVersion())||!"1.0.0".equals(d.schemaVersion())||!"VALID".equals(validator.validate(d).outcome())||!TypedPredicateEvaluator.VERSION.equals(template.evaluatorVersion())||!"VALID".equals(validator.validatePredicate(d,template.predicate()).outcome()))throw new DomainError("HELD","VERSION_UNSUPPORTED","Pinned predicate unsupported");return d;}
  private boolean usesStock(Map<String,Object> node){
    if("stateQuantity".equals(node.get("operator")))return true;
    return node.get("children") instanceof List<?> children&&children.stream().anyMatch(child->child instanceof Map<?,?> m&&usesStock((Map<String,Object>)m));
  }
  private void validateConditions(Map<String,Object> slots,Definition.Goal template){
    if(!(slots.get("conditions") instanceof List<?> selected)||selected.size()!=1)throw new DomainError("HELD","VERSION_UNSUPPORTED","Complete pinned goal conditions required");
    Object condition=selected.getFirst();
    boolean valid=condition instanceof String text&&template.name().equals(text)||condition instanceof Map<?,?> m&&m.keySet().equals(Set.of("id"))&&template.name().equals(m.get("id"));
    if(!valid)throw new DomainError("HELD","VERSION_UNSUPPORTED","Condition selector not published in pinned definition");
  }
  private Map<String,Object> goalPredicate(Definition d,Map<String,Object> slots){
    var original=template(d,slots).predicate();
    if(slots.get("targetQuantity")==null)return original;
    String mode=slots.get("quantityMode").toString();
    var properties=Set.of("EXISTS_IN","THROUGHOUT").contains(mode)
      ?GoalQuantityProperty.resolveAll(d,original,Objects.toString(slots.get("unit"),null))
      :List.of(GoalQuantityProperty.resolve(d,original,Objects.toString(slots.get("unit"),null)));
    var children=new ArrayList<Map<String,Object>>();children.add(original);
    for(String quantityProperty:properties){
      String operator=GoalQuantityProperty.operator(original,quantityProperty,mode);
      children.add(Map.of("operator",operator,"property",quantityProperty,"minimum",Map.of("value",slots.get("targetQuantity"),"unit",slots.get("unit")),"unit",slots.get("unit"),"evidenceSelector",operator.equals("quantitySum")?"VERIFIED_DISTINCT":"CURRENT_STATE"));
    }
    return Map.of("operator","all","children",children);
  }

  private Definition.Goal template(Definition d,Map<String,Object> slots){var candidates=d.goals().stream().filter(g->Objects.equals(g.endpoint(),slots.get("endpoint"))&&Objects.equals(g.quantityMode(),slots.get("quantityMode"))).toList();if(candidates.size()!=1)throw new DomainError("HELD","VERSION_UNSUPPORTED","Unique pinned goal template required");return candidates.getFirst();}
  private Calculation held(Map<String,Object> goal,Map<String,Object> slots,String reason){String payload=encode(Map.of("goalId",goal.get("ID"),"goal",goal,"slots",slots,"unknown",reason));return new Calculation(new TypedPredicateEvaluator.Result(new PredicateTruth(PredicateTruth.State.UNVERIFIED,false),List.of()),DefinitionRepository.sha256(payload),payload,null,true,null);}
  private boolean deadline(DomainContext c,Map<String,Object> w,Map<String,Object> goal,Calculation current){var slots=decode(goal.get("slotsJson"));if(slots.get("dueAt")==null||c.asOf().isBefore(instant(slots.get("dueAt")))||current.held)return false;if(c.asOf().equals(instant(slots.get("dueAt")))&&Boolean.TRUE.equals(slots.get("deadlineInclusive")))return false;Instant boundary=instant(slots.get("dueAt"));if(Boolean.FALSE.equals(slots.get("deadlineInclusive")))boundary=boundary.minusNanos(1);var deadlineContext=new DomainContext(c.organizationId(),c.actorId(),c.stableRequestOwner(),boundary,c.knownAt());var atDeadline=calculate(deadlineContext,w,goal,c.asOf());return !atDeadline.held&&atDeadline.result.truth().state()==PredicateTruth.State.UNSATISFIED;}
  private Map<String,Object> latest(DomainContext c,String workId,String goalId){
    var all=repository.rows(c,"mulino.work.read.AssessmentReferences").stream().filter(a->workId.equals(a.get("workId"))).toList();
    var previousIds=new HashSet<Object>();all.forEach(a->{if(a.get("previousAssessmentId")!=null)previousIds.add(a.get("previousAssessmentId"));});
    var current=all.stream().filter(a->!previousIds.contains(a.get("ID"))&&a.get("inputSnapshotHash")!=null&&(goalId==null||goalId.equals(a.get("goalId")))).toList();
    if(current.size()>1)throw new DomainError("HELD","ASSESSMENT_CONFLICT","Immutable assessment chain has multiple current heads");
    return current.isEmpty()?null:current.getFirst();
  }
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
