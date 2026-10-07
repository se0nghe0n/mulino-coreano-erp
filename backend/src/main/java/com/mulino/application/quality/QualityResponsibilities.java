package com.mulino.application.quality;
import com.mulino.application.core.*;
import com.mulino.application.responsibility.*;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import com.mulino.domain.inventory.*;
import java.util.*;
import org.springframework.stereotype.Component;
/** Only a decision releasing this exact restriction can resolve its review duty. */
@Component
public class QualityResponsibilities implements ResponsibilityKindEvidence {
 private final ResponsibilityRepository duties;private final ResponsibilityService service;private final QualityPrimitives primitive;private final QualityEvidence evidence;
 public QualityResponsibilities(ResponsibilityRepository duties,ResponsibilityService service,QualityPrimitives primitive,QualityEvidence evidence){this.duties=duties;this.service=service;this.primitive=primitive;this.evidence=evidence;}
 public String kind(){return "QUALITY_REVIEW";}
 private String domainSource(Map<String,Object>row){try{return new com.fasterxml.jackson.databind.ObjectMapper().readTree(row.get("scopeJson").toString()).path("domainSourceId").asText();}catch(Exception e){throw DomainError.invalid("Exact quality duty source required");}}
 public void requireResolution(DomainContext c,String rootId,String scopeId,String decisionId){var scope=duties.require("Scopes",c.organizationId(),scopeId);if(!rootId.equals(scope.get("rootId"))||!Boolean.TRUE.equals(scope.get("leaf")))throw DomainError.invalid("Exact quality root required");var d=primitive.decision(c,decisionId);if(!"releaseHold".equals(d.get("operation"))||!domainSource(scope).equals(d.get("restrictionId")))throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Referenced hold release required");evidence.require(c,d.get("evidenceId").toString(),d.get("segmentId").toString(),Map.of("segmentId",d.get("segmentId"),"operation","releaseHold","restrictionId",d.get("restrictionId")));}
 public void released(DomainContext c,String restrictionId,String decisionId){for(var a:duties.rows("Assignments",c.organizationId()))if(kind().equals(a.get("kind"))&&"OPEN".equals(a.get("status"))&&Boolean.TRUE.equals(a.get("valid"))&&restrictionId.equals(domainSource(a)))service.settle(c,Map.of("assignmentId",a.get("ID"),"evidenceId",decisionId),false);}
}
