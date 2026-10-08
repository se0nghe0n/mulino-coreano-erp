# 실물 계보와 분할·합침의 보존

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

## split-merge

Q100=60+40, 합침100으로 돌아가도 retired 부모는 현재량에 더하지 않는다. 기존 예약40은 한 번 이관한다.

- `active-quantity-1` → `T03.split-merge-conservation / active-quantity`: 실물량·단위와 독립 손계산을 대조한다.
- `active-quantity-2` → `T03.split-merge-conservation / active-quantity`: 실물량·단위와 독립 손계산을 대조한다.
- `parent-3` → `T03.split-merge-conservation / parent`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `allocation-transferred-4` → `T03.split-merge-conservation / allocation-transferred`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `allocation-copies-5` → `T03.split-merge-conservation / allocation-copies`: 기존 배분40의 활성 root copy가 정확히 하나여야 한다.
- `atomic-genealogy-6` → `T03.split-merge-conservation / atomic-genealogy`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `atomic-genealogy-8` → `T03.split-merge-conservation / atomic-genealogy`: 분할의 계보·배분·감사·멱등 결과는 같은 실제 DB transaction에 속한다.
- `atomic-genealogy-9` → `T03.split-merge-conservation / atomic-genealogy`: 분할의 계보·배분·감사·멱등 결과는 같은 실제 DB transaction에 속한다.
- `atomic-genealogy-10` → `T03.split-merge-conservation / atomic-genealogy`: 분할의 계보·배분·감사·멱등 결과는 같은 실제 DB transaction에 속한다.
- `atomic-genealogy-11` → `T03.split-merge-conservation / atomic-genealogy`: 분할의 계보·배분·감사·멱등 결과는 같은 실제 DB transaction에 속한다.
- `active-quantity-12` → `T03.split-merge-conservation / active-quantity`: 실물량·단위와 독립 손계산을 대조한다.
- `active-quantity-13` → `T03.split-merge-conservation / active-quantity`: 실물량·단위와 독립 손계산을 대조한다.
- `allocation-transferred-14` → `T03.split-merge-conservation / allocation-transferred`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `allocation-copies-15` → `T03.split-merge-conservation / allocation-copies`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `atomic-genealogy-16` → `T03.split-merge-conservation / atomic-genealogy`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `atomic-genealogy-17` → `T03.split-merge-conservation / atomic-genealogy`: 두 분할 edge와 두 합침 edge의 원천 identity를 잃지 않는다.
- `atomic-second-genealogy-transaction` → `T03.split-merge-conservation / atomic-genealogy`: 분할의 계보·배분·감사·멱등 결과는 같은 실제 DB transaction에 속한다.
- `active-physical-identities` → `T03.split-merge-conservation / active-quantity`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T03.split-merge-conservation / active-quantity`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `complete-source-genealogy` → `T03.split-merge-conservation / atomic-genealogy`: 부모→자식과 자식→합침의 모든 원천 ID·quantity·unit을 정확한 관계 tuple로 보존한다.
- `retired-parent-physical-consumption` → `T03.split-merge-conservation / atomic-genealogy`: retired parent 원량은 물리 소비가 아닌 계보 분할이므로 재소비 원행은0이다.
- `actual-baseline-physical-rows` → `T03.split-merge-conservation / active-quantity`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.
- `atomic-genealogy-8-source-nonempty` → `T03.split-merge-conservation / atomic-genealogy`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.
- `atomic-genealogy-8-baseline-nonempty` → `T03.split-merge-conservation / atomic-genealogy`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.
- `atomic-genealogy-9-source-nonempty` → `T03.split-merge-conservation / atomic-genealogy`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.
- `atomic-genealogy-9-baseline-nonempty` → `T03.split-merge-conservation / atomic-genealogy`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.
- `atomic-genealogy-10-source-nonempty` → `T03.split-merge-conservation / atomic-genealogy`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.
- `atomic-genealogy-10-baseline-nonempty` → `T03.split-merge-conservation / atomic-genealogy`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.
- `atomic-genealogy-11-source-nonempty` → `T03.split-merge-conservation / atomic-genealogy`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.
- `atomic-genealogy-11-baseline-nonempty` → `T03.split-merge-conservation / atomic-genealogy`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.
- `atomic-second-genealogy-transaction-source-nonempty` → `T03.split-merge-conservation / atomic-genealogy`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.
- `atomic-second-genealogy-transaction-baseline-nonempty` → `T03.split-merge-conservation / atomic-genealogy`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.

## pallet-two-lots

물류 단위의 membership 시간과 각 LOT identity는 독립이며40+60=100은 단일 LOT 병합을 허가하지 않는다.

- `pallet-quantity-1` → `T03.logistics-multiple-lots / pallet-quantity`: 실물량·단위와 독립 손계산을 대조한다.
- `pallet-quantity-2` → `T03.logistics-multiple-lots / pallet-quantity`: 실물량·단위와 독립 손계산을 대조한다.
- `lot-membership-3` → `T03.logistics-multiple-lots / lot-membership`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `lot-membership-4` → `T03.logistics-multiple-lots / lot-membership`: 실제 팔레트 조회도 서로 다른 제조 LOT 두 개를 보존한다.
- `cross-lot-segment-merge-5` → `T03.logistics-multiple-lots / cross-lot-segment-merge`: 공개 명령의 구조화 outcome을 확인한다.
- `cross-lot-segment-merge-6` → `T03.logistics-multiple-lots / cross-lot-segment-merge`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `cross-lot-segment-merge-7` → `T03.logistics-multiple-lots / cross-lot-segment-merge`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `cross-lot-segment-merge-8` → `T03.logistics-multiple-lots / cross-lot-segment-merge`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `cross-lot-segment-merge-9` → `T03.logistics-multiple-lots / cross-lot-segment-merge`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `membership-time-10` → `T03.logistics-multiple-lots / membership-time`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `active-physical-identities` → `T03.logistics-multiple-lots / pallet-quantity`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T03.logistics-multiple-lots / pallet-quantity`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T03.logistics-multiple-lots / pallet-quantity`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## reject-cycle

순환·retired parent 재소비·장소/통제/단위 불일치는 실제 원천 행과 배분을 바꾸지 않는다.

- `invalid-results-1` → `T03.cycle-and-retired-parent / invalid-results`: 공개 명령의 구조화 outcome을 확인한다.
- `invalid-quantity-effects-2` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-quantity-effects-3` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-quantity-effects-4` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-allocation-effects-5` → `T03.cycle-and-retired-parent / invalid-allocation-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-results-6` → `T03.cycle-and-retired-parent / invalid-results`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `invalid-results-7` → `T03.cycle-and-retired-parent / invalid-results`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `invalid-results-8` → `T03.cycle-and-retired-parent / invalid-results`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `active-physical-identities` → `T03.cycle-and-retired-parent / invalid-results`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T03.cycle-and-retired-parent / invalid-results`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `cycle-specific-error` → `T03.cycle-and-retired-parent / invalid-results`: 응답의 구조화 오류 코드 /response/error/code가 GENEALOGY_CYCLE다(계획 §3.4, contracts/command-response.schema.json).
- `actual-baseline-physical-rows` → `T03.cycle-and-retired-parent / invalid-results`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## reject-consumeRetiredParent

순환·retired parent 재소비·장소/통제/단위 불일치는 실제 원천 행과 배분을 바꾸지 않는다.

- `invalid-results-1` → `T03.cycle-and-retired-parent / invalid-results`: 공개 명령의 구조화 outcome을 확인한다.
- `invalid-quantity-effects-2` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-quantity-effects-3` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-quantity-effects-4` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-allocation-effects-5` → `T03.cycle-and-retired-parent / invalid-allocation-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-results-6` → `T03.cycle-and-retired-parent / invalid-results`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `invalid-results-7` → `T03.cycle-and-retired-parent / invalid-results`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `invalid-results-8` → `T03.cycle-and-retired-parent / invalid-results`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `active-physical-identities` → `T03.cycle-and-retired-parent / invalid-results`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T03.cycle-and-retired-parent / invalid-results`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T03.cycle-and-retired-parent / invalid-results`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## reject-mergeDifferentPlace

순환·retired parent 재소비·장소/통제/단위 불일치는 실제 원천 행과 배분을 바꾸지 않는다.

- `invalid-results-1` → `T03.cycle-and-retired-parent / invalid-results`: 공개 명령의 구조화 outcome을 확인한다.
- `invalid-quantity-effects-2` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-quantity-effects-3` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-quantity-effects-4` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-allocation-effects-5` → `T03.cycle-and-retired-parent / invalid-allocation-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-results-6` → `T03.cycle-and-retired-parent / invalid-results`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `invalid-results-7` → `T03.cycle-and-retired-parent / invalid-results`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `invalid-results-8` → `T03.cycle-and-retired-parent / invalid-results`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `active-physical-identities` → `T03.cycle-and-retired-parent / invalid-results`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T03.cycle-and-retired-parent / invalid-results`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T03.cycle-and-retired-parent / invalid-results`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## reject-mergeDifferentControl

순환·retired parent 재소비·장소/통제/단위 불일치는 실제 원천 행과 배분을 바꾸지 않는다.

- `invalid-results-1` → `T03.cycle-and-retired-parent / invalid-results`: 공개 명령의 구조화 outcome을 확인한다.
- `invalid-quantity-effects-2` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-quantity-effects-3` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-quantity-effects-4` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-allocation-effects-5` → `T03.cycle-and-retired-parent / invalid-allocation-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-results-6` → `T03.cycle-and-retired-parent / invalid-results`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `invalid-results-7` → `T03.cycle-and-retired-parent / invalid-results`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `invalid-results-8` → `T03.cycle-and-retired-parent / invalid-results`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `active-physical-identities` → `T03.cycle-and-retired-parent / invalid-results`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T03.cycle-and-retired-parent / invalid-results`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T03.cycle-and-retired-parent / invalid-results`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## reject-mergeIncompatibleUnit

순환·retired parent 재소비·장소/통제/단위 불일치는 실제 원천 행과 배분을 바꾸지 않는다.

- `invalid-results-1` → `T03.cycle-and-retired-parent / invalid-results`: 공개 명령의 구조화 outcome을 확인한다.
- `invalid-quantity-effects-2` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-quantity-effects-3` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-quantity-effects-4` → `T03.cycle-and-retired-parent / invalid-quantity-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-allocation-effects-5` → `T03.cycle-and-retired-parent / invalid-allocation-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `invalid-results-6` → `T03.cycle-and-retired-parent / invalid-results`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `invalid-results-7` → `T03.cycle-and-retired-parent / invalid-results`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `invalid-results-8` → `T03.cycle-and-retired-parent / invalid-results`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `active-physical-identities` → `T03.cycle-and-retired-parent / invalid-results`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T03.cycle-and-retired-parent / invalid-results`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T03.cycle-and-retired-parent / invalid-results`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## indistinguishable-mixture

원천 영향40과 현재 후보100을 구분한다. 구별 근거 없는60 해제는 효과0이다.

- `source-affected-1` → `T03.indistinguishable-mixture / source-affected`: 실제 trace의 문제 원천40과 관찰된 기준 단위BOX를 decimal primary로 직접 대조한다.
- `source-affected-2` → `T03.indistinguishable-mixture / source-affected`: 원천40의 기준 단위를 보존한다.
- `current-candidate-scope-3` → `T03.indistinguishable-mixture / current-candidate-scope`: 실물량·단위와 독립 손계산을 대조한다.
- `current-candidate-scope-4` → `T03.indistinguishable-mixture / current-candidate-scope`: 실물량·단위와 독립 손계산을 대조한다.
- `current-candidate-scope-5` → `T03.indistinguishable-mixture / current-candidate-scope`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `arbitrary-clean-selection-6` → `T03.indistinguishable-mixture / arbitrary-clean-selection`: 분리 근거 없는 식별 불가능 혼합 물량의 임의 깨끗한 선택 해제는 효과 없이 HELD로 보류된다(D03 식별 불가능 혼합 범위 보류).
- `arbitrary-clean-selection-7` → `T03.indistinguishable-mixture / arbitrary-clean-selection`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `arbitrary-clean-selection-8` → `T03.indistinguishable-mixture / arbitrary-clean-selection`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `arbitrary-clean-selection-9` → `T03.indistinguishable-mixture / arbitrary-clean-selection`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `arbitrary-clean-selection-10` → `T03.indistinguishable-mixture / arbitrary-clean-selection`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `trace-certainty-11` → `T03.indistinguishable-mixture / trace-certainty`: 계보의 영향 후보와 오염 확정을 구분한다.
- `trace-certainty-12` → `T03.indistinguishable-mixture / trace-certainty`: 보류 이유는 정확한 분리 범위의 검증 증거가 없다는 EVIDENCE_UNVERIFIED다(contracts/domain-vocabulary.json EVIDENCE_UNVERIFIED=HELD, 계획 §4.2).
- `trace-certainty-13` → `T03.indistinguishable-mixture / trace-certainty`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `active-physical-identities` → `T03.indistinguishable-mixture / source-affected`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T03.indistinguishable-mixture / source-affected`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `mixed-source-quantities` → `T03.indistinguishable-mixture / source-affected`: 문제 원천40과 나머지60의 계보 근거를 원 행에서 대조한다. 이 관계만으로 실제 clean subset을 선택하지 않는다.
- `actual-baseline-physical-rows` → `T03.indistinguishable-mixture / source-affected`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.


## 실행 증거

공통 실행 기록은 [inventory 최종 증거](../T03/evidence/inventory-final/README.md)에 있다.
이 case의 각 subcase RED 원본은 `evidence/contract-red-<case>-<subcase>.json`에 보존했다.

수량 primary 수정의 최신 [실행 증거](../T03/evidence/inventory-primary-fix/README.md)를 보존했다.
