package com.mulino.domain.definitions;

import com.sap.cds.ql.Select;
import com.sap.cds.services.persistence.PersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class DefinitionRepository {
  private final PersistenceService db;
  private final ObjectMapper json=new ObjectMapper();
  public DefinitionRepository(PersistenceService db) {this.db=db;}
  public Definition get(String organizationId,String id) {
    var row=db.run(Select.from("mulino.definitions.DefinitionVersions")
        .where(x->x.get("organizationId").eq(organizationId).and(x.get("ID").eq(id))))
        .first().orElseThrow(()->new NoSuchElementException("Definition unavailable"));
    try {
      String content=(String)row.get("content");
      if(!sha256(content).equals(row.get("contentHash"))) throw new IllegalStateException("Definition hash mismatch");
      Definition d=json.readValue(content,Definition.class);
      if(!organizationId.equals(d.organizationId())||!id.equals(d.id())
          ||!Objects.equals(row.get("version"),d.version())
          ||!Objects.equals(row.get("evaluatorVersion"),d.evaluatorVersion())
          ||!Objects.equals(row.get("schemaVersion"),d.schemaVersion())
          ||!Objects.equals(row.get("parentVersionId"),d.parentVersionId())
          ||!"PUBLISHED".equals(row.get("state"))||!"PUBLISHED".equals(d.state())) throw new IllegalStateException("Definition metadata mismatch or unpublished");
      return new Definition(d.organizationId(),d.id(),d.version(),d.parentVersionId(),"PUBLISHED",(String)row.get("contentHash"),d.evaluatorVersion(),d.schemaVersion(),d.nouns(),d.attributes(),d.verbs(),d.relations(),d.goals(),d.capabilities());
    } catch(java.io.IOException e) {throw new IllegalStateException("Invalid stored definition",e);}
  }
  public static String sha256(String content) {
    try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));}
    catch(java.security.NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
  }
}
