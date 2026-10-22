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
