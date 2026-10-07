# language: ko
@C5 @D10 @D14 @D26 @sit @uat
기능: 목표·의무·판정·접수 책임의 독립 계약 C5

  시나리오: 종료된 부모 뒤 이상 접수의 책임 failed-link
    먼저 사례 파일 "verification/cases/C5/case.json"의 "failed-link"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "intake" 역할이 "anomaly" 행동을 수행한다
    만일 "시스템" 역할이 "link-fault" 행동을 수행한다
    만일 "intake" 역할이 "followup" 행동을 수행한다
    만일 "시스템" 역할이 "alert" 행동을 수행한다
    만일 "A" 역할이 "intake-query" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "failed-followup" assertion으로 "종료된 부모 뒤 이상 접수의 책임 failed-link. intake-tracking의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "intake-open" assertion으로 "종료된 부모 뒤 이상 접수의 책임 failed-link. intake-tracking의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "intake-owner-human" assertion으로 "종료된 부모 뒤 이상 접수의 책임 failed-link. intake-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "intake-one" assertion으로 "종료된 부모 뒤 이상 접수의 책임 failed-link. intake-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "intake-current-assignment1" assertion으로 "종료된 부모 뒤 이상 접수의 책임 failed-link. intake-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "alert-not-closure" assertion으로 "종료된 부모 뒤 이상 접수의 책임 failed-link. alert-delivery-closes-intake의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-not-rewritten" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "parent-assessment-not-rewritten" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 종료된 부모 뒤 이상 접수의 책임 missing-intake-owner
    먼저 사례 파일 "verification/cases/C5/case.json"의 "missing-intake-owner"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "intake" 역할이 "anomaly" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "anomaly-outcome" assertion으로 "종료된 부모 뒤 이상 접수의 책임 missing-intake-owner. intake-activation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "anomaly-works-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "anomaly-obligations-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "anomaly-assignments-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "anomaly-outbox-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "anomaly-denial-audit" assertion으로 "종료된 부모 뒤 이상 접수의 책임 missing-intake-owner. intake-activation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "intake-not-activated" assertion으로 "종료된 부모 뒤 이상 접수의 책임 missing-intake-owner. intake-activation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-not-rewritten" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "parent-assessment-not-rewritten" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 종료된 부모 뒤 이상 접수의 책임 missing-supervisor
    먼저 사례 파일 "verification/cases/C5/case.json"의 "missing-supervisor"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "intake" 역할이 "anomaly" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "anomaly-outcome" assertion으로 "종료된 부모 뒤 이상 접수의 책임 missing-supervisor. intake-activation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "anomaly-works-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "anomaly-obligations-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "anomaly-assignments-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "anomaly-outbox-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "anomaly-denial-audit" assertion으로 "종료된 부모 뒤 이상 접수의 책임 missing-supervisor. intake-activation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "intake-not-activated" assertion으로 "종료된 부모 뒤 이상 접수의 책임 missing-supervisor. intake-activation의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "parent-not-rewritten" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "parent-assessment-not-rewritten" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다

  시나리오: 같은 이상 재처리3회는 후속 업무와 의무 각각1개로 연결한다
    먼저 사례 파일 "verification/cases/C5/case.json"의 "same-anomaly-three-retries"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "intake" 역할이 "anomaly" 행동을 수행한다
    만일 "시스템" 역할이 "link-fault" 행동을 수행한다
    만일 "intake" 역할이 "failed-followup" 행동을 수행한다
    만일 "시스템" 역할이 "release-link-fault" 행동을 수행한다
    만일 "시스템" 역할이 "retry-1-advance" 행동을 수행한다
    만일 "시스템" 역할이 "retry-1" 행동을 수행한다
    만일 "시스템" 역할이 "retry-1-terminal" 행동을 수행한다
    만일 "시스템" 역할이 "retry-2-advance" 행동을 수행한다
    만일 "시스템" 역할이 "retry-2" 행동을 수행한다
    만일 "시스템" 역할이 "retry-2-terminal" 행동을 수행한다
    만일 "시스템" 역할이 "retry-3-advance" 행동을 수행한다
    만일 "시스템" 역할이 "retry-3" 행동을 수행한다
    만일 "시스템" 역할이 "retry-3-terminal" 행동을 수행한다
    만일 "A" 역할이 "connected" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "one-anomaly-work" assertion으로 "같은 이상 재처리3회는 후속 업무와 의무 각각1개로 연결한다. anomaly-work-count의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "one-anomaly-obligation" assertion으로 "같은 이상 재처리3회는 후속 업무와 의무 각각1개로 연결한다. anomaly-obligation-count의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "connected-owner" assertion으로 "같은 이상 재처리3회는 후속 업무와 의무 각각1개로 연결한다. connected-work-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "one-current-assignment" assertion으로 "같은 이상 재처리3회는 후속 업무와 의무 각각1개로 연결한다. connected-work-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "assignment-owner" assertion으로 "같은 이상 재처리3회는 후속 업무와 의무 각각1개로 연결한다. connected-work-owner의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "intake-closed-after-link" assertion으로 "같은 이상 재처리3회는 후속 업무와 의무 각각1개로 연결한다. intake-close-after-link의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "work-link-identity" assertion으로 "같은 이상 재처리3회는 후속 업무와 의무 각각1개로 연결한다. intake-close-after-link의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "duty-work-link" assertion으로 "같은 이상 재처리3회는 후속 업무와 의무 각각1개로 연결한다. intake-close-after-link의 독립 고정 기대값을 대조한다."를 확인한다

  시나리오: 정상 관측은 근거를 보존하고 새 업무와 실행 시도를 만들지 않는다
    먼저 사례 파일 "verification/cases/C5/case.json"의 "normal-observation-no-work"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "A" 역할이 "before-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "intake" 역할이 "normal" 행동을 수행한다
    만일 "시스템" 역할이 "normal-tick-advance" 행동을 수행한다
    만일 "시스템" 역할이 "normal-tick" 행동을 수행한다
    만일 "시스템" 역할이 "normal-tick-terminal" 행동을 수행한다
    만일 "A" 역할이 "after-db-snapshot" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "no-normal-work" assertion으로 "정상 관측은 근거를 보존하고 새 업무와 실행 시도를 만들지 않는다. normal-observation-work의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "work-set-unchanged" assertion으로 "같은 대상의 독립 전후 원행을 비교한다. 조회/거부 감사의 추가는 별도 허용한다."를 확인한다
    그러면 "no-normal-execution" assertion으로 "정상 관측은 근거를 보존하고 새 업무와 실행 시도를 만들지 않는다. normal-observation-run의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "no-normal-obligation" assertion으로 "정상 관측은 근거를 보존하고 새 업무와 실행 시도를 만들지 않는다. intake-close-after-link의 독립 고정 기대값을 대조한다."를 확인한다
    그러면 "normal-intake-reason" assertion으로 "정상 관측은 근거를 보존하고 새 업무와 실행 시도를 만들지 않는다. intake-close-after-link의 독립 고정 기대값을 대조한다."를 확인한다
