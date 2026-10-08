# 수령 응답 유실과 멱등성의 테스트 계약

fixture는 미수령 실물 입력을 제공한다. 수령60의 결과나 COMMITTED
record를 미리 seed하지 않는다. 첫 confirmReceipt 뒤 실제 fault가
DB commit 후 응답을 버린다. token을 갱신하고 새 RPC ID의 두 retry를
각각 start→barrier reached ACK→resume→terminal await로 수행한다.
replay의 활동/명령/transaction ID는 결과 reference로 연결한다.

실물60 BOX, 수령 movement·COMMITTED command·receipt outbox 각각1,
현재 remainder assignment1과 동일 transaction의 감사/결과를
독립 원행으로 검사한다. IN_PROGRESS 뒤 rollback의 실물/원장/outbox
효과0도 별도 분기다. rollback된 record를 성공으로 재사용하지 않는다.

같은 key의40은 IDEMPOTENCY_CONFLICT이고 추가 효과0이다. 다른
주체의 결과 조회와 철회된 결과 replay는 원 결과를 누설하지 않는다.
PO-A/K-A와 PO-B/K-B는 구별된 실물60씩이며 총120 BOX다.
REJECTED의 변경은 새 canonical key를 사용하며 같은 원천 사건의
효과를 다시 만들지 않는다. DB unique column의 조직/안정 주체/
capability/key를 독립 catalog로 확인한다. 가상 retention sweep
뒤에도 업무 효과 보존기한까지 tombstone과 원 효과 참조가 남는다.

입력 수집 중 destination 보완은 같은 conversation과 새 RPC/state로
이어진다. 구조화는 효과0이며 검증한 목적지·새 canonical hash/revision을
반환한다. 최종 command effect key와 구별한다.

모든 fixture는 가상값이다. Step2의 parser·assertion·RED 준비와 실제
제품 DB/API/MCP/경합/host/모델 인수를 구별한다. 제품 인수는 NOT_RUN이다.

작성 검증의 실제 명령·exit·scenario 수·파일 hash는
`verification/cases/T08/evidence/authority-suite/checks.json`에 있다.
`observation-bindings.json`은 이 case의 모든 catalog observation을
구체 subcase/action/assertion 및 JSON pointer에 연결한다.
고정 수량 oracle의 primary와 보조 관계/assertion을 함께 보존한다.
이 연결은 작성 증거이며 실제 제품 효과를 관측한 결과가 아니다.

## barrier 표기(Step 2 재검토 2차)

동시 재시도 두 start는 [V2 경합 관찰 계약](../V2/race-observation-contract.md)의
top-level `testTransactionId`·`testParticipantId`·`testBarrierId`·
`testBarrierPoint`로 barrier를 건다. control의 transactionId는 같은
label이다. 두 ACK의 `data.database.transactionId`가 서로 다른 실제
transaction임을 `race-distinct-db-transactions`로 확인한다.
