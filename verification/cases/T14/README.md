# 선적 배분 구간 인계와 원천 관측을 보존한다

## 독립 손계산과 범위

PO-A60과 PO-B40을 S1(A40+B10)과 S2(A20+B30)에 나눈다. 서로 다른 leaf 합계는100 BOX이며
같은 A40을 또 배정하면 거부한다. 항구와 W 도착을 구별하고 보관 인계는 소유권 변경으로 기록하지 않는다. 출하 주장100,
실제 수령98, 확인된 운송2는98+2=100이며 자동 손실은0이다. 다른 분기의 미관측2는 UNKNOWN이다. 종료된 부모의
온도35 CELSIUS 이상과 업무 연결 실패를 주입하고 실제 notification scheduler tick/terminal과
전달 뒤에도 접수 책임이 남는지 확인한다.

fixture의 historicalFacts는 대조에 필요한 원천 입력이다. 승인·매칭·기관 판정·판매 적격
projection·의무 해소를 예상값으로 설치하지 않는다. 구매·수령·반품처럼 이 사례가 검증하는 효과는 Gherkin의 개별
명령으로 실행한다. 모든 fixture는 synthetic=true이며 운영 규제와 지급 권한의 근거가 아니다.

## 원행 관찰 계약

observe는 설치된 isolated organization·item·case·subcase와 API가 발급한 실제
snapshot token을 명시한다. source별 rawRows와 sourceEvidence의 실제
query/parameter/mapping/artifact를 요구한다. 수량은 decimal 문자열과 실제 단위로 검사한다.
identity는 strict alias/result ref를 사용하며 고정 수량 기대값은 결과에서 복사하지 않는다. 의무는
종류와 OPEN 범위로 좁혀 owner·nextAction·nextCheckAt·현재 assignment 수를 검사한다. 거부의
전후 원행과 허용된 감사는 별도로 관찰한다. 원행 배열의 sameAs 비교는 observer의 안정된 ID 순서를 요구한다.

## 규범 catalog와 실제 assertion 연결

| oracle / named observation | subcase / assertion |
|---|---|
| T14.many-to-many-shipment / distinct-cargo-total | many-to-many/physical100, many-to-many/physical-unique |
| T14.many-to-many-shipment / double-order-allocation | many-to-many/double-rejected, many-to-many/cargo-no-double, many-to-many/double-denial-audit |
| T14.many-to-many-shipment / leg-facts | many-to-many/po-cargo-tuples, many-to-many/port-not-W, many-to-many/handover-custodian, many-to-many/ownership-unchanged, many-to-many/planned-actual-legs, many-to-many/source-genealogy4, many-to-many/genealogy-original-source |
| T14.arrival-discrepancy-not-loss / received | discrepancy-transit2/received-api98, discrepancy-transit2/received98, discrepancy-transit2/receipt98, discrepancy-unobserved2/received-api98, discrepancy-unobserved2/received98, discrepancy-unobserved2/receipt98 |
| T14.arrival-discrepancy-not-loss / transit | discrepancy-transit2/transit-api2, discrepancy-transit2/transit2 |
| T14.arrival-discrepancy-not-loss / automatic-loss | discrepancy-transit2/loss-api0, discrepancy-transit2/no-loss-movement, discrepancy-transit2/loss-before-after, discrepancy-unobserved2/loss-api0, discrepancy-unobserved2/no-loss-movement, discrepancy-unobserved2/loss-before-after |
| T14.arrival-discrepancy-not-loss / separate-observations | discrepancy-transit2/shipclaim100, discrepancy-transit2/physical-total100, discrepancy-unobserved2/shipclaim100, discrepancy-unobserved2/physical-total100, discrepancy-unobserved2/unobserved-is-unknown, discrepancy-unobserved2/unobserved-not-zero |
| T14.arrival-discrepancy-not-loss / discrepancy-responsibility | discrepancy-transit2/discrepancy-owner-one, discrepancy-transit2/discrepancy-owner-owner, discrepancy-unobserved2/discrepancy-owner-one, discrepancy-unobserved2/discrepancy-owner-owner |
| T14.orphan-temperature-intake / orphan-intake | orphan-temperature/intake-human-one, orphan-temperature/intake-human-owner |
| T14.orphan-temperature-intake / notification-closes-intake | orphan-temperature/no-notification-resolution, orphan-temperature/links-still0, orphan-temperature/notification-delivered |

## 실행과 제한

B2는 feaca0af9673620eff9a5ac0f08a657ce14e9ccd다. 실제 제품
adapter·DB·기관·host·model 실행은 NOT_RUN이다. schema 검증과 Cucumber file
selector RED는 준비 증거이며 runtime PASS가 아니다. `SupplyAssertionTest`는 선언한
assertion과 고정 관찰 입력의 mutant를 검사하며 제품 service나 효과를 흉내 내지 않는다. 정확한 실행
명령·exit·발견 수·fixture hash는 evidence/preparation/에 기록한다. 실모델·유료 호출·외부
제출·은행 이체를 실행하지 않는다.
