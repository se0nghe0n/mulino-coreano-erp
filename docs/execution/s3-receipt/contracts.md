# S3 실수령 계약

baseline은 c6c910b이며 사용자 Step3의 S3 수령 Subtask다.
별도 branch step3/s3-receipt에서 수령 모듈·V19·수령 primitive만 쓴다.

receiveProvisional은 RECORD다. 기존 PHYSICAL_RECEIPT eventId와
rangeRootId·startQuantity·quantity·unit·itemId·placeId·workId·ownerId·
supervisorId·nextAction·nextCheckAt·occurredAt을 받는다. lotId·
purchaseLineId·transitSegmentId는 선택이다. 원천 접수와 대조 책임만
만들며 QuantitySegment를 생성하지 않는다. 식별 미확인은 적격0이다.

confirmReceipt는 COMMAND다. receiptId는 provisional observation ID다.
canonicalOccurrenceId·lotId를 받고, 선택 slot receivingCustodianId를
받는다(아래 개정). 원문/claim/현재 정책/중복 대조를
통과한 PHYSICAL_RECEIPT만 허용한다. rangeRootId는 실제 구별 가능한
물량의 ID다. 같은 원천 key나 문서 hash로 실물 동일성을 대신하지 않는다.
서로 다른 출처의 동일60은 같은 canonical occurrence로 연결한다.
root별 구간 중복은 transaction fence와 DB trigger에서 거부한다.

최초 접수는 실물과 RECEIPT 원장을 만든다. transitSegmentId가 있으면
정확히 같은 item·LOT·수량·단위의 TRANSIT leaf를 retire하고 수령 leaf로
옮긴다. 기존 controlScope·혼합 불확실성·법적 소유·보관 주체를
보존한다. 최초 접수의 소유는 언제나 미확인으로 남긴다. 보관 주체도
아래 개정의 검증된 지명이 없으면 미확인이며, 책임을 맡은 인간 owner나
확인 명령의 실행 actor로 대신하지 않는다. 부분 운송량은 먼저 split해야 한다. 동일 transaction에서
구매 기여·초과 대조 책임·판정 pending·감사·멱등 결과를 기록한다.
초과5는 별도 실물로 보존하며 주문100의 기여를105로 늘리지 않는다.

getReceipt/getReceipts와 Receipt/ReceiptObservation 명사는 같은
scope/snapshot/인가를 쓴다. cumulativeArrival은 실제 확정 수령을
한 번만 합산한다. 원천 철회는 과거 물리 원장을 고치지 않으며 현재
누적 판정을 UNKNOWN과 재대조 필요로 보인다.

검증 실행 증거는 별도 파일로 추가한다. 문서 작성이나 compilation을
PostgreSQL gateway 인수 PASS로 취급하지 않는다. 외부 공급자/기관,
유료 모델, BTP 실행은 이번 Subtask 범위에 없다.

## 개정: 증거로 지명된 수령 보관 주체 (s4e-custody, 2026-10-08)

S4 이행(`FulfillmentCommands.requireWarehouse`)은 내부 보관 주체가
확인된 창고 segment만 예약·출고한다. 직접 수령의 보관 주체를 기록할
공개 명령이 없어 계획 §13 E1(W에서 수령한 60 중 30 판매·출고)에
도달할 수 없었다(`docs/execution/s4d-native/README.md`). 계획 §4.1은
QuantitySegment에 위치·보관자·owner를 따로 두고, §4.2는 "위치·보관자·
owner·위험 부담은 별도 관계이며 인계가 소유권 이전을 자동 발생시키지
않는다"고 정한다. 그래서 보관 주체는 검증된 증거로만 알게 하고 실행
actor에서 추론하지 않는다.

- confirmReceipt의 선택 slot `receivingCustodianId`는 직접 수령에만
  허용한다. transit 수령에 slot이 있으면 TYPE_INVALID로 거부하고,
  transit 수령은 기존대로 transit leaf의 보관 주체·소유를 보존한다.
- slot의 actor는 같은 조직의 HUMAN/AGENT이고 같은 TARGET·ITEM·PLACE·
  WORK에서 현재 confirmReceipt 권한을 가져야 한다. 모르는 actor는
  FORBIDDEN, 외부 actor·권한 없음·철회된 권한은 SCOPE_INELIGIBLE로
  거부하며 효과는 0이다.
- 그 canonical의 현재 검증 chain(원본 blob과 event payload) 중 하나
  이상이 같은 `receivingCustodianId`를 지명해야 한다. 원본과 payload가
  서로 다르거나 검증 출처끼리 다른 보관 주체를 지명하면 HELD
  EVIDENCE_CONFLICT, 아무도 지명하지 않거나 slot과 다르면 HELD
  EVIDENCE_UNVERIFIED다. 지명하지 않는 출처(예: 운송 증빙)는 중립이다.
- 이미 기록된 범위의 중복 출처가 slot으로 다른 보관 주체를 주장하면
  EVIDENCE_CONFLICT다.
- slot이 없으면 S3 동작 그대로 보관 주체·소유 모두 미확인이다.
  `ReceiptGatewayPostgresTest`의 기존 null assertion은 유지한다.
- schema 변경은 없다. 기존 `QuantitySegments.custodianId`를 쓴다.

검증: `ReceiptGatewayPostgresTest`의 새 4개 test와
`FulfillmentPostgresTest`의 미확인 보관 주체 예약 거부 test, 실행 기록은
`docs/execution/s4e-custody/README.md`다.
