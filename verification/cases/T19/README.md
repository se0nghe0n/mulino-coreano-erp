# 송장 차이 환율 근거와 관리 결정을 보존한다

## 독립 손계산과 범위

사실 입력은 발주100 BOX×1 EUR, 실제 수령100 BOX와 별도 판매·인도30 BOX다. 가격 차이 분기는100
BOX×1.05 EUR=105 EUR여서 원 차이는5 EUR다. 수량 차이 분기는송장105 BOX×1 EUR=105 EUR여서
수량5 BOX와 금액5 EUR를 각각 대조한다. 물류100 도착 목표의 SATISFIED는 정산 차이의 UNSATISFIED를
바꾸지 않는다. 원100 EUR×고정1500 KRW/EUR=150000 KRW이며 후속 fx-v2의1600 적용은 저장된
fx-v1 snapshot을 덮지 않는다. COMMERCIAL·DOMESTIC_TAX·CORRECTION·CREDIT_NOTE를
구별한다. ordinary RECORD는 제안만 기록하며 현재 MANAGER의 hash/revision/근거 확인만 차액5
EUR를 한 번 확정한다. 원 차이와 미해소 의무는 남고 은행·자동 세금 발행/제출은0이다.

fixture의 historicalFacts는 대조에 필요한 원천 입력이다. 승인·매칭·기관 판정·판매 적격
projection·의무 해소를 예상값으로 설치하지 않는다. 구매·수령·반품처럼 이 사례가 검증하는 효과는 Gherkin의 개별
명령으로 실행한다. 모든 fixture는 synthetic=true이며 운영 규제와 지급 권한의 근거가 아니다.

## 원행 관찰 계약

observe는 설치된 isolated organization·item·case·subcase와 API가 발급한 실제
snapshot token을 명시한다. source별 rawRows와 sourceEvidence의 실제
query/parameter/mapping/artifact를 요구한다. 수량은 decimal 문자열과 실제 단위로 검사한다.
identity는 strict alias/result ref를 사용하며 고정 수량 기대값은 결과에서 복사하지 않는다. 의무는
종류와 OPEN 범위로 좁혀 owner·nextAction·nextCheckAt·현재 assignment 수를 검사한다. 거부의
전후 원행과 허용된 감사는 별도로 관찰한다. 원행 배열의 sameAs 비교는 observer의 안정된 ID 순서를 요구한다.

## 규범 catalog와 실제 assertion 연결

| oracle / named observation | subcase / assertion |
|---|---|
| T19.invoice-quantity-price-match / logistics-assessment | invoice-match-difference/logistics-satisfied, invoice-match-difference/logistics-db, invoice-quantity-difference/logistics-still-satisfied |
| T19.invoice-quantity-price-match / original-invoice-difference | invoice-match-difference/original5, invoice-match-difference/original-difference5, invoice-quantity-difference/amount-difference5EUR, invoice-quantity-difference/original-money5 |
| T19.invoice-quantity-price-match / settlement-assessment | invoice-match-difference/settlement-unsatisfied, invoice-quantity-difference/settlement-still-open |
| T19.invoice-quantity-price-match / settlement-duty | invoice-match-difference/settlement-owner-one, invoice-match-difference/settlement-owner-owner, invoice-quantity-difference/quantity-mismatch-owner-one, invoice-quantity-difference/quantity-mismatch-owner-owner |
| T19.invoice-quantity-price-match / matching-links | invoice-match-difference/purchase-three-way, invoice-match-difference/sale-delivery-invoice, invoice-match-difference/adjustment-original-kept, invoice-quantity-difference/quantity-match-scope, invoice-quantity-difference/quantity-difference5BOX |
| T19.fx-document-payment-reference / original-amount | fx-commercial/original100EUR, fx-commercial/invoice-kind-exact, fx-domestic_tax/original100EUR, fx-domestic_tax/invoice-kind-exact, fx-correction/original100EUR, fx-correction/invoice-kind-exact, fx-credit_note/original100EUR, fx-credit_note/invoice-kind-exact |
| T19.fx-document-payment-reference / converted-amount | fx-commercial/converted150000KRW, fx-domestic_tax/converted150000KRW, fx-correction/converted150000KRW, fx-credit_note/converted150000KRW |
| T19.fx-document-payment-reference / fx-snapshot | fx-commercial/fx-exact-snapshot, fx-commercial/fx-original-immutable, fx-domestic_tax/fx-exact-snapshot, fx-domestic_tax/fx-original-immutable, fx-correction/fx-exact-snapshot, fx-correction/fx-original-immutable, fx-credit_note/fx-exact-snapshot, fx-credit_note/fx-original-immutable |
| T19.fx-document-payment-reference / commercial-as-domestic-tax-document | fx-commercial/commercial-no-domestic-tax |
| T19.fx-document-payment-reference / payment-reference-bank-transfer | fx-commercial/bankTransfers-zero, fx-commercial/bankTransfers-count0, fx-commercial/payment-is-reference, fx-domestic_tax/bankTransfers-zero, fx-domestic_tax/bankTransfers-count0, fx-domestic_tax/payment-is-reference, fx-correction/bankTransfers-zero, fx-correction/bankTransfers-count0, fx-correction/payment-is-reference, fx-credit_note/bankTransfers-zero, fx-credit_note/bankTransfers-count0, fx-credit_note/payment-is-reference |
| T19.fx-document-payment-reference / automatic-tax-issue-or-submit | fx-commercial/taxOperations-zero, fx-commercial/taxOperations-count0, fx-commercial/tax-outbox0, fx-domestic_tax/taxOperations-zero, fx-domestic_tax/taxOperations-count0, fx-domestic_tax/tax-outbox0, fx-correction/taxOperations-zero, fx-correction/taxOperations-count0, fx-correction/tax-outbox0, fx-credit_note/taxOperations-zero, fx-credit_note/taxOperations-count0, fx-credit_note/tax-outbox0 |
| T19.fx-document-payment-reference / qc-pass-payment-authorization | fx-commercial/paymentAuthorizations-zero, fx-commercial/paymentAuthorizations-count0, fx-domestic_tax/paymentAuthorizations-zero, fx-domestic_tax/paymentAuthorizations-count0, fx-correction/paymentAuthorizations-zero, fx-correction/paymentAuthorizations-count0, fx-credit_note/paymentAuthorizations-zero, fx-credit_note/paymentAuthorizations-count0 |
| T19.settlement-manager-decision / ordinary-write-confirmed-settlement-effects | settlement-valid/ordinary-not-manager, settlement-valid/ordinary-not-manager-error, settlement-valid/ordinary-confirmation0, settlement-valid/ordinary-denial-audit, settlement-unauthorized/ordinary-not-manager, settlement-unauthorized/ordinary-not-manager-error, settlement-unauthorized/ordinary-confirmation0, settlement-unauthorized/ordinary-denial-audit, settlement-stale/ordinary-not-manager, settlement-stale/ordinary-not-manager-error, settlement-stale/ordinary-confirmation0, settlement-stale/ordinary-denial-audit |
| T19.settlement-manager-decision / observations-proposal-preserved | settlement-valid/ordinary-proposal-APPLIED, settlement-valid/original-preserved-ordinary, settlement-valid/proposal-preserved-ordinary, settlement-valid/ordinary-record-proposal, settlement-unauthorized/ordinary-proposal-APPLIED, settlement-unauthorized/original-preserved-ordinary, settlement-unauthorized/proposal-preserved-ordinary, settlement-unauthorized/ordinary-record-proposal, settlement-stale/ordinary-proposal-APPLIED, settlement-stale/original-preserved-ordinary, settlement-stale/proposal-preserved-ordinary, settlement-stale/ordinary-record-proposal |
| T19.settlement-manager-decision / authorized-confirmation-count | settlement-valid/one-manager-confirmation, settlement-valid/manager-applied |
| T19.settlement-manager-decision / confirmed-settlement-difference | settlement-valid/confirmed5EUR, settlement-valid/confirmed-api5 |
| T19.settlement-manager-decision / preserved-original-difference | settlement-valid/preserved-original-api5, settlement-valid/original5-preserved, settlement-unauthorized/original5-preserved, settlement-stale/original5-preserved |
| T19.settlement-manager-decision / confirmed-difference-bank-transfer | settlement-valid/no-bank-delta, settlement-valid/no-bank0, settlement-unauthorized/no-bank-delta, settlement-unauthorized/no-bank0, settlement-stale/no-bank-delta, settlement-stale/no-bank0 |
| T19.settlement-manager-decision / management-decision-boundary | settlement-valid/current-manager-boundary, settlement-valid/confirmation-not-resolution-one, settlement-valid/confirmation-not-resolution-owner, settlement-unauthorized/manager-denial, settlement-unauthorized/manager-denial-error, settlement-unauthorized/denied-manager0, settlement-unauthorized/denied-manager-responsibility-one, settlement-unauthorized/denied-manager-responsibility-owner, settlement-stale/manager-denial, settlement-stale/manager-denial-error, settlement-stale/denied-manager0, settlement-stale/denied-manager-responsibility-one, settlement-stale/denied-manager-responsibility-owner |

## 실행과 제한

B2는 feaca0af9673620eff9a5ac0f08a657ce14e9ccd다. 실제 제품
adapter·DB·기관·host·model 실행은 NOT_RUN이다. schema 검증과 Cucumber file
selector RED는 준비 증거이며 runtime PASS가 아니다. `SupplyAssertionTest`는 선언한
assertion과 고정 관찰 입력의 mutant를 검사하며 제품 service나 효과를 흉내 내지 않는다. 정확한 실행
명령·exit·발견 수·fixture hash는 evidence/preparation/에 기록한다. 실모델·유료 호출·외부
제출·은행 이체를 실행하지 않는다.
