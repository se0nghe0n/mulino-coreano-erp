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
transactions는 transactionCommandKeys로 지정된 두 경합 명령만 읽는다.
조회·fixture 설치·명시적 fresh split 거래를 경합 거래 수로 더하지 않는다.
lockProbeScopeOnly는 fixture가 지정한 segment/allocation/fence 세 scope의
원행을 immutable scopeId ASC로 읽는다. acquisitionOrdinal1,2,3이
그 순서에 일치해야 한다. movement는 ledgerSequence ASC로 읽는다.
수량 bound의 DB SUM은 관찰 SQL과 해당 allocations/obligations 원 행으로
추적하며 제품의 aggregate 응답을 복사하지 않는다.

## split-commits-first

실물60/기존 약속40은 보존된다. 신규20 경합은20 이하이며 기존 배분은 한 번만 자식으로 이관한다. stale contender는 revision conflict이고 explicit fresh split은 별도 명령이다.

- `allocation-transfer-1` → `V2.split-reserve-race / allocation-transfer`: 공개 명령의 구조화 outcome을 확인한다.
- `allocation-transfer-2` → `V2.split-reserve-race / allocation-transfer`: 공개 명령의 구조화 outcome을 확인한다.
- `allocation-transfer-3` → `V2.split-reserve-race / allocation-transfer`: 검증 실패를 해당 오류 코드로 구별한다.
- `active-physical-4` → `V2.split-reserve-race / active-physical`: 실물량·단위와 독립 손계산을 대조한다.
- `active-physical-5` → `V2.split-reserve-race / active-physical`: 실물량·단위와 독립 손계산을 대조한다.
- `existing-obligation-6` → `V2.split-reserve-race / existing-obligation`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `new-executable-reservation-7` → `V2.split-reserve-race / new-executable-reservation`: 실물량·단위와 독립 손계산을 대조한다.
- `new-executable-reservation-8` → `V2.split-reserve-race / new-executable-reservation`: 독립 DB의 완전한 명령/행동 scope에서 해당 효과 원 행이0개다. 누락과 빈 결과를 혼동하지 않는다.
- `all-executable-reservations-9` → `V2.split-reserve-race / all-executable-reservations`: 실물량·단위와 독립 손계산을 대조한다.
- `all-executable-reservations-10` → `V2.split-reserve-race / all-executable-reservations`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `retired-parent-reconsumption-11` → `V2.split-reserve-race / retired-parent-reconsumption`: 공개 명령의 구조화 outcome을 확인한다.
- `retired-parent-reconsumption-12` → `V2.split-reserve-race / retired-parent-reconsumption`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `retired-parent-reconsumption-13` → `V2.split-reserve-race / retired-parent-reconsumption`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `retired-parent-reconsumption-14` → `V2.split-reserve-race / retired-parent-reconsumption`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `retired-parent-reconsumption-15` → `V2.split-reserve-race / retired-parent-reconsumption`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `allocation-transfer-16` → `V2.split-reserve-race / allocation-transfer`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `allocation-transfer-17` → `V2.split-reserve-race / allocation-transfer`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `allocation-transfer-18` → `V2.split-reserve-race / allocation-transfer`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `allocation-transfer-19` → `V2.split-reserve-race / allocation-transfer`: 실제 첫 read 뒤 두 번째 거래를 commit하기 전 barrier reached ACK다.
- `allocation-transfer-22` → `V2.split-reserve-race / allocation-transfer`: barrier와 terminal await는 같은 contender transaction을 관찰한다.
- `allocation-transfer-23` → `V2.split-reserve-race / allocation-transfer`: 제출 ACK가 아닌 terminal await를 확인한다.
- `genuine-two-transactions` → `V2.split-reserve-race / allocation-transfer`: 독립 DB transaction 원 행이 contender와 winner 각 하나씩이다. command ID로 거래 증거를 대신하지 않는다.
- `genuine-distinct-transaction-identities` → `V2.split-reserve-race / allocation-transfer`: 서로 다른 실제 DB transaction ID가 중복되지 않는다.
- `genuine-two-capabilities` → `V2.split-reserve-race / allocation-transfer`: 실제 거래는 분할과 예약 각각이며 mock lock이나 제출 ACK만으로 대신하지 않는다.
- `active-physical-identities` → `V2.split-reserve-race / allocation-transfer`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `V2.split-reserve-race / allocation-transfer`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `V2.split-reserve-race / allocation-transfer`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## reserve-commits-first

실물60/기존 약속40은 보존된다. 신규20 경합은20 이하이며 기존 배분은 한 번만 자식으로 이관한다. stale contender는 revision conflict이고 explicit fresh split은 별도 명령이다.

- `allocation-transfer-1` → `V2.split-reserve-race / allocation-transfer`: 공개 명령의 구조화 outcome을 확인한다.
- `allocation-transfer-2` → `V2.split-reserve-race / allocation-transfer`: 공개 명령의 구조화 outcome을 확인한다.
- `allocation-transfer-3` → `V2.split-reserve-race / allocation-transfer`: 검증 실패를 해당 오류 코드로 구별한다.
- `active-physical-4` → `V2.split-reserve-race / active-physical`: 실물량·단위와 독립 손계산을 대조한다.
- `active-physical-5` → `V2.split-reserve-race / active-physical`: 실물량·단위와 독립 손계산을 대조한다.
- `existing-obligation-6` → `V2.split-reserve-race / existing-obligation`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `new-executable-reservation-7` → `V2.split-reserve-race / new-executable-reservation`: 실물량·단위와 독립 손계산을 대조한다.
- `new-executable-reservation-8` → `V2.split-reserve-race / new-executable-reservation`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `all-executable-reservations-9` → `V2.split-reserve-race / all-executable-reservations`: 실물량·단위와 독립 손계산을 대조한다.
- `all-executable-reservations-10` → `V2.split-reserve-race / all-executable-reservations`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `retired-parent-reconsumption-11` → `V2.split-reserve-race / retired-parent-reconsumption`: 공개 명령의 구조화 outcome을 확인한다.
- `retired-parent-reconsumption-12` → `V2.split-reserve-race / retired-parent-reconsumption`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `retired-parent-reconsumption-13` → `V2.split-reserve-race / retired-parent-reconsumption`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `retired-parent-reconsumption-14` → `V2.split-reserve-race / retired-parent-reconsumption`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `retired-parent-reconsumption-15` → `V2.split-reserve-race / retired-parent-reconsumption`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `allocation-transfer-16` → `V2.split-reserve-race / allocation-transfer`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `allocation-transfer-17` → `V2.split-reserve-race / allocation-transfer`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `allocation-transfer-18` → `V2.split-reserve-race / allocation-transfer`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `allocation-transfer-19` → `V2.split-reserve-race / allocation-transfer`: 실제 첫 read 뒤 두 번째 거래를 commit하기 전 barrier reached ACK다.
- `allocation-transfer-22` → `V2.split-reserve-race / allocation-transfer`: barrier와 terminal await는 같은 contender transaction을 관찰한다.
- `allocation-transfer-23` → `V2.split-reserve-race / allocation-transfer`: 제출 ACK가 아닌 terminal await를 확인한다.
- `genuine-two-transactions` → `V2.split-reserve-race / allocation-transfer`: 독립 DB transaction 원 행이 contender와 winner 각 하나씩이다. command ID로 거래 증거를 대신하지 않는다.
- `genuine-distinct-transaction-identities` → `V2.split-reserve-race / allocation-transfer`: 서로 다른 실제 DB transaction ID가 중복되지 않는다.
- `genuine-two-capabilities` → `V2.split-reserve-race / allocation-transfer`: 실제 거래는 분할과 예약 각각이며 mock lock이나 제출 ACK만으로 대신하지 않는다.
- `active-physical-identities` → `V2.split-reserve-race / allocation-transfer`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `V2.split-reserve-race / allocation-transfer`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `V2.split-reserve-race / allocation-transfer`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## actual50-correction

실물60→50, executable≤50이어도 약속60은 삭제하지 않는다. 부족10은 정확한 인간 owner와 현재 assignment1을 가진다.

- `active-physical-1` → `V2.actual50-correction / active-physical`: 실물량·단위와 독립 손계산을 대조한다.
- `active-physical-2` → `V2.actual50-correction / active-physical`: 실물량·단위와 독립 손계산을 대조한다.
- `executable-allocation-3` → `V2.actual50-correction / executable-allocation`: 실물량·단위와 독립 손계산을 대조한다.
- `executable-allocation-4` → `V2.actual50-correction / executable-allocation`: 독립 read-only DB SUM query는 원 행 scope의 실행 배분 상한50을 검증한다. product API projection을 복사하지 않는다.
- `promised-obligation-total-5` → `V2.actual50-correction / promised-obligation-total`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `minimum-shortage-duty-6` → `V2.actual50-correction / minimum-shortage-duty`: 실물량·단위와 독립 손계산을 대조한다.
- `minimum-shortage-duty-7` → `V2.actual50-correction / minimum-shortage-duty`: 독립 read-only DB SUM query는 원 행 scope의 부족 하한10을 검증한다. product API projection을 복사하지 않는다.
- `shortage-duty-8` → `V2.actual50-correction / shortage-duty`: 해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `shortage-duty-9` → `V2.actual50-correction / shortage-duty`: 책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `shortage-duty-10` → `V2.actual50-correction / shortage-duty`: fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `past-reservation-deletion-to-hide-shortage-12` → `V2.actual50-correction / past-reservation-deletion-to-hide-shortage`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `past-reservation-deletion-to-hide-shortage-13` → `V2.actual50-correction / past-reservation-deletion-to-hide-shortage`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `active-physical-identities` → `V2.actual50-correction / active-physical`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `V2.actual50-correction / active-physical`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `V2.actual50-correction / active-physical`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.


## 실행 증거

공통 실행 기록은 [inventory 최종 증거](../T03/evidence/inventory-final/README.md)에 있다.
이 case의 각 subcase RED 원본은 `evidence/contract-red-<case>-<subcase>.json`에 보존했다.
