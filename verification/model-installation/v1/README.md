# MODEL fixture의 명시적 설치 입력이다

원본 M01–M60 fixture와 corpus/binding의 218개 artifact를 보존하면서
UUID형 model Given을 실제 제품 PostgreSQL에 설치한다. 이 디렉터리는
원본을 고친 것이 아니라 coordinator가 선택한 별도 synthetic 입력이다.
Task, 사용자 Step, S5, S6 및 전체 모델 인수는 OPEN이다.

`ModelFixtureInstaller`는 `baseline.domainFacts`인 경로만 처리한다.
기존 typed scenario installer와 그 의미는 바꾸지 않는다. send, observe,
readiness, session 및 backend/schema는 수정하지 않는다. 설치기는 corpus,
expected intent, oracle을 읽지 않는다. `inventory.py`만 별도의 분석
산출물에서 원본 oracle pointer와 실제 누락 입력의 관계를 보여 준다.

## 입력과 역변환이다

`original-input-lock.json`은 a7f18cc8의 원본 raw byte SHA-256을 고정한다.
`installation-inputs.tar.gz`는 60개의 새 입력과 실제 synthetic source
원문을 담는다. `generated-input-index.json`은 각 원본 fixture SHA와 새
입력 SHA, archive SHA를 별도로 고정한다. 원본 SHA를 새 내용의 SHA로
바꾸지 않는다. 각 새 row와 correction은 다음 세 값을 false로 둔다.

- originalInputProvided
- originalAuthorityClaimed
- operatingApprovalClaimed

각 ledger는 원본 before/after 또는 정확한 새 ID와 논리 inverse를 담는다.
append-only 제품 row를 삭제해 undo한다는 뜻은 아니다. 실제 복구는
coordinator가 소유한 격리 DB의 설치 전 snapshot 또는 해당 attempt의
폐기로 수행한다. 설치기는 공유 DB를 비우거나 UUID를 remap하지 않는다.
source bootstrap은 한 SQL transaction이다. M05/M50의 별도 권한과 HTTP
후속 단계는 독립 commit이므로 실패하면 부분 Given을 재사용하지 않고
해당 소유 attempt DB/server를 폐기한다. installation result에 후속 실패와
이미 commit된 source receipt를 그대로 보존한다.
동일 UUID가 60개 case에 쓰이므로 attempt마다 fresh DB/server가 필요하다.
중복 설치는 strict duplicate error이며 새 blob만 rollback 정리한다.

원본 READ/WRITE/RECORD와 semantic action은 현재 capability 계약으로
닫힌 변환을 한다. role은 해당 kind와 원래 grant action의 교집합이다.
원래 SOURCE, TARGET, validity/revision/revocation은 유지하며 all-of이다.
SOURCE 없는 READ grant는 별도의 새 권한이며 old grant로 표시하지 않는다.
manager root는 원래 grant마다 하나씩 만든다. M42/M46의 없는 write,
M47의 revoke, M48의 좁은 action, M57의 closeRecall 한정은 유지한다.

S1은 원래 SalesOrder ID와 다른 delivery Work를 쓴다. typedReferences와
실제 table/id binding을 반환하며 원래 alias를 몰래 덮어쓰지 않는다.
M59는 원래 BOX_to_EA=12의 승인된 별도 변환 ledger를 둔다. A/B와
연결된 O1/S1 수량만 정확히 12배로 바꾸며 원본 baseUnit EA를 유지한다.
0.5EA 요청을 반올림하거나 고정 TYPE_INVALID oracle을 줄이지 않는다.

M50만 승인된 physical identity 예외가 있다. 원래 A UUID/active60은
ORIGINAL_REFERENCE_ONLY snapshot으로 보존하고 해당 row를 먼저 심지
않는다. 원래 B40을 설치한 뒤 실제 confirmReceipt가 만든 leaf60을
correctedPhysicalBindings.A로 반환한다. 원본 aliasMap.A는 원래 UUID다.
이 예외를 다른 case에 적용하지 않는다. 원본 그대로의 M50은
BLOCKED_PHYSICAL_IDENTITY_CONFLICT이다.

## source와 관찰 경계다

원래 DOC1/DOC2는 고정 SHA의 원문이 없으므로 UNKNOWN이다. 별도 source
원문은 새 ID와 실제 일치 SHA를 가지며 original과 섞지 않는다.
Verification Boolean은 입력받지 않는다. 실제 document/event/assertion,
profile, inbox, canonical의 scope, quantity, unit, clock와 hash를 대조해
설치기가 검증 결과를 계산한다. sourceFactReceipts의 SQL readback은 실제
row 값과 transaction/snapshot을 보존한다. typed binding은 실제 row의
선택자이며 corpus값이나 expected Boolean을 공급하지 않는다.

COMMAND Work에는 실제 설치된 Work row에서 읽은 초기 WorkTransitions
snapshot을 남긴다. 현재 제품은 이 history가 없는 COMMAND Work를
읽지 않는다. metadata를 AVAILABLE로 만들거나 Boolean assessment를
심는 것으로 이 공백을 대신하지 않는다.

FX, selection, 문서 text 등의 static claim은 명시적 미검증 receipt로
분리한다. 현재 제품 사실, VERIFIED/AVAILABLE 증거 또는 모델 prompt
입력으로 취급하지 않는다. 원본 oracle이 이를 요구하면 그대로 실패 또는
미충족으로 남긴다. `fixtureComplete=false`와 unverifiedOriginalClaims는
이 경계를 표시한다. static mapping 완료는 실제 60개 인수 PASS가 아니다.

M05의 shipment/cargo READ와 M03/M50의 receipt authority는 개별 승인된
새 목적 grant/root다. 원래 grant는 유지한다. M05 getShipment는 실제
QueryHandler가 공개하지만 acceptance catalog에 없는 현재 query라서
그 case만 별도 TARGET-bound assignment를 남긴다. M03/M50 원래 O1의
Human owner/supervisor는 membership만 보완하며 capability/grant를 더하지
않는다. M50은 실제 첫 command, 동일 결과 replay, 다른 payload conflict를
실행한다. 결과의 replay Boolean이나 이력/effect count를 만들지 않는다.

## 재현 방법이다

repo root에서 archive를 풀거나 결정적 generator를 실행한다.

```sh
tar -xzf verification/model-installation/v1/installation-inputs.tar.gz
python3 verification/model-installation/v1/build_inputs.py
python3 verification/model-installation/v1/package_inputs.py
```

Archive를 먼저 푼 뒤 generator를 실행한다. 이 순서는 현재 fixture mapping에서
다시 만들지 않는 과거 M50 generated source 파일 12개를 보존한다. 빈
`generated/`에서 새로 만들면 committed package와 달리 그 12개가 빠진다.
generator는 archive에서 풀어 read-only로 저장된 input을 덮어쓰기 전에
user-write 권한을 추가한다.

`package_inputs.py`가 출력한 SHA와 index를 대조한다. 생성 과정도 원본
lock을 전후 확인한다. 설치 hook에는 다음 system property를 지정한다.

```text
-Dverification.model.installationInputs=verification/model-installation/v1/inputs
-Dverification.actual.blobRoot=<해당 attempt 소유의 blob root>
```

`./verify prepare`는 committed archive의 index/hash, M01–M60 전체 입력,
현재 acceptance contract와 template SHA, 원본 fixture SHA를 함께 검사한다.
동일 검사는 다음 명령으로 독립 실행할 수 있다.

```sh
python3 verification/model-installation/v1/test_archive_contract_pins.py
```

회귀 검사는 오래된 pin, 누락 입력, 손상 JSON을 거부하고 원본 218개
artifact lock을 대조한다. 이 준비 검사는 archive 정합성만 확인하며
MODEL fixture 설치, DB 효과 또는 60개 scenario의 수용을 증명하지 않는다.

실제 focused probe는 `ModelFixtureProbe inventory Mxx output.json`,
`inventory-all ALL output.json`, `install Mxx output.json`을 쓴다.
JDK21과 harness dependency classpath를 사용한다. 실제 설치에는 기존
ActualConfiguration의 loopback URL, DB, disposable 표시와 JWT 입력이
필요하다. 인증 원문은 artifact에 넣지 않는다. M05/M50 helper는 실제 HTTP
요청도 하므로 그 DB에 연결된 해당 attempt server가 필요하다. 이는 scenario
installer에 대한 계약이며, 아래 focused PostgreSQL test의 환경 변수와
구별한다.

focused unit/실제 PG 검사는 아래 command로 실행한다. Repository는
complete database/role provisioner를 제공하지 않는다. Operator는 먼저
별도 isolated fresh PostgreSQL 18.6 SCRAM cluster, dedicated database와
owner role, 그리고 non-5432 port를 준비한다. Role은 migrations와
`btree_gist` extension 생성에 필요한 권한을 가진다. Test suffix, port,
JDK 21 runtime, temporary Maven settings/repository/compiler, 그리고
nonblank `MODEL_INSTALLER_TEST_DB_PASSWORD`를 secure environment로
주입한다. Actual PostgreSQL test는 JDBC URL이 없으면 `SKIPPED`이며 PASS
증거가 아니다. URL이 있으면 guard가 opt-in, credential과 URL의 전용
identity를 DB connect 전에 검증한다. 실제 DB identity, 버전과 빈 schema는
접속한 뒤 migration write 전에 대조한다.

```sh
(
  set -eu
  : "${TEST_SUFFIX:?Set the pre-created test DB/user suffix}"
  : "${PG_PORT:?Set the fresh cluster port}"
  : "${SYSTEM_JAVA_HOME:?Set a JDK 21 runtime}"
  : "${MAVEN_SETTINGS:?Set current temporary proxy settings}"
  : "${MAVEN_REPOSITORY:?Set a writable Maven repository}"
  : "${TEMURIN_JAVAC:?Set the checksum-verified JDK 21 javac}"
  : "${MODEL_INSTALLER_TEST_DB_PASSWORD:?Securely inject a temporary password}"
  export MODEL_INSTALLER_TEST_DISPOSABLE=true
  export MODEL_INSTALLER_TEST_JDBC_URL="jdbc:postgresql://127.0.0.1:${PG_PORT}/mulino_model_fixture_${TEST_SUFFIX}"
  export MODEL_INSTALLER_TEST_DB_USER="model_fixture_${TEST_SUFFIX}"
  export MODEL_INSTALLER_TEST_DB_PASSWORD
  unset MAVEN_ARGS
  cleanup() {
    unset MODEL_INSTALLER_TEST_DISPOSABLE MODEL_INSTALLER_TEST_JDBC_URL \
      MODEL_INSTALLER_TEST_DB_USER MODEL_INSTALLER_TEST_DB_PASSWORD
  }
  trap cleanup 0
  test_selector=ModelFixtureInstallerTest#actualPostgresInstallsTypedIdentityInventory
  test_selector="${test_selector}SourceAndResponsibilityAndRollsBackInvalidFacts"
  if JAVA_HOME="$SYSTEM_JAVA_HOME" ./mvnw -B -ntp \
      -s "$MAVEN_SETTINGS" -Dmaven.repo.local="$MAVEN_REPOSITORY" \
      -Dmaven.compiler.fork=true \
      -Dmaven.compiler.executable="$TEMURIN_JAVAC" \
      -f verification/harness/pom.xml -Dtest="$test_selector" test; then
    test_status=0
  else
    test_status=$?
  fi
  exit "$test_status"
)
```

이 command는 cloud runtime의 JDK 21 `java`와 checksum이 확인된 Temurin
`javac`를 분리해 사용한다. 현재 Maven proxy와 toolchain 설정은
[cloud 실행 기록](../../../docs/execution/model-archive-cloud-integration.md)에
있다. 이 설정은 server 구성을 바꾸지 않는다. Test는 database/role을
생성하거나 drop하지 않고 PostgreSQL을 stop하지도 않는다. Operator는
전용 cluster를 멈추고 port가 닫혔으며 runtime/credential directory가
삭제됐는지 확인한 뒤 cluster를 폐기한다.
`MODEL_INSTALLER_TEST_DISPOSABLE` 값은 정확히 `true`여야 한다. 이
marker는 operator assertion일 뿐 server identity를 증명하지 않는다.
`MODEL_INSTALLER_TEST_JDBC_URL`은 URL userinfo, query, fragment 없이
`localhost`, `127.0.0.1` 또는 `::1`의 loopback TCP 주소와 명시 포트를
지정한다. 기본 PostgreSQL port `5432`는 거부된다. URL database는
`mulino_model_fixture_<16 lowercase hex>`이고
`MODEL_INSTALLER_TEST_DB_USER`는 같은 suffix의
`model_fixture_<16 lowercase hex>`여야 한다. Password는 비어 있지 않은
임시 값으로 별도 environment injection을 한다. 이 설정은 test opt-in일
뿐 database나 role을 생성하지 않는다. 매 시도마다 새 suffix의 dedicated
user/database를 별도 isolated cluster에 준비한다. Test role은 DB owner이고
migrations 및 `btree_gist` extension 생성에 필요한 권한이 있어야 한다.
Shared, pre-migrated, production cluster/database는 지정하지 않는다.

Actual test는 current database/user identity와 PostgreSQL 18.6을
대조하고, `public` schema에 relation, routine, type이 없어야 통과한다.
Fresh database에 연속된 current migration 45개를 version 숫자 순서로
적용한다. JDBC simple query mode에서 file마다 별도 transaction을 쓴다.
Flyway는 호출하지 않는다. 종료 전에 `btree_gist` 설치를 확인한다.
SQL exception 상세는 redaction하고 SQLSTATE를 보존한다.

Cloud run은 Maven Central의
`io.zonky.test.postgres:embedded-postgres-binaries-linux-amd64:18.6.0`
Linux amd64 artifact를 사용했다. Published SHA-1은
`0a36625fa44dc0d65cb1cee6041e468d96bdbb8d`이고 local SHA-256은
`008ee189eaa7ca2b58bd01dd950291e638b998392cf982297acd4779ec898ac1`이다.
18.6 binary는 synthetic disposable cluster에만 썼으며
binary/runtime는 repository artifact가 아니다. 이 probe는 M01–M60
scenario install이나 전체 PostgreSQL/application acceptance가 아니다.

실제 모델, UAT, corpus scenario, Workflow 및 API 구매는 실행하지 않았다.
실제 scenario 실행은 coordinator의 Haiku5.5 native dynamic Workflow
범위이며 이 installer proof와 구별한다. 정확한 실행 결과와 미해결
case는 `proof-index.json`, `corrected-mapping-inventory.json`,
`missing-input-inventory.json`에 기록한다. SUPPLIER scope와 aggregate
stocktake 등 제품/schema 작업은 다른 owner 범위로 남긴다.

기존 `proof-index.json`과 `focused-evidence.tar.gz`는 이 archive pin 변경
전의 inputs를 사용한 과거 PostgreSQL 증거다. 이 변경은 DB 설치를 다시
실행하거나 그 증거를 새 archive에 재귀속하지 않으며, 새 M01–M60
scenario 수용을 의미하지 않는다.

## 2026-10-24 current source 설치 증거다

현재 packaged MODEL 입력 조사와 fresh DB 설치의 결과는
[실행 기록](../../../docs/execution/model-corpus-current-installation.md)에
연결한다. 이 검사는 `dda42dd6` code baseline과 현재 input archive hash를
고정한 별도 run이다. 42개 database-only install과 M05/M50의 실제 HTTP
helper 설치를 구분한다. 모든 설치 결과의 `fixtureComplete`는 false이며
model call·scenario run은 0, 전체 acceptance는 `NOT_RUN`이다.

current summary와 raw command/log는 ignored local path
`.local/model-corpus-current/`에만 있다. 이 path는 repository에 포함하지
않았고 공개 download로 취급하지 않는다. 기존 `proof-index.json`은
`a7f18cc8` baseline의 과거 archive 증거로 남긴다. current installation
summary가 이 historical proof를 교체하거나 재귀속하지 않는다.
