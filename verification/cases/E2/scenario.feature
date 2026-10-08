# language: ko
@E2 @D03 @D16 @D18 @sit @uat
기능: 겹친 제한과 중복 없는 회수 처분·예외 책임

  시나리오: 동일 LOT60의 QC20만 해제해도 회수 조사60 제한과 조사 책임은 남는다
    먼저 사례 파일 "verification/cases/E2/case.json"의 "overlapping-holds"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "procurement" 역할이 "split" 행동을 수행한다
    만일 "sales" 역할이 "pick20" 행동을 수행한다
    만일 "sales" 역할이 "pick40" 행동을 수행한다
    만일 "qc" 역할이 "qc-hold20" 행동을 수행한다
    만일 "admin" 역할이 "investigate" 행동을 수행한다
    만일 "admin" 역할이 "recall-hold60" 행동을 수행한다
    만일 "observer" 역할이 "before-release" 행동을 수행한다
    만일 "시스템" 역할이 "before-release-db" 행동을 수행한다
    만일 "qc" 역할이 "qc-release20" 행동을 수행한다
    만일 "observer" 역할이 "before-dispatch" 행동을 수행한다
    만일 "시스템" 역할이 "before-dispatch-db" 행동을 수행한다
    만일 "sales" 역할이 "dispatch20-target" 행동을 수행한다
    만일 "sales" 역할이 "dispatch20" 행동을 수행한다
    만일 "sales" 역할이 "dispatch40-target" 행동을 수행한다
    만일 "sales" 역할이 "dispatch40" 행동을 수행한다
    만일 "observer" 역할이 "after-dispatch" 행동을 수행한다
    만일 "observer" 역할이 "after-dispatch-mcp" 행동을 수행한다
    만일 "시스템" 역할이 "after-dispatch-db" 행동을 수행한다
    그러면 "physical-held-1" assertion으로 "physical-held"를 확인한다
    그러면 "physical-held-2" assertion으로 "physical-held"를 확인한다
    그러면 "dispatched-after-QC-only-release-3" assertion으로 "dispatched-after-QC-only-release"를 확인한다
    그러면 "dispatched-after-QC-only-release-4" assertion으로 "dispatched-after-QC-only-release"를 확인한다
    그러면 "dispatched-after-QC-only-release-5" assertion으로 "dispatched-after-QC-only-release"를 확인한다
    그러면 "dispatched-after-QC-only-release-6" assertion으로 "dispatched-after-QC-only-release"를 확인한다
    그러면 "dispatched-after-QC-only-release-7" assertion으로 "dispatched-after-QC-only-release"를 확인한다
    그러면 "recall-hold-8" assertion으로 "recall-hold"를 확인한다
    그러면 "recall-hold-9" assertion으로 "recall-hold"를 확인한다
    그러면 "candidate-as-confirmed-contamination-10" assertion으로 "candidate-as-confirmed-contamination"를 확인한다
    그러면 "candidate-as-confirmed-contamination-11" assertion으로 "candidate-as-confirmed-contamination"를 확인한다
    그러면 "candidate-as-confirmed-contamination-12" assertion으로 "candidate-as-confirmed-contamination"를 확인한다
    그러면 "investigation-duty-13" assertion으로 "investigation-duty"를 확인한다
    그러면 "investigation-duty-14" assertion으로 "investigation-duty"를 확인한다
    그러면 "investigation-duty-15" assertion으로 "investigation-duty"를 확인한다
    그러면 "investigation-duty-16" assertion으로 "investigation-duty"를 확인한다
    그러면 "investigation-duty-17" assertion으로 "investigation-duty"를 확인한다
    그러면 "investigation-duty-18" assertion으로 "investigation-duty"를 확인한다
    그러면 "investigation-duty-19" assertion으로 "investigation-duty"를 확인한다
    그러면 "qc-release-applied" assertion으로 "QC20 해제는 응답 APPLIED와 같은 restriction ID·실물20·BOX·RELEASED 원행으로 확인한다. 회수 조사60은 ACTIVE이며 출고 효과0이다."를 확인한다
    그러면 "qc-release-same-restriction" assertion으로 "QC20 해제는 응답 APPLIED와 같은 restriction ID·실물20·BOX·RELEASED 원행으로 확인한다. 회수 조사60은 ACTIVE이며 출고 효과0이다."를 확인한다
    그러면 "qc20-released-before-dispatch" assertion으로 "QC20 해제는 응답 APPLIED와 같은 restriction ID·실물20·BOX·RELEASED 원행으로 확인한다. 회수 조사60은 ACTIVE이며 출고 효과0이다."를 확인한다
    그러면 "qc20-remains-released-after-dispatch" assertion으로 "QC20 해제는 응답 APPLIED와 같은 restriction ID·실물20·BOX·RELEASED 원행으로 확인한다. 회수 조사60은 ACTIVE이며 출고 효과0이다."를 확인한다
    그러면 "recall60-scope-quantity-remains-active" assertion으로 "QC20 해제는 응답 APPLIED와 같은 restriction ID·실물20·BOX·RELEASED 원행으로 확인한다. 회수 조사60은 ACTIVE이며 출고 효과0이다."를 확인한다
    그러면 "qc20-active-before-release" assertion으로 "해제 직전 같은 restriction ID와 분할 물량20·BOX의 QC_REVIEW가 ACTIVE였음을 독립 원행으로 확인한다. 이미 RELEASED였던 행이나 no-op을 실제 해제로 세지 않는다."를 확인한다
    그러면 "dispatch20-rejected" assertion으로 "출고20: QC20만 해제한 뒤 회수 보류가 남은 같은 형식의 출고는 REJECTED다"를 확인한다
    그러면 "dispatch20-reason" assertion으로 "출고20: 출고 거부 이유는 INSUFFICIENT_ELIGIBLE_QUANTITY다"를 확인한다
    그러면 "dispatch20-denial-audit" assertion으로 "출고20: 출고 거부 감사가 정확히 1건 남는다"를 확인한다
    그러면 "dispatch40-rejected" assertion으로 "출고40: QC20만 해제한 뒤 회수 보류가 남은 같은 형식의 출고는 REJECTED다"를 확인한다
    그러면 "dispatch40-reason" assertion으로 "출고40: 출고 거부 이유는 INSUFFICIENT_ELIGIBLE_QUANTITY다"를 확인한다
    그러면 "dispatch40-denial-audit" assertion으로 "출고40: 출고 거부 감사가 정확히 1건 남는다"를 확인한다
    그러면 "recall-suspends-untouched40-before" assertion으로 "출고 전: QC가 닿지 않은 자식40의 배분은 회수 보류만으로 SUSPENDED이고 소비되지 않는다"를 확인한다
    그러면 "recall-suspends-untouched40-after" assertion으로 "출고 시도 뒤: QC가 닿지 않은 자식40의 배분은 회수 보류만으로 SUSPENDED이고 소비되지 않는다"를 확인한다
    그러면 "after-dispatch-mcp-same-snapshot" assertion으로 "MCP 조회는 API 조회와 같은 snapshot을 읽는다"를 확인한다
    그러면 "physical-held-1-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 physical-held-1와 같은 값을 읽는다"를 확인한다
    그러면 "dispatched-after-QC-only-release-3-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 dispatched-after-QC-only-release-3와 같은 값을 읽는다"를 확인한다
    그러면 "dispatched-after-QC-only-release-6-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 dispatched-after-QC-only-release-6와 같은 값을 읽는다"를 확인한다
    그러면 "dispatched-after-QC-only-release-7-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 dispatched-after-QC-only-release-7와 같은 값을 읽는다"를 확인한다
    그러면 "recall-hold-8-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 recall-hold-8와 같은 값을 읽는다"를 확인한다
    그러면 "candidate-as-confirmed-contamination-12-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 candidate-as-confirmed-contamination-12와 같은 값을 읽는다"를 확인한다
    그러면 "investigation-duty-19-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 investigation-duty-19와 같은 값을 읽는다"를 확인한다

  시나리오: 회수 보류가 없으면 같은 QC20 해제 뒤 같은 형식의 출고40이 적용된다(대조군)
    먼저 사례 파일 "verification/cases/E2/case.json"의 "qc-release-only-control"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "procurement" 역할이 "split" 행동을 수행한다
    만일 "sales" 역할이 "pick20" 행동을 수행한다
    만일 "sales" 역할이 "pick40" 행동을 수행한다
    만일 "qc" 역할이 "qc-hold20" 행동을 수행한다
    만일 "observer" 역할이 "before-release" 행동을 수행한다
    만일 "시스템" 역할이 "before-release-db" 행동을 수행한다
    만일 "qc" 역할이 "qc-release20" 행동을 수행한다
    만일 "observer" 역할이 "before-dispatch" 행동을 수행한다
    만일 "시스템" 역할이 "before-dispatch-db" 행동을 수행한다
    만일 "sales" 역할이 "dispatch40-target" 행동을 수행한다
    만일 "sales" 역할이 "dispatch40" 행동을 수행한다
    만일 "observer" 역할이 "after-dispatch" 행동을 수행한다
    만일 "observer" 역할이 "after-dispatch-mcp" 행동을 수행한다
    만일 "시스템" 역할이 "after-dispatch-db" 행동을 수행한다
    그러면 "control-dispatch40-applied" assertion으로 "대조군: 회수 보류가 없으면 같은 형식의 출고40은 APPLIED다"를 확인한다
    그러면 "control-warehouse-dispatch40" assertion으로 "대조군: 창고 출고량이 40 BOX 늘어난다"를 확인한다
    그러면 "control-dispatch40-movement" assertion으로 "대조군: 출고40의 실제 이동 원행 합계가 40 BOX다"를 확인한다
    그러면 "control-allocation40-consumed" assertion으로 "대조군: 자식40의 배분은 CONSUMED다"를 확인한다
    그러면 "control-qc-release-applied" assertion으로 "대조군: QC20 해제는 APPLIED다"를 확인한다
    그러면 "after-dispatch-mcp-same-snapshot" assertion으로 "MCP 조회는 API 조회와 같은 snapshot을 읽는다"를 확인한다
    그러면 "control-warehouse-dispatch40-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 control-warehouse-dispatch40와 같은 값을 읽는다"를 확인한다

  시나리오: 승인50 중 회수25와 동일 실물 폐기25는 최종25이며 미확인25의 종료를 막는다
    먼저 사례 파일 "verification/cases/E2/case.json"의 "same-25-not-50"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "admin" 역할이 "investigate" 행동을 수행한다
    만일 "admin" 역할이 "recall-proposal" 행동을 수행한다
    만일 "admin" 역할이 "recall-approve" 행동을 수행한다
    만일 "receiver" 역할이 "recover25" 행동을 수행한다
    만일 "admin" 역할이 "recall-dispose-clearance" 행동을 수행한다
    만일 "manager" 역할이 "dispose-decision" 행동을 수행한다
    만일 "manager" 역할이 "dispose25" 행동을 수행한다
    만일 "observer" 역할이 "before-close" 행동을 수행한다
    만일 "시스템" 역할이 "before-close-db" 행동을 수행한다
    만일 "admin" 역할이 "false-close" 행동을 수행한다
    만일 "observer" 역할이 "after-close" 행동을 수행한다
    만일 "observer" 역할이 "after-close-mcp" 행동을 수행한다
    만일 "시스템" 역할이 "after-close-db" 행동을 수행한다
    그러면 "unique-recovered-1" assertion으로 "unique-recovered"를 확인한다
    그러면 "unique-recovered-2" assertion으로 "unique-recovered"를 확인한다
    그러면 "unique-finally-processed-3" assertion으로 "unique-finally-processed"를 확인한다
    그러면 "unique-finally-processed-4" assertion으로 "unique-finally-processed"를 확인한다
    그러면 "unknown-5" assertion으로 "unknown"를 확인한다
    그러면 "unknown-6" assertion으로 "unknown"를 확인한다
    그러면 "25-plus-25-equals50-7" assertion으로 "25-plus-25-equals50"를 확인한다
    그러면 "25-plus-25-equals50-8" assertion으로 "25-plus-25-equals50"를 확인한다
    그러면 "unapproved-unknown-close-9" assertion으로 "unapproved-unknown-close"를 확인한다
    그러면 "unapproved-unknown-close-10" assertion으로 "unapproved-unknown-close"를 확인한다
    그러면 "unapproved-unknown-close-11" assertion으로 "unapproved-unknown-close"를 확인한다
    그러면 "unapproved-unknown-close-12" assertion으로 "unapproved-unknown-close"를 확인한다
    그러면 "unapproved-unknown-close-13" assertion으로 "unapproved-unknown-close"를 확인한다
    그러면 "unapproved-unknown-close-reason" assertion으로 "정상 형식의 종료 요청도 미확인 잔여25 때문에 RECALL_RESIDUAL_UNKNOWN으로 거부된다"를 확인한다
    그러면 "unapproved-unknown-close-audit" assertion으로 "거짓 종료 거부 감사가 정확히 1건 남는다"를 확인한다
    그러면 "status-axes-14" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-15" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-16" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-17" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-18" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-19" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-20" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-21" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-22" assertion으로 "status-axes"를 확인한다
    그러면 "after-close-mcp-same-snapshot" assertion으로 "MCP 조회는 API 조회와 같은 snapshot을 읽는다"를 확인한다
    그러면 "unique-recovered-1-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 unique-recovered-1와 같은 값을 읽는다"를 확인한다
    그러면 "unique-finally-processed-3-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 unique-finally-processed-3와 같은 값을 읽는다"를 확인한다
    그러면 "unknown-5-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 unknown-5와 같은 값을 읽는다"를 확인한다
    그러면 "25-plus-25-equals50-8-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 25-plus-25-equals50-8와 같은 값을 읽는다"를 확인한다
    그러면 "unapproved-unknown-close-11-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 unapproved-unknown-close-11와 같은 값을 읽는다"를 확인한다
    그러면 "unapproved-unknown-close-12-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 unapproved-unknown-close-12와 같은 값을 읽는다"를 확인한다
    그러면 "status-axes-14-api" assertion으로 "API 진입점: 처리25 BOX는 DISPOSED·W·재고없음, 미확인25 BOX는 UNKNOWN·CUSTOMER·UNVERIFIED로 두 축이 따로 보인다"를 확인한다
    그러면 "status-axes-14-mcp" assertion으로 "MCP 진입점: 처리25 BOX는 DISPOSED·W·재고없음, 미확인25 BOX는 UNKNOWN·CUSTOMER·UNVERIFIED로 두 축이 따로 보인다"를 확인한다

  시나리오: 일반 역할의 예외 종료0 뒤 ADMIN 근거 있는 미확인25 예외 종료에도 잔여 책임은 보인다
    먼저 사례 파일 "verification/cases/E2/case.json"의 "exception-responsibility"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "admin" 역할이 "investigate" 행동을 수행한다
    만일 "admin" 역할이 "recall-proposal" 행동을 수행한다
    만일 "admin" 역할이 "recall-approve" 행동을 수행한다
    만일 "receiver" 역할이 "recover25" 행동을 수행한다
    만일 "admin" 역할이 "recall-dispose-clearance" 행동을 수행한다
    만일 "manager" 역할이 "dispose-decision" 행동을 수행한다
    만일 "manager" 역할이 "dispose25" 행동을 수행한다
    만일 "observer" 역할이 "before-exception" 행동을 수행한다
    만일 "시스템" 역할이 "before-exception-db" 행동을 수행한다
    만일 "operator" 역할이 "operator-exception" 행동을 수행한다
    만일 "observer" 역할이 "after-operator" 행동을 수행한다
    만일 "observer" 역할이 "after-operator-mcp" 행동을 수행한다
    만일 "시스템" 역할이 "after-operator-db" 행동을 수행한다
    만일 "admin" 역할이 "admin-exception" 행동을 수행한다
    만일 "observer" 역할이 "closed" 행동을 수행한다
    만일 "observer" 역할이 "closed-mcp" 행동을 수행한다
    만일 "시스템" 역할이 "closed-db" 행동을 수행한다
    그러면 "unauthorized-exception-close-1" assertion으로 "unauthorized-exception-close"를 확인한다
    그러면 "unauthorized-exception-close-2" assertion으로 "unauthorized-exception-close"를 확인한다
    그러면 "unauthorized-exception-close-3" assertion으로 "unauthorized-exception-close"를 확인한다
    그러면 "unauthorized-exception-close-4" assertion으로 "unauthorized-exception-close"를 확인한다
    그러면 "unauthorized-exception-close-5" assertion으로 "unauthorized-exception-close"를 확인한다
    그러면 "unauthorized-exception-close-6" assertion으로 "unauthorized-exception-close"를 확인한다
    그러면 "unauthorized-exception-close-7" assertion으로 "unauthorized-exception-close"를 확인한다
    그러면 "exception-unresolved-scope-8" assertion으로 "exception-unresolved-scope"를 확인한다
    그러면 "exception-unresolved-scope-9" assertion으로 "exception-unresolved-scope"를 확인한다
    그러면 "exception-residual-duty-10" assertion으로 "exception-residual-duty"를 확인한다
    그러면 "exception-residual-duty-11" assertion으로 "exception-residual-duty"를 확인한다
    그러면 "exception-residual-duty-12" assertion으로 "exception-residual-duty"를 확인한다
    그러면 "exception-residual-duty-13" assertion으로 "exception-residual-duty"를 확인한다
    그러면 "exception-residual-duty-14" assertion으로 "exception-residual-duty"를 확인한다
    그러면 "exception-residual-duty-15" assertion으로 "exception-residual-duty"를 확인한다
    그러면 "exception-residual-duty-16" assertion으로 "exception-residual-duty"를 확인한다
    그러면 "exception-evidence-17" assertion으로 "exception-evidence"를 확인한다
    그러면 "exception-evidence-18" assertion으로 "exception-evidence"를 확인한다
    그러면 "exception-evidence-19" assertion으로 "exception-evidence"를 확인한다
    그러면 "exception-evidence-20" assertion으로 "exception-evidence"를 확인한다
    그러면 "exception-evidence-21" assertion으로 "exception-evidence"를 확인한다
    그러면 "exception-evidence-22" assertion으로 "exception-evidence"를 확인한다
    그러면 "exception-evidence-23" assertion으로 "exception-evidence"를 확인한다
    그러면 "exception-evidence-24" assertion으로 "exception-evidence"를 확인한다
    그러면 "exception-evidence-25" assertion으로 "exception-evidence"를 확인한다
    그러면 "exception-evidence-26" assertion으로 "exception-evidence"를 확인한다
    그러면 "exception-evidence-27" assertion으로 "exception-evidence"를 확인한다
    그러면 "exception-evidence-28" assertion으로 "exception-evidence"를 확인한다
    그러면 "exception-evidence-29" assertion으로 "exception-evidence"를 확인한다
    그러면 "closed-mcp-same-snapshot" assertion으로 "MCP 조회는 API 조회와 같은 snapshot을 읽는다"를 확인한다
    그러면 "exception-unresolved-scope-8-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 exception-unresolved-scope-8와 같은 값을 읽는다"를 확인한다
    그러면 "exception-residual-duty-16-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 exception-residual-duty-16와 같은 값을 읽는다"를 확인한다
    그러면 "exception-evidence-21-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 exception-evidence-21와 같은 값을 읽는다"를 확인한다
    그러면 "exception-evidence-22-mcp" assertion으로 "MCP 진입점: 같은 snapshot에서 exception-evidence-22와 같은 값을 읽는다"를 확인한다
    그러면 "after-operator-mcp-same-snapshot" assertion으로 "MCP 조회는 예외 종료 거부 뒤 API 조회와 같은 snapshot을 읽는다"를 확인한다
    그러면 "unauthorized-exception-close-4-mcp" assertion으로 "MCP 진입점: 일반 역할의 예외 종료 거부 뒤 decisions는 시도 전과 같아 효과0이다"를 확인한다
    그러면 "unauthorized-exception-close-5-mcp" assertion으로 "MCP 진입점: 일반 역할의 예외 종료 거부 뒤 recallScopes는 시도 전과 같아 효과0이다"를 확인한다
    그러면 "unauthorized-exception-close-6-mcp" assertion으로 "MCP 진입점: 일반 역할의 예외 종료 거부 뒤 obligations는 시도 전과 같아 효과0이다"를 확인한다
