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
  @SuppressWarnings({"unchecked","rawtypes"})
  public List<Map<String,Object>> rows(String entity,DomainContext context) {
    if(!Set.of("Works","GoalReferences","AssessmentReferences","ObligationReferences","SubjectLinks","EvidenceReferences").contains(entity)) throw DomainError.invalid("Unsupported read entity");
    return (List)db.run(Select.from("mulino.work.read."+entity)
        .where(r->r.get("organizationId").eq(context.organizationId())
            .and(r.get("effectiveAt").le(context.asOf()))
            .and(r.get("recordedAt").le(context.knownAt())))
        .orderBy("ID")).listOf(Map.class);
  }
}
