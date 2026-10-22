package com.mulino.application.inventory;
import com.mulino.application.core.*;
import com.mulino.domain.inventory.InventoryRepository;
import com.mulino.domain.work.read.WorkReadRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
/** Candidate reachability is a typed physical-subject/customer relation, not contamination. */
@Component
public final class LotCustomerRead {
 private final InventoryRepository inventory;private final WorkReadRepository works;private final ReadAuthorizer auth;private final com.mulino.domain.inventory.ReadRelationRepository meanings;private final com.mulino.domain.trade.sales.SalesRepository sales;
 public LotCustomerRead(InventoryRepository inventory,WorkReadRepository works,ReadAuthorizer auth,com.mulino.domain.inventory.ReadRelationRepository meanings,com.mulino.domain.trade.sales.SalesRepository sales){this.inventory=inventory;this.works=works;this.auth=auth;this.meanings=meanings;this.sales=sales;}
 public List<String> customers(DomainContext c,Set<String> segments){
  var names=meanings.names(c);
  var relations=inventory.rows(c,"ObjectRelations").stream().filter(r->!time(r.get("validFrom")).isAfter(c.asOf())&&(r.get("validUntil")==null||time(r.get("validUntil")).isAfter(c.asOf()))).toList();var visibleWorks=new HashSet<String>();var workItems=new HashMap<String,Object>();var segmentItems=new HashMap<Object,Object>();
  for(var segment:inventory.rows(c,"QuantitySegments"))if(segments.contains(segment.get("ID")))segmentItems.put(segment.get("ID"),segment.get("itemId"));
  for(var w:works.rows("Works",c))if(auth.permittedScopes(c,"traceLot",Map.of("TARGET",List.of(w.get("ID").toString()),"WORK",List.of(w.get("ID").toString()),"ITEM",List.of(w.get("itemId").toString())))){visibleWorks.add(w.get("ID").toString());workItems.put(w.get("ID").toString(),w.get("itemId"));}
  var linked=new HashSet<String>();for(var r:relations)if("SUBJECT_OF".equals(names.get(r.get("relationDefinitionId")))&&"QuantitySegment".equals(r.get("sourceType"))&&"Work".equals(r.get("targetType"))&&segments.contains(r.get("sourceId"))&&visibleWorks.contains(r.get("targetId"))&&Objects.equals(segmentItems.get(r.get("sourceId")),workItems.get(r.get("targetId"))))linked.add(r.get("targetId").toString());
  var customers=new TreeSet<String>();for(var r:relations)if("CUSTOMER".equals(names.get(r.get("relationDefinitionId")))&&"Work".equals(r.get("sourceType"))&&"Customer".equals(r.get("targetType"))&&linked.contains(r.get("sourceId"))){String id=r.get("targetId").toString();if(sales.rows(c,"Customers").stream().anyMatch(customer->id.equals(customer.get("ID"))))customers.add(id);}return List.copyOf(customers);
 }
 public boolean permittedEndpoint(DomainContext c,String capability,String type,String id,String item){
  if(!type.equals("Work")||item==null)return false;
  return works.rows("Works",c).stream().anyMatch(w->id.equals(w.get("ID"))&&item.equals(w.get("itemId"))&&auth.permittedScopes(c,capability,Map.of("TARGET",List.of(id),"WORK",List.of(id),"ITEM",List.of(item))));
 }
 private static Instant time(Object v){return v instanceof Instant i?i:Instant.parse(v.toString());}
}
