# 조직과 위임 경계의 테스트 계약

다른 조직의 ID·검색·FK·nested relation과 인증 문맥을 개별
subcase로 검사한다. alias는 fixture 설치가 발급한 opaque ID이며
payload의 조직·actor·역할과 문서의 ADMIN 지시는 권한이 아니다.

- direct/search/nested: 실제 response에 다른 조직 object가 없고
  구조화된 거부 또는 빈 검색 결과를 확인한다. 오류 object는
  `FORBIDDEN` code만 가져 타 조직 ID를 error details로 누설하지 않는다.
- FK: 실제 관계 원행의 변화0과 DB의 조직+target FK column을 대조한다.
- grant: 최소 범위 발급·철회, revision/효력시각/boundary·감사/의무,
  scoped IDENTITY_ADMIN의 assignment/revoke와 FDE·업무 ADMIN의
  암묵적 권한 상향 거부를 구별한다.
- actor/delegator/organization은 실제 인증 provenance의 ID와 비교한다.
  미설정 정책은 POLICY_UNRESOLVED, 업무 효과0이다.

모든 거부의 전후 scope는 독립 DB snapshot이다. 감사와 rejected
command record는 업무 원행 불변 비교에서 제외한다. 실제 인가는
서버에서 수행하며 fixture나 문서가 서버 결정을 대신하지 않는다.

모든 fixture는 가상값이다. Step2의 parser·assertion·RED 준비와 실제
제품 DB/API/MCP/경합/host/모델 인수를 구별한다. 제품 인수는 NOT_RUN이다.

작성 검증의 실제 명령·exit·scenario 수·파일 hash는
`verification/cases/T08/evidence/authority-suite/checks.json`에 있다.
`observation-bindings.json`은 이 case의 모든 catalog observation을
구체 subcase/action/assertion 및 JSON pointer에 연결한다.
고정 수량 oracle의 primary와 보조 관계/assertion을 함께 보존한다.
이 연결은 작성 증거이며 실제 제품 효과를 관측한 결과가 아니다.
