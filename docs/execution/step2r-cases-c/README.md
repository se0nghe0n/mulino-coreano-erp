# Step2 재검토 2차 cases-c 수정 기록

사용자 Step2(tests) 재검토 2차에서 cases-c에 배정된 7개 항목을 Claude
Opus high로 고친 기록이다. 기준은 tag `step2r2-baseline`(`d21aee7c`),
branch `step2r/cases-c`, worktree
`/Volumes/VideoStore/Developer/mulino-ontology-step2r-cases-c`다.
소유 범위는 `verification/cases/{T24,T17,V4,T22,V7,V5,V6,T20,V2,V3,C1,
T03,T04,T05,T16,T18}/**`와 그 생성기, `verification/model-binding/
semantic-paths.json`, registry의 해당 subcaseIds다. 제품 runtime·
실모델·host·배포는 실행하지 않았고 모두 NOT_RUN이다. 아래 PASS는
harness·준비·RED 계약 검증이며 제품 인수가 아니다.

## commit

| commit | 내용 |
|---|---|
| `db45081f` | T24 sweep 주체를 서버 감사 원행으로, T22 retry를 slots={commandId, reason}으로(공유 생성기 `T06/author_contracts.py`) |
| `4f700617` | C1·T03·T04·T05·T16·T18 오류 pointer 22건을 `/response/error/code`로, T04 QUANTITY_INVALID→TYPE_INVALID |
| `f3eced5f` | model-binding `response.errorCode`→`/response/error/code`(생성기 `generate.py`) |
| `ef01e073` | T20 MCP 도메인 오류의 structuredContent 코드·isError, 경합 arming 표기(생성기 `mcp-tests/author_cases.py`) |
| `1b92a1aa` | V5·V6·V7 barrier 표기, V7 safe retry 입력, V4/V6/V7 bindings 생성기 |
| `615a7473` | T17 second-reserve SELL·코드·배분0, release-hold revision, post-dispatch-expiry subcase |
| `c4396706` | V2 reserve-commits-first를 불변식 판정으로 |
| `eded4248` | V4 노출 쓰기 면 열거 subcase, capability별 주 효과 관찰, 대조 호출 자기 키 |
| `258bdf6a` | T17 새 subcase의 oracleId·Gherkin 따옴표 수정 |
| `71bc0ba3` | harness 소유 `AuthorityAssertionsTest` subcase 수 437→438(소유 밖, 아래 참고) |
| `e97af647` | V7 감사 원행 filter를 모든 행에 있는 capabilityId로 |

## 항목 처리

| 항목 | 판정 | 처리 |
|---|---|---|
| 1 T24 prepare FAIL×5 | DONE | 보존 sweep 5개 subcase의 `sweep-authenticated-reviewer`는 `db-after` 감사 원행(action=retentionSweep)의 actorId=config·organizationId=ORG-A·policyVersion·sweepId를 relationSet으로 읽는다. `/provenance` 원천은 0건이고 `./verify prepare` 문제는 0이다. |
| 2 오류 pointer | DONE | C1 3·T03 2·T04 11(raw-db 포함)·T05 2·T16 3·T18 1을 `/response/error/code`로 바꿨다. T04 EA0.5·정밀도·scale 3건은 계획 §3.3·§3.4의 TYPE_INVALID다. model-binding은 corpus 이름을 두고 pointer만 바꿨다. T20의 raw MCP 결과는 s0-protocol.md 표대로 `/response/body/result/structuredContent/error/code`와 `isError=true`를 domain-forbidden·conflict·rejected·stdio에 추가했다. 계획이 이름 붙이지 않은 T04 RAW_CORE_WRITE_FORBIDDEN·DB_PRIVILEGE_DENIED·TRANSACTION_ROLLED_BACK, T03 GENEALOGY_CYCLE·SEPARATION_EVIDENCE_REQUIRED, T20 MRTR 코드는 그대로 두었다(Step 3 어휘 공개 대상). backend 전용 이름은 넣지 않았다. |
| 3 T22·V7 retry | DONE | T22는 getObject(CommandRecord)로 현재 revision을 읽고 slots={commandId, reason}만 보낸다. 감사 원행 actorId=operations·targetId=원 command·멱등키를 확인한다. V7은 slots={commandId, reason, claimFencingToken}만 보내고(claim fence에 필요) subjectRefs를 같은 command로 맞췄다. 감사 원행(delegator·REJECTED)과 원 command new20 owner(warehouse) 보존을 확인한다. |
| 4 barrier 표기 | DONE | T20·V6·V7 start 요청에 top-level testTransactionId·testParticipantId·testBarrierId·testBarrierPoint를 두고 control transactionId를 같은 label로 바꿨다. T20·V6·V7(effect-first)은 두 ACK의 실제 DB transaction이 다름을 확인한다. V5 worker는 API 요청이 없어 같은 네 필드를 pause fault arm에 둔다. 이전 판처럼 claim 원행의 barrierId를 되돌려 넣지 않는다. |
| 5 T17 | DONE | second-reserve에 action SELL, INSUFFICIENT_ELIGIBLE_QUANTITY, 두 번째 주문 line 배분0을 추가했다. release-hold expectedRevision은 hold 응답이다. post-dispatch-expiry는 DISPOSITION 허용 09:05 만료 전 출고30, 시계 09:06, 인도20 기록 뒤 인도20·운송10 보존, 새 출고·배분0, 인도 판정 UNSATISFIED, VIOLATION_RESPONSE 의무(owner sales)를 확인한다. catalog `T17.late-restriction-actual-delivery`의 observation에 연결했다. |
| 6a V4 열거 | DONE(계약), 실행 NOT_RUN | `exposed-write-surface` subcase가 MCP server/discover·tools/list를 raw wire로 읽고 host 조작 `enumerateWriteSurface`로 OData $metadata·worker registry·관리 endpoint까지 열거해 15개 probe class를 모든 대상에 시도한다. 판정과 extractor 원행 계약은 `verification/cases/V4/README.md`에 있다. 이 조작은 host-observation schema에 없어 CROSS_OWNER(harness)다. 추가 전에는 NOT_IMPLEMENTED이고 V4 노출 면은 계속 NOT_RUN이다. |
| 6b V4 주 효과 | DONE | route×family 80개에 C3 `CAP_EFFECTS`와 같은 원천을 조직 범위 effect-before/after로 관찰하고 unchanged-effect-*, reader-command-not-committed를 추가했다. |
| 6c V4 대조 키 | DONE | 대조 호출은 `V4-<subcase>-authorized`를 쓴다. batch operations·blob businessAction 안의 키도 바꾼다. |
| 7 V2 | DONE | 아래 "V2 판단"을 본다. |

## V2 판단

계획 §13.2 V2는 실물60·기존 의무40·신규 실행배분 최대20·부모
재소비0만 정한다. §4.2는 lock 뒤 revision을 다시 읽고 충돌이면 새
의도로 몰래 실행하지 말고 conflict를 돌려주라고 한다. reserve가 A60의
revision을 올리는지는 계획이 정하지 않는다. 올리면 contender 분할은
CONFLICT/STALE_REVISION·효과0이 맞고, 올리지 않으면 요청 revision1이
최신이라 ALLOC40과 신규20을 한 번씩 자식으로 옮기는 APPLIED가 맞다
(backend `FulfillmentPostgresTest.v2ReserveFirst`). 그래서 결과 하나를
고정하지 않았다. 대신 harness에 조건부 assertion이 없으므로 두 경로에서
같은 값이 되는 비교로 아래를 모두 요구한다.

- 응답 outcome ∈ {APPLIED, CONFLICT}(나머지 enum 6개 notEquals).
- 응답 outcome = 같은 거래 감사 원행 outcome(`raced-db` derivation
  contenderOutcome).
- 감사가 CONFLICT일 때의 errorCode 집합 = lock 뒤 재검증이
  STALE_REVISION인 행의 result 집합. 재검증이 STALE인데 적용, 최신인데
  충돌, 다른 코드는 실패한다.
- contender CONFLICT ⇔ 현재 revision으로 보낸 수렴 분할 APPLIED.
  CONFLICT로 보고하고 효과를 남기면 실패한다.
- 경합 직후·최종 실행 배분 ALLOC40·신규20 각 한 번, active 실물 60,
  최종 자식 40·20뿐, A60 retired, A60을 가리키는 active 배분0.

남은 한계: CONFLICT일 때만 있는 API `/response/error/code`는 직접
고정하지 못하고 감사 원행 코드로 묶었다. 자식별 배분 수용량(40 배분이
40 자식에 있는지)은 ID 조인 연산이 없어 합계·부모0으로만 막았다.
판정 근거는 `verification/cases/V2/race-observation-contract.md`에 있다.

## 실행한 검사

`. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`, Java 21.0.5,
Python 3.14이다. Maven과 `./verify`는 `$MULINO_SLOT`으로 하나씩 실행했다.
registry `expectedSubcases`는 coordinator 소유라 commit 값은 797이다.
이 branch의 실제 합계는 799(T17 +1, V4 +1)이다.

| 명령 | exit | 결과 |
|---|---|---|
| `./verify prepare` (commit `71bc0ba3`, clean, 797) | 1 | FAIL, 문제 1개: Case/subcase discovery count differs from registry. 799 subcase |
| 같은 명령, 임시 799 commit(`e4ff92fe`, clean, 검사 뒤 되돌림) | 0 | PREPARED, 41 case/799 subcase/22117 assertion, 문제0, workingTreeDirty=false, artifactKindAttributionGaps 4(T25, 기존) |
| 같은 명령, `e97af647` + 임시 799(dirty) | 0 | PREPARED, 문제0 |
| `./verify harness`, 임시 799 | 0 | 461 test, 실패·오류·skip 0 |
| `./verify contract-red` T24/T22/T20/T17/V2/V4/V5/V6/V7/C1/T03/T04/T05/T16/T18 | 1 | 각각 발견=NOT_IMPLEMENTED=subcase 수 13/15/58/15/3/93/4/9/4/2/8/9/3/4/4, skip0 (V7은 `e97af647`에서 다시 4/4) |
| `python3 verification/coverage/assemble.py --check-preparation`, 임시 799 commit | 2 | preparationStatus PREPARED, runtime NOT_RUN, gateComplete=false |
| `python3 -m unittest discover -s verification/coverage` | 0 | 57 OK(임시 799 상태) |
| `python3 -I verification/requirements/validate_catalog.py` | 0 | structure VALID, 122 oracle/499 observation |
| `python3 -m unittest discover -s verification/requirements` | 0 | 25 OK |
| `verification/model-binding/run selftest` | 0 | 61 test, 실패0 |
| `verification/model-binding/run prepare` | 0 | preparationStatus PREPARED, 모델 호출0 |
| `python3 -I verification/model-corpus/validate.py` | 0 | VALID |
| `python3 -m unittest discover -s verification/mcp-tests` | 0 | 3 OK(T01/T20/T25/C3/T26 생성기 재현) |
| 생성기 재실행: T06 author_contracts, mcp-tests author_cases, model-binding generate, C3 author_prerequisites | 0 | 재실행 뒤 git diff 없음 |
| `python3 -I verification/cases/V4/author_review_fixes.py --check` | 0 | CURRENT |
| `python3 -I verification/cases/V7/bind_observations.py V4/V6/V7 --check`, T08 `--check` | 0 | CURRENT |
| `python3 verification/cases/V2/cases_b_invariants.py` | 0 | 문제0. 수정 전 V2 case.json에는 새 규칙 11이 2건을 보고한다 |

임시 799 검사용 commit은 `git reset --soft`로 지우고 registry를 797로
되돌렸다. push하지 않았다.

## registry 변경

- T17 subcaseIds에 `post-dispatch-expiry` 추가(15개).
- V4 subcaseIds 끝에 `exposed-write-surface` 추가(93개).
- `expectedSubcases`는 바꾸지 않았다(797). coordinator가 다시 계산한다.

## 소유 밖에서 바꾼 것

- `verification/cases/T06/author_contracts.py`: T06·T22·T24 공유
  생성기다. T22·T24 부분과 `ass`/`raw`의 explain 인자만 바꿨고 T06
  출력은 byte 단위로 같다.
- `verification/mcp-tests/author_cases.py`: T01·T20·T25 공유 생성기다.
  T20 부분만 바꿨고 T01·T25 출력은 같다.
- `verification/model-binding/generate.py`: semantic-paths.json 생성기다.
- `verification/harness/src/test/.../cases/authority/AuthorityAssertionsTest.java`:
  V4 subcase 추가로 합계 437→438 한 숫자만 바꿨다(commit `71bc0ba3`).
  harness worker 변경과 겹치면 이 commit만 다시 맞춘다.

## cross-owner 요청

- harness 소유자: `enumerateWriteSurface`를
  `contracts/acceptance-host-observation.schema.json` operation 목록과
  `verification/host-observation-guide.md` 표에 추가한다(identity
  environmentId·enumerationId, extractor rawRows surfaces·surfaceItems·
  probeCoverage·probes; 계약은 V4 README). 조건부 assertion(outcome별
  guard)이 있으면 V2 contender의 API error.code를 직접 고정할 수 있다.
- catalog 소유자: `V4.all-alternate-write-paths`의 same-auth-path
  (allEffectClassesAndCommandFamiliesCovered, noRawCoreCRUD)에
  `V4/exposed-write-surface`를 연결한다. 이 subcase의 assertion은 이미
  해당 oracle의 same-auth-path·inventory/approval/outbox-effects
  observation을 oracleRef로 가리킨다. T17 post-dispatch-expiry는 기존
  `T17.late-restriction-actual-delivery` observation에 연결했다. 계획
  §13.1의 "recall/만료" 중 만료 쪽을 별도 oracle로 둘지 판단한다.
- Step 3 adapter: V2 raced-db의 contenderOutcome derivation,
  lockRevalidations.result의 REVISION_CURRENT, V4 enumerateWriteSurface,
  T20 raw MCP domain error의 structuredContent/error/code·isError, OData
  등 raw 경로를 driver.wire로 다루는 일. 오류 어휘(TYPE_INVALID 등 계획
  §3.4 코드와 RAW_CORE_WRITE_FORBIDDEN 같은 case 고유 코드)를 공개할 때
  위 case를 대조한다.
- coordinator: registry expectedSubcases를 다시 계산한다(이 branch 799).

## 하지 않은 것

- 제품·host·wire·경합·모델·배포 실행은 없다. 모두 NOT_RUN이다.
- V7 new-effect-quantity0의 `/data/data/dispatchedQuantity`를 rawRows
  sum으로 옮기는 harness-04 권고는 이번 항목 밖이라 두었다.
- 계획이 이름 붙이지 않은 case 고유 오류 코드는 바꾸지 않았다.
- `docs/execution/*`의 다른 기록과 T17·T24·V7의 옛 `checks/`·`evidence/`
  기록은 당시 실행 기록이라 다시 쓰지 않았다.
