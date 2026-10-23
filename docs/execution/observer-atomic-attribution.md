# Observer allocation 원행의 atomic attribution

Task/Step은 OPEN이다. baseline은
`b507aa8a66c5f7cf967bb6607833425c86550512`이고 작업 branch는
`fix/observer-atomic-attribution-b507`이다. 작업 tree는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-8f21ea6b`다.
실제 runtime은 codex/gpt-6.1-sol, high, serviceTier=priority(Fast)다.
root·native Haiku frozen tree·controller를 수정하지 않는다.

## 실제 쓰기 원천과 유지한 의미

- `FulfillmentStockPrimitives.reserve:17`은 새 allocation에 생성 명령의
  commandId를 저장한다. `transition:19`는 state/revision만 바꾼다.
- `FulfillmentCommands.execute:46–49`는 replacement에서 옛 행을
  REPLACED로 바꾸고 predecessorId를 가진 새 행을 만든다. 새 응답에는
  `fulfillmentBasis.replacedAllocationId`가 남는다. release 응답에는
  실제 RELEASED 행과 `fulfillmentBasis.releasedAllocationId`가 남는다.
- `ApplicationCommands.apply:103–112`는 handler 실행과 CommandRecord의
  finish를 같은 transaction에서 처리한다. CommandRepository의
  begin/finish는 요청 원문, 결과 원문, createdAt/completedAt을 저장한다.
- `database/migrations/V8__commands.sql:46–55`의 terminal trigger는
  COMMITTED/REJECTED CommandRecord의 UPDATE/DELETE를 거부한다.

기존 allocation.commandId와 생성 귀속/idempotency 표시는 바꾸지 않는다.
새 lastCommandId를 최초로 이름을 언급한 명령이나 case/fixture key에서
찾지 않는다. 별도 API 호출, evaluator, oracle 계산도 하지 않는다.

## 물리적 증명과 명시적 한계

ScenarioObservationProjection의 allocationAttribution은 기존 read-only
REPEATABLE_READ connection에서 현재 allocation의 `xmin::text`와
`pg_snapshot_xmax(pg_current_snapshot())::text`를 직접 읽는다. 같은
조직의 현재 CommandRecords 중 같은 xmin을 가진 모든 행을 읽고,
state·시간·capability 필터를 적용하기 **전** 정확히 한 개인지 확인한다.
두 번째 명령이 미래 시간이거나 미완료여도 하나로 축소하지 않는다.

유일한 행은 COMMITTED이고 createdAt/completedAt이 knownAt와 asOf를
모두 넘지 않아야 한다. canonicalIntentJson의 실제 capability/target,
resultJson의 APPLIED/commandId/revision, 실제 allocation의 조직·ID·
segment·Work·order line·customer·수량·unit·range·생성 command를 대조한다.
replacement의 옛 행은 요청의 allocationId, predecessor/basis 관계,
expectedRevision+1과 실제 successor의 유지된 생성 관계로 검증한다.
후속 release가 successor의 state/revision/xmin을 바꾸어도 옛 tuple과
불변 replacement 결과를 과거 successor tuple로 보간하지 않는다.
release는 정확한 target, RELEASED 상태와 실제 revision 증가가 필요하다.

모든 증명이 성립할 때만 lastCommandId는 실제 CommandRecords.ID이고
transactionId는 실제 PostgreSQL xmin 문자열이다. atomicAttribution에는
`PROVEN_CURRENT_TUPLE`, `historyComplete=false`, 실제 tuple/snapshot 값과
시간 경계를 남긴다. 성립하지 않으면 두 필드를 생략하고 아래 결손을
남긴다. 기존 행의 상태·수량을 감추거나 기대값을 복사하지 않는다.

- `ALLOCATION_VERSION_NOT_UNIQUE`
- `XID_EPOCH_OR_AGE_UNSUPPORTED`
- `NO_COMMAND_IN_TUPLE_TRANSACTION`
- `AMBIGUOUS_COMMAND_TRANSACTION`
- `COMMAND_NOT_COMMITTED`
- `COMMAND_OUTSIDE_TIME_BOUNDARY`
- `COMMAND_EFFECT_DOES_NOT_PROVE_ALLOCATION_WRITE`

xmin은 현재 row version을 삽입한 32-bit transaction ID다. 장기 고유성을
가정하지 않고, cluster의 full snapshot xmax가 첫 10억 미만이고
`3 <= xmin < xmax`인 경우로 지원을 제한한다. epoch를 추정하거나 hash,
최소 UUID, synthetic transaction ID로 바꾸지 않는다. wrap 이후 cluster,
그 범위를 벗어난 오래된 cluster와 특수 XID(0/1/2)는 unsupported다.
서로 다른 subtransaction의 xmin이 다르면 같은 top-level transaction으로
합쳐 주지 않는다. 비교 범위는 동일 DB와 observer snapshot이다.
[PostgreSQL system columns](https://www.postgresql.org/docs/18/ddl-system-columns.html),
[transaction identifiers](https://www.postgresql.org/docs/18/transaction-id.html)
문서에 근거한다.

이는 전체 allocation 변경 이력이나 wall-clock commit timestamp가 아니다.
검증 PG는 track_commit_timestamp=off다. asOf/knownAt은 저장된 application
시계의 command 경계이며 PostgreSQL snapshot은 commit 가시성을 정한다.
현재 mutable 행의 과거 수량/상태를 복원하지 않는다. 직접 SQL 수정,
다중 command changeset, 명령 없는 sweeper, 지원하지 않는 capability,
관계/원문 누락에는 writer를 만들어 주지 않는다. 원 행의 physical xmin이
현재 CommandRecord와 달라진 경우도 명시적 결손이다.

## 원래 T17 assertion 경계

- `replacement-atomic-9`: 옛/새 allocation의 실제 lastCommandId 비교에
  필요한 원천을 제공한다.
- `replacement-atomic-10`: 두 현재 tuple의 native transactionId 비교에
  필요한 원천을 제공한다.
- `released-replacement-allocation-21`: observer 측 release writer/transaction
  원천만 제공한다. 이 assertion이 함께 읽는 command response의
  `/response/transactionId`는 현 FulfillmentCommands.response와
  ApplicationCommands가 제공하지 않는다. backend/transport는 이 Subtask
  소유권 밖이며 값 주입으로 보완하지 않는다. 이 원천 mismatch는 남는다.

native 재실행이나 위 assertion의 새 PASS를 주장하지 않는다. T17의 원래
257개 assertion과 모든 case/fixture/grant/oracle bytes를 그대로 유지한다.

## 집중 검사와 증거

PostgreSQL 18.6의 소유 container 하나에서 CSN-shaped 격리 schema로
observer를 검증했다. 원래 fixture installer나 제품 명령을 실행한 인수로
과장하지 않는다. 별도 검사는 원본 V8 terminal 함수/trigger를 그 schema에
그대로 설치해 불변 명령 원문의 UPDATE/DELETE 거부도 확인한다.

최종 집중 명령은 다음과 같다. Docker 주소와 JDBC URL은 소유 container의
실제 loopback port이고 원문·SQL·boundValues는 `.local`에 보존한다.

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 21) \
OBSERVER_PG_TEST=true OBSERVER_PG_URL="$OWNED_JDBC_URL" \
OBSERVER_TEST_EVIDENCE="$PWD/.local/observer-atomic-attribution/observations" \
./mvnw -f verification/harness/pom.xml \
-Dtest=AllocationAtomicAttributionPostgresTest,ObservationSourceRelationsPostgresTest,ScenarioObservationProjectionTest test
```

최종 66개는 새 atomic 21개, 기존 relation 37개, projection unit 8개이며
FAIL/ERROR/SKIP은0이고 exit0이다. 정상 replacement, 별도 후속 release,
다른 조직·다중 명령·미래 명령·원문/target/range/revision 불일치·무명령
수정·불변 record 변조, 미commit 가시성, RR snapshot 유지, cutoff와
실제 read-only 쓰기 거부를 포함한다. 완전한 observation JSON은 기존
acceptance-observation schema로 검증했다. 최초 집중 실행 19개 중
후속 release 검사1개가 실패한 로그와 수정 뒤 증거를 구별해 보존한다.

정확한 명령/exit/count/source/class/JAR/log hashes, 원래 case hash 비교,
container/schema cleanup은 `.local/observer-atomic-attribution/handoff.json`
및 개별 logs에 남긴다. 전체 backend/harness와 actual scenario/native는
NOT_RUN이다. 최종 통합과 실제 시나리오 인수는 primary가 소유한다.
