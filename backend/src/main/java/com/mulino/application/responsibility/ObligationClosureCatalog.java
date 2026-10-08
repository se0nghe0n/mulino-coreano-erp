package com.mulino.application.responsibility;

import com.mulino.application.core.DomainError;
import java.util.*;

/**
 * Closure contract for every obligation kind the product can create (plan §5.3, §7.1, §7.2).
 * resolution: the kind-specific verified fulfilment evidence resolveObligation accepts.
 * autoClosure: the domain fact that closes the duty inside its own command transaction.
 * waiverCapability: the per-kind authority's typed decision capability (MANAGER/QC/ADMIN class).
 * A kind whose closure the plan does not define stays fail-closed with a structured HELD
 * naming the current human owner and next action; it is never a generic VERSION_UNSUPPORTED.
 */
public final class ObligationClosureCatalog {
 public static final String MANAGER="decideQuantityDutyWaiver",QC="decideQualityDutyWaiver",ADMIN="decideRecallDutyWaiver";
 public static final Set<String> DECISION_CAPABILITIES=Set.of(MANAGER,QC,ADMIN);
 public record Closure(String kind,String resolution,String autoClosure,String waiverCapability,String nextAction){
  public boolean resolvable(){return resolution!=null;}
  public boolean waivable(){return waiverCapability!=null;}
  public String authorityClass(){return waiverCapability==null?null:MANAGER.equals(waiverCapability)?"MANAGER":QC.equals(waiverCapability)?"QC":"ADMIN";}
 }
 private static final Map<String,Closure> KINDS=new TreeMap<>();
 private static void kind(String kind,String resolution,String auto,String waiver,String next){KINDS.put(kind,new Closure(kind,resolution,auto,waiver,next));}
 static {
  kind("FOLLOWUP_REVIEW","verified RESPONSE_COMPLETED covering the exact duty root range",null,null,"검증된 대응 완료 증거로 해소하거나 계속 대응한다");
  kind("QUALITY_REVIEW","QC releaseHold decision of the exact referenced restriction","releaseHold of the referenced restriction",null,"참조 제한의 QC 해제 결정으로만 해소한다");
  kind("REGULATORY_REVIEW","current verified regulatory decision/submission version of the same procedure","recordRegulatoryDecision/recordSubmission of the procedure",null,"같은 절차의 현재 검증된 기관 결과로 해소한다");
  kind("EXTERNAL_RECONCILIATION","verified CONFIRMED_SUCCESS result evidence for every outbox effect of the decision",null,null,"recordExternalReconciliation로 외부 결과를 확정한다");
  kind("DELIVERY_CORRECTED_DEFICIT","current verified PHYSICAL_DELIVERY correction whose recognized quantity restores the exact duty range",null,MANAGER,"정정된 실제 인도의 부족량을 대조하거나 MANAGER 면제 결정을 받는다");
  kind("SETTLEMENT_DIFFERENCE","MANAGER-confirmed settlement adjustments leaving zero remaining difference for the exact invoice match",null,MANAGER,"정산 조정 확정 또는 MANAGER 면제 결정을 받는다");
  kind("RECEIPT_SHORTFALL",null,"verified receipt contribution of the purchase line range",MANAGER,"남은 발주 수령을 대조하거나 MANAGER 면제 결정을 받는다");
  kind("RECEIPT_EXCESS",null,null,MANAGER,"초과 수령량을 대조하거나 MANAGER 면제 결정을 받는다");
  kind("RECEIPT_UNALLOCATED",null,null,MANAGER,"미배분 수령량을 대조하거나 MANAGER 면제 결정을 받는다");
  kind("PURCHASE_EXCESS_RECONCILIATION",null,null,MANAGER,"초과 실수령량을 대조하거나 MANAGER 면제 결정을 받는다");
  kind("ALLOCATION_SHORTAGE",null,null,MANAGER,"부족 배분을 대체하거나 MANAGER 면제 결정을 받는다");
  kind("RETURN_SETTLEMENT_REVIEW",null,null,MANAGER,"반품 환불·정산 차이를 대조하거나 MANAGER 면제 결정을 받는다");
  kind("RETURN_QC_REVIEW",null,null,QC,"반품 실물의 QC 판단을 기록하거나 QC 면제 결정을 받는다");
  kind("RECALL_INVESTIGATION",null,null,ADMIN,"회수 조사 결과를 기록하거나 ADMIN 면제 결정을 받는다");
  kind("RECALL_EXCLUDED_SCOPE",null,null,ADMIN,"회수 제외 범위를 검토하거나 ADMIN 면제 결정을 받는다");
  // Closed by the domain's own verified observation; no separate resolve/waive is defined.
  kind("RECEIPT_RECONCILIATION",null,"verified receipt confirmation of the same observation",null,"같은 수령 관측을 검증해 확정한다");
  kind("DELIVERY_RECONCILIATION",null,"verified recordDelivery confirmation of the same observation",null,"같은 인도 관측을 검증해 확정한다");
  kind("RETURN_RECONCILIATION",null,"verified receiveReturn confirmation of the same observation",null,"같은 반품 관측을 검증해 확정한다");
  // The plan does not define closure evidence or a waiver authority for these: fail-closed.
  kind("RECALL_EXCEPTION_RESIDUAL",null,null,null,"ADMIN 예외의 미확인 잔여 책임을 유지하고 실회수·처분 증거를 확보한다");
  kind("DELIVERY_RESTRICTION_RESPONSE",null,null,null,"제한 위반 인도의 대응을 계속하고 담당 결정을 기록한다");
  kind("RETURN_COMMERCIAL_REVIEW",null,null,null,"반품의 교환·환불 결정을 별도로 기록한다");
  kind("SUPPLIER_DISPATCH_RECONCILIATION",null,null,null,"발주 전달 결과와 공급자 수락을 대조한다");
  kind("SUPPLIER_REPLY_REVIEW",null,null,null,"공급자 거절 또는 변경에 대한 새 승인 범위를 정한다");
  kind("PURCHASE_CANCELLATION_ACCEPTANCE",null,null,null,"공급자 취소 수락과 이미 실행된 범위를 확인한다");
  kind("UNVERIFIED_SOURCE_REVIEW",null,null,null,"원천 관측을 대조해 확정 또는 미확인을 유지한다");
  kind("RUNTIME_RECOVERY",null,null,null,"실행 복구 원인을 확인하고 안전한 재시도를 결정한다");
  kind("INVENTORY_VALIDITY",null,null,null,"정지된 배분과 현재 적격성을 검토한다");
 }
 private ObligationClosureCatalog(){}
 /** RETURN_<decision> execution duties from decideReturnDisposition have no defined closure. */
 public static Closure closure(String kind){
  var known=KINDS.get(kind);if(known!=null)return known;
  return new Closure(kind,null,null,null,"정의되지 않은 의무 종료 경로다; 담당자가 계속 책임진다");
 }
 public static Map<String,Closure> kinds(){return Collections.unmodifiableMap(KINDS);}
 /** Effect-free hold naming the retained human responsibility; returned only after authorization. */
 public static DomainError held(Map<String,Object> assignment,String operation){
  var closure=closure(String.valueOf(assignment.get("kind")));
  var responsibility=new LinkedHashMap<String,Object>();
  responsibility.put("assignmentId",String.valueOf(assignment.get("ID")));responsibility.put("rootId",String.valueOf(assignment.get("rootId")));
  responsibility.put("kind",closure.kind());responsibility.put("status",String.valueOf(assignment.get("status")));
  responsibility.put("ownerId",String.valueOf(assignment.get("ownerId")));responsibility.put("supervisorId",String.valueOf(assignment.get("supervisorId")));
  responsibility.put("nextAction",assignment.get("nextAction")==null?closure.nextAction():assignment.get("nextAction").toString());
  responsibility.put("nextCheckAt",String.valueOf(assignment.get("nextCheckAt")));
  responsibility.put("closureNextAction",closure.nextAction());
  var contract=new LinkedHashMap<String,Object>();contract.put("resolution",closure.resolution()==null?"UNDEFINED":closure.resolution());contract.put("autoClosure",closure.autoClosure()==null?"NONE":closure.autoClosure());contract.put("waiverAuthority",closure.authorityClass()==null?"UNDEFINED":closure.authorityClass());
  responsibility.put("closure",contract);
  return new ResponsibilityHeld("waiveObligation".equals(operation)?"OBLIGATION_WAIVER_UNDEFINED":"OBLIGATION_RESOLUTION_UNDEFINED",operation+" has no plan-defined path for "+closure.kind()+"; responsibility retained",responsibility);
 }
}
