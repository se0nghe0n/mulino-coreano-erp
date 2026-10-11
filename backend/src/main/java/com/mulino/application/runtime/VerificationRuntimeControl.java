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
 private final VerificationFaults faults;
 private final VerificationScheduler scheduler;
 private final VerificationResponder responder;
 public VerificationRuntimeControl(VerificationClock clock,VerificationBarriers barriers,VerificationFaults faults,VerificationScheduler scheduler,VerificationResponder responder){this.clock=clock;this.barriers=barriers;this.faults=faults;this.scheduler=scheduler;this.responder=responder;}
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
 /** Fault points the pipeline reports; a host adapter refuses any other authored point instead of arming it. */
 @GetMapping("/fault/points") public Map<String,Object> faultPoints(HttpServletRequest request){
  loopback(request);return Map.of("points",new java.util.TreeSet<>(VerificationFaults.POINTS),"profile","verification");
 }
 /** arm/inject/install a fault for one organization at one pipeline point, or disarm/remove it by id. */
 @PostMapping("/fault") public org.springframework.http.ResponseEntity<Map<String,Object>> fault(@RequestBody Map<String,Object> body,HttpServletRequest request){
  loopback(request);
  try{var view=new java.util.LinkedHashMap<String,Object>(faults.control(body));view.put("operation",body.get("operation"));view.put("profile","verification");view.put("observedAt",Instant.now().toString());return org.springframework.http.ResponseEntity.ok(view);}
  catch(VerificationFaults.UnsupportedPoint unsupported){return org.springframework.http.ResponseEntity.status(422).body(Map.of("supported",false,"point",unsupported.getMessage(),"points",new java.util.TreeSet<>(VerificationFaults.POINTS)));}
 }
 /** One scheduler cycle (tick or due sweep) as one submitted task; the reply is the submission, not the terminal. */
 @PostMapping("/scheduler/tick") public org.springframework.http.ResponseEntity<Map<String,Object>> tick(@RequestBody Map<String,Object> body,HttpServletRequest request){
  loopback(request);
  try{return org.springframework.http.ResponseEntity.ok(scheduler.tick(body));}
  catch(IllegalArgumentException invalid){return org.springframework.http.ResponseEntity.status(422).body(Map.of("accepted",false,"reason",String.valueOf(invalid.getMessage())));}
 }
 /** The scheduler's own submission, cycle and terminal rows (read-only). */
 @GetMapping("/scheduler/records") public Map<String,Object> schedulerRecords(@RequestParam(required=false) String schedulerId,HttpServletRequest request){
  loopback(request);var view=new java.util.LinkedHashMap<String,Object>(scheduler.records(schedulerId));view.put("profile","verification");view.put("observedAt",Instant.now().toString());return view;
 }
 /** Configure how the synthetic external receiver answers outbox deliveries. */
 @PostMapping("/responder") public org.springframework.http.ResponseEntity<Map<String,Object>> responder(@RequestBody Map<String,Object> body,HttpServletRequest request){
  loopback(request);
  try{return org.springframework.http.ResponseEntity.ok(responder.configure(body));}
  catch(IllegalArgumentException invalid){return org.springframework.http.ResponseEntity.status(422).body(Map.of("accepted",false,"reason",String.valueOf(invalid.getMessage()),"modes",new java.util.TreeSet<>(VerificationResponder.MODES)));}
 }
 /** Every request the synthetic receiver received (read-only). */
 @GetMapping("/responder/requests") public Map<String,Object> responderRequests(@RequestParam(required=false) String externalOperationId,HttpServletRequest request){
  loopback(request);return Map.of("requests",responder.requests(externalOperationId),"profile","verification","observedAt",Instant.now().toString());
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
