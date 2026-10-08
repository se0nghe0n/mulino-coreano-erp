# language: ko
@T26 @D26 @sit @uat @recovery
기능: 영속 업무의 장애 복구와 artifact 복원

  시나리오: 큐 메시지 없이 DB의 due 의무와 대기를 재발견한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "due-wait-db-rediscovery"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "owner" 역할이 "wait" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "stop-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "stop-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "stop-app-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "start-again-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-again-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-again-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-again-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "tick" 행동을 수행한다
    만일 "시스템" 역할이 "terminal" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "terminal-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "durable-wait-before" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "empty-message-queue" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "discovery-source-db" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "attempt-cause-due" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "due-obligation-discovered" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "no-fake-receipt" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "timeout-never-satisfied" assertion으로 "시간 경과·queue 성공이 증거 없는 목표를 충족시키지 않는다."를 확인한다
    그러면 "queue-empty-after-restart" assertion으로 "재시작 뒤 terminal 관찰 시점에도 queue message0이다. 재발견은 DB due index에서만 온다."를 확인한다

  시나리오: 종료된 부모 뒤 이상 접수의 연결 장애를 DB에서 복구한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "orphan-intake-recovered"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "link-fault" 행동을 수행한다
    만일 "receiver" 역할이 "anomaly" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "link-fault-clear" 행동을 수행한다
    만일 "시스템" 역할이 "restart-api" 행동을 수행한다
    만일 "시스템" 역할이 "restart-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "restart-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "restart-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "tick" 행동을 수행한다
    만일 "시스템" 역할이 "terminal" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "terminal-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "before-new-work-zero" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "before-link-pending" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "before-intake-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "before-intake-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "before-intake-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "after-work-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "after-duty-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "intake-link-confirmed" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "canonical-intake-linked-duty" assertion으로 "재시도에서 같은 접수와 단 하나 의무를 연결한다."를 확인한다

  시나리오: 외부 전달의 실제 실패와 retry 소진 뒤 책임·알림 dedupe를 보존한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "outbox-exhaustion-alert-dedupe"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "buyer" 역할이 "po-propose" 행동을 수행한다
    만일 "manager" 역할이 "po-approve" 행동을 수행한다
    만일 "시스템" 역할이 "po-responder" 행동을 수행한다
    만일 "buyer" 역할이 "po-dispatch" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "retry-clock-0" 행동을 수행한다
    만일 "시스템" 역할이 "tick-0" 행동을 수행한다
    만일 "시스템" 역할이 "terminal-0" 행동을 수행한다
    만일 "시스템" 역할이 "retry-clock-1" 행동을 수행한다
    만일 "시스템" 역할이 "tick-1" 행동을 수행한다
    만일 "시스템" 역할이 "terminal-1" 행동을 수행한다
    만일 "시스템" 역할이 "retry-clock-2" 행동을 수행한다
    만일 "시스템" 역할이 "tick-2" 행동을 수행한다
    만일 "시스템" 역할이 "terminal-2" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    만일 "시스템" 역할이 "repeat-tick" 행동을 수행한다
    만일 "시스템" 역할이 "repeat-terminal" 행동을 수행한다
    만일 "operations" 역할이 "after-api" 행동을 수행한다
    만일 "시스템" 역할이 "after-db" 행동을 수행한다
    그러면 "terminal-0-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-0-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "terminal-1-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-1-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "terminal-2-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-2-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "repeat-terminal-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "repeat-terminal-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "outbox-exhausted" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "outbox-attempts-three" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "no-delivered-document" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "one-alert-before" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "one-alert-after" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "alert-key" assertion으로 "문제 ID와 stateVersion의 같은 alert key는 반복 전달에도 하나다."를 확인한다
    그러면 "notification-duty-still-open" assertion으로 "알림 성공은 의무 완료 효과0이다."를 확인한다
    그러면 "notification-assignment-unchanged" assertion으로 "알림 ACK 전후 현재 인간 책임과 다음 행동은 유지된다."를 확인한다
    그러면 "no-alert-resolution" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다

  시나리오: 실제 이상 상태 아홉 종류를 운영 조회와 원 행으로 노출한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "operational-nine-categories"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "owner" 역할이 "wait" 행동을 수행한다
    만일 "warehouse" 역할이 "reserve" 행동을 수행한다
    만일 "qc" 역할이 "hold" 행동을 수행한다
    만일 "receiver" 역할이 "source-first" 행동을 수행한다
    만일 "receiver" 역할이 "source-conflict" 행동을 수행한다
    만일 "시스템" 역할이 "restore-damage" 행동을 수행한다
    만일 "buyer" 역할이 "exhausted-propose" 행동을 수행한다
    만일 "manager" 역할이 "exhausted-approve" 행동을 수행한다
    만일 "시스템" 역할이 "exhausted-responder" 행동을 수행한다
    만일 "buyer" 역할이 "exhausted-dispatch" 행동을 수행한다
    만일 "buyer" 역할이 "unknown-propose" 행동을 수행한다
    만일 "manager" 역할이 "unknown-approve" 행동을 수행한다
    만일 "시스템" 역할이 "unknown-responder" 행동을 수행한다
    만일 "buyer" 역할이 "unknown-dispatch" 행동을 수행한다
    만일 "시스템" 역할이 "pause-claim" 행동을 수행한다
    만일 "시스템" 역할이 "claim-tick" 행동을 수행한다
    만일 "시스템" 역할이 "claim-observed" 행동을 수행한다
    만일 "시스템" 역할이 "stop-worker" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "claim-terminal" 행동을 수행한다
    만일 "시스템" 역할이 "operational-retry-clock-0" 행동을 수행한다
    만일 "시스템" 역할이 "failure-tick-0" 행동을 수행한다
    만일 "시스템" 역할이 "failure-terminal-0" 행동을 수행한다
    만일 "시스템" 역할이 "operational-retry-clock-1" 행동을 수행한다
    만일 "시스템" 역할이 "failure-tick-1" 행동을 수행한다
    만일 "시스템" 역할이 "failure-terminal-1" 행동을 수행한다
    만일 "시스템" 역할이 "operational-retry-clock-2" 행동을 수행한다
    만일 "시스템" 역할이 "failure-tick-2" 행동을 수행한다
    만일 "시스템" 역할이 "failure-terminal-2" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "failure-terminal-0-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "failure-terminal-0-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "failure-terminal-1-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "failure-terminal-1-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "failure-terminal-2-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "failure-terminal-2-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "api-all-nine" assertion으로 "아홉 운영 문제 유형을 scope에서 빠짐없이 관찰한다."를 확인한다
    그러면 "db-all-nine" assertion으로 "API의 Boolean 대신 독립 DB issue 원 행을 대조한다."를 확인한다
    그러면 "issue-owner-supervisor" assertion으로 "책임 공백 문제도 지정 감독자에게 owner·다음 행동·기한을 반환한다."를 확인한다
    그러면 "ownerless-issue-supervisor" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "issue-id-state-relations" assertion으로 "동일 snapshot API와 independent DB는 ID·state·owner·다음행동이 같다."를 확인한다
    그러면 "ownerless-active-raw" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "allocation-suspended" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "raw-intake-unmatched" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "raw-evidence-conflict" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "raw-stale-claim" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "raw-exhausted" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "raw-external-unknown" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "projection-stale-revision" assertion으로 "원장 revision과 실제 projection revision 차이를 관찰한다."를 확인한다
    그러면 "claim-terminal-origin" assertion으로 "lease 관찰 뒤 worker를 중지한 같은 자율 task의 실제 종료를 확인한다. ACK나 미완료 handle로 recovery 증거를 대신하지 않는다."를 확인한다
    그러면 "claim-terminal-terminalStatus" assertion으로 "lease 관찰 뒤 worker를 중지한 같은 자율 task의 실제 종료를 확인한다. ACK나 미완료 handle로 recovery 증거를 대신하지 않는다."를 확인한다

  시나리오: OPERATIONS가 원 canonical key와 현재 위임으로만 안전 재시도한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "safe-retry-canonical-current-grant"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "commit-fault" 행동을 수행한다
    만일 "warehouse" 역할이 "original" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "clear-commit-fault" 행동을 수행한다
    만일 "operations" 역할이 "retry" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "before-movement-zero" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "movement-once" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "canonical-key-kept" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "current-grant-rechecked" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "safe-retry-fence-used" assertion으로 "운영 retry의 확정 명령이 현재 claim fence token을 사용한다."를 확인한다
    그러면 "safe-retry-original-actor" assertion으로 "저장된 원 command의 actor는 원 요청을 인증한 warehouse다. retry payload는 actor를 전달하지 않으므로 서버가 저장 원행에서 다시 읽은 값이다."를 확인한다
    그러면 "retry-canonical-hash" assertion으로 "저장 command의 canonical hash는 원 요청 응답의 hash와 같다. retry payload는 hash를 전달하지 않는다."를 확인한다
    그러면 "moved-20" assertion으로 "rollback0 + 안전 retry20 = 실제 이동20 한 번이다."를 확인한다

  시나리오: 원 grant 철회 뒤 OPERATIONS retry도 새 효과를 만들지 못한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "safe-retry-revoked-blocked"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "commit-fault" 행동을 수행한다
    만일 "warehouse" 역할이 "original" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "clear-commit-fault" 행동을 수행한다
    만일 "supervisor" 역할이 "revoke" 행동을 수행한다
    만일 "시스템" 역할이 "restart-api" 행동을 수행한다
    만일 "시스템" 역할이 "restart-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "restart-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "restart-worker-b" 행동을 수행한다
    만일 "operations" 역할이 "retry" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "db-segments-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "db-genealogy-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "db-movements-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "db-allocations-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "db-quantity-delta-zero" assertion으로 "전후 보유량20−20=0BOX다. 현재와 baseline 단위를 모두 검사한다."를 확인한다
    그러면 "retry-denial-audit" assertion으로 "철회된 grant로 재시도한 retrySafeCommand는 명령 감사에 outcome=REJECTED, errorCode=FORBIDDEN 한 행만 남긴다(contracts/audit-observation-fields.json)."를 확인한다
    그러면 "retry-forbidden" assertion으로 "운영 scope는 원 actor의 현재 grant를 대신하지 않는다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다

  시나리오: 철회된 원 actor 대신 payload가 지목한 actor로 재시도할 수 없다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "safe-retry-forged-original-actor"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "commit-fault" 행동을 수행한다
    만일 "warehouse" 역할이 "original" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "clear-commit-fault" 행동을 수행한다
    만일 "supervisor" 역할이 "revoke" 행동을 수행한다
    만일 "시스템" 역할이 "restart-api" 행동을 수행한다
    만일 "시스템" 역할이 "restart-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "restart-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "restart-worker-b" 행동을 수행한다
    만일 "operations" 역할이 "retry" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "forged-actor-rejected" assertion으로 "payload의 originalActorId는 권한이 아니다. 저장된 원 actor의 철회된 grant 때문에 또는 허용되지 않은 필드 때문에 거부되며 어느 경우도 효과를 만들지 않는다."를 확인한다
    그러면 "db-segments-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "db-genealogy-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "db-movements-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "db-allocations-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "db-quantity-delta-zero" assertion으로 "전후 보유량20−20=0BOX다. 현재와 baseline 단위를 모두 검사한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "forged-actor-no-committed-retry" assertion으로 "위조된 actor로 시도한 retry는 COMMITTED command를 남기지 않는다."를 확인한다

  시나리오: 호출자가 제시한 다른 canonical hash로 안전 재시도할 수 없다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "safe-retry-forged-request-hash"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "commit-fault" 행동을 수행한다
    만일 "warehouse" 역할이 "original" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "clear-commit-fault" 행동을 수행한다
    만일 "operations" 역할이 "retry" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "before-movement-zero" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "forged-hash-rejected" assertion으로 "저장 hash와 다른 호출자 hash로는 재시도하지 않는다."를 확인한다
    그러면 "forged-hash-no-movement" assertion으로 "거부된 위조 hash 재시도는 실제 이동을 만들지 않는다."를 확인한다
    그러면 "forged-hash-no-committed-retry" assertion으로 "위조 hash retry는 COMMITTED command를 남기지 않는다."를 확인한다
    그러면 "stored-hash-unchanged" assertion으로 "원 canonical key의 저장 command hash는 원 응답의 hash 그대로이며 호출자 hash로 바뀌지 않는다. 거부된 retry record는 별도 허용 기록이다."를 확인한다

  시나리오: UNKNOWN_EXTERNAL은 대조 전에 재발행하지 않고 확인 성공을 연결한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "unknown-external-reconcile-before-retry"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "buyer" 역할이 "po-propose" 행동을 수행한다
    만일 "manager" 역할이 "po-approve" 행동을 수행한다
    만일 "시스템" 역할이 "po-responder" 행동을 수행한다
    만일 "buyer" 역할이 "po-dispatch" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "tick" 행동을 수행한다
    만일 "시스템" 역할이 "terminal" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "operations" 역할이 "unsafe-retry" 행동을 수행한다
    만일 "시스템" 역할이 "manual-status-query" 행동을 수행한다
    만일 "receiver" 역할이 "attach-reconciliation-evidence" 행동을 수행한다
    만일 "receiver" 역할이 "reconcile" 행동을 수행한다
    만일 "operations" 역할이 "replay" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "terminal-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "before-external-unknown" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "unsafe-retry-rejected" assertion으로 "외부 성공 여부 미확인을 재전송으로 해결하지 않는다."를 확인한다
    그러면 "external-once-before" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "external-once-after" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "same-external-id" assertion으로 "응답 유실·대조 후에도 실제 externalOperationId는 안정적이다."를 확인한다
    그러면 "confirmed-result" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "actual-external-status" assertion으로 "대조 근거는 fixture의 미래 성공 문서가 아니라 실제 상대 operation의 read-only 조회다."를 확인한다
    그러면 "external-reconcile-evidence-hash" assertion으로 "현재 조회한 원문 hash를 도메인 대조 기록과 연결한다."를 확인한다
    그러면 "local-outbox-linked" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다

  시나리오: 새 사건 없이 lot 만료20을 정지하고 sweeper 지연에도 출고를 막는다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "lot-expiry-no-event"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "warehouse" 역할이 "reserve" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-sweeper" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "start-sweeper" 행동을 수행한다
    만일 "시스템" 역할이 "sweep" 행동을 수행한다
    만일 "시스템" 역할이 "sweep-terminal" 행동을 수행한다
    만일 "시스템" 역할이 "sweep-db" 행동을 수행한다
    만일 "시스템" 역할이 "repeat-sweep" 행동을 수행한다
    만일 "operations" 역할이 "repeat-api" 행동을 수행한다
    만일 "시스템" 역할이 "repeat-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-again-sweeper" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "operations" 역할이 "guard-api" 행동을 수행한다
    만일 "시스템" 역할이 "guard-db" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "sweep-terminal-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "sweep-terminal-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "boundary-index-deadline" assertion으로 "별도 이벤트 전에 scope의 다음 실제 유효 경계를 등록한다."를 확인한다
    그러면 "boundary-cause" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "guard-db-segments-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-genealogy-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-movements-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-quantity-delta-zero" assertion으로 "전후 보유량20−20=0BOX다. 현재와 baseline 단위를 모두 검사한다."를 확인한다
    그러면 "guard-no-consumed-allocation" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-original-allocation-kept" assertion으로 "실행 거부가 기존 예약을 삭제·대체하지 않는다. SUSPENDED 대조 전이는 허용한다."를 확인한다
    그러면 "guard-allocation-quantity-kept" assertion으로 "예약20의 미해결 책임 수량을 보존한다."를 확인한다
    그러면 "no-new-event" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "dispatch-outbox-zero" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-outcome" assertion으로 "자동 정지 후에도 실제 commit guard가 만료 이후 출고를 거부한다. 중지 지연의 독립 인수는 delayed-guard 사례에 있다."를 확인한다
    그러면 "allocation-suspended" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "expiry-duty-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "held-20" assertion으로 "유효성 만료는 실제 물량 감소가 아니다.20BOX가 남는다."를 확인한다
    그러면 "api-executable-zero" assertion으로 "예약20은 책임으로 보존하지만 신규 실행 가능 배분0BOX다."를 확인한다
    그러면 "sweep-db-expiry-event-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-refresh-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-still-unverified" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-refresh-time" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-followup-open-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-allocation-suspended" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-no-user-business-event" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-expiry-event-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-refresh-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-still-unverified" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-refresh-time" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-followup-open-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-allocation-suspended" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-no-user-business-event" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-no-due-task" assertion으로 "같은 만료 경계는 이미 처리됐다. 반복 sweep에서 새 자율 task를 만들지 않는다."를 확인한다
    그러면 "repeat-events-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "repeat-assessments-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "repeat-obligations-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "before-no-expiry-events" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "before-no-expiry-assessments" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "before-no-expiry-obligations" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "sweep-db-followup-responsibility-present" assertion으로 "사용자 요청 전에 자동 만료 의무의 실제 ID·root·책임 업무·인간 owner·supervisor·다음 행동·확인 시점이 모두 존재해야 한다. 빈 원 행이나 owner 없는 OPEN은 통과하지 못한다."를 확인한다
    그러면 "sweep-db-followup-responsibility-values" assertion으로 "자동 만료 의무는 fixture의 인간 책임자·감독자·다음 행동·확인 시점을 갖는다. 뒤 출고의 책임 복구로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-current-expiry-assignment-one" assertion으로 "자동 생성된 만료 후속 의무의 현재 OPEN assignment는 하나다. 초기 활성화 의무와 구별하고 빈 행·중복 assignment를 거부한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-responsibility-present" assertion으로 "자동 sweep의 같은 snapshot에 실제 assignment ID·의무·root·업무와 인간 책임 필드가 모두 있어야 한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-responsibility-values" assertion으로 "자동 sweep가 current OPEN assignment의 owner·supervisor·다음 행동·확인 시점을 함께 upsert한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-obligationId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-expiry-assignment-rootId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-expiry-assignment-workId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-followup-responsibility-present" assertion으로 "사용자 요청 전에 자동 만료 의무의 실제 ID·root·책임 업무·인간 owner·supervisor·다음 행동·확인 시점이 모두 존재해야 한다. 빈 원 행이나 owner 없는 OPEN은 통과하지 못한다."를 확인한다
    그러면 "repeat-db-followup-responsibility-values" assertion으로 "자동 만료 의무는 fixture의 인간 책임자·감독자·다음 행동·확인 시점을 갖는다. 뒤 출고의 책임 복구로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-current-expiry-assignment-one" assertion으로 "자동 생성된 만료 후속 의무의 현재 OPEN assignment는 하나다. 초기 활성화 의무와 구별하고 빈 행·중복 assignment를 거부한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-responsibility-present" assertion으로 "자동 sweep의 같은 snapshot에 실제 assignment ID·의무·root·업무와 인간 책임 필드가 모두 있어야 한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-responsibility-values" assertion으로 "자동 sweep가 current OPEN assignment의 owner·supervisor·다음 행동·확인 시점을 함께 upsert한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-obligationId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-expiry-assignment-rootId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-expiry-assignment-workId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-expiry-assignment-identity-kept" assertion으로 "반복 sweep는 동일한 현재 OPEN assignment 원 행·ID·owner·supervisor·다음 행동·확인 시점을 유지한다."를 확인한다

  시나리오: sweeper 중지 중 lot 만료 뒤 출고를 거부한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "lot-expiry-delayed-guard"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "warehouse" 역할이 "reserve" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-sweeper" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "operations" 역할이 "guard-api" 행동을 수행한다
    만일 "시스템" 역할이 "guard-db" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "boundary-index-deadline" assertion으로 "별도 이벤트 전에 scope의 다음 실제 유효 경계를 등록한다."를 확인한다
    그러면 "boundary-cause" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "guard-db-segments-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-genealogy-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-movements-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-quantity-delta-zero" assertion으로 "전후 보유량20−20=0BOX다. 현재와 baseline 단위를 모두 검사한다."를 확인한다
    그러면 "guard-no-consumed-allocation" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-original-allocation-kept" assertion으로 "실행 거부가 기존 예약을 삭제·대체하지 않는다. SUSPENDED 대조 전이는 허용한다."를 확인한다
    그러면 "guard-allocation-quantity-kept" assertion으로 "예약20의 미해결 책임 수량을 보존한다."를 확인한다
    그러면 "no-new-event" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "dispatch-outbox-zero" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-outcome" assertion으로 "sweeper가 중지돼도 실제 commit guard가 만료 이후 출고를 거부한다."를 확인한다
    그러면 "allocation-suspended" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "expiry-duty-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "held-20" assertion으로 "유효성 만료는 실제 물량 감소가 아니다.20BOX가 남는다."를 확인한다
    그러면 "api-executable-zero" assertion으로 "예약20은 책임으로 보존하지만 신규 실행 가능 배분0BOX다."를 확인한다

  시나리오: 새 사건 없이 disposition 만료20을 정지하고 sweeper 지연에도 출고를 막는다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "disposition-expiry-no-event"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "warehouse" 역할이 "reserve" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-sweeper" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "start-sweeper" 행동을 수행한다
    만일 "시스템" 역할이 "sweep" 행동을 수행한다
    만일 "시스템" 역할이 "sweep-terminal" 행동을 수행한다
    만일 "시스템" 역할이 "sweep-db" 행동을 수행한다
    만일 "시스템" 역할이 "repeat-sweep" 행동을 수행한다
    만일 "operations" 역할이 "repeat-api" 행동을 수행한다
    만일 "시스템" 역할이 "repeat-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-again-sweeper" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "operations" 역할이 "guard-api" 행동을 수행한다
    만일 "시스템" 역할이 "guard-db" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "sweep-terminal-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "sweep-terminal-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "boundary-index-deadline" assertion으로 "별도 이벤트 전에 scope의 다음 실제 유효 경계를 등록한다."를 확인한다
    그러면 "boundary-cause" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "guard-db-segments-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-genealogy-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-movements-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-quantity-delta-zero" assertion으로 "전후 보유량20−20=0BOX다. 현재와 baseline 단위를 모두 검사한다."를 확인한다
    그러면 "guard-no-consumed-allocation" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-original-allocation-kept" assertion으로 "실행 거부가 기존 예약을 삭제·대체하지 않는다. SUSPENDED 대조 전이는 허용한다."를 확인한다
    그러면 "guard-allocation-quantity-kept" assertion으로 "예약20의 미해결 책임 수량을 보존한다."를 확인한다
    그러면 "no-new-event" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "dispatch-outbox-zero" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-outcome" assertion으로 "자동 정지 후에도 실제 commit guard가 만료 이후 출고를 거부한다. 중지 지연의 독립 인수는 delayed-guard 사례에 있다."를 확인한다
    그러면 "allocation-suspended" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "expiry-duty-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "held-20" assertion으로 "유효성 만료는 실제 물량 감소가 아니다.20BOX가 남는다."를 확인한다
    그러면 "api-executable-zero" assertion으로 "예약20은 책임으로 보존하지만 신규 실행 가능 배분0BOX다."를 확인한다
    그러면 "sweep-db-expiry-event-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-refresh-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-still-unverified" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-refresh-time" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-followup-open-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-allocation-suspended" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-no-user-business-event" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-expiry-event-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-refresh-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-still-unverified" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-refresh-time" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-followup-open-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-allocation-suspended" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-no-user-business-event" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-no-due-task" assertion으로 "같은 만료 경계는 이미 처리됐다. 반복 sweep에서 새 자율 task를 만들지 않는다."를 확인한다
    그러면 "repeat-events-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "repeat-assessments-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "repeat-obligations-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "before-no-expiry-events" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "before-no-expiry-assessments" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "before-no-expiry-obligations" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "sweep-db-followup-responsibility-present" assertion으로 "사용자 요청 전에 자동 만료 의무의 실제 ID·root·책임 업무·인간 owner·supervisor·다음 행동·확인 시점이 모두 존재해야 한다. 빈 원 행이나 owner 없는 OPEN은 통과하지 못한다."를 확인한다
    그러면 "sweep-db-followup-responsibility-values" assertion으로 "자동 만료 의무는 fixture의 인간 책임자·감독자·다음 행동·확인 시점을 갖는다. 뒤 출고의 책임 복구로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-current-expiry-assignment-one" assertion으로 "자동 생성된 만료 후속 의무의 현재 OPEN assignment는 하나다. 초기 활성화 의무와 구별하고 빈 행·중복 assignment를 거부한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-responsibility-present" assertion으로 "자동 sweep의 같은 snapshot에 실제 assignment ID·의무·root·업무와 인간 책임 필드가 모두 있어야 한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-responsibility-values" assertion으로 "자동 sweep가 current OPEN assignment의 owner·supervisor·다음 행동·확인 시점을 함께 upsert한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-obligationId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-expiry-assignment-rootId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-expiry-assignment-workId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-followup-responsibility-present" assertion으로 "사용자 요청 전에 자동 만료 의무의 실제 ID·root·책임 업무·인간 owner·supervisor·다음 행동·확인 시점이 모두 존재해야 한다. 빈 원 행이나 owner 없는 OPEN은 통과하지 못한다."를 확인한다
    그러면 "repeat-db-followup-responsibility-values" assertion으로 "자동 만료 의무는 fixture의 인간 책임자·감독자·다음 행동·확인 시점을 갖는다. 뒤 출고의 책임 복구로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-current-expiry-assignment-one" assertion으로 "자동 생성된 만료 후속 의무의 현재 OPEN assignment는 하나다. 초기 활성화 의무와 구별하고 빈 행·중복 assignment를 거부한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-responsibility-present" assertion으로 "자동 sweep의 같은 snapshot에 실제 assignment ID·의무·root·업무와 인간 책임 필드가 모두 있어야 한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-responsibility-values" assertion으로 "자동 sweep가 current OPEN assignment의 owner·supervisor·다음 행동·확인 시점을 함께 upsert한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-obligationId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-expiry-assignment-rootId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-expiry-assignment-workId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-expiry-assignment-identity-kept" assertion으로 "반복 sweep는 동일한 현재 OPEN assignment 원 행·ID·owner·supervisor·다음 행동·확인 시점을 유지한다."를 확인한다

  시나리오: sweeper 중지 중 disposition 만료 뒤 출고를 거부한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "disposition-expiry-delayed-guard"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "warehouse" 역할이 "reserve" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-sweeper" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "operations" 역할이 "guard-api" 행동을 수행한다
    만일 "시스템" 역할이 "guard-db" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "boundary-index-deadline" assertion으로 "별도 이벤트 전에 scope의 다음 실제 유효 경계를 등록한다."를 확인한다
    그러면 "boundary-cause" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "guard-db-segments-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-genealogy-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-movements-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-quantity-delta-zero" assertion으로 "전후 보유량20−20=0BOX다. 현재와 baseline 단위를 모두 검사한다."를 확인한다
    그러면 "guard-no-consumed-allocation" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-original-allocation-kept" assertion으로 "실행 거부가 기존 예약을 삭제·대체하지 않는다. SUSPENDED 대조 전이는 허용한다."를 확인한다
    그러면 "guard-allocation-quantity-kept" assertion으로 "예약20의 미해결 책임 수량을 보존한다."를 확인한다
    그러면 "no-new-event" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "dispatch-outbox-zero" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-outcome" assertion으로 "sweeper가 중지돼도 실제 commit guard가 만료 이후 출고를 거부한다."를 확인한다
    그러면 "allocation-suspended" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "expiry-duty-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "held-20" assertion으로 "유효성 만료는 실제 물량 감소가 아니다.20BOX가 남는다."를 확인한다
    그러면 "api-executable-zero" assertion으로 "예약20은 책임으로 보존하지만 신규 실행 가능 배분0BOX다."를 확인한다

  시나리오: 새 사건 없이 grant 만료20을 정지하고 sweeper 지연에도 출고를 막는다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "grant-expiry-no-event"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "warehouse" 역할이 "reserve" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-sweeper" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "start-sweeper" 행동을 수행한다
    만일 "시스템" 역할이 "sweep" 행동을 수행한다
    만일 "시스템" 역할이 "sweep-terminal" 행동을 수행한다
    만일 "시스템" 역할이 "sweep-db" 행동을 수행한다
    만일 "시스템" 역할이 "repeat-sweep" 행동을 수행한다
    만일 "operations" 역할이 "repeat-api" 행동을 수행한다
    만일 "시스템" 역할이 "repeat-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-again-sweeper" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "operations" 역할이 "guard-api" 행동을 수행한다
    만일 "시스템" 역할이 "guard-db" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "sweep-terminal-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "sweep-terminal-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "boundary-index-deadline" assertion으로 "별도 이벤트 전에 scope의 다음 실제 유효 경계를 등록한다."를 확인한다
    그러면 "boundary-cause" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "guard-db-segments-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-genealogy-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-movements-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-quantity-delta-zero" assertion으로 "전후 보유량20−20=0BOX다. 현재와 baseline 단위를 모두 검사한다."를 확인한다
    그러면 "guard-no-consumed-allocation" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-original-allocation-kept" assertion으로 "실행 거부가 기존 예약을 삭제·대체하지 않는다. SUSPENDED 대조 전이는 허용한다."를 확인한다
    그러면 "guard-allocation-quantity-kept" assertion으로 "예약20의 미해결 책임 수량을 보존한다."를 확인한다
    그러면 "no-new-event" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "dispatch-outbox-zero" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-outcome" assertion으로 "자동 정지 후에도 실제 commit guard가 만료 이후 출고를 거부한다. 중지 지연의 독립 인수는 delayed-guard 사례에 있다."를 확인한다
    그러면 "allocation-suspended" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "expiry-duty-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "held-20" assertion으로 "유효성 만료는 실제 물량 감소가 아니다.20BOX가 남는다."를 확인한다
    그러면 "api-executable-zero" assertion으로 "예약20은 책임으로 보존하지만 신규 실행 가능 배분0BOX다."를 확인한다
    그러면 "sweep-db-expiry-event-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-refresh-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-still-unverified" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-refresh-time" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-followup-open-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-allocation-suspended" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-no-user-business-event" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-expiry-event-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-refresh-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-still-unverified" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-refresh-time" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-followup-open-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-allocation-suspended" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-no-user-business-event" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-no-due-task" assertion으로 "같은 만료 경계는 이미 처리됐다. 반복 sweep에서 새 자율 task를 만들지 않는다."를 확인한다
    그러면 "repeat-events-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "repeat-assessments-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "repeat-obligations-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "before-no-expiry-events" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "before-no-expiry-assessments" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "before-no-expiry-obligations" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "sweep-db-followup-responsibility-present" assertion으로 "사용자 요청 전에 자동 만료 의무의 실제 ID·root·책임 업무·인간 owner·supervisor·다음 행동·확인 시점이 모두 존재해야 한다. 빈 원 행이나 owner 없는 OPEN은 통과하지 못한다."를 확인한다
    그러면 "sweep-db-followup-responsibility-values" assertion으로 "자동 만료 의무는 fixture의 인간 책임자·감독자·다음 행동·확인 시점을 갖는다. 뒤 출고의 책임 복구로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-current-expiry-assignment-one" assertion으로 "자동 생성된 만료 후속 의무의 현재 OPEN assignment는 하나다. 초기 활성화 의무와 구별하고 빈 행·중복 assignment를 거부한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-responsibility-present" assertion으로 "자동 sweep의 같은 snapshot에 실제 assignment ID·의무·root·업무와 인간 책임 필드가 모두 있어야 한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-responsibility-values" assertion으로 "자동 sweep가 current OPEN assignment의 owner·supervisor·다음 행동·확인 시점을 함께 upsert한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-obligationId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-expiry-assignment-rootId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-expiry-assignment-workId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-followup-responsibility-present" assertion으로 "사용자 요청 전에 자동 만료 의무의 실제 ID·root·책임 업무·인간 owner·supervisor·다음 행동·확인 시점이 모두 존재해야 한다. 빈 원 행이나 owner 없는 OPEN은 통과하지 못한다."를 확인한다
    그러면 "repeat-db-followup-responsibility-values" assertion으로 "자동 만료 의무는 fixture의 인간 책임자·감독자·다음 행동·확인 시점을 갖는다. 뒤 출고의 책임 복구로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-current-expiry-assignment-one" assertion으로 "자동 생성된 만료 후속 의무의 현재 OPEN assignment는 하나다. 초기 활성화 의무와 구별하고 빈 행·중복 assignment를 거부한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-responsibility-present" assertion으로 "자동 sweep의 같은 snapshot에 실제 assignment ID·의무·root·업무와 인간 책임 필드가 모두 있어야 한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-responsibility-values" assertion으로 "자동 sweep가 current OPEN assignment의 owner·supervisor·다음 행동·확인 시점을 함께 upsert한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-obligationId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-expiry-assignment-rootId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-expiry-assignment-workId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-expiry-assignment-identity-kept" assertion으로 "반복 sweep는 동일한 현재 OPEN assignment 원 행·ID·owner·supervisor·다음 행동·확인 시점을 유지한다."를 확인한다

  시나리오: sweeper 중지 중 grant 만료 뒤 출고를 거부한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "grant-expiry-delayed-guard"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "warehouse" 역할이 "reserve" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-sweeper" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "operations" 역할이 "guard-api" 행동을 수행한다
    만일 "시스템" 역할이 "guard-db" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "boundary-index-deadline" assertion으로 "별도 이벤트 전에 scope의 다음 실제 유효 경계를 등록한다."를 확인한다
    그러면 "boundary-cause" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "guard-db-segments-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-genealogy-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-movements-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-quantity-delta-zero" assertion으로 "전후 보유량20−20=0BOX다. 현재와 baseline 단위를 모두 검사한다."를 확인한다
    그러면 "guard-no-consumed-allocation" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-original-allocation-kept" assertion으로 "실행 거부가 기존 예약을 삭제·대체하지 않는다. SUSPENDED 대조 전이는 허용한다."를 확인한다
    그러면 "guard-allocation-quantity-kept" assertion으로 "예약20의 미해결 책임 수량을 보존한다."를 확인한다
    그러면 "no-new-event" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "dispatch-outbox-zero" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-outcome" assertion으로 "sweeper가 중지돼도 실제 commit guard가 만료 이후 출고를 거부한다."를 확인한다
    그러면 "allocation-suspended" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "expiry-duty-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "held-20" assertion으로 "유효성 만료는 실제 물량 감소가 아니다.20BOX가 남는다."를 확인한다
    그러면 "api-executable-zero" assertion으로 "예약20은 책임으로 보존하지만 신규 실행 가능 배분0BOX다."를 확인한다

  시나리오: 새 사건 없이 policy 만료20을 정지하고 sweeper 지연에도 출고를 막는다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "policy-expiry-no-event"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "warehouse" 역할이 "reserve" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-sweeper" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "start-sweeper" 행동을 수행한다
    만일 "시스템" 역할이 "sweep" 행동을 수행한다
    만일 "시스템" 역할이 "sweep-terminal" 행동을 수행한다
    만일 "시스템" 역할이 "sweep-db" 행동을 수행한다
    만일 "시스템" 역할이 "repeat-sweep" 행동을 수행한다
    만일 "operations" 역할이 "repeat-api" 행동을 수행한다
    만일 "시스템" 역할이 "repeat-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-again-sweeper" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "operations" 역할이 "guard-api" 행동을 수행한다
    만일 "시스템" 역할이 "guard-db" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "sweep-terminal-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "sweep-terminal-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "boundary-index-deadline" assertion으로 "별도 이벤트 전에 scope의 다음 실제 유효 경계를 등록한다."를 확인한다
    그러면 "boundary-cause" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "guard-db-segments-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-genealogy-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-movements-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-quantity-delta-zero" assertion으로 "전후 보유량20−20=0BOX다. 현재와 baseline 단위를 모두 검사한다."를 확인한다
    그러면 "guard-no-consumed-allocation" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-original-allocation-kept" assertion으로 "실행 거부가 기존 예약을 삭제·대체하지 않는다. SUSPENDED 대조 전이는 허용한다."를 확인한다
    그러면 "guard-allocation-quantity-kept" assertion으로 "예약20의 미해결 책임 수량을 보존한다."를 확인한다
    그러면 "no-new-event" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "dispatch-outbox-zero" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-outcome" assertion으로 "자동 정지 후에도 실제 commit guard가 만료 이후 출고를 거부한다. 중지 지연의 독립 인수는 delayed-guard 사례에 있다."를 확인한다
    그러면 "allocation-suspended" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "expiry-duty-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "held-20" assertion으로 "유효성 만료는 실제 물량 감소가 아니다.20BOX가 남는다."를 확인한다
    그러면 "api-executable-zero" assertion으로 "예약20은 책임으로 보존하지만 신규 실행 가능 배분0BOX다."를 확인한다
    그러면 "sweep-db-expiry-event-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-refresh-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-still-unverified" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-refresh-time" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-followup-open-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-allocation-suspended" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-no-user-business-event" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-expiry-event-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-refresh-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-still-unverified" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-refresh-time" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-followup-open-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-allocation-suspended" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-no-user-business-event" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-no-due-task" assertion으로 "같은 만료 경계는 이미 처리됐다. 반복 sweep에서 새 자율 task를 만들지 않는다."를 확인한다
    그러면 "repeat-events-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "repeat-assessments-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "repeat-obligations-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "before-no-expiry-events" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "before-no-expiry-assessments" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "before-no-expiry-obligations" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "sweep-db-followup-responsibility-present" assertion으로 "사용자 요청 전에 자동 만료 의무의 실제 ID·root·책임 업무·인간 owner·supervisor·다음 행동·확인 시점이 모두 존재해야 한다. 빈 원 행이나 owner 없는 OPEN은 통과하지 못한다."를 확인한다
    그러면 "sweep-db-followup-responsibility-values" assertion으로 "자동 만료 의무는 fixture의 인간 책임자·감독자·다음 행동·확인 시점을 갖는다. 뒤 출고의 책임 복구로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-current-expiry-assignment-one" assertion으로 "자동 생성된 만료 후속 의무의 현재 OPEN assignment는 하나다. 초기 활성화 의무와 구별하고 빈 행·중복 assignment를 거부한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-responsibility-present" assertion으로 "자동 sweep의 같은 snapshot에 실제 assignment ID·의무·root·업무와 인간 책임 필드가 모두 있어야 한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-responsibility-values" assertion으로 "자동 sweep가 current OPEN assignment의 owner·supervisor·다음 행동·확인 시점을 함께 upsert한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-obligationId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-expiry-assignment-rootId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-expiry-assignment-workId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-followup-responsibility-present" assertion으로 "사용자 요청 전에 자동 만료 의무의 실제 ID·root·책임 업무·인간 owner·supervisor·다음 행동·확인 시점이 모두 존재해야 한다. 빈 원 행이나 owner 없는 OPEN은 통과하지 못한다."를 확인한다
    그러면 "repeat-db-followup-responsibility-values" assertion으로 "자동 만료 의무는 fixture의 인간 책임자·감독자·다음 행동·확인 시점을 갖는다. 뒤 출고의 책임 복구로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-current-expiry-assignment-one" assertion으로 "자동 생성된 만료 후속 의무의 현재 OPEN assignment는 하나다. 초기 활성화 의무와 구별하고 빈 행·중복 assignment를 거부한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-responsibility-present" assertion으로 "자동 sweep의 같은 snapshot에 실제 assignment ID·의무·root·업무와 인간 책임 필드가 모두 있어야 한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-responsibility-values" assertion으로 "자동 sweep가 current OPEN assignment의 owner·supervisor·다음 행동·확인 시점을 함께 upsert한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-obligationId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-expiry-assignment-rootId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-expiry-assignment-workId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-expiry-assignment-identity-kept" assertion으로 "반복 sweep는 동일한 현재 OPEN assignment 원 행·ID·owner·supervisor·다음 행동·확인 시점을 유지한다."를 확인한다

  시나리오: sweeper 중지 중 policy 만료 뒤 출고를 거부한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "policy-expiry-delayed-guard"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "warehouse" 역할이 "reserve" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-sweeper" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "operations" 역할이 "guard-api" 행동을 수행한다
    만일 "시스템" 역할이 "guard-db" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "boundary-index-deadline" assertion으로 "별도 이벤트 전에 scope의 다음 실제 유효 경계를 등록한다."를 확인한다
    그러면 "boundary-cause" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "guard-db-segments-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-genealogy-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-movements-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-quantity-delta-zero" assertion으로 "전후 보유량20−20=0BOX다. 현재와 baseline 단위를 모두 검사한다."를 확인한다
    그러면 "guard-no-consumed-allocation" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-original-allocation-kept" assertion으로 "실행 거부가 기존 예약을 삭제·대체하지 않는다. SUSPENDED 대조 전이는 허용한다."를 확인한다
    그러면 "guard-allocation-quantity-kept" assertion으로 "예약20의 미해결 책임 수량을 보존한다."를 확인한다
    그러면 "no-new-event" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "dispatch-outbox-zero" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-outcome" assertion으로 "sweeper가 중지돼도 실제 commit guard가 만료 이후 출고를 거부한다."를 확인한다
    그러면 "allocation-suspended" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "expiry-duty-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "held-20" assertion으로 "유효성 만료는 실제 물량 감소가 아니다.20BOX가 남는다."를 확인한다
    그러면 "api-executable-zero" assertion으로 "예약20은 책임으로 보존하지만 신규 실행 가능 배분0BOX다."를 확인한다

  시나리오: 정책과 정의 전환이 영향 scope의 다음 경계를 갱신한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "planned-transition-reindexes"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "owner" 역할이 "wait" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "configApprover" 역할이 "policy-activate" 행동을 수행한다
    만일 "configApprover" 역할이 "definition-activate" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "boundary-before" assertion으로 "전환 전 최초 경계는 기존 정책 만료다."를 확인한다
    그러면 "boundary-after" assertion으로 "예정된 두 전환의 영향 scope에서 경계를 앞당긴다."를 확인한다
    그러면 "boundary-transition-set" assertion으로 "두 종류의 변경이 같은 scope index에 연결된다."를 확인한다
    그러면 "old-work-stays-v1" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "no-execution-on-activation" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다

  시나리오: 응급 repair의 dry-run diff·현재 인가·근거·audit·재검증을 수행한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "emergency-repair-dryrun-apply"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "시스템" 역할이 "damage" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "operations" 역할이 "dry-run" 행동을 수행한다
    만일 "operations" 역할이 "dryrun-api" 행동을 수행한다
    만일 "시스템" 역할이 "dryrun-db" 행동을 수행한다
    만일 "readOnly" 역할이 "unauthorized-apply" 행동을 수행한다
    만일 "operations" 역할이 "denied-api" 행동을 수행한다
    만일 "시스템" 역할이 "denied-db" 행동을 수행한다
    만일 "operations" 역할이 "apply" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "dryrun-diff" assertion으로 "dry-run은 실제 손상 projection의 현재값과 예정값 차이를 반환한다. 업무 수량 변경은 포함하지 않는다."를 확인한다
    그러면 "denied-db-segments-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "denied-db-genealogy-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "denied-db-movements-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "denied-db-allocations-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "denied-db-quantity-delta-zero" assertion으로 "전후 보유량20−20=0BOX다. 현재와 baseline 단위를 모두 검사한다."를 확인한다
    그러면 "repair-denied" assertion으로 "관리 복구도 조회 전용 주체가 쓰기를 실행하지 못한다."를 확인한다
    그러면 "repair-applied-audit" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "repair-evidence" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "repair-diff-hash-audit" assertion으로 "승인된 diff hash와 실제 apply audit가 같아야 한다."를 확인한다
    그러면 "projection-repaired" assertion으로 "apply 후 원장 revision을 독립 DB와 다시 대조한다."를 확인한다
    그러면 "no-direct-sql-recovery" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "repair-discovery-scope" assertion으로 "장애 발견에서 repair 대상 실물 scope를 먼저 식별한다."를 확인한다
    그러면 "repair-last-committed-command" assertion으로 "직접 SQL 대신 마지막 확정 application 명령을 확인한다."를 확인한다
    그러면 "repair-last-commit-db" assertion으로 "API에서 찾은 최근 확정 명령을 DB 원 행과 대조한다."를 확인한다
    그러면 "repair-held-20" assertion으로 "repair는20BOX 실제 보유량을 생성·삭제하지 않는다."를 확인한다
    그러면 "repair-owner-kept" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "before-db-projection-one" assertion으로 "인가된 APPLY 이전 손상 projection 대상은 하나이며 sourceRevision0과 실제 ID를 유지한다."를 확인한다
    그러면 "before-db-projection-identity-revision" assertion으로 "인가된 APPLY 이전 손상 projection 대상은 하나이며 sourceRevision0과 실제 ID를 유지한다."를 확인한다
    그러면 "before-db-projection-complete" assertion으로 "인가된 APPLY 이전 손상 projection 대상은 하나이며 sourceRevision0과 실제 ID를 유지한다."를 확인한다
    그러면 "dryrun-db-projection-one" assertion으로 "인가된 APPLY 이전 손상 projection 대상은 하나이며 sourceRevision0과 실제 ID를 유지한다."를 확인한다
    그러면 "dryrun-db-projection-identity-revision" assertion으로 "인가된 APPLY 이전 손상 projection 대상은 하나이며 sourceRevision0과 실제 ID를 유지한다."를 확인한다
    그러면 "dryrun-db-projection-complete" assertion으로 "인가된 APPLY 이전 손상 projection 대상은 하나이며 sourceRevision0과 실제 ID를 유지한다."를 확인한다
    그러면 "dryrun-db-projection-unchanged" assertion으로 "dry-run·거부된 APPLY는 projection ID·scope·revision·값을 포함한 독립 원 행 전체를 바꾸지 않는다."를 확인한다
    그러면 "denied-db-projection-one" assertion으로 "인가된 APPLY 이전 손상 projection 대상은 하나이며 sourceRevision0과 실제 ID를 유지한다."를 확인한다
    그러면 "denied-db-projection-identity-revision" assertion으로 "인가된 APPLY 이전 손상 projection 대상은 하나이며 sourceRevision0과 실제 ID를 유지한다."를 확인한다
    그러면 "denied-db-projection-complete" assertion으로 "인가된 APPLY 이전 손상 projection 대상은 하나이며 sourceRevision0과 실제 ID를 유지한다."를 확인한다
    그러면 "denied-db-projection-unchanged" assertion으로 "dry-run·거부된 APPLY는 projection ID·scope·revision·값을 포함한 독립 원 행 전체를 바꾸지 않는다."를 확인한다

  시나리오: DB·원문·정의·evaluator·skills·config의 complete 복원을 검증한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "restore-complete"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "owner" 역할이 "wait" 행동을 수행한다
    만일 "warehouse" 역할이 "split" 행동을 수행한다
    만일 "receiver" 역할이 "evidence" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "backup" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-backup" 행동을 수행한다
    만일 "시스템" 역할이 "restore" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-restore" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    만일 "operations" 역할이 "restored-evidence" 행동을 수행한다
    만일 "foreign" 역할이 "cross-org" 행동을 수행한다
    그러면 "backup-parts" assertion으로 "백업 ACK가 아니라 실제 bundle part inventory를 확인한다."를 확인한다
    그러면 "backup-no-secret" assertion으로 "비밀 대신 복구에 필요한 명시적 비밀 없는 config만 포함한다."를 확인한다
    그러면 "isolated-restore" assertion으로 "새 isolated host 환경의 실제 복원을 관찰한다."를 확인한다
    그러면 "restore-completeness" assertion으로 "필요 원문 또는 과거 evaluator 누락을 복구 완료로 표시하지 않는다."를 확인한다
    그러면 "restore-required-gap" assertion으로 "누락 part를 비대상/0으로 감추지 않는다."를 확인한다
    그러면 "restore-quantity-20" assertion으로 "실제 split8+12=20BOX다. retired 부모를 현재량에 더하지 않는다."를 확인한다
    그러면 "restore-genealogy" assertion으로 "백업 전 실제 분할 계보와 복원 원 행이 같다."를 확인한다
    그러면 "restore-duties" assertion으로 "현재 유효한 owner·다음 행동·기한·업무 링크를 그대로 복원한다."를 확인한다
    그러면 "v1-work" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "v1-evaluator" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "past-assessment-preserved" assertion으로 "과거 v1 판정 의미와 input snapshot을 현재 정의로 덮어쓰지 않는다."를 확인한다
    그러면 "blob-hash" assertion으로 "실제 복원 파일 bytes/hash의 독립 extractor 관찰을 확인한다."를 확인한다
    그러면 "cross-org-denied" assertion으로 "복원 환경에서도 다른 조직의 direct ID 조회를 차단한다."를 확인한다
    그러면 "no-cross-org-grant" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "secret-manager-channel" assertion으로 "secret의 실제 복원 경로가 bundle/repository/evidence가 아님을 확인한다."를 확인한다
    그러면 "db-evidence-hash" assertion으로 "복원 DB의 원 document hash를 실제 원문 bytes의 hash와 대조한다."를 확인한다
    그러면 "api-evidence-hash" assertion으로 "API도 같은 불변 문서 hash를 반환하며 파일 부재 여부는 별도 가용성으로 보존한다."를 확인한다
    그러면 "api-evidence-availability" assertion으로 "URI 존재만으로 실제 원문 가용성을 확정하지 않는다."를 확인한다
    그러면 "restore-execution-state" assertion으로 "누락된 blob/evaluator로 후속 실행을 열지 않는다."를 확인한다

  시나리오: DB·원문·정의·evaluator·skills·config의 missing-blob 복원을 검증한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "restore-missing-blob"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "owner" 역할이 "wait" 행동을 수행한다
    만일 "warehouse" 역할이 "split" 행동을 수행한다
    만일 "receiver" 역할이 "evidence" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "backup" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-backup" 행동을 수행한다
    만일 "시스템" 역할이 "damage-bundle" 행동을 수행한다
    만일 "시스템" 역할이 "restore" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-restore" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    만일 "operations" 역할이 "restored-evidence" 행동을 수행한다
    만일 "foreign" 역할이 "cross-org" 행동을 수행한다
    그러면 "backup-parts" assertion으로 "백업 ACK가 아니라 실제 bundle part inventory를 확인한다."를 확인한다
    그러면 "backup-no-secret" assertion으로 "비밀 대신 복구에 필요한 명시적 비밀 없는 config만 포함한다."를 확인한다
    그러면 "isolated-restore" assertion으로 "새 isolated host 환경의 실제 복원을 관찰한다."를 확인한다
    그러면 "restore-completeness" assertion으로 "필요 원문 또는 과거 evaluator 누락을 복구 완료로 표시하지 않는다."를 확인한다
    그러면 "restore-required-gap" assertion으로 "누락 part를 비대상/0으로 감추지 않는다."를 확인한다
    그러면 "restore-quantity-20" assertion으로 "실제 split8+12=20BOX다. retired 부모를 현재량에 더하지 않는다."를 확인한다
    그러면 "restore-genealogy" assertion으로 "백업 전 실제 분할 계보와 복원 원 행이 같다."를 확인한다
    그러면 "restore-duties" assertion으로 "현재 유효한 owner·다음 행동·기한·업무 링크를 그대로 복원한다."를 확인한다
    그러면 "v1-work" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "v1-evaluator" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "past-assessment-preserved" assertion으로 "과거 v1 판정 의미와 input snapshot을 현재 정의로 덮어쓰지 않는다."를 확인한다
    그러면 "blob-hash" assertion으로 "실제 복원 파일 bytes/hash의 독립 extractor 관찰을 확인한다."를 확인한다
    그러면 "cross-org-denied" assertion으로 "복원 환경에서도 다른 조직의 direct ID 조회를 차단한다."를 확인한다
    그러면 "no-cross-org-grant" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "secret-manager-channel" assertion으로 "secret의 실제 복원 경로가 bundle/repository/evidence가 아님을 확인한다."를 확인한다
    그러면 "db-evidence-hash" assertion으로 "복원 DB의 원 document hash를 실제 원문 bytes의 hash와 대조한다."를 확인한다
    그러면 "api-evidence-hash" assertion으로 "API도 같은 불변 문서 hash를 반환하며 파일 부재 여부는 별도 가용성으로 보존한다."를 확인한다
    그러면 "api-evidence-availability" assertion으로 "URI 존재만으로 실제 원문 가용성을 확정하지 않는다."를 확인한다
    그러면 "restore-execution-state" assertion으로 "누락된 blob/evaluator로 후속 실행을 열지 않는다."를 확인한다

  시나리오: DB·원문·정의·evaluator·skills·config의 missing-v1-evaluator 복원을 검증한다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "restore-missing-v1-evaluator"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "owner" 역할이 "wait" 행동을 수행한다
    만일 "warehouse" 역할이 "split" 행동을 수행한다
    만일 "receiver" 역할이 "evidence" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "backup" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-backup" 행동을 수행한다
    만일 "시스템" 역할이 "damage-bundle" 행동을 수행한다
    만일 "시스템" 역할이 "restore" 행동을 수행한다
    만일 "시스템" 역할이 "inspect-restore" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    만일 "operations" 역할이 "restored-evidence" 행동을 수행한다
    만일 "foreign" 역할이 "cross-org" 행동을 수행한다
    그러면 "backup-parts" assertion으로 "백업 ACK가 아니라 실제 bundle part inventory를 확인한다."를 확인한다
    그러면 "backup-no-secret" assertion으로 "비밀 대신 복구에 필요한 명시적 비밀 없는 config만 포함한다."를 확인한다
    그러면 "isolated-restore" assertion으로 "새 isolated host 환경의 실제 복원을 관찰한다."를 확인한다
    그러면 "restore-completeness" assertion으로 "필요 원문 또는 과거 evaluator 누락을 복구 완료로 표시하지 않는다."를 확인한다
    그러면 "restore-required-gap" assertion으로 "누락 part를 비대상/0으로 감추지 않는다."를 확인한다
    그러면 "restore-quantity-20" assertion으로 "실제 split8+12=20BOX다. retired 부모를 현재량에 더하지 않는다."를 확인한다
    그러면 "restore-genealogy" assertion으로 "백업 전 실제 분할 계보와 복원 원 행이 같다."를 확인한다
    그러면 "restore-duties" assertion으로 "현재 유효한 owner·다음 행동·기한·업무 링크를 그대로 복원한다."를 확인한다
    그러면 "v1-work" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "v1-evaluator" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "past-assessment-preserved" assertion으로 "과거 v1 판정 의미와 input snapshot을 현재 정의로 덮어쓰지 않는다."를 확인한다
    그러면 "blob-hash" assertion으로 "실제 복원 파일 bytes/hash의 독립 extractor 관찰을 확인한다."를 확인한다
    그러면 "cross-org-denied" assertion으로 "복원 환경에서도 다른 조직의 direct ID 조회를 차단한다."를 확인한다
    그러면 "no-cross-org-grant" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "secret-manager-channel" assertion으로 "secret의 실제 복원 경로가 bundle/repository/evidence가 아님을 확인한다."를 확인한다
    그러면 "db-evidence-hash" assertion으로 "복원 DB의 원 document hash를 실제 원문 bytes의 hash와 대조한다."를 확인한다
    그러면 "api-evidence-hash" assertion으로 "API도 같은 불변 문서 hash를 반환하며 파일 부재 여부는 별도 가용성으로 보존한다."를 확인한다
    그러면 "api-evidence-availability" assertion으로 "URI 존재만으로 실제 원문 가용성을 확정하지 않는다."를 확인한다
    그러면 "restore-execution-state" assertion으로 "누락된 blob/evaluator로 후속 실행을 열지 않는다."를 확인한다

  시나리오: 큐 메시지 없이 DB의 due 의무와 대기를 재발견한다 — harness tick/sweep 없이 scheduler loop가 스스로 찾는다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "due-wait-autonomous-loop"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "owner" 역할이 "wait" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "stop-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "stop-app-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "start-again-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-again-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-again-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "start-loop-while-observing" 행동을 수행한다
    만일 "시스템" 역할이 "terminal" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "terminal-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "durable-wait-before" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "empty-message-queue" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "discovery-source-db" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "attempt-cause-due" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "due-obligation-discovered" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "no-fake-receipt" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "timeout-never-satisfied" assertion으로 "시간 경과·queue 성공이 증거 없는 목표를 충족시키지 않는다."를 확인한다
    그러면 "queue-empty-after-restart" assertion으로 "재시작 뒤 terminal 관찰 시점에도 queue message0이다. 재발견은 DB due index에서만 온다."를 확인한다
    그러면 "autonomous-trigger-loop" assertion으로 "관찰 창의 첫 제출(operationEvidence의 taskId) 행을 scheduler가 직접 기록한 제출 원행에서 읽으면 제출 주체는 scheduler loop다. 요청 parameter의 되풀이가 아니라 scheduler 기록이며 harness tick이 만든 제출이면 실패한다."를 확인한다
    그러면 "autonomous-within-30s" assertion으로 "harness가 관찰 group을 시작하기 직전에 잡은 관찰 경계부터 30초(개발/CI 관찰 제한, plan §10) 안에 자율 제출이 관찰된다. validator의 관찰 창과 같은 기준이다."를 확인한다
    그러면 "autonomous-after-loop-start" assertion으로 "제출은 멈춰 있던 loop process의 시작 command보다 앞설 수 없고 그 시작부터도 30초 안이다. 시작 전 제출은 다른 주체의 것이다."를 확인한다
    그러면 "autonomous-attempt-source" assertion으로 "독립 DB attempt 원행도 scheduler loop가 시작한 시도만 있다. harness tick이나 API 호출로 시작한 시도는 없다."를 확인한다

  시나리오: 새 사건 없이 lot 만료20을 정지하고 sweeper 지연에도 출고를 막는다 — harness tick/sweep 없이 scheduler loop가 스스로 찾는다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "lot-expiry-autonomous-loop"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "warehouse" 역할이 "reserve" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "start-loop-while-observing" 행동을 수행한다
    만일 "시스템" 역할이 "sweep-terminal" 행동을 수행한다
    만일 "시스템" 역할이 "sweep-db" 행동을 수행한다
    만일 "시스템" 역할이 "repeat-sweep" 행동을 수행한다
    만일 "operations" 역할이 "repeat-api" 행동을 수행한다
    만일 "시스템" 역할이 "repeat-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-again-sweeper" 행동을 수행한다
    만일 "warehouse" 역할이 "dispatch" 행동을 수행한다
    만일 "operations" 역할이 "guard-api" 행동을 수행한다
    만일 "시스템" 역할이 "guard-db" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "sweep-terminal-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "sweep-terminal-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "boundary-index-deadline" assertion으로 "별도 이벤트 전에 scope의 다음 실제 유효 경계를 등록한다."를 확인한다
    그러면 "boundary-cause" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "guard-db-segments-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-genealogy-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-movements-unchanged" assertion으로 "허용된 audit·대조 책임과 분리해 금지된 업무 효과 전후 원 행을 비교한다."를 확인한다
    그러면 "guard-db-quantity-delta-zero" assertion으로 "전후 보유량20−20=0BOX다. 현재와 baseline 단위를 모두 검사한다."를 확인한다
    그러면 "guard-no-consumed-allocation" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-original-allocation-kept" assertion으로 "실행 거부가 기존 예약을 삭제·대체하지 않는다. SUSPENDED 대조 전이는 허용한다."를 확인한다
    그러면 "guard-allocation-quantity-kept" assertion으로 "예약20의 미해결 책임 수량을 보존한다."를 확인한다
    그러면 "no-new-event" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "dispatch-outbox-zero" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "guard-outcome" assertion으로 "자동 정지 후에도 실제 commit guard가 만료 이후 출고를 거부한다. 중지 지연의 독립 인수는 delayed-guard 사례에 있다."를 확인한다
    그러면 "allocation-suspended" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "expiry-duty-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "held-20" assertion으로 "유효성 만료는 실제 물량 감소가 아니다.20BOX가 남는다."를 확인한다
    그러면 "api-executable-zero" assertion으로 "예약20은 책임으로 보존하지만 신규 실행 가능 배분0BOX다."를 확인한다
    그러면 "sweep-db-expiry-event-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-refresh-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-still-unverified" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-goal-refresh-time" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-followup-open-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-allocation-suspended" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "sweep-db-no-user-business-event" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-expiry-event-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-refresh-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-still-unverified" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-goal-refresh-time" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-followup-open-one" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-allocation-suspended" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-db-no-user-business-event" assertion으로 "독립 원 행의 발생·판정·후속 책임을 고정 기대값과 대조한다."를 확인한다
    그러면 "repeat-no-due-task" assertion으로 "같은 만료 경계는 이미 처리됐다. 반복 sweep에서 새 자율 task를 만들지 않는다."를 확인한다
    그러면 "repeat-events-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "repeat-assessments-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "repeat-obligations-identity-kept" assertion으로 "반복 sweep는 같은 만료 사건·현재 판정·후속 의무 원 행과 ID를 유지한다."를 확인한다
    그러면 "before-no-expiry-events" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "before-no-expiry-assessments" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "before-no-expiry-obligations" assertion으로 "만료 이전 baseline에는 해당 만료 사건·재평가·후속 의무가 없다. sweep가 만들 효과를 먼저 seed해 통과하지 않는다."를 확인한다
    그러면 "sweep-db-followup-responsibility-present" assertion으로 "사용자 요청 전에 자동 만료 의무의 실제 ID·root·책임 업무·인간 owner·supervisor·다음 행동·확인 시점이 모두 존재해야 한다. 빈 원 행이나 owner 없는 OPEN은 통과하지 못한다."를 확인한다
    그러면 "sweep-db-followup-responsibility-values" assertion으로 "자동 만료 의무는 fixture의 인간 책임자·감독자·다음 행동·확인 시점을 갖는다. 뒤 출고의 책임 복구로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-current-expiry-assignment-one" assertion으로 "자동 생성된 만료 후속 의무의 현재 OPEN assignment는 하나다. 초기 활성화 의무와 구별하고 빈 행·중복 assignment를 거부한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-responsibility-present" assertion으로 "자동 sweep의 같은 snapshot에 실제 assignment ID·의무·root·업무와 인간 책임 필드가 모두 있어야 한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-responsibility-values" assertion으로 "자동 sweep가 current OPEN assignment의 owner·supervisor·다음 행동·확인 시점을 함께 upsert한다."를 확인한다
    그러면 "sweep-db-expiry-assignment-obligationId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-expiry-assignment-rootId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "sweep-db-expiry-assignment-workId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-followup-responsibility-present" assertion으로 "사용자 요청 전에 자동 만료 의무의 실제 ID·root·책임 업무·인간 owner·supervisor·다음 행동·확인 시점이 모두 존재해야 한다. 빈 원 행이나 owner 없는 OPEN은 통과하지 못한다."를 확인한다
    그러면 "repeat-db-followup-responsibility-values" assertion으로 "자동 만료 의무는 fixture의 인간 책임자·감독자·다음 행동·확인 시점을 갖는다. 뒤 출고의 책임 복구로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-current-expiry-assignment-one" assertion으로 "자동 생성된 만료 후속 의무의 현재 OPEN assignment는 하나다. 초기 활성화 의무와 구별하고 빈 행·중복 assignment를 거부한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-responsibility-present" assertion으로 "자동 sweep의 같은 snapshot에 실제 assignment ID·의무·root·업무와 인간 책임 필드가 모두 있어야 한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-responsibility-values" assertion으로 "자동 sweep가 current OPEN assignment의 owner·supervisor·다음 행동·확인 시점을 함께 upsert한다."를 확인한다
    그러면 "repeat-db-expiry-assignment-obligationId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-expiry-assignment-rootId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-db-expiry-assignment-workId-linked" assertion으로 "자동 sweep가 만든 assignment는 같은 snapshot의 만료 의무·stable root·책임 업무에 연결된다. 이름이나 count만 같은 별도 책임으로 대신하지 않는다."를 확인한다
    그러면 "repeat-expiry-assignment-identity-kept" assertion으로 "반복 sweep는 동일한 현재 OPEN assignment 원 행·ID·owner·supervisor·다음 행동·확인 시점을 유지한다."를 확인한다
    그러면 "autonomous-trigger-loop" assertion으로 "관찰 창의 첫 제출(operationEvidence의 taskId) 행을 scheduler가 직접 기록한 제출 원행에서 읽으면 제출 주체는 scheduler loop다. 요청 parameter의 되풀이가 아니라 scheduler 기록이며 harness tick이 만든 제출이면 실패한다."를 확인한다
    그러면 "autonomous-within-30s" assertion으로 "harness가 관찰 group을 시작하기 직전에 잡은 관찰 경계부터 30초(개발/CI 관찰 제한, plan §10) 안에 자율 제출이 관찰된다. validator의 관찰 창과 같은 기준이다."를 확인한다
    그러면 "autonomous-after-loop-start" assertion으로 "제출은 멈춰 있던 loop process의 시작 command보다 앞설 수 없고 그 시작부터도 30초 안이다. 시작 전 제출은 다른 주체의 것이다."를 확인한다
    그러면 "autonomous-attempt-source" assertion으로 "독립 DB attempt 원행도 scheduler loop가 시작한 시도만 있다. harness tick이나 API 호출로 시작한 시도는 없다."를 확인한다

  시나리오: 종료된 부모 뒤 이상 접수의 연결 장애를 DB에서 복구한다 — harness tick/sweep 없이 scheduler loop가 스스로 찾는다
    먼저 사례 파일 "verification/cases/T26/case.json"의 "orphan-intake-autonomous-loop"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "link-fault" 행동을 수행한다
    만일 "receiver" 역할이 "anomaly" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "link-fault-clear" 행동을 수행한다
    만일 "시스템" 역할이 "stop-again-api" 행동을 수행한다
    만일 "시스템" 역할이 "stop-again-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "stop-again-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "start-again-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-again-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-again-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "start-loop-while-observing" 행동을 수행한다
    만일 "시스템" 역할이 "terminal" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "terminal-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "before-new-work-zero" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "before-link-pending" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "before-intake-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "before-intake-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "before-intake-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "after-work-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "after-duty-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "intake-link-confirmed" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "canonical-intake-linked-duty" assertion으로 "재시도에서 같은 접수와 단 하나 의무를 연결한다."를 확인한다
    그러면 "autonomous-trigger-loop" assertion으로 "관찰 창의 첫 제출(operationEvidence의 taskId) 행을 scheduler가 직접 기록한 제출 원행에서 읽으면 제출 주체는 scheduler loop다. 요청 parameter의 되풀이가 아니라 scheduler 기록이며 harness tick이 만든 제출이면 실패한다."를 확인한다
    그러면 "autonomous-within-30s" assertion으로 "harness가 관찰 group을 시작하기 직전에 잡은 관찰 경계부터 30초(개발/CI 관찰 제한, plan §10) 안에 자율 제출이 관찰된다. validator의 관찰 창과 같은 기준이다."를 확인한다
    그러면 "autonomous-after-loop-start" assertion으로 "제출은 멈춰 있던 loop process의 시작 command보다 앞설 수 없고 그 시작부터도 30초 안이다. 시작 전 제출은 다른 주체의 것이다."를 확인한다
    그러면 "autonomous-attempt-source" assertion으로 "독립 DB attempt 원행도 scheduler loop가 시작한 시도만 있다. harness tick이나 API 호출로 시작한 시도는 없다."를 확인한다
