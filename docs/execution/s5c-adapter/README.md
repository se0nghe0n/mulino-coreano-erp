# S5c: 시나리오 adapter 3차 — harness 전달, 제품 계약 차이, host control

s5b 실행(`../s5b-adapter/run-85ce2365.json`)에서 남은 adapter·제품 공백을
닫는 round다. 지시(`prompt-s5c-adapter.txt`)의 순서대로 action.harness 전달,
제품과 계약의 차이, ManagementCoverage, host control, OData 오류 응답,
관찰 원천과 installer를 다뤘다. 이 round는 중간에 session 한도로 두 번
끊겼다. 끊기기 전 commit(f5f0081f..ec1e4b2d)은 다시 하지 않고 이어서
남은 항목을 했다.

worktree `mulino-ontology-step3-s5c-adapter`, branch `step3/s5c-adapter`,
기준 `41f8d709`. 소유: `backend/**`, `verification/actual/**`,
`verification/harness/src/main/java/org/mulino/verification/actual/**`,
CaseRunner·AcceptanceDriver(harness 전달만), 이 디렉터리. case·fixture·
contract는 바꾸지 않았다.

## 실행 결과

모두 `./verify scenarios --actual`(backend package 뒤
`verification/actual/scenarios/run.sh`), partial fixture, 일회용
PostgreSQL, 기본 mode(요청 재작성·grant 분리 없음). inventory는
`inventory.py`로 만들었다. 각 실행 직전 같은 commit에서 `./verify prepare`를
돌려 T25가 읽는 `prepare.json`을 그 commit으로 맞췄다(8d99176e 실행만 예외,
아래).

| 실행 | code | 실행 action | PASS | FAIL/TEST | FAIL/ADAPTER | NOT_IMPL |
|---|---|---:|---:|---:|---:|---:|
| s5b 최종 `../s5b-adapter/run-85ce2365.json` | 85ce2365(clean) | 1,664 | 0 | 3 | 769 | 30 |
| 중간(파일 없음) | 8d99176e(clean) | 1,685 | 0 | 3 | 774 | 25 |
| 중간(파일 없음) | 53d51502(clean) | 1,685 | 5 | 3 | 769 | 25 |
| **최종 `run-35ec4c83.json`** | 35ec4c83(clean) | **1,685** | **5** | 3 | 769 | 25 |

- PASS 5는 T25 `coverage-none`·`coverage-dropOracle`·
  `coverage-dropObservation`·`coverage-skipCase`·`coverage-mandatoryWaiver`다.
  모두 부분 fixture 위의 PASS다. 8d99176e 실행에서는 `prepare.json`이 이틀 전
  다른 commit(89f2089f)의 것이라 다섯 subcase가 `preparation-current-commit`
  에서 FAIL했다. 같은 commit에서 prepare를 다시 돌리자 PASS했다.
- 8d99176e 실행이 드러낸 두 문제를 고치고 다시 돌렸다. T24
  `authorized-blob`의 download 응답에 `downloadedArtifacts`가 없었고
  (53d51502), 제품 evidence download가 거부를 HTTP 500으로 답했다
  (35ec4c83). 지금은 403과 FORBIDDEN envelope이고 그 subcase는 아래
  FORBIDDEN 묶음에 들어간다.
- 실행 action이 늘어난 subcase 22개, 줄어든 것 8개(85ce2365 대비). 줄어든
  8개는 모두 V8이다. f5f0081f부터 adapter가 `action.harness`를 읽어
  provision하지 않은 `environmentId`·지원하지 않는 barrier 지점을 보내기 전에
  NOT_IMPLEMENTED로 거부한다. 전에는 harness를 무시하고 보냈다.

### 85ce2365 대비 바뀐 첫 실패

| 묶음 | 전 | 후 | 이유 |
|---|---:|---:|---|
| process/verifyCoverage host control 없음 | 22 | 0 | 40956570. 5 PASS, 10 runtime manifest 검증 미구현(REQUIRED_PATH_RUNTIME_EVIDENCE), 1 APPROVED_MODEL_EXECUTION_EVIDENCE, 5 model adapter 없음(MISSING_ADAPTER model), 1 case 결함 4 |
| `Item scope required` | 16 | 0 | 89f2089f. 16건 모두 같은 기준 조회에서 FORBIDDEN(case 결함 1) |
| blob getEvidence route 없음 | 1 | 0 | 61068f32 |
| 관찰 원천 schemaCatalog·restoreSessions | 5 | 4 | 1건이 다른 원천을 먼저 만나 진행 |
| FORBIDDEN 기준 조회 | 629 | 645 | 위 16건 + T24 authorized-blob(전에는 route 없음) − V8 unavailable-environment-gates(이제 environment 미provision으로 앞에서 멈춤) |
| V8 barrier ACK 불일치 | 0 | 1 | 새로 닿은 단계. case 결함 5 |

### 남은 첫 실패 상위(35ec4c83)

| 건수 | 첫 실패 | 소유 |
|---:|---|---|
| 645 | 기준 조회 FORBIDDEN | Step 2 case 결함 1 |
| 23 | `Unsupported Work goal slot` | slot 이름(case 결함 2, 사용자 결정 3) |
| 18 | `Unsupported sales order slots` | slot 이름 |
| 17 | `Unsupported lifecycle slot`(T26) | slot 이름 |
| 12 | VERSION_UNSUPPORTED(정의 lifecycle 미구현) | 사용자 결정 2 |
| 11 | verifyCoverage runtime manifest 검증 미구현 | Step 3 host adapter |
| 8 | `Missing workId` | case 요청(이번에 분류하지 않음) |
| 7 | `Unsupported evidence slot` | case 요청(이번에 분류하지 않음) |
| 6 | `Missing proposalId`(T22) | slot 이름 |
| 5 | MISSING_ADAPTER model(T25) | model runner(이 round 밖) |

## 항목별 상태

| # | 항목 | 상태 | commit |
|---|---|---|---|
| 1 | action.harness 전달(barrier·environment·auth variant·MCP context) | 완료. worker(executionContext DURABLE_WORKER)는 제품 surface가 없어 열지 않음 | f5f0081f |
| 2a | typed `{value,unit}` 수량 | 완료(TypedQuantities, 판매·재고·품질·입고·반품·배송 handler) | a8bcbbaf |
| 2b | 판매 주문의 TradeItem subject | 완료(itemId slot이 없으면 유일한 TradeItem subject) | a8bcbbaf |
| 2c | Work goal·lifecycle·판매 주문 slot 이름 | 제품 수정 없음. 어느 계약도 slot 이름을 정하지 않는다(아래). case 결함으로 넘김 | — |
| 2d | VERSION_UNSUPPORTED(12) | 제품 수정 없음. 원인은 정의 lifecycle capability 8개 미구현(계획 §270). 사용자 결정 2 | — |
| 2e | 목록 값 slot | 제품 수정 없음. 계약 자기모순. 사용자 결정 1 | — |
| 3 | ManagementCoverage all-of | 완료, RED 먼저 | 41485548 |
| 4a | verifyCoverage·inspectArtifacts(T25) | 완료 | 40956570 |
| 4b | barrier hook | 완료(verification profile) | f5f0081f |
| 4c | fault point·fault control | 완료(명령 pipeline 4지점). 도메인 내부 지점은 열지 않음 | ec1e4b2d |
| 4d | scheduler tick·sweep·await | 완료(verification scheduler + host command + 독립 extractor). 자연 tick 수동 관찰은 NOT_IMPLEMENTED | 93a2e1bd, 4ae14cb1 |
| 4e | externalResponder | 부분. configure·setResponse·loseResponse·commitThenLoseResponse·inspectRequests·queryOperation·queryStatus 완료. 알림 channel·queue ACK·deliverNotification은 제품 outbox가 아니라 NOT_IMPLEMENTED | 93a2e1bd, 4ae14cb1 |
| 4f | process start·stop·restart, backup·restore·cutover·schema·clientProbe 등 | 하지 않음(아래 남은 공백) | — |
| 4g | blob route | 완료(evidence download·upload) | 61068f32 |
| 4h | worker route | 제품 surface 없음, `no product surface`로 TEST 분류 | 61068f32 |
| 5 | OData command·$batch 실패 envelope | 완료(자기 잠금 timeout이 원인) | 30a09d97 |
| 6a | 조직 단위 getInventory snapshot(PG-ORGANIZATION-SNAPSHOT) | 완료 | 89f2089f |
| 6b | 관찰 원천 workClosures·obligationResolutions | 완료(approximate) | 68e725e7 |
| 6c | 관찰 원천 supervisionIssues | 완료(approximate) | 8d99176e |
| 6d | 관찰 원천 locks·lockWaits | 열지 않음. 제품 gap(아래) | 8d99176e(기록만) |
| 6e | installer: Restriction·DispositionBasis·eligibility 근거·검증 chain | 하지 않음. 제품 요구와 fixture가 말하는 사실의 차이를 아래에 적음 | — |


## 제품과 계약의 차이(지시 2)

### slot 이름은 계약이 정하지 않는다

`contracts/intent.schema.json`의 `slots`는 이름이 열린 object이고 값 모양만
정한다. 제품이 publish하는 `CommandSchemas.input`도 `slots: object`뿐이며
"각 handler가 bounded typed slot을 검증한다"고 적는다. 계획 §5.2(154–159행)는
waitWork의 reason·대상·재개 predicate처럼 개념만 말하고 wire key를 정하지
않는다. capability·definition 계약(`acceptance-capabilities.json`,
`domain-vocabulary.json`)에도 slot 이름이 없다. 그래서 아래 거부는 "계약이
허용하는 것을 제품이 거부"가 아니다. 제품 handler의 허용 목록이 유일한
정의이고, case가 다른 이름을 쓴다. 제품을 바꾸면 case마다 다른 이름을 추측해
받아 주는 셈이라 바꾸지 않았다. 계약에 capability별 slot schema가 없다는 것
자체는 사용자 결정 3이다.

case 요청에서 센 차이(요청 건수, 대표 case). 제품 허용 목록은
`WorkLifecycle.publicDraft`·`validateKeys`, `SalesCommands.slots`다.

| capability | case가 보내는 제품 밖 slot | 건수 | case |
|---|---|---:|---|
| createDraft | closeReason, itemId, nextCheckAt, overdueAction, reason, resumePredicate, verifierId | 22 | C3 |
| createDraft | destination(Place 참조; 제품은 destinationId) | 17 | T20 |
| createWork | workKind(제품은 workType) | 16 | T13 |
| createWork(goal 형) | workKind, nextAction, nextCheckAt(제품은 kind·itemId·definitionVersionId) | 13 | T26 |
| activateWork | goalVersionId | 14 | T07 |
| waitWork | 평평한 reason·targetId·resumePredicate·verifierId·nextCheckAt·overdueAction(제품은 wait aggregate) | 13 | T11 |
| createDraft | itemId, predicateInputId | 12 | T07 |
| createWorkLink | physicalScope(제품은 VERIFIED_RANGE_REQUIRED로 거부, 의도된 동작) | 8 | T11 |
| C3 전 lifecycle | 모든 capability에 같은 범용 slot 묶음 | 6×9 | C3 |
| closeWork | assessmentId / decisionEvidenceId·reasonText·successorWorkId | 6 / 4 | T11 |
| createDraft | lotIdentification | 5 | T09 |
| createWork | interval | 5 | T09 |
| createWork | goalPredicate, targetId, verb | 4 | T06 |
| createFollowup | nextAction·nextCheckAt·ownerId·parentWorkId·sourceOccurrenceId·supervisorId | 3 | C5 |
| createDraft | customerId, itemId | 2 | T21 |
| resumeWork | resumeEvidenceRefs / resumeEvidenceIds / clientMetadata | 2 / 1 / 1 | T21 / T11 / V1 |
| reviseGoal | approvalId, proposalHash, targetQuantity | 1 | T13 |
| createSalesOrder | packagingVersion | 26 | C4 |
| createSalesOrder | conditionPolicy, endpoint | 22 | C3 |
| createSalesOrder | endpoint (+ownerId 1) | 5 | C2, T13 |
| dispatchPurchaseOrder | workId·approvedRevision·approvedHash(제품은 proposalId·proposalHash·approvalId) | 6 subcase | T22 |

### VERSION_UNSUPPORTED는 정의 lifecycle 미구현이다

s5b의 VERSION_UNSUPPORTED 12건(C3 migrateWorkDefinition 3, T21 8, V1 1)은
모두 첫 action `createDefinitionDraft`에서 난다. 제품에 그 capability를
등록한 handler가 없어 `CommandDefinitionResolver`가 `DomainError.unsupported()`
("Unsupported capability or definition")를 낸다. 정의 버전이 틀린 것이
아니다. 계획 §270은 `createDefinitionDraft`, `validateDefinition`,
`submitDefinitionReview`, `approveDefinition`, `publishDefinition`,
`activateDefinition`, `retireDefinition`, `migrateWorkDefinition` 8개를
정한다. 제품에는 읽기(`DefinitionQueries`)와 검증기(`DefinitionValidator`),
PUBLISHED 행 읽기(`DefinitionRepository.get`)만 있다. case 요청 203건이 이
capability를 쓴다.

구현하지 않은 이유: case의 package 모양(`nounTypes[{name,coreRef}]`,
`verbDefinitions[{kind,slotTypes,endpoint}]`, `goalTemplates[{quantity,unit,
evaluatorId,...}]`, `compatibilityManifest`)은 제품의 저장 정의
(`Definition` record: nouns{name,core}, verbs{intentKind,capabilityId,stage,
slots}, goals{predicate}, capabilities)와 다르고, 어느 계약도 draft package의
wire 모양을 정하지 않는다. 한쪽을 골라 8개 capability를 만들면 그 선택이
곧 계약이 된다. 사용자 결정 2.

## 사용자 결정이 필요한 것

1. **목록 값 slot(PG-INTENT-ARRAY-SLOT).** `intent.schema.json`
   `$defs.value`에는 배열 branch가 없다. 그런데 제품 handler가 목록을 읽고
   (`InventoryCommands` splitQuantity quantities·mergeQuantity segmentIds,
   `WorkLifecycle` eventKinds·conditions·obligationIds, `ShipmentCommands`
   cargo·legs, `IdentityCommands` scopes, `PurchaseCommands` approvePurchase
   conditions), case도 top-level 목록 slot을 쓴다(moveQuantity
   sourceSegmentIds 36, confirmReceipt evidenceIds 35, createWork eventKinds 28,
   approvePurchase conditions 26, createGrant actions 25, reserveQuantity
   sourceSegmentIds 22, splitQuantity children 16 등 30여 capability). 제품
   runtime은 slots를 object로만 검사하므로 실제로 목록을 거부하지 않는다.
   모순은 계약 문서에 있다.
   - 선택 A: `$defs.value`에 bounded 배열 branch(원소는 `$defs.value`,
     maxItems 상한)를 publish한다. 제품·case 변경 없음.
   - 선택 B: 배열을 금지하고 목록을 handler별 aggregate object(예:
     `{"items":[...]}`, 다섯째 branch)로 감싼다. 제품 handler와 case 요청
     수백 건을 함께 바꾼다.
   - 권고: A. 다섯째 aggregate branch가 이미 중첩 배열을 허용하므로 top-level
     배열 금지는 안전을 더하지 않고, 실제 handler와 case가 모두 목록을 쓴다.
2. **정의 lifecycle의 package wire 모양.** 위 VERSION_UNSUPPORTED 절.
   - 선택 A: 제품 `Definition` record를 draft package의 wire 모양으로
     publish하고 case를 그에 맞춘다. 제품은 8개 capability를 기존 검증기와
     저장 모양 위에 만든다.
   - 선택 B: case 모양을 계약으로 정하고 제품이 그것을 `Definition`으로
     변환한다(coreRef·slotTypes·evaluatorId 같은 field의 의미를 새로 정해야
     한다).
   - 권고: A. 제품 검증기·pin·hash가 이미 그 모양에 묶여 있고, case 쪽
     field의 의미(예: evaluatorId와 evaluatorVersion의 차이)는 아직 정의되지
     않았다. 어느 쪽이든 구현은 별도 Step 3 slice다.
3. **capability별 slot schema.** slot 이름을 정하는 계약이 없어 위 표의
   차이를 누가 맞출지 판정할 기준이 없다. 지금은 제품 handler 허용 목록이
   사실상 계약이고 case가 맞춘다(round 12의 방식). 권고: 제품이
   capability별 slot 목록을 publish(`CommandSchemas`나 capability manifest)하고
   `check_request_contracts.py`가 그것으로 case를 검사한다.

## host control(지시 4)

### scheduler: tickScheduler·sweepDue·awaitRuntimeTask

scenario server는 자기 scheduler loop를 돌리지 않는다(fixture
`runtimeProfile`이 없으면 controlledTicks). 그래서 harness tick 하나가
scheduler 주기 하나다.

- backend `VerificationScheduler`(verification profile 전용): 주기는
  `RuntimeVerificationHost`가 매초 도는 제품 조합 그대로다
  (`RuntimeRecoverySweeper.sweep`, `RuntimeIntakeProcessor.recoverDue`,
  `DurableDeliveryWorker.tick`). 제출·주기·terminal 행을 scheduler 자신이
  기록하고, tick 응답은 제출만 담는다. 시각은 wall clock(host command 구간과
  비교되므로), 요청 instant(`clock`·`clockInstant`·`asOf`)가 있으면
  verification clock을 먼저 옮긴다. 주기는 모든 조직의 due 행을 처리한다.
- adapter `SchedulerHostControls`: host command `scheduler_host.py`가 tick을
  제출하거나 terminal을 읽고, 독립 extractor `scheduler_extract.py`가 tick
  응답이 아니라 `GET /scheduler/records`로 scheduler 자신의 행을 다시 읽어
  operationEvidence의 typed identity로 그 task 행만 고른다(T22의
  `rawRows/tasks/0`은 이 선택으로 그 tick의 task다). await는 terminal snapshot
  파일을 쓰고 extractor 입력 hash로 묶는다. bearer는 환경 변수로만 넘긴다.
- 확인: T11·T22·T16·T26 부분 실행에서 T22 6 subcase의 tick·await 12건이
  host observation 검증을 통과해 EXECUTED였다(첫 실패는 그 뒤 case slot).
- 남김: `OBSERVE_NEXT_NATURAL_TICK` 수동 관찰(T26 `*-autonomous-loop`)은
  loop가 없어 NOT_IMPLEMENTED다. `schedulerCycles`의 `startedBy`는
  `HARNESS_CONTROL`이라 자연 tick NO_TASK 증명에 쓸 수 없다(guide대로).
  `RUNTIME_TASK_SNAPSHOT` observer 읽기 mode(T26 `sweep-db`)는 아직 없다.

### externalResponder

backend `VerificationResponder`는 verification scheduler의 delivery tick이
말하는 외부 수신자 fixture다. 제품 판정은 그대로다: 검증된 근거 없이는 모든
전달이 `UNKNOWN_EXTERNAL`이고 사람의 reconciliation이 필요하다
(`RuntimeService.deliverUnderFence`). 또 제품 delivery guard는
`fixtureExternalEffect`만 허용하므로 `dispatchPurchaseOrder` outbox는 수신자에
닿기 전에 `unsupported`로 남는다(제품 동작, product adapter 없음).

| control | 처리 |
|---|---|
| configure, setResponse | mode = behavior·responseMode, 또는 response.delivered/status. supportsIdempotency·statusQuerySupported·statusQueryMode=ACTUAL_REPLY·remoteResultId 적용. receiverId·sourceProfile·externalEffect·retryableFailure는 receipt의 descriptiveParameters |
| loseResponse(AFTER_EXTERNAL_EFFECT), commitThenLoseResponse | ACCEPT_THEN_DROP_RESPONSE |
| inspectRequests | 받은 요청 목록 `data.requests` |
| queryOperation, queryStatus | 한 externalOperationId에 대한 수신자 답을 receipt 문서로 쓰고 receiptArtifact·receiptHash·documentUri·evidenceHash·result를 돌려준다 |
| ACK_AND_DELETE_MESSAGE(V5), channel(T14), deliverNotification(C5·T10) | NOT_IMPLEMENTED. queue 수신 확인과 알림 channel은 제품 outbox가 아니다 |

### 남은 host 공백

- process start·stop·restart(T26·V5·V7 187건): `environmentId`마다 별도
  backend process를 띄우는 runtime 환경이 필요하다. 하지 않았다.
- backup·restore·retentionSweep·dataInventory·archive*·schema*·
  compileSchema·compilerSchemaProbe·cutoverStage·clientProbe·
  deploymentProbe·enumerateWriteSurface·scanArtifacts·claim: 하지 않았다.
- fault의 도메인 내부 지점(intake·transfer·worker·restore)과 T04 primitive
  단계 지점, 그 밖의 fault operation(release·pause·enable 등).

## 관찰 원천과 installer(지시 6)

- workClosures → `mulino_work_WorkTransitions`, obligationResolutions →
  `mulino_responsibility_ResolutionCredits`, supervisionIssues →
  `@supervisionIssues`(RecoverySchedules HELD_MANUAL + IntakeRecoveries
  HELD_*, kind = 기록된 hold outcome). 셋 다 approximate.
- locks(T04·T16·V3): case는 명령마다 lock 획득 순서·잠근 revision·
  scopeFenceId를 읽는다. 제품은 fence 획득을 행으로 남기지 않는다(advisory
  lock은 transaction 범위). 제품 gap이다.
- lockWaits(V2·V3·V8): `pg_locks`로 대기 backend는 볼 수 있지만 그 backend를
  case의 scopeId·barrier transactionId로 잇는 mapping을 제품이 노출하지
  않는다. 제품 gap이다.
- installer의 Restriction·DispositionBasis·eligibility 근거는 하지 않았다.
  제품의 범위 근거 행(`V20__quality_eligibility.sql`)은 segmentId가 있으면
  workId·commandId·policyHash·validUntil·decisionId가 NOT NULL이고,
  `QualityEligibility`는 그 evidenceRef가 SEGMENT subject의 검증된 chain
  (DocumentVersion·Event·InboxRecord 1개·Claim·CanonicalOccurrence·
  VERIFIED Verification)일 때만 근거로 센다. fixture의 `eligibilityBases`·
  `eligibilityFacts`·`dispositionBases`·`restrictions`는 segment·category·
  action·수량·유효기간·결정자만 말하고 Work·명령·결정·검증 chain을 말하지
  않는다. installer가 이것을 만들면 fixture가 말하지 않은 책임 Work와 검증
  판정을 지어낸다. 34개 Restriction alias 대부분은 속성이 없는 자리표시자다
  (case 명령이 만드는 hold). case 결함 6.

## Step 2 case 결함(oracle은 바꾸지 않음)

1. **grant 차원과 all-of(기준 조회 FORBIDDEN 645).** case의 grant
   3,972개가 모두 `scopeComposition: PER_DIMENSION`이다. 2026-10-09 사용자
   결정(all-of)에서는 대상에 없는 차원(inventory 조회에 WORK, definition
   package 등)을 가진 grant가 그 대상을 덮지 못한다. 이번에 조직 단위
   getInventory가 열리면서 `Item scope required` 16건이 모두 같은 FORBIDDEN으로
   옮겨 왔다(T02 1, T07 12, T21 1, V1 2). 예: T07 `date-as-place`의 steward
   grant는 items·places·works·definitionPackages를 한 grant에 둔다. case별
   첫 실패: C3 306, V4 93, T20 59, T08 23, T10 19, T24 13, T01 12, T07 12, T11 12, T04 9, T19 9, V6 9, T03 8, T22 7, T09 6, T17 6, T12 5, C5 4, T16 4, T26 4, V7 4, C1 3, T05 3, V2 3, V3 3, T02 2, T06 2, V1 2, T13 1, T15 1, T21 1.
2. **slot 이름.** 위 "slot 이름은 계약이 정하지 않는다" 표 전체. 특히 T26
   createWork goal 형 13건, T13 workKind 16건, T11 wait 평평한 slot 13건,
   C4 packagingVersion 26건, T22 dispatchPurchaseOrder 6 subcase,
   V8·T26 reserveQuantity `orderLineId`(Unsupported fulfillment slot 4).
3. **C3 범용 slot 묶음.** 모든 capability에 같은 slot 묶음을 보내 handler가
   인가 전에 TYPE_INVALID로 거부한다(s5b와 같음).
4. **T25 `model-preparation-not-runtime`.** corpus 원문 category 이름
   (`clear_synonyms`, `ambiguity_target_conflict`, ...)과 case 기대 이름
   (`normalSynonym`, `ambiguityConflict`, ...)이 다르다(40956570에서 발견, 이번
   실행에서 그대로).
5. **V8 barrier.** `real-lock-two-transactions`·`upgrade-revalidate-
   transactions`의 reserve40은 `orderLineId` slot으로 scope lock 전에
   TYPE_INVALID가 되어 barrier에 닿지 않는다. 제품 registry는 DECLARED·
   timedOut을 그대로 답하고 harness는 "Barrier ACK differs from requested
   state"로 멈춘다. adapter는 관찰한 상태를 바꾸지 않는다.
6. **installer가 지을 수 없는 fixture 사실.** 근거·제한 fixture
   (`eligibilityBases` 22, `eligibilityFacts` 33, `dispositionBases` 1,
   `restrictions` 2)는 제품 범위 근거 행이 요구하는 책임 Work·결정·명령·
   검증 chain을 말하지 않는다. Restriction alias 34개 대부분은 속성이 없다.
   Step 2가 이 사실을 fixture에 쓰거나, case가 근거를 명령(placeHold·
   recordDispositionBasis 등)으로 만들게 정해야 한다.

## 검사

| 명령 | 결과 |
|---|---|
| `-Dtest=VerificationSchedulerTest,VerificationBarriersTest` | 7 tests, 0 failures |
| `-Dtest=EvidenceDownloadTest,VerificationSchedulerTest` | 5 tests, 0 failures. EvidenceDownloadTest는 수정 전 2건 RED(servlet 예외) 확인 |
| 끊기기 전 commit의 focused test | 각 commit 본문(ManagementCoverageTest 5/0, TypedQuantitiesTest 4/0, S1ReadIntegrationTest 9/0, InventoryPostgresTest 10/0, CommandTransactionTest 12/0 등) |
| harness compile | 각 adapter commit 뒤 통과 |
| `./verify prepare` | 53d51502·35ec4c83에서 exit 0 |
| 부분 실행 T11·T22·T16·T26(4ae14cb1) | verification profile context 기동, T22 tick·await 12건 host 검증 통과 |
| 시나리오 기본 실행 | 위 표, 최종 35ec4c83 clean tree |

하지 않은 것: backend 전체 suite, `./verify harness`, native S1–S4(지시대로
통합 때 돌린다). probe mode 실행도 하지 않았다. 정의 lifecycle,
목록 slot, process start/stop/restart, installer 근거 설치는 위 이유로
하지 않았다.
