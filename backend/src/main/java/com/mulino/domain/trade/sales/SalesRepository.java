package com.mulino.domain.trade.sales;
import com.mulino.application.core.*;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class SalesRepository {
 private final PersistenceService db;private final JdbcTemplate jdbc;
 public SalesRepository(PersistenceService db,JdbcTemplate jdbc){this.db=db;this.jdbc=jdbc;}
 private String entity(String e){if(!Set.of("Customers","Orders","OrderRevisions","OrderLines","Observations","Deliveries","ExecutionEffects","DeliveryCorrections").contains(e))throw DomainError.unsupported();return "mulino.trade.sales."+e;}
 public List<Map<String,Object>> rows(DomainContext c,String e){return db.run(Select.from(entity(e)).where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("recordedAt").le(c.knownAt())))).listOf(Map.class).stream().map(x->(Map<String,Object>)x).toList();}
 public Map<String,Object> require(DomainContext c,String e,String id){return rows(c,e).stream().filter(x->id.equals(x.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);}
 public Map<String,Object> subject(String org,String e,String id){return db.run(Select.from(entity(e)).where(x->x.get("organizationId").eq(org).and(x.get("ID").eq(id)))).first().map(x->(Map<String,Object>)new LinkedHashMap<String,Object>(x)).orElseThrow(DomainError::forbidden);}
 public void insert(String e,Map<String,Object> row){db.run(Insert.into(entity(e)).entry(row));}
 public void update(DomainContext c,String e,String id,Map<String,Object> row){db.run(Update.entity(entity(e)).data(row).where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("ID").eq(id))));}
 /** Same lock namespace (organization:key) as the gateway and the other repositories, so one key names exactly one lock (plan §4.2). */
 public void fence(DomainContext c,String key){jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",c.organizationId()+":"+key);}
}
