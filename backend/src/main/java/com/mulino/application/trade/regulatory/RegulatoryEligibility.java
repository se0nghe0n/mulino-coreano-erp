package com.mulino.application.trade.regulatory;
import com.mulino.application.core.*;
import com.mulino.domain.trade.regulatory.RegulatoryRepository;
import com.mulino.domain.inventory.InventoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.mulino.application.trade.regulatory.RegulatoryInputs.*;
@Component
public class RegulatoryEligibility {
 public record Range(BigDecimal start,BigDecimal end){}
 public record Result(String state,BigDecimal eligibleQuantity,List<Range> allowedRanges,List<String> evidenceRefs,Instant nextValidityBoundary,List<String> unknowns,List<Map<String,Object>> conditions){
  public Result(String state,BigDecimal eligibleQuantity,List<Range> allowedRanges,List<String> evidenceRefs,Instant nextValidityBoundary,List<String> unknowns){this(state,eligibleQuantity,allowedRanges,evidenceRefs,nextValidityBoundary,unknowns,List.of());}
 }
 private final RegulatoryRepository r;private final RegulatoryEvidence evidence;private final InventoryRepository inventory;private final com.mulino.domain.inventory.PhysicalRanges physicalRanges;
 public RegulatoryEligibility(RegulatoryRepository r,RegulatoryEvidence evidence,InventoryRepository inventory,com.mulino.domain.inventory.PhysicalRanges physicalRanges){this.r=r;this.evidence=evidence;this.inventory=inventory;this.physicalRanges=physicalRanges;}
 public Result assess(DomainContext c,String itemId,String lotId,String physicalScopeId,String action,BigDecimal quantity,String unit){
  return assess(c,itemId,lotId,physicalScopeId,action,quantity,unit,null);
 }
 public Result assessForFulfillment(DomainContext c,String itemId,String lotId,String physicalScopeId,String action,BigDecimal quantity,String unit,com.mulino.application.inventory.FulfillmentEvidencePurpose purpose){
  Objects.requireNonNull(purpose).requirePhysicalAssessment(c,physicalScopeId,action);
  return assess(c,itemId,lotId,physicalScopeId,action,quantity,unit,purpose);
 }
 private Result assess(DomainContext c,String itemId,String lotId,String physicalScopeId,String action,BigDecimal quantity,String unit,com.mulino.application.inventory.FulfillmentEvidencePurpose purpose){
  var seg=inventory.current(c,"QuantitySegments",physicalScopeId);if(quantity.signum()<0||!unit.equals(seg.get("unit")))return unknown("PHYSICAL_SCOPE_UNCERTAIN");if(seg.get("validFrom")!=null&&com.mulino.domain.evidence.EvidenceTypes.instant(seg.get("validFrom")).isAfter(c.asOf()))return unknown("PHYSICAL_SCOPE_NOT_EFFECTIVE");if(seg.get("retiredAt")!=null&&!com.mulino.domain.evidence.EvidenceTypes.instant(seg.get("retiredAt")).isAfter(c.asOf())&&(seg.get("retirementRecordedAt")==null||!com.mulino.domain.evidence.EvidenceTypes.instant(seg.get("retirementRecordedAt")).isAfter(c.knownAt())))return unknown("PHYSICAL_SCOPE_RETIRED");quantity=quantity.min(new BigDecimal(seg.get("quantity").toString()));if(!itemId.equals(seg.get("itemId"))||!Objects.equals(lotId,seg.get("lotId"))||!unit.equals(seg.get("unit"))||!"IDENTIFIED".equals(seg.get("mixtureStatus"))||!"CONFIRMED".equals(seg.get("identificationStatus")))return unknown("PHYSICAL_SCOPE_UNCERTAIN");
  var policies=r.rows(c,"Policies").stream().filter(p->action.equals(p.get("action"))&&"ACTIVE".equals(p.get("status"))&&valid(p,c.asOf())).toList();if(policies.size()!=1)return unknown("REGULATORY_POLICY_UNRESOLVED");var policy=policies.getFirst();
  List<Range> allowed=new ArrayList<>(),denied=new ArrayList<>();List<String> refs=new ArrayList<>();Instant boundary=policy.get("validUntil")==null?null:com.mulino.domain.evidence.EvidenceTypes.instant(policy.get("validUntil"));
  var decisions=r.rows(c,"DecisionVersions");var labels=r.rows(c,"LabelVerifications");
  var labelChecks=new LinkedHashMap<String,LabelCheck>();var labelAllowed=new ArrayList<Range>();var labelDenied=new ArrayList<Range>();var labelRefs=new TreeSet<String>();
  boolean requiresLabel=Boolean.TRUE.equals(policy.get("requiresLabel"));
  if(!requiresLabel)labelRefs.add(policy.get("ID").toString());
  if(requiresLabel)for(var procedure:r.rows(c,"Procedures")){
   if(!itemId.equals(procedure.get("itemId"))||!Objects.equals(lotId,procedure.get("lotId"))||!policy.get("ID").equals(procedure.get("policyId")))continue;
   var scopeSegment=inventory.current(c,"QuantitySegments",procedure.get("physicalScopeId").toString());
   var projected=physicalRanges.project(c,procedure.get("physicalScopeId").toString(),physicalScopeId,BigDecimal.ZERO,new BigDecimal(scopeSegment.get("quantity").toString()));
   if(projected.isEmpty())continue;
   var versions=r.rows(c,"ProcedureVersions").stream().filter(v->procedure.get("ID").equals(v.get("procedureId"))).toList();
   var heads=versions.stream().filter(v->versions.stream().noneMatch(n->v.get("ID").equals(n.get("previousVersionId")))).toList();
   if(heads.size()!=1)continue;
   var head=heads.getFirst();var check=label(c,procedure,head,policy,labels,itemId,purpose);labelChecks.put(procedure.get("ID").toString(),check);labelRefs.addAll(check.evidenceRefs());
   if(check.boundary()!=null&&(boundary==null||check.boundary().isBefore(boundary)))boundary=check.boundary();
   for(var local:projected){BigDecimal start=local.start().max(BigDecimal.ZERO),end=local.end().min(quantity);if(start.compareTo(end)<0){if("ALLOWED".equals(check.state()))labelAllowed.add(new Range(start,end));if("DENIED".equals(check.state()))labelDenied.add(new Range(start,end));}}
  }
  var rawAllowed=new ArrayList<Range>();var rawDenied=new ArrayList<Range>();var regulatoryRefs=new TreeSet<String>();
  for(var d:decisions){if(!itemId.equals(d.get("itemId"))||!Objects.equals(lotId,d.get("lotId"))||!action.equals(d.get("action"))||!unit.equals(d.get("unit"))||!policy.get("ID").equals(d.get("policyId")))continue;
   if(decisions.stream().anyMatch(n->d.get("ID").equals(n.get("previousVersionId"))&&!com.mulino.domain.evidence.EvidenceTypes.instant(n.get("validFrom")).isAfter(c.asOf())))continue;
   Instant from=com.mulino.domain.evidence.EvidenceTypes.instant(d.get("validFrom"));Instant until=d.get("validUntil")==null?null:com.mulino.domain.evidence.EvidenceTypes.instant(d.get("validUntil"));for(Instant time:List.of(from))if(time.isAfter(c.asOf())&&(boundary==null||time.isBefore(boundary)))boundary=time;if(until!=null&&until.isAfter(c.asOf())&&(boundary==null||until.isBefore(boundary)))boundary=until;
   if(!valid(d,c.asOf()))continue;
   var proc=r.require(c,"Procedures",d.get("procedureId").toString());if(!(purpose==null?evidence.current(c,d.get("occurrenceId").toString()):evidence.current(c,"REGULATORY_DECISION",d,proc,policy,purpose)))continue;var visibleVersions=r.rows(c,"ProcedureVersions").stream().filter(v->proc.get("ID").equals(v.get("procedureId"))).toList();var visibleHead=visibleVersions.stream().filter(v->visibleVersions.stream().noneMatch(n->v.get("ID").equals(n.get("previousVersionId")))).findFirst();if(visibleHead.isEmpty()||!d.get("procedureVersionId").equals(visibleHead.get().get("ID")))continue;var version=r.require(c,"ProcedureVersions",d.get("procedureVersionId").toString());if(!"SUBMITTED".equals(version.get("status"))||!(purpose==null?evidence.current(c,version.get("occurrenceId").toString()):evidence.current(c,"REGULATORY_SUBMISSION",version,proc,policy,purpose)))continue;
   var label=labelChecks.get(proc.get("ID").toString());
   var projected=physicalRanges.project(c,d.get("physicalScopeId").toString(),physicalScopeId,new BigDecimal(d.get("startQuantity").toString()),new BigDecimal(d.get("quantity").toString()));for(var local:projected){BigDecimal start=local.start().max(BigDecimal.ZERO),end=local.end().min(quantity);if(start.compareTo(end)>=0)continue;regulatoryRefs.add(d.get("occurrenceId").toString());("ALLOWED".equals(d.get("decision"))?rawAllowed:rawDenied).add(new Range(start,end));
    if(!requiresLabel||label!=null&&"ALLOWED".equals(label.state())){refs.add(d.get("occurrenceId").toString());("ALLOWED".equals(d.get("decision"))?allowed:denied).add(new Range(start,end));}}
  }
  var currentAllowed=union(allowed);var currentDenied=union(denied);
  // Only admitted, clipped physical intervals can contradict one another. Touching endpoints do not overlap.
  boolean conflict=currentAllowed.stream().anyMatch(a->currentDenied.stream().anyMatch(d->a.start.max(d.start).compareTo(a.end.min(d.end))<0));
  var ranges=subtract(currentAllowed,currentDenied);BigDecimal total=ranges.stream().map(x->x.end.subtract(x.start)).reduce(BigDecimal.ZERO,BigDecimal::add);
  var regulatoryCondition=condition("REGULATORY",rawAllowed,rawDenied,regulatoryRefs);
  var labelCondition=requiresLabel?new LinkedHashMap<>(condition("LABEL",labelAllowed,labelDenied,labelRefs)):new LinkedHashMap<String,Object>(Map.of("condition","LABEL","state","ALLOWED","evidenceRefs",List.of(policy.get("ID")),"allowedRanges",List.of(new Range(BigDecimal.ZERO,quantity))));
  labelCondition.put("required",requiresLabel);labelCondition.put("policyId",policy.get("ID"));labelCondition.put("basis",requiresLabel?"CURRENT_LABEL_VERIFICATION":"NOT_REQUIRED_BY_POLICY");
  labelCondition.put("checks",labelChecks.values().stream().map(LabelCheck::details).toList());
  if(requiresLabel)refs.addAll(labelRefs);
  return new Result(conflict?"CONFLICT":total.signum()>0?"ALLOWED":"UNKNOWN",total,ranges,refs.stream().distinct().toList(),boundary,total.signum()>0?List.of():List.of("NO_CURRENT_REGULATORY_ALLOWANCE"),List.of(regulatoryCondition,labelCondition));
 }
 private record LabelCheck(String state,List<String> evidenceRefs,Instant boundary,Map<String,Object> details){}
 /** The same selected label head controls execution and the public LABEL explanation. */
 private LabelCheck label(DomainContext c,Map<String,Object> procedure,Map<String,Object> version,Map<String,Object> policy,List<Map<String,Object>> labels,String itemId,com.mulino.application.inventory.FulfillmentEvidencePurpose purpose){
  boolean required=Boolean.TRUE.equals(policy.get("requiresLabel"));var details=new LinkedHashMap<String,Object>();details.put("procedureId",procedure.get("ID"));details.put("procedureVersionId",version.get("ID"));details.put("policyId",policy.get("ID"));details.put("required",required);
  if(!required)return new LabelCheck("ALLOWED",List.of(policy.get("ID").toString()),null,details);
  var candidates=labels.stream().filter(row->procedure.get("ID").equals(row.get("procedureId"))&&version.get("ID").equals(row.get("procedureVersionId"))).toList();
  Instant boundary=null;
  for(var row:candidates)for(String key:List.of("validFrom","validUntil"))if(row.get(key)!=null){Instant time=com.mulino.domain.evidence.EvidenceTypes.instant(row.get(key));if(time.isAfter(c.asOf())&&(boundary==null||time.isBefore(boundary)))boundary=time;}
  var latest=candidates.stream().filter(row->!com.mulino.domain.evidence.EvidenceTypes.instant(row.get("validFrom")).isAfter(c.asOf())).max(Comparator.comparingInt(row->((Number)row.get("revision")).intValue()));
  if(latest.isEmpty())return new LabelCheck("UNKNOWN",List.of(),boundary,details);
  var row=latest.get();
  boolean current=valid(row,c.asOf())&&evidence.current(c,"LABEL_VERIFICATION",row,procedure,policy,purpose);details.put("currentEvidence",current);
  if(!current)return new LabelCheck("UNKNOWN",List.of(),boundary,details);
  details.put("labelVerificationId",row.get("ID"));details.put("decision",row.get("decision"));
  var item=inventory.current(c,"TradeItems",itemId);boolean packaging=Objects.equals(item.get("packagingVersionId"),row.get("packagingVersionId")),specification=Objects.equals(item.get("specificationVersionId"),row.get("specificationVersionId"));
  details.put("packagingVersionId",row.get("packagingVersionId"));details.put("specificationVersionId",row.get("specificationVersionId"));details.put("packagingMatched",packaging);details.put("specificationMatched",specification);
  String state=!packaging||!specification||"REJECTED".equals(row.get("decision"))?"DENIED":"VERIFIED".equals(row.get("decision"))?"ALLOWED":"UNKNOWN";details.put("state",state);
  return new LabelCheck(state,current?List.of(row.get("occurrenceId").toString()):List.of(),boundary,details);
 }
 private Map<String,Object> condition(String name,List<Range> allowed,List<Range> denied,Collection<String> refs){
  var a=union(allowed);var d=union(denied);boolean conflict=a.stream().anyMatch(x->d.stream().anyMatch(y->x.start().max(y.start()).compareTo(x.end().min(y.end()))<0));var ranges=subtract(a,d);
  return Map.of("condition",name,"state",conflict?"CONFLICT":!ranges.isEmpty()?"ALLOWED":!d.isEmpty()?"DENIED":"UNKNOWN","evidenceRefs",List.copyOf(refs),"allowedRanges",ranges);
 }
 private Result unknown(String cause){return new Result("UNKNOWN",BigDecimal.ZERO,List.of(),List.of(),null,List.of(cause));}
 public static List<Range> union(List<Range> input){var sorted=input.stream().sorted(Comparator.comparing(Range::start)).toList();var out=new ArrayList<Range>();for(var x:sorted){if(out.isEmpty()||out.getLast().end.compareTo(x.start)<0)out.add(x);else{var old=out.removeLast();out.add(new Range(old.start,old.end.max(x.end)));}}return List.copyOf(out);}
 public static List<Range> subtract(List<Range> allowed,List<Range> denied){var out=new ArrayList<Range>();for(var a:allowed){BigDecimal cursor=a.start;for(var d:denied){if(d.end.compareTo(cursor)<=0||d.start.compareTo(a.end)>=0)continue;if(d.start.compareTo(cursor)>0)out.add(new Range(cursor,d.start.min(a.end)));cursor=cursor.max(d.end);}if(cursor.compareTo(a.end)<0)out.add(new Range(cursor,a.end));}return List.copyOf(out);}
}
