# S2 물량 명령과 보존 계약

동일 부모를 다시 소비하거나 실사 관측을 즉시 재고 조정으로 처리하면
실물량이 부풀려진다. 물량 변경은 공통 command transaction 안에서
현재 leaf를 잠그고 소비하며, 원래 물량과 위치 기록을 보존한다.

작업 baseline은 `40a240c3886d3316ba1fd686e9b44bfa1ba6ec2d`다.
사용자 Step 3·계획 S2에 해당한다. 작업자는 inventory CDS/V14,
inventory domain/application과 이 문서·전용 tests를 소유한다.
공통 envelope·인가·정책·승인·감사·outbox·멱등성은 다른 모듈이
소유한다. 실제 통합 검사 전 worker 결과만으로 S2를 종료하지 않는다.

## 입력과 primitive

공개 capability ID는 `registerItem`, `linkExternalId`, `splitQuantity`,
`mergeQuantity`, `moveQuantity`, `recordStocktake`, `adjustQuantity`,
`disposeQuantity`다. semanticVersion과 definitionVersion은 별도다.
공통 envelope의 `slots`는 typed 값이고 발생 시각은 UTC Instant다.
commit되는 handler 결과는 APPLIED다. 외부 코드 상충도 대조 기록의
저장은 APPLIED이며 effects.reconciliationState=PENDING_RECONCILIATION과
nextAction을 남긴다. 실물/품목 병합이 실행됐다는 뜻은 아니다.
stocktake만 RECORD이며 나머지는 COMMAND다.

- `splitQuantity`: segmentId·quantities[]·unit·occurredAt·evidenceRef를
  받는다. 부모 전량과 자식 합은 같아야 한다. 부모는 retired가 되고
  같은 item/LOT/controlScope의 새 자식이 생긴다.
- `mergeQuantity`: segmentIds[]·expectedRevisions·occurredAt·evidenceRef를
  받는다. 모든 source revision을 확인한다. item·제조 LOT·단위·위치·
  controlScope·custodian·owner·식별 상태와 현재 물류 소속이 호환돼야
  한다. 다른 LOT를 단일 segment로 합치지 않는다.
- `moveQuantity`: segmentId·destinationId·occurredAt·evidenceRef를
  받는다. 양쪽 Place.kind가 INTERNAL_STORAGE이고 내부 custodian이
  확인돼야 한다. 고객/외부 장소와 EXTERNAL custodian은 거부한다.
  원 위치를 덮지 않고 부모를 소비해 새 위치의 child를 만든다.
- `recordStocktake`: segmentId·observedQuantity·unit·occurredAt·evidenceRef를
  받는다. 관측량은 0도 가능하며 원장·보유량 효과는 없다.
- `adjustQuantity`: segmentId·stocktakeId·quantity·unit·direction·reason·
  occurredAt·evidenceRef를 받는다. 관측 차이와 일치하고 count를 한 번만
  적용한다. 증가도 원 leaf를 소비하고 원량 계보와 별도 증가 원장을
  남긴다. 감소는 child 잔여량과 감소 원장의 합으로 원량을 보존한다.
- `disposeQuantity`: segmentId·quantity·unit·reason·occurredAt·evidenceRef를
  받는다. 보유 감소는 승인된 폐기량만큼이다. 해결하지 않은 active
  allocation이 있으면 책임을 버리지 않고 거부한다.

조정·폐기는 현재 MANAGER 정책/승인 검사를 공통 guard에서 받는다.
활성 제한이 있으면 inventory guard가 해당 조정·폐기를 차단한다.
QC/ADMIN 처분 lifecycle과 판매 적격성 전체는 후속 S3/S4의 인수다.
CONFIRMED 처분 근거 row만으로 기존 제한을 해제하지 않는다.

## 잠금과 거래

fence 이름은 `inventory/segment/<ID>`, `inventory/control/<scope>`,
`inventory/item/<ID>`, `inventory/place/<ID>`다. 외부 코드 충돌에는
issuer/namespace/value의 SHA-256 fence를, 조정에는 stocktake fence를
추가한다. PostgreSQL key는 organizationId + ':' + fence 이름이다.
모든 key를 정렬하고 잠근 뒤 현재 leaf·인가·revision을 다시 읽는다.
다중 source는 각 source를 개별 인가한다. ID 목록의 OR 허용 하나로
다른 물량을 소비하지 않는다.

Restrictions/DispositionBases 삽입·변경 trigger도 같은 control fence를
잠근다. 빈 scope에 새 제한을 넣는 경우에도 실행과 직렬화한다.
StockPrimitives는 Spring transaction 없이 실행할 수 없다.
repository의 physical insert/update는 package 내부에만 열려 있다.
REST/OData/MCP adapter에 원장 CRUD를 제공하지 않는다.

V14는 parent 전량=계보 자식량+명시 감소량을 transaction 종료 시
검사한다. 기존 S1 DAG·LOT 일치·effective/knowledge 시점과 typed relation
제약은 유지한다. 기존 S1 fixture는 retired parent와 자식/edge를 한
transaction에서 넣고 원하는 역사 시점을 최초 INSERT에 쓴다.

기존 EXECUTABLE/SUSPENDED allocation은 split/merge의 새 child에 한 번만
이관하고 predecessor를 REPLACED로 남긴다. capacity를 넘으면 rollback이다.
이 foundation은 판매 예약·피킹·출고의 공개 인수를 대신하지 않는다.
식별 불가능 혼합은 자식·계보에 불확실성을 전파하며 숫자 계보만으로
깨끗한 subset을 선택하지 않는다.

## 검증 범위

StockCommandPostgresTest는 100→60+40, 부모 재소비, allocation 40 한 번
이관, merge 조건, decimal/타 조직 거부, 실사와 폐기 구별, rollback,
동시 소비와 restriction phantom fence, 혼합 전파, 외부 코드 충돌,
개별 source 인가와 실사 차이 재적용 거부를 실제 PG/CQN에서 확인한다.
InventoryPostgresTest의 기존 시점·관계 assertion도 유지한다.

실행 결과는 checks.json에 기록한다. 직접 javac 검사는 소스 compile
보조 증거이며 Maven/CAP compiler·실제 PostgreSQL 인수와 구별한다.
전체 V2 예약/정정·V3 출고/QC 경합·C1 판매 적격성과 S3/S4 인수는
이 primitive tests만으로 PASS라고 표시하지 않는다. paid model/BTP는
실행하지 않는다.

## 명사 subject의 실제 대상 결합

subjectRefs는 권한 scope 목록을 복사하지 않는다. 물량 명령은 validated
segmentId/segmentIds를 QuantitySegment로, 그 조직의 실제 segment가
참조하는 itemId만 TradeItem으로 결합한다. 이 경로는 T03/T04의
QuantitySegment와 E1/E2/T13의 TradeItem 진입점을 같은 실물에 연결한다.
Place·Organization과 관계없는 다른 품목은 subject로 허용하지 않는다.

registerItem은 아직 존재하지 않는 ID를 생성하므로 기존 subject는 없다.
linkExternalId는 validated itemId의 실제 TradeItem 하나만 결합한다.
필수 대상은 typed slots에 이미 있어 명사 선언 0개는 허용한다. 명사를
선언하면 공통 resolver가 발행된 noun/action, 정확한 ID와 cardinality를
잠금 전후에 확인한다. InventorySubjectBindingTest는 실제 원천과 품목,
merge의 여러 source, 생성/연결의 차이와 권한 scope ID 배제를 확인한다.
