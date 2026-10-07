# 위탁과 고객 보관의 판매 범위

실제 제품/API/DB/host/model은 NOT_RUN이다. 이 파일은 구현 전
검증 계약이며 synthetic 정책·문서를 실제 규제값으로 사용하지 않는다.
fixture는 식별된 시작 사실만 설치한다. 검증할 효과·승인은 반드시
명시 행동으로 실행한다. 조회/거부 감사·REJECTED 명령 결과·책임 알림을
허용하면서 COMMITTED 결과와 BUSINESS outbox 등 금지 효과를 분리한다.

rawRows는 sourceEvidence의 actual SQL·parameters·mapping·artifact를
가진 동일 scope/snapshot의 원 행이다. fixture alias와 신규 result ID는
strict 참조로 연결하고 수량·unit·상태·시간은 독립 고정 oracle로 둔다.
책임 count는 실제 obligationId와 current assignment scope로 한정한다.
transactions는 transactionCommandKeys로 지정된 두 경합 명령만 읽는다.
조회·fixture 설치·명시적 fresh split 거래를 경합 거래 수로 더하지 않는다.
lockProbeScopeOnly는 fixture가 지정한 segment/allocation/fence 세 scope의
원행을 immutable scopeId ASC로 읽는다. acquisitionOrdinal1,2,3이
그 순서에 일치해야 한다. movement는 ledgerSequence ASC로 읽는다.
수량 bound의 DB SUM은 관찰 SQL과 해당 allocations/obligations 원 행으로
추적하며 제품의 aggregate 응답을 복사하지 않는다.

## custody-not-sale

위탁40+고객 보관60=보유100, confirmed SELL40, 예약0/미예약40. 인계·QC해제·앱WRITE가 고객 소유60의 SELL 근거를 만들지 않는다.

- `held-1` → `C1.initial-eligibility / held`: 실물량·단위와 독립 손계산을 대조한다.
- `held-2` → `C1.initial-eligibility / held`: 실물량·단위와 독립 손계산을 대조한다.
- `sell-eligible-3` → `C1.initial-eligibility / sell-eligible`: 실물량·단위와 독립 손계산을 대조한다.
- `sell-eligible-4` → `C1.initial-eligibility / sell-eligible`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `unreserved-eligible-5` → `C1.initial-eligibility / unreserved-eligible`: 실물량·단위와 독립 손계산을 대조한다.
- `unreserved-eligible-6` → `C1.initial-eligibility / unreserved-eligible`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `customer60-added-by-qc-or-app-write-7` → `C1.initial-eligibility / customer60-added-by-qc-or-app-write`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `customer60-added-by-qc-or-app-write-8` → `C1.initial-eligibility / customer60-added-by-qc-or-app-write`: 공개 명령의 구조화 outcome을 확인한다.
- `customer60-added-by-qc-or-app-write-9` → `C1.initial-eligibility / customer60-added-by-qc-or-app-write`: 검증 실패를 해당 오류 코드로 구별한다.
- `customer60-added-by-qc-or-app-write-10` → `C1.initial-eligibility / customer60-added-by-qc-or-app-write`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `customer60-added-by-qc-or-app-write-11` → `C1.initial-eligibility / customer60-added-by-qc-or-app-write`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `customer60-added-by-qc-or-app-write-12` → `C1.initial-eligibility / customer60-added-by-qc-or-app-write`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `customer60-added-by-qc-or-app-write-13` → `C1.initial-eligibility / customer60-added-by-qc-or-app-write`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `active-physical-identities` → `C1.initial-eligibility / held`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `C1.initial-eligibility / held`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `C1.initial-eligibility / held`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.

## revoked-basis

실물40은 유지되며 기존 약속20을 삭제하지 않는다. 처분 허용 철회는 앞으로의 배분/출고0과 owner 있는 의무를 남긴다.

- `new-reservation-1` → `C1.revoked-disposition / new-reservation`: 공개 명령의 구조화 outcome을 확인한다.
- `new-reservation-2` → `C1.revoked-disposition / new-reservation`: 검증 실패를 해당 오류 코드로 구별한다.
- `new-reservation-3` → `C1.revoked-disposition / new-reservation`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `new-dispatch-4` → `C1.revoked-disposition / new-dispatch`: 공개 명령의 구조화 outcome을 확인한다.
- `new-dispatch-5` → `C1.revoked-disposition / new-dispatch`: 검증 실패를 해당 오류 코드로 구별한다.
- `new-dispatch-6` → `C1.revoked-disposition / new-dispatch`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `new-dispatch-7` → `C1.revoked-disposition / new-dispatch`: 명령 효과 scope에서 생성된 업무 원 행 수가0이다. 감사는 별도로 확인한다.
- `new-reservation-8` → `C1.revoked-disposition / new-reservation`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `new-reservation-9` → `C1.revoked-disposition / new-reservation`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `new-reservation-10` → `C1.revoked-disposition / new-reservation`: 금지 효과의 동일 scope 전후 원 행을 비교한다. 조회/거부 감사는 별도 scope다.
- `existing-allocation-11` → `C1.revoked-disposition / existing-allocation`: 독립 원 행의 실물·수량·관계·범위를 정확히 대조한다.
- `existing-obligation-scope-12` → `C1.revoked-disposition / existing-obligation-scope`: 독립 원 행을 해당 범위에서 합산하며 부모와 자식을 이중 합산하지 않는다.
- `revocation-duty-13` → `C1.revoked-disposition / revocation-duty`: 해당 obligation root/scope의 현재 유효 assignment는 하나다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `revocation-duty-14` → `C1.revoked-disposition / revocation-duty`: 책임자의 실제 ID와 다음 행동·확인 시각에 공백이 없어야 한다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `revocation-duty-15` → `C1.revoked-disposition / revocation-duty`: fixture의 지정된 인간 owner/supervisor와 고정 후속 행동/시각이 유지된다. 의무 root는 실제 명령/대조 결과 또는 동일 snapshot 의무 조회의 obligationId로 한정한다.
- `new-reservation-16` → `C1.revoked-disposition / new-reservation`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `new-dispatch-17` → `C1.revoked-disposition / new-dispatch`: 허용된 denial 감사1과 금지된 업무 효과0을 분리한다.
- `active-physical-identities` → `C1.revoked-disposition / new-reservation`: 현재 active 실물 identity를 한 번씩만 합산하며 중복 실물은 거부한다.
- `response-definition-version` → `C1.revoked-disposition / new-reservation`: 수량/제한을 읽는 실제 정의 버전은 고정 v1이며 다른 의미로 대체하지 않는다.
- `actual-baseline-physical-rows` → `C1.revoked-disposition / new-reservation`: 서버에 실제 설치된 시작 실물의 ID·decimal·unit을 원 행에서 확인한다. baseline 자체는 업무 실행 coverage가 아니다.


## 실행 증거

공통 실행 기록은 [inventory 최종 증거](../T03/evidence/inventory-final/README.md)에 있다.
이 case의 각 subcase RED 원본은 `evidence/contract-red-<case>-<subcase>.json`에 보존했다.
