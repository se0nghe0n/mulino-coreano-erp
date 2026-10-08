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
    // The re-issue and a waiver of the same organization never interleave: every guarded command first takes the organization row
    // FOR UPDATE (ApplicationCommands.apply -> PolicyCommandGuard.fence -> PolicyRepository.fence) and holds it to commit, so a waiver
    // reads either the pre- or the post-restoration revision of a committed transaction (s4l review index 0, REFUTED by that fence).
    // A same-quantity supersession of a CURRENT contribution changes nothing an open root was issued for, so it re-issues nothing and a
    // decision bound to the current revision stays executable (s4l[2]). Roots issued for a CHANGED/UNVERIFIED contribution are re-issued.
    if(!openRoots.isEmpty()){if(sameContribution(correcting,occurrences)&&openRoots.stream().allMatch(root->issuedCurrent(c,match,root)))continue;
     for(var root:openRoots)duties.reissueOpen(c,root,RESTORED,c.knownAt().plusSeconds(3600),"CONTRIBUTION_RESTORED:"+canonicalId);continue;}
    // A residual a MANAGER waiver already validly covered is not revived by a restoration (plan §4.3 134행 "해소/면제 결정이 이미 유효하면
    // 자동 부활시키지 않는다"): only the CURRENT residual a waiver recorded when it executed counts, never the root's opening amount.
    if(waivedResidual(c,roots,decimal(assessed.get("settlementDifference"))))continue;
   }
   // A relinked correction whose chain contains the canonical an open UNVERIFIED root of this match was opened for (an invalidating
   // correction, AssessmentCorrectionImpact.apply) adopts that root: it is re-issued for the relinked contribution instead of a second
   // root opening beside it (s4l[3], plan §4.3 정정 재평가, §5.3 "현재 책임의 단절/중복을 거부").
   if(!current){var adopted=unverifiedRoots(c,match,chain,canonicalId);if(!adopted.isEmpty()){for(var root:adopted)duties.reissueOpen(c,root,NEXT,c.knownAt().plusSeconds(3600),"CONTRIBUTION_RELINKED:"+canonicalId);continue;}}
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
 /**
  * True when a waiver of a root of this match covered exactly the present CURRENT residual. The covered residual is the one the waiver
  * recorded when it executed (SettlementResponsibilities.waiverCoverage); a waiver with no recorded CURRENT residual, or one taken while
  * the contribution was CHANGED/UNVERIFIED, never suppresses a root (fail closed, s4l[1]/[5]).
  */
 private boolean waivedResidual(DomainContext c,List<String> roots,java.math.BigDecimal remaining){
  if(remaining==null)return false;
  for(var a:dutyRows.rows("Assignments",c.organizationId())){
   if(!roots.contains(Objects.toString(a.get("rootId"),""))||!"WAIVED".equals(a.get("status")))continue;
   var covered=COVERED.matcher(Objects.toString(a.get("basis"),""));
   if(covered.find()&&new java.math.BigDecimal(covered.group(1)).compareTo(remaining)==0)return true;
  }
  return false;
 }
 private static final java.util.regex.Pattern COVERED=java.util.regex.Pattern.compile("\\[SETTLEMENT_COVERED CURRENT (-?[0-9]+(?:\\.[0-9]+)?)\\]$");
 /** The correcting canonical restates the quantity and unit of the canonical it directly supersedes. Unknown predecessor: changed. */
 private static boolean sameContribution(Map<String,Object> correcting,Map<String,Map<String,Object>> occurrences){
  var prior=correcting.get("supersedesId")==null?null:occurrences.get(correcting.get("supersedesId").toString());
  if(prior==null||correcting.get("quantity")==null||prior.get("quantity")==null)return false;
  return decimal(correcting.get("quantity")).compareTo(decimal(prior.get("quantity")))==0&&Objects.equals(correcting.get("unit"),prior.get("unit"));
 }
 /** The match's own difference root, or a root opened for a CURRENT contribution, was issued for an unchanged contribution. */
 private boolean issuedCurrent(DomainContext c,Map<String,Object> match,String root){
  if(root.equals(Objects.toString(match.get("dutyRootId"),null)))return true;
  return "CURRENT".equals(residual(c,root).path("contributionState").asText());
 }
 /** Open roots of this match opened for an UNVERIFIED contribution of a canonical in the relinked chain, other than this canonical. */
 private List<String> unverifiedRoots(DomainContext c,Map<String,Object> match,Set<String> chain,String canonicalId){
  var out=new ArrayList<String>();
  for(String root:state.matchRoots(c,match)){if(!state.open(c,root))continue;var residual=residual(c,root);String opened=residual.path("contributionCanonicalId").asText(null);
   if("UNVERIFIED".equals(residual.path("contributionState").asText())&&opened!=null&&!opened.equals(canonicalId)&&chain.contains(opened))out.add(root);}
  return out;
 }
 private com.fasterxml.jackson.databind.JsonNode residual(DomainContext c,String root){try{return new ObjectMapper().readTree(dutyRows.require("Roots",c.organizationId(),root).get("scopeJson").toString()).path("residual");}catch(Exception e){throw DomainError.invalid("Settlement difference scope invalid");}}
 private static java.math.BigDecimal decimal(Object v){return v==null?null:v instanceof java.math.BigDecimal b?b:new java.math.BigDecimal(v.toString());}
}
