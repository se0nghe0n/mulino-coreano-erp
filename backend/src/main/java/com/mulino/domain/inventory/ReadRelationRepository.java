package com.mulino.domain.inventory;
import com.mulino.application.core.DomainContext;
import com.sap.cds.ql.Select;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/** Typed relation meaning is read from the published definition in this snapshot. */
@Repository
public class ReadRelationRepository {
 private final PersistenceService db;
 public ReadRelationRepository(PersistenceService db){this.db=db;}
 public Map<String,String> names(DomainContext c){
  var published=db.run(Select.from("mulino.definitions.DefinitionVersions").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("state").eq("PUBLISHED")).and(r.get("createdAt").le(c.knownAt())))).listOf(Map.class).stream().map(r->r.get("ID").toString()).collect(Collectors.toSet());
  var names=new HashMap<String,String>();
  for(var row:db.run(Select.from("mulino.definitions.RelationDefinitions").where(r->r.get("organizationId").eq(c.organizationId()).and(r.get("createdAt").le(c.knownAt())))).listOf(Map.class))if(published.contains(row.get("definitionVersionId")))names.put(row.get("ID").toString(),row.get("name").toString());
  return Map.copyOf(names);
 }
}
