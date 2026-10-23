package com.mulino.application.trade.sales;

import com.mulino.application.core.*;
import com.mulino.domain.evaluation.CanonicalFactAdmission.Purpose;
import com.mulino.domain.trade.sales.SalesRepository;
import java.util.*;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Server-only proof of the one command whose applied delivery caused this reevaluation.
 * The factory is package-private; neither an enum supplied by a client nor READ mints it. */
public final class DeliveryRecordCause {
 private final DomainContext identity;
 private final Purpose purpose;
 private final SalesRepository sales;
 private final ReadAuthorizer auth;
 private final Map<String,Object> cause;
 private final Set<Map<String,Collection<String>>> sources=new LinkedHashSet<>();
 private DeliveryRecordCause(DomainContext c,Purpose purpose,SalesRepository sales,ReadAuthorizer auth,String work){
  if(!purpose.causalRecord()||!TransactionSynchronizationManager.isActualTransactionActive())throw DomainError.forbidden();
  this.identity=c;this.purpose=purpose;this.sales=sales;this.auth=auth;this.cause=sales.currentRecordCause(c,work,purpose.capability());revalidate(c);
 }
 static Optional<DeliveryRecordCause> authorize(DomainContext c,Purpose purpose,SalesRepository sales,ReadAuthorizer auth,String work){
  try{var witness=new DeliveryRecordCause(c,purpose,sales,auth,work);track(witness);return Optional.of(witness);}
  catch(DomainError|IllegalStateException unavailable){return Optional.empty();}
 }
 private void revalidate(DomainContext c){
  if(!identity.organizationId().equals(c.organizationId())||!identity.actorId().equals(c.actorId())||!identity.stableRequestOwner().equals(c.stableRequestOwner())||!cause.get("commandId").equals(CommandExecution.commandId()))throw DomainError.forbidden();
  var current=sales.currentRecordCause(c,cause.get("workId").toString(),purpose.capability());
  for(String key:List.of("ID","observationId","canonicalOccurrenceId","dispatchId","segmentId","workId","itemId","placeId","commandId"))if(!Objects.equals(cause.get(key),current.get(key)))throw DomainError.forbidden();
  auth.authorizeScopes(c,purpose.capability(),scopes(cause,cause.get("observationId").toString()));
 }
 /** Past deliveries of this Work may contribute only under their own current source scope. */
 public boolean permitsCanonical(DomainContext c,Map<String,Object> fact){
  revalidate(c);
  return "PHYSICAL_DELIVERY".equals(fact.get("kind"))&&cause.get("workId").equals(fact.get("workId"))&&permitsSource(c,fact);
 }
 public boolean permitsSource(DomainContext c,Map<String,Object> row){
  revalidate(c);
  if(!c.organizationId().equals(row.get("organizationId"))||!cause.get("itemId").equals(row.get("itemId"))||row.get("workId")!=null&&!cause.get("workId").equals(row.get("workId")))return false;
  var scope=scopes(row,cause.get("observationId").toString());
  if(!auth.permittedScopes(c,purpose.capability(),scope))return false;sources.add(scope);return true;
 }
 private Map<String,Collection<String>> scopes(Map<String,Object> row,String target){
  var scope=new LinkedHashMap<String,Collection<String>>();scope.put("TARGET",List.of(target));scope.put("WORK",List.of(Objects.toString(row.get("workId"),cause.get("workId").toString())));scope.put("ITEM",List.of(cause.get("itemId").toString()));scope.put("PLACE",List.of(Objects.toString(row.get("placeId"),cause.get("placeId").toString())));if(row.get("sourceProfileId")!=null)scope.put("SOURCE",List.of(row.get("sourceProfileId").toString()));return scope;
 }
 private static final Object PROOFS=new Object();
 @SuppressWarnings("unchecked") private static void track(DeliveryRecordCause witness){
  var proofs=(List<DeliveryRecordCause>)TransactionSynchronizationManager.getResource(PROOFS);
  if(proofs==null){proofs=new ArrayList<>();TransactionSynchronizationManager.bindResource(PROOFS,proofs);TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){TransactionSynchronizationManager.unbindResourceIfPossible(PROOFS);}});}proofs.add(witness);
 }
 @SuppressWarnings("unchecked") static void verifyCurrentSources(DomainContext c){
  var proofs=(List<DeliveryRecordCause>)TransactionSynchronizationManager.getResource(PROOFS);if(proofs==null)return;
  for(var proof:proofs){proof.revalidate(c);for(var scope:proof.sources)proof.auth.authorizeScopes(c,proof.purpose.capability(),scope);}
 }
}
