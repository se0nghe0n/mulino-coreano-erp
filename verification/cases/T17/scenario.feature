# language: ko
@T17 @D17 @sit @uat
기능: 예약·출고·확인 인도와 사실/허가 경계

  시나리오: 100 보유 중 적격60의 예약·피킹·출고와 중복 배분을 대조한다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "reserve-pick-dispatch"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "qc" 역할이 "hold-B40" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "observer" 역할이 "reserved" 행동을 수행한다
    만일 "시스템" 역할이 "reserved-db" 행동을 수행한다
    만일 "sales" 역할이 "second-order" 행동을 수행한다
    만일 "observer" 역할이 "before-second" 행동을 수행한다
    만일 "시스템" 역할이 "before-second-db" 행동을 수행한다
    만일 "sales" 역할이 "second-reserve" 행동을 수행한다
    만일 "observer" 역할이 "after-second" 행동을 수행한다
    만일 "시스템" 역할이 "after-second-db" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "observer" 역할이 "dispatched" 행동을 수행한다
    만일 "시스템" 역할이 "dispatched-db" 행동을 수행한다
    그러면 "held-after-reserve-1" assertion으로 "held-after-reserve"를 확인한다
    그러면 "held-after-reserve-2" assertion으로 "held-after-reserve"를 확인한다
    그러면 "eligible-after-reserve-3" assertion으로 "eligible-after-reserve"를 확인한다
    그러면 "eligible-after-reserve-4" assertion으로 "eligible-after-reserve"를 확인한다
    그러면 "unreserved-after-reserve-5" assertion으로 "unreserved-after-reserve"를 확인한다
    그러면 "unreserved-after-reserve-6" assertion으로 "unreserved-after-reserve"를 확인한다
    그러면 "second-reservation-same60-7" assertion으로 "second-reservation-same60"를 확인한다
    그러면 "second-reservation-same60-8" assertion으로 "second-reservation-same60"를 확인한다
    그러면 "second-reservation-same60-9" assertion으로 "second-reservation-same60"를 확인한다
    그러면 "second-reservation-same60-10" assertion으로 "second-reservation-same60"를 확인한다
    그러면 "second-reservation-same60-11" assertion으로 "second-reservation-same60"를 확인한다
    그러면 "second-reservation-same60-code" assertion으로 "같은 A60에 SELL 예약60이 이미 있어 미예약 적격은0 BOX다. 두 번째 주문의 예약60은 REJECTED·INSUFFICIENT_ELIGIBLE_QUANTITY다(계획 §3.4·§4.2)."를 확인한다
    그러면 "second-reservation-same60-line-allocation0" assertion으로 "거부 뒤 두 번째 주문 line을 가리키는 배분 원행은0개다. 같은 실물60을 두 주문에 중복 배분하지 않는다."를 확인한다
    그러면 "warehouse-after-dispatch-12" assertion으로 "warehouse-after-dispatch"를 확인한다
    그러면 "warehouse-after-dispatch-13" assertion으로 "warehouse-after-dispatch"를 확인한다
    그러면 "in-transit-after-dispatch-14" assertion으로 "in-transit-after-dispatch"를 확인한다
    그러면 "in-transit-after-dispatch-15" assertion으로 "in-transit-after-dispatch"를 확인한다
    그러면 "delivered-before-observation-16" assertion으로 "delivered-before-observation"를 확인한다
    그러면 "delivered-before-observation-17" assertion으로 "delivered-before-observation"를 확인한다
    그러면 "dispatch-allocation-18" assertion으로 "dispatch-allocation"를 확인한다
    그러면 "dispatch-allocation-19" assertion으로 "dispatch-allocation"를 확인한다

  시나리오: 출고30의 CONSUMED 배분을 참조한 정상 인도30은 신규 출고를 만들지 않는다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "normal-consumed-delivery"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "observer" 역할이 "before-delivery" 행동을 수행한다
    만일 "시스템" 역할이 "before-delivery-db" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "observer" 역할이 "after-delivery" 행동을 수행한다
    만일 "시스템" 역할이 "after-delivery-db" 행동을 수행한다
    그러면 "delivered-1" assertion으로 "delivered"를 확인한다
    그러면 "delivered-2" assertion으로 "delivered"를 확인한다
    그러면 "cargo-in-transit-3" assertion으로 "cargo-in-transit"를 확인한다
    그러면 "cargo-in-transit-4" assertion으로 "cargo-in-transit"를 확인한다
    그러면 "delivery-assessment-5" assertion으로 "delivery-assessment"를 확인한다
    그러면 "delivery-assessment-6" assertion으로 "delivery-assessment"를 확인한다
    그러면 "allocation-still-7" assertion으로 "allocation-still"를 확인한다
    그러면 "allocation-still-8" assertion으로 "allocation-still"를 확인한다
    그러면 "delivery-created-new-allocation-9" assertion으로 "delivery-created-new-allocation"를 확인한다
    그러면 "delivery-created-new-allocation-10" assertion으로 "delivery-created-new-allocation"를 확인한다
    그러면 "delivery-created-new-warehouse-dispatch-11" assertion으로 "delivery-created-new-warehouse-dispatch"를 확인한다

  시나리오: 출고30 뒤 SELL 금지에도 확인 인도20과 운송10을 보존하고 위반·회수 책임을 남긴다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "late-restriction-actual-delivery"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "admin" 역할이 "late-hold" 행동을 수행한다
    만일 "observer" 역할이 "before-delivery" 행동을 수행한다
    만일 "시스템" 역할이 "before-delivery-db" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "observer" 역할이 "after-delivery" 행동을 수행한다
    만일 "시스템" 역할이 "after-delivery-db" 행동을 수행한다
    그러면 "actual-delivered-1" assertion으로 "actual-delivered"를 확인한다
    그러면 "actual-delivered-2" assertion으로 "actual-delivered"를 확인한다
    그러면 "remaining-transit-3" assertion으로 "remaining-transit"를 확인한다
    그러면 "remaining-transit-4" assertion으로 "remaining-transit"를 확인한다
    그러면 "new-warehouse-dispatch-5" assertion으로 "new-warehouse-dispatch"를 확인한다
    그러면 "new-warehouse-dispatch-6" assertion으로 "new-warehouse-dispatch"를 확인한다
    그러면 "new-executable-allocation-7" assertion으로 "new-executable-allocation"를 확인한다
    그러면 "new-executable-allocation-8" assertion으로 "new-executable-allocation"를 확인한다
    그러면 "fact-not-permission-9" assertion으로 "fact-not-permission"를 확인한다
    그러면 "fact-not-permission-10" assertion으로 "fact-not-permission"를 확인한다
    그러면 "fact-not-permission-11" assertion으로 "fact-not-permission"를 확인한다
    그러면 "fact-not-permission-12" assertion으로 "fact-not-permission"를 확인한다
    그러면 "late-restriction-duty-13" assertion으로 "late-restriction-duty"를 확인한다
    그러면 "late-restriction-duty-14" assertion으로 "late-restriction-duty"를 확인한다
    그러면 "late-restriction-duty-15" assertion으로 "late-restriction-duty"를 확인한다
    그러면 "late-restriction-duty-16" assertion으로 "late-restriction-duty"를 확인한다
    그러면 "late-restriction-duty-17" assertion으로 "late-restriction-duty"를 확인한다
    그러면 "late-restriction-duty-18" assertion으로 "late-restriction-duty"를 확인한다
    그러면 "late-restriction-duty-19" assertion으로 "late-restriction-duty"를 확인한다
    그러면 "late-restriction-duty-20" assertion으로 "late-restriction-duty"를 확인한다
    그러면 "late-restriction-duty-21" assertion으로 "late-restriction-duty"를 확인한다
    그러면 "late-restriction-duty-22" assertion으로 "late-restriction-duty"를 확인한다
    그러면 "late-restriction-duty-23" assertion으로 "late-restriction-duty"를 확인한다
    그러면 "late-restriction-duty-24" assertion으로 "late-restriction-duty"를 확인한다
    그러면 "late-restriction-duty-25" assertion으로 "late-restriction-duty"를 확인한다
    그러면 "late-restriction-duty-26" assertion으로 "late-restriction-duty"를 확인한다

  시나리오: 출고30 뒤 처분 허용이 만료돼도 실제 인도20과 운송10을 보존하고 위반 대응 책임을 남긴다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "post-dispatch-expiry"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "시스템" 역할이 "expiry-advance" 행동을 수행한다
    만일 "observer" 역할이 "before-delivery" 행동을 수행한다
    만일 "시스템" 역할이 "before-delivery-db" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "observer" 역할이 "after-delivery" 행동을 수행한다
    만일 "시스템" 역할이 "after-delivery-db" 행동을 수행한다
    그러면 "actual-delivered-1" assertion으로 "처분 허용 만료 뒤에도 고객이 실제로 받은 인도20 BOX를 사실로 보존한다. API와 canonical 인도 원행이 같은 20 BOX다."를 확인한다
    그러면 "actual-delivered-2" assertion으로 "처분 허용 만료 뒤에도 고객이 실제로 받은 인도20 BOX를 사실로 보존한다. API와 canonical 인도 원행이 같은 20 BOX다."를 확인한다
    그러면 "remaining-transit-3" assertion으로 "출고30 중 인도되지 않은 10 BOX는 TRANSIT에 그대로 남는다(30−20). 만료를 이유로 운송 물량을 지우거나 창고로 되돌리지 않는다."를 확인한다
    그러면 "remaining-transit-4" assertion으로 "출고30 중 인도되지 않은 10 BOX는 TRANSIT에 그대로 남는다(30−20). 만료를 이유로 운송 물량을 지우거나 창고로 되돌리지 않는다."를 확인한다
    그러면 "new-warehouse-dispatch-5" assertion으로 "인도 기록은 새 창고 출고를 만들지 않는다. 출고 수량 delta0 BOX, DISPATCH 원행 전후 동일."를 확인한다
    그러면 "new-warehouse-dispatch-6" assertion으로 "인도 기록은 새 창고 출고를 만들지 않는다. 출고 수량 delta0 BOX, DISPATCH 원행 전후 동일."를 확인한다
    그러면 "new-executable-allocation-7" assertion으로 "인도 기록은 새 실행 배분을 만들지 않는다. 배분 원행과 API effect ledger가 전후 같다."를 확인한다
    그러면 "new-executable-allocation-8" assertion으로 "인도 기록은 새 실행 배분을 만들지 않는다. 배분 원행과 API effect ledger가 전후 같다."를 확인한다
    그러면 "fact-not-permission-9" assertion으로 "만료 뒤 인도는 확정 사실이지만 정상 이행 허가가 아니다. 인도 원행은 canonical이고 인도 목표 판정은 UNSATISFIED다."를 확인한다
    그러면 "fact-not-permission-10" assertion으로 "만료 뒤 인도는 확정 사실이지만 정상 이행 허가가 아니다. 인도 원행은 canonical이고 인도 목표 판정은 UNSATISFIED다."를 확인한다
    그러면 "fact-not-permission-12" assertion으로 "만료 뒤 인도는 확정 사실이지만 정상 이행 허가가 아니다. 인도 원행은 canonical이고 인도 목표 판정은 UNSATISFIED다."를 확인한다
    그러면 "expiry-violation-duty-13" assertion으로 "만료 뒤 인도의 위반 대응 의무 VIOLATION_RESPONSE가 현재 하나 OPEN이며 owner sales, nextAction 인도 부족과 제한 대응을 확인한다, nextCheckAt 2026-10-07T10:00:00Z다. API와 DB가 같다."를 확인한다
    그러면 "expiry-violation-duty-14" assertion으로 "만료 뒤 인도의 위반 대응 의무 VIOLATION_RESPONSE가 현재 하나 OPEN이며 owner sales, nextAction 인도 부족과 제한 대응을 확인한다, nextCheckAt 2026-10-07T10:00:00Z다. API와 DB가 같다."를 확인한다
    그러면 "expiry-violation-duty-15" assertion으로 "만료 뒤 인도의 위반 대응 의무 VIOLATION_RESPONSE가 현재 하나 OPEN이며 owner sales, nextAction 인도 부족과 제한 대응을 확인한다, nextCheckAt 2026-10-07T10:00:00Z다. API와 DB가 같다."를 확인한다
    그러면 "expiry-violation-duty-16" assertion으로 "만료 뒤 인도의 위반 대응 의무 VIOLATION_RESPONSE가 현재 하나 OPEN이며 owner sales, nextAction 인도 부족과 제한 대응을 확인한다, nextCheckAt 2026-10-07T10:00:00Z다. API와 DB가 같다."를 확인한다
    그러면 "expiry-violation-duty-17" assertion으로 "만료 뒤 인도의 위반 대응 의무 VIOLATION_RESPONSE가 현재 하나 OPEN이며 owner sales, nextAction 인도 부족과 제한 대응을 확인한다, nextCheckAt 2026-10-07T10:00:00Z다. API와 DB가 같다."를 확인한다
    그러면 "expiry-violation-duty-18" assertion으로 "만료 뒤 인도의 위반 대응 의무 VIOLATION_RESPONSE가 현재 하나 OPEN이며 owner sales, nextAction 인도 부족과 제한 대응을 확인한다, nextCheckAt 2026-10-07T10:00:00Z다. API와 DB가 같다."를 확인한다
    그러면 "expiry-violation-duty-19" assertion으로 "만료 뒤 인도의 위반 대응 의무 VIOLATION_RESPONSE가 현재 하나 OPEN이며 owner sales, nextAction 인도 부족과 제한 대응을 확인한다, nextCheckAt 2026-10-07T10:00:00Z다. API와 DB가 같다."를 확인한다

  시나리오: 원천 falseClaim 보고는 주장만 접수하며 정상 인도 효과0과 접수 책임을 남긴다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "claim-falseclaim"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "observer" 역할이 "before-claim" 행동을 수행한다
    만일 "시스템" 역할이 "before-claim-db" 행동을 수행한다
    만일 "receiver" 역할이 "claim" 행동을 수행한다
    만일 "observer" 역할이 "after-claim" 행동을 수행한다
    만일 "시스템" 역할이 "after-claim-db" 행동을 수행한다
    그러면 "claim-inventory-effects-1" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-2" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-3" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-4" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-order-fulfilment-5" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-6" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-7" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-8" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-new-dispatch-9" assertion으로 "claim-new-dispatch"를 확인한다
    그러면 "claim-new-dispatch-10" assertion으로 "claim-new-dispatch"를 확인한다
    그러면 "claim-not-canonical-11" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-12" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-13" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-14" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-reconciliation-15" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-16" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-17" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-18" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-19" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-20" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-21" assertion으로 "claim-reconciliation"를 확인한다

  시나리오: 원천 unidentifiedScope 보고는 주장만 접수하며 정상 인도 효과0과 접수 책임을 남긴다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "claim-unidentifiedscope"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "observer" 역할이 "before-claim" 행동을 수행한다
    만일 "시스템" 역할이 "before-claim-db" 행동을 수행한다
    만일 "receiver" 역할이 "claim" 행동을 수행한다
    만일 "observer" 역할이 "after-claim" 행동을 수행한다
    만일 "시스템" 역할이 "after-claim-db" 행동을 수행한다
    그러면 "claim-inventory-effects-1" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-2" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-3" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-4" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-order-fulfilment-5" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-6" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-7" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-8" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-new-dispatch-9" assertion으로 "claim-new-dispatch"를 확인한다
    그러면 "claim-new-dispatch-10" assertion으로 "claim-new-dispatch"를 확인한다
    그러면 "claim-not-canonical-11" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-12" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-13" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-14" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-reconciliation-15" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-16" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-17" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-18" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-19" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-20" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-21" assertion으로 "claim-reconciliation"를 확인한다

  시나리오: 원천 conflictingQuantity 보고는 주장만 접수하며 정상 인도 효과0과 접수 책임을 남긴다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "claim-conflictingquantity"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "observer" 역할이 "before-claim" 행동을 수행한다
    만일 "시스템" 역할이 "before-claim-db" 행동을 수행한다
    만일 "receiver" 역할이 "claim" 행동을 수행한다
    만일 "observer" 역할이 "after-claim" 행동을 수행한다
    만일 "시스템" 역할이 "after-claim-db" 행동을 수행한다
    그러면 "claim-inventory-effects-1" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-2" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-3" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-4" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-order-fulfilment-5" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-6" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-7" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-8" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-new-dispatch-9" assertion으로 "claim-new-dispatch"를 확인한다
    그러면 "claim-new-dispatch-10" assertion으로 "claim-new-dispatch"를 확인한다
    그러면 "claim-not-canonical-11" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-12" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-13" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-14" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-reconciliation-15" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-16" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-17" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-18" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-19" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-20" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-21" assertion으로 "claim-reconciliation"를 확인한다

  시나리오: 원천 noPriorDispatch 보고는 주장만 접수하며 정상 인도 효과0과 접수 책임을 남긴다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "claim-nopriordispatch"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "observer" 역할이 "before-claim" 행동을 수행한다
    만일 "시스템" 역할이 "before-claim-db" 행동을 수행한다
    만일 "receiver" 역할이 "claim" 행동을 수행한다
    만일 "observer" 역할이 "after-claim" 행동을 수행한다
    만일 "시스템" 역할이 "after-claim-db" 행동을 수행한다
    그러면 "claim-inventory-effects-1" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-2" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-3" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-4" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-order-fulfilment-5" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-6" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-7" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-8" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-new-dispatch-9" assertion으로 "claim-new-dispatch"를 확인한다
    그러면 "claim-new-dispatch-10" assertion으로 "claim-new-dispatch"를 확인한다
    그러면 "claim-not-canonical-11" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-12" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-13" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-14" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-reconciliation-15" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-16" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-17" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-18" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-19" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-20" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-21" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "no-prior-dispatch-22" assertion으로 "no-prior-dispatch"를 확인한다
    그러면 "no-prior-dispatch-23" assertion으로 "no-prior-dispatch"를 확인한다
    그러면 "no-prior-dispatch-24" assertion으로 "no-prior-dispatch"를 확인한다

  시나리오: 원천 scopeOverflow 보고는 주장만 접수하며 정상 인도 효과0과 접수 책임을 남긴다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "claim-scopeoverflow"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "observer" 역할이 "before-claim" 행동을 수행한다
    만일 "시스템" 역할이 "before-claim-db" 행동을 수행한다
    만일 "receiver" 역할이 "claim" 행동을 수행한다
    만일 "observer" 역할이 "after-claim" 행동을 수행한다
    만일 "시스템" 역할이 "after-claim-db" 행동을 수행한다
    그러면 "claim-inventory-effects-1" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-2" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-3" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-inventory-effects-4" assertion으로 "claim-inventory-effects"를 확인한다
    그러면 "claim-order-fulfilment-5" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-6" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-7" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-order-fulfilment-8" assertion으로 "claim-order-fulfilment"를 확인한다
    그러면 "claim-new-dispatch-9" assertion으로 "claim-new-dispatch"를 확인한다
    그러면 "claim-new-dispatch-10" assertion으로 "claim-new-dispatch"를 확인한다
    그러면 "claim-not-canonical-11" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-12" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-13" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-not-canonical-14" assertion으로 "claim-not-canonical"를 확인한다
    그러면 "claim-reconciliation-15" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-16" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-17" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-18" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-19" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-20" assertion으로 "claim-reconciliation"를 확인한다
    그러면 "claim-reconciliation-21" assertion으로 "claim-reconciliation"를 확인한다

  시나리오: A20 정지 예약을 B20으로 대체한 뒤 A 보류를 해제해도 실행 예약은20이다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "replacement-no-revival"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "qc" 역할이 "hold" 행동을 수행한다
    만일 "observer" 역할이 "suspended" 행동을 수행한다
    만일 "시스템" 역할이 "suspended-db" 행동을 수행한다
    만일 "sales" 역할이 "replace" 행동을 수행한다
    만일 "observer" 역할이 "replaced" 행동을 수행한다
    만일 "시스템" 역할이 "replaced-db" 행동을 수행한다
    만일 "qc" 역할이 "release-hold" 행동을 수행한다
    만일 "observer" 역할이 "released" 행동을 수행한다
    만일 "시스템" 역할이 "released-db" 행동을 수행한다
    만일 "sales" 역할이 "allocation-release" 행동을 수행한다
    만일 "observer" 역할이 "allocation-released" 행동을 수행한다
    만일 "시스템" 역할이 "allocation-released-db" 행동을 수행한다
    그러면 "old-allocation-1" assertion으로 "old-allocation"를 확인한다
    그러면 "old-allocation-2" assertion으로 "old-allocation"를 확인한다
    그러면 "new-allocation-3" assertion으로 "new-allocation"를 확인한다
    그러면 "new-allocation-4" assertion으로 "new-allocation"를 확인한다
    그러면 "executable-reserved-total-5" assertion으로 "executable-reserved-total"를 확인한다
    그러면 "executable-reserved-total-6" assertion으로 "executable-reserved-total"를 확인한다
    그러면 "old-revival-7" assertion으로 "old-revival"를 확인한다
    그러면 "old-revival-8" assertion으로 "old-revival"를 확인한다
    그러면 "replacement-atomic-9" assertion으로 "replacement-atomic"를 확인한다
    그러면 "replacement-atomic-10" assertion으로 "replacement-atomic"를 확인한다
    그러면 "replacement-atomic-11" assertion으로 "replacement-atomic"를 확인한다
    그러면 "replacement-atomic-12" assertion으로 "replacement-atomic"를 확인한다
    그러면 "old-revival-13" assertion으로 "old-revival"를 확인한다
    그러면 "old-revival-14" assertion으로 "old-revival"를 확인한다
    그러면 "hold-before-replace-15" assertion으로 "replacement-atomic"를 확인한다
    그러면 "replacement-command-effects-16" assertion으로 "replacement-atomic"를 확인한다
    그러면 "release-hold-applied-17" assertion으로 "old-revival"를 확인한다
    그러면 "release-hold-target-18" assertion으로 "old-revival"를 확인한다
    그러면 "hold-active-before-release-19" assertion으로 "old-revival"를 확인한다
    그러면 "hold-released-20" assertion으로 "old-revival"를 확인한다
    그러면 "released-replacement-allocation-21" assertion으로 "old-revival"를 확인한다
    그러면 "released-executable-total-22" assertion으로 "old-revival"를 확인한다
    그러면 "released-executable-rows-23" assertion으로 "old-revival"를 확인한다

  시나리오: 조건별 근거·FEFO·인도 끝점과 UNKNOWN 실행 경계를 확인한다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "eligibility-unknown"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "qc" 역할이 "early-hold" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "observer" 역할이 "eligibility" 행동을 수행한다
    만일 "observer" 역할이 "before-reserve" 행동을 수행한다
    만일 "시스템" 역할이 "before-reserve-db" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "observer" 역할이 "after-reserve" 행동을 수행한다
    만일 "시스템" 역할이 "after-reserve-db" 행동을 수행한다
    그러면 "eligibility-response-1" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-2" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-3" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-4" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-5" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-6" assertion으로 "eligibility-response"를 확인한다

  시나리오: 조건별 근거·FEFO·인도 끝점과 CONFLICT 실행 경계를 확인한다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "eligibility-conflict"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "qc" 역할이 "early-hold" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "observer" 역할이 "eligibility" 행동을 수행한다
    만일 "observer" 역할이 "before-reserve" 행동을 수행한다
    만일 "시스템" 역할이 "before-reserve-db" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "observer" 역할이 "after-reserve" 행동을 수행한다
    만일 "시스템" 역할이 "after-reserve-db" 행동을 수행한다
    그러면 "eligibility-response-1" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-2" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-3" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-4" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-5" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-6" assertion으로 "eligibility-response"를 확인한다

  시나리오: 조건별 근거·FEFO·인도 끝점과 FEFO-no-reason 실행 경계를 확인한다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "eligibility-fefo-no-reason"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "qc" 역할이 "early-hold" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "observer" 역할이 "eligibility" 행동을 수행한다
    만일 "observer" 역할이 "before-reserve" 행동을 수행한다
    만일 "시스템" 역할이 "before-reserve-db" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "observer" 역할이 "after-reserve" 행동을 수행한다
    만일 "시스템" 역할이 "after-reserve-db" 행동을 수행한다
    그러면 "eligibility-response-1" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-2" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-3" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-4" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-5" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-6" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-7" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-8" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-9" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-10" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-11" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-12" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-13" assertion으로 "eligibility-response"를 확인한다

  시나리오: 조건별 근거·FEFO·인도 끝점과 FEFO-with-reason 실행 경계를 확인한다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "eligibility-fefo-with-reason"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "qc" 역할이 "early-hold" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "observer" 역할이 "eligibility" 행동을 수행한다
    만일 "observer" 역할이 "before-reserve" 행동을 수행한다
    만일 "시스템" 역할이 "before-reserve-db" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "observer" 역할이 "after-reserve" 행동을 수행한다
    만일 "시스템" 역할이 "after-reserve-db" 행동을 수행한다
    그러면 "eligibility-response-1" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-2" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-3" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-4" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-5" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-6" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-7" assertion으로 "eligibility-response"를 확인한다

  시나리오: 조건별 근거·FEFO·인도 끝점과 packaging-unapproved 실행 경계를 확인한다
    먼저 사례 파일 "verification/cases/T17/case.json"의 "eligibility-packaging-unapproved"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "qc" 역할이 "early-hold" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "observer" 역할이 "eligibility" 행동을 수행한다
    만일 "observer" 역할이 "before-reserve" 행동을 수행한다
    만일 "시스템" 역할이 "before-reserve-db" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "observer" 역할이 "after-reserve" 행동을 수행한다
    만일 "시스템" 역할이 "after-reserve-db" 행동을 수행한다
    그러면 "eligibility-response-1" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-2" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-3" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-4" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-5" assertion으로 "eligibility-response"를 확인한다
    그러면 "eligibility-response-6" assertion으로 "eligibility-response"를 확인한다
    그러면 "unauthorized-packaging-fulfilment-7" assertion으로 "unauthorized-packaging-fulfilment"를 확인한다
    그러면 "unauthorized-packaging-fulfilment-8" assertion으로 "unauthorized-packaging-fulfilment"를 확인한다
    그러면 "unauthorized-packaging-fulfilment-9" assertion으로 "unauthorized-packaging-fulfilment"를 확인한다
    그러면 "unauthorized-packaging-fulfilment-10" assertion으로 "unauthorized-packaging-fulfilment"를 확인한다
    그러면 "unauthorized-packaging-fulfilment-11" assertion으로 "unauthorized-packaging-fulfilment"를 확인한다
    그러면 "unauthorized-packaging-fulfilment-12" assertion으로 "unauthorized-packaging-fulfilment"를 확인한다
    그러면 "unauthorized-packaging-fulfilment-13" assertion으로 "unauthorized-packaging-fulfilment"를 확인한다
