package com.mulino.application.trade.recall;
import java.util.*;
import java.util.function.Predicate;
/**
 * Current decision authority must cover every value of a scope dimension; one matching
 * alternative (PLACE C of PLACE [C,W]) is not authority over the other (plan §7.1).
 * Dimensions compose all-of (2026-10-09 user decision, AGENTS.md): every dimension kind the
 * actor's authority names must cover the target, and a target without that dimension is not
 * covered. Dimensions the authority does not name do not constrain it.
 */
public final class ManagementCoverage {
 private ManagementCoverage(){}
 public static boolean covers(List<Map<String,Object>> authorities,String organizationId,String actorId,String capability,Map<String,List<String>> scopes,Predicate<Map<String,Object>> active){
  var rows=authorities.stream().filter(x->actorId.equals(x.get("actorId"))&&capability.equals(x.get("capabilityId"))&&active.test(x)).toList();
  if(rows.stream().anyMatch(x->"ORGANIZATION".equals(x.get("scopeKind"))&&organizationId.equals(x.get("scopeId"))))return true;
  var named=rows.stream().map(x->String.valueOf(x.get("scopeKind"))).filter(k->!"ORGANIZATION".equals(k)).distinct().toList();
  if(named.isEmpty())return false;
  return named.stream().allMatch(kind->{var values=scopes.getOrDefault(kind,List.of());return !values.isEmpty()&&values.stream().allMatch(v->rows.stream().anyMatch(x->kind.equals(x.get("scopeKind"))&&v.equals(x.get("scopeId"))));});
 }
}
