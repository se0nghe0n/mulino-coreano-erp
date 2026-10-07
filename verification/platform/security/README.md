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
