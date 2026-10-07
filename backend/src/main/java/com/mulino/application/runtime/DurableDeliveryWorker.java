package com.mulino.application.runtime;

import com.mulino.application.core.*;
import com.mulino.domain.runtime.RuntimeRepository;
import com.sap.cds.services.runtime.CdsRuntime;
import java.time.Duration;
import java.util.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** One bounded native-host tick. IO is outside transaction; failures remain durable. */
@Service
public class DurableDeliveryWorker {
 private final RuntimeService service;private final RuntimeRepository repository;private final ExecutionClock clock;private final CdsRuntime cds;
 private final ObjectMapper json=new ObjectMapper();
 public DurableDeliveryWorker(RuntimeService service,RuntimeRepository repository,ExecutionClock clock,CdsRuntime cds){this.service=service;this.repository=repository;this.clock=clock;this.cds=cds;}
 public int tick(String worker,Map<String,ExternalDeliveryAdapter> adapters){
  service.recoverExpiredDeliveries();
  var candidates=repository.db().queryForList("SELECT organizationId,actorId,stableRequestOwner,operation FROM mulino_runtime_Outbox WHERE status='PENDING' AND nextCheckAt<=? ORDER BY nextCheckAt,ID LIMIT 100",RuntimeRepository.at(clock.instant()));
  int[] delivered={0};
  for(var row:candidates){
   String operation=(String)row.get("operation");ExternalDeliveryAdapter adapter=adapters.get(operation);if(adapter==null)continue;
   DomainContext c=new DomainContext((String)row.get("organizationid"),(String)row.get("actorid"),(String)row.get("stablerequestowner"),clock.instant(),clock.instant());
   cds.requestContext().run(request->{
    Optional<Map<String,Object>> claimed;
    try{claimed=service.claimDelivery(c,worker,operation,adapter.support(operation),3,Duration.ofSeconds(5));}catch(org.springframework.security.access.AccessDeniedException|DomainError denied){return;}
    if(claimed.isEmpty())return;var delivery=claimed.get();
    ExternalDeliveryAdapter.Result result;
    try{var payload=json.readValue((String)delivery.get("payloadjson"),Map.class);result=adapter.deliver((String)delivery.get("operation"),(String)delivery.get("externaloperationid"),payload);}
    catch(RuntimeException failure){result=ExternalDeliveryAdapter.Result.UNKNOWN;}
    try{service.deliveryResult(c,(String)delivery.get("id"),((Number)delivery.get("fencingtoken")).longValue(),worker,result,null);}
    catch(DomainError expired){/* intent is durable; expiry recovery will require reconciliation */}
    delivered[0]++;
   });
  }
  return delivered[0];
 }
}
