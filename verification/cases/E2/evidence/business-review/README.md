# V3·T20·E2 review 반례 수정 증거

거부나 no-op이 핵심 행동의 정상 수행처럼 보이지 않도록 기존 인수
계약을 보강했다. 제품 DB/API/MCP·실모델 인수는 NOT_RUN이다.

- V3 `actual-baseline-physical-rows`는 일반 QuantitySegment의 active
  실물을 읽는다. `SCOPE_FENCE` 조건은 locks assertion에 유지한다.
- T20 state/accept 입력은 같은 typed 발주 명령의 부족한 channel만
  보완한다. MANAGER 결정이 없으면 `WAITING_APPROVAL`,
  `APPROVAL_REQUIRED`와 `requiredRole=MANAGER`를 요구하며 구매 주문,
  proposal·승인·원장·outbox가 바뀌지 않아야 한다. 같은 proposal의 실제
  MANAGER 승인 뒤 새 effect key로만 주문·outbox 1회를 허용한다.
- T20 다른 주체는 writer와 같은 capability/grant를 가진 별도 신원이다.
  자기 state의 정상 continuation과 타인 state의
  `REQUEST_STATE_PRINCIPAL_MISMATCH`를 함께 검사한다.
- E2는 같은 QC20 restriction의 해제 직전 ACTIVE, 해제 응답 APPLIED와
  같은 identity, 해제 뒤 RELEASED·20 BOX를 독립 원행으로 검사한다.
  recall60의 같은 identity·LOT·ACTIVE·60 BOX와 출고0도 유지한다.

T20 구매 관련 7개 subcase는 T13의 typed command envelope와 맞췄다.
`APPROVE`, proposalRevision, 결정 시각·유효기간·소비 정책을 동일하게
사용한다. API 요청과 MCP tools/call의 arguments는 같은 업무 명령이며
MCP의 JSON-RPC·header·structuredContent만 transport wrapper다. 구매
업무 O1은 기존 scope에서 읽고 그 실제 goal version에 proposal을 연결한다.
승인된 전달의 local order/outbox 생성은 `ACCEPTED_PENDING_EXTERNAL`이며
외부 성공으로 단정하지 않는다.

`BusinessReviewAssertionTest`의 고정 관찰은 SELFTEST다. 실제 제품이나
승인 engine을 대신하지 않는다. 관찰을 정상값에서 wrong identity·수량·
unit·상태·결정자·hash·generic 오류·새 주문·출고로 바꿔 실제 선언된
AssertionEngine assertion이 거부하는지 검증한다.

최종 focused Maven은 60 tests PASS이며 own tests는 12개다. V3/T20/E2
schema와 모든 Korean feature selector도 통과했다. 선택한 실제 Gherkin
RED는 62 expected/discovered/started와 62 NOT_IMPLEMENTED assertion
실패, skip0, exit1이다. 이는 구현 전 계약 RED이며 제품 PASS가 아니다.

첫 focused 실행의 3 errors도 보존했다. SELFTEST hash 형식 오류를
수정하고 common의 strict purchaseOrderId identity 계약을 적용한 뒤
재검증했다. 첫 실행 작업 트리의 개별 입력 hash는 기록하지 못했다.
최종 입력 hash·baseline·dependency·명령·version·exit·artifact는
`summary.json`에 남겼다. 통합 branch의 전체 registry 검증은 coordinator의
후속 gate다.
