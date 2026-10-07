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
`https://mulino-native.invalid`, `isolated-ontology`다. fixture 설치는
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

## disposable native runner

`./verify actual-s1 [새 evidence directory]`는 harness를 compile한 뒤
`verification/actual/s1/run.sh`를 실행한다. backend JAR는 현재 source에서
이미 build되어 있어야 한다. runner는 backend를 다시 build하지 않는다.

runner는 `verification/platform/versions.json`의 pinned PostgreSQL image로
새 container와 anonymous volume을 만들고 ephemeral loopback port를
사용한다. 새 RSA key와 disposable DB password를 임시 directory에만
둔다. 실행 JAR는 임시 directory에 복사하고 원본과 hash가 같은지
확인한다. backend는 `server.port=0`으로 시작하며 실제 할당 port에서
HTTP 인증 응답을 확인한 후 native HTTP/JDBC 인수를 실행한다.

native fixture는 authority wall clock을 한 번 캡처해 membership와
grant를 그 시점의 60초 전부터 15분 뒤까지 유효하게 만든다. 고정
physical `asOf`·`knownAt`은 바꾸지 않는다. template hash와 실제 derived
fixture hash, authority wall clock과 유효 기간을 모두 receipt에 남긴다.
이는 새 synthetic native fixture만의 처리이며 normative fixture나
backend 현재 인가 규칙을 바꾸지 않는다.

`run-receipt.json`은 clean commit, 모든 tracked source input의 실행
전·후 hash와 dirty 상태, 실제 실행한 복사 JAR hash, fixture hash,
HTTP response·독립 JDBC rows artifact hash, cleanup 결과를 기록한다.
source/JAR drift 또는 cleanup 실패는 native PASS가 있어도 FAIL이다.
미지원 단계는 NOT_RUN이지만 이미 관찰한 업무 위반을 숨기지 않는다.

trap은 자신이 만든 backend PID, container와 그 anonymous volume,
임시 key directory만 정리한다. raw HTTP·DB·runtime log와 receipt는
남긴다. token·private key·password를 evidence에 복사하지 않는다.
실제 TCP HTTP 실행은 통합 backend에서 root가 수행할 미실행 check다.

추가 check에서 `javac --release 21`로 변경 source와 tests를 compile하고
JUnit Platform launcher로 adapter contract 6개를 실행해 모두 통과했다.
기존 4개와 현재 authority clock·FAIL 우선순위 2개다. shell syntax와
Python compile이 통과했고, 임시 Git fixture의 source drift가 native
PASS를 FAIL로 바꾸는 receipt check도 통과했다. Maven slot은 사용하지
않았다. 이 결과는 실제 PostgreSQL/HTTP 실행 성공을 뜻하지 않는다.


## native issuer URL 수정

설치된 Spring Security 7.1.1의 `JwtClaimAccessor.getIssuer()`는
`java.net.URL`을 반환하고 `getClaimAsURL("iss")`를 호출한다.
실제 `Jwt.withTokenValue(...).claim("iss",...).build().getIssuer()`
probe에서 `synthetic-fixture-issuer`는 URL 변환
`IllegalArgumentException`을 냈고 `https://mulino-native.invalid`는
그 URL을 반환했다. native fixture와 runner를 후자로 맞췄다.
`.invalid` 주소로 DNS나 HTTP 요청을 수행하지 않는다. 기존 Step2
normative fixture issuer 추상 계약은 바꾸지 않는다.

수정 후 focused Maven adapter suite는 7 tests, failures 0, errors 0,
skipped 0, BUILD SUCCESS(2.727초)를 확인했다.
[JUnit XML](issuer-contract-tests.xml)과 [probe 관찰](issuer-probe.txt)을
남긴다. 실제 backend TCP 인수는 여전히 root 통합 실행 대상이다.


backend의 `mulino.evidence.blob-root`도 자신이 생성한 0700 fixture
안의 `blobs`로 지정한다. BlobStore가 새 0700 directory를 만들며
trap의 fixture cleanup으로 blob와 key가 함께 정리된다. 기존 기본
`/tmp/mulino-evidence-blobs`는 확인하거나 수정하지 않는다.

## 실제 fixture dependency 실패 수정

root native 실행은 V5의 TradeItem 필수 specificationVersionId를
누락해 SQLSTATE 23502로 실패했다. V5의 후반 ALTER가 specification과
packaging version을 필수로 만들고 같은 product의 version만 연결한다.
기존 fixture의 자동 product 생성과 비어 있는 version 연결은 이 계약을
만족하지 못했다.

native fixture에 Product·SpecificationVersion·PackagingVersion alias와
TradeItem의 explicit dependency를 추가했다. installer는 product 다음
두 immutable version을 설치하고 authored synthetic content의 SHA256을
저장한 뒤 TradeItem에 실제 UUID를 연결한다. native HTTP assertion은
product/version 연결과 실제 응답의 contentHash를 확인한다. 원래
normative case 기대값이나 oracle를 읽어 row를 만들지 않는다.

fixture 실패 메시지에는 underlying SQL exception class, SQLSTATE와
서버 table/constraint/column identifier만 남긴다. 서버 message·detail·
SQL statement·parameter·credential 원문을 복사하지 않는다.
focused Maven adapter suite 9 tests, failures 0, errors 0, skipped 0,
BUILD SUCCESS(2.953초)를 확인했고 마지막 native assertion 변경도
`javac --release 21`로 compile했다. [JUnit XML](fixture-dependency-tests.xml)을
남긴다. 실제 native 재실행은 root 통합 backend에서 필요하다.
