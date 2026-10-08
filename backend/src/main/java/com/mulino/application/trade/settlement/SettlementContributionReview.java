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
 * When such a restoration finds an open root, that root's open assignments are re-issued (revision bump) instead, and a residual a
 * MANAGER waiver of this match already covered is not reopened (plan §4.3, §5.3).
 */
@Component
public class SettlementContributionReview implements SettlementContributionPort {
 static final String KIND="SETTLEMENT_DIFFERENCE",NEXT="정정된 인정 기여와 송장 대조 차이를 조정하거나 MANAGER 면제 결정을 받는다",RESTORED="복원된 인정 기여 기준으로 남은 송장 대조 차이를 다시 확인하고 조정하거나 새 MANAGER 면제 결정을 받는다";
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
   boolean current="CURRENT".equals(assessed.get("contributionState"));
   if(current&&"SATISFIED".equals(assessed.get("result")))continue;
   if(current){
    var roots=state.matchRoots(c,match);var openRoots=roots.stream().filter(root->state.open(c,root)).toList();
    // The restored contribution changes what an open root now owns. Re-issuing its open assignments (revision bump, refreshed next
    // action) makes every decision bound to the pre-restoration revision, such as a decided but unexecuted waiver, fail its revision
    // binding, so the outcome does not depend on whether that waiver executes before or after this correction (plan §5.3 waive "현재
    // scope/revision ... 책임의 단절 거부").
    if(!openRoots.isEmpty()){for(var root:openRoots)duties.reissueOpen(c,root,RESTORED,c.knownAt().plusSeconds(3600),"CONTRIBUTION_RESTORED:"+canonicalId);continue;}
    // A residual a MANAGER waiver already validly covered is not revived by a restoration (plan §4.3 "해소/면제 결정이 이미 유효하면 자동
    // 부활시키지 않는다"): the original match difference (no confirmed adjustment since) or a restored residual an earlier root waived.
    if(waivedResidual(c,match,roots,decimal(assessed.get("settlementDifference"))))continue;
   }
   var invoice=r.require(c,"Invoices",invoiceId);String workId=invoice.get("workId").toString();
   var target=works.require(c,workId,true);if("CLOSED".equals(target.get("status")))target=works.ensureFollowup(c,workId,canonicalId,KIND,NEXT,c.knownAt().plusSeconds(3600));
   var residual=new TreeMap<String,Object>();residual.put("invoiceId",invoiceId);residual.put("matchId",match.get("ID"));residual.put("currency",invoice.get("currency"));residual.put("contributionCanonicalId",canonicalId);residual.put("contributionState",assessed.get("contributionState"));if(current&&assessed.get("settlementDifference")!=null)residual.put("settlementDifference",decimal(assessed.get("settlementDifference")).stripTrailingZeros().toPlainString());
   var scope=new TreeMap<String,Object>();scope.put("originalWorkId",workId);scope.put("domainSourceId",canonicalId);scope.put("physicalScopeId",invoiceId);scope.put("residual",residual);
   String scopeJson;try{scopeJson=new ObjectMapper().writeValueAsString(scope);}catch(Exception e){throw new IllegalStateException(e);}
   String version=correcting.get("canonicalHash")!=null?correcting.get("canonicalHash").toString():String.valueOf(correcting.getOrDefault("revision",0));
   var params=new LinkedHashMap<String,Object>();params.putAll(Map.of("workId",target.get("ID"),"sourceKind","OCCURRENCE","sourceId",canonicalId,"sourceVersion",version,"kind",KIND,"scopeJson",scopeJson,"nextAction",NEXT,"nextCheckAt",c.knownAt().plusSeconds(3600)));
   var result=duties.openDuty(c,params);opened.add(result.get("rootId").toString());
  }
  return List.copyOf(opened);
 }
 /** True when a closed-by-waiver root of this match covered exactly the present CURRENT residual. */
 private boolean waivedResidual(DomainContext c,Map<String,Object> match,List<String> roots,java.math.BigDecimal remaining){
  if(remaining==null)return false;var json=new ObjectMapper();var assignments=dutyRows.rows("Assignments",c.organizationId());
  for(String root:roots){
   if(assignments.stream().noneMatch(a->root.equals(a.get("rootId"))&&"WAIVED".equals(a.get("status"))))continue;
   java.math.BigDecimal covered;
   if(root.equals(Objects.toString(match.get("dutyRootId"),null)))covered=decimal(match.get("originalDifference"));
   else{try{var residual=json.readTree(dutyRows.require("Roots",c.organizationId(),root).get("scopeJson").toString()).path("residual");if(!"CURRENT".equals(residual.path("contributionState").asText())||!residual.hasNonNull("settlementDifference"))continue;covered=new java.math.BigDecimal(residual.get("settlementDifference").asText());}catch(Exception e){throw DomainError.invalid("Settlement difference scope invalid");}}
   if(covered!=null&&covered.compareTo(remaining)==0)return true;
  }
  return false;
 }
 private static java.math.BigDecimal decimal(Object v){return v==null?null:v instanceof java.math.BigDecimal b?b:new java.math.BigDecimal(v.toString());}
}
