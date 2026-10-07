package org.mulino.verification;
import org.junit.platform.suite.api.*;
import static io.cucumber.junit.platform.engine.Constants.*;
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("examples/HARNESS-EXAMPLE/scenario.feature")
@ConfigurationParameter(key=GLUE_PROPERTY_NAME,value="org.mulino.verification")
@ConfigurationParameter(key=FILTER_TAGS_PROPERTY_NAME,value="@contract-red")
@ConfigurationParameter(key=PLUGIN_PROPERTY_NAME,value="pretty,json:target/contract-red-cucumber.json")
public class ContractRedTest {}
