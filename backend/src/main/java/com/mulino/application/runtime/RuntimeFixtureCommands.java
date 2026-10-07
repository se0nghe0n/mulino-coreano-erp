package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.runtime.RuntimeRepository;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Actual command/outbox fixture path, installed only for isolated verification. */
@Component
@Profile("verification")
public class RuntimeFixtureCommands implements CommandHandler {
 private final WorkAccess works;private final RuntimeRepository repository;
 public RuntimeFixtureCommands(WorkAccess works,RuntimeRepository repository){this.works=works;this.repository=repository;}
 public Set<String> capabilities(){return Set.of("fixtureExternalEffect");}
 public CommandPreparation prepare(DomainContext c,Map<String,Object> intent){
  var slots=slots(intent);String work=(String)slots.get("workId");var actual=works.require(c,work,false);
  if(!Set.of("ACTIVE","WAITING").contains(actual.get("status")))throw DomainError.invalid("Fixture requires active Work");
  return CommandPreparation.ordinary(Map.of("WORK",List.of(work)),List.of("work:"+work),"EXTERNAL_FIXTURE",work,((Number)actual.get("revision")).intValue());
 }
 public List<SubjectBinding> subjectBindings(DomainContext c,Map<String,Object> intent,CommandPreparation prep){return List.of(SubjectBinding.optional("Work",Set.of((String)slots(intent).get("workId"))));}
 public Map<String,Object> execute(DomainContext c,Map<String,Object> intent){
  var payload=slots(intent);String command=CommandExecution.commandId();String external=UUID.nameUUIDFromBytes((c.organizationId()+":"+command+":fixtureExternalEffect").getBytes(StandardCharsets.UTF_8)).toString();
  String outbox=repository.enqueue(c,command,external,"fixtureExternalEffect",payload);
  return Map.of("outcome","ACCEPTED_PENDING_EXTERNAL","effects",Map.of("workId",payload.get("workId"),"outboxId",outbox,"externalOperationId",external));
 }
 private Map<String,Object> slots(Map<String,Object> intent){
  if(!(intent.get("slots") instanceof Map<?,?> raw)||!Set.of("workId","dropResponse").containsAll(raw.keySet()))throw DomainError.invalid("Unsupported fixture slots");
  var result=new LinkedHashMap<String,Object>();raw.forEach((k,v)->result.put(k.toString(),v instanceof Map<?,?> t?t.containsKey("id")?t.get("id"):t.get("value"):v));
  if(!(result.get("workId") instanceof String id))throw DomainError.invalid("Actual fixture Work required");CommandRequests.uuid(id);
  if(result.containsKey("dropResponse")&&!(result.get("dropResponse") instanceof Boolean))throw DomainError.invalid("Boolean fixture response control required");
  result.putIfAbsent("dropResponse",false);return result;
 }
}
