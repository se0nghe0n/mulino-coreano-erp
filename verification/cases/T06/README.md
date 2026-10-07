# D06의 시간·정정 계약

당시 알고 있던100을 현재98로 덮으면 과거 판정과 책임의 근거가
사라진다. 이 case는 동일 asOf의 다른 knownAt 조회, 원 revision 불변,
시간 정보와 미확인 상태의 구별을 검사한다.

`case.json`의10개 subcase와 `scenario.feature`의10개 시나리오는
행동·assertion을 일대일로 연결한다. `fixtures/`는 역할·위임·정책·
시계·실물 identity와 이전 사실이다. `payloads/`는 가상 원문이며
fixture의 실제 파일 SHA-256과 대조한다. fixture는 성공 결과를
seed하지 않는다. `author_contracts.py`는 세 evidence case의 정적
계약을 재생성하는 도구다. 서버·DB·worker를 구현하거나 실행하지 않는다.

| subcase | 독립 기대값 |
|---|---|
| known-at-correction | 당시100 BOX, 정정 후98 BOX. 원 revision1은 불변이다 |
| inconsistent-after-dispatch | 기출고100의98 정정은 과거 가짜 이동0, PENDING와 인간 대조 책임이다 |
| inclusive-deadline | 마감2026-10-06 00:00+09와 같은 사건은 포함 조건에서 충족이다 |
| exclusive-deadline | 같은 사건을 제외 끝점에서는 충족으로 처리하지 않는다 |
| date-only-range | 날짜 범위가 기한을 걸치면 정확한 도착 시각은 미확인이다 |
| four-states | MISSING·UNKNOWN·NOT_APPLICABLE·CONFLICT를 보존하고 knownQuantity0을 추론하지 않는다 |
| all-false-conflict | 확정 false가 있으므로 UNSATISFIED, conflict는 남는다 |
| all-true-conflict | 상충 조건이 남으므로 UNVERIFIED다 |
| any-true-conflict | 인정 가능한 true로 SATISFIED지만 conflict는 남는다 |
| not-unknown | UNKNOWN의 부정도 UNVERIFIED다 |

시간은 발생2026-10-05 09:00+09, 최초 기록2026-10-07 00:00Z,
정정 기록01:00Z를 구별한다. 발생/기록 시계는 실제 업무 시계다.
Maven 실행 시각은 별도다. 날짜-only source는 발생 instant를 입력하지
않고 날짜와 반개방 범위를 전달한다. 전체 fixture envelope의 evidence
timestamp는 upload/원천 metadata이며 실제 source의 DATE precision을
SECOND로 대체하지 않는다. 날짜-only 사례도 identity 대조·canonical 연결·
100 BOX 수령을 명시 실행하므로 미확인 이유는 수량 부재가 아니라
기한을 걸치는 발생 범위다.

독립 observer는 요청한 각 원천의 raw row, 실제 SQL·parameter·mapping
version·snapshot·완전한 scope를 반환한다. `evidence_revisions`에는
정정 전후 quantity/unit/revision을 직접 대조한다. 비교 배열은 observer가
stable row ID로 정렬한다. API projection을 DB 원 행으로 복사하지 않는다.
원장·배분·원 revision의 전후 비교에는 실제 두 snapshot이 필요하다.

실제 제품 adapter는 없다. schema/selector RED와 assertion 고정 표본의
PASS는 준비 검증이며 제품 API·PostgreSQL·규제·실모델 인수는 NOT_RUN이다.

# 영역 공통 인수

B2는 `feaca0af9673620eff9a5ac0f08a657ce14e9ccd`다. 소유 범위는
T06/T22/T24와 `cases/evidence` JUnit이다. 공통 schema·runner·registry·
catalog를 수정하지 않는다. 모든 assertion은 독립 catalog의 정확한
oracle/observation을 참조한다. observation 연결 개수와 의미 검토는
별개이며 worker 납품이 사용자 Step2 종료를 뜻하지 않는다.

`EvidenceAssertionContractTest`는 실제 case assertion을 읽어 고정
관찰과 mutant를 넣는다. 제품 상태를 계산·보관하는 fake는 없다.
수량·단위·원 revision·offset·알려진0 오추론·상충 누락·중복 수령·
담당/기한·추가 외부 요청·감사 rollback7축·legal hold 삭제·blob 부활·sentinel 발견을
실패로 만드는지 확인한다.

최신 `checks/validation-results.json`은3개 schema exit0, 전체 harness
149개 검사와 소유19개 검사 PASS, 실제 Gherkin selector의38개
NOT_IMPLEMENTED 실패와 skip0/exit1을 기록한다. T06 제품 scenarios는
10개 모두 NOT_RUN/exit2다. 각 RED 실행 보고서와 실제 Cucumber 발견
artifact를 보존한다. command output은 JSON 문자열로 저장하여 원 byte의
공백을 유지하고 SHA-256과 길이를 함께 기록한다.
