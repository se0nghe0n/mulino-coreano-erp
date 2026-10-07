package com.mulino.domain.inventory;
import com.mulino.application.core.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;
/** Relocate known existing customer physical stock; never synthesize inventory. */
@Component
public class ReturnStockPrimitives {
 private final InventoryRepository r;private final StockPrimitives stock;private final PhysicalRanges ranges;private final QualityPrimitives quality;
 public ReturnStockPrimitives(InventoryRepository r,StockPrimitives stock,PhysicalRanges ranges,QualityPrimitives quality){this.r=r;this.stock=stock;this.ranges=ranges;this.quality=quality;}
 public Map<String,String> receive(DomainContext c,Map<String,Object>receipt,Map<String,Object>delivery,String evidence,String command,String policyHash){
  String destination=receipt.get("placeId").toString();if(!"INTERNAL_STORAGE".equals(r.current(c,"Places",destination).get("kind")))throw DomainError.invalid("Return destination must be internal storage");
  var at=StockPrimitives.instant(receipt.get("occurredAt"));var q=(BigDecimal)receipt.get("quantity");var start=((BigDecimal)receipt.get("startQuantity")).subtract((BigDecimal)delivery.get("startQuantity"));var ancestor=delivery.get("segmentId").toString();
  var fences=new TreeSet<String>(List.of("inventory/item/"+receipt.get("itemId"),"return/range/"+receipt.get("rangeRootId"),"inventory/place/"+destination));
  for(var segment:r.currentRows(c,"QuantitySegments"))if(segment.get("retiredAt")==null&&receipt.get("itemId").equals(segment.get("itemId")))fences.addAll(stock.fences(segment));r.fence(c,fences);
  var matches=new ArrayList<Map<String,Object>>();var local=new HashMap<String,BigDecimal>();
  for(var segment:r.currentRows(c,"QuantitySegments")){if(segment.get("retiredAt")!=null||!receipt.get("itemId").equals(segment.get("itemId"))||!receipt.get("lotId").equals(segment.get("lotId"))||!receipt.get("unit").equals(segment.get("unit"))||!"CONFIRMED".equals(segment.get("identificationStatus"))||"UNCERTAIN_MIXTURE".equals(segment.get("mixtureStatus"))||!delivery.get("placeId").equals(segment.get("placeId")))continue;
   var projected=ranges.project(c,ancestor,segment.get("ID").toString(),start,q);if(projected.size()==1&&projected.getFirst().end().subtract(projected.getFirst().start()).compareTo(q)==0){matches.add(segment);local.put(segment.get("ID").toString(),projected.getFirst().start());}}
  if(matches.size()!=1)throw new DomainError("HELD","IDENTITY_UNRESOLVED","Return must match one existing exact customer leaf; reconcile partial or uncertain identity first");
  var source=matches.getFirst();String segment=stock.transferRange(c,source.get("ID").toString(),local.get(source.get("ID").toString()),q,destination,at,evidence,command,"RETURN_MOVE");
  var hold=new LinkedHashMap<String,Object>();hold.putAll(Map.of("segmentId",segment,"controlScope",r.current(c,"QuantitySegments",segment).get("controlScope"),"action","ALL","category","QC","state","ACTIVE","startQuantity",BigDecimal.ZERO,"quantity",q,"unit",receipt.get("unit"),"validFrom",at));hold.put("validUntil",java.time.Instant.parse("9999-12-31T23:59:59Z"));hold.put("policyHash",policyHash);hold.put("workId",receipt.get("workId"));hold.put("nextCheckAt",receipt.get("nextCheckAt"));hold.put("evidenceRef",evidence);hold.put("commandId",command);
  var restriction=quality.record(c,"Restrictions",hold);return Map.of("segmentId",segment,"restrictionId",restriction.get("ID").toString());
 }
}
