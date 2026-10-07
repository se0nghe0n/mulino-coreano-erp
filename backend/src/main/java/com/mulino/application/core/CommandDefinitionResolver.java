package com.mulino.application.core;
import com.mulino.domain.definitions.*;
import com.sap.cds.ql.Select;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.*;
import org.springframework.stereotype.Component;
/** Pinned published meaning, independently checked from current execution policy. */
@Component
public class CommandDefinitionResolver {
  private final PersistenceService db;private final DefinitionRepository definitions;
  public CommandDefinitionResolver(PersistenceService db,DefinitionRepository definitions){this.db=db;this.definitions=definitions;}
  public void verify(DomainContext context,Map<String,Object> intent,CommandHandler handler){
    String ref=(String)intent.get("definitionVersion");boolean uuid;try{UUID.fromString(ref);uuid=true;}catch(IllegalArgumentException e){uuid=false;}
    final String column=uuid?"ID":"version";
    var rows=db.run(Select.from("mulino.definitions.DefinitionVersions").where(b->b.get("organizationId").eq(context.organizationId()).and(b.get(column).eq(ref)).and(b.get("state").eq("PUBLISHED")))).listOf(Map.class);
    if(rows.size()!=1)throw DomainError.unsupported();
    Definition d;try{d=definitions.get(context.organizationId(),(String)rows.get(0).get("ID"));}catch(NoSuchElementException|IllegalStateException e){throw DomainError.unsupported();}
    if(!handler.definitionVersions().isEmpty()&&!handler.definitionVersions().contains(d.version())&&!handler.definitionVersions().contains(d.id()))throw DomainError.unsupported();
    if(d.capabilities().stream().noneMatch(cap->cap.capabilityId().equals(intent.get("capabilityId"))&&cap.semanticVersion().equals(handler.semanticVersion())&&handler.evaluatorVersions().contains(cap.evaluatorVersion())&&"1.0.0".equals(cap.inputSchemaVersion())&&"1.0.0".equals(cap.outputSchemaVersion())))throw DomainError.unsupported();
  }
}
