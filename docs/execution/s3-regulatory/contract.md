# 기관 결과의 사실과 실행 허용을 분리한다

기준선은 c6c910b이며 regulatory 소유 파일만 작성한다. 작성 문서는
PREPARED다. 실제 접수 근거 없는 기록은 SUBMISSION_UNCONFIRMED다.
검증된 original JSON, event payload, canonical verification, source namespace와
기관 policy version이 같은 대상·실물 scope·LOT·기간·수량에 결합될 때만
SUBMITTED 또는 기관 결정을 기록한다. 외부 기관에 전송하지 않는다.

Policy는 immutable 설치 자료이며 ACTIVE, 기간, authority, namespace,
action, version이 있어야 한다. 테스트 policy는 fictional=true인 가상
정책이다. 현행 법적 허용을 주장하지 않는다. 운영 정책 확정은 별도다.

허용은 정확한 segment의 반개구간 [startQuantity,startQuantity+quantity)다.
겹친 ALLOWED의 합집합에서 current REJECTED/REVOKED 등의 범위를 빼고
실물 수량 이하로 제한한다. QC와 처분 조건은 별도이며 quality가
allowedRanges 교집합을 사용한다. 불명확 혼합과 다른 계보 scope는0이다.

보완·반려와 재작성·재제출은 이전 version을 참조한다. decision 정정은
previousVersionId를 명시한다. 시점·knownAt의 과거 근거는 보존한다.
표시 확인은 현재 specification/packaging version에 결합한다.

PREPARED와 정상 전체 허용은 잔여 대응 의무를 발명하지 않는다.
제출 미확인, 부분 허용, 보완/반려/철회에는 서로 겹치지 않는 REGULATORY_REVIEW와
인간 owner·nextAction·nextCheck가 남는다. 실제 확인된 접수는 미확인
접수 원인만 해소한다. 전체 current 허용의 증거가 있는 경우에만 부분
허용 또는 보완 원인의 의무를 RESOLVED로 바꾼다. OPEN leaf의 상대
범위를 실제 잔여 구간으로 투영한다. 철회로 다시 영향받은 부분은 새
source version의 원인으로 열고, 해소된 과거 root를 재개하지 않는다.
과거 기록을 지우거나 WAIVED로 감추지 않는다. 모두 같은 gateway
transaction에서 처리한다.

## 실행 검증

1d895f0에서 Java 21, Node 24.19.0과 고정 PostgreSQL 18 image로
`./mvnw -f backend/pom.xml -Dtest=S3RegulatoryActualTest test`를 실행했다.
9개 테스트가 failure/error/skip 없이 통과했고 exit code는 0이다.
`checks.json`에 해당 source와 compiled class hash, test 이름을 남겼다.
이전 6회 실패 로그도 `attempts/`에 보존했다.

8개는 canonical 근거 fixture를 쓰는 gateway/domain component 검증이다.
1개는 실제 attachEvidence → recordActivity → matchSourceIdentity →
linkCanonicalOccurrence → recordSubmission → recordRegulatoryDecision
경로로 실물 100 중 허용 30을 확인했다. 해당 공개 경로는 외부 원문을
받아 제출과 결정을 연결하며 canonical을 직접 seed하지 않는다.
겹친 허용 구간은 중복 합산하지 않고, 부분 허용 뒤 철회 시 OPEN
책임의 합계가 100으로 돌아가는 별도 회귀도 통과했다. 표시 VERIFIED
뒤 REJECTED와 표시 만료 boundary, 전체 허용 시 불필요한 review 의무가
없는 경우, 원문 소실과 권한/조직 불일치도 검증했다.

이 suite는 signed HTTP/MCP/native transport나 통합 quality/adapter의
전체 시나리오를 대신하지 않는다. 현행 법령 적합성, 실제 신고, paid
model과 배포는 실행하지 않았다. coordinator의 통합 gate는 별도다.
