# 확정 endpoint와 goal 저장 필드의 정합

2026-10-22에 T17의 인도 끝점과 V5/T26 goal의 직접 대응 필드를
수정했다. 정상 활성화 fixture의 완성은 미해결이다. 기존 증거 요건을
현재 재고 조건으로 바꾸어 PASS를 만들지 않았다.

## 범위와 실행 기준

- baseline: `6f88fd0616f62dec07b9c268cc1ec33d7d9421a2`
- worktree: `/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-5596ce60`
- branch: `fix/align-case-contracts`
- 모델: runtime의 inherited model은 `gpt-6-astra`다. live catalog에서
  `codex`의 Astra `serviceTier=ultrafast` 지원은 확인했다. catalog의
  `currentValue=default`는 이 child의 실제 tier 증명이 아니며, 이
  실행 경로에는 현재 turn의 tier를 설정하거나 확인할 도구가 없다.
  Ultrafast 실제 적용 여부는 parent의 dispatch 기록으로 확인해야 한다.
- Java `21.0.12.1`을 `JAVA_HOME`과 PATH 앞에 지정했다.
- backend, C3/V4, ActualAcceptanceDriver, normative hash/vocabulary,
  fixture 원문, Gherkin, oracle bindings는 변경하지 않았다.
- 재위임, push, PR, 외부 알림, 실모델 호출은 하지 않았다.

## 수정과 보존

T17의 eligibility 5개 subcase에 있는 `eligibility-response-5/6`
10개 assertion만 `CUSTOMER_ACCEPTED`에서 `DELIVERED`로 바꿨다.
전자는 DB orders 원행의 exactSet이고 후자는 API response의 equals다.
10월 12일 사용자 결정과 이미 수정된 판매 요청에 맞춘다. pointer,
op, scope, evidence/oracle 연결, 수량과 나머지 assertion은 같다.

V5 4개와 T26 13개 `createWork` goal에서 다음 두 이름만 바꿨다.

- `quantity: "20"` → `targetQuantity: "20"`
- `destinationId: {$alias: "W"}` → `placeId: {$alias: "W"}`

`GoalInput.java`의 저장 필드와 직접 대응한다. 단위, 목표 끝점,
quantityMode, 기한, 시간대, 증거 policy, owner/supervisor,
nextAction/nextCheckAt, 요청 provenance와 모든 assertion을 보존했다.
`author_resume_contracts.py`는 이 변환과 T17 한정 assertion 변환을
재현한다. 구/신 필드 값이 상충하면 덮어쓰지 않고 실패한다.
selftest는 비대상 assertion과 미확정 시간·증거 조건이 유지되는지도
검사한다.

## 정상 활성화 fixture를 막는 계약

단순히 빠진 필드를 채워 정상 fixture라고 부를 수 없는 근거다.

1. `GoalInput.validate`는 `deadlineInclusive`를 허용하지 않는다.
   STATE_AT은 별도 `evaluationAt`, `action`, `includeReserved`,
   `scope`, `conditions`, `evaluatorVersion`을 요구한다. 기존 dueAt은
   의무 확인 기한이기도 하며 평가시점과 같다는 명시가 없다.
   누적 기간의 `periodEndInclusive`로 옮기면 다른 모드의 뜻을 섞는다.
   따라서 이 필드를 삭제하거나 새 시점을 추측하지 않았다.
2. V5/T26 fixture의 `baseline.definitionContract`는
   `verifiedDistinctQuantity`, 사건 `EV-READY`, P/W 범위의20BOX,
   `missingEvidenceResult=UNVERIFIED`, `evaluator-v1`을 명시한다.
   기다림도 `AWAITING_VERIFIED_EVIDENCE`와 같은 사건의
   `evidenceAvailable`을 요구한다. 기존 실물20BOX가 있다는 이유로
   목표를 충족시키는 것은 이 요건을 약화한다.
3. `TypedPredicateEvaluator.VERSION`은 `core-v1`이다.
   `AssessmentService.definition/validateConditions`는 해당 evaluator,
   유일한 endpoint+mode template, 발행된 template 이름과 일치하는
   단일 condition selector를 요구한다. `verifiedDistinctQuantity`를
   지원하는 발행 template이나 의미 보존 mapping은 확인하지 못했다.
4. `AssessmentFactProvider`의 STATE_AT은 canonical event 대신
   segment 상태를 읽는다. PHYSICAL/PHYSICAL_HELD와 예약 포함 조건을
   넣어 현재20BOX를 세게 하면 EV-READY 검증 없이 충족할 수 있다.
   반대로 CUMULATIVE_EVENT로 바꾸면 사용자가 보존하라고 한 목표
   모드를 변경한다. 둘 다 이 수정의 범위가 아니다.
5. `ScenarioFixtureInstaller.survey`는 `baseline.definitionContract`를
   consumed/descriptive로 인정하지 않아 omitted fact로 남긴다.
   별도 DefinitionVersion content가 없으면 installer convention을
   발행한다. 현재 convention에는 `arrived`/`delivered` 누적 template만
   있고 STATE_AT/VERIFIED template이 없다. fixture의 설명을 저장형
   definition으로 옮기기만 해도 지원 evaluator와 증거 취득은 해결되지
   않는다. adapter/backend 소유자의 조치가 필요하다.
6. `createWork`는 `WorkLifecycle.execute`에서 이미 활성화한다.
   이17개 case는 뒤이어 `create-activate`를 호출한다. 이후 호출은
   DRAFT만 허용하므로 goal 문제가 해결된 뒤 별도 state conflict가
   남는다. createDraft로 바꾸거나 activate를 삭제하면 명령/관찰 범위가
   바뀌므로 이번 endpoint/goal 수정에서는 보존했다.

다음 통합 판단은 EV-READY 검증을 포함한 STATE_AT/VERIFIED 조건의
지원 계약, 명시적 평가시점·행동·예약 처리, definition 설치 방법과
create/activate 흐름이다. 이 판단 없이 `core-v1`, `conditions`,
PHYSICAL 또는 임의 평가시점을 넣어 정상 fixture로 표시하지 않는다.
V5/T26의 활성화는 여전히 미해결이며 actual 결과를 주장하지 않는다.

## 검사

| 명령/검사 | exit | 결과 |
|---|---:|---|
| `python3 -m unittest discover -s verification/cases -p test_resume_contracts.py -v` | 0 | 10 tests 통과 |
| `python3 -m unittest discover -s verification/mcp-tests -p test_generators_reproduce.py -v` | 0 | 3 tests 통과 |
| `python3 -m unittest discover -s verification/requirements -p test_case_generators_reproduce.py -v` | 0 | 6 tests 통과 |
| `python3 verification/cases/author_resume_contracts.py` 재실행 | 0 | `changes {}` |
| baseline JSON과 허용 변환만 적용한 expected의 deep equality | 0 | T17/V5/T26 일치 |
| `./verify validate verification/cases/T17/case.json verification/cases/V5/case.json verification/cases/T26/case.json` | 0 | 세 case 모두 PREPARATION_SCHEMA_VALID |
| `./verify prepare` | 1 | 아래 기존 hash/vocabulary 실패 |
| `git diff --check` | 0 | whitespace 오류 없음 |

prepare 시각은 `2026-10-22T08:10:03.020849Z`다. 41 cases,
802 subcases, 24,034 assertions를 검사했다. 자산 검사12개 중10개가
통과했다. 준비 실패는 세 normative 문서의 hash drift와 vocabulary의
미사용 `CAPABILITY_UNSUPPORTED/REJECTED` pending pair다. 다른 worker가
소유한 lock/vocabulary를 바꾸지 않았다. capability slot known-open과
실행 gate도 숨기지 않았다.

로컬 원본은 `verification/harness/target/evidence/prepare.json`과
`/tmp/mulino-goal-fixtures-prepare.log`다. runtime receipt가 아니다.
schema 검증 원본은 `/tmp/mulino-goal-fixtures-validate.log`다.
actual scenarios, backend integration, 실모델 UAT는 `NOT_RUN`이다.

변경 후 case SHA-256은 다음과 같다. fixture JSON은 baseline과 같다.

- T17: `2490d07c5c50dda152d8ba40bfe52c065894246028cc14c61c6615b0ed861f0e`
- V5: `f289511ecc279bcdeccc5f46aec869798b9899ef60441b69e882cfe44ff73543`
- T26: `c1c9c469409a30cbae339ab67b71f0d2c056edbb5a60caed47119e926a907527`
