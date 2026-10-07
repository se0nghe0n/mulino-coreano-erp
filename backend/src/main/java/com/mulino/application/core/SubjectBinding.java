package com.mulino.application.core;
import java.util.*;
/** Handler-owned subject identity and declaration cardinality, computed from validated targets. */
public record SubjectBinding(String nounType,Set<String> targetIds,int minimumCount,int maximumCount) {
  public SubjectBinding {
    if(nounType==null||nounType.isBlank()||targetIds==null||minimumCount<0||maximumCount<minimumCount||maximumCount>targetIds.size())throw new IllegalArgumentException("Invalid subject contract");
    targetIds=Set.copyOf(targetIds);targetIds.forEach(CommandRequests::uuid);
  }
  public static SubjectBinding optional(String nounType,Set<String> targetIds){return new SubjectBinding(nounType,targetIds,0,targetIds.size());}
  public static SubjectBinding required(String nounType,Set<String> targetIds){return new SubjectBinding(nounType,targetIds,targetIds.size(),targetIds.size());}
  public Map<String,Object> facts(){return Map.of("nounType",nounType,"targetIds",new TreeSet<>(targetIds),"minimumCount",minimumCount,"maximumCount",maximumCount);}
}
