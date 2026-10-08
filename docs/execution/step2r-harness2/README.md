# Step2 재검토 2차: harness·coverage 교차 요청 처리 기록

1차 수정 worker들의 cross-owner 요청 가운데 harness·contracts·
coverage·root `./verify` 소유분을 사용자 Step2 모델(Claude Opus high)로
처리한 기록이다. 기준은 tag `step2r2-baseline`(`d21aee7c`), branch는
`step2r/harness2`다. 테스트 계약의 수정 증거이며 제품 S0–S6 runtime
인수가 아니다. 제품 runtime은 NOT_RUN이고 gateComplete=false다.

## 항목별 처리

| # | 판정 | 처리 | commit |
|---|---|---|---|
| 1 | DONE | `CatalogLinkValidator.requiredProfileReachability`가 assembler와 같은 requiredLayers→profile 규칙(deployment 확장, regulatory 증거 profile 포함)으로 도달 불가 observation을 문제로 낸다. `PreparationAssetChecks`가 `validate_catalog.py`(assembler가 쓰는 같은 lock 검증기)를 실행한다. 이 branch에서 도달 불가 profile은 0개다(1차 통합에서 E1/E2/T25/C3/T23/V8 profile이 이미 추가됨). | `5a7f470e` |
| 2 | DONE | `./verify coverage`는 model-binding prepare 뒤 `assemble.py --check-preparation [--index]`의 exit code를 돌려준다. Main 보고서에 `discovered`·`started`·`completed`·`skipped=0`, profile 자체 `gateComplete` 규칙. `ExecutionReceiptProducer`가 `--actual` schema~skills 실행의 ACTUAL receipt·envelope·index를 쓰며 selftest·unimplemented·dirty tree·build commit 불일치·version 누락이면 쓰지 않는다. assembler는 RAW_CAPTURE role과 `caseVersions`를 받는다. native actual-sN 연결 방법은 `verification/coverage/README.md`. | `22e72344`, `afa85e71` |
| 3 | DONE | CaseRunner 기록에 op·unit·baseline·unit source·where·field·resolvedExpected/Where·observed(·Unit·Baseline). assembler `record_problem`이 선언 일치와 bytes projection 일치를 확인하고 operator를 재적용한다. `$alias`는 installFixture `aliasMap`에서 푼다. | `a37c770c` |
| 4 | DONE | `OBSERVE_NEXT_NATURAL_TICK`·`observationWindowSeconds`(1–30)·`triggeredBy`를 schema와 `HostObservationValidator.naturalTick`, guide에 정의했다. 이전 schema는 이 세 field를 거부해 맞는 제품도 T26 자율 subcase를 통과할 수 없었다. harness tick의 자연 tick 주장도 거부한다. | `8e49031b` |
| 5 | DONE | `coverageSnapshot`: 네 inputSnapshotKind의 필수·금지 parameter, 입력·registry·catalog hash 결속, `rawRows.input`, mutation, CURRENT_EXECUTION link 규칙. guide에 T25 rawRows 출력 이름 표. semanticOracleEquivalence 값 정렬은 NOT_A_DEFECT(아래). | `8e49031b`, `afa85e71` |
| 6 | DONE | harness-guide에 단일 오류 envelope과 `current`=행 유효성. prepare가 `/response/code`·`*/errorCode`·`*/error_code`를 거부한다. 남은 위반 22개(C1 3, T03 2, T04 11, T05 2, T16 3, T18 1)는 case를 고치지 않고 보고한다. | `5a7f470e`, `afa85e71` |
| 7 | DONE | `cases_b_invariants.py`, `test_generators_reproduce.py`, T08 `bind_observations.py --check`를 `./verify prepare`(따라서 assembler `--check-preparation`과 `./verify coverage`)에 연결했다. 결과는 `prepare.json`의 `caseAssetChecks`. | `5a7f470e` |

NOT_A_DEFECT: 5번의 선택 항목 semanticOracleEquivalence 어휘 정렬.
PREPARATION 입력(준비 보고)의 값은 `REQUIRES_CASE_REVIEW`이고 T25가
그 값을 기대한다. assembler runtime manifest의 값
`REQUIRES_CASE_AND_RUNTIME_REVIEW`는 실행 증거까지 본 입력의 성질이다.
같게 만들면 T25 PREPARATION 기대와 충돌한다.

## 실행한 checks

환경은 `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`(Java 21.0.5,
Maven wrapper), Python 3.14다. Maven·`./verify`는 `$MULINO_SLOT`로
하나씩 실행했다. 최종 결과는 clean tree `afa85e71`에서 얻었다.

| 명령 | exit | 결과 |
|---|---|---|
| `./verify harness` | 0 | 470 tests, failure/error/skip0(기준 461). 새 `StepTwoRoundTwoRegressionTest` 7건, `HostObservationValidatorTest` 2건 |
| `./verify prepare` | 1 | 41 case·797 subcase·21788 assertion. 문제 27 = T24 `/provenance` 5(1차부터) + 비정규 오류 pointer 22. 도달 불가 profile 0, registry 불일치 0. `caseAssetChecks` 4개 exit0 |
| `./verify coverage` | 1 | assembler FAIL: preparation FAIL(`Fresh preparation ... failed`, 위 prepare FAIL 때문), runtime FAIL, gateComplete=false. 그 밖의 preparation·coverage 문제 0 |
| `python3 verification/coverage/validate.py` | 1 | `Manifest differs ... runtimeStatus`. 아래 관찰 참조 |
| `./verify contract-red verification/cases/T26/case.json` | 1 | 25/25 발견·NOT_IMPLEMENTED, skip0 |
| `./verify contract-red verification/cases/T25/case.json` | 1 | 22/22, skip0 |
| `python3 -I verification/requirements/validate_catalog.py` | 0 | structure VALID, 122 oracle·499 observation |
| `python3 -m unittest discover -s verification/coverage -p 'test_*.py'` | 0 | 61 tests OK(기준 57+새 4) |
| `verification/model-binding/run selftest` | 0 | 61 tests |
| `verification/model-binding/run prepare` | 0 | preparationStatus PREPARED |
| `python3 -m unittest discover -s verification/mcp-tests -p test_generators_reproduce.py` | 0 | 3 OK |
| `python3 verification/cases/V2/cases_b_invariants.py` | 0 | 0 problem |
| `python3 verification/cases/T08/bind_observations.py --check` | 0 | CURRENT |

case를 수정하지 않았으므로 case별 contract-red는 영향받는 계약(T26
자연 tick, T25 verifyCoverage)에만 실행했다. registry는 바꾸지 않았다
(`expectedSubcases` 797).

관찰: `validate.py`는 저장 manifest를 `--check-preparation` 없이
재조립해 비교한다. fresh preparation이 실패한 manifest는 그 문제 하나가
재조립 결과에 없어서 항상 불일치로 끝난다. PREPARED일 때는 생기지
않는다. 이번 변경 범위 밖이라 고치지 않았다.

## 하지 않은 것과 넘길 일

- 실제 backend·DB로 `--actual`을 실행하지 않았다. receipt producer의
  수용은 임시 디렉터리 형식 시험과 실제 assembler `receipt()` 호출로만
  확인했다. 제품 증거가 아니다.
- **cases-c(C1/T03/T04/T05/T16/T18)**: 비정규 오류 pointer 22개를
  `/response/error/code`로 바꾸면 prepare 문제에서 빠진다.
- **T24 소유자**: `sweep-authenticated-reviewer` 5개의 `/provenance`
  source(1차부터).
- **Step 1(skills)**: `.agents/skills/ontology-scenario-testing/references/
  repository-harness.md`의 "`./verify coverage` 결과 PREPARED",
  "receipt producer 없음", "Main은 gateComplete=false·discovered key 없음"
  문장이 낡았다. 새 사실은 harness-guide와 coverage README에 있다.
- **Step 3(actual/)**: ActualAcceptanceDriver artifact는 RAW_CAPTURE로
  묶인다. 실제 실행에는 `ACTUAL_SCHEMA_VERSION`·`ACTUAL_DB_VERSION`·
  (mcp/skills)`ACTUAL_MCP_PROTOCOL_VERSION`·`ACTUAL_BUILD_COMMIT`=검사
  commit과 clean tree가 필요하다. DB version은 가능하면 adapter가 직접
  관찰해 넘기는 편이 낫다. native actual-sN 연결 두 방법은 coverage README.
  host adapter는 자연 tick watcher와 verifyCoverage rawRows 계약을 따라야
  한다.
- **model-binding 소유자**: `semantic-paths.json`의
  `response.errorCode → /response/errorCode`(1차 cases-b 요청 그대로).
- **T26·deployment 소유자**: 운영 process가 sweeper loop를 실행하는지의
  deployment profile 검사.
- 오류 pointer 검사는 마지막 segment 규칙이다. 다른 이름의 비정규
  오류 field를 모두 잡지는 않는다.
