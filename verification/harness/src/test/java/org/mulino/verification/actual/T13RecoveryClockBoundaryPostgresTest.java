package org.mulino.verification.actual;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mulino.verification.Json;

/** Repeat the original strict supplier checks beside a separately declared future synthetic schedule. */
@EnabledIfEnvironmentVariable(named="T13_PG_TEST",matches="true")
class T13RecoveryClockBoundaryPostgresTest extends T13SupplierSourceFactsPostgresTest {
 static final String FUTURE_ORG=UUID.randomUUID().toString(),FUTURE_ID=UUID.randomUUID().toString();
 static final Instant FUTURE=Instant.parse("2026-10-07T09:30:00Z");
 static String originalRow,originalHistory;
 static Connection connection() throws Exception {var config=ActualConfiguration.environment(System.getenv());return DriverManager.getConnection(config.jdbcUrl(),config.username(),config.password());}
 @BeforeAll static void installExplicitFutureHistory() throws Exception {
  try(var db=connection()) {
   db.setAutoCommit(false);
   try(var q=db.prepareStatement("SELECT set_config('mulino.recovery_recorded_at',?,true)")){q.setString(1,FUTURE.toString());q.execute();}
   try(var q=db.prepareStatement("INSERT INTO mulino_runtime_RecoverySchedules(organizationId,ID,workId,kind,nextCheckAt,pendingAssessment,ownerId,supervisorId,nextAction) VALUES(?,?,?,'WORK_RECHECK',?,true,?,?,'Explicit synthetic future-history regression Given')")) {
    q.setString(1,FUTURE_ORG);q.setString(2,FUTURE_ID);q.setString(3,UUID.randomUUID().toString());q.setTimestamp(4,Timestamp.from(FUTURE.minusSeconds(3600)));q.setString(5,UUID.randomUUID().toString());q.setString(6,UUID.randomUUID().toString());assertEquals(1,q.executeUpdate());
   }
   db.commit();originalRow=raw(db,"SELECT row_to_json(s)::text FROM mulino_runtime_RecoverySchedules s WHERE organizationId=? AND ID=?");
   originalHistory=raw(db,"SELECT row_to_json(s)::text FROM mulino_runtime_RecoveryObservationHistory s WHERE organizationId=? AND sourceId=?");
  }
 }
 static String raw(Connection db,String sql) throws Exception {try(var q=db.prepareStatement(sql)){q.setString(1,FUTURE_ORG);q.setString(2,FUTURE_ID);try(var r=q.executeQuery()){assertTrue(r.next());String value=r.getString(1);assertFalse(r.next());return value;}}}
 @AfterAll static void preserveFutureGivenAndRecordReceipt() throws Exception {
  try(var db=connection()) {
   String row=raw(db,"SELECT row_to_json(s)::text FROM mulino_runtime_RecoverySchedules s WHERE organizationId=? AND ID=?"),history=raw(db,"SELECT row_to_json(s)::text FROM mulino_runtime_RecoveryObservationHistory s WHERE organizationId=? AND sourceId=?");
   assertEquals(originalRow,row);assertEquals(originalHistory,history);
   var receipt=Json.object().put("synthetic",true).put("originalScenarioModified",false).put("future",FUTURE.toString()).put("scheduleRawBefore",originalRow).put("scheduleRawAfter",row).put("historyRawBefore",originalHistory).put("historyRawAfter",history).put("futureUnchanged",true).put("scope","Additional HTTP/PG counterexample, not original fixture data");
   Json.write(Path.of(System.getenv("T13_TEST_EVIDENCE")).resolve("future-history-given.json"),receipt);
  }
 }
}
