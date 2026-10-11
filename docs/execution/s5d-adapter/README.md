# S5d: 시나리오 adapter 4차 — round 13 Step 3 발견과 2026-10-11 계약 결정

round 13 통합 실행(e804b144, `.mulino-tools/int-r13/inv.json`)은 PASS 17(모두
부분 fixture), FAIL 707, NOT_IMPLEMENTED 78, 실행 action 4,566이었다. 첫 실패
상위는 기준 조회 FORBIDDEN 173, `filter field missing: commandIdempotencyKey`
59, `Invalid typed selector` 51, `expected FORBIDDEN, observed TYPE_INVALID`
31이었다. 이번 round는 그 원인 중 제품과 installer 쪽의 것(round 13 README의
S3-1..S3-11)을 닫고, 2026-10-11 사용자 결정 1·3(AGENTS.md)을 구현했다. 고친
뒤 실제 제품에 다시 실행해 확인했다(2026-10-09 run-driven 절차).

worktree `mulino-ontology-step3-s5d-adapter`, branch `step3/s5d-adapter`, 기준
`e804b144`. 소유: `backend/**`, `contracts/**`(결정 1·3만),
`verification/actual/**`, `verification/harness/**`. case·fixture
(`verification/cases/**`)와 `database/migrations`는 바꾸지 않았다.

## Commit

| commit | 항목 | 내용 |
|---|---|---|
| `c5724455` | 1 (S3-1) | 설치한 COMMAND Work에 같은 revision의 WorkTransitions snapshot을 쓴다 |
| `0909b756` | 2 (S3-2·S3-9) | 거래·통제 alias(PurchaseProposal·PurchaseOrder(Line)·Invoice·Recall/Investigation·Shipment/CargoScope·RegulatoryProcedure·ReconciliationCase·PolicyVersion 초안·Grant 묶기·managementAuthorities)를 제품 행으로 설치한다 |
| `3672c64f` | 3 (결정 1) | `intent.schema.json`에 상한 100의 배열 값, 제품 envelope가 같은 상한을 검사한다 |
| `fc5f633f` | 8 (S3-11) | report·stdout·hash를 stream으로 쓴다 |
| `87ca35a1` | 6 (S3-6) | registerItem의 decimalPlaces를 schema의 정수 문자열로 받는다 |
| `d06a5b8d` | 1의 뒤 | 보조 조회 FORBIDDEN이 조회 거래를 rollback-only로 만들던 HTTP 500, 없는 정의 ID의 500, RegulatoryProcedure 명사 조회 인가 |
| `3fc01159` | 5 | commandId가 비었거나 기록 없는 관찰 행에 `NO_COMMAND_RECORD` 키 |
| `13e0722f` | 5 (S3-3) | Allocation·Obligation·Restriction·DispositionBasis·Dispatch·ReturnAuthorization·Handover·CommandRecord를 getObject 명사로 읽는다 |
| `be9d22b7` | 2의 수정 | 0909b756의 managementAuthorities 선언이 컴파일되지 않던 것을 고친다(아래 "하지 않은 것") |
| `dcc46a3b` | 4 (결정 3) | `contracts/capability-slots.json`(90 capability)을 공개하고 제품이 그 파일로 slot 이름을 검사한다 |
| `8cd38e62` | 4 (결정 3) | `./verify prepare`가 모든 case 명령을 그 목록에 대조한다 |
| `52cb9458` | 6 (S3-7) | splitQuantity 응답에 요청 순서의 `children` |
| `cead70e9` | 6 (S3-8) | installer convention 정의의 기록 verb intentKind를 handler와 맞춘다 |
| `fe06abe7` | 5 | commandId 열이 없는 행(Works 등)을 만든 명령의 키로 귀속한다 |
| `93ba83ad` | 5 | 설치한 actor의 stableRequestOwner를 그 actor id로 둔다 |
| `6b9aed36` | 2 | Obligation·Handover alias를 책임 행으로 설치한다 |
| 이 commit | — | 최종 실행 inventory `run-6b9aed36.json`과 이 문서 |

## 실행 비교

모두 `./verify scenarios --actual`(backend package 뒤
`verification/actual/scenarios/run.sh`), 기본 mode(partial fixture, 요청 재작성·
grant 분리 없음), 일회용 PostgreSQL. inventory는
`verification/actual/scenarios/inventory.py`로 만들었다. 최종 실행 직전 같은
commit(6b9aed36)에서 `./verify prepare`를 돌렸다(PREPARED). 최종 실행은 **기본
heap**에서 끝났다(`JAVA_TOOL_OPTIONS` 없음). report는 841 MB이고 stream으로
썼다(S3-11).

| 실행 | 실행 action | PASS(모두 부분 fixture) | FAIL/TEST | FAIL/ADAPTER | NOT_IMPL |
|---|---:|---:|---:|---:|---:|
| round 13 `../step2r-round13/run-1f649a1a.json` | 4,559 | 17 | 58 | 648 | 79 |
| 통합 e804b144(coordinator, int-r13) | 4,566 | 17 | 58 | 649 | 78 |
| 중간 cead70e9(파일 없음) | 5,658 | 25 | 99 | 599 | 79 |
| **최종 `run-6b9aed36.json`** | **5,781** | **54** | 104 | 565 | 79 |

| 첫 실패 묶음 | e804b144 | 6b9aed36 |
|---|---:|---:|
| 상류 FORBIDDEN `Unavailable scope` | 173 | 77 |
| `filter field missing: commandIdempotencyKey` | 59 | 7 |
| `Invalid typed selector` | 51 | 0 |
| expected FORBIDDEN, observed TYPE_INVALID | 31 | 66 |
| `Unsupported slot for <capability>`(결정 3 envelope) | — | 117 |

- e804b144 대비 186 subcase가 더 많은 action을 실행했고 6개가 줄었다. 줄어든
  6개는 모두 T25 model 계열로, 첫 실패가 `verifyCoverage input snapshot absent:
  verification/harness/target/evidence/model-binding-preparation.json`이다. 이
  worktree의 target에는 model-binding prepare 산출물이 없다(coordinator 실행에는
  있었다). 제품·adapter 변경과 무관하고 PASS를 잃은 subcase는 없다.
- 새 PASS 37: V4 24, T08 5, T09 3, T11 3, T10 2(나머지 PASS 17은 그대로). V4는
  관찰 행 명령 키 귀속(fe06abe7)과 stableRequestOwner(93ba83ad)가 함께 연 것이다.
  모두 부분 fixture 위의 PASS이고 runtime 인수가 아니다.
- FAIL/TEST 58→104는 C3·V4가 기준 조회를 지나 worker route(제품 surface 없음,
  TEST 분류)에 닿은 것이다(28→74).
- TYPE_INVALID-before-FORBIDDEN 31→66: C3 subcase가 기준 조회를 지나 시도
  명령에 닿으면서, 시도 요청의 모양 오류(declared subject, 증거 field, shipment
  exact slot 등)가 계획 §3.3 순서대로 인가 전에 TYPE_INVALID가 됐다. 아래 Step 2
  결함 4.
- `Unsupported slot for` 117은 결정 3의 envelope 검사다. 대부분은 전에도 handler의
  이름 검사가 TYPE_INVALID로 거부하던 명령이다(예: createWork nextAction 17은 전에
  `Unsupported lifecycle slot`). 전에 이름 검사가 없던 handler(Purchase·Settlement·
  Identity·Policy·Responsibility)에 보낸 계약 밖 slot은 이제 무시되지 않고
  거부된다(예: proposePurchase unitPrice 6, dispatchPurchaseOrder workId 6,
  proposeHandover fromOwnerId 5).

### 남은 첫 실패 상위(6b9aed36)

| 건수 | 첫 실패 | 소유 |
|---:|---|---|
| 77 | 상류 FORBIDDEN | 아래 표 |
| 74 | `access-before/response/data/grantId` 없음(C3) | Step 2 결함 3 |
| 74 | worker route 제품 surface 없음 | 제품 surface(항목 9, 하지 않음) |
| 66 | expected FORBIDDEN, observed TYPE_INVALID | Step 2 결함 4·5 |
| 30 | client/model runner 없음(C3 model-query) | model runner(항목 9, 하지 않음) |
| 27 | `Invalid workId`(createSalesOrder) | 사용자 결정 UD-1(S3-4) |
| 17 | 관찰 원천 locks | 제품 gap(s5c) |
| 17 | `Unsupported slot for createWork: nextAction` | 사용자 결정 UD-2(S3-5) |
| 16·9 | `GOAL_SLOT_MISSING` periodStart·periodStartInclusive | Step 2 미결 5(round 13) |
| 14 | 관찰 원천 identity_matches·tombstones·deletion_log·domain_records | 제품 gap |
| 13 | process/clientProbe host control 없음 | host adapter |
| 12 | VERSION_UNSUPPORTED(정의 lifecycle) | Step 3(결정 2 구현 slice, 하지 않음) |
| 10 | `Unsupported query field` | case 요청(TEST) |
| 10·10 | nested·projection route 제품 surface 없음 | 제품 surface |

상류 FORBIDDEN 77의 내역:

| 건수 | 위치 | 원인 |
|---:|---|---|
| 21 | C3 target-before getDefinition | DefinitionPackage alias(정의 초안)는 정의 lifecycle이 없어 설치할 형식이 없다. 전에는 HTTP 500(`Definition unavailable`)이었다(d06a5b8d) |
| 9 | C3 precondition-approval approveRecall | case가 `scopeId`에 RECALL alias(제품 Recall 명사 = Investigation)를 넣는다. 제품 승인은 RecallScope ID를 받는다. Step 2 결함 6 |
| 9 | C3 ReturnAuthorization | 원 Delivery alias를 설치할 형식이 없다(Delivery는 dispatch·canonical 인도 사건이 필요) |
| 9 | T19 recordInvoice | `OLD-WORK`의 baseline works 항목이 없어 PO-A line·SALE-LINE이 설치되지 않는다. Step 2 결함 7 |
| 6 | T20 purchase-work getWork | 조회 scope가 lotId L인데 구매 Work O1에는 lot이 없다. Step 2 결함 8 |
| 4 | T26 reserve | 판매 line 미설치(round 13 Step 2 미결 11) |
| 3·3·3·3 | C3 Restriction·DispositionBasis·Dispatch·CommandRecord | 사실 없는 자리표시 alias거나, 제품 행이 책임 Work·결정·명령·검증 chain을 요구한다(s5c 결함 6) |
| 3 | C3 getPolicy | 사실 없는 PolicyVersion alias(case 결함) |
| 2·1 | V1·T21 after getWork | 정의 lifecycle 뒤의 Work(VERSION_UNSUPPORTED 뒤 단계) |
| 1 | T02 before getInventory | 원인을 확인하지 못했다. P_BUNDLE 품목·PACK1 segment는 설치되고 grant는 ITEM P_BUNDLE을 덮는다. UnitConversion(서로 다른 두 품목 사이의 환산)은 제품 UnitConversions(한 품목 안)로 표현할 수 없어 설치하지 않았다 |

## 항목별 상태

| # | 항목 | 상태 | 요약 |
|---|---|---|---|
| 1 | S3-1 getWork가 installer Work를 본다 | DONE | c5724455. snapshot을 쓰자 getInventory·getWork가 HTTP 500(UnexpectedRollbackException)으로 바뀌어 d06a5b8d가 원인(@Transactional 보조 조회의 FORBIDDEN이 공유 거래를 rollback-only로 만듦)을 고쳤다. getWork 기준 조회 FORBIDDEN은 C3 36→0. T20 6은 결함 8 |
| 2 | S3-2·S3-9 alias 설치 | DONE(부분) | 0909b756·6b9aed36. 설치: PurchaseProposal, PurchaseOrder, PurchaseOrderLine(T19 historicalFacts), Invoice, Recall/Investigation, Shipment/CargoScope, RegulatoryProcedure, ReconciliationCase, PolicyVersion 초안(DRAFT/IN_REVIEW→DRAFT, APPROVED/PUBLISHED→APPROVED), Grant alias 묶기, managementAuthorities, Obligation, Handover. 설치하지 않음(notRepresented/omitted): Delivery, ReturnAuthorization, Restriction·DispositionBasis(s5c 결함 6), Dispatch, CommandRecord, Lease, ExternalOperation, Occurrence, DefinitionPackage, UnitConversion(품목 간), T26 판매 line(미결 11). 단위(`baseline.units`)는 품목 base unit으로 이미 설치되고 목록 자체에는 제품 행이 없다 |
| 3 | 결정 1 배열 slot | DONE | 3672c64f. schema 배열 branch(maxItems 100, 원소 `$defs.value`), 제품 envelope 같은 상한. `request_contract.py`의 PG-INTENT-ARRAY-SLOT 규칙 삭제는 Step 2 후속(결함 2) |
| 4 | 결정 3 capability slot 목록 | DONE | dcc46a3b·8cd38e62. 아래 절 |
| 5a | commandIdempotencyKey filter | DONE | 3fc01159·fe06abe7. 59→7. 남은 7: T01 excluded-* 6(case가 명령 키 없이 보냄, 결함 5)과 V4 1 |
| 5b | S3-3 getObject 명사 | DONE | 13e0722f. 51→0 |
| 6 | S3-4 createSalesOrder | 사용자 결정 UD-1 | 계획 §6은 workId·price를 입력으로 두지 않고 "별도 인도 목표"를 만든다고 한다. 구현에 schema 변경이 필요하다(아래) |
| 6 | S3-5 createWork nextAction·nextCheckAt | 사용자 결정 UD-2 | 계획 §5.2 activateWork는 초기 의무를 같은 거래에서 만든다. 제품은 초기 의무를 만들지 않는다. 구현에 schema 변경이 필요하다(아래) |
| 6 | S3-6 정수 slot | DONE | 87ca35a1. 정수 slot은 registerItem decimalPlaces 하나였다. schema가 표현하는 정규 정수 문자열을 받는다 |
| 6 | S3-7 split 자식 ID | DONE / UD-3 | 52cb9458. `children[{index,segmentId,quantity,unit}]`. 이름(alias) 붙인 자식 pointer는 UD-3 |
| 6 | S3-8 증거 명령 subject | DONE(installer) | cead70e9. 제품 규칙(선언한 대상이 있으면 정의가 그 capability·intentKind의 verb를 공개)은 계획 §3.2·§3.3과 맞다. installer convention 정의가 증거 verb를 COMMAND로 공개한 것이 원인이었다(handler는 RECORD만 받는다) |
| 7 | S3-10 V4 검사 순서 | Step 2 결함 4 | V4 move 4(`Configured internal storage and confirmed custody required`)와 blob 10(`Capability and intent kind must match endpoint`)은 계획 §3.3 순서(slot·타입·지원 효과가 정책보다 먼저)대로 TYPE_INVALID다 |
| 8 | S3-11 report stream | DONE | fc5f633f. 최종 실행 841 MB report를 기본 heap에서 썼다 |
| 9 | model runner(30)·worker route(74) | 하지 않음 | 시간 안에 닿지 못했다 |

### 결정 3: capability별 slot 목록

- `contracts/capability-slots.json`이 제품 command capability 90개의 wire slot
  이름이다. 요청 형식이 갈리는 곳은 `forms`(createWork/createDraft public·goal,
  recordDelivery confirmation·observation, receiveReturn, correctEvidence
  EVENT·DOCUMENT·CLAIM, recordLegEvent/recordHandover exact, reviseGoal)를 둔다.
- 제품은 그 파일을 classpath에 그대로 묶어 읽는다(`CapabilitySlots`, backend
  pom resource). 시작 때 등록된 handler capability와 목록이 같은지 확인하고,
  명령마다 handler 전에 목록 밖 이름을 TYPE_INVALID로 거부한다. 이름 검사가
  있던 handler는 literal 대신 이 파일을 읽는다. 그래서 공개 목록과 실제로 받는
  이름은 어긋날 수 없다(어긋나면 시작이 실패하거나 같은 집합을 읽는다).
- 이름 검사가 없던 handler(Identity·Policy·Settlement·Purchase·Responsibility·
  Assessment)의 목록은 handler가 읽는 이름을 코드에서 옮겨 적었다. 이 handler에
  읽지 않는 이름을 보내면 전에는 무시됐고 이제는 거부된다. 바뀐 handler의
  backend test 31 class 386 tests가 통과했다(test 하나가 읽히지 않는
  `originalQuantity`를 보내 그 slot을 뺐다). 목록이 실제 읽는 이름보다 넓을 수는
  있다(Settlement의 matchInvoice evidence·canonicalOccurrenceId 등은 범용 증빙
  이름을 함께 둔다).
- `./verify prepare`(6b9aed36): 1,713 명령 중 630이 목록 밖 slot을 쓴다(의도된
  위반 3 제외). case별: E1 63, T13 60, T26 46, T21 35, E2 32, T18 31, T22 30,
  T17 28, T07 25, T11 25, C4 22, T10 21, T15 21, T06 20, T19 19, T14 16, T20 15,
  T02 12, T09 12, T16 11, C3 9, V1 9, T03 8, T04 8, T12 8, C5 7, T05 7, V8 7,
  C1 4, C2 4, T24 4, V3 4, V5 4, T23 2, V2 1. 상위 이름: dispatchQuantity
  destinationId 50·quantity 42·originId 25, confirmReceipt quantity 37·placeId
  22·itemId 21·occurredAt 20·canonicalOccurrenceKey 19, createWork destination
  30·item 30·nextAction 17·nextCheckAt 17, proposePurchase ownerId 26·
  supervisorId 22, pickQuantity quantity 25, transferObligation
  acceptanceEvidenceId 22, recordDelivery allocationId 22·sourceEvidenceId 21,
  approvePurchase consumptionPolicy 22·decidedAt 22, recordActivity activityKind
  18·occurredAt 17. 전체는 `prepare.json`의 `capabilitySlotContract.violations`.
- 제품이 공개하지 않는 capability를 case가 부른다: 정의 lifecycle 8개
  (createDefinitionDraft 52 등, 결정 2 구현 slice), retrySafeCommand 35
  (제품에는 runtime의 retryOriginal만 있고 command handler가 없다),
  recordRelation 13, emergencyRepair 9, structureIntent 6(MCP 도구). prepare는
  이것을 `KNOWN_OPEN CAPABILITY-UNPUBLISHED owner=Step 3`로 남긴다.

## 사용자 결정이 필요한 것

**UD-1 (S3-4) createSalesOrder의 업무와 가격.** 계획 §6 입력은 customer·
item/qty/unit/due/destination·인도 끝점·품질/포장 조건이고 결과는
"SalesOrderRevision/Line·별도 인도 목표"다. 제품은 기존 `workId`와
`price`·`currency`를 필수로 받는다(OrderLines.price·currency NOT NULL). C4·E2 등
27 subcase가 `Invalid workId`에서 멈춘다.
- A: createSalesOrder가 인도 Work를 직접 만든다(owner는 요청 human 또는
  ownerId slot, 목표는 정의의 인도 template). price·currency는 선택으로 하고
  OrderLines의 두 열을 nullable로 바꾼다(migration, database 소유자).
- B: 지금 제품 입력(workId·price·currency)을 계약으로 두고 계획 §6과 case를
  고친다.
- 권고: A. 계획이 주문에서 별도 인도 목표가 생긴다고 정하고, 판매 단가는
  정산(§6 정산 행)의 사실이다. 다만 owner 결정 규칙은 정해야 한다.

**UD-2 (S3-5) 초기 책임의 nextAction·nextCheckAt.** 계획 §5.2 activateWork는
"주 책임자·…·초기 의무"를 같은 거래에서 만들고 §5.3은 대상 업무가 owner·
nextAction·nextCheck를 갖추라고 한다. 제품 activateWork는 초기 의무를 만들지
않고(ResponsibilityService.activated는 human 확인만 한다), 의무 root의
sourceKind는 OCCURRENCE·DECISION·EVENT·CLAIM·DOCUMENT만 허용한다(V10 CHECK).
T26 13·V5 4가 `Unsupported slot for createWork: nextAction`에서 멈춘다.
- A: createWork/createDraft가 nextAction·nextCheckAt을 받고 activateWork가 그
  업무를 원천으로 하는 초기 의무를 만든다(Roots.sourceKind에 WORK 추가, migration).
- B: Work 행에 nextAction·nextCheckAt 열을 두고 의무는 만들지 않는다(migration).
- C: case에서 두 slot을 빼고 제품 기본값을 읽게 한다(계획 §5.2와 다르다).
- 권고: A. 계획이 초기 의무를 명시한다.

**UD-3 (S3-7·Step 2 미결 1·12) 이름 붙은 split 자식.** 제품은 이제
`children[index]`를 돌려준다. case는 `/response/children/<alias>/segmentId`처럼
요청 자식 이름으로도 읽는데, 결정 1의 목록 slot(`quantities`)에는 자식 이름이
없다.
- A: splitQuantity가 `childAliases`(quantities와 같은 길이의 문자열 목록)를 받아
  `children[i].alias`로 돌려준다(slot 계약 추가).
- B: case가 index pointer를 쓴다.
- 권고: B. 자식 이름은 요청 내 지역 이름이라 제품 상태가 아니다.

## Step 2 case 결함(바꾸지 않고 기록)

1. **capability slot 계약 위반 630 명령**(위 결정 3 절). 대부분 계획·계약이 정한
   이름과 다르거나(dispatchQuantity destinationId: 제품은 transitPlaceId),
   handler가 읽지 않는 값을 함께 보낸다(proposePurchase ownerId·supervisorId).
2. `verification/cases/request_contract.py`는 목록 slot을 아직
   PG-INTENT-ARRAY-SLOT KNOWN_OPEN으로 센다. schema가 배열을 공개했으므로 규칙과
   `contracts/request-contracts.json`의 knownOpen 항목을 함께 지워야 한다(지금
   항목만 지우면 checker가 owner 조회에서 실패한다).
3. C3 74 subcase가 `getAccessContext`의 `data.grantId`·`data.grantActions`를
   읽는다. 제품 응답은 `grants[]`(각 ID·actions·scopes)이고 어느 계약도 단일
   grant 평면 field를 정하지 않는다. `/response/data/grants/0/ID`처럼 읽어야 한다.
4. 계획 §3.3 순서(slot·타입·지원 효과가 정책보다 먼저)를 따르면 TYPE_INVALID가
   기대인데 FORBIDDEN을 고정한 oracle: V4 blob 10, V4 move 4(S3-10), C3·V4의
   declared subject 14, C3 시도 요청 모양 오류(shipment exact slot 4, 증거 field 4,
   receiptId·eventId·carrierRef 등 각 2) 등 66.
5. T01 excluded-* 6: 명령 키 없이(`Invalid commandIdempotencyKey`) 보내 계획 §3.3의
   envelope 검사가 capability 지원 판정보다 먼저 TYPE_INVALID다.
   CAPABILITY_UNSUPPORTED를 보려면 유효한 envelope를 보내야 한다.
6. C3 approveRecall·recordRecallNotice 등: `scopeId`에 RECALL alias를 넣는다. 제품
   Recall 명사는 Investigation이고 승인은 RecallScope ID를 받는다.
   `precondition-recall` 응답의 `/response/data/scopeId`를 써야 한다.
7. T19: `OLD-WORK`의 baseline works 항목이 없어 historicalFacts의 PO-A·
   SALE-LINE을 설치할 수 없다.
8. T20 purchase-work: getWork scope의 lotId L이 구매 Work O1(lot 없음, 계획 §3.3
   LOT은 수령 단계)과 맞지 않는다.
9. C3의 사실 없는 자리표시 alias(Restriction·DispositionBasis·Dispatch·
   CommandRecord·Delivery·Lease·ExternalOperation·Occurrence, 사실 없는
   PolicyVersion)와 PUBLISHED POLICY의 승인자.

## 하지 않은 것과 기록

- 0909b756은 그 상태로 harness가 컴파일되지 않는다(managementAuthorities가
  `Exception`을 선언). 그 commit의 부분 실행은 메서드를 더하기 전 build였다.
  be9d22b7이 고쳤고 force push·history 재작성은 하지 않았다.
- fe06abe7 본문의 "PASS 4→33"은 그 commit과 93ba83ad를 함께 넣은 부분 실행
  결과다.
- 결정 2 구현(정의 lifecycle 8 capability), worker route, model runner,
  locks·tombstones 관찰 원천, process host control은 하지 않았다.
- installer가 만든 synthetic 값(구매 승인·발주 channel·Invoice referenceId·
  reconciliation event/claim·의무 원천 문서·handover 만료 등)은 모두
  installerConventions에 남는다. 업무 사실이나 runtime 인수로 표시하지 않는다.
- backend 전체 suite와 native S1–S4는 지시대로 돌리지 않았다(통합 때 coordinator).
  probe mode(`ACTUAL_RELAX_WIRE=true`)도 돌리지 않았다.

## 검사

| 명령 | exit | 결과 |
|---|---:|---|
| backend focused(S2WorkActualIntegrationTest RED→GREEN 포함, d06a5b8d) | 0 | S2WorkActual 8/0, S3RegulatoryActual 9/0, DefinitionQueriesUnavailable 1/0, CommandEnvelope 4/0, ItemPrecisionSlot 2/0, StockCommandPostgres 14/0 |
| backend S1ReadIntegrationTest(13e0722f) | 0 | 10/0 |
| backend 바뀐 handler 31 class(dcc46a3b) | 0 | 386 tests, 0 failure(처음 1 failure를 고친 뒤) |
| backend StockCommandPostgresTest(52cb9458) | 0 | 14/0 |
| `./verify harness`(cead70e9, 6b9aed36에서 다시) | 0 | 533 tests, failures 0 |
| `./verify prepare`(6b9aed36) | 0 | PREPARED, preparationProblems 0, capabilitySlotContract FINDINGS(630) |
| `./verify scenarios --actual`(6b9aed36, 기본 heap) | 1 | 위 실행 비교, workingTreeDirty false |
| 부분 실행 C3·T20·T19(r1·r2), V4·C3·T08(r3), C3(r4) | 1 | 각 commit 본문 |
