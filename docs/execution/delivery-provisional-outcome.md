# 미확정 인도 관측의 공개 outcome

Task/Step은 OPEN이다. 이 변경은 구현 계획 §3.4의 기존 공개 outcome을
실제 provisional 응답에 적용한다. 새 capability·권한·업무 효과는 없다.

`DeliveryCommands.execute()`의 canonicalOccurrenceId 없는 분기는 실제
sales Observations PROVISIONAL과 DELIVERY_RECONCILIATION 의무를 같은
command transaction에 저장한다. 물리량·기여량은 모두0이다. 이 반환문의
APPLIED만 ACCEPTED_PENDING_EXTERNAL로 바꾼다. 이 분기는 기존 공개
recordDelivery와 recordObservedMovement가 함께 사용한다.
CommandOutcomes.ALL과 command-response.schema.json은 이미 이 값을
허용한다. NOT_APPLIED 집합에는 없으므로 기존 commit·감사·멱등 결과
저장 경로를 그대로 사용한다. 같은 key는 같은 실제 결과/ID를 replay한다.

canonicalOccurrenceId를 검증하는 별도 confirmation 반환문은 APPLIED를
유지한다. duty kind, source/canonical 검증, 현재 scope authority,
physical ledger, quantity, evaluation, query/observer, schema를 바꾸지 않는다.

기존 FulfillmentPostgresTest.run()은 모든 기존 APPLIED 검사를 그대로
유지한다. intakeDelivery의 provisional 호출 하나만 별도 helper에서
ACCEPTED_PENDING_EXTERNAL을 정확히 검사한다. 둘 중 하나를 허용하거나
APPLIED로 normalize하지 않는다. 기존 confirmation helper도 APPLIED다.

새 집중 PG methods는 같은 기존 fixture class에 둔다. 별도 class가
기존 static host를 빌리거나 JUnit tests를 상속하지 않는다. 실제 DB·
현재 IdentityAuthorization·PolicyCommandGuard·command transaction을
사용한다. 기존 fixture의 ExecutionClock/JwtDecoder test doubles만 유지한다.
기존 seed·grant·수량·clock은 수정하지 않는다.

검사는 dispatch 있음/없음에서 실제 EVENT/document/claim을 기록한 뒤
관측1·실제 열린 reconciliation duty1·COMMITTED command를 확인한다.
QuantitySegments/Movements/DeliveryTransfers/Deliveries/ExecutionEffects/
WorkContributions 원행은 provisional 전후 완전히 같다. 같은 key의
두 replay도 결과/ID가 같고 관측/의무를 추가하지 않는다. 실제 matching과
canonical link 뒤 confirmation은 APPLIED이며 실제20 이동·기여를 확인한다.
revoked assignment와 foreign grant scope는 FORBIDDEN/REJECTED이고 새
관측·의무·물리 효과0이다. 실제 duty를 기록한 뒤 failure를 발생시켜
transaction의 새 관측/root/duty/command/audit가 함께 rollback됨도 확인한다.

원래 T17 257 assertions/case/grant/quantity/clock 파일은 변경하지 않는다.
실제 native 시나리오·전체 backend/harness는 primary 소유이며 실행하지
않는다. 원로그·SQL·JUnit·source/class hash·cleanup은
`.local/delivery-provisional-outcome/`에 보존한다.

dispatch 유무의 원래 APPLIED 응답은 새 기대와 불일치하는 2 FAIL RED로
보존했다. 수정 후 새5개 및 기존 confirmation/claim 회귀3개의 집중
실행은8 PASS / 0 FAIL / 0 ERROR / 0 SKIP다. 실제 foreign 조직 scope와
confirmation 후 원 provisional key의 동일 pending 결과 replay를 강화한
새5개 재실행도5 PASS다. 중복 제외 최종 집중 검사는8개다.
실제 PG는 recordDelivery 경로를 실행했다. recordObservedMovement는
같은 non-confirmation 분기의 코드 수정 대상이며, 기존 fixture에 이
capability의 새 권한을 더해 별도 alias 인수를 만들지는 않았다.
원래 T17 native 인수는 계속 primary 소유의 NOT_RUN이다.
