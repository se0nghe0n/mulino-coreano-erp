# Step 2 round 10: 적용 명령의 제품 검사 사슬 전수 점검

rounds 6–9는 리뷰가 지적한 첫 실패 검사만 고쳤고, 다음 리뷰가 그 뒤의
검사를 찾았다(수령 보관자 → 운송 leaf → pick → 운송 장소·pick 시각).
이번에는 case가 적용을 기대하는 명령(또는 다른 검사 뒤의 검사에서 실패하기를
기대하는 명령)마다 backend의 검사 사슬 전체를 순서대로 읽었다. 그리고 41개
case의 모든 해당 action이 각 검사를 만족하는지 대조했다. fixture나 요청이
어길 수 있는 검사는 `./verify prepare` 규칙으로 옮겼다. backend는 읽기만 했다.

읽은 제품 코드는 다음과 같다(경로는 `backend/src/main/java/com/mulino/`
아래).

- `application/core/ApplicationCommands.apply`: 정의, handler prepare, fence,
  재-prepare, `authorizeScopes`, expectedRevision, `CommandGuard.verify`,
  execute, 다시 `authorizeScopes`, `verifyCommit`, 감사
- `application/identity/IdentityAuthorization.permittedScopes`와
  `application/policy/PolicyCommandGuard`
- `application/inventory/FulfillmentCommands`(reserve·replace·release·pick·
  dispatch, `requireWarehouse`, `requireContinuousAuthority`)
- `domain/inventory/FulfillmentStockPrimitives`(pick, dispatch,
  observeDelivery), `domain/inventory/StockPrimitives`(leaf, split, move,
  replaceRange)
- `application/inventory/InventoryCommands`(split·merge·move·stocktake·
  adjust·dispose)
- `application/trade/receipt/ReceiptCommands`,
  `domain/inventory/ReceiptStockPrimitives`
- `application/trade/returns/ReturnCommands`,
  `application/trade/sales/DeliveryCommands`,
  `application/quality/QualityEligibility`, `application/core/ExecutionClock`,
  `application/core/CommandDefinitionResolver`

점검 도구는 `/tmp`의 일회성 Python audit(action 분류: 적용 기대/고정 code
반례/미단언)과 그 결과를 옮긴 Java 규칙이다. 강제 규칙은
`ContractValidator`의 `pickBeforeDispatchProblems`, `dispatchTransitProblems`,
`occurrenceTimeProblems`, `grantAuthorityProblems`, `commandBasisProblems`,
`warehouseCustodyProblems`, `receiptCustodyProblems`, `placeKindProblems`이고,
계약은 `contracts/execution-preconditions.json`과
`contracts/fixture-place-kinds.json`(1.4.0)이다.

## 분류

각 검사는 넷 중 하나로 분류했다.

- **규칙**: fixture·요청이 어길 수 있고 이번에 prepare 규칙으로 강제한다.
- **adapter**: case 어휘를 제품 slot으로 옮기는 Step 3 adapter의 결정적
  변환이다. 값이 case에 있음을 확인했고, 없는 값을 adapter가 고르지 않는다.
- **installer**: fixture를 제품 행으로 설치하는 Step 3 installer의 규약이다
  (`execution-preconditions.json` `openInstallerConventions`).
- **case 흐름**: 앞선 action이 만드는 상태다. 대상 subcase의 oracle이 직접
  검증한다.

## 공통 gateway (`ApplicationCommands.apply`)

| 순서 | 검사(실패 code) | 충족 근거 | 분류·대조 결과 |
|---|---|---|---|
| G1 | typed parse·capability·intentKind (TYPE_INVALID, VERSION_UNSUPPORTED) | 요청 `capabilityId`·`intentKind` | adapter |
| G2 | 정의 발행·capability 계약 (VERSION_UNSUPPORTED) | 요청 `definitionVersion` = fixture `versions.definition` | audit 대조: 적용 기대 action에서 불일치는 T21·V1이 실행 중 발행한 definition-v2뿐이다(의도). 정의 내용은 installer |
| G3 | handler prepare (아래 capability별) | — | — |
| G4 | 선언 subject와 command target 일치 (TYPE_INVALID) | `subjectRefs` | adapter. reserve의 QuantitySegment subject는 target으로 쓰인다 |
| G5 | `authorizeScopes` (FORBIDDEN) | actor role·grant·scope·위임자 | 규칙 `grantAuthorityProblems` |
| G6 | expectedRevision = 현재 revision (CONFLICT STALE_REVISION) | 요청 `expectedRevision` | 출고는 규칙(pick revision 연결). 나머지는 case 흐름 |
| G7 | `CommandGuard.verify`: 현재 COMMAND 정책의 capability rule, effect class, 승인 | fixture `versions.policy` | installer(정책 내용, 결정 capability grant) |
| G8 | execute 뒤 `authorizeScopes`·`verifyCommit`·감사 저장 | — | case 흐름(T24 감사 실패 fault) |

## capability별 검사 사슬

| capability (action 수: 적용/반례/미단언, case 수) | 순서대로 본 제품 검사 | 충족하는 fixture·요청 값 | 강제 |
|---|---|---|---|
| `dispatchQuantity` (31/32/5, 20 case: C1 C2 C3 C4 E1 E2 T04 T05 T06 T08 T09 T11 T13 T16 T17 T18 T24 T26 V3 V7) | 1 slot 허용 목록 → 2 배분 존재·EXECUTABLE/SUSPENDED (STALE_REVISION) → 3 판매 line 현재·품목/단위 → 4 scope 인가 TARGET·ITEM·PLACE(segment)·WORK (FORBIDDEN) → 5 leaf 미소비 → 6 EXECUTABLE (INSUFFICIENT) → 7 현재 SELL (INSUFFICIENT) → 8 `requireWarehouse` (SCOPE_INELIGIBLE) → 9 pickedAt (TYPE_INVALID) → 10 DISPATCH (INSUFFICIENT) → 11 transitPlaceId·kind TRANSIT (TYPE_INVALID) → 12 PLACE[transit] 인가 (FORBIDDEN) → 13 occurredAt ≤ now, ≥ pickedAt (TYPE_INVALID), SELL·DISPATCH 연속 (INSUFFICIENT) → 14 evidenceRef → G6 revision → execute: leaf 유효시점, 운송 이동 | `allocationId`(alias/`$result`), `cargoPlaceId`, `occurredAt`/요청 `asOf`/시계, 요청 `evidenceRefs`, fixture pick(`pickedAt`) 또는 pick action, grant·장소 scope | 1 adapter / 2 규칙(종결 배분) / 3 case 흐름 / 4·12 규칙 `grantAuthorityProblems`·`dispatchTransitProblems` / 7·10 eligibility 입력은 installer·case 흐름 / 8 규칙 `warehouseCustodyProblems`·`receiptCustodyProblems` / 9 규칙 `pickBeforeDispatchProblems` / 11 규칙 `dispatchTransitProblems` / 13 규칙 `occurrenceTimeProblems` / 14 규칙 `commandBasisProblems` |
| `pickQuantity` (41/3/4, 13 case) | 1 slot {allocationId} → 2 배분 상태 → 3 line → 4 scope 인가 → 5 leaf → 6 EXECUTABLE → 7 현재 SELL → G6 → execute: 'Allocation already picked' | `allocationId`, actor의 pickQuantity role·grant·`capabilityIds` | 4 규칙 `grantAuthorityProblems`(T26 capabilityIds) / 두 번째 pick 규칙 `pickBeforeDispatchProblems` / 나머지 dispatch 1–7과 같다 |
| `reserveQuantity` (56/30/3, 21 case) | 1 slot {segmentId, salesLineId, startQuantity, quantity, unit} → 2 segment·line → 3 품목/단위 → 4 scope 인가 → 5 `requireWarehouse` → 6 leaf → 7 단위·scale → 8 start+q ≤ 실물 → 9 SELL 정확 구간 → 10 겹치는 예약 → 11 line 미이행량 (SALES_LINE_QUANTITY_EXCEEDED) → G6 segment revision | segment(slot 또는 QuantitySegment subject), `orderLineId`/`orderId`, `quantity`; startQuantity 미기재는 0(adapter 규약) | 4 규칙 / 5 규칙 `warehouseCustodyProblems`(fixture segment, baseline 행 포함) / 1 adapter: audit에서 segment 없는 적용 기대 예약 6개는 모두 QuantitySegment subject를 가진다. 같은 fixture segment를 기본 좌표로 두 번 예약하는 적용 기대는 0개 / 9·11 case 흐름 |
| `replaceAllocation` (4/3, C3 T17) | 예약 1–11 + 원 배분 상태, 대체되는 segment scope 인가, 수량 동일 | `allocationId`, 새 segment | 인가 규칙. 나머지 case 흐름 |
| `releaseAllocation` (4/3, C3 T17) | slot {allocationId} → 배분 상태 → line(현재 아니어도 됨) → scope 인가 → G6 | `allocationId` | 인가 규칙 |
| `moveQuantity` (18/23/1, 6 case: C3 T08 T13 T16 T26 V4) | 1 slot·intentKind → 2 evidenceRef (TYPE_INVALID) → 3 leaf: occurredAt ≤ now, validFrom ≤ occurredAt → 4 원천 scope 인가 → 5 목적지·출발지 INTERNAL_STORAGE, 서로 다름, 내부 보관자 (TYPE_INVALID) → 6 목적지 PLACE 인가 → 7 제한 guard → G6 | `segmentId`, `destinationId`, 근거, 시계 | 2 규칙 `commandBasisProblems`(T26 safe-retry 원 이동 2개 수정) / 3 규칙 `occurrenceTimeProblems` / 4·6 규칙 `grantAuthorityProblems` / 5 규칙 `warehouseCustodyProblems` |
| `splitQuantity` (22/11/3, 12 case) | slot·evidenceRef → leaf 시점 → scope 인가 → 자식 2..100, 합 = 부모 (TYPE_INVALID) → 제한 guard → G6 | `segmentId`, `children`/`quantity`, 근거 | 근거 규칙(T26 restore 분할 3개 수정) / 시점 규칙 / 인가 규칙 / 보존은 receipt·transit 규칙(round 8)과 case 흐름 |
| `confirmReceipt` (44/9/12, 17 case) | 관측(receiveProvisional) 존재 → LOT 일치 → canonical occurrence → receivingCustodianId(FORBIDDEN → SCOPE_INELIGIBLE) → 운송 leaf scope 인가 → G6 → execute: 범위·시각 = 대조 범위, 검증 chain, 보관자 상충/미확인 (HELD), 겹치는 수령, 수령지 INTERNAL_STORAGE, 운송 leaf 정확 일치 | 수량·LOT·장소·`occurredAt`, basis slot, 보관자 slot | 보관자·운송 leaf 규칙 `receiptCustodyProblems`(round 7–9) / 장소 scope 규칙 `grantAuthorityProblems` / 관측·canonical chain 설치는 adapter·installer(round 7–9 요청). audit: 적용 기대 수령의 장소는 모두 INTERNAL_STORAGE |
| `receiveProvisional` (4/3, C3 T16) | slot·id → 품목·장소·단위·수량 → occurredAt ≤ now → nextCheckAt > now → owner·supervisor HUMAN, Work 담당 일치 → 원천 event PHYSICAL_RECEIPT → event scope 인가 → 발주 line 범위 | 수량·장소·시각, fixture 책임(`responsibilities`·정책의 nextAction/nextCheckAt) | adapter·installer. audit: 4개 모두 fixture에 nextCheckAt 원천이 있다 |
| `receiveReturn` (8/4/3, 5 case: C3 C4 E1 T13 T18) | 반품 승인 현재(유효기간·정책 hash) → occurredAt ≤ knownAt → nextCheckAt > now → 목적지 INTERNAL_STORAGE → 고객 leaf 하나와 일치 → 보관자 권한·증거 | 승인 `$result`, 목적지, 시각(요청 asOf), fixture 책임 | 시점 규칙 / 목적지 kind audit 0 위반 / 나머지 adapter·case 흐름 |
| `recordDelivery` (18/7/3, 7 case: C3 C4 E1 E2 T13 T17 T18) | slot·id → 품목·장소·고객·Work → occurredAt ≤ now → nextCheckAt > now → 원천 event → event scope 인가 → (확정) 출고 소비 확인, 범위, occurredAt ≥ 출고 occurredAt, TRANSIT leaf 식별 → line 기한·목적지로 정당 수량 | `dispatchId`·`cargoScopeId` `$result`, `occurredAt`, 원천 증거 | 출고 뒤 시각 규칙 `occurrenceTimeProblems` / 나머지 adapter·case 흐름. audit: 18개 모두 fixture에 nextCheckAt 원천이 있다 |

## 이번에 찾아 고친 것

| 검사 | 위반 | 처리 |
|---|---|---|
| dispatch 11·12 운송 장소 (P1) | 기준 `d5a14d90`에서 배분을 지명한 출고 65개 중 40개(C1 1, C2 1, C3 6, T04 4, T05 1, T08 1, T09 2, T11 1, T13 1, T16 2, T24 2, T26 9, V3 3, V7 6)가 장소를 지명하지 않았다. C2·T09 `cumulative-versus-state`와 T13을 뺀 그 fixture에는 TRANSIT 장소도 없었다 | 지금 배분을 지명한 출고 66개(T06 포함) 모두 `cargoPlaceId` TRANSIT. 장소가 없던 fixture에 `TRANSIT`을 더하고 출고자·위임자 장소 scope에 넣었다 |
| dispatch 13 pick 뒤 시각 (P2) | C2·T09 `cumulative-versus-state`(10-06T09:00 < pick 10-07T02:00), T09 `exists-versus-end-throughout`·T11 `cancel-after-shipment`(01:00 < 02:00) | C2·T09 둘은 fixture clock을 업무 시작(10-05T09:00, 10-07T00:00)으로 옮기고 clock control을 넣었다. 출고·수령·조회 시각과 누적100·현재40·EXISTS/STATE/THROUGHOUT 판정은 그대로다. T11은 출고를 pick 시각 02:00으로 적었다(시각 단언 없음) |
| G5 grant capability scope (P2) | T26 만료 반례 9개의 warehouse `capabilityIds`에 pickQuantity가 없었다 | 생성기 `grant_pick`이 capabilityIds에도 넣는다 |
| G5 위임자 (새로 찾음) | fixture actor인 supervisor가 위임한 action을 갖지 않은 fixture 117개(C2 1, C5 5, T06 10, T09 9, T10 19, T11 12, T12 5, T22 15, T24 13, T26 24, V5 4). 11개 capability에서 적용 기대 action 46개(수령 10, 예약 14, pick 13, 출고 2, 이동 4, 분할 3)와 고정 반례 출고 8개가 영향을 받았다. T26 grant 만료 반례는 위임자 결함만으로도 FORBIDDEN이 나와 만료를 가려내지 못했다 | supervisor role·grant에 위임한 action을 모두 더했다(손으로 쓴 75개는 `author_round10.py`, T06/T22/T24는 T06 생성기, T26 파생 4개는 T26 생성기). supervisor가 스스로 하는 action의 기대는 바뀌지 않음을 확인했다(위임자가 가지지 않은 capability로 행동하는 action 0개) |
| moveQuantity 2·split 근거 (새로 찾음) | T26 safe-retry 원 이동 2개(파생 2개 포함 4개)와 restore 분할 3개가 근거를 지명하지 않았다 | T26 생성기가 `evidenceRef`(synthetic 참조)를 넣는다 |
| pick 전 반례의 code (P3 a) | code만으로 받았다 | case가 보이는 이유만 받는다(권한 없음·다른 정의 버전·소비된 배분). round 9 test 두 곳을 맞췄다 |
| 배분 없는 출고 (P3 b) | T06 `dispatch-pending`은 segment만 보내 모든 제품이 형식 오류로 거부했다 | 정정 중인 기출고의 소비된 `old-allocation`을 TRANSIT과 함께 출고하는 잘 갖춘 요청으로 바꾸고 CONFLICT·STALE_REVISION을 고정했다. 정정이 2 BOX를 출고 가능한 실물로 되살리는 제품은 적용해 실패한다. T24 batch·worker 공격은 route 수준 FORBIDDEN으로 남는다 |

## 새 prepare 규칙

| 규칙 | 계약 | 거부하는 모양(회귀 test) |
|---|---|---|
| `dispatchTransitProblems` | fixture-place-kinds.json `dispatchTransit` | 장소 누락(T05·T13·C3·T24·T26), INTERNAL_STORAGE 지명, 출고자·위임자 장소 scope 밖. FORBIDDEN 반례는 scope 면제 |
| `occurrenceTimeProblems` | execution-preconditions.json `clock`·`occurrence` | review 7 모양(옛 clock의 C2·T09·T11), clock advance 없는 미래 발생, fixture pick 이전 출고(V3), 출고 이전 인도(C4) |
| `grantAuthorityProblems` | `grantAuthority` | T26 capabilityIds 누락, 위임자 미보유(C2), actor capability 없음(T05), 만료 grant(T26), 장소 scope 밖(T05 W, T13 W-alt). 고정 반례(T20)는 적용 기대가 아니다 |
| `commandBasisProblems` | `commandBasis` | T26 이동·분할 근거 누락 |
| `warehouseCustodyProblems` | `warehouseCustody` | 예약 실물이 외부 장소(T05 CON40→PORT), 같은 장소로 이동(T26) |
| `pickBeforeDispatchProblems` 강화 | `prePickDispatch` | 이유 없는 FORBIDDEN·STALE_REVISION·INSUFFICIENT·SCOPE_INELIGIBLE 반례(T05, T26 LOT), 배분 없는 출고(T06), api route 공격(T24 deny-worker) |

현재 41 case에서 위 규칙의 문제는 0이다(`./verify prepare` PREPARED).

## 강제하지 않은 것

- 정의 버전 일치(G2)는 audit로만 대조했다. 실행 중 발행하는 버전(T21·V1)을
  구별하는 규칙은 만들지 않았다.
- QualityEligibility 입력(QC·규제·처분 근거·LOT 만료·정책)과 판매 line은
  installer와 case 흐름이다. 출고가 SELL·DISPATCH를 만족하는지는 각 case의
  oracle이 검증한다.
- grant scope 어휘(work·segment·place 외 차원), 결정 capability의 grant
  action, capability별 COMMAND 정책, 발행 정의 내용은 installer 규약으로
  남겼다(`openInstallerConventions`). 특히 실행 중 만든 판매·구매 Work는
  fixture alias가 없어 WORK 차원의 설치 방식이 정해지지 않았다.
- adapter 변환(slot 이름, `receiptId`·canonical chain, nextAction/nextCheckAt의
  fixture 원천, startQuantity 기본 0)은 값이 case나 fixture에 있음만
  확인했다.
- 제품 실행은 하지 않았다. 이 점검은 계약과 selftest이며 제품 PASS가 아니다.
