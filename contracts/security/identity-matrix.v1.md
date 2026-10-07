# 신원과 endpoint 계약 v1

payload나 역할 header는 신원을 만들지 않는다. Java21, CAP5.1.1,
Spring Boot4.1.1 후보의 모든 공개 경로는 하나의 Spring Security chain과
검증된 AuthContext를 거쳐 같은 application authorization을 사용한다.
계약은 S0 slice의 납품 기준이며 전체 도메인 인수 완료 주장이 아니다.

| 입력/경계 | 신뢰 조건 | 실패 결과 |
|---|---|---|
| Bearer JWT | 허용 RS256 서명, 고정 issuer/audience, exp/nbf 검증 | 401 |
| organizationId | 검증된 IdP claim과 조직 mapping | 누락401, 타 조직403 |
| sub | issuer+subject로 고정 actor mapping | 누락401 |
| stableRequestOwner | trusted claim mapping의 안정 owner | 누락401; token/RPC ID로 대체 금지 |
| delegator | 서버 grant의 delegator; 서명 claim만으로 위임 발급 금지 | 미확인/범위 초과403 |
| role/capability | 서버의 현재 배정과 grant의 교집합 | payload/header 역할 확대403 |
| policy | commit fence 아래 현재 승인/정책/물량별 허용 | 미설정 효과0 |
| replay | organization+stable owner+capability+key, 현재 조회 권한 | 타주체 결과 누출0 |
| worker | 원 actor/grant와 현재 fence 재검증 | system user 자체 권한 승격 금지 |

GET `/api/platform/scopes/{id}`, POST `/api/platform/actions/reserve`,
GET `/odata/v4/PlatformService/Scopes`, POST
`/odata/v4/PlatformService/reserve`, POST `/mcp`는 같은 chain으로 보호한다.
OData metadata·nested·projection·batch도 별도 허용 경로를 만들지 않는다.
범용 core INSERT/UPDATE/DELETE는 공개하지 않는다. 미구현 nested/batch,
worker/blob 경로는 NOT_RUN이며 응답 거부만으로 rollback을 증명하지 않는다.
READ grant의 쓰기 거부는 권한 있는 동일 입력의 counter-call과 독립 DB의
전후 효과로 확인한다. batch 구현 시 금지 부분을 포함한 changeset 전체를
rollback한다. 외부 조직 검색은 빈 결과 또는 정보가 없는 구조화 거부다.

LOCAL_SIGNED_FIXTURE는 개발 profile에만 허용한다. 테스트용 RSA private
key와 token은 임시 경로/환경에만 두며 commit·로그에 보존하지 않는다.
운영은 검증된 IAS/IdP binding, trusted organization/owner mapping,
현재 capability/grant/승인자 mapping이 모두 없으면 startup 또는 해당
scope를 fail-closed한다. key와 issuer 설정만으로 운영 binding을 대신하지
않는다. R5의 실제 사용자/intake owner/supervisor mapping과 R7 BTP 인수는
NOT_RUN이다. fixture 조직이나 역할을 운영 기본값으로 사용하지 않는다.

2026-10-08 공식 자료를 확인했다. CAP 문서는 identity dependency와
IAS/XSUAA binding이 함께 있어야 자동 보안을 켠다고 설명한다.
`model-relaxed`의 공개 가능성과 custom chain precedence를 확인했다.
완전한 override는 `cds.security.authentication.authConfig.enabled=false`로
CAP chain을 끈다. `cds.security.mock.enabled=false`와
`cds.security.mock.defaultUsers=false`로 mock 신원을 끈다. 명시 chain은
모든 경로를 보호하며 CAP UserInfo에 동일 검증 주체를 연결해야 한다.
Spring 문서는 signature/exp/nbf/issuer 검증과 별도 audience 검증을
설명한다. 이 계약의 current grant/policy fence는 애플리케이션 책임이다.

- [CAP Java Security](https://cap.cloud.sap/docs/java/security)
- [Spring Security JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
