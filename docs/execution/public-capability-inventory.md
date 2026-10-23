# 기존 공개 command capability inventory 대조

이 변경은 기존 서버 등록의 발견·공개다. 새 handler, endpoint, slot,
role/grant나 업무 효과를 만들지 않는다. Task/Step은 OPEN이며 runtime
acceptance는 주장하지 않는다. baseline은 `ce0eecc1`이다.

## 등록과 공개 경로

`OntologyApplication`의 component scan에 포함되는 main source의 실제
CommandHandler 구현 23개를 조사했다. 그 metadata 메서드를 실행한 결과
총98 capability이고 일반 profile에는97개다. verification 전용1개는
아래에 별도로 분류한다. 내부 service의 메서드 이름을 capability로
추정하지 않았다.

`ApplicationCommands` 생성자는 주입된 List<CommandHandler>의
capabilities()를 실제 router에 등록하고 중복을 거부한다. publishesSlots
handler 집합과 CapabilitySlots의 unconditional 계약이 일치해야 시작한다.
operations()/manifest()는 이 router에서 나온다.

공개 `OntologyRest`는 capability 목록과 commands/records 경로를 이
ApplicationCommands에 연결한다. `OntologyCap`도 동일한 명령/검증 pipeline을
사용하고 `OntologyMcp`의 tools/list/call도 같은 operations를 사용한다.
이런 경로에 이미 등록된 이름만 새 inventory entry로 추가했다.
정의에 해당 capability가 포함되어야 하고 현재 인증·scope·정책·revision
검증을 통과해야 실제 업무를 실행할 수 있다는 조건은 그대로다.

## 추가6개와 개별 근거

모두 `backend/src/main/java/com/mulino/application/responsibility/`
`ResponsibilityCommands.java:capabilities()`에 이미 등록되어 있다.
기본 CommandHandler.intentKinds()는 COMMAND, semanticVersion()은1.0.0이다.
각 slot 목록은 기존 `contracts/capability-slots.json`의 해당 key다.
그 파일은 수정하지 않았다.

| 새 inventory entry | 기존 execute 분기와 업무 경계 | 기존 slot 계약 |
|---|---|---|
| createObligation | openDuty로 보내는 기존 alias다. prepare/execute는 GOAL_QUANTITY_SHORTFALL에 이 alias를 쓰면 거부한다. | kind,nextAction,nextCheckAt,quantity,scopeJson,sourceId,sourceKind,sourceVersion,unit,workId |
| openObligation | openDuty→openGoalShortfall의 실제 typed proof 경로도 포함한다. 실제 COMMITTED assessGoal command/hash와 현재 scope/proof를 요구한다. | 위 createObligation과 동일 |
| expireHandover | decide(..., EXPIRED)를 호출한다. 실제 expiresAt 이후에만 만료할 수 있다. | handoverId |
| decideQuantityDutyWaiver | decideWaiver의 기존 kind별 quantity 결정이다. 실제 assignment, designated human authority, 현재 정책과 proposal/approval guard를 요구한다. 결정 자체를 waiver 적용으로 표시하지 않는다. | assignmentId,decision,reason,validUntil |
| decideQualityDutyWaiver | 위 결정 경로의 quality authority다. 다른 종류의 의무를 임의 면제하지 않는다. | 위와 동일 |
| decideRecallDutyWaiver | 위 결정 경로의 recall authority다. 현재 kind별 catalog/정책을 따른다. | 위와 동일 |

기존114 entries의 값과 순서는 보존하고 뒤에6개만 추가했다.
새 entry의 implemented=true/semanticVersion=1.0.0은 실제 구현 등록을
대조한 뜻이며 실제 actor 업무 성공이나 시나리오 PASS가 아니다.
과거 entry의 PLAN-1.0/implemented 표시는 이번 변경에서 재분류하지 않는다.
모든 일반 handler 이름의 공개 inventory/slot 존재 여부는 그 과거 flag와
무관하게 대조한다.

## 제외·미지원 이름과 담당 경계

| 이름 | 분류·근거 | 잔여 담당 |
|---|---|---|
| fixtureExternalEffect | RuntimeFixtureCommands의 @Profile(verification) 전용이고 slot 계약도 profile=verification이다. 일반 ApplicationCommands manifest에는 없다. 일반 업무 공개 inventory에 추가하지 않는다. verification host에서는 실제 외부효과 fixture일 뿐이다. | runtime verification owner |
| retrySafeCommand | 기존 계획/probe entry는 보존한다. CommandHandler와 slot 계약이 없다. 내부 retryOriginal 메서드는 이 이름의 공개 handler가 아니다. | runtime/client recovery owner |
| recordRelation | 기존 계획 entry를 보존한다. handler/slot이 없고 내부 WorkLink/typed relation 로직을 범용 write로 공개하지 않는다. | definition/relations product owner |
| emergencyRepair | 기존 계획/dryRun probe만 있다. handler/slot이 없으며 새 우회 수정 권한을 만들지 않는다. | management/runtime recovery owner |
| structureIntent | 기존 QUERY/probe entry를 보존한다. command registry/slot에는 없다. 내부 unsupportedIntent 분류는 구조화 도구 구현이 아니다. | S5 client/intent product owner |

마지막 네 이름은 기존 request-contracts.json에도 NO_HANDLER로 기록되어 있다.
새 검사에서 실제 ApplicationCommands의 validation 경로로 이 이름들과
임의 unknownInventoryMutation을 보내 HELD VERSION_UNSUPPORTED 및 effects={}를
확인한다. registry publication이 이 이름을 지원 명령으로 승격하지 않는다.
query 구현 전체의 별도 inventory 정합성은 이번 CommandHandler 범위가 아니다.

모든 일반97 handler에는 이미 slot 계약이 있었으며, 이번 조사에서 실제
일반 handler의 slot 미정의 공백은 없었다. case가 참조한다는 이유만으로
없는 handler/slot을 새로 선언하지 않았다. 기존 unsupported slot findings도
지우거나 intentional violation으로 바꾸지 않았다.

## deterministic 검사와 한계

새 PublicCapabilityInventoryTest는 main source의 concrete CommandHandler
클래스를 load하고 CALLS_REAL_METHODS mock으로 실제 metadata 메서드만
실행한다. Spring context/DB/HTTP 업무는 실행하지 않는다. handler scope
선택이나 authorization을 mock으로 성공시켜 업무 PASS를 만들지 않는다.

검사는 모든 일반 등록→inventory/slot, implemented command→실제 handler,
slot→handler/profile, form의 published slot 부분집합, duplicate 등록,
실제 ApplicationCommands manifest와 unknown probe 거부를 확인한다.
그 외 기존 미구현 command는 정확히 retrySafeCommand/recordRelation/
emergencyRepair로 보존하며, 임의 이름을 implemented=false로 더하는 것도
검사에서 허용하지 않는다.

처음 검사 작성 때 unknown outcome code를 CAPABILITY_UNSUPPORTED라고
잘못 적은 실패는 보존하고, 실제 기존 VERSION_UNSUPPORTED에 맞게 고쳤다.
그 뒤 올바른 RED는2 PASS/1 FAIL이며 누락6개를 정확히 보고했다.
원문/JUnit/metadata·source/class hash는 `.local/public-capability-inventory/`다.

원래 T11 A의 grants/roleCapabilities는 변경하지 않았다. openObligation을
inventory에 공개해도 A에게 호출 권한이 생기지 않는다. 원래 부족10 root
생성/이전 흐름과 별도 RECEIPT_UNALLOCATED90 OPEN 경계는 계속 미해결이다.
실제 PG/native scenarios/full backend/full harness는 이번 변경에서 NOT_RUN이다.

## 이번 실행 결과와 통합 경계

- 등록/slot/router JUnit: 3 PASS / 0 FAIL / 0 SKIP.
- request-contract script: VALID, problems=0 (41 cases / 374 fixtures).
  기존 knownOpen3707은 보존한다.
- prepare: exit1, asset checks10 PASS / 2 FAIL. 기존 slot findings413 및
  unpublished names emergencyRepair9/recordRelation13/retrySafeCommand35/
  structureIntent6은 그대로다. 이 수치를 줄이기 위해 alias를 만들지 않았다.
- 실패를 전체 원문으로 재현했다. mcp generator tests는1 PASS / 2 FAIL,
  case generator tests는5 PASS / 1 FAIL이다.

실패는6개 이름의 추가가 기존 generator의 catalog 소비 결과를 바꾸기
때문이다. `verification/mcp-tests/author_cases.py`의 T20 expected capability
목록, `verification/cases/C3/author_prerequisites.py`의 C3 case와
`verification/cases/V4/author_review_fixes.py`의 V4 case/observation-bindings가
현재 committed output과 다르다. 원문은 mcp-generators-red.log와
case-generators-red.log에 있다. prepare.json에도2개 실패를 유지한다.

그 파일들의 원래 cases/oracles/fixtures/grants/bindings는 이번 소유권
밖이며 변경하지 않았다. 해당 authoring owner가 공개 inventory 발견과
원래 fixture의 명시적 권한·oracle 보존을 함께 처리해야 한다. 단순한
전체 재생성으로 원래 actor에게6개 capability를 새로 주면 안 된다.
검사를 제외하거나 예외를 늘려 기존 generator mismatch를 숨기지 않았다.
따라서 이 publication은 standalone scoped commit이지만 prepare를 통과한
통합 결과는 아니며 root가 결합 검증을 소유한다.
