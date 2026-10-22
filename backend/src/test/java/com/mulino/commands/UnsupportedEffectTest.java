package com.mulino.commands;

import com.mulino.application.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnsupportedEffectTest {
  private Map<String,Object> intent(Object effect) {
    return Map.of("intentKind","COMMAND","definitionVersion","definition-v1",
        "capabilityId","structureIntent","subjectRefs",List.of(),
        "slots",Map.of("requestedEffect",effect),"provenance",Map.of("requestedEffect","USER"),
        "commandIdempotencyKey","excluded-test");
  }
  @Test void sixExcludedCategoriesRetainSafeRequestMeaningAndNoEffects() {
    for(String effect:List.of("manufacture","BOM","B2C","generalLedger","taxSubmit","bankTransfer")) {
      var parsed=CommandRequests.parse(intent(effect),true);
      var response=DomainError.unsupportedIntent(parsed).response();
      assertEquals("HELD",response.get("outcome"));
      assertEquals(Map.of(),response.get("effects"));
      var error=(Map<?,?>)response.get("error");
      assertEquals("VERSION_UNSUPPORTED",error.get("code"));
      assertEquals(effect,error.get("requestedEffect"));
    }
  }
  @Test void arbitraryTextObjectsAndOtherCapabilitiesAreNotReflected() {
    for(Object value:List.of("private-subject-id",Map.of("secret","not-returned"),List.of("BOM"))) {
      assertFalse(((Map<?,?>)DomainError.unsupportedIntent(CommandRequests.parse(intent(value),true))
          .response().get("error")).containsKey("requestedEffect"));
    }
    var other=new HashMap<>(intent("BOM"));other.put("capabilityId","unrelated");
    assertFalse(((Map<?,?>)DomainError.unsupportedIntent(other).response().get("error")).containsKey("requestedEffect"));
  }
  @Test void malformedEnvelopeStillFailsBeforeUnsupportedEffectClassification() {
    var missing=new HashMap<>(intent("BOM"));missing.remove("commandIdempotencyKey");
    assertEquals("TYPE_INVALID",assertThrows(DomainError.class,()->CommandRequests.parse(missing,true)).code());
    var forged=new HashMap<>(intent("BOM"));forged.put("actorId","forged");
    assertThrows(DomainError.class,()->CommandRequests.parse(forged,true));
  }
}
