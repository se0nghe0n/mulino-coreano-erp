package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.identity.IdentityRepository;
import java.util.*;
import org.springframework.stereotype.Service;

/** Due time triggers current commands; it never invents approval or arrival. */
@Service
public class RuntimeRecoverySweeper {
 private final RuntimeService runtime;private final WorkAccess works;private final ExecutionClock clock;private final IdentityRepository identities;private final RuntimeRecoveryExecution execution;private final List<ValidityBoundarySweep> validity;
 public RuntimeRecoverySweeper(RuntimeService runtime,WorkAccess works,ExecutionClock clock,IdentityRepository identities,RuntimeRecoveryExecution execution){this(runtime,works,clock,identities,execution,List.of());}
 @org.springframework.beans.factory.annotation.Autowired
 public RuntimeRecoverySweeper(RuntimeService runtime,WorkAccess works,ExecutionClock clock,IdentityRepository identities,RuntimeRecoveryExecution execution,List<ValidityBoundarySweep> validity){this.runtime=runtime;this.works=works;this.clock=clock;this.identities=identities;this.execution=execution;this.validity=List.copyOf(validity);}
 public int sweep(){
  runtime.recoverExpiredExecutions();runtime.captureControlInvalidations();
  var due=runtime.dueSchedules(100);int changed=0;for(var boundary:validity)changed+=boundary.sweep();
  for(var schedule:due){
   String org=(String)schedule.get("organizationid"),owner=(String)schedule.get("ownerid"),workId=(String)schedule.get("workid");
   var actor=identities.actor(org,owner).orElse(null);
   if(actor==null){hold(schedule,"OWNER_UNAVAILABLE","Supervisor must restore a human owner or authorize reassignment",null);continue;}
   var c=new DomainContext(org,owner,(String)actor.get("stableRequestOwner"),clock.instant(),clock.instant());
   var work=works.require(c,workId,false);
   if("CLOSED".equals(work.get("status"))&&!Boolean.TRUE.equals(work.get("pendingInvalidation"))){runtime.removeSchedule(c,workId,(String)schedule.get("kind"));continue;}
   String key="recovery:"+schedule.get("id")+":"+schedule.get("revision");
   Map<String,Object> assessment;
   try{assessment=execution.execute(c,work,"assessGoal",key+":assessment");}
   catch(org.springframework.security.access.AccessDeniedException|DomainError denied){hold(schedule,"CURRENT_AUTHORITY_DENIED","Supervisor must review current owner authority and pending assessment",c);changed++;continue;}
   if("BUSY".equals(assessment.get("outcome")))continue;
   if(!"APPLIED".equals(assessment.get("outcome"))){hold(schedule,"ASSESSMENT_HELD","Review pinned evaluator, policy and required evidence",c);changed++;continue;}
   work=works.require(c,workId,false);
   if("WAITING".equals(work.get("status"))){
    Map<String,Object> resumed;
    try{resumed=execution.execute(c,work,"resumeWork",key+":resume");}catch(org.springframework.security.access.AccessDeniedException|DomainError denied){resumed=Map.of("outcome","HELD");}
    if("BUSY".equals(resumed.get("outcome")))continue;
    if(!"APPLIED".equals(resumed.get("outcome"))){hold(schedule,"WAIT_PREDICATE_UNVERIFIED","Owner must verify waiting predicate; elapsed time is not approval or arrival",c);changed++;continue;}
   }
   runtime.completedRecovery(schedule,"RECHECKED");changed++;
  }
  return changed;
 }
 private void hold(Map<String,Object> schedule,String outcome,String action,DomainContext c){
  boolean linked=false;if(c!=null)try{linked=runtime.linkRecoveryDuty(c,(String)schedule.get("workid"),action);}catch(DomainError unavailable){/* Rollback completed before recording the manual gap. */}
  runtime.manualRecovery(schedule,outcome,linked?action:"Supervisor must restore owner/source responsibility or authorize reassignment; "+action,c);
 }
}
