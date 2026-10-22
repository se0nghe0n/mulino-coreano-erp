package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import org.mulino.verification.*;
import java.nio.file.Path;

/** Product-owned boundary. No implementation is fabricated by the native client runner. */
public interface NativeClientIntegration {
    AcceptanceDriver driver();
    /** Must validate current deterministic evidence, authenticated context capture and installation scope.
     * Throw before the model call if any API/DB/host/semantic extractor is unavailable. */
    void requireReady(String caseId, String turnId);
    /** Open only from authenticated transport evidence, never actor claims or a case oracle. */
    Session open(String actionId, String route, JsonNode actor, String utterance, JsonNode permittedContext);

    interface Session extends AutoCloseable {
        /** Recheck deterministic prerequisites and authenticated input provenance for this session.
         * Direct ActualClientPort execution must be fenced just like the corpus execution gate. */
        void requireReady();
        /** Product tools with public input schemas, not case-specific planned calls. */
        JsonNode tools();
        /** Actual authenticated transport. Return Codex DynamicToolCallResponse contentItems/success.
         * Persist independent raw wire with hash, timestamps, authenticated principal and scope. */
        JsonNode call(JsonNode nativeRequest);
        /** Receives raw final model text and native capture. Must produce the existing host,
         * wire, snapshot and final-response-observation contracts using independent extractors.
         * The model's self labels are never evidence. Missing observations must return NOT_IMPLEMENTED. */
        StepResult observe(Path capture, JsonNode nativeEvidence);
        @Override default void close() {}
    }
}
