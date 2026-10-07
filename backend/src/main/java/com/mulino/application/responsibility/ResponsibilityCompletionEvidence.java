package com.mulino.application.responsibility;
import com.mulino.application.core.DomainContext;
import java.math.BigDecimal;
/** Evidence owner returns immutable source/hash/review-verified duty range; never caller scope hints. */
public interface ResponsibilityCompletionEvidence {
 record Coverage(String rootId,BigDecimal startQuantity,BigDecimal quantity,String unit,String verificationId,String coverageId) {}
 Coverage requireCoverage(DomainContext context,String canonicalOccurrenceId);
}
