# S2 판정 구현과 검증 기록

문서 수나 자식 Work 종료를 목표의 수량 기여로 합산하면 실제 이행을
과대평가한다. 기존 Work·Goal·Assessment ID를 유지하며 고정된 정의,
정책과 평가 입력을 불변 snapshot으로 남긴다. Step3 S2의 평가 subtask
범위다. Task branch 통합과 전체 S2 인수는 coordinator가 판정한다.

- `TypedPredicateEvaluator`는 bounded typed predicate, verified physical
  scope 중복 제거, actual state, 시간 구간과 UNKNOWN/CONFLICT를
  계산한다. 누락·미검증 입력을 수량0으로 치환하지 않는다.
- `AssessmentFactProvider`는 실제 조직·knownAt 범위 CQN에서 canonical,
  verification, claim, event, inbox, document, segment와 Work credit을
  읽는다. 품목·LOT·장소 scope와 명시된 기간 끝점을 적용한다.
  disjoint credit만 기여로 사용하며 shared activity나 자식 종료 상태를
  수량으로 승격하지 않는다. 원천의 정정·충돌 revision도 보존한다.
- `AssessmentService`는 목표의 고정 정의·조건·evaluator·정책을
  검증한다. Assessment와 InputSnapshots, Work pending 표시를 같은
  transaction에 저장한다. 미지원 고정 버전은 HELD다. 고정 clock의
  동일 기록 시각에서도 previousAssessmentId 체인으로 현재 판정을
  찾으며 이전 판정의 납기 위반을 보존한다.
- `requireFulfilled`는 현재 입력 snapshot과 policy를 다시 대조한다.
  imported S1 판정이나 stale 입력은 이행 종료 근거가 아니다.
  정정 hook은 과거 판정을 바꾸지 않고 pending 표시와 실제 원천의
  후속 책임을 같은 transaction에 남긴다.

## 실행 증거

baseline은 `40a240c3886d3316ba1fd686e9b44bfa1ba6ec2d`다.
`focused-e270015.json`과 압축 log는 OWN `e270015`에서 실행한
20개 focused test의 PASS를 기록한다. 실패·오류·skip은0이다.
실제 disposable PostgreSQL의 Spring+CQN 판정2개와 pure test18개다.
업무 clock은 `2026-10-08T00:10:00Z`로 고정했다. Maven에 필요한
공유 srv model import3개를 로컬에서만 추가했으며 coordinator가
최종 통합에서 소유한다.

실제 DB assertion은 같은 canonical60에 연결된 문서2개를60으로
유지하고, 독립 SQL 합계와 대조한다. 목표100은 UNSATISFIED다.
늦은40을 추가하면 새 판정은 SATISFIED이면서 deadlineViolated를
유지한다. 이전 판정 UPDATE와 snapshot DELETE는 DB가 거부한다.
미확인 입력은 UNVERIFIED이며 future-v9 고정 evaluator는 HELD다.

`2140de0`은 추가 review 반례로 명시된 기간의 시작·끝 포함 여부를
반영한다. 실제 PG fixture는 기존 목표를 수정하지 않고 새 불변 목표
버전을 만든다. 시작60과 끝40에 대해 [시작,끝)은60, (시작,끝]은40,
[시작,끝]은100이어야 한다. 공개 reviseGoal의 slot 정규화는 Work
owner의 별도 통합 test가 검증한다.

`focused-2140de0.json`과 압축 log에 추가 경계 suite의 실제 실행을
기록했다. PG3개·pure19개, 총22개가 PASS이며 실패·오류·skip은0이다.
실행 command, test fixture SHA256, 개별 assertion 이름과 clock을
manifest에 남겼다.

## 검증 경계

위 실행은 service+CQN/PG와 pure test의 해당 assertion만 PASS다.
전체 T09/T12/T21/T26/C2/C5나 public HTTP/MCP 결합의 완료를
뜻하지 않는다. Work·evidence·책임 결합과 public boundary 경로는
coordinator의 aggregate 검증 증거를 따른다. SELL 등 action eligibility,
S3/S4 receipt adapter, 외부 client/model과 BTP는 NOT_RUN이다.
현재 물리 상태에 예약 물량을 다시 더하지 않으며 예약 제외 해석이나
지원하지 않는 action은 미확인으로 남긴다. 구간의 관측 공백이나
현재 상태만으로 미래 THROUGHOUT 충족을 추론하지 않는다.
