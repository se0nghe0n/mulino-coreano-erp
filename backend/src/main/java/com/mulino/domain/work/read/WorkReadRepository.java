package com.mulino.domain.work.read;

import com.mulino.application.core.*;
import com.sap.cds.ql.Select;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.stereotype.Repository;

/** S1 read-facing immutable references; lifecycle writes belong to S2. */
@Repository
public class WorkReadRepository {
  private final PersistenceService db;
  public WorkReadRepository(PersistenceService db) { this.db=db; }
  public boolean documentKnown(DomainContext context,String id) {
    return db.run(Select.from("mulino.evidence.DocumentVersions")
        .where(r->r.get("organizationId").eq(context.organizationId()).and(r.get("ID").eq(id))
            .and(r.get("recordedAt").le(context.knownAt())))).rowCount() > 0;
  }
  @SuppressWarnings({"unchecked","rawtypes"})
  public List<Map<String,Object>> rows(String entity,DomainContext context) {
    if(!Set.of("Works","GoalReferences","AssessmentReferences","ObligationReferences","SubjectLinks","EvidenceReferences").contains(entity)) throw DomainError.invalid("Unsupported read entity");
    if(entity.equals("Works"))return workHistory(context);
    return (List)db.run(Select.from("mulino.work.read."+entity)
        .where(r->r.get("organizationId").eq(context.organizationId())
            .and(r.get("effectiveAt").le(context.asOf()))
            .and(r.get("recordedAt").le(context.knownAt())))
        .orderBy("ID")).listOf(Map.class);
  }
  @SuppressWarnings({"unchecked","rawtypes"})
  private List<Map<String,Object>> workHistory(DomainContext c) {
    List<Map<String,Object>> result=new ArrayList<>();
    var works=db.run(Select.from("mulino.work.read.Works").where(r->r.get("organizationId").eq(c.organizationId()))).listOf(Map.class);
    for(var raw:works){Map<String,Object> w=(Map)raw;
      if(!"COMMAND".equals(w.get("lifecycleMode"))){if(instant(w.get("effectiveAt")).isAfter(c.asOf())||instant(w.get("recordedAt")).isAfter(c.knownAt()))continue;result.add(w);continue;}
      var transitions=db.run(Select.from("mulino.work.WorkTransitions").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("workId").eq(w.get("ID"))).and(r.get("recordedAt").le(c.knownAt()))).orderBy("revision desc")).listOf(Map.class);
      for(var transition:transitions)try{Map<String,Object> snapshot=new com.fasterxml.jackson.databind.ObjectMapper().readValue(String.valueOf(transition.get("snapshotJson")),Map.class);if(!instant(snapshot.get("effectiveAt")).isAfter(c.asOf())){result.add(snapshot);break;}}catch(java.io.IOException failure){throw new IllegalStateException("Invalid work transition snapshot",failure);}
    }
    result.sort(Comparator.comparing(w->String.valueOf(w.get("ID"))));return result;
  }
  private java.time.Instant instant(Object value){return value instanceof java.time.Instant i?i:java.time.Instant.parse(String.valueOf(value));}
}
