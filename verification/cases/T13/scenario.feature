# language: ko
@T13 @D13 @sit @uat
기능: 구매 승인 범위와 실제 도착 기여를 분리한다

  시나리오: 승인 뒤 quantity 변경은 원승인으로 전달되지 않는다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "changed-quantity"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "procurement" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "procurement" 역할이 "revision" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "procurement" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "changed-field-value" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "old-approval-rejected" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "purchaseOrders-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "outbox-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "segments-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "approval-binding" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "denial-audit" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 승인 뒤 price 변경은 원승인으로 전달되지 않는다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "changed-price"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "procurement" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "procurement" 역할이 "revision" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "procurement" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "changed-field-value" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "old-approval-rejected" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "purchaseOrders-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "outbox-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "segments-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "approval-binding" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "denial-audit" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 승인 뒤 itemId 변경은 원승인으로 전달되지 않는다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "changed-itemId"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "procurement" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "procurement" 역할이 "revision" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "procurement" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "changed-field-value" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "old-approval-rejected" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "purchaseOrders-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "outbox-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "segments-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "approval-binding" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "denial-audit" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 승인 뒤 supplierId 변경은 원승인으로 전달되지 않는다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "changed-supplierId"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "procurement" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "procurement" 역할이 "revision" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "procurement" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "changed-field-value" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "old-approval-rejected" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "purchaseOrders-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "outbox-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "segments-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "approval-binding" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "denial-audit" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "supplier-not-item" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 승인 뒤 dueAt 변경은 원승인으로 전달되지 않는다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "changed-dueAt"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "procurement" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "procurement" 역할이 "revision" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "procurement" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "changed-field-value" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "old-approval-rejected" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "purchaseOrders-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "outbox-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "segments-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "approval-binding" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "denial-audit" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 승인 뒤 destinationId 변경은 원승인으로 전달되지 않는다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "changed-destinationId"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "procurement" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "procurement" 역할이 "revision" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "procurement" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "changed-field-value" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "old-approval-rejected" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "purchaseOrders-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "outbox-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "segments-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "approval-binding" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "denial-audit" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 승인 뒤 endpoint 변경은 원승인으로 전달되지 않는다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "changed-endpoint"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "procurement" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "procurement" 역할이 "revision" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "procurement" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "changed-field-value" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "old-approval-rejected" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "purchaseOrders-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "outbox-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "segments-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "approval-binding" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "denial-audit" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 구매 결정 REJECT는 범위와 유효성을 보존한다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "decision-reject"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "procurement" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "procurement" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "purchaseOrders-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "outbox-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "dispatch-not-approved" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "decision-kept" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "denial-audit-one" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 구매 결정 CONDITIONAL는 범위와 유효성을 보존한다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "decision-conditional"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "procurement" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "procurement" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "purchaseOrders-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "outbox-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "dispatch-not-approved" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "decision-kept" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "conditions-kept" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "denial-audit-one" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 구매 결정 UNAUTHORIZED는 범위와 유효성을 보존한다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "decision-unauthorized"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "procurement" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "unauthorized" 역할이 "approval" 행동을 수행한다
    만일 "procurement" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "purchaseOrders-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "outbox-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "unauthorized-rejected" assertion으로 "고정 oracle unauthorized-approved-effects의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "unauthorized-rejected-error" assertion으로 "고정 oracle unauthorized-approved-effects의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "approvals-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "denial-audit-one" assertion으로 "고정 oracle unauthorized-approved-effects의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 구매 결정 EXPIRED는 범위와 유효성을 보존한다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "decision-expired"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "procurement" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "시스템" 역할이 "expired-clock" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "procurement" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "purchaseOrders-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "outbox-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "dispatch-not-approved" assertion으로 "고정 oracle old-approval-revised-dispatch의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "decision-kept" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "denial-audit-one" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 승인 뒤 현재 policy 변경을 재검증한다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "policy-rechecked"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "config" 역할이 "policy-draft" 행동을 수행한다
    만일 "config" 역할이 "policy-approved" 행동을 수행한다
    만일 "config" 역할이 "policy-active" 행동을 수행한다
    만일 "procurement" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "procurement" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "current-policy-rejected" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "new-policy-used" assertion으로 "고정 oracle approval-bound의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "po-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "external-zero" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다

  시나리오: 발주 전달과 공급자 ACCEPT는 도착을 만들지 않는다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "supplier-accept"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "procurement" 역할이 "proposal-view" 행동을 수행한다
    만일 "시스템" 역할이 "proposal-db" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "시스템" 역할이 "delivery-ack" 행동을 수행한다
    만일 "source" 역할이 "transmission" 행동을 수행한다
    만일 "procurement" 역할이 "transmission-view" 행동을 수행한다
    만일 "시스템" 역할이 "transmission-db" 행동을 수행한다
    만일 "source" 역할이 "supplier-reply" 행동을 수행한다
    만일 "procurement" 역할이 "reply-view" 행동을 수행한다
    만일 "시스템" 역할이 "reply-db" 행동을 수행한다
    그러면 "proposal-inventory-zero" assertion으로 "고정 oracle purchase-created-held-inventory의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "transmission-arrival-zero" assertion으로 "고정 oracle transmission-created-arrival의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "transmission-inventory-zero" assertion으로 "고정 oracle transmission-created-arrival의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "supplier-arrival-zero" assertion으로 "고정 oracle supplier-acceptance-created-arrival의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "supplier-inventory-zero" assertion으로 "고정 oracle supplier-acceptance-created-arrival의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "reply-distinct" assertion으로 "고정 oracle supplier-acceptance-created-arrival의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 발주 전달과 공급자 REJECT는 도착을 만들지 않는다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "supplier-reject"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "procurement" 역할이 "proposal-view" 행동을 수행한다
    만일 "시스템" 역할이 "proposal-db" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "시스템" 역할이 "delivery-ack" 행동을 수행한다
    만일 "source" 역할이 "transmission" 행동을 수행한다
    만일 "procurement" 역할이 "transmission-view" 행동을 수행한다
    만일 "시스템" 역할이 "transmission-db" 행동을 수행한다
    만일 "source" 역할이 "supplier-reply" 행동을 수행한다
    만일 "procurement" 역할이 "reply-view" 행동을 수행한다
    만일 "시스템" 역할이 "reply-db" 행동을 수행한다
    그러면 "proposal-inventory-zero" assertion으로 "고정 oracle purchase-created-held-inventory의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "transmission-arrival-zero" assertion으로 "고정 oracle transmission-created-arrival의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "transmission-inventory-zero" assertion으로 "고정 oracle transmission-created-arrival의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "supplier-arrival-zero" assertion으로 "고정 oracle supplier-acceptance-created-arrival의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "supplier-inventory-zero" assertion으로 "고정 oracle supplier-acceptance-created-arrival의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "reply-distinct" assertion으로 "고정 oracle supplier-acceptance-created-arrival의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 발주 전달과 공급자 CHANGE는 도착을 만들지 않는다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "supplier-change"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "procurement" 역할이 "proposal-view" 행동을 수행한다
    만일 "시스템" 역할이 "proposal-db" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "시스템" 역할이 "delivery-ack" 행동을 수행한다
    만일 "source" 역할이 "transmission" 행동을 수행한다
    만일 "procurement" 역할이 "transmission-view" 행동을 수행한다
    만일 "시스템" 역할이 "transmission-db" 행동을 수행한다
    만일 "source" 역할이 "supplier-reply" 행동을 수행한다
    만일 "procurement" 역할이 "reply-view" 행동을 수행한다
    만일 "시스템" 역할이 "reply-db" 행동을 수행한다
    그러면 "proposal-inventory-zero" assertion으로 "고정 oracle purchase-created-held-inventory의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "transmission-arrival-zero" assertion으로 "고정 oracle transmission-created-arrival의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "transmission-inventory-zero" assertion으로 "고정 oracle transmission-created-arrival의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "supplier-arrival-zero" assertion으로 "고정 oracle supplier-acceptance-created-arrival의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "supplier-inventory-zero" assertion으로 "고정 oracle supplier-acceptance-created-arrival의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "reply-distinct" assertion으로 "고정 oracle supplier-acceptance-created-arrival의 실제 값과 범위를 확인한다"를 확인한다

  시나리오: 미실행 취소는 기출하 수령 송장을 지우지 않는다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "cancel-unexecuted"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "procurement" 역할이 "cancel" 행동을 수행한다
    만일 "procurement" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "only40-cancel" assertion으로 "고정 oracle cancel-scope의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "unconfirmed-cancel-owner-one" assertion으로 "고정 oracle cancel-scope의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "unconfirmed-cancel-owner-owner" assertion으로 "고정 oracle cancel-scope의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "segments-preserved" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "shipments-preserved" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "receipts-preserved" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "invoices-preserved" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다

  시나리오: 수령60 더하기40과 초과5를 분리하고 반품과 이동을 재기여하지 않는다
    먼저 사례 파일 "verification/cases/T13/case.json"의 "partial-excess-return-relocation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "procurement" 역할이 "work" 행동을 수행한다
    만일 "procurement" 역할이 "proposal" 행동을 수행한다
    만일 "manager" 역할이 "approval" 행동을 수행한다
    만일 "procurement" 역할이 "dispatch" 행동을 수행한다
    만일 "procurement" 역할이 "split-plan" 행동을 수행한다
    만일 "procurement" 역할이 "planned" 행동을 수행한다
    만일 "warehouse" 역할이 "receipt60" 행동을 수행한다
    만일 "warehouse" 역할이 "receipt40" 행동을 수행한다
    만일 "warehouse" 역할이 "receipt5" 행동을 수행한다
    만일 "procurement" 역할이 "after-receipts" 행동을 수행한다
    만일 "시스템" 역할이 "receipt-db" 행동을 수행한다
    만일 "sales" 역할이 "sale" 행동을 수행한다
    만일 "qc" 역할이 "qc-allow" 행동을 수행한다
    만일 "regulator" 역할이 "agency-procedure" 행동을 수행한다
    만일 "regulator" 역할이 "agency-submit" 행동을 수행한다
    만일 "regulator" 역할이 "agency-allow" 행동을 수행한다
    만일 "manager" 역할이 "disposition" 행동을 수행한다
    만일 "regulator" 역할이 "label" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "dispatch-sale" 행동을 수행한다
    만일 "source" 역할이 "delivery" 행동을 수행한다
    만일 "sales" 역할이 "return-approval" 행동을 수행한다
    만일 "warehouse" 역할이 "return" 행동을 수행한다
    만일 "procurement" 역할이 "after-return" 행동을 수행한다
    만일 "시스템" 역할이 "return-db" 행동을 수행한다
    만일 "warehouse" 역할이 "move" 행동을 수행한다
    만일 "procurement" 역할이 "after-move" 행동을 수행한다
    만일 "시스템" 역할이 "move-db" 행동을 수행한다
    만일 "procurement" 역할이 "proposal80" 행동을 수행한다
    만일 "manager" 역할이 "approval80" 행동을 수행한다
    만일 "manager" 역할이 "reduce-goal" 행동을 수행한다
    만일 "procurement" 역할이 "new-goal" 행동을 수행한다
    그러면 "plan-original-goal" assertion으로 "고정 oracle contribution-scope의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "plan-original-goal-version" assertion으로 "고정 oracle contribution-scope의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "arrival-api100" assertion으로 "고정 oracle authorized-arrival-contribution의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "held-db105" assertion으로 "고정 oracle excess-reconciliation의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "arrival-db100" assertion으로 "고정 oracle authorized-arrival-contribution의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "excess5" assertion으로 "고정 oracle excess-reconciliation의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "excess-owner-one" assertion으로 "고정 oracle excess-owner의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "excess-owner-owner" assertion으로 "고정 oracle excess-owner의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "distinct-contributions" assertion으로 "고정 oracle contribution-scope의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "canonical-contributions" assertion으로 "고정 oracle contribution-scope의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "return-no-new-contribution" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "relocation-no-new-contribution" assertion으로 "실제 전후 원행이 같아 금지 효과가 없다"를 확인한다
    그러면 "new-goal80" assertion으로 "고정 oracle contribution-scope의 실제 값과 범위를 확인한다"를 확인한다
    그러면 "new-goal-revision" assertion으로 "고정 oracle contribution-scope의 실제 값과 범위를 확인한다"를 확인한다
