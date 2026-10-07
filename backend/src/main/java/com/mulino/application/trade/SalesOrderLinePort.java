package com.mulino.application.trade;

import com.mulino.application.core.DomainContext;
import java.math.BigDecimal;
import java.util.Map;

/** Authoritative organization-scoped persisted facts; joins caller transaction, never creates stock. */
public interface SalesOrderLinePort {
  Map<String,Object> requireLine(DomainContext context,String lineId);
  BigDecimal remainingQuantity(DomainContext context,String lineId);
}
