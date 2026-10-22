package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.domain.runtime.RuntimeRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Synthetic lease setup only. Command authorization and commit fencing remain in the production pipeline. */
@RestController
@Profile("verification")
@RequestMapping("/__verification/runtime/worker-claims")
public class VerificationWorkerClaims {
  private final RuntimeRepository repository;
  private final ReadAuthorizer auth;
  private final ExecutionClock clock;
  public VerificationWorkerClaims(RuntimeRepository repository,ReadAuthorizer auth,ExecutionClock clock){
    this.repository=repository;this.auth=auth;this.clock=clock;
  }
  @PostMapping
  @Transactional
  public ResponseEntity<Map<String,Object>> claim(@RequestBody Map<String,Object> body,HttpServletRequest request){
    if(!Set.of("127.0.0.1","::1","0:0:0:0:0:0:0:1").contains(request.getRemoteAddr()))throw new org.springframework.security.access.AccessDeniedException("Verification claims require loopback");
    if(!body.keySet().equals(Set.of("workId","capabilityId","commandId","workerId","ttlSeconds")))throw DomainError.invalid("Explicit lease fields required");
    String work=text(body,"workId",36),capability=text(body,"capabilityId",100),command=text(body,"commandId",240),worker=text(body,"workerId",100);
    try{if(!UUID.fromString(work).toString().equals(work))throw new IllegalArgumentException();}catch(IllegalArgumentException invalid){throw DomainError.invalid("Work must be UUID");}
    long seconds;
    try{if(!(body.get("ttlSeconds") instanceof Number n))throw new IllegalArgumentException();seconds=new java.math.BigDecimal(n.toString()).longValueExact();}
    catch(IllegalArgumentException|ArithmeticException invalid){throw DomainError.invalid("Lease duration must be integer seconds");}
    if(seconds<1||seconds>300)throw DomainError.invalid("Lease duration must be 1..300 seconds");
    // Authenticated identity/membership is mandatory. Deliberately do not grant command authority:
    // a READ-only actor must be able to hold a real lease for a negative authorization test.
    var context=auth.context(clock.instant(),clock.instant());
    if(repository.db().queryForObject("SELECT count(*) FROM mulino_work_read_Works WHERE organizationId=? AND ID=?",Integer.class,context.organizationId(),work)!=1)throw DomainError.forbidden();
    var issued=repository.claim(context,work,capability,command,worker,Duration.ofSeconds(seconds));
    if(issued.isEmpty())return ResponseEntity.status(409).body(Map.of("issued",false,"profile","verification"));
    var lease=issued.get();
    return ResponseEntity.ok(Map.of("issued",true,"profile","verification","claim",Map.of("claimId",lease.get("attemptId"),"fencingToken",lease.get("leaseToken")),"workId",work,"capabilityId",capability,"commandId",command,"leaseExpiresAt",clock.instant().plusSeconds(seconds).toString()));
  }
  @ExceptionHandler(DomainError.class)
  public ResponseEntity<Map<String,Object>> failure(DomainError error){
    return ResponseEntity.status("FORBIDDEN".equals(error.code())?403:422).body(error.response());
  }
  private static String text(Map<String,Object> body,String key,int max){
    if(!(body.get(key) instanceof String value)||value.isBlank()||value.length()>max)throw DomainError.invalid("Invalid lease "+key);
    return value;
  }
}
