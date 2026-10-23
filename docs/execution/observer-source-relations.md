# 독립 관찰의 실제 원천 관계 연결

T17과 T11 실제 실행에서 저장된 값과 observer 표시가 분리된 공백을
수정했다. mapping1.6.0은 같은 read-only REPEATABLE READ transaction에서
아래 실제 원천만 연결한다. 원래 raw 행의 수량·ID·개수는 그대로다.

- 수량 행이 비어 있어도 요청 조직·ITEM이 각각 하나이고 실제 TradeItems의
  baseUnit이 유일하게 존재하면 data.unit으로 읽는다. createdAt·recordedAt은
  knownAt 이하이고 SQL·bound values를 snapshot에 남긴다. 원천이 없거나
  외부 조직·미래 행이면 단위를 만들지 않으며 중복이면 관찰을 거부한다.
- movement.originId는 실제 sourceId다. INITIAL_BALANCE의 null 원천은
  그대로 null이며 도착 targetId를 출발 원천으로 바꾸지 않는다.
- restriction.reason은 같은 조직·segment·원래 command의 placeHold 결정
  원문이다. 해제 뒤에도 원래 사유를 보존하며 releaseReason은 별도 해제
  결정과 restrictionId를 검증한 원문이다.
- allocation의 FEFOExceptionReason은 자신의 실제 COMMITTED reserve 또는
  replace 명령 resultJson에서 읽는다. allocation ID·조직·segment·주문 line·
  단위·수량·시작 좌표와 knownAt이 모두 맞아야 한다. 다른 명령이나 범위의
  사유는 표시하지 않는다. eligibility나 예외 허용을 새로 판정하지 않는다.
- 평가 전 Work의 실제 PENDING transition도 별도 marker로 표시한다.
  assessmentBinding=UNASSESSED이며 assessment·condition·snapshot·capture
  ID가 없다. 실제 goal 관계와 조직·knownAt·asOf를 검증하며 후속 transition이
  pending을 해소하면 현재 marker는 사라진다. 기존 envelope는 수정하지
  않는다. 평가 chain head가 모호하면 관찰을 거부한다.

실제 PostgreSQL의 CSN 형태 독립 observer 검사23개와 기존 집중 회귀를
포함한173 executions가 실패·오류·skip0으로 통과했다. 이 중154개가
실제 JDBC 검사다. SharedCaptureObservationPostgresTest는 기존64개를
상속해 다시 실행하므로173개를 unique test 수로 표시하지 않는다.
새 class는 setup을 composition으로 재사용해 기존64개를 추가 상속하지
않았다. 원래 T09/T11/T17 case hash는 그대로이며 prepare12개도 통과했다.
생성한 observer DB container 부재를 확인했다.

증거는 `.local/resume-20261023/observer-source-relations/`의 proof.json,
JUnit·SQL 관찰·명령·원로그·정리 기록에 남긴다. baseline1dd7cecf와
검사한 working patch의 각 hash를 구별했다. 새 테스트 파일은 당시
untracked였으므로 별도 hash로 명시했다. 제품 write/인가/schema와
원래 oracle·fixture·grant·시계는 바꾸지 않았다.

이 결과는 제품의 정상 인도 승인, claim 대조, 조건 evaluator 또는 원자성
인수 완료가 아니다. claim 원천의 typed grain, 주문 line 관계, condition
진단 의미, 책임 종류와 실제 평가/recall 생성 등은 별도 미완료로 유지한다.
실제 Haiku 재실행은 아직 NOT_RUN이며 Task·Step은 OPEN이다.

## 현재 주문 line의 끝점 연결

후속 mapping1.7.0은 sales Orders의 원래 header 행을 유지하고 실제
currentRevision → OrderRevisions.revisionNumber → OrderLines.revisionId를
연결한다. 조직·Work·customer·요청 ITEM과 knownAt/asOf를 검증한 실제
currentLines와 currentRevisionId를 제공한다. 현재 line이 정확히 하나일
때만 그 line의 deliveryEndpoint를 header에 표시한다. 과거 revision이나
다른 조직·Work·ITEM·customer·미래 행을 대체 원천으로 쓰지 않는다.

원천이 없거나 revision이 모호하면 orderLineBinding으로 관계 상태를
표시하고 끝점을 만들지 않는다. 여러 현재 line은 모두 보존하고 단일
끝점을 임의 선택하지 않는다. purchase Order의 의미는 바꾸지 않았다.

이 변경의 새 반례13개를 포함한 집중186 executions가 실패·오류·skip0으로
통과했다. 새 relation class36개와 기존 base64개·capture67개·engine19개다.
상속 반복64개를 포함하며 unique test 수나 native 인수로 표시하지 않는다.
원로그·source/class hash·원본 case 보존·생성 DB 정리는
`.local/resume-20261023/observer-sales-order-lines/`에 남긴다. 이후 통합
prepare와 실제 Haiku 인수는 별도로 수행하며 Task·Step은 OPEN이다.
