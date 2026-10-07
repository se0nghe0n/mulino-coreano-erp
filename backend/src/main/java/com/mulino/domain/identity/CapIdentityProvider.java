package com.mulino.domain.identity;

import com.sap.cds.services.request.UserInfo;
import com.sap.cds.services.runtime.UserInfoProvider;
import org.springframework.stereotype.Component;

@Component
public class CapIdentityProvider implements UserInfoProvider {
  private final CurrentIdentity identity;
  private final org.springframework.core.env.Environment environment;

  public CapIdentityProvider(CurrentIdentity identity,org.springframework.core.env.Environment environment) {
    this.identity = identity;this.environment=environment;
  }

  @Override
  public UserInfo get() {
    try {
      var i = identity.get();
      var user=UserInfo.create()
          .setName(i.actor())
          .setTenant(i.organization())
          .setIsAuthenticated(true);
      if(java.util.Arrays.asList(environment.getActiveProfiles()).contains("platform-spike"))user.addRole("platform-spike");
      return user;
    } catch (org.springframework.security.access.AccessDeniedException anonymous) {
      return UserInfo.create().setName("anonymous").setIsAuthenticated(false);
    }
  }
}
