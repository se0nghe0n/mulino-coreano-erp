package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public final class ReferenceResolverTest {
    private final JsonNode aliases=Json.parse("{\"LOT\":\"server-lot-1\",\"qc\":\"server-human-1\"}");
    private Map<String,JsonNode> results() {
        return new HashMap<>(Map.of("created",Json.parse("{\"driverStatus\":\"EXECUTED\",\"provenance\":{\"scopeComplete\":true},\"response\":{\"workId\":\"generated-W1\",\"proposalHash\":\"sha1\",\"quantity\":\"100\",\"state\":\"STATE-A\",\"jsonState\":{\"boundWorkId\":\"generated-W1\",\"inputResponses\":[1,2]}},\"data\":{\"rows\":[{\"lotId\":\"server-lot-1\",\"workId\":\"generated-W1\",\"ownerId\":\"server-human-1\"},{\"lotId\":\"other-lot\",\"workId\":\"other-work\",\"ownerId\":\"other-human\"}]}}")));
    }
    @Test void dynamicIdentityBindsWhereExpectedAndScopeWithoutChangingDeclaration() {
        var r=results();var a=Json.parse("{\"op\":\"exactSet\",\"source\":{\"actionId\":\"created\",\"pointer\":\"/data/rows\",\"where\":{\"lotId\":{\"$alias\":\"LOT\"}},\"field\":\"workId\"},\"expected\":[{\"$result\":{\"actionId\":\"created\",\"pointer\":\"/response/workId\"}}],\"scope\":{\"workId\":{\"$result\":{\"actionId\":\"created\",\"pointer\":\"/response/workId\"}}}} ");
        new AssertionEngine().check(a,r,aliases);assertTrue(a.path("expected").get(0).has("$result"));
        ((ObjectNode)r.get("created").path("data").path("rows").get(0)).put("workId","wrong-work");assertThrows(AssertionError.class,()->new AssertionEngine().check(a,r,aliases));
    }
    @Test void baselineWhereAndOwnerIdentityAreBoundToRealIds() {
        var r=results();var a=Json.parse("{\"op\":\"sameAs\",\"source\":{\"actionId\":\"created\",\"pointer\":\"/data/rows\",\"where\":{\"ownerId\":{\"$alias\":\"qc\"}}},\"baseline\":{\"actionId\":\"created\",\"pointer\":\"/data/rows\",\"where\":{\"workId\":{\"$result\":{\"actionId\":\"created\",\"pointer\":\"/response/workId\"}}}},\"expected\":true}");
        new AssertionEngine().check(a,r,aliases);
        ((ObjectNode)r.get("created").path("data").path("rows").get(0)).put("ownerId","wrong-human");assertThrows(AssertionError.class,()->new AssertionEngine().check(a,r,aliases));
    }
    @Test void quantityCannotBeCopiedFromActualResultIntoExpectedOracle() {
        var a=Json.parse("{\"op\":\"equals\",\"source\":{\"actionId\":\"created\",\"pointer\":\"/response/quantity\"},\"expected\":{\"$result\":{\"actionId\":\"created\",\"pointer\":\"/response/quantity\"}}}");
        assertThrows(IllegalArgumentException.class,()->new AssertionEngine().check(a,results(),aliases));
    }
    @Test void missingAliasOrUnavailableResultNeverCoercesToNullOrZero() {
        var r=results();var resolver=new ReferenceResolver(r,aliases);
        assertThrows(IllegalArgumentException.class,()->resolver.identity(Json.parse("{\"$alias\":\"MISSING\"}")));
        r.put("created",StepResult.missing("created","NOT_IMPLEMENTED").toJson());
        assertThrows(AssertionError.class,()->resolver.resolve(Json.parse("{\"$result\":{\"actionId\":\"created\",\"pointer\":\"/response/workId\"}}")));
    }
    @Test void opaqueMutationChangesOneByteOfIssuedStateAndPreservesOriginal() {
        var r=results();var resolver=new ReferenceResolver(r,aliases);
        var t=Json.parse("{\"$transform\":{\"source\":{\"$result\":{\"actionId\":\"created\",\"pointer\":\"/response/state\"}},\"operation\":\"opaqueByteXor\",\"index\":0,\"xor\":1}}");
        assertEquals("RTATE-A",resolver.resolve(t).asText());assertEquals("STATE-A",r.get("created").path("response").path("state").asText());
        ((ObjectNode)t.path("$transform")).put("index",7);assertThrows(IllegalArgumentException.class,()->resolver.resolve(t));
        ((ObjectNode)t.path("$transform")).put("index",0).put("xor",0);assertThrows(IllegalArgumentException.class,()->resolver.resolve(t));
    }
    @Test void jsonReplaceRemoveUseActualStateAndPreserveOtherFields() {
        var resolver=new ReferenceResolver(results(),aliases);
        var base="\"source\":{\"$result\":{\"actionId\":\"created\",\"pointer\":\"/response/jsonState\"}}";
        var replace=Json.parse("{\"$transform\":{"+base+",\"operation\":\"jsonPointerReplace\",\"pointer\":\"/boundWorkId\",\"value\":\"wrong-work\"}}");
        var changed=resolver.resolve(replace);assertEquals("wrong-work",changed.path("boundWorkId").asText());assertEquals(Json.parse("[1,2]"),changed.path("inputResponses"));
        var remove=Json.parse("{\"$transform\":{"+base+",\"operation\":\"jsonPointerRemove\",\"pointer\":\"/inputResponses/0\"}}");assertEquals(Json.parse("[2]"),resolver.resolve(remove).path("inputResponses"));
        ((ObjectNode)remove.path("$transform")).put("pointer","/unknown");assertThrows(IllegalArgumentException.class,()->resolver.resolve(remove));
    }
    @Test void transformRejectsFakeSourceUnknownOperationExtraFieldsAndUnavailableInput() {
        var resolver=new ReferenceResolver(results(),aliases);
        for(String t:List.of("{\"$transform\":{\"source\":\"FAKE-STATE\",\"operation\":\"opaqueByteXor\",\"index\":0,\"xor\":1}}","{\"$transform\":{\"source\":{\"$result\":{\"actionId\":\"created\",\"pointer\":\"/response/state\"}},\"operation\":\"eval\",\"code\":\"SQL\"}}","{\"$transform\":{\"source\":{\"$result\":{\"actionId\":\"created\",\"pointer\":\"/response/state\"}},\"operation\":\"opaqueByteXor\",\"index\":0,\"xor\":1,\"extra\":true}}"))
            assertThrows(IllegalArgumentException.class,()->resolver.resolve(Json.parse(t)));
        var r=results();r.put("created",StepResult.missing("created","NOT_IMPLEMENTED").toJson());
        assertThrows(AssertionError.class,()->new ReferenceResolver(r,aliases).resolve(Json.parse("{\"$transform\":{\"source\":{\"$result\":{\"actionId\":\"created\",\"pointer\":\"/response/state\"}},\"operation\":\"opaqueByteXor\",\"index\":0,\"xor\":1}}")));
    }
}
