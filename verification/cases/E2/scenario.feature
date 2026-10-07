# language: ko
@E2 @D03 @D16 @D18 @sit @uat
기능: 겹친 제한과 중복 없는 회수 처분·예외 책임

  시나리오: 동일 LOT60의 QC20만 해제해도 회수 조사60 제한과 조사 책임은 남는다
    먼저 사례 파일 "verification/cases/E2/case.json"의 "overlapping-holds"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "procurement" 역할이 "split" 행동을 수행한다
    만일 "qc" 역할이 "qc-hold20" 행동을 수행한다
    만일 "admin" 역할이 "investigate" 행동을 수행한다
    만일 "admin" 역할이 "recall-hold60" 행동을 수행한다
    만일 "qc" 역할이 "qc-release20" 행동을 수행한다
    만일 "observer" 역할이 "before-dispatch" 행동을 수행한다
    만일 "시스템" 역할이 "before-dispatch-db" 행동을 수행한다
    만일 "sales" 역할이 "dispatch60" 행동을 수행한다
    만일 "observer" 역할이 "after-dispatch" 행동을 수행한다
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
    그러면 "status-axes-14" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-15" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-16" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-17" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-18" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-19" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-20" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-21" assertion으로 "status-axes"를 확인한다
    그러면 "status-axes-22" assertion으로 "status-axes"를 확인한다

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
    만일 "시스템" 역할이 "after-operator-db" 행동을 수행한다
    만일 "admin" 역할이 "admin-exception" 행동을 수행한다
    만일 "observer" 역할이 "closed" 행동을 수행한다
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
