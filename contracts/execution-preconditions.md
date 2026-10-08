# 실행 전제 계약: 시계·발생 시각·권한 사슬·근거

step2r round 6–9는 리뷰가 찾은 제품 검사 하나씩을 고쳤고, 다음 리뷰가
그 뒤의 검사를 찾았다(수령 보관자 → 운송 leaf → pick → 운송 장소·pick
시각). round 10은 적용을 기대하는 명령마다 제품의 검사 사슬 전체를
순서대로 읽고, fixture나 요청이 어길 수 있는 검사를 `./verify prepare`
규칙으로 옮겼다. 전수 점검표는
[precondition-audit](../docs/execution/step2r-round10/precondition-audit.md)에
있다. 기계가 읽는 원본은
[execution-preconditions.json](execution-preconditions.json)이고, 출고 운송
장소 규칙은 [fixture-place-kinds.json](fixture-place-kinds.json)의
`dispatchTransit`이다. backend는 읽기만 했다.

## 제품 시계

제품은 주입된 시계 하나로 모든 명령의 `asOf`·`knownAt`을 정한다
(`ExecutionClock`). 인수 runtime은 그 시계를 fixture clock의 `asOf`에서
시작하고 clock control로만 옮긴다. control은 `instant`, 없으면 `asOf`,
없으면 `knownAt` parameter로 시계를 맞춘다. 실행 중 pick의 `pickedAt`과
기록 시각은 그 action의 시계다.

## 발생 시각

action의 업무 발생 시각은 명시한 `occurredAt`(slot 또는 요청), 없으면 요청
`asOf`, 없으면 그 action의 시계다. adapter는 이 순서로 정하고 다른 값을
고르지 않는다.

- 발생 시각은 그 action의 시계보다 뒤일 수 없다(미래 실제 발생 거부).
- 출고는 pick 뒤다. 앞선 pickQuantity의 시계 또는 fixture 배분의 `pickedAt`
  이전으로 날짜를 적으면 제품은 TYPE_INVALID 'Dispatch occurrence cannot
  precede its authorized pick'를 낸다. FORBIDDEN·STALE_REVISION·
  SCOPE_INELIGIBLE·VERSION_UNSUPPORTED로 고정한 반례는 이 검사 전에 결정된다.
- 적용을 기대하는 인도는 자기가 가리키는 출고의 발생 시각 이후다
  (`observeDelivery`).

C2·T09 `cumulative-versus-state`와 T09 `exists-versus-end-throughout`은
과거 출고 시각을 그대로 둔다. fixture clock을 시나리오의 업무 시작으로
옮기고 출고·수령·조회 앞에 clock control을 두어, pick이 출고보다 앞서고
누적·상태 계산은 바뀌지 않는다. T11 `cancel-after-shipment`는 출고 시각을
단언하지 않으므로 출고를 pick 시각(fixture asOf)으로 적었다.

## 권한 사슬

`IdentityAuthorization.permittedScopes`는 role, grant action, grant scope의
모든 차원, 위임자 자신의 권한을 교집합으로 본다.

- grant scope에 `capabilityIds`가 있으면 grant action을 모두 담는다.
- 위임자(`delegatorAlias`)가 fixture actor이면 위임하는 action을 모두
  role과 grant에 가진다(계획 §7.1). round 10에서 C2·C5·T06·T09–T12·
  T22·T24·T26·V5 fixture 117개의 supervisor가 이를 어겼고 고쳤다.
  fixture actor가 아닌 위임자 alias는 fixture의 root 위임자이며, installer가
  위임한 범위를 덮는 root grant의 actor로 설치한다(Step 3 요청).
- 적용을 기대하는 COMMAND·RECORD action의 actor는 그 capability를 role,
  grant action, `capabilityIds`(있으면)에 가지고, action 시계에 grant가
  유효하다.
- 그 actor와 fixture actor인 위임자의 grant가 장소를 나열하면, 제품이
  인가하는 장소(예약·분할·이동하는 fixture segment의 장소, pick·출고·해제·
  교체하는 fixture 배분 segment의 장소, 이동 목적지, 수령 장소, 출고 운송
  장소)를 담는다.

## 근거와 창고 보관

- 출고와 재고 명령(split·merge·move·stocktake·adjust·dispose)은
  evidenceRef를 요구한다. 적용을 기대하면 요청 어딘가에 근거(비어 있지 않은
  `evidenceRefs`, `evidenceRef`·`evidenceId`·`evidenceIds`·`evidence`)가 있다.
- 예약·출고·이동하는 fixture 실물은 INTERNAL_STORAGE에 내부 보관자로 있고,
  이동은 다른 INTERNAL_STORAGE로 간다.

## pick 전 반례

pick하지 않은 배분의 출고는 'Pick before dispatch required'로 거부된다.
같은 code가 pick 뒤 검사에서도 나오므로(revision 대조, 운송 PLACE 인가,
DISPATCH·연속 권한), code만으로는 반례가 pick 검사 전에 결정됨을 보이지
못한다. pick 없는 반례는 case가 보이는 이유가 있을 때만 받는다.
FORBIDDEN은 actor에게 현재 출고 권한이 없음(actor 아님, role·grant 없음,
grant 유효기간 밖, 앞선 철회)을, VERSION_UNSUPPORTED는 다른 정의 버전을,
STALE_REVISION은 앞선 action이 소비했거나 fixture가 종결로 선언한 배분을
보인다. 배분을 지명하지 않은 출고는 batch·worker route나 다른 조직의
대상을 지명한 FORBIDDEN 거부일 때만 받는다.

## 남은 installer 규약

grant scope 어휘(work·segment·place 차원)의 설치, 결정 capability의 grant
action, capability별 현재 COMMAND 정책과 발행 정의 내용은 installer가
fixture에서 만들어야 한다. 이 계약은 그 규약을 `openInstallerConventions`로
남긴다.
