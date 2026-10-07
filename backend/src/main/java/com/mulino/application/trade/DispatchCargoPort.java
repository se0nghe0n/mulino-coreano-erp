package com.mulino.application.trade;

import com.mulino.application.core.DomainContext;
import java.math.BigDecimal;
import java.util.Map;

/** Authoritative organization-scoped persisted facts; joins caller transaction, never creates stock. */
public interface DispatchCargoPort {
  Map<String,Object> requireDispatch(DomainContext context,String dispatchId);
  Map<String,Object> requireCargoScope(DomainContext context,String cargoScopeId);
}
