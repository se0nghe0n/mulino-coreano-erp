package com.mulino.domain.evidence;
import com.mulino.application.core.DomainContext;
import java.util.Map;
/** Runtime-owned organization/operation lookup. Returned Work is server data, never a claim grant. */
public interface ExternalOperationScopePort {
  Map<String,Object> require(DomainContext context,String externalOperationId);
}
