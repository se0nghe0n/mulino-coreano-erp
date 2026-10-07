package com.mulino.domain.inventory;
import com.mulino.application.core.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
/** Receipt physical effects occur here only, under the gateway transaction. */
@Component
public final class ReceiptStockPrimitives {
 private final InventoryRepository r; private final StockPrimitives stock;
 public ReceiptStockPrimitives(InventoryRepository r,StockPrimitives stock){this.r=r;this.stock=stock;}
 public String receive(DomainContext c,Map<String,Object> receipt,String evidence,String command){
  String item=receipt.get("itemId").toString(),lot=receipt.get("lotId").toString(),place=receipt.get("placeId").toString();Instant at=StockPrimitives.instant(receipt.get("occurredAt"));
  var itemRow=r.current(c,"TradeItems",item);var lotRow=r.current(c,"ManufacturingLots",lot);var destination=r.current(c,"Places",place);
  if(!item.equals(lotRow.get("itemId"))||!receipt.get("unit").equals(itemRow.get("baseUnit"))||!"INTERNAL_STORAGE".equals(destination.get("kind")))throw DomainError.invalid("Receipt item LOT unit or destination mismatch");
  BigDecimal q=InventoryQuantity.parse(receipt.get("quantity").toString(),((Number)itemRow.get("decimalPlaces")).intValue());
  var fences=new TreeSet<String>(List.of("inventory/item/"+item,"inventory/place/"+place,"receipt/range/"+receipt.get("rangeRootId")));
  String transit=Objects.toString(receipt.get("transitSegmentId"),null);if(transit!=null)fences.addAll(stock.fences(r.current(c,"QuantitySegments",transit)));r.fence(c,fences);
  Map<String,Object> source=null;
  if(transit!=null){source=stock.leaf(c,transit,at);if(!item.equals(source.get("itemId"))||!lot.equals(source.get("lotId"))||!receipt.get("unit").equals(source.get("unit"))||!"CONFIRMED".equals(source.get("identificationStatus"))||!"TRANSIT".equals(r.current(c,"Places",source.get("placeId").toString()).get("kind"))||q.compareTo(StockPrimitives.amount(source))!=0)throw DomainError.invalid("Exact identified transit leaf required; split partial cargo first");if(r.currentRows(c,"SegmentAllocations").stream().anyMatch(a->transit.equals(a.get("segmentId"))&&Set.of("EXECUTABLE","SUSPENDED").contains(a.get("state"))))throw DomainError.invalid("Receipt cannot consume active allocation");}
  var target=StockPrimitives.row(c,StockPrimitives.id(),at);target.putAll(Map.of("itemId",item,"lotId",lot,"placeId",place,"quantity",q,"unit",receipt.get("unit"),"identificationStatus","CONFIRMED","mixtureStatus","IDENTIFIED","controlScope","receipt/"+receipt.get("rangeRootId"),"validFrom",at,"evidenceRef",evidence));if(source!=null){target.put("ownerId",source.get("ownerId"));target.put("custodianId",source.get("custodianId"));target.put("controlScope",source.get("controlScope"));target.put("mixtureStatus",source.get("mixtureStatus"));}r.insert("QuantitySegments",target);
  if(source!=null){r.update(c,"QuantitySegments",transit,Map.of("retiredAt",at,"retirementRecordedAt",c.knownAt(),"revision",((Number)source.get("revision")).intValue()+1));var edge=StockPrimitives.row(c,StockPrimitives.id(),at);edge.putAll(Map.of("sourceId",transit,"targetId",target.get("ID"),"quantity",q,"unit",receipt.get("unit"),"kind","RECEIPT_MOVE","uncertain",false,"occurredAt",at,"evidenceRef",evidence));r.insert("GenealogyEdges",edge);for(var membership:r.currentRows(c,"LogisticsMemberships"))if(transit.equals(membership.get("segmentId"))&&membership.get("validUntil")==null){if(!at.isAfter(StockPrimitives.instant(membership.get("validFrom"))))throw DomainError.invalid("Receipt must follow cargo membership start");r.update(c,"LogisticsMemberships",membership.get("ID").toString(),Map.of("validUntil",at,"revision",((Number)membership.get("revision")).intValue()+1));var next=StockPrimitives.row(c,StockPrimitives.id(),at);next.putAll(Map.of("segmentId",target.get("ID"),"logisticsUnitId",membership.get("logisticsUnitId"),"validFrom",at));r.insert("LogisticsMemberships",next);}}
  var movement=StockPrimitives.row(c,StockPrimitives.id(),at);movement.putAll(Map.of("targetId",target.get("ID"),"quantity",q,"unit",receipt.get("unit"),"kind",source==null?"RECEIPT":"RECEIPT_MOVE","occurredAt",at,"evidenceRef",evidence,"commandId",command));if(source!=null)movement.put("sourceId",transit);r.insert("QuantityMovements",movement);return target.get("ID").toString();
 }
}
