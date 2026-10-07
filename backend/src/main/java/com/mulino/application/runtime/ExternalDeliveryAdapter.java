package com.mulino.application.runtime;

import java.util.Map;
/** Explicit capabilities. Calls happen after IN_FLIGHT is committed, outside DB tx. */
public interface ExternalDeliveryAdapter {
  record Support(boolean idempotency,boolean authoritativeLookup){}
  enum Result { CONFIRMED_SUCCESS, CONFIRMED_FAILURE, UNKNOWN }
  Support support(String operation);
  Result deliver(String operation,String externalOperationId,Map<String,Object> payload);
  Result lookup(String operation,String externalOperationId);
}
