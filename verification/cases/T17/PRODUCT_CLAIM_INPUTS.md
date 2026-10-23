# T17 미확인 인도 주장과 검증된 물량 참조 분리

Task와 Step는 OPEN이다. 이 문서는 `bffbde51`의 두 HTTP 500을 해결하는
bounded product 변경을 기록한다. 이전 실패 원본·257 assertion·모든
T17 입력/가역 ledger는 수정하지 않는다. 인도 goal target은 세 경우
모두 30이고, late/expiry의 관측 인도량은 20이다.

## 동기화 기준

root가 지정한 네 변경만 적용했다. sibling branch는 병합하지 않았다.

| root commit | 이 worktree의 sync commit |
| --- | --- |
| 9b3342ef | cce45ef6 |
| 6fb7fdbd | 99dd6012 |
| 5cd13c68 | 573b4d5e |
| 415a7bb7 | 6197f791 |

## 저장과 승격의 경계

V40은 Observations에 nullable `claimedRangeRootId`, `claimPayloadJson`
두 필드를 추가한다. 새 관측은 검증/인가를 통과한 공개 입력의 모든
slot을 JSON으로 보존한다. source document/event의 원문은 바꾸지 않는다.
주장 root는 존재·조직·품목을 조회하지 않고 그대로 저장하므로 그 존재를
응답으로 노출하지 않는다. `rangeRootId`는 검증된 재고 연결이며 새
provisional 관측에서는 NULL이다.

인도 원천 대조는 주장 root와 실제 Dispatch의 root가 같은지, 원래
Work/item/customer/place, 구간·수량·시각이 일치하는지 계속 검사한다.
확정에는 기존 canonical 검증도 필요하다. 모두 통과한 같은 transaction에서
기존 운송 실물을 이동하고 `rangeRootId`를 채운다. 다른 root·미확인
root·미출고 보고가 정상 인도나 주문 이행을 만들지 않는다.

V28 FK와 적용된 migration checksum은 그대로다. V40은 기존 검증
참조의 nullable 여부만 바꾸며, CONFIRMED 관측은 NULL root 또는
주장과 다른 root를 가질 수 없도록 CHECK를 추가한다. Deliveries의
필수 root/FK는 바꾸지 않는다. legacy 관측에 원래 claim을 backfill하지
않는다. 이전 knowledge 시점의 조회는 이후 확정된 재고 참조를 노출하지
않으며, legacy의 기존 참조를 새 원문 증거로 주장하지 않는다.

## 검증 범위

focused backend는 opaque missing/foreign-org/other-item root의 원문
보존·동일한 대조 거부, 필수 입력·현재 grant 거부, rollback, 실제 두
transaction의 중복 요청, 정상/recall/expiry 인도, knowledge 시점 조회를
검사한다. 별도 PostgreSQL migration 검사는 V39 데이터와 FK/checksum을
보존한 V40 전환을 확인한다. 실제 HTTP T17 검사는 네 claim의 raw 입력,
인간 대조 의무·다음 행동·기한과 재고/인도 효과0을 함께 확인한다.

root 소유 `RecoveryHistoryMigrationPostgresTest`는 V38에서 latest까지
migration 수 1을 기대한다. V40 추가 후에는 V39 전용 검사임을 유지하도록
두 번째 Flyway target을 39로 고정해야 한다. 이 worker는 해당 runtime
소유 파일을 수정하지 않는다. full backend/full harness와 통합 후 최종
검증은 root 소유다. 실모델·Haiku·push·Grok는 실행하지 않는다.

## 이번 실행 결과

- focused backend의 서로 다른 11개 검사 PASS, FAIL/SKIP 0이다.
  최종 결과는 3차 실행의 통과 9개와 fixture를 보완한 5차 실행의
  통과 2개를 method별로 합친 것이다. 이전 실패 로그는 보존했다.
- Java 21.0.12.1과 이 worktree의 fresh package로 실행한 실제
  HTTP/Spring/PostgreSQL T17 suite는 34 PASS / 0 FAIL / 0 SKIP이다.
  falseclaim/unidentifiedscope는 HTTP 200/APPLIED, PROVISIONAL 관측,
  원래 opaque root, 인간 대조 의무·기한과 실물/기여 효과 0을 확인했다.
- T17 Python 31 PASS, 두 작성기의 byte 재현 2 PASS다. 257 assertion,
  이전 ledger, 원래 source descriptor와 문장 복원도 유지한다.
- `./verify prepare`는 41 case, 802 subcase, 24,260 assertion과 문제
  0개로 PREPARED다. full backend/full harness/실제 Haiku는 NOT_RUN이다.

원본 로그와 source/JAR hash, migration/원장 증거는
`evidence/product-claim-repair-summary.json`에 연결한다. 실행 디렉터리는
`.local/t17-source-pg-3e56d7d0-5861-438a-b6fe-96794617b7cb/`와
`.local/t17-product-claim/`이다. 임시 인증키와 source PG container 삭제,
실행 중 product/fixture 입력 hash의 무변경을 확인했다.

이 결과는 전체 257개 업무 oracle의 실제 모델 인수가 아니다.
root의 V39 검사 target 고정, 통합과 최종 결합 검증이 남는다.
