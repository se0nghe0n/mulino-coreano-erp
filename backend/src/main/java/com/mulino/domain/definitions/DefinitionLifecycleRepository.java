package com.mulino.domain.definitions;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Plan §8 package lifecycle rows (V35). DefinitionVersions holds content/hash/DRAFT|IN_REVIEW|PUBLISHED; the lifecycle
 * revision, validations, reviews, approvals, the organization's ACTIVE pointer and retirements are separate rows.
 * Callers run inside the command transaction and fence the organization's definition key first.
 */
@Repository
public class DefinitionLifecycleRepository {
  private final JdbcTemplate jdbc;
  public DefinitionLifecycleRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
  private static Timestamp ts(Instant i){return i==null?null:Timestamp.from(i);}
  private Optional<Map<String,Object>> one(String sql,Object... args){var rows=jdbc.queryForList(sql,args);return rows.isEmpty()?Optional.empty():Optional.of(rows.getFirst());}

  public Optional<Map<String,Object>> version(String org,String id){return one("SELECT ID,version,state,contentHash,content,parentVersionId,evaluatorVersion,schemaVersion FROM mulino_definitions_DefinitionVersions WHERE organizationId=? AND ID=?",org,id);}
  public boolean versionExists(String org,String version){return !jdbc.queryForList("SELECT 1 FROM mulino_definitions_DefinitionVersions WHERE organizationId=? AND version=?",org,version).isEmpty();}
  public void insertVersion(String org,String id,String version,String parent,String hash,String content,String evaluator,String schema,Instant at){
    jdbc.update("INSERT INTO mulino_definitions_DefinitionVersions(organizationId,ID,revision,createdAt,version,parentVersionId,state,contentHash,content,evaluatorVersion,schemaVersion) VALUES(?,?,0,?,?,?,'DRAFT',?,?,?,?)",
        org,id,ts(at),version,parent,hash,content,evaluator,schema);
  }
  public void reviseDraft(String org,String id,String hash,String content,String evaluator,String schema){
    jdbc.update("UPDATE mulino_definitions_DefinitionVersions SET state='DRAFT',contentHash=?,content=?,evaluatorVersion=?,schemaVersion=? WHERE organizationId=? AND ID=? AND state IN ('DRAFT','IN_REVIEW')",hash,content,evaluator,schema,org,id);
  }
  public void state(String org,String id,String from,String to){
    if(jdbc.update("UPDATE mulino_definitions_DefinitionVersions SET state=? WHERE organizationId=? AND ID=? AND state=?",to,org,id,from)!=1)throw new IllegalStateException("Definition state changed under fence");
  }
  public Optional<Map<String,Object>> lifecycle(String org,String id){return one("SELECT * FROM mulino_definitions_DefinitionLifecycles WHERE organizationId=? AND definitionId=?",org,id);}
  public int revision(String org,String id){return lifecycle(org,id).map(r->((Number)r.get("revision")).intValue()).orElse(0);}
  public void createLifecycle(String org,String id,String actor,String regressionRefs,Instant at){jdbc.update("INSERT INTO mulino_definitions_DefinitionLifecycles(organizationId,definitionId,revision,createdBy,regressionFixtureRefs,createdAt,recordedAt) VALUES(?,?,1,?,?,?,?)",org,id,actor,regressionRefs,ts(at),ts(at));}
  /** Bumps the lifecycle revision; a package published outside the lifecycle (fixture/import) gets its first row here. */
  public int bump(String org,String id,String actor,Instant at){
    if(jdbc.update("UPDATE mulino_definitions_DefinitionLifecycles SET revision=revision+1,recordedAt=? WHERE organizationId=? AND definitionId=?",ts(at),org,id)==0)createLifecycle(org,id,actor,"[]",at);
    return revision(org,id);
  }
  public void regressionRefs(String org,String id,String refs){jdbc.update("UPDATE mulino_definitions_DefinitionLifecycles SET regressionFixtureRefs=? WHERE organizationId=? AND definitionId=?",refs,org,id);}

  public void insertValidation(Map<String,Object> r){insert("mulino_definitions_DefinitionValidations",r);}
  public Optional<Map<String,Object>> validation(String org,String id){return one("SELECT * FROM mulino_definitions_DefinitionValidations WHERE organizationId=? AND ID=?",org,id);}
  public void insertReview(Map<String,Object> r){insert("mulino_definitions_DefinitionReviews",r);}
  public Optional<Map<String,Object>> review(String org,String id){return one("SELECT * FROM mulino_definitions_DefinitionReviews WHERE organizationId=? AND ID=?",org,id);}
  public Optional<Map<String,Object>> latestReview(String org,String definitionId,String hash,String kind){return one("SELECT * FROM mulino_definitions_DefinitionReviews WHERE organizationId=? AND definitionId=? AND hash=? AND reviewKind=? ORDER BY recordedAt DESC, ID LIMIT 1",org,definitionId,hash,kind);}
  public void insertApproval(Map<String,Object> r){insert("mulino_definitions_DefinitionApprovals",r);}
  public Optional<Map<String,Object>> approval(String org,String id){return one("SELECT * FROM mulino_definitions_DefinitionApprovals WHERE organizationId=? AND ID=?",org,id);}
  public List<Map<String,Object>> approvals(String org,String definitionId,String hash,String kind){return jdbc.queryForList("SELECT * FROM mulino_definitions_DefinitionApprovals WHERE organizationId=? AND definitionId=? AND hash=? AND reviewKind=? ORDER BY recordedAt, ID",org,definitionId,hash,kind);}
  public void consume(String org,String approvalId,String commandRef,Instant at){
    if(jdbc.update("UPDATE mulino_definitions_DefinitionApprovals SET consumedAt=?,consumedBy=? WHERE organizationId=? AND ID=? AND consumedAt IS NULL",ts(at),commandRef,org,approvalId)!=1)throw new IllegalStateException("Approval consumed under fence");
  }
  public Optional<Map<String,Object>> pointer(String org){return one("SELECT * FROM mulino_definitions_ActiveDefinitionPointers WHERE organizationId=?",org);}
  public void point(String org,String definitionId,String version,String hash,Instant effectiveAt,String actor,Instant at){
    if(jdbc.update("UPDATE mulino_definitions_ActiveDefinitionPointers SET definitionId=?,version=?,hash=?,effectiveAt=?,actorId=?,revision=revision+1,recordedAt=? WHERE organizationId=?",definitionId,version,hash,ts(effectiveAt),actor,ts(at),org)==0)
      jdbc.update("INSERT INTO mulino_definitions_ActiveDefinitionPointers(organizationId,definitionId,version,hash,effectiveAt,actorId,revision,recordedAt) VALUES(?,?,?,?,?,?,1,?)",org,definitionId,version,hash,ts(effectiveAt),actor,ts(at));
  }
  public void unpoint(String org,String definitionId){jdbc.update("DELETE FROM mulino_definitions_ActiveDefinitionPointers WHERE organizationId=? AND definitionId=?",org,definitionId);}
  public boolean retired(String org,String definitionId){return !jdbc.queryForList("SELECT 1 FROM mulino_definitions_DefinitionRetirements WHERE organizationId=? AND definitionId=?",org,definitionId).isEmpty();}
  public void retire(String org,String definitionId,String reason,String actor,Instant at){jdbc.update("INSERT INTO mulino_definitions_DefinitionRetirements(organizationId,definitionId,reason,actorId,retiredAt) VALUES(?,?,?,?,?)",org,definitionId,reason,actor,ts(at));}
  public void insertMigration(Map<String,Object> r){insert("mulino_definitions_WorkDefinitionMigrations",r);}

  private void insert(String table,Map<String,Object> row){
    var values=new ArrayList<Object>();for(Object v:row.values())values.add(v instanceof Instant i?ts(i):v);
    jdbc.update("INSERT INTO "+table+" ("+String.join(",",row.keySet())+") VALUES ("+String.join(",",Collections.nCopies(row.size(),"?"))+")",values.toArray());
  }
}
