# T17 첫 실패 입력 교정

기준은 `20c2666848b89b3ab7b4a1b1598e58cb408d13c4`다.
원래 257개 업무 assertion, 사업 actor/grant, 수량, 날짜, 별칭과
기존 15개 public response pointer 교정을 보존한다. 전체 Task와
Step는 OPEN이다. 이 문서는 실제 시나리오 인수 결과가 아니다.

## 새로 작성한 입력

`author_first_failure_inputs.py`는 기존 public/LOT/source/profile
변환 다음에 적용한다. `first-failure-input-delta.json`에 원본과
변경을 남기며 전체 역변환을 검사한다. public generator도 이
추가 단계를 합성한다. 이전 public ledger는 바꾸지 않는다.

- 원래 창고 dispatch가 있는 8개 fixture에 별도 `dispatchInputs`와
  action이 DISPATCH인 fictional policy를 추가한다. 각 입력은 원래
  condition pointer와 원문을 함께 보존한다. 원래 dispatch의 예약이
  명시한 A만 준비하며 B40에 DISPATCH 허용을 추가하지 않는다. `authoredBy`는
  `coordinator-selected-fixture`이며 `operatingApprovalClaimed`와
  `originalAuthorityClaimed`는 false다. SELL만으로 DISPATCH가 허용됐다고
  소급 해석하지 않는다.
- `T17SourceFacts`는 이 명시 입력만 별도 원문→claim→review→canonical→
  typed decision으로 보낸다. 품질 category별 DISPATCH 처분 근거와
  별도 DISPATCH regulatory procedure/decision/label을 기록한다.
  UNKNOWN/CONFLICT/DENIED에는 양의 허용 근거를 만들지 않는다.
- 보류 요청 8개에는 원래 요청과 증거 alias를 보존한 원문,
  category/action/범위/Work와 명시적인 새 유효기간을 추가한다.
  새 기간은 원래 fixture 시계 `2026-10-07T09:00:00Z`부터 해당 물량의
  QC condition 끝점 `2026-10-31T23:59:59Z`까지로 선택한다. 새 transit
  물량이 생기기 전으로 source 시각을 소급하지 않는다.
  이 선택은 원래 보류 기간이나 운영 승인으로 표시하지 않는다.
  실제 업무 호출 actor는 그대로 qc/admin이다. late-hold는 공개
  getObject 조회의 실제 `data.ID`로 transit segment를 결합한다.
  원래 request JSON은 별도 원문으로 보존하며 공통 encoder의
  identity 목록을 넓히지 않는다.

## 남은 경계

- `placeHold`는 command 권한 외에 policy의 category capability도
  현재 호출자에게 요구한다. 원래 qc에는 `decideQuality`, admin에는
  `decideRecallRestriction`가 없다. t17Source는 QC 처분 근거를
  작성할 수 있지만 qc/admin의 현재 grant를 대신하지 못한다.
  올바른 canonical source가 있어도 이 경계는 남는다. 사업 actor의
  grant 또는 policy를 넓히지 않는다. 보류 APPLIED를 주장하지 않는다.
- 7개 createSalesOrder에 `dueAt` 값이 원래부터 없다. 대상은
  reserve-pick-dispatch/second-order, replacement-no-revival/order,
  eligibility 5개/order다. 원래 날짜를 다른 주문의 날짜나 현재 날짜로
  추론하지 않는다. ledger의 `unresolvedDueAt`에 그대로 남긴다.
- conditions SQL observer와 raw observation projection 공백은 별도다.
  기존 assertion을 삭제·면제하거나 TYPE_INVALID를 업무 거부의
  성공으로 바꾸지 않는다. 실제 Haiku dynamic Workflow 시나리오와
  model client acceptance는 실행하지 않는다.

## 검증 명령

```sh
npm ci --prefix backend
python3 -m unittest discover -s verification/cases/T17 -p 'test_*.py'
./mvnw -B -ntp -f verification/harness/pom.xml -Dtest=T17SourceFactsTest test
python3 verification/cases/T17/run_source_pg_test.py
./verify prepare
```

PG runner는 coordinator 소유 원문 그대로다. 새 backend build와
독립 PostgreSQL/Spring/HTTP 검증을 사용하고 생성한 컨테이너와
임시 인증키를 정리한다. 세부 명령·source/JAR hash·응답은 반환하는
`.local` 경로에서 확인한다. 모든 확인은 한정된 입력/인가 검사다.

## 이번 변경의 확인 결과

Python 보존 검사 21개, 전체 harness 613개 PASS/PG opt-in 17개 skip,
`prepare` 41 case/802 subcase/24,260 assertion의 구조 검사를 통과했다.
별도 fresh build의 HTTP/Spring/PostgreSQL 검사 17개도 모두 통과했다.
SELL-only의 DISPATCH 거부와 명시 DISPATCH의 30 BOX 출고,
UNKNOWN/DENIED/CONFLICT 및 잘못된 범위·기간·타 조직 증거의 차단,
두 보류 source chain 뒤 원래 actor의 FORBIDDEN을 확인했다.
이는 원래 257개 업무 oracle의 실제 시나리오 인수가 아니다.

명령, 로그와 SHA-256, source matrix, 이전 실패 및 남은 경계는
[evidence/first-failure-input-repair-summary.json](evidence/first-failure-input-repair-summary.json)에 남겼다.
replacement의 release-hold에는 reason과 operation별 canonical 원문이
아직 없으며, 앞선 dueAt 및 category 권한 경계가 먼저 남아 있다.
