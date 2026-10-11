package com.mulino.evidence;

import com.mulino.adapters.blob.EvidenceDownload;
import com.mulino.application.core.DomainError;
import com.mulino.application.evidence.EvidenceQueries;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** A refused evidence download answers with the domain envelope, never a servlet 500. */
class EvidenceDownloadTest {
  @Test void refusedDownloadIsForbiddenWithTheEnvelope() throws Exception {
    var queries = mock(EvidenceQueries.class);
    when(queries.download("d1")).thenThrow(DomainError.forbidden());
    var mvc = MockMvcBuilders.standaloneSetup(new EvidenceDownload(queries)).build();
    mvc.perform(get("/api/evidence/documents/d1/content")).andExpect(status().isForbidden())
      .andExpect(jsonPath("$.outcome").value("REJECTED")).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  }

  @Test void unavailableOriginalIsABadRequestWithTheEnvelope() throws Exception {
    var queries = mock(EvidenceQueries.class);
    when(queries.download("d2")).thenThrow(new DomainError("REJECTED", "EVIDENCE_UNAVAILABLE", "Original content unavailable"));
    var mvc = MockMvcBuilders.standaloneSetup(new EvidenceDownload(queries)).build();
    mvc.perform(get("/api/evidence/documents/d2/content")).andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error.code").value("EVIDENCE_UNAVAILABLE"));
  }
}
