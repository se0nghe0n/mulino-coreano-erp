package com.mulino.domain.inventory;

import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ScopeRepository {
  private final PersistenceService db;
  private final JdbcTemplate jdbc;

  public PersistenceService db() {
    return db;
  }

  public ScopeRepository(PersistenceService db, JdbcTemplate jdbc) {
    this.db = db;
    this.jdbc = jdbc;
  }

  public void fence(String id) {
    try {
      jdbc.queryForList("SELECT set_config('lock_timeout', ?, true)", "2000ms");
      jdbc.queryForList("SELECT id FROM mulino_platform_scopes WHERE id = ? FOR UPDATE", id);
    } catch (org.springframework.dao.DataAccessException failure) {
      if (failure.getMostSpecificCause() instanceof java.sql.SQLException sql
          && Set.of("55P03", "40P01", "40001").contains(sql.getSQLState()))
        throw new LockConflict(failure);
      throw failure;
    }
  }

  public static class LockConflict extends RuntimeException {
    public LockConflict(Throwable cause) {
      super("Scope lock conflict", cause);
    }
  }

  public Map<String, Object> scope(String id) {
    return db.run(Select.from("mulino.platform.Scopes").byId(id))
        .first()
        .map(x -> (Map<String, Object>) x)
        .orElseThrow(
            () ->
                new org.springframework.security.access.AccessDeniedException("Unavailable scope"));
  }

  public void insert(String entity, Map<String, Object> row) {
    db.run(Insert.into("mulino.platform." + entity).entry(row));
  }

  public void update(String entity, String id, Map<String, Object> row) {
    db.run(Update.entity("mulino.platform." + entity).byId(id).data(row));
  }
}
