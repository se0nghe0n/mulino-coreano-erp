# 플랫폼 계약 검증 기록

제품 부재를 oracle 실패와 구별하기 위해 T23/V8의 실제 Gherkin
selector와 공통 AssertionEngine selftest를 실행했다. 제품 인수는
완료되지 않았다. 모든 제품 profile의 결과는 NOT_RUN이다.

작성 baseline은 `step2-b2`의
`feaca0af9673620eff9a5ac0f08a657ce14e9ccd`다. 최종 검증 당시 공통
dependency HEAD는 `77c2df3440b6d8b362bfb52fed677e441c372b4c`였고
플랫폼 owned 변경은 아직 commit 전이어서 report의 dirty 표시는 true다.
검증은 2026-10-07에 지정 worktree에서 수행했다.

## 고정된 산출물

| case | subcase | assertion |
| --- | ---: | ---: |
| T23 | 10 | 189 |
| V8 | 12 | 172 |
| 합계 | 22 | 361 |

현재 catalog에 배정된 7 oracle의 16 named observation을 모두 연결했다.
정적 연결 누락과 추가 이름은 각각 0이다. 선언 연결은 제품 관측 coverage가
아니다. `evidence/declaration-links.json`에 실제 assertion source 연결을
기록했다. 공통 catalog·runner·schema·registry는 변경하지 않았다.

## 실행한 command와 관찰 결과

아래 command는 repository root에서 순차 실행했다. 각 invocation의
실제 Java/Maven/entrypoint/exit/timestamp는
`evidence/*-wrapper-commands.json`에 있고 stdout/stderr는 같은 이름의
`.txt`에 있다. 표시용 log는 tab과 trailing whitespace만 정리했고
원 stdout/stderr bytes는 같은 경로의 `.txt.gz`로 보존했다. Java는
Zulu 21.0.5+11-LTS, Maven은 3.9.16이었다.

| command | exit | 결과 |
| --- | ---: | --- |
| `./verify validate verification/cases/T23/case.json verification/cases/V8/case.json` | 0 | 두 case와 22 fixture schema 유효 |
| `./verify harness` | 0 | 149 tests, failure/error/skip 각각 0 |
| `./verify contract-red verification/cases/T23/case.json verification/cases/V8/case.json` | 1 | 실제 Gherkin 22건 전부 NOT_IMPLEMENTED RED |
| `./verify schema` | 2 | NOT_RUN |
| `./verify contracts` | 2 | NOT_RUN |
| `./verify recovery` | 2 | NOT_RUN |
| `./verify mcp` | 2 | NOT_RUN |
| `./verify deployment` | 2 | NOT_RUN |

selector 결과의 expected/discovered/started/failed/NOT_IMPLEMENTED assertion
failure는 각각 22이고 skipped는 0이다. alias 해석 오류, undefined step,
환경 오류로 RED를 대신하지 않았다. 실제 Cucumber step 결과와 22개
subcase 보고서, summary를 `evidence/contract-red/`에 보존했다.

149 tests 중 `PlatformAssertionTest`의 19 tests가 모두 통과했다.
정확한 Korean step·case schema와 아래 fixed observation mutant를
실제 AssertionEngine으로 검사했다.

- retired parent 중복 산입, 수량/단위 오류, 물리 identity 중복
- rollback delta가 0이어도 다른 baseline unit
- 의무 owner/수량/다음 확인시점 소실, 기존 v1 정의 의미와 멱등 결과 변조
- 비허용 compiler drift, archive tree hash 불일치, 제조의 수입 relabel
- LOCAL을 BTP로 대체, 누락 blob/evaluator의 허위 완료와 삭제 원문의 재등장
- auth smoke를 생략한 cutover, 외부 발주 재발행, 잘못된 SQLSTATE
- 누락 관찰·불완전 scope의 zero PASS, 옛 schema migration source,
  exact manifest의 MCP SDK 소실

`evidence/harness-summary.json`은 이 command가 실제 실행했다고 보고한
suite만 집계했다. 이전 selector 실행의 stale Surefire XML은 제외했다.
플랫폼 suite의 원 XML은 `evidence/harness-reports/`에 있다.

## 수정 전 오류와 최종 경계

이전 selector는 공통 AssertionEngine이 NOT_IMPLEMENTED 확인보다 alias와
result 해석을 먼저 수행해 wrapper exit 3을 반환했다. 이 결과는 유효한
RED로 세지 않았다. 공통 availability 선행 검사 dependency 이후에
위 전수 selector를 다시 실행했다. 이전 자체 selftest에서는 scope에 추가한
P alias가 canned alias map에 없어 오류가 났다. map을 고친 뒤 최신 19건을
모두 통과했다. 실패 log는 worktree의 `evidence/history/`에 보존했다.

실제 제품 host/API/독립 DB/blob/worker/BTP/client adapter는 구현되지
않았다. 제품 profile 보고서는 `evidence/product-profiles/`에 있으며
gateComplete는 false다. profile의 NOT_RUN은 실제 운영·BTP·지원 client
수용을 뜻하지 않는다. R1 exact selected manifest, R3 실제 inventory,
R5 cutover 책임, R7 계정·entitlement·비용 승인과 client 증거를 synthetic
fixture나 local 성공으로 대체하지 않았다.

유료 호출·실제 모델·배포는 실행하지 않았다. 옛 code/schema/tests/skills와
운영 문서는 읽지 않았다. archive 보존 후보의 Git commit 존재는
metadata만 확인했고, 실제 archive/restore와 현재 인가는 제품 Step의
별도 증거가 필요하다. 최종 registry/coverage 통합은 coordinator의 범위다.
