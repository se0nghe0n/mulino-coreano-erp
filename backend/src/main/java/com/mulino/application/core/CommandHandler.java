package com.mulino.application.core;
import java.util.*;
/** Explicit typed business handlers. All callbacks run inside the command transaction. */
public interface CommandHandler {
  Set<String> capabilities();
  Map<String,Object> execute(DomainContext context, Map<String,Object> intent);
  default String semanticVersion(){return "1.0.0";}
  default Set<String> definitionVersions(){return Set.of();}
  default Set<String> evaluatorVersions(){return Set.of("core-v1");}
  default Set<String> intentKinds(){return Set.of("COMMAND");}
  /** Explicit installed control-plane handler classification; never derived from request data. */
  default boolean mutatesAuthorization(String capability){return false;}
  /** Validate slots, cardinality and effect type without writing. Return trusted effect scopes. */
  default CommandPreparation prepare(DomainContext context,Map<String,Object> intent){
    throw DomainError.unsupported();
  }
}
