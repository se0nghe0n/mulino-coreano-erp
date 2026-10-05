package com.mulinocoreano.backend.evidence;

import java.sql.SQLException;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * READ_COMMITTED observes the receipt committed by the advisory-lock holder. Claim locks serialize
 * revision changes; source/link unique constraints preserve identities.
 */
@Component
public class EvidenceTransactions {
  private final TransactionTemplate transaction;

  public EvidenceTransactions(PlatformTransactionManager manager) {
    transaction = new TransactionTemplate(manager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    transaction.setTimeout(30);
  }

  public <T> T execute(Supplier<T> action) {
    for (int attempt = 0; attempt < 3; attempt++) {
      try {
        return transaction.execute(status -> action.get());
      } catch (RuntimeException error) {
        boolean retry = false;
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
          if (cause instanceof SQLException sql && "40P01".equals(sql.getSQLState())) retry = true;
        }
        if (!retry) throw error;
        if (attempt == 2) throw new ResponseStatusException(HttpStatus.CONFLICT);
      }
    }
    throw new IllegalStateException();
  }
}
