# S2 판정 구현 기록

같은 실물 사건의 문서 수와 현재 재고를 목표 기여로 합산하면 과거
판정을 재현할 수 없다. AssessmentReferences는 GoalReferences와 기존
Work ID를 유지하며 V11에서 고정 의미와 입력 snapshot을 추가한다.

- `TypedPredicateEvaluator`는 bounded typed predicate, distinct physical
  scope 합산, actual state, interval coverage와 UNKNOWN/CONFLICT를
  계산한다. verified가 아닌 입력은 수량0으로 치환하지 않는다.
- `AssessmentFactProvider`는 조직·knownAt 범위 CQN 조회로 canonical,
  verification, claim, event, inbox, document와 segment 입력을 만든다.
  S3/S4 action eligibility·receipt contribution은 미구현이며 해당 판정은
  UNVERIFIED다. raw claim은 이행 입력으로 승격하지 않는다.
- `AssessmentService`는 immutable Assessment와 InputSnapshots를 같은
  transaction에 저장하며 Work pending 표시를 갱신한다. 원 정의와
  evaluator를 지원하지 않거나 현재 EVIDENCE policy가 없으면 HELD다.
  이전 판정의 납기 위반은 새 충족 판정에도 유지한다.
- `requireFulfilled`는 새 CQN snapshot과 현재 policy로 최신 판정 hash를
  대조한다. imported S1 판정과 stale 입력은 이행 종료 근거가 아니다.

현재 결과는 구현 중이다. Unit/실제 PostgreSQL 실행 기록을 추가하기
전에는 업무 PASS를 주장하지 않는다. 유료 model/BTP는 NOT_RUN이다.
