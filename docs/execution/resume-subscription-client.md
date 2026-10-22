# 기존 구독 native client 실행 경로

2026-10-22에 baseline `082c703e9cb8198c91ff1507b7bcaef442d31a3a`에서
구현했다. 사용자 Step 2의 client 실행 부분이며 전체 Task 인수가 아니다.
실모델 호출은 하지 않았다. 외부 메시지·push·PR·BTP 작업도 하지 않았다.

## R8 결정과 비용 경계

사용자는 기존 Codex 또는 Claude 구독 인증만 허용했다. 이를
`verification/native-client/subscription-approval.json`에 기록하고 코드의
SHA-256 pin으로 변경을 거부한다. 선택한 실행 경로는 설치된
`codex-cli 0.163.0`, `gpt-6-astra`, `ultrafast`, reasoning `high`다.
증분 API 비용·credit 구매 승인 한도는 USD 0이며, 구독 사용량의 금액을
0으로 추정한다는 뜻이 아니다. 업무 수용치 확정과 최종 평가는 남아 있다.

runner는 임시 CODEX_HOME의 생성된 설정만 사용한다. 기존 auth.json은
내용을 읽거나 복사하지 않고 symlink로 native client에 연결한다.
인증 해석·갱신은 Codex가 수행하며 임시 디렉터리는 종료 후 제거한다.
file 인증이 없으면 Keychain이나 다른 provider로 몰래 전환하지 않는다.
원래 config.toml, 환경의 API key·provider URL·proxy, plugin·MCP 설정을
가져오지 않는다. `forced_login_method=chatgpt`와 builtin openai provider를
고정하고 원격 model catalog의 exact model/tier를 확인한다.

`account/read`의 auth 종류와 `account/rateLimits/read`의 credit 가용성을
확인한다. email·account ID·인증 원문은 artifact에 저장하지 않는다.
usage-based plan, credit 가용/무제한/미관찰 상태는 실행을 거부한다.
credit을 구매·리셋·소비하도록 요청하는 API는 구현하지 않았다. 실행 시점에
정책을 다시 검사하므로 저장된 preflight를 계속 유효한 승인으로 쓰지 않는다.

## 구현과 연결

`AgentRunner.ActualClientPort`와 `ModelBindingRuntimeProvider`에
ServiceLoader 등록을 제공한다. 기본 실행은 비활성이고
`verification.nativeClient.enabled=true`와 실제 integration이 모두 필요하다.
독립 관찰 schema와 기존 ModelBindingRunner의 검사는 변경하지 않았다.

제품 구현은 다음 SPI 하나를 별도 등록한다.

`org.mulino.verification.nativeclient.NativeClientIntegration`

- `driver()`는 실제 fixture/API/DB adapter를 반환한다.
- `requireReady(caseId, turnId)`와 `Session.requireReady()`는 현재
  deterministic prerequisite, 인증된 context 원 wire, installation,
  독립 host·DB·최종 답변 extractor의 가용성을 확인한다. 누락이면 모델
  호출 전에 예외를 낸다. arbitrary approval JSON을 인정하는 경로가 아니다.
- `open()`은 actor payload를 신원으로 믿지 않고 실제 인증된 transport와
  context capture를 결합한다. caseId·oracle를 모델 입력에 넣지 않는다.
- `tools()`는 실제 public capability schema를 제공한다. case의 예상
  호출 목록을 만들어 주지 않는다. `Session.call()`은 native dynamic
  tool 요청을 실제 인증 transport로 보내고 Codex의
  `contentItems`/`success` 응답을 반환한다.
- `Session.observe()`는 capture의 원 final response와 실제 wire/DB를
  독립적으로 관찰하여 기존 StepResult/host/semantic contracts를 채운다.
  structured intent도 실제 모델 출력·tool 요청에서 관찰해야 한다.
  모델 self label을 DB/API 결과로 복사하지 않는다. 관찰 불가는
  NOT_IMPLEMENTED이며, 생성된 native capture는 계속 artifact로 남긴다.
- tool timeout은 결과 미확인이다. transport는 interruption/close를
  처리하고 이미 전송한 쓰기의 결과를 대조해야 한다. runner는 재시도하지
  않으며 API 효과가 없었다고 추정하지 않는다.

입력은 raw utterance, 서버가 인증한 permitted context, 실제로 실행한
이전 user/assistant 대화다. 미실행 이전 turn을 대화로 꾸미지 않는다.
fixture·title·requirementRefs·expectedIntent·oracle가 context에 섞이면
호출 전에 거부한다. actor 구조는 모델 prompt에 직렬화하지 않는다.

skill은 계획의 `agents/skills/<name>/` 여섯 package를 사용한다. 처음에
metadata만 넣고 `load_operational_skill` dynamic tool로 본문과 reference를
읽는다. 경로와 hash를 검증하며 실제 요청/반환 본문을 native transcript에
보존한다. 이는 명시적 client bridge loading이며 자동 native skill discovery의
성공으로 표시하지 않는다. global developer skills를 업무 package로 세지 않는다.

프로세스별 격리 workspace, 180초 전체 제한, bounded frame/capture,
stdout JSON-RPC, 오류 시 종료·자식 정리를 구현했다. 모델·tier mismatch,
알 수 없는 host 요청·builtin tool, 잘못된 thread/turn, 중복 call ID,
모호한 마지막 답변은 실패다. raw stderr와 임의 예외 원문은 저장하지 않는다.
정상 capture에는 명령·binary/config/prompt/tool/skill hash, process ID,
시간, native events, skill loads, token usage와 원 final response가 있다.
금액은 native가 제공하지 않으므로 null과 이유를 남긴다. exact 모델/tier는
app-server가 확인한 설정이며 별도의 provider 청구 영수증이라고 주장하지 않는다.

## 실행한 검증

Java `21.0.12.1`을 PATH에 사용했다.

```sh
./mvnw -B -ntp -f verification/harness/pom.xml \
  -Dtest=NativeClientContractTest,ModelBindingContractTest,ModelBindingResponseEvidenceTest,ModelBindingEvaluatorTest test
verification/model-binding/run native-preflight
```

첫 명령은 74개 tests에서 failures/errors/skipped 0으로 통과했다.
contract doubles와 기존 oracle/wire/최종 답변 검증이다.
실제 모델 평가가 아니다. 결과 요약은 `verification/native-client/checks.json`이다.
native-preflight는 `initialize`, `account/read`,
`account/rateLimits/read`, `model/list`, 빈 ephemeral `thread/start`만
사용한다. `turn/start`는 전송하지 않는다.

`verification/native-client/local-preflight.json`은 실제 설치 client의
안전한 관찰 결과다. exact model/tier, ChatGPT 인증, credit 가용 false,
dynamic tool schema의 thread/start 수락을 확인했다. `actualModelCalls=0`,
`runtimeStatus=NOT_RUN`, `turnStarted=false`를 유지한다. Claude Code
2.1.308도 설치·claude.ai 구독 로그인이 확인됐지만 대체 실행하지 않았다.
local schema 생성으로 확인한 명령·필드 근거는 같은 디렉터리의
`local-capabilities.json`에 남겼다.

## 남은 통합 blocker

1. 이 baseline에는 `agents/skills/`의 실제 업무 package가 없다.
   제품 담당이 여섯 package와 manifest를 제공해야 한다. loader는
   `RUNTIME_SKILL_MISSING`으로 차단하며 가짜 package를 만들지 않았다.
2. NativeClientIntegration의 실제 구현이 없다. authenticated context,
   captured MCP wire, independent physical rows, host process/extractor,
   final-response semantic observation을 연결해야 한다. 허용받은 소유
   범위 밖의 ActualAcceptanceDriver·backend·fixture installer는 수정하지 않았다.
3. coordinator가 deterministic prerequisite와 R8 업무 수용치를 결합한 뒤
   명시적으로 native client를 활성화하고 기존 UAT runner를 실행해야 한다.
   `./verify model --manifest`의 기존 NOT_RUN 보고를 client preflight만으로
   PASS로 바꾸지 않았다. 60건×3 평가·실제 tool 효과·실제 skill 해석은 NOT_RUN이다.

통합 후 명시적 실행 명령은 `verification/model-binding/run actual UAT`다.
이 명령만 native 실행을 활성화하고 60개 binding을 각3회 실행한다. 첫 실패나
NOT_RUN에서 추가 구독 소비를 중단하고 부분 보고서를 보존한다. 개별 native
capture가 usage 원천이며 집계가 없으면 null로 남긴다. 기존 RED runner는
실제 호출0을 전제로 하므로 실제 인수 실행에 사용하지 않는다.
최종 productGateComplete는 별도 전체 증거 통합 전까지 false다.
