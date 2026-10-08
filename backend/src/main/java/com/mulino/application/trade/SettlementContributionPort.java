package com.mulino.application.trade;

import com.mulino.application.core.DomainContext;
import java.util.List;

/**
 * Settlement-owned hook on the evidence correction path (plan §4.3·§6 정산). When a verified correcting canonical
 * changes the recognized contribution behind an existing invoice match, settlement opens its own owned difference
 * duty in the same transaction; joins the caller transaction and never writes stock or matches.
 */
public interface SettlementContributionPort {
  /** Returns the SETTLEMENT_DIFFERENCE root IDs opened (or already present) for matches whose contribution changed. */
  List<String> contributionChanged(DomainContext context,String correctingCanonicalId);
}
