# QC 제한과 출고의 직렬화

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
[경합 관찰 계약](../V2/race-observation-contract.md)을 따른다. `transactions`와 `locks`
원행은 application 원장의 보조 증거이며 경합 증거는 `contender-waits`의
pg_catalog 관찰과 `lockRevalidations` trace다.

## hold-first

출고 contender가 보류 전 적격20을 읽고 멈춘 뒤, QC 보류 winner가 scope fence를 잡은 채 멈춘 동안 재개돼 그 fence에서 실제로 기다린다. 보류 commit 뒤 출고는 fence 아래에서 제한을 다시 읽어 효과0으로 거부되고 배분은 SUSPENDED, 책임은 남는다.

- `scope-lock-1` → `V3.hold-before-dispatch / scope-lock`: 공개 명령의 구조화 outcome을 확인한다.
- `new-dispatched-2` → `V3.hold-before-dispatch / new-dispatched`: 공개 명령의 구조화 outcome을 확인한다.
- `new-dispatched-3` → `V3.hold-before-dispatch / new-dispatched`: 검증 실패를 해당 오류 코드로 구별한다.
- `new-dispatched-4` → `V3.hold-before-dispatch / new-dispatched`: 실물량·단위와 독립 손계산을 대조한다.
- `new-dispatched-5` → `V3.hold-before-dispatch / new-dispatched`: contender 출고의 효과는0이다. winner의 QC 보류가 배분을 정지시키는 것은 허용 효과이므로 전체 행 동일성 대신 출고 명령 범위의 이동·BUSINESS outbox 0건과 실물 위치·수량 불변을 본다.
- `new-dispatched-6` → `V3.hold-before-dispatch / new-dispatched`: contender 출고의 효과는0이다. winner의 QC 보류가 배분을 정지시키는 것은 허용 효과이므로 전체 행 동일성 대신 출고 명령 범위의 이동·BUSINESS outbox 0건과 실물 위치·수량 불변을 본다. QC 보류는 이동을 만들지 않으므로 이동 원행 전체가 경합 전과 같다.
- `new-dispatched-7` → `V3.hold-before-dispatch / new-dispatched`: contender 출고의 효과는0이다. winner의 QC 보류가 배분을 정지시키는 것은 허용 효과이므로 전체 행 동일성 대신 출고 명령 범위의 이동·BUSINESS outbox 0건과 실물 위치·수량 불변을 본다.
- `new-dispatched-8` → `V3.hold-before-dispatch / new-dispatched`: contender 출고의 효과는0이다. winner의 QC 보류가 배분을 정지시키는 것은 허용 효과이므로 전체 행 동일성 대신 출고 명령 범위의 이동·BUSINESS outbox 0건과 실물 위치·수량 불변을 본다.
- `allocation-9` → `V3.hold-before-dispatch / allocation`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `blocked-dispatch-duty-10` → `V3.hold-before-dispatch / blocked-dispatch-duty`: 해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `blocked-dispatch-duty-11` → `V3.hold-before-dispatch / blocked-dispatch-duty`: 책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `blocked-dispatch-duty-12` → `V3.hold-before-dispatch / blocked-dispatch-duty`: fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `scope-lock-13` → `V3.hold-before-dispatch / scope-lock`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `scope-lock-14` → `V3.hold-before-dispatch / scope-lock`: 제한 삽입과 출고는 같은 실제 scope fence에 참여하고 현재 제한을 재읽는다.
- `scope-lock-16` → `V3.hold-before-dispatch / scope-lock`: 비동기 요청은 실제 terminal await가 있어야 끝난다.
- `race-contender-initial-scope` → `V3.hold-before-dispatch / scope-lock`: contender의 barrier ACK는 일시정지 전에 실제로 읽은 초기 관점(대상·revision 또는 적격량)과 실제 PostgreSQL transaction 식별자를 artifact와 함께 보고한다. 초기 read 없이 멈춘 hook은 이 관점을 보일 수 없다(race-observation-contract.md).
- `race-contender-initial-eligible20` → `V3.hold-before-dispatch / scope-lock`: contender의 barrier ACK는 일시정지 전에 실제로 읽은 초기 관점(대상·revision 또는 적격량)과 실제 PostgreSQL transaction 식별자를 artifact와 함께 보고한다. 초기 read 없이 멈춘 hook은 이 관점을 보일 수 없다(race-observation-contract.md).
- `race-contender-waits-on-winner-lock` → `V3.hold-before-dispatch / scope-lock`: winner가 scope lock을 잡고 commit 전 멈춘 동안 재개된 contender는 같은 scope lock에서 실제로 기다린다. 독립 read-only connection이 pg_locks·pg_stat_activity·pg_blocking_pids로 이를 관찰한 뒤에만 winner를 놓는다. 순차 실행이나 hook 위치 오류는 이 WAIT를 만들 수 없다(계획 §4.2, V8 lock 계약).
- `race-both-transactions-open-at-wait` → `V3.hold-before-dispatch / scope-lock`: winner가 scope lock을 잡고 commit 전 멈춘 동안 재개된 contender는 같은 scope lock에서 실제로 기다린다. 독립 read-only connection이 pg_locks·pg_stat_activity·pg_blocking_pids로 이를 관찰한 뒤에만 winner를 놓는다. 순차 실행이나 hook 위치 오류는 이 WAIT를 만들 수 없다(계획 §4.2, V8 lock 계약). 이 시점 두 transaction은 모두 OPEN이므로 contender의 초기 read는 winner commit 전에 시작된 같은 transaction 안에 있다.
- `race-distinct-db-transactions` → `V3.hold-before-dispatch / scope-lock`: 두 참가자는 서로 다른 실제 DB transaction이다.
- `race-winner-reached-after-lock` → `V3.hold-before-dispatch / scope-lock`: winner는 scope lock 획득 뒤 commit 전 지점에 실제로 멈췄다.
- `race-contender-revalidated-after-lock` → `V3.hold-before-dispatch / scope-lock`: contender는 lock을 얻은 뒤 현재 상태를 다시 읽고 결정한다. 재검증 trace는 실제 application transaction의 SQL/CQN으로 기록하며 오류 응답에서 만들지 않는다(계획 §4.2 lock 뒤 재조회).
- `active-physical-identities` → `V3.hold-before-dispatch / scope-lock`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `V3.hold-before-dispatch / scope-lock`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `V3.hold-before-dispatch / scope-lock`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.
- `scope-lock-14-source-nonempty` → `V3.hold-before-dispatch / scope-lock`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.
- `scope-lock-14-baseline-nonempty` → `V3.hold-before-dispatch / scope-lock`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.

## dispatch-first

QC 보류 contender가 출고 전 revision1을 읽고 멈춘 뒤, 출고 winner가 fence를 잡은 채 멈춘 동안 재개돼 기다린다. 출고 commit 뒤 옛 revision의 보류는 CONFLICT/STALE_REVISION이다. 실제20 이력과 CONSUMED는 보존되고, 현재 revision으로 새로 낸 QC 보류가 이동한 실물의 보류와 후속 책임을 남긴다.

- `committed-dispatch-preserved-1` → `V3.dispatch-before-hold / committed-dispatch-preserved`: 공개 명령의 구조화 outcome을 확인한다.
- `committed-dispatch-preserved-2` → `V3.dispatch-before-hold / committed-dispatch-preserved`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `allocation-3` → `V3.dispatch-before-hold / allocation`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `post-dispatch-hold-response-4` → `V3.dispatch-before-hold / post-dispatch-hold-response`: 해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다. 후속 책임은 출고 commit 뒤의 현재 revision으로 새로 낸 QC 보류가 남긴다.
- `post-dispatch-hold-response-5` → `V3.dispatch-before-hold / post-dispatch-hold-response`: 책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다. 후속 책임은 출고 commit 뒤의 현재 revision으로 새로 낸 QC 보류가 남긴다.
- `post-dispatch-hold-response-6` → `V3.dispatch-before-hold / post-dispatch-hold-response`: fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다. 후속 책임은 출고 commit 뒤의 현재 revision으로 새로 낸 QC 보류가 남긴다.
- `history-deletion-or-fake-rollback-7` → `V3.dispatch-before-hold / history-deletion-or-fake-rollback`: 출고 commit으로 생긴 이동 이력은 뒤의 QC 보류로 삭제되거나 가짜 rollback되지 않는다.
- `post-dispatch-hold-response-8` → `V3.dispatch-before-hold / post-dispatch-hold-response`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다. 후속 책임은 출고 commit 뒤의 현재 revision으로 새로 낸 QC 보류가 남긴다.
- `history-deletion-or-fake-rollback-9` → `V3.dispatch-before-hold / history-deletion-or-fake-rollback`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `post-dispatch-hold-response-11` → `V3.dispatch-before-hold / post-dispatch-hold-response`: 비동기 요청은 실제 terminal await가 있어야 끝난다.
- `race-contender-initial-scope` → `V3.dispatch-before-hold / post-dispatch-hold-response`: contender의 barrier ACK는 일시정지 전에 실제로 읽은 초기 관점(대상·revision 또는 적격량)과 실제 PostgreSQL transaction 식별자를 artifact와 함께 보고한다. 초기 read 없이 멈춘 hook은 이 관점을 보일 수 없다(race-observation-contract.md).
- `race-contender-initial-revision` → `V3.dispatch-before-hold / post-dispatch-hold-response`: contender의 barrier ACK는 일시정지 전에 실제로 읽은 초기 관점(대상·revision 또는 적격량)과 실제 PostgreSQL transaction 식별자를 artifact와 함께 보고한다. 초기 read 없이 멈춘 hook은 이 관점을 보일 수 없다(race-observation-contract.md).
- `race-contender-waits-on-winner-lock` → `V3.dispatch-before-hold / post-dispatch-hold-response`: winner가 scope lock을 잡고 commit 전 멈춘 동안 재개된 contender는 같은 scope lock에서 실제로 기다린다. 독립 read-only connection이 pg_locks·pg_stat_activity·pg_blocking_pids로 이를 관찰한 뒤에만 winner를 놓는다. 순차 실행이나 hook 위치 오류는 이 WAIT를 만들 수 없다(계획 §4.2, V8 lock 계약).
- `race-both-transactions-open-at-wait` → `V3.dispatch-before-hold / post-dispatch-hold-response`: winner가 scope lock을 잡고 commit 전 멈춘 동안 재개된 contender는 같은 scope lock에서 실제로 기다린다. 독립 read-only connection이 pg_locks·pg_stat_activity·pg_blocking_pids로 이를 관찰한 뒤에만 winner를 놓는다. 순차 실행이나 hook 위치 오류는 이 WAIT를 만들 수 없다(계획 §4.2, V8 lock 계약). 이 시점 두 transaction은 모두 OPEN이므로 contender의 초기 read는 winner commit 전에 시작된 같은 transaction 안에 있다.
- `race-distinct-db-transactions` → `V3.dispatch-before-hold / post-dispatch-hold-response`: 두 참가자는 서로 다른 실제 DB transaction이다.
- `race-winner-reached-after-lock` → `V3.dispatch-before-hold / post-dispatch-hold-response`: winner는 scope lock 획득 뒤 commit 전 지점에 실제로 멈췄다.
- `race-contender-revalidated-after-lock` → `V3.dispatch-before-hold / post-dispatch-hold-response`: contender는 lock을 얻은 뒤 현재 상태를 다시 읽고 결정한다. 재검증 trace는 실제 application transaction의 SQL/CQN으로 기록하며 오류 응답에서 만들지 않는다(계획 §4.2 lock 뒤 재조회).
- `stale-hold-conflict` → `V3.dispatch-before-hold / post-dispatch-hold-response`: 출고가 먼저 commit하면 segment revision이 바뀐다. 출고 전 revision1을 들고 기다린 QC 보류는 fence 뒤 재검사에서 CONFLICT/STALE_REVISION이며 효과0이다. 옛 revision의 보류를 APPLIED로 받는 구현은 revision 계약을 우회한다(계획 §3.4·§4.2, s3-quality 계약 creation expectedRevision=segment revision, V2와 같은 의미).
- `stale-hold-reason` → `V3.dispatch-before-hold / post-dispatch-hold-response`: 출고가 먼저 commit하면 segment revision이 바뀐다. 출고 전 revision1을 들고 기다린 QC 보류는 fence 뒤 재검사에서 CONFLICT/STALE_REVISION이며 효과0이다. 옛 revision의 보류를 APPLIED로 받는 구현은 revision 계약을 우회한다(계획 §3.4·§4.2, s3-quality 계약 creation expectedRevision=segment revision, V2와 같은 의미).
- `stale-hold-no-restriction` → `V3.dispatch-before-hold / post-dispatch-hold-response`: 출고가 먼저 commit하면 segment revision이 바뀐다. 출고 전 revision1을 들고 기다린 QC 보류는 fence 뒤 재검사에서 CONFLICT/STALE_REVISION이며 효과0이다. 옛 revision의 보류를 APPLIED로 받는 구현은 revision 계약을 우회한다(계획 §3.4·§4.2, s3-quality 계약 creation expectedRevision=segment revision, V2와 같은 의미). fixture에는 제한이 없으므로 경합 직후 제한 원행은0이다.
- `fresh-hold-applied` → `V3.dispatch-before-hold / post-dispatch-hold-response`: 출고 뒤 현재 revision으로 낸 새 QC 보류는 APPLIED이고 이동한 실물의 보류·후속 책임을 남긴다.
- `active-physical-identities` → `V3.dispatch-before-hold / committed-dispatch-preserved`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `V3.dispatch-before-hold / committed-dispatch-preserved`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `V3.dispatch-before-hold / committed-dispatch-preserved`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## late-v1-release

v1 release는 OLD_HOLD만 참조한다. 현재 v2 hold와 출고0을 보존하며 원천 선후 미확인은 대조 assignment1로 남긴다.

- `new-hold-1` → `V3.late-old-qc-release / new-hold`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `newly-dispatched-2` → `V3.late-old-qc-release / newly-dispatched`: 실물량·단위와 독립 손계산을 대조한다.
- `newly-dispatched-3` → `V3.late-old-qc-release / newly-dispatched`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `late-release-overwrites-new-hold-4` → `V3.late-old-qc-release / late-release-overwrites-new-hold`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `newly-dispatched-5` → `V3.late-old-qc-release / newly-dispatched`: 공개 명령의 구조화 outcome을 확인한다.
- `newly-dispatched-6` → `V3.late-old-qc-release / newly-dispatched`: 검증 실패를 해당 오류 코드로 구별한다.
- `source-order-reconciliation-7` → `V3.late-old-qc-release / source-order-reconciliation`: 해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `source-order-reconciliation-8` → `V3.late-old-qc-release / source-order-reconciliation`: 책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `source-order-reconciliation-9` → `V3.late-old-qc-release / source-order-reconciliation`: fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `source-order-reconciliation-10` → `V3.late-old-qc-release / source-order-reconciliation`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `active-physical-identities` → `V3.late-old-qc-release / new-hold`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `V3.late-old-qc-release / new-hold`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `V3.late-old-qc-release / new-hold`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.
