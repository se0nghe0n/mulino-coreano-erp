package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.domain.runtime.RuntimeRepository;
import java.util.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RuntimeCommands implements CommandHandler {
 private final RuntimeService service;private final RuntimeRepository repository;private final ObjectMapper json=new ObjectMapper();
 public RuntimeCommands(RuntimeService service,RuntimeRepository repository){this.service=service;this.repository=repository;}
 public Set<String> capabilities(){return Set.of("recordExternalReconciliation");}
 public Set<String> intentKinds(){return Set.of("RECORD","COMMAND");}
 public CommandPreparation prepare(DomainContext c,Map<String,Object> intent){
  var slots=slots(intent);String id=text(slots,"outboxId");UUID.fromString(id);
  var rows=repository.db().queryForList("SELECT * FROM mulino_runtime_Outbox WHERE organizationId=? AND ID=?",c.organizationId(),id);if(rows.size()!=1)throw DomainError.forbidden();var row=rows.getFirst();
  String work=json.readValue((String)row.get("payloadjson"),Map.class).get("workId").toString();
  if(!Set.of("CONFIRMED_SUCCESS","CONFIRMED_FAILURE","UNRESOLVED").contains(text(slots,"decision")))throw DomainError.invalid("Invalid reconciliation decision");text(slots,"evidenceId");
  return CommandPreparation.ordinary(Map.of("WORK",List.of(work),"TARGET",List.of(id)),List.of("external-operation:"+row.get("externaloperationid"),"work:"+work),"RECONCILIATION",id,null);
 }
 public Map<String,Object> execute(DomainContext c,Map<String,Object> intent){
  var slots=slots(intent);String decision=text(slots,"decision");var result=decision.equals("CONFIRMED_SUCCESS")?ExternalDeliveryAdapter.Result.CONFIRMED_SUCCESS:decision.equals("CONFIRMED_FAILURE")?ExternalDeliveryAdapter.Result.CONFIRMED_FAILURE:ExternalDeliveryAdapter.Result.UNKNOWN;
  service.recordExternalReconciliation(c,text(slots,"outboxId"),result,text(slots,"evidenceId"));
  return Map.of("outcome","APPLIED","effects",Map.of("outboxId",text(slots,"outboxId"),"reconciliationDecision",decision));
 }
 private Map<String,Object> slots(Map<String,Object> intent){if(!(intent.get("slots") instanceof Map<?,?> raw)||!Set.of("outboxId","decision","evidenceId").containsAll(raw.keySet()))throw DomainError.invalid("Unsupported reconciliation slots");var s=new LinkedHashMap<String,Object>();raw.forEach((k,v)->s.put(k.toString(),v instanceof Map<?,?> typed?typed.containsKey("value")?typed.get("value"):typed.get("id"):v));return s;}
 private String text(Map<String,Object> slots,String key){if(!(slots.get(key) instanceof String v)||v.isBlank())throw DomainError.invalid("Missing reconciliation slot");return v;}
}
