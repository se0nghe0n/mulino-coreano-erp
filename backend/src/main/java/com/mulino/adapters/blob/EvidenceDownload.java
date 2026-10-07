package com.mulino.adapters.blob;

import com.mulino.application.evidence.EvidenceQueries;
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
}
