# language: ko
@V4 @D08 @D17 @D24
기능: 대체 진입 경로와 내부 이동이 출고 인가를 우회하지 못한다

  @sit
  시나리오: 대체 경로 direct의 inventory 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "direct-inventory"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-logisticsMemberships" assertion으로 "moveQuantity의 주 효과 원천 logisticsMemberships 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-custodyHandovers" assertion으로 "moveQuantity의 주 효과 원천 custodyHandovers 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 direct의 allocation 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "direct-allocation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "reserveQuantity의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 direct의 work 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "direct-work"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-goalVersions" assertion으로 "createDraft의 주 효과 원천 goalVersions 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 direct의 approval 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "direct-approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-proposals" assertion으로 "approvePurchase의 주 효과 원천 proposals 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 direct의 trade 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "direct-trade"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "createSalesOrder의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 direct의 evidence 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "direct-evidence"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-stocktakes" assertion으로 "recordStocktake의 주 효과 원천 stocktakes 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 direct의 identity 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "direct-identity"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-validityBoundaries" assertion으로 "createGrant의 주 효과 원천 validityBoundaries 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 direct의 definition 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "direct-definition"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-definitionPackages" assertion으로 "createDefinitionDraft의 주 효과 원천 definitionPackages 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 direct의 policy 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "direct-policy"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-policyDrafts" assertion으로 "createPolicyDraft의 주 효과 원천 policyDrafts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 direct의 operations 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "direct-operations"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-retryAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 retryAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-executionAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 executionAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 nested의 inventory 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "nested-inventory"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-logisticsMemberships" assertion으로 "moveQuantity의 주 효과 원천 logisticsMemberships 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-custodyHandovers" assertion으로 "moveQuantity의 주 효과 원천 custodyHandovers 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 nested의 allocation 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "nested-allocation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "reserveQuantity의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 nested의 work 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "nested-work"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-goalVersions" assertion으로 "createDraft의 주 효과 원천 goalVersions 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 nested의 approval 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "nested-approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-proposals" assertion으로 "approvePurchase의 주 효과 원천 proposals 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 nested의 trade 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "nested-trade"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "createSalesOrder의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 nested의 evidence 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "nested-evidence"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-stocktakes" assertion으로 "recordStocktake의 주 효과 원천 stocktakes 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 nested의 identity 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "nested-identity"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-validityBoundaries" assertion으로 "createGrant의 주 효과 원천 validityBoundaries 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 nested의 definition 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "nested-definition"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-definitionPackages" assertion으로 "createDefinitionDraft의 주 효과 원천 definitionPackages 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 nested의 policy 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "nested-policy"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-policyDrafts" assertion으로 "createPolicyDraft의 주 효과 원천 policyDrafts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 nested의 operations 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "nested-operations"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-retryAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 retryAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-executionAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 executionAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 batch의 inventory 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "batch-inventory"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-logisticsMemberships" assertion으로 "moveQuantity의 주 효과 원천 logisticsMemberships 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-custodyHandovers" assertion으로 "moveQuantity의 주 효과 원천 custodyHandovers 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 batch의 allocation 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "batch-allocation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "reserveQuantity의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 batch의 work 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "batch-work"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-goalVersions" assertion으로 "createDraft의 주 효과 원천 goalVersions 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 batch의 approval 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "batch-approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-proposals" assertion으로 "approvePurchase의 주 효과 원천 proposals 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 batch의 trade 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "batch-trade"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "createSalesOrder의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 batch의 evidence 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "batch-evidence"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-stocktakes" assertion으로 "recordStocktake의 주 효과 원천 stocktakes 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 batch의 identity 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "batch-identity"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-validityBoundaries" assertion으로 "createGrant의 주 효과 원천 validityBoundaries 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 batch의 definition 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "batch-definition"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-definitionPackages" assertion으로 "createDefinitionDraft의 주 효과 원천 definitionPackages 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 batch의 policy 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "batch-policy"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-policyDrafts" assertion으로 "createPolicyDraft의 주 효과 원천 policyDrafts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 batch의 operations 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "batch-operations"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-retryAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 retryAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-executionAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 executionAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 projection의 inventory 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "projection-inventory"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-logisticsMemberships" assertion으로 "moveQuantity의 주 효과 원천 logisticsMemberships 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-custodyHandovers" assertion으로 "moveQuantity의 주 효과 원천 custodyHandovers 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 projection의 allocation 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "projection-allocation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "reserveQuantity의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 projection의 work 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "projection-work"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-goalVersions" assertion으로 "createDraft의 주 효과 원천 goalVersions 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 projection의 approval 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "projection-approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-proposals" assertion으로 "approvePurchase의 주 효과 원천 proposals 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 projection의 trade 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "projection-trade"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "createSalesOrder의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 projection의 evidence 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "projection-evidence"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-stocktakes" assertion으로 "recordStocktake의 주 효과 원천 stocktakes 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 projection의 identity 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "projection-identity"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-validityBoundaries" assertion으로 "createGrant의 주 효과 원천 validityBoundaries 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 projection의 definition 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "projection-definition"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-definitionPackages" assertion으로 "createDefinitionDraft의 주 효과 원천 definitionPackages 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 projection의 policy 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "projection-policy"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-policyDrafts" assertion으로 "createPolicyDraft의 주 효과 원천 policyDrafts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 projection의 operations 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "projection-operations"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-retryAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 retryAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-executionAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 executionAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 mcp의 inventory 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "mcp-inventory"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-logisticsMemberships" assertion으로 "moveQuantity의 주 효과 원천 logisticsMemberships 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-custodyHandovers" assertion으로 "moveQuantity의 주 효과 원천 custodyHandovers 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 mcp의 allocation 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "mcp-allocation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "reserveQuantity의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 mcp의 work 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "mcp-work"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-goalVersions" assertion으로 "createDraft의 주 효과 원천 goalVersions 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 mcp의 approval 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "mcp-approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-proposals" assertion으로 "approvePurchase의 주 효과 원천 proposals 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 mcp의 trade 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "mcp-trade"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "createSalesOrder의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 mcp의 evidence 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "mcp-evidence"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-stocktakes" assertion으로 "recordStocktake의 주 효과 원천 stocktakes 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 mcp의 identity 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "mcp-identity"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-validityBoundaries" assertion으로 "createGrant의 주 효과 원천 validityBoundaries 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 mcp의 definition 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "mcp-definition"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-definitionPackages" assertion으로 "createDefinitionDraft의 주 효과 원천 definitionPackages 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 mcp의 policy 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "mcp-policy"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-policyDrafts" assertion으로 "createPolicyDraft의 주 효과 원천 policyDrafts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 mcp의 operations 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "mcp-operations"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-retryAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 retryAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-executionAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 executionAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 worker의 inventory 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "worker-inventory"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-logisticsMemberships" assertion으로 "moveQuantity의 주 효과 원천 logisticsMemberships 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-custodyHandovers" assertion으로 "moveQuantity의 주 효과 원천 custodyHandovers 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 worker의 allocation 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "worker-allocation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "reserveQuantity의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 worker의 work 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "worker-work"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-goalVersions" assertion으로 "createDraft의 주 효과 원천 goalVersions 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 worker의 approval 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "worker-approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-proposals" assertion으로 "approvePurchase의 주 효과 원천 proposals 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 worker의 trade 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "worker-trade"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "createSalesOrder의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 worker의 evidence 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "worker-evidence"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-stocktakes" assertion으로 "recordStocktake의 주 효과 원천 stocktakes 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 worker의 identity 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "worker-identity"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-validityBoundaries" assertion으로 "createGrant의 주 효과 원천 validityBoundaries 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 worker의 definition 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "worker-definition"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-definitionPackages" assertion으로 "createDefinitionDraft의 주 효과 원천 definitionPackages 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 worker의 policy 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "worker-policy"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-policyDrafts" assertion으로 "createPolicyDraft의 주 효과 원천 policyDrafts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 worker의 operations 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "worker-operations"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-retryAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 retryAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-executionAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 executionAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 blob의 inventory 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "blob-inventory"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-logisticsMemberships" assertion으로 "moveQuantity의 주 효과 원천 logisticsMemberships 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-custodyHandovers" assertion으로 "moveQuantity의 주 효과 원천 custodyHandovers 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 blob의 allocation 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "blob-allocation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "reserveQuantity의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 blob의 work 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "blob-work"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-goalVersions" assertion으로 "createDraft의 주 효과 원천 goalVersions 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 blob의 approval 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "blob-approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-proposals" assertion으로 "approvePurchase의 주 효과 원천 proposals 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 blob의 trade 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "blob-trade"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "createSalesOrder의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 blob의 evidence 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "blob-evidence"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-stocktakes" assertion으로 "recordStocktake의 주 효과 원천 stocktakes 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 blob의 identity 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "blob-identity"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-validityBoundaries" assertion으로 "createGrant의 주 효과 원천 validityBoundaries 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 blob의 definition 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "blob-definition"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-definitionPackages" assertion으로 "createDefinitionDraft의 주 효과 원천 definitionPackages 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 blob의 policy 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "blob-policy"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-policyDrafts" assertion으로 "createPolicyDraft의 주 효과 원천 policyDrafts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 blob의 operations 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "blob-operations"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-retryAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 retryAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-executionAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 executionAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 management의 inventory 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "management-inventory"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-logisticsMemberships" assertion으로 "moveQuantity의 주 효과 원천 logisticsMemberships 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-custodyHandovers" assertion으로 "moveQuantity의 주 효과 원천 custodyHandovers 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 management의 allocation 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "management-allocation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "reserveQuantity의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 management의 work 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "management-work"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-goalVersions" assertion으로 "createDraft의 주 효과 원천 goalVersions 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 management의 approval 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "management-approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-proposals" assertion으로 "approvePurchase의 주 효과 원천 proposals 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 management의 trade 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "management-trade"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "createSalesOrder의 주 효과 원천 orders 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 management의 evidence 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "management-evidence"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-stocktakes" assertion으로 "recordStocktake의 주 효과 원천 stocktakes 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 management의 identity 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "management-identity"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-validityBoundaries" assertion으로 "createGrant의 주 효과 원천 validityBoundaries 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 management의 definition 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "management-definition"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-definitionPackages" assertion으로 "createDefinitionDraft의 주 효과 원천 definitionPackages 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 management의 policy 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "management-policy"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-policyDrafts" assertion으로 "createPolicyDraft의 주 효과 원천 policyDrafts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 대체 경로 management의 operations 쓰기는 현재 READ 위임을 넘지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "management-operations"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "attempt" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-same-route" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "attempt-code" assertion으로 "same-auth-path attempt-code"를 확인한다
    그러면 "attempt-outcome" assertion으로 "same-auth-path attempt-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "allocation-effects-rows0" assertion으로 "allocation-effects allocation-effects-rows0"를 확인한다
    그러면 "approval-effects-rows0" assertion으로 "approval-effects approval-effects-rows0"를 확인한다
    그러면 "outbox-effects-rows0" assertion으로 "outbox-effects outbox-effects-rows0"를 확인한다
    그러면 "authorized-route-committed" assertion으로 "same-auth-path authorized-route-committed"를 확인한다
    그러면 "authorized-route-reaches-command" assertion으로 "same-auth-path authorized-route-reaches-command"를 확인한다
    그러면 "unchanged-effect-retryAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 retryAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "unchanged-effect-executionAttempts" assertion으로 "retrySafeCommand의 주 효과 원천 executionAttempts 원행 전체를 조직 범위에서 거부 전후 exact 대조한다. 공용20개 원천에 없는 실제 업무 효과를 놓치지 않는다(C3 CAP_EFFECTS와 같은 표)."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "거부된 reader 자신의 command record는 COMMITTED가 아니다. REJECTED record와 거부 감사는 허용된 별도 기록이다."를 확인한다

  @sit
  시나리오: 허용된 RECORD와 금지된 출고가 한 changeset에 있으면 전체 업무 효과를 rollback한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "mixed-atomic-batch"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "recordOnly" 역할이 "batch" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "mcp-batch-before" 행동을 수행한다
    만일 "recordOnly" 역할이 "mcp-batch" 행동을 수행한다
    만일 "delegator" 역할이 "mcp-batch-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "mcp-batch-after" 행동을 수행한다
    그러면 "batch-code" assertion으로 "mixed-batch-allowed-partial-effects batch-code"를 확인한다
    그러면 "batch-outcome" assertion으로 "mixed-batch-allowed-partial-effects batch-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "mixed-batch-allowed-partial-effects unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "mixed-batch-allowed-partial-effects unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "mixed-batch-allowed-partial-effects unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "mixed-batch-allowed-partial-effects unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "mixed-batch-allowed-partial-effects unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "mixed-batch-allowed-partial-effects unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "mixed-batch-allowed-partial-effects unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "mixed-batch-allowed-partial-effects unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "mixed-batch-allowed-partial-effects unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "mixed-batch-allowed-partial-effects unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "mixed-batch-allowed-partial-effects unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "mixed-batch-allowed-partial-effects unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "mixed-batch-allowed-partial-effects unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "mixed-batch-allowed-partial-effects unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "mixed-batch-allowed-partial-effects unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "mixed-batch-allowed-partial-effects unchanged-relations"를 확인한다
    그러면 "mixed-batch-allowed-partial-effects-rows0" assertion으로 "mixed-batch-allowed-partial-effects mixed-batch-allowed-partial-effects-rows0"를 확인한다
    그러면 "inventory-effects-rows0" assertion으로 "inventory-effects inventory-effects-rows0"를 확인한다
    그러면 "batch-terminal-operations" assertion으로 "same-auth-path batch-terminal-operations"를 확인한다
    그러면 "mcp-batch-http" assertion으로 "MCP에는 changeset이 없다. 허용된 RECORD와 금지된 출고를 한 JSON-RPC batch 배열로 보내면 envelope 자체가 유효하지 않은 요청이라 HTTP 400이다(contracts/mcp/s0-protocol.md: batch와 malformed 입력은 거부한다)."를 확인한다
    그러면 "mcp-batch-invalid-request" assertion으로 "batch 배열은 JSON-RPC Invalid Request(-32600)로 한 번에 거부한다. 요소별 tool result로 나눠 일부를 실행하지 않는다. Mcp-Name header도 없지만 envelope 검사가 mirrored header 검사보다 먼저이므로 -32020이 아니다(contracts/mcp/s0-protocol.md 오류 우선순위)."를 확인한다
    그러면 "mcp-batch-no-tool-result" assertion으로 "거부된 batch 응답에는 tool result가 없다. 허용 요소만 실행한 결과를 돌려주지 않는다."를 확인한다
    그러면 "mcp-batch-allowed-record-not-committed" assertion으로 "batch 안의 허용된 recordStocktake는 MCP 경로에서도 COMMITTED command를 남기지 않는다(부분 효과0)."를 확인한다
    그러면 "mcp-batch-allowed-claims0" assertion으로 "허용된 RECORD의 실행 claim도 0이다. 거부 전에 일부 요소가 실행 단계에 들어가지 않았다."를 확인한다
    그러면 "mcp-batch-unchanged-segments" assertion으로 "MCP batch 전후 조직 범위의 segments 원행 전체가 같다. 허용된 RECORD(stocktakes 포함)와 금지된 출고 모두 효과0이다."를 확인한다
    그러면 "mcp-batch-unchanged-movements" assertion으로 "MCP batch 전후 조직 범위의 movements 원행 전체가 같다. 허용된 RECORD(stocktakes 포함)와 금지된 출고 모두 효과0이다."를 확인한다
    그러면 "mcp-batch-unchanged-allocations" assertion으로 "MCP batch 전후 조직 범위의 allocations 원행 전체가 같다. 허용된 RECORD(stocktakes 포함)와 금지된 출고 모두 효과0이다."를 확인한다
    그러면 "mcp-batch-unchanged-approvals" assertion으로 "MCP batch 전후 조직 범위의 approvals 원행 전체가 같다. 허용된 RECORD(stocktakes 포함)와 금지된 출고 모두 효과0이다."를 확인한다
    그러면 "mcp-batch-unchanged-works" assertion으로 "MCP batch 전후 조직 범위의 works 원행 전체가 같다. 허용된 RECORD(stocktakes 포함)와 금지된 출고 모두 효과0이다."를 확인한다
    그러면 "mcp-batch-unchanged-obligations" assertion으로 "MCP batch 전후 조직 범위의 obligations 원행 전체가 같다. 허용된 RECORD(stocktakes 포함)와 금지된 출고 모두 효과0이다."를 확인한다
    그러면 "mcp-batch-unchanged-outbox" assertion으로 "MCP batch 전후 조직 범위의 outbox 원행 전체가 같다. 허용된 RECORD(stocktakes 포함)와 금지된 출고 모두 효과0이다."를 확인한다
    그러면 "mcp-batch-unchanged-claims" assertion으로 "MCP batch 전후 조직 범위의 claims 원행 전체가 같다. 허용된 RECORD(stocktakes 포함)와 금지된 출고 모두 효과0이다."를 확인한다
    그러면 "mcp-batch-unchanged-stocktakes" assertion으로 "MCP batch 전후 조직 범위의 stocktakes 원행 전체가 같다. 허용된 RECORD(stocktakes 포함)와 금지된 출고 모두 효과0이다."를 확인한다

  @sit
  시나리오: 공개하지 않은 core CRUD QuantityMovement가 원장을 변경하지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "raw-crud-quantitymovement"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "raw-patch" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "unexposed-http-route" assertion으로 "same-auth-path unexposed-http-route"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다

  @sit
  시나리오: 공개하지 않은 core CRUD QuantitySegment가 원장을 변경하지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "raw-crud-quantitysegment"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "raw-patch" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "unexposed-http-route" assertion으로 "same-auth-path unexposed-http-route"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다

  @sit
  시나리오: 공개하지 않은 core CRUD Allocation가 원장을 변경하지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "raw-crud-allocation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "raw-patch" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "unexposed-http-route" assertion으로 "same-auth-path unexposed-http-route"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다

  @sit
  시나리오: 공개하지 않은 core CRUD Approval가 원장을 변경하지 못한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "raw-crud-approval"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "raw-patch" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "unexposed-http-route" assertion으로 "same-auth-path unexposed-http-route"를 확인한다
    그러면 "unchanged-segments" assertion으로 "same-auth-path unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "same-auth-path unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "same-auth-path unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "same-auth-path unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "same-auth-path unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "same-auth-path unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "same-auth-path unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "same-auth-path unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "same-auth-path unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "same-auth-path unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "same-auth-path unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "same-auth-path unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "same-auth-path unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "same-auth-path unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "same-auth-path unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "same-auth-path unchanged-relations"를 확인한다

  @sit
  시나리오: 내부 이동과 외부 실제 관측을 구별한다: internal-success
    먼저 사례 파일 "verification/cases/V4/case.json"의 "internal-success"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "move" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "internal-move-applied" assertion으로 "internal-move-created-customer-dispatch internal-move-applied"를 확인한다
    그러면 "only-internal-move" assertion으로 "internal-move-created-customer-dispatch only-internal-move"를 확인한다
    그러면 "internal-move-created-customer-dispatch-DISPATCH" assertion으로 "internal-move-created-customer-dispatch internal-move-created-customer-dispatch-DISPATCH"를 확인한다
    그러면 "internal-move-created-order-fulfilment-unchanged" assertion으로 "internal-move-created-order-fulfilment internal-move-created-order-fulfilment-unchanged"를 확인한다
    그러면 "internal-move-created-return-or-disposal-RETURN" assertion으로 "internal-move-created-return-or-disposal internal-move-created-return-or-disposal-RETURN"를 확인한다
    그러면 "internal-move-created-return-or-disposal-DISPOSAL" assertion으로 "internal-move-created-return-or-disposal internal-move-created-return-or-disposal-DISPOSAL"를 확인한다

  @sit
  시나리오: 내부 이동과 외부 실제 관측을 구별한다: destination-id-swap
    먼저 사례 파일 "verification/cases/V4/case.json"의 "destination-id-swap"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "move" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "move-code" assertion으로 "internal-move-created-customer-dispatch move-code"를 확인한다
    그러면 "move-outcome" assertion으로 "internal-move-created-customer-dispatch move-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "internal-move-created-customer-dispatch unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "internal-move-created-customer-dispatch unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "internal-move-created-customer-dispatch unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "internal-move-created-customer-dispatch unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "internal-move-created-customer-dispatch unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "internal-move-created-customer-dispatch unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "internal-move-created-customer-dispatch unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "internal-move-created-customer-dispatch unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "internal-move-created-customer-dispatch unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "internal-move-created-customer-dispatch unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "internal-move-created-customer-dispatch unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "internal-move-created-customer-dispatch unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "internal-move-created-customer-dispatch unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "internal-move-created-customer-dispatch unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "internal-move-created-customer-dispatch unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "internal-move-created-customer-dispatch unchanged-relations"를 확인한다
    그러면 "internal-move-created-customer-dispatch-DISPATCH" assertion으로 "internal-move-created-customer-dispatch internal-move-created-customer-dispatch-DISPATCH"를 확인한다
    그러면 "internal-move-created-order-fulfilment-unchanged" assertion으로 "internal-move-created-order-fulfilment internal-move-created-order-fulfilment-unchanged"를 확인한다
    그러면 "internal-move-created-return-or-disposal-RETURN" assertion으로 "internal-move-created-return-or-disposal internal-move-created-return-or-disposal-RETURN"를 확인한다
    그러면 "internal-move-created-return-or-disposal-DISPOSAL" assertion으로 "internal-move-created-return-or-disposal internal-move-created-return-or-disposal-DISPOSAL"를 확인한다

  @sit
  시나리오: 내부 이동과 외부 실제 관측을 구별한다: renamed-action
    먼저 사례 파일 "verification/cases/V4/case.json"의 "renamed-action"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "move" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "move-code" assertion으로 "internal-move-created-customer-dispatch move-code"를 확인한다
    그러면 "move-outcome" assertion으로 "internal-move-created-customer-dispatch move-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "internal-move-created-customer-dispatch unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "internal-move-created-customer-dispatch unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "internal-move-created-customer-dispatch unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "internal-move-created-customer-dispatch unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "internal-move-created-customer-dispatch unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "internal-move-created-customer-dispatch unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "internal-move-created-customer-dispatch unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "internal-move-created-customer-dispatch unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "internal-move-created-customer-dispatch unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "internal-move-created-customer-dispatch unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "internal-move-created-customer-dispatch unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "internal-move-created-customer-dispatch unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "internal-move-created-customer-dispatch unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "internal-move-created-customer-dispatch unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "internal-move-created-customer-dispatch unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "internal-move-created-customer-dispatch unchanged-relations"를 확인한다
    그러면 "internal-move-created-customer-dispatch-DISPATCH" assertion으로 "internal-move-created-customer-dispatch internal-move-created-customer-dispatch-DISPATCH"를 확인한다
    그러면 "internal-move-created-order-fulfilment-unchanged" assertion으로 "internal-move-created-order-fulfilment internal-move-created-order-fulfilment-unchanged"를 확인한다
    그러면 "internal-move-created-return-or-disposal-RETURN" assertion으로 "internal-move-created-return-or-disposal internal-move-created-return-or-disposal-RETURN"를 확인한다
    그러면 "internal-move-created-return-or-disposal-DISPOSAL" assertion으로 "internal-move-created-return-or-disposal internal-move-created-return-or-disposal-DISPOSAL"를 확인한다

  @sit
  시나리오: 내부 이동과 외부 실제 관측을 구별한다: raw-primitive
    먼저 사례 파일 "verification/cases/V4/case.json"의 "raw-primitive"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "move" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "move-code" assertion으로 "internal-move-created-customer-dispatch move-code"를 확인한다
    그러면 "move-outcome" assertion으로 "internal-move-created-customer-dispatch move-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "internal-move-created-customer-dispatch unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "internal-move-created-customer-dispatch unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "internal-move-created-customer-dispatch unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "internal-move-created-customer-dispatch unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "internal-move-created-customer-dispatch unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "internal-move-created-customer-dispatch unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "internal-move-created-customer-dispatch unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "internal-move-created-customer-dispatch unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "internal-move-created-customer-dispatch unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "internal-move-created-customer-dispatch unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "internal-move-created-customer-dispatch unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "internal-move-created-customer-dispatch unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "internal-move-created-customer-dispatch unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "internal-move-created-customer-dispatch unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "internal-move-created-customer-dispatch unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "internal-move-created-customer-dispatch unchanged-relations"를 확인한다
    그러면 "internal-move-created-customer-dispatch-DISPATCH" assertion으로 "internal-move-created-customer-dispatch internal-move-created-customer-dispatch-DISPATCH"를 확인한다
    그러면 "internal-move-created-order-fulfilment-unchanged" assertion으로 "internal-move-created-order-fulfilment internal-move-created-order-fulfilment-unchanged"를 확인한다
    그러면 "internal-move-created-return-or-disposal-RETURN" assertion으로 "internal-move-created-return-or-disposal internal-move-created-return-or-disposal-RETURN"를 확인한다
    그러면 "internal-move-created-return-or-disposal-DISPOSAL" assertion으로 "internal-move-created-return-or-disposal internal-move-created-return-or-disposal-DISPOSAL"를 확인한다

  @sit
  시나리오: 내부 이동과 외부 실제 관측을 구별한다: return-bypass
    먼저 사례 파일 "verification/cases/V4/case.json"의 "return-bypass"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "move" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "move-code" assertion으로 "internal-move-created-customer-dispatch move-code"를 확인한다
    그러면 "move-outcome" assertion으로 "internal-move-created-customer-dispatch move-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "internal-move-created-customer-dispatch unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "internal-move-created-customer-dispatch unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "internal-move-created-customer-dispatch unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "internal-move-created-customer-dispatch unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "internal-move-created-customer-dispatch unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "internal-move-created-customer-dispatch unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "internal-move-created-customer-dispatch unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "internal-move-created-customer-dispatch unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "internal-move-created-customer-dispatch unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "internal-move-created-customer-dispatch unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "internal-move-created-customer-dispatch unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "internal-move-created-customer-dispatch unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "internal-move-created-customer-dispatch unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "internal-move-created-customer-dispatch unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "internal-move-created-customer-dispatch unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "internal-move-created-customer-dispatch unchanged-relations"를 확인한다
    그러면 "internal-move-created-customer-dispatch-DISPATCH" assertion으로 "internal-move-created-customer-dispatch internal-move-created-customer-dispatch-DISPATCH"를 확인한다
    그러면 "internal-move-created-order-fulfilment-unchanged" assertion으로 "internal-move-created-order-fulfilment internal-move-created-order-fulfilment-unchanged"를 확인한다
    그러면 "internal-move-created-return-or-disposal-RETURN" assertion으로 "internal-move-created-return-or-disposal internal-move-created-return-or-disposal-RETURN"를 확인한다
    그러면 "internal-move-created-return-or-disposal-DISPOSAL" assertion으로 "internal-move-created-return-or-disposal internal-move-created-return-or-disposal-DISPOSAL"를 확인한다

  @sit
  시나리오: 내부 이동과 외부 실제 관측을 구별한다: disposal-bypass
    먼저 사례 파일 "verification/cases/V4/case.json"의 "disposal-bypass"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "move" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "move-code" assertion으로 "internal-move-created-customer-dispatch move-code"를 확인한다
    그러면 "move-outcome" assertion으로 "internal-move-created-customer-dispatch move-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "internal-move-created-customer-dispatch unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "internal-move-created-customer-dispatch unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "internal-move-created-customer-dispatch unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "internal-move-created-customer-dispatch unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "internal-move-created-customer-dispatch unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "internal-move-created-customer-dispatch unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "internal-move-created-customer-dispatch unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "internal-move-created-customer-dispatch unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "internal-move-created-customer-dispatch unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "internal-move-created-customer-dispatch unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "internal-move-created-customer-dispatch unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "internal-move-created-customer-dispatch unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "internal-move-created-customer-dispatch unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "internal-move-created-customer-dispatch unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "internal-move-created-customer-dispatch unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "internal-move-created-customer-dispatch unchanged-relations"를 확인한다
    그러면 "internal-move-created-customer-dispatch-DISPATCH" assertion으로 "internal-move-created-customer-dispatch internal-move-created-customer-dispatch-DISPATCH"를 확인한다
    그러면 "internal-move-created-order-fulfilment-unchanged" assertion으로 "internal-move-created-order-fulfilment internal-move-created-order-fulfilment-unchanged"를 확인한다
    그러면 "internal-move-created-return-or-disposal-RETURN" assertion으로 "internal-move-created-return-or-disposal internal-move-created-return-or-disposal-RETURN"를 확인한다
    그러면 "internal-move-created-return-or-disposal-DISPOSAL" assertion으로 "internal-move-created-return-or-disposal internal-move-created-return-or-disposal-DISPOSAL"를 확인한다

  @sit
  시나리오: 내부 이동과 외부 실제 관측을 구별한다: observed-unauthorized
    먼저 사례 파일 "verification/cases/V4/case.json"의 "observed-unauthorized"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "recordOnly" 역할이 "move" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "observation-accepted" assertion으로 "observed-unauthorized-fact observation-accepted"를 확인한다
    그러면 "actual-claim-scope" assertion으로 "observed-unauthorized-fact actual-claim-scope"를 확인한다
    그러면 "unauthorized-move-reconciliation-assignment1" assertion으로 "unauthorized-move-reconciliation unauthorized-move-reconciliation-assignment1"를 확인한다
    그러면 "unauthorized-move-reconciliation-owner" assertion으로 "unauthorized-move-reconciliation unauthorized-move-reconciliation-owner"를 확인한다
    그러면 "unauthorized-move-reconciliation-assignment-scope" assertion으로 "unauthorized-move-reconciliation unauthorized-move-reconciliation-assignment-scope"를 확인한다
    그러면 "internal-move-created-customer-dispatch-DISPATCH" assertion으로 "internal-move-created-customer-dispatch internal-move-created-customer-dispatch-DISPATCH"를 확인한다
    그러면 "internal-move-created-order-fulfilment-unchanged" assertion으로 "internal-move-created-order-fulfilment internal-move-created-order-fulfilment-unchanged"를 확인한다
    그러면 "internal-move-created-return-or-disposal-RETURN" assertion으로 "internal-move-created-return-or-disposal internal-move-created-return-or-disposal-RETURN"를 확인한다
    그러면 "internal-move-created-return-or-disposal-DISPOSAL" assertion으로 "internal-move-created-return-or-disposal internal-move-created-return-or-disposal-DISPOSAL"를 확인한다

  @sit
  시나리오: 실행 중 시스템이 노출한 모든 쓰기 면을 열거하고 READ 주체의 우회 효과0을 확인한다
    먼저 사례 파일 "verification/cases/V4/case.json"의 "exposed-write-surface"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "effect-before" 행동을 수행한다
    만일 "reader" 역할이 "surface-discover" 행동을 수행한다
    만일 "reader" 역할이 "surface-tools" 행동을 수행한다
    만일 "시스템" 역할이 "enumerate" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "effect-after" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-control" 행동을 수행한다
    만일 "delegator" 역할이 "authorized-after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "authorized-after" 행동을 수행한다
    그러면 "surfaces-enumerated" assertion으로 "실행 중 시스템의 다섯 노출 면(OData $metadata entity set·action·function, MCP server/discover, MCP tools/list, worker handler registry, 관리/actuator endpoint)을 실제로 읽었다. 고정 목록이 아닌 이 열거가 아래 probe의 대상이다(계획 §4.2 실제 노출 경로)."를 확인한다
    그러면 "surfaces-read-artifacts" assertion으로 "각 노출 면의 원문 artifact 위치·hash와 열거 항목 수가 독립 extractor 원행에 있다."를 확인한다
    그러면 "no-unlisted-write-surface" assertion으로 "쓰기 가능한 노출 항목(entity set의 insert/update/delete/upsert, action, tool, handler, endpoint)은 모두 공개 capability allowlist에 있다. raw core CRUD나 목록 밖 쓰기 면이 하나라도 있으면 실패한다."를 확인한다
    그러면 "mcp-tools-equal-allowlist" assertion으로 "READ 주체가 받은 MCP tools/list는 공개 capability allowlist와 정확히 같다. 목록 밖 tool은 없다."를 확인한다
    그러면 "mcp-discover-complete" assertion으로 "server/discover를 실제로 왕복해 노출 capability를 읽었다."를 확인한다
    그러면 "probe-classes-covered" assertion으로 "열거한 대상마다 direct 생성·수정·삭제, deep insert, upsert, $batch changeset, draft activation, nested navigation 생성·수정·삭제, bound/unbound action, MCP tools/call, worker 제출, 관리 endpoint 쓰기를 시도했다."를 확인한다
    그러면 "probe-coverage-complete" assertion으로 "probe class마다 열거된 적용 대상 전부를 시도했다. 일부 대상만 probe하고 통과하지 않는다."를 확인한다
    그러면 "probes-observed" assertion으로 "모든 probe의 대상·방식·결과·commit 여부와 redacted transcript가 원행으로 남는다. probe 0개는 통과가 아니다."를 확인한다
    그러면 "probes-none-committed" assertion으로 "READ grant 주체의 probe는 하나도 commit되지 않았다."를 확인한다
    그러면 "probes-none-applied" assertion으로 "READ grant 주체의 probe 응답 중 APPLIED는0이다."를 확인한다
    그러면 "probes-none-unknown" assertion으로 "시간초과·응답 유실 등 결과 미확인 probe는0이다. 미확인을 거부로 세지 않는다."를 확인한다
    그러면 "unchanged-segments" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 segments 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-movements" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 movements 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-allocations" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 allocations 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-approvals" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 approvals 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-works" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 works 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-goals" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 goals 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-obligations" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 obligations 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-assignments" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 assignments 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-outbox" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 outbox 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-claims" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 claims 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-grants" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 grants 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 capabilityAssignments 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 boundaries 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-definitions" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 definitions 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-policies" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 policies 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-relations" assertion으로 "열거 probe 전후 동일 조직·업무·실물 scope의 relations 원행 전체가 같아 업무 효과0이다. 감사와 거부 record는 별도다."를 확인한다
    그러면 "unchanged-effect-activeDefinitionPointers" assertion으로 "모든 capability의 주 효과 원천 activeDefinitionPointers 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-activePolicies" assertion으로 "모든 capability의 주 효과 원천 activePolicies 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-activities" assertion으로 "모든 capability의 주 효과 원천 activities 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-adjustmentProposals" assertion으로 "모든 capability의 주 효과 원천 adjustmentProposals 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-bankTransfers" assertion으로 "모든 capability의 주 효과 원천 bankTransfers 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-cancellationRequests" assertion으로 "모든 capability의 주 효과 원천 cancellationRequests 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-canonicalOccurrences" assertion으로 "모든 capability의 주 효과 원천 canonicalOccurrences 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-canonical_links" assertion으로 "모든 capability의 주 효과 원천 canonical_links 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-cargoAllocations" assertion으로 "모든 capability의 주 효과 원천 cargoAllocations 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-charges" assertion으로 "모든 capability의 주 효과 원천 charges 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-custodyHandovers" assertion으로 "모든 capability의 주 효과 원천 custodyHandovers 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-definitionApprovals" assertion으로 "모든 capability의 주 효과 원천 definitionApprovals 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-definitionPackages" assertion으로 "모든 capability의 주 효과 원천 definitionPackages 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-deliveries" assertion으로 "모든 capability의 주 효과 원천 deliveries 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-dispositionBases" assertion으로 "모든 capability의 주 효과 원천 dispositionBases 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-dispositions" assertion으로 "모든 capability의 주 효과 원천 dispositions 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-documents" assertion으로 "모든 capability의 주 효과 원천 documents 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-dutyTransitions" assertion으로 "모든 capability의 주 효과 원천 dutyTransitions 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-events" assertion으로 "모든 capability의 주 효과 원천 events 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-evidenceLinks" assertion으로 "모든 capability의 주 효과 원천 evidenceLinks 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-evidence_revisions" assertion으로 "모든 capability의 주 효과 원천 evidence_revisions 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-executionAttempts" assertion으로 "모든 capability의 주 효과 원천 executionAttempts 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-externalIdentifiers" assertion으로 "모든 capability의 주 효과 원천 externalIdentifiers 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-externalOperationResults" assertion으로 "모든 capability의 주 효과 원천 externalOperationResults 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-genealogy" assertion으로 "모든 capability의 주 효과 원천 genealogy 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-goalVersions" assertion으로 "모든 capability의 주 효과 원천 goalVersions 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-handovers" assertion으로 "모든 capability의 주 효과 원천 handovers 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-identity_matches" assertion으로 "모든 capability의 주 효과 원천 identity_matches 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-investigations" assertion으로 "모든 capability의 주 효과 원천 investigations 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-invoiceDifferences" assertion으로 "모든 capability의 주 효과 원천 invoiceDifferences 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-invoices" assertion으로 "모든 capability의 주 효과 원천 invoices 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-items" assertion으로 "모든 capability의 주 효과 원천 items 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-labels" assertion으로 "모든 capability의 주 효과 원천 labels 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-legs" assertion으로 "모든 capability의 주 효과 원천 legs 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-logisticsMemberships" assertion으로 "모든 capability의 주 효과 원천 logisticsMemberships 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-managementAuthorities" assertion으로 "모든 capability의 주 효과 원천 managementAuthorities 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-notices" assertion으로 "모든 capability의 주 효과 원천 notices 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-obligationResolutions" assertion으로 "모든 capability의 주 효과 원천 obligationResolutions 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-obligationTransfers" assertion으로 "모든 capability의 주 효과 원천 obligationTransfers 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-orders" assertion으로 "모든 capability의 주 효과 원천 orders 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-paymentReferences" assertion으로 "모든 capability의 주 효과 원천 paymentReferences 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-policyApprovals" assertion으로 "모든 capability의 주 효과 원천 policyApprovals 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-policyDrafts" assertion으로 "모든 capability의 주 효과 원천 policyDrafts 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-procedures" assertion으로 "모든 capability의 주 효과 원천 procedures 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-projections" assertion으로 "모든 capability의 주 효과 원천 projections 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-proposals" assertion으로 "모든 capability의 주 효과 원천 proposals 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-purchaseMatches" assertion으로 "모든 capability의 주 효과 원천 purchaseMatches 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-purchaseOrders" assertion으로 "모든 capability의 주 효과 원천 purchaseOrders 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-purchaseRevisions" assertion으로 "모든 capability의 주 효과 원천 purchaseRevisions 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-recallClosures" assertion으로 "모든 capability의 주 효과 원천 recallClosures 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-recallScopes" assertion으로 "모든 capability의 주 효과 원천 recallScopes 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-receiptContributions" assertion으로 "모든 capability의 주 효과 원천 receiptContributions 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-receipts" assertion으로 "모든 capability의 주 효과 원천 receipts 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-reconciliations" assertion으로 "모든 capability의 주 효과 원천 reconciliations 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-recoveries" assertion으로 "모든 capability의 주 효과 원천 recoveries 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-regressionRuns" assertion으로 "모든 capability의 주 효과 원천 regressionRuns 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-regulatoryDecisions" assertion으로 "모든 capability의 주 효과 원천 regulatoryDecisions 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-restrictions" assertion으로 "모든 capability의 주 효과 원천 restrictions 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-retryAttempts" assertion으로 "모든 capability의 주 효과 원천 retryAttempts 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-returns" assertion으로 "모든 capability의 주 효과 원천 returns 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-saleMatches" assertion으로 "모든 capability의 주 효과 원천 saleMatches 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-settlementAdjustments" assertion으로 "모든 capability의 주 효과 원천 settlementAdjustments 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-shipments" assertion으로 "모든 capability의 주 효과 원천 shipments 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-stocktakes" assertion으로 "모든 capability의 주 효과 원천 stocktakes 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-submissions" assertion으로 "모든 capability의 주 효과 원천 submissions 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-supplierCommitments" assertion으로 "모든 capability의 주 효과 원천 supplierCommitments 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-unitConversions" assertion으로 "모든 capability의 주 효과 원천 unitConversions 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-validityBoundaries" assertion으로 "모든 capability의 주 효과 원천 validityBoundaries 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-workClosures" assertion으로 "모든 capability의 주 효과 원천 workClosures 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-workDefinitionMigrations" assertion으로 "모든 capability의 주 효과 원천 workDefinitionMigrations 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "unchanged-effect-workLinks" assertion으로 "모든 capability의 주 효과 원천 workLinks 원행 전체가 조직 범위에서 열거 probe 전후 같다."를 확인한다
    그러면 "reader-command-not-committed" assertion으로 "probe 동안 reader 명의로 COMMITTED된 command record는0이다."를 확인한다
    그러면 "authorized-control-applied" assertion으로 "같은 설치에서 위임 있는 delegator의 공개 MCP moveQuantity 20 BOX는 APPLIED다. 서버가 모든 쓰기를 무조건 막아 통과하는 것이 아니다."를 확인한다
    그러면 "authorized-control-committed-once" assertion으로 "대조 호출은 자기 멱등키로 정확히 한 번 COMMITTED다."를 확인한다
