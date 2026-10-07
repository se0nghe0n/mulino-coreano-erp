# S3 실수령 계약

baseline은 c6c910b이며 사용자 Step3의 S3 수령 Subtask다.
별도 branch step3/s3-receipt에서 수령 모듈·V19·수령 primitive만 쓴다.

receiveProvisional은 RECORD다. 기존 PHYSICAL_RECEIPT eventId와
rangeRootId·startQuantity·quantity·unit·itemId·placeId·workId·ownerId·
supervisorId·nextAction·nextCheckAt·occurredAt을 받는다. lotId·
purchaseLineId·transitSegmentId는 선택이다. 원천 접수와 대조 책임만
만들며 QuantitySegment를 생성하지 않는다. 식별 미확인은 적격0이다.

confirmReceipt는 COMMAND다. receiptId는 provisional observation ID다.
canonicalOccurrenceId·lotId를 받는다. 원문/claim/현재 정책/중복 대조를
통과한 PHYSICAL_RECEIPT만 허용한다. rangeRootId는 실제 구별 가능한
물량의 ID다. 같은 원천 key나 문서 hash로 실물 동일성을 대신하지 않는다.
서로 다른 출처의 동일60은 같은 canonical occurrence로 연결한다.
root별 구간 중복은 transaction fence와 DB trigger에서 거부한다.

최초 접수는 실물과 RECEIPT 원장을 만든다. transitSegmentId가 있으면
정확히 같은 item·LOT·수량·단위의 TRANSIT leaf를 retire하고 수령 leaf로
옮긴다. 기존 controlScope·혼합 불확실성·법적 소유·보관 주체를
보존한다. 최초 접수의 소유·보관 주체는 미확인으로 남기며 책임을
맡은 인간 owner로 대신하지 않는다. 부분 운송량은 먼저 split해야 한다. 동일 transaction에서
구매 기여·초과 대조 책임·판정 pending·감사·멱등 결과를 기록한다.
초과5는 별도 실물로 보존하며 주문100의 기여를105로 늘리지 않는다.

getReceipt/getReceipts와 Receipt/ReceiptObservation 명사는 같은
scope/snapshot/인가를 쓴다. cumulativeArrival은 실제 확정 수령을
한 번만 합산한다. 원천 철회는 과거 물리 원장을 고치지 않으며 현재
누적 판정을 UNKNOWN과 재대조 필요로 보인다.

검증 실행 증거는 별도 파일로 추가한다. 문서 작성이나 compilation을
PostgreSQL gateway 인수 PASS로 취급하지 않는다. 외부 공급자/기관,
유료 모델, BTP 실행은 이번 Subtask 범위에 없다.
