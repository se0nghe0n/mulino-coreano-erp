# language: ko
@V6 @D13 @D16 @D22
기능: 수령60의 응답 유실과 재시도에서 한 번의 효과를 보존한다

  @sit
  시나리오: 수령60 재시도는 응답 유실/rollback 뒤에도 확정 효과 한 번이다: lost-response-token-rpc-concurrent
    먼저 사례 파일 "verification/cases/V6/case.json"의 "lost-response-token-rpc-concurrent"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "inject-fault" 행동을 수행한다
    만일 "warehouse" 역할이 "first" 행동을 수행한다
    만일 "delegator" 역할이 "after-first-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-first" 행동을 수행한다
    만일 "시스템" 역할이 "refresh-token" 행동을 수행한다
    만일 "시스템" 역할이 "retry-a" 행동을 수행한다
    만일 "시스템" 역할이 "retry-a-ready" 행동을 수행한다
    만일 "시스템" 역할이 "retry-b" 행동을 수행한다
    만일 "시스템" 역할이 "retry-b-ready" 행동을 수행한다
    만일 "시스템" 역할이 "resume-a" 행동을 수행한다
    만일 "시스템" 역할이 "resume-b" 행동을 수행한다
    만일 "시스템" 역할이 "result-a" 행동을 수행한다
    만일 "시스템" 역할이 "result-b" 행동을 수행한다
    만일 "warehouse" 역할이 "replay" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "response-lost-transport" assertion으로 "retry-result response-lost-transport"를 확인한다
    그러면 "retry-a-ready-point" assertion으로 "retry-result retry-a-ready-point"를 확인한다
    그러면 "retry-a-ready-state" assertion으로 "retry-result retry-a-ready-state"를 확인한다
    그러면 "retry-b-ready-point" assertion으로 "retry-result retry-b-ready-point"를 확인한다
    그러면 "retry-b-ready-state" assertion으로 "retry-result retry-b-ready-state"를 확인한다
    그러면 "resume-a-point" assertion으로 "retry-result resume-a-point"를 확인한다
    그러면 "resume-a-state" assertion으로 "retry-result resume-a-state"를 확인한다
    그러면 "resume-b-point" assertion으로 "retry-result resume-b-point"를 확인한다
    그러면 "resume-b-state" assertion으로 "retry-result resume-b-state"를 확인한다
    그러면 "receipt-physical-total" assertion으로 "physical-receipt-total receipt-physical-total"를 확인한다
    그러면 "one-movements-K-60" assertion으로 "committed-effect-count one-movements-K-60"를 확인한다
    그러면 "one-commands-K-60" assertion으로 "committed-effect-count one-commands-K-60"를 확인한다
    그러면 "receipt-movement" assertion으로 "committed-effect-count receipt-movement"를 확인한다
    그러면 "receipt-result-transaction" assertion으로 "retry-result receipt-result-transaction"를 확인한다
    그러면 "receipt-audit-transaction" assertion으로 "retry-result receipt-audit-transaction"를 확인한다
    그러면 "receipt-owner-one" assertion으로 "committed-effect-count receipt-owner-one"를 확인한다
    그러면 "one-receipt-followup" assertion으로 "committed-effect-count one-receipt-followup"를 확인한다
    그러면 "result-a-completed" assertion으로 "retry-result result-a-completed"를 확인한다
    그러면 "result-a-same-handle" assertion으로 "retry-result result-a-same-handle"를 확인한다
    그러면 "result-a-receipt" assertion으로 "retry-result result-a-receipt"를 확인한다
    그러면 "result-a-command" assertion으로 "retry-result result-a-command"를 확인한다
    그러면 "result-a-applied" assertion으로 "retry-result result-a-applied"를 확인한다
    그러면 "result-b-completed" assertion으로 "retry-result result-b-completed"를 확인한다
    그러면 "result-b-same-handle" assertion으로 "retry-result result-b-same-handle"를 확인한다
    그러면 "result-b-receipt" assertion으로 "retry-result result-b-receipt"를 확인한다
    그러면 "result-b-command" assertion으로 "retry-result result-b-command"를 확인한다
    그러면 "result-b-applied" assertion으로 "retry-result result-b-applied"를 확인한다
    그러면 "stable-owner-preserved" assertion으로 "retry-result stable-owner-preserved"를 확인한다
    그러면 "definition-preserved" assertion으로 "retry-result definition-preserved"를 확인한다

  @sit
  시나리오: 수령60 재시도는 응답 유실/rollback 뒤에도 확정 효과 한 번이다: rollback-in-progress
    먼저 사례 파일 "verification/cases/V6/case.json"의 "rollback-in-progress"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "inject-fault" 행동을 수행한다
    만일 "warehouse" 역할이 "first" 행동을 수행한다
    만일 "delegator" 역할이 "after-first-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-first" 행동을 수행한다
    만일 "시스템" 역할이 "refresh-token" 행동을 수행한다
    만일 "시스템" 역할이 "retry-a" 행동을 수행한다
    만일 "시스템" 역할이 "retry-a-ready" 행동을 수행한다
    만일 "시스템" 역할이 "retry-b" 행동을 수행한다
    만일 "시스템" 역할이 "retry-b-ready" 행동을 수행한다
    만일 "시스템" 역할이 "resume-a" 행동을 수행한다
    만일 "시스템" 역할이 "resume-b" 행동을 수행한다
    만일 "시스템" 역할이 "result-a" 행동을 수행한다
    만일 "시스템" 역할이 "result-b" 행동을 수행한다
    만일 "warehouse" 역할이 "replay" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "rollback-movements" assertion으로 "retry-result rollback-movements"를 확인한다
    그러면 "rollback-segments" assertion으로 "retry-result rollback-segments"를 확인한다
    그러면 "rollback-outbox" assertion으로 "retry-result rollback-outbox"를 확인한다
    그러면 "rollback-not-committed" assertion으로 "retry-result rollback-not-committed"를 확인한다
    그러면 "retry-a-ready-point" assertion으로 "retry-result retry-a-ready-point"를 확인한다
    그러면 "retry-a-ready-state" assertion으로 "retry-result retry-a-ready-state"를 확인한다
    그러면 "retry-b-ready-point" assertion으로 "retry-result retry-b-ready-point"를 확인한다
    그러면 "retry-b-ready-state" assertion으로 "retry-result retry-b-ready-state"를 확인한다
    그러면 "resume-a-point" assertion으로 "retry-result resume-a-point"를 확인한다
    그러면 "resume-a-state" assertion으로 "retry-result resume-a-state"를 확인한다
    그러면 "resume-b-point" assertion으로 "retry-result resume-b-point"를 확인한다
    그러면 "resume-b-state" assertion으로 "retry-result resume-b-state"를 확인한다
    그러면 "receipt-physical-total" assertion으로 "physical-receipt-total receipt-physical-total"를 확인한다
    그러면 "one-movements-K-60" assertion으로 "committed-effect-count one-movements-K-60"를 확인한다
    그러면 "one-commands-K-60" assertion으로 "committed-effect-count one-commands-K-60"를 확인한다
    그러면 "receipt-movement" assertion으로 "committed-effect-count receipt-movement"를 확인한다
    그러면 "receipt-result-transaction" assertion으로 "retry-result receipt-result-transaction"를 확인한다
    그러면 "receipt-audit-transaction" assertion으로 "retry-result receipt-audit-transaction"를 확인한다
    그러면 "receipt-owner-one" assertion으로 "committed-effect-count receipt-owner-one"를 확인한다
    그러면 "one-receipt-followup" assertion으로 "committed-effect-count one-receipt-followup"를 확인한다
    그러면 "result-a-completed" assertion으로 "retry-result result-a-completed"를 확인한다
    그러면 "result-a-same-handle" assertion으로 "retry-result result-a-same-handle"를 확인한다
    그러면 "result-a-receipt" assertion으로 "retry-result result-a-receipt"를 확인한다
    그러면 "result-a-command" assertion으로 "retry-result result-a-command"를 확인한다
    그러면 "result-a-applied" assertion으로 "retry-result result-a-applied"를 확인한다
    그러면 "result-b-completed" assertion으로 "retry-result result-b-completed"를 확인한다
    그러면 "result-b-same-handle" assertion으로 "retry-result result-b-same-handle"를 확인한다
    그러면 "result-b-receipt" assertion으로 "retry-result result-b-receipt"를 확인한다
    그러면 "result-b-command" assertion으로 "retry-result result-b-command"를 확인한다
    그러면 "result-b-applied" assertion으로 "retry-result result-b-applied"를 확인한다
    그러면 "stable-owner-preserved" assertion으로 "retry-result stable-owner-preserved"를 확인한다
    그러면 "definition-preserved" assertion으로 "retry-result definition-preserved"를 확인한다

  @sit
  시나리오: 멱등 namespace·현재 조회 권한·새 canonical key를 구별한다: same-key40-conflict
    먼저 사례 파일 "verification/cases/V6/case.json"의 "same-key40-conflict"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "initial" 행동을 수행한다
    만일 "delegator" 역할이 "committed-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "committed" 행동을 수행한다
    만일 "warehouse" 역할이 "conflict" 행동을 수행한다
    만일 "warehouse" 역할이 "replay" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "conflict-code" assertion으로 "different-payload conflict-code"를 확인한다
    그러면 "conflict-outcome" assertion으로 "different-payload conflict-outcome"를 확인한다
    그러면 "conflict-keeps-raw-effects" assertion으로 "conflict-new-effects conflict-keeps-raw-effects"를 확인한다
    그러면 "unique-namespace-owner" assertion으로 "namespaces unique-namespace-owner"를 확인한다
    그러면 "unique-command-namespace-columns" assertion으로 "namespaces unique-command-namespace-columns"를 확인한다

  @sit
  시나리오: 멱등 namespace·현재 조회 권한·새 canonical key를 구별한다: other-owner-no-result
    먼저 사례 파일 "verification/cases/V6/case.json"의 "other-owner-no-result"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "initial" 행동을 수행한다
    만일 "delegator" 역할이 "committed-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "committed" 행동을 수행한다
    만일 "other" 역할이 "leak" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "leak-code" assertion으로 "other-owner-result-leak leak-code"를 확인한다
    그러면 "leak-outcome" assertion으로 "other-owner-result-leak leak-outcome"를 확인한다
    그러면 "result-not-visible" assertion으로 "other-owner-result-leak result-not-visible"를 확인한다
    그러면 "leak-no-identity-error-details" assertion으로 "other-owner-result-leak leak-no-identity-error-details"를 확인한다
    그러면 "other-owner-did-not-write" assertion으로 "conflict-new-effects other-owner-did-not-write"를 확인한다

  @sit
  시나리오: 멱등 namespace·현재 조회 권한·새 canonical key를 구별한다: replay-after-read-revocation
    먼저 사례 파일 "verification/cases/V6/case.json"의 "replay-after-read-revocation"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "initial" 행동을 수행한다
    만일 "delegator" 역할이 "committed-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "committed" 행동을 수행한다
    만일 "delegator" 역할이 "revoke" 행동을 수행한다
    만일 "warehouse" 역할이 "leak" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "leak-code" assertion으로 "namespaces leak-code"를 확인한다
    그러면 "leak-outcome" assertion으로 "namespaces leak-outcome"를 확인한다
    그러면 "no-replayed-result" assertion으로 "other-owner-result-leak no-replayed-result"를 확인한다
    그러면 "prior-receipt-unchanged" assertion으로 "conflict-new-effects prior-receipt-unchanged"를 확인한다

  @sit
  시나리오: 멱등 namespace·현재 조회 권한·새 canonical key를 구별한다: distinct-po-distinct-keys
    먼저 사례 파일 "verification/cases/V6/case.json"의 "distinct-po-distinct-keys"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "initial" 행동을 수행한다
    만일 "delegator" 역할이 "committed-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "committed" 행동을 수행한다
    만일 "warehouse" 역할이 "po-a" 행동을 수행한다
    만일 "warehouse" 역할이 "po-b" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "po-a-replay-preserves-effect" assertion으로 "namespaces po-a-replay-preserves-effect"를 확인한다
    그러면 "po-b-applied" assertion으로 "namespaces po-b-applied"를 확인한다
    그러면 "distinct-order-keys" assertion으로 "namespaces distinct-order-keys"를 확인한다
    그러면 "distinct-orders-physical" assertion으로 "namespaces distinct-orders-physical"를 확인한다

  @sit
  시나리오: 멱등 namespace·현재 조회 권한·새 canonical key를 구별한다: rejected-new-key
    먼저 사례 파일 "verification/cases/V6/case.json"의 "rejected-new-key"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "initial" 행동을 수행한다
    만일 "delegator" 역할이 "committed-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "committed" 행동을 수행한다
    만일 "warehouse" 역할이 "bad" 행동을 수행한다
    만일 "warehouse" 역할이 "same-rejected-key" 행동을 수행한다
    만일 "warehouse" 역할이 "new-request" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "bad-code" assertion으로 "namespaces bad-code"를 확인한다
    그러면 "bad-outcome" assertion으로 "namespaces bad-outcome"를 확인한다
    그러면 "same-rejected-key-code" assertion으로 "different-payload same-rejected-key-code"를 확인한다
    그러면 "same-rejected-key-outcome" assertion으로 "different-payload same-rejected-key-outcome"를 확인한다
    그러면 "new-key-valid-dedup" assertion으로 "namespaces new-key-valid-dedup"를 확인한다
    그러면 "rejected-original-kept" assertion으로 "namespaces rejected-original-kept"를 확인한다
    그러면 "new-key-same-canonical-physical" assertion으로 "conflict-new-effects new-key-same-canonical-physical"를 확인한다

  @sit
  시나리오: 멱등 namespace·현재 조회 권한·새 canonical key를 구별한다: retained-tombstone
    먼저 사례 파일 "verification/cases/V6/case.json"의 "retained-tombstone"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "initial" 행동을 수행한다
    만일 "delegator" 역할이 "committed-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "committed" 행동을 수행한다
    만일 "시스템" 역할이 "advance-time" 행동을 수행한다
    만일 "시스템" 역할이 "effect-record-retention" 행동을 수행한다
    만일 "delegator" 역할이 "retention-request" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "tombstone-kept-until" assertion으로 "namespaces tombstone-kept-until"를 확인한다
    그러면 "tombstone-references-prior-effect" assertion으로 "namespaces tombstone-references-prior-effect"를 확인한다

  @sit
  시나리오: 입력 수집 중 목적지 보완은 새 효과 key 충돌이 아니다
    먼저 사례 파일 "verification/cases/V6/case.json"의 "destination-input-supplement"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "input-missing" 행동을 수행한다
    만일 "warehouse" 역할이 "input-complete" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "needs-input" assertion으로 "completed-intent needs-input"를 확인한다
    그러면 "structured-ready" assertion으로 "draft-supplement-idempotency-conflict structured-ready"를 확인한다
    그러면 "same-conversation" assertion으로 "completed-intent same-conversation"를 확인한다
    그러면 "canonical-proposal-revision" assertion으로 "completed-intent canonical-proposal-revision"를 확인한다
    그러면 "validated-destination" assertion으로 "completed-intent validated-destination"를 확인한다
    그러면 "no-effect-key-during-input" assertion으로 "completed-intent no-effect-key-during-input"를 확인한다
    그러면 "unchanged-segments" assertion으로 "draft-supplement-physical-effects unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "draft-supplement-physical-effects unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "draft-supplement-physical-effects unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "draft-supplement-physical-effects unchanged-approvals"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "draft-supplement-physical-effects unchanged-outbox"를 확인한다
    그러면 "draft-supplement-idempotency-conflict-rows0" assertion으로 "draft-supplement-idempotency-conflict draft-supplement-idempotency-conflict-rows0"를 확인한다
    그러면 "canonical-hash-row" assertion으로 "completed-intent canonical-hash-row"를 확인한다
