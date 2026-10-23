# T11 실제 입력과 receipt identity 연결

Step baseline은 `477bba574e9ac6c4be32b236d93164f763b12401`이다.
branch는 `fix/t11-runtime-inputs-and-receipt-identity`이며 이전 branch의
`b7f55549`를 보존했다. Task/Step은 OPEN이다.

## 850496aa의 unsupported 생성 입력을 철회한다

`850496aa`는 parent90 세 변형에 authored action `recordGoalShortfall`을
추가하고 기존 제품 handler `openObligation`을 호출했다. 그러나 공개
acceptance inventory와 원래 fixture 권한을 충족하지 못했다.
prepare의 `Unknown public capability "openObligation"`, exit 3은
`.local/t11-runtime-input-correction/prepare-original-red.log`에 보존한다.
이 입력 commit은 coordinator가 통합하지 않았다.

이번 수정은 세 parent subcase 전체를 baseline과 동일하게 복원한다.
생성 명령과 그 명령 응답을 참조한 transfer/accept 입력도 함께 철회한다.
`getAssessment`가 의무를 만든다고 표시하거나 없는 ID를 채우지 않는다.
따라서 원래 `/response/obligationId` 부재는 여전히 실제 인수의 잔여
제품/공개계약 경계다. prepare 성공을 parent 흐름 성공으로 해석하지 않는다.

## 공개·허가된 대안이 없는 근거

계획 §5.2는 부족10 이전 후에도 부모90/100을 FULFILLED로 종료할 수 없고
미충족 판정을 보존하도록 한다. §5.3은 의무의 실제 source decision,
stable root, 현재 assignment와 실제 recipient 수락을 요구한다.
D11 표는 자식 종료/부모90≠100·부족10 이전을 명시한다.
계획은 이 의미를 요구하며 `openObligation`이라는 API 이름을 지정하지 않는다.

현재 코드와 원래 grant를 함께 확인한 결과는 다음과 같다.

| 후보 | 현재 계약과 배제 근거 |
|---|---|
| `getAssessment`, `getObligations` | 공개 QUERY다. 의무 생성 효과와 생성 root/assignment ID를 반환하는 명령이 아니다. |
| `transferObligation`, `proposeHandover`, `acceptHandover` | 공개되어 있고 원래 A에게 허용된다. 기존 실제 assignment 또는 handover가 선행해야 하며 부족10의 최초 proof/root를 만들지 않는다. |
| `createFollowup` | 공개되어 있고 A에게 허용된다. `WorkLifecycle`은 원래 CLOSED Work와 기존 obligationIds를 검증해 연결한다. 현재 ACTIVE 부모의 새로운 부족10을 증명·생성하는 대체 경로가 아니다. |
| `activateWork`의 초기 의무 | `ResponsibilityService.initialDuty`는 nextAction이 있을 때 일반 WORK_NEXT_ACTION을 만든다. 원래 부모 createWork에는 nextAction이 없고 이 의무는 증명된 수량10이 아니다. 새 text나 slot을 추가해 대체하지 않는다. |
| receipt shortfall/excess/unallocated | 실제 PO line과 ReceiptCredits가 있어야 RECEIPT_SHORTFALL이다. 현재 no-PO90은 RECEIPT_UNALLOCATED90이며 부족10으로 재해석하거나 부분 이전하지 않는다. |
| `createObligation` | handler/slot catalog에만 있으며 공개 inventory와 원래 grant에는 없다. `ResponsibilityCommands.prepare/execute`는 GOAL_QUANTITY_SHORTFALL을 이 alias로 보내면 명시적으로 거부한다. |
| `openObligation` | handler/slot catalog에는 있다. 해당 kind에 대해 GoalShortfallProof와 actual COMMITTED assessGoal command/hash를 검증하고 실제 root/current assignments를 반환하는 현재 유일한 제품 경로다. 그러나 공개 inventory와 원래 actor grants에는 없다. |

정확한 구현 근거는 다음과 같다.

- `contracts/acceptance-capabilities.json`: QUERY 및 lifecycle/transfer 명령은
  선언되어 있지만 `openObligation`과 `createObligation`은 없다.
- `contracts/capability-slots.json`: 두 handler의 입력 slot은 선언되어 있다.
- `ResponsibilityCommands.capabilities/prepare/execute`: GOAL kind는
  openObligation 전용이며 현재 capability 권한을 우회하지 않는다.
- `ResponsibilityService.openDuty/openGoalShortfall`: 실제 proof로 root를 만들고
  `rootId`와 현재 `assignments`를 반환한다. 과거 assessment 조회는 이 경로가 아니다.
- `ApplicationCommands.apply`: `cap`의 현재 scope 권한과 commit 권한을 확인한다.
- `verification/cases/T11/fixtures/parent90-{cancelled,impossible,superseded}.json`:
  A를 포함한 모든 actor의 roleCapabilities/grant.actions에 openObligation이 없다.

이는 inventory 한 줄만 추가하면 해결되는 입력 오타가 아니다. 현재 제품의
명시적 부족10 경로를 공개할지, 어떤 실제 업무 actor가 어떤 scope 권한으로
호출할지의 경계가 함께 남는다. 이번 소유권에서는 capability/grant/제품 의미를
추가하지 않는다. 새로운 의무나 PO·source·waiver를 발명하지 않는다.

## 계속 적용하는 안전한 입력 수정

`author_runtime_inputs.py`는 baseline 위의 가역 계층이다.
`runtime-input-ledger.json`에 변경 전후, 추가 행동과 복구 근거를 기록한다.

- shared-distinct60의 실제 두 link는 NEEDS_INPUT VERIFIED_RANGE_REQUIRED였다.
  원래 canonical source의 0부터 60 BOX와 serial 라벨을 대조해
  `[0,30)`, `[30,60)`으로 옮겼다. 원래 SHARES_ACTIVITY kind와 양을 유지한다.
  두 link의 expectedRevision은 activity가 실제 속한 parent-one의 getWork다.
- wait-timeout의 unsupported `resumeEvidenceIds: []`를 공개 `evidence: {}`로
  연결한다. 증거 없음이라는 원래 negative 입력을 보존하며 DOC를 proof로
  넣지 않는다. 최신 getWork revision을 읽어 stale revision의 선행 거부를 막는다.
  서버의 실제 resume predicate 판정은 유지하며 원래 REJECTED 기대도 바꾸지 않는다.
- pending-correction의 close는 새 getWork의 현재 revision을 사용한다.
  정정 근거나 evaluator·fault 처리를 합성하지 않는다. 원래 기대를 유지한다.

원래 110 assertion JSON 블록, fixtures의 grants/양/시계/source,
126개 이전 binding ledger와 독립 mandatory oracle 파일은 byte 보존한다.
parent 세 변형을 포함해 선택한 세 subcase 밖 아홉 subcase 전체가 baseline과
동일하다. 기존 parent-range strict inverse는 이 새 계층을 엄격히 역변환한다.
T09/T12 공통 authoring은 수정하지 않았으며 unrecorded mutation을 거부한다.

## Receipt 응답 identity와 남은 실제 실패

`caf9dac6`은 coordinator 검토 중이며 이번 수정에서는 변경하지 않는다.
확정 `Receipts.canonicalOccurrenceId`만 top-level과 effects.occurrenceId에
연결한다. provisional은 canonical ID를 만들거나 반환하지 않는다.
이전 실제 PG RED/GREEN과 source/class hash는 `.local/t11-runtime-inputs/`에
보존한다. 이번 입력 수정에는 새 PG 실행이 필요하지 않아 반복하지 않는다.

원래 RECEIPT_UNALLOCATED90은 별도로 OPEN이다. alternative-close까지의
PASS를 주장하지 않는다. unsupported-evaluator의 HELD/REJECTED 정책 차이,
pending-correction의 잘못된 correction 입력과 fault scope,
cancel-after-shipment의 reserve 적격 거부, closed-followup의 recordActivity
occurrenceId 부재는 별도 경계다. wait-timeout의 원래 raw는 6 assertions가
PASS지만 required control adapter가 missing으로 기록되어 NOT_RUN이었다.
resume 입력 수정으로 이 adapter 판정까지 해결했다고 주장하지 않는다.

이번 재현·보존·prepare 결과와 source hash는
`.local/t11-runtime-input-correction/`에 남긴다. 실제 scenario/full suite,
새 capability/grant, 평가/DB schema/observer/공통 runner는 변경·실행하지 않는다.

이번 corrective 검증은 T11 Python 13 PASS, 이전 T09/T11/T12 보존 Python
20 PASS, prepare asset checks 12 PASS다. prepare는 PREPARED/exit 0이며
runtime gate는 OPEN이다. 기존 slot-contract FINDINGS 414개(T11 7개)는
그대로 보고된다. parent의 옛 acceptanceEvidenceId, correction의 미지원
입력 등 남은 계약 공백을 waiver/삭제/검사 완화로 숨기지 않았다.

coordinator는 receipt commit을 `f4852678`에 별도로 통합하고 실제 PG
3개를 확인했다. 입력은 거부된 중간 상태를 통합하지 않고
`caf9dac6..684b0c22`의 최종 delta8개 파일만 검토·적용했다.
root의 Python13개와 prepare asset checks12개가 통과했다. 원본110개
assertion 블록·fixture12개·이전 ledger를 보존했고 parent 세 변형을
포함한9개 subcase가 baseline과 동일함을 독립 대조했다. 증거는
`.local/resume-20261023/t11-runtime-input-integrated/`에 남긴다.
실제 Haiku 재실행은 아직 NOT_RUN이며 Task와 현재 Step은 OPEN이다.
