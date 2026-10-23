# T09 목표와 immutable 입력 projection

Task와 현재 Step은 OPEN이다. baseline은
`db594723c3159bbefadf3b6c8e8674a55fd0e4fd`다. 작업 tree는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-8f21ea6b`,
branch는 `fix/t09-goal-input-projection`이다. 실제 runtime은
`codex/gpt-6.1-sol`, `reasoningEffort=medium`, `serviceTier=default`다.
`t3_thread_configuration`으로 확인했다.

저장된 GoalReferences와 InputSnapshots의 이름·관계를 observer가
빠뜨렸다. 목표의 target과 평가 수량을 구별하면서 원문 값을 투영한다.
backend, schema, 인가, case와 oracle은 변경하지 않는다.

## 원천 관계

아래 ID는 진단의 B-goal-input-projection 14개와 정확히 대응한다.
각 organization/Work/Goal/Assessment ID, 원문 pointer, hash와 시점은
`.local/t09-goal-input-projection/source-bindings.json`에 있다.
원천 HTTP 자료의 분석은 새 scenario PASS가 아니다.

| assertion ID | 저장 원천과 projection |
|---|---|
| cumulative_event-field-placeId | GoalReferences.slotsJson.placeId |
| cumulative_event-field-periodStart | slotsJson.periodStart |
| cumulative_event-field-periodEnd | slotsJson.periodEnd |
| cumulative_event-field-eventKinds | slotsJson.eventKind 한 값을 배열로 표현한다 |
| cumulative_event-field-distinctContributionScope | slotsJson.contributionScope |
| state_at-field-placeId | slotsJson.placeId |
| state_at-field-evaluationTime | slotsJson.evaluationAt |
| state_at-field-eligibilityAction | slotsJson.action |
| state_at-field-includeReservations | slotsJson.includeReserved |
| exists-same-goal-fields | targetQuantity/unit/quantityMode와 slotsJson의 place/period |
| state-same-goal-fields | 위와 같은 원천이며 해당 Work의 Goal만 읽는다 |
| throughout-same-goal-fields | 위와 같은 원천이며 해당 Work의 Goal만 읽는다 |
| state-same-assessment-input-snapshot | InputSnapshots.ID를 snapshotId로 읽는다 |
| throughout-same-assessment-input-snapshot | InputSnapshots.ID를 snapshotId로 읽는다 |

`quantity`는 GoalReferences.targetQuantity이며 단위는 원행 unit이다.
quantity slot의 단위를 amount로 읽거나 evaluatedQuantity를 만들지 않는다.
중첩 expression/source에서 임의 후보를 찾아 빈 slot을 채우지 않는다.

입력은 정확한 contentJson bytes의 SHA-256과 contentHash,
AssessmentReferences.inputSnapshotHash를 대조한다. 같은 organization의
정확한 Assessment ID, Work, Goal, asOf, knownAt, conditionsJson과
snapshot.conditions가 일치해야 한다. embedded goal이 있으면 그
organization/Work/ID/slots도 대조한다. Assessment당 알려진 snapshot이
여러 개면 UNKNOWN에 해당하는 unsupported로 거부한다. 원래 input의
UNKNOWN, 원문 evidence, source ID와 조건 ID를 그대로 둔다.

모든 SQL은 기존 read-only REPEATABLE READ connection에서 실행한다.
recordedAt과 knownAt 경계를 유지하며 현재 fact를 옛 snapshot에 합치지
않는다. 외부 공개/private auth 경로는 변경하지 않는다.

## 집중 검증

Java 21, PostgreSQL 18.6, repository Maven wrapper를 사용했다.
생성 CSN이 없어 첫 PG 실행이 실패했으며, backend의 잠긴 npm 의존성을
설치하고 현재 CDS에서 CSN만 생성했다. backend source는 바꾸지 않았다.
중간 컴파일·test fixture 오류와 최종 성공 로그를 모두 보존했다.

- projection/ReferenceResolver/InputEncoding unit 30개가 통과했다.
- 실제 PG 집중 검증 13개가 통과했다. 잘못된 조직·Work·Goal·hash·조건·
  knownAt, 없는 Assessment, 중복 snapshot, 누락 slot, 변조 embedded
  Goal/slots, read-only snapshot과 동시 commit 경계를 포함한다.
- 기존 HTTP 자료의 immutable 행을 별도 PG에 넣고 새 observer로 읽었다.
  누적 사례는 Goal 3개/snapshot 4개, 상태 사례는 Goal 4개/snapshot 6개다.
  이 검증은 source-row replay이며 제품 command 재실행이 아니다.
- `./verify prepare`는 exit 0이다. 전체 runtime 인수 PASS를 뜻하지 않는다.
- `./verify scenarios --actual`은 실행하지 않았다. Haiku Workflow 소유다.

원래 T09의 94 assertions와 fixture/source/actions/grants/quantity/clock은
baseline과 byte가 같다. T09 tracked 파일 32개의 hash 대조는
`.local/t09-goal-input-projection/byte-preservation.json`에 있다.

## 남은 경계

실제 InputSnapshots는 Goal/Assessment별로 ID가 다르다. 두
same-assessment-input-snapshot assertion의 기대인 ID 동등성을 만들 수
없다. snapshotId 누락은 해소하지만 실제 ID를 투영하므로 원래 실패가
ID 불일치로 남을 수 있다. query revision이나 합성 공통 ID를 쓰지 않는다.

movementId command 응답 3개와 무PO receipt contribution 3개는 별도
owner 경계다. assessmentInputs의 segmentId/movementId도 immutable
원문에서 증명하지 않은 값을 보완하지 않는다. 기존 27 PASS/20 FAIL 및
47 NOT_RUN 결과를 이번 집중 검증으로 재분류하지 않는다.

명령·exit·count·source/class/fixture hash와 cleanup은
`.local/t09-goal-input-projection/handoff.json`으로 반환한다.
