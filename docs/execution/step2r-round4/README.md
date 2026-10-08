# Step 2 재검토 4라운드: round 3 교차 소유자 잔여 항목 처리 기록

round 3(cases3·harness3)이 다른 소유자에게 넘긴 8개 항목을 이번
round에서 처리한 기록이다. 기준 tag `step2r4-baseline`(`3023e0fa`),
branch는 `step2r/round4`, worktree는
`/Volumes/VideoStore/Developer/mulino-ontology-step2r-round4`다.

이전 worker가 실행 도중 API 중단(약 15:31 KST)으로 종료되어 uncommitted
편집을 남겼다. 이 worker는 그 내용을 검토하고 유지한 뒤 문서를 마무리하고
checks를 실행해 두 commit으로 분리했다.

손댄 범위: `verification/cases/**`, `verification/coverage/**`,
`verification/requirements/**`, `verification/mcp-tests/**`, harness의
`actual/` 밖 Java와 test, `verification/harness-guide.md`,
`verification/host-observation-guide.md`,
`contracts/acceptance-fixture.schema.json`,
`contracts/acceptance-host-observation.schema.json`,
`contracts/mcp/s0-protocol.md`. 손대지 않은 범위: `backend/**`,
`verification/harness/src/main/java/org/mulino/verification/actual/**`
(Step 3). registry 변경 없음(subcase 799 그대로, 새 case ID 없음).

## 항목별 처리

| # | 항목 | 판정 | 내용 | commit |
|---|---|---|---|---|
| 1 | check_vocabulary를 prepare gate로 연결 | DONE | `check_vocabulary.py`를 `review()`로 분리해 PENDING 34건을 owner가 있는 KNOWN_OPEN 줄로 출력한다. `PreparationAssetChecks`에 caseAssetCheck `vocabulary`(--check, fail-closed)를 더한다. KNOWN_OPEN 줄은 prepare.json `knownOpenGaps`로 가고 assembler가 같은 `review()`를 호출해 manifest `knownOpenGaps`에 넣는다. `validate_saved`가 재대조한다 | `afbce62d` |
| 2 | harness-guide "독립 observer의 논리 원행 계약" 절 | DONE | `audit`/`queryAudit` 계약(presence ALWAYS/CONDITIONAL/PENDING, where는 ALWAYS만, outcome과 errorCode 구분, retiredNames)과 V2 promiseCoverage 파생 규칙(EXECUTABLE_ALLOCATION/SHORTAGE_OBLIGATION, 원행 값 복사, 부족 의무 원행 대조)을 `harness-guide.md`에 옮긴다. `ContractValidator.auditFieldProblems`는 공개되지 않은 감사 source·field·where(ALWAYS 아님)를 prepare 문제로 만든다. `V2/race-observation-contract.md`는 그 절을 가리킨다 | `afbce62d` |
| 3 | fixture `baseline.runtimeProfile` 정의와 검사 | DONE | fixture schema에 `baseline.runtimeProfile`을 더한다(두 flag 필수, true/true·false/false만 허용). `ContractValidator.runtimeProfileProblems`는 harness tick과 수동 watcher 혼용, profile 불일치, 자율 loop 패턴 위반(watcher가 top-level parallel branch 0 첫 action이 아님, 다른 branch가 미리 멈춘 process start 외의 일을 함, restart 사용)을 거부한다. `host-observation-guide.md`에 "runtimeProfile과 자율 loop 패턴" 절을 더한다. parallel은 barrier가 아니며 실패는 fail-closed다 | `afbce62d` |
| 4 | verifyCoverage PREPARATION input commit·clean field | DONE | `host-observation-guide.md` verifyCoverage 절에 PREPARATION `rawRows.input`의 `codeCommit`·`workingTreeDirty`(묶인 준비 보고 bytes와 같음)·`checkoutCommit`·`checkoutDirty`(40/64자리 hex·boolean) 표를 더한다. `HostObservationValidator.preparationInput`이 강제한다. 현재 clean checkout과 맞는지는 T25 case assertion이 판정한다 | `afbce62d` |
| 5 | 자연 tick operationEvidence echo 제거 | DONE | host schema의 tickScheduler·sweepDue operationEvidence에서 `trigger`·`triggeredBy`·`observationWindowSeconds`를 지우고 validator도 거부한다. 증거는 extractor `rawRows.schedulerSubmissions`(`schedulerId`, `tickId`\|`sweepId`, `taskId`, `invocationHandle`, `submittedAt`, `submittedBy`)다. SUBMITTED identity는 창 안 첫 행과 같고 NO_TASK면 행이 없다. SCHEDULER_LOOP를 주장하는 harness tick 행은 거부한다. T26 `autonomous-trigger-loop` 3건은 `operationEvidence.taskId`로 고른 행의 `submittedBy`가 `{SCHEDULER_LOOP}`인지 exactSet으로 본다. 생성기 `T26/author_review_fixes.py`를 고쳤다 | `afbce62d` |
| 6 | C3 `catalogSha256` 재계산 | DONE | `author_prerequisites.py`가 `catalogSha256`도 다시 계산하고 C3 observation 목록이 catalog와 같은지 assert한다. stamp `61968b22`→`c1187224`. `check_derived_bindings.py`의 KNOWN_OPEN은 비었다 | `ad215f11` |
| 7 | layer-route KNOWN_OPEN 4건(T20 3·V4 1) | DONE | lock reviewUpdate로 requiredLayers를 좁히지 않고, case가 해당 layer 증거를 읽게 해서 닫는다. T20 `allowed-tools-as-server-authorization`(SKILLS: DISCOVERED·BODY_READ 행 각 1), T20 `document-instruction-authority`(SKILLS: BODY_READ 1, package exactSet = ontology-work-coordinator), T20 `skill-hash-as-loading-proof`(MCP: toolCalls COMMAND·RECORD 쓰기 0), V4 `mixed-batch-allowed-partial-effects`(MCP: JSON-RPC batch → HTTP 400·-32600·result 없음·COMMITTED command 0·claim 0·조직 범위 원행 9종 불변). `layer-route-review.json` 항목을 지웠다. test는 V4 MCP 경로를 지운 임시 복사본으로 unexplained·KNOWN_OPEN·stale 판정을 계속 시험한다 | `ad215f11` |
| 8 | MRTR requestState TTL을 `s0-protocol.md`에 기록 | DONE | "MRTR requestState(S5 계약)" 절. 발급 시각부터 TTL 600초, 599초 정상(T20 before-expiry STRUCTURED), 601초 REQUEST_STATE_EXPIRED, 다른 결속 실패 code, 운영 SLA가 아닌 개발/CI 값. `author_cases.py`는 문서의 `requestStateTtlSeconds=600`과 상수가 다르면 멈춘다. `test_generators_reproduce.py`가 이 문서를 입력에 넣는다 | `ad215f11` |

## 실행한 checks

환경은 Java 21, Python 3.14. 최종 결과는 clean tree `afbce62d`에서 얻었다.
중간 commit `ad215f11` 단독으로는 `test_coverage.py` 1건(layer gap 4건
기대)이 실패하며 `afbce62d`에서 고쳐진다.

| 명령 | exit | 결과 |
|---|---|---|
| `./verify harness` | 0 | 493 tests, failure/error/skip 0. 기준 487 대비 +6: `StepTwoRoundFourRegressionTest` 4건 신규, `HostObservationValidatorTest` 2건 추가 |
| `./verify prepare` | 0 | PREPARED, workingTreeDirty=false, 41/799/23956(기준 23936: T20 +6, V4 +14), 문제 0, caseAssetChecks 11개 PASS, knownOpenGaps 34(전부 vocabulary; layer-route 0, binding stamp 0; 기준 5), runtimeGates 1(regulatory) |
| `./verify coverage` | 2 | NOT_RUN, preparationStatus PREPARED, runtimeStatus NOT_RUN, gateComplete=false, preparationProblems 0, coverageProblems 0(기준은 KNOWN_OPEN layer gap 4건), knownOpenGaps 34 |
| `python3 verification/coverage/validate.py` | 0 | VALID, runtimeStatus NOT_RUN. `-I` 없이 실행한다(같은 폴더의 `assemble` import) |
| `python3 -m unittest discover -s verification/coverage -p 'test_*.py'` | 0 | 68 OK(282s) |
| `./verify contract-red` T20 T26 V4 C3 | 1 | expected=discovered=started=NOT_IMPLEMENTED=485, skipped 0. commit 전 같은 내용으로 실행 |
| `verification/model-binding/run selftest` | 0 | 61 OK(commit 전 같은 내용) |
| `verification/model-binding/run prepare` | 0 | PREPARED |
| `python3 -m unittest discover -s verification/requirements -p 'test_*.py'` | 0 | 37 OK(commit 전 같은 내용) |
| `python3 -m unittest discover -s verification/mcp-tests -p 'test_*.py'` | 0 | 3 OK(commit 전 같은 내용) |
| `python3 -m unittest discover -s verification/cases -p test_check_vocabulary.py` | 0 | 6 OK(commit 전 같은 내용) |
| `python3 -I verification/requirements/validate_catalog.py` | 0 | structure VALID, 122 oracle·499 observation |
| `python3 -I verification/cases/V2/cases_b_invariants.py` | 0 | 문제 0 |
| `python3 -I verification/cases/check_vocabulary.py --check` | 0 | VALID, pending 34 |
| `python3 -I verification/requirements/check_layer_routes.py` | 0 | VALID, KNOWN_OPEN 0, unexplained 0 |
| `python3 -I verification/requirements/check_derived_bindings.py` | 0 | VALID, 5 files, KNOWN_OPEN 0 |
| `python3 -I verification/cases/V4/author_review_fixes.py --check` | 0 | CURRENT |
| `python3 -I verification/cases/T08/bind_observations.py --check` | 0 | CURRENT |
| `python3 -I verification/cases/V7/bind_observations.py V4\|V6\|V7 --check` | 0 | 셋 다 CURRENT |
| 생성기 제자리 재실행(author_cases.py, T26, C3, V4, T06 author_contracts, platform-tests build_cases, model-binding generate.py) | 0 | diff 없음 |

NOT_RUN(coordinator): `./verify regulatory`, 다른 case의 `contract-red`,
`--actual`·제품 runtime. coordinator의 check 범위 조정에 따라 통합 때
coordinator가 실행한다.

## 하지 않은 것과 넘길 일

- Step 3 actual/adapter: `ObserverSnapshot`·`JdbcObservation`과 host
  adapter가 수동 tick/sweep의 `rawRows.schedulerSubmissions`, PREPARATION
  input `codeCommit`·`workingTreeDirty`·`checkoutCommit`·`checkoutDirty`,
  promiseCoverage 파생, 계약에 맞는 감사 원행을 만들기 전까지 해당 관찰은
  NOT_RUN이다.
- Step 3 backend: `queryAudit` source(PENDING_PRODUCT), 감사 PENDING field,
  vocabulary 추가 34건(MRTR REQUEST_STATE_* code 포함). MCP server는
  JSON-RPC batch에 HTTP 400·-32600을 반환해야 한다(V4가 단언한다).
- Step 1 skill 소유자: `.agents/skills/ontology-scenario-testing/references/repository-harness.md`는
  예전 caseAssetChecks 목록 그대로이고 vocabulary check, 감사 field gate,
  runtimeProfile 규칙, schedulerSubmissions가 없다.
- MCP batch의 HTTP 400·-32600은 `s0-protocol.md` 오류 표의 "malformed
  JSON/invalid envelope → 400, invalid-request" 행에서 고정했다.
- parallel watcher는 barrier가 아니다. watcher가 비정상적으로 늦으면 첫
  제출을 놓쳐 NO_TASK가 되고 case는 실패 쪽으로 닫힌다. 거짓 PASS 경로는
  없다.
- 제품 실행은 하지 않았다. 새 assertion은 모두 NOT_RUN이다.
