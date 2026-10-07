package com.mulino.application.evidence;
import java.util.*;
/** Intake/review persistence is an applied RECORD; evidence truth and physical effects stay separate. */
final class EvidenceCommandOutcomes {
 private EvidenceCommandOutcomes() {}
 static Map<String,Object> applied(Map<String,Object> recorded) {
  Object status=recorded.get("outcome");
  if(!Set.of("RECORDED","RECORDED_UNAVAILABLE","REPLAYED","EVIDENCE_CONFLICT","VERIFIED_RECORD_ONLY","MATCHED","UNVERIFIED","CONFLICT").contains(status)
      ||recorded.get("id")==null||recorded.get("revision")==null)throw new IllegalStateException("Evidence command returned no persisted record");
  var result=new LinkedHashMap<String,Object>(recorded);result.put("evidenceStatus",status);result.put("outcome","APPLIED");result.put("inventoryEffects","NONE");return result;
 }
}
