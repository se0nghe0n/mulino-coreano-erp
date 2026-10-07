package com.mulino.domain.identity;

import com.sap.cds.ql.Select;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** CQN owns identity data; the only SQL is a fixed PostgreSQL authority fence. */
@Repository
public class IdentityRepository {
  private final PersistenceService db;
  private final JdbcTemplate jdbc;
  public IdentityRepository(PersistenceService db, JdbcTemplate jdbc) { this.db=db; this.jdbc=jdbc; }
  public List<Map<String,Object>> rows(String entity,String organizationId) {
    return db.run(Select.from("mulino.identity."+entity).where(b -> b.get("organizationId").eq(organizationId)))
        .listOf(Map.class).stream().map(r -> (Map<String,Object>)r).toList();
  }
  public Optional<Map<String,Object>> external(String issuer,String subject,String alias) {
    return db.run(Select.from("mulino.identity.ExternalIdentities").where(b ->
        b.get("issuer").eq(issuer).and(b.get("subject").eq(subject)).and(b.get("organizationAlias").eq(alias))))
        .first().map(r -> (Map<String,Object>)r);
  }
  public Optional<Map<String,Object>> actor(String org,String id) {
    return rows("Actors",org).stream().filter(r -> id.equals(r.get("ID"))).findFirst();
  }
  /** All capability/grant writes and future guarded commands lock the same actor rows in ID order. */
  public void fence(String org,Collection<String> actors) {
    if(!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Authority fence requires the application transaction");
    jdbc.queryForList("SELECT set_config('lock_timeout', ?, true)","2000ms");
    for (String actor:new TreeSet<>(actors)) {
      var found=jdbc.queryForList("SELECT actorId FROM mulino_identity_AuthorityFences WHERE organizationId=? AND actorId=? FOR UPDATE",org,actor);
      if(found.isEmpty()) throw new org.springframework.security.access.AccessDeniedException("Unavailable authority");
    }
  }
}
