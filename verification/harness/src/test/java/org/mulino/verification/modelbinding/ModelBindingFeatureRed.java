package org.mulino.verification.modelbinding;
import org.mulino.verification.*;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.cucumber.junit.platform.engine.Constants.*;

/** Normal suite does not run this deliberately failing entry point unless explicitly requested. */
public final class ModelBindingFeatureRed {
    @Test void executeAllSixtyRealKoreanGherkinScenarios() throws Exception {
        var c=new BindingContract(Path.of(System.getProperty("repo.root")));c.prepare();
        var builder=LauncherDiscoveryRequestBuilder.request().filters(EngineFilter.includeEngines("cucumber")).configurationParameter(GLUE_PROPERTY_NAME,"org.mulino.verification.modelbinding").configurationParameter(PLUGIN_PROPERTY_NAME,"json:target/model-binding-cucumber-red.json");
        for(var e:c.registry.path("cases"))builder.selectors(DiscoverySelectors.selectFile(c.validator.path(c.binding(e.path("caseId").asText()).path("featureRef").asText()).toFile()));
        var listener=new SummaryGeneratingListener();LauncherFactory.create().execute(builder.build(),listener);var s=listener.getSummary();long missing=s.getFailures().stream().filter(f->f.getException() instanceof AssertionError&&f.getException().getMessage().contains("NOT_IMPLEMENTED")).count();
        var r=c.report("FAIL","NOT_RUN");r.put("preparationStatus","PASS").put("profile",System.getProperty("model.binding.profile","SIT")).put("command",System.getProperty("verification.command")).put("exitCode",1).put("discoveredScenarios",s.getTestsFoundCount()).put("startedScenarios",s.getTestsStartedCount()).put("skippedScenarios",s.getTestsSkippedCount()).put("failedScenarios",s.getTestsFailedCount()).put("notImplementedAssertionFailures",missing);r.set("failureReasons",Json.MAPPER.valueToTree(s.getFailures().stream().map(f->f.getException().toString()).toList()));Json.write(c.validator.path("verification/harness/target/evidence/model-binding-gherkin-red"+(System.getProperty("model.binding.profile","SIT").equals("SIT")?"":"-uat")+".json"),r);
        assertEquals(60,s.getTestsFoundCount());assertEquals(60,s.getTestsStartedCount());assertEquals(0,s.getTestsSkippedCount());assertEquals(60,missing,"Every RED is product absence, not format/environment failure");throw new AssertionError("NOT_IMPLEMENTED: 60 substantive scenarios RED; actual model calls 0; product runtime NOT_RUN");
    }
}
