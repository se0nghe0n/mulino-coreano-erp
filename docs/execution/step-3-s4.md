# S4 판매·인도·반품·회수·정산 구현

S3 종료 baseline은 `eafddb5f44ee942dd5172bcdc21af91b04766a2b`이며
로컬 tag는 `step3-s3-complete`다. 사용자 Step3은 계속 ACTIVE다.
이 계획을 기록한 commit을 `step3-s4-baseline`으로 고정하고 모든
writer는 같은 기준선의 독립 worktree에서 시작한다.

## 소유권

모든 worker는 실제 GPT-6.1 Sol medium이다. 지원되는 Ultrafast 또는
Fast를 사용하되 도구가 노출하지 않는 service tier를 변경했다고
주장하지 않는다. 경로 prefix는
`/Volumes/VideoStore/Developer/mulino-ontology-step3-s4-`이고 branch는
`step3/s4-<suffix>`다. coordinator만 Task branch에 통합한다.

| suffix | 독점 소유 범위 |
|---|---|
| integration | `application/trade` 바로 아래 공통 ports, 기존 core/evidence/work/evaluation/responsibility 연결, V28 cross-domain 제약, S4 schema compatibility와 기존 S1 parity test |
| sales | `application/trade/sales`, `domain/trade/sales`, `sales.cds`, `sales-model.cds`, V23; SalesOrder와 Delivery/ObservedMovement 및 인도 정정 |
| inventory | 기존 inventory/quality와 InventoryValiditySweeper, 신규 fulfillment primitive/commands, `inventory.cds`, V24; 배분·피킹·출고·물리 범위 보존 |
| returns | `application/trade/returns`, `domain/trade/returns`, `returns.cds`, `returns-model.cds`, V25, 신규 ReturnStockPrimitives |
| recall | `application/trade/recall`, `domain/trade/recall`, `recall.cds`, `recall-model.cds`, V26, 필요 시 신규 RecallStockPrimitives |
| settlement | `application/trade/settlement`, `domain/trade/settlement`, `settlement.cds`, `settlement-model.cds`, V27 |
| adapter | `verification/actual/s4`, S4 native runner와 고유 tests, 기존 실제 adapter의 필요한 연결, `./verify actual-s4` |
| e2 | `verification/actual/s4/e2*.json`, `e2-author.py`, 고유 검증 기록; 회수 종단 입력·독립 관찰 |
| c4 | `verification/actual/s4/c4*.json`, `c4-author.py`, 고유 검증 기록; 인도 정정·관측 종단 입력·독립 관찰 |

각 worker는 자신의 고유 tests와 `docs/execution/s4-<suffix>`도 소유한다.
E2/C4는 같은 `step3-s4-baseline`에서 만든 추가 worktree에 `d9ec350a`까지의
Task 통합 결과를 dependency로 반영했다. Adapter 담당자와 두 고유 입력
경로의 소유권을 분리했고 shared fixture·runner의 소유권은 유지한다.
다른 worker의 파일은 임의로 수정하지 않는다. Integration은 다른
소유 package 아래 파일을 바꾸기 전 담당자와 계약을 정한다. 기존 S3
기능을 삭제하거나 대체 stub으로 검증을 통과시키지 않는다.

## 먼저 고정할 계약

SalesOrderLine, Dispatch/CargoScope, 실제 Delivery, Return, Recall의
읽기 계약을 integration과 담당자가 먼저 합의하고 작은 OWN commit으로
공유한다. Evidence는 기존 원본→claim→현재 source 대조→canonical 경로를
사용하며 각 도메인의 TradeEvidenceScopePort가 원문 의미·정체성·범위와
동일성을 확인한다. DB에 검증 완료 결과를 seed하는 것으로 public 경로
인수를 대신하지 않는다.

모든 원장 쓰기는 inventory package의 primitive만 수행한다. 판매
writer는 직접 원장을 쓰지 않고 inventory가 제공하는 dispatch/관측
이동 primitive를 호출한다. Returns/recall의 고유 primitive도 공통
보존·계보·현재 권한/fence 규약을 따른다.

기존 split의 배분 이관에는 S4의 정확한 물량 좌표·customer·action·
권한 범위를 보존해야 한다. 부모의 허용/제한 범위가 자식에 어떻게
대응하는지 명시하고, 단순히 과거 허용을 복사하거나 더 넓은 범위를
허용하지 않는다. 식별 불가능 혼합은 범위 숫자로 깨끗한 물량을
만들 수 없다. 기존 예약 의무는 실제50 정정에도 삭제하지 않는다.

## 필수 결합 인수

- C1의 보유100/판매40과 처분 근거 철회 뒤 새 예약·출고0, 기존 책임
  보존을 실제 command로 확인한다.
- V2는 실물60/기존예약40에서 split과 신규20의 실제 transaction
  경합, 부모 재소비0, 실제50 정정 후 실행배분≤50과 부족 책임을
  deterministic barrier로 검증한다.
- V3는 현재 적격 조회 뒤 QC hold를 먼저 commit하면 출고0, 반대순서는
  출고 이력과 후속 책임 유지, 늦은 옛 해제로 새 hold 해제0을 검증한다.
- CONSUMED 배분의 정상 인도, 출고 뒤 recall/만료에도 실제20 인도와
  운송10 보존, 허위/미연결 관측의 정상 효과0을 확인한다.
- C4의 인도100/반품20은 별개 사건이다. 실제98 정정은 과거 판정과
  현재 의무2를 남기며 권한 있게 해소한 의무는 재생성하지 않는다.
- E2는 QC20와 회수보류60 독립, ADMIN scope50에서 회수25/폐기25를
  처리50으로 합산하지 않음, 미확인25와 예외 책임을 확인한다.
- E1은 실제 구매100→수령60+40→QC60/보류40·기관30→예약/출고/인도30→
  반품보류10→송장 차이5다. W보유80, 누적구매100, 과거인도30,
  반품10, 현재판매0, 은행효과0과 QC/반품/정산 각각의 인간 책임을
  같은 ID·snapshot의 명사/동사 조회와 독립 원장으로 대조한다.

긴 Maven/verify/native 실행은 전체 동시2개다. Slot을 받은 뒤 실행하고
종료하면 즉시 반환한다. Java21/고정 Node24와 fresh worktree의 npm ci를
사용한다. 실제 모델·외부 신고·은행·유료 배포는 실행하지 않는다.
각 OWN commit, 실제 command/result/raw evidence, 제한과 미완료를
반환한다. 전체 통합·관련 지적 수정·결합 checks 전 S4를 닫지 않는다.
사용자 Step3 전체의 Sol xhigh/Astra low review는 S6 구현까지 통합한
뒤 수행한다.

초기 통합에서 확인한 물량 좌표·이동 합계·판매 revision·증거 결합·회수
보류 문제와 검증 조건은 [통합 검토 기록](s4-integration-notes.md)에 남긴다.
Source 수정과 runtime 검증 상태를 구별한다.
