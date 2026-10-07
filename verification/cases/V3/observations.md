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
transactions는 transactionCommandKeys로 지정된 두 경합 명령만 읽는다.
조회·fixture 설치·명시적 fresh split 거래를 경합 거래 수로 더하지 않는다.
lockProbeScopeOnly는 fixture가 지정한 segment/allocation/fence 세 scope의
원행을 immutable scopeId ASC로 읽는다. acquisitionOrdinal1,2,3이
그 순서에 일치해야 한다. movement는 ledgerSequence ASC로 읽는다.
수량 bound의 DB SUM은 관찰 SQL과 해당 allocations/obligations 원 행으로
추적하며 제품의 aggregate 응답을 복사하지 않는다.

## hold-first

hold 먼저면 출고0/배분SUSPENDED/책임 유지다. dispatch 먼저면 실제20 이력과CONSUMED를 보존하고 이동한 실물의 보류·후속 책임을 남긴다.

- `scope-lock-1` → `V3.hold-before-dispatch / scope-lock`: 공개 명령의 구조화 outcome을 확인한다.
- `new-dispatched-2` → `V3.hold-before-dispatch / new-dispatched`: 공개 명령의 구조화 outcome을 확인한다.
- `new-dispatched-3` → `V3.hold-before-dispatch / new-dispatched`: 검증 실패를 해당 오류 코드로 구별한다.
- `new-dispatched-4` → `V3.hold-before-dispatch / new-dispatched`: 실물량·단위와 독립 손계산을 대조한다.
- `new-dispatched-5` → `V3.hold-before-dispatch / new-dispatched`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `new-dispatched-6` → `V3.hold-before-dispatch / new-dispatched`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `new-dispatched-7` → `V3.hold-before-dispatch / new-dispatched`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `new-dispatched-8` → `V3.hold-before-dispatch / new-dispatched`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `allocation-9` → `V3.hold-before-dispatch / allocation`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `blocked-dispatch-duty-10` → `V3.hold-before-dispatch / blocked-dispatch-duty`: 해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `blocked-dispatch-duty-11` → `V3.hold-before-dispatch / blocked-dispatch-duty`: 책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `blocked-dispatch-duty-12` → `V3.hold-before-dispatch / blocked-dispatch-duty`: fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `scope-lock-13` → `V3.hold-before-dispatch / scope-lock`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `scope-lock-14` → `V3.hold-before-dispatch / scope-lock`: 제한 삽입과 출고는 같은 실제 scope fence에 참여하고 현재 제한을 재읽는다.
- `scope-lock-15` → `V3.hold-before-dispatch / scope-lock`: barrier reached와 terminal은 동일 contender transaction의 실제 증거다.
- `scope-lock-16` → `V3.hold-before-dispatch / scope-lock`: 비동기 요청은 실제 terminal await가 있어야 끝난다.
- `active-physical-identities` → `V3.hold-before-dispatch / scope-lock`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `V3.hold-before-dispatch / scope-lock`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `V3.hold-before-dispatch / scope-lock`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.
- `scope-lock-14-source-nonempty` → `V3.hold-before-dispatch / scope-lock`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.
- `scope-lock-14-baseline-nonempty` → `V3.hold-before-dispatch / scope-lock`: 동일 transaction/revision/fence 비교의 구체 원 행은 실제 하나 있어야 한다.

## dispatch-first

hold 먼저면 출고0/배분SUSPENDED/책임 유지다. dispatch 먼저면 실제20 이력과CONSUMED를 보존하고 이동한 실물의 보류·후속 책임을 남긴다.

- `committed-dispatch-preserved-1` → `V3.dispatch-before-hold / committed-dispatch-preserved`: 공개 명령의 구조화 outcome을 확인한다.
- `committed-dispatch-preserved-2` → `V3.dispatch-before-hold / committed-dispatch-preserved`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `allocation-3` → `V3.dispatch-before-hold / allocation`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `post-dispatch-hold-response-4` → `V3.dispatch-before-hold / post-dispatch-hold-response`: 해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `post-dispatch-hold-response-5` → `V3.dispatch-before-hold / post-dispatch-hold-response`: 책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `post-dispatch-hold-response-6` → `V3.dispatch-before-hold / post-dispatch-hold-response`: fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `history-deletion-or-fake-rollback-7` → `V3.dispatch-before-hold / history-deletion-or-fake-rollback`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `post-dispatch-hold-response-8` → `V3.dispatch-before-hold / post-dispatch-hold-response`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `history-deletion-or-fake-rollback-9` → `V3.dispatch-before-hold / history-deletion-or-fake-rollback`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `post-dispatch-hold-response-10` → `V3.dispatch-before-hold / post-dispatch-hold-response`: barrier reached와 terminal은 동일 contender transaction의 실제 증거다.
- `post-dispatch-hold-response-11` → `V3.dispatch-before-hold / post-dispatch-hold-response`: 비동기 요청은 실제 terminal await가 있어야 끝난다.
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
- `source-order-reconciliation-7` → `V3.late-old-qc-release / source-order-reconciliation`: 해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `source-order-reconciliation-8` → `V3.late-old-qc-release / source-order-reconciliation`: 책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `source-order-reconciliation-9` → `V3.late-old-qc-release / source-order-reconciliation`: fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `source-order-reconciliation-10` → `V3.late-old-qc-release / source-order-reconciliation`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `active-physical-identities` → `V3.late-old-qc-release / new-hold`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `V3.late-old-qc-release / new-hold`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `V3.late-old-qc-release / new-hold`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.


## 실행 증거

공통 실행 기록은 [inventory 최종 증거](../T03/evidence/inventory-final/README.md)에 있다.
이 case의 각 subcase RED 원본은 `evidence/contract-red-<case>-<subcase>.json`에 보존했다.
