package com.mulino.application.trade.settlement;
import com.mulino.application.core.DomainContext;
import java.math.BigDecimal;
import java.util.Map;
/** Server facts in the common transaction. Reference is a canonical receipt or delivery UUID. */
public interface SettlementTradeFacts {
 Map<String,Object> line(DomainContext c,String scopeKind,String lineId);
 /** quantity is currently recognized real contribution; occurrenceId is authoritative evidence. */
 Map<String,Object> contribution(DomainContext c,String scopeKind,String lineId,String referenceId);
 void invoiced(DomainContext c,String scopeKind,String lineId,String invoiceId,BigDecimal quantity,String unit);
}
