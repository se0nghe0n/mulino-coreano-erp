# 플랫폼 후보와 테스트 harness 조사

조사일은 2026-10-07이며 baseline은
`393cb5c3d9f4d16fe728933ef211f9b882a73d25`다. 이 기록은
[구현 계획](../ontology-implementation-plan.md) §2·§11·§13과
[stage gate](../../.agents/skills/ontology-implementation/references/stage-gates.md)를
위한 사전 조사다. CAP 최종 채택이나 S0 완료 기록이 아니다.

공식 문서, published artifact metadata와 저장소 밖 dependency probe에서
CAP Java5·Boot4.1 조합의 resolution을 확인했다. 앱·test 코드, schema와
DB를 만들거나 실행하지 않았다. 기존 구현을 읽거나 재사용하지 않았다.
R2 추적 선택은 미정이며 사용자 Step2 코드 작성은 `PENDING_R2`다.
**S0 runtime 인수는 `NOT_RUN`이다.** CAP fallback을 선택할 실제
incompatibility 근거는 발견하지 않았다.

## 후보 버전과 확인 범위

버전은 조사 시점의 값이다. 아래 표와
[candidate manifest](evidence/platform-research/candidate-manifest.json)는
후속 실행의 입력이며 최종 build stack을 고정하지 않는다.

| 항목 | 후보 또는 관찰 값 | 확인 범위 |
|---|---|---|
| Java | Zulu21.0.5+11 | 로컬 `java -version` |
| Maven | 3.9.16 | SHA512 대조·실행 성공 |
| Maven Wrapper | 3.3.4, only-script | 생성·실행 성공 |
| CAP BOM/starter/PG feature | 5.1.1 | POM·dependency resolution |
| CDS Maven plugin | 5.1.1 | published metadata만 확인 |
| Spring Boot | 4.1.1 | CAP starter POM·resolution |
| Spring Framework | 7.0.9 | Boot BOM·resolution |
| Spring Security | 7.1.1 | Boot BOM 관리 값, runtime 미검증 |
| PostgreSQL JDBC | 42.7.13 | CAP PG feature·Boot BOM·resolution |
| Flyway core/PG module | 12.4.0 | Boot BOM·resolution |
| JUnit 기본 조합 | 6.0.3 | Boot BOM·resolution |
| Testcontainers | 2.0.5 | PG/Jupiter 모듈 resolution |
| Surefire/Failsafe | 3.5.6 | Boot parent 관리 값 |
| CDS dk/runtime/compiler | 10.1.0 / 10.1.1 / 7.1.1 | npm metadata·lock resolution |
| CDS PG plugin | 3.1.1 | npm metadata·lock resolution |
| PostgreSQL image | 18.6 | registry manifest 조회, 실행 안 함 |
| Cucumber-JVM | 8.0.4 | metadata·공식 tag 문서만 확인 |
| Cucumber용 JUnit override | 6.1.2 | 후보, resolution·실행 안 함 |
| SAP resource-server starter | 4.1.2 | metadata만 확인, identity 선택 미정 |

[CAP versions](https://cap.cloud.sap/docs/java/versions)는 Java21,
Maven3.9.14 이상과 최신3.9 계열을 안내한다. Maven4는 권장하지 않는다.
[June release](https://cap.cloud.sap/docs/releases/2026/jun26)는 CAP5와
Spring Boot4 지원을 설명한다. [CAP starter POM][cap-pom]의 Boot4.1.1,
[PG feature POM][pg-pom]의 JDBC42.7.13은 [Boot BOM][boot-pom]과 일치한다.

로컬 도구는 Docker29.4.0, Node26.10.0, npm11.19.1이었다. npm lock
resolution은202개 package에서 성공했으나 `@sap/xsenv:6.2.2`가 허용하는
Node20/22/24 범위와 로컬26이 달라 `EBADENGINE` warning을 출력했다.
Node24 exact patch는 후속 검증할 후보이며 이 조사에서 설치하지 않았다.

CAP 공식 PostgreSQL 자체 검증 대상은15.x로 기록돼 있다. 선택 후보18.6의
CQN·잠금·schema·outbox는 S0에서 실제 증명해야 한다. PG multitenancy와
extensibility 제한을 정의 데이터 기반 FDE 구현 불가로 해석하지 않는다.
[CAP PostgreSQL](https://cap.cloud.sap/docs/guides/databases/postgres)

PostgreSQL 공식 [18.6 release](https://www.postgresql.org/docs/release/18.6/)와
registry image annotation에서18.6을 확인했다. `postgres:18.5` 조회는
`not found`였고18.6 조회는 성공했다. 실행 후보의 index digest는
`sha256:fc973eb97c9fd04bfa1840e0f510719a584ccb3be8debfe6a4144637a9dfe8cf`다.
arm64/v8 child digest는
`sha256:a63b24165ea0eacced7585766d790ac08bbaefe10a1841f87783a4505d29a69c`다.

## 공개 API와 필요한 인수

- Boot4.1.1 JAR에서
  `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`를
  확인했다. Testcontainers2의 artifact는 `testcontainers-postgresql`과
  `testcontainers-junit-jupiter`이며 새 PG class는
  `org.testcontainers.postgresql.PostgreSQLContainer`다.
  `@ServiceConnection` 또는 `@DynamicPropertySource`로 독립 disposable
  datasource를 제공할 수 있다. [PG 모듈][tc-pg], [Boot 연동][boot-tc]
- CAP5.1.1 JAR에 `CdsRuntime.requestContext()`, `changeSetContext()`,
  `ChangeSetContextRunner.markTransactional().run(...)`가 있다.
  `PersistenceService.run(...)`와 Spring `JdbcTemplate`을 같은
  `TransactionTemplate`/`@Transactional` application 경계에 둔다.
  audit/outbox/idem 실패 뒤 독립 connection에서 효과0을 관찰한다.
  test 전체 rollback으로 앱 rollback 실패를 숨기지 않는다.
  [ChangeSet][cap-tx], [Spring test transaction][spring-test-tx]
- V2/V3/V7에는 실제 두 transaction과 barrier가 필요하다.
  `Select.from(...).byId(...).lock()`과 `.lock(int timeout)`이 공개 API다.
  segment·grant·policy의 안정된 fence를 고정 순서로 잠근 뒤 상태를
  재조회한다. 현재 제한 row만 잠그면 새 restriction INSERT phantom을
  막았다는 증거가 아니다. QC commit·grant 철회의 양쪽 선후에서
  원장·의무·response·outbox를 관찰한다.
  [CAP 잠금][cap-lock], [PG18 잠금][pg-lock], [PG18 isolation][pg-iso]
- Flyway만 DDL을 실행하며 `spring.sql.init.mode=never`를 사용한다.
  CDS의 `deploy --to postgres --dry`는 고정 compiler로 DDL을 생성하는
  경로이며 실제 deploy와 구별한다. generated DDL/CSN과 custom constraint
  허용 차이를 검사한다. Flyway의 `migrate`, `validate`, `info`, `target`으로
  fresh와 새 ontology v1→v2를 인수한다. PG 지원 module이 별도로 필요하며
  현재 Flyway 공식 문서는 PG18 verified를 표시한다.
  [CAP persistence][cap-db], [PG evolution][pg-evolution], [Flyway PG][flyway-pg]
- CAP5.1.1에는 `OutboxService.submit(String, OutboxMessage[, Schedule])`와
  `outboxed(...)`가 있다. `OutboxMessageEventContext` handler와
  `cds.outbox.Messages` 조회를 관찰 입력으로 사용할 수 있다.
  outbox는 commit 후 처리하며 user context를 system user로 낮춘다.
  현재 grant·claim fence를 worker에서 재검증해야 한다. 기본 unordered
  처리나 queue 성공은 목표 성공이 아니다. Java에는 Node의
  succeeded/failed callback과 동등한 API가 아직 없다. 안정된 external
  operation ID와 UNKNOWN_EXTERNAL 재발행 금지 계약은 별도 검증한다.
  [CAP Java queues](https://cap.cloud.sap/docs/java/event-queues)
- custom MCP/REST 인증은 security dependency와 endpoint 설정을 명시한다.
  CAP의 기본 `model-relaxed`, unknown endpoint 설정과 custom
  `SecurityFilterChain` precedence를 확인한다. `@WithMockUser`는 local
  테스트 입력이며 실제 token·IAS binding 인수가 아니다.
  [CAP Security](https://cap.cloud.sap/docs/java/security)

## Gherkin runner 후보

Cucumber8.0.4의 공식 tag 문서는 JUnit6 Platform을 명시한다. 8.0.0의
changelog는 Platform/Jupiter6.1.2 사용을 기록한다. Boot 기본6.0.3을 그대로
섞는 조합을 확정하지 않는다. 후보는 Cucumber BOM/java/platform-engine
모두8.0.4와 JUnit 계열6.1.2 explicit override다. 이 추가 조합은
`NOT_RUN`이며 dependency resolution도 아직 하지 않았다.
[Cucumber changelog][cucumber-changelog], [engine README][cucumber-engine]

Surefire는 feature resource만 자동 발견하지 못하므로 `@Suite`,
`@IncludeEngines("cucumber")`, `@SelectClasspathResource(...)` 또는
Console Launcher를 사용한다. 3.5.6에서는
`cucumber.junit-platform.naming-strategy=long`을 설정할 수 있다.
runner의 `failIfNoTests=true`와 예상 scenario 수를 함께 확인한다.
SIT/UAT glue를 나누더라도 공유 fixture와 oracle를 축소하지 않는다.

## 실행 증거와 한계

아래 명령은 저장소 밖 temporary probe에서 실행했다. Maven probe 입력은
manifest의 parent/BOM/direct dependency 목록에 보존했다. Maven cache와
credential settings는 복사하지 않았다. 선택한 로그 전체를 읽고 secret
pattern을 검사했으며 credential·token 원문은 발견하지 않았다.
Maven 로그의 행 끝 공백만 정규화했다. 원본과 보존본의 hash는 manifest에
구별해 기록했다.

| 명령 | exit | 관찰·보존 증거 |
|---|---|---|
| dependency plugin3.10.0 `resolve` | 0 | `BUILD SUCCESS`,14.931초, [resolution.log](evidence/platform-research/resolution.log) |
| help plugin3.5.2 `effective-pom` | 0 | 관리 버전 확인, 큰 POM은 복사하지 않음 |
| wrapper plugin3.3.4 `wrapper` | 0 | 3.9.16/only-script 생성, [wrapper.log](evidence/platform-research/wrapper.log) |
| `./mvnw -s settings.xml -version` | 0 | Maven3.9.16·Java21.0.5 확인 |
| npm lock-only install | 0 | engine warning 있음, [npm-resolution.log](evidence/platform-research/npm-resolution.log) |
| `docker buildx imagetools inspect postgres:18.5` | 1 | tag not found |
| `docker buildx imagetools inspect postgres:18.6 --raw` | 0 | [image index](evidence/platform-research/postgres-image-index.json), DB 실행 안 함 |

```sh
./apache-maven-3.9.16/bin/mvn -s settings.xml -B -ntp \
  -Dstyle.color=never \
  org.apache.maven.plugins:maven-dependency-plugin:3.10.0:resolve
./apache-maven-3.9.16/bin/mvn -s settings.xml -B -ntp \
  -Dstyle.color=never \
  org.apache.maven.plugins:maven-help-plugin:3.5.2:effective-pom \
  -Doutput=effective-pom.xml
./apache-maven-3.9.16/bin/mvn -s settings.xml -B -ntp \
  -Dstyle.color=never \
  org.apache.maven.plugins:maven-wrapper-plugin:3.3.4:wrapper \
  -Dmaven=3.9.16 -Dtype=only-script
./mvnw -s settings.xml -version
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
docker buildx imagetools inspect postgres:18.6 --raw
```

Maven3.9.16 tarball의 SHA512는
`831a8591fe20c8243b1dbe7d71e3244f31d1665b0804b2e825e38cbbe5ce0cafb8338851f90780735568773e0a6cd07bbec107cda0b896b008b861075358b6f6`이다.
published checksum과 대조했다. Wrapper 실행은 일반 `~/.m2/wrapper/dists`
cache에도 distribution을 저장했다. dependency resolution은 앱 compile,
Spring context 시작, 테스트 발견·실행이나 업무 성공의 증거가 아니다.

남은 인수는 CAP/CDS compile, Node24 patch, Cucumber/JUnit override,
운영 security/identity, PG18.6 CQN·거래·잠금·fence·Flyway·outbox,
MCP wire·restart·restore와 BTP다. 모두 `NOT_RUN`이다. 실모델과 유료
배포도 실행하지 않았다. 후속 S0 decision은 이 기록을 근거로 실행한 뒤
별도로 작성한다.

[cap-pom]: https://repo.maven.apache.org/maven2/com/sap/cds/cds-starter-spring-boot/5.1.1/cds-starter-spring-boot-5.1.1.pom
[pg-pom]: https://repo.maven.apache.org/maven2/com/sap/cds/cds-feature-postgresql/5.1.1/cds-feature-postgresql-5.1.1.pom
[boot-pom]: https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom
[tc-pg]: https://java.testcontainers.org/modules/databases/postgres/
[boot-tc]: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
[cap-tx]: https://cap.cloud.sap/docs/java/event-handlers/changeset-contexts
[spring-test-tx]: https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/tx.html
[cap-lock]: https://cap.cloud.sap/docs/java/working-with-cql/query-execution
[pg-lock]: https://www.postgresql.org/docs/18/explicit-locking.html
[pg-iso]: https://www.postgresql.org/docs/18/transaction-iso.html
[cap-db]: https://cap.cloud.sap/docs/java/cqn-services/persistence-services
[pg-evolution]: https://cap.cloud.sap/docs/guides/databases/postgres#schema-evolution
[flyway-pg]: https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database
[cucumber-changelog]: https://raw.githubusercontent.com/cucumber/cucumber-jvm/v8.0.4/CHANGELOG.md
[cucumber-engine]: https://raw.githubusercontent.com/cucumber/cucumber-jvm/v8.0.4/cucumber-junit-platform-engine/README.md
