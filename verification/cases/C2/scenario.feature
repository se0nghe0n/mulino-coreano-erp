# language: ko
@C2 @D09 @D12 @sit @uat
기능: 목표·의무·판정·접수 책임의 독립 계약 C2

  시나리오: 월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다
    먼저 사례 파일 "verification/cases/C2/case.json"의 "cumulative-versus-state"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "cumulative" 행동을 수행한다
    만일 "A" 역할이 "state" 행동을 수행한다
    만일 "warehouse" 역할이 "receive60" 행동을 수행한다
    만일 "A" 역할이 "sale60" 행동을 수행한다
    만일 "warehouse" 역할이 "reserve60" 행동을 수행한다
    만일 "warehouse" 역할이 "pick60" 행동을 수행한다
    만일 "시스템" 역할이 "clock-dispatch60" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch60" 행동을 수행한다
    만일 "시스템" 역할이 "clock-receive40" 행동을 수행한다
    만일 "warehouse" 역할이 "receive40" 행동을 수행한다
    만일 "시스템" 역할이 "clock-known" 행동을 수행한다
    만일 "A" 역할이 "cumulative-assessment" 행동을 수행한다
    만일 "A" 역할이 "state-assessment" 행동을 수행한다
    만일 "A" 역할이 "inventory" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "api-arrived100" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. cumulative-arrival의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "raw-receipt-sum100" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. cumulative-arrival의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "api-held40" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. state-at-held의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "raw-current-leaf-sum40" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. state-at-held의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "arrival-distinct-identities" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative-assessment-api-result" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. cumulative의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative-assessment-db-result" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. cumulative의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state-assessment-api-result" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. state-at의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state-assessment-db-result" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. state-at의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-goal-mode" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-quantity" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-unit" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-placeId" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-endpoint" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-evidencePolicyVersion" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-periodStart" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-periodEnd" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-eventKinds" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cumulative_event-field-distinctContributionScope" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-goal-mode" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-quantity" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-unit" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-placeId" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-endpoint" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-evidencePolicyVersion" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-evaluationTime" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-eligibilityAction" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "state_at-field-includeReservations" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "fixed-asof" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "fixed-knownat" assertion으로 "월60 수령·화60 출고·수40 수령의 누적100과 현재40을 구별한다. mode-evidence-scope의 독립 고정 기대값을 대조한다."를 확인한다
