package com.mulino.application.trade.recall;
import java.util.*;
import java.util.function.Predicate;
/**
 * Current decision authority must cover every value of a scope dimension; one matching
 * alternative (PLACE C of PLACE [C,W]) is not authority over the other (plan §7.1).
 */
public final class ManagementCoverage {
 private ManagementCoverage(){}
 public static boolean covers(List<Map<String,Object>> authorities,String organizationId,String actorId,String capability,Map<String,List<String>> scopes,Predicate<Map<String,Object>> active){
  var rows=authorities.stream().filter(x->actorId.equals(x.get("actorId"))&&capability.equals(x.get("capabilityId"))&&active.test(x)).toList();
  if(rows.stream().anyMatch(x->"ORGANIZATION".equals(x.get("scopeKind"))&&organizationId.equals(x.get("scopeId"))))return true;
  return scopes.entrySet().stream().anyMatch(d->!d.getValue().isEmpty()&&d.getValue().stream().allMatch(v->rows.stream().anyMatch(x->d.getKey().equals(x.get("scopeKind"))&&v.equals(x.get("scopeId")))));
 }
}
