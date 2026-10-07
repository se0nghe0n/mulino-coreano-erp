# language: ko
@T06 @D06 @sit @uat @contract-red
기능: 과거에 알던 사실과 현재 정정·시간·미확인 상태를 구별한다

  시나리오: 100 기록을98로 정정해 당시100과 현재98을 읽는다
    먼저 사례 파일 "verification/cases/T06/case.json"의 "known-at-correction"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "record-original" 행동을 수행한다
    만일 "recorder" 역할이 "then" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "시스템" 역할이 "later" 행동을 수행한다
    만일 "recorder" 역할이 "correct" 행동을 수행한다
    만일 "recorder" 역할이 "then-again" 행동을 수행한다
    만일 "recorder" 역할이 "current" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "then-again-quantity" assertion으로 "then again quantity의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "current-quantity" assertion으로 "current quantity의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "original-immutable" assertion으로 "original immutable의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "utc-instant" assertion으로 "utc instant의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "preserve-originalOffset" assertion으로 "preserve originalOffset의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "preserve-timezone" assertion으로 "preserve timezone의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "preserve-precision" assertion으로 "preserve precision의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "revision-pair" assertion으로 "revision pair의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "occurrence-recording-time-pair" assertion으로 "occurrence recording time pair의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 기출고100의98 정정은 가짜 과거 이동 없이 대조로 남는다
    먼저 사례 파일 "verification/cases/T06/case.json"의 "inconsistent-after-dispatch"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "recorder" 역할이 "correct" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    만일 "intake" 역할이 "api-duty-STOCK_RECONCILIATION" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch-pending" 행동을 수행한다
    만일 "recorder" 역할이 "pending-check" 행동을 수행한다
    만일 "시스템" 역할이 "pending-db" 행동을 수행한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "segments-unchanged" assertion으로 "segments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "allocations-unchanged" assertion으로 "allocations unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "projection-pending" assertion으로 "projection pending의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "STOCK_RECONCILIATION-one-assignment" assertion으로 "STOCK_RECONCILIATION one assignment의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "STOCK_RECONCILIATION-responsibility" assertion으로 "STOCK_RECONCILIATION responsibility의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "STOCK_RECONCILIATION-human-owner" assertion으로 "STOCK_RECONCILIATION human owner의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "STOCK_RECONCILIATION-api-responsibility" assertion으로 "STOCK_RECONCILIATION api responsibility의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "pending-rejection" assertion으로 "pending rejection의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "pending-movements" assertion으로 "pending movements의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "pending-allocations" assertion으로 "pending allocations의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "pending-outbox" assertion으로 "pending outbox의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 기한과 같은 사건 시각의 포함 끝점을 검증한다
    먼저 사례 파일 "verification/cases/T06/case.json"의 "inclusive-deadline"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "record" 행동을 수행한다
    만일 "reconciler" 역할이 "match" 행동을 수행한다
    만일 "reconciler" 역할이 "link" 행동을 수행한다
    만일 "recorder" 역할이 "confirm" 행동을 수행한다
    만일 "recorder" 역할이 "assessment" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "deadline-status" assertion으로 "deadline status의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "deadline-meaning" assertion으로 "deadline meaning의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 기한과 같은 사건 시각의 제외 끝점을 검증한다
    먼저 사례 파일 "verification/cases/T06/case.json"의 "exclusive-deadline"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "record" 행동을 수행한다
    만일 "reconciler" 역할이 "match" 행동을 수행한다
    만일 "reconciler" 역할이 "link" 행동을 수행한다
    만일 "recorder" 역할이 "confirm" 행동을 수행한다
    만일 "recorder" 역할이 "assessment" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "deadline-status" assertion으로 "deadline status의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "deadline-meaning" assertion으로 "deadline meaning의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 날짜만 알려진 사건이 기한을 걸치면 미확인이다
    먼저 사례 파일 "verification/cases/T06/case.json"의 "date-only-range"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "record" 행동을 수행한다
    만일 "reconciler" 역할이 "match" 행동을 수행한다
    만일 "reconciler" 역할이 "link" 행동을 수행한다
    만일 "recorder" 역할이 "confirm" 행동을 수행한다
    만일 "recorder" 역할이 "evidence" 행동을 수행한다
    만일 "recorder" 역할이 "assessment" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "date-range" assertion으로 "date range의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "date-precision" assertion으로 "date precision의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "date-unverified" assertion으로 "date unverified의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "date-known-receipt-100" assertion으로 "date known receipt 100의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: 미입력·조사전·적용안됨·상충을0으로 합치지 않는다
    먼저 사례 파일 "verification/cases/T06/case.json"의 "four-states"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "view-before" 행동을 수행한다
    만일 "시스템" 역할이 "db-before" 행동을 수행한다
    만일 "recorder" 역할이 "states" 행동을 수행한다
    만일 "recorder" 역할이 "view-after" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "response-states" assertion으로 "response states의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "raw-states" assertion으로 "raw states의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "claims-unchanged" assertion으로 "claims unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "movements-unchanged" assertion으로 "movements unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "segments-unchanged" assertion으로 "segments unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "allocations-unchanged" assertion으로 "allocations unchanged의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "state-0-not-known-zero" assertion으로 "state 0 not known zero의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "state-1-not-known-zero" assertion으로 "state 1 not known zero의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "state-2-not-known-zero" assertion으로 "state 2 not known zero의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "state-3-not-known-zero" assertion으로 "state 3 not known zero의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "conflict-values" assertion으로 "conflict values의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: typed all 판정과 상충 flag를 독립 검증한다
    먼저 사례 파일 "verification/cases/T06/case.json"의 "all-false-conflict"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "create-goal" 행동을 수행한다
    만일 "recorder" 역할이 "assessment" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "predicate-result" assertion으로 "predicate result의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "predicate-conflict" assertion으로 "predicate conflict의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "raw-predicate" assertion으로 "raw predicate의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "no-physical-inference" assertion으로 "no physical inference의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: typed all 판정과 상충 flag를 독립 검증한다
    먼저 사례 파일 "verification/cases/T06/case.json"의 "all-true-conflict"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "create-goal" 행동을 수행한다
    만일 "recorder" 역할이 "assessment" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "predicate-result" assertion으로 "predicate result의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "predicate-conflict" assertion으로 "predicate conflict의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "raw-predicate" assertion으로 "raw predicate의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "no-physical-inference" assertion으로 "no physical inference의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: typed any 판정과 상충 flag를 독립 검증한다
    먼저 사례 파일 "verification/cases/T06/case.json"의 "any-true-conflict"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "create-goal" 행동을 수행한다
    만일 "recorder" 역할이 "assessment" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "predicate-result" assertion으로 "predicate result의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "predicate-conflict" assertion으로 "predicate conflict의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "raw-predicate" assertion으로 "raw predicate의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "no-physical-inference" assertion으로 "no physical inference의 실제 값과 범위를 대조한다"를 확인한다

  시나리오: typed not 판정과 상충 flag를 독립 검증한다
    먼저 사례 파일 "verification/cases/T06/case.json"의 "not-unknown"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "recorder" 역할이 "create-goal" 행동을 수행한다
    만일 "recorder" 역할이 "assessment" 행동을 수행한다
    만일 "시스템" 역할이 "db-after" 행동을 수행한다
    그러면 "predicate-result" assertion으로 "predicate result의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "predicate-conflict" assertion으로 "predicate conflict의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "raw-predicate" assertion으로 "raw predicate의 실제 값과 범위를 대조한다"를 확인한다
    그러면 "no-physical-inference" assertion으로 "no physical inference의 실제 값과 범위를 대조한다"를 확인한다
