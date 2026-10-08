# Step2 재검토 3차: closure review의 harness·coverage 지적 처리 기록

Step 2 closure review(`step2-closure-1`)의 harness·coverage 소유 지적을
사용자 Step2 모델(Claude Opus high)로 처리한 기록이다. 기준은 tag
`step2r3-baseline`(`3f51bed0`), branch는 `step2r/harness3`다. 테스트
계약의 수정 증거이며 제품 S0–S6 runtime 인수가 아니다. 제품 runtime은
NOT_RUN이고 gateComplete=false다.

## 항목별 처리

| # | 지적 | 판정 | 처리 | commit |
|---|---|---|---|---|
| 1 | P2 receipt 표지 검사가 `snapshot.capturedAt`을 표지로 오인 | DONE | producer `actionMarker`와 assembler `provenance_marker`가 provenance label 값(source·adapter·adapterVersion·buildVersion·snapshot.isolation·sourceQuery.mappingVersion)만 본다. 실제 observe snapshot을 가진 실행의 수락과 selftest label 거부를 Java·Python 회귀 test로 고정했다 | `adc7aefe` |
| 2a | layer route 검사 미연결, KNOWN_OPEN이 exit0 상수 | DONE | 예외를 `verification/requirements/layer-route-review.json`에 owner·reason·closeWhen과 기록하고, 필드 누락은 예외가 아니며 남은 항목은 stale로 실패한다. prepare `layer-routes` 검사와 assembler `layer_routes`가 같은 `review()`를 쓴다. KNOWN_OPEN 관찰은 coverage에서 NOT_RUN으로 고정된다 | `f87c591f`, `adc7aefe` |
| 2b | V4/V6/V7/C3 binding drift, 생성기 미검사 | DONE(C3는 CROSS_OWNER) | V4·V6·V7은 `V7/bind_observations.py`로 다시 만들었다(차이는 catalogSha256 한 줄). prepare에 V4·V6·V7 `--check`, 모든 bindings의 stamp 검사(`check_derived_bindings.py`), V4·T06·platform 생성기 재현 test를 더했다. C3는 생성기가 catalogSha256을 갱신하지 않아 생성기 실행만으로 고칠 수 없다. KNOWN_OPEN 한 건으로 기록했다 | `f87c591f` |
| 2c | clean tree·build commit gate가 실행 뒤 자기 진술뿐 | DONE(build는 한계 기록) | Main이 첫 case 전에 `preRun`(HEAD·git status·시각)을 관찰하고 producer는 실행 뒤 상태와 같은 HEAD의 clean을 요구한다. receipt `workingTreeObservations`를 assembler가 report `preRun`과 대조한다. build commit은 backend가 build-info를 노출하지 않아 관찰할 수 없으므로 `buildIdentity.source=DECLARED_ACTUAL_BUILD_COMMIT`로 선언임을 적고 class 문서의 attestation 주장을 고쳤다 | `adc7aefe` |
| 2d | case 파일을 명시한 부분 실행이 gateComplete=true PASS | DONE | `discovered`는 저장소가 그 profile에 선언한 subcase 수, `explicitCaseSelection=true`면 gateComplete=false·receipt NOT_EMITTED. assembler는 모든 선언이 report `cases[]`에 있고 `explicitCaseSelection=false`일 때만 profile PASS | `adc7aefe` |
| 2e | assembler가 index profile과 report/receipt profile을 대조하지 않음 | DONE | report `profile`(deployment 두 profile은 `deployment` 허용)과 receipt argv가 index profile을 이름으로 가져야 한다. 다르면 FAIL | `adc7aefe` |
| 2f | 캡처 bytes가 선택 계약을 어기면 runner 기록으로 미룸 | DONE | `SelectionViolation`: pointer 누락·null, where·field 원행의 field 누락·null은 runner PASS와의 모순(FAIL). 판정 불가는 결과 없는 action과 풀 수 없는 참조뿐 | `adc7aefe` |
| 2g | RESULT_REVISION echo를 구별할 수 없음 | DONE | observer 요청에서 발급 값을 빼고 `snapshotRef=RESULT_REVISION`과 `snapshotSource`(발급 action·route·capability·actor·해석된 request)를 보낸다. harness가 보관한 값과 결과를 비교하므로 echo는 불일치다. actual observer는 이전과 같이 NOT_IMPLEMENTED | `b8570dca` |
| 2h | `enumerateWriteSurface` 미정의인데 prepare 0 문제 | DONE | schema enum·operationEvidence·rawRows 네 표, `HostObservationValidator.writeSurface`(allowlist 재계산 포함), guide 절. prepare는 vocabulary 밖 process operation을 문제로 낸다 | `93e66f59`, `adc7aefe` |
| 2i | T15 regulatory 증거 profile이 종이 위에서만 도달 가능 | DONE(NOT_RUN gate) | 실행 경로를 만들지 않고 명시적 gate로 남겼다. `./verify regulatory`는 exit2 `NOT_RUN_GATED`, prepare `runtimeGates`가 T15 subcase 3개·관찰 3개를, assembler가 regulatory profile missingReason을 같은 문장으로 적는다. 검토된 규제 기록과 승인된 runner는 R-gate 결정 없이 만들 수 없다 | `adc7aefe` |

문서: `e3326aad`(harness-guide, coverage README), requirements README는
`f87c591f`, host-observation-guide는 `93e66f59`.

## 실행한 checks

환경은 `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`(Java 21,
Maven wrapper), Python 3.14다. Maven·`./verify`는 `$MULINO_SLOT`로
하나씩 실행했다. 아래 최종 결과는 clean tree `e3326aad`에서 얻었다.

| 명령 | exit | 결과 |
|---|---|---|
| `./verify harness` | 0 | 477 tests, failure/error/skip0(기준 473). 새 test 4건: `StepTwoRoundTwoRegressionTest.actualObserveWithSnapshotIsEligibleAndSelftestLabelsAreRefused`, `HostObservationValidatorTest` 2건, `ReviewContractTest.resultRevisionObserverGetsIssuingRequestIdentityNotTheRevision`. 기존 test는 preRun·subset·argv·KNOWN_OPEN 검사를 더했다 |
| `./verify prepare` | 0 | PREPARED, 41 case·799 subcase·23899 assertion, 문제 0, `caseAssetChecks` 10개 PASS, `knownOpenGaps` 5(layer 4, C3 stamp 1), `runtimeGates` regulatory 1 |
| `./verify coverage` | 2 | NOT_RUN, preparationStatus PREPARED, runtimeStatus NOT_RUN, gateComplete=false, preparation 문제 0. coverage 문제에 KNOWN_OPEN layer gap 4건 |
| `python3 verification/coverage/validate.py` | 0 | VALID, runtimeStatus NOT_RUN |
| `./verify regulatory` | 2 | `NOT_RUN_GATED regulatory ...` |
| `./verify contract-red verification/cases/V4/case.json` | 1 | 93/93 발견·NOT_IMPLEMENTED, skip0 |
| `./verify contract-red verification/cases/V6/case.json` | 1 | 9/9, skip0 |
| `./verify contract-red verification/cases/V7/case.json` | 1 | 4/4, skip0 |
| `python3 -I verification/requirements/validate_catalog.py` | 0 | structure VALID, 122 oracle·499 observation |
| `python3 -m unittest discover -s verification/coverage -p test_coverage.py` | 1→0 | 67 tests. 첫 실행에서 `test_saved_manifest_cannot_hide_working_tree_state` 1건이 실행 도중 내 commit으로 HEAD가 바뀌어 실패했다. 그 test(`-k saved_manifest`, 3건)를 단독 재실행해 OK |
| `python3 -m unittest discover -s verification/requirements -p 'test_*.py'` | 0 | 35 OK(layer route 7, 생성기 재현 3 포함) |
| `python3 -m unittest discover -s verification/mcp-tests -p test_generators_reproduce.py` | 0 | 3 OK |
| `verification/model-binding/run selftest` | 0 | 61 tests |
| `verification/model-binding/run prepare` | 0 | preparationStatus PREPARED |
| `python3 -I verification/cases/V7/bind_observations.py V4\|V6\|V7 --check` | 0 | 셋 다 CURRENT(재생성 전 기준에서는 셋 다 exit1) |
| `python3 -I verification/cases/T08/bind_observations.py --check` | 0 | CURRENT |
| `python3 -I verification/cases/V4/author_review_fixes.py --check` | 0 | CURRENT(재생성 전 exit1) |
| `python3 -I verification/requirements/check_layer_routes.py` | 0 | VALID, KNOWN_OPEN 4, 문제 0 |
| `python3 -I verification/requirements/check_derived_bindings.py` | 0 | VALID, KNOWN_OPEN 1(C3), 재생성 전에는 V4·V6·V7 STALE로 exit1 |

반례 확인: T24 `scenario.feature`·V8 `case.json`·V4 `scenario.feature`에
한 줄을 덧붙이면 각각 `test_t06_contracts`·`test_platform_cases`·
`test_v4_review_fixes`가 실패함을 손으로 확인하고 되돌렸다.

case.json·fixture·Gherkin과 registry는 바꾸지 않았다. 바꾼 case 자산은
V4·V6·V7 `observation-bindings.json`(생성기 출력)뿐이다.

중간 commit `f87c591f` 단독으로는 `StepTwoRoundTwoRegressionTest.
preparationAssetChecksFailClosed`가 실패한다. 새 검사 script의 빈
test module을 만드는 test 수정이 `adc7aefe`에 들어 있다. 위 결과는
최종 commit 기준이다.

## 하지 않은 것과 넘길 일

- 실제 backend·DB로 `--actual`을 실행하지 않았다. receipt producer의
  수락은 형식 test와 회귀 test로만 확인했다.
- build commit 관찰: backend(Step 3, `backend/**`)가 검증 profile에서
  build-info(commit)를 노출하면 harness가 실행 전후로 비교할 수 있다.
  지금은 선언으로 남는다.
- RESULT_REVISION 재계산: actual observer(`ObserverSnapshot`,
  `verification/harness/.../actual/`, Step 3 소유)가 `snapshotSource`로
  projection revision을 재계산하기 전까지 result-bound 관찰은 NOT_RUN이다.
- `enumerateWriteSurface` host adapter는 없다. V4 `exposed-write-surface`는
  NOT_RUN이다.
- C3 `author_prerequisites.py`가 catalogSha256을 갱신하도록 고치는 일은
  C3 owner 몫이다. 고친 뒤 `check_derived_bindings.py`의 KNOWN_OPEN 항목을
  지워야 한다(남기면 stale로 실패한다).
- KNOWN_OPEN layer gap 4건은 T20 owner(3)와 V4 owner(1)가 닫거나 lock
  reviewUpdate로 requiredLayers를 좁혀야 한다. 닫으면
  `layer-route-review.json`의 항목도 지운다.
- skill reference `.agents/skills/ontology-scenario-testing/references/
  repository-harness.md`(Step 1 소유)는 caseAssetChecks 목록, receipt
  조건(label 표지·preRun·부분 실행), `enumerateWriteSurface` 정의 여부,
  regulatory gate, RESULT_REVISION 요청 형태가 이 branch와 다르다.
- 이 라운드 범위 밖으로 남긴 closure P3: 자연 tick의 operationEvidence가
  요청 parameter를 되풀이해야 하는 계약(T26 case와 함께 바꿔야 한다),
  `/data/data` primary와 observer가 고르는 derivation(V7 case와 schema
  변경이 함께 필요), T25 verifyCoverage가 요구하는 assembler 기능
  (inputSnapshotKind·currentExecution·mutation), T25 PREPARATION 입력의
  commit 결속(T25 case), `test_generators_reproduce.py` docstring
  (mcp-tests 소유).
