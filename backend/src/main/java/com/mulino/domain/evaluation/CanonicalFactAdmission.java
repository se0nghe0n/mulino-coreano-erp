package com.mulino.domain.evaluation;

import com.mulino.application.core.DomainContext;
import java.math.BigDecimal;
import java.util.*;

/** Authoritative applied domain facts, distinct from a verified source claim. */
public interface CanonicalFactAdmission {
  /** Internal operation selected by the evaluator entrypoint, never a request slot. */
  enum Purpose {
    ASSESS_GOAL("assessGoal"), CLOSE_WORK("closeWork"), OPEN_OBLIGATION("openObligation"),
    RECORD_DELIVERY("recordDelivery"), RECORD_OBSERVED_MOVEMENT("recordObservedMovement");
    private final String capability;
    Purpose(String capability){this.capability=capability;}
    public String capability(){return capability;}
    public boolean causalRecord(){return this==RECORD_DELIVERY||this==RECORD_OBSERVED_MOVEMENT;}
  }
  Set<String> eventKinds();
  Admission admit(DomainContext context,String workId,String goalId,Map<String,Object> canonical);
  /** Existing providers retain assessment behavior; unimplemented purposes fail closed. */
  default Admission admit(DomainContext context,String workId,String goalId,Map<String,Object> canonical,Purpose purpose){
    return purpose==Purpose.ASSESS_GOAL?admit(context,workId,goalId,canonical):new Admission(false,null,null,List.of());
  }
  record Admission(boolean admitted,BigDecimal recognizedQuantity,String lotId,List<String> evidenceRefs){
    public Admission(boolean admitted,BigDecimal recognizedQuantity,List<String> evidenceRefs){this(admitted,recognizedQuantity,null,evidenceRefs);}
    public Admission {evidenceRefs=List.copyOf(evidenceRefs);}
  }
}
