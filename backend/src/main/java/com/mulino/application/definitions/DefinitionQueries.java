package com.mulino.application.definitions;

import com.mulino.application.core.*;
import com.mulino.domain.definitions.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class DefinitionQueries implements QueryHandler {
  private final DefinitionRepository repository;
  private final ReadAuthorizer authorizer;
  public DefinitionQueries(DefinitionRepository repository,ReadAuthorizer authorizer) {
    this.repository=repository;this.authorizer=authorizer;
  }
  public Set<String> operations() {return Set.of("getDefinition");}
  public QueryResult query(DomainContext context,QueryRequest request) {
    authorizer.authorize(context,"getDefinition",request.id());
    if(request.id()==null) throw new IllegalArgumentException("Pinned definition ID required");
    var d=repository.get(context.organizationId(),request.id());
    if(request.definitionVersion()!=null&&!request.definitionVersion().equals(d.version())&&!request.definitionVersion().equals(d.id())) throw new IllegalArgumentException("Definition pin mismatch");
    return QueryResult.of(d,Map.of("definitionVersionId",d.id(),"definitionVersion",d.version()));
  }
}
