# Step2 재검토 3라운드 cases3 worker 실행 기록

- 사용자 Step 2(tests) 재검토 3라운드, Claude Opus high. Task tag
  `step2r3-baseline`(=`3f51bed0`)에서 branch `step2r/cases3`, worktree
  `/Volumes/VideoStore/Developer/mulino-ontology-step2r-cases3`.
- 입력: `/Volumes/VideoStore/Developer/.mulino-tools/step2-closure-1.json`과
  `step2-closure-1-todo.json`(confirmedNew·partial·p3).
- 소유: `verification/cases/**`(V4/V6/V7/C3 observation-bindings와 그 binding
  생성기 제외), `verification/mcp-tests/**`,
  `verification/model-binding/semantic-paths.json`, 새
  `contracts/audit-observation-fields.json`(+`.md`).
- 소유 밖을 건드린 곳: `verification/platform-tests/build_cases.py`의 V8 감사
  단언 두 줄. V8 case 파일의 유일한 생성기라 case 소유 범위의 변경을
  생성기로 다시 만들었다. T23 출력은 바뀌지 않았다. C3 post-processor가
  `C3/observation-bindings.json`의 `caseHash` 한 줄을 다시 썼다(아래 참고).
- 제품 runtime·실제 모델·client·BTP·규제 검토는 실행하지 않았다. 모두
  NOT_RUN이다. registry는 바꾸지 않았다(subcase 수 799 그대로, 새 subcase 없음).

## 항목별 판정

| 항목 | 판정 | 요약 |
|---|---|---|
| 1 T26 자율 loop와 harness tick 공존 | DONE | 자율 fixture 3개 분리, process 재시작과 수동 watcher를 한 `parallel`로 시작 |
| 2 V2 reserve-commits-first | DONE | 자식별 배분 위치 고정 2개, retired-parent probe를 ORDER3·STALE_REVISION으로 진단화, promiseCoverage 정의와 원행 직접 대조 |
| 3 T20 host-allowed-tools-write | DONE | 모델 시도 요구 삭제, scripted `tools/call reserveQuantity` 결정적 거부 4단언 |
| 4 감사 raw-row 어휘 | DONE | `contracts/audit-observation-fields.json`·`.md`, 440개 감사 단언을 정규 이름으로, 생성기 3개 수정 |
| 5a C3 closeRecall 선행 사슬 | DONE | 단계마다 recall을 다시 읽어 그 revision을 보낸다 |
| 5b clientInfo·공식 code | DONE | wire-missing-meta -32602 고정, wire-invalid-client-info HTTP 400 |
| 5c requestState TTL600 | DONE(fixture) / CROSS_OWNER(s0-protocol.md) | 생성기 상수 하나에서 fixture·두 경계 clock 계산 |
| 5d V3 apply-old-release | DONE | APPLIED·외부대기 아님, OLD_HOLD RELEASED 아님, APPLIED 감사0 |
| 5e T25 exit code | DONE | 전체 artifact exitCode=0·status=PASS 보편 단언 2개 |
| 5f T25 PREPARATION 입력 결속 | DONE(case) / CROSS_OWNER(guide·validator) | 준비 보고 commit=현재 checkout, 두 tree clean |
| 6 vocabulary 정렬과 검사 도구 | DONE + 추가 요청 34건 | T01 pair·T11·E2·T18·T03·V2 정렬, 의무 kind 정렬, `check_vocabulary.py` |
| harness 회귀 테스트 갱신 | CROSS_OWNER | 6개 테스트 파일 10건, 검증한 patch 첨부 |

## 1. T26: 자율 loop와 harness tick

closure 검토의 확정 부분은 fixture가 모든 T26 subcase에
`pausedUntilTickControl=true`를 설치하고 그 의미가 어디에도 정의되지
않았다는 점이었다. loop가 그 flag를 지키면 자율 subcase는 영원히
NO_TASK이고, 무시하면 재시작 직후 첫 제출이 watcher보다 먼저 나와
`HostObservationValidator`의 "submittedAt은 watcher command 구간 안"을
어긴다.

`verification/cases/T26/author_review_fixes.py`를 고쳤다.

- 자율 subcase 세 개는 각자의 fixture(`fixtures/<subcase>.json`)를 쓴다.
  base fixture와 같고 `runtimeProfile.controlledTicks=false`,
  `pausedUntilTickControl=false`만 다르다. harness tick 계열 fixture는 그대로다.
- clock 전진에 반응할 수 있는 loop process를 모두 전진 전에 멈춘다.
  lot-expiry는 sweeper만 멈췄으므로 scheduler stop/start를 더했다.
  orphan-intake의 restart는 stop(전진 전)과 start(전진 뒤)로 나눴다.
- 전진 뒤 process start들과 수동 watcher를 `parallel` action
  `restart-while-observing` 하나로 묶는다. branch 0이 watcher, branch 1이
  start들이다. loop는 process가 뜬 뒤에만 제출할 수 있다.
- `autonomous-within-30s`의 기준을 scheduler(lot-expiry는 due-sweeper)
  start command의 `/data/hostObservation/command/startedAt`으로 바꿨다.
  예전 `processObservation.completedAt`은 RUNNING 관찰이 첫 tick보다 늦을 때
  올바른 제품도 음수 경과로 실패시킬 수 있었다.
- self-check가 parallel 구조와 restart 부재를 확인한다.
  `verification/cases/T26/README.md`에 두 flag의 의미표와 남은 한계를 적었다.

남은 한계: parallel은 barrier가 아니다. watcher host command가 process
기동보다 늦게 시작되면 첫 제출을 놓쳐 NO_TASK가 되고 subcase는 PASS가
아니다. 잘못된 PASS 경로는 없다.

## 2. V2 reserve-commits-first

- `final-alloc40-on-child40`, `final-winner20-on-child20`: 실행 배분의
  segmentId를 같은 snapshot의 40·20 BOX 자식 id와 `sameAs`로 고정한다.
  이관 index를 무시하고 ALLOC40을 20 자식에 얹는 제품은 실패한다(계획 §4.2
  기존 배분 1회 이관·동일 실물 초과 금지). 40 자식이 ALLOC40으로 차므로 올바른
  배치는 하나뿐이다.
- retired-parent probe는 미충족 주문량10의 새 주문 ORDER3(업무 S3·약속
  PROMISE3, fixture에 추가)을 노린다. 예전 ORDER는 이미 ALLOC40으로 덮여
  retired 여부와 무관하게 거부됐다. vocabulary는 소모된 부모 재사용을
  `CONFLICT/STALE_REVISION`으로 정한다(`StockPrimitives.leaf`, renamedCodes).
  그래서 두 경합 subcase 모두 outcome CONFLICT와
  `retired-parent-reconsumption-code`=STALE_REVISION을 고정했다. retired
  부모를 살아 있는 60으로 보는 제품은 APPLIED나 다른 code로 드러난다.
- promiseCoverage: `V2/race-observation-contract.md`에 "제품 projection이
  아니라 같은 snapshot의 allocations·obligations 원행에서 observer가 만든
  파생"이라고 규칙을 적었다. `shortage-obligation-row-quantity`가 정정
  응답의 obligationId 원행 quantity·unit을 직접 읽어 부족 행과 대조한다.
- 퇴역 outcome `PENDING_EXTERNAL`의 notEquals 단언을 지웠다.
  `V2/cases_b_invariants.py`는 exclusion 목록을 vocabulary outcome에서
  계산하고 새 단언·ORDER3 대상도 요구한다.

## 3. T20 host-allowed-tools-write

`write-attempt-observed`·`write-refused-by-server`는 모델이 실제로 쓰기를
시도해야 성립했다. grant를 확인하고 거절하는 올바른 client가 실패했다.
`verification/mcp-tests/author_cases.py`에서 두 단언을 지우고, 같은
readAgent 주체의 scripted `tools/call reserveQuantity`(action
`scripted-write`)를 probe 뒤에 둔다. HTTP 200, structuredContent outcome
REJECTED, code FORBIDDEN, isError=true를 고정한다. 모델 실행에는 쓰기
적용·외부전달·승인대기0만 남긴다. 전후 원행 불변은 두 행동을 모두 덮는다.

## 4. 감사 raw-row 어휘

`contracts/audit-observation-fields.json`은 `backend/db/commands.cds`의
`CommandAudits`(+`CommandRecords` join, `auditFactsJson` key)에서 이름을 가져왔다.
명령 감사 `audit`와 조회 감사 `queryAudit`를 분리했다. 조회 행에는 멱등키가
없어 같은 source에 두면 `AssertionEngine`의 where 비-null 규칙 때문에 모든
명령 감사 filter가 올바른 제품에서도 실패한다. presence(ALWAYS·CONDITIONAL·
PENDING)와 where 규칙, 바뀐 이름표는 `contracts/audit-observation-fields.md`에 있다.

정규화한 곳(감사 단언 440개 전부가 이제 계약 이름이다).

- 손으로 관리하는 case: C1, C5, E1, E2, T03, T04, T05, T07, T09, T10, T11,
  T13, T14, T16, T19, V2, T26(base subcase). `commandKey`→`commandIdempotencyKey`,
  `action`→`capabilityId`, `id`→`auditId`. T07은 `outcome=TYPE_INVALID`를
  `[key, REJECTED, TYPE_INVALID]`로, T26은 `result=FORBIDDEN`을
  outcome REJECTED·errorCode FORBIDDEN으로, `basisEvidenceId`를 `evidenceIds`로.
  E1의 `authorizationResult/businessOutcome`은 `expectedDenial=true`·
  `outcome=APPLIED` 0건으로. V2 observer derivation(`derivedContenderOutcome`)
  의 where도 바꿨다.
- 생성기: T22/T24는 `T06/author_contracts.py`(kind DENIAL/MUTATION/QUERY를
  outcome·key·queryAudit로), C3는 `author_prerequisites.py`(read-audit를
  queryAudit로, 관찰 source에 queryAudit 추가), V8은
  `platform-tests/build_cases.py`(`denialAudit/0/outcome=FORBIDDEN`을
  queryAudit `[REJECTED, FORBIDDEN]`으로).

## 5. 부분 해소 원 항목

- C3 closeRecall: notice·recovery·disposal 앞에 recall 재조회
  (`precondition-recall-after-approval|notice|recovery`)를 넣고 각 단계가 직전
  재조회의 revision을 보낸다. 승인·통지·회수가 recall revision을 올리는
  설계와 올리지 않는 설계가 모두 통과한다. disposal은 기존처럼
  recordRecovery DISPOSED이다(closure 검토가 계획 §196·s4-recall 계약과
  맞다고 판정).
- T20 wire: `_meta` 전체 누락은 필수 field 누락이므로 400·-32602로
  고정했다(2026-07-28 basic/index "Per-request protocol fields", 직접 확인).
  header와 비교할 body 값이 없어 -32020으로 보지 않는다. clientInfo 형식
  오류에 HTTP 400을 더했다.
- TTL: `MRTR_TTL_SECONDS` 하나에서 fixture `baseline.mrtr`와 09:09:59Z·
  09:10:01Z가 계산된다(`mcp-tests/README.md`, `T20/README.md`).
- V3 late-v1-release: 대체된 OLD_HOLD를 늦은 v1 claim으로 해제하는 명령은
  APPLIED·ACCEPTED_PENDING_EXTERNAL이 아니고, OLD_HOLD 원행은 RELEASED가
  아니며, 그 key의 APPLIED 감사는 0건이다. code는 고정하지 않았다.
  vocabulary에 "대체된 제한 해제" code가 없다(후보 STALE_REVISION, 아래 요청).
- T25: `runtime-artifacts-all-exit0`·`runtime-artifacts-all-pass`는 필터한
  목록이 전체와 같아야 하는 `sameAs`다. 기존 exit1/2/3 단언은 harness
  테스트가 고정하므로 남겼다. PREPARATION 입력 다섯 subcase에
  `preparation-clean-tree`·`verifier-clean-tree`·`preparation-current-commit`을
  더했다. 검증기 rawRows `input`의 `codeCommit`·`workingTreeDirty`·
  `checkoutCommit`·`checkoutDirty`는 출력 계약 확장이다.

## 6. vocabulary 정렬

| case | 이전 | 지금 | 근거 |
|---|---|---|---|
| T11 stale-close | REJECTED·STALE_REVISION | CONFLICT·STALE_REVISION(감사 outcome도) | vocabulary STALE_REVISION=CONFLICT, 계획 §3.4 |
| E2 false-close | REJECTED·RECALL_RESIDUAL_UNKNOWN | HELD(감사 outcome도) | 미확인 책임이 남는 보류, §6·§13.3 |
| T18 false-close | REJECTED | HELD + code RECALL_RESIDUAL_UNKNOWN 단언 추가 | E2와 같은 입력·의미 |
| T03 clean-release | REJECTED·SEPARATION_EVIDENCE_REQUIRED | HELD·EVIDENCE_UNVERIFIED | D03 "식별 불가능 혼합 범위 보류", vocabulary EVIDENCE_UNVERIFIED=HELD |
| V2 retired-parent | REJECTED | CONFLICT·STALE_REVISION | vocabulary renamedCodes(retired parent) |
| T01 excluded-* | REJECTED·CAPABILITY_UNSUPPORTED | 유지, pair 추가 요청 | 제외 기능은 영구 거부이며 책임이 남는 HELD가 아니다(§1·D01) |

의무 kind(`docs/execution/step2r-profiles/README.md` 대응표 기준).

| case 값 | 지금 | 비고 |
|---|---|---|
| QC(E1) | QUALITY_REVIEW | 1:1 |
| SETTLEMENT(E1) | SETTLEMENT_DIFFERENCE | 1:1 |
| RETURN(E1) | 교환·환불 owner 의무는 RETURN_COMMERCIAL_REVIEW, `return-duty-kind-set`이 반품 의무 집합 {RETURN_QC_REVIEW, RETURN_COMMERCIAL_REVIEW, RETURN_SETTLEMENT_REVIEW}을 exact 대조 | 1:N, 계획 §6 "반품 기본 QC 보류, 교환/환불/정산 의무 별도" |
| DELIVERY_DEFICIT(C4) | DELIVERY_CORRECTED_DEFICIT | 1:1 |
| RECALL_RESPONSE(E2 조사, T17, T18 미확인 잔여) | RECALL_INVESTIGATION | 조사 책임 |
| RECALL_RESPONSE(E2 ADMIN 예외 종료 뒤) | RECALL_EXCEPTION_RESIDUAL | 예외 잔여 책임 |
| VIOLATION_RESPONSE(T17) | DELIVERY_RESTRICTION_RESPONSE | 제한 위반 인도 대응 |
| RECONCILIATION(T18 임시 반품) | RETURN_RECONCILIATION | provisional return |
| EXCHANGE/REFUND/SETTLEMENT_REVIEW(T18) | RETURN_QC/COMMERCIAL/SETTLEMENT_REVIEW 집합 | E1과 같은 반품 의무 집합 |
| PURCHASE_CANCELLATION·PURCHASE_EXCESS(T13) | PURCHASE_CANCELLATION_ACCEPTANCE·PURCHASE_EXCESS_RECONCILIATION | 1:1 |
| SALES_PROMISE(C1, V2) | 유지(추가 요청) | 고객 판매 약속 의무. ALLOCATION_SHORTAGE는 부족분만이라 1:1이 아니다 |

### Step 3 vocabulary 추가 요청(PENDING 34건)

`verification/cases/check_vocabulary.py`의 `PENDING`과 같다. 각 항목은 계획이
요구하지만 backend가 아직 내지 않는 이름이다.

- code, S5 MRTR(§9.1): REQUEST_STATE_INTEGRITY_FAILED, REQUEST_STATE_EXPIRED,
  REQUEST_STATE_PRINCIPAL_MISMATCH, REQUEST_STATE_INTENT_MISMATCH,
  REQUEST_STATE_CONSUMED, INPUT_RESPONSE_UNMATCHED, APPROVAL_REQUIRED(§7.1).
- code, 거래·감사(§4.2·§7.4): TRANSACTION_ROLLED_BACK, RAW_CORE_WRITE_FORBIDDEN,
  DB_PRIVILEGE_DENIED, AUDIT_PERSISTENCE_FAILED, GENEALOGY_CYCLE(§4.2 계보 비순환).
- code, 정의 발행 검사(§8): CARDINALITY_INVALID, REQUIRED_STAGE_INVALID,
  UNIT_INCOMPATIBLE, RELATION_CYCLE, GOAL_INCOMPLETE, ORGANIZATION_SCOPE_INVALID,
  REGRESSION_FAILED.
- pair: CAPABILITY_UNSUPPORTED/REJECTED(§1 제외 범위·D01).
- outcome: STRUCTURED(§3.3 structureIntent 효과0 구조화).
- kind: SALES_PROMISE, RECONCILIATION(T17 원천 claim, 후보 DELIVERY_RECONCILIATION
  또는 UNVERIFIED_SOURCE_REVIEW), REAUTHORIZE, QUANTITY_SHORTFALL(후보
  RECEIPT_SHORTFALL), TRANSPORT_DISCREPANCY, MOVEMENT_RECONCILIATION,
  VERSION_UNSUPPORTED(의무 kind로), STOCK_RECONCILIATION, SOURCE_QUARANTINE,
  EVIDENCE_CONFLICT(의무 kind로), RECEIPT_REMAINDER, DELIVER·ARRIVAL_REMAINDER
  (fixture seed).
- 결정 요청: 대체된 제한을 해제하는 명령의 code(V3, 후보 STALE_REVISION).
- 감사 PENDING field(`contracts/audit-observation-fields.json`): delegatorId,
  reason, before, after, requestId, approvalRefs, evidenceIds, movementId,
  grantRevision, proposalHash, sweepId와 source `queryAudit`, 거부 뒤
  policyVersion(§7.4).

### 검사 도구

`verification/cases/check_vocabulary.py [--check] [--json]`는 준비 산출물 없이
committed case·fixture만 읽는다. outcome·code·code/outcome pair·의무 kind
(obligations·assignments 원행, 의무 응답, getObligations filter, fixture
seed)·감사 source/field/where presence/outcome 값을 vocabulary와 감사 계약에
대조한다. notEquals·absent는 금지값이라 검사하지 않는다. 쓰이지 않는 PENDING
항목도 problem이다. `verification/cases/test_check_vocabulary.py`의 mutant
6개가 각 위반 종류를 잡는지 확인한다. caseAssetCheck로 연결하는 일은
harness3 몫이다.

## 다른 소유자에게 넘기는 일

1. harness 회귀 테스트(harness3): case 이름이 바뀌어 6개 테스트 파일의
   10건이 옛 이름 표본으로 실패한다. 검증한 수정은
   [`harness-test-updates.patch`](harness-test-updates.patch)다(`git apply`).
   ChannelsContractTest(scripted write), RuntimeAssertionTest(parallel·
   command.startedAt), CaseContractRoutesSelfTest(branches 순회),
   OutcomeEffectAssertionSelfTest·SalesAssertionMutationTest(의무 kind),
   RecipientAcceptanceAssertionTest(감사 이름). patch를 적용하면
   `./verify harness` 473 PASS다(아래 증거). 이 branch만으로는 10건 실패다.
2. harness3: `check_vocabulary.py --check`를 caseAssetCheck로 연결한다.
   `promiseCoverage` 파생 규칙과 `audit`/`queryAudit` 계약을 harness-guide
   observer 절로 옮긴다. `runtimeProfile` 두 flag와 자율 loop parallel 패턴을
   host-observation-guide·fixture schema에 반영한다. verifyCoverage rawRows
   `input`의 `codeCommit`·`workingTreeDirty`·`checkoutCommit`·`checkoutDirty`를
   guide 표와 `HostObservationValidator`(PREPARATION)에 더한다.
3. harness3: V4/V6/V7 observation-bindings는 baseline부터 catalog hash가
   낡았다(이 branch는 그 case를 바꾸지 않았다). C3 bindings는 post-processor가
   새 `caseHash`로 다시 썼다. 통합 뒤 binding 생성기를 다시 실행한다.
4. contracts/mcp 소유자: `s0-protocol.md`에 requestState TTL600초를 기록한다.
5. model-corpus 소유자: corpus의 `response.errorCode` 기대값 GOAL_UNSATISFIED,
   UNRESOLVED_RECALL_SCOPE, UNSUPPORTED_CAPABILITY가 vocabulary에 없다. corpus는
   reviewed lock으로 고정돼 있어 이 worker가 바꾸지 않았다.
   `semantic-paths.json`은 감사·kind 이름을 담지 않아 바꿀 필요가 없었다.
   `generate.py` 재실행 결과 byte가 같다.
6. Step 3: 위 PENDING 34건, V3 code 결정, 반품 의무 kind 집합(E1·T18)이 제품
   설계와 맞는지 확인.

## 실행 증거

환경 `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`, Java 21.0.5.
Maven·verify는 `$MULINO_SLOT`로 하나씩 실행했다. 커밋 전 작업 tree 기준이며
clean tree 재실행 결과는 아래 "커밋 뒤" 표에 있다.

| 명령 | exit | 결과 |
|---|---|---|
| `./verify harness`(harness-test-updates.patch 적용) | 0 | 473 tests, failure/error/skip 0 |
| `./verify harness`(patch 없음) | 1 | 473 중 failure 9·error 1. 모두 위 1번 표본 이름 |
| `./verify prepare` | 0 | PREPARED 41 case·799 subcase·23936 assertion, problem0, caseAssetChecks 4개 PASS |
| `./verify contract-red` 수정 case 27개(C1·C3·C4·C5·E1·E2·T03·T04·T05·T07·T09·T10·T11·T13·T14·T16·T17·T18·T19·T20·T22·T24·T25·T26·V2·V3·V8) | 1 | expected=discovered=started=NOT_IMPLEMENTED 606, skip0 |
| `python3 -I verification/requirements/validate_catalog.py` | 0 | VALID, 122 oracle·499 관찰 |
| `python3 -I verification/cases/check_vocabulary.py --check` | 0 | VALID, problem0, PENDING 34 |
| `python3 -m unittest discover -s verification/cases -p 'test_check_vocabulary.py'` | 0 | 6 OK |
| `python3 -I verification/cases/V2/cases_b_invariants.py` | 0 | 0 problem |
| `python3 -m unittest discover -s verification/mcp-tests -p 'test_*.py'` | 0 | 3 OK(T01/T20/T25, C3, T26 새 fixture 3개 포함) |
| 생성기 재실행: author_cases·T06/author_contracts·platform-tests/build_cases·C3·T26·model-binding/generate | 0 | 두 번째 실행 diff 없음 |
| `python3 -I verification/cases/T08/bind_observations.py --check` | 0 | CURRENT |
| `bind_observations.py V4/V6/V7 --check`, `V4/author_review_fixes.py --check` | 1 | baseline부터의 catalog hash drift(harness3 소유, 이 branch 변경 없음) |
| `python3 -I verification/requirements/check_layer_routes.py` | 0 | VALID, unexplained 0, KNOWN_OPEN 4(baseline과 같음) |

### 커밋 뒤(clean tree, HEAD `e1bc23c8`)

| 명령 | exit | 결과 |
|---|---|---|
| `./verify prepare` | 0 | PREPARED, codeCommit `e1bc23c8`, workingTreeDirty=false, 41/799/23936, problem0 |
| `./verify coverage` | 2 | NOT_RUN, preparationStatus PREPARED, runtimeStatus NOT_RUN, preparationProblems 0, coverageProblems 0(baseline과 같은 상태) |
| `./verify harness`(patch 없음) | 1 | 473 중 failure 9·error 1, 모두 harness-test-updates.patch가 고치는 10건 |
| `git apply harness-test-updates.patch && ./verify harness` | 0 | 473 PASS. 실행 뒤 test 파일을 되돌려 tree를 clean으로 남겼다 |
| `python3 -m unittest discover -s verification/coverage -p 'test_*.py'` | 0 | 61 OK(264s, 커밋 직전 같은 내용의 tree) |
| `verification/model-binding/run selftest` | 0 | 61 tests, failure0 |
| `verification/model-binding/run prepare` | 0 | corpusIntegrity VALID, PREPARED |

## 하지 않은 일

- 제품·host·모델 실행은 없다. 새 단언은 모두 NOT_RUN이다.
- harness 테스트와 guide·validator·schema는 소유 밖이라 patch와 요청만 남겼다.
- T25 `runtime-artifacts-no-exit1/2/3`은 harness 테스트가 고정해 지우지 않았다.
- T20 KNOWN_OPEN(skill 계층 연결)은 이 항목 범위 밖이라 그대로다.
- V2 `promised-obligation-total-5`의 SALES_PROMISE와 T01 pair는 Step 3 결정
  전까지 PENDING이다.
- C1/T03/T04/T05/T16/V2/V3 `observations.md`의 단언 목록은 case.json에서 다시
  만들었다. C1·T04·T05·T16은 이번 변경 전부터 설명 문구가 어긋나 있었다.
