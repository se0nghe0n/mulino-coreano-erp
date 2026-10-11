package com.mulino.adapters.blob;

import com.mulino.application.core.DomainError;
import com.mulino.application.evidence.EvidenceQueries;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class EvidenceDownload {
  private final EvidenceQueries evidence;
  public EvidenceDownload(EvidenceQueries evidence) { this.evidence=evidence; }
  @GetMapping("/api/evidence/documents/{id}/content")
  public ResponseEntity<byte[]> download(@PathVariable String id) {
    var content=evidence.download(id);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
      .header("X-Content-Type-Options","nosniff")
      .header("Content-Disposition","attachment; filename=\"evidence.bin\"")
      .cacheControl(CacheControl.noStore()).body(content.bytes());
  }
  /** A refused download is a domain answer with the same envelope and status rule as the command/query API (OntologyRest), not a servlet 500. */
  @ExceptionHandler(DomainError.class)
  ResponseEntity<Map<String,Object>> refused(DomainError failure) {
    return ResponseEntity.status(failure.code().equals("FORBIDDEN")?403:failure.outcome().equals("CONFLICT")?409:400)
      .contentType(MediaType.APPLICATION_JSON).cacheControl(CacheControl.noStore()).body(failure.response());
  }
}
