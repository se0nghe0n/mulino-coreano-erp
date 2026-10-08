# Step 2 재검토 7라운드: closure review 4 지적 처리 기록

Step 2 closure review 4(Claude Opus xhigh `step2-closure-4-opus.json`
FAIL, Astra low `step2-closure-4-astra.json` PASS)의 지적을 처리한
기록이다. 기준 commit은 `69b2b8ef`, branch는 `step2r/round7`, worktree는
`/Volumes/VideoStore/Developer/mulino-ontology-step2r-round7`다. 작업
모델은 사용자 Step 2 지정대로 Claude Opus high다.

손댄 범위는 다음과 같다.

- `contracts/fixture-place-kinds.{json,md}`, `contracts/mcp/s0-protocol.md`
- `verification/cases/{E1,T13}/**`, C1 반례 fixture 1개
- harness의 `actual/` 밖 Java와 test
- `verification/harness-guide.md`, `verification/host-observation-guide.md`
- round 6 작성 script, 이 디렉터리의 작성 script

손대지 않은 범위는 `backend/**`, `verification/actual/**`,
`verification/harness/src/main/java/org/mulino/verification/actual/**`
(Step 3), `.agents/skills/**`(Step 1)다.

## 항목별 처리

### 필수(P2)

| # | 지적 | 판정 | 내용 |
|---|---|---|---|
| 1 | E1 세 subcase·T13 `partial-excess-return-relocation`의 첫 수령이 내부 보관을 만들지 않아, 계약을 지키는 제품에서는 받은 실물의 예약·출고·이동이 SCOPE_INELIGIBLE이다 | FIXED | **계약 규칙**: `contracts/fixture-place-kinds.json`에 `directReceiptCustody`를 더했다(version 1.1.0). 기존 fixture QuantitySegment를 확인하지 않는 confirmReceipt는 직접 수령이다. 그 실물(또는 split·이동·보류·예약으로 이어진 결과)을 뒤에서 `reserveQuantity`·`replaceAllocation`·`pickQuantity`·`dispatchQuantity`·`moveQuantity`로 쓰면, 수령은 `receivingCustodianId` slot을 보낸다. slot 값은 다음을 모두 만족한다. 같은 조직의 Human/Agent alias다. confirmReceipt role·grant가 있고, grant 장소 scope가 있으면 수령 장소가 들어 있다. 인용한 수령 원본(DocumentVersion)의 `fixtureContent.receivingCustodianAlias`가 같은 alias를 지명하고, 그 evidence sha256은 canonical content의 hash와 같다. 같은 수령(`canonicalOccurrenceKey`, 없으면 `commandIdempotencyKey`)의 confirm·retry는 모두 같은 slot을 보낸다. 운송 수령은 slot을 보내지 않는다. 제품 근거는 `ReceiptCommands`다(읽기만 했다). slot은 직접 수령에만 쓰고, 수령 권한이 있는 내부 actor만 받으며, 검증된 증거 chain이 지명해야 받는다. `contracts/fixture-place-kinds.md:76`의 "남은 범위"는 "직접 수령의 보관자" 절과 적용표로 바꿨다. **강제**: `ContractValidator.receiptCustodyProblems`가 위 규칙을 검사하고 `Main` prepare에 연결했다. **E1**: 세 fixture에서 receipt60·receipt40이 `receiver`를 지명한다. receiver는 내부 Human이고 확인하는 `procurement`와 다른 사람이다. receiver에 W scope의 confirmReceipt role·grant를 더했다. warehouse-60·40 원본이 같은 alias를 지명하고 evidence sha256을 다시 계산했다. 첫 수령 뒤 재시도·중복 confirm은 E1에 없다. 운송 증빙 carrier-60은 보관자를 지명하지 않으므로 제품 규칙상 중립이다. **T13**: receipt60·40·5가 `warehouse`를 지명한다. warehouse는 그 subcase에서 유일한 수령 권한자다. hash만 있던 `warehouse-receipt` evidence를, 같은 보관자를 지명하는 DocumentVersion alias로 만들었다. **양성 대조**: E1 세 subcase에서 receipt40 뒤·qc-hold60 전에 `received-custody`(API getObject)와 `received-custody-db`(segments)를 관찰한다. `received-custody-control`은 W의 활성 실물이 정확히 수령60·수령40 두 행이고 보관자가 receiver임을 단언한다. 그래서 E1의 판매 적격0(`current-sell-eligible-9/10/9-mcp`, `two-entry-anchor-eligibleQuantity`)은 보관 미확인이 아니라 QC·기관·반품 보류 때문에 나온다. T13도 receipt-db에서 W 60·40·5의 보관자가 warehouse임을 단언한다. **다른 case 점검**: 직접 수령을 가진 subcase 전체를 split·이동·보류·예약의 파생까지 따라가 검사했다. 수령 실물을 뒤에서 예약·출고·이동하는 subcase는 E1×3과 T13 1개뿐이었다(T02, T06×3, T07, T09, T11×5, T12×2, T21×2, T22, C3×3, V1, V6×8, V8은 해당 없음). 그 직접 수령 실물에 양의 판매 적격을 기대하는 assertion도 없다. 회귀 test: `directReceiptsWhoseStockIsUsedNameAnEvidencedInternalReceivingCustodian`(41 case 문제0, mutant 8종이 각각 이유로 거부됨, T13 receipt40만 뺀 경우 move가 잡힘, C2 운송 수령의 slot 거부, T07 미사용 수령 허용), `e1ProvesConfirmedInternalCustodyBeforeTheHolds` |

### 권장(P3)

| # | 지적 | 판정 | 내용·소유자 |
|---|---|---|---|
| 2 | C1 `unrecognized-place-kind`가 saleSourcePlaceAliases를 `['W']`로 줄여 장소 종류와 판매 출처 정책을 섞었다 | FIXED | round 6 `author_place_kind_controls.py`가 원 fixture의 `['W','W2']`를 그대로 두고 `W-UNRECOGNIZED`를 더한다. 다시 실행했을 때 바뀐 것은 이 fixture의 그 값뿐이었다. 그래서 CON40과 UNK40의 차이는 장소 종류 하나다. 회귀: `unrecognizedKindNegativeKeepsTheSaleSourcePlaces` |
| 3 | NO_TASK 수동 관찰에 최소 길이가 없다 | FIXED | 창 전체(30초)를 요구하면 "창 끝 뒤에 끝날 수 없다"는 상한과 정확히 같은 시각에서만 만족되므로, 지적이 허용한 "최소 1 tick" 방식을 택했다. `CaseRunner.controlRequest`가 fixture `runtimeProfile.tickSeconds`를 `naturalTickSeconds`로 watcher 요청에 넣는다. `HostObservationValidator.naturalTick`는 이 값이 1–창 길이의 정수인지 확인하고, NO_TASK는 `completedAt ≥ observeFrom+naturalTickSeconds`이어야 받는다. `runtimeProfileProblems`는 case가 직접 쓴 `naturalTickSeconds`와, 수동 관찰 subcase의 `tickSeconds`가 없거나 창보다 긴 경우를 거부한다. host-observation-guide.md와 harness-guide.md에 적었다. 회귀: `noTaskObservationCoversAtLeastOneNaturalTick`(1초 수용, tick 1초에서 0.5초 거부, 값 누락·31 거부, prepare 두 경우), `caseRunnerResolvesTheFixtureTickForAPassiveWatcher`(T26 fixture의 tick 1이 요청에 들어감). 기존 `HostObservationValidatorTest` NO_TASK 표본에는 `naturalTickSeconds=1`을 넣었다 |
| 4 | s0-protocol 406이 정확한 문자열 비교다 | FIXED | 406 행을 "`application/json`과 `text/event-stream`을 모두 나열하지 않음"으로 바꾸고 비교 규칙을 적었다. 순서·공백·대소문자·parameter는 무관하다. `q=0` 범위(RFC 9110 §12.4.2)와 wildcard는 나열로 세지 않는다. `ContractValidator.acceptsMcp`가 같은 비교를 하고 `wireTransportProblems`가 이를 쓴다. case 요청은 기본값을 그대로 보낸다. 생성기의 s0-protocol 문장 고정(39행)은 그대로 둔다. backend `OntologyMcp`·`PlatformMcp`의 `contains` 검사도 두 type이 있으면 받는다(소스를 읽기만 했다). 회귀: `acceptListsBothMediaTypesInAnyFormButNotAsRefusedOrWildcardRanges`(수용 6, 거부 9, 순서 바꾼 T20 요청 수용, q=0 요청 406 요구) |
| 5 | QUERY probe 면제가 extractor가 보고한 writeCapable·capabilityId를 믿는다 | DEFERRED | 지적이 제안한 수정은 hash로 묶인 raw tools/list의 `readOnlyHint`나 OData FUNCTION 근거를 harness가 직접 읽는 방식이다. 지금 selftest surface artifact는 구조 없는 text이고, OData bound/unbound action은 $metadata만으로 읽기 전용을 보일 수 없다. 그래서 면제를 action에서 빼면 round 6 P2(읽기 전용 action의 probe 강제)가 다시 생긴다. 둘째 제안(QUERY 항목마다 READ actor 호출과 DB 무효과 증거)은 새 host 증거 형식이 필요하다. 실제 adapter가 내는 raw surface 형식이 정해진 뒤에 한다. 영향은 작다. 다른 case가 query tool을 DB 전후 불변으로 검사한다. 소유자: Step 2 tests worker(Claude Opus high) 다음 round. Step 3 actual adapter 담당이 raw surface 형식을 정해야 한다 |
| 6 | placeKindProblems 사각지대: 보관자 미확인·외부 INTERNAL_STORAGE segment의 선언 반례 없음, location 없는 segment 건너뜀 | DEFERRED | (1) segment 수준 `custodyControl` 선언은 그 반례를 쓰는 case와 함께 만들어야 의미가 있다. 지금은 쓰는 case가 없다. (2) location 없는 segment 약 400개 중 fulfilment에 쓰이는 것은 T24 `deny-batch` 하나이고 그것은 반례다. 그 fixture는 T06 생성기 소유라, location을 요구하면 생성기 변경이 함께 필요하다. 둘 다 지금 뒤집히는 oracle이 없다. 소유자: Step 2 tests worker(Claude Opus high) 다음 round. contracts/fixture-place-kinds.md "남은 범위"에 적었다 |

## commit

- `9fd76fc6` fix(검증): closure review 4의 직접 수령 보관자와 P3 지적을 닫는다
- 이 README: docs commit

## 실행한 checks

환경은 Java 21(zulu 21.0.5), Python 3.14.7, `env.sh`다. Maven·verify는
`$MULINO_SLOT`으로 한 번에 하나씩 실행했다. `./verify harness`, 첫
`./verify prepare`, contract-red는 commit 직전에 같은 내용의 tree에서
돌렸다. 나머지는 fix commit `9fd76fc6`의 clean tree에서 돌렸다.

| 명령 | exit | 결과 |
|---|---|---|
| `./verify harness` | 0 | 513 tests, failure/error/skip 0(기준 507 + `StepTwoRoundSevenRegressionTest` 6) |
| `./verify prepare` | 0 | PREPARED, codeCommit `9fd76fc6`, workingTreeDirty=false, 41/801/23991(기준 23987 + E1 3·T13 1), preparationProblems 0, caseAssetChecks 11개 PASS(generators-reproduce·case-generators-reproduce 포함), knownOpenGaps 34, runtimeGates 1 |
| `./verify coverage` | 2 | NOT_RUN, preparationStatus PREPARED, runtimeStatus NOT_RUN, gateComplete=false, preparationProblems 0, coverageProblems 0, knownOpenGaps 34, caseProfiles 3030, observations 499 |
| `python3 verification/coverage/validate.py` | 0 | VALID, runtimeStatus NOT_RUN, gateComplete=false |
| `./verify contract-red` E1·T13·C1 | 1 | expected=discovered=started=NOT_IMPLEMENTED=23, skipped 0 |
| `python3 -m unittest discover -s verification/coverage -p 'test_*.py'` | 0 | 68 OK(260s). 첫 실행은 테스트 도중 이 README를 untracked로 써서 `test_saved_manifest_reassembly_rejects_forged_observation_status`가 "working tree state differs"로 error 1이었다. README를 치우고 clean tree에서 다시 돌려 통과했다 |
| `python3 -m unittest discover -s verification/requirements -p 'test_*.py'` | 0 | 37 OK(`test_case_generators_reproduce` 포함) |
| `python3 -m unittest discover -s verification/mcp-tests -p 'test_*.py'` | 0 | 3 OK(생성기 재현) |
| `python3 -m unittest discover -s verification/cases -p test_check_vocabulary.py` | 0 | 6 OK |
| `verification/model-binding/run selftest` | 0 | 61 tests OK |
| `verification/model-binding/run prepare` | 0 | PREPARED |
| `python3 -I verification/requirements/validate_catalog.py` | 0 | structure VALID, oracle 122·observation 499·case 41·requirement 26 |
| `python3 -I verification/cases/V2/cases_b_invariants.py` | 0 | 문제 0(E1, E2, C4, V2, V3) |
| `python3 -I verification/cases/check_vocabulary.py --check` | 0 | VALID, problems 0, knownOpen 34 |
| `python3 -I verification/requirements/check_layer_routes.py` | 0 | VALID, unexplained 0, KNOWN_OPEN 0 |
| `python3 -I verification/requirements/check_derived_bindings.py` | 0 | VALID, 5 files, KNOWN_OPEN 0 |
| `python3 -I verification/cases/T08/bind_observations.py --check` | 0 | CURRENT |
| `python3 -I verification/cases/V7/bind_observations.py V4\|V6\|V7 --check` | 0 | 셋 다 CURRENT |
| `python3 -I verification/cases/V4/author_review_fixes.py --check` | 0 | CURRENT |
| 생성기 제자리 재실행(author_cases.py, C3 author_prerequisites.py, T06 author_contracts.py, build_cases.py, T26 author_review_fixes.py 두 번, V4 author_review_fixes.py, model-binding generate.py, round 6 author_place_kind_controls.py, round 7 author_receipt_custody.py 두 번) | 0 | 실행 전후 `git status`·`git diff` hash 같음(drift 없음). `migrate_place_kinds.py` dry run도 DRY 0 |

NOT_RUN: `./verify regulatory`, `--actual`·제품 runtime, 실제 host
adapter·MCP wire·extractor. 수령 보관자 규칙, 양성 대조, NO_TASK 최소
길이, Accept 비교는 모두 계약과 selftest까지만 확인했다. 제품 인수는
`NOT_RUN`이다.

## cross-owner 요청

- Step 3 actual(`verification/harness/src/main/java/org/mulino/verification/actual/**`,
  `verification/actual/**`):
  - `FixtureInstaller`는 수령 원본 DocumentVersion의
    `fixtureContent.receivingCustodianAlias`를 해석해 원본 bytes와 event
    payload에 같은 `receivingCustodianId`로 설치해야 한다. case의
    confirmReceipt `receivingCustodianId` slot(`{"$alias":…}` 또는 typed
    `{"value":{"$alias":…}}`)은 제품 slot으로 그대로 보낸다. 제품은 원본과
    payload가 모두 지명해야 받는다. T13 `warehouse-receipt`는 이제 content가
    있는 alias다.
  - 수동 watcher host adapter는 요청의 `naturalTickSeconds`를 받는다.
    NO_TASK를 보고하려면 적어도 `observeFrom+naturalTickSeconds`까지
    관찰해야 한다.
  - round 6 요청(FixtureInstaller kind 기본값 제거, native fixture kind·
    보관자, observeFrom 소비)은 그대로 남아 있다.
- Step 1 skill 소유자(GPT-6.1 Sol high):
  `.agents/skills/ontology-scenario-testing/references/repository-harness.md`
  "fixture 장소 종류와 transport 준비 검사" 절의 다음 두 문장이 이번 변경으로
  낡았다. 고쳐야 한다.
  - "E1·T13 첫 수령의 `receivingCustodianId` slot도 receipt 계약과 함께
    정해야 한다": 직접 수령 보관자 규칙과 `receiptCustodyProblems`로 바꾼다.
  - "Accept 누락은 … 406": media type 비교와 `naturalTickSeconds`를 반영한다.
- Step 3 backend: 요청 없음. `ReceiptCommands`의 현재 규칙에 fixture를 맞췄다.

## 하지 않은 것

- 제품 실행, `--actual`, `./verify regulatory`.
- backend·`verification/actual/**`·`.agents/skills/**` 변경(위 요청).
- 위 항목 5·6(DEFERRED).
