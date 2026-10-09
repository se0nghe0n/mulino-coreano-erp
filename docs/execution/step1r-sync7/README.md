# Step 1 skills에 sync 6 이후 실행 계약을 반영한다

sync 6의 수령 보관자 미정 문장과 정산 root 생성 금액 비교는 최신
계약과 다르다. 정적 review 반복 지침과 api/fixture/db만 있다는 설명도
2026-10-09 결정 및 S5b 실행 범위를 반영하지 못한다. 기존 문장을
교체하고 규칙별 소유 skill에서 계약 원본을 찾도록 한다.

- worker는 사용자 Step 1 지정인 GPT-6.1 Sol high다.
- baseline은 `41f8d7097b200390a69facc36e6679de64a987c1`이다.
- branch는 `step1r/sync7`, 쓰기 checkout은
  `/Volumes/VideoStore/Developer/mulino-ontology-step1r-sync7`뿐이다.
- 변경은 `.agents/skills/`와 이 디렉터리로 한정한다.
- [AGENTS.md](../../../AGENTS.md), 이전 sync commit `99762666`,
  세 skill과 references, [round 7](../step2r-round7/README.md)부터
  [round 8](../step2r-round8/README.md),
  [round 9](../step2r-round9/README.md),
  [round 10](../step2r-round10/README.md),
  [round 11](../step2r-round11/README.md),
  [round 12](../step2r-round12/README.md),
  [S4 final](../s4-closure-final/README.md),
  [S4l](../s4l-closure/README.md),
  [S5a](../s5a-adapter/README.md), [S5b](../s5b-adapter/README.md)를 읽었다.
- 기계/의미 계약은
  [fixture-place-kinds.json](../../../contracts/fixture-place-kinds.json)과
  [의미표](../../../contracts/fixture-place-kinds.md),
  [execution-preconditions.json](../../../contracts/execution-preconditions.json)과
  [설명](../../../contracts/execution-preconditions.md),
  [intent.schema.json](../../../contracts/intent.schema.json),
  [request-contracts.json](../../../contracts/request-contracts.json),
  [request_contract.py](../../../verification/cases/request_contract.py)다.

## 동기화한 규칙과 소유 파일

아래 경로는 `.agents/skills/` 기준이다. 세 `SKILL.md`도 해당 reference로
가는 진입 문장과 실행 범위 설명을 갱신한다. 계약 전체를 복제하지 않는다.

- 보관의 CONFIRMED/UNCONFIRMED/OUTSIDE 및 어휘 밖 kind UNKNOWN:
  `ontology-implementation/references/implementation-contracts.md`다.
- INTERNAL_STORAGE 직접 수령의 명시 receiving custodian·현재 권한·
  검증 basis 원본·재시도 일치:
  `ontology-implementation/references/implementation-contracts.md`다.
- TRANSIT의 식별된 정확 수량 leaf 수령·부분 split·보관자 승계:
  `ontology-implementation/references/implementation-contracts.md`다.
- custodyControl의 첫 실패·outcome/code·원행 효과0·결과 미사용:
  `ontology-scenario-testing/references/repository-harness.md`다.
- pick 선행·revision 연결·PLACE scope 내 TRANSIT·pick 이후 발생:
  `ontology-implementation/references/implementation-contracts.md`다.
  fixture pick과 pick 전 거부의 작성 검사는 repository-harness.md다.
- passive watcher observeFrom/naturalTickSeconds·NO_TASK 두 tick·종료
  뒤 extractor·완료 schedulerCycles·SCHEDULER_CYCLE_RECORD gate:
  `ontology-agent-implementation/references/runtime-and-client.md`다.
  prepare·관찰 규칙은 repository-harness.md에서 연결한다.
- 설치 행 recordedAt≤시작 asOf·조회 knownAt·늦게 알려진 근거:
  `ontology-scenario-testing/references/repository-harness.md`다.
- reserve/replace의 segment 구간·판매 line 이중 예약 금지:
  `ontology-implementation/references/implementation-contracts.md`다.
  prepare의 reserveCapacityProblems는 repository-harness.md다.
- 모든 명령 intent schema·case 작성 slot별 provenance·APPROVED_DEFAULT
  추론 금지·action.harness 분리·조회 contracted key·KNOWN_OPEN:
  `ontology-agent-implementation/references/protocol-and-intent.md`다.
  prepare의 request-contracts check는 repository-harness.md다.
- grant 차원 all-of·같은 대상의 위임자 권한·probe 한정 차원별 분리:
  `ontology-agent-implementation/references/runtime-and-client.md`다.
- 실제 scenarios 실행·inventory.py·run 간 첫 실패 비교·원인 triage·
  oracle 보존: `ontology-scenario-testing/references/repository-harness.md`다.
  `ontology-implementation/references/stage-gates.md`의 review 시점도
  최신 결정으로 교체한다.
- SETTLEMENT_DIFFERENCE root·reissueOpen·동일 잔여의 실행 시 coverage
  suffix·IMPORTED 무효화만 제외하고 정정 책임 보존:
  `ontology-implementation/references/implementation-contracts.md`다.
- Accept 두 media type 비교와 S5b 실제 route/RESULT_REVISION 범위:
  protocol-and-intent.md와 repository-harness.md의 낡은 문장을 교체한다.

## 모순과 남은 범위

모든 요청 규칙은 소유 skill에 배치했다. 다음 충돌은 최신 근거의
우선순위를 적고 원본 소유 경로는 수정하지 않았다.

1. round 12·request-contracts.json·request_contract.py는 PER_DIMENSION을
   설치하라고 하거나 conformance 기본값으로 쓴다. AGENTS.md의
   2026-10-09 결정과 S5b는 기본 실행 all-of, 분리는 probe 한정이다.
   skill은 후자를 따른다. fixture 및 계약 변경은 다른 worker 소유다.
2. "IMPORTED Works only invalidate"는 그대로 쓰면 반대 의미다.
   S4l과 현재 AssessmentCorrectionImpact는 IMPORTED의 무효화만
   건너뛰고 정정 책임은 보존한다. sync 6의 영향 집합 전체 제외와
   FOLLOWUP_REVIEW 생성 금지 문장을 이 규칙으로 교체했다.
3. QualityEligibility의 enum은 UNRECOGNIZED도 있어 네 값이다.
   요청한 세 보관 판단을 설명하면서 어휘 밖 kind의 UNKNOWN과
   PLACE_KIND_UNRECOGNIZED를 별도로 남겼다.
4. execution-preconditions의 과거 요청 asOf fallback과 cargoPlaceId
   mapping 요청은 round 12 intent envelope 및 S5b 기본 실행의 무수선
   원칙과 함께 읽어야 한다. skill은 명령의 제품 시계와 명시 slot,
   corpus/제품 이름 구별을 적고 일반 adapter의 임의 수선을 금지한다.
5. inventory의 NOT_IMPLEMENTED는 status이며 TEST/ADAPTER/PRODUCT는
   classification이다. 원인과 실행 상태를 하나의 enum으로 합치지
   않고, 자동 분류가 추정임을 명시했다.

S4 final의 R5-1(비CURRENT 면제 coverage), R5-2(basis 길이),
R5-3(IMPORTED 의무 불가 상태), relink 없는 정산 및 canonical 재도출
backlog는 그대로 남긴다. ManagementCoverage 단일 차원 통과는 최신
결정대로 all-of 수정 대상이다. 내부 보관자 미확인/외부 segment의
별도 선언 반례, QUERY 면제 raw surface 검증과 나머지 round 11
backlog를 해소했다고 쓰지 않는다.

## 실행한 checks

지정 worktree에서 실행했다. 아래 명령과 구조 검사는 모두 exit0이다.

- `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh` 뒤
  `$MULINO_SLOT ./verify prepare`: PREPARED다. Java 21.0.5, Maven
  3.9.16, baseline HEAD의 skill 수정 tree(workingTreeDirty=true)에서
  실행했다. 41 case·802 subcase·24,034 assertion, preparationProblems 0,
  caseAssetChecks 12개 PASS(request-contracts 포함), knownOpenGaps 122,
  runtimeGates 2(regulatory·SCHEDULER_CYCLE_RECORD)다. runtimeStatus는
  NOT_RUN, gateComplete=false다. 출력 report는
  `verification/harness/target/evidence/prepare.json`이다.
- `python3 -IB verification/cases/check_request_contracts.py`:
  VALID, 41 cases·374 fixtures, problems 0, knownOpen 800건/67줄이다.
- `python3 -IB -m unittest discover -s verification/cases`:
  28 tests OK다(request-contract tests 22개 포함).
- `python3 -IB verification/requirements/validate_catalog.py`:
  structure VALID, oracle 122·observation 499·case 41·requirement 26이다.
  runtimeCoverage NOT_RUN과 QA source review 미완료를 유지한다.
- `python3 -IB verification/requirements/check_layer_routes.py`:
  VALID, unexplainedGaps/knownOpen/problems 0이다.
- `python3 -IB verification/requirements/check_derived_bindings.py`:
  VALID, 5 files, knownOpen/problems 0이다.
- Ruby Psych safe_load: frontmatter 3개가 parse되며 name/description,
  이름과 directory 일치, 허용 key, description 길이가 유효하다.
- inline Python: 세 실제 skill directory와 discovery symlink 3개,
  상대 Markdown link 전부 및 새 파일 참조가 존재한다. 양식은 Scenario
  1개·Feature 0개·지원 단계 6개다. 상대 link는 skill 84개·README
  18개, 합계 102개이며 broken 0이다.
- `git diff --check`: 공백 오류0이다. 변경 파일은 모두 소유 범위 안이다.

prepare가 이미 vocabulary·binding·생성기 재현·cases-b-invariants를
검사하므로 같은 검사를 개별로 반복하지 않았다. 별도 개발 skill
validator는 `.agents/`에 없고, verification/skills-tests는 제품 host
discovery/loading 인수 계약으로 개발 skill 구조 검사를 대신하지 않는다.

## 실행하지 않은 것

`./verify scenarios --actual`, backend/native runtime, harness Java test
전체, 실모델/client/BTP/규제 실행은 하지 않았다. 이 작업은 skill sync와
준비 검사이며 S5b 실행 결과를 이번 worker의 PASS로 주장하지 않는다.
prepare가 생성한 ignored target artifact 외 verification/backend/fixture/
contract를 수정하지 않았다. 다른 worktree·main checkout도 수정하지
않았다. 새 subagent, 정적 adversarial review, push·merge·PR·알림은
하지 않았다. coordinator가 commit 통합과 결합 실행을 맡는다.
