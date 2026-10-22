# V5/T26 복구 목표의 증거 계약

사용자 Step2 runtime 실패 중 정상 활성화와 목표 판정의 소유 범위를
수정한다. baseline은 `082c703e9cb8198c91ff1507b7bcaef442d31a3a`,
branch는 `fix/resume-goal-evaluation`이다. T3 isolated worktree
`/Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/t3-943824b5`에서
작업한다. 상위가 최종 통합과 전체 gate를 판정한다.

## 의미와 최소 변경

계획 §5의 `STATE_AT`은 평가시점의 상태를 판정하는 모드다. 증거의
출처가 현재 segment여야 한다는 뜻은 아니다. §5의 증거 기반 판정과
§7.2의 원천·동일성 대조, §13.2 V5의 대기 책임을 함께 적용한다.

기존 `quantitySum / VERIFIED_DISTINCT` typed predicate를 재사용한다.
새 연산자나 새 모드는 만들지 않는다. `STATE_AT`과 이 selector의
조합에서는 `evaluationAt`에 유효한 canonical occurrence를 읽는다.
현재 재고20만 있거나 EV-READY의 claim만 있으면 `UNVERIFIED`다.
원문 blob·검증·source inbox·claim·event가 모두 유효해야 수량으로
인정한다. 동시 다른 문서가 같은 물리 범위를 보고해도 한 번만 센다.

- STATE_AT 증거는 Work에 귀속되지 않은 재고 관측도 읽는다. 품목,
  장소, LOT이 지정되면 해당 범위와 알려진 시각을 적용한다. 누적
  이행량의 기존 Work/명시적 contribution 귀속 규칙은 유지한다.
- `evidenceSource`는 sourceNamespace, externalEventId, sourceVersion의
  세 문자열을 모두 요구한다. fixture는 synthetic-warehouse의
  EV-READY/1을 지정한다. 다른 사건이나 source version은 대체 증거가
  아니다. 선택은 실제 verification→claim→event 관계로 확인한다.
- `evaluationAt`은 fixture의 dueAt와 같은 시점으로 명시한다. 이는
  복구 시점에 증거를 확인하는 테스트 선택이며 서버 기본값이 아니다.
  평가시점이 아직 미래면 미확인으로 남는다. canonical effectiveUntil은
  배타적이다. 뒤늦게 알려진 검증은 knownAt에 따라 판정한다.
- `action=PHYSICAL`, `includeReserved=true`는 예약을 공제하지 않는
  실물 관측 범위다. canonical 검증으로 판매 적격이나 미예약 수량을
  주장할 수 없으므로 다른 action/예약 제외 조합은 효과 없이 보류한다.
- `deadlineInclusive` boolean을 보존한다. true일 때 기한과 같은
  instant는 아직 기한 위반이 아니며, false일 때 기한 직전까지의
  사실을 쓴다. 미지정은 기존 동작을 유지한다.
- targetQuantity의 추가 predicate도 발행 template의 aggregation을
  따른다. STATE_AT이라는 이유로 CURRENT_STATE로 덮어쓰지 않는다.
- fixture의 설명형 evaluator-v1은 실제 실행 가능한 core-v1로 명시
  전환한다. typed template 이름 verified-ready와 endpoint VERIFIED를
  고정하고 같은 selector를 wait resumePredicate에도 사용한다.
  미지원 evaluator를 서버가 자동으로 바꾸지는 않는다.

`baseline.definitionContract.goalTemplate`을 installer helper가 발행
내용에 그대로 추가한다. 겹친 endpoint/mode나 evaluator 불일치는
거부한다. 관측·검증·완료 상태를 seed하지 않는다. 별도 fixture
PolicyVersion으로 synthetic-evidence-v1을 명시한다.

createWork는 이미 활성화와 첫 책임 생성을 수행하므로 후속 중복
activateWork를 제거한다. revision 참조와 비상 복구의 마지막 확정
command 참조만 create로 연결한다. 수량·효과·책임 expected는 낮추지
않는다. 원본 D/T/C/V oracle와 부정 사례는 유지한다.

## 검증과 제한

Java21을 PATH 앞에 지정하고 backend에서 npm ci를 수행했다.
검증은 실제 PostgreSQL과 로컬 생성 인증 context만 사용한다.
실모델·BTP·push·PR·외부 메시지는 실행하지 않는다. 재위임하지 않는다.
Astra의 Ultrafast 지원은 live catalog에서 확인했다. child 실행의
실제 service tier는 상위의 명시적 dispatch 설정으로 확인해야 한다.

검사 결과와 clean commit 후 실제 선택 실행 결과를 아래에 추가한다.

### 결정적 검사

- `./mvnw -q -f backend/pom.xml
  -Dtest=GoalInputTest,TypedPredicateEvaluatorTest,AssessmentPostgresTest test`
  는 25 tests, failure/error/skip 0으로 통과했다. AssessmentPostgresTest의
  10개에는 실제 원문·event·claim 등록, matchSourceIdentity 대조와
  canonical link, UNVERIFIED→SATISFIED, 재고 이동0이 포함된다.
  로그는 `/tmp/recovery-backend-final.log`, 원본 JUnit은
  `backend/target/surefire-reports/TEST-com.mulino.*.xml`이다.
- `./mvnw -q -f verification/harness/pom.xml
  -Dtest=RecoveryGoalDefinitionTest test`는 2 tests 통과다.
- `python3 -m unittest discover -s verification/cases
  -p test_resume_contracts.py -q`는 12 tests 통과다.
- `./verify prepare`는 PREPARED, 41 cases / 802 subcases /
  24,034 assertions, 12 asset checks 통과다. 실제 runtime PASS는 아니다.
- 생성기 재실행은 `changes {}`다. 기존 V5 91개, T26 710개 assertion의
  수량·효과·책임 기대는 유지한다. 중복 activate 제거에 따른 마지막
  command 참조 1개와 복원 evaluator 이름 4개만 새 계약에 대응한다.
  restore fault 2개도 같은 core-v1을 가리킨다. evaluator 누락 negative를
  없애지 않는다. definitionArtifacts의 v1 pin도 같은 이름을 쓴다.

초기 test 작성 중 immutable Work identity를 바꾸려던 test setup은
새 Work/Goal 생성으로 고쳤다. snapshot에 predicate 문자열 전체가
들어 있다고 가정한 test도 실제 immutable definition ID/hash와
발행 내용의 selector를 대조하도록 고쳤다. 제품 guard를 완화하지 않았다.
