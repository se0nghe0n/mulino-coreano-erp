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
 public record Result(String state,BigDecimal eligibleQuantity,List<Range> allowedRanges,List<String> evidenceRefs,Instant nextValidityBoundary,List<String> unknowns){}
 private final RegulatoryRepository r;private final RegulatoryEvidence evidence;private final InventoryRepository inventory;
 public RegulatoryEligibility(RegulatoryRepository r,RegulatoryEvidence evidence,InventoryRepository inventory){this.r=r;this.evidence=evidence;this.inventory=inventory;}
 public Result assess(DomainContext c,String itemId,String lotId,String physicalScopeId,String action,BigDecimal quantity,String unit){
  var seg=inventory.current(c,"QuantitySegments",physicalScopeId);if(!itemId.equals(seg.get("itemId"))||!Objects.equals(lotId,seg.get("lotId"))||!unit.equals(seg.get("unit"))||!"IDENTIFIED".equals(seg.get("mixtureStatus"))||!"CONFIRMED".equals(seg.get("identificationStatus")))return unknown("PHYSICAL_SCOPE_UNCERTAIN");
  var policies=r.rows(c,"Policies").stream().filter(p->action.equals(p.get("action"))&&"ACTIVE".equals(p.get("status"))&&valid(p,c.asOf())).toList();if(policies.size()!=1)return unknown("REGULATORY_POLICY_UNRESOLVED");var policy=policies.getFirst();
  List<Range> allowed=new ArrayList<>(),denied=new ArrayList<>();List<String> refs=new ArrayList<>();Instant boundary=policy.get("validUntil")==null?null:com.mulino.domain.evidence.EvidenceTypes.instant(policy.get("validUntil"));
  var decisions=r.rows(c,"DecisionVersions");var labels=r.rows(c,"LabelVerifications");
  for(var d:decisions){if(!itemId.equals(d.get("itemId"))||!Objects.equals(lotId,d.get("lotId"))||!physicalScopeId.equals(d.get("physicalScopeId"))||!action.equals(d.get("action"))||!unit.equals(d.get("unit"))||!policy.get("ID").equals(d.get("policyId")))continue;
   if(decisions.stream().anyMatch(n->d.get("ID").equals(n.get("previousVersionId"))&&!com.mulino.domain.evidence.EvidenceTypes.instant(n.get("validFrom")).isAfter(c.asOf())))continue;
   Instant from=com.mulino.domain.evidence.EvidenceTypes.instant(d.get("validFrom"));Instant until=d.get("validUntil")==null?null:com.mulino.domain.evidence.EvidenceTypes.instant(d.get("validUntil"));for(Instant time:List.of(from))if(time.isAfter(c.asOf())&&(boundary==null||time.isBefore(boundary)))boundary=time;if(until!=null&&until.isAfter(c.asOf())&&(boundary==null||until.isBefore(boundary)))boundary=until;
   if(!valid(d,c.asOf())||!evidence.current(c,d.get("occurrenceId").toString()))continue;
   var proc=r.require(c,"Procedures",d.get("procedureId").toString());var version=r.require(c,"ProcedureVersions",d.get("procedureVersionId").toString());if(!"SUBMITTED".equals(version.get("status"))||!evidence.current(c,version.get("occurrenceId").toString()))continue;
   if(Boolean.TRUE.equals(policy.get("requiresLabel"))){var item=inventory.current(c,"TradeItems",itemId);var latest=labels.stream().filter(l->proc.get("ID").equals(l.get("procedureId"))&&d.get("procedureVersionId").equals(l.get("procedureVersionId"))).max(Comparator.comparingInt(l->((Number)l.get("revision")).intValue()));boolean label=latest.stream().anyMatch(l->proc.get("ID").equals(l.get("procedureId"))&&d.get("procedureVersionId").equals(l.get("procedureVersionId"))&&"VERIFIED".equals(l.get("decision"))&&item.get("packagingVersionId").equals(l.get("packagingVersionId"))&&item.get("specificationVersionId").equals(l.get("specificationVersionId"))&&valid(l,c.asOf())&&evidence.current(c,l.get("occurrenceId").toString()));if(latest.isPresent()){Instant begin=com.mulino.domain.evidence.EvidenceTypes.instant(latest.get().get("validFrom"));if(begin.isAfter(c.asOf())&&(boundary==null||begin.isBefore(boundary)))boundary=begin;}if(latest.isPresent()&&latest.get().get("validUntil")!=null){Instant expiry=com.mulino.domain.evidence.EvidenceTypes.instant(latest.get().get("validUntil"));if(expiry.isAfter(c.asOf())&&(boundary==null||expiry.isBefore(boundary)))boundary=expiry;}if(!label)continue;}
   BigDecimal start=new BigDecimal(d.get("startQuantity").toString()),end=start.add(new BigDecimal(d.get("quantity").toString())).min(quantity);if(start.compareTo(end)>=0)continue;refs.add(d.get("occurrenceId").toString());("ALLOWED".equals(d.get("decision"))?allowed:denied).add(new Range(start,end));
  }
  var ranges=subtract(union(allowed),union(denied));BigDecimal total=ranges.stream().map(x->x.end.subtract(x.start)).reduce(BigDecimal.ZERO,BigDecimal::add);return new Result(total.signum()>0?"ALLOWED":"UNKNOWN",total,ranges,refs.stream().distinct().toList(),boundary,total.signum()>0?List.of():List.of("NO_CURRENT_REGULATORY_ALLOWANCE"));
 }
 private Result unknown(String cause){return new Result("UNKNOWN",BigDecimal.ZERO,List.of(),List.of(),null,List.of(cause));}
 public static List<Range> union(List<Range> input){var sorted=input.stream().sorted(Comparator.comparing(Range::start)).toList();var out=new ArrayList<Range>();for(var x:sorted){if(out.isEmpty()||out.getLast().end.compareTo(x.start)<0)out.add(x);else{var old=out.removeLast();out.add(new Range(old.start,old.end.max(x.end)));}}return List.copyOf(out);}
 public static List<Range> subtract(List<Range> allowed,List<Range> denied){var out=new ArrayList<Range>();for(var a:allowed){BigDecimal cursor=a.start;for(var d:denied){if(d.end.compareTo(cursor)<=0||d.start.compareTo(a.end)>=0)continue;if(d.start.compareTo(cursor)>0)out.add(new Range(cursor,d.start.min(a.end)));cursor=cursor.max(d.end);}if(cursor.compareTo(a.end)<0)out.add(new Range(cursor,a.end));}return List.copyOf(out);}
}
