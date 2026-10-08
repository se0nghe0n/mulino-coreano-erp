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
  운송 실물의 확인 수령은 보관자를 이어받는다(제품 S3 transit fixture와
  같다). 이 보관자는 외부 장소의 적격을 만들지 않는다. 장소가 외부면
  적격은 0이다.
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

## 직접 수령의 보관자

기존 fixture QuantitySegment를 확인하지 않는 confirmReceipt는 직접
수령이다. 이어받을 운송 leaf가 없으므로 수령한 실물의 보관자는 명령의
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
- slot은 같은 조직의 Human/Agent alias이며 fixture actor로서
  confirmReceipt role과 grant를 갖고, grant의 장소 scope가 있으면 수령
  장소를 포함한다. 제품은 수령 보관자에게 그 장소의 현재 수령 권한을
  요구한다.
- 인용한 수령 원본(slot `evidenceId`나 요청 `evidenceRefs`의
  DocumentVersion alias)의 `fixtureContent.receivingCustodianAlias`가 같은
  alias를 지명한다. 다른 보관자를 지명하는 원본은 없어야 하고, 지명한
  원본의 fixture evidence sha256은 canonical content(key 정렬, 공백 없음,
  UTF-8)의 SHA-256이다. adapter는 그 alias를 원본과 event payload의
  `receivingCustodianId`로 설치한다. 제품은 검증된 수령 증거가 지명한
  보관자만 slot으로 받기 때문이다.
- 같은 수령(같은 `canonicalOccurrenceKey`, 없으면 같은
  `commandIdempotencyKey`)의 모든 confirm·retry는 같은 slot 값을 보내거나
  모두 보내지 않는다. 중복 출처나 재시도가 보관자를 바꾸지 못한다.
- 운송 수령(기존 fixture QuantitySegment를 확인하는 수령)은 slot을 보내지
  않는다. leaf의 보관자를 이어받는다.

적용 case는 다음과 같다. 보관자는 확인한 actor와 다르게 둘 수 있으면
다르게 두어, 호출자에서 보관자를 추론하는 제품이 양성 대조에서 드러나게
했다.

| case | 수령 | 보관자 | 원본 | 양성 대조 |
|---|---|---|---|---|
| E1 세 subcase | receipt60·receipt40(procurement가 확인) | receiver(내부 Human, confirmReceipt grant 추가) | warehouse-60·warehouse-40 | `received-custody-control`: 두 수령 뒤·첫 QC 보류 전 W 활성 실물은 수령60·수령40 두 행이고 보관자는 receiver다 |
| T13 `partial-excess-return-relocation` | receipt60·receipt40·receipt5(warehouse가 확인) | warehouse(그 장소의 유일한 수령 권한자) | warehouse-receipt(새 DocumentVersion alias) | `received-custody-control`: W 활성 실물 60·40·5의 보관자는 warehouse다 |

그 밖의 직접 수령(T02·T06·T07·T09·T11·T12·T21·T22·C3·V1·V6·V8)은 수령한
실물을 뒤에서 예약·출고·이동하지 않아 slot이 필요 없다. 그 case의 기대는
수령 자체·기여·멱등·권한이다. 그 실물의 양의 판매 적격을 단언하는
case도 없다.

## 남은 범위

- `verification/actual/**`의 native fixture와 Step 3
  `FixtureInstaller`(kind 기본값 `WAREHOUSE`)는 Step 3 소유다. 이 계약을
  따르도록 바꾸는 일은 cross-owner 요청이다. FixtureInstaller는 수령 원본의
  `receivingCustodianAlias`를 원본 bytes와 event payload의
  `receivingCustodianId`로 설치해야 한다(round 7 cross-owner 요청).
- INTERNAL_STORAGE에서 보관자가 미확인·외부인 segment의 선언 반례와,
  location 없는 segment의 보고는 아직 없다
  (`docs/execution/step2r-round7/README.md` DEFERRED).
