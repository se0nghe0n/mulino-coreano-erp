package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.application.work.WorkAccess;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Due time requests current assessment; it never creates approval or arrival. */
@Service
public class RuntimeRecoverySweeper {
 private final RuntimeService runtime;private final WorkAccess works;private final ExecutionClock clock;
 public RuntimeRecoverySweeper(RuntimeService runtime,WorkAccess works,ExecutionClock clock){this.runtime=runtime;this.works=works;this.clock=clock;}
 @Transactional public int sweep(){
  var due=runtime.dueSchedules(100);int changed=0;
  for(var schedule:due){
   var c=new DomainContext((String)schedule.get("organizationid"),(String)schedule.get("ownerid"),(String)schedule.get("ownerid"),clock.instant(),clock.instant());
   var work=works.require(c,(String)schedule.get("workid"),true);
   if("CLOSED".equals(work.get("status"))&&!Boolean.TRUE.equals(work.get("pendingInvalidation"))){runtime.removeSchedule(c,(String)work.get("ID"),(String)schedule.get("kind"));continue;}
   works.markInvalidation(c,(String)work.get("ID"),true);
   // Retains the canonical owner; a pending assessment blocks subsequent completion.
   runtime.schedule(c,(String)work.get("ID"),(String)schedule.get("kind"),clock.instant().plusSeconds(1),null,true,(String)work.get("ownerId"),(String)work.get("supervisorId"),"Current grant/policy and wait predicate need evidence-backed recheck");changed++;
  }
  return changed;
 }
}
