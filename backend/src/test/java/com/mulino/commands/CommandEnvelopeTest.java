package com.mulino.commands;
import com.mulino.application.core.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class CommandEnvelopeTest {
 Map<String,Object> intent(){return new LinkedHashMap<>(Map.of("intentKind","COMMAND","definitionVersion","definition-v1","capabilityId","createWork","subjectRefs",List.of(),"slots",Map.of("quantity",Map.of("value","100","unit","BOX")),"provenance",Map.of("quantity","USER"),"commandIdempotencyKey","one"));}
 @Test void canonicalizationIgnoresTransportCollectionOrderAndCollectionRequest(){var a=intent();var b=new TreeMap<>(a);b.put("conversationRequestId","input-2");b.put("commandIdempotencyKey","two");assertEquals(CommandRequests.hash(a),CommandRequests.hash(b));b.put("slots",Map.of("quantity",Map.of("unit","BOX","value","101")));assertNotEquals(CommandRequests.hash(a),CommandRequests.hash(b));}
 @Test void actorRoleClockAndRuntimeClaimCannotBePayloadAuthority(){for(String field:List.of("actorId","organizationId","role","asOf","leaseToken","executionClaim")){var a=intent();a.put(field,"forged");assertThrows(DomainError.class,()->CommandRequests.parse(a,true));}}
 @Test void requestKindsAndFinalKeyRemainSeparate(){var a=intent();a.put("intentKind","QUERY");assertThrows(DomainError.class,()->CommandRequests.parse(a,true));var b=intent();b.remove("commandIdempotencyKey");assertDoesNotThrow(()->CommandRequests.parse(intent(),false));final var missing=b;assertThrows(DomainError.class,()->CommandRequests.parse(missing,true));}
 /** User decision 2026-10-11: a list slot is a published bounded array value; the product enforces the same bound as the schema. */
 @Test void listSlotsAreBoundedTypedValuesLikeTheIntentSchema() throws Exception {
  var schema=new com.fasterxml.jackson.databind.ObjectMapper().readTree(java.nio.file.Path.of("../contracts/intent.schema.json").toFile());
  var array=java.util.stream.StreamSupport.stream(schema.path("$defs").path("value").path("oneOf").spliterator(),false).filter(x->"array".equals(x.path("type").asText())).findFirst().orElseThrow();
  assertEquals(CommandRequests.MAX_LIST_SLOT_ITEMS,array.path("maxItems").asInt());assertEquals("#/$defs/value",array.path("items").path("$ref").asText());
  var ok=intent();ok.put("slots",Map.of("segmentIds",List.of("a","b"),"children",List.of(Map.of("value","1","unit","BOX"),List.of("x"))));assertDoesNotThrow(()->CommandRequests.parse(ok,true));
  var big=intent();big.put("slots",Map.of("segmentIds",Collections.nCopies(CommandRequests.MAX_LIST_SLOT_ITEMS+1,"a")));assertEquals("TYPE_INVALID",assertThrows(DomainError.class,()->CommandRequests.parse(big,true)).code());
  var edge=intent();edge.put("slots",Map.of("segmentIds",Collections.nCopies(CommandRequests.MAX_LIST_SLOT_ITEMS,"a")));assertDoesNotThrow(()->CommandRequests.parse(edge,true));
  var number=intent();number.put("slots",Map.of("quantities",List.of(1)));assertEquals("TYPE_INVALID",assertThrows(DomainError.class,()->CommandRequests.parse(number,true)).code());
  var nested=intent();nested.put("slots",Map.of("plan",List.of(Collections.nCopies(CommandRequests.MAX_LIST_SLOT_ITEMS+1,"a"))));assertThrows(DomainError.class,()->CommandRequests.parse(nested,true));
 }
}
