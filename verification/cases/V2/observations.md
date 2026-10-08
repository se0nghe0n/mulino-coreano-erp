# 분할·배분 경합과 실제량 정정

실제 제품/API/DB/host/model은 NOT_RUN이다. 이 파일은 구현 전
검증 계약이며 synthetic 정책·문서를 실제 규제값으로 사용하지 않는다.
fixture는 식별된 시작 사실만 설치한다. 검증할 효과·승인은 반드시
명시 행동으로 실행한다. 조회/거부 감사·REJECTED 명령 결과·책임 알림을
허용하면서 COMMITTED 결과와 BUSINESS outbox 등 금지 효과를 분리한다.

rawRows는 sourceEvidence의 actual SQL·parameters·mapping·artifact를
가진 동일 scope/snapshot의 원 행이다. fixture alias와 신규 result ID는
strict 참조로 연결하고 수량·unit·상태·시간은 독립 고정 oracle로 둔다.
책임 count는 실제 obligationId와 current assignment scope로 한정한다.
오류 코드는 command-response 계약의 `/response/error/code`로만 읽는다.
movement는 ledgerSequence ASC로 읽는다. 수량 bound의 DB SUM은 관찰
SQL과 해당 allocations/obligations 원 행으로 추적하며 제품의
aggregate 응답을 복사하지 않는다.

경합 subcase의 barrier arming·ACK·독립 lock probe·lock 뒤 재검증은
[경합 관찰 계약](race-observation-contract.md)을 따른다. `transactions`와 `locks`
원행은 application 원장의 보조 증거이며 경합 증거는 `contender-waits`의
pg_catalog 관찰과 `lockRevalidations` trace다.

## split-commits-first

실물60/기존 약속40은 보존되고 신규 주문 ORDER2의 실행 배분은 20 이하다. 기존 배분은 한 번만 자식으로 이관한다. contender는 초기 read 뒤 멈추고, winner가 같은 scope lock을 잡은 채 멈춘 동안 재개돼 그 lock에서 실제로 기다린다. winner commit 뒤 contender는 lock 아래에서 revision을 다시 읽어 CONFLICT/STALE_REVISION이다. explicit fresh split은 별도 명령이다.

- `allocation-transfer-1` → `V2.split-reserve-race / allocation-transfer`: 공개 명령의 구조화 outcome을 확인한다.
- `allocation-transfer-2` → `V2.split-reserve-race / allocation-transfer`: 공개 명령의 구조화 outcome을 확인한다.
- `allocation-transfer-3` → `V2.split-reserve-race / allocation-transfer`: 검증 실패를 해당 오류 코드로 구별한다.
- `active-physical-4` → `V2.split-reserve-race / active-physical`: 실물량·단위와 독립 손계산을 대조한다.
- `active-physical-5` → `V2.split-reserve-race / active-physical`: 실물량·단위와 독립 손계산을 대조한다.
- `existing-obligation-6` → `V2.split-reserve-race / existing-obligation`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `new-demand-promise-20` → `V2.split-reserve-race / existing-obligation`: 신규 주문 ORDER2의 약속20은 기존 약속40과 별도 root로 보존된다.
- `new-executable-reservation-7` → `V2.split-reserve-race / new-executable-reservation`: 실물량·단위와 독립 손계산을 대조한다.
- `new-executable-reservation-8` → `V2.split-reserve-race / new-executable-reservation`: 독립 DB의 완전한 명령/행동 scope에서 해당 효과 원 행이0개다. 누락과 빈 결과를 혼동하지 않는다.
- `new-executable-reservation-api-exact` → `V2.split-reserve-race / new-executable-reservation`: API 신규 실행 예약은 DB 원 행과 같은 0이다.
- `all-executable-reservations-9` → `V2.split-reserve-race / all-executable-reservations`: 실물량·단위와 독립 손계산을 대조한다.
- `all-executable-reservations-10` → `V2.split-reserve-race / all-executable-reservations`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `all-executable-reservations-api-exact` → `V2.split-reserve-race / all-executable-reservations`: API 실행 예약 합계는 같은 snapshot의 DB EXECUTABLE 합계 40과 정확히 같다. 상한만으로는 이중 합산이나 유실을 잡지 못한다(AGENTS.md 명사/동사 동일 수량).
- `retired-parent-reconsumption-11` → `V2.split-reserve-race / retired-parent-reconsumption`: 분할로 retired된 부모 A60을 다시 예약하면 CONFLICT다. 소모된 물리 부모는 이미 바뀐 대상이다(contracts/domain-vocabulary.json STALE_REVISION=CONFLICT, 계획 §4.2 부모 재소비 금지).
- `retired-parent-reconsumption-code` → `V2.split-reserve-race / retired-parent-reconsumption`: 거부 이유는 retired 부모의 STALE_REVISION이다. 수량 부족 같은 다른 이유로 부모 재소비 금지를 대신하지 않는다.
- `retired-parent-reconsumption-12` → `V2.split-reserve-race / retired-parent-reconsumption`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `retired-parent-reconsumption-13` → `V2.split-reserve-race / retired-parent-reconsumption`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `retired-parent-reconsumption-14` → `V2.split-reserve-race / retired-parent-reconsumption`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `retired-parent-reconsumption-15` → `V2.split-reserve-race / retired-parent-reconsumption`: retired 부모 예약 시도의 CONFLICT 감사1과 금지된 업무 효과0을 분리한다.
- `allocation-transfer-16` → `V2.split-reserve-race / allocation-transfer`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `allocation-transfer-17` → `V2.split-reserve-race / allocation-transfer`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `allocation-transfer-18` → `V2.split-reserve-race / allocation-transfer`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `allocation-transfer-19` → `V2.split-reserve-race / allocation-transfer`: contender는 초기 read 뒤 scope lock 전에 실제로 멈췄다(barrier reached ACK).
- `allocation-transfer-23` → `V2.split-reserve-race / allocation-transfer`: 제출 ACK가 아닌 terminal await를 확인한다.
- `race-contender-initial-scope` → `V2.split-reserve-race / allocation-transfer`: contender의 barrier ACK는 일시정지 전에 실제로 읽은 초기 관점(대상·revision 또는 적격량)과 실제 PostgreSQL transaction 식별자를 artifact와 함께 보고한다. 초기 read 없이 멈춘 hook은 이 관점을 보일 수 없다(race-observation-contract.md).
- `race-contender-initial-revision` → `V2.split-reserve-race / allocation-transfer`: contender의 barrier ACK는 일시정지 전에 실제로 읽은 초기 관점(대상·revision 또는 적격량)과 실제 PostgreSQL transaction 식별자를 artifact와 함께 보고한다. 초기 read 없이 멈춘 hook은 이 관점을 보일 수 없다(race-observation-contract.md).
- `race-contender-waits-on-winner-lock` → `V2.split-reserve-race / allocation-transfer`: winner가 scope lock을 잡고 commit 전 멈춘 동안 재개된 contender는 같은 scope lock에서 실제로 기다린다. 독립 read-only connection이 pg_locks·pg_stat_activity·pg_blocking_pids로 이를 관찰한 뒤에만 winner를 놓는다. 순차 실행이나 hook 위치 오류는 이 WAIT를 만들 수 없다(계획 §4.2, V8 lock 계약).
- `race-both-transactions-open-at-wait` → `V2.split-reserve-race / allocation-transfer`: winner가 scope lock을 잡고 commit 전 멈춘 동안 재개된 contender는 같은 scope lock에서 실제로 기다린다. 독립 read-only connection이 pg_locks·pg_stat_activity·pg_blocking_pids로 이를 관찰한 뒤에만 winner를 놓는다. 순차 실행이나 hook 위치 오류는 이 WAIT를 만들 수 없다(계획 §4.2, V8 lock 계약). 이 시점 두 transaction은 모두 OPEN이므로 contender의 초기 read는 winner commit 전에 시작된 같은 transaction 안에 있다.
- `race-distinct-db-transactions` → `V2.split-reserve-race / allocation-transfer`: 두 참가자는 서로 다른 실제 DB transaction이다.
- `race-winner-reached-after-lock` → `V2.split-reserve-race / allocation-transfer`: winner는 scope lock 획득 뒤 commit 전 지점에 실제로 멈췄다.
- `race-contender-revalidated-after-lock` → `V2.split-reserve-race / allocation-transfer`: contender는 lock을 얻은 뒤 현재 상태를 다시 읽고 결정한다. 재검증 trace는 실제 application transaction의 SQL/CQN으로 기록하며 오류 응답에서 만들지 않는다(계획 §4.2 lock 뒤 재조회).
- `race-split-lock-reread-revision2` → `V2.split-reserve-race / allocation-transfer`: split 한 거래가 A60을 retired로 바꿔 revision1→2가 된다. contender는 lock 뒤 2를 읽고 요청 revision1과 비교해 STALE_REVISION을 낸다(V8 lock 계약과 같은 재검증).
- `genuine-two-transactions` → `V2.split-reserve-race / allocation-transfer`: 독립 DB transaction 원 행이 contender와 winner 각 하나씩이다. command ID로 거래 증거를 대신하지 않는다. application 원장의 거래 행은 보조 증거이며 경합 증거는 contender-waits의 pg_catalog 관찰이다.
- `genuine-distinct-transaction-identities` → `V2.split-reserve-race / allocation-transfer`: 서로 다른 실제 DB transaction ID가 중복되지 않는다. application 원장의 거래 행은 보조 증거이며 경합 증거는 contender-waits의 pg_catalog 관찰이다.
- `genuine-two-capabilities` → `V2.split-reserve-race / allocation-transfer`: 실제 거래는 분할과 예약 각각이며 mock lock이나 제출 ACK만으로 대신하지 않는다. application 원장의 거래 행은 보조 증거이며 경합 증거는 contender-waits의 pg_catalog 관찰이다.
- `active-physical-identities` → `V2.split-reserve-race / allocation-transfer`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `V2.split-reserve-race / allocation-transfer`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `V2.split-reserve-race / allocation-transfer`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## reserve-commits-first

실물60·기존 약속40·신규 약속20이 보존되고 신규 실행 배분은 20 이하다. reserve가 먼저 commit한 뒤 contender 분할의 결과는 계획이 고정하지 않는다. 예약이 A60 revision을 올리면 CONFLICT/STALE_REVISION·효과0, 올리지 않으면 두 배분을 한 번씩 자식으로 옮기는 APPLIED가 맞다. 어느 쪽이든 응답·감사·lock 뒤 재검증이 같은 결과를 말하고 실물·배분 불변식을 지켜야 한다. 이어서 현재 revision으로 수렴 분할을 시도해 두 경로의 최종 상태를 같게 만든다(계획 §4.2, §13.2 V2).

- `allocation-transfer-1` → `V2.split-reserve-race / allocation-transfer`: 공개 명령의 구조화 outcome을 확인한다.
- `active-physical-4` → `V2.split-reserve-race / active-physical`: 실물량·단위와 독립 손계산을 대조한다.
- `active-physical-5` → `V2.split-reserve-race / active-physical`: 실물량·단위와 독립 손계산을 대조한다.
- `existing-obligation-6` → `V2.split-reserve-race / existing-obligation`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `new-demand-promise-20` → `V2.split-reserve-race / existing-obligation`: 신규 주문 ORDER2의 약속20은 기존 약속40과 별도 root로 보존된다.
- `new-executable-reservation-7` → `V2.split-reserve-race / new-executable-reservation`: 실물량·단위와 독립 손계산을 대조한다.
- `new-executable-reservation-8` → `V2.split-reserve-race / new-executable-reservation`: 신규 예약20은 별도 주문 ORDER2의 현재 EXECUTABLE 배분 한 번이다. 명시 분할로 자식에 이관된 뒤 비활성 이력 행을 더하지 않는다(계획 §6 대체+원배분 비활성).
- `new-executable-reservation-api-exact` → `V2.split-reserve-race / new-executable-reservation`: API 신규 실행 예약은 DB 원 행과 같은 20이다.
- `all-executable-reservations-9` → `V2.split-reserve-race / all-executable-reservations`: 실물량·단위와 독립 손계산을 대조한다.
- `all-executable-reservations-10` → `V2.split-reserve-race / all-executable-reservations`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `all-executable-reservations-api-exact` → `V2.split-reserve-race / all-executable-reservations`: API 실행 예약 합계는 같은 snapshot의 DB EXECUTABLE 합계 60과 정확히 같다. 상한만으로는 이중 합산이나 유실을 잡지 못한다(AGENTS.md 명사/동사 동일 수량).
- `retired-parent-reconsumption-11` → `V2.split-reserve-race / retired-parent-reconsumption`: 분할로 retired된 부모 A60을 다시 예약하면 CONFLICT다. 소모된 물리 부모는 이미 바뀐 대상이다(contracts/domain-vocabulary.json STALE_REVISION=CONFLICT, 계획 §4.2 부모 재소비 금지).
- `retired-parent-reconsumption-code` → `V2.split-reserve-race / retired-parent-reconsumption`: 미충족 주문량10이 남은 ORDER3에 대한 1 BOX 예약이므로 주문량 부족으로 거부될 수 없다. 거부 이유는 retired 부모의 STALE_REVISION이어야 하며, retired 부모를 살아 있는 60으로 보는 제품은 APPLIED나 다른 코드로 드러난다.
- `retired-parent-reconsumption-12` → `V2.split-reserve-race / retired-parent-reconsumption`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `retired-parent-reconsumption-13` → `V2.split-reserve-race / retired-parent-reconsumption`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `retired-parent-reconsumption-14` → `V2.split-reserve-race / retired-parent-reconsumption`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `retired-parent-reconsumption-15` → `V2.split-reserve-race / retired-parent-reconsumption`: retired 부모 예약 시도의 CONFLICT 감사1과 금지된 업무 효과0을 분리한다.
- `allocation-transfer-16` → `V2.split-reserve-race / allocation-transfer`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `allocation-transfer-18` → `V2.split-reserve-race / allocation-transfer`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `allocation-transfer-19` → `V2.split-reserve-race / allocation-transfer`: contender는 초기 read 뒤 scope lock 전에 실제로 멈췄다(barrier reached ACK).
- `allocation-transfer-23` → `V2.split-reserve-race / allocation-transfer`: 제출 ACK가 아닌 terminal await를 확인한다.
- `race-contender-initial-scope` → `V2.split-reserve-race / allocation-transfer`: contender의 barrier ACK는 일시정지 전에 실제로 읽은 초기 관점(대상·revision 또는 적격량)과 실제 PostgreSQL transaction 식별자를 artifact와 함께 보고한다. 초기 read 없이 멈춘 hook은 이 관점을 보일 수 없다(race-observation-contract.md).
- `race-contender-initial-revision` → `V2.split-reserve-race / allocation-transfer`: contender의 barrier ACK는 일시정지 전에 실제로 읽은 초기 관점(대상·revision 또는 적격량)과 실제 PostgreSQL transaction 식별자를 artifact와 함께 보고한다. 초기 read 없이 멈춘 hook은 이 관점을 보일 수 없다(race-observation-contract.md).
- `race-contender-waits-on-winner-lock` → `V2.split-reserve-race / allocation-transfer`: winner가 scope lock을 잡고 commit 전 멈춘 동안 재개된 contender는 같은 scope lock에서 실제로 기다린다. 독립 read-only connection이 pg_locks·pg_stat_activity·pg_blocking_pids로 이를 관찰한 뒤에만 winner를 놓는다. 순차 실행이나 hook 위치 오류는 이 WAIT를 만들 수 없다(계획 §4.2, V8 lock 계약).
- `race-both-transactions-open-at-wait` → `V2.split-reserve-race / allocation-transfer`: winner가 scope lock을 잡고 commit 전 멈춘 동안 재개된 contender는 같은 scope lock에서 실제로 기다린다. 독립 read-only connection이 pg_locks·pg_stat_activity·pg_blocking_pids로 이를 관찰한 뒤에만 winner를 놓는다. 순차 실행이나 hook 위치 오류는 이 WAIT를 만들 수 없다(계획 §4.2, V8 lock 계약). 이 시점 두 transaction은 모두 OPEN이므로 contender의 초기 read는 winner commit 전에 시작된 같은 transaction 안에 있다.
- `race-distinct-db-transactions` → `V2.split-reserve-race / allocation-transfer`: 두 참가자는 서로 다른 실제 DB transaction이다.
- `race-winner-reached-after-lock` → `V2.split-reserve-race / allocation-transfer`: winner는 scope lock 획득 뒤 commit 전 지점에 실제로 멈췄다.
- `genuine-two-transactions` → `V2.split-reserve-race / allocation-transfer`: 독립 DB transaction 원 행이 contender와 winner 각 하나씩이다. command ID로 거래 증거를 대신하지 않는다. application 원장의 거래 행은 보조 증거이며 경합 증거는 contender-waits의 pg_catalog 관찰이다.
- `genuine-distinct-transaction-identities` → `V2.split-reserve-race / allocation-transfer`: 서로 다른 실제 DB transaction ID가 중복되지 않는다. application 원장의 거래 행은 보조 증거이며 경합 증거는 contender-waits의 pg_catalog 관찰이다.
- `genuine-two-capabilities` → `V2.split-reserve-race / allocation-transfer`: 실제 거래는 분할과 예약 각각이며 mock lock이나 제출 ACK만으로 대신하지 않는다. application 원장의 거래 행은 보조 증거이며 경합 증거는 contender-waits의 pg_catalog 관찰이다.
- `active-physical-identities` → `V2.split-reserve-race / allocation-transfer`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `V2.split-reserve-race / allocation-transfer`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `V2.split-reserve-race / allocation-transfer`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.
- `contender-outcome-not-rejected` → `V2.split-reserve-race / allocation-transfer`: contender 분할의 결과는 APPLIED 또는 CONFLICT 둘 중 하나다. command-response outcome enum에서 REJECTED를 제외한다. 두 결과 모두 아래 불변식과 기록 일치를 함께 만족해야 한다(계획 §4.2, §13.2 V2).
- `contender-outcome-not-waiting-approval` → `V2.split-reserve-race / allocation-transfer`: contender 분할의 결과는 APPLIED 또는 CONFLICT 둘 중 하나다. command-response outcome enum에서 WAITING_APPROVAL를 제외한다. 두 결과 모두 아래 불변식과 기록 일치를 함께 만족해야 한다(계획 §4.2, §13.2 V2).
- `contender-outcome-not-needs-input` → `V2.split-reserve-race / allocation-transfer`: contender 분할의 결과는 APPLIED 또는 CONFLICT 둘 중 하나다. command-response outcome enum에서 NEEDS_INPUT를 제외한다. 두 결과 모두 아래 불변식과 기록 일치를 함께 만족해야 한다(계획 §4.2, §13.2 V2).
- `contender-outcome-not-accepted-pending-external` → `V2.split-reserve-race / allocation-transfer`: contender 분할의 결과는 APPLIED 또는 CONFLICT 둘 중 하나다. command-response outcome enum에서 ACCEPTED_PENDING_EXTERNAL를 제외한다. 두 결과 모두 아래 불변식과 기록 일치를 함께 만족해야 한다(계획 §4.2, §13.2 V2).
- `contender-outcome-not-held` → `V2.split-reserve-race / allocation-transfer`: contender 분할의 결과는 APPLIED 또는 CONFLICT 둘 중 하나다. command-response outcome enum에서 HELD를 제외한다. 두 결과 모두 아래 불변식과 기록 일치를 함께 만족해야 한다(계획 §4.2, §13.2 V2).
- `contender-reported-equals-recorded` → `V2.split-reserve-race / allocation-transfer`: contender 응답 outcome은 같은 거래가 남긴 감사 원행의 outcome(raced-db derivation contenderOutcome)과 같다. 응답만 CONFLICT로 꾸미고 실제로 적용하거나 그 반대로 보고하면 실패한다.
- `contender-conflict-is-stale-revision` → `V2.split-reserve-race / allocation-transfer`: contender가 CONFLICT면 감사 오류 코드는 STALE_REVISION이고 lock 뒤 재검증도 STALE_REVISION이다. APPLIED면 두 쪽 모두 빈 집합이다. 재검증이 STALE인데 적용하면(낡은 의도를 몰래 실행) 또는 재검증이 최신인데 충돌로 거부하면 실패한다(계획 §4.2).
- `contender-conflict-iff-converge-split-applied` → `V2.split-reserve-race / allocation-transfer`: contender가 CONFLICT(효과0)일 때만 뒤의 수렴 분할이 APPLIED다. contender가 APPLIED면 A60은 이미 retired라 수렴 분할은 적용되지 않는다. CONFLICT로 기록하고 분할 효과를 남기면 수렴 분할이 막혀 실패한다.
- `raced-physical-60` → `V2.split-reserve-race / active-physical`: 경합 직후 active 실물 합은 두 결과 모두 60 BOX다(분할 전 A60 하나 또는 자식 40+20).
- `raced-allocations-exactly-once` → `V2.split-reserve-race / allocation-transfer`: 경합 직후 실행 배분은 기존 ALLOC 40 BOX와 신규 예약 20 BOX가 각각 정확히 한 번이다. contender가 적용됐다면 둘 다 자식으로 한 번씩 이관됐고, 충돌이면 A60에 그대로 있다. 유실·중복은 실패한다.
- `race-contender-revalidated-after-lock` → `V2.split-reserve-race / allocation-transfer`: contender는 lock을 얻은 뒤 A60을 다시 읽고 요청 revision1과 비교한다. 그 결과(STALE_REVISION 또는 최신)는 contender-conflict-is-stale-revision이 응답·감사와 묶는다. 재검증 trace는 실제 application transaction에서 만든다(계획 §4.2).
- `final-children-40-20` → `V2.split-reserve-race / active-physical`: 두 결과 모두 최종 active 실물은 분할 한 번의 자식 40 BOX와 20 BOX뿐이다. 분할이 두 번 적용되거나 부모가 남으면 실패한다.
- `final-parent-retired` → `V2.split-reserve-race / retired-parent-reconsumption`: 최종적으로 A60은 retired다.
- `final-no-allocation-on-retired-parent` → `V2.split-reserve-race / allocation-transfer`: retired A60을 가리키는 active 배분은0이다. 분할은 기존·신규 배분을 모두 자식으로 옮긴다.
- `final-allocations-exactly-once` → `V2.split-reserve-race / allocation-transfer`: 최종 실행 배분도 ALLOC 40 BOX와 신규 예약 20 BOX 각각 한 번이다(합 60 = 실물 60).
- `final-alloc40-on-child40` → `V2.split-reserve-race / allocation-transfer`: 기존 배분 ALLOC40은 40 BOX 자식 segment 위에만 있다. 20 BOX 자식에 40 배분을 얹는 이관은 실물 초과 예약이다(계획 §4.2 기존 배분 1회 이관·동일 실물 초과 금지).
- `final-winner20-on-child20` → `V2.split-reserve-race / allocation-transfer`: 경합 승자 예약20은 20 BOX 자식 segment 위에 있다. 40 자식은 ALLOC40으로 이미 가득 차므로 다른 배치는 초과 예약이다(계획 §4.2).

## actual50-correction

실물60→50, executable≤50이어도 약속60은 삭제하지 않는다. 부족10은 정확한 인간 owner와 현재 assignment1을 가진다.

- `active-physical-1` → `V2.actual50-correction / active-physical`: 실물량·단위와 독립 손계산을 대조한다.
- `active-physical-2` → `V2.actual50-correction / active-physical`: 실물량·단위와 독립 손계산을 대조한다.
- `executable-allocation-3` → `V2.actual50-correction / executable-allocation`: 실물량·단위와 독립 손계산을 대조한다.
- `executable-allocation-4` → `V2.actual50-correction / executable-allocation`: 독립 read-only DB SUM query는 원 행 scope의 실행 배분 상한50을 검증한다. product API projection을 복사하지 않는다.
- `promised-obligation-total-5` → `V2.actual50-correction / promised-obligation-total`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `minimum-shortage-duty-6` → `V2.actual50-correction / minimum-shortage-duty`: 실물량·단위와 독립 손계산을 대조한다.
- `minimum-shortage-duty-7` → `V2.actual50-correction / minimum-shortage-duty`: 독립 read-only DB SUM query는 원 행 scope의 부족 하한10을 검증한다. product API projection을 복사하지 않는다.
- `promise-coverage-conserved-60` → `V2.actual50-correction / minimum-shortage-duty`: 약속60은 정정 뒤 현재 EXECUTABLE 배분과 현재 부족 의무로 정확히 한 번씩 덮인다. promiseCoverage는 두 원천을 promiseRootId별로 합친 독립 read-only 관찰이다. 실행50+부족10, 전부 정지 시 실행0+부족60은 통과하고 실행50+부족60 이중 집계나 실행0+부족10 유실은 실패한다(계획 §4.2 부족 의무와 대체 배분).
- `promise-coverage-kinds` → `V2.actual50-correction / minimum-shortage-duty`: 약속60은 정정 뒤 현재 EXECUTABLE 배분과 현재 부족 의무로 정확히 한 번씩 덮인다. promiseCoverage는 두 원천을 promiseRootId별로 합친 독립 read-only 관찰이다. 실행50+부족10, 전부 정지 시 실행0+부족60은 통과하고 실행50+부족60 이중 집계나 실행0+부족10 유실은 실패한다(계획 §4.2 부족 의무와 대체 배분).
- `promise-coverage-shortage-is-correction-duty` → `V2.actual50-correction / shortage-duty`: 약속60은 정정 뒤 현재 EXECUTABLE 배분과 현재 부족 의무로 정확히 한 번씩 덮인다. promiseCoverage는 두 원천을 promiseRootId별로 합친 독립 read-only 관찰이다. 실행50+부족10, 전부 정지 시 실행0+부족60은 통과하고 실행50+부족60 이중 집계나 실행0+부족10 유실은 실패한다(계획 §4.2 부족 의무와 대체 배분). 부족 쪽 행은 정정 명령이 반환한 바로 그 부족 의무 하나다.
- `shortage-obligation-row-quantity` → `V2.actual50-correction / shortage-duty`: 정정 명령이 반환한 부족 의무의 obligations 원행 수량·단위를 직접 읽어 promiseCoverage의 부족 행과 같음을 확인한다. promiseCoverage는 같은 snapshot의 allocations·obligations 원행에서 observer가 만든 파생이며 제품 projection이 아니다(race-observation-contract.md). 실행 배분 합과 이 원행 수량의 합이 약속60이다.
- `shortage-duty-8` → `V2.actual50-correction / shortage-duty`: 해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `shortage-duty-9` → `V2.actual50-correction / shortage-duty`: 책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `shortage-duty-10` → `V2.actual50-correction / shortage-duty`: fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `past-reservation-deletion-to-hide-shortage-12` → `V2.actual50-correction / past-reservation-deletion-to-hide-shortage`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `past-reservation-deletion-to-hide-shortage-13` → `V2.actual50-correction / past-reservation-deletion-to-hide-shortage`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `active-physical-identities` → `V2.actual50-correction / active-physical`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `V2.actual50-correction / active-physical`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `V2.actual50-correction / active-physical`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.
