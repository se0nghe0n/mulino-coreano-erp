package com.mulino.application.runtime;

import com.mulino.application.core.DomainContext;
import java.util.Map;
/** Domain/control rechecks original immutable command scope and current policy. */
public interface ExternalDeliveryAuthorization {
 void require(DomainContext context,String commandId,String operation,Map<String,Object> payload);
}
