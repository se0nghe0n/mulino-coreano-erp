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
| 1 | 새 skills와 실행 지침 | Claude Sonnet 5.5 / high (최초 작성 GPT-6.1 Sol high) | COMPLETE (Claude 재검토 closure PASS) |
| 2 | 전체 계획의 tests | Claude Opus / high | COMPLETE (Claude 검토 재수행 중) |
| 3 | 새 시스템 구현 | Claude Opus / medium | ACTIVE |
| 4 | 실제 E2E | Claude Sonnet / medium | PENDING |
| 5 | 패턴 분석·refactor | Claude Opus / high | PENDING |
| 6 | 검증된 운영 매뉴얼 | Claude Opus / low | PENDING |
| 7 | 미완료·실패 해소 반복 | 해당 작업의 모델 / effort | PENDING |

2026-10-08 Claude Code 인계 뒤 모델 지정은 [AGENTS.md](../../AGENTS.md)의
표를 따른다. 매 Step의 필수 review는 Claude Opus `xhigh`와 Claude Fable
`low`다. 아래의 Sol/Astra 기록은 Codex 시기의 사실 기록으로 보존한다.
Codex 시기의 필수 review는 GPT-6.1 Sol `xhigh`와 GPT-6 Astra `low`였다.
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
| S0 | 추적·stack spike·schema/auth/MCP 기준선 | COMPLETE |
| S1 | core·정의·증거·신원·두 진입점 읽기 | COMPLETE |
| S2 | 목표·책임·거래·승인·idem·감사·복구 | COMPLETE (구현 core) |
| S3 | 구매·운송·수입·수령·QC | COMPLETE (개발 fixture) |
| S4 | 판매·반품·회수·정산·E1 | ACTIVE |
| S5 | MCP·skills/client/model·정의 전환 | NOT_RUN |
| S6 | 운영·BTP·upgrade·restore·cutover | NOT_RUN |

사용자 Step 1의 skills 파일은 S5 인수의 준비물이다. 문서와 manifest
존재는 서버 구현·client loading·실제 호출·운영 인수를 대신하지 않는다.
Java21/CAP/Maven/PostgreSQL의 로컬 조합과 startup은 S0에서 검증했다.
S1–S3 native adapter의 실제 서명 HTTP·DB 실행 증거를 각 기록에
연결했다. 전체 normative case와 운영 인수는 아직 완료하지 않았다.
운영 매뉴얼은 사용자 Step 6의 검증 후 산출물이다.

## 필수 인수와 미결정값

D01–D26, T01–T26, C1–C5, V1–V8, E1/E2의 전체 인수는 미완료다.
완료한 S0–S3의 한정 실행 근거를 전체 coverage로 확대하지 않는다.
세부 oracle과 해당 S gate는 계획 §13을 따른다. 실모델·규제·BTP
인수를 로컬 결정적 tests나 논리 review로 대신하지 않는다.

R2는 [결정 기록](decisions.md)에서 로컬 추적으로 확정했다. R1의 로컬
stack과 R3의 실자료 없음도 확정했다. R4/R6 운영 정책, R5 실제 신원,
R7 BTP, R8 모델 비용과 수용치는 각 gate의 증거가 필요하다. R8 질문은 아직
답변 대기다. 경과 시간을 동의로 취급하지 않는다. 이와 독립적인
구현과 결정적 검증은 계속한다. 알려지지 않은 정책을 임의 허용하지
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


## 현재 gate: S0 완료, S1 시작

[Step3 S0 기록](step-3-s0.md)의 모든 산출물을 통합했다. Task72b0a72
전체 실행에서 PostgreSQL11 tests, 보안23 requests, MCP118 assertions가
통과했고 실행 JAR/source hash·독립 DB 효과를 대조했다. R1은 로컬
CAP/CQN 기반으로, R3는 실제 자료 없이 새 DB로 시작하는 것으로
확정했다. S0는 COMPLETE다. S1 baseline은 로컬 tag `step3-s0-complete`로
고정하며 같은 기준선의 독립 worktree에서 실제 Sol6.1 medium으로
신원·정의·물량·증거·공통 읽기 및 실제 인수 adapter를 구현한다.
사용자 Step3 전체와 운영/model/BTP 인수는 여전히 미완료다.


## 현재 gate: S1 완료, S2 시작

[S1 통합 기록](step-3-s1.md)의 여섯 산출물을 통합했다. Task35c810e에서
전체 backend64·harness417 tests가 실패/skip 없이 통과했다. 준비 검사는
41 case·789 subcase·20473 assertion PREPARED다. 별도 native 실행의
필수 version fixture 누락을 수정한 Taska59ee02에서 새 PG·실제 서명
HTTP·독립 JDBC21 assertions가 통과했다. backend/DB 입력은64 PASS 때와
차이0이며 같은 JAR hash를 실행했다. 실패와 통과 증거를 모두 보존했다.

S1은 COMPLETE이며 다음 공통 baseline을 로컬 tag `step3-s1-complete`로
고정한다. S2에서 업무·목표·책임·현재 인가·승인·멱등성·감사·outbox·
복구를 구현한다. 기존 read reference는 같은 거래의 권위 상태와
결합하며 독립 shadow state로 유지하지 않는다. 사용자 Step3와
전체 T/C/V/E·운영/model/BTP 인수는 계속 미완료다.

## S2 core 완료

`91fce58`에서 실제 HTTP/JDBC48·최종 harness424가 통과했다.
backend는 `e249df7`의 235 PASS 이후 변경이 없고 최종 JAR hash가
일치한다. [S2 실행 기록](step-3-s2.md)과 원본 증거 manifest를 따른다.
전체 사용자 Step3는 ACTIVE이며 S3 구매·운송·수입·수령·QC를 시작한다.

S3의 [소유권·결합 기록](step-3-s3.md)을 기준으로 독립 Subtask를
병렬 실행한다. 개별 PASS는 S3 통합 완료가 아니다.

## S3 개발 fixture 인수 완료

[S3 통합 기록](step-3-s3.md)과 [결합 manifest](evidence/step3-s3/final/summary.json)를
기준으로 S3를 닫았다. Backend 371개는 전체 실행과 수정한 identity
suite의 결합 결과이며 failure/error/skip 0이다. 제품 source는 그대로다.
Harness 427개와 native HTTP/JDBC 594개 assertion이 통과했다. 새
패키지와 native 실행 JAR의 내부 파일 496개가 모두 같은 내용이다.

구매100→실수령60+40, 중복·초과 수령, 잔여 책임40→0, 기관허용30과
QC100, 독립 제한, 취소 책임, 실제 응답 유실 뒤 수령60의 단일 효과를
확인했다. 기존 TRANSIT60 사례는 이동만 검증했다. 세 좁은 review
지적을 해소했으며 사용자 Step3 전체 두 모델 review는 아직 남았다.
S4 판매·반품·회수·정산 구현으로 진행한다. 모델/BTP와 전체 normative
coverage를 이 완료 판정에 포함하지 않는다.

S4의 [소유권·필수 인수](step-3-s4.md)를 고정했다. 같은 기준선의
일곱 독립 worktree에서 공통 계약, 판매/인도, 재고 실행, 반품, 회수,
정산, 실제 종단 adapter를 병렬 구현한다. 공유 계약의 OWN commit을
먼저 연결하고 모든 결과는 Task branch의 결합 검사로 확인한다.

## Claude 인계와 S4 통합본 실제 기준선

2026-10-08 Codex가 S4 도중 멈춘 상태를 Claude Code coordinator가
이어받았다. T3로 가져온 Codex 대화는 S2 종료에서 끝나지만 실제 Git은
S4 cherry-pick 통합 중이었으므로 Git과 이 기록을 기준으로 판단한다.

- 통합본은 `014045bf`부터 `d0ae28ca`까지 23개 commit 동안 컴파일되지
  않았다. `ReturnCommands.java`의 `if(receive)` 닫는 괄호가 빠졌다.
  worker 수정 `fd1fdeb8`을 `db38413d`로 통합해 해소했다. 이 구간의 Task
  commit에 연결된 PASS 주장은 그 commit에서 빌드된 증거가 아니다.
- 통합되지 않았던 `414aea2f`(DISPATCH/CARGO_SCOPE 증거 subject),
  `ebec1696`(판매 revision의 asOf 투영), `1bba7be3`(회수 예외 책임 인계
  test)를 각각 `468d4417`, `18aff0d6`, `50646c99`로 통합했다.
- 커밋되지 않은 Codex worker WIP는 손실을 막기 위해 원 branch에 보존
  commit으로 남겼다. 정산 `25c5f0f3`, C4 입력 `74786df6`, E2 입력
  `b3db371d`이며 모두 미검증이다.
- `50646c99`에서 첫 전체 backend 실행(`./mvnw -B -ntp -f
  backend/pom.xml test`)은 420 tests 중 failure16·error44로 FAIL이다.
  60건 중 약42건은 S4 `b60a3982`가 추가한
  `AssessmentCorrectionImpact.evidenceLinked`의 canonical 조회 실패
  (`Unavailable scope`)이며 S3 수령·운송 gateway의 회귀를 포함한다.
  나머지는 반품 canonical insert7, 판매 Works update5, 재고3,
  정산1, 인도 정정 NPE1, schema parity 열 수1(1863→1864)이다.
- 이 기준선을 로컬 tag `step3-s4c-baseline`으로 고정하고 Claude Opus
  medium worker 여섯 명이 독립 worktree에서 수정한다.

| Subtask | branch / 절대 worktree | 소유 범위 |
|---|---|---|
| scope | `step3/s4c-scope` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s4c-scope` | evaluation·evidence core, S3 gateway·회수 tests |
| returns | `step3/s4c-returns` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s4c-returns` | 반품 package와 tests |
| sales | `step3/s4c-sales` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s4c-sales` | 판매 package와 tests |
| fulfillment | `step3/s4c-fulfillment` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s4c-fulfillment` | 재고·QC·출고, T17 보강, V2/V3 DB 대기 증명 |
| settlement | `step3/s4c-settlement` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s4c-settlement` | 정산, V29, schema parity |
| flows | `step3/s4c-flows` / `/Volumes/VideoStore/Developer/mulino-ontology-step3-s4c-flows` | `verification/actual/s4`, C4·E2·C1 종단 입력 |

긴 Maven 실행은 `/Volumes/VideoStore/Developer/.mulino-tools/with-slot.sh`가
기계 전체에서 동시2개로 제한한다. 공용 환경은 같은 디렉터리의
`env.sh`가 Java21·Node24 PATH·임시 JWT 공개키로 고정한다. 이미 닫은
사용자 Step 1·2는 Claude Opus xhigh와 Fable low로 다시 검토 중이다.
S4는 ACTIVE이며 S5·S6은 NOT_RUN이다.

## 2026-10-22 별도 문서 작업: 설계자 인터뷰와 SAP 연관성

사용자는 설계 철학을 인터뷰하고 SAP 개발·BTP·Business AI와의 관계를
조사해 저장소에 게시하도록 요청했다. 이 작업의 기준선은
`c10ed6f876b22f731d7a176a37c7ebdd6ef3e9b9`이며 위 구현 Task의 Step나
시스템 S0–S6 인수 상태를 변경하는 작업은 아니다.

- [인터뷰](../interviews/2026-10-22-ontology-design.md)에 설계자와
  별도 학생 지원자의 역할, 수량 기준의 대표 요청, 미정인 지표를 기록했다.
- [설계 철학](../ontology-design-philosophy.md)에 통사구조를 선택한
  이유와 Agent의 선제적 파이프라인 제안 의도를 반영했다.
- [SAP 연관성](../ontology-sap-relevance.md)과
  [공식 출처](../research/sap-relevance-sources.md)에 표준 업무와의
  비교, CAP 로컬 채택, 제품별 범위·버전·미검증 연동을 구분했다.
- README의 문서 진입점을 추가하고 초기 상태로 남아 있던 CAP 후보와
  추적 미정 문구를 기존 S0·R2 근거에 맞췄다.

변경 파일 6개를 검토했고, Python으로 상대 링크·anchor 71개와
인용한 로컬 manifest 값을 대조해 오류 0건을 확인했다.
`git diff --cached --check`도 exit 0이다. dependency의 버전과 기존
S0·R2 판정은 원문을 대조했다. 문서만 변경하므로 backend·실모델·
SAP tenant·BTP 실행은 재검증하지 않았다. 학생의 현재·향후 기여,
단가·손실 산식은 미정이다.

### 후속 정렬: 통사구조와 SAP 사례의 연결

같은 날 사용자가 SAP 사례 검증과 통사구조 중심의 목표 정렬을 요청했다.
시험 환경은 없으며 공식 문서·API 명세와 로컬 사례부터 대조하도록
확인했다. 구현 기준선은 계속 `c10ed6f8`이며 제품 코드는 변경하지 않았다.

- [사례 대조](../ontology-sap-case-validation.md)에 Public Edition 2608의
  출고 조회·POD 차이·반품 검사 경로와 가상 사례 A–H를 연결했다.
- `recordDelivery`의 물리량/목표 기여량, 반품 처리, 책임 인계 거절의
  소스를 읽어 고객 인수 거부와 혼동하면 안 되는 지점을 기록했다.
- README·설계 철학·SAP 연관성·구현 계획에 통사구조의 업무 표현과
  조합을 중심 목적으로 반영했다. 논의 기록 §23에 사용자 선택과
  기존 범위·미정 항목을 보존했다.
- 출처 기록 S22–S26에 확인한 API 문서와 조회/입력 명세의 차이를
  남겼다. 최초 조사와 합쳐 공식 출처 26개다.

문서 상대 링크·anchor·fence와 diff whitespace를 검사하고, 사례 A의
80/100·20/100 산술을 확인했다. 이는 가상 사례의 설명 검증이다.
SAP API·backend·실모델을 실행하지 않았고, 거부 원천과 POD 읽기 경로,
가격·손실 산식은 미해결로 남겼다. 전체 구현 Task의 gate는 변경하지 않았다.

## 2026-10-22 구현 재개: 확정 계약 통합

재개 기준선은 `79a0cb6e`이고 첫 통합 milestone은 `082c703e`다.
해당 commit을 `fork/feat/ontology-implementation`에 push했다.
모든 새 worker는 `codex/gpt-6-astra`와 명시적인
`serviceTier=ultrafast`로 실행했다. 실모델 평가는 기존 Codex 또는
Claude 구독 인증만 사용하며 별도 API 과금은 승인하지 않았다.

- 외부 worker의 인증된 claim 제출 endpoint와 실제 HTTP adapter를
  연결했다. 정상 claim/replay, 위조·만료·타 주체·철회·READ grant를
  별도 PostgreSQL 검사로 확인했다.
- process role의 첫 start는 application을 시작하고 후속 start는
  같은 PID를 확인한다. process/host 관찰 계약과 검사를 함께 수정했다.
- T01 제외 업무는 `HELD VERSION_UNSUPPORTED`, 판매 인도 끝점은
  `DELIVERED`, `qualityTerms`는 필수라는 기존 결정을 반영했다.
- goal 필드·기간·grant fixture를 정합화하고 문서 hash를 갱신했다.
  normative 관찰과 인수 oracle는 삭제하거나 완화하지 않았다.

| 실행 | 결과와 범위 |
|---|---|
| 기존 backend 전체 검사 | 556개 중 354개 통과, 202개는 테스트 공개키 누락으로 context 오류 |
| 공개키 지정 재검사 | Platform 11개와 나머지 191개 모두 통과; 기존 556개 검사 전체의 성공을 확인 |
| worker endpoint/runtime/Platform 결합 검사 | `6f88fd06`에서 24개 통과; 새 endpoint 7개 포함 |
| harness 전체 검사 | `6f88fd06`에서 컴파일한 535개 통과 |
| worker adapter/process/host 결합 검사 | `33c27a80`에서 22개 통과 |
| fixture 검사 | `082c703e`에서 10개 통과 |
| 최종 `./verify prepare` | `082c703e`, 41 case·802 subcase·24,034 assertion, 준비 오류0 |

이는 서로 다른 통합 시점의 검사 기록이며 최신 전체 runtime 인수를
뜻하지 않는다. 기본 전체 검사 로그는
`.local/resume-20261022/backend-test-baseline.log`, 재검사는
`backend-retry.log`, 결합 검사는 `adapter-integrated-test.log`, 준비
검사는 `prepare-final-integrated.log`에 보존했다. 임시 키는 commit하지
않았다. 실행용 별도 worktree에서는 과거 구현의 untracked 파일을
포함하지 않는다.

T01/C3/V4 414개 subcase를 `64d5ccbd`의 격리 PostgreSQL에서 실행했다.
4,688개 실제 동작을 수행했고, 83개 PASS·301개 FAIL·30개 NOT_RUN이다.
PASS는 모두 partial fixture 실행이므로 전체 인수로 계산하지 않는다.
inventory가 추가한 미선택 388개는 이번에 실행한 실패가 아니다.
원본 결과는 격리 worktree의
`verification/harness/target/evidence/actual-scenarios-run-01d3c3d1-d857-430d-b3c7-50a4cc7c955f/`
에 있고 원인 목록은 `.local/resume-20261022/actual-contracts-inventory.json`이다.
T01은 조회 권한·fixture 전제와 제외 업무의 requestedEffect 관찰이
부족했다. C3/V4는 명령별 입력·정의 전제·관찰 공백과 실제 client 부재가
남았다. 실패 분류기의 ADAPTER/TEST 표시는 확정 판정이 아니다.

다음 Step의 공통 baseline은 `082c703e`이며 복구 목표의 증거 검증,
실제 worker claim의 case 연결, 구독 client runner를 독립 worktree에서
구현한다. 정의 회귀 실행과 나머지 운영·관찰 공백도 남아 있다.
실모델·BTP·규제 인수와 전체 S4–S6 완료는 아직 주장하지 않는다.
BTP 계정·region·entitlement·배포 예산은 미확정이다.

## 2026-10-22 구독 client 통합과 후속 실행

`2c0e84b3`에서 실제 MCP 전송, native client service 등록, 독립 DB의
물리 행 capture와 final response 원문 보존을 통합했다.
`06a13663`에서는 C3 `waitWork`의 API·MCP·worker 입력을 맞췄다.
두 commit은 fork에 push했다. 전체 Step 2는 아직 열려 있다.

- native 계약 20개가 통과했다. 전체 harness는 564개 중 563개가
  통과했고, T26의 변경된 evaluator 이름을 반영하지 않은 selftest
  하나가 실패했다. 해당 literal을 `core-v1`로 고친 뒤
  `RuntimeAssertionTest` 20개가 모두 통과했다. 서로 다른 실행의
  결과이며 단일 실행 전체 통과로 기록하지 않는다.
- C3 대기 명령 세 경로는 실제 동작 44회와 assertion 92개를
  통과했다. 선택·partial fixture 실행이며 전체 profile 인수는 아니다.
- 깨끗한 별도 worktree의 `2c0e84b3`에서 802개 전체 scenario
  기준 실행을 시작했다. 실행 로그는
  `/tmp/ontology-actual-all-20261022.log`에 남긴다. 완료 전에는
  실행한 subcase 수나 PASS 수를 확정하지 않는다.
- T01 전체 흐름, 복구 runtime, 실모델 독립 관찰 연결은 별도
  `gpt-6.1-sol` / `serviceTier=ultrafast` worker가 진행한다.

실모델 호출은 여전히 0회다. native 연결의 누락된 fixture·서버 인증
capture·관찰 계약은 `resume-model-integration.md`에 기록했다.
또한 현재 model prerequisite가 C3의 model subcase를 포함한 전체
profile PASS를 요구하므로 순환 의존성을 해소해야 한다. 결정적
부분의 누락·실패를 감추지 않는 별도 gate가 필요하다. 정의 발행에서
regression `NOT_RUN`을 허용하는 기존 공백도 미해결이다.

## 2026-10-23 T01 통합과 병렬 실행 원행 대조

`1e6a6a52`에서 재개했다. T01 worker의 `f91536aa`부터 `cbe7bd94`까지
9개 commit을 검토하고 Task branch의 `e0eebfab`부터 `3eb56f2c`까지
통합했다. T01 원문·claim·canonical 연결과 LOT 범위, 공급 근거,
고객 후보 조회를 보완했다. 상세 증거는
[T01 실행 기록](resume-t01-complete.md)을 따른다.

- 통합 source의 전체 harness 586개가 한 실행에서 모두 통과했다.
  backend 조회·품질·목표 검사 36개도 통과했다.
- T01 Python 계약 2개와 generator 재현성 3개가 통과했다.
  새 sourceInstaller는 조직 범위의 fixture 전용 actor로 명시하고,
  업무 actor의 item 범위 제한은 유지했다.
- `./verify prepare`는 41 case·802 subcase·24,248 assertion을
  준비했고 exit0이다. 제품 runtime 인수 결과가 아니다.
- 기존 worker의 clean source `7754cd87` 실제 T01 결과는
  12개 subcase·125개 assertion PASS다. coordinator가 report의
  원천 연결 96개를 재검사했다. partialFixtures=true이므로 전체
  profile 인수로 승격하지 않는다. 통합 source의 실제 재실행은 별도다.
- 검토한 조회 provider 두 파일의 runtime package source hash를
  갱신했다. 정적 package6개·discovery link12개가 통과했다.

검사 로그는 `.local/resume-20261023/`의
`harness-t01-integrated.log`, `backend-t01-integrated.log`,
`prepare-t01-integrated.log`다. root의 보존된 untracked 파일 때문에
workingTreeDirty=true이며 clean source 실행으로 표시하지 않는다.

이전 Haiku 실행의 원본 10개 report를 계획과 독립 대조했다.
802개는 중복·누락 없이 일치했고 원행 상태는 PASS122·FAIL592·
NOT_RUN88이다. inventory/aggregate의 NOT_IMPLEMENTED88개는 이
NOT_RUN88개를 재분류한 표시다. 실행 실패 88개가 추가된 것이 아니다.
PASS122개는 모두 partial fixture다. workflow agent 종료 때 초기
백그라운드 실행이 함께 끝난 뒤 셸 재실행으로 얻은 결과이므로
정상 dynamic workflow 전체 실행의 증거로 표시하지 않는다.
원본은 `t3-b11bf8e4/.local/scenario-run-20261022/`에 보존했다.
독립 대조의 절대 경로·SHA256은
`.local/resume-20261023/raw-inventory-check.json`에 남겼다.

현재 Claude Haiku 5.5 native dynamic workflow가 이 680개 non-PASS의
원행을 병렬 분류한다. PRODUCT·TEST_INPUT·ADAPTER·ENV·EXTERNAL_GATE를
실제 action/assertion·artifact pointer에 연결한 뒤 수정 순서를 정한다.
전체 Task와 현재 통합 Step는 열려 있다. 실모델 호출과 BTP 배포는
아직 실행하지 않았다.

통합 source `9a2e3df7`의 T01 실제 재실행도 완료했다. clean 별도
worktree에서 12개 subcase·125개 assertion·원천 연결96개가 모두
통과했고 actual action60개를 수행했다. native Workflow
`wf_c50cb2c6-a3e`의 4개 agent가 Haiku 5.5임을 runtime metadata에서
확인했다. 모든 subcase의 missingAdapters=[]다. 명시적 T01 선택과
partial fixture의 한계는 유지하며 전체 profile gate는 닫지 않는다.
원본 SHA256·실행 jar·미설치 사실은
[T01 통합 재검증](resume-t01-complete.md#2026-10-23-통합-source의-실제-재검증)에
기록했다.

## 2026-10-23 누적 목표 setup 입력 보완

Sol Ultrafast worker의 `8452ea29`를 검토하고 `d3d4c2df`로 통합했다.
T09·T11·T12·T13의 실제 실패28개에 속한 setup 요청38개만 보완했다.
T13은 fixture 시작부터 원 구매 기한까지의 명시적 period와 수령 사건
종류·기여 범위를 추가했다. 다른 세 case는 기존 period를 보존하고
startInclusive=true를 추가했다. 기한/끝점 변경과 미지원 evaluator 등
후속 probe는 수정하지 않았다.

coordinator가 네 case 전체를 비교해 assertion392개와 선택 밖 action이
동일함을 확인했다. 입력 계약·authoring 재현성·Java 목표 매핑/검증의
Python 검사4개와 `./verify prepare`가 통과했다. 준비 수는 계속
41 case·802 subcase·24,248 assertion이다. 이는 실제 scenario PASS가
아니며 backend/harness 전체 검사를 불필요하게 반복하지 않았다.
대조 artifact는 `.local/resume-20261023/cumulative-coordinator-diff.json`,
검사 로그는 `cumulative-setup-contracts.log`와
`prepare-cumulative-integrated.log`다.

현재 Claude Haiku 5.5 native dynamic Workflow에 같은 통합 source의
T09 9개·T11 12개·T12 5개·T13 17개, 총43개 subcase의 병렬 실제
재검증을 맡겼다. case별 독립 실행 worktree 네 개에서 정상 source
빌드를 수행하고, 모든 실행 종료 뒤 원행·hash·누락·중복·후속 실패를
대조한다. 기존 assertion392개와 partial fixture 한계를 유지한다.
전체 profile gate와 현재 통합 Step는 열려 있다.

## 2026-10-23 원행 분류와 후속 구현

기준 실행의 non-PASS680개를 Haiku dynamic Workflow로 분류하고,
coordinator가 419개 source hash와 pointer1,976개를 대조했다. 최종
분류는 TEST_INPUT434·ADAPTER220·PRODUCT22·EXTERNAL_GATE4다.
이는 기준 source1e6a6a52의 첫 실패 진단이며 현재 source의 인수 결과가
아니다. [분류와 후속 실행](scenario-triage-20261023.md)에 원본 hash,
정당한 null action36개와 후속 부분 실행을 기록했다.

누적 목표 입력 통합 source d3d4c2df의 후속 실행은 3PASS·28FAIL·
12NOT_RUN이다. T11은 실행 agent가 완료하지 못해 별도로 재실행 중이다.
목표 활성화23개가 공통 정의 검증 실패를 드러내 후속 수정을 진행했다.

T17 예약7개에 실물 범위 [0,30)의 startQuantity와 provenance를
명시한 worker8d1bc8c6을 022c642a로 통합했다. assertion257개와
선택 밖 action147개를 보존했다. coordinator의 Python 검사3개와
T17 준비 검사가 통과했다. 실제 재실행 전에는 PASS로 집계하지 않는다.

기본 fixture 정의의 capability manifest 누락8개와 미구현 선언1개를
정합화한 workera0303696을 4cdda709로 통합했다. 실제 Spring 등록
handler/query를 사용하는 PostgreSQL 검사4개에서 기존 위반9개가
수정 후 VALID·문제0개로 바뀌었다. 생산 validator·evaluator의 거부
규칙은 유지한다. recovery fixture28개는 동일 metadata만 중복 제거하고
충돌을 계속 거부한다. getCommandResult 구현과 해당 case 인수는 여전히
미완료이며 기본 fixture에서 가용한 기능으로 선언하지 않는다.

모든 변경의 결합 검사와 Haiku 실제 재실행이 끝나기 전에는 현재
통합 Step와 전체 profile gate를 닫지 않는다.

T11 재실행도 source d3d4c2df에서 완료했다. 같은 source의 네 case43개는
최종6PASS·37FAIL이고 actual action178개를 실행했다. 앞선 부분 실행의
12NOT_RUN을 지우지 않고 새 report에 연결했다. 새 공통 정의 수정의
결합 검사는 전체 harness588개·실제 backend 등록 검사4개와 prepare가
모두 통과했다. 준비 수는41 case·802 subcase·24,248 assertion이다.
로그는 `.local/resume-20261023/`의
`harness-default-definition-integrated.log`,
`default-definition-backend-integrated.log`,
`prepare-default-definition-integrated.log`다.

## 2026-10-23 공통 정의 후속 실제 실행과 입력 참조 통합

source66e4d068의 Haiku native Workflow 여섯 병렬 실행을 독립 대조했다.
고유70개는20PASS·50FAIL이며 assertion774개 중209PASS·49FAIL·
516미관찰이다. T01은12개·125assertion·원천 연결96개가 다시 통과했다.
T12의 child NOT_RUN 집계는 원본 FAIL로 바로잡았고, T17의 잘못된 jar
경로 표기는 canonical 실행 script·hash·runtime 로그로 대조했다.
모든 gateComplete는 false다. [원본과 후속 수정](scenario-triage-20261023.md)에
한계와 artifact를 기록했다.

setup45개 중 목표35개는APPLIED,3개는 앞선 수령 실패로 미도달이다.
예약7개는 좌표 검사 다음 판매 적격성 판정에서 거부됐다. fixture의
누락된 적격성 원천 사실을 성공으로 가정하지 않는다.

T13 응답 참조 worker73a64ccd를7b617659, T09·T11·T12 원천 입력
worker d4884b90를d9faa36a로 통합했다. assertion392개와 부정 probe를
보존했다. coordinator의 집중 검사14개와 prepare가 통과했다.
양성 수령10개의 source graph와 상태/기간 definition은 여전히 미완료다.
정적 builder와 새 입력을 실제 scenario PASS로 집계하지 않는다.
현재 통합 Step와 전체 Task는 열려 있고 실모델·규제·BTP 인수는 미실행이다.

## 2026-10-23 실제 수령 graph 입력의 연결

worker9888f36a와 custody 후속eb503e55를17f9bf7d·fd6bfb4f로 통합했다.
수령9개의 원문·관측·임시 수령·대조·canonical·확정 명령을 실제 입력
참조로 연결했고 LOT 없는1개의 부정 probe는 유지했다. assertion269개,
기존 business grant와 실물 사실을 보존했다. 입력 전용 encoding은
assertion이나 관측 business 값에서 원문을 만들 수 없다.

coordinator의 전체 harness601개와 원천 mapping8개 검사가 통과했다.
prepare도41 case·802 subcase·24,248 assertion에서 문제0개로 통과했다.
로그는 `harness-source-graph-integrated.log`와
`source-graph-source-integrated.log`, `prepare-source-graph-integrated.log`다.
새로운 실제 source의 scenario
PASS는 아직 주장하지 않는다. 상태/기간 목표와 확인 수령 의미를 별도
통합한 뒤 결합 gate를 판단한다. 현재 Step는 계속 열려 있다.

## 2026-10-23 상태·기간 목표의 결합 검증

worker180918d4를31e9a4f3로 통합하고7d22ce26에서 T09 입력을 결합했다.
상태·기간의 typed template을 공개하고 요청 수량·관측 구간·실제 재고
원장을 판정에 연결했다. 확인 전 원천은 누적 수령으로 세지 않으며
PHYSICAL_RECEIPT 원천과 CONFIRMED_RECEIPT 목표 의미를 구별한다.
기존 유일 template의 문맥 선택 동작과 business oracle는 보존했다.

coordinator의 backend72개·전체 harness601개·입력9개·runtime package
6개/발견 link12개와 prepare가 통과했다. backend 첫 시도의20개
ApplicationContext 오류는 로컬 공개키 환경 누락이었고 보정 후 한 번의
전체72개 실행이 통과했다. 원래 오류 로그도 보존했다.

이전 sourcead80e1bc의 Haiku43개 결과는9PASS·34FAIL이다. 예상
assertion392개 중87PASS·41FAIL·264미관찰이고 모든 gate는 미완료다.
최신 구현의 인수 수치로 섞지 않는다. T13 첫 ENV 시도와 한 번의
정상 재실행을 보존하고 원본 jar까지 대조했다.

source7d22ce26의 수령·목표 결합38개를 Haiku native dynamic Workflow로
실행했다. 아래 후속 결과와 구분하며 전체 Task·현재 Step·실모델/규제/
BTP 인수는 열려 있다.


## 2026-10-23 관측 원천·승인 범위와 수령 revision의 통합

T09의 두 기간 관측을 원문→EVENT/claim→대조→canonical graph로 연결했다.
worker76fe11e2·61aa091b를 b2cf0466·409c9228로 통합했다. source 전용
ITEM=P actor를 더하고 기존 business grant를 보존했다. feature 역할
표기8줄과 보조 행동8개만 바꾸며269개 assertion 원문을 유지했다.
원문 보고 시각과 coverage 구간을 구별하고 PostgreSQL microsecond
격자의 닫힌 끝점을 저장한다. gap의 최종 point는 구간으로 늘리지 않는다.

worker65095c42를9ac49b39로 통합해 T13 양성 fixture15개의 manager에
품목P/P-alt의 approvePurchase 관리 권한을 추가했다. 기존 grant·업무
사실·case123개 assertion은 그대로다. 실제 compiled authorization
probe195개를 포함한 입력 검사5개가 통과했으며 HTTP 인수는 아니다.

04af8e3c의 coordinator backend 결합134개가 한 번의 실행에서 모두
통과했다. 앞선 실행은133PASS·1FAIL이며 설명 문장만 사용하던 새 기간
테스트가 원인이다. 같은 시간 경계·기대 결과를 구조화된 원문에 연결해
고쳤고 첫 실패 로그도 보존했다. 실제 시나리오 oracle는 줄이지 않았다.

source7d22ce26의 Haiku Workflow wf_9c782f6d-e35는38개 중20PASS·18FAIL이다.
예상394개 assertion은204PASS·56FAIL·134미관찰이다. coordinator가
고유 행·source/fixture·실행 jar와10개 Haiku metadata를 대조했다.
모든20PASS 행은 전체 assertion·runtimeComplete·adapter 조건을 만족한다.
명시 subset·partial fixture이므로 전체 gate는 false다.

이 실행은 수령 확정 입력의 revision1이 실제 임시 수령 revision0과
불일치함을 드러냈다. dbc75fc6에서 T09/T11/T12의10개 확정 요청을 실제
임시 수령 응답 revision 참조로 고쳤다. 입력 검사12개와 prepare가
통과했다. prepare는41 case·802 subcase·24,248 assertion·문제0개이며
실제 실행을 뜻하지 않는다.

새 source dbc75fc6의 T09/T11/T12/T13, 고유43개·예상392개 assertion을
Haiku native dynamic Workflow로 병렬 실행 중이다. T17의 실제 적격성
원천 fixture는 별도 Sol Ultrafast 작업으로 수정한다. 사용량 snapshot
도구가 없어 정확한 잔여 한도는 알 수 없으며 두 작업만 분리했다.
전체 Task와 현재 Step는 계속 열려 있다.


## 2026-10-23 불변 판정 조회와 typed fixture 원천의 통합

source dbc75fc6의 Haiku wf_a6ac9383-20e는43개 중9PASS·34FAIL이다.
예상392개 assertion은122PASS·92FAIL·178미관찰이다. coordinator가
원본 report·fixture·source·실행 jar와6개 실제 Haiku5.5 metadata를
대조했다. 보고서의 terminal 행은 완전하지만 모든 전체 gate는 false다.
미관찰 assertion을 실행한 NOT_RUN subcase로 세지 않는다.

이 실행에서 T09 receive60, T11 수령5건과 T12 지연 수령은 실제 APPLIED로
다음 단계까지 진행했다. T09 관측4건도 실제 canonical 연결을 통과했다.
T09/T11의 확인된 전체 leaf 예약3건에는 startQuantity=0을 명시했다.
남은 판정·발주·정책 입력 실패를 각 원인으로 나눠 수정했다.

worker b9f3239f·60903408을1c8519c2·81412545로 통합했다. getAssessment의
직접 조회는 실제 Work/Assessment ID와 고정 시각·현재 권한·GoalVersion·
pending을 읽는다. 조회는 판정·의무·command·outbox를 쓰지 않는다.
action을 가진 공급 평가는 기존 T01의 명사/업무 snapshot을 유지한다.
미지원 장소/고객 scope는 무시하지 않고 명시 거부한다.
26개 fixture에 원래 EVIDENCE 정책을 명시하고27개의 명시 평가와 현재
revision 조회를 연결했다. 원래269개 assertion의 업무 기대값은 그대로며
126개 구조 바인딩만 공개 ID/outcome/goalVersion 필드에 맞췄다.
명시 평가는02시, 기존 knowledge 조회는04시라는 순서를 지켰다.

T13 worker c5ffe116을636f294f로 통합했다. 외부 작업15개의 라벨을
고정 UUIDv5로 연결하고 현재 개정 품목의 subject를 바로잡았다.
공개 domain-vocabulary가 정한 승인 outcome10건은 HELD로 정렬하고
정확한 APPROVAL_* code guard10개를 더했다. TYPE_INVALID 등 다른
거부로 승인 보호가 통과한 것처럼 세지 않는다. 개정값은 실제 proposal
조회로 대조하고 단위/통화2개도 별도로 검사한다. 기존123개 업무 조건을
보존하며 총135개 assertion이다. 원래 raw JSON 불변 주장은 하지 않는다.

T17의 전용 원천 actor·typed fictional policy·검증 체인과 LOT 만료
fixture를 통합했다. 원문→event/claim→review→canonical→typed decision을
실제 명령으로 거치며 ALLOWED/VERIFIED row를 직접 seed하지 않는다.
원래 명시 기한이 달랐던 B/EARLY는 별도 LOT으로 연결했다. 누락된 정상
기한은 coordinator가 합성 fixture 작성값으로 given 유효창 끝을,
출고 후 만료 사례는09:05 경계를 새로 명시했다. 기존 입력에 이미
있었거나 사용자가 실제 운영 기한을 승인했다고 표시하지 않는다.
257개 assertion·feature 원문과 수량·시각·기존 업무 권한은 유지했다.
만료 누락 copy와 UNKNOWN/CONFLICT 원천은 계속 fail-closed다.

결합 backend156개가 한 번의 실행에서 통과했다. 전체 harness의4개
오류는 registry 변경 뒤 self-test allowlist hash가 낡은 것이었다.
고정 sample의 hash를 현재 registry와 맞춘 뒤 전체615개 중610PASS,
명시 PG opt-in5개 SKIP·실패0개다. 별도 실제 HTTP/Spring/PostgreSQL
검사5개는 모두 PASS다. T17 PG 검증은 현재 source를 먼저 package하고
실행 jar hash를 남긴다. 입력 검사45개와 runtime package 정적 검사도
통과했다. prepare는41 case·802 subcase·24,260 assertion·문제0개이며
artifactCoverageStatus는 NOT_RUN이다. 결합 증거는
`.local/resume-20261023/all-contracts-integration-proof.json`이다.

source35234fa6을 고정한 새 Haiku5.5 native dynamic Workflow는 T01 회귀와
T09/T11/T12/T13의 고유55개·예상529개 assertion을 병렬 실행 중이다.
T17은 남은 공개 slot12건과 ALT 품목의 LOT 참조를 별도 Sol Ultrafast
작업으로 수정한다. 이 실행 결과를 이전 source의 PASS 수와 합산하지 않는다.
실모델 client 인수·규제·BTP 및 전체 Task/현재 Step는 계속 열려 있다.


## 2026-10-23 T17 공개 입력과 실제 인도 원천의 통합

worker9ac38382를37e26e85로 통합했다. T17 공개 slot 위반12건 중9건을
교정하고 OTHERPACK의 ALT 품목은 별도 L_ALT에 연결했다. 기존 업무
actor·grant·수량·시각·조건을 보존한다. 정상 인도는 원문 접수·관측·대조·
canonical 확인을 거치며 미검증 claim은 canonical로 승격하지 않는다.
원래257개 업무 oracle은 역변환으로 대조했고 보조 원천 행동25개를 더했다.

baf6ea4d는 실제 응답 effects의 dispatchId·cargoScopeId·deliveryId 세
UUID 경로만 input encoding과 assertion identity에 연결했다. 원래13개와
추가2개를 합친15개 assertion의 구조 경로만 교정했다. 수량·상태·판정을
관찰값에서 기대값으로 복사하는 것은 계속 거부한다. 첫 결합 검사에서
낡은 self-test 응답4곳을 발견해 현재 effects 구조로 고쳤으며 부정
mutation 검사는 유지했다. 원래 실패 로그도 보존한다.

결합 harness620개 중612PASS·별도 PG opt-in8개 SKIP·실패0개다.
현재 source를 새로 package한 실제 HTTP/Spring/PostgreSQL8개는 모두
PASS이며 입력 검사17개도 통과했다. prepare는41 case·802 subcase·
24,260 assertion·문제0개다. 상세 증거는
`.local/resume-20261023/t17-public-integration-proof.json`이다.

526beabf에서 판정 head 충돌의 outcome을 공개 어휘의 HELD로 맞췄다.
복수 head의 임의 선택이나 이력 쓰기를 막는 실제 PG 회귀를 포함해
판정24개·명사 조회14개·어휘4개가 한 번의 backend 실행에서 통과했다.
앞선 backend156개 전체를 이 변경 후 다시 실행했다고 표시하지 않는다.

baf6ea4d 고정 T17의15개·예상257개 assertion을 Haiku5.5 native
dynamic Workflow로 실행했다. 기존 source35234fa6의55개 wave와
결과를 섞지 않는다. 이 wave의 source에는 공개되지 않은 slot3건이
아직 남아 있으며 후속 제품 변경의 실행 결과로 바꾸지 않는다.

후속 worker4aeefb08·37fbaa69는6a95d072·f85c7b73으로 통합했다.
FEFO는 현재 적격·미예약·권한 있는 창고 후보만 비교하고 명시 사유도
적격성·겹침·line 잔여를 우회하지 않는다. 대체 evidence는 실제 원문·
조직·대상·타입·현재 접근과 hash를 검증하며 원 배분 교체와 한 거래다.
해제 사유는 bounded 문자열로 받고 같은 command 효과·감사 연결에 남긴다.
20c26668에서 runtime skill의 같은 세 계약과 package hash를 동기화했다.
이 source의 harness612PASS·PG opt-in8SKIP, 입력17PASS와 prepare
41/802/24,260·문제0개를 확인했다. 이행68개·판정24개·명사 조회14개·
어휘4개를 한 번에 실행한 backend110개도 모두 PASS다.20c26668을 새로
package한 실제 HTTP/Spring/PG8개도 전부 통과했다. 결합 증거는
`.local/resume-20261023/fulfillment-business-integration-proof.json`이다.

별도 Haiku wf_1c670b00-ec4의 baf6ea4d 실행은 고유15개 모두 FAIL이다.
257개 assertion 중6PASS·18FAIL·233미관찰이며6PASS를 subcase 성공으로
세지 않는다. 실제3개 Haiku metadata·source·fixture·실행 jar와 원본을
대조했다. root 기준 jar와 전체 ZIP entry bytes는 같고 container metadata만
달라 hash가 달랐다. 출고7건은 SELL만 명시된 원천의 DISPATCH 허용이
없어 거부됐으며 code/message 불일치가 아니다. 보류 category·dueAt 입력,
conditions와 effectLedger 관찰 경로, claim pending outcome도 남았다.
증거는 `.local/resume-20261023/t17-public-wave-coordinator-proof.json`이다.

보류/출고 원천과 실제 입력의 후속 수정은 새 Sol Ultrafast worktree에서
진행한다. 필요한 DISPATCH 결정과 보류 기간은 새 합성 fixture 입력으로
명시하며 원래 given이나 사용자 운영 승인으로 소급 표시하지 않는다.
원래 수량·시각·업무 grant·257개 조건과 UNKNOWN/CONFLICT를 보존한다.
전체 Task/현재 Step와 실모델 client·규제·BTP 인수는 계속 열려 있다.


## 2026-10-23 불변 판정·승인 통합 source의 실제55개 검증

source35234fa6의 Haiku native Workflow wf_a269a513-c5c는 고유55개 중
23PASS·32FAIL이다. 예상529개 assertion은310PASS·58FAIL·161미관찰이다.
T01 회귀12개·125개 assertion은 모두 PASS이며 각 행의 runtimeComplete도
true다. reportComplete라는 top-level key는 없으며 이를 false로 단정하지
않는다. 명시 case 선택·partial fixture 실행의 전체 gate는 모두 false다.

coordinator가 실제7개 Haiku5.5 metadata와 case별 새 실행 tree의 source,
fixture·원본/고유 run report·실행 jar hash·기동·clean 상태를 대조했다.
모든 case는1회 실행했고 retry는 없었다. agent shell prelude와 실제
harness stdout을 구별해 원본과 hash가 같은지 확인했다. 독립 증거는
`.local/resume-20261023/assessment-purchase-wave-coordinator-proof.json`이다.

T13 개정7건은 실제 HELD/APPROVAL_NOT_CURRENT, 발주·outbox·segment 효과0과
감사 조건까지 통과했고 원행 consumptionPolicy 필드 누락만 남았다.
T09 기간/coverage, T11 waitReason, T12 불변 버전·입력 snapshot과 조건의
관찰 공백을 실제 업무 실패와 구별한다. 실제 INITIAL_BALANCE row를
숨기거나 미발행 condition/evaluator 이름을 기대값으로 채우지 않는다.
unsupported evaluator의 HELD는 공개 VERSION_UNSUPPORTED 계약과 같으며
기존 REJECTED oracle은 별도 protocol 교정 대상으로 남긴다.

자유 문자열 기여 범위, 미지원 resume/correction slot, 판매/출고 원천,
정책·공급자·부분 수령 입력과 claim pending 경계는 계속 열려 있다.
원행 observer의 한정 보완은 별도 Sol Ultrafast worktree에서 진행하며
T17 원천 writer와 파일 소유권을 분리했다. 새 source의 결합 검증과 Haiku
재실행 전에는 이55개 결과를 최신 제품 전체 인수로 승격하지 않는다.

## 2026-10-23 원천·관찰·구매 승인 소비 정책 통합

T17 source 입력, scoped DB projection과 그 빈 recovery source 회귀,
구매 승인 소비 정책 저장·집행을 통합했다. T17 후속 입력은 새로운
합성 선택임을 명시하고 원래 257개 업무 assertion을 보존했다.
기존 사업 grant 불변은 이전 layer의 사실이며, 후속 layer에는 기존
scope·기간을 유지한 최소 category capability 8건이 명시적으로 추가됐다.

제품 결합 47개와 최종 harness 629개(실제 observer PG13 포함)가 통과했다.
T17 opt-in22개도 별도 fresh HTTP/Spring/PostgreSQL 실행에서 모두 통과했다.
새 V38 열 때문에 실패한 관찰 테스트 1건의 첫 로그와 수정 후 결과를
모두 보존했다. [통합 기록](resume-source-observer-policy.md)을 따른다.
전체 Task와 현재 Step는 OPEN이며 다음 실제 실행은 Haiku 5.5의
native dynamic Workflow로 6개 case·70개 subcase를 병렬 실행한다.
