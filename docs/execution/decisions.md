# 실행 결정 기록

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
