package com.mulino.application.core;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

/** Identity module maps verified issuer/subject to server identity and current grants. */
public interface ReadAuthorizer {
  DomainContext context(Instant asOf, Instant knownAt);
  void authorize(DomainContext context, String capabilityId, String targetId);
  boolean permittedScopes(DomainContext context,String capabilityId,Map<String,? extends Collection<String>> candidateScopes);
  default void authorizeScopes(DomainContext context,String capabilityId,Map<String,? extends Collection<String>> candidateScopes) {
    if(!permittedScopes(context,capabilityId,candidateScopes))throw DomainError.forbidden();
  }
  default void authorizeScope(DomainContext context,String capabilityId,String scopeKind,String scopeId) {
    authorize(context,capabilityId,scopeId);
  }
  default boolean permitted(DomainContext context,String capabilityId,String scopeId,String scopeKind) {
    try { authorizeScope(context,capabilityId,scopeKind,scopeId); return true; }
    catch(DomainError denied) { if(!"FORBIDDEN".equals(denied.code()))throw denied; return false; }
  }
}
