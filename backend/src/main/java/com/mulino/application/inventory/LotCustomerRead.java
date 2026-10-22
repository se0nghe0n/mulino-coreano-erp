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
 private final InventoryRepository inventory;private final WorkReadRepository works;private final ReadAuthorizer auth;
 public LotCustomerRead(InventoryRepository inventory,WorkReadRepository works,ReadAuthorizer auth){this.inventory=inventory;this.works=works;this.auth=auth;}
 public List<String> customers(DomainContext c,Set<String> segments){
  var relations=inventory.rows(c,"ObjectRelations").stream().filter(r->!time(r.get("validFrom")).isAfter(c.asOf())&&(r.get("validUntil")==null||time(r.get("validUntil")).isAfter(c.asOf()))).toList();var visibleWorks=new HashSet<String>();
  for(var w:works.rows("Works",c))if(auth.permittedScopes(c,"traceLot",Map.of("TARGET",List.of(w.get("ID").toString()),"WORK",List.of(w.get("ID").toString()),"ITEM",List.of(w.get("itemId").toString()))))visibleWorks.add(w.get("ID").toString());
  var linked=new HashSet<String>();for(var r:relations)if("QuantitySegment".equals(r.get("sourceType"))&&"Work".equals(r.get("targetType"))&&segments.contains(r.get("sourceId"))&&visibleWorks.contains(r.get("targetId")))linked.add(r.get("targetId").toString());
  var customers=new TreeSet<String>();for(var r:relations)if("Work".equals(r.get("sourceType"))&&"Customer".equals(r.get("targetType"))&&linked.contains(r.get("sourceId")))customers.add(r.get("targetId").toString());return List.copyOf(customers);
 }
 private static Instant time(Object v){return v instanceof Instant i?i:Instant.parse(v.toString());}
}
