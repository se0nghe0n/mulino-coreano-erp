# 온톨로지 설계와 SAP 개발·BTP·Business AI의 접점

조사일: 2026-10-22. 저장소 대조 기준: `c10ed6f8`.

이 프로젝트의 중심은 **통사구조에서 착안한 업무 표현과 조합**이다.
사람과 Agent가 같은 대상·행동·조건으로 업무를 읽고 필요한 절차를
구성하도록 한다. SAP와의 접점은 업무 의미를 보존하는 모델링,
CAP 기반 구현, 자연어 요청을
통제 가능한 실행으로 연결하는 방법에 있다. 이 문서는 그 접점을
설명하며 SAP 제품 연동이나 프로젝트 전체 인수 완료를 주장하지 않는다.

설계 의도는 [설계 철학](ontology-design-philosophy.md)과
[설계자 인터뷰](interviews/2026-10-22-ontology-design.md), 제품별 버전·
확인 방식·한계는 [출처 기록](research/sap-relevance-sources.md)에 남긴다.
아래 SAP 자료는 모두 공급사 공식 자료이며 독립적인 효과 검증은 아니다.

후속 [SAP 사례 대조](ontology-sap-case-validation.md)는 Public Edition
2608의 출고 조회·POD·반품 검사 명세와 로컬 구현을 비교한다. 출고량,
고객 거부량, 목표 미기여량, 사후 반품량을 서로 치환할 수 없다는 점을
구체화했다. 사용자는 시험 tenant가 없으므로 문서·명세·로컬 사례부터
대조하도록 했으며, 실제 SAP API 호출은 수행하지 않았다.

## 1. 출발점: 업무 의도에서 필요한 정보를 찾는다

교육 시장용 ERP 개발자인 설계자는 GUI의 학습 부담과 여러 단계의
정보 조회·재정리 비용을 문제로 봤다. 통사구조를 택한 직접적인
이유는 새로운 업무를 표현하는 유연성을 규칙화하기 위해서다.
Agent가 절차를 고안해 사람에게 선제적으로 제안하는 경험을 지향한다.

이를 SAP가 해결하지 않은 문제라고 소개하면 부정확하다. Fiori는
역할에 맞는 단순하고 일관된 경험을 지향한다. Joule은 자연어 기반의
조회·거래·탐색·분석 기능을 구분하고, 일부 요청은 관련 앱으로 연결한다.
따라서 비교할 질문은 어떤 조합의 대화·화면·시각화가 사용자의 정보
탐색과 판단을 돕는가이다.
[Fiori 원칙](https://www.sap.com/design-system/fiori-design-web/v1-108/discover/sap-design-system/vision-and-mission/design-principles),
[Joule 기능 구분](https://help.sap.com/docs/joule/capabilities-guide/about-this-document),
[거래 기능](https://help.sap.com/docs/joule/capabilities-guide/transactional-capabilities).

이 프로젝트의 구체적인 탐구 대상은 자유로운 표현을 받으면서도
그 요청이 가리키는 물량·시점·증거·판정·책임이 흐려지지 않게 하는
방법이다. 학습 시간 단축이나 Agent 정확도 향상은 아직 실측하지 않았다.

## 2. 설계 결정과 SAP 개념을 대조한다

| 프로젝트의 결정과 이유 | SAP에서 확인한 접점 | 같다고 볼 수 없는 부분 |
|---|---|---|
| 대상과 업무를 동등한 진입점으로 두고 같은 ID·사실을 읽는다. 표현의 유연성과 일관된 해석을 지향한다. | [RAP의 business object](https://learning.sap.com/courses/building-transactional-apps-with-the-abap-restful-application-programming-model/defining-business-objects-and-their-behavior)는 data와 behavior를 함께 모델링한다. | RAP은 ABAP 개발 모델이다. 자체 명사·동사 모델과 일대일로 대응하거나 이 저장소에서 RAP을 썼다는 뜻은 아니다. |
| 사실·목표·증거를 분리하고 Agent가 업무 문맥을 읽게 한다. | [SAP AI-native foundation](https://architecture.learning.sap.com/docs/ai-native-north-star-architecture/foundation-layer)은 business semantics와 API metadata를 통한 grounding을 설명한다. | shared enterprise ontology는 문서의 장기 비전이다. 자체 모델의 출처나 현재 SAP 기능과의 동일성 근거가 아니다. |
| 입고 사실과 판매 가능한 조건의 충족을 구별한다. 도착 뒤 검사·반품이 필요할 수 있다. | [입고 품질 검사](https://help.sap.com/docs/SAP_S4HANA_CLOUD/87f9b54f9c4f4e75aff0061860a6589a/4e68ee6fa3174cce917154141ef9729a.html)는 inspection stock·lot·usage decision을 구별한다. | Public Edition 2608 Warehouse Management의 설명이다. unrestricted stock가 자체 판매 조건 전체의 충족을 뜻하지는 않는다. |
| 업무 종료와 목표 수량 충족을 독립적으로 판정한다. | [Delivery Completed 교육](https://learning.sap.com/courses/inventory-management-in-sap-cloud-erp/applying-tolerances-and-the-delivery-complete-indicator-1)에서는 전량 수령 전에도 open PO quantity가 0이 될 수 있다. | SAP의 indicator를 자체 Goal 판정으로 치환하지 않는다. 후속 GR의 실제 동작은 대상 제품·설정에서 확인해야 한다. |
| 종료 이후 후속 업무와 책임을 추적한다. | [고객 반품 처리](https://help.sap.com/docs/SAP_S4HANA_ON-PREMISE/7b24a64d9d0941bda1afa753263d9e39/ef17554b70b946e588cf4fb378fa4622.html)는 기존 문서 참조부터 검사·후속 물류·환급을 연결한다. | SAP에도 후속 처리와 document flow가 있다. 자체 Obligation의 수락·이전 규칙이 SAP 표준과 같다는 근거는 아니다. |
| Agent의 제안과 서버의 실행 허용을 나눈다. | [Joule governance 설명](https://www.sap.com/docs/download/2026/06/526001c1-567f-0010-bca6-c68f7e60039b.pdf)은 위임된 권한과 시스템 수준의 통제를 설명한다. | 설계 방향의 접점이다. 이 저장소의 권한 구현이나 모든 연결의 identity 전달을 SAP가 인증한 것은 아니다. |

이 대조에서 드러나는 강점은 새로운 이름의 수보다 구분의 이유다.
예를 들어 도착 수량, 검사 결과, 처분 제한을 한 상태로 합치면
“도착했으니 판매해도 된다”는 잘못된 해석을 만들 수 있다. 어떤 사실이
무엇을 증명하며 무엇까지는 증명하지 않는지 설명할 수 있어야 한다.

## 3. 대표 요청으로 보는 의미 해석과 검증

> 이번주 납품 완료/거부 비율과 거부 수량의 단가 및 총손실, 완료/거부
> 비율 대비 단가 비율을 보여줘

설계자가 확정한 것은 비율이 **수량 기준**이라는 점이다. 나머지를
임의로 채운 계산 결과는 정확한 응답으로 취급할 수 없다.

| 의미 항목 | 확인된 내용 또는 계산 전에 필요한 정의 |
|---|---|
| 비율 단위 | 건수가 아닌 수량이다. 서로 다른 품목의 단위를 합칠 수 있는지와 환산 기준은 별도 확인이 필요하다. |
| 기간·모집단 | “이번주”의 시간대·주 경계, 계획 납기와 실제 처리일 중 기준, 보류·부분 완료의 포함 여부를 정해야 한다. |
| 완료·거부 분류 | 동일 물량의 중복 계상과 부분 거부를 처리하고, 이후 정정·반품을 원래 판정과 구별해야 한다. |
| 단가 | 매입원가·판매가 등 어떤 가격인지, 통화·단위·가격 시점이 무엇인지 미정이다. |
| 총손실 | 산식이 미정이다. 거부된 물량의 금액이 곧 확정 손실은 아니다. 회수·재판매·환급 등을 반영할지는 업무 정의에 달렸다. |
| 비율 비교 | 수량 구성비와 금액 구성비의 비교인지, 완료군과 거부군의 평균 단가 비교인지 등 의미가 미정이다. |

다음은 이 요청을 검증하기 위한 분석 관점이며 추가 승인된 제품
요구사항이나 이미 구현된 파이프라인은 아니다. 해석한 기간·모집단·
단위·가격 기준을 명시하고, 해당 사실과 증거를 조회해 집계한 뒤,
표나 그래프를 원천 물량으로 추적할 수 있게 하면 응답을 검토할 수
있다. 정의가 없는 지표는 미정으로 남기고, 정의가 있는 지표와
구별한다. 실제 사용자 질문 방식과 출력 화면은 아직 정하지 않았다.

SAP Analytics Cloud의 Joule도 자연어 질문에 대한 차트 예시와
기간·집계·ranking을 제공한다. 접점은 자연어 분석 경험에 있다.
그 기능이 자체 거부·손실 지표를 자동으로 정의하거나 위 복합 요청을
그대로 처리한다는 근거는 없다.
[Analytical Insights 질문 예시](https://help.sap.com/docs/PRODUCT_ID/18850a0e13944f53aa8a8b7c094ea29e/726ea0677c034dfebadbf9587213ea59.html).

## 4. CAP 구현에서 BTP 확장으로 이어지는 논점

이 저장소의 CAP는 현재 단순한 후보가 아니다.
[S0 판정](../verification/platform/decision.md)은 로컬 CAP 경로를
채택하며, [dependency](../backend/pom.xml)는 CAP Java 5.1.1,
Java 21, Spring Boot 4.1.1을 지정한다.
[S0 manifest](execution/evidence/step3-s0/72b0a72/summary.json)는
`72b0a72`의 기술 fixture PASS를 기록한다. 이는 과거 해당 기준선의
증거이며 이번 문서 작업에서 재실행한 결과가 아니다.

CAP의 Application·Persistence·Remote Service 구분은 자체 업무
서비스와 외부 SAP 호출의 경계를 논의할 출발점이다. Remote Service는
별도의 모델·protocol·destination 또는 binding과 호출이 필요하다.
로컬 CQN persistence 성공을 S/4HANA 연동이나 BTP 배포 성공으로
확장해 말할 수 없다.
[CQN Services](https://cap.cloud.sap/docs/java/cqn-services/index),
[Remote Services](https://cap.cloud.sap/docs/java/cqn-services/remote-services).

실제 SAP 확장을 검토한다면 먼저 대상 edition의 표준 프로세스와
요구를 대조해야 한다. SAP의 Fit-to-Standard는 표준을 확인하고
차이를 구체화하는 접근이다. Clean Core 설명은 released interface와
확장 경계를 강조한다. CAP를 사용했다는 사실만으로 Clean Core
적합성이나 확장의 필요성이 성립하지 않는다.
[Fit-to-Standard](https://learning.sap.com/courses/implementing-sap-s-4hana-cloud-private-edition/describing-fit-to-standard-analysis-workshops_c77f59be-0b11-45b3-a63d-524bc2a1731e),
[SAP 확장 원칙](https://news.sap.com/2025/08/extend-sap-s4hana-cloud-right-way-clean-clear/).

이를 바탕으로 한 **후속 설계 제안**은 다음과 같다. 아직 채택된 연동
요구나 구현 일정은 아니다.

- SAP 표준이 소유할 재고·문서·판정을 먼저 정하고, 자체 의미 모델이
  참조할 ID·시점·단위와 보완할 업무를 한정한다.
- 필요한 API가 공개됐는지, edition·권한·이벤트 지원이 무엇인지
  확인한 뒤 side-by-side 확장 여부를 결정한다.
- 외부 쓰기는 로컬 DB transaction과 같은 원자적 작업으로 가정하지
  않는다. 재시도·중복 효과·응답 유실·대사와 실패 후 책임을 검증한다.

## 5. Business AI와 연결할 때 제품 범위를 구별한다

업무 의미 모델, graph 저장소, 분석 데이터, Agent 도구는 서로 다른
역할이다. 용어가 비슷하다는 이유로 기술 경험을 서로 대신할 수 없다.

| SAP 자료 | 확인할 접점 | 주장할 수 없는 내용 |
|---|---|---|
| [AI-native North Star](https://architecture.learning.sap.com/docs/ai-native-north-star-architecture/foundation-layer) | 업무 의미와 API metadata를 통한 grounding | 장기 ontology 비전 전체가 현재 GA라는 주장 |
| [Business Data Cloud FAQ](https://www.sap.com/docs/download/2025/02/4208db2f-f47e-0010-bca6-c68f7e60039b.pdf), 2026-07-13 갱신 | metadata·access API를 갖춘 data product | 자체 Work·Goal·Obligation이 곧 BDC data product라는 주장 |
| [HANA Cloud 기능 범위](https://help.sap.com/doc/eef71122810d4aa18d8eb6c37031f98a/latest/en-US/Feature_Scope_Description_SAP_HANA_Cloud_en.pdf), 2026-09-21 | RDF·SPARQL Knowledge Graph engine | 관계형 의미 모델만으로 HANA·RDF/OWL 구현 경험을 주장하는 것 |
| [Joule Studio classic MCP](https://help.sap.com/docs/Joule_Studio/45f9d2b8914b4f0ba731570ff9a85313/3d9dfad0bc39468292d508f0808a12fe.html) | Agent 도구 연결과 인증 계약 | 자체 MCP가 바로 연결되거나 모든 principal propagation이 된다는 주장 |
| [Joule Studio Agents](https://help.sap.com/docs/joule-studio/joule-studio/agents?version=CLOUD) | Agent Gateway·MCP·authorization 설명 | 조사 당시 EAC 한정 기능을 일반 사용 가능하다고 설명하는 것 |

Agent가 파이프라인을 제안하더라도 실제 도구의 입력·출력과 업무
효과를 먼저 시험해야 한다. SAP의 classic 교육도 Agent에 노출하기
전에 underlying functionality를 독립적으로 검증하도록 안내한다.
도구가 정확히 실행되는지와 Agent가 사용자의 의도를 정확히 읽는지는
서로 다른 검증 질문이다.
[Joule 도구와 MCP 교육](https://learning.sap.com/courses/getting-started-with-joule-studio-classic-edition/leveraging-tools-and-model-context-protocol-mcp-for-custom-joule-agents_eb29ffe4-58db-451a-9e3c-91b4daa6ad47).

설계·개발의 AI 활용도 운영 Agent와 구별한다. EAC 범위의 Joule
Pro-Code 문서는 사람이 요구·명세를 검토하며 coding agent와 개발하는
흐름을 설명한다. 이 프로젝트에서는 설계자가 결정하고 AI가 명세를
구현한다. 협업 방식의 비교이며 Joule Studio를 사용했다는 뜻은 아니다.
[Pro-Code Workflow](https://help.sap.com/docs/joule-studio/joule-studio/pro-code-workflow?version=CLOUD).

## 6. 학생 지원자의 포트폴리오로 사용할 때

설계자와 지원자는 다른 사람이다. 설계자는 교육 ERP 개발자이며
설계 결정을 맡았다. 학생 지원자의 현재·향후 기여는 인터뷰 답변상
**미정**이다. 이 문서와 AI 구현물을 근거로 학생이 설계·구현·테스트를
직접 수행했다고 쓰지 않는다. 이 조사 역시 학생의 조사 실적으로
귀속하지 않는다.

기여가 확인된 뒤에는 실제 맡은 범위에 맞춰 다음 질문에 답하는
자료로 활용할 수 있다. 이는 배정된 작업 목록이 아니다.

- 개발: 업무 의미를 어떤 모델·서비스 계약으로 표현했고, 어떤
  반례를 직접 구현하거나 검증했는가?
- BTP: 로컬 CAP 경험과 실제 클라우드 배포·외부 SAP 연동 경험을
  구분하며, 어떤 확장 경계를 직접 판단했는가?
- Business AI: 의도 해석과 도구 실행을 어떻게 나눠 평가했고,
  근거가 부족하거나 권한이 없는 요청을 어떻게 다뤘는가?

관련성의 한 사례로 SAP의 Singapore 인턴 공고는 MCP, Agent
Evaluation, AI 개발 도구와 구현·시험·협업을 언급한다. 특정 해외
공고와의 접점이며 한국 인턴의 요건이나 학생의 자격 충족·합격
가능성을 판단하는 근거는 아니다.
[공고 458752, 2026-10-06 게시](https://careers.sap.com/job/Singapore-SAP-Intern-Full-Stack-Developer-SAP-Build-Mobile-and-Agentic-Qualities-117440/1434449133/).

## 7. 현재 증거의 한계

[실제 시나리오 기록](execution/s5e-adapter/run-d63302d7.json)의
`d63302d7` 실행은 부분 fixture PASS 109, FAIL 614,
NOT_IMPLEMENTED 79다. 이 역사적 기록을 전체 제품의 성공률이나
Agent 정확도 개선 수치로 해석하지 않는다. 이번 작업은 문서 조사이며
최신 코드의 시나리오를 재실행하지 않았다.

현재 주장할 수 있는 것은 설계 의도, 소스에 존재하는 구현, 각
기준선에 연결된 검증 기록이다. 실제 SAP tenant 연동, BTP 운영 인수,
사용자 학습·정보 정리 비용 감소, 위 대표 요청의 종단 실행은 이
문서에서 입증하지 않는다. 후속 검증에서는 비교할 사용자 과제와
정답 기준을 먼저 정하고 같은 조건의 결과를 남겨야 한다.
