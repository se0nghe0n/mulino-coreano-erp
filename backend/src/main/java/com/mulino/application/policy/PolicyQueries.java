package com.mulino.application.policy;
import com.mulino.application.core.*;
import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.domain.governance.PolicyRepository;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class PolicyQueries implements QueryHandler {
 private final PolicyRepository repo;private final IdentityAuthorization auth;
 public PolicyQueries(PolicyRepository repo,IdentityAuthorization auth){this.repo=repo;this.auth=auth;}
 public Set<String> operations(){return Set.of("getPolicy");}
 public QueryResult query(DomainContext c,QueryRequest r){auth.authorize(c,"getPolicy",r.id());var row=repo.row("PolicyDrafts",c.organizationId(),r.id()).orElseGet(()->repo.row("PolicyVersions",c.organizationId(),r.id()).orElseThrow(DomainError::forbidden));return QueryResult.of(row,Map.of("organizationId",c.organizationId()));}
}
