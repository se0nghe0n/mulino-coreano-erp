package com.mulino.domain.evaluation;

import com.mulino.application.core.DomainContext;
import java.math.BigDecimal;
import java.util.*;

/** Authoritative applied domain facts, distinct from a verified source claim. */
public interface CanonicalFactAdmission {
  Set<String> eventKinds();
  Admission admit(DomainContext context,String workId,String goalId,Map<String,Object> canonical);
  record Admission(boolean admitted,BigDecimal recognizedQuantity,String lotId,List<String> evidenceRefs){
    public Admission(boolean admitted,BigDecimal recognizedQuantity,List<String> evidenceRefs){this(admitted,recognizedQuantity,null,evidenceRefs);}
    public Admission {evidenceRefs=List.copyOf(evidenceRefs);}
  }
}
