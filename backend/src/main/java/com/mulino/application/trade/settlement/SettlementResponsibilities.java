package com.mulino.application.trade.settlement;
import com.mulino.application.core.*;
import com.mulino.application.responsibility.ResponsibilityKindEvidence;
import com.mulino.domain.responsibility.ResponsibilityRepository;
import com.mulino.domain.trade.settlement.SettlementRepository;
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
  var adjustment=r.rows(c,"Adjustments").stream().filter(x->adjustmentId.equals(x.get("ID"))).findFirst().orElseThrow(SettlementResponsibilities::unverified);
  if(!"CONFIRMED".equals(adjustment.get("status"))||!invoiceId.equals(adjustment.get("invoiceId"))||!matchId.equals(adjustment.get("matchId"))||adjustment.get("approverId")==null)throw unverified();
  if(adjustment.get("occurrenceId")==null||duties.evidenceRows("Occurrences",c.organizationId()).stream().anyMatch(x->adjustment.get("occurrenceId").equals(x.get("supersedesId"))))throw unverified();
  var match=r.rows(c,"Matches").stream().filter(x->matchId.equals(x.get("ID"))&&invoiceId.equals(x.get("invoiceId"))).findFirst().orElseThrow(SettlementResponsibilities::unverified);
  if(Boolean.TRUE.equals(match.get("currencyDifference"))||Boolean.TRUE.equals(match.get("scopeDifference")))throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Currency or scope difference is not closed by an amount adjustment");
  var assessed=settlement.assess(c,match,r.rows(c,"Adjustments"));
  if(!"SATISFIED".equals(assessed.get("result")))throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Settlement is "+assessed.get("result")+": remaining amount, quantity difference or current contribution is not reconciled by confirmed adjustments");
 }
 private static DomainError unverified(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","MANAGER-confirmed adjustment of this exact invoice match required");}
}
