# Step 2 재검토 8라운드: closure review 5 지적 처리 기록

Step 2 closure review 5(Claude Opus xhigh `step2-closure-5-opus.json`
FAIL, Astra low PASS)의 남은 지적을 처리한 기록이다. 기준 commit은
`3b7e2d68`, branch는 `step2r/round8`, worktree는
`/Volumes/VideoStore/Developer/mulino-ontology-step2r-round8`다. 작업
모델은 사용자 Step 2 지정대로 Claude Opus high다.

손댄 범위는 다음과 같다.

- `contracts/fixture-place-kinds.{json,md}`, `contracts/acceptance-case.schema.json`
- `verification/cases/{C2,T09,T13,T14,T16,E1}/**`, `verification/cases/registry.json`
- harness의 `actual/` 밖 Java와 test
- `verification/harness-guide.md`, `verification/host-observation-guide.md`
- round 7 작성 script(E1 반례를 건너뛰게 했다), 이 디렉터리의 작성 script

손대지 않은 범위는 `backend/**`, `verification/actual/**`,
`verification/harness/src/main/java/org/mulino/verification/actual/**`
(Step 3), `.agents/skills/**`(Step 1)다.

## 항목별 처리

### 필수(P2)

| # | 지적 | 판정 | 내용 |
|---|---|---|---|
| 1 | `directReceiptCustody`가 fixture QuantitySegment를 지명하는 confirmReceipt를 모두 운송 수령으로 봤다. 제품은 TRANSIT 장소의 식별된 leaf를 수량 그대로 받을 때만 운송 수령을 받는다. 그래서 C2·T09 `cumulative-versus-state`, T16 `provisional-holds`, T14 수령98은 계약을 지키는 제품이 통과할 수 없었다 | FIXED | **계약**: `contracts/fixture-place-kinds.json` 1.2.0에 `transitReceipt`를 더했다. 운송 수령은 slot에서 fixture QuantitySegment를 지명하거나, 그런 leaf를 나눈 앞선 splitQuantity의 명시 children 자식(`/response/children/<alias>/segmentId`)을 `$result`로 지명하는 confirmReceipt다. 조건은 다음과 같다. leaf는 하나이고 Place.kind가 TRANSIT이다. 식별돼 있다(INDISTINGUISHABLE_MIXTURE 아님). 수령 수량·단위가 leaf와 decimal로 같다. itemId·lotId가 leaf와 같다. 수령 장소는 INTERNAL_STORAGE다. 제품 근거는 `ReceiptStockPrimitives.receive:21`과 `docs/execution/s3-receipt/contracts.md:20-25`다(읽기만 했다). 운송 수령 실물을 뒤에서 예약·출고·이동하면 leaf 보관자가 내부 보관자여야 한다. `contracts/fixture-place-kinds.md:41-43`의 "TRANSIT·EXTERNAL_PORT 확인 수령은 보관자를 이어받는다"를 고치고 "운송 수령" 절과 적용표를 더했다. **강제**: `ContractValidator.receiptCustodyProblems`가 위 조건을 prepare에서 검사한다. **C2·T09**: fixture의 PORT(EXTERNAL_PORT)를 TRANSIT 장소(`이탈리아발 운송 구간`)로 바꿨다. A60·B40의 내부 보관자 warehouse는 그대로다. PORT를 읽는 assertion은 없었다. **T16**: TRANSIT60을 PORT(IT-port)에서 TRANSIT 장소로 옮겼다. 보관자 CUSTODIAN·owner SUPPLIER는 그대로다. 7개 actor grant의 placeAliases도 PORT 대신 TRANSIT을 담는다. PORT를 읽던 `receipt-transit-double-creation-7`(확인 뒤 운송 장소에 남은 활성 실물 0행)은 같은 뜻으로 TRANSIT을 읽는다. **T14 두 subcase**: `shipped` 뒤 warehouse가 Q100을 RECEIVED98·REMAINDER2로 나눈다(`split98`, evidence warehouse-receipt). 수령98은 자식98 전체를 받는다. `transit2`는 수령 응답의 `/response/remainder` 대신 자식2에 운송 중 확인을 기록한다. 분할과 수령이 부모를 retire하므로 `received98`·`physical-total100`·`transit2`의 원행 합계는 활성 행만 읽는다. 전에는 retired 행을 포함하면 100이 아니라 200이 될 수 있었다. 새 assertion은 `split98-applied`(APPLIED), `received-leaf-retired`(자식98 활성 0행), `remainder2-active`(TRANSIT의 자식2 활성 1행)다. 받은 98·운송2·손실0·차이 책임1·합계100의 의미는 그대로다. **전수 점검**: 41 case의 모든 confirmReceipt(start `call` 포함)를 읽었다. fixture leaf를 지명하는 수령은 C2×2·T09×2·T14×2·T16×1, 모두 7개였고 모두 고쳤다. 나머지는 직접 수령이다. 회귀 test: `StepTwoRoundEightRegressionTest.transitReceiptsConsumeAnExactIdentifiedTransitLeaf`. 41 case 문제 0이고, 지적의 모양(C2·T09·T16 leaf를 EXTERNAL_PORT로 되돌림, T14 split 없는 수령98)이 각각 거부된다. 자식 수량 97, children pointer 미해석, lot 불일치, 수령 장소 TRANSIT, 혼합 leaf, 외부 보관자 leaf의 사용도 거부된다. round 7 test가 고정한 C2 receive60 운송 수령은 이제 TRANSIT leaf를 받는다(주석 갱신) |

### 권장(P3)

| # | 지적 | 판정 | 내용·소유자 |
|---|---|---|---|
| 2(a) | 수령 보관자 slot에 양성 대조만 있다. slot을 증거·권한 확인 없이 보관자로 쓰는 제품도 통과한다 | FIXED | confirmReceipt action에 `custodyControl`(SCOPE_INELIGIBLE→REJECTED, EVIDENCE_CONFLICT→HELD, EVIDENCE_UNVERIFIED→HELD) 선언을 허용했다(`contracts/acceptance-case.schema.json`, `directReceiptCustody.custodyControl`). harness 선언이고 `CaseRunner`는 `request`만 보내므로 제품에 가지 않는다. 선언 값은 제품 `ReceiptCommands`가 처음 걸리는 검사여야 한다. 순서는 권한·조직, 원본끼리 상충, 원본이 slot을 지명하지 않음이다. 그 action의 `/response/outcome`·`/response/error/code`를 고정하고, 뒤의 observe에서 segments·receipts count 0으로 효과 0을 단언해야 한다. 반례 수령의 결과는 뒤에서 쓸 수 없다. 원본 수집도 넓혔다. evidenceId·evidenceIds·verifiedEvidenceIds·evidence 등 증거 slot과 evidenceRefs의 alias(이름 문자열 포함)를 센다. `$result`로 인용한 첨부 action은 그 action이 인용한 alias와 inline JSON content의 `receivingCustodianAlias`(content SHA-256 검사)까지 센다. 보관자 actor의 `organizationAlias`가 확인 actor와 다르면 SCOPE_INELIGIBLE이다. **E1 `receipt-custody-unverified`**: full-flow-quantities fixture와 구매·출하 선행 7개 명령을 그대로 쓴다. receipt60의 slot만 procurement로 바꿨다. procurement는 W 수령 권한이 있는 내부 Human이고, 원본 warehouse-60은 receiver를 지명한다. 단언은 HELD, EVIDENCE_UNVERIFIED, W 활성 실물 0, procurement 보관 실물 0, 수령 원장 0, MCP 조회 snapshot이 API 조회와 같음이다. registry는 802 subcase가 됐다. 회귀 test: `declaredCustodyNegativeIsTheProductsFirstFailingCheck`. 반례 수용과 선언 제거·다른 값·미정의 값·outcome/code 고정 제거·효과0 제거·결과 사용·유효한 slot에 선언·다른 capability 선언의 거부를 검사한다. SCOPE_INELIGIBLE(observer)·EVIDENCE_CONFLICT(delivery-proof가 procurement 지명) 반례 수용, verifiedEvidenceIds 원본 인정, 다른 보관자를 지명한 verifiedEvidenceIds, `$result` 첨부 JSON 원본과 그 hash 불일치, 다른 조직 보관자도 검사한다. round 7 test의 E1 양성 대조 검사는 이 반례를 건너뛴다 |
| 2(b) | NO_TASK 최소 1 tick은 watcher가 tick을 봤다고 보장하지 않는다 | FIXED | `HostObservationValidator.naturalTick`: NO_TASK는 `completedAt ≥ observeFrom+2×naturalTickSeconds`이다. 상수는 `NO_TASK_TICKS=2`다. `naturalTickSeconds`는 1–창/2의 정수다. tick 1초면 2초로 30초 창 안이다. extractor는 watcher가 끝난 뒤 읽기 시작해야 한다(`extractor.command.startedAt ≥ command.completedAt`). 그래서 completedAt까지의 행을 모두 본다. `runtimeProfileProblems`는 수동 관찰 fixture의 `tickSeconds`가 창의 절반을 넘으면 거부한다. T26 fixture는 tick 1초라 그대로 통과한다. `host-observation-guide.md`와 `harness-guide.md`에 이유(observeFrom 위상, 고정 지연 주기, 제출 행 기록 시점)를 적었다. 회귀: `noTaskCoversTwoTicksAndReadsRowsUpToCompletion`. 0→2초를 받고, 0→1초(지적의 모양), extractor 1초 시작, tick 16, fixture tick 16을 거부한다. round 7 test와 `HostObservationValidatorTest`의 NO_TASK 표본도 2초·종료 뒤 extractor로 맞췄다 |
| 2(c) | T13 relocation/return 대조가 이동·반품이 없어도 통과한다 | FIXED | return-db·move-db가 segments도 읽는다. sameAs 앞에서 다음을 단언한다. `dispatch-sale-applied`·`return-applied`(APPLIED)와 `returned-at-W`(W 활성 105 = 105−10+10). `move-applied`(APPLIED)와 `relocated-at-W-alt`(W-alt 활성 20), `left-at-W`(W 활성 85)다. 제품 moveQuantity(`docs/execution/s2-inventory/contracts.md`)는 leaf 전체를 옮긴다. 그래서 수령40의 20 이동이 계획 §4.2대로 되도록 이동 전에 warehouse가 receipt40을 MOVE20·STAY20으로 나눈다(`split40`). move는 자식 MOVE20을 옮긴다. 회귀: `t13ObservesTheReturnAndTheMoveBeforeTheirContributionChecks` |

## commit

- `18b2c674` fix(검증): closure review 5의 운송 수령 leaf와 P3 지적을 닫는다
- 이 README: docs commit

## 실행한 checks

환경은 Java 21(zulu 21.0.5), Python 3.14, `env.sh`다. Maven·verify는
`$MULINO_SLOT`으로 한 번에 하나씩 실행했다. `./verify harness`, 첫
`./verify prepare`, contract-red는 commit 직전에 같은 내용의 tree에서
돌렸다. 나머지는 fix commit `18b2c674`의 clean tree에서 돌렸다.

| 명령 | exit | 결과 |
|---|---|---|
| `./verify harness` | 0 | 517 tests, failure/error/skip 0(기준 513 + `StepTwoRoundEightRegressionTest` 4) |
| `./verify prepare` | 0 | PREPARED, codeCommit `18b2c674`, workingTreeDirty=false, 41/802/24009(기준 41/801/23991 + E1 반례 6·T14 6·T13 6 assertion), preparationProblems 0, caseAssetChecks 11개 PASS(generators-reproduce·case-generators-reproduce 포함), knownOpenGaps 34, runtimeGates 1 |
| `./verify coverage` | 2 | NOT_RUN, preparationStatus PREPARED, runtimeStatus NOT_RUN, gateComplete=false, preparationProblems 0, coverageProblems 0, knownOpenGaps 34, caseProfiles 3032, observations 499 |
| `python3 verification/coverage/validate.py` | 0 | VALID, runtimeStatus NOT_RUN, gateComplete=false |
| `./verify contract-red` C2·T09·T13·T14·T16·E1 | 1 | expected=discovered=started=NOT_IMPLEMENTED=39, skipped 0 |
| `python3 -m unittest discover -s verification/coverage -p 'test_*.py'` | 0 | 68 OK(259s), clean tree |
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
| 생성기 제자리 재실행(mcp-tests author_cases.py, C3 author_prerequisites.py, T06 author_contracts.py, platform-tests build_cases.py, T26 author_review_fixes.py 두 번, V4 author_review_fixes.py, model-binding generate.py) | 0 | 실행 전후 `git status`·`git diff` hash 같음(drift 없음) |
| 작성 script 재실행(round 6 author_place_kind_controls.py, round 7 author_receipt_custody.py 두 번, round 8 author_transit_receipts.py 두 번) | 0 | drift 없음. `migrate_place_kinds.py` dry run도 DRY 0 |

NOT_RUN: `./verify regulatory`, `--actual`·제품 runtime, 실제 host
adapter·MCP wire·extractor. 운송 수령 규칙, 보관자 반례, NO_TASK 두 tick,
T13 결과 단언은 계약과 selftest까지만 확인했다. 제품 인수는 `NOT_RUN`이다.

## cross-owner 요청

- Step 3 actual(`verification/harness/src/main/java/org/mulino/verification/actual/**`,
  `verification/actual/**`):
  - case 운송 수령의 leaf slot(`segmentId`·`existingSegmentId`·
    `existingTransitSegmentId`)은 제품 receiveProvisional의
    `transitSegmentId`로 옮긴다. 제품은 confirmReceipt slot이 아니라
    provisional observation에서 leaf를 읽는다.
  - splitQuantity의 명시 `children`(alias·quantity·unit)은 제품
    `quantities[]`로 옮기고, 응답을 `/response/children/<alias>/segmentId`로
    노출한다. 기존 T15와 새 T13·T14 split이 이 모양이다.
  - action field `custodyControl`은 harness 선언이므로 제품에 보내지 않는다.
    E1 `receipt-custody-unverified`는 round 7 요청대로 warehouse-60 원본과
    event payload에 receiver를 설치해야 반례가 된다.
  - 수동 watcher host adapter는 NO_TASK를 보고하려면
    `observeFrom+2×naturalTickSeconds`까지 관찰하고, extractor를 watcher 종료
    뒤에 실행해야 한다.
  - round 6·7 요청(FixtureInstaller kind 기본값 제거, native fixture kind·
    보관자, observeFrom 소비, receivingCustodianAlias 설치)은 그대로 남아 있다.
- Step 1 skill 소유자(GPT-6.1 Sol high):
  `.agents/skills/ontology-scenario-testing/references/repository-harness.md`
  의 다음 문장이 낡았다. 고쳐야 한다.
  - 98행 "E1·T13 첫 수령의 `receivingCustodianId` slot도 receipt 계약과 함께
    정해야 한다": 운송 수령 규칙(`transitReceipt`), 직접 수령 보관자 규칙,
    `custodyControl` 반례 선언으로 바꾼다.
  - 240–250행의 자연 tick 설명에 NO_TASK의 두 tick 관찰과 extractor 실행
    시점을 더한다.
- Step 3 backend: 요청 없음. `ReceiptStockPrimitives`·`ReceiptCommands`·
  moveQuantity의 현재 규칙에 fixture를 맞췄다.

## 하지 않은 것

- 제품 실행, `--actual`, `./verify regulatory`.
- backend·`verification/actual/**`·`.agents/skills/**` 변경(위 요청).
- confirmReceipt 밖 명령의 전수 점검. leaf 일부를 split 없이 이동·보류·
  출고하는 명령(계획 §4.2)이 있는지는 보지 않았다. T13 move만 split을
  앞세웠다. 예를 들어 T16 qc-hold는 수령60 중 20을 보류한다. 제품
  placeHold가 부분 수량을 받는지에 따라 같은 종류의 문제가 될 수 있다.
  소유자: Step 2 tests worker(Claude Opus high) 다음 round.
- round 7의 DEFERRED 두 항목(QUERY probe 면제의 raw surface 대조,
  placeKindProblems 사각지대)은 이번 brief 범위 밖이라 그대로다.
