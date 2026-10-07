# S3 품질 검증 기록

실제 PG/gateway 검증이 필요한 조건이므로 pure range 결과만으로 완료를
선언하지 않는다. 외부 기관·실 ERP·유료 모델 호출은 실행하지 않았다.

## 검증 환경

Java 21, Node 24, Maven wrapper와 disposable PostgreSQL 18.6을 쓴다.
테스트 원본 blob root는 private 0700 임시 디렉터리다. source JSON bytes,
Document, Event, Claim을 만든 뒤 public attachEvidence, recordActivity,
matchSourceIdentity, linkCanonicalOccurrence를 호출한다. VERIFIED 결과를
SQL로 seed하지 않는다. Work는 createDraft와 activateWork로 만든다.

PostgreSQL image는
`postgres@sha256:4ef4dbc939d61acea57712655ddb4b4ab27419c913f94cca0cd57cb3ea3c2280`다.

실행 명령은 root에서 다음과 같다.

```sh
./mvnw -f backend/pom.xml -Dtest=QualityPostgresTest,QualityRangesTest test
```

## 결과와 보존된 실패

QualityRangesTest의 decimal 교집합·독립 hold·범위 union·full closed
시간 경계 4개 검사는 PASS다. fresh DB의 V20/V21 migration 적용도 PASS다.

- 첫 PG 실행: SourceProfiles revision0이 revision>0 제약에 실패했다.
- 다음 compile: 동료 테스트의 ambiguous lambda가 실패했다. owner 수정에
  동기화했다.
- 다음 Spring context: ReceiptRepository final의 CGLIB proxy 생성이
  실패했다. receipt owner의 수정에 동기화했다.
- fourth: fixture helper가 null operation을 Set.of.contains에 전달했다.
  fixture null guard를 추가했다.
- fifth: 새 post-lock scope reread가 lambda capture를 final이 아니게 했다.
  final physicalId로 고쳤다.
- sixth: main/test compile와 pure4는 PASS다. PG9는 imported S1 Work의
  immutable trigger에 실패했다. final oracle을 만들지 않고 public
  lifecycle Work 생성으로 fixture를 고쳤다.

로그는 `/tmp/mulino-s3-quality-tests-{fourth,fifth,sixth}.log`에 보존한다.
- seventh: WorkLifecycle이 지원하는 definition-v1 대신 fixture 전용
  version을 써서 public creation이 HELD됐다. 지원 version으로 고쳤다.
- eighth: 전체 goal definition 검증이 role-only decision capability와
  아직 설치하지 않은 S4 reserveQuantity를 거부했다. 실제 handler만
  definition에 설치하고 creation 전 validator VALID 검사를 추가했다.

- ninth: pure4와 actual commercial role-denial1은 PASS다. 나머지8은
  scoped basis ACTIVE가 기존 CONFIRMED/REVOKED state 제약에 걸렸다.
  V20에 legacy와 scoped state를 분리하고 scoped 시간 구간의 closed
  endpoint도 명시했다. Work·source reconciliation·기관 gateway는 통과했다.

tenth source `469aa4d7`에서 fresh V1–V21 migration, main/test compile,
pure4와 actual PG/gateway9 모두 PASS다. 총13, failure0, error0다.
`/tmp/mulino-s3-quality-tests-tenth.log`에 원문 결과가 있다.

추가 actual decimal 교집합 검사는 별도 실행 대기다. 그 외 13개 결과를
소급 변경하지 않는다. 전체 Step 통합과 native adapter 검증은 coordinator가
별도 source에서 실행한다.
