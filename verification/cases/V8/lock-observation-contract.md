# V8 실제 DB lock 관측 계약

두 요청을 순차 실행해도 최종 예약60과 `STALE_REVISION`이 나올 수
있다. 따라서 최종 값만으로 PostgreSQL 잠금을 인수하지 않는다.
`real-lock-two-transactions`와 `upgrade-revalidate-transactions`는 첫
transaction이 scope lock을 보유한 동안 두 번째 transaction을
`BEFORE_SCOPE_LOCK` barrier에서 먼저 재개한다. 독립 DB observer가
실제 WAIT를 확인한 뒤 첫 holder를 재개하고 두 terminal을 기다린다.

## 실제 관측과 identity

`testTransactionId`는 테스트 참가자를 식별하는 label이다.
`reserve40-reached`와 `reserve30-reached`의
`data.database.transactionId`는 각각 해당 요청이 사용 중인
PostgreSQL transaction의 실제 XID를 문자열로 반환해야 한다.
테스트 label을 복사해서는 안 된다. 각 ACK artifact에는 참가자,
실제 backend PID, transaction XID, 실행 중인 command ID와 scope lock
SQL의 바인딩을 남긴다. upgrade의 `lock-` prefix도 같은 계약이다.

`waiter-before-release`의 `lockProbe`는 쓰기나 잠금 획득을 수행하지
않는다. 독립 connection에서 `pg_catalog.pg_locks`,
`pg_catalog.pg_stat_activity`, `pg_catalog.pg_blocking_pids(pid)`를
읽는다. 실제 observer가 해당 holder/waiter를 관측할 때까지 최대30초
범위에서 기다린다. timeout은 실제 위반이면 FAIL, connection/권한 등
필요 adapter를 실행하지 못한 경우 NOT_RUN으로 남긴다. 빈 row나
서버의 `waiting=true`를 실제 WAIT로 대체하지 않는다.

`lockWaits`는 이 catalog query에서 정규화한 row다. waiter의
`transactionId`와 holder의 `blockingTransactionId`는 ACK의 실제 XID와
일치하고 서로 달라야 한다. waiter의 미획득 `transactionid` lock은
`ShareLock`, `granted=false`, `waitEventType=Lock`이다. 그 resource XID가
holder XID와 같고 holder backend가 `pg_blocking_pids(waiter_pid)`에
있어야 한다. holder가 같은 resource의 `ExclusiveLock`을 보유한
원 catalog row와 두 backend의 XID/PID를 artifact에 보존한다.

`scopeId`는 실제 command의 target/bound SQL trace와 catalog wait를
연결한 QuantitySegment ID다. unrelated advisory/table lock이나 다른
segment의 WAIT를 같은 scope lock으로 바꾸지 않는다.
`databaseTransactions`의 `OPEN`과 lock 상태는 같은 catalog snapshot의
실제 backend/transaction 관측을 정규화한다. API 응답의 예상 상태를
복사하지 않는다. 두 source의 `sourceEvidence`에는 read-only SQL,
바인딩, mapping version, complete row 범위와 artifact를 기록한다.
기존 observer의 snapshot/provenance 계약도 적용한다.

## release 뒤 재검증

첫 요청의 terminal은 실제 commit 뒤에 반환한다. 두 번째 terminal은
lock 해제 뒤 acquisition과 현재 revision reread를 거친 결과다.
`lockRevalidations`에는 실제 application transaction의 CQN/SQL trace로
확인한 target, transaction XID, 잠금 후 읽은 revision2, 요청 revision1,
`AFTER_SCOPE_LOCK`, `STALE_REVISION`을 남긴다. error 응답만으로 이
trace를 만들지 않는다. 최종 예약60, 실물60, 새 배분40 한 번과 기존
부족40의 owner/다음 행동/기한을 함께 확인한다.

## SELFTEST 범위

`V8LockChoreographySelftestTest`는 CaseRunner와 명시적으로 표시한
`CANNED_CONTRACT_SELFTEST` driver로 authored 순서를 실행한다. 기존의
serialized 순서, DB lock 없는 row, 다른 blocker, 옛 revision reread를
각각 주입하면 최종 수량/응답이 맞아도 FAIL이어야 한다. 이 결과는
harness 계약 검증이며 PostgreSQL·API·upgrade 제품 PASS가 아니다.
현재 실제 제품 adapter는 없으므로 해당 제품 경로는 NOT_RUN이다.
