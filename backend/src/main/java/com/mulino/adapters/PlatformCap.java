package com.mulino.adapters;
import com.mulino.application.PlatformCommands;import com.sap.cds.services.handler.EventHandler;import com.sap.cds.services.handler.annotations.*;import com.sap.cds.services.cds.CdsReadEventContext;import com.sap.cds.services.EventContext;import org.springframework.stereotype.Component;
@Component @ServiceName("PlatformService") public class PlatformCap implements EventHandler {
 private final PlatformCommands service;public PlatformCap(PlatformCommands service){this.service=service;}
 @On(event="READ",entity="PlatformService.Scopes") public void read(CdsReadEventContext c){c.setResult(service.list());}
 @On(event="reserve") public void reserve(EventContext c){var r=service.reserve((String)c.get("scopeId"),(String)c.get("quantity"),((Number)c.get("expectedRevision")).intValue(),(String)c.get("idempotencyKey"));c.put("result",r.toString());c.setCompleted();}
}
