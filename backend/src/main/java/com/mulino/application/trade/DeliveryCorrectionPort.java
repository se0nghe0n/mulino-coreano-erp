package com.mulino.application.trade;

import com.mulino.application.core.DomainContext;
import java.math.BigDecimal;
import java.util.Map;

/** Domain-owned authoritative effect or correction; joins caller transaction, never fabricates stock. */
public interface DeliveryCorrectionPort {
  Map<String,Object> correctionImpact(DomainContext context,String deliveryId,String canonicalId);
}
