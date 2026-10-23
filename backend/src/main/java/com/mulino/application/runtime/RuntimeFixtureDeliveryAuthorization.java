package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.runtime.RuntimeRepository;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Authorizes only the independent fixture effect, and exists only in the verification profile. */
@Component
@Profile("verification")
public class RuntimeFixtureDeliveryAuthorization implements ExternalDeliveryAuthorization {
 private final RuntimeRepository repository;private final IdentityAuthorization auth;private final CommandGuard guard;private final WorkAccess works;private final ObjectMapper json=new ObjectMapper();
 public RuntimeFixtureDeliveryAuthorization(RuntimeRepository repository,IdentityAuthorization auth,CommandGuard guard,WorkAccess works){this.repository=repository;this.auth=auth;this.guard=guard;this.works=works;}
 public boolean supports(String operation){return "fixtureExternalEffect".equals(operation);}
 public void require(DomainContext c,ExternalDeliveryAuthorization.Delivery delivery){
  String commandId=delivery.commandId(),operation=delivery.operation();Map<String,Object> payload=delivery.payload();
  if(!c.actorId().equals(delivery.actorId())||!c.stableRequestOwner().equals(delivery.stableRequestOwner()))throw DomainError.forbidden();
  RuntimeRepository.transaction();if(!operation.equals("fixtureExternalEffect")||!(payload.get("workId") instanceof String work))throw DomainError.unsupported();
  var rows=repository.db().queryForList("SELECT * FROM mulino_commands_CommandRecords WHERE organizationId=? AND ID=?",c.organizationId(),commandId);if(rows.size()!=1)throw DomainError.forbidden();var record=rows.getFirst();
  if(!"COMMITTED".equals(record.get("state"))||!c.actorId().equals(record.get("actorid"))||!c.stableRequestOwner().equals(record.get("stablerequestowner"))||record.get("canonicalintentjson")==null)throw DomainError.forbidden();
  var original=json.readValue((String)record.get("canonicalintentjson"),Map.class);var result=json.readValue((String)record.get("resultjson"),Map.class);
  if(!Objects.equals(record.get("canonicalhash"),result.get("canonicalIntentHash"))||!commandId.equals(result.get("commandId")))throw DomainError.forbidden();
  var resultEffects=result.get("effects") instanceof Map<?,?> map?map:Map.of();
  var expectedPayload=new LinkedHashMap<String,Object>();
  if(!(original.get("slots") instanceof Map<?,?> declared))throw DomainError.forbidden();
  declared.forEach((k,v)->expectedPayload.put(k.toString(),v instanceof Map<?,?> typed?typed.containsKey("id")?typed.get("id"):typed.get("value"):v));expectedPayload.putIfAbsent("dropResponse",false);
  String external=UUID.nameUUIDFromBytes((c.organizationId()+":"+commandId+":fixtureExternalEffect").getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
  if(!delivery.outboxId().equals(resultEffects.get("outboxId"))||!delivery.externalOperationId().equals(external)||!external.equals(resultEffects.get("externalOperationId"))||!CommandRequests.hash(payload).equals(CommandRequests.hash(expectedPayload)))throw DomainError.forbidden();
  Object originalWork=original.get("slots") instanceof Map<?,?> slots?slots.get("workId"):null;if(originalWork instanceof Map<?,?> typed)originalWork=typed.containsKey("id")?typed.get("id"):typed.get("value");
  Object resultWork=result.get("effects") instanceof Map<?,?> effects?effects.get("workId"):null;
  if(!Objects.equals(record.get("capabilityid"),original.get("capabilityId"))||!Objects.equals(record.get("canonicalhash"),CommandRequests.hash(original))||!work.equals(originalWork)&&!work.equals(resultWork))throw DomainError.forbidden();
  var prep=CommandPreparation.ordinary(Map.of("WORK",List.of(work)),List.of("work:"+work),"EXTERNAL_FIXTURE",work,null);
  guard.fence(c,prep);auth.authorizeScope(c,(String)record.get("capabilityid"),"WORK",work);guard.verify(c,operation,CommandRequests.hash(payload),prep,original);
  var actual=works.require(c,work,true);if(!Set.of("ACTIVE","WAITING").contains(actual.get("status")))throw DomainError.invalid("Fixture external effect needs current active Work");
 }
}
