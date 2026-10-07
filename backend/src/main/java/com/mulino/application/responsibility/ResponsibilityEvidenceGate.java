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
 private final ResponsibilityRepository r;private final ExecutionClock clock;private final org.springframework.beans.factory.ObjectProvider<ResponsibilityKindEvidence> kindResolvers;
 public ResponsibilityEvidenceGate(ResponsibilityRepository r,ExecutionClock clock,org.springframework.beans.factory.ObjectProvider<ResponsibilityKindEvidence> kindResolvers){this.kindResolvers=kindResolvers;this.r=r;this.clock=clock;}
 private Map<String,Object> require(String entity,DomainContext c,String id){return r.evidenceRows(entity,c.organizationId()).stream().filter(x->id.equals(x.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);}
 public void requireResolution(DomainContext c,String kind,String root,String scope,String evidence){
  if(!"FOLLOWUP_REVIEW".equals(kind)){var resolvers=kindResolvers.stream().filter(x->kind.equals(x.kind())).toList();if(resolvers.size()!=1)throw DomainError.unsupported();resolvers.getFirst().requireResolution(c,root,scope,evidence);return;}
  var occurrence=require("Occurrences",c,evidence);var leaf=r.require("Scopes",c.organizationId(),scope);
  if(!"RESPONSE_COMPLETED".equals(occurrence.get("kind"))||!"KNOWN".equals(occurrence.get("valueState"))||!"COMPLETE".equals(occurrence.get("reassessmentState")))throw DomainError.invalid("Actual response evidence required");
  Map<?,?> declared;try{declared=new com.fasterxml.jackson.databind.ObjectMapper().readValue(leaf.get("scopeJson").toString(),Map.class);}catch(Exception e){throw DomainError.invalid("Invalid duty scope");}
  if(declared.get("physicalScopeId")==null||!declared.get("physicalScopeId").equals(occurrence.get("physicalScopeId")))throw DomainError.invalid("Resolution scope mismatch");
  var assignments=r.rows("Assignments",c.organizationId());var current=assignments.stream().filter(x->scope.equals(x.get("scopeId"))&&"OPEN".equals(x.get("status"))&&Boolean.TRUE.equals(x.get("valid"))).findFirst().orElseThrow(DomainError::forbidden);
  if(!Objects.equals(current.get("workId"),occurrence.get("workId")))throw DomainError.invalid("Resolution responsible work mismatch");
  if(current.get("quantity")!=null && (occurrence.get("quantity")==null||((java.math.BigDecimal)occurrence.get("quantity")).compareTo((java.math.BigDecimal)current.get("quantity"))!=0||!Objects.equals(current.get("unit"),occurrence.get("unit"))))throw DomainError.invalid("Resolution quantity mismatch");
  if(r.evidenceRows("Occurrences",c.organizationId()).stream().anyMatch(x->evidence.equals(x.get("supersedesId"))))throw DomainError.invalid("Superseded response evidence");
  var verified=r.evidenceRows("Verifications",c.organizationId()).stream().filter(x->evidence.equals(x.get("canonicalOccurrenceId"))&&"VERIFIED".equals(x.get("verdict"))).toList();
  if(verified.stream().noneMatch(v->List.of("sourceMatched","identityMatched","quantityMatched","timeMatched","duplicateChecked").stream().allMatch(k->Boolean.TRUE.equals(v.get(k)))&&v.get("basisDocumentId")!=null))throw DomainError.invalid("Verified actual response basis required");
 }
 public void requireWaiver(DomainContext c,String kind,String root,String evidence,String reason){
  if(reason==null||reason.isBlank())throw DomainError.invalid("Waiver reason required");
  var approval=require("Approvals",c,evidence);
  if(!"APPROVED".equals(approval.get("decision"))||!("WAIVE_"+kind).equals(approval.get("action"))||!clock.instant().isBefore(java.time.Instant.parse(approval.get("expiresAt").toString())))throw DomainError.invalid("Authorized kind waiver decision required");
  var assignment=r.require("Assignments",c.organizationId(),approval.get("targetId").toString());
  if(!root.equals(assignment.get("rootId"))||!"OPEN".equals(assignment.get("status")))throw DomainError.invalid("Waiver target mismatch");
 }
}
