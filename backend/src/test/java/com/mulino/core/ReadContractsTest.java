package com.mulino.core;

import static org.junit.jupiter.api.Assertions.*;
import com.mulino.application.core.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReadContractsTest {
  private static final String ID="00000000-0000-0000-0000-000000000001";
  private final DomainContext context=new DomainContext(ID,ID,ID,Instant.parse("2026-10-07T09:00:00Z"),Instant.parse("2026-10-07T09:00:01Z"));
  @Test void numericTransportRejectsSilentRoundingAndBinary(){
    assertEquals("0.5",DecimalValue.of("0.5","BOX").value());
    for(String value:List.of("1e2","01","0.1234567890123","999999999999999999999999999","NaN"))assertThrows(DomainError.class,()->DecimalValue.of(value,"EA"));
    assertThrows(DomainError.class,()->TransportValues.normalize(0.1d));
    assertEquals(Map.of("quantity","0.1","unit","BOX"),TransportValues.normalize(Map.of("quantity",new BigDecimal("0.1000"),"unit","BOX")));
  }
  @Test void requestRejectsIdentityOverrideAndUnboundedPaging(){
    assertThrows(DomainError.class,()->QueryRequests.parse("getObject",Map.of("id",ID,"actorId",ID)));
    assertThrows(DomainError.class,()->QueryRequests.parse("getObject",Map.of("id",ID,"limit",201)));
    assertThrows(DomainError.class,()->QueryRequests.parse("getObject",Map.of("id",ID,"asOf","Friday")));
    assertEquals(ID,QueryRequests.parse("getWork",Map.of("workId",ID)).id());
  }
  @Test void stableCursorIsBoundToIdentityScopeAndTime(){
    var q=QueryRequests.parse("searchWorks",Map.of("scope",Map.of("itemId",ID)));
    String cursor=ReadCursor.encode(ID,context,q);assertEquals(ID,ReadCursor.decode(cursor,context,q));
    var other=new DomainContext(ID,"00000000-0000-0000-0000-000000000002",ID,context.asOf(),context.knownAt());
    assertThrows(DomainError.class,()->ReadCursor.decode(cursor,other,q));
    assertThrows(DomainError.class,()->ReadCursor.decode(cursor,context,QueryRequests.parse("searchWorks",Map.of())));
  }
  @Test void errorsRetainSafeOutcomeAndZeroEffects(){
    var response=DomainError.forbidden().response();assertEquals("REJECTED",response.get("outcome"));assertEquals(Map.of(),response.get("effects"));
    assertEquals("FORBIDDEN",((Map<?,?>)response.get("error")).get("code"));
  }
}
