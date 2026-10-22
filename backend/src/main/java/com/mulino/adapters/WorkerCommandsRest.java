package com.mulino.adapters;

import com.mulino.application.core.*;
import com.mulino.application.runtime.WorkerClaimResolver;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

/** Authenticated worker submission shares the existing command/authorization pipeline. */
@RestController
public class WorkerCommandsRest {
  private final WorkerClaimResolver claims;
  private final ApplicationCommands commands;

  public WorkerCommandsRest(WorkerClaimResolver claims,ApplicationCommands commands){
    this.claims=claims;this.commands=commands;
  }

  @PostMapping("/api/runtime/worker/commands")
  @SuppressWarnings("unchecked")
  public ResponseEntity<Map<String,Object>> execute(@RequestBody Map<String,Object> request){
    try{
      // Resolve first so absent/invalid claims cannot be used to probe command validation.
      var claim=claims.resolve(request.get("claim") instanceof Map<?,?> value?value:null);
      if(!request.keySet().equals(Set.of("intent","claim"))||!(request.get("intent") instanceof Map<?,?>))
        throw DomainError.invalid("Worker submission requires intent and claim");
      // The resolver transaction closes before execution; the pipeline rechecks the lease
      // under its own commit fence and keeps denial auditing out of the effect transaction.
      return response(commands.executeClaimed((Map<String,Object>)request.get("intent"),claim));
    }catch(AccessDeniedException denied){return response(DomainError.forbidden().response());}
    catch(DomainError failure){return response(failure.response());}
  }

  private ResponseEntity<Map<String,Object>> response(Map<String,Object> result){
    String code=result.get("error") instanceof Map<?,?> error?String.valueOf(error.get("code")):"";
    if(Set.of("FORBIDDEN","STALE_EXECUTION").contains(code))
      return ResponseEntity.status(403).body(DomainError.forbidden().response());
    return ResponseEntity.ok(result);
  }
}
