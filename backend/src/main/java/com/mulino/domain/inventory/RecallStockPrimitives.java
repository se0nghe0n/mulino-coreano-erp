package com.mulino.domain.inventory;
import com.mulino.application.core.*;
import com.mulino.application.quality.QualityEligibility;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
/** Domain authority is checked by recall service; no public raw ledger path. */
@Component
public final class RecallStockPrimitives {
 private final InventoryRepository r;private final StockPrimitives stock;private final QualityPrimitives quality;private final PhysicalRanges ranges;
 public RecallStockPrimitives(InventoryRepository r,StockPrimitives stock,QualityPrimitives quality,PhysicalRanges ranges){this.r=r;this.stock=stock;this.quality=quality;this.ranges=ranges;}
 public List<String> investigate(DomainContext c,String root,BigDecimal start,BigDecimal q,String work,String evidence,Instant next,String policyHash){
  var s=r.current(c,"QuantitySegments",root);r.fence(c,stock.fences(s));s=r.current(c,"QuantitySegments",root);var ids=new ArrayList<String>();
  for(String action:List.of("SELL","PICK","DISPATCH")){var v=new LinkedHashMap<String,Object>();v.putAll(Map.of("segmentId",root,"controlScope",s.get("controlScope"),"startQuantity",start,"quantity",q,"unit",s.get("unit"),"category","RECALL_INVESTIGATION","action",action,"state","ACTIVE","validFrom",c.asOf()));v.putAll(Map.of("evidenceRef",evidence,"workId",work,"nextCheckAt",next,"commandId",CommandExecution.commandId(),"policyHash",policyHash,"validUntil",Instant.parse("9999-12-31T23:59:59Z")));ids.add(quality.record(c,"Restrictions",v).get("ID").toString());}
  for(var leaf:r.currentRows(c,"QuantitySegments"))if(leaf.get("retiredAt")==null&&!ranges.project(c,root,leaf.get("ID").toString(),start,q).isEmpty())quality.suspend(c,leaf.get("ID").toString(),"RECALL_INVESTIGATION");return ids;
 }
 public String recover(DomainContext c,String root,BigDecimal start,BigDecimal q,String segment,String place,Instant at,String evidence){
  var projected=requireRange(c,root,start,q,segment);return stock.transferRange(c,segment,projected.start(),q,place,at,evidence,CommandExecution.commandId(),"RECALL_RECOVERY");
 }
 public void dispose(DomainContext c,String root,BigDecimal start,BigDecimal q,String segment,Instant at,String evidence){var projected=requireRange(c,root,start,q,segment);
  // QC restrictions are independently controlled; ADMIN recall approval never supplies QC permission.
  for(var restriction:r.currentRows(c,"Restrictions"))if(QualityEligibility.active(restriction,c.knownAt(),c.knownAt())&&!"RECALL_INVESTIGATION".equals(restriction.get("category"))&&Objects.equals(r.current(c,"QuantitySegments",segment).get("controlScope"),restriction.get("controlScope"))){
   if(restriction.get("segmentId")==null)throw new DomainError("HELD","INDEPENDENT_DISPOSITION_RESTRICTION","Independent scope restriction still active");
   var covered=ranges.project(c,restriction.get("segmentId").toString(),segment,(BigDecimal)restriction.get("startQuantity"),(BigDecimal)restriction.get("quantity"));if(!QualityRanges.intersect(covered,List.of(projected)).isEmpty())throw new DomainError("HELD","INDEPENDENT_DISPOSITION_RESTRICTION","Independent QC or authority restriction requires its own current decision");
  }
  stock.disposeRange(c,segment,projected.start(),q,at,evidence,CommandExecution.commandId());
 }
 private QualityRanges.Range requireRange(DomainContext c,String root,BigDecimal start,BigDecimal q,String segment){var s=r.current(c,"QuantitySegments",segment);var rootRow=r.current(c,"QuantitySegments",root);var fences=new TreeSet<>(stock.fences(s));fences.addAll(stock.fences(rootRow));r.fence(c,fences);s=r.current(c,"QuantitySegments",segment);if(!Objects.equals(rootRow.get("itemId"),s.get("itemId"))||!Objects.equals(rootRow.get("lotId"),s.get("lotId"))||!Objects.equals(rootRow.get("unit"),s.get("unit"))||s.get("retiredAt")!=null)throw DomainError.invalid("Current recall physical leaf required");var mapped=ranges.project(c,root,segment,start,q);if(mapped.size()!=1||QualityRanges.quantity(mapped).compareTo(q)!=0)throw DomainError.invalid("Recall interval must map to one exact current physical subset");return mapped.getFirst();}
}
