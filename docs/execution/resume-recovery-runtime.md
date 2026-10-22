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
