# T21 인수 계약

이 계약은 제품 구현에 앞서 기대 동작을 고정한다. API·독립 DB·blob·
control adapter는 아직 없으며 실제 제품 판정은 NOT_RUN이다. 표본
JUnit은 실제 AssertionEngine만 검사하고 제품 workflow를 만들지 않는다.

각 fixture의 시각은 2026-10-07T09:00:00Z이고 Asia/Seoul, SECOND
정밀도, inclusive deadline을 명시한다. 정책은 synthetic 값이며
실제 법규·승인·운영 자료의 인수가 아니다. actor의 역할과 grant는
명시한 조직·품목·장소·업무·case namespace로 제한한다.

installFixture 이후 서버 ID를 strict alias/result ref로 연결한다.
observe는 실제 API가 발급한 snapshotRevision, 완전한 scope와
원 행/source query artifact를 요구한다. effect0은 허용된 감사·대조
의무와 구분하고 금지된 원장·배분·이행·outbox의 전후 원 행을 비교한다.

## 독립 계산과 책임

DRAFT→IN_REVIEW→PUBLISHED→ACTIVE→RETIRED_FOR_NEW_WORK를
실행하고 CONFIG_APPROVER 결정과 실제 hash/revision을 연결한다.
발행 내용 불변과 활성 포인터 변경은 별도로 검사한다. 이름·선택 속성·
필수 slot·단위·끝점·관계 의미의6 diff와9 validation 반례를 검사한다.
v1 도착60 목표는 v2 판매 적격 의미가 배포돼도 그대로다. 현재 QC
제한은 신규 예약을 막는다. 현재 도착 기여40+20=60 후 수령20→18
정정이면40+18=58로 재평가하며 이전 SATISFIED 판정을 보존한다.
명시 전환은 실제 검증 결과 ID·대상 업무/자료·mapping·승인·인간
목표 변경 권한과 새/이전 GoalVersion을 검사한다. regression PASS를
미리 fixture에 넣지 않고 validateDefinition의 실제 run ID를 참조한다.
미지원 v1은 VERSION_UNSUPPORTED·효과0과 별도 kind의 보류 의무를
남긴다. 기존 도착 잔여 의무와 새 보류 의무를 합쳐 count1로 가정하지 않는다.
ACTIVE 포인터를 복구해도40+실제 수령20=60과 과거 확정 외부 결과를
보존한다. 외부 결과 fixture는 이번 포인터 변경 전의 역사 입력이다.

## Observation 연결

| Oracle / observation | 실제 assertion |
|---|---|
| T21.definition-publish-and-impact / definition-generated-new-capability | `approved-lifecycle/no-new-execution-capability`, `invalid-executable-payloads/sql-rejected`, `invalid-executable-payloads/sql-effects0`, `invalid-executable-payloads/script-rejected`, `invalid-executable-payloads/script-effects0`, `invalid-executable-payloads/privilege-rejected`, `invalid-executable-payloads/privilege-effects0`, `invalid-executable-payloads/operator-rejected`, `invalid-executable-payloads/operator-effects0`, `invalid-executable-payloads/unchanged-capabilities` |
| T21.definition-publish-and-impact / definition-lifecycle | `approved-lifecycle/active-pointer-deploys-reviewed-version`, `approved-lifecycle/draft-state`, `approved-lifecycle/submit-state`, `approved-lifecycle/publish-state`, `approved-lifecycle/activate-state`, `approved-lifecycle/retire-state`, `approved-lifecycle/config-approver-binding`, `approved-lifecycle/published-hash-is-reviewed`, `approved-lifecycle/activated-hash-is-published`, `approved-lifecycle/published-version-content`, `approval-required-and-rejected-revision/fde-no-approval`, `approval-required-and-rejected-revision/unapproved-no-publish`, `approval-required-and-rejected-revision/revised-state`, `approval-required-and-rejected-revision/new-revision`, `approval-required-and-rejected-revision/rejection-preserved`, `approval-required-and-rejected-revision/no-active-v2`, `published-content-immutable/mutation-rejected`, `published-content-immutable/no-mutated-effects`, `published-content-immutable/unchanged-segments`, `published-content-immutable/unchanged-movements`, `published-content-immutable/unchanged-businessOutbox`, `published-content-immutable/immutable-package-content`, `published-content-immutable/immutable-package-hash`, `published-content-immutable/only-active-pointer-v1` |
| T21.definition-publish-and-impact / impact-validation | `six-impact-diff-classes/rename-diff`, `six-impact-diff-classes/rename-raw-diff`, `six-impact-diff-classes/rename-support-manifest`, `six-impact-diff-classes/rename-regression-complete`, `six-impact-diff-classes/rename-regression-results`, `six-impact-diff-classes/optional-attribute-diff`, `six-impact-diff-classes/optional-attribute-raw-diff`, `six-impact-diff-classes/optional-attribute-support-manifest`, `six-impact-diff-classes/optional-attribute-regression-complete`, `six-impact-diff-classes/optional-attribute-regression-results`, `six-impact-diff-classes/required-slot-diff`, `six-impact-diff-classes/required-slot-raw-diff`, `six-impact-diff-classes/required-slot-support-manifest`, `six-impact-diff-classes/required-slot-regression-complete`, `six-impact-diff-classes/required-slot-regression-results`, `six-impact-diff-classes/unit-diff`, `six-impact-diff-classes/unit-raw-diff`, `six-impact-diff-classes/unit-support-manifest`, `six-impact-diff-classes/unit-regression-complete`, `six-impact-diff-classes/unit-regression-results`, `six-impact-diff-classes/endpoint-diff`, `six-impact-diff-classes/endpoint-raw-diff`, `six-impact-diff-classes/endpoint-support-manifest`, `six-impact-diff-classes/endpoint-regression-complete`, `six-impact-diff-classes/endpoint-regression-results`, `six-impact-diff-classes/relation-meaning-diff`, `six-impact-diff-classes/relation-meaning-raw-diff`, `six-impact-diff-classes/relation-meaning-support-manifest`, `six-impact-diff-classes/relation-meaning-regression-complete`, `six-impact-diff-classes/relation-meaning-regression-results`, `validation-constraints/types-error`, `validation-constraints/cardinality-error`, `validation-constraints/required-stage-error`, `validation-constraints/unit-error`, `validation-constraints/cycle-error`, `validation-constraints/goal-error`, `validation-constraints/organization-error`, `validation-constraints/regression-error`, `validation-constraints/manifest-error`, `validation-constraints/all-validation-findings` |
| T21.definition-publish-and-impact / invalid-payload-published | `validation-constraints/types-cannot-publish`, `validation-constraints/types-publish-rejected`, `validation-constraints/types-publish-effects0`, `validation-constraints/cardinality-cannot-publish`, `validation-constraints/cardinality-publish-rejected`, `validation-constraints/cardinality-publish-effects0`, `validation-constraints/required-stage-cannot-publish`, `validation-constraints/required-stage-publish-rejected`, `validation-constraints/required-stage-publish-effects0`, `validation-constraints/unit-cannot-publish`, `validation-constraints/unit-publish-rejected`, `validation-constraints/unit-publish-effects0`, `validation-constraints/cycle-cannot-publish`, `validation-constraints/cycle-publish-rejected`, `validation-constraints/cycle-publish-effects0`, `validation-constraints/goal-cannot-publish`, `validation-constraints/goal-publish-rejected`, `validation-constraints/goal-publish-effects0`, `validation-constraints/organization-cannot-publish`, `validation-constraints/organization-publish-rejected`, `validation-constraints/organization-publish-effects0`, `validation-constraints/regression-cannot-publish`, `validation-constraints/regression-publish-rejected`, `validation-constraints/regression-publish-effects0`, `validation-constraints/manifest-cannot-publish`, `validation-constraints/manifest-publish-rejected`, `validation-constraints/manifest-publish-effects0`, `validation-constraints/no-invalid-published`, `invalid-executable-payloads/sql-rejected`, `invalid-executable-payloads/sql-effects0`, `invalid-executable-payloads/script-rejected`, `invalid-executable-payloads/script-effects0`, `invalid-executable-payloads/privilege-rejected`, `invalid-executable-payloads/privilege-effects0`, `invalid-executable-payloads/operator-rejected`, `invalid-executable-payloads/operator-effects0`, `invalid-executable-payloads/unchanged-definitionPackages`, `invalid-executable-payloads/unchanged-activeDefinitionPointers`, `invalid-executable-payloads/unchanged-businessOutbox` |
| T21.pinned-old-current-policy / current-sell-policy | `pinned-meaning-current-policy/sell-current-denied`, `pinned-meaning-current-policy/v1-reserve-forbidden`, `pinned-meaning-current-policy/db-current-qc-restriction` |
| T21.pinned-old-current-policy / new-work-endpoint | `pinned-meaning-current-policy/new-endpoint`, `pinned-meaning-current-policy/db-new-goal-endpoint` |
| T21.pinned-old-current-policy / old-work-endpoint | `pinned-meaning-current-policy/old-endpoint`, `pinned-meaning-current-policy/db-old-goal-endpoint` |
| T21.pinned-old-current-policy / v1-correction-reassessment | `pinned-meaning-current-policy/old-definition`, `pinned-meaning-current-policy/old-evaluator`, `pinned-meaning-current-policy/db-old-work-pinned`, `pinned-meaning-current-policy/arrival60-result`, `pinned-meaning-current-policy/corrected58-result`, `pinned-meaning-current-policy/correction-definition-v1`, `pinned-meaning-current-policy/correction-evaluator-v1`, `pinned-meaning-current-policy/correction-still-arrived`, `pinned-meaning-current-policy/current-qc-still-denied`, `pinned-meaning-current-policy/corrected-receipt-58`, `pinned-meaning-current-policy/immutable-assessment-history`, `pinned-meaning-current-policy/db-current-arrival58`, `pinned-meaning-current-policy/previous-assessment-linked` |
| T21.pinned-old-current-policy / v1-used-to-bypass-current-policy | `pinned-meaning-current-policy/v1-reserve-no-effects`, `pinned-meaning-current-policy/unchanged-segments`, `pinned-meaning-current-policy/unchanged-movements`, `pinned-meaning-current-policy/unchanged-allocations`, `pinned-meaning-current-policy/unchanged-businessOutbox` |
| T21.unsupported-and-explicit-migration / active-pointer-rollback-physical-compensation | `active-pointer-rollback-preserves-effects/unchanged-segments`, `active-pointer-rollback-preserves-effects/unchanged-movements`, `active-pointer-rollback-preserves-effects/unchanged-receiptContributions`, `active-pointer-rollback-preserves-effects/unchanged-obligations`, `active-pointer-rollback-preserves-effects/unchanged-businessOutbox`, `active-pointer-rollback-preserves-effects/unchanged-externalOperationResults`, `active-pointer-rollback-preserves-effects/pointer-restored`, `active-pointer-rollback-preserves-effects/physical60-preserved` |
| T21.unsupported-and-explicit-migration / explicit-migration | `explicit-authorized-migration/unauthorized-goal-change`, `explicit-authorized-migration/unauthorized-goal-effects0`, `explicit-authorized-migration/migration-applied`, `explicit-authorized-migration/new-goal-previous-id`, `explicit-authorized-migration/old-and-new-goals`, `explicit-authorized-migration/migration-work-data-list`, `explicit-authorized-migration/mapping-complete`, `explicit-authorized-migration/regression-decisions`, `explicit-authorized-migration/unsupported-values-explicit` |
| T21.unsupported-and-explicit-migration / unsupported-result | `unsupported-v1-held/unsupported-error`, `unsupported-v1-held/old-work-no-fallback`, `unsupported-v1-held/unsupported-assessment0` |
| T21.unsupported-and-explicit-migration / unsupported-version-duty | `unsupported-v1-held/unsupported-assessment-no-fallback`, `unsupported-v1-held/work-held`, `unsupported-v1-held/current-duty-count`, `unsupported-v1-held/current-duty-owner`, `unsupported-v1-held/current-duty-action`, `unsupported-v1-held/current-duty-time`, `unsupported-v1-held/current-duty-state`, `unsupported-v1-held/duty-supervisor`, `unsupported-v1-held/api-duty-count`, `unsupported-v1-held/api-duty-details`, `unsupported-v1-held/old-work-no-fallback`, `unsupported-v1-held/unsupported-assessment0` |
| T21.unsupported-and-explicit-migration / unsupported-version-effects | `unsupported-v1-held/unsupported-effects0`, `unsupported-v1-held/unchanged-segments`, `unsupported-v1-held/unchanged-movements`, `unsupported-v1-held/unchanged-allocations`, `unsupported-v1-held/unchanged-goalVersions`, `unsupported-v1-held/unchanged-businessOutbox`, `unsupported-v1-held/old-work-no-fallback`, `unsupported-v1-held/unsupported-assessment0` |

## 실제 검사 결과

B2와 공통 수정77c2df3을 기준으로 schema 검증 exit0, harness145개
검사 PASS·skip0, 본 영역 표본15개 PASS를 확인했다. 네 Gherkin
file selector는32개를 발견·시작했고 NOT_IMPLEMENTED assertion failure
32개·scenario skip0·exit1이다. 제품 scenarios32개는 NOT_RUN·exit2다.
`evidence/checks.json`은 case별 count와 원본 report·로그·hash를 연결한다.
선행 goalVersionId schema 오류는 공통 수정 후 해소됐으며 최종 RED가 아니다.
