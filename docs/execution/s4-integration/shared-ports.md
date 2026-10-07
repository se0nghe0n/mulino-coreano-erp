# S4 공통 거래 port 계약

기준선은 `1387024a6c5176d3f663f41e30dd22c1b268faef`다. 공통 port는
caller transaction에 참여하며 persisted 사실만 읽는다. organizationId로
대상을 제한하고 없는 대상과 다른 조직 대상은 FORBIDDEN으로 처리한다.
caller의 현재 capability와 WORK/ITEM/PLACE/TARGET grant 검증은 command
service가 실행 경계에서 수행한다. port 호출만으로 권한을 승격하지 않는다.

## 수량과 좌표

ID 필드는 UUID 문자열, quantity/price/startQuantity는 BigDecimal,
revision은 Integer, occurredAt/dueAt은 Instant다. 수량은 item base unit이며
root 기준 `[startQuantity,startQuantity+quantity)`의 반열린 범위다.
root는 물리 좌표의 ancestor segment다. descendant에서 사용할 때
PhysicalRanges.project의 genealogy source/target offset으로 변환한다.
식별 불가능·offset 누락은 허용 범위를 만들지 않는다.

- SalesOrderLinePort.requireLine은 ID, organizationId, orderId, revisionId,
  workId, customerId, itemId, quantity, unit, destinationId, dueAt,
  deliveryEndpoint, qualityTerms, packageTerms, price, currency, revision을
  반환한다. 남은 수량은 유효 주문 수량에서 현재 인정된 인도 기여량을
  한 번만 뺀다. 반품을 인도 미이행으로 되돌리지 않는다.
- DispatchCargoPort.requireDispatch는 ID, organizationId, workId,
  salesLineId, customerId, occurredAt, revision을 반환한다.
  requireCargoScope는 ID, dispatchId, organizationId, salesLineId, workId,
  customerId, itemId, lotId, rangeRootId, startQuantity, quantity, unit,
  sourceSegmentId, transitSegmentId를 반환한다. quantity는 실제 출고다.
- DeliveredCargoPort.requireDelivery는 ID, organizationId, dispatchId,
  cargoScopeId, salesLineId, workId, customerId, itemId, lotId, rangeRootId,
  startQuantity, quantity, unit, placeId, occurredAt, canonicalOccurrenceId,
  segmentId를 반환한다. quantity는 원 인도 관측이다. currentDeliveryQuantity는
  asOf/knownAt에 유효한 명시적 정정을 반영하되 반품을 빼지 않는다.
  원 출고량, 원 인도량, 정정 인도량, 반품량은 각각 별개다.
- ReturnQuantityPort.returnedQuantity는 해당 delivery의 실제 반품 receipt를
  canonical 정체성과 정확한 물리 범위로 중복 제거한 합이다.
- SalesDeliveryCreditPort.deliveryCredit는 lineId와 deliveryId를 대조하여
  quantity, unit, occurrenceId, referenceId를 반환한다. quantity는 해당
  주문의 현재 인정된 실제 인도 기여다. 허위·미연결 관측은 기여하지 않는다.
- SalesExecutionPort.recordExecutionEffect는 line의 확정 dispatch/invoice
  reference를 한 번 기록한다. 같은 reference의 다른 수량은 거부한다.
- DeliveryCorrectionPort.correctionImpact는 deliveryId와 현재 canonicalId를
  대조한다. 실제 원장을 새로 쓰지 않고 원 인도와 과거 판정을 보존한다.
  현재 부족 의무만 이어가며 RESOLVED/WAIVED 책임을 부활시키지 않는다.

## 정산 연결

SettlementTradeFacts의 scopeKind는 PURCHASE 또는 SALES다.
Purchase 가격·통화·supplier는 line의 proposalRevision과 일치하는
immutable ProposalRevision에서 읽는다. receipt contribution은
ReceiptCredits.contributedQuantity이며 초과 실제 수량을 주문 완료로
승격하지 않는다. sales contribution은 SalesDeliveryCreditPort를 쓴다.
INVOICED effect는 기존 purchase/sales 실행 port로 같은 transaction에
기록한다. 외부 은행 효과는 없다.

## 증거 연결

Evidence subjectKind는 고정 enum이고 handler의 SubjectBinding noun과
일치해야 한다. EvidenceSubjectPort가 설치되어 authoritative 대상 조회를
통과해야 original attach와 event record가 가능하다. provider 없이 enum만
추가된 타입은 fail-closed다. TradeEvidenceScopePort는 claim, event payload,
original bytes와 persisted exact scope를 함께 대조한다. source profile,
immutable hash, 현재 verification, knownAt/asOf를 기존 경로에서 검증한다.
