package com.mulino.application.evidence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.mulino.application.responsibility.ResponsibilityService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/** Reuses the real gateway fixture; focused Maven method selector avoids rerunning inherited baseline cases. */
class ReceiptResidualGatewayPostgresTest extends ReceiptGatewayPostgresTest {
 @Autowired ResponsibilityService duties;
 BigDecimal openShortfall(){return jdbc.queryForObject("SELECT COALESCE(sum(quantity),0) FROM mulino_work_read_ObligationReferences WHERE kind='RECEIPT_SHORTFALL' AND status='OPEN' AND valid",BigDecimal.class);}
 String arrive(int q){var input=provisional(uuid(),Integer.toString(q),true,null);var result=confirm(input,canonical(input,null),uuid());assertEquals("APPLIED",result.get("outcome"),result.toString());return ((Map<?,?>)result.get("effects")).get("receiptId").toString();}
 @Test void transferredReceiptLeafConservesRecipientHistoryAndExactCredits(){
  ordered100Fixture();arrive(60);assertEquals(0,new BigDecimal("40").compareTo(openShortfall()));
  String target=uuid();
  jdbc.update("INSERT INTO mulino_work_read_Works(organizationId,ID,createdAt,recordedAt,effectiveAt,itemId,definitionVersionId,kind,status,ownerId,supervisorId,lifecycleMode) SELECT organizationId,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,itemId,definitionVersionId,kind,'ACTIVE',ownerId,supervisorId,lifecycleMode FROM mulino_work_read_Works WHERE ID=?",target,WORK);
  jdbc.update("INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,'acceptHandover','ORGANIZATION',?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,uuid(),ACTOR,ORG);
  jdbc.update("INSERT INTO mulino_identity_GrantActions VALUES(?,?,?)",ORG,GRANT,"acceptHandover");
  String assignment=jdbc.queryForObject("SELECT ID FROM mulino_work_read_ObligationReferences WHERE kind='RECEIPT_SHORTFALL' AND status='OPEN' AND valid",String.class);
  request(()->new TransactionTemplate(tx).execute(status->{var c=auth.context(Instant.now(),Instant.now());var h=duties.propose(c,Map.of("workId",WORK,"assignmentId",assignment,"recipientId",ACTOR,"targetWorkId",target,"quantity","10","expiresAt",Instant.now().plusSeconds(100),"nextAction","Check recipient remainder","nextCheckAt",Instant.now().plusSeconds(1000)),true);duties.decide(c,h.get("ID").toString(),"ACCEPTED");return null;}));
  arrive(20);assertEquals(0,new BigDecimal("20").compareTo(openShortfall()));
  assertEquals("10.000000000000",jdbc.queryForObject("SELECT quantity::text FROM mulino_work_read_ObligationReferences WHERE workId=? AND kind='RECEIPT_SHORTFALL' AND status='OPEN' AND valid",String.class,target));
  arrive(20);assertEquals(0,openShortfall().signum());assertEquals(1,count("mulino_responsibility_ReceiptResidualRoots"));
  assertEquals("40.000000000000",jdbc.queryForObject("SELECT sum(quantity)::text FROM mulino_responsibility_ReceiptResidualCredits",String.class));
  assertEquals("RESOLVED",jdbc.queryForObject("SELECT status FROM mulino_work_read_ObligationReferences WHERE workId=? AND kind='RECEIPT_SHORTFALL' AND valid",String.class,target));
  assertEquals("TRANSFERRED",jdbc.queryForObject("SELECT status FROM mulino_work_read_ObligationReferences WHERE ID=?",String.class,assignment));
  assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE mulino_responsibility_ReceiptResidualCredits SET quantity=1"));
 }
 @Test void lateCommitFailureRollsBackReceiptCreditAndDutyRangeSplit(){
  ordered100Fixture();arrive(60);var input=provisional(uuid(),"20",true,null);String occurrence=canonical(input,null);
  int scopes=count("mulino_responsibility_Scopes"),assignments=count("mulino_work_read_ObligationReferences"),observationCredits=count("mulino_responsibility_ObservationReceiptCredits");
  doThrow(new IllegalStateException("late-remedy-rollback")).when(localPolicy).verifyCommit(any(),eq("confirmReceipt"),anyString(),any(),anyMap(),eq(false));
  assertThrows(IllegalStateException.class,()->confirm(input,occurrence,uuid()));
  assertEquals(scopes,count("mulino_responsibility_Scopes"));assertEquals(assignments,count("mulino_work_read_ObligationReferences"));assertEquals(observationCredits,count("mulino_responsibility_ObservationReceiptCredits"));assertEquals(0,count("mulino_responsibility_ReceiptResidualCredits"));assertEquals(1,count("mulino_trade_receipt_Receipts"));assertEquals("60.000000000000",jdbc.queryForObject("SELECT sum(contributedQuantity)::text FROM mulino_trade_purchase_ReceiptCredits",String.class));assertEquals(0,new BigDecimal("40").compareTo(openShortfall()));
 }
 @Test void previouslySettledLeafIsNeverReactivatedByReceipt(){
  ordered100Fixture();String initial=arrive(60);
  String occurrence=jdbc.queryForObject("SELECT canonicalOccurrenceId FROM mulino_trade_receipt_Receipts WHERE ID=?",String.class,initial);
  // Fixture seeds prior settlement state; this case tests non-reactivation, not settlement authorization.
  jdbc.update("UPDATE mulino_work_read_ObligationReferences SET status='RESOLVED',evidenceId=?,basis='PREVIOUS_SETTLEMENT' WHERE kind='RECEIPT_SHORTFALL' AND status='OPEN'",occurrence);
  arrive(20);assertEquals(0,openShortfall().signum());assertEquals(0,count("mulino_responsibility_ReceiptResidualCredits"));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_work_read_ObligationReferences WHERE kind='RECEIPT_SHORTFALL' AND status='RESOLVED' AND basis='PREVIOUS_SETTLEMENT'",Integer.class));
 }
}
