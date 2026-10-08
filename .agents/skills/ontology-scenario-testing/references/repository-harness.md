# 저장소 harness와 실행 증거

이 파일은 `./verify`·Gherkin·증거가 저장소에서 실제로 하는 일만 적는다.
전체 계약은 [harness 가이드](../../../../verification/harness-guide.md),
[coverage README](../../../../verification/coverage/README.md)다.
명령이나 schema가 바뀌면 그 문서가 우선이다.

## 명령과 exit code

| 명령 | 의미 |
|---|---|
| `./verify validate <case.json>` | JSON Schema·semantic 검증. 제품 PASS 아님 |
| `./verify contract-red <case.json>` | 미구현 driver로 실행한 의미 있는 RED. 정상 exit1 |
| `./verify prepare` | case↔assertion↔Gherkin·registry·catalog 연결, 비정규 오류 pointer 거부, `caseAssetChecks` 11개와 감사 원행·runtimeProfile 검사(아래). 결과 `PREPARED`(제품 PASS 아님) |
| `./verify coverage [--index <file>]` | `model-binding/run prepare` 뒤 `assemble.py --check-preparation`을 실행하고 assembler exit code(0 PASS·1 FAIL·2 NOT_RUN·3 형식)를 그대로 돌려준다. 준비 보고만 만드는 명령이 아니다. 기본 index가 비어 있으면 runtime은 `NOT_RUN`(exit2) |
| `./verify scenarios --actual` | loopback 실제 backend와 disposable DB 필요(`ACTUAL_BASE_URL`, `ACTUAL_DISPOSABLE_DATABASE=true`, `DB_URL` 등). `--actual` 없으면 `NOT_RUN` exit2 |
| `./verify actual-s1`…`actual-s4` | `verification/actual/sN/run.sh`: 복사한 빌드, disposable PostgreSQL, native flow 실행. 커밋된 clean tree가 필요하고 s2–s4는 먼저 `python3 verification/actual/sN/build.py`로 custody를 만든다 |
| `./verify schema`·`contracts`·`scenarios`·`recovery`·`mcp`·`skills` | profile 실행. `--actual` 없이는 위반이 없어도 `NOT_RUN` exit2(위반이 있으면 exit1), `--actual`이 있어야 실제 driver로 실행하고 조건이 맞으면 coverage receipt를 낸다(아래 "실행 receipt producer") |
| `./verify model`·`deployment` | `--manifest <path>`로 실행 manifest를 검증한다. 승인 증거가 있어도 실행은 `NOT_RUN`(exit2). `--actual`은 exit3 |
| `./verify regulatory` | 준비 보고의 `runtimeGates`에 남는 필수 규제 증거다. 실제 runner가 없어 `NOT_RUN_GATED`(exit2) |
| `./verify harness` | harness selftest. 제품 증거가 아니다 |

exit0 harness/준비 성공, exit1 assertion·계약 실패, exit2 필수 경로
`NOT_RUN`, exit3 환경·형식·discovery 오류(의미 있는 RED가 아님).
`--actual`은 `harness/contract-red/prepare/coverage/validate/model/deployment`와
함께 쓸 수 없다. Java `Main`에는 `coverage` mode가 없다.

## 준비 gate의 case 자산 검사

현재 `caseAssetChecks`는 11개다. 정확한 argv·script와 추가 조건은
[PreparationAssetChecks.java](../../../../verification/harness/src/main/java/org/mulino/verification/PreparationAssetChecks.java)와
harness 가이드의 prepare 절을 따른다.

- `normative-catalog-lock`, `layer-routes`, `cases-b-invariants`, `vocabulary`.
- `t08-observation-bindings`, `v4-observation-bindings`,
  `v6-observation-bindings`, `v7-observation-bindings`: 생성기의 `--check`다.
- `observation-binding-stamps`: 모든 bindings의 case·catalog hash를 검사한다.
- `generators-reproduce`, `case-generators-reproduce`: 임시 복사본에서
  생성기 출력과 commit된 bytes를 비교한다. 저장소 출력물을 고치지 않는다.
  다만 MCP 생성기 검사는 T01/T20/T25의 파생 재현과 C3/T26의
  post-processor fixed point를 구별한다. 비소유 부분의 손 편집까지
  검출한다는 보장은 없다(Step 2 후속 소유).

script·Python 누락, stale binding, 생성기 drift는 준비 실패다.
`vocabulary`는 `verification/cases/check_vocabulary.py --check`로
[domain-vocabulary.json](../../../../contracts/domain-vocabulary.json)과
[audit-observation-fields.json](../../../../contracts/audit-observation-fields.json)을
대조한다. 공개되지 않은 outcome·오류 code/짝·의무 kind·감사 이름은
fail-closed다. 명시적 PENDING만 owner·근거가 있는 `KNOWN_OPEN`으로 남기며
쓰이지 않는 PENDING도 실패다. prepare의 `knownOpenGaps`와 coverage manifest의
같은 필드에 드러난다. assembler는 같은 `review()`를 읽고 `validate.py`는
현재 입력과 다시 대조한다. 검사 exit0이 gap 해소나 제품 PASS는 아니다.

## fixture 장소 종류와 transport 준비 검사

장소 종류의 기계 원본은
[fixture-place-kinds.json](../../../../contracts/fixture-place-kinds.json),
의미와 보관자 규칙은
[fixture-place-kinds.md](../../../../contracts/fixture-place-kinds.md)다.
fixture(baseRefs 포함)의 모든 Place alias에 kind를 적고
`baseline.places`의 kind도 alias와 같게 둔다.

- `INTERNAL_STORAGE`의 segment는 같은 조직의 Human/Agent alias를
  `custodianAlias`로 가진다. 내부 보관이 확인돼야 QC·규제·고객·처분
  조건으로 판매·출고 적격을 판단한다. Organization·Customer·Supplier·
  Manufacturer alias는 내부 보관자가 아니다.
- `TRANSIT`·`CUSTOMER`·`SUPPLIER`·`EXTERNAL_PORT`는 외부 장소이며
  적격은 확정 0이다. 수입 운송 화물의 내부 보관자는 확인 수령 때
  이어받지만 외부 장소에서 적격을 만들지 않는다.
- 어휘 밖 kind는 Place alias의
  `kindControl=UNRECOGNIZED_PLACE_KIND`로 선언한 반례만 쓴다.
  kind는 어휘 밖이고 `EXTERNAL_`로 시작하지 않아야 한다. 제품은 이를
  UNKNOWN·적격0으로 보고 confirmed eligible에 넣지 않는다. 확정 0인
  외부 장소와 구별한다. 제품이 모든 `EXTERNAL_*`를 외부로 보더라도
  fixture는 선언한 `EXTERNAL_PORT`만 쓴다.

`ContractValidator.placeKindProblems`는 kind 누락·미선언 어휘 밖 kind·
`baseline.places` 불일치·내부 보관자 없는 INTERNAL_STORAGE segment를
prepare 문제로 낸다. 옛 WAREHOUSE·INTERNAL_WAREHOUSE·PORT·TRANSPORT를
기본값이나 묵시 mapping으로 수선하지 않는다. C1
`unrecognized-place-kind`는 다른 조건이 같은 위탁40의
INTERNAL_STORAGE 적격40·ALLOWED와 WAREHOUSE 반례 적격0·UNKNOWN을
비교한다. T16 `confirmed-eligible-control`과 T17
`eligibility-positive-control`은 장소 오류만으로 적격0이 통과하지 않게
하는 양성 대조다. 이 fixture 계약과 selftest는 제품 실행 인수가 아니다.

Streamable HTTP의 `route=wire` 요청은
`Accept: application/json, text/event-stream`을 보내고 Origin을 생략한다.
Origin은 T20 `wire-bad-origin`의 허용 목록 밖 403 반례에만 둔다.
Accept 누락은 T20 `wire-missing-accept`의 406 반례다. 두 경우 모두
업무 효과0을 단언한다. `ContractValidator.wireTransportProblems`는
해당 action의 `/response/httpStatus`를 각각 403·406으로 고정한 반례만
예외로 받고, Accept 누락/변경·Origin 포함이나 두 위반을 함께 가진
요청은 거부한다. raw adapter가 header를 보충하거나 바꾸지 않는다.

Step 3 FixtureInstaller의 kind 기본값 제거·반례 그대로 설치, native
fixture의 kind/내부 보관자 전환은 cross-owner 요청이다. E1·T13 첫 수령의
`receivingCustodianId` slot도 receipt 계약과 함께 정해야 한다.
[round 6 기록](../../../../docs/execution/step2r-round6/README.md)의 남은
범위이며 이 skill 변경으로 구현됐다고 보고하지 않는다.

## case 한 개의 구성

`verification/cases/<ID>/`의 `case.json`, `fixture.json`(또는 `fixtures/`),
`scenario.feature`가 한 벌이다. `case.json`의 subcase 하나가 Scenario 하나다.
action은 JSON 선언 순서대로, assertion은 모두 Gherkin에 쓴다. case는
`verification/cases/registry.json`에 정확히 등록하고 `oracleRef`는 독립
catalog(`verification/requirements/mandatory-oracles.json`)의 항목에 연결한다.
case-local capability를 만들지 않는다. 공통 schema·runner·registry·catalog는 각 소유자가
고친다.

복사 원본을 구별한다.

- `verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/`는 문법
  selftest 예제다. `oracleRef`가 없고 `requirementRefs`가 `HARNESS-EXAMPLE`이며
  `canned-observations.json`이 따라붙는다. 제품 assertion은 `oracleRef`가 필수이므로
  이 case를 제품 ID로 바꾸면 검증을 통과하지 못한다. 문장 문법 확인에만 쓴다.
- 제품 case의 골격은 `verification/cases/E1/` 같은 기존 case의 `case.json`·
  fixture 구조(action·assertion·`oracleRef`·`requirementRefs`)를 따른다.
  기존 `scenario.feature`의 `그러면` 설명은 업무 설명 규칙이 생기기 전 것이어서
  assertion ID만 반복하는 곳이 많다. 그 설명 문장은 따라 하지 않는다.
- case ID는 T01–T26, C1–C5, V1–V8, E1, E2의 고정 41개뿐이다. 집합은
  `Main.prepare`의 expected 집합과 registry `expectedCases:41`이 강제한다.
  `acceptance-case.schema.json`의 `caseId` pattern은 형태만 거른다(`T00`·`T27`·
  `HARNESS-*`도 통과하므로 집합을 보장하지 않는다). 새 반례는 새 caseId가 아니라 이 41개
  중 맞는 case의 새 subcase로 추가한다. `registry.json`의 `subcaseIds`와
  `expectedSubcases`는 registry 소유자가, 해당 case는 case 소유자가 함께 고친다.
  ID 집합이나 registry 변경은 harness·registry 소유자에게 요청한다.

`./verify validate <case.json>` → `./verify contract-red <case.json>` →
`./verify prepare` 후에만 case가 있다고 보고한다.

## Gherkin 문법

`ScenarioGlue`의 제품 단계는 세 가지뿐이다. 다른 문장은 undefined step이다.

```gherkin
먼저 사례 파일 "<case.json 경로>"의 "<subcase id>"를 준비한다
만일 "<actorRef>" 역할이 "<action id>" 행동을 수행한다
그러면 "<assertion id>" assertion으로 "<업무 수량·단위·판정 설명>"를 확인한다
```

첫 단계는 준비 단계이고 action 단계는 선언 순서와 같다. 다음 두 가지는 기계가
검증하지 않으므로 review가 지킨다.

- 두 번째 인자(`humanExplanation`; `ScenarioGlue.assertion`의 둘째 parameter)는
  glue가 비교하지 않는다. 업무 독자가
  Gherkin만 읽어도 알도록 수량·단위·판정·owner를 쓴다
  (`"W 현재 보유 80 BOX = 100−30+10"`). assertion ID나 `"확인한다"`만 쓰지 않는다.
  이 값은 `case.json`의 `expected`·`unit`과 같아야 한다.
- `HARNESS-EXAMPLE`의 canned 단계(`고정 관찰 표본과…`)는 harness selftest 전용이다.

## NOT_RUN과 태그

`@pending`은 runner 기능이 아니다. 쓰지 않는다.

- 사례 Gherkin은 `case.json` 옆 `scenario.feature`를 file selector로 고르므로
  `@contract-red` 태그가 필요 없다. 이 태그는 HARNESS-EXAMPLE suite를 가르는
  JUnit 필터이며 NOT_RUN 표지가 아니다.
- 미구현 case는 `NOT_IMPLEMENTED` assertion RED(exit1)이고 skip은 금지(skip0)다.
  발견·시작·`NOT_IMPLEMENTED` 실패 수가 모두 subcase 수와 같아야 한다.
- `--actual` 실행에서 `NOT_IMPLEMENTED`만 실패한 scenario는 `NOT_RUN`으로
  기록된다. 위반이 하나라도 있으면 `FAIL`이다.
- profile 미실행은 `./verify` exit2와 `runtime-manifest.json`의 `NOT_RUN`이다.
- Main profile 보고(`verification/harness/target/evidence/<profile>.json`)는
  `discovered`·`started`·`completed`(그 profile에 선택된 subcase 수)와
  `skipped=0`, profile 자체의 `gateComplete`를 낸다. `gateComplete`는 `--actual`
  실제 driver로 선택된 모든 subcase가 발견·시작·완료되고 PASS일 때만 true다.
  `--actual`이 없거나 하나라도 `NOT_RUN`·`FAIL`이면 false다. 선행 profile은
  `prerequisiteRuntimeComplete=false`로 남고 assembler가 합성한다.

## 응답 오류 envelope과 의무 행 유효성

- 명령 오류 코드는 `/response/error/code`(`contracts/command-response.schema.json`의
  `/error/code`) 하나다. 응답 최상위 `errorCode`·`code`는 계약이 금지한다. raw MCP
  tool 결과는 `/response/body/result/structuredContent/error/code`, JSON-RPC
  protocol 오류(`/response/body/error/code`, 정수)는 다른 공식 envelope이라 허용한다.
  `./verify prepare`는 `/response/code`나 `/response/…/errorCode`·`error_code`
  pointer를 `not the canonical error envelope`로 거부한다. 새 assertion은 처음부터
  `/response/error/code`를 읽는다.
- MCP wire는 [s0-protocol.md](../../../../contracts/mcp/s0-protocol.md)의
  오류 우선순위를 따른다. JSON parse 실패(-32700), batch 배열·object가
  아닌 본문·형식 위반(-32600)을 mirrored header 누락/불일치(-32020)보다
  먼저 판정한다. `params` 형식 위반은 object·array가 아닌 값일 때다.
  object 안의 필수 `_meta` 누락이나 clientInfo·clientCapabilities 내용
  오류는 400 -32602다(T20 `wire-missing-meta`, `wire-invalid-client-info`,
  `wire-missing-capabilities`). `_meta` 누락은 비교할 version 값이 없어
  -32020이 아니다. header는 유효한 단일 envelope에서만 대조한다.
  Mcp-Name이 없는 V4 batch도 -32600이고, 유효한 단일 object의 T20
  method/name header 불일치는 -32020이다. 인증·Origin·Accept·Content-Type의
  transport 거부와 다른 오류 사이의 순서는 이 규칙이 정하지 않는다.
  OntologyMcp의 meta/clientInfo/name 오류 불일치는 round 6의 Step 3
  cross-owner 요청이며 새 wire 실행 인수는 `NOT_RUN`이다.
- `obligations`·`assessments` 원행의 `current`는 **행 유효성**(대체·정정되지 않은
  revision인가)이며 `status`와 독립이다. 해소된 의무도 `current=true`일 수 있다.
  "현재 열린 의무"는 `{"current":true,"status":"OPEN"}`처럼 둘을 함께 쓴다.
  `current`만으로 미해결을 뜻하게 쓰지 않는다.
- 감사의 논리 원행은 `audit`(명령)와 `queryAudit`(조회)다. field·presence와
  퇴역 이름은 `contracts/audit-observation-fields.json`, 관찰·파생 규칙은
  harness 가이드의 "독립 observer의 논리 원행 계약" 절을 따른다.
  `where`는 ALWAYS field만 쓴다. CONDITIONAL·PENDING은 ALWAYS field로
  고른 행에서 투영한다. `outcome`과 `errorCode`를 구별하고 코드 값을
  outcome으로 쓰지 않는다. 공개되지 않은 source·field·where는
  `ContractValidator.auditFieldProblems`의 준비 문제다. `queryAudit`의
  PENDING_PRODUCT와 PENDING field는 구현 요청이며 빈 행으로 인수를 대신하지 않는다.
  V2 `promiseCoverage`도 같은 절의 독립 원행 파생 규칙을 따르고 원행과 대조한다.

## Runtime profile과 host 관찰

정확한 요청·원행·validator 규칙은
[host-observation-guide.md](../../../../verification/host-observation-guide.md)와
[fixture schema](../../../../contracts/acceptance-fixture.schema.json)를 따른다.

- `baseline.runtimeProfile`은 `controlledTicks`·`pausedUntilTickControl`을
  함께 둔다. true/true는 harness tick만, false/false는 자율 loop다.
  profile 부재는 harness tick fixture다. 혼합 flag나 한 subcase에서
  harness tick·수동 watcher 혼용은 준비 문제다.
- 자율 loop의 scheduler·due-sweeper는 watcher group 전까지 기동하지
  않는다. installFixture 뒤 group 앞의 fault·seed·조회·관찰·clock 전진
  중에도 loop process가 실행 중이면 준비 실패다. api·worker는 group
  밖에서 시작하고, 장애 후 clock 전진 뒤의 재기동도 group 밖에서 한다.
  top-level `parallel`의 branch 0 첫 action에 수동 watcher를 두고,
  나머지 branch에는 loop process `start` 하나씩만 둔다. lot-expiry의
  scheduler와 due-sweeper도 별도 branch다. `restart`는 쓰지 않는다.
  이미 실행 중인 process의 start는 lifecycle 검사가 거부한다.
- `CaseRunner.controlRequest`는 수동 watcher(passiveWatch) 요청의
  parameter에 `observeFrom`을 넣어 host adapter로 보낸다. group watcher는
  어떤 branch도 제출하기 직전 잡은 harness 시각(group 결과의
  `data.observationBoundaryAt`과 같은 값), group 밖 반복 watcher는
  dispatch 직전의 harness 시각이다. case가 `observeFrom`을 직접 쓰면
  `ContractValidator.runtimeProfileProblems`가 prepare에서 거부한다.
  자연 tick 창은 `[observeFrom, observeFrom+observationWindowSeconds]`다.
  extractor는 지속 제출 기록에서 이 창 안의 행만 읽는다. `parallel`은
  barrier가 아니지만 늦게 뜬 watcher도 경계 뒤 제출을 읽고, 단독 반복
  watcher는 앞선 sweep 행을 자기 관찰로 보고하지 않는다.
  validator는 observeFrom 누락·group 경계와 불일치를 거부한다.
  watcher command는 observeFrom 전에 시작하거나 창 끝 뒤에 끝날 수
  없고, SUBMITTED의 submittedAt은 observeFrom부터 command 종료 사이이며
  1–30초 관찰 창 안이어야 한다.
  `autonomous-within-30s`의 baseline은 `<group>/data/observationBoundaryAt`,
  `autonomous-after-loop-start`는 loop start command의 startedAt 하한과
  이후 30초를 따로 본다. 창 끝 뒤에야 시작한 watcher는 NO_TASK·창 밖
  행으로 fail-closed다. 경계와 scheduler 시계는 LOCAL 동일 host를 전제한다.
- watcher 요청의 `trigger=OBSERVE_NEXT_NATURAL_TICK`,
  `observationWindowSeconds`(1–30), `triggeredBy=SCHEDULER_LOOP`는 설정이다.
  이를 tickScheduler/sweepDue `operationEvidence`에 echo하면 계약 실패다.
  제출 증거는 독립 extractor의 `rawRows.schedulerSubmissions`다.
  SUBMITTED identity는 관찰 창 안 가장 이른 제출 행과 일치해야 하고,
  NO_TASK는 행이 없어야 한다. T26은 그 행의 `submittedBy`를 단언한다.
  자연 tick 증거를 만들려고 harness tick·sweep·resumeWork를 실행하지 않는다.
  observeFrom 전달·검증은 harness 계약이며 실제 watcher host adapter와
  extractor 구현/인수는 Step 3 actual 소유, `NOT_RUN`이다.
- verifyCoverage의 PREPARATION `rawRows.input`에는 `codeCommit`,
  `workingTreeDirty`, `checkoutCommit`, `checkoutDirty`를 둔다.
  validator는 commit 형식(40/64자리 소문자 hex)·boolean과 묶인 준비 보고의
  앞 두 값 일치를 검사한다. 제품 실행(`requireActualHost=true`)에서는
  `checkoutMatchesHarness`가 뒤 두 값을 같은 저장소에서 harness가 읽은
  `git rev-parse HEAD`·`git status --porcelain`과 대조한다. selftest의
  고정 checkout은 이 제품 비교 대상이 아니다. 준비 보고와 현재 checkout의
  commit 일치·두 tree clean은 T25 assertion이 판정한다.
  낡거나 dirty인 보고를 준비 성공으로 숨기지 않는다. `assemble.py`는
  coverage manifest를 만들 뿐 T25 host의 input snapshot·currentExecution·
  mutatedInput·CURRENT_EXECUTION link를 내지 않는다. 그 출력과 checkout
  관찰은 Step 3 verifyCoverage actual adapter 소유이며 T25는 `NOT_RUN`이다.

## 증거 pipeline과 증거 class

Gherkin·`./verify prepare`는 `PREPARED`일 뿐 실행 PASS가 아니다.
아래 pipeline의 산출물에서 결과를 인용한다.

| 단계 | 산출물·도구 |
|---|---|
| 실제 실행 | `verification/actual/sN/run.sh`가 쓰는 `target/evidence/actual-sN-run-<uuid>/run-receipt.json`(안의 `recordType`은 `SN_DISPOSABLE_RUN_RECEIPT`, N은 1–4)과 `actual-sN-native.json`, 정리 결과와 artifact hash |
| 입력 index | `verification/coverage/runtime-evidence-index.json`([schema](../../../../verification/coverage/runtime-evidence-index.schema.json)) |
| coverage receipt | [execution-receipt.schema.json](../../../../verification/coverage/execution-receipt.schema.json): `evidenceClass=ACTUAL`, `codeCommit`, `executionIdentity`, `command`, `versions`, `reportArtifact`, `inputs`, `artifacts`. `--actual` profile 실행이 producer로 만든다(아래) |
| 조립·검증 | `python3 verification/coverage/assemble.py` → `verification/harness/target/evidence/runtime-manifest.json`([schema](../../../../verification/coverage/runtime-manifest.schema.json)); `python3 verification/coverage/validate.py` |
| 준비 기록 | `verification/harness-manifest.json`, `verification/cases/<ID>/evidence/preparation/manifest.json` |

native `run-receipt.json`은 소스·빌드·정리 custody를 보인다. 그것만으로 coverage의
ACTUAL receipt가 되지는 않는다. coordinator가 index·receipt로 연결·조립해야 한다.

증거 class는 coverage index schema에서 `ACTUAL`, `SELFTEST`,
`CONTRACT_RED`만 값이다. `STUB`·`LOGIC_REVIEW`는 index 값이 아니라 보고서에서
구별해 적는 범주이며 둘 다 제품 PASS가 아니다. 보고 규칙은 아래와 같다.

1. PASS는 `runtime-manifest.json`의 해당 item(case/subcase/profile)이나 전체
   `status`가 `PASS`이고 `validate.py`가 현재 입력에 대해 exit0(`VALID`)인
   경우만 쓴다. 저장 manifest가 현재 입력·재조립 결과와 어긋나면 `validate.py`가
   실패하므로 PASS로 인용하지 않는다. 반대로 `VALID`는 일관성일 뿐 PASS가
   아니다. `NOT_RUN` manifest도 `VALID`일 수 있으므로 `runtimeStatus`·`status`를
   함께 인용한다. 그렇지 않은 PASS 주장은 `NOT_RUN`이다.
2. `SELFTEST`·`CONTRACT_RED`·`canned-observations.json`·`./verify harness`는
   제품 PASS로 세지 않는다. 손으로 쓴 evidence JSON은 증거가 아니다.
3. 보고에는 command·exit code·codeCommit·receipt 경로·hash·status를 함께 인용한다.
4. manifest가 `NOT_RUN`이거나 현재 입력과 어긋나면 그 상태를 그대로 보고한다.

## 실행 receipt producer

`./verify <schema|contracts|scenarios|recovery|mcp|skills> --actual`은
`ExecutionReceiptProducer`로 coverage receipt를 만든다. 산출물은
`verification/harness/target/evidence/<profile>-receipt.json`, envelope
`receipts/<profile>-<runId>/`, 갱신된 `actual-runtime-evidence-index.json`이다.
`./verify model`·`deployment`에는 producer가 없다(승인된 별도 runner 필요).

producer의 전체 조건은 harness 가이드의 `--actual` 절과 coverage README를
따른다. 부족하면 receipt 대신 `COVERAGE_RECEIPT`의 `NOT_EMITTED` 이유가
나오고 assembler에서 해당 profile은 `NOT_RUN`이다.

- 전체 profile 실행이어야 한다. case 파일을 명시한 부분 실행은
  `explicitCaseSelection=true`, `gateComplete=false`이며 receipt를 만들지 않는다.
- 모든 case는 PRODUCT이고 실행 provenance의 label 값에 selftest/stub 등의
  표지가 없어야 한다. `capturedAt` 같은 data key는 표지 검사의 대상이 아니다.
- 첫 case 전 `preRun`과 실행 뒤의 같은 HEAD가 clean이어야 한다.
  `workingTreeObservations`로 남기며 필요한 version은 가이드의 환경변수로 받는다.
- `ACTUAL_BUILD_COMMIT` 일치는 선언이다.
  `buildIdentity.source=DECLARED_ACTUAL_BUILD_COMMIT`를 관찰된 build identity로
  보고하지 않는다. receipt의 profile·command도 index와 일치해야 한다.

receipt는 무결성·연결 증거이며 adapter 뒤 시스템이 진짜라는 attestation이
아니다. receipt가 있어도 PASS가 아니다. assembler가 assertion 기록(`op`·
`unit`·`where`·`baseline`·observed)을 캡처된 bytes로 다시 판정해 manifest status를
정한다. 현재 `ActualAcceptanceDriver`는 `api`·`fixture`·`db` adapter만 공급하므로
mcp·client·process·host가 필요한 subcase는 `NOT_RUN`이고, 따라서 그 profile의 `gateComplete`·manifest PASS를
주장하지 않는다. native `actual-sN`의 `run-receipt.json`은 case 결과가 아니라
custody 증거라 그대로 coverage receipt가 되지 않는다. 같은 disposable backend에
`./verify scenarios --actual`을 실행하는 연결은 Step 3 소유다
(`verification/coverage/README.md`). receipt·report를 손으로 만들지 않는다.

## 모든 쓰기 경로 열거(V4)

요구: V4 경로 집합은 고정 목록이 아니라 실행 중 시스템이 실제 노출한 면에서
만든다. OData/CAP service의 `$metadata` entity set·action·function, MCP
`server/discover`·`tools/list`, worker handler registry, 관리/actuator endpoint를
읽고, 열거 항목의 kind에 적용되는 probe class를 시도한다. `$batch`
changeset, deep insert, upsert, draft activation, nested navigation도 포함한다.
capability allowlist에 없는 쓰기 가능 항목은 FAIL이다. core entity는
노출하지 않거나 `@readonly`·
`@restrict`로 막고 READ grant 주체의 우회 효과가 0임을 전후 DB 관찰로 증명한다.
존재하지 않는 경로의 404만으로는 부족하다. 계획 §4.2·§13.2 V4는 "실제 노출된
direct/nested/batch/projection 경로 모두 검사"를 요구하고 위 열거 수단은 이 skill이
CAP 노출 면에서 구체화한 것이다.

현재 V4는 route inventory 92개와 `exposed-write-surface` 1개다.
`enumerateWriteSurface`는 host-observation schema·guide에 정의돼 있다.
원행은 surfaces·surfaceItems·probes·probeCoverage다.
`HostObservationValidator.PROBE_POLICY`와 `KIND_SURFACES`가 다음 적용 정책을
고정한다. 아래 QUERY 면제가 아닌 항목은 kind에 적용되는 class 중
요청한 class마다 probe 행이 필요하다. `writeCapable=false`인 readonly
entity set도 쓰기 거부를 관찰한다.

| item kind | 허용 surface | 적용 probe class |
|---|---|---|
| `ENTITY_SET` | ODATA_METADATA | DIRECT_CREATE·DIRECT_UPDATE·DIRECT_DELETE·DEEP_INSERT·UPSERT·BATCH_CHANGESET·DRAFT_ACTIVATE·NESTED_NAVIGATION_CREATE·NESTED_NAVIGATION_UPDATE·NESTED_NAVIGATION_DELETE |
| `BOUND_ACTION` | ODATA_METADATA | BOUND_ACTION·BATCH_CHANGESET |
| `UNBOUND_ACTION` | ODATA_METADATA | UNBOUND_ACTION·BATCH_CHANGESET |
| `FUNCTION` | ODATA_METADATA | 없음 |
| `TOOL` | MCP_SERVER_DISCOVER·MCP_TOOLS_LIST | MCP_TOOL_CALL |
| `WORKER_HANDLER` | WORKER_HANDLER_REGISTRY | WORKER_HANDLER_SUBMIT |
| `MANAGEMENT_ENDPOINT` | MANAGEMENT_ENDPOINTS | MANAGEMENT_ENDPOINT_WRITE |

QUERY 면제는 `TOOL`·`BOUND_ACTION`·`UNBOUND_ACTION`에만 적용한다.
harness가 hash로 묶인 allowlist bytes
([acceptance-capabilities.json](../../../../contracts/acceptance-capabilities.json))를
읽어 다음 세 조건을 모두 확인할 때 FUNCTION처럼 적용 probe class를
비운다: capabilityId의 kind가 QUERY, `writeCapable=false`, `itemId`가
그 capability id 자체이거나 `.`·`/` 뒤 그 id로 끝나는 이름이다.
extractor는 capabilityId와 이름을 실제 노출 면대로 적는다.
COMMAND·RECORD, id 없음·목록 밖, QUERY id를 빌린 다른 이름, 범용
`query`·`command` dispatcher와 writeCapable 항목은 면제되지 않는다.
범용 dispatcher에는 쓰기 요청을 보내 거부와 효과0을 관찰한다.
면제 항목도 열거·hash·allowlist 대조에는 남고 applicableTargets 계산에서만
빠진다. READ_ONLY_NO_EFFECT라는 probe outcome은 추가하지 않았다.

정책에 없는 kind·맞지 않는 surface는 거부한다. `writeCapable=true` 항목은
별도로 모두 probe 대상이며 FUNCTION도 이 규칙을 면제받지 않는다.
없는 navigation·draft 경로도 시도하고 `NOT_EXPOSED`로 기록한다.
validator는 allowlist bytes와 `applicableTargets`를 다시 계산하고
`probedTargets`·`complete`를 실제 probe 행과 대조한다. 적용 항목이 있는데
0/0 complete=true로 줄이는 것은 실패다. 404만으로 효과0을 증명하지
않으며 V4의 전후 DB assertion이 필요하다. kind 자체의 정직한 분류는
extractor에 남은 신뢰다. 실제 host adapter는 없어 열거 subcase는
`NOT_IMPLEMENTED`→`NOT_RUN`이다. 계약 정의나 selftest 표본은 실제 열거가 아니다.

열거 subcase의 실제 PASS 전에는 V4 노출 면 인수를 `NOT_RUN`으로 보고한다.
projection·tool·handler 변경 시 수동 열거 결과는 Task handoff·checks에 남긴다.
남은 실제 adapter·coverage 인수와 관련 소유자는
[이번 cross-owner 기록](../../../../docs/execution/step1r-sync5/README.md)과
[Step 2 round 5](../../../../docs/execution/step2r-round5/README.md),
[round 6 요청](../../../../docs/execution/step2r-round6/README.md)을 따른다.

## 명사·동사 조회와 query 계약(계획 §3.4)

API의 logical revision, runtime task terminal snapshot과 DB MVCC snapshot은
서로 다르다. `$result`로 받은 값 자체를 observer에 보내지 않는다.
[harness 가이드](../../../../verification/harness-guide.md)의 snapshot 절에
따라 요청·보고·검증을 구별한다.

- API revision은 `snapshotRef=RESULT_REVISION`이다. `snapshotSource`에는
  발급 action의 id·pointer·kind·route·capabilityId·actorRef·fixture actor·
  해석된 request만 보낸다. observer는 권한 범위의 원행에서 projection
  revision을 독립 재계산해 `data.snapshotRevision`,
  `data.snapshot.readMode=RESULT_REVISION`, `snapshot.revisionQuery`를 낸다.
  harness는 보관한 발급 revision과 비교한다.
- `awaitRuntimeTask` control의
  `/data/hostObservation/runtimeTask/snapshot/id`를 참조한 observe는
  `snapshotRef=RUNTIME_TASK_SNAPSHOT`이다. `snapshotSource`는 발급 action의
  id·pointer, `operation=awaitRuntimeTask`, 해석된 schedulerId·taskId·
  invocationHandle·scope를 보내며 snapshot id는 숨긴다. observer는 task
  identity로 host snapshot artifact를 스스로 찾아 읽고
  `data.snapshot.readMode=RUNTIME_TASK_SNAPSHOT`과
  `snapshot.runtimeTaskSnapshot={schedulerId, taskId, invocationHandle,
  snapshotId, artifactRef, sha256}`을 낸다. harness는 schedulerId를 요청과,
  taskId·invocationHandle·artifactRef를 await 결과의 runtimeTask와,
  snapshotId를 보관한 발급 값과 대조한다. artifact bytes의 SHA-256도
  직접 계산하며 해당 파일은 이 observe의 `artifactRefs`에 있어야 한다.
  DB read의 `snapshot.capturedAt`은 task `completedAt`보다 이르면 안 된다.
  `data.snapshotRevision`은 observer 자신의 값이다.
- literal `CURRENT_COMMITTED`·`CURRENT_LOCK_WAIT`는 앞선 action 뒤의
  새 read이며 readMode가 directive와 같아야 한다. `data.snapshot.id`는
  모든 mode에서 observer 자신의 DB snapshot token이고 echo는 거부된다.

prepare의 `ContractValidator.snapshotRefProblems`는 `$result` 참조가
invoke/query/start의 `/response/snapshotRevision` 또는 위 awaitRuntimeTask
snapshot id인 경우만 받는다. 다른 pointer는 준비 실패다. 두 read mode의
실제 `ObserverSnapshot` 구현은 Step 3 actual 소유이며 현재
`NOT_IMPLEMENTED`→`NOT_RUN`이다. harness selftest 성공과 구별한다.

같은 ID·`snapshotRevision`·`asOf`/`knownAt`·`scope`로 두 진입점
(`getObject`/`getWork`, `searchObjects`/`searchWorks` 등)을 호출한다.
다음을 필드별로 비교한다: 객체/업무 ID, 수량과 단위, `unknowns`,
`conflicts`, `evidenceRefs`, 의무의 owner/nextAction/nextCheck.
`nextCursor`는 진입점·query별 opaque 상태이므로 두 진입점 사이에서 같다고
단언하지 않는다. paging은 고정 snapshot에서 page를 넘겨도 ID 순서가 안정이고
tie-break가 결정적인지, page 사이의 시점 변화가 응답에 밝혀지는지로 확인한다.
다른 조직 객체의 존재 노출 여부도 관찰한다. 목표 판정은 물류 도착과 정산을
독립 assertion으로 둔다.

## 고정 수량 primary와 V7 원행

수량 primary는 독립 기대값·단위·operator를 실제 응답 또는 DB 원행과
대조한다. 원행 집계에는 case가 고정한 `where`를 쓴다.
`CatalogLinkValidator.fixedQuantityAssertion`은 source·baseline·unitSource·
baselineUnitSource 중 하나라도 `/data/data/` 파생 pointer이면 고정 수량
primary로 세지 않는다. observer derivation을 원행에서 재계산해 일치해도
이 primary를 대신하지 못한다.

V7 세 subcase의 `new-effect-quantity0`은 `after` 관찰의
`/data/rawRows/movements`에서 `kind=DISPATCH`,
`commandIdempotencyKey=new20`을 고정 filter로 쓰고 `sumEquals 0 BOX`를
단언한다. 단위는 active segment 원행에서 읽는다. 보조
`no-new-dispatch-rows`는 전후 DISPATCH 원행 전체가 같은지 검사하고,
restart 사례의 baseline은 `prior-committed`다. 새 미허용 효과0과
기확정 효과 보존을 함께 본다. 실제 V7 새 assertion 인수는 `NOT_RUN`이다.

다른 case의 `/data/data/` 보조 assertion은 아직 case가 derivation filter를
고정하지 않는 gap이 남아 있다. C4·E1·E2·T17·T18·T23·T26·V2·V8의
104줄은 Step 2 후속 소유이며 이 round의 primary 수정으로 해소됐다고
보고하지 않는다.
