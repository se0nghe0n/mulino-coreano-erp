package com.mulino.application.inventory;
import com.mulino.application.core.*;
import com.mulino.application.quality.QualityEligibility;
import com.mulino.domain.inventory.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
/** Independent restrictions follow physical identity; exact disjoint siblings are unaffected. */
@Component
public final class InventoryRestrictionGuard {
 private final InventoryRepository repository;private final ExecutionClock clock;
 public InventoryRestrictionGuard(InventoryRepository repository){this.repository=repository;this.clock=null;}
 @org.springframework.beans.factory.annotation.Autowired
 public InventoryRestrictionGuard(InventoryRepository repository,ExecutionClock clock){this.repository=repository;this.clock=clock;}
 public void fence(DomainContext c,CommandPreparation p) { /* Gateway holds sorted inventory/control fences. */ }
 public void verify(DomainContext c,String capability,String hash,CommandPreparation p,Map<String,Object> intent) {
  if(!Set.of("INTERNAL_MOVE","ADJUSTMENT","DISPOSE").contains(p.effectClass()))return;
  var target=repository.current(c,"QuantitySegments",p.targetId());Instant now=clock==null?c.knownAt():clock.instant();String scope=(String)target.get("controlScope");
  var edges=repository.currentRows(c,"GenealogyEdges");
  for(var restriction:repository.currentRows(c,"Restrictions"))if(scope.equals(restriction.get("controlScope"))&&QualityEligibility.active(restriction,now,now)) {
   if(restriction.get("segmentId")!=null){String ancestor=(String)restriction.get("segmentId");var physical=repository.current(c,"QuantitySegments",ancestor);var whole=PhysicalRanges.project(edges,ancestor,(String)target.get("ID"),BigDecimal.ZERO,StockPrimitives.amount(physical));var exact=QualityEligibility.range(restriction);var overlap=PhysicalRanges.project(edges,ancestor,(String)target.get("ID"),exact.start(),exact.end().subtract(exact.start()));if(overlap.isEmpty()&&QualityRanges.quantity(whole).compareTo(StockPrimitives.amount(target))==0)continue;if(overlap.isEmpty()&&!ancestor.equals(target.get("ID"))&&!descendant(edges,ancestor,(String)target.get("ID")))continue;}
   if(Set.of("ADJUSTMENT","DISPOSE").contains(p.effectClass())||Set.of("ALL",p.effectClass(),capability).contains(restriction.get("action")))throw new DomainError("REJECTED","SCOPE_RESTRICTED","Current action restriction requires referenced release decision");
  }
 }
 private boolean descendant(List<Map<String,Object>> edges,String ancestor,String target){var reachable=new HashSet<String>(Set.of(ancestor));boolean changed;do{changed=false;for(var edge:edges)if(reachable.contains(edge.get("sourceId")))changed|=reachable.add((String)edge.get("targetId"));}while(changed);return reachable.contains(target);}
}
