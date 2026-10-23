package com.mulino.domain.trade.sales;
import com.mulino.application.core.*;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class SalesRepository {
 private final PersistenceService db;private final JdbcTemplate jdbc;
 public SalesRepository(PersistenceService db,JdbcTemplate jdbc){this.db=db;this.jdbc=jdbc;}
 private String entity(String e){if(!Set.of("Customers","Orders","OrderRevisions","OrderLines","Observations","Deliveries","ExecutionEffects","DeliveryCorrections").contains(e))throw DomainError.unsupported();return "mulino.trade.sales."+e;}
 public List<Map<String,Object>> rows(DomainContext c,String e){return db.run(Select.from(entity(e)).where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("recordedAt").le(c.knownAt())))).listOf(Map.class).stream().map(x->(Map<String,Object>)x).toList();}
 public Map<String,Object> require(DomainContext c,String e,String id){return rows(c,e).stream().filter(x->id.equals(x.get("ID"))).findFirst().orElseThrow(DomainError::forbidden);}
 public Map<String,Object> subject(String org,String e,String id){return db.run(Select.from(entity(e)).where(x->x.get("organizationId").eq(org).and(x.get("ID").eq(id)))).first().map(x->(Map<String,Object>)new LinkedHashMap<String,Object>(x)).orElseThrow(DomainError::forbidden);}
 public void insert(String e,Map<String,Object> row){db.run(Insert.into(entity(e)).entry(row));}
 public void update(DomainContext c,String e,String id,Map<String,Object> row){db.run(Update.entity(entity(e)).data(row).where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("ID").eq(id))));}
 /** Same lock namespace (organization:key) as the gateway and the other repositories, so one key names exactly one lock (plan §4.2). */
 public void fence(DomainContext c,String key){jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",c.organizationId()+":"+key);}
 /** A server-owned reassessment cause must already have its real physical effect in this transaction. */
 public Map<String,Object> currentRecordCause(DomainContext c,String workId,String capability){
  if(!Set.of("recordDelivery","recordObservedMovement").contains(capability))throw DomainError.forbidden();
  String commandId=CommandExecution.commandId();
  var command=db.run(Select.from("mulino.commands.CommandRecords").where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("ID").eq(commandId)))).first().orElseThrow(DomainError::forbidden);
  if(!"RECORD".equals(command.get("effectClass"))||!"IN_PROGRESS".equals(command.get("state"))||!c.actorId().equals(command.get("actorId"))||!c.stableRequestOwner().equals(command.get("stableRequestOwner"))||!capability.equals(command.get("capabilityId")))throw DomainError.forbidden();
  Map<String,Object> intent;try{intent=new com.fasterxml.jackson.databind.ObjectMapper().readValue(command.get("canonicalIntentJson").toString(),Map.class);}catch(java.io.IOException invalid){throw DomainError.forbidden();}
  if(!"RECORD".equals(intent.get("intentKind"))||!capability.equals(intent.get("capabilityId"))||!Objects.equals(command.get("canonicalHash"),CommandRequests.hash(intent))||!(intent.get("slots") instanceof Map<?,?> slots)||!(slots.get("observationId") instanceof String observation)||!(slots.get("canonicalOccurrenceId") instanceof String canonical))throw DomainError.forbidden();
  var report=require(c,"Observations",observation);var applied=rows(c,"Deliveries").stream().filter(d->observation.equals(d.get("observationId"))&&canonical.equals(d.get("canonicalOccurrenceId"))&&workId.equals(d.get("workId"))).toList();
  if(!"CONFIRMED".equals(report.get("state"))||!workId.equals(report.get("workId"))||applied.size()!=1)throw DomainError.forbidden();
  var delivery=applied.getFirst();
  var transfer=db.run(Select.from("mulino.inventory.DeliveryTransfers").where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("commandId").eq(commandId)).and(x.get("canonicalId").eq(canonical)).and(x.get("dispatchId").eq(delivery.get("dispatchId"))).and(x.get("segmentId").eq(delivery.get("segmentId"))))).listOf(Map.class);
  if(transfer.size()!=1)throw DomainError.forbidden();
  for(String key:List.of("startQuantity","quantity"))if(!(transfer.getFirst().get(key) instanceof java.math.BigDecimal actual)||!(delivery.get(key) instanceof java.math.BigDecimal expected)||actual.compareTo(expected)!=0)throw DomainError.forbidden();
  if(!Objects.equals(transfer.getFirst().get("unit"),delivery.get("unit"))||!com.mulino.domain.inventory.StockPrimitives.instant(transfer.getFirst().get("occurredAt")).equals(com.mulino.domain.inventory.StockPrimitives.instant(delivery.get("occurredAt"))))throw DomainError.forbidden();
  var target=db.run(Select.from("mulino.work.read.Works").where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("ID").eq(workId)))).first().orElseThrow(DomainError::forbidden);
  if(!"COMMAND".equals(target.get("lifecycleMode"))||!"DELIVERY".equals(target.get("kind"))||!Set.of("ACTIVE","WAITING").contains(target.get("status"))||!Objects.equals(target.get("itemId"),delivery.get("itemId")))throw DomainError.forbidden();
  var fact=db.run(Select.from("mulino.evidence.CanonicalOccurrences").where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("ID").eq(canonical)))).first().orElseThrow(DomainError::forbidden);
  if(fact.get("sourceProfileId")==null)throw DomainError.forbidden();
  var cause=new LinkedHashMap<String,Object>(delivery);cause.put("sourceProfileId",fact.get("sourceProfileId"));cause.put("commandId",commandId);cause.put("capabilityId",capability);return cause;
 }
}
