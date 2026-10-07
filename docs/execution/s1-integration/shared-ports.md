# S1 공통 read port

서로 다른 adapter가 인가·시점·오류를 다시 계산하지 않도록 공통
application port를 둔다. S0 platform 증명 경로와 제품 read 경로는
분리한다. S1에서는 제품 write dispatcher를 열지 않는다.

- `DomainContext`: 검증된 외부 신원에서 서버가 찾은 조직·actor·owner,
  요청의 `asOf`와 `knownAt`이다. client identity override를 허용하지 않는다.
- `ReadAuthorizer`: 현재 mapping과 capability/target grant를 검사한다.
  search의 target은 null이고 결과별 target도 다시 검사한다.
- `QueryRequest`: operation, id, scope, whitelist filter, page limit,
  cursor, definitionVersion, asOf, knownAt, snapshotRef다.
- `QueryHandler`: 지원 operation을 명시하고 scope를 제한한 CQN을 사용한다.
- `QueryResult`: data, scope, unknowns, conflicts, evidenceRefs, nextCursor다.
  공통 application은 snapshotRevision을 포함한 공개 envelope를 만든다.
- `DomainError`: domain outcome과 code를 모든 adapter에 동일하게 전달한다.

내부 조직은 UUID다. FK는 `mulino_identity_Organizations(ID)`를 참조한다.
actor는 `(organizationId, ID)`로 참조한다. CDS namespace와 Flyway table은
CAP compiler의 underscore 이름을 사용한다.
