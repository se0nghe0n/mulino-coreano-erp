# Step 2 재검토 6라운드: closure review 3 확인 지적 처리 기록

Step 2 closure review 3(Claude Opus xhigh, Astra low)의 skeptic 검증을
거친 지적 7건(`step2-closure-3-verdicts.json`, 원 주장
`step2-closure-3-claims.json`)을 처리한 기록이다. 기준 tag
`step2r6-baseline`(`75999b09`), branch `step2r/round6`, worktree
`/Volumes/VideoStore/Developer/mulino-ontology-step2r-round6`다. 작업
모델은 사용자 Step 2 지정대로 Claude Opus high다.

손댄 범위: `contracts/fixture-place-kinds.{json,md}`(신규),
`contracts/acceptance-fixture.schema.json`(설명), `contracts/mcp/s0-protocol.md`,
`verification/cases/**`(fixture 373개 중 306개 전환·생성기 출력·C1 반례·
T16/T17 양성 대조·registry), harness의 `actual/` 밖 Java와 test,
`verification/harness-guide.md`, `verification/host-observation-guide.md`,
생성기 `verification/mcp-tests/author_cases.py`,
`verification/cases/T06/author_contracts.py`,
`verification/platform-tests/build_cases.py`,
`verification/cases/V4/author_review_fixes.py`. 손대지 않은 범위:
`backend/**`, `verification/actual/**`,
`verification/harness/src/main/java/org/mulino/verification/actual/**`
(Step 3), `.agents/skills/**`(Step 1),
`verification/harness/src/test/resources/examples/HARNESS-EXAMPLE/`(제품
case가 아닌 문법 selftest라 bytes를 유지했다).

## 항목별 처리

### 필수

| # | 지적 | 판정 | 내용 |
|---|---|---|---|
| 1 | index 0·5 (P1) fixture Place.kind 어휘 계약 없음, C1/C2 양성 전제 불가 | FIXED | `contracts/fixture-place-kinds.json`(기계 원본)과 `.md`(의미표)를 새로 두었다. kind는 INTERNAL_STORAGE(내부)·TRANSIT·CUSTOMER·SUPPLIER·EXTERNAL_PORT(외부, 확정0)다. 내부 보관자는 같은 조직의 Human/Agent alias다. 반례는 `kindControl=UNRECOGNIZED_PLACE_KIND`로만 선언한다. 제품 근거는 s2-inventory moveQuantity와 `QualityEligibility.custody`(읽기만)다. `ContractValidator.placeKindProblems`(prepare 연결)가 kind 누락·어휘 밖 kind·baseline.places 불일치·내부 보관 segment의 내부 custodian 누락을 거부한다. fixture 306개를 `migrate_place_kinds.py`로 옮겼다(기록 `migration-log.txt`, 재실행 DRY 0). 옛 kind는 INTERNAL_WAREHOUSE·WAREHOUSE→INTERNAL_STORAGE, PORT→EXTERNAL_PORT, TRANSPORT→TRANSIT, baseline EXTERNAL_CUSTOMER→CUSTOMER로 바꿨다. kind 없는 Place는 alias 의미로 정했다. 조직 alias `CUSTODIAN`(kind WAREHOUSE)은 내부 Human 보관자로 바꿨다. 내부 보관·운송·항만의 자기 화물 segment에는 내부 custodian(`warehouse`, 없으면 `receiver`, 둘 다 없으면 새 Human `warehouse`)을 두었다. 생성기 소유 fixture(T01/T20/T25, T06/T22/T24, T23/V8)는 생성기를 고쳐 다시 만들었고 T26 파생 4개도 다시 만들었다. 반례: C1 `unrecognized-place-kind`(조건이 같은 위탁40이 INTERNAL_STORAGE에서는 적격40·ALLOWED, kind WAREHOUSE 반례에서는 적격0·UNKNOWN, 품목 보유80·적격40, 원행 두 실물). 양성 대조: T16 `provisional-holds`의 `confirmed-eligible-control`(확인 수령 직후·보류 전 적격60)과 T17 eligibility-* 다섯 subcase의 `eligibility-positive-control`(같은 DB 관찰에서 A가 ALLOWED 1행). 아래 "만족 가능성 확인"에 대상별 근거를 적었다. 회귀: `StepTwoRoundSixRegressionTest.everyFixturePlaceHasAContractKindAndInternalStorageStockAnInternalCustodian`(41 case 문제0, mutant 9종 각 이유로 거부, 선언 반례 수용), `theUnrecognizedKindNegativeAndThePositiveControlsAreAuthored` |
| 2 | index 1 (P1) T20/V4 wire 요청에 Accept 없음, 허용 목록 밖 Origin | FIXED | `author_cases.py` wire()와 V4 `author_review_fixes.py` wire()가 `Accept: application/json, text/event-stream`을 보내고 Origin을 보내지 않는다. 생성기는 s0-protocol.md의 Accept 문장과 상수가 다르면 멈춘다. Origin은 T20 `wire-bad-origin`(403)만 보낸다. 새 T20 `wire-missing-accept`는 Accept 없는 요청에 HTTP 406과 업무 효과0을 고정한다(T20 58→59 subcase). s0-protocol.md에 406 행을 더하고, 계약과 fixture가 허용 Origin을 선언하지 않으므로 case 요청은 Origin을 보내지 않는다고 적었다. `ContractValidator.wireTransportProblems`(prepare 연결)는 Streamable HTTP 요청의 Accept 누락·변경과 Origin을, 그 action의 `/response/httpStatus`를 406·403으로 고정한 반례가 아니면 거부한다. 둘 다 어기면 기대 status가 모호해 거부한다. 회귀: `wireRequestsCarryAcceptAndNoOriginUnlessPinnedAsTheTransportNegative`(round 5 모양 Origin·Accept 누락 mutant, 고정 해제한 반례, 둘 다 어긴 요청을 거부, V4 두 요청의 Accept 확인) |
| 3 | index 2 (P2) 쓰기 면 probe 정책이 읽기 전용 tool·action에도 probe를 강제 | FIXED | 선택: capability kind에서 적용성을 정한다. `HostObservationValidator.queryExempt`는 TOOL·BOUND_ACTION·UNBOUND_ACTION 항목이 QUERY capability를 부르면 FUNCTION처럼 적용 probe class를 비운다. kind는 extractor가 아니라 hash로 묶인 allowlist bytes(`contracts/acceptance-capabilities.json`)에서 harness가 다시 읽는다. `writeCapable=false`이고 항목 이름이 그 capability id(또는 `.`·`/` 뒤 id)일 때만 면제한다. COMMAND·RECORD, capabilityId 없음·목록 밖(범용 `query`·`command` dispatcher), writeCapable 항목은 표의 probe를 모두 받는다. READ_ONLY_NO_EFFECT outcome은 만들지 않았다. host-observation-guide.md에 QUERY 면제 절을 더했다. 회귀: `queryToolsAndActionsNeedNoWriteProbeButCommandsRecordsAndUnknownItemsDo`(QUERY tool·unbound·bound action은 probe 없이 수용, COMMAND·RECORD·QUERY id를 빌린 다른 이름·dispatcher·writeCapable QUERY는 never probed로 거부, 과대 applicableTargets 거부) |

### 권장(P3)

| # | 지적 | 판정 | 내용·소유자 |
|---|---|---|---|
| 4 | index 3·6 관찰 경계가 watcher host adapter에 전달되지 않음 | FIXED | `CaseRunner.controlRequest`가 수동 watcher(passiveWatch) 요청 parameter에 `observeFrom`을 넣어 port에 보낸다. group watcher는 group 직전 경계(`data.observationBoundaryAt`과 같은 값), group 밖 watcher(T26 `repeat-sweep`)는 dispatch 직전 harness 시각이다. 검증에도 같은 요청을 쓴다. `naturalTick`은 창을 `[observeFrom, observeFrom+window]`로 잰다. observeFrom이 없으면 거부하고, 경계 값이 있으면 같아야 하며, command는 observeFrom 전에 시작하거나 observeFrom+window 뒤에 끝날 수 없다. extractor는 그 창 안의 지속 제출 행만 읽는다(guide 갱신). 그래서 group watcher는 경계 직후 제출을 잃지 않고 단독 repeat watcher는 앞선 sweep 행을 보고하지 않는다. case가 observeFrom을 쓰면 `runtimeProfileProblems`가 거부한다. 회귀: `theWatcherReceivesObserveFromAndTheWindowIsMeasuredFromIt`, `caseRunnerResolvesObserveFromForAStandaloneWatcherAndPrepareRejectsAnAuthoredOne`. 기존 `HostObservationValidatorTest`·`StepTwoRoundFiveRegressionTest` natural tick 표본은 observeFrom을 갖도록 갱신했다 |
| 5 | index 4 s0-protocol "params 형식 위반 -32600"과 T20 -32602 | FIXED | s0-protocol.md에 `params`가 object·array가 아닐 때만 envelope -32600이고, object 안의 `_meta` 누락·clientInfo·clientCapabilities 내용 오류는 400 -32602라고 적었다. `_meta`가 없으면 version header와 비교할 본문 값이 없어 -32020이 아니라고 적었다. 오류 표에도 -32602 행을 더했다. `verification/cases/T20/README.md`의 "`_meta` 누락은 -32602·-32020 모두 허용, envelope만 고정" 문장을 고정값 -32602에 맞췄다. backend 불일치는 아래 cross-owner 요청이다 |

### 만족 가능성 확인(항목 1)

계획 §4.1·§4.2·§6을 따르는 제품이 전환 뒤 fixture로 각 기대를 만들 수
있는지 fixture 내용으로 확인했다. 실행 인수는 아니며 `NOT_RUN`이다.

| 대상 | 근거 |
|---|---|
| C1 custody-not-sale 적격40·미예약40 | CON40@W(INTERNAL_STORAGE), 보관자 CUSTODIAN(내부 Human), eligibilityFacts QC·규제·고객 ALLOWED·처분 CONFIRMED. CUS60은 처분 UNKNOWN이라 0. 앱 WRITE 예약은 창고 보관 검사를 통과한 뒤 적격 부족으로 INSUFFICIENT_ELIGIBLE_QUANTITY다 |
| C2 예약60·출고60·현재40 | A60·B40은 EXTERNAL_PORT에서 내부 보관자 `warehouse`를 가진 수입 화물이다. receive60은 기존 운송 segment(A60)를 W로 확인하고 보관자를 잇는다. 정책 `currentSaleConditions`가 판매 조건을 허용한다. W는 INTERNAL_STORAGE다 |
| T04 hold-dispose 90, T05 custody-not-sale 40, V3 hold-first 20 | 각 segment가 INTERNAL_STORAGE W에 있고 보관자 CUSTODIAN(내부 Human)이며 판매 근거는 기존 eligibilityFacts다 |
| T17 reserve-pick-dispatch 적격60, eligibility-* | W(INTERNAL_STORAGE)의 A·B·EARLY·U·OTHERPACK에 내부 보관자 `receiver`. 양성 대조는 같은 관찰의 A ALLOWED 1행이다 |
| T16 provisional-holds | TRANSIT60@EXTERNAL_PORT, 보관자 CUSTODIAN(내부). 확인 수령 뒤 W 60이 적격60(양성 대조), 임시 접수 0·QC만 해제 뒤 0은 임시 상태·RECALL60 때문이다 |
| T26 예약 기반 subcase | Q20@W(INTERNAL_STORAGE), 보관자 `warehouse`, W2 INTERNAL_STORAGE(이동 대상), C_PLACE CUSTOMER |

### 남긴 범위

| 범위 | 이유 | 소유자 |
|---|---|---|
| E1·T13 첫 수령 보관자 | E1 full-flow는 SUPPLIER 장소의 A를 선적해 W에 수령하고, T13 `partial-excess-return-relocation`은 기존 segment 없는 수령 뒤 예약·이동한다. 두 case의 confirmReceipt는 수령 보관자를 보내지 않는다. 제품은 첫 수령에 내부 보관자 slot(`receivingCustodianId`)을 요구하고 운송 수령은 이전 보관자를 잇는다(native s3/s4 flow는 slot을 보낸다). 공급자 장소의 실물에 내부 보관자를 주는 것은 의미가 틀려 fixture로 고치지 않았다. case 요청에 수령 보관자를 더할지는 계획이 정하지 않은 slot이라 Step 3 수령 계약과 함께 정해야 한다 | Step 2 tests worker(Claude Opus high) 다음 round, Step 3 receipt 담당과 결정 |
| round 5의 항목 9–12 | 이번 지적 대상이 아니라 그대로다(round 5 README) | round 5에 적은 소유자 |

## commit

- `89abd2c3` fix(검증): closure review 3의 장소 종류·MCP header·probe 지적을 닫는다
- 이 README: docs commit

## 실행한 checks

환경은 Java 21(zulu 21.0.5), Python 3.14.7, `env.sh`, Maven·verify는
`$MULINO_SLOT`로 한 번에 하나씩 실행했다. `./verify harness`와 첫
`./verify prepare`만 commit 직전 같은 내용의 tree에서 돌렸고, 나머지는
fix commit `89abd2c3`의 clean tree(`workingTreeDirty=false`)에서 얻었다.

| 명령 | exit | 결과 |
|---|---|---|
| `./verify harness` | 0 | 507 tests, failure/error/skip 0. 기준 501 대비 +6(`StepTwoRoundSixRegressionTest`). natural tick 표본 2개 test는 observeFrom을 갖게 갱신(`HostObservationValidatorTest`, `StepTwoRoundFiveRegressionTest`) |
| `./verify prepare` | 0 | PREPARED, codeCommit `89abd2c3`, workingTreeDirty=false, 41/801/23987(기준 23962·799), preparationProblems 0, caseAssetChecks 11개 PASS, knownOpenGaps 34, runtimeGates 1 |
| `./verify coverage` | 2 | NOT_RUN, preparationStatus PREPARED, runtimeStatus NOT_RUN, gateComplete=false, preparationProblems 0, coverageProblems 0, knownOpenGaps 34, caseProfiles 3030, observations 499 |
| `python3 verification/coverage/validate.py` | 0 | VALID, runtimeStatus NOT_RUN, gateComplete=false |
| `./verify contract-red` 41 case 전체 | 1 | expected=discovered=started=NOT_IMPLEMENTED=801, skipped 0. fixture가 바뀐 case가 거의 전부라 전체를 돌렸다 |
| `python3 -m unittest discover -s verification/coverage -p 'test_*.py'` | 0 | 68 OK(292s) |
| `python3 -m unittest discover -s verification/requirements -p 'test_*.py'` | 0 | 37 OK(`test_case_generators_reproduce` 포함) |
| `python3 -m unittest discover -s verification/mcp-tests -p 'test_*.py'` | 0 | 3 OK(생성기 재현) |
| `python3 -m unittest discover -s verification/cases -p test_check_vocabulary.py` | 0 | 6 OK |
| `verification/model-binding/run selftest` | 0 | 61 tests OK |
| `verification/model-binding/run prepare` | 0 | PREPARED |
| `python3 -I verification/requirements/validate_catalog.py` | 0 | structure VALID, 122 oracle·499 observation·41 case·26 requirement |
| `python3 -I verification/cases/V2/cases_b_invariants.py` | 0 | 문제 0 |
| `python3 -I verification/cases/check_vocabulary.py --check` | 0 | VALID, problems 0, knownOpen 34 |
| `python3 -I verification/requirements/check_layer_routes.py` | 0 | VALID, unexplained 0, KNOWN_OPEN 0 |
| `python3 -I verification/requirements/check_derived_bindings.py` | 0 | VALID, 5 files, KNOWN_OPEN 0 |
| `python3 -I verification/cases/T08/bind_observations.py --check` | 0 | CURRENT |
| `python3 -I verification/cases/V7/bind_observations.py V4\|V6\|V7 --check` | 0 | 셋 다 CURRENT |
| `python3 -I verification/cases/V4/author_review_fixes.py --check` | 0 | CURRENT |
| 생성기 제자리 재실행(author_cases.py, C3 author_prerequisites.py, T06 author_contracts.py, build_cases.py, T26 author_review_fixes.py 두 번, V4 author_review_fixes.py, model-binding generate.py) | 0 | 실행 전후 `git diff`·`git status` hash 같음(drift 없음) |
| `python3 -I docs/execution/step2r-round6/migrate_place_kinds.py` | 0 | 쓰기 뒤 재실행 DRY 0(fixed point) |
| `python3 -I docs/execution/step2r-round6/author_place_kind_controls.py` 두 번 | 0 | 두 번째 실행 뒤 변화 없음 |

NOT_RUN: `./verify regulatory`, `--actual`·제품 runtime, 실제 host adapter·
MCP wire·extractor. 새 assertion·반례·observeFrom·QUERY 면제는 모두 계약과
selftest까지이며 제품 인수는 `NOT_RUN`이다.

## cross-owner 요청

- Step 3 actual(`verification/harness/src/main/java/org/mulino/verification/actual/**`,
  `verification/actual/**`):
  - `FixtureInstaller`는 Place kind가 없으면 `WAREHOUSE`를 넣는다. 이제
    case fixture는 모두 계약 kind를 가진다. 기본값을 없애고 kind 누락을
    거부하거나 계약 kind만 그대로 설치해야 한다. `kindControl` 반례는 kind를
    그대로 설치한다. 조직 alias를 custodian으로 받던 경로는 내부 Human
    보관자(Actor)로 설치된다.
  - native fixture `verification/actual/s2/fixture.json`의 Place W에 kind가
    없고, s1·s2의 A60·B40은 INTERNAL_STORAGE(또는 kind 없음)에 보관자가
    없다. 같은 계약을 따르게 바꿔야 한다.
  - 수동 watcher host adapter는 요청의 `observeFrom`을 읽고 extractor가
    `[observeFrom, observeFrom+observationWindowSeconds]`의 제출 행만
    내야 한다. enumerateWriteSurface extractor는 항목마다 `capabilityId`와
    이름을 정직하게 적는다(QUERY 면제가 그 값을 쓴다).
- Step 3 backend(`OntologyMcp`):
  - `_meta` 누락과 clientCapabilities 누락을 400 -32020 HeaderMismatch로
    답한다(OntologyMcp.java:26). 계약과 T20은 -32602다.
  - tools/call의 `Mcp-Name` 불일치를 -32602로 답한다. 계약과 T20
    `wire-name-mismatch`는 -32020이다.
  - clientInfo 형식 오류를 검사하지 않는다. T20 `wire-invalid-client-info`는
    400 -32602다.
  - Accept 누락의 406과 Origin 403은 계약과 같다. Origin 허용 목록은
    loopback만이라 case 요청이 Origin을 보내지 않는 현재 계약과 맞는다.
- Step 3 receipt 담당: E1·T13의 첫 수령·공급자 출발 수령 보관자(위 "남긴
  범위").
- Step 1 skill 소유자(GPT-6.1 Sol high):
  `.agents/skills/ontology-scenario-testing/references/repository-harness.md`에
  fixture 장소 종류 계약과 prepare 검사, Streamable HTTP Accept/Origin 규칙과
  `wire-missing-accept`, MCP 오류 우선순위의 -32602 구분, 쓰기 면 QUERY 면제,
  watcher `observeFrom`을 반영해야 한다. 표의 "kind별 probe 정책"과 "관찰
  창은 `data.observationBoundaryAt`부터" 문장이 이번 변경으로 낡았다.

## 하지 않은 것

- 제품 실행, `--actual`, `./verify regulatory`.
- backend·`verification/actual/**` 변경(위 요청).
- E1·T13 수령 보관자 slot(위 "남긴 범위").
- READ_ONLY_NO_EFFECT probe outcome(항목 3은 capability kind 면제로 닫았다).
