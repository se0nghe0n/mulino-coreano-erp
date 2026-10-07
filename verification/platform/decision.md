# S0 플랫폼 판정

CAP가 단일 저장 모델과 PostgreSQL 거래를 유지할 수 있는지 실제로
확인해야 후속 도메인을 같은 경계 위에 구현할 수 있다. 새 isolated
기술 fixture에서 CAP 후보의 강제된 이중 persistence·별도 인가·비원자적
쓰기 문제를 발견하지 않았다. **로컬 CAP 경로를 채택한다.** 부모 Task
branch의 통합 재검증 전에는 S0 통합 gate를 닫지 않는다.

사용자 Step3 구현의 S0 산출물이다. baseline은
`4280a750dca0aef35abaf10b6797881181dd88d7`, 구현 commit은
`9ba927c`, `8c6248f`, `8310120`다. 추적 issue·보존 inventory·R3의
실제 운영 자료 없음 결정은 coordinator 산출물과 결합한다. 기존 코드,
schema, 테스트, role skill을 읽거나 복사하지 않았다. S1 도메인은 아직
구현하지 않았다.

## 검증된 경로

- `backend/pom.xml`: CAP Java5.1.1, Boot4.1.1, Java21, Maven3.9.16의
  한 module이다. CDS는 고정 compiler7.1.1로 CSN/EDMX를 생성한다.
  CAP 기본 CSN 이름과 OData adapter를 실제 startup에서 확인했다.
- `database/migrations`: Flyway12.4.0만 DDL을 실행한다. CAP deployer와
  Spring SQL initializer는 사용하지 않는다. v1과 v2를 빈 PG18.6에
  적용했고 v1의 WAITING/definition-v1/evidence/idem/PENDING outbox를
  남긴 새 DB를 v2로 올렸다. `Preserved`는 S0 기술 fixture이며 실제
  Work/Definition/Evidence 도메인의 V8 전체 인수를 뜻하지 않는다.
- `PlatformCommands`: 현재 인증 주체·조직·stable owner에서 현재 grant와
  policy를 검사한다. scope fence를 획득한 뒤 제한을 다시 읽는다.
  도메인 수량 효과, audit, durable outbox, idem 결과를 CQN으로 같은
  Spring transaction에 기록한다. rollback assertion은 테스트 전체
  rollback이 아니라 독립 JDBC 관찰을 사용한다.
- `ScopeRepository`: PostgreSQL 전용 고정 잠금 SQL에 값을 binding한다.
  모든 제한 추가·grant 철회·policy 변경도 같은 안정된 scope row를
  잠그는 계약이다. 실제 `pg_blocking_pids(holderPID)`와
  `pg_stat_activity`의 Lock waiter를 관찰한 뒤 mutator를 commit한다.
  timeout/deadlock/serialization SQLSTATE는 효과0 `LOCK_CONFLICT`다.
  자동 재시도는0회이며2초 lock timeout 뒤 최신 의도를 자동 실행하지
  않는다. 다중 scope·분할·출고 등의 전체 V2/V3는 후속 인수다.
- `SecurityConfiguration`/`CapIdentityProvider`: 한 Spring filter chain이
  모든 endpoint에 서명 JWT를 요구하고 CAP에도 동일 identity를 연결한다.
  org/owner claim과 iss/aud/exp/nbf를 검증한다. local profile의 RSA fixture만
  지원한다. 실제 IdP binding 구현 전 모든 nonlocal startup을 거부한다.
  빈 binding marker, role header, payload 역할은 권한이 되지 않는다.
- REST read/action, CAP OData read/action, custom MCP read/action은 같은
  application 인가/command를 호출한다. grant 없는 직접 조회는403이며
  검색은 조직과 현재 grant에 따라 빈 결과다. 일반 core CRUD는 노출하지
  않는다. S0 CAP projection은 단순 조회만 인수했으며 전체 query shaping과
  도메인별 공개 수량·단위 표현은 S1 이후 계약으로 확장해야 한다.
- MCP2026-07-28의 `server/discover`, tools/list/call, namespaced metadata,
  header 일치, stateless JSON 응답을 직접 구현했다. Java SDK2.0.1은
  해당 modern protocol을 지원하지 않아 SDK를 사용했다는 주장을 하지
  않는다. 모든 result는 complete이며 업무 거부는 tool isError와 구조화
  outcome이다. full T20, 실제 agent client discovery/skill loading은
  별도 S5 인수다.

## 실행 결과와 source parity

`versions.json`과 `spike/dependencies.txt`에 실제 조합을 고정했다.
현재 공식 CAP 문서는 Java PG15.x 검증 범위를 안내하므로 PG18.6의 지원을
문서만으로 추정하지 않았다. 실제 container version, compiler schema
비교와 transaction/lock 실행을 근거로 로컬 후보를 판정했다. 사전 조사
이미지 digest와 실제 pull digest가 달라 실제 이미지를 따로 고정했다.
[CAP PostgreSQL](https://cap.cloud.sap/docs/guides/databases/postgres),
[CAP persistence](https://cap.cloud.sap/docs/java/cqn-services/persistence-services),
[CAP security](https://cap.cloud.sap/docs/java/security)

`JWT_PUBLIC_KEY=<ephemeral public.pem> NODE24_BIN=<Node24.19.0>`와
Node24 bin을 PATH 앞에 두고 `npm ci --ignore-scripts --no-audit --no-fund`로
고정 lock을 준비한 `cd backend; ../mvnw -B -ntp test package`에서
**11 tests, failures0/errors0/skipped0**을 확인했다. audit 실패 rollback,
예약 경합, restriction phantom·grant·policy 선행 commit, 역순 효과 이력,
2초 잠금 충돌, populated schema upgrade, 실제 fresh compiler 출력과
Flyway DB column/type 비교, pg_dump/pg_restore 효과 복원을 검사했다.
`spike/integration.log`와 `integration-results.txt`가 증거다.
`lock-timeout-before-fix.log`는 Spring7의 SQLSTATE55P03가
UncategorizedSQLException으로 분류돼 처음 실패한 기록을 보존한다.
원인별 repository 변환을 추가하고 같은 assertion으로 재검증했다.

독립 보안 담당의23 HTTP assertions와 protocol 담당의29 requests/
118 assertions가 통과했다. 최초500/잘못된 metadata namespace 실패와
fixture nbf 경과 실패도 담당 artifact에 보존했다. DB를 별도로 관찰해
보안 counter-call3회의 효과3과 protocol 각 실행의 효과1을 확인했다.
최종 protocol DB 관찰은 서로 다른 key로 실행한2회의 누적 효과2다.

기존 worker wire 보고서는 실행 JAR hash를 소급 복원할 수 없어 source
parity가 완결되지 않았다. 최초 부모 clean run은 npm bootstrap이 없어
`cds command not found`로 실패했다. runner가 npm11.19.1을 검사하고
Node24.19.0에서 고정 lock의 `npm ci`를 먼저 실행하도록 수정했다.
coordinator는 실패 로그를 보존하고 전체 command를 재실행한다. `deploy/local/verify-platform.sh`가 최신
통합 source를 다시 빌드하고 executed JAR SHA256/input hashes를 생성한
뒤 새 DB에서 보안·wire를 실행한다. coordinator는 이 결과를 결합한 뒤
S0를 닫아야 한다. 이전 HTTP 성공을 새 JAR 성공으로 자동 승격하지 않는다.

## 남은 경계

R1은 로컬 CAP/CQN 선택으로 결정한다. 운영 R4 규제 policy, R5 실제
IdP/조직 관리자/위임/책임 mapping, R7 BTP region·entitlement·비용·binding·
TLS, R8 실제 모델·client·skill 인수는 미해결이다. nonlocal startup 거부를
실행으로 확인했으며 운영 접근을 활성화하지 않는다. buildpack은
NOT_RUN이며 로컬11 tests나 signed fixture가 BTP/운영 인증 성공을
대신하지 않는다.

S0 outbox는 동일 거래에서 PENDING record를 내구성 있게 남기는 부분
증거다. worker claim/lease/reconciler, 외부 대조·응답 유실·restart, 실제
도메인 증거 blob/정의 artifact 복원, 모든 D/C/V/E와 모델 인수는
후속 구현과 검증 대상이다. full41 case PASS나 V1–V8 전체 PASS로
기록하지 않는다. CAP의 기술적 기각 조건은 현재 발견하지 않았다.
