package com.mulino.application.evidence;
import com.mulino.application.core.DomainContext;
import java.util.*;
/** Required atomic invalidation/duty port. Historical assessments and stock movements remain immutable. */
public interface EvidenceCorrectionImpact {
  record Correction(String previousId,String currentId,Set<String> affectedWorkIds,Set<String> affectedGoalIds) {
    public Correction { affectedWorkIds=Set.copyOf(affectedWorkIds);affectedGoalIds=Set.copyOf(affectedGoalIds); }
  }
  default void evidenceLinked(DomainContext context,String claimId,String canonicalId){throw com.mulino.application.core.DomainError.unsupported();}
  void apply(DomainContext context,Correction correction);
}
