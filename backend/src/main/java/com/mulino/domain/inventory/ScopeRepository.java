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
    jdbc.queryForList("SELECT id FROM mulino_platform_scopes WHERE id = ? FOR UPDATE", id);
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
