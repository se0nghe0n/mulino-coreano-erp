# T01 원천 연결 구현과 실제 검증

## 범위

사용자 Step 2 T01을 시작 commit `5861058e`에서 구현한다.
12개 subcase와 125개 assertion을 유지한다. 전체 ontology Task 완료와는
구별한다. 재위임, 모델 호출, BTP, push, PR, 외부 메시지는 실행하지 않는다.

## 변경

- T01의 명시적 sourceFacts와 원문을 읽고 실행 중인 서버의
  `recordActivity`, `matchSourceIdentity`, `linkCanonicalOccurrence`로
  canonical을 생성한다. VERIFIED 행이나 판매 적격60을 직접 seed하지 않는다.
- 실제 수령60+40, 개별 QC·고객·처분 근거와 기관 허용, 보류40을 연결한다.
  현재 보유를 누적 도착량에 복사하지 않는다. 이전 baseline만 있는 fixture의
  미지원 설명은 그대로 남긴다.
- Work와 LOT의 직접 및 SubjectLinks 관계로 조회 범위를 제한한다.
  getAssessment의 SELL 설명은 실제 QualityEligibility 결과에서 가져온다.
- 불필요한 고객 selector를 제거해 같은 업무 snapshot을 사용한다.
  관계 assertion은 존재하지 않는 `type` 대신 실제 relationDefinitionId를
  대조한다. 공급 assertion은 전체 재고 segmentIds와 공급 가능
  eligibleSegmentIds를 구별한다. 근거 집합은 실제 canonical·원문·정책을
  모두 단언한다.

## 검증 진행

실제 실행 결과와 clean commit은 실행 후 기록한다. 아직 T01 완료를
주장하지 않는다. Java21, npm ci, Docker PostgreSQL을 사용한다.

## 첫 실제 PASS와 강화 검사

`234d242e152953bca32482f0e423351e7caf5e66`의 clean 실행에서
T01 12/12 subcase, 125/125 assertion이 통과했다. 실행 action은60개이고
모든 subcase의 runtimeComplete가 true다. profile의 gateComplete=false는
별도 선행 profile 인수가 없다는 뜻이며 전체 시스템 인수로 확대하지 않는다.

증거는 아래 경로다.

`verification/harness/target/evidence/actual-scenarios-run-bbdca81a-1839-4d1a-b31f-8c8ba03cd073/scenarios.json`

실행 중 독립 SQL 대조에서 완료된7개 fixture 조직의 수령100, 보류40,
QC/CUSTOMER/COMMERCIAL 각각60, 원문이 있는 VERIFIED canonical 연결을
확인했다(`/tmp/t01-source-audit.json`). 전체12개 SQL 대조를 시도한 시점에는
runner가 이미 DB를 정리했으므로 이를12개 PASS로 기록하지 않는다.

Work·Customer의 ObjectRelations 끝점은 기존 schema whitelist에 없어
`V37__work_customer_read_relations.sql`로 같은 조직의 실제 끝점만 허용했다.
정의·cardinality·cycle 검사는 유지한다. 새 조회는 발행된 관계 정의의
SUBJECT_OF/CUSTOMER를 따라가며 다른 LOT와 item의 정보를 합치지 않는다.
추가 회귀와 최종 clean 재실행 결과는 아래에 기록한다.

원천 helper는 setup artifact의 sourceFactEvidence에 실제 HTTP command
결과를 남긴다. 신규 verify_source_report.py는 12개 fixture의 원문→claim과
원문→canonical 연결, 대조·발행 응답, LOT 물량, 의무 tuple을 독립 재검사한다.
새로 추가한 고객 영향 회귀는 다른 LOT의 고객을 제외하고, 실제 Work 관계가
있는 QuantitySegment 명사 조회와 존재하지 않는 고객 끝점 거부를 검증한다.
