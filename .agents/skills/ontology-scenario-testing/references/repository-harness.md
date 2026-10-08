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

script·Python 누락, stale binding, 생성기 drift는 준비 실패다.
`vocabulary`는 `verification/cases/check_vocabulary.py --check`로
[domain-vocabulary.json](../../../../contracts/domain-vocabulary.json)과
[audit-observation-fields.json](../../../../contracts/audit-observation-fields.json)을
대조한다. 공개되지 않은 outcome·오류 code/짝·의무 kind·감사 이름은
fail-closed다. 명시적 PENDING만 owner·근거가 있는 `KNOWN_OPEN`으로 남기며
쓰이지 않는 PENDING도 실패다. prepare의 `knownOpenGaps`와 coverage manifest의
같은 필드에 드러난다. assembler는 같은 `review()`를 읽고 `validate.py`는
현재 입력과 다시 대조한다. 검사 exit0이 gap 해소나 제품 PASS는 아니다.

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
- 자율 loop는 clock advance 전 process를 stop하고, top-level `parallel`의
  branch 0 첫 action에 watcher, 나머지 branch에는 멈춘 process의 start만
  둔다. restart는 쓰지 않는다. prepare가 이 패턴을 검사한다.
  parallel은 barrier가 아니며 watcher가 늦으면 NO_TASK/창 밖 관찰로 닫힌다.
- watcher 요청의 `trigger=OBSERVE_NEXT_NATURAL_TICK`,
  `observationWindowSeconds`(1–30), `triggeredBy=SCHEDULER_LOOP`는 설정이다.
  이를 tickScheduler/sweepDue `operationEvidence`에 echo하면 계약 실패다.
  제출 증거는 독립 extractor의 `rawRows.schedulerSubmissions`다.
  SUBMITTED identity는 관찰 창 안 가장 이른 제출 행과 일치해야 하고,
  NO_TASK는 행이 없어야 한다. T26은 그 행의 `submittedBy`를 단언한다.
  자연 tick 증거를 만들려고 harness tick·sweep·resumeWork를 실행하지 않는다.
- verifyCoverage의 PREPARATION `rawRows.input`에는 `codeCommit`,
  `workingTreeDirty`, `checkoutCommit`, `checkoutDirty`를 둔다.
  validator는 commit 형식(40/64자리 소문자 hex)·boolean과 묶인 준비 보고의
  앞 두 값 일치를 검사한다. 현재 checkout과 commit 일치·두 tree clean은
  T25 assertion이 판정한다. 낡거나 dirty인 보고를 준비 성공으로 숨기지 않는다.

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
읽고, 열거한 쓰기 가능 항목에 `$batch` changeset, deep insert, upsert, draft
activation, nested navigation의 생성·수정·삭제를 시도한다. capability allowlist에
없는 쓰기 가능 항목은 FAIL이다. core entity는 노출하지 않거나 `@readonly`·
`@restrict`로 막고 READ grant 주체의 우회 효과가 0임을 전후 DB 관찰로 증명한다.
존재하지 않는 경로의 404만으로는 부족하다. 계획 §4.2·§13.2 V4는 "실제 노출된
direct/nested/batch/projection 경로 모두 검사"를 요구하고 위 열거 수단은 이 skill이
CAP 노출 면에서 구체화한 것이다.

현재 V4는 route inventory 92개와 `exposed-write-surface` 1개다.
`enumerateWriteSurface`는 host-observation schema·guide에 정의돼 있다.
원행은 surfaces·surfaceItems·probes·probeCoverage이며 validator가 allowlist와
완전성을 재계산한다. 실제 host adapter는 없어 열거 subcase는
`NOT_IMPLEMENTED`→`NOT_RUN`이다. 계약 정의나 selftest 표본은 실제 열거가 아니다.

열거 subcase의 실제 PASS 전에는 V4 노출 면 인수를 `NOT_RUN`으로 보고한다.
projection·tool·handler 변경 시 수동 열거 결과는 Task handoff·checks에 남긴다.
남은 실제 adapter·coverage 인수와 관련 소유자는
[이번 cross-owner 기록](../../../../docs/execution/step1r-sync4/README.md)을 따른다.

## 명사·동사 조회와 query 계약(계획 §3.4)

API의 logical revision과 DB MVCC snapshot은 다르다. `$result` revision은
observer 요청에 값으로 넘기지 않는다. `snapshotRef=RESULT_REVISION`과
발급 action의 `snapshotSource`를 보내고 독립 재계산 결과를 harness가
보관한 값과 비교한다(harness 가이드 snapshot 절). 현재 actual observer의
재계산은 `NOT_IMPLEMENTED`이므로 이 관찰은 `NOT_RUN`이다.

같은 ID·`snapshotRevision`·`asOf`/`knownAt`·`scope`로 두 진입점
(`getObject`/`getWork`, `searchObjects`/`searchWorks` 등)을 호출한다.
다음을 필드별로 비교한다: 객체/업무 ID, 수량과 단위, `unknowns`,
`conflicts`, `evidenceRefs`, 의무의 owner/nextAction/nextCheck.
`nextCursor`는 진입점·query별 opaque 상태이므로 두 진입점 사이에서 같다고
단언하지 않는다. paging은 고정 snapshot에서 page를 넘겨도 ID 순서가 안정이고
tie-break가 결정적인지, page 사이의 시점 변화가 응답에 밝혀지는지로 확인한다.
다른 조직 객체의 존재 노출 여부도 관찰한다. 목표 판정은 물류 도착과 정산을
독립 assertion으로 둔다.
