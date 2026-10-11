# Step 2 round 14: s5d 실행이 드러낸 case 결함을 공개 slot·응답 계약에 맞춘다

s5d adapter 4차 실행(`../s5d-adapter/run-6b9aed36.json`)은 PASS 54(모두 부분
fixture), FAIL 669, NOT_IMPLEMENTED 79, 실행 action 5,781이었다. 그 README의
"Step 2 case 결함" 9개와 2026-10-11 사용자 결정(AGENTS.md: 판매 주문의 자기
인도 Work, createWork의 nextAction·nextCheckAt과 첫 의무, 분할 자식의 index
참조)이 이번 round의 입력이다. 원인은 case가 제품이 공개한 capability별 slot
목록(`contracts/capability-slots.json`)과 응답 모양을 따르지 않은 것, 접근
문맥·회수 scope를 계약에 없는 pointer로 읽은 것, fixture가 설치에 필요한 사실을
말하지 않은 것이다. 고친 뒤 실제 제품에 다시 실행해 확인했다(AGENTS.md
2026-10-09 run-driven 절차).

worktree `mulino-ontology-step2r-round14`, branch `step2r/round14`, 기준 feat
head `9dfa95db`. `backend/**`, `contracts/capability-slots.json`,
`contracts/intent.schema.json`, `verification/actual/**`,
`verification/harness/**`는 읽기만 했다(Step 3 작업자 소유).

## Commit

| commit | 항목 | 내용 |
|---|---|---|
| `4f4831a3` | 2 | PG-INTENT-ARRAY-SLOT 규칙과 knownOpen 항목을 같은 commit에서 지우고 schema의 maxItems를 읽는다 |
| `61c235b5` | 3·6 | C3 접근 문맥 `grants/0/ID`·`grants/0/actions`, 회수 명령은 조회한 `/response/data/scopeId` |
| `e3ee7e4c` | 5 | T01 excluded-* 6에 명령 멱등 키 |
| `b1307818` | 7 | T19 OLD-WORK works 항목, T20 구매 Work 조회에서 lotId 제거, C3 사실 없는 alias 정리·POLICY 사실, round 14 script |
| `ad65dc3d` | 8·1 | 분할 자식 index pointer, 한 부분만 떼는 split을 quantities로(splitPart) |
| `86443b1b` | 1 | 공개 이름과 다른 slot 32 규칙 rename, 다시 말한 slot 19 규칙 drop |
| `df22710b` | 1·UD-1 | C3·V4 판매 주문 workId 제거, C2·T09·T11 reserve는 판매 line 지명 |
| `bd70f286` | 4 | 묶지 않는·공개되지 않은 subject 선언 제거, C3 requiredFill, V4 blob·이동 probe의 TYPE_INVALID 기대 |
| `0ff62209` | 1 | 분할 자식 전량 이동의 quantity, T16 감소 조정 direction |
| `ae8b4a06` | 1 | cases_b 불변식 4를 공개 dispatch slot으로 판정 |
| `1662b92d` | 7 | T20 purchase-work의 currentGoalVersionId |
| `0183320b` | 4·6 | 회수 승인 scope·APPROVE(C3·E2·T18), C3 대조 호출 requiredFill. 이 commit의 전체 실행 `run-0183320b.json` |
| `68d8dbd9` | 6 | C3 회수 단계가 승인 ID를 `/response/effects/approvalId`에서 읽는다(0183320b 실행에서 발견) |
| 이 commit | — | 최종 실행 inventory `run-68d8dbd9.json`, 중간 실행 `run-0183320b.json`과 이 문서 |

## 항목별 상태

| # | 항목 | 상태 | 요약 |
|---|---|---|---|
| 1 | 공개 slot 이름 | DONE(부분) / 기록 | 630→448(정적, `./verify prepare` capabilitySlotContract). 448 중 30은 정의 package verb의 false positive(Step 3 발견 1), 17은 사용자 결정 UD-2로 둔 createWork nextAction·nextCheckAt. 나머지는 이름이 아니라 명령 순서·실체가 다른 것(아래 표) |
| 2 | PG-INTENT-ARRAY-SLOT 제거 | DONE | 4f4831a3. 규칙과 knownOpen 항목을 같은 commit에서 지웠다. `schema_errors`가 maxItems를 읽는다 |
| 3 | C3 grantId pointer | DONE | 61c235b5. `/response/data/grants/0/ID`·`/actions`(279+279). 실행에서 `grantId` 실패 74→0 |
| 4 | FORBIDDEN 대신 TYPE_INVALID 66 | DONE(부분) | bd70f286·0183320b. subject 선언 정리와 requiredFill로 잘 만든 요청이 인가에 닿고, 일부러 잘못 만든 V4 blob 10·이동 5는 TYPE_INVALID를 기대한다. 남은 것은 실체가 필요한 C3 시도(아래) |
| 5 | T01 excluded 멱등 키 | DONE | e3ee7e4c. envelope을 지나 capability 판정에 닿고, 제품은 HELD VERSION_UNSUPPORTED("Unsupported capability or definition")로 답한다. oracle(REJECTED·CAPABILITY_UNSUPPORTED)은 그대로 두었다(Step 3 발견 5) |
| 6 | C3 회수 scopeId | DONE | 61c235b5·0183320b. 조회한 `/response/data/scopeId`, decision APPROVE. E2·T18 승인도 제안 응답의 scope로 |
| 7 | T19·T20·C3 alias | DONE | b1307818·1662b92d. T19는 PO-A가 설치돼 FORBIDDEN이 사라지고 EVIDENCE_UNVERIFIED(송장 원본 scope)에서 멈춘다. T20 purchase-work는 200으로 답한다 |
| 8 | 분할 자식 index | DONE / 2 기록 | ad65dc3d. T14 transit2·unobserved2는 harness validator 때문에 이름을 유지(Step 3 발견 2) |
| UD-1 | 판매 주문 workId | DONE / T11 기록 | df22710b. C3·V4에서 workId를 빼고 C4는 이미 `/response/workId`를 읽는다. T11 cancel-after-shipment는 기록 |
| UD-2 | createWork nextAction·nextCheckAt | 유지 | T26·V5 17 명령. Step 3가 slot을 공개하면 위반에서 빠진다 |
| UD-3 | 분할 자식 index | DONE | 항목 8 |

## 항목 1: 공개 slot 목록

심판은 제품이 공개한 `contracts/capability-slots.json`(사용자 결정 2026-10-11
(3))이다. 이름만 다른 slot은 공개 이름으로 바꾸고 값은 그대로 둔다. 계약이 다른
곳에서 이미 정하는 값을 다시 말한 slot은 같을 때만 지운다. 둘 다 공통 규칙
(`contracts/request-contracts.json` commandSlots)으로 두어 모든 생성기와 round
script가 같은 결과를 쓴다. 명령 순서나 다른 실체가 필요한 것은 바꾸지 않고
기록했다.

정적 위반(Python 재현과 `./verify prepare`가 같은 수): 630 → 448.

| 단계 | 위반 명령 | 원인 |
|---|---:|---|
| 기준 9dfa95db | 630 | s5d README 결정 3 절 |
| 분할 index·splitPart(ad65dc3d) | 618 | E1·E2·T26·V8 부분 split, 이름 붙인 children |
| rename 32·drop 19(86443b1b) | 459 | 아래 표 |
| 판매 line·주문 Work(df22710b) | 455 | reserve orderId, createSalesOrder |
| move·adjust(0ff62209) | 453 | 분할 자식 전량 이동, T16 decrease |
| 회수 승인(0183320b) | 448 | E2·T18 approveRecall |

### 바꾼 것(규칙)

- renames(32): activatePolicy/approvePolicy policyId→id, approvePolicy
  policyHash→contentHash, revokeGrant grantId→id, createPolicyDraft
  policyVersion→version·effectiveAt→effectiveFrom·synthetic→fixtureOnly·
  officialSource→source, recordLegEvent eventKind→kind, transfer/resolve/
  waiveObligation obligationId→assignmentId, transferObligation acceptedById→
  recipientId·sourceWorkId→workId, resolveObligation resolutionEvidenceId→
  evidenceId, receiveReturn returnAuthorizationId→authorizationId, releaseHold
  qcDecisionEvidenceId·decisionEvidenceId→evidenceId, recordHandover
  confirmedLocationId→placeId·custodianId→toCustodianId, assignCapability
  capability→assignedCapability·subjectId→actorId, replaceAllocation
  sourceAllocationId→allocationId·replacementSegmentId→segmentId,
  recordDispositionBasis evidence→evidenceId, openInvestigation evidenceId→
  basisEvidenceId, linkExternalId code→value, disposeQuantity·recordStocktake
  evidenceId→evidenceRef, recordSupplierReply evidenceId→evidence·quantity→
  proposedQuantity·orderId→purchaseId, reserveQuantity saleLineId→salesLineId,
  proposePurchase unitPrice→price, createFollowup parentWorkId→workId.
- contractDrops(19): pick/dispatch quantity가 그 allocation과 같을 때(계획 §6
  출고는 allocation 전체를 소비. reserve·replace·split 자식·fixture
  allocation에서 읽고 60 명령이 같았다. T06 2/100 부분 출고는 둔다), dispatch
  destinationId·originId·segmentId·workId(allocation과 판매 line이 정한다),
  proposePurchase ownerId·supervisorId(계획 §6 입력이 아니고 Work가 책임을
  갖는다. handler가 전에도 읽지 않았고 oracle도 읽지 않는다), approvePurchase·
  recordSettlementAdjustment decidedAt이 subcase 시계와 같을 때, consumptionPolicy
  SINGLE_ORDER_REVISION, releaseHold quantity가 그 보류와 같을 때,
  openInvestigation CANDIDATE, revokeGrant reason, recordStocktake locationId,
  moveQuantity sourceId·reason, transferObligation effectiveAt=시계, handover·
  cancelPurchase workId, createSalesOrder workId(C3·V4만, UD-1).
- 모양: splitPart(부분 하나→부분과 나머지 quantities), 분할 자식 전량
  이동(whole_leaf), T16 decrease→direction DECREASE.

### 바꾸지 않고 기록한 것(418 명령, 정의 verb false positive 30 제외)

이름을 바꾸면 시험 내용이 달라지거나, 공개 계약이 같은 사실을 다른 명령
순서·다른 실체로 받는 것이다. case 작성자가 새 action·fixture 실체를 정해야
한다.

| 묶음 | 명령 | case | 공개 계약이 요구하는 것 |
|---|---:|---|---|
| 2단계 수령 | confirmReceipt 37, receiveProvisional 1 | C2·E1·T02·T06·T07·T09·T11·T12·T13·T14·T16·T21·T22·V1·V8 | 실물 사실(item·place·quantity·occurredAt·원천 key)은 receiveProvisional(RECORD)이 받고 confirmReceipt는 receiptId·canonicalOccurrenceId·lotId·custodian만 받는다(`../s3-receipt/contracts.md`). 앞 단계 action과 canonical 사건이 필요하다 |
| 증거 사건 어휘 | recordActivity 38, attachEvidence 21, correctEvidence 12 | C4·C5·E1·T02·T05·T06·T07·T09·T11·T12·T14·T16·T20·T21·T22·T26·V1·V2·V3 | typed subject 하나·sourceNamespace(source profile)·externalEventId·sourceVersion·effectiveFrom·payload·documentId, 정정은 supersedesId(`../s1-evidence/contracts.md`). case별 영역 field(temperature, coverage 등)를 payload로 옮기는 결정이 필요하다(round 13 미결 8) |
| 관측 원천 chain | recordDelivery 22, receiveReturn 9, recordObservedMovement 1 | C4·E1·E2·T13·T17·T18 | 원천 key 대신 기록된 사건의 eventId, 소비된 allocation 대신 dispatchId. 앞 단계 recordActivity가 필요하다. 소비된 배분 참조는 Step 3 발견 4 |
| 동일성·대조 | linkCanonicalOccurrence 11, matchSourceIdentity 7, resolveEvidenceConflict 1 | E1·T06·T07·T22 | linkCanonicalOccurrence는 reconciliationId 하나, match/resolve는 claimId·basisDocumentId·policyVersion·sourceIdentity |
| 운송 exact | recordLegEvent 11, createShipment 6, recordHandover 2 | E1·T05·T13·T14 | cargo·legs aggregate, cargoId·legId·physicalScopeId·occurrenceId |
| 규제 | recordRegulatoryDecision 8, recordSubmission 8, prepareRegulatoryProcedure 7, verifyLabel 2 | E1·T13·T15 | procedureId·occurrenceId·physicalScopeId·policyId(문자열 version 아님), verifyLabel·결정·제출은 RECORD |
| 회수 chain | closeRecall 4, recordRecovery 3, recordRecallNotice 2, proposeRecall 5, disposeQuantity 3, recordDispositionBasis 일부 | E2·T18 | scopeId·approvalId·scopeHash·canonicalOccurrenceId·actualEventId·rootSegmentId·partitionHash. 회수 사건 chain이 필요하다 |
| 업무·책임 | transferObligation 22(T10 부분 scope·C3 acceptanceEvidenceId), closeWork 10, proposeHandover 5, createFollowup 4, resumeWork 4, resolve/waive/accept/reject 7 | C3·C4·C5·T10·T11·T12·T14·T21·V1 | createFollowup draft 객체, closeWork reason·workId, handover workId·recipientId·nextAction·nextCheckAt. 계획이 요구하는 것은 Step 3 발견 4 |
| 정의·목표 | createWork 5(T06 술어·T09 interval), createDraft 16 | T06·T07·T09·T20·T21·V1 | 술어는 정의 규칙 payload(round 13 미결 2), T07 predicateInputId, T20 의도된 NEEDS_INPUT 입력 |
| 구매·정산 | recordExternalReconciliation 9, dispatchPurchaseOrder 6, recordSupplierReply 3, revisePurchase 1, reviseGoal 1, approvePurchase 1, recordInvoice 1, matchInvoice 1, approvePolicy 5, createPolicyDraft 8, activatePolicy 1 | E1·T13·T15·T19·T20·T22·T23·T26 | outboxId(외부 작업 ID 아님), 승인된 proposal, changes 객체, 정책 content aggregate |
| 재고 probe | splitQuantity 9, mergeQuantity 5, adjustQuantity 3, placeHold 6, releaseHold 5, dispatchQuantity 2, 기타 6 | E2·T03·T04·T06·T14·T16·T17·T21·T24·V1·V3·V8 | T03 cycle·merge, T04 primitive guard(의도된 probe), V8 faultPoint(harness fault 주입), T14 transit 이름 자식(Step 3 발견 2), T06 부분 출고 |
| 사용자 결정 | createWork nextAction·nextCheckAt 17, reserveQuantity workId 7, createSalesOrder ownerId 1 | T21·T26·V1·V5·T13 | UD-2(Step 3가 slot 공개), reserve는 판매 line(round 13 기록), ownerId는 Step 3 3713c28e가 공개 |
| T11 판매 Work | createSalesOrder workId(공개 slot이라 위반 수에는 없다) | T11 | 그 판매는 나중에 취소하는 SOURCE Work 아래에서 출고되고 contracts·movements 보존 oracle이 그 연결에 기댄다. UD-1대로 주문 자신의 Work로 바꾸려면 ROOT 의무가 그 Work에서 나와야 한다(UD-2 첫 의무) |

## 항목 4: 계획 §3.3 순서와 FORBIDDEN 기대

계획 §3.3은 slot·타입·단위·cardinality·지원 효과를 적용 정책보다 먼저
검사한다. s5d 실행의 66 subcase는 판정을 이렇게 나눴다.

- 잘 만든 요청이어야 하는 시도(C3·V4 approval·T08·V6·V7): 제품 resolver는
  선언 subject가 정의가 공개한 명사이고 handler가 묶는 대상일 때만 받는다
  ("Declared subject differs from command target", "Subject action is not
  published"). `commandSlots.unboundSubjects`가 구매 승인·수정의
  PurchaseProposal, Handover, PolicyVersion, recordCharge Invoice와 정의에 없는
  명사(Allocation·Grant·Obligation·Recall·ReconciliationCase·
  ReturnAuthorization) 선언을 지운다. typed slot이 대상을 이미 지명하고 선언
  0개는 허용된다. requiredFill이 proposeRecall investigationId·startQuantity,
  linkCanonicalOccurrence reconciliationId, revisePurchase changes,
  cancelPurchase externalAcceptance UNKNOWN, openInvestigation itemId, C3
  splitQuantity quantities를 채운다. 시도와 권한 있는 대조 호출은 같은 요청이다.
- 일부러 잘못 만든 probe(V4): blob-* 10은 증거 blob endpoint가 업무 명령을 싣지
  않으므로 시도와 권한 있는 대조 호출 모두 TYPE_INVALID 거부와 commit 0을
  기대한다. 고객 장소로의 내부 이동 4와 raw-primitive 1도 TYPE_INVALID다. 같은
  명령의 FORBIDDEN 증명은 잘 만든 direct-*·C3 api-* subcase에 남고 효과 0·출고·
  반품·폐기 없음 oracle은 그대로다. 보안 단언은 약해지지 않았다(blob으로는 누구도
  쓸 수 없다는 더 강한 단언이 됐다).
- 남긴 것: C3 confirmReceipt·receiveProvisional(2단계 수령), correctEvidence
  (supersedesId), createFollowup(draft), createShipment·recordLegEvent·
  recordHandover(운송 exact), verifyLabel(RECORD와 label version),
  recordObservedMovement(eventId), recordInvoice(PO line fixture 없음),
  resolveEvidenceConflict(claim), createWorkLink(대상 Work fixture 없음),
  createSalesOrder(Step 3 UD-1 구현 전 `Invalid workId`), V4 mcp-definition·
  operations와 C3 mcp-recordRelation(공개되지 않은 capability).

## 실행 비교

모두 `./verify scenarios --actual`(backend package 뒤
`verification/actual/scenarios/run.sh`), 기본 mode(partial fixture, 요청 재작성·
grant 분리 없음), 일회용 PostgreSQL, 기본 heap. inventory는
`verification/actual/scenarios/inventory.py`로 만들었다. 각 실행 직전 같은
commit에서 `./verify prepare`가 PREPARED(problems 0)였고 실행 시작 때 작업 트리는
비어 있었다(`working-tree-status.txt`). 제품은 feat head의 backend 그대로다(Step 3
UD-1·UD-2 구현 전).

| 실행 | 실행 action | PASS(모두 부분 fixture) | FAIL/ADAPTER | FAIL/TEST | NOT_IMPL |
|---|---:|---:|---:|---:|---:|
| s5d 기준 `../s5d-adapter/run-6b9aed36.json` | 5,781 | 54 | 565 | 104 | 79 |
| 중간 `run-0183320b.json` | 5,839 | 94 | 523 | 105 | 80 |
| **최종 `run-68d8dbd9.json`** | **5,913** | **96** | 519 | 107 | 80 |

| 첫 실패 묶음 | 6b9aed36 | 68d8dbd9 |
|---|---:|---:|
| C3 `access-before/response/data/grantId` 없음 | 74 | 0 |
| expected FORBIDDEN, observed TYPE_INVALID | 66 | 35 |
| 상류 FORBIDDEN `Unavailable scope` | 77 | 51 |
| `Unsupported slot for <capability>`(envelope) | 70 | 64 |
| 권한 있는 대조 호출 commit 0(`count mismatch: 0 != 1`) | 6 | 72 |

- 6b9aed36 대비 21 subcase가 더 많은 action을 실행했고 줄어든 것은 없다.
- 새 PASS 46: C3 31, V4 15. 잃은 PASS 4: V4 direct·batch·mcp·management-trade.
  createSalesOrder에서 workId를 빼자(UD-1) 아직 workId를 요구하는 feat backend가
  `Invalid workId` TYPE_INVALID로 답한다. Step 3 3713c28e(UD-1 구현)를 통합하면
  다시 닿아야 한다. 모두 부분 fixture 위의 PASS이고 runtime 인수가 아니다.
- C3 접근 문맥 pointer 74→0. 남은 1은 T08 issue-within-scope의 createGrant 응답
  `/response/grantId`다(다른 응답).
- TYPE_INVALID-before-FORBIDDEN 66→35: 남은 35는 항목 4의 "남긴 것"(C3 27, V6
  2, V4 6 중 4는 위 `Invalid workId`, 2는 공개되지 않은 capability)이다.
- 상류 FORBIDDEN 77→51: C3 approveRecall 9·T19 9·T20 6·C3 getPolicy 3이
  사라졌다. 남은 것은 C3 target-before 42(정의 draft 21, ReturnAuthorization 9, Restriction·
  DispositionBasis·Dispatch·CommandRecord 각 3: installer 형식 없음), T26
  reserve 4(round 13 미결 11), V1·T21 after 3, T02 1, T20 approval 1.
- 권한 있는 대조 호출 commit 0이 6→72로 는 것은 C3 시도가 이제 FORBIDDEN에
  닿고(그 assertion은 통과) 다음 assertion인 대조 호출 commit을 처음 평가하기
  때문이다. 원인은 "하지 않은 것과 기록"에 적었다.
- 중간 실행에서 C3 회수 승인 precondition이 처음 APPLIED했다(항목 6). 그 응답의
  approvalId 위치를 고친 68d8dbd9가 9 subcase를 다음 단계로 보냈다.

### 남은 첫 실패 상위(68d8dbd9)

| 건수 | 첫 실패 | 소유 |
|---:|---|---|
| 77 | worker route 제품 surface 없음(C3) | 제품 surface |
| 72 | 권한 있는 대조 호출 commit 0(C3) | fixture 사실·installer 형식·slot 실체(위) |
| 51 | 상류 FORBIDDEN | installer 형식(C3), Step 2 미결(T26) |
| 35 | expected FORBIDDEN, observed TYPE_INVALID | 항목 4 남긴 것, Step 3 UD-1 |
| 30 | client/model runner 없음(C3 model-query) | model runner |
| 27 | `Invalid workId`(createSalesOrder) | Step 3 UD-1(3713c28e) |
| 17 | 관찰 원천 locks | 제품 gap |
| 17 | `Unsupported slot for createWork: nextAction` | Step 3 UD-2 |
| 16·9 | `GOAL_SLOT_MISSING` periodStart·periodStartInclusive | Step 2 미결 5(round 13) |
| 14 | 관찰 원천 identity_matches·tombstones·deletion_log·domain_records | 제품 gap |
| 13 | process/clientProbe host control 없음 | host adapter |
| 12 | VERSION_UNSUPPORTED(정의 lifecycle) | Step 3 |
| 10 | `Unsupported query field` | case 요청(TEST) |
| 9 | T19 EVIDENCE_UNVERIFIED(송장 원본·정산 scope) | case 증거 chain |
| 8 | proposePurchase `Missing workId`(E1·T23·T26) | Step 2 미결 6(round 13) |
| 6 | HTTP 500(Step 3 발견 8) | 제품 |
| 6 | T01 HELD VERSION_UNSUPPORTED | Step 3 발견 5 |

## Step 3 발견

1. **capability slot 검사의 false positive.** `CapabilitySlotCheck.intents`가
   capabilityId·slots·intentKind COMMAND를 가진 모든 객체를 명령으로 센다.
   저장 Definition 모양(사용자 결정 2)의 `slots/package/verbs[i]`(예:
   `{capabilityId: createWork, slots: {item: TradeItem, destination: Place}}`)도
   명령으로 세어 30 명령(C3 migrate 3, T21·V1)이 위반으로 남는다. 정의
   package 안은 검사에서 빼야 한다.
2. **분할 자식 index와 transit 수령 검사.** `ContractValidator.splitChildren`과
   `contracts/fixture-place-kinds.json` transitReceipt.splitChildPointer는 자식을
   `/response/children/<alias>/segmentId`로만 식별한다. 사용자 결정 UD-3의 index
   pointer로 바꾸면 T14 discrepancy-transit2·unobserved2의 transit 수령이
   ./verify prepare 문제가 되므로 두 subcase는 이름 자식을 유지했고 제품은
   `Unsupported slot for splitQuantity: children`으로 거부한다. validator가
   `quantities[i]`를 index로 읽어야 한다.
3. **split 응답의 이전된 배분.** E2는 split 뒤 `/response/allocations/<i>/id`로
   옮겨 간 배분을 읽는다(s2-inventory: EXECUTABLE 배분은 자기 범위의 새 자식으로
   옮긴다). 제품 split 응답에는 children만 있고 allocations가 없다(계획 §3.4
   명령은 적용 효과를 반환).
4. **계획이 입력으로 두는데 공개 목록에 없는 slot.** 바꾸지 않고 둔 case가 아래
   값을 보낸다.
   - recordDelivery의 소비된 배분 참조(계획 §6 "이미 소비된 배분 참조"): C4·E1·
     E2·T13·T17·T18 allocationId 22.
   - recordExternalReconciliation의 externalOperationId(계획 §7.2
     "externalOperationId의 조회/접수 증거로"): 제품은 outboxId만 받는다(T13·
     T22·T23·T26 9).
   - reserveQuantity FEFO 예외 사유(계획 §6 판매 "FEFO … 예외는 사유를 남긴다"):
     T17 FEFOExceptionReason.
   - transferObligation 인수 근거(계획 §5.3·§7.2, 기존 PG-PLAN-SLOT
     acceptanceEvidenceId)와 부분 이전의 자식 scope(계획 §5.3 "원 의무 scope를
     겹치지 않는 자식 scope로 분할", T10 transfer/remainingPhysicalScope).
   - approvePurchase 소비 정책(계획 §7.1 "유효기간·소비 정책을 저장"): 제품은
     한 order revision 소비만 하고 저장하지 않는다(T13 SINGLE_GOAL_REVISION).
   - createPolicyDraft 확인자·적용일(계획 §4.1 PolicyVersion "공식/내부 근거·
     확인자", §7.2): T15 reviewerId·applicableDate.
   - authorizeReturn 대상 실물 범위(계획 §6 "원 인도·대상 실물/수량"): C4·E1·
     T18 physicalScopeId.
   - adjustQuantity의 MANAGER 확인 근거(계획 §7.1 재고 감소/실사 조정 "기본 배포
     정책은 MANAGER 확인"): T16 decisionEvidenceId, 공개 slot은 evidenceRef 하나.
   - activatePolicy 영향 scope·효력시점(계획 §7.2 "영향 scope·… 정책 fence"):
     T26 affectedScope·effectiveAt.
5. **제외 업무의 거부 code.** T01 excluded-*가 이제 envelope을 지나
   capability 판정에 닿고 제품은 HELD VERSION_UNSUPPORTED("Unsupported capability
   or definition")로 답한다. T01은 REJECTED·CAPABILITY_UNSUPPORTED를 기대하고,
   vocabulary는 CAPABILITY_UNSUPPORTED를 HELD로만 짝지으며 T01 쌍은 기존
   KNOWN_OPEN(pair CAPABILITY_UNSUPPORTED/REJECTED, §1 제외 범위·D01, owner Step
   3)이다. 계획 §1 제외 업무의 거부 code·outcome을 정해야 한다.
6. **installer 정책 상태.** retirePolicy는 PolicyDrafts의 ACTIVE 상태를 읽는데
   installer는 ACTIVE PolicyVersion을 PolicyVersions·ActivePolicies에만 쓴다.
   PUBLISHED 정책은 이제 approverAlias를 말하므로 PolicyApprovals 행을 쓸 수 있다.
7. **harness test가 고정한 옛 case 모양(8 failures, `./verify harness`).** 이번
   변경이 의도한 것이며 test 기대값을 바꿔야 한다(`verification/harness/**`는
   이번 round Step 3 소유라 고치지 않았다).
   - StepTwoRoundEightRegressionTest:159 `/response/children/MOVE20/segmentId` →
     `/response/children/0/segmentId`(UD-3).
   - StepTwoRoundElevenRegressionTest:91 V4 blob-allocation은 이제 권한 있는 대조
     호출도 TYPE_INVALID를 기대하므로 예약 용량 검사 대상이 아니다(목록에서
     빼야 한다).
   - BusinessReviewAssertionTest e2BothRelease…·e2QcRelease…,
     SupplyAssertionTest.regulatoryDecision…: 합성 split 응답이 옛 자식 모양이라
     `split/response/children/0/segmentId`가 없다. 제품 응답
     `children[{index,segmentId,quantity,unit}]`로 만들어야 한다.
   - BusinessReviewAssertionTest.t20RoutesWrap…:89 T20 승인의 consumptionPolicy
     SINGLE_ORDER_REVISION은 제품 규칙을 다시 말해 지웠다.
   - RecipientAcceptanceAssertionTest:149 `slots/acceptedById` →
     `slots/recipientId`.
   - AuthorityPrerequisiteAssertionsTest.recallClosure…:102 합성 회수 승인 응답이
     `{"approvalId": …}`라 case가 읽는 `/response/effects/approvalId`가 없다. 제품
     응답처럼 effects 안에 두어야 한다(68d8dbd9).
8. **HTTP 500(제품 예외).** 0183320b 실행에서 C3 reader 시도가 구조화 오류
   대신 500을 받았다. prepareRegulatoryProcedure·recordRegulatoryDecision은
   `RegulatoryCommands.policy`(RegulatoryCommands.java:17)에서 정책 행의 null
   field로 NPE, revokeGrant는 `IdentityCommands.prepare`(IdentityCommands.java:22)의
   `List.of(…null)`에서 NPE다. MCP route 3건은 `OntologyMcp.toolResult`(:55)가
   `java.time.Instant`를 직렬화하지 못해 500이다. 인가 거부 전에 제품이
   예외로 끝나므로 C3 6 subcase가 `attempt/response/error/code` 없음으로 멈춘다.
9. **기존 발견 유지.** 정의 lifecycle 8 capability·retrySafeCommand·
   recordRelation·emergencyRepair·structureIntent 미공개, worker·nested·projection
   route, model runner, locks 등 관찰 원천.

## 검사

| 명령 | exit | 결과 |
|---|---:|---|
| `./verify prepare`(68d8dbd9) | 0 | PREPARED, preparationProblems 0, capabilitySlotContract FINDINGS: 1,713 명령 중 448 위반(정의 package verb 30 포함), 의도된 위반 면제 3 |
| `./verify harness`(68d8dbd9) | 1 | 533 tests, failures 8. 모두 Step 3 소유 test가 옛 case 모양을 고정한 것(Step 3 발견 7). 0183320b에서는 7 |
| `./verify contract-red <바뀐 32 case.json>`(0183320b) | 1 | 717 scenario 모두 NOT_IMPLEMENTED RED, skip 0(의미 있는 RED) |
| `./verify contract-red verification/cases/C3/case.json`(68d8dbd9) | 1 | 309 scenario 모두 NOT_IMPLEMENTED RED, skip 0 |
| `python3 -I verification/cases/check_request_contracts.py` | 0 | VALID, 41 cases, 374 fixtures, problems 0, knownOpen 4050(시작 5079) |
| `python3 -m unittest discover -s verification/cases` | 0 | 40 tests OK |
| `python3 -m unittest discover -s verification/requirements` | 0 | OK(생성기·round 12·13·14 재현 포함) |
| `python3 -m unittest discover -s verification/mcp-tests -p 'test_*.py'` | 0 | 3 tests OK |
| `python3 -m unittest discover -s verification/coverage` | 0 | OK |
| `validate_catalog.py`·`check_layer_routes.py`·`check_derived_bindings.py`·`check_vocabulary.py --check`·`V2/cases_b_invariants.py` | 0 | VALID / 0 problem |
| `T08/bind_observations.py --check`, `V7/bind_observations.py V4·V6·V7 --check`, `V4/author_review_fixes.py --check`, `author_round12.py --check`, `author_round13.py --check`, `author_round14.py --check` | 0 | CURRENT |
| 생성기 전부 + round 12·13·14 + bind 두 번 실행 | 0 | 두 번째 실행 변경 없음 |
| `./verify scenarios --actual`(0183320b, 68d8dbd9) | 1 | 위 실행 비교 |

## 하지 않은 것과 기록

- 위 "바꾸지 않고 기록한 것"은 새 action·fixture 실체(수령 2단계, 사건 chain,
  운송 cargo/leg, 회수 사건, 정책 content 등)를 case 작성자가 정해야 한다.
- C3 권한 있는 대조 호출(`authorized-committed-once`)의 나머지 실패는 대부분
  fixture 사실이나 installer 형식이 원인이다: 적격성 사실 미설치(reserve·pick·
  dispatch INSUFFICIENT_ELIGIBLE_QUANTITY), 설치 alias에 대한 scope FORBIDDEN(의무·
  구매 승인·정책·handover·capability), stocktake·merge revision·wait·goal 객체가
  필요한 slot, 증거 chain(EVIDENCE_UNVERIFIED).
- T13·T21·V1 assertion이 읽는 `/response/data/goal/...`은 oracle pointer라
  두었다. T20처럼 제품 getWork는 `data.currentGoal`·`data.currentGoalVersionId`를
  준다.
- T03 reject-cycle의 의도된 위반 split과 `/children/0/revision`, T04 primitive
  guard, V8 faultPoint(harness fault 주입을 action.harness로 옮기려면 driver가
  읽어야 한다)는 두었다.
- probe mode(`ACTUAL_RELAX_WIRE=true`)는 돌리지 않았다. backend 전체 suite와
  native S1–S4는 지시대로 돌리지 않았다.
- `./verify harness`는 Step 3 소유 test 8개가 옛 case 모양을 고정해 실패한다
  (Step 3 발견 7). 이번 round에서 고치지 않았다.
