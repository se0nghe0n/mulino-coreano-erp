package com.mulino.application.trade.sales;

import com.mulino.application.core.*;
import com.mulino.application.trade.DispatchCargoPort;
import com.mulino.domain.trade.sales.SalesRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Boundary witnesses only; actual stored links and all-of authorization have a separate PG test. */
class PriorDispatchReadTest {
 final Instant at=Instant.parse("2026-10-07T09:00:00Z");
 final DomainContext c=new DomainContext("org","actor","actor",at,at);
 SalesRepository r=mock(SalesRepository.class);ReadAuthorizer auth=mock(ReadAuthorizer.class);
 DispatchCargoPort dispatch=mock(DispatchCargoPort.class);SalesQueries read=new SalesQueries(r,auth,dispatch);
 Map<String,Object> observation,actual;
 Map<String,Object> row(Object...pairs){var result=new HashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)result.put(pairs[i].toString(),pairs[i+1]);return result;}
 @BeforeEach void setup(){
  observation=row("ID","observation","organizationId","org","workId","work","itemId","item","customerId","customer","salesLineId","line","cargoScopeId","cargo","lotId","lot","claimedRangeRootId","root","rangeRootId",null,"unit","BOX","startQuantity",BigDecimal.ZERO,"quantity",new BigDecimal("20"),"occurredAt",at,"effectiveAt",at,"state","PROVISIONAL","dispatchId","dispatch","placeId","customer-place");
  actual=row("ID","dispatch","organizationId","org","workId","work","itemId","item","customerId","customer","salesLineId","line","cargoScopeId","cargo","lotId","lot","rangeRootId","root","unit","BOX","startQuantity",BigDecimal.ZERO,"quantity",new BigDecimal("30"),"occurredAt",at,"destinationId","customer-place");
  when(r.rows(c,"Observations")).thenReturn(List.of(observation));when(r.rows(c,"Deliveries")).thenReturn(List.of());
  when(auth.permittedScopes(eq(c),eq("getObject"),anyMap())).thenReturn(true);
  when(dispatch.requireDispatch(c,"dispatch")).thenAnswer(x->actual);
 }
 @SuppressWarnings("unchecked") Map<String,Object> check(){var q=new QueryRequest("getObject","observation",Map.of("objectType","DeliveryObservation"),Map.of(),50,null,null,at,at,null);return (Map<String,Object>)((Map<?,?>)read.query(c,q).data()).get("priorDispatchCheck");}
 @Test void matchedIsOnlyAPrerequisiteAndDoesNotChangeTheReport(){var before=new HashMap<>(observation);assertEquals("ALLOWED",check().get("status"));assertEquals("PRIOR_DISPATCH_MATCHED",check().get("reasonCode"));assertEquals(before,observation);verify(r,never()).insert(anyString(),anyMap());}
 @Test void noReferenceIsKnownDeniedAndNeverLooksUpAClaimedRoot(){observation.remove("dispatchId");observation.remove("salesLineId");assertEquals(row("status","DENIED","reasonCode","PRIOR_DISPATCH_MISSING","required",true,"linkedDispatchId",null,"salesLineId",null),check());verifyNoInteractions(dispatch);}
 @Test void missingOrCorruptHistoryIsExplicitlyUnknown(){
  when(dispatch.requireDispatch(c,"dispatch")).thenThrow(DomainError.forbidden());assertEquals("UNKNOWN",check().get("status"));assertEquals("DISPATCH_UNAVAILABLE",check().get("reasonCode"));
  doThrow(DomainError.invalid("history")).when(dispatch).requireDispatch(c,"dispatch");assertEquals("UNKNOWN",check().get("status"));assertEquals("DISPATCH_HISTORY_INCONSISTENT",check().get("reasonCode"));
 }
 @Test void crossOrganizationAndIncompleteLinksAreNotPositive(){actual.put("organizationId","foreign");assertEquals("UNKNOWN",check().get("status"));assertEquals("DISPATCH_UNAVAILABLE",check().get("reasonCode"));actual.put("organizationId","org");observation.remove("salesLineId");assertEquals("UNKNOWN",check().get("status"));assertEquals("DISPATCH_LINK_INCOMPLETE",check().get("reasonCode"));}
 @Test void readableContradictoryIdentityAndOverflowAreDenied(){for(String field:List.of("itemId","workId","customerId","salesLineId","cargoScopeId","lotId","claimedRangeRootId","unit")){Object before=observation.put(field,"wrong");assertEquals("DENIED",check().get("status"),field);assertEquals("DISPATCH_SCOPE_MISMATCH",check().get("reasonCode"));observation.put(field,before);}observation.put("quantity",new BigDecimal("31"));assertEquals("DENIED",check().get("status"));}
 @Test void unreadableLinkedDispatchDoesNotLeakItsScopeOrBecomeAllowed(){when(auth.permittedScopes(eq(c),eq("getObject"),argThat(s->s.get("TARGET").contains("dispatch")))).thenReturn(false);assertEquals("UNKNOWN",check().get("status"));assertEquals("DISPATCH_READ_SCOPE_UNAVAILABLE",check().get("reasonCode"));}
}
