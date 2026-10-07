package org.mulino.verification.actual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;

/** Native synthetic acceptance authority is current; physical fact time remains fixed. */
public final class NativeFixtureAuthority {
    public static ObjectNode current(JsonNode template,Instant wallClock) {
        ObjectNode fixture=template.deepCopy();
        for(JsonNode actor:fixture.path("actors")) {
            ObjectNode grant=(ObjectNode)actor.path("grant");
            grant.put("validFrom",wallClock.minusSeconds(60).toString());
            grant.put("validUntil",wallClock.plusSeconds(900).toString());
        }
        return fixture;
    }
    private NativeFixtureAuthority() {}
}
