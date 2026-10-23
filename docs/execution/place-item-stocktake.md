# 장소·품목 단위 실사 관찰을 기록한다

같은 장소의 A60·B40을 관측한 98을 A60 한 segment의 수량으로
기록하지 않도록 recordStocktake의 장소·품목 입력을 추가한다.
관측 98과 기록 시점 장부 100은 서로 다른 값이며, 2의 손실이나
개별 segment의 관측 수량을 추정하지 않는다. 계획 §4.3, D06/T06,
M60의 원천 관찰 계약을 다룬다. 전체 M60 인수는 아직 미실행이다.

## 저장과 인가 계약

기존 segmentId 입력은 그대로 유지한다. 새 입력은 itemId·placeId와
선택 lotId이며 segmentId와 함께 사용할 수 없다. observedQuantity,
unit, occurredAt, evidenceRef는 두 경로에 공통이다. 단위 정밀도와
미래 발생 시각을 검사하며, 원래 명령·actor·grant를 자동 변경하지 않는다.

현재 item/place와 모든 active leaf를 같은 품목·장소·선택 LOT로 고른다.
서버 ExecutionClock에 비해 미래이거나 기록 시각이 불명인 행은 거부한다.
현재 command guard의 최대 256 TARGET 조합에 맞춰 place와 최대
255개 member를 한 번에 검사한다. 상한을 넘으면 일부 합계 대신
전체 명령을 거부한다. item/place 및 각 leaf의 공통 fence를 잡은 뒤
cohort를 다시 읽는다. place와 각 member의 TARGET·ITEM·PLACE 및
명시 LOT가 같은 grant의 all-of로 허용돼야 한다. 철회·숨긴 member는
집계로 우회할 수 없다.

V46은 PlaceStocktakes와 PlaceStocktakeMembers 두 immutable table을
추가한다. 관측 시각 occurredAt과 장부 비교 snapshotAt을 나누고,
RECORDED_UNASSESSED 상태·실제 commandId·observedQuantity·bookQuantity를
저장한다. member는 실제 segmentId·revision·장부 수량·단위를 담으며
관측량을 나눠 갖지 않는다. SQL trigger가 header INSERT 시점의 실제
전체 source row를 sourceMembersJson으로 기록한다. caller JSON은
그대로 채택하지 않는다. member와 원천의 정확한 일치, 전체 member 수·
합계와 실제 COMMITTED recordStocktake 결과 연결을 deferred constraint로
검사한다. 나중의 mutable row로 과거 snapshot을 다시 계산하지 않는다.
UPDATE·DELETE와 값이 있는 table의 TRUNCATE는 거부한다.

응답 effects는 placeStocktakeId와 빈 quantityEffects다. movement,
allocation, adjustment, 의무, outbox 또는 은행 효과를 만들지 않는다.
기존 adjustQuantity는 기존 segment Stocktake만 사용하며 새 집계 관측을
손실 조정으로 자동 적용하지 않는다. getObject/searchObjects의
PlaceStocktake noun은 같은 원천 member를 모두 읽을 권한이 있을 때만
관측과 장부 비교를 반환한다. 공통 business world의 추가 조회 인가도
유지한다. 원천 raw JSON은 public 응답에서 제거한다.

## 실행 증거

작업은 coordinator의 별도 tree에서 진행했다.

- 경로: /Users/se0nghe0n/.t3/worktrees/mulino-coreano-erp/coordinator-scenario-descendants-e995595a
- branch: fix/place-stocktake-33f075f8
- baseline: 33f075f8, identity V45를 own tree에 먼저 결합했다.
- 최종 검사: PlaceStocktakePostgresTest 8개, 기존 StockCommandPostgresTest
  14개, InventorySubjectBindingTest 4개, IdentityTradeScopesPostgresTest
  7개, 합계 33개가 실패·오류·skip 없이 통과했다.

새 PostgreSQL에서 V45+V46 전체 migration과 실제 CQN/gateway를 실행했다.
인증된 HTTP로 기록·동일 응답 replay·getObject 조회를 검증했다. raw SQL
관측은 observed98/book100/member60·40, 원 물리 행 byte 불변, 사업 효과0을
확인했다. command와 header의 실제 xmin transaction도 대조했다.
빈 실제 범위의 관측0과 장부0, 미래 row의 전체 거부, 256 leaf 상한 거부,
단위·정밀도·혼합 selector·타 scope 거부, 철회·member 숨김, immutable
위반과 외부 changeset rollback을 검사했다. rollback 시 header/member/
command/audit가 함께 0이었다. 실패를 새 기대값으로 바꾸지 않았다.

실행 명령은 해당 tree의 .local/place-stocktake/run.py에 보존한다.
JDK 21·Maven 3.9.16·공식 Node 24.19.0·고정 npm/CDS lock과 명시한
OrbStack socket을 사용했다. toolchain exact 출력은 versions.json,
최종 raw JUnit·source/class hash·HTTP와 cleanup은 같은 디렉터리의
proof-8.json, source-before-8.json, actual-main-classes.json, http/,
cleanup.json에 있다. sourceDrift는 빈 배열이며 product class는 346개다.
자체 PG container 21개가 모두 사라졌고 임시 key/blob 경로도 없다.
다른 작업자의 실행 자원은 정리하지 않았다.

첫 compile의 CAP requestContext overload 모호성, CONFIRMED fixture의
LOT 누락, 공통 world 조회에 필요한 getInventory 권한 누락, HTTP JWT의
필수 claim 누락과 기존 immutable 행을 바꾸려던 future fixture 오류는
각 attempt 원문에 남겼다. 제품 거부를 통과로 세지 않았다. 최종 future
반례는 새 미래 genesis 행으로 설치해 기존 immutable 사실을 보존했다.

증거 archive .local/place-stocktake/evidence.tar.gz의 SHA256은 다음과 같다.

```text
f7d1a22849351b13059c285616f213fb14faabf8e258ed75b2cb02c120b32cd3
```

원본 60 model corpus·fixture·oracle와 41 scenario의 기대값을 바꾸지
않았다. M60 source graph와 새 observation ID를 root typed bridge에
연결하는 작업, Haiku 실제 시나리오, 전체 backend/harness와 S5/S6
인수는 별도 gate다. 실제 model·Workflow·BTP·규제 실행은 NOT_RUN이다.
