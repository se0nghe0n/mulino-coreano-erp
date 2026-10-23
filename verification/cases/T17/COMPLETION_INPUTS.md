# T17 후속 synthetic 입력 교정

기준은 `2348dfd312579412874398937773f6f4c0f2b96f`다. root의
observer 통합은 가져오지 않았다. 전체 Task와 Step는 OPEN이다.
원래 257개 업무 assertion byte block, 원래 scenario 문장과 순서,
이전 15개 pointer ledger 및 first-failure ledger는 바꾸지 않는다.

## 새로 승인된 fixture precondition

`author_completion_inputs.py`와 `completion-input-delta.json`이
이번에 승인된 변경만 이전 layer 다음에 적용한다. public generator와
각 단계의 strict inverse 검사도 합성한다. 모든 새 선택은
`authoredBy=coordinator-selected-fixture`,
`originalAuthorityClaimed=false`, `operatingApprovalClaimed=false`다.

- 원래 dueAt이 없던 7개 주문은 원래 fixture 시계
  `2026-10-07T09:00:00Z`의 1시간 뒤인 `2026-10-07T10:00:00Z`를
  새 기한으로 선택한다. 원래 조건과 LOT의 가장 이른 만료보다
  빠른지 검사한다. 원래 있던 기한은 그대로 둔다.
- 두 주문에 없던 packageTerms도 공개 계약의 필수 입력이다.
  원래 TradeItem의 `packagingVersion=sealed-box-v1`을 참조한
  새 synthetic 거래 조건을 작성한다. 원래 조건이었다고 쓰지 않는다.
- positive hold가 있는 8개 fixture 중 7개의 qc에 `decideQuality`,
  나머지 late recall의 admin에 `decideRecallRestriction`만 추가한다. 원래
  subject·issuer·audience·organization·grant delegator·scope·기간·
  revision과 기존 capability 순서는 보존한다. 새 capability는
  원래 ITEM scope 안에서만 작동하며 all-of 해석도 유지한다.
  fixture installer의 기존 delegation 규칙에 따라 synthetic root
  delegator도 이 추가 capability를 위임할 근거를 받는다.
  실제 운영 grant/policy는 변경하지 않는다.
- replacement proof는 원래 요청·증거 descriptor를 보존한 새
  `witnessKind=replacement-proof` JSON이다. A20, B20, 원래 예약 ID,
  실제 주문 Work와 sales-line ID를 연결한다. 제출은 실행 전 proposal이며
  `committedEffectClaimed=false`다. attach→event/claim→match→canonical
  공개 경로를 사용하고 그 문서를 replaceAllocation에 전달한다.
- releaseHold도 원래 evidence descriptor와 restriction ID를 보존한
  operation별 원문을 같은 공개 경로로 작성한다. reason은 새로 정한
  “원래 QC 검토 보류를 해제하고 대체 예약은 유지한다”다.
  원래 요청의 reason이었다고 주장하지 않는다.
- reserve 뒤의 physical segment, hold로 정지된 allocation, 새 replacement
  allocation은 서로 다른 revision을 가진다. 각 대상의 공개 조회 또는
  생성 응답으로 expectedRevision을 결합한다. 원래 잘못된 참조는
  ledger의 before에 보존한다.

## 원래 경계와 검증 범위

별도 negative copy는 원래 actor/grant를 복원해 같은 canonical hold
source에도 FORBIDDEN과 제한 행 0을 확인한다. missing dueAt copy와
원래의 가짜 proof, 잘못된 Work/scope의 원문도 업무 효과를 만들 수
없어야 한다. 새 category capability가 다른 command capability를
대신하거나, ITEM 일치로 잘못된 WORK/PLACE 차원을 우회하면 실패다.

이 검증은 결정적 HTTP/Spring/PostgreSQL source/input 검사다.
Haiku 5.5 native dynamic Workflow 시나리오와 실제 model-client
인수는 실행하지 않는다. observer와 product의 새로운 첫 실패는
root owner에게 증거와 함께 반환한다.

```sh
python3 -m unittest discover -s verification/cases/T17 -p 'test_*.py'
python3 verification/cases/T17/run_source_pg_test.py
./mvnw -B -ntp -f verification/harness/pom.xml test
./verify prepare
```

coordinator 소유 `run_source_pg_test.py`는 수정하지 않는다.

## 실행 결과

2026-10-23 검사 결과는 Python 25 PASS, fresh HTTP/Spring/PG 22 PASS다.
전체 harness는 613 PASS와 PG opt-in 22 SKIP이며, 별도 PG 실행에서
그 22개를 모두 검증했다. prepare는 41 cases, 802 subcases,
24,260 assertions를 준비했고 preparationProblems는 0이다.

새 주문 7건, QC/recall hold와 replacement→releaseHold→releaseAllocation이
실제 APPLIED에 도달했다. 원래 grant copy의 hold는 FORBIDDEN이다.
missing dueAt, 원래 미검증 proof, 다른 Work·segment·organization 및
불일치 grant 차원은 효과를 만들지 못했다. 중간 실패에서 드러난
aggregate revision 참조를 고친 후 fresh runner를 다시 통과했다.

[증거 요약](evidence/completion-input-repair-summary.json)에 정확한 명령,
로그 경로와 SHA-256, source snapshot, JAR hash, 원래/새 grant와 날짜
목록을 기록했다. 원래 257개 assertion byte block, 두 이전 ledger,
원래 feature 행의 복원과 generator 재현을 확인했다.

prepare의 기존 recordDelivery slot 경고와 Allocation 조회 목록 경고는
남는다. Allocation 조회 자체는 실제 HTTP/PG에서 통과했다. 공통
검사기·observer 통합과 전체 업무 oracle 실행은 root owner의 남은
범위다. 이 결과로 FullTask/Step 또는 실제 모델 인수를 닫지 않는다.
