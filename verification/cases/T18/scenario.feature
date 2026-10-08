# language: ko
@T18 @D18 @sit @uat
기능: 반품·양방향 trace·ADMIN 회수와 종료 대조

  시나리오: 인도60 뒤 반품20을 새 수령·보류로 연결하고 같은 실물 재접수 효과를 막는다
    먼저 사례 파일 "verification/cases/T18/case.json"의 "return-new-receipt"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "receiver" 역할이 "return-authorize" 행동을 수행한다
    만일 "receiver" 역할이 "return" 행동을 수행한다
    만일 "observer" 역할이 "before-duplicate" 행동을 수행한다
    만일 "시스템" 역할이 "before-duplicate-db" 행동을 수행한다
    만일 "receiver" 역할이 "duplicate-return" 행동을 수행한다
    만일 "observer" 역할이 "returned" 행동을 수행한다
    만일 "시스템" 역할이 "returned-db" 행동을 수행한다
    만일 "receiver" 역할이 "return-disposition" 행동을 수행한다
    그러면 "historical-delivery-1" assertion으로 "historical-delivery"를 확인한다
    그러면 "historical-delivery-2" assertion으로 "historical-delivery"를 확인한다
    그러면 "return-received-3" assertion으로 "return-received"를 확인한다
    그러면 "return-received-4" assertion으로 "return-received"를 확인한다
    그러면 "return-eligible-5" assertion으로 "return-eligible"를 확인한다
    그러면 "return-eligible-6" assertion으로 "return-eligible"를 확인한다
    그러면 "duplicate-return-effects-7" assertion으로 "duplicate-return-effects"를 확인한다
    그러면 "duplicate-return-effects-8" assertion으로 "duplicate-return-effects"를 확인한다
    그러면 "duplicate-return-effects-9" assertion으로 "duplicate-return-effects"를 확인한다
    그러면 "duplicate-return-effects-10" assertion으로 "duplicate-return-effects"를 확인한다
    그러면 "duplicate-return-effects-11" assertion으로 "duplicate-return-effects"를 확인한다
    그러면 "duplicate-return-effects-12" assertion으로 "duplicate-return-effects"를 확인한다
    그러면 "return-added-purchase-contribution-13" assertion으로 "return-added-purchase-contribution"를 확인한다
    그러면 "return-added-purchase-contribution-14" assertion으로 "return-added-purchase-contribution"를 확인한다
    그러면 "return-followup-15" assertion으로 "return-followup"를 확인한다
    그러면 "return-followup-16" assertion으로 "return-followup"를 확인한다

  시나리오: LOT 미식별 반품20의 임시 접수는 가용0과 대조 책임을 유지한다
    먼저 사례 파일 "verification/cases/T18/case.json"의 "unidentified-return-provisional"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "receiver" 역할이 "return-authorize" 행동을 수행한다
    만일 "receiver" 역할이 "return" 행동을 수행한다
    만일 "observer" 역할이 "provisional" 행동을 수행한다
    만일 "시스템" 역할이 "provisional-db" 행동을 수행한다
    그러면 "return-followup-1" assertion으로 "return-followup"를 확인한다
    그러면 "return-followup-2" assertion으로 "return-followup"를 확인한다
    그러면 "return-eligible-3" assertion으로 "return-eligible"를 확인한다
    그러면 "return-eligible-4" assertion으로 "return-eligible"를 확인한다
    그러면 "return-followup-5" assertion으로 "return-followup"를 확인한다
    그러면 "return-followup-6" assertion으로 "return-followup"를 확인한다
    그러면 "return-followup-7" assertion으로 "return-followup"를 확인한다
    그러면 "return-followup-8" assertion으로 "return-followup"를 확인한다
    그러면 "return-followup-9" assertion으로 "return-followup"를 확인한다
    그러면 "return-followup-10" assertion으로 "return-followup"를 확인한다
    그러면 "return-followup-11" assertion으로 "return-followup"를 확인한다

  시나리오: 양방향 LOT trace와 후보60·승인50·기관 통지 및 scope 변경 재승인을 확인한다
    먼저 사례 파일 "verification/cases/T18/case.json"의 "trace-and-recall-decisions"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "sales" 역할이 "order" 행동을 수행한다
    만일 "sales" 역할이 "reserve" 행동을 수행한다
    만일 "sales" 역할이 "pick" 행동을 수행한다
    만일 "sales" 역할이 "dispatch" 행동을 수행한다
    만일 "receiver" 역할이 "delivery" 행동을 수행한다
    만일 "admin" 역할이 "investigate" 행동을 수행한다
    만일 "admin" 역할이 "recall-proposal" 행동을 수행한다
    만일 "admin" 역할이 "recall-approve" 행동을 수행한다
    만일 "observer" 역할이 "trace" 행동을 수행한다
    만일 "시스템" 역할이 "trace-db" 행동을 수행한다
    만일 "observer" 역할이 "before-unauthorized" 행동을 수행한다
    만일 "시스템" 역할이 "before-unauthorized-db" 행동을 수행한다
    만일 "operator" 역할이 "unauthorized" 행동을 수행한다
    만일 "observer" 역할이 "after-unauthorized" 행동을 수행한다
    만일 "시스템" 역할이 "after-unauthorized-db" 행동을 수행한다
    만일 "admin" 역할이 "notice" 행동을 수행한다
    만일 "admin" 역할이 "changed-scope" 행동을 수행한다
    만일 "admin" 역할이 "old-approval-new-notice" 행동을 수행한다
    만일 "observer" 역할이 "recall" 행동을 수행한다
    만일 "시스템" 역할이 "recall-db" 행동을 수행한다
    그러면 "unauthorized-recall-decision-1" assertion으로 "unauthorized-recall-decision"를 확인한다
    그러면 "unauthorized-recall-decision-2" assertion으로 "unauthorized-recall-decision"를 확인한다
    그러면 "unauthorized-recall-decision-3" assertion으로 "unauthorized-recall-decision"를 확인한다
    그러면 "unauthorized-recall-decision-4" assertion으로 "unauthorized-recall-decision"를 확인한다
    그러면 "unauthorized-recall-decision-5" assertion으로 "응답의 구조화 오류 코드 /response/error/code가 FORBIDDEN다(계획 §3.4, contracts/command-response.schema.json)."를 확인한다
    그러면 "bidirectional-trace-6" assertion으로 "bidirectional-trace"를 확인한다
    그러면 "bidirectional-trace-7" assertion으로 "bidirectional-trace"를 확인한다
    그러면 "bidirectional-trace-8" assertion으로 "bidirectional-trace"를 확인한다
    그러면 "bidirectional-trace-9" assertion으로 "bidirectional-trace"를 확인한다
    그러면 "bidirectional-trace-10" assertion으로 "bidirectional-trace"를 확인한다
    그러면 "approval-notice-11" assertion으로 "approval-notice"를 확인한다
    그러면 "approval-notice-12" assertion으로 "approval-notice"를 확인한다
    그러면 "approval-notice-13" assertion으로 "approval-notice"를 확인한다
    그러면 "approval-notice-14" assertion으로 "approval-notice"를 확인한다
    그러면 "approval-notice-15" assertion으로 "approval-notice"를 확인한다

  시나리오: ADMIN scope50의 회수25·동일 실물 폐기25와 미확인25는 처리25다
    먼저 사례 파일 "verification/cases/T18/case.json"의 "recall-closure-accounting"를 준비한다
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
    그러면 "recovery-distinct-1" assertion으로 "recovery-distinct"를 확인한다
    그러면 "recovery-distinct-2" assertion으로 "recovery-distinct"를 확인한다
    그러면 "processed-distinct-3" assertion으로 "processed-distinct"를 확인한다
    그러면 "processed-distinct-4" assertion으로 "processed-distinct"를 확인한다
    그러면 "unknown-5" assertion으로 "unknown"를 확인한다
    그러면 "unknown-6" assertion으로 "unknown"를 확인한다
    그러면 "false-closure-7" assertion으로 "false-closure"를 확인한다
    그러면 "false-closure-8" assertion으로 "false-closure"를 확인한다
    그러면 "false-closure-9" assertion으로 "false-closure"를 확인한다
    그러면 "false-closure-10" assertion으로 "false-closure"를 확인한다
    그러면 "false-closure-11" assertion으로 "false-closure"를 확인한다
    그러면 "exclusive-endstates-12" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-13" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-14" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-15" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-16" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-17" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-18" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-19" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-20" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-21" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-22" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-23" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-24" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-25" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-26" assertion으로 "exclusive-endstates"를 확인한다
    그러면 "exclusive-endstates-27" assertion으로 "exclusive-endstates"를 확인한다
