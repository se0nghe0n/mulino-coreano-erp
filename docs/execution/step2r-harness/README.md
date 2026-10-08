# Step2 재검토 harness 지적 수정 기록

Step2 재검토(Opus xhigh·Fable low)에서 확인된 harness 지적을 사용자
Step2 모델(Claude Opus high)로 고친 기록이다. 기준은 통합 Task HEAD
`7b540e8f`, worktree는 `step2r/harness`다. 이 기록은 테스트 계약
수정의 증거이며 제품 S0–S6 runtime 인수가 아니다. 제품 runtime은
NOT_RUN이고 gateComplete=false다.

## 지적별 처리

| ID | 판정 | 처리 |
|---|---|---|
| harness-00 | FIXED | UAT 전용 agent action은 scripted runner에서 port 호출 없이 NOT_IMPLEMENTED, subcase NOT_RUN이다. requiredAdapters를 driver·runner 공급과 대조한다. runner 선택(`--agent-runner`)과 evidence `agentRunner`/`agentActionRunners`를 추가했다. 미실행 action의 `$result`·`$alias`에 기대는 action도 exit3 대신 NOT_IMPLEMENTED다. |
| harness-01 | FIXED(+T24 CROSS_OWNER) | T08 6개 subcase의 인증 주체 oracle을 `after` 관찰 audit 원행으로 옮겼다. payload-actor의 위조 actor를 이동 권한 있는 warehouse로 바꿨다. `/provenance` 등 driver metadata source는 prepare 문제로 보고한다. T24 5건이 남는다. |
| harness-02 | FIXED | `$result` snapshotRef는 RESULT_REVISION+revisionQuery로 독립 재계산을, 두 directive는 새 read를 요구한다. snapshot.id echo와 capturedAt 역행을 거부한다. case schema는 directive 외 literal을 거부한다. |
| harness-03 | FIXED | CaseRunner 기본 PRODUCT 정책은 SELFTEST/CAPTURED/CANNED provenance와 비 ACTUAL_HOST process 증거를 거부한다. |
| harness-04 | FIXED(+case CROSS_OWNER 권고) | `data.data`의 모든 scalar leaf는 observer `derivations`를 rawRows에서 재계산해 일치해야 한다. HARNESS-EXAMPLE과 guide 예시를 rawRows로 옮겼다. |
| harness-05 | FIXED | `model|deployment --manifest`를 run manifest schema로 검증하고 NOT_RUN(exit2)으로 보고한다. 알 수 없는 option은 exit3이다. |
| harness-06 | FIXED(+T25 CROSS_OWNER) | db_snapshot artifactKind는 연결 subcase의 독립 observe assertion을 요구한다. 같은 oracle sibling의 같은 subcase 관찰도 인정한다. DB adapter를 선언하지 않은 연결은 catalog layer 불일치 gap으로 따로 보고한다. |
| P3 scope | FIXED | assertion `scope`는 비강제 추적 선언임을 guide와 evidence(`declaredScope`, `scopeEnforced:false`)에 명시했다. 강제 선택은 `source.where`다. |
| P3 Gherkin | FIXED | actual Gherkin은 첫 실패 뒤 남은 assertion을 평가하고 `failureKind`로 판정한다. 오류 문구 검색을 쓰지 않는다. |
| P3 adapters | FIXED | harness-00과 같은 수정으로 requiredAdapters를 강제한다. |

## 실행한 checks

환경은 `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`, Maven과
`./verify`는 `$MULINO_SLOT`로 하나씩 실행했다. openjdk 21.0.5,
Apache Maven 3.9.16이다.

| 명령 | exit | 결과 |
|---|---|---|
| `./verify harness` | 0 | 446 tests, failure/error/skip0. 기준 `7b540e8f`는 432였다. 새 회귀 시험은 `StepTwoReReviewRegressionTest` 9건과 기존 시험 보강이다. 수정 내용과 같은 미commit tree에서 실행했다. |
| `./verify prepare` (`ab3d5eb7`, clean tree) | 1 | 41 case·789 subcase·20461 assertion. 문제 5건은 모두 T24 `sweep-authenticated-reviewer`의 `/provenance` source다. artifactKind gap 4건(T25)은 문제가 아닌 별도 목록이다. registry 합계 불일치는 없다. |
| `./verify contract-red verification/cases/T08/case.json` | 1 | 23 발견·23 시작·23 NOT_IMPLEMENTED, skip0 |
| `./verify contract-red verification/cases/C3/case.json` | 1 | 309/309 NOT_IMPLEMENTED, skip0. agent intent 검사 이후에도 C3 schema·semantic이 통과한다. |
| `./verify contract-red` | 1 | HARNESS-EXAMPLE 한 scenario의 RED |
| `./verify model --manifest verification/harness/target/manifests/model-run.json` | 2 | NOT_RUN, 387 subcase 선택, runManifest VALID, approvalEvidence ABSENT, agentRunner SCRIPTED_SIT |
| `python3 verification/cases/T08/bind_observations.py --check` | 0 | CURRENT. 기존 커밋 case로 기존 파일을 byte 단위로 재현한 뒤 재생성했다. |
| `python3 -I verification/requirements/validate_catalog.py` | 0 | structure VALID, oracle122·observation499 |
| `python3 -m unittest test_catalog` (requirements) | 0 | 24 OK |
| `python3 -m unittest test_coverage` (coverage) | 0 | 43 OK |
| `python3 -I verification/model-corpus/validate.py` | 0 | VALID |
| `python3 -m unittest test_validate` (model-corpus) | 0 | 61 OK |
| `python3 verification/coverage/validate.py` | 0 | VALID, runtime NOT_RUN |
| `python3 verification/coverage/assemble.py --check-preparation` | 0 | preparationStatus FAIL. 위 prepare FAIL을 그대로 읽은 결과다. |
| `verification/model-binding/run selftest` | 0 | 58 tests PASS |
| `verification/model-binding/run prepare` | 0 | PREPARED |

## 다른 소유자에게 넘기는 조치

- T24 case 소유자: `legal-hold`, `active-reference`, `R6-unconfirmed`,
  `authorized-delete`, `restore-deleted`의 `sweep-authenticated-reviewer`
  가 `/provenance/authenticatedActor/subject`를 읽는다. 서버나 host가
  기록한 독립 원행(예 sweep 감사 원행의 actor, hostObservation 추출
  행)으로 옮겨야 prepare가 PREPARED로 돌아온다.
- coverage(catalog) 소유자: T25 `independent-traceability/coverage-link`,
  `result-separation`, `evidence-manifest-and-entrypoints/evidence-fields`,
  `wrapper-truth`의 artifactKinds에 db_snapshot이 있지만 연결 subcase는
  DB adapter가 없다. lock 절차로 artifactKinds를 바로잡거나 T25 소유자가
  DB 관찰을 추가한다. assembler도 observation 기록에 artifactKinds를
  싣는 것이 좋다.
- case 소유자(`/data/data` 사용): T26(heldQuantity·unit), V8·T23
  (`/inventory/*`), V7(dispatchedQuantity·unit, catalog primary),
  V2(executableAllocationQuantity·shortageQuantity)와 unitSource만 쓰는
  C4·E1·E2·T17·T18이다. 새 계약에서는 observer가 derivations를 선언해야
  통과한다. V7 primary는 `/data/rawRows/movements`의 `sumEquals`와 고정
  where로 옮기기를 권한다.
- Step3 native 소유자(`verification/harness/src/main/java/.../actual/`):
  JdbcObservation·S2–S4JdbcObservation은 snapshotRef가 있으면 아직
  NOT_IMPLEMENTED다. 지원하려면 자신의 MVCC token을 `snapshot.id`에,
  RESULT_REVISION이면 재계산 값과 `revisionQuery`, directive면
  `readMode`를, `data.data`를 쓰면 `derivations`를 내야 한다. driver의
  `provenance.authenticatedActor`는 oracle 원천이 아니다.
- coordinator: `StepTwoReReviewRegressionTest`의 T08 구조 검사는
  `a61f0bf2`(T08)와 함께 통합해야 통과한다.

## 하지 않은 것

제품 DB/API/MCP·실모델·배포는 실행하지 않았다. 실제 client port와
R8 승인 증거가 없어 UAT는 NOT_RUN이다. 다른 소유자의 case·catalog·
actual/ 코드는 바꾸지 않았다. registry 합계는 바꾸지 않았다.
