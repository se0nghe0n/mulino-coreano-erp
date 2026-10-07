# S3 구매·운송·수입·수령·QC 통합 기록

사용자 Step3는 ACTIVE다. S2 core 완료 tag `step3-s2-complete`의
235 backend·424 harness·48 native HTTP/JDBC 검사 뒤 S3를 시작한다.
모든 writer는 이 기록을 포함하는 `step3-s3-baseline`에서 분기하며
실제 GPT-6.1 Sol medium으로 실행한다. 지원되는 가장 빠른 service tier를
사용하되 도구가 tier 설정·확인을 노출하지 않는 한 전환 성공을 주장하지
않는다. 구현 소유권과 별개로 coordinator만 Task branch에 통합한다.

## Subtask 소유권

공통 worktree prefix는
`/Volumes/VideoStore/Developer/mulino-ontology-step3-s3-`다.
branch는 `step3/s3-<suffix>`다. 기존 ERP 코드·tests를 재사용하지 않는다.

| suffix | 소유하는 코드·schema·인수 |
|---|---|
| integration | application/trade 공통 ports와 결합, core/identity/policy/work/evaluation 및 기존 query의 필요한 glue, 공유 build/import, V21 필요 시, S3 schema parity manifest와 결합 검사 |
| purchase | domain/trade/purchase·application/trade/purchase, purchase.cds·purchase-model.cds·V16, 제안/개정/승인/전달/공급자 응답/취소, T13 |
| shipment | domain/trade/shipment·application/trade/shipment, shipment.cds·shipment-model.cds·V17, cargo/leg/인계/수량 차이와 C5 책임, T14 |
| regulatory | domain/trade/regulatory·application/trade/regulatory, regulatory.cds·regulatory-model.cds·V18, 작성/제출/기관 결정/표시 확인과 version·부분 범위, T15 |
| receipt | domain/trade/receipt·application/trade/receipt 및 새 ReceiptStockPrimitives, receipt.cds·receipt-model.cds·V19, provisional/실수령/운송 물량 이동/PO 기여, 중복·60+40·초과·V6 |
| quality | domain/inventory의 새 Quality 계열·application/quality, inventory.cds의 제한/처분 확장·quality.cds·quality-model.cds·V20, hold/release/disposition/현재 적격량·무이벤트 만료 sweeper, T16·C1 |
| remedy | 새 TradeResidualRemedy와 최소 responsibility hook, 실제 후속 수령에 따른 부족 책임 잔여량 대조·부분 해소·이력 보존 |
| adapter | verification/actual/s3·S3 고유 harness·실제 transport/fixture/SQL 확장·verify entrypoint, 공개 API와 독립 DB를 통한 S3 인수 |

각 worker의 고유 tests와 `docs/execution/s3-<suffix>`도 해당 worker가
소유한다. 다른 소유 파일은 직접 고치지 않고 필요 계약과 반례를 보낸다.
공통 ports는 integration이 먼저 고정하며 도메인 worker는 제공자와
소비자를 연결한다. 임시 fallback/stub을 최종 기능으로 남기지 않는다.
새 model import는 각자의 `backend/srv/<suffix>-model.cds`에 두어
공유 파일 충돌 없이 단독 CDS compilation을 지원한다. migration의
cross-domain FK는 해당 dependency가 존재하는 순서에 둔다.

## 결합 계약과 검사

모든 쓰기는 기존 ApplicationCommands와 현재 identity/policy/grant/
subject/revision/approval 검사를 사용한다. domain 결과는 공통 APPLIED와
별도 업무 상태로 표현한다. 실제 외부 전달 미확인은 별도 pending으로
남긴다. 현재 인가 시계와 실제 사건/기록 시각을 혼동하지 않는다.
물량은 inventory primitive에서만 쓰며 원장·의무·판정 invalidation·
감사·outbox·멱등 결과가 같은 transaction을 공유한다.

필수 종단 흐름은 구매100→승인→전달/공급자 응답→선적→실수령60+40,
다른 출처의 같은60 중복 제외, 초과/부족의 별도 대조, 기관허용30·QC100의
최대30 판매 후보, 겹친 제한의 독립 해제, 취소 후 잔여 책임 보존이다.
예정 운송·작성 문서는 실물/제출을 만들지 않는다. 임시 수령은 적격0,
기존 운송 물량은 새 입고량으로 중복 생성하지 않는다.

긴 Maven/verify/native 실행은 전체 동시2개다. coordinator에게 slot을
받고 실제 실행 종료 뒤 즉시 반환한다. Java21과 고정 Node24를 사용하고
fresh worktree의 backend에서 npm ci를 먼저 수행한다. 실제 외부 기관·
공급자 전송, 유료 모델과 BTP는 이 S3 개발 fixture 실행에 포함하지 않는다.
모든 소유 산출물 통합·관련 검토 지적 수정·결합 검사가 통과하기 전
S3를 닫거나 S4를 시작하지 않는다.

## 초기 통합

공통 inventory metric/noun provider와 canonical evidence/잔여 책임
ports, 구매·운송·규제·수령 초기 구현을 `320eda6`까지 통합했다.
QC 구현과 각 영역의 최종 tests는 진행 중이다. 이는 S3 완료가 아니다.
[초기 계약 검토](s3-review/early-review.md)의 표시 거절/시간 경계
지적을 수정하고 실제 실행 근거를 추가한다.

실제 native의 주 구매→신규 수령 흐름은 final 재고를 seed하지 않는다.
별도의 기존 운송 물량 이동 사례는 원본/원장이 일치하는 TRANSIT60을
명시적인 시작 fixture로 사용한다. 실제 confirmReceipt 후 총60 유지,
운송0/보관60과 단일 이동·재시도 무효과를 독립 관찰한다. 이 사례는
운송 재고의 최초 취득 성공을 주장하지 않는다.

## 첫 결합 컴파일과 수정 중인 인수

`e71bbfe`의 첫 결합에서 main 158개 source compile은 통과했다.
테스트 compile은 QualityPostgresTest의 미정의 변수 두 참조로 실패했고
`2a53f0b`에서 수정했다. [원본 로그와 source 기록](evidence/step3-s3/initial-compile/summary.json)을
보존한다. 테스트를 실행한 결과로 표시하지 않는다.

worker의 후속 결합 compile은 통과했지만 전체 context에서
ReceiptRepository의 final 선언이 Spring proxy 생성을 막았다.
`56d6be4`에서 수정했고 실제 DB 검사를 재개한다. 별도로 거래 원본의
PURCHASE_ORDER subject DB 제약과 같은 물량에 대한 후속 기관 결정의
canonical identity를 보완한다. 실제 수령 뒤 부족 책임을 현재 잔여량과
맞추는 동작도 검증 중이다. 이 항목과 native 경로가 남아 S3는 ACTIVE다.
