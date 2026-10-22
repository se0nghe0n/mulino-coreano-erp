package org.mulino.verification.nativeclient;

import org.mulino.verification.*;
import org.mulino.verification.modelbinding.*;
import java.nio.file.Path;

/** Installed by default, but model execution remains explicitly disabled and fail-closed. */
public final class NativeModelBindingProvider implements ModelBindingRuntimeProvider {
    private final NativeClientIntegration integration=NativeCodexClient.discover();
    private final NativeClientPolicy policy=new NativeClientPolicy(Path.of(System.getProperty("repo.root",".")));
    public AcceptanceDriver driver(){return integration==null?new UnimplementedDriver():integration.driver();}
    public AgentRunner.ActualClientPort actualClient(){return new NativeCodexClient(policy,integration);}
    public UatExecutionGate executionGate(){return (caseId,turnId)->{
        NativeClientPolicy.require(Boolean.getBoolean("verification.nativeClient.enabled"),"NATIVE_CLIENT_DISABLED");
        NativeClientPolicy.require(integration!=null,"NATIVE_INTEGRATION_ABSENT");
        integration.requireReady(caseId,turnId);
        try{return policy.approval();}catch(java.io.IOException e){throw new IllegalStateException("R8_CONFIG_UNAVAILABLE");}
    };}
}
