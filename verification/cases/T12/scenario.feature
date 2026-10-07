# language: ko
@T12 @D12 @sit @uat
기능: 목표·의무·판정·접수 책임의 독립 계약 T12

  시나리오: 판정 근거와 장소·상충을 구별한다 no-receipt-evidence
    먼저 사례 파일 "verification/cases/T12/case.json"의 "no-receipt-evidence"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "work" 행동을 수행한다
    만일 "A" 역할이 "initial-assessment" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "claim" 행동을 수행한다
    만일 "A" 역할이 "assessment" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "assessment-api-result" assertion으로 "판정 근거와 장소·상충을 구별한다 no-receipt-evidence. insufficient-evidence의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assessment-db-result" assertion으로 "판정 근거와 장소·상충을 구별한다 no-receipt-evidence. insufficient-evidence의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "versions-pinned" assertion으로 "판정 근거와 장소·상충을 구별한다 no-receipt-evidence. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "goal-version-linked" assertion으로 "판정 근거와 장소·상충을 구별한다 no-receipt-evidence. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "condition-result-detail" assertion으로 "판정 근거와 장소·상충을 구별한다 no-receipt-evidence. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "condition-evidence-input-match" assertion으로 "판정 근거와 장소·상충을 구별한다 no-receipt-evidence. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "previous-assessment-link" assertion으로 "판정 근거와 장소·상충을 구별한다 no-receipt-evidence. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assessment-asof" assertion으로 "판정 근거와 장소·상충을 구별한다 no-receipt-evidence. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assessment-knownat" assertion으로 "판정 근거와 장소·상충을 구별한다 no-receipt-evidence. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "input-snapshot-rows" assertion으로 "판정 근거와 장소·상충을 구별한다 no-receipt-evidence. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "unknown-quantity-not-zero" assertion으로 "판정 근거와 장소·상충을 구별한다 no-receipt-evidence. missing-as-zero의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-fabricated-contribution" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-fabricated-movement" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 판정 근거와 장소·상충을 구별한다 insufficient-source-scope
    먼저 사례 파일 "verification/cases/T12/case.json"의 "insufficient-source-scope"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "work" 행동을 수행한다
    만일 "A" 역할이 "initial-assessment" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "claim" 행동을 수행한다
    만일 "A" 역할이 "assessment" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "assessment-api-result" assertion으로 "판정 근거와 장소·상충을 구별한다 insufficient-source-scope. insufficient-evidence의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assessment-db-result" assertion으로 "판정 근거와 장소·상충을 구별한다 insufficient-source-scope. insufficient-evidence의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "versions-pinned" assertion으로 "판정 근거와 장소·상충을 구별한다 insufficient-source-scope. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "goal-version-linked" assertion으로 "판정 근거와 장소·상충을 구별한다 insufficient-source-scope. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "condition-result-detail" assertion으로 "판정 근거와 장소·상충을 구별한다 insufficient-source-scope. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "condition-evidence-input-match" assertion으로 "판정 근거와 장소·상충을 구별한다 insufficient-source-scope. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "previous-assessment-link" assertion으로 "판정 근거와 장소·상충을 구별한다 insufficient-source-scope. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assessment-asof" assertion으로 "판정 근거와 장소·상충을 구별한다 insufficient-source-scope. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assessment-knownat" assertion으로 "판정 근거와 장소·상충을 구별한다 insufficient-source-scope. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "input-snapshot-rows" assertion으로 "판정 근거와 장소·상충을 구별한다 insufficient-source-scope. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "unknown-quantity-not-zero" assertion으로 "판정 근거와 장소·상충을 구별한다 insufficient-source-scope. missing-as-zero의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-fabricated-contribution" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-fabricated-movement" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 판정 근거와 장소·상충을 구별한다 wrong-warehouse
    먼저 사례 파일 "verification/cases/T12/case.json"의 "wrong-warehouse"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "work" 행동을 수행한다
    만일 "A" 역할이 "initial-assessment" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "receipt" 행동을 수행한다
    만일 "A" 역할이 "assessment" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "assessment-api-result" assertion으로 "판정 근거와 장소·상충을 구별한다 wrong-warehouse. wrong-place-condition의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assessment-db-result" assertion으로 "판정 근거와 장소·상충을 구별한다 wrong-warehouse. wrong-place-condition의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "versions-pinned" assertion으로 "판정 근거와 장소·상충을 구별한다 wrong-warehouse. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "goal-version-linked" assertion으로 "판정 근거와 장소·상충을 구별한다 wrong-warehouse. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "condition-result-detail" assertion으로 "판정 근거와 장소·상충을 구별한다 wrong-warehouse. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "condition-evidence-input-match" assertion으로 "판정 근거와 장소·상충을 구별한다 wrong-warehouse. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "previous-assessment-link" assertion으로 "판정 근거와 장소·상충을 구별한다 wrong-warehouse. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assessment-asof" assertion으로 "판정 근거와 장소·상충을 구별한다 wrong-warehouse. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assessment-knownat" assertion으로 "판정 근거와 장소·상충을 구별한다 wrong-warehouse. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "input-snapshot-rows" assertion으로 "판정 근거와 장소·상충을 구별한다 wrong-warehouse. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 판정 근거와 장소·상충을 구별한다 conflicting-claims
    먼저 사례 파일 "verification/cases/T12/case.json"의 "conflicting-claims"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "work" 행동을 수행한다
    만일 "A" 역할이 "initial-assessment" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "claim" 행동을 수행한다
    만일 "A" 역할이 "conflicting-claim" 행동을 수행한다
    만일 "A" 역할이 "assessment" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "assessment-api-result" assertion으로 "판정 근거와 장소·상충을 구별한다 conflicting-claims. insufficient-evidence의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assessment-db-result" assertion으로 "판정 근거와 장소·상충을 구별한다 conflicting-claims. insufficient-evidence의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "versions-pinned" assertion으로 "판정 근거와 장소·상충을 구별한다 conflicting-claims. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "goal-version-linked" assertion으로 "판정 근거와 장소·상충을 구별한다 conflicting-claims. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "condition-result-detail" assertion으로 "판정 근거와 장소·상충을 구별한다 conflicting-claims. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "condition-evidence-input-match" assertion으로 "판정 근거와 장소·상충을 구별한다 conflicting-claims. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "previous-assessment-link" assertion으로 "판정 근거와 장소·상충을 구별한다 conflicting-claims. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assessment-asof" assertion으로 "판정 근거와 장소·상충을 구별한다 conflicting-claims. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assessment-knownat" assertion으로 "판정 근거와 장소·상충을 구별한다 conflicting-claims. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "input-snapshot-rows" assertion으로 "판정 근거와 장소·상충을 구별한다 conflicting-claims. assessment-snapshot의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "unknown-quantity-not-zero" assertion으로 "판정 근거와 장소·상충을 구별한다 conflicting-claims. missing-as-zero의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-fabricated-contribution" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-fabricated-movement" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 기한 변경과 실제100→98 정정은 과거 위반을 지우지 않는다
    먼저 사례 파일 "verification/cases/T12/case.json"의 "deadline-revision-and-correction"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "work" 행동을 수행한다
    만일 "A" 역할이 "receipt" 행동을 수행한다
    만일 "A" 역할이 "late-assessment" 행동을 수행한다
    만일 "A" 역할이 "late-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "late-db" 행동을 수행한다
    만일 "A" 역할이 "resolve-quantity-duty" 행동을 수행한다
    만일 "A" 역할이 "before-revision" 행동을 수행한다
    만일 "A" 역할이 "revision" 행동을 수행한다
    만일 "A" 역할이 "revised-assessment" 행동을 수행한다
    만일 "A" 역할이 "revised-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "revised-db" 행동을 수행한다
    만일 "A" 역할이 "correction" 행동을 수행한다
    만일 "A" 역할이 "corrected-assessment" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "late-assessment-api-result" assertion으로 "기한 변경과 실제100→98 정정은 과거 위반을 지우지 않는다. historical-violation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "late-assessment-db-result" assertion으로 "기한 변경과 실제100→98 정정은 과거 위반을 지우지 않는다. historical-violation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "revised-assessment-api-result" assertion으로 "기한 변경과 실제100→98 정정은 과거 위반을 지우지 않는다. historical-violation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "revised-assessment-db-result" assertion으로 "기한 변경과 실제100→98 정정은 과거 위반을 지우지 않는다. historical-violation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "corrected-assessment-api-result" assertion으로 "기한 변경과 실제100→98 정정은 과거 위반을 지우지 않는다. historical-violation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "corrected-assessment-db-result" assertion으로 "기한 변경과 실제100→98 정정은 과거 위반을 지우지 않는다. historical-violation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "old-assessment-immutable" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "old-goal-immutable" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "revised-assessment-immutable" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "old-due-preserved" assertion으로 "기한 변경과 실제100→98 정정은 과거 위반을 지우지 않는다. historical-violation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "new-due-added" assertion으로 "기한 변경과 실제100→98 정정은 과거 위반을 지우지 않는다. historical-violation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "quantity-duty-resolved" assertion으로 "기한 변경과 실제100→98 정정은 과거 위반을 지우지 않는다. historical-violation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "past-deadline-condition" assertion으로 "기한 변경과 실제100→98 정정은 과거 위반을 지우지 않는다. historical-violation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "correction-assessment-appended" assertion으로 "기한 변경과 실제100→98 정정은 과거 위반을 지우지 않는다. historical-violation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "supersedes-preserved" assertion으로 "기한 변경과 실제100→98 정정은 과거 위반을 지우지 않는다. historical-violation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "corrected-debt2" assertion으로 "기한 변경과 실제100→98 정정은 과거 위반을 지우지 않는다. historical-violation의 독립 고정 기대값을 대조한다."를 확인한다
