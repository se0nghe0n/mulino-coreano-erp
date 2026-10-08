# fixture 장소 종류와 보관자 계약

판매·출고 적격은 실물이 회사가 통제하는 보관 장소에 있고 내부 보관자가
확인될 때만 생긴다(계획 §4.1 장소·보관자, §4.2, §6). 계획은 Place의
종류 어휘를 정하지 않았다. 그래서 Step 2 fixture가 `INTERNAL_WAREHOUSE`,
`WAREHOUSE`, `PORT`, `TRANSPORT`처럼 제각각 쓰거나 kind를 아예 빼면
같은 case의 판정이 adapter의 임의 mapping에 달렸다. 제품은
`INTERNAL_STORAGE`만 내부 보관으로 보고 어휘 밖 kind를 UNKNOWN·적격0으로
다룬다. 그 결과 계약을 지키는 제품이 C1 판매 적격40, C2 예약60·출고60을
만들 수 없었고, 반대로 T16·T17의 적격0 단언은 장소 종류만으로 통과했다
(Step 2 closure review 3, P1).

기계가 읽는 원본은 [fixture-place-kinds.json](fixture-place-kinds.json)이다.
`./verify prepare`의 `ContractValidator.placeKindProblems`가 강제한다.

## 장소 종류

| kind | 보관 | 의미 | 판매·출고 적격 |
|---|---|---|---|
| INTERNAL_STORAGE | 내부 | fixture 조직이 통제하는 창고·보관 장소 | segment가 내부 보관자를 가질 때만 다른 조건으로 판정한다 |
| TRANSIT | 외부 | 운송 중(선박·트럭·운송 구간) | 확정 0 |
| CUSTOMER | 외부 | 고객 장소(인도 완료·고객 현장) | 확정 0 |
| SUPPLIER | 외부 | 공급자·원산지 장소(인계 전) | 확정 0 |
| EXTERNAL_PORT | 외부 | 항만·통관·보세 구역 | 확정 0 |

제품 근거: `docs/execution/s2-inventory/contracts.md`의 moveQuantity
(양쪽 INTERNAL_STORAGE와 내부 custodian), backend
`QualityEligibility.custody`(TRANSIT·CUSTOMER·SUPPLIER·`EXTERNAL_*`은
OUTSIDE, 그 밖의 kind는 UNRECOGNIZED → UNKNOWN·적격0),
`FulfillmentCommands.requireWarehouse`. 제품은 `EXTERNAL_`로 시작하는 모든
kind를 외부로 보지만 fixture는 선언한 EXTERNAL_PORT만 쓴다.

## 보관자

- segment의 `custodianAlias`가 보관자다. Human·Agent alias(fixture 조직의
  actor)는 내부 보관자다. Organization·Customer·Supplier·Manufacturer alias는
  외부 보관자다. 제품의 `InventoryRepository.internalCustodian`도 같은
  조직의 HUMAN·AGENT actor만 내부로 본다.
- INTERNAL_STORAGE에 있는 segment(alias나 `baseline.segments`의
  `locationAlias`·`placeAlias`)는 같은 조직의 내부 보관자를 가져야 한다.
- 수입 중인 자기 화물(TRANSIT·EXTERNAL_PORT)도 내부 보관자를 둔다.
  이 보관자는 외부 장소의 적격을 만들지 않는다. 장소가 외부면 적격은
  0이다.
- 운송 실물의 확인 수령이 보관자를 이어받는 것은 TRANSIT 장소의 식별된
  leaf를 수량 그대로 받을 때뿐이다(아래 "운송 수령", 제품 S3 transit
  fixture와 같다). EXTERNAL_PORT·SUPPLIER·CUSTOMER 장소의 실물이나 leaf
  일부를 운송 수령으로 확인하는 명령은 제품이 거부한다.
- 고객·공급자 장소의 실물은 작성한 보관자를 그대로 둔다.

## 반례 선언

어휘 밖 kind는 명시 반례로만 쓴다. Place alias에
`"kindControl": "UNRECOGNIZED_PLACE_KIND"`를 두고 kind는 어휘 밖이며
`EXTERNAL_`로 시작하지 않는 값이다. C1 `unrecognized-place-kind`가
유일한 반례다. 조건이 같은 위탁40이 INTERNAL_STORAGE에서는 적격40·
ALLOWED이고, kind `WAREHOUSE`(반례)에서는 적격0·UNKNOWN이다. 품목 범위는
보유80·적격40이다. 미확인을 확정 0(DENIED)이나 40으로 바꾸지 않는다.

## 옛 fixture kind

| 옛 kind | 계약 kind |
|---|---|
| INTERNAL_WAREHOUSE, WAREHOUSE | INTERNAL_STORAGE |
| PORT | EXTERNAL_PORT |
| TRANSPORT | TRANSIT |
| EXTERNAL_CUSTOMER(baseline.places) | CUSTOMER |

kind가 없던 Place는 alias 이름의 의미로 정했다. W·W2·W-B·W-alt는
INTERNAL_STORAGE, TRANSIT은 TRANSIT, CUSTOMER·CUSTOMER-PLACE·C_PLACE는
CUSTOMER, SUPPLIER_PLACE·IT-origin은 SUPPLIER, PORT·IT-port·KR-port는
EXTERNAL_PORT다. 전환 기록과 script는
`docs/execution/step2r-round6/`에 있다.

## 운송 수령

confirmReceipt가 slot에서 fixture QuantitySegment(`{"$alias":…}` 또는
typed `{"value":{"$alias":…}}`)를 지명하거나, 그런 leaf를 나눈 앞선
splitQuantity의 명시 자식(`/response/children/<alias>/segmentId`)을
`$result`로 지명하면 운송 수령이다. 제품 `ReceiptStockPrimitives.receive`
(읽기만 했다)는 운송 수령을 다음 조건에서만 받는다. 아니면 INVALID
"Exact identified transit leaf required; split partial cargo first"다.

- leaf는 하나이고 Place.kind가 TRANSIT인 장소에 있다.
- leaf는 식별돼 있다(identifiability가 INDISTINGUISHABLE_MIXTURE가 아니고,
  identificationStatus가 있으면 CONFIRMED다).
- 수령 수량·단위가 leaf와 decimal로 정확히 같다. 일부만 받으면 먼저
  split한다(계획 §4.2 "부분 이동·보류·출고는 먼저 범위가 구별되도록
  분할한다", `docs/execution/s3-receipt/contracts.md`).
- 수령 itemId·lotId가 있으면 leaf의 itemAlias·lotAlias와 같다.
- 수령 장소(placeId·destinationId·locationId)는 INTERNAL_STORAGE다.

운송 수령은 `receivingCustodianId`를 보내지 않고 leaf의 보관자를 이어받는다.
그 실물을 뒤에서 예약·pick·출고·이동하면 leaf 보관자는 내부 보관자여야
한다. 기계 규칙은 [fixture-place-kinds.json](fixture-place-kinds.json)의
`transitReceipt`이고 `ContractValidator.receiptCustodyProblems`가 강제한다.

round 6 전환에서 PORT가 EXTERNAL_PORT가 됐는데, 그 장소의 leaf를 운송
수령으로 확인하는 case가 남았다. 계약을 지키는 제품은 그 수령을 거부해
C2·T09 `cumulative-versus-state`, T16 `provisional-holds`를 통과할 수
없었다. T14의 수령98은 100 BOX leaf를 나누지 않고 일부만 받았다(Step 2
closure review 5, P2). 적용 결과는 다음과 같다.

| case | 수령 | leaf | 처리 |
|---|---|---|---|
| C2·T09 `cumulative-versus-state` | receive60·receive40 | A60·B40 | PORT(EXTERNAL_PORT)에서 TRANSIT 장소로 옮겼다. 내부 보관자 warehouse는 그대로다. PORT를 읽는 assertion은 없었다 |
| T16 `provisional-holds` | confirm | TRANSIT60 | PORT(IT-port, EXTERNAL_PORT)에서 TRANSIT 장소로 옮겼다. 보관자 CUSTODIAN·owner SUPPLIER는 그대로다. grant 장소 scope와 `receipt-transit-double-creation-7`(확인 뒤 운송 장소에 남은 활성 실물 0)도 새 장소를 따른다 |
| T14 `discrepancy-transit2`·`discrepancy-unobserved2` | receive98 | Q100의 자식 RECEIVED98 | 수령 전에 warehouse가 Q100을 98+2로 나눈다(`split98`). 수령98은 자식98 전체를 받고, 운송 중 확인(`transit2`)은 자식2에 기록한다. DB 합계는 활성 행만 읽고, 분할 적용·소비된 자식98·활성 자식2를 단언한다 |

## 직접 수령의 보관자

운송 수령이 아닌 confirmReceipt는 직접 수령이다. 이어받을 운송 leaf가 없으므로 수령한 실물의 보관자는 명령의
`receivingCustodianId` slot에서만 생긴다. slot이 없으면 제품
(`ReceiptCommands`, 읽기만)은 보관자 없는 segment를 만들고, 그 실물의
예약·pick·출고·이동은 SCOPE_INELIGIBLE, 적격은 UNKNOWN이다. 계약을 지키는
제품이 E1과 T13 `partial-excess-return-relocation`의 예약·출고30·W-alt
이동을 만들 수 없었다(Step 2 closure review 4, P2). 기계 규칙은
[fixture-place-kinds.json](fixture-place-kinds.json)의
`directReceiptCustody`이고 `ContractValidator.receiptCustodyProblems`가
`./verify prepare`에서 강제한다.

- 직접 수령의 실물(또는 그 실물에서 split·이동·보류·예약으로 이어진
  결과)을 뒤에서 `reserveQuantity`·`replaceAllocation`·`pickQuantity`·
  `dispatchQuantity`·`moveQuantity`로 쓰면 그 수령은 slot을 보낸다.
- slot은 확인한 actor와 같은 조직(`organizationAlias`)의 fixture actor다.
  actor가 아니거나 다른 조직이면 제품은 확인한 조직의 actor에서 찾지
  못해 REJECTED FORBIDDEN을 낸다(`identity.actor(...).orElseThrow(forbidden)`).
  그 actor는 Human/Agent alias이며 confirmReceipt role과 grant를 갖고, grant의
  장소 scope가 있으면 수령 장소를 포함한다. 아니면 SCOPE_INELIGIBLE이다.
  제품은 수령 보관자에게 그 장소의 현재 수령 권한을 요구한다.
- 수령의 canonical occurrence를 검증한 원본(verification basis)이 같은
  alias를 지명한다. 제품은 그 occurrence의 검증된 chain에서만 보관자를
  읽는다(`ReceiptCommands.evidencedCustodians`,
  `TradeEvidence.verifiedCanonical`). 그래서 원본은 basis slot
  (`verificationBasisSlots`: `evidenceId`·`evidenceIds`·
  `verifiedEvidenceIds`)의 DocumentVersion alias(`$alias`, typed `value`,
  alias 이름 문자열)와, basis slot이 `$result`로 인용한 앞선 action이
  첨부한 문서(그 action의 document·basis slot이 지명한 DocumentVersion
  alias, 그리고 inline JSON content가 밝힌 `receivingCustodianAlias`)다.
  같은 `canonicalOccurrenceKey`의 앞선 confirmReceipt(중복 출처)의 basis도
  같은 occurrence의 chain이므로 센다. 요청 `evidenceRefs`와 다른 slot은
  증인일 뿐 검증 basis가 아니어서 세지 않는다(step2r round 9).
  DocumentVersion은 `fixtureContent.receivingCustodianAlias`로 지명하며
  fixture evidence sha256은 canonical content(key 정렬, 공백 없음, UTF-8)의
  SHA-256이다. inline 문서의 sha256은 content의 SHA-256이다. 다른 보관자를
  지명하는 basis 원본은 없어야 한다. adapter는 각 basis 원본을 수령
  canonical occurrence의 검증된 chain으로 설치하고, 그 alias를 원본과 event
  payload의 `receivingCustodianId`로 설치한다.
- 같은 수령(같은 `canonicalOccurrenceKey`, 없으면 같은
  `commandIdempotencyKey`)의 모든 confirm·retry는 같은 slot 값을 보내거나
  모두 보내지 않는다. 중복 출처나 재시도가 보관자를 바꾸지 못한다.
- 운송 수령은 slot을 보내지 않는다. leaf의 보관자를 이어받는다.

### 보관자 반례 선언

위 규칙은 양성 대조만 쓰게 했다. 그래서 slot을 증거·권한 확인 없이 그대로
보관자로 쓰는 제품도 모든 case를 통과했다(Step 2 closure review 5, P3).
slot을 가진 confirmReceipt action에 `custodyControl`을 두면 제품의 거부를
단언하는 반례가 된다. 값은 FORBIDDEN(outcome REJECTED, round 9),
SCOPE_INELIGIBLE(REJECTED), EVIDENCE_CONFLICT(HELD), EVIDENCE_UNVERIFIED(HELD)다.

- 선언 값은 제품(`ReceiptCommands`)이 처음 걸리는 검사여야 한다. 확인한
  조직의 actor가 아니면(actor 아님, 다른 조직) FORBIDDEN이 먼저다. 다음이
  준비 단계의 SCOPE_INELIGIBLE(Human/Agent 아님, confirmReceipt role·grant나
  장소 scope 없음)이다. 그 뒤 basis 원본끼리 다른 보관자를 지명하면
  EVIDENCE_CONFLICT, basis 원본이 정확히 slot 보관자를 지명하지 않으면
  EVIDENCE_UNVERIFIED다. 요청 `evidenceRefs`의 증인 문서는 이 분류에 들지
  않는다(Step 2 closure review 6, P3).
- subcase는 그 action의 `/response/outcome`과 `/response/error/code`를
  고정하고, 뒤의 observe action에서 `/data/rawRows/segments`나
  `/data/rawRows/receipts`의 count 0으로 효과 0을 단언한다.
- 반례 수령의 결과를 뒤에서 쓰지 않는다. 같은 수령의 slot 일치 검사에서
  반례는 빠진다. 증거가 지명한 보관자로 다시 확인하는 것은 정상이다.
- 이 field는 harness 선언이며 제품에 보내지 않는다(`CaseRunner`는
  `request`만 보낸다).

E1 `receipt-custody-unverified`가 첫 반례다. full-flow-quantities fixture와
구매·출하 선행 명령을 그대로 쓰고 receipt60의 slot만 procurement로
바꿨다. procurement는 W 수령 권한이 있는 내부 Human이지만 원본
warehouse-60은 receiver를 지명한다. 기대는 HELD·EVIDENCE_UNVERIFIED, W
활성 실물 0행, procurement 보관 실물 0행, 수령 원장 0행이다.

적용 case는 다음과 같다. 보관자는 확인한 actor와 다르게 둘 수 있으면
다르게 두어, 호출자에서 보관자를 추론하는 제품이 양성 대조에서 드러나게
했다.

| case | 수령 | 보관자 | 원본 | 양성 대조 |
|---|---|---|---|---|
| E1 세 subcase | receipt60·receipt40(procurement가 확인) | receiver(내부 Human, confirmReceipt grant 추가) | warehouse-60·warehouse-40(`evidenceId` basis slot) | `received-custody-control`: 두 수령 뒤·첫 QC 보류 전 W 활성 실물은 수령60·수령40 두 행이고 보관자는 receiver다 |
| T13 `partial-excess-return-relocation` | receipt60·receipt40·receipt5(warehouse가 확인) | warehouse(그 장소의 유일한 수령 권한자) | warehouse-receipt(새 DocumentVersion alias). round 9부터 요청 evidenceRefs와 함께 `evidenceId` basis slot에도 둔다 | `received-custody-control`: W 활성 실물 60·40·5의 보관자는 warehouse다 |

T13은 receipt40의 20 BOX를 W-alt로 옮기기 전에 20+20으로 나눈다
(`split40`). 제품 moveQuantity는 leaf 전체를 옮기기 때문이다(round 8).

그 밖의 직접 수령(T02·T06·T07·T09·T11·T12·T21·T22·C3·V1·V6·V8)은 수령한
실물을 뒤에서 예약·출고·이동하지 않아 slot이 필요 없다. 그 case의 기대는
수령 자체·기여·멱등·권한이다. 그 실물의 양의 판매 적격을 단언하는
case도 없다.

## 출고 운송 장소

출고는 배분 범위를 창고 보관에서 운송 장소로 옮기는 명령이다(계획 §6).
제품 `FulfillmentCommands`는 pick 검사 뒤에 `transitPlaceId`(TYPE_INVALID),
Place.kind TRANSIT('Transit place required'), 그 장소의 PLACE 인가
(FORBIDDEN)를 차례로 본다. round 9가 pick을 채우자 T05·C3·V7·V3·T24·T04·T13의
적용·실행 시점 기대가 이 검사에서 막혔고, C2·T09·T11·T16·T26의 출고도
장소를 지명하지 않았다(Step 2 closure review 7, P1). 기계 규칙은 [fixture-place-kinds.json](fixture-place-kinds.json)의
`dispatchTransit`(1.4.0)이고 `ContractValidator.dispatchTransitProblems`가
강제한다.

- 배분을 지명한 모든 출고(`slots.allocationId` 또는 요청 최상위
  `allocationId`)는 같은 객체에 `cargoPlaceId`로 fixture TRANSIT 장소를
  지명한다. corpus slot은 C4·E1·E2·T17·T18이 쓰던 `cargoPlaceId`이고 Step 3
  adapter가 제품 slot `transitPlaceId`로 옮긴다. adapter는 장소를 고르지
  않는다. 반례도 양성 출고의 잘 갖춘 사본이어서 장소 누락이 거부 이유가
  되지 않는다.
- FORBIDDEN으로 고정한 반례가 아니면, 출고 actor와 fixture actor인 위임자의
  grant가 장소를 나열할 때 그 TRANSIT 장소를 담는다.
- TRANSIT 장소가 없던 fixture에는 `TRANSIT`(이름 '출고 운송 구간')을 더하고
  출고 권한자와 그 위임자의 장소 scope에 넣었다(round 10
  `author_round10.py`, T06·T26 생성기).

## 남은 범위

- `verification/actual/**`의 native fixture와 Step 3
  `FixtureInstaller`(kind 기본값 `WAREHOUSE`)는 Step 3 소유다. 이 계약을
  따르도록 바꾸는 일은 cross-owner 요청이다. FixtureInstaller는 수령 원본의
  `receivingCustodianAlias`를 원본 bytes와 event payload의
  `receivingCustodianId`로 설치해야 한다(round 7 cross-owner 요청).
- INTERNAL_STORAGE에서 보관자가 미확인·외부인 segment의 선언 반례와,
  location 없는 segment의 보고는 아직 없다
  (`docs/execution/step2r-round7/README.md` DEFERRED).
- 운송 수령의 leaf slot(`segmentId`·`existingSegmentId`·
  `existingTransitSegmentId`)을 제품 receiveProvisional의
  `transitSegmentId`로, splitQuantity의 명시 `children`을 제품
  `quantities[]`와 `/response/children/<alias>/segmentId` 응답으로 옮기는 것은
  Step 3 actual adapter의 몫이다(round 8 cross-owner 요청).
- round 8은 confirmReceipt만 전수 점검했다. leaf 일부를 split 없이
  이동·보류·출고하는 다른 명령(계획 §4.2)은 전수 점검하지 않았다. T13의
  move만 이번에 split을 앞세웠다.
