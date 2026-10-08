# 수령·QC·실사·무이벤트 만료

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

## provisional-holds

운송 실물60은 확인 수령에서 이동하며 재생성되지 않는다. QC20만 해제해도 회수60은ACTIVE라 판매0이다.
`confirmed-eligible-control`(step2r round 6)은 확인 수령 직후·보류 전 W의 60이 판매 적격 60 BOX임을 본다. 그래서 임시 접수의 0과 QC만 해제한 뒤의 0은 장소 종류가 아니라 임시 상태와 남은 RECALL60 때문이다.

- `provisional-eligible-1` → `T16.provisional-and-independent-holds / provisional-eligible`: 실물량·단위와 독립 손계산을 대조한다.
- `provisional-eligible-2` → `T16.provisional-and-independent-holds / provisional-eligible`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `receipt-transit-double-creation-3` → `T16.provisional-and-independent-holds / receipt-transit-double-creation`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `receipt-transit-double-creation-4` → `T16.provisional-and-independent-holds / receipt-transit-double-creation`: 실물량·단위와 독립 손계산을 대조한다.
- `receipt-transit-double-creation-5` → `T16.provisional-and-independent-holds / receipt-transit-double-creation`: 실물량·단위와 독립 손계산을 대조한다.
- `receipt-transit-double-creation-6` → `T16.provisional-and-independent-holds / receipt-transit-double-creation`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `receipt-transit-double-creation-7` → `T16.provisional-and-independent-holds / receipt-transit-double-creation`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `hold-authority-8` → `T16.provisional-and-independent-holds / hold-authority`: 공개 명령의 구조화 outcome을 확인한다.
- `hold-authority-9` → `T16.provisional-and-independent-holds / hold-authority`: 응답의 구조화 오류 코드 /response/error/code가 FORBIDDEN다(계획 §3.4, contracts/command-response.schema.json).
- `hold-authority-10` → `T16.provisional-and-independent-holds / hold-authority`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `hold-authority-11` → `T16.provisional-and-independent-holds / hold-authority`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `hold-authority-12` → `T16.provisional-and-independent-holds / hold-authority`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `eligible-after-QC-only-release-13` → `T16.provisional-and-independent-holds / eligible-after-QC-only-release`: 실물량·단위와 독립 손계산을 대조한다.
- `remaining-recall-14` → `T16.provisional-and-independent-holds / remaining-recall`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `hold-authority-15` → `T16.provisional-and-independent-holds / hold-authority`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `active-physical-identities` → `T16.provisional-and-independent-holds / provisional-eligible`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T16.provisional-and-independent-holds / provisional-eligible`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `ordinary-proposal-recorded` → `T16.provisional-and-independent-holds / hold-authority`: 일반 scoped RECORD는 QC 제안과 근거를 보존할 수 있다.
- `ordinary-cannot-place-qc-hold` → `T16.provisional-and-independent-holds / hold-authority`: 일반 Agent는 QC 제한의 실제 결정을 할 수 없다.
- `ordinary-qc-proposal-evidence` → `T16.provisional-and-independent-holds / hold-authority`: QC 제안은 문서/사건으로 남지만 QC 결정으로 승격하지 않는다.
- `actual-baseline-physical-rows` → `T16.provisional-and-independent-holds / provisional-eligible`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## stocktake-adjust-move

실사98은 보유100을 즉시 덮어쓰지 않는다. 승인 감소2 뒤98=20+78이며 내부 위치 변화로 새 재고가 생기지 않는다.

- `held-before-adjustment-1` → `T16.movement-stocktake-adjustment / held-before-adjustment`: 실물량·단위와 독립 손계산을 대조한다.
- `held-before-adjustment-2` → `T16.movement-stocktake-adjustment / held-before-adjustment`: 실물량·단위와 독립 손계산을 대조한다.
- `adjustment-proof-3` → `T16.movement-stocktake-adjustment / adjustment-proof`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `adjustment-proof-4` → `T16.movement-stocktake-adjustment / adjustment-proof`: 공개 명령의 구조화 outcome을 확인한다.
- `adjustment-proof-5` → `T16.movement-stocktake-adjustment / adjustment-proof`: 응답의 구조화 오류 코드 /response/error/code가 FORBIDDEN다(계획 §3.4, contracts/command-response.schema.json).
- `adjustment-proof-6` → `T16.movement-stocktake-adjustment / adjustment-proof`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `adjustment-proof-7` → `T16.movement-stocktake-adjustment / adjustment-proof`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `adjustment-proof-8` → `T16.movement-stocktake-adjustment / adjustment-proof`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `held-after-adjustment-9` → `T16.movement-stocktake-adjustment / held-after-adjustment`: 실물량·단위와 독립 손계산을 대조한다.
- `held-after-adjustment-10` → `T16.movement-stocktake-adjustment / held-after-adjustment`: 실물량·단위와 독립 손계산을 대조한다.
- `adjustment-proof-11` → `T16.movement-stocktake-adjustment / adjustment-proof`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `distinct-total-after-internal-move-12` → `T16.movement-stocktake-adjustment / distinct-total-after-internal-move`: 실물량·단위와 독립 손계산을 대조한다.
- `distinct-total-after-internal-move-13` → `T16.movement-stocktake-adjustment / distinct-total-after-internal-move`: 실물량·단위와 독립 손계산을 대조한다.
- `adjustment-proof-14` → `T16.movement-stocktake-adjustment / adjustment-proof`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `adjustment-proof-16` → `T16.movement-stocktake-adjustment / adjustment-proof`: 부분20 이동 전에 분할 movement가 원장 순서에 먼저 존재한다.
- `active-physical-identities` → `T16.movement-stocktake-adjustment / held-before-adjustment`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T16.movement-stocktake-adjustment / held-before-adjustment`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T16.movement-stocktake-adjustment / held-before-adjustment`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## expiry-sweeper

예약20은 T까지 허용된다. queue 비어도 T 이후 sweeper가 정지/의무를 upsert하며 sweep 지연 중 출고 guard도 실제 현재조건을 재검증한다.

- `pick-applied` → `T16.no-event-expiry / boundary-recheck`: 만료 전 예약20을 warehouse가 pick한다. 그래서 뒤 출고가 거부되는 이유는 만료뿐이다. pick이 없으면 만료 처리가 없는 제품도 pick 누락(FulfillmentCommands "Pick before dispatch required")으로 출고를 거부해 이 subcase를 통과한다.
- `dispatch-after-sweep-rejected` → `T16.no-event-expiry / boundary-recheck`: sweeper가 만료 경계에서 예약을 SUSPENDED로 바꿨으므로 pick된 예약의 출고도 거부되고 이유는 INSUFFICIENT_ELIGIBLE_QUANTITY다(FulfillmentCommands "Suspended allocation cannot execute"). pick 누락의 TYPE_INVALID가 아니다.
- `dispatch-after-sweep-code` → `T16.no-event-expiry / boundary-recheck`: sweeper가 만료 경계에서 예약을 SUSPENDED로 바꿨으므로 pick된 예약의 출고도 거부되고 이유는 INSUFFICIENT_ELIGIBLE_QUANTITY다(FulfillmentCommands "Suspended allocation cannot execute"). pick 누락의 TYPE_INVALID가 아니다.
- `allocation-after-boundary-1` → `T16.no-event-expiry / allocation-after-boundary`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `post-expiry-dispatched-2` → `T16.no-event-expiry / post-expiry-dispatched`: 실물량·단위와 독립 손계산을 대조한다.
- `post-expiry-dispatched-3` → `T16.no-event-expiry / post-expiry-dispatched`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `expiry-duty-4` → `T16.no-event-expiry / expiry-duty`: 해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `expiry-duty-5` → `T16.no-event-expiry / expiry-duty`: 책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `expiry-duty-6` → `T16.no-event-expiry / expiry-duty`: fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `boundary-recheck-7` → `T16.no-event-expiry / boundary-recheck`: 무이벤트 경계 이후30초 이내 실제 autonomous sweep task의 terminal을 관찰한다.
- `boundary-recheck-8` → `T16.no-event-expiry / boundary-recheck`: 이벤트 없는 만료 전에 예약의 nextValidityBoundary T를 실제 index 원 행으로 등록한다.
- `boundary-recheck-9` → `T16.no-event-expiry / boundary-recheck`: sweep의 배분 정지와 의무 upsert는 같은 실제 transaction이다.
- `boundary-recheck-10` → `T16.no-event-expiry / boundary-recheck`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `active-physical-identities` → `T16.no-event-expiry / allocation-after-boundary`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T16.no-event-expiry / allocation-after-boundary`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T16.no-event-expiry / allocation-after-boundary`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.
- `actual-expiry-obligation-scope` → `T16.no-event-expiry / expiry-duty`: 현재 allocation20의 만료 의무를 동일 DB snapshot에서 정확히 하나 조회한다.

## expiry-delayed-sweep

예약20은 T까지 허용된다. queue 비어도 T 이후 sweeper가 정지/의무를 upsert하며 sweep 지연 중 출고 guard도 실제 현재조건을 재검증한다.

- `pick-applied` → `T16.no-event-expiry / boundary-recheck`: 만료 전 예약20을 warehouse가 pick한다. 그래서 뒤 출고가 거부되는 이유는 만료뿐이다. pick이 없으면 만료 처리가 없는 제품도 pick 누락(FulfillmentCommands "Pick before dispatch required")으로 출고를 거부해 이 subcase를 통과한다.
- `boundary-recheck-1` → `T16.no-event-expiry / boundary-recheck`: 공개 명령의 구조화 outcome을 확인한다.
- `boundary-recheck-2` → `T16.no-event-expiry / boundary-recheck`: 응답의 구조화 오류 코드 /response/error/code가 INSUFFICIENT_ELIGIBLE_QUANTITY다(계획 §3.4, contracts/command-response.schema.json).
- `post-expiry-dispatched-3` → `T16.no-event-expiry / post-expiry-dispatched`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `post-expiry-dispatched-4` → `T16.no-event-expiry / post-expiry-dispatched`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `allocation-after-boundary-5` → `T16.no-event-expiry / allocation-after-boundary`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `post-expiry-dispatched-6` → `T16.no-event-expiry / post-expiry-dispatched`: 실물량·단위와 독립 손계산을 대조한다.
- `post-expiry-dispatched-7` → `T16.no-event-expiry / post-expiry-dispatched`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `expiry-duty-8` → `T16.no-event-expiry / expiry-duty`: 해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `expiry-duty-9` → `T16.no-event-expiry / expiry-duty`: 책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `expiry-duty-10` → `T16.no-event-expiry / expiry-duty`: fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조/sweeper가 반환한 obligationId로 한정한다.
- `boundary-recheck-11` → `T16.no-event-expiry / boundary-recheck`: 무이벤트 경계 이후30초 이내 실제 autonomous sweep task의 terminal을 관찰한다.
- `boundary-recheck-12` → `T16.no-event-expiry / boundary-recheck`: 이벤트 없는 만료 전에 예약의 nextValidityBoundary T를 실제 index 원 행으로 등록한다.
- `boundary-recheck-13` → `T16.no-event-expiry / boundary-recheck`: sweep의 배분 정지와 의무 upsert는 같은 실제 transaction이다.
- `boundary-recheck-14` → `T16.no-event-expiry / boundary-recheck`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `active-physical-identities` → `T16.no-event-expiry / boundary-recheck`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `T16.no-event-expiry / boundary-recheck`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `T16.no-event-expiry / boundary-recheck`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.
- `actual-expiry-obligation-scope` → `T16.no-event-expiry / expiry-duty`: 현재 allocation20의 만료 의무를 동일 DB snapshot에서 정확히 하나 조회한다.


## 실행 증거

공통 실행 기록은 [inventory 최종 증거](../T03/evidence/inventory-final/README.md)에 있다.
이 case의 각 subcase RED 원본은 `evidence/contract-red-<case>-<subcase>.json`에 보존했다.
