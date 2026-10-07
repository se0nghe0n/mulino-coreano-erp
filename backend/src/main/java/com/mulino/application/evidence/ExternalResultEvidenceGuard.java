package com.mulino.application.evidence;
import com.mulino.application.core.DomainContext;
/** Confirming an external effect requires immutable, currently verified exact operation evidence. */
public interface ExternalResultEvidenceGuard {
  void requireExternalResult(DomainContext context,String documentId,String externalOperationId,String decision);
}
