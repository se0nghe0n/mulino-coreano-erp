package com.mulino.adapters.verification;

import java.time.*;
import java.util.*;

/** Bounded process-local, short-lived, single-read receipts. Never persisted or logged. */
public final class McpCaptureStore {
  private record Entry(Map<String,Object> owner,Map<String,Object> receipt,Instant expires) {}
  private final Map<String,Entry> entries=new HashMap<>();
  private final Clock clock;
  private final Duration ttl;
  private final int limit;
  McpCaptureStore() { this(Clock.systemUTC(),Duration.ofSeconds(60),128); }
  McpCaptureStore(Clock clock,Duration ttl,int limit) { this.clock=clock;this.ttl=ttl;this.limit=limit; }
  synchronized String put(Map<String,Object> owner,Map<String,Object> receipt) {
    prune();if(entries.size()>=limit)return null;
    String id=UUID.randomUUID().toString();
    var value=new LinkedHashMap<>(receipt);value.put("captureId",id);value.put("expiresAt",clock.instant().plus(ttl).toString());
    entries.put(id,new Entry(Map.copyOf(owner),Collections.unmodifiableMap(value),clock.instant().plus(ttl)));
    return id;
  }
  synchronized Optional<Map<String,Object>> take(String id,Map<String,Object> owner) {
    prune();var entry=entries.get(id);
    if(entry==null||!entry.owner().equals(owner))return Optional.empty();
    entries.remove(id);return Optional.of(entry.receipt());
  }
  private void prune() { entries.values().removeIf(e->!clock.instant().isBefore(e.expires())); }
}
