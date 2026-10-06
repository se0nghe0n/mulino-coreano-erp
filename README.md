# MULINO COREANO

> Mulino Bianco 한국 진출 가상 ERP + AI 에이전트 거버넌스 시스템

---

## 프로젝트 한 줄 소개

이탈리아 식품 브랜드 **Mulino Bianco**가 한국에 현지 제조 법인을 설립한다고 가정하고, EU 기준 ERP를 한국 식품 법규에 맞게 현지화(Localization)한 뒤, 그 위에 AI 에이전트를 얹어 구매·공급망·품질 업무를 자동화한 가상 ERP 시스템입니다.

---

## 아키텍처 (3개 레이어)

| 레이어 | 구성 | 역할 |
|---|---|---|
| L0 | PostgreSQL 18 + Spring Boot REST API | ERP와 Case 상태를 저장하고 CLI·MCP에 노출 |
| L1 | 백엔드 도메인 승인 게이트 | 구매 MANAGER·입고 QC·리콜 ADMIN 승인과 불변 감사 |
| L2 | Multi-Agent (Claude Code / Codex) | Orchestrator / Supply Chain / Procurement / QC |

---

## 구현과 인수 현황

2026-10-06 현재 accepted production source `4e6935d`는 로컬 PoC다.
Zig CLI와 native runner, 구매·입고 품질·리콜 승인, 증거 판단과 Supplier
CRUD를 구현했다. 역사적 Supplier baseline `0132e0a`의 549건 실행·
SIT 20건은 별도 checkpoint로 유지한다.

현재 보강 과정의 backend는 587건 발견·18건 제외·569건 실행과 bootJar가
통과했다. 이후 helper·prose CANCEL 교정 뒤 focused 22건·전체 SIT 20건·
bootJar·runner 84건이 통과했다. 최종 교정 뒤 full backend를 다시 실행하지
않았다. 현재 CLAUDE / `claude-sonnet-5` 실제 업무 UAT는 5 PASS,
21개 완전 native Run·receipt 일치·permission denial 0이다. QC·리콜은
인간 승인 대기까지이며 ERP 적용은 없다. 과거 실패와 unknown 비용을 보존한다.
Codex account 인증 gate·parity와 실제 대화 클라이언트 #35 인수는 남아 있다.
이 증거는 운영 배포나 규제 인수를 뜻하지 않는다.

MONITOR는 대화 MCP로 일원화했다. 전용 대시보드와 OAuth는 범위에서
제외했다. 인증서 필수 유형 전체 검사와 자동 30일 사전 알림은 이슈 등록을
기다리는 알려진 갭이다. 식약처 실제 전송·법정 양식 검증·개인별 운영 IAM을
수행하지 않았다. 아래 비교표는 설계 가정과 구현 방향이며 법적 적합성
인증이나 실제 SAP 연동을 주장하지 않는다.

[범위 결정](docs/16_decisions.md)과
[발표 자료·증거](docs/portfolio/README.md)에 검증 계층과 한계를 기록했다.

---

## SAP 모듈 매핑

| 설계 테이블 | 대응 SAP 모듈 | 역할 |
|---|---|---|
| `suppliers`, `supplier_certifications` | SAP MM | 공급업체 마스터 |
| `purchase_orders`, `purchase_order_items` | SAP MM | 구매오더 (`ME21N`) |
| `inbound`, `raw_material_lots` | SAP MM | 입고처리 (`MIGO`) |
| `warehouses`, `stock` | SAP EWM | 창고관리 |
| `production_records`, `production_lots` | SAP PP | 생산오더 |
| `products`, `raw_materials` | SAP MM | 자재/제품 마스터 |
| `orders`, `order_items`, `outbound` | SAP SD | 수주오더 (`VA01`) |
| `customers` | SAP SD | 거래처 마스터 |
| `recalls`, `alert_rules`, `alert_events` | SAP QM | 품질알림 및 검사/알람 관리 |
| `governance_*`, `regulatory_submissions` | SAP GRC | 거버넌스, 리스크, 컴플라이언스 |

---

## As-Is / To-Be (현지화)

| 항목 | As-Is (EU) | To-Be (한국) |
|---|---|---|
| 추적성 설계 기준 | EU 기반 추적성 가정 | 한국 이력 추적·제출 기록 모델 |
| 알레르겐 표시 | EU 14종 | 한국 22종 (19개 법정군 계층 관리) |
| 인증서 종류 | HACCP/BRC/IFS | HACCP/GMP/이력추적등록 |
| 리콜 보고 | EU 기반 보고 절차 가정 | 식약처 즉시 보고를 위한 OFFLINE/PENDING 초안 |
| 이력 보관 | EU 기반 보관 정책 가정 | 소비기한 + 2년 보관 뷰와 리콜 기록 보호 |
| 세금계산서 | 해당 없음 | 국세청 전자세금계산서 의무 관리 |

---

## 기술 스택

- **DB**: PostgreSQL 18 (현재 DDL 61개 테이블. 원래 ERP 30개와 인터페이스
  13개에 후속 테이블 18개를 추가했다)
- **Backend**: Spring Boot 4.1.x + Java 21 + Gradle
- **Tool 노출**: Single Zig CLI (`mulino`) + MCP Server
- **Agent**: Claude Code / Codex Subagent Architecture (Orchestrator / Supply Chain / Procurement / QC)
- **MONITOR**: 대화 MCP의 `monitor_status`·`list_attention`

---

## 핵심 설계 개념

- **3-Way Match**: 발주 → 입고(HOLD 기본) → 송장 검증 (SAP MM 핵심)
- **Batch Management**: LOT 기반 양방향 추적 (역추적/순추적) 및 FEFO 유통기한 관리
- **Governance Persistence**: 에이전트 액션을 가로채 DB 승인 큐(`governance_actions`) 및 불변 감사 로그(`governance_audit_logs`)로 통제
- **Extensible Architecture**: 다단계 BOM(반제품), 자재 유형(포장재/첨가물), IoT 시계열 파티셔닝(BRIN)

## 로컬 Human gateway 경계 (#33)

local 백엔드와 인간 stdio MCP는 host 전용 `MULINO_LOCAL_HUMAN_SECRET`을
공유한다. 미설정·잘못된 key는 사용자 조회 전에 401로 거부한다.
service secret과 다른 값을 사용하고 agent 환경·인자·stdin·context·로그인
volume에는 전달하지 않는다. key 생성과 보호되는 조회 경로는
[Human gateway 계약](docs/14_human_purchase_api.md#로컬-human-gateway-경계-33)을 따른다.
역할 헤더는 공유 로컬 신원이며 개인 인증이나 인간 동의의 증거가 아니다.

## Supplier 오류 코드

공급업체 master의 경로·신원·감사 계약은
[Supplier master API](docs/21_supplier_master_api.md)에 정의한다.

| 코드 | HTTP | 의미 |
|---|---|---|
| SUP001 | 404 | 공급업체가 없음 |
| SUP002 | 409 | 공급업체 version이 변경됨 |
