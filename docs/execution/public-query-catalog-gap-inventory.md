# 공개 QUERY와 acceptance catalog의 차이를 기록한다

원본 READ 의미를 공개 QUERY capability로 대응할 때 현재 제품의 실제
조회 ID가 catalog에 있어야 한다. 기준선
`e995595a3e39ffb2170a6e02c9991b6ea38c1989`의 handler 등록과
`contracts/acceptance-capabilities.json`을 대조한다. fixture 권한의
확대나 원 oracle의 수정은 이 조사에 포함하지 않는다.

## 누락된 17개 ID

| 실제 QueryHandler | catalog에서 누락한 operations |
|---|---|
| InventoryQueries | getTrace |
| PurchaseQueries | getPurchase |
| ShipmentQueries | getShipment, searchShipments |
| ReceiptQueries | getReceipt, getReceipts |
| RegulatoryQueries | getRegulatoryProcedure |
| SalesQueries | getSalesOrder, getSalesOrders, getDelivery, getDeliveries |
| ReturnQueries | getReturn, getReturns |
| RecallQueries | getRecall, getRecalls, traceRecall |
| SettlementQueries | getSettlement |

`ApplicationQueries`는 Spring이 주입한 QueryHandler 목록의
operations를 실제 공개 조회 목록으로 등록한다. OntologyRest의
`/api/ontology/capabilities`와 OntologyMcp의 tools/list는 이 목록을
사용한다. ObjectReadProvider는 별도의 새 QUERY ID를 만들지 않고
getObject/searchObjects의 noun을 제공한다. QueryHandler이기도 한
provider의 operations는 같은 목록에 들어간다.

ShipmentQueries의 `verifiedObservations`는 요청이 getObject여도
`getShipment` 현재 인가를 별도로 검사한다. 따라서 getShipment는
실제 공개 조회이면서 내부 prerequisite다. catalog 누락을 기존 grant의
all-of 조건 삭제나 ORGANIZATION 확대를 통해 해결하지 않는다.

이미 catalog에 있는 17개 실제 QUERY는 다음과 같다.

`evaluateEligibility`, `getAccessContext`, `getAssessment`,
`getDefinition`, `getEvidence`, `getGrant`, `getInbox`, `getInventory`,
`getObject`, `getObligations`, `getPolicy`, `getReconciliation`,
`getWork`, `searchObjects`, `searchOperationalIssues`, `searchWorks`,
`traceLot`이다. 실제 등록 ID의 kind 불일치는 없다.

반대로 catalog의 `convertUnit`, `getCommandResult`, `structureIntent`는
현재 ApplicationQueries 등록 목록에 없다. 기존 계획 계약이므로 이
조사에서 삭제하거나 등록됐다고 표시하지 않는다.

## Root 승인용 최소 변경안

대상 파일은 `contracts/acceptance-capabilities.json` 하나다.
위 누락한 17개 ID 각각에 아래와 같은 entry를 추가한다.

```json
{
  "id": "getShipment",
  "kind": "QUERY",
  "implemented": false,
  "schemaVersion": "1.0.0",
  "semanticVersion": "PLAN-1.0"
}
```

기존 QUERY 형식과 planned 상태를 유지하며 schemaVersion을 올리거나
구현·runtime PASS를 주장하지 않는다. 기존 entry나 case·fixture·oracle를
수정하지 않는다. catalog 자체는 root의 최소 변경 승인 전까지
수정하지 않는다.

## 실제 등록 증거

IdentityTradeScopesPostgresTest의 등록 조사 test는 실제 Spring bean의
operations 합집합과 ApplicationQueries.operations가 같은지 검사하고
ObjectReadProvider의 noun도 관찰한다. catalog 동등성 통과를 주장하지
않으며 누락은 위와 같이 별도 기록한다. 최종 실행 artifact와 hash는
다음과 같이 남긴다.

2026-10-24 실제 Spring context의 QUERY는 34개, ObjectReadProvider는
9개였다. Source-only 추출과 실제 등록 결과가 일치했다. 실제 등록
artifact는 `.local/identity-trade-scopes/evidence/` 아래
`live-query-registration.json`이며 SHA256은
`91bfeed6e55d660fe4b416ac87672fa20f42e86641f6722f1871cba51e711cd3`다.
catalog 대조 결과는 `query-catalog-diff.json`에 남겼다.
원 catalog SHA256은
`dd4987f1ce7050dd510da42e4ab4cf5706d19413e6181bbc29924a251505c3a4`다.
제안만 담은 `proposed-query-entries.json`과
`proposed-query-catalog.patch`도 같은 디렉터리에 있다. 이 patch는
적용하지 않았다. catalog equality와 M05 재실행은 NOT_RUN이다.
