package org.mulino.verification.modelbinding;
import org.mulino.verification.*;

/** Runtime ports; native subscription provider remains gated until product observation SPI is installed. */
public interface ModelBindingRuntimeProvider {
    AcceptanceDriver driver();
    AgentRunner.ActualClientPort actualClient();
    UatExecutionGate executionGate();
}
