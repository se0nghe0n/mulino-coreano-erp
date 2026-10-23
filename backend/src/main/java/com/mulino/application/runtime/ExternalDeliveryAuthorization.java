package com.mulino.application.runtime;

import com.mulino.application.core.DomainContext;
import java.util.Map;

/** Handler-owned revalidation only: never re-executes or reconsumes the committed command. */
public interface ExternalDeliveryAuthorization {
 boolean supports(String operation);
 void require(DomainContext context,Delivery delivery);
 record Delivery(String outboxId,String commandId,String externalOperationId,String operation,
                 String actorId,String stableRequestOwner,Map<String,Object> payload) {
  public Delivery { payload=Map.copyOf(payload); }
 }
}
