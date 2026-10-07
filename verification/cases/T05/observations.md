# 위치·보관·소유·처분 허용의 독립성

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

## custody-not-sale

위탁40+고객 보관60=보유100, confirmed SELL40, 예약0/미예약40. 인계·QC해제·앱WRITE가 고객 소유60의 SELL 근거를 만들지 않는다.

- `held-1` → `T05.ownership-custody-disposition / held`: 실물량·단위와 독립 손계산을 대조한다.
- `held-2` → `T05.ownership-custody-disposition / held`: 실물량·단위와 독립 손계산을 대조한다.
- `sell-eligible-3` → `T05.ownership-custody-disposition / sell-eligible`: 실물량·단위와 독립 손계산을 대조한다.
- `sell-eligible-4` → `T05.ownership-custody-disposition / sell-eligible`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `unreserved-eligible-5` → `T05.ownership-custody-disposition / unreserved-eligible`: 실물량·단위와 독립 손계산을 대조한다.
- `unreserved-eligible-6` → `T05.ownership-custody-disposition / unreserved-eligible`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `relations-independent-7` → `T05.ownership-custody-disposition / relations-independent`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `relations-independent-8` → `T05.ownership-custody-disposition / relations-independent`: 공개 명령의 구조화 outcome을 확인한다.
- `relations-independent-9` → `T05.ownership-custody-disposition / relations-independent`: 검증 실패를 해당 오류 코드로 구별한다.
- `relations-independent-10` → `T05.ownership-custody-disposition / relations-independent`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `relations-independent-11` → `T05.ownership-custody-disposition / relations-independent`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `relations-independent-12` → `T05.ownership-custody-disposition / relations-independent`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `relations-independent-13` → `T05.ownership-custody-disposition / relations-independent`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `relations-independent-14` → `T05.ownership-custody-disposition / relations-independent`: SELL 부적격이어도 반송 허용은 별도 행동의 처분 근거로 판단한다.
- `active-physical-identities` → `T05.ownership-custody-disposition / held`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T05.ownership-custody-disposition / held`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T05.ownership-custody-disposition / held`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## planned-not-actual

계획 운송은 실물/수령 사건을 생성하지 않는다.

- `current-location-1` → `T05.planned-location / current-location`: 실제 마지막 확인 위치는 이탈리아 항구다.
- `current-location-2` → `T05.planned-location / current-location`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `planned-destination-3` → `T05.planned-location / planned-destination`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `planned-destination-4` → `T05.planned-location / planned-destination`: 계획 목적지를 현재 장소와 따로 저장한다.
- `planned-receipt-effects-5` → `T05.planned-location / planned-receipt-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `planned-receipt-effects-6` → `T05.planned-location / planned-receipt-effects`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `planned-receipt-effects-7` → `T05.planned-location / planned-receipt-effects`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `active-physical-identities` → `T05.planned-location / current-location`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T05.planned-location / current-location`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T05.planned-location / current-location`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## manager-disposition

문서/제안은 보존하되 일반WRITE 확인효과0, MANAGER 확인1 뒤SELL40. reserve/dispatch10에 새 승인 추가0이며 독립 QC는 계속 적용한다.

- `ordinary-write-confirmed-basis-effects-1` → `T05.disposition-manager-decision / ordinary-write-confirmed-basis-effects`: 공개 명령의 구조화 outcome을 확인한다.
- `ordinary-write-confirmed-basis-effects-2` → `T05.disposition-manager-decision / ordinary-write-confirmed-basis-effects`: 검증 실패를 해당 오류 코드로 구별한다.
- `ordinary-write-confirmed-basis-effects-3` → `T05.disposition-manager-decision / ordinary-write-confirmed-basis-effects`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `ordinary-write-confirmed-basis-effects-4` → `T05.disposition-manager-decision / ordinary-write-confirmed-basis-effects`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `eligible-before-manager-confirmation-5` → `T05.disposition-manager-decision / eligible-before-manager-confirmation`: 실물량·단위와 독립 손계산을 대조한다.
- `eligible-before-manager-confirmation-6` → `T05.disposition-manager-decision / eligible-before-manager-confirmation`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `proposal-evidence-preserved-7` → `T05.disposition-manager-decision / proposal-evidence-preserved`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `proposal-evidence-preserved-8` → `T05.disposition-manager-decision / proposal-evidence-preserved`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `proposal-evidence-preserved-9` → `T05.disposition-manager-decision / proposal-evidence-preserved`: 공개 명령의 구조화 outcome을 확인한다.
- `authorized-confirmation-count-10` → `T05.disposition-manager-decision / authorized-confirmation-count`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `eligible-after-manager-confirmation-11` → `T05.disposition-manager-decision / eligible-after-manager-confirmation`: 실물량·단위와 독립 손계산을 대조한다.
- `eligible-after-manager-confirmation-12` → `T05.disposition-manager-decision / eligible-after-manager-confirmation`: 실물량·단위와 독립 손계산을 대조한다.
- `manager-decision-binding-13` → `T05.disposition-manager-decision / manager-decision-binding`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `ordinary-authorized-reservation-after-confirmation-14` → `T05.disposition-manager-decision / ordinary-authorized-reservation-after-confirmation`: 실물량·단위와 독립 손계산을 대조한다.
- `ordinary-authorized-reservation-after-confirmation-15` → `T05.disposition-manager-decision / ordinary-authorized-reservation-after-confirmation`: 실물량·단위와 독립 손계산을 대조한다.
- `ordinary-authorized-dispatch-after-confirmation-16` → `T05.disposition-manager-decision / ordinary-authorized-dispatch-after-confirmation`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `ordinary-authorized-dispatch-after-confirmation-17` → `T05.disposition-manager-decision / ordinary-authorized-dispatch-after-confirmation`: 공개 명령의 구조화 outcome을 확인한다.
- `new-human-approval-added-to-reservation-or-dispatch-18` → `T05.disposition-manager-decision / new-human-approval-added-to-reservation-or-dispatch`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `new-human-approval-added-to-reservation-or-dispatch-19` → `T05.disposition-manager-decision / new-human-approval-added-to-reservation-or-dispatch`: 처분 확인 후 일반 reserve는 새 인간 승인 없이 실행한다.
- `manager-decision-binding-20` → `T05.disposition-manager-decision / manager-decision-binding`: 실물량·단위와 독립 손계산을 대조한다.
- `manager-decision-binding-21` → `T05.disposition-manager-decision / manager-decision-binding`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `manager-decision-binding-22` → `T05.disposition-manager-decision / manager-decision-binding`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `active-physical-identities` → `T05.disposition-manager-decision / ordinary-write-confirmed-basis-effects`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T05.disposition-manager-decision / ordinary-write-confirmed-basis-effects`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T05.disposition-manager-decision / ordinary-write-confirmed-basis-effects`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.


## 실행 증거

공통 실행 기록은 [inventory 최종 증거](../T03/evidence/inventory-final/README.md)에 있다.
이 case의 각 subcase RED 원본은 `evidence/contract-red-<case>-<subcase>.json`에 보존했다.
