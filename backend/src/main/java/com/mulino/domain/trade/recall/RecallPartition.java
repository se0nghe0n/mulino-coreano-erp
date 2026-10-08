package com.mulino.domain.trade.recall;
import com.mulino.application.core.DomainError;
import com.mulino.domain.inventory.QualityRanges;
import java.math.BigDecimal;
import java.util.*;
/** Recovered is location/control progress, never another final-disposition credit. */
public final class RecallPartition {
 private RecallPartition(){}
 public static final List<String> TERMINAL=List.of("SAFE","DISPOSED","CONSUMED_LOST","EXCEPTION");
 public static Map<String,BigDecimal> quantities(BigDecimal start,BigDecimal q,List<Map<String,Object>> actions){
  var full=List.of(new QualityRanges.Range(start,start.add(q)));var occupied=new ArrayList<QualityRanges.Range>();var result=new LinkedHashMap<String,BigDecimal>();
  for(String kind:TERMINAL){var ranges=actions.stream().filter(x->kind.equals(x.get("kind"))).map(RecallPartition::range).toList();
   if(QualityRanges.quantity(ranges).compareTo(ranges.stream().map(x->x.end().subtract(x.start())).reduce(BigDecimal.ZERO,BigDecimal::add))!=0||!QualityRanges.intersect(occupied,ranges).isEmpty()||!QualityRanges.subtract(ranges,full).isEmpty())throw new DomainError("HELD","RECALL_PARTITION_CONFLICT","Final recall partitions overlap or exceed scope");
   occupied.addAll(ranges);result.put(kind,QualityRanges.quantity(ranges));
  }
  result.put("UNKNOWN",QualityRanges.quantity(QualityRanges.subtract(full,occupied)));
  result.put("RECOVERED",QualityRanges.quantity(actions.stream().filter(x->"RECOVERED".equals(x.get("kind"))).map(RecallPartition::range).toList()));
  result.put("ACCOUNTED",q.subtract(result.get("UNKNOWN")));return Collections.unmodifiableMap(result);
 }
 /** Investigation facts projected onto one scope version: parts outside the version are not counted, never dropped. */
 public static List<Map<String,Object>> clip(BigDecimal start,BigDecimal q,List<Map<String,Object>> actions){
  var full=new QualityRanges.Range(start,start.add(q));var out=new ArrayList<Map<String,Object>>();
  for(var a:actions){var r=range(a);var lo=r.start().max(full.start());var hi=r.end().min(full.end());if(lo.compareTo(hi)>=0)continue;var copy=new LinkedHashMap<String,Object>(a);copy.put("startQuantity",lo);copy.put("quantity",hi.subtract(lo));out.add(copy);}
  return out;
 }
 public static QualityRanges.Range range(Map<String,Object>x){var s=(BigDecimal)x.get("startQuantity");return new QualityRanges.Range(s,s.add((BigDecimal)x.get("quantity")));}
}
