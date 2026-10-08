package com.mulino.application.core;

import java.util.Set;

/**
 * The single registry of command outcomes (contracts/domain-vocabulary.json). Plan §3.4 names
 * APPLIED, WAITING_APPROVAL, NEEDS_INPUT, REJECTED, CONFLICT and ACCEPTED_PENDING_EXTERNAL; HELD is
 * the fail-closed extension for an effect-free hold that retains its owner and next action
 * (plan §8 "효과 없이 보류", §7.1 policy-unset blocking). There is no separate PENDING_EXTERNAL outcome.
 */
public final class CommandOutcomes {
  public static final Set<String> ALL=Set.of("APPLIED","ACCEPTED_PENDING_EXTERNAL","WAITING_APPROVAL","NEEDS_INPUT","REJECTED","CONFLICT","HELD");
  /** Outcomes that never commit a domain effect; the command record is stored as REJECTED. */
  public static final Set<String> NOT_APPLIED=Set.of("REJECTED","CONFLICT","NEEDS_INPUT","WAITING_APPROVAL","HELD");
  private CommandOutcomes(){}
}
