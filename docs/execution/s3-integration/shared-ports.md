# S3 최소 공통 계약

baseline은 `c6c910b81c8a61fbdf309da9144022482408d5dc`다. 새 gateway를
추가하지 않는다. 모든 도메인 CommandHandler는 ApplicationCommands를
통과한다. prepare는 실제 DB 대상·revision·인가 scope와 정렬 가능한
fence key를 반환한다. subjectBindings는 payload scope 대신 실제 대상의
정확한 noun/ID를 결합한다. 기존 gateway가 현재 grant/policy·승인 hash·
expectedRevision을 fence 뒤와 commit 직전에 재검사한다.

업무 상태는 detail 필드이고 성공 outcome은 APPLIED다. 외부 전달 결과
미확인은 ACCEPTED_PENDING_EXTERNAL이다. 도메인 거부는 DomainError를
사용하고 효과·감사·outbox·멱등 결과는 기존 한 transaction에 속한다.
각 도메인은 자신의 QueryHandler와 model import를 소유한다.

`InventoryReadFacts`는 getInventory의 조회 확장이다. quality는
eligibleQuantity/reservedQuantity/unreservedEligibleQuantity 및
eligibilityStatus를, receipt는 cumulativeArrival를 제공한다. 한 metric의
제공자는 하나다. 공통 조회는 List로 여러 제공자를 결합하며
getIfAvailable의 단일 bean 가정을 추가하지 않는다. 전달받은 active leaf는
현재 조회 권한이 확인된 범위다. 과거 receipt·증거·관계의 조회 권한은
각 제공자가 별도 확인한다. DomainContext의 organization/asOf/knownAt을
공유하며 현재 실행 권한 검사와 역사 조회를 혼동하지 않는다.

수령 실물 효과는 receipt 소유 ReceiptStockPrimitives에서만 만든다.
구매 line의 기여·운송 실물 이동 여부는 domain port로 대조한다.
원장 신규 생성과 기존 운송 segment 이동을 같은 canonical occurrence에
동시에 적용하지 않는다. evidence CanonicalOccurrences/Verifications와
WorkContributionRead 및 AssessmentService.invalidate를 재사용한다.
확인된 수령은 현재 Work 판정의 input으로 들어가며 raw claim이나 단순
문서 attachment는 verified 사건량이 아니다.

이 문서는 API 계약이며 실행 인수 PASS가 아니다. 통합 schema parity와
실제 PostgreSQL/common gateway 결합 검사는 도메인 commit 통합 뒤 수행한다.
