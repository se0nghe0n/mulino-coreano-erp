# 2026-10-23 출고의 실제 이동 ID

dispatchQuantity가 만든 QuantityMovements의 실제 ID를 응답 movementId와
effects.movementId로 반환한다. 같은 organization·command·source segment·
target segment와 kind=DISPATCH인 행 한 개만 허용한다. 원천이 없거나
여럿이면 transaction을 실패시킨다.

이동 ID는 출고 ID·allocation ID·운송 segment ID와 별개다. primitive는
기존 물리 이동과 Dispatches/CargoScopes를 기록한 뒤 같은 gateway
transaction에서 해당 이동을 읽는다. 응답용 map에만 ID를 추가하며
저장 schema·물리 수량·기존 판정·인가 규칙을 변경하지 않는다.

## 검증

실제 PostgreSQL의 부분20과 전체100 출고에서 기존 응답의 이동 ID 누락을
2 FAIL로 재현했다. 보완 후 관련8개가 실패·오류·SKIP 없이 PASS했고
Maven exit는0이다. coordinator의 serviceTier는 default다.

- 응답 ID가 실제 DISPATCH 행을 가리키며 command·source·target·수량·
  단위·발생 시각·근거와 일치한다.
- 같은 요청 replay는 같은 이동·출고 ID를 반환하고 이동 한 개를 유지한다.
- 효과 기록 뒤 예외가 나면 이동·출고·cargo·물리 leaf 변경을 rollback한다.
  같은 요청으로 다시 실행하면 실제 이동 한 개를 만든다.
- 기존 부분 출고/인도 수량, 운송 장소 권한, 내부 원천 권한, rollback/
  replay/권한 회수 검사를 함께 통과했다.

수정 source와 실행 class hash·명령·JUnit·RED/GREEN 로그·cleanup proof는
`.local/resume-20261023/dispatch-movement-identity/`에 있다. 두 실행이 만든
container4개의 부재를 확인했다. 기존 T09 assertion·fixture는 바꾸지 않았다.

## 남은 범위

이는 집중 gateway/PG 검증이다. 전체 backend나 HTTP 시나리오 인수를
대신하지 않는다. 별도 실행 중인 Haiku Workflow는 db594723으로 고정돼
이 변경을 포함하지 않는다. 원래 T09의 이동 입력 oracle3개 통과나
전체 Task 완료를 주장하지 않는다. snapshot 입력 원천과 수령 기여,
감사 원장의 PENDING field 등 남은 계약을 별도로 처리한다.

Task와 현재 Step은 OPEN이다.
