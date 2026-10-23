package com.mulino.adapters.verification;

import com.mulino.application.identity.IdentityAuthorization;
import com.mulino.domain.identity.IdentityRepository;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Deliberately absent unless an explicitly enabled, loopback verification process owns it. */
@Configuration(proxyBeanMethods=false)
@Conditional(McpCaptureConfiguration.Enabled.class)
public class McpCaptureConfiguration {
  public static class Enabled implements Condition {
    @Override public boolean matches(ConditionContext c, AnnotatedTypeMetadata metadata) {
      var e=c.getEnvironment();
      return Set.copyOf(Arrays.asList(e.getActiveProfiles())).equals(Set.of("local","verification"))
          && "true".equals(e.getProperty("mulino.verification.mcp-capture-enabled"))
          && Set.of("127.0.0.1","::1","[::1]").contains(e.getProperty("server.address",""));
    }
  }
  @Bean McpCaptureStore mcpCaptureStore() { return new McpCaptureStore(); }
  @Bean McpIdentitySnapshot mcpIdentitySnapshot(IdentityAuthorization auth,IdentityRepository repository,CdsRuntime runtime) {
    return new McpIdentitySnapshot(auth,repository,runtime);
  }
  @Bean FilterRegistrationBean<McpCaptureFilter> mcpCaptureFilter(McpCaptureStore store,McpIdentitySnapshot snapshots) {
    var registration=new FilterRegistrationBean<>(new McpCaptureFilter(store,snapshots));
    registration.setOrder(-99); // Spring Security's servlet filter is -100; use its verified SecurityContext.
    registration.addUrlPatterns("/mcp/ontology");
    return registration;
  }
}
