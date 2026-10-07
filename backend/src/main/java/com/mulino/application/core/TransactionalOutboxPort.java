package com.mulino.application.core;
import java.util.Map;
/** Runtime owns persistence. Called in the same Spring transaction as business effects. */
public interface TransactionalOutboxPort {
  String enqueue(DomainContext context,String commandId,String externalOperationId,
      String operation,Map<String,Object> payload);
}
