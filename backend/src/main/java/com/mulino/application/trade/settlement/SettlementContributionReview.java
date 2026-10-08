package com.mulino.application.trade.settlement;
import com.mulino.application.core.*;
import com.mulino.application.responsibility.ResponsibilityService;
import com.mulino.application.trade.SettlementContributionPort;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import com.mulino.domain.trade.settlement.SettlementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Component;
/**
 * A verified post-match correction of a delivery or receipt fact re-derives each affected match with the shared
 * settlement predicate. When the recognized contribution no longer equals the matched one, a SETTLEMENT_DIFFERENCE
 * duty keyed to the correcting canonical (a new root per correction) is opened with the work owner and a next action,
 * so the difference is never ownerless and adjustments have an open duty to target (plan §6 정산, §4.3, §5.3).
 * A correction that restores the contribution but leaves the match unsatisfied, with no open root of the match, opens one too.
 */
@Component
public class SettlementContributionReview implements SettlementContributionPort {
 static final String KIND="SETTLEMENT_DIFFERENCE",NEXT="정정된 인정 기여와 송장 대조 차이를 조정하거나 MANAGER 면제 결정을 받는다";
 private final SettlementRepository r;private final SettlementState state;private final WorkAccess works;private final ResponsibilityService duties;private final ResponsibilityRepository dutyRows;
 public SettlementContributionReview(SettlementRepository r,SettlementState state,WorkAccess works,ResponsibilityService duties,ResponsibilityRepository dutyRows){this.r=r;this.state=state;this.works=works;this.duties=duties;this.dutyRows=dutyRows;}
 public List<String> contributionChanged(DomainContext c,String canonicalId){
  var occurrences=new HashMap<String,Map<String,Object>>();for(var x:dutyRows.evidenceRows("Occurrences",c.organizationId()))occurrences.put(x.get("ID").toString(),x);
  var correcting=occurrences.get(canonicalId);if(correcting==null)throw DomainError.invalid("Correcting canonical occurrence required");
  // The correcting canonical and every canonical it supersedes name the same contribution.
  var chain=new LinkedHashSet<String>();String cursor=canonicalId;while(cursor!=null&&chain.add(cursor)){var row=occurrences.get(cursor);cursor=row==null||row.get("supersedesId")==null?null:row.get("supersedesId").toString();}
  var opened=new ArrayList<String>();
  for(var match:r.rows(c,"Matches")){if(match.get("occurrenceId")==null||!chain.contains(match.get("occurrenceId").toString()))continue;
   String invoiceId=match.get("invoiceId").toString();r.fence(c,"settlement/invoice/"+invoiceId);
   var assessed=state.assess(c,match,r.rows(c,"Adjustments"));
   // A current (restored) contribution leaves nothing to own only when the match is SATISFIED; an open contribution-change root
   // then closes by the restoring canonical (SettlementResponsibilities). Otherwise the remaining difference (for example a confirmed
   // adjustment of an earlier, now restored, correction) must be owned: by an open root of this match, or by a new root here.
   if("CURRENT".equals(assessed.get("contributionState"))&&("SATISFIED".equals(assessed.get("result"))||state.matchRoots(c,match).stream().anyMatch(root->state.open(c,root))))continue;
   var invoice=r.require(c,"Invoices",invoiceId);String workId=invoice.get("workId").toString();
   var target=works.require(c,workId,true);if("CLOSED".equals(target.get("status")))target=works.ensureFollowup(c,workId,canonicalId,KIND,NEXT,c.knownAt().plusSeconds(3600));
   var residual=new TreeMap<String,Object>();residual.put("invoiceId",invoiceId);residual.put("matchId",match.get("ID"));residual.put("currency",invoice.get("currency"));residual.put("contributionCanonicalId",canonicalId);residual.put("contributionState",assessed.get("contributionState"));
   var scope=new TreeMap<String,Object>();scope.put("originalWorkId",workId);scope.put("domainSourceId",canonicalId);scope.put("physicalScopeId",invoiceId);scope.put("residual",residual);
   String scopeJson;try{scopeJson=new ObjectMapper().writeValueAsString(scope);}catch(Exception e){throw new IllegalStateException(e);}
   String version=correcting.get("canonicalHash")!=null?correcting.get("canonicalHash").toString():String.valueOf(correcting.getOrDefault("revision",0));
   var params=new LinkedHashMap<String,Object>();params.putAll(Map.of("workId",target.get("ID"),"sourceKind","OCCURRENCE","sourceId",canonicalId,"sourceVersion",version,"kind",KIND,"scopeJson",scopeJson,"nextAction",NEXT,"nextCheckAt",c.knownAt().plusSeconds(3600)));
   var result=duties.openDuty(c,params);opened.add(result.get("rootId").toString());
  }
  return List.copyOf(opened);
 }
}
