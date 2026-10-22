# SAP 연관성 조사 — 출처와 판단 범위

조사일: 2026-10-22. 저장소 대조 기준: `c10ed6f8`.

SAP의 공식 제품 도움말, 개발 문서, 교육 자료, 아키텍처 문서와
채용 안내를 사용한다. 여러 URL이 있어도 모두 SAP가 발행한 자료이므로
독립적인 사용자 연구나 효과 검증으로 취급하지 않는다. 제품이 밝힌
기능·설계 방향을 확인하는 근거다. 검색 수집일은 발행일이 아니다.

`본문`은 웹 도구로 본문을 확인한 자료, `검색 본문`은 직접 열기로
본문을 확보하지 못해 공식 URL의 검색 본문으로 확인한 자료,
`PDF`는 공식 배포 파일을 내려받아 본문을 확인한 자료를 뜻한다.
실제 SAP tenant를 실행하거나 entitlement를 확인하지 않았다.
rolling 문서와 `latest` PDF는 이후 내용이 바뀔 수 있다.

## 사용자 경험과 자연어 업무

### S01 — SAP Fiori Design Principles

- [공식 디자인 문서](https://www.sap.com/design-system/fiori-design-web/v1-108/discover/sap-design-system/vision-and-mission/design-principles)
- 범위: URL에 고정된 v1-108 디자인 지침. 검색 본문 확인.
- 근거: 역할, 일관성, 단순성, 적응성 등을 UX 원칙으로 설명한다.
- 한계: 특정 설치 환경의 사용성·학습 시간을 측정한 결과가 아니다.
  SAP가 GUI 복잡성을 줄이려는 접근을 하지 않았다는 주장을 반박한다.

### S02 — Joule Capabilities

- [공식 기능 안내](https://help.sap.com/docs/joule/capabilities-guide/about-this-document)
- 범위: rolling 도움말. 검색 본문 확인.
- 근거: transactional, navigational, informational, analytical insights를
  구분한다. 자연어로 정보와 기능에 접근하며 일부 단계는 관련 앱으로
  안내한다. 실제 기능은 사용하는 SAP solution에 따라 다르다.
- 한계: 모든 자연어 업무를 실행하거나 모든 GUI를 대체한다는 뜻이 아니다.

### S03 — Transactional Capabilities

- [공식 기능 안내](https://help.sap.com/docs/joule/capabilities-guide/transactional-capabilities)
- 범위: rolling 도움말. 검색 본문 확인.
- 근거: 대화에서 business object를 조회·관리하고 관련 SAP Fiori 앱으로
  이동한다. 여러 업무 영역의 지원 기능과 insights cards를 안내한다.
- 한계: 문서에 나열된 기능의 존재는 이 프로젝트와 Joule의 연결이나
  동일한 업무 정의·시각화 동작을 입증하지 않는다.

## 의미 모델·데이터·실행 기술

### S04 — Foundation Layer: AI, Data & Knowledge Graph

- [SAP Architecture Center](https://architecture.learning.sap.com/docs/ai-native-north-star-architecture/foundation-layer)
- 범위: 2026-05-13 갱신, AI-native North Star architecture. 본문 확인.
- 근거: business semantics와 API·data-product metadata를 연결하는
  semantic grounding을 설명한다. shared enterprise ontology를
  명시적으로 **long-term vision**으로 부른다.
- 한계: 비전 문서의 모든 설명을 현재 GA 기능이나 구현 보증으로
  바꾸지 않는다. 자체 명사·동사 모델의 출처나 동일성 근거가 아니다.

### S05 — SAP Business Data Cloud FAQ

- [공식 문서 페이지](https://www.sap.com/documents/2025/02/4208db2f-f47e-0010-bca6-c68f7e60039b.html)
- [공식 PDF](https://www.sap.com/docs/download/2025/02/4208db2f-f47e-0010-bca6-c68f7e60039b.pdf)
- 범위: PDF 표지의 갱신일은 2026-07-13. URL의 2025/02와 구별한다.
  PDF 본문 3–5쪽 확인.
- 근거: SAP data product의 데이터, descriptive metadata, access API를
  설명한다. 데이터 소비에 업무 문맥을 남기는 방향과 비교할 수 있다.
- 한계: `Work`·`GoalVersion`·`Obligation`은 BDC data product와
  같은 개념이 아니다. 분석용 데이터 제공과 거래 실행을 구별한다.

### S06 — SAP HANA Cloud Feature Scope Description

- [공식 PDF](https://help.sap.com/doc/eef71122810d4aa18d8eb6c37031f98a/latest/en-US/Feature_Scope_Description_SAP_HANA_Cloud_en.pdf)
- 범위: 조사본 표지 2026-09-21, Knowledge Graph 절 10–11쪽.
  공식 PDF 본문을 확인했다.
- 근거: RDF·SPARQL 기반 Knowledge Graph engine을 기능범위에 등재한다.
- 적용 범위: 문서 서두는 일부 기능의 가용성이 license와 account
  유형에 따라 다를 수 있다고 설명한다.
- 한계: SAP Knowledge Graph의 의미 자산과 HANA의 저장·조회 엔진을
  같은 제품 단위로 취급하지 않는다. 관계형 온톨로지 설계만으로
  RDF/OWL·SPARQL·HANA 구현 경험을 주장할 수 없다.

### S07 — Defining Business Objects and their Behavior

- [SAP Learning](https://learning.sap.com/courses/building-transactional-apps-with-the-abap-restful-application-programming-model/defining-business-objects-and-their-behavior)
- 범위: ABAP RAP 교육 자료, 날짜 미표시. 본문 확인.
- 근거: business object의 data model과 behavior, 외부에 공개하는
  projection을 구별한다.
- 한계: CAP Java와 다른 개발 모델이다. 대상과 행동을 함께 다룬다는
  개념 비교에 사용하며, 명사·동사 두 진입점과의 일대일 대응이나
  이 저장소의 ABAP 구현을 뜻하지 않는다.

### S08 — CAP Java CQN Services

- [공식 개발 문서](https://cap.cloud.sap/docs/java/cqn-services/index)
- 범위: rolling CAP Java reference. 본문 확인.
- 근거: Application, Persistence, Remote Service와 CDS/CQN의 관계를
  설명한다.
- 한계: CQN을 사용한다는 사실은 SAP 원격 업무 데이터를 조회했거나
  BTP에 배포했다는 증거가 아니다. 이 저장소의 선택은 아래 L01로 확인한다.

### S09 — CAP Java Remote Services

- [공식 개발 문서](https://cap.cloud.sap/docs/java/cqn-services/remote-services)
- 범위: rolling reference. 직접 열기는 timeout, 공식 검색 본문 확인.
- 근거: remote service 정의와 protocol·destination 또는 binding 설정,
  명시적인 remote 호출이 필요하다.
- 한계: 현재 로컬 persistence 성공을 remote S/4HANA 연동 성공으로
  이월하지 않는다. 실제 API edition·인증·요청/응답은 별도 검증 대상이다.

## SAP 업무 의미와 확장 경계

### S10 — How to Extend SAP S/4HANA Cloud the Right Way

- [SAP 공식 설명](https://news.sap.com/2025/08/extend-sap-s4hana-cloud-right-way-clean-clear/)
- 범위: 2025-08-12 발행. 본문 확인.
- 근거: 표준 프로세스 유지, released interface, BTP side-by-side 및
  ABAP Cloud on-stack 확장을 설명한다.
- 한계: CAP 채택만으로 Clean Core 적합성이 성립하지 않는다.
  이 저장소의 실제 SAP integration assessment나 인증 자료가 아니다.

### S11 — Describing Fit-to-Standard Analysis Workshops

- [SAP Learning](https://learning.sap.com/courses/implementing-sap-s-4hana-cloud-private-edition/describing-fit-to-standard-analysis-workshops_c77f59be-0b11-45b3-a63d-524bc2a1731e)
- 범위: S/4HANA Cloud Private Edition 교육, 날짜 미표시.
  본문 확인.
- 근거: sandbox의 표준 프로세스를 고객 요구와 대조하고 필요한
  configuration·확장 요구를 수집한다.
- 한계: 이 문서 조사를 실제 Fit-to-Standard workshop 수행 경력으로
  표시하지 않는다. 업무 의미가 비슷하다는 이유만으로 extension을 결정하지 않는다.

### S12 — Quality Inspections During Goods Receipt

- [SAP Help](https://help.sap.com/docs/SAP_S4HANA_CLOUD/87f9b54f9c4f4e75aff0061860a6589a/4e68ee6fa3174cce917154141ef9729a.html)
- 범위: SAP S/4HANA Cloud Public Edition 2608, Warehouse Management.
  검색 본문 확인.
- 근거: inspection 설정에 따른 goods receipt, quality inspection stock,
  inspection lot, full/partial lot usage decision과 후속 처리를 설명한다.
- 한계: 모든 SAP 구성에서 동일 순서를 강제하는 설명이 아니다.
  unrestricted stock를 자체 모델의 모든 판매 조건 충족과 동일시하지 않는다.

### S13 — Applying Tolerances and the Delivery Complete Indicator

- [SAP Learning](https://learning.sap.com/courses/inventory-management-in-sap-cloud-erp/applying-tolerances-and-the-delivery-complete-indicator-1)
- 범위: Inventory Management in SAP Cloud ERP, 날짜 미표시. 본문 확인.
- 근거: 미달 수령은 partial delivery로 처리할 수 있다. Delivery
  Completed를 설정하면 전량 수령 전에도 open PO quantity가 0이 될
  수 있으며 이 강의는 잔여 goods receipt도 가능하다고 설명한다.
- 한계: indicator 하나를 자체 목표의 수량 충족 증거로 읽으면 안 된다.
  후속 GR 가능 여부는 실제 대상 제품·앱·설정에서 확인해야 한다.

### S14 — Customer Returns Processing

- [SAP Help](https://help.sap.com/docs/SAP_S4HANA_ON-PREMISE/7b24a64d9d0941bda1afa753263d9e39/ef17554b70b946e588cf4fb378fa4622.html)
- 범위: SAP S/4HANA 2025 FPS01 (Feb 2026). 검색 본문 확인.
- 근거: 기존 sales order/billing item 참조, returns order·delivery·GR,
  검사·후속 물류·환급 및 document flow를 설명한다.
- 한계: 반품·후속 처리·문서 연결이 SAP에 없다는 주장은 성립하지
  않는다. 자체 `Obligation`의 수락·이전 규칙과 동일하다고도 단정하지 않는다.

## Agent 도구와 통제

### S15 — Leveraging Tools and MCP for Custom Joule Agents

- [SAP Learning](https://learning.sap.com/courses/getting-started-with-joule-studio-classic-edition/leveraging-tools-and-model-context-protocol-mcp-for-custom-joule-agents_eb29ffe4-58db-451a-9e3c-91b4daa6ad47)
- 범위: Joule Studio, classic edition 교육. 날짜 미표시. 본문 확인.
- 근거: Agent에 공개하기 전에 underlying functionality를 독립적으로
  시험하고 운영 가능하게 해야 한다고 설명한다.
- 한계: 도구의 결정적 실행과 자연어 의도 해석의 정확성은 다른 검증이다.
  교육 지침을 오류 없는 Agent 실행의 보증으로 읽지 않는다.

### S16 — Add MCP Servers to Your Joule Agent

- [SAP Help](https://help.sap.com/docs/Joule_Studio/45f9d2b8914b4f0ba731570ff9a85313/3d9dfad0bc39468292d508f0808a12fe.html)
- 범위: Joule Studio, classic edition. 날짜 미표시. 검색 본문 확인.
- 근거: BTP destination·streamable HTTPS·별도 인증이 필요하다.
  interactive OAuth user authorization과 principal propagation에
  지원 제한을 명시한다.
- 한계: MCP를 쓴다는 공통점만으로 이 저장소의 protocol·인증 계약과
  바로 연결되지 않는다. 표준화된 호출 경로가 업무 승인을 대신하지 않는다.

### S17 — Agents

- [SAP Help](https://help.sap.com/docs/joule-studio/joule-studio/agents?version=CLOUD)
- 범위: 조사 당시 **Early Adopter Care(EAC)** 한정 페이지. 검색 본문 확인.
- 근거: MCP tools, Agent Gateway, 기존 authorization model과 A2A를 설명한다.
- 한계: EAC 밖 사용자는 사용할 수 없다는 페이지 경고를 보존한다.
  classic edition의 S16과 기능·인증 지원을 합쳐 하나의 제품 계약으로
  만들지 않는다. URL의 CLOUD나 PRODUCTION 문자열은 GA 증거가 아니다.

### S18 — SAP Joule Agents: Governance and Auditability

- [공식 문서 페이지](https://www.sap.com/documents/2026/06/526001c1-567f-0010-bca6-c68f7e60039b.html)
- [공식 PDF](https://www.sap.com/docs/download/2026/06/526001c1-567f-0010-bca6-c68f7e60039b.pdf)
- 범위: Customer Enablement briefing, v1.0, 2026-06-17. PDF 3–5쪽 확인.
- 근거: Agent identity, 위임한 사용자의 권한 범위, 기존 ERP 승인 및
  감사 적용을 설명한다. system-level enforcement를 명시한다.
- 한계: 상위 통제 방향의 설명이며 특정 연결 방식의 identity 전달을
  보증하지 않는다. S16의 제약과 함께 읽는다. 법규 적합성을 판정하는
  데 쓰지 않으며 이 프로젝트의 규제 인수·인증 근거도 아니다.

## 채용 맥락

### S19 — SAP Intern: Full Stack Developer, Mobile and Agentic Qualities

- [SAP 공식 공고](https://careers.sap.com/job/Singapore-SAP-Intern-Full-Stack-Developer-SAP-Build-Mobile-and-Agentic-Qualities-117440/1434449133/)
- 범위: Singapore, requisition 458752, 2026-10-06 게시. 본문 확인.
- 근거: MCP, Agent Evaluation, AI 개발 도구와 구현·시험·협업을 언급한다.
- 한계: 해당 해외 직무의 예시다. 한국 인턴의 요건, 지원자의 자격 충족,
  합격 가능성이나 현재 지원 가능 여부를 대신하지 않는다.

## 분석 사용자 경험과 개발 협업

### S20 — Examples of Analytical Insights Questions

- [SAP Analytics Cloud Help](https://help.sap.com/docs/PRODUCT_ID/18850a0e13944f53aa8a8b7c094ea29e/726ea0677c034dfebadbf9587213ea59.html)
- 범위: SAP Analytics Cloud 2026.21. 검색 본문 확인.
- 근거: Joule에서 sample model에 자연어 질문을 하여 chart를 조회하는
  예시와 기간·집계·ranking·measure filter를 설명한다.
- 한계: 예시 model·기능의 전제가 있다. 자체 납품 거부·단가·손실
  지표가 자동 정의되거나 모든 복합 요청을 정확히 처리한다는 뜻이 아니다.

### S21 — Joule Studio Pro-Code Workflow

- [SAP Help](https://help.sap.com/docs/joule-studio/joule-studio/pro-code-workflow?version=CLOUD)
- 범위: 조사 당시 EAC 한정. 검색 본문 확인.
- 근거: intent, requirements, specification, solution, testing,
  deployment 흐름과 사람이 요구·명세를 검토하는 AI coding agent
  협업을 설명한다.
- 한계: 개발을 돕는 coding agent와 배포 뒤 업무를 수행하는 Agent를
  구별한다. 이 프로젝트의 도구 사용을 Joule Studio 개발 경험이라고
  부르지 않으며, 이 문서만으로 지원자의 기여를 판단하지 않는다.

## 통사구조 사례 대조를 위한 API 근거 — 2026-10-22 추가

사용자는 시험 tenant가 없으며 공식 문서·API 명세와 로컬 사례부터
대조하도록 했다. S22–S26은 Public Edition 2608을 비교 기준으로
삼는다. [사례 대조](../ontology-sap-case-validation.md)의 V01–V05와
순서대로 대응한다. 직접 열기가 빈 본문 또는 접근 불가여서 공식 URL의
검색 본문을 확인했다. 실행 환경의 metadata·응답을 검증한 것은 아니다.

### S22 — Retrieve Outbound Delivery

- [SAP Help](https://help.sap.com/docs/PRODUCT_ID/588780cab2774a7ab9fffca3a7f919fe/364944fb5b1246c7b4de26401b5f1f82.html)
- 범위: APIs for Delivery and Transportation, 2608 Latest.
- 근거: `API_OUTBOUND_DELIVERY_SRV;v=2`의 header GET과
  `to_DeliveryDocumentItem` 확장 조회, 직접 접근 가능한 entity를 설명한다.
- 한계: 이 조회가 고객 거부·손실 지표를 완성한다는 뜻은 아니다.

### S23 — Outbound Delivery Items

- [SAP Help](https://help.sap.com/docs/SAP_S4HANA_CLOUD/588780cab2774a7ab9fffca3a7f919fe/3719380e965045c1a7ec98002a33ae83.html)
- 범위: 같은 API 문서의 2608 Latest, `A_OutbDeliveryItem`.
- 근거: 문서/item 식별자, Material·Batch, 문서 수량과 기준 단위 수량,
  단위, POD 관련 상태가 별도 필드로 제공된다.
- 한계: 필드 이름의 Actual을 고객의 인수 수락으로 해석하지 않는다.
  수량 필드 둘을 독립 물량으로 합산하지 않는다.

### S24 — ProofOfDeliveryRequest_In – ProofOfDeliveryDifferences

- [SAP Help](https://help.sap.com/docs/SAP_S4HANA_CLOUD/588780cab2774a7ab9fffca3a7f919fe/7d550d418b1848f5a03ea8fc7787a4ff.html)
- 범위: 같은 Public Edition API 문서, 2608 Latest.
- 근거: POD 수량 또는 차이를 입력하고 다른 수량을 계산할 수 있다.
  판매 단위·기준 단위와 차이 사유를 구분하며 사유 자동 결정에는
  설정된 기본 사유가 필요하다.
- 한계: 입력 node 명세다. 조회 endpoint가 아니며 수량 차이가 곧
  고객 인수 거부라는 뜻도 아니다. 실제 차이 사유·근거가 필요하다.

### S25 — Delivery Management API catalog

- [SAP Help](https://help.sap.com/docs/SAP_S4HANA_CLOUD/588780cab2774a7ab9fffca3a7f919fe/946a4b3ae6ec400ab56d71f92fa64b46.html)
- 범위: 같은 Public Edition API catalog, 2608 Latest.
- 근거: Proof of Delivery – Update (B2B)를 SOAP로 분류한다.
- 한계: OData 출고 조회와 같은 protocol·읽기 기능으로 취급하지 않는다.
  이번 작업에서 SOAP 쓰기를 수행하거나 채택하지 않았다.

### S26 — Operations for Returns Inspection (A2X)

- [SAP Help](https://help.sap.com/docs/SAP_S4HANA_CLOUD/03c04db2a7434731b7fe21dca77440da/97e1c03f7d3646eaac0b3eec3be207da.html?locale=en-US&version=2608.500)
- 범위: Public Edition, URL version `2608.500`.
- 근거: `api_returnsinspection_2`의 OData v4 경로에 대해
  `ReturnsInspection` GET과 create/update 등 operations를 설명한다.
- 한계: 반품 검사 데이터의 API다. 원래 납품 시 고객이 거부한 수량,
  환급액, 최종 손실을 같은 값으로 해석할 수 없다.

## 저장소 내부 근거

### L01 — CAP 로컬 채택과 실행 기록

- [S0 플랫폼 판정](../../verification/platform/decision.md)
- [S0 통합 실행 manifest](../execution/evidence/step3-s0/72b0a72/summary.json)
- [현재 backend dependency](../../backend/pom.xml)
- 계획 초기 문구와 달리 로컬 CAP 경로는 채택됐으며, 통합 manifest에는
  `72b0a72`의 기술 fixture PASS가 기록돼 있다. 현재 dependency는 CAP
  Java 5.1.1·Java 21·Spring Boot 4.1.1을 지정한다.
- 이 조사에서 테스트를 재실행하지 않았다. 해당 기록은 모든 최신
  도메인 시나리오, 운영 identity, Joule/S/4HANA/BTP 인수를 뜻하지 않는다.

### L02 — 현재 시나리오의 완료 범위

- [S5e 실행 설명](../execution/s5e-adapter/README.md)
- [실행 inventory](../execution/s5e-adapter/run-d63302d7.json)
- `d63302d7`의 실제 실행 기록은 부분 fixture PASS 109, FAIL 614,
  NOT_IMPLEMENTED 79다. 전체 인수 완료나 정확도 개선 실측으로 쓰지 않는다.
- 이 숫자는 해당 기준선의 역사적 실행 결과이며 이 문서 작업의
  테스트 결과가 아니다. 화면 학습 시간·triage 비용은 측정하지 않았다.

## 조사에서 유지한 반례와 확인 과제

1. Fiori·Joule이 이미 UX 단순화와 자연어 접근을 다룬다. 자체 설계의
   출발점이 SAP 제품의 기능 부재를 입증하지 않는다.
2. SAP 표준은 부분 입고·품질 판정·반품·document flow를 지원한다.
   자체 설계의 구체성을 설명하되 해당 개념의 발명을 주장하지 않는다.
3. Delivery Completed 이후 추가 GR 설명은 상세 강의 S13의 범위로
   한정한다. 실제 대상 제품·앱·설정 확인 없이 제품 전반의 실행
   규칙으로 일반화하지 않는다.
4. S04의 ontology 비전, S06의 graph engine, S17의 EAC 기능은 서로
   다른 범위다. 이를 묶어 현재 단일 GA 기능이라고 서술하지 않는다.
5. 국내 수입 규정과 SAP localization의 구체 충족 여부는 이 조사로
   판정하지 않는다. 인증·규제·법률 주장은 별도 근거가 필요하다.
