# 현재 recovery 시각과 미래 이력의 경계

공유 서버의 T13 전송 검사가 이전 fixture의 미래 recovery 행을
처리하다 실패했다. 현재 시점에 아직 보이지 않는 행을 선택하지 않도록
고쳐 현재 due 업무와 외부 전송이 계속 진행된다. 미래 행과 append-only
이력, 과거로 이력을 다시 쓰지 못하는 DB trigger는 보존한다.

기준선은 `ce9afb325df609b129a721636a6d10e738fd83e9`다. coordinator가
별도 worktree `coordinator-recovery-current-ce9`, branch
`fix/recovery-current-history-boundary-20261024`에서 수정했다.
원본 T13 case·fixture·actor/grant/clock·oracle은 변경하지 않았다.

## 원인과 변경

a7f18cc8 전체 harness r2의 T13 supplier 두 ERROR와 delivery 두 FAIL을
원자료로 보존했다. supplier의 reconciliation 응답은
CONFLICT/STALE_EXECUTION이며 그 outbox는 PENDING·deliveryAttempts0이었다.
반환된 tick 응답의 SUBMITTED를 성공으로 해석하지 않고 scheduler가
보존한 terminal을 직접 읽었다. 해당 cycle은
FAILED/UncategorizedSQLException였다. PG 원로그는
`Recovery clock cannot move backwards`와 RecoverySchedules의 UPDATE를
기록했다. 이 자료는 r2의 자원을 정리하기 전에 읽기 전용으로 확보했다.

원인 SQL은 현재 due 또는 pendingAssessment인 READY 행을 모두
선택했다. 같은 DB의 다른 fixture에서 미래에 기록한 행도 포함해
과거 fixture clock으로 갱신하려다 trigger가 거부했다. 한 행의 실패가
전체 cycle을 중단해 현재 outbox의 첫 전송도 실행하지 못했다.

`RuntimeService.dueSchedules`는 한 번 읽은 ExecutionClock을 기준으로
trigger-owned RecoveryObservationHistory에 더 미래의 기록이 있는
schedule을 제외한다. 미리 보관된 미래 행을 과거 버전으로 재구성해
실행하지 않는다. 미래의 정확한 경계 시각이 되면 다시 선택할 수 있다.
기존 bounded query·row lock·SKIP LOCKED·인가·판정·책임 처리는 유지한다.
기록이 없는 legacy 행의 의미도 바꾸지 않는다.

lease 전용 RuntimePostgresTest의 V13 최소 schema에는 이 selector가
읽는 history 열 네 개만 추가했다. 전체 migration과 append-only trigger의
실제 chronology는 새 RecoveryClockBoundaryPostgresTest에서 검증한다.

## 실제 검증

`.local/recovery-clock-boundary/`가 이 worktree의 증거 기준 경로다.
최초 새 test의 ExecutionClock 구문 compile 실패는
`backend-focused.log`에 보존했다. 익명 구현으로 교정한 뒤
RuntimePostgresTest7·RecoveryHistoryMigrationPostgresTest1·새 clock1,
합9개가 실패·오류·skip0으로 통과했다.

새 counterexample을 기준선 RuntimeService로 되돌려 별도 실행하자
현재 하나만 선택해야 할 때 미래 행까지 선택해1 FAIL·exit1을 재현했다.
`backend-red-reproduction.log`에 보존했다. 수정 source를 복원해 실제
HTTP/PG를 실행한 뒤 새 clock 검사1개를 다시 통과했고 최종9개 JUnit을
`backend-green-junit/`에 보존했다.

새 clock 검사는 전체 Flyway migration을 실제 PG에 적용한다. 현재
행과 미래 행을 설치해 다음을 확인한다.

- 현재·경계1ms 전에는 현재 due 행만 선택한다.
- 미래 행과 history 원문을 바꾸지 않는다.
- 직접 과거 시각으로 갱신하려 하면 기존 trigger가 계속 거부한다.
- 정확한 미래 경계에는 두 행을 선택하고 정상 완료를 기록한다.

실제 HTTP/PG probe에는 원 T13 supplier10·delivery10과 미래 이력을
함께 둔 새 회귀10, 합30개가 실패·오류·skip0, exit0으로 통과했다.
새 회귀는 기존 supplier test를 같은 assertion으로 상속하며 별도
synthetic 미래 schedule 한 개만 추가한다. 전후 row/history 원문이
동일함을 `http-pg/future-history-given.json`에서 대조했다.

실행 명령은 다음과 같다. scenario runner나 모델은 실행하지 않았다.

```sh
./mvnw -B -ntp -o -f backend/pom.xml \
  -Dtest=RecoveryClockBoundaryPostgresTest,RuntimePostgresTest,RecoveryHistoryMigrationPostgresTest \
  test
T13_PG_TEST=true \
T13_TEST_EVIDENCE="$PWD/.local/recovery-clock-boundary/http-pg" \
T17_PG_TEST_CLASS=T13SupplierSourceFactsPostgresTest,T13PurchaseDeliveryPostgresTest,T13RecoveryClockBoundaryPostgresTest \
python3 verification/cases/T17/run_source_pg_test.py
```

실제 runner 경로에 남은 T17 이름은 이름일 뿐 T17 검증을 주장하지 않는다.
실제 증거는 `.local/t17-source-pg-bee6daf0-5f73-4941-9e83-a60320b0b594/`다.
JDK21.0.12.1·Maven3.9.16·Node24.16.0·locked CDS와 고정 PG18.6 digest를
사용했고 OrbStack socket을 명시했다. Node24.19.0의 과거 platform
인수를 이 실행이 재현했다고 표시하지 않는다.

`proof.json`은 명령 결과·source hash·원문 경계·JUnit·정리를 연결한다.
로그에서 소유 container5개의 ID를 확인하고 모두 부재를 대조했다.
HTTP/PG runner의 서버·container·private/public key·blob 정리도 true다.
source와 artifact SHA-256은 `artifact-hashes.json`에 기록한다.
전체 backend/harness, Haiku Workflow, 실제 모델·skills·규제·BTP 인수는
이 집중 검사로 대신하지 않는다. Task·Step·S5·S6는 OPEN이다.
