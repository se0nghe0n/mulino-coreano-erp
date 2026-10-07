package com.mulino.application.evidence;

import com.mulino.application.core.*;
import com.mulino.application.trade.TradeEvidence;
import com.mulino.application.evidence.EvidenceRecords.CanonicalInput;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** Real persisted source/original/CQN canonical guard; not receipt-domain acceptance. */
class S3CanonicalGatePostgresTest extends EvidencePersistenceTest {
  @Autowired TradeEvidence gate;
  String reviewed(String physical){String e=event(),doc=document(),claim=claim(e,doc,"60");return request(()->records.verifyReviewedCanonical(new CanonicalInput(claim,physical,doc,"synthetic-v1",true,true,true,true,true,null,null,"Trusted test review"))).get("id").toString();}
  Map<String,Object> require(String canonical,String physical,Instant asOf,Instant known){return request(()->gate.requireCanonical(auth.context(asOf,known),canonical,"RECEIPT",ITEM,physical,new BigDecimal("60"),"BOX"));}
  @Test void exactSourceQuantityAndSnapshotAreRequiredAndLateSourceConflictDoesNotRewriteHistory(){
    String physical=uuid(),canonical=reviewed(physical);Instant before=Instant.now();
    assertEquals(canonical,require(canonical,physical,OCCURRED.plusSeconds(1),before).get("ID"));
    assertThrows(DomainError.class,()->require(canonical,uuid(),OCCURRED.plusSeconds(1),before));
    assertThrows(DomainError.class,()->require(canonical,physical,OCCURRED.minusNanos(1),before));
    assertThrows(DomainError.class,()->require(canonical,physical,OCCURRED.plusSeconds(1),OCCURRED));
    assertThrows(DomainError.class,()->request(()->gate.requireCanonical(auth.context(OCCURRED.plusSeconds(1),before),canonical,"RECEIPT",ITEM,physical,new BigDecimal("40"),"BOX")));
    request(()->records.recordActivity(event("R60","1","receipt58",null)));
    assertEquals(canonical,require(canonical,physical,OCCURRED.plusSeconds(1),before).get("ID"));
    assertEquals("EVIDENCE_UNVERIFIED",assertThrows(DomainError.class,()->require(canonical,physical,OCCURRED.plusSeconds(1),Instant.now().plusSeconds(1))).code());
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_QuantityMovements",Integer.class));
  }
  @Test void sourcePolicyDriftAndSupersededOriginalCannotAuthorizeEffect(){
    String physical=uuid(),canonical=reviewed(physical);assertEquals(canonical,require(canonical,physical,OCCURRED.plusSeconds(1),Instant.now()).get("ID"));
    jdbc.update("UPDATE mulino_evidence_SourceProfiles SET policyVersion='synthetic-v2',revision=2 WHERE organizationId=? AND ID=?",ORG,SOURCE);
    assertEquals("EVIDENCE_UNVERIFIED",assertThrows(DomainError.class,()->require(canonical,physical,OCCURRED.plusSeconds(1),Instant.now())).code());
    jdbc.update("UPDATE mulino_evidence_SourceProfiles SET policyVersion='synthetic-v1',revision=3 WHERE organizationId=? AND ID=?",ORG,SOURCE);
    String doc=jdbc.queryForObject("SELECT basisDocumentId FROM mulino_evidence_Verifications WHERE canonicalOccurrenceId=?",String.class,canonical);Instant previous=Instant.now();
    request(()->records.attachDocument(document(doc),"original".getBytes()));
    assertEquals(canonical,require(canonical,physical,OCCURRED.plusSeconds(1),previous).get("ID"));
    assertEquals("EVIDENCE_UNVERIFIED",assertThrows(DomainError.class,()->require(canonical,physical,OCCURRED.plusSeconds(1),Instant.now().plusSeconds(1))).code());
  }
}
