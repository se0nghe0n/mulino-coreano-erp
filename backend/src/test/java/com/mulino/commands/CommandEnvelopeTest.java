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
}
