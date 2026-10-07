package com.mulino.application.trade;

import com.mulino.application.core.DomainContext;
import java.math.BigDecimal;
import java.util.Map;

/** Domain-owned authoritative effect or correction; joins caller transaction, never fabricates stock. */
public interface SalesDeliveryCreditPort {
  Map<String,Object> deliveryCredit(DomainContext context,String lineId,String deliveryId);
}
