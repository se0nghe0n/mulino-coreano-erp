package org.mulino.verification.actual;

/** An observed violation takes precedence over unavailable actions in a partial attempt. */
public final class ActualAttemptStatus {
    public static String classify(boolean violation,boolean unavailable,boolean environmentFailure) {
        if(violation)return "FAIL";
        if(environmentFailure)return "ENVIRONMENT_OR_CONTRACT_FAILURE";
        return unavailable?"NOT_RUN":"PASS";
    }
    private ActualAttemptStatus() {}
}
