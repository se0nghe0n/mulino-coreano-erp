# S1 신원과 조회 인가

외부 subject를 actor UUID로 해석하면 `writer-a` 같은 실제 로그인
표기를 잃고, token의 owner claim을 그대로 믿으면 멱등 namespace가
요청자 입력에 좌우된다. 외부 issuer·subject·조직 alias를 서버의
UUID 조직·actor와 연결하고 stable owner는 actor row에서 읽는다.
S0의 `CurrentIdentity`와 로그인 검증은 기술 fixture로 유지한다.

baseline은 `164414418b6c3ce2959979939388d59cf2068e2c`이며 사용자
Step3·계획 S1 작업이다. 소유 범위는 identity CDS·V3·identity Java·
해당 tests·이 기록이다. 공통 port는 integration worker의
`b1a3b87`, `88a5287`, `e414963`을 사용한다. 이전 ERP source·schema·
tests를 재사용하지 않았다.

## schema와 서비스 계약

- `V3__identity.sql`은 organization, actor, external identity,
  membership, capability assignment, grant와 action/scope, authority
  fence를 생성한다. 조직과 actor ID는 UUID이며 CDS UUID와 SQL
  `VARCHAR(36)`가 대응한다. actor 참조는 `(organizationId, ID)`
  composite FK다. 다른 조직 actor의 membership/grant를 거부한다.
- `IdentityRepository`는 CQN으로 현재 identity·membership·assignment·
  grant를 조회한다. PostgreSQL SQL은 binding한 authority row 잠금과
  transaction-local 2초 lock timeout뿐이다. 효과 쓰기 API는 없다.
- `IdentityAuthorization`은 검증된 JWT의 issuer/subject/org alias를
  서버 mapping으로 해석한다. role claim/header/payload를 인가로
  사용하지 않는다. 현재 조직 membership ∩ capability assignment ∩
  현재 grant/action/scope가 모두 있어야 조회를 허용한다. ADMIN/FDE
  문자열에 business 권한을 부여하지 않는다.
- `permittedScopes`의 scope 차원은 교집합이고 같은 차원 ID들은
  대안이다. 예를 들어 ITEM P와 PLACE W grant는 P 또는 W가 아니라
  P이면서 W인 후보를 요구한다. WORK·TARGET·SOURCE도 같은 규칙이다.
  SOURCE는 evidence의 namespace에 대응하는 서버 SourceProfile UUID다.
- 중앙의 null target 검사는 후보가 있다는 coarse 검사다. 각 handler가
  실제 행의 TARGET·ITEM·PLACE·WORK·SOURCE 연결을 조합해 다시 검사한다.
  조합 정보가 없으면 허용하지 않는다. 역사 조회의 asOf/knownAt은
  현재 만료·철회를 되돌리는 인가 시점이 아니다.
- `IdentityQueries`는 getAccessContext/getGrant port를 구현한다.
  다른 조직 또는 다른 actor/delegator의 grant를 노출하지 않는다.
  getGrant direct 요청도 handler에서 실제 grant scope를 검사한다.
- `IdentityControlGuard`는 후속 S2 command 경계의 guard port다.
  인간 delegator·같은 조직 recipient·현재 행동/범위·validity 상한·
  actor fence를 요구하며 self grant/self capability 상향을 차단한다.
  여러 ID scope의 모든 조합을 검사하고 256 조합을 넘으면 거부한다.
  capability 부여는 자신에게 있는 해당 capability 범위 안으로 더
  제한한다. 관리 manifest 기반의 별도 허용 capability 범위 확장은
  아직 없다. 이 guard는 공개 쓰기 API 또는 명령 완료가 아니다.

## 실행 증거

Node24.19.0·Java21·CAP5.1.1·Boot4.1.1·Maven3.9.16과 고정 PG18.6
container에서 수행했다. npm lock으로 90 packages를 설치했다.
JWT public fixture만 환경에서 참조하며 token/private key는 기록하지
않는다. 테스트를 위한 임시 CDS import는 `srv/identity-build.cds`로
준비했으며 납품에는 포함하지 않는다. 통합 srv가 identity를 import해야
한다.

`JWT_PUBLIC_KEY=<ephemeral public.pem>`와 Node24 PATH를 설정하고
`cd backend; ../mvnw -B -ntp
-Dtest=IdentityAuthorizationTest,IdentityPersistenceTest test`를 실행해
최종 8 tests·failure0·error0·skipped0을 확인했다. 실제 빈 DB Flyway와 CQN
mapping, 서버 owner, 조직 composite FK, 직접/검색 범위 거부,
조회 grant가 쓰기 capability를 허용하지 않음, 철회 뒤 거부를 검사한다.
PostgreSQL 테스트의 JWT는 in-process authenticated fixture이므로
signed HTTP acceptance 증거가 아니다. 새 scope 차원 교집합 assertion은
unit 6 tests·failure0·error0으로 별도 통과한 뒤 최종 PG 포함
8 tests를 다시 통과했다. `final-checks.log`가 최종 증거다.

최초 unit 실패는 fixture JwtAuthenticationToken의 authenticated 상태가
빠진 원인이며, 최초 PG test compile 실패는 CAP requestContext run의
Consumer/Function overload 모호성이다. 수정 전 로그도 보존한다.

## 미실행과 후속 경계

실제 IdP·운영 관리자 mapping R5는 NOT_RUN이다. 기존 SecurityConfiguration이
local 이외 startup을 계속 거부한다. 전체 C3 matrix, T08 HTTP,
V4 actor/grant 변경 영향, V7 grant 철회 경합은 S2·통합 인수에서
실행해야 한다. control-plane 명령의 revision·멱등·감사·boundary index·
책임/nextCheck 갱신은 동일 transaction의 S2 command 구현에 연결해야
한다. S1 guard만으로 해당 쓰기나 V7를 PASS로 표시하지 않는다.
