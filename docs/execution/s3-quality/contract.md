# S3 품질·행동 적격 계약

QC 통과만으로 고객 보관 물량을 판매할 수 없으므로 현재 허용을
실물 좌표와 행동별로 분리한다. 이 문서의 정책은 fictional local
fixture이며 공식 법규 승인이나 운영 처분 권한이 아니다.

## 명령과 인가

`placeHold`, `releaseHold`, `revokeDispositionBasis`는 COMMAND다.
`recordDispositionBasis`는 RECORD다. 모든 쓰기는
ApplicationCommands의 현재 grant·policy·definition·revision·audit·
idempotency transaction을 사용한다. creation의 expectedRevision은
segment revision이고 release/revoke는 참조한 permission revision이다.

creation slots는 segmentId, action, category, startQuantity, quantity,
unit, validFrom, validUntil, evidenceId, reason, workId, nextCheckAt이다.
customerId가 있으면 그 고객에게만 적용한다. release/revoke slots는
restrictionId/dispositionBasisId, evidenceId, reason이다.

현재 COMMAND policy의 각 rule은 effectClass QUALITY_CONTROL과
categoryCapabilities를 가진다. 예를 들어 QC→decideQuality,
CUSTOMER→confirmCustomerConditions,
COMMERCIAL→confirmCommercialDisposition,
RECALL→decideRecallRestriction이다. 해당 decision capability의
현재 assignment와 grant를 모두 가진 인간만 결정한다. 미설정은
POLICY_UNRESOLVED이고 QC 권한을 COMMERCIAL로 자동 승격하지 않는다.

증거는 원본 JSON blob, exact SEGMENT subject, current VERIFIED
canonical scope를 요구한다. creation 원문은 segmentId, operation,
action, category, startQuantity, quantity, unit, validFrom, validUntil을
명령과 동일하게 담는다. release/revoke 원문은 operation과 참조
permission ID를 담는다. superseded/invalidated evidence는 허용하지 않는다.

## 판정과 수량

ELIGIBILITY policy의 actions.SELL.requiredCategories에는 QC,
CUSTOMER, COMMERCIAL이 모두 필요하다. regulatory 정책과 해당 실물의
기관 허용은 별도 provider로 읽는다. 미설정 정책이나 미식별 mixture는
confirmed eligible에 포함하지 않는다. 보유·owner·custodian과 처분
권한은 독립이다.

수량 좌표는 half-open [startQuantity,startQuantity+quantity)다.
같은 조건의 겹친 허용은 union하고 QC·고객·상업·기관 범위는
intersection한다. 제한 범위는 subtraction한다. 기관30/QC100이
다른 조건100과 만나면 최대30이다. 서로 어긋난 허용30 두 개를
양만 비교해30으로 만들지 않는다. 보류는 원장을 감소시키지 않는다.

permission/LOT의 시간 유효성은 full closed [from,until]이다.
PostgreSQL의 microsecond 저장 정밀도에 맞춰 until+1µs를 다음 만료
경계로 계산한다. current identity/policy의 기존 half-open 계약은
그대로 재검증한다. release는 releasedAt부터 적용한다.

release는 지정한 hold만 해제하고 그 hold의 QUALITY_REVIEW 의무만
증거로 해소한다. 겹친 다른 hold와 SUSPENDED allocation은 유지한다.
hold/revoke는 각 allocation의 현재 행동·고객·실물 범위를 재평가한다.
교집합이 부족한 allocation만 정지하고 근거와 책임을 유지한다.
다른 행동, 제한 밖 범위, 겹친 다른 허용이 충분하면 실행 상태를 유지한다.

InventoryReadFacts는 eligibleQuantity, reservedQuantity,
unreservedEligibleQuantity, eligibilityStatus를 소유한다. reserved에는
EXECUTABLE과 SUSPENDED 책임을 포함한다. 식별된 allocation 좌표를
eligible에서 빼고 미식별 좌표의 예약량은 보수적으로 차감한다.

## 무이벤트 만료

InventoryValiditySweeper는 저장된 기존 allocation을 다시 읽고
segment/control/item/place, 현재 policy 및 identity fence 안에서
적격성과 reserve 권한을 검증한다. 만료한 예약은 SUSPENDED로 변경하고
원 source command와 stable expiry scope로 INVENTORY_VALIDITY 의무를
연결한다. owner, nextAction, nextCheck 및 boundary audit는 같은
transaction에 남는다. 실패하면 부분 변경을 남기지 않는다.

S4 판매·예약·출고 명령은 이 구현 범위에 없다. sweep가 늦어도 현재
eligibility를 읽으면 만료량은0이다. 새 S4 실행 guard는 반드시 같은
scope fence에서 실제 allocation 범위와 현재 판정을 다시 검사해야 한다.

## 검증 상태

초기 Java21 compile과 QualityRangesTest 네 검사는 PASS다.
첫 PostgreSQL 실행에서 fresh PostgreSQL18.6 V20 적용은 PASS지만
SourceProfiles fixture revision0이 revision>0 제약에 걸렸다. revision1로
수정했다. 이후 compile와 Spring context 문제를 dependency 수정으로
해소했고 source helper의 null operation 처리도 수정했다. 현재 gateway
종단 검사는 재실행 대기다. 최종 결과는 evidence.md에 남긴다.

실기관·실공급자·유료 모델·BTP 호출은 이 local fixture 범위에서
실행하지 않았다.

creation 원천은 `QUALITY_DECISION_<operation>_<category>` 사건으로
구별하고 canonical quantity도 해당 permission quantity와 같아야 한다.
서로 다른 독립 조건을 하나의 사건 수량으로 합치지 않는다. 기존 generic
segment 대조가 전량만 허용해 부분30 검증을 만들 수 없는 결합 gap을
발견했으며 integration이 bounded interval 대조를 구현한다.

`requireExecutableAllocation`은 S4 소비자가 기존 gateway transaction에서
호출할 port다. 저장된 조회시점을 실행 권한으로 사용하지 않고 server
clock과 같은 scope·policy·identity fence에서 실제 배분 좌표를 다시
검증한다. 늦은 sweep 전에 만료20의 실행을 이 guard가 거부한다.

hold/revoke의 QUALITY_REVIEW root와 leaf 및 assignment에는 실제 제한
수량·단위를 전달하고 residual scope에 startQuantity와 행동·category를
담는다. 수량 없는 일반 의무의 기본1로 보류20을 표시하지 않는다.

동일 sourceDecisionId·sourceVersion·category·operation·segment는
출처가 달라도 같은 occurrence identity를 쓴다. 서로 다른 결정은 같은
시간에 기록돼도 별개다. 공유 대조는 원문의 canonical JSON 의미 hash가
다르면 같은 identity로 합치지 않는다.
