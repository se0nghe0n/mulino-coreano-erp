package com.mulino.application.trade.receipt;
import com.mulino.application.core.*;
import com.mulino.application.trade.ReceiptEvidenceScopePort;
import com.mulino.domain.inventory.*;
import com.mulino.domain.trade.receipt.ReceiptRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public final class ReceiptEvidenceScopes implements ReceiptEvidenceScopePort {
 private final ReceiptRepository r;private final InventoryRepository inventory;private final ReadAuthorizer auth;
 public ReceiptEvidenceScopes(ReceiptRepository r,InventoryRepository inventory,ReadAuthorizer auth){this.r=r;this.inventory=inventory;this.auth=auth;}
 public Scope require(DomainContext c,String id){
  inventory.fence(c,List.of("receipt/range/"+id));var rows=r.rows(c,"Observations").stream().filter(x->id.equals(x.get("rangeRootId"))).toList();if(rows.isEmpty())throw DomainError.forbidden();
  var first=rows.stream().filter(x->x.get("lotId")!=null).findFirst().orElse(rows.getFirst());for(var row:rows)if(!List.of("itemId","placeId","workId","quantity","unit","occurredAt","startQuantity","purchaseLineId","transitSegmentId").stream().allMatch(k->same(first.get(k),row.get(k))))throw new DomainError("HELD","EVIDENCE_CONFLICT","Physical receipt range has conflicting observations");
  if(rows.stream().anyMatch(x->x.get("lotId")!=null&&!Objects.equals(x.get("lotId"),first.get("lotId"))))throw new DomainError("HELD","EVIDENCE_CONFLICT","Physical receipt LOT claims conflict");
  auth.authorizeScopes(c,"getEvidence",Map.of("TARGET",List.of(id),"ITEM",List.of(first.get("itemId").toString()),"PLACE",List.of(first.get("placeId").toString()),"WORK",List.of(first.get("workId").toString())));
  if(first.get("lotId")==null)throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Receipt LOT is unidentified");var lot=inventory.current(c,"ManufacturingLots",first.get("lotId").toString());if(!first.get("itemId").equals(lot.get("itemId")))throw DomainError.invalid("Receipt LOT item mismatch");
  return new Scope(id,first.get("itemId").toString(),first.get("lotId").toString(),first.get("placeId").toString(),first.get("workId").toString(),(BigDecimal)first.get("quantity"),first.get("unit").toString(),StockPrimitives.instant(first.get("occurredAt")),(BigDecimal)first.get("startQuantity"));
 }
 private static boolean same(Object a,Object b){if(a instanceof BigDecimal x&&b instanceof BigDecimal y)return x.compareTo(y)==0;return Objects.equals(a,b);}
}
