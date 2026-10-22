# 외부 worker 명령 제출 경계

2026-10-22에 사용자 Step 3의 외부 worker endpoint를 구현했다.
S5 채널 통합의 부분 작업이며 전체 Step 또는 시스템 인수가 아니다.

- baseline: `79a0cb6e9fe0e21a5296d4868b2b49d9c722a6e8`
- branch: `feat/worker-command-endpoint` (시작 시 `t3/ca7a818a`)
- worktree: `/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-ca7a818a`
- 통합 대상: parent의 `feat/ontology-implementation`이다.
- 실행 모델은 GPT-6 Astra다. 이 worker에서 service tier를 확인하거나
  변경하는 runtime control은 제공되지 않아 Ultrafast 적용은 확인하지
  못했다. 위임·모델 변경·외부 알림·push·PR은 수행하지 않았다.

## 공개 계약과 구현

`POST /api/runtime/worker/commands`는 기존 JWT security filter의
`anyRequest().authenticated()` 보호를 받는다. 요청은 다음 형태다.

```json
{
  "intent": { "capabilityId": "...", "...": "기존 명령 envelope" },
  "claim": { "claimId": "발급된 attempt UUID", "fencingToken": 1 }
}
```

`claimId`는 내부 claim의 `attemptId`, `fencingToken`은 `leaseToken`이다.
외부 입력으로 actor, organization, work, capability, key, leaseOwner를
받지 않는다. 인증된 issuer/subject/organization alias를 현재 identity와
membership에 대조하고 서버의 actor/stableRequestOwner로 claim을 찾는다.
저장된 scope와 leaseOwner를 복원한 뒤 기존 `fenceAndVerify`를 호출한다.

resolver transaction을 닫은 후 `ApplicationCommands.executeClaimed`를
호출한다. 기존 pipeline은 현재 grant·capability·scope·명령 key·Work와
claim을 대조하며 효과 transaction의 commit 경계에서 재검증한다.
사전 조회 이후 만료·교체돼도 이 재검증을 우회하지 않는다. 기존 내부
`RuntimeRecoveryExecution`과 intake 실행 경로는 변경하지 않았다.

claim 누락·위조·만료·다른 주체·다른 조직·현재 인가 실패는 HTTP 403과
`{outcome: REJECTED, error: {code: FORBIDDEN, ...}, effects: {}}`다.
내부 `STALE_EXECUTION`도 이 공개 경계에서는 같은 결과로 반환한다.
인증 자체가 없으면 기존 security filter가 HTTP 401로 거부한다.
그 밖의 domain outcome은 HTTP 200의 기존 명령 envelope로 보존한다.

endpoint는 claim 발급·heartbeat·finish를 새로 노출하지 않는다.
유효한 ACTIVE claim의 같은 요청 replay는 같은 저장 결과를 반환한다.
종료·만료된 claim은 replay에도 사용할 수 없다. 기존 trusted runtime이
발급한 claim과 그 원래 actor의 인증으로 제출해야 한다.

## 파일 소유권과 연결 과제

- `backend/src/main/java/com/mulino/adapters/WorkerCommandsRest.java`
- `backend/src/main/java/com/mulino/application/runtime/WorkerClaimResolver.java`
- `backend/src/main/java/com/mulino/domain/runtime/RuntimeRepository.java`
- `backend/src/test/java/com/mulino/runtime/WorkerCommandsRestPostgresTest.java`
- 이 실행 기록이다.

backend 외부의 schema 변경은 실행에 필수가 아니다. scenario/harness
owner는 worker 제출 adapter에서 위 route와 필드 이름을 사용하고,
V4 실제 쓰기 면 열거에 범용 worker dispatcher를 포함해야 한다.
해당 파일들은 이 worker의 소유 범위 밖이라 수정하지 않았다.

## 실행과 관찰

환경은 Java `21.0.12.1`, Maven wrapper `3.9.16`, Spring Boot `4.1.1`,
CAP Java `5.1.1`, Testcontainers `2.0.5`, OrbStack Docker `29.4.0`다.
PostgreSQL image는 다음 digest로 고정했다.

`postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280`

```sh
npm ci --prefix backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -f backend/pom.xml \
  -Dtest=WorkerCommandsRestPostgresTest,RuntimePostgresTest test
git diff --check
```

`npm ci`는 exit 0이다. 기존 dependency audit는 high 4·critical 4건을
보고했다. 이 endpoint 작업에서 dependency 변경은 하지 않았다.
최초 잘못된 `./backend/mvnw` 호출은 exit 127이었다. root `./mvnw`로
수정했다. 최초 테스트는 fixture 초기화 순서 오류로 5 errors였다.
명시적인 seed 호출 순서로 수정한 재실행은 5 tests/0 failures였다.
이후 identity·READ grant와 원행 불변 검증을 추가했다.

최종 결합 실행은 2026-10-22 17:06:09 KST에 exit 0으로 끝났다.
새 HTTP 테스트 7개와 기존 runtime 테스트 6개, 총 13개가 실패·오류·
skip 없이 통과했다. `git diff --check`도 exit 0이다. 기존 runtime
테스트는 두 worker 경합, commit 중 scope 회수 차단, stale claim의
효과/outbox rollback, outbox replay와 외부 결과 불명 복구를 검사한다.

실행 artifact는 worktree의 다음 파일과 local log다.

- `backend/target/surefire-reports/com.mulino.runtime.WorkerCommandsRestPostgresTest.txt`
- `backend/target/surefire-reports/com.mulino.runtime.RuntimePostgresTest.txt`
- `/tmp/worker-endpoint-final-tests.log`

위 결과는 baseline 위의 이 문서와 함께 commit한 source로 실행했다.
테스트 중 source 수정은 없었고, 이후 실행 결과 문서만 보완했다.

새 HTTP 테스트는 실제 서명 JWT, CAP, disposable PostgreSQL을 사용한다.
정상 요청과 replay에서 같은 결과·COMMITTED 명령 1개·outbox 1개를
관찰한다. claim 누락/빈 값/알 수 없는 ID/다른 token/소수 token/추가
identity 필드/조직·actor 불일치/만료 경계/재발급 뒤 옛 claim/다른 명령
key/철회 grant/READ grant/변경된 stable owner/철회 membership을 거부한다.
거부 전후 Work·의무·root·ExecutionAttempt 원행과 outbox·확정 명령 수를
대조한다. 정상 commit 이후 grant 철회는 기존 효과를 보존하고 replay
결과의 무권한 조회를 거부한다.

fixture source SHA-256:
`06ba0a0546f9729ef542926eb1a978df78ab389ee36435a573ea89ca86692c2d`

전체 D/T/C/V/E scenario, V4 노출 면 전체, model UAT, BTP·운영 identity
인수는 이 작업에서 `NOT_RUN`이다. verification profile의 synthetic
`fixtureExternalEffect` 검증을 실제 외부 전달 또는 전체 인수 PASS로
해석하지 않는다. coverage receipt를 발행하거나 manifest를 변경하지
않았다. 통합 후 coordinator가 필요한 전체 인수와 연결을 수행한다.
