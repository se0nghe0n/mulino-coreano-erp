# MODEL 입력과 공개 조회 목록의 cloud 결합 검증이다

2026-10-24에 `3de8cc99`에서 재개했다. 이 수정은 최신 공개 QUERY
catalog와 그 파생 입력을 정합하는 범위다. 제품 handler, DB schema,
기존 업무 fixture와 업무 oracle은 변경하지 않았다. Task, 현재 Step,
S5와 S6는 OPEN이다.

같은 tree의 canonical remote commit은 `36ebefd7`→`ec643ab4`,
`8759fd2a`→`07ab3925`다. 이 문서 기준선 `62b3c91b`는 remote와 같은 SHA며,
이전 local 통합 상태는 `archive/ontology-cloud-local-994708ce`에 보존했다.

사용자가 이 대화의 cloud 작업에 허용한 `GPT-6-Luna` worker 두 개를
`xhigh`로 실행했다. 실제 T3 `codex_2` 또는 Haiku dynamic Workflow로
표시하지 않는다. 초기 local implementation phase에서는 push나 배포가 없었다.
이후 canonical remote publication은 사용자 승인으로 이어졌으며, 별도 API
결제·업무 model 호출·배포는 없었다.

## 확인한 실패와 통합한 수정이다

- 원 archive의 M01–M60 입력 60개는 이전 capability catalog SHA를
  authority와 template 두 곳에 고정했다. 현재 catalog에서 이 입력은
  DB 접근 전에 거부된다. 정적 hash 대조와 archive 회귀의 RED로
  확인했으며 실제 DB 설치 실패를 실행한 것으로 표시하지 않는다.
- `36ebefd7`에서 새 catalog에 맞춰 두 pin만 갱신했다. 원본 218개
  artifact, 입력 record 수와 fixture hash, generated source 1,476개의
  경로와 byte를 보존했다. archive-first 생성·포장은 두 번 동일한
  archive와 index를 만들었다. 과거 PG proof는 그대로 보존했다.
- 별도로 untouched `3de8cc99`의 기존 generator 검사를 실행했다.
  MCP suite는 1 PASS/2 FAIL, case suite는 5 PASS/1 FAIL이었다.
  새 QUERY 17개가 T20/C3/V4의 공개 도구 기대 목록에 반영되지 않았다.
- `8759fd2a`에서 36개 discovery 목록과 해당 파생 hash만 갱신했다.
  원본 업무 assertion, actor/grant, fixture, 양과 시계는 그대로다.
  clone에 없는 worker commit에 의존하던 보존 test는 실제 통합 history의
  114→120→137 단계를 확인하도록 고쳤다. 정확한 역복원과 negative
  mutation 검사로 보호 범위를 유지했다.

새 archive SHA256은 다음과 같다.

`32d5c2df808ad89892dd7d7fe2e3c681e212c0f9176402be44b215602dda1519`

정합 후 입력은 새 정적 준비물이다. 기존 `proof-index.json`과
`focused-evidence.tar.gz`의 과거 실제 PG 설치 결과가 새 입력에 대한
실행 증거가 되지는 않는다. 16개 미완료 mapping과 전체 model 인수의
미실행 상태도 이 수정으로 닫지 않았다.

## 통합 checkout의 실제 검사다

`8759fd2a10529991e3ceb3811b0d86e03ea156d2`에서 preparation의 Python
asset 13개를 직접 실행했고 모두 exit 0이었다. 같은 checkout의 별도
public inventory preservation suite도 6개 모두 PASS였다.
실제 toolchain은 Linux amd64, OpenJDK 21.0.12.1, Maven 3.9.16,
Python 3.12.14다. 과거 macOS 실행 환경과 구별한다.

asset에는 archive 회귀 6개, MCP generator 3개, case generator 6개와
catalog, layer, vocabulary, request, observation binding 검사가 포함된다.
실행 전후 tracked 파일 4,867개의 SHA를 대조해 drift 0을 확인했다.
이 수를 고유 업무 scenario 또는 assertion coverage로 합산하지 않는다.

이 Python-only 실행 당시 `PreparationAssetChecks.java`는 JDK compiler의
parse 단계만 통과했다. 이는 문법 검사이지 Java 전체 compile이 아니었다.
이후 통합 기준선 `62b3c91b59dfd80d428138f817d989e4d844ffaa`에서
Java 준비 검증을 회복해 별도로 실행했다.

`2026-10-24T04:43:36.219226009Z`에 같은 62b3c91 기준으로
`./verify prepare`가 exit 0, `PREPARED`를 반환했다. 고정 corpus는 41 case,
802 subcase, 24,437 assertion이고 preparation problem은 0이다. case asset
group 13개가 모두 PASS이고, 실행 checkout의 tracked regular file 4,868개에서
drift는 0이다. 이는 준비물 조립·검사 PASS다. 알려진 open gap 196개,
artifact-kind attribution gap 0개, gated runtime profile 2개가 남았다.
report는 `gateComplete=false`, `runtimeStatus=NOT_RUN`,
`runtimeComplete=false`, `productRuntimeClaimed=false`,
`artifactCoverageStatus=NOT_RUN`, `semanticOracleEquivalence=REQUIRES_CASE_REVIEW`
를 기록한다. 전체 product/backend/scenario acceptance로 승격하지 않는다.

toolchain은 Linux amd64, 설치된 Debian system Java image OpenJDK 21.0.12.1,
Maven 3.9.16이다. 이 system image의 `lib/ct.sym` 부재로 `--release 21` 첫
compile은 실패했다. Adoptium 공식 release의 Temurin 21.0.13+9 archive를
내려받고 공식 SHA256
`d7f5d5cf865a2902b70ff549b97586a7d3f234e016f041ab447ea740c7d66d1c`를
대조했다. Temurin runtime으로 Maven을 실행한 첫 시도에서 main
source 103개와 test source 113개는 compile됐지만, Surefire plugin dependency
resolution이 proxy certificate의 PKIX 신뢰 검증 실패로 중단됐다.

이후 focused test 재실행은 system Java runtime 21.0.12.1 아래에서 forked
compiler로 Temurin `javac`를 지정했다. 그 로그는 main/test compile 단계 모두
`Nothing to compile`이라고 기록한다. 따라서 이 성공 test run에서 새 compile이
일어난 것이 아니라 앞선 Temurin 시도의 최신 class files를 사용했다. 인증서나
trust store를 바꾸지 않았고 TLS 검증을 끄지 않았으며, 최종 runtime은 system
Java image의 기존 trust 설정을 사용했다. 제품 source와 dependency 버전은
바꾸지 않았다.

별도 focused run은 `NativeProductIntegrationTest` 5/5 PASS,
`ModelFixtureInstallerTest` 5 PASS와 1 SKIP, 총 11개 중 10 PASS·0 failure·
0 error·1 skip, exit 0이다. 유일한 skip은 이 test가 소유한 disposable
PostgreSQL database가 설정되지 않아 실제 probe를 실행하지 않은 결과다.
두 class는 focused/mock harness 계약 검사이며 실제 client/model acceptance나
실제 PostgreSQL 설치 결과가 아니다. 아래의 후속 PostgreSQL probe는 이
두 class 실행과 별개의 한-method 검사이며 두 결과를 합산하지 않는다.

## 소유한 PostgreSQL에서의 한정 probe다

2026-10-24 local tested source commit
`e2f71ce0d0c7e6e2299bb6bb6cea325feedab234`와 tree
`d42793c02e60cde908b344ca64455098615a9b88`에서 fresh disposable
PostgreSQL 18.6 cluster의 `ModelFixtureInstallerTest` actual PostgreSQL
method 하나를 실행했다. Exact test selector는 `-Dtest=ModelFixtureInstallerTest#actualPostgresInstallsTypedIdentityInventorySourceAndResponsibilityAndRollsBackInvalidFacts`다. JUnit 결과는 1 test, 1 PASS·0 failure·0 error·0 skip, Maven exit 0이다. 이는 cloud-run local commit이며 canonical remote SHA로 부르지 않는다.

Test는 `MODEL_INSTALLER_TEST_DISPOSABLE=true`, 전용 loopback JDBC URL,
동일한 16-character lowercase hex suffix의 dedicated database/user 및
nonblank 임시 password를 요구한다. URL은 userinfo/query/fragment, 원격·
다중 host와 default port 5432를 거부한다. URL이 있으면 configuration
validation이 DB connect 전에 실행된다. fresh `public` schema가 비어 있고
current database/user가 일치하는지 확인한다. SCRAM authentication에서
잘못된 password의 SQLSTATE `28P01`도 확인했다. Coordinator-owned runtime은
`127.0.0.1` TCP만 listen하고 Unix-domain listener를 끄며, host security나
TLS 설정은 바꾸지 않았다.

Probe는 current source의 V1–V45 migration 45개 hash가 file과 일치함을
대조하고 version의 숫자 순으로 적용했다. JDBC simple query mode에서
migration file마다 독립 transaction을 썼고 Flyway를 호출하지 않았다.
`btree_gist` 1.8을 확인했다. Synthetic setup의 install은 commit됐다.
Proof에는 33 `sourceFactReceipts`와 33 논리 inverse가 기록됐다. Invalid
fact는 SQLSTATE `23514`로 거부됐고 invalid/unknown organization row는
각각 0이었다. Invalid input 전후 blob file 수는 1→1이었다.

최종 reuse regression은 `REJECTED_NONEMPTY_DATABASE`/`55000`을 확인했다.
거부 전후 organization row는 1→1, fixture quantity는
`60.000000000000`→`60.000000000000`, source document row는 2→2로
보존됐다. Product proof는 `committed=true`와 `fixtureComplete=true`지만
`acceptanceStatus=NOT_RUN`이다. Inverse 항목은 논리 기록이지 append-only
product row의 물리 rollback이 아니다. Owned disposable DB를 버려 정리한다.

최종 attempt `e28adc8d2ad3ce7d`는 server stop, port closed, temporary
runtime directory removed를 모두 기록했다. 4,870 tracked regular file에서
source drift가 0이고 protected original fixture artifact 218개의 hash도
모두 일치했다. 별도 unit run의 `ModelFixtureTestDatabaseConfigurationTest`
는 5/5 PASS다. JUnit XML은
`.local/model-pg-probe/guard-unit-evidence/`에 복사했다.
SHA256은 `dedd084054fd4d434c3901a3e6181c46d0b4c1b1dd7828f630861c82c601209e`다.

각 시도는 별개로 기록하며 합산하지 않는다. `attempt-1fb5bb6bd1236619`는
sandbox의 AF_UNIX socket 제한으로 server start가 실패해 DB test를 실행하지
않았고 resources는 정리됐다. `attempt-ee33ff490c033a20`은 source
`df7323e5`에서 server 및 SCRAM bootstrap을 통과했으나 blank password가
거부돼 test가 1 error로 끝났다. 모든 temporary resources는 정리됐다.
`attempt-c60e8d8d96c4d035`는 nonblank password로 한 test를 통과했지만
후속 reused-database assertion 전의 source를 썼다. 최종
`attempt-e28adc8d2ad3ce7d`만 nonempty-DB rejection과 기존 row/quantity/
document 보존을 포함한 현재 한정 PASS다.

최종 evidence는 `.local/model-pg-probe/attempt-e28adc8d2ad3ce7d/`의
`summary.json`, JUnit XML, `model-fixture-postgresql-proof.json`에 있다.
Proof SHA256은
`550cc22cba1b5f1863ad382bd67ccdab6e9eb432275463ffc696b863bde3525c`다.
Focused Maven log SHA256은
`6fbed13631961848069a32c26e4da548c9e70cf4acc7bcd78eab2cf30310b60d`,
JUnit XML SHA256은
`8945958e7c6db5854e292057b86b69fee6d7ceefa27857334d1712a7637f0f7d`다.
`.local/model-pg-probe/final-focused-proof-summary.json`은 proof, current
migration inventory, reuse rejection, cleanup, JUnit counts, 4,870개 tracked
source의 drift 0, 45 migration hash, protected fixture checks의 cross-check를
모은다. Maven Central binary provenance는
`.local/model-pg-probe/tooling/binary-provenance.json`에 기록했다. Ignored
local artifacts와 cluster runtime은 repository에 publish하지 않았다.

이 결과는 standalone generic synthetic setup 하나이며 M01–M60 scenario
installer, corpus scenario, backend/application/HTTP/client/model acceptance,
S5/S6 또는 전체 Task의 인수가 아니다. Model call 수는 0이다. `./verify
prepare`의 41 case·802 subcase·24,437 assertion은 준비물만 검사한다.

8759fd2a 기준의 초기 Python asset 검사 argv, exit, 소요 시간, source commit과
원로그 SHA는 `.local/model-archive-integration/combined-checks.json`에 있다.
62b3c91b 기준 Java focused test와 전체 prepare의 명령과 결과는 같은
디렉터리의 `dependency-diagnosis/java-prepare-commands.json`과
`java-prepare-summary.json`에 있다. 후속 actual PG method의 argv와 결과는
`.local/model-pg-probe/attempt-*/summary.json`에 기록했다.
`.local/model-archive-integration/archive-worker/`와 `query-worker/`에
각 worker의 RED·재현·역복원 로그를 복사했고 기존 원본도 보존했다.

## 실행 흔적과 재현 환경이다

첫 focused Maven 시도는 기본 cache 경로 권한 때문에 exit 1이었다.
workspace 소유 cache를 사용한 다음 시도도 직접 DNS 실패로
`cucumber-bom:8.0.4`, `junit-bom:6.1.2`를 받지 못해 test 전에 exit 1이었다.
이후 공식 Maven Central BOM URL 두 곳의 HTTP HEAD는 현재 cloud proxy 경로에서
HTTP 200을 반환했다. 임시 Maven settings는 이 프로세스의 기존 HTTPS proxy로
만들었고 proxy credentials는 없었다. proxy port는 실행마다 바뀌므로
settings의 숫자나 값을 이 문서에 고정하지 않는다. 최종 focused run과 prepare는
현재 proxy, 임시 settings, 쓰기 가능한 Maven repository를 사용했다.

재실행 시에는 현재 프로세스의 `HTTPS_PROXY`에서 임시 proxy settings를
생성해 `MAVEN_SETTINGS`로 지정하고, 쓰기 가능한 `MAVEN_REPOSITORY`,
system Java runtime 21의 `JAVA_HOME`, workspace-owned `MAVEN_USER_HOME`, checksum
검증된 Temurin `TEMURIN_JAVAC` 경로를 환경에 넣는다. 포트는 이전 실행 값을
복사하지 않고 새 settings에 반영한다. 아래는 실제 실행의 옵션 형태이며,
임시 경로·proxy port는 환경에 따라 달라진다.

```sh
export JAVA_HOME="$SYSTEM_JAVA_HOME"
export MAVEN_USER_HOME="$WORKSPACE_MAVEN_HOME"
export MAVEN_SETTINGS="$CURRENT_RUN_DIR/proxy-settings.xml"
export MAVEN_REPOSITORY="$WRITABLE_MAVEN_REPOSITORY"
export TEMURIN_JAVAC="$CHECKSUM_VERIFIED_TEMURIN_HOME/bin/javac"
unset MAVEN_ARGS

./mvnw -B -ntp -U -s "$MAVEN_SETTINGS" \
  -Dmaven.repo.local="$MAVEN_REPOSITORY" \
  -Dmaven.compiler.fork=true \
  -Dmaven.compiler.executable="$TEMURIN_JAVAC" \
  -f verification/harness/pom.xml \
  -Dtest=ModelFixtureInstallerTest,NativeProductIntegrationTest test

export MAVEN_ARGS="-s $MAVEN_SETTINGS -Dmaven.repo.local=$MAVEN_REPOSITORY -Dmaven.compiler.fork=true -Dmaven.compiler.executable=$TEMURIN_JAVAC"
./verify prepare
```

새 실행의 상세 기록은 `.local/model-archive-integration/dependency-diagnosis/`
안의 `java-prepare-summary.json`, `prepare.json`, `prepare-wrapper-commands.json`,
`java-prepare-commands.json`, `verify-prepare.log`,
`focused-java-system-runtime.log`에 있다. 초기 직접 DNS와 두 공식 BOM HTTP HEAD
응답은 `dns.json`과 `official-head.log`, system/Temurin Java 차이는
`focused-java.log`, `focused-java-full-jdk.log`, `full-jdk-toolchain.log`에서
확인한다. 공식 JDK archive/checksum metadata는 `jdk21-assets.json`에 있다.
이 증거는 무시된 로컬 실행 기록이며 제품 변경이나 dependency 변경을 포함하지 않는다.

첫 cache·DNS 실패와 Temurin runtime의 TLS 실패는 각 원로그에 그대로
보존했다. Java focused checks와 `./verify prepare`가 회복됐고 별도로
소유한 fresh PG18.6의 단일 installer/rollback/reuse test도 PASS다. 이는
backend 전체, M01–M60 설치/scenario, 업무 scenario, 실제 client/model 및
S6/BTP의 acceptance가 아니므로 해당 범위는 `NOT_RUN`이다. 특히 두 gated
profile은 regulatory evidence 검토와 scheduler natural-cycle 기록이 없어
gate를 통과하지 않는다. 최신 prepare와 이 focused PG probe가 완료됐어도
전체 Task, 현재 Step, S5와 S6는 `OPEN`이다.
