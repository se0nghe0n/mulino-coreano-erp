package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import org.mulino.verification.Json;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Reads current saved evidence; PREPARED and a self-authored PASS flag never authorize a model. */
final class NativeDeterministicGate {
    static final List<String> REQUIRED=List.of("schema","contracts","scenarios","recovery","mcp","skills");
    private final Path root;
    NativeDeterministicGate(Path root) {this.root=root.toAbsolutePath().normalize();}
    void requireReady() {
        Path file=root.resolve("verification/harness/target/evidence/runtime-manifest.json");
        try {
            NativeClientPolicy.require(Files.isRegularFile(file),"DETERMINISTIC_EVIDENCE_MISSING");
            JsonNode report=Json.read(file);
            requirePassingProfiles(report);
            NativeClientPolicy.require(!report.path("workingTreeDirty").asBoolean(true),"DETERMINISTIC_EVIDENCE_DIRTY");
            var git=new ProcessBuilder("git","rev-parse","HEAD").directory(root.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            NativeClientPolicy.require(git.waitFor(5,TimeUnit.SECONDS)&&git.exitValue()==0,"BASELINE_UNAVAILABLE");
            String head=new String(git.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim();
            NativeClientPolicy.require(head.equals(report.path("codeCommit").asText()),"DETERMINISTIC_EVIDENCE_STALE");
            // Existing validator independently reassembles source hashes, receipts and statuses.
            var check=new ProcessBuilder("python3","verification/coverage/validate.py",file.toString(),"--root",root.toString())
                .directory(root.toFile()).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if(!check.waitFor(60,TimeUnit.SECONDS)){check.destroyForcibly();throw new IllegalStateException("DETERMINISTIC_VALIDATION_TIMEOUT");}
            NativeClientPolicy.require(check.exitValue()==0,"DETERMINISTIC_EVIDENCE_INVALID");
        } catch(IOException e) {throw new IllegalStateException("DETERMINISTIC_EVIDENCE_UNAVAILABLE");}
        catch(InterruptedException e) {Thread.currentThread().interrupt();throw new IllegalStateException("DETERMINISTIC_VALIDATION_INTERRUPTED");}
    }
    static void requirePassingProfiles(JsonNode report) {
        NativeClientPolicy.require(report.path("recordType").asText().equals("ACCEPTANCE_HARNESS_REPORT")&&
            report.path("preparationStatus").asText().equals("PREPARED"),"DETERMINISTIC_EVIDENCE_INVALID");
        for(String name:REQUIRED) {
            List<JsonNode> matches=new ArrayList<>();
            for(JsonNode profile:report.path("profiles"))if(profile.path("profile").asText().equals(name))matches.add(profile);
            NativeClientPolicy.require(matches.size()==1&&matches.getFirst().path("status").asText().equals("PASS"),"DETERMINISTIC_PREREQUISITE_"+name.toUpperCase(Locale.ROOT)+"_NOT_PASS");
        }
        for(JsonNode problem:report.path("coverageProblems"))NativeClientPolicy.require(!problem.path("status").asText().equals("FAIL"),"DETERMINISTIC_COVERAGE_FAILED");
    }
}
