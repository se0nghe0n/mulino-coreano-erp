# 온톨로지 구현 계획

기준일: 2026-10-07. 문서 버전: v1.0 — 계획 완결성 검토 완료, 구현 전. 이 문서는 [설계 철학](ontology-design-philosophy.md)과 [D01–D26의 채택된 설계](ontology-discussion.md)를 실제 구현 단위·계약·인수 증거로 연결한다. 계획을 완성하는 작업과 애플리케이션을 구현·운영하는 작업은 구별한다. 아래 API·모듈·파일명은 새 구현의 규범적 설계이며 현재 저장소에 이미 존재한다는 뜻이 아니다.

## 1. Task의 범위와 완료 기준

구현 Task는 이탈리아 완제품 구매 필요 접수부터 운송·수입·국내 수령·QC·B2B 인도, 반품·회수·정산 참조와 후속 책임까지를 지원하는 새 온톨로지 시스템이다. 명사와 동사를 두 진입점으로 제공하고 같은 대상·업무·증거·책임을 읽는다. 국내 제조·BOM·혼합 가공·소비자 직접 판매·총계정원장·세금 신고·은행 이체는 제외한다. 외부 기관 제출은 초기에는 담당자가 제출·접수 증거를 기록하며, 로컬 문서 생성이 실제 제출인 것처럼 표시하지 않는다.

현재 Task는 이 구현을 수행할 수 있는 **계획의 완성**이다. 계획 완료에는 D01–D26 각각의 데이터 소유 모듈, 명령·조회·상태·불변식, 정상·예외 인수, 구현 Step와 선행 조건, 미결정값의 해소 절차가 있어야 한다. 검토자는 문서를 읽고 근본적인 업무 의미를 새로 결정하지 않고 구현할 수 있어야 한다. 실제 DB·API·MCP·Skills·BTP 실행 성공은 향후 구현 완료의 증거이며 이번 문서 검토로 대체하지 않는다.

### 채택 상태와 구현 판단

| 구분 | 처리 |
|---|---|
| 채택된 설계 | 논의 기록 §10·§12와 D01–D26의 코멘트 없는 기본안, C1–C5를 필수로 구현한다 |
| 사용자 기술 선택 | Java21, 최신 stateless MCP, Agent Skills, fork에서 새 구현을 계획한다 |
| 이 문서가 정하는 구현 계약 | 아래 식별자·명령·거래 경계·상태 전이·테스트를 계획 기준선으로 사용한다. 업무 의미를 변경하면 논의 기록에 차이를 남긴다 |
| 기존 저장소 재사용 | 필수가 아니다. Case·Work Item·Run을 새 업무에 억지로 일대일 매핑하지 않는다. 기존 기록 보존과 새 시스템의 의미 대응은 §12를 따른다 |
| D02·D05/D07·D14/D04 보완 제안 | 포장 대체·전용 계약 관리·관측 diff 상세안 전체를 자동 채택하지 않는다. 기준선은 명시적 품목/단위, 버전 있는 근거 문서, 출처 있는 활동/측정 기록을 제공한다. 자동 포장 대체·재포장 효과·법률 조항 실행·미관측값 자동 계산은 지원하지 않는다. 제안 확장은 독립 변경으로 비교·인수한다 |

계획의 선택값은 개발·검증용 기본값과 실제 운영 정책을 구별한다. 규제·실제 승인자·보존 기간·외부 계정은 코드에 임의 확정하지 않는다. 각 값은 §11의 구축 manifest에 근거·확정 주체·적용 시점과 함께 넣고 누락된 범위의 실제 운영 활성화를 막는다. 이것이 나머지 기능의 설계나 구현을 생략하는 이유가 되지는 않는다.

## 2. 아키텍처와 기술 결정 절차

초기 구현은 Java21의 단일 배포 모듈형 애플리케이션과 PostgreSQL 한 거래 경계를 기준으로 한다. CAP Java5·Spring Boot4.1·Maven·CDS/CQN을 우선 검증한다. 정확한 patch·driver·DB major·buildpack·SDK는 Step 0의 호환 manifest에 고정한다. 이는 CAP 최종 채택이나 현재 코드의 변경 완료가 아니다.

| 모듈과 새 경로 | 소유하는 데이터와 역할 | 직접 쓰기 금지 대상 |
|---|---|---|
| `backend/domain/definitions` | 어휘·slot·관계·목표/evaluator·capability 정의와 발행 | 물량·권한 효과 |
| `backend/domain/identity` | 주체·조직·역할·위임·행동 인가 | 스스로 주장한 Agent 역할을 권한으로 수용 |
| `backend/domain/work` | 업무·목표 버전·계획·의무·책임·대기·판정 | 도메인 실물 수량 |
| `backend/domain/inventory` | 품목·LOT·추적 물량·계보·원장·예약·적격성 | 자유 속성으로 핵심 수량/허용 상태 변경 |
| `backend/domain/trade` | 구매·판매·약속·운송·수입·수령·반품·회수·정산 | inventory 원장 우회 |
| `backend/domain/evidence` | 원본 문서·사건·주장·증거 연결·정정·원천 대조 | 관측을 검증된 사실로 무조건 승격 |
| `backend/domain/governance` | 승인 결정·정책·감사·보존 및 법적 보류 | 승인 없는 제한 해제 |
| `backend/application` | use case 거래·잠금·멱등성·인가·도메인 조정 | adapter별 업무 규칙 복제 |
| `backend/adapters` | OData 조회/action, MCP, 외부 inbox/outbox, 문서 저장 | 핵심 테이블 범용 CRUD 노출 |
| `backend/runtime` | scheduler·claim/lease·reconciler·실행 시도·운영 조회 | 큐 성공을 목표 성공으로 처리 |
| `agents/skills` | 발견 가능한 Agent Skills와 참고 계약·예제 | 서버 판정·권한 정책 대체 |
| `database/migrations`, `contracts`, `verification`, `deploy` | schema·공개 JSON 계약·fixture/인수 근거·배포 manifest | 생성물 수동 수정·비밀 commit |

Java package와 Maven module의 물리 분할은 이 소유 경계를 보존하는 범위에서 구현자가 정한다. 도메인 저장은 repository, 거래 조정은 application service, presentation은 adapter에 둔다. CAP 채택 시 CQN을 기본 persistence 경로로 쓰고 jOOQ/JPA의 중복 모델을 추가하지 않는다. PostgreSQL 잠금·특수 제약을 위한 제한된 고정 SQL은 전용 repository에 격리하고 바인딩과 integration test를 둔다.

### Step 0에서 닫을 기술 선택

Java21/CAP/CDS→CQN→PostgreSQL의 read/action, transaction rollback, V2·V3 잠금, Flyway upgrade, authenticated custom MCP endpoint, transactional outbox를 한 작은 vertical slice로 검증한다. BTP entitlement가 있으면 같은 조합의 binding·runtime·TLS를 검증하고 없으면 로컬 구현 기준선과 BTP 운영 gate를 분리한다. 서비스 가용성·비용이 확인되지 않은 실배포는 시작하지 않는다.

CAP가 유지되려면 하나의 schema 소유권, 하나의 업무 거래, 모든 쓰기 진입점의 동일 인가와 V1–V8이 유지돼야 한다. 이를 위해 독립적인 두 persistence 모델, CAP 밖의 별도 권한 경로, 비원자적 핵심 쓰기를 강제해야 하면 CAP 후보를 기각하고 Java21 Spring Boot+jOOQ/PostgreSQL 대안을 같은 oracle로 검증한다. 단순 코드량이나 학습 편의만으로 계약을 완화하지 않는다. 판정·정확한 버전·실행 로그를 `verification/platform/decision.md`에 남긴 뒤 도메인 구현을 시작한다.

웹 UI는 필수 MVP가 아니다. 조회와 승인·업무 관리는 MCP 대화에서 인수하며 필요하면 동일 API 위에 React/TypeScript·UI5 Web Components를 추가한다. graph DB·RDF/OWL·Temporal·Kafka·전체 event sourcing은 필수가 아니다. 관계형 계보·목표/의무·한 DB transaction·outbox/reconciler로 아래 계약을 만족시키는 것이 우선이다. 이 선택이 감사 사건·불변 이력을 생략하는 근거는 아니다.

## 3. 공통 데이터·통사 계약

### 3.1 식별자와 공통 필드

모든 aggregate는 서버 발급 불투명 UUID, `organizationId`, 낙관적 동시성용 `revision`, 생성/기록 시각을 갖는다. 외부 표기·이름·SKU는 내부 ID를 대체하지 않는다. FK에는 조직 범위가 일치해야 하며 API 필터와 DB 접근 경계 모두 검사한다. 사람·외부 거래처는 별도 조직 범위를 식별하되 초기 운영 조직은 단일 수입사다.

수량·금액은 이진 부동소수점 대신 decimal을 사용한다. 계약 전송에서는 decimal 문자열과 단위를 함께 보내며 DB는 `numeric(38,12)`를 기본으로 한다. 범위를 넘거나 기준 단위의 허용 소수 자릿수를 위반하면 조용히 반올림하지 않고 거부한다. 금액 반올림은 별도 통화·계산 정책으로 결과와 원 값을 보존한다. 수량 오차 기본값은0이며 허용 오차는 품목/정책 버전과 승인 근거가 있을 때만 적용한다.

시간은 실제 발생·유효 시점과 기록 시점을 구별한다. UTC instant와 원 offset/시간대·정밀도를 보존하고 날짜만 알려지면 날짜 범위로 저장한다. 미입력(`MISSING`), 조사 전(`UNKNOWN`), 적용 안 됨(`NOT_APPLICABLE`), 상충(`CONFLICT`)을 값0이나 null 하나로 합치지 않는다. 목표 기한은 시간대·끝점 포함 여부를 확정하며 모호하면 초안에서 확인한다.

### 3.2 정의·개별 데이터의 분리

| 개념 | 최소 필드·제약 |
|---|---|
| `DefinitionVersion` | versionId·상태·부모 버전·hash·발행자·발행 시각. 발행 후 불변 |
| `NounType` | 허용 속성·ID 정책·고정 core 참조·확장 가능 범위 |
| `AttributeDefinition` | 대상 유형·scalar/reference/enum 타입·단위·cardinality·필수 단계·허용값·정정/시간 정책 |
| `VerbDefinition` | kind=`RELATION/ACTIVITY/WORK`·역할 slot·대상 타입·개수·필수 단계·조건·지원 capability |
| `RelationDefinition` | source/target 유형·방향·cardinality·기간·순환 허용 여부. 배분은 수량 있는 독립 관계 |
| `GoalTemplate` | 목표 slot·수량 의미·끝점·evidence policy·evaluatorId/version·기본값 출처 |
| `CapabilityContract` | actionId·입출력 schema version·효과·권한·허용 정의 버전·handler/evaluator 대응 |
| `PolicyVersion` | 정책 종류·적용 대상/행동·유효기간·공식/내부 근거·확인자. 정의 의미와 별도 현재 정책 |

규칙 payload는 제공된 typed predicate의 조합만 허용한다. 초기 operator는 `equals/in`, 단위 호환 `compare/range`, `exists`, 승인된 relation 범위의 cardinality, 시간 구간 포함, `all/any/not`, 검증된 사건의 distinct quantity 합계, 현재 상태량 평가다. predicate는 property/slot 참조·타입·단위·시간 의미·evidence selector를 명시하고 미지원 operator는 발행을 거부한다. UNKNOWN/CONFLICT는 false나0으로 조용히 바꾸지 않는다. `all`은 하나라도 확정 false면 미충족, 모두 true일 때만 충족이며 그 외는 미확인이다. `any`는 하나라도 인정 가능한 true이면 충족, 모두 확정 false면 미충족이며 그 외는 미확인이다. 상충 flag는 결과와 함께 보존한다. 별도 일반 목적 DSL/parser나 임의 코드를 요구하는 계약은 아니다.

확장 속성은 승인된 schema에 맞는 값만 저장한다. 서버가 계산하는 적격량·잔량·목표 판정·역할/승인은 확장 속성으로 덮어쓸 수 없다. 관계 등록이 모든 경우에 업무나 Run을 만드는 것은 아니다. 활동은 실제 수행 사건을 남기며 목표 업무는 명시적으로 생성한다.

### 3.3 공통 요청 구조

`contracts/intent.schema.json`은 `intentKind=QUERY|RECORD|COMMAND`, `definitionVersion`, `capabilityId`, 유형이 확인된 `subjectRefs`, slot 값, 조건, 근거 참조, 원문/문맥 참조, 각 값의 provenance(`USER|CONTEXT|APPROVED_DEFAULT`)를 정의한다. 요청자·실행자·위임은 인증 문맥에서 검증하며 payload가 이를 임의로 바꾸지 못한다. 필수 slot·타입·단위·관계 cardinality·지원 효과·적용 정책 순으로 검증한다.

예시 `createWork(purchase, item=P, quantity=100 BOX, destination=W, dueAt=T, endpoint=ARRIVED, quantityMode=CUMULATIVE)`는 품목·단위·끝점이 확인돼야 최종 명령이 된다. 제조 LOT는 수령 단계 필수 slot이므로 구매 초안에 없다는 이유로 거부하지 않는다. 반면 대상·수량·기한처럼 활성 목표를 결정하는 값이 빠졌다면 `NEEDS_INPUT`으로 남긴다. `locatedAt(quantity, Friday)`는 장소 slot 타입 오류다.

해석 초안의 `conversationRequestId`는 입력 수집을 잇는 식별자이며 실물 효과 멱등키가 아니다. 추가 답변으로 초안이 바뀔 수 있다. 효과0인 구조화·검증이 끝나면 서버가 canonical intent hash와 `proposalRevision`을 확정한다. 승인 필요 행동은 이 hash/버전에 승인을 연결하고 실행 시 별도의 `commandIdempotencyKey`를 쓴다. 승인 후 내용 변경은 새 proposalRevision·재승인이며 기존 실행 키 재사용은 conflict다. 모든 쓰기에 새 승인을 요구하지 않고 §7의 행동 정책에만 적용한다.

### 3.4 공개 조회와 명령 응답

두 진입점은 `getObject`, `searchObjects`, `getWork`, `searchWorks`, `getInventory`, `getObligations`, `traceLot`, `getEvidence`, `getAssessment`, `getDefinition`을 공유한다. 명사 조회에서 연결 업무를, 업무 조회에서 대상·물량·목표·근거를 반환한다. 동일 ID·snapshotRevision/평가시점으로 읽은 결과가 일치해야 한다. 페이지는 안정된 ID tie-break와 cursor를 사용하며 page 사이 시점 변화는 명시한다. 허용 filter·sort·relation만 query schema로 열고 자유 SQL은 제공하지 않는다.

조회 응답은 `data`, `asOf`, `knownAt`, `scope`, `unknowns`, `conflicts`, `evidenceRefs`, `nextCursor`를 포함한다. 명령은 `outcome=APPLIED|WAITING_APPROVAL|NEEDS_INPUT|REJECTED|CONFLICT|ACCEPTED_PENDING_EXTERNAL`, 업무/활동/의무 ID, 새 revision, 적용 효과와 다음 행동을 반환한다. 외부 결과 불명확은 성공으로 표시하지 않는다. 오류는 `TYPE_INVALID`, `VERSION_UNSUPPORTED`, `POLICY_UNRESOLVED`, `FORBIDDEN`, `STALE_REVISION`, `IDEMPOTENCY_CONFLICT`, `INSUFFICIENT_ELIGIBLE_QUANTITY`, `EVIDENCE_CONFLICT`처럼 구조화하고 비밀이나 다른 조직의 식별 존재를 노출하지 않는다.

## 4. 물량·사건·증거의 저장과 거래

### 4.1 core entity catalog

| Aggregate | 필수 구조와 관계 |
|---|---|
| 품목 | Product·TradeItem·SpecificationVersion·PackagingVersion·UnitConversion·ExternalIdentifier(발급자/namespace/유효기간). 내용량 동등성과 주문 대체 허용은 별개 |
| 제조 LOT | Manufacturer·TradeItem·원 LOT 표기·생산/기한 근거. 외부 LOT 표기가 같아도 제조자/품목이 다르면 합치지 않음 |
| 추적 물량 | QuantitySegment: 품목·제조 LOT 또는 식별 보류 참조·양·기준 단위·위치·보관자·owner·통제 범위·active 여부 |
| 물류 단위 | LogisticsUnit과 시간 유효한 membership. 여러 LOT를 담아도 각 물량/LOT를 보존 |
| 장소/운송 | Place 계층, Shipment·Leg·CustodyHandover·마지막 확인 위치. 예정 목적지와 실제 위치 분리 |
| 물량 효과 | QuantityMovement, GenealogyEdge, Allocation, Reconciliation. 원/대상 segment·수량·단위·사건·근거·명령·정정 참조 |
| 제한/허용 | Restriction·DispositionBasis: 범위·행동·수량/기간·상태·근거·결정자. QC·규제·소유·처분·고객 조건 독립 |
| 문서/증거 | DocumentVersion(uri/hash/media/source), Claim, EvidenceLink(event/범위/역할), AssessmentInputSnapshot |
| 실제 사건 | Activity/Event: kind·대상 범위·발생/기록시각·수행자·원천 ID/version·payload hash·supersedes/invalidates |
| 외부 접수 | InboxRecord·IdentityMatch·ReconciliationCase: 원문·원천·중복키·상충·접수 담당·다음 확인·연결 결과 |

원문 파일은 접근 통제된 immutable object storage에 두고 DB는 hash·버전·접근 metadata를 관리한다. 로컬 검증은 같은 계약의 파일 저장 adapter를 사용할 수 있다. upload 완료·hash 검증 전에는 증거가 사용 가능하지 않으며 DB 참조 실패/고아 upload는 대조 작업으로 정리한다. 다운로드는 서버 인가 후 단기 참조를 발급한다. 외부 URI만 존재하고 원본을 계속 읽을 수 없다면 증거 가용성을 미확인으로 표시한다.

### 4.2 quantity transaction

기준선은 active segment의 동일 실물 범위를 중복 소비하지 않는 관계형 원장이다. 핵심 원장은 inventory 내부 primitive만 쓸 수 있다. `splitQuantity`, `mergeQuantity`, `moveQuantity`, `adjustQuantity`, `reserveQuantity`, `replaceAllocation`, `pickQuantity`, `dispatchQuantity`, `confirmReceipt`, `disposeQuantity` 등의 공개 도메인 명령이 이 primitive를 호출하며 raw movement/원장 수정은 도구나 범용 action으로 노출하지 않는다. 모든 명령은 expectedRevision·조직·멱등키·근거·행동 인가를 검증한다.

영향받는 segment/배분/제한/위임 scope를 정해진 ID 순서로 잠근다. 같은 범위의 제한 추가도 같은 scope lock을 사용해 신규 제한 phantom을 막는다. lock을 얻은 뒤 현재 유효 정책·원장·적격성과 revision을 다시 읽고 결정한다. 정책/위임 revision도 검증과 commit 사이에 바뀌지 않도록 해당 scope의 같은 fence를 사용한다. DB SERIALIZABLE 또는 동등한 충돌 검출 방식을 선택해 V2·V3·V7로 입증하고 deadlock/serialization failure는 한정 재시도한다. 재시도 후 revision 충돌이면 새 의도로 몰래 실행하지 않고 conflict를 돌려준다.

분할은 부모를 retired로 만들고 자식+잔량+근거 있는 감소를 동일 거래에 기록한다. 합침은 품목·제조 LOT·장소·통제 조건·단위·관계가 호환될 때만 허용하며 source를 소모하고 새 segment를 만든다. 계보는 방향성 비순환이며 원천 ID를 잃지 않는다. 기존 배분은 자식에 한 번만 이관한다. 제조 LOT가 다른 물량은 물류 단위로 함께 묶을 수 있어도 단일 LOT segment로 합치지 않는다.

같은 LOT의 물리적으로 섞인 물량에 특정 subset의 결함이 뒤늦게 알려졌으나 어느 실물이 그 subset인지 식별할 수 없으면 수학적 계보만으로 깨끗한 부분을 임의 선택하지 않는다. 영향 후보를 식별 가능한 전체 혼합 범위까지 넓히고 분리/검사 근거가 확보돼야 부분 해제를 허용한다. 계보 조회는 확정 영향과 후보·불확실 범위를 구분한다.

`보유량`은 현재 위치의 active physical segment를 한 번만 합산한다. `행동별 적격량`은 현재 정책·QC·규제·고객·처분 근거를 만족하는 범위다. `예약량`에는 부적격이 된 미해결 배분도 별도 표기하며 `미예약 적격량`은 적격 범위에서 실행 가능한 활성 배분을 뺀다. 신규 배분·출고는 동일 실물 초과를 금지한다. 정정/QC로 줄어든 가능량은 과거 예약을 삭제하지 않고 부족 의무와 대체 배분 대상으로 만든다.

`moveQuantity`의 실행 권한은 설정된 내부 장소·보관 범위의 이동에 한정한다. 고객/외부 인도·주문 이행·반품·폐기 효과는 이름이나 목적지 ID를 바꿔 내부 이동으로 실행할 수 없고 해당 도메인 명령의 권한·제약을 통과해야 한다. 동일한 내부 primitive를 호출하더라도 application service가 효과 class와 scope를 확인한다. 이미 발생한 무허가 이동의 관측은 아래 사실 대조 경로로 기록하며 정상 판매/출고 허가로 승격하지 않는다.

부분 이동·보류·출고는 먼저 범위가 구별되도록 분할한다. 위치·보관자·owner·위험 부담은 별도 관계이며 인계가 소유권 이전을 자동 발생시키지 않는다. 금지된 core UPDATE를 DB role과 application action 경계에서 차단하고 실제 노출된 direct/nested/batch/projection 경로 모두 검사한다.

### 4.3 관측·증거·정정

공급자 출하 주장과 창고 수령은 다른 사실이다. `sourceNamespace+externalEventId+sourceVersion` 중복 수신은 inbox에서 한 번만 접수한다. 같은 키의 다른 hash는 충돌로 남기며 last-write-wins하지 않는다. 이 키는 명령 멱등키나 실제 실물 동일성의 증명이 아니다. 서로 다른 출처가 같은 수령60을 보고한 경우 검증된 canonical occurrence/물량 범위로 연결해60으로 집계한다. 동일성 미확인·범위 겹침이 있으면 자동 합산하지 않고 대조한다.

원천 관측은 편집하지 않고 새 revision/supersedes로 정정한다. 정정 명령은 원 사건과 영향받는 current projection·판정·예약·의무를 같은 처리 단위에서 재평가하고 감사한다. 이미 출고된 물량의 과거 위치로 가짜 입출고를 만들지 않는다. 역산이 모순이면 기록은 보존하고 재고 대조 의무로 보류한다. 현재 projection의 변경이 큰 경우 revision을 pending으로 두고 조회에 미확정을 표시하며 후속 명령은 필요한 확정까지 막는다.

반품은 원 인도에 연결된 새 physical receipt이며 원 인도100을80으로 바꾸는 정정이 아니다. 실제98이었던 인도100의 정정은 당시 판정을 보존하고 현재 유효 의무2를 평가한다. 의무의 해소/면제 결정이 이미 유효하면 자동 부활시키지 않는다. 미관측은0, 수량 차이는 즉시 분실, 운송 예정은 실제 도착으로 해석하지 않는다.

## 5. 목표·업무·책임·판정의 실행 모델

### 5.1 업무와 실행 시도의 구별

`Work`는 정의·업무 유형·대상·현재 GoalVersion·상태·owner·supervisor·revision을 가진다. `ExecutionAttempt`는 work/capability/command·lease token·시작/heartbeat/종료·기술 결과를 기록한다. 한 업무에 여러 시도와 활동이 있을 수 있고 시도 성공이 목표 충족이나 업무 종료를 뜻하지 않는다. 사용자 대화도 Work ID로 이어지며 MCP 연결이 끊겨도 업무가 유지된다.

`GoalVersion`은 대상·수량·단위·지점·끝점·기한·시간대·평가 방식·기본값 provenance·evidencePolicyVersion을 가진다. 수량 모드는 `CUMULATIVE_EVENT`, `STATE_AT`, `EXISTS_IN`, `THROUGHOUT` 중 하나다. 누적은 기간·사건 종류·기여 범위·중복 배제, 시점 상태는 평가시점·위치·행동 적격성·예약 포함 여부를 필수로 받는다. 기간 중/기간 내내는 구간과 관측 충족 정책을 받으며 관측 공백이 있으면 충족을 자동 추론하지 않는다.

`Assessment`는 결과 `SATISFIED|UNSATISFIED|UNVERIFIED`, 상충 flag, goal/definition/policy/evaluator 버전, 평가시점, 입력 snapshot/증거/계산 근거, `conditionResults(conditionId,result,conflict,evidenceRefs,evaluatorVersion)`, 이전 판정 참조를 불변으로 저장한다. 규칙이 인정한 조건을 모두 확인했을 때만 충족이다. 부족을 입증할 충분한 증거가 있으면 미충족, 판정 근거가 부족하면 미확인이다. 지연 도착으로 현재 물량 의무를 해소할 수 있어도 과거 납기 위반은 유지한다.

`WorkLink`는 `CONTRIBUTES_TO|DEPENDS_ON|SHARES_ACTIVITY`를 구분한다. blocking dependency graph의 순환은 거부하고 shared activity는 모든 부모에 결과 전량을 복제하지 않는다. 물량 기여 allocation은 실제 사건 범위와 목표 조건으로 배분한다. 하위 업무가 모두 종료됐어도 부모 목표100에 기여90이면 부모 충족이 아니다.

### 5.2 상태 전이와 명령

| 명령 | 전이/조건 | 같은 거래에서 보장하는 결과 |
|---|---|---|
| `createDraft` | 없음→DRAFT, 부족한 slot 허용 | Work·원문/해석·초안 GoalVersion·요청 ID. 실물/발주 효과 없음 |
| `cancelDraft` | DRAFT→CLOSED/CANCELLED | 초안 폐기 사유·요청/감사 기록. 실물/외부 효과 없음; 접수 의무가 연결됐다면 책임 해소/이전 검증 |
| `activateWork` | DRAFT→ACTIVE | 필수 slot·지원 evaluator·주 책임자·현재 권한 검증, 목표 snapshot·초기 의무 |
| `waitWork` | ACTIVE→WAITING | reason·대상·재개 predicate·verifier·nextCheckAt·overdueAction 필수 |
| `resumeWork` | WAITING→ACTIVE | 재개 조건의 근거와 현재 권한 확인. 시간 경과만으로 승인/도착을 생성하지 않음 |
| `reviseGoal` | DRAFT/ACTIVE/WAITING 유지 | 새 GoalVersion·변경자·사유·이전 판정 유지. 승인 범위 변경 시 재승인 요구 |
| `closeWork` | ACTIVE/WAITING→CLOSED | 종료 사유 `FULFILLED\|CANCELLED\|IMPOSSIBLE\|SUPERSEDED`, 잔여 의무 해소 또는 후속 책임 연결 검증. FULFILLED는 현재 GoalVersion의 모든 필수 조건이 현재 적용 가능한 근거 revision에서 SATISFIED이고 판정에 영향을 주는 미확정 처리가 없어야 함 |
| `createFollowup` | 새 업무 | 종료 후 정정/반품/이상의 현재 의무를 새 업무에 연결. 원 업무/과거 판정은 변경하지 않음 |

의무를 다른 업무로 넘겼다는 이유만으로 원 목표90/100을 FULFILLED로 종료할 수 없다. 이런 경우 승인된 취소·불능·대체 종료 사유와 미충족 판정을 보존한다. 목표가 도착인 업무는 도착 조건이 충족되고 잔여 QC 책임이 별도로 연결됐을 때 이행 종료할 수 있다.

폐쇄 업무의 원 사건을 다시 쓰는 `reopen`은 기본 action으로 제공하지 않는다. 새 사실·정정은 판정 이력과 followup을 만들고 원 업무의 연결 조회에서 현재 상황을 볼 수 있다. 취소/불능 종료도 이미 출하된 실물·외부 계약·청구를 자동 취소하지 않는다. 모든 전이는 expectedRevision으로 경합을 막으며 종료 사유와 목표 충족 여부는 독립이다.

### 5.3 의무와 책임 인계

`Obligation`은 sourceOccurrence/decision, kind, 대상 범위, 현재 유효성, status=`OPEN|RESOLVED|TRANSFERRED|WAIVED`, responsibleWorkId, ownerId, nextAction, nextCheckAt, resolution/waiver/transferEvidence를 갖는다. source+kind+scope+발생 버전에 안정된 의무키를 두고 같은 사건 재처리로 중복 생성하지 않는다. 같은 내용의 새 사건은 새 의무일 수 있다. 무효화/면제는 권한 있는 근거를 요구한다. `resolveObligation`은 해당 의무의 충족 증거를 검증하고, `waiveObligation`은 의무 종류별 권한자의 면제 결정·사유를 요구한다.

업무 간 이전은 원 의무를 지우거나 새 의무를 독립 생성하지 않는다. `transferObligation`은 stable obligation root 아래의 기존 assignment를 TRANSFERRED로 닫고 새 targetWork assignment를 OPEN으로 만든다. 대상 업무·담당의 수락과 양쪽 링크·효력시점은 같은 거래이며 수락 전 원 assignment가 OPEN이다. root당 현재 유효 assignment는 하나다. 거절/만료는 원 책임을 유지하고 이전 체인과 현재 책임자를 양방향 조회한다. root의 미해결 유효성은 assignment의 TRANSFERRED만으로 해소되지 않는다. 대상 업무는 ACTIVE/WAITING이고 인간 owner·nextAction·nextCheck를 갖춰야 한다. 이전 체인은 순환을 거부한다. 부분 이전은 원 의무 scope를 겹치지 않는 자식 scope로 분할해 원량=이전량+잔여량을 보존하고, 각 scope의 유효 assignment가 하나씩 남도록 같은 거래에서 확정한다. 전체와 자식 의무를 합산해 잔여량을 중복 표시하지 않는다.

활성 업무의 주 책임자는 한 명의 인간이다. 수행자·Agent·외부 조직·판정자는 따로 기록한다. 초기 배정은 즉시 가능하고 기존 owner의 교체는 `Handover(PROPOSED→ACCEPTED|REJECTED|EXPIRED)`로 잔여 의무·상태·권한을 전달한다. 수락과 owner 변경은 한 거래이며 수락 전 기존 owner가 유지된다. 긴급 재배정은 ADMIN의 별도 명령·사유·감사로 수행한다. 부재/비활성화는 감독자에게 책임 공백으로 노출하고 단순 알림을 인수 수락으로 처리하지 않는다.

외부 관측 접수는 지정된 intake owner를 가진다. 정책상 의무가 없으면 근거를 남기고 접수만 종료한다. 의무가 있으면 신규/기존 업무와 연결하고 owner·다음 행동이 확인된 뒤 접수 추적을 종료한다. 업무 생성/배정/연결 실패 시 접수 담당·기한이 유지된다. 정상 관측마다 Work/Run을 만들지 않는다. intake owner와 supervisor가 설정되지 않으면 외부 업무 접수 기능을 활성화하지 않는다.

## 6. 도메인 command와 목표 계약

모든 명령은 §3 공통 envelope를 쓰며 인증·현재 위임·조직 범위·정의/capability·expected revision·필요 승인을 검사한다. 성공한 명령의 도메인 기록, 원장/배분 변경, 의무·판정의 변경 표시, 감사, outbox 요청, 멱등 결과는 한 DB transaction에 들어간다. 크거나 외부 의존 판정은 그 transaction에서 pending obligation/outbox를 만들고 나중에 확정하며 즉시 성공 판정을 꾸미지 않는다. 원문 upload와 외부 시스템 효과는 이 DB transaction에 포함된다고 가정하지 않는다.

| 명령 묶음 / 소유 모듈 | 입력·전제 | 상태와 거래 효과·잔여 책임 |
|---|---|---|
| `registerItem`, `linkExternalId` / inventory | 규격·기준 단위·외부 issuer/namespace·유효기간, 품목 관리 권한 | stable item/spec version·코드 충돌 대조. 이름/코드 일치만으로 병합 금지 |
| `recordActivity`, `attachEvidence`, `correctEvidence` / evidence | 원천/발생시각·대상·범위·문서 hash·정정 원본 | 사건/주장과 증거를 분리. 판정 재평가·대조 의무, 중복 실물 효과 금지 |
| `proposePurchase`, `revisePurchase`, `approvePurchase` / trade+governance | 필요 출처·item/qty/unit/price/currency/due/destination·분할/대체 조건·GoalVersion | immutable Proposal/OrderRevision. MANAGER 결정은 hash에 결합. 변경 범위가 달라지면 기존 승인 무효 |
| `dispatchPurchaseOrder`, `recordSupplierReply` / trade | 승인된 revision·전달 채널/외부키, 공급자 근거 | 전달 요청/외부결과 대조와 SupplierCommitment의 수락/거절/변경. 공급자 수락·발주 전달·물품 도착은 별개 |
| `cancelPurchase` / trade | current revision·취소 범위·권한·근거 | 미실행 부분 취소 요청, 출하/수령/청구는 보존. 외부 취소 수락 미확인은 담당 대기 |
| `createShipment`, `recordLegEvent`, `recordHandover` / trade | origin/destination/carrier·cargo scope·PO line allocation·계획/실제 구별 | 여러 발주/선적의 배분, 구간 사건·보관 인계. 운송 지연·미확인·차이는 대응 의무; 예정에서 실물 생성 금지 |
| `prepareRegulatoryProcedure`, `recordSubmission`, `recordRegulatoryDecision`, `verifyLabel` / trade | 절차/정책 version·신고 item·품목/LOT/실물 scope·기관 문서 | PREPARED/SUBMISSION_UNCONFIRMED/SUBMITTED와 외부 결과를 구별. 보완/반려/재제출·부분 허용은 원 version 연결. 판매 허용에는 별도 QC/처분 근거도 필요 |
| `receiveProvisional`, `confirmReceipt` / inventory+trade | 실제 관측·위치·시간·품목/LOT 식별 수준·PO 기여 후보 | 임시 접수는 가용0. 확인 후 실제 물량/원장·ReceiptContribution·잔여/초과 대조. 기존 운송 실물 이동인지 최초 접수인지 확인해 이중 생성 방지 |
| `placeHold`, `releaseHold`, `recordDispositionBasis`, `revokeDispositionBasis` / governance+inventory | 물량 scope·행동·기간·decision/evidence·현재 revision | 제한은 독립 기록. release는 참조한 제한만 해제. QC 담당 결정, 처분 근거 확인권한 적용. 영향 예약 정지·의무 보존 |
| `splitQuantity`, `mergeQuantity`, `moveQuantity`, `recordStocktake`, `adjustQuantity`, `disposeQuantity` / inventory | 원천 leaf·qty/unit·위치·근거·범위 | §4 보존/잠금. 실사는 관측이며 승인된 조정과 분리. 폐기/분실/샘플 감소 사유·승인과 원차이 보존 |
| `createSalesOrder`, `reviseSalesOrder` / trade | customer·item/qty/unit/due/destination·인도 끝점·품질/포장 조건 | SalesOrderRevision/Line·별도 인도 목표. 변경은 이미 확정된 배송/청구 보존 |
| `reserveQuantity`, `replaceAllocation`, `releaseAllocation` / inventory+trade | current eligible scope·order line·처분 근거·양·revision | 배분 `EXECUTABLE\|SUSPENDED\|REPLACED\|CONSUMED\|RELEASED`. 대체+원배분 비활성 한 거래. 과거 대체된 예약 자동 부활 금지 |
| `pickQuantity`, `dispatchQuantity` / inventory+trade | active executable allocation·현재 적격성·출발/목적 범위·revision | 미래 실행을 허가하는 명령. 출고는 allocation 소비·창고 보유 감소·운송 위치 이동, 계보 유지 |
| `recordDelivery`, `recordObservedMovement` / evidence+trade | 검증된 RECORD 주체/원천·Dispatch/CargoScope·이미 소비된 배분 참조·실제 시각/범위/증빙 | 관측 claim 접수→원천/동일성/수량 대조 후 확정 사건을 반영. 현재 판매 허용이 없어도 실제 발생 사실을 보존하며 위반/회수 대응 의무 연결. 출고가 없는 창고 물량을 이 경로로 정상 판매 인도 처리하지 않음 |
| `authorizeReturn`, `receiveReturn`, `decideReturnDisposition` / trade+inventory | 원 인도·대상 실물/수량·사유·반품 결정 | 기존 고객 물량을 이동하거나 미식별 임시 접수. 동일 실물 반복 반품 생성 금지. 반품 기본 QC 보류, 교환/환불/정산 의무 별도 |
| `openInvestigation`, `proposeRecall`, `approveRecall`, `recordRecallNotice`, `recordRecovery`, `closeRecall` / trade+governance | LOT/영향 후보 scope version·근거·ADMIN 승인·처리 증거 | 후보≠오염 확정. 승인 scope·통지·실회수·처분·미확인 독립. 종료 대조와 미해결 예외 책임 필요 |
| `recordInvoice`, `recordCharge`, `matchInvoice`, `recordSettlementAdjustment`, `recordPaymentReference` / trade | invoice kind·line·currency·FX snapshot·PO/receipt/sale scope·근거 | 수량/가격/통화 차이와 settlement goal. 원 금액/환산값 분리. 상업송장≠국내 세금 증빙, 지급 참조≠은행 이체 |
| Work/Obligation/Definition 명령 / work+definitions | §5·§8 계약 | 물량 쓰기 없이 목표·책임·정의 의미의 lifecycle 관리. 실물 action 필요 시 동일 application command로 연결 |

### 업무별 추가 규칙

**구매·운송:** 구매100 요청은 계획 수량이며 현재 재고100을 만들지 않는다. 공급자 출하 주장과 실제 실물 식별·인계의 확정은 evidence policy에 따른다. 공급자 대체는 품목 대체가 아니다. 부분 수령은 PO line의 누적 기여에 배분하고 초과량은 원 주문 완료량에 몰래 포함하지 않는다. 반품 접수나 같은 실물의 창고 재이동은 원 구매 도착의 새 기여로 세지 않는다. 기여는 목표의 허용 사건 종류와 canonical 실물/이행 범위에 묶는다. 납기 변경은 원 약속·현재 약속·과거 위반을 함께 보존한다.

**수입·수령:** 기관 결과는 item/실물 scope에 결합하고 유통 적격성과 내부 QC를 별도 계산한다. 기관 허용30·QC통과100이면 해당 나머지 조건이 같을 때 판매 후보는 최대30이다. 규제 policy가 없으면 공식 법적 적격성을 주장하지 않는다. 식별/수량 불확실 접수도 기록하되 대조 전 정규 적격 재고는0이다.

**판매:** `evaluateEligibility(action, customer, scope, evaluationTime)`은 조건별 `ALLOWED|DENIED|UNKNOWN|CONFLICT`와 근거를 반환한다. 불명확 범위는 confirmed eligible에 넣지 않는다. FEFO는 적격 후보 간 정렬의 기본 제안이며 예외는 사유를 남긴다. 확인된 포장 대체 허용 없이 동량만으로 이행하지 않는다. 출고와 고객 인도를 구별하고 인도 끝점은 주문 계약대로 평가한다.

**사실 기록과 실행 권한:** 출고 뒤 배분이 CONSUMED여도 `recordDelivery`는 해당 Dispatch/CargoScope로 인도를 기록한다. recall/처분 허용 만료 뒤 실제20이 인도됐다면 사실·위반·대응 의무를 남겨야 하며 현재 SELL 부적격을 이유로 사실을 삭제하지 않는다. 인증된 원천이라는 이유만으로 허위 보고를 확정하지 않는다. 식별/수량/사건 연결이 미확인·상충이면 inbox/claim에 보관하고 재고·정상 이행 효과0과 대조 책임을 남긴다. 확인된 사건의 `applyObservedDelivery`는 기존 운송 실물을 고객으로 대조 이동시킬 수 있지만 새 창고 출고나 새 배분을 만들지 않는다. 실물 사실 반영과 목표 충족은 별도다. 확인한 인도 사실은 보존하되 GoalVersion의 끝점/제약/evidence policy로 기여와 판정을 다시 계산하므로 비준수 사건을 무조건 정상 이행으로 표시하지 않는다. prior Dispatch가 없는 이동 보고는 별도 실물 대조/권한 있는 조정으로 처리하고 정상 출고 승인·주문 이행을 자동 생성하지 않는다.

**반품·회수:** 반품20을 원 인도60의 일부와 연결하고 이미 반환한 범위를 다시 재고로 만들지 않는다. 재판매는 자동 허용하지 않는다. 회수 수량은 대상 version별 중복 없는 실물 범위를 기준으로 대조한다. 회수25 뒤 폐기25는 회수50이 아니다. 현재 위치/통제와 최종 처리 상태를 별도 축으로 둔다. 종료 대조는 `안전 해소·폐기·확인된 소비/손실·승인된 미해결 예외·미확인`의 상호 배타적 범위를 사용한다. 미확인은 근거 있는 ADMIN 예외 결정과 잔여 책임 없이 종료할 수 없다. 제출·접수 증거는 회수 승인과 별개다.

**정산:** PO line↔receipt contribution↔invoice line과 sale/delivery↔invoice를 수량·가격·통화별 대조한다. 물류 목표가 충족돼도 정산 차이는 별도 미해결 목표다. 어떤 수령 상태를 지급 근거로 인정하는지는 확인된 조건 정책으로 결정하며 QC pass를 자동 지급 승인으로 해석하지 않는다. 환율은 pair/rate/date/source/policy snapshot, 금액은 원통화/원금/환산값을 함께 보존한다. 허용 차이 내라도 원차이·조정 근거를 지우지 않는다. 실제 은행 지급·수금·세금 발행/신고 자동 연동은 미지원이다.

## 7. 신원·승인·멱등성·감사

### 7.1 신원과 행동 권한

AuthContext는 검증한 issuer/subject/audience/organization에서 만든다. Agent grant는 delegator·actor·actions·target/work scope·validFrom/until·revokedAt·revision을 서버에서 조회한다. 미래 업무 실행의 권한은 조직 접근∩역할∩현재 위임∩행동별 승인∩물량별 행동 허용의 교집합이다. 관측 접수는 RECORD 권한·원천·범위를 검사하며 현재 판매 적격성을 요구하지 않는다. 접수한 주장을 실물/목표의 확정 사실로 반영하는 것은 증거 정책·실물 대조·중복 방지 조건을 별도로 통과해야 한다. 외부 거래처 역할은 내부 승인 권한을 만들지 않는다. 사용자 역할을 payload나 문서로 받아 권한을 넓히지 않는다.

| 행동 | 필요한 수행 권한 | 별도 결정/승인 |
|---|---|---|
| 조회·검색·증거 다운로드 | 해당 조직/업무 READ | 읽기마다 업무 승인 없음 |
| 일반 초안·사건/증거 등록·운송/수령 기록 | 배정된 업무 WRITE/RECORD와 범위 | 원천 사실 입력이 승인·QC 해제를 의미하지 않음 |
| 구매 확정·승인 범위 변경 | PROCUREMENT 제안 / MANAGER 결정 | MANAGER가 proposal hash를 승인 |
| QC 보류·해제 | QC scope | QC의 사유·근거 있는 결정. 일반 Agent는 제안만 가능 |
| 회수 결정·scope 변경·예외 종료 | ADMIN | ADMIN 승인에 scope version·hash 결합 |
| 예약·피킹·출고 실행 | SALES/WAREHOUSE의 지정 capability | 별도 승인 규칙이 없으면 새로운 인간 승인 추가 없음. 현재 행동별 적격성 필수 |
| 인도·반품·이미 발생한 이동의 관측 기록 | RECORD capability와 조직/대상/원천 scope | 현재 SELL 적격성은 접수 전제가 아님. canonical 사실 반영은 §6 원천·동일성·수량·중복·evidence policy 대조를 통과해야 함 |
| 재고 감소/실사 조정·처분 근거 확인·정산 차이 확정 | 해당 업무 제안 / 확인된 관리 capability | 기본 배포 정책은 MANAGER 확인. QC/회수 관련 제한은 해당 QC/ADMIN 결정 추가 필요 |
| 책임 인계·긴급 재배정 | 기존 owner+인수자 / ADMIN | 인계 수락 또는 긴급 재배정 사유 |
| 정의 발행·정책 활성화 | FDE 작성 / 지정된 CONFIG_APPROVER | §8 영향 분석·회귀 결과를 검토한 승인 |

이 표의 신규 capability는 구현 기본 정책이다. 역할명 문자열 자체가 권한이 아니며 실제 사용자의 capability 배정은 구축 manifest에서 확정한다. policy 미설정 행동은 효과 없이 차단하고 필요 확인을 반환한다. FDE와 ADMIN도 일반 업무 권한을 암묵적으로 전부 갖지 않는다.

승인은 immutable proposal/scope hash, 대상 revision, 승인자, 승인 시각, 유효기간·소비 정책을 저장한다. 부정/조건부 결정도 보존한다. 영향 내용이나 정책이 바뀌면 실행 전 재검토하며 과거 승인을 새 행동에 재사용하지 않는다. 실행한 동일 승인 요청 replay는 기존 효과를 반환할 수 있지만 현재 조회 인가를 검사한다.

### 7.2 control plane과 대조 명령

아래도 §3 envelope·revision·멱등성·감사·조직 fence를 적용한다. 관리 명령을 `recordActivity`나 직접 SQL에 숨기지 않는다. bootstrap identity/capability는 검토한 설치 manifest와 일회성 관리 절차로 생성하고 그 뒤에는 아래 명령만 사용한다.

| 명령·조회 | 실행자와 상태/거래 guard |
|---|---|
| `createGrant`, `revokeGrant`, `getGrant`, `getAccessContext` | 인간 delegator가 자신에게 있는 조직/행동/대상 범위 안에서 grant를 발급한다. Agent는 자기 현재 위임을 확대할 수 없다. 철회는 grant revision·효력시점·영향 nextValidityBoundary를 같은 거래에 갱신하며 실행 fence와 직렬화한다 |
| `assignCapability`, `revokeCapability` | 설정된 조직 IDENTITY_ADMIN의 관리 범위에 한정한다. 조직 밖/허용 관리 scope 밖 권한을 부여하지 않는다. capability assignment revision·현재 권한 projection·감사·영향 duty를 갱신한다. 모든 FDE/업무 ADMIN에게 이 권한을 암묵 부여하지 않는다 |
| `proposeHandover`, `acceptHandover`, `rejectHandover`, `emergencyReassign` | 현재 owner·인수자·ADMIN의 §5 조건을 검증한다. 수락/현재 책임 변경은 원자적이며 만료/거부 중 owner 불변이다 |
| `resolveObligation`, `waiveObligation`, `transferObligation` | owner의 수행 증거 또는 의무 종류별 권한자의 면제/인수 근거를 요구한다. 현재 scope/revision·후속 유효성·수량 분할을 검사하고 현재 책임의 단절/중복을 거부한다 |
| `createPolicyDraft`, `approvePolicy`, `activatePolicy`, `retirePolicy`, `getPolicy` | FDE draft→CONFIG_APPROVER 승인→내용 hash 불변 발행→효력시점 활성/신규 사용 종료. 공식/내부 근거·영향 scope·회귀 결과·정책 fence·boundary index 갱신을 요구한다. 의무를 소급 삭제하지 않는다 |
| `matchSourceIdentity`, `linkCanonicalOccurrence`, `resolveEvidenceConflict`, `getInbox`, `getReconciliation` | 지정된 접수/대조 담당의 scope 안에서 원천·후보·판정 근거를 기록한다. 다른 조직/겹친 실물/임의 source 우선순위를 거부한다. 원문 보존·확정 또는 미확인 유지·필요 projection/판정/의무 변경을 한 거래 또는 명시 pending 처리로 연결한다 |
| `recordExternalReconciliation` | 지정 대조 담당이 externalOperationId의 조회/접수 증거로 외부 성공/실패/미확인을 확정한다. 확인된 성공이면 로컬 결과를 연결하고 외부 재발행 금지, 확인된 실패만 정책상 재시도 가능으로 전이한다 |
| `searchOperationalIssues`, `retrySafeCommand` | OPERATIONS scope. retry는 새 권한이 아니며 원 canonical 명령/멱등키·현재 위임·claim fence를 다시 검사한다. UNKNOWN_EXTERNAL은 대조 완료 전 실행하지 않는다 |

인가 실패·scope 변경·중복 관리 요청도 구조화된 결과와 감사로 남긴다. 활성 정책은 기존 의미 사전과 분리돼 효력시점에 적용한다. `retirePolicy`가 법적으로 유효한 제한을 임의 해제하지 않게 종료 근거와 대체 정책/적용 범위를 검사한다.

### 7.3 업무 멱등성과 외부 효과

DB unique `(organizationId, stableRequestOwner, capabilityId, commandIdempotencyKey)`에 canonical payload hash·definition/capability version·결과 refs를 묶는다. stableRequestOwner는 token/RPC/Run ID가 아니며 서버가 확인한 요청 주체/위임 연속성이다. 다른 주체의 동일 key는 독립 namespace이거나 인가 거부이며 타인의 결과를 누설하지 않는다. 같은 key의 다른 hash는 conflict다.

명령 record 상태는 `IN_PROGRESS|COMMITTED|REJECTED|UNKNOWN_EXTERNAL`이다. 내부 DB 효과와 COMMITTED 결과는 한 거래이며 rollback된 IN_PROGRESS를 성공으로 재사용하지 않는다. claim은 expiry/fencing token을 가지고 stale worker가 commit하지 못한다. REJECTED 결과를 수정하려면 새 canonical 요청·새 key를 사용한다. transient rollback은 같은 key로 재시도할 수 있다. 효과 tombstone은 해당 업무 효과의 보존 기간보다 먼저 삭제하지 않으며 오래된 key가 새 효과를 만들지 못한다.

외부 전달은 outbox에서 안정된 externalOperationId로 수행한다. 상대가 멱등키를 지원하면 같은 ID를 쓴다. 응답 유실·시간초과는 `UNKNOWN_EXTERNAL`로 두고 상대 상태 조회/담당 대조 후 재시도한다. 멱등 지원·조회 수단이 없는 외부 쓰기는 자동 재발행하지 않는다. 로컬 작업 취소는 이미 성공한 외부 발주·보고·이체의 취소가 아니다.

### 7.4 감사·보존

감사는 actor/delegator/work/target/action·변경 전후/효과 refs·원 요청·승인·근거·definition/policy version·결과를 기록한다. 감사 저장 실패는 같은 DB의 업무 효과를 rollback한다. 조회 감사는 업무 상태 변경과 구별한다. token·비밀·불필요한 개인 원문을 queue/error/log에도 남기지 않는다.

문서·원장·정의·감사·개인정보별 retention policy는 적용일·공식/내부 근거·법적 보류·삭제 방식·확인자를 가진다. 기간을 단일 상수로 확정하지 않는다. 삭제는 FK/증거 참조/진행 의무와 충돌하는지 검사하고 tombstone/redaction과 실제 blob 삭제의 대조 로그를 남긴다. legal hold 중 삭제는 거부한다. 복원본에도 적용해야 할 삭제 이력과 보존 범위를 runbook에 포함한다.

## 8. FDE 사전 발행·버전 전환

`DefinitionPackage` 상태는 `DRAFT→IN_REVIEW→PUBLISHED→ACTIVE→RETIRED_FOR_NEW_WORK`다. 반려하면 새 draft revision을 만든다. PUBLISHED 내용/hash는 불변이며 ACTIVE 포인터만 배포한다. `createDefinitionDraft`, `validateDefinition`, `submitDefinitionReview`, `approveDefinition`, `publishDefinition`, `activateDefinition`, `retireDefinition`, `migrateWorkDefinition`을 제공한다.

발행 검사에는 slot·relation 타입/cardinality, 필수 단계, 단위 정책, 관계별 순환, goal template의 완결성, evaluator/capability 지원 manifest, 조직 scope, regression fixture 결과가 포함된다. 이름 변경·선택 속성 추가·필수 속성/단위/끝점/관계 의미 변경을 diff로 구별한다. arbitrary SQL·script·새 실행 권한은 정의 payload에 넣을 수 없다. 새 효과는 개발자가 capability를 구현·배포해야 한다.

manifest는 `definitionVersion → capability semanticVersion / evaluatorVersion / input-output schemaVersion / supportedWorkMigration`을 명시한다. 기존 업무는 원 GoalVersion·정의·판정 의미를 유지한다. 현재 행동의 법규·안전·권한 정책은 별도 유효시점으로 다시 적용한다. 미지원 과거 evaluator는 최신 뜻으로 몰래 대체하지 않고 효과 없이 보류·담당에게 알린다.

명시적 전환은 영향받는 업무/자료 목록, mapping·미지원값·회귀 결과, 변경 승인, 새 GoalVersion과 이전 참조를 남긴다. 수량이나 끝점이 바뀌는 전환은 목표 변경의 권한/승인도 통과한다. ACTIVE 포인터 복구는 이미 발생한 데이터/외부 효과를 되돌리지 않는다. v1 업무의 진행/정정/재평가와 v2 신규 업무, 현재 제한을 동시에 인수한다.

## 9. Stateless MCP·Agent Skills·외부 adapter

### 9.1 protocol 계약

확인한 최신 공식 규격은 MCP `2026-07-28`이다. 새 adapter는 request마다 version/clientInfo/capabilities `_meta`를 검증하고 `server/discover`를 제공한다. HTTP의 대응 header와 본문 불일치는 거부한다. 세션 없는 구형 서버 설정을 최신 규격 준수라고 부르지 않는다. 현대 protocol에는 initialize handshake나 서버발 JSON-RPC 요청을 가정하지 않는다. [규격](https://modelcontextprotocol.io/specification/2026-07-28), [versioning](https://modelcontextprotocol.io/specification/2026-07-28/basic/versioning), [transport](https://modelcontextprotocol.io/specification/2026-07-28/basic/transports)

MCP tools는 §3 조회와 §5–§8의 지원 command schema를 노출한다. resources는 인가된 정의·업무·증거 요약, prompts는 선택적인 진행 안내다. schema 안의 core 필드는 OData/action과 같은 version을 사용한다. MCP adapter가 별도 인가·멱등 저장소를 만들지 않는다. 서버 도메인 오류를 protocol error 또는 tool result로 매핑하는 표를 `contracts/mcp-errors.md`에 고정하고 도메인 outcome을 잃지 않는다.

MRTR는 `input_required`·`inputRequests`·`requestState`에 대응하는 `inputResponses`로 재요청하며 매번 새 JSON-RPC ID를 쓴다. 상태는 주체·method/의도·TTL에 묶고 무결성 검증하며 single-use가 필요한 승인 소비는 DB에서 원자적으로 강제한다. `requestState`나 client의 accept 문자열이 업무 승인 자체는 아니다. 입력을 모으는 intent ID와 최종 효과 key는 §3.3대로 구별한다. client capability 없는 elicitation은 요구하지 않고 명시적인 미지원/추가 입력 방법을 반환한다. [MRTR 규격](https://modelcontextprotocol.io/specification/2026-07-28/basic/patterns/mrtr)

기본 원격 transport는 authenticated Streamable HTTP다. stdio는 로컬 개발/client 검증이 필요한 경우 같은 protocol과 제한된 자격을 사용한다. 구형 protocol 지원은 독립 adapter+compatibility test를 추가하기 전 제공한다고 표시하지 않는다. 실제 SDK와 target client의 version·지원 기능은 Step 0에서 조사하고 S5에서 wire transcript로 인수한다. SDK가 최신 규격 일부를 지원하지 않으면 얇은 protocol adapter를 구현하거나 호환 client를 사용해 요구 규격을 유지하며, 구형으로 몰래 낮추지 않는다. 대상 host의 미지원은 해당 client 통합의 미완료로 남긴다.

### 9.2 Agent Skills 산출물

파일 기반 [Agent Skills 규격](https://agentskills.io/specification)을 따르며 각 skill에 `name`·`description` frontmatter, 필요한 `references/`·`scripts/`를 둔다. metadata→본문→참고 자료의 점진 로딩을 사용한다. `allowed-tools`는 실험적 client 기능이므로 서버 인가를 대체하지 않는다. Skills over MCP는 별도 optional extension이며 기본 구축의 필수 조건으로 추가하지 않는다.

| 새 skill 디렉터리 | 필수 절차와 지원 command |
|---|---|
| `ontology-work-coordinator` | 두 진입점 검색→같은 ID 확인→모호한 slot 확인→목표/의무 연결→권한 범위의 하위 작업 위임→후속 책임 확인 |
| `ontology-procurement-transport` | 구매 proposal/승인 상태 조회→발주 전달 결과→공급 약속→운송 증거→잔여 의무. 승인 획득을 Agent 자기 선언으로 대체하지 않음 |
| `ontology-import-qc` | 수입 scope/정책 확인→제출/기관 근거 등록→부분 수령/식별 대조→QC 결정 제안/권한 실행→남은 제한 확인 |
| `ontology-sales-returns-recall` | 행동별 적격성→예약/출고/인도→반품 vs 정정 구별→회수 범위/ADMIN 승인→수량·미해결 책임 대조 |
| `ontology-settlement` | 송장 종류 식별→물류/금액 대조→차이·지급참조·정산 목표. 실제 이체 도구 없음 |
| `ontology-definition-authoring` | 현재 사전 읽기→허용 타입/capability로 draft→영향 분석/회귀→지정 승인→불변 발행. 운영 Agent의 임의 어휘 확장 금지 |

모든 package는 name/version/hash·지원 정의/capability/schema·검증한 client 범위 manifest를 갖는다. `.agents/skills`와 `.claude/skills`는 repository 규칙대로 실제 skill 디렉터리를 가리키는 link를 구성하고 오래된 문서/명령을 정리한다. 서버가 확인하는 것은 capability/definition compatibility와 grant다. client가 보낸 skill hash만으로 실제 loading이나 안전한 해석을 증명하지 않는다.

인수는 skill 검색 노출·본문/참고 자료 loading·올바른 실제 tool 호출·남은 의무 확인을 별도로 남긴다. 두 진입점·다국어/동의 표현·모호함·같은 이름의 여러 대상·문서 속 악성 지시·오래된 skill을 포함한다. server 계약 test와 실모델 의미 평가를 구별한다. 유료 모델 호출은 비용 범위 승인 후 수행한다.

### 9.3 외부 원천

초기에는 인증된 담당자의 document/event 접수 adapter를 제공하고 자동 운송사/창고/기관 connector는 검증된 source profile이 있을 때만 활성화한다. profile은 기준 사실, source namespace, 식별자 mapping, revision/시점 신뢰, schema, 중복 정책, 충돌 대조 owner, 재시도/외부 쓰기 여부를 정의한다.

inbox 접수→식별→검증→canonical 사건/증거 연결→판정/의무 영향 처리 상태를 영속화한다. 식별 실패·상충·순서 미확정은 quarantine과 owner/nextCheck로 남긴다. 외부 source version의 선후가 검증되지 않으면 도착 순서로 현재 제한을 덮지 않는다. adapter output·문서는 데이터이며 Agent 지시나 권한이 아니다.

## 10. 실행 복구·관측·운영

outbox는 DB commit과 실행 요청을 연결하지만 성공 메시지의 보관을 지속 업무 원장으로 사용하지 않는다. scheduler는 due obligation·WAITING nextCheck·미연결 intake·UNKNOWN_EXTERNAL·만료 claim을 DB에서 다시 찾는다. claim의 unique scope/lease/fencing token은 같은 효과의 이중 실행을 막고, stale worker의 결과는 거부한다. 재시도 횟수 소진·permanent failure에는 담당과 다음 확인/대조 행동이 남는다.

인과 사건 revision 제어, 업무 정의 version, delivery attempt 번호는 서로 다르다. 큐의 재시도나 도착 순서가 이 세 의미를 대신하지 않는다. CAP Java queue의 system user context를 원 사용자 위임으로 취급하지 않는다. Java에서 제공하지 않는 Node callback에 복구를 의존하지 않는다. [CAP Java Event Queues](https://cap.cloud.sap/docs/java/event-queues)

개발/CI 복구 profile은 scheduler tick1초, claim TTL5초, heartbeat1초, 테스트 관찰 제한30초를 기본으로 사용하되 가상 시계와 barrier로 race를 통제한다. 최종 재시도 횟수·backoff·외부 timeout은 versioned config로 고정한다. 이 수치는 운영 SLA 약속이 아니다. 실제 운영 부하에서 값과 복구 목표를 측정해 deployment profile로 확정한다.

운영 조회는 책임 없는 활성 업무, overdue wait, pending evidence conflict, unmatched intake, suspended allocation, stale claim, exhausted outbox, unknown external result, 불일치 원장/projection과 owner/nextAction을 반환한다. alert key는 문제 ID/상태 version이며 반복 알림을 묶는다. 알림 전달 완료는 의무 완료가 아니다. 정상 관측에는 무조건 새 업무를 만들지 않는다.

예약/업무 scope마다 `nextValidityBoundary`를 기록해 LOT 기한·처분 허용·grant·정책 유효기간의 다음 경계를 찾는다. 별도 이벤트나 사용자 요청이 없어도 due sweeper가 같은 scope lock 안에서 현재 적격/권한·실행 배분을 재평가하고 필요한 SUSPENDED 전이·부족/재인가 의무·owner/nextCheck를 원자적으로 upsert한다. 실제 시점의 모든 실행도 현재 조건을 재검증하므로 sweeper 지연이 만료 후 출고를 허용하지 않는다. 예정된 정책 변경·definition 전환은 이 index의 영향 scope를 갱신한다. 큐가 비어 있는 상태에서 T까지 허용된 예약20을 T 이후로 진행시켰을 때 test 복구 제한 내 정지/의무 생성, 출고0을 인수한다.

복구 runbook은 장애 발견→대상 범위/최근 확정 명령 확인→외부 결과 대조→claim 회수/안전 재시도→의무/수량 대조→담당 확인 순서다. 관리자 직접 SQL로 상태를 맞추는 것을 정상 복구 경로로 두지 않는다. emergency repair도 dry-run diff·권한·근거·감사·재검증을 남긴다.

백업에는 DB, 원문 evidence blob, 정의/capability/evaluator 호환 artifact, skill package, 비밀을 제외한 config/배포 manifest를 포함한다. 복원 rehearsal은 새 isolated 환경에서 수량/계보·현재 의무·근거 hash·정의v1 판정·인증 경계를 확인한다. 복구된 DB에 필요한 원문이나 evaluator가 없으면 복구 완료가 아니다. 실제 비밀은 별도 secret manager 복구 절차를 사용하며 repository나 test evidence에 저장하지 않는다.

## 11. 구현 Step와 통합 gate

Task는 §1 전체이며 아래 Step는 순차 milestone다. Step 안의 독립 Subtask만 병렬화한다. 담당은 논리 소유 역할이며 실제 agent/개발자 배정은 착수 시 기록한다. 동시 code writer는 같은 Step baseline에서 별도 branch/worktree를 사용하고 coordinator만 Task branch에 통합한다. 모든 Subtask 산출물 통합·충돌 해결·해당 checks 통과 전 다음 Step로 넘어가지 않는다.

| Step | 입력·Subtask 소유 / 산출물 | 통합 종료 조건 |
|---|---|---|
| S0 착수·기술 기준선 | coordinator: issue/Phase·fork/archive inventory; platform: §2 호환 spike·lock/manifest; security: identity/endpoint 계약; protocol: 최신 SDK/client 조사와 최소 wire 왕복 | 추적 issue가 있고 지원 조합/대안 판정·새 경로 layout·schema 소유·auth/mcp 계약이 고정됨. BTP 계정 미확보는 이후 운영 gate로 명시하며 지원 불명 후보를 확정 stack으로 쓰지 않음 |
| S1 core·정의·읽기 | definitions: §3/8 schema·발행 fixture; inventory: item/LOT/segment/계보·단위; evidence: 원문/사건/정정 모델; identity: 조직/grant/read 인가 | 빈 DB Flyway·CQN/read·조직 FK/접근·type/cardinality·두 진입점 fixture·D02–D08 기본 tests·V1 schema compatibility 통과 |
| S2 업무·거래 실행 | work: §5 목표/판정/의무/인계; application: §6/7 envelope·승인·idem·audit; runtime: outbox/claim/reconciler | 구현된 core 명령에 한해 C2 evaluator·C3/C5·V1/V4/V5/V6/V7 기반 사례 통과. 미구현 수령/출고/MCP 경로는 NOT_RUN이며 전체 gate PASS로 세지 않음 |
| S3 구매·운송·수입·수령 | trade: PO/commitment/shipment/regulatory; inventory/QC: receipt/provisional/hold/move/count; integration: 수신 원천 대조 | 구매100→60+40, 중복 증거, 초과/부족·부분 기관처리/QC·겹친 제한·취소 잔여 의무. D13–D16·C1/C5 관련 tests와 실제 수령60의 V6 통과 |
| S4 판매·반품·회수·정산 | trade: sale/delivery/return/recall/settlement; inventory: reservation replacement/dispatch/return ledger; governance:ADMIN recall/closure | C1/C4·V2/V3, 부분 인도/거부·반품/정정·추적 후보·회수 대조·송장 차이, 복합 E1 시나리오 통과 |
| S5 채널·구축 통합 | protocol: 전체 action/query MCP; agents:6 skills+manifest/links; definitions: 실제발행/전환 UX; integration: source adapter profiles | 최신 protocol contract/wire tests, 대상 client 발견·로딩·tool 실행, FDE v1→v2·미지원 버전·MRTR 변경/retry. 결정적 의미 fixture 통과 후 비용 승인된 실제 모델 평가 |
| S6 운영·배포·전환 인수 | platform: BTP auth/binding/TLS/restore/upgrade; operations: retention/관측/runbook; coordinator: replacement PR/inventory·evidence index | V8 새 ontology schema upgrade, fail-closed auth, backup restore·cutover rehearsal, 필수 D/C/V/E traceability 전부 PASS. 선택 UI/구형client/존재하지 않는 legacy live자료 경로만 근거 있는 비대상 가능; 미실행 모델/BTP/필수 case를 비대상이나 waiver로 숨기지 않음 |

V gate의 최초 전체 인수는 V1/V5/V7=S5까지 전체 command 재확인, V2/V3=S4, V6=수령S3 후 모든effect S5, V4=모든 MCP/내부경로가 존재하는 S5, V8=S6이다. C3 실모델 인수는 S5다. 앞선 core/seed/stub 통과는 부분 계약 증거로만 기록하며 이후 도메인/채널이 추가될 때 관련 gate 범위를 확장한다.

S0–S2의 contract stub을 후속 구현이 공유한다. 인증·인가·승인·보존 경계는 마지막 배포 때 처음 넣지 않는다. S5 실모델 또는 S6 유료 배포의 승인/계정이 없으면 해당 실행은 대기지만 다른 결정적 검증은 계속할 수 있다. Step gate의 필수 인수를 생략해 다음 Step가 끝났다고 보고하지 않는다.

### 구축·결정 register

| 결정 ID / 소유자 | 입력·기본 경로 | 언제 무엇으로 닫는가 / 실패 시 경로 |
|---|---|---|
| R1 stack / platform | Java21 CAP 우선·PostgreSQL·Maven·Flyway | S0 exact lock/manifest와 §2 oracle. CAP 불충족이면 명시 대안 spike |
| R2 추적 issue / coordinator+사용자 | 원본/fork 목표 대응표 §12, 원본 board 규칙 | 실제 개발 착수 전 issue/Phase/board 경계. 문서 계획은 계속하되 임의 이슈 생성/원본 scope 변경 안 함 |
| R3 실제 자료 / data owner | 기본 fresh isolated DB, code/history archive | S0 authoritative DB 존재 inventory. 있으면 S6 매핑·수량/의무 대조·복구 승인 전 cutover 금지 |
| R4 초기 품목/정책 / FDE+업무 권한자 | 개발 fixture는 가상값, 실제품목·관할·적용일·공식 근거·확인자 | S3 운영 scope의 정책 package; 모르는 규제/처분은 허용하지 않고 미확인. 공식 근거 검토 전 법규 적합 인수 아님 |
| R5 역할·책임 / 조직 관리자 | §7 기본 capability, 명시 intake owner/supervisor | S2 local fixture, S6 실제 identity/grant/승인자 mapping. 누락 scope 활성화 금지 |
| R6 기간·오차·대체·retention / FDE+권한자 | 오차0·명시대체만·보존 삭제 비활성 | S6 source/effectiveDate/approval 가진 policy. 근거 없으면 자동 완화·삭제 안 함 |
| R7 BTP / 계정 owner+platform | Cloud Foundry·IAS 검토·PG binding | region/entitlement/cost cap·manifest·실배포 logs로 S6 종료. 없으면 운영 배포 미인수 |
| R8 모델 평가 / 사용자+QA | §13 고정 corpus·반복횟수·수용 제안 | S5 비용 승인·모델/client/prompt manifest·결과. 필수 오류0 미달이면 수정 후 회귀 |
| R9 보완 제안 / 사용자 | D02/D05/D14 상세안은 별도 제안으로 유지 | 관련 확장 issue 전에 영향 범위만 결정. §1 baseline 동작을 생략하거나 제안 전체를 자동채택하지 않음 |

각 R은 `status, owner, inputRefs, proposedValue, confirmedValue, decidedAt, blockedStep, evidencePath`를 deployment manifest/decision log에 기록한다. 계획 완료에 실제 계정값을 요구하지 않지만 구현·운영 gate에는 구체 확정값과 증거가 필요하다. 값이 미정인 이유·안전한 상태·결정 주체·차단 단계가 없는 “나중에 정함”은 허용하지 않는다.

## 12. fork 대체·자료 보존·목표 추적

현재 확인한 로컬 baseline은 branch `feat/57-scenario-tests`, commit `2ea59977308d8dd26267550f4035903d46d05f75`이며 origin은 원본 `mulino-coreano/mulino-coreano-erp`다. 이 계획 파일들은 아직 untracked다. 실행 시 fork remote/default/main/integration commit과 작업 트리를 다시 확인하고 별도 Task branch의 PR로 대체한다. main 직접 commit/push·force push는 사용하지 않는다. 원본 조직 저장소를 fork 변경의 대상으로 착각하지 않는다.

이전 조회의 fork default `integration/merged-prs`=`187e99d91ca3bbbf35197f5bd6a8a7489c797e50`, main=`ae02ff63751714510db4d93a39498b8d51b3697c`, 별도 integration=`5a1b12ac0d2821dae9d87924a3e24ff2bfa783ca`는 보존 후보 위치를 찾는 기록이며 현재 remote HEAD의 증거로 재사용하지 않는다.

### 파일·데이터 inventory와 cutover

S0에서 파일별 `currentHash, classification, replacementPath, archiveRef, owner, validation`을 만든다. `AGENTS.md/CLAUDE.md`, README·startup 문서, `.github` templates/CI, backend·database·mcp-server·agents/CLI·skills와 links, 기존 테스트·docs를 모두 포함한다. 분류는 유지·새 구현으로 대체·역사 archive·제거다. 실제 제거 대상은 PR diff로 검토하고 보존 commit/tag 또는 복원 검증한 Git bundle에서 회수할 수 있어야 한다. 새 main에 옛 schema·실행 명령·완료 주장이 현재 사실인 것처럼 남지 않게 한다. 기존 제조 테스트 성공은 새 수입 시스템 인수로 세지 않는다.

자료 inventory는 실제 authoritative DB/원문 저장소/외부 효과/미해결 업무가 있는지 별도로 확인한다. 없으면 새 독립 DB·ontology fixture에서 시작하며 과거 제조 schema를 live migration할 의무를 만들지 않는다. 있으면 원본 snapshot·읽기 전용 archive·LOT/수량/증거/의무 mapping과 미확인 격리 목록을 작성한다. 제조 실적을 수입 실적으로 이름만 바꿔 옮기지 않는다. 매핑할 수 없는 자료는 원본 조회를 유지하고 누락 수량/책임 없이 대조한 뒤 권한자가 전환 범위를 확인한다.

V8의 필수 schema upgrade는 **새 온톨로지 schema v1→v2**에 진행 중 업무·정의v1·증거·멱등 record가 존재하는 경우다. 기존 ERP의 변환 여부와 별개다. Flyway가 유일한 DDL 실행 주체이며 CAP 자동 deployer와 경쟁하지 않는다. 고정 CSN/compiler에서 기대 schema를 비교하고 custom constraint의 허용 차이를 versioned manifest로 검사한다. 빈 설치와 이전 schema upgrade에서 FK·수량 제약·outbox·CQN을 함께 검증한다.

cutover는 write freeze→최종 snapshot/hash→자료/의무 대조→새 version 적용→필수 smoke/인가/재개→쓰기 개방 순서다. 개방 전 실패하면 검증된 snapshot과 호환 artifact로 복구한다. 개방 후 새 쓰기나 외부 효과가 있다면 old commit 복구만으로 되돌리지 않는다. 새 요청 접수 중지·outbox 상태 고정·외부 결과 대조 후 forward repair 또는 승인된 snapshot 복원+명령/외부효과 재대조를 수행한다. 어떤 효과를 재생/보상할 수 있는지 runbook에 명시하고 실물/계약을 SQL rollback으로 취소했다고 보고하지 않는다.

### 기존 이슈와 새 Task의 관계

2026-10-07 REST 조회에서 열린 milestone은 Phase4·5·6·8이고 아래 관련 이슈가 열려 있었다. GraphQL issue list는401, project item-list는 owner 조회 오류여서 board item의 존재/상태는 검증하지 못했다. issue의 `in progress` label을 통합·실행 성공의 증거로 사용하지 않는다. 아래는 관심사 대응이며 새 fork 구현이 이미 추적된다는 뜻은 아니다.

| 새 계획 관심사 | 기존 이슈 참고 | 새 착수에서 확인할 차이 |
|---|---|---|
| 신원·원격 MCP | #21 Auth0, #22 OAuth, #47 human/idem, #52 stdio | IAS 후보·최신 stateless protocol·새 조직/위임 계약. 기존 scope로 덮어쓰지 않음 |
| 업무·증거·복구·승인 | #43 책임, #44 증거, #45 결정, #49 lease, #50 구매, #51 후속, #33 governance | 새 Work/Goal/Obligation 구조와 승인/복구 기준 |
| QC·회수·Agent | #26 QC, #27 LOT 회수, #24 runtime, #53 CLI | 완제품 수입·Agent Skills·CLI 비필수와 기존 제조 전제 차이 |
| 인수·대화·포트폴리오 | #25/#34 실환경, #35 연속성, #36 MONITOR, #57 test, #67 발표 | D/C/V별 새 증거, 별도 runtime/규제 인수, 사용자 채널 대화 유지 |

새 Task의 구현 이슈와 Phase/보드 경계는 R2로 확정한다. 현재는 사용자가 요청한 계획 검토·문서 보완이며 원격 issue/board/PR를 생성하거나 상태를 바꾸지 않는다. 기존 프로젝트의 다음 Phase로 임의 착수하지 않는다.

## 13. 요구사항별 인수 추적과 증거

### 13.1 D01–D26 추적표

아래 T는 `verification/cases/Txx`에 구현할 test case ID다. 문서상 계약을 식별하며 현재 test 파일이 존재한다는 주장이 아니다. 각 test는 seed/입력·실행 단계·정상 oracle·오류 oracle·DB/응답/의무 관찰을 가진다. 담당은 §2 모듈 소유자이고 QA가 통합 결과를 확인한다.

| 요구 | 구현 계약/담당 | Step / 필수 사례와 기대 결과 |
|---|---|---|
| D01 | §1/3 조회, coordinator | S1–S6 / T01 다섯 역량 질문에 동일 ID·시점·근거·책임으로 답하고 제외 기능 효과 없음 |
| D02 | §3/4 품목, inventory | S1 / T02 다른 issuer의 같은 SKU 분리, 코드 기간 충돌 대조,12EA↔묶음 환산은 재고/주문 이행 생성 안 함 |
| D03 | §4 계보, inventory | S1/S4 / T03 여러 LOT 팔레트·분할/합침·순환/부모 재소비 거부·식별 불가능 혼합 범위 보류 |
| D04 | §3/4 decimal/원장, inventory | S1/S3 / T04 EA0.5 거부·파손 보류10은 보유 불변·폐기10만 감소·정밀도 초과 거부 |
| D05 | §4/6 관계/처분, inventory | S3/S4 / T05/C1 위탁40/고객보관60→보유100/판매40, 예정 목적지≠현재 위치 |
| D06 | §3/4 사건 시간, evidence | S1/S4 / T06 당시 knownAt과 현재 정정 조회 차이, 미확인/상충/미입력/해당없음 구분 |
| D07 | §3/4 타입/문서, definitions+evidence | S1 / T07 잘못된 관계 끝점/cardinality 거부·문서 두 장 동일 수령60은60 |
| D08 | §7 신원, identity | S1/S2 / T08/C3 다른 조직 direct ID/search/FK·조회 전용 grant 쓰기거부·grant발급/철회·권한상향차단 |
| D09 | §5 GoalVersion, work | S2 / T09/C2 누적100·현재40, 단계별 필수 LOT, 기간 유지 관측 공백은 미확인 |
| D10 | §5 의무/인계, work | S2 / T10 인계 수락 전 owner 유지·업무간 의무이전 실패/부분이전 보존·긴급 재배정 감사·C5 접수 책임 |
| D11 | §5 전이, work | S2 / T11 자식 종료/부모90≠100·부족10이전 후에도FULFILLED거부·의존 순환 거부·초안취소·대기timeout≠성공 |
| D12 | §5 판정, work+evidence | S2/S4 / T12 출처/범위 부족 미확인·기한변경 후 위반 유지·정정 판정 이력·C4 |
| D13 | §6 구매, trade | S3 / T13 승인100→120 변경 원승인 거부·전달/수락/도착 분리·60+40/초과5 대조 |
| D14 | §6 운송/9 inbox, trade | S3 / T14 다대다 선적·구간 인계·출하100/도착98/운송중2≠손실·부모 없는 이상 C5 |
| D15 | §6 수입, trade+governance | S3 / T15 기관허용30/QC100→판매 최대30·부분/보완/반려/재제출 version·로컬작성≠제출 |
| D16 | §4/6 수령/QC, inventory | S3 / T16 임시접수 적격0·독립 제한 각각 해제·이동/실사/조정 원장·무이벤트 만료20예약 정지/의무 생성 |
| D17 | §6 판매, trade+inventory | S4 / T17 출고CONSUMED뒤 정상인도·출고후recall/실제20인도와운송10·허위보고효과0·대체예약부활금지·V2/V3 |
| D18 | §6 반품/회수, trade | S4 / T18/C4 인도60/반품20 별도·중복 반품 거부·회수25/폐기25≠50·ADMIN 종료/미확인 책임·양방향 trace |
| D19 | §6 정산, trade | S4 / T19 PO/수령/송장 차이·EUR/환산 snapshot·세금문서 구분·물류 완료/정산 미완료·은행효과0 |
| D20 | §3/9 intent/MCP, adapters+agents | S2/S5 / T20 두 진입점 동일 snapshot·날짜/장소 오류·모호함 MRTR·조회→쓰기 오해 평가·C3 |
| D21 | §8 FDE, definitions | S1/S5 / T21 v1/v2·필수slot/단위/끝점 변경 영향·불변발행·미지원 handler 보류·명시전환·V1 |
| D22 | §4/9 source, evidence+adapters | S3/S5 / T22 같은source키다른hash충돌·다른출처동일사건 중복배제·대조명령/권한·UNKNOWN_EXTERNAL결과확정과재발행금지 |
| D23 | §2/12 schema/cutover, platform | S0/S6 / T23 archive복원·fresh/live자료 분기·새schema v1→v2·quantity/의무 대조·CDS drift·V8 |
| D24 | §7 감사/보존, governance | S1/S6 / T24 검색/blob/batch/worker/관리명령인가·감사실패rollback·legalhold삭제거부·비밀redaction·복원후삭제정책 |
| D25 | 본절 traceability, QA | 모든 Step / T25 D26개/C5개/V8개 evidence누락 검사·결정적test/모델/규제/BTP 분리 |
| D26 | §10 복구, runtime | S2/S6 / T26 due wait/claim/orphan intake/outbox소진·만료boundary·safe retry 인가·C5/V5·DB+blob+definition restore |

### 13.2 C1–C5, V1–V8 고정 fixture

C1: 같은 품목·QC/규제/고객 조건을 만족하는 구별된 위탁판매40와 고객보관60, 예약0을 seed한다. 보유100·판매 적격40·미예약40이며 QC 해제/앱 권한으로60을 더할 수 없다. 위탁 허용 철회 후 기존 예약 책임은 남고 신규 예약·출고는0이다.

C2: 월60 도착→화60 출고→수40 도착을 seed한다. 수요일까지 누적 도착100 충족, 수요일 지정시점 재고100은40으로 미충족이다. 두 목표의 quantityMode·평가시점·증거 scope를 검증한다.

C3: 일반 역할 WRITE와 현재 grant READ만 준다. 모든 공개/worker 쓰기 경로는 업무·재고·후속효과0이며 조회 audit는 허용한다. 별도 실모델 corpus의 조회 문장은 쓰기 tool 실행으로 변환되지 않아야 한다.

C4: 인도100→실제 반품20은 과거100과 새20이다. 별도 fixture의 실제98 정정은 과거 판정을 남기고 현재 유효한 의무2를 생성한다. 이미 권한 있게 해소된 의무2는 부활하지 않는다. 반품 이중 접수는 새 물량을 만들지 않는다.

C5: 부모 업무가 종료된 재고에 새 온도 이상, 정책상 대응 필요, 업무 연결 실패를 주입한다. intake owner/nextAction/nextCheck가 유지되고 재시도 뒤 같은 의무/업무 하나가 연결된다. 정상 관측은 업무0이며 알림 성공만으로 접수 추적을 종료하지 않는다.

| Gate | 실행과 관찰 oracle |
|---|---|
| V1 | v1 도착 목표 대기→v2 판매 적격 의미/evaluator 배포. v1 판정 의미 유지·현재 제한 적용·미지원이면 효과0/owner 있는 보류. skill hash로 통과 대체 금지 |
| V2 | A60 예약40, 분할과 신규 예약20 경합. 실물60·기존 의무40·신규 실행배분 최대20·부모 재소비0. 실제50 정정 뒤 실행 배분≤50·부족 의무 보존 |
| V3 | 출고 적격 조회 barrier→QC 보류 commit→출고 재개는 효과0. 반대순서는 출고이력+후속책임. 늦은 옛해제는 새보류를 덮지 못함 |
| V4 | READ grant로 direct/nested/batch/projection/MCP/worker 대체경로 호출. 재고/배분/승인/outbox효과0, 원자적 batch 전체 rollback. 내부move로고객출고우회효과0·실제무허가이동은관측대조/책임으로기록 |
| V5 | queue성공 후 WAITING만 남기고 전체재시작, due시간 전진. test profile 제한 내 한 의무 재개/escalation;2worker/반복장애에도 중복효과0 |
| V6 | 수령60 commit/응답유실→token갱신/newRPC/동시retry 효과1. 같은key40 conflict·타주체 결과누출0·별개 주문은 별개key. 입력수집 단계 destination 보완은 effect conflict 아님 |
| V7 | enqueue후 grant철회·인가검사직후철회·재시작. 직렬화 선후대로 미허용 새효과0·기확정효과보존·차단의무owner/다음행동 유지 |
| V8 | exact manifest로 빈설치·ontology schema v1→v2·진행업무/정의v1·DB제약/잠금·auth/binding/TLS·MCPwire·backup복원. local/BTP/client별 결과를 따로 기록 |

### 13.3 종단 fixture와 평가 수용

E1: 구매100을 승인하고60+40을 W에서 실제 수령한다. 같은 수령60을 운송/창고 증빙 두 개로 보고해도 총100이다. 구별된60은 QC통과,40은 QC보류, 그60 중30만 기관 허용이다. 다른 조건은 전부 허용으로 고정한다. 해당30을 판매·출고·인도하고10을 반품 받아 보류한다. 기대 결과는 구매 누적100, W 현재보유80(100−30+10), 과거 인도30, 반품10, 현재 판매 적격0(남은30 기관미확인+QC보류40+반품보류10), 은행이체0이다. 송장 차이5를 추가하면 물류 목표와 별개로 QC/반품/정산 각각의 미해결 의무와 owner가 조회돼야 한다.

E2: 동일 LOT의 물량60에서 QC보류20와 회수 조사/보류60을 겹친다. QC20 해제만으로 출고60은 허용되지 않는다. 조사 후보를 오염 확정으로 표시하지 않는다. ADMIN 회수 scope50에서 실제25 회수·그25 폐기 후 남은25 미확인이라면 처리량50으로 종료할 수 없다. 승인된 예외 처리 시에도 잔여 책임/근거를 표시한다.

결정적 인수는 T01–T26·C1–C5·V1–V8·E1/E2 모두 실행하고, 해당 fixture에서 무권한 쓰기·중복효과·수량 이중소비·허위 완료0건을 요구한다. 실패한 결과를 문서 수정이나 fixture 축소로 감추지 않는다. SQL constraint만 보지 않고 API response·원장·판정·의무·감사·outbox를 함께 관찰한다.

실모델 corpus는 기본 정상/동의표현20, 모호성/대상충돌10, 조회/쓰기 경계10, 버전/권한/문서지시10, 업무예외/책임10의60건을 한국어 중심으로 고정하고 최소10건의 이탈리아어/영어 원문 또는 혼합 표현을 포함한다. 각3회 반복해 client/model/prompt/skill/definition version을 기록한다. 제안 수용값은 명확한 요청의 올바른 구조화≥95%, 모호한 입력의 부당 실행0, 무권한·중복·허위완료0이다. p50/p95 지연·token·총비용·확인질문 비율은 별도 보고하고 실제 업무 수용치/비용 상한은 R8에서 실행 전 확정한다. 이 수치는 사용자 사업 SLA로 이미 합의됐다는 뜻이 아니다.

### 13.4 증거 형식과 실행 entrypoint

새 구현은 `verification/manifest.json`에 각 requirement/case의 code commit, schema/definition/evaluator/skill version, tool/client/model/DB/buildpack version, fixture hash, 정확한 실행 command, timestamp, expected/observed, artifact refs, PASS/FAIL/NOT_RUN을 기록한다. `NOT_RUN`·논리검토·문서존재를 runtime PASS로 바꾸지 않는다. source 법규 검토는 출처/적용일/검토자, 모델 평가는 비용 승인을 별도 기록한다.

구현 S0에서 새 저장소에 `./verify schema`, `./verify contracts`, `./verify scenarios`, `./verify recovery`, `./verify mcp`, `./verify skills`, `./verify model --manifest ...`, `./verify deployment --manifest ...`, `./verify coverage`의 wrapper entrypoint를 제공한다. 이는 새 구현의 납품 계약이며 지금 실행 가능한 명령이 아니다. wrapper는 내부 Maven/JUnit/Testcontainers/protocol/client runner의 실제 command/version과 exit code를 노출한다. schema→contracts→scenarios/recovery→mcp/skills→승인된model/deployment 순서로 dependency를 지키며 중복 테스트를 의례적으로 반복하지 않는다.

coverage verifier는 D26개·C5개·V8개·E2개 각각이 실제 assertion과 artifact에 연결되는지 확인한다. 숫자나 ID가 존재하는지만 검사해서 완전성을 주장하지 않는다. QA는 case 내용이 본절 oracle와 같은지 review하고 새로운 반례가 있으면 관련 case와 계약을 함께 고친다.

## 14. 계획 검토와 완료 판정

실제 GPT-6.1 Sol high 세 검토자의 반복 검토와 최종 교정 확인에서 D01–D26의 구현 계획 완결성을 통과했다. 라운드별 기준선·반례·교차 판정은 [완결성 검토 보고서](reviews/2026-10-07-ontology-implementation-completeness-sol-high.md)에 기록한다. 이 판정은 아래 계획 기준에 한정하며 구현·실환경 인수는 미실행이다.

첫 [Sol high 스택 검토](reviews/2026-10-07-ontology-stack-sol-high-debate.md)는 V1–V8을 도출했으나 D 전체의 구현 계획을 완성한 것은 아니었다. 이번 반복 검토는 업무 범위·명령/상태·전달/운영의 세 분야를 실제 GPT-6.1 Sol high로 나눠 수행한다. 수정 뒤에는 다른 분야의 주장을 교차 검토하고 현재 문서의 각 요구에 증거가 있는지 재검증한다.

계획 완료의 조건은 D 전체 추적, command/data/state/authority/transaction/recovery 계약, C/V/E oracle, 순차 Step gate, 제안/채택 경계, unresolved 결정 register, fork 보존/cutover, 실행 인수와 문서 검토의 구분이 모두 있는 것이다. 구현자가 임의의 업무 의미를 채워야 하는 blocking gap이 하나라도 남으면 계획 완료로 표시하지 않는다. 실제 애플리케이션을 만들고 tests를 실행하는 것은 다음 구현 Task다.

## 15. 기술 조사 근거

CAP Java5/Java21·Spring Boot4 지원과 PostgreSQL 관련 제약은 공식 자료를 기준으로 검증한다. PostgreSQL multitenancy/extensibility 제한을 FDE 정의 데이터 확장과 혼동하지 않는다. 문서의 테스트 DB 버전은 다른 major의 성공/실패를 대신하지 않는다. HANA Cloud는 SAP native 확장이 필요할 때 재평가할 후보이며 이번 계획의 필수 DB는 아니다.

- [CAP June2026](https://cap.cloud.sap/docs/releases/2026/jun26): 지원 계열과 권장 버전을 구별한다.
- [CAP Java PostgreSQL](https://cap.cloud.sap/docs/java/cqn-services/persistence-services#postgresql), [BTP PostgreSQL 구성](https://cap.cloud.sap/docs/guides/databases/postgres#cap-java-on-sap-btp): PG service의 `java_buildpack`·CFEnv datasource 설정과 Java21 runtime을 실제 manifest로 검증한다. 다른 JDK/buildpack 예제를 혼합하지 않는다.
- [Schema evolution](https://cap.cloud.sap/docs/guides/databases/postgres#schema-evolution): Flyway 소유권·custom constraint·고정 compiler schema 비교의 근거다.
- [CAP Java Security](https://cap.cloud.sap/docs/java/security#auto-configuration), [CAP 인증](https://cap.cloud.sap/docs/guides/security/authentication): IAS 후보와 실제 dependency/binding/custom endpoint 보호를 함께 검증한다. 로그인 성공만으로 업무 인가 인수가 아니다.
- [CAP Java queue](https://cap.cloud.sap/docs/java/event-queues): system user context·Java 지원 범위를 기준으로 검증한다.

이 문서는 실제 공식 규제값을 확정하거나 법규 인수를 선언하지 않는다. 제품·관할·적용일별 공식 근거는 R4/R6의 별도 구축 검증 대상이다.
