package org.mulino.verification.modelbinding;
import org.mulino.verification.*;

/** Optional separately installed real runtime ports. Step2 ships no provider. */
public interface ModelBindingRuntimeProvider {
    AcceptanceDriver driver();
    AgentRunner.ActualClientPort actualClient();
    UatExecutionGate executionGate();
}
