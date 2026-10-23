package com.mulino.application.evaluation;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The immutable capture keeps lineage; duty identity follows the actual proof it used. */
class GoalShortfallBasisTest {
  final AssessmentService service=new AssessmentService(null,null,null,null,null,null,null);
  final ObjectMapper json=new ObjectMapper();

  Map<String,Object> snapshot(boolean child) throws Exception {
    return json.readValue("""
      {"goalId":"goal","goal":{"ID":"goal","workId":"parent","definitionVersionId":"definition"},
       "goalSlots":{"targetQuantity":"100","unit":"BOX","quantityMode":"CUMULATIVE_EVENT"},
       "definitionHash":"definition-hash","policy":{"ID":"policy","contentHash":"policy-hash"},
       "captureBoundary":{"asOf":"2026-10-07T02:00:00Z","knownAt":"2026-10-07T02:00:00Z","admissionPurpose":"ASSESS_GOAL"},
       "conditions":[{"outcome":"UNSATISFIED"}],
       "facts":{"properties":{"Receipt.quantity":[{"sourceId":"receipt","canonicalId":"receipt",
         "physicalScopeId":"physical","value":"90","unit":"BOX","sourceVersion":"1",
         "sourceHash":"canonical-hash","verified":true,"evidenceRefs":["verification","document"]}]},
         "relations":{},"sources":{"evidence.CanonicalOccurrences":[{"ID":"receipt","workId":"parent",
           "physicalScopeId":"physical","quantity":"90","unit":"BOX","revision":1}],
           "evidence.DocumentVersions":[{"ID":"document","sha256":"document-hash"}],
           "work.Contributions":[{"ID":"credit30","occurrenceId":"receipt","sourceGoalVersionId":"child-goal",
             "startQuantity":"0","quantity":"30","unit":"BOX"}]}}}
      """,Map.class);
  }
  @SuppressWarnings("unchecked") Map<String,Object> map(Object value){return (Map<String,Object>)value;}
  Map<String,Object> sources(Map<String,Object> snapshot){return map(map(snapshot.get("facts")).get("sources"));}
  @SuppressWarnings("unchecked") List<Map<String,Object>> credits(Map<String,Object> snapshot){return (List<Map<String,Object>>)sources(snapshot).get("work.Contributions");}
  @SuppressWarnings("unchecked") Map<String,Object> fact(Map<String,Object> snapshot){return ((List<Map<String,Object>>)map(map(snapshot.get("facts")).get("properties")).get("Receipt.quantity")).getFirst();}
  @SuppressWarnings("unchecked") Map<String,Object> actual(Map<String,Object> snapshot){return ((List<Map<String,Object>>)sources(snapshot).get("evidence.CanonicalOccurrences")).getFirst();}
  void child(Map<String,Object> snapshot){
    map(snapshot.get("goal")).put("workId","child");fact(snapshot).put("value","30");
    fact(snapshot).put("physicalScopeId","physical|credit30");fact(snapshot).put("evidenceRefs",List.of("verification","document","credit30"));
  }
  @Test void partialThenFullChildAllocationKeepsDirectParentNinetyAndFrozenCapture() throws Exception {
    var first=snapshot(false);var saved=json.writeValueAsString(first);var second=snapshot(false);
    credits(second).add(Map.of("ID","credit60","occurrenceId","receipt","startQuantity","30","quantity","60","unit","BOX"));
    assertNotEquals(json.writeValueAsString(first),json.writeValueAsString(second));
    assertEquals(service.shortfallBasisHash(first),service.shortfallBasisHash(second));
    assertEquals(saved,json.writeValueAsString(first),"Hashing must not erase lineage from the stored snapshot");
  }
  @ParameterizedTest @ValueSource(strings={"physicalScopeId","value","unit","sourceVersion","sourceHash","evidenceRefs","verified"})
  void changedCurrentQuantityProofCannotReuseFirstDuty(String field) throws Exception {
    var first=snapshot(false);var second=snapshot(false);
    fact(second).put(field,field.equals("evidenceRefs")?List.of("other"):field.equals("verified")?false:"changed");
    assertNotEquals(service.shortfallBasisHash(first),service.shortfallBasisHash(second));
  }
  @ParameterizedTest @ValueSource(strings={"goal","definition","policy","document","canonical","clock","purpose"})
  void changedGoalAuthorityOrEvidenceVersionRemainsDifferent(String field) throws Exception {
    var first=snapshot(false);var second=snapshot(false);
    switch(field){
      case "goal"->second.put("goalId","new-goal");
      case "definition"->second.put("definitionHash","new-definition");
      case "policy"->map(second.get("policy")).put("contentHash","new-policy");
      case "document"->sources(second).put("evidence.DocumentVersions",List.of(Map.of("ID","document","sha256","new-hash")));
      case "canonical"->actual(second).put("revision",2);
      case "clock"->map(second.get("captureBoundary")).put("knownAt","2026-10-08T02:00:00Z");
      case "purpose"->map(second.get("captureBoundary")).put("admissionPurpose","OPEN_OBLIGATION");
    }
    assertNotEquals(service.shortfallBasisHash(first),service.shortfallBasisHash(second));
  }
  @ParameterizedTest @ValueSource(strings={"startQuantity","sourceGoalVersionId","quantity","unit","ID"})
  void childRangeIdentityAndBindingRemainPartOfProof(String field) throws Exception {
    var first=snapshot(true);child(first);var second=snapshot(true);child(second);
    credits(second).getFirst().put(field,"changed");
    assertNotEquals(service.shortfallBasisHash(first),service.shortfallBasisHash(second));
  }
  @Test void referencedCreditAndUnrelatedCanonicalAreNeverPruned() throws Exception {
    for(boolean referenced:List.of(true,false)){
      var first=snapshot(false);var second=snapshot(false);
      if(referenced){fact(first).put("evidenceRefs",List.of("credit30"));fact(second).put("evidenceRefs",List.of("credit30"));}
      else{credits(first).getFirst().put("occurrenceId","other");credits(second).getFirst().put("occurrenceId","other");}
      credits(second).getFirst().put("sourceGoalVersionId","changed");
      assertNotEquals(service.shortfallBasisHash(first),service.shortfallBasisHash(second));
    }
  }
}
