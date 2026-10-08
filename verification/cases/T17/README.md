# 예약·출고·확인 인도와 사실/허가 경계의 인수 계약

B2 `feaca0af9673620eff9a5ac0f08a657ce14e9ccd`에서 작성한
Step 2 테스트 계약이다. 제품 API/DB/host 실행은 NOT_RUN이다.
fixture 설치는 입력이며 업무 효과나 승인 성공을 미리 저장하지 않는다.
모든 action·assertion은 같은 한국어 scenario에 대응한다.

| subcase | 손계산과 책임 |
|---|---|
| reserve-pick-dispatch | 100 보유 중 적격60의 예약·피킹·출고와 중복 배분을 대조한다 |
| normal-consumed-delivery | 출고30의 CONSUMED 배분을 참조한 정상 인도30은 신규 출고를 만들지 않는다 |
| late-restriction-actual-delivery | 출고30 뒤 SELL 금지에도 확인 인도20과 운송10을 보존하고 위반·회수 책임을 남긴다 |
| post-dispatch-expiry | 처분 허용 09:05 만료 전 출고30, 09:06 실제 인도20 기록 뒤 인도20·운송10 보존, 새 출고·배분0, 위반 대응 의무 OPEN(owner sales) |
| claim-falseclaim | 원천 falseClaim 보고는 주장만 접수하며 정상 인도 효과0과 접수 책임을 남긴다 |
| claim-unidentifiedscope | 원천 unidentifiedScope 보고는 주장만 접수하며 정상 인도 효과0과 접수 책임을 남긴다 |
| claim-conflictingquantity | 원천 conflictingQuantity 보고는 주장만 접수하며 정상 인도 효과0과 접수 책임을 남긴다 |
| claim-nopriordispatch | 원천 noPriorDispatch 보고는 주장만 접수하며 정상 인도 효과0과 접수 책임을 남긴다 |
| claim-scopeoverflow | 원천 scopeOverflow 보고는 주장만 접수하며 정상 인도 효과0과 접수 책임을 남긴다 |
| replacement-no-revival | A20 정지 예약을 B20으로 대체한 뒤 A 보류를 해제해도 실행 예약은20이다 |
| eligibility-unknown | 조건별 근거·FEFO·인도 끝점과 UNKNOWN 실행 경계를 확인한다 |
| eligibility-conflict | 조건별 근거·FEFO·인도 끝점과 CONFLICT 실행 경계를 확인한다 |
| eligibility-fefo-no-reason | 조건별 근거·FEFO·인도 끝점과 FEFO-no-reason 실행 경계를 확인한다 |
| eligibility-fefo-with-reason | 조건별 근거·FEFO·인도 끝점과 FEFO-with-reason 실행 경계를 확인한다 |
| eligibility-packaging-unapproved | 조건별 근거·FEFO·인도 끝점과 packaging-unapproved 실행 경계를 확인한다 |

수량은 BOX이며 송장 차이는 EUR다. API와 독립 DB의 동일 snapshot을
대조한다. 원 행은 각 requested source의 완전한 query·parameters·mapping·
snapshot artifact와 함께 반환해야 한다. 미확인/상충은 0으로 바꾸지 않는다.
거부 감사와 접수 책임은 허용하되 금지된 원장·배분·정상 이행·외부
효과는 명시한 전후 원 행으로 비교한다. 과거 인도와 현재 반품을 빼서
한 값으로 만들지 않는다. 회수25를 다시 폐기해도 distinct 실물은25다.

| oracle | named observation과 assertion |
|---|---|
| T17.reserve-pick-dispatch | held-after-reserve → reserve-pick-dispatch:held-after-reserve-1, reserve-pick-dispatch:held-after-reserve-2 |
| T17.reserve-pick-dispatch | eligible-after-reserve → reserve-pick-dispatch:eligible-after-reserve-3, reserve-pick-dispatch:eligible-after-reserve-4 |
| T17.reserve-pick-dispatch | unreserved-after-reserve → reserve-pick-dispatch:unreserved-after-reserve-5, reserve-pick-dispatch:unreserved-after-reserve-6 |
| T17.reserve-pick-dispatch | warehouse-after-dispatch → reserve-pick-dispatch:warehouse-after-dispatch-12, reserve-pick-dispatch:warehouse-after-dispatch-13 |
| T17.reserve-pick-dispatch | dispatch-allocation → reserve-pick-dispatch:dispatch-allocation-18, reserve-pick-dispatch:dispatch-allocation-19 |
| T17.reserve-pick-dispatch | in-transit-after-dispatch → reserve-pick-dispatch:in-transit-after-dispatch-14, reserve-pick-dispatch:in-transit-after-dispatch-15 |
| T17.reserve-pick-dispatch | delivered-before-observation → reserve-pick-dispatch:delivered-before-observation-16, reserve-pick-dispatch:delivered-before-observation-17 |
| T17.reserve-pick-dispatch | second-reservation-same60 → reserve-pick-dispatch:second-reservation-same60-code, reserve-pick-dispatch:second-reservation-same60-line-allocation0, reserve-pick-dispatch:second-reservation-same60-7, reserve-pick-dispatch:second-reservation-same60-8, reserve-pick-dispatch:second-reservation-same60-9, reserve-pick-dispatch:second-reservation-same60-10, reserve-pick-dispatch:second-reservation-same60-11 |
| T17.normal-consumed-delivery | delivered → normal-consumed-delivery:delivered-1, normal-consumed-delivery:delivered-2 |
| T17.normal-consumed-delivery | cargo-in-transit → normal-consumed-delivery:cargo-in-transit-3, normal-consumed-delivery:cargo-in-transit-4 |
| T17.normal-consumed-delivery | delivery-assessment → normal-consumed-delivery:delivery-assessment-5, normal-consumed-delivery:delivery-assessment-6 |
| T17.normal-consumed-delivery | allocation-still → normal-consumed-delivery:allocation-still-7, normal-consumed-delivery:allocation-still-8 |
| T17.normal-consumed-delivery | delivery-created-new-allocation → normal-consumed-delivery:delivery-created-new-allocation-9, normal-consumed-delivery:delivery-created-new-allocation-10 |
| T17.normal-consumed-delivery | delivery-created-new-warehouse-dispatch → normal-consumed-delivery:delivery-created-new-warehouse-dispatch-11 |
| T17.late-restriction-actual-delivery | actual-delivered → late-restriction-actual-delivery:actual-delivered-1, late-restriction-actual-delivery:actual-delivered-2 |
| T17.late-restriction-actual-delivery | remaining-transit → late-restriction-actual-delivery:remaining-transit-3, late-restriction-actual-delivery:remaining-transit-4 |
| T17.late-restriction-actual-delivery | new-warehouse-dispatch → late-restriction-actual-delivery:new-warehouse-dispatch-5, late-restriction-actual-delivery:new-warehouse-dispatch-6 |
| T17.late-restriction-actual-delivery | new-executable-allocation → late-restriction-actual-delivery:new-executable-allocation-7, late-restriction-actual-delivery:new-executable-allocation-8 |
| T17.late-restriction-actual-delivery | fact-not-permission → late-restriction-actual-delivery:fact-not-permission-9, late-restriction-actual-delivery:fact-not-permission-10, late-restriction-actual-delivery:fact-not-permission-11, late-restriction-actual-delivery:fact-not-permission-12 |
| T17.late-restriction-actual-delivery | late-restriction-duty → late-restriction-actual-delivery:late-restriction-duty-13, late-restriction-actual-delivery:late-restriction-duty-14, late-restriction-actual-delivery:late-restriction-duty-15, late-restriction-actual-delivery:late-restriction-duty-16, late-restriction-actual-delivery:late-restriction-duty-17, late-restriction-actual-delivery:late-restriction-duty-18, late-restriction-actual-delivery:late-restriction-duty-19, late-restriction-actual-delivery:late-restriction-duty-20, late-restriction-actual-delivery:late-restriction-duty-21, late-restriction-actual-delivery:late-restriction-duty-22, late-restriction-actual-delivery:late-restriction-duty-23, late-restriction-actual-delivery:late-restriction-duty-24, late-restriction-actual-delivery:late-restriction-duty-25, late-restriction-actual-delivery:late-restriction-duty-26 |
| T17.false-or-unmatched-delivery | claim-inventory-effects → claim-falseclaim:claim-inventory-effects-1, claim-falseclaim:claim-inventory-effects-2, claim-falseclaim:claim-inventory-effects-3, claim-falseclaim:claim-inventory-effects-4, claim-unidentifiedscope:claim-inventory-effects-1, claim-unidentifiedscope:claim-inventory-effects-2, claim-unidentifiedscope:claim-inventory-effects-3, claim-unidentifiedscope:claim-inventory-effects-4, claim-conflictingquantity:claim-inventory-effects-1, claim-conflictingquantity:claim-inventory-effects-2, claim-conflictingquantity:claim-inventory-effects-3, claim-conflictingquantity:claim-inventory-effects-4, claim-nopriordispatch:claim-inventory-effects-1, claim-nopriordispatch:claim-inventory-effects-2, claim-nopriordispatch:claim-inventory-effects-3, claim-nopriordispatch:claim-inventory-effects-4, claim-scopeoverflow:claim-inventory-effects-1, claim-scopeoverflow:claim-inventory-effects-2, claim-scopeoverflow:claim-inventory-effects-3, claim-scopeoverflow:claim-inventory-effects-4 |
| T17.false-or-unmatched-delivery | claim-order-fulfilment → claim-falseclaim:claim-order-fulfilment-5, claim-falseclaim:claim-order-fulfilment-6, claim-falseclaim:claim-order-fulfilment-7, claim-falseclaim:claim-order-fulfilment-8, claim-unidentifiedscope:claim-order-fulfilment-5, claim-unidentifiedscope:claim-order-fulfilment-6, claim-unidentifiedscope:claim-order-fulfilment-7, claim-unidentifiedscope:claim-order-fulfilment-8, claim-conflictingquantity:claim-order-fulfilment-5, claim-conflictingquantity:claim-order-fulfilment-6, claim-conflictingquantity:claim-order-fulfilment-7, claim-conflictingquantity:claim-order-fulfilment-8, claim-nopriordispatch:claim-order-fulfilment-5, claim-nopriordispatch:claim-order-fulfilment-6, claim-nopriordispatch:claim-order-fulfilment-7, claim-nopriordispatch:claim-order-fulfilment-8, claim-scopeoverflow:claim-order-fulfilment-5, claim-scopeoverflow:claim-order-fulfilment-6, claim-scopeoverflow:claim-order-fulfilment-7, claim-scopeoverflow:claim-order-fulfilment-8 |
| T17.false-or-unmatched-delivery | claim-new-dispatch → claim-falseclaim:claim-new-dispatch-9, claim-falseclaim:claim-new-dispatch-10, claim-unidentifiedscope:claim-new-dispatch-9, claim-unidentifiedscope:claim-new-dispatch-10, claim-conflictingquantity:claim-new-dispatch-9, claim-conflictingquantity:claim-new-dispatch-10, claim-nopriordispatch:claim-new-dispatch-9, claim-nopriordispatch:claim-new-dispatch-10, claim-scopeoverflow:claim-new-dispatch-9, claim-scopeoverflow:claim-new-dispatch-10 |
| T17.false-or-unmatched-delivery | claim-not-canonical → claim-falseclaim:claim-not-canonical-11, claim-falseclaim:claim-not-canonical-12, claim-falseclaim:claim-not-canonical-13, claim-falseclaim:claim-not-canonical-14, claim-unidentifiedscope:claim-not-canonical-11, claim-unidentifiedscope:claim-not-canonical-12, claim-unidentifiedscope:claim-not-canonical-13, claim-unidentifiedscope:claim-not-canonical-14, claim-conflictingquantity:claim-not-canonical-11, claim-conflictingquantity:claim-not-canonical-12, claim-conflictingquantity:claim-not-canonical-13, claim-conflictingquantity:claim-not-canonical-14, claim-nopriordispatch:claim-not-canonical-11, claim-nopriordispatch:claim-not-canonical-12, claim-nopriordispatch:claim-not-canonical-13, claim-nopriordispatch:claim-not-canonical-14, claim-scopeoverflow:claim-not-canonical-11, claim-scopeoverflow:claim-not-canonical-12, claim-scopeoverflow:claim-not-canonical-13, claim-scopeoverflow:claim-not-canonical-14 |
| T17.false-or-unmatched-delivery | claim-reconciliation → claim-falseclaim:claim-reconciliation-15, claim-falseclaim:claim-reconciliation-16, claim-falseclaim:claim-reconciliation-17, claim-falseclaim:claim-reconciliation-18, claim-falseclaim:claim-reconciliation-19, claim-falseclaim:claim-reconciliation-20, claim-falseclaim:claim-reconciliation-21, claim-unidentifiedscope:claim-reconciliation-15, claim-unidentifiedscope:claim-reconciliation-16, claim-unidentifiedscope:claim-reconciliation-17, claim-unidentifiedscope:claim-reconciliation-18, claim-unidentifiedscope:claim-reconciliation-19, claim-unidentifiedscope:claim-reconciliation-20, claim-unidentifiedscope:claim-reconciliation-21, claim-conflictingquantity:claim-reconciliation-15, claim-conflictingquantity:claim-reconciliation-16, claim-conflictingquantity:claim-reconciliation-17, claim-conflictingquantity:claim-reconciliation-18, claim-conflictingquantity:claim-reconciliation-19, claim-conflictingquantity:claim-reconciliation-20, claim-conflictingquantity:claim-reconciliation-21, claim-nopriordispatch:claim-reconciliation-15, claim-nopriordispatch:claim-reconciliation-16, claim-nopriordispatch:claim-reconciliation-17, claim-nopriordispatch:claim-reconciliation-18, claim-nopriordispatch:claim-reconciliation-19, claim-nopriordispatch:claim-reconciliation-20, claim-nopriordispatch:claim-reconciliation-21, claim-scopeoverflow:claim-reconciliation-15, claim-scopeoverflow:claim-reconciliation-16, claim-scopeoverflow:claim-reconciliation-17, claim-scopeoverflow:claim-reconciliation-18, claim-scopeoverflow:claim-reconciliation-19, claim-scopeoverflow:claim-reconciliation-20, claim-scopeoverflow:claim-reconciliation-21 |
| T17.false-or-unmatched-delivery | no-prior-dispatch → claim-nopriordispatch:no-prior-dispatch-22, claim-nopriordispatch:no-prior-dispatch-23, claim-nopriordispatch:no-prior-dispatch-24 |
| T17.allocation-replacement-no-revival | old-allocation → replacement-no-revival:old-allocation-1, replacement-no-revival:old-allocation-2 |
| T17.allocation-replacement-no-revival | new-allocation → replacement-no-revival:new-allocation-3, replacement-no-revival:new-allocation-4 |
| T17.allocation-replacement-no-revival | executable-reserved-total → replacement-no-revival:executable-reserved-total-5, replacement-no-revival:executable-reserved-total-6 |
| T17.allocation-replacement-no-revival | old-revival → replacement-no-revival:old-revival-7, replacement-no-revival:old-revival-8, replacement-no-revival:old-revival-13, replacement-no-revival:old-revival-14 |
| T17.allocation-replacement-no-revival | replacement-atomic → replacement-no-revival:replacement-atomic-9, replacement-no-revival:replacement-atomic-10, replacement-no-revival:replacement-atomic-11, replacement-no-revival:replacement-atomic-12 |
| T17.eligibility-fefo-contract | eligibility-response → eligibility-unknown:eligibility-response-1, eligibility-unknown:eligibility-response-2, eligibility-unknown:eligibility-response-3, eligibility-unknown:eligibility-response-4, eligibility-unknown:eligibility-response-5, eligibility-unknown:eligibility-response-6, eligibility-conflict:eligibility-response-1, eligibility-conflict:eligibility-response-2, eligibility-conflict:eligibility-response-3, eligibility-conflict:eligibility-response-4, eligibility-conflict:eligibility-response-5, eligibility-conflict:eligibility-response-6, eligibility-fefo-no-reason:eligibility-response-1, eligibility-fefo-no-reason:eligibility-response-2, eligibility-fefo-no-reason:eligibility-response-3, eligibility-fefo-no-reason:eligibility-response-4, eligibility-fefo-no-reason:eligibility-response-5, eligibility-fefo-no-reason:eligibility-response-6, eligibility-fefo-no-reason:eligibility-response-7, eligibility-fefo-no-reason:eligibility-response-8, eligibility-fefo-no-reason:eligibility-response-9, eligibility-fefo-no-reason:eligibility-response-10, eligibility-fefo-no-reason:eligibility-response-11, eligibility-fefo-no-reason:eligibility-response-12, eligibility-fefo-no-reason:eligibility-response-13, eligibility-fefo-with-reason:eligibility-response-1, eligibility-fefo-with-reason:eligibility-response-2, eligibility-fefo-with-reason:eligibility-response-3, eligibility-fefo-with-reason:eligibility-response-4, eligibility-fefo-with-reason:eligibility-response-5, eligibility-fefo-with-reason:eligibility-response-6, eligibility-fefo-with-reason:eligibility-response-7, eligibility-packaging-unapproved:eligibility-response-1, eligibility-packaging-unapproved:eligibility-response-2, eligibility-packaging-unapproved:eligibility-response-3, eligibility-packaging-unapproved:eligibility-response-4, eligibility-packaging-unapproved:eligibility-response-5, eligibility-packaging-unapproved:eligibility-response-6 |
| T17.eligibility-fefo-contract | unauthorized-packaging-fulfilment → eligibility-packaging-unapproved:unauthorized-packaging-fulfilment-7, eligibility-packaging-unapproved:unauthorized-packaging-fulfilment-8, eligibility-packaging-unapproved:unauthorized-packaging-fulfilment-9, eligibility-packaging-unapproved:unauthorized-packaging-fulfilment-10, eligibility-packaging-unapproved:unauthorized-packaging-fulfilment-11, eligibility-packaging-unapproved:unauthorized-packaging-fulfilment-12, eligibility-packaging-unapproved:unauthorized-packaging-fulfilment-13 |

검증 증거는 evidence/에 보존했다. 최종 전체 harness143개
(판매 표본·mutant13개 포함)는 PASS이며 JSON schema는 유효하다. 이 case의 실제 Gherkin
selector는 14개를 발견·시작했고 모두 NOT_IMPLEMENTED assertion으로
RED(exit1), scenario skip0이다. 실제 제품 profile은 NOT_RUN(exit2)이다.
전체 명령·version·집계는 E1/evidence/sales-preparation-summary.json에 있다.

## Step2 adversarial review 보완

기존 최종 상태만으로 중간 명령의 실행을 인정하지 않도록 보완했다.
검증은 고정 관찰 표본을 사용하는 AssertionEngine SELFTEST와 실제
제품 API/DB 실행을 구별한다. 제품 인수는 계속 NOT_RUN이다.

replacement-no-revival의 기존14개 assertion을 유지하고9개를
추가했다. 대체 전 SUSPENDED20BOX와 대체 후 A의 REPLACED/B의
EXECUTABLE20BOX를 실제 대체 command·transaction과 연결한다.
QC 제한은 대체 직후 ACTIVE이며 releaseHold의 APPLIED와 동일
restriction ID, 실제 A20의 RELEASED를 모두 요구한다.
그 뒤 releaseAllocation도 B20의 RELEASED와 command·transaction,
실행 가능 예약0/행0을 확인한다. 각 단계의 A 예약 부활은 계속
금지한다. 기존 catalog oracle와 named observation은 그대로다.

추가 assertion ID는 hold-before-replace-15,
replacement-command-effects-16, release-hold-applied-17,
release-hold-target-18, hold-active-before-release-19,
hold-released-20, released-replacement-allocation-21,
released-executable-total-22, released-executable-rows-23이다.
앞2개는 replacement-atomic, 나머지는 old-revival 관찰을 보강한다.

보완 검증은 JDK21 javac와 JUnit Platform/Jupiter6.1.2 직접 launcher로
실행했다. OutcomeEffectAssertionSelfTest의7개 method가 발견·시작·
성공했고 skip/FAIL0이다. 전체 정상 표본과42개 거부/no-op·출처·
수량·transaction·책임 mutant를 검증했다. Maven queue를 쓰지 않은
독립 SELFTEST이며 통합 harness check는 coordinator가 수행한다.
정확한 argv·버전·source hash·log는
C4/evidence/review-outcomes/selftest-commands.json에 보존했다.

## Step 2 재검토 2차 보완

- reserve-pick-dispatch의 second-reserve는 action SELL을 보내고
  REJECTED에 더해 INSUFFICIENT_ELIGIBLE_QUANTITY와 두 번째 주문
  line 배분0을 확인한다(이전에는 outcome만 봤다).
- replacement-no-revival의 release-hold expectedRevision은 대상 제한을
  만든 hold 응답에서 읽는다. 이전 판은 replace 응답의 revision을
  썼는데 그것은 다른 aggregate의 revision이다.
- post-dispatch-expiry는 계획 §13.1 D17 "출고 뒤 recall/만료에도 실제20
  인도와 운송10 보존"의 만료 쪽이다. late-restriction-actual-delivery는
  recall 보류 쪽이다. 같은 catalog oracle
  T17.late-restriction-actual-delivery의 actual-delivered,
  remaining-transit, new-warehouse-dispatch, new-executable-allocation,
  fact-not-permission, late-restriction-duty에 연결했다. 회수 의무는
  만료 쪽에 없으므로 RECALL_INVESTIGATION assertion은 넣지 않았다.
