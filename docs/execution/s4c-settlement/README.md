# S4c 정산 수정과 schema parity 실행 기록

사용자 Step3(Claude Opus medium) 구현 worker의 기록이다. 기준선은 tag
`step3-s4c-baseline`(`50646c99`), branch는 `step3/s4c-settlement`,
worktree는 `/Volumes/VideoStore/Developer/mulino-ontology-step3-s4c-settlement`다.
coordinator만 Task branch에 통합한다. 이 기록의 focused PASS는 전체
backend, T19 독립 oracle, E1 결합 인수, S4 종료를 뜻하지 않는다.

## 범위

| Task | 판정 | 결과 |
|---|---|---|
| 1 | fixture 결함 | `exactManagerConfirmationRetainsOriginalVarianceAndDuty`가 seed한 Place kind `WAREHOUSE`는 product 어휘(INTERNAL_STORAGE/TRANSIT/CUSTOMER)에 없다. `ReceiptStockPrimitives.receive`가 INTERNAL_STORAGE를 요구해 REJECTED됐다. seed만 고쳤고 assertion은 그대로다. |
| 2 | product 결함 | 같은 수령 기여의 두 번째 송장이 차이0이면 MATCHED/SATISFIED였다. CREDIT_NOTE/CORRECTION을 새 이행량으로 대조할 수 있었다. V29와 command/query로 고쳤다. |
| 3 | 기록 결함 | V24에 `DeliveryTransfers.legitimateQuantity`(8eb247d8)가 들어온 뒤 inventory를 다시 관찰하지 않아 1863≠1864였다. V29 뒤 실제 관찰로 재생성했다. |

## 정산 계약 (D19)

- `Matches.scopeDifference`는 같은 line·reference·invoiceKind의 기존
  대조 수량과 이번 대조 수량의 합이 실제 수령/인도량을 넘는지를
  불변으로 저장한다. 참이면 DIFFERENCE와 별도 인간
  `SETTLEMENT_DIFFERENCE` 의무(owner·nextAction·nextCheck)를 만든다.
  물류 목표와 원 차이는 바꾸지 않는다. 은행·세금 발행 효과는 0이다.
- invoiceKind별 누적이므로 COMMERCIAL과 DOMESTIC_TAX 참조는 서로의
  중복으로 세지 않는다.
- CREDIT_NOTE/CORRECTION의 `matchInvoice`는 HELD
  `SETTLEMENT_CORRECTION_REVIEW_REQUIRED`이며 Match·INVOICED 효과·의무가
  0이다. V29 trigger도 같은 INSERT를 거부한다.
- V29 CHECK: status는 MATCHED/DIFFERENCE이고 저장된 차이(scope·통화·
  수량·가격·원금액)에서 파생되며 DIFFERENCE와 dutyRootId 존재가 같다.
- 금액·수량은 Decimal(38,12)/BigDecimal이고 float·tolerance가 없다.
- `getSettlement`와 `getObject`(objectType=Invoice)는 같은 data를 읽고
  scopeDifference가 있으면 UNSATISFIED다. `sort`는 ID만 허용한다.

## 실행 증거

환경: `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`(Java21
zulu, Node24, 임시 JWT public key), machine-wide 2-slot limiter,
Testcontainers PostgreSQL(OrbStack). 실제 모델·은행·외부 기관 호출은 없다.
공통 명령 prefix는 아래와 같다.

```
$MULINO_SLOT ./mvnw -B -ntp -f backend/pom.xml \
  -Dmulino.evidence.blob-root=$(mktemp -d) -Dtest=<classes> test
```

| 시점 | classes | exit | 결과 | log |
|---|---|---|---|---|
| fixture 수정 뒤 | SettlementCommandPostgresTest,SettlementAmountsTest | 0 | 6/0/0/0, 2/0/0/0 | raw/run1.log.gz |
| V29·command 뒤 | 같음 | 0 | 7/0/0/0, 2/0/0/0 | raw/run2.log.gz |
| mutation(overlap=false) | SettlementCommandPostgresTest | 1 | 7 중 exactManager 1 failure `expected <true> but was <false>`; 원복 | raw/mut1.log.gz |
| inventory 관찰 | S1ReadIntegrationTest | 1 | 8 중 1 failure `expected <1863> but was <1865>`(관찰용) | raw/s1-obs.log.gz |
| 최종 | S1ReadIntegrationTest,SettlementCommandPostgresTest,SettlementAmountsTest | 0 | 8/0/0/0, 7/0/0/0, 2/0/0/0 | raw/final.log.gz |

결과 형식은 run/fail/error/skip이다.

## schema-compatibility.json 재생성

이전 `schema-compatibility.json`을 만든 generator는 repository에 없다
(4c1ea087은 결과 JSON과 관찰 artifact만 commit했다). 관찰 artifact에는
열 목록이 없어 inventory를 재현할 수 없었다. 그래서
`S1ReadIntegrationTest`가 `target/s4-compatibility-observed.json`에
`columns`(fresh Flyway public schema의 실제 열)를 함께 쓰게 했고,
[regenerate-schema-compatibility.py](regenerate-schema-compatibility.py)가
그 관찰에서 수와 목록을 복사한다. 손으로 쓴 것은 새 두 열의 review
사유뿐이다. 구조/PK 차이나 미검토 열이 있으면 쓰지 않고 종료한다.

```
# 1) 관찰: 위 Maven 명령으로 -Dtest=S1ReadIntegrationTest (count assertion에서 실패해도 artifact는 기록됨)
cp backend/target/s4-compatibility-observed.json docs/execution/s4c-settlement/schema-observed-v29.json
# 2) 재생성
python3 -I docs/execution/s4c-settlement/regenerate-schema-compatibility.py \
  docs/execution/s4c-settlement/schema-observed-v29.json \
  1414cfe5305192cc7915fcb35db76e97c411617b V1–V29
# 출력: {"columns": 1865, "s3": 1432, "s4Added": 433, "timestampWidening": 340,
#        "notNullStrengthening": 787, "observedArtifactSha256": "29f62fe9…"}
```

관찰값: 열 1865(S3 1432 + S4 433), timestamp widening 340, NOT NULL
strengthening 787(새 항목 `deliverytransfers.legitimatequantity`),
structural/PK 차이 0. `scopedifference`는 CDS도 not null이라
strengthening이 아니다. test의 S4 delta guard를 431→433으로 바꿨다.
`observedArtifactSha256`은 commit한 artifact 파일의 hash다. test는
`Map.of`로 쓰므로 JVM마다 key 순서가 달라 재실행 파일의 bytes는 다를 수
있다. 최종 실행의 artifact는 JSON으로 비교해 같았다.

## 미실행과 남은 요청

- NOT_RUN: 전체 backend suite(coordinator 담당), T19 독립 수량/금액/FX
  oracle, E1·actual S4 adapter, SALE scope 중복 송장의 실제 PG 사례,
  V2/V3 경합.
- V29 이전에 저장된 Match는 overlap을 평가하지 않았다. 기록된 환경에
  그런 행은 없으며 DEFAULT FALSE는 ADD COLUMN을 위한 값이다.
- 중복 대조도 기존처럼 INVOICED execution effect(송장 ID 기준)를
  기록한다. purchase/sales 측이 INVOICED 합계를 이행량으로 쓰는 곳이
  생기면 해당 owner가 중복 송장을 구별해야 한다.
- `legitimateQuantity`는 inventory worker가 V24를 제자리 수정해 추가했다.
  이미 V24를 적용한 DB에는 Flyway checksum 불일치가 난다. coordinator
  판단이 필요하다.
