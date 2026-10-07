import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.EngineFilter;
public class OutcomeEffectLauncher {
 public static void main(String[] args) {
  var request=LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass("org.mulino.verification.cases.sales.OutcomeEffectAssertionSelfTest")).filters(EngineFilter.includeEngines("junit-jupiter")).build();
  var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(request);
  var summary=listener.getSummary();summary.printTo(new java.io.PrintWriter(System.out,true));summary.printFailuresTo(new java.io.PrintWriter(System.out,true));
  if(summary.getTestsFoundCount()!=7 || summary.getTestsSucceededCount()!=7 || summary.getTestsFailedCount()!=0) System.exit(1);
 }
}
