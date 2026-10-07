package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.application.work.WorkLifecycleObserver;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Canonical lifecycle emits an index update, never another Work state. */
@Component
public class RuntimeWorkObserver implements WorkLifecycleObserver {
 private final RuntimeService service;private final ExecutionClock clock;private final ObjectMapper json=new ObjectMapper();
 public RuntimeWorkObserver(RuntimeService service,ExecutionClock clock){this.service=service;this.clock=clock;}
 @Override public void changed(DomainContext c,Map<String,Object> work){
  String status=(String)work.get("status");boolean pending=Boolean.TRUE.equals(work.get("pendingInvalidation"));
  Instant next=clock.instant().plusSeconds(1);String action=pending?"Reevaluate current policy and original goal":"Review current authority and waiting predicate";
  if("WAITING".equals(status)&&work.get("waitJson") instanceof String waitJson){var wait=json.readValue(waitJson,Map.class);next=Instant.parse(wait.get("nextCheckAt").toString());action=wait.get("overdueAction").toString();}
  Instant boundary=service.nextAuthorityBoundary(c,(String)work.get("ownerId"));
  if(!"WAITING".equals(status)&&!pending&&(!"ACTIVE".equals(status)||boundary==null)){service.removeSchedule(c,(String)work.get("ID"),"WORK_RECHECK");return;}
  service.schedule(c,(String)work.get("ID"),"WORK_RECHECK",("ACTIVE".equals(status)&&!pending&&boundary!=null)?boundary:next,boundary,pending,(String)work.get("ownerId"),(String)work.get("supervisorId"),action);
 }
}
