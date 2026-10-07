package com.mulino.domain.evidence;

import com.mulino.application.core.*;
import com.sap.cds.ql.*;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EvidenceRepository {
  private final PersistenceService db;
  private final JdbcTemplate jdbc;
  public EvidenceRepository(PersistenceService db,JdbcTemplate jdbc) { this.db=db; this.jdbc=jdbc; }
  public PersistenceService db() { return db; }
  public Optional<Map<String,Object>> find(String entity,String org,String id) {
    return db.run(Select.from("mulino.evidence."+entity).where(x->x.get("organizationId").eq(org).and(x.get("ID").eq(id)))).first()
      .map(row->new LinkedHashMap<String,Object>(row));
  }
  public Map<String,Object> require(String entity,String org,String id) { return find(entity,org,id).orElseThrow(DomainError::forbidden); }
  public List<Map<String,Object>> rows(String entity,String org) {
    return db.run(Select.from("mulino.evidence."+entity).where(x->x.get("organizationId").eq(org)).orderBy("ID"))
      .listOf(Map.class).stream().map(row->(Map<String,Object>)new LinkedHashMap<String,Object>(row)).toList();
  }
  public void insert(String entity,Map<String,Object> row) { db.run(Insert.into("mulino.evidence."+entity).entry(row)); }
  /** Fixed PostgreSQL primitive. Same source-key registration serializes conflict detection. */
  public void sourceFence(String org,String namespace,String event,String version) {
    jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",org+"|"+namespace+"|"+event+"|"+version);
  }
  public Map<String,Object> subject(String org,String kind,String id) {
    String entity=switch(kind) {
      case "ITEM" -> "mulino.inventory.TradeItems";
      case "LOT" -> "mulino.inventory.ManufacturingLots";
      case "SEGMENT" -> "mulino.inventory.QuantitySegments";
      case "PLACE" -> "mulino.inventory.Places";
      case "WORK" -> "mulino.work.Works";
      default -> throw DomainError.invalid("Unsupported evidence subject type");
    };
    return db.run(Select.from(entity).where(x->x.get("organizationId").eq(org).and(x.get("ID").eq(id)))).first()
      .map(row->new LinkedHashMap<String,Object>(row)).orElseThrow(DomainError::forbidden);
  }
  /** Ancestor and descendant scopes overlap; sibling split scopes remain disjoint. */
  public Set<String> overlappingScopes(String org,String segmentId) {
    var edges=db.run(Select.from("mulino.inventory.GenealogyEdges").where(x->x.get("organizationId").eq(org))).listOf(Map.class);
    Set<String> result=new HashSet<>();result.add(segmentId);
    for(boolean forward:List.of(true,false)) {
      Set<String> direction=new HashSet<>();direction.add(segmentId);boolean changed;
      do {changed=false;for(var edge:edges) {
        String from=Objects.toString(edge.get(forward?"sourceId":"targetId"),null),to=Objects.toString(edge.get(forward?"targetId":"sourceId"),null);
        if(from!=null&&to!=null&&direction.contains(from))changed|=direction.add(to);
      }}while(changed);
      result.addAll(direction);
    }
    return result;
  }
  public boolean referenced(UUID blob) {
    return db.run(Select.from("mulino.evidence.DocumentVersions").where(x->x.get("blobId").eq(blob.toString()))).rowCount()>0;
  }
}
