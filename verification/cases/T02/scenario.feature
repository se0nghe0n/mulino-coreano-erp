# language: ko
@T02 @D02 @sit @uat
기능: 품목 식별과 환산의 업무 효과를 구별한다

  시나리오: 같은 SKU와 LOT 표기를 발급자·제조자·품목 문맥별로 구별한다
    먼저 사례 파일 "verification/cases/T02/case.json"의 "issuer-and-lot-context"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "steward" 역할이 "register-a" 행동을 수행한다
    만일 "steward" 역할이 "identifier-a" 행동을 수행한다
    만일 "steward" 역할이 "evidence-a" 행동을 수행한다
    만일 "steward" 역할이 "receipt-a" 행동을 수행한다
    만일 "steward" 역할이 "register-b" 행동을 수행한다
    만일 "steward" 역할이 "identifier-b" 행동을 수행한다
    만일 "steward" 역할이 "evidence-b" 행동을 수행한다
    만일 "steward" 역할이 "receipt-b" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "api-distinct-item-ids" assertion으로 "distinct-item-ids"를 확인한다
    그러면 "db-item-count" assertion으로 "distinct-item-ids"를 확인한다
    그러면 "db-item-set" assertion으로 "distinct-item-ids"를 확인한다
    그러면 "db-lot-count" assertion으로 "distinct-lot-ids"를 확인한다
    그러면 "db-lot-set" assertion으로 "distinct-lot-ids"를 확인한다
    그러면 "issuer-item-relations" assertion으로 "distinct-item-ids"를 확인한다
    그러면 "manufacturer-lot-relations" assertion으로 "distinct-lot-ids"를 확인한다
    그러면 "unchanged-mergeRecords" assertion으로 "implicit-merges"를 확인한다

  시나리오: 동일 issuer 코드의 유효기간을 대조하고 이전 mapping을 보존한다
    먼저 사례 파일 "verification/cases/T02/case.json"의 "period-overlap"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "first-link" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "steward" 역할이 "second-link" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "overlap-outcome" assertion으로 "code-conflict"를 확인한다
    그러면 "overlap-api-effects" assertion으로 "silent-remap"를 확인한다
    그러면 "unchanged-externalIdentifiers" assertion으로 "silent-remap"를 확인한다
    그러면 "unchanged-mergeRecords" assertion으로 "silent-remap"를 확인한다
    그러면 "unchanged-segments" assertion으로 "silent-remap"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "silent-remap"를 확인한다
    그러면 "current-duty-count" assertion으로 "code-reconciliation"를 확인한다
    그러면 "current-duty-owner" assertion으로 "code-reconciliation"를 확인한다
    그러면 "current-duty-action" assertion으로 "code-reconciliation"를 확인한다
    그러면 "current-duty-time" assertion으로 "code-reconciliation"를 확인한다
    그러면 "current-duty-state" assertion으로 "code-reconciliation"를 확인한다
    그러면 "duty-supervisor" assertion으로 "code-reconciliation"를 확인한다
    그러면 "conflict-candidate-items" assertion으로 "code-conflict"를 확인한다

  시나리오: 동일 issuer 코드의 유효기간을 대조하고 이전 mapping을 보존한다
    먼저 사례 파일 "verification/cases/T02/case.json"의 "period-boundary-nonoverlap"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "first-link" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "steward" 역할이 "second-link" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "nonoverlap-applied" assertion으로 "silent-remap"를 확인한다
    그러면 "historical-mappings-preserved" assertion으로 "silent-remap"를 확인한다
    그러면 "boundary-current-item" assertion으로 "silent-remap"를 확인한다
    그러면 "unchanged-mergeRecords" assertion으로 "silent-remap"를 확인한다
    그러면 "unchanged-segments" assertion으로 "silent-remap"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "silent-remap"를 확인한다

  시나리오: 1 BUNDLE의 내용량12 EA는 실물이나 주문 이행을 만들지 않는다
    먼저 사례 파일 "verification/cases/T02/case.json"의 "conversion-no-substitution"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "steward" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "steward" 역할이 "convert" 행동을 수행한다
    만일 "steward" 역할이 "substitution" 행동을 수행한다
    만일 "steward" 역할이 "after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "conversion-source-row" assertion으로 "converted-content"를 확인한다
    그러면 "converted-content-12" assertion으로 "converted-content"를 확인한다
    그러면 "reverse-original-unit" assertion으로 "converted-content"를 확인한다
    그러면 "substitution-unverified" assertion으로 "substitution"를 확인한다
    그러면 "substitution-missing" assertion으로 "substitution"를 확인한다
    그러면 "unchanged-segments" assertion으로 "conversion-created-inventory"를 확인한다
    그러면 "unchanged-movements" assertion으로 "conversion-created-inventory"를 확인한다
    그러면 "unchanged-fulfilments" assertion으로 "conversion-created-order-fulfilment"를 확인한다
    그러면 "unchanged-activities" assertion으로 "repackaging-effects"를 확인한다
    그러면 "unchanged-businessOutbox" assertion으로 "repackaging-effects"를 확인한다
    그러면 "held-bundle-delta" assertion으로 "conversion-created-inventory"를 확인한다
