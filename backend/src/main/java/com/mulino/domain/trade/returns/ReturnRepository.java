package com.mulino.domain.trade.returns;
import com.mulino.application.core.*;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.stereotype.Repository;
@Repository
public class ReturnRepository {
 private final PersistenceService db;
 public ReturnRepository(PersistenceService db){this.db=db;}
 private String entity(String e){if(!Set.of("Authorizations","Observations","Receipts","Dispositions").contains(e))throw DomainError.unsupported();return "mulino.trade.returns."+e;}
 public List<Map<String,Object>> rows(DomainContext c,String e){return currentRows(c,e).stream().filter(x->!com.mulino.domain.inventory.StockPrimitives.instant(x.get("recordedAt")).isAfter(c.knownAt())).toList();}
 public List<Map<String,Object>> currentRows(DomainContext c,String e){return db.run(Select.from(entity(e)).where(x->x.get("organizationId").eq(c.organizationId()))).listOf(Map.class).stream().map(x->(Map<String,Object>)x).toList();}
 public Map<String,Object> require(DomainContext c,String e,String id){return rows(c,e).stream().filter(x->id.equals(x.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);}
 public void insert(String e,Map<String,Object> row){db.run(Insert.into(entity(e)).entry(row));}
 public void confirmed(DomainContext c,String id){db.run(Update.entity(entity("Observations")).data(Map.of("state","CONFIRMED","revision",1)).where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("ID").eq(id))));}
}
