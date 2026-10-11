package com.mulino.application.core;

/**
 * Named failure points of the shared write pipeline (plan section 10: the dev/CI profile injects faults to prove
 * rollback and lost-response recovery). Production registers no bean, so every call is a no-op. Only the explicit
 * verification profile registers one, and it fails only a command that matches a fault armed for the same organization
 * (and, when the fault names them, the same capability and command key).
 */
public interface CommandFaults {
  /** Throw if an armed fault matches this command at one of these points. */
  void check(String organizationId, String capabilityId, String commandKey, String... points);
}
