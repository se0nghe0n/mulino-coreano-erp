package com.mulino.application.trade.settlement;
import com.mulino.application.core.*;
import com.mulino.application.trade.TradeEvidence;
import com.mulino.domain.trade.settlement.SettlementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.mulino.domain.trade.settlement.SettlementAmounts.*;
/**
 * One settlement predicate for command, query and duty closure (plan §6 정산, §4.3, §5.3).
 * The recognized receipt/delivery contribution is read from current facts at every use, so a
 * later correction or a superseded receipt chain is never hidden behind the immutable match.
 */
@Component
public class SettlementState {
 private final SettlementRepository r;private final SettlementTradeFacts facts;private final TradeEvidence evidence;
 public SettlementState(SettlementRepository r,SettlementTradeFacts facts,TradeEvidence evidence){this.r=r;this.facts=facts;this.evidence=evidence;}

 /** Current recognized contribution; PURCHASE re-verifies the receipt canonical chain like the SALE delivery credit does. */
 public Map<String,Object> contribution(DomainContext c,String scopeKind,String lineId,String referenceId){
  var current=facts.contribution(c,scopeKind,lineId,referenceId);
  if("PURCHASE".equals(scopeKind)){String occurrence=Objects.toString(current.get("occurrenceId"),"");var line=facts.line(c,scopeKind,lineId);var fact=r.external(c,"mulino.evidence.CanonicalOccurrences",occurrence);
   evidence.requireCanonical(c,occurrence,"PHYSICAL_RECEIPT",line.get("itemId").toString(),Objects.toString(fact.get("physicalScopeId"),null),decimal(fact.get("quantity")),Objects.toString(fact.get("unit"),null));}
  return current;
 }

 /** Recognized (not actual) contribution: excess receipts and corrected deliveries never count as billable quantity. */
 public static BigDecimal recognized(Map<String,Object> contribution){return nonnegative(contribution.get("recognizedQuantity"));}

 /** Settlement result of one match from its immutable differences, the given confirmed adjustments and current facts. */
 public Map<String,Object> assess(DomainContext c,Map<String,Object> match,List<Map<String,Object>> adjustments){
  var out=new LinkedHashMap<String,Object>();boolean cd=Boolean.TRUE.equals(match.get("currencyDifference")),scope=Boolean.TRUE.equals(match.get("scopeDifference"));
  BigDecimal qd=decimal(match.get("quantityDifference")),received=decimal(match.get("receivedQuantity"));
  out.put("quantityDifference",qd);out.put("quantityDifferenceUnit",match.get("unit"));out.put("priceDifference",match.get("priceDifference"));out.put("currencyDifference",cd);out.put("scopeDifference",scope);out.put("receivedQuantity",received);
  String state;BigDecimal current=null;
  try{current=recognized(contribution(c,match.get("scopeKind").toString(),match.get("lineId").toString(),match.get("referenceId").toString()));state=current.compareTo(received)==0?"CURRENT":"CHANGED";}
  catch(DomainError e){state="UNVERIFIED";}
  out.put("contributionState",state);
  if(current!=null){out.put("currentReceivedQuantity",current);if(!"CURRENT".equals(state)){out.put("currentQuantityDifference",decimal(match.get("invoiceQuantity")).subtract(current));if(!cd)out.put("currentOriginalDifference",decimal(match.get("invoiceAmount")).subtract(product(current.min(decimal(match.get("orderedQuantity"))),decimal(match.get("unitPrice")))));}}
  if(match.get("originalDifference")==null){out.put("result","UNVERIFIED");return out;}
  BigDecimal original=decimal(match.get("originalDifference")),approved=adjustments.stream().filter(x->match.get("ID").equals(x.get("matchId"))&&"CONFIRMED".equals(x.get("status"))).map(x->decimal(x.get("amount"))).reduce(BigDecimal.ZERO,BigDecimal::add),remaining=original.subtract(approved);
  out.put("originalDifference",original);out.put("settlementDifference",remaining);
  if(out.get("currentOriginalDifference") instanceof BigDecimal now)out.put("currentSettlementDifference",now.subtract(approved));
  boolean satisfied=remaining.signum()==0&&qd.signum()==0&&!cd&&!scope&&"CURRENT".equals(state);
  out.put("result","UNVERIFIED".equals(state)?"UNVERIFIED":satisfied?"SATISFIED":"UNSATISFIED");
  return out;
 }

 /** True while any visible assignment of the duty root is still OPEN (a transferred root keeps its open children). */
 public boolean open(DomainContext c,Object rootId){
  if(rootId==null)return false;
  return r.externalRows(c,"mulino.work.read.ObligationReferences").stream().anyMatch(x->rootId.equals(x.get("rootId"))&&"OPEN".equals(x.get("status"))&&!Instant.parse(x.get("recordedAt").toString()).isAfter(c.knownAt()));
 }

 /** Every SETTLEMENT_DIFFERENCE root of one invoice match: its own match difference and any contribution-change roots. */
 public List<String> matchRoots(DomainContext c,Map<String,Object> match){
  var out=new ArrayList<String>();if(match.get("dutyRootId")!=null)out.add(match.get("dutyRootId").toString());var json=new ObjectMapper();
  for(var root:r.externalRows(c,"mulino.responsibility.Roots")){if(!"SETTLEMENT_DIFFERENCE".equals(root.get("kind"))||out.contains(root.get("ID").toString()))continue;try{if(match.get("ID").toString().equals(json.readTree(root.get("scopeJson").toString()).path("residual").path("matchId").asText()))out.add(root.get("ID").toString());}catch(Exception e){throw DomainError.invalid("Settlement difference scope invalid");}}
  return out;
 }

 /** SETTLEMENT_DIFFERENCE roots opened for a CREDIT_NOTE/CORRECTION invoice, keyed by the correction invoice. */
 public Map<String,String> correctionRoots(DomainContext c){
  var out=new LinkedHashMap<String,String>();var json=new ObjectMapper();
  for(var root:r.externalRows(c,"mulino.responsibility.Roots")){if(!"SETTLEMENT_DIFFERENCE".equals(root.get("kind")))continue;try{var residual=json.readTree(root.get("scopeJson").toString()).path("residual");if(residual.hasNonNull("correctionInvoiceId"))out.put(residual.get("correctionInvoiceId").asText(),root.get("ID").toString());}catch(Exception e){throw DomainError.invalid("Settlement difference scope invalid");}}
  return out;
 }
}
