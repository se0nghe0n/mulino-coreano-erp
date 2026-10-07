# 새 구현 스택 adversarial debate — GPT-6.1 Sol high

검토일: 2026-10-07

## 범위와 방법

사용자가 요청한 실제 `gpt-6.1-sol`, `reasoning_effort=high`의 독립 검토자 세 명이 별도 문맥에서 구현 계획을 읽었다. 의미·수량, 복구·MCP·Skills, CAP·PostgreSQL·BTP를 나눠 반례를 제시하고 서로의 주장에 교차 반박했다. 작성자가 결과를 통합했다. 문서 논리와 인수 조건 검토이며 새로운 애플리케이션 실행·DB 경합 실험·BTP 배포·실제 모델 인수 결과가 아니다.

검토 기준선은 Java21, CAP Java 우선 검증 후보, PostgreSQL, 최신 stateless MCP2026-07-28 및 Agent Skills 구축 요구다. [구현 계획](../ontology-implementation-plan.md), [설계 철학](../ontology-design-philosophy.md), [논의 기록](../ontology-discussion.md)의 관련 D 항목을 읽었다.

## 판정

기술 이름만으로 네 가지 인수 영역이 충족된다고 판단할 수 없다. 기존 작은 검증 목록을 유지하되 아래 실패 사례를 통과 조건에 포함한다. 스택의 최종 채택은 실행 증거 확보 뒤에 결정한다.

## 반례와 교차 반박의 정리

| 논점 | 공격한 실패 사례 | 교차 검토와 최종 처리 |
|---|---|---|
| 정의 버전 | 선택 속성 추가만 검증하면 최신 evaluator가 기존 목표의 뜻을 바꿔도 통과 | 구 binary 영구 보존까지 요구할 필요는 없다. 지원 정의–capability/evaluator 대응과 미지원 보류를 V1에 명시 |
| 수량 | 예약된 부모를 분할하면서 부모 예약과 자식 가용량을 중복 노출 | 예약 이력·미충족 의무는 적격량과 다르다. 실물 배분의 보존과 분할/정정 경합을 V2에 명시 |
| QC·출고 | 적격성 조회 뒤 보류 확정, 또는 새 보류 뒤 옛 해제 도착 | 직렬화만으로 사건의 인과 순서를 해결하지 못한다. TOCTOU와 stale release를 V3의 별도 oracle로 유지 |
| CRUD 우회 | 직접 PATCH는 막았으나 nested/batch/대체 projection이 핵심 효과 변경 | 실제 노출 경로 목록과 DB 효과0을 V4에 명시 |
| 지속 의무 | 큐 처리 성공 뒤 대기 의무가 영원히 깨어나지 않음 | outbox는 업무 scheduler가 아니다. 재발견·claim 회수·기한 escalation을 V5에 명시 |
| 재전송 | 같은 key의 다른 수량이나 다른 주체가 이전 결과 사용 | MRTR 상태 보호만으로 해결되지 않는다. 범위·내용 결합과 현재 조회 인가를 V6에 명시 |
| 비동기 권한 | enqueue 뒤 위임 철회, worker 기술 신원으로 새 효과 실행 | 확정 효과 replay와 미확정 후속 효과를 구별한다. 권한 철회와 commit 순서, 차단 뒤 책임을 V7에 명시 |
| 실제 플랫폼 | 문서 예제를 혼합하고 빈 DB 성공을 운영 upgrade 성공으로 간주 | Java21·buildpack·PG binding 실증, schema 소유권, 인증 fail-closed, 최신 MCP client 왕복을 V8 및 플랫폼 확인 사항에 명시 |

Skills hash는 감사 자료다. 서버 handler 의미나 client의 실제 로딩을 증명하지 않으므로 V1의 호환성 계약 및 별도 skill 발견·로딩 인수로 처리한다. MRTR의 무결성·주체/요청/TTL 결합과 일회성 승인 소비 문구는 검토 중 보완됐으며 해당 반례는 문서상 해소됐다. 실제 구현의 충족 여부는 미검증이다.

## 스택 판정과 남은 증거

공식 자료상 Java21을 사용한다는 이유만으로 CAP 후보를 배제할 근거는 찾지 못했다. PostgreSQL 문서의 테스트 기준 버전도 다른 버전이 반드시 실패한다는 뜻은 아니다. 반면 BTP에서 동작한다고 확정할 증거도 없다. CAP 우선 검증, 광범위한 우회가 필요할 때 Spring Boot+jOOQ 대안이라는 판단을 유지한다.

검토 결과는 [구현 계획의 V1–V8](../ontology-implementation-plan.md)에 반영했다. 실제 schema·API·SDK/클라이언트·배포 manifest를 고정한 실행 검증은 아직 수행하지 않았다. 이번 작업으로 코드·DB·BTP·원격 브랜치나 보드를 변경하지 않았다.

공식 근거: [CAP5 릴리스](https://cap.cloud.sap/docs/releases/2026/jun26), [BTP PostgreSQL](https://cap.cloud.sap/docs/guides/databases/postgres#cap-java-on-sap-btp), [schema evolution](https://cap.cloud.sap/docs/guides/databases/postgres#schema-evolution), [CAP Java Security](https://cap.cloud.sap/docs/java/security#auto-configuration), [Java Event Queues](https://cap.cloud.sap/docs/java/event-queues), [MCP2026-07-28](https://modelcontextprotocol.io/specification/2026-07-28), [Agent Skills](https://agentskills.io/specification).
