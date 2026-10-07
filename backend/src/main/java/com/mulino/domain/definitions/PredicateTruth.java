package com.mulino.domain.definitions;

import java.util.List;

/** Logical combiner only: evidence-to-fact evaluation belongs to S2. */
public record PredicateTruth(State state,boolean conflict) {
  public enum State { SATISFIED, UNSATISFIED, UNVERIFIED }
  public static PredicateTruth all(List<PredicateTruth> values) {
    if(values.isEmpty()) throw new IllegalArgumentException("Operand required");
    State s=values.stream().anyMatch(v->v.state==State.UNSATISFIED)?State.UNSATISFIED:
        values.stream().allMatch(v->v.state==State.SATISFIED)?State.SATISFIED:State.UNVERIFIED;
    return new PredicateTruth(s,values.stream().anyMatch(PredicateTruth::conflict));
  }
  public static PredicateTruth any(List<PredicateTruth> values) {
    if(values.isEmpty()) throw new IllegalArgumentException("Operand required");
    State s=values.stream().anyMatch(v->v.state==State.SATISFIED)?State.SATISFIED:
        values.stream().allMatch(v->v.state==State.UNSATISFIED)?State.UNSATISFIED:State.UNVERIFIED;
    return new PredicateTruth(s,values.stream().anyMatch(PredicateTruth::conflict));
  }
  public PredicateTruth not() {return new PredicateTruth(state==State.UNVERIFIED?state:state==State.SATISFIED?State.UNSATISFIED:State.SATISFIED,conflict);}
}
