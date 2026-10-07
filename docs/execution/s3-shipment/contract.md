# S3 운송 계약과 현재 실행 범위

계획 목적지만으로 현재 재고를 만들면 예정 물량과 실제 물량이 중복된다.
Shipment는 immutable cargo/PO 배분과 예정 route를 저장하고 실제 canonical
관측을 별도 기록한다. 원장 쓰기는 receipt의 inventory primitive가 소유한다.

기준선은 c6c910b81c8a61fbdf309da9144022482408d5dc다. 사용자 Step3/S3,
D14·T14·C5의 shipment 소유 경로만 구현한다. 추적은 R2 로컬 기록이다.

- `createShipment`: originId/destinationId/carrierRef, 선택 workId,
  cargo[{itemId,quantity,unit,allocations:[{poLineId,quantity}]}],
  legs[{originId,destinationId,carrierRef,plannedDepartureAt?,plannedArrivalAt?}].
  다대다 PO 배분은 cargo량과 같고 accepted PO 잔량을 넘지 않는다.
- `recordLegEvent`: shipmentId/cargoId/legId/kind/placeId/physicalScopeId,
  occurrenceId/quantity/unit/occurredAt. kind는 DEPARTURE, ARRIVAL,
  IN_TRANSIT, DELAY, TEMPERATURE_ANOMALY다. departure가 계획 일부라면
  lineAllocations[{poLineId,quantity}]로 실제 PO 귀속을 명시한다.
- `recordHandover`: 동일 공통 slot과 fromCustodianId/toCustodianId를 쓴다.
  canonical kind는 CUSTODY_HANDOVER다. actor와 장소는 조직 FK로 묶인다.
- 실제 증거는 SHIPMENT_<kind> canonical의 item/physical scope/place/수량/
  단위/시점과 현재 verified source에 정확히 대조한다. immutable event
  payload의 shipmentId/cargoId/legId/placeId/physicalScopeId와 custody
  actor도 일치해야 한다. 문서 존재나 문자열 true는 확인 근거가 아니다.
- actual departure의 PO 실행 기여는 한 physical cargo scope당 한 번이다.
  뒤 구간 출발과 보관 인계는 출하량을 다시 늘리지 않는다.
- 출발100·도착98·운송중2는 ACCOUNTED_IN_TRANSIT다. lossQuantity는
  UNVERIFIED를 유지하며 차이2를 분실이나 감소 원장으로 만들지 않는다.
- 연결 Work의 이상은 IntakeDutyPort로 같은 거래에 의무를 만든다.
  부모가 없으면 기존 evidence inbox의 intake owner/supervisor/nextAction/
  nextCheck를 보존하고 RuntimeIntakeService의 정책 기반 연결을 기다린다.
  알림 성공으로 접수 책임을 닫지 않는다.

`getShipment`와 Shipment `getObject`는 같은 query provider를 사용한다.
planned route와 current verified observation을 구별하고 현재 evidence
철회·상충·supersession이 있으면 실제 확인 관측에서 제외한다.

공개 repository는 requireCargo(context,cargoId), cargoRows(context,shipmentId),
cargoAllocations(context,cargoId)를 제공한다. ShipmentQueries의
verifiedObservations(context,cargoId)는 receipt가 actual physicalScopeId를
읽도록 제공하며 getShipment 권한을 다시 검사한다.

실제 supplier/carrier 전송, paid model과 BTP 실행은 NOT_RUN이다.

후속 대조는 원문과 event payload의 `shipmentEventId` UUID를 요구한다.
같은 실제 사건의 복수 보고는 같은 UUID이며 다음 관측은 새 UUID다.
V21의 semantic fingerprint는 같은 UUID의 다른 cargo/수량/시점을 상충으로
보존한다. source key와 물리 좌표만으로 같은 사건을 추정하지 않는다.

`DepartureAllocations`는 실제 최초 출하의 PO 귀속을 immutable하게 남긴다.
부분 출하를 여러 번 기록해도 각 PO line의 cargo 계획을 넘지 않고,
뒤 구간의 같은 물리 범위는 최초 귀속을 변경하거나 다시 기여하지 않는다.

## 실제 집중 검사

Java21.0.5/Node24.19.0과 고정 PostgreSQL container에서 실제 command
pipeline 25개 검사(운송10·기존 evidence15)가 모두 통과했다. failure,
error, skip은0이다. 결과와 원문·compiled hash는 `checks.json`에 있다.

초기 검사에서 UUID 외의 occurrenceIdentity, 보관 인계의 null kind,
수량 문자열의 잔여 scale을 발견했다. 실패 로그를 삭제하지 않고 수정 뒤
같은 public gateway 인수를 재실행했다. 실제 source 접수→match→link→
운송 기록이 통과하며 mock domain/policy를 사용하지 않았다.

C5의 orphan 운송 이상은 durable inbox와 recovery row의 owner,
supervisor, nextAction, nextCheck 유지까지 확인했다. 정책으로 새 대응
Work/의무를 실제 연결하는 재시도 인수는 공통 runtime 결합 범위다.
기존 연결 Work의 의무와 종료 Work의 followup은 기존 IntakeDutyPort를
사용한다. 전체 S3·Step3 완료나 실제 외부 전송의 성공을 뜻하지 않는다.
