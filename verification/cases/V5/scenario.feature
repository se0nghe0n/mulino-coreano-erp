# language: ko
@V5 @D26 @sit @uat @recovery
기능: queue 성공과 업무 책임을 구별하는 지속 실행

  시나리오: queue 성공 뒤 WAITING만 남겨 모든 process를 재시작하고 같은 의무 하나를 재개한다
    먼저 사례 파일 "verification/cases/V5/case.json"의 "waiting-full-restart"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "owner" 역할이 "wait" 행동을 수행한다
    만일 "시스템" 역할이 "queue-success" 행동을 수행한다
    만일 "시스템" 역할이 "initial-tick" 행동을 수행한다
    만일 "시스템" 역할이 "initial-terminal" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "stop-all-api" 행동을 수행한다
    만일 "시스템" 역할이 "stop-all-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "stop-all-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "stop-all-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "restart-all-api" 행동을 수행한다
    만일 "시스템" 역할이 "restart-all-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "restart-all-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "restart-all-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "tick" 행동을 수행한다
    만일 "시스템" 역할이 "terminal" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "initial-terminal-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "initial-terminal-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "terminal-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "before-empty-queue" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "before-waiting" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "same-duty-id" assertion으로 "전체 재시작 뒤에도 원 stable obligation root 하나를 재개한다."를 확인한다
    그러면 "resumed-once" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "restart-within30" assertion으로 "실제 마지막 process 기동 완료→task terminal까지 wall-clock30초 이내다. 업무 시계와 구분한다."를 확인한다
    그러면 "api-assessment-unsatisfied" assertion으로 "queue 성공은 목표 충족0이다."를 확인한다
    그러면 "no-satisfied-assessment" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "no-fulfilled-close" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "all-process-running" assertion으로 "실제 restart lifecycle validator가 전후 instance 차이와 terminal을 검사한다."를 확인한다

  시나리오: 두 실제 worker의 claim 경합·lease 만료·늦은 이전 token의 commit을 검증한다
    먼저 사례 파일 "verification/cases/V5/case.json"의 "two-workers-expired-fence"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "owner" 역할이 "wait" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "pause-a" 행동을 수행한다
    만일 "시스템" 역할이 "tick-a" 행동을 수행한다
    만일 "시스템" 역할이 "claim-a" 행동을 수행한다
    만일 "시스템" 역할이 "barrier-a" 행동을 수행한다
    만일 "operations" 역할이 "before" 행동을 수행한다
    만일 "시스템" 역할이 "before-db" 행동을 수행한다
    만일 "시스템" 역할이 "expire-lease" 행동을 수행한다
    만일 "시스템" 역할이 "pause-b" 행동을 수행한다
    만일 "시스템" 역할이 "tick-b" 행동을 수행한다
    만일 "시스템" 역할이 "claim-b" 행동을 수행한다
    만일 "시스템" 역할이 "barrier-b" 행동을 수행한다
    만일 "operations" 역할이 "takeover-api" 행동을 수행한다
    만일 "시스템" 역할이 "takeover-db" 행동을 수행한다
    만일 "시스템" 역할이 "resume-b" 행동을 수행한다
    만일 "시스템" 역할이 "terminal-b" 행동을 수행한다
    만일 "시스템" 역할이 "resume-a" 행동을 수행한다
    만일 "시스템" 역할이 "terminal-a" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "terminal-b-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-b-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "terminal-a-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-a-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "live-claim-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "new-live-worker" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "after-terminal-live-claims-zero" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "claim-exact-TTL" assertion으로 "실제 claim은 통제된09:00:10에 취득하고 TTL5초인09:00:15에 만료한다."를 확인한다
    그러면 "claim-scope-unique" assertion으로 "인계 시점에 동일 scope의 유효 claim은 하나다."를 확인한다
    그러면 "old-claim-expired" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "lease-five-seconds" assertion으로 "고정 test profile TTL5초를 실제 lease 시각으로 검증한다."를 확인한다
    그러면 "new-fence-used-at-commit" assertion으로 "신규 worker commit은 실제 현재 claim의 token을 사용한다."를 확인한다
    그러면 "one-command-commit" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "no-stale-commit" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "stale-attempt-rejected" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-attempt-results" assertion으로 "실제 API에서 조회한 두 worker attempt의 terminal 결과를 DB 거부·commit 행과 대조한다."를 확인한다
    그러면 "distinct-version-attempt-tuple" assertion으로 "deliveryAttempt은1→2, causal eventRevision은1, 업무 definitionVersion은v1로 별개다."를 확인한다
    그러면 "one-duty-transition" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "single-escalation-outbox" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "unique-escalation-operation" assertion으로 "의무 escalation의 동일 외부 요청은 하나다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다

  시나리오: commit 전후 반복 crash와 전체 restart에도 확정 효과 한 번을 유지한다
    먼저 사례 파일 "verification/cases/V5/case.json"의 "repeated-crash-no-duplicate"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "owner" 역할이 "wait" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "crash-0" 행동을 수행한다
    만일 "시스템" 역할이 "tick-0" 행동을 수행한다
    만일 "시스템" 역할이 "terminal-0" 행동을 수행한다
    만일 "시스템" 역할이 "restart-0-api" 행동을 수행한다
    만일 "시스템" 역할이 "restart-0-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "restart-0-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "restart-0-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "expire-0" 행동을 수행한다
    만일 "시스템" 역할이 "crash-1" 행동을 수행한다
    만일 "시스템" 역할이 "tick-1" 행동을 수행한다
    만일 "시스템" 역할이 "terminal-1" 행동을 수행한다
    만일 "시스템" 역할이 "restart-1-api" 행동을 수행한다
    만일 "시스템" 역할이 "restart-1-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "restart-1-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "restart-1-worker-b" 행동을 수행한다
    만일 "시스템" 역할이 "expire-1" 행동을 수행한다
    만일 "시스템" 역할이 "final-clock" 행동을 수행한다
    만일 "시스템" 역할이 "final-tick" 행동을 수행한다
    만일 "시스템" 역할이 "final-terminal" 행동을 수행한다
    만일 "operations" 역할이 "api" 행동을 수행한다
    만일 "시스템" 역할이 "db" 행동을 수행한다
    그러면 "terminal-0-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-0-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "terminal-1-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-1-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "final-terminal-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "final-terminal-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "exactly-one-commit" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "exactly-one-transition" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "single-escalation-outbox" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "unique-escalation-operation" assertion으로 "반복 restart에서 알림 outbox의 동일 externalOperationId도 복제되지 않는다."를 확인한다
    그러면 "attempt-key-first-retry" assertion으로 "worker retry 번호·restart는 같은 logical command key를 바꾸지 않는다."를 확인한다
    그러면 "attempt-key-final-replay" assertion으로 "commit 후 응답 유실 재시도도 같은 key다."를 확인한다
    그러면 "three-observed-attempts" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "no-physical-duplication" assertion으로 "같은 physicalScope가 반복 장애에서 복제되지 않는다."를 확인한다
    그러면 "held-remains20" assertion으로 "기술 retry는 별도 물량을 생성하지 않는다.20BOX다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다

  시나리오: 세 번의 실제 worker 실패 소진 뒤 인간 책임과 다음 확인을 유지한다
    먼저 사례 파일 "verification/cases/V5/case.json"의 "bounded-retry-exhausted-owner"를 준비한다
    만일 "시스템" 역할이 "setup" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-api" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-scheduler" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-a" 행동을 수행한다
    만일 "시스템" 역할이 "start-app-worker-b" 행동을 수행한다
    만일 "owner" 역할이 "create" 행동을 수행한다
    만일 "owner" 역할이 "create-activate" 행동을 수행한다
    만일 "owner" 역할이 "wait" 행동을 수행한다
    만일 "시스템" 역할이 "advance" 행동을 수행한다
    만일 "시스템" 역할이 "permanent-failure" 행동을 수행한다
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
    그러면 "terminal-0-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-0-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "terminal-1-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-1-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "terminal-2-origin" assertion으로 "실제 scheduler가 자율 생성한 task의 종료를 관찰한다."를 확인한다
    그러면 "terminal-2-terminal" assertion으로 "제출 ACK가 아니라 실제 task terminal을 확인한다."를 확인한다
    그러면 "db-current-assignment-one" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "db-owner" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-duty-status" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-action" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "db-next-check" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "api-owner" assertion으로 "API가 DB와 같은 인간 주 책임자를 반환한다."를 확인한다
    그러면 "api-action" assertion으로 "이름이 같은 Boolean 대신 실제 다음 행동을 확인한다."를 확인한다
    그러면 "api-check" assertion으로 "고정 업무 시계의 다음 확인 시점을 검사한다."를 확인한다
    그러면 "three-attempts" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "retry-exhausted-state" assertion으로 "해당 scope의 각 실제 원 행 값과 고정 기대값을 대조한다."를 확인한다
    그러면 "no-committed-effect" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "no-duty-duplicate" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
    그러면 "no-satisfied-goal" assertion으로 "독립 DB 원 행을 정확한 대상 scope로 세어 중복·누락을 거부한다."를 확인한다
