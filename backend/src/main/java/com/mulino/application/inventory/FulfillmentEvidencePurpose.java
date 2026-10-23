package com.mulino.application.inventory;

import com.mulino.application.core.*;
import com.mulino.application.trade.SalesOrderLinePort;
import com.mulino.domain.inventory.*;
import java.math.BigDecimal;
import java.util.*;

/** Server-only witness for one supported fulfillment operation, physical target and sales line.
 * The package-private factory is called by fulfillment preparation, including before runtime
 * command fences. Every use re-reads the same authoritative context and current grants; it
 * neither grants a public evidence read nor substitutes for the gateway's policy/commit fences.
 */
public final class FulfillmentEvidencePurpose {
  private final DomainContext identity;
  private final String capability,segmentId,lineId,itemId,placeId,workId,customerId,unit;
  private final InventoryRepository inventory;
  private final SalesOrderLinePort sales;
  private final ReadAuthorizer auth;
  private final Set<Map<String,Collection<String>>> admittedSourceScopes=new LinkedHashSet<>();

  private FulfillmentEvidencePurpose(DomainContext c,String capability,Map<String,Object> segment,
      Map<String,Object> line,InventoryRepository inventory,SalesOrderLinePort sales,ReadAuthorizer auth) {
    this.identity=c;this.capability=capability;this.segmentId=(String)segment.get("ID");
    this.lineId=(String)line.get("ID");this.itemId=(String)segment.get("itemId");
    this.placeId=(String)segment.get("placeId");this.workId=(String)line.get("workId");
    this.customerId=(String)line.get("customerId");this.unit=(String)segment.get("unit");
    this.inventory=inventory;this.sales=sales;this.auth=auth;
  }
  static FulfillmentEvidencePurpose authorize(DomainContext c,String capability,String segmentId,
      String lineId,InventoryRepository inventory,SalesOrderLinePort sales,ReadAuthorizer auth) {
    if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Fulfillment evidence requires the preparation/command transaction");
    if(!Set.of("reserveQuantity","replaceAllocation","pickQuantity","dispatchQuantity").contains(capability))throw DomainError.forbidden();
    var purpose=new FulfillmentEvidencePurpose(c,capability,inventory.current(c,"QuantitySegments",segmentId),sales.requireLine(c,lineId),inventory,sales,auth);
    purpose.revalidate(c);
    track(purpose);
    return purpose;
  }
  private static final Object TRANSACTION_PROOFS=new Object();
  @SuppressWarnings("unchecked")
  private static void track(FulfillmentEvidencePurpose purpose){
    var proofs=(List<FulfillmentEvidencePurpose>)org.springframework.transaction.support.TransactionSynchronizationManager.getResource(TRANSACTION_PROOFS);
    if(proofs==null){
      proofs=new ArrayList<>();org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(TRANSACTION_PROOFS,proofs);
      org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){
        @Override public void afterCompletion(int status){org.springframework.transaction.support.TransactionSynchronizationManager.unbindResourceIfPossible(TRANSACTION_PROOFS);}
      });
    }
    proofs.add(purpose);
  }
  /** Called after domain effects, inside the handler before the gateway's final policy check.
   * Current grant revocation is fenced; this final read also catches elapsed source grants.
   * Throwing here preserves the command's domain envelope and rolls back all its effects.
   */
  @SuppressWarnings("unchecked")
  static void verifyCurrentSources(DomainContext c){
    var proofs=(List<FulfillmentEvidencePurpose>)org.springframework.transaction.support.TransactionSynchronizationManager.getResource(TRANSACTION_PROOFS);
    if(proofs==null)return;
    for(var purpose:proofs)if(!purpose.admittedSourceScopes.isEmpty()){
      purpose.revalidate(c);
      for(var scope:purpose.admittedSourceScopes)purpose.auth.authorizeScopes(c,purpose.capability,scope);
    }
  }
  /** Only fulfillment's FEFO selection may mint another exact, currently authorized target. */
  FulfillmentEvidencePurpose forSegment(DomainContext c,String id) {
    return authorize(c,capability,id,lineId,inventory,sales,auth);
  }
  public void requireAssessment(DomainContext c,Map<String,Object> segment,String action,String customer) {
    requirePhysicalAssessment(c,(String)segment.get("ID"),action);
    if(!Objects.equals(customerId,customer))throw DomainError.forbidden();
  }
  public void requirePhysicalAssessment(DomainContext c,String physicalScopeId,String action) {
    revalidate(c);
    if(!segmentId.equals(physicalScopeId)||!("SELL".equals(action)||"DISPATCH".equals(action)&&"dispatchQuantity".equals(capability)))throw DomainError.forbidden();
  }
  private void revalidate(DomainContext c) {
    if(!identity.organizationId().equals(c.organizationId())||!identity.actorId().equals(c.actorId())
        ||!identity.stableRequestOwner().equals(c.stableRequestOwner()))throw DomainError.forbidden();
    var segment=inventory.current(c,"QuantitySegments",segmentId);var line=sales.requireLine(c,lineId);
    if(!itemId.equals(segment.get("itemId"))||!placeId.equals(segment.get("placeId"))
        ||!unit.equals(segment.get("unit"))||!itemId.equals(line.get("itemId"))
        ||!unit.equals(line.get("unit"))||!workId.equals(line.get("workId"))
        ||!customerId.equals(line.get("customerId"))||!Boolean.TRUE.equals(line.get("currentRevision")))throw DomainError.forbidden();
    auth.authorizeScopes(c,capability,scopes(workId,null,placeId));
  }
  /** The exact canonical physical range, not an unrelated same-org object, supplies the graph context. */
  public boolean permitsCanonical(DomainContext c,Map<String,Object> canonical) {
    revalidate(c);
    if(!c.organizationId().equals(canonical.get("organizationId"))||!itemId.equals(canonical.get("itemId"))
        ||!(canonical.get("physicalScopeId") instanceof String source))return false;
    var ancestor=inventory.object(c,"QuantitySegments",source);
    if(!itemId.equals(ancestor.get("itemId"))||!unit.equals(ancestor.get("unit")))return false;
    if(PhysicalRanges.project(inventory.rows(c,"GenealogyEdges"),source,segmentId,BigDecimal.ZERO,StockPrimitives.amount(ancestor)).isEmpty())return false;
    return permitsSource(c,canonical);
  }
  /** Source WORK/PLACE/SOURCE dimensions are additionally checked with the same command,
   * never OR'ed with public getEvidence. TARGET always remains this affected physical target.
   */
  public boolean permitsSource(DomainContext c,Map<String,Object> row) {
    revalidate(c);
    if(!c.organizationId().equals(row.get("organizationId"))||!itemId.equals(row.get("itemId")))return false;
    String sourceWork=Objects.toString(row.get("workId"),workId);
    String sourcePlace=Objects.toString(row.get("placeId"),placeId);
    var sourceScopes=scopes(sourceWork,Objects.toString(row.get("sourceProfileId"),null),sourcePlace);
    if(!auth.permittedScopes(c,capability,sourceScopes))return false;
    admittedSourceScopes.add(sourceScopes);return true;
  }
  private Map<String,Collection<String>> scopes(String work,String source,String place) {
    var scopes=new LinkedHashMap<String,Collection<String>>();scopes.put("TARGET",List.of(segmentId));
    scopes.put("ITEM",List.of(itemId));scopes.put("PLACE",List.of(place));scopes.put("WORK",List.of(work));
    if(source!=null)scopes.put("SOURCE",List.of(source));return scopes;
  }
}
