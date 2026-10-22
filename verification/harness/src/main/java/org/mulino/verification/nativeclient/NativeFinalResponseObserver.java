package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import org.mulino.verification.*;
import java.nio.file.Path;
import java.util.List;

/** Lossless response custody. No NLP heuristics, string similarity or model self-labels become facts. */
final class NativeFinalResponseObserver {
    static StepResult observe(String action,JsonNode scope,Path capture,JsonNode nativeEvidence,NativeIntegrationEvidence store) throws Exception {
        JsonNode recorded=Json.read(capture);
        NativeClientPolicy.require(recorded.equals(nativeEvidence),"NATIVE_CAPTURE_EVIDENCE_MISMATCH");
        JsonNode response=recorded.path("result").path("finalResponse");
        NativeClientPolicy.require(response.isTextual(),"NATIVE_FINAL_RESPONSE_MISSING");
        String evidenceClass=recorded.path("evidenceClass").asText().equals("ACTUAL_NATIVE_CLIENT")
            ?"ACTUAL_NATIVE_RESPONSE_CAPTURE":"CAPTURED_CONTRACT_SELFTEST";
        var report=store.base(evidenceClass,scope).put("actionId",action)
            .put("status","NOT_IMPLEMENTED").put("reason","Independent complete final-response semantic extractor unavailable");
        report.set("sourceCapture",store.descriptor(capture));report.set("sourceResponse",response.deepCopy());
        var host=NativeHostObserver.extract(recorded);report.set("rawHostObservation",host);
        var parsed=NativeResponseSemantics.extract(response.asText());report.set("semanticExtraction",parsed);
        if(parsed.path("complete").asBoolean()) {
            var semantic=Json.object().put("evidenceClass",evidenceClass.equals("ACTUAL_NATIVE_RESPONSE_CAPTURE")&&host.path("complete").asBoolean()?"ACTUAL_HOST":"CAPTURED_CONTRACT_SELFTEST")
                .put("sourceTranscriptRef",store.descriptor(capture).path("path").asText()).put("complete",true);
            semantic.set("scope",scope.deepCopy());semantic.set("sourceResponse",response.deepCopy());semantic.set("claims",parsed.path("claims"));
            semantic.set("coverage",Json.array().add("CLAIMED_EFFECTS").add("COMPLETION").add("RESIDUAL_RESPONSIBILITY"));
            semantic.set("extractor",Json.object().put("independent",true).put("source","INDEPENDENT_FINAL_RESPONSE_SEMANTICS")
                .put("name","bounded-korean-clauses").put("version","1").set("command",Json.object().put("implementation","NativeResponseSemantics.extract")));
            // Still a diagnostic until actual extractor process custody/host contract is wired.
            report.set("semanticObservation",semantic);
        }
        report.put("reason","Host acceptance extractor command/custody not integrated; arbitrary text semantics remain UNKNOWN");
        report.set("implementationSource",store.source("verification/harness/src/main/java/org/mulino/verification/nativeclient/NativeFinalResponseObserver.java"));
        var artifact=store.save("final-response",report);
        StepResult missing=StepResult.missing(action,"NATIVE_FINAL_RESPONSE_SEMANTICS_NOT_IMPLEMENTED");
        return new StepResult(action,missing.driverStatus(),null,null,missing.reason(),missing.provenance(),
            List.of(store.descriptor(capture).path("path").asText(),artifact.path("path").asText()));
    }
}
