package com.mulino.application.definitions;

import com.mulino.application.core.*;
import com.mulino.domain.definitions.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class DefinitionQueries implements QueryHandler {
  private final DefinitionRepository repository;
  private final ReadAuthorizer authorizer;
  private final DefinitionLifecycleRepository lifecycle;
  public DefinitionQueries(DefinitionRepository repository,ReadAuthorizer authorizer,DefinitionLifecycleRepository lifecycle) {
    this.repository=repository;this.authorizer=authorizer;this.lifecycle=lifecycle;
  }
  public Set<String> operations() {return Set.of("getDefinition");}
  public QueryResult query(DomainContext context,QueryRequest request) {
    authorizer.authorize(context,"getDefinition",request.id());
    if(request.id()==null) throw new IllegalArgumentException("Pinned definition ID required");
    // An unknown or other-organization definition ID is an unavailable scope (plan §3.4), not a server error.
    // Plan §8: drafts and reviews are readable packages too; state is the stored DRAFT|IN_REVIEW|PUBLISHED and lifecycleState
    // adds the ACTIVE pointer and RETIRED_FOR_NEW_WORK. revision is the lifecycle revision the next lifecycle command expects.
    Map<String,Object> d;
    try{d=repository.read(context.organizationId(),request.id());}catch(java.util.NoSuchElementException unavailable){throw DomainError.forbidden();}
    if(request.definitionVersion()!=null&&!request.definitionVersion().equals(d.get("version"))&&!request.definitionVersion().equals(d.get("id"))) throw new IllegalArgumentException("Definition pin mismatch");
    String org=context.organizationId(),id=request.id();
    d.put("revision",lifecycle.revision(org,id));
    d.put("lifecycleState",lifecycle.retired(org,id)?"RETIRED_FOR_NEW_WORK":lifecycle.pointer(org).map(p->id.equals(p.get("definitionId"))).orElse(false)?"ACTIVE":d.get("state"));
    return QueryResult.of(d,Map.of("definitionVersionId",id,"definitionVersion",String.valueOf(d.get("version"))));
  }
}
