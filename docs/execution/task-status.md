# 전체 구현 Task의 실행 기록

기록일: 2026-10-08. 상태는 실행 증거로 갱신한다. 계획 문서 검토
완료와 새 시스템 구현 완료는 서로 다른 판정이다.

## Task와 기준선

- Task: [구현 계획](../ontology-implementation-plan.md)의 채택된
  D01–D26·C1–C5 전체를 구현하고 T/C/V/E와 운영 인수를 완료한다.
- fork remote: `git@github.com:se0nghe0n/mulino-coreano-erp.git`이다.
- Step 1 baseline: `847758afa6cb58e2a4c661e578773f1531098ee8`이다.
  이 baseline의 tracked 파일은 온톨로지 문서 7개와 README다.
- 로컬 main의 baseline fast-forward는 사용자 승인으로 수행했다.
  원격 main push와 PR merge는 이 기록 시점에 미실행이다.
- 옛 구현은 새 작업 트리에서 제거했다. Git 이력은 보존하며
  옛 코드·tests·schema·skills·운영 문서를 읽거나 재사용하지 않는다.
  원본 #57의 테스트 방법론만 새 업무에 맞게 참고한다.
- 새 Task는 사용자 답변 `track locally.`에 따라 로컬 Git과 이 기록으로
  추적한다. [R2 결정](decisions.md)을 확정했고 [이슈 초안](implementation-issue.md)은
  로컬 범위 기록으로 유지한다. 원본 board/Phase를 새 Task로 간주하지 않는다.
- 실제 authoritative DB·blob·외부 효과·미해결 업무 유무는 S0의
  별도 inventory 대상이다. 파일 제거로 자료가 없음을 추론하지 않는다.

## 사용자 작업 순서

| Step | 작업 | 실행 모델 / effort | 상태 |
|---|---|---|---|
| 1 | 새 skills와 실행 지침 | GPT-6.1 Sol / high | COMPLETE |
| 2 | 전체 계획의 tests | GPT-6.1 Sol / high | COMPLETE |
| 3 | 새 시스템 구현 | GPT-6.1 Sol / medium | ACTIVE |
| 4 | 실제 E2E | GPT-6.1 Sol / low | PENDING |
| 5 | 패턴 분석·refactor | GPT-6 Astra / high | PENDING |
| 6 | 검증된 운영 매뉴얼 | GPT-6 Astra / low | PENDING |
| 7 | 미완료·실패 해소 반복 | 해당 작업의 모델 / effort | PENDING |

매 Step의 필수 review는 GPT-6.1 Sol `xhigh`와 GPT-6 Astra `low`다.
모든 산출물 통합·지적 수정·결합 checks까지 끝나야 Step를 닫는다.
Step 1의 정적 checks와 두 실제 reviewer의 closure가 `fe0d8df`에서
통과했다. 필수 지적이 모두 해결돼 Step 1은 `COMPLETE`다. 이 기록의
통합·최종 일치 검사는 coordinator가 수행했다. R2는 로컬 추적으로
확정했으며 Step 2를 시작한다. [Step 1 검토 기록](step-1-review.md)을 따른다.

## Step 1 소유권과 통합

Task integration branch는 `feat/ontology-implementation`이고 경로는
`/Volumes/VideoStore/Developer/mulino-coreano-ontology`다.
모든 Step 1 worker는 위 baseline의 독립 branch/worktree에서 작업한다.

| Subtask | branch | 절대 worktree | 소유 범위 |
|---|---|---|---|
| 개발 skills | `step1/ontology-skill` | `/Volumes/VideoStore/Developer/mulino-ontology-step1-domain` | `ontology-implementation` |
| Agent skills | `step1/agent-skill` | `/Volumes/VideoStore/Developer/mulino-ontology-step1-agent` | `ontology-agent-implementation` |
| 테스트 skills | `step1/scenario-skill` | `/Volumes/VideoStore/Developer/mulino-ontology-step1-testing` | `ontology-scenario-testing` |
| workspace 지침 | `step1/workspace-guide` | `/Volumes/VideoStore/Developer/mulino-ontology-step1-workspace` | root 지침·README·execution·templates |

workspace 지침의 소유 파일은 root `AGENTS.md`, `CLAUDE.md`, `.gitignore`,
`README.md`, `docs/execution/*`와 새 `.github` templates다. 다른 worker의
skills 파일은 변경하지 않는다. 다음 commit을 coordinator가 통합했다.

| 산출물 | worker commit | Task branch commit |
|---|---|---|
| Agent skill | `5def7b4` | `18514d5` |
| 개발 skill | `489d` | `3f81593` |
| 테스트 skill | `29cf` | `0461587` |
| workspace 지침 | `c7a814f` | `d7c1cde` |
| 이슈 완료 조건 절 | `2fe7263` | `bde2cee` |
| refactor·운영 매뉴얼 단계 교정 | `1f50779` | `c8a29c7` |
| E1 판매 주문·예약 선행 절차 교정 | `33ff9cc` | `fe0d8df` |

기록 기준의 Task HEAD는 `fe0d8df`다. E1 template의 판매 주문·30 예약
선행 절차 교정도 통합했다. Astra low는 이 HEAD에서 두 P2의 해결을
확인하고 closure PASS를 반환했고 Sol xhigh도 같은 HEAD에서 PASS를
반환했다. 모든 skills는 아직 실제
애플리케이션·MCP/client 호출로 인수한 것이 아니다.

## Step 2 소유권과 현재 통합 gate

Step 2의 최초 공통 baseline은 `cef526e9f77679d8429fb517184f018d64824443`다.
아래 writer는 각각 독립 worktree에서 실제 GPT-6.1 Sol high로 수행한다.

| Subtask | branch / 절대 worktree | 소유 범위 | 상태 |
|---|---|---|---|
| 공통 harness | `step2/test-harness` / `/Volumes/VideoStore/Developer/mulino-ontology-step2-harness` | `contracts`, harness·base fixture·wrapper | `611330d`까지 통합, 공통 gate PASS |
| 실모델 corpus | `step2/model-corpus` / `/Volumes/VideoStore/Developer/mulino-ontology-step2-model-corpus` | `verification/model-corpus` | `c7aad7e` 통합, scoped closure PASS |
| 독립 oracle 목록 | `step2/oracle-catalog` / `/Volumes/VideoStore/Developer/mulino-ontology-step2-oracles` | `verification/requirements` | `7f972dd` 통합, scoped closure PASS |

공통 계약과 독립 oracle 목록·host 관찰·준비 검사를 통합했다.
`611330d`의 결합 harness91개가 통과했다. 이 결과와 인계 기록을 담은
로컬 tag `step2-b2`를 B2로 고정하고 모든 사례별 writer와 model-binding
writer에 같은 기준점의 독립 worktree를 배정한다. 원격 tag는 만들지 않는다.

Sol xhigh와 Astra low의 설계 사전 검토는 관찰/control 미지원의 효과0
오인, 비동기 경합의 순차 실행 오인, snapshot 불일치, UAT 정답 주입,
필수 subcase 누락과 wire 정규화 위험을 지적했다. 해당 경계를 harness와
oracle 목록에 반영해 실행 검증한다. 이 사전 검토는 전체 Step 2 코드의
최종 review가 아니다. 최종 통합 뒤 같은 두 모델/effort로 다시 검토한다.

## B2의 병렬 작성 소유권

B2는 `feaca0af9673620eff9a5ac0f08a657ce14e9ccd`이며 로컬 tag는
`step2-b2`다. 아래 worktree를 모두 같은 commit에서 생성했다. 실제
GPT-6.1 Sol high writer가 각각 독점 경로를 작성하며 coordinator만
Task branch에 통합한다. 공통 파일은 각 writer가 임의 변경하지 않는다.

| Subtask | branch | 절대 worktree | 소유 범위 |
|---|---|---|---|
| definitions | `step2/cases-definitions` | `/Volumes/VideoStore/Developer/mulino-ontology-step2-definitions` | T02·T07·T21·V1 |
| inventory | `step2/cases-inventory` | `/Volumes/VideoStore/Developer/mulino-ontology-step2-inventory` | T03·T04·T05·T16·C1·V2·V3 |
| work | `step2/cases-work` | `/Volumes/VideoStore/Developer/mulino-ontology-step2-work` | T09·T10·T11·T12·C2·C5 |
| supply | `step2/cases-supply` | `/Volumes/VideoStore/Developer/mulino-ontology-step2-supply` | T13·T14·T15·T19 |
| sales | `step2/cases-sales` | `/Volumes/VideoStore/Developer/mulino-ontology-step2-sales` | T17·T18·C4·E1·E2 |
| authority | `step2/cases-authority` | `/Volumes/VideoStore/Developer/mulino-ontology-step2-authority` | T08·C3·V4·V6·V7 |
| evidence | `step2/cases-evidence` | `/Volumes/VideoStore/Developer/mulino-ontology-step2-evidence` | T06·T22·T24 |
| runtime | `step2/cases-runtime` | `/Volumes/VideoStore/Developer/mulino-ontology-step2-runtime` | T26·V5 |
| platform | `step2/cases-platform` | `/Volumes/VideoStore/Developer/mulino-ontology-step2-platform` | T23·V8와 platform-tests |
| channels | `step2/cases-channels` | `/Volumes/VideoStore/Developer/mulino-ontology-step2-channels` | T01·T20·T25와 mcp/skills-tests |
| model-binding | `step2/model-binding` | `/Volumes/VideoStore/Developer/mulino-ontology-step2-model-binding` | M01–M60 registry·binding·runner |
| coverage | `step2/coverage-integration` | `/Volumes/VideoStore/Developer/mulino-ontology-step2-coverage` | 전체 준비/실행 evidence manifest assembly |

case writer는 자신의 case 디렉터리와 고유 JUnit package를 소유한다.
model-binding은 별도 M60 계약과 package, coverage는
`verification/coverage`와 고유 package만 소유한다. coverage writer는
runtime manifest를 만들되 registry의 최종 case/subcase 등록은 통합 뒤
coordinator가 확인한다. 상세 인수 조건은 [작성 인계](step-2-case-handoff.md)다.

공통 runtime manifest의 출력 경로는
`verification/harness/target/evidence/runtime-manifest.json`이다.
실제 입력 artifact·hash·실행 여부를 연결하며 부재를 성공이나0으로
채우지 않는다. assembly 도구는 통합했지만 실제 제품 인수는 아직
NOT_RUN이다. 전체 검토와 fresh assembly 결과는 아래 기록을 따른다.

B2 이후 실제 사례가 드러낸 공통 보완은 `ac616a1`과 `b7ca0a0`으로
통합했다. raw wire async, 실제 두 ID 참조, 미실행 reference 판정 순서와
수량 observation의 보조 관계 연결을 교정했다. 결합 harness130개가
통과했고 모든 작성 worktree에도 같은 dependency를 반영했다.
파일 한도 환경 오류가 발생해 긴 검증은 동시2개로 조정했다. 전체
Step2 완료와 제품 gate는 아직 판정하지 않았다.

## Step 2 전체 통합과 검토 수정

10개 영역의 case와 model-binding, coverage assembler를 Task branch에
통합했다. `9b5e5d7`에 전체41 case와785개 필수 subcase의 registry를
고정했다. 독립 catalog는122 oracle·499 observation이며 D01–D26을
연결한다. 별도 모델 corpus는60 case·73 turn·221 공통 assertion과
154 공통 semantic path, negative 경로의 추가4 path를 포함한다.

`9b5e5d7`에서 결합 harness320건이 failure/error/skip 없이 통과했다.
첫 전체 preparation은 고정 수량의 primary assertion3개가 없어
FAIL이었다. `f426712`에서 T03 원천40과 C1 신규 예약·출고0의 실제
수량·단위 검사를 보강한 뒤 preparation은41 case·785 subcase·
19996 assertion, 문제0으로 PREPARED다. 실패와 수정 후 증거를 모두
[evidence/step2-integrated](evidence/step2-integrated/summary.json)에 남겼다.
이 preparation은 의미 검토나 실제 제품 실행의 PASS를 뜻하지 않는다.

실제 GPT-6.1 Sol xhigh와 GPT-6 Astra low가 같은 `9b5e5d7`에서
전체 Step2를 분담 검토했다. Astra의9개 새 지적과 Sol의 추가 지적을
[전체 검토 기록](step-2-review.md)에 남기고 수정 중이다. 각 수정
writer는 같은 `f426712`의 독립 worktree에서 GPT-6.1 Sol high로
작업한다. coordinator가 통합하고 두 reviewer의 closure와 결합
checks까지 통과한 뒤에만 Step2를 닫는다.

| 수정 Subtask | branch / 절대 worktree | 소유 범위 |
|---|---|---|
| 공통 계약·timeout | `step2/review-common` / `/Volumes/VideoStore/Developer/mulino-ontology-step2-review-common` | host schema·validator·CaseRunner·공통 tests |
| host 참조·복구 | `step2/review-case-contracts` / `/Volumes/VideoStore/Developer/mulino-ontology-step2-review-case-contracts` | T26·V5·T14와 고유 tests |
| 승인·물량·QC | `step2/review-business` / `/Volumes/VideoStore/Developer/mulino-ontology-step2-review-business` | V3·T20·E2와 작성 도구·고유 tests |
| 모델 집계 | `step2/review-coverage` / `/Volumes/VideoStore/Developer/mulino-ontology-step2-review-coverage` | coverage assembler·고유 tests |
| 수락 주체 | `step2/review-handover` / `/Volumes/VideoStore/Developer/mulino-ontology-step2-review-handover` | T10·T11와 고유 tests |
| 인가 positive 전제 | `step2/review-authority` / `/Volumes/VideoStore/Developer/mulino-ontology-step2-review-authority` | C3와 고유 tests |
| 실제 DB 경합 | `step2/review-locks` / `/Volumes/VideoStore/Developer/mulino-ontology-step2-review-locks` | V8와 고유 tests |
| 적용 사실 증명 | `step2/review-outcomes` / `/Volumes/VideoStore/Developer/mulino-ontology-step2-review-outcomes` | C4·T17와 고유 tests |
| 실제 모델 응답 연결 | `step2/review-model-binding` / `/Volumes/VideoStore/Developer/mulino-ontology-step2-review-model-binding` | model-binding와 고유 tests |

root `./verify model`과 `./verify coverage`는 아직 별도 model-binding
runner와 전체 coverage assembler에 연결되지 않았다. Step2에서는
각 README의 별도 명령으로 준비·RED를 검증하며, 실제 adapter와
통합 entrypoint는 Step3에 구현한다. 제품 runtime은 NOT_RUN이다.

## 시스템 S0–S6의 별도 gate

| 시스템 Step | 인수 범위 | 상태 |
|---|---|---|
| S0 | 추적·stack spike·schema/auth/MCP 기준선 | ACTIVE |
| S1 | core·정의·증거·신원·두 진입점 읽기 | NOT_RUN |
| S2 | 목표·책임·거래·승인·idem·감사·복구 | NOT_RUN |
| S3 | 구매·운송·수입·수령·QC | NOT_RUN |
| S4 | 판매·반품·회수·정산·E1 | NOT_RUN |
| S5 | MCP·skills/client/model·정의 전환 | NOT_RUN |
| S6 | 운영·BTP·upgrade·restore·cutover | NOT_RUN |

사용자 Step 1의 skills 파일은 S5 인수의 준비물이다. 문서와 manifest
존재는 서버 구현·client loading·실제 호출·운영 인수를 대신하지 않는다.
Java21/CAP/Maven/PostgreSQL은 후보이며 exact 조합은 S0에서 검증한다.
`./verify harness`의 실행을 확인했고 실제 제품 profile은 adapter 부재로
`NOT_RUN`이다. startup과 운영 인수는 아직 확인하지 않았다. 운영
매뉴얼은 사용자 Step 6의 검증 후 산출물이다.

## 필수 인수와 미결정값

D01–D26, T01–T26, C1–C5, V1–V8, E1/E2 모두 구현/실행 `NOT_RUN`이다.
세부 oracle과 해당 S gate는 계획 §13을 따른다. 실모델·규제·BTP
인수를 로컬 결정적 tests나 논리 review로 대신하지 않는다.

R2는 [결정 기록](decisions.md)에서 로컬 추적으로 확정했다. R1 stack,
R3 실자료, R4/R6 정책, R5 실제 신원, R7 BTP, R8 모델 비용과 수용치는
각 gate의 실제 증거로 확정해야 한다. R8의 추가 UAT 비용 질문은 아직
답변 대기다. 경과 시간을 동의로 취급하지 않는다. 이와 독립적인
Step 2 테스트 작성은 계속한다. 알려지지 않은 정책을 임의 허용하지
않으며 필수 gate를 비대상으로 바꾸지 않는다.

## 실행 증거와 다음 행동

workspace worker는 `git log -1`로 baseline과 `git remote -v`로 fork
remote를 확인했다. coordinator는 Java21.0.5와 Docker29.4.0을 확인하고
Docker daemon을 시작했다고 전달했다. 활성 container는 없었으며
`mvn`은 PATH에 없었다. 이는 도메인·MCP·BTP 실행 인수 증거가 아니다.
Maven wrapper는 후속 구현의 준비 대상이다.

workspace worker의 `git diff --check`는 통과했다. Python 정적 검사로
새 문서의 상대 링크, `CLAUDE.md → AGENTS.md` symlink, PR의 네 절과
세 checklist, feature template과 이슈 초안의 다섯 절을 확인했다.
`git check-ignore`로 `.env`와 Maven `target`은 제외되고 deployment와
verification manifest는 제외되지 않음을 확인했다. 이 결과는 workspace
Subtask의 정적 증거이며 전체 Step 1 aggregate PASS가 아니다.

coordinator가 3개 skill의 `quick_validate.py`를 uv+PyYAML 환경에서
실행해 모두 PASS했다고 전달했다. aggregate Python 검사에서 새 문서
14개·상대 링크42개·symlink4개·evidence JSON의 NOT_RUN 필드와 PR/이슈
절 구조를 확인했고 baseline 대비 `git diff --check`도 PASS였다.
fresh 검사 결과는 [정적 검사 증거](evidence/step1-static-checks.txt)에
기록했다. 두 실제 reviewer의 최종 범위 내 판정도 PASS다.

아래 과거 기록 이후 Step2의 지적 수정·통합과 결합 검사를 완료했다. [공통 계약 검토 기록](step-2-common-review.md)은 이전 부분
검토이며, [전체 검토 기록](step-2-review.md)이 현재 gate를 추적한다.
startup·도메인·DB·MCP/client·모델·BTP 인수는 NOT_RUN이다.

Step 1 기록 통합과 최종 일치 검사는 `393cb5c`에서 완료했다. 이후
R2와 독립적인 읽기 조사를 실제 GPT-6.1 Sol high 두 worker가 수행했다.
[플랫폼 조사](platform-research.md)는 후보 버전과 dependency resolution
증거를, [테스트 계약 제안](test-contract-proposal.md)은 공통 harness와
독립 관찰·RED/NOT_RUN·병렬 소유권의 제안을 남긴다. `434220b`,
`d6d60b3`, `d42f159`에서 통합했다. 제안 계약은 아직 동결하지 않았다.
이 조사에서 앱·test 코드와 DB를 만들거나 실행하지 않았다.

2026-10-07의 후속 GitHub 조회에서 fork의 `has_issues=false`를 확인했다.
이후 사용자가 로컬 추적을 선택해 R2 대기를 해소했다. 조사 문서의
`PENDING_R2`는 당시 snapshot이며 현재 상태는 이 기록을 따른다.


## 현재 gate: Step2 완료, Step3/S0 시작

Step2 최종 코드 baseline은 `d7ef715`다. 전체 harness407 PASS와
41 case·789 subcase·20461 assertion PREPARED, 실제 Gherkin789 RED를
확인했다. 마지막 enum 연결 변경은 focused16 PASS와 실제 생성→집계,
모델 SIT/UAT 각60 RED로 검사했다. 전체 준비는 PREPARED이고 coverage
문제0·VALID다. [최종 검토 기록](step-2-review.md)과 그 evidence를 따른다.
실제 Sol xhigh·Astra low의 최종 판정은 모두 PASS다.

Step3의 같은 시작 baseline은 로컬 tag `step2-complete`로 고정한다.
실제 GPT-6.1 Sol medium으로 아래 S0 Subtask를 병렬 수행한다. 모든
산출물 통합·실제 checks 통과와 platform decision 전에는 S1 도메인
구현으로 넘어가지 않는다. 사용자 Step3 전체의 필수 두 reviewer
검토는 구현 통합이 끝난 뒤 수행한다.

| Subtask | branch / 절대 worktree | 소유 범위 |
|---|---|---|
| CAP/DB 실행 기반 | `step3/s0-platform` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s0-platform` | backend·database·platform decision/versions/spike·local deploy |
| 인증 경계 | `step3/s0-security` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s0-security` | contracts/security·verification/platform/security |
| 최신 MCP wire | `step3/s0-protocol` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s0-protocol` | contracts/mcp·verification/platform/protocol |
| 보존·자료 inventory | `step3/s0-inventory` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s0-inventory` | docs/execution/s0-inventory·verification/platform/inventory |

platform worker가 S0 backend의 단일 writer다. security/protocol은
계약·독립 probe를 소유하고 platform과 공유한다. 공통 실행 기반을
먼저 통합한 뒤 필요한 backend 작업을 새 baseline에서 분할한다.
coordinator만 Task branch에 통합하며 긴 Maven/verify 실행은 동시2개로
제한한다. 사용자 원본 ERP checkout과 과거 소스는 재사용하지 않는다.

R2는 로컬 추적으로 확정됐다. R3/R5/R7/R8의 미정 운영 입력은 해당
scope를 활성화하지 않고 독립적인 구현을 계속한다. 실모델·유료 배포는
아직 수행하지 않는다. 전체 Task와 S0–S6 인수는 미완료다.
