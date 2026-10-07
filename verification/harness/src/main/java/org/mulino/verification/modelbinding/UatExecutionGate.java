package org.mulino.verification.modelbinding;
import com.fasterxml.jackson.databind.JsonNode;

/** Supplied only by a separately authorized runtime. A bound client alone is never cost approval. */
@FunctionalInterface
public interface UatExecutionGate {
    /** Return immutable actual approval/config evidence, or throw before any paid invocation. */
    JsonNode requireAuthorized(String caseId,String turnId);
    static UatExecutionGate notAuthorized(){return (c,t)->{throw new IllegalStateException("R8 actual model/client/config and cost authorization absent");};}
}
