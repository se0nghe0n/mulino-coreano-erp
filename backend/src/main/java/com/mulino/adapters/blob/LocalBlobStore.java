package com.mulino.adapters.blob;

import com.mulino.application.core.DomainError;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Local immutable object store: caller-supplied paths and URI fetching do not exist. */
@Component
public class LocalBlobStore {
  public record Staged(UUID id, String sha256, long size) {}
  private final Path root;
  public LocalBlobStore(@Value("${mulino.evidence.blob-root:${java.io.tmpdir}/mulino-evidence-blobs}") String root) {
    this.root = Path.of(root).toAbsolutePath().normalize();
    try { Files.createDirectories(this.root.resolve("staging")); Files.createDirectories(this.root.resolve("objects")); }
    catch (IOException e) { throw new IllegalStateException("Blob store unavailable",e); }
  }
  public Staged stage(byte[] content, String expectedHash) {
    if (content == null || content.length > 16*1024*1024) throw DomainError.invalid("Document too large or missing");
    String hash = hash(content);
    if (expectedHash == null || !hash.equals(expectedHash))
      throw new DomainError("REJECTED","EVIDENCE_HASH_MISMATCH","Original content hash differs");
    UUID id = UUID.randomUUID();
    try { Files.write(path("staging",id),content,StandardOpenOption.CREATE_NEW); }
    catch(IOException e) { throw new IllegalStateException("Upload unavailable",e); }
    return new Staged(id,hash,content.length);
  }
  public void commit(Staged staged) {
    try {
      byte[] bytes=Files.readAllBytes(path("staging",staged.id()));
      if(!hash(bytes).equals(staged.sha256()) || bytes.length!=staged.size()) throw DomainError.invalid("Staged content changed");
      Files.move(path("staging",staged.id()),path("objects",staged.id()),StandardCopyOption.ATOMIC_MOVE);
      Files.setPosixFilePermissions(path("objects",staged.id()),PosixFilePermissions.fromString("r--------"));
    } catch(IOException e) { throw new IllegalStateException("Upload commit unavailable",e); }
  }
  public void discard(UUID id) {
    try { Files.deleteIfExists(path("staging",id)); Files.deleteIfExists(path("objects",id)); }
    catch(IOException e) { throw new IllegalStateException("Upload cleanup unavailable",e); }
  }
  public byte[] read(UUID id,String expectedHash) {
    try {
      Path p=path("objects",id);
      if(Files.isSymbolicLink(p)) throw new IOException("Symlink denied");
      byte[] content=Files.readAllBytes(p);
      if(!hash(content).equals(expectedHash)) throw new IOException("Hash mismatch");
      return content;
    } catch(IOException e) { throw new DomainError("REJECTED","EVIDENCE_UNAVAILABLE","Original content unavailable"); }
  }
  public boolean available(UUID id,String expectedHash) {
    try { read(id,expectedHash); return true; } catch(DomainError e) { return false; }
  }
  /** Reference predicate must inspect all committed DB references. Grace excludes active uploads. */
  public int cleanupOrphans(Instant olderThan,Predicate<UUID> referenced) {
    int removed=0;
    for(String dir:List.of("staging","objects")) {
      try(var files=Files.list(root.resolve(dir))) {
        for(Path p:files.toList()) {
          UUID id;
          try { id=UUID.fromString(p.getFileName().toString()); } catch(IllegalArgumentException e) { continue; }
          if(Files.getLastModifiedTime(p,LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(olderThan) && !referenced.test(id)) {
            Files.deleteIfExists(p); removed++;
          }
        }
      }catch(IOException e) { throw new IllegalStateException("Blob cleanup unavailable",e); }
    }
    return removed;
  }
  private Path path(String directory,UUID id) { return root.resolve(directory).resolve(id.toString()); }
  public static String hash(byte[] bytes) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    catch(Exception e) { throw new IllegalStateException(e); }
  }
}
