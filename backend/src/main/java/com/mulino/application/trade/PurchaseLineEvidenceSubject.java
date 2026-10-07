package com.mulino.application.trade;

import com.mulino.application.core.*;
import com.mulino.domain.evidence.EvidenceSubjectPort;
import com.mulino.domain.trade.purchase.PurchaseRepository;
import java.util.*;
import org.springframework.stereotype.Component;

/** Invoice originals bind to an immutable applied purchase line, never a payload-selected entity. */
@Component
public final class PurchaseLineEvidenceSubject implements EvidenceSubjectPort {
  private final PurchaseRepository repository;private final ExecutionClock clock;
  public PurchaseLineEvidenceSubject(PurchaseRepository repository,ExecutionClock clock){this.repository=repository;this.clock=clock;}
  public Set<String> subjectKinds(){return Set.of("PURCHASE_ORDER_LINE");}
  public Map<String,Object> require(String org,String kind,String id){
    if(!"PURCHASE_ORDER_LINE".equals(kind))throw DomainError.invalid("Purchase line evidence subject required");
    var c=new DomainContext(org,"00000000-0000-0000-0000-000000000000","lookup",clock.instant(),clock.instant());
    var result=new LinkedHashMap<>(repository.require(c,"OrderLines",id));
    result.put("placeId",result.get("destinationId"));return result;
  }
}
