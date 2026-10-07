# S3 구매 구현과 현재 검증 범위

구매 계획량이 재고가 되거나 개정된 발주가 옛 승인을 사용하는 것을
막기 위해 proposal revision과 order snapshot을 불변으로 저장한다.
공급자 응답은 발주 전달 요청과 실제 도착에서 분리한다.

- `PurchaseCommands`는 공통 ApplicationCommands transaction에서
  현재 scope·revision·policy와 MANAGER decision authority를 확인한다.
  proposalHash에는 품목·공급자·양·단위·가격·통화·기한·목적지·
  endpoint·quantityMode·revision을 포함한다.
- `PurchaseEvidence`는 실제 원문과 event payload의 orderId·itemId·
  reply·quantity·unit·proposalRevision을 대조한다. 단순 문서나
  client의 VERIFIED 선언으로 SupplierCommitment를 만들지 않는다.
- `PurchaseLinePort`는 canonical 실제 도착량을 여러 PO line에
  배분하되 동일 occurrence 총 배분량을 초과하지 못하게 잠근다.
  발주 초과분은 원 주문 이행량으로 더하지 않는다.
- `cancelPurchase`는 출하·수령·청구 각 누적량의 최대치를 보존한다.
  미실행 범위의 취소를 outbox에 요청하고 외부 수락은 UNKNOWN으로
  남긴다. 인간 owner·nextAction·nextCheck가 있는 OPEN 의무를 만든다.
- `PurchaseQueries`는 불변 revision과 응답·취소·수령 기록으로
  getPurchase와 PurchaseProposal/PurchaseOrder 명사 조회를 지원한다.

조건부/반려 결정은 불변 decision으로 보존한다. 현재 구현은 조건부
결정에서 바로 전달하지 않는다. 조건 충족을 검토한 새 MANAGER 승인을
받는 경로를 제공하며 자동 predicate 충족에 따른 전달은 아직 없다.
외부 발주 전송은 로컬 transactional outbox와 기존 runtime reconciler를
사용한다. 공급자 network를 호출하지 않았다.

## 실제 PostgreSQL 검사

`PurchaseCommandPostgresTest` 7개가 PostgreSQL18·CAP/CQN·실제 공통
command gateway에서 PASS다. 실패/오류/skip은 0이다.

수행 명령은 `checks.json`에 보존했다. Java21·고정 Node24와
`npm ci`를 사용했다. 최초 실행의 공유 dependency 충돌·테스트 seed
오류와 수정 전 revise dueAt·취소 outbox UUID 오류를 원본 log로
보존했다. final run7은 14.517초에 exit0이었다.

이 검사는 승인100→개정120 거부, 재고 불변, 전달/수락/도착 분리,
현재 MANAGER 철회, 만료·stale revision, 반려/조건부 거부, 멱등 재시도와
동일 키 다른 payload 충돌, 취소40 뒤 출하60와 잔여 책임을 확인했다.
원문 공급자 응답의 공개 record→review→link 경로와 60+40/초과·
다중PO 실제 receipt는 통합 native adapter 검증에 남아 있다.
전체 S3/사용자 Step3 완료로 표시하지 않는다.
