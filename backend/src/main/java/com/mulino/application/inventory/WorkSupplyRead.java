package com.mulino.application.inventory;

import com.mulino.application.core.*;
import com.mulino.application.quality.QualityEligibility;
import com.mulino.domain.inventory.*;
import com.mulino.domain.evidence.EvidenceRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;

/** Read-only explanation of the same exact ranges used by inventory eligibility. */
@Component
public final class WorkSupplyRead {
  private final InventoryRepository inventory;
  private final QualityEligibility eligibility;
  private final EvidenceRepository evidence;
  private final ReadAuthorizer auth;
  public WorkSupplyRead(InventoryRepository inventory,QualityEligibility eligibility,EvidenceRepository evidence,ReadAuthorizer auth){this.inventory=inventory;this.eligibility=eligibility;this.evidence=evidence;this.auth=auth;}
  public Map<String,Object> read(DomainContext c,Map<String,Object> scope,Map<String,Object> filters){
    String action=Objects.toString(filters.get("action"),"SELL"),customer=Objects.toString(scope.getOrDefault("customerId",filters.get("customerId")),null);
    if(!Set.of("SELL","DISPATCH","DISPOSE","INTERNAL_MOVE").contains(action))throw DomainError.invalid("Unsupported eligibility action");
    var selected=new ArrayList<String>();var conditions=new ArrayList<Map<String,Object>>();
    for(var segment:inventory.rows(c,"QuantitySegments")){
      if(!List.of("itemId","lotId","placeId").stream().allMatch(k->scope.get(k)==null||Objects.equals(scope.get(k),segment.get(k))))continue;
      if(!auth.permittedScopes(c,"getAssessment",Map.of("TARGET",List.of(segment.get("ID").toString()),"ITEM",List.of(segment.get("itemId").toString()),"PLACE",List.of(segment.get("placeId").toString()))))continue;
      var eligibilityScope=new LinkedHashMap<String,Object>();eligibilityScope.put("action",action);if(customer!=null)eligibilityScope.put("customerId",customer);
      var per=eligibility.read(c,"getAssessment",segment.get("itemId").toString(),eligibilityScope,List.of(segment));
      Object eligible=per.values().get("eligibleQuantity");if(eligible==null||new BigDecimal(eligible.toString()).signum()==0)continue;
      var result=eligibility.assess(c,segment,action,customer);if(result.eligibleQuantity().signum()==0)continue;
      selected.add(segment.get("ID").toString());
      for(String category:List.of("QC","CUSTOMER","COMMERCIAL"))for(var basis:inventory.rows(c,"DispositionBases")){
        if(!segment.get("ID").equals(basis.get("segmentId"))||!category.equals(basis.get("category"))||!action.equals(basis.get("action"))||!QualityEligibility.active(basis,c.asOf(),c.knownAt())||!result.evidenceRefs().contains(basis.get("evidenceRef")))continue;
        conditions.add(Map.of("kind",category.equals("COMMERCIAL")?"DISPOSITION":category,"result","ALLOWED","evidenceId",basis.get("evidenceRef"),"segmentId",segment.get("ID")));
      }
      for(var condition:result.conditions())if("REGULATORY".equals(condition.get("condition"))&&"ALLOWED".equals(condition.get("state"))){
        for(var v:evidence.rows("Verifications",c.organizationId()))if(result.evidenceRefs().contains(v.get("canonicalOccurrenceId"))&&"VERIFIED".equals(v.get("verdict"))){
          var fact=evidence.require("CanonicalOccurrences",c.organizationId(),v.get("canonicalOccurrenceId").toString());
          if("REGULATORY_DECISION".equals(fact.get("kind"))&&segment.get("ID").equals(fact.get("physicalScopeId")))conditions.add(Map.of("kind","REGULATORY","result",condition.get("state"),"evidenceId",v.get("basisDocumentId"),"segmentId",segment.get("ID")));
        }
      }
    }
    return Map.of("action",action,"eligibleSegmentIds",selected,"conditions",conditions);
  }
}
