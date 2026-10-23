package com.mulino.domain.evaluation;

import com.mulino.domain.definitions.DefinitionRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Internal storage only. A capture ID conveys no authority to read it. */
@Repository
public class EvidenceCaptureRepository {
 private final JdbcTemplate jdbc;
 public EvidenceCaptureRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public String acquire(String org,String scope,Instant asOf,Instant knownAt,Instant horizon,String purpose,String payload,Instant recordedAt){
  if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Evidence capture requires parent transaction");
  try{var b=new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload).path("boundary");
   if(!org.equals(b.path("organizationId").asText())||!new com.fasterxml.jackson.databind.ObjectMapper().readTree(scope).equals(b.path("scope"))||!asOf.equals(Instant.parse(b.path("asOf").asText()))||!knownAt.equals(Instant.parse(b.path("knownAt").asText()))||!horizon.equals(Instant.parse(b.path("evaluationHorizon").asText()))||!purpose.equals(b.path("admissionPurpose").asText()))throw new IllegalArgumentException("Evidence capture boundary differs from its payload");
  }catch(java.io.IOException invalid){throw new IllegalArgumentException("Invalid evidence capture payload",invalid);}
  String hash=DefinitionRepository.sha256(payload);
  jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",org+"|evidence-capture|"+hash);
  Map<String,Object> scopeValues;
  try{scopeValues=new com.fasterxml.jackson.databind.ObjectMapper().readValue(scope,Map.class);}catch(java.io.IOException invalid){throw new IllegalArgumentException("Invalid capture scope",invalid);}
  if(scopeValues.get("itemId")==null)throw new IllegalArgumentException("Capture item scope required");
  String proposed=UUID.randomUUID().toString();
  jdbc.update("INSERT INTO mulino_evaluation_EvidenceCaptures(organizationId,ID,itemId,lotId,placeId,workId,scopeJson,asOf,knownAt,evaluationHorizon,admissionPurpose,contentHash,contentJson,recordedAt) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(organizationId,contentHash) DO NOTHING",org,proposed,scopeValues.get("itemId"),scopeValues.get("lotId"),scopeValues.get("placeId"),scopeValues.get("workId"),scope,Timestamp.from(asOf),Timestamp.from(knownAt),Timestamp.from(horizon),purpose,hash,payload,Timestamp.from(recordedAt));
  var rows=jdbc.queryForList("SELECT ID,contentJson FROM mulino_evaluation_EvidenceCaptures WHERE organizationId=? AND contentHash=?",org,hash);
  if(rows.size()!=1||!payload.equals(rows.getFirst().get("contentjson")))throw new IllegalStateException("Evidence capture digest collision or source corruption");
  return rows.getFirst().get("id").toString();
 }
}
