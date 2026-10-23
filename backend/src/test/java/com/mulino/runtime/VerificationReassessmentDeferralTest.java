package com.mulino.runtime;

import com.mulino.application.runtime.VerificationFaults;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.*;
import static org.junit.jupiter.api.Assertions.*;

class VerificationReassessmentDeferralTest {
 final VerificationFaults faults=new VerificationFaults();
 final String org=UUID.randomUUID().toString(),work=UUID.randomUUID().toString();
 Map<String,Object> body(String op){return Map.of("operation",op,"organizationId",org,"point",VerificationFaults.DEFER_POINT);}
 void tx(){TransactionSynchronizationManager.setActualTransactionActive(true);TransactionSynchronizationManager.initSynchronization();}
 void complete(boolean commit){var sync=TransactionSynchronizationManager.getSynchronizations();if(commit)sync.forEach(TransactionSynchronization::afterCommit);sync.forEach(s->s.afterCompletion(commit?TransactionSynchronization.STATUS_COMMITTED:TransactionSynchronization.STATUS_ROLLED_BACK));TransactionSynchronizationManager.clearSynchronization();TransactionSynchronizationManager.setActualTransactionActive(false);}
 @AfterEach void clean(){TransactionSynchronizationManager.clear();}
 @Test void releaseNeedsReachedCommittedTransactionAndExactOrganization(){
  assertThrows(IllegalArgumentException.class,()->faults.control(body("release")));faults.control(body("arm"));assertThrows(IllegalArgumentException.class,()->faults.control(body("release")));tx();faults.checkpoint(org,"correctEvidence","key","actual-command",Set.of(work));assertTrue(faults.held(org,work));assertThrows(IllegalArgumentException.class,()->faults.control(body("release")));complete(true);
  assertFalse(faults.held(UUID.randomUUID().toString(),work));assertFalse(faults.held(org,UUID.randomUUID().toString()));assertThrows(IllegalArgumentException.class,()->faults.control(Map.of("operation","release","organizationId",UUID.randomUUID().toString(),"point",VerificationFaults.DEFER_POINT)));assertEquals("RELEASED",faults.control(body("release")).get("state"));assertFalse(faults.held(org,work));assertEquals("RELEASED",faults.control(body("release")).get("state"));
 }
 @Test void optionalCapabilityAndCommandAreIsolationFilters(){
  var arm=new HashMap<>(body("arm"));arm.put("capabilityId","correctEvidence");arm.put("commandKey","exact");faults.control(arm);tx();faults.checkpoint(org,"recordActivity","exact","c",Set.of(work));faults.checkpoint(org,"correctEvidence","other","c",Set.of(work));faults.checkpoint(UUID.randomUUID().toString(),"correctEvidence","exact","c",Set.of(work));assertFalse(faults.held(org,work));faults.checkpoint(org,"correctEvidence","exact","c",Set.of(work));assertTrue(faults.held(org,work));complete(true);faults.control(body("release"));
 }
 @Test void rollbackDoesNotAllowSuccessAcknowledgement(){faults.control(body("arm"));tx();faults.checkpoint(org,"correctEvidence","key","c",Set.of(work));complete(false);assertFalse(faults.held(org,work));assertThrows(IllegalArgumentException.class,()->faults.control(body("release")));}
 @Test void newerRolledBackAttemptCannotBorrowOlderReleaseAcknowledgement(){faults.control(body("arm"));tx();faults.checkpoint(org,"correctEvidence","first","c1",Set.of(work));complete(true);faults.control(body("release"));faults.control(body("arm"));tx();faults.checkpoint(org,"correctEvidence","second","c2",Set.of(work));complete(false);assertThrows(IllegalArgumentException.class,()->faults.control(body("release")));}
 @Test void noTransactionOrNoAffectedWorkCannotReachPoint(){faults.control(body("arm"));faults.checkpoint(org,"correctEvidence","k","c",Set.of());assertThrows(IllegalStateException.class,()->faults.checkpoint(org,"correctEvidence","k","c",Set.of(work)));assertFalse(faults.held(org,work));assertThrows(IllegalArgumentException.class,()->faults.control(body("release")));}
 @Test void unknownFailureAndAmbiguousSelectorsRemainRefused(){assertThrows(VerificationFaults.UnsupportedPoint.class,()->faults.control(Map.of("operation","arm","organizationId",org,"point","unknown")));var a=new HashMap<>(body("arm"));a.put("fault","rollback");assertThrows(IllegalArgumentException.class,()->faults.control(a));faults.control(body("arm"));faults.control(body("arm"));tx();faults.checkpoint(org,"correctEvidence","k","c",Set.of(work));complete(true);assertThrows(IllegalArgumentException.class,()->faults.control(body("release")));}
 @Test void boundedActiveControlsAndAffectedWorks(){for(int i=0;i<128;i++)faults.control(body("arm"));assertThrows(IllegalArgumentException.class,()->faults.control(body("arm")));tx();var works=new HashSet<String>();for(int i=0;i<1001;i++)works.add("w"+i);assertThrows(IllegalArgumentException.class,()->faults.checkpoint(org,"correctEvidence","k","c",works));assertFalse(faults.held(org,work));complete(false);}
 @Test void originalRollbackAndLostResponsePointsStillFireOnlyOnceInMatchingScope(){for(String point:List.of("command.beforeCommit","AFTER_DB_COMMIT_BEFORE_RESPONSE")){faults.control(Map.of("operation","arm","organizationId",org,"point",point,"capabilityId","correctEvidence","commandKey","k"));assertDoesNotThrow(()->faults.check(UUID.randomUUID().toString(),"correctEvidence","k",point));assertThrows(VerificationFaults.Injected.class,()->faults.check(org,"correctEvidence","k",point));assertDoesNotThrow(()->faults.check(org,"correctEvidence","k",point));}}
}
