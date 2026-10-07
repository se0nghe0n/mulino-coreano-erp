# 단계와 착수 결정

## 사용자 단계와 S gate

현재 구현 Task의 사용자 단계는 아래 실행 순서다. 사용자가 바꾸면 최신
지시를 따른다. 사용자 단계는 작업 종류의 순서이고 S0–S6는 제품 기능의
통합 인수 관문이다. 예를 들어 사용자 단계1의 skill 작성은 S5 전체의
완료가 아니며, 단계2의 테스트 준비는 S0 기술 검증의 성공이 아니다.

| 사용자 단계 | 산출물과 지정 실행 모델/effort |
|---|---|
| 1 | 개발 skill 작성: GPT-6.1 Sol high |
| 2 | 테스트 구현: GPT-6.1 Sol high |
| 3 | 애플리케이션 구현: GPT-6.1 Sol medium |
| 4 | E2E 검증·교정: GPT-6.1 Sol low |
| 5 | 재사용 패턴/추상화 검토와 구현 refactoring: GPT-6 Astra high |
| 6 | 검증된 실제 동작으로 운영 매뉴얼 작성: GPT-6 Astra low |
| 7 | 지정 순서를 반복해 남은 실패/공백 해소 |

각 사용자 단계의 통합 산출물은 다음 단계 전에 실제 GPT-6.1 Sol xhigh와
GPT-6 Astra low의 review를 받는다. coordinator가 두 결과를 대조하고
조치할 지적을 통합·재검증해야 단계를 닫는다. prompt에 모델 이름을 적는
것은 runtime model/effort 선택이 아니다. 지원되지 않으면 실제 제한을
보고하며 성공한 review처럼 표시하지 않는다.

테스트를 먼저 만드는 사용자 순서를 S gate의 순환 의존으로 바꾸지 않는다.
테스트 단계에서는 계획의 공개 계약·fixture·oracle와 필요한 harness를
작성할 수 있다. 미구현 효과의 실패/NOT_RUN, harness 한계, S0가 아직
검증하지 않은 조합을 기록한다. 앱 구현 단계에서 S0 oracle를 실제 통과한
뒤 S1→S6의 선행 계약을 만족시키며 구현한다. 실모델·BTP·wire·복원 등
필수 인수가 미실행이면 사용자 단계의 산출물 범위와 제품 미완료를 함께
보고한다. 준비가 끝났다는 이유로 해당 S gate를 PASS로 올리지 않는다.

## S0의 CAP 우선 검증

Java21과 단일 PostgreSQL 거래 경계는 기준선이다. CAP Java5 /
Spring Boot4.1 / Maven / CDS→CQN은 우선 검증할 후보다. 정확한 patch,
driver, DB major, compiler, SDK, client, buildpack을 설치 manifest에
고정하기 전 지원을 추정하지 않는다. 공식 문서와 실제 실행 로그를 함께
`verification/platform/decision.md`에 남긴다.

작은 vertical slice에서 read/action, authenticated custom MCP endpoint,
rollback, V2/V3 scope lock, grant/policy fence, transactional outbox,
Flyway fresh install와 upgrade를 검증한다. slice의 부분 V 증거와 모든
도메인/채널이 존재한 뒤의 V 전체 증거는 따로 기록한다. V8의 BTP 부분은
account/entitlement/cost가 확인된 환경에서 인수하며 로컬 PASS로 대체하지
않는다.

CAP 채택 조건은 schema 소유권 하나, 업무 거래 하나, 모든 쓰기 진입점의
동일 인가, V1–V8을 만족시킬 경로다. CAP가 중복 persistence 모델·별도
권한 경로·비원자적 core 쓰기를 강제하면 기각 이유를 적고
Java21 Spring Boot+jOOQ/PostgreSQL 대안을 같은 oracle로 검증한다.
CAP 채택 시 CQN을 기본으로 쓰고 JPA/jOOQ의 이중 모델을 추가하지 않는다.
PG 전용 고정 SQL은 binding·integration test를 가진 repository에 한정한다.
Flyway만 DDL을 실행하며 CAP 자동 deployer와 경쟁시키지 않는다.

검증에 필요한 공식 자료는 그 시점의 내용을 확인한다.

- [CAP release](https://cap.cloud.sap/docs/releases/2026/jun26):
  지원 계열·권장 patch와 Java/Spring 호환성을 확인한다.
- [CAP Java persistence](https://cap.cloud.sap/docs/java/cqn-services/persistence-services#postgresql):
  CQN/PostgreSQL 경로와 제한을 확인한다.
- [PG schema evolution](https://cap.cloud.sap/docs/guides/databases/postgres#schema-evolution):
  고정 compiler의 기대 schema와 custom constraint 허용 차이를 검증한다.
- [CAP Security](https://cap.cloud.sap/docs/java/security#auto-configuration):
  선택한 identity dependency/binding과 custom endpoint 보호를 검증한다.
- [CAP Java queue](https://cap.cloud.sap/docs/java/event-queues):
  지원 Java 기능과 system user context를 확인한다. 원 grant를 대신하지 않는다.

## S0–S6 종료 증거의 위치

정확한 acceptance는 계획 §11·§13을 따른다. 이 표는 해당 작업을 찾는
index다. 앞선 seed/stub 통과는 뒤의 전체 command gate를 대신하지 않는다.

| Gate | 반드시 함께 통합하는 범위 |
|---|---|
| S0 | 추적 issue/Phase/fork, 보존 ref·자료 inventory, exact stack decision, schema 소유, auth/MCP 계약 |
| S1 | 정의/type/cardinality, item/LOT/segment/계보, 사건/문서, 조직/grant/read, 빈 DB·CQN·두 진입점 |
| S2 | GoalVersion/evaluator, 의무/인계, approval/idem/audit, outbox/claim/reconciler와 core C2/C3/C5·V1/V4–V7 |
| S3 | 구매100→실수령60+40, canonical 증거, 운송/기관/QC 독립 범위, 잔여 책임, 실제 수령 V6 |
| S4 | C1/C4, 판매/인도/반품/회수/정산, V2/V3, E1과 겹친 제한·회수 oracle |
| S5 | 전체 MCP/action 경로, 6 운영 skills, 실제 client 발견/로딩/호출, 정의 전환/MRTR, 전체 V1/V4–V7, 승인된 실모델 |
| S6 | V8 새 ontology v1→v2, 운영 identity/TLS/binding, retention·restore·cutover, 모든 D/C/V/E 실제 인수 |

필수 모델/BTP/fixture의 미실행은 NOT_RUN이다. 선택 UI, 구형 client,
존재하지 않는 과거 live 자료 경로만 근거를 적어 비대상 처리한다.
기존 schema 변환이 비대상이어도 새 ontology schema v1→v2는 필수다.

## 미결정 register를 닫는 방법

각 R에 `status, owner, inputRefs, proposedValue, confirmedValue, decidedAt,
blockedStep, evidencePath`를 기록한다. 미정 이유·안전한 상태·결정자·차단
단계를 갖추면 다른 독립 구현을 진행할 수 있다. 값 없는 운영 scope는
활성화하지 않는다.

| 결정 | 닫을 근거 / 닫히지 않은 범위의 처리 |
|---|---|
| R1 stack | S0 exact manifest+spike logs / CAP 확정 주장 금지 |
| R2 추적 | 실제 fork issue·Phase·board 경계 / 기존 board scope 자동 변경 금지 |
| R3 자료 | authoritative DB/blob/외부 효과/의무 존재 inventory / fresh fixture와 자료 cutover 분리 |
| R4 품목·규제 | 공식 source·관할·적용일·확인자 있는 policy / 법적 허용 미확인 |
| R5 신원·책임 | local fixture와 실제 identity/grant/승인자/intake owner/supervisor mapping / 해당 scope 활성화 금지 |
| R6 기간·오차·대체·보존 | source/effectiveDate/approval / 오차0·명시대체·삭제 비활성 |
| R7 BTP | region/entitlement/cost cap+배포/binding/TLS logs / 운영 배포 미인수 |
| R8 모델 | 사전 비용 승인+corpus/모델/client/prompt manifest+관찰 결과 / 미실행을 의미 정확성으로 주장 금지 |
| R9 보완안 | 사용자 확장 결정 / 포장·계약 엔진·관측 diff 상세안 전체 자동채택 금지 |

보존 inventory는 현재 hash·분류·replacementPath·archiveRef·owner·validation을
기록한다. 실제 자료가 없으면 새 독립 DB로 시작한다. 있으면 원본 snapshot,
격리, LOT/수량/증거/의무 대조를 갖춘 뒤 cutover 범위를 확인한다. 현재
사용자의 새 구현 지시를 legacy 코드 조사/복사의 근거로 확대하지 않는다.
