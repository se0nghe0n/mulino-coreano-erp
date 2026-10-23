package com.mulino.application.trade.purchase;

import com.mulino.application.core.DomainError;
import java.util.Map;

/** Published purchase approval consumption semantics; old missing values fail closed. */
final class PurchaseApprovalPolicy {
    static final String SINGLE_ORDER_REVISION = "SINGLE_ORDER_REVISION";

    private PurchaseApprovalPolicy() {}

    static String fromInput(Object value) {
        if (!SINGLE_ORDER_REVISION.equals(value)) {
            throw DomainError.invalid("Unsupported purchase approval consumption policy");
        }
        return SINGLE_ORDER_REVISION;
    }

    static void requireDispatchable(Map<String, Object> approval) {
        if (!SINGLE_ORDER_REVISION.equals(approval.get("consumptionPolicy"))) {
            throw new DomainError("HELD", "APPROVAL_CONSUMPTION_POLICY_UNSUPPORTED",
                "Approval requires a new decision with a supported consumption policy");
        }
        // The purchase fence serializes dispatch; Orders has unique constraints on
        // both (organizationId, approvalId) and (organizationId, proposalId, proposalRevision).
        // Its immutable row is the consumption record, committed with the outbox.
    }
}
