# 구매부터 반품·정산 후속까지 전체 수량·책임의 인수 계약

B2 `feaca0af9673620eff9a5ac0f08a657ce14e9ccd`에서 작성한
Step 2 테스트 계약이다. 제품 API/DB/host 실행은 NOT_RUN이다.
fixture 설치는 입력이며 업무 효과나 승인 성공을 미리 저장하지 않는다.
모든 action·assertion은 같은 한국어 scenario에 대응한다.

| subcase | 손계산과 책임 |
|---|---|
| full-flow-quantities | 구매100→수령60+40→허용30 판매→반품10으로 W80·과거인도30·적격0을 계산한다 |
| independent-goals-and-owners | 전체 물류100 충족 뒤 송장5EUR 차이와 QC40·반품10·정산 owner는 별도로 남는다 |
| whole-runtime-and-model-reference | 실제 원장·배분·판정·책임·감사·outbox를 묶고 실모델 gate는 독립 host 입력으로 구분한다 |

수량은 BOX이며 송장 차이는 EUR다. API와 독립 DB의 동일 snapshot을
대조한다. 원 행은 각 requested source의 완전한 query·parameters·mapping·
snapshot artifact와 함께 반환해야 한다. 미확인/상충은 0으로 바꾸지 않는다.
거부 감사와 접수 책임은 허용하되 금지된 원장·배분·정상 이행·외부
효과는 명시한 전후 원 행으로 비교한다. 과거 인도와 현재 반품을 빼서
한 값으로 만들지 않는다. 회수25를 다시 폐기해도 distinct 실물은25다.

초기 창고 W는0이며 supplier의 식별된100BOX만 입력으로 설치한다.
수령60+40=100 뒤 인도30과 실제 반품10을 기록하므로 W=100−30+10=80이다.
남은 기관 미확인30·QC 보류40·반품 보류10의 합은80이며 판매 적격은0이다.
수령60의 문서 두 개는 같은 physical occurrence에 연결하므로120이 되지
않는다. 구매100×1EUR=100EUR와 송장105EUR의 차이는5EUR다.
은행 이체0은 BANK_TRANSFER outbox 행과 실제 API count로 대조한다.

inputs의 결정·상태 문서는 B2 시점 입력 artifact이며 제품 실행 증거가
아니다. 미래 runtime/model manifest의 hash를 추정하지 않고 실제
artifact inventory로 식별한다. 부재한 실행은 NOT_RUN이며 유료 모델
실행 권한이나 제품 PASS를 입력으로 만들어 넣지 않는다.

| oracle | named observation과 assertion |
|---|---|
| E1.full-flow-quantities | purchase-cumulative-arrival → full-flow-quantities:purchase-cumulative-arrival-1, full-flow-quantities:purchase-cumulative-arrival-2, full-flow-quantities:e1-mcp-same-snapshot, full-flow-quantities:purchase-cumulative-arrival-1-mcp |
| E1.full-flow-quantities | warehouse-current-held → full-flow-quantities:warehouse-current-held-3, full-flow-quantities:warehouse-current-held-4, full-flow-quantities:warehouse-current-held-3-mcp |
| E1.full-flow-quantities | historical-delivery → full-flow-quantities:historical-delivery-5, full-flow-quantities:historical-delivery-6, full-flow-quantities:historical-delivery-5-mcp |
| E1.full-flow-quantities | return-received → full-flow-quantities:return-received-7, full-flow-quantities:return-received-8, full-flow-quantities:return-received-7-mcp |
| E1.full-flow-quantities | current-sell-eligible → full-flow-quantities:current-sell-eligible-9, full-flow-quantities:current-sell-eligible-10, full-flow-quantities:received-custody-control, full-flow-quantities:current-sell-eligible-9-mcp |
| E1.full-flow-quantities | agency-unverified-held → full-flow-quantities:agency-unverified-held-11, full-flow-quantities:agency-unverified-held-12, full-flow-quantities:agency-unverified-held-axis70, full-flow-quantities:agency-unverified-held-11-mcp |
| E1.full-flow-quantities | QC-held → full-flow-quantities:QC-held-13, full-flow-quantities:QC-held-14, full-flow-quantities:QC-held-13-mcp |
| E1.full-flow-quantities | return-held → full-flow-quantities:return-held-15, full-flow-quantities:return-held-16, full-flow-quantities:return-held-15-mcp |
| E1.full-flow-quantities | bank-transfer → full-flow-quantities:bank-transfer-17, full-flow-quantities:bank-transfer-18, full-flow-quantities:bank-transfer-18-mcp |
| E1.full-flow-quantities | receipt60-doc-duplicate-effects → full-flow-quantities:receipt60-doc-duplicate-effects-19, full-flow-quantities:receipt60-doc-duplicate-effects-20, full-flow-quantities:receipt60-doc-duplicate-effects-21, full-flow-quantities:receipt60-doc-duplicate-effects-22, full-flow-quantities:receipt60-doc-duplicate-effects-23, full-flow-quantities:receipt60-doc-duplicate-effects-24, full-flow-quantities:receipt60-doc-duplicate-effects-26, full-flow-quantities:receipt60-after-doc-mcp-same-snapshot, full-flow-quantities:receipt60-doc-duplicate-effects-22-mcp, full-flow-quantities:receipt60-doc-duplicate-effects-23-mcp, full-flow-quantities:receipt60-doc-duplicate-effects-24-mcp |
| E1.full-flow-quantities | return-added-purchase-arrival → full-flow-quantities:return-added-purchase-arrival-25, full-flow-quantities:return-added-purchase-arrival-mcp |
| E1.independent-goals-and-owners | logistics-goal → independent-goals-and-owners:logistics-goal-1, independent-goals-and-owners:logistics-goal-2, independent-goals-and-owners:goals-mcp-same-snapshot, independent-goals-and-owners:logistics-goal-1-mcp |
| E1.independent-goals-and-owners | invoice-difference → independent-goals-and-owners:invoice-difference-3, independent-goals-and-owners:invoice-difference-4, independent-goals-and-owners:invoice-difference-3-mcp |
| E1.independent-goals-and-owners | QC-duty → independent-goals-and-owners:QC-duty-5, independent-goals-and-owners:QC-duty-6, independent-goals-and-owners:QC-duty-7, independent-goals-and-owners:QC-duty-8, independent-goals-and-owners:QC-duty-9, independent-goals-and-owners:QC-duty-10, independent-goals-and-owners:QC-duty-11, independent-goals-and-owners:QC-duty-hold60-resolved, independent-goals-and-owners:QC-duty-hold60-resolution-evidence, independent-goals-and-owners:QC-duty-hold40-identity, independent-goals-and-owners:QC-duty-11-mcp |
| E1.independent-goals-and-owners | return-duty → independent-goals-and-owners:return-duty-12, independent-goals-and-owners:return-duty-13, independent-goals-and-owners:return-duty-14, independent-goals-and-owners:return-duty-15, independent-goals-and-owners:return-duty-16, independent-goals-and-owners:return-duty-17, independent-goals-and-owners:return-duty-18, independent-goals-and-owners:return-duty-18-mcp |
| E1.independent-goals-and-owners | settlement-duty → independent-goals-and-owners:settlement-duty-19, independent-goals-and-owners:settlement-duty-20, independent-goals-and-owners:settlement-duty-21, independent-goals-and-owners:settlement-duty-22, independent-goals-and-owners:settlement-duty-23, independent-goals-and-owners:settlement-duty-24, independent-goals-and-owners:settlement-duty-25, independent-goals-and-owners:settlement-duty-25-mcp |
| E1.independent-goals-and-owners | two-entry-snapshot → independent-goals-and-owners:two-entry-snapshot-26, independent-goals-and-owners:two-entry-snapshot-27, independent-goals-and-owners:two-entry-snapshot-28, independent-goals-and-owners:two-entry-snapshot-29, independent-goals-and-owners:two-entry-snapshot-30, independent-goals-and-owners:two-entry-snapshot-31, independent-goals-and-owners:two-entry-snapshot-32, independent-goals-and-owners:two-entry-snapshot-33, independent-goals-and-owners:two-entry-snapshot-34, independent-goals-and-owners:two-entry-anchor-snapshot, independent-goals-and-owners:two-entry-anchor-asOf, independent-goals-and-owners:two-entry-anchor-knownAt, independent-goals-and-owners:two-entry-anchor-duty-fields, independent-goals-and-owners:two-entry-anchor-qc-duty-ledger, independent-goals-and-owners:two-entry-anchor-return-duty-ledger, independent-goals-and-owners:two-entry-anchor-settlement-duty-ledger, independent-goals-and-owners:two-entry-anchor-qc-duty-identity, independent-goals-and-owners:two-entry-anchor-return-owner, independent-goals-and-owners:two-entry-anchor-settlement-owner, independent-goals-and-owners:two-entry-duties-noun-verb, independent-goals-and-owners:two-entry-anchor-purchaseArrivalQuantity, independent-goals-and-owners:two-entry-anchor-heldQuantity, independent-goals-and-owners:two-entry-anchor-deliveredQuantity, independent-goals-and-owners:two-entry-anchor-returnedQuantity, independent-goals-and-owners:two-entry-anchor-eligibleQuantity, independent-goals-and-owners:received-custody-control, independent-goals-and-owners:two-entry-anchor-invoiceDifference, independent-goals-and-owners:noun-mcp-same-snapshot, independent-goals-and-owners:two-entry-snapshot-26-mcp, independent-goals-and-owners:two-entry-snapshot-27-mcp, independent-goals-and-owners:two-entry-snapshot-28-mcp, independent-goals-and-owners:two-entry-snapshot-29-mcp, independent-goals-and-owners:two-entry-snapshot-30-mcp, independent-goals-and-owners:two-entry-snapshot-31-mcp, independent-goals-and-owners:two-entry-snapshot-32-mcp, independent-goals-and-owners:two-entry-snapshot-33-mcp, independent-goals-and-owners:two-entry-snapshot-34-mcp, independent-goals-and-owners:two-entry-anchor-snapshot-mcp, independent-goals-and-owners:two-entry-anchor-asOf-mcp, independent-goals-and-owners:two-entry-anchor-knownAt-mcp, independent-goals-and-owners:two-entry-anchor-duty-fields-mcp, independent-goals-and-owners:two-entry-anchor-qc-duty-ledger-mcp, independent-goals-and-owners:two-entry-anchor-return-duty-ledger-mcp, independent-goals-and-owners:two-entry-anchor-settlement-duty-ledger-mcp, independent-goals-and-owners:two-entry-anchor-qc-duty-identity-mcp, independent-goals-and-owners:two-entry-anchor-return-owner-mcp, independent-goals-and-owners:two-entry-anchor-settlement-owner-mcp, independent-goals-and-owners:two-entry-duties-noun-verb-mcp, independent-goals-and-owners:two-entry-anchor-purchaseArrivalQuantity-mcp, independent-goals-and-owners:two-entry-anchor-heldQuantity-mcp, independent-goals-and-owners:two-entry-anchor-deliveredQuantity-mcp, independent-goals-and-owners:two-entry-anchor-returnedQuantity-mcp, independent-goals-and-owners:two-entry-anchor-eligibleQuantity-mcp, independent-goals-and-owners:two-entry-anchor-invoiceDifference-mcp |
| E1.independent-goals-and-owners | logistics-closes-unrelated-duties → independent-goals-and-owners:logistics-closes-unrelated-duties-35, independent-goals-and-owners:logistics-closes-unrelated-duties-36, independent-goals-and-owners:logistics-closes-unrelated-duties-37, independent-goals-and-owners:logistics-closes-unrelated-duties-38, independent-goals-and-owners:logistics-closes-unrelated-duties-35-mcp, independent-goals-and-owners:logistics-closes-unrelated-duties-36-mcp, independent-goals-and-owners:logistics-closes-unrelated-duties-37-mcp, independent-goals-and-owners:logistics-closes-unrelated-duties-38-mcp |
| E1.whole-runtime-and-model-reference | actual-runtime-observation → whole-runtime-and-model-reference:actual-runtime-observation-1, whole-runtime-and-model-reference:actual-runtime-observation-2, whole-runtime-and-model-reference:actual-runtime-observation-3, whole-runtime-and-model-reference:actual-runtime-observation-4, whole-runtime-and-model-reference:actual-runtime-observation-5, whole-runtime-and-model-reference:actual-runtime-observation-6, whole-runtime-and-model-reference:actual-runtime-observation-7, whole-runtime-and-model-reference:actual-runtime-observation-8, whole-runtime-and-model-reference:actual-runtime-observation-9, whole-runtime-and-model-reference:actual-runtime-observation-10, whole-runtime-and-model-reference:actual-runtime-observation-10-reason, whole-runtime-and-model-reference:actual-runtime-observation-10-audit, whole-runtime-and-model-reference:actual-runtime-observation-11, whole-runtime-and-model-reference:actual-runtime-observation-12, whole-runtime-and-model-reference:actual-runtime-observation-13, whole-runtime-and-model-reference:actual-runtime-observation-14, whole-runtime-and-model-reference:actual-runtime-observation-15, whole-runtime-and-model-reference:actual-runtime-observation-16, whole-runtime-and-model-reference:actual-runtime-observation-17, whole-runtime-and-model-reference:actual-runtime-observation-18, whole-runtime-and-model-reference:actual-runtime-observation-19, whole-runtime-and-model-reference:actual-runtime-observation-20, whole-runtime-and-model-reference:actual-runtime-observation-21, whole-runtime-and-model-reference:actual-runtime-observation-28, whole-runtime-and-model-reference:actual-runtime-observation-29, whole-runtime-and-model-reference:received-custody-control, whole-runtime-and-model-reference:runtime-mcp-same-snapshot, whole-runtime-and-model-reference:actual-runtime-observation-5-mcp, whole-runtime-and-model-reference:actual-runtime-observation-6-mcp, whole-runtime-and-model-reference:actual-runtime-observation-7-mcp, whole-runtime-and-model-reference:actual-runtime-observation-8-mcp |
| E1.whole-runtime-and-model-reference | model-reference → whole-runtime-and-model-reference:model-reference-22, whole-runtime-and-model-reference:model-reference-23, whole-runtime-and-model-reference:model-reference-24, whole-runtime-and-model-reference:model-reference-25, whole-runtime-and-model-reference:model-reference-26, whole-runtime-and-model-reference:model-reference-27 |

검증 증거는 evidence/에 보존했다. 최종 전체 harness143개
(판매 표본·mutant13개 포함)는 PASS이며 JSON schema는 유효하다. 이 case의 실제 Gherkin
selector는 3개를 발견·시작했고 모두 NOT_IMPLEMENTED assertion으로
RED(exit1), scenario skip0이다. 실제 제품 profile은 NOT_RUN(exit2)이다.
전체 명령·version·집계는 E1/evidence/sales-preparation-summary.json에 있다.

## 2026-10-08 재검토 수정

- 의무 원행의 `current`는 행의 현재 유효성(최신 version)이며 status와
  독립이다. QC_PASS로 해소된 hold60 의무도 `current=true`·`RESOLVED`로
  남고, 남은 OPEN QC 의무는 qc-hold40의 의무 하나다. C4와 같은 의미다
  (계획 §5.3).
- 기관미확인 보유30은 규제 UNKNOWN·QC 통과·구매 수령분 버킷이고,
  규제 축만 본 구매 수령분 UNKNOWN은 70이다. 반품10의 규제 축은 고정하지
  않는다.
- 명사/동사 `linkedSummary`는 서로 같을 뿐 아니라 goals 조회의 snapshot,
  고정 시계, goals-db의 OPEN 의무 원행, 고정 수량 100/80/30/10/0 BOX와
  5 EUR에 묶인다. `linkedSummary.duties`(id·kind·ownerId·status·
  nextAction·nextCheckAt)와 `linkedSummary.quantities`(…Quantity·unit·
  invoiceDifference·invoiceDifferenceUnit)를 읽는다.
- consume-again은 첫 출고와 같은 형식(cargoPlaceId TRANSIT, 직전
  getObject의 현재 revision)이며 INSUFFICIENT_ELIGIBLE_QUANTITY와 거부
  감사1을 고정한다.
- 모든 oracle이 MCP layer를 요구하므로 `mcp` profile을 선언하고 각
  subcase의 최종 조회를 같은 snapshot의 MCP route로 다시 읽어 같은 값을
  확인한다.
- 오류 코드는 `contracts/command-response.schema.json`의
  `/response/error/code`로만 읽는다.
