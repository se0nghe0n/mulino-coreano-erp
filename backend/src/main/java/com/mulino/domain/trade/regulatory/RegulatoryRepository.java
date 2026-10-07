package com.mulino.domain.trade.regulatory;
import com.mulino.application.core.*;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.stereotype.Repository;
@Repository
public class RegulatoryRepository {
 private final PersistenceService db;
 public RegulatoryRepository(PersistenceService db){this.db=db;}
 private String entity(String name){if(!Set.of("Policies","Procedures","ProcedureVersions","DecisionVersions","LabelVerifications").contains(name))throw DomainError.unsupported();return "mulino.trade.regulatory."+name;}
 public List<Map<String,Object>> rows(DomainContext c,String name){return db.run(Select.from(entity(name)).where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("recordedAt").le(c.knownAt())))).listOf(Map.class).stream().map(x->(Map<String,Object>)new LinkedHashMap<String,Object>(x)).toList();}
 public Map<String,Object> require(DomainContext c,String name,String id){return rows(c,name).stream().filter(x->id.equals(x.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);}
 public void insert(String name,Map<String,Object> row){db.run(Insert.into(entity(name)).entry(row));}
 public void advance(DomainContext c,String id,int revision,String version){long n=db.run(Update.entity(entity("Procedures")).data(Map.of("revision",revision+1,"currentVersionId",version)).where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("ID").eq(id)).and(x.get("revision").eq(revision)))).rowCount();if(n!=1)throw new DomainError("CONFLICT","STALE_REVISION","Regulatory procedure changed");}
}
