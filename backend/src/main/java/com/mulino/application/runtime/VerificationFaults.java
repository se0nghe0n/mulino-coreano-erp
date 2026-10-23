package com.mulino.application.runtime;

import com.mulino.application.core.CommandFaults;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Explicit verification profile only. A host control arms a fault for one organization at one pipeline point; the next
 * matching command fails there with {@link Injected}, a non-transient technical failure, so the command transaction
 * rolls back (or, after commit, the response is lost). Points the pipeline does not report are refused at arm time
 * instead of being accepted and never firing.
 */
@Component
@Profile("verification")
public class VerificationFaults implements CommandFaults, com.mulino.application.core.ReassessmentDeferral {
  /** Explicit pipeline locations. The correction checkpoint defers only recovery; the other points keep
   * their existing rollback or lost-response semantics. */
  public static final String DEFER_POINT="evidence.correct.afterPendingBeforeAssessment";
  public static final Set<String> POINTS = Set.of(DEFER_POINT,"command.beforeCommit", "AFTER_IN_PROGRESS_BEFORE_EFFECT_COMMIT", "AUDIT_INSERT",
      "AFTER_DB_COMMIT_BEFORE_RESPONSE");
  private static final Set<String> FIELDS = Set.of("operation", "faultId", "organizationId", "point", "capabilityId", "commandKey", "occurrences", "fault");
  private final Map<String, Fault> faults = new ConcurrentHashMap<>();
  private final Map<String, Deferred> deferred = new LinkedHashMap<>();
  private static final int MAX_DEFERRED=128,MAX_WORKS=1000;
  private static final class Deferred {
    final String id,org,cap,key;String state="ARMED",command;
    Set<String> works=Set.of();Instant reached,committed;
    Deferred(String id,String org,String cap,String key){this.id=id;this.org=org;this.cap=cap;this.key=key;}
    Map<String,Object> view(){var m=new LinkedHashMap<String,Object>();m.put("faultId",id);m.put("organizationId",org);m.put("point",DEFER_POINT);m.put("state",state);m.put("controlKind","DEFER_REASSESSMENT");m.put("committed",committed!=null);m.put("workIds",new TreeSet<>(works));if(cap!=null)m.put("capabilityId",cap);if(key!=null)m.put("commandKey",key);if(command!=null)m.put("commandRecordId",command);if(reached!=null)m.put("reachedAt",reached.toString());if(committed!=null)m.put("committedAt",committed.toString());return m;}
  }
  private Map<String,Object> armDeferred(Map<String,Object> body){
    if(body.get("fault")!=null)throw new IllegalArgumentException("Deferred reassessment is not an injected failure");
    if(body.get("occurrences")!=null&&(!(body.get("occurrences") instanceof Number n)||n.intValue()!=1||n.doubleValue()!=1))throw new IllegalArgumentException("Deferred checkpoint is one transaction");
    String org=UUID.fromString(text(body,"organizationId")).toString();
    String id=body.get("faultId")==null?UUID.randomUUID().toString():text(body,"faultId");
    if(faults.containsKey(id)||deferred.containsKey(id))throw new IllegalArgumentException("Fault identifier already exists");
    if(deferred.size()>=MAX_DEFERRED){var it=deferred.values().iterator();while(it.hasNext()){var d=it.next();if(!Set.of("ARMED","REACHED","DEFERRED").contains(d.state)){it.remove();break;}}}
    if(deferred.size()>=MAX_DEFERRED)throw new IllegalArgumentException("Deferred checkpoint bound reached");
    var d=new Deferred(id,org,optional(body,"capabilityId"),optional(body,"commandKey"));deferred.put(id,d);return d.view();
  }
  private boolean selector(Deferred d,Map<String,Object> body){
    return d.org.equals(UUID.fromString(text(body,"organizationId")).toString())&&DEFER_POINT.equals(text(body,"point"))
      &&(body.get("faultId")==null||d.id.equals(body.get("faultId")))
      &&(body.get("capabilityId")==null||Objects.equals(d.cap,body.get("capabilityId")))
      &&(body.get("commandKey")==null||Objects.equals(d.key,body.get("commandKey")));
  }
  private Map<String,Object> releaseDeferred(Map<String,Object> body){
    if(!DEFER_POINT.equals(text(body,"point")))throw new UnsupportedPoint(text(body,"point"));
    UUID.fromString(text(body,"organizationId"));
    if(body.containsKey("fault")||body.containsKey("occurrences"))throw new IllegalArgumentException("Release cannot change an armed checkpoint");
    var matches=deferred.values().stream().filter(d->selector(d,body)).toList();
    var active=matches.stream().filter(d->Set.of("ARMED","REACHED","DEFERRED").contains(d.state)).toList();
    if(active.size()>1)throw new IllegalArgumentException("Ambiguous deferred checkpoint; exact identifier required");
    if(active.size()==1){var d=active.getFirst();if(!"DEFERRED".equals(d.state)||d.committed==null)throw new IllegalArgumentException("Checkpoint has not reached a committed correction");d.state="RELEASED";return d.view();}
    var released=matches.stream().filter(d->"RELEASED".equals(d.state)).toList();
    if(matches.size()==1&&released.size()==1)return released.getFirst().view();
    throw new IllegalArgumentException("No exact committed checkpoint to release");
  }
  @Override public synchronized void checkpoint(String organization,String cap,String key,String command,Set<String> works){
    if(works.isEmpty())return;
    var matches=deferred.values().stream().filter(d->"ARMED".equals(d.state)&&d.org.equals(organization)&&(d.cap==null||d.cap.equals(cap))&&(d.key==null||d.key.equals(key))).toList();
    if(matches.isEmpty())return;
    if(works.size()>MAX_WORKS)throw new IllegalArgumentException("Deferred affected Work bound exceeded");
    if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()||!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive())throw new IllegalStateException("Checkpoint requires the actual correcting transaction");
    for(var d:matches){d.state="REACHED";d.command=Objects.requireNonNull(command);d.works=Set.copyOf(works);d.reached=Instant.now();
      org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){
        @Override public void afterCommit(){synchronized(VerificationFaults.this){d.committed=Instant.now();d.state="DEFERRED";}}
        @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)synchronized(VerificationFaults.this){d.state="ROLLED_BACK";d.works=Set.of();}}
      });
    }
  }
  @Override public synchronized boolean held(String org,String work){return deferred.values().stream().anyMatch(d->d.org.equals(org)&&d.works.contains(work)&&Set.of("REACHED","DEFERRED").contains(d.state));}


  /** The failure an armed point throws. It is not a SQL serialization failure, so the pipeline never retries it. */
  public static final class Injected extends RuntimeException {
    public Injected(String point, String faultId) { super("Verification fault " + faultId + " injected at " + point); }
  }
  /** An authored point the pipeline does not report. */
  public static final class UnsupportedPoint extends RuntimeException {
    public UnsupportedPoint(String point) { super(point); }
  }

  static final class Fault {
    final String faultId, organizationId, point, capabilityId, commandKey, kind; final Instant armedAt = Instant.now();
    int remaining, fired; String state = "ARMED"; Instant firedAt;
    Fault(String faultId, String organizationId, String point, String capabilityId, String commandKey, String kind, int remaining) {
      this.faultId = faultId; this.organizationId = organizationId; this.point = point; this.capabilityId = capabilityId;
      this.commandKey = commandKey; this.kind = kind; this.remaining = remaining;
    }
    Map<String, Object> view() {
      var m = new LinkedHashMap<String, Object>();
      m.put("faultId", faultId); m.put("organizationId", organizationId); m.put("point", point);
      if (capabilityId != null) m.put("capabilityId", capabilityId);
      if (commandKey != null) m.put("commandKey", commandKey);
      if (kind != null) m.put("fault", kind);
      m.put("state", state); m.put("remaining", remaining); m.put("fired", fired); m.put("armedAt", armedAt.toString());
      if (firedAt != null) m.put("firedAt", firedAt.toString());
      return m;
    }
  }

  /** arm/inject/install create a fault; disarm/remove retire it by id. */
  public synchronized Map<String, Object> control(Map<String, Object> body) {
    if (!FIELDS.containsAll(body.keySet())) throw new IllegalArgumentException("Unsupported fault control field");
    String operation = text(body, "operation");
    switch (operation) {
      case "release" -> {return releaseDeferred(body);}
      case "arm", "inject", "install" -> {
        String point = text(body, "point");
        if (!POINTS.contains(point)) throw new UnsupportedPoint(point);
        if(DEFER_POINT.equals(point))return armDeferred(body);
        int occurrences = body.get("occurrences") instanceof Number n ? n.intValue() : 1;
        if (occurrences < 1 || occurrences > 10) throw new IllegalArgumentException("Fault occurrences must be 1..10");
        String id = body.get("faultId") == null ? UUID.randomUUID().toString() : text(body, "faultId");
        if(deferred.containsKey(id))throw new IllegalArgumentException("Fault identifier belongs to deferred checkpoint");
        if (faults.containsKey(id) && "ARMED".equals(faults.get(id).state)) throw new IllegalArgumentException("Fault already armed");
        var f = new Fault(id, UUID.fromString(text(body, "organizationId")).toString(), point, optional(body, "capabilityId"), optional(body, "commandKey"), optional(body, "fault"), occurrences);
        faults.put(id, f);
        return f.view();
      }
      case "disarm", "remove" -> {
        var d=deferred.get(text(body,"faultId"));
        if(d!=null){if(!selector(d,body))throw new IllegalArgumentException("Deferred checkpoint scope differs");if(Set.of("REACHED","DEFERRED").contains(d.state))throw new IllegalArgumentException("Committed checkpoint requires scoped release");if("ARMED".equals(d.state))d.state="DISARMED";return d.view();}
        Fault f = faults.get(text(body, "faultId"));
        if (f == null) return new LinkedHashMap<>(Map.of("faultId", body.get("faultId"), "state", "NOT_SEEN"));
        if ("ARMED".equals(f.state)) f.state = "DISARMED";
        return f.view();
      }
      default -> throw new IllegalArgumentException("Unsupported fault operation");
    }
  }

  @Override public void check(String organizationId, String capabilityId, String commandKey, String... points) {
    Fault hit = null; String at = null;
    synchronized (this) {
      for (Fault f : faults.values()) {
        if (!"ARMED".equals(f.state) || !f.organizationId.equals(organizationId)) continue;
        if (f.capabilityId != null && !f.capabilityId.equals(capabilityId)) continue;
        if (f.commandKey != null && !f.commandKey.equals(commandKey)) continue;
        if (!Arrays.asList(points).contains(f.point)) continue;
        f.remaining--; f.fired++; f.firedAt = Instant.now(); if (f.remaining == 0) f.state = "FIRED";
        hit = f; at = f.point; break;
      }
    }
    if (hit != null) throw new Injected(at, hit.faultId);
  }

  private static String text(Map<String, Object> body, String key) {
    Object v = body.get(key);
    if (!(v instanceof String s) || s.isBlank()) throw new IllegalArgumentException("Fault control requires " + key);
    return s;
  }
  private static String optional(Map<String, Object> body, String key) { return body.get(key) == null ? null : text(body, key); }
}
