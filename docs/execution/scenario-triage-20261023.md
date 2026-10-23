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

## 입력 참조 수정 source의 43개 실제 결과

source `ad80e1bc821746b7b2c82782421d5ced7bfbcbc3`의 native Workflow
`wf_6c6fbaf8-81f`와 T13 재실행 `wf_814e7a4c-83c`가 완료됐다.
metadata12개는 모두 Haiku5.5다. coordinator가 원본·source·fixture·
실행 jar hash와 고유43개 및 assertion392개를 독립 대조했다.

| case | subcase PASS | subcase FAIL | assertion PASS | assertion FAIL | 미관찰 |
|---|---:|---:|---:|---:|---:|
| T09 | 5 | 4 | 34 | 0 | 60 |
| T11 | 3 | 9 | 29 | 3 | 78 |
| T12 | 0 | 5 | 14 | 35 | 16 |
| T13 | 1 | 16 | 10 | 3 | 110 |
| 합계 | 9 | 34 | 87 | 41 | 264 |

PASS9개는 각각 모든 assertion을 관찰했고 runtimeComplete=true다.
실행 전체의 runtime/prerequisite/gateComplete는 false다. T13의
decision-unauthorized가 새로 통과했지만 양성 manager 승인은 권한에서
거부됐다. T09·T11은 예약 좌표 누락, T12는 판정 조회 projection 등
후속 실패를 드러냈다. 이 source에는 수령 graph와 새 목표 정의가 아직
없었으므로 최신 통합 구현의 실패 수로 옮기지 않는다.

T13 첫 attempt는 실행 agent가22초 만에 종료하면서 진행 중이던 Maven
프로세스가 SIGTERM·exit143으로 끝났다. 제품 실패가 아닌 ENV이며 원본을
보존했다. 같은 source의 한 번 허용된 재실행이17개 terminal report를
생성했다. validator는 assertion110개 미관찰을 reportComplete=false로
잘못 표시했다. 모든 행이 있는 report와 runtime/gate 완료를 구별한다.
T13 실행 jar hash도 coordinator가 직접 재계산해 일치를 확인했다.

원본은 `t3-6f9c71e3/.local/input-contract-wave-20261023/`이고 독립
검사는 `.local/resume-20261023/input-contract-wave-coordinator-proof.json`이다.
custom watcher 두 개는 child가 중단했으며 새 실행에서는 native 완료
알림과 foreground terminal 대기를 사용한다.

## 상태·기간 목표와 확인 수령 의미의 통합

worker180918d4를31e9a4f3로 통합하고7d22ce26에서 입력을 결합했다.
기존 누적 template·capability·policy를 보존하고 held-at·held-exists·
held-throughout typed template을 추가했다. T09의 목표6개는 selector를,
기간 목표4개는 관측 정책을 명시한다. 기존 API의 유일 template 문맥
선택은 모든 mode에서 유지하며 추가 필수 입력 제한을 도입하지 않았다.

기간 목표에도 원래 요청 수량을 적용하고 경계별 판정·근거를 합친다.
현재 재고·검증 관측 구간·실제 retirement 원장을 구별하며 point나
관측 공백으로 연속 coverage를 만들지 않는다. CONFIRMED_RECEIPT 목표는
PHYSICAL_RECEIPT 원천을 읽되 기존 receipt admission의 확정 transaction을
통과한 물량만 세고, 중복이나 확인 전 원천으로 실적을 만들지 않는다.
정정 영향도 같은 event 의미를 사용한다.

coordinator의 backend72개가 한 번의 재실행에서 통과했다. 앞선20개
ApplicationContext 오류는 JWT_PUBLIC_KEY 누락이었으며 첫 로그를
보존하고 기존 로컬 검증 공개키로 재실행했다. assertion 실패는 없었다.
전체 harness601개·입력9개·runtime package6개/발견 link12개·prepare도
통과했다. prepare는41 case·802 subcase·24,248 assertion·문제0개다.
로그는 `.local/resume-20261023/`의 backend-temporal-integrated-auth-retry,
harness-temporal-graph-combined, temporal-source-setup,
runtime-packages-temporal, prepare-temporal-graph-combined 로그다.

source7d22ce26의 T01·T09·T11·T12, 고유38개·예상 assertion394개를
새 Haiku dynamic Workflow로 실행했다. T01은 공통 정의와 harness
변경의 회귀를 포함한다. 당시 관측 source의 canonical 연결은 미완성이며
아래 후속 결과와 구분한다. 원문 주장만으로 검증 구간을 만들지 않는다.
현재 통합 Step와 전체 gate는 계속 열려 있다.


## 실제 수령 frontier의 재확인과 관측 graph 결합

source7d22ce26, Workflow wf_9c782f6d-e35의4 case가 모두 terminal report를
남겼다. 고유38개 중20PASS·18FAIL이고 예상394 assertion은204PASS·
56FAIL·134미관찰이다. T01은12PASS/125assertion, T09는5PASS4FAIL/
34PASS13FAIL47미관찰, T11은3PASS9FAIL/31PASS8FAIL71미관찰,
T12는0PASS5FAIL/14PASS35FAIL16미관찰이다. retry는0회다.

coordinator가 원본 report·fixture·source·실행 jar hash와10개 실제
Haiku5.5 metadata를 대조했다. 모든 PASS 행은 전체 assertion을 관찰하고
runtimeComplete=true·missingAdapters=[]다. report의 모든 행이 terminal인
것과 전체 runtime/gate 완료는 다르며 전체 gate는 false다.
독립 증거는 `.local/resume-20261023/temporal-receipt-wave-coordinator-proof.json`이다.

T09 receive60·T11 receive90·T12 receipt는 실제 대조와 canonical 연결
후 STALE_REVISION으로 거부됐다. 원본 wireRequest는 expectedRevision=1,
제품 Observations와 공개 임시 수령 응답은0이다. 이전 입력 helper의
"저장 revision1" 주석이 틀렸다. dbc75fc6에서10개 확정 요청을 실제
임시 수령 응답 revision 참조로 바꿨다. 물리 사실·행동 ID·순서·269개
assertion은 그대로다. 입력 검사12개와 prepare가 통과했다.

관측 backend76fe11e2·graph61aa091b는 b2cf0466·409c9228로 통합했다.
원문/claim/실제 segment 범위·수량·단위·구간을 대조하며 중복 interval은
하나의 canonical identity로 연결하고 겹치는 상충 관측은 보류한다.
원문 보고 시각과 coverage 유효 구간을 구별한다. 닫힌 끝은 명시적으로
PostgreSQL microsecond 후속점으로 저장하고 point에 tail을 만들지 않는다.
source 전용 ITEM reviewer의5개 capability만 추가했으며 WORK 차원을
더하면 all-of로 거부되는 PG 반례도 통과했다. 기존 business grant와
269개 assertion 원문은 보존했다. feature 역할 변경8줄과 보조 행동8개가
승인된 범위이며 나머지 원본 줄을 보존했다.

coordinator의 결합134개 backend가04af8e3c에서 모두 통과했다.
첫133PASS/1FAIL은 새 기간 테스트의 설명 문장 source였으며 경계와
기대 결과를 보존해 JSON 원문으로 수정했다. 원래 실패 로그도 남겼다.
source12개·prepare도 통과했다. 로그는 backend-observation-temporal-combined,
backend-observation-temporal-corrected, receipt-revision-source-corrected,
prepare-receipt-revision-combined이다.

T13은9ac49b39에서15개 양성 fixture에 품목별 관리 권한30행만 더했다.
기존 business grant와123개 assertion은 보존했다. authorization probe는
HTTP/actual scenario가 아니며 새43개 Haiku 실행에서 승인 frontier를 본다.
T09/T11 예약 좌표·T12 판정 projection 등 남은 실패를 계속 추적한다.
새 source의 실행 결과를 기다리며 이전 source의 실패 수를 섞지 않는다.


## source dbc75fc6의 canonical·승인 wave

native Workflow wf_a6ac9383-20e의6개 agent metadata는 모두 Haiku5.5다.
coordinator가 고유 행·source/fixture·canonical run.sh·실행 jar·원본 보고서
hash를 대조했다. 각 case는 한 번의 시도로 terminal report를 남겼다.

| case | subcase PASS | FAIL | assertion PASS | FAIL | 미관찰 |
|---|---:|---:|---:|---:|---:|
| T09 | 5 | 4 | 34 | 13 | 47 |
| T11 | 3 | 9 | 31 | 8 | 71 |
| T12 | 0 | 5 | 14 | 35 | 16 |
| T13 | 1 | 16 | 43 | 36 | 44 |
| 합계 | 9 | 34 | 122 | 92 | 178 |

모든9PASS 행은 전체 assertion·runtimeComplete·빈 missingAdapters를
만족한다. 다만 T11 resume-without-proof는 미지원 slot의 TYPE_INVALID로
거부된 약한 PASS이며 실제 증거 guard의 인수로 쓰지 않는다.
실행 전체와 모든 profile gate는 미완료다. 독립 증거는
`.local/resume-20261023/canonical-approval-wave-coordinator-proof.json`이다.
manifest의 runEvidenceDir는 copied evidence 상위 경로였으므로 실제
execution tree의 고유 actual-scenarios-run 디렉터리까지 대조했다.
미관찰178개는 수행한 NOT_RUN scenario가 아니라 assertion 미관찰이다.

수령 revision 수정은 실제 효과까지 진행했다. T09 receive60과 T11
receive90/receipt/receive60은 APPLIED였고 T12 지연 수령도 APPLIED였다.
T09 두 기간 사례의 관측4개도 실제 canonical 연결을 통과했다.
후속 예약3건은 좌표가 없어 TYPE_INVALID였으므로 전체 leaf 근거를
대조해 startQuantity=0을 명시했다. 잘못된 창고 수령은 계속 거부됐다.

새로 드러난 getAssessment는 Work data를 반환하고 원래 pinned 정책이
설치되지 않아 기존 판정을 보류하고 있었다. 실제 불변 판정 read와
명시 assessGoal, 현재 revision 조회, 원래 EVIDENCE 정책을 연결했다.
126개 구조 바인딩은 ledger의 역변환으로 원래269개 업무 기대값과
대조한다. 현재 시간·snapshot·인가를 속이는 adapter alias는 만들지 않았다.
T01 공급 평가의 noun snapshot 회귀와 미지원 scope 거부도 실제 backend
검사에 포함했다. 미발행 arrived-at-W/due-by-T 조건은 그대로 실패 경계다.

T13의 stable external operation UUID, 현재 proposal 조회와 품목 subject를
교정했다. 공개 vocabulary에 직접 명시된 승인 HELD만 정렬하고 정확한
code guard를 더해 slot validation의 우연한 거부로 통과하지 못하게 했다.
135개 assertion은 원래123개의 수량·단위·승인 결합·외부 효과0·감사 조건을
보존한다. policy content/hash·미지원 conditional·취소 PO 정보·공급자와
부분 수령의 후속 원천은 여전히 별도 실패 경계다.

T17은 source profile370개를 설치 필드에 맞추고 새 원천 전용 profile과
실제 typed QC/규제/라벨 판단 체인을 더했다. 기한이 다른 명시 segment는
동일 LOT에 덮어쓰지 않고 별도 typed LOT으로 분리했다. coordinator의
새 합성 기한 convention과 모든 old→new 참조를 역변환 ledger로 남긴다.
운영 기한·공식 규제 승인 근거가 아니며 실제 제품 guard를 우회하지 않는다.
source35234fa6의 결합 backend156개, 전체 harness610개가 통과했다.
harness에서 별도 opt-in인 PG5개는 SKIP이고 실제 HTTP/Spring/PG 실행
5개는 따로 모두 통과했다. 입력 검사45개도 PASS다. prepare는
41 case·802 subcase·24,260 assertion·문제0개이며 실행 인수는 아니다.
증거는 `.local/resume-20261023/all-contracts-integration-proof.json`이다.

이 source에서 T01/T09/T11/T12/T13의55개·예상529개 assertion을 새
Haiku5.5 native dynamic Workflow로 병렬 실행 중이다. T17은 공개 slot와
ALT 품목 LOT 연결의 남은 입력 계약을 별도 수정하며 이번 wave에서 제외한다.
기존 실패를 비대상으로 바꾸지 않으며 전체 Step는 열려 있다.


## source baf6ea4d의 T17 입력·인도 연결

37e26e85에서 공개 입력 오류12건 중9건과 OTHERPACK의 LOT/품목 관계를
교정했다. 원래257개 업무 조건은 보존하고25개 원천 보조 행동을 더했다.
baf6ea4d는 실제 effects 응답의 dispatchId·cargoScopeId·deliveryId만
canonical UUID로 바인딩한다. 원천/기대값 분리와 현재 권한·실행 완료
검사는 유지한다.15개 assertion은 구조 경로만 역변환 ledger로 대조한다.

harness612PASS·PG opt-in8SKIP, 별도 실제 HTTP/Spring/PG8PASS,
입력17PASS와 prepare41/802/24,260·문제0개를 확인했다. 첫 full harness의
낡은 self-test capture4개 실패도 보존하며 현재 effects 모양으로 교정했다.
증거는 `.local/resume-20261023/t17-public-integration-proof.json`이다.
실제 T17의15개·257개 assertion을 별도 Haiku5.5 native dynamic Workflow로
실행했으며 source35234fa6의55개 결과와 합산하지 않는다.

이 wave의 source에는 미지원 FEFOExceptionReason·대체 evidenceId·해제
reason이 남아 있다. 후속6a95d072·f85c7b73·20c26668에서 실제 FEFO 후보
검사·대체 원문/현재 권한·사유 감사와 공개 slot/runtime 계약을 통합했다.
reason을 무시하는 allowlist, 권한 확대, 미검증 원천의 VERIFIED seed로
해결하지 않는다. 새 source의 harness612PASS·PG opt-in8SKIP, 입력17PASS와
prepare 문제0개를 확인했다. 기존 이행 전체를 포함한 backend110개와
새로 package한 source20c26668의 실제 HTTP/Spring/PG8개도 PASS다.
보류 근거·기간·권한, DISPATCH 원천, 원래 claim pending outcome과
포장 대체 판단은 아직 별도 실패 경계다.
526beabf의 판정 충돌 HELD 정렬은 실제 복수 head/no-write 회귀를 포함한
backend42개로 검증했다. 전체 인수 gate는 계속 false다.

Haiku wf_1c670b00-ec4의 실제 source baf6ea4d 결과는15FAIL·0PASS다.
assertion은6PASS·18FAIL·233미관찰이며6PASS가 전체 subcase 성공은 아니다.
coordinator가3개 실제 Haiku metadata, 고유 run 원본과 fixture/hash를
대조했다. 별도 빌드 jar의 ZIP entry bytes는 root 기준 jar와 전부 같았다.
출고7건의 INSUFFICIENT_ELIGIBLE_QUANTITY는 SELL만 있고 DISPATCH 원천이
없는 fixture에서 나온 정상 guard이며 child의 code/message 불일치 해석을
채택하지 않는다. category·dueAt의 입력 오류와 조건/효과 원행 mapping,
claim pending outcome을 각각 수정 대상으로 남긴다. 독립 증거는
`.local/resume-20261023/t17-public-wave-coordinator-proof.json`이다.
후속 원천 수정에서 추가하는 DISPATCH/보류 기간은 명시적인 새 합성
fixture 결정이며 기존 given 또는 실제 운영 허용이라고 주장하지 않는다.


## source35234fa6의 판정·승인 통합 wave

native wf_a269a513-c5c의 실제7개 agent metadata는 모두 Haiku5.5다.
coordinator가 각 case의 새 clean tree·고정 source·원본/고유 run report와
fixture·실행 jar hash·기동을 대조했다. 각 case는1회 실행했고 retry는 없다.

| case | subcase PASS | FAIL | assertion PASS | FAIL | 미관찰 |
|---|---:|---:|---:|---:|---:|
| T01 | 12 | 0 | 125 | 0 | 0 |
| T09 | 5 | 4 | 38 | 9 | 47 |
| T11 | 3 | 9 | 31 | 8 | 71 |
| T12 | 0 | 5 | 24 | 25 | 16 |
| T13 | 3 | 14 | 92 | 16 | 27 |
| 합계 | 23 | 32 | 310 | 58 | 161 |

예상 assertion은529개다. 모든23PASS 행은 전체 assertion·runtimeComplete·
빈 missingAdapters를 만족한다. T01의12개 회귀는 전부 통과했다.
명시 case 선택·partial fixture이므로 전체 gate는 false다. top-level
reportComplete key가 없다는 사실을 false나 전체 runtime 실패와 혼동하지
않는다. 원본 증거는
`.local/resume-20261023/assessment-purchase-wave-coordinator-proof.json`이다.

T13의 개정7건은 정확한 APPROVAL_NOT_CURRENT code와 금지 효과0·감사가
모두 통과했다. 남은 approval-binding 실패의 정확한 원인은 원행의
consumptionPolicy 필드 누락이다. T09의 기간·coverage·활동 수량,
T11 waitReason, T12의 불변 버전·goal·조건/input snapshot 관찰 필드를
같은 DB snapshot에서 읽도록 한정 보완한다. 초기 movement1개나 미발행
조건 이름처럼 실제 불일치인 것은 observer에서 숨기거나 만들지 않는다.

기여 startQuantity 누락, unsupported resumeEvidenceIds/originalEvidenceId,
판매 적격 원천, 정책·공급자·취소·부분 수령 입력은 별도 실패다.
T11 unsupported-evaluator의 제품 HELD/VERSION_UNSUPPORTED는 공개 어휘와
같다. 기존 REJECTED outcome/audit oracle은 protocol 정렬이 필요하며
근거 없이 제품을 바꾸지 않는다. observer writer와 T17 원천 writer를
분리해 진행하며 전체 Step는 계속 열려 있다.
