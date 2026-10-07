package com.mulino.domain.evaluation;

import com.mulino.application.core.*;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class AssessmentRepository {
  private final PersistenceService db;
  public AssessmentRepository(PersistenceService db){this.db=db;}
  public List<Map<String,Object>> rows(DomainContext c,String entity){
    if(!Set.of("mulino.work.read.Works","mulino.work.read.GoalReferences","mulino.work.read.SubjectLinks","mulino.work.WorkLinks","mulino.work.WorkContributions","mulino.evidence.SourceProfiles","mulino.work.read.AssessmentReferences","mulino.evaluation.InputSnapshots","mulino.evidence.CanonicalOccurrences","mulino.evidence.Verifications","mulino.evidence.Claims","mulino.evidence.Events","mulino.evidence.InboxRecords","mulino.evidence.DocumentVersions","mulino.inventory.QuantitySegments","mulino.inventory.ObjectRelations","mulino.inventory.UnitConversions","mulino.definitions.RelationDefinitions","mulino.trade.sales.Deliveries").contains(entity))throw DomainError.unsupported();
    return db.run(Select.from(entity).where(x->x.get("organizationId").eq(c.organizationId()).and(x.get(entity.startsWith("mulino.definitions.")||entity.equals("mulino.work.WorkLinks")?"createdAt":"recordedAt").le(c.knownAt())))).listOf(Map.class).stream().map(x->(Map<String,Object>)new LinkedHashMap<String,Object>(x)).toList();
  }
  public void save(Map<String,Object> assessment,Map<String,Object> snapshot){db.run(Insert.into("mulino.work.read.AssessmentReferences").entry(assessment));db.run(Insert.into("mulino.evaluation.InputSnapshots").entry(snapshot));}
}
