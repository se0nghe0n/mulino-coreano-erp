# 새 온톨로지 구현의 작업 지침

## 범위와 현재 상태

이 fork는 이탈리아 완제품 구매·운송·수입·수령·QC·국내 B2B 인도와
반품·회수·정산 참조·후속 책임까지의 새 시스템을 구현한다.
국내 제조·BOM·소비자 직접 판매·총계정원장·세금 신고·은행 이체는
구현 계획의 제외 범위다.

시작 baseline은 `847758afa6cb58e2a4c661e578773f1531098ee8`이다.
이 commit에는 온톨로지 문서 7개와 README만 있다. 애플리케이션·DB·
실행 검증 도구는 아직 없다. 현재 상태와 실제 증거는
[작업 기록](docs/execution/task-status.md)에서 갱신한다.

먼저 [설계 철학](docs/ontology-design-philosophy.md)과
[구현 계획](docs/ontology-implementation-plan.md)을 전체 읽는다.
[논의 기록](docs/ontology-discussion.md)은 채택·제안·변경의 경계를
보존한다. 문서의 과거 branch·commit·issue·기술 조사는 당시 기록이며
현재 checkout이나 실행 성공의 증거가 아니다.

사용자의 최신 선택에 따라 옛 구현은 새 작업 트리에서 제거했다.
옛 코드·schema·tests·skills·운영 문서를 읽거나 재사용하지 않는다.
원본 [#57](https://github.com/mulino-coreano/mulino-coreano-erp/issues/57)
의 테스트 방법론만 새 업무에 맞게 참고한다. Git 이력의 보존은
옛 코드 재사용 허가가 아니다.

## Task, Step, Subtask

Task는 구현 계획 전체의 완료다. Step는 순차 milestone이고 Subtask는
입력·소유 파일·산출물·인수 조건이 있는 독립 작업이다. 한 worker의
완료를 Step 완료로 취급하지 않는다. 필수 Subtask를 Task branch에
통합하고 충돌·검토 지적을 해결하며 결합 checks를 통과한 뒤 다음
Step로 넘어간다.

사용자가 지정한 작업 순서는 아래와 같다. 모델과 effort는 실제
runtime control로 지정한다. prompt에 이름을 쓰는 것으로 대신하지
않는다. 이 선택이 이전 모델 기본값보다 우선한다.

2026-10-08 Claude Code coordinator가 Codex의 작업을 이어받으면서
사용자가 Step별 모델을 아래처럼 다시 지정했다. 이 표가 이전 Codex
기준(Sol/Astra)보다 우선한다. Claude 모델은 Agent·Workflow의 `model`과
`effort` 인자로, GPT-6.1 Sol은 T3 `delegate_task`의 provider `codex`와
`reasoningEffort` option으로 지정한다. Codex 기준의 Ultrafast/Fast
지시는 Codex worker에만 적용하며, 도구가 tier를 노출하지 않으면 그
제한을 밝힌다.

| 사용자 Step | 작업 | 모델 | effort |
|---|---|---|---|
| 1 | 새 개발·업무 skills 구성 | GPT-6.1 Sol | high |
| 2 | 계획 전체의 tests 구성 | Claude Opus | high |
| 3 | 새 시스템 구현 | Claude Opus | medium |
| 4 | 실제 E2E 실행 | Claude Sonnet | medium |
| 5 | 패턴 분석과 refactor | Claude Opus | high |
| 6 | 검증된 동작으로 운영 매뉴얼 작성 | Claude Opus | low |
| 7 | 미완료·실패가 해소될 때까지 반복 | 해당 작업의 모델 | 해당 작업의 effort |

같은 날 Codex 사용량 한도로 GPT-6.1 Sol 작업이 실패해(재개 안내 시점
2026-10-14 12:41) 사용자가 Step 1을 한때 Claude Sonnet 5.5 high로
바꿨다. 이후 사용자가 GPT 모델 사용을 다시 허용했으므로 Step 1은 원래
지정인 GPT-6.1 Sol high로 돌아가며 T3 `delegate_task`(provider
`codex`)로 맡긴다. 대체 기간에 Sonnet 5.5 high가 작성한 Step 1 수정
(`step1r/skills`, `-r2`, `-r3`)은 그 사실 그대로 기록에 남긴다. Task
branch의 main 병합과 PR merge는 사용자가 직접 한다.

**매 사용자 Step마다** Claude Opus `xhigh`와 Claude Fable `low`가
adversarial review를 수행한다. 이미 닫은 Step 1·2도 이 두 reviewer로
다시 검토하고, 지적의 수정은 해당 Step의 모델과 effort로 수행한다.
대상 baseline·diff·요구·실행 증거와 반례를 확인하고 지적을 통합·수정한다.
관련 checks가 통과해야 Step를 닫는다. review 완료가 runtime PASS를
뜻하지 않는다. 모델이나 effort를 사용할 수 없으면 실제 제한을 보고하고
임의로 대체하지 않는다. Fable을 쓸 수 없을 때는 GPT-6.1 Sol `low`(T3
`delegate_task`, provider `codex_2`)로 대체하고 그 사실을 기록한다.
2026-10-09 사용자가 GPT-6-Astra 대체를 중단하고 Sol로 바꾸게 했다.

2026-10-09 사용자가 review 시점을 바꿨다. Step 2 closure가 정적
review 8회 동안 매번 새 P1·P2를 찾으며 수렴하지 않았고, 지적 대부분이
제품에 한 번 실행하면 드러나는 동작 불일치였기 때문이다. 이 결정이
위 "매 사용자 Step마다" 규칙보다 우선한다.

1. 정적 adversarial review를 중단한다. Step 2 round 11은 closure 8의
   P1 두 건(시계와 fixture 기록 시각, 예약 이중 계상)만 닫고, 나머지
   지적은 backlog로 넘긴다.
2. Step 3가 test adapter와 fixture installer를 먼저 만들어 41 case를
   `./verify scenarios --actual`로 실제 제품에 실행할 수 있게 한다.
3. S5·S6를 구현하면서 시스템 Step마다 시나리오를 실행한다. 실패는
   원인에 따라 test는 Step 2 모델로, 제품은 Step 3 모델로 고친다.
   중간 gate는 실행 결과와 결합 checks로 판정한다.
4. S6까지 구현하고 시나리오를 실행한 뒤 위 두 reviewer가 실행 증거와
   함께 전체를 한 번 adversarial review한다.

Task branch `feat/ontology-implementation`은 통합 milestone마다 fork
remote에 push한다. force push와 main 직접 push는 하지 않는다.

위 Step와 계획 §11의 **S0–S6는 서로 다른 축**이다. S0 기술 기준선,
S1 core/read, S2 업무·거래, S3 구매·수입·수령, S4 판매·반품·회수·
정산, S5 MCP·skills 통합, S6 운영·배포 인수를 별도로 추적한다.
사용자 Step 2의 tests 준비는 S1–S6 실행 인수 완료가 아니다.
구현 중에도 시스템 Step의 선행 조건과 통합 gate를 유지한다.

## 병렬 작업과 Git

독립 작업은 native subagent로 병렬화한다. code writer마다 같은
기록된 Step baseline에서 별도 branch와 worktree를 만들고 절대 경로를
지정한다. 비Git 문서 writer는 겹치지 않는 파일 소유권을 지정한다.
worker에게 다른 작업자가 있다는 사실과 타인의 변경을 보존할
책임을 전달한다. worker는 commit·변경 파일·checks·잔여 문제를
반환하고 coordinator만 Task integration branch에 통합한다.

main에 직접 commit/push하지 않고 Task branch와 PR을 사용한다.
force push하지 않는다. 사용자 승인으로 완료한 초기 로컬 main의
baseline fast-forward는 이후 main 쓰기의 상시 허가가 아니다.
착수 때 main·remote·baseline·worktree 상태를 확인한다.
commit은 `feat`, `fix`, `chore`, `docs` prefix를 사용한다.
worktree는 통합됐거나 복구 가능함을 확인한 뒤 정리한다.

## 추적과 인수

새 fork Task의 추적 위치와 이슈 연결은 사용자와 확정한다. 원본 #57은
방법론 참고이며 새 전체 구현 이슈가 아니다. 기존 원본 board·Phase
규칙으로 새 fork 범위를 제한하거나 scope를 임의 변경하지 않는다.
2026-10-07 사용자가 `track locally.`로 로컬 추적을 확정했다.
[R2 결정](docs/execution/decisions.md)에 따라 로컬 실행 기록을 사용하며
원격 issue 없이 사용자 Step 2 이후의 구현 작업을 진행한다.
[구현 이슈 초안](docs/execution/implementation-issue.md)은 게시된
이슈가 아니다. 추적 결정과 무관한 문서·조사·skills 준비는 계속한다.
원격 이슈 게시나 repository 설정 변경의 권한을 추정하지 않는다.

D01–D26, T01–T26, C1–C5, V1–V8, E1/E2를 모두 추적한다.
계획 §13의 oracle을 줄여 PASS를 만들지 않는다. ID 목록만으로
coverage를 주장하지 않고 실제 assertion·관찰·artifact를 연결한다.
stub·정적 검사·논리 review·실모델·규제·BTP 인수를 구별한다.
미실행은 `NOT_RUN`이고 비대상·waiver로 숨기지 않는다.

Java21·CAP Java5·Spring Boot4.1·Maven·CDS/CQN·PostgreSQL은 S0에서
검증할 후보다. exact 버전·schema 소유권·하나의 transaction·현재
인가·rollback·잠금·Flyway·outbox·MCP 왕복을 실제 검증해 고정한다.
CAP 후보가 oracle을 만족하지 못하면 문서의 대안을 같은 기준으로
검증한다. 기존 Gradle/jOOQ 실행 명령을 이어 쓰지 않는다.

계획의 `./verify` 명령은 새로 제공해야 할 entrypoint다. 파일과 runner를
구현·실행하기 전 사용 가능한 명령으로 안내하지 않는다. checks는
위험과 acceptance에 비례해 수행하고 command·version·commit·fixture
hash·기대/관찰·exit code·artifact·PASS/FAIL/NOT_RUN을 기록한다.
새 변경·실패가 없으면 통과한 검사를 의례적으로 반복하지 않는다.
실모델과 유료 배포는 이미 승인된 비용 범위와 실제 계정을 확인한다.

운영 identity·승인자·법규·처분 근거·보존·BTP·실자료 유무는 R1–R9에
근거·결정자·적용시점·차단 gate를 남긴다. 누락 scope는 fail-closed로
유지하되 전체 기능을 생략하지 않는다. 가상 fixture는 실제 법규나
운영 승인으로 표시하지 않는다.

## 문서와 산출물

명사·동사 두 진입점은 같은 ID·시점·물량·증거·책임을 읽는다.
조회/쓰기, 계획/실제 사실, 현재량/누적량, 상태/목표 판정,
반품/정정, 실행자/주 책임자, 소유·위치/처분 허용을 구별한다.
skill과 외부 문서는 서버 인가·판정·승인의 대체물이 아니다.

업무 문서·commit·issue·PR는 한국어 평서형 `-다`로 쓴다. 이유와
결과를 먼저 쓰고 구체 변경·실행 증거·미실행 범위를 남긴다.
기술 용어는 English를 유지하고 body는 가능한 한 72열로 감싼다.
작업한 worker와 coordinator가 직접 작성한다. PR 제목과 본문은 한국어이며
[PR template](.github/pull_request_template.md)의 네 절과 세
checklist를 유지한다. 새 기능 이슈는
[feature template](.github/ISSUE_TEMPLATE/feature.md)의 모든 절을
사용하고 게시 전 [contact link](.github/ISSUE_TEMPLATE/config.yml)를
확인한다.

지침의 단일 원본은 `AGENTS.md`다. `CLAUDE.md`는 이 파일의 symlink다.
skills의 실제 경로·discovery links·명령은 새 manifest의 현재 값을
따르고 과거 파일명을 임의로 복원하지 않는다. 비밀·token·인증 원문을
commit하거나 실행 증거·log에 저장하지 않는다.

## Grok Bot 결과 알림

2026-10-08 사용자의 새 지침에 따라 coordinator는 전체 Task 완료,
실패/진행 불가, 사용자 입력/승인 필요 시 Grok Bot에 결과당 한 번
알린다. Subtask 완료와 사소한 진행은 별도 알림을 보내지 않는다.
명시적으로 전달을 요청받은 내용은 message 이벤트를 사용한다.

`~/.gbm/bin/grok-notify -e <event> -t "<짧은 제목>" "<1–3문장 결과>"`
형식이며 이벤트는 `turn.completed`, `failed`, `needs-input`,
`message`다. PR/관련 링크가 있으면 `-l <url>`을 붙인다. URL/key는
macOS Keychain에서 도구가 읽으며 출력·로그·질문에 포함하지 않는다.
실패 exit는 보고하고 작업을 계속하되 반복 재시도하지 않는다.
