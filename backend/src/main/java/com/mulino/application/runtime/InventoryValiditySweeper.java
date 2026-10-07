package com.mulino.application.runtime;
import com.mulino.application.core.*;
import com.mulino.application.quality.QualityEligibility;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.application.policy.PolicyCommandGuard;
import com.mulino.application.trade.TradeImpact;
import com.mulino.domain.inventory.*;
import com.mulino.domain.identity.IdentityRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
/** No event is needed: recheck current fences, suspend future execution, preserve duties. */
@Component
public class InventoryValiditySweeper implements ValidityBoundarySweep {
 private final JdbcTemplate jdbc;private final InventoryRepository r;private final StockPrimitives stock;private final QualityPrimitives primitive;private final QualityEligibility eligibility;private final IdentityAuthorization auth;private final IdentityRepository identities;private final PolicyCommandGuard policy;private final TradeImpact impact;private final ExecutionClock clock;private final TransactionTemplate tx;private final com.mulino.domain.governance.PolicyRepository policies;private final CommandRepository commands;
 public InventoryValiditySweeper(JdbcTemplate jdbc,InventoryRepository r,StockPrimitives stock,QualityPrimitives primitive,QualityEligibility eligibility,IdentityAuthorization auth,IdentityRepository identities,PolicyCommandGuard policy,TradeImpact impact,ExecutionClock clock,PlatformTransactionManager manager,com.mulino.domain.governance.PolicyRepository policies,CommandRepository commands){this.jdbc=jdbc;this.r=r;this.stock=stock;this.primitive=primitive;this.eligibility=eligibility;this.auth=auth;this.identities=identities;this.policy=policy;this.impact=impact;this.clock=clock;this.tx=new TransactionTemplate(manager);this.policies=policies;this.commands=commands;}
 public int sweep(){int changed=0;for(var row:jdbc.queryForList("SELECT organizationId,id FROM mulino_inventory_SegmentAllocations WHERE state='EXECUTABLE' ORDER BY organizationId,id LIMIT 1000")){Boolean applied=tx.execute(status->check(row.get("organizationid").toString(),row.get("id").toString()));if(Boolean.TRUE.equals(applied))changed++;}return changed;}
 private boolean check(String org,String allocationId){
  // Resolving the persisted owner is only a read context, never a system grant.
  var seed=new DomainContext(org,"", "",clock.instant(),clock.instant());var allocation=r.current(seed,"SegmentAllocations",allocationId);String actorId=Objects.toString(allocation.get("authorizationActorId"),null);var actor=actorId==null?null:identities.actor(org,actorId).orElse(null);var c=new DomainContext(org,actorId==null?"":actorId,actor==null?"":Objects.toString(actor.get("stableRequestOwner"),""),clock.instant(),clock.instant());
  var segment=r.current(c,"QuantitySegments",(String)allocation.get("segmentId"));r.fence(c,stock.fences(segment));policies.fence(org);if(actorId!=null)auth.fence(c,List.of(actorId));allocation=r.current(c,"SegmentAllocations",allocationId);if(!"EXECUTABLE".equals(allocation.get("state")))return false;
  String action=Objects.toString(allocation.get("action"),"SELL");var result=eligibility.assess(c,segment,action,Objects.toString(allocation.get("customerId"),null));boolean enough;
  if(allocation.get("startQuantity") instanceof BigDecimal start)enough=QualityRanges.quantity(QualityRanges.intersect(result.ranges(),List.of(new QualityRanges.Range(start,start.add((BigDecimal)allocation.get("quantity")))))).compareTo((BigDecimal)allocation.get("quantity"))>=0;
  else enough="ALLOWED".equals(result.state())&&result.eligibleQuantity().compareTo(StockPrimitives.amount(segment))>=0;
  var scopes=Map.of("TARGET",List.of((String)segment.get("ID")),"ITEM",List.of((String)segment.get("itemId")),"PLACE",List.of((String)segment.get("placeId")));
  boolean permitted=actor!=null&&auth.permittedScopes(c,"reserveQuantity",scopes);try{var rule=policy.rule(c,"reserveQuantity");permitted&="RESERVE".equals(rule.rule().get("effectClass"));}catch(DomainError missing){permitted=false;}
  if(enough&&permitted)return false;
  String workId=Objects.toString(allocation.get("workId"),null),source=Objects.toString(allocation.get("commandId"),null);
  if(workId==null){var basis=r.currentRows(c,"DispositionBases").stream().filter(b->segment.get("ID").equals(b.get("segmentId"))&&"COMMERCIAL".equals(b.get("category"))&&b.get("workId")!=null).findFirst().orElseThrow(()->DomainError.invalid("Allocation lacks persisted responsibility scope"));workId=basis.get("workId").toString();source=basis.get("commandId").toString();}
  primitive.suspend(c,(String)segment.get("ID"),permitted?"CURRENT_ELIGIBILITY_EXPIRED":"CURRENT_AUTHORITY_DENIED");
  impact.recorded(c,workId,allocationId+":validity:"+Objects.toString(allocation.get("nextValidityBoundary"),"current")+":"+allocation.get("revision"),"INVENTORY_VALIDITY",(String)segment.get("ID"),"Review suspended allocation and replace current authority or stock",clock.instant().plusSeconds(60),source);
  commands.audit(c,source,Map.of("capabilityId","inventoryValidityBoundary"),CommandRequests.hash(Map.of("allocationId",allocationId,"beforeRevision",allocation.get("revision"))),Map.of("outcome","APPLIED","revision",((Number)allocation.get("revision")).intValue()+1),clock.instant(),Map.of("allocationId",allocationId,"currentEligibility",result.state(),"authorityPermitted",permitted,"sourceCommandId",source,"workId",workId));
  return true;
 }
}
