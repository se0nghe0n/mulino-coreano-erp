package com.mulino.application.trade.regulatory;
import com.mulino.application.core.*;
import com.mulino.domain.trade.regulatory.RegulatoryRepository;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class RegulatoryQueries implements QueryHandler {
 private final RegulatoryRepository r;private final ReadAuthorizer auth;
 public RegulatoryQueries(RegulatoryRepository r,ReadAuthorizer auth){this.r=r;this.auth=auth;}
 public Set<String> operations(){return Set.of("getRegulatoryProcedure");}
 public QueryResult query(DomainContext c,QueryRequest q){if(q.id()==null||!q.filters().isEmpty())throw DomainError.invalid("Exact regulatory procedure required");var proc=r.require(c,"Procedures",q.id());var scopes=Map.<String,Collection<String>>of("ITEM",List.of(proc.get("itemId").toString()),"WORK",List.of(proc.get("workId").toString()),"TARGET",List.of(proc.get("ID").toString(),proc.get("physicalScopeId").toString()));auth.authorizeScopes(c,"getRegulatoryProcedure",scopes);
  var versions=r.rows(c,"ProcedureVersions").stream().filter(v->q.id().equals(v.get("procedureId"))).toList();var decisions=r.rows(c,"DecisionVersions").stream().filter(v->q.id().equals(v.get("procedureId"))).toList();var labels=r.rows(c,"LabelVerifications").stream().filter(v->q.id().equals(v.get("procedureId"))).toList();var head=versions.stream().filter(v->versions.stream().noneMatch(n->v.get("ID").equals(n.get("previousVersionId")))).findFirst().orElseThrow(DomainError::forbidden);var data=new LinkedHashMap<String,Object>(proc);data.put("currentVersionId",head.get("ID"));data.put("status",head.get("status"));data.put("versions",versions);data.put("decisions",decisions);data.put("labels",labels);data.put("legalPermissionClaimed",false);
  var refs=new ArrayList<String>();for(var rows:List.of(versions,decisions,labels))for(var v:rows)for(String key:List.of("documentVersionId","occurrenceId"))if(v.get(key)!=null)refs.add(v.get(key).toString());return new QueryResult(data,Map.of("itemId",proc.get("itemId"),"physicalScopeId",proc.get("physicalScopeId"),"workId",proc.get("workId")),List.of("OPERATING_REGULATORY_POLICY_REQUIRES_AUTHORITY_CONFIRMATION"),List.of(),refs.stream().distinct().toList(),null);
 }
}
