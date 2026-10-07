package com.mulino.adapters.blob;

import com.mulino.application.core.DomainError;
import com.mulino.application.core.ExecutionClock;
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
  private final ExecutionClock clock;
  public LocalBlobStore(@Value("${mulino.evidence.blob-root:${java.io.tmpdir}/mulino-evidence-blobs}") String root,ExecutionClock clock) {
    this.clock=clock;
    this.root = Path.of(root).toAbsolutePath().normalize();
    try {
      var privateDirectory=PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
      Files.createDirectories(this.root,privateDirectory);
      Files.createDirectories(this.root.resolve("staging"),privateDirectory);
      Files.createDirectories(this.root.resolve("objects"),privateDirectory);
      for(Path directory:List.of(this.root,this.root.resolve("staging"),this.root.resolve("objects"))) {
        if(Files.isSymbolicLink(directory)||!Files.getPosixFilePermissions(directory).equals(PosixFilePermissions.fromString("rwx------")))throw new IOException("Blob directories must be private");
      }
    }
    catch (IOException e) { throw new IllegalStateException("Blob store unavailable",e); }
  }
  public Staged stage(byte[] content, String expectedHash) {
    if (content == null || content.length > 16*1024*1024) throw DomainError.invalid("Document too large or missing");
    String hash = hash(content);
    if (expectedHash == null || !hash.equals(expectedHash))
      throw new DomainError("REJECTED","EVIDENCE_HASH_MISMATCH","Original content hash differs");
    UUID id = UUID.randomUUID();
    try(var channel=Files.newByteChannel(path("staging",id),Set.of(StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE),PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))) {
      var buffer=java.nio.ByteBuffer.wrap(content);while(buffer.hasRemaining())channel.write(buffer);
    }
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
    if(olderThan.isAfter(clock.instant().minus(Duration.ofHours(1))))throw DomainError.invalid("Orphan cleanup requires at least one-hour grace");
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
