# S4h 정산 리뷰 지적 수정 기록

사용자 Step3(Claude Opus medium) 구현 worker의 기록이다. 기준선은 tag
`step3-s4h-baseline`(`2e1d90cc`), branch는 `step3/s4h-settle`,
worktree는 `/Volumes/VideoStore/Developer/mulino-ontology-step3-s4h-settle`다.
coordinator만 Task branch에 통합한다. 아래 focused PASS는 전체 backend
suite, T19 독립 oracle, S4 종료를 뜻하지 않는다.

입력은 `/Volumes/VideoStore/Developer/.mulino-tools/s4review/settle.json`의
confirmed 10건(s4-settle-00~09)과 unverifiedP3 5건이다. 계획 근거는 §6 정산
(수량·가격·통화 대조, 정산 차이는 별도 미해결 목표, FX snapshot 보존,
은행효과0), §4.3(정정은 영향 판정을 같은 단위에서 재평가, 후속 명령은
확정까지 막음), §5.3(해소 증거·kind별 면제 권한), D13, D19다.

## 판정

| ID | 판정 | 원인과 수정 |
|---|---|---|
| s4-settle-00 (P1) | FIXED | match가 실제 수령량(`actualQuantity`)과 line 전체 주문량으로 대조했다. 이제 현재 인정 기여(`recognizedQuantity`)로 대조하고, 같은 invoiceKind의 line 누적 대조가 주문량을 넘으면 `scopeDifference`다. 60+45(40+초과5)의 두 번째 송장 45는 DIFFERENCE, 수량차이5·원차이5, 의무1. |
| s4-settle-01 (P1) | FIXED | resolver와 query가 다른 판정식을 썼다. `SettlementState.assess` 하나를 명령·조회·해소가 함께 쓴다. SATISFIED는 잔여0·수량차이0·통화/범위 차이 없음·현재 기여 불변일 때만이다. 수량차이20 송장은 조정20 확정 뒤에도 해소 HELD, 조회 UNSATISFIED, 의무 OPEN. |
| s4-settle-02 (P2) | FIXED (읽기·명령 시점) | 불변 Match만 읽었다. 조회·해소·대조가 매번 현재 인정 기여를 다시 구해 다르면 `contributionState=CHANGED`·UNSATISFIED(현재 수량/금액 차이 노출), 검증할 수 없으면 UNVERIFIED다. 정정 시점에 새 정산 의무를 여는 hook은 sales owner 범위라 하지 않았다(아래 요청). |
| s4-settle-03 (P2) | FIXED | 01과 같은 원인. 금액0 조정은 TYPE_INVALID로 거부한다. 수량차이는 금액으로 닫히지 않고 MANAGER 면제(decideQuantityDutyWaiver)로만 닫힌다. |
| s4-settle-04 (P2) | FIXED | 조정이 match 상태·의무 상태를 보지 않았다. 조정(PROPOSE/CONFIRM)은 DIFFERENCE이고 그 SETTLEMENT_DIFFERENCE 의무에 OPEN assignment가 있을 때만 허용하고, 아니면 `REJECTED/SETTLEMENT_DIFFERENCE_NOT_OPEN`이다. |
| s4-settle-05 (P2) | FIXED | FX는 slot이 있을 때만 검사했다. 원본 `fxSnapshot`과 slot은 같이 있거나 같이 없어야 하며(아니면 HELD), canonical INVOICE 검토도 FX 없는 송장의 원본·payload에 snapshot이 있으면 HELD다. |
| s4-settle-06 (P2) | FIXED | CREDIT_NOTE/CORRECTION에 의무·연결 조회가 없었다. `relatedInvoiceId` 필수(원본이 증명, semantic hash 포함), COMMERCIAL/DOMESTIC_TAX에는 거부. 기록 거래에서 원 송장 work에 SETTLEMENT_DIFFERENCE 검토 의무(owner·nextAction·nextCheck)를 연다. 원 송장 `getSettlement`는 `corrections`·`reviewDutyRootIds`를 보이고 검토가 열려 있으면 UNVERIFIED다. 이 의무는 조정으로 해소되지 않고 MANAGER 면제로만 닫힌다. |
| s4-settle-07 (P2) | FIXED | 모든 match가 송장 수량 전체를 INVOICED로 기록했다. 이제 같은 기여·line에서 겹치지 않는 인정 수량만 기록하고 DOMESTIC_TAX는 기록하지 않는다. |
| s4-settle-08 (P2) | FIXED | 04와 같은 원인. RESOLVED 뒤 남은 stale 제안의 확정과 새 제안은 거부되고 결과는 SATISFIED·잔여0으로 남는다. |
| s4-settle-09 (P2) | FIXED | PURCHASE 기여는 저장된 ReceiptCredits만 읽었다. `SettlementState.contribution`이 PHYSICAL_RECEIPT canonical chain을 `TradeEvidence.requireCanonical`로 재검증한다. 수령 claim이 정정되면 새 대조는 HELD/EVIDENCE_UNVERIFIED, 조회는 UNVERIFIED, 해소는 HELD다. |
| P3 transfer test | NOT_HANDLED (소유 밖) | `ResponsibilityService.transfer` test는 responsibility owner 범위다. 확인·수정하지 않았다. |
| P3 INVOICED 초과 | FIXED | s4-settle-07과 같다. |
| P3 FX 누락 | FIXED | s4-settle-05와 같다. 외화 송장에 snapshot을 필수로 하는 정책은 계획에 근거가 없어 넣지 않았다. |
| P3 지급참조 unique | FIXED | 한 송금이 여러 송장을 정산할 수 없었고 위반이 원시 DB 예외였다. V32가 unique를 (조직, 송장, 외부참조)로 바꾸고, 같은 송장 중복은 `CONFLICT/PAYMENT_REFERENCE_ALREADY_RECORDED`다. |
| P3 relatedInvoiceId 미증명 | FIXED | s4-settle-06과 같다. |

## 변경 파일

- `backend/src/main/java/com/mulino/application/trade/settlement/SettlementState.java`(새):
  공통 판정식, 현재 기여 재검증, 의무 OPEN 판정, 정정 검토 root 조회.
- `SettlementCommands.java`, `SettlementQueries.java`, `SettlementResponsibilities.java`,
  `SettlementEvidence.java`, `SettlementTradeFacts.java`(문서만): 위 표.
- `database/migrations/V32__settlement_recognized_contribution.sql`: Matches 수량
  CHECK를 `quantity>=0`·`receivedQuantity>=0`으로(인정 기여0 청구도 의무 있는
  DIFFERENCE로 남김), 나머지 V27 불변식은 그대로. 지급참조 unique 교체. 열 변화 없음.
- `contracts/domain-vocabulary.json`: 오류 code 2개 추가. `DomainVocabularyContractTest` 4/0.
- test: `SettlementCommandPostgresTest`(새 7개, 기존 correction test 갱신),
  `SettlementStateTest`(새, mock).

기존 assertion 변경은 하나다. `correctionInvoiceIsHeldForHumanReviewWithoutNewFulfillment`의
"SETTLEMENT_DIFFERENCE 의무 0"은 리뷰가 결함을 고정한다고 지적한 값이라 "OPEN이고
owner·nextAction·nextCheck가 있는 의무 2"로 바꿨다. 같은 test의 원본에는 이제 필수인
`relatedInvoiceId`를 넣었다. 다른 assertion은 그대로다.

## 실행 증거

환경: `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`, `$MULINO_SLOT`,
Testcontainers PostgreSQL. 실제 모델·은행·외부 기관 호출은 없다. 명령은
`$MULINO_SLOT ./mvnw -B -ntp -f backend/pom.xml -Dmulino.evidence.blob-root=$(mktemp -d) -Dtest=<classes> test`다.
결과 형식은 run/fail/error/skip이다.

| 시점 | classes | exit | 결과 | log |
|---|---|---|---|---|
| RED: 새 test + 이전 main(V32·SettlementState 없음) | SettlementCommandPostgresTest | 1 | 14/7/1/0. 새 7개와 갱신 1개가 결함 동작에서 실패(00: MATCHED, 01: 해소 APPLIED, 04/08: 늦은 확정 APPLIED, 05: APPLIED, 06: 의무0·고아 APPLIED, 09: 정정 뒤 SATISFIED, 지급참조: unique 예외). 기존 6개는 통과 | raw/red.log.gz |
| GREEN | SettlementCommandPostgresTest, SettlementAmountsTest, SettlementStateTest, S4SettlementFactsTest, DomainVocabularyContractTest, S1ReadIntegrationTest, PurchaseCommandPostgresTest, SalesCommandPostgresTest | 0 | 14, 2, 4, 1, 4, 8, 22, 6 = 61/0/0/0 | raw/green.log.gz |

schema parity: V32는 열을 바꾸지 않는다. GREEN 실행의
`backend/target/s4-compatibility-observed.json`을
`docs/execution/s4c-settlement/schema-observed-v29.json`과 JSON으로 비교해 같았다
(열 1865). 그래서 `regenerate-schema-compatibility.py`는 실행하지 않았고
`schema-compatibility.json`도 바뀌지 않는다.

native(코드 commit `6eb7aaec`, source clean 전후, drift 없음):

| 명령 | 출력 dir | receipt status / nativeStatus |
|---|---|---|
| `$MULINO_SLOT python3 verification/actual/s4/build.py` 뒤 `$MULINO_SLOT ./verify actual-s4 /tmp/s4h-settle-actual-s4-b1791437106` | E1·E2·C1·C4 suite | PASS / PASS |
| `$MULINO_SLOT python3 verification/actual/s3/build.py` 뒤 `$MULINO_SLOT ./verify actual-s3 /tmp/s4h-settle-actual-s3-b1791437445` | S3 회귀 | PASS / PASS |

E1의 송장40 대 수령40(인정40) 차이5는 그대로 DIFFERENCE/UNSATISFIED다.
native 입력(author 산출물)은 바꾸지 않았다. 첫 actual-s4 시도는 실행 중
이 기록 디렉터리가 untracked로 생겨 `sourceDrift`로 FAIL(native PASS)이었고,
디렉터리를 치운 뒤 다시 실행했다. 첫 actual-s3 시도는 s3 build 없이 실행해
custody 파일이 없어 실패했다. 이 출력 dir들은 /tmp의 일회성 증거다.

## 미실행과 요청

- NOT_RUN: 전체 backend suite(coordinator), T19 독립 oracle, V2/V3 경합.
- SALE 배송 정정 뒤 정산 재평가의 실제 PG 사례는 없다. 판정식은
  `SettlementStateTest`(mock, CHANGED→UNSATISFIED, 현재 수량차이2·금액차이2000)와
  PURCHASE 수령 chain 정정 PG test로만 확인했다.
- 요청(sales owner): `DeliveryCorrection.correctionImpact`가 정정된 delivery를
  참조하는 Match가 있으면 같은 거래에서 SETTLEMENT_DIFFERENCE 의무를 열게 한다.
  지금은 읽기·해소·대조 시점에만 드러나고, MATCHED 송장은 정정 뒤 owner 있는
  정산 의무가 없다(물류 DELIVERY_CORRECTED_DEFICIT가 열려 있는 동안만 보인다).
- 요청(responsibility owner): P3 transfer 경로 PG test.
- 해소/면제된 수량 차이는 조회에서 계속 UNSATISFIED다. 면제는 충족이 아니라는
  §6 "원차이·조정 근거를 지우지 않는다"로 읽었다.
- V32 이전 Match의 `receivedQuantity`는 실제 수령량일 수 있다. 기록된 환경에
  초과 수령 송장 대조는 없다.
