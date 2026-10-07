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
 public QueryResult query(DomainContext c,QueryRequest q){if(!q.filters().isEmpty())throw DomainError.invalid("Unsupported regulatory filter");if(q.id()==null){var selected=new ArrayList<Object>();for(var candidate:r.rows(c,"Procedures")){if(q.scope().get("itemId")!=null&&!q.scope().get("itemId").equals(candidate.get("itemId")))continue;try{selected.add(query(c,new QueryRequest(q.operation(),candidate.get("ID").toString(),q.scope(),Map.of(),q.limit(),null,q.definitionVersion(),q.asOf(),q.knownAt(),q.snapshotRef())).data());}catch(DomainError denied){if(!"FORBIDDEN".equals(denied.code()))throw denied;}if(selected.size()>=q.limit())break;}return QueryResult.of(selected,q.scope());}var proc=r.require(c,"Procedures",q.id());var scopes=Map.<String,Collection<String>>of("ITEM",List.of(proc.get("itemId").toString()),"WORK",List.of(proc.get("workId").toString()),"TARGET",List.of(proc.get("ID").toString(),proc.get("physicalScopeId").toString()));auth.authorizeScopes(c,"getRegulatoryProcedure",scopes);
  var versions=r.rows(c,"ProcedureVersions").stream().filter(v->q.id().equals(v.get("procedureId"))).toList();var decisions=r.rows(c,"DecisionVersions").stream().filter(v->q.id().equals(v.get("procedureId"))).toList();var labels=r.rows(c,"LabelVerifications").stream().filter(v->q.id().equals(v.get("procedureId"))).toList();var head=versions.stream().filter(v->versions.stream().noneMatch(n->v.get("ID").equals(n.get("previousVersionId")))).findFirst().orElseThrow(DomainError::forbidden);var data=new LinkedHashMap<String,Object>(proc);data.put("currentVersionId",head.get("ID"));data.put("status",head.get("status"));data.put("revision",java.util.stream.Stream.of(versions,decisions,labels).flatMap(Collection::stream).mapToInt(x->((Number)x.get("revision")).intValue()).max().orElse(1));data.put("versions",versions);data.put("decisions",decisions);data.put("labels",labels);data.put("legalPermissionClaimed",false);
  var refs=new ArrayList<String>();for(var rows:List.of(versions,decisions,labels))for(var v:rows)for(String key:List.of("documentVersionId","occurrenceId"))if(v.get(key)!=null)refs.add(v.get(key).toString());return new QueryResult(data,Map.of("itemId",proc.get("itemId"),"physicalScopeId",proc.get("physicalScopeId"),"workId",proc.get("workId")),List.of("OPERATING_REGULATORY_POLICY_REQUIRES_AUTHORITY_CONFIRMATION"),List.of(),refs.stream().distinct().toList(),null);
 }
}
