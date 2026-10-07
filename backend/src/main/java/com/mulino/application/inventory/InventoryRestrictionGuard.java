package com.mulino.application.inventory;
import com.mulino.application.core.*;
import com.mulino.domain.inventory.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
/** A restriction is independent of custody/ownership and does not disappear by splitting. */
@Component
public final class InventoryRestrictionGuard {
 private final InventoryRepository repository;
 public InventoryRestrictionGuard(InventoryRepository repository){this.repository=repository;}
 public void fence(DomainContext c,CommandPreparation p) { /* Gateway holds sorted inventory/control fences. */ }
 public void verify(DomainContext c,String capability,String hash,CommandPreparation p,Map<String,Object> intent) {
  if(!Set.of("INTERNAL_MOVE","ADJUSTMENT","DISPOSE").contains(p.effectClass()))return;
  var target=repository.current(c,"QuantitySegments",p.targetId());Instant now=c.knownAt();String scope=(String)target.get("controlScope");
  for(var restriction:repository.currentRows(c,"Restrictions"))if(scope.equals(restriction.get("controlScope"))&&"ACTIVE".equals(restriction.get("state"))&&!StockPrimitives.instant(restriction.get("validFrom")).isAfter(now)&&(restriction.get("validUntil")==null||now.isBefore(StockPrimitives.instant(restriction.get("validUntil"))))) {
   if(Set.of("ADJUSTMENT","DISPOSE").contains(p.effectClass())||Set.of("ALL",p.effectClass(),capability).contains(restriction.get("action")))throw new DomainError("REJECTED","SCOPE_RESTRICTED","Current action restriction requires referenced release decision");
  }
 }
}
