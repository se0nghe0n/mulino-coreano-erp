package com.mulino.application.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Cursor is a position bound to the query and identity, never an access grant. */
public final class ReadCursor {
  private ReadCursor(){}
  public static String encode(String id,DomainContext c,QueryRequest q){
    UUID.fromString(id);
    return Base64.getUrlEncoder().withoutPadding().encodeToString((binding(c,q)+":"+id).getBytes(StandardCharsets.UTF_8));
  }
  public static String decode(String cursor,DomainContext c,QueryRequest q){
    if(cursor==null)return null;
    try{
      String raw=new String(Base64.getUrlDecoder().decode(cursor),StandardCharsets.UTF_8);
      String[] parts=raw.split(":",2);
      if(parts.length!=2||!parts[0].equals(binding(c,q)))throw DomainError.invalid("Cursor scope or snapshot changed");
      UUID.fromString(parts[1]);return parts[1];
    }catch(RuntimeException failure){throw DomainError.invalid("Invalid cursor or query binding");}
  }
  private static String binding(DomainContext c,QueryRequest q){
    try{
      String raw=c.organizationId()+"|"+c.actorId()+"|"+c.asOf()+"|"+c.knownAt()+"|"+q.operation()+"|"+new TreeMap<>(q.scope())+"|"+new TreeMap<>(q.filters());
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    }catch(Exception failure){throw new IllegalStateException(failure);}
  }
}
