package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.application.work.WorkAccess;
import java.time.Duration;
import java.util.*;
import org.springframework.stereotype.Service;

/** All assessment/resume mutations use the ordinary fenced command pipeline. */
@Service
public class RuntimeRecoveryExecution {
 private final ApplicationCommands commands;private final RuntimeService runtime;private final WorkAccess works;
 public RuntimeRecoveryExecution(ApplicationCommands commands,RuntimeService runtime,WorkAccess works){this.commands=commands;this.runtime=runtime;this.works=works;}
 public Map<String,Object> execute(DomainContext c,Map<String,Object> work,String capability,String key){
  String id=(String)work.get("ID");var lease=runtime.claim(c,id,capability,key,"durable-recovery",Duration.ofSeconds(5));
  if(lease.isEmpty())return Map.of("outcome","BUSY");
  var slots=new LinkedHashMap<String,Object>();slots.put("workId",Map.of("type","WORK","id",id));if(capability.equals("resumeWork"))slots.put("evidence",Map.of());
  var intent=new LinkedHashMap<String,Object>();intent.put("intentKind","COMMAND");intent.put("capabilityVersion","1.0.0");intent.put("subjectRefs",List.of());var provenance=new LinkedHashMap<String,String>();slots.keySet().forEach(k->provenance.put(k,"CONTEXT"));intent.put("provenance",provenance);intent.put("capabilityId",capability);intent.put("definitionVersion",work.get("definitionVersionId"));intent.put("commandIdempotencyKey",key);intent.put("expectedRevision",work.get("revision"));intent.put("slots",slots);
  var result=commands.executeClaimed(intent,lease.get());
  try{runtime.finish(c,lease.get(),"APPLIED".equals(result.get("outcome"))?"SUCCEEDED":"HELD","RECOVERY_COMMAND_RESULT");}catch(DomainError expired){return Map.of("outcome","STALE_EXECUTION");}
  return result;
 }
}
