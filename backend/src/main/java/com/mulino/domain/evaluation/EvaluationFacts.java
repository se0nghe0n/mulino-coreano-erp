package com.mulino.domain.evaluation;

import java.time.Instant;
import java.util.*;

/** Exact immutable source values; absence never means a numeric zero. */
public record EvaluationFacts(Map<String,List<Fact>> properties, Map<String,List<Fact>> relations) {
  public EvaluationFacts {properties=copy(properties);relations=copy(relations);}
  private static Map<String,List<Fact>> copy(Map<String,List<Fact>> in) {var out=new TreeMap<String,List<Fact>>();in.forEach((k,v)->out.put(k,List.copyOf(v)));return Collections.unmodifiableMap(out);}
  public enum State { KNOWN, MISSING, UNKNOWN, NOT_APPLICABLE, CONFLICT }
  public record Fact(String sourceId, String sourceVersion, String sourceHash, String canonicalId,
      String physicalScopeId, Object value, String unit, State state, boolean verified,
      Instant effectiveFrom, Instant effectiveUntil, Instant recordedAt, List<String> evidenceRefs) {
    public Fact {evidenceRefs=List.copyOf(evidenceRefs);}
  }
}
