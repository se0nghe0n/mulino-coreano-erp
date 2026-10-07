# 판매·인도 계약

Task는 새 온톨로지 전체이며 사용자 Step3과 S4는 ACTIVE다.
기준선은 `1387024a6c5176d3f663f41e30dd22c1b268faef`다.
판매 수정은 새 OrderRevision/Line을 발행하고 이전 line의 출고·인도·
송장 효과를 보존한다. 원장 쓰기는 inventory primitive만 수행한다.

PHYSICAL_DELIVERY의 원본 JSON은 deliveryEventId, dispatchId,
cargoScopeId, salesLineId, customerId, itemId, lotId, rangeRootId,
physicalScopeId(Observation ID), startQuantity, quantity, unit, placeId,
occurredAt을 포함한다. event payload가 동일 의미를 가져야 한다.
`recordDelivery`는 eventId를 가진 관측을 먼저 저장한다. canonical이
대조된 뒤 observationId와 canonicalOccurrenceId로 실제 인도를 기록한다.
미연결 보고는 물리·목표 효과0이고 대조 의무가 남는다.

실제 인도와 정당한 목표 기여는 별도 수량이다. 현재 recall/만료는
실제 인도를 지우지 않으며 위반 대응 책임을 만든다. 반품은 원 인도
누적량을 차감하지 않는다. 인도 정정은 새로운 DeliveryCorrection과
목표 재판정을 연결하고 역방향 창고 이동을 만들지 않는다.
