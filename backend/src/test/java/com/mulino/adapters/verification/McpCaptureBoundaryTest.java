package com.mulino.adapters.verification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.*;

class McpCaptureBoundaryTest {
  @Test void enabledOnlyForExplicitLocalVerificationAndLiteralLoopbackBinding() {
    var environment=new MockEnvironment();var context=mock(ConditionContext.class);when(context.getEnvironment()).thenReturn(environment);
    var condition=new McpCaptureConfiguration.Enabled();
    for(var profiles:List.of(new String[]{},new String[]{"local"},new String[]{"verification"},new String[]{"production"},new String[]{"local","verification","production"})) {
      environment.setActiveProfiles(profiles);environment.setProperty("mulino.verification.mcp-capture-enabled","true");environment.setProperty("server.address","127.0.0.1");
      assertFalse(condition.matches(context,null));
    }
    environment.setActiveProfiles("local","verification");assertTrue(condition.matches(context,null));
    for(String address:List.of("0.0.0.0","localhost","192.0.2.1","::")){environment.setProperty("server.address",address);assertFalse(condition.matches(context,null));}
    environment.setProperty("server.address","127.0.0.1");environment.setProperty("mulino.verification.mcp-capture-enabled","false");assertFalse(condition.matches(context,null));
  }
  @Test void socketAddressesCannotBeReplacedWithForwardedLoopbackHeaders() {
    var request=new MockHttpServletRequest();request.setLocalAddr("127.0.0.1");request.setRemoteAddr("192.0.2.1");
    request.addHeader("X-Forwarded-For","127.0.0.1");assertFalse(McpCaptureFilter.loopback(request));
    request.setRemoteAddr("127.0.0.1");assertTrue(McpCaptureFilter.loopback(request));
    request.setLocalAddr("192.0.2.2");assertFalse(McpCaptureFilter.loopback(request));
  }
  @Test void productionContextRegistersNeitherObserverEndpointNorFilterEvenWithEnableFlag() {
    try(var context=new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
      context.getEnvironment().setActiveProfiles("local","verification","production");
      context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("fixture",Map.of("mulino.verification.mcp-capture-enabled","true","server.address","127.0.0.1")));
      context.register(McpCaptureConfiguration.class,McpCaptureController.class);context.refresh();
      assertEquals(0,context.getBeansOfType(McpCaptureController.class).size());assertEquals(0,context.getBeansOfType(McpCaptureStore.class).size());
      assertEquals(0,context.getBeansOfType(org.springframework.boot.web.servlet.FilterRegistrationBean.class).size());
    }
  }
  @Test void foreignOwnerCannotReadOrConsumeReceiptAndSuccessfulReadIsSingleUse() {
    var store=new McpCaptureStore();var owner=Map.<String,Object>of("subject","alice","organizationId","a");
    String id=store.put(owner,Map.of("value","own snapshot"));
    assertTrue(store.take(id,Map.of("subject","bob","organizationId","a")).isEmpty());
    assertTrue(store.take(id,Map.of("subject","alice","organizationId","b")).isEmpty());
    assertTrue(store.take(UUID.randomUUID().toString(),owner).isEmpty());
    assertEquals("own snapshot",store.take(id,owner).orElseThrow().get("value"));assertTrue(store.take(id,owner).isEmpty());
  }
  @Test void expiryPrunesBoundedStoreWithoutBackgroundThreadsOrPersistentResources() {
    class Mutable extends Clock {Instant now=Instant.parse("2026-10-24T00:00:00Z");public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return now;}}
    var clock=new Mutable();var store=new McpCaptureStore(clock,Duration.ofSeconds(1),1);var owner=Map.<String,Object>of("actorId","a");
    String id=store.put(owner,Map.of());assertNull(store.put(owner,Map.of()));
    clock.now=clock.now.plusSeconds(1);assertTrue(store.take(id,owner).isEmpty());assertNotNull(store.put(owner,Map.of()));
  }
}
