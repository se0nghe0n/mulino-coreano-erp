package com.mulino.application.evaluation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.core.*;
import com.mulino.application.evidence.EvidenceCorrectionImpact;
import com.mulino.domain.evaluation.AssessmentRepository;
import java.util.*;
import org.springframework.stereotype.Component;
/** Exact identifier match against actual persisted inputs; correction never rewrites history. */
@Component
public class AssessmentCorrectionImpact implements EvidenceCorrectionImpact {
 private final AssessmentService service;private final AssessmentRepository repository;private final ObjectMapper json=new ObjectMapper();
 public AssessmentCorrectionImpact(AssessmentService service,AssessmentRepository repository){this.service=service;this.repository=repository;}
 public void apply(DomainContext c,Correction correction){
  var ids=new TreeSet<>(correction.affectedWorkIds());
  for(var snapshot:repository.rows(c,"mulino.evaluation.InputSnapshots"))try{if(contains(json.readValue(snapshot.get("contentJson").toString(),Object.class),correction.previousId()))ids.add(snapshot.get("workId").toString());}catch(java.io.IOException e){throw DomainError.invalid("Invalid historical input snapshot");}
  for(String id:ids)service.invalidate(c,id);
 }
 private boolean contains(Object value,String id){if(value instanceof Map<?,?> m)return m.values().stream().anyMatch(v->contains(v,id));if(value instanceof List<?> l)return l.stream().anyMatch(v->contains(v,id));return id.equals(value);}
}
