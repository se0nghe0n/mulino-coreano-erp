# Step2 재검토 cases-b 수정 기록

사용자 Step2(tests) 재검토에서 확정된 cases-b 지적을 Claude Opus
high로 고친 기록이다. 소유 범위는 `verification/cases/{E1,E2,C4,V2,V3}/**`,
`contracts/command-response.schema.json`, registry의 해당 subcaseIds다.
기준은 Task HEAD `7b540e8f`, branch `step2r/cases-b`, worktree
`/Volumes/VideoStore/Developer/mulino-ontology-step2r-cases-b`다.
제품 runtime·실모델·배포는 이 작업에서 실행하지 않았고 모두
NOT_RUN이다. 아래 PASS는 harness·준비·RED 계약 검증이다.

## commit

| commit | 내용 |
|---|---|
| `fd579b4c` | command-response schema에 error.code 단일 pointer, 담당 사례 6개 pointer 정규화 |
| `790fac1e` | E2 출고 probe 정상 형식·원인 고정, 대조군 subcase, false-close 정상 형식·code |
| `b9146dca` | E1 current 의미, 규제 버킷/축, 명사·동사 원장 고정, consume-again 형식·code |
| `9fb82992` | C4 반품 뒤 판정·부족 의무0, duty filter |
| `ab418728` | V2 신규 주문 ORDER2, API 정확값, actual50 약속60 보존 |
| `039e3d3d` | V2·V3 lock WAIT 독립 관찰·lock 뒤 재검증, V3 stale 보류 CONFLICT+fresh 보류 |
| `8452926b` | C4 selftest를 깨는 확인 assertion 제거, filter 유지 |
| `a85744db` | E1·E2 mcp profile과 같은 snapshot의 MCP 조회 |
| `a2f51fc2` | `verification/cases/V2/cases_b_invariants.py` 구조 불변식 검사 |
| `56dc9b0d` | E1·E2·C4 README 연결표와 의미 |

## 지적 처리

| ID | 판정 | 처리 |
|---|---|---|
| cases-b-00 | FIXED | E2 dispatch60을 피킹된 단일 allocationId·cargoPlaceId·현재 revision의 dispatch20/40으로 바꾸고 REJECTED·INSUFFICIENT_ELIGIBLE_QUANTITY·거부 감사1, 자식40 배분 SUSPENDED(회수만 원인)를 고정했다. 대조군 subcase에서 같은 출고40이 APPLIED다. false-close는 scopeVersion·scopeHash·근거를 보내고 RECALL_RESIDUAL_UNKNOWN·감사1을 고정했다. E1 consume-again도 같은 처리를 했다. T17 second-reserve는 소유 밖이다. |
| cases-b-01 | FIXED | V2·V3 경합이 contender의 실제 초기 read ACK, winner lock 보유 중 contender의 pg_catalog WAIT, 두 transaction OPEN, lock 뒤 lockRevalidations를 요구한다. 없는 lockProbeScopeOnly 문구를 지웠다. |
| cases-b-02 | FIXED | V2 경합 신규 예약을 별도 ORDER2(20)·S2·PROMISE2로 옮겼고 new-executable-reservation-8에 active·EXECUTABLE을 더했다. |
| cases-b-03 | FIXED | V3 dispatch-first의 옛 revision 보류는 CONFLICT/STALE_REVISION·제한0이고, 현재 segment revision으로 낸 fresh-hold가 책임을 남긴다. |
| cases-b-04 | FIXED | cases-b-00과 같은 출고 수정, qc-release20 revision을 qc-hold20으로 바꿨다. V3 late-v1-release의 해제·출고 revision은 직전 getObject에서 읽는다. T17 release-hold는 소유 밖이다. |
| cases-b-05 | FIXED | current=행 유효성(status와 독립)으로 정해 E1을 고쳤고 C4는 이미 이 의미였다. hold60 의무 RESOLVED·current와 hold40 의무 식별을 추가했다. kind 어휘 게시는 CROSS_OWNER다. |
| cases-b-06 | FIXED | C4 반품 뒤 getAssessment SATISFIED, current 판정 원행 SATISFIED뿐, DELIVERY_DEFICIT 0건을 추가했다. |
| cases-b-07 | FIXED | E1 기관미확인30을 규제 UNKNOWN·QCHoldActive=false·PURCHASE 버킷으로 명시하고 규제 축 UNKNOWN70을 따로 확인한다. |
| cases-b-08 | FIXED | actual50에 promiseCoverage 합계60과 부족 쪽 원천=정정 obligationId를 추가했다. 실행50·부족10 정확값은 계획 §13.2보다 강해 넣지 않았다. |
| cases-b-09 | FIXED | schema에 /error/code를 선언하고 담당 사례를 /response/error/code로 바꿨다. 다른 사례는 아래 목록으로 넘긴다. |
| cases-b-10 | FIXED | linkedSummary를 goals snapshot·고정 시계·goals-db OPEN 의무 원행·고정 수량에 묶고 동사 duties 동일성을 추가했다. |
| cases-b-11 | FIXED | cases-b-09와 같다. envelope 정의는 schema `$comment`와 `required`다. harness-guide 문구는 CROSS_OWNER다. |
| cases-b-12 | FIXED | cases-b-06과 같다. |
| cases-b-13 | FIXED | V2 두 경합 subcase에 API 실행 예약 40/60·신규 0/20 정확값을 추가했다. actual50은 cases-b-08의 보존식으로 처리했다. |
| cases-b-14 | FIXED | barrier arming을 V8 형식의 top-level test 필드로 옮기고 `verification/cases/V2/race-observation-contract.md`에 위치·test profile 한정·ACK·probe 계약을 적었다. test profile 밖 거부 negative subcase는 만들지 않았다. |
| P3-0 (T17 만료) | CROSS_OWNER | T17 소유자가 출고 뒤 만료 subcase를 판단해야 한다. |
| P3-1 (C4 위치 선택) | FIXED | duty 조회에 kind·status filter를 넣었다. 잘못된 의무 해소는 이미 valid-resolution-4와 new-unresolved-deficit-2가 잡는다. |

## 실행한 검사

환경은 `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`, Java
21.0.5, Maven 3.9.16, Python 3.14.7이다. Maven과 `./verify`는
`$MULINO_SLOT`으로 하나씩 실행했다. 대상 commit은 `56dc9b0d`다.

| 명령 | exit | 결과 |
|---|---|---|
| `$MULINO_SLOT ./verify harness` | 1 | 432 test, failure 2(CoverageSchemaTest 2개, 원인은 registry 총계) |
| 같은 명령, registry expectedSubcases만 임시 790 | 0 | 432 test, failure/error/skip 0. 임시 변경은 되돌렸다 |
| `$MULINO_SLOT ./verify prepare` | 1 | FAIL, 790 subcase·20622 assertion, 문제 1개: registry 총계 |
| 같은 명령, 임시 790 | 0 | PREPARED, 문제0 |
| `$MULINO_SLOT ./verify contract-red verification/cases/E1/case.json` | 1 | 발견3·시작3·NOT_IMPLEMENTED3·skip0 |
| 같은 명령 E2 | 1 | 4·4·4·skip0 |
| 같은 명령 C4 | 1 | 4·4·4·skip0 |
| 같은 명령 V2 | 1 | 3·3·3·skip0 |
| 같은 명령 V3 | 1 | 3·3·3·skip0 |
| `python3 verification/requirements/validate_catalog.py` | 0 | structure VALID, oracle122·observation499 |
| `python3 -m unittest discover -s verification/requirements -p 'test_*.py'` | 0 | 24 OK |
| `python3 -m unittest discover -s verification/coverage -p 'test_*.py'` | 1 | 43 중 3 failure, 원인은 registry 총계 |
| 같은 명령, 임시 790 | 0 | 43 OK |
| `python3 verification/model-corpus/validate.py` | 0 | VALID, usage/cost null |
| `python3 -m unittest discover -s verification/model-corpus -p 'test_*.py'` | 0 | 61 OK |
| `$MULINO_SLOT verification/model-binding/run selftest` | 0 | 58 test, failure0 |
| `$MULINO_SLOT verification/model-binding/run prepare` | 0 | PREPARED, 모델 호출0 |
| `python3 verification/cases/V2/cases_b_invariants.py` | 0 | 0 problem |
| 같은 검사를 `git archive 7b540e8f` 사본에 실행 | 1 | 65 problem: 원래 결함을 모두 보고 |

registry `expectedSubcases`는 coordinator 소유라 바꾸지 않았다. E2에
`qc-release-only-control`을 더해 실제 subcase는 790이며 coordinator가
총계를 다시 계산해야 harness·prepare·coverage가 위 임시 결과대로
통과한다.

## coordinator에게 넘기는 일

- registry `expectedSubcases`를 790(이 branch 기준)으로 다시 계산한다.
- 다른 worker 소유 사례의 비정규 오류 pointer:
  C1 `/response/errorCode` 3, T03 2, T04 11(raw-db route 포함), T05 2,
  T16 3, T18 `/response/code` 1. T20의
  `/response/body/result/structuredContent/error/code` 5개는 raw MCP wire의
  같은 envelope이라 정규 형태다. `verification/model-binding/
  semantic-paths.json`의 `response.errorCode → /response/errorCode`도
  모델 binding 소유자가 맞춰야 한다.
- harness-guide에 단일 오류 envelope과 의무 `current`=행 유효성 정의를
  적고, preparation이 비정규 오류 pointer를 거부하게 하는 일은 harness
  소유다.
- 의무 kind 어휘(E1 QC/RETURN/SETTLEMENT, C4 DELIVERY_DEFICIT, E2
  RECALL_RESPONSE, V2 SALES_PROMISE 대 S4 구현의 QUALITY_REVIEW·
  RETURN_*_REVIEW·SETTLEMENT_DIFFERENCE)를 catalog에서 확정해야 한다.
- 계획 §3.4 오류·outcome과 구현 차이: backend는 출고 정지를
  SCOPE_INELIGIBLE, 소비된 배분을 REVISION_CONFLICT, 미확인 잔여 회수
  종료를 HELD/RECALL_RESIDUAL_UNKNOWN으로 낸다. S4 native e2-flow는
  출고 거부를 HELD로 고정한다. 사례는 계획 목록과 C1·V3 관례대로
  REJECTED·INSUFFICIENT_ELIGIBLE_QUANTITY, 회수 종료는 REJECTED·
  RECALL_RESIDUAL_UNKNOWN이다. 어느 쪽을 바꿀지 결정이 필요하다.
- V2 reserve-commits-first는 contender split의 CONFLICT/STALE_REVISION을
  기대하지만 backend `FulfillmentPostgresTest`는 같은 순서의 split을
  APPLIED로 둔다. 이번 지적 범위 밖이라 바꾸지 않았다.
- T17 second-reserve·release-hold의 형식·revision 문제(cases-b-00,
  cases-b-04)와 출고 뒤 만료 subcase(P3-0)는 T17 소유자 몫이다.
- testBarrier arming 표기가 T20·V5·V6·V7에서 다르다. 계약은
  `verification/cases/V2/race-observation-contract.md`에 있다.
- 다른 case의 profile/layer 누락(T04·T09·T12·T15·T23·T25·C3·V8)과
  preparation이 필수 profile 누락을 잡게 하는 검사는 coverage 소유다.

## 하지 않은 것

- 제품 adapter·barrier·lock probe가 없으므로 경합·MCP 경로는 실행하지
  않았다. 모두 NOT_RUN이다.
- V3 출고 요청은 피킹·cargoPlaceId 없는 기존 형식을 유지했다. 거부
  사례는 구조화 code를 고정해 형식 오류와 구별된다.
- test profile 밖 barrier arming 거부의 negative subcase를 만들지
  않았다.
- `cases_b_invariants.py`를 harness나 `./verify`에 연결하지 않았다.
