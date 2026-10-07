package com.mulino.domain.evidence;
import java.util.*;
/** Domain-owned authoritative noun lookup; no payload-defined entity names. */
public interface EvidenceSubjectPort {
  Set<String> subjectKinds();
  Map<String,Object> require(String organizationId,String subjectKind,String subjectId);
}
