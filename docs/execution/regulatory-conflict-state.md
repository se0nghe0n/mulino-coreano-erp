# 규제 결정의 물리 범위 상충을 보존한다

기준은 `b507aa8a66c5f7cf967bb6607833425c86550512`이다. 구현 계획
§3의 UNKNOWN/CONFLICT 구별과 §6의 조건별 eligibility 계약을 따른다.

`RegulatoryEligibility`는 기존 admission을 통과한 현재 결정의 물리
범위를 요청 범위에 투영·절단한 뒤 ALLOWED와 부정 결정의 교집합이
양수이면 CONFLICT를 반환한다. 같은 끝점에 닿거나 서로 떨어진 범위는
상충이 아니다. 기존 차집합과 eligibleQuantity, evidenceRefs,
nextValidityBoundary, unknowns 계산은 유지한다. 상충 범위는 적격량에
들어가지 않으며, 이미 증명된 안전한 나머지 범위를 새로 없애지 않는다.

현재 공개 decision enum에서 제출 상태를 유지하는 부정 결정은
REVOKED이다. 독립 절차의 ALLOWED/REVOKED 원본으로 상충을 입증한다.
REJECTED는 절차 head를 바꾸므로 기존 admission에 따라 현재 허용/부정
범위로 채택되지 않는다. DENIED 같은 새 입력 alias를 만들지 않는다.
만료·미래·다른 조직/정책·superseded 결정, 현재 원본 검증이나 필수
label이 없는 결정은 상충을 증명하지 않는다. deny-only/무증거 UNKNOWN
의 기존 의미도 바꾸지 않는다.

`QualityEligibility`의 기존 REGULATORY 조건은 `reg.state()`를 그대로
전달하므로 수정하지 않는다. 실제 reserve는 기존 안전 범위 교집합을
그대로 검사한다. 중첩 구간 reserve는 REJECTED/할당 0이며, 안전한
구간은 기존 권한과 다른 조건이 모두 증명된 경우에만 APPLIED이다.

## 독립 PG 검증

`RegulatoryConflictPostgresTest`는 자기 PostgreSQL/key/blob만 소유한다.
기존 107개 테스트를 상속하거나 다른 클래스의 static host/teardown을
빌리지 않는다. 새 synthetic fixture는 실제 attachEvidence,
recordActivity, matchSourceIdentity, linkCanonicalOccurrence,
prepareRegulatoryProcedure, recordSubmission, recordRegulatoryDecision을
거친다. IdentityAuthorization/policy/source admission은 mock하지 않는다.
고정 시계는 이 독립 fixture의 ExecutionClock이며 host now로 바꾸지 않는다.

검사는 부분/전체 중첩, 비중첩·접점, deny-only/무증거, supersession과
원본 불변, validFrom/배타적 validUntil, 실제 나중 기록과 knownAt,
원본 blob 유실, REJECTED 절차 head, 다른 만료 정책/조직,
요청 범위 clipping·무관한 물리 범위, 필수 label 누락,
현재 getEvidence 권한 철회, evidenceRefs, reserve 효과를 확인한다.
기존 `S4RegulatoryProjectionTest`는 분할된 물리 범위 투영을 회귀 확인한다.

원래 결함 RED 2건(UNKNOWN/ALLOWED 대신 CONFLICT 필요)과 개발 중 실패를
`.local/regulatory-conflict-state`에 보존한다. 첫 knownAt fixture는 새
source를 미래 시각에 생성하다 EVIDENCE_UNVERIFIED가 되어 실패했다.
최종 검사는 source를 먼저 만들고 실제 결정 명령만 나중에 기록해
knownAt을 검사한다. 별도 Work-subject 원본 실험도 canonical 연결에서
HELD EVIDENCE_UNVERIFIED로 실패했다. 해당 시도 소스/SQL/로그를 보존하며
제품 source 규칙을 우회하거나 성공 테스트로 세지 않는다.

## 남은 경계

Work-bound 규제 원본의 유효 chain에 대한 추가 PG 반례는 이 Subtask에서
입증하지 못했다. 현재 getEvidence 철회/원본 유실의 fail-closed는 실제
SEGMENT-bound 원본으로 검증한다. 기존 WorkSupplyRead의 ALLOWED-only 관계와
목표 predicate의 REGULATORY typed condition alias 공백은 바꾸지 않는다.
따라서 이 변경은 T17 목표 판정이나 native 시나리오 PASS의 증거가 아니다.
원래 case/oracle/grant/clock/schema/migration과 QualityEligibility는
수정하지 않았다. Task/Step는 OPEN이며 primary가 통합·최종 검증한다.

## 실행 결과

최종 집중 실행은 실제 PG 14 PASS와 기존 projection 1 PASS, 총 15 PASS,
0 FAIL/ERROR/SKIP이다. locked `npm ci --prefix backend`와 이 tree의
Java21 `-DskipTests package`도 exit 0이다. Java는 Homebrew 21.0.12.1,
Node는 24.16.0이며 자체 CDS 생성과 JAR를 사용한다. 이전 실행의 원래
RED 2건, knownAt fixture 실패 1건, Work-subject 실험 실패 1건을 지우지
않았다. 전체 backend/harness/native actual은 이 범위에서 실행하지 않는다.

원로그/JUnit/실제 SQL row는 `.local/regulatory-conflict-state` 아래
`red`, `green`, `boundary-proof`, `work-source-proof`, `final-focused`에
있다. `build`는 npm/package 명령·exit와 원로그다. source/class/JAR/log
SHA256, 원래 case bytes 보존, exact own host/key/blob 부재 확인은 같은
디렉터리의 `evidence.json`과 `hashes.json`에 기록한다.
