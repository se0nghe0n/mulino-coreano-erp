package org.mulino.verification;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Read-only case-asset checks that preparation must pass in addition to the Java linkage checks.
 * Each one is the single owner-maintained source of its rule, so preparation calls it instead of
 * re-implementing it: the normative catalog/lock validator (the same one the coverage assembler loads),
 * the observation-level MCP/SKILLS layer route review (also loaded by the assembler), the cases-b review
 * invariants, the derived observation-binding drift checks (T08, V4, V6, V7 content and every bindings
 * file's hash stamps) and the generator reproduction tests (T01/T20/T25/C3/T26 and V4/T06/T22/T24/T23/V8).
 * None of them writes the repository: generators run in temporary copies. A missing interpreter or script
 * fails closed. Output lines starting with KNOWN_OPEN are recorded gaps with a named owner; they are
 * copied into the record and the preparation report instead of being hidden in the output tail.
 */
final class PreparationAssetChecks {
    record Check(String name,String script,List<String> argv) {}
    static final String BIND_VX="verification/cases/V7/bind_observations.py";
    static final List<Check> CHECKS=List.of(
        new Check("normative-catalog-lock","verification/requirements/validate_catalog.py",List.of("verification/requirements/validate_catalog.py")),
        new Check("layer-routes","verification/requirements/check_layer_routes.py",List.of("verification/requirements/check_layer_routes.py")),
        new Check("cases-b-invariants","verification/cases/V2/cases_b_invariants.py",List.of("verification/cases/V2/cases_b_invariants.py")),
        new Check("t08-observation-bindings","verification/cases/T08/bind_observations.py",List.of("verification/cases/T08/bind_observations.py","--check")),
        new Check("v4-observation-bindings",BIND_VX,List.of(BIND_VX,"V4","--check")),
        new Check("v6-observation-bindings",BIND_VX,List.of(BIND_VX,"V6","--check")),
        new Check("v7-observation-bindings",BIND_VX,List.of(BIND_VX,"V7","--check")),
        new Check("observation-binding-stamps","verification/requirements/check_derived_bindings.py",List.of("verification/requirements/check_derived_bindings.py")),
        new Check("generators-reproduce","verification/mcp-tests/test_generators_reproduce.py",
            List.of("-m","unittest","discover","-s","verification/mcp-tests","-p","test_generators_reproduce.py")),
        new Check("case-generators-reproduce","verification/requirements/test_case_generators_reproduce.py",
            List.of("-m","unittest","discover","-s","verification/requirements","-p","test_case_generators_reproduce.py")));
    private PreparationAssetChecks() {}

    static ArrayNode run(Path root,List<String> problems) {
        ArrayNode records=Json.array();
        String python=System.getProperty("verification.python","python3");
        for(Check check:CHECKS) {
            ObjectNode record=Json.object();record.put("name",check.name()).put("script",check.script());
            List<String> argv=new ArrayList<>(List.of(python,"-I"));argv.addAll(check.argv());
            record.set("argv",Json.MAPPER.valueToTree(argv));
            Path script=root.resolve(check.script());
            if(!Files.isRegularFile(script)) {
                record.put("status","FAIL").putNull("exitCode");problems.add("Case asset check script missing: "+check.name()+" "+check.script());records.add(record);continue;
            }
            try {
                record.put("scriptSha256",Json.sha256(script));
                Path output=Files.createTempFile("preparation-asset-check",".txt");
                try {
                    Process process=new ProcessBuilder(argv).directory(root.toFile()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
                    if(!process.waitFor(600,TimeUnit.SECONDS)) {process.destroyForcibly();throw new IOException("timed out after 600s");}
                    int exit=process.exitValue();
                    List<String> lines=Files.readAllLines(output,StandardCharsets.UTF_8);
                    record.put("exitCode",exit).put("status",exit==0?"PASS":"FAIL");
                    ArrayNode knownOpen=Json.array();
                    for(String line:lines) if(line.startsWith("KNOWN_OPEN")) knownOpen.add(line);
                    record.set("knownOpen",knownOpen);
                    record.set("outputTail",Json.MAPPER.valueToTree(lines.subList(Math.max(0,lines.size()-12),lines.size())));
                    if(exit!=0) problems.add("Case asset check failed: "+check.name()+" exit="+exit+": "+(lines.isEmpty()?"":lines.get(lines.size()-1)));
                } finally {Files.deleteIfExists(output);}
            } catch(IOException e) {
                record.put("status","FAIL").putNull("exitCode").put("error",e.getMessage());problems.add("Case asset check could not run: "+check.name()+": "+e.getMessage());
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();record.put("status","FAIL").putNull("exitCode");problems.add("Case asset check interrupted: "+check.name());
            }
            records.add(record);
        }
        return records;
    }
}
