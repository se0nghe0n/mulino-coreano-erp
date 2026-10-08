package com.mulino.application.responsibility;
import com.mulino.application.core.*;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import com.sap.cds.ql.Select;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.stereotype.Component;
/** Core response evidence is verified canonically; domain-specific kind resolvers remain closed. */
@Component
public class ResponsibilityEvidenceGate implements ResponsibilityEvidence {
 private final ResponsibilityRepository r;private final ExecutionClock clock;private final org.springframework.beans.factory.ObjectProvider<ResponsibilityKindEvidence> kindResolvers;private final org.springframework.beans.factory.ObjectProvider<ResponsibilityCompletionEvidence> completionEvidence;
 public ResponsibilityEvidenceGate(ResponsibilityRepository r,ExecutionClock clock,org.springframework.beans.factory.ObjectProvider<ResponsibilityKindEvidence> kindResolvers,org.springframework.beans.factory.ObjectProvider<ResponsibilityCompletionEvidence> completionEvidence){this.kindResolvers=kindResolvers;this.completionEvidence=completionEvidence;this.r=r;this.clock=clock;}
 private Map<String,Object> require(String entity,DomainContext c,String id){return r.evidenceRows(entity,c.organizationId()).stream().filter(x->id.equals(x.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);}
 public void requireResolution(DomainContext c,String kind,String root,String scope,String evidence){
  if(!"FOLLOWUP_REVIEW".equals(kind)){var resolvers=kindResolvers.stream().filter(x->kind.equals(x.kind())).toList();if(resolvers.size()!=1)throw DomainError.unsupported();resolvers.getFirst().requireResolution(c,root,scope,evidence);return;}
  var occurrence=require("Occurrences",c,evidence);var leaf=r.require("Scopes",c.organizationId(),scope);var dutyRoot=r.require("Roots",c.organizationId(),root);
  if(!root.equals(leaf.get("rootId"))||!Boolean.TRUE.equals(leaf.get("leaf")))throw DomainError.invalid("Resolution requires current exact duty root and leaf scope");
  if(!"RESPONSE_COMPLETED".equals(occurrence.get("kind"))||!"KNOWN".equals(occurrence.get("valueState"))||!"COMPLETE".equals(occurrence.get("reassessmentState")))throw DomainError.invalid("Actual response evidence required");
  Map<?,?> declared;try{declared=new com.fasterxml.jackson.databind.ObjectMapper().readValue(leaf.get("scopeJson").toString(),Map.class);}catch(Exception e){throw DomainError.invalid("Invalid duty scope");}
  if(declared.get("physicalScopeId")==null||!declared.get("physicalScopeId").equals(occurrence.get("physicalScopeId")))throw DomainError.invalid("Resolution scope mismatch");
  var assignments=r.rows("Assignments",c.organizationId());var current=assignments.stream().filter(x->scope.equals(x.get("scopeId"))&&"OPEN".equals(x.get("status"))&&Boolean.TRUE.equals(x.get("valid"))).findFirst().orElseThrow(DomainError::forbidden);
  if(!Objects.equals(current.get("workId"),occurrence.get("workId")))throw DomainError.invalid("Resolution responsible work mismatch");
  var provider=completionEvidence.getIfAvailable();if(provider==null)throw DomainError.unsupported();var coverage=provider.requireCoverage(c,evidence);
  java.math.BigDecimal rootQuantity=dutyRoot.get("quantity")==null?java.math.BigDecimal.ONE:(java.math.BigDecimal)dutyRoot.get("quantity");
  java.math.BigDecimal actualQuantity=occurrence.get("quantity")==null?java.math.BigDecimal.ONE:(java.math.BigDecimal)occurrence.get("quantity");
  if(!root.equals(coverage.rootId())||coverage.startQuantity().signum()<0||coverage.quantity().signum()<=0||coverage.startQuantity().add(coverage.quantity()).compareTo(rootQuantity)>0||actualQuantity.compareTo(coverage.quantity())!=0||!Objects.equals(coverage.unit(),dutyRoot.get("unit"))||!Objects.equals(coverage.unit(),occurrence.get("unit")))throw DomainError.invalid("Verified completion duty-root coverage mismatch");
  java.math.BigDecimal leafStart=(java.math.BigDecimal)leaf.get("startQuantity"),leafQuantity=(java.math.BigDecimal)leaf.get("quantity");
  if(leafStart.compareTo(coverage.startQuantity())<0||leafStart.add(leafQuantity).compareTo(coverage.startQuantity().add(coverage.quantity()))>0)throw DomainError.invalid("Completion evidence does not cover this exact duty leaf range");
  if(r.evidenceRows("Occurrences",c.organizationId()).stream().anyMatch(x->evidence.equals(x.get("supersedesId"))))throw DomainError.invalid("Superseded response evidence");
  var verified=r.evidenceRows("Verifications",c.organizationId()).stream().filter(x->evidence.equals(x.get("canonicalOccurrenceId"))&&"VERIFIED".equals(x.get("verdict"))).toList();
  if(verified.stream().noneMatch(v->Objects.equals(coverage.verificationId(),v.get("ID"))&&List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(k->Boolean.TRUE.equals(v.get(k)))&&v.get("basisDocumentId")!=null))throw DomainError.invalid("Verified actual response basis required");
  reserveCredit(c,root,leaf,current,occurrence,evidence,coverage);

 }
 private void reserveCredit(DomainContext c,String root,Map<String,Object>leaf,Map<String,Object>assignment,Map<String,Object>occurrence,String evidence,ResponsibilityCompletionEvidence.Coverage coverage){
  r.fence(c.organizationId(),"completion:"+evidence);
  var bindings=r.rows("CompletionBindings",c.organizationId());var existing=bindings.stream().filter(x->evidence.equals(x.get("occurrenceId"))).findFirst();Map<String,Object> binding;
  java.math.BigDecimal capacity=occurrence.get("quantity")==null?java.math.BigDecimal.ONE:(java.math.BigDecimal)occurrence.get("quantity");
  if(existing.isPresent()){binding=existing.get();if(!coverage.coverageId().equals(binding.get("coverageId"))||!root.equals(binding.get("rootId")))throw DomainError.invalid("Completion evidence already bound to a different duty root");}
  else{binding=creditRow(c);binding.putAll(Map.of("occurrenceId",evidence,"rootId",root,"startQuantity",coverage.startQuantity(),"quantity",capacity,"verificationId",coverage.verificationId(),"coverageId",coverage.coverageId()));if(occurrence.get("unit")!=null)binding.put("unit",occurrence.get("unit"));r.insert("CompletionBindings",binding);}
  String bindingId=binding.get("ID").toString();var credits=r.rows("ResolutionCredits",c.organizationId());
  if(credits.stream().anyMatch(x->assignment.get("ID").equals(x.get("assignmentId"))))throw DomainError.invalid("Assignment completion already credited");
  java.math.BigDecimal quantity=(java.math.BigDecimal)leaf.get("quantity"),start=(java.math.BigDecimal)leaf.get("startQuantity");
  var used=credits.stream().filter(x->bindingId.equals(x.get("bindingId"))).map(x->(java.math.BigDecimal)x.get("quantity")).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add);
  if(used.add(quantity).compareTo(capacity)>0)throw DomainError.invalid("Completion evidence quantity already consumed");
  for(var credit:credits)if(bindingId.equals(credit.get("bindingId"))){var from=(java.math.BigDecimal)credit.get("startQuantity");var q=(java.math.BigDecimal)credit.get("quantity");if(start.compareTo(from.add(q))<0&&from.compareTo(start.add(quantity))<0)throw DomainError.invalid("Completion evidence range already consumed");}
  var credit=creditRow(c);credit.putAll(Map.of("bindingId",bindingId,"rootId",root,"scopeId",leaf.get("ID"),"assignmentId",assignment.get("ID"),"startQuantity",start,"quantity",quantity));r.insert("ResolutionCredits",credit);
 }
 private Map<String,Object> creditRow(DomainContext c){var row=new LinkedHashMap<String,Object>();row.put("organizationId",c.organizationId());row.put("ID",UUID.randomUUID().toString());row.put("revision",0);row.put("createdAt",c.knownAt());row.put("recordedAt",c.knownAt());return row;}
 /** Typed decision only: exact kind action, approved, unexpired, this assignment at this revision, and the stated reason. */
 public void requireWaiver(DomainContext c,String kind,String root,String assignmentId,int assignmentRevision,String approvalId,String reason){
  if(reason==null||reason.isBlank())throw DomainError.invalid("Waiver reason required");
  var approval=require("Approvals",c,approvalId);
  if(!"APPROVED".equals(approval.get("decision"))||!("WAIVE_"+kind).equals(approval.get("action"))||approval.get("expiresAt")==null||!clock.instant().isBefore(instant(approval.get("expiresAt"))))throw DomainError.invalid("Authorized kind waiver decision required");
  if(!assignmentId.equals(String.valueOf(approval.get("targetId")))||!(approval.get("targetRevision") instanceof Number n)||n.intValue()!=assignmentRevision||!root.equals(String.valueOf(approval.get("proposalId"))))throw DomainError.invalid("Waiver decision is bound to a different duty or revision");
  var assignment=r.require("Assignments",c.organizationId(),assignmentId);
  if(!root.equals(assignment.get("rootId"))||!"OPEN".equals(assignment.get("status")))throw DomainError.invalid("Waiver target mismatch");
 }
 private static java.time.Instant instant(Object v){return v instanceof java.time.Instant t?t:v instanceof java.sql.Timestamp s?s.toInstant():java.time.Instant.parse(v.toString());}
}
