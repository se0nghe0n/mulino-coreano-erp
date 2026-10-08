# V2·V3 경합 관찰 계약

V2 `split-commits-first`·`reserve-commits-first`와 V3 `hold-first`·
`dispatch-first`가 공유하는 계약이다. 실제 제품 adapter와 barrier는
아직 없으므로 이 사례들은 NOT_RUN이며 이 문서는 구현 전 계약이다.

## 왜 lock WAIT를 독립 관찰하는가

이전 판은 contender를 초기 read 뒤 멈추고 winner를 끝까지 commit한
다음 contender를 재개했다. 이 순서는 두 요청을 차례로 실행해도 같은
최종 값과 `STALE_REVISION`·`INSUFFICIENT_ELIGIBLE_QUANTITY`를 만든다.
hook이 초기 read 전에 멈추면 contender는 이미 commit된 상태를 처음
읽고 거부하므로 lock 뒤 재검증이 없는 구현도 통과했다. barrier ACK는
요청 parameter를 되돌려 줄 뿐이고 `transactions`·`locks` 원행은 제품이
쓴 기록이다. V8 lock 계약과 backend `FulfillmentPostgresTest.race`처럼
두 transaction이 같은 scope lock에서 실제로 부딪힌 사실을 별도
connection의 `pg_catalog`로 관찰해야 한다(계획 §4.2, harness-guide
"V2/V3/V7는 실제 두 거래의 ACK/commit·잠금/fence 증거").

## 순서

1. `contender` start: 초기 read를 실제로 실행한 뒤 scope lock 전에
   멈춘다. 지점은 V2 `AFTER_INITIAL_READ_BEFORE_SCOPE_LOCK`, V3
   hold-first의 출고 `AFTER_ELIGIBILITY_READ_BEFORE_SCOPE_FENCE`, V3
   dispatch-first의 QC 보류 `AFTER_INITIAL_READ_BEFORE_SCOPE_FENCE`다.
2. `reached` waitReached: contender ACK.
3. `winner` start: 같은 scope lock을 얻은 뒤 commit 전
   `AFTER_SCOPE_LOCK`에서 멈춘다.
4. `winner-reached` waitReached: winner ACK.
5. `resume`: contender만 재개한다. contender는 winner가 잡은 lock에서
   기다려야 한다.
6. `contender-waits` observe: 아래 독립 lock probe다. WAIT를 관찰하기
   전에는 winner를 놓지 않는다.
7. `winner-resume` → `winner-terminal` await → `terminal` await.
8. 이후 DB 관찰은 두 실제 transaction ID와 lock 재검증 trace를 읽는다.

모든 start는 await 하나로 끝난다. sleep이나 timeout 우연을 경합
PASS로 세지 않는다.

## 요청 측 arming

`testTransactionId`, `testParticipantId`, `testBarrierId`,
`testBarrierPoint`는 V8과 같은 top-level request 필드다. 업무 `slots`에
넣지 않으며 canonical intent hash와 멱등 payload에서 제외한다. 검증용
test profile에서만 받는다. 다른 profile에서 이 필드가 오면 효과0의
`REJECTED`·`TYPE_INVALID`여야 한다. DB transaction을 열어 둔 채
멈추게 하는 운영 경로는 서비스 거부 통로가 되기 때문이다.
`testTransactionId`는 참가자 label이며 실제 XID가 아니다.

barrier control의 `barrierId`·`participantId`·`transactionId`·`point`·
`state`는 요청과 ACK가 정확히 같아야 한다(harness-guide). 여기서
`transactionId`는 위 label이다.

## barrier ACK

`reached`와 `winner-reached`의 `data.database.transactionId`는 그
요청이 사용 중인 PostgreSQL transaction의 실제 식별자를 문자열로
반환한다. label을 복사하지 않는다. artifact에는 참가자, backend PID,
transaction ID, 실행 중인 command ID를 남긴다.

contender ACK는 멈추기 전 실제로 읽은 관점을 `data.initialRead`로
반환한다. V2와 V3 dispatch-first는 `scopeId`·`revision`(1), V3
hold-first는 `scopeId`·`eligibleQuantity`(20)·`unit`(BOX)이다. artifact에는
그 read의 bound SQL/CQN을 남긴다. 초기 read 없이 멈춘 hook은 이 값을
보일 수 없다.

## 독립 lock probe

`contender-waits`의 `lockProbe`는 쓰기나 lock 획득을 하지 않는다. 별도
read-only connection에서 `pg_catalog.pg_locks`,
`pg_catalog.pg_stat_activity`, `pg_catalog.pg_blocking_pids(pid)`를 읽고
최대30초 안에 waiter/holder를 관찰할 때까지 기다린다. timeout은 실제
위반이면 FAIL, connection·권한 등 adapter를 실행하지 못했으면
NOT_RUN이다. 빈 행이나 서버의 `waiting=true`를 WAIT로 대체하지 않는다.

`lockWaits`는 waiter·holder·scope 조합마다 한 행으로 정규화한다.
필드는 `scopeId`, waiter `transactionId`, holder
`blockingTransactionId`, waiter `granted=false`, holder
`holderGranted=true`, `waitEventType=Lock`, `source=pg_catalog`다.
holder backend는 `pg_blocking_pids(waiter_pid)`에 있어야 한다. lock
type·mode는 구현의 잠금 방식(advisory, transactionid, tuple 등)에
따라 다르므로 고정하지 않고 원 catalog 행을 artifact에 남긴다.
`databaseTransactions`는 같은 catalog snapshot에서 holder
`OPEN·HOLDING_SCOPE_LOCK`, waiter `OPEN·WAITING_SCOPE_LOCK`이다.
WAIT 시점 waiter가 OPEN이므로 contender의 초기 read는 winner commit
전에 시작된 같은 transaction 안에 있다.

## lock 뒤 재검증

`lockRevalidations`는 실제 application transaction의 SQL/CQN trace에서
만든다. 필드는 `scopeId`, `transactionId`, `lockedRevision`,
`requestedRevision`, `point=AFTER_SCOPE_LOCK`, `result`다. 오류
응답에서 이 행을 만들지 않는다. V2 split-commits-first는 lock 뒤
revision2를 읽고 요청 revision1과 비교해 `STALE_REVISION`이다. V3
dispatch-first의 `lockedRevision`은 경합 직후 다시 조회한 segment
revision과 같다. V3 hold-first의 재검증 결과는
`INSUFFICIENT_ELIGIBLE_QUANTITY`다. 재검증이 요청 revision과 같으면
`result`는 `REVISION_CURRENT`다. `result`는 모든 행에 있으며 null이 아니다.

`transactions`와 `locks` 원행은 application 원장의 보조 증거로
유지한다. 경합 PASS의 근거는 위 독립 관찰과 재검증이다.

## V2 reserve-commits-first의 두 허용 결과

reserve가 먼저 commit한 뒤 contender 분할(요청 revision1)의 결과를
계획은 하나로 고정하지 않는다. 계획 §13.2 V2는 불변식(실물60·기존
의무40·신규 실행배분 최대20·부모 재소비0)만 정하고, §4.2는 lock 뒤
revision을 다시 읽어 충돌이면 새 의도로 몰래 실행하지 말고 conflict를
돌려주라고 한다. 예약이 A60의 revision을 올리는 구현은
CONFLICT/STALE_REVISION·효과0이 맞다. 올리지 않는 구현은 요청 revision이
여전히 최신이므로 두 배분을 한 번씩 자식으로 옮기는 APPLIED가 맞다.
backend `FulfillmentPostgresTest.v2ReserveFirst`는 후자다.

그래서 이 subcase는 결과를 고정하지 않고 아래 조건을 모두 요구한다.
harness에는 조건부 assertion이 없으므로 같은 조건을 두 경로에서 같은
값이 되는 비교로 표현했다.

1. 응답 outcome은 APPLIED 또는 CONFLICT다(contracts/domain-vocabulary.json의 나머지 outcome 5개 notEquals).
2. 응답 outcome은 같은 거래의 감사 원행 outcome과 같다. `raced-db`
   observer는 `data.data.contenderOutcome`을 derivation
   `{rowPointer:/rawRows/audit, where:{commandIdempotencyKey:<contender key>},
   aggregate:single, field:outcome}`로 낸다(observe scope의
   `derivedContenderOutcome`).
3. 감사 원행이 CONFLICT인 경우의 `errorCode` 집합과 contender
   transaction의 `lockRevalidations` 중 `result=STALE_REVISION`인 행의
   `result` 집합이 같다. CONFLICT면 둘 다 `[STALE_REVISION]`, APPLIED면
   둘 다 빈 집합이다. 재검증이 STALE인데 적용하거나, 최신인데 충돌로
   거부하거나, 다른 코드로 거부하면 실패한다.
4. contender 감사가 CONFLICT인 경우와 뒤의 수렴 분할(현재 revision을
   getObject로 다시 읽어 보내는 explicit-fresh-split) 감사가 APPLIED인
   경우가 정확히 함께 성립한다(actorId 집합 비교). CONFLICT로 기록하고
   분할 효과를 남기면 수렴 분할이 막혀 실패한다.
5. 경합 직후와 최종 상태 모두 실행 배분은 ALLOC40·신규20이 각각 한
   번이고 active 실물은 60이다. 최종 active 실물은 자식 40·20뿐이고
   A60과 A60을 가리키는 active 배분은 없다.

API 응답 자체의 `/response/error/code`는 CONFLICT일 때만 존재하므로
조건부 비교가 없는 현재 harness로는 직접 고정하지 못한다. 감사 원행의
코드와 응답 outcome으로 묶었다. 남은 한계는 harness 소유자에게 조건부
assertion(예: outcome별 guard)을 요청한다.

## V2 actual50의 promiseCoverage

`promiseCoverage`는 약속 root별로 현재 실행 배분과 현재 부족 의무를
합친 read-only 관찰이다. 현재 EXECUTABLE allocation은
`coverageKind=EXECUTABLE_ALLOCATION`, `sourceId`=allocation ID다. 같은
약속 root에 연결된 현재 OPEN 부족 의무는
`coverageKind=SHORTAGE_OBLIGATION`, `sourceId`=obligation ID다. 각 행은
`promiseRootId`, `quantity`, `unit`을 가진다. 같은 원천을 두 번 세지
않으며 SQL·parameter·mapping version은 sourceEvidence에 남긴다.

`promiseCoverage`는 제품이 내보내는 projection이 아니다. 독립 observer가
같은 snapshot의 `allocations`·`obligations` 원행만으로 만드는 파생이다.
EXECUTABLE_ALLOCATION 행은 `allocations`에서 state=EXECUTABLE·active인
행을, SHORTAGE_OBLIGATION 행은 `obligations`에서 current=true·status=OPEN인
부족 의무 행을 promise root로 묶는다. quantity·unit은 원행 값을 그대로
복사하고 다시 계산하지 않는다. 제품 응답이나 API projection을 읽어
만들지 않는다. case는 이 파생에만 기대지 않도록 부족 의무 원행
(`obligations`의 id=정정 응답 obligationId)의 quantity·unit을 직접 읽어
`shortage-obligation-row-quantity`로 부족 행과 대조한다. 이 파생 규칙은
harness 공통 observer 계약(`verification/harness-guide.md` "독립 observer의
논리 원행 계약")에도 있다.

## 남은 일

test profile 밖 arming 거부의 negative subcase는 아직 없다. T20·V5·V6·V7의
다른 arming 표기를 이 계약으로 맞추는 일은 각 소유자의 작업이다.
