package com.mulino.application.trade;

import com.mulino.application.core.DomainContext;
import java.math.BigDecimal;
import java.util.Map;

/** Authoritative organization-scoped persisted facts; joins caller transaction, never creates stock. */
public interface ReturnQuantityPort {
  BigDecimal returnedQuantity(DomainContext context,String deliveryId);
}
