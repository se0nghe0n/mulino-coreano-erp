package com.mulino.application.trade;
import com.mulino.application.core.*;
import com.mulino.domain.evidence.EvidenceSubjectPort;
import com.mulino.domain.inventory.InventoryRepository;
import java.util.*;
import org.springframework.stereotype.Component;
/** Fixed tenant-scoped physical dispatch/cargo nouns; no payload-defined entity access. */
@Component
public final class InventoryTradeEvidenceSubject implements EvidenceSubjectPort {
 private final InventoryRepository repository;private final ExecutionClock clock;
 public InventoryTradeEvidenceSubject(InventoryRepository repository,ExecutionClock clock){this.repository=repository;this.clock=clock;}
 public Set<String> subjectKinds(){return Set.of("DISPATCH","CARGO_SCOPE");}
 public Map<String,Object> require(String org,String kind,String id){
  var c=new DomainContext(org,"00000000-0000-0000-0000-000000000000","lookup",clock.instant(),clock.instant());
  if("DISPATCH".equals(kind)){var dispatch=new LinkedHashMap<>(repository.object(c,"Dispatches",id));dispatch.put("placeId",dispatch.get("destinationId"));return dispatch;}
  if(!"CARGO_SCOPE".equals(kind))throw DomainError.invalid("Physical dispatch evidence noun required");
  var cargo=new LinkedHashMap<>(repository.object(c,"CargoScopes",id));var dispatch=repository.object(c,"Dispatches",cargo.get("dispatchId").toString());
  if(!id.equals(dispatch.get("cargoScopeId"))||!Objects.equals(cargo.get("workId"),dispatch.get("workId"))||!Objects.equals(cargo.get("customerId"),dispatch.get("customerId")))throw DomainError.invalid("Cargo subject must belong to one exact dispatch");
  cargo.put("itemId",dispatch.get("itemId"));cargo.put("lotId",dispatch.get("lotId"));cargo.put("placeId",dispatch.get("destinationId"));return cargo;
 }
}
