package com.mulino.domain.inventory;

import com.mulino.application.core.DomainContext;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;

/** Exact ancestor-to-descendant coordinates. Missing mappings never create permission. */
@Component
public final class PhysicalRanges {
  private final InventoryRepository repository;
  public PhysicalRanges(InventoryRepository repository){this.repository=repository;}
  public List<QualityRanges.Range> project(DomainContext c,String ancestor,String descendant,BigDecimal start,BigDecimal quantity){
    return project(repository.rows(c,"GenealogyEdges"),ancestor,descendant,start,quantity);
  }
  public static List<QualityRanges.Range> project(List<Map<String,Object>> edges,String ancestor,String descendant,BigDecimal start,BigDecimal quantity){
    if(quantity.signum()<=0)return List.of();
    return walk(edges,ancestor,descendant,List.of(new QualityRanges.Range(start,start.add(quantity))),new HashSet<>());
  }
  private static List<QualityRanges.Range> walk(List<Map<String,Object>> edges,String source,String target,List<QualityRanges.Range> ranges,Set<String> path){
    if(source.equals(target))return ranges;
    if(!path.add(source))throw new IllegalStateException("Cyclic physical genealogy");
    var out=new ArrayList<QualityRanges.Range>();
    for(var edge:edges)if(source.equals(edge.get("sourceId"))&&!Boolean.TRUE.equals(edge.get("uncertain"))&&edge.get("sourceStartQuantity") instanceof BigDecimal from&&edge.get("targetStartQuantity") instanceof BigDecimal to){
      var q=(BigDecimal)edge.get("quantity");
      var overlap=QualityRanges.intersect(ranges,List.of(new QualityRanges.Range(from,from.add(q))));
      var mapped=overlap.stream().map(r->new QualityRanges.Range(r.start().subtract(from).add(to),r.end().subtract(from).add(to))).toList();
      if(!mapped.isEmpty())out.addAll(walk(edges,(String)edge.get("targetId"),target,mapped,new HashSet<>(path)));
    }
    return QualityRanges.union(out);
  }
}
