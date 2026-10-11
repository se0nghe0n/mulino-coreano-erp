# Step 2 round 13: all-of grant과 capability별 slot으로 case를 제품 검사에 통과시킨다

s5b 실행(`../s5b-adapter/run-85ce2365.json`)에서 629 subcase가 기준
조회 FORBIDDEN에서 멈췄다. 한 fixture grant가 품목·업무·장소·segment를
함께 적었고, 제품은 grant가 이름 붙인 모든 차원이 대상과 맞을 때만
권한을 준다(2026-10-09 사용자 결정 all-of). C3·V4·V6·T08·V7은 모든
명령에 같은 slot 묶음을 보내 인가 전에 TYPE_INVALID로 거부됐다. 이번
round는 그 두 원인을 없애고, slot 이름과 값 모양을 계약으로 판정하고,
C3 fixture alias와 T08 검사 순서를 정리했다. 고친 뒤 실제 제품에 다시
실행해 확인했다(AGENTS.md 2026-10-09 run-driven 절차).

worktree `mulino-ontology-step2r-round13`, branch `step2r/round13`,
기준 `41f8d709`. backend·`verification/actual/**`·actual adapter Java는
읽기만 했다. 첫 두 작업자가 세션 한도로 끊겨 세 번째 작업자가
a9b6fe35부터 이어받았다.

## Commit

| commit | 내용 |
|---|---|
| `b17a2d76` | fixture grant를 all-of 하나로 줄이고 정적 인가 검사(`check_request_contracts.py` grant_problems)를 더한다 |
| `7b6c69b4` | capability별 선언 slot, rename·{value,unit}·wait 계약, C3 미사용 alias 정리 |
| `d0ce5153` | 7b6c69b4 실행 inventory `run-7b6c69b4.json` |
| `a9b6fe35` | 공유 slot case의 인가 probe가 필수 slot을 채운다 |
| `88667217` | a9b6fe35 실행 inventory `run-9d213f5a.json`(파일 이름은 amend 전 commit) |
| `53e71c0c` | 계약이 정한 slot 모양(split·move·Work goal·placeHold·handover), 생성 명령의 subject 선언, SalesOrder 명사별 ITEM 판정, fixture별 C3 alias 정리와 WORK2 works 항목 |
| `4afb2721` | feat(8eb72d91: s5c Step 3 adapter와 2026-10-11 사용자 결정) merge |
| `7ca87800` | 맨 근거 alias를 참조로, 근거를 인용하는 actor에 getEvidence(citedEvidenceRead 42 capability). 53e71c0c 실행 inventory `run-53e71c0c.json` 포함. commit 본문의 "57개"는 오기이고 42개다 |
| `913f88cd` | 사용자 결정 3: handler slot 이름(destination→destinationId, salesOrderLineId→salesLineId, reserve action SELL, activateWork goalVersionId), T25 corpus category 이름 |
| `19761e36` | 사용자 결정 2: T21·V1·C3 정의 package를 저장 Definition 모양으로 |
| `1f649a1a` | 고객 조직을 목적지로 적은 판매 line에 CUSTOMER 장소(C1·T03·T04·T05·T16·V2·V3 fixture 33개) |
| 이 commit | 최종 실행 inventory와 이 문서 |

## 항목별 상태

| 항목 | 상태 | 요약 |
|---|---|---|
| 1. all-of grant | DONE(case) / OPEN(Step 3 S3-1·S3-2·S3-9) | b17a2d76: grant마다 그 actor가(위임자로도) 부르는 capability가 모두 품목을 내놓을 때만 ITEM key를 남기고 나머지 차원과 `scopeComposition`을 지운다. `check_request_contracts.py`가 subcase마다 인가를 기대한 조회·명령은 grant가 덮고 FORBIDDEN 고정 행동은 덮지 않음을 정적으로 확인한다. 이번 resume: getObject 명사별 판정(SalesOrder {TARGET, WORK}, 53e71c0c), 인용 근거의 getEvidence(7ca87800), 판매 line 목적지(1f649a1a). 남은 기준 조회 FORBIDDEN은 installer가 쓴 Work를 제품이 못 보는 것(S3-1)과 설치 형식 없는 alias(S3-2)다 |
| 2. capability별 slot | DONE | 7b6c69b4·a9b6fe35: 공유 slot case 5개는 선언 slot에만 값을 투영하고 인가 probe가 닿도록 필수 slot을 채운다. 53e71c0c: 생성 명령의 subject 선언 삭제로 "Declared subject differs from command target" 11건이 사라졌다. 남은 `expected FORBIDDEN, observed TYPE_INVALID`는 공유 slot 묶음이 아니라 설치되지 않은 실체·값 모양·정의 verb·이동 검사 순서다(실행 비교) |
| 3. slot 이름·값 모양 | DONE / 일부 기록 | 아래 판정표. 계약이 정한 곳은 계약, 이름이 없는 곳은 사용자 결정 3대로 제품 handler 이름. 시험 내용이 바뀌는 5묶음은 기록만 했다 |
| 4. C3 alias | DONE / OPEN(Step 3 S3-2) | 7b6c69b4: 쓰지 않는 설치 불가 alias 4개 삭제, 쓰는 alias 6개에 관계 사실. 53e71c0c: fixture마다 쓰는지로 판정해 설치 불가하거나 사실 없는 미사용 alias를 지우고(Q60·B20·WORK-B·ALLOCATION2 포함, model-query 기반 fixture.json은 case 전체 기준), priorEntities에만 있던 WORK2에 baseline works 항목을 준다. 같은 원칙으로 1f649a1a가 판매 line 목적지 장소를 준다 |
| 5. T08 검사 순서 | DONE | 계획 §3.3이 "필수 slot·타입·단위·관계 cardinality·지원 효과·적용 정책 순"을 정하고 요청자·위임을 인증 문맥에서 검증한다. `intent.schema.json`은 `additionalProperties: false`라 organizationId·actorId·delegatorId·role이 든 envelope는 타입 단계에서 깨진다. 제품의 TYPE_INVALID 우선이 계획 순서와 같으므로 b17a2d76이 코드 oracle을 TYPE_INVALID로 바꿨다. 효과 0·REJECTED·외부 자료 없음 oracle은 유지. error 객체 전체 비교는 Step 2 미결 10, V4 이동의 같은 순서 문제는 S3-10 |
| 사용자 결정 1–3(2026-10-11) | DONE | 1: 목록 slot을 감싸거나 풀지 않았다. 2: 19761e36. 3: 913f88cd와 위 판정표 |

## 실행 비교

모두 `./verify scenarios --actual`(backend package 뒤
`verification/actual/scenarios/run.sh`), 기본 mode(partial fixture, 요청
재작성·grant 분리 없음), 일회용 PostgreSQL. inventory는
`verification/actual/scenarios/inventory.py`로 만들었다. 최종 실행 직전 같은
commit에서 `./verify prepare`를 돌렸다(T25가 그 `prepare.json`을 읽는다).
최종 실행은 기본 heap(4 GB)에서 harness가 모든 subcase를 마친 뒤 report를
한 문자열로 쓰다 `OutOfMemoryError`로 끝나(`Json.write`, Main.java:110)
`JAVA_TOOL_OPTIONS=-Xmx10g`로 다시 돌렸다. case·제품은 같다.

"첫 실패 상류 FORBIDDEN"은 첫 문제가 앞선 action의 FORBIDDEN 거부인 subcase
수, 마지막 열은 FORBIDDEN을 기대한 probe가 TYPE_INVALID를 받은 subcase 수다.

| 실행 | 실행 action | PASS | 그중 부분 fixture | FAIL/TEST | FAIL/ADAPTER | NOT_IMPL | 첫 실패 상류 FORBIDDEN | expected FORBIDDEN·observed TYPE_INVALID |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| s5b 기준 `../s5b-adapter/run-85ce2365.json` | 1,664 | 0 | 0 | 3 | 769 | 30 | 629 | 0 |
| s5c 기준 `../s5c-adapter/run-35ec4c83.json`(feat 제품, 옛 case) | 1,685 | 5 | 5 | 3 | 769 | 25 | 645 | 0 |
| `run-7b6c69b4.json` | 4,403 | 10 | 10 | 29 | 677 | 86 | 179 | 35 |
| `run-9d213f5a.json`(a9b6fe35) | 4,399 | 10 | 10 | 29 | 677 | 86 | 179 | 31 |
| `run-53e71c0c.json` | 4,471 | 10 | 10 | 29 | 676 | 87 | 174 | 20 |
| **최종 `run-1f649a1a.json`** | 4,559 | 17 | 17 | 58 | 648 | 79 | 173 | 31 |

- s5b 대비 실행 action 1,664→4,559. 611 subcase가 더 많은 action을 실행했고 8개가
  줄었다(V8: feat f5f0081f부터 adapter가 provision하지 않은 environment를 보내기
  전에 멈춘다, s5c README).
- 기준 조회·상류 FORBIDDEN 629→173. 남은 173은 grant가 아니다. C3 150은 설치되지
  않는 거래 alias(S3-2, 114)와 installer가 쓴 Work를 제품이 못 보는 getWork(S3-1,
  36), T20 6·T21 1·V1 2도 S3-1이다. T19 9는 이번 fix로 근거 alias·getEvidence를
  지나 설치되지 않는 PurchaseOrderLine `PO-A`에서 멈춘다(S3-2). T26 4(판매 line
  미설치, Step 2 미결 11)와 T02 1(단위 미설치, S3-9).
- 공유 slot `TYPE_INVALID`(s5b probe 65 `expected FORBIDDEN, observed
  TYPE_INVALID`, 대부분 "Unsupported ... slot")는 없다. 남은 31은 공유 slot 묶음이
  아니다: V4 blob probe 10이 새로 연 blob route에서 "Capability and intent kind
  must match endpoint"(OntologyRest.java:30, 형식 검사가 인가보다 먼저인 계획
  §3.3 순서, S3-10과 같은 판정 필요), V4 이동 4(S3-10), 증거 명령의 정의 verb
  C3 4·V4 1(S3-8), C3 registerItem 정수 2(S3-6), 설치되지 않는 실체를 가리키는 값(C3
  correctEvidence·recordObservedMovement 4, V6 receiptId 2), query field 3, 의도된
  계약 밖 field 1.
- PASS 17(모두 부분 fixture 위): T10 ordinary-emergency·reason-missing, T20 wire 5와
  locationFridayTypeError, T25 coverage 5, V4 raw-crud 4. s5c는 T25 5였다.
- FAIL/TEST 29→58은 feat의 adapter가 worker route를 `no product surface`(TEST)로
  분류한 C3 18·V4 10·V6 1이다.

### 남은 첫 실패 상위(최종 1f649a1a)

| 건수 | 첫 실패 | 소유 |
|---:|---|---|
| 173 | 상류 FORBIDDEN | Step 3(S3-1·S3-2·S3-9), Step 2 미결 11 |
| 59 | `filter field missing: commandIdempotencyKey`(관찰 filter) | adapter 관찰 원천 |
| 51 | `Invalid typed selector`(getObject 명사) | Step 3 S3-3 |
| 31 | expected FORBIDDEN, observed TYPE_INVALID | 위 목록 |
| 30 | client/model runner 없음(C3 model-query) | model runner |
| 28 | worker route 제품 surface 없음 | 제품 surface |
| 27 | `Invalid workId`(createSalesOrder) | Step 3 S3-4 |
| 17 | `Unsupported lifecycle slot`(T26·V5) | Step 3 S3-5 |
| 16 | 관찰 원천 locks | 제품 gap(s5c) |
| 16·9 | `GOAL_SLOT_MISSING` periodStart·periodStartInclusive | Step 2 미결 5 |
| 15 | target-before revision 없음 | 위 조회 실패의 뒤 |
| 13 | `Unsupported evidence slot` | Step 2 미결 8 |
| 12 | VERSION_UNSUPPORTED(정의 lifecycle) | Step 3(결정 2 구현) |

## item 3: slot 이름과 값 모양

판정 기준은 두 단계로 바뀌었다. brief는 capability·정의 계약을 심판으로
정했다. 계약 문서가 slot을 정한 곳(`s2-inventory/contracts.md`의 재고
명령, `s3-receipt`, `s4-recall`, `s1-evidence`, `intent.schema.json`의 값
모양)은 그대로 심판이다. 계약이 이름을 정하지 않은 곳은 2026-10-11 사용자
결정 3(AGENTS.md, 8eb72d91)대로 제품 handler가 받는 slot이 정의다. case가
제품 이름을 쓰고 oracle의 의미는 그대로 둔다. 이름을 바꾸면 case가 시험하는
것이 달라지는 곳은 고치지 않고 아래에 적었다. 목록 slot은 결정 1대로
그대로 둔다(intent schema의 상한 있는 배열은 Step 3가 공개한다. 그때까지
`check_request_contracts.py`는 PG-INTENT-ARRAY-SLOT KNOWN_OPEN으로 센다).

| 묶음(s5b·s5c 수) | 근거 | 조치 |
|---|---|---|
| dispatch `cargoPlaceId` | `fixture-place-kinds.json` dispatchTransit이 제품 slot `transitPlaceId`를 선언 | 7b6c69b4 rename(유지) |
| reserve `orderLineId`·`salesOrderLineId` | 계획 §6 order line, `s4-sales/contracts.md` salesLineId | 7b6c69b4·913f88cd rename(T20 포함). V8 reserve40도 salesLineId다 |
| typed `{value,unit}` 수량 | intent schema는 두 모양 허용, s2-inventory는 quantity·unit 분리 | 7b6c69b4 값+unit(유지). feat a8bcbbaf로 판매·재고 handler가 typed 수량도 받는다 |
| sales order `packagingVersion`·`endpoint`(C4 26 등) | 계획 §6 포장 조건·인도 끝점 | 7b6c69b4 packageTerms·deliveryEndpoint(유지) |
| Work goal `workKind`(T13 16) | 계획 §5.1 업무 유형 | 7b6c69b4 workType(유지) |
| waitWork 평평한 slot(T11 13) | 계획 §5.2 wait 6요소 | 7b6c69b4 wait aggregate(유지) |
| C3 범용 slot 묶음 | 결정 3 | 7b6c69b4·a9b6fe35 선언 slot 투영(유지) |
| splitQuantity `children` | s2-inventory: segmentId·quantities[]·unit | 53e71c0c: `[{value,unit}]`→quantities+unit, 0 loss·allocationTransfer·reason 삭제(T03·T04·T16·V2, E1·E2·T13·T14 reason). alias가 붙은 children은 미결 1 |
| moveQuantity `quantity`/`unit` | s2-inventory: leaf 전체 이동 | 53e71c0c: fixture leaf 전량과 같은 값만 삭제(T26 4) |
| Work goal `lotIdentification`(T09) | 계획 §3.3: LOT는 수령 단계 slot | 53e71c0c: `MISSING` 삭제 |
| Work goal `interval`(T09) | 계획 §5.1, handler period | 53e71c0c: EXISTS_IN·THROUGHOUT의 interval→period |
| Work `itemId` slot(T07·T21·V1) | handler: TradeItem subject | 53e71c0c: subject로 |
| placeHold `kind` | handler category(QC·CUSTOMER·COMMERCIAL·RECALL) | 53e71c0c: kind→category(C1·T03·T04·T05·T16·V3) |
| proposeHandover `toOwnerId`(T10) | 계획 §5.3 수신자, handler recipientId | 53e71c0c: recipientId. fromOwnerId·residualObligationIds·reason은 handler에 없고 nextAction·nextCheckAt 필수 누락은 KNOWN_OPEN |
| 생성 명령의 subject 선언(C3·V4·T08) | s2-inventory 명사 subject: handler가 묶는 대상만, 0개 허용 | 53e71c0c: 만들 실체의 명사 선언 삭제 |
| 근거 alias 맨 문자열(T13·T14·T15·T19·V8) | s1-evidence: ID는 server UUID | 7ca87800: `{"$alias": ...}` |
| createDraft `destination`(T20 18) | 결정 3, handler destinationId | 913f88cd: typed Place 참조→destinationId |
| reserveQuantity `action: SELL`(C4·E1·E2·T17·T18·T21·V1) | 결정 3: reserve는 언제나 SELL | 913f88cd: 삭제 |
| activateWork `goalVersionId`(T07 12·T21·V1) | 결정 3: workId만 받고 현재 GoalVersion을 활성화 | 913f88cd: workId와 같은 draft 응답에서 온 값만 삭제 |
| T25 corpus category 이름 | corpus 원문(`verification/model-corpus/corpus.json`) | 913f88cd: categories-exact 기대값을 corpus 이름으로. 수는 그대로. catalog의 corpusCounts key는 설명용이라 두었다 |
| 정의 lifecycle package(T21·V1·C3) | 결정 2: 제품이 저장하는 Definition record | 19761e36: 아래 절 |

### 바꾸지 않고 기록한 것(이름을 바꾸면 시험 내용이 달라짐)

- T26 13·V5 4 createWork: `nextAction`·`nextCheckAt`을 보내고 assignment와
  응답 responsibility의 그 값을 단언한다. 제품 createWork는 공개 평면 형식과
  내부 `goal` 형식 모두 이 slot이 없다. 빼면 oracle이 제품 기본값을 보게
  된다. Step 3 발견 S3-5.
- T22 6 dispatchPurchaseOrder: `workId`·`approvedRevision`·`approvedHash`.
  제품은 `proposalId`·`proposalHash`·`approvalId`를 받는다. fixture에 승인된
  proposal이 없어 work를 proposal로 바꾸면 다른 실체를 지명하게 된다.
  proposal·승인 fixture를 쓰거나(installer가 PurchaseProposal을 설치하지
  못한다, S3-2) 명령으로 만들어야 한다.
- T21·V1 `try-v1-reserve`: 판매 line 없이 `workId`로 v1 Work를 지명한다.
  reserve는 판매 line을 받으므로 work→line은 이름이 아니라 대상 변경이다.
  T26 reserve의 workId(판매 line과 함께 보냄)도 line의 work와 같은지
  fixture가 말하지 않아 두었다.
- T07 12 createDraft `predicateInputId`: 술어 진리표의 입력을 고르는 값이다.
  제품 slot이 없고 계획 §3.2는 술어를 정의 규칙 payload로 둔다. T06
  `verb`·`goalPredicate`·`targetId`(4)도 같다.
- T06·T09 등 CUMULATIVE_EVENT 기간 누락(T13 16 periodStart, T11·T12 9
  periodStartInclusive), T09 관측 충족 정책 누락: 계획 §5.1 필수값이고
  case 작성자가 값을 정해야 한다.

### 정의 package(사용자 결정 2)

`request_contract.stored_definition`이 작성용 package를 제품 Definition
record(`Definition.java`)로 쓴다. T21·V1은 `author_round13.py`
edit_definitions, C3는 `author_prerequisites.py`가 같은 함수를 부른다.

- nounTypes→nouns(`core`는 coreRef가 자기 이름일 때). verb slot이 참조하는
  TradeItem·Place는 core noun으로 더한다. DecimalWithUnit slot→DECIMAL과
  같은 이름의 속성(목표 단위, 소수 0, 1..1, READ).
- WORK verb→`createWork` COMMAND DRAFT verb, capabilities에 createWork
  `1.0.0`/`core-v1`/`1.0.0`(CommandHandler 기본 semantic version,
  DefinitionValidator가 요구하는 runtime contract)를 더한다.
- goalTemplate→goal(quantityMode·endpoint·evaluatorVersion)과 typed
  predicate(STATE_AT은 stateQuantity·CURRENT_STATE, 그 밖은
  quantitySum·VERIFIED_DISTINCT, 목표량은 minimum). evaluatorId는
  evaluatorVersion과 같아 뺐다. dueAt·evaluationTime·evidencePolicyVersion은
  GoalVersion 값이라 정의에 두지 않는다.
- compatibilityManifest→capabilities. regressionFixtureRefs·
  regressionResults는 정의 내용이 아니라 package 옆 slot이다.
- 한 field 변형 package(validation-constraints 9, invalid-executable 4,
  revision·mutation)는 v2 저장 정의에 그 결함 하나를 넣는다. sql·script·
  newExecutionPrivilege는 저장 정의 밖 field로 남아 의도된 위반 그대로다.
- six-impact 5건은 fixture가 말하는 v1(ARRIVED·CUMULATIVE_EVENT·arrival-v1,
  slot item·quantity·destination·dueAt, relation locatedAt 1..1, 목표
  minimum 1 BOX)에 변경 하나와 그 draft의 manifest를 넣는다. optional
  attribute의 noun은 case가 말하지 않아 QuantitySegment로, required slot
  변경은 Work의 destination REFERENCE 속성(requiredStage ACTIVATE_WORK)으로
  썼다.
- 기록: relation-meaning draft는 change 형식으로 두었다. 저장 relation에
  meaning field가 없어 cardinality 1→2만 쓸 수 있고 그러면 diff class가
  RELATION_MEANING_CHANGED가 아니게 된다. manifest 변형의
  `definitionVersion`은 package 자신의 version과 달라 뺐고
  semanticVersion·outputSchemaVersion은 v2 manifest 값을 따랐다.
- 제품의 8개 정의 lifecycle capability는 아직 없다(VERSION_UNSUPPORTED,
  s5c). 이 정렬은 정적이며 실행으로 확인하지 못했다.

## Step 3 발견(case를 두고 제품·installer 쪽에서 닫을 것)

S3-1. **installer가 설치한 Work를 제품 조회가 보지 못한다.**
`ScenarioFixtureInstaller.work`(ScenarioFixtureInstaller.java:445)는
`mulino_work_read_Works`에 `lifecycleMode=COMMAND` 행만 넣고
`WorkTransitions`를 쓰지 않는다. `WorkReadRepository.workHistory`
(WorkReadRepository.java:31-37)는 COMMAND Work를 transition snapshot에서만
읽으므로, 설치된 Work가 하나도 보이지 않고 `getWork`가
`DomainError.forbidden()`("Unavailable scope")으로 답한다. a9b6fe35
실행에서 getWork 44건(C3 36, T20 6, T01 2)이 모두 FORBIDDEN이었다. grant
문제가 아니다(위임자 grant는 ITEM P, Work는 TARGET·WORK·ITEM P를
내놓아 all-of로 통과한다). installer가 revision 1 snapshot을 함께 쓰거나
fixture Work를 IMPORTED로 설치해야 한다.

S3-2. **installer에 형식이 없는 거래 alias.** C3·V4가 대상 조회로 읽는
PurchaseOrder·PurchaseOrderLine(T19 PO-A)·PurchaseProposal·Invoice·Recall·Shipment·
RegulatoryProcedure·ReconciliationCase·DefinitionPackage·Restriction·
DispositionBasis·Obligation·Handover·Dispatch·ReturnAuthorization·
CommandRecord·Lease·ExternalOperation·Occurrence·CargoScope는
`SUPPORTED_TYPES`(ScenarioFixtureInstaller.java:23-24)에 없다. 이 round는
쓰는 alias에 이웃 사실이 함축하는 관계 사실을 주었다(author_round13.py
C3_FACTS). Grant alias(GRANT)는 actor grant로 이미 설치되므로 그 ID에
묶기만 하면 된다. PolicyVersion POLICY는 형식은 있으나 installer가 활성
정책만 설치한다(kind·content·effectiveFrom 필수, 상태 없음). C3의
approvePolicy·activatePolicy는 IN_REVIEW 초안이 필요하다.

S3-3. **getObject가 받지 않는 명사.** Allocation·Obligation·Restriction·
DispositionBasis·Dispatch·ReturnAuthorization·Handover·CommandRecord 등은
`ApplicationQueries.objectTypes`에 없어 `Invalid typed selector`가 된다
(KNOWN_OPEN PG-TARGET-REVISION-READ). 계획 §3.4는 명사 조회에서 연결
업무를 반환한다고만 한다.

S3-4. **createSalesOrder가 계획에 없는 입력을 요구한다.** 계획 §6
입력은 customer·item/qty/unit/due/destination·인도 끝점·품질/포장
조건이고 결과로 "별도 인도 목표"를 만든다. handler(SalesCommands.java:16-18)는
기존 `workId`와 `price`·`currency`를 필수로 요구해 `Invalid workId`로
거부한다.

S3-5. **createWork에 초기 책임의 nextAction·nextCheckAt slot이 없다.**
계획 §5.2 activateWork는 주 책임자와 초기 의무를 같은 거래에서 만들고
§5.3은 대상 업무가 인간 owner·nextAction·nextCheck를 갖추라고 한다.
T26·V5는 createWork에 그 값을 보내고 assignment의 nextAction·
nextCheckAt을 단언하지만, 공개 형식(WorkLifecycle.java:92)과 내부 `goal`
형식(validateKeys, 같은 파일 97행) 모두 받지 않는다(`Unsupported
lifecycle slot` 17).

S3-6. **intent schema와 handler의 값 모양.** `intent.schema.json` slot
값은 문자열·boolean·{value,unit}·{type,id}·aggregate object이고 수가 없다.
registerItem은 decimalPlaces를 정수로 요구한다("Integer base precision
required", C3 2). typed {value,unit} 수량은 feat a8bcbbaf 이후 판매·재고
handler가 받는다(case는 그 전 capability 계약대로 값+unit으로 보낸다). 목록
slot은 사용자 결정 1로 정리됐고 schema 공개가 남았다(PG-INTENT-ARRAY-SLOT,
지금 211건 KNOWN_OPEN).

S3-7. **split 결과의 자식 식별.** 제품 split은 새 leaf를
`effects.segmentIds`에 quantities 순서로만 돌려준다
(InventoryCommands.java:99). `command-response.schema.json`은 effects를
object로만 정한다. case는 `/response/children/<index|alias>/segmentId`로
자식을 읽는다(T03·T13·T14·T15·T16·V2·E1·E2). 응답 모양을 계약으로 정해야
case pointer와 `ContractValidator.splitChildren`을 함께 고칠 수 있다.

S3-8. **증거 명령의 subject 선언.** attachEvidence·recordActivity는
subjectRefs를 선언하면 정의에 해당 verb가 없어 "Subject action is not
published"로 거부한다(C3 4). s2-inventory 계약은 선언 0개를 허용하므로
case를 고칠 수도 있으나, 증거 명령은 typed subject slot이 필수라 선언과
slot의 관계를 s1-evidence 계약이 정해야 한다.

S3-9. **installer가 설치하지 않는 fixture 사실(실행으로 확인).** T02
conversion-no-substitution의 기준 조회 FORBIDDEN은 P_BUNDLE 품목이 설치되지
않아서다(`baseline.units`·`baseline.unitConversions` omitted, BUNDLE 단위).
T26 BASE_ORDER_LINE은 work·목적지가 없어 omitted이고(T26 생성기 소유 fixture,
case 쪽 미결 11) 그 reserve가 FORBIDDEN이다.

S3-10. **V4 이동 검사 순서.** V4 destination-id-swap·renamed-action·
raw-primitive·return-bypass·disposal-bypass의 moveQuantity는 CUSTOMER 장소로의
이동을 TYPE_INVALID("Configured internal storage and confirmed custody
required", InventoryCommands.java:61-63)로 거부하고 FORBIDDEN oracle에 닿지
않는다(`nonGrantForbidden` DOMAIN_CHECK_ORDER 5건). 계획 §3.3 순서(지원 효과가
적용 정책보다 먼저)는 제품 순서를 지지하므로 item 5의 규칙을 적용하면 이
oracle은 TYPE_INVALID가 된다. brief가 item 5를 T08로 한정해 바꾸지 않았다.
coordinator 판정이 필요하다.

S3-11. **harness report 크기.** 최종 실행에서 harness가 802 subcase를 마친 뒤
`Json.write`(writeValueAsString, Main.java:110)가 report 전체를 한 문자열로 만들다
기본 heap에서 `OutOfMemoryError`로 끝났다. 앞 실행 report가 443 MB였다. stream
쓰기나 run.sh의 heap 지정이 필요하다(owner: harness·run.sh). 이번에는
`JAVA_TOOL_OPTIONS=-Xmx10g`로 돌렸다.

S3-12. **기존 발견 유지.** transferObligation acceptanceEvidenceId
(PG-PLAN-SLOT), `action.harness` 전달 경로(s5b 발견 5), direct/batch의
HTTP 400 CAP 오류(V4 `observed "400"`), MCP structureIntent/MRTR(T20).

## Step 2 미결(이번 round에 고치지 않은 case 결함)

1. alias를 붙인 split children(T13 1, T14 3, T15 2): 요청을
   quantities+unit으로 바꾸려면 `/response/children/<alias>/segmentId`
   pointer와 `ContractValidator.splitChildren`(alias로 자식을 추적해 transit
   수령 규칙을 검사)을 함께 바꿔야 한다. 응답 계약(S3-7)이 먼저다.
2. T06 술어 4건·T07 predicateInputId 12건: 계획 §3.2대로 술어를 정의
   fixture의 규칙 payload로 옮기고 createWork는 conditions로 참조해야 한다.
3. T09 EXISTS_IN·THROUGHOUT: 계획 §5.1의 관측 충족 정책(observationPolicy)이
   case에 없다. 어휘(정책 이름)를 정한 계약이 없다.
4. T26·V5 createWork(위 "바꾸지 않고 기록한 것"): 내부 `goal` 형식과
   `deadlineInclusive`는 공개 평면 slot으로 옮길 수 있으나 S3-5가 남아 같은
   거부가 난다.
5. CUMULATIVE_EVENT 기간: T13 16건은 기간이 없고 T11·T12 9건은 시작 포함
   여부가 없다. 계획 §5.1 필수값이라 case 작성자가 값을 정해야 한다.
6. proposePurchase에 업무·GoalVersion이 없음(E1·T23·T26 8, 계획 §6),
   T22 dispatchPurchaseOrder가 승인 proposal이 아닌 work를 지명(6).
7. confirmReceipt를 임시 접수 없이 부르는 C2·T09·T11·T16(4건):
   s3-receipt 계약은 receiveProvisional 뒤 receiptId·canonicalOccurrenceId를
   요구한다.
8. recordActivity·attachEvidence 어휘(C5·T05·T06·T11·T14·T22 12건)를
   s1-evidence 계약의 subject·kind·sourceNamespace·externalEventId·
   sourceVersion·effectiveFrom·timeZone·timePrecision·valueState·payload로
   옮기는 일.
9. T16 recordStocktake의 locationId·evidenceId.
10. T08 payload-org `no-identity-error-details`: oracle이 error 객체 전체를
    `{code}`와 같다고 본다. `command-response.schema.json`은 error에
    `message`를 허용하고 제품은 "Unsupported command field"를 함께 준다.
    oracle이 계약보다 엄격하다. brief가 oracle 완화를 금하므로 바꾸지 않고
    사용자·reviewer 판정으로 남긴다.
11. T26 BASE_ORDER_LINE fixture(T26 생성기 소유): work·목적지가 없어 판매
    line이 설치되지 않는다.
12. 응답 pointer: C4·E1·E2·T17·T18의 reserve는 `/response/line/id`, split
    자식은 `/response/children/...`를 읽는다. 제품은 `effects.salesLineId`·
    `effects.segmentIds`를 돌려준다. 응답 모양 계약(S3-7)과 함께 정해야 한다.
13. createSalesOrder의 계획 필수값(due·품질/포장 조건) 누락 case(E2 등)는
    S2-SLOT-REQUIRED KNOWN_OPEN이다.

## 검사(최종 1f649a1a)

| 명령 | exit | 결과 |
|---|---:|---|
| `./verify prepare` | 0 | PREPARED, preparationProblems 0 |
| `./verify harness` | 0 | 530 tests, failures 0 |
| `./verify contract-red <바뀐 20 case.json>`(53e71c0c) | 1 | 568 scenario 모두 NOT_IMPLEMENTED RED, skip 0(의미 있는 RED) |
| `./verify contract-red <53e71c0c 뒤 바뀐 31 case.json>`(최종 1f649a1a) | 1 | 614 scenario 모두 NOT_IMPLEMENTED RED, skip 0 |
| `python3 -I verification/cases/check_request_contracts.py` | 0 | VALID, 41 cases, 374 fixtures, problems 0, knownOpen 5079(시작 a9b6fe35 5254) |
| `python3 -m unittest discover -s verification/cases` | 0 | 39 tests OK(test_request_contract 33) |
| `python3 -m unittest discover -s verification/requirements` | 0 | 39 tests OK(생성기·round 12·13 재현 포함) |
| `python3 -m unittest discover -s verification/mcp-tests -p 'test_*.py'` | 0 | 3 tests OK(C3·T26 고정점 포함) |
| `python3 -m unittest discover -s verification/coverage` | 0 | 68 tests OK |
| `validate_catalog.py`·`check_layer_routes.py`·`check_derived_bindings.py`·`check_vocabulary.py --check`·`V2/cases_b_invariants.py` | 0 | VALID / 0 problem |
| `T08/bind_observations.py --check`, `V7/bind_observations.py V4·V6·V7 --check`, `V4/author_review_fixes.py --check`, `author_round12.py --check`, `author_round13.py --check` | 0 | CURRENT |
| 생성기 전부 + round 12·13 + bind 두 번 실행 | 0 | 두 번째 실행 변경 없음 |
| `./verify scenarios --actual`(최종, 기본 mode) | 1 | 위 실행 비교 |

## 하지 않은 것

- probe mode(`ACTUAL_RELAX_WIRE=true`) 실행은 하지 않았다.
- `run-53e71c0c.json`은 실행 도중 Python 규칙 파일(request_contract.py,
  request-contracts.json)을 고쳐 `workingTreeDirty: true`다. scenario 실행의
  Java harness는 그 파일을 읽지 않는다(`PreparationAssetChecks`만 읽는다).
  시작 시 `working-tree-status.txt`는 비어 있었다.
- 정의 package 정렬은 제품에 lifecycle capability가 없어 정적으로만 확인했다.
- 위 Step 2 미결과 "바꾸지 않고 기록한 것"은 case 작성자의 값 결정이나
  다른 실체 지명이 필요해 고치지 않았다.
- 7ca87800 commit 본문의 citedEvidenceRead capability 수(57)는 오기다.
  실제 42개다. force push·history 재작성은 하지 않았다.
