package com.mulino.application.trade;

import com.mulino.application.core.DomainContext;
import java.math.BigDecimal;
import java.time.Instant;

/** Receipt-owned provisional identity, before an inventory segment exists. */
public interface ReceiptEvidenceScopePort {
  Scope require(DomainContext context,String receiptRangeId);
  record Scope(String receiptRangeId,String itemId,String lotId,String placeId,
      String workId,BigDecimal quantity,String unit,Instant occurredAt,BigDecimal startQuantity) {}
}
