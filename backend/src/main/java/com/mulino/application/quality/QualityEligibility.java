package com.mulino.application.quality;
import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.application.inventory.InventoryReadCandidates;
import com.mulino.domain.work.read.WorkReadRepository;
import com.mulino.application.trade.InventoryReadFacts;
import com.mulino.application.trade.regulatory.RegulatoryEligibility;
import com.mulino.domain.governance.PolicyRepository;
import com.mulino.domain.inventory.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
/** Conditions intersect exact physical coordinates; ownership and custody are never permissions. */
@Component
public class QualityEligibility implements InventoryReadFacts,QueryHandler {
 private final InventoryRepository r;private final StockPrimitives stock;private final QualityEvidence evidence;private final RegulatoryEligibility regulatory;private final PolicyRepository policies;private final IdentityAuthorization auth;private final WorkReadRepository works;
 public QualityEligibility(InventoryRepository r,StockPrimitives stock,QualityEvidence evidence,RegulatoryEligibility regulatory,PolicyRepository policies,IdentityAuthorization auth,WorkReadRepository works){this.r=r;this.stock=stock;this.evidence=evidence;this.regulatory=regulatory;this.policies=policies;this.auth=auth;this.works=works;}
 public Set<String>metrics(){return Set.of("eligibleQuantity","reservedQuantity","unreservedEligibleQuantity","eligibilityStatus","allocationShortageQuantity","reservationResponsibilityQuantity");}
 public Set<String>operations(){return Set.of("evaluateEligibility");}
 public record Result(String state,BigDecimal eligibleQuantity,List<QualityRanges.Range>ranges,List<Map<String,Object>>conditions,List<String>evidenceRefs,List<String>unknowns,Instant nextValidityBoundary){}
 public Result assess(DomainContext c,Map<String,Object>segment,String action,String customerId){
  return assess(c,segment,action,customerId,null);
 }
 public Result assessForFulfillment(DomainContext c,Map<String,Object>segment,String action,String customerId,com.mulino.application.inventory.FulfillmentEvidencePurpose purpose){
  Objects.requireNonNull(purpose).requireAssessment(c,segment,action,customerId);
  return assess(c,segment,action,customerId,purpose);
 }
 private Result assess(DomainContext c,Map<String,Object>segment,String action,String customerId,com.mulino.application.inventory.FulfillmentEvidencePurpose purpose){
  var unknowns=new TreeSet<String>();var refs=new TreeSet<String>();var conditions=new ArrayList<Map<String,Object>>();BigDecimal quantity=StockPrimitives.amount(segment);String id=(String)segment.get("ID");Instant at=c.asOf(),next=null;
  var allowed=List.of(new QualityRanges.Range(BigDecimal.ZERO,quantity));
  if(StockPrimitives.instant(segment.get("validFrom")).isAfter(at)||(segment.get("retiredAt")!=null&&!StockPrimitives.instant(segment.get("retiredAt")).isAfter(at)&&(segment.get("retirementRecordedAt")==null||!StockPrimitives.instant(segment.get("retirementRecordedAt")).isAfter(c.knownAt())))||!"CONFIRMED".equals(segment.get("identificationStatus"))||"UNCERTAIN_MIXTURE".equals(segment.get("mixtureStatus"))){unknowns.add("PHYSICAL_SUBSET_NOT_IDENTIFIABLE");allowed=List.of();}
  var policy=policies.current(c.organizationId(),"ELIGIBILITY",auth.now());boolean policyResolved=false;
  if(policy.size()==1)try{var content=new ObjectMapper().readTree(policy.getFirst().get("content").toString());policyResolved=content.path("actions").path(action).path("requiredCategories").isArray()&&content.path("actions").path(action).path("requiredCategories").toString().equals("[\"QC\",\"CUSTOMER\",\"COMMERCIAL\"]");if(policy.getFirst().get("effectiveUntil")!=null)next=minimum(next,StockPrimitives.instant(policy.getFirst().get("effectiveUntil")));refs.add((String)policy.getFirst().get("ID"));}catch(Exception invalid){policyResolved=false;}
  if(!policyResolved){unknowns.add("CURRENT_ELIGIBILITY_POLICY_UNRESOLVED");allowed=List.of();}
  var lot=r.object(c,"ManufacturingLots",(String)segment.get("lotId"));
  if(lot.get("expiresAt")==null){unknowns.add("LOT_EXPIRY_UNKNOWN");allowed=List.of();}else{Instant expires=StockPrimitives.instant(lot.get("expiresAt"));next=minimumFuture(next,expires.plusNanos(1000),at);if(at.isAfter(expires)){conditions.add(Map.of("condition","LOT_VALIDITY","state","DENIED"));allowed=List.of();}}
  var bases=r.rows(c,"DispositionBases");
  for(String category:List.of("QC","CUSTOMER","COMMERCIAL")){
   var ranges=new ArrayList<QualityRanges.Range>();
   for(var b:bases)if(b.get("segmentId")!=null&&category.equals(b.get("category"))&&action.equals(b.get("action"))&&(b.get("customerId")==null||Objects.equals(customerId,b.get("customerId")))){
    var projected=PhysicalRanges.project(r.rows(c,"GenealogyEdges"),(String)b.get("segmentId"),id,range(b).start(),(BigDecimal)b.get("quantity"));if(projected.isEmpty())continue;next=permissionBoundary(next,b,at);if(active(b,at,c.knownAt())&&(purpose==null?evidence.valid(c,(String)b.get("evidenceRef"),(String)b.get("segmentId")):evidence.valid(c,b,purpose))){ranges.addAll(projected);refs.add((String)b.get("evidenceRef"));}
   }
   String state=ranges.isEmpty()?"UNKNOWN":"ALLOWED";if(ranges.isEmpty())unknowns.add(category+"_AUTHORITY_UNCONFIRMED");conditions.add(Map.of("condition",category,"state",state,"allowedRanges",dto(ranges)));
   allowed=QualityRanges.intersect(allowed,ranges);
  }
  var reg=purpose==null?regulatory.assess(c,(String)segment.get("itemId"),(String)segment.get("lotId"),id,action,quantity,(String)segment.get("unit")):regulatory.assessForFulfillment(c,(String)segment.get("itemId"),(String)segment.get("lotId"),id,action,quantity,(String)segment.get("unit"),purpose);
  conditions.add(Map.of("condition","REGULATORY","state",reg.state()));unknowns.addAll(reg.unknowns());refs.addAll(reg.evidenceRefs());next=minimum(next,reg.nextValidityBoundary());allowed=QualityRanges.intersect(allowed,reg.allowedRanges().stream().map(x->new QualityRanges.Range(x.start(),x.end())).toList());
  var blocks=new ArrayList<QualityRanges.Range>();boolean blocked=false;
  for(var hold:r.rows(c,"Restrictions"))if(Objects.equals(segment.get("controlScope"),hold.get("controlScope"))&&Set.of(action,"ALL").contains(hold.get("action"))){
   if(hold.get("segmentId")!=null&&PhysicalRanges.project(r.rows(c,"GenealogyEdges"),(String)hold.get("segmentId"),id,range(hold).start(),(BigDecimal)hold.get("quantity")).isEmpty()&&!overlap(c,id,(String)hold.get("segmentId")))continue;
   next=permissionBoundary(next,hold,at);if(active(hold,at,c.knownAt())){
    if(hold.get("segmentId")==null){blocks.add(new QualityRanges.Range(BigDecimal.ZERO,quantity));blocked=true;}
    else {var projected=PhysicalRanges.project(r.rows(c,"GenealogyEdges"),(String)hold.get("segmentId"),id,range(hold).start(),(BigDecimal)hold.get("quantity"));if(!projected.isEmpty()){blocks.addAll(projected);blocked=true;refs.add((String)hold.get("evidenceRef"));}else if(overlap(c,id,(String)hold.get("segmentId"))&&!exactAncestry(c,(String)hold.get("segmentId"),segment)){blocks.add(new QualityRanges.Range(BigDecimal.ZERO,quantity));blocked=true;unknowns.add("ANCESTOR_SCOPE_REQUIRES_PHYSICAL_RECONCILIATION");}}
   }
  }
  allowed=QualityRanges.subtract(allowed,blocks);conditions.add(Map.of("condition","RESTRICTIONS","state",blocked?"DENIED":"ALLOWED","blockedRanges",dto(blocks)));
  BigDecimal eligible=QualityRanges.quantity(allowed);String state=eligible.signum()>0?(eligible.compareTo(quantity)==0?"ALLOWED":"PARTIAL"):(unknowns.isEmpty()?"DENIED":"UNKNOWN");
  return new Result(state,eligible,allowed,List.copyOf(conditions),List.copyOf(refs),List.copyOf(unknowns),next);
 }
 private boolean exactAncestry(DomainContext c,String ancestor,Map<String,Object> segment){var root=r.object(c,"QuantitySegments",ancestor);return QualityRanges.quantity(PhysicalRanges.project(r.rows(c,"GenealogyEdges"),ancestor,(String)segment.get("ID"),BigDecimal.ZERO,StockPrimitives.amount(root))).compareTo(StockPrimitives.amount(segment))==0;}
 private boolean overlap(DomainContext c,String a,String b){var all=new HashSet<String>();all.add(b);boolean changed;do{changed=false;for(var edge:r.rows(c,"GenealogyEdges"))if(all.contains(edge.get("sourceId")))changed|=all.add((String)edge.get("targetId"));}while(changed);return all.contains(a);}
 /** Time permissions use full closed [validFrom,validUntil]; release is effective at releasedAt. */
 public static boolean active(Map<String,Object>b,Instant at){return active(b,at,Instant.MAX);}
 public static boolean active(Map<String,Object>b,Instant at,Instant known){if(b.get("validFrom")==null||at.isBefore(StockPrimitives.instant(b.get("validFrom")))||(b.get("validUntil")!=null&&at.isAfter(StockPrimitives.instant(b.get("validUntil")))))return false;return b.get("releasedAt")!=null?(StockPrimitives.instant(b.get("releasedAt")).isAfter(known)||at.isBefore(StockPrimitives.instant(b.get("releasedAt")))):"ACTIVE".equals(b.get("state"));}
 public static QualityRanges.Range range(Map<String,Object>b){var start=b.get("startQuantity") instanceof BigDecimal x?x:BigDecimal.ZERO;return new QualityRanges.Range(start,start.add((BigDecimal)b.get("quantity")));}
 private static Instant permissionBoundary(Instant next,Map<String,Object>b,Instant at){if("ACTIVE".equals(b.get("state"))&&b.get("validUntil")!=null)next=minimumFuture(next,StockPrimitives.instant(b.get("validUntil")).plusNanos(1000),at);if(b.get("validFrom")!=null)next=minimumFuture(next,StockPrimitives.instant(b.get("validFrom")),at);return next;}
 private static Instant minimumFuture(Instant a,Instant b,Instant at){return b.isAfter(at)?minimum(a,b):a;}
 private static Instant minimum(Instant a,Instant b){return b==null?a:a==null?b:a.isBefore(b)?a:b;}
 public static List<Map<String,Object>>dto(Collection<QualityRanges.Range>ranges){return QualityRanges.union(ranges).stream().map(x->Map.<String,Object>of("startQuantity",InventoryQuantity.text(x.start()),"quantity",InventoryQuantity.text(x.end().subtract(x.start())))).toList();}
 /**
  * Only stock in controlled internal storage under a confirmed internal custodian can be sale-eligible (plan §4.2, §6), matching
  * the fulfillment write gate (FulfillmentCommands.requireWarehouse). Only an explicit external place kind (in transit, delivered
  * to a customer, at a supplier, or an EXTERNAL_* kind) is OUTSIDE and a known zero. A place whose kind is unrecorded, internal
  * storage whose custodian is unrecorded, or a kind outside the place vocabulary (for example a free-text WAREHOUSE) is not
  * counted but reported as unknown rather than a known zero (plan §3.1): an unrecognized kind is not evidence of external custody.
  */
 private enum Custody{CONFIRMED,UNCONFIRMED,UNRECOGNIZED,OUTSIDE}
 static final Set<String> EXTERNAL_PLACE_KINDS=Set.of("TRANSIT","CUSTOMER","SUPPLIER");
 private Custody custody(DomainContext c,Map<String,Object> segment){
  Object kind=r.object(c,"Places",(String)segment.get("placeId")).get("kind");
  if(kind==null)return Custody.UNCONFIRMED;
  if("INTERNAL_STORAGE".equals(kind))return segment.get("custodianId")!=null&&r.internalCustodian(c,(String)segment.get("custodianId"))?Custody.CONFIRMED:Custody.UNCONFIRMED;
  return EXTERNAL_PLACE_KINDS.contains(kind)||kind.toString().startsWith("EXTERNAL_")?Custody.OUTSIDE:Custody.UNRECOGNIZED;
 }
 /** The custody condition applied to a segment result for SELL/DISPATCH, shared by the item-scope read and evaluateEligibility. */
 private Result withCustody(DomainContext c,Map<String,Object> segment,java.util.function.Supplier<Result> assessed){
  var custody=custody(c,segment);
  if(custody==Custody.OUTSIDE)return new Result("DENIED",BigDecimal.ZERO,List.of(),List.of(Map.of("condition","CUSTODY","state","DENIED")),List.of(),List.of(),null);
  var result=assessed.get();if(custody==Custody.CONFIRMED){var conditions=new ArrayList<>(result.conditions());conditions.add(Map.of("condition","CUSTODY","state","ALLOWED"));return new Result(result.state(),result.eligibleQuantity(),result.ranges(),List.copyOf(conditions),result.evidenceRefs(),result.unknowns(),result.nextValidityBoundary());}
  var u=new TreeSet<String>(result.unknowns());u.add("CUSTODY_UNCONFIRMED");if(custody==Custody.UNRECOGNIZED)u.add("PLACE_KIND_UNRECOGNIZED");
  var conditions=new ArrayList<>(result.conditions());conditions.add(Map.of("condition","CUSTODY","state","UNKNOWN"));
  return new Result("UNKNOWN",BigDecimal.ZERO,List.of(),List.copyOf(conditions),result.evidenceRefs(),List.copyOf(u),result.nextValidityBoundary());
 }
 /**
  * Item-scope SELL facts. Stock that is in transit or at a customer/supplier place is held history, not sale-eligible warehouse
  * stock (plan §13.3 E1 "현재 판매 적격0"), so it is never assessed as eligible. Without a current eligibility policy the
  * confirmed subset cannot be determined at all; the quantities are then null with UNKNOWN status, never zero (plan §3.1, §6).
  */
 public Facts read(DomainContext c,String operation,String itemId,Map<String,Object>scope,List<Map<String,Object>>segments){
  BigDecimal eligible=BigDecimal.ZERO,reserved=BigDecimal.ZERO,unreserved=BigDecimal.ZERO;var unknowns=new TreeSet<String>();var refs=new TreeSet<String>();boolean partial=false,undeterminable=false;
  for(var segment:segments){Result result=withCustody(c,segment,()->assess(c,segment,(String)scope.getOrDefault("action","SELL"),(String)scope.get("customerId")));undeterminable|=result.unknowns().contains("CURRENT_ELIGIBILITY_POLICY_UNRESOLVED");eligible=eligible.add(result.eligibleQuantity());unknowns.addAll(result.unknowns());refs.addAll(result.evidenceRefs());partial|=!"ALLOWED".equals(result.state());
   var allocations=r.rows(c,"SegmentAllocations").stream().filter(a->segment.get("ID").equals(a.get("segmentId"))&&Set.of("EXECUTABLE","SUSPENDED").contains(a.get("state"))).toList();var ranges=new ArrayList<QualityRanges.Range>();BigDecimal unidentified=BigDecimal.ZERO;for(var a:allocations){reserved=reserved.add((BigDecimal)a.get("quantity"));if(a.get("startQuantity")==null)unidentified=unidentified.add((BigDecimal)a.get("quantity"));else ranges.add(range(a));}
   unreserved=unreserved.add(QualityRanges.quantity(QualityRanges.subtract(result.ranges(),ranges)).subtract(unidentified).max(BigDecimal.ZERO));
  }
  BigDecimal shortage=BigDecimal.ZERO;
  for(var a:r.rows(c,"SegmentAllocations"))if("SUSPENDED".equals(a.get("state"))&&Set.of("PHYSICAL_SHORTAGE","PHYSICAL_RANGE_UNCONFIRMED").contains(Objects.toString(a.get("suspensionReason"),""))){var source=r.object(c,"QuantitySegments",(String)a.get("segmentId"));if(itemId.equals(source.get("itemId"))&&(scope.get("placeId")==null||scope.get("placeId").equals(source.get("placeId")))&&(scope.get("lotId")==null||scope.get("lotId").equals(source.get("lotId")))&&(scope.get("customerId")==null||scope.get("customerId").equals(a.get("customerId")))&&auth.permittedScopes(c,operation,Map.of("TARGET",List.of((String)source.get("ID")),"ITEM",List.of(itemId),"PLACE",List.of((String)source.get("placeId")))))shortage=shortage.add((BigDecimal)a.get("quantity"));}
  if(shortage.signum()>0)unknowns.add("UNRESOLVED_ALLOCATION_PHYSICAL_SCOPE");
  var values=new LinkedHashMap<String,Object>();values.put("eligibleQuantity",undeterminable?null:InventoryQuantity.text(eligible));values.put("reservedQuantity",InventoryQuantity.text(reserved));values.put("allocationShortageQuantity",InventoryQuantity.text(shortage));values.put("reservationResponsibilityQuantity",InventoryQuantity.text(reserved.add(shortage)));values.put("unreservedEligibleQuantity",undeterminable?null:InventoryQuantity.text(unreserved));values.put("eligibilityStatus",undeterminable?"UNKNOWN":eligible.signum()>0?(partial?"PARTIAL":"ALLOWED"):(unknowns.isEmpty()?"DENIED":"UNKNOWN"));
  return new Facts(values,List.copyOf(unknowns),List.of(),List.copyOf(refs));
 }
 /** S4 consumers use this inside their gateway transaction; a delayed sweep grants no execution. */
 public Result requireExecutableAllocation(DomainContext c,String allocationId,String capability,String action,String customerId){
  var allocation=r.current(c,"SegmentAllocations",allocationId);var segment=r.current(c,"QuantitySegments",allocation.get("segmentId").toString());r.fence(c,stock.fences(segment));policies.fence(c.organizationId());auth.fence(c,List.of(c.actorId()));allocation=r.current(c,"SegmentAllocations",allocationId);segment=r.current(c,"QuantitySegments",allocation.get("segmentId").toString());
  if(!Objects.equals(action,Objects.toString(allocation.get("action"),"SELL"))||!Objects.equals(customerId,allocation.get("customerId")))throw new DomainError("REJECTED","SCOPE_INELIGIBLE","Allocation action or customer differs");
  if("SUSPENDED".equals(allocation.get("state")))throw new DomainError("REJECTED","INSUFFICIENT_ELIGIBLE_QUANTITY","Suspended allocation is not executable");
  if(!"EXECUTABLE".equals(allocation.get("state")))throw new DomainError("CONFLICT","STALE_REVISION","Allocation terminal or replaced");
  var current=new DomainContext(c.organizationId(),c.actorId(),c.stableRequestOwner(),auth.now(),auth.now());var result=assess(current,segment,action,customerId);BigDecimal required=(BigDecimal)allocation.get("quantity");boolean enough=allocation.get("startQuantity") instanceof BigDecimal start?QualityRanges.quantity(QualityRanges.intersect(result.ranges(),List.of(new QualityRanges.Range(start,start.add(required))))).compareTo(required)>=0:"ALLOWED".equals(result.state())&&result.eligibleQuantity().compareTo(StockPrimitives.amount(segment))>=0;
  if(!enough)throw new DomainError("REJECTED","INSUFFICIENT_ELIGIBLE_QUANTITY","Current exact allocation intersection is insufficient");auth.authorizeScopes(c,capability,Map.of("TARGET",List.of((String)segment.get("ID")),"ITEM",List.of((String)segment.get("itemId")),"PLACE",List.of((String)segment.get("placeId"))));return result;
 }
 public QueryResult query(DomainContext c,QueryRequest q){
  if(!Set.of("action","customerId","itemId").containsAll(q.filters().keySet()))throw DomainError.invalid("Unsupported eligibility filter");
  if(q.scope().get("organizationId")!=null&&!c.organizationId().equals(q.scope().get("organizationId")))throw DomainError.forbidden();
  if(q.id()==null)return itemQuery(c,q);
  var segment=r.object(c,"QuantitySegments",q.id());
  auth.authorizeScopes(c,q.operation(),InventoryReadCandidates.segmentScopes(segment,null));
  String action=action(q);String customer=Objects.toString(q.filters().get("customerId"),null);
  var result=evaluate(c,segment,action,customer);
  return new QueryResult(segmentData(segment,action,result),q.scope(),result.unknowns(),List.of(),result.evidenceRefs(),null);
 }
 private String action(QueryRequest q){
  String action=Objects.toString(q.filters().getOrDefault("action","SELL"));
  if(!Set.of("SELL","DISPATCH","DISPOSE","INTERNAL_MOVE").contains(action))throw DomainError.invalid("Unsupported eligibility action");
  return action;
 }
 private Result evaluate(DomainContext c,Map<String,Object> segment,String action,String customer){
  return Set.of("SELL","DISPATCH").contains(action)?withCustody(c,segment,()->assess(c,segment,action,customer)):assess(c,segment,action,customer);
 }
 private Map<String,Object> segmentData(Map<String,Object> segment,String action,Result result){
  var data=new LinkedHashMap<String,Object>();
  data.putAll(Map.of("segmentId",segment.get("ID"),"itemId",segment.get("itemId"),"action",action,"unit",segment.get("unit"),"eligibilityStatus",result.state(),"conditions",result.conditions(),"allowedRanges",dto(result.ranges())));
  data.put("eligibleQuantity",result.unknowns().contains("CURRENT_ELIGIBILITY_POLICY_UNRESOLVED")?null:InventoryQuantity.text(result.eligibleQuantity()));
  data.put("nextValidityBoundary",result.nextValidityBoundary());return data;
 }
 /** An item query is a collection of independently evaluated physical ranges, never a guessed aggregate. */
 private QueryResult itemQuery(DomainContext c,QueryRequest q){
  if(!Set.of("organizationId","itemId","placeId","lotId","workId","customerId").containsAll(q.scope().keySet()))throw DomainError.invalid("Unsupported eligibility scope");
  var scope=new LinkedHashMap<String,Object>(q.scope());
  for(String key:List.of("itemId","customerId"))if(q.filters().containsKey(key)){
   if(scope.containsKey(key)&&!Objects.equals(scope.get(key),q.filters().get(key)))throw DomainError.invalid("Conflicting eligibility scope");
   scope.put(key,q.filters().get(key));
  }
  for(var value:scope.entrySet())if(!value.getKey().equals("organizationId")){
   if(!(value.getValue() instanceof String id))throw DomainError.invalid("Typed scope ID required");
   try{UUID.fromString(id);}catch(IllegalArgumentException invalid){throw DomainError.invalid("UUID scope ID required");}
  }
  if(!(scope.get("itemId") instanceof String item))throw DomainError.invalid("Item scope or segment ID required");
  String action=action(q),customer=(String)scope.get("customerId"),workId=(String)scope.get("workId");
  r.object(c,"TradeItems",item);
  // Work scope means its declared item/LOT subject, not its allocations or another customer's stock.
  Map<String,Object> work=null;List<Map<String,Object>> subjects=List.of();
  if(workId!=null){
   work=works.rows("Works",c).stream().filter(w->workId.equals(w.get("ID"))&&item.equals(w.get("itemId"))).findFirst().orElseThrow(DomainError::forbidden);
   subjects=works.rows("SubjectLinks",c).stream().filter(w->workId.equals(w.get("workId"))&&item.equals(w.get("itemId"))).toList();
  }
  var visible=new ArrayList<Map<String,Object>>();var unknowns=new TreeSet<String>();var refs=new TreeSet<String>();
  for(var segment:r.rows(c,"QuantitySegments").stream().sorted(Comparator.comparing(row->(String)row.get("ID"))).toList()){
   if(!item.equals(segment.get("itemId"))||!InventoryReadCandidates.active(segment,c)||!InventoryReadCandidates.matches(segment,scope))continue;
   if(work!=null&&work.get("lotId")!=null&&!Objects.equals(work.get("lotId"),segment.get("lotId"))&&subjects.stream().noneMatch(link->Objects.equals(link.get("lotId"),segment.get("lotId"))))continue;
   if(!auth.permittedScopes(c,q.operation(),InventoryReadCandidates.segmentScopes(segment,workId)))continue;
   var result=evaluate(c,segment,action,customer);var data=segmentData(segment,action,result);
   data.put("placeId",segment.get("placeId"));data.put("lotId",segment.get("lotId"));if(customer!=null)data.put("customerId",customer);
   data.put("unknowns",result.unknowns());data.put("evidenceRefs",result.evidenceRefs());
   visible.add(data);unknowns.addAll(result.unknowns());refs.addAll(result.evidenceRefs());
  }
  // A denied selection cannot be disguised as a successful empty result.
  if(visible.isEmpty()){
   var targets=new LinkedHashMap<String,Collection<String>>();targets.put("TARGET",List.of(item));targets.put("ITEM",List.of(item));
   if(scope.get("placeId") instanceof String place)targets.put("PLACE",List.of(place));if(workId!=null)targets.put("WORK",List.of(workId));
   auth.authorizeScopes(c,q.operation(),targets);
  }
  var data=new LinkedHashMap<String,Object>();data.put("itemId",item);data.put("action",action);
  data.put("segmentIds",visible.stream().map(row->row.get("segmentId")).toList());data.put("segments",List.copyOf(visible));
  return new QueryResult(data,scope,List.copyOf(unknowns),List.of(),List.copyOf(refs),null);
 }
}
