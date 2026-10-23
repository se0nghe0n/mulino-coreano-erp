# 실행 결정 기록

## 2026-10-24 잔여 실패 해소의 실행 설정

사용자가 완료까지 계속 수정하도록 지시했다. 수정은 T3 provider
`codex_2`, model `gpt-6.1-sol`, `reasoningEffort=xhigh`,
`serviceTier=priority`로 실행한다. 현재 runtime catalog에서 해당
provider의 xhigh와 Fast(priority) 지원을 확인하고 세 writer의 실제
위임 인자에 적용했다. 이전 provider의 지원을 근거로 추정하지 않았다.

테스트 swarm은 `claude-haiku-5-5`의 native dynamic Workflow를
사용한다. 기존 구독 인증과 전체 oracle을 유지한다. 별도 API 과금,
BTP 운영 계정·배포 예산은 새로 승인된 것으로 해석하지 않는다.
실행 전후의 원문·모델·source·JAR·자원 정리를 각각 대조한다.

## 2026-10-22 재개: 실행 모델과 구독 인증

- 사용자는 전체 잔여 구현의 완료를 요청하고 구현 진행 모델을 모두
  GPT-6 Astra + Ultrafast로 지정했다. 과거 Opus 작업 3개는 변경 전
  중단했으며, 각각 같은 기준선에서 Astra 작업으로 다시 배정했다.
- R8 인증 경로는 Codex 또는 Claude의 기존 구독으로 정했다.
  현재 호스트에서 Codex의 ChatGPT 로그인과 Claude의 구독 로그인을
  확인했다. 로그인 상태 확인은 실제 모델 호출 성공이나 업무 인수가 아니다.
- 별도 API 과금은 허용하지 않으며, 구독 사용량·token 등 제공되지 않은
  측정값을 0으로 기록하지 않는다. 기술 평가 결과와 사업 SLA를 구분한다.
- R7의 BTP 계정·region·entitlement·배포 예산은 여전히 미확정이다.
  로컬 구현과 결정적 검증을 계속하며 BTP 인수를 완료로 표시하지 않는다.

이 기록은 모델 실행·배포 성공을 주장하지 않는다. 실제 결과는 해당
실행 artifact와 통합 기록으로 연결한다.

## 2026-10-23 Sol Fast service tier

사용자가 Sol을 쓰는 작업을 Fast service tier로 계속하도록 지정했다.
현재 T3 catalog에서 codex/gpt-6.1-sol의 Fast 값이 priority임을 확인했다.
현재 thread의 실제 선택은 Sol/xhigh/serviceTier=priority다. 이후 Sol
위임은 기존 선택한 effort를 유지하고 serviceTier=priority를 명시한다.
기존 Sol thread를 재개할 때도 먼저 실제 설정을 확인·적용한다.

이 선택은 Sol의 이전 default 지시보다 우선한다. 종료된 실행의 실제
tier 기록과 Haiku native Workflow 실행 방식은 바꾸지 않는다.

## R2: 로컬 추적 확정

| 항목 | 확정 값 |
|---|---|
| status | CONFIRMED |
| owner | 사용자·coordinator |
| inputRefs | 구현 계획 §11 R2, 2026-10-07 사용자 답변 `track locally.` |
| proposedValue | fork 이슈 등록 또는 로컬 추적 |
| confirmedValue | 이 Task는 로컬 Git과 `docs/execution/task-status.md`로 추적한다 |
| decidedAt | 2026-10-07, Asia/Seoul |
| blockedStep | 없음. 사용자 Step 2의 코드 작성에 착수한다 |
| evidencePath | 이 기록과 `task-status.md` |

사용자의 명시적 선택이 계획의 원격 issue 선행 조건을 이번 Task에
한해 대체한다. 구현 이슈 초안은 로컬 범위 기록으로 유지한다.
원본 #57은 테스트 방법론의 출처이며 새 구현의 추적 이슈가 아니다.
원격 repository 설정·이슈·board 상태 변경은 이 결정에 포함되지 않는다.

이 결정은 R8의 추가 모델 호출 비용이나 R7의 실제 배포 계정·비용을
확정하지 않는다. 해당 실행 전에는 각 결정의 입력을 별도로 확인한다.
개발·검토 subagent의 모델과 effort는 사용자가 이미 지정한 순서를 따른다.

## R3: 실제 운영 자료 없음, 새 DB로 시작

| 항목 | 확정 값 |
|---|---|
| status | CONFIRMED |
| owner | 사용자·coordinator |
| inputRefs | 구현 계획 §11 R3·§12, 2026-10-08 사용자 답변 |
| proposedValue | 실제 자료 여부 확인 후 새 DB 또는 보존·이관 경로 결정 |
| confirmedValue | 실제 운영 자료 없음. 새 격리 DB와 개발 fixture로 진행 |
| decidedAt | 2026-10-08, Asia/Seoul |
| blockedStep | 없음. 과거 실제 자료의 live migration은 비대상 |
| evidencePath | 이 기록, `s0-inventory`의 사용자 선언과 Git/resource metadata |

사용자는 “실제 운영 자료 없음 — 새 DB와 개발 fixture로 진행”을
선택했다. authoritative DB·원문·이미 발생한 외부 효과·미해결 업무의
운영 자료 부재는 이 사용자 선언에 근거한다. 빈 fork나 Docker metadata로
추론한 결과가 아니다. 새 온톨로지 schema v1→v2 upgrade와 isolated
backup/restore 인수는 계속 필수다.

확인된 기존 PostgreSQL volume은 그대로 보존한다. 이 결정은 기존
volume·DB·Git archive 삭제나 실제 운영 환경 활성화의 허가가 아니다.


## R1: CAP/CQN 로컬 기술 기준선 확정

| 항목 | 확정 값 |
|---|---|
| status | CONFIRMED_LOCAL |
| owner | platform·coordinator |
| inputRefs | 구현 계획 §2·S0, platform decision과 통합 실행 증거 |
| proposedValue | CAP 우선 검증, 강제된 거래/인가 분리 시 대안 검증 |
| confirmedValue | Java21·CAP5.1.1·Boot4.1.1·CQN·PG18.6·Flyway12.4.0, exact manifest 고정 |
| decidedAt | 2026-10-08, Asia/Seoul |
| blockedStep | 로컬 도메인 구현 차단 없음. BTP와 실제 IdP는 별도 미인수 |
| evidencePath | verification/platform/decision.md·versions.json, step3-s0/72b0a72/summary.json |

한 schema·transaction·인가 경로, 실제 rollback/lock·fresh/upgrade·
복원·인증 MCP를 검증했다. CAP 기각 조건은 발견하지 않았다. 운영
buildpack·identity·BTP 지원은 로컬 결과로 확정하지 않는다.

## T09: 공통 원천 snapshot 계약

| 항목 | 현재 값 |
|---|---|
| status | COORDINATOR_DECIDED, 구현·인수 OPEN |
| owner | coordinator |
| inputRefs | D09, T09 same-trajectory-three-goals, 구현 계획 §5.1·§13 |
| proposedValue | 공통 원천 캡처와 목표별 불변 평가를 분리해 보존한다 |
| confirmedValue | 공통 원천 캡처와 목표별 불변 평가 envelope를 분리한다 |
| decidedAt | 2026-10-23, Asia/Seoul |
| blockedStep | 사용자 답변 대기는 해소했다. 동일성2개와 원천 관계3개의 구현·결합 인수는 OPEN이다 |
| evidencePath | t09-immutable-source-contract.md, 별도 tree의 진단 원문 |

같은 원천을 읽은 세 평가에서 실제 snapshot ID와 조건 행 수가 다름을
확인한 뒤 의미를 질문했다. 이후 사용자가 더 묻지 말고 계속 진행하도록
지시해 coordinator가 기존 인수 기준을 유지하는 공통 원천 캡처를
선택했다. 사용자가 세부 저장 설계를 직접 선택한 것으로 기록하지 않는다.
실제 영속 identity와 불변 원문·범위·시점·인가의 binding을 구현하며
합성 공통 ID로 맞추지 않는다. 원래 assertion은 보존한다.
