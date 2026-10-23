package org.mulino.verification.nativeclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mulino.verification.Json;
import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Launches the independent extractor without a shell, network credentials, or product/database arguments. */
final class NativeHostExtractionProcess {
    record Result(JsonNode observation,ObjectNode custody,List<String> artifactRefs) {}
    static Result extract(Path capture,NativeIntegrationEvidence store) throws Exception {
        var source=store.descriptor(capture);Path root=store.root.toRealPath();
        NativeClientPolicy.require(Files.size(capture)<=NativeHostExtractionMain.MAX_CAPTURE_BYTES,"HOST_EXTRACTION_INPUT_TOO_LARGE");
        Path parent=root.resolve("verification/harness/target");Files.createDirectories(parent);
        Path directory=Files.createTempDirectory(parent,"host-extraction-",PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path stdout=privateFile(directory.resolve("stdout.json")),stderr=privateFile(directory.resolve("stderr.txt"));
        String classpath=classpath();Path javaBinary=Path.of(System.getProperty("java.home"),"bin","java").toRealPath();
        var argv=List.of(javaBinary.toString(),"-cp",classpath,NativeHostExtractionMain.class.getName(),root.toString(),
            Json.required(source,"path"),Json.required(source,"sha256"));
        var processBuilder=new ProcessBuilder(argv).directory(root.toFile()).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
        processBuilder.environment().clear(); // In particular no inherited JAVA_TOOL_OPTIONS, API keys, or DB credentials.
        var implementation=NativeHostExtractionMain.implementationClasses();
        Instant started=Instant.now();Process process=processBuilder.start();
        boolean finished=false;
        try {finished=process.waitFor(15,TimeUnit.SECONDS);}
        finally {if(!finished){process.destroyForcibly();process.waitFor(5,TimeUnit.SECONDS);}}
        Instant completed=Instant.now();
        var command=Json.object().put("processId",process.pid()).put("startedAt",started.toString()).put("completedAt",completed.toString())
            .put("timedOut",!finished).put("exitCode",finished?process.exitValue():124).put("cwd",root.toString())
            .put("environmentPolicy","EMPTY_NO_PRODUCT_CREDENTIALS").put("javaBinarySha256",Json.sha256(javaBinary));
        command.set("argv",Json.MAPPER.valueToTree(argv));command.set("inputCapture",source);
        command.set("stdout",store.descriptor(stdout));command.set("stderr",store.descriptor(stderr));
        command.set("implementationClasses",implementation);
        var custody=store.base("INDEPENDENT_NATIVE_HOST_EXTRACTION_PROCESS",Json.object());custody.set("command",command);
        var receipt=store.save("host-extraction-process",custody);var refs=List.of(source.path("path").asText(),
            command.path("stdout").path("path").asText(),command.path("stderr").path("path").asText(),receipt.path("path").asText());
        NativeClientPolicy.require(finished&&process.exitValue()==0,"HOST_EXTRACTION_PROCESS_FAILED");
        NativeClientPolicy.require(Files.size(stdout)<=NativeHostExtractionMain.MAX_CAPTURE_BYTES,"HOST_EXTRACTION_OUTPUT_TOO_LARGE");
        JsonNode result=Json.read(stdout);NativeClientPolicy.rejectSecrets(result);
        NativeClientPolicy.require(result.path("recordType").asText().equals("NATIVE_HOST_EXTRACTION_V1")
            &&result.path("sourceCaptureRef").equals(source.path("path"))&&result.path("sourceCaptureSha256").equals(source.path("sha256"))
            &&Json.sha256(capture).equals(source.path("sha256").asText()),"HOST_EXTRACTION_INPUT_CHANGED");
        NativeClientPolicy.require(implementation.equals(result.path("implementationClasses"))
            &&implementation.equals(NativeHostExtractionMain.implementationClasses()),"HOST_EXTRACTOR_IMPLEMENTATION_CHANGED");
        return new Result(result,custody,refs);
    }
    private static Path privateFile(Path path) throws Exception {
        return Files.createFile(path,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
    }
    private static String classpath() {
        String original=System.getProperty("surefire.test.class.path",System.getProperty("java.class.path"));
        return String.join(File.pathSeparator,Arrays.stream(original.split(java.util.regex.Pattern.quote(File.pathSeparator),-1))
            .map(x->Path.of(x.isEmpty()?".":x).toAbsolutePath().normalize().toString()).toList());
    }
    private NativeHostExtractionProcess() {}
}
