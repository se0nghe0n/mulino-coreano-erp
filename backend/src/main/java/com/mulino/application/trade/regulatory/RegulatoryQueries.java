package com.mulino.application.trade.regulatory;
import com.mulino.application.core.*;
import com.mulino.domain.trade.regulatory.RegulatoryRepository;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class RegulatoryQueries implements QueryHandler,ObjectReadProvider {
 private final RegulatoryRepository r;private final ReadAuthorizer auth;
 public RegulatoryQueries(RegulatoryRepository r,ReadAuthorizer auth){this.r=r;this.auth=auth;}
 public Set<String> objectTypes(){return Set.of("RegulatoryProcedure");}
 public Set<String> operations(){return Set.of("getRegulatoryProcedure");}
 public QueryResult query(DomainContext c,QueryRequest q){
  if(!Set.of("organizationId","itemId","lotId","workId","objectType").containsAll(q.scope().keySet())||!Set.of("type","sort").containsAll(q.filters().keySet())||q.filters().containsKey("sort")&&!"ID".equals(q.filters().get("sort"))||q.filters().containsKey("type")&&!"RegulatoryProcedure".equals(q.filters().get("type"))||q.scope().containsKey("objectType")&&!"RegulatoryProcedure".equals(q.scope().get("objectType")))throw DomainError.invalid("Unsupported regulatory scope or filter");
  if(q.id()==null)return page(c,q);
  var proc=r.require(c,"Procedures",q.id());for(var e:q.scope().entrySet()){Object expected=e.getKey().equals("objectType")?"RegulatoryProcedure":proc.get(e.getKey());if(!Objects.equals(expected,e.getValue()))throw DomainError.forbidden();}var scopes=Map.<String,Collection<String>>of("ITEM",List.of(proc.get("itemId").toString()),"WORK",List.of(proc.get("workId").toString()),"TARGET",List.of(proc.get("ID").toString(),proc.get("physicalScopeId").toString()));auth.authorizeScopes(c,Set.of("getObject","searchObjects").contains(q.operation())?q.operation():"getRegulatoryProcedure",scopes);
  var versions=r.rows(c,"ProcedureVersions").stream().filter(v->q.id().equals(v.get("procedureId"))).toList();var decisions=r.rows(c,"DecisionVersions").stream().filter(v->q.id().equals(v.get("procedureId"))).toList();var labels=r.rows(c,"LabelVerifications").stream().filter(v->q.id().equals(v.get("procedureId"))).toList();var head=versions.stream().filter(v->versions.stream().noneMatch(n->v.get("ID").equals(n.get("previousVersionId")))).findFirst().orElseThrow(DomainError::forbidden);var data=new LinkedHashMap<String,Object>(proc);data.put("currentVersionId",head.get("ID"));data.put("status",head.get("status"));data.put("revision",java.util.stream.Stream.of(versions,decisions,labels).flatMap(Collection::stream).mapToInt(x->((Number)x.get("revision")).intValue()).max().orElse(1));data.put("versions",versions);data.put("decisions",decisions);data.put("labels",labels);data.put("legalPermissionClaimed",false);data.put("type","RegulatoryProcedure");
  var refs=new ArrayList<String>();for(var rows:List.of(versions,decisions,labels))for(var v:rows)for(String key:List.of("documentVersionId","occurrenceId"))if(v.get(key)!=null)refs.add(v.get(key).toString());return new QueryResult(data,Map.of("itemId",proc.get("itemId"),"physicalScopeId",proc.get("physicalScopeId"),"workId",proc.get("workId")),List.of("OPERATING_REGULATORY_POLICY_REQUIRES_AUTHORITY_CONFIRMATION"),List.of(),refs.stream().distinct().toList(),null);
 }
 private QueryResult page(DomainContext c,QueryRequest q){
  String after=ReadCursor.decode(q.cursor(),c,q);var rows=new ArrayList<Object>();var refs=new TreeSet<String>();var unknowns=new TreeSet<String>();String last=null;boolean more=false;
  for(var root:r.rows(c,"Procedures").stream().sorted(Comparator.comparing(x->x.get("ID").toString())).toList()){
   String id=root.get("ID").toString();if(after!=null&&id.compareTo(after)<=0)continue;
   try{var result=query(c,new QueryRequest(q.operation(),id,q.scope(),q.filters(),q.limit(),null,q.definitionVersion(),q.asOf(),q.knownAt(),null));if(rows.size()==q.limit()){more=true;break;}rows.add(result.data());refs.addAll(result.evidenceRefs());unknowns.addAll(result.unknowns());last=id;}
   catch(DomainError denied){if(!"FORBIDDEN".equals(denied.code()))throw denied;}
  }
  return new QueryResult(rows,q.scope(),List.copyOf(unknowns),List.of(),List.copyOf(refs),more?ReadCursor.encode(last,c,q):null);
 }

}
