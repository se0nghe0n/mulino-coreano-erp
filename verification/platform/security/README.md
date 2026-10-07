# S0 독립 HTTP 보안 probe

서명 실패와 현재 grant의 거부를 실제 HTTP 응답으로 관찰한다. stdlib Node
probe는 backend의 JWT 생성 코드·테스트 mock·fixture 판정값을 재사용하지
않는다. `prepare`는 임시 RSA key와 JWT를 생성하고 public key만 서버에
제공한다. `run`은 공개 endpoint를 호출하며 token과 private key를 evidence에
쓰지 않는다. token의 `role: WRITE`와 payload 역할은 서버 grant보다 크지
않아야 한다.

```sh
node verification/platform/security/security-smoke.mjs prepare /tmp/mulino-security
# 서버 local profile에 /tmp/mulino-security/public.pem을 설정한다.
# endpoint/seed 계약에 맞는 local-config.json을 임시 경로에 만든다.
node verification/platform/security/security-smoke.mjs run \
  /tmp/mulino-security/local-config.json \
  verification/platform/security/http-evidence.json
```

config는 `baseUrl`, `tokenPath`, `snapshot: {method,path}`, `cases`를 가진다.
case는 `id`, `route: {method,path}`, 선택 `token`, `payload`, `headers`,
`status: [허용 HTTP status]`, 선택 `bodyContains`, `snapshot`을 가진다.
status와 error code는 API 계약에서 정하며 실행 뒤 실제 응답에 맞춰
oracle를 약화하지 않는다. 테스트 seed의 권한 있는 동일 입력 counter-call을
별도 case로 포함한다. 응답에 token이 들어가는 endpoint는 사용하지 않는다.

snapshot 비교는 공개 조회의 전후 표현 불변만 증명한다. audit/outbox/원장
등 전체 DB 효과0은 플랫폼의 독립 DB 관찰과 결합해야 한다. actor에 따른
조직 경계는 다른 조직 direct ID와 search/nested가 구현될 때 확장한다.
S0 순차 revoked/policy 거부는 V7의 실제 두 transaction fence/restart나
V6의 응답 유실/새 token/RPC/동시 replay를 대신하지 않는다. 아직 없는
경로를 seed된 PASS로 처리하지 않는다.

R5 실제 조직 관리자, identity provider claim mapping, 사용자 capability,
intake owner와 supervisor는 운영 입력 미확보다. 해당 운영 scope는
BLOCKED이며 LOCAL_SIGNED_FIXTURE의 PASS가 운영 접근을 활성화하지 않는다.

최종 실행은 `http-evidence.json`의23개 HTTP assertion이 PASS다.
익명·잘못된 서명/issuer/audience·만료·미래 nbf·필수 claim 누락은401,
타 조직 direct 조회와 grant 없는 direct 조회는403이다. 타 조직/미배정
주체의 OData 목록은 빈 결과다. REST/OData/MCP의 READ grant 쓰기는
거부되고 같은 공개 경로의 writer counter-call은 성공한다. API error와
MCP tool denial의 응답 차이를 각각 assertion으로 검사한다.

`http-evidence-initial-failure.json`은 OData500과 MCP metadata400을
보존한다. `http-evidence-expired-fixture.json`의 nbf 실패는 최초 발급의
5분 미래 시각이 대기 중 지난 fixture 오류다. 마지막 발급은1시간 미래
nbf를 사용했고 같은 oracle로 모두 통과했다. 실패 기록을 삭제하지 않았다.

최종 PASS도 full DB/outbox/worker effect0, 모든 도메인 C3/V4,
V6 수령 replay, V7 동시 철회/restart와 실제 운영 R5를 대신하지 않는다.
policy UNKNOWN과 동시 fence 증거는 플랫폼 담당의 DB/transaction 검증과
결합해야 한다. 운영 profile은 실제 IdP binding 구현 전 모두 startup을
거부하며 dummy binding marker도 허용하지 않는다.

S0 검증 서버는 `--spring.profiles.active=local,platform-spike`로 실행한다.
일반 제품 profile에서 S0 REST/MCP route는 등록되지 않고 CAP의
`PlatformService`는 서버가 부여하는 spike role 없이는 접근할 수 없다.
JWT payload의 role 이름으로 이 role을 얻을 수 없다.
