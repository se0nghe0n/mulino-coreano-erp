package com.mulino.application.trade.sales;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mulino.application.core.DomainError;
import java.util.Map;

/** A reported identity is opaque until the source and actual dispatch reconcile. */
final class DeliveryObservationClaim {
 private static final ObjectMapper JSON = new ObjectMapper();
 private DeliveryObservationClaim() {}
 static void capture(Map<String,Object> row, Map<String,Object> input) {
  row.put("claimedRangeRootId", input.get("rangeRootId"));
  try { row.put("claimPayloadJson", JSON.writeValueAsString(input)); }
  catch (JsonProcessingException e) { throw DomainError.invalid("Delivery claim must be JSON"); }
  // Never look up a claimed root: existence, organization and item are not disclosed at intake.
  row.put("rangeRootId", null);
 }
 static String root(Map<String,Object> row) {
  // Legacy rows retain their existing verified relation; their unknown original payload is not invented.
  Object value = row.get("claimedRangeRootId");
  if (value == null) value = row.get("rangeRootId");
  if (!(value instanceof String text) || text.isBlank()) throw DomainError.invalid("Delivery report root missing");
  return text;
 }
}
