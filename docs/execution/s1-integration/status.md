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
