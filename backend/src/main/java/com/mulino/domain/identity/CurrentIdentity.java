package com.mulino.domain.identity;
import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.stereotype.Component;
@Component public class CurrentIdentity {
 public record Identity(String actor,String organization,String owner){}
 public Identity get(){ var a=SecurityContextHolder.getContext().getAuthentication(); if(a==null || !(a.getPrincipal() instanceof Jwt j)) throw new org.springframework.security.access.AccessDeniedException("Authentication required"); return new Identity(j.getSubject(),j.getClaimAsString("organizationId"),j.getClaimAsString("stableRequestOwner")); }
}
