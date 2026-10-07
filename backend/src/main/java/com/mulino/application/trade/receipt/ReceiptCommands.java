package com.mulino.application.trade.receipt;
import com.mulino.application.core.*;
import com.mulino.application.inventory.InventoryCommands;
import com.mulino.application.trade.purchase.PurchaseLinePort;
import com.mulino.application.trade.TradeEvidence;
import com.mulino.application.evaluation.AssessmentService;
import com.mulino.application.runtime.IntakeDutyPort;
import com.mulino.application.responsibility.ResponsibilityService;
import com.mulino.domain.evidence.EvidenceRepository;
import com.mulino.domain.inventory.*;
import com.mulino.domain.trade.receipt.ReceiptRepository;
import com.mulino.domain.identity.IdentityRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.mulino.application.inventory.InventoryCommands.*;
@Component
public final class ReceiptCommands implements CommandHandler {
 private final ReceiptRepository r;private final InventoryRepository inventory;private final ReceiptStockPrimitives stock;private final EvidenceRepository evidence;private final TradeEvidence verified;private final PurchaseLinePort purchase;private final ReadAuthorizer auth;private final IdentityRepository identity;private final AssessmentService assessments;private final IntakeDutyPort intake;private final ResponsibilityService duties;private final com.mulino.application.trade.TradeImpact impact;private final com.mulino.application.work.WorkAccess work;
 public ReceiptCommands(ReceiptRepository r,InventoryRepository inventory,ReceiptStockPrimitives stock,EvidenceRepository evidence,TradeEvidence verified,PurchaseLinePort purchase,ReadAuthorizer auth,IdentityRepository identity,AssessmentService assessments,IntakeDutyPort intake,ResponsibilityService duties,com.mulino.application.trade.TradeImpact impact,com.mulino.application.work.WorkAccess work){this.r=r;this.inventory=inventory;this.stock=stock;this.evidence=evidence;this.verified=verified;this.purchase=purchase;this.auth=auth;this.identity=identity;this.assessments=assessments;this.intake=intake;this.duties=duties;this.impact=impact;this.work=work;}
 public Set<String> capabilities(){return Set.of("receiveProvisional","confirmReceipt");}
 public Set<String> intentKinds(){return Set.of("RECORD","COMMAND");}
 private Map<String,Object> observation(DomainContext c,Map<String,Object> intent){return r.require(c,"Observations",uuid(slots(intent),"receiptId"));}
 public CommandPreparation prepare(DomainContext c,Map<String,Object> intent){
  String cap=text(intent,"capabilityId",100);var s=slots(intent);boolean provisional=cap.equals("receiveProvisional");
  var allowed=provisional?Set.of("eventId","rangeRootId","startQuantity","quantity","unit","itemId","lotId","placeId","purchaseLineId","transitSegmentId","workId","ownerId","supervisorId","nextAction","nextCheckAt","occurredAt"):Set.of("receiptId","canonicalOccurrenceId","lotId");
  if(!allowed.containsAll(s.keySet())||!Objects.equals(intent.get("intentKind"),provisional?"RECORD":"COMMAND"))throw DomainError.invalid("Receipt slots or intent kind mismatch");
  Map<String,Object> row;
  if(provisional){
   row=new LinkedHashMap<>(s);for(String k:List.of("eventId","rangeRootId","itemId","placeId","workId","ownerId","supervisorId"))uuid(s,k);for(String k:List.of("lotId","purchaseLineId","transitSegmentId"))if(s.get(k)!=null)uuid(s,k);
   var item=inventory.current(c,"TradeItems",uuid(s,"itemId"));inventory.current(c,"Places",uuid(s,"placeId"));if(!s.get("unit").equals(item.get("baseUnit")))throw DomainError.invalid("Receipt base unit mismatch");InventoryQuantity.parse(s.get("quantity"),((Number)item.get("decimalPlaces")).intValue());start(s.get("startQuantity"));time(c,s);
   Instant next=Instant.parse(text(s,"nextCheckAt",80));if(!next.isAfter(c.knownAt()))throw DomainError.invalid("Future reconciliation check required");text(s,"nextAction",320);
   for(String k:List.of("ownerId","supervisorId"))if(!"HUMAN".equals(identity.actor(c.organizationId(),uuid(s,k)).orElseThrow(DomainError::forbidden).get("kind")))throw DomainError.invalid("Human receipt responsibility required");
   var responsible=work.require(c,uuid(s,"workId"),false);if(!Objects.equals(responsible.get("ownerId"),s.get("ownerId"))||!Objects.equals(responsible.get("supervisorId"),s.get("supervisorId")))throw DomainError.invalid("Receipt responsibility must match authoritative Work assignment");
   var event=evidence.require("Events",c.organizationId(),uuid(s,"eventId"));if(!"PHYSICAL_RECEIPT".equals(event.get("kind"))||!Objects.equals(event.get("itemId"),s.get("itemId"))||!Objects.equals(event.get("workId"),s.get("workId")))throw DomainError.invalid("Receipt source event scope mismatch");auth.authorizeScopes(c,cap,com.mulino.domain.evidence.EvidenceTypes.scopes(event));
   if(s.get("purchaseLineId")!=null){var line=purchase.requireLine(c,uuid(s,"purchaseLineId"));if(!Objects.equals(line.get("itemId"),s.get("itemId"))||!Objects.equals(line.get("unit"),s.get("unit"))||!Objects.equals(line.get("workId"),s.get("workId"))||!Objects.equals(line.get("destinationId"),s.get("placeId")))throw DomainError.invalid("Receipt purchase line scope mismatch");}
  }else{row=observation(c,intent);String lot=uuid(s,"lotId");var l=inventory.current(c,"ManufacturingLots",lot);if(!row.get("itemId").equals(l.get("itemId"))||row.get("lotId")!=null&&!lot.equals(row.get("lotId")))throw DomainError.invalid("Receipt LOT mismatch");uuid(s,"canonicalOccurrenceId");}
  var scopes=new LinkedHashMap<String,List<String>>();scopes.put("TARGET",List.of(provisional?row.get("rangeRootId").toString():row.get("ID").toString()));scopes.put("ITEM",List.of(row.get("itemId").toString()));scopes.put("PLACE",List.of(row.get("placeId").toString()));scopes.put("WORK",List.of(row.get("workId").toString()));
  if(row.get("transitSegmentId")!=null){var transit=inventory.current(c,"QuantitySegments",row.get("transitSegmentId").toString());auth.authorizeScopes(c,cap,Map.of("TARGET",List.of(transit.get("ID").toString()),"ITEM",List.of(transit.get("itemId").toString()),"PLACE",List.of(transit.get("placeId").toString())));}
  var fences=new TreeSet<String>(List.of("receipt/range/"+row.get("rangeRootId"),"inventory/item/"+row.get("itemId"),"inventory/place/"+row.get("placeId"),"evidence-subject:"+row.get("workId")));if(row.get("purchaseLineId")!=null)fences.add("purchase/line/"+row.get("purchaseLineId"));
  return CommandPreparation.ordinary(scopes,new ArrayList<>(fences),provisional?"RECORD":"RECEIPT",scopes.get("TARGET").getFirst(),provisional?null:((Number)row.get("revision")).intValue());
 }
 public List<SubjectBinding> subjectBindings(DomainContext c,Map<String,Object> intent,CommandPreparation p){var s=slots(intent);if(intent.get("capabilityId").equals("receiveProvisional"))return List.of(SubjectBinding.optional("TradeItem",Set.of(uuid(s,"itemId"))),SubjectBinding.optional("Work",Set.of(uuid(s,"workId"))));var row=observation(c,intent);return List.of(SubjectBinding.optional("Receipt",Set.of(row.get("ID").toString())),SubjectBinding.optional("TradeItem",Set.of(row.get("itemId").toString())),SubjectBinding.optional("Work",Set.of(row.get("workId").toString())));}
 public Map<String,Object> execute(DomainContext c,Map<String,Object> intent){
  var p=prepare(c,intent);inventory.fence(c,p.fenceKeys());var s=slots(intent);
  if(intent.get("capabilityId").equals("receiveProvisional")){
   var row=StockPrimitives.row(c,StockPrimitives.id(),c.asOf());row.putAll(s);row.put("quantity",InventoryQuantity.parse(s.get("quantity"),((Number)inventory.current(c,"TradeItems",s.get("itemId").toString()).get("decimalPlaces")).intValue()));row.put("startQuantity",start(s.get("startQuantity")));row.put("occurredAt",time(c,s));row.put("nextCheckAt",Instant.parse(s.get("nextCheckAt").toString()));row.put("identificationStatus",s.get("lotId")==null?"UNKNOWN":"CLAIMED");row.put("state","PROVISIONAL");r.insert("Observations",row);
   impact.recorded(c,row.get("workId").toString(),row.get("ID").toString(),"RECEIPT_RECONCILIATION",row.get("rangeRootId").toString(),row.get("nextAction").toString(),StockPrimitives.instant(row.get("nextCheckAt")));assessments.invalidate(c,row.get("workId").toString());return applied(row.get("ID").toString(),null,BigDecimal.ZERO,BigDecimal.ZERO,false);
  }
  var row=new LinkedHashMap<>(observation(c,intent));String canonical=uuid(s,"canonicalOccurrenceId");row.put("lotId",uuid(s,"lotId"));
  var scope=new ReceiptEvidenceScopes(r,inventory,auth).require(c,row.get("rangeRootId").toString());if(!row.get("lotId").equals(scope.lotId())||!StockPrimitives.instant(row.get("occurredAt")).equals(scope.occurredAt()))throw DomainError.invalid("Confirmed receipt identity/time differs from reconciled range");
  verified.requireCanonical(c,canonical,"PHYSICAL_RECEIPT",row.get("itemId").toString(),row.get("rangeRootId").toString(),(BigDecimal)row.get("quantity"),row.get("unit").toString());
  var prior=r.currentRows(c,"Receipts").stream().filter(x->row.get("rangeRootId").equals(x.get("rangeRootId"))&&overlap(row,x)).toList();
  if(!prior.isEmpty()){var old=prior.getFirst();if(prior.size()!=1||!same(row,old))throw new DomainError("HELD","EVIDENCE_CONFLICT","Receipt range overlaps committed physical history");if(!canonical.equals(old.get("canonicalOccurrenceId")))throw new DomainError("HELD","EVIDENCE_CONFLICT","Duplicate source must link the original canonical occurrence");if("PROVISIONAL".equals(row.get("state")))r.confirmed(c,row.get("ID").toString());return applied(old.get("ID").toString(),old.get("segmentId").toString(),(BigDecimal)old.get("contributedQuantity"),(BigDecimal)old.get("excessQuantity"),true);}
  String segment=stock.receive(c,row,canonical,CommandExecution.commandId());BigDecimal contributed=BigDecimal.ZERO,excess=(BigDecimal)row.get("quantity");
  if(row.get("purchaseLineId")!=null){var credit=purchase.creditReceipt(c,row.get("purchaseLineId").toString(),canonical,(BigDecimal)row.get("quantity"),row.get("unit").toString());contributed=new BigDecimal(credit.get("contributedQuantity").toString());excess=new BigDecimal(credit.get("excessQuantity").toString());var line=purchase.requireLine(c,row.get("purchaseLineId").toString());BigDecimal remaining=((BigDecimal)line.get("quantity")).subtract(new BigDecimal(credit.get("cumulativeQuantity").toString()));if(remaining.signum()>0)impact.recorded(c,row.get("workId").toString(),canonical,"RECEIPT_SHORTFALL",row.get("rangeRootId").toString(),"남은 발주 수령량을 대조한다",StockPrimitives.instant(row.get("nextCheckAt")),Map.of("purchaseLineId",row.get("purchaseLineId")),remaining,row.get("unit").toString());}
  var receipt=StockPrimitives.row(c,StockPrimitives.id(),c.asOf());for(String k:List.of("rangeRootId","startQuantity","quantity","unit","itemId","lotId","placeId","purchaseLineId","workId","occurredAt"))if(row.get(k)!=null)receipt.put(k,row.get(k));receipt.putAll(Map.of("observationId",row.get("ID"),"canonicalOccurrenceId",canonical,"segmentId",segment,"contributedQuantity",contributed,"excessQuantity",excess,"physicalEffect",row.get("transitSegmentId")==null?"NEW_STOCK":"TRANSIT_MOVE"));r.insert("Receipts",receipt);r.confirmed(c,row.get("ID").toString());assessments.invalidate(c,row.get("workId").toString());
  if(excess.signum()>0)impact.recorded(c,row.get("workId").toString(),canonical,row.get("purchaseLineId")==null?"RECEIPT_UNALLOCATED":"RECEIPT_EXCESS",row.get("rangeRootId").toString(),"대조할 초과 또는 미배분 수령량 "+InventoryQuantity.text(excess),StockPrimitives.instant(row.get("nextCheckAt")),Map.of("receiptId",receipt.get("ID")),excess,row.get("unit").toString());
  return applied(receipt.get("ID").toString(),segment,contributed,excess,false);
 }
 private static Map<String,Object> applied(String id,String segment,BigDecimal contributed,BigDecimal excess,boolean duplicate){var effects=new LinkedHashMap<String,Object>();effects.put("receiptId",id);if(segment!=null)effects.put("segmentId",segment);effects.put("contributedQuantity",InventoryQuantity.text(contributed));effects.put("excessQuantity",InventoryQuantity.text(excess));effects.put("duplicate",duplicate);return Map.of("outcome","APPLIED","revision",segment==null?0:1,"effects",effects);}
 private static BigDecimal start(Object value){if(!(value instanceof String s)||!s.matches("(?:0|[1-9][0-9]*)(?:\\.[0-9]{1,12})?"))throw DomainError.invalid("Decimal range start required");BigDecimal q=new BigDecimal(s);if(q.precision()-q.scale()>26)throw DomainError.invalid("Range start overflow");return q;}
 private static boolean overlap(Map<String,Object>a,Map<String,Object>b){var start=(BigDecimal)a.get("startQuantity");var other=(BigDecimal)b.get("startQuantity");return start.compareTo(other.add((BigDecimal)b.get("quantity")))<0&&other.compareTo(start.add((BigDecimal)a.get("quantity")))<0;}
 private static boolean same(Map<String,Object>a,Map<String,Object>b){return ((BigDecimal)a.get("startQuantity")).compareTo((BigDecimal)b.get("startQuantity"))==0&&((BigDecimal)a.get("quantity")).compareTo((BigDecimal)b.get("quantity"))==0&&List.of("itemId","lotId","unit","placeId","purchaseLineId").stream().allMatch(k->Objects.equals(a.get(k),b.get(k)));}
}
