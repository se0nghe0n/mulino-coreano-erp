package com.mulino.application.quality;
import com.mulino.application.core.*;
import com.mulino.application.policy.PolicyCommandGuard;
import com.mulino.application.trade.TradeImpact;
import com.mulino.application.inventory.InventoryCommands;
import com.mulino.domain.inventory.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.mulino.application.inventory.InventoryCommands.*;
@Component
public class QualityCommands implements CommandHandler {
 private final InventoryRepository r;private final StockPrimitives stock;private final QualityPrimitives primitive;private final QualityEvidence evidence;private final PolicyCommandGuard policy;private final TradeImpact impact;private final QualityResponsibilities responsibilities;private final QualityEligibility eligibility;private final com.mulino.application.identity.IdentityAuthorization auth;private final com.mulino.domain.identity.IdentityRepository identities;
 public QualityCommands(InventoryRepository r,StockPrimitives stock,QualityPrimitives primitive,QualityEvidence evidence,PolicyCommandGuard policy,TradeImpact impact,QualityResponsibilities responsibilities,QualityEligibility eligibility,com.mulino.application.identity.IdentityAuthorization auth,com.mulino.domain.identity.IdentityRepository identities){this.r=r;this.stock=stock;this.primitive=primitive;this.evidence=evidence;this.policy=policy;this.impact=impact;this.responsibilities=responsibilities;this.eligibility=eligibility;this.auth=auth;this.identities=identities;}
 public Set<String>capabilities(){return Set.of("placeHold","releaseHold","recordDispositionBasis","revokeDispositionBasis");}
 public Set<String>intentKinds(){return Set.of("COMMAND","RECORD");}
 private boolean releasing(String cap){return Set.of("releaseHold","revokeDispositionBasis").contains(cap);}
 private String entity(String cap){return Set.of("placeHold","releaseHold").contains(cap)?"Restrictions":"DispositionBases";}
 private String targetKey(String cap){return cap.equals("releaseHold")?"restrictionId":"dispositionBasisId";}
 public CommandPreparation prepare(DomainContext c,Map<String,Object>intent){
  String cap=text(intent,"capabilityId",80);if(!capabilities().contains(cap))throw DomainError.unsupported();var s=slots(intent);boolean release=releasing(cap);
  Set<String>allowed=release?Set.of(targetKey(cap),"evidenceId","reason"):Set.of("segmentId","action","category","customerId","evidenceId","reason","validFrom","validUntil","startQuantity","quantity","unit","workId","nextCheckAt");
  if(!allowed.containsAll(s.keySet()))throw DomainError.invalid("Unsupported quality slot");
  if(!Objects.equals(intent.get("intentKind"),cap.equals("recordDispositionBasis")?"RECORD":"COMMAND"))throw DomainError.invalid("Quality intent kind mismatch");
  Map<String,Object>target=release?r.current(c,entity(cap),uuid(s,targetKey(cap))):r.current(c,"QuantitySegments",uuid(s,"segmentId"));String segmentId=release?(String)target.get("segmentId"):(String)target.get("ID");if(segmentId==null)throw DomainError.invalid("Legacy restriction requires identified physical scope");var segment=r.current(c,"QuantitySegments",segmentId);
  text(s,"reason",240);var originalExpected=new LinkedHashMap<String,Object>();originalExpected.put("segmentId",segmentId);originalExpected.put("operation",cap);if(release)originalExpected.put(targetKey(cap),uuid(s,targetKey(cap)));else for(String field:List.of("action","category","startQuantity","quantity","unit","validFrom","validUntil","workId"))originalExpected.put(field,text(s,field,80));if(s.containsKey("customerId"))originalExpected.put("customerId",uuid(s,"customerId"));evidence.require(c,uuid(s,"evidenceId"),segmentId,originalExpected);
  if(release){if(!"ACTIVE".equals(target.get("state")))throw new DomainError("CONFLICT","STALE_REVISION","Referenced permission is inactive");}
  else{
   if(!cap.equals("placeHold")||segment.get("retiredAt")==null)stock.leaf(c,segmentId,c.knownAt());else if(r.currentRows(c,"QuantitySegments").stream().noneMatch(child->child.get("retiredAt")==null&&!PhysicalRanges.project(r.currentRows(c,"GenealogyEdges"),segmentId,(String)child.get("ID"),BigDecimal.ZERO,StockPrimitives.amount(segment)).isEmpty()))throw DomainError.invalid("No identifiable descendant hold scope");String action=text(s,"action",80);if(!Set.of("SELL","DISPATCH","DISPOSE","INTERNAL_MOVE","ALL").contains(action))throw DomainError.invalid("Unsupported action");
   String category=text(s,"category",40);if(!Set.of("QC","CUSTOMER","COMMERCIAL","RECALL").contains(category)||(cap.equals("recordDispositionBasis")&&(category.equals("RECALL")||action.equals("ALL"))))throw DomainError.invalid("Unsupported permission category");
   if(s.containsKey("customerId"))uuid(s,"customerId");
   Instant from=instant(s,"validFrom"),until=instant(s,"validUntil");if(from.isAfter(c.knownAt())||until.isBefore(from))throw DomainError.invalid("Closed validity interval required");
   var q=stock.quantity(c,segment,s.get("quantity"),text(s,"unit",40));BigDecimal start=decimal(s,"startQuantity");stock.quantity(c,segment,start.signum()==0?"1":start.toPlainString(),text(s,"unit",40));if(start.signum()<0||start.add(q).compareTo(StockPrimitives.amount(segment))>0)throw DomainError.invalid("Physical permission range exceeds segment");
   uuid(s,"workId");if(!instant(s,"nextCheckAt").isAfter(c.knownAt()))throw DomainError.invalid("Future next check required");
  }
  var fences=new TreeSet<>(stock.fences(segment));fences.add("quality/segment/"+segmentId);if(release)fences.add("quality/decision/"+target.get("ID"));
  String work=(String)(release?target.get("workId"):s.get("workId"));if(work==null)throw DomainError.invalid("Responsibility work required");
  String category=release?(String)target.get("category"):text(s,"category",40);
  var rule=policy.rule(c,cap).rule();Object configured=rule.get("categoryCapabilities");
  if(!(configured instanceof Map<?,?> categories)||!(categories.get(category) instanceof String decisionCapability)||decisionCapability.isBlank())throw new DomainError("HELD","POLICY_UNRESOLVED","Category decision authority unresolved");
  if(!"HUMAN".equals(identities.actor(c.organizationId(),c.actorId()).orElseThrow(DomainError::forbidden).get("kind")))throw DomainError.forbidden();
  auth.authorizeScopes(c,decisionCapability,Map.of("TARGET",List.of(segmentId),"ITEM",List.of((String)segment.get("itemId")),"PLACE",List.of((String)segment.get("placeId")),"WORK",List.of(work)));
  return CommandPreparation.ordinary(Map.of("TARGET",List.of(segmentId),"ITEM",List.of((String)segment.get("itemId")),"PLACE",List.of((String)segment.get("placeId")),"WORK",List.of(work)),new ArrayList<>(fences),"QUALITY_CONTROL",(String)target.get("ID"),((Number)target.get("revision")).intValue());
 }
 public List<SubjectBinding> subjectBindings(DomainContext c,Map<String,Object>i,CommandPreparation p){return List.of(SubjectBinding.optional("QuantitySegment",Set.copyOf(p.scopes().get("TARGET"))),SubjectBinding.optional("TradeItem",Set.copyOf(p.scopes().get("ITEM"))),SubjectBinding.optional("Work",Set.copyOf(p.scopes().get("WORK"))));}
 public Map<String,Object>execute(DomainContext c,Map<String,Object>i){var p=prepare(c,i);var s=slots(i);String cap=text(i,"capabilityId",80),segment=p.scopes().get("TARGET").getFirst(),command=CommandExecution.commandId(),hash=policy.rule(c,cap).policyHash();var decision=new LinkedHashMap<String,Object>();decision.putAll(Map.of("actorId",c.actorId(),"operation",cap,"segmentId",segment,"evidenceId",uuid(s,"evidenceId"),"policyHash",hash,"commandId",command,"reason",text(s,"reason",240)));
  if(releasing(cap))decision.put(targetKey(cap),uuid(s,targetKey(cap)));String decisionId=primitive.decision(c,decision);Map<String,Object>row;
  if(releasing(cap))row=primitive.release(c,entity(cap),uuid(s,targetKey(cap)),decisionId);
  else{var values=new LinkedHashMap<String,Object>();for(String k:List.of("action","category","customerId","unit","workId"))if(s.get(k)!=null)values.put(k,s.get(k));for(String k:List.of("validFrom","validUntil","nextCheckAt"))values.put(k,instant(s,k));values.putAll(Map.of("segmentId",segment,"controlScope",r.current(c,"QuantitySegments",segment).get("controlScope"),"startQuantity",decimal(s,"startQuantity"),"quantity",new BigDecimal(s.get("quantity").toString()),"state","ACTIVE","decisionId",decisionId,"evidenceRef",uuid(s,"evidenceId"),"commandId",command,"policyHash",hash));row=primitive.record(c,entity(cap),values);}
  var suspended=new ArrayList<Map<String,Object>>();if(cap.equals("placeHold")||cap.equals("revokeDispositionBasis")){var physical=r.current(c,"QuantitySegments",segment);for(var allocation:r.currentRows(c,"SegmentAllocations"))if("EXECUTABLE".equals(allocation.get("state"))&&(segment.equals(allocation.get("segmentId"))||!PhysicalRanges.project(r.currentRows(c,"GenealogyEdges"),segment,(String)allocation.get("segmentId"),BigDecimal.ZERO,StockPrimitives.amount(physical)).isEmpty())){var allocationPhysical=r.current(c,"QuantitySegments",(String)allocation.get("segmentId"));var current=eligibility.assess(c,allocationPhysical,Objects.toString(allocation.get("action"),"SELL"),Objects.toString(allocation.get("customerId"),null));var amount=(BigDecimal)allocation.get("quantity");boolean enough=allocation.get("startQuantity") instanceof BigDecimal start?QualityRanges.quantity(QualityRanges.intersect(current.ranges(),List.of(new QualityRanges.Range(start,start.add(amount))))).compareTo(amount)>=0:"ALLOWED".equals(current.state())&&current.eligibleQuantity().compareTo(StockPrimitives.amount(allocationPhysical))>=0;if(!enough){primitive.suspendAllocation(c,(String)allocation.get("ID"),cap);suspended.add(allocation);}}}
  if(cap.equals("placeHold")||cap.equals("revokeDispositionBasis"))impact.recorded(c,(String)row.get("workId"),(String)row.get("ID"),"QUALITY_REVIEW",segment,"Review current quality and action authority",c.knownAt().plusSeconds(60),Map.of("startQuantity",InventoryQuantity.text(row.get("startQuantity")),"segmentId",segment,"action",row.get("action"),"category",row.get("category")),(BigDecimal)row.get("quantity"),(String)row.get("unit"));else impact.invalidate(c,(String)row.get("workId"));
  if(cap.equals("releaseHold"))responsibilities.released(c,(String)row.get("ID"),decisionId);
  return Map.of("outcome","APPLIED","revision",row.get("revision"),"effects",Map.of("decisionId",decisionId,entity(cap).equals("Restrictions")?"restrictionId":"dispositionBasisId",row.get("ID"),"suspendedAllocationIds",suspended.stream().map(x->x.get("ID")).toList()));
 }
 public static Instant instant(Map<String,Object>s,String key){try{return Instant.parse(text(s,key,80));}catch(java.time.format.DateTimeParseException e){throw DomainError.invalid("UTC instant required");}}
 public static BigDecimal decimal(Map<String,Object>s,String key){try{String v=text(s,key,80);if(!v.matches("(?:0|[1-9][0-9]*)(?:\\.[0-9]{1,12})?"))throw DomainError.invalid("Decimal string required");var d=new BigDecimal(v);if(d.precision()>38)throw DomainError.invalid("Decimal precision");return d;}catch(NumberFormatException e){throw DomainError.invalid("Decimal required");}}
}
