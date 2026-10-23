# S5 실구독 client 실행 입력과 인계

2026-10-24 KST에 baseline
`a7f18cc8e7124b9fd1cc8bf811687ed7230a4981`을 조사했다.
이 문서는 실제 업무 모델을 호출하기 전의 준비 산출물이다.
Task·사용자 Step·S5·S6는 OPEN이며 실제 client/model/skills 업무 인수는
`NOT_RUN`이다. backend·harness·Haiku scripted SIT의 성공은 이 인수를
대체하지 않는다.

## 소유권과 증거 위치

작성 worktree는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-117b0795`이며
branch는 `docs/s5-native-client-inputs-a7f18cc8`이다. Task branch와 같은
baseline에서 시작했다. tracked 소유 파일은 이 문서 하나다.
나머지 산출물은 이 worktree의 ignored 경로
`.local/s5-native-client-readiness/{proof,brief,scripts}/`에만 둔다.
coordinator의 source freeze와 실행 자원을 건드리지 않는다.

`proof/commands.json`과 `proof/supplementary-commands.json`은 실제 argv,
시각, exit와 안전한 출력을 보존한다. `proof/source-inventory.json`은
조사한 현재 source·계약·corpus·package의 path/SHA-256/bytes를 보존한다.
`proof/SHA256SUMS`는 문서·brief·script·proof의 최종 hash 목록이다.
이 준비 JSON들은 runtime receipt나 PASS 증거가 아니다.

설계 철학·구현 계획 전체, AGENTS.md, 개발용 Agent 구현 skill과
`runtime-and-client.md`, 시나리오 skill과 관련 harness 증거 지침,
`native-client-fast.md`, 실제 실행 source를 기준으로 조사했다.
옛 ERP 코드·Git 이력은 읽거나 재사용하지 않았다.

## 현재 확인한 계정·설정 입력

| 입력 | 관찰과 실행 한계 |
|---|---|
| Codex executable | `/opt/homebrew/bin/codex --version`은 exit0, `codex-cli 0.163.0`이다. 현재 adapter의 exact version과 같다 |
| Codex 로그인 | `codex login status`는 exit0이며 ChatGPT 로그인을 보고했다. 현재 `CODEX_HOME`은 `/Users/se0nghe0n/.codex2`다. 해당 file auth가 존재한다. 기본 `.codex`의 auth 존재와 구별한다 |
| Claude executable | `/Users/se0nghe0n/.local/bin/claude --version`은 exit0, `2.1.309 (Claude Code)`다 |
| Claude 로그인 | `claude auth status --json`은 exit0, `loggedIn=true`, `authMethod=claude.ai`, `apiProvider=firstParty`, `subscriptionType=team`이다. identity 필드는 저장하지 않았다 |
| Java·Docker | 현재 Java는 `21.0.12.1`, Docker server는 `29.4.0`이며 읽기 전용 version 명령은 exit0이다. DB·서버는 시작하지 않았다 |
| 실행 환경 | `ActualConfiguration`이 요구하는 9개 환경변수는 이 worker에서 모두 미설정이다. API key와 Anthropic endpoint/token 환경변수도 존재 여부만 확인했으며 모두 미설정이다 |
| 로컬 빌드·인수 | 이 worktree에는 backend JAR, model-binding classpath, runtime-manifest가 없다. root의 진행 중 결과를 이 branch의 인수로 가져오지 않았다 |

로그인은 현재 사용 가능한 token quota·원격 model 접근·실제 소비 성공을
증명하지 않는다. 준비 명령이 시작한 업무 model turn은0이다.
account/auth를 갱신할
수 있는 native app-server preflight는 실행하지 않았다.
`NativeCodexClient.RuntimeHome`은 기존 auth를 symlink로 연결하며 native
client가 갱신할 수 있으므로 이 준비 작업의 읽기 전용 범위와 구별한다.

기존 root의
`.local/resume-20261023/native-client-fast/preflight.json`도 읽었다.
원본 SHA-256은
`d1cc423bd034191a6cfece1cf9c1759ed85af1e61bb5d30007a0899919fcc93d`다.
`proof/historical-fast-preflight.json`에 식별자를 제외한 관찰을 남겼다.
2026-10-23T14:54:46Z 당시 ChatGPT/promax, credit fallback false,
Sol/high/priority, dynamic tool schema 수락, `turnStarted=false`였다.
이는 과거 quota/billing 경계의 증거이며 현재 잔량 증거로 재사용하지 않는다.
token·구독 소비 금액·현재 잔량은 관찰이 없어 null로 남긴다.

`subscription-approval-fast-20261023.json`의 pin은
`6508a61c9772bf0c48d54dff14ac0ff606484c3aa4115fa9fb3985b24b36dec1`이다.
기존 구독을 허용하며 별도 API·credit 구매·fallback은 거부한다.
USD 0은 추가 API/구매 credit 비용 상한이며 구독 사용량 금액이0이라는
뜻이 아니다. BTP 계정·entitlement·배포 비용도 승인된 입력이 아니다.

## 모델과 실행 역할을 구별한다

`proof/live-catalog.json`은 이번 `orchestrator_capabilities({})`의 안전한
현재 결과다. catalog의 기본 선택값을 실제 실행값으로 주장하지 않는다.

| 역할 | 현재 exact ID와 설정 |
|---|---|
| 후속 수정 worker | T3 `codex_2`, `gpt-6.1-sol`, `reasoningEffort=xhigh`, `serviceTier=priority`를 명시한다. 현재 catalog가 둘을 지원한다 |
| 테스트 Workflow | T3 `claudeAgent`, `claude-haiku-5-5`에서 native dynamic Workflow를 사용한다. 준비 brief의 `effort=medium`은 catalog의 기본값에 맞춘 실행 제안이다. Haiku에는 Sol의 serviceTier를 보내지 않는다 |
| 현재 업무 client adapter | `NativeCodexClient`는 builtin `openai`, `gpt-6.1-sol`, `high`, `priority`, ChatGPT file auth와 fallback false를 고정한다. T3 provider 선택을 받는 adapter가 아니다 |
| Claude 업무 client | 현재 ServiceLoader에는 `NativeCodexClient`만 등록돼 있다. Haiku/Claude CLI를 업무 client로 실행하는 adapter는 없다. Haiku가 shell로 scripted SIT를 실행한 것은 업무 모델 인수가 아니다 |

수정 worker의 최신 xhigh 지정과 업무 client의 high pin은 다른 설정이다.
후속 실행 전 coordinator가 업무 client의 승인 pin을 유지할지 최신
effort 선택에 맞출지 결정 기록과 대조해야 한다. 이 문서는 high를
xhigh 실행으로 표시하지 않는다. 변경한다면 `NativeClientPolicy`,
`CodexAppServer`의 catalog 검사·turn effort, `RuntimeHome.CONFIG`,
승인 JSON/pin과 계약 tests를 함께 바꾸고 preflight를 새로 남겨야 한다.

`agents/runtime-package-manifest.json`의 validatedClients에는 과거
Ultrafast preflight가 남아 있다. 현재 실행 선택은 priority이며 과거
entry를 현재 승인으로 쓰지 않는다. 실제 재인수 결과로 새 entry를
기록해야 하며 과거 기록을 소급해 바꾸지 않는다.

## 실제 entrypoint와 profile의 범위

현재 real model entrypoint는 아래 하나다. 이번에는 실행하지 않았다.

```sh
verification/model-binding/run actual UAT
```

이 script는 corpus pin을 검사하고 harness를 package한 뒤
`verification.nativeClient.enabled=true`로 `ModelBindingActualMain`에
연결한다. 60 case를 각3회 실행하며 첫 FAIL/NOT_RUN에서 중단한다.
부분 선택·shard·repeat 변경 인자는 없다. 여러 Workflow agent가 이
명령을 각각 실행하면 전체 corpus를 중복 소비하므로 한 executor만
소유해야 한다. 모델 호출 수를180으로 단정하지 않는다. 계획은
180 attempts, 73 turns×3인219 user turns이며 retry도 별도 기록한다.

| 명령 | 실제 의미와 이번 상태 |
|---|---|
| `python3 -I verification/model-corpus/validate.py` | 실행 exit0, corpusIntegrity VALID,60 case, 실제 foreign/mixed19개다. 업무 인수는 NOT_RUN이다 |
| `python3 -I verification/native-client/validate-runtime-skills.py` | 실행 exit1, SLOT_CONTRACT_DRIFT다. 아래의 정확한 차이를 남겼다 |
| `verification/model-binding/run native-preflight` | 설치된 Codex app-server의 계정·catalog와 빈 ephemeral thread 설정을 검사한다. turn/start는 보내지 않는다. 이번에는 NOT_RUN이다 |
| `verification/model-binding/run prepare` | binding·schema·source 연결 준비다. 모델 실행이 아니며 이번에는 NOT_RUN이다 |
| `verification/model-binding/run actual UAT` | 명시적 구독 업무 모델 실행이다. 이번에는 NOT_RUN이다 |
| `NativeIntegrationMain readiness` | source/config와 blocker를 보고하고 현재 exit2로 끝난다. readiness 성공이나 actual 모델 실행 entrypoint가 아니다 |
| `NativeIntegrationMain transport-sit` | 별도 S1 fixture의 실제 MCP/DB 진단이며 model calls0이다. 이번에는 NOT_RUN이다 |
| `./verify mcp --actual`, `./verify skills --actual` | 결정적 profile·receipt 경로다. 대상 client의 실제 발견·본문 loading·모델 의미 성공은 별도다. 이번에는 NOT_RUN이다 |
| `./verify model --manifest …` | manifest 검사 뒤 NOT_RUN exit2인 wrapper다. `--actual`은 exit3이다. actual UAT 대용으로 실행하지 않았다 |

`NativeDeterministicGate`는 현재 clean HEAD의 `runtime-manifest.json`에서
schema/contracts/scenarios/recovery/mcp/skills 여섯 profile PASS와 별도
`coverage/validate.py` exit0을 요구한다. source·commit이 바뀐 결과나
부분 shard PASS로 대체하지 않는다. 현재 worker에는 해당 manifest가 없다.

## 구현으로 해소할 정확한 공백

이 항목은 준비 중 발견한 implementation gap이며 adversarial review가
아니다. 아래 파일은 수정하지 않았다. 제품 규칙이나 oracle를 줄여서
entrypoint를 여는 변경은 제안하지 않는다.

1. **실제 package 계약의 drift를 고친다.**
   `agents/skills/ontology-work-coordinator/references/capabilities.json`의
   `createFollowup.slots`에는 공개
   `contracts/capability-slots.json`의 `sourceEventId`가 빠져 있다.
   해당 reference와 실제 공개 계약을 맞춘 뒤 package manifest의
   file/package/source hash를 현재 bytes로 재생성한다. 지금 source hash
   불일치도13개다. 전체 목록은 `proof/contract-differences.json`에 있다.
   단순 manifest refresh는 누락 slot 자체를 고치지 못한다.
2. **M fixture를 완전하게 설치한다.**
   `NativeProductIntegration.ProductDriver.installFixture()`는 typed alias만
   받고 `baseline.domainFacts`가 있으면 거부한다. 현재 M fixture60개는
   모두 UUID alias와 domainFacts를 가진다. `ScenarioFixtureInstaller`와
   별도 model fixture installer 경계에 실제 aliasMap/installationId,
   principalFacts/grantFacts, domainFacts→정의·원장·업무·의무·정책의
   명시적 mapping을 제공해야 한다. 기존 예상 Boolean을 DB에 seed하거나
   모르는 사실을 버리고 설치 성공으로 반환할 수 없다. all-of grant와
   각 fixture의 업무 시계·원천·승인 hash를 보존해야 한다.
3. **인증된 허용 context를 제공한다.**
   `ModelBindingRunner`는 `getObject`에
   `view=ACTOR_PERMITTED_BUSINESS_CONTEXT`와 installation/case/turn
   scope를 보낸다. `QueryRequests.FIELDS`에는 view가 없으며
   `ApplicationQueries`도 이 전용 view를 제공하지 않는다.
   이 두 파일과 공개 query schema/MCP schema에 권한 범위의 context read
   계약을 추가하거나 binding과 같은 의미의 명시적 조회를 연결해야 한다.
   fixture 전체·expectedIntent·oracle·정답 capability를 prompt에 넣는
   우회는 허용하지 않는다. 조회 snapshot/시간과 원 wire를 함께 반환한다.
4. **서버가 확인한 actor/grant를 원 wire에 연결한다.**
   `NativeMcpTransport`의 `authenticatedActorStatus`는 현재
   NOT_IMPLEMENTED다. `OntologyMcp`와 인증/application 경계의 실제
   resolver에서 actorId/issuer/subject/audience/organization/grant를
   correlation ID로 관찰하는 local verification hook이 필요하다.
   `captured-api.schema.json`의 scope·원 request/response·시각·주체와
   `ACTUAL_AUTHENTICATED_TRANSPORT` provenance를 채워야 한다.
   harness가 JWT를 서명했다는 사실로 서버 인가 관찰을 대신하지 않는다.
5. **독립 state/effect/obligation 관찰을 완성한다.**
   `NativePhysicalCatalog`, `NativePhysicalObserver`, `NativePhysicalFacts`,
   `NativeProjectionObserver`와 `ProductDriver.observe()`가 대상이다.
   raw physical rows와 simple-object projection 진단은 있지만 현재
   observe는 MODEL_BINDING_OBSERVATION_NOT_IMPLEMENTED를 반환한다.
   world/context projection의 독립 row recipe, semantic selector,
   immutable effect ID 전후 차이와 physicalScope 중복 배제,
   현재 의무·인간 owner·다음 행동을 연결해야 한다.
   `semantic-paths.json`의158 path와 관련 physical/observer/projection
   schema를 실제 SQL·column·MVCC·API revision으로 연결한다.
6. **host·마지막 답변의 독립 extractor를 연결한다.**
   `NativeHostObserver`는 원 app-server events와 종료를 읽는다.
   `NativeFinalResponseObserver`는 원 답변을 보존하지만 항상
   NATIVE_FINAL_RESPONSE_SEMANTICS_NOT_IMPLEMENTED를 반환한다.
   bounded Korean parser의 진단을 전체 자연어 수용으로 표시하지 않는다.
   실제 extractor process의 argv/version/hash/bytes/exit/scope와 host
   custody를 연결하고 CLAIMED_EFFECTS/COMPLETION/
   RESIDUAL_RESPONSIBILITY/조회 ANSWERED_READ_VALUES를 관찰해야 한다.
   불명확한 의미는 UNKNOWN으로 남긴다. 답변 self label이나 oracle를
   관찰값으로 복사하지 않는다.
7. **실행 readiness와 R8 집계를 연결한다.**
   `NativeProductIntegration.requireReady()`는 결정적 gate와 config 검사
   뒤 항상 NATIVE_INDEPENDENT_OBSERVATIONS_NOT_IMPLEMENTED를 던진다.
   위 연결의 현재 가용성과 scope를 검사하는 readiness로 바꾸어야 한다.
   `NativeClientPolicy.approval()`은 현재 승인 status를 AUTHORIZED로
   바꾸지만 businessAcceptanceThresholds는 UNCONFIRMED다.
   승인된 반복·수용치·설정의 확인과 실제 attempt/retry usage 집계를
   명시적으로 연결해야 한다. 미제공 금액을0으로 계산하지 않는다.
   model report를 coverage에 올리는 실행 receipt/index 연결도 별도다.

위 항목은 환경변수만 채워 해결되지 않는다. 현재 UAT 명령을 실행하면
첫 M fixture 설치 단계에서 중단하며 업무 모델 호출까지 도달하지 않는다.
이번에는 이를 증명하려고 모델 runner를 실행하지 않았다.

## 다음 한 번의 Haiku dynamic Workflow 실행 brief

ignored `brief/haiku-workflow.md`와 `scripts/later-owned-runtime.sh`에
실행 인계를 작성했다. shell script는 작성·syntax 검사만 했으며 DB,
서버, 모델을 실행하지 않았다. 다음 Workflow는 준비 점검·실행·증거
대조를 나누되 full UAT executor 하나가 서버/DB/key를 소유한다.
실제 native Workflow API는 Claude host가 제공하는 runtime schema를
사용한다. T3 `delegate_task`만 호출한 것을 native Workflow라고 부르지
않으며 `Workflow` 이름을 붙인 임의 script도 대용으로 쓰지 않는다.

후속 coordinator는 T3 `claudeAgent`/`claude-haiku-5-5`와 선택한 effort를
명시하고 실제 workflow/node model metadata를 보존한다. 현재 catalog는
model 존재만 증명한다. 해당 T3 provider가 사용하는 계정과 billing
경계는 실행 전 별도로 확인한다. CLI의 team 로그인만으로 router나
다른 provider의 과금 경계를 추정하지 않는다.

실행 순서는 다음과 같다. 아래 명령은 모두 저장소 root 기준이다.

```sh
python3 -I verification/model-corpus/validate.py
python3 -I verification/native-client/validate-runtime-skills.py
verification/model-binding/run prepare
python3 verification/coverage/validate.py \
  verification/harness/target/evidence/runtime-manifest.json --root .
verification/model-binding/run native-preflight
```

앞의 공백 수정, 현재 clean commit의 필수 결정적 profile 인수,
R8 실행 입력 확인 뒤에만 마지막 preflight와 actual UAT로 진행한다.
이번 기준선에서는 skills 검사부터 실패하므로 즉시 중단해야 한다.
별도 smoke를 추가한다면 반복과 구독 소비도 실행 계획에 합산한다.
전체 runner에는 smoke/case filter가 없으며 숨은 반복을 만들지 않는다.

실행 자원은 새 UUID run ID와 `mulino-s5-client-<runId>` container,
새 temp directory·RSA key·blob root·loopback backend PID가 소유한다.
DB image는 `verification/platform/versions.json`의 고정 digest를 읽는다.
현재 값은
`postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280`다.
DB/schema는 disposable installation이며 Flyway가 DDL을 소유한다.
root의 SIT DB·key·port·PID·blob을 재사용하지 않는다.

```sh
sh .local/s5-native-client-readiness/scripts/later-owned-runtime.sh
```

script는 필수 선행 입력을 확인하고 생성한 자원에만 연결한 뒤
`verification/model-binding/run actual UAT`를 호출한다. credentials는
환경과 private temp 파일로만 전달하며 trace를 켜지 않는다.
corpus fixture issuer/audience는
`https://fixture.invalid/identity`/`ontology-fixture`다.
`NativeMcpTransport`가 actor와 이 값을 대조하므로 scenarios runner의
다른 issuer/audience를 그대로 쓰지 않는다. cleanup은 소유한 backend
종료·container/volume 제거·temp key/blob 제거와 exit를 기록한다.
native Codex 임시 home/process 정리는 `RuntimeHome`/`CodexAppServer`가
소유한다. 기존 사용자 auth는 정리 대상이 아니다.

## client/tool/skills/업무 oracle의 필수 관찰

실행 report는 실제 source commit·JAR SHA·schema migration·definition/
evaluator·capability/input-output schema·prompt/tool/package version과
각 fixture hash를 고정한다. package 여섯 개와 discovery link12개는
현재 존재하지만 이것만으로 본문 loading을 증명하지 않는다.

1. 실제 인증 MCP tools/list의 schema와 원 wire/hash를 보존한다.
   native dynamic tool은 이 실제 목록에서만 구성한다. 최신 wire의
   metadata·header·MRTR 상태·새 RPC ID도 원 transcript로 관찰한다.
2. 실제 model input에는 허용 context·raw utterance·실행된 이전 대화와
   package metadata를 넣는다. `load_operational_skill` 요청/반환과
   SKILL.md·필요 reference 경로/hash/본문을 별도로 관찰한다.
   현재 bridge는 자동 native skill discovery 성공과 구별한다.
3. 실제 tool call→서버의 actor/grant→domain response→독립 DB/effect
   delta→최종 답변을 installation/turn/call ID와 시각으로 연결한다.
   두 진입점, 동의/다국어, 모호성, READ→쓰기 오해, 악성 문서,
   오래된 정의/skill, token 갱신 retry를 해당 oracle로 확인한다.
4. E1/M16은 구매 도착100·W 보유80·인도30·반품10·적격0과 남은
   QC/반품/정산 책임을 구별한다. M31/C3의 READ grant 조회는 업무
   쓰기0이다. M50은 같은 stable key의 실제 replay와 효과1회를,
   M57은 회수25+같은25 폐기가25이고 미확인25가 남음을 검사한다.
   모델 답변의 완료 주장도 실제 의무·owner와 대조한다.
5. `model-binding-actual.json`, 개별 native capture, 원 MCP wire,
   독립 DB/MVCC/projection/host/extractor artifact와 cleanup을 hash로
   묶는다. input/output·cache/reasoning token, p50/p95·확인질문 비율,
   attempt/retry와 금액 누락 이유를 남긴다. provider 미제공 metric은
   null이다. 업무 PASS와 비용/usage 전체 gate를 구별한다.

180 attempts/219 turns의 미실행 부분은 NOT_RUN으로 남긴다.
원 report/receipt/index와 현재 coverage validator가 확인하지 않은
범위를 문서나 준비 JSON으로 PASS로 만들지 않는다.

## 실제 외부 입력과 남은 NOT_RUN

현재 계정 로그인은 있어 새 계정 구매를 요청할 근거가 없다. 다음의
미확정 입력은 구현 공백과 별도로 남는다.

- R8의 실제 업무 수용치·구조화율 분모·반복/재시도 한도와 업무 client의
  exact 설정 기록이 필요하다. corpus의 실행 승인 metadata는 과거
  NOT_AUTHORIZED이고 구독 정책은 별도 AUTHORIZED_SUBSCRIPTION_POLICY다.
  둘을 합쳐 이미 업무 수용이 확정됐다고 해석하지 않는다.
- 현재 원격 model 접근·잔여 구독 quota·credit fallback·T3 Haiku의
  실제 계정 경계는 후속 실행 시 다시 확인해야 한다. 갱신이나 quota
  문제는 로그인 존재와 다른 상태다. 별도 API 결제를 열지 않는다.
- R4/R5/R6의 실제 규제 근거·운영 identity/승인자/정책/보존과 R7의
  BTP region/entitlement/cost cap은 운영 활성화 입력이다. 가상 local
  UAT fixture를 준비하는 데 실제 운영 값을 임의로 만들 필요는 없다.
  이 입력이 없는 운영·규제·BTP 인수는 NOT_RUN으로 유지한다.

이번 실행은 corpus 무결성 검사와 로그인/버전 조회·source 대조에
한정된다. 업무 model 호출·시나리오·Workflow·실배포·외부 메시지는0이다.
발견한 skills 실패는 그대로 인계하며 이 준비 문서로 S5를 닫지 않는다.

## 2026-10-24 package 계약 drift의 한정 수정

준비 commit `647660866b67f594f7fcedbe173b2ff1fa6efdfc` 뒤에 별도 branch
`fix/s5-runtime-package-contract-a7f18cc8`에서 수정했다. 제품 code의
기준선은 계속 `a7f18cc8`이며 root에는 아직 통합하지 않았다.
이번 thread의 실제 modelSelection은 `codex_2/gpt-6.1-sol`,
`reasoningEffort=xhigh`, `serviceTier=priority`로 확인했다.
`proof/package-fix/runtime-selection.json`에 실제 응답을 보존했다.
catalog의 지원값과 thread의 선택값을 각각 확인했으며 대체하지 않았다.

`ontology-work-coordinator/references/capabilities.json`에 공개 계약의
`createFollowup.sourceEventId` slot 한 개를 같은 순서로 추가했다.
slot 목록은 필수값 목록이 아니며, 기존 서버의 선택 입력을 reference에
빠짐없이 싣는 수정이다. 새로운 행동·권한·업무 의미를 추가하지 않았다.
이전 준비 단계의 exit1 로그와 manifest/reference bytes는 보존했다.

기존 `verification/native-client/validate-runtime-skills.py --refresh`를
그대로 사용해 manifest의 reference file/package/전체 skill hash와
source pin13개를 현재 bytes로 갱신했다. generator 코드 변경은 없다.
현재 generator는 version을 갱신하지 않으며 기존 hash 동기화에서도
version `1.0.0`을 유지했다. 이번에도 manifestVersion, aggregate version,
여섯 package version과 capabilitySemanticVersion은 모두 유지한다.
기존 공개 schema/capability 의미의 누락을 고치는 범위이므로 새 의미
버전을 발행하지 않는다. 실제 bytes 변경은 아래 SHA로 구별한다.

| 대상 | 수정 후 SHA-256 |
|---|---|
| coordinator `references/capabilities.json` | `80cbadb5d2e05c384d906612eb6cf512dd1a49cacddd553a34c5b10d9000e52d` |
| coordinator package | `3768d64002fe400accc9387e9541e3fcb9618a04b702492d42c2740f7eeeaf12` |
| 전체 skill bytes | `3966173e4bb7ec7ac03cbdd285042aeddafdc124963adecb225d3b1eb169786c` |
| runtime package manifest 파일 | `fa21fffaca3f77321e547c3acd66603ffaaa84429c2a401af0d17547c9224cca` |

다른 다섯 package entry, coordinator의 capability 목록·provider·metadata,
sourceBaseline, historical validatedClients와 승인 pin은 그대로다.
discovery symlink와 tracked mode도 바꾸지 않았다. source hash37개 중
13개만 달랐으며 나머지24개는 그대로다. 모든 source bytes는 a7과 같다.

### 13개 stale source pin의 원인과 현재 authoritative hash

각 pin이 가리키던 새 온톨로지 source bytes의 commit을 찾아 현재 a7
bytes와 비교했다. 아래는 이미 통합된 변경 때문에 pin이 낡은 원인이다.
이 변경들의 제품 동작을 이번에 새로 인수했다는 뜻은 아니다.
`proof/package-fix/source-drifts.json`에 전체 경로, 이전/현재 SHA,
pin 원본 commit, 관련 commit과 실제 source diff/hash를 보존했다.
`authoritative-sources.json`에는 현재 source37개의 SHA/bytes를 남겼다.
마지막 manifest 갱신 commit은 `08e2a7a5`이며 모든 pin이 그 commit의
source를 가리킨다고 추정하지 않았다. 옛 ERP 이력은 사용하지 않았다.

아래 Java 경로는 `backend/src/main/java/com/mulino/` 기준이다.

| source | pin 이후 반영된 원인과 관련 commit | a7 authoritative SHA-256 |
|---|---|---|
| `application/evaluation/AssessmentService.java` | closeWork 현재 권한, 현재 목표 부족 의무, 공통 capture와 불변 평가, capture 시점 대조, 종료 뒤 후속 책임을 연결했다. `1f0e9dd6`, `37cb940e`, `e8c66499`, `29e724f5`, `5bcb46b5`, `83639692`다 | `4db2bc66936b1ebde34691d6ab01d357fd45d52477693e57928e23ee81f17a8c` |
| `application/evidence/EvidenceQueries.java` | `CanonicalEvidenceVerification.verifiedForRead`로 조회 검증을 연결하고 실행 적격성의 내부 증거 검증과 분리했다. `e2fc92af`다 | `501fb4c4993e207ae5e026676e1fe4acffcd840f884718d84464005fa32c93e4` |
| `application/inventory/FulfillmentCommands.java` | 실행 목적의 증거 검증·fence를 사용하고 출고 응답에 실제 물리 이동 ID를 연결했다. `e2fc92af`, `b59ce369`다 | `235481c8b741cc334777a3da7910e2aed1754324c9385d742e1a0d5932b67213` |
| `application/inventory/InventoryQueries.java` | 품목 범위 적격성 조회와 조건별 evaluator 결과를 공개 조회에 연결했다. `905e04b8`, `fc58924f`다 | `ffc6006e744a821e037d0f5312470cfec5830267d86b181adae24adbcdf56e9e` |
| `application/quality/QualityEligibility.java` | 실행용 증거 검증, 품목별 물리 범위 조회, QC/규제/처분/고객 조건별 결과를 연결했다. `e2fc92af`, `905e04b8`, `fc58924f`다 | `17ef097867d771a2aa47bfafe69741bcb11f66acdc24b310c23e1eef1bea00df` |
| `application/responsibility/ResponsibilityCommands.java` | 현재 목표 부족 근거와 obligation 응답을 연결하고 이전·인수의 ITEM scope와 책임을 보존했다. `37cb940e`, `83639692`다 | `084c71f4d2aea47ebdd3e32b8fc0bcccfab17fbd0c658003f7358c37110a6688` |
| `application/trade/purchase/PurchaseCommands.java` | 외부 발주 전달 시 원 명령·확정 효과·현재 승인/권한을 재검증하는 `verifyDelivery`를 추가했다. `00cedd66`다 | `4a3f661358e601f2c6132b80caeed7d4a3374f4f7ae9a5696c84493740e5cf85` |
| `application/trade/receipt/ReceiptCommands.java` | 확정 수령과 중복 응답에 저장된 canonical occurrence ID를 연결하고 임시 접수와 구별했다. `f4852678`다 | `9ea8337968bfb5f5c31494b02b85384a5cead3755effe6fda9b5be32ffd5f637` |
| `application/trade/sales/DeliveryCommands.java` | 미확인 보고와 확정 재고 참조를 분리하고 pending external 응답, 원천 scope와 실제 인도의 인과 재평가를 연결했다. `e33e69d5`, `64054871`, `fc58924f`다 | `0b59f4f0a21f95da5fcbe92e31ec1c226daebdc13b4839617ac84052f0858b67` |
| `application/trade/sales/SalesQueries.java` | 미확인 인도 관측, 원천에 묶인 world ledger, 실제 인도 근거와 수량 조회를 연결했다. `e33e69d5`, `64b0cfb7`, `fc58924f`, `eaee3408`다 | `c0adc44566c2cf380ce62536d9eb4e2bc212256cc417ebb9f4dda6638c4fb1d3` |
| `application/work/WorkLifecycle.java` | 기여 구간의 source 목표를 고정하고 sourceEventId로 종료 뒤 후속 업무의 근거·멱등 연결을 지원했다. `4be9480d`, `83639692`다 | `8bcbdeebdfbd512dbd7b7e0d963c7d70622ee80b671da93628f3938083be192c` |
| `domain/work/read/WorkReadHandler.java` | source 목표 구간, 공통 capture의 관찰 시점과 실제 원천 world ledger의 업무 조회를 연결했다. `4be9480d`, `6c3310a3`, `64b0cfb7`다 | `e2dcd8506136e46d8de2c42d2aa11d52b33dc10ec3092145ff71dd4ea996a10f` |
| `contracts/capability-slots.json` | `createFollowup`의 허용 slot에 sourceEventId를 추가했으나 package reference가 뒤따르지 않았다. `83639692`다 | `eb6a2c56d5561066a8079f3e5439a3d933c6e0a6c3960072a0b5b1df804640a1` |

### 이번 집중 실행 증거

다음 명령만 이번 변경의 집중 검사로 실행했다. 실제 argv·exit·stdout/
stderr와 원 JUnit을 `proof/package-fix/`에 보존했다.

```sh
python3 -I verification/native-client/validate-runtime-skills.py --refresh
python3 -I verification/native-client/validate-runtime-skills.py
python3 -I verification/native-client/validate-runtime-skills.py --refresh
./mvnw -B -ntp -f verification/harness/pom.xml \
  '-Dtest=NativeClientContractTest#runtimePackagesHaveDirectDiscoveryLinksAndMatchingContentHashes' \
  test
```

세 Python 실행은 모두 exit0이며 STATIC_PACKAGE_DISCOVERY의
packages6/discoveryLinks12/files13을 확인했다. 두 번째 refresh가 만든
manifest는 첫 refresh와 byte 단위로 같다. Java runtime의 package 파일
검색·symlink·SHA 결합을 확인하는 기존 unit method 한 개도 exit0이며
tests1/failures0/errors0/skipped0이다. 모델/계정/서버를 호출하는 method는
선택하지 않았다. 원 generator·제품·contracts·fixtures·검증 source는
변경하지 않았다.

이 결과는 static package 계약 복구다. 본문/reference의 실제 loading,
업무 client/model/tool 효과, 전체 scenario·규제·배포는 계속 NOT_RUN이다.
앞 절의 준비 시점 실패를 소급해 PASS로 바꾸지 않는다. root 통합 뒤
source가 달라지면 root가 그 시점의 source pin을 다시 대조해야 한다.
Task·S5·S6는 OPEN이다.
