# decimal과 수량 거래의 불변식

실제 제품/API/DB/host/model은 NOT_RUN이다. 이 파일은 구현 전
검증 계약이며 synthetic 정책·문서를 실제 규제값으로 사용하지 않는다.
fixture는 식별된 시작 사실만 설치한다. 검증할 효과·승인은 반드시
명시 행동으로 실행한다. 조회/거부 감사·REJECTED 명령 결과·책임 알림을
허용하면서 COMMITTED 결과와 BUSINESS outbox 등 금지 효과를 분리한다.

rawRows는 sourceEvidence의 actual SQL·parameters·mapping·artifact를
가진 동일 scope/snapshot의 원 행이다. fixture alias와 신규 result ID는
strict 참조로 연결하고 수량·unit·상태·시간은 독립 고정 oracle로 둔다.
책임 count는 실제 obligationId와 current assignment scope로 한정한다.
transactions는 transactionCommandKeys로 지정된 두 경합 명령만 읽는다.
조회·fixture 설치·명시적 fresh split 거래를 경합 거래 수로 더하지 않는다.
lockProbeScopeOnly는 fixture가 지정한 segment/allocation/fence 세 scope의
원행을 immutable scopeId ASC로 읽는다. acquisitionOrdinal1,2,3이
그 순서에 일치해야 한다. movement는 ledgerSequence ASC로 읽는다.
수량 bound의 DB SUM은 관찰 SQL과 해당 allocations/obligations 원 행으로
추적하며 제품의 aggregate 응답을 복사하지 않는다.

## indivisible-ea

numeric(38,12)의 정밀도/scale와 EA 불가분 정책을 거부 outcome·원 입력·영속 효과0으로 검증한다.

- `invalid-decimals-1` → `T04.decimal-boundary / invalid-decimals`: 공개 명령의 구조화 outcome을 확인한다.
- `invalid-decimals-2` → `T04.decimal-boundary / invalid-decimals`: 검증 실패를 해당 오류 코드로 구별한다.
- `rounded-effects-3` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-4` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-5` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-6` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-7` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-8` → `T04.decimal-boundary / rounded-effects`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `wire-decimals-9` → `T04.decimal-boundary / wire-decimals`: 유효하지 않은 원 decimal 문자열을 그대로 반환하며 binary float/반올림으로 바꾸지 않는다.
- `wire-decimals-10` → `T04.decimal-boundary / wire-decimals`: 원 입력의 실제 단위를 그대로 대조한다.
- `wire-decimals-11` → `T04.decimal-boundary / wire-decimals`: 금액 역시 정확한 decimal 문자열을 사용한다.
- `wire-decimals-12` → `T04.decimal-boundary / wire-decimals`: 금액과 원 통화를 함께 보존한다.
- `active-physical-identities` → `T04.decimal-boundary / invalid-decimals`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T04.decimal-boundary / invalid-decimals`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T04.decimal-boundary / invalid-decimals`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## scale-overflow

numeric(38,12)의 정밀도/scale와 EA 불가분 정책을 거부 outcome·원 입력·영속 효과0으로 검증한다.

- `invalid-decimals-1` → `T04.decimal-boundary / invalid-decimals`: 공개 명령의 구조화 outcome을 확인한다.
- `invalid-decimals-2` → `T04.decimal-boundary / invalid-decimals`: 검증 실패를 해당 오류 코드로 구별한다.
- `rounded-effects-3` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-4` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-5` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-6` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-7` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-8` → `T04.decimal-boundary / rounded-effects`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `wire-decimals-9` → `T04.decimal-boundary / wire-decimals`: 유효하지 않은 원 decimal 문자열을 그대로 반환하며 binary float/반올림으로 바꾸지 않는다.
- `wire-decimals-10` → `T04.decimal-boundary / wire-decimals`: 원 입력의 실제 단위를 그대로 대조한다.
- `wire-decimals-11` → `T04.decimal-boundary / wire-decimals`: 금액 역시 정확한 decimal 문자열을 사용한다.
- `wire-decimals-12` → `T04.decimal-boundary / wire-decimals`: 금액과 원 통화를 함께 보존한다.
- `active-physical-identities` → `T04.decimal-boundary / invalid-decimals`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T04.decimal-boundary / invalid-decimals`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T04.decimal-boundary / invalid-decimals`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## precision-overflow

numeric(38,12)의 정밀도/scale와 EA 불가분 정책을 거부 outcome·원 입력·영속 효과0으로 검증한다.

- `invalid-decimals-1` → `T04.decimal-boundary / invalid-decimals`: 공개 명령의 구조화 outcome을 확인한다.
- `invalid-decimals-2` → `T04.decimal-boundary / invalid-decimals`: 검증 실패를 해당 오류 코드로 구별한다.
- `rounded-effects-3` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-4` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-5` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-6` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-7` → `T04.decimal-boundary / rounded-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rounded-effects-8` → `T04.decimal-boundary / rounded-effects`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `wire-decimals-9` → `T04.decimal-boundary / wire-decimals`: 유효하지 않은 원 decimal 문자열을 그대로 반환하며 binary float/반올림으로 바꾸지 않는다.
- `wire-decimals-10` → `T04.decimal-boundary / wire-decimals`: 원 입력의 실제 단위를 그대로 대조한다.
- `wire-decimals-11` → `T04.decimal-boundary / wire-decimals`: 금액 역시 정확한 decimal 문자열을 사용한다.
- `wire-decimals-12` → `T04.decimal-boundary / wire-decimals`: 금액과 원 통화를 함께 보존한다.
- `active-physical-identities` → `T04.decimal-boundary / invalid-decimals`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T04.decimal-boundary / invalid-decimals`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T04.decimal-boundary / invalid-decimals`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## hold-dispose

보류의 위치/실물 효과는0이며 실제 폐기 movement10만 원장량을 줄인다. 기본 MANAGER와 QC 결정은 별개다.

- `held-after-hold-1` → `T04.hold-versus-disposal / held-after-hold`: 실물량·단위와 독립 손계산을 대조한다.
- `held-after-hold-2` → `T04.hold-versus-disposal / held-after-hold`: 실물량·단위와 독립 손계산을 대조한다.
- `eligible-after-hold-3` → `T04.hold-versus-disposal / eligible-after-hold`: 실물량·단위와 독립 손계산을 대조한다.
- `eligible-after-hold-4` → `T04.hold-versus-disposal / eligible-after-hold`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `decrease-evidence-5` → `T04.hold-versus-disposal / decrease-evidence`: 공개 명령의 구조화 outcome을 확인한다.
- `decrease-evidence-6` → `T04.hold-versus-disposal / decrease-evidence`: 검증 실패를 해당 오류 코드로 구별한다.
- `decrease-evidence-7` → `T04.hold-versus-disposal / decrease-evidence`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `decrease-evidence-8` → `T04.hold-versus-disposal / decrease-evidence`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `decrease-evidence-9` → `T04.hold-versus-disposal / decrease-evidence`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `held-after-disposal-10` → `T04.hold-versus-disposal / held-after-disposal`: 실물량·단위와 독립 손계산을 대조한다.
- `held-after-disposal-11` → `T04.hold-versus-disposal / held-after-disposal`: 실물량·단위와 독립 손계산을 대조한다.
- `physical-disposal-12` → `T04.hold-versus-disposal / physical-disposal`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `decrease-evidence-13` → `T04.hold-versus-disposal / decrease-evidence`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `decrease-evidence-14` → `T04.hold-versus-disposal / decrease-evidence`: 공개 명령의 구조화 outcome을 확인한다.
- `active-physical-identities` → `T04.hold-versus-disposal / held-after-hold`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T04.hold-versus-disposal / held-after-hold`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `hold-physical-delta` → `T04.hold-versus-disposal / held-after-hold`: 독립 손계산의 실물 delta 0 BOX를 전후 실제 단위와 함께 대조한다.
- `disposal-physical-delta` → `T04.hold-versus-disposal / held-after-disposal`: 독립 손계산의 실물 delta -10 BOX를 전후 실제 단위와 함께 대조한다.
- `actual-baseline-physical-rows` → `T04.hold-versus-disposal / held-after-hold`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## rollback-afterMovementBeforeAllocation

DB 수량·배분·성공 감사·outbox·COMMITTED 멱등 결과는 분리 commit할 수 없다.

- `rollback-committed-result-1` → `T04.atomic-ledger / rollback-committed-result`: 공개 명령의 구조화 outcome을 확인한다.
- `rollback-committed-result-2` → `T04.atomic-ledger / rollback-committed-result`: 검증 실패를 해당 오류 코드로 구별한다.
- `rollback-ledger-effects-3` → `T04.atomic-ledger / rollback-ledger-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-ledger-effects-4` → `T04.atomic-ledger / rollback-ledger-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-ledger-effects-5` → `T04.atomic-ledger / rollback-ledger-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-allocation-effects-6` → `T04.atomic-ledger / rollback-allocation-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-outbox-effects-7` → `T04.atomic-ledger / rollback-outbox-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-committed-result-8` → `T04.atomic-ledger / rollback-committed-result`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-ledger-effects-9` → `T04.atomic-ledger / rollback-ledger-effects`: 도메인 성공 감사도 rollback이며 별도 실패 접수 audit만 허용한다.
- `active-physical-identities` → `T04.atomic-ledger / rollback-committed-result`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T04.atomic-ledger / rollback-committed-result`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T04.atomic-ledger / rollback-committed-result`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## rollback-afterAllocationBeforeAudit

DB 수량·배분·성공 감사·outbox·COMMITTED 멱등 결과는 분리 commit할 수 없다.

- `rollback-committed-result-1` → `T04.atomic-ledger / rollback-committed-result`: 공개 명령의 구조화 outcome을 확인한다.
- `rollback-committed-result-2` → `T04.atomic-ledger / rollback-committed-result`: 검증 실패를 해당 오류 코드로 구별한다.
- `rollback-ledger-effects-3` → `T04.atomic-ledger / rollback-ledger-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-ledger-effects-4` → `T04.atomic-ledger / rollback-ledger-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-ledger-effects-5` → `T04.atomic-ledger / rollback-ledger-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-allocation-effects-6` → `T04.atomic-ledger / rollback-allocation-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-outbox-effects-7` → `T04.atomic-ledger / rollback-outbox-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-committed-result-8` → `T04.atomic-ledger / rollback-committed-result`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-ledger-effects-9` → `T04.atomic-ledger / rollback-ledger-effects`: 도메인 성공 감사도 rollback이며 별도 실패 접수 audit만 허용한다.
- `active-physical-identities` → `T04.atomic-ledger / rollback-committed-result`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T04.atomic-ledger / rollback-committed-result`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T04.atomic-ledger / rollback-committed-result`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## rollback-afterAuditBeforeOutbox

DB 수량·배분·성공 감사·outbox·COMMITTED 멱등 결과는 분리 commit할 수 없다.

- `rollback-committed-result-1` → `T04.atomic-ledger / rollback-committed-result`: 공개 명령의 구조화 outcome을 확인한다.
- `rollback-committed-result-2` → `T04.atomic-ledger / rollback-committed-result`: 검증 실패를 해당 오류 코드로 구별한다.
- `rollback-ledger-effects-3` → `T04.atomic-ledger / rollback-ledger-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-ledger-effects-4` → `T04.atomic-ledger / rollback-ledger-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-ledger-effects-5` → `T04.atomic-ledger / rollback-ledger-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-allocation-effects-6` → `T04.atomic-ledger / rollback-allocation-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-outbox-effects-7` → `T04.atomic-ledger / rollback-outbox-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-committed-result-8` → `T04.atomic-ledger / rollback-committed-result`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-ledger-effects-9` → `T04.atomic-ledger / rollback-ledger-effects`: 도메인 성공 감사도 rollback이며 별도 실패 접수 audit만 허용한다.
- `active-physical-identities` → `T04.atomic-ledger / rollback-committed-result`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T04.atomic-ledger / rollback-committed-result`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T04.atomic-ledger / rollback-committed-result`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## rollback-afterOutboxBeforeCommittedResult

DB 수량·배분·성공 감사·outbox·COMMITTED 멱등 결과는 분리 commit할 수 없다.

- `rollback-committed-result-1` → `T04.atomic-ledger / rollback-committed-result`: 공개 명령의 구조화 outcome을 확인한다.
- `rollback-committed-result-2` → `T04.atomic-ledger / rollback-committed-result`: 검증 실패를 해당 오류 코드로 구별한다.
- `rollback-ledger-effects-3` → `T04.atomic-ledger / rollback-ledger-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-ledger-effects-4` → `T04.atomic-ledger / rollback-ledger-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-ledger-effects-5` → `T04.atomic-ledger / rollback-ledger-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-allocation-effects-6` → `T04.atomic-ledger / rollback-allocation-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-outbox-effects-7` → `T04.atomic-ledger / rollback-outbox-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-committed-result-8` → `T04.atomic-ledger / rollback-committed-result`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `rollback-ledger-effects-9` → `T04.atomic-ledger / rollback-ledger-effects`: 도메인 성공 감사도 rollback이며 별도 실패 접수 audit만 허용한다.
- `active-physical-identities` → `T04.atomic-ledger / rollback-committed-result`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T04.atomic-ledger / rollback-committed-result`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T04.atomic-ledger / rollback-committed-result`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## primitive-guards

raw DB와 application 경계의 거부·수량 불변·bounded retry 및 실제 ordered lock trace/recheck를 각각 확인한다.

- `guarded-primitives-1` → `T04.atomic-ledger / guarded-primitives`: 공개 명령의 구조화 outcome을 확인한다.
- `guarded-primitives-2` → `T04.atomic-ledger / guarded-primitives`: 검증 실패를 해당 오류 코드로 구별한다.
- `guarded-primitives-3` → `T04.atomic-ledger / guarded-primitives`: 공개 명령의 구조화 outcome을 확인한다.
- `guarded-primitives-4` → `T04.atomic-ledger / guarded-primitives`: 검증 실패를 해당 오류 코드로 구별한다.
- `guarded-primitives-5` → `T04.atomic-ledger / guarded-primitives`: 공개 명령의 구조화 outcome을 확인한다.
- `guarded-primitives-6` → `T04.atomic-ledger / guarded-primitives`: 검증 실패를 해당 오류 코드로 구별한다.
- `guarded-primitives-7` → `T04.atomic-ledger / guarded-primitives`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `guarded-primitives-8` → `T04.atomic-ledger / guarded-primitives`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `guarded-primitives-9` → `T04.atomic-ledger / guarded-primitives`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `guarded-primitives-10` → `T04.atomic-ledger / guarded-primitives`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `guarded-primitives-11` → `T04.atomic-ledger / guarded-primitives`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `guarded-primitives-12` → `T04.atomic-ledger / guarded-primitives`: 독립 DB role privilege 조회에서 core UPDATE 허용 행이 없다.
- `guarded-primitives-13` → `T04.atomic-ledger / guarded-primitives`: 충돌 후 payload/expected revision을 몰래 최신 의도로 바꾸지 않는다. bounded retry 한계3 안에서 종료한다.
- `guarded-primitives-14` → `T04.atomic-ledger / guarded-primitives`: 같은 scope의 실제 잠금 획득은 고정 ID 순서다.
- `guarded-primitives-16` → `T04.atomic-ledger / guarded-primitives`: lock 획득 후 실제 revision reread로 commit guard를 확인한다.
- `active-physical-identities` → `T04.atomic-ledger / guarded-primitives`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T04.atomic-ledger / guarded-primitives`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T04.atomic-ledger / guarded-primitives`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.
- `guarded-primitives-16-source-nonempty` → `T04.atomic-ledger / guarded-primitives`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.
- `guarded-primitives-16-baseline-nonempty` → `T04.atomic-ledger / guarded-primitives`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.


## 실행 증거

공통 실행 기록은 [inventory 최종 증거](../T03/evidence/inventory-final/README.md)에 있다.
이 case의 각 subcase RED 원본은 `evidence/contract-red-<case>-<subcase>.json`에 보존했다.
