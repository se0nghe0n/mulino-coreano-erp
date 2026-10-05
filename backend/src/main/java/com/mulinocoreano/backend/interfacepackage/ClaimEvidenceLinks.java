package com.mulinocoreano.backend.interfacepackage;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Both Dispatcher and authorized #44 writes share this relation contract and DB invalidation. */
@Component
public class ClaimEvidenceLinks {
  private final JdbcClient jdbc;

  public ClaimEvidenceLinks(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public void insert(long claimId, long evidenceId, String relation) {
    jdbc.sql(
            "INSERT INTO"
                + " claim_evidence(claim_id,evidence_id,relation,writer_principal,writer_user_id,writer_run_id)"
                + " VALUES(:claim,:source,:relation,nullif(current_setting('mulino.evidence_principal',TRUE),''),nullif(current_setting('mulino.evidence_user',TRUE),'')::bigint,nullif(current_setting('mulino.evidence_run',TRUE),'')::bigint)"
                + " ON CONFLICT(claim_id,evidence_id,relation) DO NOTHING")
        .param("claim", claimId)
        .param("source", evidenceId)
        .param("relation", relation)
        .update();
  }
}
