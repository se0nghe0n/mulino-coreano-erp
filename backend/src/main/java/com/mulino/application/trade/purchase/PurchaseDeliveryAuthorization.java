package com.mulino.application.trade.purchase;

import com.mulino.application.core.*;
import com.mulino.application.runtime.ExternalDeliveryAuthorization;
import java.util.*;
import org.springframework.stereotype.Component;

/** A real purchase authorization guard, independent of which external adapter is configured. */
@Component
public final class PurchaseDeliveryAuthorization implements ExternalDeliveryAuthorization {
 private final CommandRepository commands;
 private final PurchaseCommands purchases;
 private final CommandDefinitionResolver definitions;
 private final CommandGuard guard;
 public PurchaseDeliveryAuthorization(CommandRepository commands,PurchaseCommands purchases,
      CommandDefinitionResolver definitions,CommandGuard guard){
  this.commands=commands;this.purchases=purchases;this.definitions=definitions;this.guard=guard;
 }
 public boolean supports(String operation){return "dispatchPurchaseOrder".equals(operation);}
 public void require(DomainContext c,Delivery delivery){
  if(!supports(delivery.operation())||!c.actorId().equals(delivery.actorId())||!c.stableRequestOwner().equals(delivery.stableRequestOwner()))throw DomainError.forbidden();
  var record=commands.owned(c,delivery.commandId()).orElseThrow(DomainError::forbidden);
  if(!"COMMITTED".equals(record.get("state")))throw DomainError.forbidden();
  var original=commands.original(record);var result=commands.result(record);
  CommandRequests.parse(original,true);
  String hash=CommandRequests.hash(original),cap=delivery.operation();
  for(String key:List.of("capabilityId","capabilityVersion","definitionVersion","commandIdempotencyKey"))if(!Objects.equals(record.get(key),original.get(key)))throw DomainError.forbidden();
  if(!cap.equals(original.get("capabilityId"))||!purchases.semanticVersion().equals(original.get("capabilityVersion"))||!hash.equals(record.get("canonicalHash"))||!hash.equals(result.get("canonicalIntentHash"))||!delivery.commandId().equals(result.get("commandId"))||!"APPLIED".equals(result.get("outcome")))throw DomainError.forbidden();
  commands.fence(c,List.of("command:"+c.stableRequestOwner()+":"+cap+":"+original.get("commandIdempotencyKey")));
  var fenced=commands.owned(c,delivery.commandId()).orElseThrow(DomainError::forbidden);
  for(String field:List.of("state","canonicalHash","canonicalIntentJson","resultJson","actorId","stableRequestOwner","commandIdempotencyKey","capabilityId","definitionVersion","capabilityVersion"))if(!Objects.equals(record.get(field),fenced.get(field)))throw DomainError.forbidden();
  var effects=result.get("effects") instanceof Map<?,?> map?map:Map.of();
  for(String field:List.of("orderId","poLineId","outboxId"))if(!Objects.equals(result.get(field),effects.get(field)))throw DomainError.forbidden();
  if(!Objects.equals(result.get("orderId"),effects.get("purchaseId")))throw DomainError.forbidden();
  var initial=purchases.prepare(c,original);
  commands.fence(c,initial.fenceKeys());guard.fence(c,initial);
  var current=purchases.prepare(c,original);
  if(!initial.equals(current))throw new DomainError("CONFLICT","STALE_REVISION","Purchase delivery scope changed");
  definitions.verify(c,original,purchases);
  // Preserve purchase approval causes before a revised item also invalidates old optional subjects.
  purchases.verifyDelivery(c,original,result,delivery);
  definitions.verifySubjects(c,original,purchases,current);
  if(!commands.scopes(record).equals(current.scopes()))throw DomainError.forbidden();
  guard.verify(c,cap,hash,current,original);
 }
}
