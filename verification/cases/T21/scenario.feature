# language: ko
@T21 @D21 @sit @uat
기능: 불변 정의 발행과 명시 전환을 검증한다

  시나리오: draft 검토·CONFIG_APPROVER 승인·불변 발행·활성 포인터·신규 사용 종료를 검증한다
    먼저 사례 파일 "verification/cases/T21/case.json"의 "approved-lifecycle"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "fde" 역할이 "draft" 행동을 수행한다
    만일 "fde" 역할이 "validate" 행동을 수행한다
    만일 "fde" 역할이 "submit" 행동을 수행한다
    만일 "approver" 역할이 "approve" 행동을 수행한다
    만일 "fde" 역할이 "publish" 행동을 수행한다
    만일 "fde" 역할이 "activate" 행동을 수행한다
    만일 "fde" 역할이 "active-package" 행동을 수행한다
    만일 "시스템" 역할이 "db-active-package" 행동을 수행한다
    만일 "fde" 역할이 "retire" 행동을 수행한다
    만일 "fde" 역할이 "definition" 행동을 수행한다
    만일 "시스템" 역할이 "db-definition" 행동을 수행한다
    그러면 "active-pointer-deploys-reviewed-version" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "draft-state" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "submit-state" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "publish-state" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "activate-state" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "retire-state" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "config-approver-binding" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "published-hash-is-reviewed" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "activated-hash-is-published" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "published-version-content" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "no-new-execution-capability" assertion으로 "definition-generated-new-capability"를 확인한다

  시나리오: FDE 자기 승인을 차단하고 반려 뒤 새 draft revision을 만든다
    먼저 사례 파일 "verification/cases/T21/case.json"의 "approval-required-and-rejected-revision"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "fde" 역할이 "draft" 행동을 수행한다
    만일 "fde" 역할이 "validate" 행동을 수행한다
    만일 "fde" 역할이 "submit" 행동을 수행한다
    만일 "fde" 역할이 "self-approve" 행동을 수행한다
    만일 "fde" 역할이 "unapproved-publish" 행동을 수행한다
    만일 "approver" 역할이 "reject" 행동을 수행한다
    만일 "fde" 역할이 "revised" 행동을 수행한다
    만일 "fde" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "fde-no-approval" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "unapproved-no-publish" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "revised-state" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "new-revision" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "rejection-preserved" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "no-active-v2" assertion으로 "definition-lifecycle"를 확인한다

  시나리오: 발행 내용 수정 시도를 거부하고 ACTIVE 포인터만 바꾼다
    먼저 사례 파일 "verification/cases/T21/case.json"의 "published-content-immutable"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "fde" 역할이 "draft" 행동을 수행한다
    만일 "fde" 역할이 "validate" 행동을 수행한다
    만일 "fde" 역할이 "submit" 행동을 수행한다
    만일 "approver" 역할이 "approve" 행동을 수행한다
    만일 "fde" 역할이 "publish" 행동을 수행한다
    만일 "fde" 역할이 "activate" 행동을 수행한다
    만일 "fde" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "fde" 역할이 "mutate" 행동을 수행한다
    만일 "fde" 역할이 "prior-package" 행동을 수행한다
    만일 "fde" 역할이 "rollback-pointer" 행동을 수행한다
    만일 "fde" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "mutation-rejected" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "no-mutated-effects" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "unchanged-segments" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "unchanged-movements" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "immutable-package-content" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "immutable-package-hash" assertion으로 "definition-lifecycle"를 확인한다
    그러면 "only-active-pointer-v1" assertion으로 "definition-lifecycle"를 확인한다

  시나리오: 이름·선택속성·필수slot·단위·끝점·관계 의미 변경을 구별한다
    먼저 사례 파일 "verification/cases/T21/case.json"의 "six-impact-diff-classes"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "fde" 역할이 "draft-rename" 행동을 수행한다
    만일 "fde" 역할이 "validate-rename" 행동을 수행한다
    만일 "fde" 역할이 "read-rename" 행동을 수행한다
    만일 "시스템" 역할이 "db-rename" 행동을 수행한다
    만일 "fde" 역할이 "draft-optional-attribute" 행동을 수행한다
    만일 "fde" 역할이 "validate-optional-attribute" 행동을 수행한다
    만일 "fde" 역할이 "read-optional-attribute" 행동을 수행한다
    만일 "시스템" 역할이 "db-optional-attribute" 행동을 수행한다
    만일 "fde" 역할이 "draft-required-slot" 행동을 수행한다
    만일 "fde" 역할이 "validate-required-slot" 행동을 수행한다
    만일 "fde" 역할이 "read-required-slot" 행동을 수행한다
    만일 "시스템" 역할이 "db-required-slot" 행동을 수행한다
    만일 "fde" 역할이 "draft-unit" 행동을 수행한다
    만일 "fde" 역할이 "validate-unit" 행동을 수행한다
    만일 "fde" 역할이 "read-unit" 행동을 수행한다
    만일 "시스템" 역할이 "db-unit" 행동을 수행한다
    만일 "fde" 역할이 "draft-endpoint" 행동을 수행한다
    만일 "fde" 역할이 "validate-endpoint" 행동을 수행한다
    만일 "fde" 역할이 "read-endpoint" 행동을 수행한다
    만일 "시스템" 역할이 "db-endpoint" 행동을 수행한다
    만일 "fde" 역할이 "draft-relation-meaning" 행동을 수행한다
    만일 "fde" 역할이 "validate-relation-meaning" 행동을 수행한다
    만일 "fde" 역할이 "read-relation-meaning" 행동을 수행한다
    만일 "시스템" 역할이 "db-relation-meaning" 행동을 수행한다
    그러면 "rename-diff" assertion으로 "impact-validation"를 확인한다
    그러면 "rename-raw-diff" assertion으로 "impact-validation"를 확인한다
    그러면 "rename-support-manifest" assertion으로 "impact-validation"를 확인한다
    그러면 "rename-regression-complete" assertion으로 "impact-validation"를 확인한다
    그러면 "rename-regression-results" assertion으로 "impact-validation"를 확인한다
    그러면 "optional-attribute-diff" assertion으로 "impact-validation"를 확인한다
    그러면 "optional-attribute-raw-diff" assertion으로 "impact-validation"를 확인한다
    그러면 "optional-attribute-support-manifest" assertion으로 "impact-validation"를 확인한다
    그러면 "optional-attribute-regression-complete" assertion으로 "impact-validation"를 확인한다
    그러면 "optional-attribute-regression-results" assertion으로 "impact-validation"를 확인한다
    그러면 "required-slot-diff" assertion으로 "impact-validation"를 확인한다
    그러면 "required-slot-raw-diff" assertion으로 "impact-validation"를 확인한다
    그러면 "required-slot-support-manifest" assertion으로 "impact-validation"를 확인한다
    그러면 "required-slot-regression-complete" assertion으로 "impact-validation"를 확인한다
    그러면 "required-slot-regression-results" assertion으로 "impact-validation"를 확인한다
    그러면 "unit-diff" assertion으로 "impact-validation"를 확인한다
    그러면 "unit-raw-diff" assertion으로 "impact-validation"를 확인한다
    그러면 "unit-support-manifest" assertion으로 "impact-validation"를 확인한다
    그러면 "unit-regression-complete" assertion으로 "impact-validation"를 확인한다
    그러면 "unit-regression-results" assertion으로 "impact-validation"를 확인한다
    그러면 "endpoint-diff" assertion으로 "impact-validation"를 확인한다
    그러면 "endpoint-raw-diff" assertion으로 "impact-validation"를 확인한다
    그러면 "endpoint-support-manifest" assertion으로 "impact-validation"를 확인한다
    그러면 "endpoint-regression-complete" assertion으로 "impact-validation"를 확인한다
    그러면 "endpoint-regression-results" assertion으로 "impact-validation"를 확인한다
    그러면 "relation-meaning-diff" assertion으로 "impact-validation"를 확인한다
    그러면 "relation-meaning-raw-diff" assertion으로 "impact-validation"를 확인한다
    그러면 "relation-meaning-support-manifest" assertion으로 "impact-validation"를 확인한다
    그러면 "relation-meaning-regression-complete" assertion으로 "impact-validation"를 확인한다
    그러면 "relation-meaning-regression-results" assertion으로 "impact-validation"를 확인한다

  시나리오: 발행 검사는 타입·cardinality·단계·단위·순환·목표·조직·회귀·support를 검사한다
    먼저 사례 파일 "verification/cases/T21/case.json"의 "validation-constraints"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "fde" 역할이 "draft-types" 행동을 수행한다
    만일 "fde" 역할이 "validate-types" 행동을 수행한다
    만일 "fde" 역할이 "publish-types" 행동을 수행한다
    만일 "fde" 역할이 "draft-cardinality" 행동을 수행한다
    만일 "fde" 역할이 "validate-cardinality" 행동을 수행한다
    만일 "fde" 역할이 "publish-cardinality" 행동을 수행한다
    만일 "fde" 역할이 "draft-required-stage" 행동을 수행한다
    만일 "fde" 역할이 "validate-required-stage" 행동을 수행한다
    만일 "fde" 역할이 "publish-required-stage" 행동을 수행한다
    만일 "fde" 역할이 "draft-unit" 행동을 수행한다
    만일 "fde" 역할이 "validate-unit" 행동을 수행한다
    만일 "fde" 역할이 "publish-unit" 행동을 수행한다
    만일 "fde" 역할이 "draft-cycle" 행동을 수행한다
    만일 "fde" 역할이 "validate-cycle" 행동을 수행한다
    만일 "fde" 역할이 "publish-cycle" 행동을 수행한다
    만일 "fde" 역할이 "draft-goal" 행동을 수행한다
    만일 "fde" 역할이 "validate-goal" 행동을 수행한다
    만일 "fde" 역할이 "publish-goal" 행동을 수행한다
    만일 "fde" 역할이 "draft-organization" 행동을 수행한다
    만일 "fde" 역할이 "validate-organization" 행동을 수행한다
    만일 "fde" 역할이 "publish-organization" 행동을 수행한다
    만일 "fde" 역할이 "draft-regression" 행동을 수행한다
    만일 "fde" 역할이 "validate-regression" 행동을 수행한다
    만일 "fde" 역할이 "publish-regression" 행동을 수행한다
    만일 "fde" 역할이 "draft-manifest" 행동을 수행한다
    만일 "fde" 역할이 "validate-manifest" 행동을 수행한다
    만일 "fde" 역할이 "publish-manifest" 행동을 수행한다
    만일 "fde" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "types-error" assertion으로 "impact-validation"를 확인한다
    그러면 "types-cannot-publish" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "types-publish-rejected" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "types-publish-effects0" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "cardinality-error" assertion으로 "impact-validation"를 확인한다
    그러면 "cardinality-cannot-publish" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "cardinality-publish-rejected" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "cardinality-publish-effects0" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "required-stage-error" assertion으로 "impact-validation"를 확인한다
    그러면 "required-stage-cannot-publish" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "required-stage-publish-rejected" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "required-stage-publish-effects0" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "unit-error" assertion으로 "impact-validation"를 확인한다
    그러면 "unit-cannot-publish" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "unit-publish-rejected" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "unit-publish-effects0" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "cycle-error" assertion으로 "impact-validation"를 확인한다
    그러면 "cycle-cannot-publish" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "cycle-publish-rejected" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "cycle-publish-effects0" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "goal-error" assertion으로 "impact-validation"를 확인한다
    그러면 "goal-cannot-publish" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "goal-publish-rejected" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "goal-publish-effects0" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "organization-error" assertion으로 "impact-validation"를 확인한다
    그러면 "organization-cannot-publish" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "organization-publish-rejected" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "organization-publish-effects0" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "regression-error" assertion으로 "impact-validation"를 확인한다
    그러면 "regression-cannot-publish" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "regression-publish-rejected" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "regression-publish-effects0" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "manifest-error" assertion으로 "impact-validation"를 확인한다
    그러면 "manifest-cannot-publish" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "manifest-publish-rejected" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "manifest-publish-effects0" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "no-invalid-published" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "all-validation-findings" assertion으로 "impact-validation"를 확인한다

  시나리오: 정의에 SQL·script·새 실행 권한·미지원 operator를 넣어 발행하지 못한다
    먼저 사례 파일 "verification/cases/T21/case.json"의 "invalid-executable-payloads"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "fde" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "fde" 역할이 "attempt-sql" 행동을 수행한다
    만일 "fde" 역할이 "attempt-script" 행동을 수행한다
    만일 "fde" 역할이 "attempt-privilege" 행동을 수행한다
    만일 "fde" 역할이 "attempt-operator" 행동을 수행한다
    만일 "fde" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "sql-rejected" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "sql-effects0" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "script-rejected" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "script-effects0" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "privilege-rejected" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "privilege-effects0" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "operator-rejected" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "operator-effects0" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "unchanged-definitionPackages" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "unchanged-activeDefinitionPointers" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "invalid-payload-published"를 확인한다
    그러면 "unchanged-capabilities" assertion으로 "definition-generated-new-capability"를 확인한다

  시나리오: v1 도착60 목표를 보존하고 v2 신규 의미·현재 QC 제한·정정58을 함께 검증한다
    먼저 사례 파일 "verification/cases/T21/case.json"의 "pinned-meaning-current-policy"를 준비한다
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
    그러면 "old-endpoint" assertion으로 "old-work-endpoint"를 확인한다
    그러면 "new-endpoint" assertion으로 "new-work-endpoint"를 확인한다
    그러면 "old-definition" assertion으로 "v1-correction-reassessment"를 확인한다
    그러면 "old-evaluator" assertion으로 "v1-correction-reassessment"를 확인한다
    그러면 "sell-current-denied" assertion으로 "current-sell-policy"를 확인한다
    그러면 "v1-reserve-forbidden" assertion으로 "current-sell-policy"를 확인한다
    그러면 "v1-reserve-no-effects" assertion으로 "v1-used-to-bypass-current-policy"를 확인한다
    그러면 "unchanged-segments" assertion으로 "v1-used-to-bypass-current-policy"를 확인한다
    그러면 "unchanged-movements" assertion으로 "v1-used-to-bypass-current-policy"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "v1-used-to-bypass-current-policy"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "v1-used-to-bypass-current-policy"를 확인한다
    그러면 "db-old-work-pinned" assertion으로 "v1-correction-reassessment"를 확인한다
    그러면 "db-old-goal-endpoint" assertion으로 "old-work-endpoint"를 확인한다
    그러면 "db-new-goal-endpoint" assertion으로 "new-work-endpoint"를 확인한다
    그러면 "db-current-qc-restriction" assertion으로 "current-sell-policy"를 확인한다
    그러면 "arrival60-result" assertion으로 "v1-correction-reassessment"를 확인한다
    그러면 "corrected58-result" assertion으로 "v1-correction-reassessment"를 확인한다
    그러면 "correction-definition-v1" assertion으로 "v1-correction-reassessment"를 확인한다
    그러면 "correction-evaluator-v1" assertion으로 "v1-correction-reassessment"를 확인한다
    그러면 "correction-still-arrived" assertion으로 "v1-correction-reassessment"를 확인한다
    그러면 "current-qc-still-denied" assertion으로 "v1-correction-reassessment"를 확인한다
    그러면 "corrected-receipt-58" assertion으로 "v1-correction-reassessment"를 확인한다
    그러면 "immutable-assessment-history" assertion으로 "v1-correction-reassessment"를 확인한다
    그러면 "db-current-arrival58" assertion으로 "v1-correction-reassessment"를 확인한다
    그러면 "previous-assessment-linked" assertion으로 "v1-correction-reassessment"를 확인한다

  시나리오: v1 handler/evaluator가 없으면 최신 의미로 대체하지 않고 인간 책임의 보류를 남긴다
    먼저 사례 파일 "verification/cases/T21/case.json"의 "unsupported-v1-held"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "owner" 역할이 "resume" 행동을 수행한다
    만일 "owner" 역할이 "assessment" 행동을 수행한다
    만일 "owner" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "owner" 역할이 "duties" 행동을 수행한다
    그러면 "unsupported-error" assertion으로 "unsupported-result"를 확인한다
    그러면 "unsupported-assessment-no-fallback" assertion으로 "unsupported-version-duty"를 확인한다
    그러면 "unsupported-effects0" assertion으로 "unsupported-version-effects"를 확인한다
    그러면 "unchanged-segments" assertion으로 "unsupported-version-effects"를 확인한다
    그러면 "unchanged-movements" assertion으로 "unsupported-version-effects"를 확인한다
    그러면 "unchanged-allocations" assertion으로 "unsupported-version-effects"를 확인한다
    그러면 "unchanged-goalVersions" assertion으로 "unsupported-version-effects"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "unsupported-version-effects"를 확인한다
    그러면 "work-held" assertion으로 "unsupported-version-duty"를 확인한다
    그러면 "current-duty-count" assertion으로 "unsupported-version-duty"를 확인한다
    그러면 "current-duty-owner" assertion으로 "unsupported-version-duty"를 확인한다
    그러면 "current-duty-action" assertion으로 "unsupported-version-duty"를 확인한다
    그러면 "current-duty-time" assertion으로 "unsupported-version-duty"를 확인한다
    그러면 "current-duty-state" assertion으로 "unsupported-version-duty"를 확인한다
    그러면 "duty-supervisor" assertion으로 "unsupported-version-duty"를 확인한다
    그러면 "api-duty-count" assertion으로 "unsupported-version-duty"를 확인한다
    그러면 "api-duty-details" assertion으로 "unsupported-version-duty"를 확인한다
    그러면 "old-work-no-fallback" assertion으로 "unsupported-result"를 확인한다
    그러면 "unsupported-assessment0" assertion으로 "unsupported-result"를 확인한다

  시나리오: 수량60→80과 끝점 변경을 mapping·승인·새 GoalVersion으로 명시 전환한다
    먼저 사례 파일 "verification/cases/T21/case.json"의 "explicit-authorized-migration"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "fde" 역할이 "draft" 행동을 수행한다
    만일 "fde" 역할이 "validate" 행동을 수행한다
    만일 "fde" 역할이 "submit" 행동을 수행한다
    만일 "approver" 역할이 "approve" 행동을 수행한다
    만일 "fde" 역할이 "publish" 행동을 수행한다
    만일 "fde" 역할이 "activate" 행동을 수행한다
    만일 "fde" 역할이 "migration-review" 행동을 수행한다
    만일 "approver" 역할이 "migration-approval" 행동을 수행한다
    만일 "fde" 역할이 "unauthorized-migrate" 행동을 수행한다
    만일 "owner" 역할이 "migrate" 행동을 수행한다
    만일 "owner" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unauthorized-goal-change" assertion으로 "explicit-migration"를 확인한다
    그러면 "unauthorized-goal-effects0" assertion으로 "explicit-migration"를 확인한다
    그러면 "migration-applied" assertion으로 "explicit-migration"를 확인한다
    그러면 "new-goal-previous-id" assertion으로 "explicit-migration"를 확인한다
    그러면 "old-and-new-goals" assertion으로 "explicit-migration"를 확인한다
    그러면 "migration-work-data-list" assertion으로 "explicit-migration"를 확인한다
    그러면 "mapping-complete" assertion으로 "explicit-migration"를 확인한다
    그러면 "regression-decisions" assertion으로 "explicit-migration"를 확인한다
    그러면 "unsupported-values-explicit" assertion으로 "explicit-migration"를 확인한다

  시나리오: ACTIVE 포인터 복구는 이미 확정한 실물·의무·외부 효과를 보상하지 않는다
    먼저 사례 파일 "verification/cases/T21/case.json"의 "active-pointer-rollback-preserves-effects"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "fde" 역할이 "draft" 행동을 수행한다
    만일 "fde" 역할이 "validate" 행동을 수행한다
    만일 "fde" 역할이 "submit" 행동을 수행한다
    만일 "approver" 역할이 "approve" 행동을 수행한다
    만일 "fde" 역할이 "publish" 행동을 수행한다
    만일 "fde" 역할이 "activate" 행동을 수행한다
    만일 "steward" 역할이 "receipt-evidence" 행동을 수행한다
    만일 "steward" 역할이 "receipt" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "fde" 역할이 "prior-package" 행동을 수행한다
    만일 "fde" 역할이 "rollback-pointer" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "unchanged-segments" assertion으로 "active-pointer-rollback-physical-compensation"를 확인한다
    그러면 "unchanged-movements" assertion으로 "active-pointer-rollback-physical-compensation"를 확인한다
    그러면 "unchanged-receiptContributions" assertion으로 "active-pointer-rollback-physical-compensation"를 확인한다
    그러면 "unchanged-obligations" assertion으로 "active-pointer-rollback-physical-compensation"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "active-pointer-rollback-physical-compensation"를 확인한다
    그러면 "unchanged-externalOperationResults" assertion으로 "active-pointer-rollback-physical-compensation"를 확인한다
    그러면 "pointer-restored" assertion으로 "active-pointer-rollback-physical-compensation"를 확인한다
    그러면 "physical60-preserved" assertion으로 "active-pointer-rollback-physical-compensation"를 확인한다
