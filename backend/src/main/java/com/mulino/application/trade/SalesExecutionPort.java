package com.mulino.application.trade;

import com.mulino.application.core.DomainContext;
import java.math.BigDecimal;
import java.util.Map;

/** Domain-owned authoritative effect or correction; joins caller transaction, never fabricates stock. */
public interface SalesExecutionPort {
  void recordExecutionEffect(DomainContext context,String lineId,String kind,String referenceId,BigDecimal quantity,String unit);
}
