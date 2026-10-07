# language: ko
@V1 @D21 @D09 @D17 @sit @uat
기능: 과거 정의 의미와 현재 정책을 함께 적용한다

  시나리오: v1 도착60 목표를 보존하고 v2 신규 의미·현재 QC 제한·정정58을 함께 검증한다
    먼저 사례 파일 "verification/cases/V1/case.json"의 "pinned-meaning-current-policy"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "fde" 역할이 "draft" 행동을 수행한다
    만일 "fde" 역할이 "validate" 행동을 수행한다
    만일 "fde" 역할이 "submit" 행동을 수행한다
    만일 "approver" 역할이 "approve" 행동을 수행한다
    만일 "fde" 역할이 "publish" 행동을 수행한다
    만일 "fde" 역할이 "activate" 행동을 수행한다
    만일 "owner" 역할이 "new-work" 행동을 수행한다
    만일 "owner" 역할이 "new-active" 행동을 수행한다
    만일 "qc" 역할이 "current-hold" 행동을 수행한다
    만일 "owner" 역할이 "old-assessment" 행동을 수행한다
    만일 "owner" 역할이 "new-assessment" 행동을 수행한다
    만일 "owner" 역할이 "current-eligibility" 행동을 수행한다
    만일 "steward" 역할이 "before-command" 행동을 수행한다
    만일 "시스템" 역할이 "db-before-command" 행동을 수행한다
    만일 "owner" 역할이 "try-v1-reserve" 행동을 수행한다
    만일 "steward" 역할이 "after-command" 행동을 수행한다
    만일 "시스템" 역할이 "db-after-command" 행동을 수행한다
    만일 "steward" 역할이 "receipt20-evidence" 행동을 수행한다
    만일 "steward" 역할이 "receipt20" 행동을 수행한다
    만일 "owner" 역할이 "arrival60" 행동을 수행한다
    만일 "steward" 역할이 "correct18" 행동을 수행한다
    만일 "owner" 역할이 "reassessment" 행동을 수행한다
    만일 "owner" 역할이 "policy-after-correction" 행동을 수행한다
    만일 "시스템" 역할이 "db-correction" 행동을 수행한다
    그러면 "old-endpoint" assertion으로 "v1-endpoint"를 확인한다
    그러면 "new-endpoint" assertion으로 "new-v2-endpoint"를 확인한다
    그러면 "old-definition" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "old-evaluator" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "sell-current-denied" assertion으로 "current-sell-permission"를 확인한다
    그러면 "v1-reserve-forbidden" assertion으로 "current-sell-permission"를 확인한다
    그러면 "v1-reserve-no-effects" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "unchanged-segments" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "unchanged-movements" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "db-old-work-pinned" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "db-old-goal-endpoint" assertion으로 "v1-endpoint"를 확인한다
    그러면 "db-new-goal-endpoint" assertion으로 "new-v2-endpoint"를 확인한다
    그러면 "db-current-qc-restriction" assertion으로 "current-sell-permission"를 확인한다
    그러면 "arrival60-result" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "corrected58-result" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "correction-definition-v1" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "correction-evaluator-v1" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "correction-still-arrived" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "current-qc-still-denied" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "corrected-receipt-58" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "immutable-assessment-history" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "db-current-arrival58" assertion으로 "old-assessment-semantics"를 확인한다
    그러면 "previous-assessment-linked" assertion으로 "old-assessment-semantics"를 확인한다

  시나리오: v1 handler/evaluator가 없으면 최신 의미로 대체하지 않고 인간 책임의 보류를 남긴다
    먼저 사례 파일 "verification/cases/V1/case.json"의 "unsupported-v1-held"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "owner" 역할이 "resume" 행동을 수행한다
    만일 "owner" 역할이 "assessment" 행동을 수행한다
    만일 "owner" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "owner" 역할이 "duties" 행동을 수행한다
    그러면 "unsupported-error" assertion으로 "unsupported"를 확인한다
    그러면 "unsupported-assessment-no-fallback" assertion으로 "no-silent-fallback"를 확인한다
    그러면 "unsupported-effects0" assertion으로 "unsupported-command-effects"를 확인한다
    그러면 "unchanged-segments" assertion으로 "unsupported-command-effects"를 확인한다
    그러면 "unchanged-movements" assertion으로 "unsupported-command-effects"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "unsupported-command-effects"를 확인한다
    그러면 "unchanged-goalVersions" assertion으로 "unsupported-command-effects"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "unsupported-command-effects"를 확인한다
    그러면 "work-held" assertion으로 "unsupported-duty"를 확인한다
    그러면 "current-duty-count" assertion으로 "unsupported-duty"를 확인한다
    그러면 "current-duty-owner" assertion으로 "unsupported-duty"를 확인한다
    그러면 "current-duty-action" assertion으로 "unsupported-duty"를 확인한다
    그러면 "current-duty-time" assertion으로 "unsupported-duty"를 확인한다
    그러면 "current-duty-state" assertion으로 "unsupported-duty"를 확인한다
    그러면 "duty-supervisor" assertion으로 "unsupported-duty"를 확인한다
    그러면 "api-duty-count" assertion으로 "unsupported-duty"를 확인한다
    그러면 "api-duty-details" assertion으로 "unsupported-duty"를 확인한다
    그러면 "old-work-no-fallback" assertion으로 "unsupported"를 확인한다
    그러면 "unsupported-assessment0" assertion으로 "unsupported"를 확인한다

  시나리오: skill hash 제시는 미지원v1의 실행 대응이나 올바른 해석의 증명이 아니다
    먼저 사례 파일 "verification/cases/V1/case.json"의 "skill-hash-no-runtime-proof"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "owner" 역할이 "claimed-skill" 행동을 수행한다
    만일 "owner" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "hash-cannot-prove-support" assertion으로 "skill-hash-substitutes-runtime-proof"를 확인한다
    그러면 "hash-effects0" assertion으로 "skill-hash-substitutes-runtime-proof"를 확인한다
    그러면 "unchanged-segments" assertion으로 "skill-hash-substitutes-runtime-proof"를 확인한다
    그러면 "unchanged-movements" assertion으로 "skill-hash-substitutes-runtime-proof"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "skill-hash-substitutes-runtime-proof"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "skill-hash-substitutes-runtime-proof"를 확인한다
    그러면 "unchanged-goalVersions" assertion으로 "skill-hash-substitutes-runtime-proof"를 확인한다
    그러면 "meaning-stays-v1" assertion으로 "skill-hash-substitutes-runtime-proof"를 확인한다
