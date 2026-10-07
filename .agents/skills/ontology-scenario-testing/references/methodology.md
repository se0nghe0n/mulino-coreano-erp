# #57 시나리오 방법론의 재사용 범위

원천은 [공식 이슈 #57](https://github.com/mulino-coreano/mulino-coreano-erp/issues/57)의
기존 설계 문서 `2026-09-25-scenario-tests-design.md` §4–8이다.
여기에는 새 온톨로지에서도 사용하는 방법론만 남긴다. 과거 애플리케이션,
제조 시나리오·전체 테스트 목록·CLI·버전·날짜·timeout은 요구로 옮기지 않는다.
새 업무 계약은 [온톨로지 구현 계획](../../../../docs/ontology-implementation-plan.md)
§3–13에서 읽는다.

SAP Activate의 테스트 계층과 업무 인수 관점을 적용한다. Cucumber/Gherkin은
현업이 읽는 시나리오를 실행하는 후보 도구이며 SAP 표준 도구를 사용한다고
주장하지 않는다. 실제 parser/test runner 선택과 호환 버전은 기술 manifest로
검증한다.

| 계층 | 판정할 대상 | 증거 |
|---|---|---|
| Unit | 타입·수량·목표/evaluator·시간 경계의 개별 규칙 | 독립 입력/기대값과 관찰 결과 |
| SIT | 실제 DB/API/MCP/runtime를 거친 업무 프로세스·인가·복구 | 응답과 영속 업무 상태·효과 대조 |
| UAT | 같은 시나리오를 실제 client/model로 수행한 업무 결과 | 모델/skill 실행·업무 결과·usage/비용 |
| Regression | 통합 뒤 기존 계약이 유지되는지 | 통합 commit의 영향 사례와 필수 SIT |

한국어 Gherkin의 파일은 업무 프로세스, 시나리오는 정상/예외 인수 사례다.
태그에 case/requirement와 실행 계층을 연결하고 단계는 역할과 업무
행동으로 쓴다. `그러면`은 업무 수량·상태·담당·책임을 단언한다.
메시지 문구나 JSON 필드 존재를 업무 결과 대신 검증하지 않는다.
숫자는 손계산으로 정하고 업무 시계·fixture를 공유한다.

SIT의 scripted agent와 UAT의 실제 model은 같은 fixture·시나리오·서버·
업무 oracle를 사용한다. 교체 지점은 Agent의 실행 방식이다. 실제
모델의 표현과 중간 경로는 달라질 수 있으므로 허용 효과와 최종 책임을
검증한다. 통합 경로를 mock으로 바꿔 통과시키지 않는다.

미구현 `@pending` 사례는 목록과 이유를 별도 출력하고 NOT_RUN으로
보고한다. 태그 삭제는 기능 구현과 실제 SIT 통과 증거가 있을 때 한다.
UAT 사전 조건이 없으면 skip 이유를 기록하고 PASS에서 제외한다.
실제 model 호출은 승인한 비용 범위에서 수행하며 runtime/model·실행별
결과·실패 원인·token·비용·최종 업무 상태를 기록한다.
