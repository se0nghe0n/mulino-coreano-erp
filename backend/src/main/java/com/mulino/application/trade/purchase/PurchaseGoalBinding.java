package com.mulino.application.trade.purchase;

import com.mulino.application.core.*;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.trade.purchase.PurchaseHash;
import com.mulino.domain.trade.purchase.PurchaseRepository;
import java.util.Map;
import java.util.Objects;

/** Pins the server's current immutable Work goal, without reconstructing missing history. */
final class PurchaseGoalBinding {
  private final PurchaseRepository repository;
  private final WorkAccess works;

  PurchaseGoalBinding(PurchaseRepository repository, WorkAccess works) {
    this.repository = repository;
    this.works = works;
  }

  String current(DomainContext context, String workId) {
    // The command pipeline also holds work:<id>, shared with reviseGoal.
    var work = works.require(context, workId, true);
    if (work.get("currentGoalVersionId") == null) throw unresolved();
    var goal = works.currentGoal(context, workId);
    if (!Objects.equals(context.organizationId(), goal.get("organizationId"))
        || !Objects.equals(workId, goal.get("workId"))
        || !Objects.equals(work.get("currentGoalVersionId"), goal.get("ID"))
        || !Objects.equals(work.get("definitionVersionId"), goal.get("definitionVersionId"))) {
      throw DomainError.forbidden();
    }
    return goal.get("ID").toString();
  }

  void requireCurrent(DomainContext context, Map<String,Object> proposal) {
    if (proposal.get("goalVersionId") == null) throw unresolved();
    // Resolve the historical row in the same organization and Work before using its pin.
    var goal = repository.external(context, "mulino.work.read.GoalReferences",
        proposal.get("goalVersionId").toString());
    if (!Objects.equals(proposal.get("workId"), goal.get("workId"))) {
      throw DomainError.forbidden();
    }
    if (!Objects.equals(proposal.get("goalVersionId"), current(context, proposal.get("workId").toString()))
        || !Objects.equals(proposal.get("proposalHash"), PurchaseHash.of(proposal))) {
      throw new DomainError("HELD", "APPROVAL_HASH_MISMATCH",
          "Work goal or proposal hash changed; revise the proposal and obtain a new approval");
    }
  }

  private DomainError unresolved() {
    return new DomainError("HELD", "POLICY_UNRESOLVED",
        "Immutable purchase goal pin unavailable; establish the current Work goal and revise the proposal");
  }
}
