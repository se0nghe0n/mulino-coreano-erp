package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real V37 -> V38 upgrade: unknown historical policy is never backfilled. */
class PurchaseApprovalMigrationPostgresTest {
  @Test void upgradePreservesUnknownHistoricalPolicyAndRequiresPolicyForNewRows() throws Exception {
    try(var pg=new PostgreSQLContainer("postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280")) {
      pg.start();
      Flyway.configure().dataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword()).target("37").load().migrate();
      String org=UUID.randomUUID().toString(),approval=UUID.randomUUID().toString();
      try(var c=DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());var s=c.createStatement()) {
        // Parent references are irrelevant to this DDL upgrade; preserve a valid old row shape.
        s.execute("SET session_replication_role = replica");
        try(var q=c.prepareStatement("INSERT INTO mulino_trade_purchase_Approvals(organizationId,ID,proposalId,proposalRevision,proposalHash,approverId,policyHash,decision,conditionsJson,decidedAt,validUntil) VALUES(?,?,?,1,?,?,?,'APPROVE','[]',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP+interval '1 day')")) {
          q.setString(1,org);q.setString(2,approval);q.setString(3,UUID.randomUUID().toString());q.setString(4,"a".repeat(64));q.setString(5,UUID.randomUUID().toString());q.setString(6,"b".repeat(64));q.executeUpdate();
        }
        s.execute("SET session_replication_role = origin");
      }
      var flyway=Flyway.configure().dataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword()).target("38").load();
      assertEquals(1,flyway.migrate().migrationsExecuted);flyway.validate();assertEquals(0,flyway.migrate().migrationsExecuted);
      try(var c=DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());var s=c.createStatement()) {
        try(var rs=s.executeQuery("SELECT consumptionPolicy,proposalHash,decision FROM mulino_trade_purchase_Approvals")) {
          assertTrue(rs.next());assertNull(rs.getString(1));assertEquals("a".repeat(64),rs.getString(2));assertEquals("APPROVE",rs.getString(3));assertFalse(rs.next());
        }
        assertThrows(SQLException.class,()->s.executeUpdate("UPDATE mulino_trade_purchase_Approvals SET consumptionPolicy='SINGLE_ORDER_REVISION'"));
        for(String value:List.of("NULL","'REUSABLE'")) {
          var failure=assertThrows(SQLException.class,()->s.executeUpdate("INSERT INTO mulino_trade_purchase_Approvals(organizationId,ID,consumptionPolicy) VALUES('"+org+"','"+UUID.randomUUID()+"',"+value+")"));
          assertEquals("23514",failure.getSQLState());
        }
      }
    }
  }
}
