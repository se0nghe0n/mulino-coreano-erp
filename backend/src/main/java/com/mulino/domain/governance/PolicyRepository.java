package com.mulino.domain.governance;

import com.sap.cds.ql.Select;
import com.sap.cds.services.persistence.PersistenceService;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Repository;

/** Effective policy is selected independently of a Work's pinned definition meaning. */
@Repository
public class PolicyRepository {
 private final PersistenceService db;
 public PolicyRepository(PersistenceService db) {this.db=db;}
 public List<Map<String,Object>> current(String organizationId,String kind,Instant at) {
   return db.run(Select.from("mulino.governance.PolicyVersions").where(x->
       x.get("organizationId").eq(organizationId).and(x.get("kind").eq(kind))
       .and(x.get("effectiveFrom").le(at)).and(x.get("effectiveUntil").isNull().or(x.get("effectiveUntil").gt(at)))))
       .listOf(Map.class).stream().map(x->(Map<String,Object>)x).toList();
 }
}
