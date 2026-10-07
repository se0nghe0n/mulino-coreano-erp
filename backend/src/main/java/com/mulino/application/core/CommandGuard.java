package com.mulino.application.core;
import java.util.*;
/** Control owns current policy/authority and approval checks, after sorted common fences. */
public interface CommandGuard {
  void fence(DomainContext context,CommandPreparation preparation);
  void verify(DomainContext context,String capability,String canonicalHash,
      CommandPreparation preparation,Map<String,Object> intent);
  default void consume(DomainContext context,CommandPreparation preparation,Map<String,Object> intent,String commandId){}
}
