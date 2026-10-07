package org.mulino.verification;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.cucumber.junit.platform.engine.Constants.*;

/** Dynamic file selectors preserve true JUnit6 discovery without cucumber.features duplication. */
public final class CaseFeatureTest {
    @Test void executeEveryDeclaredKoreanScenario() throws Exception {
        Path root=Path.of(System.getProperty("repo.root")).toAbsolutePath().normalize();
        String files=System.getProperty("verification.caseFiles","");
        if(files.isBlank()) throw new IllegalArgumentException("Explicit case files required; failIfNoTests");
        var builder=LauncherDiscoveryRequestBuilder.request().filters(EngineFilter.includeEngines("cucumber"))
            .configurationParameter(GLUE_PROPERTY_NAME,"org.mulino.verification")
            .configurationParameter(PLUGIN_PROPERTY_NAME,"pretty,json:target/feature-red-cucumber.json");
        int expected=0;ContractValidator validator=new ContractValidator(root);
        for(String ref:files.split("\\|",-1)) {
            Path file=validator.path(ref);var c=validator.caseFile(file);expected+=c.path("subcases").size();
            builder.selectors(DiscoverySelectors.selectFile(file.resolveSibling("scenario.feature").toFile()));
        }
        var listener=new SummaryGeneratingListener();LauncherFactory.create().execute(builder.build(),listener);
        var summary=listener.getSummary();
        var r=Json.object();r.put("expectedScenarios",expected).put("discoveredScenarios",summary.getTestsFoundCount()).put("startedScenarios",summary.getTestsStartedCount()).put("skippedScenarios",summary.getTestsSkippedCount()).put("failedScenarios",summary.getTestsFailedCount());
        long unavailable=summary.getFailures().stream().filter(f->f.getException() instanceof AssertionError && f.getException().getMessage()!=null && f.getException().getMessage().contains("NOT_IMPLEMENTED")).count();
        r.put("notImplementedAssertionFailures",unavailable).put("status","FAIL").put("productCoverageClaimed",false);
        r.set("failureReasons",Json.MAPPER.valueToTree(summary.getFailures().stream().map(f->f.getException().toString()).toList()));
        Json.write(root.resolve("verification/harness/target/evidence/feature-red-summary.json"),r);
        assertEquals(expected,summary.getTestsFoundCount(),"Scenario discovery count");
        assertEquals(expected,summary.getTestsStartedCount(),"Scenario execution count");
        assertEquals(0,summary.getTestsSkippedCount(),"No skipped scenario allowed");
        assertEquals(expected,unavailable,"Each RED must be NOT_IMPLEMENTED assertion, not format/environment failure");
        throw new AssertionError("NOT_IMPLEMENTED: "+unavailable+" independently discovered Korean scenarios failed required product assertions");
    }
}
