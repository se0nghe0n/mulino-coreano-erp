package com.mulino.application.core;

/**
 * Named pause points of the shared write pipeline (plan section 10: the dev/CI profile controls races with a virtual
 * clock and barriers). Production has no implementation bean, so every call is a no-op. Only the explicit
 * verification profile registers one, and it pauses only a request that carried a loopback barrier header for exactly
 * that point. Several authored point names may describe the same pipeline location; each location lists them all.
 */
public interface CommandBarrier {
  /** Pause the current request if its declared barrier names one of these points. */
  void reach(String... points);
  /** Report a point the request is about to block on (for example a fence held by another transaction) without pausing. */
  void blocking(String... points);
}
