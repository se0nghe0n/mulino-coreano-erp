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
  /**
   * Product handlers publish their slot names in contracts/capability-slots.json (CapabilitySlots). Only a test fixture
   * handler outside the product contract returns false; it is then neither listed nor slot-checked.
   */
  default boolean publishesSlots(){return true;}
  /** Explicit subject nouns and concrete effect targets; scopes never imply subject identity. */
  default List<SubjectBinding> subjectBindings(DomainContext context,Map<String,Object> intent,CommandPreparation preparation){return List.of();}
  /** Validate slots, cardinality and effect type without writing. Return trusted effect scopes. */
  default CommandPreparation prepare(DomainContext context,Map<String,Object> intent){
    throw DomainError.unsupported();
  }
}
