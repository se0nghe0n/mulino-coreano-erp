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
