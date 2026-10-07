package com.mulino.application.evidence;

import com.mulino.application.core.*;
import com.mulino.application.trade.TradeEvidenceScopePort;
import com.mulino.domain.evidence.*;
import com.mulino.adapters.blob.LocalBlobStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.mulino.application.evidence.EvidenceRecords.*;
import static com.mulino.domain.evidence.EvidenceTypes.*;

/** Actual original/blob/claim/review/link contract, with an explicit installed fixture domain provider. */
@Import(S3TypedEvidencePostgresTest.Configuration.class)
class S3TypedEvidencePostgresTest extends EvidencePersistenceTest {
  @Autowired EvidenceReconciliation reconcile;
  @BeforeEach void reviewerPermission(){jdbc.update("INSERT INTO mulino_identity_CapabilityAssignments(organizationId,ID,actorId,capabilityId,scopeKind,scopeId,validFrom,validUntil) VALUES(?,?,?,'matchSourceIdentity','ORGANIZATION',?,CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP+INTERVAL '1 day')",ORG,uuid(),ACTOR,ORG);jdbc.update("INSERT INTO mulino_identity_GrantActions VALUES(?,?,'matchSourceIdentity')",ORG,GRANT);}
  Map<String,Object> report(String source,String decisionId,String status){
    String payload="{\"decisionId\":\""+decisionId+"\",\"itemId\":\""+ITEM+"\",\"status\":\""+status+"\",\"quantity\":\"30\"}";
    byte[] original=payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    String doc=request(()->records.attachDocument(new DocumentInput(new Subject(SubjectKind.ITEM,ITEM),"warehouse",source,"application/json",LocalBlobStore.hash(original),"SYNTHETIC exact authority decision",null),original)).get("id").toString();
    String event=request(()->records.recordActivity(new EventInput(new Subject(SubjectKind.ITEM,ITEM),"S3_AUTHORITY_DECISION","warehouse",source,"1",new EffectiveTime(OCCURRED,null,"UTC","SECOND"),ValueState.KNOWN,payload,null,null))).get("id").toString();
    String claim=claim(event,doc,"30");
    return request(()->reconcile.match(auth.context(Instant.now(),Instant.now()),new EvidenceReconciliation.Review(claim,doc,ITEM,null,"synthetic-v1","warehouse:"+source+":1","30","BOX",OCCURRED,"Exact immutable decision scope"),"matchSourceIdentity"));
  }
  Map<String,Object> link(Map<String,Object> review){return request(()->reconcile.link(auth.context(Instant.now(),Instant.now()),review.get("id").toString()));}
  @Test void sameTimeDistinctDecisionsStayDistinctAndTwoSourcesSameDecisionDeduplicate(){
    String firstId=uuid(),secondId=uuid();var first=report("official-a",firstId,"ALLOWED");assertEquals("MATCHED",first.get("outcome"));String canonical=link(first).get("id").toString();
    var second=report("official-b",secondId,"ALLOWED");assertEquals("MATCHED",second.get("outcome"));assertNotEquals(canonical,link(second).get("id"));
    var proof=report("warehouse-copy",firstId,"ALLOWED");assertEquals("MATCHED",proof.get("outcome"));assertEquals(canonical,link(proof).get("id"));
    assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_Verifications",Integer.class));
    assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM mulino_inventory_QuantityMovements",Integer.class));
  }
  @Test void sameIdentityContradictoryMeaningCannotLinkOrOverwriteHistory(){
    String decision=uuid();var accepted=report("official",decision,"ALLOWED");String canonical=link(accepted).get("id").toString();
    var contradictory=report("competing-copy",decision,"REVOKED");assertEquals("CONFLICT",contradictory.get("outcome"));assertEquals("EVIDENCE_UNVERIFIED",assertThrows(DomainError.class,()->link(contradictory)).code());
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_CanonicalOccurrences",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM mulino_evidence_Verifications",Integer.class));assertEquals(decision,jdbc.queryForObject("SELECT occurrenceIdentity FROM mulino_evidence_CanonicalOccurrences WHERE ID=?",String.class,canonical));
  }
  @TestConfiguration static class Configuration {
    @Bean TradeEvidenceScopePort fixtureAuthorityProvider(EvidenceRepository repository){return new TradeEvidenceScopePort(){
      public Set<String> eventKinds(){return Set.of("S3_AUTHORITY_DECISION");}
      public Scope require(DomainContext c,String physical,Map<String,Object> claim,Map<String,Object> event,Map<String,Object> doc,byte[] original){
        repository.subject(c.organizationId(),"ITEM",physical);
        try{var mapper=new ObjectMapper();var actual=mapper.readTree(original);var reported=mapper.readTree(event.get("payload").toString());if(!actual.equals(reported)||!physical.equals(actual.path("itemId").asText())||!physical.equals(claim.get("itemId"))||!Set.of("ALLOWED","REVOKED").contains(actual.path("status").asText())||new java.math.BigDecimal(actual.path("quantity").asText()).compareTo((java.math.BigDecimal)claim.get("quantity"))!=0)throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Exact original authoritative fixture decision required");return new Scope(Map.of("itemId",physical),Map.of("ITEM",List.of(physical),"TARGET",List.of(physical)),EvidenceTypes.uuid(actual.path("decisionId").asText()));}catch(java.io.IOException invalid){throw new DomainError("HELD","EVIDENCE_UNVERIFIED","Exact JSON original required");}
      }
    };}
  }
}
