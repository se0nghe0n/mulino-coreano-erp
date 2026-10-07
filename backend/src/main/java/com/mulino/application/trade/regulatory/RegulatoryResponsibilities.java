package com.mulino.application.trade.regulatory;
import com.mulino.application.core.*;
import com.mulino.application.trade.TradeImpact;
import com.mulino.application.responsibility.*;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import com.mulino.domain.trade.regulatory.RegulatoryRepository;
import com.mulino.domain.inventory.InventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;
/** A process has durable residual review responsibility; remedy closes only proven causes. */
@Component
public class RegulatoryResponsibilities implements ResponsibilityKindEvidence {
 private final RegulatoryRepository r;private final ResponsibilityRepository duties;private final ResponsibilityService service;private final TradeImpact impact;private final RegulatoryEvidence evidence;private final RegulatoryEligibility eligibility;private final InventoryRepository stock;
 public RegulatoryResponsibilities(RegulatoryRepository r,ResponsibilityRepository duties,ResponsibilityService service,TradeImpact impact,RegulatoryEvidence evidence,RegulatoryEligibility eligibility,InventoryRepository stock){this.r=r;this.duties=duties;this.service=service;this.impact=impact;this.evidence=evidence;this.eligibility=eligibility;this.stock=stock;}
 public String kind(){return "REGULATORY_REVIEW";}
 private Map<String,Object> source(DomainContext c,String id){for(String entity:List.of("ProcedureVersions","DecisionVersions","LabelVerifications"))for(var x:r.rows(c,entity))if(id.equals(x.get("ID")))return x;throw DomainError.forbidden();}
 private String sourceId(Map<String,Object> row){try{return new ObjectMapper().readTree(row.get("scopeJson").toString()).path("domainSourceId").asText();}catch(Exception e){throw DomainError.invalid("Regulatory duty scope invalid");}}
 public void requireResolution(DomainContext c,String root,String scope,String evidenceId){var leaf=duties.require("Scopes",c.organizationId(),scope);if(!root.equals(leaf.get("rootId"))||!Boolean.TRUE.equals(leaf.get("leaf")))throw DomainError.invalid("Exact regulatory root required");var old=source(c,sourceId(leaf));var remedy=source(c,evidenceId);if(!old.get("procedureId").equals(remedy.get("procedureId")))throw DomainError.invalid("Regulatory remedy procedure mismatch");
  if("SUBMISSION_UNCONFIRMED".equals(old.get("status"))){if(!"SUBMITTED".equals(remedy.get("status"))||!evidence.current(c,remedy.get("occurrenceId").toString()))throw RegulatoryEvidence.held();return;}
  var proc=r.require(c,"Procedures",old.get("procedureId").toString());var seg=stock.current(c,"QuantitySegments",proc.get("physicalScopeId").toString());var policy=r.require(c,"Policies",proc.get("policyId").toString());BigDecimal n=new BigDecimal(seg.get("quantity").toString());var result=eligibility.assess(c,proc.get("itemId").toString(),proc.get("lotId").toString(),proc.get("physicalScopeId").toString(),policy.get("action").toString(),n,seg.get("unit").toString());if(!Set.of("ALLOWED","VERIFIED").contains(remedy.get("decision"))||!evidence.current(c,remedy.get("occurrenceId").toString())||result.eligibleQuantity().compareTo(n)!=0)throw RegulatoryEvidence.held();
 }
 public void changed(DomainContext c,Map<String,Object> proc,Map<String,Object> version,String status){String pid=proc.get("ID").toString();var current=new ArrayList<Map<String,Object>>();for(var a:duties.rows("Assignments",c.organizationId()))if(kind().equals(a.get("kind"))&&"OPEN".equals(a.get("status"))&&Boolean.TRUE.equals(a.get("valid")))try{if(pid.equals(source(c,sourceId(a)).get("procedureId")))current.add(a);}catch(DomainError ignored){}
  for(var a:current)try{requireResolution(c,a.get("rootId").toString(),a.get("scopeId").toString(),version.get("ID").toString());service.settle(c,Map.of("assignmentId",a.get("ID"),"evidenceId",version.get("ID")),false);}catch(DomainError unresolved){if(!Set.of("EVIDENCE_UNVERIFIED","TYPE_INVALID").contains(unresolved.code()))throw unresolved;}
  boolean residual=Set.of("SUBMISSION_UNCONFIRMED","REJECTED","SUPPLEMENTARY_REQUIRED","REVOKED").contains(status);
  if("ALLOWED".equals(status)){var seg=stock.current(c,"QuantitySegments",proc.get("physicalScopeId").toString());var pol=r.require(c,"Policies",proc.get("policyId").toString());residual=eligibility.assess(c,proc.get("itemId").toString(),proc.get("lotId").toString(),proc.get("physicalScopeId").toString(),pol.get("action").toString(),new BigDecimal(seg.get("quantity").toString()),seg.get("unit").toString()).eligibleQuantity().compareTo(new BigDecimal(seg.get("quantity").toString()))<0;}
  boolean alreadyOpen=current.stream().anyMatch(a->"OPEN".equals(duties.require("Assignments",c.organizationId(),a.get("ID").toString()).get("status")));
  if(residual&&!alreadyOpen)impact.recorded(c,proc.get("workId").toString(),version.get("ID").toString(),kind(),proc.get("physicalScopeId").toString(),"RECONCILE_REGULATORY_REMAINDER",c.knownAt().plusSeconds(3600));else impact.invalidate(c,proc.get("workId").toString());
 }
}
