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
