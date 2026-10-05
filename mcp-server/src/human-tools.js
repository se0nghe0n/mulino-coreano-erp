// Human conversation surface: read projections and explicit, version-bound decisions.
export const identifierSchema = { anyOf: [
  { type: "integer", minimum: 1, maximum: Number.MAX_SAFE_INTEGER },
  { type: "string", pattern: "^[1-9][0-9]{0,18}$" },
] };
const refSchema = { type: "string", minLength: 1, maxLength: 100 };
const versionSchema = { type: "integer", minimum: 1, maximum: 2147483647 };
const keySchema = { type: "string", minLength: 1, maxLength: 200,
  description: "동일 입력의 사용자 지시 재시도에만 재사용. 생략 시 생성한 UUID를 오류에도 반환합니다. 자동 재시도 금지." };
const schema = (properties, required = Object.keys(properties)) => ({ type: "object", properties, required, additionalProperties: false });
const read = (name, description, field, path, format, id = false) => ({ name, description,
  inputSchema: schema({ [field]: id ? identifierSchema : refSchema }), scope: "erp:read", path, format,
  validate(args) { if (id) requireIdentifier(args[field], field); else {
    requireText(args[field], field, 100);
    if ([".", ".."].includes(args[field])) throw new Error(`${field}에는 경로의 점 구간을 사용할 수 없습니다.`);
  } },
});
export function requireIdentifier(value, field) {
  if (!(typeof value === "number" ? Number.isSafeInteger(value) && value > 0
    : typeof value === "string" && /^[1-9]\d{0,18}$/.test(value)
      && (value.length < 19 || value <= "9223372036854775807"))) {
    throw new Error(`${field}는 양의 정수 또는 signed 64-bit 범위의 정확한 숫자 문자열이어야 합니다.`);
  }
}
function requireText(value, field, max) {
  if (typeof value !== "string" || !value.trim() || value.length > max) throw new Error(`${field}는 비어 있지 않은 ${max}자 이하 문자열이어야 합니다.`);
}
function requireVersion(value) {
  if (!Number.isInteger(value) || value < 1 || value > 2147483647) throw new Error("expectedVersion은 1~2147483647 정수여야 합니다.");
}
export function rejectUnknown(args, allowed) {
  if (Object.keys(args).some(key => !allowed.includes(key))) throw new Error("지원되지 않는 입력입니다. 사용자와 권한은 인증으로 결정됩니다.");
}
const show = value => value === null || value === undefined ? "미제공" : typeof value === "object" ? JSON.stringify(value) : String(value);
const rows = values => (values ?? []).map(value => `- ${show(value)}`).join("\n") || "없음";
const lineText = line => `${show(line.materialName)} (자재 ${show(line.materialId)}, 계약 ${show(line.sourceTermId)}): ${show(line.buyQuantity)} ${show(line.buyUnit)} × ${show(line.buyUnitPrice)}원 = ${show(line.lineAmountKrw)}원; 기준 ${show(line.baseQuantity)} ${show(line.baseUnit)}, 환산 ${show(line.baseUnitsPerBuyUnit)}, 기준단가 ${show(line.baseUnitPrice)}원; 납기 ${show(line.expectedDeliveryDate)}`;
const selectionReason = value => ({ SELECTED: "공급 조건을 충족한 후보 선정", NO_ELIGIBLE_SUPPLIER: "적격 공급사 없음", NO_PURCHASE_REQUIRED: "추가 구매 불필요" })[value] ?? show(value);
const evidenceNotes = values => values == null ? "미제공" : values.length === 0 ? "기록된 항목 없음" : values.map(v => `${show(v.code)}${v.detail ? `: ${v.detail}` : ""}${v.resourceRef ? ` (${v.resourceRef})` : ""}`).join("; ");
function approvalEvidenceText(e) {
  if (!e) return "계획 근거 미제공";
  const result = e.result ?? {};
  const supply = e.supply;
  const lots = projected => Array.isArray(supply) ? rows(supply.filter(l => l.projected === projected).map(l => `${show(l.sourceRef)}: ${show(l.quantity)} ${show(l.item?.unit)} / 가용일 ${show(l.availableOn)} / 만료일 ${show(l.expiresOn)} / 제외 사유 ${show(l.exclusionReason)}`)) : "미제공";
  return `저장된 계획 근거: ${show(e.planRef)} / 계획 버전 ${show(e.version)} / 기준 시각 ${show(e.asOf)} / 목표일 ${show(e.targetDate)}
출처 해시: ${show(e.sourceHash)} / 계획 해시: ${show(e.planHash)}
출처 참조: ${Array.isArray(e.sourceRefs) ? e.sourceRefs.join(", ") : "미제공"}
수요 근거: ${result.forecasts == null ? "미제공" : rows(Object.entries(result.forecasts).map(([id, f]) => `제품 ${id}: 이력 ${show(f.historyWindowStart)}~${show(f.historyEnd)}, 일평균 ${show(f.dailyMean)}, 안전재고 ${show(f.safetyQuantity)}`))}
자재 계산: ${result.requirements?.materials == null ? "미제공" : rows(result.requirements.materials.map(m => `자재 ${show(m.material?.id)}: 총소요 ${show(m.grossQuantity)}, 공급 반영 ${show(m.suppliedQuantity)}, 순소요 ${show(m.netQuantity)} ${show(m.material?.unit)} / 필요일 ${show(m.needDate)}`))}
추천 이유: ${result.purchases == null ? "미제공" : rows(result.purchases.map(p => `자재 ${show(p.material?.id)}: ${selectionReason(p.selection?.reason)} / 선정 공급사 ${show(p.selection?.chosen?.supplierId)}
  비교 후보: ${p.selection?.feasibleCandidates == null ? "미제공" : p.selection.feasibleCandidates.map(c => `공급사 ${show(c.supplierId)} 총액 ${show(c.totalAmount)}원, 예상 도착 ${show(c.expectedArrival)}`).join("; ") || "없음"}
  제외 후보: ${p.selection?.rejectedCandidates == null ? "미제공" : p.selection.rejectedCandidates.map(c => `공급사 ${show(c.supplierId)}: ${(c.reasons ?? []).join(", ")}`).join("; ") || "없음"}`))}
계획 기준 재고 (제외 사유가 있으면 사용 가능 재고로 해석하지 않음):
${lots(false)}
예상 공급 (계획 당시 미입고·미완료):
${lots(true)}
남은 불확실성: 계산 이슈 ${evidenceNotes(result.issues)} / 공급사 경고 ${result.purchases == null ? "미제공" : result.purchases.map(p => `자재 ${show(p.material?.id)}: ${evidenceNotes(p.selection?.warnings)}`).join("; ") || "기록된 항목 없음"}
이 근거는 저장 당시 자료입니다. 현재 재고와 이후 입고·생산 이행, 실제 수요는 별도 확인해야 하며 예상 공급은 현재 재고가 아닙니다.`;
}
export function approvalText(data) {
  if (data.error) return `구매 결정 미완료: ${show(data.error)}\n현재 승인안을 다시 조회하고 변경된 버전과 내용을 확인하세요.`;
  const proposal = data.proposal;
  return `승인 ${show(data.ref ?? data.id)}: ${show(data.status)}\nCase: ${show(data.caseRef)} / 계획: ${show(data.planRef)}\n필요 권한: ${show(data.requiredRole)} (구매 결정: MANAGER)\n제안 버전: ${show(data.version)}\n제안 해시: ${show(data.proposalHash)}\n만료: ${show(data.expiresAt)}\n`
    + (proposal ? `창고: ${show(proposal.warehouseId)} / 목표일: ${show(proposal.targetDate)}\n${(proposal.orders ?? []).map(order => `공급사: ${show(order.supplierName)} (${show(order.supplierId)}), 통화 ${show(order.currency)}, 납기 ${show(order.expectedDeliveryDate)}\n${(order.lines ?? []).map(lineText).join("\n")}\n공급사 합계: ${show(order.totalKrw)}원`).join("\n")}\n총액: ${show(proposal.totalKrw)}원` : "제안 근거 미제공")
    + `\n${approvalEvidenceText(data.planEvidence)}\n미조치 시: ${show(data.noActionConsequence)}`
    + `\n최종 결정: ${show(data.decision)}\n발주 ID: ${show(data.purchaseOrderIds)}\n다음 행동: 대기 중이면 표시된 버전·해시와 구매 내용을 확인한 인간의 명시적 승인 또는 차단 선택이 필요합니다. 생산·입고의 이행 여부는 별도로 확인해야 합니다.`;
}
const qualityText = d => `입고 품질: ${show(d.approvalId ?? d.receipt?.inbound_id)} / ${show(d.status)}
제안: ${show(d.proposedStatus)} / QC 인간 결정 필요
버전: ${show(d.version)} / 해시: ${show(d.proposalHash)}
근거: ${show(d.proposal ?? d)}
결정: ${show(d.decision)}`;
export const conversationTools = [
  {name:"get_claim",scope:"erp:read",description:"동일 Case의 원본·정정·관계와 판단 이력, 최신 revision·evidenceFingerprint를 조회합니다. 인간 판단은 외부 사실 검증이나 ERP 승인이 아닙니다.",
   inputSchema:schema({caseRef:refSchema,claimId:identifierSchema}),
   validate(a){requireText(a.caseRef,"caseRef",20);requireIdentifier(a.claimId,"claimId");},
   path:a=>`/epistemic/cases/${encodeURIComponent(a.caseRef)}/claims/${a.claimId}`,format:d=>`Claim: ${show(d)}`},
  {name:"register_evidence",scope:"work:write",write:true,description:"Case 원본 관측을 등록합니다. 정정은 correctsEvidenceRef와 correctionReason을 가진 새 원본입니다. content SHA-256은 서버가 계산합니다. URI는 수집하지 않습니다.",
   inputSchema:schema({caseRef:refSchema,sourceType:{type:"string",enum:["EMAIL","EXCEL_3PL","API","SLACK","MANUAL"]},externalRef:{type:"string",minLength:1,maxLength:255},observedAt:{type:"string"},title:{type:"string",minLength:1,maxLength:200},content:{type:"string",maxLength:262144},contentUri:{type:"string",maxLength:500},contentHash:{type:"string",pattern:"^[0-9a-f]{64}$"},correctsEvidenceRef:refSchema,correctionReason:{type:"string",maxLength:4000},requestKey:keySchema},["caseRef","sourceType","externalRef","observedAt","title"]),
   validate(a){requireText(a.caseRef,"caseRef",20);requireText(a.externalRef,"externalRef",255);requireText(a.title,"title",200);requireText(a.observedAt,"observedAt",100);},
   path:a=>`/epistemic/cases/${encodeURIComponent(a.caseRef)}/evidence`,body:({caseRef,requestKey,...source})=>source,format:d=>`Evidence: ${show(d)}`},
  {name:"create_claim",scope:"work:write",write:true,description:"Case에 ASSERTED 주장을 기록합니다. 증거 지지 관계만으로 VERIFIED가 되지 않습니다.",
   inputSchema:schema({caseRef:refSchema,subjectType:{type:"string",minLength:1,maxLength:50},subjectRef:{type:"string",minLength:1,maxLength:100},claimText:{type:"string",minLength:1,maxLength:16000},supersedesClaimId:identifierSchema,supersessionReason:{type:"string",minLength:1,maxLength:4000},requestKey:keySchema},["caseRef","subjectType","subjectRef","claimText"]),
   validate(a){requireText(a.caseRef,"caseRef",20);requireText(a.subjectType,"subjectType",50);requireText(a.subjectRef,"subjectRef",100);requireText(a.claimText,"claimText",16000);if(a.supersedesClaimId!==undefined){requireIdentifier(a.supersedesClaimId,"supersedesClaimId");requireText(a.supersessionReason,"supersessionReason",4000);}else if(a.supersessionReason!==undefined)throw new Error("Predecessor required");},
   path:a=>`/epistemic/cases/${encodeURIComponent(a.caseRef)}/claims`,body:({subjectType,subjectRef,claimText,supersedesClaimId,supersessionReason})=>({subjectType,subjectRef,claimText,supersedesClaimId,supersessionReason}),format:d=>`Claim: ${show(d)}`},
  {name:"link_claim_evidence",scope:"work:write",write:true,description:"같은 Case 원본을 SUPPORTS 또는 REFUTES로 연결합니다. 새 관계는 이전 판단을 stale로 남기고 반박 근거가 있으면 CONFLICTED로 표시합니다.",
   inputSchema:schema({caseRef:refSchema,claimId:identifierSchema,evidenceRef:refSchema,relation:{type:"string",enum:["SUPPORTS","REFUTES"]},requestKey:keySchema},["caseRef","claimId","evidenceRef","relation"]),
   validate(a){requireText(a.caseRef,"caseRef",20);requireIdentifier(a.claimId,"claimId");requireText(a.evidenceRef,"evidenceRef",20);if(!["SUPPORTS","REFUTES"].includes(a.relation))throw new Error("Invalid relation");},
   path:a=>`/epistemic/cases/${encodeURIComponent(a.caseRef)}/claims/${a.claimId}/links`,body:({evidenceRef,relation})=>({evidenceRef,relation}),format:d=>`Claim: ${show(d)}`},
  {name:"judge_claim",scope:"work:write",write:true,description:"get_claim의 revision·evidenceFingerprint·모든 정정과 반박을 검토한 인간의 명시적 사실 판단을 기록합니다. 자동 판단·재시도 금지. 인간 attestation이며 독립 외부 검증이나 ERP 승인 권한이 아닙니다.",
   inputSchema:schema({caseRef:refSchema,claimId:identifierSchema,status:{type:"string",enum:["VERIFIED","REFUTED","CONFLICTED"]},expectedRevision:{type:"integer",minimum:0,maximum:Number.MAX_SAFE_INTEGER},evidenceFingerprint:{type:"string",pattern:"^[0-9a-f]{64}$"},reason:{type:"string",minLength:1,maxLength:4000},requestKey:keySchema},["caseRef","claimId","status","expectedRevision","evidenceFingerprint","reason"]),
   validate(a){requireText(a.caseRef,"caseRef",20);requireIdentifier(a.claimId,"claimId");requireText(a.reason,"reason",4000);if(!Number.isSafeInteger(a.expectedRevision)||a.expectedRevision<0||!/[0-9a-f]{64}/.test(a.evidenceFingerprint)||!["VERIFIED","REFUTED","CONFLICTED"].includes(a.status))throw new Error("Invalid judgment");},
   path:a=>`/epistemic/cases/${encodeURIComponent(a.caseRef)}/claims/${a.claimId}/judgment`,body:({status,expectedRevision,evidenceFingerprint,reason})=>({status,expectedRevision,evidenceFingerprint,reason}),format:d=>`인간 판단: ${show(d)}`},

  read("get_lot_trace", "완제품 LOT의 전수 원재료·공급사·영향 LOT·고객 출하와 불완전 근거를 조회합니다.", "lotId", a=>`/recall/lots/${a.lotId}/trace`, d=>`LOT 추적: ${show(d)}`, true),
  read("get_recall_approval", "ADMIN 리콜 승인안과 OFFLINE/PENDING 보고 초안, 버전·해시·보관 기한을 조회합니다.", "approvalId", a=>`/recall/approvals/${a.approvalId}`, d=>`리콜 승인: ${show(d)}`, true),
  {name:"request_recall_investigation",scope:"work:write",write:true,description:"OPERATOR 또는 MANAGER가 동일 Case의 QC 작업에 사고 LOT과 조사 사유를 배정합니다.",
   inputSchema:schema({lotId:identifierSchema,caseRef:refSchema,reason:{type:"string",minLength:1,maxLength:4000},requestKey:keySchema},["lotId","caseRef","reason"]),
   validate(a){requireIdentifier(a.lotId,"lotId");requireText(a.caseRef,"caseRef",20);requireText(a.reason,"reason",4000);},
   path:a=>`/recall/lots/${a.lotId}/assign`,body:({caseRef,reason})=>({caseRef,reason}),format:d=>`리콜 조사: ${show(d)}`},
  {name:"decide_recall",scope:"recall:decide",write:true,description:"ADMIN 인간의 명시적 리콜 승인·반려·취소를 기록합니다. get_recall_approval의 전수 범위·버전·해시를 검토한 인간 선택만 전달하세요. APPROVE는 모든 영향 LOT을 RECALLED로 바꿉니다. OFFLINE 보고 초안은 실제 식약처 제출이 아닙니다. 자동 승인·재시도 금지.",
   inputSchema:schema({approvalId:identifierSchema,decision:{type:"string",enum:["APPROVE","BLOCK","CANCEL"]},expectedVersion:versionSchema,proposalHash:{type:"string",pattern:"^[0-9a-f]{64}$"},reason:{type:"string",minLength:1,maxLength:4000},requestKey:keySchema},["approvalId","decision","expectedVersion","proposalHash","reason"]),
   validate(a){requireIdentifier(a.approvalId,"approvalId");requireVersion(a.expectedVersion);requireText(a.reason,"reason",4000);if(!["APPROVE","BLOCK","CANCEL"].includes(a.decision)|| !/^[0-9a-f]{64}$/.test(a.proposalHash))throw new Error("Invalid recall decision");},
   path:a=>`/recall/approvals/${a.approvalId}/decision`,body:({decision,expectedVersion,proposalHash,reason})=>({decision,expectedVersion,proposalHash,reason}),format:d=>`리콜 결정: ${show(d)}`},
  read("get_quality_approval", "입고 QC 승인안의 안전 근거·버전·해시·결정 상태를 조회합니다.", "approvalId", a => `/quality/approvals/${a.approvalId}`, qualityText, true),
  read("get_inbound_quality", "입고 온도·알레르겐·인증과 생산 적격성을 조회합니다.", "inboundId", a => `/quality/inbound/${a.inboundId}`, qualityText, true),
  { name:"record_production_input",scope:"work:write",write:true,description:"OPERATOR가 QC 승인된 원재료 LOT의 실제 생산 투입을 기록합니다. HOLD·차단·승인 대기는 서버가 거절합니다.",
    inputSchema:schema({productionRecordId:identifierSchema,rawMaterialLotId:identifierSchema,quantity:{type:"number",exclusiveMinimum:0},requestKey:keySchema},["productionRecordId","rawMaterialLotId","quantity"]),
    validate(a){requireIdentifier(a.productionRecordId,"productionRecordId");requireIdentifier(a.rawMaterialLotId,"rawMaterialLotId");if(!Number.isFinite(a.quantity)||a.quantity<=0)throw new Error("Invalid quantity");},
    path:()=>"/quality/production-inputs",body:({productionRecordId,rawMaterialLotId,quantity})=>({productionRecordId,rawMaterialLotId,quantity}),format:qualityText },
  { name: "request_quality_inspection", scope:"work:write",write:true,description:"OPERATOR 또는 MANAGER가 동일 Case의 QC 에이전트에게 입고 검사를 배정합니다.",
    inputSchema:schema({inboundId:identifierSchema,caseRef:refSchema,requestKey:keySchema},["inboundId","caseRef"]),
    validate(a){requireIdentifier(a.inboundId,"inboundId");requireText(a.caseRef,"caseRef",20);},
    path:a=>`/quality/inbound/${a.inboundId}/assign`,body:({caseRef})=>({caseRef}),format:qualityText },
  { name: "inspect_inbound", scope: "work:write", write: true, description: "OPERATOR 또는 MANAGER가 입고 검사를 요청합니다. ERP 상태 변경은 QC 승인 뒤 적용됩니다.",
    inputSchema: schema({inboundId: identifierSchema, caseRef: refSchema, requestKey: keySchema}, ["inboundId","caseRef"]),
    validate(a) { requireIdentifier(a.inboundId,"inboundId"); requireText(a.caseRef,"caseRef",20); },
    path: a => `/quality/inbound/${a.inboundId}/inspect`, body: ({caseRef}) => ({caseRef}), format: qualityText },
  { name: "decide_quality", scope: "qc:decide", write: true, description: "QC 인간의 명시적 승인·반려·취소를 기록합니다. get_quality_approval의 버전·해시와 근거를 확인하고 매번 인간 선택을 받은 뒤 호출하세요. 자동 승인·재시도 금지.",
    inputSchema: schema({approvalId: identifierSchema, decision: {type:"string",enum:["APPROVE","BLOCK","CANCEL"]}, expectedVersion:versionSchema, proposalHash:{type:"string",pattern:"^[0-9a-f]{64}$"}, reason:{type:"string",minLength:1,maxLength:4000},requestKey:keySchema}, ["approvalId","decision","expectedVersion","proposalHash","reason"]),
    validate(a) {requireIdentifier(a.approvalId,"approvalId");requireVersion(a.expectedVersion);requireText(a.reason,"reason",4000);if(!["APPROVE","BLOCK","CANCEL"].includes(a.decision)|| !/^[0-9a-f]{64}$/.test(a.proposalHash)) throw new Error("Invalid quality decision");},
    path: a => `/quality/approvals/${a.approvalId}/decision`, body: ({decision,expectedVersion,proposalHash,reason})=>({decision,expectedVersion,proposalHash,reason}), format:qualityText },
  read("get_approval", "구매 승인안의 정확한 품목·금액·버전·해시, 저장된 계획의 계산·출처·기준 시각, 예상 공급·불확실성·미조치 영향과 MANAGER 권한을 확인합니다.", "approvalId", a => `/approvals/${a.approvalId}`, approvalText, true),
  read("get_purchase_order", "실제 발주 상태와 품목, 수량·단가, 입고 수량 및 승인 근거를 조회합니다.", "purchaseOrderId", a => `/purchase-orders/${a.purchaseOrderId}`, d => `발주 ${show(d.id)}: ${show(d.status)}\n공급사: ${show(d.supplierName)} (${show(d.supplierId)})\nCase: ${show(d.caseRef)} / 계획: ${show(d.planRef)} / 승인: ${show(d.approvalId)}\n${(d.items ?? []).map(line => `${lineText(line)}; 입고 수량 ${show(line.receivedBaseQuantity)} ${show(line.baseUnit)}`).join("\n")}\n다음 행동: 납기·입고와 생산 이행을 별도 확인하세요.`, true),
  { name: "decide_purchase", scope: "procurement:decide", write: true,
    description: "MANAGER의 구매 승인, 차단 또는 취소 결정을 기록합니다. get_approval로 표시한 정확한 구매 내용·버전·해시에 대해 매번 인간의 명시적 선택을 받은 뒤만 호출하세요. 저장된 정책·과거 동의로 자동 승인하지 마세요. 서버는 클라이언트의 확인 절차를 증명하지 못합니다. APPROVE는 ERP 발주를 생성할 수 있습니다. 자동 재시도 금지.",
    inputSchema: schema({ approvalId: identifierSchema, decision: { type: "string", enum: ["APPROVE", "BLOCK", "CANCEL"] }, expectedVersion: versionSchema, proposalHash: { type: "string", pattern: "^[0-9a-f]{64}$" }, reason: { type: "string", minLength: 1, maxLength: 4000 }, requestKey: keySchema }, ["approvalId", "decision", "expectedVersion", "proposalHash", "reason"]),
    validate(a) { requireIdentifier(a.approvalId, "approvalId"); requireVersion(a.expectedVersion); requireText(a.reason, "reason", 4000); if (!["APPROVE", "BLOCK", "CANCEL"].includes(a.decision)) throw new Error("decision은 APPROVE, BLOCK 또는 CANCEL이어야 합니다."); if (typeof a.proposalHash !== "string" || !/^[0-9a-f]{64}$/.test(a.proposalHash)) throw new Error("proposalHash는 소문자 64자리 SHA-256 해시여야 합니다."); },
    path: a => `/approvals/${a.approvalId}/decision`, body: ({ decision, expectedVersion, proposalHash, reason }) => ({ decision, expectedVersion, proposalHash, reason }), format: approvalText },
  { name: "answer_attention", scope: "work:write", write: true,
    description: "인간에게 요청된 일반 질문에 명시적 답변을 기록합니다. 최신 attentionRequestId·version과 답변 적용 범위를 확인하세요. governanceActionId가 있는 구매 승인 요청은 이 도구로 답하지 말고 get_approval 및 decide_purchase를 사용하세요. THIS_CASE는 답변 범위이며 향후 구매 자동 승인 권한이 아닙니다. 작업 재개 예약은 완료가 아닙니다. 자동 재시도 금지.",
    inputSchema: schema({ attentionRequestId: identifierSchema, answer: { type: "string", minLength: 1, maxLength: 8000 }, expectedVersion: versionSchema, scope: { type: "string", enum: ["THIS_ACTION", "THIS_CASE"] }, requestKey: keySchema }, ["attentionRequestId", "answer", "expectedVersion", "scope"]),
    validate(a) { requireIdentifier(a.attentionRequestId, "attentionRequestId"); requireText(a.answer, "answer", 8000); requireVersion(a.expectedVersion); if (!["THIS_ACTION", "THIS_CASE"].includes(a.scope)) throw new Error("scope는 THIS_ACTION 또는 THIS_CASE여야 합니다."); },
    path: a => `/attention/${a.attentionRequestId}/answer`, body: ({ answer, expectedVersion, scope }) => ({ answer, expectedVersion, scope }),
    format: d => `주의 요청 ${show(d.attentionRequestId)}: ${show(d.status)}\nCase: ${show(d.caseRef)} / 작업: ${show(d.workItemRef)}\n답변: ${show(d.answer)}\n범위: ${show(d.scope)} / 버전: ${show(d.version)}\n재개: ${show(d.resume)}\n다음 행동: get_case로 작업 진행과 남은 의무를 확인하세요. 재개 예약은 업무 완료를 뜻하지 않습니다.` },
];
export async function callConversationTool(tool, args, api, requestKey) {
  rejectUnknown(args, Object.keys(tool.inputSchema.properties));
  tool.validate(args);
  const data = await api(tool.path(args), tool.write ? { method: "POST", headers: { "Content-Type": "application/json", "Idempotency-Key": requestKey }, body: JSON.stringify(tool.body(args)) } : undefined);
  return { content: [{ type: "text", text: tool.format(data) }], structuredContent: tool.write ? { ...data, requestKey } : data, ...(data.error ? { isError: true } : {}) };
}
