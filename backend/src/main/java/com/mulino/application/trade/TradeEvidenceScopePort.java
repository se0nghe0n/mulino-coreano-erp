package com.mulino.application.trade;

import com.mulino.application.core.DomainContext;
import java.util.*;

/** Domain meaning of immutable original/event payload against a persisted exact target. */
public interface TradeEvidenceScopePort {
  Set<String> eventKinds();
  /** Reject EVIDENCE_UNVERIFIED when original/event/claim differ from the exact server scope. */
  Scope require(DomainContext context,String physicalScopeId,Map<String,Object> claim,
      Map<String,Object> event,Map<String,Object> document,byte[] original);
  record Scope(Map<String,Object> canonicalFields,Map<String,List<String>> scopes,String occurrenceIdentity,String semanticHash) {
    public Scope(Map<String,Object> canonicalFields,Map<String,List<String>> scopes){this(canonicalFields,scopes,null,null);}
    public Scope(Map<String,Object> canonicalFields,Map<String,List<String>> scopes,String occurrenceIdentity){this(canonicalFields,scopes,occurrenceIdentity,null);}
    public Scope {canonicalFields=Map.copyOf(canonicalFields);scopes=Map.copyOf(scopes);}
  }
}
