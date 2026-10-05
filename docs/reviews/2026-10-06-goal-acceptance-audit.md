# 2026-10-06 목표 인수 재감사

현재 fork source와 원 이슈를 대조한다. 코드·scripted SIT·실모델·실제
대화 클라이언트는 각각 증거가 필요하며 전체 완료를 가정하지 않는다.

## ASK와 저장된 계획의 직접 조회

현재 JAR와 실제 stdio MCP SDK의 ask_inventory가 AMR 물리 재고 40,
BSC 15를 반환했다. PostgreSQL과 일치했고 조회 전후 Case는 1개였다.
같은 경로로 기존 native P2P-001의 저장된 계획도 읽었다. 모델 호출은 0건이다.

2026-09-05 business clock의 계획은 AMR 적격 재고 30을 사용한다.
QUARANTINE과 사용일 전에 기한이 끝난 LOT은 제외한다. AMR·BSC의 최초
추가 생산 필요일은 2026-09-20이다. 이는 저장된 fixture 판단이며
10월 6일 신규 예측이나 실제 ChatGPT/Codex 대화 인수가 아니다.
[공개 관측값·원본 hash](2026-10-06-ask-proof.json)를 보관한다.

## 요구별 판단

| 요구 | 확인된 증거 | 미완료 또는 한계 |
|---|---|---|
| #24 runtime/model 설정·역할 전환 | 실제 Claude P2P-001의 5개 Run과 PENDING 전후, 양 executor 테스트 | Codex parity 실패는 별도 보존 |
| #25 ASK/ACT/DECIDE/APPLY/MONITOR·반려 | 위 stdio 조회, Claude P2P-001·002, 입고 대기와 반려 Case OPEN | 가격 변경 최종 조정 실패·전체 UAT 미완료 |
| #26 QC·#27 추적/ADMIN 리콜 | 각각 SIT 6건, JAR/CLI/stdio·불변 감사·schema parity | 실제 QM 실패·RC 불완전 |
| #35 양방향 실제 대화·개별 승인 UX | 실제 세션 증거 없음 | tunnel/key 입력 대기 |
| #41 Supplier | 7개 업무 테스트와 5개 REST/Swagger, 이력·schema proof | production 신원·배포 인수가 아님 |
| #44 Evidence/Claim | 원본·관계·판정·정정·supersession·새 Run context | VERIFIED는 인간 attestation |
| #36 범위·#67 발표 | 대화 MONITOR, scaffold 제거, 14페이지 편집 객체·렌더 | native 실패·unknown·범위 제한을 표시 |
| 필수 인증 전체·자동 30일 통지 | 현재 검사 시 HACCP 한정 | 새 board 이슈 등록 대기, 임의 구현 안 함 |
| 통합 fork | 551 executable/SIT 20, 7 PR head ancestry | 조직 main 실제 merge와 별개 |

## 남은 실제 gate

동일 runtime의 모델 없는 재검사도 account endpoint AUTHENTICATION이다.
cached login/model 목록은 실제 모델 접근 성공을 증명하지 않는다.
로그인 갱신 절차는 미실행이다. #35의 tunnel ID·안전한 key 참조와
workspace 연결, 인증서 갭의 board 등록 결정도 기다린다.

최초 모델 실패의 정확한 원인은 unknown이다. 발주가 존재한다는 이유로
최종 조정 실패를 통과시키지 않는다. 실제 신원 IAM·OAuth 구현,
MFDS 전송/공식 서식 검증과 운영 배포를 이번 성과로 주장하지 않는다.
실패 시도와 unknown 비용은 별도 bundle에 보존한다.
