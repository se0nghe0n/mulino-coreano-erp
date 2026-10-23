# S5 서버 인증 MCP transport 관찰

서명한 JWT만으로 서버가 확정한 주체를 증명할 수 없던 공백을
verification 전용 서버 receipt로 보완한다. 실제 요청의 검증된
SecurityContext와 제품의 `IdentityAuthorization.context`가 확정한
issuer·subject·audiences·조직·actor·stableRequestOwner를 읽는다.
같은 read snapshot에서 해당 actor의 grant·actions·scopes·delegator·
유효기간·철회시각·revision과 membership·capability assignment를
관찰한다. caller가 보낸 actor·role·owner나 기대 oracle을 복사하지 않는다.

기준선은 `a7f18cc8e7124b9fd1cc8bf811687ed7230a4981`이다.
격리 branch는 `fix/s5-auth-transport-capture`이며 다른 writer의 변경을
가져오지 않았다. 기존 backend·공개 schema·agents·model fixture·
물량/final-response observer는 수정하지 않았다. 추가 허용을 받은
기존 harness test 한 메서드만 새 fail-closed 계약에 맞췄다.

## 활성화와 소유권

다음 조건이 모두 맞아야 filter·store·조회 controller가 등록된다.

- active profile 집합이 정확히 `local`, `verification`이어야 한다.
- `mulino.verification.mcp-capture-enabled=true`여야 한다.
- `server.address`가 literal loopback이어야 한다.
- 실제 요청의 local/remote socket 주소도 모두 loopback이어야 한다.

실행 환경에서는 `SPRING_PROFILES_ACTIVE=local,verification`,
`MULINO_VERIFICATION_MCP_CAPTURE_ENABLED=true`를 설정한다.
기존 verification clock이 요구하는
`MULINO_VERIFICATION_INSTANT`도 명시한다. wall-clock 관찰 시각과
제품의 `authorityAsOf`를 구별한다. `production` profile이 포함되거나
`local`만 있으면 enable flag가 있어도 관찰 면은 등록되지 않는다.

`POST /mcp/ontology`에 UUID인 `X-Mulino-Verification-Capture`를
보내면 서버가 관찰한다. 요청 처리 후 원 request/response bytes의
SHA-256, JSON-RPC ID·method·tool name, nonce, HTTP status와 시각을
receipt에 묶는다. 공개 MCP body와 오류 계약은 그대로 유지한다.
관찰이 완성되면 verification 응답 header
`X-Mulino-Verification-Receipt`에 서버가 발급한 ID를 보낸다.

인증된 `GET /internal/verification/mcp-captures/{id}`만 receipt를
읽는다. 현재 identity mapping과 membership을 다시 확인하고, issuer·
subject·audiences·조직 alias/ID·actor·서버 owner가 원 요청과 같아야
한다. 다른 주체·조직·숨겨진 ID·소비된 ID는 같은 404 body를 돌려준다.
다른 주체의 실패한 조회는 원 receipt를 소비하지 않는다. 미인증 요청은
기존 security chain이 401로 거부한다. `Cache-Control: no-store`를 쓴다.

store는 process-local 메모리에 최대 128개만 보관한다. 읽기 유효기간은
60초이며 성공한 소유자 조회는 한 번만 가능하다. 만료 행은 다음 store
접근에서 제거되고 process 종료 시 전체가 사라진다. 파일·DB·outbox에
snapshot을 저장하지 않는다. 업무 원행은 SELECT로만 읽으며 observer
조회가 업무 쓰기를 만들지 않는다는 실제 전후 table 원행 비교를 했다.

## 인증 관찰과 행동 인가

`observationPhase=BEFORE_DISPATCH_READ_SNAPSHOT`과
`actionAuthorization=NOT_ASSERTED`를 명시한다. captured grant의 존재나
활성 여부만으로 어떤 명령도 허용됐다고 판단하지 않는다. 이 관찰은
인가 또는 commit fence 안의 결정 증거가 아니다. 제품의 all-of scope,
위임자 권한, 철회, 현재 제한과 commit fence는 기존 경로가 판단한다.
그 경로를 변경하거나 별도 observer 인가로 대체하지 않았다.

`NativeMcpTransport`는 같은 credential로 서버 receipt를 별도 조회하고
원 request/response hash·RPC·nonce·HTTP status·시각·서버 identity와
관찰의 필수 필드를 검증한다. org/issuer/subject/audience 기대값은
교차 대조에만 사용한다. 관찰 필드는 서버 receipt에서만 가져온다.
JWT·Authorization header·private key는 evidence에 넣지 않는다.

완전하고 일치하면 `authenticatedActorStatus=CAPTURED`로 저장한다.
없거나 불완전하거나 다른 요청이면 raw wire를 `UNAVAILABLE`로 먼저
저장하고 `send`를 실패시킨다. unsigned/synthetic 대체 관찰이나 JWT만의
성공 경로는 없다. 기존 no-hook double도 `SERVER_CAPTURE_ABSENT`로
실패해야 한다. 해당 selftest는 저장된 raw request/response를 독립적으로
읽어 tool schema·typed rejection·nativeReply·credential 비저장을
검사하며 synthetic hook을 실제 backend 증거로 세지 않는다.

## 집중 검증과 보존된 실패

[실행 manifest](evidence/s5-server-authenticated-transport/manifest.json)에
정확한 command·exit code·기준 commit·source/input/class/JAR hash·
artifact hash·cleanup을 기록한다. 압축 artifact는 원 bytes를 보존한다.
상태는 아래 집중 검증 범위에만 적용하며 coverage manifest의 전체
제품 인수 PASS를 뜻하지 않는다.

| 검증 | 결과 | 범위 |
|---|---|---|
| backend boundary + 기존 IdentityAuthorizationTest | PASS, 11 tests, exit0 | profile/production 미등록·socket guard·single-use·foreign owner·expiry/capacity·기존 all-of/철회/위임 경계다 |
| 실제 HTTP/CAP/PostgreSQL | PASS, 10 tests, exit0 | identity/DB owner·wrong issuer/audience/org·미인증·foreign/hidden receipt·철회·scope 불일치·위임자 권한 부재·payload actor·wire mismatch·읽기 전후 DB 불변이다 |
| harness 집중 검증 | PASS, 10 tests, exit0 | 변조/누락 4 tests, 기존 integration selftest 5 tests, packaged JAR 실제 probe 1 test다 |
| packaged JAR 실제 probe | PASS, exit0 | transport가 실제 서버 receipt를 소비하고 DB owner·철회 revision2와 FORBIDDEN을 함께 관찰한다. 같은 JAR를 local-only로 재시작하면 capture 부재로 실패한다 |
| backend/harness package | PASS, exit0 | 이 worktree에서 빌드한 JAR/class를 hash로 연결한다 |
| scenario swarm·41/802 SIT | NOT_RUN | root의 후속 실행 소유다 |
| 실제 client/model·Workflow·배포 | NOT_RUN | 이번 작업에서 실행하지 않았다 |

첫 backend build는 새 worktree에 CDS 의존성이 없어 exit1이었다.
`backend/package-lock.json`의 `npm ci --ignore-scripts` 뒤 해결했다.
첫 PostgreSQL test는 Testcontainers가 `/var/run/docker.sock`을 찾아
exit1이었다. 실제 Docker context의 socket을 `DOCKER_HOST`로 전달해
해결했다. 이 실패 때 남은 해당 public-key 임시 디렉터리도 제거했다.

packaged probe의 첫 compile은 test의 Timestamp import 충돌로
exit1이었다. import를 한정했다. 다음 실행은 PostgreSQL 초기화 중
접속해 exit1이었다. 소유 DB의 실제 JDBC 접속 준비를 제한 시간 안에서
확인하도록 test를 고쳤다. 초기 실패 log와 그 실행의 cleanup proof를
보존한다. 성공 뒤 새 변경이 없는 전체 suite를 반복하지 않았다.

재현 명령은 다음과 같다. wrapper는 Maven 3.9.16, JDK는
21.0.12.1이며 실제 PostgreSQL은 고정 digest의 18.6이다.

```sh
(cd backend && npm ci --ignore-scripts)
./mvnw -B -ntp -f backend/pom.xml -DskipTests package
./mvnw -B -ntp -f backend/pom.xml \
  -Dtest=McpCaptureBoundaryTest,IdentityAuthorizationTest test
DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')" \
  ./mvnw -B -ntp -f backend/pom.xml \
  -Dtest=McpCaptureHttpPostgresTest test
S5_AUTH_CAPTURE_ACTUAL=true ./mvnw -B -ntp \
  -f verification/harness/pom.xml \
  -Dtest=NativeMcpTransportActualTest,NativeMcpTransportCaptureTest,NativeProductIntegrationTest test
./mvnw -B -ntp -f verification/harness/pom.xml -DskipTests package
```

actual probe는 자신의 임시 key/blob 디렉터리·Docker container·backend
process만 만들고 finally에서 제거한다. private key는 디스크에 쓸 때도
0600이며 evidence에 복사하지 않는다. PostgreSQL·backend 종료와 임시
파일 제거는 probe JSON과 실제 assertion으로 확인한다. Spring HTTP
test의 별도 PostgreSQL·임시 파일 제거도 cleanup JSON으로 남긴다.

## 남은 인수

`NativeProductIntegration`의 readiness/ProductDriver gate는 그대로다.
transport가 CAPTURED여도 전체 native client 실행을 열지 않는다.
완전한 model fixture 설치, physical/semantic/final-response observer와
scope binding, 실제 client/model 인수 및 다른 S5·S6 gate는 후속 작업이다.
관찰 hook의 집중 검증만으로 Task·S5·S6를 완료로 표시하지 않는다.
root의 frozen source와 실행에는 이번 변경을 통합하지 않았다.
