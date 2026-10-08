package org.mulino.verification;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

public interface AgentRunner {
    StepResult run(String actionId, String route, JsonNode actor, JsonNode action, AcceptanceDriver driver);
    /** Evidence label of the runner that executed an agent action. */
    String kind();
    /** Adapters this runner itself supplies (a real client/model host), never product adapters. */
    default Set<String> providedAdapters() { return Set.of(); }
    /** True only when an actual client/model host receives the raw utterance. */
    default boolean actualClient() { return false; }

    /** SIT sends only the declared typed intent to the actual tool/API. */
    final class Scripted implements AgentRunner {
        public String kind() { return "SCRIPTED_SIT"; }
        public StepResult run(String id,String route,JsonNode actor,JsonNode action,AcceptanceDriver driver) {
            JsonNode intent=action.path("intent");
            // A UAT-only action carries no planned typed intent. The scripted runner never invents one.
            if(!intent.path("capabilityId").isTextual() || intent.path("capabilityId").asText().isBlank())
                return StepResult.missing(id,"NOT_IMPLEMENTED: scripted SIT runner has no declared typed intent; UAT-only agent action requires the actual client runner");
            return driver.invoke(id,route,actor,intent.path("capabilityId").asText(),intent);
        }
    }
    /** Actual host receives raw user input plus permitted context, never oracle or scripted intent. */
    interface ActualClientPort {
        StepResult execute(String actionId, String route, JsonNode actor, String userUtterance, JsonNode permittedContext);
    }
    final class Actual implements AgentRunner {
        private final ActualClientPort client;
        public Actual(ActualClientPort client) { this.client=Objects.requireNonNull(client); }
        public String kind() { return "ACTUAL_UAT"; }
        public Set<String> providedAdapters() { return Set.of("client","model","skills"); }
        public boolean actualClient() { return true; }
        public StepResult run(String id,String route,JsonNode actor,JsonNode action,AcceptanceDriver ignored) {
            return client.execute(id,route,actor,Json.required(action,"userUtterance"),action.get("permittedContext"));
        }
    }
    /** Explicit selection: scripted by default; actual only through exactly one installed client port. */
    static AgentRunner fromProperty(String selection) {
        return switch(selection==null ? "scripted" : selection) {
            case "scripted" -> new Scripted();
            case "actual" -> {
                List<ActualClientPort> ports=new ArrayList<>();ServiceLoader.load(ActualClientPort.class).forEach(ports::add);
                if(ports.size()!=1) throw new IllegalArgumentException("verification.agentRunner=actual requires exactly one installed AgentRunner.ActualClientPort; found "+ports.size());
                yield new Actual(ports.get(0));
            }
            default -> throw new IllegalArgumentException("Unknown verification.agentRunner "+selection);
        };
    }
}
