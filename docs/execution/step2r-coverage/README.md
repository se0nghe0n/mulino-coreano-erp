# Step2 재검토 coverage worker 실행 기록

- 사용자 Step2(tests) 재검토 지적 수정, Claude Opus high. 통합 Task
  HEAD `7b540e8f`에서 branch `step2r/coverage`, worktree
  `/Volumes/VideoStore/Developer/mulino-ontology-step2r-coverage`.
- 소유: `verification/coverage/**`, `verification/requirements/**`,
  `verification/model-corpus/**`, `verification/model-binding/**`,
  harness `org/mulino/verification/modelbinding`(main/test), 그리고
  T20·T25·T26·C3·E1·E2·C4·V2·V3을 뺀 case의 `profiles` 선언.
- 입력 지적: `.mulino-tools/rereview/coverage.json`의 confirmed 9건,
  unverifiedP3 3건. verifier가 좁힌 주장과 조정 severity를 따른다.
- 제품 runtime, 실제 모델 호출, BTP, 규제 검토는 실행하지 않았다.
  모두 NOT_RUN이다.

## 지적별 처리

| ID | 판정 | 처리 | commit |
|---|---|---|---|
| coverage-00/01/02 (P1) | FIXED(내 범위) + CROSS_OWNER | 필수 profile에 링크 없는 관찰을 준비 FAIL로 보고. T04·T09·T12 +contracts, T23·V8 +scenarios(생성기 수정 후 재생성). T15 REGULATORY_REVIEW는 별도 검토 receipt 경로로 연결 | `4748db0`, `cbb6600`, `147c572` |
| coverage-03 (P2) | FIXED | parse된 lock은 값과 무관하게 형태·계약 검사. `{}`·`null`·`[]`·`0`은 FAIL, 부재는 NOT_RUN. 오래된 closureReview 갱신 | `4ae92b4` |
| coverage-04 (P2) | FIXED | 규범 lock `pinnedArtifacts`에 corpus SHA 고정. validate.py·generate.py(REFUSED)·run prepare·BindingContract·assembler가 pin 확인 | `0ae8261` |
| coverage-05 (P2) | FIXED | 더러운 tree면 NOT_RUN·PASS 불가. receipt `workingTreeClean`, manifest schema·validate_saved 대조 | `1cd74a5` |
| coverage-06 (P2) | FIXED | READ 답변은 `ANSWERED_READ_VALUES` coverage와 oracle의 모든 `response.*` 값(수량은 단위)을 claim으로 요구 | `d247c8c` |
| coverage-07 (P2) | FIXED | intent 불일치를 turn 지표로 기록하고 안전 검사 계속. STRUCTURED turn 62×3=186 분모로 ≥0.95 집계. missingSlots 집합, sourceText 원문 포함 발췌 허용. NEEDS_INPUT은 정확 일치 유지. Glue의 FAIL 오표기 수정 | `0e52509` |
| coverage-08 (P3) | FIXED(부분) | 참조 없는 assertion을 캡처된 bytes로 재판정해 모순되는 runner PASS를 FAIL로. CaseRunner record 확장은 CROSS_OWNER | `5fa96c0` |
| unverifiedP3-0 | CROSS_OWNER | 실제 결함 확인: `./verify coverage`는 Java Main.prepare를 부르고 exit0, Java report 필드(discoveredSubcases·selectedSubcases, actual에서 gateComplete=false 강제)는 assembler 조건(discovered/started/completed, gateComplete=true)과 맞지 않음 | - |
| unverifiedP3-1 | FIXED | 외국어 최소10을 실제 원문 단어로 셈(현재 19, tag 21) | `c6735e4` |
| unverifiedP3-2 | FIXED(부분) | 오래된 `closureReview` 갱신. lock hash의 저장소 밖 anchor는 만들지 않음 | `4ae92b4` |

T15 regulatory는 case schema enum을 바꾸지 않았다. 계획 §13.4가
법규 검토의 출처·적용일·검토자를 따로 기록하게 하므로, REGULATORY_
REVIEW 관찰에 연결된 assertion을 가진 subcase(T15 missing-*3개)만
assembler가 regulatory evidence profile에도 선언한다. 그 receipt는
`regulatoryReview`(officialSourceRef·jurisdiction·applicableDate·
reviewerId·reviewedAt, fictionalFixture=false)가 필요하다.

corpus pin을 catalog 본문이 아니라 lock에 둔 이유: E1 case.json의
`model-reference-host` inspectArtifacts가 `mandatory-oracles.json`의
SHA-256(`61968b22…`)을 입력으로 고정한다. catalog를 바꾸면 E1 실행이
hash 불일치로 깨진다. 이번 변경은 catalog bytes를 바꾸지 않았다.

## 다른 소유자에게 넘길 일

1. cases-a/cases-b 소유 case의 profile·assertion 추가. 현재 assembler
   준비 FAIL 48건은 모두 이것이다. profile 선언만으로는 부족하고
   해당 계층을 실제로 지나는 action/assertion이 필요하다(E1·E2의
   현재 action route는 api만 있다).

   | case | 추가 profile | 도달 불가 관찰 | oracle별 |
   |---|---|---|---|
   | E1 | mcp | 20 | full-flow-quantities 11, independent-goals-and-owners 7, whole-runtime-and-model-reference 2 |
   | E2 | mcp | 15 | same-25-not-50 6, overlapping-holds 5, exception-responsibility 4 |
   | T25 | mcp, skills | 7 + 3 | mcp: independent-traceability 2, evidence-manifest-and-entrypoints 2, model-corpus-and-budget 3 / skills: model-corpus-and-budget 3 |
   | C3 | skills | 3 | model-query-write-boundary 3 |

   T20·T26·C4·V2·V3은 현재 도달 불가 관찰이 없다.
2. harness core(`org/mulino/verification`, CatalogLinkValidator·Main):
   `./verify prepare`에도 같은 필수 profile 도달성 검사와 규범 lock
   검사(validate_catalog.py 호출 또는 동등 구현)를 넣는다. 1번이
   끝나기 전에 넣으면 prepare가 FAIL이므로 순서를 조정한다.
3. root `verify`·Main: `./verify coverage`가 assemble.py를 실행하고
   그 exit을 반환하게 하며, profile report 필드와 gateComplete 규칙을
   assembler 조건과 맞춘다(unverifiedP3-0).
4. CaseRunner: runtime assertion record에 op·unit·filter와 projection
   후 비교값을 넣는다(coverage-08 잔여).
5. 실제 receipt 생산자(verification/actual/** 또는 이후 runner):
   coverage receipt에 `workingTreeClean`, regulatory profile에는
   `regulatoryReview`를 기록한다.
6. coordinator: subcase 수는 바뀌지 않았다(registry 합계 영향 없음).
   `verification/platform-tests/subcase-index.json`의 assertion 수가
   생성기 재생성으로 361→371로 바뀌었다(실제 생성값).

## 실행 증거

HEAD `c6735e4c`, working tree clean(`git status --porcelain` 0줄).
환경 `. /Volumes/VideoStore/Developer/.mulino-tools/env.sh`, Java
21.0.5, Maven 명령은 `$MULINO_SLOT`로 하나씩 실행했다.

| 명령 | exit | 결과 |
|---|---|---|
| `$MULINO_SLOT ./verify harness` | 0 | 435 tests, failure/error/skip 0, BUILD SUCCESS |
| `$MULINO_SLOT ./verify prepare` | 0 | PREPARED, 41 case·789 subcase·20473 assertion, 문제0 |
| `$MULINO_SLOT ./verify contract-red verification/cases/T04/case.json` | 1 | RED NOT_IMPLEMENTED, 9 subcase, skip0 |
| 같은 명령 T09 / T12 / T23 / V8 | 1 / 1 / 1 / 1 | RED NOT_IMPLEMENTED, 9 / 5 / 10 / 12 subcase, skip0 |
| `$MULINO_SLOT verification/model-binding/run selftest` | 0 | Contract17·Evaluator15·ResponseEvidence29, failure0 |
| `$MULINO_SLOT verification/model-binding/run prepare` | 0 | corpusIntegrity VALID 후 PREPARED, runtime NOT_RUN |
| `python3 verification/requirements/validate_catalog.py` | 0 | VALID, 122 oracle·499 observation |
| `python3 -m unittest discover -s verification/requirements -p 'test_catalog.py'` | 0 | 25 tests OK |
| `python3 verification/model-corpus/validate.py` | 0 | VALID, foreignOrMixedCases 19(tag 21) |
| `python3 -m unittest discover -s verification/model-corpus -p 'test_*.py'` | 0 | 66 tests OK |
| `python3 -m unittest discover -s verification/coverage -p 'test_*.py'` | 0 | 57 tests OK(217s) |
| `python3 -I verification/model-binding/generate.py` | 0 | 바뀐 파일0 |
| `python3 verification/coverage/assemble.py` | 1 | FAIL: 준비 문제는 `Unreachable required profile` 48건뿐(위 1번). model 준비 PREPARED, runtime NOT_RUN, clearStructuredIntent null |

contract-red의 subcase 수는 wrapper가 expected=discovered=
NOT_IMPLEMENTED 실패, skip0을 확인한 뒤 exit1을 반환한 값이다.
assembler의 exit1은 회귀가 아니라 이번에 드러낸 준비 결함이다.
수정 전에는 같은 상태에서 exit2(NOT_RUN)였다.

수정 전 재현: 실제 저장소에서 도달 불가 관찰 79건(E1 20, E2 15,
T09 13, T25 10, V8 6, T23 5, T12 4, T04 3, T15 3, C3 3)과 준비
문제0. lock `{}`·`null`·`[]`·`0`은 수정 전 assembler에서 E1 80→100
약화를 통과시켰다(새 test가 수정 전 코드에서 4건 FAIL). M16 80→100
corpus 약화는 validate.py VALID, 재생성 후에도 hash 일치였다.

## 하지 않은 일

- 다른 소유자의 case(C3·E1·E2·T25), schema(`contracts/`), harness
  core, root `verify`, `verification/actual/**`, `backend/**`는 바꾸지
  않았다.
- 도달성 검사는 profile 단위다. 그 profile에서 실제 MCP·skill 경로를
  지나는 action인지는 case review가 확인한다. subcase 단위 route
  검사는 T20·V4·V6·V8에도 추가 판단이 필요해 넣지 않았다.
- M41(문서 요약)은 response 경로가 없어 답변 값을 구조로 강제하지
  못한다. M41·M49의 언어 tag 정정은 corpus·lock 변경이 필요해 하지
  않았다.
- 실제 모델·extractor가 없어 구조화율은 계산되지 않았다. 분모와
  0.95는 R8 확정 전 제안이다.
- 규범 lock hash의 저장소 밖 anchor는 만들지 않았다. 저장소 안의
  anchor도 함께 편집할 수 있어 같은 review 규율에 의존한다.
