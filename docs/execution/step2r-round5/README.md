# Step 2 재검토 5라운드: closure review 2 확인 지적 처리 기록

Step 2 closure review 2(Claude Opus xhigh, Astra low)의 skeptic 검증을
거친 지적을 이번 round에서 처리한 기록이다. 입력은
`step2-closure-2-opus-xhigh-verdicts.json`(11건)과
`step2-closure-2-astra-low-verdicts.json`(8건)이다. 기준 tag
`step2r5-baseline`(`a89885f8`), branch `step2r/round5`, worktree
`/Volumes/VideoStore/Developer/mulino-ontology-step2r-round5`다. 작업 모델은
사용자 Step 2 지정대로 Claude Opus high다.

손댄 범위: `verification/cases/{T26,V4,V7}/**`, harness의 `actual/` 밖
Java와 test, `verification/harness-guide.md`,
`verification/host-observation-guide.md`, `verification/mcp-tests/`,
`contracts/acceptance-{case,observation,host-observation}.schema.json`,
`contracts/mcp/s0-protocol.md`. 손대지 않은 범위: `backend/**`,
`verification/harness/src/main/java/org/mulino/verification/actual/**`,
`.agents/skills/**`(Step 1). registry 변경 없음(41 case, 799 subcase).

## 항목별 처리

### 필수(P2)

| # | 지적 | 판정 | 내용 |
|---|---|---|---|
| 1 | opus[0] T26 terminal snapshot 관찰 5건이 RESULT_REVISION 재작성 때문에 만족 불가 | FIXED | `CaseRunner.observationRequest`는 `awaitRuntimeTask` control의 `/data/hostObservation/runtimeTask/snapshot/id`에 묶인 snapshotRef를 새 read mode `RUNTIME_TASK_SNAPSHOT`으로 보낸다. `snapshotSource`에는 `operation=awaitRuntimeTask`와 해석된 `schedulerId`·`taskId`·`invocationHandle`·`scope`만 있고 snapshot id는 없다. observer는 `snapshot.runtimeTaskSnapshot={schedulerId, taskId, invocationHandle, snapshotId, artifactRef, sha256}`을 낸다. harness는 이를 await 결과의 `runtimeTask`, 보관한 발급 id, artifact bytes의 SHA-256과 대조한다. artifact가 이 관찰의 `artifactRefs`에 있어야 하고 DB read가 task `completedAt`보다 이르면 안 된다. API revision은 그대로 숨긴다(RESULT_REVISION). `ContractValidator.snapshotRefProblems`(prepare 연결)는 `$result` snapshotRef가 invoke/query/start의 `/response/snapshotRevision`이거나 await의 runtime snapshot id일 때만 받는다. 현재 41 case 문제 0. 회귀: `StepTwoRoundFiveRegressionTest.runtimeTaskSnapshotObserveIsSatisfiableAndBoundToTheHostArtifact`(awaitRuntimeTask에 묶인 observe가 PASS하고 mutant 10종이 각 이유로 거부됨), `everyT26RuntimeSnapshotObserveUsesTheRuntimeReadModeAndNoOtherPointerIsAccepted` |
| 2 | opus[1] orphan-intake-autonomous-loop가 장애·seed 단계에 자유 loop scheduler를 띄움(+lot-expiry 재확인) | FIXED | `T26/author_review_fixes.py`가 세 자율 loop subcase에서 loop process(scheduler, due-sweeper)를 group 전까지 한 번도 띄우지 않는다. api·worker만 먼저 띄우고, clock 전진 뒤 api·worker를 group 밖에서 다시 시작한다. lot-expiry 재확인: round 4에서는 scheduler가 create·activate·reserve·before-db 동안 떠 있었다. LOT 경계(09:00:20)는 clock 전진 뒤라 그 단계의 sweep 대상은 없었지만, 자율 fixture의 scheduler가 activation 쪽 due 업무를 먼저 처리할 수 있었다. 같은 규칙으로 닫았다. `ContractValidator.runtimeProfileProblems`는 group이 시작하는 process가 installFixture 뒤 group 앞의 어떤 비-lifecycle action(fault·seed·조회·관찰·clock)에서든 실행 중이면 거부한다. 회귀: `autonomousLoopProcessesStayStoppedUntilTheWatcherGroup`(round 4 모양을 세 subcase 모두에서 거부하고 옛 규칙만으로는 통과했음을 확인) |
| 3 | opus[3] V4 mcp-batch가 Mcp-Name 없이 -32600만 기대 | FIXED | 선택: `s0-protocol.md`에 우선순위를 적었다. 본문 envelope 오류(-32700, batch 배열·object 아님·형식 위반 -32600)가 mirrored header 오류(-32020)보다 먼저다. batch 배열에는 header와 비교할 단일 method/name이 없기 때문이다. transport 거부(401/403/406/415)와 다른 행 사이의 순서는 정하지 않았다. T20 `wire-method-mismatch`는 단일 object 본문이라 envelope이 유효하므로 -32020 그대로이고 모순이 없다. backend `PlatformMcp`·`OntologyMcp` 소스도 -32600을 -32020보다 먼저 검사한다(소스 확인만 했고 실행 인수는 아니다). V4 생성기의 `mcp-batch-invalid-request` 설명에 이 근거를 더했다 |

### 권장(P3)

| # | 지적 | 판정 | 내용·소유자 |
|---|---|---|---|
| 4 | opus[2] api/scheduler/worker start가 30초 watcher 창 안에서 직렬 실행 | FIXED | api·worker start는 group 밖으로 뺐다. group의 non-watcher branch는 loop process start 하나씩만 가진다(lot-expiry는 scheduler·due-sweeper 두 branch). prepare는 branch 하나에 start가 둘 이상이면 거부한다. 창 기준은 아래 5와 같은 관찰 경계로 통일했다. `autonomous-within-30s` baseline은 group의 `/data/observationBoundaryAt`이다. 새 `autonomous-after-loop-start`는 loop start command `startedAt` 하한과 그 뒤 30초를 본다 |
| 5 | astra[1] watcher/start race | FIXED | pre-group boundary 방식. `CaseRunner.parallel`은 branch 0 첫 action이 수동 watcher인 group에서 어떤 branch도 제출하기 전에 `Instant.now()`를 잡아 결과 `data.observationBoundaryAt`에 남기고 watcher 검증에 넘긴다. `HostObservationValidator.naturalTick`은 창을 그 경계부터 잰다. watcher command가 경계보다 먼저 시작했다고 주장하면 거부한다. SUBMITTED의 submittedAt은 경계부터 command 종료 사이다. extractor는 scheduler의 지속 제출 기록을 읽으므로 watcher thread가 늦게 떠도 경계 뒤 제출을 잃지 않는다. 남은 한계: 경계+30초 뒤까지 늦는 watcher는 NO_TASK로 fail-closed이고, 경계(harness 시계)와 submittedAt(scheduler 시계)은 LOCAL 같은 host를 전제한다. 회귀: `passiveWindowStartsAtThePreGroupBoundary`, `parallelWatcherGroupRecordsItsObservationBoundary` |
| 6 | astra[0]/[7] write-surface 완전성이 extractor의 applicableTargets를 믿음 | FIXED | `HostObservationValidator.PROBE_POLICY`(kind→probe class)와 `KIND_SURFACES`(kind→surface)를 정의했다. ENTITY_SET→entity 쓰기 10종, BOUND/UNBOUND_ACTION→자기 call과 BATCH_CHANGESET, FUNCTION→없음, TOOL→MCP_TOOL_CALL, WORKER_HANDLER→WORKER_HANDLER_SUBMIT, MANAGEMENT_ENDPOINT→MANAGEMENT_ENDPOINT_WRITE. 적용 여부는 writeCapable과 무관하다. 요청한 class마다 적용 항목의 probe 행이 있어야 하고 `applicableTargets`는 harness가 다시 센 값과 같아야 한다. 정책에 없는 kind·맞지 않는 surface는 거부한다. schema의 `surfaceItems.kind`는 enum이다. guide에 표를 더했다. 회귀: `writeSurfaceCoverageIsRecomputedFromTheApplicabilityPolicy`(0/0 complete=true, 과소 보고, 미지 kind, 잘못된 surface, readonly entity set probe 누락). 남은 신뢰: 항목 `kind` 자체는 extractor가 적는다 |
| 7 | opus[4] T25 checkoutCommit/checkoutDirty를 harness git 상태와 비교 안 함 | FIXED | 제품 실행(`requireActualHost=true`)의 PREPARATION verifyCoverage에서 `checkoutMatchesHarness`가 두 값을 harness가 같은 저장소에서 읽은 `git rev-parse HEAD`·`git status --porcelain`과 비교한다. selftest 표본은 고정 commit이라 비교하지 않는다. 회귀: `preparationCheckoutIsComparedWithTheHarnessGitState`(helper 단위). 제품 경로 자체는 Step 3 verifyCoverage adapter가 생겨야 실행된다 |
| 8 | opus[7]/astra[3] V7 primary가 observer가 고른 `/data/data` 파생값 | FIXED | V7 세 subcase의 `new-effect-quantity0`을 `after` movements 원행의 고정 filter(`kind=DISPATCH`, `commandIdempotencyKey=new20`) `sumEquals 0 BOX`로 바꿨다. 단위는 active segment 원행에서 읽는다. 보조 `no-new-dispatch-rows`는 전후(restart는 `prior-committed` 대비) DISPATCH 원행 전체 불변을 본다. `CatalogLinkValidator.fixedQuantityAssertion`은 source·baseline·unitSource·baselineUnitSource 중 하나라도 `/data/data/`이면 고정 수량 primary로 세지 않는다. 다른 111개 quantity observation은 이미 원행/응답 primary가 있어 영향이 없다. 회귀: `fixedQuantityPrimariesReadRowsNotObserverDerivations`, `AuthorityAssertionsTest` 갱신. `bind_observations.py V7`로 bindings를 다시 만들었다 |
| 9 | opus[9]/astra[5] observer가 고르는 derivation 의미(보조 assertion) | DEFERRED | primary 쪽은 8로 닫혔다. 남은 `/data/data/` pointer 104줄(C4·E1·E2·T17·T18·T23·T26·V2·V8)은 보조 assertion이며 derivation filter를 case가 고정하지 않는다. case 쪽 `observation.derivations` 선언과 정확 비교는 그 9개 case의 의도 filter를 case마다 정해야 해서 이번 round 범위를 넘는다. 소유자: Step 2 tests worker(Claude Opus high), 다음 Step 2 round. 해당 case 생성기(T23/V8 `platform-tests`, T26 post-processor 등)와 함께 고친다 |
| 10 | opus[10]/astra[6] 생성기 재현 test가 post-processor fixed point만 증명 | DEFERRED(문서는 FIXED) | `verification/mcp-tests/test_generators_reproduce.py` docstring의 "hand edit fails" 주장을 고쳤다. T01/T20/T25만 파생이고 C3/T26은 fixed point만 증명하며 비소유 부분의 손 편집은 남는다고 적었다. 비소유 부분 hash 고정(C3·T26·V4)은 소유 경계 정의가 post-processor마다 달라 미뤘다. 소유자: Step 2 tests worker, 다음 Step 2 round |
| 11 | opus[8]/astra[4] T25 verifier 계약 field를 assemble.py가 내지 않음 | DEFERRED | `assemble.py`는 coverage manifest를 만들 뿐 verifyCoverage host 출력(input.snapshotKind/path/sha256, currentExecution, mutatedInput, CURRENT_EXECUTION link)을 만들지 않는다. 그 출력을 만드는 verifyCoverage host adapter는 Step 3 actual 소유다. T25는 그때까지 NOT_RUN이고 거짓 PASS는 없다. 소유자: Step 3 actual/host adapter 담당(Claude Opus medium). adapter가 assemble.py를 감싸 이 field를 내고 `HostObservationValidator`를 통과하는 test를 같이 둔다 |
| 12 | opus[6]/astra[2] backend build identity가 선언값 | DEFERRED | receipt는 계속 `buildIdentity.source=DECLARED_ACTUAL_BUILD_COMMIT`이다. 관찰된 build identity에는 backend build-info endpoint 또는 `--actual` 경로가 직접 빌드·기동하는 방식이 필요하다. 소유자: Step 3 backend + `ExecutionReceiptProducer`(Step 3 통합 담당) |
| 13 | opus[5] RESIDUAL confirmedNew-3 PARTIAL | FIXED | 남은 원인이 2(opus[1])였다. 2와 4로 닫았다 |

## commit

- `3016ef32` fix(검증): Step 2 closure review 2의 확인된 P2 3건과 P3 일부를 닫는다
- 이 README: docs commit

## 바뀐 파일

- `verification/harness/src/main/java/org/mulino/verification/CaseRunner.java`:
  RUNTIME_TASK_SNAPSHOT 요청·검증, 관찰 경계 기록.
- `.../ContractValidator.java`: `snapshotRefProblems`, loop process 실행
  금지와 branch당 start 하나 규칙.
- `.../HostObservationValidator.java`: 관찰 경계 overload, 쓰기 면 적용
  정책, PREPARATION checkout 비교.
- `.../CatalogLinkValidator.java`: `/data/data` primary 거부.
- `.../Main.java`: prepare에 `snapshotRefProblems` 연결.
- `contracts/acceptance-observation.schema.json`,
  `contracts/acceptance-host-observation.schema.json`,
  `contracts/acceptance-case.schema.json`(설명), `contracts/mcp/s0-protocol.md`.
- `verification/cases/T26/{author_review_fixes.py,case.json,scenario.feature,oracle-bindings.json,README.md}`.
- `verification/cases/V4/{author_review_fixes.py,case.json,scenario.feature,observation-bindings.json}`.
- `verification/cases/V7/{case.json,scenario.feature,observation-bindings.json,README.md}`.
- `verification/harness-guide.md`, `verification/host-observation-guide.md`,
  `verification/mcp-tests/test_generators_reproduce.py`.
- test: `StepTwoRoundFiveRegressionTest`(신규 8건),
  `tickScheduler-submitted-rows.json`(신규 표본), `HostObservationValidatorTest`·
  `StepTwoRoundFourRegressionTest`·`RuntimeAssertionTest`·
  `AuthorityAssertionsTest` 갱신.

## 실행한 checks

환경은 Java 21(zulu 21.0.5), Python 3.14, `env.sh`, Maven·verify는
`$MULINO_SLOT`로 한 번에 하나씩 실행했다. 아래 결과는 fix commit
`3016ef32`의 clean tree(`workingTreeDirty=false`)에서 얻었다. 이 README는
그 뒤 docs commit으로만 더했다.

| 명령 | exit | 결과 |
|---|---|---|
| `./verify harness` | 0 | 501 tests, failure/error/skip 0. 기준 493 대비 +8(`StepTwoRoundFiveRegressionTest` 신규 8건). 기존 4개 test는 바뀐 계약에 맞춰 갱신했다(`HostObservationValidatorTest` 메시지 1, `StepTwoRoundFourRegressionTest` running mutant, `RuntimeAssertionTest` group·기준, `AuthorityAssertionsTest` V7 primary) |
| `./verify prepare` | 0 | PREPARED, codeCommit `3016ef32`, workingTreeDirty=false, 41/799/23962(기준 23956: T26 +3 `autonomous-after-loop-start`, V7 +3 `no-new-dispatch-rows`), preparationProblems 0, caseAssetChecks 11개 PASS, knownOpenGaps 34(전부 vocabulary), runtimeGates 1(regulatory) |
| `./verify coverage` | 2 | NOT_RUN, preparationStatus PREPARED, runtimeStatus NOT_RUN, gateComplete=false, preparationProblems 0, coverageProblems 0, knownOpenGaps 34, caseProfiles 3024, observations 499 |
| `python3 verification/coverage/validate.py` | 0 | VALID, runtimeStatus NOT_RUN, gateComplete=false |
| `./verify contract-red` T26 V4 V7 | 1 | expected=discovered=started=NOT_IMPLEMENTED=122(T26 25, V4 93, V7 4), skipped 0 |
| `python3 -m unittest discover -s verification/coverage -p 'test_*.py'` | 0 | 68 OK(284s). fix commit 직전 같은 내용의 tree에서 실행 |
| `python3 -m unittest discover -s verification/requirements -p 'test_*.py'` | 0 | 37 OK(`test_case_generators_reproduce` 포함). commit 직전 같은 내용 |
| `python3 -m unittest discover -s verification/mcp-tests -p 'test_*.py'` | 0 | 3 OK(생성기 재현). commit 직전 같은 내용 |
| `python3 -m unittest discover -s verification/cases -p test_check_vocabulary.py` | 0 | 6 OK. commit 직전 같은 내용 |
| `verification/model-binding/run selftest` | 0 | 61 tests OK |
| `verification/model-binding/run prepare` | 0 | PREPARED |
| `python3 -I verification/requirements/validate_catalog.py` | 0 | structure VALID, 122 oracle·499 observation·41 case·26 requirement |
| `python3 -I verification/cases/V2/cases_b_invariants.py` | 0 | 문제 0 |
| `python3 -I verification/cases/check_vocabulary.py --check` | 0 | VALID, problems 0, knownOpen 34 |
| `python3 -I verification/requirements/check_layer_routes.py` | 0 | VALID, unexplained 0, KNOWN_OPEN 0 |
| `python3 -I verification/requirements/check_derived_bindings.py` | 0 | VALID, 5 files, KNOWN_OPEN 0 |
| `python3 -I verification/cases/T08/bind_observations.py --check` | 0 | CURRENT |
| `python3 -I verification/cases/V7/bind_observations.py V4\|V6\|V7 --check` | 0 | 셋 다 CURRENT(V7은 이번에 다시 생성) |
| `python3 -I verification/cases/V4/author_review_fixes.py --check` | 0 | CURRENT |
| 생성기 제자리 재실행(author_cases.py, C3 author_prerequisites.py, T06 author_contracts.py, platform-tests build_cases.py, T26·V4 author_review_fixes.py, model-binding generate.py) | 0 | 실행 전후 `git diff --stat` 같음(drift 없음). T26 생성기는 두 번 연속 실행해 bytes가 같았다(fixed point) |

NOT_RUN: `./verify regulatory`, 이번에 손대지 않은 case의 `contract-red`,
`--actual`·제품 runtime. coordinator가 통합 때 실행한다.

## cross-owner 요청

- Step 3 actual(`verification/harness/src/main/java/org/mulino/verification/actual/**`):
  `ObserverSnapshot.readMode`는 `RUNTIME_TASK_SNAPSHOT`을 지금
  RESULT_REVISION 문구의 `UnsupportedOperationException`으로 NOT_IMPLEMENTED
  처리한다. host snapshot artifact를 task identity로 찾아
  `snapshot.runtimeTaskSnapshot`을 내도록 구현하고 문구를 바로잡는다.
  수동 tick host adapter는 관찰 경계부터 scheduler의 지속 제출 기록을
  읽는다. verifyCoverage adapter는 `checkoutCommit`·`checkoutDirty`를 자기
  checkout에서 읽고 T25 출력 field를 낸다. enumerateWriteSurface extractor는
  schema enum의 `kind`를 정직하게 적는다.
- Step 3 backend: MCP envelope(-32600)을 mirrored header(-32020)보다 먼저
  검사하는 현재 순서를 유지한다. 자율 loop subcase는 case가 start하기 전
  scheduler·due-sweeper가 떠 있지 않다고 전제한다(이미 실행 중이면 start
  lifecycle이 거부한다). backend build-info endpoint(항목 12).
- Step 1 skill 소유자(GPT-6.1 Sol high):
  `.agents/skills/ontology-scenario-testing/references/repository-harness.md`가
  snapshotRef를 RESULT_REVISION 하나로만 설명하고, 자율 loop를 "clock
  advance 전 stop"과 "watcher가 늦으면 NO_TASK"로 설명한다. RUNTIME_TASK_SNAPSHOT,
  loop process를 group 전까지 띄우지 않는 규칙, branch당 start 하나, 관찰
  경계, 쓰기 면 적용 정책, PREPARATION checkout 비교, `/data/data` primary
  금지를 반영해야 한다.
- coordinator: 기존 `lot-expiry-no-event` 계열 base subcase는 시작한 적
  없는 due-sweeper를 `stop-sweeper`로 멈춘다. lifecycle 검사상 그 stop은
  SUCCEEDED terminal일 수 없고 FAILED로 기록된다. 이를 단언하는 assertion이
  없어 판정에는 영향이 없지만 의도가 암묵 실행 sweeper라면 case 소유자가
  명시해야 한다. 이번 round에서는 바꾸지 않았다.

## 하지 않은 것

- 제품 실행, `--actual`, `./verify regulatory`, 이번에 손대지 않은 case의
  contract-red는 하지 않았다. 새 assertion과 새 read mode는 모두 NOT_RUN이다.
- 항목 9–12는 위 소유자에게 남겼다.
