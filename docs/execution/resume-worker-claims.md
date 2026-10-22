# 실제 worker claim 인수 연결

2026-10-22의 baseline은 `082c703e9cb8198c91ff1507b7bcaef442d31a3a`다.
branch는 `fix/worker-claim-acceptance`, isolated worktree는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-017f189c`다.
이 기록은 전체 Step 2나 V7 인수 완료를 뜻하지 않는다.

## 변경 계약

`POST /__verification/runtime/worker-claims`를 추가한다.
`verification` profile, loopback remote address, 인증된 현재 identity와
membership을 모두 요구한다. 같은 조직에 존재하는 Work만 받는다.
입력은 `workId`, `capabilityId`, `commandId`, `workerId`, 정수
`ttlSeconds`(1–300)다. actor·조직·stable owner는 JWT로부터 서버가
해석하며 payload에서 받지 않는다.

실제 `RuntimeRepository.claim`의 transaction과 fencing token 발급을
재사용한다. 이 control은 READ actor의 부정 검사에도 실제 lease를
준비할 수 있다. command 권한을 부여하거나 명령을 실행하지 않는다.
현재 grant와 Work scope, lease 유효성, commit fencing은 기존
`WorkerCommandsRest`와 `ApplicationCommands.executeClaimed`가 검사한다.
중복 active lease는 HTTP 409이며, 잘못된 입력은 422,
다른 조직/신원은 403 또는 인증 경계의 401이다.

case는 다음 두 단계를 명시한다.

1. `control.type=workerClaim`, `operation=issue`와 명시적 actorRef,
   Work·capability·command key·worker·TTL로 실제 발급 endpoint를 호출한다.
2. 명령의 `harness.workerClaim`에
   `{"$result":{"actionId":"발급 action","pointer":"/response/claim"}}`
   을 둔다. runner가 해석한 참조를 worker envelope의 `claim`에 붙인다.

claim 값·성공·권한을 adapter가 합성하지 않는다. `$result` 없는 호출은
계속 claim 없이 전송한다. 기존 명시적 endpoint envelope도 보존하며,
두 곳에서 claim을 지정하면 거부한다. 비동기 start/await에도 실제
worker route를 연결한다. receipt는 원래 HTTP 응답과 요청을 보존한다.

`acceptance-case.schema.json`은 control 입력과 참조를 검증한다.
`author_worker_claims.py`가 C3/V4/V7의 worker action에 발급·참조를
연결하고 발급 성공 assertion을 추가한다. C3·V4 기존 generator에도
연결해 재생성 시 유실되지 않는다. 기존 business/read assertion을
삭제하거나 기대값을 완화하지 않는다.

C3 worker-waitWork에는 제품이 요구하는 `wait.target`을 추가한다.
V4 worker-work의 Work 계열 대조는 Work scope가 없는 createDraft에서
기존 Work를 대상으로 하는 waitWork로 바꾼다. 기존 효과0, APPLIED,
COMMITTED 1회 assertion은 유지한다. 같은 subcase에 COMMAND actor의
claim 누락·명시적 위조·실제 발급 후 시계 만료 거부를 추가한다.
lease 부정 검사는 grant 부정 검사와 구별하며, 유효 참조를 쓰는 READ
검사는 grant 검증에서 면제하지 않는다.

## 확인한 증거

Java 21.0.12.1, Maven wrapper, disposable PostgreSQL과 local generated
RSA key를 사용한다. 실제 모델, 배포, 외부 알림, push, PR은 실행하지
않는다. GPT-6 Astra로 실행한다. live catalog에 Astra Ultrafast 지원은
있지만 이 worker의 실제 tier 값은 노출되지 않는다. parent의 명시적
`serviceTier=ultrafast` dispatch 기록이 tier 확인 근거다.

- backend `WorkerCommandsRestPostgresTest,RuntimePostgresTest`: 17 tests,
  failures/errors/skipped 0이다. HTTP 발급과 DB 원행 일치, 정상 효과1회,
  유효 lease + READ 거부, 만료·위조·다른 actor·조직 거부를 확인한다.
- 새 commit 경합은 HTTP 발급 lease로 명령을 시작하고 실제
  `AFTER_AUTH_SCOPE_FENCE_ACQUIRED_BEFORE_EFFECT_COMMIT`에서 멈춘다.
  verification clock을 lease 만료 시점으로 전진한 뒤 재개하면
  FORBIDDEN이며 outbox와 COMMITTED 명령은 0이다.
- harness 전체: 542 tests, failures/errors/skipped 0이다. 별도 transport
  테스트의 응답은 전달 검증용이며 제품 성공 증거로 사용하지 않는다.
- Python worker authoring/contract 4 tests와 기존 request contract
  34 tests를 통과한다. C3/V4 generator 재생성과 binding hash를 검사한다.
- `./verify prepare`: PREPARED, preparationProblems 0, exit 0이다.
- `git diff --check`를 통과한다.

초기 실제 run의 control ACK 형식 누락을 수정했다. 두 번째 run에서
worker async route 미지원과 wait.target 누락을 발견해 수정했다.
세 번째 run
`verification/harness/target/evidence/actual-scenarios-run-99923727-2d85-4b44-8e22-69528066075c`
의 C3 worker-waitWork는 전체 assertion PASS다. READ actor는 유효 lease를
발급받아 FORBIDDEN, COMMAND actor는 APPLIED와 Work revision 2,
COMMITTED 1회를 보인다. 이 run은 V4 Work 대조 변경 전이다.

## 남은 원인과 소유권

V7의 두 인가 경합은 실제 worker endpoint에서
`INSUFFICIENT_ELIGIBLE_QUANTITY: Current exact sale permission denied`로
멈춘다. 기존 fixture의 `policies.qc/regulatory/customer=ALLOWED` 선언은
실제 DispositionBases·현재 규제 허용 원행이 아니다. 현재 installer는
그 원행을 설치하지 않으므로 인가 barrier까지 도달하지 않는다.
V7의 enqueue barrier `AFTER_ENQUEUE_COMMIT_BEFORE_EXECUTION_AUTH`도
제품이 보고하는 point가 아니다. 이를 성공/다른 barrier로 바꾸지 않는다.

C3/V4 재고 단독 명령은 실제 WORK scope가 없어 유효 claim으로도
worker scope 검증을 통과하지 못한다. C3 activateWork는 설치된 goal에
timezone 등이 없어 GOAL_SLOT_MISSING이다. V4 allocation은 현재 적격
근거가 없어 거부된다. 이 실패를 claim 부재로 숨기지 않는다.

claim 연결에는 ScenarioFixtureInstaller 변경이 필요하지 않다.
GoalInput/Assessment*/TypedPredicateEvaluator, V5/T26,
author_resume_contracts.py, ScenarioFixtureInstaller는 변경하지 않는다.
V7 전체 인수에는 명시적 품질·규제 fixture와 실제 설치 지원이 필요하다.
별도 helper를 추가한다면 fixture 설치 직후의 extension으로 통합하고,
기대값이 아닌 authored 사실과 원행 증거를 검증해야 한다.

최종 clean source commit 뒤 selected actual run을 추가로 수행해 아래에
결과를 기록한다. 전체 profile receipt·운영 인수는 발행하지 않는다.

## 최종 clean commit 실행

source commit은 `0ce1a7c9de4254e7105185ce0d0e7902e1f0d5fb`다.
2026-10-22 17:19:52 KST의 runner preRun과 shell inventory 모두
working tree가 깨끗했음을 기록한다. 선택 파일은 원본 case에서 해당
subcase를 그대로 추출한 ignored artifact이며 새 oracle를 만들지 않는다.

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
./verify scenarios --actual \
  verification/harness/target/worker-claims-selected/C3.json \
  verification/harness/target/worker-claims-selected/V4.json \
  verification/harness/target/worker-claims-selected/V7.json
```

| Subcase | 실제 결과 | 관찰 |
|---|---|---|
| C3 worker-waitWork | PASS, 32 assertions | 유효 lease + READ는 FORBIDDEN/효과0, COMMAND는 APPLIED/COMMITTED 1회다. |
| V4 worker-work | PASS, 31 assertions | COMMAND actor의 누락·위조·만료 claim은 FORBIDDEN/효과0, 유효 lease + READ도 거부하고 정상 대조는 APPLIED/COMMITTED 1회다. |
| V7 authorization-revoke-first | FAIL | lease 발급과 실제 worker HTTP 호출은 실행했다. 판매 적격 근거가 없어 출고가 거부되어 barrier에 도달하지 못했다. |
| V7 authorization-effect-first | FAIL | 같은 fixture 전제 부족으로 barrier에 도달하지 못했다. |

전체 선택 run은 exit 1이다. V7은 `dispatch-ready`에서
`Barrier ACK differs from requested state`로 중단되어 이후 인가 철회·
commit 경합과 assertion이 실행되지 않았다. 실제 worker response는
`INSUFFICIENT_ELIGIBLE_QUANTITY`, message는
`Current exact sale permission denied`다. V7 성공으로 보고하지 않는다.

최종 실행 artifact root는 다음과 같다.

`verification/harness/target/evidence/actual-scenarios-run-5de46844-d7df-44d7-b59e-5bd3087f7bc4/`

`scenarios.json`, `code-commit.txt`, `working-tree-status.txt`,
`executed-jar-sha256.txt`, `container-cleanup.txt`를 남긴다. raw HTTP와
독립 DB 관찰은 다음 디렉터리의 receipt에 연결된다.

`verification/harness/target/evidence/actual/82546bdc-5091-42d4-be70-738982927bd7/`

- 실행 JAR SHA-256:
  `c2d96bd1cda8df12a6e8325981f0df5befe3289cd5662ded38b4b76332f70f6b`
- C3/V4 Work fixture SHA-256:
  `40db91477845763404ba5e67346a39cfa1953adfdabeb3e58925058fee2d4298`
- V7 fixture SHA-256:
  `96937bac4f22284cd241bc764c10b122867302846a94d632bccd9409d1956ebb`

runner가 자신이 만든 backend process, PostgreSQL container와
credential 임시 디렉터리를 정리했다. 전체 C3/V4/V7 또는 전체 profile
인수와 coverage receipt는 이 부분 실행으로 닫지 않는다. baseline의
모든 기존 assertion ID와 op/source/baseline/expected/unit 계약을
비교했으며 삭제·변경은 0이다. V4의 Work 명령 변경에 맞춘 설명만
수정하고, 새 claim assertions을 추가했다.
