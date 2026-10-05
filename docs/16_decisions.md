# 범위 결정 기록

## 2026-10-06: MONITOR를 대화 MCP로 일원화 (#36)

### 배경

원래 기획은 L3 전용 화면을 상태 관찰 표면으로 두었다. 현재 구현은
`mcp-server/src/tools.js`의 `monitor_status`와 `list_attention`으로
상태·주의 항목을 조회한다. 같은 REST 데이터를 위한 별도 화면은 로컬
포트폴리오 목적에서 추가 업무 결과를 제공하지 않는다.

### 대안과 근거

- 전용 화면 유지: 상시 표시가 가능하지만 같은 집계·주의 큐를 다시 구현한다.
  화면 자체만으로 능동 알림이나 다중 클라이언트 인증을 해결하지 못한다.
- 대화 MCP 일원화: 기존 조회 계약을 사용해 상태·주의 항목·Case를 설명한다.
  `GET /api/v1/monitor`는 실행 가능한 대기를 재판정하고 Case를 만들지 않는다.
- 능동 알림: 사용자 질의 전 통지는 별도 채널·운영 정책이 필요하다.
  대화 조회로 해결했다고 간주하지 않는다.

두 번째 대안을 선택한다. 전용 대시보드 구현과 OAuth는 이번 범위에서
제외한다. 능동 알림을 구현 완료로 바꾸거나 향후 OAuth 일정을 약속하지 않는다.

### 영향

AGENTS.md·README.md·기획안·타임라인에서 L3 계층과 전용 화면 기술 계획을
제거했다. `dashboard/` scaffold와 feature 템플릿 L3 체크박스를 제거했다.
라벨 문서의 활성 L3 분류를 제거했으며 GitHub 라벨과 이슈 이력은 보존했다.
Phase 8 번호는 기존 milestone 이력대로 유지한다.

이슈가 참조한 `docs/10_requirements.md`는 baseline에 존재하지 않는다.
FR-19의 상태 관찰과 채널 계약을 실제 SSOT인
[인터페이스 개요](08_interface_overview.md)의 현재 계약에 반영했다.
[Case 계약](10_case_table_contract.md)과 중복 파일을 만들지 않았다.
CLAUDE.md는 AGENTS.md symlink를 유지한다.

### 증거와 한계

source baseline은 `0132e0a62ad9f086e5c70c5c857d4546f5921ef9`다.
조회 도구의 존재·로컬 scripted SIT는 구현 근거다. #24/#25 현재 native UAT와
#35 실제 클라이언트 인수 전까지 대화 표면 선택은 설계 판단이다.
10월 3일 과거 모델 UAT를 현재 source의 인수로 재사용하지 않는다.

개인별 운영 IAM, 실제 ChatGPT workspace 연결, Slack·Email 어댑터,
능동 알림, 운영 배포를 확인하지 않았다. MFDS 실제 전송·법정 양식 검증도
수행하지 않았다. 인증서 필수 유형 전체 검사·자동 30일 알림은 이슈 등록을
기다리는 갭이므로 규제 Goal 6 전체 완료를 주장하지 않는다.

[발표 자료와 증거 계층](portfolio/README.md)에 source·검증·인수를 구분한다.
