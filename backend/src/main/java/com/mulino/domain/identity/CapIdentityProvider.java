package com.mulino.domain.identity;

import com.sap.cds.services.request.UserInfo;
import com.sap.cds.services.runtime.UserInfoProvider;
import org.springframework.stereotype.Component;

@Component
public class CapIdentityProvider implements UserInfoProvider {
  private final CurrentIdentity identity;

  public CapIdentityProvider(CurrentIdentity identity) {
    this.identity = identity;
  }

  @Override
  public UserInfo get() {
    try {
      var i = identity.get();
      return UserInfo.create()
          .setName(i.actor())
          .setTenant(i.organization())
          .setIsAuthenticated(true);
    } catch (org.springframework.security.access.AccessDeniedException anonymous) {
      return UserInfo.create().setName("anonymous").setIsAuthenticated(false);
    }
  }
}
