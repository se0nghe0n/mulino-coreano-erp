# language: ko
@T07 @D07 @sit @uat
기능: typed 관계와 증거의 동일성을 검증한다

  시나리오: 잘못된 typed 입력 date-as-place을 거부한다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "date-as-place"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "steward" 역할이 "attempt" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "error-code" assertion으로 "invalid-types"를 확인한다
    그러면 "api-effects-empty" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-segments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-movements" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-fulfilments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-relations" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-works" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-runs" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-grants" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "invalid-effects"를 확인한다
    그러면 "denial-audit" assertion으로 "invalid-types"를 확인한다

  시나리오: 잘못된 typed 입력 wrong-endpoint을 거부한다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "wrong-endpoint"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "steward" 역할이 "attempt" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "error-code" assertion으로 "invalid-types"를 확인한다
    그러면 "api-effects-empty" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-segments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-movements" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-fulfilments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-relations" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-works" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-runs" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-grants" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "invalid-effects"를 확인한다
    그러면 "denial-audit" assertion으로 "invalid-types"를 확인한다

  시나리오: 잘못된 typed 입력 cardinality-overflow을 거부한다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "cardinality-overflow"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "steward" 역할이 "attempt" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "error-code" assertion으로 "invalid-types"를 확인한다
    그러면 "api-effects-empty" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-segments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-movements" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-fulfilments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-relations" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-works" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-runs" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-grants" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "invalid-effects"를 확인한다
    그러면 "denial-audit" assertion으로 "invalid-types"를 확인한다

  시나리오: 잘못된 typed 입력 unsupported-operator을 거부한다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "unsupported-operator"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "fde" 역할이 "attempt" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "error-code" assertion으로 "invalid-types"를 확인한다
    그러면 "api-effects-empty" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-segments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-movements" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-fulfilments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-relations" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-works" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-runs" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-grants" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "invalid-effects"를 확인한다
    그러면 "denial-audit" assertion으로 "invalid-types"를 확인한다

  시나리오: 잘못된 typed 입력 extension-eligible을 거부한다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "extension-eligible"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "steward" 역할이 "attempt" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "error-code" assertion으로 "invalid-types"를 확인한다
    그러면 "api-effects-empty" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-segments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-movements" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-fulfilments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-relations" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-works" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-runs" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-grants" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "invalid-effects"를 확인한다
    그러면 "denial-audit" assertion으로 "invalid-types"를 확인한다

  시나리오: 잘못된 typed 입력 extension-role을 거부한다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "extension-role"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "steward" 역할이 "attempt" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "error-code" assertion으로 "invalid-types"를 확인한다
    그러면 "api-effects-empty" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-segments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-movements" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-fulfilments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-relations" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-works" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-runs" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-capabilityAssignments" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-grants" assertion으로 "invalid-effects"를 확인한다
    그러면 "unchanged-approvals" assertion으로 "invalid-effects"를 확인한다
    그러면 "denial-audit" assertion으로 "invalid-types"를 확인한다

  시나리오: 유효한 위치 관계 등록은 Work나 Run을 만들지 않는다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "relation-without-work"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "steward" 역할이 "relation" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "relation-applied" assertion으로 "relation-created-work"를 확인한다
    그러면 "exact-typed-relation" assertion으로 "relation-created-work"를 확인한다
    그러면 "unchanged-works" assertion으로 "relation-created-work"를 확인한다
    그러면 "unchanged-runs" assertion으로 "relation-created-run"를 확인한다
    그러면 "unchanged-segments" assertion으로 "relation-created-work"를 확인한다
    그러면 "unchanged-movements" assertion으로 "relation-created-work"를 확인한다

  시나리오: all/any/not은 UNKNOWN과 CONFLICT를 거짓이나0으로 바꾸지 않는다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "predicate-truth-and-conflict"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "owner" 역할이 "draft-all-false-unknown" 행동을 수행한다
    만일 "owner" 역할이 "activate-all-false-unknown" 행동을 수행한다
    만일 "steward" 역할이 "all-false-unknown" 행동을 수행한다
    만일 "시스템" 역할이 "db-all-false-unknown" 행동을 수행한다
    만일 "owner" 역할이 "draft-all-true" 행동을 수행한다
    만일 "owner" 역할이 "activate-all-true" 행동을 수행한다
    만일 "steward" 역할이 "all-true" 행동을 수행한다
    만일 "시스템" 역할이 "db-all-true" 행동을 수행한다
    만일 "owner" 역할이 "draft-all-true-unknown" 행동을 수행한다
    만일 "owner" 역할이 "activate-all-true-unknown" 행동을 수행한다
    만일 "steward" 역할이 "all-true-unknown" 행동을 수행한다
    만일 "시스템" 역할이 "db-all-true-unknown" 행동을 수행한다
    만일 "owner" 역할이 "draft-any-true-unknown" 행동을 수행한다
    만일 "owner" 역할이 "activate-any-true-unknown" 행동을 수행한다
    만일 "steward" 역할이 "any-true-unknown" 행동을 수행한다
    만일 "시스템" 역할이 "db-any-true-unknown" 행동을 수행한다
    만일 "owner" 역할이 "draft-any-false" 행동을 수행한다
    만일 "owner" 역할이 "activate-any-false" 행동을 수행한다
    만일 "steward" 역할이 "any-false" 행동을 수행한다
    만일 "시스템" 역할이 "db-any-false" 행동을 수행한다
    만일 "owner" 역할이 "draft-any-false-unknown" 행동을 수행한다
    만일 "owner" 역할이 "activate-any-false-unknown" 행동을 수행한다
    만일 "steward" 역할이 "any-false-unknown" 행동을 수행한다
    만일 "시스템" 역할이 "db-any-false-unknown" 행동을 수행한다
    만일 "owner" 역할이 "draft-all-false-conflict" 행동을 수행한다
    만일 "owner" 역할이 "activate-all-false-conflict" 행동을 수행한다
    만일 "steward" 역할이 "all-false-conflict" 행동을 수행한다
    만일 "시스템" 역할이 "db-all-false-conflict" 행동을 수행한다
    만일 "owner" 역할이 "draft-any-true-conflict" 행동을 수행한다
    만일 "owner" 역할이 "activate-any-true-conflict" 행동을 수행한다
    만일 "steward" 역할이 "any-true-conflict" 행동을 수행한다
    만일 "시스템" 역할이 "db-any-true-conflict" 행동을 수행한다
    만일 "owner" 역할이 "draft-not-true" 행동을 수행한다
    만일 "owner" 역할이 "activate-not-true" 행동을 수행한다
    만일 "steward" 역할이 "not-true" 행동을 수행한다
    만일 "시스템" 역할이 "db-not-true" 행동을 수행한다
    만일 "owner" 역할이 "draft-not-false" 행동을 수행한다
    만일 "owner" 역할이 "activate-not-false" 행동을 수행한다
    만일 "steward" 역할이 "not-false" 행동을 수행한다
    만일 "시스템" 역할이 "db-not-false" 행동을 수행한다
    만일 "owner" 역할이 "draft-not-unknown" 행동을 수행한다
    만일 "owner" 역할이 "activate-not-unknown" 행동을 수행한다
    만일 "steward" 역할이 "not-unknown" 행동을 수행한다
    만일 "시스템" 역할이 "db-not-unknown" 행동을 수행한다
    만일 "owner" 역할이 "draft-not-conflict" 행동을 수행한다
    만일 "owner" 역할이 "activate-not-conflict" 행동을 수행한다
    만일 "steward" 역할이 "not-conflict" 행동을 수행한다
    만일 "시스템" 역할이 "db-not-conflict" 행동을 수행한다
    그러면 "all-false-unknown-api-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-false-unknown-api-conflict" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-false-unknown-db-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-false-unknown-input-state" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-false-unknown-input-count" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-true-api-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-true-api-conflict" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-true-db-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-true-input-state" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-true-input-count" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-true-unknown-api-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-true-unknown-api-conflict" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-true-unknown-db-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-true-unknown-input-state" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-true-unknown-input-count" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-true-unknown-api-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-true-unknown-api-conflict" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-true-unknown-db-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-true-unknown-input-state" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-true-unknown-input-count" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-false-api-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-false-api-conflict" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-false-db-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-false-input-state" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-false-input-count" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-false-unknown-api-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-false-unknown-api-conflict" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-false-unknown-db-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-false-unknown-input-state" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-false-unknown-input-count" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-false-conflict-api-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-false-conflict-api-conflict" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-false-conflict-db-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-false-conflict-input-state" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "all-false-conflict-input-count" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-true-conflict-api-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-true-conflict-api-conflict" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-true-conflict-db-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-true-conflict-input-state" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "any-true-conflict-input-count" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-true-api-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-true-api-conflict" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-true-db-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-true-input-state" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-true-input-count" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-false-api-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-false-api-conflict" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-false-db-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-false-input-state" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-false-input-count" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-unknown-api-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-unknown-api-conflict" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-unknown-db-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-unknown-input-state" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-unknown-input-count" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-conflict-api-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-conflict-api-conflict" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-conflict-db-result" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-conflict-input-state" assertion으로 "predicate-three-valued"를 확인한다
    그러면 "not-conflict-input-count" assertion으로 "predicate-three-valued"를 확인한다

  시나리오: 운송·창고 문서 두 장이 증명한 같은 실물60은 한 번 수령한다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "two-documents-one-receipt"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "steward" 역할이 "attach-carrier" 행동을 수행한다
    만일 "steward" 역할이 "attach-warehouse" 행동을 수행한다
    만일 "steward" 역할이 "link-carrier" 행동을 수행한다
    만일 "steward" 역할이 "link-warehouse" 행동을 수행한다
    만일 "steward" 역할이 "receipt" 행동을 수행한다
    만일 "steward" 역할이 "after-receipt" 행동을 수행한다
    만일 "시스템" 역할이 "db-receipt" 행동을 수행한다
    만일 "steward" 역할이 "same-receipt" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "api-held-60" assertion으로 "receipt-total"를 확인한다
    그러면 "db-held-60" assertion으로 "receipt-total"를 확인한다
    그러면 "document-count-2" assertion으로 "document-count"를 확인한다
    그러면 "canonical-count-1" assertion으로 "canonical-occurrence-count"를 확인한다
    그러면 "physical-identities-unique" assertion으로 "duplicate-physical-effects"를 확인한다
    그러면 "document-ids" assertion으로 "document-count"를 확인한다
    그러면 "evidence-role-links" assertion으로 "document-count"를 확인한다
    그러면 "same-occurrence-id" assertion으로 "canonical-occurrence-count"를 확인한다
    그러면 "duplicate-api-effects0" assertion으로 "duplicate-physical-effects"를 확인한다
    그러면 "unchanged-segments" assertion으로 "duplicate-physical-effects"를 확인한다
    그러면 "unchanged-movements" assertion으로 "duplicate-physical-effects"를 확인한다
    그러면 "unchanged-receiptContributions" assertion으로 "duplicate-physical-effects"를 확인한다

  시나리오: 증거 가용성 incomplete-upload을 확인하고 기여를 차단한다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "incomplete-upload"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "steward" 역할이 "attach" 행동을 수행한다
    만일 "steward" 역할이 "availability" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-receiptContributions" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "unchanged-movements" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "unchanged-segments" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "evidence-unverified" assertion으로 "unreadable-source"를 확인한다
    그러면 "usable-for-assessment0" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "availability-raw-state" assertion으로 "unreadable-source"를 확인한다

  시나리오: 증거 가용성 hash-mismatch을 확인하고 기여를 차단한다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "hash-mismatch"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "steward" 역할이 "attach" 행동을 수행한다
    만일 "steward" 역할이 "availability" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-receiptContributions" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "unchanged-movements" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "unchanged-segments" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "evidence-unverified" assertion으로 "unreadable-source"를 확인한다
    그러면 "usable-for-assessment0" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "availability-raw-state" assertion으로 "unreadable-source"를 확인한다

  시나리오: 증거 가용성 db-reference-failure을 확인하고 기여를 차단한다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "db-reference-failure"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "fault" 행동을 수행한다
    만일 "steward" 역할이 "attach" 행동을 수행한다
    만일 "steward" 역할이 "availability" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-receiptContributions" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "unchanged-movements" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "unchanged-segments" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "evidence-unverified" assertion으로 "unreadable-source"를 확인한다
    그러면 "usable-for-assessment0" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "availability-raw-state" assertion으로 "unreadable-source"를 확인한다
    그러면 "blob-repair-count" assertion으로 "blob-reconciliation"를 확인한다
    그러면 "blob-repair-owned" assertion으로 "blob-reconciliation"를 확인한다
    그러면 "blob-repair-action" assertion으로 "blob-reconciliation"를 확인한다

  시나리오: 증거 가용성 orphan-upload을 확인하고 기여를 차단한다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "orphan-upload"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "fault" 행동을 수행한다
    만일 "steward" 역할이 "attach" 행동을 수행한다
    만일 "steward" 역할이 "availability" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-receiptContributions" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "unchanged-movements" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "unchanged-segments" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "evidence-unverified" assertion으로 "unreadable-source"를 확인한다
    그러면 "usable-for-assessment0" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "availability-raw-state" assertion으로 "unreadable-source"를 확인한다
    그러면 "blob-repair-count" assertion으로 "blob-reconciliation"를 확인한다
    그러면 "blob-repair-owned" assertion으로 "blob-reconciliation"를 확인한다
    그러면 "blob-repair-action" assertion으로 "blob-reconciliation"를 확인한다

  시나리오: 증거 가용성 unreadable-uri을 확인하고 기여를 차단한다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "unreadable-uri"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "fault" 행동을 수행한다
    만일 "steward" 역할이 "attach" 행동을 수행한다
    만일 "steward" 역할이 "availability" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-receiptContributions" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "unchanged-movements" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "unchanged-segments" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "evidence-unverified" assertion으로 "unreadable-source"를 확인한다
    그러면 "usable-for-assessment0" assertion으로 "unavailable-evidence-contribution"를 확인한다
    그러면 "availability-raw-state" assertion으로 "unreadable-source"를 확인한다

  시나리오: 원문 bytes를 보존하고 서버 인가 후에만 다운로드한다
    먼저 사례 파일 "verification/cases/T07/case.json"의 "immutable-authorized-download"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "upload" 행동을 수행한다
    만일 "steward" 역할이 "original" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "steward" 역할이 "overwrite" 행동을 수행한다
    만일 "outsider" 역할이 "unauthorized" 행동을 수행한다
    만일 "steward" 역할이 "still-original" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "original-body" assertion으로 "blob-reconciliation"를 확인한다
    그러면 "original-hash" assertion으로 "blob-reconciliation"를 확인한다
    그러면 "post-overwrite-body" assertion으로 "blob-reconciliation"를 확인한다
    그러면 "post-overwrite-hash" assertion으로 "blob-reconciliation"를 확인한다
    그러면 "unauthorized-forbidden" assertion으로 "blob-reconciliation"를 확인한다
    그러면 "no-download-body" assertion으로 "blob-reconciliation"를 확인한다
    그러면 "no-signed-url" assertion으로 "blob-reconciliation"를 확인한다
    그러면 "unchanged-documents" assertion으로 "blob-reconciliation"를 확인한다
    그러면 "unchanged-evidenceLinks" assertion으로 "blob-reconciliation"를 확인한다
    그러면 "unchanged-segments" assertion으로 "blob-reconciliation"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "blob-reconciliation"를 확인한다
