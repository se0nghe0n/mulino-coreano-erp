package com.mulino.application.trade.settlement;
import com.mulino.application.core.*;
import com.mulino.application.trade.TradeEvidenceScopePort;
import com.mulino.domain.evidence.EvidenceSubjectPort;
import com.mulino.domain.trade.settlement.SettlementRepository;
import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;
import static com.mulino.application.trade.settlement.SettlementInputs.*;
import static com.mulino.domain.trade.settlement.SettlementAmounts.*;
/** Exact original-to-claim proof; source semantics, immutable invoice identity and time are server checked. */
@Component
public class SettlementEvidence implements EvidenceSubjectPort,TradeEvidenceScopePort {
 private final SettlementRepository r;private final ExecutionClock clock;
 public SettlementEvidence(SettlementRepository r,ExecutionClock clock){this.r=r;this.clock=clock;}
 public Set<String> subjectKinds(){return Set.of("INVOICE");}
 public Set<String> eventKinds(){return Set.of("INVOICE","CHARGE","SETTLEMENT_ADJUSTMENT","PAYMENT_REFERENCE");}
 public Map<String,Object> require(String org,String kind,String id){return r.require(new DomainContext(org,"00000000-0000-0000-0000-000000000000","lookup",clock.instant(),clock.instant()),"Invoices",id);}
 public Scope require(DomainContext c,String physical,Map<String,Object> claim,Map<String,Object> event,Map<String,Object> document,byte[] bytes){try{var json=new com.fasterxml.jackson.databind.ObjectMapper();var original=json.readValue(bytes,Map.class);var payload=json.readValue(event.get("payload").toString(),Map.class);var invoice=r.require(c,"Invoices",physical);String kind=event.get("kind").toString();
  for(var source:List.of(claim,event)){if(!"INVOICE".equals(source.get("subjectKind"))||!physical.equals(source.get("subjectId"))||!invoice.get("itemId").equals(source.get("itemId")))throw held();}
  if(!invoice.get("itemId").equals(document.get("itemId")))throw held();
  var expected=new LinkedHashMap<String,Object>();if(kind.equals("INVOICE")){for(String k:List.of("lineId","referenceId","scopeKind","invoiceKind","itemId","quantity","unit","unitPrice","originalAmount","currency","sourceNamespace","sourceKey","sourceVersion"))expected.put(k,invoice.get(k));if(invoice.get("relatedInvoiceId")!=null)expected.put("relatedInvoiceId",invoice.get("relatedInvoiceId"));if(invoice.get("fxPair")==null&&(original.get("fxSnapshot")!=null||payload.get("fxSnapshot")!=null))throw held();if(!invoice.get("sourceHash").equals(document.get("sha256")))throw held();if(!(claim.get("quantity") instanceof BigDecimal q)||q.compareTo(decimal(invoice.get("quantity")))!=0||!invoice.get("unit").equals(claim.get("unit")))throw held();if(invoice.get("fxPair")!=null){var fx=map(original.get("fxSnapshot"));var f=fx(invoice.get("currency").toString(),decimal(invoice.get("originalAmount")),fx);if(!f.pair().equals(invoice.get("fxPair"))||f.rate().compareTo(decimal(invoice.get("fxRate")))!=0||!f.date().toString().equals(invoice.get("fxDate").toString())||!f.source().equals(invoice.get("fxSource"))||!f.policyVersion().equals(invoice.get("fxPolicyVersion"))||!f.rounding().equals(invoice.get("fxRounding"))||f.convertedAmount().compareTo(decimal(invoice.get("convertedAmount")))!=0||!encode(fx).equals(encode(payload.get("fxSnapshot"))))throw held();}}
  else {if(claim.get("quantity")!=null||claim.get("unit")!=null)throw held();expected.put("invoiceId",physical);expected.put("amount",decimal(original.get("amount")));expected.put("currency",currency(original.get("currency")));switch(kind){case "CHARGE"->expected.put("kind",text(original,"kind"));case "SETTLEMENT_ADJUSTMENT"->{expected.put("mode",text(original,"mode"));expected.put("reason",text(original,"reason"));if("CONFIRM".equals(original.get("mode")))for(String k:List.of("proposalId","proposalHash","policyVersion"))expected.put(k,text(original,k));}case "PAYMENT_REFERENCE"->{expected.put("externalPaymentReference",text(original,"externalPaymentReference"));expected.put("observedAt",Instant.parse(text(original,"observedAt")));}default->throw held();}}
  Instant at=Instant.parse(text(original,"occurredAt"));expected.put("occurredAt",at);if(!at.equals(Instant.parse(claim.get("effectiveFrom").toString()))||!at.equals(Instant.parse(event.get("effectiveFrom").toString()))||at.isAfter(c.knownAt()))throw held();SettlementCommands.requireFields(original,expected);SettlementCommands.requireFields(payload,expected);
  String occurrenceKey=kind.equals("INVOICE")?invoice.get("sourceNamespace")+"|"+invoice.get("sourceKey")+"|"+invoice.get("sourceVersion"):kind+"|"+text(original,"externalEventId");String occurrence=UUID.nameUUIDFromBytes(occurrenceKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();var normalized=new TreeMap<String,Object>();expected.forEach((k,v)->normalized.put(k,v instanceof BigDecimal d?d.stripTrailingZeros().toPlainString():v.toString()));String semanticHash=CommandRequests.hash(normalized);
  return new Scope(Map.of("itemId",invoice.get("itemId"),"workId",invoice.get("workId")),Map.of("ITEM",List.of(invoice.get("itemId").toString()),"WORK",List.of(invoice.get("workId").toString()),"TARGET",List.of(physical,invoice.get("lineId").toString())),occurrence,semanticHash);
 }catch(java.io.IOException|IllegalArgumentException|NullPointerException e){throw held();}}
}
