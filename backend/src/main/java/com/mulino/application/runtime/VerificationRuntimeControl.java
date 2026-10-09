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
 private final VerificationBarriers barriers;
 public VerificationRuntimeControl(VerificationClock clock,VerificationBarriers barriers){this.clock=clock;this.barriers=barriers;}
 @PostMapping("/clock") public Map<String,Object> advance(@RequestBody Map<String,Object> body,HttpServletRequest request){
  return Map.of("instant",clock.advance(instant(body,request)).toString(),"profile","verification","authorityClock",true);
 }
 /** Fixture isolation boundary: start a freshly installed synthetic organization at its fixture asOf. */
 @PostMapping("/clock/fixture-start") public Map<String,Object> fixtureStart(@RequestBody Map<String,Object> body,HttpServletRequest request){
  return Map.of("instant",clock.resetForFixture(instant(body,request)).toString(),"profile","verification","authorityClock",true,"fixtureStart",true);
 }
 /** Pause points the pipeline reports; a host adapter refuses any other authored point instead of waiting for it. */
 @GetMapping("/barrier/points") public Map<String,Object> barrierPoints(HttpServletRequest request){
  loopback(request);return Map.of("points",new java.util.TreeSet<>(VerificationBarriers.POINTS),"profile","verification","pauseLimitSeconds",VerificationBarriers.PAUSE_LIMIT.toSeconds());
 }
 /** waitReached: bounded wait for REACHED (or BLOCKED for a fence-wait point); resume: release the paused participant. */
 @PostMapping("/barrier") public Map<String,Object> barrier(@RequestBody Map<String,Object> body,HttpServletRequest request) throws InterruptedException {
  loopback(request);
  if(!java.util.Set.of("operation","barrierId","participantId","timeoutSeconds").containsAll(body.keySet()))throw new IllegalArgumentException("Unsupported barrier control field");
  String operation=String.valueOf(body.get("operation")),barrier=String.valueOf(body.get("barrierId")),participant=String.valueOf(body.get("participantId"));
  long timeout=body.get("timeoutSeconds") instanceof Number n?n.longValue():30;if(timeout<1||timeout>60)throw new IllegalArgumentException("Barrier timeout must be 1..60 seconds");
  var view=new java.util.LinkedHashMap<String,Object>(switch(operation){
   case "waitReached"->barriers.waitFor(barrier,participant,java.util.Set.of("REACHED","BLOCKED"),java.time.Duration.ofSeconds(timeout));
   case "resume"->barriers.resume(barrier,participant);
   default->throw new IllegalArgumentException("Unsupported barrier operation");
  });
  view.put("operation",operation);view.put("profile","verification");view.put("observedAt",Instant.now().toString());return view;
 }
 private static void loopback(HttpServletRequest request){
  if(!java.util.Set.of("127.0.0.1","0:0:0:0:0:0:0:1","::1").contains(request.getRemoteAddr()))throw new org.springframework.security.access.AccessDeniedException("Verification controls require loopback");
 }
 private static Instant instant(Map<String,Object> body,HttpServletRequest request){
  if(!java.util.Set.of("127.0.0.1","0:0:0:0:0:0:0:1","::1").contains(request.getRemoteAddr()))throw new org.springframework.security.access.AccessDeniedException("Verification controls require loopback");
  if(!body.keySet().equals(java.util.Set.of("instant")))throw new IllegalArgumentException("Only explicit instant is accepted");
  return Instant.parse(body.get("instant").toString());
 }
}
