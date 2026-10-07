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
현재 PG9 재실행 대기 상태이며 gateway acceptance PASS로 표시하지 않는다.
