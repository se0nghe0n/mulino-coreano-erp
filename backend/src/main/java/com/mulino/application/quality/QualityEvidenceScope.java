package com.mulino.application.quality;
import com.mulino.application.core.*;
import com.mulino.application.trade.TradeEvidenceScopePort;
import com.mulino.application.work.WorkAccess;
import com.mulino.domain.inventory.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;
/** Explicit source decision range; generic observations cannot manufacture partial permissions. */
@Component
public class QualityEvidenceScope implements TradeEvidenceScopePort {
 private final InventoryRepository r;private final StockPrimitives stock;private final WorkAccess works;
 public QualityEvidenceScope(InventoryRepository r,StockPrimitives stock,WorkAccess works){this.r=r;this.stock=stock;this.works=works;}
 public Set<String>eventKinds(){return Set.of("QUALITY_DECISION_recordDispositionBasis_QC","QUALITY_DECISION_recordDispositionBasis_CUSTOMER","QUALITY_DECISION_recordDispositionBasis_COMMERCIAL","QUALITY_DECISION_placeHold_QC","QUALITY_DECISION_placeHold_CUSTOMER","QUALITY_DECISION_placeHold_COMMERCIAL","QUALITY_DECISION_placeHold_RECALL");}
 public Scope require(DomainContext c,String physicalScopeId,Map<String,Object>claim,Map<String,Object>event,Map<String,Object>document,byte[]original){
  try{var mapper=new ObjectMapper();var source=mapper.readTree(original);var payload=mapper.readTree(event.get("payload").toString());var segment=r.current(c,"QuantitySegments",physicalScopeId);
   if(!source.isObject()||!source.equals(payload)||!"CONFIRMED".equals(segment.get("identificationStatus"))||!"IDENTIFIED".equals(segment.get("mixtureStatus"))||!physicalScopeId.equals(source.path("segmentId").asText())||!"SEGMENT".equals(claim.get("subjectKind"))||!physicalScopeId.equals(claim.get("subjectId"))||!physicalScopeId.equals(document.get("subjectId"))||!("QUALITY_DECISION_"+source.path("operation").asText()+"_"+source.path("category").asText()).equals(event.get("kind")))throw held();
   String sourceDecision=source.path("sourceDecisionId").asText(),sourceVersion=source.path("sourceVersion").asText();CommandRequests.uuid(sourceDecision);if(sourceVersion.isBlank()||sourceVersion.length()>80||!sourceDecision.equals(event.get("externalEventId"))||!sourceVersion.equals(event.get("sourceVersion")))throw held();
   String unit=source.path("unit").asText();var quantity=stock.quantity(c,segment,source.path("quantity").asText(),unit);var start=new BigDecimal(source.path("startQuantity").asText());stock.quantity(c,segment,start.signum()==0?"1":start.toPlainString(),unit);if(start.signum()<0||start.add(quantity).compareTo(StockPrimitives.amount(segment))>0||!(claim.get("quantity") instanceof BigDecimal cq)||quantity.compareTo(cq)!=0||!unit.equals(claim.get("unit")))throw held();
   String workId=source.path("workId").asText();CommandRequests.uuid(workId);var work=works.require(c,workId,false);if(!segment.get("itemId").equals(work.get("itemId")))throw held();
   QualityCommands.instant(Map.of("validFrom",source.path("validFrom").asText()),"validFrom");QualityCommands.instant(Map.of("validUntil",source.path("validUntil").asText()),"validUntil");
   return new Scope(Map.of("itemId",segment.get("itemId"),"placeId",segment.get("placeId"),"workId",workId),Map.of("TARGET",List.of(physicalScopeId),"ITEM",List.of((String)segment.get("itemId")),"PLACE",List.of((String)segment.get("placeId")),"WORK",List.of(workId)));
  }catch(DomainError e){throw e;}catch(Exception e){throw held();}
 }
 private static DomainError held(){return new DomainError("HELD","EVIDENCE_UNVERIFIED","Exact identified source decision range required");}
}
