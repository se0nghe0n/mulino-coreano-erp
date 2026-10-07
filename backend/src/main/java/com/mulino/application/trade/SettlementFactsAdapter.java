package com.mulino.application.trade;

import com.mulino.application.core.*;
import com.mulino.application.trade.purchase.PurchaseLinePort;
import com.mulino.application.trade.settlement.SettlementTradeFacts;
import com.mulino.domain.trade.purchase.PurchaseRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;

/** Immutable trade revisions and applied contributions, shared by settlement without stock writes. */
@Component
public final class SettlementFactsAdapter implements SettlementTradeFacts {
  private final PurchaseLinePort purchase;
  private final PurchaseRepository repository;
  private final SalesOrderLinePort sales;
  private final SalesDeliveryCreditPort deliveries;
  private final SalesExecutionPort execution;
  public SettlementFactsAdapter(PurchaseLinePort purchase,PurchaseRepository repository,
      SalesOrderLinePort sales,SalesDeliveryCreditPort deliveries,SalesExecutionPort execution){
    this.purchase=purchase;this.repository=repository;this.sales=sales;this.deliveries=deliveries;this.execution=execution;
  }
  public Map<String,Object> line(DomainContext c,String kind,String id){
    if("SALE".equals(kind))return sales.requireLine(c,id);
    purchaseKind(kind);
    var line=new LinkedHashMap<>(purchase.requireLine(c,id));
    var revision=repository.rows(c,"ProposalRevisions").stream()
      .filter(r->Objects.equals(r.get("proposalId"),line.get("proposalId"))&&Objects.equals(r.get("revision"),line.get("proposalRevision")))
      .findFirst().orElseThrow(DomainError::forbidden);
    for(String key:List.of("price","currency","supplierId"))line.put(key,revision.get(key));
    return line;
  }
  public Map<String,Object> contribution(DomainContext c,String kind,String lineId,String referenceId){
    if("SALE".equals(kind)){var credit=deliveries.deliveryCredit(c,lineId,referenceId);return Map.of("quantity",credit.get("actualQuantity"),"recognizedQuantity",credit.get("contributedQuantity"),"unit",credit.get("unit"),"occurrenceId",credit.get("canonicalOccurrenceId"),"referenceId",referenceId);}
    purchaseKind(kind);
    var credit=purchase.receiptCredit(c,lineId,referenceId);
    return Map.of("quantity",credit.get("actualQuantity"),"recognizedQuantity",credit.get("contributedQuantity"),"unit",credit.get("unit"),
      "occurrenceId",credit.get("occurrenceId"),"referenceId",referenceId);
  }
  public void invoiced(DomainContext c,String kind,String lineId,String invoiceId,BigDecimal q,String unit){
    if("SALE".equals(kind))execution.recordExecutionEffect(c,lineId,"INVOICED",invoiceId,q,unit);
    else {purchaseKind(kind);purchase.recordExecutionEffect(c,lineId,"INVOICED",invoiceId,q,unit);}
  }
  private static void purchaseKind(String kind){if(!"PURCHASE".equals(kind))throw DomainError.invalid("Unknown settlement trade scope");}
}
