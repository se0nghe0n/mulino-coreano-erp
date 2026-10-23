package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mulino.verification.Json;
import java.nio.file.*;
import java.util.*;

/** A read-only process entrypoint. Its only input is one hash-bound, complete native wire capture. */
public final class NativeHostExtractionMain {
    static final long MAX_CAPTURE_BYTES=16L*1024*1024;
    public static void main(String[] args) {
        try {
            NativeClientPolicy.require(args.length==3,"HOST_EXTRACTION_ARGUMENTS_INVALID");
            Path root=Path.of(args[0]).toRealPath(),relative=Path.of(args[1]);
            NativeClientPolicy.require(!relative.isAbsolute(),"HOST_EXTRACTION_PATH_ESCAPE");
            Path capture=root.resolve(relative).toRealPath();
            NativeClientPolicy.require(capture.startsWith(root)&&Files.isRegularFile(capture),"HOST_EXTRACTION_PATH_ESCAPE");
            NativeClientPolicy.require(Files.size(capture)<=MAX_CAPTURE_BYTES,"HOST_EXTRACTION_INPUT_TOO_LARGE");
            NativeClientPolicy.require(args[2].matches("[a-f0-9]{64}")&&Json.sha256(capture).equals(args[2]),"HOST_EXTRACTION_INPUT_CHANGED");
            var raw=Json.read(capture);NativeClientPolicy.rejectSecrets(raw);
            ObjectNode result=Json.object().put("recordType","NATIVE_HOST_EXTRACTION_V1")
                .put("sourceCaptureRef",root.relativize(capture).toString()).put("sourceCaptureSha256",args[2]);
            result.set("host",NativeHostObserver.extract(raw));
            NativeClientPolicy.require(raw.path("result").path("finalResponse").isTextual(),"NATIVE_FINAL_RESPONSE_MISSING");
            result.set("semantics",NativeResponseSemantics.extract(raw.path("result").path("finalResponse").asText()));
            result.set("implementationClasses",implementationClasses());
            // Detect replacement during parsing/extraction as well as before it.
            NativeClientPolicy.require(Json.sha256(capture).equals(args[2]),"HOST_EXTRACTION_INPUT_CHANGED");
            System.out.println(result);
        } catch(Exception invalid) {
            String code=invalid.getMessage();
            if(code==null||!code.matches("[A-Z0-9_]{1,100}"))code="HOST_EXTRACTION_INVALID_INPUT";
            System.err.println(code);System.exit(2);
        }
    }
    static com.fasterxml.jackson.databind.node.ArrayNode implementationClasses() throws Exception {
        var implementation=Json.array();
        for(Class<?> type:List.of(NativeHostExtractionMain.class,NativeHostObserver.class,NativeResponseSemantics.class,NativeClientPolicy.class,Json.class)) {
            String resource="/"+type.getName().replace('.','/')+".class";
            try(var stream=type.getResourceAsStream(resource)) {
                NativeClientPolicy.require(stream!=null,"HOST_EXTRACTOR_IMPLEMENTATION_MISSING");
                String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes()));
                implementation.add(Json.object().put("class",type.getName()).put("sha256",hash));
            }
        }
        return implementation;
    }
    private NativeHostExtractionMain() {}
}
