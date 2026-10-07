# S1 application·read integration 기록

사용자 Step3의 S1에서 두 진입점이 서로 다른 인가·물량·책임을 읽지
않도록 하나의 read application을 구현한다. 기준선은
`164414418b6c3ce2959979939388d59cf2068e2c`다.

## 구현 범위

- `ApplicationQueries`: 현재 신원·coarse grant를 검사하고 typed handler의
  결과별 인가를 거쳐 REPEATABLE_READ transaction으로 읽는다.
- `WorkReadHandler`: 수입한 immutable Work/Goal/Assessment/Obligation 참조,
  item·LOT·인간 owner·원문 document 연결을 읽는다. S2 lifecycle을
  수행하거나 fixture의 업무 상태를 실행 성공으로 만들지 않는다.
- `OntologyRest`, `OntologyCap`, `OntologyMcp`: 공통 parser와 application을
  사용하며 독자 인가·저장·평가 경로를 만들지 않는다. 제품 MCP는
  `/mcp/ontology`이고 S0의 `/mcp`는 분리한다.
- `V7__core_read_links.sql`: 같은 조직의 item/LOT/definition/human owner/
  document만 연결하고 목표-판정과 업무-품목 FK를 검사한다.
- `intent.schema.json`: QUERY/RECORD/COMMAND 구분, typed slot·provenance와
  UUID subject를 정의한다. S1에서 write dispatcher는 열지 않는다.

## 인수 경계

수입된 참조의 읽기 검증은 Work lifecycle·goal evaluator·의무 인계의
실행 인수가 아니다. inventory의 판매 적격량·예약·누적 수령량은
후속 단계 구현 전 `UNKNOWN`이다. 전체 T01의60/100 판정과 S2/S3
효과는 NOT_RUN으로 남긴다. 실제 PG·CQN·HTTP 및 compatibility 검증은
dependency 통합 후 기록한다. 아직 실행하지 않은 검사를 PASS로
표시하지 않는다.

## S2 인계 조건

S1의 imported read reference는 S2의 권위 있는 Work·Goal·Assessment·
Obligation 상태와 통합돼야 한다. 두 모델을 독립적으로 변경하는 shadow
world를 유지하지 않는다. lifecycle command와 read projection 갱신은
하나의 committed transaction에서 대조하고 같은 두 진입점 계약을
계속 검증한다. S1의 read-only fixture 상태를 lifecycle 성공 증거로
승격하지 않는다.

## Native 조회 검증

`ReadContractsTest,S1ReadIntegrationTest`의11 tests가 실제 PostgreSQL18.6과
CAP5.1.1 CQN 경로에서 PASS했다. HTTP tests는 임시 RSA key로 서명한
JWT를 사용하고 검증된 외부 신원과 서버의 조직/actor/current grant를
연결했다. 비밀과 token은 실행 증거에 저장하지 않는다.

- REST의 getObject/getWork와 OData query, 제품 MCP tools/call은 같은
  snapshot·물량100BOX·document ID·human owner·nextAction을 반환했다.
- 제품 MCP tools/list 발견과 tools/call의 구조화된 FORBIDDEN을 대조했다.
- getEvidence의 typed selector는 실제 document metadata를 읽고 타 조직과
  잘못된 selector를 거부했다. 문서 내용 가용성은 UNKNOWN으로 보존했다.
- 같은 조직의 FK와 HUMAN responsibility guard, 다른 조직의 WORK
  evidence reference 및 stale projection의 SNAPSHOT_CHANGED를 검증했다.
- 같은 transaction의 SET LOCAL TIME ZONE을 UTC·Asia/Seoul·NewYork로
  바꾸고 CQN의 Instant insert/read와 offset 입력을 대조했다. effective
  boundary 직전의 Work와 knownAt 직전의 document가 노출되지 않았다.
- 전체568 columns의 이름·type·width·decimal precision·PK를 대조했다.
  `schema-compatibility.json`의 명시110 Timestamp widening과259 mandatory
  NOT NULL strengthening 외의 차이는 없다. literal DDL identity를
  주장하지 않으며 nullability weakening은 허용하지 않는다.

초기 fixture seed 누락, 테스트 authentication constructor와 schema
allowlist 생성 오류는 final 실행 전에 수정했다. 재실행 command와
로그는 evidence에 연결한다. 전체 backend test와 root의 fresh deployment
재검증은 별도의 통합 gate다. S1 native 일부 PASS가 전체 T01/T20이나
S2–S6·운영 identity·BTP·실모델 gate PASS를 뜻하지 않는다.
