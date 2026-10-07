# S1 inventory 계약

같은 SKU·LOT 표기가 서로 다른 실물을 합치거나 retired 부모와 자식을
함께 합산하지 않도록 stable UUID와 조직 복합 FK를 둔다.

- baseline은 `164414418b6c3ce2959979939388d59cf2068e2c`다.
- namespace는 `mulino.inventory`이며 Flyway V5가 DDL을 소유한다.
- Product는 품목 개념이며 TradeItem은 주문/기준 단위 식별자다.
- SpecificationVersion·PackagingVersion은 Product에 귀속하고 불변이다.
- ExternalIdentifier는 issuer+namespace+value의 겹치는 유효기간을 거부한다.
- ManufacturingLot은 manufacturer+TradeItem+originalLot 문맥으로 식별한다.
- QuantitySegment는 item/LOT/위치/custodian/owner/controlScope를 분리한다.
- QuantityMovement와 GenealogyEdge는 외부 범용 CRUD에 노출하지 않는다.
- `ItemRegistration`은 metadata-only 내부 초안이며 Spring handler로 등록하지
  않는다. S2 envelope/audit/idempotency 통합 전 공개 action은 NOT_RUN이다.

조회 handler는 `getObject/searchObjects/getInventory/getTrace`다. objectType은
Product/TradeItem/ManufacturingLot/QuantitySegment/LogisticsUnit/Place/Manufacturer다.
`getInventory`는 itemId와 선택 placeId/lotId scope를 받는다. decimal은 문자열과
unit으로 반환한다. heldQuantity는 asOf/knownAt 기준 active leaf만 합산한다.
getTrace는 segment 또는 ManufacturingLot에서 양방향 계보를 조회한다.
불확실 혼합은 영향 후보이며 숫자만으로 깨끗한 subset을 선택할 수 없다.

명사/업무 조회는 coordinator가 같은 DomainContext와 snapshot을 전달한다.
전체 query 거래·인가·audit/idempotency는 공통 application 경계가 담당한다.
Repository는 모든 CQN에 organizationId와 knownAt predicate를 붙인다.

S1은 QC·정책·처분/예약·수령 누적을 구현했다고 주장하지 않는다.
eligibleQuantity/reservedQuantity/unreservedEligibleQuantity/cumulativeArrival은
null과 명시 UNKNOWN 사유를 반환한다. T02의 confirmReceipt와 T03 배분이관,
T04 QC/제한, T05 처분 결정 및 V2/V3 전체 경합은 S2–S4 통합 전 NOT_RUN이다.
수량 primitive와 보호된 trade 명령은 뒤 단계에서 원자적 구현한다.

ObjectRelations는 발행된 RelationDefinition의 source/target type과 조직 내
실제 endpoint를 검사한다. 지원 endpoint는 위 inventory core 유형뿐이다.
유효기간별 maximumCount와 금지된 cycle을 DB에서 검사하며 locatedAt은
QuantitySegment.placeId와 모순될 수 없다. minimumCount의 필수 입력 stage 및
관계 생성/폐기 lifecycle은 S2 공개 command에서 구현할 범위다.

실행 검사는 `evidence/maven-test.log.gz`와 두 Surefire report에 남겼다.
`./mvnw -f backend/pom.xml -Dtest=InventoryQuantityTest,InventoryPostgresTest test`
결과는 9 tests, 0 failure, 0 error다. PostgreSQL18 Testcontainers와 CAP CQN,
Flyway V1–V5를 실제 실행했다. JwtDecoder와 ReadAuthorizer는 mock이다.
현재 identity/grant end-to-end 증거는 별도 identity 인수에 의존한다.
단독 worker 실행은 임시 `backend/srv/inventory-test-import.cds`로 identity,
definitions, inventory model을 compile했고 완료 후 파일을 제거했다.
통합 branch에서는 공통 srv import가 같은 model을 compile해야 한다.

추가 hardening은 계보 부모의 retiredAt이 자식 validFrom 이후이거나
retirementRecordedAt이 자식 recordedAt 이후인 입력을 거부한다. 그렇지 않으면
과거 snapshot에서 부모와 자식이 함께 active로 보일 수 있다.
`lineageRejectsHistoricalParentChildOverlap` regression을 추가했다.
이 추가 regression은 worker의 위 9 PASS 실행 뒤 작성했다. Maven slot을
통합 작업에 넘겼으므로 최신 source의 10-test 결과는 coordinator의 통합
실행에서 확인해야 하며 worker 기록만으로 PASS를 주장하지 않는다.
