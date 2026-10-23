package com.mulino.domain.work;

import com.mulino.application.core.*;
import com.mulino.application.work.WorkAccess;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Repository
public class WorkRepository implements WorkAccess,com.mulino.application.work.WorkContributionRead {
 @org.springframework.beans.factory.annotation.Autowired private com.mulino.application.work.ContributionGoalBinding contributionGoals;
 private final ExecutionClock clock; private final PersistenceService db; private final JdbcTemplate jdbc; private final org.springframework.beans.factory.ObjectProvider<com.mulino.application.work.WorkLifecycleObserver> observers; private final org.springframework.beans.factory.ObjectProvider<com.mulino.application.work.WorkFollowup> followups;
 public WorkRepository(PersistenceService db,JdbcTemplate jdbc,ExecutionClock clock,org.springframework.beans.factory.ObjectProvider<com.mulino.application.work.WorkLifecycleObserver> observers,org.springframework.beans.factory.ObjectProvider<com.mulino.application.work.WorkFollowup> followups){this.db=db;this.clock=clock;this.jdbc=jdbc;this.observers=observers;this.followups=followups;}
 public Map<String,Object> require(DomainContext c,String id,boolean lock){
  if(lock){if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Work lock requires transaction");jdbc.queryForList("SELECT ID FROM mulino_work_read_Works WHERE organizationId=? AND ID=? FOR UPDATE",c.organizationId(),id);}
  return db.run(Select.from("mulino.work.read.Works").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(id)))).first().map(r->new LinkedHashMap<String,Object>(r)).orElseThrow(DomainError::forbidden);
 }
 public Map<String,Object> currentGoal(DomainContext c,String id){var w=require(c,id,false);Object goal=w.get("currentGoalVersionId");if(goal==null)throw new DomainError("NEEDS_INPUT","GOAL_MISSING","Imported work has no current lifecycle goal");return db.run(Select.from("mulino.work.read.GoalReferences").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(goal)))).first().map(r->new LinkedHashMap<String,Object>(r)).orElseThrow(DomainError::forbidden);}
 public void insert(String entity,Map<String,Object> row){db.run(Insert.into(entity).entry(row));}
 public void update(DomainContext c,String id,Map<String,Object> changes){db.run(Update.entity("mulino.work.read.Works").data(changes).where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(id))));}
 public List<Map<String,Object>> contributions(DomainContext c,String workId,String goalId){
  var result=new ArrayList<Map<String,Object>>();var links=links(c);
  for(var raw:db.run(Select.from("mulino.work.WorkContributions").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("recordedAt").le(c.knownAt())))).listOf(Map.class)){
   var link=links.stream().filter(l->Objects.equals(l.get("ID"),raw.get("linkId"))).findFirst();
   boolean target=Objects.equals(workId,raw.get("targetWorkId"))&&Objects.equals(goalId,raw.get("goalId"));
   boolean source=link.isPresent()&&"CONTRIBUTES_TO".equals(link.get().get("kind"))&&Objects.equals(workId,raw.get("sourceWorkId"))&&Objects.equals(goalId,raw.get("sourceGoalVersionId"));
   boolean unknown=link.isPresent()&&"CONTRIBUTES_TO".equals(link.get().get("kind"))&&Objects.equals(workId,link.get().get("sourceWorkId"))&&raw.get("sourceGoalVersionId")==null;
   if(target||source||unknown){var row=new LinkedHashMap<String,Object>(raw);row.put("attributionRole",target?"TARGET":"SOURCE");row.put("attributionState",unknown&&!target?"UNKNOWN":"PINNED");row.put("boundGoalId",target?raw.get("goalId"):raw.get("sourceGoalVersionId"));row.put("boundConditionId",target?raw.get("conditionId"):raw.get("sourceConditionId"));result.add(row);}
  }
  result.sort(Comparator.comparing(row->row.get("ID").toString()));
  return result;
 }
 public Map<String,Object> activity(DomainContext c,String id,String occurrence){var event=db.run(Select.from("mulino.evidence.Events").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(id)))).first().map(r->new LinkedHashMap<String,Object>(r)).orElseThrow(DomainError::forbidden);var claims=db.run(Select.from("mulino.evidence.Claims").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("eventId").eq(id)))).listOf(Map.class);boolean linked=false;for(var claim:claims)if(db.run(Select.from("mulino.evidence.Verifications").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("claimId").eq(claim.get("ID"))).and(r.get("canonicalOccurrenceId").eq(occurrence)).and(r.get("verdict").eq("VERIFIED")))).rowCount()>0)linked=true;if(!linked)throw new DomainError("NEEDS_INPUT","ACTIVITY_OCCURRENCE_UNVERIFIED","Registered activity must be verified against the actual occurrence");return event;}
 public Map<String,Object> occurrence(DomainContext c,String id){return db.run(Select.from("mulino.evidence.CanonicalOccurrences").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(id)))).first().map(r->new LinkedHashMap<String,Object>(r)).orElseThrow(DomainError::forbidden);}
 public void credit(DomainContext c,String linkId,String source,String target,Map<String,Object> p){
  if(p.containsKey("conditionId")&&!"quantity".equals(p.get("conditionId")))throw DomainError.invalid("Unsupported contribution condition");
  String occurrence=String.valueOf(p.get("occurrenceId"));
  jdbc.queryForList("SELECT ID FROM mulino_evidence_CanonicalOccurrences WHERE organizationId=? AND ID=? FOR UPDATE",c.organizationId(),occurrence);
  var actual=occurrence(c,occurrence);var targetWork=require(c,target,true);var sourceWork=require(c,source,true);
  var goal=currentGoal(c,target);var sourceGoal=currentGoal(c,source);
  String quantity=GoalInput.text(p,"quantityValue"),unit=GoalInput.text(p,"quantityUnit"),start=GoalInput.text(p,"startQuantity");
  new DecimalValue(quantity,unit);new DecimalValue(start,unit);
  var begin=new java.math.BigDecimal(start);var amount=new java.math.BigDecimal(quantity);
  if(begin.signum()<0||amount.signum()<=0||actual.get("quantity")==null||begin.add(amount).compareTo(new java.math.BigDecimal(actual.get("quantity").toString()))>0)throw DomainError.invalid("Contribution exceeds actual occurrence");
  if(!unit.equals(actual.get("unit")))throw DomainError.invalid("Contribution unit differs from canonical actual");
  for(var prior:db.run(Select.from("mulino.work.WorkContributions").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("occurrenceId").eq(occurrence)))).listOf(Map.class)){
   var priorStart=new java.math.BigDecimal(prior.get("startQuantity").toString());var priorAmount=new java.math.BigDecimal(prior.get("quantity").toString());
   if(priorStart.compareTo(begin.add(amount))<0&&begin.compareTo(priorStart.add(priorAmount))<0)throw new DomainError("CONFLICT","ACTUAL_RANGE_ALREADY_CREDITED","Actual contribution range already credited");
  }
  String targetCondition=contributionGoals.condition(c,targetWork,goal,actual,unit);
  String sourceCondition=contributionGoals.condition(c,sourceWork,sourceGoal,actual,unit);
  var row=new LinkedHashMap<String,Object>();row.put("organizationId",c.organizationId());row.put("ID",UUID.randomUUID().toString());
  row.put("linkId",linkId);row.put("occurrenceId",occurrence);row.put("targetWorkId",target);row.put("goalId",goal.get("ID"));row.put("conditionId",targetCondition);
  row.put("sourceWorkId",source);row.put("sourceGoalVersionId",sourceGoal.get("ID"));row.put("sourceConditionId",sourceCondition);
  row.put("startQuantity",begin);row.put("quantity",amount);row.put("unit",unit);row.put("recordedAt",clock.instant());
  insert("mulino.work.WorkContributions",row);markInvalidation(c,target,true);
 }
 public String definitionId(DomainContext c,String version){return db.run(Select.from("mulino.definitions.DefinitionVersions").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("state").eq("PUBLISHED")).and(r.get("version").eq(version)))).first().map(r->r.get("ID").toString()).orElseThrow(DomainError::unsupported);}
 public List<Map<String,Object>> links(DomainContext c){return db.run(Select.from("mulino.work.WorkLinks").where(r->r.get("organizationId").eq(c.organizationId()))).listOf(Map.class).stream().map(r->(Map<String,Object>)r).toList();}
 public void replaceOwner(DomainContext c,String id,String previous,String next){var w=require(c,id,true);if(!Objects.equals(previous,w.get("ownerId")))throw new DomainError("CONFLICT","OWNER_CHANGED","Owner changed");if(!Set.of("ACTIVE","WAITING").contains(w.get("status")))throw DomainError.invalid("Only active work accepts owner handover");mutate(c,w,Map.of("ownerId",next),"ownerHandover");}
 public Map<String,Object> ensureFollowup(DomainContext c,String original,String source,String reason,String nextAction,java.time.Instant nextCheck){return followups.getObject().ensure(c,original,source,reason,nextAction,nextCheck);}
 public void markInvalidation(DomainContext c,String id,boolean pending){var w=require(c,id,true);mutate(c,w,Map.of("pendingInvalidation",pending),"assessmentInvalidation");}
 public java.util.Optional<Map<String,Object>> find(DomainContext c,String id){return db.run(Select.from("mulino.work.read.Works").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("ID").eq(id)))).first().map(r->new LinkedHashMap<String,Object>(r));}
 private void mutate(DomainContext c,Map<String,Object> w,Map<String,Object> values,String operation){
  var changes=new LinkedHashMap<String,Object>(values);changes.put("revision",((Number)w.get("revision")).intValue()+1);changes.put("recordedAt",clock.instant());changes.put("effectiveAt",clock.instant());update(c,String.valueOf(w.get("ID")),changes);var snapshot=new LinkedHashMap<>(w);snapshot.putAll(changes);var transition=new LinkedHashMap<String,Object>();transition.put("organizationId",c.organizationId());transition.put("ID",UUID.randomUUID().toString());transition.put("workId",w.get("ID"));transition.put("revision",changes.get("revision"));transition.put("operation",operation);transition.put("previousState",w.get("status"));transition.put("state",w.get("status"));transition.put("actorId",c.actorId());transition.put("recordedAt",changes.get("recordedAt"));try{transition.put("snapshotJson",new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(TransportValues.normalize(snapshot)));}catch(Exception e){throw new IllegalStateException(e);}insert("mulino.work.WorkTransitions",transition);observers.orderedStream().forEach(o->o.changed(c,snapshot));
 }
 public void observed(DomainContext c,Map<String,Object> work){observers.orderedStream().forEach(o->o.changed(c,work));}
}
