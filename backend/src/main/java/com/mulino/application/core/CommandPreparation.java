package com.mulino.application.core;
import java.util.*;
/** Trusted server classification; scope dimensions intersect. No payload authority. */
public record CommandPreparation(Map<String,List<String>> scopes, List<String> fenceKeys,
    Set<String> authorityActors, String effectClass, String approvalAction,
    String proposalId, int proposalRevision, String targetId, Integer currentRevision) {
  public CommandPreparation {
    scopes=Map.copyOf(scopes);fenceKeys=List.copyOf(fenceKeys);authorityActors=Set.copyOf(authorityActors);
    Objects.requireNonNull(effectClass);
  }
  public static CommandPreparation ordinary(Map<String,List<String>> scopes,List<String> fences,
      String effectClass,String targetId,Integer revision){
    return new CommandPreparation(scopes,fences,Set.of(),effectClass,null,null,0,targetId,revision);
  }
}
