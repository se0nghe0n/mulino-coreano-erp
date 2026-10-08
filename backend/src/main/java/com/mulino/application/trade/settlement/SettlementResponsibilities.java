package com.mulino.application.trade.settlement;
import com.mulino.application.core.*;
import com.mulino.application.responsibility.ResponsibilityKindEvidence;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import com.mulino.domain.trade.settlement.SettlementRepository;
import com.mulino.domain.trade.settlement.SettlementAmounts;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
/**
 * A settlement difference is fulfilled by MANAGER-confirmed adjustments of the same invoice match
 * only when the shared settlement predicate is SATISFIED: zero remaining amount, no quantity, currency
 * or scope difference and an unchanged current recognized contribution. The original variance stays
 * recorded (plan §6 정산, §5.3). Anything an amount cannot reconcile, and every CREDIT_NOTE/CORRECTION
 * review, closes only by the typed MANAGER waiver.
 */
@Component
public class SettlementResponsibilities implements ResponsibilityKindEvidence {
 private final ResponsibilityRepository duties;private final SettlementRepository r;private final SettlementState settlement;
 public SettlementResponsibilities(ResponsibilityRepository duties,SettlementRepository r,@Lazy SettlementState settlement){this.duties=duties;this.r=r;this.settlement=settlement;}
 public String kind(){return "SETTLEMENT_DIFFERENCE";}
 public void requireResolution(DomainContext c,String rootId,String scopeId,String adjustmentId){
  var leaf=duties.require("Scopes",c.organizationId(),scopeId);var root=duties.require("Roots",c.organizationId(),rootId);
  if(!rootId.equals(leaf.get("rootId"))||!Boolean.TRUE.equals(leaf.get("leaf"))||!kind().equals(root.get("kind")))throw DomainError.invalid("Exact settlement difference leaf required");
  String invoiceId,matchId;boolean correction;
  try{var scope=new ObjectMapper().readTree(root.get("scopeJson").toString());invoiceId=scope.path("residual").path("invoiceId").asText();matchId=scope.path("residual").path("matchId").asText();correction=scope.path("residual").hasNonNull("correctionInvoiceId");}catch(Exception e){throw DomainError.invalid("Settlement difference scope invalid");}
  if(correction)throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Correction invoice review closes only by a MANAGER waiver decision");
  r.fence(c,"settlement/invoice/"+invoiceId);
  var found=r.rows(c,"Adjustments").stream().filter(x->adjustmentId.equals(x.get("ID"))).findFirst();
  if(found.isEmpty()){requireRestoration(c,rootId,root,invoiceId,matchId,adjustmentId);return;}
  var adjustment=found.get();
  if(!"CONFIRMED".equals(adjustment.get("status"))||!invoiceId.equals(adjustment.get("invoiceId"))||!matchId.equals(adjustment.get("matchId"))||adjustment.get("approverId")==null)throw unverified();
  // The confirmed adjustment must have been proposed for this exact duty root (SettlementCommands.proposalHash).
  if(!SettlementCommands.proposalHash(invoiceId,matchId,SettlementAmounts.decimal(adjustment.get("amount")),adjustment.get("currency").toString(),adjustment.get("reason").toString(),rootId).equals(adjustment.get("proposalHash")))throw unverified();
  if(adjustment.get("occurrenceId")==null||duties.evidenceRows("Occurrences",c.organizationId()).stream().anyMatch(x->adjustment.get("occurrenceId").equals(x.get("supersedesId"))))throw unverified();
  var match=r.rows(c,"Matches").stream().filter(x->matchId.equals(x.get("ID"))&&invoiceId.equals(x.get("invoiceId"))).findFirst().orElseThrow(SettlementResponsibilities::unverified);
  if(Boolean.TRUE.equals(match.get("currencyDifference"))||Boolean.TRUE.equals(match.get("scopeDifference")))throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Currency or scope difference is not closed by an amount adjustment");
  var assessed=settlement.assess(c,match,r.rows(c,"Adjustments"));
  if(!"SATISFIED".equals(assessed.get("result")))throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Settlement is "+assessed.get("result")+": remaining amount, quantity difference or current contribution is not reconciled by confirmed adjustments");
 }
 /**
  * Restoration (plan §4.3 정정 재평가, §5.3 resolveObligation 충족 증거): a contribution-change root is fulfilled by the current verified
  * canonical that supersedes the correction which opened it, when the recognized contribution behind the match is CURRENT again and
  * the shared settlement predicate is SATISFIED. The original match difference root has no such resolution.
  */
 private void requireRestoration(DomainContext c,String rootId,Map<String,Object> root,String invoiceId,String matchId,String canonicalId){
  String opened;try{opened=new ObjectMapper().readTree(root.get("scopeJson").toString()).path("residual").path("contributionCanonicalId").asText(null);}catch(Exception e){throw DomainError.invalid("Settlement difference scope invalid");}
  if(opened==null||opened.equals(canonicalId))throw unverified();
  var match=r.rows(c,"Matches").stream().filter(x->matchId.equals(x.get("ID"))&&invoiceId.equals(x.get("invoiceId"))).findFirst().orElseThrow(SettlementResponsibilities::unverified);
  if(Boolean.TRUE.equals(match.get("currencyDifference"))||Boolean.TRUE.equals(match.get("scopeDifference")))throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Currency or scope difference is not closed by a restoring correction");
  var current=settlement.contribution(c,match.get("scopeKind").toString(),match.get("lineId").toString(),match.get("referenceId").toString());
  if(!canonicalId.equals(Objects.toString(current.get("occurrenceId"),null)))throw unverified();
  var occurrences=new HashMap<String,Map<String,Object>>();for(var x:duties.evidenceRows("Occurrences",c.organizationId()))occurrences.put(x.get("ID").toString(),x);
  boolean later=false;var seen=new HashSet<String>();String cursor=canonicalId;while(cursor!=null&&seen.add(cursor)){var row=occurrences.get(cursor);cursor=row==null||row.get("supersedesId")==null?null:row.get("supersedesId").toString();if(opened.equals(cursor))later=true;}
  if(!later)throw unverified();
  var assessed=settlement.assess(c,match,r.rows(c,"Adjustments"));
  if(!"SATISFIED".equals(assessed.get("result")))throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Settlement is "+assessed.get("result")+": the restoring correction does not reconcile the match");
 }
 /**
  * The residual this SETTLEMENT_DIFFERENCE waiver covers, read with the shared predicate when the waiver executes (plan §4.3 134행
  * "해소/면제 결정이 이미 유효하면 자동 부활시키지 않는다", §5.3 waiveObligation, §6 정산). Only a CURRENT contribution has a residual a
  * later CURRENT restoration can compare with; a CHANGED/UNVERIFIED or correction-invoice waiver records its state and no amount.
  */
 public String waiverCoverage(DomainContext c,String rootId){
  var root=duties.require("Roots",c.organizationId(),rootId);if(!kind().equals(root.get("kind")))return null;
  String invoiceId,matchId;try{var residual=new ObjectMapper().readTree(root.get("scopeJson").toString()).path("residual");if(residual.hasNonNull("correctionInvoiceId"))return COVERED+" NONE]";invoiceId=residual.path("invoiceId").asText(null);matchId=residual.path("matchId").asText(null);}catch(Exception e){throw DomainError.invalid("Settlement difference scope invalid");}
  var match=r.rows(c,"Matches").stream().filter(x->Objects.equals(matchId,Objects.toString(x.get("ID"),null))&&Objects.equals(invoiceId,Objects.toString(x.get("invoiceId"),null))).findFirst();
  if(match.isEmpty())return COVERED+" NONE]";
  var assessed=settlement.assess(c,match.get(),r.rows(c,"Adjustments"));String state=Objects.toString(assessed.get("contributionState"),"UNVERIFIED");
  if("CURRENT".equals(state)&&assessed.get("settlementDifference") instanceof java.math.BigDecimal remaining)return COVERED+" CURRENT "+remaining.stripTrailingZeros().toPlainString()+"]";
  return COVERED+" "+state+"]";
 }
 /** Marker prefix the waived assignment basis ends with; parsed only from the server-appended suffix. */
 static final String COVERED="[SETTLEMENT_COVERED";
 private static DomainError unverified(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","MANAGER-confirmed adjustment of this exact invoice match required");}
}
