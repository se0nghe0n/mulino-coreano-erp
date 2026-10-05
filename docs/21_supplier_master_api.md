# Supplier master API (#41)

공급업체 변경은 발주·입고 이력의 신원을 보존해야 한다. 이 API는 기존
`suppliers`의 ID를 유지하고 삭제 대신 `is_active=false`를 기록한다.
비활성 공급업체도 인간 조회에서 반환하며 새 계획 후보에서는 제외한다.

## 신원과 권한

`local` 프로필에서 host 전용 `X-Mulino-Local-Human` key와
`X-Mulino-Local-Role`을 사용한다. key는 agent 환경에 전달하지 않는다.
모든 조회는 인증된 active Human의 `erp:read` 권한이 필요하다.
생성·수정·비활성화는 active OPERATOR 또는 MANAGER의 `work:write`가
필요하며 transaction 안에서 DB에 저장된 실제 역할을 다시 검사한다.
재전송도 이 검사를 통과해야 한다. VIEWER/QC/ADMIN, Service, Agent의
master 변경은 허용하지 않는다. 혼합 Human/Service/Bearer 인증은 거부한다.
`!local`에서는 이 모듈의 모든 경로를 차단한다. 기존 익명 Case intake
경계는 유지한다. 별도의 PENDING 승인안은 만들지 않는다.

## 경로

| Method | 경로 | 동작 |
|---|---|---|
| POST | `/api/v1/suppliers` | 생성, 201 |
| GET | `/api/v1/suppliers/{id}` | 비활성 이력을 포함한 단건 조회 |
| GET | `/api/v1/suppliers?page=0&size=20` | ID 순 목록, size 1~100 |
| PUT | `/api/v1/suppliers/{id}` | 전체 입력 교체, version 검사 |
| DELETE | `/api/v1/suppliers/{id}?expectedVersion=0` | 비활성화, version 검사 |

생성 입력은 `name`, `country`, 선택 필드 `contactName`, `contactEmail`,
`contactPhone`, `addressLine`, `city`, `postalCode`, `paymentTerms`,
`currency`다. 이름·국가는 빈 문자열을 허용하지 않는다. 필드 길이는
스키마와 같고 email 형식을 검사한다. currency는 대문자 3자리다.
`paymentTerms`는 NET_30/NET_60/COD/PREPAID 중 하나다.
생략한 paymentTerms/currency는 NET_30/KRW다. 기존 행의 주소는 NULL이며
updated_at은 NULL로 유지한다. 새 SQL insert에는 CURRENT_TIMESTAMP default를
사용하고 실제 변경은 JPA Auditing이 기록한다. 기존 TIMESTAMP를 LocalDateTime으로
매핑하여 과거 데이터의 timezone을 추정하지 않는다.

수정 입력은 `{"supplier":{생성 입력},"expectedVersion":0}` 형태다.
응답은 공통 ApiResult의 data에 supplierId, 입력 필드, active, version,
createdAt, updatedAt을 반환한다. JPA Auditing이 생성·수정 시각을 기록한다.
preferred currency는 계획·발주 KRW 금액을 환산하지 않는다.

## 멱등성·감사·동시성

모든 변경에는 `Idempotency-Key`가 필요하다. 기존 request receipt에
인간·operation별 canonical input hash와 응답을 저장한다. 같은 key/input은
동일 응답을 반환하고 행·감사를 추가하지 않는다. target/version을 포함한
다른 입력으로 key를 재사용하면 409다. 정확한 과거 재전송은 자기 변경으로
증가한 version과 무관하게 원래 응답을 반환한다.

READ_COMMITTED transaction에서 global planning guard, stored Human,
request receipt, Supplier 순으로 조율한다. 수정·비활성화는 Supplier row를
잠그고 expectedVersion으로 stale 명령을 거부한다. 같은 key의 동시 생성은
첫 commit의 receipt를 읽는다. 공급업체 변경과 계획·발주·QC·리콜의
source snapshot 변경은 같은 global guard로 직렬화한다.

불변 `governance_audit_logs`에 SUPPLIER_CREATED/UPDATED/DEACTIVATED,
실제 actor_id와 Human 신원, before/after supplier 값을 저장한다.
master 변경·receipt·audit는 함께 commit하거나 모두 rollback한다.

| 코드 | HTTP | 의미 |
|---|---|---|
| SUP001 | 404 | 공급업체가 없음 |
| SUP002 | 409 | expectedVersion 불일치 |

## 검증 경계

Supplier 통합 테스트는 인간 CRUD·정확한 replay·잘못된 권한·입력·혼합
credentials·비활성 인간·동시 생성·감사 실패 rollback·계획 제외·이력 보존을
검증한다. 실제 built JAR REST/Swagger와 DDL/Flyway 구조 비교는 별도
검증한다. scripted SIT는 실제 모델 UAT의 증거가 아니다.

자동 인증서 통지와 모든 필수 인증 종류에 대한 QC 확대는 #41 범위에
포함하지 않는다. 현재 품질 계약은 [품질 흐름](18_inbound_quality_api.md)을
따른다. 기존 gap은 별도 등록·승인이 필요하다.
