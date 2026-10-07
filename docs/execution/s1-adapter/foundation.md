# S1 실제 인수 adapter foundation

Step2의 `UnimplementedDriver`는 제품 실행 없이 RED 원인을 보존한다.
실제 제품 조회를 이 경로와 구분하기 위해 opt-in HTTP/JDBC adapter를
추가한다. 이 foundation은 전체 T/C/V/E 인수 완료를 주장하지 않는다.

## 실행 경로

- `./verify scenarios --actual <case.json>`은 실제 driver를 선택한다.
  기본 profile과 `contract-red`는 원래 미구현 driver를 유지한다.
- `./verify actual-s1`은 별도 S1 physical-read subset이다. synthetic
  V3/V5 fixture 설치, 서명한 HTTP `getInventory`·`getObject`, 독립
  JDBC physical rows를 검증한다. normative 41개 case registry를
  대체하거나 전체 T01 PASS를 만들지 않는다.
- Korean Gherkin `ScenarioGlue`도 같은 factory를 사용한다.
  `verification.driver=actual`과 `CaseFeatureTest` 명시 실행은 각
  scenario의 모든 기존 assertion을 요구한다. RED 기본값을 보존한다.

필수 환경 입력은 `ACTUAL_DISPOSABLE_DATABASE=true`,
`ACTUAL_BASE_URL`(loopback HTTP), `ACTUAL_BUILD_COMMIT`, `DB_URL`,
`DB_USERNAME`, `DB_PASSWORD`, `JWT_PRIVATE_KEY`(PKCS8 RSA),
`JWT_ISSUER`, `JWT_AUDIENCE`다. 인증 원문·private key·password는
receipt에 쓰지 않는다. backend는 동일한 public key를 사용한다.

`verification/actual/s1/fixture.json`의 issuer/audience는 각각
`synthetic-fixture-issuer`, `isolated-ontology`다. fixture 설치는
backend의 Flyway schema가 이미 적용된 disposable DB에만 수행한다.
DB 삭제·migration·backend 시작은 이 adapter의 기능이 아니다.

## fixture와 관찰

`FixtureInstaller`는 V3 identity와 V5 physical identity의 지원된
유형을 parameterized INSERT로 하나의 JDBC transaction에 설치한다.
alias는 매 실행 새 UUID를 만들며 실제 row가 commit된 뒤 반환한다.
인간·Agent identity, 서버 owner, grant, membership, capability를
실제 table에 저장한다. 범위를 좁힌 grant를 조직 범위로 확대하지
않는다. 미지원 alias·baseline·evidence·responsibility·baseRefs는
명시적으로 거절하며 일부를 생략하고 설치 성공을 반환하지 않는다.

`JdbcObservation`은 별도 read-only REPEATABLE_READ transaction에서
organization/item/lot scope와 asOf/knownAt을 적용해 physical segment를
직접 읽는다. projection SQL·parameters·실제 PostgreSQL snapshot·
raw rows·artifact를 기록한다. unknown source는 빈 배열로 바꾸지 않는다.

제품 `snapshotRevision`은 authorized projection content hash다.
PostgreSQL MVCC snapshot ID와 같지 않다. adapter가 해당 projection을
독립적으로 복원하지 못하는 현재 상태에서는 요청 snapshotRef를
복사하지 않고 `NOT_IMPLEMENTED`를 반환한다. native subset의 독립
DB receipt는 자신의 실제 MVCC snapshot으로 남는다.

## 미실행 경계

product write, async submission/terminal ACK, scheduler/process/barrier/
clock control, raw MCP/OData transport는 미구현이다. full T01 fixture의
work/evidence/eligibility/restriction과 다른 source도 아직 설치하지
않는다. S1 제품의 eligible/cumulative quantity UNKNOWN을 0 또는
fixture 기대값으로 대체하지 않는다. prerequisites 완료와 전체 gate는
별도 통합 확인이 필요하며 Main의 actual report도 gateComplete=false다.

## foundation check

`./mvnw -B -ntp -f verification/harness/pom.xml
-Dtest=ActualAdapterContractTest test`에서 4 tests, failures 0,
errors 0, skipped 0, BUILD SUCCESS를 확인했다. Java 21.0.5,
Maven 3.9.16이다. [JUnit XML](adapter-contract-tests.xml)은 adapter
선택·RS256 signature·disposable/loopback guard·unknown source 거부
검증이며 실제 backend/DB 실행 증거가 아니다. native runtime은 아직
통합 backend에서 실행하지 않았다. 이후 fixture timestamp mapping을
asOf/knownAt으로 보완했으므로 통합 실행에서 다시 확인해야 한다.
