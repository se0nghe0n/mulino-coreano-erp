# 전체 구현 Task의 실행 기록

기록일: 2026-10-07. 상태는 실행 증거로 갱신한다. 계획 문서 검토
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
| 2 | 전체 계획의 tests | GPT-6.1 Sol / high | ACTIVE |
| 3 | 새 시스템 구현 | GPT-6.1 Sol / medium | PENDING |
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

## 시스템 S0–S6의 별도 gate

| 시스템 Step | 인수 범위 | 상태 |
|---|---|---|
| S0 | 추적·stack spike·schema/auth/MCP 기준선 | NOT_RUN |
| S1 | core·정의·증거·신원·두 진입점 읽기 | NOT_RUN |
| S2 | 목표·책임·거래·승인·idem·감사·복구 | NOT_RUN |
| S3 | 구매·운송·수입·수령·QC | NOT_RUN |
| S4 | 판매·반품·회수·정산·E1 | NOT_RUN |
| S5 | MCP·skills/client/model·정의 전환 | NOT_RUN |
| S6 | 운영·BTP·upgrade·restore·cutover | NOT_RUN |

사용자 Step 1의 skills 파일은 S5 인수의 준비물이다. 문서와 manifest
존재는 서버 구현·client loading·실제 호출·운영 인수를 대신하지 않는다.
Java21/CAP/Maven/PostgreSQL은 후보이며 exact 조합은 S0에서 검증한다.
startup 명령과 `./verify` wrapper는 아직 실행 가능한 것으로 확인하지
않았다. 운영 매뉴얼은 사용자 Step 6의 검증 후 산출물이다.

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

다음은 공통 테스트 harness와 계약을 검증·통합하고 같은 기준선에서
사례별 테스트를 작성하는 일이다. startup·도메인·DB·MCP/client·모델·
BTP 인수는 NOT_RUN이다.

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
