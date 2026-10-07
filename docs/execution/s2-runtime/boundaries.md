# S2 runtime 계약

업무 성공과 기술 실행 성공을 분리하고 worker 재시작 후 DB에서 같은
효과 범위를 찾는다. `ExecutionScopes`는 organization/work/capability/
command별 fencing token을 보존한다. `ExecutionAttempts`는 별도 이력을
남긴다. stale token과 만료 시도는 caller transaction의 effect와 함께
rollback된다. 두 worker의 scope lock은 `FOR UPDATE SKIP LOCKED`다.

`TransactionalOutboxPort`는 caller의 Spring transaction이 없으면 거부한다.
외부 발행 전에 IN_FLIGHT를 commit한다. 응답 유실이나 process 중단의
만료 lease는 UNKNOWN_EXTERNAL로 보존하며 자동 발행하지 않는다.
상대 멱등/authoritative lookup 지원은 delivery 때 명시해서 저장한다.
재발행은 권한 있는 confirmed failure 대조 기록 뒤 같은 operation ID다.
로컬 취소나 시도 성공은 외부 취소·목표 충족을 뜻하지 않는다.

queue payload의 secret key와 bearer/private-key 원문은 거부한다. 오류에는
구조화 technical code만 저장한다. canonical Work·Goal·Obligation state를
runtime table에 복제하지 않는다. RecoverySchedules는 due discovery index다.
시간 경과는 current grant/policy와 재개 predicate 확인의 이유이며 승인이나
도착의 근거가 아니다.

canonical duty는 RuntimeDutyPort로 실제 책임 module에 연결한다.
확정 외부 결과는 immutable original과 verified canonical operation의
증거 guard를 통과해야 한다. fixture native JVM runner와 독립 HTTP/SQLite
외부 효과 service를 제공한다. 실제 process kill/restart 종단 실행은 아직
NOT_RUN이다. RecoverySchedules는 due 업무를 pending assessment로 표시하며
시간만으로 재개하지 않는다. 안전 명령 retry는 공통 command의 원 요청
재실행 API와 결합해야 한다.

실제 PostgreSQL 계약 tests 6건이 통과했다. 마지막 두 tests의 authority와
responsibility hook은 mock이며 전체 인가·책임 통합 성공으로 세지 않는다.
현재 source와 통합된 module의 결합 checks가 남는다. V5/V6/V7 전체는
NOT_RUN이다.
