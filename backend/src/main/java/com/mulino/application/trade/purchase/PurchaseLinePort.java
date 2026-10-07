package com.mulino.application.trade.purchase;
import com.mulino.application.core.DomainContext;
import java.math.BigDecimal;
import java.util.Map;
/** Trade allocation port; calls join the caller's transaction and never create stock. */
public interface PurchaseLinePort {
 Map<String,Object> requireLine(DomainContext context,String lineId);
 default Map<String,Object> receiptCredit(DomainContext context,String lineId,String occurrenceId){throw com.mulino.application.core.DomainError.unsupported();}
 Map<String,Object> creditReceipt(DomainContext context,String lineId,String canonicalOccurrenceId,BigDecimal quantity,String unit);
 void recordExecutionEffect(DomainContext context,String lineId,String kind,String referenceId,BigDecimal quantity,String unit);
}
