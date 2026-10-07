# 구매 승인 범위와 실제 도착 기여를 분리한다

## 독립 손계산과 범위

구매100은 계획이며 재고가 아니다. 승인된 제안의 수량을120으로 바꾸면 새 hash와 revision을 승인해야 한다.
가격·품목·공급자·기한·목적지·끝점 변경도 각각 검증한다. 전달·공급자 답변·도착은 다른 사실이다. 실제 수령60+40=허용
기여100 BOX이고 초과5 BOX는 별도 대조한다. 실제 반품10과 내부 이동20은 원 구매에 더하지 않는다.
목표100을80으로 줄이는 변경은 MANAGER가 새 제안을 승인한 뒤 실행한다.

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
| T13.approval-version-binding / old-approval-revised-dispatch | changed-quantity/old-approval-rejected, changed-quantity/purchaseOrders-zero, changed-quantity/outbox-zero, changed-quantity/segments-zero, changed-quantity/denial-audit, changed-price/old-approval-rejected, changed-price/purchaseOrders-zero, changed-price/outbox-zero, changed-price/segments-zero, changed-price/denial-audit, changed-itemId/old-approval-rejected, changed-itemId/purchaseOrders-zero, changed-itemId/outbox-zero, changed-itemId/segments-zero, changed-itemId/denial-audit, changed-supplierId/old-approval-rejected, changed-supplierId/purchaseOrders-zero, changed-supplierId/outbox-zero, changed-supplierId/segments-zero, changed-supplierId/denial-audit, changed-dueAt/old-approval-rejected, changed-dueAt/purchaseOrders-zero, changed-dueAt/outbox-zero, changed-dueAt/segments-zero, changed-dueAt/denial-audit, changed-destinationId/old-approval-rejected, changed-destinationId/purchaseOrders-zero, changed-destinationId/outbox-zero, changed-destinationId/segments-zero, changed-destinationId/denial-audit, changed-endpoint/old-approval-rejected, changed-endpoint/purchaseOrders-zero, changed-endpoint/outbox-zero, changed-endpoint/segments-zero, changed-endpoint/denial-audit, decision-reject/dispatch-not-approved, decision-conditional/dispatch-not-approved, decision-expired/dispatch-not-approved, policy-rechecked/po-zero, policy-rechecked/external-zero |
| T13.approval-version-binding / unauthorized-approved-effects | decision-unauthorized/purchaseOrders-zero, decision-unauthorized/outbox-zero, decision-unauthorized/unauthorized-rejected, decision-unauthorized/unauthorized-rejected-error, decision-unauthorized/approvals-zero, decision-unauthorized/denial-audit-one |
| T13.approval-version-binding / approval-bound | changed-quantity/changed-field-value, changed-quantity/approval-binding, changed-price/changed-field-value, changed-price/approval-binding, changed-itemId/changed-field-value, changed-itemId/approval-binding, changed-supplierId/changed-field-value, changed-supplierId/approval-binding, changed-supplierId/supplier-not-item, changed-dueAt/changed-field-value, changed-dueAt/approval-binding, changed-destinationId/changed-field-value, changed-destinationId/approval-binding, changed-endpoint/changed-field-value, changed-endpoint/approval-binding, decision-reject/purchaseOrders-zero, decision-reject/outbox-zero, decision-reject/decision-kept, decision-reject/denial-audit-one, decision-conditional/purchaseOrders-zero, decision-conditional/outbox-zero, decision-conditional/decision-kept, decision-conditional/conditions-kept, decision-conditional/denial-audit-one, decision-expired/purchaseOrders-zero, decision-expired/outbox-zero, decision-expired/decision-kept, decision-expired/denial-audit-one, policy-rechecked/current-policy-rejected, policy-rechecked/new-policy-used |
| T13.delivery-commitment-arrival-separate / purchase-created-held-inventory | supplier-accept/proposal-inventory-zero, supplier-reject/proposal-inventory-zero, supplier-change/proposal-inventory-zero |
| T13.delivery-commitment-arrival-separate / transmission-created-arrival | supplier-accept/transmission-arrival-zero, supplier-accept/transmission-inventory-zero, supplier-reject/transmission-arrival-zero, supplier-reject/transmission-inventory-zero, supplier-change/transmission-arrival-zero, supplier-change/transmission-inventory-zero |
| T13.delivery-commitment-arrival-separate / supplier-acceptance-created-arrival | supplier-accept/supplier-arrival-zero, supplier-accept/supplier-inventory-zero, supplier-accept/reply-distinct, supplier-reject/supplier-arrival-zero, supplier-reject/supplier-inventory-zero, supplier-reject/reply-distinct, supplier-change/supplier-arrival-zero, supplier-change/supplier-inventory-zero, supplier-change/reply-distinct |
| T13.delivery-commitment-arrival-separate / cancel-scope | cancel-unexecuted/only40-cancel, cancel-unexecuted/unconfirmed-cancel-owner-one, cancel-unexecuted/unconfirmed-cancel-owner-owner, cancel-unexecuted/segments-preserved, cancel-unexecuted/shipments-preserved, cancel-unexecuted/receipts-preserved, cancel-unexecuted/invoices-preserved |
| T13.partial-and-excess-contributions / authorized-arrival-contribution | partial-excess-return-relocation/arrival-api100, partial-excess-return-relocation/arrival-db100 |
| T13.partial-and-excess-contributions / excess-reconciliation | partial-excess-return-relocation/held-db105, partial-excess-return-relocation/excess5 |
| T13.partial-and-excess-contributions / return-added-to-purchase | partial-excess-return-relocation/return-no-new-contribution |
| T13.partial-and-excess-contributions / relocation-added-to-purchase | partial-excess-return-relocation/relocation-no-new-contribution |
| T13.partial-and-excess-contributions / contribution-scope | partial-excess-return-relocation/plan-original-goal, partial-excess-return-relocation/plan-original-goal-version, partial-excess-return-relocation/distinct-contributions, partial-excess-return-relocation/canonical-contributions, partial-excess-return-relocation/new-goal80, partial-excess-return-relocation/new-goal-revision |
| T13.partial-and-excess-contributions / excess-owner | partial-excess-return-relocation/excess-owner-one, partial-excess-return-relocation/excess-owner-owner |

## 실행과 제한

B2는 feaca0af9673620eff9a5ac0f08a657ce14e9ccd다. 실제 제품
adapter·DB·기관·host·model 실행은 NOT_RUN이다. schema 검증과 Cucumber file
selector RED는 준비 증거이며 runtime PASS가 아니다. `SupplyAssertionTest`는 선언한
assertion과 고정 관찰 입력의 mutant를 검사하며 제품 service나 효과를 흉내 내지 않는다. 정확한 실행
명령·exit·발견 수·fixture hash는 evidence/preparation/에 기록한다. 실모델·유료 호출·외부
제출·은행 이체를 실행하지 않는다.
