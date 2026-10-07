# S2 Work 경계

S1 Work와 별도 mutable 세계를 만들지 않도록 기존
`mulino.work.read.Works`를 canonical lifecycle record로 확장한다.
`GoalReferences` 자체가 immutable GoalVersion이며 currentGoalVersionId가
현재 목표를 가리킨다. S1 import는 lifecycleMode=IMPORTED로 식별한다.
S1 AssessmentReferences는 과거 imported 판단으로 보존한다.

WorkAccess는 responsibility/evaluation/runtime의 동일 transaction port다.
평가 module은 WorkAssessmentGuard를, 책임 module은 WorkResponsibility를
구현한다. port가 없으면 activation/close/resume은 fail-closed다.
명령 gateway가 인가·audit·idempotency·outbox·transaction을 소유한다.

기준선 40a240c3886d3316ba1fd686e9b44bfa1ba6ec2d다.
사용자 Step3 / S2 ACTIVE이며 통합 인수는 아직 NOT_RUN이다.

WorkContributions는 실제 canonical occurrence의 논리적인 credit interval을
배분한다. startQuantity/quantity는 typed decimal이고 같은 actual occurrence의
범위를 서로 다른 조건 이름이나 부모 업무로 중복 credit할 수 없다.
이 allocation은 실물 분할이나 serial identity의 검증을 생성하지 않는다.
문자열 physicalScope만으로 범위를 확인하지 않는다. 실제 scope binding이
있는 adapter가 opaque reference와 typed interval로 변환해야 한다.

allocation은 원 GoalVersion의 출처를 보존하고 targetWork의 현재 목표에서
동일 실제 사건을 평가할 때 읽는다. 목표 변경이 과거 실제 기여를 삭제하지
않으며 evaluator가 현재 사건 종류·기간·정책·단위를 다시 적용한다.
기여량을 자식 업무 종료 상태에서 만들지 않는다.
