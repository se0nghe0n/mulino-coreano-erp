package com.mulino.application.trade.settlement;
import com.mulino.application.core.*;
import com.mulino.domain.trade.settlement.SettlementRepository;
import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;
import static com.mulino.domain.trade.settlement.SettlementAmounts.*;
/** Noun and verb share immutable invoice, match, evidence and duty snapshot. */
@Component
public class SettlementQueries implements QueryHandler,ObjectReadProvider {
 private final SettlementRepository r;private final ReadAuthorizer auth;private final SettlementTradeFacts facts;private final SettlementState state;
 public SettlementQueries(SettlementRepository r,ReadAuthorizer auth,SettlementTradeFacts facts,SettlementState state){this.r=r;this.auth=auth;this.facts=facts;this.state=state;}
 public Set<String> operations(){return Set.of("getSettlement");}
 public Set<String> objectTypes(){return Set.of("Invoice");}
 private boolean visible(DomainContext c,Map<String,Object> x){return !Instant.parse(x.get("recordedAt").toString()).isAfter(c.knownAt())&&(x.get("effectiveAt")==null||!Instant.parse(x.get("effectiveAt").toString()).isAfter(c.asOf()));}
 private List<Map<String,Object>> related(DomainContext c,String table,String invoice){return r.rows(c,table).stream().filter(x->invoice.equals(x.get("invoiceId"))&&visible(c,x)).toList();}
 public QueryResult query(DomainContext c,QueryRequest q){if(!Set.of("organizationId","itemId","workId","invoiceId","type","objectType").containsAll(q.scope().keySet())||!Set.of("type","sort").containsAll(q.filters().keySet()))throw DomainError.invalid("Unsupported settlement query scope or filter");if(q.filters().containsKey("sort")&&!"ID".equals(q.filters().get("sort")))throw DomainError.invalid("Unsupported settlement sort");if(q.filters().get("type")!=null&&!"Invoice".equals(q.filters().get("type")))throw DomainError.invalid("Unsupported invoice type");if(q.id()==null){if(!"searchObjects".equals(q.operation()))throw DomainError.invalid("Invoice ID required");return page(c,q);}var invoice=r.require(c,"Invoices",q.id());for(var entry:q.scope().entrySet()){Object expected=entry.getKey().equals("invoiceId")?invoice.get("ID"):Set.of("type","objectType").contains(entry.getKey())?"Invoice":invoice.get(entry.getKey());if(!Objects.equals(expected,entry.getValue()))throw DomainError.forbidden();}if(!visible(c,invoice))throw DomainError.forbidden();var line=visibleLine(c,invoice);var readScope=SettlementCommands.scopes(line);auth.authorizeScopes(c,q.operation(),readScope);var data=new LinkedHashMap<>(invoice);data.put("readScope",readScope);var matches=related(c,"Matches",q.id());var adjustments=related(c,"Adjustments",q.id());var charges=related(c,"Charges",q.id());var payments=related(c,"PaymentReferences",q.id());data.put("type","Invoice");data.put("matches",matches);data.put("charges",charges);data.put("adjustments",adjustments);data.put("paymentReferences",payments);data.put("bankEffect","0");data.put("taxIssuanceEffect","0");data.put("paymentAuthorized",false);var settlement=new LinkedHashMap<String,Object>();settlement.put("result","UNVERIFIED");settlement.put("currency",invoice.get("currency"));
  // §6 정산·§4.3: the same predicate as duty closure, re-derived from current recognized facts at read time.
  if(!matches.isEmpty()){var m=matches.getFirst();settlement.putAll(state.assess(c,m,adjustments));if(m.get("dutyRootId")!=null)data.put("dutyRootId",m.get("dutyRootId"));var all=state.matchRoots(c,m);all.remove(Objects.toString(m.get("dutyRootId"),""));data.put("contributionDutyRootIds",all);settlement.put("differenceDutyOpen",state.matchRoots(c,m).stream().anyMatch(x->state.open(c,x)));}
  // A CREDIT_NOTE/CORRECTION under open human review keeps the original invoice unverified and visible.
  var correctionRoots=state.correctionRoots(c);var corrections=r.rows(c,"Invoices").stream().filter(x->q.id().equals(x.get("relatedInvoiceId"))&&visible(c,x)).map(x->{var joined=SettlementCommands.scopes(visibleLine(c,x));auth.authorizeScopes(c,q.operation(),joined);var correction=new LinkedHashMap<>(x);correction.put("readScope",joined);return correction;}).toList();var reviewRoots=new ArrayList<Object>();for(var x:corrections){Object root=correctionRoots.get(x.get("ID").toString());if(root!=null)reviewRoots.add(root);}if(correctionRoots.containsKey(q.id()))reviewRoots.add(correctionRoots.get(q.id()));data.put("corrections",corrections);data.put("reviewDutyRootIds",reviewRoots);if(corrections.stream().anyMatch(x->state.open(c,correctionRoots.get(x.get("ID").toString())))){settlement.put("result","UNVERIFIED");settlement.put("correctionReviewOpen",true);}
  data.put("settlement",settlement);auth.authorizeScopes(c,"getWork",Map.of("ITEM",List.of(invoice.get("itemId").toString()),"WORK",List.of(invoice.get("workId").toString())));auth.authorizeScopes(c,"getObligations",Map.of("ITEM",List.of(invoice.get("itemId").toString()),"WORK",List.of(invoice.get("workId").toString())));var work=r.external(c,"mulino.work.read.Works",invoice.get("workId").toString());if(!visible(c,work))throw DomainError.forbidden();var goals=r.externalRows(c,"mulino.work.read.GoalReferences").stream().filter(x->invoice.get("workId").equals(x.get("workId"))&&visible(c,x)).toList();data.put("logistics",Map.of("workId",work.get("ID"),"status",work.get("status"),"goals",goals));var duties=r.externalRows(c,"mulino.work.read.ObligationReferences").stream().filter(x->visible(c,x)&&(invoice.get("workId").equals(x.get("workId"))||data.get("dutyRootId")!=null&&data.get("dutyRootId").equals(x.get("rootId"))||x.get("rootId")!=null&&(reviewRoots.contains(x.get("rootId"))||data.get("contributionDutyRootIds") instanceof List<?> changed&&changed.contains(x.get("rootId"))))).toList();for(var duty:duties)auth.authorizeScopes(c,"getObligations",Map.of("ITEM",List.of(invoice.get("itemId").toString()),"WORK",List.of(duty.get("workId").toString())));data.put("obligations",duties);var refs=new ArrayList<String>(List.of(invoice.get("evidenceId").toString()));matches.forEach(x->refs.add(x.get("invoiceOccurrenceId").toString()));return new QueryResult(data,Map.of("organizationId",c.organizationId(),"itemId",invoice.get("itemId"),"workId",invoice.get("workId")),matches.isEmpty()?List.of("INVOICE_UNMATCHED"):List.of(),List.of(),refs,null);
 }
 private Map<String,Object> visibleLine(DomainContext c,Map<String,Object> invoice){
  String kind=invoice.get("scopeKind").toString();var line=facts.line(c,kind,invoice.get("lineId").toString());
  if(!visible(c,line)||!Objects.equals(invoice.get("itemId"),line.get("itemId"))||!Objects.equals(invoice.get("workId"),line.get("workId")))throw DomainError.forbidden();
  String namespace="PURCHASE".equals(kind)?"mulino.trade.purchase.":"mulino.trade.sales.";
  var order=r.external(c,namespace+"Orders",line.get("orderId").toString());if(!visible(c,order))throw DomainError.forbidden();
  Map<String,Object> revision;
  if("PURCHASE".equals(kind))revision=r.externalRows(c,namespace+"ProposalRevisions").stream().filter(x->Objects.equals(x.get("proposalId"),line.get("proposalId"))&&Objects.equals(x.get("revision"),line.get("proposalRevision"))).findFirst().orElseThrow(DomainError::forbidden);
  else revision=r.external(c,namespace+"OrderRevisions",line.get("revisionId").toString());
  if(!visible(c,revision))throw DomainError.forbidden();return line;
 }
 private QueryResult page(DomainContext c,QueryRequest q){
  String after=ReadCursor.decode(q.cursor(),c,q);var rows=new ArrayList<Object>();var refs=new TreeSet<String>();var unknowns=new TreeSet<String>();String last=null;boolean more=false;
  for(var root:r.rows(c,"Invoices").stream().sorted(Comparator.comparing(x->x.get("ID").toString())).toList()){
   String id=root.get("ID").toString();if(after!=null&&id.compareTo(after)<=0)continue;
   try{var result=query(c,new QueryRequest(q.operation(),id,q.scope(),q.filters(),q.limit(),null,q.definitionVersion(),q.asOf(),q.knownAt(),null));if(rows.size()==q.limit()){more=true;break;}rows.add(result.data());refs.addAll(result.evidenceRefs());unknowns.addAll(result.unknowns());last=id;}
   catch(DomainError denied){if(!"FORBIDDEN".equals(denied.code()))throw denied;}
  }
  return new QueryResult(rows,q.scope(),List.copyOf(unknowns),List.of(),List.copyOf(refs),more?ReadCursor.encode(last,c,q):null);
 }

}
