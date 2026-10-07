package com.mulino.domain.governance;
import com.mulino.application.core.*;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import java.time.Instant;
import org.springframework.stereotype.Repository;
@Repository
public class ApprovalRepository {
  private final PersistenceService db;
  public ApprovalRepository(PersistenceService db){this.db=db;}
  public Optional<Map<String,Object>> find(DomainContext c,String id){return db.run(Select.from("mulino.commands.Approvals").where(b->b.get("organizationId").eq(c.organizationId()).and(b.get("ID").eq(id)))).first().map(r->(Map<String,Object>)r);}
  /** Called only by a typed approval decision handler after current decision authority checks. */
  public String create(DomainContext c,Map<String,Object> fields){Map<String,Object> row=new LinkedHashMap<>(fields);String id=UUID.randomUUID().toString();row.put("ID",id);row.put("organizationId",c.organizationId());row.put("approverId",c.actorId());db.run(Insert.into("mulino.commands.Approvals").entry(row));return id;}
  public boolean consumed(DomainContext c,String id){return db.run(Select.from("mulino.commands.ApprovalConsumptions").where(b->b.get("organizationId").eq(c.organizationId()).and(b.get("approvalId").eq(id)))).rowCount()>0;}
  public void consume(DomainContext c,String approvalId,String commandId,Instant now){db.run(Insert.into("mulino.commands.ApprovalConsumptions").entry(Map.of("ID",UUID.randomUUID().toString(),"organizationId",c.organizationId(),"approvalId",approvalId,"commandId",commandId,"consumedAt",now)));}
}
