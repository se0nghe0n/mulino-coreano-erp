# Step 1 개발 skills를 round 6과 S4 closure 3에 맞춘다

장소 종류가 없는 fixture나 Accept가 빠진 wire는 업무 oracle를
검증하기 전에 실패한다. QUERY tool에 쓰기 probe를 강제하거나
watcher 창을 group 경계로만 설명해도 올바른 구현을 인수할 수 없다.
Step 2 round 6의 준비·관찰 계약과 S4 정정/정산의 책임 보존 규칙을
개발 skills에 반영한다. 구현·계약·실행 증거의 범위는 구별한다.

- worker는 사용자 Step 1 지정인 GPT-6.1 Sol high다.
- baseline은 `a9051a68d27b090deea2150fb1770943025ac6be`다.
- branch는 `step1r/sync6`, 유일한 쓰기 checkout은
  `/Volumes/VideoStore/Developer/mulino-ontology-step1r-sync6`다.
- 입력은 [AGENTS.md](../../../AGENTS.md),
  [Step 2 round 6](../step2r-round6/README.md)(`89abd2c3`),
  [S4 closure 3](../s4k-closure/README.md)(`3addaefe`),
  [이전 sync](../step1r-sync5/README.md),
  [장소 JSON](../../../contracts/fixture-place-kinds.json)과
  [의미표](../../../contracts/fixture-place-kinds.md),
  [S0 protocol](../../../contracts/mcp/s0-protocol.md),
  [harness guide](../../../verification/harness-guide.md),
  [host guide](../../../verification/host-observation-guide.md)다.
- 소유 범위는 `.agents/skills/`의 개발 skill 세 개와 이 README다.
  coordinator가 통합·결합 checks·Step closure review를 맡는다.

## 항목별 상태

1. FIXED — fixture의 다섯 Place.kind와 내부 보관자 계약을 적는다.
   INTERNAL_STORAGE segment는 같은 조직의 Human/Agent custodian을
   가진다. TRANSIT·CUSTOMER·SUPPLIER·EXTERNAL_PORT는 외부 장소다.
   kind 누락·미선언 kind·baseline.places 불일치·내부 보관자 누락은
   prepare 실패다. 옛 kind를 adapter 기본값으로 수선하지 않는다.
2. FIXED — 어휘 밖 kind는 kindControl=UNRECOGNIZED_PLACE_KIND로
   선언한 반례만 쓴다. UNKNOWN·적격0은 confirmed eligible에 넣지
   않으며 외부 장소의 확정0과 구별한다. C1 반례와 T16/T17 양성
   대조를 연결한다. 제품 실행 PASS를 주장하지 않는다.
3. FIXED — Streamable HTTP는 필수 Accept를 보내고 Origin은
   bad-origin 403 반례에만 둔다. missing-accept 406 반례와 prepare의
   status 고정 검사를 적는다. raw adapter가 header를 보충하지 않는다.
4. FIXED — params가 object/array가 아닌 envelope 오류 -32600과
   object 안의 meta/clientInfo/clientCapabilities 내용 오류 -32602를
   구별한다. mirrored header -32020과 transport의 별도 경계를 유지한다.
5. FIXED — TOOL·BOUND_ACTION·UNBOUND_ACTION의 QUERY 면제는 hash로
   묶인 capability kind·writeCapable=false·itemId 이름을 모두 확인한다.
   readonly ENTITY_SET·COMMAND·RECORD·범용 dispatcher는 probe를 유지한다.
   면제 항목도 열거/hash/allowlist 검사를 받는다.
6. FIXED — passive watcher의 observeFrom은 harness가 요청에 넣는다.
   group 경계와 단독 watcher dispatch 직전 시각을 구별하고 해당 창의
   지속 제출 행만 읽는다. case 직접 지정·누락·경계 불일치·command의
   창 이탈은 거부한다. 실제 watcher adapter 인수는 NOT_RUN이다.
7. FIXED — S4 CURRENT 비충족 복원은 기존 root의 열린 assignment를
   revision·nextAction·nextCheck·basis로 다시 발행한다. 옛 revision의
   면제는 거부하고 같은 basis 재처리는 revision을 바꾸지 않는다.
   유효하게 면제한 같은 잔여는 새 root/follow-up으로 부활시키지 않는다.
   정정 영향 집합은 IMPORTED Work를 제외한다.
8. CROSS_OWNER — FixtureInstaller/native fixture·첫 수령 custodian slot,
   OntologyMcp 오류 불일치·watcher/열거 adapter는 round 6의 요청으로
   남긴다. S4 일반 supersession의 미연결 정산 root와 IMPORTED 직접
   정정의 후속 책임은 S5 DEFERRED다. 이번 변경으로 닫았다고 쓰지 않는다.

## 수정 파일

- 세 `SKILL.md`: 장소·protocol·QUERY 면제·observeFrom·S4 계약을
  찾는 진입 경로를 갱신한다. frontmatter 이름과 discovery는 유지한다.
- `ontology-scenario-testing/references/repository-harness.md`:
  fixture/transport prepare, params 오류, QUERY 면제와 watcher 창을
  적고 Step 3 cross-owner·NOT_RUN을 보존한다.
- `ontology-implementation/references/implementation-contracts.md`:
  장소/보관자 적격성·probe 면제와 S4 복원·면제·IMPORTED 규칙 및
  DEFERRED 후속 책임을 적는다.
- `ontology-agent-implementation/references/protocol-and-intent.md`:
  Accept/Origin 반례와 -32600/-32602/-32020 경계를 적는다.
- `ontology-agent-implementation/references/runtime-and-client.md`:
  observeFrom을 실제 host 요청·창·extractor 계약에 연결한다.

## Checks

지정 worktree에서 Python 3.14.7과 Ruby Psych 5.3.1로 읽기 전용
검사를 실행했다. 아래 10개 명령은 모두 exit0이다.

- `python3 -I verification/cases/check_vocabulary.py --check`:
  VALID, problems 0, knownOpen 34다. 열린 vocabulary는 Step 3 소유다.
- `python3 -I verification/requirements/check_layer_routes.py`:
  VALID, unexplainedGaps/knownOpen/problems 0, runtimeCoverage NOT_RUN이다.
- `python3 -I verification/requirements/check_derived_bindings.py`:
  VALID, files 5, knownOpen/problems 0이다.
- `python3 -I verification/requirements/validate_catalog.py`:
  structure VALID, 41 case·122 oracle·499 observation·26 requirement다.
  runtimeCoverage NOT_RUN, semanticCompleteness REQUIRES_QA_SOURCE_REVIEW다.
  hash-lock된 acceptance-oracles.md는 수정하지 않았다.
- `python3 -I verification/cases/V2/cases_b_invariants.py`: 문제 0이다.
- `python3 -I verification/cases/T08/bind_observations.py --check`:
  CURRENT다.
- `python3 -I verification/cases/V7/bind_observations.py <ID> --check`:
  V4·V6·V7로 각각 실행했고 셋 다 CURRENT다.
- `python3 -I verification/cases/V4/author_review_fixes.py --check`:
  CURRENT다. 생성기를 쓰기 모드로 실행하지 않았다.

추가 구조 검사도 exit0이다.

- Ruby Psych safe_load가 세 frontmatter를 실제 parse했다.
  필수 name/description·허용 key·이름/길이·미완성 scaffold를 검사했고
  세 skill 모두 VALID다. dependency 설치는 하지 않았다.
- inline Python이 개발 skill 전체와 이 README의 상대 링크를 검사했다.
  상대 링크 58개·broken 0이며 실제 skill directory 3개와
  `.claude/skills` symlink
  3개가 정상이다. symlink는 real directory를 직접 가리킨다.
- Gherkin 양식은 Scenario 1개·Feature 0개·단계 6개를 유지한다.
  모든 단계는 ScenarioGlue의 세 문장 형태와 일치한다. 양식은
  수정하지 않았다.
- QUERY 면제 조건과 wire transport 예외를 현재 Java validator의
  queryExempt·wireTransportProblems와 읽기 전용으로 대조했다.
- 변경 8개 파일은 모두 사용자 소유 범위 안이다.
- `git diff --check`: exit0, 공백 오류 0이다.

## 하지 않은 일

Maven·`./verify`·제품 runtime·실모델·client·BTP·규제 실행은 하지
않았다. backend·contracts·verification·제품 skills·다른 worktree도
수정하지 않았다. 입력 기록의 round 6 507 selftests와 S4 backend
513 tests/native S1–S4 PASS는 재실행한 결과가 아니다. 한정 native
custody 증거를 coverage manifest PASS로 바꾸지 않았다.

adversarial review·push·merge·PR 생성도 하지 않았다. coordinator가
commit을 통합한 뒤 결합 checks와 지정 review로 Step closure를
판단한다. 전체 제품 인수와 cross-owner/DEFERRED 항목은 미완료다.
