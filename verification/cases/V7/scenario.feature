# language: ko
@V7 @D08 @D26
기능: 철회와 실행 commit의 직렬화 순서와 재시작 책임을 검증한다

  @sit
  시나리오: 현재 grant 철회와 출고 commit의 실제 순서를 대조한다: enqueue-then-revoke
    먼저 사례 파일 "verification/cases/V7/case.json"의 "enqueue-then-revoke"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-start" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-ready" 행동을 수행한다
    만일 "delegator" 역할이 "revoke" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-resume" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-result" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "dispatch-ready-point" assertion으로 "worker-auth dispatch-ready-point"를 확인한다
    그러면 "dispatch-ready-state" assertion으로 "worker-auth dispatch-ready-state"를 확인한다
    그러면 "revoke-committed" assertion으로 "worker-auth revoke-committed"를 확인한다
    그러면 "dispatch-resume-point" assertion으로 "worker-auth dispatch-resume-point"를 확인한다
    그러면 "dispatch-resume-state" assertion으로 "worker-auth dispatch-resume-state"를 확인한다
    그러면 "dispatch-result-completed" assertion으로 "worker-auth dispatch-result-completed"를 확인한다
    그러면 "dispatch-result-same-handle" assertion으로 "worker-auth dispatch-result-same-handle"를 확인한다
    그러면 "dispatch-result-code" assertion으로 "worker-auth dispatch-result-code"를 확인한다
    그러면 "dispatch-result-outcome" assertion으로 "worker-auth dispatch-result-outcome"를 확인한다
    그러면 "new-dispatch-rows0" assertion으로 "worker-auth new-dispatch-rows0"를 확인한다
    그러면 "new20-command-not-committed" assertion으로 "worker-auth new20-command-not-committed"를 확인한다
    그러면 "new-effect-quantity0" assertion으로 "new-effects-quantity new-effect-quantity0"를 확인한다
    그러면 "blocked-duty-assignment1" assertion으로 "blocked-duty blocked-duty-assignment1"를 확인한다
    그러면 "blocked-duty-owner" assertion으로 "blocked-duty blocked-duty-owner"를 확인한다
    그러면 "blocked-duty-assignment-scope" assertion으로 "blocked-duty blocked-duty-assignment-scope"를 확인한다
    그러면 "revoked-current-grant" assertion으로 "worker-auth revoked-current-grant"를 확인한다
    그러면 "common-scope-fence" assertion으로 "worker-auth common-scope-fence"를 확인한다

  @sit
  시나리오: 현재 grant 철회와 출고 commit의 실제 순서를 대조한다: authorization-revoke-first
    먼저 사례 파일 "verification/cases/V7/case.json"의 "authorization-revoke-first"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-start" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-ready" 행동을 수행한다
    만일 "delegator" 역할이 "revoke" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-resume" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-result" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "dispatch-ready-point" assertion으로 "blocked-or-post-revoke-duty dispatch-ready-point"를 확인한다
    그러면 "dispatch-ready-state" assertion으로 "blocked-or-post-revoke-duty dispatch-ready-state"를 확인한다
    그러면 "revoke-committed" assertion으로 "linearization-evidence revoke-committed"를 확인한다
    그러면 "dispatch-resume-point" assertion으로 "blocked-or-post-revoke-duty dispatch-resume-point"를 확인한다
    그러면 "dispatch-resume-state" assertion으로 "blocked-or-post-revoke-duty dispatch-resume-state"를 확인한다
    그러면 "dispatch-result-completed" assertion으로 "blocked-or-post-revoke-duty dispatch-result-completed"를 확인한다
    그러면 "dispatch-result-same-handle" assertion으로 "blocked-or-post-revoke-duty dispatch-result-same-handle"를 확인한다
    그러면 "dispatch-result-code" assertion으로 "linearization-evidence dispatch-result-code"를 확인한다
    그러면 "dispatch-result-outcome" assertion으로 "linearization-evidence dispatch-result-outcome"를 확인한다
    그러면 "new-dispatch-rows0" assertion으로 "linearization-evidence new-dispatch-rows0"를 확인한다
    그러면 "new20-command-not-committed" assertion으로 "linearization-evidence new20-command-not-committed"를 확인한다
    그러면 "revoke-is-only-new-fence-commit" assertion으로 "linearization-evidence revoke-is-only-new-fence-commit"를 확인한다
    그러면 "new-effect-quantity0" assertion으로 "effect-if-revoke-commits-first new-effect-quantity0"를 확인한다
    그러면 "blocked-or-post-revoke-duty-assignment1" assertion으로 "blocked-or-post-revoke-duty blocked-or-post-revoke-duty-assignment1"를 확인한다
    그러면 "blocked-or-post-revoke-duty-owner" assertion으로 "blocked-or-post-revoke-duty blocked-or-post-revoke-duty-owner"를 확인한다
    그러면 "blocked-or-post-revoke-duty-assignment-scope" assertion으로 "blocked-or-post-revoke-duty blocked-or-post-revoke-duty-assignment-scope"를 확인한다
    그러면 "revoked-current-grant" assertion으로 "linearization-evidence revoked-current-grant"를 확인한다
    그러면 "common-scope-fence" assertion으로 "linearization-evidence common-scope-fence"를 확인한다

  @sit
  시나리오: 현재 grant 철회와 출고 commit의 실제 순서를 대조한다: authorization-effect-first
    먼저 사례 파일 "verification/cases/V7/case.json"의 "authorization-effect-first"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-start" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-ready" 행동을 수행한다
    만일 "시스템" 역할이 "revoke-start" 행동을 수행한다
    만일 "시스템" 역할이 "revoke-blocked" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-resume" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-result" 행동을 수행한다
    만일 "시스템" 역할이 "revoke-resume" 행동을 수행한다
    만일 "시스템" 역할이 "revoke-result" 행동을 수행한다
    만일 "warehouse" 역할이 "post-revoke" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "dispatch-ready-point" assertion으로 "blocked-or-post-revoke-duty dispatch-ready-point"를 확인한다
    그러면 "dispatch-ready-state" assertion으로 "blocked-or-post-revoke-duty dispatch-ready-state"를 확인한다
    그러면 "revoke-blocked-point" assertion으로 "blocked-or-post-revoke-duty revoke-blocked-point"를 확인한다
    그러면 "revoke-blocked-state" assertion으로 "blocked-or-post-revoke-duty revoke-blocked-state"를 확인한다
    그러면 "race-distinct-db-transactions" assertion으로 "출고와 철회는 각자 멈춘 시점의 실제 PostgreSQL transaction ID를 barrier ACK로 보고하며 서로 다르다. 요청의 tx label을 복사하지 않는다(V2 race-observation-contract.md)."를 확인한다
    그러면 "dispatch-resume-point" assertion으로 "blocked-or-post-revoke-duty dispatch-resume-point"를 확인한다
    그러면 "dispatch-resume-state" assertion으로 "blocked-or-post-revoke-duty dispatch-resume-state"를 확인한다
    그러면 "revoke-resume-point" assertion으로 "blocked-or-post-revoke-duty revoke-resume-point"를 확인한다
    그러면 "revoke-resume-state" assertion으로 "blocked-or-post-revoke-duty revoke-resume-state"를 확인한다
    그러면 "dispatch-result-completed" assertion으로 "blocked-or-post-revoke-duty dispatch-result-completed"를 확인한다
    그러면 "dispatch-result-same-handle" assertion으로 "blocked-or-post-revoke-duty dispatch-result-same-handle"를 확인한다
    그러면 "revoke-result-completed" assertion으로 "blocked-or-post-revoke-duty revoke-result-completed"를 확인한다
    그러면 "revoke-result-same-handle" assertion으로 "blocked-or-post-revoke-duty revoke-result-same-handle"를 확인한다
    그러면 "effect20-applied" assertion으로 "linearization-evidence effect20-applied"를 확인한다
    그러면 "revoke-after-applied" assertion으로 "linearization-evidence revoke-after-applied"를 확인한다
    그러면 "post-revoke-code" assertion으로 "effect-after-serialized-revoke post-revoke-code"를 확인한다
    그러면 "post-revoke-outcome" assertion으로 "effect-after-serialized-revoke post-revoke-outcome"를 확인한다
    그러면 "preserved20-quantity" assertion으로 "preserved-if-effect-commits-first preserved20-quantity"를 확인한다
    그러면 "effect-after-serialized-revoke-rows0" assertion으로 "effect-after-serialized-revoke effect-after-serialized-revoke-rows0"를 확인한다
    그러면 "dispatch-terminal-before-revoke" assertion으로 "linearization-evidence dispatch-terminal-before-revoke"를 확인한다
    그러면 "linearization-order" assertion으로 "linearization-evidence linearization-order"를 확인한다
    그러면 "blocked-or-post-revoke-duty-assignment1" assertion으로 "blocked-or-post-revoke-duty blocked-or-post-revoke-duty-assignment1"를 확인한다
    그러면 "blocked-or-post-revoke-duty-owner" assertion으로 "blocked-or-post-revoke-duty blocked-or-post-revoke-duty-owner"를 확인한다
    그러면 "blocked-or-post-revoke-duty-assignment-scope" assertion으로 "blocked-or-post-revoke-duty blocked-or-post-revoke-duty-assignment-scope"를 확인한다
    그러면 "revoked-current-grant" assertion으로 "linearization-evidence revoked-current-grant"를 확인한다
    그러면 "common-scope-fence" assertion으로 "linearization-evidence common-scope-fence"를 확인한다

  @sit
  시나리오: 현재 grant 철회와 출고 commit의 실제 순서를 대조한다: restart-after-revoke
    먼저 사례 파일 "verification/cases/V7/case.json"의 "restart-after-revoke"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "prior-dispatch" 행동을 수행한다
    만일 "delegator" 역할이 "prior-committed-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "prior-committed" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-start" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-ready" 행동을 수행한다
    만일 "delegator" 역할이 "revoke" 행동을 수행한다
    만일 "시스템" 역할이 "cancel-old-handle" 행동을 수행한다
    만일 "시스템" 역할이 "dispatch-result" 행동을 수행한다
    만일 "시스템" 역할이 "restart-application" 행동을 수행한다
    만일 "시스템" 역할이 "restart-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "restart-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "restart-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "scheduler-tick" 행동을 수행한다
    만일 "시스템" 역할이 "runtime-claim" 행동을 수행한다
    만일 "시스템" 역할이 "runtime-terminal" 행동을 수행한다
    만일 "delegator" 역할이 "safe-retry" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "prior20-applied" assertion으로 "safe-retry prior20-applied"를 확인한다
    그러면 "dispatch-ready-point" assertion으로 "safe-retry dispatch-ready-point"를 확인한다
    그러면 "dispatch-ready-state" assertion으로 "safe-retry dispatch-ready-state"를 확인한다
    그러면 "revoke-committed" assertion으로 "safe-retry revoke-committed"를 확인한다
    그러면 "dispatch-result-completed" assertion으로 "safe-retry dispatch-result-completed"를 확인한다
    그러면 "dispatch-result-same-handle" assertion으로 "safe-retry dispatch-result-same-handle"를 확인한다
    그러면 "old-worker-cancelled" assertion으로 "safe-retry old-worker-cancelled"를 확인한다
    그러면 "safe-retry-code" assertion으로 "safe-retry safe-retry-code"를 확인한다
    그러면 "safe-retry-outcome" assertion으로 "safe-retry safe-retry-outcome"를 확인한다
    그러면 "safe-retry-server-recorded" assertion으로 "서버 감사 원행에서 safe retry 실행 주체는 인증된 delegator, 행동은 retrySafeCommand, 결과는 REJECTED다. 요청은 commandId·사유·claim fencing token만 보내며 원 actor·hash·멱등키를 payload로 주지 않는다(계획 §7.2)."를 확인한다
    그러면 "safe-retry-original-owner-kept" assertion으로 "원 command record new20의 stableRequestOwner는 서버가 기록한 warehouse 그대로다. retry 요청자가 원 주체를 바꾸지 못한다(계획 §7.3)."를 확인한다
    그러면 "new-dispatch-rows0" assertion으로 "safe-retry new-dispatch-rows0"를 확인한다
    그러면 "new20-command-not-committed" assertion으로 "safe-retry new20-command-not-committed"를 확인한다
    그러면 "new-effect-quantity0" assertion으로 "unauthorized-recovered-effects new-effect-quantity0"를 확인한다
    그러면 "prior20-raw-preserved" assertion으로 "safe-retry prior20-raw-preserved"를 확인한다
    그러면 "prior20-still-total" assertion으로 "prior-committed-preserved prior20-still-total"를 확인한다
    그러면 "blocked-duty-after-restart-assignment1" assertion으로 "blocked-duty-after-restart blocked-duty-after-restart-assignment1"를 확인한다
    그러면 "blocked-duty-after-restart-owner" assertion으로 "blocked-duty-after-restart blocked-duty-after-restart-owner"를 확인한다
    그러면 "blocked-duty-after-restart-assignment-scope" assertion으로 "blocked-duty-after-restart blocked-duty-after-restart-assignment-scope"를 확인한다
    그러면 "revoked-current-grant" assertion으로 "safe-retry revoked-current-grant"를 확인한다
    그러면 "common-scope-fence" assertion으로 "safe-retry common-scope-fence"를 확인한다
