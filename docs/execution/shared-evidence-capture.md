# 공통 evidence capture와 목표별 평가 envelope

Task와 Step은 OPEN이다. baseline은
`477bba574e9ac6c4be32b236d93164f763b12401`이고 branch는
`feat/shared-evidence-capture`다. 작업 tree는
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-8f21ea6b`다.
기존 `fix/t09-goal-input-projection`의 `700101fd`는 보존했다.
실제 runtime은 `t3_thread_configuration`으로 확인한
`codex/gpt-6.1-sol`, high, serviceTier=priority(Fast)다.

사용자의 질문 없이 계속 진행하라는 autonomy 지시에 따라 coordinator가
기존 mandatory oracle을 유지하면서 공통 원천 capture와 목표별 불변
평가 envelope를 분리하기로 결정했다. 세부 설계를 사용자가 선택했다고
기록하지 않는다. 과거 PENDING 기록은 이번 brief의 구현 지시로 대체됐다.

## 저장과 동시성

V43이 `mulino_evaluation_EvidenceCaptures`와 InputSnapshots의 nullable
`evidenceCaptureId`를 추가한다. 과거 snapshot의 원문/hash는 변경하지
않고 capture를 backfill하지 않는다. 과거 평가를 현재 facts로 재구성하지
않으며, 새 명시적 평가가 새 계약을 사용한다.

실제 capture 행의 UUID가 공통 identity다. SHA-256은 원문 검증과 내부
unique key/transaction lock에만 쓰고 공통 ID로 노출하지 않는다.
`EvidenceCaptureRepository.acquire`는 부모 transaction 안에서 조직과
payload hash의 advisory lock을 얻는다. organization/hash unique 조건의
INSERT 뒤 같은 capture 원문 bytes를 다시 대조한다. 다른 payload와
충돌하면 공유하지 않고 거부한다. ID는 실제 저장된 행에서 읽는다.

capture payload는 다음 두 부분을 고정한다.

- boundary: organization, 실제 item/optional LOT/place/Work scope,
  asOf/knownAt/evaluationHorizon, admission purpose/contract, 고정된
  definition/evaluator와 policy identity/hash, source 선택에 쓰인
  action/예약 포함/평가시점/사건·구간/source selector다.
- facts: 당시 provider가 선택한 properties/relations/sources 전체다.
  source ID/revision/hash, 실제 값/단위, evidenceRefs와 시점을 보존한다.

quantityMode·목표 수량·조건 판정은 평가별 envelope에 둔다.
observationPolicy는 Goal의 평가 정책이며 raw provider 원천 선택자는
아니므로 envelope에 보존한다. 실제 facts와 원천 boundary가 다르면
공유하지 않는다. JSON 배열 순서까지 다른 원문을 억지로 정규화해서
공유하지 않는다. 같은 세 T09 모드의 source capture만 실제로 재사용한다.

새 InputSnapshots는 이전처럼 Assessment/Work/Goal에 각각 귀속되고
자체 UUID·원문·hash를 유지한다. 추가 binding은 capture의 실제 UUID를
참조한다. FK는 조직과 asOf/knownAt을 함께 묶는다. INSERT trigger는
Assessment와 envelope의 Goal/Work/정의/정책/조건/hash, capture boundary와
facts의 동등성을 검증한다. capture는 UPDATE/DELETE가 불가능하다.
조직별 item/LOT/place/Work FK, LOT/Work의 item 일치, 원천 조직과
knownAt 경계, 원문 hash와 필수 payload shape도 DB에서 검증한다.

capture는 내부 저장소이며 새 공개 read/write endpoint를 만들지 않는다.
ID 자체로 권한을 부여하지 않는다. 기존 getAssessment의 현재 all-of
인가와 contribution 양끝 Work 인가를 유지한다. 읽기는 저장된 envelope를
검증해 반환하고 평가·capture 생성은 assessGoal에서만 수행한다.
read-only guard 계산은 capture를 INSERT하지 않는다.

## 공개 평가와 관찰 행

`AssessmentPresentation`은 authored template의 실제 root truth를
이름 붙은 조건 한 개로 표현한다. 내부 AST 조건과 입력은 diagnostics에
전부 남긴다. 기존 conditionsJson 및 envelope.conditions도 유지한다.
T09 `held-throughout` 한 행, T12 `arrived-at-W` 한 행이 이 구분을 따른다.
공개 authored 조건의 inputs/evidence는 하위 계산 입력의 합집합이며
각 내부 논리 부모의 원래 빈 inputs는 diagnostics에서 그대로 보존한다.

`quantityEvaluation`은 고정된 단일 property/operator/unit의 실제 수량
판정 입력에서만 산출한다. 같은 quantity leaf가 여러 개면 실제 합이
같아야 한다. UNKNOWN/상충/단위·원천 모호함 또는 interval 모드에서는
공개 scalar를 생략한다. 목표 수량을 evaluatedQuantity로 복사하지 않는다.
getAssessment는 hash로 고정된 이 계산 근거의 evaluatedQuantity/unit을
노출한다. noPO의 실제 receipt60+40 판정100과 구매 기여0은 별개다.

observer mapping 1.5.0의 새 assessmentInputs는 AST 조건별 행이 아니라
capture의 구별된 원천 관계별 행이다. 중복된 property/조건에 같은
movement가 등장해도 `(segmentId,movementId)`는 한 번만 읽는다.
관계는 같은 immutable payload에서 physicalScopeId=Fact.sourceId=
movement.sourceId이고 movement.ID가 Fact.evidenceRefs에 있을 때만 낸다.
다중 관계는 모두 보존하고 첫 후보/기대값/현재 테이블로 채우지 않는다.

snapshotId는 실제 EvidenceCaptures.ID, inputEnvelopeId는 실제
InputSnapshots.ID다. authored conditions와 전체 내부 진단은 중첩한다.
원천 관계가 없으면 capture당 한 행을 보존한다. 따라서 실제 source가
같은 세 모드에서 3/3/6 AST 개수와 무관하게 같은 capture 관계를 읽는다.
과거 envelope는 원래 ID를 유지하며 새 공통 identity를 합성하지 않는다.

T11 PENDING은 envelope의 과거 상태를 바꾸는 값이 아니다.
실제 WorkTransitions의 asOf/knownAt 경계 안 최신 pending 상태와 실제
기존 Assessment를 연결한 `WORK_PENDING_INPUT` marker다. marker는
capture ID나 movement 사용을 만들지 않는다. 시점 입력이 없으면
PENDING projection은 fail-closed로 생략한다.

## primary 통합 hook

아래 두 파일은 소유권 밖이어서 수정하지 않았다.

coordinator는 통합 시 아래 두 hook을 연결했다. getWork는 기존
getAssessment 인가를 통과한 snapshot의 조직·Work·Goal·hash를 검증한
후에만 authored 조건을 사용한다. 새 공개 권한이나 read 면을 추가하지
않는다. 실제 PG에서 getWork와 getAssessment의 조건 일치와 ITEM 권한
제거 후 양쪽 거부를 확인했다. observer PG 회귀에는 원래 capture
진입점에서 요청 asOf가 전달되는지도 추가했다. 결합 검증은 진행 중이다.

1. `ScenarioJdbcObservation.capture`에서 projection 생성 시 기존 인자 뒤에
   `OffsetDateTime.parse(Json.required(request,"asOf"))`를 넘긴다.
   새 overload가 이를 받아 PENDING의 effective-time fence를 적용한다.
   기존 constructor는 호환성을 유지하되 PENDING을 추정하지 않는다.
2. `WorkReadHandler.linked`의 AssessmentReferences conditionResults에도
   검증된 envelope.publicConditions를 연결한다. 현재 getAssessment와
   observer는 새 표현을 쓰지만 getWork의 기존 raw AST 직접 decode는
   이 hook 전까지 유지된다. 현재 인가/조회 면을 넓히지 않아야 한다.

## 집중 검증과 잔여 경계

Java21, PostgreSQL18.6과 저장소 Maven wrapper를 사용했다.
실제 runtime 설정·명령/exit·source/class/JAR/fixture hash와 cleanup은
`.local/shared-evidence-capture/handoff.json`에 있다.

- backend focused 6개 PASS다. 실제 PG에서 세 모드의 capture1/envelope3,
  EXISTS_IN 충족과 STATE_AT/THROUGHOUT 미충족, 실제100 공개 수량,
  현재 all-of ITEM 불일치 거부, CAP/JDBC transaction rollback을 확인했다.
- fresh V43와 V42→V43 migration을 실제 PG에서 확인했다. 과거 원문/hash
  및 null capture를 보존하고, 동시4 transaction이 같은 실제 UUID로
  수렴하며 다른 조직/scope/knownAt/source/admission은 분리됐다.
  UPDATE/DELETE·오염 hash·foreign source/조직 FK·잘못된 envelope binding을
  거부했다.
- harness focused 36개 PASS다. 새 observer PG3개와 기존 projection8,
  ReferenceResolver19, InputEncoding6개다. 관계 tuple 중복 제거,
  authored root와 중첩 AST, T12 한 행, T11 marker와 과거 asOf 제외를
  확인했다. 기존 통과 검사는 새 변경 없는 한 반복하지 않았다.
- package와 `./verify prepare`는 exit0이다. full backend/harness 및
  `./verify scenarios --actual`은 NOT_RUN이며 primary/Haiku 소유다.

중간 RED/환경/fixture 실패 로그도 보존했다. 첫 Testcontainers 실행은
Docker socket 미설정으로 실패했고, 현재 OrbStack socket을 명시해 실제
PG 검증을 마쳤다. 테스트의 물량 변경·보존식 없는 퇴역 fixture가 기존
불변성 검증에 거부돼, 실제 부모/자식/이동 원장을 가진 fixture로 고쳤다.
기존 제품 재고 규칙을 완화하지 않았다.

원래 T09 94/T11 110 assertions, 모든 cases/fixtures/requirements/contracts는
baseline과 byte가 같다. Receipt/trade 응답, Main.java, verify, decisions,
task-status, AGENTS는 수정하지 않았다. 다른 tree/root도 수정하지 않았다.

`raw-receipt-sum100`은 WorkContributions의 cross-Work link-backed attribution을
조회하지만 실제 receipt goal의 direct canonical 입력60+40은 이미 다른
원천에서100을 증명한다. 이 capture가 그 사실을 고정해도 WorkLink 없는
가짜 allocation을 만들지는 않는다. 현재 관찰 source mismatch를 남긴다.
noPO purchase credit0/excess100은 그대로다. 별도 direct-goal contribution
view가 필요하면 관찰 계약 owner가 독립 원천 identity와 의미를 정해야 한다.

`throughout-complete/no-quantity-changing-events`의 expected0은 fixture가
저장한 INITIAL_BALANCE1과 별도 불일치다. movements source를 줄이거나
원행을 숨기지 않았다. 기존 assertion/fixture도 변경하지 않았다.
위 잔여와 두 통합 hook, 원래 scenario의 새 actual 검증이 남아 있으므로
전체 Task/Step 완료를 주장하지 않는다.
