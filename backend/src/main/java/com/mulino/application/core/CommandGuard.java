package com.mulino.application.core;
import java.util.*;
/** Control owns current policy/authority and approval checks, after sorted common fences. */
public interface CommandGuard {
  void fence(DomainContext context,CommandPreparation preparation);
  void verify(DomainContext context,String capability,String canonicalHash,
      CommandPreparation preparation,Map<String,Object> intent);
  /** Controls use an expiry-checked pre-effect proof for their own intentional authority mutation. */
  default void verifyCommit(DomainContext context,String capability,String canonicalHash,
      CommandPreparation preparation,Map<String,Object> intent,boolean mutatesAuthorization){
    verify(context,capability,canonicalHash,preparation,intent);
  }
  default void consume(DomainContext context,CommandPreparation preparation,Map<String,Object> intent,String commandId){}
}
