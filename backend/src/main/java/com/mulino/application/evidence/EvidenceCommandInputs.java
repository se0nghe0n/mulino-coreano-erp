package com.mulino.application.evidence;
import com.mulino.application.core.*;
import com.mulino.domain.evidence.EvidenceTypes.*;
import java.time.*;
import java.util.*;
import static com.mulino.domain.evidence.EvidenceTypes.*;
/** Strict typed slots. No public verification booleans, actor IDs or executable URI. */
final class EvidenceCommandInputs {
  private EvidenceCommandInputs() {}
  @SuppressWarnings("unchecked") static Map<String,Object> slots(Map<String,Object> intent,Set<String> fields) {
    if(!(intent.get("slots") instanceof Map<?,?> raw)||raw.keySet().stream().anyMatch(k->!(k instanceof String)))throw DomainError.invalid("Evidence slots required");
    Map<String,Object> slots=(Map<String,Object>)raw;if(!fields.containsAll(slots.keySet()))throw DomainError.invalid("Unsupported evidence slot");return slots;
  }
  static String string(Map<String,Object> s,String name){Object value=s.get(name);if(value==null)return null;if(!(value instanceof String v))throw DomainError.invalid("String evidence slot required");return text(v,1000000);}
  static String required(Map<String,Object>s,String name){return text(string(s,name),1000000);}
  @SuppressWarnings("unchecked") static Subject subject(Map<String,Object>s) {
    if(!(s.get("subject") instanceof Map<?,?> raw)||!Set.of("kind","id").equals(raw.keySet()))throw DomainError.invalid("Typed evidence subject required");
    try {return new Subject(SubjectKind.valueOf(required((Map<String,Object>)raw,"kind")),required((Map<String,Object>)raw,"id"));}catch(IllegalArgumentException e){throw DomainError.invalid("Invalid evidence subject");}
  }
  static Instant time(Map<String,Object>s,String name){try{return Instant.parse(required(s,name));}catch(RuntimeException e){throw DomainError.invalid("UTC evidence instant required");}}
  static ValueState state(Map<String,Object>s){try{return ValueState.valueOf(required(s,"valueState"));}catch(IllegalArgumentException e){throw DomainError.invalid("Explicit value state required");}}
  static EvidenceRecords.EventInput event(Map<String,Object>s){return new EvidenceRecords.EventInput(subject(s),required(s,"kind"),required(s,"sourceNamespace"),required(s,"externalEventId"),required(s,"sourceVersion"),new EffectiveTime(time(s,"effectiveFrom"),s.get("effectiveUntil")==null?null:time(s,"effectiveUntil"),required(s,"timeZone"),required(s,"timePrecision")),state(s),required(s,"payload"),string(s,"supersedesId"),string(s,"invalidatesId"));}
  static EvidenceRecords.DocumentInput document(Map<String,Object>s){return new EvidenceRecords.DocumentInput(subject(s),required(s,"sourceNamespace"),required(s,"sourceReference"),required(s,"mediaType"),required(s,"expectedHash"),required(s,"provenance"),string(s,"supersedesId"));}
  static EvidenceReconciliation.Review review(Map<String,Object>s){return new EvidenceReconciliation.Review(required(s,"claimId"),required(s,"basisDocumentId"),string(s,"physicalScopeId"),string(s,"existingCanonicalId"),required(s,"policyVersion"),required(s,"sourceIdentity"),string(s,"quantity"),string(s,"unit"),time(s,"effectiveFrom"),required(s,"reason"));}
}
