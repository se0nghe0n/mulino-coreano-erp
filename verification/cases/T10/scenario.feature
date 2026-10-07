# language: ko
@T10 @D10 @sit @uat
기능: 목표·의무·판정·접수 책임의 독립 계약 T10

  시나리오: 인계 PROPOSED 동안 인간 주 책임을 보존한다
    먼저 사례 파일 "verification/cases/T10/case.json"의 "handover-proposed"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "propose" 행동을 수행한다
    만일 "시스템" 역할이 "notification" 행동을 수행한다
    만일 "A" 역할이 "after-notification" 행동을 수행한다
    만일 "시스템" 역할이 "after-notification-db" 행동을 수행한다
    만일 "A" 역할이 "current-work" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "notification-keeps-A" assertion으로 "인계 PROPOSED 동안 인간 주 책임을 보존한다. notification-as-acceptance의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "proposed-keeps-A" assertion으로 "인계 PROPOSED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "인계 PROPOSED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "db-owner" assertion으로 "인계 PROPOSED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "handover-state" assertion으로 "인계 PROPOSED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "one-human-owner" assertion으로 "인계 PROPOSED 동안 인간 주 책임을 보존한다. active-human-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "separate-roles" assertion으로 "인계 PROPOSED 동안 인간 주 책임을 보존한다. active-human-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "handover-current-assignment" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-owner" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-next-action" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-next-check" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다

  시나리오: 인계 REJECTED 동안 인간 주 책임을 보존한다
    먼저 사례 파일 "verification/cases/T10/case.json"의 "handover-rejected"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "propose" 행동을 수행한다
    만일 "시스템" 역할이 "notification" 행동을 수행한다
    만일 "A" 역할이 "after-notification" 행동을 수행한다
    만일 "시스템" 역할이 "after-notification-db" 행동을 수행한다
    만일 "B" 역할이 "decision" 행동을 수행한다
    만일 "A" 역할이 "current-work" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "notification-keeps-A" assertion으로 "인계 REJECTED 동안 인간 주 책임을 보존한다. notification-as-acceptance의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "proposed-keeps-A" assertion으로 "인계 REJECTED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "인계 REJECTED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "db-owner" assertion으로 "인계 REJECTED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "handover-state" assertion으로 "인계 REJECTED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "one-human-owner" assertion으로 "인계 REJECTED 동안 인간 주 책임을 보존한다. active-human-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "separate-roles" assertion으로 "인계 REJECTED 동안 인간 주 책임을 보존한다. active-human-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "handover-current-assignment" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-owner" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-next-action" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-next-check" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다

  시나리오: 인계 EXPIRED 동안 인간 주 책임을 보존한다
    먼저 사례 파일 "verification/cases/T10/case.json"의 "handover-expired"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "propose" 행동을 수행한다
    만일 "시스템" 역할이 "notification" 행동을 수행한다
    만일 "A" 역할이 "after-notification" 행동을 수행한다
    만일 "시스템" 역할이 "after-notification-db" 행동을 수행한다
    만일 "시스템" 역할이 "advance-clock" 행동을 수행한다
    만일 "시스템" 역할이 "expire-handover-advance" 행동을 수행한다
    만일 "시스템" 역할이 "expire-handover" 행동을 수행한다
    만일 "시스템" 역할이 "expire-handover-terminal" 행동을 수행한다
    만일 "A" 역할이 "current-work" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "notification-keeps-A" assertion으로 "인계 EXPIRED 동안 인간 주 책임을 보존한다. notification-as-acceptance의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "proposed-keeps-A" assertion으로 "인계 EXPIRED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "인계 EXPIRED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "db-owner" assertion으로 "인계 EXPIRED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "handover-state" assertion으로 "인계 EXPIRED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "one-human-owner" assertion으로 "인계 EXPIRED 동안 인간 주 책임을 보존한다. active-human-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "separate-roles" assertion으로 "인계 EXPIRED 동안 인간 주 책임을 보존한다. active-human-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "handover-current-assignment" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-owner" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-next-action" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-next-check" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다

  시나리오: 인계 ACCEPTED 동안 인간 주 책임을 보존한다
    먼저 사례 파일 "verification/cases/T10/case.json"의 "handover-accepted"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "propose" 행동을 수행한다
    만일 "시스템" 역할이 "notification" 행동을 수행한다
    만일 "A" 역할이 "after-notification" 행동을 수행한다
    만일 "시스템" 역할이 "after-notification-db" 행동을 수행한다
    만일 "B" 역할이 "decision" 행동을 수행한다
    만일 "A" 역할이 "current-work" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "notification-keeps-A" assertion으로 "인계 ACCEPTED 동안 인간 주 책임을 보존한다. notification-as-acceptance의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "proposed-keeps-A" assertion으로 "인계 ACCEPTED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "인계 ACCEPTED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "db-owner" assertion으로 "인계 ACCEPTED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "handover-state" assertion으로 "인계 ACCEPTED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "one-human-owner" assertion으로 "인계 ACCEPTED 동안 인간 주 책임을 보존한다. active-human-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "separate-roles" assertion으로 "인계 ACCEPTED 동안 인간 주 책임을 보존한다. active-human-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "acceptance-owner-same-transaction" assertion으로 "인계 ACCEPTED 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "handover-current-assignment" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-owner" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-next-action" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-next-check" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다

  시나리오: 인계 ABSENT 동안 인간 주 책임을 보존한다
    먼저 사례 파일 "verification/cases/T10/case.json"의 "handover-absent"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "propose" 행동을 수행한다
    만일 "시스템" 역할이 "notification" 행동을 수행한다
    만일 "A" 역할이 "after-notification" 행동을 수행한다
    만일 "시스템" 역할이 "after-notification-db" 행동을 수행한다
    만일 "A" 역할이 "supervision" 행동을 수행한다
    만일 "A" 역할이 "current-work" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "notification-keeps-A" assertion으로 "인계 ABSENT 동안 인간 주 책임을 보존한다. notification-as-acceptance의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "proposed-keeps-A" assertion으로 "인계 ABSENT 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "인계 ABSENT 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "db-owner" assertion으로 "인계 ABSENT 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "handover-state" assertion으로 "인계 ABSENT 동안 인간 주 책임을 보존한다. owner-by-state의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "one-human-owner" assertion으로 "인계 ABSENT 동안 인간 주 책임을 보존한다. active-human-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "separate-roles" assertion으로 "인계 ABSENT 동안 인간 주 책임을 보존한다. active-human-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "supervisor-gap" assertion으로 "인계 ABSENT 동안 인간 주 책임을 보존한다. active-human-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "handover-current-assignment" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-owner" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-next-action" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다
    그러면 "handover-assignment-next-check" assertion으로 "인계 상태에 따른 잔여 의무의 유효 assignment1과 인간 담당·다음 행동·기한을 확인한다."를 확인한다

  시나리오: 의무 이전의 책임 원자성 link-failure
    먼저 사례 파일 "verification/cases/T10/case.json"의 "link-failure"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "link-fault" 행동을 수행한다
    만일 "A" 역할이 "transfer" 행동을 수행한다
    만일 "A" 역할이 "responsibility" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "transfer-rollback" assertion으로 "의무 이전의 책임 원자성 link-failure. orphan-target-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-open" assertion으로 "의무 이전의 책임 원자성 link-failure. source-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-one" assertion으로 "의무 이전의 책임 원자성 link-failure. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-owner" assertion으로 "의무 이전의 책임 원자성 link-failure. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-next-action" assertion으로 "의무 이전의 책임 원자성 link-failure. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-next-check" assertion으로 "의무 이전의 책임 원자성 link-failure. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-target-assignment" assertion으로 "의무 이전의 책임 원자성 link-failure. orphan-target-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-row-preserved" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 의무 이전의 책임 원자성 closed-target
    먼저 사례 파일 "verification/cases/T10/case.json"의 "closed-target"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "transfer" 행동을 수행한다
    만일 "A" 역할이 "responsibility" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "transfer-rollback" assertion으로 "의무 이전의 책임 원자성 closed-target. orphan-target-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-open" assertion으로 "의무 이전의 책임 원자성 closed-target. source-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-one" assertion으로 "의무 이전의 책임 원자성 closed-target. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-owner" assertion으로 "의무 이전의 책임 원자성 closed-target. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-next-action" assertion으로 "의무 이전의 책임 원자성 closed-target. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-next-check" assertion으로 "의무 이전의 책임 원자성 closed-target. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-target-assignment" assertion으로 "의무 이전의 책임 원자성 closed-target. orphan-target-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-row-preserved" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 의무 이전의 책임 원자성 missing-owner
    먼저 사례 파일 "verification/cases/T10/case.json"의 "missing-owner"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "transfer" 행동을 수행한다
    만일 "A" 역할이 "responsibility" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "transfer-rollback" assertion으로 "의무 이전의 책임 원자성 missing-owner. orphan-target-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-open" assertion으로 "의무 이전의 책임 원자성 missing-owner. source-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-one" assertion으로 "의무 이전의 책임 원자성 missing-owner. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-owner" assertion으로 "의무 이전의 책임 원자성 missing-owner. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-next-action" assertion으로 "의무 이전의 책임 원자성 missing-owner. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-next-check" assertion으로 "의무 이전의 책임 원자성 missing-owner. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-target-assignment" assertion으로 "의무 이전의 책임 원자성 missing-owner. orphan-target-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-row-preserved" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 의무 이전의 책임 원자성 missing-next-action
    먼저 사례 파일 "verification/cases/T10/case.json"의 "missing-next-action"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "transfer" 행동을 수행한다
    만일 "A" 역할이 "responsibility" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "transfer-rollback" assertion으로 "의무 이전의 책임 원자성 missing-next-action. orphan-target-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-open" assertion으로 "의무 이전의 책임 원자성 missing-next-action. source-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-one" assertion으로 "의무 이전의 책임 원자성 missing-next-action. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-owner" assertion으로 "의무 이전의 책임 원자성 missing-next-action. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-next-action" assertion으로 "의무 이전의 책임 원자성 missing-next-action. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-next-check" assertion으로 "의무 이전의 책임 원자성 missing-next-action. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-target-assignment" assertion으로 "의무 이전의 책임 원자성 missing-next-action. orphan-target-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-row-preserved" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 의무 이전의 책임 원자성 missing-next-check
    먼저 사례 파일 "verification/cases/T10/case.json"의 "missing-next-check"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "transfer" 행동을 수행한다
    만일 "A" 역할이 "responsibility" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "transfer-rollback" assertion으로 "의무 이전의 책임 원자성 missing-next-check. orphan-target-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-open" assertion으로 "의무 이전의 책임 원자성 missing-next-check. source-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-one" assertion으로 "의무 이전의 책임 원자성 missing-next-check. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-owner" assertion으로 "의무 이전의 책임 원자성 missing-next-check. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-next-action" assertion으로 "의무 이전의 책임 원자성 missing-next-check. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-responsibility-next-check" assertion으로 "의무 이전의 책임 원자성 missing-next-check. source-responsibility의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-target-assignment" assertion으로 "의무 이전의 책임 원자성 missing-next-check. orphan-target-assignment의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-row-preserved" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 의무 이전의 책임 원자성 successful-transfer
    먼저 사례 파일 "verification/cases/T10/case.json"의 "successful-transfer"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "transfer" 행동을 수행한다
    만일 "A" 역할이 "responsibility" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "transfer-applied" assertion으로 "의무 이전의 책임 원자성 successful-transfer. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-transferred" assertion으로 "의무 이전의 책임 원자성 successful-transfer. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "target-owner-one" assertion으로 "의무 이전의 책임 원자성 successful-transfer. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "target-owner-owner" assertion으로 "의무 이전의 책임 원자성 successful-transfer. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "target-owner-next-action" assertion으로 "의무 이전의 책임 원자성 successful-transfer. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "target-owner-next-check" assertion으로 "의무 이전의 책임 원자성 successful-transfer. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "root-still-open" assertion으로 "의무 이전의 책임 원자성 successful-transfer. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "target-valid-work" assertion으로 "의무 이전의 책임 원자성 successful-transfer. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "transfer-chain" assertion으로 "의무 이전의 책임 원자성 successful-transfer. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "transfer-links-one-transaction" assertion으로 "의무 이전의 책임 원자성 successful-transfer. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 의무 이전의 책임 원자성 cycle
    먼저 사례 파일 "verification/cases/T10/case.json"의 "cycle"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "transfer" 행동을 수행한다
    만일 "B" 역할이 "back-transfer" 행동을 수행한다
    만일 "A" 역할이 "responsibility" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "transfer-applied" assertion으로 "의무 이전의 책임 원자성 cycle. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "source-transferred" assertion으로 "의무 이전의 책임 원자성 cycle. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "target-owner-one" assertion으로 "의무 이전의 책임 원자성 cycle. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "target-owner-owner" assertion으로 "의무 이전의 책임 원자성 cycle. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "target-owner-next-action" assertion으로 "의무 이전의 책임 원자성 cycle. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "target-owner-next-check" assertion으로 "의무 이전의 책임 원자성 cycle. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "root-still-open" assertion으로 "의무 이전의 책임 원자성 cycle. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "target-valid-work" assertion으로 "의무 이전의 책임 원자성 cycle. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "transfer-chain" assertion으로 "의무 이전의 책임 원자성 cycle. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "transfer-links-one-transaction" assertion으로 "의무 이전의 책임 원자성 cycle. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "cycle-rejected" assertion으로 "의무 이전의 책임 원자성 cycle. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "current-root-assignment1" assertion으로 "의무 이전의 책임 원자성 cycle. transfer-guard의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False
    먼저 사례 파일 "verification/cases/T10/case.json"의 "partial-commit"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "partial-transfer" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "partial-applied" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "transferred-quantity" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. transferred-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "remaining-quantity" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. remaining-scope의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "unresolved-root-sum10" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. unresolved-root-total의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "disjoint-leaf-scopes" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "leaf-SOURCE-one" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "leaf-SOURCE-owner" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "leaf-SOURCE-next-action" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "leaf-SOURCE-next-check" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "leaf-TARGET-one" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "leaf-TARGET-owner" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "leaf-TARGET-next-action" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "leaf-TARGET-next-check" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "root-is-container" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "atomic-child-links" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "same-transaction" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 False. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 부분 이전4와 잔여6의 root10을 중복 없이 보존한다 True
    먼저 사례 파일 "verification/cases/T10/case.json"의 "partial-failure"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "partial-fault" 행동을 수행한다
    만일 "A" 역할이 "partial-transfer" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "partial-rejected" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 True. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "original-assignment-retained" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "original-root-retained" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "original-owner10-one" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 True. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "original-owner10-owner" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 True. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "original-owner10-next-action" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 True. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "original-owner10-next-check" assertion으로 "부분 이전4와 잔여6의 root10을 중복 없이 보존한다 True. current-assignments의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 긴급 재배정·해소·면제의 근거와 권한 admin-emergency
    먼저 사례 파일 "verification/cases/T10/case.json"의 "admin-emergency"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "admin" 역할이 "decision" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "admin-applied" assertion으로 "긴급 재배정·해소·면제의 근거와 권한 admin-emergency. emergency-evidence의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "new-owner-B" assertion으로 "긴급 재배정·해소·면제의 근거와 권한 admin-emergency. emergency-evidence의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "emergency-audit" assertion으로 "긴급 재배정·해소·면제의 근거와 권한 admin-emergency. emergency-evidence의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 긴급 재배정·해소·면제의 근거와 권한 ordinary-emergency
    먼저 사례 파일 "verification/cases/T10/case.json"의 "ordinary-emergency"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "decision" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "decision-outcome" assertion으로 "긴급 재배정·해소·면제의 근거와 권한 ordinary-emergency. emergency-evidence의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "decision-works-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-assignments-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-obligations-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-outbox-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-denial-audit" assertion으로 "긴급 재배정·해소·면제의 근거와 권한 ordinary-emergency. emergency-evidence의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 긴급 재배정·해소·면제의 근거와 권한 reason-missing
    먼저 사례 파일 "verification/cases/T10/case.json"의 "reason-missing"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "admin" 역할이 "decision" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "decision-outcome" assertion으로 "긴급 재배정·해소·면제의 근거와 권한 reason-missing. emergency-evidence의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "decision-works-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-assignments-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-obligations-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-outbox-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-denial-audit" assertion으로 "긴급 재배정·해소·면제의 근거와 권한 reason-missing. emergency-evidence의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 긴급 재배정·해소·면제의 근거와 권한 resolve-no-proof
    먼저 사례 파일 "verification/cases/T10/case.json"의 "resolve-no-proof"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "decision" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "decision-outcome" assertion으로 "긴급 재배정·해소·면제의 근거와 권한 resolve-no-proof. unauthorized-resolution의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "decision-works-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-assignments-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-obligations-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-outbox-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-denial-audit" assertion으로 "긴급 재배정·해소·면제의 근거와 권한 resolve-no-proof. unauthorized-resolution의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 긴급 재배정·해소·면제의 근거와 권한 waive-wrong-authority
    먼저 사례 파일 "verification/cases/T10/case.json"의 "waive-wrong-authority"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "A" 역할이 "decision" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "decision-outcome" assertion으로 "긴급 재배정·해소·면제의 근거와 권한 waive-wrong-authority. unauthorized-waiver의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "decision-works-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-assignments-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-obligations-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-outbox-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "decision-denial-audit" assertion으로 "긴급 재배정·해소·면제의 근거와 권한 waive-wrong-authority. unauthorized-waiver의 독립 고정 기대값을 대조한다."를 확인한다
