package com.mulino.domain.inventory;
import com.mulino.application.core.*;
import com.sap.cds.ql.Insert;
import com.sap.cds.services.persistence.PersistenceService;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
/** Only inventory-owned primitives mutate permissions or allocations. */
@Component
public class QualityPrimitives {
 private final InventoryRepository r;private final PersistenceService db;
 public QualityPrimitives(InventoryRepository r,PersistenceService db){this.r=r;this.db=db;}
 public Map<String,Object> record(DomainContext c,String entity,Map<String,Object>values){var row=base(c);row.putAll(values);r.insert(entity,row);return row;}
 public Map<String,Object> release(DomainContext c,String entity,String id,String decisionId){var old=r.current(c,entity,id);if(!"ACTIVE".equals(old.get("state")))throw new DomainError("CONFLICT","STALE_REVISION","Permission no longer active");r.update(c,entity,id,Map.of("state",entity.equals("Restrictions")?"RELEASED":"REVOKED","releasedAt",c.knownAt(),"releaseDecisionId",decisionId,"revision",((Number)old.get("revision")).intValue()+1));return r.current(c,entity,id);}
 public String decision(DomainContext c,Map<String,Object>values){var row=base(c);row.putAll(values);db.run(Insert.into("mulino.quality.Decisions").entry(row));return (String)row.get("ID");}
 public void suspendAllocation(DomainContext c,String allocationId,String reason){var old=r.current(c,"SegmentAllocations",allocationId);if(!"EXECUTABLE".equals(old.get("state")))return;r.update(c,"SegmentAllocations",allocationId,Map.of("state","SUSPENDED","suspendedAt",c.knownAt(),"suspensionReason",reason,"revision",((Number)old.get("revision")).intValue()+1));}
 public List<Map<String,Object>> suspend(DomainContext c,String segmentId,String reason){var out=new ArrayList<Map<String,Object>>();for(var a:r.currentRows(c,"SegmentAllocations"))if(segmentId.equals(a.get("segmentId"))&&"EXECUTABLE".equals(a.get("state"))){r.update(c,"SegmentAllocations",(String)a.get("ID"),Map.of("state","SUSPENDED","suspendedAt",c.knownAt(),"suspensionReason",reason,"revision",((Number)a.get("revision")).intValue()+1));out.add(a);}return out;}
 public Map<String,Object>decision(DomainContext c,String id){return db.run(com.sap.cds.ql.Select.from("mulino.quality.Decisions").where(x->x.get("organizationId").eq(c.organizationId()).and(x.get("ID").eq(id)))).first().map(x->new LinkedHashMap<String,Object>(x)).orElseThrow(DomainError::forbidden);}
 private Map<String,Object>base(DomainContext c){var row=new LinkedHashMap<String,Object>();row.put("organizationId",c.organizationId());row.put("ID",UUID.randomUUID().toString());row.put("revision",0);row.put("createdAt",c.knownAt());row.put("recordedAt",c.knownAt());return row;}
}
