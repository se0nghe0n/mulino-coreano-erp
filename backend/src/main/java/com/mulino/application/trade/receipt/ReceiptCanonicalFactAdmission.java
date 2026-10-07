package com.mulino.application.trade.receipt;
import com.mulino.application.core.*;
import com.mulino.domain.evaluation.CanonicalFactAdmission;
import com.mulino.domain.inventory.*;
import com.mulino.domain.trade.receipt.ReceiptRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;
/** Verified observations become goal facts only after the receipt physical transaction commits. */
@Component
public final class ReceiptCanonicalFactAdmission implements CanonicalFactAdmission {
 private final ReceiptRepository r;private final InventoryRepository inventory;private final ReadAuthorizer auth;private final com.mulino.application.trade.purchase.PurchaseLinePort purchase;
 public ReceiptCanonicalFactAdmission(ReceiptRepository r,InventoryRepository inventory,ReadAuthorizer auth,com.mulino.application.trade.purchase.PurchaseLinePort purchase){this.r=r;this.inventory=inventory;this.auth=auth;this.purchase=purchase;}
 public Set<String> eventKinds(){return Set.of("PHYSICAL_RECEIPT");}
 public Admission admit(DomainContext c,String workId,String goalId,Map<String,Object> occurrence){
  String id=occurrence.get("ID").toString();var rows=r.rows(c,"Receipts").stream().filter(x->id.equals(x.get("canonicalOccurrenceId"))).toList();
  if(rows.size()!=1)return denied();var receipt=rows.getFirst();
  if(!Objects.equals(receipt.get("workId"),occurrence.get("workId"))||!Objects.equals(receipt.get("itemId"),occurrence.get("itemId"))||!Objects.equals(receipt.get("placeId"),occurrence.get("placeId"))||!Objects.equals(receipt.get("rangeRootId"),occurrence.get("physicalScopeId"))||!Objects.equals(receipt.get("unit"),occurrence.get("unit"))||!(occurrence.get("quantity") instanceof BigDecimal q)||q.compareTo((BigDecimal)receipt.get("quantity"))!=0||!StockPrimitives.instant(receipt.get("occurredAt")).equals(StockPrimitives.instant(occurrence.get("effectiveFrom")))||StockPrimitives.instant(receipt.get("occurredAt")).isAfter(c.asOf()))return denied();
  if(!auth.permittedScopes(c,"assessGoal",Map.of("TARGET",List.of(workId),"WORK",new ArrayList<>(new LinkedHashSet<>(List.of(workId,receipt.get("workId").toString()))),"ITEM",List.of(receipt.get("itemId").toString()),"PLACE",List.of(receipt.get("placeId").toString()))))return denied();
  var segment=inventory.object(c,"QuantitySegments",receipt.get("segmentId").toString());
  if(!Objects.equals(segment.get("itemId"),receipt.get("itemId"))||!Objects.equals(segment.get("lotId"),receipt.get("lotId"))||!Objects.equals(segment.get("unit"),receipt.get("unit"))||((BigDecimal)segment.get("quantity")).compareTo((BigDecimal)receipt.get("quantity"))!=0)return denied();
  var movements=inventory.rows(c,"QuantityMovements").stream().filter(x->Objects.equals(segment.get("ID"),x.get("targetId"))&&id.equals(x.get("evidenceRef"))&&Set.of("RECEIPT","RECEIPT_MOVE").contains(x.get("kind"))).toList();if(movements.size()!=1||((BigDecimal)movements.getFirst().get("quantity")).compareTo((BigDecimal)receipt.get("quantity"))!=0)return denied();
  BigDecimal recognized=(BigDecimal)receipt.get("quantity");if(receipt.get("purchaseLineId")!=null){var line=purchase.requireLine(c,receipt.get("purchaseLineId").toString());var credit=purchase.receiptCredit(c,line.get("ID").toString(),id);if(!Objects.equals(receipt.get("workId"),line.get("workId"))||!Objects.equals(receipt.get("itemId"),line.get("itemId"))||!Objects.equals(receipt.get("placeId"),line.get("destinationId"))||!Objects.equals(receipt.get("unit"),credit.get("unit"))||((BigDecimal)credit.get("actualQuantity")).compareTo((BigDecimal)receipt.get("quantity"))!=0||((BigDecimal)credit.get("contributedQuantity")).compareTo((BigDecimal)receipt.get("contributedQuantity"))!=0)return denied();recognized=(BigDecimal)credit.get("contributedQuantity");}
  return new Admission(true,recognized,receipt.get("lotId").toString(),List.of(receipt.get("ID").toString(),segment.get("ID").toString(),movements.getFirst().get("ID").toString()));
 }
 private static Admission denied(){return new Admission(false,null,null,List.of());}
}
