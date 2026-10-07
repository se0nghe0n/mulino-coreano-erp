package org.mulino.verification;
import org.junit.platform.suite.api.*;
import static io.cucumber.junit.platform.engine.Constants.*;
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/harness-smoke.feature")
@ConfigurationParameter(key=GLUE_PROPERTY_NAME,value="org.mulino.verification")
@ConfigurationParameter(key=FILTER_TAGS_PROPERTY_NAME,value="not @contract-red")
@ConfigurationParameter(key=PLUGIN_PROPERTY_NAME,value="pretty,json:target/cucumber.json")
public class HarnessTest {}
