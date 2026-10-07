package com.mulino.application.runtime;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/** Available only in explicit verification profile and only on loopback hosts. */
@RestController
@Profile("verification")
@RequestMapping("/__verification/runtime")
public class VerificationRuntimeControl {
 private final VerificationClock clock;
 public VerificationRuntimeControl(VerificationClock clock){this.clock=clock;}
 @PostMapping("/clock") public Map<String,Object> advance(@RequestBody Map<String,Object> body,HttpServletRequest request){
  if(!java.util.Set.of("127.0.0.1","0:0:0:0:0:0:0:1","::1").contains(request.getRemoteAddr()))throw new org.springframework.security.access.AccessDeniedException("Verification controls require loopback");
  if(!body.keySet().equals(java.util.Set.of("instant")))throw new IllegalArgumentException("Only explicit instant is accepted");
  return Map.of("instant",clock.advance(Instant.parse(body.get("instant").toString())).toString(),"profile","verification","authorityClock",true);
 }
}
