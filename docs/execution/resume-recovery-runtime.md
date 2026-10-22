# Step2 복구 runtime 후속 수정

baseline은 `5861058e1e5ca035e169ab2291848b07407a6122`다.
branch는 `fix/resume-recovery-runtime`이고 isolated worktree는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-6b07740b`다.
상위가 통합과 전체 gate를 판정한다. 재위임·push·PR·외부 메시지·
실모델·BTP 실행을 하지 않는다. 다른 worker 소유의 fixture installer,
T01 queries/facts, native client/model binding은 수정하지 않는다.

live catalog에서 inherited provider `codex`, model `gpt-6.1-sol`과
`serviceTier=ultrafast` 지원을 확인했다. catalog의 currentValue는
provider 기본값이며 이 child의 실제 tier 영수증은 아니다. 실제 child
설정은 상위의 명시적 dispatch metadata로 확인해야 한다.

## 기한과 고정 평가시점

계획 §5.1의 STATE_AT은 `evaluationAt`의 상태다. 이를 기한 직전으로
바꾸지 않는다. exclusive deadline은 그 기한에 새로 발생한 사실을
인정하지 않는 별도 cutoff다. 실제 평가 시점과 이 cutoff를 분리한다.

수정 전에는 `deadlineInclusive=false`의 `dueAt-1ns` context를 미래
판정 guard에도 써서, 이미 실제 시간이 dueAt에 도달했는데도 고정
`evaluationAt=dueAt`를 미래로 취급했다. 기한 전에 확인된10/목표20의
부족이 UNVERIFIED가 되어 `deadlineViolated=false`로 누락됐다.
새 PostgreSQL test로 이 실패를 재현했다.

이제 deadline의 사실 cutoff와 policy 평가 context는 유지하고,
STATE_AT의 미래 guard에만 실제 평가 horizon을 전달한다. 기한 전에
확인된 부족은 위반이고 기한에 추가된 사실은 exclusive 기한 충족에
쓰지 않는다. 실제 horizon 뒤의 evaluationAt는 여전히 UNVERIFIED다.
inclusive=true의 정확한 기한에서는 아직 위반이 아니고, 지나면
위반을 기록하는 기존 규칙도 유지한다. GoalInput schema나 고정
평가시점·quantity oracle는 바꾸지 않았다.

## 중지 중 clock과 process

clock의 상태를 다음과 같이 명시한다.

- 실행 중에는 HTTP200, profile=verification, authorityClock=true,
  요청과 응답 instant 일치를 모두 확인한 뒤 SERVER_VERIFIED다.
- 중지 중에는 host가 실제 STOPPED pid 상태를 확인하고
  `<pid-file>.clock.json`에 desired instant를 원자적으로 보존한다.
  결과는 PENDING_RESTART, authorityClockApplied=false,
  httpExecuted=false이며 server response는 없다.
- 다음 start/restart는 desired instant를 JVM 시작 인자로 준다.
  실제 서버 clock HTTP 응답까지 검증해야 clockReapplied를 기록한다.
  틀린 profile/instant/authority 응답은 예외이며 적용 성공이 아니다.
- 같은 fixture에서 역방향 clock 요청은 중지 중에도 거부한다.
  새 fixture 시작만 기존의 명시적 reset 계약을 사용한다.
- api/scheduler/worker role은 기존과 같이 한 application JVM이다.
  실행 중 start는 같은 instance에 붙고 새로운 JVM을 만들지 않는다.
  실패한 process command를 acknowledged=true로 기록하지 않는다.

## case 입력과 남은 계약 공백

T26 restore 세 case의 split 결과 참조를 공개된
`children[0].segmentId`로 고쳤다. index0의 수량8을 선택하며 수량이나
계보·복원·권한·negative oracle를 바꾸지 않는다.

safe-retry 네 case의 의도적 beforeCommit rollback에는 성공 response
revision이 없다. optional outer expectedRevision의 그 참조만 제거한다.
원 move의 expectedRevision=1과 현재 grant/fence/효과0·1 검증은 보존한다.
없는 commandId나 저장 원행을 합성하지 않는다. 현재 제품의
`retrySafeCommand=NO_HANDLER`와 rollback 뒤 commandId 부재는 별도
미완료 계약이다. 이 입력 수정으로 safe retry 구현 완료를 주장하지 않는다.

`author_recovery_runtime.py`가 이 일곱 참조 수정을 재현한다. V5 91개와
T26 710개의 기존 assertion을 유지하며 generator의 재실행은 변경0이다.

## 결정적 검사

Java21을 PATH 앞에 두고 backend에서 npm ci를 실행했다.

- backend: AssessmentPostgresTest14, GoalInputTest5,
  TypedPredicateEvaluatorTest10, 총29 tests, failure/error/skip0이다.
  `/tmp/recovery-runtime-backend-final2.log`와 backend JUnit report에 있다.
- harness: ActualStoppedClockTest3, ActualS2ClockAckTest2,
  BackendProcessControlsTest1, ActualWorkerTransportTest6, 총12 tests,
  failure/error/skip0이다. `/tmp/recovery-runtime-harness-final2.log`에 있다.
  실제 로컬 OS process와 HTTP를 썼지만 ontology business PASS는 아니다.
- Python process_host9와 scoped generator2 tests가 통과했다.
  `/tmp/recovery-runtime-python.log`에 process 결과를 남겼다.
- `./verify prepare` exit0이다. 실제 runtime PASS와 구별한다.
  `/tmp/recovery-runtime-prepare.log`에 결과가 있다.

초기 test 작성 중 같은 source key/hash와 definition version을 중복
만드는 setup 오류도 발생했다. 별도 source와 독립 fixture test로 고쳤다.
기한 부족 누락의 RED와 이 setup 오류를 구별한다.

## 실제 선택 실행

아래에 clean commit 기준 V5/T26 결과와 첫 실패를 추가한다.

### 첫 실행과 lifecycle 입력 보완

`e2a16bf2c0efa48dd9758d914dd3d96e8aed9870` clean source로
`./verify scenarios --actual verification/cases/V5/case.json
verification/cases/T26/case.json`을 실행했다. exit1이며 29개 시작/종료,
268 actions, PASS0 / FAIL25 / NOT_RUN4다. 첫 evidence는
`verification/harness/target/evidence/actual-scenarios-run-ce3f67ef-bd5f-4c3e-93b8-b10d97a1da8e/`
다. codeCommit/workingTreeDirty=false와 jar hash를 runner가 기록했다.

실패한 process control의 ACK를 정직하게 false로 바꾸자 기존 case의
반복 stop 오류가 먼저 드러났다. api stop이 한 JVM을 종료한 뒤 같은
JVM의 scheduler/worker stop은 실제로 실패한다. 이전 ACK=true가 이
실패를 숨겼다. 세 case의 여러 role stop을 한 application stop으로
합친다. V5의 중지 뒤 restart 요청은 start로 고친다. 첫 start가 새
JVM을 띄우고 후속 role start는 같은 instance에 붙는다. 의무·수량·
owner·negative assertion은 하나도 제거하지 않는다. 삭제한 것은
같은 JVM을 거듭 stop하던 중복 control actions뿐이다.

첫 실행에서 복원 세 case는 split child 참조를 통과해 NOT_RUN까지
진행했다. safe-retry 네 case는 예상대로 다음 계약 공백인
`original/response/commandId` 부재에서 멈췄다. rollback 뒤 원행·ID를
만드는 구현이나 safe retry handler는 이 수정 범위에 없다.

### 최종 실제 선택 실행

제품/입력 source `4d85b1f4a1ecd3f402791d4b197a5b6c9a5bb41a`의
clean snapshot에서 같은 V5/T26 명령을 다시 실행했다. exit1이며
29개 시작/종료, skipped0, 실제290 actions다. 최종 집계는
**PASS0 / FAIL25 / NOT_RUN4**이며 전체 gateComplete=false다.
createWork17/17, waitWork11/11은 APPLIED를 유지했다.

원본 evidence의 절대 경로는 다음과 같다.

`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-6b07740b/verification/harness/target/evidence/actual-scenarios-run-743e6ce8-e600-4766-9cfc-bf5258150352/`

- `scenarios.json`: 각 action/response, assertion, fixture hash와 실패다.
- `code-commit.txt`, `working-tree-status.txt`,
  `executed-jar-sha256.txt`: clean source와 실행 jar를 고정한다.
- `inventory.json`: 기존 inventory.py가 생성한 최초 실패 분류다.
  ADAPTER/PRODUCT/TEST 표시는 heuristic이며 원인 확정이 아니다.
  여기에 들어 있는 미선택773개는 이번29개의 NOT_RUN4와 구별한다.
- `container-cleanup.txt`: 이 run이 만든 disposable PostgreSQL만
  제거했다. 다른 worker의 container/process는 건드리지 않았다.
- 실행 flags는 partialFixtures=true, relaxWire=false다.
  전체 fixture 설치나 runtime 인수 완료를 주장하지 않는다.

V5 waiting-full-restart, T26 due-wait-db-rediscovery와
due-wait-autonomous-loop 세 case 모두 다음을 실제 관찰했다.

1. application stop 뒤 advance가 PENDING_RESTART이고
   authorityClockApplied=false, receipt의 httpExecuted=false다.
2. 첫 start는 새 instance를 띄우고 후속 role start는 같은 instance에
   ATTACHED_EXISTING으로 붙는다. clockReapplied는
   `2026-10-07T09:00:10Z`이고 실제 server observation도 같은 instant,
   profile=verification, authorityClock=true다.
3. 이전 ConnectException은29개 결과 전체에서0건이다.
   due-wait의 tick/terminal도 실제 실행됐다. 다만 독립 observer와
   다른 복구 경로의 미구현이 남아 업무 PASS는 아니다.

복원 세 case의 split은 APPLIED이고 후속 attachEvidence 요청에 실제
index0 child ID가 전달됐다. 더 이상 `/response/segmentId` 오류는 없다.
attachEvidence는 별도 VERSION_UNSUPPORTED로 HELD다. 미구현 backup/
observer 등도 남아 복원 인수는 NOT_RUN이며 이를 성공으로 바꾸지 않는다.

선택29개에서 inventory가 고른 첫 실패 묶음은 다음과 같다. 실제
명령의 앞선 HELD 등은 위 원본 action에도 보존한다.

| 묶음 | 수 | 남은 입력/구현 |
|---|---:|---|
| observer | 6 | queueMessages/operationalIssues/boundaryCauses/alerts/projections 매핑 |
| 예약 scope | 5 | BASE_ORDER_LINE 등 선행 관계와 Unavailable scope |
| 예약 slot | 5 | reserveQuantity의 workId 미지원 |
| 안전 재시도 | 4 | rollback 응답 commandId 부재와 retrySafeCommand 미구현 |
| orphan query | 2 | Unsupported query field |
| 구매 | 2 | proposePurchase의 Missing workId |
| worker | 3 | barrier transactionId 불일치, fault workerId/error 미지원 |
| queue responder | 1 | ACK_AND_DELETE_MESSAGE 미지원 |
| emergencyRepair | 1 | VERSION_UNSUPPORTED |

위 미구현은 이번 소유 범위를 넘어 수정하지 않았다. 특히 worker
fault를 단순 command fault로 바꾸거나 실제 crash/lease semantics를
가짜로 만들어 통과시키지 않았다. restore의 evaluator/blob negative,
수량·owner·효과 assertion도 유지했다.

## 최종 변경과 검증 요약

- `e2a16bf2`: 시간 경계, host desired clock와 실제 서버 검증,
  rollback revision/split child 입력, 관련 tests를 수정했다.
- `4d85b1f4`: 한 application의 중복 stop과 중지 뒤 start 입력을
  바로잡았다. source 수정 이후 제품 코드는 바뀌지 않았다.
- 후속 documentation commit은 이 실행 증거만 추가한다.
- 최종 Python은 process9 + generator3 + 기존 resume13, 총25 tests다.
  backend29 + harness12도 통과했다. prepare는 마지막 lifecycle 입력
  변경 뒤 다시 exit0이며 로그는 `/tmp/recovery-runtime-prepare-final.log`다.
- baseline과 최종 JSON의 assertion 배열을 직접 비교했다.
  V5 91개, T26 710개 모두 내용까지 완전히 동일하다.
- 최종 case hash: V5
  `c057ef7e197bf4dee9ee1b6fd33647835ecbe8082bb3e31be30e63e22dcc4b22`,
  T26
  `18c26bd8a1541b4e8ce3b81ffff17b30fba819c041ef65214ae2a4fa576a24de`다.
