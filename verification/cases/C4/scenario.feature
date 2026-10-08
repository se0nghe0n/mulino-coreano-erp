# language: ko
@C4 @D06 @D12 @D18 @sit @uat
기능: 새 반품과 과거 정정 및 유효 의무 해소

  시나리오: 과거 인도100과 새 반품20을 분리하며 중복 반품·과거80 덮어쓰기를 막는다
    먼저 사례 파일 "verification/cases/C4/case.json"의 "return-not-correction"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "observer" 역할이 "return-history-assessment" 행동을 수행한다
    만일 "observer" 역할이 "before-return" 행동을 수행한다
    만일 "시스템" 역할이 "before-return-db" 행동을 수행한다
    만일 "receiver" 역할이 "return-authorize" 행동을 수행한다
    만일 "receiver" 역할이 "return" 행동을 수행한다
    만일 "observer" 역할이 "before-duplicate" 행동을 수행한다
    만일 "시스템" 역할이 "before-duplicate-db" 행동을 수행한다
    만일 "receiver" 역할이 "duplicate-return" 행동을 수행한다
    만일 "observer" 역할이 "returned" 행동을 수행한다
    만일 "observer" 역할이 "after-return-assessment" 행동을 수행한다
    만일 "시스템" 역할이 "returned-db" 행동을 수행한다
    그러면 "historical-delivery-1" assertion으로 "historical-delivery"를 확인한다
    그러면 "historical-delivery-2" assertion으로 "historical-delivery"를 확인한다
    그러면 "new-return-3" assertion으로 "new-return"를 확인한다
    그러면 "new-return-4" assertion으로 "new-return"를 확인한다
    그러면 "duplicate-return-material-effects-5" assertion으로 "duplicate-return-material-effects"를 확인한다
    그러면 "duplicate-return-material-effects-6" assertion으로 "duplicate-return-material-effects"를 확인한다
    그러면 "duplicate-return-material-effects-7" assertion으로 "duplicate-return-material-effects"를 확인한다
    그러면 "duplicate-return-material-effects-8" assertion으로 "duplicate-return-material-effects"를 확인한다
    그러면 "return-overwrites-delivery-to80-9" assertion으로 "return-overwrites-delivery-to80"를 확인한다
    그러면 "physical-reference-10" assertion으로 "physical-reference"를 확인한다
    그러면 "physical-reference-11" assertion으로 "physical-reference"를 확인한다
    그러면 "return-overwrites-delivery-to80-12" assertion으로 "반품 전 기준선 인도 판정은 SATISFIED다"를 확인한다
    그러면 "return-overwrites-delivery-to80-13" assertion으로 "반품 뒤에도 인도 판정은 SATISFIED다"를 확인한다
    그러면 "return-overwrites-delivery-to80-14" assertion으로 "반품 뒤 주문 업무의 현재 판정 원행은 SATISFIED뿐이다"를 확인한다
    그러면 "return-overwrites-delivery-to80-15" assertion으로 "반품은 인도 부족 의무를 만들지 않는다"를 확인한다

  시나리오: 인도100의 실제98 정정은 과거 판정과 현재 부족2의 인간 책임을 함께 남긴다
    먼저 사례 파일 "verification/cases/C4/case.json"의 "corrected-delivery-98"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "observer" 역할이 "old-assessment" 행동을 수행한다
    만일 "observer" 역할이 "historical" 행동을 수행한다
    만일 "시스템" 역할이 "historical-db" 행동을 수행한다
    만일 "receiver" 역할이 "correct98" 행동을 수행한다
    만일 "observer" 역할이 "corrected" 행동을 수행한다
    만일 "시스템" 역할이 "corrected-db" 행동을 수행한다
    만일 "observer" 역할이 "current-assessment" 행동을 수행한다
    만일 "observer" 역할이 "current-duties" 행동을 수행한다
    그러면 "historical-assessment-1" assertion으로 "historical-assessment"를 확인한다
    그러면 "historical-assessment-2" assertion으로 "historical-assessment"를 확인한다
    그러면 "currently-supported-delivery-3" assertion으로 "currently-supported-delivery"를 확인한다
    그러면 "currently-supported-delivery-4" assertion으로 "currently-supported-delivery"를 확인한다
    그러면 "current-unresolved-deficit-5" assertion으로 "current-unresolved-deficit"를 확인한다
    그러면 "current-unresolved-deficit-6" assertion으로 "current-unresolved-deficit"를 확인한다
    그러면 "historical-assessment-7" assertion으로 "historical-assessment"를 확인한다
    그러면 "historical-assessment-8" assertion으로 "historical-assessment"를 확인한다
    그러면 "historical-assessment-9" assertion으로 "historical-assessment"를 확인한다
    그러면 "historical-assessment-10" assertion으로 "historical-assessment"를 확인한다
    그러면 "deficit-owner-11" assertion으로 "deficit-owner"를 확인한다
    그러면 "deficit-owner-12" assertion으로 "deficit-owner"를 확인한다
    그러면 "deficit-owner-13" assertion으로 "deficit-owner"를 확인한다
    그러면 "deficit-owner-14" assertion으로 "deficit-owner"를 확인한다
    그러면 "deficit-owner-15" assertion으로 "deficit-owner"를 확인한다
    그러면 "deficit-owner-16" assertion으로 "deficit-owner"를 확인한다
    그러면 "deficit-owner-17" assertion으로 "deficit-owner"를 확인한다

  시나리오: 정정으로 생긴 부족2를 RESOLVED 근거로 해소한 뒤 재대조해도 같은 의무는 부활하지 않는다
    먼저 사례 파일 "verification/cases/C4/case.json"의 "resolved-no-resurrection-resolved"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "observer" 역할이 "old-assessment" 행동을 수행한다
    만일 "observer" 역할이 "historical" 행동을 수행한다
    만일 "시스템" 역할이 "historical-db" 행동을 수행한다
    만일 "receiver" 역할이 "correct98" 행동을 수행한다
    만일 "observer" 역할이 "corrected" 행동을 수행한다
    만일 "시스템" 역할이 "corrected-db" 행동을 수행한다
    만일 "observer" 역할이 "duty" 행동을 수행한다
    만일 "manager" 역할이 "resolution" 행동을 수행한다
    만일 "observer" 역할이 "before-reprocess" 행동을 수행한다
    만일 "시스템" 역할이 "before-reprocess-db" 행동을 수행한다
    만일 "receiver" 역할이 "reconfirm98" 행동을 수행한다
    만일 "observer" 역할이 "after-reprocess" 행동을 수행한다
    만일 "시스템" 역할이 "after-reprocess-db" 행동을 수행한다
    그러면 "new-unresolved-deficit-1" assertion으로 "new-unresolved-deficit"를 확인한다
    그러면 "new-unresolved-deficit-2" assertion으로 "new-unresolved-deficit"를 확인한다
    그러면 "resolved-debt-resurrection-3" assertion으로 "resolved-debt-resurrection"를 확인한다
    그러면 "valid-resolution-4" assertion으로 "valid-resolution"를 확인한다
    그러면 "valid-resolution-5" assertion으로 "valid-resolution"를 확인한다
    그러면 "valid-resolution-6" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-applied-7" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-new-occurrence-8" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-new-revision-9" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-baseline-98-10" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-canonical-revision-11" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-effective-98-12" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-old-revision-13" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-same-responsibility-id-14" assertion으로 "valid-resolution"를 확인한다

  시나리오: 정정으로 생긴 부족2를 WAIVED 근거로 해소한 뒤 재대조해도 같은 의무는 부활하지 않는다
    먼저 사례 파일 "verification/cases/C4/case.json"의 "resolved-no-resurrection-waived"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "observer" 역할이 "old-assessment" 행동을 수행한다
    만일 "observer" 역할이 "historical" 행동을 수행한다
    만일 "시스템" 역할이 "historical-db" 행동을 수행한다
    만일 "receiver" 역할이 "correct98" 행동을 수행한다
    만일 "observer" 역할이 "corrected" 행동을 수행한다
    만일 "시스템" 역할이 "corrected-db" 행동을 수행한다
    만일 "observer" 역할이 "duty" 행동을 수행한다
    만일 "manager" 역할이 "resolution" 행동을 수행한다
    만일 "observer" 역할이 "before-reprocess" 행동을 수행한다
    만일 "시스템" 역할이 "before-reprocess-db" 행동을 수행한다
    만일 "receiver" 역할이 "reconfirm98" 행동을 수행한다
    만일 "observer" 역할이 "after-reprocess" 행동을 수행한다
    만일 "시스템" 역할이 "after-reprocess-db" 행동을 수행한다
    그러면 "new-unresolved-deficit-1" assertion으로 "new-unresolved-deficit"를 확인한다
    그러면 "new-unresolved-deficit-2" assertion으로 "new-unresolved-deficit"를 확인한다
    그러면 "resolved-debt-resurrection-3" assertion으로 "resolved-debt-resurrection"를 확인한다
    그러면 "valid-resolution-4" assertion으로 "valid-resolution"를 확인한다
    그러면 "valid-resolution-5" assertion으로 "valid-resolution"를 확인한다
    그러면 "valid-resolution-6" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-applied-7" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-new-occurrence-8" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-new-revision-9" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-baseline-98-10" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-canonical-revision-11" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-effective-98-12" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-old-revision-13" assertion으로 "valid-resolution"를 확인한다
    그러면 "reconfirm-same-responsibility-id-14" assertion으로 "valid-resolution"를 확인한다
