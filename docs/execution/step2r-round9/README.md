# Step 2 재검토 9라운드: closure review 6 지적 처리 기록

Step 2 closure review 6(Claude Opus xhigh `step2-closure-6-opus.json`,
GPT-6-Astra `step2-closure-6-astra.json`)은 둘 다 같은 P2로 FAIL이었다.
이 기록은 그 P2와 P3 세 건, 그리고 전수 점검에서 찾아 coordinator
follow-up으로 같은 round에 닫은 P2급 두 건의 처리다. 기준 commit은 `b2c6d3a6`, branch는
`step2r/round9`, worktree는
`/Volumes/VideoStore/Developer/mulino-ontology-step2r-round9`다. 작업
모델은 사용자 Step 2 지정대로 Claude Opus high다.

손댄 범위는 다음과 같다.

- `contracts/fixture-place-kinds.{json,md}`(1.3.0), `contracts/acceptance-case.schema.json`
- `verification/cases/{T05,T13}/**`(case·fixture·feature·README/observations)
- follow-up: `verification/cases/{T16,T26}/**`, fixture 13개 파일의 배분 14개(C1, C3,
  T04×4, T08, T24, V3×3, V7 두 파일), 생성기 `T26/author_review_fixes.py`·
  `T06/author_contracts.py`, `contracts/acceptance-fixture.schema.json` 설명
- harness의 `actual/` 밖 Java(`ContractValidator`, `HostObservationValidator`,
  `Main`)와 test
- `verification/harness-guide.md`, `verification/host-observation-guide.md`
- 이 디렉터리의 작성 script `author_pick_before_dispatch.py`,
  `author_pick_followup.py`

손대지 않은 범위는 `backend/**`, `verification/actual/**`,
`verification/harness/src/main/java/org/mulino/verification/actual/**`
(Step 3), `.agents/skills/**`(Step 1)다. backend는 읽기만 했다
(`FulfillmentCommands`, `FulfillmentStockPrimitives`, `ReceiptCommands`,
`TradeEvidence`, `DomainError`, `application/runtime/*Sweeper`).

## 항목별 처리

### 필수(P2)

| # | 지적 | 판정 | 내용 |
|---|---|---|---|
| 1 | T13 `partial-excess-return-relocation`과 T05 `manager-disposition`이 실행 중 예약한 배분을 pick 없이 출고하고 APPLIED를 단언한다. pickQuantity를 가진 actor도 없다. 계약을 지키는 제품은 출고를 거부하므로 T13의 출고·인도·반품 단언과 T05의 출고 단언이 실패한다 | FIXED | **제품 근거**(읽기만): `FulfillmentCommands.prepare` 38행은 `pickedAt`이 없으면 출고를 `DomainError.invalid("Pick before dispatch required")`(REJECTED TYPE_INVALID)로 거부한다. `FulfillmentStockPrimitives.dispatch` 22행도 같다. `pickedAt`은 `FulfillmentStockPrimitives.pick` 20행만 기록하고 배분 revision을 1 올린다. adapter는 pick을 만들지 않는다(`harness-guide.md` 395행 부근). **T13**: sales의 roleCapabilities·grant.actions에 pickQuantity를 더했다. sales는 이미 예약·출고하는 actor다. reserve와 dispatch-sale 사이에 `pick`을 넣었다(allocationId = reserve `/response/allocationId`, expectedRevision = reserve `/response/revision`). dispatch-sale의 expectedRevision은 pick `/response/revision`을 쓴다. `pick-applied`(APPLIED)를 dispatch-sale-applied 앞에 단언하고 README oracle 행에 더했다. **T05**: warehouse의 role·grant에 pickQuantity를 더했다. warehouse는 그 fixture에서 예약·출고 권한을 이미 가진 물리 작업자다. `pick`은 reserved-db 뒤에 둬서 EXECUTABLE 예약 관찰(14·15)이 그대로다. 일반 출고의 expectedRevision은 pick을 잇는다. `pick-before-dispatch-applied`(APPLIED)를 16 앞에 단언하고 observations.md에 더했다. pick은 실행 단계이며 RESERVE_OR_DISPATCH_APPROVAL이 아니므로 18(새 승인 0)도 그대로다. **전수 점검**: 41 case의 모든 dispatchQuantity(start `call`, parallel branch 포함)를 읽었다. 실행 중 배분(`$result`, T26처럼 request 최상위 allocationId 포함)을 출고하는 것은 42개다. 수정 뒤 31개는 같은 배분의 pick이 앞선다(T05 1, T13 1, T09 2, T11 1, T17 8, T18 4, C2 1, C4 4, E1 4, E2 5). 그중 수정 전에 pick 없이 적용을 기대한 것은 T13·T05 둘뿐이었다. 적용을 기대하는 나머지는 모두 revision이 pick이나 그 뒤 조회를 잇는다(규칙이 41 case에서 문제 0). pick 없던 11개(T16 expiry-sweeper·expiry-delayed-sweep, T26 9개)는 거부 반례였고 follow-up 2에서 pick과 code를 더했다. **강제**: `ContractValidator.pickBeforeDispatchProblems`를 `./verify prepare`에 연결했다. 앞선 action의 `$result`인 allocationId를 출고하면서 적용을 기대하면(outcome APPLIED 고정, 또는 뒤의 action·assertion이 결과의 다른 부분을 읽음) 같은 `$result`(actionId·pointer)를 지명하는 앞선 pick을 요구한다. 그 pick을 APPLIED 밖으로 고정하거나, 출고 expectedRevision이 pick보다 앞선 action의 `$result`면 문제다. 회귀: `StepTwoRoundNineRegressionTest.dispatchOfARuntimeAllocationIsPrecededByItsPick`. 41 case 문제 0이다. 지적의 모양(T13·T05에서 pick 제거)이 거부된다. 고정 assertion 없이 delivery가 결과를 읽는 경우, 다른 배분 pointer의 pick, 출고 뒤 pick, pick 전 revision, REJECTED로 고정한 pick도 거부된다. 결과를 아무도 읽지 않는 거부 반례는 받는다 |

### 권장(P3)

| # | 지적 | 판정 | 내용·소유자 |
|---|---|---|---|
| 2(a) | custodyControl 분류가 확인 조직 밖·actor 아닌 slot을 SCOPE_INELIGIBLE로 둔다. 제품은 `identity.actor(c.organizationId(),custodian).orElseThrow(DomainError::forbidden)`(ReceiptCommands 71행)로 REJECTED FORBIDDEN을 낸다 | FIXED | `custodianProblems`에 FORBIDDEN 묶음을 맨 앞에 두었다. slot이 fixture actor가 아니거나, actor·alias의 `organizationAlias`가 확인 actor와 다르면 FORBIDDEN이다. 그다음 SCOPE_INELIGIBLE(Human/Agent 아님, confirmReceipt role·grant나 장소 scope 없음)이다. 계약 `custodyControl.values`에 `FORBIDDEN: REJECTED`를 더했다(fixture-place-kinds.json 1.3.0). schema enum과 `.md`·`harness-guide.md`의 순서 설명도 고쳤다. 회귀: `custodianOutsideTheConfirmingOrganizationIsForbidden`. ORG-B receiver에 SCOPE_INELIGIBLE을 선언하면 "first failing … (FORBIDDEN" 문제다. FORBIDDEN 선언(REJECTED/FORBIDDEN 고정)은 ORG-B receiver와 actor 아닌 SUPPLIER에서 받는다. 고정이 없으면 거부하고, 같은 조직의 권한 없는 observer에 FORBIDDEN을 선언하면 SCOPE_INELIGIBLE이 먼저라 거부한다. round 8 test의 "정의되지 않은 값" 표본은 FORBIDDEN 대신 TYPE_INVALID를 쓴다 |
| 2(b) | EVIDENCE_CONFLICT·EVIDENCE_UNVERIFIED가 요청 evidenceRefs까지 센다. 제품은 수령 canonical occurrence의 검증된 chain(`TradeEvidence.verifiedCanonical`)에서만 보관자를 읽는다 | FIXED | `originals`가 verification basis만 센다. 계약 `directReceiptCustody.verificationBasisSlots` = evidenceId·evidenceIds·verifiedEvidenceIds다. 거기에 basis slot이 `$result`로 인용한 첨부 action의 document·basis slot과 inline JSON content를 더한다. 같은 `canonicalOccurrenceKey`의 앞선 confirmReceipt(중복 출처)의 basis도 센다. 요청 evidenceRefs와 다른 slot은 세지 않는다. 그 결과 T13 receipt60·40·5는 원본 warehouse-receipt를 evidenceRefs에만 인용해 양성 대조가 깨진다. 그래서 같은 원본을 `evidenceId` slot에도 둔다(evidenceRefs는 그대로). E1은 이미 evidenceId를 쓴다. 회귀: `onlyVerificationBasesOfTheCanonicalOccurrenceNameTheCustodian`. evidenceRefs에만 둔 warehouse-60은 원본이 아니다. qc를 지명한 증인 delivery-proof는 상충을 만들지 않는다. T13에서 evidenceId를 빼면 미확인이다. 중복 출처의 basis가 qc를 지명하면 상충이다. round 8 test의 EVIDENCE_CONFLICT 표본은 delivery-proof를 verifiedEvidenceIds basis로 바꿨다. 같은 문서가 evidenceRefs에만 있으면 첫 실패가 EVIDENCE_UNVERIFIED여서 CONFLICT 선언이 거부됨도 단언한다 |
| 2(c) | NO_TASK 두 tick은 처리 시간이 한 tick을 넘으면 완료된 scheduler 주기를 증명하지 못한다. guide가 과장했다 | FIXED(하네스), 실행은 Step 3 대기 | `HostObservationValidator.naturalTick`: NO_TASK는 extractor `rawRows.schedulerCycles[]`(schedulerId, tickId/sweepId, startedAt, completedAt, startedBy)를 요구한다. `startedBy=SCHEDULER_LOOP`이고 `startedAt ≥ observeFrom`, `completedAt ≤ watcher completedAt`인 주기가 하나 이상 있어야 한다. 없으면 미완료 관찰이며 NO_TASK가 아니다. 두 tick 하한과 watcher 종료 뒤 extractor 규칙은 그대로 둔다. `host-observation-guide.md`의 "두 주기를 관찰하면 창 안에서 적어도 한 tick이 시작부터 기록까지 끝난다"를 지우고, 반례(+1.2초 완료, +2.2초 제출, +2초 watcher 종료)와 새 기록 표를 적었다. Java 주석의 "whatever … processing delay"도 고쳤다. 회귀: `noTaskNeedsASchedulerRecordedCycleCompletedInsideTheWatch`. 구간 안 완료 주기는 받는다. 기록 없음, observeFrom 전 시작(지적의 모양), watcher 뒤 완료, startedBy HARNESS, 역순 시각, 다른 scheduler는 거부한다. round 7 `noTask` 표본과 `HostObservationValidatorTest`의 유효 NO_TASK 표본에 주기 행을 더했다. backend `application/runtime`의 sweeper·scheduler에서는 주기 완료 기록을 찾지 못했다. 그래서 실제 NO_TASK 관찰은 Step 3가 기록과 extractor를 제공할 때까지 fail-closed다(cross-owner 요청) |

### follow-up(coordinator, P2급): 전수 점검에서 찾은 두 건

처음에는 brief 범위 밖이라 DEFERRED로 기록했다. coordinator가 둘 다 P2급
(계약을 지키는 제품이 실패하거나 틀린 제품이 통과한다)이라 같은 round에서
닫으라고 했다.

| # | 지적 | 판정 | 내용 |
|---|---|---|---|
| F1 | fixture 배분(`$alias`)은 picked 상태를 선언하지 않는다. 제품은 pickedAt 없는 배분의 출고를 검사 대상 규칙보다 먼저 거부하므로 C3 `*-dispatchQuantity/authorized-same-input`×3(APPLIED), V7 `authorization-effect-first`(await APPLIED)·`restart-after-revoke/prior-dispatch`(APPLIED)와 철회 경합, T24 `audit-rollback-and-retry` fail(AUDIT_PERSISTENCE_FAILED)·retry(APPLIED), T04 rollback 4개(TRANSACTION_ROLLED_BACK), V3 `hold-first`·`dispatch-first` 경합이 실패할 수 있다 | FIXED | **선택**: fixture 선언 경로다. 이 배분들은 case 밖에서 예약된 설치 상태(state·revision을 fixture가 이미 적는다)이고, subcase는 출고 준비가 끝난 배분의 인가(C3 reader/delegator 같은 입력, T08, V7 철회), rollback fault 지점(T04), 감사 실패(T24), lock 경합(V3), 근거 철회(C1)를 시험한다. pick action을 넣으면 그 전후 효과 창과 경합 안에 쓰기·fence·revision 변화가 생긴다. 그래서 출고하는 모든 fixture 배분 14개가 state를 적은 자리(alias, `baseline.priorEntities`, `baseline.allocations`/`allocation` 행)에 `pickedAt`(fixture clock asOf, 지난 사실)과 `pickedByAlias`(그 fixture에서 출고하는 actor: C3는 delegator, 나머지는 warehouse)를 선언한다. 손으로 쓴 13개(C1, C3 fixture-dispatchQuantity, T04×4, T08, V3×3, V7 fixture·restart-fixture의 ALLOCATION·ALLOCATION2)는 `author_pick_followup.py`가 쓴다. C3 post-processor는 이 fixture를 쓰지 않는다. T24는 생성기 `T06/author_contracts.py`의 배분 행에 넣고 재생성했다. V3 `late-v1-release`의 SUSPENDED 배분도 pick된 뒤 정지된 것으로 선언해, 거부 이유가 정지뿐이다. **강제**: `pickBeforeDispatchProblems`가 fixture 배분 출고마다 fixture pick 선언이나 앞선 pickQuantity를 요구한다. pickedAt은 ISO instant이며 fixture clock knownAt 이전이고, pickedByAlias는 fixture actor다. 선언 뒤에 실패로 고정하지 않은 두 번째 pick은 제품이 'Allocation already picked'로 거부하므로 문제다. 계약 설명은 `acceptance-fixture.schema.json` aliases와 `harness-guide.md`에 있다. 회귀: `fixtureAllocationsArePickedAndExpiryNegativesOnlyFailForTheExpiry`. 9개 대표 subcase가 선언 그대로 통과하고, 선언을 지우면 모두 "has no picked state"로 거부된다. knownAt 뒤 시각, instant 아님, actor 아닌 picker, 두 번째 pick도 거부되고, 선언 대신 명시 pick action은 받는다 |
| F2 | T26 만료 guard 반례 9개는 pick 없이 출고하고 `guard-outcome` REJECTED만 고정한다. T16 `expiry-sweeper`의 출고는 아무것도 고정하지 않는다. 만료 guard가 없는 제품도 pick 누락(TYPE_INVALID)으로 거부해 통과한다 | FIXED | **제품 code**(읽기만): grant 만료는 출고 prepare의 scope 인가(`FulfillmentCommands` 28행 `auth.authorizeScopes`, `ReadAuthorizer`)에서 FORBIDDEN이다. LOT 만료·처분 근거 만료·적격 정책 만료(`QualityEligibility` 25–29행, 정책 미해결이면 허용 범위 없음)는 38행의 정지 배분('Suspended allocation cannot execute', sweep 뒤) 또는 현재 판매 범위('Current exact sale permission denied', sweep 전) 검사에서 INSUFFICIENT_ELIGIBLE_QUANTITY다. 둘 다 pick 검사보다 앞이다. revision 대조(`ApplicationCommands` 76행)는 prepare 뒤다. **T26**: 생성기 `T26/author_review_fixes.py`에 `pick_before_guard`를 더했다. 기본 8개 subcase에서 warehouse fixture에 pickQuantity grant를 주고, reserve 직후(만료 경계 09:00:10 전, `before` baseline 전) pick하며, 출고 expectedRevision은 pick을 잇는다. `guard-pick-applied`(APPLIED)와 `guard-code`(grant FORBIDDEN, 나머지 INSUFFICIENT_ELIGIBLE_QUANTITY)를 `guard-outcome` 앞뒤에 둔다. `lot-expiry-autonomous-loop`는 같은 생성기가 lot-expiry-no-event에서 다시 파생해 이어받는다. feature와 `oracle-bindings.json`도 재생성했다. **T16**: `author_pick_followup.py`가 두 subcase에 warehouse pick(reserve 뒤, reserved-db baseline 전)과 `pick-applied`를 넣는다. expiry-sweeper 출고는 `dispatch-after-sweep-rejected`(REJECTED)·`dispatch-after-sweep-code`(INSUFFICIENT_ELIGIBLE_QUANTITY)로 고정한다. **강제**: pick 안 된 실행 중 배분 출고는 APPLIED 밖 outcome과 pick 검사 전 code(STALE_REVISION·FORBIDDEN·INSUFFICIENT_ELIGIBLE_QUANTITY·SCOPE_INELIGIBLE·VERSION_UNSUPPORTED)를 함께 고정한 반례일 때만 받는다. 비동기 출고는 await action의 assertion에서 고정을 읽는다. 회귀: 같은 test가 T26 9개의 pick 위치·APPLIED·code·revision·grant를 확인한다. pick과 code를 지우면 거부되고, code만 남기면 받는다(그 code는 pick 검사보다 먼저 나오므로 반례가 여전히 guard를 가려낸다). T16 expiry-sweeper도 같다. `dispatchOfARuntimeAllocationIsPrecededByItsPick`의 T05 반례 표본은 code 미고정·TYPE_INVALID 고정을 거부하고 FORBIDDEN 고정을 받는다 |

## commit

- `dff7b63a` fix(검증): closure review 6의 pick 없는 출고와 P3 지적을 닫는다
- `b1a37748` docs: Step 2 round 9의 처리 판정과 실행 증거, cross-owner 요청을 기록한다
- `e3b4d084` fix(검증): fixture 배분의 pick 상태와 만료 반례의 pick·code를 고정한다
- 이 README 갱신: docs commit

## 실행한 checks

환경은 Java 21(zulu 21.0.5), Python 3.14, `env.sh`다. Maven·verify는
`$MULINO_SLOT`으로 한 번에 하나씩 실행했다. 아래는 follow-up 뒤의 최종
값이다. `./verify harness`, 첫 `./verify prepare`, contract-red는 follow-up
commit `e3b4d084` 직전에 같은 내용의 tree에서 돌렸다. 나머지는
`e3b4d084`의 clean tree에서 돌렸다. 첫 수정 `dff7b63a` 시점 값은 괄호에
적었다.

| 명령 | exit | 결과 |
|---|---|---|
| `./verify harness` | 0 | 522 tests, failure/error/skip 0(기준 517 + `StepTwoRoundNineRegressionTest` 5; `dff7b63a` 때 521) |
| `./verify prepare`(commit 전) | 0 | PREPARED, 41/802/24033, preparationProblems 0, workingTreeDirty=true |
| `./verify prepare`(clean) | 0 | PREPARED, codeCommit `e3b4d084`, workingTreeDirty=false, 41/802/24033(기준 24009 + T05·T13 pick 2 + T26 9×2 + T16 4), preparationProblems 0, caseAssetChecks 11개 PASS(generators-reproduce·case-generators-reproduce 포함), knownOpenGaps 34, runtimeGates 1(`dff7b63a` 때 24011) |
| `./verify coverage` | 2 | NOT_RUN, preparationStatus PREPARED, runtimeStatus NOT_RUN, gateComplete=false, preparationProblems 0, coverageProblems 0, knownOpenGaps 34, caseProfiles 3032, observations 499 |
| `python3 verification/coverage/validate.py` | 0 | VALID, runtimeStatus NOT_RUN, gateComplete=false |
| `./verify contract-red` T05·T13·E1(`dff7b63a` 직전) | 1 | expected=discovered=started=NOT_IMPLEMENTED=24(T05 3, T13 17, E1 4), skipped 0 |
| `./verify contract-red` T04·T08·T16·T24·T26·C1·C3·V3·V7(`e3b4d084` 직전) | 1 | expected=discovered=started=NOT_IMPLEMENTED=393(T04 9, T08 23, T16 4, T24 13, T26 25, C1 3, C3 309, V3 3, V7 4), skipped 0 |
| `python3 -m unittest discover -s verification/coverage -p 'test_*.py'` | 0 | 68 OK(260s), clean tree |
| `python3 -m unittest discover -s verification/requirements -p 'test_*.py'` | 0 | 37 OK(`test_case_generators_reproduce` 포함, T06 생성기 재현) |
| `python3 -m unittest discover -s verification/mcp-tests -p 'test_*.py'` | 0 | 3 OK(T26 post-processor fixed point 포함) |
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
| 생성기 제자리 재실행(mcp-tests author_cases.py, C3 author_prerequisites.py, T06 author_contracts.py, platform-tests build_cases.py, T26 author_review_fixes.py 두 번, V4 author_review_fixes.py, model-binding generate.py) | 0 | 실행 뒤 `git status --porcelain` 0줄(drift 없음) |
| 작성 script 재실행(round 6 author_place_kind_controls.py, round 7 두 번, round 8 두 번, round 9 author_pick_before_dispatch.py·author_pick_followup.py 각 두 번) | 0 | drift 없음. `migrate_place_kinds.py` dry run도 DRY 0 |

NOT_RUN: `./verify regulatory`, `--actual`·제품 runtime, 실제 host
adapter·MCP wire·extractor. pick 선행 규칙, fixture pick 선언, 만료 반례의
code, FORBIDDEN 분류, basis 원본 규칙, NO_TASK 완료 주기 규칙은 계약과
selftest까지만 확인했다. 제품 인수는 `NOT_RUN`이다.

## cross-owner 요청

- Step 3 actual(`verification/harness/src/main/java/org/mulino/verification/actual/**`,
  `verification/actual/**`):
  - T13·T05의 새 `pick`은 제품 pickQuantity에 allocationId만 보낸다(제품
    slot 허용 목록이 allocationId 하나다). 응답 `/response/revision`을
    출고 expectedRevision으로 노출한다. 출고의 occurredAt은 pick의
    pickedAt보다 앞설 수 없다(`requireContinuousAuthority`).
  - confirmReceipt의 basis slot(evidenceId·evidenceIds·verifiedEvidenceIds)이
    지명한 DocumentVersion을 그 수령 canonical occurrence의 검증된 chain
    (Verification basis)으로 설치한다. 요청 evidenceRefs의 증인은 basis로
    설치하지 않는다. T13 warehouse-receipt가 세 수령의 basis다.
  - 수동 watcher host adapter는 NO_TASK를 보고하려면 extractor
    `rawRows.schedulerCycles[]`(schedulerId, tickId/sweepId, startedAt,
    completedAt, startedBy)를 scheduler가 스스로 남긴 주기 완료 기록에서
    읽어야 한다. backend scheduler/sweeper가 그 기록을 남겨야 한다. 지금
    backend에서는 찾지 못했다.
  - FixtureInstaller는 fixture Allocation의 `pickedAt`·`pickedByAlias`(alias,
    `baseline.priorEntities`, `baseline.allocations`/`allocation` 행)를 그 배분의
    pickedAt으로 설치한다(F1). C3·V7·T08의 priorEntities 배분은 segment·수량·
    state도 그 자리에 있다.
  - T26·T16의 새 pick은 제품 pickQuantity에 allocationId만 보내고, 출고
    expectedRevision은 pick 응답 revision이다.
  - round 6–8 요청은 그대로 남아 있다.
- Step 1 skill 소유자(GPT-6.1 Sol high):
  `.agents/skills/ontology-scenario-testing/references/repository-harness.md`
  240–250행의 자연 tick 설명에 NO_TASK의 완료 주기 기록
  (`rawRows.schedulerCycles`) 요구를 더한다. round 8 요청(98행 수령
  보관자 문장, 두 tick·extractor 시점)도 남아 있다. 수령 보관자 문장에는
  FORBIDDEN 분류와 verification basis 원본 규칙을 함께 반영한다.
- Step 3 backend: 요청 없음. 제품 규칙(pick 선행, FORBIDDEN 조회, 검증
  chain 보관자)에 case와 harness를 맞췄다. 주기 완료 기록은 위 actual
  요청과 함께 다룬다.

## 하지 않은 것

- 제품 실행, `--actual`, `./verify regulatory`.
- backend·`verification/actual/**`·`.agents/skills/**` 변경(위 요청).
- fixture pick을 설치하는 FixtureInstaller(위 Step 3 요청).
- T26 README의 guard 설명은 고치지 않았다. 새 단언의 의미는 case의
  oracleExplanation과 이 기록에 있다.
- round 7의 DEFERRED 두 항목(QUERY probe 면제의 raw surface 대조,
  placeKindProblems 사각지대)은 이번 brief 범위 밖이라 그대로다.
