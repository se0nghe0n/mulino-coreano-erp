# 실제 시나리오 원행 분류와 후속 실행

이 기록은 전체 인수 완료가 아니다. 기준 실행의 원인 분류와 새 source의
선택 실행을 구별한다. 분류 label은 수정 순서를 위한 진단이며 제품 판정이
아니다.

## 680개 기준 실행 분류

기준 source는 `1e6a6a521142aacb74778972005e80f209d5f658`이다.
원본802개 중 PASS122개는 partial fixture이고, FAIL592개와 NOT_RUN88개를
분류했다. inventory의 NOT_IMPLEMENTED88개는 원본 NOT_RUN의 표시다.

| 분류 | 행 수 |
|---|---:|
| TEST_INPUT | 434 |
| ADAPTER | 220 |
| PRODUCT | 22 |
| EXTERNAL_GATE | 4 |
| 합계 | 680 |

native Workflow `wf_55351ea3-0fd`의 agent metadata24개가 모두
`claude-haiku-5-5`다. 최초 fan-in이 정당한 null action ID를 거부해
재작업했으며, 최종 checker는 null과 pointer 부재를 구별한다.
coordinator가 최종680개 고유 행, 원행 상태, 419개 source file SHA256과
JSON pointer1,976개를 독립 재검사했고 오류0이다. 실패 action이 기록되지
않은36개는 null을 유지한다. V7의 없는 dispatch-ready action 대신 실제
harnessError를 가리키도록 두 pointer를 수정했다.

fingerprint label은273개이고 신뢰도는 HIGH327·MEDIUM292·LOW61이다.
label이 달라도 같은 원인일 수 있다. LOW를 면제하거나 분류 결과를
현재 source의 실패 수로 전환하지 않는다. 기존 종합 markdown의 산술과
V7 문구는 최신 JSON과 다를 수 있어 인수 근거로 사용하지 않았다.

최종 원본은 `t3-c57d50ca/.local/triage-20261023/aggregate-triage.json`이며
SHA256은
`a103711e02137747f10e0dbf62e0382b3368c02ad857530d966ac4b1a52ad50d`다.
root의 독립 검사 기록은
`.local/resume-20261023/triage-coordinator-validation-final.json`이다.
이 artifact는 진단이며 coverage receipt가 아니다.

## 누적 목표 입력 통합 후 실행

`d3d4c2df7230cd3bbd096fb83732271a4c802858`에서 native Workflow
`wf_88329bcd-6b2`가 T09·T11·T12·T13을 병렬 실행했다. agent metadata6개는
모두 Haiku5.5다. T09·T12·T13의 source는 clean이고 원본 report hash를
coordinator가 대조했다.

| case | PASS | FAIL | NOT_RUN |
|---|---:|---:|---:|
| T09 | 3 | 6 | 0 |
| T11 | 0 | 0 | 12 |
| T12 | 0 | 5 | 0 |
| T13 | 0 | 17 | 0 |
| 합계 | 3 | 28 | 12 |

고유31개에서 actual action104개를 실행했다. 예상392개 assertion 중
41개가 기록됐고, 나머지는 앞선 action 실패 또는 T11 미실행 때문에
관찰되지 않았다. PASS3개의 assertion18개는 모두 원본 기대와 일치하고
각 runtimeComplete=true다. 미관찰351개를 성공이나 효과0으로 해석하지
않는다. 전체43개 실행 gate는 미완료다.

T11 agent는 backend package command 뒤에 완료 결과를 남기지 않았다.
마지막 exit0은 harness package의 값이며 backend 완료 값이 아니다.
동일 source의 T11만 새 native Workflow `wf_e7355455-033`으로 재실행한다.
기존 부분 실행 결과를 덮어쓰지 않는다.

T09의 activate2개와 T12·T13의 createWork21개는 공통 정의 검증에서
`HELD VERSION_UNSUPPORTED: Pinned predicate unsupported`를 반환했다.
이는 이번에 보완한 기간 입력 다음에 드러난 실패다. 실제 등록 handler와
기본 fixture 정의의 capability manifest 일치를 별도 수정한다.
기타 scope·conditions·destination 입력 실패도 남았다. oracle나 미지원
evaluator 반례를 바꾸어 PASS로 만들지 않는다.

원본은 `t3-0754e9b5/.local/cumulative-rerun-20261023/manifest.json`이
가리키는 세 실행 report다. root 대조 기록은
`.local/resume-20261023/cumulative-rerun-coordinator-proof.json`이다.
모든 실행은 partialFixtures=true·relaxWire=false다.

이전 중단 recovery 실행의 잔류 PID57150은 cwd·로그·run ID로 소유를
확인한 뒤 종료했고 해당 container를 정지했다. DB 자료·jar·worktree와
실패 증거는 보존했다. 정리 기록은
`.local/resume-20261023/owned-orphan-cleanup.json`이다.

전체 Task와 현재 통합 Step는 열려 있다. 실모델 인수·실제 규제·BTP
배포 인수를 주장하지 않는다.

## T11 재실행과 같은 source의 43개 결과

T11 재실행은 같은 clean source d3d4c2df에서 exit1로 완료했다.
12개 고유 subcase는 3PASS·9FAIL이며 actual action74개를 실행했다.
원본 report SHA256은
`d12ddf2e6a39d0f26ea91edb8053ed06f1b0ce34a30e6e8d8b04dbd07d881c0f`다.
root의 `t11-retry-coordinator-proof.json`에 경로와 독립 집계를 보존했다.

앞선 세 case 실행과 합하면 같은 source의 43개는 6PASS·37FAIL이다.
actual action178개이며 assertion392개 중73개가 기록됐다. 나머지319개는
앞선 action 실패로 미관찰이며 성공으로 집계하지 않는다. 앞선 부분 실행의
12NOT_RUN 기록은 보존한다. T11에서도 공통 정의 검증 거부와 별도 slot·
조건 오류가 드러났다. 전체 gate는 여전히 미완료다.

## 공통 정의·예약 입력 통합 후 70개 실제 실행

clean source `66e4d068017753147017f5d08062ba460e4e385c`에서
native Workflow `wf_38ad6525-680`이 여섯 case를 병렬 실행했다.
각 실행은 별도 worktree에서 정상 빌드했고 실제 agent metadata14개가
모두 Haiku5.5다. coordinator가 case/fixture/report hash, source commit,
실행 jar hash, 원본 assertion ID와 runtimeComplete를 독립 대조했다.

| case | subcase PASS | subcase FAIL | assertion PASS | assertion FAIL | 미관찰 |
|---|---:|---:|---:|---:|---:|
| T01 | 12 | 0 | 125 | 0 | 0 |
| T09 | 5 | 4 | 34 | 0 | 60 |
| T11 | 3 | 9 | 29 | 3 | 78 |
| T12 | 0 | 5 | 10 | 26 | 29 |
| T13 | 0 | 17 | 4 | 3 | 116 |
| T17 | 0 | 15 | 7 | 17 | 233 |
| 합계 | 20 | 50 | 209 | 49 | 516 |

고유 subcase70개와 예상 assertion774개는 누락·중복 없이 일치한다.
PASS20개는 각각 필요한 assertion이 모두 기록됐고 runtimeComplete=true,
missingAdapters=[]다. 나머지516개 assertion은 미관찰이며 성공이나 효과0이
아니다. 모든 실행이 explicit selection·partialFixtures=true·relaxWire=false이고
gateComplete=false다. 따라서 전체802개 인수 비율로 환산하지 않는다.
T01 source verifier도 원천 연결96개를 다시 통과했다.

T12의 child aggregate는 NOT_RUN으로 표시했지만 원본 scenarios.json의
status는 FAIL이고 다섯 subcase가 실제 실행됐다. 위 표는 원본을 따른다.
T17의 child result는 executedJarPath에 harness jar를 잘못 적었다.
보존된 canonical run.sh의 backend jar 복사·hash·실행 순서, runtime 시작
로그와 새 backend jar hash가 모두 일치해 제품 실행을 확인했다.
T13의 jar 경로 누락도 같은 hash·시작 로그 연결로 확인했다.
원본 child artifact는 덮어쓰지 않는다.

원본은 `t3-e936866a/.local/definition-reserve-wave-20261023/`의
aggregate.json이 가리키는 여섯 scenarios.json이다. coordinator의 보정·
원본 hash·jar binding·실패 위치는
`.local/resume-20261023/definition-reserve-wave-coordinator-proof.json`에
보존했다. 이 검사는 새 acceptance receipt를 발행하지 않는다.

이전에 수정한 setup45개도 별도로 추적했다. 목표 요청38개는
35APPLIED·3미도달이며 같은 d3d4c2df source의 이전 실행과 비교하면
32개가 HELD에서 APPLIED로 바뀌었다. 세 미도달은 T11의 앞선 수령
실패 뒤 followup이다. 예약7개는 startQuantity 형식 거부를 지나
INSUFFICIENT_ELIGIBLE_QUANTITY에 도달했다. 해당 fixture의
baseline.conditions와 sourceProvenance가 설치되지 않은 사실을 확인했다.
좌표 수정으로 판매 적격성까지 증명한 것은 아니다.

T09 lot-not-yet-required와 receipt-missing-lot 두 subcase는 FAIL에서
PASS로 바뀌었다. 다른 failure frontier는 공개되지 않은 수령/관측 slot,
T13의 잘못된 goal ID 참조, 누락된 상태·기간 definition, 조회/독립 관찰
projection이다. 첫 실패 해소와 전체 subcase PASS를 구별한다.

## 후속 입력·참조 수정의 통합

T13 worker73a64ccd를 7b617659로 통합했다. 구매 제안16개는 생성 응답의
goalVersionId와 최신 revision을 참조하고, 공급자 답변3개는 주문 ID와
revision을 참조한다. 미공개 originalQuantity 사본은 기존 주문에서 읽도록
요청에서 제거했다. 주문 물량·답변·문서·actor·grant와 assertion123개는
그대로다. 집중 검사4개가 통과했고 실제 실행 결과는 아직 없다.

T09·T11·T12 worker d4884b90를 d9faa36a로 통합했다. 관측9개를 공개
EVENT 형식으로 옮기고 원문이 있는8개는 attachEvidence를 먼저 보낸다.
문서 없는 반례는 계속 문서가 없다. 별도 고객 인도 Place와 명시적 period를
보완했다. assertion269개와 기존 action·음성 probe는 보존했다.
양성 수령10개의 실제 source graph 연결은 아직 완료하지 않았다.
동적 ID를 원문 JSON·bytes·hash로 만드는 runner 경계와 상태/기간
definition, 확인 수령 event 의미를 연결해야 한다.
[정확한 입력 경계](../../verification/cases/T09/source-contract-frontier.md)를
보존하며 builder의 정적 검사를 실제 실행으로 표시하지 않는다.

coordinator의 결합 검사는 원천 mapping6개·기간4개·T13 참조4개가
통과했다. 실제 receipt matcher는 원문을 허용하고 필수 필드 누락·불일치
18개를 거부했다. prepare는 계속41 case·802 subcase·24,248 assertion이다.
T13의 승인된 참조 수정에 맞춰 기간 검사 hash lock만 갱신하고, 별도
T13 검사에서 이전 source와 assertion/action의 차이를 계속 제한한다.
backend와 harness source는 바뀌지 않아 앞선 통과 검사를 반복하지 않았다.

현재 통합 Step는 열려 있다. 실제 source graph·typed definition·적격성
prerequisite와 재실행 증거를 결합한 뒤 다음 gate를 판단한다.

## 동적 원문과 실제 수령 명령 graph 연결

source ad80e1bc 기준 worker9888f36a를17f9bf7d로 통합했다. 수령9개는
원문 첨부·PHYSICAL_RECEIPT EVENT·임시 수령·원천 대조·canonical 연결·
확정 요청으로 구성한다. LOT 없는1개는 원문/event/임시 수령 뒤 원래의
부정 probe를 보낸다. 양성 명령 구성이 실제 인수 PASS를 뜻하지 않는다.

입력 전용 `$encode`는 실제 alias/result identity를 해석한 뒤 같은
정렬 JSON의 UTF-8 bytes에서 payload·base64·SHA256을 만든다.
65,536 bytes·깊이32·node4,096 상한을 두며 assertion expected/identity,
관측 business 값 복사와 임의 실행·산술·중첩 변환은 허용하지 않는다.
새 synthetic receiptSource actor는 ITEM=P all-of scope와 원천 처리6개
capability만 갖고 원래 확정 actor의 권한을 대신하지 않는다.

coordinator가27개 변경 파일을 검토했다. assertion269개의 원문 block,
기존 business actor/grant·실물 사실·evidence·책임과 feature 줄을 보존했고
T13 tree도 불변이다. 대조는
`.local/resume-20261023/source-graph-integration-proof.json`에 남겼다.

prepare가 임시 수령의 운송 정보를 추적하지 못한 한 건은 worker의
한정 후속eb503e55를fd6bfb4f로 통합해 해결했다. 엄격한 선행 순차 명령
참조만 인정하고 직접 수령·위조·미래·병렬 참조의 거부는 유지한다.
coordinator의 전체 harness601개와 source mapping8개 검사가 통과했다.
prepare도41 case·802 subcase·24,248 assertion에서 문제0개로 통과했다.
이는 단위·계약 검사이며 새 source의 실제 scenario 실행은 아직 없다.
공통 상태/기간 definition과 확인 수령의 목표 의미 연결은 별도 구현 중이다.
