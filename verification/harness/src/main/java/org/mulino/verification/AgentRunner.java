package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;

public interface AgentRunner {
    StepResult run(String actionId, String route, JsonNode actor, JsonNode action, AcceptanceDriver driver);
    /** SIT sends only the declared typed intent to the actual tool/API. */
    final class Scripted implements AgentRunner {
        public StepResult run(String id,String route,JsonNode actor,JsonNode action,AcceptanceDriver driver) {
            JsonNode intent=action.get("intent");
            return driver.invoke(id,route,actor,Json.required(intent,"capabilityId"),intent);
        }
    }
    /** Actual host receives raw user input plus permitted context, never oracle or scripted intent. */
    interface ActualClientPort {
        StepResult execute(String actionId, String route, JsonNode actor, String userUtterance, JsonNode permittedContext);
    }
    final class Actual implements AgentRunner {
        private final ActualClientPort client;
        public Actual(ActualClientPort client) { this.client=client; }
        public StepResult run(String id,String route,JsonNode actor,JsonNode action,AcceptanceDriver ignored) {
            return client.execute(id,route,actor,Json.required(action,"userUtterance"),action.get("permittedContext"));
        }
    }
}
