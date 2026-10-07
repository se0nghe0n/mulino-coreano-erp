# language: ko
@T09 @D09 @sit @uat
기능: 목표·의무·판정·접수 책임의 독립 계약 T09

  시나리오: 월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다
    먼저 사례 파일 "verification/cases/T09/case.json"의 "cumulative-versus-state"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "cumulative" 행동을 수행한다
    만일 "A" 역할이 "state" 행동을 수행한다
    만일 "warehouse" 역할이 "receive60" 행동을 수행한다
    만일 "A" 역할이 "sale60" 행동을 수행한다
    만일 "warehouse" 역할이 "reserve60" 행동을 수행한다
    만일 "warehouse" 역할이 "pick60" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch60" 행동을 수행한다
    만일 "warehouse" 역할이 "receive40" 행동을 수행한다
    만일 "A" 역할이 "cumulative-assessment" 행동을 수행한다
    만일 "A" 역할이 "state-assessment" 행동을 수행한다
    만일 "A" 역할이 "inventory" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "api-arrived100" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. cumulative-arrived의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "raw-receipt-sum100" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. cumulative-arrived의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "api-held40" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. state-held의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "raw-current-leaf-sum40" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. state-held의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "arrival-distinct-identities" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative-assessment-api-result" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. cumulative-result의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative-assessment-db-result" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. cumulative-result의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state-assessment-api-result" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. state-result의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state-assessment-db-result" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. state-result의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-goal-mode" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-quantity" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-unit" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-placeId" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-endpoint" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-evidencePolicyVersion" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-periodStart" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-periodEnd" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-eventKinds" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-distinctContributionScope" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-goal-mode" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-quantity" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-unit" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-placeId" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-endpoint" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-evidencePolicyVersion" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-evaluationTime" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-eligibilityAction" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-includeReservations" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. goal-mode-fields의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 단계별 필수 정보 lot-not-yet-required
    먼저 사례 파일 "verification/cases/T09/case.json"의 "lot-not-yet-required"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "draft" 행동을 수행한다
    만일 "A" 역할이 "activate" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "draft-outcome" assertion으로 "단계별 필수 정보 lot-not-yet-required. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "activation-no-lot-allowed" assertion으로 "단계별 필수 정보 lot-not-yet-required. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-draft-physical-movements" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-draft-physical-contributions" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-draft-physical-outbox" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 단계별 필수 정보 missing-quantity
    먼저 사례 파일 "verification/cases/T09/case.json"의 "missing-quantity"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "draft" 행동을 수행한다
    만일 "A" 역할이 "activate" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "draft-outcome" assertion으로 "단계별 필수 정보 missing-quantity. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "activation-needs-input" assertion으로 "단계별 필수 정보 missing-quantity. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "draft-still-draft" assertion으로 "단계별 필수 정보 missing-quantity. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-draft-physical-movements" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-draft-physical-contributions" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-draft-physical-outbox" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 단계별 필수 정보 missing-dueAt
    먼저 사례 파일 "verification/cases/T09/case.json"의 "missing-dueAt"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "draft" 행동을 수행한다
    만일 "A" 역할이 "activate" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "draft-outcome" assertion으로 "단계별 필수 정보 missing-dueAt. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "activation-needs-input" assertion으로 "단계별 필수 정보 missing-dueAt. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "draft-still-draft" assertion으로 "단계별 필수 정보 missing-dueAt. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-draft-physical-movements" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-draft-physical-contributions" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-draft-physical-outbox" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 단계별 필수 정보 missing-endpoint
    먼저 사례 파일 "verification/cases/T09/case.json"의 "missing-endpoint"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "draft" 행동을 수행한다
    만일 "A" 역할이 "activate" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "draft-outcome" assertion으로 "단계별 필수 정보 missing-endpoint. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "activation-needs-input" assertion으로 "단계별 필수 정보 missing-endpoint. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "draft-still-draft" assertion으로 "단계별 필수 정보 missing-endpoint. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-draft-physical-movements" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-draft-physical-contributions" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-draft-physical-outbox" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 단계별 필수 정보 receipt-missing-lot
    먼저 사례 파일 "verification/cases/T09/case.json"의 "receipt-missing-lot"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "draft" 행동을 수행한다
    만일 "A" 역할이 "activate" 행동을 수행한다
    만일 "A" 역할이 "receipt" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "draft-outcome" assertion으로 "단계별 필수 정보 receipt-missing-lot. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "activation-no-lot-allowed" assertion으로 "단계별 필수 정보 receipt-missing-lot. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "receipt-outcome" assertion으로 "단계별 필수 정보 receipt-missing-lot. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "receipt-movements-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "receipt-contributions-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "receipt-approvals-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "receipt-outbox-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "receipt-denial-audit" assertion으로 "단계별 필수 정보 receipt-missing-lot. stage-requirements의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-draft-physical-movements" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-draft-physical-contributions" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-draft-physical-outbox" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 기간 내내100의 관측 throughout-gap
    먼저 사례 파일 "verification/cases/T09/case.json"의 "throughout-gap"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "work" 행동을 수행한다
    만일 "A" 역할이 "initial-observation" 행동을 수행한다
    만일 "A" 역할이 "final-observation" 행동을 수행한다
    만일 "A" 역할이 "assessment" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "assessment-api-result" assertion으로 "기간 내내100의 관측 throughout-gap. gap-result의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assessment-db-result" assertion으로 "기간 내내100의 관측 throughout-gap. gap-result의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "fixed-interval" assertion으로 "기간 내내100의 관측 throughout-gap. interval-policy의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "coverage-intervals" assertion으로 "기간 내내100의 관측 throughout-gap. interval-policy의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-inferred-satisfied" assertion으로 "기간 내내100의 관측 throughout-gap. inferred-continuity의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 기간 내내100의 관측 throughout-complete
    먼저 사례 파일 "verification/cases/T09/case.json"의 "throughout-complete"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "work" 행동을 수행한다
    만일 "A" 역할이 "initial-observation" 행동을 수행한다
    만일 "A" 역할이 "final-observation" 행동을 수행한다
    만일 "A" 역할이 "assessment" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "assessment-api-result" assertion으로 "기간 내내100의 관측 throughout-complete. throughout-result의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assessment-db-result" assertion으로 "기간 내내100의 관측 throughout-complete. throughout-result의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "fixed-interval" assertion으로 "기간 내내100의 관측 throughout-complete. sufficient-continuity-evidence의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "coverage-intervals" assertion으로 "기간 내내100의 관측 throughout-complete. sufficient-continuity-evidence의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "initial-held" assertion으로 "기간 내내100의 관측 throughout-complete. initial-held의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "final-held" assertion으로 "기간 내내100의 관측 throughout-complete. final-held의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-quantity-changing-events" assertion으로 "기간 내내100의 관측 throughout-complete. sufficient-continuity-evidence의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "qualifying-condition" assertion으로 "기간 내내100의 관측 throughout-complete. sufficient-continuity-evidence의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다
    먼저 사례 파일 "verification/cases/T09/case.json"의 "exists-versus-end-throughout"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "exists" 행동을 수행한다
    만일 "A" 역할이 "state" 행동을 수행한다
    만일 "A" 역할이 "throughout" 행동을 수행한다
    만일 "A" 역할이 "initial" 행동을 수행한다
    만일 "시스템" 역할이 "initial-db" 행동을 수행한다
    만일 "A" 역할이 "sale" 행동을 수행한다
    만일 "warehouse" 역할이 "reserve" 행동을 수행한다
    만일 "warehouse" 역할이 "pick" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "A" 역할이 "exists-assessment" 행동을 수행한다
    만일 "A" 역할이 "state-assessment" 행동을 수행한다
    만일 "A" 역할이 "throughout-assessment" 행동을 수행한다
    만일 "A" 역할이 "final" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "initial-raw100" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. initial-held의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "final-held0" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. final-held의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "final-no-leaf" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. same-trajectory-three-goals의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "exists-assessment-api-result" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. exists-in-result의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "exists-assessment-db-result" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. exists-in-result의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state-assessment-api-result" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. end-state-at-result의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state-assessment-db-result" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. end-state-at-result의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "throughout-assessment-api-result" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. throughout-result의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "throughout-assessment-db-result" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. throughout-result의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "exists-same-goal-fields" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. same-trajectory-three-goals의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "exists-same-input-scope" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. same-trajectory-three-goals의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state-same-goal-fields" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. same-trajectory-three-goals의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state-same-input-scope" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. same-trajectory-three-goals의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "throughout-same-goal-fields" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. same-trajectory-three-goals의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "throughout-same-input-scope" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. same-trajectory-three-goals의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state-same-assessment-input-snapshot" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. same-trajectory-three-goals의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "throughout-same-assessment-input-snapshot" assertion으로 "구간 처음100·끝0의 같은 실물 경로에서 세 목표를 구별한다. same-trajectory-three-goals의 독립 고정 기대값을 대조한다."를 확인한다
