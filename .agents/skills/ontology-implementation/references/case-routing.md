# 요구사항과 oracle 찾기

이 표는 구현 범위와 빠뜨리기 쉬운 관찰을 찾는 index다. seed 수량, 실행
순서와 정확한 전체 oracle는 `docs/ontology-implementation-plan.md` §13의
기준선을 사용한다. ID 존재나 문서 내용 일치는 실행 coverage가 아니다.

## D01–D26 → T01–T26

| 요구/test | 변경 소유 경계 / 꼭 관찰할 실패 |
|---|---|
| D01/T01 | 두 진입점 / 다섯 역량 질문의 ID·시점·근거·책임 동일, 제외 효과0 |
| D02/T02 | item/spec/unit / issuer 충돌, 환산을 재고나 주문 대체로 승격 금지 |
| D03/T03 | segment/계보 / 부모 재소비·순환, 혼합 식별 불가 범위 |
| D04/T04 | decimal/원장 / EA0.5·정밀도 거부, 보류≠감소 |
| D05/T05 | 위치·처분 / C1, 계획 목적지≠현재 위치 |
| D06/T06 | 시간·정정 / asOf/knownAt 차이, 미입력·미확인·상충·해당없음 |
| D07/T07 | 타입·문서 / endpoint/cardinality 거부, 문서2개 수령60 |
| D08/T08 | identity / C3, 조직 ID/search/FK·grant 발급/철회·상향 거부 |
| D09/T09 | GoalVersion / C2, 단계별 slot, THROUGHOUT 관측 공백 |
| D10/T10 | 의무·인계 / 수락 전 owner, 이전 실패/부분 보존, C5 |
| D11/T11 | Work 전이 / 부모90/100, 부족 이전≠FULFILLED, 순환/timeout |
| D12/T12 | Assessment / evidence 부족, 납기 위반 보존, C4 |
| D13/T13 | PO/승인 /100→120 재승인, 전달/수락/도착 분리, 초과5 |
| D14/T14 | Shipment/inbox / 다대다 배분, 운송중2≠분실, C5 |
| D15/T15 | regulatory / 기관30/QC100 상한30, version, 작성≠제출 |
| D16/T16 | 수령/QC / 임시적격0, 독립 제한, 무이벤트 만료 예약20 |
| D17/T17 | sales/allocation / CONSUMED 뒤 인도, recall 뒤 실제사실, 허위0, V2/V3 |
| D18/T18 | return/recall / C4, 중복반품0, 회수25+폐기25≠50, 양방향 trace |
| D19/T19 | settlement / EUR/FX snapshot·송장 종류, 물류/정산 독립, 은행0 |
| D20/T20 | intent/MCP / 두 조회 snapshot, slot 오류/MRTR, C3 의미 평가 |
| D21/T21 | FDE / 발행 불변·지원 manifest·명시 전환, V1 |
| D22/T22 | source/reconciliation / source hash conflict·canonical dedup·UNKNOWN_EXTERNAL |
| D23/T23 | platform/cutover / 자료 fresh/live 분기, 새schema v1→v2, CDS drift, V8 |
| D24/T24 | auth/audit/retention / 모든 경로·blob, 감사 rollback, legal hold·redaction |
| D25/T25 | traceability / assertion+artifact 연결, 결정적/모델/규제/BTP 분리 |
| D26/T26 | runtime / due/claim/intake/outbox/boundary, safe retry, C5/V5, 복원 |

## 교차 fixture의 수용 경계

| ID | 핵심 판정과 관찰 |
|---|---|
| C1 | 구별된 위탁40/고객보관60: 보유100·판매40·미예약40. QC/앱 권한으로60을 더하지 않음. 허용 철회 후 새예약/출고0·책임 유지 |
| C2 | 월60 수령→화60 출고→수40 수령: 누적100 충족, 지정시점 재고100은40으로 미충족 |
| C3 | role WRITE∩grant READ: 공개/worker 쓰기·후속효과0, 조회 audit 허용. 자연어 조회 의미 평가는 별도 |
| C4 | 인도100/반품20과 실제98 정정 분리. 과거 판정·현재 의무2, 해소된 의무 부활0·중복반품0 |
| C5 | 부모 종료 뒤 이상·업무연결 실패: intake 책임 유지, retry 뒤 의무/업무1. 정상관측 업무0·알림≠종료 |
| V1 | v1 업무/v2 정의·evaluator: 과거 의미+현재 제한, 미지원 보류 owner/효과0 |
| V2 | 예약40/실물60의 분할+신규20 경합, 부모소비0. 실제50 정정 후 실행배분≤50·부족 책임 |
| V3 | eligibility 조회 뒤 QC hold commit→출고0. 역순 출고이력+후속책임. 옛해제로 새보류 덮기0 |
| V4 | READ grant의 direct/nested/batch/projection/MCP/worker 우회효과0. atomic batch rollback·내부move 우회0 |
| V5 | queue 뒤 DB WAITING만 남겨 전체restart, due 전진·2worker·장애에서도 재개/책임1·중복0 |
| V6 | 수령60 commit/응답유실·newtoken/newRPC/동시retry 효과1. 동일key40 conflict·타주체 누출0 |
| V7 | enqueue/인가검사 직후 grant철회·restart: 직렬화 순서에 맞는 새효과0/기효과보존·차단책임 |
| V8 | exact manifest·빈설치·새ontology v1→v2·진행v1자료·DB/lock/auth/TLS/wire/backup. local/BTP/client 결과 분리 |
| E1 | 구매100·수령60+40·중복60 증빙·QC60/40·기관30→출고/인도30·반품보류10·송장차이5. 보유80·판매0·은행0·QC/반품/정산 책임 |
| E2 | LOT60의 QC20+회수보류60 중첩. QC해제만으로 출고불가. 회수scope50·회수25/폐기25·미확인25는50처리/종료 불가 |

최초 전체 V1/V5/V7과 모든 effect의 V6/V4는 S5, V2/V3는 S4,
실수령 V6는 S3, V8은 S6에서 인수한다. 앞선 partial gate의 통과 범위와
나중에 추가할 명령/adapter를 manifest에 명시한다.

## E1 구현 전 walkthrough

다음은 oracle 설계 점검이다. 예상값이며 실제 runtime 관찰이 아니다.

1. 구매100 승인 hash/GoalVersion을 저장한다. 원장 재고는 아직0이다.
2. W 실제 수령60을 canonical occurrence R60에 연결하고 운송/창고 문서가
   같은 R60을 증명하도록 한다. 다른 source key라도 원장 효과와 PO
   ReceiptContribution은60 하나다. 별도 실물 R40 수령 뒤 총100이다.
3. 구별된60은 QC pass,40은 QC hold다. QC60 안의30에만 기관 허용을 둔다.
   나머지 조건은 허용으로 고정한다. current eligible30을 원장과 제한
   scope의 교집합으로 계산한다. 태그 합산으로90/100이 되면 실패다.
4.30 예약→출고에서 배분 CONSUMED와 W 보유70/운송30을 관찰한다.
   인도 RECORD를 Dispatch/CargoScope에 대조해 고객30으로 옮긴다.
5. 그 인도 일부10 반품을 새 receipt/보류로 연결한다. W는80,
   구매 누적100·과거 인도30·반품10·현재 판매0이다. 반품은 구매110이나
   인도20 정정이 아니다. 은행 효과는0이다.
6. 송장 차이5와 QC40/반품10의 미해결 의무·인간 owner·다음 행동을
   양쪽 조회에서 확인한다. 물류 도착 목표와 정산 목표 판정은 독립이다.

각 단계의 API response, active leaf/원장, contribution, restriction,
allocation, assessment, obligation, audit, outbox를 assertion으로 남긴다.
DB 한 합계만 맞는 것은 E1 통과가 아니다.

## V3 경합 walkthrough

출고 application service의 최초 적격 조회 뒤에 deterministic barrier를
두고 다른 transaction의 QC hold commit을 완료한 뒤 출고를 재개한다.
새 제한 추가와 출고가 같은 segment/scope fence에 참여해야 한다.
hold가 먼저 직렬화됐으면 출고효과·원장감소·allocation 소비·출고 outbox는0,
거부 결과와 기존/새 책임을 확인한다. 실패 assertion 이후 fixture를
약화해 PASS로 만들지 않는다.

반대 순서도 실행한다. 출고가 먼저 commit됐으면 그 출고를 지우지 않고
운송/고객 범위의 새 제한과 후속 의무를 연결한다. 별도 source version
fixture에서 늦게 도착한 옛 release가 새 hold를 지우지 않아야 한다.
sleep 타이밍에 기대지 않고 barrier/clock·두 transaction·정확한 DB
version과 격리/lock evidence를 기록한다. V3는 한 번의 순차 happy path나
mock lock 테스트로 입증하지 않는다.
