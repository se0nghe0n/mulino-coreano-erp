# Step2 재검토 수정: T20·T25·T26·C3

Opus xhigh·Fable low 재검토가 확인한 지적 16건과 미검증 P3 3건을
수정했다. 기준은 Task HEAD `7b540e8f`, branch `step2r/cases-a`다.
핵심은 틀린 제품이 통과하거나 맞는 제품이 실패하는 oracle을 없애는
것이다. 계획 §3.3·§7·§9·§10·§13 oracle을 낮추지 않았다.
제품·host·모델·규제·BTP 실행은 하지 않았으며 모두 NOT_RUN이다.

## 바꾼 것

| 지적 | 처리 | 근거 |
|---|---|---|
| cases-a-00/02/06 T25 NOT_RUN 고정 | `inputSnapshotKind` 도입. 준비 사실은 `prepare.json`, runtime 사실은 실제 manifest. runtime link PASS·exit0, mutant PASS→FAIL 전이 | plan §11 S6, §13.4 |
| cases-a-01 C3 주 효과 원천 미관찰 | capability별 `CAP_EFFECTS` 원천을 조직 전체 scope로 전후 비교, reader command COMMITTED0 | plan §4.1·§6·§13.2 C3 |
| cases-a-03 T26 retry payload 신원 | retry는 `slots={commandId, reason}`만. 위조 actor·hash negative 추가 | plan §3.3, §7.2 |
| cases-a-04 T20 여섯 skill 동일 | skill별 문장·필수 QUERY(subset)·금지 command·업무별 의무 | plan §9.2 |
| cases-a-05 T26 자율 scheduling | harness tick 없는 자연 tick 관찰 subcase3, 30초 제한, DB attempt 출처 | plan §10 |
| cases-a-07 C3 closeRecall 선행 사슬 | ADMIN 승인→통지→회수20→폐기20→admin 종료, 긴급 재배정 독자 payload | plan §6, §13.1 D18, §13.3 E2 |
| cases-a-08 T20 generator drift | 수동 수정 이관 뒤 재생성, tools/list raw·HTTP·resultType 단언, 재현 test | — |
| cases-a-09 T20 오류 oracle | 공식 -32020/-32022/-32602, data.supported/requested, 401/403 body 비요구, invalid clientInfo | MCP 2026-07-28 basic·transports |
| cases-a-10 T20 RECORD 쓰기 | COMMAND·RECORD 각각0, 근거·의무·관계 전후 비교, principal 고정 | plan §9.2 |
| cases-a-11/12 C3 model-query 공허 | tools/list 전체·QUERY 호출·문장별 필요 조회·조회 감사 | plan §9.2, §13.2 |
| cases-a-13 allowed-tools-write | frontmatter 설치·쓰기 요청·실제 제출·서버 FORBIDDEN | plan §9.2 |
| cases-a-14 MRTR 결속 | TTL600 경계 쌍, 변형별 코드, method 변형 재구성 | plan §9.1 |
| cases-a-15·P3-0 key 공유 | counter-call 별도 key, payload·revision 동일 | plan §7.3 |
| P3-1 clientInfo·코드 | 코드 고정은 반영. 계획·catalog 문장은 소유 밖 | — |
| P3-2 관찰 이름 | 선행·정상 업무 assertion을 해당 효과 관찰에 연결 | — |
| catalog layer | C3에 `skills`, T25에 `mcp`·`skills` profile 추가 | mandatory-oracles requiredLayers |

생성물은 생성기로만 다시 만들었다. T01·T20·T25는
`verification/mcp-tests/author_cases.py`, C3는
`verification/cases/C3/author_prerequisites.py`, T26은 새
`verification/cases/T26/author_review_fixes.py`다. 뒤 두 script는
결정적 post-processor이며 같은 입력에서 다시 실행해도 같은 결과다.
`verification/mcp-tests/test_generators_reproduce.py`가 세 생성기의
출력과 commit된 파일을 byte 단위로 비교한다.

공유 harness test 네 개를 바뀐 assertion에 맞추고 반례 test를 추가했다.
ChannelsContractTest(+5), AuthorityPrerequisiteAssertionsTest(+2와 key
비교 수정), AuthorityAssertionsTest(+2), RuntimeAssertionTest(+1과
T26+V5 subcase 수 24→29)다.

| case | 전 subcase/assertion | 후 subcase/assertion |
|---|---|---|
| C3 | 309 / 11277 | 309 / 12132 |
| T20 | 56 / 983 | 58 / 1125 |
| T25 | 22 / 1644 | 22 / 1694 |
| T26 | 20 / 558 | 25 / 689 |

registry는 T20·T26 `subcaseIds`만 고쳤다. `expectedSubcases`는
coordinator가 다시 계산한다. 이 브랜치의 실제 합계는 796이다(789+7).

## 실행 증거

환경은 `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`, Java
21.0.5, Maven wrapper, Python 3다. Maven·verify는 `$MULINO_SLOT`으로
하나씩 실행했다.

| 명령 | registry 합계 | exit | 결과 |
|---|---|---|---|
| `./verify prepare` (변경 전) | 789 | 0 | PREPARED, 41/789/20473, 문제0 |
| `./verify harness` | 789(commit값) | 1 | 442 test, 실패2: CoverageSchemaTest 2개, 원인은 합계 불일치 |
| `./verify harness` | 796(임시) | 0 | 442 test PASS, failure/error/skip0 |
| `./verify prepare` | 789(commit값) | 1 | FAIL, 문제1개 `Case/subcase discovery count differs from registry` |
| `./verify prepare` | 796(임시) | 0 | PREPARED, 41/796/21651, 문제0 |
| `./verify contract-red verification/cases/T20/case.json` | 789 | 1 | 58/58 발견·시작·NOT_IMPLEMENTED, skip0 |
| `./verify contract-red verification/cases/T25/case.json` | 789 | 1 | 22/22, skip0 |
| `./verify contract-red verification/cases/T26/case.json` | 789 | 1 | 25/25, skip0 |
| `./verify contract-red verification/cases/C3/case.json` | 789 | 1 | 309/309, skip0 |
| `python3 verification/requirements/validate_catalog.py` | — | 0 | structure VALID, 122 oracle/499 observation |
| `python3 -m unittest … -p 'test_catalog.py'` | — | 0 | 24 PASS |
| `python3 verification/model-corpus/validate.py` | — | 0 | VALID |
| `python3 -m unittest … model-corpus -p 'test_*.py'` | — | 0 | 61 PASS |
| `verification/model-binding/run selftest` | — | 0 | 58 PASS |
| `verification/model-binding/run prepare` | — | 0 | preparationStatus PREPARED |
| `python3 -m unittest … coverage -p 'test_*.py'` | 789 | 1 | 43 중 3 실패, 모두 합계 불일치로 준비 FAIL |
| `python3 -m unittest … coverage -p 'test_*.py'` | 796(임시) | 0 | 43 PASS |
| `python3 verification/coverage/assemble.py --check-preparation` | 796(임시) | 2 | preparation PREPARED, runtime NOT_RUN |
| `python3 verification/coverage/validate.py` | 796(임시) | 0 | VALID, runtime NOT_RUN |
| `python3 -m unittest … mcp-tests -p 'test_*.py'` | — | 0 | 3 PASS, T20 값 하나를 바꾸면 실패 확인 |

임시 합계796은 확인용으로만 썼고 commit한 registry는 789 그대로다.

## 하지 않은 것과 넘길 일

- 제품 runtime·실제 host·모델·규제·BTP는 실행하지 않았다. NOT_RUN이다.
- verifyCoverage의 입력 종류, T26의 observe-only tick 의미, C3의
  `includeDescendants`, `contracts/mcp-errors.md`는 공통 harness·계약
  소유자가 host 관찰 계약과 validator에 반영해야 한다.
- V4의 같은 20개 원천과 key 재사용, T22·V7·V4의 retrySafeCommand
  입력 모양 통일은 각 case 소유자의 일이다.
- 계획 §9.1·catalog의 clientInfo 문장과
  `verification/platform/protocol/compatibility-repair.md`의 틀린 설명은
  해당 소유자가 고쳐야 한다.
- C3 positive counter-call 중 회수·긴급 재배정 밖 capability의 일반 slot이
  각 업무의 실제 입력 계약과 맞는지는 이번 범위에서 전수 검토하지 않았다.
