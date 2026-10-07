# language: ko
@T08 @D08
기능: 조직 경계와 현재 위임 권한을 분리해 검증한다

  @sit
  시나리오: 다른 조직 direct-id 경로에서 존재 정보와 효과를 차단한다
    먼저 사례 파일 "verification/cases/T08/case.json"의 "direct-id"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "attack" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "delegator" 역할이 "own-read" 행동을 수행한다
    그러면 "attack-code" assertion으로 "cross-org-effects attack-code"를 확인한다
    그러면 "attack-outcome" assertion으로 "cross-org-effects attack-outcome"를 확인한다
    그러면 "no-foreign-object" assertion으로 "cross-org-identity-leak no-foreign-object"를 확인한다
    그러면 "no-identity-error-details" assertion으로 "cross-org-identity-leak no-identity-error-details"를 확인한다
    그러면 "unchanged-segments" assertion으로 "cross-org-effects unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "cross-org-effects unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "cross-org-effects unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "cross-org-effects unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "cross-org-effects unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "cross-org-effects unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "cross-org-effects unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "cross-org-effects unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "cross-org-effects unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "cross-org-effects unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "cross-org-effects unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "cross-org-effects unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "cross-org-effects unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "cross-org-effects unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "cross-org-effects unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "cross-org-effects unchanged-relations"를 확인한다
    그러면 "opaque-bound-id" assertion으로 "fk-scope opaque-bound-id"를 확인한다
    그러면 "own-org-id" assertion으로 "fk-scope own-org-id"를 확인한다

  @sit
  시나리오: 다른 조직 search 경로에서 존재 정보와 효과를 차단한다
    먼저 사례 파일 "verification/cases/T08/case.json"의 "search"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "attack" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "delegator" 역할이 "own-read" 행동을 수행한다
    그러면 "foreign-search-empty" assertion으로 "cross-org-identity-leak foreign-search-empty"를 확인한다
    그러면 "unchanged-segments" assertion으로 "cross-org-effects unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "cross-org-effects unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "cross-org-effects unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "cross-org-effects unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "cross-org-effects unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "cross-org-effects unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "cross-org-effects unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "cross-org-effects unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "cross-org-effects unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "cross-org-effects unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "cross-org-effects unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "cross-org-effects unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "cross-org-effects unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "cross-org-effects unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "cross-org-effects unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "cross-org-effects unchanged-relations"를 확인한다
    그러면 "opaque-bound-id" assertion으로 "fk-scope opaque-bound-id"를 확인한다
    그러면 "own-org-id" assertion으로 "fk-scope own-org-id"를 확인한다

  @sit
  시나리오: 다른 조직 nested 경로에서 존재 정보와 효과를 차단한다
    먼저 사례 파일 "verification/cases/T08/case.json"의 "nested"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "attack" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "delegator" 역할이 "own-read" 행동을 수행한다
    그러면 "attack-code" assertion으로 "cross-org-effects attack-code"를 확인한다
    그러면 "attack-outcome" assertion으로 "cross-org-effects attack-outcome"를 확인한다
    그러면 "no-foreign-object" assertion으로 "cross-org-identity-leak no-foreign-object"를 확인한다
    그러면 "no-identity-error-details" assertion으로 "cross-org-identity-leak no-identity-error-details"를 확인한다
    그러면 "unchanged-segments" assertion으로 "cross-org-effects unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "cross-org-effects unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "cross-org-effects unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "cross-org-effects unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "cross-org-effects unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "cross-org-effects unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "cross-org-effects unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "cross-org-effects unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "cross-org-effects unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "cross-org-effects unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "cross-org-effects unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "cross-org-effects unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "cross-org-effects unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "cross-org-effects unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "cross-org-effects unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "cross-org-effects unchanged-relations"를 확인한다
    그러면 "opaque-bound-id" assertion으로 "fk-scope opaque-bound-id"를 확인한다
    그러면 "own-org-id" assertion으로 "fk-scope own-org-id"를 확인한다

  @sit
  시나리오: 다른 조직 fk 경로에서 존재 정보와 효과를 차단한다
    먼저 사례 파일 "verification/cases/T08/case.json"의 "fk"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "delegator" 역할이 "attack" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "delegator" 역할이 "own-read" 행동을 수행한다
    그러면 "attack-code" assertion으로 "cross-org-effects attack-code"를 확인한다
    그러면 "attack-outcome" assertion으로 "cross-org-effects attack-outcome"를 확인한다
    그러면 "no-foreign-object" assertion으로 "cross-org-identity-leak no-foreign-object"를 확인한다
    그러면 "no-identity-error-details" assertion으로 "cross-org-identity-leak no-identity-error-details"를 확인한다
    그러면 "unchanged-segments" assertion으로 "cross-org-effects unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "cross-org-effects unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "cross-org-effects unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "cross-org-effects unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "cross-org-effects unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "cross-org-effects unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "cross-org-effects unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "cross-org-effects unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "cross-org-effects unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "cross-org-effects unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "cross-org-effects unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "cross-org-effects unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "cross-org-effects unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "cross-org-effects unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "cross-org-effects unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "cross-org-effects unchanged-relations"를 확인한다
    그러면 "fk-scope-rows0" assertion으로 "fk-scope fk-scope-rows0"를 확인한다
    그러면 "organization-fk-catalog" assertion으로 "fk-scope organization-fk-catalog"를 확인한다
    그러면 "opaque-bound-id" assertion으로 "fk-scope opaque-bound-id"를 확인한다
    그러면 "own-org-id" assertion으로 "fk-scope own-org-id"를 확인한다

  @sit
  시나리오: 다른 조직 payload-org 경로에서 존재 정보와 효과를 차단한다
    먼저 사례 파일 "verification/cases/T08/case.json"의 "payload-org"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "attack" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "delegator" 역할이 "own-read" 행동을 수행한다
    그러면 "attack-code" assertion으로 "cross-org-effects attack-code"를 확인한다
    그러면 "attack-outcome" assertion으로 "cross-org-effects attack-outcome"를 확인한다
    그러면 "no-foreign-object" assertion으로 "cross-org-identity-leak no-foreign-object"를 확인한다
    그러면 "no-identity-error-details" assertion으로 "cross-org-identity-leak no-identity-error-details"를 확인한다
    그러면 "unchanged-segments" assertion으로 "cross-org-effects unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "cross-org-effects unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "cross-org-effects unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "cross-org-effects unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "cross-org-effects unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "cross-org-effects unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "cross-org-effects unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "cross-org-effects unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "cross-org-effects unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "cross-org-effects unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "cross-org-effects unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "cross-org-effects unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "cross-org-effects unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "cross-org-effects unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "cross-org-effects unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "cross-org-effects unchanged-relations"를 확인한다
    그러면 "opaque-bound-id" assertion으로 "fk-scope opaque-bound-id"를 확인한다
    그러면 "own-org-id" assertion으로 "fk-scope own-org-id"를 확인한다

  @sit
  시나리오: 다른 조직 issuer 경로에서 존재 정보와 효과를 차단한다
    먼저 사례 파일 "verification/cases/T08/case.json"의 "issuer"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "invalidIssuer" 역할이 "attack" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "delegator" 역할이 "own-read" 행동을 수행한다
    그러면 "attack-code" assertion으로 "invalid-auth attack-code"를 확인한다
    그러면 "attack-outcome" assertion으로 "invalid-auth attack-outcome"를 확인한다
    그러면 "no-foreign-object" assertion으로 "cross-org-identity-leak no-foreign-object"를 확인한다
    그러면 "no-identity-error-details" assertion으로 "cross-org-identity-leak no-identity-error-details"를 확인한다
    그러면 "unchanged-segments" assertion으로 "cross-org-effects unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "cross-org-effects unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "cross-org-effects unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "cross-org-effects unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "cross-org-effects unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "cross-org-effects unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "cross-org-effects unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "cross-org-effects unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "cross-org-effects unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "cross-org-effects unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "cross-org-effects unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "cross-org-effects unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "cross-org-effects unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "cross-org-effects unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "cross-org-effects unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "cross-org-effects unchanged-relations"를 확인한다
    그러면 "opaque-bound-id" assertion으로 "fk-scope opaque-bound-id"를 확인한다
    그러면 "own-org-id" assertion으로 "fk-scope own-org-id"를 확인한다

  @sit
  시나리오: 다른 조직 subject 경로에서 존재 정보와 효과를 차단한다
    먼저 사례 파일 "verification/cases/T08/case.json"의 "subject"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "invalidSubject" 역할이 "attack" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "delegator" 역할이 "own-read" 행동을 수행한다
    그러면 "attack-code" assertion으로 "invalid-auth attack-code"를 확인한다
    그러면 "attack-outcome" assertion으로 "invalid-auth attack-outcome"를 확인한다
    그러면 "no-foreign-object" assertion으로 "cross-org-identity-leak no-foreign-object"를 확인한다
    그러면 "no-identity-error-details" assertion으로 "cross-org-identity-leak no-identity-error-details"를 확인한다
    그러면 "unchanged-segments" assertion으로 "cross-org-effects unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "cross-org-effects unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "cross-org-effects unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "cross-org-effects unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "cross-org-effects unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "cross-org-effects unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "cross-org-effects unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "cross-org-effects unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "cross-org-effects unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "cross-org-effects unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "cross-org-effects unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "cross-org-effects unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "cross-org-effects unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "cross-org-effects unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "cross-org-effects unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "cross-org-effects unchanged-relations"를 확인한다
    그러면 "opaque-bound-id" assertion으로 "fk-scope opaque-bound-id"를 확인한다
    그러면 "own-org-id" assertion으로 "fk-scope own-org-id"를 확인한다

  @sit
  시나리오: 다른 조직 audience 경로에서 존재 정보와 효과를 차단한다
    먼저 사례 파일 "verification/cases/T08/case.json"의 "audience"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "invalidAudience" 역할이 "attack" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "delegator" 역할이 "own-read" 행동을 수행한다
    그러면 "attack-code" assertion으로 "invalid-auth attack-code"를 확인한다
    그러면 "attack-outcome" assertion으로 "invalid-auth attack-outcome"를 확인한다
    그러면 "no-foreign-object" assertion으로 "cross-org-identity-leak no-foreign-object"를 확인한다
    그러면 "no-identity-error-details" assertion으로 "cross-org-identity-leak no-identity-error-details"를 확인한다
    그러면 "unchanged-segments" assertion으로 "cross-org-effects unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "cross-org-effects unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "cross-org-effects unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "cross-org-effects unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "cross-org-effects unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "cross-org-effects unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "cross-org-effects unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "cross-org-effects unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "cross-org-effects unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "cross-org-effects unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "cross-org-effects unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "cross-org-effects unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "cross-org-effects unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "cross-org-effects unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "cross-org-effects unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "cross-org-effects unchanged-relations"를 확인한다
    그러면 "opaque-bound-id" assertion으로 "fk-scope opaque-bound-id"를 확인한다
    그러면 "own-org-id" assertion으로 "fk-scope own-org-id"를 확인한다

  @sit
  시나리오: 서버 인증·역할·현재 위임·정책의 교집합 write-role-read-grant
    먼저 사례 파일 "verification/cases/T08/case.json"의 "write-role-read-grant"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "attack" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "attack-code" assertion으로 "server-authority attack-code"를 확인한다
    그러면 "attack-outcome" assertion으로 "server-authority attack-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "grant-exceeding-write unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "grant-exceeding-write unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "grant-exceeding-write unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "grant-exceeding-write unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "grant-exceeding-write unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "grant-exceeding-write unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "grant-exceeding-write unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "grant-exceeding-write unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "grant-exceeding-write unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "grant-exceeding-write unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "grant-exceeding-write unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "grant-exceeding-write unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "grant-exceeding-write unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "grant-exceeding-write unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "grant-exceeding-write unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "grant-exceeding-write unchanged-relations"를 확인한다
    그러면 "role-or-payload-escalation-rows0" assertion으로 "role-or-payload-escalation role-or-payload-escalation-rows0"를 확인한다
    그러면 "verified-auth-actor" assertion으로 "server-authority verified-auth-actor"를 확인한다
    그러면 "verified-auth-org" assertion으로 "server-authority verified-auth-org"를 확인한다
    그러면 "verified-delegator" assertion으로 "server-authority verified-delegator"를 확인한다

  @sit
  시나리오: 서버 인증·역할·현재 위임·정책의 교집합 payload-actor
    먼저 사례 파일 "verification/cases/T08/case.json"의 "payload-actor"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "attack" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "attack-code" assertion으로 "server-authority attack-code"를 확인한다
    그러면 "attack-outcome" assertion으로 "server-authority attack-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "grant-exceeding-write unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "grant-exceeding-write unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "grant-exceeding-write unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "grant-exceeding-write unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "grant-exceeding-write unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "grant-exceeding-write unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "grant-exceeding-write unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "grant-exceeding-write unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "grant-exceeding-write unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "grant-exceeding-write unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "grant-exceeding-write unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "grant-exceeding-write unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "grant-exceeding-write unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "grant-exceeding-write unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "grant-exceeding-write unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "grant-exceeding-write unchanged-relations"를 확인한다
    그러면 "role-or-payload-escalation-rows0" assertion으로 "role-or-payload-escalation role-or-payload-escalation-rows0"를 확인한다
    그러면 "verified-auth-actor" assertion으로 "server-authority verified-auth-actor"를 확인한다
    그러면 "verified-auth-org" assertion으로 "server-authority verified-auth-org"를 확인한다
    그러면 "verified-delegator" assertion으로 "server-authority verified-delegator"를 확인한다

  @sit
  시나리오: 서버 인증·역할·현재 위임·정책의 교집합 document-admin
    먼저 사례 파일 "verification/cases/T08/case.json"의 "document-admin"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "attack" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "attack-code" assertion으로 "server-authority attack-code"를 확인한다
    그러면 "attack-outcome" assertion으로 "server-authority attack-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "grant-exceeding-write unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "grant-exceeding-write unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "grant-exceeding-write unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "grant-exceeding-write unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "grant-exceeding-write unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "grant-exceeding-write unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "grant-exceeding-write unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "grant-exceeding-write unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "grant-exceeding-write unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "grant-exceeding-write unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "grant-exceeding-write unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "grant-exceeding-write unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "grant-exceeding-write unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "grant-exceeding-write unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "grant-exceeding-write unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "grant-exceeding-write unchanged-relations"를 확인한다
    그러면 "role-or-payload-escalation-rows0" assertion으로 "role-or-payload-escalation role-or-payload-escalation-rows0"를 확인한다
    그러면 "verified-auth-actor" assertion으로 "server-authority verified-auth-actor"를 확인한다
    그러면 "verified-auth-org" assertion으로 "server-authority verified-auth-org"를 확인한다
    그러면 "verified-delegator" assertion으로 "server-authority verified-delegator"를 확인한다

  @sit
  시나리오: 서버 인증·역할·현재 위임·정책의 교집합 fde-business
    먼저 사례 파일 "verification/cases/T08/case.json"의 "fde-business"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "fde" 역할이 "attack" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "attack-code" assertion으로 "server-authority attack-code"를 확인한다
    그러면 "attack-outcome" assertion으로 "server-authority attack-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "grant-exceeding-write unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "grant-exceeding-write unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "grant-exceeding-write unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "grant-exceeding-write unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "grant-exceeding-write unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "grant-exceeding-write unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "grant-exceeding-write unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "grant-exceeding-write unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "grant-exceeding-write unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "grant-exceeding-write unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "grant-exceeding-write unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "grant-exceeding-write unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "grant-exceeding-write unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "grant-exceeding-write unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "grant-exceeding-write unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "grant-exceeding-write unchanged-relations"를 확인한다
    그러면 "role-or-payload-escalation-rows0" assertion으로 "role-or-payload-escalation role-or-payload-escalation-rows0"를 확인한다
    그러면 "verified-auth-actor" assertion으로 "server-authority verified-auth-actor"를 확인한다
    그러면 "verified-auth-org" assertion으로 "server-authority verified-auth-org"를 확인한다
    그러면 "verified-delegator" assertion으로 "server-authority verified-delegator"를 확인한다

  @sit
  시나리오: 서버 인증·역할·현재 위임·정책의 교집합 admin-business
    먼저 사례 파일 "verification/cases/T08/case.json"의 "admin-business"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "admin" 역할이 "attack" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "attack-code" assertion으로 "server-authority attack-code"를 확인한다
    그러면 "attack-outcome" assertion으로 "server-authority attack-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "grant-exceeding-write unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "grant-exceeding-write unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "grant-exceeding-write unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "grant-exceeding-write unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "grant-exceeding-write unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "grant-exceeding-write unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "grant-exceeding-write unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "grant-exceeding-write unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "grant-exceeding-write unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "grant-exceeding-write unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "grant-exceeding-write unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "grant-exceeding-write unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "grant-exceeding-write unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "grant-exceeding-write unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "grant-exceeding-write unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "grant-exceeding-write unchanged-relations"를 확인한다
    그러면 "role-or-payload-escalation-rows0" assertion으로 "role-or-payload-escalation role-or-payload-escalation-rows0"를 확인한다
    그러면 "verified-auth-actor" assertion으로 "server-authority verified-auth-actor"를 확인한다
    그러면 "verified-auth-org" assertion으로 "server-authority verified-auth-org"를 확인한다
    그러면 "verified-delegator" assertion으로 "server-authority verified-delegator"를 확인한다

  @sit
  시나리오: 서버 인증·역할·현재 위임·정책의 교집합 unresolved-policy
    먼저 사례 파일 "verification/cases/T08/case.json"의 "unresolved-policy"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "warehouse" 역할이 "attack" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "attack-code" assertion으로 "server-authority attack-code"를 확인한다
    그러면 "attack-outcome" assertion으로 "server-authority attack-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "grant-exceeding-write unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "grant-exceeding-write unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "grant-exceeding-write unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "grant-exceeding-write unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "grant-exceeding-write unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "grant-exceeding-write unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "grant-exceeding-write unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "grant-exceeding-write unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "grant-exceeding-write unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "grant-exceeding-write unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "grant-exceeding-write unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "grant-exceeding-write unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "grant-exceeding-write unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "grant-exceeding-write unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "grant-exceeding-write unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "grant-exceeding-write unchanged-relations"를 확인한다
    그러면 "role-or-payload-escalation-rows0" assertion으로 "role-or-payload-escalation role-or-payload-escalation-rows0"를 확인한다
    그러면 "verified-auth-actor" assertion으로 "server-authority verified-auth-actor"를 확인한다
    그러면 "verified-auth-org" assertion으로 "server-authority verified-auth-org"를 확인한다
    그러면 "verified-delegator" assertion으로 "server-authority verified-delegator"를 확인한다

  @sit
  시나리오: grant와 identity 관리 범위를 검증한다: issue-within-scope
    먼저 사례 파일 "verification/cases/T08/case.json"의 "issue-within-scope"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "delegator" 역할이 "manage" 행동을 수행한다
    만일 "delegator" 역할이 "access" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "grant-applied" assertion으로 "grant-revocation grant-applied"를 확인한다
    그러면 "grant-exact-scope" assertion으로 "grant-revocation grant-exact-scope"를 확인한다
    그러면 "grant-actions-read" assertion으로 "self-amplification grant-actions-read"를 확인한다

  @sit
  시나리오: grant와 identity 관리 범위를 검증한다: delegate-outside-scope
    먼저 사례 파일 "verification/cases/T08/case.json"의 "delegate-outside-scope"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "limitedDelegator" 역할이 "manage" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "manage-code" assertion으로 "self-amplification manage-code"를 확인한다
    그러면 "manage-outcome" assertion으로 "self-amplification manage-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "self-amplification unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "self-amplification unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "self-amplification unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "self-amplification unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "self-amplification unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "self-amplification unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "self-amplification unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "self-amplification unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "self-amplification unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "self-amplification unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "self-amplification unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "self-amplification unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "self-amplification unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "self-amplification unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "self-amplification unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "self-amplification unchanged-relations"를 확인한다
    그러면 "identity-admin-only-rows0" assertion으로 "identity-admin-only identity-admin-only-rows0"를 확인한다

  @sit
  시나리오: grant와 identity 관리 범위를 검증한다: agent-self-amplify
    먼저 사례 파일 "verification/cases/T08/case.json"의 "agent-self-amplify"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "reader" 역할이 "manage" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "manage-code" assertion으로 "self-amplification manage-code"를 확인한다
    그러면 "manage-outcome" assertion으로 "self-amplification manage-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "self-amplification unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "self-amplification unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "self-amplification unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "self-amplification unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "self-amplification unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "self-amplification unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "self-amplification unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "self-amplification unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "self-amplification unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "self-amplification unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "self-amplification unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "self-amplification unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "self-amplification unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "self-amplification unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "self-amplification unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "self-amplification unchanged-relations"를 확인한다
    그러면 "identity-admin-only-rows0" assertion으로 "identity-admin-only identity-admin-only-rows0"를 확인한다

  @sit
  시나리오: grant와 identity 관리 범위를 검증한다: revoke-grant
    먼저 사례 파일 "verification/cases/T08/case.json"의 "revoke-grant"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "delegator" 역할이 "manage" 행동을 수행한다
    만일 "warehouse" 역할이 "access" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    만일 "warehouse" 역할이 "post-revoke" 행동을 수행한다
    그러면 "revocation-applied" assertion으로 "grant-revocation revocation-applied"를 확인한다
    그러면 "revoked-exact" assertion으로 "grant-revocation revoked-exact"를 확인한다
    그러면 "boundary-update" assertion으로 "grant-revocation boundary-update"를 확인한다
    그러면 "revoke-audit" assertion으로 "grant-revocation revoke-audit"를 확인한다
    그러면 "grant-revocation-assignment1" assertion으로 "grant-revocation grant-revocation-assignment1"를 확인한다
    그러면 "grant-revocation-owner" assertion으로 "grant-revocation grant-revocation-owner"를 확인한다
    그러면 "grant-revocation-assignment-scope" assertion으로 "grant-revocation grant-revocation-assignment-scope"를 확인한다
    그러면 "post-revoke-code" assertion으로 "grant-revocation post-revoke-code"를 확인한다
    그러면 "post-revoke-outcome" assertion으로 "grant-revocation post-revoke-outcome"를 확인한다
    그러면 "post-revoke-current-grant" assertion으로 "grant-revocation post-revoke-current-grant"를 확인한다

  @sit
  시나리오: grant와 identity 관리 범위를 검증한다: identity-assign
    먼저 사례 파일 "verification/cases/T08/case.json"의 "identity-assign"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "identityAdmin" 역할이 "manage" 행동을 수행한다
    만일 "identityAdmin" 역할이 "access" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "assignment-applied" assertion으로 "identity-admin-only assignment-applied"를 확인한다
    그러면 "assignment-actor-org" assertion으로 "grant-revocation assignment-actor-org"를 확인한다
    그러면 "same-transaction-audit" assertion으로 "grant-revocation same-transaction-audit"를 확인한다

  @sit
  시나리오: grant와 identity 관리 범위를 검증한다: identity-revoke
    먼저 사례 파일 "verification/cases/T08/case.json"의 "identity-revoke"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "identityAdmin" 역할이 "manage" 행동을 수행한다
    만일 "identityAdmin" 역할이 "access" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "assignment-applied" assertion으로 "identity-admin-only assignment-applied"를 확인한다
    그러면 "assignment-actor-org" assertion으로 "grant-revocation assignment-actor-org"를 확인한다
    그러면 "same-transaction-audit" assertion으로 "grant-revocation same-transaction-audit"를 확인한다

  @sit
  시나리오: grant와 identity 관리 범위를 검증한다: out-of-org-assign
    먼저 사례 파일 "verification/cases/T08/case.json"의 "out-of-org-assign"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "identityAdmin" 역할이 "manage" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "manage-code" assertion으로 "out-of-scope-assignment manage-code"를 확인한다
    그러면 "manage-outcome" assertion으로 "out-of-scope-assignment manage-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "out-of-scope-assignment unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "out-of-scope-assignment unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "out-of-scope-assignment unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "out-of-scope-assignment unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "out-of-scope-assignment unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "out-of-scope-assignment unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "out-of-scope-assignment unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "out-of-scope-assignment unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "out-of-scope-assignment unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "out-of-scope-assignment unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "out-of-scope-assignment unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "out-of-scope-assignment unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "out-of-scope-assignment unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "out-of-scope-assignment unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "out-of-scope-assignment unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "out-of-scope-assignment unchanged-relations"를 확인한다
    그러면 "identity-admin-only-rows0" assertion으로 "identity-admin-only identity-admin-only-rows0"를 확인한다

  @sit
  시나리오: grant와 identity 관리 범위를 검증한다: fde-assign
    먼저 사례 파일 "verification/cases/T08/case.json"의 "fde-assign"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "fde" 역할이 "manage" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "manage-code" assertion으로 "out-of-scope-assignment manage-code"를 확인한다
    그러면 "manage-outcome" assertion으로 "out-of-scope-assignment manage-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "out-of-scope-assignment unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "out-of-scope-assignment unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "out-of-scope-assignment unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "out-of-scope-assignment unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "out-of-scope-assignment unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "out-of-scope-assignment unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "out-of-scope-assignment unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "out-of-scope-assignment unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "out-of-scope-assignment unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "out-of-scope-assignment unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "out-of-scope-assignment unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "out-of-scope-assignment unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "out-of-scope-assignment unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "out-of-scope-assignment unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "out-of-scope-assignment unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "out-of-scope-assignment unchanged-relations"를 확인한다
    그러면 "identity-admin-only-rows0" assertion으로 "identity-admin-only identity-admin-only-rows0"를 확인한다

  @sit
  시나리오: grant와 identity 관리 범위를 검증한다: work-admin-assign
    먼저 사례 파일 "verification/cases/T08/case.json"의 "work-admin-assign"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "delegator" 역할이 "before-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before" 행동을 수행한다
    만일 "admin" 역할이 "manage" 행동을 수행한다
    만일 "delegator" 역할이 "after-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after" 행동을 수행한다
    그러면 "manage-code" assertion으로 "out-of-scope-assignment manage-code"를 확인한다
    그러면 "manage-outcome" assertion으로 "out-of-scope-assignment manage-outcome"를 확인한다
    그러면 "unchanged-segments" assertion으로 "out-of-scope-assignment unchanged-segments"를 확인한다
    그러면 "unchanged-movements" assertion으로 "out-of-scope-assignment unchanged-movements"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "out-of-scope-assignment unchanged-allocations"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "out-of-scope-assignment unchanged-approvals"를 확인한다
    그러면 "unchanged-works" assertion으로 "out-of-scope-assignment unchanged-works"를 확인한다
    그러면 "unchanged-goals" assertion으로 "out-of-scope-assignment unchanged-goals"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "out-of-scope-assignment unchanged-obligations"를 확인한다
    그러면 "unchanged-assignments" assertion으로 "out-of-scope-assignment unchanged-assignments"를 확인한다
    그러면 "unchanged-outbox" assertion으로 "out-of-scope-assignment unchanged-outbox"를 확인한다
    그러면 "unchanged-claims" assertion으로 "out-of-scope-assignment unchanged-claims"를 확인한다
    그러면 "unchanged-grants" assertion으로 "out-of-scope-assignment unchanged-grants"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "out-of-scope-assignment unchanged-capabilityAssignments"를 확인한다
    그러면 "unchanged-boundaries" assertion으로 "out-of-scope-assignment unchanged-boundaries"를 확인한다
    그러면 "unchanged-definitions" assertion으로 "out-of-scope-assignment unchanged-definitions"를 확인한다
    그러면 "unchanged-policies" assertion으로 "out-of-scope-assignment unchanged-policies"를 확인한다
    그러면 "unchanged-relations" assertion으로 "out-of-scope-assignment unchanged-relations"를 확인한다
    그러면 "identity-admin-only-rows0" assertion으로 "identity-admin-only identity-admin-only-rows0"를 확인한다
