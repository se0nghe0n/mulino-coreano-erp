package com.mulino.domain.governance;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
@Repository
public class PolicyRepository {
 private final PersistenceService db; private final JdbcTemplate jdbc;
 public PolicyRepository(PersistenceService db,JdbcTemplate jdbc){this.db=db;this.jdbc=jdbc;}
 public List<Map<String,Object>> rows(String entity,String org){return db.run(Select.from("mulino.governance."+entity).where(x->x.get("organizationId").eq(org))).listOf(Map.class).stream().map(x->(Map<String,Object>)x).toList();}
 public Optional<Map<String,Object>> row(String entity,String org,String id){return rows(entity,org).stream().filter(x->id.equals(x.get("ID"))).findFirst();}
 public void insert(String entity,Map<String,Object> row){db.run(Insert.into("mulino.governance."+entity).entry(row));}
 public void update(String entity,String org,String id,Map<String,Object> data){db.run(Update.entity("mulino.governance."+entity).data(data).where(x->x.get("organizationId").eq(org).and(x.get("ID").eq(id))));}
 public void activate(String org,String kind,String id){
   db.run(Delete.from("mulino.governance.ActivePolicies").where(x->x.get("organizationId").eq(org).and(x.get("kind").eq(kind))));
   insert("ActivePolicies",Map.of("organizationId",org,"kind",kind,"policyId",id,"revision",1));
 }
 public void retire(String org,String id){db.run(Delete.from("mulino.governance.ActivePolicies").where(x->x.get("organizationId").eq(org).and(x.get("policyId").eq(id))));}
 public List<Map<String,Object>> current(String org,String kind,Instant at){
   var pointers=rows("ActivePolicies",org).stream().filter(x->kind.equals(x.get("kind"))).map(x->x.get("policyId")).toList();
   return rows("PolicyVersions",org).stream().filter(x->pointers.contains(x.get("ID"))&&kind.equals(x.get("kind"))&&!at.isBefore(instant(x.get("effectiveFrom")))&&(x.get("effectiveUntil")==null||at.isBefore(instant(x.get("effectiveUntil"))))).toList();
 }
 public void fence(String org){
   if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Policy fence requires transaction");
   // Organization row exists before bootstrap; locking it avoids a missing-fence insertion phantom.
   if(jdbc.queryForList("SELECT ID FROM mulino_identity_Organizations WHERE ID=? FOR UPDATE",org).isEmpty())throw new SecurityException("Unavailable policy scope");
 }
 private static Instant instant(Object v){return v instanceof Instant i?i:Instant.parse(v.toString());}
}
