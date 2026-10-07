package com.mulino.application.trade.purchase;
import com.mulino.application.core.*;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.definitions.*;
import com.mulino.domain.evaluation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Component;
/** Conditions select immutable published predicates; execution always evaluates current facts. */
@Component
public class PurchaseConditions {
 private final DefinitionRepository definitions;private final AssessmentFactProvider facts;private final DefinitionValidator validator;private final WorkAccess works;private final ObjectMapper json=new ObjectMapper();
 public PurchaseConditions(DefinitionRepository definitions,AssessmentFactProvider facts,DefinitionValidator validator,WorkAccess works){this.definitions=definitions;this.facts=facts;this.validator=validator;this.works=works;}
 public Map<String,Object> bind(DomainContext c,String workId,List<?> selectors){var w=works.require(c,workId,false);var d=definitions.get(c.organizationId(),w.get("definitionVersionId").toString());return Map.of("selectors",selectors,"definitionVersionId",d.id(),"definitionHash",d.contentHash(),"evaluatorVersion",TypedPredicateEvaluator.VERSION);}
 public Map<String,Object> evaluate(DomainContext c,Map<String,Object> revision,Map<String,Object> approval){try{var binding=json.readValue(approval.get("conditionsJson").toString(),Map.class);var w=works.require(c,revision.get("workId").toString(),false);String definition=binding.get("definitionVersionId").toString();var d=definitions.get(c.organizationId(),definition);if(!Objects.equals(w.get("definitionVersionId"),definition)||!d.contentHash().equals(binding.get("definitionHash"))||!TypedPredicateEvaluator.VERSION.equals(binding.get("evaluatorVersion"))||!TypedPredicateEvaluator.VERSION.equals(d.evaluatorVersion()))throw unsupported();var selectors=(List<?>)binding.get("selectors");if(selectors.isEmpty())throw unsupported();var results=new ArrayList<Map<String,Object>>();
 for(Object selector:selectors){String name=selector instanceof String s?s:selector instanceof Map<?,?> m&&m.keySet().equals(Set.of("id"))?Objects.toString(m.get("id")):null;if(name==null)throw unsupported();var template=d.goals().stream().filter(g->name.equals(g.name())).findFirst().orElseThrow(PurchaseConditions::unsupported);if(!TypedPredicateEvaluator.VERSION.equals(template.evaluatorVersion())||!"VALID".equals(validator.validatePredicate(d,template.predicate()).outcome()))throw unsupported();
 var slots=new LinkedHashMap<String,Object>();try{var goal=works.currentGoal(c,w.get("ID").toString());if(goal.get("slotsJson")!=null)slots.putAll(json.readValue(goal.get("slotsJson").toString(),Map.class));}catch(DomainError absent){/* A bounded state prerequisite may use published physical facts without a Work goal. */}
 slots.put("quantityMode",template.quantityMode());slots.put("endpoint",template.endpoint());slots.put("scope",Map.of("itemId",revision.get("itemId"),"placeId",revision.get("destinationId")));slots.put("action","PHYSICAL");slots.put("includeReserved",true);
 var loaded=facts.load(c,w,slots,d);var result=new TypedPredicateEvaluator().evaluate(template.predicate(),loaded,c.asOf(),c.knownAt());if(result.truth().state()!=PredicateTruth.State.SATISFIED||result.truth().conflict())throw new DomainError("HELD","APPROVAL_CONDITION_UNVERIFIED","Current authoritative approval condition is not satisfied");results.add(Map.of("conditionId",name,"predicate",template.predicate(),"conditions",result.conditions(),"factSnapshotHash",DefinitionRepository.sha256(json.writeValueAsString(loaded))));
 }
 return Map.of("definitionVersionId",definition,"definitionHash",d.contentHash(),"evaluatorVersion",TypedPredicateEvaluator.VERSION,"asOf",c.asOf().toString(),"knownAt",c.knownAt().toString(),"conditions",results);
 }catch(DomainError e){throw e;}catch(Exception e){throw unsupported();}}
 private static DomainError unsupported(){return new DomainError("HELD","APPROVAL_CONDITION_UNSUPPORTED","Conditional selector or pinned predicate is unsupported");}
}
