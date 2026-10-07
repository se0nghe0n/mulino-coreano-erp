# language: ko
@T11 @D11 @sit @uat
기능: 목표·의무·판정·접수 책임의 독립 계약 T11

  시나리오: 자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 CANCELLED
    먼저 사례 파일 "verification/cases/T11/case.json"의 "parent90-cancelled"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "parent" 행동을 수행한다
    만일 "A" 역할이 "child30" 행동을 수행한다
    만일 "A" 역할이 "child60" 행동을 수행한다
    만일 "A" 역할이 "receive90" 행동을 수행한다
    만일 "A" 역할이 "child30-link" 행동을 수행한다
    만일 "A" 역할이 "child30-assessment" 행동을 수행한다
    만일 "A" 역할이 "child30-close" 행동을 수행한다
    만일 "A" 역할이 "child60-link" 행동을 수행한다
    만일 "A" 역할이 "child60-assessment" 행동을 수행한다
    만일 "A" 역할이 "child60-close" 행동을 수행한다
    만일 "A" 역할이 "parent-assessment" 행동을 수행한다
    만일 "A" 역할이 "before-transfer-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-transfer-db" 행동을 수행한다
    만일 "A" 역할이 "followup" 행동을 수행한다
    만일 "A" 역할이 "deficit-transfer" 행동을 수행한다
    만일 "A" 역할이 "before-false-close-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-false-close-db" 행동을 수행한다
    만일 "A" 역할이 "false-close" 행동을 수행한다
    만일 "A" 역할이 "rejected-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "rejected-db" 행동을 수행한다
    만일 "A" 역할이 "alternative-close" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "false-close-outcome" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 CANCELLED. false-fulfilled-closure의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "false-close-movements-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "false-close-approvals-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "false-close-outbox-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "false-close-denial-audit" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 CANCELLED. false-fulfilled-closure의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-still-active" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 CANCELLED. false-fulfilled-closure의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-contribution90" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 CANCELLED. parent-contribution의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-assessment-api-result" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 CANCELLED. parent-assessment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-assessment-db-result" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 CANCELLED. parent-assessment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "alternative-applied" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 CANCELLED. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "alternative-reason" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 CANCELLED. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "unsatisfied-assessment-preserved" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "successor-owner-one" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 CANCELLED. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "successor-owner-owner" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 CANCELLED. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "successor-owner-next-action" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 CANCELLED. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "successor-owner-next-check" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 CANCELLED. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 IMPOSSIBLE
    먼저 사례 파일 "verification/cases/T11/case.json"의 "parent90-impossible"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "parent" 행동을 수행한다
    만일 "A" 역할이 "child30" 행동을 수행한다
    만일 "A" 역할이 "child60" 행동을 수행한다
    만일 "A" 역할이 "receive90" 행동을 수행한다
    만일 "A" 역할이 "child30-link" 행동을 수행한다
    만일 "A" 역할이 "child30-assessment" 행동을 수행한다
    만일 "A" 역할이 "child30-close" 행동을 수행한다
    만일 "A" 역할이 "child60-link" 행동을 수행한다
    만일 "A" 역할이 "child60-assessment" 행동을 수행한다
    만일 "A" 역할이 "child60-close" 행동을 수행한다
    만일 "A" 역할이 "parent-assessment" 행동을 수행한다
    만일 "A" 역할이 "before-transfer-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-transfer-db" 행동을 수행한다
    만일 "A" 역할이 "followup" 행동을 수행한다
    만일 "A" 역할이 "deficit-transfer" 행동을 수행한다
    만일 "A" 역할이 "before-false-close-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-false-close-db" 행동을 수행한다
    만일 "A" 역할이 "false-close" 행동을 수행한다
    만일 "A" 역할이 "rejected-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "rejected-db" 행동을 수행한다
    만일 "A" 역할이 "alternative-close" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "false-close-outcome" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 IMPOSSIBLE. false-fulfilled-closure의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "false-close-movements-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "false-close-approvals-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "false-close-outbox-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "false-close-denial-audit" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 IMPOSSIBLE. false-fulfilled-closure의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-still-active" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 IMPOSSIBLE. false-fulfilled-closure의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-contribution90" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 IMPOSSIBLE. parent-contribution의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-assessment-api-result" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 IMPOSSIBLE. parent-assessment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-assessment-db-result" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 IMPOSSIBLE. parent-assessment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "alternative-applied" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 IMPOSSIBLE. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "alternative-reason" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 IMPOSSIBLE. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "unsatisfied-assessment-preserved" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "successor-owner-one" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 IMPOSSIBLE. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "successor-owner-owner" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 IMPOSSIBLE. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "successor-owner-next-action" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 IMPOSSIBLE. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "successor-owner-next-check" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 IMPOSSIBLE. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 SUPERSEDED
    먼저 사례 파일 "verification/cases/T11/case.json"의 "parent90-superseded"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "parent" 행동을 수행한다
    만일 "A" 역할이 "child30" 행동을 수행한다
    만일 "A" 역할이 "child60" 행동을 수행한다
    만일 "A" 역할이 "receive90" 행동을 수행한다
    만일 "A" 역할이 "child30-link" 행동을 수행한다
    만일 "A" 역할이 "child30-assessment" 행동을 수행한다
    만일 "A" 역할이 "child30-close" 행동을 수행한다
    만일 "A" 역할이 "child60-link" 행동을 수행한다
    만일 "A" 역할이 "child60-assessment" 행동을 수행한다
    만일 "A" 역할이 "child60-close" 행동을 수행한다
    만일 "A" 역할이 "parent-assessment" 행동을 수행한다
    만일 "A" 역할이 "before-transfer-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-transfer-db" 행동을 수행한다
    만일 "A" 역할이 "followup" 행동을 수행한다
    만일 "A" 역할이 "deficit-transfer" 행동을 수행한다
    만일 "A" 역할이 "before-false-close-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-false-close-db" 행동을 수행한다
    만일 "A" 역할이 "false-close" 행동을 수행한다
    만일 "A" 역할이 "rejected-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "rejected-db" 행동을 수행한다
    만일 "A" 역할이 "alternative-close" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "false-close-outcome" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 SUPERSEDED. false-fulfilled-closure의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "false-close-movements-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "false-close-approvals-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "false-close-outbox-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "false-close-denial-audit" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 SUPERSEDED. false-fulfilled-closure의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-still-active" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 SUPERSEDED. false-fulfilled-closure의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-contribution90" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 SUPERSEDED. parent-contribution의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-assessment-api-result" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 SUPERSEDED. parent-assessment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-assessment-db-result" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 SUPERSEDED. parent-assessment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "alternative-applied" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 SUPERSEDED. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "alternative-reason" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 SUPERSEDED. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "unsatisfied-assessment-preserved" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "successor-owner-one" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 SUPERSEDED. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "successor-owner-owner" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 SUPERSEDED. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "successor-owner-next-action" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 SUPERSEDED. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "successor-owner-next-check" assertion으로 "자식 종료·부족10 이전 뒤에도 부모90은100 충족이 아니다 SUPERSEDED. alternative-close의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 업무 상태의 독립 판정 draft-cancel
    먼저 사례 파일 "verification/cases/T11/case.json"의 "draft-cancel"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "draft" 행동을 수행한다
    만일 "A" 역할이 "cancel" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "draft-cancel-applied" assertion으로 "업무 상태의 독립 판정 draft-cancel. cancel-draft-physical-effects의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "draft-cancelled" assertion으로 "업무 상태의 독립 판정 draft-cancel. cancel-draft-physical-effects의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "movements-no-effect" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "contributions-no-effect" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "approvals-no-effect" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "outbox-no-effect" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 업무 상태의 독립 판정 wait-timeout
    먼저 사례 파일 "verification/cases/T11/case.json"의 "wait-timeout"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "wait" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "due-wait-advance" 행동을 수행한다
    만일 "시스템" 역할이 "due-wait" 행동을 수행한다
    만일 "시스템" 역할이 "due-wait-terminal" 행동을 수행한다
    만일 "A" 역할이 "resume-without-proof" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "resume-denied" assertion으로 "업무 상태의 독립 판정 wait-timeout. timeout-generated-arrival-or-approval의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "wait-remains" assertion으로 "업무 상태의 독립 판정 wait-timeout. timeout-generated-arrival-or-approval의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-satisfied-result" assertion으로 "업무 상태의 독립 판정 wait-timeout. timeout-generated-arrival-or-approval의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "movements-no-effect" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "contributions-no-effect" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "approvals-no-effect" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 업무 상태의 독립 판정 unsupported-evaluator
    먼저 사례 파일 "verification/cases/T11/case.json"의 "unsupported-evaluator"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "draft" 행동을 수행한다
    만일 "A" 역할이 "activate" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "activate-outcome" assertion으로 "업무 상태의 독립 판정 unsupported-evaluator. unsupported-activation-effects의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "activate-movements-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "activate-contributions-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "activate-approvals-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "activate-outbox-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "activate-denial-audit" assertion으로 "업무 상태의 독립 판정 unsupported-evaluator. unsupported-activation-effects의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "unsupported-keeps-draft" assertion으로 "업무 상태의 독립 판정 unsupported-evaluator. unsupported-activation-effects의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 업무 상태의 독립 판정 pending-correction
    먼저 사례 파일 "verification/cases/T11/case.json"의 "pending-correction"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "receipt" 행동을 수행한다
    만일 "A" 역할이 "confirmed-before-correction-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "confirmed-before-correction-db" 행동을 수행한다
    만일 "시스템" 역할이 "delay-reassessment" 행동을 수행한다
    만일 "A" 역할이 "correction" 행동을 수행한다
    만일 "A" 역할이 "close" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    만일 "시스템" 역할이 "release-reassessment" 행동을 수행한다
    만일 "시스템" 역할이 "finish-reassessment-advance" 행동을 수행한다
    만일 "시스템" 역할이 "finish-reassessment" 행동을 수행한다
    만일 "시스템" 역할이 "finish-reassessment-terminal" 행동을 수행한다
    만일 "A" 역할이 "terminal-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "terminal-db" 행동을 수행한다
    그러면 "pending-close-denied" assertion으로 "업무 상태의 독립 판정 pending-correction. pending-assessment-fulfilled-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "pending-work-still-active" assertion으로 "업무 상태의 독립 판정 pending-correction. pending-assessment-fulfilled-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "pending-input-retained" assertion으로 "업무 상태의 독립 판정 pending-correction. pending-assessment-fulfilled-close의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "physical-no-pending-effect" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-post-correction-false-close" assertion으로 "업무 상태의 독립 판정 pending-correction. pending-assessment-fulfilled-close의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 업무 상태의 독립 판정 stale-revision
    먼저 사례 파일 "verification/cases/T11/case.json"의 "stale-revision"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "wait" 행동을 수행한다
    만일 "A" 역할이 "after-wait-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-wait-db" 행동을 수행한다
    만일 "A" 역할이 "stale-close" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "stale-close-outcome" assertion으로 "업무 상태의 독립 판정 stale-revision. stale-revision-effects의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "stale-close-works-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "stale-close-movements-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "stale-close-assignments-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "stale-close-outbox-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "stale-close-denial-audit" assertion으로 "업무 상태의 독립 판정 stale-revision. stale-revision-effects의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "stale-code" assertion으로 "업무 상태의 독립 판정 stale-revision. stale-revision-effects의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 업무 상태의 독립 판정 cancel-after-shipment
    먼저 사례 파일 "verification/cases/T11/case.json"의 "cancel-after-shipment"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "sale" 행동을 수행한다
    만일 "warehouse" 역할이 "reserve" 행동을 수행한다
    만일 "warehouse" 역할이 "pick" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "A" 역할이 "shipped-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "shipped-db" 행동을 수행한다
    만일 "A" 역할이 "close-with-duty" 행동을 수행한다
    만일 "A" 역할이 "transfer" 행동을 수행한다
    만일 "A" 역할이 "close" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "duty-blocks-first-close" assertion으로 "업무 상태의 독립 판정 cancel-after-shipment. cancel-preservation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cancel-applied" assertion으로 "업무 상태의 독립 판정 cancel-after-shipment. cancel-preservation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "movements-retained" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "contracts-retained" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "claims-retained" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "cancel-reason" assertion으로 "업무 상태의 독립 판정 cancel-after-shipment. cancel-preservation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "not-fulfilled" assertion으로 "업무 상태의 독립 판정 cancel-after-shipment. cancel-preservation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "residual-owner-one" assertion으로 "업무 상태의 독립 판정 cancel-after-shipment. cancel-preservation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "residual-owner-owner" assertion으로 "업무 상태의 독립 판정 cancel-after-shipment. cancel-preservation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "residual-owner-next-action" assertion으로 "업무 상태의 독립 판정 cancel-after-shipment. cancel-preservation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "residual-owner-next-check" assertion으로 "업무 상태의 독립 판정 cancel-after-shipment. cancel-preservation의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: blocking 업무 의존 순환은 두 번째 링크를 거부한다
    먼저 사례 파일 "verification/cases/T11/case.json"의 "dependency-cycle"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "link" 행동을 수행한다
    만일 "A" 역할이 "before-reverse-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-reverse-db" 행동을 수행한다
    만일 "A" 역할이 "reverse" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "reverse-outcome" assertion으로 "blocking 업무 의존 순환은 두 번째 링크를 거부한다. dependency-cycle-created의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "reverse-workLinks-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "reverse-movements-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "reverse-outbox-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "reverse-denial-audit" assertion으로 "blocking 업무 의존 순환은 두 번째 링크를 거부한다. dependency-cycle-created의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "only-forward-link" assertion으로 "blocking 업무 의존 순환은 두 번째 링크를 거부한다. dependency-cycle-created의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 공유 활동60을 부모 둘에30씩 배분하여120으로 복제하지 않는다
    먼저 사례 파일 "verification/cases/T11/case.json"의 "shared-distinct60"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "parent-one" 행동을 수행한다
    만일 "A" 역할이 "parent-two" 행동을 수행한다
    만일 "A" 역할이 "receive60" 행동을 수행한다
    만일 "A" 역할이 "parent-one-link" 행동을 수행한다
    만일 "A" 역할이 "parent-two-link" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "shared-sum60" assertion으로 "공유 활동60을 부모 둘에30씩 배분하여120으로 복제하지 않는다. shared-distinct-contribution의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "shared-scope-partition" assertion으로 "공유 활동60을 부모 둘에30씩 배분하여120으로 복제하지 않는다. shared-distinct-contribution의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 종료 뒤 새 이상은 원 업무와 판정 대신 followup에 책임을 연결한다
    먼저 사례 파일 "verification/cases/T11/case.json"의 "closed-followup"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "temperature" 행동을 수행한다
    만일 "A" 역할이 "followup" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "closed-work-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "closed-assessment-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "followup-parent-link" assertion으로 "종료 뒤 새 이상은 원 업무와 판정 대신 followup에 책임을 연결한다. followup-link의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "followup-owner-one" assertion으로 "종료 뒤 새 이상은 원 업무와 판정 대신 followup에 책임을 연결한다. followup-link의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "followup-owner-owner" assertion으로 "종료 뒤 새 이상은 원 업무와 판정 대신 followup에 책임을 연결한다. followup-link의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "followup-owner-next-action" assertion으로 "종료 뒤 새 이상은 원 업무와 판정 대신 followup에 책임을 연결한다. followup-link의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "followup-owner-next-check" assertion으로 "종료 뒤 새 이상은 원 업무와 판정 대신 followup에 책임을 연결한다. followup-link의 독립 고정 기대값을 대조한다."를 확인한다
